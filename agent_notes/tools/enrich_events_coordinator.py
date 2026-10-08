"""One-shot repair of event summaries after direct function reads."""
from pathlib import Path
r=Path(__file__).resolve().parents[2];p=r/'agent_notes/lbc_other_events.md'
s=p.read_text(encoding='utf-8-sig')
def paragraph(prefix,new):
    global s
    a=s.index(prefix);b=s.find('\n\n',a)
    if b<0:b=len(s)
    s=s[:a]+new+s[b:]
paragraph('`start` выдаёт всем', '''`start` выдаёт всем Halloween advancement и включает звук. Еда с custom_data.halloween_food:1 запускает consume-item advancement: функция прибавляет pumpkin_counter1 и при значении1..100 отмечает **одноимённый критерий одного** достижения true_advancements/event/eat_halloween_food. У него100 impossible-критериев без явного requirements: завершение требует всех100, затем reward вызывает gain/default_kit один раз. При counter>100 функция не добавляет критерий. Сам eat-trigger отзывается каждый обычный players_tick, разрешая повторное употребление.

`player_tick_1s` проверяет screamer_halloween item_display в2 блоках; **только при наличии** вызывает screamer_activate. Та удаляет один limit1 display, добавляет1..50 memcoins, показывает награду и равновероятно выбирает сцену1..7. В коде16 веток, но8..16 недостижимы из этого random и все эти helpers пустые. Сцены1..7: невидимый скелет; Darkness10с и TNT наY+240 с fuse170t; scared+60/lookback+10;8 silverfish с1HP и случайной Motion;6 wind-markers плюс LevitationVII1с;4 wind_charge; замена #solid на jack_o_lantern в кубе11³. Каждая играет свой звук. Wind-marker движется0.2 вперёд, поворачивается10° и удаляется с шансом4% за tick-call.''')
paragraph('Помечает ближайшего подходящего игрока `damager`', 'Находит игрока с совпадающим lbcID2 и временно помечает damager. Если в кубе1×1×1 есть mob без damager/spectator, вызывает explode; затем снимает damager у всех игроков. Screamer display эта функция не удаляет. [Исходник](../world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/jack_o_lantern_check.mcfunction)')
paragraph('Создаёт block_display-фонарь с тегами', 'Создаёт block_display-фонарь с slowraycast/rare/jack_o_lantern/shield_blockable; копирует Rotation, lbcID2 и team_number на display в0.001 блока, limit1, без actived. В этом spawn нет назначения damager и нет добавления actived; поиск владельца происходит позднее при проверке попадания. [Исходник](../world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/jack_o_lantern_spawn.mcfunction)')
paragraph('Раз в секунду добавляет игроку случайное целое', 'Раз в секунду проверяет наличие screamer_halloween display в2 блоках; при наличии вызывает screamer_activate. Без display ни награды, ни случайной сцены нет. Награду1..50 memcoins и random1..7 делает вызываемый helper, не этот проверяющий файл. [Исходник](../world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/player_tick_1s.mcfunction)')
paragraph('Advancement `eat_halloween_food` запускается', 'Consume-item advancement для custom_data.halloween_food:1 вызывает этот reward. Функция прибавляет pumpkin_counter1 и отмечает criterion1..100 в одном achievement, через return run. Награда default kit относится к завершению всех100 критериев, а не к каждому предмету. При counter>100 новых критериев нет. [Trigger](../world/datapacksdisabled/LBC/data/lbc/advancement/eat_halloween_food.json) · [Все100 критериев/reward](../world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/event/eat_halloween_food.json) · [Исходник](../world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction)')
insert='''## Торговец Bucks

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

'''
anchor='## Файлы: other / bucks';assert anchor in s
s=s.replace(anchor,insert+anchor,1)
p.write_text(s,encoding='utf-8')
print('events source-backed summaries and misleading cards corrected')
