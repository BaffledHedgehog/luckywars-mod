# Time fragments and Cunny: bounded source audit ? 2026-11-04

Read-only source comparison; only this new note written. No Java/resource/RP/server changes, builds, tests, commits, question-file edits, or author-path changes. TMT LAST. Findings describe source definitions and registration, not runtime qualification or readiness.

## Boundaries and exact identity

Legacy root L = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`; F = L/function. Native root N = `src/main/java/me/skywars2/game/`. Serena initial instructions read before Java inspection. Root AGENTS and agent_notes/AGENTS apply; existing author content preserved.

**Completed Clocks/Overheaven must not be redone.** `N/LbcStand.java:148` registers native `skywars2:stand_clocks`, with `stand_clocks:1`, stand model and durability25. `LbcTimeStop.useClocks` (line116) calls the existing dimension stop, observes native cooldown, gameplay admission, action blocking and magic suppression. `LbcStand` owns Overheaven progression, fist variants and existing knife links. These are separate from fragments carrying `clocks:1` and a string `type`.

The imported `item_defs/lbc/time_clock__6b7715f03a34.json` is another distinct time-stopper item (`time_stopper:1`, spellwithcooldown), not a fragment. Do not infer support from a common `time_clock` filename/model.

Actual fragment source loot is `L/loot_table/clocks.json`: carrot_on_a_stick, custom data `{transformable:1,accessory:1,magicwand:1,clocks:1,type:...}`. Sixteen variants, all imported definitions below:

| Type | item_defs/lbc/time_clock suffix |
|---|---|
|1t|4541d0c68705|
|2t|9b2f3d3ba79e|
|5t|141ec6471ea4|
|10t|e02bd48af3a8|
|1s|0c83b10b8d01|
|2s|e9ceda1c665f|
|3s|c15ec2410261|
|4s|5407fcb61922|
|5s|b3209a1bc005|
|10s|6b0b262bb363|
|15s|78156c8e5622|
|30s|910588bd7f1c|
|jump_1s|e9dff710e85e|
|jump_2s|45c2192a9a0f|
|jump_5s|2f348ee3e5ae|
|jump_10s|7545c917b74c|

## Source trigger, cost and exact replay work

`F/workingitems/spell1_default.mcfunction:70` and `spell1_default_left:55` route `clocks:1` to `clock_fragment/select` / `select_left`. These leaves reset `RC_Raycast` to0 then dispatch on the actually used hand's type. Direct types call one global datapack family; **30s calls tick90s**, not tick30s. No local mana debit, item consumption, cooldown, or jammer condition exists on those two dispatch lines or in fragment selectors/jump bodies. This is a source fact, not a new suppression-policy ruling. Shared outer input gates still require separate consideration during any implementation.

Jump functions are explicit ordered synchronous invocations, not native world time advancement, rewind or elapsed real seconds. Counts obtained directly from the four jump files:

| Jump | tick | tick2t | tick5t | tick10t | tick1s | Additional unconditional calls |
|---|---:|---:|---:|---:|---:|---|
|1s|20|10|5|2|1|none|
|2s|40|20|10|4|2|tick2s?1|
|5s|100|50|25|10|5|tick2s?2, tick3s?1, tick4s?1, tick5s?1|
|10s|200|100|50|20|10|tick2s?5, tick3s?3, tick4s?2, tick5s?2, tick10s?1|

The source repeats tick5t **five times per nominal second**, an observable anomaly; do not normalize silently. Extra independent predicate draws: jump1s gives tick2s/3s/4s/5s/10s/15s/90s at50/35/25/20/10/7/3%; jump2s gives tick3s/4s/5s/10s/15s/90s at70/50/40/20/15/6%; jump5s gives tick10s/90s/15s at50/15/35%; jump10s gives tick90s/15s at35/70%. Invocation order is the literal file order, not sorted cadence order.

A jump10s click therefore reaches393 unconditional global family invocations plus two optional calls before following their descendants. This does not establish equivalent Minecraft entity/block/scheduled-tick simulation. Its cost depends on every reached family and world population; multiplying a scalar cooldown alone is not a faithful implementation.

## Cunny: actual mechanics versus source placeholders

`spell1_default:100` / `spell1_default_left:84` detect the presence of custom-data `cunny`; selectors read its integer and route `select1` values0..21. Unknown values do nothing. Main/offhand read separately. Imported `L/loot_table/imba/cunny_slabs.json` and native resource imports are definitions, not live native consumers. Exact native Java searches for `cunny`, `time_clock`, `jump_1s/2s/5s/10s` found no consumer (excluding documentation/resources).

Correct the prior broad shorthand ?22 global-script branches?: there are22 selectors, but **11..20 are source TODO bodies that only clear one matching item**. Do not invent ten new effects. All numbered branches end with `clear @s *[custom_data~{cunny:N}] 1`, a matching-inventory clear rather than guaranteed used-hand debit.

| N | Actual active body excluding presentation |
|---|---|
|0|`swrg:game/start/0` global match start/reset then clear|
|1|Sets shared team comparison; removes all four armor slots from nonallied players at distance0.01..|
|2|Owned cunny2buffs armor+6, toughness+3, attack+6, scale?50%, maxHP+50%, movement+20%; inf_shulker, inf_steak, evolution_sword loot|
|3|Infinite Regeneration III and rare grassifier slowraycast marker|
|4|Twenty explicit neighboring spawn-helper calls, including duplicated cardinal distance2 positions|
|5|Nahida dakimakura loot|
|6|Random nonspectator nonally love_girl plus caster girl_seeker tags|
|7|Temporary caster exclusion tag; whip helper at every other nonspectator player|
|8|One new Cunny loot plus global all-player child helper|
|9|Independent random X/Z ?1000000..1000000, macro teleport helper|
|10|luckyblock_all_stacks + op_hoe + god_dice; kills one random entity without role/type filter|
|11..20|TODO; clear only|
|21|Spawn helper at up to64 random entities|

The global reset, unfiltered random kill, broad player effects, arbitrary-distance teleport and lucky-block pool dependencies are separate admission/lifecycle problems. Some branches reach TMT-connected reward graph territory, so this is not an independent small next-port batch. Source text/media need not be copied to establish these mechanics.

## Paper and Ignite evidence

`plugins/luckywars/src/main/java/me/luckywars/SigmaWarsMain.java:74` calls `SwrgNativeService.register`. The legacy runtime's `register` calls start; `LegacyFunctionRuntime.java:108` schedules load functions and global tick-tag functions every Bukkit tick, and registers `legacyfunction` dispatch. Fragment/Cunny exact-name Java search in Paper found no dedicated native override; datapack leaves remain the relevant source definitions. This establishes generic runtime registration, **not an observed successful click** for these families or proof every nested command works.

Ignite source tree was inspected: only the BedWars hook/mixin Java pair exists there; no clock/Cunny/time-fragment direct handler found. Do not assign fragment ownership to Ignite merely because it is an available legacy source.

## Native owners and unresolved implementation contract

Reuse `LbcTimeStop` for dimension stop, frozen native bodies, contact/projectile damage queue and checkpoint state. `reduceSourceCooldown` is an existing source-seconds adapter; `addPlayerCounters` modifies that owner's timer. Neither is a global replay facility. `LbcStand` owns Clocks and Overheaven; native mana/status/ability services own their individual counters. `LbcSourceCounters` reads/writes canonical source charge counters; it is not a scheduler. `LbcFrost` owns cold/frozen timers and damage-cause attribution on affected entities, with no need for a second cold ledger.

`LbcVanityRollback` already coordinates whole-state capture/restore, admission blocking, staged native body/storage/world restoration and publication. Reuse its typed ownership if new queued replay work is ever added. A forward replay is not Vanity restore; restore is not permission to call arbitrary functions. Existing successful Clocks/Overheaven/Vanity work is a dependency to preserve, not an unfinished item recommendation.

A future finite implementation must identify the *native* owners reached by every source tick family, define one typed logical pulse boundary, retain source ordering/RNG, stage heavy work through existing fair native work, keep accepted cursors checkpointable, avoid duplicate natural ticks and ensure stop/end/owner departure cancellation. No arbitrary interpreter, string-command bridge, whole-world/NBT scan or per-cell actors proposed. Current audit does not claim full descendant closure or runtime support.

Material author questions for root backlog only (question file untouched): whether literal global-only script acceleration including tick5t anomaly/30s?90s is intended; behavior of ordinary native entity/block/scheduled ticks during acceleration; source no-local-jammer policy versus desired classification; used-hand versus matching-inventory consumption for Cunny; whether source TODO variants remain consume-only; admission of reset/unfiltered kill/global effects/large teleport in live matches. These are not answered by existing Clocks MAG policy, native TimeStop pause rules or broad rewind qualification.
