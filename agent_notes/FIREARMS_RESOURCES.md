# AK47 / Magnum staged resource import — 21 October 2026

`tools/import_lbc_firearms.py` imports only source presentation and dependencies.
Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

- `loot_table/stalin.json` → `data/lbc/loot_table/staging/ak47.json`, native `skywars2:ak47`.
- `loot_table/stalin_ak_finite.json` → `staging/ak47_finite.json`, same native ID;
  source `finite:1` differentiates this stack.
- Magnum entry in `loot_table/transformables_shooter_all.json` → `staging/magnum.json`,
  native `skywars2:magnum`. It is not added to the ordinary shooter pool.
- Complete `loot_table/magnum_ammo.json` → `staging/magnum_ammo.json` unchanged
  apart from the shared schema migration. All six rounds use native Polymer IDs
  `skywars2:magnum_round_{standard,incendiary,electric,acid,void,explosive}`;
  original custom_data also accepts legacy vanilla rounds. Only native IDs change.
  The original `lbc:magnum_ammo` alias supplies already ported 64-arrow UltraReforge;
  no two-arrow NPC trade is invented.
  Four rolls, standard weight 2, other weights, count distributions and components
  are preserved; no free-ammo source or trade is introduced.

Gun import asserts equality with the migrated source entry except its item ID.
Names/lore, original translation keys, formatting, model, tooltip, enchantments,
attributes (if present), counts, weights and custom_data are retained. Infinite
AK retains Infinity 1 and literal `morgenshtern ` / `Beta Server Harem event` lore;
finite AK retains `ak47lore` and `ak47lore.1` rather than borrowing infinite lore.

Read-only verification passed against the current **26.3** resource pack:
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8`.
All used keys plus six Magnum mode names exist in RU/EN/RPR; eight item
definitions, their model parents and explicit texture files resolve. AK uses the
vanilla `minecraft:item/handheld_rod` parent. Tier A/B/S/SS tooltip sprites and
`minecraft:lbcsounds.shot` referenced OGG files exist. Magnum uses vanilla sounds.
No resource-pack or template changes are required or made. A graphical-client
visual comparison has not been performed.

Validation: importer, catalog and final build passed. Two final firearm matches
passed on `062c19b3dec8e378e09589633075e414182b820f850cd5ecae961790fd49ef40`
(seven suites per match, including both cleanups; 50.75 seconds). Resource audit
passed: 2,635 packaged entries, 1,977 excluded entries, 114 Java sources and CRC.
Four tables are now promoted to `ported/`; the original `lbc:magnum_ammo` alias
is byte-identical to ported ammo for existing 64-arrow UltraReforge.

Archive `build/verification/skywars2-firearms-062c19b3.jar` is qualified and ready,
not installed. Main JAR remains ba2e46e6. Additional shared regression is running
separately. Native ammo changes only item IDs; source presentation remains exact.
All six modes are implemented, big acid aims **down**, RP/template are unchanged.
Next bounded audit: [Govnomet / Helicopter Boss](FIREARMS_NEXT_SOURCE.md).
