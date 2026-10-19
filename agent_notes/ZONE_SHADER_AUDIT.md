# Zone border shader audit (read-only sources)

## Radius: scale is radius, not diameter

`assets/lbc/models/item/zone_border.json` contains 2,048 wall elements. Their vertices lie on a circle centered at model coordinates `(8, 0, 8)` with radial distance exactly `16` model units (checked from the JSON element coordinates; no texture image was opened). Minecraft item-model coordinates divide by 16, so the mesh has local radius `1`. Therefore the supplied display transform `scale=[400,1100,400]` yields **world radius 400 blocks and diameter 800 blocks**. `scaleY=1100` changes the mesh's vertical extent; it does not change the XZ radius. A change in X/Z scale by `Δs` changes radius by `Δs` blocks and diameter by `2Δs` blocks. The item selector points to `lbc:item/zone_border`; its constant model tint matches the shader's reserved tint.

The post shader itself intersects a **unit-radius** cylinder (`remainder = 1 - dot(closest, closest)`) in local coordinates. It obtains that local coordinate system from four transformed references: reference 0 is the center, and the differences to references 1–3 form the basis; the inverse basis maps world-relative positions into the unit cylinder. It does not calculate radius by measuring visible model geometry. If those references carry the display scale, the unit cylinder maps back to the transformed world radius above.

## Reference reconstruction: degenerate by static model geometry, client result unverified

The current model's `post_data_00` through `post_data_56` helpers have the same `from=[7.9990234375,0,7.9990234375]` and `to=[8.0009765625,0,8.0009765625]`, with UV values advancing by `0.25`. I traced the shader code: `lbc_zone_tag(uv, atlasSize) = floor(fract(uv.x * atlasSize.x) * 64)`; in `rendertype_outline.vsh`, `field=tag/2`, and for fields below 12 the encoded scalar is exactly `Position[field % 3]`. The outline vertex shader remaps `gl_Position` to the tagged one-pixel slot, but does not add an axis or pivot offset to `Position`. The fragment shader packs that scalar's float bits into the two tag pixels. The post vertex shader decodes 12 floats into references as `ref[p][axis] = zoneFloat(3*p + axis)`, then forms `basis = mat3(ref1-ref0, ref2-ref0, ref3-ref0)` and exits if `abs(determinant(basis)) < 1e-12`.

For coincident helper geometry and consistent quad interpolation, the direct formulas imply `ref0 = ref1 = ref2 = ref3 = T(8,0,8)`, where `T` is the display transform (scale/translation/rotation/interpolation). That yields `basis = mat3(0)` and the post vertex shader would leave `zoneEnabled=0`. No inspected RP shader synthesizes reference axes. This is a **static-source prediction only**: I did not observe the target client, and this prediction conflicts with the user's report that the shader is already in use. The source alone cannot explain that report; treat actual post-pass activation as unverified here rather than conclude the effect is broken.

The item vertex/fragment shader reserves a special tint for this model, discards the helper quads from ordinary item rendering, and draws only a 10%-opaque red fallback. The model item's item-definition tint is `16645887`, which is exactly RGB `#FD FE FF`; the shader's reserved matching tint is `ivec3(253,254,255)`. The model tint therefore matches the reserved branch input.

## Center, height window, and animation

The intended post path transports position values and the inverse view-projection matrix through the glowing-outline framebuffer: helper fragments encode float bits into RGB pixels, and the post shader reads those pixels back. It uses `references[0]` as center and `inverse(mat3(ref1-ref0, ref2-ref0, ref3-ref0))` as the world-to-local basis. Each screen pixel reconstructs a camera ray from the inverse view-projection matrix, intersects the unit cylinder, clips by scene depth and local height, then shades the two visible intersections.

The supplied Y scale and translation do not define the shader wall's visible height. `zone_window.glsl` defines a camera-relative vertical band: half-height is 5 through horizontal distance 5, grows with a smooth near-distance term through distance 20, then grows quadratically; opacity fades over the last 10% (minimum 1 block). The item fallback clamps its mesh vertices to this band. The post shader also checks this window for wall intersections and ground-contact glow.

With ordinary Minecraft display interpolation, a changing X/Z scale changes the mesh radius continuously between the old and new transform. The outline shader comments that its reference positions already include scale, translation, rotation, and display interpolation; if reference axes are present, the post basis tracks the interpolated transform each rendered frame. For the supplied new shrink `R=384 -> 4` over `4000` ticks, the corresponding linear scale change is `-380/4000 = -0.095` blocks per tick (nominally 200 seconds at 20 TPS); the diameter changes from 768 to 8 blocks. The helper-reference layout prediction above remains unverified against the client you report using.

## Viewport, range, and glowing-channel caveats

- The post pass reconstructs rays from framebuffer `ScreenSize` and `texCoord`; neighbor rays are offset by one framebuffer pixel. The helper transport reserves the first 57 horizontal pixels and accepts its header on either the top or bottom framebuffer row to handle OpenGL/Vulkan viewport convention. At widths below 57 the post pass exits; if the outline pass does not write its header, the post effect stays disabled.
- The data transport depends on the entity reaching the glowing-outline pass with the shader's reserved `#FD FE FF` tint. The outline fragment shader reserves that narrow strip and writes 57 tagged data pixels; other glowing entities in the same strip are explicitly discarded there. It is not a generic property of setting an arbitrary glow color.
- The wall geometry is a 2,048-sided mesh approximation; the analytic post path intersects an exact cylinder. The fragment shader includes a mesh-depth tolerance specifically to reduce self-occlusion mismatch. If post shading is disabled, only the translucent mesh fallback remains.
- The supplied `view_range=64` is an entity-side visibility parameter; the RP shader sources neither define its units/semantics nor override entity visibility/tracking. They therefore do not establish that the entire 400-block-radius wall will remain visible at all viewer distances.
- No source shader performs chunk loading or forces chunk visibility. A client cannot render unloaded geometry/terrain; the post pass is screen-space over the scene depth/color already supplied to it. The RP source does not establish behavior for chunks outside the client's loaded/rendered scene.

## Read-only source links

- [Item definition](<C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/lbc/items/zone_border.json>)
- [Zone border model: mesh, helper quads, and item display transforms](<C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/lbc/models/item/zone_border.json>)
- [Shader tint/tag constants](<C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/lbc/shaders/include/zone_data.glsl>)
- [World-relative height window](<C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/lbc/shaders/include/zone_window.glsl>)
- [Post vertex: decoding references and rejecting singular basis](<C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/lbc/shaders/post/zone_border.vsh>)
- [Post fragment: cylinder intersection, clipping, and shading](<C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/lbc/shaders/post/zone_border.fsh>)
- [Energy/noise field](<C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/lbc/shaders/include/zone_border.glsl>)
- [Item vertex shader](<C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/shaders/core/item.vsh>) · [item fragment shader](<C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/shaders/core/item.fsh>)
- [Outline data encoder](<C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/shaders/core/rendertype_outline.vsh>) · [outline framebuffer writer](<C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/shaders/core/rendertype_outline.fsh>)

RP inspection was read-only. No PNG was opened, and no code/resource files were changed.



