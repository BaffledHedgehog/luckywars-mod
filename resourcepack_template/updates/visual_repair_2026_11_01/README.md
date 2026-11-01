# Black holes, laser bloom and Techno radar — 1 November 2026

Copy this directory's **`assets/` folder into the resource-pack root**, merging
its contents with the pack's existing `assets/` folder and replacing the three matching existing files. The two new shader
files must also be copied. Reload resources with F3+T. The actual resource pack
was only read; no server restart or JAR update is needed for these corrections.

Files supplied:

- `assets/minecraft/post_effect/end_of_frame.json`
- `assets/minecraft/post_effect/entity_outline.json`
- `assets/minecraft/shaders/core/particle.vsh`
- `assets/lbc/shaders/post/polymorph_outline_bloom_sample.fsh` (new)
- `assets/lbc/shaders/post/polymorph_outline_bloom.fsh` (new)

These are replacements based on the current installed pack, including its signed
glow, weapon spread, zone, nuclear, black-hole and polymorph changes. They are not
older copies of deleted template files. `source_manifest.json` records the hashes
of the installed files used as the patch base. If those files have since changed,
merge the corresponding changes below instead of overwriting newer work.

The client log gives the direct black-hole/laser cause: at 21:51:32 and 21:52:18,
`logs/latest.log` rejects the **entire** `minecraft:end_of_frame` post chain because
it accesses `minecraft:entity_outline`. Native 26.3 `LevelTargetBundle.MAIN_TARGETS`
permits only `minecraft:main`; `entity_outline` is available inside the separate
outline chain. Rejection disables the black-hole compositor and ordinary bloom
together even though their own models, packets and shaders are present.

The repaired end-of-frame chain uses the existing ordinary bloom sampler again.
Polymorph sphere bloom now samples its exact outline marker inside the legal
outline chain, before Sobel filtering. It retains the existing blur/color design
and does not recognize pink terrain. Its composite explicitly preserves scene
alpha, which contains black-hole/nuclear control records at this stage. All other
post passes and the black-hole carrier ABI are unchanged.

Techno/MK2 radar has a separate depth regression. Its two particle color markers
were projected to clip depth zero. Native 26.3 uses reverse depth: zero is far,
and particle depth tests use `GREATER_THAN_OR_EQUAL`. The two special markers now
use `z = w`, the near plane, so terrain does not hide their circles. Ordinary
particles retain their original depth and occlusion. Colors, circle geometry,
audience, particle count and server gameplay remain unchanged.

Validation: `tools/check_visual_repair_2026_11_01.py` audits native allowed targets,
compiles actual shader variants, renders the unchanged black-hole tracer and cyan
bloom, verifies sphere-only emission and exact alpha preservation, and compares
both radar markers against an occluding depth buffer. Evidence is written under
`build/verification/combat-visual-flight-2026-11-01/`. Offscreen GPU results verify
these pipeline contracts; they are not an in-game client visual qualification.
