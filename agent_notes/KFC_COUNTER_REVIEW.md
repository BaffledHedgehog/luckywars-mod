# KFC player counter review - 21 October 2026

## Author requirement (user)

Исход еды с прибавкой счётчиков: «идея была про прибавку 1 ко всем счетчикам
связанным с игроком на уровне кода». Обновлять реальные Java-состояния уже
перенесённых зарядов, таймеров и эффектов, а не только оставшиеся legacy scores.
Этот исход сам по себе не добавляет атрибуты; отдельный исход улучшения восьми
атрибутов остаётся как в источнике. Не менять глобальный тик сервера или ID.

## Native counter units - author clarification (user)

> что, нет нет не надо так исправлять. Да, там и правда где то +1 секунда, где то +1 это тик, так и надо оставить

Every existing owner keeps +1 in its stored unit. There is no normalization of native
one-tick cooldown increments into twenty ticks based on the legacy scoreboard cadence.
Frost stores seconds; native Stand, TimeStop and Homing timers store ticks.

## Scope and evidence

Read-only source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/`.
Exact ordered source: `other/eatables/kfc_chick/all_up_scores_no_mana.mcfunction`.
Cadence evidence: `players_tick`, `players_2t`, `players_5t`, `players_1s`,
`other/target_curse`, and the corresponding `workingitems` child functions.
The table lists all 112 executable additions in order. `reverse_mana` occurs twice;
the commented `nexus_compass_cd` is excluded. The source ledger remains for compatibility;
the Live rows additionally update the actual service owner. No reflection or field-wide mutation is used.

The unported rows are an explicit boundary: retaining their score is not a claim that their
legacy mechanic is implemented. Searching production Java for the exact names and corresponding
state owners found no current `dmg_dealt`, `lucky_block_count`, `greed_prestige`,
`nanobots_tier`, or `living_legend` gameplay-counter implementation. These remain ledger-only;
coins, wins, kills, unlocks and profile selection IDs are unrelated and unchanged.

## Source mapping

| # | Source score | Current port treatment |
|---:|---|---|
| 1 | `bow` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 2 | `jump1` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 3 | `Distance` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 4 | `RC_Raycast` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 5 | `coldown1` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 6 | `coldown8` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 7 | `coldown13` | Live: LbcShooters PhantomBurst.cooldown +1 source point; existing phase delay +1 tick. |
| 8 | `coldown18` | Live merged native Razor queue: active volleys/delay +1 each; no second independent legacy channel. |
| 9 | `coldown18b` | Same merged Razor owner; source duplicate-channel bug is already deliberately removed (PINEGUN_PORT). |
| 10 | `coldown16` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 11 | `coldown17` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 12 | `coldown25` | Live: attack HelicopterBurst.remaining +1 tick; players_5t removes five per five ticks. |
| 13 | `bow_charge` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 14 | `nitro` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 15 | `snowball` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 16 | `foodlvl` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 17 | `hplvl` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 18 | `totem` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 19 | `reactivefuel` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 20 | `sprint_timer` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 21 | `end_damage` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 22 | `piss` | Live: LbcPsycho Affliction.piss +1 through rejection-aware afflict API. |
| 23 | `laser_minigun_spread` | Live: LbcLaserMinigun Spool.spread +1. |
| 24 | `MaxHealth` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 25 | `lookback` | Live: LbcPsycho Affliction.lookback +1 through afflict API. |
| 26 | `scared` | Live: LbcPsycho Affliction.scared +1 through afflict API. |
| 27 | `attack_damage` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 28 | `simpleTimer` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 29 | `shock` | Live: LbcElectro Shock.pulses +1 through shock API; active interval counter +1 native tick. |
| 30 | `supplyFall` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 31 | `swordcharged1` | Live: LbcElectro cyber Charge.charges +1. |
| 32 | `swordcharging1` | Live: LbcElectro cyber Charge.hits +1. |
| 33 | `swordcharged2` | Live: LbcTerra swordCharges +1. |
| 34 | `swordcharging2` | Collapsed native Terra hit gate: successful native hit directly grants charge; no persistent charging flag to increment. |
| 35 | `swordcharged3` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 36 | `swordcharging3` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 37 | `swordcharged4` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 38 | `swordcharging4` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 39 | `swordcharged5` | Live: LbcElectro yellow Charge.charges +1. |
| 40 | `swordcharging5` | Live: LbcElectro yellow Charge.hits +1. |
| 41 | `swordcharged6` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 42 | `swordcharging6` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 43 | `swordcharged7` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 44 | `swordcharging7` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 45 | `swordcharged8` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 46 | `luck_amount` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 47 | `falltimer` | Live native counterpart: LbcStand retained falling count +1 (not entity projectile ages). |
| 48 | `twitching` | Live: LbcPsycho Affliction.twitching +1; existing forced-use remaining +1 tick. |
| 49 | `Lifetime` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 50 | `coldownshish` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 51 | `coldownhohol` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 52 | `coldownMagnum` | Live: shared LbcShooters SLR/Magnum cooldown +1 native tick. |
| 53 | `cyborgBossFly` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 54 | `divider_cd` | Authoritative live score +1; LbcDuplicating.addPlayerCounters enrolls newly positive scores in dividerTimers. |
| 55 | `lust_cd` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 56 | `lust_cd_small` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 57 | `food_count` | Authoritative live score +1, consumed by LbcColors; no Java shadow value. |
| 58 | `indolence_cd` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 59 | `sleep_time` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 60 | `Distance_const` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 61 | `dmg_dealt` | Ledger only: source gameplay statistic/event state has no corresponding ported player-counter handler; not substituted with unrelated profile statistics. |
| 62 | `lolidefecation` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 63 | `lucky_block_count` | Ledger only: source gameplay statistic/event state has no corresponding ported player-counter handler; not substituted with unrelated profile statistics. |
| 64 | `rocket_sound` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 65 | `reactivefuelsub` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 66 | `reverse_mana` | Authoritative live seconds via LbcMana.reverse; both ordered occurrences apply (+2 total), rejection retained. |
| 67 | `technomk2shield` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 68 | `nanobots_coldown` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 69 | `nanobots_tier` | Ledger only: source gameplay statistic/event state has no corresponding ported player-counter handler; not substituted with unrelated profile statistics. |
| 70 | `gens_type` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 71 | `greed_prestige` | Ledger only: source gameplay statistic/event state has no corresponding ported player-counter handler; not substituted with unrelated profile statistics. |
| 72 | `x` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 73 | `y` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 74 | `z` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 75 | `nomagic` | Authoritative live seconds via LbcMana.noMagic; rejection retained. |
| 76 | `gatling_fovos` | Live: LbcBlind GatlingQueue.remaining +1 volley; absent queue starts at one, not default twenty. |
| 77 | `november` | Ledger only: source gameplay statistic/event state has no corresponding ported player-counter handler; not substituted with unrelated profile statistics. |
| 78 | `ray` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 79 | `acid_rifle` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 80 | `screamray` | Live: LbcPsycho.rays +1 native tick through grant API. |
| 81 | `laser_minigun_spin` | Live: LbcLaserMinigun Spool.spin +1. |
| 82 | `laser_minigun_cd` | Live: LbcLaserMinigun Spool.grace +1. |
| 83 | `laser_minigun_sound` | Live: LbcLaserMinigun Spool.sound +1. |
| 84 | `blaze` | Live: existing LbcBlaze.addHeadCharges API +1; no duplicate raw score write. |
| 85 | `coldownoraora` | Live: LbcStand Burst.cooldown +1 native stored tick; unit conversion explicitly declined by the author. |
| 86 | `fevgalea_charge` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 87 | `h` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 88 | `s` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 89 | `v` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 90 | `hueDelta` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 91 | `h+` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 92 | `color` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 93 | `const.rgb` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 94 | `target` | Live: LbcHoming Curse.remaining +1 native stored tick; absent curse created on player with self attribution. |
| 95 | `reverse_mana` | Authoritative live seconds via LbcMana.reverse; both ordered occurrences apply (+2 total), rejection retained. |
| 96 | `void_resistance` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 97 | `dimensional_shifting` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 98 | `all_seeing_eye` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 99 | `living_legend` | Ledger only: source gameplay statistic/event state has no corresponding ported player-counter handler; not substituted with unrelated profile statistics. |
| 100 | `skill_issue` | Live: LbcItems TurnCurse.remaining +1 tick; absent curse created rather than dead-score-only. |
| 101 | `coldowntimestand` | Live: LbcTimeStop player Cooldown.ticks +1 native stored tick; unit conversion explicitly declined by the author; dimension stop unchanged. |
| 102 | `coldownlightning` | Live: LbcStand retained lightningCooldown +1 native stored tick; unit conversion explicitly declined by the author. |
| 103 | `cd_helicopter` | Live: boss HelicopterBurst.remaining +1 tick, source players_tick cadence. |
| 104 | `metal_divider_cd` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 105 | `nexus_roadgun_cd` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 106 | `nexus_corrupter_cd` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 107 | `MagnumNyauwu` | Live: LbcMagnum active queued round count +1; absent queue starts only with a valid held Magnum. |
| 108 | `speed` | Ledger only: transient source scratch/derived/entity value; no persistent player gameplay owner. Does not mutate native attributes, coordinates, projectile age or service clocks. |
| 109 | `danmaku` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 110 | `danmaku_score` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 111 | `pulsar_cd` | Ledger only: corresponding legacy handler/counter is not ported; no invented gameplay state or effect. |
| 112 | `adv_game_minigun_shooted` | Live: LbcLaserMinigun player shot count +1. |

## Additional current player counters

The author clarification extends beyond the old hardcoded list. Explicit bridges also cover:

- Existing Frost cold amount, cooldown and frozen duration in their native stored units (seconds for the two timers); no new freeze is created. Existing cleansing remains authoritative.
- Existing five mana-potion timers, Overload jump/slam timers and Colors RTP timer +1 in stored tick units. Mana/current maximum/base regeneration remain outcome nine.
- Existing finite native MobEffect durations +1 tick; amplifier, ambient, particle and icon flags remain. Infinite durations are unchanged. Native rejection hooks remain in effect.
- Existing rocket-boot jump count +1; an active next-charge deadline moves one tick later. The server tick and absolute clock are unchanged.
- Existing KFC gigaponos duration +1 tick; ordinary KFC gun and Mobgun cooldown +1; AK47 actual ammo +1.
- Existing native player weapon queues/spool counts: Razor volley/delay; Phantom delay; helicopter cadence phase; Stand queued fists/delay; Magnum player shot count; minigun player shot count. New queue activation still obeys its existing held-item contract.
- Existing Psycho forced-use duration and Electro inter-pulse timer +1 tick.
- Existing Homing sustained rain/destiny cast counts +1; target curse is mapped separately above.
- Stand active grant delay +1 tick. Player-owned time-stop cooldown changes; dimension-wide time-stop duration and frozen-entity bookkeeping do not.

The boundary excludes scheduling/work cursors, captured terrain, service generations, entity IDs,
random seeds, display animation ages, projectile lifetime/coordinates, target-refresh deadlines,
HUD caches, and static mode/config/selection values. These are not player gameplay counters.
The collapsed Terra hit gate and fixed dual Razor channels are recorded in the mapping rather
than recreated as artificial dead Java fields.

## Verification

`KfcCounterChecks.run(game)` exercises outcome seven twice on a real active player: unchanged native counter
increments; actual curses from zero; exact one-volley gatling enrollment; existing Frost values;
mana boost and finite native effect extension; shot counters; both helicopter timers; and unchanged
mana resources, world/server clock and player ID. Root owns compilation, registration and native
match qualification. Existing `kfc-scores` also checks cleanse rejection and exact source ledger behavior.

Native duration-only extension uses LbcIndirectKills.extendEffectDuration and preserves existing poison/wither, levitation and pending mana-annihilation attribution. Final native checks explicitly clear combat credit and verify actual delayed poison/wither damage still credits the original player. Build and two final KFC matches passed on `9b2ac45e9ca4a298ee184972d90cb912fc84af2359d2b88628875809e4db92b1`; see [qualification](KFC_CORRECTIONS_PORT.md).
