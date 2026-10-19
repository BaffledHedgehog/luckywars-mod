# Teleport wand resource import

The Teleport family is imported from the read-only legacy source at
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.
This import covers item payload and presentation; runtime casting, Academy
behavior, effects, and charge lifetime are implemented in `LbcWands`,
`LbcTeleport`, `LbcAcademy`, and the shared `ProjectileLifetimes`.
Run `python tools/import_lbc_wands.py --teleport` to regenerate only these
resource tables:

- `src/main/resources/data/lbc/loot_table/ported/teleport_wand.json`
- `src/main/resources/data/lbc/loot_table/ported/academy_teleport_wand.json`

The ordinary payload is selected from
`loot_table/sauvojen_tuntija_teleport.json`. It keeps the source
`minecraft:carrot_on_a_stick` presentation as native ID `skywars2:teleport_wand`,
model `lbc:st/teleport`, tooltip style `lbc:tier_c`, and translated name
`sauvojen_wand_teleport` in light purple with italics disabled. Lore order and
style are preserved: `type` + `staff`, the aqua `stteleport_wand_mana_use`
score + `mana_on_activate`, then `sauvojen_wand_teleport.lore.1` through `.3`
in gold and `.4` in yellow, all with italics disabled. The source custom data is
exactly
`{craftusable:1,transformable:1,staff:1,magicwand:1,stteleport:1}`.

The configured activation cost is 3000 mana, assigned to
`stteleport_wand_mana_use` in `function/config.mcfunction`.

The Academy native ID is `skywars2:academy_teleport_wand`. Its custom data adds
`upgradable:0,upgrade_available:1,upgraded:1,mode:1`; it appends the original
green, non-italic `upgraded_item` lore from `item_modifier/add_upgrade_desc.json`
and applies `item_modifier/tiers/a.json`, matching
`function/other/magic_academy/upgrade/stteleport.mcfunction`. That source
function emits the existing `upgrade_succesful_stteleport` message and applies
the `a` tier. No translation keys or translated text are added by this import.
