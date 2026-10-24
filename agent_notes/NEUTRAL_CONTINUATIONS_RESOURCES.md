# Neutral continuations: source and presentation audit (24 October 2026)

Read-only audit of `other/craft/{fotja,ananta,swargia}{,_inv}.mcfunction` in the old LBC source. Author RP is read-only at the path in RESOURCE_PACK.md. No production, resource, template, or build changes were made for this audit. Runtime completeness is reviewed separately by the parent.

| Recipe | Required source flags | Output loot table | Native existing item | Cost | Source tier/model |
|---|---|---|---|---|---|
| fotja | neutralmage + kiuaskivi | sauvojen_tuntija_fotja | LbcWands.FIRE_WAND | 100 | B / lbc:st/fire |
| ananta | neutralmage + tannerkivi | sauvojen_tuntija_ananta | LbcWands.STONE_WAND | 2000 | B / lbc:st/stone |
| swargia | neutralmage + void_curse | sauvojen_tuntija_swargia | LbcWands.RUBY_WAND | 2000 | C / lbc:st/ruby |

All three spawn a second output `lbc:neutral_ball`. Ordinary source consumes one nearby item ENTITY of each donor, which can delete a whole stack. Inventory functions decrement every matching slot among the four player crafting slots. These are source implementation hazards rather than evidence that a native exact one-of-each recipe should delete entire stacks. All grant only the respective criterion `fire`, `stone`, or `void` in `lbc:true_advancements/staffs/neutral`. Sound is enchantment_table.use, volume/pitch1; source 100-particle dust has scale2 and RGB fire(.933,.757,.376), stone(.286,.275,.247), ruby(.839,.376,.933).

Ordinary loot is carrot_on_a_stick, count1, name translate `sauvojen_wand_fire`, `sauvojen_wand_stone`, or `sauvojen_wand_ruby`, light_purple and italic=false. Six lore rows: [type,staff] aqua; [dynamic cost score,mana_on_activate] aqua; `.lore.1`, `.lore.2`, `.lore.3` gold; `.lore.4` yellow; every component italic=false. Cost score holders are stfire_wand_mana_use, ststone_wand_mana_use, struby_wand_mana_use in lbc.math. Custom data craftusable/transformable/staff/magicwand plus stfire, ststone, or struby. Do not rewrite lore into plain strings or silently truncate the last two craft-hint rows.

All name and four lore keys exist in author `assets/minecraft/lang/ru_ru.json` and `en_us.json`. Names: Фотья Эстиас / Fotya Estias; Анантакала / Anantakala; Сваргия Маника / Swargia Manika. Author RP already contains each assets/lbc/items/st/{fire,stone,ruby}.json and assets/lbc/models/item/st/{fire,stone,ruby}.json, plus tier_b/tier_c tooltip sprites. No new presentation assets identified.

These are existing ordinary wand outputs, not three new families. LbcWands already registers ordinary AND Academy fire/stone/ruby with those same models/keys and source costs. Source recipe output is ordinary, never upgraded; do not introduce automatic Academy output. LbcKiuas already owns the Kiuaskivi donor and shared curse; LbcTanner owns Tannerkivi. Reuse these qualified donor items and Neutral Ball; do not reopen donor runtime or already completed Fire/Stone/Ruby upgrades because acquisition recipes reference them. The source lore mentions downstream Fireblaze/Kiuas, wall/Tanner, destruction/distortion/multiply27 continuations; those hints do not expand this recipe scope.

## Source anomaly requiring explicit handling

`swargia_inv` grants advancement to nearest nonspectator @p within14 instead of @s; fotja_inv and ananta_inv use @s. This may credit a nearby bystander when crafter is spectator/excluded or execution context differs. Native crafting should normally credit the actual crafter, but this discrepancy must be surfaced for author ruling rather than silently represented as exact source parity. Ground recipes all use nearest @p, and entity-stack destruction versus one-unit native consumption needs the established crafting ruling applied consistently. No other presentation blocker found. Full Academy presentation parity and actual output runtime are outside this focused audit.

## Native integration

All three exact native recipes now share the existing Ice recipe's Neutral
Ball remainder and original partial-criterion path. The actual crafter receives
the corresponding criterion, following the established native crafting ruling.
The scoped Void loot is enabled with its new native runtime. Current qualification
and author decisions are recorded in [VOID_ARCANUM_PORT.md](VOID_ARCANUM_PORT.md).

The author explicitly chose one merged body, velocity pulling, shared small/large
shader, WILD across hostile contributions, successful F spawning and doubling
upkeep by level. Later follow-ups set the price ceiling176, majority ownership
and equal random ties. These override the source crowd/teleport/double-charge
implementation described below.

Runtime source is `items/arcanums/void_curse{,2,_spawn,_spawn2,_tick}` and
`raycast/voidcurse`, `voidcursed1` through `voidcursed5`. Main and offhand spawn
separate markers for4000 each, four local blocks ahead of the feet-based cast
origin. Source per-hand tick loops over every owned hole, causing double
charging when both hands are active. Hole update runs each ordinary tick via
`tick_not_timestopped`. Near holes move together/count within10, up to five
tiers: direct generic amounts .2/.25/.3/.35/.4; hurt radii1.5/2/4/8/24;
nonplayer teleport radii3/4/8/12/30; movement .13 except max .5. First two
tiers use air-destroy cubes with drops; tiers3/4 strict cubes; max uses the
air template at2% integrity and an obsidian cube. Source owned/allied players
are excluded from targeting/damage; technical roles require the shared guard.
Native attribution must cover damage, displacement and terrain consequences.
Use resident chunks and a bounded live terrain work set rather than queuing
the entire destructive cube anew every tick. The existing BlackHoleVisuals
service is used under the author ruling; no duplicate
slot allocator is needed. The author shader guide has been read, read-only.

## Void Arcanum scoped resources

Parent subsequently authorized only the new scoped loot table and three recipe JSON files. `ported/void_arcanum.json` copies the first entry of the project's modern `transformables_arcanum.json` without changing components, modifiers, weights, or quality; only item ID becomes `skywars2:void_arcanum`. Its source base is repeating_command_block, tier A, model `lbc:arcanumblackhole`, gold nonitalic `void_arcanum` name. Eight lore rows: aqua type/arcanum; aqua literal `6 ` plus magic_damage; aqua score void_arcanum_mana_tick_sec plus mana_on_use; aqua score void_arcanum_mana_activate plus mana_on_activate; three aqua void_arcanum.lore.1/.2/.3; yellow craft_with_neutralmage. All original component structures and italic=false are preserved. Flags are craftusable, transformable, arcanum, arcanumt, void_curse.

Read-only author RP verification: all ten required translation keys exist in both assets/minecraft/lang/ru_ru.json and en_us.json, including shared type/arcanum/magic_damage/mana_on_use/mana_on_activate. Names are Око вечной тьмы / Eye of Eternal Darkness. Original lore says the hole pursues mobs and players and a new hole is created by F (English: swapping items in hands). Author assets/lbc/items/arcanumblackhole.json points to lbc:item/arcanumblackhole; assets/lbc/models/item/arcanumblackhole.json exists. No template additions needed.

Source config sets activation4000 and tick11, then computes void_arcanum_mana_tick_sec as tick*20 =220. Before the parent's runtime implementation, no Java initialization of these exact score holders was found; runtime must initialize all three, especially220 for dynamic lore. Resource JSON files neutral_fire_combine, neutral_stone_combine, neutral_ruby_combine contain only their corresponding skywars2 recipe type. No existing loot, Java, build, or template changed by this resource subtask. Runtime rules for cross-owner merge, pull, and visuals remain with parent/user.
