# Ordinary Terra wand item resource

The ordinary Terra wand is sourced from
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/sauvojen_tuntija_terra.json`.
The imported standalone item payload is
`src/main/resources/data/lbc/loot_table/ported/terra_wand.json`; regenerate it with
`python tools/import_lbc_wands.py`.

The source item is `minecraft:carrot_on_a_stick` with `lbc:tier_c`, model
`lbc:st/terra`, translated name `sauvojen_wand_terra`, and custom data
`{craftusable:1,transformable:1,staff:1,magicwand:1,stterra:1}`. Its lore preserves
the source order and styles: the two aqua `type`/`staff` and mana-score lines,
gold lore keys `.lore.1` through `.lore.3`, yellow `.lore.4`, and yellow
`craft_with_fart_target`. The mana cost score is `stterra_wand_mana_use`, whose
legacy default is 1000 in `data/lbc/function/config.mcfunction`.

The Polymer server item is `skywars2:terra_wand`; both its registered-item
identity and the legacy `stterra` flag route into the shared wand matching and
mana/cast-hook pipeline. The cast invokes `game.items().terra().cast(player)`.
The local Terra cast has no separate global time-stop guard; the source's
outer `tick_not_timestopped` dispatcher pauses flight. The native port keeps
the shared mana/cast pipeline and pauses flight while time is stopped. Tiny
Terra remains its existing independent wand and spell.

The item design is confirmed against the old loot table. The model and
translation values remain references into the user's read-only resource pack;
no resource-pack files or translation entries are copied here.
