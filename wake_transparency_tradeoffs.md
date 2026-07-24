# Wake transparency vs. depth occlusion — method comparison

## The goal

Render wake/foam quads so that, at the same time:

1. **Faint wake stays translucent.** Low-opacity foam pixels blend with whatever is
   beneath them (water, sand, etc.) — the water must stay visible through them.
2. **Opaque wake stays opaque.** Pixels where the foam has reached full alpha (1.0)
   must look like solid white foam and must **occlude** the water block underneath —
   water must not draw over/through them.
3. Do (1) and (2) for the *same texture*, whose alpha varies continuously per pixel,
   ideally in as few render passes as possible.

## The hard position

GPU depth testing is fixed-function and binary. A `RenderPipeline`'s
`DepthStencilState.writeDepth` flag is one boolean for the entire draw call: a
fragment that reaches the ROP either commits a depth value or it doesn't. There is
no hardware concept of "70% occluded" — occlusion is all-or-nothing per fragment,
per draw.

Our input, alpha, is continuous (0.0–1.0). We want *how much a pixel blocks the
water behind it* to track that continuous value smoothly. But "how much" isn't a
thing depth testing can express — only "does" or "doesn't". Every technique below
is a different way of forcing a continuous value through a binary gate, and every
one of them gives something up to do it. That tradeoff is the actual constraint —
it's not a bug we haven't found the fix for yet.

The only two escape hatches from "binary per-draw" are:
- **`discard` per fragment in the shader** — genuinely per-pixel, but it deletes the
  fragment's color output along with its depth write. You can't keep one without
  the other from a single fragment invocation.
- **Dithering** — trade the hard boundary for noise, so it isn't perceived as a
  sharp edge. Still binary per-pixel, just spatially scrambled.

Nothing else can make "write depth" a soft, continuous decision within a single
draw. There's a third, more exotic hatch — fragment shader interlock / Rasterizer
Order Views — that lets a shader do its own conditional read-modify-write instead
of relying on the fixed-function ROP at all. It's covered as Method H below, and
it doesn't actually get further than Methods F/G: it just relocates the same cost.

---

## Method A — Single translucent pass, no depth write

**Goal it targets:** (1) only.

Standard `BlendFunction.TRANSLUCENT`, `writeDepth = false`.

- **Pros:** One pass. Perfectly smooth alpha gradient. Cheapest possible option.
- **Cons:** Never occludes anything. Water (or anything else translucent drawn
  after/around it) can render straight over 100%-opaque foam, tinting even
  "fully white" pixels with the water's color.
- **Why incomplete:** No depth write at all means goal (2) is entirely unmet —
  this is literally the bug that started this conversation.

## Method B — Single alpha-tested pass, depth write on (`ALPHA_CUTOUT` only)

**Goal it targets:** (2) only.

`ALPHA_CUTOUT` discards any fragment below a threshold (e.g. 0.5–0.65); survivors
depth-test and depth-write normally.

- **Pros:** One pass. Correct occlusion for anything above the cutoff — and this
  discard already happens **per pixel** (the shader samples the interpolated
  texture alpha at that exact fragment's `texCoord0`), not per node/quad. There
  was never a "granularity" problem here.
- **Cons:** Everything below the cutoff doesn't exist — no color at all, not even
  faint. As a wake node ages and its opacity fades under the threshold, the whole
  node's visible pixels binary-flip off in one frame ("popping"), instead of
  fading out.
- **Why incomplete:** Goal (1) is entirely unmet. There's no soft trailing fade,
  just a hard-edged shape that appears/disappears.

## Method C — Two passes, both writing color (our original implementation)

**Goal it targets:** (1) and (2) together.

Pass 1: all alpha, translucent blend, no depth write (Method A).
Pass 2: `ALPHA_CUTOUT`, translucent blend **and** depth write (Method B), same
geometry, same texture.

- **Pros:** Solves both goals simultaneously — faint areas show via pass 1, and
  pass-1 areas above the cutoff also get depth-blocked by pass 2.
- **Cons:**
  - Every pixel above the cutoff gets **blended twice** (once per pass), so the
    resulting color/brightness is not what either pass alone would produce. This
    creates a visible seam exactly at the cutoff boundary — not just an occlusion
    edge, but a color discontinuity layered on top of it.
  - Doubles CPU iteration (every node's vertices built twice) and doubles the
    fragment-shader/fill cost over the wake footprint (two full geometry
    submissions).
- **Why incomplete:** Solves both goals but introduces a *new* artifact (the
  double-blend jump) on top of the inherent occlusion edge from Method B.

## Method D — Two passes, second pass depth-only (`ColorTargetState.WRITE_NONE`) — current state

**Goal it targets:** (1) and (2), minus the double-blend bug from Method C.

Same as Method C, but pass 2's `ColorTargetState` write mask is `WRITE_NONE`: it
still discards/depth-tests/depth-writes exactly as before, it just never touches
the color attachment.

- **Pros:** Removes the double-blend artifact — every visible pixel's color comes
  from exactly one blend (pass 1), so there's no brightness/saturation jump at the
  cutoff anymore. Same occlusion behavior as Method C.
- **Cons:** Doesn't reduce the fundamental cost — pass 2 is still a full geometry
  submission (CPU rebuild, vertex shader, full fragment shader incl. fog/lightmap/
  overlay math it no longer needs, depth test). `WRITE_NONE` only skips the final
  ROP blend-and-writeback step, which is a small bandwidth saving, not a
  shading-cost saving.
  - **The hard occlusion edge is still there.** Whether water can draw over a
    pixel still flips at exactly `alpha == cutoff`. We removed the color jump, not
    the boundary itself.
- **Why incomplete:** Goal (3) (fewer passes / cheaper) is barely touched, and the
  remaining visible border (occlusion snapping on/off) — the thing the fix was
  originally asked for — is still fully present. This method fixes a bug Method C
  introduced; it does not resolve the original complaint.

## Method E — Dithered/stochastic cutout (proposed, not yet implemented)

**Goal it targets:** hiding the hard occlusion edge left by Method D.

Custom fragment shader for pass 2: instead of comparing alpha against one flat
constant, compare against a spatial dither pattern (e.g. a 4×4 Bayer matrix
indexed by `ivec2(gl_FragCoord.xy) & 3`). As alpha rises through a range, an
increasing *fraction* of pixels in that region start passing the test, rather than
all of them flipping at once.

- **Pros:** Turns a sharp iso-contour into a fine stippled transition band —
  much less perceptible as a "line" than a smooth alpha gradient hitting a wall.
  Standard, proven technique (used for foliage cutouts and LOD cross-fades).
- **Cons:**
  - Requires writing and maintaining a custom fragment shader (we currently rely
    entirely on Mojang's stock `core/entity.fsh` + shader defines).
  - Without TAA (which Minecraft's core pipeline here doesn't give us), the
    pattern is static per-frame — it reads as noise/stipple, not a smooth blur.
    At typical foam viewing distance this is usually acceptable, but it is a
    texture-like artifact, not a true gradient.
  - Doesn't reduce pass count or CPU/GPU cost at all — it only changes *where*
    the pass-2 discard boundary falls, pixel by pixel.
- **Why incomplete:** Softens the *perception* of the binary edge; does not
  remove the fact that occlusion is still binary per pixel. It's camouflage, not
  a fix to the underlying constraint.

## Method F — Move the mask onto water's own shader (inverted responsibility)

**Goal it targets:** true single-pass wake rendering.

Instead of wake writing depth to block water, render wake in one translucent pass
only (Method A), and patch water's own fragment shader to sample the wake alpha
texture at the corresponding position and discard/blend *itself* out under
high-alpha wake pixels.

- **Pros:** Only one wake-side draw. Conceptually the "right" place to resolve
  this, since it's the receiving surface that needs to know it's covered.
- **Cons:**
  - Requires mixing into/replacing vanilla's fluid render pipeline — code we
    don't own, and something this mod already treats carefully around Iris/shader
    pack compatibility (see `areShadersEnabled`/`shader_compat.md`). Shader packs
    replace core water shaders wholesale, so this hook would silently stop
    working under most of the shader packs already listed as "compatible."
  - Still two shader executions overall (wake's + water's extra sampling), just
    relocated — not actually less total work, only fewer wake-side draw calls.
  - Much larger blast radius: a bug here can break vanilla water rendering, not
    just wake rendering.
- **Why incomplete:** Trades "two passes on our own geometry" for "an invasive,
  fragile dependency on code we don't control and that shader packs routinely
  replace." Not pursued for this reason.

## Method G — Manual depth-buffer writes from the shader (compute/image store)

**Goal it targets:** true per-fragment *partial* depth control, bypassing the
fixed-function write stage entirely.

Have the fragment shader write into a depth-like texture via `imageStore` (or a
storage buffer), and have consuming geometry manually sample and compare against
it instead of using the real hardware depth test.

- **Pros:** In theory, total control — you could encode something richer than a
  boolean if you also changed every consumer to read it.
- **Cons:** The real depth/Z-buffer that vanilla rendering (water, terrain,
  entities, particles) actually tests against is still the fixed-function one;
  writing to a side texture doesn't make water respect it unless water is also
  rewritten to consult that texture (this collapses into Method F's problem, plus
  more manual synchronization). Minecraft's `RenderPipeline`/`RenderType`
  abstraction used here doesn't expose arbitrary image-store bind points for this
  snippet system without a much deeper custom pipeline.
- **Why incomplete:** Doesn't actually solve anything Method F doesn't already
  require, at higher implementation cost, for no additional benefit in this
  codebase.

## Method H — Fragment shader interlock / Rasterizer Order Views (theoretical)

**Goal it targets:** a genuine per-fragment "write color but conditionally skip
depth" knob — the thing that would, if it existed, collapse this whole document
into one pass.

Modern APIs (D3D11.3+/D3D12 ROVs, `GL_ARB_fragment_shader_interlock`,
`VK_EXT_fragment_shader_interlock`) let a fragment shader take a per-pixel lock
and manually read-modify-write an arbitrary buffer/image with the same
front-to-back ordering guarantee the fixed-function depth test normally gives
you for free. In principle: skip the hardware depth test/write entirely, and
have the shader itself decide, per fragment, whether to commit a value to a
depth-like resource — genuinely conditional, genuinely per-pixel, no `discard`
needed.

- **Pros:** The only mechanism that actually offers a true per-fragment
  conditional commit instead of a binary discard-or-keep. Closest thing to the
  "shader-level knob" that fixed-function rasterization doesn't expose.
- **Cons:**
  - It only lets you programmably read/write a buffer **you** own. The real
    depth buffer that vanilla water, terrain, entities, and particles test
    against via fixed-function hardware doesn't move — writing into your own
    interlocked buffer doesn't make water's unmodified draw call respect it.
    To make water actually occlude/not-occlude based on this buffer, water's
    own shader has to be changed to sample it — which is Method F's cost,
    not a new, cheaper path around it.
  - Where it *would* help — synchronizing multiple overlapping wake quads
    against each other — isn't the problem here at all; the problem is a wake
    quad against water's untouched pipeline.
  - Real perf cost: interlock/ROV serializes overlapping fragment shader
    invocations per pixel, and support/behavior varies across GL/Vulkan/Metal/
    older hardware — another custom-shader-path maintenance burden on top of
    the Iris/shader-pack fragility already flagged for Method F.
- **Why incomplete:** It's real proof the "binary per-draw" constraint is a
  fixed-function ROP limitation and not a made-up one — genuinely programmable
  conditional writes exist. But the moment you need a party outside your own
  draw call (water) to respect that conditional result, you're back to
  rewriting water's shader. It doesn't add a cheaper option; it just confirms
  Method F/G's cost is the actual floor, not a symptom of not having looked
  hard enough for a shader-level toggle.

---

## Where this leaves us

- Methods A and B each solve exactly one of the two goals and fail the other —
  they aren't candidates, just useful reference points.
- Method C solved both goals but introduced a double-blend seam.
- Method D (current) fixes that seam but leaves the original, inherent occlusion
  edge in place, and doesn't reduce cost.
- Method E can make that remaining edge much less visible, at the cost of a
  custom shader and a static dither pattern instead of a true gradient.
- Methods F and G could in principle get to a single pass, but only by taking on
  dependencies on vanilla/shader-pack-owned code that this project has
  historically had to work around, not with.
- Method H confirms there's no shader-level trick that avoids that cost: the
  "binary per-draw" constraint is a real fixed-function hardware limitation
  (no per-fragment depth-write-enable output, only whole-fragment `discard`),
  and even the fully-programmable alternative (interlock/ROV) only moves the
  problem back to "rewrite water's shader," same as Method F.

There is no version of this that is simultaneously: one pass, perfectly smooth,
and fully correct occlusion. Pick two.

## 2026-07-24: Method D breaks under shader packs specifically

Reported symptom under Iris: faint wake showed the sea floor instead of
water beneath it — the opposite of Method D's intent (only opaque foam
should occlude water; faint wake shouldn't occlude anything). Root cause,
as best understood: once pass 1 (`WAKE_COLOR_PIPELINE`) was correctly
classified as `ENTITIES_TRANSLUCENT` for Iris (see
`OCCLUSION_INVESTIGATION.md`, "Iris pipeline classification"), it became
subject to whatever a shader pack's own translucent-object handling does for
its water/SSR effects — and that logic appears to treat *any* translucent
fragment as occluding the pack's own water render beneath it, regardless of
how close to zero its alpha actually is. Pass 1 draws at every alpha level
down to near-zero specifically so faint wake fades smoothly (that's its
entire purpose under Method D) — which is exactly what triggers this.

Tried and reverted: a third pipeline, used only when
`WakesClient.areShadersEnabled`, effectively Method B (single
`ALPHA_CUTOUT` pass, real color output, depth write) replacing both passes
under shaders. No pass 1 means no near-zero-alpha translucent fragments for
a shader pack to misinterpret. Removed to keep investigating with the
original two-pass split still active under shaders instead.
