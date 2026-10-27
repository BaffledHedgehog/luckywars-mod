# Meditation warning displays (26.3)

Copy ONLY the six new `assets/lbc/items/meditation_warning_*`, corresponding
`models/item/`, `textures/item/` files and
`assets/lbc/shaders/include/meditation_warning.glsl` into the pack. Apply the
three small shader edits below to the CURRENT pack files, then F3+T. Do not
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

`minecraft:custom_model_data.colors[0]` is an RGB grayscale fade byte:
`grayRGB = opacityByte * 0x010101`, byte 0..255. Missing color defaults white.
Opacity is `base alpha * opacityByte/255 * ColorModulator.a`; RGB stays at its
fullbright bloom palette value. Floats/strings/flags are unused. Fade requires
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
```

Inside `main`, immediately after setting `vertexColor`, add:

```glsl
meditationWarningFade = Color.r; // raw grayscale tint, BEFORE directional light
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
    ? vec4(rawColor.rgb, meditationBase * clamp(meditationWarningFade, 0.0, 1.0) * ColorModulator.a)
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

In `assets/minecraft/shaders/post/bloom_sample.fsh`, add the include next to
existing includes, and add the call immediately BEFORE `fragColor = outColor;`:

```glsl
#include <lbc:meditation_warning.glsl>
// ... inside main, immediately before fragColor assignment:
outColor = max(outColor, lbc_meditation_warning_bloom(checkColor));
```

Retain the existing 15-color list, tolerant red branch, blur/composite shaders
and post-chain order. This extends existing palette bloom continuously to faded
intensities; it does not allocate another full-screen pass. Since existing bloom
samples the ALREADY blended scene, arbitrary backgrounds can reduce its ability
to isolate translucent warning colors; palette-colored world pixels can glow too.
Perfect material-only glow would require a separate material mask/render target.
Geometry opacity remains correct independently of that limitation.

## Verification

Native 26.3 source was checked for model tint schema and arbitrary Euler rotations.
The merged shaders were compiled/linked on the available standalone OpenGL context
for ordinary, cutout, alpha-only, OIT accumulation and glint variants. GPU samples
checked each helper base alpha, grayscale half/zero fade, exclusion of legacy
alpha254/130 from warning recognition, and faded palette bloom. This is shader/asset validation, not an in-game image
review; the user must apply the pack edits before the new displays can render.
