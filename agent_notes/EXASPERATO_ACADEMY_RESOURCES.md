# Academy Exasperato resource audit

## Imported item state

`src/main/resources/data/lbc/loot_table/ported/exasperato_wand.json` retains the
ordinary source payload under `skywars2:exasperato_wand`; the Academy sibling
`academy_exasperato_wand.json` is built from it. Both preserve the carrot-on-a-stick
proxy, `lbc:exasperato` item model, source translation components and lore order.
The Academy variant keeps tooltip style `lbc:tier_x` after the source upgrade
modifier, appends the green `upgraded_item` lore component, and carries
`{transformable:1,staff:1,magicwand:1,exasperato_wand:1,upgradable:0,upgrade_available:1,upgraded:1,mode:1}`.
Its custom data is emitted by `minecraft:set_custom_data`, while model and tooltip
are native `minecraft:set_components` loot functions.

The imported table is registered as a normal Minecraft loot table. Runtime
`LbcLoadouts.rollTable` retrieves it from `reloadableRegistries()` and rolls it,
so the source typed item components flow through the vanilla loot serializer and
into the resulting stack; this is the same registration/roll path used by the
ported item tables. No separate wand registry entry is needed for the resource
payload.

## Source comparison

- Ordinary item: `world/datapacksdisabled/LBC/data/lbc/loot_table/tuntijaa_air_exasperoso.json`.
- Academy upgrade function: `.../function/other/magic_academy/upgrade/exasperato_wand.mcfunction`;
  it applies `lbc:tiers/x`.
- `item_modifier/add_upgrade_desc.json` appends the green `upgraded_item`
  translation component. `item_modifier/tiers/x.json` sets tooltip style
  `lbc:tier_x`.
- `item_modifier/swrg_unlocks/academy_exasperato.json` is only the locked GUI
  presentation, so it is not the Academy item payload.
- `loot_table/64_nukes.json` uses tier `lbc:tier_ss` for a special drop; that is
  not the Academy upgrade.
- The RP defines `assets/lbc/items/exasperato.json` as `lbc:item/exasperato`;
  `assets/lbc/models/item/exasperato.json` inherits `minecraft:item/handheld`
  and uses `minecraft:item/exasperato` texture.

## Translation availability

The actual pack is read-only and was checked in both
`assets/minecraft/lang/ru_ru.json` and `en_us.json`. Every key used by the
imported item is already present in both: `exasperato_wand`, `type`, `staff`,
`mana_on_activate`, `exasperato_wand.lore.1`, `exasperato_wand.lore.2`, and
`upgraded_item`. Therefore no translation template or mod-language entry is
needed. The root admin-only template entry was left intact.

The Academy Exasperato source exposes modes 1–4 with keys `mode_default`,
`mode_electro_ray`, `mode_chaos_area`, and `mode_nuke`; all four keys already
exist in both pack languages. `mode_ray` also exists in both languages, but is a
different generic key. The source does not use `mode_chaos`; the chaos mode is
named `mode_chaos_area`. No mode translation additions are required.

Relevant source behavior is in
`function/workingitems/awaken_tuntija_wands/exasperato/click_upgraded.mcfunction`,
`click_upgraded_left.mcfunction`, and `click2_upgraded.mcfunction`. The current
Java `LbcWands` implementation rejected Academy Exasperato casts at the time of
the resource-only audit. The four modes are now connected; see
[EXASPERATO_ACADEMY_PORT.md](EXASPERATO_ACADEMY_PORT.md) for current behavior and checks.
