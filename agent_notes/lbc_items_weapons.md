# LBC: оружие и боевые предметы

Охват пакета: 256 функций из `items/bows`, `items/crossbow`, `items/shields`, `items/shooter/slr` и `items/swords`/`items/wands`. Здесь описываются триггеры и конечное действие; выдачу/точный состав компонентов сверяйте по [каталогу loot](lbc_loot_definitions.md), а условия advancement по [каталогу advancement](lbc_advancements.md). Дочерние `workingitems/**`, лучи и снаряды указаны как зависимости, пока не прослеженные в этом пакете.

## Контексты исполнения и геометрия

`execute as` меняет исполнителя, но сохраняет текущую позицию и поворот, пока цепочка не использует `at`, `positioned` или другой перенос контекста. Селектор `dx/dy/dz` задаёт прямоугольный объём поиска; `distance` лишь отсекает сущности внутри этого объёма и сам по себе не расширяет его до сферы. Для единиц: NBT `fuse:100s` означает Short со значением 100 тиков (5 секунд), типичная NBT `Duration` также задаётся в тиках, а `schedule function ... 1s` — реальная секунда.

## Общий вход и частота срабатывания

`items/swords` вызывается наградой advancement `gowno`, а его критерий — `minecraft:player_hurt_entity` с незаблокированным уроном, не вызванным стрелой; это событие попадания, не цикл удержания меча. Advancement отзывается в конце функции, чтобы снова реагировать на следующее подходящее событие. Поэтому заряд, добавляемый кодом внутри `items/swords` (в частности, Banhammer, Blazefire и Fevgalea), набирается по вызовам боевого обработчика, а не автоматически каждый тик ношения. Дополнительные вызовы через `drops/tmt/trigger_smth*` — отдельные явные маршруты TMT.

Луки и арбалеты входят разными событиями: `bow_pulling` использует advancement `minecraft:using_item` для обычного bow и вызывает `charge_add`; `crossbow` использует `minecraft:shot_crossbow` и запускает обработку выстрела, тогда как `crossbow_charging` отдельно слушает `using_item` для vanilla crossbow. Их повторяемость определяется именно этими advancement-событиями с отзывом в функциях; это не тождественно `players_tick`. Сама `players_tick` обслуживает накопленные `bow_charge`/`crossbow_charge` и сбрасывает или передаёт их дальше при смене предмета.

Общий правоклик-маршрут устроен иначе: advancement `interaction_rightclickable` срабатывает при взаимодействии с сущностью-триггером `rc_able` и вызывает `items/spells`. Основная рука проверяется через `SelectedItem`; левая — через `weapon.offhand`, отдельными helper-ами `spell_left`, `spell1_left`, `spellsummoner_left`. Они блокируются при `lefthand=1`, а магические вызовы также учитывают `nomagic_active` и, у summoner, `stopper_magic`. Наличие предмета во второй руке само по себе не означает, что код далее читает именно её: конкретные mode/cast helpers следует проверять отдельно.

## Общие луки

`items/bows/charge_add` увеличивает `bow_charge` на 1, если `time_stopped` не активен; при значении ровно 1 и `custom_data.infinite_arrow:1` выдаёт loot `lbc:imba/infinite_arrow`, а при включённых skills зовёт внешний `swrg_kit_integration/game/skills/bow_charge`. В конце отзывается advancement `bow_pulling`. `charge_query` вызывает внешнюю зарядку `shish_gun` при выбранном соответствующем предмете и обнуляет `bow_charge` вместе с `bow_charge_sub`, если выбранный предмет не обычный bow; аналогичный `charge_query_crossbow` обнуляет `crossbow_charge`, если выбран не crossbow. Триггер установки advancement-ов относится к их определениям; сами функции не проверяют наличие лука до изменения счётчиков.

## Madoka Bow

`custom_data.madoka_bow:1` маркирует основу `bow` из loot `lbc:madoka_bow`; пользовательский выстрел проходит через advancement `lbc:madoka_bow` (смотрите его критерий и запрет измерений в [каталоге](lbc_advancements.md)). `shoot` переименовывает ближайшую spectral arrow в обычную arrow, добавляет ближайшей стреле эффект raid omen на 10t с amplifier 76, а затем копирует `lbcID2` и `team_number` владельца в помеченную стрелу; замена spectral-происходит через временную стрелу с теми же Motion, Rotation и Owner. `tick` иногда рисует сердечки (predicate `lbc:chance20`) и при попадании снаряда в блок создаёт загрузку 11×11×11 из измерения `white` в overworld, заполняет ближайшие сундуки таблицей `swrg:chests/default|midle|center`, выдаёт эффект уровня/звук и удаляет marker; фрагмент chest loot принадлежит SWRG-пакету.

При попадании по цели наличие upgraded-предмета в любом контейнерном слоте вызывает `hit_find`: 54 проверки слотов по очереди перезаписывают macro storage, так что при нескольких подходящих предметах выбирается последний совпавший индекс, а не первый. Для выбранного предмета удаляются `upgraded`, `upgradable`, `upgrade_available`, последняя строка lore заменяется на `madoka_cursed` и ставится `pissed_by_madoka_sob_sob_sob_sob_sob:1`. Такой же сброс происходит для offhand; владелец получает сообщение `lost_upgrade`. Отдельная `hit_adv` снимает 10000 raw единиц маны, выдаёт частицы и секретное достижение; в показанном наборе не найден вызов, связывающий её с попаданием.

## Улучшенные арбалеты

`custom_data.imba_fast_crossbow:1` включает повторяемый advancement во время использования crossbow вне `nexus`/`imprinted`; при наличии в инвентаре стрелы он вызывает `imba_fast_crossbow_shoot`. Функция создаёт одну обычную arrow в глазах владельца, задаёт Owner и разбросанный вектор через `raycast_vpered_big_with_razbros`, пробует передать ей trinket-эффекты, играет звук и тратит одну стрелу, кроме `infinite_arrow:1`. Это функция без собственного cooldown; время её повтора задаётся advancement/игрой.

`imba_shotgun_crossbow` создаёт256 стрел с тегами `imba_shotgun,killedarrow`, затем по каждой задаёт Owner, `lbcID2`, team_number, item и Rotation от владельца и запускает веерный `raycast_vpered_razbros`. После этого функция убивает все обычные arrows без `killedarrow` в радиусе1 около владельца; она сама не проверяет инвентарный боезапас и не очищает arrows из inventory. Конечный урон и жизнь созданных стрел зависят от универсального raycast/runtime и будут описаны в соответствующих пакетах. `imba_gatlingator_crossbow`, `imba_sniper_crossbow` и `imba_super_shotgun_crossbow` передают управление трём внешним режимам `workingitems/awaken_tuntija_wands/erecto`; gatling тратит3 arrows, две другие сначала убивают arrows в радиусе1 у глаз, а перед вызовом ставят игроку `caster` и `Distance=0`. Их конечное поражение цели принадлежит внешнему коду и здесь не выводится по имени функции.

## Щиты

`energy_shield` пока нет time stop вызывает `energy_shield_2` и сразу сбрасывает advancement. При каждом срабатывании проходятся все projectiles-raycaster сущности в радиусе 7: `energy_shield_proj` сначала умножает каждую компоненту Motion на 1000, затем записывает её с коэффициентом −0.001; итоговая скорость равна обратной исходной (с округлением), а не исходной скорости, умноженной на −0.001. Затем функция телепортирует projectile на 0.1 блока вперёд по направлению его взгляда, ставит `mirrored` и искры. Затем shield присваивает отражённым снарядам Owner, `owner`, `lbcID2` и `team_number` игрока и снимает `mirrored` с тех, кто дальше 7, но не дальше 14 блоков; наличие advancement-а на попадание отражённой стрелой также описано в [каталоге](lbc_advancements.md).

`holyshield` при отсутствии остановки времени проверяет область на 3 блока вперёд и радиус 4: выдаёт `instant_health` всем `#minecraft:unholy_monsters` и рисует им золотую пыль (для undead-целей этот эффект может наносить урон), удаляет рядом `corrupted_dust` и выдаёт секретное достижение, если найден zombie с `DeathLootTable:"lbc:reactive_shish"`. Это не постоянная аура: команда выполняется только на срабатывании advancement-а, который затем отзывается. `shield_block` не срабатывает при `time_stopped>=2` и в измерениях `nexus`/`imprinted`; иначе находит raycaster в радиусе 2 перед игроком и убивает `shield_blockable`, а `shield_blockable2` — только при условии, что в его текущем блоке воздух; обе группы, принадлежащие тому же игроку или команде по `lbcID2`/`team_number`, помечаются `donotblock` и пропускаются. В конце очищает этот временный тег у сущностей в радиусе 5.

## SLR (магический стрелок)

Предмет задаётся loot `lbc:slr` как carrot-on-a-stick с `custom_data.shooter`, `magicwand`, `mode:1`, `slr`; advancement `lbc:slr` реагирует на использование. Shift-клик меняет режим через внешнюю функцию `other/magic_academy/change_mode_4` и сообщает выбранную цифру 1–4; обычный клик, если есть arrow и `coldownMagnum<=0`, запускает выстрел из точки `^-.2 ^-.2 ^`. `shoot` расходует одну arrow, кроме бесконечных, исполняет один из четырёх рекурсивных лучей по `mode` (standard / poison / explosion / electro), проигрывает звук, добавляет cooldown-score 15, генерирует случайную отдачу по yaw −6..6° и pitch −15..−5° и телепортирует владельца с новой Rotation.

Лучевые функции шагают по `^ ^ ^.2`, увеличивая `Distance` на 1; цикл может выполнить 1500 перемещений и дойти до счётчика 1501, то есть геометрический предел — около 300 блоков, если раньше не встретится преграда. Лучи рисуют частицы и ломают блоки из `#minecraft:breakable_slr`; режим взрыва вместо этого останавливается при не-воздухе из `#minecraft:airs` и создаёт TNT с `fuse:2s` (Short: 2 тика = 0,1 секунды). На каждом шаге селектор цели ограничен `dx=dy=dz=0` (куб примерно 1×1×1 блок); `Distance<=17` для поиска запускающей цели и `Distance<=15` для урона — только дополнительные фильтры, они не превращают куб в сферу радиусом 17 или 15. Повторный поиск при продвижении луча может задеть одну цель несколькими вызовами, поэтому указанное значение нельзя автоматически считать уроном за весь выстрел. Обычный режим наносит общий урон 10 cactus; головная ветка дополнительно наносит 20 урона и снимает helmet, после чего общий урон 10 всё равно выполняется; урон по факту зависит от Minecraft damage cooldown. Poison наносит 8 cactus damage, накладывает poison, slowness и blindness на 10 секунд и hunger на 10 секунд с amplifier 1; electro накладывает slowness, weakness и mining fatigue на 4 секунды с amplifier 127. В explosion-режиме прямой hit наносит 5 cactus damage; блоковая ветка выставляет `Distance=1600`, а `dmg_with_id_explode` при попадании в сущность — `Distance=1700`; обе создают TNT с `fuse:2s` (2 тика = 0,1 секунды).

Исходники этих механик перечислены по точным путям в конце документа.


## Мечи: Banhammer и Blazefire

`custom_data.banhammer:1` увеличивает `swordcharging7` при каждом вызове hit-handler `items/swords`, пока меч выбран; это заряд от подходящих событий попадания, не от каждого тика удержания. Каждые 15 таких вызовов сбрасывается промежуточный счётчик и добавляется `swordcharged7`. Ability обнуляет `Distance`, помечает владельца `caster` и пускает взглядовый луч по 0.4 блока, максимум 200 шагов (80 блоков); первый моб в 10 блоках или преграда вызывает падение молота. Молот item_display появляется на Y+200 с сохранёнными `lbcID2` и `team_number`, владелец ставится на `Distance=19999`, а заряд уменьшается на 1; его `tick_fall` опускает его на 2 блока за каждый из первых 99 Lifetime ticks, при 100 наносит эффект в радиусе 5.5, при 200 убивает display. На приземлении все не-spectator игроки в радиусе 5.5 получают 40 393 одинаковых block-marker particle commands и macro-команду `kick`; если skills включены игроку с `lbc.skill=12` эффект пропускается, и совпавший владелец получает секретное достижение.

`custom_data` меча Blazefire получает заряд `swordcharged9` через hit-handler; применить волну можно из правой или левой руки при заряде не меньше 5. Клик ставит на владельца `firefarting`; `players_2t` проверяет эту метку раз в 2 тика и запускает `tick_5t`. Каждый такой проход тратит 3 заряда, запускает пламенный луч из глаз с продвижением на 1 блок, поражает каждую сущность максимум раз за проход тегом `hitten` на 1 `in_fire` плюс 0.1 `arrow` damage, затем снимает временные метки. Когда charge достигает 0, метка активности снимается и счётчик обнуляется; на цикл проигрывается звук fire charge.

## Chaos Sword, Triple Golden Sword и Kara

`custom_data.chaos_swo:1` запускает `chaos_sword_imba_pre`: это 80 вызовов спавна, то есть две похожие пачки по 40 item_display golden swords на разновысотных точках примерно Y+5..44 и координатных смещениях 0/±11. Каждый display размера3 копирует `lbcID2` и `team_number`, получает случайный угол и отмечается `homing_sword,slowraycast,imba_rc,magic`. При свободном пути мечи на каждом тике преследуют ближайшую допустимую цель, пропуская владельца и союзников по ID/команде; в пределах8 блоков наносят4 arrow damage с владельцем (или4 generic без него), после чего меч разбивается. При попадании в блок он помечается `inground`; блокирующая стена и `Lifetime>=100` завершают полёт. Это залп множества отдельных преследующих мечей.

`custom_data.triple_golden_sword:1` заряжает `swordcharged8` вплоть до 6; click тратит один заряд и выпускает три display-меча веером с yaw −10°, 0°, +10°. Они используют тот же преследующий/попадающийся блокам цикл, что Chaos Sword, но на игроках и мобах отдельно исключают владельца и его команду; каждый попавший меч наносит 4 урона и исчезает. `custom_data.kara:1` запускает central slowraycast marker; на каждом `tick_kara` он уничтожает блоки вдоль вертикали от Y=0 до Y=319, оставляет end_rod-частицы/взрывной звук и пока Lifetime<100 рождает два боковых луча с yaw ±30° и смещением ±0.8. Marker исчезает при Lifetime 100; сколько блоков он реально разрушит зависит от мира и позиции, а прямой урон живым целям в этих функциях отсутствует.

## Fevgalea Star

`custom_data.fevgalea_star:1` добавляет `fevgalea_charge` при каждом обработанном ударе и порождает на случайной из трёх полос marker-звезду с `lifetime:40`, Owner ID и командой. По клику обычная атака при нулевом заряде требует 1000 mana и тратит её, создавая marker дождя на 100 ticks на 15 блоков над владельцем; он движется вперёд и с 50% вероятностью каждого шага выпускает waterdrop, у которой 25 ticks жизни и луч длиной 11 шагов, повреждающий мобов внешним `lbcr:dmg_bypl_3nocd`. Shift при заряде до 6 требует 5000 mana, лечит владельца мгновенно (amplifier 99), даёт Resistance IV 7s, добавляет `void_resistance` 7 и тратит 5000 mana; её луч на 120 шагов наносит вокруг владельца урон, равный текущему Health цели, за вычетом защиты от `nomagic_active` и `stopper_magic`.

При заряде 1..6 обычный клик запускает луч звезды: три цветовых варианта повреждают до трёх узких точек каждой позиции внешним `lbcr:damagewithid_10maxhealth`, `...10health` или `...10speed`; он проходит до 10 шагов по 0.2 блока и завершается через `lbcr:raycast_end_with_tp`. Shift с зарядом 7+ атакует всех не-союзных мобов в радиусе 20: `damage_100hp` передаёт `floor(Health × 0.5)` как целый урон в общий `damage_with_tmp`, затем снимает 7 заряда; обычный charged-click вместо этого расходует 1 заряд и запускает shift-луч. Содержимое внешнего damage helper определяет итоговое снятие здоровья/скорости, поэтому здесь даны только точные переданные ему значения.

## Piston Sword, Ruchka и WTF

Клик Piston Sword запускает луч до200 шагов с шагом0,33, останавливаясь на позиции, где блок не равен ID `minecraft:air`; затем рекурсивно пытается сдвинуть цепь блоков вперёд, клонируя текущий блок в следующую позицию и заменяя исходную позицию воздухом. При неудаче clone ветка возвращает `return fail`; если последующая очистка исходной позиции не сработала, `ray_2_r_blocked` сбрасывает временный флаг и очищает следующий блок. Поэтому итог зависит от успешности clone/setblock. Удар проигрывает piston extend; прямого entity damage в этом луче нет.

`ruchka` не обращается к item custom_data в своей функции: диспетчер `items/swords` вызывает её при выбранном предмете с `custom_data.ruchka:1`, если рядом нет `stopper`. Она сохраняет team владельца и находит недружеского игрока в 7 блоках с `HurtTime:10s`, затем передаёт эффект внешнему `projectiles/gadgets/ruchka_pl`; после этого забирает у владельца один предмет с моделью `lbc:ruchka`. Для `wtf_item:1` похожий триггер срабатывает по удару: предмет расходуется, выбирается одна недружеская сущность с `HurtTime:10s` в 7 блоках без сортировки по близости (селектор не ограничен игроками), и на 10 блоков за её спиной создаётся один item_display `shef` размером3, а цель получает `shef_hated`. Display каждый tick движется к помеченной сущности; отдельный damage selector может задеть одну сущность с тегом `shef_hated` в точечном кубе+distance5 у display и наносит ей10 cactus damage. Если помеченной сущности нет, display удаляется; звук выбирается равновероятно из шести вариантов с predicate `chance0_5`.

## Fish-разрушители (ручные функции)

`items/swords/get_penised*` вызываются core-диспетчером при попадании по не-spectator игроку без команды, получившему урон в этом тике (`HurtTime:10s`) в пределах 7 блоков. Diamond даёт `lookback +10` и Blindness 5s. `_2`/`_3` выполняют `tp @s ~ ~ ~` (координаты не меняются), затем `spreadplayers ~ ~ 0 100 false @s`, распределяя цель по горизонтали в пределах 100 блоков; `_2` добавляет lookback +10, `_3` ещё target/nomagic +100 и Unluck 100s amplifier 100. `_4`/`_5` вызывают `lbc:projectiles/teleport_nahui`, затем добавляют lookback/scared/target/nomagic и Unluck по 1000/10000; конечный эффект helper не приписывается без его кода. Все дают Blindness 5s и звук puffer fish всем игрокам в позиции команды; варианты 2–5 содержат по 13 звуковых команд. Здесь нет цены, cooldown или расхода предмета.


## Danmaku Staff и его volleys

Loot `lbc:danmaku_staff` задаёт `custom_data.danmaku_staff:1`, `spellwithcooldown`, `staff`, `magicwand`; обработчик работает в `players_tick` только при `danmaku>=1` и отсутствии `stopper_magic` в 20 блоках. Обычный клик вызывает `click_2`, добавляет 20 к `danmaku`, а затем списывает `danmaku_staff_mana_use` (в `config` это 1000); если все четыре части брони имеют `mage:1`, дополнительно увеличивается `manamax` на bonus 150, а не текущая mana. Вызов trinket helper отдельный. Каждый активный тик уменьшает danmaku на 1, поэтому окно атаки 20 server ticks; снимок `danmaku % 20` делит выстрелы по рукам: чётные позиции запускают mainhand series, нечётные — offhand, а fallback main series на чётной позиции срабатывает только при отсутствии spellid159 в обеих руках и danmaku_staff в обеих руках и всех container-слотах. Advanced tick выбирает паттерны с `upgraded:1` в соответствующей руке; spellid 159 и uniwand также используют ряд danmaku.

В базовых рядах `shoot_1..10` 10 фаз чередуются через 2 тика: 1 и 7 выпускают по 5 обычных и 4 homing; 2–6 — по 2 обычных, 8–10 — по 4. В left rows `shoot_1l..9l` по 2 обычных, `shoot_10l` — 5 обычных + 12 homing. Upgraded right 1–9 дают по 8 homing, right 10 — 17 обычных; upgraded left 1–10 дают по 6 обычных с расстановкой по кольцу. Каждый projectile — item_display с `lbcID2`/`team_number` владельца и `active`; homing дополнительно помечены `homing`. Mana списывается при активации, а не на каждый снаряд.

`click` сначала выполняет `click_2`, затем списывает configured цену. Normal click добавляет 20 к danmaku; sneak внутри `click_2` предварительно прибавляет стоимость независимо от score, поэтому последующее списание компенсируется. При `danmaku_score<50` sneak не стреляет и не создаёт бомбу; при score ≥50 создаёт два зеркальных красных item_display, сохраняет owner/team/score в `Distance`, обнуляет `danmaku_score`, играет звук и при `upgradable` + score ≥1500 вызывает внешний upgrade-check. Обычная бомба увеличивает Lifetime на 3 за тик, масштабируется и вычисляет радиус nested integer-макросом около 0,06×Lifetime; она удаляет чужие projectiles и magic/mecha raycasters/mob-projectiles, а мобам наносит 1 magic damage; owner, team и strict-map-object исключаются. При Lifetime больше сохранённого score обычная бомба сразу убивается. Fast helper увеличивает Lifetime на 10, наносит 2 magic damage и после превышения `Distance` копит `empty` до 60 и удаляется; вызова fast-варианта в этих 256 источниках нет.


## Sauvojen Wand: random wand and upgraded modes

`cast` запускается advancement-ом базовой или улучшенной Sauvojen Wand. Обычное применение тратит 2500 mana (настройка `config`), добавляет 150 к максимуму маны только при наличии `mage:1` во всех четырёх слотах брони и может вызвать SWRG trinket hook. Затем случайно выбирает одну из девяти стихийных палочек (`ruby, terra, electro, air, acid, teleport, stone, fire, blind`), ставит её на 2 блока впереди и копирует `lbcID2`/`team_number`; вызов босса ghast закомментирован. Shift-применение улучшенной палочки начинает медитацию со счёта 620; улучшенное применение создаёт тот же набор и выдаёт соответствующее скрытое достижение.

Базовая летающая палочка ищет ближайшего моба в пределах 150 блоков, пропуская spectator, владельца, союзников и strict map objects; завершается на не-air block или Lifetime 200. Период эмиссии: ruby/stone/air 60 тиков, blind 10, acid/electro/terra 20, teleport 80, fire 5. Эмиттеры создают marker-raycasters lifetime ruby 50, stone 80, air 120, teleport 60, terra 200, acid 300, blind 100; electro создаёт четыре raycaster-marker lifetime 20/25/30/35. Fire создаёт small_fireball с ID/team и Motion от временного marker; эта функция Owner не задаёт. Дальнейшие попадания принадлежат projectile/raycast helpers и не выводятся по model/tag.

Улучшенные палочки живут до Lifetime 400, наводятся на допустимого моба в пределах 150 и вызывают mode-функции по периодам ruby 10, stone 40, air 10, acid/electro/teleport 5, terra каждый тик, fire 5, blind 5. Это callback-период из `flying_wand_tick_upgraded`; ruby helper за запуск дважды увеличивает `cold`, поэтому `%10` оставляет достижимыми индексы 1,3,5,7,9 и пропускает чётные. Mode-функции делегируют в `workingitems/sauvojen_staffs/**` и `workingitems/awaken_tuntija_wands/**`; каждый тик также рандомизирует model (terra — одна из 4, прочие — одна из 3).
## Sauvojen meditation (upgrade attempt)

Shift-клик с улучшенной Sauvojen wand запускает отсчёт 620 score-единиц. `players_tick` проверяет его каждый игровой тик, только когда рядом нет `stopper_magic`; `tick` прерывает медитацию, если палочка ушла из главной руки. Для достижения score 1 нужно не менее 31 секунды непрерывного удержания. Получение урона в фазах вызывает `fail`: счёт сбрасывается, маркер медитации убивается и через 3 тика ставится ещё одна очистка. Субтитры появляются на отметках 620, 560, 520, 420, 310, 300, 160, 140 и 30. На score 1 вызывается внешняя проверка Magic Academy `can_upgrade`, а медиатор удаляется; сама замена предмета находится во внешней функции.

Фазы меняют мир и создают raycaster/display, а не сводятся к урону. Score 620 запускает ruby: `fill ... air destroy` очищает вертикальные срезы шириной от 7 до 21 блока на высоте 25, после чего появляется AEC radius 10. На 550..600 каждая стадия создаёт три terra display; на 520/500/480/460/440 electro строит соответственно 4/8/12/16/20 display за вызов, а на 420 активирует electroweb loop. Air-фаза на 419..383 создаёт высокий красный wool display, сначала в 18 блоках впереди и далее на каждом score сдвигает точку к 18 блокам позади; при Lifetime 60 выпускает четыре TNT `fuse:1` (один тик), наносит 8 magic мобам в distance 7 и удаляется. На 320 размещается `lbc:stone_cylinder` со смещением −23,−4,−23; на 300 появляется acid marker; в диапазоне 200..290 с шансом 50% вызывается acid-ball spawn. На 160 выдаются levitation 15s и levitation amplifier 20 на 1s, на 140 — darkness 15s. В диапазоне 40..140 chance 50 запускает scare-последовательность; в 40..120 chance 70 увеличивает countdown на 1. На 20 `phase_fire` очищает область 31×21×31 под игроком, заливает лавой область 31×30×31 и отдельный слой 121×1×121, снимает levitation, даёт Slow Falling 4s и сбрасывает `scared`.

Acid marker испускает зелёные частицы и движется на 0,25 блока за тик до Lifetime 130. `acidballer` живёт 50 тиков и с шансом 50% создаёт следующий acidball; у шара случайные углы и integer X/Z offsets от −10 до 10, затем он помещается на Y+9. Шар сдвигается на 0,4 за шаг и допускает три повторных шага (не более 1,2 блока); живёт до 40 тиков, выбирает одного `#mobs` в distance 5 и кубе 1×1×1 у смещённой точки, наносит 4 `in_fire` и poison 10s, а при встрече блока проводит цепочку окраски concrete powder до black, после black удаляет блок. `damaged` наносит 8 magic и сбрасывает score на fail, если он положителен; `damaged_acid` наносит 4 in_fire и poison 10s до такого же fail-gate. Blindness/scare ветка даёт Darkness 15s и Levitation 1s, создаёт interaction 4×4, переносит случайную scare-picture на 10 блоков и показывает одну из 10 моделей; picture/face могут двигаться вверх на 0,1 за тик и удаляются на Lifetime 100. `tick_blind_check` заканчивается execute-condition без `run`, поэтому не запускает следующего действия.



## Uni Wand

Loot `lbc:uniwand` создаёт carrot-on-a-stick с `custom_data.uniwand:1`, `mana:0`, `spellcount:0`; `click` читает сохранённый `mana` как требование и запускает cast только если текущая mana выше него либо одновременно меньше него и `manamax` тоже меньше порога. При равенстве требованию и при нехватке текущей маны, когда manamax уже не ниже порога, функция не делает ничего; в таблице базового предмета порог равен нулю, поэтому равная нулю мана тоже не проходит условие. `cast` вычитает это значение, начинает `currspell=0`, считывает `spellcount`, затем `cast_loop` повышает номер до 1 и передаёт его в `items/libspells/select_wand`; повтор продолжается, пока `currspell < spellcount`. Поскольку первый spell cast происходит до сравнения, `spellcount=0` всё равно даёт один вызов выбора; для положительного N цикл вызывает последовательно spell slots 1..N. Сами эффекты определяются выбранными spell IDs/макросом в `select_wand`, а не трёхфункциональным циклом Uni Wand.


Три meditation-функции остаются пустыми: `loop_particle`, `loop_particle_white` и `phase_acid` не исполняют команд; вызов `loop_particle` не добавляет визуального эффекта. Пустой `phase_acid` отдельный от рабочего `phase_acid_tick`, который запускается от acidmedit marker. В coverage они оставлены `documented` как фактические no-op функции.

## Источники

### Общие луки

- [items/bows/charge_add.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/charge_add.mcfunction)
- [items/bows/charge_query.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/charge_query.mcfunction)
- [items/bows/charge_query_crossbow.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/charge_query_crossbow.mcfunction)

### Madoka Bow

- [items/bows/kaname_bow/chest_loot_spawn.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/kaname_bow/chest_loot_spawn.mcfunction)
- [items/bows/kaname_bow/fall.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/kaname_bow/fall.mcfunction)
- [items/bows/kaname_bow/hit.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/kaname_bow/hit.mcfunction)
- [items/bows/kaname_bow/hit_adv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/kaname_bow/hit_adv.mcfunction)
- [items/bows/kaname_bow/hit_find.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/kaname_bow/hit_find.mcfunction)
- [items/bows/kaname_bow/hit_find_offhand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/kaname_bow/hit_find_offhand.mcfunction)
- [items/bows/kaname_bow/hit_find_stor.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/kaname_bow/hit_find_stor.mcfunction)
- [items/bows/kaname_bow/shoot.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/kaname_bow/shoot.mcfunction)
- [items/bows/kaname_bow/spec_in_norm.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/kaname_bow/spec_in_norm.mcfunction)
- [items/bows/kaname_bow/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/kaname_bow/tick.mcfunction)

### Улучшенные арбалеты

- [items/crossbow/imba_fast_crossbow.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/crossbow/imba_fast_crossbow.mcfunction)
- [items/crossbow/imba_fast_crossbow_shoot.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/crossbow/imba_fast_crossbow_shoot.mcfunction)
- [items/crossbow/imba_gatlingator_crossbow.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/crossbow/imba_gatlingator_crossbow.mcfunction)
- [items/crossbow/imba_gatlingator_crossbow_shoot.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/crossbow/imba_gatlingator_crossbow_shoot.mcfunction)
- [items/crossbow/imba_shotgun_crossbow.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/crossbow/imba_shotgun_crossbow.mcfunction)
- [items/crossbow/imba_shotgun_crossbow_at.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/crossbow/imba_shotgun_crossbow_at.mcfunction)
- [items/crossbow/imba_sniper_crossbow.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/crossbow/imba_sniper_crossbow.mcfunction)
- [items/crossbow/imba_super_shotgun_crossbow.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/crossbow/imba_super_shotgun_crossbow.mcfunction)

### Щиты

- [items/shields/energy_shield.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shields/energy_shield.mcfunction)
- [items/shields/energy_shield_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shields/energy_shield_2.mcfunction)
- [items/shields/energy_shield_proj.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shields/energy_shield_proj.mcfunction)
- [items/shields/energy_shield_rayc.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shields/energy_shield_rayc.mcfunction)
- [items/shields/holyshield.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shields/holyshield.mcfunction)
- [items/shields/shield_block.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shields/shield_block.mcfunction)
- [items/shields/shield_block_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shields/shield_block_2.mcfunction)

### SLR (магический стрелок)

- [items/shooter/slr/change_mode.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/change_mode.mcfunction)
- [items/shooter/slr/click.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/click.mcfunction)
- [items/shooter/slr/dmg_with_id_electro.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/dmg_with_id_electro.mcfunction)
- [items/shooter/slr/dmg_with_id_explode.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/dmg_with_id_explode.mcfunction)
- [items/shooter/slr/dmg_with_id_poison.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/dmg_with_id_poison.mcfunction)
- [items/shooter/slr/dmg_with_id_standart.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/dmg_with_id_standart.mcfunction)
- [items/shooter/slr/dmg_with_id_standart_head.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/dmg_with_id_standart_head.mcfunction)
- [items/shooter/slr/electro.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/electro.mcfunction)
- [items/shooter/slr/explode.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/explode.mcfunction)
- [items/shooter/slr/explode_mob.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/explode_mob.mcfunction)
- [items/shooter/slr/kb.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/kb.mcfunction)
- [items/shooter/slr/poison.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/poison.mcfunction)
- [items/shooter/slr/raycast_electro.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/raycast_electro.mcfunction)
- [items/shooter/slr/raycast_explode.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/raycast_explode.mcfunction)
- [items/shooter/slr/raycast_poison.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/raycast_poison.mcfunction)
- [items/shooter/slr/raycast_standart.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/raycast_standart.mcfunction)
- [items/shooter/slr/shoot.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/shoot.mcfunction)

### Мечи: Banhammer и Blazefire

- [items/swords/banhammer/ability.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/banhammer/ability.mcfunction)
- [items/swords/banhammer/ability_at.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/banhammer/ability_at.mcfunction)
- [items/swords/banhammer/hurt.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/banhammer/hurt.mcfunction)
- [items/swords/banhammer/rc.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/banhammer/rc.mcfunction)
- [items/swords/banhammer/rc_end.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/banhammer/rc_end.mcfunction)
- [items/swords/banhammer/tick_fall.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/banhammer/tick_fall.mcfunction)
- [items/swords/banhammer/tick_fall_end.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/banhammer/tick_fall_end.mcfunction)
- [items/swords/banhammer/tick_fall_end_advancement.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/banhammer/tick_fall_end_advancement.mcfunction)
- [items/swords/blazefire_sword/click.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/blazefire_sword/click.mcfunction)
- [items/swords/blazefire_sword/tick_5t.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/blazefire_sword/tick_5t.mcfunction)
- [items/swords/blazefire_sword/tick_5t_rc.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/blazefire_sword/tick_5t_rc.mcfunction)
- [items/swords/blazefire_sword/tick_5t_rc_damaged.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/blazefire_sword/tick_5t_rc_damaged.mcfunction)

### Chaos Sword, Triple Golden Sword и Kara

- [items/swords/chaos_sword_imba/homing_sword.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/chaos_sword_imba/homing_sword.mcfunction)
- [items/swords/chaos_sword_imba/homing_sword_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/chaos_sword_imba/homing_sword_2.mcfunction)
- [items/swords/chaos_sword_imba/homing_sword_break.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/chaos_sword_imba/homing_sword_break.mcfunction)
- [items/swords/chaos_sword_imba/homing_sword_break_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/chaos_sword_imba/homing_sword_break_pre.mcfunction)
- [items/swords/chaos_sword_imba/homing_sword_break_raycheck.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/chaos_sword_imba/homing_sword_break_raycheck.mcfunction)
- [items/swords/chaos_sword_imba/homing_sword_dmg.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/chaos_sword_imba/homing_sword_dmg.mcfunction)
- [items/swords/chaos_sword_imba/homing_sword_dmg_mob.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/chaos_sword_imba/homing_sword_dmg_mob.mcfunction)
- [items/swords/chaos_sword_imba.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/chaos_sword_imba.mcfunction)
- [items/swords/chaos_sword_imba_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/chaos_sword_imba_pre.mcfunction)
- [items/swords/imba_kara_slab/hit.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/imba_kara_slab/hit.mcfunction)
- [items/swords/imba_kara_slab/tick_kara.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/imba_kara_slab/tick_kara.mcfunction)
- [items/swords/imba_kara_slab/tick_kara_central.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/imba_kara_slab/tick_kara_central.mcfunction)
- [items/swords/triple_golden_sword/click.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/triple_golden_sword/click.mcfunction)
- [items/swords/triple_golden_sword/damage.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/triple_golden_sword/damage.mcfunction)
- [items/swords/triple_golden_sword/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/triple_golden_sword/tick.mcfunction)
- [items/swords/triple_golden_sword/tick_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/triple_golden_sword/tick_2.mcfunction)
- [items/swords/triple_golden_sword/tick_2_mob.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/triple_golden_sword/tick_2_mob.mcfunction)
- [items/swords/triple_golden_sword/tick_2_player.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/triple_golden_sword/tick_2_player.mcfunction)
- [items/swords/triple_golden_sword/tick_loop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/triple_golden_sword/tick_loop.mcfunction)
- [items/swords/triple_golden_sword/tick_loop_mob.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/triple_golden_sword/tick_loop_mob.mcfunction)
- [items/swords/triple_golden_sword/triple_sword_spawn.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/triple_golden_sword/triple_sword_spawn.mcfunction)

### Fevgalea Star

- [items/swords/fevgalea_star/click_charged.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/click_charged.mcfunction)
- [items/swords/fevgalea_star/click_charged_shift.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/click_charged_shift.mcfunction)
- [items/swords/fevgalea_star/click_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/click_select.mcfunction)
- [items/swords/fevgalea_star/click_uncharged.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/click_uncharged.mcfunction)
- [items/swords/fevgalea_star/click_uncharged_shift.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/click_uncharged_shift.mcfunction)
- [items/swords/fevgalea_star/click_uncharged_shift_damage.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/click_uncharged_shift_damage.mcfunction)
- [items/swords/fevgalea_star/click_uncharged_shift_rc.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/click_uncharged_shift_rc.mcfunction)
- [items/swords/fevgalea_star/damage_100hp.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/damage_100hp.mcfunction)
- [items/swords/fevgalea_star/hit.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/hit.mcfunction)
- [items/swords/fevgalea_star/hit_shoot.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/hit_shoot.mcfunction)
- [items/swords/fevgalea_star/rain_projectile_spawn.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/rain_projectile_spawn.mcfunction)
- [items/swords/fevgalea_star/star_charge_1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/star_charge_1.mcfunction)
- [items/swords/fevgalea_star/star_charge_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/star_charge_2.mcfunction)
- [items/swords/fevgalea_star/star_charge_3.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/star_charge_3.mcfunction)
- [items/swords/fevgalea_star/star_charge_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/star_charge_select.mcfunction)
- [items/swords/fevgalea_star/star_rain_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/star_rain_tick.mcfunction)
- [items/swords/fevgalea_star/star_rain_waterdrops_spawn.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/star_rain_waterdrops_spawn.mcfunction)
- [items/swords/fevgalea_star/star_raindrop_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/star_raindrop_tick.mcfunction)

### Fish-разрушители (ручные функции)

- [items/swords/get_penised.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/get_penised.mcfunction)
- [items/swords/get_penised_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/get_penised_2.mcfunction)
- [items/swords/get_penised_3.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/get_penised_3.mcfunction)
- [items/swords/get_penised_4.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/get_penised_4.mcfunction)
- [items/swords/get_penised_5.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/get_penised_5.mcfunction)

### Piston Sword, Ruchka и WTF

- [items/swords/piston_sword/click.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/piston_sword/click.mcfunction)
- [items/swords/piston_sword/ray.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/piston_sword/ray.mcfunction)
- [items/swords/piston_sword/ray_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/piston_sword/ray_2.mcfunction)
- [items/swords/piston_sword/ray_2_r.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/piston_sword/ray_2_r.mcfunction)
- [items/swords/piston_sword/ray_2_r_blocked.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/piston_sword/ray_2_r_blocked.mcfunction)
- [items/swords/piston_sword/ray_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/piston_sword/ray_pre.mcfunction)
- [items/swords/ruchka.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/ruchka.mcfunction)
- [items/swords/wtf_item/hit.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/wtf_item/hit.mcfunction)
- [items/swords/wtf_item/random_sound.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/wtf_item/random_sound.mcfunction)
- [items/swords/wtf_item/random_sound_low.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/wtf_item/random_sound_low.mcfunction)
- [items/swords/wtf_item/spawn.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/wtf_item/spawn.mcfunction)
- [items/swords/wtf_item/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/wtf_item/tick.mcfunction)

### Danmaku Staff и его volleys

- [items/wands/danmaku_staff/bomb_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/bomb_tick.mcfunction)
- [items/wands/danmaku_staff/bomb_tick_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/bomb_tick_2.mcfunction)
- [items/wands/danmaku_staff/bomb_tick_2_fast.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/bomb_tick_2_fast.mcfunction)
- [items/wands/danmaku_staff/bomb_tick_fast.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/bomb_tick_fast.mcfunction)
- [items/wands/danmaku_staff/bomb_tick_fast_scale.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/bomb_tick_fast_scale.mcfunction)
- [items/wands/danmaku_staff/click.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/click.mcfunction)
- [items/wands/danmaku_staff/click_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/click_2.mcfunction)
- [items/wands/danmaku_staff/click_bomb.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/click_bomb.mcfunction)
- [items/wands/danmaku_staff/damage.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/damage.mcfunction)
- [items/wands/danmaku_staff/detect_rec.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/detect_rec.mcfunction)
- [items/wands/danmaku_staff/projectile_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/projectile_tick.mcfunction)
- [items/wands/danmaku_staff/shoot.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot.mcfunction)
- [items/wands/danmaku_staff/shoot_1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_1.mcfunction)
- [items/wands/danmaku_staff/shoot_10.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_10.mcfunction)
- [items/wands/danmaku_staff/shoot_10_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_10_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_10l.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_10l.mcfunction)
- [items/wands/danmaku_staff/shoot_10l_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_10l_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_1_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_1_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_1l.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_1l.mcfunction)
- [items/wands/danmaku_staff/shoot_1l_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_1l_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_2.mcfunction)
- [items/wands/danmaku_staff/shoot_2_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_2_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_2l.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_2l.mcfunction)
- [items/wands/danmaku_staff/shoot_2l_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_2l_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_3.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_3.mcfunction)
- [items/wands/danmaku_staff/shoot_3_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_3_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_3l.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_3l.mcfunction)
- [items/wands/danmaku_staff/shoot_3l_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_3l_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_4.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_4.mcfunction)
- [items/wands/danmaku_staff/shoot_4_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_4_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_4l.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_4l.mcfunction)
- [items/wands/danmaku_staff/shoot_4l_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_4l_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_5.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_5.mcfunction)
- [items/wands/danmaku_staff/shoot_5_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_5_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_5l.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_5l.mcfunction)
- [items/wands/danmaku_staff/shoot_5l_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_5l_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_6.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_6.mcfunction)
- [items/wands/danmaku_staff/shoot_6_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_6_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_6l.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_6l.mcfunction)
- [items/wands/danmaku_staff/shoot_6l_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_6l_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_7.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_7.mcfunction)
- [items/wands/danmaku_staff/shoot_7_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_7_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_7l.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_7l.mcfunction)
- [items/wands/danmaku_staff/shoot_7l_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_7l_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_8.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_8.mcfunction)
- [items/wands/danmaku_staff/shoot_8_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_8_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_8l.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_8l.mcfunction)
- [items/wands/danmaku_staff/shoot_8l_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_8l_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_9.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_9.mcfunction)
- [items/wands/danmaku_staff/shoot_9_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_9_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_9l.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_9l.mcfunction)
- [items/wands/danmaku_staff/shoot_9l_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_9l_upgraded.mcfunction)
- [items/wands/danmaku_staff/shoot_homing.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/shoot_homing.mcfunction)
- [items/wands/danmaku_staff/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/tick.mcfunction)
- [items/wands/danmaku_staff/tick_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/tick_left.mcfunction)
- [items/wands/danmaku_staff/tick_left_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/tick_left_upgraded.mcfunction)
- [items/wands/danmaku_staff/tick_right.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/tick_right.mcfunction)
- [items/wands/danmaku_staff/tick_right_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/tick_right_upgraded.mcfunction)

### Sauvojen Wand: random wand and upgraded modes

- [items/wands/sauvojen_wand/cast.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/cast.mcfunction)
- [items/wands/sauvojen_wand/cast_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/cast_2.mcfunction)
- [items/wands/sauvojen_wand/cast_2_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/cast_2_upgraded.mcfunction)
- [items/wands/sauvojen_wand/cast_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/cast_upgraded.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_kill.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_kill.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_acid.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_acid.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_acid_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_acid_upgraded.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_acid_upgraded_change_model.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_acid_upgraded_change_model.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_air.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_air.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_air_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_air_upgraded.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_air_upgraded_change_model.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_air_upgraded_change_model.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_blind.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_blind.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_blind_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_blind_upgraded.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_blind_upgraded_change_model.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_blind_upgraded_change_model.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_electro.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_electro.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_electro_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_electro_upgraded.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_electro_upgraded_change_model.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_electro_upgraded_change_model.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_fire.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_fire.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_fire_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_fire_upgraded.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_fire_upgraded_change_model.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_fire_upgraded_change_model.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_ruby.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_ruby.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_ruby_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_ruby_upgraded.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_ruby_upgraded_change_model.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_ruby_upgraded_change_model.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_stone.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_stone.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_stone_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_stone_upgraded.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_stone_upgraded_change_model.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_stone_upgraded_change_model.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_teleport.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_teleport.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_teleport_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_teleport_upgraded.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_teleport_upgraded_change_model.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_teleport_upgraded_change_model.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_terra.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_terra.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_terra_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_terra_upgraded.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_terra_upgraded_change_model.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_terra_upgraded_change_model.mcfunction)
- [items/wands/sauvojen_wand/flying_wand_tick_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_upgraded.mcfunction)

### Sauvojen meditation (upgrade attempt)

- [items/wands/sauvojen_wand/meditate/acid_ball_spawn.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/acid_ball_spawn.mcfunction)
- [items/wands/sauvojen_wand/meditate/acid_ball_spawn1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/acid_ball_spawn1.mcfunction)
- [items/wands/sauvojen_wand/meditate/damaged.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/damaged.mcfunction)
- [items/wands/sauvojen_wand/meditate/damaged_acid.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/damaged_acid.mcfunction)
- [items/wands/sauvojen_wand/meditate/fail.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/fail.mcfunction)
- [items/wands/sauvojen_wand/meditate/fail1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/fail1.mcfunction)
- [items/wands/sauvojen_wand/meditate/loop_particle.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/loop_particle.mcfunction)
- [items/wands/sauvojen_wand/meditate/loop_particle_1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/loop_particle_1.mcfunction)
- [items/wands/sauvojen_wand/meditate/loop_particle_12.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/loop_particle_12.mcfunction)
- [items/wands/sauvojen_wand/meditate/loop_particle_white.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/loop_particle_white.mcfunction)
- [items/wands/sauvojen_wand/meditate/meditator_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/meditator_tick.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_acid.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_acid.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_acid_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_acid_tick.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_acid_tick_baller.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_acid_tick_baller.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_acid_tick_baller1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_acid_tick_baller1.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_acid_tick_baller_ball.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_acid_tick_baller_ball.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_acid_tick_baller_ball_block_corrupt.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_acid_tick_baller_ball_block_corrupt.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_acid_tick_baller_ball_block_corrupt1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_acid_tick_baller_ball_block_corrupt1.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_air.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_air.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_air1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_air1.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_air_explode.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_air_explode.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_air_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_air_tick.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_electro_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_electro_tick.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_electro_tick_at.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_electro_tick_at.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_electro_tick_at_dmg.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_electro_tick_at_dmg.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_electro_tick_at_dmgpre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_electro_tick_at_dmgpre.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_electro_tick_at_loop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_electro_tick_at_loop.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_electro_tick_at_loop_dmg.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_electro_tick_at_loop_dmg.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_electro_tick_at_loop_dmg_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_electro_tick_at_loop_dmg_pre.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_electro_tick_at_loop_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_electro_tick_at_loop_pre.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_fire.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_fire.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_ruby.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_ruby.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_stone.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_stone.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_terra_spawn.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_terra_spawn.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_terra_spawn1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_terra_spawn1.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_terra_spawn1_setup.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_terra_spawn1_setup.mcfunction)
- [items/wands/sauvojen_wand/meditate/phase_terra_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_terra_tick.mcfunction)
- [items/wands/sauvojen_wand/meditate/start.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/start.mcfunction)
- [items/wands/sauvojen_wand/meditate/stop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/stop.mcfunction)
- [items/wands/sauvojen_wand/meditate/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/tick.mcfunction)
- [items/wands/sauvojen_wand/meditate/tick_blind.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/tick_blind.mcfunction)
- [items/wands/sauvojen_wand/meditate/tick_blind_1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/tick_blind_1.mcfunction)
- [items/wands/sauvojen_wand/meditate/tick_blind_1at.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/tick_blind_1at.mcfunction)
- [items/wands/sauvojen_wand/meditate/tick_blind_check.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/tick_blind_check.mcfunction)
- [items/wands/sauvojen_wand/meditate/tick_blind_face.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/tick_blind_face.mcfunction)
- [items/wands/sauvojen_wand/meditate/tick_blind_picture.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/tick_blind_picture.mcfunction)
- [items/wands/sauvojen_wand/meditate/tick_blind_up.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/tick_blind_up.mcfunction)
- [items/wands/sauvojen_wand/meditate/tick_ok.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/tick_ok.mcfunction)

### Uni Wand

- [items/wands/uniwand/cast.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/uniwand/cast.mcfunction)
- [items/wands/uniwand/cast_loop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/uniwand/cast_loop.mcfunction)
- [items/wands/uniwand/click.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/uniwand/click.mcfunction)
