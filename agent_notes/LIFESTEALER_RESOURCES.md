# Lifestealer resource port — 2026-10-21

Source: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/transformables_staff.json`, modern `lifestealer:1` entry, not `lifestealer_old`. Only that complete entry is exposed by `ported/lifestealer.json`; the original mixed random pool is not imported.

Native IDs follow the existing wand convention: `skywars2:lifestealer_wand` and `skywars2:academy_lifestealer_wand`. The two resource table filenames remain `lifestealer` and `lifestealer_upgraded`. The importer preserves source name, lore, component formatting, model `lbc:vampirewand`, tier D and original custom data. Academy adds the common source flags (`upgradable:0,upgrade_available:1,upgraded:1,mode:1`) and applies source `add_upgrade_desc` then `tiers/a` modifiers. Item registration and Polymer carrot projection are owned by the main implementation.

Academy enum uses the two LbcWands registry constants and original condition/success keys. `lifestealerCast(player, hand, stack, targets)` completes an active Lifestealer trial at fifteen eligible mobs in one cast; it uses the casting hand consistently with the current Academy port. Its shared `ready` helper checks active player and stack identity. It does not count cumulative casts.

Source cost is `vampire_wand_mana_use` / `lbc.math`, configured 250 in `function/config.mcfunction:23`. Upgraded modes are 1 (`mode_caster`) and 2 (`mode_charge`); existing source `change_mode_2` cycles them. The project user rule permits switching with any mana and without cast effects.

Read-only design source: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8`. All sixteen required keys already exist in RU/EN; the existing vampire item definition/model and both tier D/A tooltip PNG pairs are present. No template or duplicated keys are needed.

Validation: `python tools/import_lbc_lifestealer.py` passed after generation, compares full generated JSON payloads to source-derived expected structures and checks RP resources/key existence. No build or server run performed by this resource task; no resource-pack writes.
