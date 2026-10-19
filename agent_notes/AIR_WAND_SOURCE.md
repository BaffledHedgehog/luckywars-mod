# Air wand (Stair) legacy source notes

Source tree: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data` (read-only). Paths below are relative to `data/`. This records source behavior; no runtime test was performed.

## Уточнения переноса (user)

> да реальное попадание далее 80 блоков, попадание значит цель задело взрывом (не обязательно прямое попадание)
>
> Умное удаление вместо фиксированного срока

Описание 200 тиков и Lifetime >=80 ниже относится только к старому исходнику.
В Fabric все воздушные ракеты используют общий разрушительный smart lifetime,
а испытание — фактическое положительное попадание взрыва дальше 80 блоков
от точки запуска. Отсутствие прямого попадания ракеты не мешает зачёту.
Текущий перенос: [AIR_WAND_PORT.md](AIR_WAND_PORT.md).

## Ordinary cast

`lbc/function/workingitems/spell1_default.mcfunction` dispatches main-hand Stair when mana is at least the configured cost and no `stopper_magic` marker is within 20; the offhand counterpart is in `spell1_default_left.mcfunction`. It calls `workingitems/sauvojen_staffs/air.mcfunction`, which spawns one marker via `air_preloop.mcfunction`, then deducts `stair_wand_mana_use=1000` (set in `lbc/function/config.mcfunction`). The marker has `{data:{lifetime:200},Tags:["raycasttnt","player","slowraycast","magic"]}`, inherits caster rotation, `lbcID2`, and `team_number`, and is marked `actived`.

`lbc/function/tick_not_timestopped.mcfunction` dispatches `#minecraft:raycasters` markers through `lbc:raycastselector`; `raycastselector.mcfunction` increments `Lifetime` once per tick, then `raycastselector_player.mcfunction` dispatches `raycasttnt` to `lbcr:raycast_tnt`. Ordinary speed is 1 block/tick, up to marker lifetime 200 ticks absent earlier termination. `lbcr:raycast_tnt.mcfunction` moves 1 block/tick and checks for mobs within 10 and non-air block collision. `explodewithid.mcfunction` calls `dmgwithid_un_playeronly` to tag owner/team mobs as damage-exempt, then ends the projectile; `raycast_tnt_kill.mcfunction` summons one TNT with fuse 2 ticks. These functions do not issue a direct damage command or attach a TNT owner UUID, so kill credit is not evidenced by this source. The generic TNT blast is therefore the damage/effect source; this path does not summon a firework rocket. Numerical damage depends on vanilla TNT explosion context. As required by the new-project source notes, treat absent owner attribution in this legacy source as a migration gap, not desired behavior. Fast terminal kill summons three TNT entities at Y offsets +1/0/-1, each `{fuse:2}`; homing uses ordinary one-TNT terminal kill with `{fuse:2}`.

## Academy upgrade and shared challenge flow

The Academy advancement invokes `lbc:other/magic_academy/interacted` (advancement `interacted_with_academy`). If held item is already upgraded it reports that; if `upgrade_available:1`, it goes to `interacted_upgrade`; if merely `upgradable:1`, it reports root/no conditions; otherwise it goes to `interacted_2`, which shows `can_upgrade/stair` for `{stair:1}`. That Stair function only sends `upgrade_condition_stair` text. The English text says to hit a target more than 80 blocks away; RU says the same.

The actual challenge hook is in `lbcr/function/raycast_tnt_kill.mcfunction`: only at projectile `Lifetime >= 80`, with a mob within 5 blocks at kill position, it finds a non-spectator player holding an upgradable Stair whose `lbcID2` matches the marker and calls `lbc:other/magic_academy/can_upgrade`. The generic `lbc:other/magic_academy/can_upgrade` calls `can_upgrade_book` (except unstable spellbook): it announces availability, sets `upgrade_available:1`, and clears `upgradable`. The following Academy interaction enters `interacted_upgrade`. That shared handler first writes `upgraded:1, mode:1`, modifies the item description, increments `challenge_score` only if player objective `lbc.challenge=1`, and runs generic completion/reset at 20. It then invokes `upgrade/stair`, which reports success and tiers the item. Thus the >80 trial grants availability; it does not itself perform the upgrade. Source check is Lifetime >=80 and hit proximity <=5, not a directly measured Euclidean range. No stair-specific challenge counter exists. Advancement `true_advancements/legends/academy/air.json` separately rewards inventory possession of `{upgraded:1,transformable:1,staff:1,stair:1}` and inherits from the Academy electro advancement.

## Upgraded modes

`air_upgraded_pre.mcfunction` and offhand `_left` handle sneak-click cycling through shared `change_mode_3` / `_left`; the subsequent non-sneak cast reads the held item’s `custom_data.mode` and selects:

- Mode 1, burst: exactly 4 markers, each `{lifetime:200}`, spread by `lbc:universal_razbros`; 1 block/tick like ordinary. The helper appears to randomize each projectile direction.
- Mode 2, fast rocket: 1 marker with lifetime 200. Within one `raycastselector_player` call, `raycast_tnt_fast` increments Distance and recursively advances 0.7 for Distance 1..9; at Distance 10 it calls `raycast_end_with_tp`, which resets Distance to 0 without killing the marker. It advances 9×0.7 = 6.3 blocks per dispatcher tick, continuing until impact or its 200-tick lifetime ends. It has no native projectile velocity.
- Mode 3, homing: 1 marker with lifetime 200. Within one selector call, `raycast_tnt_homing` increments Distance and recursively advances 0.4 for Distance 1..4, reaching Distance 5 and calling `raycast_end_with_tp_homing`, which resets Distance to 0 without killing the marker. It advances 4×0.4 = 1.6 blocks per dispatcher tick, turning toward the nearest eligible mob within 10 on each cycle, until impact or its 200-tick lifetime ends.

All three upgraded modes deduct the same 1000 mana in `air_upgraded_upgraded.mcfunction`. Sneak cycle labels use existing translation keys `mode_burst`, `mode_fast_rocket`, and `mode_homing`. Mode handling is duplicated in `flying_wand_tick_air_upgraded.mcfunction` for flying-wand use and selects mode 0 ordinary plus modes 1–3; ordinary projectile also has lifetime 200.

## Actual entity/effect data and caveats

The principal air wand creates marker entities, not `minecraft:firework_rocket`. Its terminal blast uses TNT summons and ID-aware generic damage, so do not claim firework explosion effects or a numeric firework damage value for this path. A distinct legacy `lbc/function/workingitems/sauvojen_staffs_tiny/air_end.mcfunction` does summon a firework: `{Life:0,LifeTime:0,ShotAtAngle:1b}`, one `burst` explosion, colors `[16770831]`, fade colors `[12422150]`, `has_trail:1b`, `has_twinkle:1b`; it does not document damage. This tiny-staff variant is separate from the ordinary/upgraded Stair implementation above.

Source questions for root (please decide intended fixes before porting):

1. `air_preloop_burst.mcfunction` obtains all four rotations from `@a[tag=caster,limit=1]` instead of `@s`. This can select an unrelated/stale caster or fail to find one; is the intended spread centered on the current cast direction?
2. A separate `raycastair` branch in `raycastselector_tuntija.mcfunction` uses `lbcr:raycast_air`; it recursively checks for any mob within 15 (`tag=!spectator,tag=!sauvojen_tuntija`) and calls `raycast_air_explode`, whereas `raycast_airplayer` uses owner/team predicates. This is not the primary Stair marker path (`raycasttnt`), but appears to be the same wind theme. Confirm whether the broad trigger and distinct friend filtering are intentional before consolidating behavior.
3. `dmgwithid_un_playeronly.mcfunction` applies temporary `nodamage`/`nodamage1` tags to same-ID/team targets. `raycast_tnt_kill.mcfunction` then removes those tags globally from all entities, so overlapping explosions may interfere. The TNT also has no explicit owner attribution; the legacy source cannot confirm credited kills.
4. Text says target "more than 80 blocks" while the challenge tests projectile `Lifetime >=80`, with one update per tick and a separate <=5 hit radius. Is the lifetime proxy and inclusive threshold the intended challenge definition?
5. Fast and homing recursion resets Distance at 10 / 5 inside each selector tick; derived travel is 6.3 / 1.6 blocks per tick. Verify user-facing range claims in game.

## Primary source files

- `lbc/function/workingitems/sauvojen_staffs/air*.mcfunction`
- `lbc/function/workingitems/spell1_default*.mcfunction`, `spell1_upgraded*.mcfunction`
- `lbc/function/items/wands/sauvojen_wand/flying_wand_tick_air*.mcfunction`
- `lbc/function/raycastselector*.mcfunction`, `tick_not_timestopped.mcfunction`
- `lbcr/function/raycast_tnt*.mcfunction`, `raycast_tnt_kill*.mcfunction`, `raycast_end_with_tp*.mcfunction`, `explodewithid*.mcfunction`, `dmgwithid_un_playeronly.mcfunction`
- `lbc/function/other/magic_academy/interacted*.mcfunction`, `can_upgrade/stair.mcfunction`, `upgrade/stair.mcfunction`, `completed_challenge.mcfunction`
- `lbc/function/config.mcfunction`; `lbc/advancement/true_advancements/legends/academy/air.json`
- Read-only language evidence: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/lang/en_us.json` and `ru_ru.json`, keys `upgrade_condition_stair` / `upgrade_succesful_stair`.
