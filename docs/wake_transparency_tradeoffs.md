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

**Known caveat, Iris/shader packs specifically:** once pass 1 is correctly
classified as `ENTITIES_TRANSLUCENT` for Iris (see `docs/occlusion_zones.md`
/ `docs/shader_compat.md`), it becomes subject to whatever a shader pack's
own translucent-object handling does for its water/SSR effects — which
appears to treat *any* translucent fragment as occluding the pack's own water
render beneath it, regardless of how close to zero its alpha is. Since pass 1
intentionally draws at every alpha level down to near-zero (that's the whole
point — smooth fade for faint wake), this can show the sea floor instead of
water under very faint wake, under some shader packs. A shader-specific
single-pass alternative was prototyped to work around this and reverted (see
history) — currently unresolved, tracked as a known limitation rather than
patched over.

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
investigation — not committed as the shipped behavior.

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
