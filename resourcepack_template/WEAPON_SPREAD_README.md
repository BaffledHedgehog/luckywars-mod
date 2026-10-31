# Dynamic weapon spread crosshair

Apply these six files from this template to the same paths in the author's
`all_in_one_1.21.8` resource pack, then reload with F3+T:

- `assets/minecraft/shaders/core/rendertype_outline.vsh`
- `assets/minecraft/shaders/core/rendertype_outline.fsh`
- `assets/minecraft/post_effect/entity_outline.json`
- `assets/lbc/shaders/include/weapon_spread_data.glsl`
- `assets/lbc/shaders/post/weapon_spread.vsh`
- `assets/lbc/shaders/post/weapon_spread.fsh`

The three replacement files were prepared from the current author pack in the
launcher `26.3` instance. They retain the existing zone, black-hole, nuclear,
fade-carrier and signed-glowing contracts and all existing post-chain passes.
Only the new marker transport, reserved row and two spread passes were added.
Other deleted templates were not restored. The author's pack was only read.

Four thin bars surround the ordinary vanilla crosshair. Their outer tips follow
the horizontal and vertical angular bounds of the current shot distribution;
their RGB is exactly `1 - scene.rgb`, and scene alpha is preserved. Bar thickness
is one framebuffer pixel at small resolutions and scales with resolution.
Near zero spread the bars shorten to fit their extents. Zero spread draws none.
No new texture, model, language key or separate post-effect activation is needed.
The existing invisible `lbc:black_hole_small` helper model is reused. The server
removes the helper during scope use, so the additional bars disappear there.

The display carries marker `#D57600` (RGB 213,118,0), `scaleX` equal to the actual
half-angle in degrees, `scaleY` equal to mode 1 (cone) or 2 (independent Euler
yaw/pitch square), and `scaleZ=1`. Its four reference points use the existing
native display transform transport. Raw outline row 58, or the matching mirrored
edge row, holds header `SPR` (83,80,82) and paired float bytes tagged 201/202.
Rows 0–49 and signed-glowing rows 50–57 retain their original owners. The raw
spread row has zero alpha and is protected from ordinary glowing overdraw.

Fields 0–11 retain the native reference points. Kind 5 uses fields 12/13 for the
client's absolute projection X/Y scales and fields 14–22 for its current world
forward, right and up unit vectors. The new decoder alone reads those fields;
the existing marker kinds retain their inverse-view-projection data unchanged.
Cone extents use `tan(halfAngle)` and the actual client projection, including
FOV and viewport aspect. Minigun mode computes the perspective envelope over
the full yaw/pitch square with analytic stationary yaw roots and pitch edges.
The local rendered camera basis makes that envelope continuous while looking
up or down, including the poles. Invalid/nonfinite records and unsupported
half-angles at or above 89.9° produce no additional bars; viewport clipping is
bounded and divisions at the projection horizon are guarded.

Run `python tools/check_weapon_spread_shader.py` from the mod project for
standalone GPU validation. The final run on RTX 4080 SUPER passed 365 checks,
including 126 independent dense CPU Euler-envelope comparisons across pitch
0/±60/±89/±90°, yaw 0/127°, FOV 45/90/110°, and three viewport aspects. It also
compiled/linked the core and post shaders, exercised actual core byte encoding,
transparent records and glow-overdraw protection, checked four-bar inversion
and exact alpha, and rejected absent/malformed records. All 57 existing raw
marker records and black-hole disk kind 1 rendered byte-identically to the
current author pack. The existing post chain is structurally unchanged after
removing the two added passes and their target. Evidence is written to
`build/verification/weapon-spread-crosshair/results.json`.

These checks use a standalone OpenGL context. Actual Minecraft HUD ordering,
resource reload and scoped transitions still require client observation after
the user applies the files.
