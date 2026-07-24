# Wake atlas GPU upload performance (issue #197)

## Current state

The wake texture atlas (`WakeTextureAtlas`, a 2048x2048 RGBA8 image packing
many small per-node sub-textures) is uploaded to the GPU via two combined
paths (this combination was chosen empirically — see "why" below):

- **Every frame**, `WakeTextureAtlas.uploadDirty()` uploads any sub-texture
  dirtied since the last call — in practice this only ever picks up the
  sparse nodes touched by the frame-rate occlusion-only redraw for chunks
  near a moving boat (see `docs/occlusion_zones.md`), since tick-driven
  dirtiness is swept up separately, below.
- **Once per simulation tick**, `WakeTextureAtlas.uploadActiveRegion()`
  uploads the atlas's whole occupied prefix in one call if anything in it is
  dirty. `claimSubTexture()` always takes the lowest free index, so occupied
  slots are always a contiguous run from row 0 (tracked as `highWaterMark`),
  and nearly every active node goes dirty every tick regardless of whether it
  individually changed (`WakeChunk.tick()` redraws all of them unconditionally).
  This upload is zero-copy (`BetterDynamicTexture.uploadTopRows`) — a
  row-major image's top N rows are already a contiguous slice of its own
  backing buffer, so no `copyRect`/scratch image is needed.

Per-node color computation is also cached: `WakeNode.biomeColor` is sampled
once at node creation (configurable via `WakesConfig.cacheNodeWaterColor` for
live updates instead), and `WakeNode.bucketColors` — a small
(`wakeColorIntervals.size()+1`-entry) precomputed color lookup table — is
rebuilt once per tick rather than recomputing a texel's color from scratch on
every one of its ~1000 texels.

## Why: the original full-atlas-every-dirty-tick upload

Reported as "[Performance] Microstuttering/frametime jitter when any wakes
effect is around and just near water" — FPS stayed high, but frametime showed
a regular sawtooth (~4-8ms) whenever wakes were actively simulating.

The original `BetterDynamicTexture.uploadIfDirty()` used one global `dirty`
flag set by any `DrawContext.draw()` call, and re-uploaded the *entire*
atlas whenever it was set — one moving boat forced a ~16 MiB GPU upload every
tick. Alongside this, several other per-texel costs compounded the problem
before the upload mechanism itself was addressed:

- `BiomeColors.getAverageWaterColor()` was called on every `WakeNode.draw()`
  invocation, tick-rate and frame-rate alike — moved to the cached
  `biomeColor` field described above.
- `WakeColor`'s constructor called `Color.RGBtoHSB()` to populate `h`/`s`/`v`
  fields nothing in the codebase ever read (confirmed via a full-repo grep) —
  removed outright.
- A texel's final color only ever depends on which of ~9 configured
  `wakeColorIntervals` buckets its wave height falls into, blended against a
  fixed-per-node tint and fixed-per-tick opacity — so there are only ~9
  possible outputs per node per tick, not one freshly-allocated `WakeColor`
  (plus hex parsing, plus `Math.pow`/`Math.exp`) per texel. Replaced with the
  `bucketColors` lookup table described above.

## Why: bounded active-region upload per tick, not full-atlas or partial-only

Even after the above, a smaller sawtooth remained, traced to the upload
mechanism itself. Three strategies were prototyped behind a temporary debug
config toggle and measured directly via F3 debug info (`Atlas upload:
Xms/t, N px rows`, plus the existing `Dirty uploads/f` and `Render/f`
counters), before settling on the current fixed combination:

- **Partial-only** — no tick-rate upload at all; every dirty sub-texture,
  tick- or frame-sourced, goes through the per-frame `uploadDirty()` path.
- **Partial + full-atlas-per-tick** — adds one full-atlas upload per tick
  (the original pre-fix behavior, just bounded to tick-rate instead of every
  dirty frame).
- **Partial + active-region-per-tick (current)** — adds the bounded
  active-region upload per tick described above.

Measured on the same scene (player wading, no boats, ~200 active nodes):

| Mode | FPS (p50) | Atlas upload | Dirty uploads/f | Render/f | Total upload CPU/s (derived) |
|---|---|---|---|---|---|
| partial + active-region-per-tick | 314 | 0.056ms/t, 64 px rows | 0.0 | 0.003ms | ≈1.9ms/s |
| partial-only | 335 | 0.000ms/t, 0 rows | 13.4 | 0.022ms | ≈6.45ms/s |
| partial + full-atlas-per-tick | 351 | 1.623ms/t, 2048 px rows | 0.0 | 0.003ms | dominated by the 1.623ms/t spike, once every tick |

`Logic`/`Write` (simulation + CPU-side redraw) were within noise across all
three (0.68-0.74ms/t, 1.31-1.40ms/t), confirming the remaining differences
are attributable to upload strategy, not simulation cost.

Partial + full-atlas-per-tick was visibly the worst on the frametime graph despite
comparable/higher raw FPS — a large periodic 1.6ms spike every single tick
(20/s) reads as a much more jarring, regular sawtooth than the same total
work spread differently, which the FPS percentile summary alone doesn't
capture.

Partial-only's `Atlas upload: 0.000ms/t` is misleading in isolation: the
cost didn't disappear, it moved entirely into `uploadDirty()`'s per-node
path, doing ~13 separate small GPU calls *every frame* (≈13.4/f x 335fps ≈
4500 individual `writeToTexture`+`copyRect` calls/sec) instead of once per
tick — visible directly as `Render` time being ~7x higher than under
partial + active-region-per-tick. Each small call pays its own fixed CPU-side
overhead (format/mip/bounds validation, driver submission), and paying that
overhead ~13x per frame loses to paying it once per tick even though the
tick call moves more bytes.

**Conclusion:** partial + active-region-per-tick had the lowest total measured
upload-related CPU time per second and the smoothest frametime graph of the
three, so it's the only strategy kept — the other two were removed rather
than left as configurable alternatives.
