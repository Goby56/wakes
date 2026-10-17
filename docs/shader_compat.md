# Shader pack compatibility

## Current state

Wakes registers its render pipelines with Iris explicitly, instead of
relying on Iris's fuzzy pipeline matcher to guess what they are:

```java
IrisApi.getInstance().assignPipeline(WAKE_COLOR_PIPELINE, IrisProgram.ENTITIES_TRANSLUCENT);
IrisApi.getInstance().assignPipeline(WAKE_PIPELINE, IrisProgram.ENTITIES);
```

(`WakesClient.java`, guarded by `FabricLoader.getInstance().isModLoaded("iris")`.)
`WAKE_PIPELINE` is the depth-only, `ALPHA_CUTOUT` pass (see
`docs/wake_transparency_tradeoffs.md`) — there's no cutout-specific entity
category in the public `IrisProgram` enum, so `ENTITIES` is the closest
available match; since that pass writes no color regardless
(`ColorTargetState.WRITE_NONE`), the main risk from this approximation is a
pack's own alpha-test threshold shifting the depth-write boundary, not a
visual color/lighting mismatch.

That risk is real. `ShaderKey.findBestMatch` only checks that the pipeline
*has* an `ALPHA_CUTOUT` define, not its value, and resolves `WAKE_PIPELINE`
to `ENTITIES_CUTOUT` (alpha > 0.1). Our 0.8 cutoff is lost, the depth pass
writes depth under faint wake, and water is rejected there: this is the
sea-floor-through-wake bug. Fixed (confirmed in-game) by the vertex-alpha
fix (`WAKE_DEPTH_PASS_ALPHA`); details in
`docs/wake_transparency_tradeoffs.md`.

Iris also forces vanilla's "Improved Transparency" (OIT in 26.3+) off while
shaders are enabled (`MixinDisableFabulousGraphics`), so the OIT path never
runs under shaders.


Before `assignPipeline()` was added, wake pipelines went through Iris's
internal fuzzy matcher (`ShaderKey.findBestMatch()`, which scores pipelines
against known categories by vertex format/blend/alpha-test) instead of being
told directly what they are. Several shader packs matched incorrectly under
this scheme and completely broke wake rendering. Explicitly assigning both
pipelines to their correct Iris program categories fixed this without
requiring any change to the pipelines themselves — confirmed working across
the packs below.

__DISCLAIMER__: Some shaders add wavy water, which means water will clip with
the wake textures. In some cases there's an option to disable this.

## [Iris](https://modrinth.com/mod/iris) shader packs

Tested with Iris 1.11.7 on Minecraft 26.3.

### Works
- BSL
- Bliss (splash planes cast jagged shadows)
- Complementary Reimagined
- Complementary Unbound
- Mellow

### Partially works
These render wakes correctly, but the wake color is hard to tell apart from
the pack's water.
- MakeUp Ultra Fast
- Photon
- Solas

### Doesn't work
- I Like Vanilla (wakes flicker)

## Optifine shaders
Currently not supported
