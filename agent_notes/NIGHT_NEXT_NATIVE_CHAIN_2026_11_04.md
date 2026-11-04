# Next independent native chain — 2026-11-04

Read-only bounded audit during root cases qualification. Only this note written. No code/resource/build/server/question/RP changes. Workspace E:/servers_mew/skywars2_last/luckywars-mod; legacy read-only. TMT LAST; author272/Academy/Reroll protected. Current completed BadApple/trinkets/levelups/skills32/36/cases and cosmetics excluded. No new question-file entry.

Historical pre-implementation audit. Most and the redesigned Axe have since been implemented under [the author answers](NIGHT_ANSWERS_2026_11_04.md); current behavior and qualification belong to [the current report](NIGHT_ANSWERS_IMPLEMENTATION_2026_11_04.md). Statements below about absent Java consumers and provisional defaults describe the audit snapshot, not the current release.

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

## Next after Most: complete IMBA Axe harvest job (imbaxe)

Subsequent bounded audit finds another independent finite family: **IMBA Axe**, source translate imba_axe (actual RP Russian value Хаос-Хуйня), custom_data imba:1,imbaxe:1. This recommendation excludes all completed families above and does not require Q1–33, Most itself, TMT, Cinema, Homing/Phoenix, fragments, Cunny or Mellstroy. No implementation changes made.

Actual producer: LBC loot_table/imba/diamondtools.json first pool, uniform0..1 roll, netherite_axe, efficiency255, minecraft:golden_axe model, lbc:tier_k, imba_axe name and imba_version lore. Current converted item_defs/minecraft/golden_axe__9834f449f80c.json still outputs **minecraft:netherite_axe**, not a registered behavior-owning native custom item. Exact current Java imbaxe search has no match; ready loot catalog contains no imbaxe entry. These are direct flag/entry checks, not an inference from absence of a class name. Converted advancement retains rewards.function, but no corresponding packaged mcfunction was found; source reward string alone is not a working native consumer.

Actual live source trigger is LBC advancement/imbaxe.json: minecraft:item_durability_changed while SelectedItem custom_data imbaxe:1, executor not spectator and not Nexus/Imprinted. Rewards directly invokes lbc:other/imba/imbaxe_use. This is not a hypothetical right-click, nor merely a dormant source function. leave_death:103 revokes the advancement, and use terminal revokes it for repeatability.

Exact source function closure is just three files:

1. other/imba/imbaxe_use (1237lines) resets216 count/IF families, snapshots its source target selector by command execution, calls imbaxe_use_at for each center, copies resulting scalar counts to storage, calls imbaxe_use_macro, revokes advancement.
2. imbaxe_use_at (479lines) has216 exact fill-replace predicates then corresponding count accumulations. Every fill is world-axis cube[-15,+15]³ centered at selected entity and uses minecraft:air replace <predicate> strict. There are215 concrete block IDs plus #minecraft:leaves. Leaves credit stick count. Selector is @e[type=#minecraft:mobs_and_markers,tag=!spectator,tag=!imbaxe_used], with execution at each center, unless dimension nexus, and requiring a nonspectator player within120. mobs_and_markers source tag is #mobs plus marker. No code in this three-file family adds imbaxe_used; it is only an exclusion predicate, not permission to invent once-per-match harvesting.
3. imbaxe_use_macro (237lines) has217 give rows,216 unique item IDs. bamboo_mosaic appears twice in output sequence. All217 rows use the IF==0 condition; counters begin0 in entry. Preserve source count and duplicate-output behavior unless author changes it. Three mushroom outputs have custom item_model/name/lore/block-state components representing normal/Neko/Loli lucky blocks, not plain vanilla mushroom blocks. Canonical native LbcLuckyBlocks output owns their behavior and must be reused exactly.

No outgoing function calls beyond these two internal edges; no referenced loot pool, scheduled function, template, external plugin command, media, currency, Academy, reroll, TMT or random reset terminal. This is substantial because its complete harvesting/mapping/output transaction spans216 palette cases and arbitrary finite selected-center cubes, despite having only three source files. Source selectors operate in executor's dimension; they are not an all-world scan. Initial advancement excludes Imprinted, and native ordinary-dimension policy remains mandatory.

A complete native implementation should enroll the **real durability-change event** for the actual source item, capture actual owner plus eligible center positions once, and enqueue shared fair world-work with immutable full216 predicate/output mapping. It must retain per-center/cell cursor, cumulative counts, eventual item-output cursor and source cause in typed full coordinator, shared TimeStop, epoch teardown, x2/x4 adapters, protected original terrain/BedWars roles and tickets. Overlapping cubes naturally count a removed block once because later centers see AIR; a one-pass palette lookup per cell is equivalent for this disjoint source palette, provided accepted queue state/ordering and #leaves tag semantics are honored. Do not loop216 full cubes synchronously or re-enumerate entities each tick. No source-center truncation or nearest-only substitute. Exclude protected helpers under current native role policy rather than treating service markers as an attack target.

Source uses strict AIR replacement, **not block loot**. It returns raw mapped items even for chests/barrels/bee nests/lecterns and does not preserve block-entity contents; do not silently convert into Fortune/Silk/ordinary drops, duplicated container payload, or mass normal block-break events. Existing native world checkpoint/immutable block-entity snapshot infrastructure still owns rewind. World-change attribution must credit actual source player for resulting unsupported terrain/fall/void consequences per author policy.

No local MAG/TECH stopper check occurs in this closure or advancement. Under root's existing source-faithful night default this is provisionally unjammable, not fresh author approval; suppression classification can be backlogged if desired. The source duplicate bamboo_mosaic give row and container destruction are explicit observable semantics, not hidden invented defaults. These do not depend on any currently unanswered Q1–33 ruling. A future source ruling can adjust them without touching protected Academy/Reroll files.

The acquisition pool remains existing full IMBA graph; registering one qualified native IMBA Axe leaf must not advertise the entire graph as complete. Public exposure should be a new exact ported leaf only after full trigger, palette, overlap/count, output, protected role, TimeStop, pending-work and whole-coordinator qualification. Actual author RP already has vanilla golden-axe model plus imba_axe/imba_version translations, so no RP write or design change is needed.

### Additional Most source facts sent to its draft owner

Read-only gzip NBT parse confirms most size9³,337block rows,palette16; column1 size3x5x3,45rows,palette4; column2 size5³,53rows,palette6; column3 size5³,45rows,palette1; column4 size5³,65rows,palette3; column5 size5³,125rows,palette4; column6 size5³,45rows,palette4. All seven have zero entities, zero alternate palettes and zero block-entity NBT rows. Source launch origin is player FEET: players_tick:7 -> items/snowballs:4 -> projectiles/eggs:32 -> most_spawn has no position modifier on this call chain. Earlier anchored-eyes commands in snowballs have independent execute scopes and do not persist into the gadget call. Native eye/projectile origin would change source geometry.
