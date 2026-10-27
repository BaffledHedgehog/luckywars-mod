# Meditation warning displays (26.3)

## Update: continuous red countdown and electric walls, 27 October 2026

For an already updated pack, copy ONLY these three prepared files to the same
relative paths, then F3+T:

- `assets/minecraft/shaders/core/item.vsh`
- `assets/minecraft/shaders/core/item.fsh`
- `assets/lbc/shaders/include/meditation_warning.glsl`

They were patched from the current read-only author pack. No texture, item model,
post-chain or bloom sampler replacement is required for this update. In
particular, do not copy an emptied/applied template file as a shader. If the
author pack changes before copying, merge the changes described below instead.

The server update turns each electric warning wall 90 degrees so its long axis
follows laser travel, and adds four midpoint columns: nine at spacing3.5 instead
of five at spacing7, keeping the same outer columns. The first phase now owns
one continuous30tick warning per attack instead of three10tick pulses. The
attack still fires on31 and the warning activation sound plays once.

All shared **red beam** warnings (including Sauvojen) interpolate in the item
shader from burgundy `#600018` to bright red `#FF0E0E`. The color ramp completes
before the final fade; only then does opacity fall while the red stays bright.
This uses one texture and continuous GLSL `mix`, with tick-updated countdown
bytes, not ten separate models/textures. Green beams, walls and other warnings
keep their previous colors and opacity. Color recognition uses the exact raw
warning texture marker, never the world scene. The old post-sampler helper is
now an inert compatibility stub, so an unremoved old call cannot make terrain
glow again. Material-only bloom remains subject to the limitation below.

The three full files already contain the following merge changes. **Do not add
them twice** when using the ready replacements.

## Initial installation reference

The six `assets/lbc/items/meditation_warning_*` item definitions, corresponding
models and textures were supplied with the initial warning update. Already
applied assets stay in the author pack; they are not recreated here. The
core-shader merge reference below is for packs without the initial edits. Do not
replace existing core shaders or `entity_outline.json`: their zone, black-hole,
nuclear, signed-effect, zoom and bloom pipelines must stay intact.

## Server/model contract

All names below have namespace `lbc:` and prefix `meditation_warning_`.
Use `item_display` with item display context `NONE`, fullbright 15/15 and the
specified `minecraft:item_model`. Coordinates below describe effective display
geometry AFTER native `ItemDisplayRenderer` Y(pi) rotation and model translation
of (-8,-8,-8)/16; there are no inherited transforms. Beam JSON Z=-8..8 becomes
effective +Z=0..1 after that native Y(pi) rotation.

| Suffix | Local geometry in blocks | Base alpha |
| --- | --- | --- |
| `beam_red`, `beam_green` | +Z from 0 to 1, centered X/Y, square width 1/16 | .45 |
| `ring_green` | XZ annulus at Y=0, outer radius .5, inner radius 1/3 | .55 |
| `wall_red` | Double-sided XY plane centered origin, width/height 1 | .20 |
| `cube_blue` | Hollow six-face cube, centered origin, sides 1 | .07 |
| `sphere_orange` | Spherical shell centered origin, nominal radius .5 | .20 |

Beam world scale `(thickness*16, thickness*16, length)` gives its width and
length; rotate +Z toward the destination. Annulus scale `(30,1,30)` gives the
10..15-block radial band. Wall scale `(width,height,1)`; cube scale `(size,size,size)`;
sphere scale `(diameter,diameter,diameter)`. Scale sphere geometry over time to
expand it. Ring uses 96 overlapping flat sectors; sphere uses 48 longitudes by
24 latitudes of rotated tangent patches (1152 elements, 2304 faces). Both are
actual world geometry, visible from inside/outside, with no screen-facing proxy.
Sphere patches overlap slightly at seams to close the surface; tiny seams and
less than 1% radius/silhouette approximation remain. These new rotation fields
are supported by native 26.3 `CuboidModelElement` / `CuboidRotation`; do not use
this model unchanged on a client that rejects arbitrary Euler rotations.

`minecraft:custom_model_data.colors[0]` is an RGB data tint. For red beams:
R is opacityByte, G is countdownByte, B is0; each byte is0..255. The initial
tint is `0xFF0000`, the bright full-opacity stage `0xFFFF00`. This data tint is
not the rendered beam color. Other warnings keep the original grayscale fade
`grayRGB = opacityByte * 0x010101`. Missing color defaults white.
Opacity is `base alpha * opacityByte/255 * ColorModulator.a`; red-beam RGB comes
from the countdown mix, other RGB stays at its fullbright palette value.
Floats/strings/flags are unused. Fade/countdown updates require
changing this component on the display stack; item display transform
interpolation does not interpolate tint. The server should send enough steps
for the desired fade. Zero opacity is discarded before OIT participation.

The six solid PNGs use unique alpha/palette pairs (253 beam,252 ring,251 wall,
250 cube,249 sphere); ordinary alpha254 and130 handling stays intact. RGBs are
red `255,14,14`, green `43,216,147`, blue `0,48,255`, orange `255,142,50`.
No glowing/outline team colors, carrier rows or post-chain registrations change.

## Merge: item.vsh

In `assets/minecraft/shaders/core/item.vsh`, add after location10 output:

```glsl
layout(location = 11) out float meditationWarningFade;
layout(location = 12) out float meditationWarningProgress;
```

Inside `main`, immediately after setting `vertexColor`, add:

```glsl
meditationWarningFade = Color.r; // raw grayscale tint, BEFORE directional light
meditationWarningProgress = Color.g; // independent red-beam countdown
```

Keep every existing statement and all `OIT_ALPHA_ONLY` / `GLINT` variants.

## Merge: item.fsh

In `assets/minecraft/shaders/core/item.fsh`, add next to existing includes:

```glsl
#include <lbc:meditation_warning.glsl>
```

After the location10 input, add:

```glsl
layout(location = 11) in float meditationWarningFade;
layout(location = 12) in float meditationWarningProgress;
```

After the current `alphaMarker` declaration in `main`, add:

```glsl
float meditationBase = lbc_meditation_warning_base_alpha(rawColor);
bool isMeditationWarning = meditationBase >= 0.0;
```

Prepend `isMeditationWarning ||` to the existing `bypassLighting` expression.
The current `bypassFog = bypassLighting` already covers warnings. Replace ONLY
the existing `vec4 color = isZoneBorder ? ... : rawColor;` declaration with:

```glsl
vec4 color = isMeditationWarning
    ? vec4(lbc_meditation_warning_color(rawColor, meditationWarningProgress), meditationBase * clamp(meditationWarningFade, 0.0, 1.0) * ColorModulator.a)
    : (isZoneBorder ? vec4(1.0, 0.0, 0.0, 26.0 / 255.0) : rawColor);
```

In the existing `ALPHA_CUTOUT` comparison, replace its threshold
`(isZoneBorder ? 0.005 : ALPHA_CUTOUT)` with:

```glsl
(isMeditationWarning ? 0.00001 : (isZoneBorder ? 0.005 : ALPHA_CUTOUT))
```

After the `ALPHA_CUTOUT` block, the existing color-modulation chain begins
`if (isZoneBorder) { color.a *= ColorModulator.a; } else if ...`.
Change ONLY that chain's opening to:

```glsl
if (isMeditationWarning) {
    if (color.a <= 0.00001) discard;
} else if (isZoneBorder) {
    color.a *= ColorModulator.a;
} else if (alphaMarker == 254) {
    // ... retain all current branches unchanged from here ...
```

Leave `executeAlphaOnlyPhase`, `calculateFinalColor`, GLINT and OIT accumulation
unchanged. Do not modify `entity.fsh`: item displays use `item.vsh`/`item.fsh`.

## Merge: existing bloom sampler

The original instructions added approximate color matching to the already
composited scene. This also matched ordinary orange terracotta, red sandstone
and other terrain colors. **Do not add that match.** If it was already applied,
remove these two lines from `assets/minecraft/shaders/post/bloom_sample.fsh`:

```glsl
#include <lbc:meditation_warning.glsl>
outColor = max(outColor, lbc_meditation_warning_bloom(checkColor));
```

The earlier correction supplied a `bloom_sample.fsh` replacement; once applied
or emptied by the author, do not copy that old template again. The new warning
include's compatibility stub makes the old call inert as well. Retain the existing 15-color list,
tolerant red branch, blur/composite shaders and post-chain order.

Keep the `meditation_warning.glsl` include in **item.fsh**: its exact texture
RGBA marker recognition is still required for fullbright warnings and their
transparency/fade/countdown. Its old bloom helper now returns zero for backwards
compatibility; a remaining old post call is harmless. The bloom correction alone
needs no server JAR change; the countdown/wall update requires the new server
build as well as the three shaders above. Translucent
warnings can have weaker/missing bloom after blending into the background.
Independent material-only bloom needs a separate mask and is not implemented by
this correction. Do not restore it by matching approximate scene colors.

## Verification

Native 26.3 source was checked for model tint schema and arbitrary Euler rotations.
The merged shaders were compiled/linked on the available standalone OpenGL context
for ordinary, cutout, alpha-only, OIT accumulation and glint variants. GPU samples
checked each helper base alpha, grayscale half/zero fade and exclusion of legacy
alpha254/130 from warning recognition. The former faded-palette bloom tests did
not cover terrain and missed the regression. The correction additionally tests
actual vanilla block texture pixels at multiple brightnesses and the unchanged
legacy glow palette on the GPU. This is shader/asset validation, not an in-game
image review; the user applies the pack edits.
