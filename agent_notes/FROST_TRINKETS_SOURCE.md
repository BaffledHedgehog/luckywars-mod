# SnowGem18 / IceGem20: исходные карточки и точки входа

Область: только два trinket исходного LBC и их GUI-контракт. Источники ниже — `E:/servers_mew/cringe1_21_4_skywars`; код/ресурсы не менялись. Механика накопления и распада `cold` подробнее описана в [FREEZE_SOURCE_AUDIT.md](FREEZE_SOURCE_AUDIT.md).

## Идентификаторы, прокси и оформление

- **ID 18 / SnowGem18** (`data/lbc/function/swrg_kit_integration/game/trinket_select/select_1.mcfunction:30`) выдаёт `lbc:trinket/snow_gem`. В loot table (`data/lbc/loot_table/trinket/snow_gem.json`) серверный предмет — `minecraft:repeating_command_block`, `minecraft:item_model=lbc:snow_pearl`, stack size 99, `custom_data={trinket:"snow_pearl"}`. Это именно Snow Pearl/`snow_pearl` внутри кода, не ключ `snow_gem`.
  - Name: `lbctrinket.17` (white, italic false); две строки lore `lbctrinket.17.lore.1` и `.2` (gray, italic false).
  - В `assets/minecraft/lang/{ru_ru,en_us}.json` пользовательского пака: RU «Ледяной жемчуг» / «Удар может накладывать стаки холода» / «Шанс 10%% + 2%% за каждый дубликат»; EN “Ice pearl” / “Inflicts cold on hit” / “Chance 10%% + 2%% vor every copy” (опечатка `vor` есть в исходном значении). Сохранять удвоенные `%` и написание дословно.
  - GUI-карточка `data/lbc/loot_table/gui/page/11000/17.json`: proxy `minecraft:stone`, model `lbc:snow_pearl`, те же name/lore, далее динамические pick/win stats.
- **ID 20 / IceGem20** (`select_1.mcfunction:32`) выдаёт `lbc:trinket/ice_gem`. Loot table `data/lbc/loot_table/trinket/ice_gem.json`: серверный `minecraft:repeating_command_block`, модель `lbc:snow_gem`, stack size 99, `custom_data={trinket:"ice_gem"}`.
  - Name `lbctrinket.19` (white, italic false); lore `lbctrinket.19.lore.1` и `.2` (gray, italic false).
  - RU: «Кристаллы льда» / «Получение урона накладывает стаки холода окружающим» / «Шанс 10%% + 1%% за каждый дубликат»; EN: “Ice crystals” / “Taking damage inflicts cold to nearby mobs and players” / “Chance 10%% + 1%% for every copy”. Значения из тех же двух JSON ресурc-пака.
  - GUI-карточка на **слоте 19**, `loot_table/gui/page/11000/19.json`: proxy `minecraft:stone`, модель `lbc:snow_gem`, те же переводы, затем динамические stats. Не путать слот 19 карточки и ID 20 в выдаче.

## Выбор, гейты и каталог

`data/lbc/function/swrg_kit_integration/gui/select/11000.mcfunction` записывает выбор по `swrg.gui_select`: слот 17 проходит с advancement `lbc:true_advancements/legends/academy/snow_wand`, слот 19 — `.../ice_wand`; без advancement выводится соответствующий `lbc.adv.upgraded_staff.{snow_wand,ice_wand}.lore`. `swarogskywars/data/swrg/function/gui/select.mcfunction` и `gui/page.mcfunction` маршрутизируют страницу 11000 в LBC-обработчики. `trinket_select/select_1.mcfunction` сопоставляет именно выборы 18→`snow_gem` и 20→`ice_gem`.

Для `skywars2/lbc_loadouts.json` применим шаблон `tools/import_lbc_loadouts.py`: записи-кандидаты `(id=18, slot=17, page=11000, table=lbc:trinket/snow_gem)` и `(id=20, slot=19, page=11000, table=lbc:trinket/ice_gem)`; собирать gate через существующий `gate(page, slot)` (он возьмёт вышеуказанные advancement и locked key), loot переносить через `migrate_loot_schema`. Идентичность/прокси остаются отдельным решением: этот helper переписывает entry name только у перечисленных в нём предметов.

## Шанс и entrypoints

Формулы читают количество совпадающих предметов в `tmp` из `clear ... 0`; это число копий `n` в инвентаре. `items/trinket_effects/snow_pearl.mcfunction` задаёт `(100 + 20n - 20) × 0.001`, то есть 10% + 2 процентных пункта на каждую копию после первой. `ice_gem.mcfunction` задаёт `(100 + 10n - 10) × 0.001`, то есть 10% + 1 п.п. за каждую следующую. Оба вызывают `func_with_chance_universal`, который выполняет `random_chance` с переданным значением.

- Snow Pearl: `items/swords.mcfunction` → `items/swords_get_trinket.mcfunction` проверяет `{trinket:"snow_pearl"}` → `trinket_effects/snow_pearl.mcfunction` → chance → `snow_pearl_1.mcfunction`. Target selector: `@e[distance=0.001..7,nbt={HurtTime:10s},predicate=!lbc:same_team]`; фильтра по `type`/spectator в этой команде нет. Далее общий `workingitems/icesword/pre`.
- Ice Gem: `items/damaged.mcfunction` → `items/damaged_get_trinket.mcfunction` проверяет `{trinket:"ice_gem"}` → `trinket_effects/ice_gem.mcfunction` → chance → `ice_gem_1.mcfunction`. Selector: `@e[distance=0.001..12,predicate=!lbc:same_team,type=#mobs,tag=!spectator]`, затем тот же `icesword/pre`.

Сверенные исходники: `data/lbc/loot_table/trinket/{snow_gem,ice_gem}.json`, `data/lbc/loot_table/gui/page/11000/{17,19}.json`, `data/lbc/function/swrg_kit_integration/{game/trinket_select/select_1,gui/select/11000}.mcfunction`, `data/lbc/function/items/{swords,swords_get_trinket,damaged,damaged_get_trinket}.mcfunction`, `data/lbc/function/items/trinket_effects/{snow_pearl,snow_pearl_1,ice_gem,ice_gem_1,func_with_chance_universal}.mcfunction`, `data/swrg/function/gui/{select,page}.mcfunction`, and resource-pack `assets/minecraft/lang/{ru_ru,en_us}.json`.
