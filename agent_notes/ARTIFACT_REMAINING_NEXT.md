# Остаток пассивов мистического артефакта

Текущий новый код содержит type 1, 5, 6 и 8 (`LbcMagic.java:73-74, 217-222, 367-370`). Для следующей порции остаются type 2, 3, 4 и 7. Исходники ниже — datapack старого Paper 1.21.11 сервера; целевой мод — Fabric 26.3.

Этот разбор относится к состоянию перед переносом 1:4. Тип теперь реализован:
[текущий контракт](ARTIFACT_ARROWS_PORT.md). Следующий ограниченный объём —
[защита от взрывов 1:2](ARTIFACT_EXPLOSIONS_NEXT.md).

## Кандидаты по исходной механике

| Тип | Триггер и поведение | Оценка объёма |
|---|---|---|
| `1:2` antiexplosive | Инвентарь выбирает держателей через `notnt_active`; `players_2t` запускает `notnt.mcfunction`, которая удаляет `#minecraft:unstable` сущности в радиусе 24 и останавливает `tag=nuke`. Дополнительные suppression gates находятся в обработчиках стрел и пяти explosion/fire/lightning эффектов. | Широко пересекается с TNT, projectile и damage/effect системами. Не объединять в небольшой пакет. |
| `1:3` nofluid | Каждые 5 тиков, вне `stopper_magic` в радиусе 20: `nofluid.mcfunction` меняет в кубе ±12 lava→basalt, water→frosted ice, powder snow→snow block; также обрабатывает falling blocks в радиусе 16. `block_otverditel.mcfunction` превращает sand→sandstone, gravel→cobblestone, red sand→red sandstone, повреждённые/обычные anvils→iron block, и 16 concrete powder цветов в соответствующий concrete; stone bricks и netherite block указаны как преобразования в тот же блок. | **Не рекомендую следующим:** массовые блоковые изменения плюс falling entities, с заметной нагрузкой/риском изменения арены. Отдельно потребуется определить пределы редактирования и восстановления блоков. |
| `1:4` no-arrows | `sel.mcfunction` ставит `no_arrows_active`, если артефакт лежит в `container.*` или `weapon.*`; снимает tag, когда предмета там нет. Каждый player tick `noarrows.mcfunction` убивает `#minecraft:arrows` в радиусе 16, перед удалением ставит cloud particles и звук extinguish у каждой стрелы. В этом вызове нет проверки `stopper_magic`. | **Рекомендуемый следующий bounded batch:** один тип с одним радиусным projectile поведением. Сохранить точный arrow entity tag, радиус, inventory gate и отсутствие дополнительного suppression gate. В новом проекте уже есть entity load/unload lifecycle (`SkyWars2.java:60-65`, `LbcItems.java:438-473`); проверить способ покрыть движение стрел в радиус каждый тик, не сканируя все стрелы по одному разу на каждого держателя. |
| `1:7` colors | Каждые 5 тиков, вне `stopper_magic` в радиусе 20, диспетчер вызывает цвета, если блок под ногами — `#minecraft:colored` или блок на уровне игрока — `#minecraft:in_colored`. Есть 16 отдельных веток в `colors/*.mcfunction`. Они дают держателю баффы/снимают некоторые дебаффы; многие ветки также влияют на чужих игроков/мобов, XP, mana, огонь, follow range или позицию/телепортацию. | Очень широкая механика, не подходит для одного переноса без разбивки по веткам и проверки каждой стороны эффекта. Purple использует raid omen и spreadplayers; cyan ищет врагов на расстоянии до 300; green/magenta меняют XP/mana вокруг. |

## Рекомендация: сначала type 4

Ограничить следующую работу type 4: подтвердить типы сущностей, входящие в `#minecraft:arrows`, точный радиус 16 и поведение для нескольких держателей; связать lifecycle стрелы с уже отслеживаемыми держателями артефакта; сохранить эффекты удаления. Не расширять в этом шаге на all projectiles, type 2 explosives, глобальную блоковую защиту type 3 или color effects type 7. Источник: старые `function/items/changed_inventory/sel.mcfunction:36-37,53`, `function/players_tick.mcfunction:14`, `function/other/artifact/noarrows.mcfunction:1-3`.

Для type 3 ключевые источники: `function/players_5t.mcfunction:1`, `function/other/artifact/afkartifact5t.mcfunction:2`, `function/other/artifact/nofluid.mcfunction:1-4`, `function/other/artifact/block_otverditel.mcfunction`. Для type 7: `players_5t.mcfunction:1`, `afkartifact5t.mcfunction:3-4`, `colors.mcfunction:1-16`, и все 16 функций `colors/*.mcfunction`. Это рекомендации по объёму; перенос и runtime поведение не проверялись.

