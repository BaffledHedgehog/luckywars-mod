# Terra из Magic Academy: сверка источника

После аудита вопрос о старом TTL 20 тиков решён пользователем (user):

> Умное удаление, без фиксированного срока

Ниже приведено поведение исходника; целевая реализация использует общий
lifetime с обычным Terra. [Перенос](TERRA_ACADEMY_PORT.md).

Источник — только старый datapack `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC` (read-only). Здесь «улучшенный» означает Academy-upgraded `stterra`, не отдельный `true_terra` awakening.

## Получение и переключение

- Основной предмет — carrot-on-a-stick, `item_model=lbc:st/terra`, custom data `stterra:1`; выдача показана в [`data/lbc/function/other/sauvojen_tuntija/drop_wand.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/drop_wand.mcfunction). Для каста `config.mcfunction` задаёт 1000 mana.
- Magic Academy использует [`interacted_2.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction) / [`interacted_upgrade.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction). Общий `can_upgrade_book` ставит `upgrade_available:1,upgradable:0`; `interacted_upgrade` добавляет `upgraded:1,mode:1` и lore `upgraded_item`, затем `upgrade/stterra` применяет tier A. `can_upgrade/stterra` только показывает translation. Полный payload разобран в [TERRA_ACADEMY_RESOURCES.md](TERRA_ACADEMY_RESOURCES.md).
- В видимых функциях `upgrade/stterra` нет проверки ресурсов, маны или уровня; точные внешние условия вызова Academy обработчика этим фрагментом не установлены. Сохранять как неопределённость, не придумывать цену.
- Связанная Academy advancement `data/lbc/advancement/true_advancements/legends/academy/terra.json` выдаётся inventory_changed за `upgraded:1,transformable:1,staff:1,stterra:1` и исключает spectator через `playerGameType:3`; это критерий toast/advancement, а не cast/upgrade gate. Creative имеет GameType 1; источник не задаёт для него исключение.
- Обычный spell dispatch выбирает `terra` либо `terra_upgraded_pre` по `spell1_default` / `spell1_upgraded`. В обоих случаях нужны `stterra:1`, минимум 1000 mana и отсутствие `stopper_magic` в радиусе 20. Источники: [`spell1_default.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_default.mcfunction), [`spell1_upgraded.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_upgraded.mcfunction).
- Каст regular `terra.mcfunction` при sneak + `upgradable:1` вместо луча требует стоять над grass block; иначе выводит `not_on_grass`. На grass запускает Academy-проверку с шансом 20% через `terra_preloop_check_upgrade`. Сам cast списывает те же 1000 mana. Это условие ветви/повторного улучшения, не новая цена улучшенного луча.

## Разница лучей

| Поле | Regular `stterra` | Academy-upgraded `stterra` |
|---|---|---|
| Снаряд | marker `raycastterra`, TTL 200 ticks | marker `fast_terra`, TTL 20 ticks |
| Скорость и предел на тик | `raycast_terra`: шаг 0.3, до Distance 7; terminal reset/teleport после 6 шагов ≈1.8 блока/тик | `raycast_terra_fast`: шаг 0.3, до Distance 26; terminal после 25 шагов ≈7.5 блока/тик |
| Урон / piercing | 8 cactus damage по кандидатам в sampled cell (до двух за вызов); продолжается сквозь цели | 17 cactus damage по кандидатам в sampled cell (до двух); продолжается сквозь цели |
| Terrain terminal | при блоке вызывает `placegrass`, создаёт травяные patches и убивает marker | при блоке вызывает `placeflowers`, создаёт цветы/patches и убивает marker |
| Particle | зелёный переходный dust, 1 на шаг | светло-зелёный переходный dust, 1 на шаг |
| Shield / время | tags `shield_blockable2,magic,slowraycast`; общий raycast/shield dispatch | те же tags; dispatch `raycastselector_rare` → `raycast_terra_fast` |

Источники: [`terra_preloop.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/terra_preloop.mcfunction), [`terra_preloop_upgraded.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/terra_preloop_upgraded.mcfunction), [`raycastselector_player.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/raycastselector_player.mcfunction), [`raycastselector_rare.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/raycastselector_rare.mcfunction), [`raycast_terra.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_terra.mcfunction), [`raycast_terra_fast.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_terra_fast.mcfunction).

Оба старта играют `block.amethyst_cluster.break` (regular в `terra_preloop`, upgraded в `terra_preloop_upgraded`). Оба копируют `lbcID2` и `team_number` в marker. `dmg_bypl_8` / `dmg_bypl_11` применяют ownership/team nodamage-фильтр через `dmgwithid_un_playeronly`; targets не завершают луч. Общий tick не двигает `slowraycast` при `time_stopped`. Соответствующие источники: [`dmg_bypl_8.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/dmg_bypl_8.mcfunction), [`dmg_bypl_11.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/dmg_bypl_11.mcfunction), [`dmgwithid_un_playeronly.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/dmgwithid_un_playeronly.mcfunction), [`tick.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick.mcfunction).

## Вопросы к источнику

- Academy item upgrade обрабатывает идентификатор `stterra`, но cast functions для обоих уровней всё ещё списывают 1000. В исходниках, доступных здесь, не найден отдельный mana multiplier.
- Старые TTL равны 200/20 ticks; политика нового мода для regular wand уже отдельно уточнена пользователем как smart lifetime без границ карты. Для Academy-upgraded lifetime пользовательская политика здесь не задана: спросить перед заменой исходного TTL 20.
- `dmg_bypl_8` после общих действий вызывает `can_upgrade` с Prisma item predicates и меняет challenge/Academy side effects на Terra-hit пути; это выглядит как скопированная Prism-логика, не переносить молча.
- Модель `lbc:terrasword_true_charge` отсутствует в datapack source; целевая привязка Academy-upgraded wand задана пользовательской пометкой в [`TERRA_PROJECTILE_MODELS.md`](TERRA_PROJECTILE_MODELS.md). Отдельный `true_terra` awakening sword имеет собственную ветку и не является источником Academy-upgraded cast.

## Flower placement и 26.3

- Проверены локальные `C:/Users/yarik_temp/.gradle/caches/fabric-loom/26.3/minecraft-merged.jar` и единственный старый baseline `C:/Users/yarik_temp/Desktop/bingoserverfabric/versions/1.21.8/server-1.21.8.jar`. Старый `data/minecraft/worldgen/configured_feature/*.json` подтверждает, что вызовы `place feature` в [`data/lbcr/function/placeflowers.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/placeflowers.mcfunction) — random-patch flower configs, а не одиночный блок.
- В 26.3 существуют все raw Feature IDs: `flower_cherry`, `flower_flower_forest`, `flower_swamp`, `flower_meadow`, `flower_default`, `flower_plain`, плюс `grass_jungle`. `patch_grass_jungle` существует только как PlacedFeature, с дополнительными count/heightmap/biome/worldgen placement; для импакт-эффекта использовать raw `grass_jungle` Feature.
- Старые поля flower конфигов (tries, xz/y spread): cherry 96/6/2; flower_forest 96/6/2; swamp 64/6/2; meadow 96/6/2; default 64/7/3; plain 64/6/2. У каждого child placement-фильтр `air`.
- Providers берутся из самих современный Feature holders, без самодельного выбора цветка: cherry weighted 16 pink-petal states; forest noise provider (11 flower states); swamp blue orchid; meadow dual-noise (8 states, включая tall-grass lower/short grass); default poppy:dandelion 2:1; plain noise-threshold provider. Старый `patch_grass_jungle`: grass:fern 3:1, tries 32, spread 7/3, фильтры air и «не podzol снизу»; современный `grass_jungle` holder содержит тот же weighted provider.
- Нативная схема: для каждого raw Feature holder собрать `PlacedFeature` с `CountPlacement.of(tries)`, `OffsetPlacement.of(TrapezoidInt.of(-spreadXZ, spreadXZ, 0), TrapezoidInt.of(-spreadY, spreadY, 0))` и соответствующими `BlockPredicateFilter`; затем вызвать `.place(level, chunkGenerator, random, origin)`. Для jungle составной фильтр = air AND NOT podzol с offset `(0,-1,0)`. Это воспроизводит random-patch distribution (разность двух `nextInt(spread+1)`), не добавляет worldgen biome/in-square offsets.
- В upgraded Terra particle source `to_color` содержит green `2.0`. 26.3 `RGB_COLOR_CODEC` использует свободный `VECTOR3F` и при создании int RGB применяет `floor(channel*255)` затем 8-bit mask, не clamp: `2.0 → 510 → 254` (`ARGB.as8BitChannel`/`ARGB.color`). Packed particle color не может сохранить HDR. Старый 1.21.8 server JAR не содержит named class для независимой проверки старого runtime rendering; не называть значение HDR без проверки и не менять его молча.
