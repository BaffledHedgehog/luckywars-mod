# Ice Sword: контракт исходника

Область проверки: исходный datapack старого сервера и ресурс-пак открывались только для чтения. Это описание кода; запуск на сервере отдельно не проверялся.

## Предмет и оформление

- В datapack меч распознаётся по `minecraft:custom_data {icesword:1}`. Основа — `minecraft:iron_sword`, модель — `lbc:icesword`, стиль подсказки — `lbc:tier_c`. Источник: `loot_table/transformables_sword.json`, запись Ice Sword около строки 614.
- Имя использует ключ `ice_sword` с `italic: false`. Первая строка lore — ключи `type` и `sword`, оба aqua и не курсивом. Следующие строки — `ice_sword.lore.1` и `ice_sword.lore.2`, обе white и не курсивом.
- В прочитанном resource pack значения такие: RU — «Меч Абсолютного Нуля», «С каждым ударом по врагу всё больше», «охлаждает и ослабляет его»; EN — “Sword of Absolute Zero”, “Every hit to the enemy makes it”, “colder and weaker”. При переносе нужно сохранить ключи и оформление. Пак не изменять. Источники: `assets/minecraft/lang/ru_ru.json`, `en_us.json`; `assets/lbc/items/icesword.json` указывает на `lbc:item/icesword`, модель находится в `assets/lbc/models/item/icesword.json` и использует `minecraft:item/icesword`.
- `advancement/true_advancements/swords/ice.json` только показывает достижение: критерий `minecraft:impossible`, заголовок `lbc.adv.sword.ice`, описание `lbc.adv.sword.ice.lore`, родитель `lbc:true_advancements/swords/guardian`.

## Получение

- В `loot_table/transformables_sword.json` Ice Sword — железный меч, количество 1, вес 10 и quality 7. Таблица `transformables_sword_all.json` включает `transformables_sword`; на неё также ссылаются общие и high-quality lucky item таблицы. `27_swords.json` ссылается на `transformables_sword_all`.
- `function/reforge.mcfunction` и `reforge_crafting_0..3.mcfunction` отправляют предмет с `sword:1` в случайный выбор из `lbc:transformables_sword`; `function/other/transform.mcfunction` делает такой же выбор для предмета с `sword:1`. Отдельного гарантированного рецепта или выдачи именно Ice Sword среди проверенных ссылок нет: меч выпадает как один из вариантов общей таблицы.

## Удар, область действия и заморозка

- В `function/items/swords.mcfunction` ветка Ice Sword проверяет выбранный атакующим предмет на `icesword:1`. При отсутствии подавления она вызывает `function/workingitems/icesword.mcfunction`.
- Вызов идёт из обработчика события `player_hurt_entity`; условие исключает только урон типа `ARROW`. Поэтому выбранный меч может активировать способность и после другого non-arrow события урона, а не только после прямого удара мечом.
- `workingitems/icesword.mcfunction` выполняет только три действия: копирует `team_number` атакующего в `*tempt` по `lbcID2`, выбирает в радиусе `0.001..7` все цели с `HurtTime:10s` и `predicate=!lbc:same_team`, вызывает на каждой `workingitems/icesword/pre.mcfunction`, затем проигрывает звук снега. Таким образом, область строится вокруг атакующего и охватывает все подходящие недавно повреждённые цели. В переносе сохраняем этот legacy non-arrow AOE — это принятое решение, а не открытый вопрос.
- `workingitems/icesword/pre.mcfunction` устанавливает базовое добавление холода 10, при нулевом `cold` сначала вызывает `other/freeze/add_cold`, затем добавляет холод к цели, ставит `cold_cd=5`, вызывает `other/freeze/check` и выпускает снежинки. Общая зависимость добавления от текущего Slowness и пороги эффектов описаны в [`FREEZE_SOURCE_AUDIT.md`](FREEZE_SOURCE_AUDIT.md); они подтверждены через `other/freeze/check` и не зависят от отсутствующих вспомогательных функций.
- `workingitems/icesword_player.mcfunction` — отдельный вспомогательный путь: при активном Slowness он вызывает `workingitems/icesword/{0..10}`, без Slowness — `workingitems/icesword/pre`. Нумерованные функции в проверенной папке отсутствуют. Пятикратные вызовы находятся только в `workingitems/icesword_player_x5.mcfunction`; этот путь используется, в частности, взрывом снежного метеора в wand-ветке (`workingitems/awaken_tuntija_wands/boltbolt/click_upgraded_mode_snow_meteor_explode.mcfunction`). Отсутствие `{0..10}` относится к этой вспомогательной ветке и не блокирует перенос прямого `icesword/pre` от Ice Sword. Также `icesword_player` вызывается из `other/eatables/snow.mcfunction`.

## Подавление магии

- `items/changed_inventory/sel.mcfunction` добавляет игроку `nomagic_active`, если в его inventory или weapon slots есть `custom_data {mystical:1,type:1}`; если такого предмета нет, тег удаляется.
- Проверка в ветке Ice Sword подавляет способность, если вокруг атакующего (радиус 20) есть активный игрок не в spectator с тегом `nomagic_active`, либо marker с тегом `stopper_magic`. Проверка marker не требует тега `activated`.
- Предмет подавителя — Panda Spawn Egg из `loot_table/stopper_magic.json`; его `entity_data` создаёт marker с тегами `stopper_magic`, `rc_1sec`, `marker_2t`. `raycasters_1s.mcfunction` активирует его и показывает частицы ведьмы. `other/stopper_magic.mcfunction` убирает поблизости призванных магических мобов, сущности типа `#minecraft:magic_entities` и сущности с тегом `magic` в радиусе 20, после чего удаляет marker.

## Ограниченный следующий этап

Переносить только Ice Sword: зарегистрировать железный меч с исходной моделью и текстом, привязать способность к существующему боевому событию, сохранить non-arrow trigger и 7-блочную AOE-выборку, союзную фильтрацию, контекст `team_number`, заморозку через общий обработчик и две проверки подавления радиусом 20. Вспомогательные numbered функции wand/snow не включать в этот этап.

## Пути исходников

Относительно `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/`:

- `data/lbc/function/items/swords.mcfunction`
- `data/lbc/function/workingitems/icesword.mcfunction`
- `data/lbc/function/workingitems/icesword/pre.mcfunction`
- `data/lbc/function/workingitems/icesword_player.mcfunction`, `icesword_player_x5.mcfunction`
- `data/lbc/function/workingitems/awaken_tuntija_wands/boltbolt/click_upgraded_mode_snow_meteor_explode.mcfunction`
- `data/lbc/function/other/freeze/{add_cold,check}.mcfunction`
- `data/lbc/function/items/changed_inventory/sel.mcfunction`
- `data/lbc/function/other/stopper_magic.mcfunction`, `stopper_magic_destroy.mcfunction`, `stopperactivate_magic.mcfunction`
- `data/lbc/function/raycasters_1s.mcfunction`
- `data/lbc/function/other/eatables/snow.mcfunction`
- `data/lbc/loot_table/transformables_sword.json`, `transformables_sword_all.json`, `lucky_items.json`, `lucky_items_high_quality.json`, `27_swords.json`, `stopper_magic.json`
- `data/lbc/function/reforge.mcfunction`, `reforge_crafting_0..3.mcfunction`, `other/transform.mcfunction`
- `data/lbc/advancement/true_advancements/swords/ice.json`
