# Wake-under-boat occlusion — investigation status (2026-07-24)

## TL;DR for the next session

**Both known issues from this session now have code in place. What's left is
in-game verification of the occlusion fix (below) — I (the assistant) cannot
run the game myself.**

1. **Iris pipeline classification (shader-pack incompatibility) — fixed and
   user-confirmed working.** 3 shader packs completely broke under the wake
   render pipeline; fixed via `IrisApi.assignPipeline()`, see "2026-07-24:
   Iris pipeline classification" below. Committed (`10a0f01`).

2. **Wake-under-boat occlusion (tick-rate mask vs. frame-rate interpolated
   rendering) — root cause confirmed, fix implemented this session, NOT YET
   TESTED in-game.** See "2026-07-24: root cause" and "2026-07-24: occlusion
   fix implemented (option 1)" below. Short version of the bug:
   `OcclusionZone.from(entity, dims)` reads the entity's raw, tick-committed
   `position()`/`getYRot()`, while the boat's visible mesh renders with
   partial-tick interpolation — so the mask lagged up to one tick's movement
   behind the boat you actually see. The fix adds a second, frame-rate pass
   using `OcclusionZone.fromInterpolated(...)`, narrowed to a cheap per-tick
   candidate set so it doesn't re-test every node every frame. Went with the
   CPU-side approach (not the fragment-shader discard alternative) because
   decompiling Iris found that option would very likely just not execute
   under most popular shader packs — see "2026-07-24: root cause" for that
   research. **Next step: launch the game, reproduce the paddling-boat
   scenario, and check whether wake is still visible under/near a moving
   boat's hull.** Uncommitted — see "Current uncommitted state".

## 2026-07-24: Iris pipeline classification

Direct application of the `IrisApi.assignPipeline()` finding from the
option-2 feasibility research above. `WakesClient.java` now does, inside
`onInitializeClient()` (guarded by `FabricLoader.getInstance().isModLoaded("iris")`,
matching the existing `areShadersEnabled()` pattern):

```java
IrisApi.getInstance().assignPipeline(WAKE_COLOR_PIPELINE, IrisProgram.ENTITIES_TRANSLUCENT);
IrisApi.getInstance().assignPipeline(WAKE_PIPELINE, IrisProgram.ENTITIES);
```

`WAKE_PIPELINE` (the depth-only, `ALPHA_CUTOUT` pass) doesn't have an exact
match — the public `IrisProgram` enum has no cutout-specific entity category
(unlike the larger internal `ShaderKey` enum, which does: `ENTITIES_CUTOUT`,
`ENTITIES_CUTOUT_DIFFUSE`, etc. — not exposed publicly). `ENTITIES` (solid,
depth-writing) is the closest available semantic match. Worth revisiting if
testing shows this specific assignment causes visible issues — the pass
writes no color (`ColorTargetState.WRITE_NONE`) regardless of which shader
pack program ends up bound to it, so the main risk is the pack's own
alpha-test threshold overriding ours and shifting the depth-write boundary,
not a visual color/lighting mismatch.

**What was explicitly *not* changed, after an earlier wrong attempt this
session that reverted too much** (fully rebuilding `WakeRenderer`/
`SplashPlaneRenderer` around `RenderTypes.entityTranslucent(...)` and the old
`AfterTranslucentTerrain`/`BeforeTranslucentTerrain` dual-hook,
camera-submerged design — reverted via `git checkout` after user correction,
no trace left): the two-pass color+depth-cutout system
(`wake_transparency_tradeoffs.md` Method D) and the `COLLECT_SUBMITS`
single-hook, no-submerged-check rendering (part of what commit 9552b32's
message calls "wakes visible through glass and water") are both intentional
and were kept as-is. The only problem was pipeline *classification*, not
pipeline *architecture*.

The SAT-based per-texel occlusion system (`OcclusionZone`, wired through
`WakeHandler`/`WakeChunk`/`WakeNode`) is **provably correct in isolation** — every
formula, every code path, every blend/shader interaction has been independently
re-derived and checked across three sessions now, and independently
recomputed against two real, precisely tick-matched screenshots this session
(0 mismatches both times — see below). The previous
session's leading hypothesis — "exclusion rate drops as boat speed increases" —
**did not survive rigorous testing against `occlusionlog-4.txt`** this session;
see "2026-07-24: the speed hypothesis does not hold up" below. The repo has
debug instrumentation (uncommitted, see "Current uncommitted state") that
captures a screenshot + log pair from the *same* session, tagged with an exact
`tick=`, so a screenshot can be located in the log with certainty instead of
inferred — this is what actually cracked the case this session.

## 2026-07-24: the speed hypothesis does not hold up

`occlusionlog-4.txt` (6136 lines, a full play session, not committed) was
parsed programmatically (script and full methodology below) rather than
eyeballed. Two checks:

1. **Independently recomputed every single `[occlusion-node]` line's
   `texelsMasked` count from scratch**, using only the `[occlusion-entity]`
   zone data logged for that same tick and a straight reimplementation of
   `OcclusionZone.overlapsNode`/`contains` (16×16 texel-center sampling, same
   as `WakeNode.draw()`). Result: **0 mismatches across all 1128 real
   node-draw samples in the log.** Every logged masked count is exactly what
   the formulas predict from that tick's own logged entity data. This is
   strictly stronger than the previous sessions' from-scratch math re-derivation
   — it confirms the *actual live numbers produced during real gameplay* are
   internally consistent, not just that the formulas look right on paper.
2. **Isolated the one actually-moving entity (`id=82`, the player's boat —
   everything else in the test world is ~30 stationary decoy boats at
   `speed=0.0`) and correlated its own speed against its own per-node masked
   ratio** across 679 samples spanning speed 0.22 → 0.40. `Pearson
   r(speed, maskedRatio) = -0.046` — essentially zero linear correlation.
   Ratio stays noisy in the 0.35–0.55 range at every speed bucket; the noise
   is just normal edge-node partial-overlap (a rotating/translating zone
   clips node cells at all sorts of fractions depending on where its boundary
   happens to fall), not a speed-driven trend.

**Conclusion: `occlusionlog-4.txt` does not contain a reproduction of the
visual bug.** The previous session's "New evidence" screenshots
(`2026-07-23_20.3*.png`) are from an earlier capture — none of the four
`occlusionlog*.txt` files in the repo currently carry a timestamp/tick
correlated to those specific screenshots, so it's not actually confirmed
those screenshots and any existing log came from the same moment. That's
what the new screenshot-correlation logging (below) is for: next session,
reproduce the bug, and use the new `tick=` fields to line a screenshot up
with its exact `[occlusion-entity]`/`[occlusion-node]` lines with certainty,
then rerun the same recompute-and-diff methodology on *that* tick
specifically. If it also shows 0 mismatches, the bug is almost certainly not
in the occlusion math at all — look at the render/upload pipeline instead
(texture upload timing, blend state, or the gizmo vs. wake quad drawing at
different times within the frame).

Methodology / script: `analyze_occlusion.py` (written to the session
scratchpad, not committed — recreate if needed). Parses `[occlusion-entity]`
and `[occlusion-node]` lines, groups entity lines into per-tick zone lists
(a new entity line immediately after a node line marks a new tick, matching
`WakeHandler.wakeLogic()`'s "compute all zones, then draw all nodes" order),
reimplements `overlapsNode`/`contains` in Python from the literal formulas in
`OcclusionZone.java`, and diffs against the logged `texelsMasked` count.

## 2026-07-24: root cause — tick-rate mask vs. frame-rate interpolated rendering

Two real screenshots this session were pinned to their exact tick via the new
`[occlusion-screenshot]` log line (see "Diagnostic logging" below) and
independently recomputed the same way as the speed-hypothesis check above:

- `2026-07-24_07.18.36.png` → `tick=1888617`, boat `id=93`. 4 logged nodes,
  all 4 matched the independent recompute exactly (168, 256, 68, 81 out of
  256). A wider grid scan found nodes that geometrically overlap the zone but
  weren't logged — checked their history: they don't exist yet at this tick,
  they get created 2 ticks later via `floodFill()`. Not a broad-phase miss.
- `2026-07-24_07.45.54.png` → `tick=1889520`, boat `id=523`. Same result: 5
  logged nodes all matched exactly (81, 251, 29, 7, 0/256), including two
  near-zero values that are genuinely correct (the rotated rectangle only
  grazes those cells' corners). 6 more nodes geometrically overlap but aren't
  logged — one of them hits 256/256 the very next tick, so it's dramatic-looking,
  but it's the same story: doesn't exist yet at this exact tick, `floodFill()`
  hasn't reached it.

**So the CPU-side computation is exactly correct at the literal instant of
both screenshots.** That's no longer in question. But the screenshots
unambiguously show wake visible under the boat, so something real is still
wrong — it's just not the math. The actual mechanism:

`OcclusionZone.from(entity, dims)` reads `entity.position()` / `entity.getYRot()`
— confirmed via decompiled bytecode to be plain field reads on `Entity`,
updated exactly once per logical tick, never interpolated. This is used for
**both** the debug gizmo (by design — see its own code comment about
deliberately not interpolating, "correct tradeoff for a debug tool") **and**
the real occlusion test in `WakeHandler.computeOcclusionZones()`. Meanwhile,
the boat's *rendered mesh* goes through
`EntityRenderDispatcher.extractEntity(Entity entity, float partialTick)`
(confirmed present in the decompiled client jar) — the standard Minecraft
mechanism that smoothly interpolates every entity's visual transform between
ticks so movement doesn't look like discrete teleports.

The result: the boat's visible hull glides continuously frame-to-frame, while
the occlusion zone (and therefore the mask painted into the wake texture,
since `WakeNode.draw()` — and thus every texel's masked/unmasked state — only
ever runs once per tick, from `WakeChunk.drawWakes()` called out of
`WakeHandler.wakeLogic()` on `ClientTickEvents.EndLevelTick`) stays frozen at
wherever the entity was as of the *last committed tick* and only ever snaps,
never glides. At any point between two ticks — which is most of the time at
any framerate higher than 20 FPS, and still a meaningful fraction of the time
even *at* a 20 FPS cap, because Minecraft's tick/render loop uses a time
accumulator and does not guarantee one fresh tick precedes every rendered
frame — the visible hull has already moved/rotated some fraction of the way
toward the next tick's position while the mask is still testing against
where the boat *was*. At this boat's ~0.4 blocks/tick speed, even a half-tick
gap is enough to expose several texels' worth of wake under the hull. This
was confirmed interactively: capping FPS to 20 made the gizmo/mask lag
*more* perceptible (not less), because each tick-to-tick snap became a larger
fraction of visible time instead of being smoothed across many frames.

**This is a real, confirmed mechanism, not a hand-wave** — it's consistent
with both screenshots' 0-mismatch CPU data, the bytecode-confirmed behavior
of `position()`/`getYRot()`, and the confirmed existence of partial-tick
interpolation in entity rendering, and it directly predicts both symptoms the
user observed (gizmo "gliding" behind the visibly-interpolated boat at low
FPS; wake visible under the actual rendered hull, not just outside the
yellow gizmo).

### Candidate fixes (neither implemented yet — decision pending)

1. **CPU-side, user-proposed**: keep a cheap heuristic (e.g. simple radius
   check) computed per tick to narrow candidate nodes, then run the real SAT
   + per-texel masking on just that candidate set every *frame*, using the
   boat's interpolated (partial-tick) transform instead of raw
   `entity.position()`. Stays entirely inside code already owned/controlled,
   no new shader. Cost: real per-frame CPU work, even if narrowed.
2. **GPU-side, discard in the wake's own fragment shader**: compute the
   interpolated zone (position/yaw lerp only — cheap) once per boat per
   frame, pass as a uniform to the wake render pass, `discard` per-fragment
   in the shader instead of baking a mask into the CPU-side texture. No
   per-frame CPU texel loop at all — the GPU is already rasterizing every
   wake pixel every frame regardless.
   - Requires a genuine custom fragment shader. Checked: today's wake
     pipelines (`WAKE_COLOR_PIPELINE`/`WAKE_PIPELINE`) use Mojang's stock
     `RenderPipelines.ENTITY_SNIPPET` parameterized only via
     `withShaderDefine(...)` (`PER_FACE_LIGHTING`, `ALPHA_CUTOUT`) — no
     hand-written shader source is used for gameplay rendering anywhere in
     the mod today (the only `.fsh` in the repo, `gui_hsv.fsh`, is for the
     config-screen color picker, never touched by Iris/shader packs). This
     would be new, real complexity, not a small addition.
   - **Checked against the actual reason `HULL_MASK_PIPELINE` was abandoned**
     (see the "Dead code" section below — it hid the *player's own legs*
     while riding the boat, because it was a separate boat-hull-shaped
     depth-only pass that wrote a depth value blocking anything drawn
     afterward at that position, including the rider). A `discard`-based
     approach confined to the wake's own fragment shader does not create
     that kind of shared depth obstacle — a discarded wake fragment writes
     neither color nor depth, so it can't occlude unrelated geometry the way
     a dedicated mask pass did. Structurally different failure surface.
   - **Iris/shader-pack risk — checked by decompiling the actual Iris jar
     (`iris-1.11.2+26.2-fabric.jar`), not just docs, 2026-07-24, and this is
     a real structural problem, likely disqualifying.** Iris classifies
     *every* `RenderPipeline` — vanilla or third-party — into one of a fixed
     set of known categories (public API: `IrisApi.assignPipeline(RenderPipeline,
     IrisProgram)`, e.g. `ENTITIES`, `ENTITIES_TRANSLUCENT`, `PARTICLES`; the
     mod doesn't currently call this anywhere, so today's wake pipelines are
     going through Iris's internal fuzzy matcher instead —
     `ShaderKey.findBestMatch()`, which scores pipelines against known
     categories by vertex format/blend/alpha-test and logs "Found perfect/okay/
     fine/decent/couldn't find any match"). **Once classified into a category,
     the active shader pack's own author-written program for that category is
     what actually renders it, if the pack provides one — not our shader
     source.** Confirmed via `TransformPatcher.patchVanilla`/`patchSodium`
     (text-level GLSL patching of vanilla-derived source) and
     `ShaderSynthesizer` (generates a completely new fallback shader from a
     hardcoded GLSL template baked into Iris's own Java code — pulled the
     literal template strings, it's generic position/color/UV0/UV1/UV2/fog,
     nothing mod-aware) as the two fallback paths for when a pack doesn't
     provide its own program. In neither of the three paths (pack's own
     program / Iris-patched vanilla source / Iris-synthesized fallback) does
     our actual fragment shader source get used verbatim. For the common
     case — a shader pack that provides its own entity-cutout/translucent
     program, which is most popular visual packs, since re-lighting entities
     is a headline feature — **our discard logic would very plausibly just
     not execute at all**, and the wake-under-boat bug would likely resurface
     silently, specifically for Iris users. This is a materially different
     and more serious risk than the "shader packs replace vanilla's OWN
     shaders" reasoning that killed Methods F/G/H in
     `wake_transparency_tradeoffs.md` — this isn't about depending on code we
     don't own, it's that Iris's classification model hands off authorship of
     the *entire* fragment shader for a matched category to the pack, with no
     found mechanism for a mod to inject always-run logic on top of a
     pack-supplied program. **This is the deciding factor against option 2**
     unless verified otherwise by actually testing with 2-3 real shader packs
     (something a heavy entity-relighting pack like Complementary/BSL, plus a
     light one) — not something to trust from static analysis alone, but the
     default expectation should be that it breaks. Option 1 (CPU-side) has no
     equivalent risk: the occlusion decision is baked into the texture before
     any shader — pack-supplied, Iris-patched, or ours — ever samples it, so
     it's completely independent of which GLSL ends up drawing the quad.

## 2026-07-24: occlusion fix implemented (option 1) — untested in-game

Implemented the CPU-side approach from "Candidate fixes" above. Deliberately
kept the existing tick-rate pass exactly as it was (still the source of truth
for stationary/settled state) and *added* a second, frame-rate pass on top
for nodes near anything actually moving, rather than restructuring the
tick-rate path itself. Four files changed:

- **`OcclusionZone.java`**: new `fromInterpolated(Entity, OcclusionDimensions,
  float partialTick)` factory, alongside the existing `from(...)`. Uses
  `entity.getPosition(partialTick)` and `entity.getViewYRot(partialTick)` —
  both confirmed via decompiling `Entity.class` to be the same
  `Mth.lerp`/`Mth.rotLerp` between `xo/yo/zo/yRotO` (last tick) and the
  current tick's committed value that vanilla's own entity renderer uses, not
  something hand-rolled. `getViewYRot` in particular already correctly
  short-circuits to the raw value at `partialTick == 1.0` and uses
  `Mth.rotLerp` (wraparound-safe) otherwise — confirmed from its bytecode.

- **`WakeHandler.java`**: `computeOcclusionZones()` (still tick-rate, still
  builds the raw-position zone list exactly as before) now also flags, per
  entity, whether it actually moved/turned this tick
  (`getX() != xo || getY() != yo || getZ() != zo || getYRot() != yRotO` —
  exact comparison against last tick's stored value, not a magnitude
  threshold, since a stationary entity's interpolated and raw positions are
  provably identical regardless of `partialTick`). For entities that moved,
  `markChunksNeedingFrameRefresh()` does a cheap, deliberately-generous AABB
  intersection (`paddedHalfWidth + paddedHalfLength + 1` block reach, ±3
  blocks vertically) against every `WakeChunk.boundingBox` and adds matches
  to `chunksNeedingFrameRefresh` — this is the "cheap per-tick heuristic"
  from the candidate-fixes discussion. It doesn't need to be precise; the
  real SAT/per-texel test still runs in `WakeNode.draw()` regardless, this
  just decides which chunks are worth re-running it on every frame. New
  method `refreshInterpolatedOcclusion(float partialTick)`: no-ops
  immediately if that set is empty (the common case — most wake nodes are
  nowhere near a moving boat most of the time), otherwise builds a fresh
  zone list via `fromInterpolated` for every occluding entity and calls
  `chunk.drawWakes(interpolatedZones)` for each flagged chunk — which is
  just `WakeNode.draw()` again, reusing 100% of the existing narrow/broad
  phase code, no duplicated masking logic.

- **`WakeRenderer.java`**: calls `wakeHandler.refreshInterpolatedOcclusion(partialTick)`
  right before `uploadIfDirty()` each frame, so a redraw lands in the same
  frame it's needed for. `partialTick` from
  `Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)` —
  the standard whole-frame interpolation factor, not `getCameraEntityPartialTicks`
  (which `SplashPlaneRenderer` uses elsewhere in this codebase for a
  different, camera-specific purpose — deliberately not reused here since we
  need the same interpolation factor vanilla uses for entity rendering in
  general, not something camera-entity-specific).

- **`WakeDebugRenderer.java`**: the yellow zone gizmo switched from
  `OcclusionZone.from` to `OcclusionZone.fromInterpolated`, using the same
  `getGameTimeDeltaPartialTick(false)` value. Necessary, not optional — its
  old doc comment argued for deliberately *not* interpolating so it stayed
  "trustworthy" relative to the real (then non-interpolated) test; now that
  the real test interpolates for nodes near motion, a non-interpolated gizmo
  would just reintroduce the exact mismatch this whole investigation was
  about, with the gizmo and the mask's roles swapped.

**What this deliberately does not yet do**: the texture upload this triggers
is still the naive whole-atlas `writeToTexture` (`BetterDynamicTexture.uploadIfDirty()`,
unchanged) — the dirty-rectangle/pooled-scratch-buffer optimization discussed
at length this session (see the performance-cost conversation — not
transcribed into this doc, only the conclusion matters here) was deliberately
deferred until *after* confirming this fix actually works and is worth
keeping. If frame-rate `dirty=true` from this change measurably worsens
frame-time jitter (issue #197 territory), that optimization is the next
planned step, not a sign this approach was wrong.

`OCCLUSION_TEMP_DISABLED` (the flag added earlier this session to test Iris
compatibility with occlusion off) is confirmed fully removed —
`grep -rn OCCLUSION_TEMP_DISABLED src` returns nothing, `./gradlew compileJava`
is clean.

## New evidence: exclusion rate is speed-correlated

To settle "is the gizmo lying to us" (previous session's open question), the
masked branch in `WakeNode.draw()` was changed from writing transparent
(`color &= 0x00FFFFFF`) to writing **opaque magenta** (`color = 0xFFFF00FF`) —
see `src/main/java/com/goby56/wakes/simulation/WakeNode.java` around line 89.
This makes the exclusion test's real, per-tick, per-texel result directly
visible, instead of "invisible" being ambiguous between "correctly excluded"
and "test never fired."

Screenshots (in `run/screenshots/`, links relative to repo root):

- [`2026-07-23_20.34.58.png`](run/screenshots/2026-07-23_20.34.58.png) — boat
  near-stationary (no visible paddle animation/bow spray). The magenta fills
  almost the entire yellow gizmo — the exclusion test is firing correctly for
  nearly every texel inside the zone.
- [`2026-07-23_20.34.26.png`](run/screenshots/2026-07-23_20.34.26.png) and
  [`2026-07-23_20.34.41.png`](run/screenshots/2026-07-23_20.34.41.png) — boat
  actively paddling (visible bow spray/side wake). Only a small, roughly
  centered sub-rectangle of the gizmo is magenta; most of the interior —
  including areas well inside the yellow boundary — is still showing normal
  (unmasked) wake color.

There are also three more recent screenshots
(`2026-07-23_20.36.49.png`, `2026-07-23_20.37.06.png`, `2026-07-23_20.37.32.png`)
in the same folder that were captured but not yet discussed/interpreted —
worth checking before starting new tests, in case they contain more of this
sequence.

**Read carefully:** the comparison in these screenshots is *wake color vs. the
yellow gizmo rectangle*, not *wake color vs. the boat's visual mesh*. The
gizmo and the real occlusion test both come from the exact same
`OcclusionZone` object (`OcclusionZone.from(entity, dims)`, no interpolation —
see `WakeDebugRenderer.addOcclusionZoneGizmos`/`OcclusionZone.java`), so this
is not the render-height/parallax question from earlier — that's a separate,
already-settled matter (see below). This is telling us that **inside a single,
static, already-verified-correct rectangle, only some of the texels are
actually being caught by `contains()`**, and the fraction caught goes down as
the boat's speed goes up.

## Ruled out this session

1. **Y-height mismatch between gizmo and real wake quad (parallax theory).**
   The previous session ended mid-investigation of whether
   `WakeDebugRenderer`'s gizmo height (`Math.floor(wakeHeight) + WATER_OFFSET`)
   matched `WakeRenderer`'s actual quad height (which also adds `surfaceBias`,
   a small epsilon plus an optional shader-compat offset). The user confirmed
   this is **not** the issue ("it is definitely NOT a visual mirage") —
   confirmed by viewing straight down, which removes any camera-angle-driven
   parallax explanation. Do not re-open this without new evidence.

2. **Two-pass color/depth interaction "eating" a zeroed-alpha texel.**
   Decompiled `com.mojang.blaze3d.pipeline.BlendFunction` (from
   `minecraft-client.jar` for `fabric-loom/26.2`): `TRANSLUCENT` is
   `(SRC_ALPHA, ONE_MINUS_SRC_ALPHA)` for color and
   `(ONE, ONE_MINUS_SRC_ALPHA)` for alpha — i.e. standard **non-premultiplied**
   straight-alpha blending (there's a separate, unused
   `TRANSLUCENT_PREMULTIPLIED_ALPHA` constant that would have made this a real
   bug if we'd been using it). With `color.a == 0`, pass 1's blend
   mathematically contributes exactly zero regardless of what's left in the
   RGB channels, and pass 2's `ALPHA_CUTOUT` (0.8) discards it before the
   depth-write stage. So masking-by-zeroing-alpha is provably equivalent to
   "not drawn at all" for both passes — this pathway cannot be the bug.
   (This is what prompted the user's question about the two passes
   interfering, and about skipping the draw call entirely — see next point.)

3. **"Just don't call `drawContext.draw()` for masked texels" as a distinct
   fix/test from zeroing alpha.** `WakeNode.draw()` already unconditionally
   redraws every texel every tick regardless of masking state (there is no
   accumulation — a texel transitions cleanly between masked/unmasked each
   tick). Skipping the write instead of zeroing would only differ from the
   current behavior for a single transient frame (the instant a texel first
   becomes covered); after that both approaches converge to the same steady
   state. Since the symptom is a *persistent*, not flickering, artifact,
   this distinction isn't diagnostic — which is why the magenta-swap test
   (point above) was done instead.

4. **UV/texel-index mapping mismatch between `WakeTextureAtlas.DrawContext`
   and `WakeRenderer`'s quad.** Traced the full mapping: `DrawContext.draw(x,
   y, color)` writes to `globX = x + column*nodeRes`, `globY = y +
   row*nodeRes`; `WakeRenderer`'s quad maps `(node.x, node.z)` (world min
   corner) to `(u0, v0)` and `(node.x+1, node.z+1)` to `(u1, v1)`, with vertex
   order such that world-Z-increasing maps to v-increasing and
   world-X-increasing maps to u-increasing — consistent with `x`/`y`
   index-increasing mapping to `globX`/`globY`-increasing. No flip/transpose.

5. **SAT broad-phase (`overlapsNode`) and narrow-phase (`contains`) math.**
   Independently re-derived both from scratch (again) this session:
   axis-1/2 world-aligned AABB-of-OBB formula, and axis-3/4 corner-projection
   test, both algebraically consistent with the rotation convention used by
   `contains()` and by `WakeDebugRenderer`'s corner-rotation math. No sign
   error, no axis-swap. A false-negative in the broad phase (silently
   skipping a node that genuinely overlaps) is not possible given this is a
   complete, standard 2-rectangle SAT (only 4 candidate axes exist for two
   rectangles, and all 4 are tested).

6. **Stale/partial GPU texture upload.** `BetterDynamicTexture.uploadIfDirty()`
   does a full `writeToTexture` of the whole `NativeImage` any time `dirty`
   is set, and `dirty` is set on every single `DrawContext.draw()` call. Since
   every texel of every node is rewritten every tick, `dirty` is essentially
   always true whenever any node exists, so the GPU texture is never more
   than one frame behind the CPU-side paint. `WakeRenderer.submit()` calls
   `uploadIfDirty()` unconditionally before submitting geometry, every frame.

7. **`Entity.getPosition(float partialTick)` routing through the new
   `InterpolationHandler` and causing a multi-tick-lagged render position for
   the boat.** This was a real, newly-considered architecture question this
   session (MC 26.2 gave every `Entity` its own `InterpolationHandler`, a
   bigger change than the old remote-entity-only smoothing). Decompiled
   `Entity.getPosition(float)`: it is still the classic
   `Mth.lerp(partialTick, xo, getX())` / same for Y/Z — a plain single-tick
   lerp between last tick's position and this tick's position, converging
   exactly to `entity.position()` at `partialTick == 1`. It does **not** route
   through `InterpolationHandler.position()`. Also decompiled
   `AbstractBoat.tick()`: it calls `this.interpolation.interpolate()`
   unconditionally, but for a **locally-authoritative** boat (i.e. the one
   you're riding yourself, `isLocalInstanceAuthoritative() == true`),
   movement is applied directly via `move(MoverType.SELF,
   getDeltaMovement())` in the same tick — the interpolation handler's
   target-chasing behavior is for *remote* entities receiving periodic
   network position corrections, not for your own vehicle. So this doesn't
   explain a persistent, speed-scaled lag for the boat you're actually
   sitting in.

8. **Yaw/position temporal phase mismatch within a single tick** (i.e. is
   `getYRot()` from before this tick's turn input, while `position()` is
   from after it, or vice versa — which would misalign the zone's rotation
   relative to where the boat actually ends up this tick). Decompiled
   `AbstractBoat.tick()`'s call order: `floatBoat()` → `controlBoat()` (which
   internally calls `setYRot()` based on turning input) → `move(MoverType.SELF,
   getDeltaMovement())`. Yaw is fully updated *before* the positional move is
   applied, and both are settled well before `ClientTickEvents.END_LEVEL_TICK`
   (where `WakeHandler.computeOcclusionZones()` reads them) fires. No
   phase mismatch.

## Dead code noticed in passing (not the bug, but should be cleaned up)

`WakesClient.java` lines ~112-138 still define `HULL_MASK_PIPELINE` /
`HULL_MASK_RENDER_TYPE` from the earlier, rejected depth-mask approach. They're
registered (so they exist as valid pipelines) but never submitted anywhere —
confirmed via a full-repo grep for `HULL_MASK`. Harmless, but delete once this
investigation is closed out.

**Why it was rejected (2026-07-24, confirmed by the user, not previously
recorded here):** it was a boat-hull-shaped depth-only pass, submitted before
the wake color pass, that wrote a depth value for every fragment inside the
hull's footprint to block wake quads spawned underneath. The depth write
doesn't know or care what it's blocking — it also occluded the **player's own
legs** while riding the boat (rendered at/under the hull), since anything
drawn afterward at that screen position/depth loses the depth test the same
way a wake quad would. This is relevant precedent for any future GPU-side
occlusion idea: a shared depth-writing mask pass is the specific thing that
broke, not "shaders" or "GPU-side masking" in general. A `discard`-based
approach scoped to the wake's own fragment shader (never writing a separate
depth obstacle other geometry has to contend with) would not have this
failure mode — see the 2026-07-24 discussion on moving occlusion into the
wake shader, below.

## Leading hypotheses for next session, ranked

None of these are confirmed — they're what's left standing after the above
eliminations, ranked by how well they fit "exclusion rate degrades with
speed."

1. **Tick-rate multiplicity / catch-up ticks.** If the client ever runs more
   than one game tick before the next rendered frame (e.g. under load, or
   just as a normal artifact of a 20 TPS logical rate vs uncapped FPS), each
   `wakeLogic()` call is internally self-consistent (fresh zone → same-tick
   redraw), so this *shouldn't* cause staleness — but this reasoning hasn't
   been empirically checked against an actual tick counter yet. The new
   `[occlusion-entity]` log (see below) doesn't currently include a tick
   number — consider adding `world.getGameTime()` to it so consecutive log
   lines can be checked for gaps or multiple lines per rendered frame.

2. **Something velocity-dependent in how many/which nodes exist under the
   boat at a given instant**, independent of the zone/test being correct.
   Paddling hard continuously creates new splash-trail nodes and drives
   `floodFill()` to spread ripples outward every tick once
   `age > WakesConfig.floodFillTickDelay`. A fast-moving, actively-paddling
   boat has a much larger and more actively-churning set of nearby nodes than
   a stationary one. This wouldn't explain texels *failing* `contains()`
   inside a correct zone by itself, but combined with the per-node log added
   this session, it's now possible to check directly whether the nodes that
   show a low `texelsMasked/total` ratio are specifically ones that were just
   inserted this tick or last tick (i.e. very "young") vs. long-established
   trail nodes.

3. **Something in `entitiesForRendering()` returning a position snapshot from
   a slightly different point in the frame than assumed** — not yet checked
   against source (only used it via API, didn't decompile its actual
   iteration/snapshot semantics this session). Worth 10 minutes next session:
   confirm it's a live view over the level's entity list, not a
   render-thread-cached copy from an earlier point (e.g. from
   `LevelExtractionEvents.END_EXTRACTION`, which `FrustumManager` already
   hooks into elsewhere in this codebase — if `entitiesForRendering()` is
   itself extraction-phase-scoped rather than a direct level query, it could
   plausibly be one step behind if extraction happens before the final
   physics step for a given tick, though `WakeHandler.tick()` is on
   `ClientTickEvents.END_LEVEL_TICK` which should be safely after all of
   that).

## Diagnostic logging added this session (uncommitted)

Three log lines, all tagged for easy `grep`/filtering and meant to be read
*together*. As of 2026-07-24 all three now share an explicit `tick=` field
(`level.getGameTime()`/`world.getGameTime()`), so lines can be joined exactly
instead of by eyeballed wall-clock proximity:

- `WakeHandler.computeOcclusionZones()` — one `[occlusion-entity]` line per
  occluding entity, per tick: `tick`, entity id, `zone.x()/z()`,
  `entity.getYRot()`, horizontal speed
  (`entity.getDeltaMovement().horizontalDistance()`), and the zone's padded
  half-width/half-length.
- `WakeNode.draw()` — one `[occlusion-node]` line per node per tick, but
  **only when `nearbyZones != null`** (i.e. only nodes the broad phase
  actually flagged as near an occluding zone this tick), giving `tick`, node
  `(x, z)`, how many zones were nearby, and `texelsMasked/total` for that
  node this tick.
- `ScreenshotLogMixin` (new 2026-07-24, injects into
  `net.minecraft.client.Screenshot.getFile`) — one `[occlusion-screenshot]`
  line per screenshot taken (F2 or `/screenshot`), giving the exact final
  filename (post disambiguation-counter — this is the literal name the file
  gets saved as) and the `tick` it was taken on. This is what makes it
  possible to take a screenshot of the bug and immediately grep the log for
  `tick=<that value>` to get the exact zone/node data behind what's on
  screen, instead of the previous approach of matching wall-clock seconds
  (which is how we ended up unsure whether `occlusionlog-4.txt` actually
  corresponds to the 2026-07-23 screenshots at all — see the section above).

Suggested next step: reproduce the bug with these logs running, screenshot
it, grep `[occlusion-screenshot]` for that file's `tick=`, then grep both
other tags for the same `tick=` value. For a specific node whose
`texelsMasked` ratio looks wrong given how deep inside the yellow gizmo it
visually appears, recompute `zone.contains()` for that node's texel world
coordinates by hand (or reuse `analyze_occlusion.py`'s approach) using the
`[occlusion-entity]` line from that *exact* tick, to see whether the stored
zone parameters actually produce the observed masked/unmasked split, or
whether the *math* is right but something is feeding it a zone that doesn't
match what the gizmo displayed for that same moment. Given this session's
finding that `occlusionlog-4.txt` showed zero such mismatches, don't assume
the bug is in this math path — if the new tick-matched data also comes back
clean, pivot to the render/upload pipeline (texture upload timing relative
to the gizmo draw call, blend state) instead of re-deriving the SAT formulas
a fourth time.

## Current uncommitted state (do not commit — left for next session to inspect)

- `src/main/java/com/goby56/wakes/simulation/WakeNode.java`:
  - masked-texel color is `0xFFFF00FF` (opaque magenta) instead of
    `color & 0x00FFFFFF` — **revert this to the alpha-zeroing behavior before
    shipping**, it's purely a visualization aid.
  - `[occlusion-node]` debug log added, now with `tick=` (see above).
- `src/main/java/com/goby56/wakes/simulation/WakeHandler.java`:
  - `[occlusion-entity]` debug log added inside `computeOcclusionZones()`,
    now with `tick=` (see above).
- `src/main/java/com/goby56/wakes/mixin/ScreenshotLogMixin.java` (new file)
  and its registration in `src/main/resources/wakes.mixins.json`:
  - `[occlusion-screenshot]` debug log (see above). Safe to keep or delete
    independently of the other two — it has no interaction with the
    occlusion system itself, it just reads `Screenshot.getFile`'s return
    value.

Everything else (the full SAT occlusion system: `OcclusionZone`,
`OcclusionDimensions`/`OcclusionDimensionsManager`, the `occludes_wake` entity
tag, the `WakeSpawnerMixin` wiring, `WakeDebugRenderer`'s gizmos) is already
committed as of `764a008 "occlusion zones"` and is not touched by the above.

Build verified compiling cleanly (`./gradlew compileJava`) after this
session's changes (tick fields + `ScreenshotLogMixin`).

Build was verified compiling cleanly (`./gradlew compileJava`) after both
logging additions.
