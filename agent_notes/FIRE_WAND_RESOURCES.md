# Обычный огненный жезл: исходный дизайн и ресурсы

Источники только читались: старый LBC datapack и пользовательский `all_in_one_1.21.8`.

## Выдача и вид

Обычный жезл Фотья Эстиас задаётся `data/lbc/loot_table/sauvojen_tuntija_fotja.json` (id лута `lbc:sauvojen_tuntija_fotja`). Это `minecraft:carrot_on_a_stick`, count 1, `minecraft:tooltip_style=lbc:tier_b`, `minecraft:item_model=lbc:st/fire`. Имя — translate `sauvojen_wand_fire`, цвет `light_purple`, `italic:false`.

Lore заменяется целиком, ровно шесть строк/групп:

1. `type` и `staff`, оба aqua, `italic:false`.
2. Score `stfire_wand_mana_use` из `lbc.math` и `mana_on_activate`, оба aqua, `italic:false`. Исходное значение score — **100** (`data/lbc/function/config.mcfunction`).
3. `sauvojen_wand_fire.lore.1`, gold, `italic:false`.
4. `sauvojen_wand_fire.lore.2`, gold, `italic:false`.
5. `sauvojen_wand_fire.lore.3`, gold, `italic:false`.
6. `sauvojen_wand_fire.lore.4`, yellow, `italic:false`.

Исходный custom data: `{craftusable:1,transformable:1,staff:1,magicwand:1,stfire:1}`.

## Academy замок и переводы

`data/lbc/item_modifier/swrg_unlocks/stfire_academy.json` задаёт закрытому жезлу имя `gui.locked` (red, bold, `italic:false`) и полностью заменяет lore двумя строками: `gui.for_open` (red), затем `lbc.adv.upgraded_tuntija.fire` (red). Условия разблокировки этим modifier не задаются.

Существующие значения ключей в RP (`assets/minecraft/lang/ru_ru.json`, `en_us.json`):

| Ключ | RU | EN |
|---|---|---|
| `sauvojen_wand_fire` | Фотья Эстиас | Fotya Estias |
| `sauvojen_wand_fire.lore.1` | Согревающее пламя | Warming Flame |
| `sauvojen_wand_fire.lore.2` | Стреляет файерболами | Shoots fireballs |
| `sauvojen_wand_fire.lore.3` | Пламя может стать сильнее! | The flame can become stronger! |
| `sauvojen_wand_fire.lore.4` | Можно объединить с Жезлом Ифрита или Киуаскиви | Can be combined with Ifrit's Rod or Kiuaskivi |
| `gui.locked` | Заблокировано (Нажми для информации) | Locked (Click for info) |
| `gui.for_open` | Для разблокировки выполните: | For unlock you need: |
| `lbc.adv.upgraded_tuntija.fire` | Магия огня               | Fire Magic |

Также уже есть `lbc.adv.upgraded_tuntija.fire.lore`: RU `Улучшите Фотья Эстиас в академии. Новые режимы - Большой шар, Шар магмы!`; EN `New modes - Big Ball, Magma Ball!` (оно не используется этим modifier).

## Точка интеграции в переносе

В `LbcWands` зарегистрирован только `TINY_FIRE` (`skywars2:tiny_fire_wand`, ключ имени `wand_fire_tiny`, модель `lbc:st/fire_tiny`) и enum `FIRE` использует его же с маркером `stfire_tiny`, ключом цены `tiny_fire_wand_mana_use`, значением 25. Ранее единственный перенесённый fire loot `ported/tiny_fire_wand.json` был tiny-вариантом. В `LbcItems.register()` уже вызывается `LbcWands.register()`, так что продолжение регистрации относится к `LbcWands`; `LbcItems` связывает его с игровой подсистемой. Переводы tiny (`wand_fire_tiny*`) не заменяют исходные ключи обычного жезла.

Импорт добавлен отдельным вызовом `python tools/import_lbc_wands.py --fire`: он пишет только `ported/fire_wand.json` и `ported/academy_fire_wand.json`, сохраняя выбранную исходную Fotya запись. Academy loot добавляет исходный `add_upgrade_desc` (зелёная `upgraded_item`) и `tiers/a`, поскольку `function/other/magic_academy/upgrade/stfire.mcfunction` вызывает именно `lbc:tiers/a`. Academy-вариант также маркируется полями `upgradable:0,upgrade_available:1,upgraded:1,mode:1`, как другие перенесённые Academy wand loot.

При переносе следует сохранить исходные ключи и оформление из обычного loot, а для Academy закрытого представления — точные две строки modifier. Существующие переводы уже находятся в RP; новых ключей для этих строк не требуется.
