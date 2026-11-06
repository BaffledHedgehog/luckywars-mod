# TMT procedural ITEM-atlas overlay ? 6 November 2026

Prepared overlay only. The author resource pack is read-only and has NOT been modified.

## Server contract (v1)

- `minecraft:item_model = "lbc:tmt/procedural"` for newly generated TMT outputs.
- Generate a visual seed once per output and persist it through copy/save/load/rewind.
- `minecraft:custom_model_data.colors[0] = 0xFA0000 | (seed & 0xFFFF)`.
  Preserve unrelated custom-model-data fields and unused color entries.
- Default color is decimal `16384000` (seed 0). It is an intentional deterministic fallback.
- This replaces the old random `lbc:tmt/tmt_1..12758` ITEM_MODEL selection.
  The independent equipment asset selection `lbc:tmt/tmt_1..707` is unchanged.

The dedicated opaque carrier sprite has exact RGBA `(31,197,83,255)`.
Only this texel AND raw tint prefix 250 activate the shader. Prefix 250 avoids
existing zone (253), black-hole (252), and nuclear (251) carrier tints. Do not
classify other items by approximate green or any broad color threshold.

65,536 visual seeds are available; different seeds can still produce colliding
images. The generator can retain a larger gameplay/random seed separately.
Client tint source reads only the explicit component, not the render invocation
seed, holder, name, camera, time, or item display context. It supplies opaque tint
index 0 to every face. Vertex shader decodes it before directional lighting.

## Apply / merge

Copy the `assets/` contents into the author pack yourself. The two item shader
files are complete merges of the ACTUAL author files read on 6 November, retaining
zone, nuclear/black-hole carrier suppression, meditation ring/countdown, alpha
markers, ordinary lighting/fog, glint and OIT. `AUTHOR_BASE_SHA256.json` records
the source shader versions. If either author shader changed since these hashes,
merge the TMT declarations and marked branches into the new source; do not
replace newer shader work blindly. The new include/model/item-definition/PNG
paths are additions. No language or atlas JSON replacement is needed.

Reload resources (F3+T) and inspect one persisted seed in inventory, first/third
person hands, ground drop, and after reconnect/reload. Repeat with enchantment
glint and resource settings. The template is not installed automatically.

## Sampling and stability

Only the material atlas already bound as `Sampler0` is sampled. The new carrier
is an item texture, so its model uses the current ITEM atlas. No block atlas,
extra sampler, sprite-name list, source sheet, or thousands of new PNGs are used.
The seed chooses output resolution16/32/64/128/256 with legacy weights
7248/2260/1871/780/599 out of12758. Patch placement uses32 logical units;
output texels retain the chosen resolution rather than snapping all items to32.
Source crop density grows with resolution, so higher-resolution outputs contain
additional atlas detail. Six overlaid patches have quarter turns/reflections,
0.5/1/2 scales, colors and rectangular/diamond/stripe masks. Two atlas-backed
underlying crops reduce padding-only output, retaining at most8 atlas fetches
per fragment. A colored connected silhouette is only the final fallback where
both backing source samples are transparent; no sprite occupancy map or
nonempty-crop guarantee is claimed.
Source alpha markers are normalized and cannot dispatch existing visual effects.

The explicit thin six-face model gives every face the complete carrier UV range;
it avoids generated sprite side strips. Local quad coordinates use the existing
native four-sequential-vertices convention already used by meditation warnings.
Image identity is independent of screen size/distance and light; brightness still
responds to normal item lighting/fog. Different faces can orient the collage
differently according to their native vertex order. Atlas repacking changes the
chosen source patches. Animated source sprites can change sampled content during
animation: the SEED and patch placement remain stable, but pixels are not frozen.
No automatic atlas dump or dynamic sprite-UV map is needed.

## Optional old-resource removal

No removal is performed. Retaining old numbered resources permits old saved
items to keep their models. Removing only unused model/item JSONs does not stop
all numbered PNGs from entering a directory-sourced atlas. To reclaim their
atlas memory after migrating old references, the author may remove the OLD
numbered `tmt_N` files in:

- `assets/lbc/items/tmt/`
- `assets/lbc/models/item/tmt/`
- `assets/minecraft/textures/item/tmt/`

Do not remove `procedural.json` or the new `assets/lbc/textures/item/tmt/` carrier.
Do not remove any equipment JSON/texture files: wearable equipment is a separate
render path and this patch preserves all 707 equipment assets. Back up and check
saved items/other references before deleting legacy numbered resources.

## Evidence and remaining limits

Run `python resourcepack_template/updates/tmt_atlas_2026_11_06/check_tmt_atlas.py`
from repository root with installed `moderngl`, `numpy`, and the local Minecraft
26.3 JAR. The check reads author resources and writes only `GPU_RESULTS.json`
here. It compiles/links all eight current item variants on an actual GPU, renders
a synthetic item atlas, checks 24 ordinary/effect material+tint cases against
unmodified author shaders, repeat-seed identity, distinct-seed variation, zero
seed channels, sequential quad offsets0/4/12, and six viewing angles. An additional GPU pass
checks resolution choices against an independent integer reference for every
transportable seed (65,536). Representative rendered outputs have185/726/2753/
6220/46402 distinct opaque colors at16/32/64/128/256 respectively, demonstrating
actual source detail above32. Full seed counts and current asset SHA-256 values
are recorded in `GPU_RESULTS.json`. The neutral-overlay fixture uses alpha1,
matching vanilla overlay mixing so source texture detail remains observable.

The passed standalone GPU checks are not a physical Minecraft GUI/hand/drop
capture, a full source-texture visual review, or a performance/VRAM measurement.
No live client was reloaded. Transparent silhouette in entity outlines still
uses the existing outline render path; this item-shader patch does not change it.
The extra bounded fragment work trades memory for sampling/arithmetic; actual
GPU frame time and atlas allocation must be measured in the intended client.
