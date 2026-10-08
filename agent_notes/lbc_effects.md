# Эффекты LBC

Охват — 46 source-функций из job `lbc_effects`. Счётчик эффекта обычно определяет число обработок, но единицу задаёт конкретный цикл: `tick` выполняется в обычном mob/player tick, `tick_5t` раз в 5 тиков, `tick_1s` раз в секунду. Вызванные функции обычно уменьшают свой scoreboard на 1 или 5 и ставят соответствующий `have_lucky_effects*` tag, который обеспечивает следующий вызов; ноль означает, что повторные срабатывания закончились. `sin_greed_active` блокирует большинство эффектов, но не все — исключения перечислены ниже. Обработчики, вызванные в `other`, `workingitems`, `projectiles` и арканумах, здесь прослежены до локального результата лишь настолько, насколько доступны их фактические функции.

## Диспетчер и периодические счётчики

`effects/tick` сначала снимает `have_lucky_effects`, затем обрабатывает эффекты игрока/моба с положительными счётчиками. Ветви `tick` с `tag=!sin_greed_active` пропускаются при sin greed; после сброса `have_lucky_effects` такой score может остаться без обслуживающего tag и не возобновиться автоматически после снятия sin greed; `effect_stand_arrow_hit`, `effect_vacuum`, `effect_add_mana`, `effect_add_manamax` и `effect_add_manaregen` не имеют этого gate и продолжают работать. Обычные эффекты вызываются раз за обычный тик и обычно сами уменьшают счётчик на 1 и заново ставят tag. `effect_shock_potion` также блокируется при `sin_greed_active` и не уменьшает score сама: остаток переносится через `other/shock_effect/zap` при получении урона. `ghosting_tick` подавляется этим gate, но отдельный секундный `ghosting_second` убывает независимо. `effect_remove_mana` и `effect_snus` идут через этот цикл; `piss` и `frozen` делегированы внешним функциям.

`tick_5t` снимает `have_lucky_effects_5t`, затем запускает RTP-счётчики 1–3 с периодом пять тиков. Каждая RTP-функция делает spreadplayers и вычитает 5 из своего счётчика, поэтому счётчик считается в тиках, а не в вызовах. `tick_1s` снимает `have_lucky_effects_1s`; раз в секунду уменьшает Gravity Curse и вызывает delayed-death обработчик. Из-за запуска только при положительном счётчике и одноразовой очистки в конце Gravity Curse удаляет свои модификаторы, когда входной счётчик равен 1, до уменьшения до 0. Внешние `mobs_tick`/`mobs_5t`/`mobs_1s` вызывают эти циклы по выставленным tags; это не расписание из самих эффектных функций.

| Файл | Фактический результат одного вызова | Условие, расход и остановка |
|---|---|---|
| `tick.mcfunction` | Сбрасывает `have_lucky_effects`, по очереди запускает положительные эффекты: взрывы, молнии, RTP4, ловушки, evoker/shock, Kiuaskivi, стрелы/телепортацию, другие обработчики и мана-пулы. | `sin_greed_active` подавляет ветви, включая shock-potion и ghosting, кроме stand-arrow, vacuum и трёх положительных мана-эффектов; `piss`/`frozen` также требуют отсутствия sin greed. |
| `tick_5t.mcfunction` | Сбрасывает tag и запускает `rtp_1/2/3` при соответствующих счётчиках >0. | Требует `have_lucky_effects_5t`; suppression tag sin greed применяется к всем трём. |
| `tick_1s.mcfunction` | Сбрасывает секундный tag и запускает Gravity Curse и delayed death. | Раз в секунду от `mobs_1s`; оба эффекта игнорируются при `sin_greed_active`. |
| `effect_gravity_curse.mcfunction` / `..._end` | При счётчике ровно 1 удаляет `minecraft:gravity_curse` модификаторы с fall-damage multiplier, gravity, step height, safe-fall distance, jump strength и movement speed; после этого вычитает 1. | Выполненная ветка уменьшает счётчик раз в секунду; родитель требует tag и отсутствие sin greed. Cleanup вызывается только на точном значении 1. |
| `effect_add_mana.mcfunction` | Добавляет 10 000 к scoreboard `mana`, выдаёт advancement `true_advancements/potions/add_mana`. | На каждый обычный tick списывает 1 с `effect_add_mana`, ставит lucky tag; sin greed не блокирует. |
| `effect_add_manamax.mcfunction` | Добавляет 2 500 к `manamax`, выдаёт advancement max-mana. | По 1 счётчику за обычный tick; не блокируется sin greed. |
| `effect_add_manaregen.mcfunction` | Добавляет 5 к `manaregen`, выдаёт advancement mana-regen. | По 1 счётчику за обычный tick; не блокируется sin greed. |
| `effect_remove_mana.mcfunction` | Вызывает `other/mana_potions/bad`, затем вычитает 1 из счётчика и ставит lucky tag. | Запускается только при `!sin_greed_active`; точный размер потери mana принадлежит внешнему обработчику. |
| `effect_snus.mcfunction` | Вызывает `other/snus`, выдаёт advancement `potions/snus`, уменьшает счётчик на 1. | Раз в обычный tick, только без sin greed; локальный размер/действие snus находится во внешней функции. |
| `effect_rtp_4.mcfunction` | Раскидывает только `@s`: при `no_borders != 0` — до 140 блоков вокруг текущей позиции, Nether с `under 120`; при `no_borders == 0` — вокруг координат 0,0 с радиусом 190, Nether тоже под уровнем 120. | Вызывается каждый обычный tick при счётчике >0, без sin greed; уменьшает `effect_rtp_4` на 1. В обоих путях `respectTeams=false`. |
| `rtp_1/2/3.mcfunction` | `spreadplayers` переносит `@s` на случайное место в радиусе максимум 10/20/30 вокруг текущей позиции; Nether ограничен `under 120`. | `tick_5t` вызывает раз в 5 тиков; score уменьшается на 5 за вызов, значит длительность score измеряется в тиках; все три подавляются sin greed. |

## Счётчики эффектов попадания и мира

Все три диспетчера сначала снимают свой `have_lucky_effects*`. Если sin greed
подавил все активные ветки соответствующего цикла, положительные scores остаются,
а tag обратно не поставлен. Без нового источника tag обслуживание может остаться
остановленным и после снятия sin greed. Особенно это влияет на достижение cleanup
Gravity Curse: наличие положительного score само по себе не запускает его.
Секундный `ghosting_second` — отдельный прямой вход по score из `mobs_1s`.

Большинство строк ниже — повторно срабатывающие способности с `tick`: один запуск локального эффекта, затем −1 к score и установка `have_lucky_effects`. Для барьеров стоппера указаны конкретные selectors. Математика команды `distance` проверяется вместе с типом и тегами; в этих строках селекторы не задают `dx/dy/dz`.

| Функция и score | Эффект одного срабатывания | Ограничение/передача |
|---|---|---|
| `effect_small_explosion` | Призывает на исполнителе silent, invulnerable Creeper с ExplosionRadius 1, Fuse 0 и `ignited:1b`; он подготовлен к немедленному взрыву, но отдельное поле `Fire` не задано. | Не срабатывает, если любой игрок в радиусе 12 несёт предмет `custom_data:{mystical:1,type:2}` или marker `stopper` в 20; −1 score, lucky tag. |
| `effect_explosion` | Создаёт TNT с `fuse:2` (int 2 тика = 0,1 секунды). | Любой игрок в радиусе 12 с предметом `custom_data:{mystical:1,type:2}` в Inventory либо marker `stopper` в 20 блоках блокирует взрыв; не sin greed; −1. |
| `effect_big_explosion` | Вызывает `projectiles/arrows/bigexplode` в локальном контексте. | Та же проверка любого игрока с mystical-предметом и marker `stopper`, затем −1; геометрия и точный локальный взрыв описаны у projectile arrow handler. |
| `effect_lightning_bolt` | Призывает молнию на позиции исполнителя. | Блокируется, если любой игрок с mystical-предметом находится в 12 блоках или `stopper_magic` marker — в 20; −1. |
| `effect_firebreak` | Ставит fire вместо воздуха в кубе ±3, затем уничтожает блоки в кубе ±1,5. | Та же mystical/stopper_magic проверка перед каждой командой fill; −1 независимо от пропуска. |
| `effect_shock` | Вызывает локальный projectile `arrows/shock`. | Marker `stopper` в 20 отключает hit; −1. |
| `effect_evoker` | Для каждой сущности, кроме evoker fangs и spectator, в 8 блоках вызывает `arrows/evoker` в позиции этой сущности. | Без явной stopper/mystical защиты; −1 один раз за вызов, fan-out — по каждому существу. |
| `effect_evokerarrow` | С шансом 35% создаёт marker `slowraycast,trappertrail,magic,other` с `data.lifetime:400`. | Требует отсутствия `stopper_magic` marker в 20; счётчик уменьшается на 1 даже если шанс не прошёл. Фактическая жизнь marker зависит от внешнего raycaster. |
| `effect_lightarrow` | С шансом 25% создаёт `slowraycast,lightarrowtrail,magic,other` marker с `data.lifetime:200`. | `stopper_magic` в 20 блокирует; score −1 независимо от броска. |
| `effect_dimshifting` | Вызывает `projectiles/teleport_nahui`, который выбирает другое измерение для исполнения. | `stopper_magic` marker в 20 блокирует; локальный счётчик −1. |
| `effect_nuking` | Вызывает внешний `other/nuke/nuke1_at` в точке исполнителя. | Требует отсутствия `stopper` в 20; точное воздействие внешнее; −1. |
| `effect_chain_lightning` | Вызывает внешний `other/imba/chain_lightning`. | `stopper` в 20 блокирует; −1. |
| `effect_timestop` | Вызывает внешний `other/imba/time_stopped_lol`. | `stopper` в 20 блокирует; −1. Подробная остановка времени описана в runtime-документе. |
| `effect_hedgehog_boom` | Вызывает внешний `other/hedgehog_boom`. | `stopper_magic` в 20 блокирует; −1. |
| `effect_kiuaskivi` | Вызывает `items/arcanums/kiuaskivi2arr`. | `stopper_magic` в 20 блокирует; −1. Локальная очистка/заполнение блоков выполняется арканум-функцией. |
| `effect_traps` | Выбрасывает `lbc:traps` loot в точке исполнения. | Без отдельной stopper-защиты; −1. |
| `effect_vacuum` | Телепортирует все не-player сущности в 100 блоках, кроме `strict_map_object`, в текущую позицию и поворот исполнителя. | `stopper` в 20 блокирует. Счётчик −1; в отличие от большинства эффектов не блокируется sin greed. |
| `effect_stand_arrow_hit` | Вызывает внешний `other/stand_arrow/hit_pre`. | `stopper_magic` в 20 блокирует; −1. В `tick` этот эффект работает и при sin greed. |
| `effect_witch_gens_reset` | Вызывает внешний `workingitems/witch_gens/reset`. | `stopper_magic` в 20 блокирует; −1. |

## Токсичные area-cloud эффекты и отдельные эффекты

`raycastselector_area_cloud` передаёт clouds по Raid Omen amplifier: −22 → термит, −21 → сера, −20 → phosphin, −19 → gremuch, 126 → `other/shock_effect/potion/convert`. Последняя ветка преобразует cloud через отдельный helper; это не прямой вызов `effect_shock_potion` у cloud. Родитель увеличивает Lifetime на 1 до выбора ветки. Runtime вызывает area-cloud обработчик каждый обычный tick только при наличии raid omen; дочерние ветки ниже используют позицию самого cloud, если не задают другой контекст.

| Функция | Точный результат | Завершение/зависимость |
|---|---|---|
| `termit_tick` | Если у cloud `Lifetime=1`, сначала прибавляет 100; затем вычисляет радиусы `tmp1=int(0.0125×Lifetime)`, `tmp2=int(0.0078×Lifetime)`, `tmp3=int(0.003×Lifetime)`, рисует flame, иногда (20%) звук лавы и передаёт эти числа macro-функции. Поэтому при первом таком вызове формулы используют Lifetime=101, а не 1. | Нет локального kill или верхнего Lifetime; воздействие повторяется каждым tick-вызовом cloud пока его жизнью не управляет другой код.  |
| `termit_tick_storage` | Макрос внутри `tmp1` радиуса перекрашивает заданные теги блоков: to_nylium→crimson nylium, logs→crimson hyphae, leaves→nether wart block, и наборы к shroomlight/soul sand/magma/netherrack/obsidian/fire/lava. `tmp2`-куб превращает `#solid` в magma block; `tmp3`-куб превращает magma в lava. Затем мобам в `tmp1` наносится 4 lava damage. | Все fill — командные кубы от `~−r` до `~+r`; блоки применяются в заданном порядке, поздние изменения могут поверх ранних менять материал. Числа передаются через storage `lbc.math`. |
| `sulfur_tick` | Ставит template `lbc:air11x11` на offset −5 по каждой оси с integrity 0.005, создаёт sneeze particles в разбросе 4, шанс 3% запускает `drops/muravei_acid` на 1 блок выше cloud. Всем не-spectator мобам на земле в 6 блоках даёт Slowness V на 2 секунды и Wither I на 10 секунд. | Повторяется пока действует cloud; template/particles и debuffs выполняются каждый tick, acid — chance roll каждый раз. Кислотная дочерняя функция принадлежит drops-пакету. |
| `phosphin_tick` | Всем не-spectator мобам в 8 блоках даёт Poison I, Wither I, Weakness I и Slowness I с duration 10 секунд. | Повторяется каждый тик cloud, поэтому эффекты поддерживаются; локального срока cloud/удаления нет. |
| `gremuch_tick` | Призывает TNT fuse `2s` (Short 2 тика =0,1 секунды) и убивает сам cloud. | Однократное срабатывание: после TNT cloud удаляется. |
| `effect_shock_potion` | Рисует у глаз переходную пыль и electric spark; при `HurtTime:10s` (SNBT Short 10, то есть 10 тиков) вызывает внешний `other/shock_effect/zap`. | Каждый обычный tick рисует частицы и ставит `have_lucky_effects`, но сам не уменьшает score. При уроне просмотренный zap забирает единицу, очищает счётчик исполнителя, переносит остаток цели цепи и наносит исполнителю 6 урона типа arrow; дальнейшая цель выбирается внешними `zap/cast` командами. |

## Сущности, временные изменения и ghosting

| Функция | Действие | Условия и окончание |
|---|---|---|
| `cumming` | При каждом player-tick с score `cumming>0` с шансом 10% сначала удаляет тег `cumming`, независимо ещё с шансом 10% добавляет его снова, с шансом 5% выдаёт Slowness amplifier 99 на 1 секунду; затем score уменьшается на 1. | Только если после бросков тег остался, `execute at @s` вызывает визуальный `cumming1`. Проверки chance10 выполняются отдельно и в указанном порядке; вход игрока проверяется по положительному score, а не по наличию этого визуального тега. |
| `cumming1` | Рисует две item-частицы white stained glass на `~ ~.9 ~` и `~ ~.85 ~`, задавая локальные направления со смещениями `^100 ^ ^70000` и `^-100 ^ ^60000`, с speed 0,00001. | Чисто визуальная функция; не изменяет entity, не удаляет marker и не снижает счётчик. |
| `ghosting_tick` | На обычном tick даёт исполнителю Invisibility II на 1 секунду, ставит lucky tag и только при спринте в воздухе зовёт `ghosting_tick1`. Если NBT `HurtTime` не равно Short 10 (10 тиков) — завершает функцию; если равно — призывает Vex с `DeathLootTable:lbc:wind_charge`, cloud particles и звук красителя. | Срабатывание на ударе завязано на точное значение HurtTime 10; внешний `apply_ghosting` выбирает для Vex особую ветку. |
| `ghosting_tick1` | Формирует строку `motion add ^ ^ ^.2 @s` и отправляет её через macro `lbc:run_command`; мост помечен в [COMMAND_BRIDGES.md](COMMAND_BRIDGES.md) как команда серверного расширения. Датапак только передаёт запрос движения, фактическое изменение Motion зависит от реализации моста. Также показывает gust/soul и звук шага по шерсти. | Вызывается только если ghosting tick видит спринт и `!on_ground`; локальной очистки/длительности нет, пока продолжается счетчик. |
| `ghosting_second` | Раз в секунду отнимает 1 `effect_ghosting`, рисует 5 soul particles, при результате ровно 0 вызывает cleanup `ghosting_end`, ставит секундный lucky tag. Внешняя `other/event/halloween/soulcorruptwand/apply_ghosting` задаёт счётчик 5 секунд, эти эффекты атрибутов и оба цикла; для Vex она вместо этого вызывает `ghosting_second_vex`. | Длительность score в секундах; cleanup вызывается на нуле после уменьшения. |
| `ghosting_end` | Удаляет пять именованных attribute modifiers: armor, armor toughness, attack damage, gravity и fall-damage multiplier. | Чистит перечисленные modifier UUID/name у исполнителя; это не сбрасывает score и не убивает сущность. |
| `ghosting_second_vex` | Выбрасывает loot `lbc:halloween/metasola`, ставит одному item в радиусе 0,1 NoGravity и Motion [0,−0.06,0], проигрывает vex charge, cloud particles, затем убивает исполнителя Vex. | Однократная ветка для Vex через `other/event/halloween/soulcorruptwand/apply_ghosting`; выбор имеет limit=1 без sort и не закреплён за UUID нового loot item. |
| Внешние дочерние функции | Результат за пределами job: `other/hedgehog_boom`, `other/nuke/nuke1_at`, `other/imba/chain_lightning`, `other/imba/time_stopped_lol`, `other/stand_arrow/hit_pre`, `workingitems/witch_gens/reset`. | Здесь зафиксированы только вызывающие эффекты, блокирующие теги/радиусы и уменьшение score; конечные эффекты принадлежат другим пакетам LBC и не покрываются этим job. |

## Точки интеграции и границы проверки

Прямые связи, подтверждённые локальными runtime-вызовами: `mobs_tick` запускает `effects/tick` для существ с `have_lucky_effects`; `mobs_5t` и `mobs_1s` обслуживают соответствующие теги; `ghosting_second` вызывается по положительному счётчику из `mobs_1s` отдельно, независимо от `have_lucky_effects`/sin greed. `players_tick` запускает `cumming`; `raycastselector_area_cloud` маршрутизирует токсичные clouds. Runtime-планирование и time-stop-пауза описаны в `lbc_runtime.md`. `effect_big_explosion`, `effect_shock` и `effect_dimshifting` передают работу в локальные projectile handlers; `effect_kiuaskivi` в арканум; `sulfur_tick` в drops; другие `other/*` и `workingitems/*` функции — внешние зависимости текущего job. Точные эффекты этих зависимостей здесь не переутверждаются.
