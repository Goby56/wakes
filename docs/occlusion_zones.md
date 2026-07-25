# Wake occlusion (hiding wake texels under boat hulls)

## Current state

When a boat sits on top of a wake, the texels directly under its hull are
masked out (alpha zeroed) instead of drawing through the hull. This is done
entirely CPU-side, baked into the wake texture atlas before any shader ever
samples it — there is no GPU-side discard/depth-mask involved.

### Core pieces

- **`OcclusionZone`** (record): a snapshot of one occluding entity's
  world-space position, yaw (as `cos`/`sin`), padded half-width/half-length,
  and `y` — the wake node grid layer it applies to. Two factories:
  - `from(entity, dims, wakeHeight)` — raw, tick-committed `entity.position()`/
    `getYRot()`.
  - `fromInterpolated(entity, dims, wakeHeight, partialTick)` — same, but
    through `entity.getPosition(partialTick)`/`getViewYRot(partialTick)`
    (the same `Mth.lerp`/`Mth.rotLerp` vanilla uses to smoothly interpolate
    entity rendering between ticks).
  - `overlapsNode(nodeX, nodeY, nodeZ)` — broad phase: SAT test between the
    zone's oriented rectangle and a node's axis-aligned 1x1 footprint, with an
    early exit if `nodeY != y`.
  - `contains(worldX, worldZ)` — narrow phase: exact point-in-oriented-
    rectangle test, only called for zones a node's broad phase already
    flagged as nearby.
- **`WakeHandler.computeOcclusionZones()`** — tick-rate (every simulation
  tick): builds the zone list from every `ProducesWake` entity with a
  resolved `OcclusionDimensions` and non-null `wakeHeight` (i.e. it's actually
  an occluding entity type — see `occludes_wake` tag below — and it's
  currently on a fluid surface). Also flags, per entity, whether it moved or
  turned this tick, and for movers, does a cheap AABB reach-check
  (`markChunksNeedingFrameRefresh()`) against every `WakeChunk`'s bounding box
  to build a small candidate set of chunks worth refreshing every frame.
- **`WakeHandler.refreshInterpolatedOcclusion(partialTick)`** — frame-rate:
  no-ops immediately if the candidate set from above is empty (the common
  case — most nodes are nowhere near a moving occluder most of the time).
  Otherwise builds a fresh `fromInterpolated` zone list and redraws just the
  flagged chunks, so the mask tracks the occluder's true interpolated render
  position instead of sitting frozen at wherever it was as of the last tick.
- **`WakeDebugRenderer`** — `drawOcclusionZones` config option draws the
  zone rectangles (yellow, from `fromInterpolated`) and red wireframes around
  every node a zone currently overlaps, for visual debugging.
- **`occludes_wake` entity tag** (`data/wakes/tags/entity_type/occludes_wake.json`)
  — currently boats + chest boats only, deliberately excluding rafts (they
  sit flush on the surface, no hull dip to hide anything under).

## Giving an entity (vanilla or modded) an occlusion zone

Two independent, datapack-only steps — no mixins or code changes needed on
the entity's side.

### 1. Opt in via the `occludes_wake` tag

An entity type only gets an occlusion zone at all if it's a member of
`#wakes:occludes_wake`. `WakeSpawnerMixin` checks this tag before ever
resolving `OcclusionDimensions` for an entity — untagged entities are never
occluded, regardless of step 2.

To add a modded (or currently-excluded vanilla) entity, ship a datapack that
extends the tag:

```json
// data/wakes/tags/entity_type/occludes_wake.json
{
  "replace": false,
  "values": [
    "mymod:river_barge"
  ]
}
```

`"replace": false` merges with the existing vanilla list instead of
overwriting it.

### 2. Override the occlusion rectangle's dimensions (optional)

Once tagged, an entity automatically gets `OcclusionDimensions.DEFAULT_BOAT`
— a 1.2 (width) x 1.8 (length) block rectangle, matching a vanilla boat's
hull. If that's the wrong size for the entity (a modded ship, a much smaller
raft-like entity, etc.), add an override file at:

```
data/<namespace>/wakes_occlusion_dimensions/<path>.json
```

The file's own resource location — `<namespace>:<path>` — must exactly match
the target entity's registry id (`EntityType.getKey(...)`). There is no
"entity" field inside the JSON; the file's location *is* the id it overrides.
For example, to size the zone for `mymod:river_barge`:

```json
// data/mymod/wakes_occlusion_dimensions/river_barge.json
{
  "width": 2.5,
  "length": 5.0
}
```

Both fields are floats in blocks:
- **`width`** — full extent along the entity's local width axis (perpendicular
  to its facing direction).
- **`length`** — full extent along the entity's local length axis (the
  direction it faces).

The rectangle is centered on the entity's world XZ position, oriented by its
yaw, and applied at the wake's surface Y layer (not the entity's own Y) — see
`OcclusionZone.from`/`fromInterpolated` above. `OcclusionDimensionsManager`
reloads these overrides on every datapack/resource reload, same as any other
`SimpleJsonResourceReloadListener` data.

## Why it's built this way

### Two passes (tick-rate + frame-rate), not one

Originally, occlusion was computed and baked into the texture once per
simulation tick only. This looked correct in isolation — independently
re-deriving the SAT math and recomputing masked-texel counts from logged data
found zero discrepancies — but was still visibly wrong in-game: wake showed
through under the actual rendered hull, worse at low FPS.

Root cause: `entity.position()`/`getYRot()` are plain per-tick field reads,
never interpolated, but the boat's *rendered* mesh goes through vanilla's
partial-tick interpolation every frame. Since the mask was baked from the
tick-committed transform while the hull glides continuously between ticks,
the mask permanently lagged up to a tick's worth of movement behind what was
on screen. Confirmed interactively: capping FPS to 20 made the lag *more*
perceptible (each snap became a larger fraction of visible time), which is
the opposite of what a rendering-side artifact would do and consistent with
a tick-vs-frame data source mismatch.

Fixed by keeping the tick-rate pass as the source of truth (cheap, correct
for anything stationary) and adding a second, frame-rate pass scoped only to
chunks near something that actually moved this tick — narrowed by a cheap
AABB heuristic first, since re-testing every node every frame regardless of
whether anything nearby moved would be wasteful.

### A GPU-side (fragment shader discard) approach was considered and rejected

An alternative: compute the interpolated zone once per boat per frame, pass
it as a uniform, and `discard` per-fragment in the wake's own shader — no
per-frame CPU texel loop at all. Rejected after decompiling Iris
(`iris-1.11.2+26.2-fabric.jar`, not just documentation): Iris classifies every
`RenderPipeline` into a fixed category and, once classified, the active
shader pack's *own* author-written program for that category renders it if
the pack provides one — not our shader source. For any shader pack that
supplies its own entity-cutout/translucent program (most popular visual
packs, since re-lighting entities is a headline feature), a discard baked
into our own GLSL would very plausibly just never execute. The CPU-side
approach has no equivalent risk — the occlusion decision is already baked
into the texture before any shader (pack-supplied, Iris-patched, or ours)
samples it.

### Y-awareness was missing entirely at first

`OcclusionZone` originally had no `y` field at all — `overlapsNode`/
`contains` were pure X/Z tests. This worked as long as an occluding entity's
own Y and its wake's surface Y were close together, but broke for a boat sunk
to the ocean floor: its wake still surfaces above it, but the zone (built
from the boat's own low Y) had no way to avoid spuriously matching wake nodes
stacked directly above at a completely unrelated Y, purely by X/Z coincidence
— visible in-game as red debug wireframes appearing at the water surface far
above a sunken boat. Fixed by deriving the zone's `y` from `wakeHeight` (the
wake's actual surface, not `entity.getY()`) and adding an early Y-equality
check to `overlapsNode`. The same bug existed in miniature in
`markChunksNeedingFrameRefresh()`'s reach AABB (centered on `entity.getY()`
instead of `wakeHeight`) and was fixed the same way.

### An even earlier GPU-side approach (hull-shaped depth mask) was tried and reverted

Before any of the above, occlusion was attempted via a dedicated boat-hull-
shaped depth-only render pass (`HULL_MASK_PIPELINE`, since removed) submitted
before the wake color pass, writing a depth value under the hull to block
wake quads from being drawn there. Abandoned because a depth write doesn't
know or care what it blocks — it also occluded the player's own legs while
riding the boat, since anything drawn afterward at that screen position/depth
loses the depth test the same way a wake quad would. This is why the later
GPU-discard alternative above was evaluated specifically as a fragment
`discard` (which writes neither color nor depth, so can't create a shared
depth obstacle) rather than a separate mask pass — and why it was still
rejected on Iris-compatibility grounds independent of that specific failure
mode.
