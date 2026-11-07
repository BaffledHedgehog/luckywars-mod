# Four remaining live legacy gadget inputs — source audit, 7 November 2026

Read-only audit; no production code, resources, JVM, server, or compiler used. Source root below is `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data`; paths beginning `function/` or `loot_table/` are under `lbc/`. Searches of `lbcr` JSON/functions found no references to these four keys. This is an implementation plan, not a claim that these owners are ported.

## Author decision — 7 November 2026

The pending launch-policy question is answered: **Gayradar — TECH;
steampunk rocket — TECH; Troll Box — no jammer**. Do not ask it again.
Gayradar's already-applied psychic disorder stays unjammed. These are launch
policies; existing native policies for the outcomes/projectiles/effects remain
with their actual owners. Implement the three complete source owners and real
native input/lifecycle paths; this answer alone does not qualify their release.

## Common real input: immediate snowball RMB detection

`function/load.mcfunction:21` creates the `snowball` used-snowball objective. `players_tick.mcfunction:7` calls `items/snowballs` for a player with a use; `items/snowballs.mcfunction:4` inspects the nearest thrown snowball's `Item.components.minecraft:custom_data{gadget:1}` and calls `projectiles/eggs`. The executor remains the **player at their feet**. Dispatch there is `trollge` line21, `kill_rand` line23, `gayradar` line28, `steampunk_rocket` line34. The final cleanup kills the gadget snowball within10 blocks (except `tmt_item`); `items/snowballs` resets the use objective and assigns IDs/team to other unassigned snowballs.

The vanilla snowball use supplies the single item debit. This is a detection hack, not an impact/flight delay. Native integration should use an immediate consumable native factory with actual private `LbcSpellSource.Source` input and the established TMT one-physical-debit contract; do not launch a placeholder snowball or wait for collision. Source acquisitions are authored loot: `loot_table/loliluckyblock.json`, `kit/steampunk.json`, and `troll_box.json` (the last is the Troll Box snowball itself). Preserve their authored components and weighted acquisition routes.

Global source tick selection parks normal processing during Time Stop and excludes Nexus/Imprinted in selected world loops. Native common role, Time Stop, epoch, clone/rewind and source-context infrastructure must remain authoritative. Per-key absence of a MAG/TECH test is not a new author decision granting immunity.

| Key | Immediate source executor/effect | Consumption / lifetime / guards | Smallest native integration |
| --- | --- | --- | --- |
| `gayradar` | Player → `other/gayradar/click`, `click_rec`, `max`, `click_2`; rank players by `dmg_dealt`, random strict-greater tie winner; if maximum is0, pick caster. Target receives **+1800 skill_issue ticks** and SlownessII180seconds. Caster-position global custom sound. | No extra mana/cost or source MAG/TECH test. Curse is90seconds of active ticks; slow is180seconds. Recursive source selector accidentally admits spectators after the first choice; native protected roles must remain protected. | Reuse `LbcItems` TurnCurse (`adoptRawFoodStatuses`, `tickTurnCurses`, `curseTicks`, clear/checkpoint/clone). Prefer a typed additive entry into that owner, not another rotation timer. Use shared caused status application for Slow. Resolve metric caveat below before choosing the score reader. |
| `kill_rand` | Player → `other/kill_rand_hihihiha`; uniform1..7. Results1..6 kill caster unless Greed or void resistance. Result7 chooses one random other non-spectator non-Greed player, then checks chosen target's void resistance, Nexus and TECH24; no resample. | Source dispatch first rejects caster TECH24. No source MAG test. Result7 awards `lbc:true_advancements/legends/kill_button` even if no eligible kill occurs. No timer or persistent entity. Scratch `owner` tag only excludes caster from choice. | Immediate owner method using shared source/WILD role protection and literal native kill semantics, with inherited cause/credit (`LbcIndirectKills.withCause` as applicable). Do not replace /kill by ordinary high damage or add team filtering. Keep selection-before-target-guards behavior. |
| `steampunk_rocket` | Player → `swrg_kit_integration/items/steampunk_rocket`; spawn **arrow at feet+.01**, copy rotation, ID, dismount player then mount arrow. Tags `rided_steampunk,killedarrow,exploding`, NoGravity, pierce127, damage0, wool-step sound; **no initial Motion**. | Native flight controller needed,40 active ticks maximum. `projectiles/pre` dispatches active tagged arrow to `racer_arrow_steampunk`; source ID identifies nearby rider. Loss of rider within3 enables gravity. No direct MAG/TECH test and no magic/mecha source tag. Ground terminal path `projectiles/prein` spawns fuse0 TNT then kills arrow; age40 simply kills arrow, **no timeout TNT instruction**. | Reuse native Arrow/mount, owned projectile/Time Stop/lifecycle/clone, and shared owned TNT/cause path. Add one typed steering/age controller, not a snowball engine. Preserve source input position, zero initial velocity and separate ground/timeout termination. |
| `trollge` | Player → `other/troll_box/select`; uniform six immediate authored mask grants **into inventory**, not auto-equip; plus six coupled global events detailed below. | No immediate MAG/TECH/cost test. Events persist until match reset (darkness has its repeating schedule). Mask6 worn beam separately requires absence of MAG20. | Reward-only adapter is incomplete. Reuse generated/source item prototypes, shared templates/terrain queues, cage/falling-block owner, dimension authority, native mob/status/raycast owners; add typed persistent match event state with captured source and lifecycle images. |

### Gayradar metric and existing author ruling

Original `load.mcfunction:144` binds `dmg_dealt` to `minecraft.custom:damage_dealt`. Current `LbcWitchStatistics.criterion("dmg_dealt")` deliberately uses DUMMY; `awarded` ignores vanilla DAMAGE_DEALT and native melee tracking owns the mutable Witch score. That is a different metric. Do not silently reuse the Witch counter or replace its approved behavior. Source-compatible radar can read native DAMAGE_DEALT with a match-relative baseline if the intended reset semantics require it; score/reset policy needs reconciliation against the existing author decisions before implementation. A classname/score-name match alone does not prove equivalence.

The current native TurnCurse comment and `AGENTS.md:3360` already record **jammer-immune psychic disorder**. Old `players_tick` MAG/TECH suppression must not be restored for this disorder. No need to ask the author that answered question again. This does not decide the radar's initial admission or other gadgets' missing jammer policies. `AGENTS.md:3353` and `agent_notes/JAMMER_QUESTIONS.md` require resolving missing legacy jammer checks rather than inferring a new exemption/gate.

### Rocket steering details

`racer_arrow_steampunk` rotates as the ID-matching nearby player and calls `raycast_vpered_vect`: read Motion scaled by10000, add the rider-facing local-forward .1 delta, and recursively scale **all three integer components by3/4** while any component exceeds ±100000. This is whole-vector attenuation with integer rounding, not component clamp or a one-time launch vector. Flame particles are30 each active flight tick. Source only copies `lbcID2`, not team_number; native captured creator/cause is still required by the user's shared indirect-kill contract. Source `clowder`/`caster` scratch tags can be typed references, with frozen actors retained rather than discarded.

## Troll Box's six real branches

Each `other/troll_box/{1..6}` grants a distinct full authored leather helmet (`troll:1,trollmask:N,craftusable:1`, `lbc:mask_N` model/equipment asset, Unbreakable, name/lore/tier and exact head attributes). Do not reconstruct just flag/model or assume a generic cosmetic helmet. Branch4 also has Binding Curse. Branches1,2,3,5,6 show global authored subtitles; branch4 invokes each victim's cage title.

| Branch | Global source behavior and duration |
| --- | --- |
| 1 | Set global `trollhunter=1`. `tick1s` → `spawn_trollge_pre`:5% each second to spawn at a random non-spectator, then spread0..10 (special under120 path in Nether). Actual Wither Skeleton at targetY+6, health20/attack8, silent/persistent/no pickups/empty loot, mask1 head/chest/feet presentation and zero drops, SpeedII/InvisibilityII20000000ticks. `mobs_1s` gives each `tp_skel_mask`20% sound chance from nine scream/pitch variants. Search found no extra teleport behavior for that suggestively named tag. |
| 2 | Set global `darkness=1`, tag caster `darkness_player`, clear old `tick90s` schedule, then call it immediately. It schedules return at45seconds and recurrence at90seconds. Modes3/4 affect only tagged casters; other modes affect all non-spectators/non-Greed outside Nexus. Enter Imprinted at same XYZ/rotation; add gravity modifier `imprinted=-1.01 add_multiplied_total`; return Overworld at same XYZ/rotation with portal sound at45seconds. `tick90s_imprinted_in_sched`5seconds later removes that gravity modifier from all players. Source return is Overworld, not saved-origin dimension. |
| 3 | Set global `powerfall=1`. Once/second outside Nexus/Imprinted create `magic,other,movingpower,slowraycast,can_deleted` marker atY310; random X/Z -100..100; lifetime800. `workingitems/falling_trollge` moves down.5 per active tick, dust, cube±3 obsidian fill replacing `#minecraft:solid`; nearby entities<=4 receive Slowness/Weakness/Wither/Blindness5seconds amplifier100. This is a real terrain/status producer with prolonged state, not a helmet passive. |
| 4 | For every non-spectator outside Nexus, `drops/empty_cage`: stone-brick floor±1, iron-bar cage to+3, upward shaft clear (legacy319 and255 passes), two damaging anvils at+40,+42. Existing `LbcCageDrops` ANVIL has shared geometry/actor/cause/lifecycle implementation and approved native upward-shaft bounds. Its current public `spawn(ItemEntity,Kind,ServerPlayer)` expects a dropped-token source; expose a typed shared context entry, **do not create a fake token**. |
| 5 | For every non-spectator outside Nexus, `spawn_walls` places25 `lbc:walls` functional templates at a5×5 grid of offsets -27,-16,-5,6,17 atY-1, integrity.995. Global backrooms sound. Reuse real functional-template placement and fair terrain budgets; this is not a map pool. |
| 6 | Set global `abyss=1`; `players_tick` wraps players atY<=0 to sameXZ,Y321. Separately, Paper's legacy `trollmask_active` tags actual worn `troll:1` headgear; `players_tick:17` absent MAG20 → `trollmasksel`, which checks **head `trollmask:6`** and calls `items/arcanums/eyelaser_pre`. Eye ray starts at eyes, .5step, Distance1..201 (stops first nonair by setting201), .05generic damage per intersecting sample, red particles/occasional creeper sound; no mana debit. Native search found no exact mask/eyelaser reader to claim reused support. |

`end_resets.mcfunction` resets abyss/powerfall/trollhunter; new-game setup also resets. Current native `GameController` only removes similarly named player tags during cleanup: this is not an implementation of the global scores or their producers. Darkness scheduling, dimensions, terrain/markers, mobs, beams, all item grants and suppression require real owner coverage and restore/clone/Time Stop handling before the four-key legacy-gap row can close. Existing generic WILD/protected-role/cause policy is already answered by the user; preserve it rather than faithfully reproducing source spectator-selector accidents or unowned TNT/anvils/statuses.

## Bounded implementation order

1. Immediate common item factory/private-source adapter for all four; implement Kill Rand first (no prolonged state), retain actual debit and rejected-input semantics.
2. Gayradar's typed TurnCurse entry plus explicit damage-metric reconciliation; no duplicate disorder owner.
3. Rocket's single typed physical-arrow controller and owned ground-only TNT terminal path, age/rider/freeze/checkpoint tests.
4. Troll Box requires a coupled typed global event owner, shared ANVIL/template/raycast/status/dimension/mob hooks and authored mask input support. Do not mark supported after only random mask grants. Jammer admission questions absent from the source remain explicit author-policy questions; no new blanket policy was invented in this audit.

## Three missing gadget owners - COMPONENT VERIFIED, NOT INSTALLED (7 November2026)

Native Gayradar, steampunk rocket and Troll Box factories now join TMT and source loot.
Gay TECH, rocket TECH, Troll unjammed; accepted psychic curse remains unjammed.
Shared curse/native damage criterion, actual riding Arrow, existing cage/templates,
canonical lifecycle/checkpoint/clone owners; no function execution or fake reward-only owner.
Internal cf398e67/d3387241 over full002901c5: six native suites pass;17families/3resources.
10469 guarded inputs/282author paths; HUD/fourWIP/Main/libs/RP preserved.
Proof: build/verification/tmt-gadgets-2026-11-07/component-verification.json.
Actual proof covers native/TMT carrot both hands, source metric/additive curse,
TECH no debit/MAG distinction,40active-tick Arrow/TimeStop/resident private replay,
all six parsed mask payloads and actual hunter outcome activation. It does NOT yet
prove Troll outcomes2..6, active hunter/power bodies/terrain/full coordinator replay,
or fully-unloaded rocket export. Keep these outstanding before full release.
One512MiB compile then one1280MiB fixture; normal exits/exact restoration/no locks.
Stopped only two validated idle own Serena JDTs under RAM pressure; editor/Main preserved.
NEW LbcTmtEquipment.java is an excluded uncompiled draft for current native GLIDER,
UNBREAKABLE/DAMAGE_RESISTANT/TOOL composition. Next integrate native component families
with literal typed values and actual native equipment/mining checks. Current content,
not the stale legacy flag list, defines meaningful scope. Full TMT active/unreleased.
