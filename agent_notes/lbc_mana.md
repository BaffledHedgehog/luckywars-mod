# Мана: регенерация, обратный поток и перегрузка

Все числа здесь — собственные scoreboard units. Панель UI показывает `mana`,
`manamax` и `manaregen_visual` напрямую; деления на 100 в панели нет.
`manaregen` — база, а `manaregen_visual` пересчитывается при обслуживании игрока
и используется как фактический шаг регенерации. Основные исходники находятся в
`LBC/data/lbc/function/other/manaregen*.mcfunction` и `negative_mana*.mcfunction`;
лучи перегрузки — во вложенном `other/negative_mana/`.

## Обычный проход: `manaregen`

В [players_tick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction)
цикл выполняется при `legacy != 1`, отсутствии положительного `nomagic` и
отсутствии marker `stopper_magic` в радиусе 20. Родитель выбирает игрока вне
spectator и без тега `time_stopped`; обычная tick-цепочка исключает Nexus/Imprinted.
Поэтому прибавку нельзя объявлять безусловной для каждого игрока каждую секунду.
В штатной цепочке это шаг на обычный обслуживаемый tick.

[manaregen](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen.mcfunction)
выполняет действия в следующем порядке:

1. Если mana <= 0 и нет `obtained_crux`, вызывает `overload_crux/give`: выбрасывает
   loot `lbc:overload_crux`, 10 dragon breath particles, звук elder guardian
   curse для самого игрока и ставит tag. Это выброшенный item, а не прямая
   запись в инвентарь. Tag снимается, когда mana >= положительного manamax;
   после нового падения маны снова возможна выдача.
2. Копирует базовый manaregen в visual. При mana belt добавляет бонус за количество
   предметов; при наличии хотя бы одной части `mage:1` вызывает броневой helper.
3. При положительном `effect_boost_mana` умножает visual на 3, делит на 2
   целочисленно и уменьшает этот счётчик на 1. Grant `mana_temp` происходит,
   если разница visual до/после boost составляет не меньше +15.
4. При `reverse_mana >= 1` вычитает visual из mana. При положительном visual
   и mana ниже manamax вызывает обычное восстановление; отрицательный visual
   вызывает его независимо от сравнения с максимумом.
5. Если после действий mana < 0, вызывает проверки отрицательной маны.

## Регенерация, экипировка и обратная мана

| Функция | Фактическое действие |
|---|---|
| `manaregen_mana_belts` | `clear ... 0` считает количество предметов с `manabelt:1`, не удаляя их. Удвоенное количество добавляет к visual; при этом бонусе больше базового manaregen выдаёт mana-belt advancement. |
| `manaregen_mage_armor` | Части `mage:1`: голова +4, грудь +6, ноги +7, ступни +4 к visual, максимум +21 от этих четырёх проверок. Далее выполняет Nyauwu-ветки ниже и `speed_up`. |
| `manaregen_mana_boost` | Один проход boost: `visual = (visual × 3) / 2` с целочисленным делением, счётчик boost −1 и advancement magic buff. Базовый manaregen не умножается. |
| `manaregen_true` | Добавляет visual к mana **только при существующем reverse_mana <= 0**. После этого отдельно ограничивает mana сверху значением manamax, если mana > manamax. Нижней границы не ставит. |
| `manaregen_true_pre` | Сначала +1 mana, затем добавляет visual при reverse_mana <= 0. Верхнего ограничения в этом helper нет. |
| `manaregen_hit` | Умножает текущий visual на 70 и вызывает `manaregen_true`. Вход из sword hook требует Nyauwu chestplate и отсутствия stopper_magic в 20. Это множитель **70**, а не 70%; отдельно baseline здесь не пересчитывается. |
| `manaregen_belt_stacked` | Рекурсивно +1 mana, −1 `manabelts`; повтор пока остаток >=1. В трёх пакетах найден только self-call: автоматическая связь с обычным belt helper не подтверждена. Ручной вызов всё равно выполняет первую прибавку. |
| `manaregen_inwater` | Пустой файл, действий нет. |

Наличие score важно: отсутствие `reverse_mana` не удовлетворяет selector
`scores={reverse_mana=..0}`. Обычная инициализация/reset должны установить нужные
scores; ручной вызов на неинициализированном игроке может не выполнить прибавку.
Обратная мана уменьшается на 1 в `players_1s` при положительном значении.
При обратном потоке обычная прибавка заблокирована, но вычитание visual уже
произошло; если visual отрицателен, такое вычитание прибавляет mana.

**Nyauwu**, внутри броневого helper, который родитель вызывает при любой `mage:1`:

- Голова `nyauwu:1` при другом живом игроке в 0,01..10 добавляет **20 к manamax
  за каждый выполненный проход**, а не назначает одноразовый modifier +20.
- Ноги `nyauwu:1` удваивают уже собранный visual, если в 0,01..10 нет сущности
  `#minecraft:mobs` без spectator-тега.
- Грудь `nyauwu:1` при mana >= manamax и блоке `#minecraft:water` вызывает
  `manaregen_true_pre` до boost и основного cap. При положительном visual после
  этого mana может остаться выше максимума: основная ветка вызывается только
  если mana ниже максимума. Другие пути могут потом ограничить значение.
- `speed_up` при Nyauwu boots и mana < manamax пытается добавить movement-speed
  modifier `916c89e6-ae62-4b11-91d7-10e7e57c726d`, amount 1, add_multiplied_total.
  При mana >= manamax снимает его независимо от boots. Этот helper не содержит
  отдельной очистки только по факту снятия boots; общие reset/equip ветки
  рассматриваются в разделе брони.

## Отрицательная мана: `negative_mana`

Диспетчер вызывается после регенерации при отрицательном mana. При отсутствии
`overload_helmet_active` выбирает ровно одну строку таблицы. Если helmet-tag есть,
обычные пять строк пропускаются; при mana <= −20001 запускается отдельная
повреждающая helmet-ветка, а при −20000..−1 эти debuffs не выдаются.

Все перечисленные `/effect give` длительности — секунды, amplifier сырой:

| Mana; helper | Эффекты одного выполненного прохода |
|---|---|
| −5000..−1; `negative_mana_1` | Slowness на 1 с, amp 1 (II). |
| −10000..−5001; `_2` | Slowness 1 с amp 2, Mining Fatigue 1 с amp 0, Weakness 1 с amp 0. Nausea в файле закомментирована. |
| −15000..−10001; `_3` | Slowness 1 с amp 6, Mining Fatigue 1 с amp 2, Nausea 4 с amp 0, Weakness 1 с amp 1, Hunger 1 с amp 0. |
| −20000..−15001; `_4` | Slowness/Mining Fatigue/Weakness на 1 с amp 127; Nausea 6 с amp 0; Hunger 1 с amp 2. |
| <= −20001; `_5` | Те же три amp 127 и Nausea 6 с; дополнительно Blindness 2 с amp 0, Hunger 1 с amp 6, затем проверка урона/взрыва. |

`negative_mana_resist` независимо добавляет владельцу `overload_chestplate_active`
Resistance на 1 с: amp 0/1/2/3/4 по тем же пяти диапазонам соответственно, скрытые
частицы. Это не отменяет сам вызов проверки урона. При одновременных отрицательных
mana, visual и manamax выдаётся advancement `oh_no_mana`.
Сами пороговые helpers не уменьшают mana и не очищают ранее выданные эффекты;
их поддерживает повторный tick, а время ванильного эффекта убывает в движке.

## Предельная перегрузка: `_5`, `_5_helmet`, `damage_with_tmp_void`

Пусть H — целое значение команды `attribute max_health get`, а R — результат
целочисленного деления mana/manamax **с округлением вниз (floorDiv)**; например,
−20001/1000 = −21. Обычный `_5` записывает `D = (H − R) × 0,1`,
helmet-вариант — `D = (H − R) × 0,045`. Это точная цепочка scoreboard → storage;
кроме самого D для сравнения снова читается целый command result из storage.
При manamax=0 деление не имеет корректного результата; код не содержит guard
от нулевого знаменателя, поэтому формулу для такого состояния применять нельзя.

Обычный вариант сравнивает целый результат D с локально считанным `Health` NBT
в holder `hplvl lbc.math`. Helmet-вариант сравнивает его с **player score `hplvl`**
(objective создан как health) и локально Health не считывает. Эти источники
сравнения нельзя считать одной и той же переменной.
При пороге >= health запускается `negative_mana/explode`; иначе вызывается
macro `damage_with_tmp_void` с исходным дробным D.

`damage_with_tmp_void` сначала выпускает обычную tentacle либо её full-set
вариант по `overload_full_active`. Затем наносит самому игроку D урона
`out_of_world`, если нет `overload_slam`. Наличие этого тега отменяет только
последний damage, не предшествующий выпуск tentacle.

`explode` выпускает **6 обычных и 15 больших tentacles**, играет impulse
для игроков в 40 блоках (volume 2, pitch 1), затем `kill @s`. Это явная смерть,
а не призыв TNT. Сама функция не проверяет, был ли caller helmet/full-set.

## Щупальца: направление, длина, урон и восстановление

Это синхронные рекурсивные проходы функциями, а не созданные projectile entities.
Каждая точка рисует dragon breath и проверяет цель; урон указан на попытку,
без вывода о суммарном DPS. Командных фильтров команды/владельческого ID нет:
исключаются `spectator` и текущие `caster` tags, поэтому союзники не защищены
проверкой `team_number` в этих helpers.

| Семейство | Поворот и шаг | Длина исполнения | Damage |
|---|---|---|---|
| `tentacle` → `tentacle_2` → `tentacle_2_1` | Случайные относительные yaw ±180° / pitch ±40°; последующий изгиб на фиксированные для луча yaw ±6° / pitch ±4° за шаг, движение 0,3. | Distance начинает с 0, увеличивается перед проверкой; максимум 31 точка, до 30 перемещений (9 блоков пути). Продолжение требует `#minecraft:airs` в новой точке. | `−mana × 0,00006`, double; выбирает максимум одну цель в кубе dx=dy=dz=0 **и** distance<=3. |
| `tentacle_full_set` → `tentacle_full_set_2` → тот же `tentacle_2_1` | Начальные относительные yaw/pitch ±10°; изгиб ±5°/±3°, шаг 0,3. | Distance начинает с −30, поэтому максимум 61 точка / 60 перемещений (18 блоков пути), с той же проверкой воздуха. | `−mana × 0,0001`, double; тот же фильтр цели. |
| `tentacle_big` → `tentacle_big_2` → `tentacle_big_2_1` | Случайные абсолютные yaw/pitch ±180°, шаг 1. | Distance начинает с 0: 31 точка / 30 перемещений. Воздух для продолжения не требуется. На каждой точке fill air strict от −1 до +1 по трём осям. | `−mana × 0,00025`, float; максимум две цели в радиусе 2. |

Начальные случайные углы выбираются с шагом 0,01°. В обычном/full-set варианте
`Distance` и `caster` принадлежат исходному исполнителю; `execute as` у цели
не переносит этот score. В full-set variant увеличена именно длина рекурсии,
а не счётчик времени. Выбор targets имеет limit без sort, не гарантирует ближайшую цель.

`tentacle_damage` наносит `outside_border` с прямым атакующим `@a[tag=caster,limit=1]`.
Затем, независимо от успешности damage, вызывает восстановление у caster с
`overload_full_active`: Saturation на 1 с amp 0, шанс **0,5%** на Instant Health
на 1 с amp 0 и +300 mana. Каждый выбранный hit вызывает helper; явной проверки
успешного урона или одноразового cooldown в нём нет. Большой вариант наносит
damage напрямую и этого восстановления не вызывает.

## Плохое зелье маны: `mana_potions/bad`, `select`

`bad` делает **один** бросок 1..10: mana −2000 всегда; manamax −1000 при 1..8
(80%); manaregen −1 при 1..6 (60%). Проверки используют одно число, поэтому
потери связаны: при уменьшении regen также уменьшается максимум.
`mana_potions/select` пуст и самостоятельно ничего не выдаёт/не выбирает.
Период повторов `bad` зависит от caller, например `effect_remove_mana` в
[lbc_effects.md](lbc_effects.md). Пустой select не отменяет прямые вызовы bad.

Получение предметов и расход при активации остальных способностей описываются
в их предметных разделах. Поведение маны при start/end/reset дополнено в
[lbc_other_attributes.md](lbc_other_attributes.md) и [lbc_runtime.md](lbc_runtime.md).

## Проверенные исходники

| Источник | Раздел |
|---|---|
| [other/manaregen.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen.mcfunction) | Обычный проход: `manaregen` |
| [other/manaregen_belt_stacked.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_belt_stacked.mcfunction) | Регенерация, экипировка и обратная мана |
| [other/manaregen_hit.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_hit.mcfunction) | Регенерация, экипировка и обратная мана |
| [other/manaregen_inwater.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_inwater.mcfunction) | Регенерация, экипировка и обратная мана |
| [other/manaregen_mage_armor.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_mage_armor.mcfunction) | Регенерация, экипировка и обратная мана |
| [other/manaregen_mana_belts.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_mana_belts.mcfunction) | Регенерация, экипировка и обратная мана |
| [other/manaregen_mana_boost.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_mana_boost.mcfunction) | Регенерация, экипировка и обратная мана |
| [other/manaregen_true.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_true.mcfunction) | Регенерация, экипировка и обратная мана |
| [other/manaregen_true_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_true_pre.mcfunction) | Регенерация, экипировка и обратная мана |
| [other/negative_mana.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana.mcfunction) | Отрицательная мана: `negative_mana` |
| [other/negative_mana_1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana_1.mcfunction) | Отрицательная мана: `negative_mana` |
| [other/negative_mana_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana_2.mcfunction) | Отрицательная мана: `negative_mana` |
| [other/negative_mana_3.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana_3.mcfunction) | Отрицательная мана: `negative_mana` |
| [other/negative_mana_4.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana_4.mcfunction) | Отрицательная мана: `negative_mana` |
| [other/negative_mana_5.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana_5.mcfunction) | Предельная перегрузка: `_5`, `_5_helmet`, `damage_with_tmp_void` |
| [other/negative_mana_5_helmet.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana_5_helmet.mcfunction) | Предельная перегрузка: `_5`, `_5_helmet`, `damage_with_tmp_void` |
| [other/negative_mana_resist.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana_resist.mcfunction) | Отрицательная мана: `negative_mana` |
| [other/negative_mana/explode.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/explode.mcfunction) | Предельная перегрузка: `_5`, `_5_helmet`, `damage_with_tmp_void` |
| [other/negative_mana/tentacle.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle.mcfunction) | Щупальца: направление, длина, урон и восстановление |
| [other/negative_mana/tentacle_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_2.mcfunction) | Щупальца: направление, длина, урон и восстановление |
| [other/negative_mana/tentacle_2_1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_2_1.mcfunction) | Щупальца: направление, длина, урон и восстановление |
| [other/negative_mana/tentacle_big.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_big.mcfunction) | Щупальца: направление, длина, урон и восстановление |
| [other/negative_mana/tentacle_big_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_big_2.mcfunction) | Щупальца: направление, длина, урон и восстановление |
| [other/negative_mana/tentacle_big_2_1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_big_2_1.mcfunction) | Щупальца: направление, длина, урон и восстановление |
| [other/negative_mana/tentacle_damage.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_damage.mcfunction) | Щупальца: направление, длина, урон и восстановление |
| [other/negative_mana/tentacle_damage_restore_hp.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_damage_restore_hp.mcfunction) | Щупальца: направление, длина, урон и восстановление |
| [other/negative_mana/tentacle_full_set.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_full_set.mcfunction) | Щупальца: направление, длина, урон и восстановление |
| [other/negative_mana/tentacle_full_set_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_full_set_2.mcfunction) | Щупальца: направление, длина, урон и восстановление |
| [damage_with_tmp_void.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/damage_with_tmp_void.mcfunction) | Предельная перегрузка: `_5`, `_5_helmet`, `damage_with_tmp_void` |
| [other/speed_up.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/speed_up.mcfunction) | Регенерация, экипировка и обратная мана |
| [other/overload_crux/give.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/overload_crux/give.mcfunction) | Обычный проход: `manaregen` |
| [other/mana_potions/bad.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/mana_potions/bad.mcfunction) | Плохое зелье маны: `mana_potions/bad`, `select` |
| [other/mana_potions/select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/mana_potions/select.mcfunction) | Плохое зелье маны: `mana_potions/bad`, `select` |
