# Energyblock: исходник LBC

Источник только для чтения: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC`.
Исследование описывает найденный код datapack; расположение в `datapacksdisabled`
не доказывает, что эта копия сейчас исполняется. Exasperato в текущую ветку
переноса не входит.

## Предмет и получение

- Исходный Electro жезл — carrot-on-a-stick из `loot_table/sauvojen_tuntija_electro.json`,
  custom data `stelectro:1`. Исходный энергетический щит — `minecraft:shield`
  из `loot_table/electro_shield.json`, custom data содержит `energy_shield:1`.
- Ground craft в `function/other/craft_ground.mcfunction` требует Electro и
  выброшенный energy shield в радиусе 1. Inventory craft в
  `function/other/craft/inventory_craft_check.mcfunction` требует те же флаги
  в `player.crafting.*`. Оба вызывают `function/other/sauvojen_tuntija/crafted_energyblock`;
  inventory-вариант после выдачи уменьшает по единице во всех подходящих ячейках
  crafting каждого из двух ингредиентов. Это старые сканеры, не нативный рецепт.
- Результат loot `loot_table/tuntijaa_electro_shield.json`: repeating command
  block с моделью `lbc:electroblocker`, custom data
  `{transformable:1,accessory:1,electroblocker:1,instantitem:1}`. Значит,
  energyblock/electroblocker — отдельный аксессуар Electro; он не является ни
  исходным `energy_shield`, ни Exasperato.

## Вход и охват слотов

- `players_tick.mcfunction` вызывает `other/instants/select` лишь для игрока с
  тегом `instantitem_active`. `items/changed_inventory/sel.mcfunction` ставит
  этот тег при наличии `instantitem:1` в `container.*`, `weapon.*` или `armor.*`
  и снимает его, когда предметов нет в этих слотах.
- В `other/instants/select.mcfunction` вызов старого/нового instant selector
  зависит от `#lbcskill` и `lbc.skill`; после этих веток безусловно проверяется
  `Inventory` игрока на `electroblocker:1`. Если предмет найден, выполняется
  `electroblock/tick`, кроме случая, когда в пределах 20 блоков есть marker с
  `stopper`.
- Проверены именно `container.*`, `weapon.*`, `armor.*` для поддержания
  `instantitem_active`, затем NBT `Inventory[]` для самого block tick. Отдельной
  проверки cursor, crafting slots или ender chest для `electroblocker` не найдено.

## Tick: снаряды и урон

- `electroblock/tick.mcfunction` имеет две активные ветки в радиусе 14. Ветка
  `#minecraft:projectiles` выбирает только `same_id` projectiles без `mirrored`,
  `accelerated`, `edited3`, затем вызывает `tick_owner`. `tick_owner` не наносит
  урон: он удваивает Motion, включает `NoGravity` и ставит `accelerated`.
- Вторая ветка использует `#raycasters_projectiles` из
  `data/minecraft/tags/entity_type/raycasters_projectiles.json`; это union
  `#minecraft:raycasters` и `#minecraft:projectiles`. Поэтому отражает как
  кастомные raycasters, так и обычные Minecraft projectiles. В диапазоне 14
  отбираются не `same_id`, не `same_team`, не `ice_crystal`; вызывается
  `repell_new`, после чего снаряду копируются `lbcID2` и `team_number` носителя.
  `repell_new` инвертирует Motion (эффективный множитель −1), поворачивает
  снаряд по собственному направлению взгляда и сдвигает его вперёд на 0.1;
  ставит `mirrored` и создаёт electric spark. Вызов не проверяет `mirrored`, так
  что повторное отражение возможно на каждом тике, пока снаряд удовлетворяет
  селекторам в радиусе.
- После обеих веток, уже от имени и в позиции носителя, `tick.mcfunction`
  вызывает `damage` для ближайшей допустимой цели на расстоянии `0.01..2.5`:
  `0.1 arrow by @s`. Исключены spectator, same-team и same-id. Это источник
  маленького урона; его не наносит `tick_owner` и он не привязан к позиции
  снаряда. Команда выполняется от имени носителя (`@s`); это прямой нативный
  DamageSource с носителем как атакующим.
- Старые ветки `raycasters`, `tick_owner_rc`, `repell`, `tick_rc` и
  `tick_accelerate` закомментированы. Пассивный tick продолжается по наличию
  instantitem-аксессуара и отсутствию stopper; отдельного срока жизни предмета
  здесь нет.

## Что не включать в поведение energyblock

`items/shields/energy_shield.mcfunction` — другая, advancement-triggered
механика исходного shield: при незамороженном времени она отзывает advancement,
затем `energy_shield_2` обрабатывает `raycasters_projectiles` в радиусе 7 через
`energy_shield_proj`, инвертирует Motion и копирует UUID владельца, `lbcID2` и
`team_number`; флаг `mirrored` снимается за радиусом 14. Она не является
пассивным эффектом аксессуара energyblock. `energy_shield_rayc.mcfunction` в
этой цепочке вызова не обнаружена.
