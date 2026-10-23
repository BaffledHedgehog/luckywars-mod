# Lucky Room normal / old - QUALIFIED

23 October 2026. Closed native geometry mechanic, two public leaves. This note
records implementation; final combined qualification/deployment status is below. No extra reward or full acquisition pool is enabled.

S = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Normal `loot_table/barriersnottraps.json:402-445` is count1 repeating command block,
weight10 quality8, tier_c/drop_not_trap, yellow/nonitalic drop_lbc_room, one
white/nonitalic trap_desc lore, `{lbh:1,drop:1}`. Old
`loot_table/legacy_luckyblock.json:2151-2169` is plain barrier/count1/empty authored
components with identical flags. Native IDs/public leaf tails are
`lucky_room_drop` and `old_lucky_room_drop` (skywars2 item / lbc:ported leaf).
`LbcLuckyRoomDropItem.Kind.NORMAL/OLD` exposes item()/flag(); matches(stack)
uses source custom data, kind(stack) distinguishes the native old ID.

`function/drops/specialdrops.mcfunction:40` chooses nearest nonspectator as both
executor and position. Native `LbcLuckyRoomDrops.ready(token)/spawn(token)` reuse
cageDrops.target role/observer guards and allow self, allies and Creative targets.
No eligible target still consumes the shared one-unit debit. Nearest selection
uses full fractional XYZ; each block coordinate floors the target origin as
native relative integer block arguments do. No token-origin structure is used.

`function/drops/luckyroom.mcfunction:1-10` is ten strict block commands:
5x5x3 yellow concrete (Y-1..Y+1); 3x2x3 air cavity (Y..Y+1); four gold
outer corners at Y+2; four red mushroom inner corners at Y, all six faces false.
The room-local strict BlockInput placement preserves overwritten-container
contents as real item drops. Local 26.3 LevelChunk.java:305-316 confirms flag256
suppresses BE preRemoveSideEffects, so strict818 alone would silently erase
contents. The room retains the old native container, performs the actual strict
placement, then drops and clears retained contents only after successful BE-
replacing writes. Unchanged/failed writes and rejected fills do not extract
anything; the shared LbcDropBlocks behavior is unchanged. Fill gamerule/build-limit failure applies per
source command; singleton setblocks are independent. No new chunk tickets,
world scan, TTL, actors, custom rewards or subsequent producer exist.
Actual thrower wraps all writes in indirectKills.trapChanges with WILD=true,
so immediate terrain/support/fall/void consequence attribution uses the shared
existing infrastructure and newer attacker priority. Terrain remains after stop;
pending tokens use the shared epoch, freeze, unload and one-unit debit lifecycle.

Actual read-only RP `26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8` already has
`drop_lbc_room` RU/RPR/EN and `trap_desc`, drop_not_trap model and tier_c sprites.
No RP/template/mod-language additions were necessary. Client pixels unverified.

`LuckyRoomDropChecks.run` is synchronous: both prototypes/public native loot
leaves/styles, exact full structure plus outside sentinels at fractional target,
real replaced-chest inventory drops, shared debit/replay, self/Creative/spectator
and service roles, freeze and stale epoch. stopProbe/cleanup verify actual match
stop epoch and pending-token no-debit/no-replay. Native client rendering,
real disk restart/chunk unload/disconnect, natural movement and peak capacity
remain separate limits. The final native checks passed; exact evidence is below.

## Combined qualification

Final tested artifact: `build/verification/skywars2-portal-batch-3cb2c7d1.jar`, SHA256 `3cb2c7d1570ff213c0c4a3cc2056a9929dd19b034c03d0d8199dadd016584f9b`.
Offline build and two actual isolated mode1 matches passed: 154
recorded results, including all prior142 and both new suite/probe/cleanup groups.
Wall time 130.25s includes boot/map/regressions, not per-tick CPU.
Exact source/class/resource and current main/user baseline proofs are in
`build/verification/portal-batch-final/qualification.json` (local ignored evidence).
Installed to main mods/skywars2.jar while stopped; rollback 68387e1d retained.
The agent did not start the main server.

Retained failed attempts: attempt1 is native API compilation corrections;
attempt2 is a fixture-only noBorders search quadrant with no reachable map
terrain. The final fixture uses actual reachable terrain, retains true random
selection/pair isolation and keeps its600-tick failure boundary unchanged.
No search behavior or production failure limit was weakened for qualification.
All final checks passed; known fake-player Polymer packet-context diagnostics
remain explicitly recorded, with no unexpected ERROR/check FAIL in final logs.

Limits: native BE codec roundtrip and actual entity portal processing are
covered; actual disk restart/unload/disconnect and client pixels are not.
There is no peak-capacity or real-client latency benchmark. The underlying
native chunk generation may exceed the soft main-thread search budget.
<!-- portal-batch-qualification-end -->
