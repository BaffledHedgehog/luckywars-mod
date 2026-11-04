# Next independent native chain — 2026-11-04

Read-only bounded audit during root cases qualification. Only this note written. No code/resource/build/server/question/RP changes. Workspace E:/servers_mew/skywars2_last/luckywars-mod; legacy read-only. TMT LAST; author272/Academy/Reroll protected. Current completed BadApple/trinkets/levelups/skills32/36/cases and cosmetics excluded. No new question-file entry.

## Recommendation: IMBA Bridge Builder, most_imba

One substantial standalone candidate is source item **bridge_builder_imba / Великий мост vB.AFF**, custom_data most_imba:1,gadget:1,imba:1, snowball carrier and lbc:bridgebuilder model. Source producer is E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/imba/lucky_items_imba.json:754–790. This is a real item in the IMBA source pool; implementing its semantics does not authorize pretending the entire IMBA reward pool is newly ready.

Most importantly, it is NOT the already qualified old projegadget:1,most:1 wool-trail bridge. LbcBalanceExtras.loaded:66 enrolls the old snowball and tick writes lime wool under that projectile. Exact current Java search found no most_imba or imba_most consumer. Converted item_defs/lbc/bridgebuilder__d20c5b8711ad.json exists but still produces minecraft:snowball with source custom_data; this proves a definition, not native actor behavior. No ported bridge-builder entry or ready catalog membership exists. Do not substitute the old bridge owner.

God Hedgehog is NOT missing: LbcGodHedgehog exists; LbcItems constructs it:1103, enrolls loaded:1515, unloaded:1638 and ticks:2072, and native damage/Consuming integration explicitly handles it. Existing qualified warnings/spawn notes corroborate this. It is excluded from recommendation.

## Exact live source entry and finite dependency closure

F = E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function.

projectiles/eggs.mcfunction:32 detects nearest just-created snowball most_imba and calls other/imba/most_spawn. most_spawn creates marker tags imba_rc/slowraycast/imba_most/mecha; rotation copied from nearest nonspectator player with snowball>=1. Native launch must capture actual causal thrower and rotation, never nearest unrelated player. Source marker has no data.lifetime.

tick_not_timestopped:3 dispatches ordinary-dimension slowraycast actors to raycastselector, which increments Lifetime then dispatches imba_rc to raycastselector_imba:6. This invokes most_tick only while a nonspectator player is within200 of marker. Nexus/Imprinted excluded by shared tick. Missing nearby player makes actor dormant, not destroyed. No source finite lifetime or completion cutoff was found; this is a finite-state/procedural chain with potentially persistent actor, not a fixed-duration beam. Native lifecycle must retain dormant state and clean it at match teardown/epoch retirement.

Exactly14 local functions, no external function calls from their bodies:

- most_spawn.
- most_tick -> most_tick_column_pre, most_tick_random_colon_select, most_tick_random_double_most_create.
- most_tick_column_pre -> most_tick_column -> itself.
- most_tick_random_colon_select -> most_tick_random_colon_select_2 -> most_tick_random_colon -> most_tick_random_colon_2 -> itself.
- most_tick_random_double_most_create -> _2 -> _3 -> _loop -> itself and _loop_3 -> _loop_2 -> itself.

Seven structure dependencies: most.nbt, most_column_1.nbt through most_column_6.nbt. All seven existing native src/main/resources/data/lbc/structure files compared byte-for-byte equal to source structure files. No resource conversion or RP edit required for those templates. Exact main-template SHA256 dc4b95c6f0564cbe00e34d0c9b4168c88673e3223c7397258f318f535182c8cf. Source byte sizes1381,379,415,302,403,556,377 respectively. No external command service, currency, random loot terminal, TMT call, Academy or reroll dependency occurs in this local function/template graph.

## Full behavior to implement, not a trigger stub

Each active actor tick moves local-forward4, places lbc:most at world offset(-4,-10,-4), rolls50% decorative columns and5% perpendicular/parallel side bridges. Every seventh Lifetime invokes main column support work. Lifetime increments even when nearby-player gate prevents work, so modulo phase follows global active tick dispatch, not only move count.

Main support uses a random1..6 column template placed at offsets(-1,-15,-1) for variant1 or(-2,-15,-2) for2..6. Counter y starts marker PosY; each recursion reduces counter4 while execution position descends5, continuing while counter10..319. Preserve this source mismatch instead of assuming vertical pitch4.

Decorative columns choose sign of lateral offset then uniform8..16 magnitude, independent vertical offset-20..10; macro places local lateral then world height. Five column-start positions form small raised/depressed pattern; each writes quartz pillar below and sea lantern, then90% recursively rises1 while execution position remains within20 of actor.

Side bridges choose yaw0/90, X/Z offsets-10..10, Y-20..20; start two opposite directions. Each loop fills5x5 chiseled-quartz deck,3x3 lower quartz bricks, four rail corners,50%sea lantern. After second step,80% support branch fills intersecting quartz-pillar arms, descending until the second fill reports no changed blocks. Loop advances local5 while within200 of original actor. Native planned-overlay semantics must reflect previous accepted writes for fill-success recursion; merely testing committed world while queue lags can create excess recursion. World build limits and already-identical fill terminate support descent; do not invent a fixed shorter radius/pillar limit or recursively block the server thread.

Necessary native owner: canonical Polymer snowball carrier/input with single debit and actual owner/aim; virtual marker state (position/rotation/Lifetime/nearby eligibility); shared fair terrain queue with ticketing and explicit saved template/cell, branch/RNG/recursion cursors; immutable existing templates; source attribution for terrain and delayed consequences; shared TimeStop; typed full coordinator and x2/x4 integration. No entity per cell, world-wide NBT scans, runtime command interpreter or synchronous giant template loops. Accepted work must drain without dropping branches; use logical generation/backpressure consistent with existing GodClaw/Expansion/Supernova terrain owners. Native current BedWars protected original terrain and beds remain mandatory established guards, not copied source bypasses.

## Suppression and availability boundaries

No local Most source entry/tick clause checks MAG or TECH. Marker has tag mecha, but this alone is NOT proof of source TECH suppression: other/stopper:2 selects ENTITY TYPE #minecraft:mecha; source data/minecraft/tags/entity_type/mecha.json excludes marker. Its values are minecart/lightning/TNT/creeper/arrows/golems. A source Danmaku bomb does select raycaster tag=mecha, so existing shared bomb-clearing integration must include virtual actor if applicable. Do not conflate that removal contract with nearby TECH suppression. Existing source-faithful night default may retain no new local jammer check, but that remains provisional, not author approval. A source classification question may be backlogged by root without blocking independent implementation if night policy permits that default.

Q1–33 contain no Most-specific design choice. This chain does not depend on Homing30, Phoenix31, Cinema35, Cunny33, permanent economy, case/cosmetic rulings or time-fragment32. Shared TimeStop is existing infrastructure, not fragment semantics. Exact source graph has no TMT references; the fact that protected TMT's tag vocabulary lists most_imba does not make ordinary Most implementation a TMT producer port.

RP read-only check confirms existing minecraft/lang/ru_ru keys bridge_builder_imba and two lore lines3299–3301. Preserve source model/tier/name/lore. No new design or audio is required by these14functions. Physical-client presentation and actual runtime qualification remain future work.

This is an independently implementable complete family under existing source-default night policy. It requires substantial native terrain actor work and full qualification before ready exposure; imported snowball and old Most handler do not close it.
