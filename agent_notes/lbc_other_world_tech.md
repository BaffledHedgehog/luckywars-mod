# LBC: lbc other world tech

Задание `lbc_other_world_tech`: проверены ровно 253 источников из `agent_notes/inventory/research_jobs.json`. В описаниях caller означает обнаруженную ссылку в проиндексированных функциях; если его нет, возможность ручного запуска остаётся. Условие caller приведено из literal command. `dx/dy/dz` вместе с `distance` задают совместное пересечение, а `execute as` сам по себе не переносит позицию. `/effect give` измеряет длительность в секундах; `fuse:2s` в SNBT — Short на два тика (0,1 с), тогда как `schedule 2s` — две секунды.


## Nexus: выбор оружия и время восстановления

`other/nexus/rc` обслуживает зарегистрированный клик `RC_Raycast>=1`: проверяет предметы в обеих руках и последовательно вызывает все подходящие ветки roadgun/corrupter/orb gun/pulsar. В конце сбрасывает `RC_Raycast=0`; проверки не образуют взаимоисключающий выбор одного предмета. Для orb gun `pre` начинает с `Distance=0`, ставит тег `caster` и выбирает ровно одну из двух веток: при sneak запускает разбор предметов лучом, иначе стреляет; после ветки снимает тег caster. Pulsar также начинает луч из глаз игрока и ставит `pulsar_cd=15`. Ежесекундный `tick_1s_player` уменьшает cooldown у roadgun, corrupter и pulsar, то есть 15 — это 15 секунд; для каждого предмета отдельная функция задаёт свой лимит и сброс. [Dispatcher](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rc.mcfunction), [orb gun](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/pre.mcfunction), [pulsar](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/pulsar/pre.mcfunction), [секундное обслуживание cooldown](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tick_1s_player.mcfunction).


Roadgun создаёт block_display, копирует ID/team/Rotation, сажает игрока на него, даёт Slow Falling15с и ставит cooldown15. При обслуживании Lifetime≤35 display продвигается на15 блоков за вызов; до36 создаёт13 частичных шаблонов white-concrete куба7×7×7 с integrity0.002, шаг между точками3 блока. Sneak найденного по ID владельца удаляет display; на Lifetime39 найденный owner даёт вход в roadgun_end, но executor остаётся display: ride dismount применяется к нему, а не к игроку. Pad5×1×5 ставится в позиции display, временно forceload-ив его чанк. Игрок окончательно теряет mount при удалении display на40; ранний sneak удаляет его без pad. На40 display удаляется. За пределами Nexus обслуживание идёт через rare-dispatcher, поэтому нельзя переводить Lifetime40 в40 игровых тиков без проверки его периода.

Corrupter трассирует луч шагом0.5 до400 по кубу1×1×1 или до solid-блока. При попадании ставит Distance1000, создаёт sim_cube marker и только тогда cooldown20с; без попадания cooldown не устанавливается. Каждый sim_cube ставит black concrete и независимо с шансом50% создаёт потомка по каждому из6 соседних направлений, если там ещё не black concrete; уничтожает не-mobs в1 блоке кроме nexus_rc/nexus_entity, наносит10 outside_border одному mob без owner/team-фильтра и удаляет себя. Шанс0.25% удаляет все sim_cube markers без радиуса. Добавленный caster-tag эти локальные helpers не снимают. Pulsar аналогично ищет solid-блок; в точке попадания передаёт внешний макро-command `pulsar` и ставит Distance58395, а cooldown15 выставляется после луча и при промахе. Внутренний эффект команды описывает [граница command bridges](COMMAND_BRIDGES.md), эти helpers не задают его сами.


## Wisp book: принадлежность, круг и пассивные ветки

Книга создаёт шесть видов item_display-wisp: air, fire, water, nature, light и shadow. Для каждого вызова суммарно считает wisps владельца (`lbcID2`) в радиусе30 и разрешает spawn, если уже не больше11; локальный предел получается12 любых типов вместе в радиусе30; удалённые wisps этим ограничением не учитываются, а следующий одноимённый предмет возвращается как loot соответствующей стихии. При создании/удалении пересчитывает свойства владельца. Spawn выбирает display по общему тегу `new` без ограничения дальности или сортировки: если одновременно остаётся несколько таких display, ID владельца может достаться другому display. Орбита пересчитывается с шагом `floor(360/n)` градусов, а `players_tick` каждый тик прибавляет к угловому score каждой wisp3° и teleport ставит display на круг радиуса1,5 блока с центром на1 блок выше глаз владельца; при делении scoreboard остаток градусов теряется. `raycasters_5t` рисует частицу по типу display.

Пассивные эффекты отличаются по типу и периоду: огонь добавляет владельцу +4/+7/+9/+10 attack damage при1/2/3/4 wisps, а при5+ задаёт `floor((n+16)/2)`; вода использует `add_multiplied_base`: увеличивает базовый max health на40/70/90/100% при1/2/3/4 wisps, при5+ — на `(80+5n)%`. Air накапливает `air_wisp_count` каждые2 тика (для1..4 wisps это6/10/13/15; при5+ `n+11`), на100 заряда тратит100 и строит синхронный луч с базовым лимитом3 цепных попадания плюс `floor(n/3)`, каждое наносит4 generic-урона; его шаг —0,25, потолок около12,5 блока. Nature проверяет раз в секунду шанс20/30/35/38% для1..4 wisps и +2 процентных пункта на каждую сверх4; при успехе равновероятно выбирает один из семи результатов (включая `effect_boost_mana=600`, Resistance/Regeneration II10с, Fire Resistance30с, Absorption X30с, Night Vision60с, Saturation II10с). Light проверяет раз в2 секунды: `random 0..99 >=25` означает75% chance; число духов открывает накопительно четыре группы очистки: с1 spirit — Hunger/Poison/Wither; с2 также Blindness/Darkness/Glowing и score `piss`; с3 также Mining Fatigue/Weakness/Unluck; с4 также24 отрицательных score, в том числе снятие маны, отложенная смерть, взрывы, молнии, ловушки, timestop и `nomagic` (полный перечень — в карточке `remove_debuff/4`). Shadow при нулевом cooldown каждый тик добавляет `n` заряда, пока заряд меньше `100n`, затем ограничивает его сверху500; прибавление может слегка перескочить `100n`. Поглощение проверяется при заряде≥100 независимо от cooldown: выбирается один projectile в10 блоках без сортировки, и только если его Owner отличается от UUID игрока, projectile уничтожается, расходуется100 заряда и cooldown ставится в `100−7n` тиков. Cooldown задерживает пополнение, но не расход уже накопленного заряда. Свой случайно выбранный projectile может помешать поглотить находящийся рядом чужой. Старый `consume_magic` в активной проверке сейчас не вызывается: связанные с magic ветки в `shadow/check` закомментированы, и буквального caller для helper в проверенных LBC-функциях нет. При выходе/смерти все шесть `all` удаляют display по ID и сбрасывают доступные связанные счётчики/атрибуты; точная общая очистка перечислена по функциям ниже. [Перестройка круга](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/start.mcfunction), [ежетиковая орбита](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/teleport/run.mcfunction), [пассивные ветки](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_1s.mcfunction), [очистка при выходе](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction).


## Magnum: режимы, боеприпасы и лучевые выстрелы

`sneak` на `check_shift`/`check_shift_left` меняет режим вперёд/назад через Academy mode helper; без sneak выстрел возможен только при `coldownMagnum=0`. `check_ammo` требует одновременно текущий `mode` и соответствующий stack `custom_data` в инвентаре; режимы 0–5 — standard, incendiary, electric, acid, void, explosive. В отличие от этого, `check_ammo_2` внутри короткой серии проверяет только mode, без патронного стека. У обычного standard-выстрела расходуется одна `standard_round`, запускается луч, поворачивается угол игрока на −3°, звучат выстрелы, выдаётся advancement и ставится `coldownMagnum=25`; на каждом вызове cooldown-handler уменьшаются cooldown всех игроков, а handler вызывается для каждого игрока с положительным cooldown. Поэтому несколько активных Magnum ускоряют общий отсчёт; это не строго один decrement на игрока за тик. Луч идёт по 0,5 блока до `Distance=400` (примерно 200 блоков), останавливается на блоке или marker `stopper`, наносит мобу 16 arrow-урона; проверка цели объединяет `dx=dy=dz=0` с `distance=..15`, поэтому здесь действует куб 1×1×1 у точки, а не широкий радиус 15. Иные типы патронов направляются в отдельные `cast/check/hit`-цепочки ниже и имеют собственные результаты/расход.

Опорные исходники: [mode checks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_shift.mcfunction), [ammo dispatcher](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_ammo.mcfunction), [alternate dispatcher](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_ammo_2.mcfunction), [standard ray](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/standard/loop.mcfunction), [standard ray checks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/standard/check.mcfunction), [standard shot output](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/standard/cast.mcfunction), [tick cooldown](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/cooldown.mcfunction).


## Magnum: что делает каждый тип патрона

Обычный путь проверяет выбранный режим и наличие патрона в инвентаре; левый клик требует Magnum в offhand, правый — в mainhand. Но обе ветки стрельбы вызывают один check_ammo, который читает mode из SelectedItem главной руки; offhand влияет отдельно только на смену режима при sneak. Всего шесть режимов: standard наносит 16 arrow-урона; incendiary наносит 12 generic-урона, ставит `Fire=401` и при попадании в блок заменяет отмеченные теги блоков на воздух/огонь; electric цепляет цели через внешний `lbcr:raycast_preloop10magnum`; acid делает 6 generic-урона и вызывает внешний `lbcr:raycast_preloop11`; void наносит 4 `outside_border`, если у цели нет `void_resistance` и тега `sin_greed_active`, а при block hit `fill ~.4..~-.4` преобразует мировые координаты в целые block positions: очистка может захватить 1 или 2 блока по каждой оси в зависимости от позиции луча; это не объём меньше одного блока; explosive при попадании создаёт немедленно взрывающегося creeper радиуса4. Все лучи двигаются синхронно по0,5 блока с максимумом Distance400 (около200 блоков); `dx=dy=dz=0` вместе с `distance=..15` оставляет проверку в кубе1×1×1 каждой точки луча. Standard/incendiary/void помечают цели hit и проходят сквозь мобов; friendly-filter у этих попаданий нет. Standard/incendiary останавливаются на блоке, void очищает блоки и продолжает луч. Acid/electric/explosive пытаются остановить луч при попадании через Distance401. Cleanup удаляет hit глобально только при Distance≥401; остановка из-за stopper раньше предела может оставить hit-теги. Режимы расходуют один соответствующий патрон и ставят cooldown соответственно 25/25/15/20/15/40 тиков для standard/incendiary/electric/acid/void/explosive. Исключение: `check_ammo_2` при `MagnumNyauwu` запускает режим по выбранному оружию без проверки или расхода патрона; cooldown-handler вызывает до4 повторов по MagnumNyauwu, уменьшая его на1 за вызов; cast каждого повтора вновь устанавливает полный cooldown. Обычный повтор при положительном cooldown не тратит патрон; искусственный вызов при cooldown≤0 снова расходует его. У explosive отдельный `hit_block` с радиусом2 существует, но его проверенный ray-loop вызывает `hit_entity` и при блоке, и при сущности, так что штатный выстрел использует радиус4.

## Laser minigun: разгон, ресурс и выстрел

`shoot/click` начинает/продлевает раскрутку: проигрывает звук при cooldown=0, ставит окно 6 тиков, `revved` и расписание тика. Пока cooldown >0, `shoot/run` наращивает spin; после окна spin убывает. На значении 30 `full_rev` пытается стрелять при mana≥100. Каждый `cast` тратит 100 mana, удерживает spin на 30, уменьшает `laser_minigun_spread` на 100 только от значения 1100+, выдаёт shooter advancement при spread≤1000 и создаёт ray-marker с ID, командой и rotation владельца. Поэтому этот ствол тратит ману и копит spread, а не боеприпасы. [Вход раскрутки](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/click.mcfunction), [тик spin/cooldown](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/run.mcfunction), [порог полной раскрутки](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/full_rev.mcfunction), [расход и spawn луча](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/cast.mcfunction).

## Techrunner Katana и Cyborg boss

`techrunner_katana` получает заряд при событии items/swords с предметом в mainhand без stopper20: обычный вход — advancement gowno (незаблокированный не-arrow урон игрока по не-interaction вне Nexus/Imprinted), также есть вызовы TMT. Каждый такой вызов добавляет swordcharging6; на3 сбрасывает прогресс и прибавляет5 swordcharged6. Это заряд от боевых событий, а не каждый тик удержания. Каждый charge также сбрасывает movement-speed amount на оружии в0.05. `spell1_default`/`spell1_default_left` разрешают dash за1 заряд и без `stopper` в20 блоках: игрок телепортируется на5 блоков по взгляду, затем трижды пробует подняться на1 блок при столкновении. Каждый dash задаёт movement-speed amount как `0.01×(floor(100×старое_amount)+2)` — приблизительно прибавляет0.02, с округлением и снимает один заряд. Оба hand-входа используют один use-helper, читающий и перезаписывающий mainhand: offhand-dash не меняет атрибуты самой offhand-катаны. Отдельно пока меч выбран, `player_speed` пересчитывает attack-damage modifier самого игрока как `floor((floor(speed/18)^2+1)/2)`; диапазон `speed` проверяется в конкретном caller, здесь не используется как плавное число.

`test_create_boss` вызывается advancement place_techno_cyborg при взаимодействии со стойкой предметом techno:1 вне Nexus/Imprinted. Он выбирает все подходящие armor stands без ограничения расстояния: ровно по1 leather helmet/chestplate/leggings/boots с techno:1; превращение не ограничено только стойкой, по которой кликнули. Он заменяет stand на skeleton босса с75 HP, fire resistance, скопированной бронёй и поворотом, даёт ID и запускает каждотиковый scheduler. При `HurtTime:10s` вызывает внешний angry raycast. Если несspectator игрок находится в3 блоках и `cyborgBossFly=0`, босс выполняет прыжковую атаку: levitation amplifier30 на1с, Resistance V на6с, 8 cactus-урона чужим мобам до10 и Blindness5с; затем блокирует следующую атаку на200 тиков. Для босса с `mk2` каждый цикл дополнительно вызывает внешний shield helper. При смерти цикл прекращается, когда следующий scheduled dispatcher больше не находит skeleton.

## Laser Minigun: разброс и луч

Правый клик spell1_default с laser_minigun в главной руке перезапускает 6-тиковое окно раскрутки. Одного удержания без повторных кликов недостаточно. Если новые клики прекращаются, spin быстро убывает, звук снижения заканчивает режим и сбрасывает `laser_minigun_spread` на12000; пока клики поддерживают окно и spin достиг30, при mana≥100 каждый tick списывается100 mana и создаётся отдельный синхронный луч. Выстрел уменьшает spread на100, пока он≥1100; на≤1000 выдаёт shooter advancement. `random -spread..spread` добавляет к yaw и pitch независимый разброс в тысячных долях градуса: spread12000 означает±12°, spread1000 —±1°, луч идёт по0,25 блока до Distance400 (около100 блоков), останавливается на блоке или подходящей в кубе1×1×1 цели; попадание пытается нанести4 generic от игрока. Первичная проверка выбирает вражеских mobs в кубе, но hit-helper повторно выбирает до3 сущностей любого типа в этом кубе и проверяет ID/team ближайшей к каждой точки сущности без type/sort; это не строгая гарантия урона ровно первой найденной mob-цели. Непопадание или попадание создаёт item_display-визуал, поставленный у глаз найденного raycaster; он до6 вызовов уменьшает ширину, без повторного следования за игроком; `stopper` в20 блоках приостанавливает как тики оружия, так и визуальную очередь.

## Supply crate: таймер, приземление и содержимое

Появление supply beacon передаёт display в supply-crate tick; `as_crate` задаёт `supplyFall=99` и сразу начинает `run`. Дальше расписание раз в тик увеличивает счётчик: на 100 звучит предупреждение, на 146–149 по очереди рисуются облака сверху вниз, на 150 ящик фиксируется barrier-колонной высотой 2 и наносит 10 generic урона целям в пределах 3 и 900 целям в пределах 1; на 200 модель меняется на открытую и появляются interaction плюс armor stands с Magnum и Techrunner Katana. `open` снимает тег `new`, поэтому повторная scheduled цепочка перестаёт подбирать этот display. Здесь появляются display/interaction и два armor stand с визуальными предметами; сама функция `open` не переносит их в инвентарь. Прямой JSON-handler convert_arrows_supply есть: клик interaction supply_drop стеком минимум2 arrow вне Nexus/Imprinted запускает обмен стрел на патроны, но не получение оружия со стоек. Один проход пытается удалить2 arrow и создаёт lbc:magnum_ammo у ближайшего non-spectator игрока; повторяется до32 проходов, пока хотя бы один container-стек содержит2 arrow. Infinite_arrow отключает clear, но не выдачу, поэтому при наличии обычного стека≥2 возможны32 бесплатные попытки. Механизм снятия Magnum/Katana со стоек этими функциями не подтверждён. [Создание beacon](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/risk_of_rain/run.mcfunction), [инициализация счётчика](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/risk_of_rain/as_crate.mcfunction), [таймер](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/supply_crate/run.mcfunction), [приземление](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/supply_crate/land.mcfunction), [открытие](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/supply_crate/open.mcfunction).

## Orb Gun: опыт, выстрел и разбор предметов

Обычный `shoot` читает XP points `N` текущего уровня и сперва посылает макро-команду списать `N+1` point через `experience add`. Затем он записывает `floor(N/2)` в storage для урона, но исходный scoreboard `tmp` остаётся `N`: проверка ветки стреляет при `N>=1`, а не по половине XP. Поэтому `N=1` уже запускает ray с уроном0; при `N=0` идёт `shoot_cant`, хотя списание `N+1` уже было запрошено. Ray применяет в storage-урон `floor(N/2)` как `outside_border` к мобам в радиусе10 от точки попадания. Луч идёт вперёд по0,33 блока до Distance600 (около198 блоков), останавливается на блоке или цели в кубе1×1×1 с дополнительным `distance=..10`; зона попадания даёт звук и 30 визуальных случайных линий длиной до15,5 блока. Sneak-ветка вместо выстрела синхронно разбирает выброшенные предметы в радиусе2 вдоль луча до Distance80 (8 блоков), заменяя каждый предмет loot-предметами рецепта; стаки обрабатываются по одной единице за рекурсивный вызов. Во внешнем `uncrafted` loot находится точное содержимое рецепта, а не в этих 25 функциях.

## Nuke: задержка, варианты и область действия

`nuke` и `nuke_bruh` — разные marker triggers: обычная версия добавляет `nuke1` и планирует `nuke1` через20 секунд только если рядом нет `stooper` в радиусе10; `nuke_bruh` задерживает свою ветку на35 секунд. При выполнении `nuke1_at` создаётся один marker для ray/particles, несколько `lbc:air` шаблонов на дистанциях25 и60 по шести направлениям, эффект Wither amplifier30 (уровень31) на20 секунд сущностям в45 блоках, 24 Creeper с Fuse1 тик и радиусом25, затем удаляются другие сущности в радиусе14 с указанными tag-исключениями. В `bruh`-версии эффект вызывается до16 раз из случайных сущностей без `spectator` tag (selector не ограничен игроками), а радиус Creeper там16. `nuke_old`/`nuke1_old` — отдельная старая команда с тремя fill-зонами воздуха и Creeper радиуса30; `nuke_eoe` вызывается Everythingend и ставит base из netherite, beacon и red stained glass pane, удаляет блоки по вертикали до абсолютной высоты319 (затем повторяет fill до255) на случайных сущностях без фильтра игроков, планируя общий этап20s. Не смешиваю эти trigger paths с `nuket`/`nuket_2`: это отдельная marker-поддержка; весь блоковый ущерб/эффект зависит от внешних raycaster/marker dispatchers и template `lbc:air`.

Дополнительные точные различия Nuke: обычная ветка выполняет12 air-template размещений (6×дальность25 с integrity0.5 и6×60 с0.25), bruh-попадание —13 (6+7). Каждое создаёт24 creeper: радиусы расположения39/18 по четырём сторонам на высотах0/+40/−40, ExplosionRadius25 у обычной и16 у bruh. В обеих ветках Wither31 на20с до45 блоков исключает только spectator-tag, не союзников. Старый nuke_old задерживается20с без проверки stooper, очищает3 fill-зоны и создаёт9 creeper радиуса30; Wither у него не исключает spectator. Nuket_2 строит пятислойный beacon pedestal, первые20 Lifetime-вызовов чистит колонну доY319 и каждый проход выбирает один из16 цветов стекла. Автоматический nuke1_at cleanup исключает игроков/creeper/strict_map_object и slowparticlesexplode_2, но не другие friendly entities.

## Railroad: временный строитель пути

Тег `railroad` обслуживается raycast-selector только при игроке не-spectator не дальше75 блоков. Первый tick увеличивает data.railtime до1, выравнивает положение к центру блока, берёт rotation ближайшего игрока и строит перед собой место для minecart; на шагах2..199 (10 секунд при тикающем вызове раз в тик) каждый проход расчищает текущий блок и2 блока над ним, меняет water на glass и lava на orange glass, ставит planks и выбирает straight/up/down rail по воздуху впереди, уклону, близости высоты сборки и наличию `prev_down`. Ветка `inf_rail=1` снимает верхний предел199, но корректное исчезновение trail/marker тогда уже не происходит по обычному таймеру. При отсутствии игрока в75 блоках вызов не выполняется и таймер стоит. Каждый7-й строительный шаг ставит redstone block для питания rails; обычный путь удерживается выше Y30, infinite выше−34, подъём ограничен Y318. В конце обычной ветки строится oak-wood колонка из2 блоков и marker удаляется; при спуске отдельный цикл ставит oak log вниз до опоры или до исчерпания счётчика.

## Test Ray: вспомогательный визуальный луч

У click нет найденного внешнего автоматического caller; внутренние9 helpers доступны вручную. Он создаёт test_ray display в(0,−800,0), копирует ID/team и проверяет до400 точек по0.25 блока до solid-блока, без урона. Затем соответствующие ID displays ставятся у глаз raycaster, получают длину scaleZ=0.5×Distance и death_timer20; width уменьшается каждый вызов visual/run, на0 display удаляется. Выбор неготовых display/raycaster глобальный и limit1 без sort. Hit оставляет caster-у тег check_id, который click локально не снимает; зато raycaster после луча снимается. [Вход](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/click.mcfunction), [hit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/hit.mcfunction).

## Файлы: other / nexus

### `lbc:other/nexus/craft_white_wire`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/tick_nexus.mcfunction`; условие вызова: `execute as @e[type=minecraft:item,distance=0..,nbt={Item:{components:{"minecraft:custom_data":{nexus_wire_red:1}}}}] at @s if entity @e[type=minecraft:item,distance=..3,nbt={Item:{components:{"minecraft:custom_data":{nexus_wire_blue:1}}}}] if entity @e[type=minecraft:item,distance=..3,nbt={Item:{components:{"minecraft:custom_data":{nexus_wire_black:1}}}}] run function lbc:other/nexus/craft_white_wire`. Loot: spawn lbc:nexus_wire_white → ~. Завершение: удаляет сущность (`@s[nbt={Item:{count:1}}]`); удаляет сущность (`@e[type=minecraft:item,distance=..3,limit=1,nbt={Item:{components:{"minecraft:custom_data":{nexus_wire_blue:1}},count:1}}]`); удаляет сущность (`@e[type=minecraft:item,distance=..3,limit=1,nbt={Item:{components:{"minecraft:custom_data":{nexus_wire_black:1}},count:1}}]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/craft_white_wire.mcfunction)

### `lbc:other/nexus/explode_wire`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/tick_nexus_raycasters.mcfunction`; условие вызова: `execute if entity @s[tag=explose_wired] run function lbc:other/nexus/explode_wire`. Создаёт `marker` с {Tags:["explose_wired","nexus_rc"]}. Изменяет блоки: fill ~ ~ ~ ~ ~ ~ minecraft:black_concrete strict. Передаёт шаг в `lbc:other/nexus/spawn_wire_black`. Завершение: удаляет сущность (`@e[type=#minecraft:inter_itdisp,distance=..2.5]`); удаляет сущность (`@e[type=minecraft:marker,distance=..0.01,tag=explose_wired]`); удаляет сущность (`@e[type=minecraft:marker,tag=explose_wired]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/explode_wire.mcfunction)

### `lbc:other/nexus/interacted_black`

Прямой вход — advancement `interaction_nexus_frame_black`: не-spectator игрок взаимодействует с `wire_converter_black`, держа предмет `{nexus_wire_black:1}`; reward calls this function. Автоматического вызова через `.mcfunction` caller нет. Передаёт шаг в `lbc:other/nexus/interacted_frame_at`. Завершение: расходует/очищает предметы; сбрасывает advancement для повторного запуска. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/interacted_black.mcfunction)

### `lbc:other/nexus/interacted_frame_at`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/interacted_black.mcfunction`; условие вызова: `execute anchored eyes positioned ^ ^ ^3 as @e[type=minecraft:interaction,distance=..10,tag=wire_converter_black,sort=nearest,limit=1] at @s run function lbc:other/nexus/interacted_frame_at`. Создаёт `item` с {NoGravity:1b,Item:{id:"minecraft:tnt",count:1},Tags:["unmodified_item"]}. Обновляет NBT/components: modify entity @e[type=minecraft:item,distance=..0.001,tag=unmodified_item,sort=nearest,limit=1] Item set from entity @e[type=mi. Завершение: удаляет сущность (`@e[type=minecraft:item_display,distance=..1]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/interacted_frame_at.mcfunction)

### `lbc:other/nexus/interacted_green`

Прямой вход — advancement `interaction_nexus_frame_green`: не-spectator игрок взаимодействует с `wire_converter_green`, держа `{nexus_wire_blue:1}`; reward calls this function. Автоматического вызова через `.mcfunction` caller нет. Передаёт шаг в `lbc:other/nexus/interacted_frame_at`. Завершение: расходует/очищает предметы; сбрасывает advancement для повторного запуска. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/interacted_green.mcfunction)

### `lbc:other/nexus/interacted_red`

Прямой вход — advancement `interaction_nexus_frame_red`: не-spectator игрок взаимодействует с `wire_converter_red`, держа `{nexus_wire_red:1}`; reward calls this function. Автоматического вызова через `.mcfunction` caller нет. Передаёт шаг в `lbc:other/nexus/interacted_frame_at`. Завершение: расходует/очищает предметы; сбрасывает advancement для повторного запуска. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/interacted_red.mcfunction)

### `lbc:other/nexus/interacted_white`

Прямой вход — advancement `interaction_nexus_frame_white`: не-spectator игрок взаимодействует с `wire_converter_white`, держа `{nexus_wire_white:1}`; reward calls this function. Автоматического вызова через `.mcfunction` caller нет. Передаёт шаг в `lbc:other/nexus/interacted_frame_at`. Завершение: расходует/очищает предметы; сбрасывает advancement для повторного запуска. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/interacted_white.mcfunction)

### `lbc:other/nexus/rc`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/tick_nexus_player.mcfunction`; условие вызова: `execute if score @s RC_Raycast matches 1.. run function lbc:other/nexus/rc`. Состояние scoreboard: scoreboard players set @s RC_Raycast 0. Передаёт шаг в `lbc:other/nexus/click/roadgun`, `lbc:other/nexus/click/corrupter`, `lbc:other/nexus/click/orb_gun/pre`, `lbc:other/nexus/click/pulsar/pre`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rc.mcfunction)

### `lbc:other/nexus/spawn_blue_wire`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/tick_nexus_raycasters.mcfunction`; условие вызова: `execute if entity @s[tag=center_cube] if block ~ ~ ~ minecraft:air run function lbc:other/nexus/spawn_blue_wire`. Создаёт `marker` с {Tags:["explose_wired","nexus_rc"]}. Loot: spawn lbc:nexus_wire_blue → ~. Обновляет NBT/components: modify entity @e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{components:{"minecraft:custom_data":{nexus_wire_blue:1}}}}. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/spawn_blue_wire.mcfunction)

### `lbc:other/nexus/spawn_cube`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/pidor_clone_rand.mcfunction`; условие вызова: `$execute in nexus positioned $(tmpx) $(tmpy) $(tmpz) positioned ~10 ~9 ~10 run function lbc:other/nexus/spawn_cube`. Состояние scoreboard: scoreboard players add #nexus_id lbc.math 1. Передаёт шаг в `lbc:other/nexus/spawn_cube_save_coords`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/spawn_cube.mcfunction)

### `lbc:other/nexus/spawn_cube_save_coords`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/spawn_cube.mcfunction`; условие вызова: `function lbc:other/nexus/spawn_cube_save_coords with storage lbc.math`. Обновляет NBT/components: modify storage lbc:nexus x$(nxs) set from storage lbc.math tmpx; modify storage lbc:nexus y$(nxs) set from storage lbc.math tmpy; modify storage lbc:nexus z$(nxs) set from storage lbc.math tmpz. Использует макропараметры `$(nxs)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/spawn_cube_save_coords.mcfunction)

### `lbc:other/nexus/spawn_wire_black`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/explode_wire.mcfunction`; условие вызова: `execute if predicate lbc:chance0_015625 run function lbc:other/nexus/spawn_wire_black`. Loot: spawn lbc:nexus_wire_black → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/spawn_wire_black.mcfunction)

### `lbc:other/nexus/tick_1s_player`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/tick1s.mcfunction`; условие вызова: `execute as @a[gamemode=!spectator] at @s if dimension minecraft:nexus run function lbc:other/nexus/tick_1s_player`. Состояние scoreboard: scoreboard players remove @s nexus_roadgun_cd 1; scoreboard players remove @s nexus_corrupter_cd 1; scoreboard players remove @s pulsar_cd 1; scoreboard players set @s nexus_compass_cd 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tick_1s_player.mcfunction)

### `lbc:other/nexus/tree_end`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_tick_move.mcfunction`; условие вызова: `execute if predicate lbc:chance10 at @s run function lbc:other/nexus/tree_end`. Передаёт шаг в `lbc:other/nexus/tree_end_pre`, `lbc:other/nexus/tree_end_all`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_end.mcfunction)

### `lbc:other/nexus/tree_end_1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_end_pre.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 1 run function lbc:other/nexus/tree_end_1`. Создаёт `interaction` с {width:1.0f,height:1.0f,response:1b,Tags:["nexus_entity","wire_converter_red"]}; `item_display` с {Tags:["nexus_entity","wire_converter_item"],interpolation_duration:3,teleport_duration:3,item_display:"fixed",item:{id:"minecraft:tnt",count:1}}; `item_display` с {Tags:["nexus_entity","wire_converter_frame"],interpolation_duration:3,teleport_duration:3,item_display:"fixed",item:{id:"minecraft:repeating_command_block",components:{"minecraft:. Loot: spawn lbc:nexus_item_red → ~. Обновляет NBT/components: modify entity @e[type=minecraft:item_display,distance=..0.001,tag=wire_converter_item,limit=1] item set from entity @e[type=min. Завершение: удаляет сущность (`@e[type=minecraft:item,distance=..0.001,limit=1]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_end_1.mcfunction)

### `lbc:other/nexus/tree_end_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_end_pre.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 2 run function lbc:other/nexus/tree_end_2`. Создаёт `interaction` с {width:1.0f,height:1.0f,response:1b,Tags:["nexus_entity","wire_converter_green"]}; `item_display` с {Tags:["nexus_entity","wire_converter_item"],interpolation_duration:3,teleport_duration:3,item_display:"fixed",item:{id:"minecraft:tnt",count:1}}; `item_display` с {Tags:["nexus_entity","wire_converter_frame"],interpolation_duration:3,teleport_duration:3,item_display:"fixed",item:{id:"minecraft:repeating_command_block",components:{"minecraft:. Loot: spawn lbc:nexus_item_green → ~. Обновляет NBT/components: modify entity @e[type=minecraft:item_display,distance=..0.001,tag=wire_converter_item,limit=1] item set from entity @e[type=min. Завершение: удаляет сущность (`@e[type=minecraft:item,distance=..0.001,limit=1]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_end_2.mcfunction)

### `lbc:other/nexus/tree_end_3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_end_pre.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 3 run function lbc:other/nexus/tree_end_3`. Создаёт `interaction` с {width:1.0f,height:1.0f,response:1b,Tags:["nexus_entity","wire_converter_black"]}; `item_display` с {Tags:["nexus_entity","wire_converter_item"],interpolation_duration:3,teleport_duration:3,item_display:"fixed",item:{id:"minecraft:tnt",count:1}}; `item_display` с {Tags:["nexus_entity","wire_converter_frame"],interpolation_duration:3,teleport_duration:3,item_display:"fixed",item:{id:"minecraft:repeating_command_block",components:{"minecraft:. Loot: spawn lbc:nexus_item_black → ~. Обновляет NBT/components: modify entity @e[type=minecraft:item_display,distance=..0.001,tag=wire_converter_item,limit=1] item set from entity @e[type=min. Завершение: удаляет сущность (`@e[type=minecraft:item,distance=..0.001,limit=1]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_end_3.mcfunction)

### `lbc:other/nexus/tree_end_4`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_end_pre.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 4 run function lbc:other/nexus/tree_end_4`. Создаёт `interaction` с {width:1.0f,height:1.0f,response:1b,Tags:["nexus_entity","wire_converter_white"]}; `item_display` с {Tags:["nexus_entity","wire_converter_item"],interpolation_duration:3,teleport_duration:3,item_display:"fixed",item:{id:"minecraft:tnt",count:1}}; `item_display` с {Tags:["nexus_entity","wire_converter_frame"],interpolation_duration:3,teleport_duration:3,item_display:"fixed",item:{id:"minecraft:repeating_command_block",components:{"minecraft:. Loot: spawn lbc:nexus_item_white → ~. Обновляет NBT/components: modify entity @e[type=minecraft:item_display,distance=..0.001,tag=wire_converter_item,limit=1] item set from entity @e[type=min. Завершение: удаляет сущность (`@e[type=minecraft:item,distance=..0.001,limit=1]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_end_4.mcfunction)

### `lbc:other/nexus/tree_end_all`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_end.mcfunction`; условие вызова: `execute if predicate lbc:chance5 run function lbc:other/nexus/tree_end_all`. Изменяет блоки: fill ~1 ~1 ~1 ~-1 ~-1 ~-1 minecraft:air strict. Завершение: удаляет сущность (`@e[distance=..100,tag=nexus_tree]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_end_all.mcfunction)

### `lbc:other/nexus/tree_end_pre`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_end.mcfunction`; условие вызова: `execute unless entity @e[type=minecraft:marker,distance=..15,tag=center_cube] unless entity @e[distance=0.01..5,tag=wire_converter_frame] run function lbc:other/nexus/tree_end_pre`. Изменяет блоки: fill ~1 ~1 ~1 ~-1 ~-1 ~-1 minecraft:air strict. Передаёт шаг в `lbc:other/nexus/tree_end_1`, `lbc:other/nexus/tree_end_2`, `lbc:other/nexus/tree_end_3`, `lbc:other/nexus/tree_end_4`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_end_pre.mcfunction)

### `lbc:other/nexus/tree_tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/tick_nexus_raycasters.mcfunction`; условие вызова: `execute if entity @s[tag=nexus_tree] if entity @a[distance=..150] run function lbc:other/nexus/tree_tick`. Передаёт шаг в `lbc:other/nexus/tree_tick_move`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_tick.mcfunction)

### `lbc:other/nexus/tree_tick_move`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_tick.mcfunction`; условие вызова: `function lbc:other/nexus/tree_tick_move`. Создаёт `marker` с {Tags:["nexus_tree","nexus_rc"]}. Передаёт шаг в `lbc:other/nexus/tree_tick_move_macros`, `lbc:other/nexus/tree_end`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_tick_move.mcfunction)

### `lbc:other/nexus/tree_tick_move_macros`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_tick_move.mcfunction`; условие вызова: `execute at @s run function lbc:other/nexus/tree_tick_move_macros with storage lbc.math`. Изменяет блоки: fill ^ ^ ^ ^ ^ ^$(tmp) lime_concrete replace air strict. Использует макропараметры `$(tmp)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_tick_move_macros.mcfunction)

## Файлы: other / nexus / click

### `lbc:other/nexus/click/corrupter`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_default.mcfunction`; условие вызова: `execute unless score @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{nexus_corrupter:1}}}}] nexus_corrupter_cd matches 1.. run function lbc:other/nexus/click/corrupter`. Состояние scoreboard: scoreboard players set @s Distance 0; scoreboard players set @s[scores={Distance=1000}] nexus_corrupter_cd 20. Передаёт шаг в `lbc:other/nexus/click/corrupter_ray`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/corrupter.mcfunction)

### `lbc:other/nexus/click/corrupter_ray`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/corrupter.mcfunction`; условие вызова: `execute anchored eyes positioned ^ ^ ^ run function lbc:other/nexus/click/corrupter_ray`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/nexus/click/corrupter_ray_end`, `lbc:other/nexus/click/corrupter_ray`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/corrupter_ray.mcfunction)

### `lbc:other/nexus/click/corrupter_ray_end`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/corrupter_ray.mcfunction`; условие вызова: `execute if entity @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..10,tag=!spectator,tag=!caster] run function lbc:other/nexus/click/corrupter_ray_end`. Создаёт `marker` с {Tags:["nexus_rc","slowraycast","rare","sim_cube"]}. Состояние scoreboard: scoreboard players set @s Distance 1000. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/corrupter_ray_end.mcfunction)

### `lbc:other/nexus/click/nexus_compass`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/nexus_compass.mcfunction)

### `lbc:other/nexus/click/nexus_compass_2`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Передаёт шаг в `lbc:other/nexus/click/nexus_compass_3`. Использует макропараметры `$(tmp)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/nexus_compass_2.mcfunction)

### `lbc:other/nexus/click/nexus_compass_3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/nexus_compass_2.mcfunction`; условие вызова: `function lbc:other/nexus/click/nexus_compass_3 with storage lbc.math`. Передаёт шаг в `lbc:other/nexus/click/nexus_compass_4`. Использует макропараметры `$(tmp)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/nexus_compass_3.mcfunction)

### `lbc:other/nexus/click/nexus_compass_4`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/nexus_compass_3.mcfunction`; условие вызова: `function lbc:other/nexus/click/nexus_compass_4 with storage lbc.math`. Использует макропараметры `$(t1)`, `$(t2)`, `$(t3)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/nexus_compass_4.mcfunction)

### `lbc:other/nexus/click/roadgun`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_default.mcfunction`; условие вызова: `execute unless score @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{nexus_roadgun:1}}}}] nexus_roadgun_cd matches 1.. at @s run function lbc:other/nexus/click/roadgun`. Эффект: @s получает `minecraft:slow_falling` на 15 секунд, amplifier 0. Создаёт `block_display` с {Tags:["slowraycast","nexus_rc","roadgun","rare"],teleport_duration:1,interpolation_duration:1}. Обновляет NBT/components: modify entity @e[type=minecraft:block_display,distance=..0.01,tag=roadgun,tag=!actived,limit=1] Rotation set from entity @s Rot. Состояние scoreboard: scoreboard players set @s nexus_roadgun_cd 15. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/roadgun.mcfunction)

## Файлы: other / nexus / click / orb_gun

### `lbc:other/nexus/click/orb_gun/damage_area`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/shoot_rc.mcfunction`; условие вызова: `execute if entity @e[dx=0,dy=0,dz=0,distance=..10,tag=!caster,limit=1] run function lbc:other/nexus/click/orb_gun/damage_area`. Состояние scoreboard: scoreboard players add @s Distance 10000. Передаёт шаг в `lbc:other/nexus/click/orb_gun/damage_at`, `lbc:other/nexus/click/orb_gun/damage_area_rand_tentacle`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/damage_area.mcfunction)

### `lbc:other/nexus/click/orb_gun/damage_area_rand_tentacle`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/damage_area.mcfunction`; условие вызова: `function lbc:other/nexus/click/orb_gun/damage_area_rand_tentacle`. Передаёт шаг в `lbc:other/nexus/click/orb_gun/damage_area_rand_tentacle_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/damage_area_rand_tentacle.mcfunction)

### `lbc:other/nexus/click/orb_gun/damage_area_rand_tentacle_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/damage_area_rand_tentacle.mcfunction`; условие вызова: `function lbc:other/nexus/click/orb_gun/damage_area_rand_tentacle_2 with storage lbc.math`. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/nexus/click/orb_gun/damage_area_rand_tentacle_2rc`. Использует макропараметры `$(rot1)`, `$(rot2)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/damage_area_rand_tentacle_2.mcfunction)

### `lbc:other/nexus/click/orb_gun/damage_area_rand_tentacle_2rc`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/damage_area_rand_tentacle_2.mcfunction`; условие вызова: `$execute rotated $(rot1) $(rot2) positioned ^ ^ ^ run function lbc:other/nexus/click/orb_gun/damage_area_rand_tentacle_2rc`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/nexus/click/orb_gun/damage_area_rand_tentacle_2rc`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/damage_area_rand_tentacle_2rc.mcfunction)

### `lbc:other/nexus/click/orb_gun/damage_at`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/damage_area.mcfunction`; условие вызова: `execute as @e[type=#minecraft:mobs,distance=..10,tag=!caster,tag=!spectator] at @s run function lbc:other/nexus/click/orb_gun/damage_at with storage lbc.math`. Использует макропараметры `$(tmp)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/damage_at.mcfunction)

### `lbc:other/nexus/click/orb_gun/exp_remove_from_strg`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/shoot.mcfunction`; условие вызова: `function lbc:other/nexus/click/orb_gun/exp_remove_from_strg with storage lbc.math`. Использует макропараметры `$(tmp)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/exp_remove_from_strg.mcfunction)

### `lbc:other/nexus/click/orb_gun/pre`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_default.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{nexus_orb_gun:1}}}}] run function lbc:other/nexus/click/orb_gun/pre`. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/nexus/click/orb_gun/uncraft`, `lbc:other/nexus/click/orb_gun/shoot`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/pre.mcfunction)

### `lbc:other/nexus/click/orb_gun/shoot`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/pre.mcfunction`; условие вызова: `execute unless predicate lbc:sneak run function lbc:other/nexus/click/orb_gun/shoot`. Состояние scoreboard: scoreboard players add tmp lbc.math 1; scoreboard players remove tmp lbc.math 1. Передаёт шаг в `lbc:other/nexus/click/orb_gun/exp_remove_from_strg`, `lbc:other/nexus/click/orb_gun/shoot_rc`, `lbc:other/nexus/click/orb_gun/shoot_cant`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/shoot.mcfunction)

### `lbc:other/nexus/click/orb_gun/shoot_cant`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/shoot.mcfunction`; условие вызова: `execute unless score tmp lbc.math matches 1.. run function lbc:other/nexus/click/orb_gun/shoot_cant`. Команда: `tellraw @s {"translate":"not_enough_xp"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/shoot_cant.mcfunction)

### `lbc:other/nexus/click/orb_gun/shoot_rc`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/shoot.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 1.. anchored eyes positioned ^ ^ ^ run function lbc:other/nexus/click/orb_gun/shoot_rc`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/nexus/click/orb_gun/damage_area`, `lbc:other/nexus/click/orb_gun/shoot_rc`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/shoot_rc.mcfunction)

### `lbc:other/nexus/click/orb_gun/susp_stews`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction`; условие вызова: `execute if data storage lbc.math {item:"minecraft:suspicious_stew"} run function lbc:other/nexus/click/orb_gun/susp_stews`. Loot: spawn lbc:uncrafted/bowl_red_mushroom_brown_mushroom_allium → ~; spawn lbc:uncrafted/bowl_red_mushroom_brown_mushroom_azure_bluet → ~; spawn lbc:uncrafted/bowl_red_mushroom_brown_mushroom_dandelion → ~; spawn lbc:uncrafted/bowl_red_mushroom_brown_mushroom_cornflower → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction)

### `lbc:other/nexus/click/orb_gun/uncraft`

Орб-ган входит в эту ветку при удержании sneak: луч продвигает игрока вперёд по `Distance`, пока не встретит предмет в пределах 2 блоков или блок не из `#minecraft:airs`; тогда вызывает `uncraft_at`. Каждый предмет поблизости помечается `uncrafted` и отдельно разбирается. Это лучевое действие, а не пассивная переработка инвентаря. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft.mcfunction)
### `lbc:other/nexus/click/orb_gun/uncraft_at`

В точке попадания функция выбирает предметные сущности в радиусе 2 и вызывает для каждой разбор; после этого продолжение луча ограничено `Distance ≤ 80` и идёт шагом 0,1 блока. Ветка по блоку тоже запускается при столкновении с небезвоздушным блоком, но сам блок эта функция не добывает. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_at.mcfunction)
### `lbc:other/nexus/click/orb_gun/uncraft_f`

Для предметной сущности функция читает её `Item.id` и запускает таблицу обратного рецепта. Затем выдаёт частицы и расходует ровно одну единицу: стек из 1 уничтожается, больший стек уменьшается на 1 и функция вызывает себя снова. Поэтому каждый экземпляр обрабатывается отдельно; выход определяется идентификатором, а точные loot-выходы перечислены в [uncrafted-аудите](lbc_loot_uncrafted.md). [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f.mcfunction)
### `lbc:other/nexus/click/orb_gun/uncraft_f_2`

Перед разбором функции хранится ID предмета и вызывается большая таблица выбора `uncrafted` loot. Этот вспомогательный файл лишь переходит к макросу выдачи опыта; он не решает входной рецепт. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_2.mcfunction)
### `lbc:other/nexus/click/orb_gun/uncraft_f_3`

Макрос создаёт experience orb со значением из `storage lbc.math tmp1`; вызывающая ветка выбирает `tmp1` случайно из 1–3. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_3.mcfunction)
### `lbc:other/nexus/click/orb_gun/uncraft_f_loot_select`

По сохранённому vanilla `Item.id` перебираются точные совпадения и выполняется один `return run loot spawn` с соответствующим рецептом. При отсутствии совпадения этот switch не подбирает близкий рецепт и не создаёт обычный выход. Полный список преобразований и точные количества лежат в [таблице uncrafted loot](lbc_loot_uncrafted.md). [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction)
### `lbc:other/nexus/click/pulsar/pre`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_default.mcfunction`; условие вызова: `execute unless score @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{nexus_pulsar:1}}}}] pulsar_cd matches 1.. run function lbc:other/nexus/click/pulsar/pre`. Состояние scoreboard: scoreboard players set @s Distance 0; scoreboard players set @s pulsar_cd 15. Передаёт шаг в `lbc:other/nexus/click/pulsar/ray`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/pulsar/pre.mcfunction)

### `lbc:other/nexus/click/pulsar/ray`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/pulsar/pre.mcfunction`; условие вызова: `execute anchored eyes positioned ^ ^ ^ run function lbc:other/nexus/click/pulsar/ray`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/nexus/click/pulsar/ray_end`, `lbc:other/nexus/click/pulsar/ray`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/pulsar/ray.mcfunction)

### `lbc:other/nexus/click/pulsar/ray_end`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/pulsar/ray.mcfunction`; условие вызова: `execute unless block ~ ~ ~ #minecraft:airs_only run function lbc:other/nexus/click/pulsar/ray_end`. Обновляет NBT/components: modify storage lbc.math cmd set value "pulsar". Состояние scoreboard: scoreboard players set @s Distance 58395. Передаёт шаг в `lbc:run_command`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/pulsar/ray_end.mcfunction)

## Файлы: other / nexus / rays

### `lbc:other/nexus/rays/pulsar_end`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/pulsar_end.mcfunction)

### `lbc:other/nexus/rays/pulsar_selector`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/pulsar_tick.mcfunction`; условие вызова: `execute if entity @s[tag=selector] run function lbc:other/nexus/rays/pulsar_selector`. Создаёт `marker` с {Tags:["slowraycast","pulsar","rare","nexus_rc","white"]}; `marker` с {Tags:["slowraycast","pulsar","rare","nexus_rc","black"]}; `marker` с {Tags:["slowraycast","pulsar","rare","nexus_rc","final"]}. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/pulsar_selector.mcfunction)

### `lbc:other/nexus/rays/pulsar_tick`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Передаёт шаг в `lbc:other/nexus/rays/pulsar_end`, `lbc:other/nexus/rays/pulsar_tick_final`, `lbc:other/nexus/rays/pulsar_tick_black`, `lbc:other/nexus/rays/pulsar_tick_white`, `lbc:other/nexus/rays/pulsar_selector`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/pulsar_tick.mcfunction)

### `lbc:other/nexus/rays/pulsar_tick_black`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/pulsar_tick_black.mcfunction)

### `lbc:other/nexus/rays/pulsar_tick_final`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/pulsar_tick_final.mcfunction)

### `lbc:other/nexus/rays/pulsar_tick_final_2`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Изменяет блоки: fill ~ ~ ~ ~ ~ ~ minecraft:black_concrete replace minecraft:white_concrete strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/pulsar_tick_final_2.mcfunction)

### `lbc:other/nexus/rays/pulsar_tick_green`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/pulsar_tick_green.mcfunction)

### `lbc:other/nexus/rays/pulsar_tick_red`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/pulsar_tick_red.mcfunction)

### `lbc:other/nexus/rays/pulsar_tick_white`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/pulsar_tick_white.mcfunction)

### `lbc:other/nexus/rays/roadgun`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_rare.mcfunction`; условие вызова: `execute if entity @s[tag=roadgun] run return run function lbc:other/nexus/rays/roadgun`. Передаёт шаг в `lbc:other/nexus/rays/roadgun_2`. Завершение: удаляет сущность (`@s[scores={Lifetime=40..}]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/roadgun.mcfunction)

### `lbc:other/nexus/rays/roadgun_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/roadgun.mcfunction`; условие вызова: `execute if score @s Lifetime matches ..39 run function lbc:other/nexus/rays/roadgun_2`. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/nexus/rays/roadgun_sm_ray_2`, `lbc:other/nexus/rays/roadgun_end`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/roadgun_2.mcfunction)

### `lbc:other/nexus/rays/roadgun_end`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/roadgun_2.mcfunction`; условие вызова: `execute if score @s Lifetime matches 39 at @a[tag=owner] run function lbc:other/nexus/rays/roadgun_end`. Команда: `ride @s dismount`; `execute at @s run tp @s ~ ~ ~`; `execute at @s run forceload add ~ ~`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/roadgun_end.mcfunction)

### `lbc:other/nexus/rays/roadgun_sm_ray_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/roadgun_2.mcfunction`; условие вызова: `execute if score @s Lifetime matches ..36 run function lbc:other/nexus/rays/roadgun_sm_ray_2`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/nexus/rays/roadgun_sm_ray_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/roadgun_sm_ray_2.mcfunction)

### `lbc:other/nexus/rays/sim_cube_t`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_rare.mcfunction`; условие вызова: `execute if entity @s[tag=sim_cube] run return run function lbc:other/nexus/rays/sim_cube_t`. Наносит 10 урона по @e[type=#minecraft:mobs,distance=..1,limit=1] типа `outside_border`. Создаёт `marker` с {Tags:["sim_cube","nexus_rc","slowraycast","rare"]}. Изменяет блоки: setblock ~ ~ ~ minecraft:black_concrete. Завершение: удаляет сущность (`@e[type=!#minecraft:mobs,distance=..1,tag=!nexus_rc,tag=!nexus_entity]`); удаляет сущность (`@e[type=minecraft:marker,tag=sim_cube]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/sim_cube_t.mcfunction)

## Файлы: other / nexus / wires_other_worlds

### `lbc:other/nexus/wires_other_worlds/black`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/instants/select2.mcfunction`; условие вызова: `execute at @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{nexus_wire_black:1}}}}] run function lbc:other/nexus/wires_other_worlds/black`. Передаёт шаг в `lbc:other/nexus/wires_other_worlds/black_at_pre`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/wires_other_worlds/black.mcfunction)

### `lbc:other/nexus/wires_other_worlds/black_at`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/wires_other_worlds/black_at_pre.mcfunction`; условие вызова: `$execute positioned $(x) $(y) $(z) run function lbc:other/nexus/wires_other_worlds/black_at`. Изменяет блоки: setblock ~ ~ ~ minecraft:end_gateway{ExactTeleport:1b,keepPacked:1b,id:"minecraft:beacon",exit_portal:[I;1,15000000,1]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/wires_other_worlds/black_at.mcfunction)

### `lbc:other/nexus/wires_other_worlds/black_at_pre`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nexus/wires_other_worlds/black.mcfunction`; условие вызова: `function lbc:other/nexus/wires_other_worlds/black_at_pre with storage lbc.math`. Передаёт шаг в `lbc:other/nexus/wires_other_worlds/black_at`. Использует макропараметры `$(x)`, `$(y)`, `$(z)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/wires_other_worlds/black_at_pre.mcfunction)

### `lbc:other/nexus/wires_other_worlds/green`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/instants/select2.mcfunction`; условие вызова: `execute at @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{nexus_wire_blue:1}}}}] run function lbc:other/nexus/wires_other_worlds/green`. Loot: spawn lbc:random_glack → ~. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/wires_other_worlds/green.mcfunction)

### `lbc:other/nexus/wires_other_worlds/red`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/instants/select2.mcfunction`; условие вызова: `execute at @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{nexus_wire_red:1}}}}] run function lbc:other/nexus/wires_other_worlds/red`. Состояние scoreboard: scoreboard players add @s mana 5000; scoreboard players add @s swordcharged1 5; scoreboard players add @s swordcharged2 5; scoreboard players add @s swordcharged3 5. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/wires_other_worlds/red.mcfunction)

## Файлы: other / nuke

### `lbc:other/nuke/nuke`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/markers_2t1.mcfunction`; условие вызова: `execute if entity @s[tag=nuke,tag=!nuke1] run return run function lbc:other/nuke/nuke`. Команда: `title @a title " "`; `title @a subtitle [{"selector":"@p[gamemode=!spectator]","color":"red","italic":false,"type":"selector"},{"translate":"nuke_activated","color":"dark_red"}]`; `tag @s add nuke1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke.mcfunction)

### `lbc:other/nuke/nuke1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke.mcfunction`; условие вызова: `execute unless entity @e[distance=..10,tag=stooper] run schedule function lbc:other/nuke/nuke1 20s`. Передаёт шаг в `lbc:other/nuke/nuke1_at`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke1.mcfunction)

### `lbc:other/nuke/nuke1_at`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/effects/effect_nuking.mcfunction`; условие вызова: `execute unless entity @e[type=minecraft:marker,distance=..20,tag=stopper,limit=1] run function lbc:other/nuke/nuke1_at`. Эффект: @e[distance=..45,tag=!spectator] получает `minecraft:wither` на 20 секунд, amplifier 30. Создаёт `marker` с {Tags:["slowraycast","slowparticlesexplode_2","player","magic"],data:{lifetime:400}}; `creeper` с {PersistenceRequired:1b,ExplosionRadius:25b,Fuse:1,ignited:1b,NoGravity:1b,Invulnerable:1b,CustomNameVisible:0b,CustomName:{"italic":false,"translate":"killed_by_nuke"}}. Завершение: удаляет сущность (`@e[type=!minecraft:creeper,type=!minecraft:player,distance=..14,tag=!slowparticlesexplode_2,tag=!strict_map_object]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke1_at.mcfunction)

### `lbc:other/nuke/nuke1_at_bruh`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke1_bruh.mcfunction`; условие вызова: `execute at @e[tag=nuke1,tag=nuke_bruh] run function lbc:other/nuke/nuke1_at_bruh`. Создаёт `marker` с {Tags:["slowraycast","slowparticlesexplode_2","player","magic"],data:{lifetime:400}}. Передаёт шаг в `lbc:other/nuke/nuke1_at_bruh_2`. Завершение: удаляет сущность (`@e[type=!minecraft:creeper,type=!minecraft:player,distance=..14,tag=!slowparticlesexplode_2,tag=!strict_map_object]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke1_at_bruh.mcfunction)

### `lbc:other/nuke/nuke1_at_bruh_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke1_at_bruh.mcfunction`; условие вызова: `execute at @e[tag=!spectator,sort=random,limit=16] run function lbc:other/nuke/nuke1_at_bruh_2`. Эффект: @e[distance=..45,tag=!spectator] получает `minecraft:wither` на 20 секунд, amplifier 30. Создаёт `creeper` с {PersistenceRequired:1b,ExplosionRadius:16b,Fuse:1,ignited:1b,NoGravity:1b,Invulnerable:1b,CustomNameVisible:0b,CustomName:{"italic":false,"translate":"killed_by_nuke"}}. Завершение: удаляет сущность (`@e[type=!minecraft:creeper,type=!minecraft:player,distance=..14,tag=!slowparticlesexplode_2,tag=!strict_map_object]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke1_at_bruh_2.mcfunction)

### `lbc:other/nuke/nuke1_bruh`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke_bruh.mcfunction`; условие вызова: `execute unless entity @e[distance=..10,tag=stooper] run schedule function lbc:other/nuke/nuke1_bruh 35s`. Передаёт шаг в `lbc:other/nuke/nuke1_at_bruh`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke1_bruh.mcfunction)

### `lbc:other/nuke/nuke1_old`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke_old.mcfunction`; условие вызова: `schedule function lbc:other/nuke/nuke1_old 20s`. Эффект: @e[distance=..45] получает `minecraft:wither` на 20 секунд, amplifier 30. Создаёт `creeper` с {ExplosionRadius:30b,Fuse:1,ignited:1b,NoGravity:1b,Invulnerable:1b,CustomNameVisible:0b,CustomName:{"italic":false,"translate":"killed_by_nuke"}}. Изменяет блоки: fill ~15 ~15 ~15 ~-15 ~45 ~-15 minecraft:air strict; fill ~15 ~15 ~15 ~-15 ~-15 ~-15 minecraft:air strict; fill ~15 ~-15 ~15 ~-15 ~-45 ~-15 minecraft:air strict. Завершение: удаляет сущность (`@e[type=!minecraft:creeper,type=!minecraft:player,distance=..14,tag=!strict_map_object]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke1_old.mcfunction)

### `lbc:other/nuke/nuke_bruh`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/markers_2t1.mcfunction`; условие вызова: `execute if entity @s[tag=nuke_bruh,tag=!nuke1] run return run function lbc:other/nuke/nuke_bruh`. Команда: `title @a title " "`; `execute unless entity @s[tag=zelensky] run title @a subtitle [{"selector":"@p[gamemode=!spectator]","color":"red","italic":false,"type":"selector"},{"translate"`; `execute if entity @s[tag=zelensky] run title @a subtitle [{"text":"Володимир Зеленський ","color":"red","italic":false},{"translate":"nuke_lol_activated","color`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke_bruh.mcfunction)

### `lbc:other/nuke/nuke_eoe`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/57.mcfunction`; условие вызова: `execute at @e[type=!minecraft:player,sort=random,limit=16] run function lbc:other/nuke/nuke_eoe`. Создаёт `marker` с {Tags:["nuke","nuke1","rc_1sec","marker_2t","mecha"]}. Изменяет блоки: fill ~1 ~ ~1 ~-1 ~ ~-1 minecraft:netherite_block strict; fill ~ ~3 ~ ~ 319 ~ minecraft:air strict; fill ~ ~3 ~ ~ 255 ~ minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke_eoe.mcfunction)

### `lbc:other/nuke/nuke_old`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/markers_2t1.mcfunction`; условие вызова: `execute if entity @s[tag=nuke_old,tag=!nuke1_old] run return run function lbc:other/nuke/nuke_old`. Команда: `title @a title " "`; `title @a subtitle [{"selector":"@p[gamemode=!spectator]","color":"red","italic":false,"type":"selector"},{"translate":"nuke_activated","color":"dark_red"}]`; `tag @s add nuke1_old`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke_old.mcfunction)

### `lbc:other/nuke/nuket`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycasters_1s.mcfunction`; условие вызова: `execute if entity @s[tag=nuke] run function lbc:other/nuke/nuket`. Изменяет блоки: fill ~1 ~ ~1 ~-1 ~ ~-1 minecraft:netherite_block strict; fill ~ ~3 ~ ~ 319 ~ minecraft:air strict; setblock ~ ~1 ~ minecraft:beacon strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuket.mcfunction)

### `lbc:other/nuke/nuket_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycasters_1s.mcfunction`; условие вызова: `execute if entity @s[tag=nuke_bruh] unless entity @e[distance=..10,tag=stooper] run function lbc:other/nuke/nuket_2`. Изменяет блоки: fill ~1 ~ ~1 ~-1 ~ ~-1 minecraft:netherite_block strict; fill ~2 ~-1 ~2 ~-2 ~-1 ~-2 minecraft:diamond_block strict; fill ~3 ~-2 ~3 ~-3 ~-2 ~-3 minecraft:emerald_block strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuket_2.mcfunction)

### `lbc:other/nuke/nuket_old`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_instant.mcfunction`; условие вызова: `execute if entity @s[tag=nuke_old] run return run function lbc:other/nuke/nuket_old`. Изменяет блоки: fill ~1 ~ ~1 ~-1 ~ ~-1 minecraft:netherite_block strict; fill ~ ~3 ~ ~ 255 ~ minecraft:air strict; setblock ~ ~1 ~ minecraft:beacon strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuket_old.mcfunction)

## Файлы: other / railroad

### `lbc:other/railroad/build_down`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/railroad/tick_build.mcfunction`; условие вызова: `execute if score tmp_build_if lbc.math matches 0 run function lbc:other/railroad/build_down`. Изменяет блоки: setblock ~ ~ ~ minecraft:powered_rail[shape=ascending_north]; setblock ~ ~ ~ minecraft:powered_rail[shape=ascending_south]; setblock ~ ~ ~ minecraft:powered_rail[shape=ascending_east]. Состояние scoreboard: scoreboard players set tmp_build_if lbc.math 1. Передаёт шаг в `lbc:other/railroad/build_down_loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/railroad/build_down.mcfunction)

### `lbc:other/railroad/build_down_loop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/railroad/build_down.mcfunction`; условие вызова: `execute positioned ~ ~-2 ~ if block ~ ~ ~ #minecraft:airs if score rail_temp lbc.math matches 1 run function lbc:other/railroad/build_down_loop`. Изменяет блоки: setblock ~ ~ ~ minecraft:oak_log. Состояние scoreboard: scoreboard players remove temp_y lbc.math 1; scoreboard players set ended_temp lbc.math 1. Передаёт шаг в `lbc:other/railroad/build_down_loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/railroad/build_down_loop.mcfunction)

### `lbc:other/railroad/build_straight`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/railroad/tick_build.mcfunction`; условие вызова: `execute if score tmp_build_if lbc.math matches 0 if block ^ ^1 ^1 #minecraft:airs if block ^ ^ ^1 #minecraft:airs if block ^ ^-1 ^1 #minecraft:airs if block ^ ^-2 ^1 #minecraft:airs if block ^ ^-3 ^1 #minecraft:airs if block ^ ^-4 ^1 #minecraft:airs if block ^ ^-5 ^1 #minecraft:airs if block ^ ^-6 ^1 #minecraft:airs if block ^ ^-7 ^1 #minecraft:airs if block ^ ^-8 ^1 #minecraft:airs if block ^ ^-9 ^1 #minecraft:airs if block ^ ^-10 ^1 #minecraft:airs if block ^ ^-11 ^1 #minecraft:airs if block ^ ^-12 ^1 #minecraft:airs if block ^ ^-13 ^1 #minecraft:airs if block ^ ^-14 ^1 #minecraft:airs if block ^ ^-15 ^1 #minecraft:airs if block ^ ^-16 ^1 #minecraft:airs if block ^ ^-17 ^1 #minecraft:airs if block ^ ^-18 ^1 #minecraft:airs if block ^ ^-19 ^1 #minecraft:airs if block ^ ^-20 ^1 #minecraft:airs if block ^ ^-21 ^1 #minecraft:airs if block ^ ^-22 ^1 #minecraft:airs if block ^ ^-23 ^1 #minecraft:airs if block ^ ^-24 ^1 #minecraft:airs if block ^ ^-25 ^1 #minecraft:airs if block ^ ^-26 ^1 #minecraft:airs if block ^ ^-27 ^1 #minecraft:airs if block ^ ^-28 ^1 #minecraft:airs if block ^ ^-29 ^1 #minecraft:airs run function lbc:other/railroad/build_straight`. Изменяет блоки: setblock ~ ~ ~ minecraft:powered_rail[shape=north_south]; setblock ~ ~ ~ minecraft:powered_rail[shape=east_west]. Состояние scoreboard: scoreboard players set tmp_build_if lbc.math 1. Передаёт шаг в `lbc:other/railroad/build_down_loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/railroad/build_straight.mcfunction)

### `lbc:other/railroad/build_up`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/railroad/tick_build.mcfunction`; условие вызова: `execute if score tmp_build_if lbc.math matches 0 unless score temp_y lbc.math matches 318.. unless block ^ ^ ^1 #minecraft:airs unless entity @s[tag=prev_down] run function lbc:other/railroad/build_up`. Изменяет блоки: setblock ~ ~ ~ minecraft:powered_rail[shape=ascending_south]; setblock ~ ~ ~ minecraft:powered_rail[shape=ascending_north]; setblock ~ ~ ~ minecraft:powered_rail[shape=ascending_west]. Состояние scoreboard: scoreboard players set tmp_build_if lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/railroad/build_up.mcfunction)

### `lbc:other/railroad/end`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/railroad/tick.mcfunction`; условие вызова: `execute if score rail_temp lbc.math matches 200.. unless score inf_rail lbc.math matches 1 run function lbc:other/railroad/end`. Изменяет блоки: fill ^ ^ ^ ^ ^1 ^ minecraft:oak_wood strict. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/railroad/end.mcfunction)

### `lbc:other/railroad/spawn`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/railroad/tick.mcfunction`; условие вызова: `execute if score rail_temp lbc.math matches 1 run function lbc:other/railroad/spawn`. Создаёт `minecart`. Изменяет блоки: fill ^ ^ ^-1 ^ ^1 ^-1 minecraft:oak_wood strict. Обновляет NBT/components: modify entity @s Rotation[0] set value 0.0f; modify entity @s Rotation[0] set value 90.0f; modify entity @s Rotation[0] set value 180.0f. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/railroad/spawn.mcfunction)

### `lbc:other/railroad/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_other.mcfunction`; условие вызова: `execute if entity @s[tag=railroad] if entity @a[gamemode=!spectator,distance=..75,limit=1] run return run function lbc:other/railroad/tick`. Состояние scoreboard: scoreboard players add rail_temp lbc.math 1. Передаёт шаг в `lbc:other/railroad/spawn`, `lbc:other/railroad/end`, `lbc:other/railroad/tick_build`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/railroad/tick.mcfunction)

### `lbc:other/railroad/tick_build`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/railroad/tick.mcfunction`; условие вызова: `execute if score rail_temp lbc.math matches 2..199 unless score inf_rail lbc.math matches 1 run function lbc:other/railroad/tick_build`. Изменяет блоки: setblock ~ ~-1 ~ minecraft:oak_planks strict; fill ~ ~ ~ ~ ~2 ~ minecraft:air strict; fill ~2 ~ ~2 ~-2 ~4 ~-2 minecraft:glass replace minecraft:water. Состояние scoreboard: scoreboard players set tmp_build_if lbc.math 0; scoreboard players set tmp_build_if_2 lbc.math 1; scoreboard players set ended_temp lbc.math 0; scoreboard players operation rail_temp lbc.math %= 7 lbc.math. Передаёт шаг в `lbc:other/railroad/build_straight`, `lbc:other/railroad/build_up`, `lbc:other/railroad/build_down`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/railroad/tick_build.mcfunction)

## Файлы: other / techno

### `lbc:other/techno/create_boss`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_create_boss.mcfunction`; условие вызова: `execute as @e[type=minecraft:armor_stand,nbt={equipment:{feet:{id:"minecraft:leather_boots",components:{"minecraft:custom_data":{techno:1}},count:1},legs:{id:"minecraft:leather_leggings",components:{"minecraft:custom_data":{techno:1}},count:1},chest:{id:"minecraft:leather_chestplate",components:{"minecraft:custom_data":{techno:1}},count:1},head:{id:"minecraft:leather_helmet",components:{"minecraft:custom_data":{techno:1}},count:1}}}] at @s run function lbc:other/techno/create_boss`. Создаёт `skeleton` с {PersistenceRequired:1b,CustomName:{"translate":"cyborg_boss"},Silent:1b,Tags:["cyborg_boss","lucky_mob","new"],Health:75.0f,attributes:[{base:75.0d,id:"minecraft:max_health"}],act. Обновляет NBT/components: modify entity @e[tag=new,tag=cyborg_boss,limit=1] equipment set from entity @s equipment; modify entity @e[tag=new,tag=cyborg_boss,limit=1] Rotation set from entity @s Rotation. Передаёт шаг в `lbc:other/techno/cyborg_boss/setup`, `lbc:other/techno/cyborg_boss/tick`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/create_boss.mcfunction)

### `lbc:other/techno/test_create_boss`

Прямой вход — advancement `place_techno_cyborg`: игрок взаимодействует с armor stand, держа `{techno:1}`, вне Nexus и Imprinted; reward calls this function. `.mcfunction` caller отдельно не найден. Передаёт шаг в `lbc:other/techno/create_boss`. Завершение: сбрасывает advancement для повторного запуска. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_create_boss.mcfunction)

### `lbc:other/techno/trade_arrows`

Прямой вход — advancement `convert_arrows_supply`: игрок взаимодействует с interaction entity tag `supply_drop`, держа не менее двух обычных стрел, вне Nexus и Imprinted; reward calls this function. `.mcfunction` caller отдельно не найден. Состояние scoreboard: scoreboard players set cycle lbc.math 0. Передаёт шаг в `lbc:other/techno/trade_arrows_1`. Завершение: сбрасывает advancement для повторного запуска. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/trade_arrows.mcfunction)

### `lbc:other/techno/trade_arrows_1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/trade_arrows.mcfunction`; условие вызова: `function lbc:other/techno/trade_arrows_1`. Loot: spawn lbc:magnum_ammo → ~. Состояние scoreboard: scoreboard players add cycle lbc.math 1. Передаёт шаг в `lbc:other/techno/trade_arrows_1`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/trade_arrows_1.mcfunction)

## Файлы: other / techno / cyborg_boss

### `lbc:other/techno/cyborg_boss/on_hit`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/cyborg_boss/run.mcfunction`; условие вызова: `execute if data entity @s {HurtTime:10s} run function lbc:other/techno/cyborg_boss/on_hit`. Передаёт шаг в `lbcr:raycast_preloop10_angry`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/cyborg_boss/on_hit.mcfunction)

### `lbc:other/techno/cyborg_boss/run`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/cyborg_boss/tick.mcfunction`; условие вызова: `execute as @e[type=minecraft:skeleton,tag=cyborg_boss] at @s run function lbc:other/techno/cyborg_boss/run`. Состояние scoreboard: scoreboard players remove @s cyborgBossFly 1. Передаёт шаг в `lbc:other/techno/cyborg_boss/on_hit`, `lbc:other/techno/cyborg_boss/wyvern`, `lbc:armor/set_bonus/technomk2/shield`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/cyborg_boss/run.mcfunction)

### `lbc:other/techno/cyborg_boss/setup`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/create_boss.mcfunction`; условие вызова: `execute as @e[type=minecraft:skeleton,tag=cyborg_boss,tag=new] run function lbc:other/techno/cyborg_boss/setup`. Состояние scoreboard: scoreboard players set @s cyborgBossFly 50. Передаёт шаг в `lbc:other/idgive`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/cyborg_boss/setup.mcfunction)

### `lbc:other/techno/cyborg_boss/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shishend/8.mcfunction`; условие вызова: `function lbc:other/techno/cyborg_boss/tick`. Передаёт шаг в `lbc:other/techno/cyborg_boss/run`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/cyborg_boss/tick.mcfunction)

### `lbc:other/techno/cyborg_boss/wyvern`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/cyborg_boss/run.mcfunction`; условие вызова: `execute if entity @a[gamemode=!spectator,distance=..3] if score @s cyborgBossFly matches 0 run function lbc:other/techno/cyborg_boss/wyvern`. Эффект: @s получает `minecraft:levitation` на 1 секунд, amplifier 30; @s получает `minecraft:resistance` на 6 секунд, amplifier 4; @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:blindness` на 5 секунд, amplifier 0. Наносит 8 урона по @s типа `cactus`. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number; scoreboard players set @s cyborgBossFly 200. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/cyborg_boss/wyvern.mcfunction)

## Файлы: other / techno / katana

### `lbc:other/techno/katana/attribute_from_storage`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players set #tier lbc.math 0; scoreboard players add #tier lbc.math 1; scoreboard players set #tier lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/katana/attribute_from_storage.mcfunction)

### `lbc:other/techno/katana/charge`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items/swords.mcfunction`; условие вызова: `execute at @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{techrunner_katana:1}}}}] unless entity @e[type=minecraft:marker,distance=..20,tag=stopper,limit=1] run function lbc:other/techno/katana/charge`. Обновляет NBT/components: modify block 10241024 100 10241024 Items[{Slot:0b}].components."minecraft:attribute_modifiers".[{type:"minecraft:movement_speed. Состояние scoreboard: scoreboard players add @s swordcharging6 1. Передаёт шаг в `lbc:other/techno/katana/charged`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/katana/charge.mcfunction)

### `lbc:other/techno/katana/charged`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/katana/charge.mcfunction`; условие вызова: `execute if score @s swordcharging6 matches 3 run function lbc:other/techno/katana/charged`. Состояние scoreboard: scoreboard players set @s swordcharging6 0; scoreboard players add @s swordcharged6 5. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/katana/charged.mcfunction)

### `lbc:other/techno/katana/speed_based_change`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/player_speed.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{techrunner_katana:1}}}}] run function lbc:other/techno/katana/speed_based_change`. Состояние scoreboard: scoreboard players operation tmp lbc.math = @s speed; scoreboard players operation tmp lbc.math /= 18 lbc.math; scoreboard players operation tmp lbc.math *= tmp lbc.math; scoreboard players operation tmp lbc.math += 1 lbc.math. Передаёт шаг в `lbc:other/techno/katana/speed_based_change_attr_strg`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/katana/speed_based_change.mcfunction)

### `lbc:other/techno/katana/speed_based_change_attr_strg`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/katana/speed_based_change.mcfunction`; условие вызова: `function lbc:other/techno/katana/speed_based_change_attr_strg with storage lbc.math`. Использует макропараметры `$(tmp)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/katana/speed_based_change_attr_strg.mcfunction)

### `lbc:other/techno/katana/use`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_default.mcfunction`; условие вызова: `execute if score @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{techrunner_katana:1}}}}] swordcharged6 matches 1.. at @s unless entity @e[type=minecraft:marker,distance=..20,tag=stopper,limit=1] run function lbc:other/techno/katana/use`. Состояние scoreboard: scoreboard players add tmp lbc.math 2; scoreboard players remove @s swordcharged6 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/katana/use.mcfunction)

## Файлы: other / techno / laser_minigun / ray

### `lbc:other/techno/laser_minigun/ray/as_display`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/end.mcfunction`; условие вызова: `execute as @e[type=minecraft:item_display,tag=laser_ray,tag=!done] run function lbc:other/techno/laser_minigun/ray/as_display`. Состояние scoreboard: scoreboard players set @s death_timer 6. Передаёт шаг в `lbc:other/techno/laser_minigun/ray/tp`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/as_display.mcfunction)

### `lbc:other/techno/laser_minigun/ray/cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/cast.mcfunction`; условие вызова: `execute as @e[type=minecraft:marker,distance=..2,tag=laser_mg_ray,limit=1] at @s run function lbc:other/techno/laser_minigun/ray/cast with storage lbc.math`. Создаёт `item_display`. Обновляет NBT/components: modify entity @e[type=minecraft:item_display,tag=laser_ray,tag=!done,limit=1] transformation.scale set value [0.0f,0.0f,0.0f]. Состояние scoreboard: scoreboard players operation #rot0 lbc.math += #rand lbc.math; scoreboard players operation #rot1 lbc.math += #rand lbc.math; scoreboard players set *test_ray Distance 0; scoreboard players operation @e[type=minecraft:item_display,tag=laser_ray,tag=!done,limit=1] lbcID2 = @s lbcID2. Передаёт шаг в `lbc:other/techno/laser_minigun/ray/loop`. Использует макропараметры `$(spread)`; значения подставляются вызывающей функцией через `with storage`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/cast.mcfunction)

### `lbc:other/techno/laser_minigun/ray/check`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/loop.mcfunction`; условие вызова: `function lbc:other/techno/laser_minigun/ray/check`. Состояние scoreboard: scoreboard players add *test_ray Distance 1. Передаёт шаг в `lbc:other/techno/laser_minigun/ray/hit`, `lbc:other/techno/laser_minigun/ray/hit_entity`, `lbc:other/techno/laser_minigun/ray/end`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/check.mcfunction)

### `lbc:other/techno/laser_minigun/ray/end`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/check.mcfunction`; условие вызова: `execute if score *test_ray Distance matches 400 run function lbc:other/techno/laser_minigun/ray/end`. Состояние scoreboard: scoreboard players set *test_ray Distance 401. Передаёт шаг в `lbc:other/techno/laser_minigun/ray/as_display`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/end.mcfunction)

### `lbc:other/techno/laser_minigun/ray/hit`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/check.mcfunction`; условие вызова: `execute unless block ~ ~ ~ #minecraft:airs run function lbc:other/techno/laser_minigun/ray/hit`. Состояние scoreboard: scoreboard players set *test_ray Distance 401. Передаёт шаг в `lbc:other/techno/laser_minigun/ray/as_display`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/hit.mcfunction)

### `lbc:other/techno/laser_minigun/ray/hit_entity`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/check.mcfunction`; условие вызова: `execute as @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..2,tag=!spectator] unless score @s lbcID2 = *check_id lbcID2 unless score @s team_number = *check_team lbcID2 as @a[gamemode=!spectator] if score @s lbcID2 = *check_id lbcID2 run function lbc:other/techno/laser_minigun/ray/hit_entity`. Наносит 4 урона по @e[distance=..0.0001,limit=1] типа `generic`. Состояние scoreboard: scoreboard players set *test_ray Distance 401. Передаёт шаг в `lbc:other/techno/laser_minigun/ray/as_display`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/hit_entity.mcfunction)

### `lbc:other/techno/laser_minigun/ray/loop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/cast.mcfunction`; условие вызова: `execute at @s run function lbc:other/techno/laser_minigun/ray/loop`. Передаёт шаг в `lbc:other/techno/laser_minigun/ray/check`, `lbc:other/techno/laser_minigun/ray/loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/loop.mcfunction)

### `lbc:other/techno/laser_minigun/ray/tp`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/as_display.mcfunction`; условие вызова: `execute if score @s lbcID2 = @e[tag=raycaster,limit=1] lbcID2 as @e[tag=raycaster,limit=1] at @s anchored eyes positioned ^ ^ ^ as @e[type=minecraft:item_display,tag=laser_ray,tag=!done,limit=1] run function lbc:other/techno/laser_minigun/ray/tp`. Команда: `tp @s ^ ^ ^ ~ ~`; `tag @s add done`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/tp.mcfunction)

## Файлы: other / techno / laser_minigun / ray / visual

### `lbc:other/techno/laser_minigun/ray/visual/run`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/visual/tick.mcfunction`; условие вызова: `execute as @e[type=minecraft:item_display,tag=done,tag=laser_ray] at @s unless entity @e[type=minecraft:marker,distance=..20,tag=stopper,limit=1] run function lbc:other/techno/laser_minigun/ray/visual/run`. Состояние scoreboard: scoreboard players remove @s death_timer 1. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/visual/run.mcfunction)

### `lbc:other/techno/laser_minigun/ray/visual/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/as_display.mcfunction`; условие вызова: `schedule function lbc:other/techno/laser_minigun/ray/visual/tick 1t`. Передаёт шаг в `lbc:other/techno/laser_minigun/ray/visual/run`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/ray/visual/tick.mcfunction)

## Файлы: other / techno / laser_minigun / shoot

### `lbc:other/techno/laser_minigun/shoot/cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/full_rev.mcfunction`; условие вызова: `execute if score @s mana matches 100.. run function lbc:other/techno/laser_minigun/shoot/cast`. Создаёт `marker` с {Tags:["raycaster","laser_mg_ray"]}. Обновляет NBT/components: modify entity @e[distance=..2,tag=laser_mg_ray,limit=1] Rotation set from entity @s Rotation. Состояние scoreboard: scoreboard players set @s laser_minigun_spin 30; scoreboard players remove @s mana 100; scoreboard players remove @s[scores={laser_minigun_spread=1100..}] laser_minigun_spread 100; scoreboard players operation @e[distance=..2,tag=laser_mg_ray,limit=1] lbcID2 = @s lbcID2. Передаёт шаг в `lbc:other/techno/laser_minigun/ray/cast`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/cast.mcfunction)

### `lbc:other/techno/laser_minigun/shoot/click`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_default.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{laser_minigun:1}}}}] anchored eyes positioned ^ ^ ^ run function lbc:other/techno/laser_minigun/shoot/click`. Состояние scoreboard: scoreboard players set @s laser_minigun_cd 6; scoreboard players set @s[tag=!revved] laser_minigun_sound 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/click.mcfunction)

### `lbc:other/techno/laser_minigun/shoot/full_rev`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/run.mcfunction`; условие вызова: `execute if score @s laser_minigun_spin matches 30.. at @s anchored eyes positioned ^ ^ ^ run function lbc:other/techno/laser_minigun/shoot/full_rev`. Состояние scoreboard: scoreboard players set @s laser_minigun_spin 30; scoreboard players remove @s laser_minigun_sound 1. Передаёт шаг в `lbc:other/techno/laser_minigun/shoot/spin_sound`, `lbc:other/techno/laser_minigun/shoot/cast`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/full_rev.mcfunction)

### `lbc:other/techno/laser_minigun/shoot/rev_down`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/run.mcfunction`; условие вызова: `execute if score @s[tag=revved] laser_minigun_cd matches 0 at @s run function lbc:other/techno/laser_minigun/shoot/rev_down`. Состояние scoreboard: scoreboard players set @s laser_minigun_spread 12000. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/rev_down.mcfunction)

### `lbc:other/techno/laser_minigun/shoot/run`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/tick.mcfunction`; условие вызова: `execute as @a[scores={laser_minigun_cd=1..}] at @s unless entity @e[type=minecraft:marker,distance=..20,tag=stopper,limit=1] run function lbc:other/techno/laser_minigun/shoot/run`. Состояние scoreboard: scoreboard players add @s laser_minigun_spin 1; scoreboard players remove @s laser_minigun_cd 1; scoreboard players remove @s laser_minigun_spin 1. Передаёт шаг в `lbc:other/techno/laser_minigun/shoot/full_rev`, `lbc:other/techno/laser_minigun/shoot/rev_down`, `lbc:other/techno/laser_minigun/shoot/spin`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/run.mcfunction)

### `lbc:other/techno/laser_minigun/shoot/spin`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/run.mcfunction`; условие вызова: `execute if score @s laser_minigun_spin matches 1.. run function lbc:other/techno/laser_minigun/shoot/spin`. Команда: `item modify entity @s weapon lbc:rotate_laser_minigun`; `schedule function lbc:other/techno/laser_minigun/shoot/tick 1t`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/spin.mcfunction)

### `lbc:other/techno/laser_minigun/shoot/spin_sound`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/full_rev.mcfunction`; условие вызова: `execute if score @s laser_minigun_sound matches 0 run function lbc:other/techno/laser_minigun/shoot/spin_sound`. Состояние scoreboard: scoreboard players set @s laser_minigun_sound 5. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/spin_sound.mcfunction)

### `lbc:other/techno/laser_minigun/shoot/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/click.mcfunction`; условие вызова: `schedule function lbc:other/techno/laser_minigun/shoot/tick 1t`. Передаёт шаг в `lbc:other/techno/laser_minigun/shoot/run`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/tick.mcfunction)

## Файлы: other / techno / magnum

### `lbc:other/techno/magnum/check_ammo`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_shift.mcfunction`; условие вызова: `execute unless predicate lbc:sneak if score @s coldownMagnum matches 0 run function lbc:other/techno/magnum/check_ammo`. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/techno/magnum/shoot/standard/cast`, `lbc:other/techno/magnum/shoot/incendiary/cast`, `lbc:other/techno/magnum/shoot/electric/cast`, `lbc:other/techno/magnum/shoot/acid/cast`, `lbc:other/techno/magnum/shoot/void/cast`, `lbc:other/techno/magnum/shoot/explosive/cast`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_ammo.mcfunction)

### `lbc:other/techno/magnum/check_ammo_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/uwu.mcfunction`; условие вызова: `execute anchored eyes positioned ^ ^ ^ run function lbc:other/techno/magnum/check_ammo_2`. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/techno/magnum/shoot/standard/cast`, `lbc:other/techno/magnum/shoot/incendiary/cast`, `lbc:other/techno/magnum/shoot/electric/cast`, `lbc:other/techno/magnum/shoot/acid/cast`, `lbc:other/techno/magnum/shoot/void/cast`, `lbc:other/techno/magnum/shoot/explosive/cast`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_ammo_2.mcfunction)

### `lbc:other/techno/magnum/check_shift`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbcr/function/raycast.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{magnum:1}}}}] unless entity @e[type=minecraft:marker,distance=..20,tag=stopper,limit=1] anchored eyes positioned ^ ^ ^ run function lbc:other/techno/magnum/check_shift`. Передаёт шаг в `lbc:other/techno/magnum/cycle`, `lbc:other/techno/magnum/check_ammo`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_shift.mcfunction)

### `lbc:other/techno/magnum/check_shift_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbcr/function/raycast_left.mcfunction`; условие вызова: `execute if items entity @s weapon.offhand *[minecraft:custom_data~{magnum:1}] unless entity @e[type=minecraft:marker,distance=..20,tag=stopper,limit=1] anchored eyes positioned ^ ^ ^ run function lbc:other/techno/magnum/check_shift_left`. Передаёт шаг в `lbc:other/techno/magnum/cycle_left`, `lbc:other/techno/magnum/check_ammo`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_shift_left.mcfunction)

### `lbc:other/techno/magnum/cooldown`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction`; условие вызова: `execute if score @s coldownMagnum matches 1.. run function lbc:other/techno/magnum/cooldown`. Состояние scoreboard: scoreboard players remove @a[scores={coldownMagnum=1..}] coldownMagnum 1. Передаёт шаг в `lbc:other/techno/magnum/shoot/uwu`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/cooldown.mcfunction)

### `lbc:other/techno/magnum/cycle`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_shift.mcfunction`; условие вызова: `execute if predicate lbc:sneak run function lbc:other/techno/magnum/cycle`. Передаёт шаг в `lbc:other/magic_academy/change_mode_4_magnum`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/cycle.mcfunction)

### `lbc:other/techno/magnum/cycle_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_shift_left.mcfunction`; условие вызова: `execute if predicate lbc:sneak run function lbc:other/techno/magnum/cycle_left`. Команда: `item modify entity @s weapon.offhand lbc:magnum_switch_mode`; `playsound minecraft:ui.button.click master @a ~ ~ ~ 0.8 1.5`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/cycle_left.mcfunction)

## Файлы: other / techno / magnum / shoot

### `lbc:other/techno/magnum/shoot/uwu`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/cooldown.mcfunction`; условие вызова: `execute if score @s MagnumNyauwu matches 1.. run function lbc:other/techno/magnum/shoot/uwu`. Состояние scoreboard: scoreboard players remove @s MagnumNyauwu 1. Передаёт шаг в `lbc:other/techno/magnum/check_ammo_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/uwu.mcfunction)

## Файлы: other / techno / magnum / shoot / acid

### `lbc:other/techno/magnum/shoot/acid/cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_ammo.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{mode:3}}},Inventory:[{components:{"minecraft:custom_data":{acid_round:1}}}]}] run function lbc:other/techno/magnum/shoot/acid/cast`. Состояние scoreboard: scoreboard players set @s[scores={coldownMagnum=..0}] MagnumNyauwu 4; scoreboard players set @s coldownMagnum 20. Передаёт шаг в `lbc:other/techno/magnum/shoot/acid/loop`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/acid/cast.mcfunction)

### `lbc:other/techno/magnum/shoot/acid/check`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/acid/loop.mcfunction`; условие вызова: `function lbc:other/techno/magnum/shoot/acid/check`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/techno/magnum/shoot/acid/hit_block`, `lbc:other/techno/magnum/shoot/acid/hit_entity`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/acid/check.mcfunction)

### `lbc:other/techno/magnum/shoot/acid/hit_block`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/acid/check.mcfunction`; условие вызова: `execute unless block ~ ~ ~ #minecraft:airs positioned ^ ^ ^-.5 run function lbc:other/techno/magnum/shoot/acid/hit_block`. Состояние scoreboard: scoreboard players set @a[tag=raycaster,limit=1] Distance 401. Передаёт шаг в `lbc:workingitems/sauvojen_staffs/acid_preloop_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/acid/hit_block.mcfunction)

### `lbc:other/techno/magnum/shoot/acid/hit_entity`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/acid/check.mcfunction`; условие вызова: `execute as @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..15,tag=!raycaster,tag=!hit,tag=!spectator] run function lbc:other/techno/magnum/shoot/acid/hit_entity`. Наносит 6 урона по @s типа `generic`. Состояние scoreboard: scoreboard players set @a[tag=raycaster,limit=1] Distance 401. Передаёт шаг в `lbcr:raycast_preloop11`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/acid/hit_entity.mcfunction)

### `lbc:other/techno/magnum/shoot/acid/loop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/acid/cast.mcfunction`; условие вызова: `function lbc:other/techno/magnum/shoot/acid/loop`. Передаёт шаг в `lbc:other/techno/magnum/shoot/acid/check`, `lbc:other/techno/magnum/shoot/acid/loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/acid/loop.mcfunction)

## Файлы: other / techno / magnum / shoot / electric

### `lbc:other/techno/magnum/shoot/electric/cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_ammo.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{mode:2}}},Inventory:[{components:{"minecraft:custom_data":{electric_round:1}}}]}] run function lbc:other/techno/magnum/shoot/electric/cast`. Состояние scoreboard: scoreboard players set @s[scores={coldownMagnum=..0}] MagnumNyauwu 4; scoreboard players set @s coldownMagnum 15. Передаёт шаг в `lbc:other/techno/magnum/shoot/electric/loop`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/electric/cast.mcfunction)

### `lbc:other/techno/magnum/shoot/electric/check`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/electric/loop.mcfunction`; условие вызова: `function lbc:other/techno/magnum/shoot/electric/check`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/techno/magnum/shoot/electric/hit_block`, `lbc:other/techno/magnum/shoot/electric/hit_entity`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/electric/check.mcfunction)

### `lbc:other/techno/magnum/shoot/electric/hit_block`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/electric/check.mcfunction`; условие вызова: `execute unless block ~ ~ ~ #minecraft:airs positioned ^ ^ ^-.5 run function lbc:other/techno/magnum/shoot/electric/hit_block`. Состояние scoreboard: scoreboard players set @a[tag=raycaster,limit=1] Distance 401. Передаёт шаг в `lbcr:raycast_preloop10magnum`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/electric/hit_block.mcfunction)

### `lbc:other/techno/magnum/shoot/electric/hit_entity`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/electric/check.mcfunction`; условие вызова: `execute as @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..15,tag=!raycaster,tag=!hit,tag=!spectator,limit=2] run function lbc:other/techno/magnum/shoot/electric/hit_entity`. Состояние scoreboard: scoreboard players set @a[tag=raycaster,limit=1] Distance 401. Передаёт шаг в `lbcr:raycast_preloop10magnum`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/electric/hit_entity.mcfunction)

### `lbc:other/techno/magnum/shoot/electric/loop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/electric/cast.mcfunction`; условие вызова: `function lbc:other/techno/magnum/shoot/electric/loop`. Передаёт шаг в `lbc:other/techno/magnum/shoot/electric/check`, `lbc:other/techno/magnum/shoot/electric/loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/electric/loop.mcfunction)

## Файлы: other / techno / magnum / shoot / explosive

### `lbc:other/techno/magnum/shoot/explosive/cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_ammo.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{mode:5}}},Inventory:[{components:{"minecraft:custom_data":{explosive_round:1}}}]}] run function lbc:other/techno/magnum/shoot/explosive/cast`. Состояние scoreboard: scoreboard players set @s[scores={coldownMagnum=..0}] MagnumNyauwu 4; scoreboard players set @s coldownMagnum 40. Передаёт шаг в `lbc:other/techno/magnum/shoot/explosive/loop`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/explosive/cast.mcfunction)

### `lbc:other/techno/magnum/shoot/explosive/check`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/explosive/loop.mcfunction`; условие вызова: `function lbc:other/techno/magnum/shoot/explosive/check`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/techno/magnum/shoot/explosive/hit_entity`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/explosive/check.mcfunction)

### `lbc:other/techno/magnum/shoot/explosive/hit_block`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Создаёт `creeper` с {ExplosionRadius:2b,Fuse:0s,CustomName:{"translate":"round_explosive"}}. Состояние scoreboard: scoreboard players set @a[tag=raycaster,limit=1] Distance 401. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/explosive/hit_block.mcfunction)

### `lbc:other/techno/magnum/shoot/explosive/hit_entity`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/explosive/check.mcfunction`; условие вызова: `execute unless block ~ ~ ~ #minecraft:airs run function lbc:other/techno/magnum/shoot/explosive/hit_entity`. Создаёт `creeper` с {ExplosionRadius:4b,Fuse:0s,CustomName:{"translate":"round_explosive"},Invulnerable:1b,ignited:1b,Silent:1b}. Состояние scoreboard: scoreboard players set @a[tag=raycaster,limit=1] Distance 401. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/explosive/hit_entity.mcfunction)

### `lbc:other/techno/magnum/shoot/explosive/loop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/explosive/cast.mcfunction`; условие вызова: `function lbc:other/techno/magnum/shoot/explosive/loop`. Передаёт шаг в `lbc:other/techno/magnum/shoot/explosive/check`, `lbc:other/techno/magnum/shoot/explosive/loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/explosive/loop.mcfunction)

## Файлы: other / techno / magnum / shoot / incendiary

### `lbc:other/techno/magnum/shoot/incendiary/cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_ammo.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{mode:1}}},Inventory:[{components:{"minecraft:custom_data":{incendiary_round:1}}}]}] run function lbc:other/techno/magnum/shoot/incendiary/cast`. Состояние scoreboard: scoreboard players set @s[scores={coldownMagnum=..0}] MagnumNyauwu 4; scoreboard players set @s coldownMagnum 25. Передаёт шаг в `lbc:other/techno/magnum/shoot/incendiary/loop`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/incendiary/cast.mcfunction)

### `lbc:other/techno/magnum/shoot/incendiary/check`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/incendiary/loop.mcfunction`; условие вызова: `function lbc:other/techno/magnum/shoot/incendiary/check`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/techno/magnum/shoot/incendiary/hit_block`, `lbc:other/techno/magnum/shoot/incendiary/hit_entity`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/incendiary/check.mcfunction)

### `lbc:other/techno/magnum/shoot/incendiary/hit_block`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/incendiary/check.mcfunction`; условие вызова: `execute unless block ~ ~ ~ #minecraft:airs run function lbc:other/techno/magnum/shoot/incendiary/hit_block`. Изменяет блоки: fill ~-1 ~-1 ~-1 ~1 ~1 ~1 minecraft:air replace #minecraft:kiuaskivi_to_air strict; fill ~-1 ~-1 ~-1 ~1 ~1 ~1 minecraft:fire replace #minecraft:kiuaskivi_to_fire. Состояние scoreboard: scoreboard players set @a[tag=raycaster,limit=1] Distance 401. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/incendiary/hit_block.mcfunction)

### `lbc:other/techno/magnum/shoot/incendiary/hit_entity`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/incendiary/check.mcfunction`; условие вызова: `execute as @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..15,tag=!raycaster,tag=!hit,tag=!spectator] run function lbc:other/techno/magnum/shoot/incendiary/hit_entity`. Наносит 12 урона по @s типа `generic`. Обновляет NBT/components: modify entity @s Fire set value 401. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/incendiary/hit_entity.mcfunction)

### `lbc:other/techno/magnum/shoot/incendiary/loop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/incendiary/cast.mcfunction`; условие вызова: `function lbc:other/techno/magnum/shoot/incendiary/loop`. Передаёт шаг в `lbc:other/techno/magnum/shoot/incendiary/check`, `lbc:other/techno/magnum/shoot/incendiary/loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/incendiary/loop.mcfunction)

## Файлы: other / techno / magnum / shoot / standard

### `lbc:other/techno/magnum/shoot/standard/cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_ammo.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{mode:0}}},Inventory:[{components:{"minecraft:custom_data":{standard_round:1}}}]}] run function lbc:other/techno/magnum/shoot/standard/cast`. Состояние scoreboard: scoreboard players set @s[scores={coldownMagnum=..0}] MagnumNyauwu 4; scoreboard players set @s coldownMagnum 25. Передаёт шаг в `lbc:other/techno/magnum/shoot/standard/loop`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/standard/cast.mcfunction)

### `lbc:other/techno/magnum/shoot/standard/check`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/standard/loop.mcfunction`; условие вызова: `function lbc:other/techno/magnum/shoot/standard/check`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/techno/magnum/shoot/standard/hit_block`, `lbc:other/techno/magnum/shoot/standard/hit_entity`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/standard/check.mcfunction)

### `lbc:other/techno/magnum/shoot/standard/hit_block`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/standard/check.mcfunction`; условие вызова: `execute unless block ~ ~ ~ #minecraft:airs run function lbc:other/techno/magnum/shoot/standard/hit_block`. Состояние scoreboard: scoreboard players set @a[tag=raycaster,limit=1] Distance 401. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/standard/hit_block.mcfunction)

### `lbc:other/techno/magnum/shoot/standard/hit_entity`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/standard/check.mcfunction`; условие вызова: `execute as @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..15,tag=!raycaster,tag=!hit,tag=!spectator] run function lbc:other/techno/magnum/shoot/standard/hit_entity`. Наносит 16 урона по @s типа `arrow`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/standard/hit_entity.mcfunction)

### `lbc:other/techno/magnum/shoot/standard/loop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/standard/cast.mcfunction`; условие вызова: `function lbc:other/techno/magnum/shoot/standard/loop`. Передаёт шаг в `lbc:other/techno/magnum/shoot/standard/check`, `lbc:other/techno/magnum/shoot/standard/loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/standard/loop.mcfunction)

## Файлы: other / techno / magnum / shoot / void

### `lbc:other/techno/magnum/shoot/void/cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_ammo.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{mode:4}}},Inventory:[{components:{"minecraft:custom_data":{void_round:1}}}]}] run function lbc:other/techno/magnum/shoot/void/cast`. Состояние scoreboard: scoreboard players set @s[scores={coldownMagnum=..0}] MagnumNyauwu 4; scoreboard players set @s coldownMagnum 15. Передаёт шаг в `lbc:other/techno/magnum/shoot/void/loop`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/void/cast.mcfunction)

### `lbc:other/techno/magnum/shoot/void/check`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/void/loop.mcfunction`; условие вызова: `function lbc:other/techno/magnum/shoot/void/check`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/techno/magnum/shoot/void/hit_block`, `lbc:other/techno/magnum/shoot/void/hit_entity`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/void/check.mcfunction)

### `lbc:other/techno/magnum/shoot/void/hit_block`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/void/check.mcfunction`; условие вызова: `execute unless block ~ ~ ~ #minecraft:airs run function lbc:other/techno/magnum/shoot/void/hit_block`. Изменяет блоки: fill ~.4 ~.4 ~.4 ~-.4 ~-.4 ~-.4 minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/void/hit_block.mcfunction)

### `lbc:other/techno/magnum/shoot/void/hit_entity`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/void/check.mcfunction`; условие вызова: `execute as @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..15,tag=!raycaster,tag=!hit,tag=!spectator] run function lbc:other/techno/magnum/shoot/void/hit_entity`. Наносит 4 урона по @s[tag=!sin_greed_active] типа `outside_border`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/void/hit_entity.mcfunction)

### `lbc:other/techno/magnum/shoot/void/loop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/void/cast.mcfunction`; условие вызова: `function lbc:other/techno/magnum/shoot/void/loop`. Передаёт шаг в `lbc:other/techno/magnum/shoot/void/check`, `lbc:other/techno/magnum/shoot/void/loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/void/loop.mcfunction)

## Файлы: other / techno / supply_crate

### `lbc:other/techno/supply_crate/land`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/supply_crate/run.mcfunction`; условие вызова: `execute if score @s supplyFall matches 150 run function lbc:other/techno/supply_crate/land`. Наносит 10 урона по @s типа `generic`; 900 урона по @s типа `generic`. Изменяет блоки: fill ~ ~ ~ ~ ~1 ~ minecraft:barrier strict. Обновляет NBT/components: modify entity @s item set value {id:"minecraft:repeating_command_block",components:{"minecraft:item_model":"lbc:supply_crate"},. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/supply_crate/land.mcfunction)

### `lbc:other/techno/supply_crate/open`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/supply_crate/run.mcfunction`; условие вызова: `execute if score @s supplyFall matches 200 run function lbc:other/techno/supply_crate/open`. Создаёт `interaction` с {width:1.2f,height:2.4f,Tags:["supply_drop","rc_1sec"]}; `armor_stand` с {DisabledSlots:1048623,Tags:["supply_drop","rc_1sec"],Rotation:[45.0f,0.0f],Invisible:1b,NoGravity:1b,equipment:{head:{id:"minecraft:carrot_on_a_stick",components:{"minecraft:custo. Обновляет NBT/components: modify entity @s item.components."minecraft:item_model" set value "lbc:supply_crate_opened". [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/supply_crate/open.mcfunction)

### `lbc:other/techno/supply_crate/run`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/supply_crate/tick.mcfunction`; условие вызова: `execute as @e[type=minecraft:item_display,tag=supply_drop,tag=new] at @s run function lbc:other/techno/supply_crate/run`. Состояние scoreboard: scoreboard players add @s supplyFall 1. Передаёт шаг в `lbc:other/techno/supply_crate/land`, `lbc:other/techno/supply_crate/open`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/supply_crate/run.mcfunction)

### `lbc:other/techno/supply_crate/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycasters_1s.mcfunction`; условие вызова: `execute if entity @s[tag=supply_drop,tag=new] run function lbc:other/techno/supply_crate/tick`. Передаёт шаг в `lbc:other/techno/supply_crate/run`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/supply_crate/tick.mcfunction)

## Файлы: other / techno / test_ray

### `lbc:other/techno/test_ray/as_display`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/end.mcfunction`; условие вызова: `execute as @e[type=minecraft:item_display,tag=test_ray,tag=!done] run function lbc:other/techno/test_ray/as_display`. Состояние scoreboard: scoreboard players set @s death_timer 20. Передаёт шаг в `lbc:other/techno/test_ray/tp`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/as_display.mcfunction)

### `lbc:other/techno/test_ray/check`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/loop.mcfunction`; условие вызова: `function lbc:other/techno/test_ray/check`. Состояние scoreboard: scoreboard players add *test_ray Distance 1. Передаёт шаг в `lbc:other/techno/test_ray/hit`, `lbc:other/techno/test_ray/end`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/check.mcfunction)

### `lbc:other/techno/test_ray/click`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Создаёт `item_display`. Состояние scoreboard: scoreboard players set *test_ray Distance 0; scoreboard players operation @e[type=minecraft:item_display,tag=test_ray,tag=!done,limit=1] lbcID2 = @s lbcID2; scoreboard players operation @e[type=minecraft:item_display,tag=test_ray,tag=!done,limit=1] team_number = @s team_number. Передаёт шаг в `lbc:other/techno/test_ray/loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/click.mcfunction)

### `lbc:other/techno/test_ray/end`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/check.mcfunction`; условие вызова: `execute if score *test_ray Distance matches 400 run function lbc:other/techno/test_ray/end`. Состояние scoreboard: scoreboard players set *test_ray Distance 401. Передаёт шаг в `lbc:other/techno/test_ray/as_display`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/end.mcfunction)

### `lbc:other/techno/test_ray/hit`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/check.mcfunction`; условие вызова: `execute unless block ~ ~ ~ #minecraft:airs run function lbc:other/techno/test_ray/hit`. Состояние scoreboard: scoreboard players set *test_ray Distance 401. Передаёт шаг в `lbc:other/techno/test_ray/as_display`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/hit.mcfunction)

### `lbc:other/techno/test_ray/loop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/click.mcfunction`; условие вызова: `function lbc:other/techno/test_ray/loop`. Передаёт шаг в `lbc:other/techno/test_ray/check`, `lbc:other/techno/test_ray/loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/loop.mcfunction)

### `lbc:other/techno/test_ray/tp`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/as_display.mcfunction`; условие вызова: `execute if score @s lbcID2 = @e[tag=raycaster,limit=1] lbcID2 as @e[tag=raycaster,limit=1] at @s anchored eyes positioned ^ ^ ^ as @e[type=minecraft:item_display,tag=test_ray,tag=!done,limit=1] run function lbc:other/techno/test_ray/tp`. Команда: `tp @s ^ ^ ^ ~ ~`; `tag @s add done`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/tp.mcfunction)

## Файлы: other / techno / test_ray / visual

### `lbc:other/techno/test_ray/visual/run`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/visual/tick.mcfunction`; условие вызова: `execute as @e[type=minecraft:item_display,tag=done,tag=test_ray] run function lbc:other/techno/test_ray/visual/run`. Состояние scoreboard: scoreboard players remove @s death_timer 1. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/visual/run.mcfunction)

### `lbc:other/techno/test_ray/visual/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/as_display.mcfunction`; условие вызова: `schedule function lbc:other/techno/test_ray/visual/tick 1t`. Передаёт шаг в `lbc:other/techno/test_ray/visual/run`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/visual/tick.mcfunction)

## Файлы: other / wisp_book / create_wisp / air

### `lbc:other/wisp_book/create_wisp/air/drop_as_item`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/air/run.mcfunction`; условие вызова: `execute if score *temp lbc.math matches 12.. run function lbc:other/wisp_book/create_wisp/air/drop_as_item`. Loot: give lbc:wisp/air → @s. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/air/drop_as_item.mcfunction)

### `lbc:other/wisp_book/create_wisp/air/run`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players set *temp lbc.math 0; scoreboard players add *temp lbc.math 1. Передаёт шаг в `lbc:other/wisp_book/create_wisp/air/spawn`, `lbc:other/wisp_book/create_wisp/air/drop_as_item`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/air/run.mcfunction)

### `lbc:other/wisp_book/create_wisp/air/spawn`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/air/run.mcfunction`; условие вызова: `execute if score *temp lbc.math matches ..11 run function lbc:other/wisp_book/create_wisp/air/spawn`. Создаёт `item_display` с {item:{id:"minecraft:repeating_command_block",components:{"minecraft:item_model":"lbc:spirit_air"},count:1},teleport_duration:2,Tags:["wisp","wisp_air","new","rc_5t"]}. Состояние scoreboard: scoreboard players operation @e[type=minecraft:item_display,tag=wisp,tag=new,limit=1] lbcID2 = @s lbcID2; scoreboard players operation @e[type=minecraft:item_display,tag=wisp,tag=new,limit=1] team_number = @s team_number. Передаёт шаг в `lbc:other/wisp_book/passive_effects/air/count`, `lbc:other/wisp_book/realign_wisps/start`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/air/spawn.mcfunction)

## Файлы: other / wisp_book / create_wisp / fire

### `lbc:other/wisp_book/create_wisp/fire/drop_as_item`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/fire/run.mcfunction`; условие вызова: `execute if score *temp lbc.math matches 12.. run function lbc:other/wisp_book/create_wisp/fire/drop_as_item`. Loot: give lbc:wisp/fire → @s. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/fire/drop_as_item.mcfunction)

### `lbc:other/wisp_book/create_wisp/fire/run`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players set *temp lbc.math 0; scoreboard players add *temp lbc.math 1. Передаёт шаг в `lbc:other/wisp_book/create_wisp/fire/spawn`, `lbc:other/wisp_book/create_wisp/fire/drop_as_item`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/fire/run.mcfunction)

### `lbc:other/wisp_book/create_wisp/fire/spawn`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/fire/run.mcfunction`; условие вызова: `execute if score *temp lbc.math matches ..11 run function lbc:other/wisp_book/create_wisp/fire/spawn`. Создаёт `item_display` с {item:{id:"minecraft:repeating_command_block",components:{"minecraft:item_model":"lbc:spirit_fire"},count:1},teleport_duration:2,Tags:["wisp","wisp_fire","new","rc_5t"]}. Состояние scoreboard: scoreboard players operation @e[type=minecraft:item_display,tag=wisp,tag=new,limit=1] lbcID2 = @s lbcID2; scoreboard players operation @e[type=minecraft:item_display,tag=wisp,tag=new,limit=1] team_number = @s team_number. Передаёт шаг в `lbc:other/wisp_book/passive_effects/fire/count`, `lbc:other/wisp_book/realign_wisps/start`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/fire/spawn.mcfunction)

## Файлы: other / wisp_book / create_wisp / light

### `lbc:other/wisp_book/create_wisp/light/drop_as_item`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/light/run.mcfunction`; условие вызова: `execute if score *temp lbc.math matches 12.. run function lbc:other/wisp_book/create_wisp/light/drop_as_item`. Loot: give lbc:wisp/light → @s. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/light/drop_as_item.mcfunction)

### `lbc:other/wisp_book/create_wisp/light/run`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players set *temp lbc.math 0; scoreboard players add *temp lbc.math 1. Передаёт шаг в `lbc:other/wisp_book/create_wisp/light/spawn`, `lbc:other/wisp_book/create_wisp/light/drop_as_item`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/light/run.mcfunction)

### `lbc:other/wisp_book/create_wisp/light/spawn`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/light/run.mcfunction`; условие вызова: `execute if score *temp lbc.math matches ..11 run function lbc:other/wisp_book/create_wisp/light/spawn`. Создаёт `item_display` с {item:{id:"minecraft:repeating_command_block",components:{"minecraft:item_model":"lbc:spirit_light"},count:1},teleport_duration:2,Tags:["wisp","wisp_light","new","rc_5t"]}. Состояние scoreboard: scoreboard players operation @e[type=minecraft:item_display,tag=wisp,tag=new,limit=1] lbcID2 = @s lbcID2; scoreboard players operation @e[type=minecraft:item_display,tag=wisp,tag=new,limit=1] team_number = @s team_number. Передаёт шаг в `lbc:other/wisp_book/passive_effects/light/count`, `lbc:other/wisp_book/realign_wisps/start`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/light/spawn.mcfunction)

## Файлы: other / wisp_book / create_wisp / nature

### `lbc:other/wisp_book/create_wisp/nature/drop_as_item`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/nature/run.mcfunction`; условие вызова: `execute if score *temp lbc.math matches 12.. run function lbc:other/wisp_book/create_wisp/nature/drop_as_item`. Loot: give lbc:wisp/nature → @s. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/nature/drop_as_item.mcfunction)

### `lbc:other/wisp_book/create_wisp/nature/run`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players set *temp lbc.math 0; scoreboard players add *temp lbc.math 1. Передаёт шаг в `lbc:other/wisp_book/create_wisp/nature/spawn`, `lbc:other/wisp_book/create_wisp/nature/drop_as_item`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/nature/run.mcfunction)

### `lbc:other/wisp_book/create_wisp/nature/spawn`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/nature/run.mcfunction`; условие вызова: `execute if score *temp lbc.math matches ..11 run function lbc:other/wisp_book/create_wisp/nature/spawn`. Создаёт `item_display` с {item:{id:"minecraft:repeating_command_block",components:{"minecraft:item_model":"lbc:spirit_nature"},count:1},teleport_duration:2,Tags:["wisp","wisp_nature","new","rc_5t","rc_1sec. Состояние scoreboard: scoreboard players operation @e[type=minecraft:item_display,tag=wisp,tag=new,limit=1] lbcID2 = @s lbcID2; scoreboard players operation @e[type=minecraft:item_display,tag=wisp,tag=new,limit=1] team_number = @s team_number. Передаёт шаг в `lbc:other/wisp_book/passive_effects/nature/count`, `lbc:other/wisp_book/realign_wisps/start`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/nature/spawn.mcfunction)

## Файлы: other / wisp_book / create_wisp / shadow

### `lbc:other/wisp_book/create_wisp/shadow/drop_as_item`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/shadow/run.mcfunction`; условие вызова: `execute if score *temp lbc.math matches 12.. run function lbc:other/wisp_book/create_wisp/shadow/drop_as_item`. Loot: give lbc:wisp/shadow → @s. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/shadow/drop_as_item.mcfunction)

### `lbc:other/wisp_book/create_wisp/shadow/run`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players set *temp lbc.math 0; scoreboard players add *temp lbc.math 1. Передаёт шаг в `lbc:other/wisp_book/create_wisp/shadow/spawn`, `lbc:other/wisp_book/create_wisp/shadow/drop_as_item`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/shadow/run.mcfunction)

### `lbc:other/wisp_book/create_wisp/shadow/spawn`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/shadow/run.mcfunction`; условие вызова: `execute if score *temp lbc.math matches ..11 run function lbc:other/wisp_book/create_wisp/shadow/spawn`. Создаёт `item_display` с {item:{id:"minecraft:repeating_command_block",components:{"minecraft:item_model":"lbc:spirit_shadow"},count:1},teleport_duration:2,Tags:["wisp","wisp_shadow","new","rc_5t"]}. Состояние scoreboard: scoreboard players operation @e[type=minecraft:item_display,tag=wisp,tag=new,limit=1] lbcID2 = @s lbcID2; scoreboard players operation @e[type=minecraft:item_display,tag=wisp,tag=new,limit=1] team_number = @s team_number. Передаёт шаг в `lbc:other/wisp_book/passive_effects/shadow/count`, `lbc:other/wisp_book/realign_wisps/start`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/shadow/spawn.mcfunction)

## Файлы: other / wisp_book / create_wisp / water

### `lbc:other/wisp_book/create_wisp/water/drop_as_item`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/water/run.mcfunction`; условие вызова: `execute if score *temp lbc.math matches 12.. run function lbc:other/wisp_book/create_wisp/water/drop_as_item`. Loot: give lbc:wisp/water → @s. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/water/drop_as_item.mcfunction)

### `lbc:other/wisp_book/create_wisp/water/run`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players set *temp lbc.math 0; scoreboard players add *temp lbc.math 1. Передаёт шаг в `lbc:other/wisp_book/create_wisp/water/spawn`, `lbc:other/wisp_book/create_wisp/water/drop_as_item`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/water/run.mcfunction)

### `lbc:other/wisp_book/create_wisp/water/spawn`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/water/run.mcfunction`; условие вызова: `execute if score *temp lbc.math matches ..11 run function lbc:other/wisp_book/create_wisp/water/spawn`. Создаёт `item_display` с {item:{id:"minecraft:repeating_command_block",components:{"minecraft:item_model":"lbc:spirit_water"},count:1},teleport_duration:2,Tags:["wisp","wisp_water","new","rc_5t"]}. Состояние scoreboard: scoreboard players operation @e[type=minecraft:item_display,tag=wisp,tag=new,limit=1] lbcID2 = @s lbcID2; scoreboard players operation @e[type=minecraft:item_display,tag=wisp,tag=new,limit=1] team_number = @s team_number. Передаёт шаг в `lbc:other/wisp_book/passive_effects/water/count`, `lbc:other/wisp_book/realign_wisps/start`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/create_wisp/water/spawn.mcfunction)

## Файлы: other / wisp_book / delete_wisp

### `lbc:other/wisp_book/delete_wisp/kill`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/air/1.mcfunction`; условие вызова: `execute as @e[type=minecraft:item_display,tag=wisp_air] if score @s lbcID2 = @a[tag=check_id,limit=1] lbcID2 run function lbc:other/wisp_book/delete_wisp/kill`. Состояние scoreboard: scoreboard players remove *delete lbc.math 1. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/kill.mcfunction)

## Файлы: other / wisp_book / delete_wisp / air

### `lbc:other/wisp_book/delete_wisp/air/1`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players set *delete lbc.math 1. Передаёт шаг в `lbc:other/wisp_book/delete_wisp/kill`, `lbc:other/wisp_book/passive_effects/air/count`, `lbc:other/wisp_book/realign_wisps/start`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/air/1.mcfunction)

### `lbc:other/wisp_book/delete_wisp/air/all`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction`; условие вызова: `function lbc:other/wisp_book/delete_wisp/air/all`. Состояние scoreboard: scoreboard players set @s air_wisp_chains 0; scoreboard players set @s air_wisp_charge 0; scoreboard players set @s air_wisp_count 0. Передаёт шаг в `lbc:other/wisp_book/realign_wisps/start`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/air/all.mcfunction)

## Файлы: other / wisp_book / delete_wisp / fire

### `lbc:other/wisp_book/delete_wisp/fire/1`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players set *delete lbc.math 1. Передаёт шаг в `lbc:other/wisp_book/delete_wisp/kill`, `lbc:other/wisp_book/passive_effects/fire/count`, `lbc:other/wisp_book/realign_wisps/start`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/fire/1.mcfunction)

### `lbc:other/wisp_book/delete_wisp/fire/all`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction`; условие вызова: `function lbc:other/wisp_book/delete_wisp/fire/all`. Передаёт шаг в `lbc:other/wisp_book/realign_wisps/start`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/fire/all.mcfunction)

## Файлы: other / wisp_book / delete_wisp / light

### `lbc:other/wisp_book/delete_wisp/light/1`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players set *delete lbc.math 1. Передаёт шаг в `lbc:other/wisp_book/delete_wisp/kill`, `lbc:other/wisp_book/passive_effects/light/count`, `lbc:other/wisp_book/realign_wisps/start`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/light/1.mcfunction)

### `lbc:other/wisp_book/delete_wisp/light/all`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction`; условие вызова: `function lbc:other/wisp_book/delete_wisp/light/all`. Состояние scoreboard: scoreboard players set @s light_wisp_count 0. Передаёт шаг в `lbc:other/wisp_book/realign_wisps/start`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/light/all.mcfunction)

## Файлы: other / wisp_book / delete_wisp / nature

### `lbc:other/wisp_book/delete_wisp/nature/1`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players set *delete lbc.math 1. Передаёт шаг в `lbc:other/wisp_book/delete_wisp/kill`, `lbc:other/wisp_book/passive_effects/nature/count`, `lbc:other/wisp_book/realign_wisps/start`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/nature/1.mcfunction)

### `lbc:other/wisp_book/delete_wisp/nature/all`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction`; условие вызова: `function lbc:other/wisp_book/delete_wisp/nature/all`. Состояние scoreboard: scoreboard players set @s nature_wisp_chance 0. Передаёт шаг в `lbc:other/wisp_book/realign_wisps/start`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/nature/all.mcfunction)

## Файлы: other / wisp_book / delete_wisp / shadow

### `lbc:other/wisp_book/delete_wisp/shadow/1`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players set *delete lbc.math 1. Передаёт шаг в `lbc:other/wisp_book/delete_wisp/kill`, `lbc:other/wisp_book/passive_effects/shadow/count`, `lbc:other/wisp_book/realign_wisps/start`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/shadow/1.mcfunction)

### `lbc:other/wisp_book/delete_wisp/shadow/all`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction`; условие вызова: `function lbc:other/wisp_book/delete_wisp/shadow/all`. Состояние scoreboard: scoreboard players set @s shadow_wisp_charge 0; scoreboard players set @s shadow_wisp_cooldown 0; scoreboard players set @s shadow_wisp_count 0. Передаёт шаг в `lbc:other/wisp_book/realign_wisps/start`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/shadow/all.mcfunction)

## Файлы: other / wisp_book / delete_wisp / water

### `lbc:other/wisp_book/delete_wisp/water/1`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players set *delete lbc.math 1. Передаёт шаг в `lbc:other/wisp_book/delete_wisp/kill`, `lbc:other/wisp_book/passive_effects/water/count`, `lbc:other/wisp_book/realign_wisps/start`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/water/1.mcfunction)

### `lbc:other/wisp_book/delete_wisp/water/all`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction`; условие вызова: `function lbc:other/wisp_book/delete_wisp/water/all`. Передаёт шаг в `lbc:other/wisp_book/realign_wisps/start`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/water/all.mcfunction)

## Файлы: other / wisp_book / passive_effects

### `lbc:other/wisp_book/passive_effects/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycasters_5t.mcfunction`; условие вызова: `execute at @s[tag=wisp] run function lbc:other/wisp_book/passive_effects/tick`. Команда: `execute if entity @s[tag=wisp_fire] run particle minecraft:flame ~ ~ ~ 0.2 0.2 0.2 0 1`; `execute if entity @s[tag=wisp_water] run particle minecraft:rain ~ ~ ~ 0.2 0.2 0.2 0 1`; `execute if entity @s[tag=wisp_nature] run particle minecraft:composter ~ ~ ~ 0.2 0.2 0.2 0 1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/tick.mcfunction)

## Файлы: other / wisp_book / passive_effects / air

### `lbc:other/wisp_book/passive_effects/air/5_and_more`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/count.mcfunction`; условие вызова: `execute if score *temp1 lbc.math matches 5.. run function lbc:other/wisp_book/passive_effects/air/5_and_more`. Состояние scoreboard: scoreboard players operation *temp1 lbc.math -= 4 lbc.math; scoreboard players operation @s air_wisp_count += *temp1 lbc.math; scoreboard players operation *temp1 lbc.math += 4 lbc.math. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/5_and_more.mcfunction)

### `lbc:other/wisp_book/passive_effects/air/count`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/air/1.mcfunction`; условие вызова: `function lbc:other/wisp_book/passive_effects/air/count`. Состояние scoreboard: scoreboard players set *temp1 lbc.math 0; scoreboard players set @s air_wisp_count 0; scoreboard players set @s air_wisp_chains 3; scoreboard players add *temp1 lbc.math 1. Передаёт шаг в `lbc:other/wisp_book/passive_effects/air/5_and_more`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/count.mcfunction)

## Файлы: other / wisp_book / passive_effects / air / as_player

### `lbc:other/wisp_book/passive_effects/air/as_player/run`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_2t.mcfunction`; условие вызова: `execute if score @s air_wisp_count matches 1.. run function lbc:other/wisp_book/passive_effects/air/as_player/run`. Состояние scoreboard: scoreboard players operation @s air_wisp_charge += @s air_wisp_count. Передаёт шаг в `lbc:other/wisp_book/passive_effects/air/as_player/zap`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/as_player/run.mcfunction)

### `lbc:other/wisp_book/passive_effects/air/as_player/zap`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/as_player/run.mcfunction`; условие вызова: `execute if score @s air_wisp_charge matches 100.. run function lbc:other/wisp_book/passive_effects/air/as_player/zap`. Состояние scoreboard: scoreboard players remove @s air_wisp_charge 100; scoreboard players operation *chain lbc.math = @s air_wisp_chains; scoreboard players set *temptemp lbc.math 0; scoreboard players operation *temp lbcID2 = @s lbcID2. Передаёт шаг в `lbc:other/wisp_book/passive_effects/air/zap/cast`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/as_player/zap.mcfunction)

## Файлы: other / wisp_book / passive_effects / air / zap

### `lbc:other/wisp_book/passive_effects/air/zap/as_display`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/end.mcfunction`; условие вызова: `execute as @e[type=minecraft:item_display,tag=lightning_ray,tag=!done] run function lbc:other/wisp_book/passive_effects/air/zap/as_display`. Состояние scoreboard: scoreboard players set @s death_timer 4. Передаёт шаг в `lbc:other/wisp_book/passive_effects/air/zap/tp`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/as_display.mcfunction)

### `lbc:other/wisp_book/passive_effects/air/zap/cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/as_player/zap.mcfunction`; условие вызова: `execute at @e[type=minecraft:item_display,distance=..4,tag=wisp_air,sort=random,limit=1,predicate=lbc:same_id] facing entity @e[type=#minecraft:mobs,distance=..10,tag=!damager,tag=!nodamage,tag=!spectator,sort=nearest,limit=1,tag=!strict_map_object] eyes run function lbc:other/wisp_book/passive_effects/air/zap/cast`. Создаёт `item_display`. Обновляет NBT/components: modify entity @e[type=minecraft:item_display,tag=lightning_ray,tag=!done,limit=1] transformation.scale set value [3.0f,3.0f,1.0. Состояние scoreboard: scoreboard players set *test_ray Distance 0; scoreboard players operation @e[type=minecraft:item_display,tag=lightning_ray,tag=!done,limit=1] lbcID2 = @s lbcID2; scoreboard players operation @e[type=minecraft:item_display,tag=lightning_ray,tag=!done,limit=1] team_number = @s team_n; scoreboard players operation *check_id lbcID2 = @s lbcID2. Передаёт шаг в `lbc:other/wisp_book/passive_effects/air/zap/loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/cast.mcfunction)

### `lbc:other/wisp_book/passive_effects/air/zap/check`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/loop.mcfunction`; условие вызова: `function lbc:other/wisp_book/passive_effects/air/zap/check`. Состояние scoreboard: scoreboard players add *test_ray Distance 1. Передаёт шаг в `lbc:other/wisp_book/passive_effects/air/zap/hit`, `lbc:other/wisp_book/passive_effects/air/zap/hit_entity`, `lbc:other/wisp_book/passive_effects/air/zap/end`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/check.mcfunction)

### `lbc:other/wisp_book/passive_effects/air/zap/end`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/check.mcfunction`; условие вызова: `execute if score *test_ray Distance matches 50.. run function lbc:other/wisp_book/passive_effects/air/zap/end`. Состояние scoreboard: scoreboard players set *test_ray Distance 401. Передаёт шаг в `lbc:other/wisp_book/passive_effects/air/zap/as_display`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/end.mcfunction)

### `lbc:other/wisp_book/passive_effects/air/zap/hit`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/check.mcfunction`; условие вызова: `execute unless block ~ ~ ~ #minecraft:airs run function lbc:other/wisp_book/passive_effects/air/zap/hit`. Состояние scoreboard: scoreboard players set *chain lbc.math 0; scoreboard players set *test_ray Distance 401. Передаёт шаг в `lbc:other/wisp_book/passive_effects/air/zap/as_display`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/hit.mcfunction)

### `lbc:other/wisp_book/passive_effects/air/zap/hit_entity`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/check.mcfunction`; условие вызова: `execute as @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..2,tag=!spectator,tag=!damager,tag=!chained] positioned ~-0.99 ~-0.99 ~-0.99 if entity @s[dx=0,dy=0,dz=0] positioned ~0.99 ~0.99 ~0.99 as @a[distance=..100,tag=damager,limit=1] run function lbc:other/wisp_book/passive_effects/air/zap/hit_entity`. Состояние scoreboard: scoreboard players remove *chain lbc.math 1; scoreboard players set *test_ray Distance 401. Передаёт шаг в `lbc:other/wisp_book/passive_effects/air/zap/as_display`, `lbc:other/wisp_book/passive_effects/air/zap/as_target/run`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/hit_entity.mcfunction)

### `lbc:other/wisp_book/passive_effects/air/zap/loop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/cast.mcfunction`; условие вызова: `function lbc:other/wisp_book/passive_effects/air/zap/loop`. Передаёт шаг в `lbc:other/wisp_book/passive_effects/air/zap/check`, `lbc:other/wisp_book/passive_effects/air/zap/loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/loop.mcfunction)

### `lbc:other/wisp_book/passive_effects/air/zap/run`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Создаёт `marker` с {Tags:["raycaster","lightning_ray_marker"]}. Состояние scoreboard: scoreboard players operation @e[type=minecraft:marker,distance=..2,tag=lightning_ray_marker,limit=1] lbcID2 = @s lbcID2; scoreboard players operation @e[type=minecraft:marker,distance=..2,tag=lightning_ray_marker,limit=1] team_number = @s te. Передаёт шаг в `lbc:other/wisp_book/passive_effects/air/zap/cast`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/run.mcfunction)

### `lbc:other/wisp_book/passive_effects/air/zap/tp`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/as_display.mcfunction`; условие вызова: `function lbc:other/wisp_book/passive_effects/air/zap/tp`. Команда: `tp @s ^ ^ ^ ~ ~`; `tag @s add done`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/tp.mcfunction)

## Файлы: other / wisp_book / passive_effects / air / zap / as_target

### `lbc:other/wisp_book/passive_effects/air/zap/as_target/end`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Команда: `tag @e[type=#minecraft:mobs,tag=chained] remove chained`; `say END`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/as_target/end.mcfunction)

### `lbc:other/wisp_book/passive_effects/air/zap/as_target/run`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/hit_entity.mcfunction`; условие вызова: `execute as @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..6,tag=!damager,tag=!spectator,tag=!chained,limit=1] at @s run function lbc:other/wisp_book/passive_effects/air/zap/as_target/run`. Наносит 4 урона по @s типа `generic`. Передаёт шаг в `lbc:other/wisp_book/passive_effects/air/zap/cast`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/as_target/run.mcfunction)

## Файлы: other / wisp_book / passive_effects / air / zap / visual

### `lbc:other/wisp_book/passive_effects/air/zap/visual/run`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/visual/tick.mcfunction`; условие вызова: `execute as @e[type=minecraft:item_display,tag=done,tag=lightning_ray] at @s unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] run function lbc:other/wisp_book/passive_effects/air/zap/visual/run`. Состояние scoreboard: scoreboard players remove @s death_timer 1. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/visual/run.mcfunction)

### `lbc:other/wisp_book/passive_effects/air/zap/visual/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/as_display.mcfunction`; условие вызова: `schedule function lbc:other/wisp_book/passive_effects/air/zap/visual/tick 2t`. Передаёт шаг в `lbc:other/wisp_book/passive_effects/air/zap/visual/run`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/zap/visual/tick.mcfunction)

## Файлы: other / wisp_book / passive_effects / fire

### `lbc:other/wisp_book/passive_effects/fire/5_and_more`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/fire/count.mcfunction`; условие вызова: `execute if score *temp1 lbc.math matches 5.. run function lbc:other/wisp_book/passive_effects/fire/5_and_more`. Состояние scoreboard: scoreboard players operation *temp1 lbc.math += 16 lbc.math. Передаёт шаг в `lbc:other/wisp_book/passive_effects/fire/5_and_more_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/fire/5_and_more.mcfunction)

### `lbc:other/wisp_book/passive_effects/fire/5_and_more_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/fire/5_and_more.mcfunction`; условие вызова: `function lbc:other/wisp_book/passive_effects/fire/5_and_more_2 with storage lbc.math`. Использует макропараметры `$(tmp)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/fire/5_and_more_2.mcfunction)

### `lbc:other/wisp_book/passive_effects/fire/count`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/fire/1.mcfunction`; условие вызова: `function lbc:other/wisp_book/passive_effects/fire/count`. Состояние scoreboard: scoreboard players set *temp1 lbc.math 0; scoreboard players add *temp1 lbc.math 1. Передаёт шаг в `lbc:other/wisp_book/passive_effects/fire/5_and_more`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/fire/count.mcfunction)

### `lbc:other/wisp_book/passive_effects/fire/particle`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Команда: `particle minecraft:flame ~ ~ ~ 0.1 0.1 0.1 0 1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/fire/particle.mcfunction)

## Файлы: other / wisp_book / passive_effects / light

### `lbc:other/wisp_book/passive_effects/light/count`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/light/1.mcfunction`; условие вызова: `function lbc:other/wisp_book/passive_effects/light/count`. Состояние scoreboard: scoreboard players set *temp1 lbc.math 0; scoreboard players add *temp1 lbc.math 1; scoreboard players operation @s light_wisp_count = *temp1 lbc.math. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/light/count.mcfunction)

### `lbc:other/wisp_book/passive_effects/light/remove_debuff`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/light/run.mcfunction`; условие вызова: `execute if score *temp lbc.math >= 25 lbc.math at @s run function lbc:other/wisp_book/passive_effects/light/remove_debuff`. Передаёт шаг в `lbc:other/wisp_book/passive_effects/light/remove_debuff/1`, `lbc:other/wisp_book/passive_effects/light/remove_debuff/2`, `lbc:other/wisp_book/passive_effects/light/remove_debuff/3`, `lbc:other/wisp_book/passive_effects/light/remove_debuff/4`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/light/remove_debuff.mcfunction)

### `lbc:other/wisp_book/passive_effects/light/run`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_2s.mcfunction`; условие вызова: `execute if score @s light_wisp_count matches 1.. run function lbc:other/wisp_book/passive_effects/light/run`. Передаёт шаг в `lbc:other/wisp_book/passive_effects/light/remove_debuff`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/light/run.mcfunction)

## Файлы: other / wisp_book / passive_effects / light / remove_debuff

### `lbc:other/wisp_book/passive_effects/light/remove_debuff/1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/light/remove_debuff.mcfunction`; условие вызова: `execute if score @s light_wisp_count matches 1.. run function lbc:other/wisp_book/passive_effects/light/remove_debuff/1`. Эффект: снимает эффекты с @s. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/light/remove_debuff/1.mcfunction)

### `lbc:other/wisp_book/passive_effects/light/remove_debuff/2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/light/remove_debuff.mcfunction`; условие вызова: `execute if score @s light_wisp_count matches 2.. run function lbc:other/wisp_book/passive_effects/light/remove_debuff/2`. Эффект: снимает эффекты с @s. Состояние scoreboard: scoreboard players set @s piss 0. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/light/remove_debuff/2.mcfunction)

### `lbc:other/wisp_book/passive_effects/light/remove_debuff/3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/light/remove_debuff.mcfunction`; условие вызова: `execute if score @s light_wisp_count matches 3.. run function lbc:other/wisp_book/passive_effects/light/remove_debuff/3`. Эффект: снимает эффекты с @s. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/light/remove_debuff/3.mcfunction)

### `lbc:other/wisp_book/passive_effects/light/remove_debuff/4`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/light/remove_debuff.mcfunction`; условие вызова: `execute if score @s light_wisp_count matches 4.. run function lbc:other/wisp_book/passive_effects/light/remove_debuff/4`. Состояние scoreboard: scoreboard players set @s effect_remove_mana 0; scoreboard players set @s effect_snus 0; scoreboard players set @s effect_small_explosion 0; scoreboard players set @s effect_evoker 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/light/remove_debuff/4.mcfunction)

## Файлы: other / wisp_book / passive_effects / nature

### `lbc:other/wisp_book/passive_effects/nature/5_and_more`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/nature/count.mcfunction`; условие вызова: `execute if score *temp1 lbc.math matches 5.. run function lbc:other/wisp_book/passive_effects/nature/5_and_more`. Состояние scoreboard: scoreboard players operation *temp1 lbc.math -= 4 lbc.math; scoreboard players operation *temp1 lbc.math *= 2 lbc.math; scoreboard players operation @s nature_wisp_chance += *temp1 lbc.math. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/nature/5_and_more.mcfunction)

### `lbc:other/wisp_book/passive_effects/nature/count`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/nature/1.mcfunction`; условие вызова: `function lbc:other/wisp_book/passive_effects/nature/count`. Состояние scoreboard: scoreboard players set *temp1 lbc.math 0; scoreboard players set @s nature_wisp_chance 0; scoreboard players add *temp1 lbc.math 1; scoreboard players add @s nature_wisp_chance 20. Передаёт шаг в `lbc:other/wisp_book/passive_effects/nature/5_and_more`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/nature/count.mcfunction)

### `lbc:other/wisp_book/passive_effects/nature/give_buff`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/nature/run.mcfunction`; условие вызова: `execute if score @s nature_wisp_chance >= *temp lbc.math at @s run function lbc:other/wisp_book/passive_effects/nature/give_buff`. Эффект: @s получает `minecraft:resistance` на 10 секунд, amplifier 1; @s получает `minecraft:regeneration` на 10 секунд, amplifier 1; @s получает `minecraft:fire_resistance` на 30 секунд, amplifier 0; @s получает `minecraft:absorption` на 30 секунд, amplifier 9; @s получает `minecraft:night_vision` на 60 секунд, amplifier 0. Состояние scoreboard: scoreboard players set @s effect_boost_mana 600. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/nature/give_buff.mcfunction)

### `lbc:other/wisp_book/passive_effects/nature/run`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_1s.mcfunction`; условие вызова: `execute at @s[scores={nature_wisp_chance=1..}] run function lbc:other/wisp_book/passive_effects/nature/run`. Передаёт шаг в `lbc:other/wisp_book/passive_effects/nature/give_buff`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/nature/run.mcfunction)

## Файлы: other / wisp_book / passive_effects / shadow

### `lbc:other/wisp_book/passive_effects/shadow/check`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction`; условие вызова: `execute if score @s shadow_wisp_charge matches 100.. at @s anchored eyes positioned ^ ^ ^ run function lbc:other/wisp_book/passive_effects/shadow/check with entity @s`. Передаёт шаг в `lbc:other/wisp_book/passive_effects/shadow/consume_physical`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/shadow/check.mcfunction)

### `lbc:other/wisp_book/passive_effects/shadow/consume_magic`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players remove @s shadow_wisp_charge 100; scoreboard players set @s shadow_wisp_cooldown 100; scoreboard players operation *temp1 lbc.math = @s shadow_wisp_count; scoreboard players operation *temp1 lbc.math *= 7 lbc.math. Завершение: удаляет сущность (`@e[type=#raycasters,limit=2,distance=..0.01]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/shadow/consume_magic.mcfunction)

### `lbc:other/wisp_book/passive_effects/shadow/consume_physical`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/shadow/check.mcfunction`; условие вызова: `$execute as @e[type=#projectiles,distance=..10,limit=1] unless data entity @s {Owner:$(UUID)} at @s as @a[limit=1,tag=check_id] run function lbc:other/wisp_book/passive_effects/shadow/consume_physical`. Состояние scoreboard: scoreboard players remove @s shadow_wisp_charge 100; scoreboard players set @s shadow_wisp_cooldown 100; scoreboard players operation *temp1 lbc.math = @s shadow_wisp_count; scoreboard players operation *temp1 lbc.math *= 7 lbc.math. Завершение: удаляет сущность (`@e[type=#projectiles,limit=1,distance=..0.001]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/shadow/consume_physical.mcfunction)

### `lbc:other/wisp_book/passive_effects/shadow/count`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/shadow/1.mcfunction`; условие вызова: `function lbc:other/wisp_book/passive_effects/shadow/count`. Состояние scoreboard: scoreboard players set *temp1 lbc.math 0; scoreboard players add *temp1 lbc.math 1; scoreboard players operation @s shadow_wisp_count = *temp1 lbc.math; scoreboard players set @s shadow_wisp_charge 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/shadow/count.mcfunction)

### `lbc:other/wisp_book/passive_effects/shadow/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction`; условие вызова: `execute if entity @s[scores={shadow_wisp_count=1..}] at @s anchored eyes positioned ^ ^ ^ run function lbc:other/wisp_book/passive_effects/shadow/tick`. Состояние scoreboard: scoreboard players operation *temp lbc.math = @s shadow_wisp_count; scoreboard players operation *temp lbc.math *= 100 lbc.math; scoreboard players operation @s shadow_wisp_charge += @s shadow_wisp_count; scoreboard players set @s shadow_wisp_charge 500. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/shadow/tick.mcfunction)

## Файлы: other / wisp_book / passive_effects / water

### `lbc:other/wisp_book/passive_effects/water/5_and_more`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/water/count.mcfunction`; условие вызова: `execute if score *temp1 lbc.math matches 5.. run function lbc:other/wisp_book/passive_effects/water/5_and_more`. Состояние scoreboard: scoreboard players operation *temp1 lbc.math *= 5 lbc.math; scoreboard players operation *temp1 lbc.math += 80 lbc.math. Передаёт шаг в `lbc:other/wisp_book/passive_effects/water/5_and_more_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/water/5_and_more.mcfunction)

### `lbc:other/wisp_book/passive_effects/water/5_and_more_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/water/5_and_more.mcfunction`; условие вызова: `function lbc:other/wisp_book/passive_effects/water/5_and_more_2 with storage lbc.math`. Использует макропараметры `$(tmp)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/water/5_and_more_2.mcfunction)

### `lbc:other/wisp_book/passive_effects/water/count`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/water/1.mcfunction`; условие вызова: `function lbc:other/wisp_book/passive_effects/water/count`. Состояние scoreboard: scoreboard players set *temp1 lbc.math 0; scoreboard players add *temp1 lbc.math 1. Передаёт шаг в `lbc:other/wisp_book/passive_effects/water/5_and_more`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/water/count.mcfunction)

## Файлы: other / wisp_book / realign_wisps

### `lbc:other/wisp_book/realign_wisps/assign_angle`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/start.mcfunction`; условие вызова: `execute as @e[type=minecraft:item_display,tag=wisp,tag=counted] run function lbc:other/wisp_book/realign_wisps/assign_angle`. Состояние scoreboard: scoreboard players operation @s wisp *= *angle lbc.math. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/assign_angle.mcfunction)

### `lbc:other/wisp_book/realign_wisps/count_wisps`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/start.mcfunction`; условие вызова: `execute as @e[type=minecraft:item_display,tag=wisp] if score @s lbcID2 = @a[tag=check_id,limit=1] lbcID2 run function lbc:other/wisp_book/realign_wisps/count_wisps`. Состояние scoreboard: scoreboard players operation @s wisp = *wisp lbc.math; scoreboard players add *wisp lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/count_wisps.mcfunction)

### `lbc:other/wisp_book/realign_wisps/start`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/air/1.mcfunction`; условие вызова: `function lbc:other/wisp_book/realign_wisps/start`. Состояние scoreboard: scoreboard players set *wisp lbc.math 0; scoreboard players operation *angle lbc.math = 360 lbc.math; scoreboard players operation *angle lbc.math /= *wisp lbc.math. Передаёт шаг в `lbc:other/wisp_book/realign_wisps/count_wisps`, `lbc:other/wisp_book/realign_wisps/assign_angle`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/start.mcfunction)

## Файлы: other / wisp_book / realign_wisps / teleport

### `lbc:other/wisp_book/realign_wisps/teleport/angle`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/teleport/run.mcfunction`; условие вызова: `execute at @s anchored eyes rotated ~ 0 positioned ^ ^1 ^ as @e[type=minecraft:item_display,tag=wisp] if score @s lbcID2 = @a[tag=check_id,limit=1] lbcID2 run function lbc:other/wisp_book/realign_wisps/teleport/angle`. Состояние scoreboard: scoreboard players add @s wisp 3. Передаёт шаг в `lbc:other/wisp_book/realign_wisps/teleport/teleport`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/teleport/angle.mcfunction)

### `lbc:other/wisp_book/realign_wisps/teleport/run`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction`; условие вызова: `execute if entity @s[tag=has_wisps] run function lbc:other/wisp_book/realign_wisps/teleport/run`. Передаёт шаг в `lbc:other/wisp_book/realign_wisps/teleport/angle`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/teleport/run.mcfunction)

### `lbc:other/wisp_book/realign_wisps/teleport/teleport`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/teleport/angle.mcfunction`; условие вызова: `function lbc:other/wisp_book/realign_wisps/teleport/teleport with storage lbc.math`. Использует макропараметры `$(tmp)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/teleport/teleport.mcfunction)




## Ручная проверка wires, converter-и и tree

Наземная сборка белого провода запускается `tick_nexus` по красному проводу как carrier, если синие и чёрные выброшенные провода лежат в пределах 3 блоков. Она создаёт `lbc:nexus_wire_white`, затем тратит по одной единице красного, синего и чёрного стека; при count=1 соответствующий item entity удаляется, при большем count второй команда записывает остаток с множителем0.99999. Это не проверка расстояния между синим и чёрным друг с другом: оба сравниваются с позицией красного carrier.

Четыре цветных converter interaction подключены advancement rewards напрямую. Игрок, кроме spectator, с одним предметом соответствующего `custom_data` кликает interaction tag `wire_converter_<color>`; reward потребляет1 провод, ищет ближайший converter interaction до10 блоков от точки в3 блока перед глазами, выдаёт временный item по ближайшему `wire_converter_item` display, удаляет display entities в пределах1 блока и чистит interaction. Все четыре converter-ветки извлекают показанный предмет одинаково: сначала создают item-заглушку TNT, затем копируют Item ближайшего display с тегом `wire_converter_item`. Если подходящего display нет, копирование не выполняется и остаётся TNT. Удаляются все item_display в радиусе1 без фильтра по тегу, затем interaction. Название green соответствует предмету `nexus_wire_blue`.

Отдельные функции `wires_other_worlds` используют провод вне Nexus: red расходует1 провод, добавляет20 XP points, 5000 mana, +5 к `swordcharged1..7` и +100 `nanobots_tier`; green расходует1 blue wire и выдаёт `lbc:random_glack`; black расходует1 провод, выбирает X/Z−2000..2000 и Y0..319, временно forceload-ит чанк и ставит End Gateway с exit `[1,15000000,1]`. Это отдельный путь использования, а не результаты клика по конвертеру. Отдельной функции `wires_other_worlds/white` в источниках нет. Точные конечные свойства loot tables сверяются по [loot definitions](lbc_loot_definitions.md).

`tree_tick_move` каждый шаг выбирает одну из шести ортогональных/вертикальных ориентаций, случайную длину1..6 и заполняет/проходит lime concrete по выбранному локальному направлению. На любом шаге отдельный chance10 может закончить marker и поставить случайно выбранный красный/зелёный/чёрный/белый converter, если рядом нет `center_cube` до15 и существующего converter frame в `0.01..5`; предварительно очищается куб3×3×3. Другая независимая проверка chance5 вызывает массовое завершение: чистит кубы вокруг всех converter frames и удаляет nexus_tree markers в100 блоках. Новые tree markers с chance20 и дальнейшее перемещение принадлежат `tick_nexus_raycasters` tag-dispatch; полная жизнь зависит от этого runtime scheduler.

Файлы `nexus_compass` и `nexus_compass_2..4` не имеют автоматически доказанного caller chain. `nexus_compass` пустой; macro `nexus_compass_2` требует переданный `tmp` верхний индекс, случайно выбирает `0..tmp`, `_3` читает из storage `lbc:nexus` координаты `xN/yN/zN`, а `_4` телепортирует текущего `@s` в координаты и затем поднимает его на60 блоков. Без подходящего storage key или макро-параметров конечная точка не доказана; это helper contract, не найденное самостоятельно используемое compass item.

## Исправление advancement caller-границ и remaining no-caller claims

В старом generated intro формулировка «caller не найден» означала отсутствие вызова в индексированных `.mcfunction`, но пропускала прямые advancement rewards. Поправлены шесть строк: четыре wire converter reward-а (`interaction_nexus_frame_black/green/red/white`), `place_techno_cyborg` и `convert_arrows_supply`. Остальные22 исходные no-function-caller claims из списка остались без непосредственного advancement reward при exact lookup по `lbc_advancement_contracts.json`; их следует читать как «нет ни прямого function caller, ни advancement reward среди изученных JSON», а не доказательство, что функцию невозможно вызвать вручную, макросом, внешним bridge или динамическим селектором.
