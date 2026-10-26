# Signed status effects

Copy the new `assets/lbc/post_effect/signed_effects/` files and the signed-effect
shader/include files into the resource pack. Also apply these outline changes:

- `assets/minecraft/post_effect/entity_outline.json`
- `assets/minecraft/shaders/core/rendertype_outline.vsh`
- `assets/minecraft/shaders/core/rendertype_outline.fsh`

These files preserve the existing zone, black-hole and nuclear-mushroom marker
channels. Enhanced glowing uses new marker colors `#D57500` through `#D57507`
and rows50-57. The existing `lbc:black_hole_small` invisible helper model is reused;
there are no new textures or translation keys. Reload with F3+T after applying.

Glowing II+ adds independent growing halos to the eight nearest enhanced glowing
actors. Their ordinary silhouette outlines and exact team colors stay intact.
The additional halo follows an analytic capsule around each actor. Higher levels
increase the radius up to screen size with bounded shader work.

Signed blindness, darkness, night vision and nausea use dedicated post chains.
Negative blindness reveals invisibility tiers separately for each viewer.
Server-side levels remain full signed int; visual strength saturates at screen
limits. The vanilla client still has its own effect-amplifier limits; player
levitation keeps vanilla physics, without position corrections.

Validation: 83 standalone GPU checks on RTX4080SUPER, plus native metadata,
effect-packet, visibility, carrier and lifecycle checks. Repeat shader validation
with `python tools/check_signed_effect_shaders.py` from the mod project.
