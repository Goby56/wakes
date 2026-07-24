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

__DISCLAIMER__: Some shaders add wavy water, which means water will clip with
the wake textures. In some cases there's an option to disable this.

## Why: without explicit assignment, several packs broke wake rendering entirely

Before `assignPipeline()` was added, wake pipelines went through Iris's
internal fuzzy matcher (`ShaderKey.findBestMatch()`, which scores pipelines
against known categories by vertex format/blend/alpha-test) instead of being
told directly what they are. Several shader packs matched incorrectly under
this scheme and completely broke wake rendering. Explicitly assigning both
pipelines to their correct Iris program categories fixed this without
requiring any change to the pipelines themselves — confirmed working across
the packs below.

## Compatible: [Iris](https://modrinth.com/mod/iris) shaders
- Complementary V4
- Complementary Reimagined*
- Rethinking Voxels
- Shrimple

\*Note
- Complementary Reimagined: Wakes sometimes suddenly disappear or have strange colors. See [issue 17](https://github.com/Goby56/wakes/issues/17) and [issue 21](https://github.com/Goby56/wakes/issues/21).

## Known incompatibilities
- LS RenderPearl
- BSL (can't figure out how to turn off wavy water, aside from that it works fine)
- Overimagined Shaders (works when camera is close to the water)

## Optifine shaders
Currently not supported
