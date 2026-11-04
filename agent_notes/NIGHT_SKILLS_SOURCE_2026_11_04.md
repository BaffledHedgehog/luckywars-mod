# Overnight closure audit: skills 28 / 30 / 31 — 2026-11-04

Read-only audit at requested HEAD 208f234e context. Only this note written; no runtime qualification. Read root AGENTS, agent_notes/AGENTS, WORKFLOW, RESOURCE_PACK and Serena manual. Legacy F = E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function; S = F/swrg_kit_integration/game/skills. RP read-only ru_ru.json verified at RESOURCE_PACK.md path. TMT LAST.

## Recommendation

Best companion is **31 Phoenix**, provided life/death ordering and suppression are resolved. **30 Homing** second, but only as whole shared-direction integration, never arrow-only. **28 Hakurei** third/larger: actual Touhou consumable closure is missing, so existing Danmaku does not make it immediately closed. None is qualified or safely ready to label complete from this audit alone.

## 31 Истинный Феникс

S/on_game_start -> phoenix/gamestart via F/start_player, global skill>=1.
Exact integer base-health B (attribute base get command truncates), initial lives L:
new base H=B+5L-5; lives=H if H>=2 else0. Calls cook_health_11 macro, which SETS base, not modifier/effective max HP.
S/skills_tick_1s -> phoenix/second_check only gamestat=2, reads base integer H and sets lives=H if>=2 else0; no heal or recomputation of conversion.
F/death_inject_before_announce -> phoenix/respawn when legacy!=1, global skill>=1, skill31. Reads integer base, integer divides2, SETS base floor(H/2); sets lives same integer if>=2 else0. cook_health_1 macro only sets base. No local MAG/TECH/artifact gate.

Important exact old caller: swarogskywars/data/swrg/function/core/player/reset_pre first leave_death, then ordinary lives-- except Vanity/Indolence, then decides dead/eliminated from lives, THEN death_inject_before_announce, THEN announces remaining lives. Do not invent clean pre-death virtual charge semantics without recording difference. Legacy death hook also runs without an explicit death predicate in reset_pre, so leaving can reach it after reset; native leaving must never revive eliminated/disconnected participants.

Native GameController.died lines1015+ already owns lives decrement, awaitingRespawn, VIP protection, Indolence, event modifier cleanup, kill rewards and participant role; addRuneLives lines1135+ owns life increases. Phoenix must share these owners and preserve authority through all external lives inputs. Source once-second lives overwrite can discard newly awarded lives; it is NOT a life->HP conversion on every life pickup. Need explicit author ruling if improving that. Base max-health must survive native respawn copy/reset while ordinary match reset restores configured base. No private shadow lives/rewind.

Source presentation:10100 slot3 ID31, gate academy/fire, ranked bypass; repeating_command_block model lbc:phoenix; white nonitalic lbcskill.28, six gray nonitalic lore lines. RP explicitly says1life->5 health, half hearts lost on respawn, config may affect threshold; preserve exact values/formatting.

## 30 Самонаводка

Full source input routes (all matter):
- F/items/bows -> S/bow_shoot global skill>=1; skill30 tags arrows within10 of shooter eyes whose lbcID2 equals nearest nonspectator bow>=1 player's ID, weak_homing_arrow.
- projectiles/pre: tagged arrows70%/tick and NO nonspectator player within15 -> homing_soft. inGround removes weak tag when homing_soft actually runs.
- projectilestick: legacy!=1, NOT #arrows and NOT #minecraft:raycasters, skill toggle=1, ID>=1,10% chance, ANY skill30 player exists, no nonspectator within15 -> homing_soft. This route has NO matching owner-skill check! It may steer other players' projectiles.
- raycastselector: legacy!=1,toggle1,ID>=1,chance50,not cant_be_homing,any skill30 exists,no nonspectator within20 -> raycastselector_player_check_skill.
- markers_2t / raycasters_5t / raycasters_1s repeat50% routing on their real cadences; these three do not repeat selector's cant_be_homing test.
- player_check requires matching owner ID AND skill30, excludes same-team player by nodmg1; homing/tick_markers steers toward nearest #mobs within100, non-spectator, not strict_map_object or exclusions. It changes orientation; homing_soft changes physical velocity.
- homing_soft excludes owner/same-team PLAYERS via temporary nodmg, chooses nearest eligible #mobs within100, adds vector0.5 to existing velocity; raycast_vpered_vect_1 uses10000 fixed-point components and recursive magnitude guard. No target case still executes later command chain using original facing; verify before translating into a no-op.

Native reuse: LbcPlanetoidSkill.DirectionOwner registry already lists wands,electro,homing,duplicating,sauvojen/stars,stand,godHedgehog,voidArcanum,specialTools,danmaku,pendingCombat,consuming,sauvojenBoss,meditation,gaycasterAcademy. Reuse this registry/typed actor owners rather than independent entity scans or another engine. Existing planetoid(owner) semantics pull toward HOLDER, so cannot blindly call it as enemy steering. Generalize target/direction policy at shared owner boundary. Native LbcHoming.beforeFireTick is specifically magic LargeFireball family: uses magic suppression and its own5t target steering, so it is NOT the source30 implementation. Retain body velocity, shooter/reflection owner, stored aim, cached motion, and collision callbacks consistently.

Presentation10100 slot2 ID30 gate academy/exahoming; model lbc:homing_skill, lbcskill.27 two lore lines. RP mentions both projectiles and wand rays, making arrow-only knowingly incomplete.

## 28 Храм Хакурей

Kill route F/items/player_killed_entity -> S/player_killed_entity toggle=1 (dispatcher also legacy check).28 adds danmaku_score_spawn and schedules danmaku_cards_spawn1t. Scheduled function runs score_items_spawn for EACH tagged nonspectator, then removes all tags. Each execution globally selects same-dimension item and XP orb Age0 OR1, no radius, victim or owner filter. score_items_spawn_1 rolls lbc:touhou_item FOUR times at every selected entity +4end_rod,amethyst chime volume.5 pitch2. Multiple killers repeat rolls for same drop; outputs can themselves enter later global scans. No local jammer checks. Native victim-bound reward recommended, but this changes explicit source global collection.

Native LbcCombatRewards already has death-scoped identity set of real ItemEntity drops (scope,track,close) and typed deferred Reward. Extend actual scope with XP orb capture + skill28 roll request, not another collector. Item count vs ENTITY count matters: four rolls per entity, not stack unit or XP value. Current collector does not close XP by itself.

Loot table exists native but no Java touhou_item/touhou_power handlers found. Exact weighted pool: ID1 power800,2 big100,3 full3,4 mana1000,5 life1, nested bomb_item10,6 time orb200,7 bomb fragment100,8 life fragment10,9 faith10,10 money1000; total3234. Preserve table and all nested output lore/models.

Actual F/swrg_kit_integration/items/touhou_items/select called by other/instants/select_old when inventory contains Touhou. It consumes ALL stacks matching each ID:1 +1power/unit,2 +10,3 any=>150; cap150. Modifiers update at10 +1attack,25 +.2 multiplied-baseHP,40 +1.5attack,55 +.2HP,70 +1attack,85 +.2HP,100 +1.5attack,115 +.2HP,130 +1attack,150 +2attack. Full-power title only crossing150.4 +100mana capped max only if belowmax;5 +lives;6 time_orbs, at>=25 subtract25 ONCE and give time_stopper;7 bomb_fragments,>=5 subtract5 ONCE give bomb_item;8 life_fragments>=10 subtract10 ONCE +1life;9 +manaregen/unit;10 +2coins/unit. Threshold conversion is once per select invocation, not while-loop. leave_death removes ten owned modifiers and resets power/fragments/orbs0.

Bomb/time_stop consumables use F/projectiles/eggs: bomb_item -> touhou_items/bomb produces paired fast yellow glass Danmaku shells, Distance300 and owned lbcID/team; time_stopper -> time_stop global11-time-stop setting and original immunity checks. Reuse LbcDanmaku native shell implementation and TimeStop service after checking fast-bomb source geometry, never reproduce gamerule mutation/schedule cancellation as another clock engine. Existing ordinary Danmaku bomb semantics differ (charge-sized lifetime), so not identical automatically.

Presentation10100 slot0 ID28 gate academy/danmaku; model lbc:score_items, lbcskill.25 and13 lore lines. Preserve source carriers, models, white/gray nonitalic formatting. All ten collectible models and bomb/time clock native item inputs must work for ANY picker, not only skill28 owner.

## Questions for root's separate author file

1. Suppression policy31 conversion/halving and30 steering/28 drops+pickup: source has no local suppression. Source-preserving default unjammable where no magic-tagged actor is involved; separate native magic shell suppression remains existing behavior. Ask explicit policy, do not silently inherit LbcHoming's MAG gate.
2.28 preserve global Age0/1 collateral/duplicate rolls, or bind four rolls to actual killed victim's real item/XP entities? Recommend victim-bound existing collector; material correction needs author decision.
3.30 projectilestick leaks steering to projectiles whose owner lacks30 whenever any30 player exists. Preserve or restrict to actual owner? Recommend owner-bound. Arrow owner tagging similarly should use actual shooter, following standing attribution requirement.
4.31 retain source base-health lives overwrite once/sec and after ordinary decrement, or convert subsequently gained lives to+5HP? Recommend exact source formulas/order initially; do not promise Rune/Touhou+life conversion that source lacks.
5.31 threshold H=2..3 yields halfbase1 and zero lives on that death; before hook ordinary branch may already mark respawn. Confirm intended final-death threshold/state. Need actual native fixture from config health/modifier interaction and Vanity/Indolence/VIP priority.

## Concrete implementation / verification boundary

Start31 only after rulings; implement shared GameController life/death transition hooks, one loadout-selected state using native base attribute, no scheduler/interpreter. Test B20/L1=>20; B20/L3=>30; odd21=>10=>5=>2=>1; authoritative elimination, external lives award overwritten exactly if retained, base-vs-effective modifiers, respawn copy, match reset, disconnect, stop, TimeStop and nonempty whole-coordinator replay.

Then30 shared direction policy with same RNG/cadences, native arrows and each existing DirectionOwner family; classify physical10/70 versus ray50 and15/20 proximity gates; test owner/team/strict protection, reflections, cant_be_homing source route discrepancy, no target, native collisions and TimeStop/RNG rewind. Do not register ready before every callback owner covered.

28 requires complete ten-collectible native inventory intake and both bomb/time inputs, actual death/XP scopes, owned attributes/reset and all typed queue/RNG/inventory snapshots. Test 2simultaneous kills, no drops, merged stacks/XP splitting, no unrelated drop pickup, one-shot thresholds with large stack, full inventory, any-picker semantics, kill attribution and rewind midway through accepted roll queue. No broad runtime or physical-client claims from this read-only audit.

