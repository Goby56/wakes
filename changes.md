### Changes
- Added bricks. Variable sized storage containers for wake nodes which should provide faster node retrievals and reduce drawing times as there are fewer quads rendered. This may fix issue #41.
- One quad rendered per brick, was originally one quad per node.
- As the nodes are no longer rendered per block the correct light information while rendering can't be provided. The light color is, therefore, baked into the actual pixel colors which are calculated once per tick.
- Every brick now has its texture storage which does use up a bit more memory than just having one texture. But this enables per tick coloring as opposed to per frame coloring which is much faster.
- Also removed memory leak caused by texture pointers being allocated but not deallocated. #89 This was when the textures was stored in each node (0.2.5)
- Removed LODs as the implementation didn't provide any performance benefits.
- Improved splashes at the front of the boat. The planes are now static for boats, have fixed splash clouds at the front and stationary clouds at paddle splashes.
- Shader compatibilty has probably been changed due to the new lighting method. A slider in the wake appearance config tab allowing the user to manually fine tune the look has been added.