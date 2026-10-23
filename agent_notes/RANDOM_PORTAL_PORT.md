# Random Portal normal / old - QUALIFIED

23 October 2026. Two native acquisition leaves share the complete gateway-pair
mechanic below. Final combined qualification and installation status are recorded
with the tested artifact at the end of this contract.

## User scope decision (user)

> Да, отдельная пара для каждого дропа

Each activation owns its own gateway pair; earlier markers must not be spread,
rewritten or killed by a later activation. This user decision resolves the
source selector-scope ambiguity.

## Exact source tokens

S = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Normal: S `loot_table/barriersnottraps.json:490-533`, count1 repeating command
block, weight10 quality0, `lbc:tier_d`, `lbc:drop_not_trap`, yellow/nonitalic
`drop_portals`, one white/nonitalic `trap_desc` lore line, `{portall:1,drop:1}`.
Old: S `loot_table/legacy_luckyblock.json:2972-2989`, count1 plain barrier,
empty authored components and identical custom data. The earlier audit's
`legacy.json` shorthand refers to this actual `legacy_luckyblock.json` file.

Native registry IDs are `skywars2:random_portal_drop` and
`skywars2:old_random_portal_drop`; public one-item/count1 leaves are
`lbc:ported/random_portal_drop` and `lbc:ported/old_random_portal_drop`.
No full acquisition pool is enabled. `LbcPortalDropItem.Kind.NORMAL/OLD` expose
`item()` and `flag()`. `register()` initializes both items and their model defaults;
`matches(ItemStack)` retains source flag matching, including copied custom data.
`kind(stack)` identifies native OLD and otherwise NORMAL for matching stacks;
both variants dispatch the same source mechanic. Old receives no custom name,
lore or tooltip style and projects as an ordinary barrier.

## Resource-pack presentation

Read-only actual pack verified at
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`.
Existing `assets/minecraft/lang/{ru_ru,rpr,en_us}.json` contain both keys.
`drop_portals`: RU/RPR `Дроп: Двухсторонний портал`, EN `Drop: Two-way portal`.
`trap_desc`: RU/RPR `Активный Дроп. Для активации выбросте предмет.`,
EN `Active Drop. Throw the item to activate.` Exact source spelling retained.
Existing `assets/lbc/items/drop_not_trap.json` references
`lbc:item/drop_not_trap`; its generated item model uses
`minecraft:item/drop_not_trap`. Existing tier_d frame/background sprites are present.
No translations/models copied into mod assets or template; actual pack untouched.
Client pixels remain unverified.

## Native behavior and intentional corrections

S `function/drops/specialdrops.mcfunction:44` and `function/drops/portall.mcfunction:1-24`
create two exact native End Gateway block entities at base Y+3, with bedrock at
base Y-1 and Y+4. Reciprocal exit coordinates are the integer-floor base XYZ,
not the gateway position: source exit is the spread marker base, remote exit is
the original item base. The legacy execute-at retained its item executor; this
reverse assignment is intentionally preserved.

The single-target native SpreadPlayers two-air/support-tag/liquid rejection,
Nether under120, float center/range arithmetic, and 10000-attempt failure limit
are preserved. Border mode uses native center-corrected `0 0` -> (.5,.5), range170;
noBorders uses the first admitted fractional item origin, range200. If native
spread would fail, the original marker position remains the fallback. Ordinary
random outcomes need not match a prior datapack RNG stream.

`LbcPortalDrops` prepares independent jobs before shared dispatch commits any
matching flag or consumes one token. The original source position is captured
at first readiness, so ordinary dropped-item physics cannot restart the search
forever. This is a deliberate consequence of deferred preparation rather than
an instant synchronous command. A native loading-only ticket owns each required
chunk reference. Main-thread work polls resident chunks and scans at most32
steps per tick within a2ms search budget; it does not invoke managedBlock,
getChunkFuture(create=true), or a synchronous spreadplayers command. The budget
is a scheduling target, not a guarantee against native chunk-generation latency.
No activation is dropped by a new TTL, queue cap or saturation guard.

Unloaded/removed/changed/stale tokens cancel preparation and release tickets.
Time Stop pauses both admission and search. Actual match stop cancels all jobs,
releases only owned tickets and clears author references. Full endpoint build
height and native coordinate bounds are preflighted to avoid a half-pair; this
safety correction replaces the legacy sequence's possible partial setblocks.
Successfully created terrain remains until ordinary map cleanup.

Owned gateway metadata persists through the native BE codec: match epoch and
causal thrower. Native gateway travel is preserved, including entity/passenger
teleport behavior, but an owned gateway rejects protected roles, spectators or
any protected indirect passenger. Unowned vanilla gateways keep vanilla rules.
Owned gateways are inert outside their epoch; there is no global endpoint scan.
The native transition keeps its vanilla ticket callback and then records actual
arrival fall/void/touching-hazard provenance via shared IndirectKills. Newer
accepted attackers still override the portal cause. Allied players and the
thrower may use the pair; no invented team or Creative restriction is added.

## Validation scope

`PortalDropChecks` and `tools/smoke_portal_batch.py` exercise native geometry,
reciprocal forward/reverse portal processing, BE codec, provenance, concurrent
preparation, both border settings, resident support rules, invalid-height atomic
preflight, role/passenger guards, pause/unload and actual match-stop cleanup.
The combined runner also retains all prior142 recorded suite results and adds
Lucky Room checks. Qualification evidence and installation are recorded below
only after the final tested artifact is verified. Real client pixels, disk
restart/unload/disconnect and peak capacity remain outside this native fixture.

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
