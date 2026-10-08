# Снаряды и гаджеты проекта LBC

Здесь разобраны все 70 функций projectile job. Важно различать владельца снаряда и `@s`: `as` меняет исполнителя, но не переносит позицию без `at`. Селектор `dx/dy/dz` задаёт объём, `distance` фильтрует внутри него. NBT `fuse:100s` означает Short на 100 тиков (5 секунд); `schedule ... 1s` — реальную секунду.

## Стрелы: коды эффектов и попадания

`arrows` читает `raid_omen` amplifier стрелы: коды 101+ направляются в `arrows_f`, кроме relay-кодов 106 и 114; также тикает `madoka_arrow` и удаляет `killedarrow`. `arrows2` вызывается у моба с активным Raid Omen, нормализует duration 2 до 1 и записывает счётчики эффекта по коду; после этого помечает lucky effects и очищает Raid Omen. При SWRG skill 19 вызывается `arrows2_nodebuff`, которая обнуляет ровно 25 отрицательных counters: remove_mana, snus, small_explosion, evoker, shock, explosion, big_explosion, traps, lightning_bolt, firebreak, kiuaskivi, lightarrow, evokerarrow, dimshifting, piss, witch_gens_reset, hedgehog_boom, delayed_death, nuking, vacuum, timestop, chain_lightning, shock_potion, skill_issue, nomagic. Положительные mana counters и RTP1..4 она не очищает. Прямые Kaname/rocket/shock-potion вызовы и создание relay-стрел уже выполняются **до** nodebuff и не отменяются задним числом.

| Amplifier | Действие `arrows2` |
|---|---|
| 16 | score `effect_boost_mana` увеличивается на `dur` |
| 17 | score `effect_add_manamax` увеличивается на `dur` |
| 18 | score `effect_add_manaregen` увеличивается на `dur` |
| 19 | score `effect_add_mana` увеличивается на `dur` |
| 20 | score `effect_remove_mana` увеличивается на `dur` |
| 40 | score `effect_rtp_1` увеличивается на `dur` |
| 41 | score `effect_rtp_2` увеличивается на `dur` |
| 42 | score `effect_rtp_3` увеличивается на `dur` |
| 74 | score `effect_snus` увеличивается на `dur` |
| 76 | вызывает `lbc:items/bows/kaname_bow/hit` |
| 97 | создаёт внутреннюю стрелу с Motion Y −10, damage 0,001 и эффектом на 30 тиков |
| 101 | score `effect_small_explosion` увеличивается на `dur` |
| 102 | score `effect_evoker` увеличивается на `dur` |
| 103 | score `effect_shock` увеличивается на `dur` |
| 104 | score `effect_explosion` увеличивается на `dur` |
| 105 | score `effect_rtp_4` увеличивается на `dur` |
| 106 | создаёт внутреннюю стрелу с Motion Y −10, damage 0,001 и эффектом на 30 тиков |
| 107 | score `effect_big_explosion` увеличивается на `dur` |
| 108 | score `effect_traps` увеличивается на `dur` |
| 109 | score `effect_lightning_bolt` увеличивается на `dur` |
| 110 | score `effect_firebreak` увеличивается на `dur` |
| 111 | score `effect_kiuaskivi` увеличивается на `dur` |
| 112 | score `effect_lightarrow` увеличивается на `dur` |
| 113 | score `effect_evokerarrow` увеличивается на `dur` |
| 114 | создаёт внутреннюю стрелу с Motion Y −10, damage 0,001 и эффектом на 30 тиков |
| 115 | score `effect_dimshifting` увеличивается на `dur` |
| 116 | score `piss` увеличивается на `dur` |
| 117 | score `effect_witch_gens_reset` увеличивается на `dur` |
| 118 | score `effect_hedgehog_boom` увеличивается на `dur` |
| 119 | score `effect_stand_arrow_hit` увеличивается на `dur` |
| 120 | score `delayed_death` **заменяется** значением `dur` |
| 121 | score `effect_nuking` увеличивается на `dur` |
| 122 | score `effect_vacuum` увеличивается на `dur` |
| 123 | score `effect_timestop` увеличивается на `dur` |
| 124 | score `effect_chain_lightning` увеличивается на `dur` |
| 125 | вызывает `lbc:items/trinket_effects/rocket_spell_cast` |
| 126 | вызывает `lbc:other/shock_effect/potion/get_effect` |
| -22 | наносит 4 урона типа lava |
| -21 | накладывает Poison 10 секунд, amplifier 4 |

`arrows_f` выполняет эффект попадания и удаляет обработанную стрелу. Код 101 призывает silent/invulnerable Creeper ExplosionRadius 1 с Fuse 0 и `ignited:1b` (отдельное поле `Fire` не задано); 104 — TNT `fuse:2s` (Short: 2 тика=0,1 секунды); 107 — семь таких Creeper радиуса взрыва 10 в центре и по ±4 осям. Для кодов, где это прописано, эффект блокируется, если любой игрок в 12 блоках несёт в Inventory предмет с `custom_data:{mystical:1,type:2}`; это не проверка принадлежности предмета владельцу стрелы; части эффектов также мешают `stopper`/`stopper_magic`. Код 102 создаёт девять evoker fangs **у каждой** non-player сущности с DeathTime=0 и каждого non-spectator игрока в радиусе 8; 103 накладывает в 5 блоках nausea II, blindness II, Speed II и Wither XI на 3 секунды. Код 108 создаёт `lbc:traps` loot, 109 вызывает молнию, 110 ставит огонь в воздухе в кубе ±3 и разрушает куб ±1,5, 111 вызывает большое заполнение Kiuaskivi. Код 118 зовёт внешний hedgehog boom, 119 выдаёт loot stand-arrow, 121 зовёт nuke, 122 переносит все не-player сущности в радиусе 100, кроме strict_map_object, к позиции попадания, 124 запускает chain молнию, 125 — выпуск Rocket Spell. Эти условия различаются по коду, их нельзя считать общей защитой всех стрел. arrows_f **безусловно убивает @s в конце**, даже если stopper заблокировал действие или код 112/113/115/116/117/120/123 не имеет в ней отдельной ветки. Коды 106/114 вообще обходят arrows_f и используются для дыма.

Путь для стрел в полёте запускается `pre`, если у стрелы есть Raid Omen. `inair` увеличивает Lifetime, может ставить Kiuaskivi fire, создавать light/trapper markers и удаляет стрелу на 300 тиках. `inairtrail` меняет частицы на отметках 50/100/150, запускает `inairtrail2` на Lifetime 199 и удаляется на 200; финал сначала создаёт один TNT fuse=0 и одну молнию в текущей точке **даже при weak_v**. Затем weak_v делает return fail; без него добавляются ещё 26 TNT и 15 молний: всего 27 TNT / 16 молний на Y от +3 до −45. Это отдельные заданные высоты, а не сплошная колонна. `inairtrappertrail` при Lifetime>=120 делает 25% проверку и требует любого @a в радиусе 80 (spectator не исключён). Только тогда запускается evoker_rand: fang создаётся лишь при #mobs !spectator !strict_map_object в 5. Если таких мобов нет в 1.5, с отдельным шансом 50% helper поворачивает marker к ближайшему подходящему мобу в 15 и двигает на 1; потом зовёт universal_pos_razb_1. Его kill при Lifetime>=390 достигается только через эти gates. Внешний raycastselector всё равно удаляет trapper marker на data.lifetime=400. Light marker имеет data.lifetime=200. Возраст обоих повышает внешний dispatcher по одному за обслуженный tick; в time stop и исключённых измерениях это не гарантированные секунды настенных часов. `inairtrappertrailactive` — прямой helper, который просто призывает один evoker fang.

## Гаджеты: снежки и попадания

`eggs` вызывается диспетчером снежков игрока. Проверка `custom_data` читает ближайший снежок, но вызов не переключает исполнителя через `as` и не переносит позицию через `at`: исполнителем, координатами и поворотом остаётся игрок/контекст вызывающего. Поэтому обычные ~ /^ ниже отсчитываются от игрока. Есть явные исключения: rasengan сдвигает позицию на Y+1.62, kapusta/jack_o_lantern — к глазам, три *_of_everything — к абсолютной точке (0,64,0). Metasola отдельно переключает as/at на ближайший подходящий снежок, временно помечая игрока thrower. В конце **этого же вызова** удаляется ближайший gadget-снежок в 10, кроме tmt_item; он повторно выбирается и не привязан UUID к первой проверке.

| `custom_data` | Функция |
|---|---|
| `teleport1` | `lbc:projectiles/gadgets/teleport` |
| `teleport1_imba` | `lbc:projectiles/gadgets/teleport_imba` |
| `islandtp` | `lbc:projectiles/gadgets/islandteleporter` |
| `randtp` | `lbc:projectiles/gadgets/swapper` |
| `rasengan` | `lbc:other/rasengan` |
| `superdice` | `lbc:projectiles/gadgets/superdice` |
| `end_of_everything` | `lbc:other/everythingend/1` |
| `baffled_of_everything` | `lbc:other/baffledend/1` |
| `shish_of_everything` | `lbc:other/shishend/1` |
| `trollge` | `lbc:other/troll_box/select` |
| `kill_rand` | `lbc:other/kill_rand_hihihiha` |
| `kapusta` | `lbc:other/kapusta/throw` |
| `gayradar` | `lbc:other/gayradar/click` |
| `bucks` | `lbc:other/bucks/rc` |
| `libspell` | `lbc:items/libspells/select` |
| `win_button` | `lbc:items/win/win` |
| `most_imba` | `lbc:other/imba/most_spawn` |
| `govno_raskid` | `lbc:swrg_kit_integration/items/govno_raskid` |
| `steampunk_rocket` | `lbc:swrg_kit_integration/items/steampunk_rocket` |
| `false_tumba` | `lbc:swrg_kit_integration/items/false_tumba` |
| `time_stopper` | `lbc:swrg_kit_integration/items/touhou_items/time_stop` |
| `bomb_item` | `lbc:swrg_kit_integration/items/touhou_items/bomb` |
| `jack_o_lantern` | `lbc:other/event/halloween/jack_o_lantern_spawn` |
| `metasola` | переключает as/at на снежок и вызывает `lbc:other/event/halloween/soulcorruptwand/metasola/throw` |
| `tmt_item` | исключение из общего удаления гаджетного снежка |

`teleport1` переносит игрока на 5 блоков вперёд от текущей позиции; `stopper` в 24 блоках запрещает эффект. `teleport1_imba` добавляет 30 к `void_resistance`, даёт Slow Falling 30 секунд, переносит игрока на 1488 блоков вперёд, помечает `syn_shlyhi` и через 1 секунду ставит pad 5×1×5 из `lbc:white_concrete_pad_5x1x5` на его текущих X/Z; X/Z берутся через floor(Pos), а целый Y **при <=1 или >=321 заменяется на 320**; только Y=2..320 сохраняется. Template ставится со смещением (−2,−2,−2), затем tag снимается. Это не clamp снизу к Y=1. `islandtp` даёт Slow Falling 3 секунды и переносит игрока к случайному `swrg.spawn`. `randtp` строит временные площадки из obsidian/air в диапазонах высоты у нижней, средней и верхней границы мира, сначала выбирает случайного non-spectator не-owner игрока и лишь **после выбора** проверяет stopper в 24 у него; при блокировке повторного выбора нет. Если цель помечена tp, телепортирует executor к ней, а её — к marker исходной площадки; в конце снимает tp/owner tags и удаляет markers.

`bucket`, `honew`, `ruchka` и `tikva` сравнивают `lbcID2` и `team_number` игрока с ближайшей целью. Bucket проверяет сферу 4.5 с центром на Y+1 от снаряда, перебирает всех non-spectator игроков и требует различие обоих ID/team. Для каждого подходящего положения повторно выбирает ближайшего игрока, переносит helmet в предмет, заменяет его loot `lbc:imba/bucket_head` и наносит 5 generic damage. Tikva использует аналогичный перебор и loot `lbc:imba/pumpkin_head`, наносит 5 cactus damage и сообщает о смене шлема. Ruchka ставит ID/team=−1, **если игроков нет** в 7 от точки Y+1, а цель в 4,5 блоках получает `twitching +=600` (600 тиков=30 секунд), advancement и сообщение. Honey перебирает противников в 4.5 от Y+1; если рядом никого нет, сбрасывает только ID=−1. honey_pl в позиции Y+1 над выбранным игроком копирует заполненные 36 inventory, offhand и 4 armor слота через drop_inv helpers, затем **сам очищает все 41 слота** и разбрасывает ближайшие items малым Motion. Drop helpers по отдельности только копируют предметы. Bucket/tikva сначала копируют существующий helmet в item с PickupDelay=40, затем заменяют слот loot. Их drop_helmet повторно выбирает ближайший modifyitem в 2, без очистки tag. Ruchka/bucket/tikva/honew после обработки убивают текущий снаряд; некоторые helper-effects/particles остаются в исходном target context без нового at.

Bridgebuilders вызывается отдельно от `eggs` — на тиках снежка с `projegadget:1` и в позиции самого projectile. `most` кладёт lime wool, `tntmost` кладёт TNT, площадку **2×1×2** на Y−2.5 с X/Z от −0.5 до +0.5 только вместо minecraft:air; `wool` выбирает один из 16 цветов, определяемых custom_data в `wools/select`. **Most/tntmost** проверяют stopper в 20; wool и bucket/tikva/honew такого gate в bridgebuilders не имеют, ruchka проверяет его. Все 16 wool-веток — отдельные if, поэтому при нескольких цветовых flags последующие цвета перекрасят предыдущие. `superdice` равновероятно выбирает 1..6, кроме `final_luck_modifier=7`, когда остаются исходы 4..6. Результат 1 объявляет исход и убивает самого игрока-executor, если у него нет `sin_greed_active`. Результат 2 передаёт половины базовых knockback resistance, armor, toughness, damage, speed, luck, max health и movement speed внешним updater-ам; 3 даёт навсегда Blindness/Hunger/Mining Fatigue/Nausea/Slowness/Weakness; 4 навсегда даёт Fire Resistance/Haste/Jump Boost/Invisibility/Speed/Slow Falling/Night Vision/Resistance/Water Breathing. Результат 5 зовёт attribute buff 3 раза для armor, 2 для toughness, 3 для damage, 5 для speed, 20 для max-health и 1 для movement speed; числа повышения определяются внешними функциями. Результат 6 выдаёт carrot-on-a-stick с `whitearrow:1,raycastspell:1`; исходы 2..6 не имеют локального таймера или обратной команды.

## Общие диспетчеры снарядов

`projectilestick` передаёт стрелы в `pre`, а снежки с компонентом `projegadget:1` — в gadget-handler. Диспетчер `items/snowballs` вызывает `eggs` по metadata; поскольку вызов не использует `as` или `at`, executor и позиция остаются игроком и его контекстом.
## Самонаводящиеся снаряды

Стрелы с potion custom_color 6710877 идут в `homing`, а 6710886 — в `homing_tnt`. Homing на время помечает владельца и союзников `nodmg`, каждый тик увеличивает Lifetime и с помощью `raycast_vpered_very_slow_vect` наводится на ближайшего подходящего моба. Если стрела застряла в блоке, для **каждой оси независимо** проверяется шанс 35% и только прошедшая ось Motion разворачивается и умножается примерно на 10 с floor промежуточного числа; а `inGround` сбрасывается; локального срока удаления нет. Для `weak_homing_arrow` при отсутствии игроков в 15 блоках действует шанс 70% начать `homing_soft`; тот наводится на врага в радиусе 100 через общий vector raycaster и удаляет свой тег после застревания. `homing_tnt` не срабатывает при союзнике/владельце в 14 блоках, иначе поворачивает контекст в сторону ближайшего врага и создаёт TNT fuse=0 **на 3 блока позади текущего снаряда вдоль этого направления** (positioned ^ ^ ^−3), а не позади позиции врага.

`shulker_bullet` при теге `lighttrail` создаёт маркер `lightarrowtrail` со сроком 200 тиков. При `trappertrail` отдельной if-командой создаётся новая shulker bullet, которой копируются ID/team/Owner; её `Target` становится ближайшим мобом не-spectator и не-владельцем в радиусе 100 блоков. Для созданной пули локального таймера удаления нет.


## Подготовка стрел и эффекты

`pre` — per-arrow dispatcher, выполняемый для стрел. Он один раз нормализует специальные potion custom_color: 6710881→damage5, 6710882→damage1, 6710883→NoGravity, 6710884→damage **10^20**, 6710885→NoGravity и перевод в homing-цвет. Также распознаёт homing, weak homing, trinket cocktail/volos, racer-стрелы, in-flight arrows и вызывает `prein` при `inGround:1b`. `prein` передаёт стрелы с amplifier 106/114 в дымовые функции, остальные raid_omen payloads — в `arrows`, groundbreaker — во внешний groundbreak, exploding — в мгновенный TNT, bedrock_arrow — в `bi_arrow`, amp97 smoke-bomb — в SWRG, а затем удаляет стрелы с `killedarrow`.

`bi_arrow` заполняет текущий куб **2×2×2** (~−0.5..~+0.5) на bedrock с `destroy`, играет звук установки камня и удаляет стрелу. Так bedrock arrow меняет блок в месте падения.


## Гоночные стрелы

`racer_arrow` находит ездока по `lbcID2` в 0,0001 блока; если владельца нет в радиусе 3, возвращает стреле гравитацию, иначе ведёт её по взгляду игрока через `raycast_vpered_slow` и создаёт облака. За каждый вызов с шансом 30% создаёт четыре lightning marker с Lifetime 30/35/40/45 и копирует ID/team. Steampunk-вариант летит через `raycast_vpered_vect`, оставляет огненные частицы и удаляется при Lifetime 40; `pre` запускает их только с соответствующими `rided_*` и `actived` тегами.


## Шалкер-снаряды

`lighttrail` создаёт lightarrowtrail marker с Lifetime 200 тиков. `trappertrail` создаёт новый shulker bullet, копирует ID/team/Owner от исходного и выбирает целью ближайшего не-spectator моба не владельца в 100 блоках; локального срока удаления у созданной пули нет.

## Дымовые стрелы

`smoke` и `smoke_old` вызываются повторно из prein пока подходит item payload (Raid Omen amp106/114 **duration=20**) и inGround. В каждом вызове рисуют дым и обновляют Blindness II на 2 секунды всем non-spectator игрокам в 5. Только команда изменения life защищена !smoke_active: если при первом таком вызове arrow life уже **ровно 100**, ставит 800; затем в любом случае добавляет smoke_active. Если первый вызов был на другом life, позднее 100 уже не изменяется этим helper. Числа short задают тики; это не гарантированные 40 секунд существования дымовой стрелы. Новая версия рисует 100 серых wool block-marker частиц со spread (7,4,7), старая — poof; `prein` включает их для приземлившихся raid_omen стрел amp106/114.


## Особые метательные предметы и телепортация

`specials` читает custom_data у snowball-executor, но штатный items/snowballs caller делает **as без at**, сохраняя позицию глаз игрока и его поворот. Поэтому новые снаряды появляются от глаз игрока (^ offsets), а Motion читается у выбранного снежка. Вложенные pernul callers тоже делают as без at до создания новых сущностей. Локальные варианты: bigexplode создаёт Piercing 100 arrow с amp107; smoke/smoke_old — amp106/114; luckygrenade — amp108; confuse — amp105; blind/poison/wither — tipped arrows с эффектами соответственно blindness 120, poison 100 и wither 100 тиков; standart имеет damage 4, boom — amp104. Обычные fire-варианты создают small_fireball, fireball с ExplosionPower 2 или dragon_fireball; imba-варианты — TNT `fuse:100s` (Short: 100 тиков=5 секунд), invulnerable fireball с ExplosionPower 127 или tagged dragon_fireball. Spike-варианты: confuse amp115/2 тика, blindness amp116/6000 тиков, poison amp117/2 тика, `spike_wither` фактически стреляет water_breathing amp1/600 тиков, standart имеет damage10, а boom — amp118; spike_impulse зовёт внешний raycast. Metal pipe бросает spyglass-предмет с attribute modifiers; ender_pernul выпускает жемчужный залп; dynamite создаёт TNT `fuse:1000s` (1000 тиков=50 секунд); smoke_bomb запускает amp97 в SWRG handler. Появившиеся projectiles берут Motion у snowball и Owner/owner у ближайшего non-spectator @p **без ограничения радиуса**; радиус 4 ограничивает выбор нового projectile, это может быть не бросивший игрок. Функция удаляет обработанный snowball, кроме `tmt_item`.

`specials_ender_pernul` выпускает 16 ender pearl и передаёт Motion/Owner выбранным дочерним снарядам. Pearl/TNT добавляют к каждой оси случайное значение −1..1 с шагом 0.1, сначала округлив старый Motion вниз до десятых. Dragon-версия выпускает 16 dragon fireball: прибавляет к Motion X/Z −0.03..0.03, Y −0.005..0.005 с шагом 0.001 (старый Motion предварительно округляется вниз до тысячных). Это разброс движения, а не ExplosionPower. Fire-версия выпускает 32 TNT в 3 блоках впереди с `fuse:100s` (100 тиков=5 секунд) и тегами groundbam/evoker/fire/electro/strong. Дочерние projectiles снимают временный pernul-тег; попадание далее обрабатывает groundbam/other.

`teleport_nahui` делает один random 1..4, затем **последовательно**, без return, проверяет текущие predicates executor Overworld → Nether → End → Imprinted. Успешная телепортация в ранней ветке меняет dimension executor, и следующая ветка может сработать ещё раз. При обычном успешном исполнении vanilla tp итог по стартовому измерению получается таким:

| Старт | 1 | 2 | 3 | 4 |
|---|---|---|---|---|
| Overworld | Overworld (через Nether) | Nether (через End) | Nexus helper | Overworld (через Imprinted) |
| Nether | Overworld | Nether (через End) | Nexus helper | Overworld (через Imprinted) |
| End | Overworld | Nether | Nexus helper | Overworld (через Imprinted) |
| Imprinted | Nether | End | Nexus helper | Overworld |

Позиция CommandSourceStack при tp сама не обновляется: дальнейшие ~ берутся из исходного контекста. `execute in` учитывает coordinate_scale (Nether ↔ обычное измерение меняет X/Z в 8 раз), так что «то же XYZ» неверно. Прямые `_ne/_oe/_on/_im` helpers по отдельности выполняют только свою таблицу назначения; эффект повторного перехода относится к верхнему dispatcher. При неизвестном измерении все четыре predicates могут не пройти. Nexus делегирован tp_nexus_rtp: он бросает X/Z −4800..4800, вызывает macro teleport на Y=2332 в nexus и удаляет gravity modifier antigravity_effect. Безопасная точка/защитные эффекты здесь не гарантированы.


## Границы и внешние зависимости

Диспетчер `items/snowballs` из другого пакета выбирает гаджет по `custom_data`; `eggs` лишь содержит маршрутизацию и локальные ветки. Внешние обработчики (например, `lbc:other/rasengan`, `lbc:other/drop_inv`, `lbc:swrg_kit_integration/...`, `tp_nexus_rtp`, groundbreak/hedgehog/nuke и rocket spell) здесь отмечены как зависимости, но их конечные эффекты не проверялись в этом пакете. Перо/стрелы могут также передавать попадание внешним `other`-обработчикам; этот документ описывает локальную точку вызова и локальную очистку, где она есть.



## Условия маршрутизации и скорость особых снарядов

У arrows_f защита mystical/type2 в Inventory любого @a<=12 проверяется только
для 101/104/107/109/110. Stopper<=20 нужен для 101/103/104/107/108/121/122/124,
stopper_magic<=20 — для 102/109/110/111/118; 119 и 125 проходят без обоих gates.
Такая защита не фильтрует союзников и не отменяет финальный kill стрелы.
Поля duration в arrow payload — тики, счётчики mobs увеличиваются на прочитанное
duration и обслуживаются своим runtime; длительность effect give задаётся секундами.

В eggs stopper<=24 проверяется у игрока для teleport1/teleport1_imba/islandtp/
randtp/kill_rand. Rasengan требует stopper_magic<=24 отсутствующим, Inventory
anti-TNT у любого @a<=12 отсутствующим и nomagic_active non-spectator @a<=20
отсутствующим. Остальные перечисленные egg-вызовы локально не имеют этих gates.
Значения custom_data проверяются независимо: составной предмет может вызвать
несколько ветвей до повторного выбора удаляемого снежка.

Передача Motion в specials:

| Группа нового снаряда | Фактическая запись |
|---|---|
| edited | целый Motion копируется со снежка (один ближайший edited<=4) |
| edited6, dynamite | каждая ось floor(Motion×10000)×0.0001, примерно ×1 |
| edited4, ender pearl | floor(Motion×10000)×0.00025, примерно ×2.5 |
| edited3, small_fireball/impulse/TNT puknul | floor(Motion×10000)×0.0006, примерно ×6 |
| edited2 fireball | floor(Motion×100000)×0.00000075, примерно ×0.075 |
| edited2 dragon_fireball | floor(Motion×100000)×0.0000005, примерно ×0.05 |
| edited2 small_fireball | прописана ×0.2, но обычная smallfireball-ветка создаёт edited3, поэтому эта строка к ней не относится |

Owner/owner и ID/team устанавливаются отдельно для edited/2/3/4, **не edited6**.
Для #arrows этой группы Rotation записывается как −floor(pitch/yaw ближайшего @p).
Повторные selectors выбирают по tag/distance/nearest, а не UUID только что созданного
объекта; наличие другого подходящего объекта может изменить адресата записи.
Metal pipe: item PickupDelay=10 тиков, attack_damage +10, attack_speed −3.1;
tooltip_style фактически вложен в custom_data. Impulse — arrow damage=1.
Spike payloads не следует описывать по названию: spike_blind передаёт счётчик piss,
spike_poison — witch_gens_reset, spike_wither — Water Breathing II.

Pernul создаёт 16 pearls / 16 dragon_fireballs / 32 TNT. Pearl/TNT staging выбирает
глобальный tag pernul/pernul_2 с limit=16/32 без distance; dragon staging имеет
distance<=5. Child helpers ищут Motion/Owner у own tag (у dragon<=10), дают разброс
и снимают staging tag. Поведение rand_motion_razbros/rand_power_razbros_small
связано с общим runtime, а не с независимой случайной ExplosionPower каждого fireball.
Shulker генерация копирует team, но Target-фильтр исключает только same_id,
не same_team. Два if lighttrail/trappertrail могут исполниться в одном вызове.

`homing_hit` всегда очищает inGround, даже когда все три 35% проверки провалились.
Racer ultra задаёт Motion примерно 2 блока/тик по взгляду выбранного ездока;
steampunk добавляет вектор 0.1 к текущему Motion, с ограничением общим helper.
Снятие NoGravity при отсутствии ездока не является удалением снаряда.
pre меняет Tags на список [updated] для первых четырёх цветовых вариантов,
а не добавляет updated к старому списку. Цвет 6710885 сменяется на 6710877
в том же проходе и сразу попадает в следующий homing if.

## Проверенные исходники

Все 70 функций прочитаны координатором; отдельно сопоставлены items/snowballs, projectilestick, raycastselector и mobs_tick.

- [projectiles/arrows/bigexplode.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/arrows/bigexplode.mcfunction)
- [projectiles/arrows/evoker.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/arrows/evoker.mcfunction)
- [projectiles/arrows/evoker_rand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/arrows/evoker_rand.mcfunction)
- [projectiles/arrows/inair.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/arrows/inair.mcfunction)
- [projectiles/arrows/inairtrail.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/arrows/inairtrail.mcfunction)
- [projectiles/arrows/inairtrail2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/arrows/inairtrail2.mcfunction)
- [projectiles/arrows/inairtrappertrail.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/arrows/inairtrappertrail.mcfunction)
- [projectiles/arrows/inairtrappertrailactive.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/arrows/inairtrappertrailactive.mcfunction)
- [projectiles/arrows/shock.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/arrows/shock.mcfunction)
- [projectiles/arrows.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/arrows.mcfunction)
- [projectiles/arrows2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/arrows2.mcfunction)
- [projectiles/arrows2_nodebuff.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/arrows2_nodebuff.mcfunction)
- [projectiles/arrows_f.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/arrows_f.mcfunction)
- [projectiles/bi_arrow.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/bi_arrow.mcfunction)
- [projectiles/eggs.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/eggs.mcfunction)
- [projectiles/gadgets/bridgebuilders.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/bridgebuilders.mcfunction)
- [projectiles/gadgets/bucket.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/bucket.mcfunction)
- [projectiles/gadgets/bucket_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/bucket_2.mcfunction)
- [projectiles/gadgets/bucket_noteam.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/bucket_noteam.mcfunction)
- [projectiles/gadgets/bucket_pl.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/bucket_pl.mcfunction)
- [projectiles/gadgets/drop_helmet.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/drop_helmet.mcfunction)
- [projectiles/gadgets/honew.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/honew.mcfunction)
- [projectiles/gadgets/honew_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/honew_2.mcfunction)
- [projectiles/gadgets/honey_pl.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/honey_pl.mcfunction)
- [projectiles/gadgets/islandteleporter.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/islandteleporter.mcfunction)
- [projectiles/gadgets/ruchka.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/ruchka.mcfunction)
- [projectiles/gadgets/ruchka_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/ruchka_2.mcfunction)
- [projectiles/gadgets/ruchka_pl.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/ruchka_pl.mcfunction)
- [projectiles/gadgets/superdice/1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/superdice/1.mcfunction)
- [projectiles/gadgets/superdice/2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/superdice/2.mcfunction)
- [projectiles/gadgets/superdice/3.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/superdice/3.mcfunction)
- [projectiles/gadgets/superdice/4.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/superdice/4.mcfunction)
- [projectiles/gadgets/superdice/5.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/superdice/5.mcfunction)
- [projectiles/gadgets/superdice/6.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/superdice/6.mcfunction)
- [projectiles/gadgets/superdice.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/superdice.mcfunction)
- [projectiles/gadgets/swapper.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/swapper.mcfunction)
- [projectiles/gadgets/teleport.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/teleport.mcfunction)
- [projectiles/gadgets/teleport_imba.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/teleport_imba.mcfunction)
- [projectiles/gadgets/teleport_imba_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/teleport_imba_2.mcfunction)
- [projectiles/gadgets/teleport_imba_3.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/teleport_imba_3.mcfunction)
- [projectiles/gadgets/teleport_imba_4.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/teleport_imba_4.mcfunction)
- [projectiles/gadgets/tikva.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/tikva.mcfunction)
- [projectiles/gadgets/tikva_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/tikva_2.mcfunction)
- [projectiles/gadgets/tikva_pl.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/tikva_pl.mcfunction)
- [projectiles/gadgets/wools/select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/wools/select.mcfunction)
- [projectiles/homing.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/homing.mcfunction)
- [projectiles/homing_hit.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/homing_hit.mcfunction)
- [projectiles/homing_soft.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/homing_soft.mcfunction)
- [projectiles/homing_tnt.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/homing_tnt.mcfunction)
- [projectiles/pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/pre.mcfunction)
- [projectiles/prein.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/prein.mcfunction)
- [projectiles/racer_arrow.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/racer_arrow.mcfunction)
- [projectiles/racer_arrow_lighting.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/racer_arrow_lighting.mcfunction)
- [projectiles/racer_arrow_steampunk.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/racer_arrow_steampunk.mcfunction)
- [projectiles/shulker_bullet.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/shulker_bullet.mcfunction)
- [projectiles/shulker_bullet_generate.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/shulker_bullet_generate.mcfunction)
- [projectiles/smoke.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/smoke.mcfunction)
- [projectiles/smoke_old.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/smoke_old.mcfunction)
- [projectiles/specials.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/specials.mcfunction)
- [projectiles/specials_dragon_pernul.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/specials_dragon_pernul.mcfunction)
- [projectiles/specials_ender_pernul.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/specials_ender_pernul.mcfunction)
- [projectiles/specials_ender_pernul_rand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/specials_ender_pernul_rand.mcfunction)
- [projectiles/specials_ender_pernul_rand2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/specials_ender_pernul_rand2.mcfunction)
- [projectiles/specials_ender_pernul_rand_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/specials_ender_pernul_rand_2.mcfunction)
- [projectiles/specials_fire_pernul.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/specials_fire_pernul.mcfunction)
- [projectiles/teleport_nahui.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/teleport_nahui.mcfunction)
- [projectiles/teleport_nahui_im.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/teleport_nahui_im.mcfunction)
- [projectiles/teleport_nahui_ne.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/teleport_nahui_ne.mcfunction)
- [projectiles/teleport_nahui_oe.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/teleport_nahui_oe.mcfunction)
- [projectiles/teleport_nahui_on.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/teleport_nahui_on.mcfunction)
