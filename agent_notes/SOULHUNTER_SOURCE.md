# Soulhunter mace and ordinary Soulless source audit (25 October 2026)

Read-only source inspection; no runtime qualification. Source root `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/` (paths below relative to it). Scope: ordinary `soulcorrupter` / native registry `skywars2:soulcorruption_wand`, Soulhunter and craft. Academy/metasola is a separate family.

## Ordinary Soulless

`loot_table/halloween/transformables_staff.json:88-227`: carrot_on_a_stick; model `lbc:soulcorruptwand`, tier B; translation `soulcorruption_wand`; custom data `{craftusable:1,transformable:1,staff:1,magicwand:1,soulcorrupter:1}`. This is the actual prerequisite ID; not Vex wind wand.

Live route: `function/players_tick.mcfunction:19` -> `items/spells.mcfunction` -> `workingitems/spell1` / `spell1_left` -> default main/offhand dispatch (`spell1_default:106`, `spell1_default_left:93`) -> `other/event/halloween/soulcorruptwand/cast`. Both hands use existing wand input. General spell admission excludes personal/nearby non-spectator nomagic; branch excludes stopper_magic marker within20 and requires1500 mana (`config:111`). Ordinary cast has no independent cooldown. Eye-position context is explicit for both default hand branches.

`cast:1-7` sends ordinary untagged-upgradable wand to `cast_2`, then subtracts1500; full mage set adds configured manamax bonus and trinket cast hook runs. `cast_2:2` applies to every `#mobs` entity within25 of eye context except spectator tag: includes caster/allies, no same-team/id exclusion or line-of-sight test.

`apply_ghosting`: Vex returns immediately through `effects/ghosting_second_vex` (no five-second ghosting). Every other target gets lucky-effect tick/second tags, +5 seconds `effect_ghosting` (additive, not overwrite), fixed modifier IDs: armor/toughness/attack_damage -100 add_multiplied_total, gravity -0.85 add_multiplied_total, fall_damage_multiplier -1 add_multiplied_total. Repeat modifier insertion uses the same IDs while the timer stacks.

`effects/ghosting_tick`: invisibility duration1 amplifier1 every active tick; airborne sprint adds horizontal forward0.2 (rotation pitch forced0), gust/soul/wool-step feedback. Only exact HurtTime10 spawns a vanilla Vex with `DeathLootTable:lbc:wind_charge`, cloud and dye-use sound. `ghosting_second` decrements1, emits five soul particles and removes all five named attributes exactly when zero; requeues second tag. `effects/tick:29` excludes active Greed. Targeting Vex directly spawns `lbc:halloween/metasola`, sets nearby item NoGravity and Motion[0,-0.06,0], plays Vex charge and cloud then kills the Vex. This ordinary Vex conversion is live despite deferral of Academy creation/deployed metasolas. Need shared effect-second dispatch/cleanup integration and owned indirect deaths per existing user attribution rule; source ghosting/Vex lack causal ownership.

## Soulhunter

`loot_table/halloween/soulhunter_mace.json`: native mace; model `lbc:soulhunter_mace`, tierSS, red name; mainhand damage+10, speed-2.4, fall damage multiplier-1 add_value; Wind BurstII/DensityV; food/consumable unlimited-duration none animation permits using_item advancement. Data `{transformable:1,shovel:1,soulhunter_mace:1,spellwithcooldown:1}`. Preserve native smash behavior.

`players_tick:6` invokes armor on actual jump statistic; `items/armor:15` either hand, pitch80..90 and mace_3<=0 triggers `jump`: mace_3=5; add1.2 vertical preserving x/z; large gust and Breeze burst. Sprint (`players_tick:10`, `items/on_sprint:5`) while airborne, either hand and !unstamined: subtract stamina mace_1 by1, add full look-direction0.2 per tick (pitch retained), gust/soul/wool sound; <=0 sets unstamined and glass-break. Non-sprint grounded recharge +1/tick up to60 irrespective held item; unstamined only clears at60 (`on_not_sprint:2`). No mana/antimagic gate in these mace functions.

`advancement/soulhunter_mace_use.json`: `minecraft:using_item` with mace custom data, excludes nexus/imprinted dimensions, reward `.../use`, immediately revokes itself. Thus maintained use can repeatedly dispatch subject to branch cooldowns, not a one-shot click-only trigger.

CRITICAL source semantics in `use:1-2`: `execute unless score @s[predicate=!lbc:sneak] mace_2 matches 1..` dash; `execute unless score @s[predicate=lbc:sneak] mace_4 matches 1..` splash. A selector that fails its posture predicate makes the score condition false and its UNLESS true. Therefore standing always runs splash (ignoring splash cooldown), while dash runs when mace_2 is ready; sneaking always runs dash (ignoring dash cooldown), while splash runs when mace_4 ready. With both initially ready, both execute on either posture. This is source behavior, not conventional intended RMB/Shift-RMB separation. User ruling required before implementation.

Dash `use_dash`: mace_2=5, replaces velocity with full look direction*3; small gust,100 souls spread3, Phantom flap. Splash `use_splash`: mace_4=20; large gust radius5 excluding near-self viewers,100 souls, zombie door break; enemy/non-self #mobs within10 excluding spectator tag receive15 player_attack attributed to temporary tagged caster. Seven destructive air fills at y0..5 form stepped disk: x/z half-extents (3,10),(5,9),(6,8),(7,7),(8,6),(9,5),(10,3), inclusive relative coordinates. No block immunity filter, no sphere, no downward excavation. Preserve causality for support/void deaths.

`players_1s:44-46` decrements positive mace_2/3/4 each second. `leave_death:448-451` sets stamina60 and three cooldowns0; `resets:152-155` stamina0/cooldowns0. HUD `ui/cooldown:57` either held hand -> `cooldown_soulhunter_mace`: stamina white E197 or red E201 if unstamined; dash E198, jump E199, splash E200 show white positive seconds or green translated ready.

Paper velocity source `plugins/luckywars/src/main/java/me/luckywars/command/MotionBrigadier.java:291` proves `motion set ~ ~1.2 ~` adds1.2 to currentY; caret set replaces; caret add adds. `capVector:384` scales whole vector proportionally if any axis exceeds10. Live registration `command/CommandRegistry.java:78`.

## Craft

Ground dispatcher `function/other/craft_ground:78`: Halloween mace item plus soulcorrupter item at0.001..1 -> `other/craft/soulhunter_mace`: spawns one loot result,100 soul-fire flames/enchant sound; kills whole selected soulcorrupter item entity and whole mace item entity (stack consumption defect). Inventory checker `other/craft/inventory_craft_check:74`: any slots in player.crafting.* containing each ingredient -> `soulhunter_mace_inv`: spawns result and reduces every matching slot0..3 of both ingredients by1. Thus extra copies in multiple slots overconsume. No3x3 branch in these functions. Existing project crafting policy should determine exact two-ingredient one-per-pickup consumption; do not accidentally retain stack/extra-slot destruction.

## Deferred / decisions / size

Decisions requiring author intent: malformed unless-score posture branches above; repeated held-use dispatch; ordinary Vex->metasola is inseparable from exact ordinary wand semantics unless explicitly excluded. Source ghosting is WILD; no inference of ally-safe behavior. Craft overconsumption should follow already established project craft convention.

Academy uses upgradable sneak meditation (sixteen vanilla Vex plus upgrade check) and upgraded two-mode ghosting/metasola generation. Generation refunds1000 then charges1500; modes deploy six distinct pig-backed persistent field types. It is materially larger and intentionally deferred.

Expected ordinary+mace batch: medium; status lifecycle/modifiers+Vex native behavior+owned attribution, native mace/three movement branches+stamina/four-row HUD, destructive terrain+splash guards, two acquisition paths. Full Academy adds a large standalone six-field family. No Java/resources/source-server edits, builds, servers or git performed.

## Metasola reward dependency — expanded audit (25 October 2026)

This section supersedes the earlier deferral of deployed metasolas: all six ordinary Vex rewards belong to this batch; Academy wand modes remain deferred. All paths are below the source root already given.

Acquisition/presentation: `loot_table/halloween/metasola.json` is one pool, one roll, six entries with omitted weights (equal). Each native snowball, tierB, model/name `lbc:metasola_N` / `metasola_N`, data `{transformable:1,metasola:1,type:N,gadget:1}`. Ghost-Vex loot gives an unowned normal item; it does not embed the ghost caster in item data. Ownership begins with whoever later throws it, not whoever created it.

Input is immediate snowball-use replacement, not collision deployment: `players_tick:7` -> `items/snowballs:4` -> `projectiles/eggs:40-42` temporarily tags player thrower, invokes `metasola/throw` as nearest matching snowball, then removes tag; final eggs command kills nearest gadget snowball within10. `throw` creates one matching display at snowball position with translation y0.6, scale1, brightness15 and matching type model. Copies lbcID2/team_number from nearest non-spectator tagged thrower; gives unique lbcID to display and matching pig. Display is magic/player/slowraycast/metasola; types1/2 additionally rc_1sec. No mana or local antimagic admission in this branch.

Physical base: `metasola/throw` summons invisible/silent vulnerable pig, gravity enabled, movement_speed -100000, fall_damage_multiplier0, empty loot. MaxHP per type is25/50/20/15/500/40. No Health NBT is supplied. Earlier audit inference of10 current HP is withdrawn: core-agent inspection of native26.3 LivingEntity loading found attributes load before absent Health defaults to getMaxHealth, so target native summon starts full25/50/20/15/500/40 HP. Legacy1.21.4 LivingEntity was not available as loose source in this audit; no source-server spawn was tested. Preserve configured full maxima unless separate legacy evidence establishes otherwise. Copies each snowball Motion component with integer scale2000 then double0.001: velocity is approximately snowball velocity*2, truncated to0.001. Display follows matching pig each active tick at pig position and yaw with pitch0. Pig retains native physics and AI (NoAI is absent).

Lifetime is a warmup counter, NOT an expiry: no data.lifetime is stored on displays, and functions do not kill them after a duration. `raycastselector:1` increments1 per active tick; `raycastselector_player:52` calls `metasola/tick`. That function first checks pig existence and non-spectator owner existence, otherwise `break` kills paired pig/display with glass particles/sound. Once threshold reached the behavior repeats forever until host/owner disappearance or external removal. Types1/2 also get `raycasters_1s:1` +20 per second through independently scheduled `tick1s:3`; this counts twice under ordinary time. Their warmups depend on global second phase and are about5s/2s for thresholds200/80; the remaining warmups are ordinary active ticks below.

| Type | Warmup / cadence | Exact behavior after warmup |
| --- | --- | --- |
| 1 | Lifetime200, action each global second | 4 magic damage to #mobs within10, !spectator,!same_id,!same_team,!metasola_base; native owner player attribution. Ravager sound, sweep; idle damage-indicator particle each tick. |
| 2 | Lifetime80, action each global second | Native Instant Health amplifier2 to friendly #mobs within10,!spectator,!metasola_base. Runs same_team then same_id separately, so an owner who also matches team is healed twice, including native undead inversion. Heart/pickup effects; idle hearts each tick. |
| 3 | Lifetime100, every active tick | WILD #mobs within20,!spectator,!metasola_base; face an arbitrary nearest-selector-order matching type3 display within20 from each victim, force pitch0; add local (left0.074,up0.08,forward0.08), not a pure inward vector. Multiple fields can redirect toward another field; no owner/team exclusion. Snowflake/gust/soul/wool. |
| 4 | Lifetime40, every active tick | For players within10 with mana<=500, same_team and same_id branches set mana500 (no addition, no effect above500). No spectator filter. Witch particle. |
| 5 | Lifetime200, every active tick | WILD20 in_fire damage to #mobs distance0.01..10 (no spectator/team/metasola_base exclusions); no source owner attribution. Move own pig down0.2. Repeated source template placement described below; lava/extinguish effects. |
| 6 | Lifetime100, every active tick | Friendly #mobs within10,!spectator,!metasola_base, same_team then same_id: Resistance duration1 amplifier4 and ensure void_resistance at least2. Duplicate friendly-owner application is idempotent. Iron-block particles/beacon sound. |

Exact source functions: `other/event/halloween/soulcorruptwand/metasola/{throw,tick,tick_1s_select,break,1_tick_1s,1_tick_1s1,2_tick_1s,2_tick_1s_at,3_tick,3_tick_at,4_tick,5_tick,5_tick1,6_tick,6_tick_1s_at}.mcfunction`.

Type5 structural mask: `structure/light_sphere_21x21.nbt` was parsed read-only with nbtlib. Despite its name, palette is fire(age0, all sides false), lava(level0), void_air; 21x21x21,4945 stored blocks:2229 fire,739 lava,1977 void_air; no entities, all axes bounds0..20, center[10,10,10] lava. `5_tick1` places at current display minus10 each axis, no rotation/mirror, integrity0.1, fresh uniform seed0..2147483646 each active tick, strict. Thus it probabilistically carves void_air and places lava/fire across this exact sparse mask; omitted cells stay untouched. It is not a light-only sphere, all-air sphere, ordinary explosion or full cube. Requires owned terrain/fire/lava consequence attribution under the existing user rule.

Guards/cadence: `tick.mcfunction` chooses `tick_not_timestopped` vs `time_stopped_tick`; slowraycast only runs in the former (`tick_not_timestopped:3`), so ordinary display follow and types3..6 behavior pause during Time Stop. However independently scheduled `tick1s` does NOT test time_stopped: types1/2 keep +20 aging AND periodic damage/heal during Time Stop. The native entity-freeze path is `time_stopped_tickentity` and its data-save logic; do not infer it suppresses that independent command damage/heal. All dispatch excludes nexus/imprinted. Global stopper_magic destroys magic entities within20 via `other/stopper_magic:5` -> `stopper_magic_destroy` (witch particle, kill display); nomagic kills magic within25 (`other/artifact/nomagic:7`). Pig lacks magic tag, so these direct display kills can leave an invisible base pig orphaned. No bespoke Greed/protected-role/antimagic immunity checks exist inside metasola actions; use shared target guards and active Greed ruling in new native implementation. Native damage/effect rules (fire resistance, undead inversion etc.) remain relevant.

Questions needing author intent beyond previously recorded mace ruling: doubled age for1/2 and their Time Stop continuation; type2 owner double healing; type3 choice of any nearby type3 field; persistent fields with no expiry; type5 actual fire/lava/void-air mask and WILD damage. Source defects such as orphan pigs, missing type5 ownership and source role omissions require integration with existing project lifecycle/attribution rules, not preservation as leaks.

With all six rewards included this batch is large: vulnerable native hosts, movement/field lifecycle and six persistent actions, exact fractional destructive template, status/void support, ownership and cleanup, plus ordinary ghosting and Soulhunter. Academy wand activation remains independently deferrable.



## Integrated qualification (25 October 2026)

Build `fa036823` passed86 recorded results in two actual mode1 matches (151.33s),
including all14 Soulhunter-family cases twice, the existing shared regressions,
real match-stop probes and cleanup. Exact316 source files/881 classes/2812
resources match the tested ready JAR. No new ERROR lines beyond the existing
fake-bot Polymer packet-context pair. Evidence: `build/verification/soulhunter-final/`.
Main installation was refused while its session.lock was held. The user then
requested leaving this build ready, so the main JAR remains `2558af6b`; no restart
or replacement occurred. This supersedes individual task-agent pending statuses.
Native fixture checks include explicit tick boundaries, not real client pixels,
actual disk restart/disconnect or peak-capacity qualification.

Source-audit resolution: both malformed mace branches are separated by explicit
user ruling; full host HP is explicit user ruling. User chose the more efficient
vortex behavior: per-instance center, no second nearby-display scan. Doubled
1/2 age, duplicate self-heal, persistent fields and the actual WILD type5 mask
are retained. Shared Time Stop pauses the entire effect, replacing the old
independent-second loophole; native paired cleanup and ownership replace source
orphan/attribution defects. These resolutions supersede the initial open-question
list; Academy activation remains deferred. Per-field second phase starts at
native deployment, rather than inheriting the legacy global scheduler phase.
