# Balance old-mode runtime source ? 3 November 2026

Historical source audit written before implementation. For current implementation, author decisions and qualification, see [Supernova chain integration](SUPERNOVA_CHAIN_PORT_2026_11_03.md). Readiness verdicts below describe that earlier audit.

Read-only source audit; only this note written. No build/server/git/source edits. Read project and agent_notes instructions, Serena initial instructions, Balance reward audit. Author has now confirmed original old mode, so its earlier question about substituting an ordinary restart is resolved: do not substitute one. TMT stays LAST. Source presence is not runtime qualification.

Roots: L = E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc; S = sibling swarogskywars/data/swrg; M = sibling maps/data/swrg; P = E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java; F = this Fabric project.

## Mode composition

L/function/other/new_game/click.mcfunction sets `legacy lbc.math=1`, not `#gamemode`. This is an overlay on the existing mode. S/loot_table/chests/{default,midle,center}.json select `_0` only for #gamemode=1 and `_lb` only for #gamemode=2; 3/4 are independently bedwars in L tick/death callbacks. No legacy flag condition replaces these chest selectors. Thus original composition is vanilla versus lucky-block event mode PLUS old runtime, not a new numeric gamemode3/4. F/GameConfig has no old-mode field. A typed controller state must own the overlay and reset it at match end; L/end_resets.mcfunction:101-105 clears legacy and all four scheduled loops. Disable source-specific skill/homing/trinket/mana/UI paths where they expressly test legacy (players_tick:33,48; items/swords:66-67; skill tick entrypoints; death_inject_before_announce:1). Do not indiscriminately disable every ordinary callback: most still run.

The old lucky-block producer is L/data parent minecraft/loot_table/blocks/red_mushroom_block.json: all six mushroom faces false + legacy=1 gives ONE `lbc:legacy_luckyblock` roll. Ordinary pool separately uses luck_amount. The complete referenced-table closure of legacy_luckyblock has 25 actual tables and root pool has 144 entries (recursive `minecraft:loot_table` value/name traversal, not model-ID regex). It is not sufficient to switch the normal lucky-block table while retaining normal flags.

## Exact fixed map

M/structure/{-1-6,-1-7,-1-10,-1-11}.nbt were decoded directly with nbtlib. All are48x48x48,110592 block records each. Click places them at(-48,48,-48),(0,48,-48),(-48,48,0),(0,48,0), respectively. This fills X/Z[-48,47],Y[48,95].

| Template | Entities | default markers | center markers | spawn markers | look markers |
|---|---:|---:|---:|---:|---:|
| -1-6 |14|8|0|2|0|
| -1-7 |16|10|2|3|0|
| -1-10 |14|8|2|3|0|
| -1-11 |18|10|2|4|1|

Totals62 entities,36 default,6 center,12 spawn,1 look,0 midle; four saver_marker tags also present. Preserve actual structure entities/block entities, not only terrain. Source has more online-player capacity than the twelve spawn positions only through its staging selection behavior; do not invent additional spawns.

P/me/luckywars/swrg/SwrgNativeService.java:1013 registers swrg:game/chests/fill -> fillChestsNative (12408): refill all three marker types with corresponding swrg tables, ensure chest/trapped chest, clear inventory, populate loot, normalize legacy stack-count attributes. This registered override wins over raw S/function/game/chests/fill.mcfunction. `maploader/clear`, GUI, attribute resets must likewise use registered native owners. Existing Fabric MapManager.load ordinary map manifests and GameController.restartFromRune do not express this fixed overlay transition; reuse their lifecycle/map owners through a typed request, never arbitrary function bridging.

## Four old loops and missing native passive leaves

L/function/other/new_game/legacy_tick*.mcfunction supplies four clocks: every tick,5ticks,20ticks,100ticks. 5t removes5 from all eight old cooldown scores. Hat removes4 and belt2 MORE from seven scores (wiki deliberately excluded); source does not clamp. Every-tick mystical_old=1 types2/4/5/6 call nomagic_old/noarrows_old/notnt_old/nofluid_old; iron reactive_old feet emit cloud and slow falling. Every-second mystical_old=1 types1/3 call noeffects1_old/nohp_old; jumpboost_old feet give jump boost amplifier3 for1s; all online players also clear portals_restricted in an11-cube. Every5s heall_old repeating-command-block inventory gives regeneration1s amplifier2.

Exact Fabric searches find NO reactive_old/jumpboost_old/magichat_old/manabelt_old/heall_old handlers, and NO mystical_old=1 passive handler. Existing LbcReactiveArtifacts handles mystical_old=2; LbcJumpArtifacts=3; LbcIncomingArtifacts=4 and gaara_old. These three do not close old passive artifacts. Magic suppression/TimeStop/observer policy needs author policy where source differs; ?old? alone does not authorize bypassing native admission.

## Eight old wand runtime leaves

All are explicit mainhand SelectedItem routes at L/function/workingitems/spell1_default.mcfunction:60-67; no corresponding old routes in the left-hand dispatcher. Shared items/spells anti-magic artifact admission still applies. Each successful source handler sets its own score1000, costs no mana at that route, and has no marker MAG/TECH guard there. Do not copy historical temporary mana overload logic.

| Flag | Function workingitems/ | cooldown score suffix | blocks while score >= |
|---|---|---|---:|
| evocator_old |evokercast_old|evoker|961|
| bassboost_old |basscast_old|bass|961|
| enderwand_old |endcast_old|ender|761|
| corrupter_old |corrupter_old|corrupt|901|
| lifestealer_old |lifestealer_old|vampire|971|
| foxusing_old |foxuscast_old|foxus|921|
| bloodmagic_old |bloodmagic_old|blood|701|
| badwiki_old |badwiki_old|wiki|941|

Evoker chooses three patterns via shared fangs1/2/3; Ender random spread radius60; Bass four random debuffs radius25; Blood damages enemies and self; Vampire scales self healing by suitable nearby mobs; Foxus uses old multi-effect selector; Wiki81 effect outcomes; Corrupter17 outcomes. Their shared actor/fang/projectile/mob behavior must carry owner attribution and native team/protected-role rules. Exact native searches for all eight flags have no consumer, so ordinary new wand implementations do not automatically handle them.

Ten additional old raycast spell flags come from spells_old: lightning_old,tnts_old,creeperz_old,wither1_old,heal1_old,arrows1_old,lava1_old,anvils_old,skeletons_old,trap2_old. Exact routing is L/function/workingitems/raycast_spells/select.mcfunction -> unstable_spellbook/raycast_loop{16,9,8,11,12,14,13,15,10,empty}_old respectively. These include target/block collision, Distance limits, summons/terrain/debits; they are separate from eight wand routes. Exact Fabric searches for these old spell-specific flags have no consumers (lightning_old is already a DIFFERENT falling-netherite DROP flag). Do not conflate shared flags with readiness of every event context. Mob spell intake must follow actual source selectors and native owning actor lifecycle; a pool ID alone does not establish it.

## Already native old consumers to reuse

Fabric src/main/java/me/skywars2/game: LbcFoodLifecycle inf_old; LbcMinigunCrossbow fast_old; LbcPendingSwords cool_old/fragmentsword_old; LbcHedgehogSpikes smoke_old; old artifact types2/3/4 above; LbcIncomingArtifacts gaara_old. Existing typed OLD drop families: Bee,Corrosion,Creature(old vex/skeleton/bullets/Bob),Creeper,Drone(droppertnt_old),FallingNetherite(lightning_old),Hole,Island(clone_old),Library,OpAnvil(opbook_old output),PotionSpawner,Scare,Slab. Reuse registered item/drop dispatch and saved-state owners; do not emit raw barriers expecting old datapack functions to execute. This is exact consumer evidence, not renewed live qualification of the complete old mode.

Other old pool outputs include vanilla enchantments/tools, containers and shared current flags. Full pool qualification must validate every emitted native/raw carrier and callback; root144 entries cannot be declared ready solely from this list. Missing exact old flags above are definite blockers. Charged bows/Supernova are outside this sub-audit.

## Craft behavior and release gate

No separate oldcraft/craft_old/crafting_old function or recipe selector was found. Existing L/function/players_tick.mcfunction:61 still calls other/craft/inventory_craft_check for craftusable in player crafting slots; that function retains ordinary shared custom combinations. Click clears inventories/perks and #lbcskill but does not disable vanilla crafting or globally replace all custom crafting. Old items lacking craftusable should not gain new recipes implicitly. Source shishgun/supernova inventory recipes are separate prerequisites already audited by parent.

Before exposing Balance: implement and qualify fixed transition + old overlay lifecycle, exact chest/legacy lucky-block production, all eight old wands, ten old ray spells, passive artifacts/equipment/cooldown modifiers, emitted item/drop/mob callbacks, termination/join/death/rewind/TimeStop integration. Preserve mainhand/artifact gate versus marker jammers/mana only after author answers any remaining policy questions; no arbitrary legacy function bridge. This note identifies a finite pool and definite missing handlers; it is not a claim that all144 entries have completed per-entry runtime qualification.
