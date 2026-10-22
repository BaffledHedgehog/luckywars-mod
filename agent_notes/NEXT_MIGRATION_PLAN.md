# Следующие этапы переноса

## Five cage drops - QUALIFIED (user, 22 October 2026)

[Current contract](CAGE_DROPS_PORT.md). User approved nearest ANY nonspectator
(self/allies included), WILD hazards and upward-only shaft clearing. Five native
Polymer tokens now preserve source cage geometry and2 anvils/1 fireball/40 potions,
native lava and strict obsidian/water/glass. Shared dispatcher source ordering,
one debit, residency and time-stop/epoch guards are reused. Causal hooks retain
falling-block/potion/fireball/chain ownership and environmental WILD policy;
placed water now supports drowning attribution. Native strict container semantics
are retained; no duplicate inventory delivery or new restoration timer is added.

Build and two mode1 fixture matches passed on09a1cf09:24 suite results,50.07s,
including WILD native damage, DROWN credit and Creeper/TNT/chest regressions.
Ready build/verification/skywars2-cage-drops-09a1cf09.jar, not installed;
evidence build/verification/cage-drops-final/. Main69de8601 and RP unchanged.
Full persistence, real-client visuals, high-altitude potion arrival and new cage
chain/role fixtures remain explicit limits. Next audit: hole/hole_old terrain
traps. Public loot stays limited to qualified leaves; modes2..4 remain gated.

## Creeper special drops - QUALIFIED (user, 22 October 2026)

[Current contract](CREEPER_DROPS_PORT.md). Both ordinary and old tokens are native
Polymer items. Shared dispatch emits exact20/16 native hostile creepers, with
ordinary native5% Air combat-death reward for the first variant. Author requires
throwing-player kill credit; primary explosions, potion clouds and chained TNT
retain it. Shared thrower capture replaces TNT's duplicate map. Source geometry,
native AI/physics/fuse/despawn, mobGriefing and TNT-only protection are preserved.

Build and two real mode1 fixture matches passed on56a15ffb:18 suite results,
55.84s, including TNT/chest regressions and all real stop/epoch cleanup probes.
Ready build/verification/skywars2-creeper-drops-56a15ffb.jar, not installed.
Evidence build/verification/creeper-drops-final/. Main69de8601 and RP unchanged.
Full chunk-unload/offline-author persistence and real-client visuals remain
manual checks, with no measured peak-capacity claim.

Next: cage traps with source shaft/placement geometry and native falling-entity
ownership, reusing LbcSpecialDrops instead of another token scan. Keep public
loot limited to qualified leaves. Crocodildo's separate author/reward questions
and unfinished modes2..4 remain outstanding.

## Thirteen-TNT special drop - QUALIFIED (user, 22 October 2026)

[Current contract](TNT_DROP_PORT.md). Native Polymer tnt_drop emits13 source TNT with fuse70, exact velocities
and fractional +0.1Y origin. Chest/TNT inputs now share LbcSpecialDrops admission,
source-order dispatch and one unit debit per tick. Existing global protection,
native explosion caching, real thrower/reflected/chain ownership, native merge
guards and time-stop/epoch/stop cleanup are reused and qualified.

Build and two actual mode1 fixture matches passed on103e3bd2:12 suite results,
55.41s, including chest regression. Archive skywars2-tnt-drops-103e3bd2.jar is
ready in build/verification, not installed; main remains69de8601, RP unchanged.
See build/verification/tnt-drops-final/ for logs/results and precise limitations.

Next: creeper drops, qualifying any creeper_wand reward dependency; then cage
traps with source shaft/placement geometry and native falling-entity ownership.
Reuse the shared dispatcher rather than adding another token scan. Public loot
leaves stay limited to ready behaviors. Crocodildo still requires its separate
HP/destruction/time-stop/suppression/nuke-egg reward decisions; modes2..4 stay
blocked until their game-flow contracts are implemented and qualified.

## Five loot-chest drops - QUALIFIED (user, 22 October 2026)

[Current contract](CHEST_DROPS_PORT.md). Five Polymer tokens now create lazy native loot chests for bastion,
end-city, buried treasure, village weaponsmith and bonus chest. Author requires
old container contents to drop; old lazy loot resolves exactly once, adjacent
inventories stay intact and all source components/presentation are retained.
Event-indexed dropped inputs reuse native placement/container APIs with deferred
load handling, one unit per tick, time-stop/residency/epoch/unload/stop guards.

Build and two native mode1 fixture matches passed on1b6710c5:12 suite results,
52.43s, including previous Helicopter encounter regression. Ready, not installed:
build/verification/skywars2-chest-drops-1b6710c5.jar. Evidence:
build/verification/chest-drops-final/. Main remains69de8601; RP/template unchanged.

Next bounded batch:13-TNT special drop using shared protection/attribution, then
creeper/cage outcomes with explicit harmful-target and terrain policies. See
[source audit](NEXT_SPECIALDROPS_SOURCE.md). [Crocodildo audit](CROCODILDO_ENCOUNTER_SOURCE.md) records HP, destruction,
time-stop/suppression and unported native nuke-egg reward questions before coding.
Muravei needs NPC acid/reinforcement audit. Do not enable unfinished modes2..4 or
publish whole Lucky Block pools based on these five qualified outcomes.

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

## Attack Helicopter / Laser Minigun / mechanism beacon: implemented, QUALIFIED; archive ready without installation

[Current contract](SHOOTER_PAIR_PORT.md): Attack Helicopter extends LbcShooters
native Arrow/onHit and destroyRubyCell ownership. It is TECH: magic suppression
does not stop it; mechanism suppression does. LbcLaserMinigun implements spool,
both hands at 100 mana per shot, native first-hit 2.5 damage, all eight frames
and Polymer beam visuals. Removing the last held copy stops firing immediately;
the motor coasts down. LbcMinigunMovement owns native hand attributes:
movement_speed -0.5 and jump_strength -1 ADD_MULTIPLIED_TOTAL per copy;
dual holding adds transient movement -1 to reach zero (native alone gives 0.25).
LbcItems owns tick/forget/stop, LbcWands/SkyWars2 input and LbcKfcFood counters.
LbcMagic shares its existing engine with native Polymer mechanism_beacon:
sphere R20 suppression/entity pulse, cube +/-15 mechanical blocks,
#minecraft:mecha block/entity tags and placer ownership. Shared chunk/dirty/load
and lifecycle helpers remain the single beacon engine.
Author decisions (user) are implemented; final artifact gameplay and shared regression passed.
Tables are promoted to ported. AK47/Magnum is now qualified; see the current firearm contract above.
Final build and six native matches passed on dee146b1: two shooter-pair, two
Woodificator/magic and two Phantom Knives/Stand. Archive
`build/verification/skywars2-shooter-pair-dee146b1.jar` is ready without installation.
Next bounded audit: Govnomet/Helicopter Boss.

## Woodificator проверен — 21 октября 2026

[Контракт](WOODIFICATOR_PORT.md): 1287 блоков реестра, 1281 цветовое
соответствие + 6 невидимых исключений, 66 цельных деревянных результатов.
242 деревянных ID образуют отдельный список сохранения: любая форма и
исходные свойства сохраняются, если у состояния нет block entity (user).
Хранилища превращаются с выбросом содержимого без дублирования.
Цвета считаются только офлайн; runtime использует готовые lookup и общую очередь.

Build и четыре изолированных матча на `1f5e4b42…` прошли: два Woodificator,
два Phantom Knives/Stand. Таблица `lbc:ported/woodificator` готова для ручной
выдачи; архив `skywars2-woodificator-1f5e4b42.jar` сохранён без установки.
Текущая ограниченная пачка shooter-пула: Attack Helicopter, Laser Minigun и mechanism beacon;
AK47/Magnum — следующий аудит, их реализация не начата.
AK47/Stalin, Magnum, Govnomet и Helicopter Boss остаются будущими этапами.
Готовность общих пулов/перековки проверять отдельно: Reforge уже достигает
неперенесённых исходов, фильтрация /loot их не блокирует.

## Уточнения Duplicating и Silence закрыты — 21 октября 2026

[Текущий контракт Duplicating](DUPLICATING_REWORK_PORT.md) заменяет прежнее
случайное перемешивание: legacy-копии, Expand без дочерних центров и shader-дыры
с видимой тенью R4, поглощением блоков от центра и velocity-притяжением.
[Silence](SILENCE_ERASE_PORT.md) удаляет сущности без смерти/дропа и очищает
падающие блоки; игрокам остаются 20 урона, god_hedgehog сохраняет иммунитет
и ответную атаку. Build и шесть изолированных матчей прошли на a1d43d2e.
Архив skywars2-silence-duplicating-a1d43d2e.jar готов без установки.

Следующая общая работа остаётся прежней: аудит полной Lucky-выдачи/перековки
и условий открытия пулов, затем статистика выбора/побед для перков и GUI.
Командные режимы и полный аудит защиты команд остаются отдельным этапом ниже.
Закрытые random-пулы не открывать только на основании этих локальных проверок.
Результаты новой пачки — [SERVER_TESTS](../SERVER_TESTS.md); прежние записи ниже
сохранены как история этапов.

План отражает текущий код и следующие этапы переноса. Результаты живых проверок — [SERVER_TESTS.md](../SERVER_TESTS.md). См. [порт loadout](LBC_LOADOUT_PORT.md), [новую пачку предметов](ITEMS_BATCH_PORT.md) и [межсистемный аудит](INTEGRATION_REVIEW.md).

## Текущее состояние

Текущая общая правка — [удары по дружественным мобам](FRIENDLY_PROJECTILES_PORT.md):
союзные игроки защищены, AI и самонаведение выбирают только врагов (user).
Все три варианта Lifestealer используют эту политику и считают принятые
попадания. Build и два изолированных матча на `a40e0041…` прошли за 46,31 с,
включая native potions и настоящий LargeFireball вне траектории. Итоги — SERVER_TESTS.

Текущая предметная пачка — [Mobgun](MOBGUN_PORT.md): регистрация, ввод/lifecycle,
native потомки и запрет сохранения ACTOR подключены. Build и два изолированных
матча прошли на `a40e0041…` за 40,46 с (шесть suites). Одно правильное splash/lingering зелье и cooldown/lore
50 тиков/2,5 секунды согласованы (user). Проверены 113 прямых/89 зельевых исходов, native потомки,
взрывы с настоящим автором, защита игроков/NPC и cleanup. Общая проверка
снарядов также прошла два матча на том же SHA; архивирование готовится без установки.
Общие Lucky/shooter-пулы закрыты до покрытия всех результатов. Затем, по
[аудиту обработчиков](REMAINING_HANDLERS_AUDIT.md) и
[получения](ACQUISITION_AUDIT.md), остаются общая статистика выбора/побед для
перков/наград и GUI, полная Lucky-выдача/перековка. Это не заявление о полной
семантической готовности всех связанных coverage-источников. RP/template прежние.
Ниже сохранены результаты прежних пачек; текущие доказательства Mobgun — SERVER_TESTS.

Ядерный гриб Exoperso и Nuke Academy Exasperato заменён на пользовательский
[shader-визуал](NUCLEAR_SHADER_PORT.md). Общий NuclearVisuals используется
также при будущем переносе ядерной бомбы и любых slowparticlesexplode/_2
маркеров. До 16 ближайших эффектов отдельно каждому клиенту (user),
урон/разрушения прежние. Build и два матча на `c41ff103…` прошли;
архив `skywars2-nuclear-16-c41ff103.jar` готов без установки.

Предыдущая пачка — [Overheaven и Field](OVERHEAVEN_PORT.md): пассив часов,
получение на Y>=200, профильные пять будущих матчей и ORAORA без дубликатов.
Все пять матчей усилены целиком, включая пятый при остатке 0; смерть сохраняет
усиление, респаун не расходует ещё один матч (user). Field использует 20 общих
LbcBolts на 150 активных тиков/7,5 секунды (user), cooldown 600, без маны/Stand.
Build и два матча на `c2f6d096…` прошли. [Часы](TIME_STOP_PORT.md) и
[Stand/ORAORA](STAND_PORT.md) реализованы ранее. Текущая пачка — обычный/Academy
[Lifestealer](LIFESTEALER_PORT.md): код и точные ресурсы добавлены, build и два
настоящих матча на `ba2e46e6…` прошли; архив без установки.
Лечение и испытание считают принятые попадания;
первое столкновение завершает заряд и рядом с владельцем (user).
[Аудит](LIFESTEALER_SOURCE.md) сохраняет отличия от legacy. Mobgun прошёл build и два матча на `a40e0041…`;
неперенесённые random-пулы закрыты.
Уточнение пользователя: ORAORA двигает кулаки и копит попадания во время
паузы; Phantom Knives выпускает ножи, но они ждут возобновления в воздухе
с сохранённым velocity. Конкретные прогоны/SHA — SERVER_TESTS.
Предыдущая пачка time stop: build и четыре матча на SHA `e6043642…` прошли. Архив
`build/verification/skywars2-time-stop-e6043642.jar` готов без установки;
proof — `build/verification/time-stop-verification.json`.

Перенесены [Phantom Knives](PHANTOM_KNIVES_PORT.md): пять волн, обе руки,
обычный/Stand/Overheaven веер, исходный Polymer-дизайн и нативные Arrow с автором.
Подтверждено в двух матчах; продолжение очереди внутри антимагии пропускает
волны без повтора (user). Стрела/ORAORA и остановка уже реализованы;
пассивы часов и межматчевый Overheaven реализованы текущей пачкой через
существующих владельцев времени, эффектов и профилей; build и два матча на `c2f6d096…` прошли.
Уточнения приобретения/повторных дропов уже записаны в STAND_PORT.
Mobgun прошёл build и два матча на `a40e0041…`; завершение случайного shooter-пула остаётся отдельным этапом.
Build и три серверных прогона (пять матчей Phantom/Pine/KFC) на SHA
`67757482…` прошли; архив без установки. RP/template не менялись.

Пары Tiny → обычный жезл добавлены для всех девяти семейств, семь ранее
отсутствовавших рецептов дополняют Terra/Electro. Обычный Acid теперь
зарегистрирован и использует общий кислотный снаряд с отдельным профилем;
Academy Acid остаётся отдельным этапом. См. [перенос](TINY_WAND_CRAFT_PORT.md)
и SERVER_TESTS.md для подтверждения проверок/установки.

Электрощит снова использует встроенный minecraft:shield вместо отсутствующей
custom-модели. Полный рисунок лута/крафта проверен в Polymer и клиентском
пакете, без ресурспака. Build и 4 suites на SHA `6fc4df11…` прошли;
архив без установки. Следующая предметная пачка прежняя.

В активный bypasses_cooldown возвращены все 14 vanilla-записей legacy LBC,
включая arrow/generic: быстрые стрелы и Stblind регистрируют повторные
попадания без invulnerability frames. Молнии и melee не включены.
Build и 9 suites на SHA `f715e0c5…` прошли; архив без установки.
Точный контракт — DAMAGE_COOLDOWN_PORT; следующая предметная пачка прежняя.

Нативные managed-молнии отделены от cooldown-bypass дуг: `LIGHTNING`
сохраняет электрическую защиту, но бьёт с обычным vanilla hurt cooldown.
Магические носители жезлов защищены общей политикой от взрывов и случайного
физического отражения; кислота и намеренные controls остаются. Build и
10 suites на SHA `b93d9fee…` прошли, архив без установки. Дальнейшие снаряды
не должны получать такую физику без уточнения user; следующая пачка прежняя.

Кристаллизация Sun/Stonekivi переведена на потоковый обход и ранние записи
порциями. Повторные касты не ждут предыдущие; сохранены форма по исходным
соседям, защита сторонних правок, контейнеры без дропа и общая атрибуция.
Полный detached-plan барьер убран как лишняя работа. См. STONEKIVI_PORT и
CRYSTALLIZATION_REVIEW; следующая предметная пачка прежняя.

Exasperato: стандартные взрывы больше не сбивают собственные стандартные
заряды с траектории. Отражение и внешние взрывы сохранены; полный payload85
и регрессия всех четырёх Academy-режимов прошли на SHA `ede0d1e1…`.
Архив готов без установки. Дальнейшая предметная пачка не меняется.

Дождь Academy Blind обновлён по уточнению автора: TTL20 и капли рядом
с живыми врагами, без пустых выстрелов. Локальные цели кэшируются с повторной
проверкой пригодности, ink trail не создаёт временные Vec3 на каждом sample.
Нативные отражения/owner и столкновения сохраняются. Build и четыре suites
на SHA `d650593d…` прошли, включая 30 облаков/300 движущихся капель.
См. SERVER_TESTS для границ измерений и [отчёт](BLIND_RAIN_REVIEW.md).
Архив готов без установки; следующая предметная пачка прежняя.

Переключение всех 21 Academy-жезла отделено от проверки/расхода маны:
Shift+ПКМ работает с нулевым и отрицательным ресурсом. Бонусы каста не
срабатывают, платные заклинания сохраняют допуск и стоимость. Это правило
действует для будущих переносов, зафиксировано в AGENTS.md. Build и 8 suites
на SHA `fc764811…` прошли, архив без установки. [Аудит](ACADEMY_MODE_AUDIT.md).

Уточнение TNT закрыто на SHA `eb8113b8…`: короткие fuse 1/2 заменены
прямыми защищёнными взрывами, исходный fuse 0 явно обходит конфиг.
`tntProtection` 0..5 снижается численно после сложности и до брони/эффектов,
все режимы Air используют защиту. Default 4; исходная кнопка GUI восстановлена,
для 100% добавлен один ключ в template. Build и 14 suites прошли, основной JAR
не заменён. [Аудит](TNT_PROTECTION_AUDIT.md), [конфиг](TNT_CONFIG_PORT.md).
Следующие предметы используют эти общие helpers; ручной summon короткого TNT
и выдача Resistance ради конфига больше не нужны. Следующая пачка предметов
Mobgun также прошёл два матча на `a40e0041…`; целевая общая проверка снарядов также прошла два матча на том же SHA.

Новая пачка — [KFC, еда и награды](KFC_PORT.md): пушка/курицы, общая огненная
турель, 20 исходов еды и вложенная съедобная разновидность, ложная победа,
Cultus с шестью вариантами и наследованием автора. Build и три матча
на SHA `4902c01a…` прошли, архив без установки. Реальные счётчики обновляются
через владельцев состояния, multicast идёт через бюджетную очередь.
Случайная замена исправлена по ответу автора: один слот/один предмет.
Ресурспак/template прежние. Полный отчёт — SERVER_TESTS.

Предыдущая пачка — [«Незнание» и награда Academy](ACADEMY_CHALLENGE_PORT.md):
исходный GUI, 20 повышений, независимые сбросы, клиентское скрытие предметов
и системного UI по уточнению автора, жетон на старте/респауне. Build и три
матча с рестартом на SHA `90da0cbb…` прошли. Дополнительный прогон на
`0e07a806…` подтвердил незавершённые 7/20 после выхода/входа и полного рестарта,
затем завершение оставшимися 13 повышениями; игровые классы и ресурсы прежние.
Архив без установки; RP/template прежние.
Источник уточнил первоначальный аудит: испытание и награда — разные поля,
reset24/reset25 различаются. Получение награды закрыто; общая статистика — далее.

Предыдущая пачка — [SLG-88 / SLR](SLR_PORT.md): нативный Polymer-предмет,
четыре режима, стрелы/отдача, пробивание мягких блоков и владельцы урона/яда/TNT.
Электрический режим по уточнению автора блокирует добровольный ввод и инвентарь,
сохраняя внешнюю скорость и нативную физику. Build и три матча на одном SHA
`9b9fd0a5…` прошли; архив сохранён без установки. Подробности — SERVER_TESTS.

Сверка оставшихся предметов выполнена: [обработчики](REMAINING_HANDLERS_AUDIT.md),
[получение предметов](ACQUISITION_AUDIT.md). Ближайшие отдельные пачки:

1. Stand/часы/ORAORA реализованы; текущий Overheaven/Field прошёл build и два матча на `c2f6d096…`.
   Обычный/Academy Lifestealer прошёл build и два матча на `ba2e46e6…`; Mobgun прошёл build и два матча на `a40e0041…`. KFC закрыт отдельной пачкой выше, вместе с едой и
   обеими наградами. При новых переносах подключать персональные игровые
   счётчики к исходу KFC; не оставлять действующие Java-состояния только в scores.
   Общий стрелковый random-пул остаётся закрыт до покрытия оставшихся предметов.
2. Общая статистика выбора/побед для перков и наград, личные/общие значения в GUI.
3. После полного покрытия результатов — общая выдача Lucky/перековки и random-пулы.
   Уже включённая таблица SLR не означает готовность остальных результатов пула.

Team-режимы/Bedwars и общая защита магазина сохраняются отдельным этапом ниже.

Предыдущая пачка — [Duplicating и Divider](DUPLICATING_PORT.md): последний из 12
крафтовых результатов. Обычный режим переработан по замыслу автора, содержимое
контейнеров копируется; Expand переносит из других измерений в измерение
заклинания. Испытание — один центр 200 тиков и 90 случайных на 30 тиков,
все с владельцем. Исходник и решения — [DUPLICATING_SOURCE.md](DUPLICATING_SOURCE.md).
Build и пять матчей на SHA `6e4879a2…` прошли; подтверждены 12/12 крафтовых
продолжений. Регрессия и итоговый архив — в SERVER_TESTS. Основной JAR не заменялся.

Проверять путь получения предмета/испытания через игровой NPC, сохранение owner
и реальный крафт, а не только ручную выдачу готового stack. Наличие документации
или loot-ресурса само по себе не подтверждает перенос поведения.

Предыдущая пачка — [Consuming](CONSUMING_PORT.md): обычный/Academy результат,
четыре режима, испытание, оба самостоятельных cons_material-донора и настоящий
крафт Ruby + донор. Принятые решения Silence R48 и глобального Avarice —
в [исходнике с уточнениями](RUBY_CONTINUATIONS_SOURCE.md). Build и пять матчей
на SHA `9f6872df…` прошли; финальная проверка и архив записаны в SERVER_TESTS.
Подтверждено 11/12 сочетаний. Основной сервер остаётся на прежнем JAR.

Следовавший за Consuming Duplicating реализован пачкой выше; решения автора
теперь записаны в AGENTS.md и отдельном исследовании Duplicating.

Предыдущая пачка — [Infbolt](INFBOLT_PORT.md), бесплатный магический инструмент:
Teleport + bolt_material, настоящие сетки 2×2/3×3, расширенная перековка
через общий обработчик Reforge и нативное яйцо молнии с владельцем.
Academy-версии нет (user); пустая выдача не расходует исходный предмет (user).
`3dd622a6…` прошёл build и пять матчей на одном SHA: по два Infbolt/Boltbolt
и отдельную регрессию общих Teleport/Electro/Exahoming. JAR сохранён без
установки; основной остаётся `a0c4cb03…`. На момент этой пачки было 10/12 сочетаний;
Consuming закрыт следующей пачкой выше.

Предыдущая пачка — [Lightning Bolts и Boltbolt](BOLTBOLT_PORT.md): обычные/Academy
предметы, крафт Teleport + Lightning Bolts, бесплатная перековка, повторная
цепь и четыре режима результата. Решения пользователя — в
[разборе исходника](BOLTBOLT_SOURCE.md). `d1f1d2f9…` прошёл build,
два адресных матча и регрессию Teleport/Electro/Exahoming на одном SHA.
Архив сохранён без установки; подробности — в SERVER_TESTS.
На момент этой предыдущей пачки было реализовано 9/12 сочетаний.
Infbolt закрыт следующей пачкой выше; остаются два продолжения Ruby.

Предыдущая пачка — [True Falling Stars и Exahoming](EXAHOMING_PORT.md):
[исходник и ответы пользователя](EXAHOMING_SOURCE.md), native fireball,
обычные/Academy режимы, испытания и общий крафт Air + True Falling Stars.
Curse глобален с уточнённым владельцем; лучи используют общий поиск
LbcExasperato по загруженным чанкам без фиксированной дальности.
`e985d0ba…` прошёл build, два адресных матча и отдельную регрессию Nuke,
отражений, призывов, Terra/Electro/Blaze. Сборка сохранена без установки,
основной JAR остаётся `a0c4cb03…`. Итоги — в [SERVER_TESTS.md](../SERVER_TESTS.md).
RP и template не изменены. Реализовано 8/12 сочетаний, остаются четыре.
Следующая семья: Teleport + lightning_bolts → boltbolt_wand;
сначала аудит донора и результата, затем общий рецепт.

Предыдущая пачка — [Vex Soul и Psychovex](PSYCHOVEX_PORT.md): самостоятельный
призыватель, полноценный крафт Blind + Summoner Vexes, шесть культистов,
исходные пресеты/броня, общие снаряды и наследование автора убийств.
Luck/Omnipresence используют события урона, Strangeness — очередь с бюджетом CPU.
Призывы исчезают после смерти владельца (user); Omnipresence выбирает одного
ближайшего врага, Vex Soul заменяет только своих vex. Academy у пары нет.
Финальные проверки и статус JAR — в SERVER_TESTS; основной сервер не изменяется.
`524c850f…` прошёл build и три прогона одного SHA: два матча призывателей,
два Psycho и одну регрессию общих предметов/Terra/Electro. Сборка сохранена
готовой без установки; основной JAR остаётся `a0c4cb03…`.

Предыдущая пачка — [Psychowand и Psychoblind](PSYCHO_PORT.md): самостоятельный донор,
четыре режима Academy результата и полноценный крафт Blind + Psychowand.
Twitching использует обычные ЛКМ/ПКМ с коротким удержанием и отпусканием,
луч действует один раз на цель за тик, Vex добивания имеет 2 миллиарда HP.
Исходные состояния и значки actionbar, общий скример и владение призванными мобами
подключены к общим helpers. `26d1fccd…` прошёл build и три матча на изолированном
сервере: два новых и один регрессионный. Оставлен готовым без установки,
основной JAR по-прежнему `a0c4cb03…`. Проверки и статус JAR — в SERVER_TESTS.

Предыдущая пачка — [Blaze Wand и Fireblaze](FIRE_BLAZE_PORT.md): отдельный
донор, турель, Blaze Head, полноценный крафт Fire + Fallingstars, Academy
и четыре режима результата. Подтверждения автора: враждебные 20 ифритов,
14 огненных шаров испытания, глобальные режимы и столбы Y=0…319.
Тяжёлые преобразования используют общий BlockAreaWork с бюджетом CPU.
Также [Sun обновляет форму каждый тик](SUN_TICK_OPTIMIZATION.md), сохраняя
пятиковую оплату и боевую ауру; малые сдвиги используют подготовленный кэш.
Фактические прогоны и статус артефакта — в SERVER_TESTS.
Итоговый `fc46a1e9…` прошёл build и три серверных прогона на одном SHA,
всего шесть матчей; оставлен готовым без установки по выбору пользователя.
Основной JAR по-прежнему `a0c4cb03…`.

Предыдущая пачка — [Kiuaskivi и Sun Arcanum](FIRE_KIUAS_PORT.md):
автоматический самостоятельный Kiuaskivi, Sun с исходным блоковым рисунком
и полноценный крафт Fire + Kiuaskivi. Две независимые руки Kiuaskivi и урон
Sun только с двумя удерживаемыми Sun сохранены (user). У созданного огня,
лавы и прямого урона — настоящий носитель как автор. Обработка областей
переиспользует общий с Tanner `BlockAreaWork`; подготовка Sun-паттерна идёт
на существующем worker MapManager. Фактические прогоны и статус итоговой
сборки записаны в SERVER_TESTS. Основной сервер не изменяется без остановки.
Итоговый `e10835ed…` прошёл build и три нативных прогона: новая семья,
Stonekivi/Tanner и Fire. Готовый JAR включает исправление Tanner `f4c97247…`;
по выбору пользователя оставлен без установки, основной сервер остаётся на `a0c4cb03…`.

Следующая ограниченная семья — `stteleport` + `lightning_bolts` → `boltbolt_wand`:
сначала аудит самостоятельного донора и результата, уточнение поведения,
затем перенос и общий крафт. Реализовано 8/12 сочетаний, осталось четыре.
Не раскрывать рецепт без работающего результата и не добавлять сканеры
предметов на земле. Truefallingstars не равен fallingstars.

Исправление текущей пачки: Tannerkivi при движении ставит в очередь только
реальные новые полосы, объединённые по секциям, вместо нового полного обхода
31³ на каждый сдвиг. Преобразуются любые блоки вне пяти точных исключений
`air/cave_air/void_air/light/structure_void` (user). `f4c97247…` прошёл build,
движение/плотный куб и полный адресный прогон двух матчей; готов к установке
после остановки основного сервера. Подробности — в SERVER_TESTS.

Предыдущая пачка — [Stonekivi и Tannerkivi](STONEKIVI_PORT.md):
нативный крафт Stone + Tannerkivi, исходные Polymer-предметы, четыре режима
обычного/Academy результата, шесть заранее подготовленных NBT, порционная
магматификация Sun с чистым фоновым планом и автоматическая аура донора.
Кометы сохраняют предметы, снаряды и союзников, исключая служебные роли;
Tannerkivi при 500 маны исполняет первый импульс и выключается (user).
`a0c4cb03…` прошёл build, адресный прогон двух матчей и три связанные регрессии;
установлен при остановленном сервере с резервной копией. Сервер не запускался.
Доказательства и пределы — в SERVER_TESTS.

Fire + Kiuaskivi → Sun Arcanum выполнен текущей пачкой выше;
[первоначальный разбор](FIRE_KIUAS_NEXT.md) остаётся историей исследования.

Предыдущая пачка — [обычный и Academy Wall Wand](WALL_WAND_PORT.md):
четыре мгновенных кирпичных формы, полноценный Polymer item, испытание четырёх
различных преград, улучшение через общего NPC и поддержка обеих рук.
Возврат разборки ограничен 500, лестница создаётся один раз во всех направлениях,
переключение по горизонту идёт вперёд (user). `a628fc57…` прошёл build и четыре
серверных прогона, установлен при остановленном сервере; прежний JAR сохранён.
Основной сервер не запускался. Подробности и пределы проверки — в SERVER_TESTS.

Stone + Tannerkivi завершён текущей пачкой выше. Остальные сочетания —
по [матрице](WAND_CRAFT_NEXT.md); самостоятельный Wall завершает донора
рецепта Bedrock, а не ещё одно сочетание.

Предыдущая пачка — [обычный и Academy Bedrock Wand](BEDROCK_WAND_PORT.md):
полноценный крафт Stone + Wall, исходные предметы и пять режимов,
возврат BlockState/BlockEntity с послойным учётом пересечений комнат,
событийная регенерация и окончательная отмена управления маяком (user).
Отзыв пола и отложенное восстановление используют общий учёт автора убийства.
`863d710b…` прошёл build и адресный прогон двух матчей; Stone, Exoperso и
Academy прошли связанные регрессии. Установлен при остановленном сервере,
предыдущий JAR сохранён. Подробности и пределы проверки — в SERVER_TESTS.

Самостоятельная способность Wall Wand перенесена отдельной пачкой выше.

Предыдущая пачка — [обычный и Academy Exoperso](EXOPERSO_PORT.md):
подготовка, взрыв о блоки/существ, общий smart lifetime, расход 25000/10000
при допуске 5000 (user), пять кастов для Academy и отдельный учёт убийств
одной серии. Использует очередь Nuke с бюджетом CPU, без потери взрывов.
Build и шесть серверных сценариев прошли; итоговый `77ace205…` установлен
с резервной копией при остановленном сервере. Основной сервер не запускался.
Результаты и проверка идентичности production кода — в SERVER_TESTS.

Этап `ststone` + `wall` → `bedrock_wand` завершён выше.
[Остальные сочетания](WAND_CRAFT_NEXT.md) идут отдельными семьями.

Предыдущая пачка — [Air + Exoperso → Exasperato](AIR_EXASPERATO_CRAFT_PORT.md):
полноценный нативный рецепт 2×2/3×3, обычный/Academy/legacy Air,
расход по одному ингредиенту, исходный Polymer-результат и эффекты только
при взятии. Специальные комбинации получают приоритет перед ванильным
ремонтом через уже существующий mixin. `b3d287d8…` прошёл build и три
серверных сценария, включая выстрел скрафченным жезлом; установлен после
проверки world lock/порта, предыдущая сборка сохранена. Основной сервер
не запускался. Доказательства — в SERVER_TESTS.

Exoperso-донор теперь имеет собственную обычную/Academy способность выше;
рецепт принимает как нативные типы, так и прежний `exoperso:1`.
После Bedrock остаются 10 крафтовых продолжений матрицы с неперенесёнными результатами.

Предыдущая пачка — [обычный и Academy Ruby](RUBY_WAND_PORT.md): летящий бур,
сжатие в точку и сквозной шахтёрский луч, задание и tier B у общего NPC.
Сохранена медитация с притягиванием союзников (user), заряд использует общий
smart lifetime с целями и блоками (user). Общее разрушение клетки разделяется
с Tiny Ruby, вставка структур — со Stone. `e3e72640…` прошёл build и пять
серверных сценариев, установлен при свободном world lock и закрытом порту;
предыдущий JAR сохранён. Основной сервер не запускался. Подробности — в SERVER_TESTS.

Дальше — [крафтовые продолжения жезлов](WAND_CRAFT_NEXT.md), используя общий
нативный recipe/result для 2×2/3×3 и расход одного донора каждого типа.
Air + Exoperso → Exasperato и способность Exoperso уже реализованы.
После Bedrock оставшиеся 10 сочетаний требуют
отдельного переноса поведения результата. Полные Lucky-пулы/выдача, команды/
Bedwars и параллельные измерения остаются последующими отдельными этапами.

Предыдущая пачка — [обычный и Academy жезл телепорта `stteleport`](TELEPORT_WAND_PORT.md):
подбрасывание, инверсия с переносом слоя 3×1×3/контейнеров без дублирования (user),
облако на нативной стреле, медитация и улучшение через общего NPC.
Заряды используют общий targets-only smart lifetime без старых 200 тиков (user).
Ввод/мана, owner, shield/role guards и вставка BlockEntity переиспользуются.
`6b2f55bf…` прошёл build и адресный серверный прогон, связанные регрессии
подтверждены. Установлен после повторной проверки world lock/порта, предыдущий
JAR сохранён, основной сервер не запускался. Подробности — в SERVER_TESTS.
Следующий предметный этап — обычный/Academy Ruby, затем крафтовые продолжения
по source coverage; полноценный loot pipeline, команды/Bedwars и параллельные
измерения остаются отдельными этапами, описанными ниже.

Добавлен [обычный и Academy воздушный жезл `stair`](AIR_WAND_PORT.md):
одна обычная ракета, четыре в залпе, быстрая ракета с тремя TNT и самонаведение.
Используются общий smart lifetime с целями/твёрдыми блоками (user), native
столкновения, текущий owner и существующая защита/оптимизация TNT. Испытание
засчитывает реальный урон взрыва дальше 80 блоков от запуска (user), включая
смертельное попадание; отдельный поиск целей по радиусу для него удалён.
Навигация по готовым helpers закреплена в ARCHITECTURE и AGENTS.
`d196ae2a…` прошёл build и четыре серверных прогона; установлен после проверки
world lock/порта, предыдущий JAR сохранён, сервер автоматически не запускался.
Итоги проверок и установки — в SERVER_TESTS. Следующие предметные этапы —
обычные/Academy жезлы телепорта и рубина, затем крафтовые продолжения.

Добавлен [обычный и Academy жезл слепоты `stblind`](BLIND_WAND_PORT.md):
пять обычных зарядов и четыре Academy-режима, нативные swept-попадания,
слепота/иссушение с автором, готовность улучшения и tier B у общего NPC.
Дождь выпускает один заряд каждые два тика; очередь пулемёта складывается
и использует общий actionbar. Проверка включает следующий матч после stop.
Итоги сборки, регрессий и установки — в SERVER_TESTS.
`de7b5723…` установлен при свободной блокировке мира и закрытом порту;
четыре серверных прогона прошли, предыдущий JAR сохранён.
Воздух добавлен следующей пачкой выше; телепорт и рубин, затем оставшиеся
крафтовые продолжения по source coverage остаются в плане.

Добавлен [обычный и Academy каменный жезл `ststone`](STONE_WAND_PORT.md):
нативный заряд 1 блок/тик, срок 200 активных тиков, исходные структуры,
четыре режима, медитация с постоянным бедроком и шансом 20% (user).
Исправлена проверка задания во второй руке (user). Автор удушения теперь
учитывается для всех созданных ловушкой удушающих блоков, включая руду/обсидиан;
союзники защищены. Build и четыре серверных прогона прошли. `c9954703…`
установлен при остановленном сервере с проверкой блокировки мира и порта;
предыдущий JAR сохранён, сервер автоматически не запускался.
Следующий после камня жезл слепоты добавлен пачкой выше; воздух,
телепорт и рубин остаются следующими предметными этапами.

Добавлен [обычный и Academy огненный жезл `stfire`](FIRE_WAND_PORT.md):
исходный Polymer loot, три нативных шара, большой шар, концентрированный луч
с лавой в первом попадании, три режима и испытание у общего NPC Academy.
Сборка, адресная серверная проверка и регрессии Academy/Exasperato/химических
эффектов прошли. `a4367788…` установлен после штатной остановки основного
сервера и проверки world lock/порта; предыдущий JAR сохранён. Следующая каменная
пачка описана выше; крафтовые продолжения огня остаются отдельно.

Выполнен первый [перенос оптимизаций взрывов](EXPLOSION_OPTIMIZATION_PORT.md):
1352 готовых луча, постоянная сила (user), кэши клеток/чанков/сопротивления,
быстрый путь пустых секций. A/B одной сборки и native Exasperato-регрессия прошли.
`e1883379…` установлен после остановки основного сервера, предыдущая версия сохранена.
Форма/объём разрушений теперь могут отличаться; owner/команды и все 32 взрыва
Nuke сохраняются. Остался холодный срез 142 мс; следующий шаг — его профиль,
полная дорожка Electro Ray и артефакты. Moonrise в тесте обнаружил конфликт
unload/removal и не выбран; Lithium runtime не проверялся после смены направления.
Параллельные Overworld/Nether/End/arena остаются отдельной задачей:
[владение состоянием и барьеры](DIMENSION_THREADING_PLAN.md).

Новая пачка — [Shit Katana, крафтовое продолжение Cyberkatana](YELLOW_KATANA_PORT.md):
native Polymer item, правильный крафт 2×2/3×3, отдельные заряды, короткие
жёлтые разряды, земляная проводимость и вращающийся луч. Две спорные ветки
согласованы с автором: все враги в радиусе 15 у проводника; вращение сквозь
стены/магический щит. Использованы существующие сервисы и общие helpers
попадания/ловушек. Актуальные тесты и установка — в SERVER_TESTS.
`3eeb1456…` установлен после проверки world lock. Полный native прогон
и регрессии прошли за 67,88 с; типы урона, локализация/generated RP прошли.

Предыдущая пачка — [Academy: постройка, NPC и испытания](ACADEMY_PORT.md)
для Terra, Electro и Exasperato. Токен, слоёная постановка исходной структуры,
условия, флаги готовности и улучшение с сохранением компонентов подключены.
`fdefb2d7…` установлен после проверки остановки; native Academy — 39,98 с,
регрессия Exasperato — 42,28 с, локализация и generated pack прошли.
Exasperato считает 10 разных задетых целей одной серии (user), а не убийства
или присутствие рядом. Native-проверки и актуальная установка — в SERVER_TESTS.

Ещё ранее — [четыре режима Academy Exasperato](EXASPERATO_ACADEMY_PORT.md).
Решения пользователя: передние заряды 50/100 тиков и умный задний,
Chaos с заменой криперов на TNT, непрерывная дорожка Electro Ray,
Nuke без фиксированной дальности поиска, бюджет ядерной серии без лимита
очереди и потери ожидающих взрывов. Настоящий RP не менялся, ключи уже есть.
`stfire` с подключением к общему NPC добавлен следующей пачкой (статус выше),
затем идут остальные обычные/Academy-жезлы. Отдельно остаются выдача
токена через challenge kit/Lucky-пулы, challenge_score/`no_knowledge` и
ветка отмены улучшения Madoka: текущая цепочка трёх жезлов их не реализует;
отдельно — shader-вариант гриба и профиль 32 больших взрывов на плотной карте.
Нативный единичный взрыв пока неделим и может превысить бюджет одного тика.

Уточнение прав (user): общий GUI/карты/старт/quickstart подчиняются онлайн-тегу
`admin`; `/settings` остаётся только для OP 2+ и консоли. Служебные переключатели
`/settings event halloween|november|tournament true|false` сохраняют флаги.
Сами события, их сезонные награды/дропы и интеграция с косметикой пока в плане:
[SERVICE_EVENTS.md](SERVICE_EVENTS.md).
Проверенная сборка `11c88545…` установлена после остановки основного сервера;
серверные проверки прав, event-настроек и GUI прошли за 29,88 с.

Уточнение GUI/ranked от 16 октября: закрытые варианты активных страниц
сопоставлены с исходными replacement/modifiers, `/settings ranked` временно
снимает gates vanilla/LBC и не выдаёт открытия. Детали —
[источник замков](LOCKED_GUI_SOURCE.md), [ranked](RANKED_POLICY_SOURCE.md).
Build, UI и native skills прошли; предыдущий `ae5a9863…` был установлен в основной сервер
после освобождения world lock. [Проверки и manifest](../SERVER_TESTS.md).
После текущих предметных этапов нужны реальные косметические подстраницы
7100–7500, включая сезонные `7300_*`/`7310_*`: их особые замки не заменять
нынешними общими. Сейчас косметический hub есть, выбор этих категорий ещё
не перенесён. Пустая 11100 остаётся отдельным пробелом исходного inventory.
При переносе случайных Lucky Block-пулов проверить исходные ranked-условия
в `lucky_items`, `lucky_items_high_quality`, `traps`, `barriersnottraps` и
`loliluckyblock`; наличие JSON/score bridge не означает завершённость этих механик.

Актуальная пачка — [обычный Exasperato](EXASPERATO_PORT.md). Сохранены 85 TNT
и 50 молний, нативные владельцы/атрибуция, общий расход 1500 маны и новая
очередь `ProjectileLifetimes` для дальних Terra/Exasperato. Заряд Exasperato
сохраняется ради подходящей живой цели **или блоков впереди**, без фиксированного
срока; обзор блоков ограничен по CPU и не подгружает мир.
Build и 14 сценариев прошли на `3864f434…`; проверенный JAR подготовлен,
установка ожидает остановки основного сервера — он сейчас держит world lock.
[Manifest](../build/verification/exasperato-installation.json),
[проверки](../SERVER_TESTS.md). Ниже сохраняется история предыдущих пачек.

Актуальная пачка — [Electroblocker](ELECTROBLOCKER_PORT.md): настоящий Polymer
аксессуар, рецепт Electro + электрощит в обеих сетках, отражение/передача автора,
однократное ускорение своих снарядов и аура `ELECTRIC` по решению пользователя.
Build и четыре живых сценария прошли на `df36d73d…`; JAR установлен с резервной
копией `4622f512…`. [Установка](../build/verification/electroblocker-installation.json),
[проверки](../SERVER_TESTS.md). Предыдущие записи ниже относятся к истории сборок.

Пачка **четыре режима Academy Exasperato** выполнена; испытание/NPC остаются в плане.
Перед реализацией сверена цепочка всех четырёх режимов и ресурсы:
[повторная проверка исходника](EXASPERATO_ACADEMY_RECHECK.md),
[модель/оформление/переводы](EXASPERATO_ACADEMY_RESOURCES.md).
Решения пользователя по количествам Chaos/волнам Electro Ray,
дорожке и времени жизни зарядов получены и закреплены в AGENTS/порт-документе. Внешний
`lbcr:raycast_end_with_tp` найден; Nuke после столкновения удаляется через
`kill @s` общего payload. Не повторять прежние выводы об отсутствующем
helper или бесконечных взрывах ракеты. Java-перенос режимов выполнен.
Обычный Exasperato также работает; фактические исходные цепочки описаны:
[источник режимов](EXASPERATO_ACADEMY_SOURCE.md). Затем — крафтовый `shit_katana`
из Cyberkatana, а не предполагаемый отдельный upgraded Cyberkatana:
[точное различие](CYBERKATANA_UPGRADE_SOURCE.md).
Крафт Electro + щит создаёт Electroblocker; это отдельный аксессуар.
Использовать существующие projectile lifecycle, владельца/kill attribution,
`LbcMana.trySpendWand`, геометрию и подавление магии. Не переносить взрывы
без владельца или рекурсивное исполнение датапака.

Последняя пачка — [крафты Electro, киберкатана и энергетический щит](ELECTRO_GEAR_PORT.md).
Build и десять сценариев прошли на `4622f512…`; JAR установлен в основной
сервер с резервной копией `24afb5ca…`. Сервер не запускался.
[Установка](../build/verification/electro-gear-installation.json),
[проверки](../SERVER_TESTS.md). Три рецепта полноценные, 60% возврата сохранены;
заряд катаны обрабатывается по событию урона. Щит разворачивает и присваивает
себе снаряды каждый тик, включая свои и союзные, как подтвердил пользователь.

Предыдущая пачка — [обычный Electro, Academy и медитация](ELECTRO_FAMILY_PORT.md).
Build и девять сценариев новой семьи / Tiny Electro / электрического урона
прошли на `24afb5ca…`. JAR установлен в основной `../mods/skywars2.jar`
с резервной копией `71d62c93…`; сервер не запускался.
[Установка](../build/verification/electro-family-installation.json),
[проверки](../SERVER_TESTS.md). Получение флага испытания и зачёт через NPC
ещё не перенесены; медитация с уже выданным флагом работает.

Предыдущая пачка — [обычный Razorpine](PINEGUN_PORT.md): Polymer, растительные
боеприпасы, шесть залпов, нативные стрелы и согласованное удвоение за один
боеприпас при двух хвоемётах. Build и четыре suite (девять сценариев) прошли
на `71d62c93…`. Проверенный JAR установлен в основной `../mods/skywars2.jar`
16 октября 2026 в 12:29 МСК; прежний `5399fbad…` сохранён для отката.
Основной сервер после установки не запускался. Эта сборка включает предыдущие
исправления маны/Carpet, сундуков Iron Axe, дуги True Terra, панели атрибутов
и разделения карт по режимам. Компактный шрифт точки остаётся ручным обновлением
ресурспака. [Сборка и резервная копия](../build/verification/pinegun-installation.json).
[Результаты и ограничения](../SERVER_TESTS.md). Следующие ниже записи о
прежних установках относятся к соответствующим историческим артефактам.

Синхронизация Terra display исправлена и проверена: прогноз только картинки,
клиппинг на препятствиях, отдельное короткое завершение визуала после native
discard. Регрессии жезлов/мечей и нагрузочные окна до 300 снарядов прошли.
Подробности: [PROJECTILE_VISUAL_SYNC.md](PROJECTILE_VISUAL_SYNC.md).

Круглое сужение зоны реализовано через Polymer: радиус 384 → 4 за 4000 тиков,
урон напрямую в HP по расстоянию за кругом. Серверные bounds атрибутов и
скоростные проверки сняты глобально; collision и finite guards сохранены.
См. [реализацию и клиентские пределы](ZONE_AND_ENTITY_LIMITS_PORT.md),
[проверки](../SERVER_TESTS.md). Это не меняет дальнейшую очередь предметов.

Полный сброс атрибутов на старте до config/perks, при победе и отмене
реализован. Удаляются все transient/permanent modifiers, используются
native player bases. Два адресных прогона прошли на `c54bb314…`;
[готовый JAR и состояние установки](../SERVER_TESTS.md).

Панель атрибутов переделана по новому `(user)`-уточнению: один столбик,
20 неподвижных тиков, font shift и negative spaces. Build и адресный прогон
прошли; [UI-JAR](../build/verification/skywars2-attribute-column-e0ffd2cd.jar)
включён в новую сборку со сбросом атрибутов. Шрифты уже перенесены
пользователем в `lbc`, включая исправление внутренних TTF-ссылок.
[Состояние проверки](../SERVER_TESTS.md).

Завершён боевой перенос [Terra Sword и улучшенного варианта](TERRA_SWORD_PORT.md).
Два предмета Polymer, исходное оформление, три модели и TTL 40/20/50 работают
в общем `LbcTerra`. По ответам пользователя сохранены зарядка от любого
приписанного нестрелового урона и бесплатная дуга от попадания улучшенного
заряда. `--swords` прошёл три сценария; обычный/Academy Terra — ещё шесть
на том же `1ce3fa37…`. Также прошла регрессия Ice Sword; JAR установлен
с резервной копией, основной сервер не запускался. [Проверки](../SERVER_TESTS.md).
Следующая пачка реализована: [три нативных рецепта Terra и Razorterra](TERRA_CRAFTING_PORT.md).
Результат находится в выходном слоте 2×2/3×3; расход выполняет vanilla ResultSlot.
Улучшенный хвоемёт переиспользует `LbcTerra`, модель `lbc:terrasword_true_beam`,
блокировку щитом и выбранные пользователем 10 рабочих тиков (до 75 блоков).
Обычная стрельба Razorpine также перенесена: [PINEGUN_PORT.md](PINEGUN_PORT.md).
Из `transformables_shooter` подключена его конкретная ветка с полным loot
bundle; весь случайный пул остальных shooter-предметов пока не перенесён.

Завершена пачка пользовательских исправлений: Overload больше не возвращает
ману за щупальца; поддержанные статусы собраны в исходный actionbar;
панель итоговых атрибутов показывается после 20 неподвижных тиков
единым столбиком через font shift / negative spaces;
лобби перенесено к `(0.5, 200, 0.5)` с восстанавливаемой картой под ним;
кислота прилипает до прежнего TTL. После матча новая подготовка не запускается
автоматически. Native paste под лобби, BE, same-state/air, следующий матч,
observer HUD и GUI/quickstart прошли проверки: два + три матча на одном
`afb96b4e…`. Эта предыдущая пачка включена в новую сборку с мечами;
[отчёт и установка](../SERVER_TESTS.md).

Поведение [обычного Electro и улучшения Magic Academy](ELECTRO_FAMILY_PORT.md)
перенесено: три дуги, два режима Academy и медитация при начатом испытании.
Три связанных рецепта и обычные катана/щит также перенесены.
Ветка Electro + энергетический щит (`energyblock` / Electroblocker) перенесена.
Обычный Exasperato перенесён; его Academy-режимы и крафтовое продолжение катаны — следующие пачки.
Использовать готовые `LbcCraftingRecipe`, `LbcElectro`, электрический урон,
геометрию и авторство; не создавать отдельные системы для родственников предметов.
Затем — остальные обычные/улучшенные жезлы и получение улучшений через NPC
Magic Academy. При переносе испытания Electro уточнить несоединённую
source-проверку прогресса; медитацию не путать с завершённым улучшением.
Полный `transformables_shooter` требует переноса оставшихся
предметов пула перед включением случайной выдачи.
При появлении следующих крафтов использовать общий `LbcCraftingRecipe`.
Отдельный визуальный этап —
проверка панели атрибутов настоящим клиентом и согласование её размещения:
сейчас это native sidebar справа, не точный слой над окном инвентаря.
Командные режимы и исключения урона остаются следующим общим этапом ниже.

Исправлена [высота отвердения падающих блоков](ARTIFACT_LIQUIDS_PORT.md):
Y округляется после смещения +0,5. Build и адресный матч артефакта 1:3
прошли на `f877ba3d…`; исправление включено в новую сборку с мечами.

Исправлена задержка [очистки артефактами](CLEANSE_IMMEDIATE_REVIEW.md):
эффекты отклоняются до применения, прежние снимаются до native/custom тика.
Build, адресный матч артефактов и регрессии мановых зелий/невидимости прошли
на `67c7a14a…`. Исправление включено в новую сборку с мечами.

Дополнительная правка (user) — [полная невидимость III+](FULL_INVISIBILITY_PORT.md).
Build и [адресный матч](INVISIBILITY_CHECKS.md) прошли; JAR `05e56333…`
включает Academy Terra. Это предыдущая сборка; её изменения включены
в установленный JAR с мечами.

Предыдущая пачка: [Academy Terra](TERRA_ACADEMY_PORT.md) на общем `LbcTerra`,
исправленная скорость обычного и граница недоступного чанка. JAR `5cc324db…`
прошёл два матча и нагрузку 300 зарядов обоих вариантов. Изменения включены
в установленный JAR с мечами; benchmark относится к прежнему артефакту.
Прежние полные прогоны относятся к указанным историческим JAR.

- Обычный SkyWars содержит lobby GUI, немедленную асинхронную подготовку выбранной карты, match lifecycle и отдельную роль наблюдателя. Пройдены GUI/observer регрессии, навыки, LBC loadouts/items и полный цикл с респауном, ничьей, разрушенной картой и восстановлением после остановки. Контракт для новых предметов: [SPECTATORS.md](SPECTATORS.md).
- Импортированы 357 критериев `lbc:true_advancements/**`; их достижимость зависит от переноса связанных механик. 122 уведомления восстановлены через успешный native award; пять acidman требуют все десять исходных достижений. [README](../README.md), [LBC_LOADOUT_PORT.md](LBC_LOADOUT_PORT.md)
- Каталог содержит одиннадцать наборов: 1, 2, 4, 5, 7, 9, 10, 15, 17, 22, 23; навык 5; 30 улучшений. BrBad №17 добавляет четыре зелья Polymer и учтённые облака/кислоту; физика кислоты по уточнению пользователя нативная. Поведение и границы: [BRBAD_PORT.md](BRBAD_PORT.md). Rocket boots №22: [ROCKET_BOOTS_PORT.md](ROCKET_BOOTS_PORT.md), [исходный контракт](ROCKET_BOOTS_SOURCE.md). Актуальные прогоны — [SERVER_TESTS.md](../SERVER_TESTS.md). Предыдущая пачка: [ITEMS_BATCH_PORT.md](ITEMS_BATCH_PORT.md).
- Первый отдельный тип предмета Polymer — `skywars2:rocket_boots`; клиентский proxy сохраняет прежний вид кольчужных ботинок. Адресный прогон после изменения прошёл за 73,81 с: конвертация/восстановление стека, tooltip, native крафт и игровой lifecycle. Применять тот же подход к следующим предметам по мере переноса; общий собственный движок подмены не нужен.
- BrBad, Intercal/Gneiss и Blaze/Ghast ранее прошли свои адресные и полные прогоны. Snow Gem №18 и Ice Gem №20 добавлены через общий `LbcFrost`; их адресный прогон прошёл за 63,19 с, общий — за 192,59 с (девять матчей, 24 группы). Ice Sword теперь переиспользует общий холод и исходную область недавно раненых целей. Актуальные проверки и установка — [SERVER_TESTS.md](../SERVER_TESTS.md). Контракты — [TRINKETS_PORT.md](TRINKETS_PORT.md), [TRINKETS_PROJECTILE_PORT.md](TRINKETS_PROJECTILE_PORT.md), [FROST_PORT.md](FROST_PORT.md), [ICE_SWORD_PORT.md](ICE_SWORD_PORT.md).

## Приоритеты

Уточнения для будущего переноса Terra: [модели](TERRA_PROJECTILE_MODELS.md)
и [lifetime](PROJECTILE_LIFETIME.md). Обычный stterra получает `terra_charge`
без фиксированной дальности/TTL при возможности дальнейшего попадания;
Magic Academy Terra-жезл — `terrasword_true_charge`; Terra Sword —
`terrasword_charge`, улучшенный Sword — `terrasword_true_charge` на ПКМ и
`terrasword_true_beam` на дугу, улучшенный сосновый хвоемёт также использует
`terrasword_true_beam`. Заряды мечей остаются ограниченной дальности,
кислота временная. Не ограничивать полёт/цели bounds структуры: закартье и
большие координаты поддерживаются. Старый урон ±500 остаётся по настройке
`noBorders`; он не является условием lifetime. Модель обычного `stterra`
и Academy-жезла, а также обеих способностей мечей подключены в `LbcTerra`;
связь улучшенного хвоемёта также подключена в [общем обработчике](TERRA_CRAFTING_PORT.md).

Перед переносом обычной/улучшенной Terra использовать подтверждённую
пользователем нативную геометрию попадания и проверять нагрузку 200–300
снарядов. Tiny Terra уже переведена на один native many-hit запрос на каст;
визуальные samples сохранены. Политика и пределы выводов:
[PROJECTILE_PERFORMANCE.md](PROJECTILE_PERFORMANCE.md). Это самостоятельная
оптимизация нынешнего луча, не готовность ещё не перенесённых Terra.

Предыдущая пачка — **мановые зелья**: пять семейств, нативные питьё/бросок/облака
через Polymer, исходные таймеры 16–20 и общая очистка. В 15 таблицах сохранены
39 исходных записей, включая обычные варианты с короткими эффектами.
[Контракт и границы](MANA_POTIONS_PORT.md), [источники](MANA_ITEMS_SOURCE.md),
[сверка предметов](MANA_ITEMS_RESOURCES.md), [проверки](../SERVER_TESTS.md).

Предыдущая пачка — **ядро маны**: общий ресурс, регенерация, cap, временное
усиление/обратный поток, бонусы mage-брони/ремней и API расхода. HUD объединён
с ракетой; статичные значения не пересылаются каждый тик.
[Контракт и границы](MANA_PORT.md), [исходный regen](MANA_CORE_SOURCE.md),
[оформление HUD](MANA_UI_SOURCE.md), [проверки](../SERVER_TESTS.md).

Предыдущая пачка — **артефакт 1:7, 16 цветов**: прямые действия, XP, огонь,
усиленные водные эффекты и накопительный RTP-статус. Мана и счётчик еды пока
используют только общие scoreboard-ресурсы и сброс при старте; это не полный
перенос mana engine. [Контракт и границы](ARTIFACT_COLORS_PORT.md),
[исходная таблица](ARTIFACT_COLORS_EFFECTS.md), [проверки](../SERVER_TESTS.md).
Сохранены все 18 исходных block tags, модель, lore и переводимые компоненты.
Предыдущая пачка 1:3 остаётся сферой радиуса 16:
[ARTIFACT_LIQUIDS_PORT.md](ARTIFACT_LIQUIDS_PORT.md).

Сверка первичных файлов исправила прежний вывод о двойной прибавке:
обычный regen выполняется **один раз**. `maxmana` не найден в живых вызовах;
условный Nyauwu `true_pre` — отдельная броневая способность, не второй tick.

**Отрицательная мана, Crux и боевая перегрузка реализованы и установлены.**
Адресный Overload-прогон прошёл за 52,44 с (четыре уникальные группы, один матч)
на JAR `72b4ec22...`. Полный прогон прошёл за 311,16 с: 20 матчей,
56 завершённых сценариев / 60 уникальных check-групп. Визуальная сверка
настоящим клиентом ещё нужна. Контракт и статус: [OVERLOAD_PORT.md](OVERLOAD_PORT.md) и
[SERVER_TESTS.md](../SERVER_TESTS.md). Исходные зависимости:
[аудит перегрузки](OVERLOAD_COMBAT_SOURCE.md),
[пассивы брони](OVERLOAD_ARMOR_SOURCE.md),
[ресурсы](OVERLOAD_RESOURCES_SOURCE.md),
[общие mana-зависимости](ARTIFACT_COLORS_DEPENDENCIES.md).

Начат этап реальных wand-потребителей: Tiny Fire/Tiny Blind, cast-бонус
mage-комплекта, Mana Gem/Blood Anal Boom и личный таймер `nomagic`.
Эта первая пачка проверена и установлена: 21 матч, 63 уникальные check-группы;
кнопка второй руки в настройках подключена с исходным default «выключено».
[Контракт и статус первой пачки](WANDS_FIRST_PORT.md).
Предыдущая пачка — Tiny Air/Stone/Teleport в общем обработчике луча, расхода
маны и cast-бонусов: [контракт](WANDS_SECOND_PORT.md). По уточнению пользователя
луч завершает действие при первом попадании ровно один раз; исходное
независимое выполнение entity/wall исправлено, включая Tiny Blind.
Пачка проверена и установлена: 22 матча / 66 уникальных check-групп.
Результат живого прогона и установки — [SERVER_TESTS.md](../SERVER_TESTS.md).
Предыдущая пачка — [Tiny Ruby/Terra](WANDS_THIRD_PORT.md). Пользователь подтвердил
Ruby как сквозное разрушение любых блоков, Terra — как пробивающий луч
с одним ударом 5 по каждой цели за каст. Это явные исключения к завершению
на первой сущности у предыдущих малых жезлов. Источник и решения:
[WANDS_THIRD_SOURCE.md](WANDS_THIRD_SOURCE.md). Крафты, описанные в их lore,
существуют отдельно в исходнике и пока не входят в перенос ПКМ-способностей.
Пачка установлена: полная регрессия 69 групп / 23 матча до последней узкой
правки дропа; итоговое единое поведение жезлов в Creative/developer проверено
отдельным адресным прогоном. [Доказательства](../SERVER_TESTS.md).
Четвёртая пачка — [Tiny Acid](WANDS_FOURTH_PORT.md): исходный залп из шести
кислотных снарядов через существующий `LbcClouds`, без второго движка физики.
Проверена и установлена: адресно 55,48 с, полностью 365,60 с,
24 матча / 72 уникальные check-группы на одном JAR. [Результаты](../SERVER_TESTS.md).
Предыдущая пачка — [Tiny Electro](WANDS_ELECTRO_PORT.md): короткий отложенный
луч с исходным случайным преломлением, вода/твёрдые проводники, shield,
нативные хитбоксы и отдельный урон по длине контакта. Адресный прогон прошёл:
55,24 с, один матч, три Electro-проверки плюс общий bot check; отдельно
подтверждён обрыв на границе щита с сохранением предшествующих эффектов.
Полная регрессия прошла на том же JAR: 388,65 с, 25 матчей / 75 check-групп.
Эта проверенная пачка включена в установленный JAR с общим электрическим уроном;
пользователь остановил основной сервер и разрешил установку. Фактические
результаты и установка: [SERVER_TESTS.md](../SERVER_TESTS.md).
Проводящий импульс один на снаряд за тик и складывается между снарядами;
по уточнению пользователя поражает владельца/союзников, прямой луч их защищает.
Electro ограничен по дальности (user); Tiny сохраняет 40 рабочих тиков.
Редкая зависимость Yellow Lightning / `electropissardy`, Tesla advancement
и крафты из lore пока не перенесены: [границы пачки](WANDS_ELECTRO_PORT.md).

Текущая пачка — [общий электрический урон](ELECTRIC_DAMAGE_PORT.md).
Базовый тип и оба Electro-типа используют единые правила `LbcDamage`:
native cooldown/no-knockback tags, +25% raw damage за обычную металлическую
часть игрока, исключение кастомных сетов и иммунитет полного смешанного
Techno/MK2. Ванильная защита сохранена; проводимость остаётся единственным
заданным исключением к защите союзников. Build и адресный прогон прошли
(51,41 с, один матч); полная регрессия прошла 26 матчей / 78 check-групп
за 382,70 с. Проверенный JAR установлен, сервер оставлен остановленным.
При следующих переносах
электричества использовать этот тип/категорию, не копировать формулу в предметы.

Перед следующей пачкой исправлена общая заморозка по `(user)`-уточнению:
центр X/Z `floor + 0.5`, оболочка по хитбоксу с минимумом один блок,
запрет активных ЛКМ/ПКМ при доступном инвентаре. Адресный прогон и полная
регрессия 26 матчей / 78 групп прошли; проверенный JAR установлен.
[Контракт](FROST_PORT.md), [проверки](FROZEN_GEOMETRY_CHECKS.md).

Также исправлен [АирДроп](AIRDROP_PORT.md): десять куриц несут светящиеся
вагонетки, служебный count становится количеством предметов. Подготовка
чанков переиспользует асинхронный загрузчик карты, обе сущности — существующую
эпоху предметов. Адресный живой прогон с обычными сундуками и stop cleanup
прошёл; проверенный JAR установлен.

[Обычный `stterra`](TERRA_REGULAR_PORT.md) перенесён: native swept hitbox,
Polymer display `lbc:terra_charge`, исходная ориентация Chaos Sword и
распределённая проверка возможных целей. Адресная проверка подтвердила полёт
до цели в ~940 блоках, FULL вне entity-ticking, бой на X≈32000, удаление
промаха, остановку перед недоступным промежуточным чанком и cleanup.
Нагрузка проверяется отдельно: [сценарии и результаты](TERRA_REGULAR_CHECKS.md).

[Academy Terra](TERRA_ACADEMY_PORT.md) использует тот же полёт с
`lbc:terrasword_true_charge`, скоростью 7,5, уроном 17 и цветами на стене.
По новому ответу `(user)` старый TTL 20 отменён: оба жезла используют умное
удаление. У обычного исправлена скорость 2,1 → 1,8 по фактической рекурсии.
Адресный прогон двух матчей прошёл; [проверки](TERRA_ACADEMY_CHECKS.md).

Боевые способности обоих Terra Sword перенесены в существующий `LbcTerra`,
проверены и описаны в [TERRA_SWORD_PORT.md](TERRA_SWORD_PORT.md).
Три рецепта семейства и улучшенный хвоемёт с beam реализованы:
[TERRA_CRAFTING_PORT.md](TERRA_CRAFTING_PORT.md). Обычный Razorpine тоже
реализован отдельно в `LbcShooters`: [PINEGUN_PORT.md](PINEGUN_PORT.md).
Его боеприпасы/серии стрел не смешаны с бесплатным улучшенным вариантом.
Получение предложения улучшения/NPC Magic Academy для трёх перенесённых
семейств теперь подключено: [ACADEMY_PORT.md](ACADEMY_PORT.md).
Остальные семейства и общие challenge-награды остаются отдельной задачей.
Затем — остальные обычные/улучшенные жезлы и источники
`nomagic` (мечи, еда, случайные дебаффы), с проверкой их условий и стоимости.
Подозрительные исходные ветки сначала согласовывать с пользователем.
После неё отдельно переносить пассивы брони Nyauwu; затем — семейства артефактов
2/3/4 и незавершённый Lucky/gameMode 2. Случайная Lucky-выдача/перековка,
посохи и ядерные таймеры остаются отдельными пачками. Team-режим требует
реального назначения команд; текущие способности соблюдают нативные союзы,
но не создают BedWars/team engine.

1. **Сохранять regression baseline** при следующих переносах. Для каждой новой способности проверять активного игрока, наблюдателя, смерть/респаун и очистку при остановке. Готовность определяется соответствующими живыми сценариями, а не наличием класса или loot table.
2. **Rocket boots №22 перенесены и проверены.** Native glide-start, три заряда с интервалом восстановления 301 тик и observer gate проверены на сервере. Перенос расходует одну пару-донор и исключает повторное улучшение. По уточнению пользователя сетка заменена нативным рецептом с выходным слотом в 2×2/3×3; Shift-клик и заполненный инвентарь прошли адресный сценарий за 82,74 с. Визуальная сверка на настоящем клиенте ещё нужна. [ROCKET_BOOTS_PORT.md](ROCKET_BOOTS_PORT.md), [исходный контракт и пользовательское исправление крафта](ROCKET_BOOTS_SOURCE.md).
3. **BrBad №17 реализован отдельной пачкой.** `LbcClouds` подключён к существующему lifecycle `LbcItems`, нативному броску и очистке. Термит сохраняет каскад конечных блоков; кислота использует нативную физику и столкновения по `(user)`-уточнению. Статус проверки и установку фиксировать только по результатам [SERVER_TESTS.md](../SERVER_TESTS.md). Общий собственный raycast-движок не создавался. Другие зависимости: [LBC_CONSUMABLE_NEXT.md](LBC_CONSUMABLE_NEXT.md), [LBC_PASSIVE_NEXT.md](LBC_PASSIVE_NEXT.md), [INTEGRATION_REVIEW.md](INTEGRATION_REVIEW.md).
4. **Подавление магии, раскрытие и пассивы артефакта.** Маяк — настоящий блок Polymer со сферой радиусом 20. Событийный снимок учитывает курсор/крафт и типы 1–8. Looper показывает описание только получателю с перком. Тип 8 поддерживает метаболизм с исправленным лечением, тип 5 снимает отрицательные эффекты, тип 6 также очищает кастомные статусы через общий с зельем обработчик. Типы 4/2/3 используют общий пространственный перехватчик стрел/unstable/падающих блоков; жидкости 1:3 обрабатываются событийной сферой. Цветовые ветки 1:7 описаны выше. [Актуальные проверки](../SERVER_TESTS.md), [цвета](ARTIFACT_COLORS_PORT.md), [отвердение](ARTIFACT_LIQUIDS_PORT.md), [защита от взрывоопасных сущностей и границы ядерной ветки](ARTIFACT_EXPLOSIONS_PORT.md), [защита от стрел](ARTIFACT_ARROWS_PORT.md), [очистка](ARTIFACT_DEBUFF_PORT.md), [метаболизм](ARTIFACT_PASSIVES_PORT.md), [маяк](MAGIC_PORT.md), [Looper](LOOPER_PORT.md). Мечи/посохи, полный mana/cast и общая случайная Lucky-выдача/перековка остаются отдельными этапами.
5. **Сверить оставшиеся settings и prefix UI.** Native `GameConfig` уже покрывает текущие настройки матча, но полный legacy selector не перенесён. В source prefix варианты 3/4 дают пустое хранилище, а заполненные списки подключены у 0/1/2. [skywars_gui.md](skywars_gui.md)
6. **Режимы 2–4 остаются отдельным незавершённым этапом.** `GameConfig` допускает значения 1–4, но `GameController.canStart` сейчас разрешает только mode 1; LBC mode 2 и BedWars modes 3–4 требуют своих игровых циклов и интеграции. [GameConfig.java](../src/main/java/me/skywars2/game/GameConfig.java), [GameController.java](../src/main/java/me/skywars2/game/GameController.java)

Приоритет CPU и событийной обработки задан в [AGENTS.md](../AGENTS.md). Не отмечать этап готовым без соответствующего runtime evidence в [SERVER_TESTS.md](../SERVER_TESTS.md).

## Будущий этап: Skywars Teams и защита команд (user)

> кстати, добавь 2 новых режима ещё - Skywars Teams и Skywars Teams with Lucky Blocks, а так же настройку skywarsTeamPlayerCount и пусть она так же будет в настройках как кнопка, чтоб в ней можно было переключать количество игроков в клетке (от 2 до 4), и чтоб эта кнопка была в меню настроек только когда выбран teams режим. Там так же нужна поддержка команд (кнопка выбора команды), и надо чтоб все предметы с аурой действия/самонаводкой/массовым срабатыванием/да и обычные атаки и снаряды не наносили вреда тиммейтам (кроме некоторых исключений, про них я потом буду уточнять, пока таких нет). Такая поддержка команд так же нужна и в бедварсе, чтоб команде нельзя было вредить, и так же чтобы на жителя-магазин нельзя было воздействовать, и чтобы игровые предметы не видели, не наводились на него, не срабатывали с ег опозиции и т д.
>
> Можешь пока просто добавить это в дальнейший план, и продолжать текущий перенос.

**Статус: запланировано; сейчас продолжается перенос жезлов.** Новые режимы
не добавлены в активный конфиг/GUI и не объявляются доступными для старта.

- Добавить `Skywars Teams` и `Skywars Teams with Lucky Blocks` с сохранением
  существующих значений режимов 1–4. Lucky Teams использует тот же общий
  командный цикл и Lucky-пулы, а не копию контроллера матча.
- Добавить сохраняемую настройку `skywarsTeamPlayerCount`: 2, 3, 4 игрока
  в клетке, переключаемые одной кнопкой. Кнопка видна только в двух Skywars
  Teams режимах; хранение настройки не должно менять одиночные режимы.
- Кнопка выбора команды доступна в Skywars Teams/Lucky Teams и BedWars.
  Общая модель членства обслуживает выбор, заполнение, старт, совместный
  спавн в клетке, переподключение, наблюдателей, победу команды и сброс матча.
  Проверить вместимость карт, пустые/неполные команды, переполнение клетки
  и удаление всей клетки после общего отсчёта.
- Ввести общий серверный контракт вредоносной цели и авторства воздействия:
  обычный melee, снаряды и их косвенный урон, ауры, самонаводка, массовые
  способности, дебаффы, кастомные статусы, отбрасывание и потеря ресурсов
  не вредят союзникам. Проверять при выборе цели и перед применением,
  чтобы смена команды/владельца не оставляла обходов. Сохранять полезные
  взаимодействия с союзниками. Первое явное исключение (user) — проводящий
  импульс Electro: он поражает также владельца/союзников. Прямой Electro bolt
  под исключение не попадает. Общий фильтр учитывает `LbcDamage.isConductionDamage`,
  сохраняя роль наблюдателя/магазина. Остальные исключения добавлять только
  по последующим уточнениям пользователя.
- Переиспользовать этот контракт в обоих BedWars режимах. Житель-магазин
  имеет отдельную серверную роль, устойчивую к сохранению/перезагрузке:
  его нельзя повредить, изменить эффектами/силами/ресурсами, выбрать целью
  самонаведения или считать источником/центром срабатывания предметов.
  Имя сущности и общий тип `Villager` не заменяют эту роль.
- Перед включением режимов провести аудит всех уже перенесённых предметов:
  прямые/косвенные владельцы, область действия, триггеры попадания/смерти,
  вторичные цепочки, окружение/огонь/взрывы и цели без автора. Проверки:
  союзник/враг/наблюдатель/житель магазина, снаряд после смены команды,
  смена матча, переподключение и очистка. Не сканировать все сущности мира
  для проверки команд: использовать членство и пространственные запросы.

Нативные союзы, уже проверяемые частью предметов, ещё не означают полного
командного движка или полной защиты: текущий Overload ray и прочие старые
ветки отдельно войдут в этот аудит. Полный перенос команд не смешивается
с текущей пачкой жезлов и получает отдельное runtime evidence.
