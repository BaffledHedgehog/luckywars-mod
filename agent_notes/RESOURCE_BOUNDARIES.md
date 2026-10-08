# Границы набора datapack

Ниже перечислены точные функции, которые вызываются исполняемыми строками или advancement rewards, но не имеют определения в LBC, maps и swarogskywars. Это не доказывает отсутствие ресурса во всех пакетах работающего сервера. Дальнейшее поведение нельзя восстанавливать по одному имени.

Команды вызывающей функции до/после такого вызова описываются по имеющимся исходникам. Не заполняем отсутствующие обработчики предполагаемой реализацией. Макросы рассматриваются отдельно: конкретный ID определяется переданными данными.

Проверка использует токены команды, сохраняя JSON/SNBT/селекторы и текстовые аргументы целиком. Слово function в lore не считается вызовом. Inline predicate/item modifier не считается отсутствующим файлом. NBT, chat click actions и автоматически составленные ID требуют отдельных исследований.

Машинный отчёт: [resource_reference_audit.json](inventory/resource_reference_audit.json).
`audit_references.py` и `document_boundaries.py` оставлены как исторические
генераторы; после переноса документации их не запускают поверх ручных правок.
Поддерживаемые проверки перечислены в [README.md](README.md).

## Отсутствующие определения функций

| Точный ID | Известные вызывающие источники |
|---|---|
| `lbc:bedwars/game/setup_team_at_player` | [bedwars/game/start_player_tp.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/start_player_tp.mcfunction) |
| `lbc:items/misc/huynya/clone` | [items/misc/huynya/at_2at_22.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/misc/huynya/at_2at_22.mcfunction) |
| `lbc:items/on_death_survival` | [items/on_death.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/on_death.mcfunction) |
| `lbc:workingitems/icesword/0` | [workingitems/icesword_player.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/icesword_player.mcfunction) |
| `lbc:workingitems/icesword/1` | [workingitems/icesword_player.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/icesword_player.mcfunction) |
| `lbc:workingitems/icesword/10` | [workingitems/icesword_player.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/icesword_player.mcfunction) |
| `lbc:workingitems/icesword/2` | [workingitems/icesword_player.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/icesword_player.mcfunction) |
| `lbc:workingitems/icesword/3` | [workingitems/icesword_player.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/icesword_player.mcfunction) |
| `lbc:workingitems/icesword/4` | [workingitems/icesword_player.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/icesword_player.mcfunction) |
| `lbc:workingitems/icesword/5` | [workingitems/icesword_player.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/icesword_player.mcfunction) |
| `lbc:workingitems/icesword/6` | [workingitems/icesword_player.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/icesword_player.mcfunction) |
| `lbc:workingitems/icesword/7` | [workingitems/icesword_player.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/icesword_player.mcfunction) |
| `lbc:workingitems/icesword/8` | [workingitems/icesword_player.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/icesword_player.mcfunction) |
| `lbc:workingitems/icesword/9` | [workingitems/icesword_player.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/icesword_player.mcfunction) |
| `lbcr:raycast_loop8_upgraded_left` | [raycast_loop8cd_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop8cd_upgraded_left.mcfunction) |
| `swrg:gui/fill` | [gui/page.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/page.mcfunction) |
| `swrg:maploader/map/65` | [gui/select/2200_mapselect.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2200_mapselect.mcfunction); [maploader/map_select_white2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_white2.mcfunction) |
| `swrg:maploader/map/65_broken` | [maploader/map_select_imprinted2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_imprinted2.mcfunction) |
| `swrg:maploader/map/66` | [gui/select/2200_mapselect.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2200_mapselect.mcfunction); [maploader/map_select_white2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_white2.mcfunction) |
| `swrg:maploader/map/66_broken` | [maploader/map_select_imprinted2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_imprinted2.mcfunction) |
| `swrg:maploader/map/67` | [gui/select/2200_mapselect.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2200_mapselect.mcfunction); [maploader/map_select_white2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_white2.mcfunction) |
| `swrg:maploader/map/67_broken` | [maploader/map_select_imprinted2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_imprinted2.mcfunction) |
| `swrg:maploader/map/68` | [gui/select/2200_mapselect.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2200_mapselect.mcfunction); [maploader/map_select_white2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_white2.mcfunction) |
| `swrg:maploader/map/68_broken` | [maploader/map_select_imprinted2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_imprinted2.mcfunction) |
| `swrg:maploader/map/69` | [gui/select/2200_mapselect.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2200_mapselect.mcfunction); [maploader/map_select_white2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_white2.mcfunction) |
| `swrg:maploader/map/69_broken` | [maploader/map_select_imprinted2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_imprinted2.mcfunction) |
| `swrg:maploader/map/70` | [gui/select/2200_mapselect.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2200_mapselect.mcfunction); [maploader/map_select_white2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_white2.mcfunction) |
| `swrg:maploader/map/70_broken` | [maploader/map_select_imprinted2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_imprinted2.mcfunction) |
| `swrg:maploader/map/71` | [gui/select/2200_mapselect.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2200_mapselect.mcfunction); [maploader/map_select_white2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_white2.mcfunction) |
| `swrg:maploader/map/71_broken` | [maploader/map_select_imprinted2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_imprinted2.mcfunction) |
| `swrg:maploader/map/72` | [gui/select/2200_mapselect.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2200_mapselect.mcfunction); [maploader/map_select_white2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_white2.mcfunction) |
| `swrg:maploader/map/72_broken` | [maploader/map_select_imprinted2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_imprinted2.mcfunction) |
| `swrg:maploader/map/73` | [gui/select/2200_mapselect.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2200_mapselect.mcfunction); [maploader/map_select_white2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_white2.mcfunction) |
| `swrg:maploader/map/73_broken` | [maploader/map_select_imprinted2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_imprinted2.mcfunction) |
| `swrg:maploader/map/74` | [gui/select/2200_mapselect.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2200_mapselect.mcfunction); [maploader/map_select_white2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_white2.mcfunction) |
| `swrg:maploader/map/74_broken` | [maploader/map_select_imprinted2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_imprinted2.mcfunction) |
| `swrg:maploader/map/75` | [gui/select/2200_mapselect.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2200_mapselect.mcfunction); [maploader/map_select_white2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_white2.mcfunction) |
| `swrg:maploader/map/75_broken` | [maploader/map_select_imprinted2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/map_select_imprinted2.mcfunction) |
| `swrg:maploader/map/76` | [gui/select/2200_mapselect.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/2200_mapselect.mcfunction) |

## Динамические вызовы

Само выражение не является отсутствующей функцией. Нужно проследить источник каждого макрополя и диапазон разрешённых значений. Это делают владельцы предметов/GUI/навыков.

| Выражение | Исходники |
|---|---|
| `$(tmpf)` | [items/trinket_effects/func_with_chance_universal.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/func_with_chance_universal.mcfunction) |
| `lbc:other/imba/cunny_slab/pasta/$(t)` | [other/imba/cunny_slab/correction_cunny_uoh_pasta_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/correction_cunny_uoh_pasta_select.mcfunction) |
| `lbc:swrg_kit_integration/game/skills/blocks/$(tmp)` | [swrg_kit_integration/game/skills/place_random_block_storage.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/place_random_block_storage.mcfunction) |
| `lbc:swrg_kit_integration/lang_$(lang)` | [load_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/load_2.mcfunction) |
| `swrg:maploader/map/$(quickstart)` | [game/quickstart/2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/quickstart/2.mcfunction); [game/quickstart/2_instant.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/quickstart/2_instant.mcfunction) |
| `swrg:maploader/map/$(quickstart)_broken` | [game/quickstart/2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/quickstart/2.mcfunction); [game/quickstart/2_instant.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/quickstart/2_instant.mcfunction) |
| `swrg:maploader/map/$(quickstart)b` | [game/quickstart/2_bedwars.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/quickstart/2_bedwars.mcfunction) |
| `swrg:maploader/map/$(quickstart)b_broken` | [game/quickstart/2_bedwars.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/quickstart/2_bedwars.mcfunction) |

## Шаблоны структур

Статический аудит команд нашёл 2753 разных template ID без одноимённого NBT в трёх исследуемых пакетах. Среди них есть карты и дополнительные структуры LBC. Пофайловое сопоставление, состав карт, подпапки и вызывающие функции находятся в [maps_catalog.md](maps_catalog.md) и [maps_structures.md](maps_structures.md).

Шаблоны могут храниться отдельно, например в generated-данных мира или другом datapack. Такое возможное происхождение не является подтверждением их наличия. Исследование фиксирует именно границы заданного набора.
