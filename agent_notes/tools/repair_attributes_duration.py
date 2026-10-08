from pathlib import Path
p=Path('agent_notes/lbc_other_attributes.md'); s=p.read_text(encoding='utf8')
s=s.replace('nausea 3 ticks и blindness 2 ticks','nausea 3 секунды и blindness 2 секунды')
needle=' Для `movement_speed` обычный шаг ±2000 при scale 100000 (±0.02), tiny +100 (0.001).'
addition=' `attribute base get S` сохраняет в scoreboard целое квантованное значение, поэтому таблица `delta/S` показывает шаг после квантования, а не гарантирует абсолютную дельту от любой дробной исходной базы.'
if addition not in s:s=s.replace(needle,needle+addition)
p.write_text(s,encoding='utf8')
# Drop helpers copy the item; the honey_pl caller owns the destructive cleanup.
old='Семейство `drop_inv` создаёт рядом с вызывающим сущность item на основе временного `minecraft:stone`, копирует поле `Item` из выбранного слота инвентаря/экипировки игрока, затем снимает временный tag `modifyitem`. У выброшенного предмета `PickupDelay:40`. Варианты различаются источником поля: `0..35` — слоты inventory, `100..103` — boots/legs/chest/head; `-106` — offhand. Ниже перечислены все точные варианты.'
new='Каждый `drop_inv` helper создаёт рядом item entity на основе временного `minecraft:stone`, копирует в неё поле `Item` из выбранного слота и снимает временный tag `modifyitem`. Сам helper исходный слот не очищает: ручной вызов оставляет оригинал рядом с копией. Caller `projectiles/gadgets/honey_pl` вызывает все 41 helper (slots 0..35, offhand и четыре armor slots), затем сам заменяет inventory/offhand/armor игрока на air; очистка выполняется caller-ом. `PickupDelay:40` равен 40 game ticks (2 секунды). Варианты различаются источником поля: `0..35` — inventory, `100..103` — boots/legs/chest/head, `-106` — offhand.'
p=Path('agent_notes/lbc_other_attributes.md'); s=p.read_text(encoding='utf8').replace(old,new); p.write_text(s,encoding='utf8')
# Keep the generator text accurate as well.
p=Path('agent_notes/tools/gen_lbc_other_attributes.py'); s=p.read_text(encoding='utf8').replace(old,new); p.write_text(s,encoding='utf8')
# Keep the refinement builder from reintroducing the wrong unit or omitting quantization.
p=Path('agent_notes/tools/refine_lbc_other_attributes.py'); s=p.read_text(encoding='utf8')
s=s.replace('nausea 3 ticks и blindness 2 ticks','nausea 3 seconds и blindness 2 seconds')
s=s.replace('nausea 3 ticks and blindness 2 ticks','nausea 3 seconds and blindness 2 seconds')
p.write_text(s,encoding='utf8')
