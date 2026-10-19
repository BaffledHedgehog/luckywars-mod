# Ruby wand resource import

`tools/import_lbc_wands.py --ruby` imports the Ruby ordinary and Academy wand tables.

The requested `sauvojen_tuntija_ruby.json` source filename is absent from the read-only legacy datapack. The ordinary Ruby entry is in `loot_table/sauvojen_tuntija_swargia.json`; the marker and payload identify it as Ruby (`sauvojen_wand_ruby`, `struby:1`). The imported item IDs are `skywars2:ruby_wand` and `skywars2:academy_ruby_wand`.

The source item name uses translation key `sauvojen_wand_ruby`, the model is `lbc:st/ruby`, and the ordinary tooltip tier is C (`lbc:tier_c`). Source lore, model, tooltip, tags, and other modifiers are retained. Academy adds `upgradable:0`, `upgrade_available:1`, `upgraded:1`, and `mode:1`, then applies `add_upgrade_desc` and `tiers/b`; its upgraded tooltip tier is B, matching `other/magic_academy/upgrade/struby.mcfunction`. No translation entries are added.
