# Shared food status consumers - 29 October 2026

Status: staged native consumers, not qualified or released. Full original Chaos dependency gate remains active. This assignment owns only new `LbcFoodStatuses.java` and this note; existing helpers/integration are root-owned. No build, server, resource, RP, commit or protected-path changes by this agent.

Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

## Actual parents and timers

Read full source `players_tick.mcfunction`, `players_1s.mcfunction`, `mobs_tick.mcfunction`, `mobs_1s.mcfunction`, `tick.mcfunction`, `tick_not_timestopped.mcfunction`, `tick1s.mcfunction`, the three status helper families and transitive teleport helpers below. `load.mcfunction` creates the three dummy objectives; `leave_death.mcfunction` zeros all three. `other/purify.mcfunction` zeros all three; `resets.mcfunction` zeros shifting. Producers include human_eye (+12000eye), golden_pie (+200legend), transdimensional_fish (+5760shifting), multiverse_fish (+11520shifting), netherite_stew (sets all three INT_MAX), KFC's actual shared-score reward (+1each) and crafted_mana eye rewards. These producers retain their current native owner; this consumer does not add another award, nutrition, debit or timer.

`tick_not_timestopped -> mobs_tick -> players_tick` runs outside Nexus/Imprinted and for nonspectator, non-time_stopped players. Each tick positive eye/legend objectives invoke their helpers only without a `stopper_magic` marker within20. MAG therefore suppresses their work **and pauses their own score drain**; TECH does not suppress their main helpers. `tick1s -> mobs_1s -> players_1s` invokes positive shifting once per second, with no source MAG or TECH marker condition. No role, ally or damage target check occurs in the source selectors beyond nonspectator and the dimension/parent conditions.

Native integration `new LbcFoodStatuses(game)`: public `tick()` from shared item tick; it visits active ordinary-dimension participants, with current project role/Time Stop admission. `lastTick` is only an actual-server-tick duplicate-call guard. Global second boundary is the established `server.getTickCount()%20==0`; there is no private status age/remaining-duration map. All three remaining lifetimes live solely in their original scoreboard objectives. `clear(player)`/`forget(player)` zero these three source scores on death/leave; `stop()` clears connected players and the duplicate-call guard. Temporary suppression does not call clear. Status handling continues in ordinary Nether/End after a real transition and does not require staying in the map's Overworld.

### Pending source/policy question retained for root

Literal legacy `tick1s` is independently scheduled and does not inspect global/personal Time Stop before shifting; literal shifting can drain/roll during Time Stop while ordinary eye/legend use the stopped tick parent (with an unstopped-player special route). Root explicitly selected **current shared Time Stop pause for all three**, instructed this discrepancy to remain pending for the author, and will copy it into the central pending list. Native code consequently pauses work and score drain for all three during shared global/dimension/input Time Stop. No separate legacy clock engine or silent claim of exact unpaused shifting is made.

## All Seeing Eye

Read `other/effect_allseeingeye.mcfunction`, `other/effect_allseeingeye_shaped_player.mcfunction`, `armor/powerhelmet_particle_2.mcfunction`.

- Every loaded nonplayer entity in original `#minecraft:mobs` at distance>=.01 receives one private forced `entity_effect` dot at Y+.5: color floats `[0,.007,0,1]`, count0, zero offsets, speed1. Count0 retains the original native directional-particle packet semantics.
- Every same-dimension nonspectator player at distance>=.01 receives20 private forced points at Y0,.1,.2,...,1.9. Source float color `[0,.01,0,1]`, count0, zero offsets, speed1. No fabricated client outline/glow entity, visibility packet, team reveal, radius limit, ally exclusion or fake player is used.
- Every nonspectator player at distance>=50 additionally selects a direction from the **original wearer position plus Y5** toward that player's eyes. The TECH marker check is at this raised execution origin, not at the remote target. Source `powerhelmet_particle_2` emits eight normal green points at forward offsets .1,.5,.9,1.3,1.7,2.1,2.6,3.2; count1,speed0,zero offsets. Its audience is actual nearest nonspectator player to that raised origin (`@p`), which need not always be the wearer. Normal private native particle delivery still owns rendering distance.
- Clears darkness,blindness,nausea; grants existing true advancement `lbc:true_advancements/potions/effect_all_seeing_eye`; subtracts exactly1eye score.

Native color packing uses `ARGB.colorFromFloat` with the original float arguments, retaining native 8-bit conversion rather than guessed hexadecimal constants. Existing `LbcTechnoArmor.helmet` was inspected: it emits single mob/player dots with different colors and target TECH behavior, so substituting it would lose the source20-point silhouette. Its `directions` helper also differs in flame particle,Y3,target distance. Root extracted a narrow shared eight-offset particle primitive for reuse; this status owns only its unique source selection and silhouette.

## Living Legend

Read `other/effect_living_legend.mcfunction`. Every admitted positive tick draws native world RNG uniform1..16 and publishes actual `#random16 lbc.math`. Exact order: white,red,black,blue,light_blue,magenta,purple,yellow,pink,brown,orange,green,lime,cyan,gray,light_gray stained glass.

One strict inclusive7x7 layer at wearer-relative Y-1 replaces only **the actual custom `#minecraft:airs` tag**. This tag includes water, plants, signs, carpets, some block entities and other authored replaceable blocks; `isAir()` or a generic radius substitute is wrong. Relative coordinates floor the source position using native BlockPos. Existing `LbcDropBlocks` filtered fill owns native volume limits, BlockInput placement and strict update flags. Writes retain native WILD causal credit through `indirectKills.trapChanges(level,wildCause(player),writes)`, with no self/ally terrain exclusion. There is no scheduled platform expiry or restoration in the source; existing round/map restoration owns cleanup.

Then sends normal dust_color_transition to same-world audience at wearerY+2, offsets2,2,2,speed40,count40,scale4. Source from_color is `[1,1,1]`, **to_color is `[10,10,10]`**. Native `DustColorTransitionOptions` codec uses `ExtraCodecs.RGB_COLOR_CODEC`, which converts these floats with `ARGB.colorFromFloat`; the implementation invokes that exact conversion, without an invented normalized-color correction. Finally subtracts1legend score and grants `lbc:true_advancements/potions/effect_living_legend`. No effects, heals, sounds, armor layers or new physics appear in this function.

## Dimensional Shifting

Read `other/effect_shifting.mcfunction`, predicate `chance5.json` (actual.05), `projectiles/teleport_nahui.mcfunction`, `_ne/_oe/_on/_im`, `tp_nexus_rtp.mcfunction`, `tick_nexus_player_tp_3.mcfunction`, and existing audit `agent_notes/lbc_projectiles.md:130`.

Every admitted source second makes exactly one native world RNG chance draw. On success, draws one uniform1..4 and publishes source `rand2 lbc.math`, then executes the shared native dimension dispatcher. Whether chance fails or a destination transition fails, subtracts1shifting score and grants `lbc:true_advancements/potions/effect_shifting`. No MAG/TECH gate is invented for this status.

Direct helper destinations: Overworld -> Nether/End/Nexus/Imprinted; Nether -> Overworld/End/Nexus/Imprinted; End -> Overworld/Nether/Nexus/Imprinted; Imprinted -> Nether/End/Nexus/Overworld. No Nexus handler exists. **The top dispatcher checks current executor dimension sequentially Overworld,Nether,End,Imprinted, without return.** Successful earlier transfers can therefore trigger later checks. Existing source audit already records this; one direct helper call is insufficient for the food status. Final ordinary success outcomes:

| Start | roll1 | roll2 | roll3 | roll4 |
|---|---|---|---|---|
| Overworld | Overworld via Nether | Nether via End | Nexus | Overworld via Imprinted |
| Nether | Overworld | Nether via End | Nexus | Overworld via Imprinted |
| End | Overworld | Nether | Nexus | Overworld via Imprinted |
| Imprinted | Nether | End | Nexus | Overworld |

Native `execute in` scales X/Z by dimension coordinate scales, keeps source Y/look; no safety relocation or portal spawn is authorized. The source context position stays the original one across nested helpers; repeated scale composition must preserve that reference. Nexus draws world RNG X/Z independently in inclusive-4800..4800, sets Y2332, then removes only `minecraft:antigravity_effect` gravity modifier. Existing `LbcHedgehogDimensions` owns actual native transitions, destination chunk acquisition, shared causal attribution and gravity cleanup. Root is responsible for the shared sequential dispatcher bridge and world-RNG overload; no mcfunction runtime is used here. Current universal target exclusions/Greed policy stay owned by the shared transition engine.

`LbcAcidCorruption` was inspected before considering new dimension code: despite its name, it has no native dimension transition bridge; its dust/projectile mechanics are unrelated. `LbcDimensionItems` is the ordinary/Nexus/Imprinted admission helper, not a teleport implementation. Reusing the real Hedgehog transition engine avoids a second dimension/velocity/attribution implementation.

## Exact root-owned shared bridges

1. `LbcDropBlocks.fill(ServerLevel, BlockPos, BlockPos, Block, boolean, Predicate<BlockState>)`: existing native fill with the original source replacement predicate.
2. `LbcTechnoArmor.directionParticles(ServerPlayer viewer, Vec3 origin, Vec3 direction, ParticleOptions particle)`: existing eight exact direction offsets, normal native private particles.
3. `LbcHedgehogDimensions.teleport(LivingEntity, ServerPlayer owner, int roll, RandomSource)`: direct helper transition with explicit world RNG for Nexus coordinates. Existing overloads retain ordinary wearer RNG behavior.
4. Root-owned sequential dispatcher using the same engine is necessary for `teleport_nahui` top-level calls; the requested explicit contract is `dispatch(LivingEntity, ServerPlayer owner, int roll, RandomSource)`.

Root confirmed implementation of bridges1..3. The status currently implements only the four sequential source checks locally and delegates each actual transfer to bridge3, so it preserves the top dispatcher without waiting for an extra bridge or copying dimension physics. Bridge4 remains an optional root extraction for other top-dispatch consumers; it is not a missing runtime dependency of this class. The top-dispatch finding and exact table were sent before freeze. Integration calls `tick()` after shared stop admission and `forget`/`stop` with existing lifecycle. No new consumable payload/resource producer is owned by this class. Actual native fixture qualification and central author resolution of the source Time Stop discrepancy remain outstanding.
