# Nexus ecology source audit — 3 November 2026

Read-only source audit; no implementation, runtime qualification, build, server operation or git operation. Legacy root L = E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc. Paper root P = E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars. Paths below are relative to those roots. Author instructions in project AGENTS.md and agent_notes/AGENTS.md govern; already confirmed cross-owner sim_cube 0.25% and wire-center 0.5% stops are not reopened.

## Important correction: the live producer is Paper, not spawn_cube

L/function/other/nexus/spawn_cube.mcfunction:1–10 has every tree/center summon and chiseled-deepslate placement commented. Its only active behavior is incrementing global #nexus_id, copying that ID into storage lbc.math nxs, and invoking spawn_cube_save_coords. The latter copies tmpx/tmpy/tmpz into storage lbc:nexus x<ID>/y<ID>/z<ID>. It does not create terrain, trees or centers.

Nevertheless ecology is reachable through the registered Paper command:

- L/function/workingitems/supernovacore.mcfunction:2 invokes other/pidor as every supernova marker.
- other/pidor stores literal clonetonexus in macro storage, probes ~ ~-0.6 ~ against #airs_only, then invokes other/pidor_yep with that storage when grounded. The latter is the macro command `$$(clonetonexus)`.
- P/SigmaWarsMain.java:135 invokes CommandRegistry.register. P/command/CommandRegistry.java:72–73 registers clonetonexus with new NexusCloneCommand(plugin).
- P/command/NexusCloneCommand.java is therefore a registered producer, not merely an unused Java definition. No observed runtime test is claimed here.

The old other/pidor_yep_2 -> pidor_yep_3 -> pidor_clone_rand graph is not reached from current pidor_yep. It formerly selected X/Z inclusive -4800..4800, Y25..1975; cloned a 21-cube to Nexus, then called spawn_cube at destination+10,+9,+10. Do not treat these old bounds or commented nine tree starts as current producer semantics.

## Registered producer semantics

P/command/NexusCloneCommand.java:53–61 SIZE=16/HALF=8, own random, concurrent pending-chunk/task sets. execute:84–118 uses command source location, source chunk key cx,cz (no world in that key), rejects an already pending chunk, and resolves first loaded world whose name contains nexus. Stop cancels tracked tasks and clears bookkeeping.

Destination horizontal grid: bound=5000/16=312; independent uniform dx,dz in [-312,312]; tx=16*dx+8 and tz=16*dz+8. Thus centers span [-4984,5000] in 16-block steps. ty is uniform source minHeight..maxHeight-1. The background copy selects the entire source chunk XZ16x16 and full source world height, pastes at (tx-8,ty-8,tz-8), includes air, then replaces the whole source region with air. This is not the command description's advertised 11-cube.

Main-thread finalizer (roughly lines149–199) transfers source chunk entities except players and markers using tx+(entityX-originX), ty+(entityY-originY), tz+(entityZ-originZ), retaining passengers/vehicle. Loads destination chunk. Creates THREE center markers: independent ax=tx-8+nextInt(16), az likewise; ay=getHighestBlockYAt(ax,az); if block ay-1 is not solid, replaces it with purple concrete. Marker position (ax+.5,ay+.5,az+.5), tags center_cube,nexus_rc. Creates SIXTEEN tree markers on vertical center line tx+.5,tz+.5: y=minY+(maxY-minY)*i/15.0+.5, where maxY=destination maxHeight-1. Tags nexus_tree,nexus_rc. These trees do not start on the pasted terrain's Y.

Then unloads destination and source chunk with save. Separate asynchronous coordinate persistence appends {x:tx+5,y:ty+321,z:tz+5} to plugin nexus_coords.json. This producer is independent of old lbc:nexus storage. Failure/async ordering is source behavior, not a proposed native algorithm.

Native exact text search of src/main/java found no clonetonexus, center_cube, nexus_tree, pidor, NexusClone or cloneToNexus. LbcHedgehogDimensions nexus()/transfer is an existing shared entity transport/RTP2332 service, not this full-height terrain-copy/tree producer. Do not duplicate shared transfer/world construction machinery; this audit does not establish that generic native terrain-copy facilities are absent. Semantic MCP navigation was attempted after initial instructions, but find_symbol returned C:/E: cross-mount/configured-workspace errors; bounded Java reads and exact searches were used instead.

## Dispatch and timelines

L/function/tick.mcfunction:3 runs tick_nexus unconditionally after the normal/stopped-time branch. tick_nexus:4 dispatches every #minecraft:raycasters entity tagged nexus_rc to tick_nexus_raycasters at itself. That dispatcher increments Lifetime then runs tree_tick only for nexus_tree with ANY player within150. Center markers call spawn_blue_wire if their own block is precisely minecraft:air. No MAG/TECH/artifact/owner check occurs on this route. Dormant ordinary mobs/rc routes are not the producer of these actors.

The source tick_nexus also kills matching kill_nexus entities unless nexus_entity/nexus_rc, clears entity effects, sets NoGravity/Silent/NoAI once via tick_nexus_data and marks dated. Its final forceload remove all is legacy infrastructure, not a native lifecycle prescription.

Each eligible tree tick calls tree_tick_move once. Independent uniform direction1..6 sets one of six axis rotations. Independent uniform length1..6. Before movement, a frame in distance0.01..5 kills the actor. Macro fills local ^0 through ^length with lime_concrete replacing AIR ONLY, strict, then teleports length. Then independent chance10 (predicate=0.10) invokes tree_end; afterward independent chance20 (=0.20) summons another nexus_tree,nexus_rc at actor. Do not collapse source command-after-kill sequencing into an invented mutually-exclusive branch without checking command-context behavior. Segment includes start and endpoint; tree length has no fixed lifetime cap. Trees pause movement without nearby players, though dispatcher Lifetime still advances.

On tree_end, create cage only if no center marker within15 and no frame in distance0.01..5. tree_end_pre clears 3x3x3 air strict, selects independent uniform1..4, then creates matching cage. Independently, chance5 (=0.05) invokes tree_end_all, even when cage placement was blocked; finally kills current tree. Thus nominal cleanup draw per eligible movement is 0.10*0.05=0.005, but it is tree-end cleanup, not the separately confirmed wire-center cancellation. tree_end_all clears 3x3x3 at EVERY item_display tagged wire_converter_frame in this dimension (distance0..), and kills nexus_tree within100 of ending tree. It does NOT kill all frames or every tree globally. No player ownership boundary exists in these selectors.

Center when mined to air: spawn_blue_wire places lbc:air21x21x21 at offset -10,-10,-10, emits nexus_wire_blue loot, sets nearest matching blue item glowing, spawns explose_wired,nexus_rc, sound, kills center. This is real source terrain removal, distinct from tree cleanup. Template contents were not decoded in this audit; placement ID and extent implied by name alone are not sufficient proof of its complete palette.

## Cages and dependencies

L/function/other/nexus/tree_end_1..4: aligned block-bottom interaction width1,height1,response1 with nexus_entity and color tag; aligned block-center item display wire_converter_item and cage frame wire_converter_frame, both nexus_entity, interpolation/teleport3, fixed display. Frame model lbc:nexus_item_cage_<red|green|black|white>. Spawn matching nexus_item_<color> loot at center, copy nearest item Item into display, delete that one temporary loot item.

Loot tables are one roll each. Red: Orb Gun100%. Green cage: Nexus Compass100%. Black: Corrupter100%. White: equal weight10 Roadgun/Pulsar, therefore50% each. All five reward tools are already native per current project qualification. These are pre-rolled display rewards, not rerolls on interaction. The green cage's blue-wire cost must not be confused with the deferred green-wire reward graph.

L/advancement/interaction_nexus_frame_<color>.json uses player_interacted_with_entity, excludes spectator, tests matching interaction tag and held custom-data wire. Rewards invoke interacted_<color>. Red consumes one red wire, GREEN CONSUMES ONE BLUE WIRE, black consumes black, white consumes white. Functions clear first, then select nearest matching interaction within10 from the point three blocks ahead of player's eyes; this is not necessarily the actually clicked entity in an overlap. interacted_frame_at spawns a no-gravity item at +.5Y, copies nearest reward display within1, removes temporary tag, kills all item_displays within1, effects, and kills chosen interaction. No suppression predicate appears in advancement/reward. This is a separate route from ordinary spells/rc dispatcher. White-wire crafting and explosion/blue-wire mechanics belong to closure dependencies, not a reason to reimplement existing red/black native consumers.

tick1s.mcfunction:17 invokes tick_1s_player for non-spectators in Nexus. It decrements roadgun/corrupter/pulsar cooldowns, initializes absent compass cooldown but its decrement is commented, and grants cage discovery advancement whenever any cage frame is within5. Discovery requires no interaction or wire.

## Author questions and coherent boundary

Source-backed substantial closure is environmental ecology: live producer hookup, tree actors/terrain, centers/blue-wire removal, pre-rolled four-color cages and exchanges, plus white-wire crafting if not yet native. This is broader than merely uncommenting spawn_cube and narrower than deferred full green/random_glack/TMT. Existing tools, transport, score/resource ownership and rollback services should be reused.

Genuine choices requiring author clarification if not answered elsewhere:

1. Natural trees/cages currently have no owner or suppression gate. Should they remain environmental under MAG/TECH/artifact, including paying a matching wire while suppressed? Source contains no such gate; do not inherit weapon suppression accidentally.
2. Preserve tree_end_all's global cage-area clears plus LOCAL100 tree kill, including foreign branches? Existing author confirmation concerns sim_cube global clear and analogous wire-center stop; it does not by itself answer this differently-shaped cleanup.
3. Which event should instantiate current Paper-style full-height chunk ecology natively, given the old supernova grounding chain and registered clonetonexus override? Native transport alone cannot establish this producer. Do not manufacture an unrelated world-generation population.

No direct damage is in tree/cage functions. Terrain removal can have delayed lethal consequences; source lacks a player owner on environmental markers. Existing mandatory indirect kill attribution contract must be considered for player-triggered producer/center consequences, without inventing ownership for preexisting natural ecology.
