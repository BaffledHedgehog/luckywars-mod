from pathlib import Path

p = Path('agent_notes/lbc_other_world_tech.md')
s = p.read_text(encoding='utf8')
edits = {
    'максимум получается12 любых типов вместе': 'локальный предел получается12 любых типов вместе в радиусе30; удалённые wisps этим ограничением не учитываются',
    'teleport ставит display в1,5 блока вокруг глаз владельца': 'teleport ставит display на круг радиуса1,5 блока с центром на1 блок выше глаз владельца',
    'вода даёт `+0,4/+0,7/+0,9/+1,0` к базовому max health, при5+ — `0,8+0,05n`': 'вода использует `add_multiplied_base`: увеличивает базовый max health на40/70/90/100% при1/2/3/4 wisps, при5+ — на `(80+5n)%`',
    'Shadow каждый тик заряжает по `n` до500; при100 снимает100 и перезаряжает cooldown на `100−7n` тиков, уничтожив ближайший чужой projectile в10 блоках, если он первый по selector.': 'Shadow при нулевом cooldown каждый тик добавляет `n` заряда, пока заряд меньше `100n`, затем ограничивает его сверху500; прибавление может слегка перескочить `100n`. Поглощение проверяется при заряде≥100 независимо от cooldown: выбирается один projectile в10 блоках без сортировки, и только если его Owner отличается от UUID игрока, projectile уничтожается, расходуется100 заряда и cooldown ставится в `100−7n` тиков. Cooldown задерживает пополнение, но не расход уже накопленного заряда. Свой случайно выбранный projectile может помешать поглотить находящийся рядом чужой.',
    'число духов открывает последовательно четыре группы очистки effects/scores.': 'число духов открывает накопительно четыре группы очистки: с1 spirit — Hunger/Poison/Wither; с2 также Blindness/Darkness/Glowing и score `piss`; с3 также Mining Fatigue/Weakness/Unluck; с4 также24 отрицательных score, в том числе снятие маны, отложенная смерть, взрывы, молнии, ловушки, timestop и `nomagic` (полный перечень — в карточке `remove_debuff/4`).',
    'начинает секундный нет, фактически каждотиковый scheduler': 'запускает каждотиковый scheduler',
}
for old, new in edits.items():
    assert old in s, old
    s = s.replace(old, new)
old = 'Blue marker с названием green и настоящий `nexus_wire_blue` совпадают по source. Red branch добавляет20 XP points, 5000 mana, +5 к `swordcharged1..7` и +100 `nanobots_tier`. Green ветка выдаёт `lbc:random_glack`; black branch выбирает координаты X/Z−2000..2000 и Y0..319, временно forceload-ит чанк и ставит End Gateway с exit `[1,15000000,1]`; white branch извлекает показанный converter-item предмет.'
new = 'Все четыре converter-ветки извлекают показанный предмет одинаково: сначала создают item-заглушку TNT, затем копируют Item ближайшего display с тегом `wire_converter_item`. Если подходящего display нет, копирование не выполняется и остаётся TNT. Удаляются все item_display в радиусе1 без фильтра по тегу, затем interaction. Название green соответствует предмету `nexus_wire_blue`.\n\nОтдельные функции `wires_other_worlds` используют провод вне Nexus: red расходует1 провод, добавляет20 XP points, 5000 mana, +5 к `swordcharged1..7` и +100 `nanobots_tier`; green расходует1 blue wire и выдаёт `lbc:random_glack`; black расходует1 провод, выбирает X/Z−2000..2000 и Y0..319, временно forceload-ит чанк и ставит End Gateway с exit `[1,15000000,1]`. Это отдельный путь использования, а не результаты клика по конвертеру. Отдельной функции `wires_other_worlds/white` в источниках нет.'
assert old in s
s = s.replace(old, new)
anchor = 'При создании/удалении пересчитывает свойства владельца.'
assert anchor in s
s = s.replace(anchor, anchor + ' Spawn выбирает display по общему тегу `new` без ограничения дальности или сортировки: если одновременно остаётся несколько таких display, ID владельца может достаться другому display.')
p.write_text(s, encoding='utf8')
