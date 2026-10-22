# Lightning Bolts и Boltbolt — 19 октября 2026

## 21 October regression fixes - QUALIFIED, ready without installation

This section supersedes the historical implementation below where it differs.
User approved five ordinary charges / 200 active ticks, with one strike per charge
every 40 ticks instead of20. The initial terminal strike remains separate. This
produces25 repeated strikes per cast; Field of Lightning retains its independent
20-tick interval. Legacy already randomly retargets within 25 if an enemy is within 15,
otherwise approaches the nearest enemy by 0.5; the donor's Chain mode is separate.
[Focused legacy comparison](BOLTBOLT_REGRESSION_SOURCE.md).

Wind Racer keeps the native mounted arrow at speed 2 with gaze steering before
physics. Its per-tick steering now requests both native position and velocity
synchronization. Vanilla Arrow's 20-tick tracking otherwise kept old motion/drag
on the client until a once-per-second correction. Fixed-direction Teleport cloud
is unchanged. No new vehicle implementation or packet engine was introduced.

North Pole now has a COMPLETE native transform: scale-only NBT silently decoded
to identity, making the old native visual three times too small. Model remains
lbc:big_snowball, scale 3, pose[-90,-90], interpolation/teleport 2. First two ticks
initialize; then fall callbacks every 2 ticks replace, rather than sum, relative
offsets .5/1/1.5/2/2.5/3 at legacy lifetime thresholds. Spin stays 6 degrees/callback.
The lower centerline sweeps a 4.5-block body radius (derived from the unrotated
model's -16..32-pixel extent at scale 3) to ground, including exact endpoint contact.
This is a practical spherical-body approximation, not rotated mesh collision.
The contact visual survives 2 ticks for client interpolation; impact damage/cold
and budgeted snow/crater work execute only once. Owner/epoch/cleanup remain shared.

Build and two native matches passed on fe456346 in 48.07 seconds. The checks capture
actual native ServerEntity movement/velocity packets over 45 consecutive arrow ticks,
including turns, count 25 actual strikes over 200 ticks, serialize the display to
verify scale/model/pose, verify all initial motion phases, and assert ground contact,
interpolation retention and single impact. Existing crafting, mana, Academy,
Watering terrain and real match-stop cleanup pass. Client graphics were not tested.
Archive: build/verification/skywars2-boltbolt-fix-fe456346.jar. Evidence:
build/verification/boltbolt-regression-final/. Source RP/template unchanged;
main JAR remains 332415d6 and was not installed/stopped by this batch.


Переносится семья `stteleport + lightning_bolts → boltbolt_wand`.
[Исходник и решения пользователя](BOLTBOLT_SOURCE.md),
[оформление предметов](BOLTBOLT_RESOURCES.md),
[зависимости перековки](REFORGE_RESOURCES.md).

## Реализация

Донор Lightning Bolts: нативные молнии, испытание четырёх отдельных
трансформируемых предметов, улучшение через общего NPC. Academy добавляет
Reforge без допуска/списания маны (user) и повторную цепь каждые 40 тиков
(user). Цепь очищается общим очистителем; артефакт отклоняет новый статус
до применения, переход в другое измерение исключает цель из прежней цепи.

Boltbolt: исходные пять смещений зарядов, наведение на врагов, shock +10,
гроза, испытание накопления 60 shock. Срок зарядов — 200 тиков (user).
Academy: обычный режим, Wind Racer, Snow Meteor, Watering. Meteor использует
исходную высоту +50, движение каждые два тика и общий холод; преобразование
снега выполняется порциями. Watering сохраняет пять блуждающих источников
на десять секунд и исходные эффекты на 240 секунд.

Wind Racer переиспользует облако Teleport на нативной стреле: скорость 2,
управление взглядом до физики, отсутствие искусственного TTL, четыре
нижних ElectroTrace с исходными сроками 30/35/40/45 и шансом 30% за тик.
Owner, защита союзников/наблюдателей/NPC, epoch и cleanup общие.

Рецепт — настоящий native shapeless 2×2/3×3, ровно по одному донору.
Принимает обычные, Academy и legacy варианты. Результат появляется в
ячейке крафта; эффекты вызываются только при взятии результата.

Уточнение 20 октября: при переносе [Infbolt](INFBOLT_PORT.md) найдена старая
reduce_count_s-рекурсия обычного Reforge. Оба инструмента теперь обрабатывают
весь стек через одну бюджетную очередь, без повторного захвата своих результатов.
Пустая выдача сохраняет остаток входа (user). Это исправляет прежний перенос
одного предмета за активацию; исторический прогон d1f1d2f9 ниже проверял его.

Reforge использует исходный полный каталог категорий через общий rollTable.
Это включает предметы с ещё не перенесёнными способностями: сохранение
их оформления и выдачи не означает реализацию всех механик каталога.
Настоящий ресурс-пак и template не изменяются; существующие ключи
и модели переиспользуются.

## Проверка

Build прошёл. Итоговый `d1f1d2f9…` проверен двумя адресными матчами
(52,50 с) и отдельной регрессией Teleport/Electro/Exahoming (39,00 с).
Проверены реальные снег и вода, 179 исходных блоков powder_snow, урон/холод,
расход, цепь, сроки, Academy, крафт и cleanup. Полный протокол и ограничения —
в [SERVER_TESTS.md](../SERVER_TESTS.md).

JAR сохранён как `build/verification/skywars2-boltbolt-d1f1d2f9.jar`,
без установки; основной сервер остаётся на `a0c4cb03…`.
Исходные loot payload и NBT сверены полностью; Java совпадает с sources JAR.
Настоящий клиент и массовая нагрузка не проверялись.

Закрыто 9/12 крафтовых продолжений. Следом: самостоятельный `bolt_material`
и Infbolt, затем два продолжения Ruby — Consuming и Duplicating. Для Infbolt
уже доступны общий rollTable, ресурсы перековки и native рецепт; новые
параллельные обработчики для них не нужны.
