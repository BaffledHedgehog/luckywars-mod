# Duplicating / Divider resources — 20 October 2026

Importer: `tools/import_lbc_duplicating.py`. Reuses Consuming's migrated-source reader and read-only RP path, plus existing wand/item import helpers.

Native IDs and payload paths:

- `skywars2:duplicating_wand` → `src/main/resources/data/lbc/loot_table/ported/duplicating_wand.json`
- `skywars2:academy_duplicating_wand` → `src/main/resources/data/lbc/loot_table/ported/academy_duplicating_wand.json`
- `skywars2:divider` → `src/main/resources/data/lbc/loot_table/ported/divider.json`

Ordinary Duplicating comes from `loot_table/tuntijaa_greed_avarice.json`; intentional source spelling `dublicating_wand` remains in model, translate keys, custom data and mana score. Original light-purple name, tier A, complete ordered lore/styles/components are preserved. Academy adds the source `add_upgrade_desc` and `tiers/ss` modifiers and existing Academy custom flags; its outer native ID changes independently of source custom data.

The independent `greed_greed` donor is Divider from `loot_table/greed_greed.json`: original model `lbc:divider`, tier S, complete name/lore and `{craftusable:1,magicwand:1,greed_greed:1,spellwithcooldown:1}` preserved. Its actual behavior is `function/workingitems/divider.mcfunction`, called by `spell1_default` and `spell1_default_left`: 26 clones of the central 25³ cube to its neighbors, four anvil sounds and `divider_cd=30`; no mana charge. This donor is distinct from Academy Duplicating's Divide mode and metal-pipe crafting output.

Validation: `python tools/import_lbc_duplicating.py` passed. Both ordinary payloads equal their full migrated selected source entries except outer native ID. Academy equals that full base plus the exact source modifiers and Academy flags. `DUPLICATING_RESOURCE_MANIFEST.json` records generated payload SHA-256 hashes and read-only RU/EN language hashes/values. All used item translations, upgrade messages, four real mode keys (`mode_default`, `mode_expand`, `mode_divide`, `mode_black_hole`), model item definitions and tier A/S/SS tooltip sprites already exist in the user's RP. No template additions are needed; the user's RP and existing template were untouched. No build/server run was performed.
