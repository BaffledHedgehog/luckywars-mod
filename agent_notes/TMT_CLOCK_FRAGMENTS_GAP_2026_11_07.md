# Clock fragments: explicit disabled domain and native owner gap

Source-only audit, 7 November 2026. No Java, resource, registration, build,
JVM, semantic/JDT, server, artifact or protected author-file changes. This note
does not implement or qualify a clock-fragment owner or TMT adapter.

The applicable preserved release instruction is `AGENTS.md:620`: **"public
fragments stay OFF."** The underlying qualified note
`INDIVIDUAL_SUFFIXES_2026_11_04.md:43-57` distinguishes the operator-only
`time-fragment-native` experiment from usable items and says **"Those items
stay disabled."** These are recorded project gates; this audit did not locate
a separate verbatim author quote approving public acceleration. Parent scope
explicitly preserves OFF and forbids silently substituting native sprint.

## Exact source and existing native boundary

Read-only legacy root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
`function/workingitems/spell1_default.mcfunction:70` and
`spell1_default_left.mcfunction:55` select `clocks:1`; the two
`workingitems/clock_fragment/select*.mcfunction` files reset `RC_Raycast` and
read string `type` from their respective real hand. Sixteen variants in
`loot_table/clocks.json`: `1t,2t,5t,10t,1s,2s,3s,4s,5s,10s,15s,30s` and
`jump_1s,jump_2s,jump_5s,jump_10s`. **30s calls tick90s.** No local payment,
debit, cooldown or jammer guard is authored in these selectors/jump bodies;
outer shared spell-input gates are a separate dependency.

Direct types re-enter global script families, not actual Minecraft world ticks:

| Source family | Immediate work and source-owned recurrence |
|---|---|
|tick|TimeStop/non-TimeStop branch plus Nexus tick; no schedule in this wrapper|
|tick2t|2t recurrence; marker_2t raycasters and mobs_2t|
|tick5t|5t recurrence; mobs_5t, rc_5t raycasters, two homing-fireball branches|
|tick10t|10t recurrence; rc_10t clouds, players_10t, BedWars10t|
|tick1s|1s recurrence; raycasters, Greed diamond, ground kills/crafts, snake, mobs, drone host, events, target curse, Nexus players, BedWars1s, timer+1|
|tick2s|2s recurrence; tick2smobs and explicit overworld forceload|
|tick3s|3s recurrence; area_3s raycast and summoned stranger cultist|
|tick4s|4s recurrence; area_4s raycast|
|tick5s|5s recurrence; players, lucky mobs and BedWars5s|
|tick10s|10s recurrence; BedWars, players and mobs|
|tick15s|15s recurrence; BedWars, crocodildo disappearance and players|
|tick90s|45s tick90s_45s schedule plus90s recurrence; conditional Imprinted darkness transitions|

All recurrence calls use legacy `schedule function` semantics, including its
default replacement behavior; a re-entry can alter the natural future phase.
Many entity branches explicitly exclude Nexus/Imprinted, while several global
and dimension-specific branches do not. Mapping every family to one generic
native item-service tick would lose these boundaries and repeat unrelated work.

Jump files contain literal ordered synchronous family calls. Per nominal second
their base sequence contains20 tick,10 tick2t,**5 tick5t**,2 tick10t and1 tick1s.
The extra tick5t is source behavior. Extra slower unconditional calls and
independent probability draws are fully recorded in
`NIGHT_TIME_FRAGMENT_SOURCE_2026_11_04.md`, rechecked against current jump files:
jump1s probabilities50/35/25/20/10/7/3 for2s/3s/4s/5s/10s/15s/90s;
jump2s70/50/40/20/15/6 for3s/4s/5s/10s/15s/90s;
jump5s50/15/35 for10s/90s/15s; jump10s35/70 for90s/15s.
Literal order is significant. No new speed cap is proposed.

`LbcStand` / `LbcTimeStop.useClocks` implement **stand_clocks**, a distinct
dimension-stop item. `LbcTimeStop` owns native frozen bodies, accepted damage,
remaining/cooldown and checkpoint state; its source-counter helpers are not a
global forward replay facility. `LbcItems.tick()` already aggregates many
independent native owners with natural-tick phases and TimeStop branching.
`TimeFragmentSprintChecks` is an operator fixture using genuine native sprint;
its existing small-scene result cannot establish equivalence to the source
script re-entry, recurrence replacement, or all sixteen fragment behaviors.

## TMT finding and proposed next boundary

Canonical `src/main/resources/skywars2/tmt_use_profiles.json:17772` onward
retains all16 carrot-on-a-stick prototypes with private string `type`. They
must remain visible evidence; `clocks` is not a service marker. The catalog has
`LEGACY_OWNER_GAP`, but no explicit disabled-gameplay disposition. Do not mark
the missing owner as a completed event adapter or claim full TMT closure.

Proposed root decision: retain an explicit clock finding with reason
"Public time fragments intentionally OFF; source scheduler re-entry has no
qualified native owner." If a released-useful-pool exclusion is adopted, give
it a distinct disabled-domain disposition and source gate, rather than
`EXCLUDED_SERVICE`; preserve a separately visible deferred owner gap. This
note does not silently change catalog disposition or `hasPending()` behavior.

Before actual implementation: reconcile each descendant against its existing
native owner; define typed pulse boundaries and recurrence phase ownership;
retain exact source order/RNG and both real hands/private TMT type; checkpoint
accepted work through the existing coordinator; prove TimeStop, cancellation,
natural-tick overlap and source schedule replacement. No function interpreter,
manual entity/world loop, scalar timer shortcut or native sprint substitution
follows from this audit. No production hook is requested this batch.
