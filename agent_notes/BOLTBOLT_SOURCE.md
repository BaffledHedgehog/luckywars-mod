# Lightning Bolts + Stteleport → Boltbolt: исходное поведение

Исследование 19 октября 2026. Только чтение исходника, сервер/build не запускались. Корень всех указанных ниже исходных путей: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`. Это описание наблюдаемого datapack, не подтверждение runtime. Прочитаны корневые `AGENTS.md`, `ARCHITECTURE.md` и `agent_notes/AGENTS.md`; `agent_notes/ARCHITECTURE.md` отсутствует.

## Обязательные требования переноса

Источник убийства и отложенных последствий обязателен (user). Общая защита союзников, наблюдателей, служебных NPC; никаких исключений Creative/developer. Рецепт обычный native 2×2/3×3, ровно 1 `lightning_bolts` + 1 `stteleport` (user), без старого ground craft/расхода всех подходящих ячеек. Один terminal на луч: исходная возможность обработать одновременно стену и моба уже запрещена пользователем. Не создавать второй движок молний: root подтвердил существующие `LbcElectro.lightning(owner,point)` и `shock(target,owner,pulses)`. Rideable Arrow повторно использует механизм `LbcTeleport.cloud()` после согласования доступности. Ввод/мана/Academy/крафт остаются у общих владельцев из ARCHITECTURE.

## Реальный ввод обеих рук

`lbc/function/load.mcfunction:8`: `RC_Raycast` — статистика `minecraft.used:minecraft.carrot_on_a_stick`; `players_tick.mcfunction:19` → `items/spells.mcfunction`. Это ПКМ, имя `left` означает offhand, не ЛКМ. `items/spells` сначала выполняет mainhand `raycastspell`/`magicwand`, затем независимо offhand (если настройка `lefthand` не 1). Одна статистическая активация способна применить обе руки; каждая отдельно проверяет и расходует текущую ману. `workingitems/spell[ _left].mcfunction` выбирает upgraded по данным соответствующей руки; `lbcr/function/raycast[ _left / _upgraded / _upgraded_left].mcfunction` выставляет временный `raycaster` и вызывает профильную ветку.

`items/spells` запрещает каст `nomagic_active` самому и при таком игроке R20. Raycast entry запрещает `stopper_magic` R20. Во время лучей дальнейшая рекурсия запрещена при `nomagic_active` R16 или `stopper_magic` R20. Старый Overload временно добавлял 10 млн маны; перенос должен использовать общий `trySpendWand`, а не копировать эту прибавку. Во всей исследованной семье отдельного cooldown нет.

## Донор Lightning Bolts

`config.mcfunction:41`: 3000 маны. Обычный mainhand начинается от глаз; обычный offhand — +0.5 вперёд. `lbcr/function/raycast_lightningpre.mcfunction` вызывает луч, затем снимает 3000, применяет mage_set_bonus и trinket cast. `raycast_lightning.mcfunction` шаг 0.5, Distance +1, рекурсия при <=200: примерно 100 блоков (201 выборка при начальном нуле). Entity terminal — пересечение единичной command-box (`dx=dy=dz=0`) с мобом, дополнительный фильтр distance<=15, не сфера поражения R15. Block terminal ставится на 0.5 назад от препятствия. End rod частицы.

`raycast_lightnings.mcfunction`: одна молния в terminal, плюс отдельная молния на КАЖДОЙ сущности (не только мобе) R3, кроме lightning_bolt и spectator. Здесь нет исключения caster/team/NPC для вторичных молний; это пробел переноса, а не желаемый friendly fire. Выдаётся advancement staffs/lightning при сущности R3 без OnGround. Distance=1000 останавливает рекурсию. Молния native, явного числа damage в этих функциях нет; не подменять догадкой о фиксированном уроне.

### Academy донора

`raycast_lightnings_check.mcfunction`: terminal проверяет минимум **четыре отдельных item entity** с `custom_data.transformable:1` R2 (limit4), не count предметов в одном stack; при успехе → общий `lbc:other/magic_academy/can_upgrade`. Проверка запускается только если **mainhand** имеет upgradable, даже при касте offhand — исходная ошибка привязки руки. `other/magic_academy/can_upgrade/lightning_bolts` только показывает translate upgrade_condition_lightning_bolt. `upgrade/lightning_bolts` показывает upgrade_succesful_lightning_bolt и применяет tier s к mainhand; фактическое upgraded/mode задаёт общий Academy pipeline.

`raycast_lightningpre_upgraded[ _left]`: Shift+ПКМ циклически переключает 3 режима и пишет название. Переключение проходит входной порог 3000, но не расходует ману. Режим хранится в custom_data.mode отдельно в руке. Обычный ПКМ → `_upgraded_2`:

1. Default: исходный луч 0.5/200, terminal `raycast_lightnings_upg` ставит молнию в точке и на всех не-lightning/non-spectator сущностях **R5**, расход3000.
2. Reforge: `raycast_lightning_mode_reforge` шаг0.35, Distance<=20 (~7 блоков), пересечение item-box с distance<=4 либо блок. Terminal `_end` превращает transformable item entities R1 через общий `lbc:reforge`. **Входной порог3000 остаётся, реальный расход0**, mage bonus не начисляется, trinket cast вызывается. `lbc/function/reforge.mcfunction` создаёт результат соответствующей категории, расходует один item в stack; не клонирует весь stack. Terminal firework/anvil sound.
3. Chain: луч0.5/200, terminal entity filter distance<=10; молния в точке и tag `lightningist` всем не-lightning/non-spectator/non-caster сущностям R3. Расход3000. `tick2s.mcfunction` → `tick2smobs.mcfunction` → `other/lightning.mcfunction`: каждые40ticks снимает tag с текущей цели, передаёт всем мобам R0.001..16 без tag и spectator, вызывает молнию. Нет TTL, owner, visited-set или запрета обратной передачи: две соседние цели могут поддерживать цепь бесконечно. `mobs_5t.mcfunction` даёт tagged целям electric_spark раз/5ticks. Из-за последовательной обработки целей tag может перейти далее в тот же двухсекундный проход; переносу нужна осознанная семантика, не зависимость от порядка entity iteration.

## Донор Stteleport

Это существующая ветка LbcTeleport; повторно не реализовывать. `config:49`3000; `workingitems/spell1_default[ _left]` → `sauvojen_staffs/teleport` → `teleport_preloop`, marker `raycastteleportwand`, lifetime200, owner/team. `teleport` расходует3000. Academy trial: `teleport_preloop.mcfunction:7` при sneak и mainhand.upgradable вызывает `teleport_preloop_upgrade`: +5 shock самому заклинателю, затем **35%** (`predicate chance35`) немедленно `can_upgrade`. Следовательно trial — шанс при касте с приседанием, не ожидание пережить пять молний. Проверка руки также mainhand-зависимая. Academy teleport modes и полный payload находятся у существующего владельца, не расширены этим исследованием.

## Результат Boltbolt

`other/craft/inventory_craft_check:17`, `other/sauvojen_tuntija/crafted_boltbolt_inv`: исходный 2×2 craft проверял присутствие обоих donors, создавал dropped loot, затем уменьшал КАЖДУЮ подходящую ячейку, включая дубликаты. Ground path `other/craft_ground:17` → `crafted_boltbolt`. При переносе заменить user-контрактом exact1+1 native crafting. `loot_table/tuntijaa_bolts_bolts.json`: count1 carrot_on_a_stick, model `lbc:boltbolt_wand`, tier_a, custom_data `{transformable:1,staff:1,raycastspell:1,boltbolt_wand:1}`. Donor mode/upgraded не наследуется этим loot.

`config:87`1830 маны для обычного и всех Academy режимов результата. `workingitems/awaken_tuntija_wands/boltbolt/click` → `loop`, thunder sound, расход1830, mage/trinket. `loop`: шаг0.75, Distance<=200 (~150блоков), cloud/sculk_soul; mob box filter distance<=10 или non-air → `end`. В этой ветке нет локального сброса Distance перед ordinary cast; перенос обязан инициализировать состояние каста, не наследовать чужую Distance.

`end`: native lightning в точке; пять marker на смещениях (+10,0,0), (0,0,+10), (-10,0,0), (0,0,-10), (0,+10,0), tags everlasting_bolt/rare/rc_1sec/slowraycast/magic, data.lifetime200. `get_id` копирует owner/team ближайшего raycaster. Мобам R5 кроме spectator/raycaster добавляет **10 shock**. Weather thunder60s. Distance500.

### Длительность и самонаведение молний

Это важная исходная неоднозначность: `tick_not_timestopped.mcfunction:3` каждый тик → `raycastselector` (Lifetime+1), а `tick1s.mcfunction:3` раз20ticks → `raycasters_1s` (Lifetime+20). Оба сравнивают с data.lifetime200 и удаляют marker. Таким образом одновременно активные dispatcher дают **примерно100ticks / 5секунд**, с зависимостью от фазы секундного scheduler; трактовка «200ticks=10секунд» не соответствует исходному двойному учёту. Это не вечные сущности вопреки имени everlasting.

`charge_tick`: каждый tick cloud+sculk_soul; исключает owner и игроков его команды, а также spectator/strict_map_object. Если нет целиR15, движется0.5 к ближайшей подходящей цели **без ограничения дальности**. Если есть цельR15 — телепортируется к случайной подходящей целиR25 (может выбрать иную, чем вошедшая вR15). Без цели не двигается. `charge_tick_1s` создаёт native lightning раз/сек независимо от присутствия цели. Последствия молнии требуют общего owner/защиты, несмотря на частичный фильтр наведения в source.

### Shock, испытание и HUD

`tick2smobs:2` каждые40ticks при shock>=1 → `other/shock`: native lightning на цели, end_rod, shock-1. Отдельный `effect_shock` — другая механика, не смешивать. `ui/get_data.mcfunction:36` HUD `\uE121`, красный текущий **счётчик** shock. Это количество будущих двухсекундных импульсов, а не секунды. Cleanses/resets/leave_death очищают shock.

Academy результата: `boltbolt/end.mcfunction:10` сразу после +10 ищет мобаR5 с shock>=60, если mainhand boltbolt_wand+upgradable → `can_upgrade`. Требуется накопить60 с учётом спада по1/2s (обычно не менее6 попаданий). Отдельной серии вызванных молний для испытания не создаётся; состояние готовности даёт terminal cast. Offhand проверяется ошибочно через mainhand. `can_upgrade/boltbolt_wand` только описание; `upgrade/boltbolt_wand` tier_ss и сообщение.

### Academy результата: четыре режима

`click_upgraded[ _left]`: Shift+ПКМ cycles4 (Default, Wind Racer, Snow Meteor, Watering), отдельный mode руки; `_pre` сбрасывает Distance0, делает payload, затем1830, mage/trinket. Для переключения общий raycast entry всё ещё требует1830, переключение бесплатно.

1. Default — тот же ordinary loop/end и пять молний.
2. Wind Racer — `click_upgraded_mode_wind_racer`: Arrow NoGravity, Pierce127, damage0, owner/team, dismount старого транспорта, mount игрока. `projectiles/pre:14` → `racer_arrow` каждый tick: находит владельцаR3, при отсутствии включает gravity; ориентирует движение по владельцу и `raycast_vpered_slow` задаёт Motion вдоль взгляда **2блока/tick**; cloud50. С вероятностью30% каждыйtick → `racer_arrow_lighting`: четыре marker raycastmolnia под ним с lifetime30/35/40/45, owner/team. Локального TTL стрелы не задано; не выдумывать фиксированный срок. Повторно использовать native rideable Arrow lifecycle.
3. Snow Meteor — луч0.75/200 с snowflake, terminal mob-box distance<=10 или блок → item_display big_snowball **+50Y**. `marker_2t` dispatcher раз2ticks; first устанавливает scale3, rotation(-90,-90), interpolation/teleport_duration2. Затем каждый2ticks падает0.5 плюс последовательные добавки1/1.5/2/2.5/3 при Lifetime>=5/10/15/20/25: максимальная сумма **10.5блока за вызов**, не только3. Проверяет non-air по Y-5..-1, затем explode. data.lifetime нет, фиксированного TTL нет. Explode: snowflake1000; air_sphere_11x11x11 со смещением(-5,-6,-5); три пересекающихся fill снегом: X±7,Y±10,Z±10 / X±10,Y±10,Z±7 / X±10,Y±7,Z±10, заменяют только tag `minecraft:tannerkivi`; damage35 freeze всем мобамR10; пять вызовов icesword_player каждому, затем big_snowball template(-3,-7,-3), sounds, kill display. В `icesword_player` есть ссылки на отсутствующие `icesword/0..10`; путь без slowness → `icesword/pre` добавляет10cold и cold_cd5. Поэтому нельзя честно обещать ровно50cold во всех состояниях: это пять обращений к существующей системе холода, с ошибочными legacy ссылками. Owner у display/watering source не записывается — исправить.
4. Watering — water_breathing240s amplifier0, strength240s amplifier2. Пять marker в центре и ±5X/±5Z, data.lifetime200, marker_2t/watering. Только `markers_2t` Lifetime+2 раз2ticks → **200ticks=10s**, здесь нет двойного dispatcher. Каждый2ticks `watering_tick` заполняет water только air в X/Z±2,Y±1, затем `universal_pos_razb_7` переносит marker по каждой оси случайно[-7,+7] от целочисленных текущих координат. Это блуждающие источники, не неподвижная водяная сфера. Учитывать owner последствий блоков/жидкости.

## Уточнения пользователя — 19 октября 2026 (user)

- Срок пяти зарядов: «Исправить: 10 секунд». Цель переноса — 200 тиков,
  без двойного отсчёта старых dispatcher.
- Reforge: «сделай 0 допуск 0 списания, идея этого режима - более удобная перековка без траты маны».
  Прежний входной порог 3000 выше описывает только исходник.
- Chain: «Сохранить повторные переходы». Запрет повторного посещения цели
  не добавляется; очистка статуса и lifecycle матча остаются обязательными.

## Исходные неоднозначности

Реальные спорные исходные механики: Reforge с входным порогом3000 и нулевым расходом; бесконечная передача lightningist без TTL; удвоенный счётчик Lifetime у пяти молний (фактически~5s). Остальное описанное число35damage/240sбаффы само по себе не повод менять баланс. Отдельно исправлять ошибки руки, утерянный owner, missing ice routes, старый двойной terminal и дублирующий расход craft согласно уже данным требованиям, а не сохранять их как баланс.

Непрямые убийства: обычные/вторичные/native молнии, shock, chain должны наследовать caster; холод, снег и стирание опоры, Watering fluid и падение/утопление, rider arrows и молнии под ними — тот же owner. Служебный NPC не должен стать ни триггером terminal, ни целью наведения, ни целью вторичной цепи. Старые широкие селекторы не доказывают разрешение вреда союзникам.
