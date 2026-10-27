# Death Compass ? native port, 27 October 2026

Source closure: [Compass/Aimbob audit](AIMBOB_COMPASS_SOURCE_2026_10_27.md). The original `other/death_compass_check`, `death_compass_attack`, `players_1s` and `lucky_items` entry73 were reread. Native26.3 SpreadPlayersCommand, Brigadier result-consumer failure handling, Warden and client CompassAngleState were checked locally.

## Author decisions (user)

> ??? ?????????? ?????? ?????? ?????????? ? ????? ?????? ??????? ?????? ? ???? ????? ????
> ?????? ?? ??????? - ?? ?????? ?????? ?????????? ?????????? (?? ?? ???? ?? ??????)

The first question mistakenly suggested stale shared death coordinates. Native `/execute store result` actually writes0 after a failed missing-NBT read, so source no-death activation was at floorXZ0. This was corrected in chat before implementation of the author's new fallback. Real recorded death history is never overwritten.

## Behavior and shared engines

Every20active ticks, a living eligible holder with the original flag in container0..35 or either hand checks the remembered death column. Own death wins. Without one, use another online player's remembered death: prefer this dimension, then nearest horizontal column, stable UUID ties. Without any recorded death there is no invented target. Source comparison deliberately floors current X/Z and ignores death Y/dimension. Armor/crafting/cursor alone do not activate. No mana, skill, durability, active-use, private cooldown or `nomagic_active` gate is invented. Ordinary input stun does not pause this passive; shared Time Stop does. Magic beacons and Imprinted/Nexus suppress activation.

A synchronized untracked LodestoneTracker points at that same GlobalPos; a new item-model property uses vanilla recovery-compass frames and the lodestone target. This changes only the carried flagged stack's runtime pointer; its source name/lore/tier/vanilla carrier remain. The original resource pack is read-only. Apply [the new model](../resourcepack_template/assets/lbc/items/death_compass.json) manually to the same path in the pack. Existing translation keys are reused, with no new copied language entries. Another-dimension target spins like a native compass; source X/Z trigger remains independent of that rendering rule.

Activation uses native clear1 scope/order (inventory/equipment, crafting, cursor). It creates a real Warden at captured origin+Y15,150current HP, native500maximum, no extra custom name/NoAI/persistence, dig_cooldown32767 and original `lbc:death_loot`. Darkness600ticks and original ranged MASTER dig sound follow even if spawn/spread fails. Only that invocation's Warden is moved. Its final bounding box cannot overlap a living nonspectator player.

Single-target spread preserves native square?10 sampling,10000iteration failure boundary, first floor with two air blocks, native teleportable tag and liquid checks, Nether under120/default world ceiling. Only resident columns are admitted; a synchronous per-column cache avoids repeated downward scans during failed random samples. No safe-spot fallback, chunk generation or terrain edits are added. Earlier independent consumption/feedback do not roll back if spread fails.

The Warden joins the existing `LbcCreatureDrops` ownership, epoch, Time Stop, reload and match-stop lifecycle, not a second summon service. Its brain/vibration/sonic targeting and harmful Darkness use that same default allied-player/role protection. The original holder remains a valid self consequence. Native damage and later kills retain the actual original holder through the existing indirect-kill system. Its actual death invokes the already packaged Death Scythe table and existing `LbcSpecialTools` engine; no parallel charge/progress state is created.

## Acquisition boundary

`lbc:ported/death_compass` is the exact original current recovery-compass leaf, weight10/quality7, with only standard26.3 schema conversion. It is operator staging (`/loot give @s loot lbc:ported/death_compass`). The genuine normal producer is the unresolved Lucky Block/lucky_items family; this patch does not enable that whole pool, invent reforge flags or claim normal-play acquisition is complete. Canned Food has the same previously documented boundary. Existing Death Scythe acquisition/engine is reused unchanged.

## Qualification

Buildc260a27d passed28 recorded results in two actual mode1matches (67.78seconds). Exact512sources/1452classes/3217resources match the tested JAR, including native Mixin registration and staged loot. All175 protected author paths and31live grimoire overrides remain byte-exact. Both matches verify native20tick scheduling, actual Warden health/brain/loot, own/fallback death targeting, pointer component, broad clear, actual allied sonic/Darkness rejection, sound payload, resident floor/sampling/no-load boundaries, input stun/Time Stop/role guards and actual stop cleanup. The final ending admission check is synchronous; observer and all activation cases run on the real scheduler.

Rejected fixture timing and ending-stage assumptions/logs are retained in `build/verification/death-compass/rejected-1/` and `rejected-2/`. Production bodies were not changed to satisfy those assumptions. The second run passed gameplay but was rejected because its stage mutation started a real lobby operation; normal owned-child shutdown and exact snapshots were restored, without forced restart.

The installer verified normal main shutdown, port/world unlock and immutable artifact hash, then installedc260a27d without starting/restarting the server. Proof: `build/verification/death-compass/qualification.json` and `main-install.json`. Current readyc260a27d and prior installed903de258 are retained; obsolete fe61bfdb was retired after verified installation. Cumulative352files/12667271337bytes removed (see cleanup receipt for authoritative total).

No physical Minecraft client frames/audio, arbitrary latency or peakTPS claim. The new model is structurally the exact vanilla recovery model with only its compass target switched to lodestone; actual pack application remains the author's step.
