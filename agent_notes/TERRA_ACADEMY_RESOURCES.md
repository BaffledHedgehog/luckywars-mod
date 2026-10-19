# Magic Academy Terra wand resource payload

The improved Terra staff keeps the ordinary staff's `lbc:st/terra` item model,
name, and original lore. The Academy upgrade applies `lbc:tiers/a` (tooltip style
`lbc:tier_a`), sets `upgraded:1` and `mode:1` in custom data, then appends the
existing green, non-italic `upgraded_item` translation line. Its Academy item
identity is `skywars2:academy_terra_wand`; its combat projectile uses the separate
`lbc:terrasword_true_charge` model.

The ordinary staff comes from `lbc:sauvojen_tuntija_terra`. Its Terra use runs
the source's 20% `can_upgrade` check; the check sets `upgrade_available:1` and
clears `upgradable`. Interacting at Magic Academy while that offer is available
runs `interacted_upgrade`: it retains `upgradable:0` and
`upgrade_available:1`, marks the held item `upgraded:1` and `mode:1`, appends
`lbc:add_upgrade_desc`, then dispatches
`lbc:other/magic_academy/upgrade/stterra`, which applies `lbc:tiers/a`. Ordinary
item import remains unchanged; `tools/import_lbc_wands.py` emits the Academy
variant as a separate loot table at `lbc:ported/academy_terra_wand`.

Source: `data/lbc/loot_table/sauvojen_tuntija_terra.json`,
`data/lbc/item_modifier/add_upgrade_desc.json`,
`data/lbc/item_modifier/tiers/a.json`,
`data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`,
`data/lbc/function/other/magic_academy/upgrade/stterra.mcfunction`, and
`data/lbc/advancement/true_advancements/legends/academy/terra.json` in the
read-only legacy LBC datapack.
