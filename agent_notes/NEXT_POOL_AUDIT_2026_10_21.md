# Shooter pools and next bounded family - 2026-10-21

## Superseding user policy and qualification

The twelve tags and pending questions below describe the historical source
audit. User answers supersede them: convert every non-invisible material via an
offline texture-color nearest-full-wood lookup, preserve exclusively wooden
shapes only without a block entity, and spill stored container items.
The importer no longer verifies/imports the obsolete tag closure. Public table
`lbc:ported/woodificator`, generated palette and runtime are qualified by
four native matches on `1f5e4b42...`; see [current contract](WOODIFICATOR_PORT.md).
The audit's Reforge exposure finding remains valid; this batch does not qualify
the unfinished ordinary/extended shooter outcomes.


Read-only source audit; no build/server/code/resource changes or runtime verification. Read project AGENTS, ARCHITECTURE and agent_notes/AGENTS. User annotations remain authoritative; kill attribution is required for terrain consequences even where legacy omits it.

## Actual pool boundary

Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.

- `lbc/loot_table/transformables_shooter.json`: 8 weighted entries, 7 distinct items. Phantom Knives, Mobgun (two rows: weights 10 and 10000), KFC Gun, Razorpine, Attack Helicopter, Laser Minigun, Woodificator. The last three have no native item handlers/registration in current Java source.
- `lbc/loot_table/transformables_shooter_all.json`: 7 direct entries: ordinary pool (weight7), Stalin, Magnum, Razorterra (`tuntijaa_terra_razor` table), Shit Cannon (`govnomet`), helicopter boss, SLR. Recursive total: 13 distinct item outcomes. Of its six additions, Razorterra and SLR have existing native owners; Stalin, Magnum, govnomet and helicopter boss remain unported. Search by source table ID alone misses native Razorterra alias.
- Native ownership: `src/main/java/me/skywars2/game/LbcShooters.java` registers PINE, SLR, KFC_GUN, PHANTOM_KNIVES; `LbcMobgun.java` owns launcher. `LbcItems` owns service lifecycle and `LbcWands` handles input. The remaining shooter string matches in KFC timer reset/Tanner exclusions are not implementations.
- Both pool files already enter resources via `build.gradle` processResources (ordinary line264, all line139), for Reforge closure. `LbcBolts.NORMAL_REFORGE`/`ULTRA_REFORGE` select them; its rollTable path can expose unfinished items. `skywars2/loot_test_catalog.json` limits public autocomplete to ported prefix/explicit entries; it does not restrict internal rolls. Therefore neither pool is ready for broader acquisition. Closed public suggestions do not establish runtime closure of Reforge.

## Recommended next single item: Woodificator

Small, synchronous terrain family; does not complete either pool. Exact legacy entry: `lbc/loot_table/transformables_shooter.json` lines484-538: count1 carrot_on_a_stick, weight10, quality8, tooltip `lbc:tier_d`, model `lbc:woodificator`, white nonitalic name key `woodificator`; aqua nonitalic type+shooter line, white nonitalic `woodificator.lore`; custom data `{transformable:1,shooter:1,raycastspell:1,woodificator:1}`. Preserve keys and formatting. The older RESOURCE_PACK.md 26.3(1) path was absent; root supplied current 26.3 path. Read-only current pack verification: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/lang/ru_ru.json` contains all four Woodificator keys; `assets/lbc/items/woodificator.json` selects `lbc:item/woodificator`. No pack edits.

Actual activation is in **lbcr**, not the lbc workingitems namespace:

1. `lbc/function/items/spells.mcfunction` lines5/11 dispatches raycastspell main/offhand; rejects caster nomagic_active and nearby nonspectator nomagic_active player (20 blocks), offhand respects `lefthand swrg.math` switch.
2. `lbc/function/workingitems/spell.mcfunction` and `spell_left.mcfunction` route unupgraded item to `lbcr:raycast` / `raycast_left`.
3. `lbcr/function/raycast.mcfunction` and `raycast_left.mcfunction` line30 check woodificator and absence of stopper_magic marker within20, start at eyes. Dispatcher sets Distance0 and raycaster tag; corresponding exit removes tag. No woodifier mana cost, cooldown, durability change or entity hit exists in these branches.
4. `lbcr/function/raycast_woodifier_loop.mcfunction` (5 lines) increments Distance; emits one oak_planks block particle; with `lbc/predicate/chance5.json` (0.05) plays roots.break master volume0.5 pitch1; calls fill; recurses +1.5 local forward while Distance1..50. Thus 51 samples at 0..75 blocks from eye start, straight through terrain, no collision stop, no persistent projectile/lifetime. All work is one command invocation chain.
5. `lbcr/function/raycast_woodifier_loop_fill.mcfunction` has 12 ordered `fill ~2 ~2 ~2 ~-2 ~-2 ~-2 <species>_planks replace #minecraft:woodifier/<species> strict`: oak, birch, spruce, cherry, mangrove, dark_oak, crimson, warped, bamboo, jungle, acacia, pale_oak. Each cube is inclusive 125 cells; at most6375 visits before dedup, with overlap. Preserve floor/block-coordinate semantics and species order.

Source tag files: `minecraft/tags/block/woodifier/{oak,birch,spruce,cherry,mangrove,dark_oak,crimson,warped,bamboo,jungle,acacia,pale_oak}.json`, 12 files. Direct value counts respectively50,40,24,7,13,62,6,72,34,26,32,26 (includes nested tags/duplicates; not expanded unique blocks). Their nested color/flowers/corals/anvil/pots tags need packaged closure, not just these twelve files.

Important source behavior for user decision: spruce includes bedrock, ancient_debris and spawner; birch includes end_portal_frame and water (water repeated); warped includes end_gateway; acacia includes lava/fire. Oak includes chests, ender chest and many workstations. This is not merely colored decorative blocks. Source strict replacement supplies no explicit container-copy/drop logic; exact native strict block-entity/update behavior must be matched deliberately. Do not silently add protected-block exclusions or cost/cooldown as though present in legacy. No direct damage, but support/terrain changes need existing indirect-kill ownership contract.

Acquisition/advancement: no standalone woodificator table found; canonical source entry is ordinary shooter pool, nested in all pool and Reforge. `lbc/advancement/true_advancements/shooter/woodificator.json` is inventory_changed (not cast), excludes playerGameType3, matches transformable+shooter+woodificator data, uses source model/name keys; `kazahstan.json` has it as display parent. General pools should stay separate; a ported single-item test table can provide bounded manual acquisition.

Reuse existing `LbcWands` input/offhand and suppression paths, `LbcItems` lifecycle, existing terrain/block replacement helpers and `LbcIndirectKills` support attribution. No new projectile entity, ticking job, owner framework or random-pool rewrite is implied by this family. Root is checking exact helper API before implementation selection.

## Concrete resource preparation

Added `tools/import_lbc_woodificator.py`; ran successfully with Python, without Gradle/server. It uses `migrate_loot_schema`, `select_ordinary_wand_entry`, `build_single_entry_table` and `write_table`; produces only `src/main/resources/data/lbc/loot_table/ported/woodificator.json` with native ID `skywars2:woodificator`, preserving source modifier payload, weight/quality, names, lore, model and colors. Random pools remain unchanged.

The twelve exact source tag files already existed under `src/main/resources/data/minecraft/tags/block/woodifier/`: `oak.json`, `birch.json`, `spruce.json`, `cherry.json`, `mangrove.json`, `dark_oak.json`, `crimson.json`, `warped.json`, `bamboo.json`, `jungle.json`, `acacia.json`, `pale_oak.json`. Importer preflights payload equality and refuses mismatched existing shared tags before writing. It traverses source-defined nested tags: all 28 files (12 woodifier + 16 shared colors) already match source; none overwritten. Direct child references: minecraft:anvil, black, blue, brown, corals, cyan, flower_pots, flowers, gray, green, light_blue, light_gray, lime, magenta, orange, pink, purple, red, white, yellow. Anvil/corals/flower_pots/flowers have no override file in this legacy source, so resolve vanilla; other sixteen are existing source color overrides. No tag exclusions changed while user decisions are pending.

Read-only current 26.3 pack validation passed: RU/EN `woodificator`, `woodificator.lore`, `type`, `shooter` keys; item model points to lbc:item/woodificator, parent minecraft:item/handheld, texture minecraft:item/woodificator exists as PNG; tier_d background/frame tooltip PNGs exist. No template or source/RP writes. Source RU name: 'Интересная трость'; lore: 'Превращает что угодно в дерево. Одревеняющая пушка!'. Runtime implementation and packaging are tracked by the main port task.
