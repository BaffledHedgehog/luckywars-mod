# SLR resources — 2026-10-20

Run `python tools/import_lbc_slr.py`. It uses the existing migrate_loot_schema/write_table helpers and verifies complete table equality after the sole native item-name replacement. Both `data/lbc/loot_table/ported/slr.json` and the already-packaged reforge identity `data/lbc/loot_table/slr.json` contain skywars2:slr. Generic `import_lbc_reforge_resources.py` now applies the same scoped SLR replacement, so reforge import cannot revert it to an inert vanilla item. Other item payloads are unchanged by this addition.

Added packaged resources: native ported SLR table, minecraft:breakable_slr, and its existing shared glasses_2/glasses dependencies. airs was already packaged; slr.json was already packaged. Existing shared tag contents were verified against source without overwriting them. Native target uses the existing minecraft:mobs entity tag.

Read-only resource-pack audit passed against the existing 26.3(1) all_in_one_1.21.8 pack: RU/EN name/lore and all four mode translation keys exist; lbc:super_rifle item model exists; tier_s background/frame sprites exist; minecraft:lbcsounds.karich sound event and each referenced OGG exist. No actual resource-pack or template file was written. No structure template is needed.

Validation: importer ran twice successfully; complete migrated payload equality checks passed; block tag dependencies checked against `C:/Users/yarik_temp/.gradle/caches/fabric-loom/26.3/minecraft-merged.jar` and local source-defined tag closure. Build intentionally left to the integration owner.
