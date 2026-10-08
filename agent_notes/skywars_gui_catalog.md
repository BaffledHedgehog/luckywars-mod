# Каталог исходников GUI SkyWars

Файлы ниже сгруппированы по назначенным каталогам. Механика и смысловые выводы описаны в [основном разборе](skywars_gui.md); этот индекс нужен для точных переходов к каждому файлу.

## Карты

GUI-тексты на страницах map-select показывают следующие наборы имен. Выбор меняет общий `#map`; окончательный loader ID дополнительно ограничен доступной интеграцией maploader.

| Страница | Кнопки карт (в порядке слотов, кроме навигации) |
| --- | --- |
| 2000 | Aegis; Lucky Blocks; Ring; Tribute; Jagged; Village; Sentinel; Embercell; Aquacrown; Dwarven; Shire; Siege; Skychurch; Strata; Towers; Tribal; Candylane; Dawn; Felkenheart; Fruitcake; Abandoned Subway; Brawl Stars; Cubes; Destroyed Towers; Forest |
| 2000b | Divine Defecation (8); Asteroid (4); Fountain (6); Bigarden (8); Desert Castle (8); Pernicious (8); Twins Towers (2); Highland Peaks (8); Pavilion (8); Sand Dunes (4); Coalwars (2); далее существующие большие карты из набора 2000 |
| 2100 | Hell; Jungle; Monkeys; Oasis; Swamp; Iron Swords; Astraeus Station; Alchemy Table; Steampunk; Iron Axe; Glacier; Sandcastle; Natural; Concordia Horizon; Acua; Beach Rush; Candy; Rubik’s Cubes; Detective; Huts; Japon; Advanced; Amesa; Blimp; The End - Chapter I: Dragon Island |
| 2200 | The End Chapters II–IV; TNT; China; End; Stonehenge; Japan; End Pillars; Blossom Meadows; Sand; Orcs; Dragonball; остальные слоты страницы используют названия, заданные loot tables |

Источники: [`page/2000`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/2000.mcfunction), [`page/2000b`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/2000b.mcfunction), [`page/2100`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/2100.mcfunction), [`page/2200`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/2200.mcfunction), [`select/2000`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2000.mcfunction), [`select/2000b`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2000b.mcfunction), [`select/2100`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2100.mcfunction), [`select/2200`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2200.mcfunction).

## Locked-подсказки

| Модификатор | Что сообщает lore / откуда unlock |
| --- | --- |
| `locked_kit_default`, `locked_skill_default` | Получить соответствующий kit/skill key |
| `locked_cosmetics_default` | Получить косметический ключ |
| `locked_suffix_pandora`, `locked_suffix_halloween` | Открыть указанную сезонную case |
| `locked_event` | Дождаться/открыть турнир |
| `locked_halloween_food`, `locked_halloween_mace`, `locked_wind_wand_upgraded` | Выполнить соответствующее LBC advancement |
| `locked_pumpkin_sword`, `locked_soulcorrupt_wand`, `locked_soulcorrupt_wand_upgraded`, `locked_soulhunter_mace` | Получить LBC advancement/предмет по названию |
| `locked_turnir1` | Получить турнирный trophy |
| `locked_wind_wand` | Получить LBC advancement ветки wind wand |

Названия подсказок здесь сокращены до смысла; фактический gating выполняется handlers `select/3000`, `4000`, `7300`, `7310`, `7400`, `7500`, а не modifier. `page/7300_default` и `page/7300_halloween` заменяют закрытые suffix на locked модели; клик по закрытому варианту не запускает покупку или открытие кейса. Кнопка ведёт к соответствующему ключу только через переход назад в меню кейсов/магазина.

## `function/gui`

- [`function/gui/clear.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/clear.mcfunction)
- [`function/gui/main.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/main.mcfunction)
- [`function/gui/match.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/match.mcfunction)

## `gui/page`

- [`function/gui/page/0.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/0.mcfunction)
- [`function/gui/page/1000-lang.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/1000-lang.mcfunction)
- [`function/gui/page/1000.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/1000.mcfunction)
- [`function/gui/page/1100.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/1100.mcfunction)
- [`function/gui/page/15000.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/15000.mcfunction)
- [`function/gui/page/15000_check_beds.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/15000_check_beds.mcfunction)
- [`function/gui/page/2000.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/2000.mcfunction)
- [`function/gui/page/2000b.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/2000b.mcfunction)
- [`function/gui/page/2100.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/2100.mcfunction)
- [`function/gui/page/2200.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/2200.mcfunction)
- [`function/gui/page/3000.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/3000.mcfunction)
- [`function/gui/page/3000_ranked.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/3000_ranked.mcfunction)
- [`function/gui/page/4000.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/4000.mcfunction)
- [`function/gui/page/4000_ranked.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/4000_ranked.mcfunction)
- [`function/gui/page/5000-5400.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/5000-5400.mcfunction)
- [`function/gui/page/5000.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/5000.mcfunction)
- [`function/gui/page/6000.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/6000.mcfunction)
- [`function/gui/page/7000.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/7000.mcfunction)
- [`function/gui/page/7100.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/7100.mcfunction)
- [`function/gui/page/7200.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/7200.mcfunction)
- [`function/gui/page/7300.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/7300.mcfunction)
- [`function/gui/page/7300_default.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/7300_default.mcfunction)
- [`function/gui/page/7300_halloween.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/7300_halloween.mcfunction)
- [`function/gui/page/7310.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/7310.mcfunction)
- [`function/gui/page/7310_default.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/7310_default.mcfunction)
- [`function/gui/page/7310_halloween.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/7310_halloween.mcfunction)
- [`function/gui/page/7400.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/7400.mcfunction)
- [`function/gui/page/7400_halloween.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/7400_halloween.mcfunction)
- [`function/gui/page/7500.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/7500.mcfunction)
- [`function/gui/page/7500_halloween.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page/7500_halloween.mcfunction)

## `function/gui`

- [`function/gui/page.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page.mcfunction)

## `gui/roll`

- [`function/gui/roll/0.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/0.mcfunction)
- [`function/gui/roll/1.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/1.mcfunction)
- [`function/gui/roll/2.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/2.mcfunction)
- [`function/gui/roll/3.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/3.mcfunction)
- [`function/gui/roll/4.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/4.mcfunction)
- [`function/gui/roll/fill0.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/fill0.mcfunction)
- [`function/gui/roll/fill1.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/fill1.mcfunction)
- [`function/gui/roll/fill2.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/fill2.mcfunction)
- [`function/gui/roll/fill3.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/fill3.mcfunction)
- [`function/gui/roll/fill4.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/fill4.mcfunction)
- [`function/gui/roll/have1.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/have1.mcfunction)
- [`function/gui/roll/have2.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/have2.mcfunction)
- [`function/gui/roll/have3.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/have3.mcfunction)
- [`function/gui/roll/have4.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/have4.mcfunction)
- [`function/gui/roll/player.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/player.mcfunction)
- [`function/gui/roll/reset.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/reset.mcfunction)

## `roll/reward`

- [`function/gui/roll/reward/0.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/reward/0.mcfunction)
- [`function/gui/roll/reward/1.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/reward/1.mcfunction)
- [`function/gui/roll/reward/2.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/reward/2.mcfunction)
- [`function/gui/roll/reward/3.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/reward/3.mcfunction)
- [`function/gui/roll/reward/4.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/reward/4.mcfunction)
- [`function/gui/roll/reward/bars.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/reward/bars.mcfunction)
- [`function/gui/roll/reward/coins.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/reward/coins.mcfunction)
- [`function/gui/roll/reward/memcoins.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/reward/memcoins.mcfunction)
- [`function/gui/roll/reward/sounds.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/roll/reward/sounds.mcfunction)

## `gui/select`

- [`function/gui/select/0.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/0.mcfunction)
- [`function/gui/select/1000.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/1000.mcfunction)
- [`function/gui/select/1000_change_prefix.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/1000_change_prefix.mcfunction)
- [`function/gui/select/1100.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/1100.mcfunction)
- [`function/gui/select/15000.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/15000.mcfunction)
- [`function/gui/select/2000.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2000.mcfunction)
- [`function/gui/select/2000_mapselect.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2000_mapselect.mcfunction)
- [`function/gui/select/2000b.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2000b.mcfunction)
- [`function/gui/select/2000b_mapselect.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2000b_mapselect.mcfunction)
- [`function/gui/select/2100.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2100.mcfunction)
- [`function/gui/select/2100_mapselect.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2100_mapselect.mcfunction)
- [`function/gui/select/2200.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2200.mcfunction)
- [`function/gui/select/2200_mapselect.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2200_mapselect.mcfunction)
- [`function/gui/select/3000.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/3000.mcfunction)
- [`function/gui/select/3000_ranked.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/3000_ranked.mcfunction)
- [`function/gui/select/4000.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/4000.mcfunction)
- [`function/gui/select/4000_ranked.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/4000_ranked.mcfunction)
- [`function/gui/select/5000.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/5000.mcfunction)
- [`function/gui/select/6000.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/6000.mcfunction)
- [`function/gui/select/7000.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/7000.mcfunction)
- [`function/gui/select/7100.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/7100.mcfunction)
- [`function/gui/select/7200.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/7200.mcfunction)
- [`function/gui/select/7300.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/7300.mcfunction)
- [`function/gui/select/7310.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/7310.mcfunction)
- [`function/gui/select/7400.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/7400.mcfunction)
- [`function/gui/select/7500.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/7500.mcfunction)
- [`function/gui/select/config.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/config.mcfunction)

## `function/gui`

- [`function/gui/select.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select.mcfunction)
- [`function/gui/set_slot_id.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/set_slot_id.mcfunction)

## `swrg/item_modifier`

- [`item_modifier/balance.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/balance.json)
- [`item_modifier/balance_mem.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/balance_mem.json)
- [`item_modifier/compass.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/compass.json)
- [`item_modifier/diamond_axe.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/diamond_axe.json)
- [`item_modifier/empty.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/empty.json)
- [`item_modifier/golden_axe.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/golden_axe.json)
- [`item_modifier/iron_axe.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/iron_axe.json)

## `item_modifier/locked`

- [`item_modifier/locked/halloween_food.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/locked/halloween_food.json)
- [`item_modifier/locked/halloween_mace.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/locked/halloween_mace.json)
- [`item_modifier/locked/pumpkin_sword.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/locked/pumpkin_sword.json)
- [`item_modifier/locked/soulcorrupt_wand.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/locked/soulcorrupt_wand.json)
- [`item_modifier/locked/soulcorrupt_wand_upgraded.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/locked/soulcorrupt_wand_upgraded.json)
- [`item_modifier/locked/soulhunter_mace.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/locked/soulhunter_mace.json)
- [`item_modifier/locked/turnir1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/locked/turnir1.json)
- [`item_modifier/locked/wind_wand.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/locked/wind_wand.json)
- [`item_modifier/locked/wind_wand_upgraded.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/locked/wind_wand_upgraded.json)

## `swrg/item_modifier`

- [`item_modifier/locked_cosmetics_default.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/locked_cosmetics_default.json)
- [`item_modifier/locked_event.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/locked_event.json)
- [`item_modifier/locked_kit_default.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/locked_kit_default.json)
- [`item_modifier/locked_skill_default.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/locked_skill_default.json)
- [`item_modifier/locked_suffix_halloween.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/locked_suffix_halloween.json)
- [`item_modifier/locked_suffix_pandora.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/locked_suffix_pandora.json)
- [`item_modifier/netherite_axe.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/netherite_axe.json)
- [`item_modifier/select.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/select.json)
- [`item_modifier/stone_axe.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/stone_axe.json)
- [`item_modifier/wooden_axe.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/item_modifier/wooden_axe.json)

## `loot_table/gui`

- [`loot_table/gui/back.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/back.json)
- [`loot_table/gui/next.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/next.json)

## `page/0`

- [`loot_table/gui/page/0/0.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/0.json)
- [`loot_table/gui/page/0/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/1.json)
- [`loot_table/gui/page/0/12.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/12.json)
- [`loot_table/gui/page/0/13.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/13.json)
- [`loot_table/gui/page/0/14.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/14.json)
- [`loot_table/gui/page/0/17.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/17.json)
- [`loot_table/gui/page/0/17_old.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/17_old.json)
- [`loot_table/gui/page/0/19.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/19.json)
- [`loot_table/gui/page/0/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/2.json)
- [`loot_table/gui/page/0/20.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/20.json)
- [`loot_table/gui/page/0/21.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/21.json)
- [`loot_table/gui/page/0/22.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/22.json)
- [`loot_table/gui/page/0/23.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/23.json)
- [`loot_table/gui/page/0/24.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/24.json)
- [`loot_table/gui/page/0/25.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/25.json)
- [`loot_table/gui/page/0/26.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/26.json)
- [`loot_table/gui/page/0/6.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/6.json)
- [`loot_table/gui/page/0/7.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/7.json)
- [`loot_table/gui/page/0/8.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/8.json)
- [`loot_table/gui/page/0/9.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/0/9.json)

## `page/1000`

- [`loot_table/gui/page/1000/0.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/0.json)
- [`loot_table/gui/page/1000/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/1.json)
- [`loot_table/gui/page/1000/10.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/10.json)
- [`loot_table/gui/page/1000/11.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/11.json)
- [`loot_table/gui/page/1000/12.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/12.json)
- [`loot_table/gui/page/1000/13.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/13.json)
- [`loot_table/gui/page/1000/14.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/14.json)
- [`loot_table/gui/page/1000/15.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/15.json)
- [`loot_table/gui/page/1000/16.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/16.json)
- [`loot_table/gui/page/1000/17.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/17.json)
- [`loot_table/gui/page/1000/19.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/19.json)
- [`loot_table/gui/page/1000/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/2.json)
- [`loot_table/gui/page/1000/20.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/20.json)
- [`loot_table/gui/page/1000/21.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/21.json)
- [`loot_table/gui/page/1000/22.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/22.json)
- [`loot_table/gui/page/1000/23.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/23.json)
- [`loot_table/gui/page/1000/24.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/24.json)
- [`loot_table/gui/page/1000/25.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/25.json)
- [`loot_table/gui/page/1000/26.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/26.json)
- [`loot_table/gui/page/1000/3.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/3.json)
- [`loot_table/gui/page/1000/4.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/4.json)
- [`loot_table/gui/page/1000/5.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/5.json)
- [`loot_table/gui/page/1000/6.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/6.json)
- [`loot_table/gui/page/1000/7.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/7.json)
- [`loot_table/gui/page/1000/8.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/8.json)
- [`loot_table/gui/page/1000/9.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1000/9.json)

## `page/1100`

- [`loot_table/gui/page/1100/0.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1100/0.json)
- [`loot_table/gui/page/1100/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/1100/1.json)

## `page/3000`

- [`loot_table/gui/page/3000/0.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/0.json)
- [`loot_table/gui/page/3000/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/1.json)
- [`loot_table/gui/page/3000/10.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/10.json)
- [`loot_table/gui/page/3000/11.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/11.json)
- [`loot_table/gui/page/3000/12.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/12.json)
- [`loot_table/gui/page/3000/13.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/13.json)
- [`loot_table/gui/page/3000/14.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/14.json)
- [`loot_table/gui/page/3000/15.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/15.json)
- [`loot_table/gui/page/3000/16.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/16.json)
- [`loot_table/gui/page/3000/17.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/17.json)
- [`loot_table/gui/page/3000/19.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/19.json)
- [`loot_table/gui/page/3000/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/2.json)
- [`loot_table/gui/page/3000/20.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/20.json)
- [`loot_table/gui/page/3000/21.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/21.json)
- [`loot_table/gui/page/3000/22.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/22.json)
- [`loot_table/gui/page/3000/23.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/23.json)
- [`loot_table/gui/page/3000/3.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/3.json)
- [`loot_table/gui/page/3000/4.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/4.json)
- [`loot_table/gui/page/3000/5.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/5.json)
- [`loot_table/gui/page/3000/6.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/6.json)
- [`loot_table/gui/page/3000/7.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/7.json)
- [`loot_table/gui/page/3000/8.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/8.json)
- [`loot_table/gui/page/3000/9.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/3000/9.json)

## `page/4000`

- [`loot_table/gui/page/4000/0.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/4000/0.json)
- [`loot_table/gui/page/4000/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/4000/1.json)
- [`loot_table/gui/page/4000/10.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/4000/10.json)
- [`loot_table/gui/page/4000/11.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/4000/11.json)
- [`loot_table/gui/page/4000/12.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/4000/12.json)
- [`loot_table/gui/page/4000/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/4000/2.json)
- [`loot_table/gui/page/4000/3.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/4000/3.json)
- [`loot_table/gui/page/4000/4.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/4000/4.json)
- [`loot_table/gui/page/4000/5.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/4000/5.json)
- [`loot_table/gui/page/4000/6.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/4000/6.json)
- [`loot_table/gui/page/4000/7.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/4000/7.json)
- [`loot_table/gui/page/4000/8.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/4000/8.json)
- [`loot_table/gui/page/4000/9.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/4000/9.json)

## `page/5000`

- [`loot_table/gui/page/5000/0.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/5000/0.json)
- [`loot_table/gui/page/5000/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/5000/1.json)
- [`loot_table/gui/page/5000/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/5000/2.json)
- [`loot_table/gui/page/5000/3.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/5000/3.json)
- [`loot_table/gui/page/5000/4.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/5000/4.json)
- [`loot_table/gui/page/5000/4_halloween.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/5000/4_halloween.json)

## `page/6000`

- [`loot_table/gui/page/6000/0.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/0.json)
- [`loot_table/gui/page/6000/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/1.json)
- [`loot_table/gui/page/6000/10.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/10.json)
- [`loot_table/gui/page/6000/11.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/11.json)
- [`loot_table/gui/page/6000/12.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/12.json)
- [`loot_table/gui/page/6000/12_halloween.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/12_halloween.json)
- [`loot_table/gui/page/6000/13.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/13.json)
- [`loot_table/gui/page/6000/13_halloween.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/13_halloween.json)
- [`loot_table/gui/page/6000/14.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/14.json)
- [`loot_table/gui/page/6000/14_halloween.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/14_halloween.json)
- [`loot_table/gui/page/6000/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/2.json)
- [`loot_table/gui/page/6000/3.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/3.json)
- [`loot_table/gui/page/6000/4.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/4.json)
- [`loot_table/gui/page/6000/5.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/5.json)
- [`loot_table/gui/page/6000/6.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/6.json)
- [`loot_table/gui/page/6000/7.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/7.json)
- [`loot_table/gui/page/6000/8.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/8.json)
- [`loot_table/gui/page/6000/9.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/6000/9.json)

## `page/7000`

- [`loot_table/gui/page/7000/10.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7000/10.json)
- [`loot_table/gui/page/7000/12.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7000/12.json)
- [`loot_table/gui/page/7000/14.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7000/14.json)
- [`loot_table/gui/page/7000/16.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7000/16.json)
- [`loot_table/gui/page/7000/4.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7000/4.json)

## `page/7200`

- [`loot_table/gui/page/7200/0.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7200/0.json)

## `page/7300`

- [`loot_table/gui/page/7300/0.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/0.json)
- [`loot_table/gui/page/7300/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/1.json)
- [`loot_table/gui/page/7300/10.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/10.json)
- [`loot_table/gui/page/7300/11.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/11.json)
- [`loot_table/gui/page/7300/12.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/12.json)
- [`loot_table/gui/page/7300/13.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/13.json)
- [`loot_table/gui/page/7300/14.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/14.json)
- [`loot_table/gui/page/7300/15.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/15.json)
- [`loot_table/gui/page/7300/16.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/16.json)
- [`loot_table/gui/page/7300/17.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/17.json)
- [`loot_table/gui/page/7300/18.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/18.json)
- [`loot_table/gui/page/7300/19.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/19.json)
- [`loot_table/gui/page/7300/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/2.json)
- [`loot_table/gui/page/7300/20.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/20.json)
- [`loot_table/gui/page/7300/21.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/21.json)
- [`loot_table/gui/page/7300/22.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/22.json)
- [`loot_table/gui/page/7300/23.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/23.json)
- [`loot_table/gui/page/7300/24.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/24.json)
- [`loot_table/gui/page/7300/25.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/25.json)
- [`loot_table/gui/page/7300/26.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/26.json)
- [`loot_table/gui/page/7300/27.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/27.json)
- [`loot_table/gui/page/7300/28.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/28.json)
- [`loot_table/gui/page/7300/3.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/3.json)
- [`loot_table/gui/page/7300/4.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/4.json)
- [`loot_table/gui/page/7300/5.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/5.json)
- [`loot_table/gui/page/7300/6.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/6.json)
- [`loot_table/gui/page/7300/7.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/7.json)
- [`loot_table/gui/page/7300/8.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/8.json)
- [`loot_table/gui/page/7300/9.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7300/9.json)

## `page/7400`

- [`loot_table/gui/page/7400/0.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7400/0.json)
- [`loot_table/gui/page/7400/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7400/1.json)

## `page/7500`

- [`loot_table/gui/page/7500/0.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7500/0.json)
- [`loot_table/gui/page/7500/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7500/1.json)
- [`loot_table/gui/page/7500/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/page/7500/2.json)

## `loot_table/gui`

- [`loot_table/gui/reset.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/reset.json)

## `roll/case0`

- [`loot_table/gui/roll/case0/main.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case0/main.json)

## `case1/a`

- [`loot_table/gui/roll/case1/a/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/a/1.json)
- [`loot_table/gui/roll/case1/a/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/a/2.json)
- [`loot_table/gui/roll/case1/a/3.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/a/3.json)
- [`loot_table/gui/roll/case1/a/4.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/a/4.json)
- [`loot_table/gui/roll/case1/a/5.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/a/5.json)
- [`loot_table/gui/roll/case1/a/6.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/a/6.json)
- [`loot_table/gui/roll/case1/a/7.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/a/7.json)
- [`loot_table/gui/roll/case1/a/main.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/a/main.json)

## `case1/b`

- [`loot_table/gui/roll/case1/b/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/b/1.json)
- [`loot_table/gui/roll/case1/b/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/b/2.json)
- [`loot_table/gui/roll/case1/b/3.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/b/3.json)
- [`loot_table/gui/roll/case1/b/4.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/b/4.json)
- [`loot_table/gui/roll/case1/b/5.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/b/5.json)
- [`loot_table/gui/roll/case1/b/main.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/b/main.json)

## `case1/c`

- [`loot_table/gui/roll/case1/c/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/c/1.json)
- [`loot_table/gui/roll/case1/c/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/c/2.json)
- [`loot_table/gui/roll/case1/c/3.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/c/3.json)
- [`loot_table/gui/roll/case1/c/4.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/c/4.json)
- [`loot_table/gui/roll/case1/c/5.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/c/5.json)
- [`loot_table/gui/roll/case1/c/main.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/c/main.json)

## `case1/d`

- [`loot_table/gui/roll/case1/d/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/d/1.json)
- [`loot_table/gui/roll/case1/d/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/d/2.json)
- [`loot_table/gui/roll/case1/d/3.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/d/3.json)
- [`loot_table/gui/roll/case1/d/main.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/d/main.json)

## `roll/case1`

- [`loot_table/gui/roll/case1/select.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case1/select.json)

## `case2/a`

- [`loot_table/gui/roll/case2/a/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case2/a/1.json)
- [`loot_table/gui/roll/case2/a/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case2/a/2.json)
- [`loot_table/gui/roll/case2/a/3.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case2/a/3.json)
- [`loot_table/gui/roll/case2/a/4.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case2/a/4.json)
- [`loot_table/gui/roll/case2/a/main.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case2/a/main.json)

## `case2/b`

- [`loot_table/gui/roll/case2/b/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case2/b/1.json)
- [`loot_table/gui/roll/case2/b/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case2/b/2.json)
- [`loot_table/gui/roll/case2/b/3.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case2/b/3.json)
- [`loot_table/gui/roll/case2/b/main.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case2/b/main.json)

## `case2/c`

- [`loot_table/gui/roll/case2/c/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case2/c/1.json)
- [`loot_table/gui/roll/case2/c/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case2/c/2.json)
- [`loot_table/gui/roll/case2/c/main.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case2/c/main.json)

## `case2/d`

- [`loot_table/gui/roll/case2/d/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case2/d/1.json)
- [`loot_table/gui/roll/case2/d/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case2/d/2.json)
- [`loot_table/gui/roll/case2/d/main.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case2/d/main.json)

## `roll/case2`

- [`loot_table/gui/roll/case2/select.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case2/select.json)

## `bars/a`

- [`loot_table/gui/roll/case3/bars/a/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/a/1.json)
- [`loot_table/gui/roll/case3/bars/a/10.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/a/10.json)
- [`loot_table/gui/roll/case3/bars/a/11.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/a/11.json)
- [`loot_table/gui/roll/case3/bars/a/12.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/a/12.json)
- [`loot_table/gui/roll/case3/bars/a/13.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/a/13.json)
- [`loot_table/gui/roll/case3/bars/a/14.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/a/14.json)
- [`loot_table/gui/roll/case3/bars/a/15.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/a/15.json)
- [`loot_table/gui/roll/case3/bars/a/16.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/a/16.json)
- [`loot_table/gui/roll/case3/bars/a/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/a/2.json)
- [`loot_table/gui/roll/case3/bars/a/3.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/a/3.json)
- [`loot_table/gui/roll/case3/bars/a/4.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/a/4.json)
- [`loot_table/gui/roll/case3/bars/a/5.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/a/5.json)
- [`loot_table/gui/roll/case3/bars/a/6.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/a/6.json)
- [`loot_table/gui/roll/case3/bars/a/7.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/a/7.json)
- [`loot_table/gui/roll/case3/bars/a/8.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/a/8.json)
- [`loot_table/gui/roll/case3/bars/a/9.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/a/9.json)
- [`loot_table/gui/roll/case3/bars/a/main.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/a/main.json)

## `bars/c`

- [`loot_table/gui/roll/case3/bars/c/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/c/1.json)
- [`loot_table/gui/roll/case3/bars/c/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/c/2.json)
- [`loot_table/gui/roll/case3/bars/c/3.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/c/3.json)
- [`loot_table/gui/roll/case3/bars/c/main.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/c/main.json)

## `bars/d`

- [`loot_table/gui/roll/case3/bars/d/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/d/1.json)
- [`loot_table/gui/roll/case3/bars/d/main.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/d/main.json)

## `case3/bars`

- [`loot_table/gui/roll/case3/bars/select.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/bars/select.json)

## `roll/case3`

- [`loot_table/gui/roll/case3/select.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/select.json)

## `sounds/a`

- [`loot_table/gui/roll/case3/sounds/a/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/a/1.json)
- [`loot_table/gui/roll/case3/sounds/a/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/a/2.json)
- [`loot_table/gui/roll/case3/sounds/a/3.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/a/3.json)
- [`loot_table/gui/roll/case3/sounds/a/4.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/a/4.json)
- [`loot_table/gui/roll/case3/sounds/a/5.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/a/5.json)
- [`loot_table/gui/roll/case3/sounds/a/6.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/a/6.json)
- [`loot_table/gui/roll/case3/sounds/a/7.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/a/7.json)
- [`loot_table/gui/roll/case3/sounds/a/main.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/a/main.json)

## `sounds/b`

- [`loot_table/gui/roll/case3/sounds/b/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/b/1.json)
- [`loot_table/gui/roll/case3/sounds/b/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/b/2.json)
- [`loot_table/gui/roll/case3/sounds/b/3.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/b/3.json)
- [`loot_table/gui/roll/case3/sounds/b/main.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/b/main.json)

## `sounds/c`

- [`loot_table/gui/roll/case3/sounds/c/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/c/1.json)
- [`loot_table/gui/roll/case3/sounds/c/main.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/c/main.json)

## `sounds/d`

- [`loot_table/gui/roll/case3/sounds/d/1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/d/1.json)
- [`loot_table/gui/roll/case3/sounds/d/2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/d/2.json)
- [`loot_table/gui/roll/case3/sounds/d/main.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/d/main.json)

## `case3/sounds`

- [`loot_table/gui/roll/case3/sounds/select.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case3/sounds/select.json)

## `roll/case4`

- [`loot_table/gui/roll/case4/common.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case4/common.json)
- [`loot_table/gui/roll/case4/epic.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case4/epic.json)
- [`loot_table/gui/roll/case4/legendary.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case4/legendary.json)
- [`loot_table/gui/roll/case4/rare.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case4/rare.json)
- [`loot_table/gui/roll/case4/select.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/case4/select.json)

## `roll/coins`

- [`loot_table/gui/roll/coins/a.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/coins/a.json)
- [`loot_table/gui/roll/coins/b.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/coins/b.json)
- [`loot_table/gui/roll/coins/c.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/coins/c.json)
- [`loot_table/gui/roll/coins/d.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/coins/d.json)
- [`loot_table/gui/roll/coins/main.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/coins/main.json)

## `roll/have`

- [`loot_table/gui/roll/have/a.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/have/a.json)
- [`loot_table/gui/roll/have/a_m.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/have/a_m.json)
- [`loot_table/gui/roll/have/b.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/have/b.json)
- [`loot_table/gui/roll/have/b_m.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/have/b_m.json)
- [`loot_table/gui/roll/have/c.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/have/c.json)
- [`loot_table/gui/roll/have/c_m.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/have/c_m.json)
- [`loot_table/gui/roll/have/d.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/have/d.json)
- [`loot_table/gui/roll/have/d_m.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/gui/roll/have/d_m.json)

## `predicate/gui`

- [`predicate/gui/0.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/0.json)
- [`predicate/gui/slot0.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot0.json)
- [`predicate/gui/slot1.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot1.json)
- [`predicate/gui/slot10.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot10.json)
- [`predicate/gui/slot11.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot11.json)
- [`predicate/gui/slot12.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot12.json)
- [`predicate/gui/slot13.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot13.json)
- [`predicate/gui/slot14.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot14.json)
- [`predicate/gui/slot15.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot15.json)
- [`predicate/gui/slot16.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot16.json)
- [`predicate/gui/slot17.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot17.json)
- [`predicate/gui/slot18.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot18.json)
- [`predicate/gui/slot19.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot19.json)
- [`predicate/gui/slot2.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot2.json)
- [`predicate/gui/slot20.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot20.json)
- [`predicate/gui/slot21.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot21.json)
- [`predicate/gui/slot22.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot22.json)
- [`predicate/gui/slot23.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot23.json)
- [`predicate/gui/slot24.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot24.json)
- [`predicate/gui/slot25.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot25.json)
- [`predicate/gui/slot26.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot26.json)
- [`predicate/gui/slot3.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot3.json)
- [`predicate/gui/slot4.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot4.json)
- [`predicate/gui/slot5.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot5.json)
- [`predicate/gui/slot6.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot6.json)
- [`predicate/gui/slot7.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot7.json)
- [`predicate/gui/slot8.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot8.json)
- [`predicate/gui/slot9.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/slot9.json)
- [`predicate/gui/use.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/gui/use.json)
