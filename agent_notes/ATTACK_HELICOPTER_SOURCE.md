# Attack Helicopter — ordinary shooter source audit, 21 October 2026

Read-only source audit; no implementation or runtime verification. `boss_helicopter` and helicopter boss are distinct and excluded. User contract: preserve exact source presentation/keys; native Polymer; reuse existing CPU-efficient services; owner attribution includes indirect kills. Read AGENTS.md, ARCHITECTURE.md, agent_notes/AGENTS.md and RESOURCE_PACK.md. This document is the only written artifact.

Source root `S = E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`; paths below are relative to S. Current RP explicitly supplied for this audit is `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8` (read only; older RESOURCE_PACK.md points at 26.3(1)).

## Mechanics and complete ordinary-specific reachable graph

| Stage | Source path:line | Observed behavior |
|---|---|---|
| Item | loot_table/transformables_shooter.json:301–378 | Weighted 10, quality 8; one carrot_on_a_stick, tooltip `lbc:tier_b`, model `lbc:attack_helicopter`; custom_data transformable/shooter/spellwithcooldown/magicwand/attack_helicopter all 1. |
| Input | function/items/spells.mcfunction:7,13; workingitems/spell1.mcfunction:6; spell1_left.mcfunction:2 | Ordinary magicwand right-click routing, both hands; initiation excluded by own/nearby nomagic, and offhand lefthand gate. No Attack Helicopter sneak/left-click action. |
| Start | function/workingitems/spell1_default.mcfunction:42; spell1_default_left.mcfunction:17 | Shared `coldown25` set to 800 if nonpositive and no stopper marker within 20. No mana, ammo, consumption or durability debit in this branch. Mainhand predicate bug discussed below. |
| Enable | function/items/changed_inventory/sel.mcfunction:67,71,79 | helicopter_active while any hotbar or weapon slot has item; remove otherwise. |
| Pulse | function/tick2t.mcfunction:1,3 → mobs_2t.mcfunction:3 → players_2t.mcfunction:9 → workingitems/attackhelicopter_pre.mcfunction:1–2 | Every 2 ticks, nonspectator, active tag and cooldown >=600. Each currently held copy shoots. Switching away suppresses emissions while countdown continues; returning during window resumes. No stopper/nomagic check in pulse. |
| Countdown | function/tick5t.mcfunction:1–2 → mobs_5t.mcfunction:9 → pre_player_5t.mcfunction:1 → players_5t.mcfunction:5 | Remove 5 every 5 ticks: ~10 seconds active window then ~30 seconds remaining recharge, total ~40 seconds from activation. Boundary phase means do not assert exact pulse count without runtime. |
| Main pulse | function/workingitems/attackhelicopter.mcfunction:1–21 | Two native arrows, eyes, pitch forced zero, lateral ±0.2 and forward 0.5. Tags helicopterbullet/groundbreaker, pickup 2, damage coefficient 0.1, PierceLevel 10. Copy lbcID2/team_number; initial horizontal speed 6 (marker vector), no Y velocity assignment, normal native gravity/drag. Temporary helicopterbullet removed, reikast marker killed. Firework blast sound volume 1/pitch 2. |
| Offhand pulse | function/workingitems/attackhelicopter_left.mcfunction:1–22 | Same with lateral ±0.6; together four arrows per pulse. The only extra action is dual-item advancement when mainhand also matches (line 21). |
| Projectile tick | function/tick_not_timestopped.mcfunction:5 → projectilestick.mcfunction:3 → projectiles/pre.mcfunction:17 → projectiles/prein.mcfunction:4 | Native arrow direct hit; groundbreaker only invokes groundbreak once inGround. Other conditional pre/prein paths require unrelated tags/potion components absent from these summons. |
| Ground impact | function/other/groundbreak.mcfunction:1–4 | `fill ~0.1 ~0.1 ~0.1 ~-0.1 ~-0.1 ~-0.1 air destroy`: floor-coordinate box, 1–8 blocks depending on impact coordinates, drops destroyed blocks; explosion particle and generic explode sound volume 1/pitch 2; kill arrow. No TNT, radial explosion or custom damage call. |
| Lifecycle | function/resets.mcfunction:34; other/purify.mcfunction:14 | Reset/purify sets cooldown zero, ending burst. No ordinary-specific duration/range cap on airborne arrows; native arrow lifecycle applies. Ground impact explicitly removes arrow. |
| Other cooldown mutations | function/other/eatables/kfc_chick/all_up_scores_no_mana.mcfunction:14; workingitems/witch_gens/vanity.mcfunction:80, vanity_respawn.mcfunction:72, envy_tick.mcfunction:29 | KFC increments cooldown; Vanity stores/restores; Envy can lower it. These are shared cooldown interactions, not helicopter-specific abilities. |
| UI | function/ui/cooldown.mcfunction:5–6 | Held item displays coldown25 while positive, otherwise ready; legacy raw score UI. |
| Advancement | advancement/true_advancements/shooter/attack_helicopter.json:1–28; function/workingitems/attackhelicopter_left.mcfunction:21 | Impossible criterion granted during offhand pulse with matching mainhand. Model same item; parent shooter/razorpine, task/toast/chat, visible. |

Native base damage coefficient 0.1 is NOT fixed 0.1 health damage: native arrow speed and vanilla hit logic determine final damage. No source NoGravity, fire or potion effect. Copied IDs/team scores alone do not establish native Owner or full friendly-fire safety.

Paper/Ignite search for attack_helicopter/helicopter/groundbreaker found no ordinary firing override: Paper `plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java:2945–2946,2968` updates active inventory tag; UI `LegacyUiElements.java:209` lists cooldown. Other helicopter matches implement boss and are excluded.

## Suspicious source behavior requiring user clarification

1. Mainhand `spell1_default.mcfunction:42` uses `unless score @s[matching item] coldown25 matches 1..`. A nonmatching filtered selector makes the score test fail, so unrelated default magicwand clicks can also set coldown25=800. Do not reproduce this accidentally; ask whether starting helicopter should require actual item.
2. Both shoot files:3–4 write arrow `UUID set from entity @s` (whole entity, no UUID source path), rather than `Owner set from entity @s UUID`. This is malformed ownership logic, not a design requirement. Native owner and indirect destruction/fire/fall attribution are mandatory in port irrespective of legacy gap.
3. Burst emission continues near stopper/nomagic after allowed initiation; only initial cast is suppressed. Ask whether ongoing bursts should suspend/suppress there. No special suppression test exists in attackhelicopter_pre.
4. Lore says literal `30 ` recharge but total ability cycle is 800 ticks/~40s, of which ~10s shoot and ~30s recharge. Confirm intended same cycle, not 30s total. Dual held copies share cooldown and double emission. Pitch-zero aiming is clearly intentional in both RU lore and source; gravity still applies.
5. Terrain effect is positional 1–8 block destroy box, not guaranteed single hit block and no unbreakable exemption is encoded by fill. Confirm exact tiny cube behavior versus one struck block and desired bedrock/container handling before changing it. Block drops can create owner-attributed indirect hazards in native port.
6. Arrow Rotation writes negate player yaw/pitch while actual Motion is level yaw vector; visuals and flight may disagree initially. Temporary global reikast selector/kill and proximity selectors can cross-talk with concurrent shooters; use direct native references instead, preserving geometry.

## Exact presentation/assets

Preserve loot_table/transformables_shooter.json:316–377 components verbatim: green nonitalic name `attack_helicopter`; five lore rows: aqua nonitalic `type`+`shooter`, aqua nonitalic literal `30 `+`recharge_time`, then white nonitalic `attack_helicopter.lore.1`, `.2`, `.3`. No extra status lore or alternate keys.

RP `assets/lbc/items/attack_helicopter.json:1–6` selects `lbc:item/attack_helicopter`; `assets/lbc/models/item/attack_helicopter.json:1–6` uses minecraft:item/generated and `minecraft:item/attack_helicopter`; existing PNG `assets/minecraft/textures/item/attack_helicopter.png`. Tooltip uses existing `assets/lbc/textures/gui/sprites/tooltip/tier_b_{background,frame}.png` and their mcmeta. No new asset or copied translation is required.

RP languages `assets/minecraft/lang/{en_us,ru_ru,rpr}.json:572–575` contain all four existing item keys; advancement keys at :1207–1208. RU/rpr name: `Боевой Вертолёт`; lore third line explicitly `НЕ РЕГУЛИРУЕТСЯ ПО ВЫСОТЕ! (стреляет только прямо)`. EN name `Attack Helicopter`, third line `Only shoots on a straight line`. Preserve all translations exactly, including RU advancement title trailing spaces (`Двойной вертолёт     `). Read-only source inspection establishes asset presence, not rendered visual equivalence.

For port integration reuse LbcWands input/guards, LbcShooters native arrow tracking, LbcItems lifecycle/epoch, LbcIndirectKills owner and block provenance, existing shared cooldown/UI/target helpers; do not add another projectile engine or world-wide per-arrow scan. Exact native integration is outside this source-only audit.
