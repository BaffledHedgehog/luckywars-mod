# Tiny Ruby и Tiny Terra: source audit

Аудит старого datapack; это описание фактических команд, не решение об ожидаемом поведении. Проверены вызовы, цены, item lore и tiny-жезлы в `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.

## Вход

ПКМ route проходит через `items/spells` → `workingitems/spell1` → `spell1_default` (main hand) или `_left` (offhand). `spell1` сбрасывает `Distance=0`, ставит `caster`/`raycaster`, после cast снимает теги. Оба Tiny-жезла дополнительно требуют mana ≥ cost и отсутствия `stopper_magic` marker в 20 блоках; внешний route блокирует `nomagic_active` у игрока-кастера и у других игроков в радиусе 20 (в селекторе исключён только `gamemode=spectator`). Отдельного cooldown в ветках нет. Оба wrapper-а после эффекта списывают стоимость, применяют Mage full-set `manamax += mage_set_bonus` и вызывают `items/trinket_effects/cast_wand` (Blood Anal Boom / Mana Gem dependencies).

| Жезл | Source item keys / mana | Полный фактический эффект |
|---|---|---|
| Ruby | `struby_tiny:1`, `magicwand:1`; model `lbc:st/ruby_tiny`; name key `wand_ruby_tiny`; 500 mana | `ruby.mcfunction` зовёт `ruby_2`; каждый вызов увеличивает `Distance`, рисует dust `[0.89,0,0.973]`, scale 1 и выполняет `fill ~ ~ ~ ~ ~ ~ minecraft:air destroy`. Затем при `Distance<=100` рекурсивно сдвигает выполнение на 0.35 блока. Из-за reset в `spell1` цикл обрабатывает точки от глаза до 35 блоков (101 вызов); он не проверяет сущности или блоки как препятствия и не завершает луч при попадании. Любой блок по линии уничтожается с дропом; повторный вызов внутри уже очищенного блока — пустой fill. Wrapper добавляет звук `block.amethyst_cluster.break` после всей линии. `ruby_end.mcfunction` не вызывается ни из этой ветки, ни из других datapack functions, найденных поиском. В нём только fill двух точек вдоль взгляда в air destroy, звук и `Distance=1000`; это недостижимый для текущего route helper, а не endpoint Ruby.
| Terra | `stterra_tiny:1`, `magicwand:1`; model `lbc:st/terra_tiny`; name key `wand_terra_tiny`; 250 mana | `terra.mcfunction` зовёт `terra_2`. Каждый шаг (0.35 блока, до 101 вызова / 35 блоков) рисует dust-color-transition с шансом 30%, воспроизводит `block.amethyst_cluster.hit` (volume .1, pitch 2) и выполняет `damage @e[dx=0,dy=0,dz=0,distance=..8,tag=!raycaster,tag=!spectator,limit=1] 5 cactus by @s`. Урон не завершает цикл: цель может получить команду урона снова на следующих точках. Нет team-фильтра, сортировка selector-а не задана. Если текущий блок не в `#minecraft:airs`, зовётся `terra_end`, который ставит `Distance=1000`, затем `short_grass` на `~ ~1 ~` и `moss_block` на `~ ~ ~`; вследствие score конечная рекурсия прекращается. Порядок source команд наносит урон и звук на шаге стены до terminal helper. `terra_end` не играет отдельного звука.

## Отдельный crafting dependency

`tick1s.mcfunction` раз в секунду вызывает `other/craft_ground` для `craftusable` item entity рядом с другим предметом, кроме измерений `nexus` и `imprinted`. В `craft_ground` Tiny Ruby + выпавший `libspell` запускают `other/craft/crafted_spell` (loot `transformables_spells`); Tiny Terra + соседний item с `Thrower` запускают `crafted_spruce_leaves` (предметы в радиусе helper-а переводятся в `minecraft:spruce_leaves`, custom_data снимается). Это отдельная от casting ветка; включение рецептов/крафтов в эту пачку отложено до переноса обычных версий.

## Lore и замечания к намерению

Решения пользователя для переноса (дословно):

- Ruby: «Сквозной луч, любые блоки — как раньше».
- Terra: «Пробивать существ, по одному удару 5 каждой цели за каст».

Это явное исключение к single-terminal правилу из предыдущей пачки: для этих двух жезлов пробитие препятствий/целей задано намеренно. Terra переносится с одним уроном 5 каждой цели за cast, а не с повторным уроном каждой точке сэмплирования исходного луча. Ruby сохраняет сквозной луч через любые блоки.

Loot задаёт Tiny Ruby lore keys `wand_ruby_tiny.lore.1/.2`, затем обычные `wand_ruby.lore.3/.4`; Tiny Terra аналогично использует `wand_terra_tiny.lore.1/.2` и обычные `wand_terra.lore.3/.4`. В актуальном resource pack Ruby tail описывает превращение выпавших гримуаров в свитки, Terra tail — превращение выпавших предметов в еловую листву. Эти тексты соответствуют отдельным crafting dependencies выше, а не эффектам cast. Tiny Ruby lore1/2 имеет английские значения `Vsplshchy Vkm` / `Vstrlvt rzrshshchy mgy` и русские `Вспглщщй Вкм` / `Встрлвт рзршщй мгй` (сокращённые безгласные записи «всепоглощающий вакуум» / «выстреливает разрушающей магией»); Tiny Terra — `Sl prrd` / `Strlt Trr-lchm` и `Сл пррд` / `Стрлт Трр-лчм` (сокращённые «сила природы» / «стреляет Терра-лучом»). Ruby описывает разрушительный след лишь в общем смысле; Terra указывает луч/урон, но не объясняет повторный урон и terminal moss/grass. Сохранять именно Tiny-ключи и их прежнее оформление при переносе.

Исходник Ruby всё ещё буквально уничтожает блоки по линии (`fill ... air destroy`), а `ruby_end` остаётся недостижимым; это детали старого datapack, отдельно от пользовательского требования о сквозном луче. Исходник Terra вызывает 5 cactus damage на каждом sample и продолжает после entity hit, но переносимый контракт — один удар каждой цели за cast из пользовательского решения выше.

## Основные источники

- `function/workingitems/spell1.mcfunction`, `spell1_default.mcfunction`, `spell1_default_left.mcfunction`, `items/spells.mcfunction`, `config.mcfunction`.
- `function/workingitems/sauvojen_staffs_tiny/{ruby,ruby_2,ruby_end,terra,terra_2,terra_end}.mcfunction`.
- `loot_table/random_sauvojen_wand_tiny.json`; `items/trinket_effects/cast_wand.mcfunction`; `function/other/craft_ground.mcfunction`, `tick1s.mcfunction`, `function/other/craft/{crafted_spell,crafted_spruce_leaves}.mcfunction`.
- Ресурс-пак только для чтения: `assets/minecraft/lang/en_us.json` и `ru_ru.json`, keys `wand_ruby_tiny*`, `wand_terra_tiny*`, `wand_ruby.lore.3/.4`, `wand_terra.lore.3/.4`.
