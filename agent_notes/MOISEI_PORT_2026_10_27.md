# Moisei Boots — approved sprint port, 27 October2026

Parent task for author answers17/18 (current answer batch18..22), baseline main736bbbc1/HEADfb0de15. Own writes only new LbcMoiseiBoots.java, MoiseiBootsChecks.java, ported/moisei_boots.json and this note. No build/server/git, original datapack, resource pack or protected author file edits. Source: [movement armor audit](MOVEMENT_ARMOR_SOURCE_2026_10_27.md), checked against actual raw helpers below.

## Production contract

`LbcMoiseiBoots(GameController)`; root accessor `game.items().moiseiBoots()`. Root owns all integration.

Public `tick`, `stop`, `clear(ServerPlayer)`, `forget(ServerPlayer)`, `loaded(Entity)`, `unloaded(Entity)`; static `equipped(ServerPlayer)` and `managed(Entity)`; `owner(Entity)`, `canAffect(Entity,Entity)`, `explosionTarget(Entity,Entity)`, `inheritChild(Entity,Entity)`. Package `snapshot()` and monotonic `spawned()` are observation surfaces for native verification.

`tick` uses the canonical shared `sprint_timer` scoreboard once per admitted active player tick. It increments BEFORE checking footwear/beacon. Thus unequipped/suppressed sprinting still accumulates the SECRET shared clock. Actual stop-sprint resets a positive timer, removes only source movement_speed c1cda724-0ea2-460a-98b8-9631eafd2515 and attack_damage585f98b8-a8b8-4e4c-838a-f2e124831040 modifiers, and removes sprinted. Normal gear removal does not clear the score/effects. Source purify:49 explicitly zeros the clock, exposed as `clear`; source existing shared modifier clear remains independent. `forget` is lifecycle cleanup, not equipment change.

Equipped Moisei uses original Speed20tick ladder: 1..40/0,41..80/1,...361..400/9,401..450/10,451..500/20,501..550/30,551..600/40,601..650/50. Author-approved651+ continues refreshing amp50 and emits TEN independent vanilla PrimedTnt EACH active sprint tick. No cooldown, throttle, budget queue, merged explosion or fake impulse. TNT location is current feet minus full-pitch look vector×5; native fuse0, zero initial motion and ordinary native power/physics remain unchanged. New native TNT and chain children retain source owner/epoch before publication; present native TNT/reflection authority wins over stored UUID.

Dirt uses all eight original flat-yaw samples and all eight threshold fill segments in source order, including300/500 two extra segments. Each segment uses native floor endpoints and full axis-aligned inclusive rectangle, replacing only #minecraft:airs. Native BlockInput flags818 match source STRICT fills; solids/containers are preserved and no artificial particles/sounds/drop or shape update is added. Each tiny original fill preflights resident chunks/world height/border and its native modification gamerule independently; no chunk loading or queued terrain work. The whole path writes within shared `trapChanges` for support/suffocation provenance.

Passives continue under active-input stun/ice. Actual sprint flag remains authoritative. Shared Time Stop pauses updates; protected roles, gameplay participation, current map, ENDING and nexus/imprinted reject. Root is adding shared dimension policy; Moisei is not a Nexus boot exception. Beacon-only suppression matches source, without inventing a nomagic player-aura gate.

Default other-allied-player protection, technical roles and Greed use existing projectile target guards. Native self-source TNT is admitted explicitly. Root must wire shared playerSource, pre-impulse explosion filtering, damage admission, chain inheritance and current-epoch delayed observer-source exemption, plus normal loaded/unloaded/forget/stop and source purify clear hooks. Match stop clears only managed actors and unloaded authority; ordinary native speed layers expire naturally.

## Correction to historical audit

Actual `function/players_sprint_end.mcfunction` contains ONLY four commands: timer0, remove the two exact sprint modifiers, remove sprinted. It contains NO bubble_pop sound. The earlier movement audit's raw100000 sprint-end sound statement was erroneous (that sound exists in other helpers such as Pop-It/Traveller). This port follows the actual source and adds no sprint-end audio. Parent was notified before implementation.

## Source presentation/acquisition/task

The new ported singleton is exact original `moisei_boots.json` with only the native26.3 function/functions codec keys migrated to type/modifier. Vanilla leather boots; max_damage600; source model lbc:armors/moisei_boots and equipped asset lbc:moisei; tier_ss; only armor+2 with original id; exact brown translated name, white type row and aqua lore; transformable/boots/moisei flags. No RP edit or invented attribute/item/recipe.

Native original `transformables_boots_all` entry4 is Moisei weight1 after18 ordinary/three rare entries. Existing ULTRA rule is **moisei flag** -> boots_all; it is a real reforge route but cannot independently prove first acquisition from generic boots. First original acquisition is loliluckyblock:688 -> moisei_boots. Root must close/declare original pool packaging without silently exposing unresolved whole pools; new direct leaf alone is not normal first acquisition. No ordinary normal-reforge or ingredient recipe was invented.

The imported actual native task `lbc:true_advancements/armor/moisei_boots` requires a killed player while feet custom_data moisei1, parent Traveller. Existing kit7 page9000/slot6 is gated by this task and references kit/moisei. That kit grants16 dirt64 rolls (1024 dirt), **not boots**. No extra Java kill award was introduced: native criterion dispatch owns it.

## Authored native checks; not yet executed here

`MoiseiBootsChecks.run(game)` starts, `.tick(game)` observes actual scheduled native updates. It never manually ticks a player/entity/effect/world or the production service. It reuses the qualified PassiveConsumableChecks private Snapshot/ScheduledClient helpers via scoped reflection, retaining the actual registered native connection, loaded/teleport ACK, latest position packet independent of measurement windows and force-writable native FOOD score restoration. Original equipment attributes, effects, hunger fields, scores, profiles, all advancement progress, combat fields/tickers, tags, use state and relevant stats are restored. Original owner/other indirect attribution leases are snapshotted through the existing native fixture.

Coverage authored:

- Exact source native presentation, actual weighted original boots_all roll, and actual existing ULTRA reforge Work consuming its native dropped donor and delivering exact boots.
- A disposable actual vanilla TestMoiseiVictim connects normally, is temporarily enrolled with an ordinary Participant and killed by actual accepted native player damage. Alpha/Beta lives are untouched. The imported native task must award and existing kit7 must switch from locked to selectable; its original loot must total1024 dirt with no boots. The fixture victim then leaves through native disconnect. The test never directly triggers/awards the criterion to fake a kill.
- All30 first/last speed boundaries through650 and independent native STRICT /fill command controls at yaw45 with pitch37, comparing the whole resident dirt plane.
- Shared unequipped100 and mid-sprint equip101 behavior, actual input stun/ice continuing the passive, and actual Time Stop pausing the canonical score.
- Three consecutive native651+ updates each emitting TEN real vanilla fuse0 TNT at full-pitch feet-back5, retaining amp50/20ticks and exact owner; self-source admitted and Greed excluded. A source ResistanceV fixture layer protects its owner while native blasts run normally.
- Actual stop-sprint timer reset and native Speed expiry after23 observed updates, followed by owned actor cleanup.

Limitations: no child build or runtime qualification, physical pixels/audio, peakTPS stress or first-acquisition whole-pool completion claim. Root must review compile/native fixture assumptions, qualify ally/role/reflection/chain behavior in the shared suite, and resolve first-acquisition packaging. Constructor-stage source/presentation checks and source-plane restoration have explicit cleanup after initialize errors; root's isolated server restoration remains the final outer fixture boundary.

## Raw source SHA256

| Source relative to data/lbc | SHA256 |
|---|---|
| function/items/on_sprint.mcfunction | 6db40ad81082d1a14cce50002956c13ff34f54104f8b0a6aa8fcba7ec30f0e2c |
| function/players_sprint_end.mcfunction | ef5df784dd9d20cb166c97c8c72f66c1c5e33ce7cc5218ffe2531a6cb9de6a3b |
| function/workingitems/moisei_boots_tick.mcfunction | af4ed301c5ef950ce2d2e2cec014f1bfd904c21ecda34f8365a973667062b249 |
| function/workingitems/moisei_boots_tick_place.mcfunction | 6c7b9730c62ce1dde11ffa7e83a4823666099b3e8c4c564486370c3f5cf1ec41 |
| function/workingitems/moisei_boots_tick_explode.mcfunction | 2954f52097b0923926e3e62db7ab5ce9ae0609da0af3916d9e5edd3594b00525 |
| loot_table/moisei_boots.json | d722d26758a6cf2bf392ba4001886ee8c75aa1d5bf488fed8908d8d54b10d37b |
| advancement/true_advancements/armor/moisei_boots.json | bdbffa8c4d6b6f340a7a63c1f70b6db133735daf0fb07868f76d9ece456c6ee9 |

## Frozen handoff SHA256

| Owned file | SHA256 |
|---|---|
| src/main/java/me/skywars2/game/LbcMoiseiBoots.java | 112e3d3ca248c7c1d2735855083eecf24b14a5317d8e541ca96a00885de7ecea |
| src/main/java/me/skywars2/game/MoiseiBootsChecks.java | d5593c457d1093b363434d4fe725dec1106784a801d85d0786d151912e312369 |
| src/main/resources/data/lbc/loot_table/ported/moisei_boots.json | d455da1685984cf9276418ef413b90b9d1c932210f93c2f1ec4ce243408597f9 |
