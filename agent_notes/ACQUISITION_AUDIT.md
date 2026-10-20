# Acquisition audit — 2026-10-20

Обновление: получение награды перенесено в [ACADEMY_CHALLENGE_PORT.md](ACADEMY_CHALLENGE_PORT.md).
Ниже сохранён первоначальный снимок аудита. [Уточнение источника](ACADEMY_CHALLENGE_SOURCE.md):
`lbcChallenge` — испытание, награде соответствует отдельный `lbcChallengeKit`;
reset24 очищает испытание/прогресс, reset25 — награду. Первоначальная рекомендация
ниже переиспользовать одно поле была неточной; текущий код разделяет состояния.

Read-only audit against current Java/resources (not historical coverage claims).

## Next bounded acquisition family: Academy challenge-kit reward

The Academy placement/NPC/trials are implemented. `LbcAcademy` registers `skywars2:academy_token` (line 80), recognizes both token and legacy `magic_academy` drops (342–355), and queues native structure placement once grounded (359–383). `ported/academy_token.json` is packaged by build.gradle (387). These are not migration gaps.

Actual missing path: the Challenge GUI cannot select Academy reward and the round loadout never grants it. `GameMenus.renderLbc` maps LBC_CHALLENGE to page 12000 but maps no loadout kind; only slot 24 has an action resetting `lbcChallenge` (396). It explicitly says challenge-perk migration is pending (413–417). `PlayerStore.Profile.lbcChallenge` is persisted (122), but Java searches show no setter selecting value 1 and no loadout reader. `LbcLoadouts.Kind` contains KITS/SKILLS/LEVELUPS/TRINKETS only; `apply` (220) grants ordinary kits/trinkets/levelups/skills and ignores the challenge reward.

Exact legacy contract:
- `data/lbc/function/swrg_kit_integration/gui/select/12000.mcfunction:8`: GUI slot 0 sets `lbc.challenge.kit=1` only with advancement `lbc:challenges/no_knowledge`.
- `.../gui/select/12000_ranked.mcfunction:1`: ranked slot 0 sets it without that advancement gate.
- `.../game/kit_select/challenge_kit_select.mcfunction`: kit=1 gives `lbc:kit/academy_token` (plus legacy statistics bookkeeping).
- `data/lbc/loot_table/kit/academy_token.json`: display/data source already preserved in native `ported/academy_token.json`.

Reuse current `GameMenus.renderLbc`, persisted profile, native advancement access pattern from `LbcLoadouts.allowed`, and `grantTable(player,"lbc:ported/academy_token")` (261). Round start and respawn already call `lbc.apply` at GameController:518/576; confirm legacy call timing before deciding whether challenge reward repeats at respawn. Scope one selectable challenge reward, gate, reset, highlighted selection and actual grant. Do not implement unrelated challenge-earned perk storage/reset as a side effect.

## Other verified acquisition gaps, not recommended as one small implementation

- Ordinary random Academy building drop: current `barriersnottraps.json` still contains `{magic_academy:1,drop:1}` (783), but this legacy table is not in build.gradle explicit packaged includes and no Java references it. Native recognition exists; general Lucky random-drop routing is not supplied by it.
- `witch_gens` coverage describes 99 legacy functions, not native generator support. Searches for witch_gens/generator/sin_ in current Java find defensive compatibility checks for `sin_greed_active` and regression fixtures, but no acquisition/activation state owner. Thus implementing witch sins would be a new substantial mechanic family, not a missing item grant for already native behavior.
- Tiny staff variants are registered in LbcWands (59,89–108) and have native ported loot. They must not be called unported based on the historical generator notes.

Source root: E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data.

## SLR acquisition source (selected next implementation batch)

Exact source item table: `data/lbc/loot_table/slr.json`. It is referenced by random Lucky pool `loliluckyblock.json:1225` with weight 10, and by reforge pool `transformables_shooter_all.json:151`. The concrete reforge caller is `data/lbc/function/ultra_reforge.mcfunction:8`: input `{shooter:1}` spawns `lbc:transformables_shooter_all`. No SLR entry appeared in current native skywars2 catalog resources or LbcLoadouts. No seasonal/perk grant was found in the checked kit-integration functions; verified paths are random Lucky acquisition and shooter reforge.
