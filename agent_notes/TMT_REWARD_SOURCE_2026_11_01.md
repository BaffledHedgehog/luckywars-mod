## Current author direction and shader feasibility,1 November2026

This section supersedes recommendations and inferred LIVE Paper status below.
Code or registration in the Paper tree alone does not prove historical execution;
the author specifically corrected that mistake for Absurd Child. For TMT the
chosen source is the FULL DATAPACK generator, creation unjammed. The author then
deferred TMT until the other item mechanics are available, asking for meaningful
random properties from all available mechanics and all legacy generator families.
Skip useless tags. Attribute/enchantment pools must be uniform over all current
entries; add missing modern sounds/use animations too. Earlier advice preserving
an unreachable index-zero entry is superseded by this explicit correction.

Incomplete generator/activation code has been archived outside src under
[drafts/tmt](drafts/tmt/README.md); no runtime registration, hooks or checks are
claimed. Its source-table extractor reproduces the literal legacy arrays exactly.
Do not mistake that extraction check for runtime generator qualification.

Read-only inspection of the actual RP found12,758 item PNGs under
assets/minecraft/textures/item/tmt/:48.04MiB on disk and243.64MiB summed RGBA8
pixels BEFORE atlas padding/packing. These are not measured GPU allocation.
Sizes:7,248 at16x16;2,260 at32x32;1,871 at64x64;780 at128x128;599 at256x256.
Equipment textures are additional and excluded from these counts.

Procedural collage in item shaders is feasible: a persistent seed per item can
select pixelated patches, rotations/reflections, scales, colors and alpha masks.
Use a dedicated explicit carrier marker; never classify ordinary textures by a
broad color heuristic. Seed transport (e.g. model tint) needs a real render test
for GUI/hand/drop agreement, without time/camera/name-dependent rerolls.

The current renderer binds a single material atlas as Sampler0; declaring another
sampler does not make the client bind the other atlas. Official Mojang documents
separate block/item atlases and a one-atlas-per-item-model restriction:
https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11 .
Local RenderTypes.java and actual RP item.vsh/item.fsh were inspected read-only.
An unmodified-client design can instead include source sprites once in one shared
atlas/source sheet, then compose combinations in GLSL. Exact atlas arrangement
and model selection require a prototype. Random arbitrary atlas crops need no
sprite-name map; selecting named whole sprites does need UV metadata because
atlas packing is dynamic. Do not promise immediate access to both existing
atlases, identical pixel-edge extrusion or automatic resource-pack adaptation.

A small fixed source sheet is predictable; adding every sprite to an existing
atlas may increase its power-of-two allocation. Shader work replaces memory with
extra fragment sampling/arithmetic, so compare GPU time as well as pack size and
VRAM. Armor rendering is a separate path; item.fsh alone does not replace its
707 equipment assets. No shader/RP changes or GPU benchmark were performed.

# TMT reward dependency audit ? 2026-11-01

Read-only audit except this new note. No source/resources/build/server changes. No final mode-readiness claim.

**Key finding:** legacy Paper already intercepts TMT with a native, approximate generator. The Fabric port has some consumers of generated markers but no located token-to-output generator. A token loot table alone is preparatory and must not be reported as generated-item parity.

Path prefixes: `LBC` = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`; `Paper` = sibling `plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java`; `Native` = `src/main/java/me/skywars2/game` in this repository.

## Trigger and source generation

- `LBC/loot_table/tmtrainer_drop.json:11-45`: count-one repeating-command-block token, tier_u/drop_trap, translated trap_tmt name/trap_desc lore, custom data `{tmt:1,drop:1}`. Brown block has two references; current native brown table references at lines 147/239. Do not use the obsolete table component schema unchanged.
- `LBC/function/items_tick_tag.mcfunction:5,19` tags loose drop tokens; `drops/specialdrops.mcfunction:78` invokes `drops/tmt` at token position. `drops/tmt.mcfunction:1-2` summons a count-one stick entity tagged tmt, runs tmt_at as its selected entity within 0.01. Generator entry `drops/tmt_at.mcfunction:1-9` chooses uniformly 1..5: carrot_on_a_stick, bow, snowball, crossbow, golden_apple, then basics, then select_random. The output count is one; no giant-stack requirement is implied.
- `drops/tmt/set_basics.mcfunction:1-2` installs the complete base custom-data marker set (tmt_item, weapon/tool/spell/armor/accessory/shooter/summoner etc., mode/type zero); golden apple receives its special continuation. `set_gapple.mcfunction:1` gives food nutrition/saturation 20, always edible, two-second consumable. `set_gapple_2.mcfunction:1-14`, `get_sound`, `set_sound` choose ten use animations and sound index 0..1734 and assign consumable sound/animation. No additional custom consume effects occur in these TMT generator files; inherited golden-apple effects and downstream marker behaviors must remain.
- `select_random.mcfunction:1-14`: exactly seven independent table draws with replacement from tags indices 0..1048; `write_random_nbttag:1-3` and `set_nbttag:1` assign integer values dynamically. Duplicate keys overwrite in order. Name once (line 16), six lore lines (18-23). Name/lore choose names index 0..11681; six independent hex digits, text-vs-translate and italic/bold/underlined/obfuscated/strikethrough probabilities are defined by select_random_name/add_random_lore. `setname:1-2` removes CUSTOM_NAME and writes ITEM_NAME, not custom-name display metadata. Preserve strings and translation keys verbatim, including glyphs, rather than translating on the server.
- `select_random:26-41`: equipment slot head/chest/legs/feet uniformly; intermediate res=tmt_1..4 is assigned but **not consumed** by random_equippable. Actual `random_equippable:1-2` uses equipment asset `lbc:tmt/tmt_1..707` and ITEM_MODEL `lbc:tmt/tmt_1..12758`, independently. Do not mistake dead res scratch storage for the output model range.
- `select_random:44-72`: one guaranteed attribute attempt plus four 20%, eight 10%, sixteen 5% attempts. `select_random_attribute:1-24`: id 1..INT_MAX, attribute table index **1..32**, amount -50..100 /100 then independent sequential chance1/chance10/chance10 overrides (-1e9..1e9, -200..1200, -100..400)/100; last successful override wins. Ten slot groups; three operations. `write_random_attribute:1` appends modifiers. Preserve source index domain (not every table entry is reachable).
- `select_random:75-95`: four 20% plus seventeen 10% enchantment attempts; `select_random_enchantment:1-8` index **1..32**, initial level 1..5, sequential independent 10% overrides to 1..255 then 1..15. `write_random_enchantment:1` overwrites duplicate enchantment keys. Verify exact attempt counts with a typed fixture before implementation; no command interpreter is necessary.

## Storage and resources

`function/config.mcfunction:122` calls `drops/tmt/set_storage`; that file lines 1-12 clears and populates six lbc:tags arrays via set_storage_nbttags, names, hex, sound, attribute, enchantment. Scratch `lbc.math` holds i/i1, str/val, name, color digits, booleans, txt/slot/operation/idint/valued/value/animation/snd; scoreboard sel/if use lbc.math. A native implementation needs immutable typed data tables and local random state, not persistent command storage or function execution. Retain ordered source values and reachable index bounds, including attribute/enchantment index-one exclusion.

Original RP verified read-only at `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`, as documented by `agent_notes/RESOURCE_PACK.md:16-24`. Scoped exact filename audit: `assets/lbc/items/tmt/` contains exactly 12,758 numbered JSONs, contiguous `tmt_1.json` through `tmt_12758.json`, no missing indices. Their first/last files lines 2-4 use type `model` and model IDs `lbc:item/tmt/tmt_1` / `_12758`. `assets/lbc/equipment/tmt/` contains exactly 707 numbered JSONs, contiguous `tmt_1.json` through `tmt_707.json`, no missing indices. First equipment lines 3-10 reference humanoid `lbc:tmt/tmt_1` and humanoid_leggings `lbc:tmt/tmt_2`; last references `_1413` / `_1414`. These correspond directly to source ITEM_MODEL keys `lbc:tmt/tmt_N` and equippable asset IDs `lbc:tmt/tmt_N`; preserve both independent ranges in typed components. Top-level `assets/lbc/items/tmt_1.json`, `tmt_4.json`, and `equipment/tmt_1.json` are absent, so Paper's older four top-level item keys are not evidence for visual correctness with this pack. `pack.mcmeta:3-10` declares min/max format `[97,1]`; directory name 1.21.8 does not establish actual pack format. Complete scoped direct-reference audit: all 12,758 item definitions parse as type `model`, and all their referenced model JSONs exist. `assets/lbc/models/item/tmt/` has exactly 12,758 numbered models, contiguous 1..12758. Endpoint model JSONs use parent `minecraft:item/handheld` and layer0 `minecraft:item/tmt/tmt_N`; the textures are in the **minecraft namespace**, `assets/minecraft/textures/item/tmt/`, with exactly 12,758 numbered PNGs, contiguous 1..12758 (endpoint files exist). Every layer reference across all 707 equipment JSONs exists: 707 humanoid textures (odd 1..1413) and 707 humanoid_leggings textures (even 2..1414), 1,414 unique PNG references, zero missing. Model texture references beyond the two endpoints were not individually parsed in this bounded check. No RP files changed.

Remaining visual qualification is runtime rendering/asset loading in the intended client, source translations/fonts/glyph mappings and consumable sound availability. The exact source key/range mappings already exist in the user's pack; no new textures or generic fallback models are warranted by this audit.

## LIVE Paper override differences

`Paper:1306-1312` registers lbc:drops/tmt override. Native token dispatch `4275`, `4308-4314` matches tmt=1, spawns one output and decrements one token. `7252-7258` uses world.dropItem without source-player attribution. `8565-8597` generates five base materials, **ten** random tag draws, six lore lines, custom display name, **only lbc:tmt_1..4**, attributes and enchants. It omits the source equipment component and golden-apple consumable customization here. `8615-8652` amount overrides are else-if and probabilities differ from source independent overwrites; `8655-8679` levels similarly else-if. `8712-8785` loads pools from packaged legacy .lwfn resources; this does not make its runtime a source-faithful generator. Paper behavior must be acknowledged as active, not assumed equal to datapack behavior. `start_ignite.bat:1-5` launches ignite.jar; no separate Ignite source directory was found in this bounded audit.

## Existing consumers and bounded native feasibility

Literal tmt search is insufficient: generated tags activate already ported engines through custom-data predicates. `Native/LbcChaosRewardItems.java:30-44` illustrates typed key consumers (unstable_spellbook/anal_whip), gameplay/observer/TimeStop/MAG gates; `:145` shows attributed output setThrower plus current item epoch. `LbcChaosArmor.java:51,86` reads its armor marker and explicitly shares TMT-compatible jump selection. `LbcItems.java:1248` and `LbcSpecialFireballs.java:86` already retain tmt_item projectile entities for overlapping effects. These are real partial consumers, not proof of full multi-marker TMT dispatch: one use can need several source engines in ordered succession.

`LBC/function/items/tmt_item.mcfunction:1-3` calls trigger_smth and revokes using_item advancement. `drops/tmt/trigger_smth:1-4` marks tmticked and runs swords, armor, crossbow_charging. The not-bow variant additionally runs crossbows/spells; not-crossbow runs bows/spells; not-damaged only marks tmticked. `items/bows:15`, `crossbows:25`, `damaged:22` invoke those guarded continuations; `players_tick:1` resets tmticked. A typed per-event reentrancy/once guard and explicit native engine delegation can reproduce this without a generic command/function interpreter.

Recommended bounded next slice, once parent creation policy is resolved: typed TMT tables and generator, one-token loose-item consumer using existing actor/epoch machinery, component-preserving output, then explicit TMT activation adapter into existing engine owners. Verify seven tag writes, all reachable extremes/overwrites, ITEM_NAME and six lore components, apple food/use metadata, equipment/model ranges, count-one output and stacked token debit, no immediate pickup/double execution, actor attribution, TimeStop defer/resume, and Vanity snapshot/rollback of random/output state. Apply existing consumer MAG/role/team rules; **do not infer a new MAG gate for output creation** merely from marker effects. Root reports no source MAG suppression check and awaits creation-policy clarification. No native generator or complete activation consumer is claimed by this audit.
