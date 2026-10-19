# Tiny Electro — source contract

Источник: старый `world/datapacksdisabled/LBC/data`; это описание наблюдаемой логики datapack, не подтверждение поведения нового порта.

## Cast и стартовая молния

- Обычная и off-hand ветки в `data/lbc/function/workingitems/spell1_default{,_left}.mcfunction` требуют `mana >= tiny_electro_wand_mana_use` (значение `100` в `config.mcfunction`), исключают stopper `distance=..20` и вызывают `sauvojen_staffs_tiny/electro` из позиции глаз.
- `spell1.mcfunction` / `spell1_left.mcfunction` обнуляют caster `Distance` перед default dispatch. `electro` синхронно проходит `electro_2`, затем играет `firework_rocket.twinkle`, списывает 100 mana, применяет Mage full-set `manamax += mage_set_bonus` и вызывает trinket hook при включённом флаге.
- `electro_2` делает шаги по `^ ^ ^.35`, добавляя `Distance` на каждом шаге; максимум — около 100 шагов/35 блоков. На попадании в моба в текущей клетке (кроме `raycaster` и `spectator`) либо на не-air блок endpoint ставится на один блок назад (`^-1`). Эти два terminal-условия независимы: моб у стены может вызвать `electro_end` дважды в одном шаге. В новом порте это известный source-баг, ранее пользователь потребовал единственный terminal; не переносить двойное срабатывание.
- `electro_end` играет twinkle и создаёт marker с `lifetime:40`, тегами `raycastmolnia`, `slowraycast`, `shield_blockable`, `mecha`, `player`; копирует Rotation, `lbcID2` и `team_number` caster-а, затем помечает marker `actived`. Marker начинает lightning-ray из найденной endpoint-позиции.

## Живой тик, дальность и turn

`tick.mcfunction` вызывает `tick_not_timestopped`, только когда datapack time-stop не активен. Там каждый `#minecraft:raycasters[tag=slowraycast]` ровно раз за обычный тик проходит `raycastselector`; selector сначала увеличивает `Lifetime`, затем вызывает `raycastselector_player` → `raycast_molnia`, а после dispatch удаляет сущность при `Lifetime >= data.lifetime`. Поэтому marker получает максимум 40 таких игровых вызовов; во время datapack time-stop этот pipeline не вызывается.

`raycast_molnia` на каждом вызове прибавляет `Distance`, рисует dust/electric-spark, играет тихий twinkle и проверяет мобов в текущей клетке. Пока `Distance=1..10`, функция рекурсивно перемещается на `^ ^ ^.2`: это 11 sample positions `Distance=1..11` (10 перемещений), суммарно 2 блока за тик; эффекты и twinkle также вызываются 11 раз за marker-tick. На `Distance=11` она вызывает `molnia_prelomlenie_pre`: позиция сохраняется, Distance сбрасывается в 0, `universal_razbros_small` независимо добавляет к yaw и pitch случайное значение ±8°; движение продолжается в следующем tick dispatch. Это случайный поворот существующего луча каждые ~2 блока, не поиск нового target и не отдельный chain marker. Отдельный `raycast_molnia_chain` Tiny Electro не вызывает. При пустом пути лимит 40 тиков даёт порядка 80 блоков продвижения; столкновения/убийство сокращают путь.

## Hit, conductor/water, shield

- В обычном sample `raycast_molnia` при наличии моба в клетке запускает `dmg_bypl_0_5nocd`: выбирает до двух сущностей `#minecraft:mobs` в клетке (tag включает `minecraft:player`), исключая `spectator`, `nodamage`, `nodamage1`, и пытается нанести каждому `0.5 arrow` от owner player-marker. Helper заранее помечает игроков того же `lbcID2` и команды; обычный lightning hit таким образом фильтрует caster/team. Проверка повторяется на каждом sample, так что попадание может породить повторные 0.5 damage attempts при последующих samples/ticks; фактический результат также зависит от native hurt cooldown.
- Вода вызывает `raycast_electro42`; этот же вызов отдельно выполняется для любого non-air блока. `minecraft:water` включена в `data/minecraft/tags/block/electroprovod.json`, поэтому водная клетка удовлетворяет обоим условиям и дважды вызывает `raycast_electro42` в одном `raycast_molnia`. Для conductive-блоков helper рисует 5 end-rod частиц и вызывает `raycast_electro_electroprovod`; тот сканирует мобов радиусом 10 (исключает spectator и `rcdenyalways`) и на каждого делает 1 generic damage плюс 4 firework частицы. Эта ветка не вызывает owner/team helper и не исключает owner/allies; на воде conductor callback делается дважды, а поскольку он не убивает marker, повторяется на каждом sample, пока луч идёт внутри проводящей клетки (примерно пять samples на блок при шаге 0.2). Это реальные source-особенности/подозрительные места, а не подтверждённое желаемое поведение. Для non-conductor блоков helper убивает marker.
- `shield_block` вызывается advancement `lbc:any_shield` по событию `minecraft:using_item` для shield, кроме измерений nexus/imprinted. Перед `shield_block_2` контекст сдвинут на один блок по взгляду игрока. В радиусе 2 проверяются `shield_blockable` marker-ы; owner ID или тот же `team_number` добавляет `donotblock`, остальные marker-ы сразу убиваются (и проигрывается shield sound). Tiny Electro использует именно этот tag.

## Пути источника

- `data/lbc/function/workingitems/{spell1_default,spell1_default_left,spell1,spell1_left}.mcfunction`
- `data/lbc/function/workingitems/sauvojen_staffs_tiny/{electro,electro_2,electro_end}.mcfunction`; `data/lbc/function/config.mcfunction`
- `data/lbc/function/{tick,tick_not_timestopped,raycastselector,raycastselector_player}.mcfunction`
- `data/lbcr/function/{raycast_molnia,molnia_prelomlenie_pre,raycast_electro42,raycast_electro_electroprovod,raycast_electro_electroprovod_damage,dmg_bypl_0_5nocd,dmgwithid_un_playeronly,universal_razbros_small}.mcfunction`
- `data/lbc/function/items/shields/{shield_block,shield_block_2}.mcfunction`; `data/lbc/advancement/any_shield.json`; `data/minecraft/tags/block/electroprovod.json`

