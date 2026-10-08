# Предметы: арканумы, trinkets и связанные механики

Покрытие основано на всех 201 исходных `.mcfunction` из `research_jobs.json`; каждый источник связан с разделом по механике. Прямые конечные эффекты, параметры, контекст и прекращение описаны там, где видны в этом пакете; внешние владельцы и отсутствующие локальные endpoints отмечены отдельно.

## Общие предметы и системные обработчики

`bows`, `crossbows`, `armor`, `swords` и `spells` — игровые dispatchers: они реагируют на удерживаемый предмет/боевое событие и зовут дочерние `workingitems:*`, `other:*`, локальные способности и SWRG hooks. `bows` каждый боевой проход смотрит `custom_data` выбранного лука, но перед выполнением explosive/tntbow/shulker/railgun ограничивает некоторые ветви маркерами `stopper` или `stopper_magic`; `tmt_item` отдельно вызывает TMT trigger при `tmticked!=1`. В конце сбрасываются bow-charge score. `bows_get` берёт ближайшую стрелу в 10 блоках, записывает `lbcID2/team_number` владельца и применяет свойства типа стрелы; эти вызовы задают принадлежность и фильтры, а не конечное попадание. `bows_get_trinket` срабатывает при зарегистрированном SWRG trinket hook и последовательно проверяет `custom_data.trinket` blaze, ghast, penis_gun, bedrcock, volos, cocktail, fourier, rocket_spell; найденный trinket передаёт число копий как временный `tmp` заряд, после чего `tmp` сбрасывается.

`damaged_get_trinket` обрабатывает полученный урон и запускает banangay→RTP, gneiss_stone, hedgehog_gauntlet, ice_gem, drojji→acid, mandoahuet→bass, intercal, snow_pearl, psycho_soul, poly_gem→polymorph, sunprism, blood_anal_boom→TNT, mana_gem. Для каждой проверки временный `tmp` становится количеством предметов с точным `custom_data.trinket`, а после большинства ветвей сбрасывается в 0. `swords_get_trinket` отдельно проверяет acid, bass, intercal, snow_pearl, psycho_soul при SWRG melee hook; `cast_wand` вызывает blood-anal TNT и mana-gem эффекты, если такие trinkets есть на игроке. Подробности подтверждённых локальных исходов выше; внешние effect owners остаются отдельными пакетами.

`changed_inventory/sel` проходит по содержимому `container.*`, `weapon.*` и четырём слотам брони и синхронизирует activation-теги для artefact custom_data, в том числе gaara/save/magnet/death_compass, techno/technomk2, gownoleggings, chaoschestplate, paladin, nanobots, overload, reactive и stand items. Для Chaos и Paladin полный set bonus включается только при счётчике 4+ частей, иначе снимается; остальные теги отражают наличие соответствующего предмета или комбинации и читаются игровыми ability-loop-ами. Внешний dispatch tick не входит в этот участок, поэтому здесь фиксируется состояние включения, а не полный эффект каждого брони/артефакта. Четыре `fix_attribute_*` повторно применяют `lbc:attributefix/<vanilla armor id>` к стандартным helmet/chestplate/leggings/boots в соответствующем слоте (включая turtle helmet); они не меняют тип предмета и не применяют модификатор к нестандартной броне.

Mellstroy mask equip check играет один из двух звуков `business_suka`/`amamamamam` с равной вероятностью 1..2 для аудитории радиуса 15 и не воспроизводит его повторно пока стоит тег `mellstroy`. При снятии маски check выключает оба звука и удаляет тег; прямой unequip helper только выполняет `stopsound`. `chaoschestplate_10s` сохраняет ID/team владельца во временные scoreboard holders и спавнит loot `lbc:traps` у врагов в пределах 25 блоков; любой игрок `nomagic_active` в радиусе 25 подавляет вызов.

Win-последовательность читает победный текст/награду из `swrg:lang`, создаёт четыре `swrg.firework` marker и планирует `win_2` через 1 секунду. Каждый `win_2` разбрасывает markers вокруг 0,0 в пределах 192 блоков, создаёт у них ракеты LifeTime 60 с двумя взрывами, вызывает `swrg:game/end/firework_random` и планирует следующую итерацию через секунду. Цикл прекращается, когда `end_resets` выполняет `schedule clear lbc:items/win/win_2`; уже существующие markers и ракеты этот сброс не удаляет.





## Babylon Gate: пять исходов

Один из пяти selector-ов задаёт относительное место запуска относительно взгляда игрока; внутри каждого дополнительно выбирается случайный заряд 1..5. Заряды — три вида стрел с raid_omen amplifier 101/109/110 на 20 тиков, trident и фейерверк с четырьмя explosion shapes; `Owner` стрелка переносится, а краткоживущий вспомогательный marker убивается после записи Motion. Select срабатывает в main/offhand при `custom_data.gate_of_babylon:1` и достаточной мане; `babylon_gate_mana_tick` снимается после выпуска.



## Арканумы пятого тира

`items/arcanums/5t/select` вызывает Babylon Gate, если custom_data в любой руке и текущая mana не ниже `babylon_gate_mana_tick`; повтор возможен на следующих select-проходах. `end5t` и `end25t` снимают main/offhand usage и соответствующие active-теги — это локальный cleanup, не расход маны.



## Sun Arcanum

Основной Sun tick создаёт шаблон `lbc:the_sun`, даёт Fire Resistance 120 секунд, платит `sun_arcanum_mana_tick`, поражает вражеских мобов on_fire damage 10, только если предмет одновременно в двух руках, и уничтожает projectiles в distance 8..25, заменяя их lava. Main и offhand select начинаются с цены `sun_arcanum_mana_activate`; вызов offhand-ветки ошибочно ведёт в тот же `sun_tick`, а альтернативный `sun_tick_left` literal caller не имеет. Active-tag снимается при снятии предмета или нехватке mana.



## Арканумы: диспетчер и механики



Арканум-диспетчер проверяет предмет в главной руке и вызывает `void_curse`, `healing`, `neutral_ball`, `kiuaskivi`, `tannerkivi`, `barrier` или `eyelaser_pre` по соответствующему `custom_data`; offhand-диспетчер `select2` поддерживает те же арканумы, кроме eyelaser. Если активный предмет убран, следующий проход снимает его active-тег. Healing работает только при недостающем HP и достаточной mana, лечит instant_health I и расходует `healing_arcanum_mana_tick`. Neutral Ball в mainhand при каждом вызове `select` (players_tick) проверяет `mana >= neutral_ball_mana_activate`, затем добавляет 1 к `manaregen` и сразу отнимает 5000 mana; клик/переход предмета не требуется, поэтому при постоянном удержании конверсия повторяется на каждом player tick и порог не ограничивает итоговую ману ниже нуля. В offhand `select2` при удерживаемом Neutral Ball и `manaregen>=1` запускает обратную операцию на каждом таком проходе: −1 `manaregen`, +5000 mana. Offhand-ветка не является уборкой: при пустой руке условие не срабатывает и обратной конверсии нет.

Void Curse стоит `void_arcanum_mana_activate` при старте, затем `void_arcanum_mana_tick` за каждый вызов tick; при нехватке остаточной маны тег активности снимается. Старт создаёт slowraycast marker в 4 блока перед игроком, выдаёт black-hole achievement только из mainhand-ветки и запускает внешние лучевые функции. Их emitter-маркеры не копируют `Owner`, `lbcID2` или `team_number`; геометрия пробивается по кубам-объёмам, заданным `dx/dy/dz`, поэтому радиус/distance не расширяет такой куб. Ветка voidcurse объединяет отверстия от разных источников, наносит обычный generic damage без владельца и использует для `voidcursed5` лишь integrity 0.02 (это не гарантирует полного стирания блока). Выбор `select3s`/`select4s` увеличивает возраст только при успешном поиске facing-target, поэтому срок их существования не равен ровно 10 секундам. `barrier` расходует стоимость старта, пока активен каждые select-вызовы требует tick-цену и даёт Resistance V на 1 секунду; при одновременном удержании в обеих руках он чистит блоки вокруг игрока (с sneak — также снизу), поэтому источник имеет реальное воздействие на мир. `kiuaskivi` даёт Fire Resistance 30 секунд и каждый вызов очищает блоки из `kiuaskivi_to_air`, ставит огонь по `kiuaskivi_to_fire`; при удержании в обеих руках есть 5% шанс выполнить большой fire-fill. `tannerkivi` даёт Resistance I на 1 секунду и заменяет подходящие блоки на dirt в области ±3; при одновременном удержании в обеих руках зона расширяется до ±15. Их стоимость запуска/тика — соответствующие scoreboard-параметры; tick-цикл завершается снятием active-тега при нехватке маны.

Eyelaser запускается без расхода mana. Обычная и Mellstroy-ветки, их точные пределы и ошибка остановки варианта1 описаны в [отдельном разделе ниже](#eyelaser-обычная-и-mellstroy-ветки).

Sun Arcanum с `custom_data.sun_arcanum:1` обрабатывается main/offhand select; при активации списывается `sun_arcanum_mana_activate`, а каждый проход с достаточной маной — `sun_arcanum_mana_tick`, иначе соответствующий active-тег снимается. `sun_tick` даёт владельцу Fire Resistance на 120 секунд и размещает `lbc:the_sun` со смещением−13 по X/Y/Z (rotation/mirror none); размер шаблона из одного лишь offset команды здесь не выводится. Урон 10 типа `on_fire` по подходящим мобам требует предмета одновременно в обеих руках. Отдельная ветка тушит projectiles на distance 8..25, заменяя их лавой через `projectile_boom`. Offhand-select вызывает тот же `sun_tick`, а `sun_tick_left` не имеет найденного caller в изученных исходниках. Babylon Gate проверяется селектором каждый проход, если предмет с `custom_data.gate_of_babylon:1` в любой руке и маны не меньше `babylon_gate_mana_tick`; затем выбирает один из пяти выпусков (позиции `^2.5 ^2 ^`, `^-2.5 ^2 ^`, `^ ^3 ^`, `^4 ^ ^`, `^-4 ^ ^`) и отдельно случайно один из пяти зарядов: три tipped arrow с raid_omen (amplifier 101/109/110 на 20 тиков), trident или firework с Lifetime 100 и четырьмя типами взрыва. Селектор исхода задаёт локальную точку выпуска, создаёт временный marker для Motion, присваивает Owner игрока, снимает `babylon_charge` и показывает частицы; затем списывает стоимость тика. Зарядов выбор 5×5, равновероятно по `random value 1..5`; последующая обработка снарядов находится у projectile/raycast подсистемы.



## Eyelaser: обычная и Mellstroy-ветки

Mainhand arcanum с eyelaser и Troll Mask6 через trollmasksel запускают eyelaser_pre: raycaster-tag, Distance0 и луч из глаз, без списания mana в этих helpers. Основной eyelaser делает синхронные шаги0.5: при исходном0 обрабатывает201 точку (0..100 блоков). На каждом шаге наносит0.05 generic одной mob-цели limit1 без sort, исключая raycaster/spectator. Дополнительные две куб-проверки с offset−0.99 ограничивают попадание пересечением hitbox. Союзники здесь не исключены. Не-air вызывает eyelaser_kill, который **только ставит Distance201**; это прекращает дальнейшую рекурсию основного варианта. При окончании pre снимает raycaster.

Отдельный eyelaser1 вызывает **Mellstroy armor tick**, а не Troll Mask6. Он исключает same_team/same_id по временным holders, проверяет stopper в20 блоках и при свободной траектории обрабатывает685 точек (до342 блоков). Важный край: общий kill-helper на блоке тоже ставит201, а предел этого варианта684. Поэтому блок **не останавливает** этот луч; при последовательных solid-блоках score каждый раз возвращается201, и дальность может превысить342, пока не прервётся по stopper/лимиту выполнения команд. Это поведение исходника, не обещание успешного бесконечного луча. Урон0.05 за подходящий шаг, отдельного расхода mana нет.

Источники: [pre](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/eyelaser_pre.mcfunction), [base](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/eyelaser.mcfunction), [вариант1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/eyelaser1.mcfunction), [kill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/eyelaser_kill.mcfunction), [Mellstroy caller](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/mellstroy_mask/tick.mcfunction), [Troll Mask caller](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/trollmasksel.mcfunction).

## Боевые, статусные и перемещенческие hooks

`damaged` расходует advancement `cocksuckers` и исполняет реакцию брони/артефакта: dynamite chestplate создаёт TNT с fuse 80 тиков (4 секунды), Chaos Chestplate вызывает внешний effectselect если нет `stopper_magic` и `nomagic_active`, Berserk полный комплект при HP≤20 с шансом 70% запускает set bonus, Shulkshell при шансе 20% телепортирует, Technomk2 имеет щитовую ветку, Glass/Nanobots вызывают броневые callbacks. Техно set bonus запускает внешний raycast; старые mystical-броня выдаёт Resistance/Levitation по типу. `damaged_by_player` при ненаблюдаемом игроке и `time_stopped<1` вызывает внешний artifact shield и затем отзывает advancement. `damaged_scheduled` сбрасывает `damage_taken` всем игрокам; конкретный scheduler/period задаётся вызывающим runtime.

`damage_up` умножает entity damage на 1,25; `damage_up_1` добавляет 0,5 к damage. `crossbow_charging` повышает `crossbow_charge`, при значении ровно 1 и инвентарном `custom_data.infinite_arrow:1` выдаёт loot `lbc:imba/infinite_arrow`, затем отзывает charging advancement. `effects_changed` зовёт SWRG skill handler и у владельца clown_mask снимает blindness и darkness. `accelerate` умножает Motion каждой оси на коэффициент 1,3 и вызывает общий `reduce_motion_to_10x`; точная конечная скорость ограничивается этим внешним helper.

Падение считает `falltimer`: `on_falling` при достижении 20 и jumpboost chestplate вызывает traveller effect; sin_lust при 10+ даёт Slow Falling по 1 секунде, overheaven при 16+ даёт Resistance V (1 секунда), если игрок не в воде и нет `stopper_magic`; затем falltimer растёт на 1. `falling` и `on_ground` сбрасывают его при `OnGround:1b`/приземлении. `on_falling_second_check` при наличии SWRG trinket calls `game/trinkets/on_fall`. `on_sprint` увеличивает sprint_timer, срабатывает на 5% для chaos leggings ability при приземлении, зовёт moisei boots и другие sword/mace callbacks; `on_not_sprint` восстанавливает soulhunter mace заряд, пока игрок на земле и score `mace_1<=59`.

`snowballs` для SWRG perk fast_charge применяет accelerate к ближайшему снежку в 10 блоках, для gravitas ставит `NoGravity:1b`; special/gadget snowballs передаются projectile special/egg dispatchers. Функция временно ставит владельцу `snowballerr`, дописывает ближайшим snowball без ID `lbcID2/team_number`, снимает метку и сбрасывает свой snowball score. `spells` ставит `caster`, при `overload_full_active` добавляет 10 000 000 mana и вызывает соответствующие workingitems функции по `custom_data` raycastspell/magicwand/summonerwand; большинство конечных заклинаний принадлежат внешнему workingitems пакету.

Kill/hit hooks отзывают собственные advancement trigger и передают SWRG события skills/kill-effects. `player_hit_player_arrow` при randrule=2 и `player_hit_player_melee` при randrule=1 вызывают `absolute_cinema/rule_fail`; универсальный PvP bow/sword hook зовёт skill handler. Убийство моба запускает skill и выбранный kill effect по `swrg.kill_effect`; убийство игрока запускает player_killed_player skill. При november=1/2 и удержании `custom_data.lightsaber:1` события убийства Sith/Jedi увеличивают соответственно глобальный `jedi`/`sith` счётчик на 1.

`on_death` в SurvivalVersion=1 вызывает literal `lbc:items/on_death_survival`, которого нет среди файлов этого datapack; далее функция делает массовое удаление `affected_by_wrath` (исключая spectator, sin_wrath, sin_greed_active и strict map object), затем сбрасывает death score. Наличие функции в другом datapack/namespace не проверялось. Это известная граница вызова, не вывод о конечном survival поведении.

## Индекс обработчика мечей и trinket-триггеров

`items/swords` вызывается боевым событием и по `SelectedItem.components.minecraft:custom_data` передаёт удар способностям соответствующего меча; конечные результаты описаны в [оружейном разборе](lbc_items_weapons.md). Локально подтверждены feeding→`workingitems/feedingsword`, cool/cool_old, fragmentsword/old, ender, heal, seaguardian, gravitationsword, icesword, evolution, cyberkatana, techrunner, shit_katana, terrasword, mortis, vexsword, true_terra, antimagic, anal_whip, пять fish-вариантов, banhammer, stand_arrow, WTF, Fevgalea, Chaos, Triple Golden Sword, Kara и Pumpkin King; внешние обращения обозначены в таблице/содержимом dispatcher-а. В большинстве новых magic-ветвей требуется отсутствие `nomagic_active` игрока и `stopper_magic` в пределах 20; старые варианты явно обходят эти фильтры. При `time_stopped>=1` для цели с HurtTime создаётся `damage_splash` marker. `swords_or_bow` вызывает SWRG `skills_hit_or_bow` и отзывает общий advancement `gowno_any_damage`.

## Связанные предметные эффекты брони

`chaoschestplate_10s` сохраняет `lbcID2/team_number` во временные holders; если нет игрока `nomagic_active` в 25 блоках, выполняет loot spawn `lbc:traps` у игроков-врагов в диапазоне 0.01..25, исключая владельца и союзников. Это прямой локальный вызов, а повтор/длительность задаётся внешним caller с именем `10s`; сам файл не содержит schedule.



## Инвентарь и атрибуты

`changed_inventory/sel` обновляет набор presence-тегов по `custom_data` в weapon/container/armor, среди них gaara, save, magnet, death_compass, techno/technomk2, gownoleggings, chaoschestplate, paladin, nanobots, overload, reactive, stand clocks, phantom, razorpine и yellow lightning. Chaos chestplate и paladin дают `*_full/set_bonus_active` только при четырёх предметах; ниже четырёх тег снимается. Четыре fix_attribute функции применяют предметный modifier к обычной vanilla броне каждого слота и не меняют custom armor. Mellstroy equip/unequip checks используют тег, чтобы случайный звук (1 из 2) запускався только при первом надевании и гарантированно останавливался при снятии.



## Диспетчер spell ID

`select` пытается считать `spellid` у ближайшего snowball без ограничения дальности и без фильтра libspell в selector; проверка `libspell:1` стоит только в NBT-пути. Снежок для удаления выбирается отдельно в10 блоках с фильтром libspell. Поэтому читаемый и удаляемый снежки могут различаться. Если ближайший снежок не содержит нужные данные, этот путь не получает корректный ID из него. Далее функция ставит caster/raycaster, сбрасывает Distance и удаляет отдельно выбранный libspell-snowball. При ID 128 пять раз повторяется random 1..242, пока значение остаётся 128; если после пятой перегенерации всё ещё 128, явной ветки ID 128 нет. `select_wand` сначала читает `SelectedItem.custom_data.spell$(currspell)` из предмета в главной руке, записывая scoreboard `spell`, и маршрутизирует выбранное заклинание к одному локальному wand helper либо `workingitems:*`/`lbcr:*`. Ниже приведены все точные маршруты из `select_wand.mcfunction`; таблица фиксирует диспетчер, а конечный результат дочерних пакетов прослеживается в их назначенном исследовании.

| ID | Функция | Дополнительный контекст выполнения |
|---|---|---|
| 1 | `lbc:workingitems/evokercast` | исходный |
| 2..4 | `lbc:workingitems/evokercast_upgraded_spelllib` | исходный |
| 5 | `lbc:workingitems/basscast` | исходный |
| 6..9 | `lbc:workingitems/basscast_upgraded_spelllib` | исходный |
| 10 | `lbc:workingitems/corrupter` | исходный |
| 11..14 | `lbc:workingitems/corrupter_upgraded_spelllib` | исходный |
| 15 | `lbc:workingitems/endcast` | исходный |
| 16 | `lbc:workingitems/endcast_lib` | исходный |
| 17 | `lbc:workingitems/lifestealer` | исходный |
| 18 | `lbc:workingitems/lifestealer_upgraded_mode_1` | исходный |
| 19 | `lbc:workingitems/lifestealer_upgraded_mode_2` | `anchored eyes positioned ^ ^ ^` |
| 20 | `lbc:workingitems/foxuscast` | исходный |
| 21..24 | `lbc:workingitems/foxuscast_upgraded_lib` | исходный |
| 25 | `lbc:workingitems/bloodmagic` | исходный |
| 26 | `lbc:workingitems/bloodmagic_upgraded` | исходный |
| 27 | `lbc:workingitems/badwiki` | исходный |
| 28..111 | `lbc:workingitems/badwiki_lib` | исходный |
| 112 | `lbcr:raycast_loop2` | `anchored eyes positioned ^ ^ ^` |
| 113..115 | `lbcr:raycast_end2_upgraded_lib_loop` | `anchored eyes positioned ^ ^ ^` |
| 116 | `lbcr:raycast_loop3` | `anchored eyes positioned ^ ^ ^` |
| 117 | `lbcr:raycast_loop3_upgraded_mode_default` | `anchored eyes positioned ^ ^ ^` |
| 118 | `lbcr:raycast_loop3_upgraded_mode_uppest` | `anchored eyes positioned ^ ^ ^` |
| 119 | `lbcr:raycast_loop3_upgraded_mode_clip` | `anchored eyes positioned ^ ^ ^` |
| 120 | `lbcr:raycast_loop5` | `anchored eyes positioned ^ ^ ^` |
| 121..122 | `lbcr:raycast_loop5_upg_lib` | `anchored eyes positioned ^ ^ ^` |
| 123 | `lbcr:raycast_preloop7_2` | `anchored eyes positioned ^ ^ ^` |
| 124 | `lbcr:raycast_preloop7_upgraded_lib` | `anchored eyes positioned ^ ^ ^` |
| 125 | `lbcr:raycast_loop4` | `anchored eyes positioned ^ ^ ^` |
| 126 | `lbcr:raycast_loop4cd_upgraded2_blaze_head` | исходный |
| 127 | `lbc:workingitems/random_spell_lib` | исходный |
| 129 | `lbcr:raycast_loop9cd_upgraded_2_loop_mode_default` | `anchored eyes positioned ^ ^ ^` |
| 130 | `lbcr:raycast_loop9cd_upgraded_2_loop_mode_sniper` | `anchored eyes positioned ^ ^ ^` |
| 131 | `lbcr:raycast_loop9cd_upgraded_2_loop_mode_laser` | `anchored eyes positioned ^ ^ ^` |
| 132 | `lbcr:raycast_loop9cd_upgraded_2_loop_mode_tank` | `anchored eyes positioned ^ ^ ^` |
| 133 | `lbcr:raycast_loop9cd_upgraded_2_loop_mode_healer` | `anchored eyes positioned ^ ^ ^` |
| 134 | `lbcr:raycast_loop8` | `anchored eyes positioned ^ ^ ^` |
| 135 | `lbcr:raycast_loop8_upgraded` | `anchored eyes positioned ^ ^ ^` |
| 136 | `lbc:workingitems/psychowand` | исходный |
| 137..140 | `lbc:workingitems/psychowand_upgraded_lib` | исходный |
| 141 | `lbcr:raycast_preloop10` | `anchored eyes positioned ^ ^ ^` |
| 142 | `lbcr:raycast_preloop10cd_upgraded_mode_chain` | `anchored eyes positioned ^ ^ ^` |
| 143 | `lbcr:raycast_preloop10cd_upgraded_mode_instant` | `anchored eyes positioned ^ ^ ^` |
| 144 | `lbcr:raycast_preloop11_upgraded` | `anchored eyes positioned ^ ^ ^` |
| 145 | `lbcr:raycast_preloop11_upgraded_lib` | `anchored eyes positioned ^ ^ ^` |
| 146 | `lbc:workingitems/omegaflag` | исходный |
| 147 | `lbc:workingitems/omegaflag_upgraded` | исходный |
| 148 | `lbc:workingitems/neutralmage/select` | исходный |
| 149 | `lbc:workingitems/neutralmage/1` | исходный |
| 150 | `lbc:workingitems/neutralmage/2` | исходный |
| 151 | `lbc:workingitems/neutralmage/3` | исходный |
| 152 | `lbc:workingitems/neutralmage/4` | исходный |
| 153 | `lbc:workingitems/neutralmage/5` | исходный |
| 154 | `lbcr:raycast_lightning` | `anchored eyes positioned ^ ^ ^` |
| 155 | `lbcr:raycast_lightning_mode_chain` | `anchored eyes positioned ^ ^ ^` |
| 156 | `lbcr:raycast_lightning_mode_reforge` | `anchored eyes positioned ^ ^ ^` |
| 157 | `lbc:workingitems/exoperso` | исходный |
| 158 | `lbc:workingitems/exoperso_upgraded` | исходный |
| 159 | `lbc:items/wands/danmaku_staff/click_2` | исходный |
| 160 | `lbc:workingitems/sauvojen_staffs/ruby_preloop` | `anchored eyes positioned ^ ^ ^` |
| 161 | `lbc:workingitems/sauvojen_staffs/ruby_ray` | `anchored eyes positioned ^ ^ ^` |
| 162 | `lbc:workingitems/sauvojen_staffs/ruby_hole` | `anchored eyes positioned ^ ^ ^` |
| 163 | `lbc:workingitems/sauvojen_staffs/terra_preloop` | `anchored eyes positioned ^ ^ ^` |
| 164 | `lbc:workingitems/sauvojen_staffs/terra_preloop_upgraded` | `anchored eyes positioned ^ ^ ^` |
| 165 | `lbc:workingitems/sauvojen_staffs/electro_preloop` | `anchored eyes positioned ^ ^ ^` |
| 166 | `lbc:workingitems/sauvojen_staffs/electro_ray` | `anchored eyes positioned ^ ^ ^` |
| 167 | `lbc:workingitems/sauvojen_staffs/air_preloop` | `anchored eyes positioned ^ ^ ^` |
| 168 | `lbc:workingitems/sauvojen_staffs/air_preloop_burst` | `anchored eyes positioned ^ ^ ^` |
| 169 | `lbc:workingitems/sauvojen_staffs/air_preloop_fast` | `anchored eyes positioned ^ ^ ^` |
| 170 | `lbc:workingitems/sauvojen_staffs/air_preloop_homing` | `anchored eyes positioned ^ ^ ^` |
| 171 | `lbc:workingitems/sauvojen_staffs/acid_preloop` | `anchored eyes positioned ^ ^ ^` |
| 172 | `lbc:workingitems/sauvojen_staffs/acid_preloop_upgraded_lib` | `anchored eyes positioned ^ ^ ^` |
| 173 | `lbc:workingitems/sauvojen_staffs/teleport_preloop` | `anchored eyes positioned ^ ^ ^` |
| 174 | `lbc:workingitems/sauvojen_staffs/teleport_inverted` | `anchored eyes positioned ^ ^ ^` |
| 175 | `lbc:workingitems/sauvojen_staffs/teleport_cloud` | исходный |
| 176 | `lbc:workingitems/sauvojen_staffs/stone_preloop` | `anchored eyes positioned ^ ^ ^` |
| 177 | `lbc:workingitems/sauvojen_staffs/stone_preloop_upgraded_default` | `anchored eyes positioned ^ ^ ^` |
| 178 | `lbc:workingitems/sauvojen_staffs/stone_preloop_upgraded_spike` | `anchored eyes positioned ^ ^ ^` |
| 179 | `lbc:workingitems/sauvojen_staffs/stone_preloop_upgraded_bridge` | `positioned ~ ~-1 ~` |
| 180 | `lbc:workingitems/sauvojen_staffs/stone_preloop_upgraded_shield` | исходный |
| 181 | `lbc:workingitems/sauvojen_staffs/fire_preloop` | `anchored eyes positioned ^ ^ ^` |
| 182 | `lbc:workingitems/sauvojen_staffs/fire_preloop_upgraded_big` | `anchored eyes positioned ^ ^ ^` |
| 183 | `lbc:workingitems/sauvojen_staffs/fire_preloop_upgraded_concentrated` | `anchored eyes positioned ^ ^ ^` |
| 184 | `lbc:workingitems/sauvojen_staffs/blind_preloop` | `anchored eyes positioned ^ ^ ^` |
| 185 | `lbc:workingitems/sauvojen_staffs/blind_preloop_upgraded_gatlingator` | `anchored eyes positioned ^ ^ ^` |
| 186 | `lbc:workingitems/sauvojen_staffs/blind_preloop_upgraded_phantom` | `anchored eyes positioned ^ ^ ^` |
| 187 | `lbc:workingitems/sauvojen_staffs/blind_preloop_upgraded_rain` | `anchored eyes positioned ^ ^ ^` |
| 188 | `lbc:workingitems/awaken_tuntija_wands/acidacid/click2` | `anchored eyes positioned ^ ^ ^` |
| 189 | `lbc:workingitems/awaken_tuntija_wands/acidacid/click_upgraded_mode_default_lib` | `anchored eyes positioned ^ ^ ^` |
| 190 | `lbc:workingitems/awaken_tuntija_wands/acidacid/click_upgraded_mode_caust_lib` | `anchored eyes positioned ^ ^ ^` |
| 191 | `lbc:workingitems/awaken_tuntija_wands/acidacid/click_upgraded_mode_caust_lib_1` | `anchored eyes positioned ^ ^ ^` |
| 192 | `lbc:workingitems/awaken_tuntija_wands/acidacid/click_upgraded_mode_gatling_lib` | `anchored eyes positioned ^ ^ ^` |
| 193 | `lbc:workingitems/awaken_tuntija_wands/acidacid/click_upgraded_mode_gatling_lib_1` | `anchored eyes positioned ^ ^ ^` |
| 194 | `lbc:workingitems/awaken_tuntija_wands/acidacid/click_upgraded_mode_rifle` | `anchored eyes positioned ^ ^ ^` |
| 195 | `lbc:workingitems/awaken_tuntija_wands/acidcorrupt/click_lib` | `anchored eyes positioned ^ ^ ^` |
| 196 | `lbc:workingitems/awaken_tuntija_wands/acidcorrupt/mode_reverse` | исходный |
| 197 | `lbc:workingitems/awaken_tuntija_wands/acidcorrupt/mode_titul` | `anchored eyes positioned ^ ^ ^` |
| 198 | `lbc:workingitems/awaken_tuntija_wands/acidcorrupt/mode_chuma` | `anchored eyes positioned ^ ^ ^` |
| 199 | `lbc:workingitems/awaken_tuntija_wands/exasperato/click2` | `anchored eyes positioned ^ ^ ^` |
| 200 | `lbc:workingitems/awaken_tuntija_wands/exasperato/click2_upgraded_mode_default` | `anchored eyes positioned ^ ^ ^` |
| 201 | `lbc:workingitems/awaken_tuntija_wands/exasperato/click2_upgraded_mode_chaos_area` | `anchored eyes positioned ^ ^ ^` |
| 202 | `lbc:workingitems/awaken_tuntija_wands/exasperato/click2_upgraded_mode_electro_ray` | `anchored eyes positioned ^ ^ ^` |
| 203 | `lbc:workingitems/awaken_tuntija_wands/exasperato/click2_upgraded_mode_nuke` | `anchored eyes positioned ^ ^ ^` |
| 204 | `lbc:workingitems/awaken_tuntija_wands/exahoming_click_lib` | `anchored eyes positioned ^ ^ ^` |
| 205 | `lbc:workingitems/awaken_tuntija_wands/exahoming_click_upgraded_mode_curse` | `anchored eyes positioned ^ ^ ^` |
| 206 | `lbc:workingitems/awaken_tuntija_wands/exahoming_click_upgraded_mode_destiny` | `anchored eyes positioned ^ ^ ^` |
| 207 | `lbc:workingitems/awaken_tuntija_wands/exahoming_click_upgraded_mode_sisyfus` | `anchored eyes positioned ^ ^ ^` |
| 208 | `lbc:workingitems/awaken_tuntija_wands/psychoblind/click_upgraded_mode_default` | `anchored eyes positioned ^ ^ ^` |
| 209 | `lbc:workingitems/awaken_tuntija_wands/psychoblind/click_upgraded_mode_invisible_charge` | `anchored eyes positioned ^ ^ ^` |
| 210 | `lbc:workingitems/awaken_tuntija_wands/psychoblind/click_upgraded_mode_mindblowing_ray` | `anchored eyes positioned ^ ^ ^` |
| 211 | `lbc:workingitems/awaken_tuntija_wands/psychoblind/click_upgraded_mode_spray` | `anchored eyes positioned ^ ^ ^` |
| 212 | `lbc:workingitems/awaken_tuntija_wands/boltbolt/click_lib` | `anchored eyes positioned ^ ^ ^` |
| 213 | `lbc:workingitems/awaken_tuntija_wands/boltbolt/click_upgraded_mode_snow_meteor` | `anchored eyes positioned ^ ^ ^` |
| 214 | `lbc:workingitems/awaken_tuntija_wands/boltbolt/click_upgraded_mode_watering` | `anchored eyes positioned ^ ^ ^` |
| 215 | `lbc:workingitems/awaken_tuntija_wands/boltbolt/click_upgraded_mode_wind_racer` | `anchored eyes positioned ^ ^ ^` |
| 216 | `lbc:workingitems/awaken_tuntija_wands/erecto/click_upgraded_2_mode_default` | `anchored eyes positioned ^ ^ ^` |
| 217 | `lbc:workingitems/awaken_tuntija_wands/erecto/click_upgraded_2_mode_ray` | `anchored eyes positioned ^ ^ ^` |
| 218 | `lbc:workingitems/awaken_tuntija_wands/erecto/click_upgraded_2_mode_sniper_lib` | `anchored eyes positioned ^ ^ ^` |
| 219 | `lbc:workingitems/awaken_tuntija_wands/erecto/click_upgraded_2_mode_sphere` | `anchored eyes positioned ^ ^ ^` |
| 220 | `lbc:workingitems/awaken_tuntija_wands/fireblaze/click2` | `anchored eyes positioned ^ ^ ^` |
| 221 | `lbc:workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_kiuas` | `anchored eyes positioned ^ ^ ^` |
| 222 | `lbc:workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_mega` | `anchored eyes positioned ^ ^ ^` |
| 223 | `lbc:workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_pillars` | `anchored eyes positioned ^ ^ ^` |
| 224 | `lbc:workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_world` | `anchored eyes positioned ^ ^ ^` |
| 225 | `lbc:workingitems/awaken_tuntija_wands/consuming_wand_upgraded_mode_default` | `anchored eyes positioned ^ ^ ^` |
| 226 | `lbc:workingitems/awaken_tuntija_wands/consuming_wand_upgraded_mode_avarice` | `anchored eyes positioned ^ ^ ^` |
| 227 | `lbc:workingitems/awaken_tuntija_wands/consuming_wand_upgraded_mode_silence` | `anchored eyes positioned ^ ^ ^` |
| 228 | `lbc:workingitems/awaken_tuntija_wands/consuming_wand_upgraded_mode_the_end_pre` | `anchored eyes positioned ^ ^ ^8` |
| 229 | `lbc:workingitems/awaken_tuntija_wands/avarice/click2_upgraded_default` | `anchored eyes positioned ^ ^ ^` |
| 230 | `lbc:workingitems/awaken_tuntija_wands/avarice/click2_upgraded_black_hole` | `anchored eyes positioned ^ ^ ^` |
| 231 | `lbc:workingitems/awaken_tuntija_wands/avarice/click2_upgraded_divide` | `anchored eyes positioned ^ ^ ^` |
| 232 | `lbc:workingitems/awaken_tuntija_wands/avarice/click2_upgraded_expand` | `anchored eyes positioned ^ ^ ^` |
| 233 | `lbc:workingitems/awaken_tuntija_wands/stonekivi/loop` | `anchored eyes positioned ^ ^ ^` |
| 234 | `lbc:workingitems/awaken_tuntija_wands/stonekivi/loop_earth` | `anchored eyes positioned ^ ^ ^` |
| 235 | `lbc:workingitems/awaken_tuntija_wands/stonekivi/loop_sun` | `anchored eyes positioned ^ ^ ^` |
| 236 | `lbc:workingitems/awaken_tuntija_wands/stonekivi/comet` | `anchored eyes positioned ^ ^ ^` |
| 237 | `lbc:workingitems/awaken_tuntija_wands/bedrock/click_lib` | исходный |
| 238 | `lbc:workingitems/awaken_tuntija_wands/bedrock/click_upgraded_mode_place_cube` | `anchored eyes positioned ^ ^ ^` |
| 239 | `lbc:workingitems/awaken_tuntija_wands/bedrock/click_upgraded_mode_remove_cube` | `anchored eyes positioned ^ ^ ^` |
| 240 | `lbc:workingitems/awaken_tuntija_wands/bedrock/click_upgraded_mode_regen_lib` | исходный |
| 241 | `lbc:workingitems/awaken_tuntija_wands/bedrock/click_upgraded_mode_remove` | `anchored eyes positioned ^ ^ ^` |
| 242 | `lbc:items/wands/sauvojen_wand/cast_2` | `anchored eyes positioned ^ ^ ^` |

Сверены все141 маршрута `select_wand` и140 маршрутов `select`: отличаются только ID189, который присутствует у wand и отсутствует у snowball. В прежней таблице были пропущены ID179 (bridge) и228 (The End со смещением8); здесь они восстановлены. Обе функции до ID119 обнуляют `check`; после dispatch снимают caster/raycaster. [Полные команды и22 шанс-модели](inventory/arcana_routes_chances_audit.json).

## Дроны и Huynya: проверенные локальные действия

`drone_pult` кликом запускает search луч шагом `^ ^ ^.33`: на каждом шаге ищется подходящая цель в `dx=dy=dz=0` и `distance<=10` (это маленький куб, не сфера). Максимум score `Distance<=900` даёт около 300 блоков продвижения; найденная цель помечается, игрок тратит один пульт, а homing item_display создаётся примерно в 45 блоках позади точки и на 5 выше, копирует ID выбранной цели и получает `Distance=10000`, прекращая луч. В тиках `raycastselector_player` у дисплея идёт homing к цели с таким же ID; он делает три последовательных продвижения по 1 блоку за вызов, и при соприкосновении с hitbox в `dx/dy/dz=0`, distance≤12 взрывается. `explode` призывает Creeper с `ExplosionRadius:8`, уже подожжённого, Fuse 0, Invulnerable и Silent, затем удаляет дисплей; владелец, назначенный при summon, по исходнику равен ID цели, а не обязательно стрелку.

`boba_eblan` вызывается из обычного/offhand spell dispatcher. В bedwars gamemode 3..4 выбирает противника не spectator/не союзника, телепортирует его относительно себя на `^1 ^ ^`, затем вызывает внешний `other/ponos` и выдаёт loot `lbc:govno_lot_2`. В обычном режиме `click` ставит `rotatemap` в macro storage и вызывает `click_m`; тот исполняет `$$(command)`, но строка `rotatemap` не содержит namespace/команды, поэтому её точное выполнение по одному лишь source-коду неясно. Если игрок в пределах X/Z ±16000 — macro запускается от позиции (0,160,0) и функция возвращается; иначе запускается из текущей позиции. Ручные `at*` helpers загружают четыре квадрата chunks в координатах 512512..512832, через секундные schedule выполняют clone куба координат −159..160, Y 0..319, в позицию 512512 0 512512, повторно заходят через marker `rotate_center` и снимают forceload. Между ними `at_2at_22` зовёт `lbc:items/misc/huynya/clone`, которой в текущем datapack нет; последующие forceload remove и kill marker всё ещё записаны, но сама ожидаемая clone-команда не подтверждена.

## Oraora

`oraora` click даёт Speed V, Jump Boost V и Health Boost X на 60 секунд, проигрывает звук и ставит `coldownoraora=40`. Cooldown уменьшается в `players_1s` раз в секунду; отдельный `players_2t` вызывает `items/oraora/tick` раз в 2 игровых тика, пока score в диапазоне 30..40. Поэтому окно огня длится примерно 10–11 секунд и даёт около 100–110 проходов, а не около 11 выстрелов. Каждый проход даёт Resistance V на 1 секунду и равновероятно выбирает одну из 10 локальных позиций выпуска item_display (перед/сбоку/сверху, offsets заданы в `tick`), после чего снимает временный `owner` tag. Полный cooldown равен 40 секундам; вне диапазона 30..40 новые снаряды не создаются.

Снаряд летит три последовательных блока за tick-call, каждый раз наводясь на ближайшую не-spectator цель; в точке удара селектор ограничен `dx=dy=dz=0` плюс `distance<=8`, значит прямоугольный куб 1×1×1 дополнительно отфильтрован по расстоянию. Если рядом цель, projectile удаляется о блоке не-air; она получает 2.5 arrow damage (если owner найден) или generic, `strong` усиливает до 5, `overheavened` — до 12.5; цель выбирается ближайшая, не помеченная как союзник/владелец и не strict map object. `projectile_create` различает три состояния владельца: обычное, strong при stand clocks и overheavened при соответствующем score; ID/team переносятся в дисплей. Применение клика приходит из spell dispatcher и пропускается при `coldownoraora>0`.

## Эффекты trinket

Trinket-dispatch выполняет hook на попадания/выстрелы; шанс хранится в `tmp` как заряд, а helper `func_with_chance_universal` применяет макросный random predicate и вызывает заданную функцию. При `tmp=1` действуют отдельные шансы; выше работают линейные формулы: acid — `8% + 7%×tmp`, bass/ghast — `10% + 5%×tmp`, bedrcock — `10% + 10%×tmp`, blaze — `17.5% + 7.5%×tmp`, cocktail — `5% + 5%×tmp`, Fourier — `10% + 5%×tmp`, TNT — `20% + 5%×tmp`, penis — `7.5% + 2.5%×tmp`. Так, acid при tmp1 использует chance15, при14+ гарантирован. На2..13 рассчитывается22..99%, но helper записывает storage `tmp`, а macro читает `tmpd`: эта ветка не использует рассчитанный шанс. Без внешнего `tmpd` макрос не выполняется; при наличии такого значения результат зависит от него; TNT при 1 использует chance25, при 2..15 — 30..95%, при 16+ гарантирован. Аналогично blaze: chance15 при 1, формула на 2..10 (32.5..92.5%), гарантия от 11; ghast: chance25 при 1, формула 20..85% на 2..15, гарантия от 16; penis: chance10 при 1, формула на 2..36, гарантия от 37.

Успешный acid-вызов создаёт впереди владельца marker `raycastacid` с Lifetime 300 и тегами `player,slowraycast,shield_blockable2,magic`; внешний `lbcr:acid_get_id` добавляет owner-информацию, а финальный hit выполняет внешний кислотный raycast. Bass успех вызывает внешний `workingitems/basscast`. Bedrcock помечает стрелы `bedrock_arrow` в радиусе 1 от точки взгляда. Blaze создаёт small_fireball и делегирует его направление `other/raycastfireballchick_2`; ghast создаёт направленную firework-ракету с LifeTime 100, Owner и `lbcID2/team_number`, использует внешний slow raycast. TNT создаёт forward TNT с `fuse:1000s` (Short: 1000 тиков = 50 секунд), помечает её `can_deleted,meowmurnya,dynamite,groundbam` и копирует владельца/команду; explosion/удар затем зависит от внешнего raycast helper. Отдельный `rocket_spell_explode` создаёт семь TNT с `fuse:1` (NBT число без суффикса, 1 тик) в точке ±2 по осям и убивает rocket marker.

Penis Gun при зарядке проверяет tmp и вызывает `charge_penis`: случайный 1..6 выбирает звук, а каждый результат создаёт соответствующий item с уникальной моделью/данными; эффект pickup/detonation уходит в другой обработчик. Polymorph вызывает `polymorph_activate` по формуле `0.1% + 0.1%×tmp`; случайно выбирает одну сущность не-player без тега magicalsheep в24 блоках. Фильтра команды/владельца или требования типа mobs здесь нет: подходят также item/display/marker. Передаёт её во внешний `lbcr:raycast_end8_mob`; только предметная сущность со всеми тремя custom_data `{transformable:1,potion:1,succubus:1}` идёт в `..._sheep_blackhole`. Psycho Soul задаёт шанс `3% + 1%×tmp`, и активация ставит подходящим мобам NoAI, hurt time и направленный marker с Lifetime 100; частицы сопровождают цель. Sunprism имеет шанс `0.04% + 0.01%×tmp` (при `tmp=1` это 0.05%), но активируется лишь при видимом небе и в пределах 24 блоков от подходящего моба ниже владельца; активация сохраняет ID/team во временные score holders, поднимает точку выпуска на3 блока, создаёт prismcheck marker и запускает внешний цветной raycast. При подходящей цели выполняются две отдельные попытки: луч к ближайшей цели ниже игрока и луч вверх. Локальные helpers не копируют ID/team в marker; полёт/урон выполняет `lbcr:raycast_preloop7_2_upgraded`.

Другие локальные trinket-эффекты: cocktail имеет шанс `5% + 5%×tmp`, помечает стрелы в 1 блоке как `cocktail_arrow,killedarrow` и копирует Owner/team-score; Fourier имеет шанс `10% + 5%×tmp`, создаёт homing_evoker marker с `slowraycast,player,magic`, записывает ID/team, а внешние `homing_evoker/tick` и `find_pos` определяют преследование. Gneiss Stone имеет шанс `8% + 2%×tmp`; успех даёт Resistance V на 2 секунды, каменную частицу и звук. Hedgehog Gauntlet имеет ту же вероятность, выпускает block_display pointed-dripstone с owner/team; при столкновении в 0,4 блока ближайшая вражеская цель получает 4 arrow damage, затем шип удаляется. Ice Gem (`9% + 1%×tmp`) вызывает внешний `workingitems/icesword/pre` для каждого врага в 0.001..12 блоках; Snow Pearl (`8% + 2%×tmp`) делает то же для врагов с HurtTime 10s в 0.001..7 блоках. Intercal (`10% + 5%×tmp`) накладывает Wither I на 10 секунд всем врагам в 10 блоках; при SWRG skill 15 дополнительный Wither IV на 3 секунды. Volos (`4% + 1%×tmp`) помечает ближайшие стрелы `volos_arrow` и назначает ID/team, а mana gem непосредственно добавляет `45 + 15×tmp` к scoreboard `mana`.

Twilight Sparkle (`3% + 2%×tmp`) создаёт видимую лошадь с седлом и алмазной конской бронёй, Slow Falling с бесконечным duration и HP 8, сразу сажает на неё игрока и выставляет `sprint_timer=100`. Tick лошади каждый вызов уменьшает таймер, запускает внешний `raycast_vpered_slow1` и с шансом 10% проигрывает взмах крыльев; в локальной функции нет команды удаления по таймеру, так что окончание определяется внешним raycast/другими dispatchers. `bedrcock_arrow` только добавляет тег стрелам в 1 блоке от взгляда; `modify_volos` также только ставит тег и копирует ID/team.

Пределы: trigger frequency и генерацию tmp для части этих trinket-хуков задают общие `items/*` боевые dispatchers; их конкретный вызов описан отдельным разделом. Указанные `lbcr:*`, `workingitems:*` и `other:*` конечные реализации принадлежат другим пакетам и здесь не засчитываются как проверенные по содержимому.




## Оставшиеся trinket-механики и диспетчеры

`cast_wand` зовётся при cast Danmaku/Sauvojen и поддерживаемых awakening spells; если предмет `trinket:"blood_anal_boom"` есть у игрока, вызывает TNT-шанс, а если есть `mana_gem` — прибавляет `45+15×tmp` mana. `tick` только проверяет poly_gem и sunprism каждый player dispatch tick, где шанс/эффекты описаны выше. `spawn_blaze` имеет вероятность `40%+10%×tmp`; он вызывается SWRG `game/trinkets/on_summon`. Успех призывает blaze с HP 20 и follow_range 200; `summon_blaze` добавляет его в команду игрока 1..32 и помечает `summactivated`, но в этой функции нет таймера жизни или явной привязки Owner.

RTP trinket (`banangay`) имеет шанс `2.5%+2.5%×tmp`. `rtp_gas` даёт звук телепорта вокруг в 16 блоках и portal particles, вызывает `rtp_gas2`, поднимает владельца на 0,5 блока, снова рисует частицы и играет звуки. `rtp_gas2` переносит игрока `spreadplayers` на поверхность в квадрат X/Z±6 вокруг(0,0), в Nether добавляет `under 125`; это не случайное перемещение вокруг текущей позиции. `rtp_gas1` и `rtp_gas21` оставлены runnable helper-ами с квадратом X/Z±50 вокруг текущей координаты; оба вызываются только в закомментированных командах `rtp_gas`, иных callers в datapack не найдено.

Rocket Spell trinket вызывает шанс `4%+1%×tmp`; для успеха требуется mana>5, затем снимается 6 mana. Ближайшая стрела в 10 блоках нормализуется из spectral-arrow при необходимости, получает тег `rocket_spell`, Raid Omen amplifier 125 на 10 тиков, звук и `lbcID2/team_number` стрелка. Позднее projectile arrow dispatcher распознаёт score tmp=125 и вызывает `rocket_spell_cast`: стрела удаляется, а функция ставит точку вверх на 80 и назад на 40 относительно контекста, где появляется гигантский item_display золотого меча (`rocket_govnowars`, scale около 2) с тегами `rocketspell,slowraycast,player,mecha`. Несмотря на Owner-score стрелы, ID/team на display в этом helper не переносятся, поэтому фактическая атрибуция дальнейших попаданий неизвестна. Rocket tick находит solid-блок в текущем или соседнем блоке и тогда создаёт семь TNT с `fuse:1` и удаляет дисплей; иначе частица следует за ним, а display падает на 1 блок за tick-call. Внешний `raycastselector_player` запускает этот tick; local-функция не устанавливает свой LifeTime-предел.

Homing Fourier marker обслуживается функцией trinket `homing_evoker/tick`: каждый тик вызывает внешний `lbcr:dmgwithid_un_playeronly`, при отсутствии `nodamage` игрока в двух блоках ищет позицию; затем ищет ближайшую допустимую цель в40 блоках и вычисляет направление из точки к цели, прежней Rotation и двух локальных смещений0,1/1; переносит marker на1 блок по полученному направлению и убивается при Lifetime 130. `find_pos` проверяет поверхность через временную точку на1000 блоков выше неё: если исходный marker попадает вdistance≤1007 от этой точки, призывает `evoker_fangs` на поверхности; иначе — в исходной позиции; сама функция не задаёт урон/владельца фангам.

## Победа и фейерверк

`win` выводит локализованные title/reward из `swrg:lang`, создаёт четыре marker `swrg.firework` и планирует `win_2` через секунду. `win_2` разбрасывает все такие marker вокруг (0,0) в пределах 192 блоков, создаёт у каждого фейерверк LifeTime 60 с двумя эффектами, запускает внешний `swrg:game/end/firework_random` для всех ракет и планирует следующий проход через секунду. Цикл не самозавершается: `end_resets` отдельно выполняет `schedule clear lbc:items/win/win_2`, поэтому после вызова сброса новая итерация не планируется. Очистка расписания не удаляет уже созданные marker/ракеты.



## Источники и точечные пробелы

Остались только явно отмеченные границы, которые исходники этого пакета не разрешают: macro `rotatemap` получает буквальную строку без видимой команды; отсутствующие endpoints и неизвестная атрибуция снаряда описаны по конкретным разделам. Все 201 исходников перечислены в coverage как documented, что означает наличие реального описания наблюдаемого поведения, а не заполнение каталога именами. Для `items/misc/huynya/at_2at_22` literal `lbc:items/misc/huynya/clone` отсутствует в текущем datapack (внешнюю регистрацию не проверял). Ice Gem/Snow Pearl зовут `workingitems/icesword/pre`; audit списка файлов нашёл в этом pack references `workingitems/icesword/0..10`, которых в pack нет, поэтому их конечное поведение не приписано. 
