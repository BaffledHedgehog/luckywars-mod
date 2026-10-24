# Erecto acquisition and presentation audit — 24 October 2026

Read-only source/RP audit; no runtime qualification claim. Legacy paths below are
relative to `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
RP means the read-only `26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`
identified in [RESOURCE_PACK.md](RESOURCE_PACK.md).

## Exact identity and presentation

`loot_table/tuntijaa_electro_electro.json:8–73` produces one
`minecraft:carrot_on_a_stick`, model `lbc:erecto`, tooltip `lbc:tier_s`.
Name component: `erecto_wand`, aqua, italic=false. Its exact existing names are
RU/rpr **Дзибдзюн Цсёнги Аккэ**, EN **Jibjun Tssengi Akke**.
Sosuenerugi / Сосуэнеруги is the Electro donor (`sauvojen_wand_electro`),
not the continuation's name.

Exactly three lore lines, with no inserted blank/status rows:

1. `type` + `staff`, both aqua and italic=false.
2. Score `erecto_wand_mana_use` / `lbc.math` + `mana_on_activate`, both aqua
   and italic=false. `function/config.mcfunction` sets that score to **1100**.
3. `erecto_wand.lore`, white, italic=false.

RU/rpr lore: `Наклон дуг зависит от расстояния, но похоже наводка не идеальна...`
EN lore: `The arc tilt depends on the distance, but it seems the aiming is not perfect...`
Custom data is exactly `{transformable:1,staff:1,magicwand:1,erecto_wand:1}`.
No source glint/enchantment or additional item flags are set by this table.

## Two donors and acquisition

`function/other/craft_ground.mcfunction:11` checks `stelectro:1` with a nearby
`lightningwand:1`. `function/other/craft/inventory_craft_check.mcfunction:11`
checks those same two flags in the player's crafting slots. Both dispatch to
`function/other/sauvojen_tuntija/crafted_ultraelectro[ _inv].mcfunction`, which
spawn `lbc:tuntijaa_electro_electro`, enchantment-table sound and pale-blue dust.
The inventory implementation explicitly reduces all matching slots 0–3;
the ground implementation kills each selected item entity. These are source
limitations, not a desired recipe contract: the requested native recipe must
consume exactly one Tesla and one Electro in both 2x2 and 3x3, allowing all
positions and preserving extra stack amounts.

Native equivalents confirmed in `src/main/java/me/skywars2/game/LbcWands.java`:
`TESLA` = `skywars2:tesla_wand` / `lightningwand:1` / name `lightning_wand`;
`ELECTRO` = `skywars2:electro_wand` / `stelectro:1` /
name `sauvojen_wand_electro`. This is ordinary Electro, not Tiny Electro,
Electro Shield, Lightning Bolts or Boltbolt.
The source also references this output from `loot_table/transformables_staff_all.json:43`
and `loot_table/source_of_energy.json:164`; their presence alone does not authorize
opening those native acquisition pools in this scoped continuation.

## Academy presentation

`function/other/magic_academy/can_upgrade/erecto_wand.mcfunction:1` sends
`upgrade_condition_erecto` in yellow. Exact RU/rpr:
`Попробуйте сфокусироваться на цели на расстоянии более 70 блоков! В ходе практики вы освоите новые способы управлять высшим электричеством!`
Exact EN: `Try focusing on a target more than 70 blocks away! With practice, you'll learn new ways to control higher electricity!`

`function/other/magic_academy/upgrade/erecto_wand.mcfunction:1–2` sends
`upgrade_succesful_erecto` in yellow and applies `lbc:tiers/ss`;
`item_modifier/tiers/ss.json` changes only tooltip style to `lbc:tier_ss`.
Exact RU/rpr: `Теперь у вашего жезла есть новые режимы! (переключать на Shift+ПКМ)`
Exact EN: `Your wand now has new modes! (switch with Shift+RMB)`
Name/model/base lore remain the same. Generic Academy machinery owns upgraded
flag/trial lore; do not infer that this two-line leaf alone adds those fields.
`advancement/true_advancements/legends/academy/erecto.json` confirms model
`lbc:erecto` and upgraded/transformable/staff/erecto flags, with parent Electro.
Runtime trial interpretation is audited separately by the parent agent.

## Existing assets and translations

All quoted keys exist in RP `assets/minecraft/lang/{ru_ru,en_us,rpr}.json`.
RP `assets/lbc/items/erecto.json` selects `lbc:item/erecto`;
`assets/lbc/models/item/erecto.json` inherits `minecraft:item/handheld` and uses
`minecraft:item/erecto`. Existing `assets/minecraft/textures/item/erecto.png`
and `.png.mcmeta` supply the animated texture. Both tier S and SS tooltip
background/frame PNGs exist under `assets/lbc/textures/gui/sprites/tooltip/`.
Spell-library erecto item/model/texture also already exist, if later required.
No missing presentation asset or translation was found for this scope; no
template addition or copied translation is justified by this audit.
