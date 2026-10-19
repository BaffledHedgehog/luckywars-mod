# Academy Terra, Electro, Exasperato: source audit

Scope: read-only comparison of disabled LBC datapack source and already-portable plugin assets. Translation checked in `C:\Users\yarik_temp\AppData\Roaming\FreesmLauncher\instances\26.3(1)\minecraft\resourcepacks\all_in_one_1.21.8\assets\minecraft\lang\en_us.json`.

## Entry and shared state

- `data/lbc/advancement/interacted_with_academy.json` awards `lbc:other/magic_academy/interacted` for interacting with an `interaction` entity tagged `magic_academy` while holding `{staff:1}`. Spectator, `minecraft:nexus`, and `minecraft:imprinted` are excluded.
- `interacted.mcfunction`: a wand with `upgrade_available:1` and without `upgraded:1` runs `interacted_upgrade`; `upgradable:1` gets the “no conditions” response; a staff with none of `upgradable`, `upgrade_available`, `upgraded` goes to `interacted_2`.
- `interacted_2.mcfunction` writes `upgradable:1` to mainhand and dispatches the per-wand requirement text. `upgradable:1` means trial started, not upgraded.
- `can_upgrade_book.mcfunction` sets `upgrade_available:1`, `upgradable:0`. `interacted_upgrade.mcfunction` sets `upgraded:1`, `mode:1`, applies upgrade lore and tier function; it does not explicitly clear `upgrade_available`. Terra/Electro apply tier A; Exasperato tier X.
- Source `true_advancements/legends/academy/{terra,electro,exasperato}.json` files check inventory for `{upgraded:1,transformable:1,staff:1,<wand marker>:1}`. They are achievement/unlock records, not trial counters. Their parents are Ruby, Terra, and Air respectively. The plugin carries these item checks under `src/main/resources/data/lbc/advancement/true_advancements/legends/academy/`.

## Trial requirements and progress

| Wand | Reachable source condition | Progress, reset, and exact requirement text |
|---|---|---|
| Terra (`stterra`) | `workingitems/sauvojen_staffs/terra.mcfunction`: with selected `upgradable:1`, player sneaks on `minecraft:grass_block`; then `terra_preloop_check_upgrade.mcfunction` rolls `lbc:chance20` and calls `can_upgrade`. Cast invokes `lbcr:placeflowers_nk`. | 20% chance per qualifying cast; no counter, timed progress, or fail/reset branch in this path. Translation key `upgrade_condition_stterra`: “It seems that this wand can do more than just sow grass. Try concentrating for a while and try to create a flower field (Shift+RMB). After some time of such practice, you will learn to use the purer natural magic of this wand!” Success `upgrade_succesful_stterra`: “You can now use pure Natura magic - the wand's projectiles are faster and much stronger, and it can now sow flowers instead of simple grass!” The source models repeated practice through repeated independent rolls, not an explicit timer.
| Electro (`stelectro`) | `electro_preloop.mcfunction` enters upgrade path only for sneak + selected `upgradable:1` Electro; `electro_preloop_upgrade.mcfunction` creates 60 `raycastelectro` markers (lifetime 10), randomizes/activates up to 60, then calls `can_upgrade` unconditionally. | Readiness is set on fan launch; no hit/damage/duration check, counter, or fail/reset path. `upgrade_condition_stelectro`: “Try meditating with this wand (Shift+RMB) - it will fill you with the endless energy of the wand, and will open up new knowledge about its use! (can be very dangerous! your body will be tormented by a huge amount of energy!)” Success `upgrade_succesful_stelectro`: “Meditation in the electric field of Sosueneruga inspired you to the idea of focused magic! Now the rod has a second mode (change to Shift+RMB), which shoots an electro beam without spread! Another railgun!!” Text does not specify a hit, damage, or duration threshold.
| Exasperato (`exasperato_wand`) | `click2.mcfunction` tags the new marker `check_upgrade` if wand is `upgradable:1`. `chtick_explode.mcfunction` counts selector `type=#minecraft:mobs,distance=..12,tag=!spectator,limit=10`; on `10..`, resolves owner by `lbcID2` and calls `can_upgrade` for held `upgradable:1,exasperato_wand:1`. | No score/progress counter or reset branch. `data/minecraft/tags/entity_type/mobs.json` explicitly includes `minecraft:player`, so players can count. Translation `upgrade_condition_exasperato`: “Try to holo-kill more than 10 mobs in one explosion, and with practice you will learn to cast the most fucked up electro-explosion spells!” This conflicts with implementation: source counts ten present entities within 12 blocks, not killed mobs, and caps count at ten, so it cannot represent “more than 10.” This is the only confirmed source/text conflict that needs author clarification before implementing the trial.

No day, kill, or no-damage predicate is present in Terra/Electro trial paths. Exasperato’s translated text says kill, but its reachable predicate is the proximity count above.

## Existing plugin representation and limits

- `LbcWands.java` registers `ACADEMY_TERRA`, `ACADEMY_ELECTRO`, `ACADEMY_EXASPERATO`. The Academy loot tables are completed-state copies: `upgradable:0,upgrade_available:1,upgraded:1,mode:1` (`academy_terra_wand.json:93`, `academy_electro_wand.json:93`, `academy_exasperato_wand.json:77`). They do not model trial-in-progress.
- `LbcWands.java:233` checks `upgradable==1` on shift cast for the relevant spell handling. This audit found no Java Academy NPC start/condition/progress/finish implementation; advancement/item coverage in `GameChecks.java` is synthetic/completed item coverage, not runtime quest progression.
- All quoted keys and English strings above are from the actual resource pack `assets/minecraft/lang/en_us.json` at the supplied instance path. Source keys: `upgrade_condition_stterra`, `upgrade_condition_stelectro`, `upgrade_condition_exasperato`, `upgrade_succesful_stterra`, `upgrade_succesful_stelectro`, `upgrade_succesful_exasperato`.

## Author clarification and implemented target (user)

> не убийств, задеваний

This resolves the Exasperato discrepancy above. The native port counts ten
distinct targets actually damaged by one charge's TNT/lightning series,
including its TNT chains, instead of counting nearby entities or kills.
Independent casts and repeated hits on the same target do not combine.
The threshold ten is retained from the source check; it was not separately
changed by the author. The old RP requirement text needs the supplied manual
value replacement in `resourcepack_template/updates/academy_trial.*.json`.
Implementation and remaining scope: [ACADEMY_PORT.md](ACADEMY_PORT.md).
