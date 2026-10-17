# Wake transparency vs. depth occlusion

## Current state

Wake/foam quads render in two passes (Method D below):

1. **Color pass** — full alpha range, translucent blend, no depth write.
   Every visible pixel's color comes from this pass alone.
2. **Depth pass** — same geometry/texture, `ALPHA_CUTOUT` (only pixels above
   the cutoff survive), depth write on, `ColorTargetState.WRITE_NONE` (no
   color output at all).

This makes faint wake stay translucent (pass 1) while fully-opaque foam still
occludes water drawn after it (pass 2's depth write), without the double-
blend color seam an earlier version of this had (see Method C → D below).
`wakeOpacity` is applied per-draw via vertex-color alpha, not baked into the
texture, since it's a runtime-changeable config value and shader defines are
fixed at pipeline registration time.

This only describes the **"Improved Transparency" off** path (and Iris, which
forces that setting off). With it on, 26.3 uses order-independent
transparency: the color pass draws through `WAKE_OIT_PIPELINES` and the depth
pass is skipped (`WakesClient.useDepthPass()`); see "Improved Transparency
(OIT)" below.

**Sea floor showing through wake (fixed for shaders).** Water is depth-rejected
wherever pass 2 wrote depth, so pass 1's color there blends over the opaque
scene (the sea floor) instead of over water.

- **Vanilla:** pass 2 writes depth above alpha 0.8, so only the 0-20% that
  pass 1 lets through at alpha 0.8-1.0 shows the sea floor. Barely visible.
- **Iris/shader packs:** our `ALPHA_CUTOUT 0.8` never reaches the GPU. It is
  a define for Mojang's `entity.fsh`, which the pack's own program replaces.
  `assignPipeline(WAKE_PIPELINE, ENTITIES)` resolves through
  `ShaderKey.findBestMatch`, which only checks that `ALPHA_CUTOUT` *exists*
  and picks `ENTITIES_CUTOUT` (`AlphaTests.ONE_TENTH_ALPHA`, alpha > 0.1).
  Pass 2 therefore writes depth above alpha 0.1, water is removed under all
  of it, and the whole 10-80% band of faint wake shows the sea floor.
  Found in Iris 1.11.7+26.3 bytecode, confirmed by the fix below working.

An earlier hypothesis (the pack's water/SSR logic treating any translucent
fragment as occluding water) is superseded by the above.

Fix for shaders (confirmed in-game under a shader pack, no visible change in
vanilla): pass 2 writes no
color, so its alpha only feeds the cutoff test. Pass 2 emits a constant vertex
alpha of `WAKE_DEPTH_PASS_ALPHA = 0.1 / 0.8 = 0.125` (pass 1 keeps
`wakeOpacity`), so Iris's `alpha > 0.1` test passes exactly where the texture
alpha is above 0.8. Vanilla's `ALPHA_CUTOUT` tests texture alpha before the
vertex color multiply, so it is unaffected. Relies
on the pack multiplying texture alpha by vertex color (most do) and not
adding a stricter alpha test of its own, so a pack that still shows the sea
floor most likely breaks one of those. Not verified directly: whether Iris
keeps pass 2's `WRITE_NONE` mask on the pack's MRT outputs (if not, pass 2
would also draw a faint 12.5% color layer).

There is no version of this that is simultaneously one pass, perfectly
smooth, and fully correct occlusion — see "why" below for what was tried and
ruled out, and why.

## Why: the constraint

GPU depth testing is fixed-function and binary. A `RenderPipeline`'s
`DepthStencilState.writeDepth` flag is one boolean for the entire draw call —
there's no hardware concept of "70% occluded," only "does" or "doesn't" write
depth, per fragment, per draw. Wake opacity is continuous; occlusion needs to
track it smoothly. Every approach below is a different way of forcing a
continuous value through that binary gate, and every one gives something up
to do it — that tradeoff is the actual constraint, not a bug still waiting
for a fix.

The only two escape hatches from "binary per-draw":
- **`discard` per fragment** — genuinely per-pixel, but deletes the
  fragment's color output along with its depth write; can't keep one without
  the other from a single invocation.
- **Dithering** — trades the hard boundary for spatially-scrambled noise, so
  it isn't perceived as a sharp edge. Still binary per-pixel underneath.

(A third, more exotic option — fragment shader interlock / ROVs — exists and
is covered below, but doesn't actually get further than the shader-rewrite
cost of Method F.)

Order-independent transparency sidesteps the constraint instead of escaping
it. Method A only failed because water drawn *after* opaque foam blends over
it, which is a draw-order problem the depth write was faking a fix for. OIT
removes draw order from blending and gives continuous, alpha-proportional
occlusion. See "Improved Transparency (OIT)" below.

## Why: what was tried, in order

**Method A — single translucent pass, no depth write.** Cheapest, perfectly
smooth alpha, but never occludes anything — water can draw straight over
100%-opaque foam. Solves "faint stays translucent," not "opaque occludes."

**Method B — single `ALPHA_CUTOUT` pass, depth write on.** Correct occlusion
above the cutoff (and genuinely per-pixel — the shader samples interpolated
alpha at that exact fragment). But everything below the cutoff doesn't exist
at all — no color, not even faint — so a fading wake node's visible pixels
binary-flip off in one frame instead of fading out. Solves "opaque occludes,"
not "faint stays translucent."

**Method C — two passes, both writing color.** Pass 1 = Method A, pass 2 =
Method B, same geometry. Solves both goals, but every pixel above the cutoff
gets blended twice, producing a visible color/brightness seam exactly at the
cutoff boundary on top of doubling CPU/fragment cost.

**Method D — two passes, second pass depth-only (current state).** Same as C
but pass 2 uses `WRITE_NONE`: still discards/depth-tests/depth-writes
identically, just never touches the color attachment. Removes the seam (every
visible pixel's color now comes from exactly one blend) without reducing the
fundamental two-full-geometry-submissions cost, and the underlying hard
occlusion edge (whether water can draw over a pixel flips at exactly
`alpha == cutoff`) is unchanged — just no longer paired with a color jump.

**Method D under shaders — prototyped alternative, reverted.** To work around
the Iris sea-floor caveat above, tried a third pipeline used only when shaders
are active — effectively Method B (single `ALPHA_CUTOUT` pass, real color
output, depth write) replacing both passes, so there's no near-zero-alpha
translucent fragment for a shader pack to misinterpret. Reverted to keep the
original two-pass split active under shaders too, pending further
investigation — not committed as the shipped behavior. In hindsight this
could not have fixed it: its `ALPHA_CUTOUT` would also have been replaced by
Iris's 0.1 alpha test (see the sea floor caveat above).

**Method E — dithered/stochastic cutout (not implemented).** Custom fragment
shader comparing alpha against a spatial dither pattern (e.g. 4x4 Bayer
matrix) instead of one flat constant, so an increasing *fraction* of pixels
in a region pass as alpha rises, turning the hard edge into a stippled
transition band. Would require writing and maintaining a custom fragment
shader (today everything uses Mojang's stock `entity.fsh` via shader defines
only); without TAA the pattern is static per-frame (reads as noise, not
blur). Doesn't reduce pass count/cost — only changes where the edge falls,
pixel by pixel. Not pursued.

**Method F — move the mask onto water's own shader (not implemented).**
Render wake in one translucent pass only, patch water's fragment shader to
sample the wake alpha texture and discard/blend itself out under high-alpha
wake. Would require mixing into vanilla's fluid render pipeline — code this
project doesn't own, and something already treated carefully around
Iris/shader-pack compatibility (shader packs replace core water shaders
wholesale, silently breaking this hook under most already-compatible packs).
Not pursued.

**Method G — manual depth-buffer writes via compute/image store (not
implemented).** The real depth buffer vanilla rendering (water, terrain,
entities) tests against is still the fixed-function one; writing to a side
texture doesn't make water respect it unless water is also rewritten to
consult it — collapses into Method F's problem at higher implementation cost.
Not pursued.

**Method H — fragment shader interlock / ROVs (theoretical).** Modern APIs
(D3D ROVs, `GL_ARB_fragment_shader_interlock`, Vulkan equivalent) give a
fragment shader a genuine per-pixel conditional commit instead of a binary
discard. Confirms the "binary per-draw" constraint is a real fixed-function
hardware limitation, not a made-up one — but it only lets a shader
read/write a buffer *it* owns. Making water actually respect that buffer
still requires rewriting water's shader (Method F's cost), plus real
serialization overhead and inconsistent driver/API support. Doesn't add a
cheaper option; confirms Methods F/G's cost is the actual floor.

## Improved Transparency (OIT), 26.3+

In 26.3 the "Improved Transparency" video setting no longer means Fabulous's
per-category render targets. It enables order-independent transparency.
Findings below are from the 26.3 client jar (shaders under
`assets/minecraft/shaders/include/oit*.glsl`, Java under
`net.minecraft.client.renderer.oit`).

### The crash that forced this (fixed)

Before Method I, with the setting on, `SubmitNodeCollection` aliases `translucentCustomGeometry`
(and every other translucent phase) to a single `oitTranslucent` phase.
`WAKE_COLOR_RENDER_TYPE` has a blend function, so `submitCustomGeometry`
routes it there (splash planes share it). The OIT phase draws through
`PreparedRenderType.drawFromBufferOit`, which throws
`IllegalStateException: Render type wakes:entity_translucent_wake_color does
not have OIT pipelines set up.` when the render type has no `OitPipelineSet`.
The depth-only pass has no blend, so it stays in the solid phase and is
unaffected. Confirmed in-game (crash in `LevelRenderer.executeOit`).

Iris forces the setting off while shaders are enabled
(`MixinDisableFabulousGraphics`), so this only affects vanilla rendering.

### How vanilla OIT works

Wavelet OIT (`OIT_WAVELET_RANK 2`). Render types opt in via
`RenderSetup.builder(...).setOitPipelines(OitPipelineSet)`; a set holds one
pipeline per `OitStage`, none of which write depth.

1. **Depth bounds:** per pixel, nearest/farthest translucent depth, plus the
   nearest translucent fragment with alpha > 0.99 (`OIT_FULLY_OPAQUE_ALPHA`).
2. **Depth bounds cull:** writes that near-opaque depth into the main depth
   buffer, so later stages depth-reject everything behind it.
3. **Transmittance:** each fragment adds absorbance (`-log(1 - alpha)`) at
   its depth, encoded as wavelet coefficients. Depth is normalized per pixel
   between the nearest and farthest bound, so nearly coplanar layers (wake
   just above water) still land in different bins.
4. **Accumulate + composite:** each fragment's color is weighted by the
   transmittance in front of it, then resolved onto the main target.

### Method I: single OIT pass (current state with the setting on)

OIT gives both goals without Method D's tradeoffs: step 2 makes opaque foam
hide the water behind it per pixel regardless of draw order, and step 3
makes faint wake attenuate water continuously. So with the setting on:

- One pass is enough. `WAKE_COLOR_RENDER_TYPE` carries `WAKE_OIT_PIPELINES`
  via `setOitPipelines`, and `WakeRenderer`/`SplashPlaneRenderer` skip the
  depth-only pass when `GameRenderer.useImprovedTransparency()` is true (it
  would only reintroduce the hard 0.8 edge). With the setting off, the same
  render type draws through its normal pipeline as before.
- `WAKE_OIT_PIPELINES` mirrors vanilla's `OIT_ENTITY` set (same bind groups,
  entity shader, and accumulate modifier adding `PER_FACE_LIGHTING` +
  overlay/lightmap samplers) except for `OIT_ENTITY_SNIPPET`'s
  `ALPHA_CUTOUT 0.1`, so faint wake keeps fading (the depth bounds stage
  already discards alpha < 0.01). Compared side by side in-game against
  using `RenderPipelines.OIT_ENTITY` directly; the no-cutout version looked
  better.
- Only alpha > 0.99 counts as fully opaque. Foam at 0.95 lets ~5% of the
  water through instead of hiding it.
- `RenderPipelines.register(OitPipelineSet)` is private, so the three stage
  pipelines are registered individually. `OitPipelineSet.Builder` prefixes
  locations itself (`minecraft:pipeline/oit_<stage>_wakes_wake`).

The wakes now take three paths:

| Setup | Approach |
|---|---|
| Vanilla, Improved Transparency off | Method D (tiny sea floor band at alpha 0.8-1.0) |
| Vanilla, Improved Transparency on | Method I, single OIT pass |
| Iris with shaders (OIT forced off) | Method D plus the vertex-alpha cutoff fix (`WAKE_DEPTH_PASS_ALPHA`) |
