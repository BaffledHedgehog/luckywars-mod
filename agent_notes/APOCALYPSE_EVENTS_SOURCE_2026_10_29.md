# Three apocalypse events: exact source contract, 29 October 2026

Read-only audit; no Java/resources/build/server/RP edits. This note does not
qualify acquisition, event completion, source clocks or peak load. Full original25
Chaos pool release gate remains closed. Witch family remains other agents' scope.

## Actual item outputs, acquisition and activation

Only two original loot tables emit the three raw flags (searched BOTH lbc/lbcr
loot roots for literal custom_data flags, excluding mere translations):

| Table | Exact native-source pool |
| --- | --- |
| lbc:end_of_everything | rolls1, pool random_chance .3; snowball End weight20 and Baffled weight10. Thus20%/10% unconditional output probabilities and70% empty. Both gadget:1 plus matchingflag1, source name/lore, tier_k, model lbc:end_of_everything or lbc:baffled_of_everything. No custom count/consumable/projectile TTL. |
| lbc:end_of_technomagic | rolls1, singleton snowball gadget:1,shish_of_everything:1; model lbc:shish_of_everything,tier_k,white nonitalic name shish_of_everything and aqua end_of_everything.lore. |

Direct table-reference callers: Bob and Aimbob first pool reference
end_of_everything; reactive_shish and random_glack_1 reference
end_of_technomagic. The forest rare reactive Shish death and raw imba spawn egg
connect to reactive_shish; random_glack and27_random_items connect to
random_glack_1. Root's current original output tables are packaged/adapted, but
no native Java consumer of any of these three event flags was found.

Exact activation is immediate native snowball THROW, not collision or dropped
item activation: players_tick sees snowball stat>0 -> items/snowballs -> nearest
snowball custom_data gadget:1 -> projectiles/eggs. Each rawflag calls its
other/{everythingend,baffledend,shishend}/1 with `positioned 0 64 0` and existing
executor/rotation/dimension context. No MAG/TECH/artifact test on these three
branches. Afterwards the nearest gadget snowball within10 is killed unless
its item has tmt_item:1. Native snowball owns item debit/stat/throw sound/motion.
Do not retain the projectile until impact or invent a countdown/TTL. The old
nearest-snowball ownership ambiguity should use the existing accepted native
thrower for attribution, not acquire an unrelated nearby snowball's author.

## Scheduling and selector rules

All source schedule calls omit mode, hence REPLACE BY FUNCTION IDENTIFIER. A
new activation replaces that next specific phase's pending invocation, NOT every
pending later phase of the same event. Older advanced phases can coexist with a
new phase1 chain. Three different families are independent. A single global
replace-one-entire-event state would change this behavior. A typed per-phase
pending registry can preserve the exact original function-ID semantics without
running/interpreting source functions.

Scheduled functions run as the server command source, without original entity,
rotation or positioned0,64,0; current mapped26.3 FunctionCallback invokes
ServerFunctionManager.getGameLoopSender -> server.createCommandSourceStack
(server respawn dimension/position,rotation0). All non-position-limited selectors
have native cross-dimension semantics; distance/AABB selectors stay local. Every
separate execute selector is reevaluated, including native sort=random at each
line, so freshly spawned TNT/markers/mobs can enter later draws. Never select one
fixed target list for an entire phase containing separate commands.

Source does NOT provide match epoch, owner attribution or global TimeStop pause;
current common gameplay role/epoch/TimeStop/WILD requirements must be explicit
native adaptations. Pending author policy questions are recorded below. Retain
raw caster for causality, not as a new filter limiting all global source targets
to their arena or current world. No forced chunkloading for fill/template actions;
source native loaded-position/command-size failures skip the individual command.

## Everything: 58 scheduled phases (ending tick1863)

Times below are ticks after an uninterrupted initial activation; the manifest
below records EVERY phase time/delay independently from original files.

| Phase | Exact operation |
| --- | --- |
| 1..15 (t0..28,every2) | At EACH swrg.look entity, strict air cube radiusphase (3^3 through31^3). Phase1 force white dust scale4,count1000,spread2/100/2 at0,64,0. Title blank+subtitle the_salt. After15 wait40. |
| 16..23 (t68..103,every5) | At every swrg.look fill lava cube radius1..8 (normal updates). Blank+our_matter. Then100tick pause. |
| 24..27 (t203,218,233,248) | Each phase9 independently random draws of up to10 @e[tag=!spectator], spawn normal TNT fuse80 at Y+4, horizontal(+5,+5),(-5,+5),(+5,-5),(-5,-5),(0,-5),(0,+5),(+5,0),(-5,0),(0,0). Phase24 subtitle hermes_bird; wait40 after27. Native TNT physical spread/updates retained. |
| 28..31 (t288..318,every10) | Four separate draws each phase excluding lightning/tag spectator. Phase28/30 drawlimit48 and cardinal offsets5;29 limit48 diagonals;31 limit64 diagonals. Native ordinary lightning. white_eagle subtitle at28; wait40 after31. |
| 32 (t358) | Up to100 random ALL entities (NO spectator exclusion), strict emerald_block replace minecraft:solid in radius6cube. yellow_lion; wait60. |
| 33..36 (t418..463,every15) | Up to64 random minecraft:mobs,tag=!spectator each phase, native large fireball power8 at diagonal(+20,+20,+20),(-20,+20,-20),(-20,+20,+20),(+20,+20,-20), motions towardcenter eachcomponent.5. red_lion; wait40 after36. |
| 37..48 (t503..613,every10) | Fixed ruby rays, not aimed cloud! At random minecraft:mobs,!spectator spawn magic slowraycast raycastruby tuntija can_deleted marker lifetime50, offsets(+20,0,0),(0,0,+20),(-20,0,0),(0,0,-20), yaw90/180/270/0. Three groups limits16,8,4; celestial_ruby at37; wait60 after48. Reuse Ruby.flying50 under explicit hostile source. |
| 49..52 (t673..703,every10) | Up to64 random nonspectator-tag entities eachphase. Native fireballs power2, offsets cardinal20, motion1 towardcenter; magnesia subtitle49; wait40 after52. |
| 53 (t743) | Up to24 random entities tag=!spectator,atY+30 -> select_eoe: uniform1..9 stationary hostile cloud (ruby,terra,electro,air,acid,teleport,stone,fire,blind), source display transform/model/tags exactly in helper. the_vessel_of_the_philosophers; wait140. |
| 54 (t883) | Up to100 random NONPLAYERS -> stevari summons source fat_skeleton at target,then kills executor ONLY if type minecraft:mobs and notplayer. Markers/items survive conversion. Exact skeleton gear differs existing ordinary/drop/egg skeleton presets: guaranteed mainhand,feet,legs;head/chest0;source attribute IDs sdiuirj4938uhf,idjf9u8weu89;DeathLootTable fatskeleton. our_blessed_stone; wait100. |
| 55 (t983) | Up to32 random MARKERS -> native Slime Size20,tags worm,lucky_mob. mobs_5t_lbc worm atY+3 strict clears radius5cube EACH5ticks. Slime itself defaults native AI/health/splitting; magnum_opus; wait140. |
| 56 (t1123) | Two independent draws: up to20 nonspectator-tag entities place gold48 at-24,-24,-24 integrity.05; up to10 nonspectator-tag entities set native spawner atY+10. Exact18-weight1 splash-potion potentials/settings in frozen source56. philosophers_stone; wait300. |
| 57 (t1423) | Up to16 random NONPLAYERS -> nuke_eoe warning20seconds/smallbeacon and global nuke1 schedule. sampo; wait440. |
| 58 (t1863) | Award end_of_everything to every nonspectator GAMEMODE player; subtitle true_knowledge; at EVERY entity tag=!spectator (not merely players/mobs) set chest with LootTable lucky_items_1. No automatic chest open/debit or guarantee drops. |

`nuke_eoe` is NOT bruh: strict radius1netherite base,strict vertical clear from
Y+3 separately through319 AND255,beaconY+1/redpaneY+2,sound nuke volume.05,
pitch1,minVolume.05; schedule lbc:other/nuke/nuke1 400ticks; marker has
nuke,nuke1,rc_1sec,marker_2t,mecha. nuke1 later selects ALL tag=nuke1 sources,
including any preexisting bruh marker. It calls normal nuke1_at (point power25,
12airtemplates,Wither radius45,killradius14), NOT bruh's random16nestedtarget
power16/13template variant. Every20ticks ordinary nuke marker executes nuket:
only stooper within10 blocks its smallbase/clear/beacon/redpane writes.
Root explicitly owns adding this exact warning/schedule variant to NukeEvent;
existing pointBurst can supply its normal geometry after required integration.

## Baffled: 8 phases, ending tick1040

| Phase/time | Exact operation |
| --- | --- |
| 1/0 | Subtitle Baffled; execute AS every minecraft:mobs,!spectator ATself -> snus. Must support nonplayer living mobs and repeat broadcast for each; player-only LegacyEffects.snus is not equivalent. |
| 2/80 | Subtitle Hedgehog; TWO separate all-entity tag=!spectator selections -> invulnerable native silverfish tags minibaffled. Second selection can include first wave! No arbitrary cap. |
| 3/200 | Subtitle Slab; foreach all nonspectator-tag entity fill FIXED XZ144..-96 at THAT entityY strictair.241x1x241=58081. Source native command modification limit/residency can reject invocation. |
| 4/360 | Subtitle Hedgehog Rider; AS every minibaffled summon native silverfish with source wither-skeleton passenger+guaranteed named/lored ironaxe,cloud10 and extinguish.5/1; then teleport original minibaffled to0,-100,0,NOT kill/discard it. |
| 5/520 | Subtitle Imbaffled; four independent NONPLAYER random draws:50 skeleton tokens,25 bob,25 aimbob,1 sauvojen_tuntija. Source actual native item drop custom_data drop1+flag1,then shared SpecialDrops handles one debit and actual actor. |
| 6/720 | Subtitle Yamete Hedgehog; foreach nonspectator-tag entity loot spawn yameteluckyblock (BOTH actual LuckyBlock and Yamete rawdrop). |
| 7/800 | Subtitle SkyBaffled; foreach first15 nonspectator-tag entities (NO random sort) loot spawn drops/magic_bean. |
| 8/1040 | Subtitle Lucky Hedgehog; nonspectator GAMEMODE PLAYERS remove all vanilla effects,grant infinite Luck4;give64 exact original normal/Neko/Loli mushroom blocks source names/lore/blockfaces/models/tier_u;award end_of_baffled. Do not reduce to one luckyblock family. |

## Shish: 11 phases, ending tick2000

| Phase/time | Exact operation |
| --- | --- |
| 1/0 | Each nonspectator GAMEMODE PLAYER native forest marker lifetime800,rc_5t,magic. Shared ChaosTrapActors forest engine already has exact12feature/babyzombie/Shish/native spread behavior; needs direct event producer bridge preserving800,not synthetic token debit. Subtitle Shish. |
| 2/100 | Set coldownshish34 for nonspectator players (not+34);native Levitation60amp16 hidden;Jet Shish. Reuse actual Reactive score/owned-effects bridge rather than newliftengine. |
| 3/200 | Worldborder center0,0,size382.75 in scheduled server world,immediate set;The Great Wall. |
| 4/300 | FOUR markers per nonspectator player;native command /data get coordinate floors then independent random integers-10..10 forX/Z;Y+8. Spawn exact hostile electro display/drone;delete temporary pre markers. Blue Lightning. |
| 5/500 | Each nonspectator player loot yellow_lightning;Yellow Lightning. This raw item passive hotbar/held source graph requires native bridging; it is not an ordinary Electro wand. |
| 6/600 | Summon40 supply_point markers at0,128,0;native spreadplayers0,0,min0,max192,notteams. First marker starts risk_of_rain. Global second schedule consumes ONE marker each second,creates source beacon display,sets supplyFall99 then immediatelyruns->100;last empty tick gives16arrows at EVERY tag=supply_drop entity. Risk of Rain. Commented nonexistent randomize_position/supply_point is NOT live. |
| 7/1000 | Subtitle The End has not come to an End.. only;next400ticks. Must not invent attack. |
| 8/1400 | For EACH nonspectator player and EACH item_display supply_drop within20 spawn75HP skeleton cyborg_boss,lucky_mob,mk2,new atdisplayY+1;source six0dropchances,attack2,silent,yaw90. Native leaf loot replaces4slots from neko_set_technomk2 thenremove new;start shared cyborg tick. Same display can spawn multiple bosses for overlapping players. CyberShish. |
| 9/1800 |30 temporary markers PERnonspectator player,independent floorXYZ+uniform-30..30 eachaxis;align+(.5,0,.5) persistent stopper/rc_1sec/marker_2t/not_block marker;native rodactivation+TECH destructive source semantics;deletepre markers;iron_golem.death1,pitch0 for allplayers;End of Technology. |
| 10/1900 | Same30/player positions for stopper_magic;beacon.deactivate1,pitch0;End of Magic. not_block suppresses blockcube deletion,not entity/effect suppression;no TTL was given. |
| 11/2000 | Each nonspectator player loot source_of_energy (10weighted draws);Source Of Energy;award end_of_technomagic. |

## Existing engines versus real remaining work

| Piece | Current native evidence / required bridge or real missing engine |
| --- | --- |
| Global77phase orchestration | MISSING. Needs typed phase schedules,exact selector scopes/order/payloads/terrain work and physical lifecycle/cause. No Runtime functions/interpreter. |
| TNT,lightning,fireballs | Native entities plus existing common admission/childsource/TimeStop infrastructure reusable; event must keep80fuse,TNTmovement and power8/2fireballs exact. No helper creepers or narrower instant explosions. |
| Terrain templates/fills | LbcDropBlocks + MapManager slices + shared queuedwork reusable;gold48/air physically exist. Must preserve command strictness,tag filters,randoms,resident/limit failures and source pendingfunctions rather than waiting forever. |
| Fixed Ruby50 | Existing Ruby.flying lifetime50 reusable with managed event Source;ordinary sourceEmitter lifetime60 is DIFFERENT. |
| Nine/electro cloud producers | LbcSauvojen.emitHostile supports actual9native engines. Existing Boss cloud implementation has correct displays/cadence but private producer and bossguard; expose generic event cloud source,do not create a Ghast merely to borrow it. |
| Forest | Exact engine exists in ChaosTrapActors;add direct source producer for events. |
| Snus on allmobs | Current generic LegacyEffects helper is player-only/private;requires narrow LivingEntity source-equivalent shared helper. |
| Stevari | Existing CreatureDrops fat-skeleton presets differ loot/dropchances/attribute IDs. Native source payload can use common managed actor admission,without aliasing its distinct payload to ordinarydrop. |
| Worm | Native slime alone exists;worm5tick strictterrain handler MISSING. Needs shared event actor sideeffect producer preserving split children source lifetime/epoch. |
| EOE Nuke | Root-owned exact20s variant of NukeEvent is pending;pointBurst alone notwarning/schedule equivalent and bruh.start iswrong. |
| Supply/RiskOfRain | Real MISSING engine:40point pacing;global supplyFall tick,land150 damage10radius3 then900radius1 andtwo barriers;open200 display+Interaction1.2x2.4 plus two invisibleArmorStand exact headpayloads. Existing Java has no supply runtimeconsumer. Source armorstands retain allowed head retrieval (DisabledSlots1048623),not auto-granted contents. |
| Cyborg | Real MISSING source engine:75HP MK2payload;everytickHurtTime10 ->4angryelectro traces lifetimes30/35/40/45;shieldResistance20amp4 eachtick;closeplayer<=3 and actualscore cyborgBossFly==0 invokeswyvern (Levitation20amp30,Resistance120amp4,8cactus same_team policy/radius10,Blind100amp0,sounds/cloud1000;cooldown200). mobs5t chance.05 powerhelmet Resistance40amp4 unlessTECH. Fresh dummy score absence must NOT silentlybecome0; source doesnotinitializeFly atspawn. |
| Yellow lightning | Raw passive hotbar/weapon inventory graph: changed_inventory/sel -> yellow_lightning(_active); players_tick held particles; players_5t -> yellow_lightning_ball_pre/ball -> native piss snowball -> projectilestick yellow_lightning_hit/_at. Existing Psycho.piss status is different and does not emit this native snowball. New ShishEncounter agent is auditing this shared consumer; ordinary Electro beam is not proof. |
| Shared drop tokens/Bean | Existing SpecialDrops/Bob/Aimbob/Sauvojen/Bean engines can be used directly withnative actualtokens andcorrect acceptedauthor;maintainoriginaloutput rather than infer fromflagnames. |
| Reward food/Witch/etc | Reuse existingowners; unrelated unfinishedcatalog remains pending. trueknowledgechest lucky_items_1 opens27random lucky_items entries,soevent introduces broad lootclosure,not merely one chest cosmetic. |

Direct event tables and reward packaging (read build include patterns,not a build):
end_of_everything,end_of_technomagic,drops/magic_bean alreadyincluded. Original
cyborgboss,fatskeleton,yellow_lightning,source_of_energy,yameteluckyblock,
lucky_items_1 are on disk but no exact matching buildinclude found. Their old
functions/function,conditions/condition codecs need native26.3 modernization
before actual use; preserve weights,all leaves,entity payload and enchantments.
cyborgboss gives supply_beacon pandaegg(entity_data item_display),SauvojenElectro
and Techno full armor; supply egg must start the SAME actual crate lifecycle.
source_of_energy10draws includes Electro/Teleport wands,electro_shield(weight10),
lightningwand,3Academy Electro/Bolts leaves and lightningegg(weight10). Do not
claim these already closure-complete solely because model/names exist.

## Questions / ambiguities held for parent; no user chat

1. Global TimeStop: pause event deadlines,perworldphaseactions,or sourceglobalclock?
   Current project default ispause; source schedules themselves areunconditional.
2. Multipleactivation exact perfunction REPLACE can overlap laterphases; preserve
   mechanically unless author explicitly chooses wholeeventreplacement.
3. Source Slab58081 exceeds vanilla defaultfilllimit32768. Needactualsourceworld
   gamerule evidence or preserve native failure when limitnotraised; do not
   silently delete a larger slice than original executablecommand permits.
4. Globalselectors versus roleprotectedobjects: preserve native protectedrole
   exclusions for damage/destructive actions,record changedselection/output
   policy. No blanketjammer added to activatingevents.
5. CyberbossFly absent score means source wyvern may remain dormant; preserve
   absent-versuszero until actualsourcewriter found. Broad KFCaddcounter is not
   evidence it exists on a fresh boss.
6. Keep artifacts/markers whose source gives no TTL alive until actualepoch/stop
   cleanup; no invented800tick cap on allshared clouds/jammers/crates.

## Frozen original source phase timing and SHA256 manifest

### everythingend

| Phase | Tick | Next delay | SHA256 |
| --- | --- | --- | --- |
| 1 | 0 | 2t | f56b9719ccb51f3537341a518f79b397fc34154a65609a7b31ced6ee432decf9 |
| 2 | 2 | 2t | 1acec80d08dff8ae8f3f077b37912b9b8ff36ff93a5262eb5a405975ed798801 |
| 3 | 4 | 2t | a4f95aa7bda42c3b3eeee04b6dcc9b1917051293ccec4616e52de53758f5e45d |
| 4 | 6 | 2t | 72ca832bf612873c9607f7bbbd7a5e06f84dff51b71f7db769f6823ffcc3478a |
| 5 | 8 | 2t | 35f322c0772783b461fc0cb2e96a93cb6ce746be0902d358f19d035d51ddb747 |
| 6 | 10 | 2t | 9a82dc9ed14000189a29713e7169e5f1910b90f607b9c6352b6061c09f6fa665 |
| 7 | 12 | 2t | 13d27ddef01b9b6d8629926f628e16b044e2892588c63065be8416ec480b3b5d |
| 8 | 14 | 2t | e4fde43df1e6d4ff1e997782b3a4639453628d732aa517a4624281a390f9a652 |
| 9 | 16 | 2t | d4e471db02623b6b8dc29fd5b55073fd7a723274c1a338cb5f16dc1ebe40dd95 |
| 10 | 18 | 2t | 5204335ee20ac1c95600a8a47c1df35278f20d9be7f56e5ed898765a43eebebe |
| 11 | 20 | 2t | 67c429f0553c397e125ca7c5f20ba0eef1c7062ece47c23643b4041448579ce4 |
| 12 | 22 | 2t | 1692718524cff6629b1ad8f682422c11325418d1ebf5a5ee5cfcd6332e3b2262 |
| 13 | 24 | 2t | 45f00abd75e0fdca7ca7f7fe4f9701e31099a30854273d0ce5dab71ac9ec2932 |
| 14 | 26 | 2t | 777ec1cf5d99314bb0ce5a0533d5875c54089b2a2358e8c4d12b6b415c81a871 |
| 15 | 28 | 2s | 2728ce8b134ad7642c85bd082708ddf4fd9298f9e4fe9ff36a1028dab1baac19 |
| 16 | 68 | 5t | 7c8333940c1b0db33e6f923d632d5688f0e01d0c8e74d9cf3621e6765c242285 |
| 17 | 73 | 5t | 0b98d6e136eafaae0add00c0165537349e9dd1a2ad3b0f297ecf450156ab0990 |
| 18 | 78 | 5t | e9d91562c1ec2ae2ac9ad08c2bbe700f0b2430bc877b075bd5c7d8964bd9bd67 |
| 19 | 83 | 5t | e1d66fc5a9b50f38133013c27f41aa88a64aed79158fda9afdfb7c1bc74ca3d0 |
| 20 | 88 | 5t | 3dda7b9886d6e2d4ac233a55c603f75e2f4c1373607b73ff70e99e3b5bc6d9f2 |
| 21 | 93 | 5t | 8e85bf7104a684a7e9afc1fc76a5560226b9692ac378c1b0a8240f91158f99c5 |
| 22 | 98 | 5t | 64f81f9471eb4c9223754021abc4da8624da5df8e08d425b6867e586d8fe641e |
| 23 | 103 | 5s | 120c90cb04d723ed0d9781d6701efef6fbd49d8bfa41d072445ae1f139770b3b |
| 24 | 203 | 15t | 7204332f2d3dff1f19d34d5a37aab1e20353b4754e132967cc8f7e6a3ccf60c3 |
| 25 | 218 | 15t | ea40905d1675c176c49ffa1a3b85512fc70e9ee0429a81e2ab3f898f91a9238a |
| 26 | 233 | 15t | 94071d8e5d266b8631d6789b3820868a36c86ef1269459212fdf52cd537631df |
| 27 | 248 | 2s | 6b418bf1097c23964f4156a0549a978eca51432a86836228fb00e0dc04a6dd67 |
| 28 | 288 | 10t | 07436efbfc8921b993d3b4d155b938cd9c83b432bbf7eafcf92853f98b0b7873 |
| 29 | 298 | 10t | 3ef981c6bb82ed8d366126cbbfa5267914abf2d568001368dca10fa45459b644 |
| 30 | 308 | 10t | 399b3221807a6b25518ff8afa05a1fcb9a475ca7feaff4d388d44ac29ba83bf8 |
| 31 | 318 | 2s | fa62cc9e1b647400b42051c8dab144211f0ea64d28a689b1166a30d7c6e8af5a |
| 32 | 358 | 3s | d47ff82d4601eb92a9cf364b4931f553b8dc83bfbf934aa617aef468694e3801 |
| 33 | 418 | 15t | 8419d1544bbbf69e877ad428a28b7746cde7ec1fa49bc4ebc5cb2adc4b2a9214 |
| 34 | 433 | 15t | 0c74cc3b5b944d36139b73476d57374f0bcfe928663f04de4b0492389e74ba89 |
| 35 | 448 | 15t | d9699d8e873c9aa1f8a8ecabe604d3f63f474c86f7eff0f095a51b49a596b5ef |
| 36 | 463 | 2s | 1ee2dfae4325fb27a8495cd293decbff0aba02bfc2e214607d7538f7ee33a7f1 |
| 37 | 503 | 10t | ecc4415bf77995d078aa75bd6a81a03bf068d44472db3ad61578b0315291cf4f |
| 38 | 513 | 10t | 1255f02d5187042705cce79571eb92267a3609b77d5b28237e153ca05ff2d502 |
| 39 | 523 | 10t | eb17b79bddc0312b4690a31f502a5a9606db7dd9744559fd6fcd4a36bc66c100 |
| 40 | 533 | 10t | d238fe588da8e01a6d2670b75437a9086854fcf1a8133632dbf11985b7911003 |
| 41 | 543 | 10t | e562f7d04eaf226c4ecca14df2f0fb11b56520370c569a381fcfd69691fa0678 |
| 42 | 553 | 10t | bc38cc8300b7a624345dd30cada5996d7f5f615c03bfdc43912ebd1fdf9106a5 |
| 43 | 563 | 10t | d061ed3c5e51d80b6f2910291c5ded8130ad9391fbf677b5d4b45b3798a87644 |
| 44 | 573 | 10t | bdc124a204a48545f04db45278edbd69ef681ce4f34851175089d3509041edc6 |
| 45 | 583 | 10t | 07f6013b906f154765f15ada20d9d26f6463f0685cffdbc190f8e7e56c9e6f6b |
| 46 | 593 | 10t | db18343320293378ad62de62df43acdbb567aee92361fcc3bd6e20e514e44fe5 |
| 47 | 603 | 10t | 902df5062d2e753cb99ca73792485d434b917ddc3cad91d82009970b0858573f |
| 48 | 613 | 3s | 5002711fa74ab148eb97a0aeb2951edf25f79a80abf9159d4423b51a0b110370 |
| 49 | 673 | 10t | 04dbf1e597e5f1301e21a4d5d203328d2830b66c1262fb098c97fb84d6a52c28 |
| 50 | 683 | 10t | 3c15466f33f9326eb15cd5c321e446fee3211f6113fcf6a059c15ba8e4c1d06a |
| 51 | 693 | 10t | 183598a465c4dcd11eb94ae313aa4053799107017fb6268c0a56d7726fc15743 |
| 52 | 703 | 2s | aa6b4fb4f3d8e517d30657280a631540ab9240f3b8ef2a34ea81cfab0b2925bf |
| 53 | 743 | 7s | f208714c394dbca2c5b794127468d55efbe3a19af77c176196f128b8ee73d33f |
| 54 | 883 | 5s | 2cbeee7391792daa50736ac9bd2b953b15d1ff5a542b9e94b6fad8288e5aac07 |
| 55 | 983 | 7s | 805108c5a638728438b8daf67e8ee7e1db4eddf147e65eaf94523ef8df9d9511 |
| 56 | 1123 | 15s | ab6c1db7f9a4f7e5cdee8c18a75add495549ebabc2e8ac44ba48569f9c80216d |
| 57 | 1423 | 22s | c115bfcb03fae2877c233feca99ac1ba3fa871bd14c80e581b516956cd7643dd |
| 58 | 1863 | end | ba6cf593240aa1080d5091fb7354bd6fe79aa4081baa700dd489fab746c00c31 |

### baffledend

| Phase | Tick | Next delay | SHA256 |
| --- | --- | --- | --- |
| 1 | 0 | 4s | 496b720922d98533fd31316055ba45d548a0ad50ce5f87290250d3345571ffa4 |
| 2 | 80 | 6s | 28d260650e32fdb064a1d8bfb4288d2ccd297c99c094e8f8a2db514715cca204 |
| 3 | 200 | 8s | fea3af34a1302d29143198a68114375b7c3e6fced9ec5c3c7df8f4718260661a |
| 4 | 360 | 8s | 45f591e426d0668a94bb57f5a737455219b999c0fa6c5bd292c0aba613ed3891 |
| 5 | 520 | 10s | ea6a460a744530898497071fe764bf4fab323ea12b4ef08c0d7b5774a43265d0 |
| 6 | 720 | 4s | 7c3350688b32ac6fb5e83548400247d294a06a4f083d33e7e9b8cbfed460d770 |
| 7 | 800 | 12s | 32a96ddf8e0677b4b87d1fa530a60d0b09d3bfa69afb2b389bf6a85739eb516f |
| 8 | 1040 | end | 6c6386517b0538fb40f134b8afd18b90e9d4cde07c30f24d1429a4b0e22cf2db |

### shishend

| Phase | Tick | Next delay | SHA256 |
| --- | --- | --- | --- |
| 1 | 0 | 5s | a82903e22f691681a66884457757429e6da2b4a3853242ce3736cf2f981132c5 |
| 2 | 100 | 5s | 398cb21b2035049e1b4a4dcccff5fda2b7da0ddc508168898c2e928180077178 |
| 3 | 200 | 5s | 885f321ac848f511f8503483355291167a2c87a8e4461771757130527c64ca24 |
| 4 | 300 | 10s | 550ad2d958ff4c64d14a0908123daea8ad98590fe2e01a96d655f9d52e60d051 |
| 5 | 500 | 5s | 575cff2db991b5a5077c8859868beb0a05b20afc15d2f39772eaa14d60ef7d84 |
| 6 | 600 | 20s | 1ed0e480547cbede689c07afb8385346ac88b4aa82ea644fc3d2ccfe5aae2b71 |
| 7 | 1000 | 20s | a57409a06ef90495e478a3900b6ed2ae0052e0156b2b7b46723adfb9d50a0395 |
| 8 | 1400 | 20s | 30e8dde7751847f0be0c4b6757d1bd219a52fc882c0ec818bde43baa6c227fe5 |
| 9 | 1800 | 5s | eba6a9bf88b45d66cb0b101f62a51974a726f188ac0ffcc39c64562d60b10399 |
| 10 | 1900 | 5s | c640770bcb7f3be4e9de9b912544b96dc35f01b52406b48279fcf27ccf9cce0d |
| 11 | 2000 | end | e2200abd7f5ebd808d53540062408728cb5c930296579fe01f6d6544c58f7e3d |

Live literal function closure: 101 distinct files, excluding commented nonexistent supply_point. In addition to numberedphase files, reachable helpers are:

- `lbc:armor/set_bonus/technomk2/shield` SHA256 59470497281416fce912b8d5e9efae2d7242f7050d5fe39c507240290f1a34e5
- `lbc:other/baffledend/hedgehogrider` SHA256 1eedde7d6ffd944d7d10a3f9ed6c0b5cf0112d64b392fb75360d4823285544be
- `lbc:other/everythingend/stevari` SHA256 07ac7ffe6ffc95982584807fb7a0e3a46e104050499593288a110bc7888da899
- `lbc:other/everythingend/trueknowledge` SHA256 80ce2bddf3ac903a729101f3f528dd3fcd134e54ffe523257705a4c17b2c963e
- `lbc:other/nuke/nuke1` SHA256 c0b358c96058a2af4c2a3170ad89babb904be75b790e97ce624710c0ec6c56cc
- `lbc:other/nuke/nuke1_at` SHA256 2dde48b76859d9aba1dde8269c4c69030b465cfb2bb5a6cddb2b9d1187000810
- `lbc:other/nuke/nuke_eoe` SHA256 5dfb9df138776e00acec97e012b57e321fde609d5647cf13dcc924f61f9f975d
- `lbc:other/sauvojen_tuntija/select_eoe` SHA256 5cfac674767ac8d3b21d756146867215ab0230c0031d6cdf76f053c99adea137
- `lbc:other/shishend/8_1` SHA256 21b8132eb75c3b8f6691bd323eb29dbd3579b64eaf4d58906b3e2e4fb38c2a49
- `lbc:other/shishend/randomize_position/30` SHA256 95c1b204f312eb3849af26fd77bf412e777f72c21f1880cda3dffb234ca4eaef
- `lbc:other/shishend/randomize_position/raycast_electro` SHA256 f7cc07404c441797715d9d3863d38726b7dfd76659d0efbd8121efca1e9becaf
- `lbc:other/shishend/risk_of_rain/as_crate` SHA256 5f2c9d8983c8cf76327ee75344aab3e4897b273fd31a76d1e78c7e9b2babc506
- `lbc:other/shishend/risk_of_rain/run` SHA256 99b035467c5818e40751c20ca5dff42aa7b6de9fc4ec35311248344ed5b43990
- `lbc:other/shishend/risk_of_rain/tick` SHA256 db7a6ff2304d7aeea1d82d064c06c9188027d2c7932a42d279b87dfc5900e12a
- `lbc:other/snus` SHA256 2db8af6e2f525bf9b5caca0517ded16352d473dd3968603a87111f917d0400ed
- `lbc:other/techno/cyborg_boss/on_hit` SHA256 14797d524b2014f72fbcf0ce9c5c53bebcf9d7e552a9bf220f45ccaef7a3e17e
- `lbc:other/techno/cyborg_boss/run` SHA256 f92bc4a64e663e2c741601ba3794799f6c0d01e817eee1497ae7a85090d274b9
- `lbc:other/techno/cyborg_boss/tick` SHA256 94bc22cf5abcec4d349f06563e1fd4229df0707d7c05c72a65244ac1525725c3
- `lbc:other/techno/cyborg_boss/wyvern` SHA256 4f7a05f3308fe22fdbbd84291d3ad96055281c9b13a5a4891e77fc4bde4e42f5
- `lbc:other/techno/supply_crate/land` SHA256 5a5bbc9b4517da3194019ac3207e4731e0aef1bf3b3fdfa0971afaf59af5304a
- `lbc:other/techno/supply_crate/open` SHA256 c26a68c0043ad2b1cdefb726389f1806d3696e9a38104a157609f7bd1617a645
- `lbc:other/techno/supply_crate/run` SHA256 9d091d30785330ac902385002479e89733b58530e5f8d12fa9eab24c92fed3c0
- `lbc:other/techno/supply_crate/tick` SHA256 660c3fdc5c10c6297b84f0d11b711d941a64a04df62825a20b0f9af7191fe22a
- `lbcr:raycast_preloop10_angry` SHA256 3d62f318d8beb073c22fcf4785c8c948f4f5a757d8922bff2f2e00b3594b5e2f

Implicit tag dispatchers additionally matter: mobs_5t_lbc(worm,cyborgpowerhelmet),raycastselector_tuntija->lbcr Ruby/Electro,raycast/select1s/3s/4s andSauvojenemitters,raycasters_1s(nuket,supplytick,cloudtargeting),markers_2t1(TECH/MAGactivation),forest12features,ordinarynativephysicalentity AI/deathloot. Literal reachability alone does not qualify these runtime edges.

## Typed native implementation candidate (not yet built/qualified)

`LbcApocalypseEvents.java` now owns all58/8/11 typed phase actions and exact
per-function-ID REPLACE timers. `kinds(stack)` and `thrown(player,stack)` preserve
all independent flags in source order,sharing one accepted WILD authority.
The native snowball caller owns its debit/removal and TMT exception.
`beginAccepted(Authority,level,position,Kind)` preserves accepted consequences;
scheduled phases recover a fresh native server CommandSourceStack. There is no
function executor/interpreter or stored replacement-globalposition.

All direct native event entities carry explicit author/order/epoch tags AND a
persistent shared LbcSpellSource.Source. Authority reconstruction reads the
actual persisted epoch tag,never the current epoch by assumption. Native TNT80,
lightning,fireballs2/8,invulnerable minibaffled/rider/passenger and actual causal
drop entities use the original payload/physical entity paths. Native temporary
markers preserve the original global pre-tag selector behavior inShish4/9/10.
No creeper helper or fake online author is introduced. `forget` preserves
accepted global consequences; stop cancels pending phases,terrainjobs and live
managed actors; native consumed/removed tokens leave the actor set onreal ticks.

Terrain fills use existing LbcDropBlocks with real native command-size/height
limits,tag predicates and strictness. Source gold48 uses the existing prepared
template slices with native BlockRotProcessor.05 and independent native seeded
processor/placement randoms. Each native terrain invocation uses the existing
Stonekivi work queue and accepted trapChanges cause; unavailable neighborhoods
skip individual operations rather than stalling the event indefinitely or
forcing chunkloads. Bounded queue latency is an explicit project adaptation;
fullphase/terrain timing under load is not qualified.

Shared bridges are parent/core/trap-agent owned: ApocalypseActors worm/Stevari/
ninecloud/fixedRuby50; ShishEncounter RiskOfRain/Cyborg/rawreward consumers;
ChaosTrapActors directforest; LegacyEffects LivingEntity snus; Reactive raw34
adoption into the existing5tick source cooldown/platform; PotionSpawnerDrops
source18potential producer metadata; NukeEvent exactEOE400tickwarning.

The literal inline spawner,Rider passenger andthree64luckyblock payloads were
copied directly from original56/Baffledhelpers into typed native inputs; static
brace validation of SNBT passed. Source phase delay arrays were independently
compared against all77 original schedule lines. This is source/static evidence,
not native branch execution,all341reward behavior or peak-load qualification.

### Actual downstream table closure

Following native `minecraft:loot_table` references plus literal LootTable and
DeathLootTable JSON fields from cyborgboss,fatskeleton,yellow_lightning,
source_of_energy,yameteluckyblock,drops/magic_bean,lucky_items_1 reaches exactly
341 distinct existing original tables and zero missing files. The complete
original filename list is below; packaging alone cannot qualify their item
consumers. Original source functions may add further dynamic/manual runtime
links,so this count is a literal JSON closure,not a catalog completeness claim.

- `lbc:aimbob`
- `lbc:alotofarrow`
- `lbc:anal_whip`
- `lbc:applepig`
- `lbc:artifact`
- `lbc:attributefood`
- `lbc:badapple`
- `lbc:barriers`
- `lbc:barriersnottraps`
- `lbc:big_bob`
- `lbc:bob`
- `lbc:boba_eblan`
- `lbc:buckets`
- `lbc:clocks`
- `lbc:creeper_wand`
- `lbc:crocodildo`
- `lbc:cyberkatana`
- `lbc:cyborgboss`
- `lbc:death_loot`
- `lbc:diamondtools`
- `lbc:dildomet`
- `lbc:divider_by_metal_pipe`
- `lbc:drone`
- `lbc:drops/magic_bean`
- `lbc:electro_shield`
- `lbc:elytra`
- `lbc:end_of_everything`
- `lbc:end_of_technomagic`
- `lbc:enderpearls`
- `lbc:evoker_arrow`
- `lbc:evolution_sword`
- `lbc:explosivepotion`
- `lbc:fatskeleton`
- `lbc:feather_greed`
- `lbc:fevgalea_star`
- `lbc:fireballs`
- `lbc:food`
- `lbc:foodblocks`
- `lbc:fragmentsword`
- `lbc:gaycaster`
- `lbc:god_dice`
- `lbc:golden_skeleton`
- `lbc:greed_greed`
- `lbc:halloween/lucky_items`
- `lbc:halloween/soulhunter_mace`
- `lbc:halloween/transformables_shovel`
- `lbc:halloween/transformables_staff`
- `lbc:halloween/transformables_sword`
- `lbc:hedgehog_wall`
- `lbc:helicopter_boss`
- `lbc:herman_antigravity`
- `lbc:hohlopitek`
- `lbc:honey`
- `lbc:ice_wand`
- `lbc:imba/alotofarrow`
- `lbc:imba/barriers_imba`
- `lbc:imba/barriersnottraps`
- `lbc:imba/bucket_rofl`
- `lbc:imba/buckets`
- `lbc:imba/diamond_area_block`
- `lbc:imba/diamondtools`
- `lbc:imba/elytra`
- `lbc:imba/enderpearls`
- `lbc:imba/fireballs`
- `lbc:imba/food`
- `lbc:imba/foodblocks`
- `lbc:imba/logs`
- `lbc:imba/lucky_items_imba`
- `lbc:imba/potions`
- `lbc:imba/slabs`
- `lbc:imba/spawnegg`
- `lbc:imba/spells`
- `lbc:imba/transformables_spells`
- `lbc:imba/traps`
- `lbc:imba/weapons`
- `lbc:imba/wool`
- `lbc:imitator`
- `lbc:inf_shulker`
- `lbc:inf_steak`
- `lbc:infinite_lucky_block`
- `lbc:infinite_totem`
- `lbc:infinity_no_shulker`
- `lbc:infinity_tuntija`
- `lbc:island_teleporter`
- `lbc:kiuaskivi_arrow`
- `lbc:knifes`
- `lbc:lightarrow`
- `lbc:lightning_bolt_egg`
- `lbc:logs`
- `lbc:loliluckyblock`
- `lbc:lucky_grenades`
- `lbc:lucky_items`
- `lbc:lucky_items_1`
- `lbc:lucky_items_high_quality`
- `lbc:luckyblock`
- `lbc:luckyblock_imba`
- `lbc:luckyblock_loli_block`
- `lbc:luckyblock_neko_block`
- `lbc:luckyblock_without_rec`
- `lbc:lust_death`
- `lbc:madoka_bow`
- `lbc:malevich_rnd`
- `lbc:metal_pipe`
- `lbc:moisei_boots`
- `lbc:muravei`
- `lbc:neko_set_assasin`
- `lbc:neko_set_assasin/boots`
- `lbc:neko_set_assasin/chestplate`
- `lbc:neko_set_assasin/helmet`
- `lbc:neko_set_assasin/leggings`
- `lbc:neko_set_assasin_pre`
- `lbc:neko_set_berserk`
- `lbc:neko_set_berserk/boots`
- `lbc:neko_set_berserk/chestplate`
- `lbc:neko_set_berserk/helmet`
- `lbc:neko_set_berserk/leggings`
- `lbc:neko_set_berserk_pre`
- `lbc:neko_set_chaos`
- `lbc:neko_set_chaos/boots`
- `lbc:neko_set_chaos/chestplate`
- `lbc:neko_set_chaos/helmet`
- `lbc:neko_set_chaos/leggings`
- `lbc:neko_set_chaos_pre`
- `lbc:neko_set_erosion`
- `lbc:neko_set_erosion/boots`
- `lbc:neko_set_erosion/chestplate`
- `lbc:neko_set_erosion/helmet`
- `lbc:neko_set_erosion/leggings`
- `lbc:neko_set_erosion_pre`
- `lbc:neko_set_femboy/boots`
- `lbc:neko_set_femboy/chestplate`
- `lbc:neko_set_femboy/helmet`
- `lbc:neko_set_femboy/leggings`
- `lbc:neko_set_flesh`
- `lbc:neko_set_flesh/boots`
- `lbc:neko_set_flesh/chestplate`
- `lbc:neko_set_flesh/helmet`
- `lbc:neko_set_flesh/leggings`
- `lbc:neko_set_flesh_pre`
- `lbc:neko_set_glass`
- `lbc:neko_set_glass/boots`
- `lbc:neko_set_glass/chestplate`
- `lbc:neko_set_glass/helmet`
- `lbc:neko_set_glass/leggings`
- `lbc:neko_set_glass_pre`
- `lbc:neko_set_mage`
- `lbc:neko_set_mage/boots`
- `lbc:neko_set_mage/chestplate`
- `lbc:neko_set_mage/helmet`
- `lbc:neko_set_mage/leggings`
- `lbc:neko_set_mage_pre`
- `lbc:neko_set_multiplying/boots`
- `lbc:neko_set_multiplying/chestplate`
- `lbc:neko_set_multiplying/helmet`
- `lbc:neko_set_multiplying/leggings`
- `lbc:neko_set_nanobots`
- `lbc:neko_set_nanobots/boots`
- `lbc:neko_set_nanobots/chestplate`
- `lbc:neko_set_nanobots/helmet`
- `lbc:neko_set_nanobots/leggings`
- `lbc:neko_set_nanobots_pre`
- `lbc:neko_set_nyauwu`
- `lbc:neko_set_overload/boots`
- `lbc:neko_set_overload/chestplate`
- `lbc:neko_set_overload/helmet`
- `lbc:neko_set_overload/leggings`
- `lbc:neko_set_paladin`
- `lbc:neko_set_paladin/boots`
- `lbc:neko_set_paladin/chestplate`
- `lbc:neko_set_paladin/helmet`
- `lbc:neko_set_paladin/leggings`
- `lbc:neko_set_paladin_pre`
- `lbc:neko_set_phantom`
- `lbc:neko_set_phantom/boots`
- `lbc:neko_set_phantom/chestplate`
- `lbc:neko_set_phantom/helmet`
- `lbc:neko_set_phantom/leggings`
- `lbc:neko_set_phantom_pre`
- `lbc:neko_set_randchanted/boots`
- `lbc:neko_set_randchanted/chestplate`
- `lbc:neko_set_randchanted/helmet`
- `lbc:neko_set_randchanted/leggings`
- `lbc:neko_set_shitty`
- `lbc:neko_set_shitty/boots`
- `lbc:neko_set_shitty/chestplate`
- `lbc:neko_set_shitty/helmet`
- `lbc:neko_set_shitty/leggings`
- `lbc:neko_set_shitty_pre`
- `lbc:neko_set_shulker`
- `lbc:neko_set_shulker/boots`
- `lbc:neko_set_shulker/chestplate`
- `lbc:neko_set_shulker/helmet`
- `lbc:neko_set_shulker/leggings`
- `lbc:neko_set_shulker_pre`
- `lbc:neko_set_steampunk`
- `lbc:neko_set_steampunk/boots`
- `lbc:neko_set_steampunk/boots_spectral`
- `lbc:neko_set_steampunk/chestplate`
- `lbc:neko_set_steampunk/helmet`
- `lbc:neko_set_steampunk/leggings`
- `lbc:neko_set_steampunk_pre`
- `lbc:neko_set_techno`
- `lbc:neko_set_techno/boots`
- `lbc:neko_set_techno/chestplate`
- `lbc:neko_set_techno/helmet`
- `lbc:neko_set_techno/leggings`
- `lbc:neko_set_techno_full`
- `lbc:neko_set_techno_pre`
- `lbc:neko_set_technomk2`
- `lbc:neko_set_technomk2/boots`
- `lbc:neko_set_technomk2/chestplate`
- `lbc:neko_set_technomk2/helmet`
- `lbc:neko_set_technomk2/leggings`
- `lbc:neko_set_traveller`
- `lbc:neko_set_traveller/boots`
- `lbc:neko_set_traveller/chestplate`
- `lbc:neko_set_traveller/helmet`
- `lbc:neko_set_traveller/leggings`
- `lbc:neko_set_traveller_pre`
- `lbc:nekolucky_items`
- `lbc:neutral_ball`
- `lbc:nexus_item_black`
- `lbc:nexus_item_green`
- `lbc:nexus_item_red`
- `lbc:nexus_item_white`
- `lbc:nexus_wire_black`
- `lbc:nexus_wire_blue`
- `lbc:nexus_wire_red`
- `lbc:nexus_wire_white`
- `lbc:nuke`
- `lbc:one_punch_glove`
- `lbc:penis_cultus`
- `lbc:potions`
- `lbc:purifypotion`
- `lbc:purifypotion_1`
- `lbc:railgun`
- `lbc:random_glack`
- `lbc:random_glack_1`
- `lbc:random_item`
- `lbc:random_sauvojen_wand`
- `lbc:random_sauvojen_wand_tiny`
- `lbc:rasengan`
- `lbc:razorpine_with_leaves`
- `lbc:reactive_shish`
- `lbc:reactive_shish_item`
- `lbc:redstone`
- `lbc:rofls`
- `lbc:sauvojen_tuntija`
- `lbc:sauvojen_tuntija_air`
- `lbc:sauvojen_tuntija_ananta`
- `lbc:sauvojen_tuntija_blind`
- `lbc:sauvojen_tuntija_electro`
- `lbc:sauvojen_tuntija_fotja`
- `lbc:sauvojen_tuntija_swargia`
- `lbc:sauvojen_tuntija_teleport`
- `lbc:sauvojen_tuntija_terra`
- `lbc:sauvojen_tuntija_wand`
- `lbc:shadowglove`
- `lbc:shish_wall_wand`
- `lbc:shishgun`
- `lbc:shock_potion`
- `lbc:siggewinne_syringe`
- `lbc:slabs`
- `lbc:slr`
- `lbc:source_of_energy`
- `lbc:spawnegg`
- `lbc:spells`
- `lbc:spells_library`
- `lbc:stacid_wand`
- `lbc:stalin`
- `lbc:stand_arrow`
- `lbc:stews`
- `lbc:stopper_magic`
- `lbc:stopper_techno`
- `lbc:stoppers`
- `lbc:succubus_potion`
- `lbc:sussy_molten_penis`
- `lbc:swapper`
- `lbc:tiny_skeleton`
- `lbc:tnt_chestplate`
- `lbc:transformables_accessory`
- `lbc:transformables_accessory_all`
- `lbc:transformables_arcanum`
- `lbc:transformables_arcanum_all`
- `lbc:transformables_axe`
- `lbc:transformables_axe_all`
- `lbc:transformables_boots`
- `lbc:transformables_boots_all`
- `lbc:transformables_bow`
- `lbc:transformables_bow_all`
- `lbc:transformables_chestplate`
- `lbc:transformables_helmet`
- `lbc:transformables_hoe`
- `lbc:transformables_hoe_all`
- `lbc:transformables_leggings`
- `lbc:transformables_pickaxe`
- `lbc:transformables_potion`
- `lbc:transformables_shield`
- `lbc:transformables_shooter`
- `lbc:transformables_shooter_all`
- `lbc:transformables_shovel`
- `lbc:transformables_shovel_all`
- `lbc:transformables_spells`
- `lbc:transformables_staff`
- `lbc:transformables_staff_all`
- `lbc:transformables_summons`
- `lbc:transformables_summons_all`
- `lbc:transformables_sword`
- `lbc:transformables_sword_all`
- `lbc:transformables_totem`
- `lbc:transformables_trident`
- `lbc:traps`
- `lbc:triple_golden_sword`
- `lbc:troll_box`
- `lbc:tuntijaa_acid_acid`
- `lbc:tuntijaa_acid_corrupt`
- `lbc:tuntijaa_air_exasperoso`
- `lbc:tuntijaa_air_homing`
- `lbc:tuntijaa_blind_psycho`
- `lbc:tuntijaa_blind_vex`
- `lbc:tuntijaa_bolts_bolts`
- `lbc:tuntijaa_bolts_reforging`
- `lbc:tuntijaa_electro_electro`
- `lbc:tuntijaa_electro_shield`
- `lbc:tuntijaa_fire_blaze`
- `lbc:tuntijaa_fire_kiuas`
- `lbc:tuntijaa_greed_avarice`
- `lbc:tuntijaa_greed_destroy`
- `lbc:tuntijaa_stone_tanner`
- `lbc:tuntijaa_stone_wall`
- `lbc:tuntijaa_terra_awakened`
- `lbc:tuntijaa_terra_razor`
- `lbc:unstable_reactive`
- `lbc:vex_sword`
- `lbc:voidman`
- `lbc:weapons`
- `lbc:witch_gens`
- `lbc:wool`
- `lbc:wtf_item`
- `lbc:yameteluckyblock`
- `lbc:yellow_lightning`
