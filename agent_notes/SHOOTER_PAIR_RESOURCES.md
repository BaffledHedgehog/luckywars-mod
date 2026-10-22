# Attack Helicopter / Laser Minigun resource audit — 21 October 2026

Source: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/transformables_shooter.json`.
Importer: [import_lbc_shooter_pair.py](../tools/import_lbc_shooter_pair.py), reusing
the Woodificator reader/RP location and existing schema, selector and table helpers.
Only ordinary entries are extracted. After required loot schema migration, the
registry ID changes to `skywars2:attack_helicopter` or `skywars2:laser_minigun`;
explicit user changes additionally set minigun damage lore to `2.5 ` and add
per-hand movement_speed -0.5/jump_strength -1 ADD_MULTIPLIED_TOTAL attributes.
The three batch tables are promoted under `data/lbc/loot_table/ported/`; broad random/Reforge pool readiness remains separate.

The actual RP at `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8`
was read only. Existing RU/EN values are reused unchanged; no template or translations added.

| Item | Source presentation and flags |
| --- | --- |
| Attack Helicopter | `lbc:attack_helicopter`, tier B, green nonitalic name; type/shooter, literal `30 ` + recharge_time, attack_helicopter.lore.1/.2/.3; transformable/shooter/spellwithcooldown/magicwand/attack_helicopter = 1 |
| Laser Minigun | `lbc:laser_minigun/1`, tier A, aqua nonitalic name; type/shooter, literal `2.5 ` + projectile_damage (author correction from `4 `), literal `100 ` + mana_cost, laser_minigun.lore; transformable/shooter/magicwand/laser_minigun = 1 |

All other nested formatting, lore order, tooltip hiding and custom data survive unchanged.
Laser retains attack_speed `-1`, add_multiplied_total, mainhand/offhand modifiers
with source IDs `generijfyjyjted` / `gjcjgfcjhjgjgjed`, hidden by tooltip_display.

Read-only importer validation passed: every referenced RU/EN key; tier A/B tooltip
background/frame; item definitions and recursively resolved geometry/textures for
attack_helicopter, helicopter_1..4, laser_minigun/1..8, laser_minigun_ray.
The source modifier orders tests 2->1,3->2,...,8->7,1->8; sequential application skips frame1 when starting from2. The author approved the repair: runtime now uses every one of the eight frames.
Laser geometry uses `minecraft:item/lasermg`; ray uses `minecraft:item/laser_mg_ray`;
helicopter inventory icon uses `minecraft:item/attack_helicopter`.

Validation: `python tools/import_lbc_shooter_pair.py` passed and round-tripped both
generated JSON tables. Importer validation itself claims no graphic client visual
check or gameplay qualification. Native effective models are implemented. Staged
gameplay passed all 12 suites in two matches; final artifact qualification passed; archive is ready without installation.

Native Polymer mechanism_beacon reuses the existing stopper model and mecha_breaker
translation/presentation through LbcMagic, with ported loot and #minecraft:mecha
block/entity tags. No new graphic/template is required. Native minigun defaults
and imported tables share the movement/jump attributes; dual holding also has
a transient runtime movement -1 multiplier for zero speed (native two -0.5
multipliers otherwise leave0.25). This runtime lock is not a loot component.
