# Analytic stone-arena ring — 27 October 2026

The current read-only author pack contained96coplanar overlapping rectangular
sectors. The existing item shader already bypassed directional/lightmap/fog
lighting for this exact material, and TrialWarningVisual already set shadow
radius/strength0. Changing brightness would not remove the overlapping geometry.

Prepared four necessary RP replacements from that current pack, following
[RESOURCE_PACK.md](RESOURCE_PACK.md). The model is now a single XZ element with
up/down faces. The shader derives local coordinates from the existing native
four-vertex quad indexing convention, independent of atlas position/texture size.
Its analytic annulus has radii1/3 and1/2, with derivative antialiasing. Exact old
RGBA marker recognition restricts the mask to the ring. `gl_FrontFacing` discards
the back face even on no-cull passes, avoiding doubled translucency.

This retains the model ID, original texture/tint, green color,0.55base alpha,
server10..15band, one-tick visual interpolation and fade. No Java, server JAR,
actual RP, bloom sampler, post chain or other warning material is changed.
Copy all four resources together; partial application is explicitly unsupported.
User instructions: [MEDITATION_RING_README.md](../resourcepack_template/MEDITATION_RING_README.md).

`tools/check_meditation_ring_shader.py` compiled/linked eight ACTUAL item shader
variants on RTX4080SUPER, including OIT accumulation/alpha-only/cutout/glint.
GPU renders use both model faces and actual core vertex/fragment shaders:
top/bottom/oblique, cull/no-cull, atlas64/256, vertex offsets0/4/12, reversed face
order, additive contribution counting, dark/full lighting and quarter-opacity
fade. Eleven other/near-match materials match the original shaders exactly.
Measured annulus area error is0.00505%; original pack hashes and installed JAR
are preserved. Evidence: `build/verification/meditation-ring/`.
An initial report serialization failed on NumPy float32 after all assertions;
the report now converts that scalar to ordinary float. No shader change was
needed for that reporting correction. Physical in-game client capture is not
claimed. No server build/restart is needed for these resource-only changes.
