# Первый набор жезлов: аудит источника

Это предложение ограничено двумя Tiny Sauvojen-жезлами. Источник не перенесён; наличие старых функций не является доказательством реализации в моде. Общие обзоры: [рабочие предметы](lbc_workingitems_main.md), [оружие и посохи](lbc_items_weapons.md), [обзор других workingitems](lbc_workingitems_other.md). Первичный источник — старый LBC в `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.

## Общий вход и правила оплаты

ПКМ по предмету вызывает advancement `lbc:interaction_rightclickable`; он выставляет `RC_Raycast`, `players_tick.mcfunction` направляет игрока в `items/spells`. Для выбранного `magicwand:1` вызывается `workingitems/spell1`; тот ставит временные `caster` и `raycaster`, сбрасывает `Distance`, затем идёт в `spell1_default` (или `spell1_default_left` для второй руки). Диспетчер проверяет предмет, достаточную ману и отсутствие `stopper_magic` в радиусе 20 блоков. Внешний `items/spells` также блокирует каст при `nomagic_active` у игрока или другого не-наблюдателя в радиусе 20. У обеих рук Tiny-жезлов есть отдельные ветки.

Для нового кода сохранять этот контракт через нынешние политики маны/магии: `LbcMana.trySpendWand` (Overload даёт только разрешение тратить при нехватке; реальная стоимость вычитается и может уйти в долг), suppress/no-magic и observer остаются запретами активации. Не копировать старую временную прибавку Overload `+10,000,000` из `items/spells`. Creative разрешён; не-наблюдательский creative cast всё равно расходует ману, а observer не получает способность.

## Предлагаемый полный минимальный срез

| Жезл | Цена из `config.mcfunction` | Триггер / перезарядка | Наблюдаемое действие источника |
|---|---:|---|---|
| Tiny Fire (`stfire_tiny:1`) | 25 | ПКМ, main/offhand; отдельного cooldown в dispatcher и cast-функции нет | `fire.mcfunction` запускает `fire_2`: small fireball появляется в точке глаз +1 с тегами `stwandfireball,can_deleted`; затем ставится velocity из `reikast` marker, добавляется `activated`, marker удаляется. Важно: `execute positioned 0.0 0.0 0.0` меняет позицию исполнения, сохраняя поворот; `^ ^ ^2.5` задаёт направленный вектор величины 2.5. Это расчёт скорости, а не ошибка от начала мира. Звук `minecraft:item.firecharge.use`, всем в master. Сам полёт/столкновение — vanilla small fireball; отдельный lifetime/удаление снаряда эти функции не задают. Ключ лута/ресурс-модели: `wand_fire_tiny`, `lbc:st/fire_tiny`; selector key `stfire_tiny` плюс общий `magicwand:1`.
| Tiny Blind (`stblind_tiny:1`) | 63 | ПКМ, main/offhand; cooldown отсутствует | `blind.mcfunction` начинает рекурсивный ray `blind_2`: от глаз, шаг 0.35, максимум `Distance ..100` (примерно 35 блоков), проверка сущности в кубе `dx=dy=dz=0` и радиусе 8 либо первой непрозрачной преграды. Попадание: одна подходящая сущность с `!raycaster,!spectator` получает blindness 4 секунд, amplifier 0, и `damage 0.25 arrow` от игрока `raycaster`; звук `minecraft:entity.squid.squirt`. `Distance=1000` останавливает луч; внешний `spell1` снимает caster/raycaster. Ключи: `wand_blind_tiny`, `lbc:st/blind_tiny`, selector `stblind_tiny` и `magicwand:1`.

Обе базовые функции после запуска эффекта снимают цену (`tiny_fire_wand_mana_use` / `tiny_blind_wand_mana_use`), затем вызывают Mage full-set бонус к `manamax` и `items/trinket_effects/cast_wand`. Полный hook: `blood_anal_boom` count 1 даёт шанс 25%; 2–15 — шанс `0.20+0.05*N`; 16+ — гарантирует запуск TNT helper. За `mana_gem` прибавляется `60+15*(N-1)` маны. Порядок — сначала TNT-roll, затем mana gem. Это реальный side effect каждого успешного каста, поэтому перенос группы должен включать оба тринкета и Mage full-set bonus, а стоимость должна списываться ровно один раз.

## Ограничения и неопределённости

- Tiny Blind source не исключает союзников: selector ray-hit не содержит `predicate=!lbc:same_team`. Это критичная точка для будущей friendly-fire политики; в переносе нужно применять целевой team-protection контракт, не выдавая старое поведение за безопасное.
- Tiny Fire velocity marker создаётся через `execute positioned 0.0 0.0 0.0`, что сбрасывает позицию исполнения, но не направление; локальный `^ ^ ^2.5` записывает повёрнутый вектор скорости, после чего marker удаляется. Нативный порт может задать этот вектор напрямую и назначить игрока owner снаряда.
- У обоих нет cooldown в этой цепочке; не добавлять выдуманный интервал. Mage bonus и trinket hook выполняются после каждого успешного dispatcher-вызова.
- Это ограниченный **предлагаемый** набор, не статус runtime-проверки. Перед портом сверить визуальные предметы/lore с `agent_notes/RESOURCE_PACK.md`; ресурс-пак только для чтения.

## Проверенные исходные файлы

- `function/items/interaction_rightclickable.json`, `function/players_tick.mcfunction`, `function/items/spells.mcfunction`.
- `function/workingitems/spell1.mcfunction`, `spell1_default.mcfunction`, `spell1_default_left.mcfunction`.
- `function/config.mcfunction` (mana costs), `function/workingitems/sauvojen_staffs_tiny/{fire,fire_2,blind,blind_2,blind_end}.mcfunction`.
- `loot_table/random_sauvojen_wand_tiny.json`, `function/items/trinket_effects/cast_wand.mcfunction`.
