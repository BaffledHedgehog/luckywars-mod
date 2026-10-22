# Указатель документации SkyWars

## Hostile Helicopter Boss encounter - QUALIFIED (user)

[Current contract](HELICOPTER_ENCOUNTER_PORT.md). The dropped Polymer token now creates the hostile
ravager/display pair and source three-phase encounter, separate from the handheld
weapon. Author approved a complete four-frame animation and ordinary reward death
under mechanism suppression. Combat/beacon death emits exactly30 source TNT and
one decorated qualified weapon; unloading/match stop is quiet and reward-free.
Free Cultus drops reuse all six effects without bats, preserving real NPC causes.
Native lightning stays WILD; native arrow owner changes replace retained causes.

Build and two isolated match cycles passed on497c091e (18 suite results,44.06s).
Ready, not installed: build/verification/skywars2-helicopter-encounter-497c091e.jar.
Evidence: build/verification/helicopter-encounter-final/. Native give/ported loot,
stack debit, concurrent pairs, defenses, arrows/time stop, six payloads, reward,
beacon and cleanup are checked. Random phase3 scatter and real-client visuals
remain manual checks. Main remains69de8601; original RP unchanged, no new assets.

Repository maintenance: b0fe490 removes generated/local files from the index;
12beb0a checkpoints preceding migration. Useful local builds/evidence/worlds remain.
Next: remaining special-drop encounters/acquisition; unfinished mode guards remain.


## LuckyWars modifier owner - QUALIFIED (user)

[Current contract](LUCKY_MODIFIERS_PORT.md). Author clarified: ordinary SkyWars has no Lucky Blocks
or their drops; do not treat a retained modifier score as an ordinary-mode bonus.
The permanent GUI selector always persists. Only committed LuckyWars mode2 start
resolves it once before chest/kit rolls. Ordinary starts and all ends retain the
last result; ending only releases the next-match commit guard. No tick scan.

Build, isolated native settings/permissions/JSON/loot-condition checks, explicit
LuckyWars service branches and two actual ordinary retention cycles passed on
69de8601 (51.13s). Ready, not installed:
build/verification/skywars2-lucky-modifiers-69de8601.jar. Evidence:
build/verification/lucky-modifiers-final/. Main server and original RP unchanged.
Modes2..4 remain blocked; a full LuckyWars gameplay start is not yet qualified.
Direct ported-item fixtures in the normal-mode regression are test setup only.
Next acquisition candidate: hostile Helicopter Boss encounter, documented in
NEXT_LUCKY_ACQUISITION_SOURCE.md; its handheld weapon is already qualified.


## LuckyWars modifier settings - CONFIGURATION QUALIFIED (user)

[Current contract](LUCKY_MODIFIERS_PORT.md). luckyModifier0..12 persists across matches/restarts;
settings slot11 cycles13 original labels/styles, and OP /settings exposes aliases.
Author scope is ONLY LuckyWars. Configuration publication never rerolls a match.
Explicit owner commit/reset API and source89%/1% random resolver are checked, but
controller match-transition hooks remain pending the resolved-state clarification.
Do not reset the permanent GUI selection or enable unfinished game modes2..4.

Build and isolated native settings/permissions/JSON/loot-condition checks passed
on5283a768. Ready archive: build/verification/skywars2-lucky-settings-5283a768.jar;
evidence: build/verification/lucky-modifiers-settings-final/. Lifecycle is NOT
qualified. Main server and original RP untouched; no new RP resources needed.
Next: finish that clarification/hooks; then the hostile Helicopter Boss encounter
source audit in NEXT_LUCKY_ACQUISITION_SOURCE.md, not the already ported weapon.


## Reroll Machine - QUALIFIED (user)

[Current contract](REROLL_MACHINE_PORT.md). Kit25 is restored on page9100 slot0 with the original
Academy Lightning gate, locked design and ranked access. Native Polymer machine
preserves the source model/name/two lore rows, with identical give/loot payloads.
Both crafting grids share Tiny Teleport's forced random transaction: one retained
machine, one unit per donor slot (up to3/8), complete reward/overflow delivery.
Debit5000 per successful donor even below zero; empty rolls preserve inputs/mana.
Ordinary categories use the source player loot context at +1Y. Matching rejects
missing catalysts before category allocations; no inventory tick scan is added.

Build and four native matches passed on b60d4969 (102.93s). Ready, not installed:
build/verification/skywars2-reroll-machine-b60d4969.jar. Evidence:
build/verification/reroll-machine-final/. Main and original RP untouched; one new
preview lore translation per locale is in the template. Native prototype model
is applied after vanilla property finalization via Fabric's component event.
Next: LuckyWars modifier ownership/settings, with separate decisions about normal
matches and resets; see LUCKYWARS_LUCK_SOURCE.md. Do not enable unfinished modes.


## Tiny Teleport forced random crafting - QUALIFIED (user)

[Current contract](TINY_REFORGE_CRAFT_PORT.md). Inventory/workbench show an opaque stable preview; actual
Ultra Reforge rewards are rolled only on committed take, with one donor consumed
and 30% chance to consume one Tiny. Full reward bundles are retained, including
inventory overflow. Click/shift/swap/drop paths cannot leak the preview; blocked
or unsupported actions do not roll. Existing Tiny pair recipes retain precedence.
Reuses the same category mapper and loot roller as existing Reforge routes.

Build and four native matches passed on 310f2fc9: two crafting (41.56s), two shared
lightning/Infbolt regression (47.29s). Ready, not installed:
build/verification/skywars2-tiny-reforge-310f2fc9.jar. Evidence:
build/verification/tiny-reforge-final/. Main and original RP unchanged; merge two
new template translation keys per locale when convenient (English fallback exists).
Next: separately define Reroll Machine/LuckyWars acquisition and hostile encounter
contracts; do not infer them from the qualified Boss Helicopter weapon.


## Ambient lightning acquisition - QUALIFIED

[Current contract](LIGHTNING_ACQUISITION_PORT.md). Live native bolts reforge one unit per dropped item per
unstopped tick within radius 4, with exact independent source categories and
nearest active recipient delivery. Shared Reforge input transactions retain
existing normal/Ultra behavior; an ownerless queue uses eight units/1 ms per tick.
Empty/unsupported/no-recipient inputs remain; lightning-specific source protection
prevents native item damage from racing conversion. Build and two native matches
passed on 6e5f45f7 (43.37s, 18 suite results). Ready archive:
build/verification/skywars2-lightning-acquisition-6e5f45f7.jar, not installed.
Evidence: build/verification/lightning-acquisition-final/. Main/RP unchanged.
Next: forced random Tiny Teleport crafting, then other acquisition/encounter work.


## KFC corrections - QUALIFIED (user)

[Current contract](KFC_CORRECTIONS_PORT.md). Chicken Blaze spirits are WILD, including nearest
allied/self targets, native direct hits, burning and owner transfer; role/service
guards remain. Win button consumes through ordinary item use without a snowball,
with the same RP model. Missing live player counters/effects are connected;
retain each counter's existing +1 step and unit, without duration normalization.

Exact 112-entry source ledger verified, reverse_mana twice; no attribute/resource/
clock/ID grant. Effect extension preserves killer attribution. Build and two native
matches passed on 9b2ac45e (42.3s); archive skywars2-kfc-fixes-9b2ac45e.jar
ready, not installed. Evidence build/verification/kfc-fixes-final/. Main/RP untouched.


## Lobby weather and arena fluid cleanup - QUALIFIED (user)

[Current contract](MAP_FLUID_CLEANUP_PORT.md). Lobby entry resets clear/rain/thunder to native maximum
timers and clears weather flags. Every-stage arena edits extend the actual next-map
cleanup bounds and persistent journal. Native fluid ticks wait during replacement
without being discarded; resident section palettes recover older untracked fluid.
Finished-map viewing, lobby overlay and tick-budgeted loading remain.
Build and two native matches passed on f0e11a5f (56.77s, map 1 ->35 ->1).
Archive skywars2-map-fluids-f0e11a5f.jar is ready, not installed. Evidence:
build/verification/map-fluids-final/. RP/template and main server untouched.


## Lightning friendship and typed items - QUALIFIED (user)

[Current contract](LIGHTNING_AND_TYPED_ITEMS_PORT.md). Chain ray/radius acquisition and repeated propagation
include allies and caster; antimagic and role/service guards remain. All actual native
LightningBolt entities ignore friendship, retain owner/ignition attribution and native
hurt cooldown; managed electric type/Techno immunity remain. ElectroNativeMixins now
centralizes lightning handling, replacing KfcLightningMixin. Typed dropped items with
a top-level custom_data `type` key resist all damage except out_of_world, evaluated
per hit; pickup, despawn and explicit removal remain. New suites: lightning-rules,
lightning-rules-chain and typed-items, with existing regression suites in
`tools/smoke_lightning_rules.py`. Build and two native matches passed on 39220c8c (26 suite results, 45.88s).
Ready archive: skywars2-lightning-rules-39220c8c.jar; not installed. Evidence:
build/verification/lightning-rules-final/. Historical notes below are preserved.

## Duplicating performance and hole calibration - QUALIFIED (user)

User: fix meditation/normal/Expand CPU and stale-position lag; final hole choice
is existing shader R=.5, shadow about 1.3, outer lens 7 and terrain destruction 7,
with no RP patch. This supersedes previous shadow 4/destruction 4 notes. Strong
core radius 1.3; weak influence 16 and input/team/service protection remain.

[Current contract](DUPLICATING_PERFORMANCE_PORT.md). Each of the 91 meditation centers now owns one worker
with current operations and one replaceable latest request. Intermediate stale
requests are coalesced; accepted casts/centers remain. Fan copies share one coherent
source capture, with ordered writes and full containers. FULL chunk residency is
checked via getChunkNow before block access, with native async source preparation;
no world reads/writes are moved to another thread. Identical non-BE writes skip
support queries, plain snapshots are reused, and existing air clear is skipped.

Build and two native matches passed on 02952e5f; archive
skywars2-duplicating-fix-02952e5f.jar is ready, not installed. Benchmark meditation
peak pending 2622 ->91, post-expiry 2748 ->1; see bounded-measurement limitations in
the contract. RP/template/main server untouched by this batch.


## Boltbolt regression fixes: QUALIFIED, ready without installation

[Current contract](BOLTBOLT_PORT.md). User: five charges / 200 ticks, strike every 40 ticks.
Wind Racer now synchronizes native arrow motion/position every tick. North Pole
restores complete scale 3 transformation and nonadditive legacy fall phases,
with ground-contact collision and 2-tick impact visual retention. Two native matches
passed on fe456346, including 45-tick real tracker packet capture,25 actual strikes,
meteor terrain and cleanup. JAR skywars2-boltbolt-fix-fe456346.jar is ready,
not installed. Remaining migration order follows the shooter-pool contract below.


## Shooter pools: QUALIFIED; archive ready without installation

[Current contract](SHOOTER_POOLS_PORT.md). Seven source tables use thirteen qualified native
Polymer weapon payloads; original nested weights/quality, conditional Mobgun
at fixed luck7 and Razor seeds are preserved. Both public pool aliases are now
available under lbc:ported/transformables_shooter and _all. Four final native
matches passed on 61a1b083 (two pool/acquisition plus two shared Infbolt regression).
Archive skywars2-shooter-pools-61a1b083.jar is ready, not installed. Original pools
were already reachable from native Reforge; autocomplete was never an acquisition
gate. RP/template and main server unchanged. Next: separately audit remaining
legacy acquisition routes, then the hostile Helicopter Boss encounter.

## Govnomet / Boss Helicopter: QUALIFIED; archive ready without installation

[Current implementation and open questions](NEXT_SHOOTER_PORT.md). Native item/input/lifecycle
and confirmed user decisions are qualified: four final native matches passed
on 332415d6. Tables are ported; ready archive skywars2-next-shooters-332415d6.jar
is not installed. RP/template unchanged. Next: acquisition and shooter-pool closure,
then the separately audited hostile Helicopter Boss encounter. See the contract.


## AK47 / Magnum: QUALIFIED; archive ready without installation

[Current contract](FIREARMS_PORT.md). Author rules (user): five Magnum shots per round;
cancel queued shots immediately when unheld; finite AK pays for its own used-hand
shot and infinite AK stays free. All six Magnum modes and native Polymer rounds
are implemented. Big acid aims down. TECH gates and shared per-player SLR cooldown
with ready sound at zero reuse existing helpers. Four tables are promoted to ported;
the original magnum_ammo alias is byte-identical for existing UltraReforge.
Final build, catalog check and two final native matches passed on `062c19b3`
(7 suites per match, including both cleanups; 50.75 seconds). Resource/CRC audit passed.
Archive `build/verification/skywars2-firearms-062c19b3.jar` is ready, not installed;
main JAR remains ba2e46e6. Source pools/RP/template unchanged. Two additional shared
regression matches passed on the same final JAR (48.9 seconds). Next bounded audit: Govnomet and Helicopter Boss.

[Attack Helicopter / Laser Minigun / mechanism beacon: implemented, QUALIFIED; archive ready without installation](SHOOTER_PAIR_PORT.md).
[Source helicopter](ATTACK_HELICOPTER_SOURCE.md), [source minigun](LASER_MINIGUN_SOURCE.md),
[resource audit](SHOOTER_PAIR_RESOURCES.md), [next AK47/Magnum audit](AK47_MAGNUM_NEXT_SOURCE.md).
Author decisions (user) implemented; three tables ported. Final build and six native matches passed
on dee146b1; archive `build/verification/skywars2-shooter-pair-dee146b1.jar` is ready without installation.
AK47/Magnum is now qualified; next audit is Govnomet/Helicopter Boss.

[Woodificator: current implementation contract](WOODIFICATOR_PORT.md): offline
texture-colour conversion, full solid wood palette, preserve wooden shapes,
container item spill. Build and four native matches passed on `1f5e4b42...`;
public table `lbc:ported/woodificator`, archive ready without installation.
[Bounded pool/source audit](NEXT_POOL_AUDIT_2026_10_21.md) records legacy inputs;
its old twelve-tag proposal is superseded by the current user contract.

[Duplicating: текущая переработка](DUPLICATING_REWORK_PORT.md): legacy-операции,
Expand с большей областью и другими измерениями, Black Hole с тенью R4,
бюджетным удалением и притяжением врагов. Build и по два изолированных матча
Duplicating/Consuming на `a1d43d2e…` прошли. [Точный аудит legacy](DUPLICATING_LEGACY_REWORK_SOURCE.md).

[Снаряды и дружественные мобы](FRIENDLY_PROJECTILES_PORT.md): удары допускают
дружественных мобов, союзные игроки защищены; самонаведение только по врагам
(user). Все варианты Lifestealer считают принятые попадания. Build и два матча на `a40e0041…` прошли за 46,31 с;
проверены native potions и настоящий LargeFireball. Итоги — SERVER_TESTS.
[Mobgun: перенос](MOBGUN_PORT.md), [исходник](MOBGUN_SOURCE.md): регистрация,
ввод, lifecycle и native потомки подключены; build и два изолированных матча
на `a40e0041…` прошли за 40,46 с. Общая проверка снарядов также прошла два матча на том же SHA;
архив `skywars2-mobgun-friendly-a40e0041.jar` сохранён без установки; регрессии Stand/TimeStop и KFC прошли.
Одно правильное splash/lingering зелье, cooldown/lore 2,5 с (user).
Общие shooter/Lucky-пулы не объявляются полностью готовыми.

[Ядерный shader-гриб](NUCLEAR_SHADER_PORT.md): Exoperso/Nuke и общий вход для
будущей ядерной бомбы; 16 ближайших эффектов отдельно каждому игроку (user),
70-секундный цикл, сохранённые урон/разрушения и жизненный цикл слотов.

[Часы и остановка времени](TIME_STOP_PORT.md): 20 секунд в измерении активации,
40 секунд кулдауна после возобновления, накопление настоящих попаданий (user),
native крафт 2×2/3×3 и сохранение задержек block/fluid ticks.
[Overheaven и Field](OVERHEAVEN_PORT.md): пассив часов, Y>=200, пять будущих
усиленных матчей, 20 общих Bolt-состояний на 150 активных тиков (user).
Build и два матча Overheaven на `c2f6d096…` прошли.
[Обычный/Academy Lifestealer](LIFESTEALER_PORT.md) реализован; build и два
настоящих матча на `ba2e46e6…` прошли, архив без установки. [Аудит](LIFESTEALER_SOURCE.md),
[ресурсы](LIFESTEALER_RESOURCES.md). Mobgun прошёл build и два матча на `a40e0041…`; random-пулы закрыты.

[Stand-стрела и ORAORA](STAND_PORT.md): одно принятое попадание/одна попытка/
одна возвращённая стрела; 90/10 и Жадность сохранены. ORAORA — 100 кулаков
за 10 секунд, обе руки требуют Stand (user). Часы/time stop описаны выше.
[Каталог `/loot`](LOOT_TEST_CATALOG.md) отделяет публичные готовые таблицы
от внутренних/неперенесённых, сохраняя вложенную выдачу и явные ID.

[Phantom Knives](PHANTOM_KNIVES_PORT.md) перенесены в общий стрелковый сервис:
пять волн, обе руки, Stand/Overheaven-варианты и нативные попадания/авторство.
[Аудит механики](PHANTOM_STAND_SOURCE.md) и [исходный дизайн](PHANTOM_STAND_RESOURCES.md).
Волны внутри антимагии пропускаются без повторения (user).
Усиление текущего матча читает общий LbcStand, включая пятый матч.

Все девять [пар Tiny → обычный жезл](TINY_WAND_CRAFT_PORT.md) подключены к
общим нативным рецептам 2×2/3×3; обычный Acid использует общий кислотный
снаряд. [Точные исходники и соответствия](TINY_WAND_CRAFT_SOURCE.md).
Подтверждённые прогоны и установка — в SERVER_TESTS.md.

Исправлен [внешний вид электрощита](ELECTRO_SHIELD_RENDER_REVIEW.md):
правильная vanilla модель выбирается через effectiveModel. Цвет основы и
шесть исходных узоров проверены также в клиентском inventory-пакете для
лута и крафта 2×2/3×3. Build и 4 suites на SHA `6fc4df11…` прошли;
архив без установки, ресурспак не нужен и не менялся.

Полностью восстановлен [legacy bypass cooldown](DAMAGE_COOLDOWN_PORT.md):
14 ванильных типов, включая обычные arrow и generic Stblind, плюс прежние
11 собственных типов. Молнии и обычный melee сохраняют свои cooldown.
Build и 9 suites на SHA `f715e0c5…` прошли; архив без установки.

Последнее уточнение — [физика магических снарядов](WAND_PHYSICS_REVIEW.md)
и [обычный cooldown молний](ELECTRIC_DAMAGE_PORT.md). Магические заряды
игнорируют случайные физические воздействия; кислота, намеренное отражение
и самонаведение сохраняются. У Арконтаса нет отдельного прямого HP-урона.
Build и 10 suites на SHA `b93d9fee…` прошли; архив без установки. Правило
сначала спрашивать о физическом воздействии на новые снаряды записано в AGENTS.

Кристаллизация Stonekivi переработана: [поведение](STONEKIVI_PORT.md),
[аудит](CRYSTALLIZATION_REVIEW.md). Видимые записи идут по ходу обхода,
несколько кастов работают по очереди короткими порциями. Полный capture/plan
барьер и ограничение 128 одиночных клеток убраны. Build и 11 suites прошли
на SHA `75c8ed2b…`; архив без установки. Результаты — SERVER_TESTS.

Шипы Анантакалы (Academy Stone, mode 4) исправлены по уточнению user:
`spikes10x10x10` ставится с integrity=1, без случайных повреждений.
См. [каменный жезл](STONE_WAND_PORT.md) и [проверки](../SERVER_TESTS.md).

Последняя правка — [собственные заряды Exasperato](EXASPERATO_PORT.md):
стандартная серия не отталкивает стандартные заряды того же текущего owner.
Внешние импульсы и отражение сохранены. Build и четыре серверных suites на
SHA `ede0d1e1…` прошли; проверен полный payload из 85 взрывов. Архив без установки.

Текущее расследование — [редкий фриз при появлении жителя](VILLAGER_STALL_AUDIT.md).
Причина не подтверждена. Включены JFR-кольцо и GC/safepoint logs текущей JVM
и следующего запуска через start.bat; игровой код/JAR не изменены.

Предыдущая правка — [смолистый дождь Blind](BLIND_WAND_PORT.md),
[проверка связей и оптимизация](BLIND_RAIN_REVIEW.md). Капля TTL20, появляется
рядом с живым врагом; пустая область не создаёт сущностей. Native owner,
отражение и физические попадания сохраняются. Build и четыре suites на SHA
`d650593d…` прошли, включая 30 облаков/300 капель; архив без установки.

Предыдущая правка — [режимы Academy-жезлов без требования маны](ACADEMY_MODE_AUDIT.md).
Все 21 жезл переключаются при нулевой/отрицательной мане без оплаты и бонусов
каста; стоимость самих заклинаний сохранена. Build и 8 suites на SHA
`fc764811…` прошли; архив без установки, ресурспак/template прежние.

Предыдущая правка — [TNT: прямые взрывы и защита](TNT_PROTECTION_AUDIT.md),
[конфиг/GUI](TNT_CONFIG_PORT.md). Fuse 0 обходит конфиг, fuse 1/2 заменены
прямыми защищёнными взрывами; `tntProtection` 0..5 работает без Resistance.
Build и 14 suites на SHA `eb8113b8…` прошли; архив без установки.

Текущая пачка переноса — [KFC, еда и награды](KFC_PORT.md):
[исходник](KFC_SOURCE.md), [оформление](KFC_RESOURCES.md),
[еда](KFC_FOOD_PORT.md), [награды](KFC_REWARDS_SOURCE.md).
Пушка/курицы, общая турель, 20 исходов, нативная еда, Cultus и ложная победа;
build и три матча на SHA `4902c01a…` прошли. Архив без установки.
Случайная замена исправлена по ответу автора: ровно один слот и один предмет.

Предыдущая пачка — [«Незнание» и награда Academy](ACADEMY_CHALLENGE_PORT.md):
[исходник](ACADEMY_CHALLENGE_SOURCE.md), [Polymer API](CHALLENGE_VIEW_RESEARCH.md).
Испытание/прогресс/награда разделены, 20 повышений у NPC, исходный GUI,
клиентское скрытие предметов и системного UI, жетон на старте/респауне.
Дополнительный build и три матча на SHA `0e07a806…` подтвердили сохранение
незавершённых 7/20 при повторном входе и полном рестарте, затем завершение
оставшимися 13 повышениями. Архив без установки; игровые классы/ресурсы
побайтово прежние (`90da0cbb…`), расширены проверки.
Добавлен один внутренний impossible-флаг без display; видимая ветка прежняя.

Mobgun прошёл build и два матча на `a40e0041…`; дальнейшие этапы — в плане.

Предыдущая пачка — [SLG-88 / SLR](SLR_PORT.md): [исходник](SLR_SOURCE.md),
[оформление и импорт](SLR_RESOURCES.md). Четыре режима, расход стрел,
нативные попадания и полное оглушение с сохранением внешней физики.
Build и три матча на SHA `9b9fd0a5…` прошли. Итоговый JAR сохранён без установки;
проверки — в SERVER_TESTS. Следующие реальные пробелы описаны в
[аудите обработчиков](REMAINING_HANDLERS_AUDIT.md) и [выдачи](ACQUISITION_AUDIT.md).

Предыдущая пачка — [Duplicating и Divider](DUPLICATING_PORT.md):
[исходник и решения автора](DUPLICATING_SOURCE.md),
[оформление](DUPLICATING_RESOURCES.md).
Обычный режим переработан по авторскому замыслу: пять случайных действий над
фрагментами разных размеров и форм; контейнеры сохраняют содержимое. Expand
копирует из других измерений в измерение заклинания. Испытание сохраняет всех
91 центра с владельцем. Build и пять матчей на SHA `6e4879a2…` прошли;
подтверждены 12/12 крафтовых продолжений. Общая проверка и архив записаны
в SERVER_TESTS. Основной JAR прежний; весь перенос датапака продолжается.

Предыдущая пачка — [Consuming](CONSUMING_PORT.md):
[исходник и решения пользователя](RUBY_CONTINUATIONS_SOURCE.md),
[оформление и ресурсы](CONSUMING_RESOURCES.md).
Ruby + любой cons_material — настоящий крафт 2×2/3×3; оба донора работают
самостоятельно. Перенесены испытание и четыре режима Academy, включая сферу
Silence R48 и глобальный Avarice. Яйцо молнии вредит союзникам (user).
Build и пять матчей на SHA `9f6872df…` прошли; итоговая проверка и JAR —
в [SERVER_TESTS.md](../SERVER_TESTS.md). Архив готов, основной JAR не заменялся.
На момент этой пачки было подтверждено 11/12 сочетаний; Duplicating добавлен выше.

Предыдущая пачка — [Infbolt](INFBOLT_PORT.md):
[исходник и уточнения](INFBOLT_SOURCE.md), [ресурсы](INFBOLT_RESOURCES.md).
Бесплатный инструмент без Academy, полная перековка стеков через общую очередь,
настоящий крафт Teleport + bolt_material и нативное яйцо молнии с владельцем.
`3dd622a6…` прошёл build и пять тестовых матчей на одном SHA.
JAR сохранён без установки; основной остаётся `a0c4cb03…`.
На момент этой пачки было подтверждено 10/12 сочетаний.
Итоги и ограничения — в [SERVER_TESTS.md](../SERVER_TESTS.md).

Предыдущая пачка — [Lightning Bolts и Boltbolt](BOLTBOLT_PORT.md):
[исходник и уточнения пользователя](BOLTBOLT_SOURCE.md),
[оформление](BOLTBOLT_RESOURCES.md), [каталог перековки](REFORGE_RESOURCES.md).
`d1f1d2f9…` прошёл build, два адресных матча и регрессию общих механизмов.
JAR сохранён без установки; основной сервер остаётся `a0c4cb03…`.
На момент этой предыдущей пачки было закрыто 9/12 сочетаний. Результаты —
в [SERVER_TESTS.md](../SERVER_TESTS.md).

Предыдущая пачка — [True Falling Stars и Exahoming](EXAHOMING_PORT.md):
[исходник и ответы пользователя](EXAHOMING_SOURCE.md), native fireball,
обычные/Academy режимы, испытания и общий крафт Air + True Falling Stars.
Curse глобален с уточнённым владельцем; лучи используют общий поиск
LbcExasperato по загруженным чанкам без фиксированной дальности.
`e985d0ba…` прошёл build, два адресных матча и отдельную регрессию общих
механизмов. JAR сохранён без установки; основной остаётся `a0c4cb03…`.
Итоги и ограничения — в [SERVER_TESTS.md](../SERVER_TESTS.md).
RP и template не изменены. Реализовано 8/12 сочетаний, остаются четыре.
Следующая семья: Teleport + lightning_bolts → boltbolt_wand;
сначала аудит донора и результата, затем общий рецепт.

Предыдущая пачка — [Vex Soul и Psychovex](PSYCHOVEX_PORT.md):
[исходник и решения пользователя](PSYCHOVEX_SOURCE.md), [предметы и пресеты](PSYCHOVEX_RESOURCES.md).
Четыре собственных vex, шесть культистов, крафт 2×2/3×3, события урона,
общие Terra/Electro и автор последствий. Проверки и статус сборки — в
[SERVER_TESTS.md](../SERVER_TESTS.md). Настоящий RP и template не изменяются.
`524c850f…` прошёл build и пять матчей на изолированном сервере; JAR сохранён
готовым без установки. Air + True Fallingstars → Exahoming реализован текущей пачкой; проверка идёт.

Предыдущая пачка — [Psychowand и Psychoblind](PSYCHO_PORT.md):
[исходник и решения пользователя](PSYCHO_SOURCE.md), [оформление](PSYCHO_RESOURCES.md).
Общий крафт 2×2/3×3, Academy, психические состояния, native-клики Twitching
и владение враждебным Vex. `26d1fccd…` прошёл build, два матча Psycho и отдельную
регрессию общих механизмов. Результаты финальных проверок — в SERVER_TESTS.
Настоящий ресурс-пак и template не изменялись; установка пока не выполняется.

Предыдущая пачка — [Blaze Wand и Fireblaze](FIRE_BLAZE_PORT.md):
[исходник](FIRE_BLAZE_SOURCE.md), [оформление](FIRE_BLAZE_RESOURCES.md).
Также обновлена [ежетиковая форма Sun](SUN_TICK_OPTIMIZATION.md): мана раз
в пять тиков, кэш изменений при движении и dirty-события вместо полного
повторного размещения. Итоговые проверки/артефакт — в [SERVER_TESTS.md](../SERVER_TESTS.md).
`fc46a1e9…` прошёл build и три финальных серверных прогона на одном SHA.
Сборка готова, оставлена без установки; настоящий RP и template не менялись.

Предыдущая пачка — [Kiuaskivi и Sun Arcanum](FIRE_KIUAS_PORT.md):
[исходник](FIRE_KIUAS_SOURCE.md), [оформление](FIRE_KIUAS_RESOURCES.md).
Самостоятельный Kiuaskivi, Sun, полноценный крафт Fire + Kiuaskivi,
независимые руки и урон Sun только при двух предметах (user).
Обработка областей вынесена в общий с Tannerkivi `BlockAreaWork`;
исправление отставания Tanner включено. Доказательства и статус установки —
в [SERVER_TESTS.md](../SERVER_TESTS.md).
Итоговый `e10835ed…` прошёл build и три нативных прогона, включая регрессии
Stonekivi/Tanner и Fire. JAR готов, не установлен по выбору пользователя;
исправление Tanner из предыдущего `f4c97247…` входит в него.
Fire + Fallingstars → Fireblaze перенесён текущей пачкой выше.

Исправление Tannerkivi: очередь при движении больше не накапливает полные
кубы; добавляются реальные новые полосы с объединением по секциям.
Преобразуются все блоки вне `air/cave_air/void_air/light/structure_void` (user).
`f4c97247…` прошёл build и полный адресный прогон двух матчей, включая
движение и сплошной куб 31³. Готов к установке после остановки основного
сервера. Цифры и актуальный статус — в [SERVER_TESTS.md](../SERVER_TESTS.md).

Предыдущая пачка — [Stonekivi и Tannerkivi](STONEKIVI_PORT.md):
[Stonekivi-источник](STONEKIVI_SOURCE.md), [Tannerkivi-источник](TANNERKIVI_SOURCE.md),
[оформление](STONEKIVI_RESOURCES.md). Полноценный крафт Stone + Tannerkivi,
четыре режима обычного/Academy результата, Sun через порционную магматификацию
и самостоятельная автоматическая аура донора. Решения автора о кометах и
первом импульсе при 500 маны записаны в AGENTS. `a0c4cb03…` прошёл build,
два последовательных матча и три связанных регрессионных прогона;
установлен с резервной копией. Основной сервер не запускался.
Проверки — в [SERVER_TESTS.md](../SERVER_TESTS.md).
Следующая семья — Fire + Kiuaskivi → Sun Arcanum по [плану](NEXT_MIGRATION_PLAN.md).
Начальное исследование: [FIRE_KIUAS_NEXT.md](FIRE_KIUAS_NEXT.md).

Предыдущая пачка — [Wall Wand: обычный и Academy](WALL_WAND_PORT.md):
[исходник](WALL_WAND_SOURCE.md), [ресурсы](WALL_WAND_RESOURCES.md).
Стена, площадка, разборка кирпичей и исходная лестница; исправления автора
записаны в AGENTS. `a628fc57…` прошёл build и четыре серверных прогона;
установлен при остановленном сервере с резервной копией. Подробности —
в [SERVER_TESTS.md](../SERVER_TESTS.md). Основной сервер не запускался.
Stone + Tannerkivi → Stonekivi завершён следующей пачкой выше.

Предыдущая пачка — [бедроковый жезл: обычный и Academy](BEDROCK_WAND_PORT.md):
[исходник](BEDROCK_WAND_SOURCE.md), [оформление и донор](BEDROCK_WAND_RESOURCES.md).
Нативный крафт Stone + Wall, комнаты с возвратом блоков/контейнеров,
событийная регенерация, окончательное снятие управления маяком (user).
`863d710b…` установлен с резервной копией; build, два последовательных матча
и связанные регрессии прошли. Отзыв пола сохраняет автора смерти от падения.
Результаты и границы проверок — в [SERVER_TESTS.md](../SERVER_TESTS.md).
Самостоятельная способность Wall Wand теперь описана выше.

Предыдущая пачка — [Exoperso: обычный и Academy](EXOPERSO_PORT.md):
[исходник](EXOPERSO_SOURCE.md), [лут/дизайн](EXOPERSO_RESOURCES.md).
Решения автора о попадании, mana и общей очереди записаны в AGENTS;
проверки и установка — в [SERVER_TESTS.md](../SERVER_TESTS.md).
`77ace205…` установлен с резервной копией; build и шесть серверных сценариев
прошли, включая реальные убийства и получение достижения. Следующий этап —
[бедроковый жезл](BEDROCK_WAND_SOURCE.md).

Предыдущая пачка — [крафт Air + Exoperso → Exasperato](AIR_EXASPERATO_CRAFT_PORT.md):
[исходный рецепт и отдельная механика донора](AIR_EXASPERATO_CRAFT_SOURCE.md).
Нативные сетки 2×2/3×3, сохранение стопок, приоритет перед ремонтом и прежний
Polymer-дизайн результата. Проверки/установка — в [SERVER_TESTS.md](../SERVER_TESTS.md).
`b3d287d8…` установлен с резервной копией; build и три серверных сценария прошли.

Предыдущая пачка — [обычный и Academy Ruby](RUBY_WAND_PORT.md):
[исходник](RUBY_WAND_SOURCE.md), [представление](RUBY_WAND_RESOURCES.md).
Уточнения автора о притягивании союзников медитацией и smart lifetime
записаны в AGENTS. Итог проверки и установки — в [SERVER_TESTS.md](../SERVER_TESTS.md).
`e3e72640…` установлен с резервной копией; пять серверных сценариев прошли,
основной сервер автоматически не запускался.
Подготовлен [следующий блок крафтов](WAND_CRAFT_NEXT.md).

Предыдущая пачка — [обычный и Academy жезл телепорта](TELEPORT_WAND_PORT.md):
[источник](TELEPORT_WAND_SOURCE.md), [оформление](TELEPORT_WAND_RESOURCES.md).
Перенос слоя с контейнерами без дублирования и targets-only smart lifetime
согласованы с автором. Облако использует нативную стрелу; общие helpers
показаны в [карте проекта](../ARCHITECTURE.md). Статус проверки и установки —
в [SERVER_TESTS.md](../SERVER_TESTS.md). `6b2f55bf…` установлен при остановленном
сервере с резервной копией; адресный прогон и связанные регрессии прошли.
Следующий предметный этап — Ruby.

Предыдущая пачка — [обычный и Academy воздушный жезл](AIR_WAND_PORT.md):
[источник](AIR_WAND_SOURCE.md), [оформление](AIR_WAND_RESOURCES.md).
Ракеты используют smart lifetime (user), испытание — фактическое попадание
взрывом дальше 80 блоков (user). Статус проверки и установки — в SERVER_TESTS.
Быстрая карта классов и helpers — [ARCHITECTURE.md](../ARCHITECTURE.md).
`d196ae2a…` установлен при остановленном сервере; четыре прогона прошли,
предыдущий JAR сохранён. Следующий предметный этап — жезл телепорта.

Предыдущая пачка — [обычный и Academy жезл слепоты](BLIND_WAND_PORT.md):
[механика источника](BLIND_WAND_SOURCE.md), [оформление](BLIND_WAND_RESOURCES.md).
Серверная проверка включает два последовательных матча; итоговый статус
сборки и установки — в [SERVER_TESTS.md](../SERVER_TESTS.md).
`de7b5723…` установлен после проверки остановки; четыре прогона прошли,
предыдущий JAR сохранён. Следующий предметный этап — жезл воздуха.

Предыдущая пачка — [обычный и Academy каменный жезл](STONE_WAND_PORT.md):
[механика источника](STONE_WAND_SOURCE.md), [оформление](STONE_WAND_RESOURCES.md),
[общий учёт автора ловушки](STONE_TRAP_ATTRIBUTION.md).
Статус проверок и установки находится в описании переноса.
Сборка `c9954703…` установлена при остановленном сервере; четыре прогона прошли,
предыдущий JAR сохранён.

Предыдущая пачка — [обычный и Academy огненный жезл](FIRE_WAND_PORT.md):
[механика источника](FIRE_WAND_SOURCE.md), [исходное оформление](FIRE_WAND_RESOURCES.md).
Статус серверных проверок и установки находится в описании переноса.
Сборка `a4367788…` установлена после штатной остановки сервера; предыдущая сохранена.

Выполнен первый [перенос оптимизаций взрывов](EXPLOSION_OPTIMIZATION_PORT.md):
готовые лучи и локальные кэши, A/B одной сборки, native Exasperato-регрессия.
Сборка `e1883379…` установлена в основной сервер после остановки пользователем.
Параллельные тики текущих измерений ещё не реализованы —
[отдельный план](DIMENSION_THREADING_PLAN.md).

Исследование производительности — [Paper, TNT и Fabric-оптимизаторы](PAPER_FABRIC_OPTIMIZATION_RESEARCH.md):
Moonrise/Lithium для 26.3, применимые алгоритмы, риски пересечения mixin и план
сравнения на плотной карте. Это исследование; моды ещё не установлены.

Новая пачка — [Shit Katana](YELLOW_KATANA_PORT.md): крафт Cyberkatana с
`govnoq`-предметом, жёлтые разряды, вращающийся луч и owner при отражении.
Решения автора о проводимости и прохождении луча записаны в AGENTS.md.
Следующие этапы после камня — остальные обычные/Academy жезлы и крафтовые продолжения.

Предыдущая пачка — [Academy: постройка, NPC и испытания](ACADEMY_PORT.md)
для Terra, Electro и Exasperato. Условие Exasperato уточнено пользователем:
считаются задетые цели, а не убийства. Улучшение сохраняет компоненты жезла.
Предыдущие [четыре режима Academy Exasperato](EXASPERATO_ACADEMY_PORT.md)
сохраняют общий owner/lifetime и бюджет ядерной серии без потери взрывов.
[Проверки и установленный JAR](../SERVER_TESTS.md), [план](NEXT_MIGRATION_PLAN.md).
Дальнейшее крафтовое продолжение Cyberkatana — `shit_katana`; не путать
с отдельной Techrunner Katana. [Исследование](CYBERKATANA_UPGRADE_SOURCE.md).
Записи ниже относятся к предыдущим пачкам.

Текущая пачка — [Electroblocker](ELECTROBLOCKER_PORT.md): аксессуар из Electro
и электрощита, полноценный крафт, сфера отражения 14, ускорение своих снарядов
и аура электрического урона по уточнению пользователя. Проверки подтвердили
передачу владельца также у Terra и кислоты. Build и четыре серверных сценария
прошли на `df36d73d…`; JAR установлен с резервной копией `4622f512…`.
Основной сервер после установки не запускался. [Проверки](../SERVER_TESTS.md),
[источник](ENERGYBLOCK_SOURCE.md), [ресурсы](ELECTROBLOCKER_RESOURCES.md).
Exasperato — отдельный жезл, следующий этап; старое объединённое название
«energyblock / Exasperato» не означает одну механику.

Последняя пачка — [три крафта Electro, киберкатана и энергетический щит](ELECTRO_GEAR_PORT.md).
Сохранены 60% возврата жезла и повторное отражение каждый тик по решениям
пользователя. Катана заряжается попаданиями; её эффекты/молнии сохраняют автора.
Build и десять серверных сценариев прошли на `4622f512…`; JAR установлен
с резервной копией `24afb5ca…`. Основной сервер не запускался.
[Проверки](../SERVER_TESTS.md), [источники](ELECTRO_GEAR_SOURCE.md),
[ресурсы](ELECTRO_GEAR_RESOURCES.md), [план](NEXT_MIGRATION_PLAN.md).
Следующие записи об установках относятся к предыдущим сборкам.

Последняя пачка — [обычный Electro, Academy и медитация](ELECTRO_FAMILY_PORT.md).
Сохранены исходные ресурсы, общий электрический урон и уточнение пользователя
о квестовом флаге `upgradable`. Build и девять серверных сценариев прошли на
`24afb5ca…`; проверенный JAR установлен с резервной копией `71d62c93…`.
Сервер после установки не запускался. NPC и зачёт испытания ещё впереди.
[Проверки](../SERVER_TESTS.md), [исходники](ELECTRO_FAMILY_SOURCE.md),
[ресурсы](ELECTRO_FAMILY_RESOURCES.md), [состав проверок](ELECTRO_FAMILY_CHECKS.md),
[дальнейший план](NEXT_MIGRATION_PLAN.md). Следующие записи об установках исторические.

Перенесён [обычный Razorpine](PINEGUN_PORT.md): Polymer, растительные
боеприпасы, native стрелы и исправленная очередь. Два хвоемёта удваивают
выстрелы за один боеприпас по уточнению пользователя. Build и четыре
прогона (девять сценариев) прошли на `71d62c93…`.
[JAR](../build/verification/skywars2-pinegun-71d62c93.jar) установлен в основной
сервер с резервной копией 16 октября в 12:29 МСК; сервер не запускался.
Все предыдущие исправления включены. Следующие записи об установках
исторические. [Проверки](../SERVER_TESTS.md), [следующий этап](NEXT_MIGRATION_PLAN.md).

Исправлен [выбор карт по режиму](MAP_MENU_MODE_SOURCE.md): три страницы
SkyWars отдельно от одной BedWars, без смешивания; старое меню при смене
режима не выбирает карту чужого режима. Build и native-проверки навигации,
меню и инвентаря прошли на `c69fdf8a…`.
[JAR](../build/verification/skywars2-map-menu-modes-c69fdf8a.jar) подготовлен
отдельно, основной сервер работает с `5399fbad…`. [Проверки](../SERVER_TESTS.md).

Для `AttributeHud.icon()` сверены [все 36 атрибутов игрока](PLAYER_ATTRIBUTE_CATALOG.md),
каждому выделен отдельный case. [Десятичная точка](ATTRIBUTE_FONT_AUDIT.md)
уменьшена в новом font-template до 1×1 px с шагом 2; исходный RP не изменён.
Build и серверная проверка столбика прошли на `27bf1a1f…`.
[Инструкция](../resourcepack_template/README.md), [проверки](../SERVER_TESTS.md).
JAR подготовлен отдельно, основной сервер работает с `5399fbad…`.

Исправлена [дуга True Terra Sword](TRUE_TERRA_BEAM_FIX.md): урон проходит
после удара мечом, хитбокс расширен до видимых краёв. По уточнению пользователя
сохранена старая кратность трёх шагов/трёх полос, каждый удар уменьшен до 1.
Build и семь сценариев мечей/электрического урона прошли на `110bc03d…`.
[JAR](../build/verification/skywars2-terra-beam-110bc03d.jar) включает также
исправления ниже и готов отдельно; основной сервер работает с `5399fbad…`.
[Проверки](../SERVER_TESTS.md), [разбор исходника](TRUE_TERRA_BEAM_SOURCE.md).

Исправлено [выпадение лута при загрузке Iron Axe](CHEST_LOOT_SPILL_FIX.md):
редакторские визуалы исключаются до спавна, техническое удаление заполненных
контейнеров выполняется без дропа. Настоящие точки сундуков сохранены.
Build, повторные загрузки/старт Iron Axe и регрессия аирдропа прошли на
`4b59596f…`. [JAR](../build/verification/skywars2-map-markers-4b59596f.jar)
готов отдельно; основной сервер работает с `5399fbad…`.
[Проверки](../SERVER_TESTS.md).

Исправлена [смерть Carpet-бота от аннигиляции маны](MANA_ANNIHILATION_FIX.md).
Вызов kill больше не обходит нативную смерть бота. Реальное взрывное зелье
проверено на `/player`: native kill, +50 монет и сообщение с автором.
Build и три прогона прошли на `8ab2bbbc…`; [JAR](../build/verification/skywars2-mana-carpet-8ab2bbbc.jar)
включает предыдущую пачку Terra и подготовлен отдельно, основной сервер
продолжает работать с `5399fbad…`. [Проверки](../SERVER_TESTS.md).

Добавлены [три нативных рецепта Terra и Razorterra](TERRA_CRAFTING_PORT.md):
результат в слоте крафта 2×2/3×3, точный расход, прежнее оформление и общий
обработчик с ракетными ботинками. Улучшенный хвоемёт использует Polymer и
`LbcTerra`, модель beam, щит и выбранные пользователем 10 тиков / до 75 блоков.
Build и четыре native suite прошли на `5a0aacc1…`.
[JAR](../build/verification/skywars2-terra-crafting-5a0aacc1.jar) подготовлен
отдельно: основной сервер работает с `5399fbad…`.
[Проверки](../SERVER_TESTS.md), [следующий этап](NEXT_MIGRATION_PLAN.md).
Следующие ниже статусы установки относятся к прежним этапам и их JAR.

Расширено [авторство убийств](INDIRECT_KILLS.md) для brbad, нативных
периодических эффектов, перегрузки от зелья маны, динамита и опасных изменений
мира. Подготовлены [сообщения смерти](DEATH_TRANSLATIONS.md) — по 131 новой
записи RU/EN без дублей в исходном паке. Четыре native suite прошли;
[сборка](../build/verification/skywars2-chemical-ownership-5399fbad.jar)
установлена на остановленный основной сервер. Подробности:
[SERVER_TESTS.md](../SERVER_TESTS.md).

Добавлено [авторство отложенных убийств жезлами](INDIRECT_KILLS.md):
Teleport/Ruby — падение и пустота, Stone — удушение в его блоках,
Acid — тики собственного яда. Поздний удар другого игрока имеет приоритет.
Build и native проверка прошли, включая vanilla MOB_KILLS/PLAYER_KILLS,
награду матча и чистый respawn. [Готовый JAR](../build/verification/skywars2-indirect-kills-f47c5aac.jar)
пока подготовлен отдельно: основной сервер работает. Подробные результаты
и границы проверки — в [SERVER_TESTS.md](../SERVER_TESTS.md).

Последнее исправление — [Tiny Electro](WANDS_ELECTRO_PORT.md): от блока дуга
запускается наружу по нормали грани, entity endpoint сохраняет прежний запуск.
Build и native прогон прошли: шесть граней, косое попадание, точный конец шага,
entity-направление, проводимость/команды, щиты, возраст и cleanup.
[Готовый JAR](../build/verification/skywars2-electro-normal-f183182b.jar),
[результаты](../SERVER_TESTS.md). Основной сервер пока держит world lock
со старым `1ce3fa37…`; новая сборка подготовлена.

Последняя правка — [синхронизация Terra и попаданий](PROJECTILE_VISUAL_SYNC.md):
ограниченное стенами/щитами визуальное упреждение на один тик, оба duration=1,
два тика на конечный кадр без продления gameplay-жизни. Пять вариантов используют
общую реализацию. Build, три функциональных матча и два нагрузочных прошли;
при 300 снарядах P95 1.4 мс (обычный) / 2.7 мс (Academy), без real clients.
[Готовый JAR](../build/verification/skywars2-projectile-visuals-bb577291.jar),
[результаты и ограничения](../SERVER_TESTS.md). Основной сервер ещё держит
world lock на старом `1ce3fa37…`; новая сборка подготовлена.

Новая реализация — [круглая зона и пределы сущностей](ZONE_AND_ENTITY_LIMITS_PORT.md):
Polymer display, радиус 384 → 4 за 200 секунд, потеря HP без защиты/knockback;
оба серверных предела всех атрибутов сняты, высокая скорость игрока и транспорта
не откатывается. [Проверки зоны](ZONE_BARRIER_CHECKS.md) и native limits прошли:
40 атрибутов, NBT motion, player/boat packets и сохранённая collision-проверка.
Build и три прогона / пять матчей / 36 check-вызовов прошли на одном
`cb61bdd8…`: [готовый JAR](../build/verification/skywars2-zone-limits-cb61bdd8.jar).
Основной сервер работает с `1ce3fa37…`, замена ожидает stop.
[Результаты и клиентские ограничения](../SERVER_TESTS.md).

Последнее исправление — [сброс всех атрибутов](ATTRIBUTE_RESET_AUDIT.md):
native player bases и удаление всех modifiers до config/perks на старте,
сразу при победе и при отмене матча. Build и два адресных прогона прошли:
четыре матча / 30 check-вызовов. [JAR](../build/verification/skywars2-attribute-reset-c54bb314.jar)
готов к замене; основной сервер работает с `1ce3fa37…`.
[Результаты](../SERVER_TESTS.md).

Свежая правка — [панель атрибутов](INVENTORY_ATTRIBUTE_UI_RESEARCH.md):
все изменённые значения одним столбиком через font shift / negative spaces,
после 20 неподвижных тиков. Build и адресная серверная проверка прошли;
[JAR](../build/verification/skywars2-attribute-column-e0ffd2cd.jar) готов.
Шрифты уже перенесены пользователем в `assets/lbc/font/attribute_hud/`
[ресурспака](RESOURCE_PACK.md); удалённый template не восстанавливается.
На основной сервер новая UI-сборка не установлена: он снова запущен с `1ce3fa37…`.
[Результаты и ограничения](../SERVER_TESTS.md).

Завершён и установлен боевой перенос [обычного и улучшенного Terra Sword](TERRA_SWORD_PORT.md):
Polymer-предметы с прежним оформлением, три модели, конечные снаряды и
индикатор зарядов. По ответам пользователя сохранены non-arrow зарядка
и бесплатная дуга после попадания улучшенного заряда. Build, три серверных
прогона / четыре матча прошли на одном `1ce3fa37…`: мечи, оба дальних жезла,
Ice Sword. Основной JAR заменён с резервной копией; сервер не запускался.
[Готовый JAR](../build/verification/skywars2-terra-swords-1ce3fa37.jar),
[результаты и ограничения](../SERVER_TESTS.md). Следующий этап — нативный крафт
пробуждения Terra, затем улучшенный хвоемёт, см. [план](NEXT_MIGRATION_PLAN.md).

Предыдущая завершённая пачка — [Overload без возврата маны](OVERLOAD_REFUND_FIX.md),
[единый actionbar](ACTIONBAR_STATUS_FIX.md), [панель атрибутов](INVENTORY_ATTRIBUTE_UI_RESEARCH.md),
[лобби на Y=200 с сохранением карты](LOBBY_ORIGIN_FIX.md) и
[прилипающая кислота](ACID_STICKING_FIX.md). Build и адресный прогон
прошли: два матча / 19 групп; отдельный GUI/quickstart — три матча / семь
сценариев. [Результаты и ограничения](../SERVER_TESTS.md).
[Готовый JAR](../build/verification/skywars2-ui-lobby-acid-afb96b4e.jar)
`afb96b4e…` включён в установленную сборку с мечами.

Предыдущая правка — [высота отвердения падающих блоков](ARTIFACT_LIQUIDS_PORT.md):
`floor(y + 0.5)` устраняет скачок вниз при начале падения. Build и
[адресные проверки](ARTIFACT_LIQUIDS_CHECKS.md) прошли на `f877ba3d…`.
[Готовый JAR](../build/verification/skywars2-falling-hardening-f877ba3d.jar)
включает предыдущие этапы; основной сервер пока не обновлён. [Результаты](../SERVER_TESTS.md).

Предыдущая правка — [немедленная очистка эффектов](CLEANSE_IMMEDIATE_REVIEW.md):
артефакты 5/6 отклоняют эффекты до применения и снимают прежние до
урона/лечения. Build, артефакты, мановые зелья и регрессия невидимости
прошли на одном `67c7a14a…`; [готовый JAR](../build/verification/skywars2-cleanse-immediate-67c7a14a.jar)
включает Academy Terra и невидимость III+. Основной сервер пока работает
на `341c6293…`, новая сборка там не установлена. [Результаты](../SERVER_TESTS.md).

Предыдущий этап — [полная невидимость III+](FULL_INVISIBILITY_PORT.md):
Polymer-представление скрывает тело/экипировку/F3+B, сохраняя обычный ID
и серверный урон. [Адресные проверки](INVISIBILITY_CHECKS.md) прошли;
JAR `05e56333…` готов и включает Academy Terra. Основной сервер пока
работает со старым `341c6293…`; пользователь перезапустит позже.

Предыдущие этапы и их результаты:

Предыдущая проверенная сборка — [Academy Terra](TERRA_ACADEMY_PORT.md) в общем
`LbcTerra`, исходная скорость обычного 1,8 и исправление границы чанка.
[Два матча и нагрузка обоих вариантов](TERRA_ACADEMY_CHECKS.md) прошли;
JAR `5cc324db…` готов. Установка ожидает штатного stop работающего основного
сервера; текущий JAR сохранён для отката.

Последняя установленная сборка — [обычный Terra](TERRA_REGULAR_PORT.md):
попадание в цель в ~940 блоках, Polymer-модель и удаление промахов без TTL.
[Проверки и замер 300 зарядов](TERRA_REGULAR_CHECKS.md) прошли вместе с
регрессией Electro; SHA `341c6293…`, основной сервер остановлен.
Полная история артефактов — [SERVER_TESTS.md](../SERVER_TESTS.md).

АирДроп исправлен и установлен: [курицы, пассажиры, количество и чанки](AIRDROP_PORT.md),
[проверки](AIRDROP_CHECKS.md). Build и адресный живой прогон прошли:
58,73 с, один матч, семь check-групп; основной сервер оставлен остановленным.
Предыдущая полная регрессия 78 групп ниже относится к сборке до этой правки;
здесь адресно проверены также обычные сундуки и очистка арены.

Новая правка заморозки (user): [центр и оболочка по хитбоксу](FROST_PORT.md),
[запрет ЛКМ/ПКМ с доступным инвентарём](FROZEN_ACTIONS_REVIEW.md),
[адресные проверки](FROZEN_GEOMETRY_CHECKS.md). Build и `--frost-only`
прошли (52,36 с, один матч); полная регрессия того же JAR прошла за 385,50 с:
26 матчей / 78 check-групп. Проверенный JAR установлен, сервер оставлен остановленным.

Снаряды и CPU: [текущая геометрия и план](PROJECTILE_PERFORMANCE.md),
[стоимость аур кислоты и native handlers](PROJECTILE_PERFORMANCE_AUDIT.md),
[Tiny/обычная/улучшенная Terra](TERRA_PROJECTILE_SOURCE_COST.md).
Обычный Terra: [перенесённое поведение](TERRA_REGULAR_PORT.md),
[проверки и нагрузка](TERRA_REGULAR_CHECKS.md).
Остальная Terra-ветка: [четыре модели и ориентация](TERRA_PROJECTILE_MODELS.md),
[жизнь дальнего снаряда и бой за картой](PROJECTILE_LIFETIME.md),
[исходные TTL и loaded/ticking чанки](PROJECTILE_LIFETIME_SOURCE.md).
Итоговая сборка включает native hitboxes Tiny Terra, боевую логику Tiny Electro
и [общий электрический урон](ELECTRIC_DAMAGE_PORT.md). Build и адресный прогон
электрической категории прошли (51,41 с, один матч); полная регрессия того же
JAR прошла 26 матчей / 78 уникальных check-групп. Проверенный JAR установлен.
Прежние нагрузочные сценарии с 300 AcidProjectile удержали 20 TPS;
они не измеряют производительность Electro.

Уточнение Looper (user): [описание только для получателя с перком](LOOPER_PORT.md),
[Polymer API и обновление представлений](LOOPER_VIEWER_API.md),
[новые проверки](LOOPER_VIEWER_TESTS.md). Серверный стек/ItemEntity не раскрывается навсегда.
Сборка с пятью пачками жезлов, боевой перегрузкой, мановыми зельями,
ядром маны, Looper и артефактами 1:1–8 включена в установленный JAR.
Более ранние регрессии запуска
прошли три матча / семь групп; их не повторяли при изменении только предметов.
Старт и quickstart при подготовке карты проверены через реальные callbacks:
[разбор исправления](MAP_START_BUG_AUDIT.md), [ручной patch сообщения](MAP_MESSAGES_PATCH.md).
Основной и тестовый серверы оставлены остановленными; build/test/install
совпадают по SHA-256. Прежний JAR сохранён в резервной копии.
Конфиг `gameMode: 2` не менялся; поддерживается запуск только режима 1.
Боевая перегрузка и первые жезлы перенесены; остальные заклинания — следующие этапы.
[Результаты и ограничения](../SERVER_TESTS.md).

Предыдущая пачка: [мановые зелья, поведение и границы](MANA_POTIONS_PORT.md),
[исходные зависимости](MANA_ITEMS_SOURCE.md), [сверка предметов](MANA_ITEMS_RESOURCES.md),
[сценарии проверки](MANA_POTIONS_CHECKS.md).
Ядро: [мана и HUD](MANA_PORT.md), [исходная цепочка](MANA_CORE_SOURCE.md),
[дизайн HUD](MANA_UI_SOURCE.md), [проверки ядра](MANA_CORE_CHECKS.md).
Отрицательная мана, Crux и боевая перегрузка перенесены: [контракт](OVERLOAD_PORT.md).
Следующий этап — жезлы и связанные таймеры запрета магии: [план](NEXT_MIGRATION_PLAN.md).
Первая пачка жезлов и cast-тринкетов: [контракт/статус](WANDS_FIRST_PORT.md),
[источник](WANDS_FIRST_SOURCE.md), [ресурсы](WANDS_FIRST_RESOURCES.md),
[проверки](WANDS_FIRST_CHECKS.md), [review](WANDS_FIRST_REVIEW.md).
Вторая пачка — Tiny Air/Stone/Teleport: [контракт](WANDS_SECOND_PORT.md),
[источник](WANDS_SECOND_SOURCE.md), [ресурсы](WANDS_SECOND_RESOURCES.md),
[проверки](WANDS_SECOND_CHECKS.md), [review](WANDS_SECOND_REVIEW.md).
Третья пачка — Tiny Ruby/Terra: [контракт и решения пользователя](WANDS_THIRD_PORT.md),
[источник](WANDS_THIRD_SOURCE.md), [ресурсы](WANDS_THIRD_RESOURCES.md),
[проверки](WANDS_THIRD_CHECKS.md), [review](WANDS_THIRD_REVIEW.md).
Четвёртая пачка — Tiny Acid: [контракт](WANDS_FOURTH_PORT.md),
[исходник Acid и отложенного Electro](WANDS_FOURTH_SOURCE.md),
[ресурсы](WANDS_FOURTH_RESOURCES.md), [API reuse](WANDS_FOURTH_API.md),
[проверки](WANDS_FOURTH_CHECKS.md), [review](WANDS_FOURTH_REVIEW.md).
Пятая пачка — боевая логика Tiny Electro: [контракт и решения пользователя](WANDS_ELECTRO_PORT.md),
[источник](WANDS_ELECTRO_SOURCE.md), [ресурсы](WANDS_ELECTRO_RESOURCES.md),
[проверки](WANDS_ELECTRO_CHECKS.md), [review](WANDS_ELECTRO_REVIEW.md).
Электрическая категория: [правила и реализация](ELECTRIC_DAMAGE_PORT.md),
[идентичность брони](ELECTRIC_ARMOR_SOURCE.md), [native API](ELECTRIC_DAMAGE_API.md),
[проверки](ELECTRIC_DAMAGE_CHECKS.md).
Адресный прогон прошёл; полный прогон и установка фиксируются в
[SERVER_TESTS.md](../SERVER_TESTS.md). Редкая ветка Yellow Lightning остаётся
в плане, отдельно от полётной/боевой способности Electro.
Личный таймер запрета магии: [источник](NOMAGIC_SOURCE.md), [перенос](NOMAGIC_PORT.md).

Предыдущая пачка: [все 16 цветов артефакта 1:7](ARTIFACT_COLORS_PORT.md),
[ресурсы и сверка](ARTIFACT_COLORS_RESOURCES.md), [сценарии проверки](ARTIFACT_COLORS_CHECKS.md),
[общие зависимости](ARTIFACT_COLORS_DEPENDENCIES.md).

Предыдущая пачка: [отвердение жидкостей и падающих блоков 1:3, сфера радиуса 16](ARTIFACT_LIQUIDS_PORT.md),
[нативный API](ARTIFACT_LIQUIDS_API.md), [ревью и физика льда](ARTIFACT_LIQUIDS_REVIEW.md),
[импорт](ARTIFACT_LIQUIDS_RESOURCES.md), [сценарии проверки](ARTIFACT_LIQUIDS_CHECKS.md).
[Исходный разбор цветов 1:7](ARTIFACT_COLORS_NEXT.md),
[точная таблица всех 16 цветов](ARTIFACT_COLORS_EFFECTS.md).

Предыдущая пачка: [защита от взрывоопасных сущностей 1:2](ARTIFACT_EXPLOSIONS_PORT.md),
[исходный/нативный контракт](ARTIFACT_EXPLOSIONS_CONTRACT.md),
[импорт ресурсов](ARTIFACT_EXPLOSIONS_RESOURCES.md),
[сценарии проверки](ARTIFACT_EXPLOSIONS_CHECKS.md).
[Исходное поведение 1:3 до пользовательского изменения формы](ARTIFACT_LIQUIDS_NEXT.md).

Предыдущая пачка: [артефакт защиты от стрел 1:4](ARTIFACT_ARROWS_PORT.md),
[исходник](ARTIFACT_ARROWS_SOURCE.md), [сценарии проверки](ARTIFACT_ARROWS_TESTS.md).

Предыдущие переносы: [выборочная очистка 1:5 и полная очистка кастомных статусов](ARTIFACT_DEBUFF_PORT.md),
[источник](ARTIFACT_DEBUFF_SOURCE.md), [аудит состояний](CUSTOM_EFFECT_CLEANSING_AUDIT.md).
Проверена и установлена: 13 матчей / 36 групп; [результаты](../SERVER_TESTS.md).
Дальнейший объём — [оставшиеся эффекты артефакта](ARTIFACT_REMAINING_NEXT.md).

Пассивные артефакты `1:6`/`1:8`: [перенос и выдача](ARTIFACT_PASSIVES_PORT.md),
[исходный контракт](ARTIFACT_PASSIVES_SOURCE.md), [проверки](ARTIFACT_PASSIVES_TESTS.md).
Предыдущая сборка проверена в 12 матчах / 33 группах; актуальные результаты — [SERVER_TESTS.md](../SERVER_TESTS.md).

Подавление магии и Looper: [текущий контракт артефакта/маяка](MAGIC_PORT.md), [реализация раскрытия артефакта и статус проверки](LOOPER_PORT.md), [исходные компоненты](MAGIC_PROVIDERS_SOURCE.md), [нативные события](MAGIC_EVENTS_API.md).

Ice Sword: [поведение переноса и получение](ICE_SWORD_PORT.md), [исходный контракт](ICE_SWORD_SOURCE.md), [сверка оформления](ICE_SWORD_RESOURCES.md).

Исторический план [источников подавления магии](MAGIC_SUPPRESSION_NEXT.md) выполнен в пределах перенесённых провайдеров. Looper раскрывает артефакт навыком 3; пассивы 1:5, 1:6 и 1:8 также перенесены. [Статус этапа](MYSTICAL_ARTIFACT_NEXT.md), [исторический план пассивов](ARTIFACT_EFFECTS_NEXT.md). Другие эффекты артефактов пока не перенесены.

Snow Gem №18 и Ice Gem №20: [перенос и общий холод/frozen](FROST_PORT.md), [исходные предметы](FROST_TRINKETS_SOURCE.md), [пороги и поведение заморозки](FREEZE_SOURCE_AUDIT.md). Проверки и установленная сборка — [SERVER_TESTS.md](../SERVER_TESTS.md).

Тринкеты: Blaze №1 и Ghast №2 — [поведение и пользовательское исправление владельца](TRINKETS_PROJECTILE_PORT.md), [сверка исходника](TRINKETS_PROJECTILE_SOURCE.md), [нативные API](TRINKETS_PROJECTILE_API.md). Intercal №10 и Gneiss Stone №16: [поведение и границы](TRINKETS_PORT.md), [аудит механик](TRINKETS_SOURCE_AUDIT.md), [контракт меню](TRINKETS_GUI_AUDIT.md). Проверки — [SERVER_TESTS.md](../SERVER_TESTS.md); следующий этап — [NEXT_MIGRATION_PLAN.md](NEXT_MIGRATION_PLAN.md).

Каталог наборов LBC содержит ID 1/2/4/5/7/9/10/15/17/22/23. Текущий перенос BrBad №17: [поведение и пользовательская физика кислоты](BRBAD_PORT.md), [аудит исходника](BRBAD_SOURCE_AUDIT.md), [SERVER_TESTS.md](../SERVER_TESTS.md). Rocket boots №22: [ROCKET_BOOTS_PORT.md](ROCKET_BOOTS_PORT.md), [исходный контракт](ROCKET_BOOTS_SOURCE.md). Следующий этап — trinkets; зависимости перечислены в [NEXT_MIGRATION_PLAN.md](NEXT_MIGRATION_PLAN.md). Предыдущая пачка динамита и наноловушек: [ITEMS_BATCH_PORT.md](ITEMS_BATCH_PORT.md); аудит исходников: [TNT_SOURCE_AUDIT.md](TNT_SOURCE_AUDIT.md), [RELATED_ITEMS_SOURCE_AUDIT.md](RELATED_ITEMS_SOURCE_AUDIT.md).

Начните с [WORKFLOW.md](WORKFLOW.md): он связывает матч, LBC и карты. Для поиска предмета по имени, модели или custom_data используйте [ITEM_LOOKUP.md](ITEM_LOOKUP.md). Место данных в новом моде: [DATAPACK_EMBEDDING.md](DATAPACK_EMBEDDING.md). Актуальные изменения GUI, загрузки карт, навыков/победы и порядок следующих работ: [GUI_FIXES.md](GUI_FIXES.md), [MAP_LOADING_FIXES.md](MAP_LOADING_FIXES.md), [SKILL_VICTORY_FIXES.md](SKILL_VICTORY_FIXES.md), [NEXT_MIGRATION_PLAN.md](NEXT_MIGRATION_PLAN.md).

Ресурс-пак пользователя, правила работы через template и требование сохранять
оформление предметов один в один: [RESOURCE_PACK.md](RESOURCE_PACK.md).

Роль наблюдателя, камера, инвентарь цели и ограничения для новых игровых механик:
[SPECTATORS.md](SPECTATORS.md).

Набор LBC10 «Призрак»: [порт и границы](GHOST_PORT.md), [аудит исходного поведения и оформления](GHOST_SOURCE_AUDIT.md).

Исходные тексты, оформление и выявленные пробелы прежней документации:
[DESIGN_SOURCE_AUDIT.md](DESIGN_SOURCE_AUDIT.md). Точные исходники и модели —
[design_source_manifest.json](inventory/design_source_manifest.json);
восстановление игровых сообщений — [GAME_TEXT_DESIGN_RESTORE.md](GAME_TEXT_DESIGN_RESTORE.md).
Восстановление исходного меню на 27 слотов и границы переноса —
[GUI_DESIGN_RESTORE.md](GUI_DESIGN_RESTORE.md).

События и границы оптимизации очистки лобби —
[CPU_EVENT_REVIEW.md](CPU_EVENT_REVIEW.md); CPU-замеры и живые проверки —
[SERVER_TESTS.md](../SERVER_TESTS.md), ближайший план переноса —
[README.md](../README.md).

Обычные навыки «Тяжеловес», «Всеядный», «Фазматик»: исходные параметры и
намеренные изменения lifecycle — [VANILLA_SKILLS_2_4_13.md](VANILLA_SKILLS_2_4_13.md),
выбор нативных событий — [VANILLA_SKILL_HOOKS.md](VANILLA_SKILL_HOOKS.md).

Каталог наборов и перков LBC — [LBC_LOADOUT_PORT.md](LBC_LOADOUT_PORT.md); детали переноса rocket boots и сверка исходника — [ROCKET_BOOTS_PORT.md](ROCKET_BOOTS_PORT.md), [ROCKET_BOOTS_SOURCE.md](ROCKET_BOOTS_SOURCE.md).

Предварительный аудит уже перенесённого BrBad №17: [RAY_CLOUD_NEXT.md](RAY_CLOUD_NEXT.md); актуальный статус — [BRBAD_PORT.md](BRBAD_PORT.md).
Сверка выдачи и улучшений — [LBC_LOADOUT_SOURCES.md](LBC_LOADOUT_SOURCES.md),
зависимости навыков — [LBC_PERK_SOURCES.md](LBC_PERK_SOURCES.md).

Нативные предметы LBC: [LBC_ITEMS_PORT.md](LBC_ITEMS_PORT.md); сверка тотема/PHD —
[LBC_PASSIVE_NEXT.md](LBC_PASSIVE_NEXT.md), очищения/BrBad —
[LBC_CONSUMABLE_NEXT.md](LBC_CONSUMABLE_NEXT.md).

Документация datapack-снимка принята полностью: 35 заданий, 10 800 источников.
Обзоры Paper/Ignite описаны отдельно ниже; число 35/35 к Java не относится.
Сквозные предметные пути — [INTEGRATION_REVIEW.md](INTEGRATION_REVIEW.md).
Полноту отражают [RESEARCH_PROGRESS.md](RESEARCH_PROGRESS.md), организация —
[RESEARCH_STATUS.md](RESEARCH_STATUS.md), аудит — [COMPLETION_AUDIT.md](COMPLETION_AUDIT.md).

## Реализации Paper и Ignite

| Тема | Документ |
|---|---|
| Регистрация, аргументы, селекторы, permissions и эффекты команд | [COMMAND_IMPLEMENTATIONS.md](COMMAND_IMPLEMENTATIONS.md) |
| Предметы без команд, мана, UI, BedWars и mixin hooks | [PLUGIN_ITEMS_AND_HOOKS.md](PLUGIN_ITEMS_AND_HOOKS.md) |
| Запуск плагина, native/legacy игра, карты и встроенные ресурсы | [NATIVE_RUNTIME_OVERVIEW.md](NATIVE_RUNTIME_OVERVIEW.md) |
| Сверка результатов и сохранность источников | [JAVA_EXTENSION_REVIEW.md](JAVA_EXTENSION_REVIEW.md) |

Авторские уточнения и требования с пометкой `(user)` важнее агентского текста.
Правила сохранения — [AGENTS.md](AGENTS.md).

## Тематические документы

| Тема | Источников в задании | Документы |
|---|---:|---|
| Цикл матча, лобби, этапы, смерть, победа и сброс | 123 | [skywars_game.md](skywars_game.md) |
| Меню, кнопки, цены, покупки, роллы и настройки | 397 | [skywars_gui.md](skywars_gui.md) |
| Выбор/загрузка карт и инструменты редактора | 234 | [skywars_maps_admin.md](skywars_maps_admin.md) |
| События SkyWars, суффиксы, теги и вспомогательные данные | 83 | [skywars_support.md](skywars_support.md) |
| Все NBT: карты, сундуки, сущности и маркеры | 1457 | [maps_catalog.md](maps_catalog.md) |
| Все события/критерии предметов и достижения | 656 | [lbc_advancements.md](lbc_advancements.md) |
| Броня, комплекты и их активные эффекты | 77 | [lbc_armor.md](lbc_armor.md) |
| Ветка BedWars в LBC | 27 | [lbc_bedwars.md](lbc_bedwars.md) |
| Дропы лаки-блоков, ловушки, постройки и боссы | 135 | [lbc_drops.md](lbc_drops.md) |
| Периодические положительные/отрицательные эффекты | 46 | [lbc_effects.md](lbc_effects.md) |
| Арканумы, тринкеты и прочие предметы | 201 | [lbc_items_arcana_trinkets.md](lbc_items_arcana_trinkets.md) |
| Луки, арбалеты, щиты, мечи, SLR и жезлы | 256 | [lbc_items_weapons.md](lbc_items_weapons.md) |
| Действие наборов, талантов, улучшений и эффектов убийства | 1355 | [lbc_kit_game.md](lbc_kit_game.md) |
| Меню выбора наборов, талантов, улучшений и тринкетов | 146 | [lbc_kit_interface.md](lbc_kit_interface.md) |
| Namespace lbcr: лучи, урон и завершение попаданий | 347 | [lbc_lbcr.md](lbc_lbcr.md) |
| Состав выдаваемых предметов и случайные таблицы | 669 | [lbc_loot_definitions.md](lbc_loot_definitions.md) |
| Все результаты разборки предметов | 625 | [lbc_loot_uncrafted.md](lbc_loot_uncrafted.md) |
| Атрибуты, таблетки, выброс инвентаря и сброс игры | 331 | [lbc_other_attributes.md](lbc_other_attributes.md) |
| Еда, мана, крафт и разборка предметов | 221 | [lbc_other_consumables_craft.md](lbc_other_consumables_craft.md), [lbc_craft_random_mana.md](lbc_craft_random_mana.md) |
| События, Halloween, bucks и troll box | 137 | [lbc_other_events.md](lbc_other_events.md) |
| Imba-предметы, артефакты и God Hedgehog | 293 | [lbc_other_imba.md](lbc_other_imba.md) |
| Магическая академия, гримуар и Sauvojen Tuntija | 247 | [lbc_other_magic.md](lbc_other_magic.md) |
| Прочие предметы, наземный крафт и преобразования | 215 | [lbc_other_main.md](lbc_other_main.md), [lbc_mana.md](lbc_mana.md) |
| Остальные самостоятельные механики other | 214 | [lbc_other_misc.md](lbc_other_misc.md) |
| Nexus, технологии, Wisp, железная дорога и nuke | 253 | [lbc_other_world_tech.md](lbc_other_world_tech.md) |
| Проверки условий и изменения компонентов | 230 | [lbc_predicates_modifiers.md](lbc_predicates_modifiers.md) |
| Снаряды и гаджеты: попадания и owner | 70 | [lbc_projectiles.md](lbc_projectiles.md) |
| Старые эмиттеры Sauvojen и усиление чёрной дыры | 20 | [lbc_raycast.md](lbc_raycast.md) |
| Теги, урон, измерения, worldgen и диалоги | 314 | [lbc_registry_support.md](lbc_registry_support.md) |
| Load/tick, остановка времени, owner, общие helpers | 250 | [lbc_runtime.md](lbc_runtime.md) |
| Предметный интерфейс и индикаторы LBC | 11 | [lbc_ui.md](lbc_ui.md) |
| Пробуждённые жезлы Tuntija | 277 | [lbc_workingitems_awaken.md](lbc_workingitems_awaken.md) |
| Генераторы и Sauvojen staffs | 324 | [lbc_workingitems_generators.md](lbc_workingitems_generators.md) |
| Основные обработчики рабочих предметов | 271 | [lbc_workingitems_main.md](lbc_workingitems_main.md) |
| Вспомогательные предметные подсистемы и боссы | 288 | [lbc_workingitems_other.md](lbc_workingitems_other.md) |

## Проверенные границы исходников

- [SOURCE_FINDINGS.md](SOURCE_FINDINGS.md): пустые JSON, время, контекст, геометрия и округление.
- [RESOURCE_BOUNDARIES.md](RESOURCE_BOUNDARIES.md): отсутствующие определения и динамические вызовы.
- [COMMAND_BRIDGES.md](COMMAND_BRIDGES.md): подстановка команд плагинов и граница внешней реализации.
- [maps_catalog.md](maps_catalog.md): каждый NBT и сопоставление с placement-командами.

Машинные индексы и coverage находятся в `inventory/` и `coverage/`. Инструменты в `tools/` позволяют повторить инвентаризацию и проверку ссылок; они не запускают игровую механику и не изменяют datapacks.


Current port status (focused/full server checks passed; tested JAR installed): [Overload native implementation and client-test limits](OVERLOAD_PORT.md), [mana contract](MANA_PORT.md), [next Mystical Artifact scope](MYSTICAL_ARTIFACT_NEXT.md).
