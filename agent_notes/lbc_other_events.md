# LBC: lbc other events

Задание `lbc_other_events`: проверены ровно 137 источников из `agent_notes/inventory/research_jobs.json`. В описаниях caller означает обнаруженную ссылку в проиндексированных функциях; если его нет, возможность ручного запуска остаётся. Условие caller приведено из literal command. `dx/dy/dz` вместе с `distance` задают совместное пересечение, а `execute as` сам по себе не переносит позицию. `/effect give` измеряет длительность в секундах; `fuse:2s` в SNBT — Short на два тика (0,1 с), тогда как `schedule 2s` — две секунды.


## Жизненный цикл Halloween

`start` выдаёт всем Halloween advancement и включает звук. Еда с custom_data.halloween_food:1 запускает consume-item advancement: функция прибавляет pumpkin_counter1 и при значении1..100 отмечает **одноимённый критерий одного** достижения true_advancements/event/eat_halloween_food. У него100 impossible-критериев без явного requirements: завершение требует всех100, затем reward вызывает gain/default_kit один раз. При counter>100 функция не добавляет критерий. Сам eat-trigger отзывается каждый обычный players_tick, разрешая повторное употребление.

`player_tick_1s` проверяет screamer_halloween item_display в2 блоках; **только при наличии** вызывает screamer_activate. Та удаляет один limit1 display, добавляет1..50 memcoins, показывает награду и равновероятно выбирает сцену1..7. В коде16 веток, но8..16 недостижимы из этого random и все эти helpers пустые. Сцены1..7: невидимый скелет; Darkness10с и TNT наY+240 с fuse170t; scared+60/lookback+10;8 silverfish с1HP и случайной Motion;6 wind-markers плюс LevitationVII1с;4 wind_charge; замена #solid на jack_o_lantern в кубе11³. Каждая играет свой звук. Wind-marker движется0.2 вперёд, поворачивается10° и удаляется с шансом4% за tick-call.

Семейство `jack_o_lantern_*` создаёт летящий block_display с тегом `jack_o_lantern`, передаёт ему направление, ID игрока и команду. Проверка на траектории запускает взрывную функцию, когда в кубе selector `dx=0,dy=0,dz=0` есть незатегованная цель-моб; это куб 1×1×1 в точке исполнения, а не радиусная проверка. Взрыв создаёт фейерверк, наносит всем мобам в радиусе 3,5 урона 6 типа arrow (с Owner игрока, если удалось найти `damager`), снимает временный тег и удаляет display.

Ссылки на опорные этапы: [запуск события](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/start.mcfunction), [учёт еды](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction), [начисление валюты и scare-развилка](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer_activate.mcfunction), [создание фонаря](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/jack_o_lantern_spawn.mcfunction), [проверка цели](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/jack_o_lantern_check.mcfunction), [взрыв и урон](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/jack_o_lantern_explode.mcfunction).


## Pumpkin King sword: проклятие и переработка предмета

Hit-hook меча выдаёт `lbc:halloween/pumpkin_curse` цели только когда выбран `pumpkin_king` и пострадавший игрок находится в радиусе 7, не spectator и не same_team. Раз в 10 тиков `tick_10t` проверяет curse marker во всех container slots, weapon slots и armor slots; при нахождении `tick_10t_true` последовательно проходит 36 inventory slots, offhand и четыре armor slots. Слот-обработчик заменяет прежнее место проклятого предмета семенами тыквы и перемещает проклятый предмет в выбранное другое место; занятый предмет назначения добавляется в массив `custom_data.storage` проклятого предмета. Например, slot 0 выбирает slot 1 или 27 (50/50), а offhand — boots или container.13. Поскольку проверки последовательные, передвижение в ещё не проверенный слот может вызвать следующий slot helper в том же проходе. Общий звук проигрывается после обхода. Урон/столкновение само по себе не удаляет проклятый предмет: отдельная секундная `drop_cursed_pumpkin` обрабатывает только выброшенные item entities с `pumpkin_curse`, причём пропускает измерения Nexus и Imprinted. Она сохраняет число совпавших путей `storage[]` в `tmp`, затем макрос переносит по одному предмету из каждого индекса массива в новые item entities (включая `id`, `count` и при наличии `components`) и после обхода удаляет исходную проклятую entity. При пустом массиве первый макровызов всё равно создаёт item-placeholder `minecraft:stone`, но чтение отсутствующей записи не может восстановить прежнее содержимое. [Условие выдачи](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/activate.mcfunction), [проверка каждые 10 тиков](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t.mcfunction), [слотовый обход](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction), [вход выброшенного предмета](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/drop_cursed_pumpkin.mcfunction), [macro-восстановление item fields](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/drop_cursed_pumpkin_recursive.mcfunction).

Для Pumpkin Curse сверены все41 полных командных шаблона: [все переходы](inventory/pumpkin_curse_slots.md), [audit](inventory/events_variants_audit.json). Особенность container.9: всегда задаёт проклятому предмету equippable=feet, даже когда выбран container.10 или18. Armor/offhand-переходы меняют equippable при переносе в выбранную броню; семена в освобождённых armor slots тоже получают соответствующий equippable. Общий shulker-buffer166 используется в текущем измерении; без доступного контейнера копирование может не выполнить ожидаемое перемещение, а дальнейшие команды не откатываются.

## Торговец Bucks


Использование rc выдаёт два random_bucks loot и создаёт постоянного жителя с maxHealth1000000. Его10 сделок заполняются через общий shulker-buffer10241024,177,10241024 в текущем измерении: первые7 покупают random_bucks и продают random_glack, последние3 наоборот. Каждый слот получает отдельные случайные результаты; maxUses2147483647, uses1. Буфер очищается в начале и перезаписывается каждой сделкой; цены и предметы определяют две loot tables. Собственного таймера исчезновения нет. Выбирается limit1 trader в0.01 без sort=nearest, поэтому совпадение не гарантирует именно только что созданного жителя.

## Soul Corruption Wand и Metasola

Обычный Soul Corruption cast обрабатывает всех non-spectator mobs до25 блоков, включая самого игрока и союзников: добавляет ghosting5 и отрицательные armor/toughness/attack/gravity/fall modifiers. Vex передаётся отдельному ghosting helper. При sneak и upgradable вместо этого создаёт16 vex и проверяет возможность Academy upgrade. Цена из config1500mana; полный Mage-set дополнительно увеличивает manamax на mage_set_bonus, trinket cast-hook вызывается при включённой настройке. Sneak у upgraded только меняет mode1/2; обычный клик mode1 делает ghosting, mode2 выдаёт metasola и возвращает1000mana перед общим списанием1500 (итог−500 при этой конфигурации).

Брошенная metasola type1..6 создаёт display и уязвимую невидимую pig-основу с maxHealth25/50/20/15/500/40. Pig получает примерно двойную Motion предмета; display следует за ней по lbcID, а lbcID2/team берутся у ближайшего thrower. Выбор display/pig limit1 в0.01 без связи с новым summon может захватить соседний экземпляр. При пропаже pig или владельца non-spectator display/pig удаляются; отдельного срока конца нет. Эффекты включаются после Lifetime200/80/100/40/200/100 соответственно (обычный тик20Гц:10/4/5/2/10/5с):

| Type | Поведение после задержки |
|---|---|
|1|Раз/с4 magic-урона врагам до10, с атрибуцией владельцу.|
|2|Раз/с Instant HealthIII на1с владельцу/союзникам до10; цель, подходящая по ID и team, обрабатывается дважды.|
|3|Каждый тик motion-команда тянет/крутит всех mobs до20, включая владельца и союзников, к одному metasola_3 display.|
|4|Каждый тик доводит mana владельца/союзников до500, если score≤500.|
|5|Каждый тик20 in_fire-урона всем mobs в0.01..10 без friendly-фильтра; pig опускается0.2, ставится световая структура integrity0.1 со случайным seed.|
|6|Каждый тик ResistanceV1с и void_resistance минимум2 владельцу/союзникам до10; сами эти helpers не уменьшают score после ухода.|

Motion-команды — внешняя регистрация из [COMMAND_BRIDGES.md](COMMAND_BRIDGES.md); здесь описаны передаваемые параметры. Работа display останавливается при time stop вместе с обычным raycast tick.

## Vex Wand и булавы Halloween

Vex Wand стоит700mana по config. Обычный cast через внешний motion add толкает mobs до5 на единичный вектор взгляда. Sneak с upgradable ставит air_sphere template и вызывает Academy upgrade, затем пытается задать всем mobs до11 Motion длины10 по их направлению к самим себе: после as @e ссылка @s уже цель, исходный игрок в этой команде не сохраняется. Нельзя уверенно приписывать ей направленное отталкивание от автора. Upgraded имеет4 режима: общий толчок; только пользователь; mobs в0.01..5; пять wind_charge с направлением и разбросом. У wind_charge копируются lbcID2/team, но Owner set from entity @p без UUID-path пытается записать целый compound вместо UUID. Цена снимается даже при неизвестном mode; offhand выбирает mode из equipment.offhand. Mage/trinket hooks такие же, как у Soul Corruption.

Soulhunter Mace тратит stamina mace_1 по1 за sprint-pass, добавляя Motion0.2 вперёд. На исчерпании ставит unstamined; на земле без sprint recharge прибавляет1 до60 и снимает блокировку. Use без sneak делает dash Motion3 и cooldown mace_2=5; с sneak — splash15 player_attack по чужим mobs до10, разрушает7 пересекающихся air/destroy-прямоугольников вокруг игрока и ставит mace_4=20. Jump задаёт MotionY1.2 и mace_3=5. Эти три cooldown убывают раз/с в players_1s:5/5/20с, не тиков. Halloween Mace отдельным helper задаёт motionY1 и cooldown_wind_charge5. Геометрия/разгон зависят от внешних motion commands.

## November, Tournament и Chorus Pumpkin

November click выбирает сторону по блоку под игроком: blue glass→november1/Jedi, red→2/Sith; очищает предметы lightsaberrc и выдаёт lightsaber loot. Если ни того ни другого, показывает подсказку и ставит два glass-блока локально впереди/по сторонам. Give_sword отдельно выдаёт меч и ставит lucky_block_count−2147483646. Связанные kill-счётчики описаны в предметных hooks.

Tournament start создаёт kills/wins objectives и очищает sidebar; end показывает kills в sidebar, wins в player list. Это не удаление статистики: add существующего objective может завершиться ошибкой, следующие команды продолжаются. В start изменение имени wins стоит до add, поэтому при первом запуске оно может не примениться.

Chorus Pumpkin создаёт20 падающих pumpkin с FallHurtAmount5/max10, назначает X/Z Motion±0.5 и Y0.5..1.5, затем spreadplayers перемещает игрока с maxRange10 вокруг прежней позиции и отзывает consume advancement. Копируются limit20 pumpkinfall в0.01, поэтому давние соседние falling_block тоже могут попасть в выбор; отдельной очистки по времени нет.

## Файлы: other / bucks

### `lbc:other/bucks/gen_trades`

Для каждого из 10 рецептов функция по очереди кладёт `lbc:random_glack` и `lbc:imba/random_bucks` в слоты 0 и 1 временного shulker box, читает созданные предметы и записывает пару в Offers.Recipes нужного торгового слота. Рецепты 0–6 покупают предмет из слота 1 и продают предмет из 0; последние 3 направлены наоборот. Таким образом, реальные цены/товары выбираются loot tables при генерации, а не задаются фиксированным списком в самой функции. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/bucks/gen_trades.mcfunction)
### `lbc:other/bucks/rc`

Snowball с `custom_data.bucks:1` запускает генератор: он дважды выдаёт `lbc:imba/random_bucks`, очищает и заново ставит жёлтый shulker box в координатах (10241024,177,10241024), затем создаёт рядом постоянного жителя luck_trader с 10 заглушками-торговлями. Следующая функция вызывается только для ближайшего ещё не отмеченного villager с тегом luck_trader в радиусе 0,01; после настройки ему ставят `spawned`, поэтому повторная инициализация не проходит. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/bucks/rc.mcfunction)
### `lbc:other/event/halloween/eat_halloween_food`

Consume-item advancement для custom_data.halloween_food:1 вызывает этот reward. Функция прибавляет pumpkin_counter1 и отмечает criterion1..100 в одном achievement, через return run. Награда default kit относится к завершению всех100 критериев, а не к каждому предмету. При counter>100 новых критериев нет. [Trigger](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_halloween_food.json) · [Все100 критериев/reward](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/event/eat_halloween_food.json) · [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction)

### `lbc:other/event/halloween/jack_o_lantern_check`

Находит игрока с совпадающим lbcID2 и временно помечает damager. Если в кубе1×1×1 есть mob без damager/spectator, вызывает explode; затем снимает damager у всех игроков. Screamer display эта функция не удаляет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/jack_o_lantern_check.mcfunction)

### `lbc:other/event/halloween/jack_o_lantern_explode`

Создаёт короткоживущую ракету-фейерверк и записывает ей Owner найденного `damager`, если он есть. Затем наносит всем mob-tag entities в радиусе 3,5 урон 6 типа arrow (с Owner-атрибуцией при найденном игроке), очищает временные теги и убивает источник луча. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/jack_o_lantern_explode.mcfunction)
### `lbc:other/event/halloween/jack_o_lantern_spawn`

Создаёт block_display-фонарь с slowraycast/rare/jack_o_lantern/shield_blockable; копирует Rotation, lbcID2 и team_number на display в0.001 блока, limit1, без actived. В этом spawn нет назначения damager и нет добавления actived; поиск владельца происходит позднее при проверке попадания. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/jack_o_lantern_spawn.mcfunction)

### `lbc:other/event/halloween/jack_o_lantern_tick`

В каждом тике передвигает луч по его собственной позиции, рисует soul_fire_flame и проверяет столкновения. Selector с `dx=dy=dz=0` ищет незатегованную цель-моба в кубе 1×1×1 на позиции исполнения; если её находит, запускает explode. Если траектория попадает в блок вне `#minecraft:airs`, также взрывается; иначе движется до Distance 8 (выход через homing helper), но рекурсивный шаг на 0,2 блока задан только для Distance 1..7. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/jack_o_lantern_tick.mcfunction)
### `lbc:other/event/halloween/particle_wind`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_rare.mcfunction`; условие вызова: `execute if entity @s[tag=particle_wind] run return run function lbc:other/event/halloween/particle_wind`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/particle_wind.mcfunction)

### `lbc:other/event/halloween/player_tick_1s`

Раз в секунду проверяет наличие screamer_halloween display в2 блоках; при наличии вызывает screamer_activate. Без display ни награды, ни случайной сцены нет. Награду1..50 memcoins и random1..7 делает вызываемый helper, не этот проверяющий файл. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/player_tick_1s.mcfunction)

### `lbc:other/event/halloween/screamer_activate`

Выбирает случайное целое 1..7 и через `return run` вызывает ровно соответствующую из сцен 1–7; записанные ниже ветки 8 и выше недостижимы, потому что их значения не может вернуть random. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer_activate.mcfunction)
### `lbc:other/event/halloween/start`

Ставит глобальное событие в progression всем игрокам через grant advancement и проигрывает общий Halloween sound. Функция не запускает все эффекты предметов сама: пищевые и боевые механики имеют отдельные hooks. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/start.mcfunction)
### `lbc:other/event/halloween/halloween_mace/ability`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items/armor.mcfunction`; условие вызова: `execute if items entity @s[x_rotation=80..90] weapon.* *[minecraft:custom_data~{halloween_mace:1}] unless score @s cooldown_wind_charge matches 1.. run function lbc:other/event/halloween/halloween_mace/ability`. Обновляет NBT/components: modify storage lbc.math cmd set value "execute as @s at @s positioned as @s rotated as @s run motion set ~ ~1 ~ @s". Состояние scoreboard: scoreboard players set @s cooldown_wind_charge 5. Передаёт шаг в `lbc:run_command`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/halloween_mace/ability.mcfunction)

## Файлы: other / event / halloween / pumpkin_king_sword

### `lbc:other/event/halloween/pumpkin_king_sword/activate`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items/swords.mcfunction`; условие вызова: `execute at @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{pumpkin_king:1}}}}] as @a[gamemode=!spectator,distance=0.01..7,nbt={HurtTime:10s},predicate=!lbc:same_team] at @s run function lbc:other/event/halloween/pumpkin_king_sword/activate`. Loot: give lbc:halloween/pumpkin_curse → @s. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/activate.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/drop_cursed_pumpkin`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/tick1s.mcfunction`; условие вызова: `execute as @e[type=minecraft:item,nbt={Item:{components:{"minecraft:custom_data":{pumpkin_curse:1}}}}] at @s unless dimension minecraft:nexus unless dimension minecraft:imprinted run function lbc:other/event/halloween/pumpkin_king_sword/drop_cursed_pumpkin`. Обновляет NBT/components: modify storage lbc.math i set value 0. Состояние scoreboard: scoreboard players set i lbc.math 0. Передаёт шаг в `lbc:other/event/halloween/pumpkin_king_sword/drop_cursed_pumpkin_recursive`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/drop_cursed_pumpkin.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/drop_cursed_pumpkin_recursive`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/drop_cursed_pumpkin.mcfunction`; условие вызова: `function lbc:other/event/halloween/pumpkin_king_sword/drop_cursed_pumpkin_recursive with storage lbc.math`. Создаёт `item` с {Item:{id:"minecraft:stone",components:{},count:1},Tags:["current_item"]}. Обновляет NBT/components: modify entity @e[type=item,limit=1,distance=..0.01,tag=current_item] Item.id set from entity @s Item.components."minecraft:cust; modify entity @e[type=item,limit=1,distance=..0.01,tag=current_item] Item.count set from entity @s Item.components."minecraft:c; modify entity @e[type=item,limit=1,distance=..0.01,tag=current_item] Item.components set from entity @s Item.components."minecr. Состояние scoreboard: scoreboard players add i lbc.math 1. Передаёт шаг в `lbc:other/event/halloween/pumpkin_king_sword/drop_cursed_pumpkin_recursive`. Использует макропараметры `$(i)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/drop_cursed_pumpkin_recursive.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/tick_10t`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_10t.mcfunction`; условие вызова: `function lbc:other/event/halloween/pumpkin_king_sword/tick_10t`. Передаёт шаг в `lbc:other/event/halloween/pumpkin_king_sword/tick_10t_true`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/tick_10t_true`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t.mcfunction`; условие вызова: `execute if items entity @s container.* *[minecraft:custom_data~{pumpkin_curse:1}] run return run function lbc:other/event/halloween/pumpkin_king_sword/tick_10t_true`. Передаёт шаг в `lbc:other/event/halloween/pumpkin_king_sword/slot/0`, `lbc:other/event/halloween/pumpkin_king_sword/slot/1`, `lbc:other/event/halloween/pumpkin_king_sword/slot/2`, `lbc:other/event/halloween/pumpkin_king_sword/slot/3`, `lbc:other/event/halloween/pumpkin_king_sword/slot/4`, `lbc:other/event/halloween/pumpkin_king_sword/slot/5`, `lbc:other/event/halloween/pumpkin_king_sword/slot/6`, `lbc:other/event/halloween/pumpkin_king_sword/slot/7`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/up_right`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/up_right.mcfunction)

## Файлы: other / event / halloween / pumpkin_king_sword / slot

### `lbc:other/event/halloween/pumpkin_king_sword/slot/0`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.0 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/0`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/0.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.1 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/1`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/1.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/10`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.10 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/10`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/10.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/11`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.11 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/11`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/11.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/12`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.12 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/12`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/12.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/13`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.13 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/13`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/13.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/14`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.14 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/14`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/14.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/15`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.15 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/15`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/15.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/16`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.16 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/16`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/16.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/17`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.17 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/17`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/17.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/18`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.18 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/18`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/18.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/19`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.19 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/19`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/19.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.2 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/2`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/2.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/20`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.20 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/20`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/20.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/21`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.21 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/21`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/21.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/22`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.22 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/22`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/22.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/23`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.23 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/23`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/23.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/24`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.24 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/24`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/24.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/25`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.25 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/25`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/25.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/26`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.26 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/26`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/26.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/27`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.27 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/27`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/27.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/28`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.28 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/28`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/28.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/29`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.29 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/29`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/29.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.3 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/3`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/3.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/30`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.30 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/30`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/30.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/31`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.31 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/31`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/31.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/32`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.32 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/32`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/32.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/33`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.33 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/33`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/33.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/34`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.34 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/34`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/34.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/35`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.35 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/35`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/35.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/4`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.4 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/4`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/4.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/5`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.5 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/5`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/5.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/6`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.6 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/6`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/6.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/7`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.7 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/7`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/7.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/8`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.8 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/8`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/8.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/9`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s container.9 *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/9`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 ; modify block 10241024 166 10241024 Items[0].components."minecraft:equippable" set value {"slot":"feet"}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/9.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/chest`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s armor.chest *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/chest`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 ; modify block 10241024 166 10241024 Items[0].components."minecraft:equippable" set value {"slot":"head"}; modify block 10241024 166 10241024 Items[0].components."minecraft:equippable" set value {"slot":"legs"}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/chest.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/feet`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s armor.feet *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/feet`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 ; modify block 10241024 166 10241024 Items[0].components."minecraft:equippable" set value {"slot":"legs"}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/feet.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/head`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s armor.head *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/head`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 ; modify block 10241024 166 10241024 Items[0].components."minecraft:equippable" set value {"slot":"chest"}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/head.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/legs`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s armor.legs *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/legs`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 ; modify block 10241024 166 10241024 Items[0].components."minecraft:equippable" set value {"slot":"chest"}; modify block 10241024 166 10241024 Items[0].components."minecraft:equippable" set value {"slot":"feet"}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/legs.mcfunction)

### `lbc:other/event/halloween/pumpkin_king_sword/slot/offhand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/tick_10t_true.mcfunction`; условие вызова: `execute if items entity @s weapon.offhand *[minecraft:custom_data~{pumpkin_curse:1}] run function lbc:other/event/halloween/pumpkin_king_sword/slot/offhand`. Обновляет NBT/components: modify block 10241024 166 10241024 Items[0].components."minecraft:custom_data".storage append from block 10241024 166 10241024 ; modify block 10241024 166 10241024 Items[0].components."minecraft:equippable" set value {"slot":"feet"}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/pumpkin_king_sword/slot/offhand.mcfunction)

## Файлы: other / event / halloween / screamer

### `lbc:other/event/halloween/screamer/1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer_activate.mcfunction`; условие вызова: `execute if score rnd lbc.math matches 1 run return run function lbc:other/event/halloween/screamer/1`. Создаёт `skeleton` с {active_effects:[{"id":"minecraft:invisibility",duration:-1,show_particles:false},{"id":"minecraft:fire_resistance",duration:-1,show_particles:false}],HasVisualFire:false}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer/1.mcfunction)

### `lbc:other/event/halloween/screamer/10`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer/10.mcfunction)

### `lbc:other/event/halloween/screamer/11`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer/11.mcfunction)

### `lbc:other/event/halloween/screamer/12`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer/12.mcfunction)

### `lbc:other/event/halloween/screamer/13`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer/13.mcfunction)

### `lbc:other/event/halloween/screamer/14`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer/14.mcfunction)

### `lbc:other/event/halloween/screamer/15`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer/15.mcfunction)

### `lbc:other/event/halloween/screamer/16`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer/16.mcfunction)

### `lbc:other/event/halloween/screamer/2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer_activate.mcfunction`; условие вызова: `execute if score rnd lbc.math matches 2 run return run function lbc:other/event/halloween/screamer/2`. Эффект: @s получает `None` на None секунд, amplifier 0. Создаёт `tnt` с {fuse:170}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer/2.mcfunction)

### `lbc:other/event/halloween/screamer/3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer_activate.mcfunction`; условие вызова: `execute if score rnd lbc.math matches 3 run return run function lbc:other/event/halloween/screamer/3`. Состояние scoreboard: scoreboard players add @s scared 60; scoreboard players add @s lookback 10. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer/3.mcfunction)

### `lbc:other/event/halloween/screamer/4`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer_activate.mcfunction`; условие вызова: `execute if score rnd lbc.math matches 4 run return run function lbc:other/event/halloween/screamer/4`. Создаёт `silverfish` с {Health:1,attributes:[{"id":"minecraft:fall_damage_multiplier",base:0}]}. Передаёт шаг в `lbc:rand_motion`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer/4.mcfunction)

### `lbc:other/event/halloween/screamer/5`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer_activate.mcfunction`; условие вызова: `execute if score rnd lbc.math matches 5 run return run function lbc:other/event/halloween/screamer/5`. Эффект: @s получает `None` на None секунд, amplifier 0. Создаёт `marker` с {Tags:["slowraycast","rare","particle_wind"],Rotation:[0,0]}; `marker` с {Tags:["slowraycast","rare","particle_wind"],Rotation:[60,0]}; `marker` с {Tags:["slowraycast","rare","particle_wind"],Rotation:[120,0]}; `marker` с {Tags:["slowraycast","rare","particle_wind"],Rotation:[180,0]}; `marker` с {Tags:["slowraycast","rare","particle_wind"],Rotation:[-60,0]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer/5.mcfunction)

### `lbc:other/event/halloween/screamer/6`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer_activate.mcfunction`; условие вызова: `execute if score rnd lbc.math matches 6 run return run function lbc:other/event/halloween/screamer/6`. Создаёт `wind_charge`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer/6.mcfunction)

### `lbc:other/event/halloween/screamer/7`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer_activate.mcfunction`; условие вызова: `execute if score rnd lbc.math matches 7 run return run function lbc:other/event/halloween/screamer/7`. Изменяет блоки: fill ~5 ~5 ~5 ~-5 ~-5 ~-5 jack_o_lantern replace #solid. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer/7.mcfunction)

### `lbc:other/event/halloween/screamer/8`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer/8.mcfunction)

### `lbc:other/event/halloween/screamer/9`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer/9.mcfunction)

## Файлы: other / event / halloween / soulcorruptwand

### `lbc:other/event/halloween/soulcorruptwand/apply_ghosting`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/cast_2.mcfunction`; условие вызова: `execute as @e[type=#mobs,distance=..25,tag=!spectator] at @s run function lbc:other/event/halloween/soulcorruptwand/apply_ghosting`. Состояние scoreboard: scoreboard players add @s effect_ghosting 5. Передаёт шаг в `lbc:effects/ghosting_second_vex`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/apply_ghosting.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_default.mcfunction`; условие вызова: `execute if score @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{soulcorrupter:1}}}}] mana >= soulcorruption_wand_mana_use lbc.math unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] at @s anchored eyes positioned ^ ^ ^ run function lbc:other/event/halloween/soulcorruptwand/cast`. Состояние scoreboard: scoreboard players operation @s mana -= soulcorruption_wand_mana_use lbc.math; scoreboard players operation @s manamax += mage_set_bonus lbc.math. Передаёт шаг в `lbc:other/event/halloween/soulcorruptwand/cast_2`, `lbc:other/event/halloween/soulcorruptwand/cast_2meditate`, `lbc:items/trinket_effects/cast_wand`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/cast.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/cast_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/cast.mcfunction`; условие вызова: `execute unless items entity @s weapon.* *[custom_data~{soulcorrupter:1,upgradable:1}] run function lbc:other/event/halloween/soulcorruptwand/cast_2`. Передаёт шаг в `lbc:other/event/halloween/soulcorruptwand/apply_ghosting`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/cast_2.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/cast_2meditate`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/cast.mcfunction`; условие вызова: `execute if items entity @s[predicate=lbc:sneak] weapon.* *[custom_data~{soulcorrupter:1,upgradable:1}] run function lbc:other/event/halloween/soulcorruptwand/cast_2meditate`. Создаёт `vex`. Передаёт шаг в `lbc:other/magic_academy/can_upgrade`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/cast_2meditate.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/cast_upgraded`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_upgraded.mcfunction`; условие вызова: `execute if score @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{soulcorrupter:1}}}}] mana >= soulcorruption_wand_mana_use lbc.math unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] at @s anchored eyes positioned ^ ^ ^ run function lbc:other/event/halloween/soulcorruptwand/cast_upgraded`. Передаёт шаг в `lbc:other/magic_academy/change_mode_2`, `lbc:other/event/halloween/soulcorruptwand/cast_upgraded_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/cast_upgraded.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/cast_upgraded_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/cast_upgraded.mcfunction`; условие вызова: `execute unless predicate lbc:sneak run function lbc:other/event/halloween/soulcorruptwand/cast_upgraded_2`. Состояние scoreboard: scoreboard players operation @s mana -= soulcorruption_wand_mana_use lbc.math; scoreboard players operation @s manamax += mage_set_bonus lbc.math. Передаёт шаг в `lbc:other/event/halloween/soulcorruptwand/cast_2`, `lbc:other/event/halloween/soulcorruptwand/cast_upgraded_2_mode_metasola`, `lbc:items/trinket_effects/cast_wand`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/cast_upgraded_2.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/cast_upgraded_2_mode_metasola`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/cast_upgraded_2.mcfunction`; условие вызова: `execute if score #mode lbc.math matches 2 run function lbc:other/event/halloween/soulcorruptwand/cast_upgraded_2_mode_metasola`. Loot: spawn lbc:halloween/metasola → ~. Состояние scoreboard: scoreboard players add @s mana 1000. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/cast_upgraded_2_mode_metasola.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/cast_upgraded_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_upgraded_left.mcfunction`; условие вызова: `execute if items entity @s weapon.offhand *[minecraft:custom_data~{soulcorrupter:1}] if score @s mana >= soulcorruption_wand_mana_use lbc.math unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] run function lbc:other/event/halloween/soulcorruptwand/cast_upgraded_left`. Передаёт шаг в `lbc:other/magic_academy/change_mode_2_left`, `lbc:other/event/halloween/soulcorruptwand/cast_upgraded_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/cast_upgraded_left.mcfunction)

## Файлы: other / event / halloween / soulcorruptwand / metasola

### `lbc:other/event/halloween/soulcorruptwand/metasola/1_tick_1s`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/tick_1s_select.mcfunction`; условие вызова: `execute if entity @s[tag=metasola_1,scores={Lifetime=200..}] run return run function lbc:other/event/halloween/soulcorruptwand/metasola/1_tick_1s`. Состояние scoreboard: scoreboard players operation *temp lbcID2 = @s lbcID2; scoreboard players operation *tempt lbcID2 = @s team_number. Передаёт шаг в `lbc:other/event/halloween/soulcorruptwand/metasola/break`, `lbc:other/event/halloween/soulcorruptwand/metasola/1_tick_1s1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/1_tick_1s.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/metasola/1_tick_1s1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/1_tick_1s.mcfunction`; условие вызова: `execute as @e[type=#mobs,distance=..10,tag=!spectator,predicate=!lbc:same_id,predicate=!lbc:same_team,tag=!metasola_base] at @s run function lbc:other/event/halloween/soulcorruptwand/metasola/1_tick_1s1`. Наносит 4 урона по @s типа `magic`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/1_tick_1s1.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/metasola/2_tick_1s`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/tick_1s_select.mcfunction`; условие вызова: `execute if entity @s[tag=metasola_2,scores={Lifetime=80..}] run return run function lbc:other/event/halloween/soulcorruptwand/metasola/2_tick_1s`. Состояние scoreboard: scoreboard players operation *temp lbcID2 = @s lbcID2; scoreboard players operation *tempt lbcID2 = @s team_number. Передаёт шаг в `lbc:other/event/halloween/soulcorruptwand/metasola/break`, `lbc:other/event/halloween/soulcorruptwand/metasola/2_tick_1s_at`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/2_tick_1s.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/metasola/2_tick_1s_at`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/2_tick_1s.mcfunction`; условие вызова: `execute as @e[type=#mobs,distance=..10,tag=!spectator,predicate=lbc:same_team,tag=!metasola_base] at @s run function lbc:other/event/halloween/soulcorruptwand/metasola/2_tick_1s_at`. Эффект: @s получает `None` на None секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/2_tick_1s_at.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/metasola/3_tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/tick.mcfunction`; условие вызова: `execute if entity @s[tag=metasola_3,scores={Lifetime=100..}] run return run function lbc:other/event/halloween/soulcorruptwand/metasola/3_tick`. Передаёт шаг в `lbc:other/event/halloween/soulcorruptwand/metasola/3_tick_at`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/3_tick.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/metasola/3_tick_at`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/3_tick.mcfunction`; условие вызова: `execute as @e[tag=!spectator,distance=..20,tag=!metasola_base,type=#mobs] at @s run function lbc:other/event/halloween/soulcorruptwand/metasola/3_tick_at`. Обновляет NBT/components: modify storage lbc.math cmd set value "execute facing entity @e[type=item_display,distance=..20,tag=metasola_3,limit=1] eyes ro. Передаёт шаг в `lbc:run_command`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/3_tick_at.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/metasola/4_tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/tick.mcfunction`; условие вызова: `execute if entity @s[tag=metasola_4,scores={Lifetime=40..}] run return run function lbc:other/event/halloween/soulcorruptwand/metasola/4_tick`. Состояние scoreboard: scoreboard players set @a[distance=..10,scores={mana=..500},predicate=lbc:same_team] mana 500; scoreboard players set @a[distance=..10,scores={mana=..500},predicate=lbc:same_id] mana 500. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/4_tick.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/metasola/5_tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/tick.mcfunction`; условие вызова: `execute if entity @s[tag=metasola_5,scores={Lifetime=200..}] run return run function lbc:other/event/halloween/soulcorruptwand/metasola/5_tick`. Наносит 20 урона по @s типа `in_fire`. Передаёт шаг в `lbc:other/event/halloween/soulcorruptwand/metasola/5_tick1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/5_tick.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/metasola/5_tick1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/5_tick.mcfunction`; условие вызова: `function lbc:other/event/halloween/soulcorruptwand/metasola/5_tick1 with storage lbc.math`. Использует макропараметры `$(rand)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/5_tick1.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/metasola/6_tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/tick.mcfunction`; условие вызова: `execute if entity @s[tag=metasola_6,scores={Lifetime=100..}] run return run function lbc:other/event/halloween/soulcorruptwand/metasola/6_tick`. Передаёт шаг в `lbc:other/event/halloween/soulcorruptwand/metasola/6_tick_1s_at`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/6_tick.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/metasola/6_tick_1s_at`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/6_tick.mcfunction`; условие вызова: `execute as @e[type=#mobs,distance=..10,tag=!spectator,predicate=lbc:same_team,tag=!metasola_base] at @s run function lbc:other/event/halloween/soulcorruptwand/metasola/6_tick_1s_at`. Эффект: @s получает `None` на None секунд, amplifier 0. Состояние scoreboard: scoreboard players set @s void_resistance 2. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/6_tick_1s_at.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/metasola/break`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/1_tick_1s.mcfunction`; условие вызова: `execute unless entity @a[predicate=lbc:same_id,limit=1] run return run function lbc:other/event/halloween/soulcorruptwand/metasola/break`. Завершение: удаляет сущность (`@e[type=pig,limit=1,tag=metasola_base,predicate=lbc:same_id_mob]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/break.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/metasola/throw`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/projectiles/eggs.mcfunction`; условие вызова: `execute as @e[type=minecraft:snowball,sort=nearest,limit=1,nbt={Item:{components:{"minecraft:custom_data":{metasola:1}}}}] at @s run function lbc:other/event/halloween/soulcorruptwand/metasola/throw`. Создаёт `item_display` с {item_display:"fixed",transformation:{"translation": [0, 0.6, 0],"scale": [1, 1, 1],"left_rotation": [0,0,0,1],"right_rotation": [0.0, 0.0, 0.0, 1.0]},item:{id:"minecraft:carrot_on. Состояние scoreboard: scoreboard players operation @e[type=item_display,distance=..0.01,limit=1] lbcID2 = @p[tag=thrower,gamemode=!spectator] ; scoreboard players operation @e[type=item_display,distance=..0.01,limit=1] team_number = @p[tag=thrower,gamemode=!specta; scoreboard players add *id.cloud lbcID 1; scoreboard players operation @e[type=item_display,distance=..0.01,limit=1] lbcID = *id.cloud lbcID. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/throw.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/metasola/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_player.mcfunction`; условие вызова: `execute if entity @s[tag=metasola] run return run function lbc:other/event/halloween/soulcorruptwand/metasola/tick`. Состояние scoreboard: scoreboard players operation *temp lbcID = @s lbcID; scoreboard players operation *temp lbcID2 = @s lbcID2; scoreboard players operation *tempt lbcID2 = @s team_number. Передаёт шаг в `lbc:other/event/halloween/soulcorruptwand/metasola/break`, `lbc:other/event/halloween/soulcorruptwand/metasola/3_tick`, `lbc:other/event/halloween/soulcorruptwand/metasola/4_tick`, `lbc:other/event/halloween/soulcorruptwand/metasola/5_tick`, `lbc:other/event/halloween/soulcorruptwand/metasola/6_tick`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/tick.mcfunction)

### `lbc:other/event/halloween/soulcorruptwand/metasola/tick_1s_select`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycasters_1s.mcfunction`; условие вызова: `execute if entity @s[tag=metasola] run function lbc:other/event/halloween/soulcorruptwand/metasola/tick_1s_select`. Передаёт шаг в `lbc:other/event/halloween/soulcorruptwand/metasola/1_tick_1s`, `lbc:other/event/halloween/soulcorruptwand/metasola/2_tick_1s`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/tick_1s_select.mcfunction)

## Файлы: other / event / halloween / soulhunter_mace

### `lbc:other/event/halloween/soulhunter_mace/jump`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items/armor.mcfunction`; условие вызова: `execute if items entity @s[x_rotation=80..90] weapon.* *[minecraft:custom_data~{soulhunter_mace:1}] unless score @s mace_3 matches 1.. run function lbc:other/event/halloween/soulhunter_mace/jump`. Обновляет NBT/components: modify storage lbc.math cmd set value "motion set ~ ~1.2 ~ @s". Состояние scoreboard: scoreboard players set @s mace_3 5. Передаёт шаг в `lbc:run_command`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulhunter_mace/jump.mcfunction)

### `lbc:other/event/halloween/soulhunter_mace/sprint_end`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulhunter_mace/sprint_tick.mcfunction`; условие вызова: `execute unless score @s mace_1 matches 1.. run function lbc:other/event/halloween/soulhunter_mace/sprint_end`. Команда: `tag @s add unstamined`; `playsound block.glass.break master @a ~ ~ ~ 1 0.8`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulhunter_mace/sprint_end.mcfunction)

### `lbc:other/event/halloween/soulhunter_mace/sprint_recharge`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items/on_not_sprint.mcfunction`; условие вызова: `execute if score @s[predicate=lbc:on_ground] mace_1 matches ..59 run function lbc:other/event/halloween/soulhunter_mace/sprint_recharge`. Состояние scoreboard: scoreboard players add @s mace_1 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulhunter_mace/sprint_recharge.mcfunction)

### `lbc:other/event/halloween/soulhunter_mace/sprint_tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items/on_sprint.mcfunction`; условие вызова: `execute if items entity @s[tag=!unstamined,predicate=!lbc:on_ground] weapon.* *[minecraft:custom_data~{soulhunter_mace:1}] run function lbc:other/event/halloween/soulhunter_mace/sprint_tick`. Обновляет NBT/components: modify storage lbc.math cmd set value "motion add ^ ^ ^.2 @s". Состояние scoreboard: scoreboard players remove @s mace_1 1. Передаёт шаг в `lbc:run_command`, `lbc:other/event/halloween/soulhunter_mace/sprint_end`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulhunter_mace/sprint_tick.mcfunction)

### `lbc:other/event/halloween/soulhunter_mace/use`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Передаёт шаг в `lbc:other/event/halloween/soulhunter_mace/use_dash`, `lbc:other/event/halloween/soulhunter_mace/use_splash`. Завершение: сбрасывает advancement для повторного запуска. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulhunter_mace/use.mcfunction)

### `lbc:other/event/halloween/soulhunter_mace/use_dash`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulhunter_mace/use.mcfunction`; условие вызова: `execute unless score @s[predicate=!lbc:sneak] mace_2 matches 1.. run function lbc:other/event/halloween/soulhunter_mace/use_dash`. Обновляет NBT/components: modify storage lbc.math cmd set value "motion set ^ ^ ^3 @s". Состояние scoreboard: scoreboard players set @s mace_2 5. Передаёт шаг в `lbc:run_command`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulhunter_mace/use_dash.mcfunction)

### `lbc:other/event/halloween/soulhunter_mace/use_splash`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulhunter_mace/use.mcfunction`; условие вызова: `execute unless score @s[predicate=lbc:sneak] mace_4 matches 1.. run function lbc:other/event/halloween/soulhunter_mace/use_splash`. Наносит 15 урона по @s типа `player_attack`. Изменяет блоки: fill ~3 ~ ~10 ~-3 ~5 ~-10 air destroy; fill ~5 ~ ~9 ~-5 ~5 ~-9 air destroy; fill ~6 ~ ~8 ~-6 ~5 ~-8 air destroy. Состояние scoreboard: scoreboard players set @s mace_4 20; scoreboard players operation *temp lbcID2 = @s lbcID2; scoreboard players operation *tempt lbcID2 = @s team_number. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulhunter_mace/use_splash.mcfunction)

## Файлы: other / event / halloween / vex_wand

### `lbc:other/event/halloween/vex_wand/cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_default.mcfunction`; условие вызова: `execute if score @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{vex_wand:1}}}}] mana >= vex_wand_mana_use lbc.math unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] at @s anchored eyes positioned ^ ^ ^ run function lbc:other/event/halloween/vex_wand/cast`. Состояние scoreboard: scoreboard players operation @s mana -= vex_wand_mana_use lbc.math; scoreboard players operation @s manamax += mage_set_bonus lbc.math. Передаёт шаг в `lbc:other/event/halloween/vex_wand/cast_2`, `lbc:other/event/halloween/vex_wand/cast_2meditate`, `lbc:items/trinket_effects/cast_wand`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast.mcfunction)

### `lbc:other/event/halloween/vex_wand/cast_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast.mcfunction`; условие вызова: `execute unless items entity @s weapon.* *[custom_data~{vex_wand:1,upgradable:1}] run function lbc:other/event/halloween/vex_wand/cast_2`. Обновляет NBT/components: modify storage lbc.math cmd1 set value "motion add ^ ^ ^1 @e[distance=..5,type=#mobs,tag=!spectator]". Передаёт шаг в `lbc:run_command1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast_2.mcfunction)

### `lbc:other/event/halloween/vex_wand/cast_2_notuser`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast_upgraded_2.mcfunction`; условие вызова: `execute if score #mode lbc.math matches 3 run function lbc:other/event/halloween/vex_wand/cast_2_notuser`. Обновляет NBT/components: modify storage lbc.math cmd set value "motion add ^ ^ ^1 @e[distance=0.01..5,type=#mobs,tag=!spectator]". Передаёт шаг в `lbc:run_command`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast_2_notuser.mcfunction)

### `lbc:other/event/halloween/vex_wand/cast_2_onlyuser`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast_upgraded_2.mcfunction`; условие вызова: `execute if score #mode lbc.math matches 2 run function lbc:other/event/halloween/vex_wand/cast_2_onlyuser`. Обновляет NBT/components: modify storage lbc.math cmd set value "motion add ^ ^ ^1 @s". Передаёт шаг в `lbc:run_command`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast_2_onlyuser.mcfunction)

### `lbc:other/event/halloween/vex_wand/cast_2_windcharges`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast_upgraded_2.mcfunction`; условие вызова: `execute if score #mode lbc.math matches 4 at @s anchored eyes positioned ^ ^ ^ run function lbc:other/event/halloween/vex_wand/cast_2_windcharges`. Создаёт `wind_charge` с {Tags:["windcharges"]}. Обновляет NBT/components: modify entity @s Owner set from entity @p[tag=rcaster]. Состояние scoreboard: scoreboard players operation @e[type=wind_charge,tag=windcharges,tag=!actived,distance=..0.01,limit=5] lbcID2 = @s lbcID; scoreboard players operation @e[type=wind_charge,tag=windcharges,tag=!actived,distance=..0.01,limit=5] team_number = @s . Передаёт шаг в `lbc:raycast_vpered_razbros_with_active`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast_2_windcharges.mcfunction)

### `lbc:other/event/halloween/vex_wand/cast_2meditate`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast.mcfunction`; условие вызова: `execute if items entity @s[predicate=lbc:sneak] weapon.* *[custom_data~{vex_wand:1,upgradable:1}] run function lbc:other/event/halloween/vex_wand/cast_2meditate`. Обновляет NBT/components: modify storage lbc.math cmd set value "motion set ^ ^ ^10 @s". Передаёт шаг в `lbc:other/magic_academy/can_upgrade`, `lbc:run_command`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast_2meditate.mcfunction)

### `lbc:other/event/halloween/vex_wand/cast_2meditate_at`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast_2meditate_at.mcfunction)

### `lbc:other/event/halloween/vex_wand/cast_upgraded`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_upgraded.mcfunction`; условие вызова: `execute if score @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{vex_wand:1}}}}] mana >= vex_wand_mana_use lbc.math unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] at @s anchored eyes positioned ^ ^ ^ run function lbc:other/event/halloween/vex_wand/cast_upgraded`. Передаёт шаг в `lbc:other/magic_academy/change_mode_4`, `lbc:other/event/halloween/vex_wand/cast_upgraded_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast_upgraded.mcfunction)

### `lbc:other/event/halloween/vex_wand/cast_upgraded_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast_upgraded.mcfunction`; условие вызова: `execute unless predicate lbc:sneak run function lbc:other/event/halloween/vex_wand/cast_upgraded_2`. Состояние scoreboard: scoreboard players operation @s mana -= vex_wand_mana_use lbc.math; scoreboard players operation @s manamax += mage_set_bonus lbc.math. Передаёт шаг в `lbc:other/event/halloween/vex_wand/cast_2`, `lbc:other/event/halloween/vex_wand/cast_2_onlyuser`, `lbc:other/event/halloween/vex_wand/cast_2_notuser`, `lbc:other/event/halloween/vex_wand/cast_2_windcharges`, `lbc:items/trinket_effects/cast_wand`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast_upgraded_2.mcfunction)

### `lbc:other/event/halloween/vex_wand/cast_upgraded_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_upgraded_left.mcfunction`; условие вызова: `execute if items entity @s weapon.offhand *[minecraft:custom_data~{vex_wand:1}] if score @s mana >= vex_wand_mana_use lbc.math unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] at @s anchored eyes positioned ^ ^ ^ run function lbc:other/event/halloween/vex_wand/cast_upgraded_left`. Передаёт шаг в `lbc:other/magic_academy/change_mode_4_left`, `lbc:other/event/halloween/vex_wand/cast_upgraded_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast_upgraded_left.mcfunction)

## Файлы: other / event / november

### `lbc:other/event/november/click`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_default.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{lightsaberrc:1}}}}] run function lbc:other/event/november/click`. Передаёт шаг в `lbc:other/event/november/click_sith`, `lbc:other/event/november/click_jedi`, `lbc:other/event/november/click_no_color`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/november/click.mcfunction)

### `lbc:other/event/november/click_jedi`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/november/click.mcfunction`; условие вызова: `execute if block ~ ~-1 ~ minecraft:blue_stained_glass run function lbc:other/event/november/click_jedi`. Loot: give lbc:lightsaber → @s. Состояние scoreboard: scoreboard players set @s november 1. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/november/click_jedi.mcfunction)

### `lbc:other/event/november/click_no_color`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/november/click.mcfunction`; условие вызова: `execute unless block ~ ~-1 ~ minecraft:blue_stained_glass unless block ~ ~-1 ~ minecraft:red_stained_glass run function lbc:other/event/november/click_no_color`. Изменяет блоки: setblock ^1 ^ ^2 minecraft:blue_stained_glass; setblock ^-1 ^ ^2 minecraft:red_stained_glass. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/november/click_no_color.mcfunction)

### `lbc:other/event/november/click_sith`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/event/november/click.mcfunction`; условие вызова: `execute if block ~ ~-1 ~ minecraft:red_stained_glass run function lbc:other/event/november/click_sith`. Loot: give lbc:lightsaber → @s. Состояние scoreboard: scoreboard players set @s november 2. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/november/click_sith.mcfunction)

### `lbc:other/event/november/give_sword`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_1s.mcfunction`; условие вызова: `execute if score @s lucky_block_count matches 1.. if score november lbc.event matches 1 run function lbc:other/event/november/give_sword`. Loot: give lbc:lightsaber → @s. Состояние scoreboard: scoreboard players set @s lucky_block_count -2147483646. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/november/give_sword.mcfunction)

## Файлы: other / event / tournament

### `lbc:other/event/tournament/end`

При положительном `tournament` в конце раунда функция добавляет (если ещё нет) статистику `tournament_kills` по убийствам игроков и dummy-цель `tournament_wins`, задаёт подписи и включает kills в sidebar, wins в player list. Она не удаляет цели и сама не начисляет победы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/tournament/end.mcfunction)

### `lbc:other/event/tournament/start`

При положительном `tournament` на старте создаёт цели убийств игроков и dummy-побед, задаёт им подписи `kills`/`wins` и очищает sidebar через `scoreboard objectives setdisplay sidebar` без цели. Оценка kills и список wins настраиваются в `end`; обе функции не записывают победы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/tournament/start.mcfunction)

## Файлы: other / halloween / eat

### `lbc:other/halloween/eat/chorus`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Создаёт `falling_block` с {BlockState:{Name:"minecraft:pumpkin"},HurtEntities:true,FallHurtAmount:5,FallHurtMax:10,Tags:["pumpkinfall"]}. Передаёт шаг в `lbc:other/halloween/eat/chorus_pumpkin_rand`. Завершение: сбрасывает advancement для повторного запуска. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/halloween/eat/chorus.mcfunction)

### `lbc:other/halloween/eat/chorus_pumpkin_rand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/halloween/eat/chorus.mcfunction`; условие вызова: `execute positioned ~ ~.5 ~ as @e[type=falling_block,limit=20,tag=pumpkinfall,distance=..0.01] at @s run function lbc:other/halloween/eat/chorus_pumpkin_rand`. Команда: `execute store result entity @s Motion[0] double 0.01 run random value -50..50`; `execute store result entity @s Motion[1] double 0.01 run random value 50..150`; `execute store result entity @s Motion[2] double 0.01 run random value -50..50`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/halloween/eat/chorus_pumpkin_rand.mcfunction)

## Troll Box: маски, случайное событие и Trollge

Снеговик с `custom_data.trollge:1` выбирает один из шести результатов с равной вероятностью: выдаёт маску и включает флаг `trollhunter`, `darkness`, `powerfall` или `abyss`, вызывает `empty_cage` либо строит стены. Маски задают собственные атрибуты: №1 +400% max health и speed; №2 +50% attack damage и +400% attack speed; №3 +100% attack damage и +400% armor; №4 — набор сильных положительных атрибутов и luck −1000, с binding curse; №5 снижает armor/toughness/max health и повышает attack speed/damage; №6 снижает speed на25%. Флаг `trollhunter` обслуживается раз в секунду вне Nexus/Imprinted: с вероятностью5% выбирается игрок и рядом появляется постоянный невидимый Wither Skeleton Trollge с 8 damage, скоростью II и невидимостью; первая подходящая сущность рассеивается в радиусе до10 блоков (в Nether с нижней границей Y=120). Его крики проверяются раз в секунду с вероятностью20%; среди девяти вариантов звуков равновероятны.

## Troll Box: параметры шести масок

Select выбирает1..6 равновероятно. Каждая выдаёт один unbreakable leather helmet с собственными моделью, tier и trollmask-ключом. Модификаторы действуют в head-slot; нижеприведённые проценты относятся к **базовому атрибуту** (add_multiplied_base), итог с другими modifiers отдельно не гарантируется.

| Mask | Модификаторы при ношении | Однократный исход select |
|---|---|---|
|1|HP+400%, speed+400%|trollhunter1 и общий титр.|
|2|attack+50%, attack-speed+400%|darkness1, darkness_player автору; очищает/немедленно запускает tick90s.|
|3|attack+100%, armor+400%|powerfall1 и титр.|
|4|speed+100%; knockback-resistance/attack/armor/toughness/HP+200%; luck−1000 add_value; Binding Curse|Ставит empty_cage всем non-spectator вне Nexus (наковальни/абсолютная вертикаль описаны в drops).|
|5|armor/toughness−80%; attack-speed+100%; HP−10%; attack+75%|По25 размещений walls с integrity0.995 вокруг каждого non-spectator вне Nexus; звук backrooms.|
|6|speed−25%|abyss1 и титр; при ношении Troll Mask6 дополнительно вызывает Eyelaser из player tick.|

Глобальные флаги остаются после select/снятия маски до внешнего reset; end_resets обнуляет trollhunter/darkness/powerfall/abyss. Когда runtime вызывает spawn_trollge_pre,5%-ная проверка выбирает случайного non-spectator и создаёт Trollge: persistent wither skeleton с20HP/8attack, долгой SpeedII/Invisibility и без equipment drops. Spawn размещает его наY+6 и через spreadplayers с maxRange10 перемещает вокруг исходной точки (Nether under120). Sound helper равновероятно выбирает9 комбинаций трёх scream-звуков/трёх pitches. Таймера удаления моба в этих файлах нет. Действие дополнительных hunter/tp-тегов находится в общем runtime.

## Файлы: other / troll_box

### `lbc:other/troll_box/1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/select.mcfunction`; условие вызова: `execute if score #random6 lbc.math matches 1 run function lbc:other/troll_box/1`. Выдаёт `minecraft:leather_helmet` ×1 игроку @s. Состояние scoreboard: scoreboard players set trollhunter lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/1.mcfunction)

### `lbc:other/troll_box/2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/select.mcfunction`; условие вызова: `execute if score #random6 lbc.math matches 2 run function lbc:other/troll_box/2`. Выдаёт `minecraft:leather_helmet` ×1 игроку @s. Состояние scoreboard: scoreboard players set darkness lbc.math 1. Передаёт шаг в `lbc:tick90s`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/2.mcfunction)

### `lbc:other/troll_box/3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/select.mcfunction`; условие вызова: `execute if score #random6 lbc.math matches 3 run function lbc:other/troll_box/3`. Выдаёт `minecraft:leather_helmet` ×1 игроку @s. Состояние scoreboard: scoreboard players set powerfall lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/3.mcfunction)

### `lbc:other/troll_box/4`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/select.mcfunction`; условие вызова: `execute if score #random6 lbc.math matches 4 run function lbc:other/troll_box/4`. Выдаёт `minecraft:leather_helmet` ×1 игроку @s. Передаёт шаг в `lbc:drops/empty_cage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/4.mcfunction)

### `lbc:other/troll_box/5`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/select.mcfunction`; условие вызова: `execute if score #random6 lbc.math matches 5 run function lbc:other/troll_box/5`. Выдаёт `minecraft:leather_helmet` ×1 игроку @s. Передаёт шаг в `lbc:other/troll_box/spawn_walls`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/5.mcfunction)

### `lbc:other/troll_box/6`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/select.mcfunction`; условие вызова: `execute if score #random6 lbc.math matches 6 run function lbc:other/troll_box/6`. Выдаёт `minecraft:leather_helmet` ×1 игроку @s. Состояние scoreboard: scoreboard players set abyss lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/6.mcfunction)

### `lbc:other/troll_box/select`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/projectiles/eggs.mcfunction`; условие вызова: `execute if data entity @e[type=minecraft:snowball,sort=nearest,limit=1] Item.components.minecraft:custom_data{"trollge":1} run function lbc:other/troll_box/select`. Передаёт шаг в `lbc:other/troll_box/1`, `lbc:other/troll_box/2`, `lbc:other/troll_box/3`, `lbc:other/troll_box/4`, `lbc:other/troll_box/5`, `lbc:other/troll_box/6`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/select.mcfunction)

### `lbc:other/troll_box/sound_trollge`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/mobs_1s.mcfunction`; условие вызова: `execute if entity @s[type=minecraft:wither_skeleton,tag=tp_skel_mask] if predicate lbc:chance20 run function lbc:other/troll_box/sound_trollge`. Команда: `execute store result score #random9 lbc.math run random value 1..9`; `execute if score #random9 lbc.math matches 1 run playsound minecraft:lbcsounds.scream_1 master @a ~ ~ ~ 2 1`; `execute if score #random9 lbc.math matches 2 run playsound minecraft:lbcsounds.scream_2 master @a ~ ~ ~ 2 1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/sound_trollge.mcfunction)

### `lbc:other/troll_box/spawn_trollge`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/spawn_trollge_pre.mcfunction`; условие вызова: `execute if predicate lbc:chance5 at @r[gamemode=!spectator] run function lbc:other/troll_box/spawn_trollge`. Создаёт `wither_skeleton` с {CustomNameVisible:0b,CustomName:{"color":"dark_red","italic":false,"text":"Trollge"},Tags:["tp_skel_mask","trollge_hunter"],Silent:1b,DeathLootTable:"empty",PersistenceRequired:1b. Передаёт шаг в `lbc:other/troll_box/trollge_spawn`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/spawn_trollge.mcfunction)

### `lbc:other/troll_box/spawn_trollge_pre`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/tick1s.mcfunction`; условие вызова: `execute if score trollhunter lbc.math matches 1 unless dimension minecraft:nexus unless dimension minecraft:imprinted run function lbc:other/troll_box/spawn_trollge_pre`. Передаёт шаг в `lbc:other/troll_box/spawn_trollge`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/spawn_trollge_pre.mcfunction)

### `lbc:other/troll_box/spawn_walls`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/5.mcfunction`; условие вызова: `execute as @a[gamemode=!spectator] at @s unless dimension nexus run function lbc:other/troll_box/spawn_walls`. Команда: `place template lbc:walls ~-5 ~-1 ~-5 none none 0.995`; `place template lbc:walls ~-16 ~-1 ~-5 none none 0.995`; `place template lbc:walls ~6 ~-1 ~-5 none none 0.995`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/spawn_walls.mcfunction)

### `lbc:other/troll_box/trollge_spawn`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/spawn_trollge.mcfunction`; условие вызова: `execute as @e[type=minecraft:wither_skeleton,tag=tp_skel_mask,tag=!tp_skel_ar,sort=nearest,limit=1] at @s run function lbc:other/troll_box/trollge_spawn`. Команда: `spreadplayers ~ ~ 0 10 under 120 false @s[predicate=lbc:the_nether]`; `spreadplayers ~ ~ 0 10 false @s[predicate=!lbc:the_nether]`; `tag @s add tp_skel_ar`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/trollge_spawn.mcfunction)

### `lbc:other/troll_box/trollmasksel`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction`; условие вызова: `execute if entity @s[tag=trollmask_active] unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] run function lbc:other/troll_box/trollmasksel`. Передаёт шаг в `lbc:items/arcanums/eyelaser_pre`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/trollmasksel.mcfunction)

