# TMT item-atlas audit — 7 November 2026

The procedural visual already exists in
`resourcepack_template/updates/tmt_atlas_2026_11_06/`. Reuse that overlay. A second
shader/model pipeline or new sprite set is unnecessary. This audit did not touch
the actual author resource pack, shared Java, deleted templates, or equipment.

Before editing the existing prepared check script, all **282** author paths from
`build/verification/tmt-remaining-domains-2026-11-07/baseline.json` were verified
unchanged, including absent paths. None of the atlas overlay paths are protected
author inputs. The only overlay edit in this audit adds a transparent-source RGB
control to `check_tmt_atlas.py`; no shader/model/texture changed.

## Actual author pipeline and native client source

The actual pack was read at
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`.
Its current core shaders exactly match the overlay's recorded author base:

| Author source | Current/base SHA-256 |
| --- | --- |
| `assets/minecraft/shaders/core/item.vsh` | `a43c082532954c2d689ce099fb55b7908e99dde25b21a9a3d2498821da675b6e` |
| `assets/minecraft/shaders/core/item.fsh` | `9daa3613c500d7f6e2072f417d643db458742818a9a22ec29531ccff29144089` |

The overlay is a merge into that author pipeline, preserving zone borders,
nuclear/black-hole carrier suppression, meditation warning ring/countdown,
ordinary alpha markers, lightmap/overlay/fog, glint and OIT. It does not replace
the entity shader or postprocessing includes. All six overlay asset SHA-256
values still equal the existing `GPU_RESULTS.json` asset manifest.

Read-only evidence came from the current local Minecraft **26.3** sources JAR
under `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/`
and the vanilla client JAR in `C:/Users/yarik_temp/.gradle/caches/fabric-loom/26.3/`:

- `RenderPipelines.java:229`: both ITEM and OIT_ITEM snippets select `core/item`
  vertex/fragment shaders and native entity-format quad vertices. Entity-format
  vertices do not imply the `core/entity` shader.
- `RenderTypes.java:207`: ITEM_CUTOUT and glint/translucent variants bind the
  supplied material texture as `Sampler0`, with existing overlay/lightmap/glint.
- `Sheets.java:35`: item and block materials have separate atlas bindings;
  `ITEMS_MAPPER` uses `TextureAtlas.LOCATION_ITEMS` with the `item` prefix.
- Vanilla `assets/minecraft/atlases/items.json`: the directory source is `item/`,
  plus item armor-trim permutations. Blocks have their own `blocks.json` atlas.
- `ItemStackRenderState.java:242`: GUI/hand/ground/display contexts forward their
  tint arrays/quads through the same `submitItem` entry point.
- `ItemFeatureRenderer.java:38`: the main item submit selects each quad's item,
  glint or special-glint render type and forwards its declared tint index into
  the native quad writer. Outlines use a separate submission path.
- `CustomModelDataSource.java:27`: tint index zero reads the explicit component
  color and returns `ARGB.opaque(value)`, independent of holder/display context.
- `TextureAtlas.java:83`: atlas sampler is nearest/clamp. `texelFetch` in the
  overlay samples explicit integer texels without cross-sprite filtering.

Consequently the carrier at `lbc:textures/item/tmt/procedural_carrier.png` binds
the **item atlas only**. The fragment shader samples that already-bound Sampler0;
there is no block atlas, extra atlas sampler, material lookup table, or generated
100 MB texture collection. The prepared carrier is a small explicit marker PNG.

## Existing implementation and server wiring

`LbcTmtRuntime.visual(ItemStack)` already sets `ITEM_MODEL = lbc:tmt/procedural`
and updates only custom-model-data color zero to
`0xFA0000 | (tmt_texture_seed & 0xFFFF)`, preserving other colors/fields.
`LbcTmtProperties.generate` persists the seed once, calls this method, and keeps
equipment's independent source asset range **1..707**. There is no separate
`LbcTmtPresentation` class to integrate.

All ordinary visual references agree with the prepared item definition/model.
Six explicit thin-model faces each have full UVs and tint index zero. This avoids
generated sprite side-strip UVs. The vertex shader reconstructs local quad UVs
from the existing native sequential four-vertex convention and decodes the raw
encoded tint before lighting. Both exact RGBA `(31,197,83,255)` **and** tint prefix
250 are required; ordinary items cannot enter through a broad color heuristic.
Reserved existing prefixes 251/252/253 are preserved.

There are **65,536 transported visual seeds**. The saved server seed is wider,
but its upper bits do not add visual variants. Output resolution is 16/32/64/
128/256 with original measured source weights 7248/2260/1871/780/599 of 12758.
Six overlaid item-atlas crops use deterministic patch placement, quarter turns,
reflections, variable scale, tint and masks; two base crops reduce empty padding.
Source alpha below 0.1 is skipped. A bounded colored connected silhouette remains
where both base samples are empty, so this is not a sprite occupancy/nonempty-
crop guarantee. Higher resolution increases source detail instead of scaling the
same low-resolution result. Maximum collage fetch count is eight per fragment.

Visual identity does not depend on camera, current holder, time or display
context. Normal lighting/fog still changes apparent brightness. Atlas repacking
can change sampled crops, and animated atlas sprites can change their sampled
pixels. These are explicit limits, not frozen-texture promises.

Native component copy/codec transport preserves the seed/model/tint. Existing
`TmtPropertyChecks` exercises all five carriers, full component codec roundtrip,
seed endpoints and procedural model/tint. Native clone NBT uses
`LbcCloneSkills.NativeImage` save/load plus metadata copies; it does not call the
visual generator. `LbcTmtComponents` projects only private native payload types,
so it does not overwrite the generated appearance during native callbacks.

## Verification scope and command

Existing `GPU_RESULTS.json` records eight compiled/linked item shader variants,
24 comparisons against unchanged author material/tint behavior, repeat-seed
identity/distinct-seed variation, quad offsets 0/4/12, six camera angles, all
65,536 GPU/CPU resolution choices and representative detail at every resolution.
All recorded asset hashes still match. This is **standalone synthetic-atlas GPU
evidence**, not physical Minecraft GUI/hand/drop screenshots or a frame-time/VRAM
measurement. Current source inspection proves the common native path; it does
not turn the synthetic check into an in-game capture.

One genuine evidence gap was corrected in the prepared Python check: previously
the collage source atlas was opaque everywhere, so transparent-padding skips were
not exercised. The added control keeps the carrier opaque, changes only the RGB
of otherwise fully transparent atlas texels, and requires identical finite visible
output for six seeds spanning all resolution choices. Its result is recorded as
`transparentSourceRgbIgnoredSeeds` when the root reruns the script.

The script parses successfully. Installed `moderngl`/`numpy` are available.
No GPU execution, JVM, Gradle, semantic/JDT tools, server, or subagent was started
by this audit. Root can rerun sequentially from repository root:

```powershell
python resourcepack_template/updates/tmt_atlas_2026_11_06/check_tmt_atlas.py
```

That check reads author/native resources and writes only the prepared overlay's
`GPU_RESULTS.json`. It compiles against currently resolved author/native includes;
the older result records core/overlay hashes rather than every include dependency.

Status: **implemented and previously synthetic-GPU qualified; current extra
padding control awaits rerun; author pack not installed; physical-client visual
confirmation and real GPU frame-time measurements remain unverified.** No new Java
visual wiring is needed. Apply only the existing prepared overlay after confirming
the author base hashes; keep unrelated author assets and equipment unchanged.

## Root GPU verification ? 7 November 2026

The root reran the prepared check on NVIDIA GeForce RTX 4080 SUPER.
The current `GPU_RESULTS.json` reports passed=true, eight compiled/linked
variants, 24 unchanged unrelated-material controls, all 65,536 CPU/GPU
resolution choices, and transparent-source RGB invariance for seeds
0/3/5/9/34/65535. The added padding control is therefore now verified.
The earlier audit's pending-rerun wording describes its original boundary.
This remains standalone synthetic-atlas evidence; no physical Minecraft
GUI/hand/drop capture, frame-time benchmark or author-pack installation
is claimed. The existing six-asset overlay is the deliverable.
