# Выходы разборки предметов LBC

Для каждой таблицы ниже указаны все пулы, число выборов, основания предметов, веса, условия и изменения компонентов. Это поведение выдачи: способ активации предмета ищите в описании соответствующей функции. Название/lore записано как текст подсказки и само по себе не исполняет способность.

При генерации обрабатываются все пулы. В обычном пуле один выбор берёт одну прошедшую условия запись по весу; `rolls` задаёт число выборов, `bonus_rolls` — добавку с учётом luck. Вес и quality — исходные параметры, а не конечный процент: вложенные таблицы, условия, luck и число выборов меняют результат. Изменения применяются по порядку: запись, затем её пул, затем таблица. Изначальный item-стек имеет одну единицу; явный `set_count` меняет количество, а `add:true` прибавляет. Компоненты, не изменённые таблицей, сохраняют defaults ванильной основы.

Все имена ресурсов и численные параметры сохранены. Вложенные таблицы и команды, содержащие тот же ID, указаны ссылками; список буквальных ссылок не исключает макросы и ссылки внутри NBT. NBT-контейнеры изучены в `maps_catalog.md`.

## `lbc:uncrafted/3_copper_nugget`

Источник: [lbc:uncrafted/3_copper_nugget](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/3_copper_nugget.json).
Пул 1: выборов `3`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_nugget`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/4_soul_sand_3_wither_head`

Источник: [lbc:uncrafted/4_soul_sand_3_wither_head](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/4_soul_sand_3_wither_head.json).
Пул 1: выборов `4`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:soul_sand`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `3`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:wither_skeleton_skull`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/4_wind_charge`

Источник: [lbc:uncrafted/4_wind_charge](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/4_wind_charge.json).
Пул 1: выборов `4`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:wind_charge`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/8_ghast_tear_soul_sand`

Источник: [lbc:uncrafted/8_ghast_tear_soul_sand](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/8_ghast_tear_soul_sand.json).
Пул 1: выборов `8`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:ghast_tear`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:soul_sand`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/9_copper_nugget`

Источник: [lbc:uncrafted/9_copper_nugget](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/9_copper_nugget.json).
Пул 1: выборов `9`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_nugget`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/acacia_boat_chest`

Источник: [lbc:uncrafted/acacia_boat_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/acacia_boat_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:acacia_boat`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/acacia_planks`

Источник: [lbc:uncrafted/acacia_planks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/acacia_planks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:acacia_planks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/acacia_planks_2`

Источник: [lbc:uncrafted/acacia_planks_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/acacia_planks_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:acacia_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/acacia_planks_2_stick_4`

Источник: [lbc:uncrafted/acacia_planks_2_stick_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/acacia_planks_2_stick_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:acacia_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/acacia_planks_3`

Источник: [lbc:uncrafted/acacia_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/acacia_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:acacia_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/acacia_planks_4_iron_ingot_2`

Источник: [lbc:uncrafted/acacia_planks_4_iron_ingot_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/acacia_planks_4_iron_ingot_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:acacia_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/acacia_planks_5`

Источник: [lbc:uncrafted/acacia_planks_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/acacia_planks_5.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:acacia_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/allium`

Источник: [lbc:uncrafted/allium](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/allium.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:allium`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/amethyst_shard_3_sculk_sensor`

Источник: [lbc:uncrafted/amethyst_shard_3_sculk_sensor](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/amethyst_shard_3_sculk_sensor.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:amethyst_shard`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:sculk_sensor`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/amethyst_shard_4`

Источник: [lbc:uncrafted/amethyst_shard_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/amethyst_shard_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:amethyst_shard`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/ancient_debris`

Источник: [lbc:uncrafted/ancient_debris](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/ancient_debris.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:ancient_debris`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/andesite`

Источник: [lbc:uncrafted/andesite](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/andesite.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:andesite`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/armadillo_scute_6`

Источник: [lbc:uncrafted/armadillo_scute_6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/armadillo_scute_6.json).
Пул 1: выборов `6`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:armadillo_scute`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bamboo_2`

Источник: [lbc:uncrafted/bamboo_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bamboo_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bamboo`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bamboo_6`

Источник: [lbc:uncrafted/bamboo_6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bamboo_6.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bamboo`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bamboo_planks`

Источник: [lbc:uncrafted/bamboo_planks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bamboo_planks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bamboo_planks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bamboo_planks_2`

Источник: [lbc:uncrafted/bamboo_planks_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bamboo_planks_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bamboo_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bamboo_planks_2_stick_4`

Источник: [lbc:uncrafted/bamboo_planks_2_stick_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bamboo_planks_2_stick_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bamboo_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bamboo_planks_3`

Источник: [lbc:uncrafted/bamboo_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bamboo_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bamboo_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bamboo_planks_5`

Источник: [lbc:uncrafted/bamboo_planks_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bamboo_planks_5.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bamboo_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bamboo_raft_chest`

Источник: [lbc:uncrafted/bamboo_raft_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bamboo_raft_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bamboo_raft`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bamboo_slab_2`

Источник: [lbc:uncrafted/bamboo_slab_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bamboo_slab_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bamboo_slab`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/basalt`

Источник: [lbc:uncrafted/basalt](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/basalt.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:basalt`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/beef`

Источник: [lbc:uncrafted/beef](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/beef.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:beef`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/beetroot_6_bowl`

Источник: [lbc:uncrafted/beetroot_6_bowl](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/beetroot_6_bowl.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:beetroot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bowl`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/beetroot_seeds`

Источник: [lbc:uncrafted/beetroot_seeds](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/beetroot_seeds.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:beetroot_seeds`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/birch_boat_chest`

Источник: [lbc:uncrafted/birch_boat_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/birch_boat_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:birch_boat`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/birch_planks`

Источник: [lbc:uncrafted/birch_planks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/birch_planks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:birch_planks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/birch_planks_2`

Источник: [lbc:uncrafted/birch_planks_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/birch_planks_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:birch_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/birch_planks_2_stick_4`

Источник: [lbc:uncrafted/birch_planks_2_stick_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/birch_planks_2_stick_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:birch_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/birch_planks_3`

Источник: [lbc:uncrafted/birch_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/birch_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:birch_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/birch_planks_4_flint_2`

Источник: [lbc:uncrafted/birch_planks_4_flint_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/birch_planks_4_flint_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:birch_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:flint`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/birch_planks_5`

Источник: [lbc:uncrafted/birch_planks_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/birch_planks_5.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:birch_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/black_dye_ingridient`

Источник: [lbc:uncrafted/black_dye_ingridient](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/black_dye_ingridient.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:ink_sac`. Вес `1`, добавка за luck `0`.
- 2: Выдать ванильную основу `minecraft:wither_rose`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/black_wool_3_oak_planks_3`

Источник: [lbc:uncrafted/black_wool_3_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/black_wool_3_oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:black_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/black_wool_6_stick`

Источник: [lbc:uncrafted/black_wool_6_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/black_wool_6_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:black_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/blackstone`

Источник: [lbc:uncrafted/blackstone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/blackstone.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:blackstone`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/blue_dye_ingridient`

Источник: [lbc:uncrafted/blue_dye_ingridient](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/blue_dye_ingridient.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:lapis_lazuli`. Вес `1`, добавка за luck `0`.
- 2: Выдать ванильную основу `minecraft:cornflower`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/blue_orchid`

Источник: [lbc:uncrafted/blue_orchid](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/blue_orchid.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:blue_orchid`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/blue_wool_3_oak_planks_3`

Источник: [lbc:uncrafted/blue_wool_3_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/blue_wool_3_oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:blue_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/blue_wool_6_stick`

Источник: [lbc:uncrafted/blue_wool_6_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/blue_wool_6_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:blue_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bone_meal`

Источник: [lbc:uncrafted/bone_meal](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bone_meal.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bone_meal`. Вес `1`, добавка за luck `0`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/bone_meal_9`

Источник: [lbc:uncrafted/bone_meal_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bone_meal_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bone_meal`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/book_ink_sac_feather`

Источник: [lbc:uncrafted/book_ink_sac_feather](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/book_ink_sac_feather.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:book`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:ink_sac`. Вес `1`, добавка за luck `0`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:feather`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bowl_cooked_rabbit_baked_potato_carrot_brown_mushroom`

Источник: [lbc:uncrafted/bowl_cooked_rabbit_baked_potato_carrot_brown_mushroom](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bowl_cooked_rabbit_baked_potato_carrot_brown_mushroom.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bowl`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cooked_rabbit`. Вес `1`, добавка за luck `0`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:baked_potato`. Вес `1`, добавка за luck `0`.
Пул 4: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:carrot`. Вес `1`, добавка за luck `0`.
Пул 5: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_mushroom`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bowl_red_mushroom_brown_mushroom`

Источник: [lbc:uncrafted/bowl_red_mushroom_brown_mushroom](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bowl_red_mushroom_brown_mushroom.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bowl`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_mushroom`. Вес `1`, добавка за luck `0`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_mushroom`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bowl_red_mushroom_brown_mushroom_allium`

Источник: [lbc:uncrafted/bowl_red_mushroom_brown_mushroom_allium](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bowl_red_mushroom_brown_mushroom_allium.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bowl`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_mushroom`. Вес `1`, добавка за luck `0`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_mushroom`. Вес `1`, добавка за luck `0`.
Пул 4: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:allium`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction).

## `lbc:uncrafted/bowl_red_mushroom_brown_mushroom_azure_bluet`

Источник: [lbc:uncrafted/bowl_red_mushroom_brown_mushroom_azure_bluet](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bowl_red_mushroom_brown_mushroom_azure_bluet.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bowl`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_mushroom`. Вес `1`, добавка за luck `0`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_mushroom`. Вес `1`, добавка за luck `0`.
Пул 4: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:azure_bluet`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction).

## `lbc:uncrafted/bowl_red_mushroom_brown_mushroom_cornflower`

Источник: [lbc:uncrafted/bowl_red_mushroom_brown_mushroom_cornflower](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bowl_red_mushroom_brown_mushroom_cornflower.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bowl`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_mushroom`. Вес `1`, добавка за luck `0`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_mushroom`. Вес `1`, добавка за luck `0`.
Пул 4: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cornflower`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction).

## `lbc:uncrafted/bowl_red_mushroom_brown_mushroom_dandelion`

Источник: [lbc:uncrafted/bowl_red_mushroom_brown_mushroom_dandelion](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bowl_red_mushroom_brown_mushroom_dandelion.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bowl`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_mushroom`. Вес `1`, добавка за luck `0`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_mushroom`. Вес `1`, добавка за luck `0`.
Пул 4: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:dandelion`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction).

## `lbc:uncrafted/bowl_red_mushroom_brown_mushroom_lily_of_the_valley`

Источник: [lbc:uncrafted/bowl_red_mushroom_brown_mushroom_lily_of_the_valley](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bowl_red_mushroom_brown_mushroom_lily_of_the_valley.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bowl`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_mushroom`. Вес `1`, добавка за luck `0`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_mushroom`. Вес `1`, добавка за luck `0`.
Пул 4: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:lily_of_the_valley`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction).

## `lbc:uncrafted/bowl_red_mushroom_brown_mushroom_oxeye_daisy`

Источник: [lbc:uncrafted/bowl_red_mushroom_brown_mushroom_oxeye_daisy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bowl_red_mushroom_brown_mushroom_oxeye_daisy.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bowl`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_mushroom`. Вес `1`, добавка за luck `0`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_mushroom`. Вес `1`, добавка за luck `0`.
Пул 4: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxeye_daisy`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction).

## `lbc:uncrafted/bowl_red_mushroom_brown_mushroom_pink_tulip`

Источник: [lbc:uncrafted/bowl_red_mushroom_brown_mushroom_pink_tulip](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bowl_red_mushroom_brown_mushroom_pink_tulip.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bowl`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_mushroom`. Вес `1`, добавка за luck `0`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_mushroom`. Вес `1`, добавка за luck `0`.
Пул 4: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pink_tulip`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction).

## `lbc:uncrafted/bowl_red_mushroom_brown_mushroom_poppy`

Источник: [lbc:uncrafted/bowl_red_mushroom_brown_mushroom_poppy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bowl_red_mushroom_brown_mushroom_poppy.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bowl`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_mushroom`. Вес `1`, добавка за luck `0`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_mushroom`. Вес `1`, добавка за luck `0`.
Пул 4: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:poppy`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction).

## `lbc:uncrafted/bowl_red_mushroom_brown_mushroom_wither_rose`

Источник: [lbc:uncrafted/bowl_red_mushroom_brown_mushroom_wither_rose](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bowl_red_mushroom_brown_mushroom_wither_rose.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bowl`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_mushroom`. Вес `1`, добавка за luck `0`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_mushroom`. Вес `1`, добавка за luck `0`.
Пул 4: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:wither_rose`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/susp_stews.mcfunction).

## `lbc:uncrafted/brick_2`

Источник: [lbc:uncrafted/brick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/brick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/brick_3`

Источник: [lbc:uncrafted/brick_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/brick_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/brick_4`

Источник: [lbc:uncrafted/brick_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/brick_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/brick_6`

Источник: [lbc:uncrafted/brick_6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/brick_6.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bricks`

Источник: [lbc:uncrafted/bricks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bricks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bricks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/brown_wool_3_oak_planks_3`

Источник: [lbc:uncrafted/brown_wool_3_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/brown_wool_3_oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/brown_wool_6_stick`

Источник: [lbc:uncrafted/brown_wool_6_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/brown_wool_6_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bundle_black_dye`

Источник: [lbc:uncrafted/bundle_black_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bundle_black_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bundle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:black_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bundle_blue_dye`

Источник: [lbc:uncrafted/bundle_blue_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bundle_blue_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bundle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:blue_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bundle_brown_dye`

Источник: [lbc:uncrafted/bundle_brown_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bundle_brown_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bundle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bundle_cyan_dye`

Источник: [lbc:uncrafted/bundle_cyan_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bundle_cyan_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bundle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cyan_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bundle_gray_dye`

Источник: [lbc:uncrafted/bundle_gray_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bundle_gray_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bundle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gray_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bundle_green_dye`

Источник: [lbc:uncrafted/bundle_green_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bundle_green_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bundle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:green_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bundle_light_blue_dye`

Источник: [lbc:uncrafted/bundle_light_blue_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bundle_light_blue_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bundle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:light_blue_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bundle_light_gray_dye`

Источник: [lbc:uncrafted/bundle_light_gray_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bundle_light_gray_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bundle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:light_gray_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bundle_lime_dye`

Источник: [lbc:uncrafted/bundle_lime_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bundle_lime_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bundle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:lime_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bundle_magenta_dye`

Источник: [lbc:uncrafted/bundle_magenta_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bundle_magenta_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bundle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:magenta_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bundle_orange_dye`

Источник: [lbc:uncrafted/bundle_orange_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bundle_orange_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bundle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:orange_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bundle_pink_dye`

Источник: [lbc:uncrafted/bundle_pink_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bundle_pink_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bundle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pink_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bundle_purple_dye`

Источник: [lbc:uncrafted/bundle_purple_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bundle_purple_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bundle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:purple_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bundle_red_dye`

Источник: [lbc:uncrafted/bundle_red_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bundle_red_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bundle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bundle_white_dye`

Источник: [lbc:uncrafted/bundle_white_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bundle_white_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bundle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/bundle_yellow_dye`

Источник: [lbc:uncrafted/bundle_yellow_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/bundle_yellow_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bundle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:yellow_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cactus`

Источник: [lbc:uncrafted/cactus](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cactus.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cactus`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/candle_black_dye`

Источник: [lbc:uncrafted/candle_black_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/candle_black_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:candle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:black_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/candle_blue_dye`

Источник: [lbc:uncrafted/candle_blue_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/candle_blue_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:candle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:blue_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/candle_brown_dye`

Источник: [lbc:uncrafted/candle_brown_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/candle_brown_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:candle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/candle_cyan_dye`

Источник: [lbc:uncrafted/candle_cyan_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/candle_cyan_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:candle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cyan_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/candle_gray_dye`

Источник: [lbc:uncrafted/candle_gray_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/candle_gray_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:candle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gray_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/candle_green_dye`

Источник: [lbc:uncrafted/candle_green_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/candle_green_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:candle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:green_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/candle_light_blue_dye`

Источник: [lbc:uncrafted/candle_light_blue_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/candle_light_blue_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:candle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:light_blue_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/candle_light_gray_dye`

Источник: [lbc:uncrafted/candle_light_gray_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/candle_light_gray_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:candle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:light_gray_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/candle_lime_dye`

Источник: [lbc:uncrafted/candle_lime_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/candle_lime_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:candle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:lime_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/candle_magenta_dye`

Источник: [lbc:uncrafted/candle_magenta_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/candle_magenta_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:candle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:magenta_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/candle_orange_dye`

Источник: [lbc:uncrafted/candle_orange_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/candle_orange_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:candle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:orange_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/candle_pink_dye`

Источник: [lbc:uncrafted/candle_pink_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/candle_pink_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:candle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pink_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/candle_purple_dye`

Источник: [lbc:uncrafted/candle_purple_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/candle_purple_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:candle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:purple_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/candle_red_dye`

Источник: [lbc:uncrafted/candle_red_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/candle_red_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:candle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/candle_white_dye`

Источник: [lbc:uncrafted/candle_white_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/candle_white_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:candle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/candle_yellow_dye`

Источник: [lbc:uncrafted/candle_yellow_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/candle_yellow_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:candle`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:yellow_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/carrot_on_a_stick_warped_fungus`

Источник: [lbc:uncrafted/carrot_on_a_stick_warped_fungus](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/carrot_on_a_stick_warped_fungus.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:fishing_rod`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:warped_fungus`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cherry_boat_chest`

Источник: [lbc:uncrafted/cherry_boat_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cherry_boat_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cherry_boat`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cherry_planks`

Источник: [lbc:uncrafted/cherry_planks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cherry_planks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cherry_planks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cherry_planks_2`

Источник: [lbc:uncrafted/cherry_planks_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cherry_planks_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cherry_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cherry_planks_2_stick_4`

Источник: [lbc:uncrafted/cherry_planks_2_stick_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cherry_planks_2_stick_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cherry_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cherry_planks_3`

Источник: [lbc:uncrafted/cherry_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cherry_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cherry_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cherry_planks_5`

Источник: [lbc:uncrafted/cherry_planks_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cherry_planks_5.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cherry_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/chest_8_copper_ingot`

Источник: [lbc:uncrafted/chest_8_copper_ingot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/chest_8_copper_ingot.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chest`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/chicken`

Источник: [lbc:uncrafted/chicken](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/chicken.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chicken`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/chiseled_stone_bricks_8_netherite_ingot`

Источник: [lbc:uncrafted/chiseled_stone_bricks_8_netherite_ingot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/chiseled_stone_bricks_8_netherite_ingot.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chiseled_stone_bricks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_ingot`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/chorus_fruit`

Источник: [lbc:uncrafted/chorus_fruit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/chorus_fruit.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chorus_fruit`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/clay_ball`

Источник: [lbc:uncrafted/clay_ball](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/clay_ball.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:clay_ball`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/clay_ball_4`

Источник: [lbc:uncrafted/clay_ball_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/clay_ball_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:clay_ball`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/closed_eyeblossom`

Источник: [lbc:uncrafted/closed_eyeblossom](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/closed_eyeblossom.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:closed_eyeblossom`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/coal_9`

Источник: [lbc:uncrafted/coal_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/coal_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:coal`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cobbled_deepslate`

Источник: [lbc:uncrafted/cobbled_deepslate](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cobbled_deepslate.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cobbled_deepslate`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cobbled_deepslate_slab_2`

Источник: [lbc:uncrafted/cobbled_deepslate_slab_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cobbled_deepslate_slab_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cobbled_deepslate_slab`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cobblestone`

Источник: [lbc:uncrafted/cobblestone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cobblestone.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cobblestone`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cobblestone_2_stick`

Источник: [lbc:uncrafted/cobblestone_2_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cobblestone_2_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cobblestone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cobblestone_2_stick_2`

Источник: [lbc:uncrafted/cobblestone_2_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cobblestone_2_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cobblestone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cobblestone_3_blaze_rod`

Источник: [lbc:uncrafted/cobblestone_3_blaze_rod](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cobblestone_3_blaze_rod.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cobblestone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:blaze_rod`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cobblestone_3_redstone_torch_3_quartz`

Источник: [lbc:uncrafted/cobblestone_3_redstone_torch_3_quartz](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cobblestone_3_redstone_torch_3_quartz.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cobblestone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:redstone_torch`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:quartz`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cobblestone_3_stick_2`

Источник: [lbc:uncrafted/cobblestone_3_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cobblestone_3_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cobblestone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cobblestone_4_oak_planks_3_redstone_iron_ingot`

Источник: [lbc:uncrafted/cobblestone_4_oak_planks_3_redstone_iron_ingot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cobblestone_4_oak_planks_3_redstone_iron_ingot.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cobblestone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:redstone`. Вес `1`, добавка за luck `0`.
Пул 4: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cobblestone_6_redstone_2_quartz`

Источник: [lbc:uncrafted/cobblestone_6_redstone_2_quartz](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cobblestone_6_redstone_2_quartz.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cobblestone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:redstone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:quartz`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cobblestone_7_redstone`

Источник: [lbc:uncrafted/cobblestone_7_redstone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cobblestone_7_redstone.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cobblestone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `7`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:redstone`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cobblestone_7_redstone_redstone_bow`

Источник: [lbc:uncrafted/cobblestone_7_redstone_redstone_bow](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cobblestone_7_redstone_redstone_bow.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cobblestone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `7`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:redstone`. Вес `1`, добавка за luck `0`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bow`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cobblestone_8`

Источник: [lbc:uncrafted/cobblestone_8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cobblestone_8.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cobblestone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cobblestone_moss_block`

Источник: [lbc:uncrafted/cobblestone_moss_block](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cobblestone_moss_block.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cobblestone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:moss_block`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cobblestone_quartz`

Источник: [lbc:uncrafted/cobblestone_quartz](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cobblestone_quartz.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cobblestone`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:quartz`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cobblestone_stick`

Источник: [lbc:uncrafted/cobblestone_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cobblestone_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cobblestone`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cobblestone_stick_2`

Источник: [lbc:uncrafted/cobblestone_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cobblestone_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cobblestone`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cocoa_beans`

Источник: [lbc:uncrafted/cocoa_beans](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cocoa_beans.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cocoa_beans`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cod`

Источник: [lbc:uncrafted/cod](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cod.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cod`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/copper_block`

Источник: [lbc:uncrafted/copper_block](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_block.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_block`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/copper_block_2`

Источник: [lbc:uncrafted/copper_block_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_block_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_block`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/copper_block_3`

Источник: [lbc:uncrafted/copper_block_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_block_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_block`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/copper_door_honeycomb`

Источник: [lbc:uncrafted/copper_door_honeycomb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_door_honeycomb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_door`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/copper_ingot_1_stick_2`

Источник: [lbc:uncrafted/copper_ingot_1_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_ingot_1_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/copper_ingot_2`

Источник: [lbc:uncrafted/copper_ingot_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_ingot_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/copper_ingot_2_amethyst_shard`

Источник: [lbc:uncrafted/copper_ingot_2_amethyst_shard](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_ingot_2_amethyst_shard.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:amethyst_shard`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/copper_ingot_2_copper_nugget`

Источник: [lbc:uncrafted/copper_ingot_2_copper_nugget](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_ingot_2_copper_nugget.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_nugget`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_ingot`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/copper_ingot_2_honeycomb`

Источник: [lbc:uncrafted/copper_ingot_2_honeycomb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_ingot_2_honeycomb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/copper_ingot_2_stick_1`

Источник: [lbc:uncrafted/copper_ingot_2_stick_1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_ingot_2_stick_1.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/copper_ingot_2_stick_2`

Источник: [lbc:uncrafted/copper_ingot_2_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_ingot_2_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/copper_ingot_3`

Источник: [lbc:uncrafted/copper_ingot_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_ingot_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/copper_ingot_3_stick_2`

Источник: [lbc:uncrafted/copper_ingot_3_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_ingot_3_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/copper_ingot_4`

Источник: [lbc:uncrafted/copper_ingot_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_ingot_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/copper_ingot_5`

Источник: [lbc:uncrafted/copper_ingot_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_ingot_5.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/copper_ingot_7`

Источник: [lbc:uncrafted/copper_ingot_7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_ingot_7.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `7`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/copper_ingot_8`

Источник: [lbc:uncrafted/copper_ingot_8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_ingot_8.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/copper_ingot_9`

Источник: [lbc:uncrafted/copper_ingot_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_ingot_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/copper_torch_8_copper_nugget`

Источник: [lbc:uncrafted/copper_torch_8_copper_nugget](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/copper_torch_8_copper_nugget.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_nugget`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_torch`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/creeper_head_paper`

Источник: [lbc:uncrafted/creeper_head_paper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/creeper_head_paper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:creeper_head`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:paper`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/crimson_planks`

Источник: [lbc:uncrafted/crimson_planks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/crimson_planks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:crimson_planks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/crimson_planks_2`

Источник: [lbc:uncrafted/crimson_planks_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/crimson_planks_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:crimson_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/crimson_planks_2_stick_4`

Источник: [lbc:uncrafted/crimson_planks_2_stick_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/crimson_planks_2_stick_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:crimson_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/crimson_planks_3`

Источник: [lbc:uncrafted/crimson_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/crimson_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:crimson_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/crying_obsidian_6_glowstone_3`

Источник: [lbc:uncrafted/crying_obsidian_6_glowstone_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/crying_obsidian_6_glowstone_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:crying_obsidian`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:glowstone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cyan_wool_3_oak_planks_3`

Источник: [lbc:uncrafted/cyan_wool_3_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cyan_wool_3_oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cyan_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/cyan_wool_6_stick`

Источник: [lbc:uncrafted/cyan_wool_6_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/cyan_wool_6_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cyan_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/dandelion`

Источник: [lbc:uncrafted/dandelion](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/dandelion.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:dandelion`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/dark_oak_boat_chest`

Источник: [lbc:uncrafted/dark_oak_boat_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/dark_oak_boat_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:dark_oak_boat`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/dark_oak_planks`

Источник: [lbc:uncrafted/dark_oak_planks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/dark_oak_planks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:dark_oak_planks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/dark_oak_planks_2`

Источник: [lbc:uncrafted/dark_oak_planks_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/dark_oak_planks_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:dark_oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/dark_oak_planks_2_stick_2_stone_slab`

Источник: [lbc:uncrafted/dark_oak_planks_2_stick_2_stone_slab](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/dark_oak_planks_2_stick_2_stone_slab.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:dark_oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stone_slab`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/dark_oak_planks_2_stick_4`

Источник: [lbc:uncrafted/dark_oak_planks_2_stick_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/dark_oak_planks_2_stick_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:dark_oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/dark_oak_planks_3`

Источник: [lbc:uncrafted/dark_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/dark_oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:dark_oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/dark_oak_planks_4_paper_2`

Источник: [lbc:uncrafted/dark_oak_planks_4_paper_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/dark_oak_planks_4_paper_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:dark_oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:paper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/dark_oak_planks_5`

Источник: [lbc:uncrafted/dark_oak_planks_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/dark_oak_planks_5.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:dark_oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/dark_oak_slab_3_quartz_3_glass_3`

Источник: [lbc:uncrafted/dark_oak_slab_3_quartz_3_glass_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/dark_oak_slab_3_quartz_3_glass_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:dark_oak_slab`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:quartz`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:glass`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/deepslate_bricks`

Источник: [lbc:uncrafted/deepslate_bricks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/deepslate_bricks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:deepslate_bricks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/diamond_2_stick_2`

Источник: [lbc:uncrafted/diamond_2_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/diamond_2_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/diamond_3_stick_2`

Источник: [lbc:uncrafted/diamond_3_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/diamond_3_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/diamond_4`

Источник: [lbc:uncrafted/diamond_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/diamond_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/diamond_5`

Источник: [lbc:uncrafted/diamond_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/diamond_5.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/diamond_7`

Источник: [lbc:uncrafted/diamond_7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/diamond_7.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `7`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/diamond_8`

Источник: [lbc:uncrafted/diamond_8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/diamond_8.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/diamond_9`

Источник: [lbc:uncrafted/diamond_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/diamond_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/diamond_stick_2`

Источник: [lbc:uncrafted/diamond_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/diamond_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/diorite`

Источник: [lbc:uncrafted/diorite](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/diorite.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diorite`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/disc_fragment_5_9`

Источник: [lbc:uncrafted/disc_fragment_5_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/disc_fragment_5_9.json).
Пул 1: выборов `9`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:disc_fragment_5`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/dried_kelp_9`

Источник: [lbc:uncrafted/dried_kelp_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/dried_kelp_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:dried_kelp`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/echo_shard_8_compass`

Источник: [lbc:uncrafted/echo_shard_8_compass](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/echo_shard_8_compass.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:echo_shard`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:compass`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/egg_sugar_pumpkin`

Источник: [lbc:uncrafted/egg_sugar_pumpkin](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/egg_sugar_pumpkin.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:egg`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:sugar`. Вес `1`, добавка за luck `0`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pumpkin`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/emerald_9`

Источник: [lbc:uncrafted/emerald_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/emerald_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:emerald`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/end_stone`

Источник: [lbc:uncrafted/end_stone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/end_stone.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:end_stone`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/end_stone_bricks`

Источник: [lbc:uncrafted/end_stone_bricks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/end_stone_bricks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:end_stone_bricks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/ender_pearl_blaze_powder`

Источник: [lbc:uncrafted/ender_pearl_blaze_powder](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/ender_pearl_blaze_powder.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:ender_pearl`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:blaze_powder`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/exposed_copper`

Источник: [lbc:uncrafted/exposed_copper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/exposed_copper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:exposed_copper`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/exposed_copper_2`

Источник: [lbc:uncrafted/exposed_copper_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/exposed_copper_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:exposed_copper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/exposed_copper_3`

Источник: [lbc:uncrafted/exposed_copper_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/exposed_copper_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:exposed_copper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/exposed_copper_door_honeycomb`

Источник: [lbc:uncrafted/exposed_copper_door_honeycomb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/exposed_copper_door_honeycomb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:exposed_copper_door`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/exposed_copper_grate_honeycomb`

Источник: [lbc:uncrafted/exposed_copper_grate_honeycomb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/exposed_copper_grate_honeycomb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:exposed_copper_grate`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/exposed_copper_trapdoor_honeycomb`

Источник: [lbc:uncrafted/exposed_copper_trapdoor_honeycomb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/exposed_copper_trapdoor_honeycomb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:exposed_copper_trapdoor`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/exposed_cut_copper_honeycomb`

Источник: [lbc:uncrafted/exposed_cut_copper_honeycomb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/exposed_cut_copper_honeycomb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:exposed_cut_copper`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/fishing_rod_carrot`

Источник: [lbc:uncrafted/fishing_rod_carrot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/fishing_rod_carrot.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:fishing_rod`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:carrot`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/glass`

Источник: [lbc:uncrafted/glass](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/glass.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:glass`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/glass_7_ender_eye_ghast_tear`

Источник: [lbc:uncrafted/glass_7_ender_eye_ghast_tear](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/glass_7_ender_eye_ghast_tear.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:glass`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `7`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:ender_eye`. Вес `1`, добавка за luck `0`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:ghast_tear`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/glow_ink_sac_item_frame`

Источник: [lbc:uncrafted/glow_ink_sac_item_frame](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/glow_ink_sac_item_frame.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:glow_ink_sac`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:item_frame`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gold_ingot_2`

Источник: [lbc:uncrafted/gold_ingot_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gold_ingot_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gold_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gold_ingot_2_stick`

Источник: [lbc:uncrafted/gold_ingot_2_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gold_ingot_2_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gold_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gold_ingot_2_stick_2`

Источник: [lbc:uncrafted/gold_ingot_2_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gold_ingot_2_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gold_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gold_ingot_3_stick_2`

Источник: [lbc:uncrafted/gold_ingot_3_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gold_ingot_3_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gold_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gold_ingot_4`

Источник: [lbc:uncrafted/gold_ingot_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gold_ingot_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gold_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gold_ingot_4_netherite_scrap_4`

Источник: [lbc:uncrafted/gold_ingot_4_netherite_scrap_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gold_ingot_4_netherite_scrap_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gold_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_scrap`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gold_ingot_4_redstone`

Источник: [lbc:uncrafted/gold_ingot_4_redstone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gold_ingot_4_redstone.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gold_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:redstone`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gold_ingot_5`

Источник: [lbc:uncrafted/gold_ingot_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gold_ingot_5.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gold_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gold_ingot_7`

Источник: [lbc:uncrafted/gold_ingot_7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gold_ingot_7.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gold_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `7`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gold_ingot_8`

Источник: [lbc:uncrafted/gold_ingot_8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gold_ingot_8.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gold_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gold_ingot_8_apple`

Источник: [lbc:uncrafted/gold_ingot_8_apple](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gold_ingot_8_apple.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gold_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:apple`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gold_ingot_9`

Источник: [lbc:uncrafted/gold_ingot_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gold_ingot_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gold_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gold_ingot_stick_2`

Источник: [lbc:uncrafted/gold_ingot_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gold_ingot_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gold_ingot`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gold_nugget_8_carrot`

Источник: [lbc:uncrafted/gold_nugget_8_carrot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gold_nugget_8_carrot.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gold_nugget`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:carrot`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gold_nugget_8_melon_slice`

Источник: [lbc:uncrafted/gold_nugget_8_melon_slice](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gold_nugget_8_melon_slice.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gold_nugget`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:melon_slice`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gold_nugget_9`

Источник: [lbc:uncrafted/gold_nugget_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gold_nugget_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gold_nugget`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/granite`

Источник: [lbc:uncrafted/granite](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/granite.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:granite`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gravel`

Источник: [lbc:uncrafted/gravel](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gravel.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gravel`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gray_wool_3_oak_planks_3`

Источник: [lbc:uncrafted/gray_wool_3_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gray_wool_3_oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gray_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gray_wool_6_stick`

Источник: [lbc:uncrafted/gray_wool_6_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gray_wool_6_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gray_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/green_wool_3_oak_planks_3`

Источник: [lbc:uncrafted/green_wool_3_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/green_wool_3_oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:green_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/green_wool_6_stick`

Источник: [lbc:uncrafted/green_wool_6_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/green_wool_6_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:green_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/gunpowder_5_sand_4`

Источник: [lbc:uncrafted/gunpowder_5_sand_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/gunpowder_5_sand_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gunpowder`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:sand`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/harness_black_dye`

Источник: [lbc:uncrafted/harness_black_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/harness_black_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_harness`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:black_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/harness_blue_dye`

Источник: [lbc:uncrafted/harness_blue_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/harness_blue_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_harness`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:blue_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/harness_brown_dye`

Источник: [lbc:uncrafted/harness_brown_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/harness_brown_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_harness`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/harness_cyan_dye`

Источник: [lbc:uncrafted/harness_cyan_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/harness_cyan_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_harness`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cyan_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/harness_gray_dye`

Источник: [lbc:uncrafted/harness_gray_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/harness_gray_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_harness`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gray_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/harness_green_dye`

Источник: [lbc:uncrafted/harness_green_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/harness_green_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_harness`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:green_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/harness_light_blue_dye`

Источник: [lbc:uncrafted/harness_light_blue_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/harness_light_blue_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_harness`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:light_blue_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/harness_light_gray_dye`

Источник: [lbc:uncrafted/harness_light_gray_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/harness_light_gray_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_harness`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:light_gray_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/harness_lime_dye`

Источник: [lbc:uncrafted/harness_lime_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/harness_lime_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_harness`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:lime_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/harness_magenta_dye`

Источник: [lbc:uncrafted/harness_magenta_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/harness_magenta_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_harness`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:magenta_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/harness_orange_dye`

Источник: [lbc:uncrafted/harness_orange_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/harness_orange_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_harness`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:orange_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/harness_pink_dye`

Источник: [lbc:uncrafted/harness_pink_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/harness_pink_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_harness`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pink_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/harness_purple_dye`

Источник: [lbc:uncrafted/harness_purple_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/harness_purple_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_harness`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:purple_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/harness_red_dye`

Источник: [lbc:uncrafted/harness_red_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/harness_red_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_harness`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/harness_yellow_dye`

Источник: [lbc:uncrafted/harness_yellow_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/harness_yellow_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_harness`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:yellow_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/heavy_core_breeze_rod`

Источник: [lbc:uncrafted/heavy_core_breeze_rod](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/heavy_core_breeze_rod.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:heavy_core`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:breeze_rod`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honey_bottle`

Источник: [lbc:uncrafted/honey_bottle](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honey_bottle.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honey_bottle`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_4`

Источник: [lbc:uncrafted/honeycomb_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_chiseled_copper`

Источник: [lbc:uncrafted/honeycomb_chiseled_copper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_chiseled_copper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chiseled_copper`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_copper_bars`

Источник: [lbc:uncrafted/honeycomb_copper_bars](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_copper_bars.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_bars`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_copper_block`

Источник: [lbc:uncrafted/honeycomb_copper_block](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_copper_block.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_block`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_copper_bulb`

Источник: [lbc:uncrafted/honeycomb_copper_bulb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_copper_bulb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_bulb`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_copper_chain`

Источник: [lbc:uncrafted/honeycomb_copper_chain](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_copper_chain.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_chain`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_copper_chest`

Источник: [lbc:uncrafted/honeycomb_copper_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_copper_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_copper_door`

Источник: [lbc:uncrafted/honeycomb_copper_door](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_copper_door.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_door`. Вес `1`, добавка за luck `0`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/honeycomb_copper_grate`

Источник: [lbc:uncrafted/honeycomb_copper_grate](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_copper_grate.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_grate`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_copper_lantern`

Источник: [lbc:uncrafted/honeycomb_copper_lantern](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_copper_lantern.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_lantern`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_copper_trapdoor`

Источник: [lbc:uncrafted/honeycomb_copper_trapdoor](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_copper_trapdoor.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_trapdoor`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_cut_copper_slab`

Источник: [lbc:uncrafted/honeycomb_cut_copper_slab](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_cut_copper_slab.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cut_copper_slab`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_cut_copper_stairs`

Источник: [lbc:uncrafted/honeycomb_cut_copper_stairs](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_cut_copper_stairs.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cut_copper_stairs`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_exposed_chiseled_copper`

Источник: [lbc:uncrafted/honeycomb_exposed_chiseled_copper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_exposed_chiseled_copper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:exposed_chiseled_copper`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_exposed_copper`

Источник: [lbc:uncrafted/honeycomb_exposed_copper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_exposed_copper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:exposed_copper`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_exposed_copper_bars`

Источник: [lbc:uncrafted/honeycomb_exposed_copper_bars](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_exposed_copper_bars.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:exposed_copper_bars`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_exposed_copper_bulb`

Источник: [lbc:uncrafted/honeycomb_exposed_copper_bulb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_exposed_copper_bulb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:exposed_copper_bulb`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_exposed_copper_chain`

Источник: [lbc:uncrafted/honeycomb_exposed_copper_chain](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_exposed_copper_chain.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:exposed_copper_chain`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_exposed_copper_chest`

Источник: [lbc:uncrafted/honeycomb_exposed_copper_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_exposed_copper_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:exposed_copper_chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_exposed_copper_lantern`

Источник: [lbc:uncrafted/honeycomb_exposed_copper_lantern](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_exposed_copper_lantern.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:exposed_copper_lantern`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_exposed_cut_copper_slab`

Источник: [lbc:uncrafted/honeycomb_exposed_cut_copper_slab](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_exposed_cut_copper_slab.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:exposed_cut_copper_slab`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_exposed_cut_copper_stairs`

Источник: [lbc:uncrafted/honeycomb_exposed_cut_copper_stairs](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_exposed_cut_copper_stairs.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:exposed_cut_copper_stairs`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_exposed_lightning_rod`

Источник: [lbc:uncrafted/honeycomb_exposed_lightning_rod](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_exposed_lightning_rod.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:exposed_lightning_rod`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_lightning_rod`

Источник: [lbc:uncrafted/honeycomb_lightning_rod](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_lightning_rod.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:lightning_rod`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_oxidized_chiseled_copper`

Источник: [lbc:uncrafted/honeycomb_oxidized_chiseled_copper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_oxidized_chiseled_copper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxidized_chiseled_copper`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_oxidized_copper`

Источник: [lbc:uncrafted/honeycomb_oxidized_copper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_oxidized_copper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxidized_copper`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_oxidized_copper_bars`

Источник: [lbc:uncrafted/honeycomb_oxidized_copper_bars](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_oxidized_copper_bars.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxidized_copper_bars`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_oxidized_copper_bulb`

Источник: [lbc:uncrafted/honeycomb_oxidized_copper_bulb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_oxidized_copper_bulb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxidized_copper_bulb`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_oxidized_copper_chain`

Источник: [lbc:uncrafted/honeycomb_oxidized_copper_chain](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_oxidized_copper_chain.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxidized_copper_chain`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_oxidized_copper_chest`

Источник: [lbc:uncrafted/honeycomb_oxidized_copper_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_oxidized_copper_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxidized_copper_chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_oxidized_copper_lantern`

Источник: [lbc:uncrafted/honeycomb_oxidized_copper_lantern](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_oxidized_copper_lantern.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxidized_copper_lantern`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_oxidized_cut_copper_slab`

Источник: [lbc:uncrafted/honeycomb_oxidized_cut_copper_slab](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_oxidized_cut_copper_slab.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxidized_cut_copper_slab`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_oxidized_cut_copper_stairs`

Источник: [lbc:uncrafted/honeycomb_oxidized_cut_copper_stairs](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_oxidized_cut_copper_stairs.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxidized_cut_copper_stairs`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_oxidized_lightning_rod`

Источник: [lbc:uncrafted/honeycomb_oxidized_lightning_rod](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_oxidized_lightning_rod.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxidized_lightning_rod`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_string`

Источник: [lbc:uncrafted/honeycomb_string](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_string.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:string`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_weathered_chiseled_copper`

Источник: [lbc:uncrafted/honeycomb_weathered_chiseled_copper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_weathered_chiseled_copper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:weathered_chiseled_copper`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_weathered_copper`

Источник: [lbc:uncrafted/honeycomb_weathered_copper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_weathered_copper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:weathered_copper`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_weathered_copper_bars`

Источник: [lbc:uncrafted/honeycomb_weathered_copper_bars](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_weathered_copper_bars.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:weathered_copper_bars`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_weathered_copper_bulb`

Источник: [lbc:uncrafted/honeycomb_weathered_copper_bulb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_weathered_copper_bulb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:weathered_copper_bulb`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_weathered_copper_chain`

Источник: [lbc:uncrafted/honeycomb_weathered_copper_chain](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_weathered_copper_chain.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:weathered_copper_chain`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_weathered_copper_chest`

Источник: [lbc:uncrafted/honeycomb_weathered_copper_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_weathered_copper_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:weathered_copper_chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_weathered_copper_lantern`

Источник: [lbc:uncrafted/honeycomb_weathered_copper_lantern](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_weathered_copper_lantern.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:weathered_copper_lantern`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_weathered_cut_copper_slab`

Источник: [lbc:uncrafted/honeycomb_weathered_cut_copper_slab](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_weathered_cut_copper_slab.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:weathered_cut_copper_slab`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_weathered_cut_copper_stairs`

Источник: [lbc:uncrafted/honeycomb_weathered_cut_copper_stairs](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_weathered_cut_copper_stairs.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:weathered_cut_copper_stairs`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/honeycomb_weathered_lightning_rod`

Источник: [lbc:uncrafted/honeycomb_weathered_lightning_rod](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/honeycomb_weathered_lightning_rod.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:weathered_lightning_rod`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/ice_9`

Источник: [lbc:uncrafted/ice_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/ice_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:ice`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/ink_sac`

Источник: [lbc:uncrafted/ink_sac](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/ink_sac.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:ink_sac`. Вес `1`, добавка за luck `0`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/iron_ingot_2`

Источник: [lbc:uncrafted/iron_ingot_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_ingot_2_stick`

Источник: [lbc:uncrafted/iron_ingot_2_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_2_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_ingot_2_stick_2`

Источник: [lbc:uncrafted/iron_ingot_2_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_2_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_ingot_3`

Источник: [lbc:uncrafted/iron_ingot_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_ingot_3_stick_2`

Источник: [lbc:uncrafted/iron_ingot_3_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_3_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_ingot_4`

Источник: [lbc:uncrafted/iron_ingot_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_ingot_4_iron_block_3`

Источник: [lbc:uncrafted/iron_ingot_4_iron_block_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_4_iron_block_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_block`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_ingot_4_redstone`

Источник: [lbc:uncrafted/iron_ingot_4_redstone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_4_redstone.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:redstone`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_ingot_5`

Источник: [lbc:uncrafted/iron_ingot_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_5.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_ingot_5_chest`

Источник: [lbc:uncrafted/iron_ingot_5_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_5_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_ingot_5_redstone_2_dropper_crafting_table`

Источник: [lbc:uncrafted/iron_ingot_5_redstone_2_dropper_crafting_table](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_5_redstone_2_dropper_crafting_table.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:redstone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:dropper`. Вес `1`, добавка за luck `0`.
Пул 4: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:crafting_table`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_ingot_5_smooth_stone_3_furnace`

Источник: [lbc:uncrafted/iron_ingot_5_smooth_stone_3_furnace](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_5_smooth_stone_3_furnace.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:smooth_stone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:furnace`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_ingot_6`

Источник: [lbc:uncrafted/iron_ingot_6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_6.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_ingot_7`

Источник: [lbc:uncrafted/iron_ingot_7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_7.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `7`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_ingot_8`

Источник: [lbc:uncrafted/iron_ingot_8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_8.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_ingot_9`

Источник: [lbc:uncrafted/iron_ingot_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_ingot_flint`

Источник: [lbc:uncrafted/iron_ingot_flint](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_flint.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:flint`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_ingot_stick_2`

Источник: [lbc:uncrafted/iron_ingot_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_ingot_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_nugget_2_iron_ingot`

Источник: [lbc:uncrafted/iron_nugget_2_iron_ingot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_nugget_2_iron_ingot.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_nugget`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_nugget_7`

Источник: [lbc:uncrafted/iron_nugget_7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_nugget_7.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_nugget`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `7`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_nugget_8_soul_torch`

Источник: [lbc:uncrafted/iron_nugget_8_soul_torch](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_nugget_8_soul_torch.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_nugget`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:soul_torch`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_nugget_8_torch`

Источник: [lbc:uncrafted/iron_nugget_8_torch](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_nugget_8_torch.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_nugget`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:torch`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/iron_nugget_9`

Источник: [lbc:uncrafted/iron_nugget_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/iron_nugget_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_nugget`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/jungle_boat_chest`

Источник: [lbc:uncrafted/jungle_boat_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/jungle_boat_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:jungle_boat`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/jungle_planks`

Источник: [lbc:uncrafted/jungle_planks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/jungle_planks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:jungle_planks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/jungle_planks_2`

Источник: [lbc:uncrafted/jungle_planks_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/jungle_planks_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:jungle_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/jungle_planks_2_stick_4`

Источник: [lbc:uncrafted/jungle_planks_2_stick_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/jungle_planks_2_stick_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:jungle_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/jungle_planks_3`

Источник: [lbc:uncrafted/jungle_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/jungle_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:jungle_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/jungle_planks_5`

Источник: [lbc:uncrafted/jungle_planks_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/jungle_planks_5.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:jungle_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/kelp`

Источник: [lbc:uncrafted/kelp](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/kelp.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:kelp`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/lapis_lazuli`

Источник: [lbc:uncrafted/lapis_lazuli](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/lapis_lazuli.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:lapis_lazuli`. Вес `1`, добавка за luck `0`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/lapis_lazuli_9`

Источник: [lbc:uncrafted/lapis_lazuli_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/lapis_lazuli_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:lapis_lazuli`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/leather_3_glass_2_wool`

Источник: [lbc:uncrafted/leather_3_glass_2_wool](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/leather_3_glass_2_wool.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:leather`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:glass`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/leather_3_iron_ingot`

Источник: [lbc:uncrafted/leather_3_iron_ingot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/leather_3_iron_ingot.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:leather`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/leather_4`

Источник: [lbc:uncrafted/leather_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/leather_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:leather`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/leather_5`

Источник: [lbc:uncrafted/leather_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/leather_5.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:leather`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/leather_7`

Источник: [lbc:uncrafted/leather_7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/leather_7.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:leather`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `7`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/leather_8`

Источник: [lbc:uncrafted/leather_8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/leather_8.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:leather`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/light_blue_wool_3_oak_planks_3`

Источник: [lbc:uncrafted/light_blue_wool_3_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/light_blue_wool_3_oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:light_blue_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/light_blue_wool_6_stick`

Источник: [lbc:uncrafted/light_blue_wool_6_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/light_blue_wool_6_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:light_blue_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/light_gray_dye_ingridient`

Источник: [lbc:uncrafted/light_gray_dye_ingridient](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/light_gray_dye_ingridient.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxeye_daisy`. Вес `1`, добавка за luck `0`.
- 2: Выдать ванильную основу `minecraft:azure_bluet`. Вес `1`, добавка за luck `0`.
- 3: Выдать ванильную основу `minecraft:white_tulip`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/light_gray_wool_3_oak_planks_3`

Источник: [lbc:uncrafted/light_gray_wool_3_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/light_gray_wool_3_oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:light_gray_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/light_gray_wool_6_stick`

Источник: [lbc:uncrafted/light_gray_wool_6_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/light_gray_wool_6_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:light_gray_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/lime_wool_3_oak_planks_3`

Источник: [lbc:uncrafted/lime_wool_3_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/lime_wool_3_oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:lime_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/lime_wool_6_stick`

Источник: [lbc:uncrafted/lime_wool_6_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/lime_wool_6_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:lime_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/magenta_wool_3_oak_planks_3`

Источник: [lbc:uncrafted/magenta_wool_3_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/magenta_wool_3_oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:magenta_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/magenta_wool_6_stick`

Источник: [lbc:uncrafted/magenta_wool_6_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/magenta_wool_6_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:magenta_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/magma_cream_4`

Источник: [lbc:uncrafted/magma_cream_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/magma_cream_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:magma_cream`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/mangrove_boat_chest`

Источник: [lbc:uncrafted/mangrove_boat_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/mangrove_boat_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:mangrove_boat`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/mangrove_planks`

Источник: [lbc:uncrafted/mangrove_planks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/mangrove_planks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:mangrove_planks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/mangrove_planks_2`

Источник: [lbc:uncrafted/mangrove_planks_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/mangrove_planks_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:mangrove_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/mangrove_planks_2_stick_4`

Источник: [lbc:uncrafted/mangrove_planks_2_stick_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/mangrove_planks_2_stick_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:mangrove_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/mangrove_planks_3`

Источник: [lbc:uncrafted/mangrove_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/mangrove_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:mangrove_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/mangrove_planks_5`

Источник: [lbc:uncrafted/mangrove_planks_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/mangrove_planks_5.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:mangrove_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/melon_slice`

Источник: [lbc:uncrafted/melon_slice](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/melon_slice.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:melon_slice`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/melon_slice_9`

Источник: [lbc:uncrafted/melon_slice_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/melon_slice_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:melon_slice`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/milk_bucket_3_wheat_3_sugar_2_egg`

Источник: [lbc:uncrafted/milk_bucket_3_wheat_3_sugar_2_egg](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/milk_bucket_3_wheat_3_sugar_2_egg.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:milk_bucket`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:wheat`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:sugar`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 4: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:egg`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/minecart_chest`

Источник: [lbc:uncrafted/minecart_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/minecart_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:minecart`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/minecart_furnace`

Источник: [lbc:uncrafted/minecart_furnace](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/minecart_furnace.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:minecart`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:furnace`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/minecart_hopper`

Источник: [lbc:uncrafted/minecart_hopper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/minecart_hopper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:minecart`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:hopper`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/minecart_tnt`

Источник: [lbc:uncrafted/minecart_tnt](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/minecart_tnt.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:minecart`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:tnt`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/mossy_cobblestone`

Источник: [lbc:uncrafted/mossy_cobblestone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/mossy_cobblestone.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:mossy_cobblestone`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/mud_bricks`

Источник: [lbc:uncrafted/mud_bricks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/mud_bricks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:mud_bricks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/mud_mangrove_roots`

Источник: [lbc:uncrafted/mud_mangrove_roots](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/mud_mangrove_roots.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:mud`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:mangrove_roots`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/mud_wheat`

Источник: [lbc:uncrafted/mud_wheat](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/mud_wheat.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:mud`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:wheat`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/mutton`

Источник: [lbc:uncrafted/mutton](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/mutton.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:mutton`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/nautilus_shell_8_heart_of_the_sea`

Источник: [lbc:uncrafted/nautilus_shell_8_heart_of_the_sea](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/nautilus_shell_8_heart_of_the_sea.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:nautilus_shell`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:heart_of_the_sea`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/nether_brick_2`

Источник: [lbc:uncrafted/nether_brick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/nether_brick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:nether_brick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/nether_brick_2_nether_wart_2`

Источник: [lbc:uncrafted/nether_brick_2_nether_wart_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/nether_brick_2_nether_wart_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:nether_brick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:nether_wart`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/nether_brick_3`

Источник: [lbc:uncrafted/nether_brick_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/nether_brick_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:nether_brick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/nether_brick_3_nether_wart_3`

Источник: [lbc:uncrafted/nether_brick_3_nether_wart_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/nether_brick_3_nether_wart_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:nether_brick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:nether_wart`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/nether_brick_4`

Источник: [lbc:uncrafted/nether_brick_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/nether_brick_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:nether_brick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/nether_brick_6`

Источник: [lbc:uncrafted/nether_brick_6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/nether_brick_6.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:nether_brick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/nether_brick_nether_wart`

Источник: [lbc:uncrafted/nether_brick_nether_wart](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/nether_brick_nether_wart.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:nether_brick`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:nether_wart`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/nether_brick_slab_2`

Источник: [lbc:uncrafted/nether_brick_slab_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/nether_brick_slab_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:nether_brick_slab`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/nether_bricks`

Источник: [lbc:uncrafted/nether_bricks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/nether_bricks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:nether_bricks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/nether_star_obsidian_3_glass_5`

Источник: [lbc:uncrafted/nether_star_obsidian_3_glass_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/nether_star_obsidian_3_glass_5.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:obsidian`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:glass`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:nether_star`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/nether_wart_9`

Источник: [lbc:uncrafted/nether_wart_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/nether_wart_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:nether_wart`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/netherite_ingot_9`

Источник: [lbc:uncrafted/netherite_ingot_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/netherite_ingot_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/netherite_ingot_diamond_upgrade_diamond_horse_armor`

Источник: [lbc:uncrafted/netherite_ingot_diamond_upgrade_diamond_horse_armor](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/netherite_ingot_diamond_upgrade_diamond_horse_armor.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond_horse_armor`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_upgrade_smithing_template`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_axe`

Источник: [lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_axe](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/netherite_ingot_netherite_upgrade_diamond_axe.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond_axe`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_upgrade_smithing_template`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_boots`

Источник: [lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_boots](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/netherite_ingot_netherite_upgrade_diamond_boots.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond_boots`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_upgrade_smithing_template`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_chestplate`

Источник: [lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_chestplate](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/netherite_ingot_netherite_upgrade_diamond_chestplate.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond_chestplate`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_upgrade_smithing_template`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_helmet`

Источник: [lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_helmet](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/netherite_ingot_netherite_upgrade_diamond_helmet.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond_helmet`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_upgrade_smithing_template`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_hoe`

Источник: [lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_hoe](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/netherite_ingot_netherite_upgrade_diamond_hoe.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond_hoe`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_upgrade_smithing_template`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_leggings`

Источник: [lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_leggings](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/netherite_ingot_netherite_upgrade_diamond_leggings.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond_leggings`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_upgrade_smithing_template`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_nautilus_armor`

Источник: [lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_nautilus_armor](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/netherite_ingot_netherite_upgrade_diamond_nautilus_armor.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond_nautilus_armor`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_upgrade_smithing_template`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_pickaxe`

Источник: [lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_pickaxe](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/netherite_ingot_netherite_upgrade_diamond_pickaxe.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond_pickaxe`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_upgrade_smithing_template`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_shovel`

Источник: [lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_shovel](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/netherite_ingot_netherite_upgrade_diamond_shovel.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond_shovel`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_upgrade_smithing_template`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_spear`

Источник: [lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_spear](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/netherite_ingot_netherite_upgrade_diamond_spear.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond_spear`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_upgrade_smithing_template`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_sword`

Источник: [lbc:uncrafted/netherite_ingot_netherite_upgrade_diamond_sword](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/netherite_ingot_netherite_upgrade_diamond_sword.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_ingot`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond_sword`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherite_upgrade_smithing_template`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/netherrack`

Источник: [lbc:uncrafted/netherrack](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/netherrack.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:netherrack`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_boat_chest`

Источник: [lbc:uncrafted/oak_boat_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_boat_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_boat`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_log_3_stick_3_coal`

Источник: [lbc:uncrafted/oak_log_3_stick_3_coal](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_log_3_stick_3_coal.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_log`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:coal`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_log_3_stick_3_soul_sand`

Источник: [lbc:uncrafted/oak_log_3_stick_3_soul_sand](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_log_3_stick_3_soul_sand.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_log`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:soul_sand`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_planks`

Источник: [lbc:uncrafted/oak_planks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_planks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_planks_2`

Источник: [lbc:uncrafted/oak_planks_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_planks_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_planks_2_stick`

Источник: [lbc:uncrafted/oak_planks_2_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_planks_2_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_planks_2_stick_2`

Источник: [lbc:uncrafted/oak_planks_2_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_planks_2_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_planks_2_stick_4`

Источник: [lbc:uncrafted/oak_planks_2_stick_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_planks_2_stick_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_planks_2_string_2`

Источник: [lbc:uncrafted/oak_planks_2_string_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_planks_2_string_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:string`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_planks_3`

Источник: [lbc:uncrafted/oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_planks_3_stick_2`

Источник: [lbc:uncrafted/oak_planks_3_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_planks_3_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_planks_4`

Источник: [lbc:uncrafted/oak_planks_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_planks_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_planks_5`

Источник: [lbc:uncrafted/oak_planks_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_planks_5.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_planks_6_book_3`

Источник: [lbc:uncrafted/oak_planks_6_book_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_planks_6_book_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:book`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_planks_6_honeycomb_3`

Источник: [lbc:uncrafted/oak_planks_6_honeycomb_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_planks_6_honeycomb_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_planks_6_oak_slab_3`

Источник: [lbc:uncrafted/oak_planks_6_oak_slab_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_planks_6_oak_slab_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_slab`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_planks_8`

Источник: [lbc:uncrafted/oak_planks_8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_planks_8.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_planks_stick_2`

Источник: [lbc:uncrafted/oak_planks_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_planks_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oak_slab_4_bookshelf`

Источник: [lbc:uncrafted/oak_slab_4_bookshelf](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oak_slab_4_bookshelf.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_slab`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bookshelf`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/obsidian_4_diamond_2_book`

Источник: [lbc:uncrafted/obsidian_4_diamond_2_book](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/obsidian_4_diamond_2_book.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:obsidian`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:book`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/obsidian_8_ender_eye`

Источник: [lbc:uncrafted/obsidian_8_ender_eye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/obsidian_8_ender_eye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:obsidian`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:ender_eye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/orange_dye_ingridient`

Источник: [lbc:uncrafted/orange_dye_ingridient](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/orange_dye_ingridient.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:open_eyeblossom`. Вес `1`, добавка за luck `0`.
- 2: Выдать ванильную основу `minecraft:orange_tulip`. Вес `1`, добавка за luck `0`.
- 3: Выдать ванильную основу `minecraft:torchflower`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/orange_tulip`

Источник: [lbc:uncrafted/orange_tulip](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/orange_tulip.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:orange_tulip`. Вес `1`, добавка за luck `0`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/orange_wool_3_oak_planks_3`

Источник: [lbc:uncrafted/orange_wool_3_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/orange_wool_3_oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:orange_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/orange_wool_6_stick`

Источник: [lbc:uncrafted/orange_wool_6_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/orange_wool_6_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:orange_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oxeye_daisy`

Источник: [lbc:uncrafted/oxeye_daisy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oxeye_daisy.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxeye_daisy`. Вес `1`, добавка за luck `0`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/oxidized_copper`

Источник: [lbc:uncrafted/oxidized_copper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oxidized_copper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxidized_copper`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oxidized_copper_2`

Источник: [lbc:uncrafted/oxidized_copper_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oxidized_copper_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxidized_copper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/oxidized_copper_3`

Источник: [lbc:uncrafted/oxidized_copper_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oxidized_copper_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxidized_copper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/oxidized_copper_door_honeycomb`

Источник: [lbc:uncrafted/oxidized_copper_door_honeycomb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oxidized_copper_door_honeycomb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxidized_copper_door`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oxidized_copper_grate_honeycomb`

Источник: [lbc:uncrafted/oxidized_copper_grate_honeycomb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oxidized_copper_grate_honeycomb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxidized_copper_grate`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oxidized_copper_trapdoor_honeycomb`

Источник: [lbc:uncrafted/oxidized_copper_trapdoor_honeycomb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oxidized_copper_trapdoor_honeycomb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxidized_copper_trapdoor`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/oxidized_cut_copper_honeycomb`

Источник: [lbc:uncrafted/oxidized_cut_copper_honeycomb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/oxidized_cut_copper_honeycomb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxidized_cut_copper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/packed_ice_9`

Источник: [lbc:uncrafted/packed_ice_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/packed_ice_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:packed_ice`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/packed_mud`

Источник: [lbc:uncrafted/packed_mud](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/packed_mud.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:packed_mud`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/pale_oak_boat_chest`

Источник: [lbc:uncrafted/pale_oak_boat_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/pale_oak_boat_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pale_oak_boat`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/pale_oak_planks`

Источник: [lbc:uncrafted/pale_oak_planks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/pale_oak_planks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pale_oak_planks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/pale_oak_planks_2`

Источник: [lbc:uncrafted/pale_oak_planks_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/pale_oak_planks_2.json).
Пул 1: выборов `2`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pale_oak_planks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/pale_oak_planks_2_stick_4`

Источник: [lbc:uncrafted/pale_oak_planks_2_stick_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/pale_oak_planks_2_stick_4.json).
Пул 1: выборов `2`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pale_oak_planks`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `4`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/pale_oak_planks_3`

Источник: [lbc:uncrafted/pale_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/pale_oak_planks_3.json).
Пул 1: выборов `3`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pale_oak_planks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/pale_oak_planks_5`

Источник: [lbc:uncrafted/pale_oak_planks_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/pale_oak_planks_5.json).
Пул 1: выборов `5`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pale_oak_planks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/paper_3_leather`

Источник: [lbc:uncrafted/paper_3_leather](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/paper_3_leather.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:paper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:leather`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/paper_8_compass`

Источник: [lbc:uncrafted/paper_8_compass](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/paper_8_compass.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:paper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:compass`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/paper_bricks`

Источник: [lbc:uncrafted/paper_bricks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/paper_bricks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:paper`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bricks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/paper_enchanted_golden_apple`

Источник: [lbc:uncrafted/paper_enchanted_golden_apple](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/paper_enchanted_golden_apple.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:paper`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:enchanted_golden_apple`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/paper_oxeye_daisy`

Источник: [lbc:uncrafted/paper_oxeye_daisy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/paper_oxeye_daisy.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:paper`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oxeye_daisy`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/paper_vine`

Источник: [lbc:uncrafted/paper_vine](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/paper_vine.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:paper`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:vine`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/pink_dye_ingridient`

Источник: [lbc:uncrafted/pink_dye_ingridient](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/pink_dye_ingridient.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pink_petals`. Вес `1`, добавка за luck `0`.
- 2: Выдать ванильную основу `minecraft:pink_tulip`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/pink_petals`

Источник: [lbc:uncrafted/pink_petals](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/pink_petals.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pink_petals`. Вес `1`, добавка за luck `0`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/pink_wool_3_oak_planks_3`

Источник: [lbc:uncrafted/pink_wool_3_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/pink_wool_3_oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pink_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/pink_wool_6_stick`

Источник: [lbc:uncrafted/pink_wool_6_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/pink_wool_6_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pink_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/piston_slime_ball`

Источник: [lbc:uncrafted/piston_slime_ball](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/piston_slime_ball.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:piston`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:slime_ball`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/pitcher_plant`

Источник: [lbc:uncrafted/pitcher_plant](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/pitcher_plant.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pitcher_plant`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/pointed_dripstone_4`

Источник: [lbc:uncrafted/pointed_dripstone_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/pointed_dripstone_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pointed_dripstone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/polished_blackstone`

Источник: [lbc:uncrafted/polished_blackstone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/polished_blackstone.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:polished_blackstone`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/polished_blackstone_2`

Источник: [lbc:uncrafted/polished_blackstone_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/polished_blackstone_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:polished_blackstone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/polished_blackstone_bricks`

Источник: [lbc:uncrafted/polished_blackstone_bricks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/polished_blackstone_bricks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:polished_blackstone_bricks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/polished_blackstone_slab_2`

Источник: [lbc:uncrafted/polished_blackstone_slab_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/polished_blackstone_slab_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:polished_blackstone_slab`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/polished_deepslate`

Источник: [lbc:uncrafted/polished_deepslate](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/polished_deepslate.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:polished_deepslate`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/polished_tuff`

Источник: [lbc:uncrafted/polished_tuff](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/polished_tuff.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:polished_tuff`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/popped_chorus_fruit`

Источник: [lbc:uncrafted/popped_chorus_fruit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/popped_chorus_fruit.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:popped_chorus_fruit`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/poppy`

Источник: [lbc:uncrafted/poppy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/poppy.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:poppy`. Вес `1`, добавка за luck `0`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/porkchop`

Источник: [lbc:uncrafted/porkchop](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/porkchop.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:porkchop`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/prismarine`

Источник: [lbc:uncrafted/prismarine](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/prismarine.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:prismarine`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/prismarine_crystals_5_prismarine_shard_4`

Источник: [lbc:uncrafted/prismarine_crystals_5_prismarine_shard_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/prismarine_crystals_5_prismarine_shard_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:prismarine_crystals`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:prismarine_shard`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/prismarine_shard_2`

Источник: [lbc:uncrafted/prismarine_shard_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/prismarine_shard_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:prismarine_shard`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/prismarine_shard_4`

Источник: [lbc:uncrafted/prismarine_shard_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/prismarine_shard_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:prismarine_shard`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/prismarine_shard_6`

Источник: [lbc:uncrafted/prismarine_shard_6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/prismarine_shard_6.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:prismarine_shard`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/prismarine_shard_8_black_dye`

Источник: [lbc:uncrafted/prismarine_shard_8_black_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/prismarine_shard_8_black_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:prismarine_shard`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:black_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/prismarine_shard_9`

Источник: [lbc:uncrafted/prismarine_shard_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/prismarine_shard_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:prismarine_shard`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/purple_wool_3_oak_planks_3`

Источник: [lbc:uncrafted/purple_wool_3_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/purple_wool_3_oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:purple_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/purple_wool_6_stick`

Источник: [lbc:uncrafted/purple_wool_6_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/purple_wool_6_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:purple_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/purpur_slab_2`

Источник: [lbc:uncrafted/purpur_slab_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/purpur_slab_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:purpur_slab`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/quartz_2`

Источник: [lbc:uncrafted/quartz_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/quartz_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:quartz`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/quartz_4`

Источник: [lbc:uncrafted/quartz_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/quartz_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:quartz`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/quartz_6`

Источник: [lbc:uncrafted/quartz_6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/quartz_6.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:quartz`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/quartz_block`

Источник: [lbc:uncrafted/quartz_block](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/quartz_block.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:quartz_block`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/quartz_slab_2`

Источник: [lbc:uncrafted/quartz_slab_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/quartz_slab_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:quartz_slab`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/rabbit`

Источник: [lbc:uncrafted/rabbit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/rabbit.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:rabbit`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/rabbit_hide_4`

Источник: [lbc:uncrafted/rabbit_hide_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/rabbit_hide_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:rabbit_hide`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/rabbit_hide_6_stick_2`

Источник: [lbc:uncrafted/rabbit_hide_6_stick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/rabbit_hide_6_stick_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:rabbit_hide`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/random_log_or_wood`

Источник: [lbc:uncrafted/random_log_or_wood](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/random_log_or_wood.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_log`. Вес `1`, добавка за luck `0`.
- 2: Выдать ванильную основу `minecraft:birch_log`. Вес `1`, добавка за luck `0`.
- 3: Выдать ванильную основу `minecraft:acacia_log`. Вес `1`, добавка за luck `0`.
- 4: Выдать ванильную основу `minecraft:cherry_log`. Вес `1`, добавка за luck `0`.
- 5: Выдать ванильную основу `minecraft:jungle_log`. Вес `1`, добавка за luck `0`.
- 6: Выдать ванильную основу `minecraft:spruce_log`. Вес `1`, добавка за luck `0`.
- 7: Выдать ванильную основу `minecraft:dark_oak_log`. Вес `1`, добавка за luck `0`.
- 8: Выдать ванильную основу `minecraft:mangrove_log`. Вес `1`, добавка за luck `0`.
- 9: Выдать ванильную основу `minecraft:pale_oak_log`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/raw_copper`

Источник: [lbc:uncrafted/raw_copper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/raw_copper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:raw_copper`. Вес `1`, добавка за luck `0`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/raw_copper_9`

Источник: [lbc:uncrafted/raw_copper_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/raw_copper_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:raw_copper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/raw_gold_9`

Источник: [lbc:uncrafted/raw_gold_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/raw_gold_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:raw_gold`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/raw_iron_9`

Источник: [lbc:uncrafted/raw_iron_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/raw_iron_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:raw_iron`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/red_dye_ingridient`

Источник: [lbc:uncrafted/red_dye_ingridient](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/red_dye_ingridient.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:beetroot`. Вес `1`, добавка за luck `0`.
- 2: Выдать ванильную основу `minecraft:red_tulip`. Вес `1`, добавка за luck `0`.
- 3: Выдать ванильную основу `minecraft:poppy`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/red_nether_bricks`

Источник: [lbc:uncrafted/red_nether_bricks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/red_nether_bricks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_nether_bricks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/red_sand_2`

Источник: [lbc:uncrafted/red_sand_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/red_sand_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_sand`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/red_sand_4`

Источник: [lbc:uncrafted/red_sand_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/red_sand_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_sand`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/red_sand_6`

Источник: [lbc:uncrafted/red_sand_6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/red_sand_6.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_sand`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/red_sandstone`

Источник: [lbc:uncrafted/red_sandstone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/red_sandstone.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_sandstone`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/red_sandstone_slab_2`

Источник: [lbc:uncrafted/red_sandstone_slab_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/red_sandstone_slab_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_sandstone_slab`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/red_wool_3_oak_planks_3`

Источник: [lbc:uncrafted/red_wool_3_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/red_wool_3_oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/red_wool_6_stick`

Источник: [lbc:uncrafted/red_wool_6_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/red_wool_6_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/redstone_4_glowstone`

Источник: [lbc:uncrafted/redstone_4_glowstone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/redstone_4_glowstone.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:redstone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:glowstone`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/redstone_4_hay_block`

Источник: [lbc:uncrafted/redstone_4_hay_block](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/redstone_4_hay_block.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:redstone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:hay_block`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/redstone_9`

Источник: [lbc:uncrafted/redstone_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/redstone_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:redstone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/redstone_stick`

Источник: [lbc:uncrafted/redstone_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/redstone_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:redstone`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/resin_brick_2`

Источник: [lbc:uncrafted/resin_brick_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/resin_brick_2.json).
Пул 1: выборов `2`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:resin_brick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/resin_brick_4`

Источник: [lbc:uncrafted/resin_brick_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/resin_brick_4.json).
Пул 1: выборов `4`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:resin_brick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/resin_brick_6`

Источник: [lbc:uncrafted/resin_brick_6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/resin_brick_6.json).
Пул 1: выборов `6`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:resin_brick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/resin_bricks`

Источник: [lbc:uncrafted/resin_bricks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/resin_bricks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:resin_brick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/resin_clump`

Источник: [lbc:uncrafted/resin_clump](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/resin_clump.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:resin_clump`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/resin_clump_9`

Источник: [lbc:uncrafted/resin_clump_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/resin_clump_9.json).
Пул 1: выборов `9`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:resin_clump`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/resin_slab_2`

Источник: [lbc:uncrafted/resin_slab_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/resin_slab_2.json).
Пул 1: выборов `2`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:resin_brick_slab`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/salmon`

Источник: [lbc:uncrafted/salmon](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/salmon.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:salmon`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/sand_2`

Источник: [lbc:uncrafted/sand_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/sand_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:sand`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/sand_4`

Источник: [lbc:uncrafted/sand_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/sand_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:sand`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/sand_6`

Источник: [lbc:uncrafted/sand_6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/sand_6.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:sand`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/sandstone`

Источник: [lbc:uncrafted/sandstone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/sandstone.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:sandstone`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/sandstone_slab_2`

Источник: [lbc:uncrafted/sandstone_slab_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/sandstone_slab_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:sandstone_slab`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/shulker_box_black_dye`

Источник: [lbc:uncrafted/shulker_box_black_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/shulker_box_black_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:shulker_box`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:black_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/shulker_box_blue_dye`

Источник: [lbc:uncrafted/shulker_box_blue_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/shulker_box_blue_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:shulker_box`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:blue_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/shulker_box_brown_dye`

Источник: [lbc:uncrafted/shulker_box_brown_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/shulker_box_brown_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:shulker_box`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/shulker_box_cyan_dye`

Источник: [lbc:uncrafted/shulker_box_cyan_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/shulker_box_cyan_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:shulker_box`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cyan_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/shulker_box_gray_dye`

Источник: [lbc:uncrafted/shulker_box_gray_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/shulker_box_gray_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:shulker_box`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gray_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/shulker_box_green_dye`

Источник: [lbc:uncrafted/shulker_box_green_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/shulker_box_green_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:shulker_box`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:green_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/shulker_box_light_blue_dye`

Источник: [lbc:uncrafted/shulker_box_light_blue_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/shulker_box_light_blue_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:shulker_box`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:light_blue_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/shulker_box_light_gray_dye`

Источник: [lbc:uncrafted/shulker_box_light_gray_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/shulker_box_light_gray_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:shulker_box`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:light_gray_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/shulker_box_lime_dye`

Источник: [lbc:uncrafted/shulker_box_lime_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/shulker_box_lime_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:shulker_box`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:lime_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/shulker_box_magenta_dye`

Источник: [lbc:uncrafted/shulker_box_magenta_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/shulker_box_magenta_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:shulker_box`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:magenta_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/shulker_box_orange_dye`

Источник: [lbc:uncrafted/shulker_box_orange_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/shulker_box_orange_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:shulker_box`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:orange_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/shulker_box_pink_dye`

Источник: [lbc:uncrafted/shulker_box_pink_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/shulker_box_pink_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:shulker_box`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pink_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/shulker_box_purple_dye`

Источник: [lbc:uncrafted/shulker_box_purple_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/shulker_box_purple_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:shulker_box`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:purple_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/shulker_box_red_dye`

Источник: [lbc:uncrafted/shulker_box_red_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/shulker_box_red_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:shulker_box`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/shulker_box_white_dye`

Источник: [lbc:uncrafted/shulker_box_white_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/shulker_box_white_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:shulker_box`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/shulker_box_yellow_dye`

Источник: [lbc:uncrafted/shulker_box_yellow_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/shulker_box_yellow_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:shulker_box`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:yellow_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/shulker_shell_2_chest`

Источник: [lbc:uncrafted/shulker_shell_2_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/shulker_shell_2_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:shulker_shell`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/slime_ball_9`

Источник: [lbc:uncrafted/slime_ball_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/slime_ball_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:slime_ball`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/slime_ball_blaze_powder`

Источник: [lbc:uncrafted/slime_ball_blaze_powder](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/slime_ball_blaze_powder.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:slime_ball`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:blaze_powder`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/snowball_2`

Источник: [lbc:uncrafted/snowball_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/snowball_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:snowball`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/snowball_4`

Источник: [lbc:uncrafted/snowball_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/snowball_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:snowball`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/spider_eye_sugar_brown_mushroom`

Источник: [lbc:uncrafted/spider_eye_sugar_brown_mushroom](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/spider_eye_sugar_brown_mushroom.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:spider_eye`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:sugar`. Вес `1`, добавка за luck `0`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_mushroom`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/spruce_boat_chest`

Источник: [lbc:uncrafted/spruce_boat_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/spruce_boat_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:spruce_boat`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/spruce_oak_log_4_furnace`

Источник: [lbc:uncrafted/spruce_oak_log_4_furnace](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/spruce_oak_log_4_furnace.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:spruce_log`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:furnace`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/spruce_planks`

Источник: [lbc:uncrafted/spruce_planks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/spruce_planks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:spruce_planks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/spruce_planks_2`

Источник: [lbc:uncrafted/spruce_planks_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/spruce_planks_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:spruce_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/spruce_planks_2_stick_4`

Источник: [lbc:uncrafted/spruce_planks_2_stick_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/spruce_planks_2_stick_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:spruce_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/spruce_planks_3`

Источник: [lbc:uncrafted/spruce_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/spruce_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:spruce_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/spruce_planks_5`

Источник: [lbc:uncrafted/spruce_planks_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/spruce_planks_5.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:spruce_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/spruce_planks_6_iron_ingot`

Источник: [lbc:uncrafted/spruce_planks_6_iron_ingot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/spruce_planks_6_iron_ingot.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:spruce_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/spruce_planks_6_spruce_slab_2`

Источник: [lbc:uncrafted/spruce_planks_6_spruce_slab_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/spruce_planks_6_spruce_slab_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:spruce_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:spruce_slab`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/spruce_planks_8_diamond`

Источник: [lbc:uncrafted/spruce_planks_8_diamond](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/spruce_planks_8_diamond.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:spruce_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:diamond`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/spruce_planks_8_redstone`

Источник: [lbc:uncrafted/spruce_planks_8_redstone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/spruce_planks_8_redstone.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:spruce_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:redstone`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/spruce_slab_7`

Источник: [lbc:uncrafted/spruce_slab_7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/spruce_slab_7.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:spruce_slab`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `7`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stick_3_string_2`

Источник: [lbc:uncrafted/stick_3_string_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stick_3_string_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:string`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stick_3_string_2_iron_ingot_tripwire_hook`

Источник: [lbc:uncrafted/stick_3_string_2_iron_ingot_tripwire_hook](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stick_3_string_2_iron_ingot_tripwire_hook.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:string`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.
Пул 4: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:tripwire_hook`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stick_3_string_3`

Источник: [lbc:uncrafted/stick_3_string_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stick_3_string_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:string`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stick_6_smooth_stone_slab`

Источник: [lbc:uncrafted/stick_6_smooth_stone_slab](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stick_6_smooth_stone_slab.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:smooth_stone_slab`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stick_8_leather`

Источник: [lbc:uncrafted/stick_8_leather](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stick_8_leather.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:leather`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stick_8_white_wool`

Источник: [lbc:uncrafted/stick_8_white_wool](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stick_8_white_wool.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `8`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stick_copper_ingot_feather`

Источник: [lbc:uncrafted/stick_copper_ingot_feather](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stick_copper_ingot_feather.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:copper_ingot`. Вес `1`, добавка за luck `0`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:feather`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stone`

Источник: [lbc:uncrafted/stone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stone.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stone`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stone_2`

Источник: [lbc:uncrafted/stone_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stone_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stone_3_iron_ingot`

Источник: [lbc:uncrafted/stone_3_iron_ingot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stone_3_iron_ingot.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:iron_ingot`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stone_3_redstone_2_redstone_torch`

Источник: [lbc:uncrafted/stone_3_redstone_2_redstone_torch](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stone_3_redstone_2_redstone_torch.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:redstone`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 3: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:redstone_torch`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stone_brick_slab_2`

Источник: [lbc:uncrafted/stone_brick_slab_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stone_brick_slab_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stone_brick_slab`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stone_bricks`

Источник: [lbc:uncrafted/stone_bricks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stone_bricks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stone_bricks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stone_bricks_moss_block`

Источник: [lbc:uncrafted/stone_bricks_moss_block](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stone_bricks_moss_block.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stone_bricks`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:moss_block`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stripped_acacia_log`

Источник: [lbc:uncrafted/stripped_acacia_log](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stripped_acacia_log.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stripped_acacia_log`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stripped_bamboo_log`

Источник: [lbc:uncrafted/stripped_bamboo_log](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stripped_bamboo_log.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stripped_bamboo_block`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stripped_birch_log`

Источник: [lbc:uncrafted/stripped_birch_log](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stripped_birch_log.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stripped_birch_log`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stripped_cherry_log`

Источник: [lbc:uncrafted/stripped_cherry_log](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stripped_cherry_log.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stripped_cherry_log`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stripped_crimson_log`

Источник: [lbc:uncrafted/stripped_crimson_log](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stripped_crimson_log.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stripped_crimson_stem`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stripped_dark_oak_log`

Источник: [lbc:uncrafted/stripped_dark_oak_log](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stripped_dark_oak_log.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stripped_dark_oak_log`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stripped_jungle_log`

Источник: [lbc:uncrafted/stripped_jungle_log](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stripped_jungle_log.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stripped_jungle_log`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stripped_mangrove_log`

Источник: [lbc:uncrafted/stripped_mangrove_log](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stripped_mangrove_log.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stripped_mangrove_log`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stripped_oak_log`

Источник: [lbc:uncrafted/stripped_oak_log](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stripped_oak_log.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stripped_oak_log`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stripped_pale_oak_log`

Источник: [lbc:uncrafted/stripped_pale_oak_log](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stripped_pale_oak_log.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stripped_pale_oak_log`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stripped_spruce_log`

Источник: [lbc:uncrafted/stripped_spruce_log](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stripped_spruce_log.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stripped_spruce_log`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/stripped_warped_log`

Источник: [lbc:uncrafted/stripped_warped_log](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/stripped_warped_log.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stripped_warped_stem`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/sugar_cane`

Источник: [lbc:uncrafted/sugar_cane](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/sugar_cane.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:sugar_cane`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `uncrafted/tmp.tmp`

Источник: [uncrafted/tmp.tmp](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/tmp.tmp).
Черновик команд для разборки жёлтых предметов, warped-вариантов и waxed copper с заглушкой `ffffff`. При копировании в функцию команды выдавали бы соответствующие uncrafted-таблицы для подходящего Item.id. Расширение `.tmp` не загружается как `.mcfunction`, поэтому этот файл сам не выполняется.

## `lbc:uncrafted/torch_carved_pumpkin`

Источник: [lbc:uncrafted/torch_carved_pumpkin](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/torch_carved_pumpkin.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:torch`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pumpkin`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/tripwire_hook_chest`

Источник: [lbc:uncrafted/tripwire_hook_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/tripwire_hook_chest.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:tripwire_hook`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:chest`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/tuff`

Источник: [lbc:uncrafted/tuff](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/tuff.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:tuff`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/tuff_brick_slab_2`

Источник: [lbc:uncrafted/tuff_brick_slab_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/tuff_brick_slab_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:tuff_brick_slab`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/tuff_bricks`

Источник: [lbc:uncrafted/tuff_bricks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/tuff_bricks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:tuff_bricks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/tuff_slab_2`

Источник: [lbc:uncrafted/tuff_slab_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/tuff_slab_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:tuff_slab`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/turtle_scute_5`

Источник: [lbc:uncrafted/turtle_scute_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/turtle_scute_5.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:turtle_scute`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `5`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/warped_planks`

Источник: [lbc:uncrafted/warped_planks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/warped_planks.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:warped_planks`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/warped_planks_2`

Источник: [lbc:uncrafted/warped_planks_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/warped_planks_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:warped_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/warped_planks_2_stick_4`

Источник: [lbc:uncrafted/warped_planks_2_stick_4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/warped_planks_2_stick_4.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:warped_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/warped_planks_3`

Источник: [lbc:uncrafted/warped_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/warped_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:warped_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/waxed_copper`

Источник: [lbc:uncrafted/waxed_copper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/waxed_copper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:waxed_copper_block`. Вес `1`, добавка за luck `0`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/waxed_copper_block`

Источник: [lbc:uncrafted/waxed_copper_block](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/waxed_copper_block.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:waxed_copper_block`. Вес `1`, добавка за luck `0`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/waxed_copper_block_2`

Источник: [lbc:uncrafted/waxed_copper_block_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/waxed_copper_block_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:waxed_copper_block`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/waxed_copper_block_3`

Источник: [lbc:uncrafted/waxed_copper_block_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/waxed_copper_block_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:waxed_copper_block`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/waxed_exposed_copper`

Источник: [lbc:uncrafted/waxed_exposed_copper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/waxed_exposed_copper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:waxed_exposed_copper`. Вес `1`, добавка за luck `0`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/waxed_exposed_copper_3`

Источник: [lbc:uncrafted/waxed_exposed_copper_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/waxed_exposed_copper_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:waxed_exposed_copper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/waxed_exposed_copper_block_2`

Источник: [lbc:uncrafted/waxed_exposed_copper_block_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/waxed_exposed_copper_block_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:waxed_exposed_copper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/waxed_oxidized_copper`

Источник: [lbc:uncrafted/waxed_oxidized_copper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/waxed_oxidized_copper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:waxed_oxidized_copper`. Вес `1`, добавка за luck `0`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/waxed_oxidized_copper_2`

Источник: [lbc:uncrafted/waxed_oxidized_copper_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/waxed_oxidized_copper_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:waxed_oxidized_copper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/waxed_oxidized_copper_3`

Источник: [lbc:uncrafted/waxed_oxidized_copper_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/waxed_oxidized_copper_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:waxed_oxidized_copper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/waxed_weathered_copper`

Источник: [lbc:uncrafted/waxed_weathered_copper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/waxed_weathered_copper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:waxed_weathered_copper`. Вес `1`, добавка за luck `0`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/waxed_weathered_copper_2`

Источник: [lbc:uncrafted/waxed_weathered_copper_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/waxed_weathered_copper_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:waxed_weathered_copper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/waxed_weathered_copper_3`

Источник: [lbc:uncrafted/waxed_weathered_copper_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/waxed_weathered_copper_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:waxed_weathered_copper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/weathered_copper`

Источник: [lbc:uncrafted/weathered_copper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/weathered_copper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:weathered_copper`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/weathered_copper_2`

Источник: [lbc:uncrafted/weathered_copper_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/weathered_copper_2.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:weathered_copper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `2`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/weathered_copper_3`

Источник: [lbc:uncrafted/weathered_copper_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/weathered_copper_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:weathered_copper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.

## `lbc:uncrafted/weathered_copper_door_honeycomb`

Источник: [lbc:uncrafted/weathered_copper_door_honeycomb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/weathered_copper_door_honeycomb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:weathered_copper_door`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/weathered_copper_grate_honeycomb`

Источник: [lbc:uncrafted/weathered_copper_grate_honeycomb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/weathered_copper_grate_honeycomb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:weathered_copper_grate`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/weathered_copper_trapdoor_honeycomb`

Источник: [lbc:uncrafted/weathered_copper_trapdoor_honeycomb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/weathered_copper_trapdoor_honeycomb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:weathered_copper_trapdoor`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/weathered_cut_copper_honeycomb`

Источник: [lbc:uncrafted/weathered_cut_copper_honeycomb](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/weathered_cut_copper_honeycomb.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:weathered_cut_copper`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:honeycomb`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `1`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/whead_seeds`

Источник: [lbc:uncrafted/whead_seeds](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/whead_seeds.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:wheat_seeds`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/wheat_3`

Источник: [lbc:uncrafted/wheat_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/wheat_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:wheat`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/wheat_9`

Источник: [lbc:uncrafted/wheat_9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/wheat_9.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:wheat`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `9`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_carpet_black_dye`

Источник: [lbc:uncrafted/white_carpet_black_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_carpet_black_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_carpet`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:black_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_carpet_blue_dye`

Источник: [lbc:uncrafted/white_carpet_blue_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_carpet_blue_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_carpet`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:blue_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_carpet_brown_dye`

Источник: [lbc:uncrafted/white_carpet_brown_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_carpet_brown_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_carpet`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_carpet_cyan_dye`

Источник: [lbc:uncrafted/white_carpet_cyan_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_carpet_cyan_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_carpet`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cyan_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_carpet_gray_dye`

Источник: [lbc:uncrafted/white_carpet_gray_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_carpet_gray_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_carpet`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gray_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_carpet_green_dye`

Источник: [lbc:uncrafted/white_carpet_green_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_carpet_green_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_carpet`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:green_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_carpet_light_blue_dye`

Источник: [lbc:uncrafted/white_carpet_light_blue_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_carpet_light_blue_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_carpet`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:light_blue_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_carpet_light_gray_dye`

Источник: [lbc:uncrafted/white_carpet_light_gray_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_carpet_light_gray_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_carpet`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:light_gray_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_carpet_lime_dye`

Источник: [lbc:uncrafted/white_carpet_lime_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_carpet_lime_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_carpet`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:lime_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_carpet_magenta_dye`

Источник: [lbc:uncrafted/white_carpet_magenta_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_carpet_magenta_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_carpet`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:magenta_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_carpet_orange_dye`

Источник: [lbc:uncrafted/white_carpet_orange_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_carpet_orange_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_carpet`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:orange_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_carpet_pink_dye`

Источник: [lbc:uncrafted/white_carpet_pink_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_carpet_pink_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_carpet`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pink_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_carpet_purple_dye`

Источник: [lbc:uncrafted/white_carpet_purple_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_carpet_purple_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_carpet`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:purple_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_carpet_red_dye`

Источник: [lbc:uncrafted/white_carpet_red_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_carpet_red_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_carpet`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_carpet_white_dye`

Источник: [lbc:uncrafted/white_carpet_white_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_carpet_white_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_carpet`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_dye_ingridient`

Источник: [lbc:uncrafted/white_dye_ingridient](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_dye_ingridient.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:bone_meal`. Вес `1`, добавка за luck `0`.
- 2: Выдать ванильную основу `minecraft:lily_of_the_valley`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_wool_3_oak_planks_3`

Источник: [lbc:uncrafted/white_wool_3_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_wool_3_oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_wool_6_stick`

Источник: [lbc:uncrafted/white_wool_6_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_wool_6_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_wool_black_dye`

Источник: [lbc:uncrafted/white_wool_black_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_wool_black_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:black_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_wool_blue_dye`

Источник: [lbc:uncrafted/white_wool_blue_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_wool_blue_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:blue_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_wool_brown_dye`

Источник: [lbc:uncrafted/white_wool_brown_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_wool_brown_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:brown_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_wool_cyan_dye`

Источник: [lbc:uncrafted/white_wool_cyan_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_wool_cyan_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:cyan_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_wool_gray_dye`

Источник: [lbc:uncrafted/white_wool_gray_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_wool_gray_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:gray_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_wool_green_dye`

Источник: [lbc:uncrafted/white_wool_green_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_wool_green_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:green_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_wool_light_blue_dye`

Источник: [lbc:uncrafted/white_wool_light_blue_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_wool_light_blue_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:light_blue_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_wool_light_gray_dye`

Источник: [lbc:uncrafted/white_wool_light_gray_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_wool_light_gray_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:light_gray_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_wool_lime_dye`

Источник: [lbc:uncrafted/white_wool_lime_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_wool_lime_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:lime_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_wool_magenta_dye`

Источник: [lbc:uncrafted/white_wool_magenta_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_wool_magenta_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:magenta_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_wool_orange_dye`

Источник: [lbc:uncrafted/white_wool_orange_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_wool_orange_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:orange_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_wool_pink_dye`

Источник: [lbc:uncrafted/white_wool_pink_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_wool_pink_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:pink_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_wool_purple_dye`

Источник: [lbc:uncrafted/white_wool_purple_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_wool_purple_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:purple_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_wool_red_dye`

Источник: [lbc:uncrafted/white_wool_red_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_wool_red_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:red_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/white_wool_white_dye`

Источник: [lbc:uncrafted/white_wool_white_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/white_wool_white_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:string`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `4`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/wither_skeleton_skull_paper`

Источник: [lbc:uncrafted/wither_skeleton_skull_paper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/wither_skeleton_skull_paper.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:wither_skeleton_skull`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:paper`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/yellow_carpet_yellow_dye`

Источник: [lbc:uncrafted/yellow_carpet_yellow_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/yellow_carpet_yellow_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_carpet`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:yellow_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/yellow_wool_3_oak_planks_3`

Источник: [lbc:uncrafted/yellow_wool_3_oak_planks_3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/yellow_wool_3_oak_planks_3.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:yellow_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:oak_planks`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `3`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/yellow_wool_6_stick`

Источник: [lbc:uncrafted/yellow_wool_6_stick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/yellow_wool_6_stick.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:yellow_wool`. Вес `1`, добавка за luck `0`.
  Изменение 1: задать количество `6`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:stick`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

## `lbc:uncrafted/yellow_wool_yellow_dye`

Источник: [lbc:uncrafted/yellow_wool_yellow_dye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/uncrafted/yellow_wool_yellow_dye.json).
Пул 1: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:white_wool`. Вес `1`, добавка за luck `0`.
Пул 2: выборов `1`; bonus_rolls `0`.

- 1: Выдать ванильную основу `minecraft:yellow_dye`. Вес `1`, добавка за luck `0`.

Буквальные ссылки на ID: [lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft_f_loot_select.mcfunction).

