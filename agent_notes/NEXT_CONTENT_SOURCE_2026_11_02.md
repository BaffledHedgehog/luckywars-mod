# Next content consumer audit — 2026-11-02

Read-only audit against current sources; only this note written. No build, git, server, RP or pending-question mutations.

## Recommendation

Close four linked successful-shot consumers: kit/catalog trinket IDs **3 penis_gun, 7 bedrcock, 9 volos, 11 cocktail**. Fourier13 is a coherent fifth extension, but needs a separate homing-fangs lifecycle and is optional for the first batch. **Drojji4 is melee, not bow.** Do not infer catalog IDs from translation suffixes (IDs are suffix+1 here).

Actual missing evidence: current `LbcTrinkets.shot()` handles only Blaze/Ghast; source Java search finds no `penis_gun`, `bedrock_arrow`, `volos_arrow`, `cocktail_arrow`, or `homing_evoker` consumer. Raw loot tables exist under `src/main/resources/data/lbc/loot_table/trinket/`; their existence does not implement their effects. Current loadout catalog omits these IDs. Existing successful-shot entry is `TrinketBowMixin:39–44`, and `LbcMachinegunSkill:130` also calls `trinkets().shot(p)`. A new implementation must preserve one admission per actual successful native shot and avoid inventing a second projectile engine.

## Exact source graph and chances

Legacy root below: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/`.

`items/bows.mcfunction:12` and `items/crossbows.mcfunction:24` call `items/bows_get_trinket` when #lbctrinket>=1. That file counts inventory custom-data identities with native `clear ... 0`, then independently invokes each effect. No mutual exclusion and no top-level MAG/TECH guard on this trinket call. Let n be actual matching item count.

| Catalog ID / identity | Source proc | Actual terminal/dependencies |
|---|---|---|
| 3 / penis_gun | 10% at n=1; (100+25(n−1))/1000 for 2..36; guaranteed n>=37 | `items/trinket_effects/charge_penis`: uniform variant1..6, corresponding authored sound, actual invulnerable item at eyes with Age−32768/Health10000/PickupDelay3; instantitem/penis_bomb/penis_num (variant2 also govnoq), six original models; `raycast_vpered_slow` launch; lbcID2/team inherited. Reuse KFC bomb owner/runtime and inventory hazard payload, not a fake arrow explosion. |
| 7 / bedrcock | (200+100(n−1))/1000 | `bedrcock_arrow` tags arrows within1 of eyes. `projectiles/prein:6` -> `projectiles/bi_arrow`: fill ±0.5 bedrock **destroy**, stone.place sound, kill arrow. This is collision/ground terminal, not a timed blast. |
| 9 / volos | (50+10(n−1))/1000 | `modify_volos` tags arrows and inherits identity/team. `projectiles/pre:12`: each projectile processing pass, 50% trial and any mobs within100 -> `swrg_kit_integration/game/trinkets/volos_arrow_tick`. Spawn blind-wand marker lifetime35, aim at nearest eligible mob within100, inherited identity/team; then independent5% terminal clears ±0.5 to air destroy,100 ink particles, amethyst fall, kill arrow. |
| 11 / cocktail | (100+50(n−1))/1000 | `cocktail_1`: cocktail_arrow+killedarrow, identity/team. `projectiles/pre:11`: any mob within4 -> cocktail_arrow_tick; source ally exclusions from `lbcr:dmgwithid_un_playeronly`; each eligible mob within4 gets 0.5 arrow damage with current arrow and credited shooter,4 electric particles;1% kill per activated pass. `prein:8` kills killedarrow at collision. |
| 13 / fourier (optional) | (150+50(n−1))/1000 | `fourier_11` launches slowraycast/player/magic/homing_evoker marker from eyes. `homing_evoker/tick`: skips fangs when author within2, otherwise find_pos summons fangs at motion_blocking surface when within7 vertical-distance constraint or current position fallback; turns/moves toward nearest eligible mobs within40; dies Lifetime>=130. Native fangs need explicit inherited author. |

Chances are raw source formulae: universal random_chance receives values directly, so certainty above1 is natural. Do not copy unrelated TNT probabilities: `trinket_effects/tnt` belongs to **cast_wand blood_anal_boom**, already implemented. `items/swords_get_trinket:1–2` counts drojji and invokes acid, so it is an independent melee closure.

## Existing native owners to reuse

`LbcKfcRewards.spawnBomb(ServerPlayer,Vec3,int)` at148 already creates typed owned six-variant bombs with clone/lifecycle support; inspect its launch/target semantics before reuse because trinket source launches from eyes, while KFC acquisition has a different origin. `LbcInventoryHazards` already owns actual picked-up mine payloads. `LbcBlind.spawn(...)` at227 owns typed blind charges and lifetime/target semantics; source volos requires lifetime35 and nearest target, not a full mana-paid wand cast (`cast`149). Add a narrow source-specific entry if required, preserving owner engine. Native arrow state/hit hook must own bedrock/cocktail/volos and their coexistence; retained native arrows keep ordinary cooldown/collision/TimeStop/clone/rewind behavior. Do not run nearby-world arrow scans to imitate datapack selectors: use shot-owned native arrows with source semantics deliberately documented.

## Original RP (read only)

Read `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/lang/ru_ru.json` using LiteralPath.

- penis_gun uses model trinket_2/name `lbctrinket.2`: «Любимый ящик с бомбочками»; «10%% шанс выстрелить доп. ивентарь-миной из лука»; «+2.5%% за каждый дубликат».
- bedrcock model trinket_6/`lbctrinket.6`: «Бедроковые шарики»; «20%% шанс появления бедрока на месте падения стрелы»; «+10%% за каждый дубликат».
- volos model trinket_8/`lbctrinket.8`: «Искаженные черви»; «5%% шанс выстрелить стрелой порчи»; «+1%% за каждый дубликат».
- cocktail model trinket_10/`lbctrinket.10`: «Коктейль Поляризации»; «10%% шанс выстрелить стрелой электрополя»; «+5%% за каждый дубликат».
- Fourier model trinket_12/`lbctrinket.12`: «Фурье-сотворённый клык»; «15%% выстрелить наводящимся заклинанием Эвокера из лука»; «+5%% за каждый дубликат».

Reuse exact raw loot-component styling and these keys; no RP rewrite required by this audit.

## Boundaries and decisions for chat

Source launch has no local suppression, but downstream `magic`/slowraycast/projectile processing may supply suppression; trace that shared admission before proposing policy. Do not claim these are wholly unjammed solely from bows_get_trinket. Need author decision only for actual intended deviations: whether radius1 source tagging should affect foreign nearby arrows or only actual shot family, and whether shared typed owner policy supersedes source ally/self exclusions. Attribution inheritance is already required by project/user rules, not an open question. No arbitrary function execution is justified.

Excluded: clocks fragments have no Java jump_1s/2s/5s/10s consumer, but their source directly replays global datapack tick families; this is broad scheduler behavior and unsuitable for a small next batch/TMT-last constraints. Cunny likewise has absent consumer but22 global-script branches, including match reset. Remaining kits6/14/16 map to lucky_baffled/key/key_luck and need acquisition/shared-pool closure validation before calling mechanics missing. Current completed work must not be recommended again.