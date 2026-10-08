"""One-shot insertion of directly checked Motion equations and selector notes."""
from pathlib import Path
r=Path(__file__).resolve().parents[2]
p=r/'agent_notes/lbc_runtime.md'
s=p.read_text(encoding='utf-8')
marker='`raycast_vpered_pre` '
pos=s.index(marker)
table='''Все координаты marker берутся после `positioned 0 0 0`. Для таблицы `d` — единичный вектор направления, `v` — прежняя Motion. floor применяется отдельно к читаемым компонентам; ниже формулы показаны до целочисленного округления.

| Helper | Скорость перед уменьшением | Когда уменьшается |
|---|---|---|
| `_vect` | v+0.1d | Если любая компонента вне±10, все три×3/4 до±10. |
| `_vect_1` | v+0.5d | Такие же пределы±10. |
| `_slow_vect` | v+d | Вход в уменьшение при любой компоненте вне±1.5; после входа все три×3/4 до±1. |
| `_slower_vect` | X/Z: v+0.1d; Y: v+0.5d | Верхний блок на Y+1..5 задаёт Y=−1. Внешняя проверка X/Z±10, Y−0.5..10; внутренний helper всё равно проверяет±10 по каждой оси. Поэтому Y=−1 даёт хотя бы один шаг до−0.75, а не обязательно до−0.5. |
| `_slowest_vect` | v+0.1d; Y добавляется только если floor(1000dY)≥−100 | Все три×3/4 при выходе за±10. |
| `_very_slow_vect` | v+tmp×d | tmp приходит из macro; пределы±10. |

Сами `_recur_low/50000/100000` сначала умножают scores на3, затем делят на4 с floor и проверяют **все три оси**. Дополнительного лимита глубины нет. Общие временные `mot0..2` и одинаковый marker-тег `reycaster` не связывают конкретный marker с владельцем: выбирается limit1, затем убиваются все markers с этим тегом.

'''
s=s[:pos]+table+s[pos:]
s=s.replace('Это сходится на конечных числах, но внутренний лимит глубины отсутствует.', 'Операции используют целочисленные scoreboard; отдельного лимита глубины нет.')
s=s.replace('`rand_motion_razbros` добавляет ±1.0 к округлённым десятым', '`rand_motion_razbros` добавляет ±1.0 к floor(Motion×10)/10')
s=s.replace('`player_speed` копирует Position в storage', '`player_speed` копирует Position в storage')
s=s.replace('`reduce_count` убивает entity предмета с count=1, иначе вычитает единицу и вызывает `ultra_reforge`, которая подбирает loot table трансформации по custom_data.category предмета.', '`reduce_count` сначала убивает @s с Item.count=1, затем без return всё равно пробует уменьшить count и вызывает ultra_reforge. Это не else-ветка. Обычные callers вызывают его для count≥2; selector @s сохраняет ссылку на удалённого исполнителя, а изменение item NBT может отдельно изменить последующие совпадения.')
s=s.replace('использование с count≤0 может дать отрицательный stack и не защищено.', 'попытка вычитания не защищена от count≤0; это запись недопустимого count, а не гарантированно существующий отрицательный stack.')
extra='Координаты в `universal_pos_razb_*` перед добавлением смещения берутся с floor: обычные варианты привязывают изменяемую ось к целому блоку, `_0_5/2_/2_motion` — к сотым. Например, X=1.9 и random=0 в варианте со scale1 дадут X=1.0. Неизменяемая Y у XZ-вариантов действительно не записывается. У Rotation-разбросов также сначала floor(угол×100), затем шум и запись×0.01.\n\n'
anchor='`universal_razbros` хранит временные углы'
s=s.replace(anchor,extra+anchor)
p.write_text(s,encoding='utf-8')
notes=r/'agent_notes/SOURCE_FINDINGS.md'
s=notes.read_text(encoding='utf-8')
s+='''
## Удаление исполнителя и продолжение функции

`kill @s` не прекращает mcfunction. Для обычного `@s` parser не добавляет
проверку alive; EntitySelector берёт объект из CommandSourceStack, а не снова
ищет его в мире. Поэтому следующая NBT/tag-команда может выбрать даже уже
удалённого исполнителя. `return`, изменение данных и другие явные условия
проверяются отдельно. Нельзя автоматически обрывать drops-dispatch или
reduce_count после kill. Первичная реализация26.3:
[EntitySelector](inventory/minecraft_entity_selector_26_3_bytecode.txt) и
[EntitySelectorParser](inventory/minecraft_entity_selector_parser_26_3_bytecode.txt).
'''
notes.write_text(s,encoding='utf-8')
print('vector/rounding/kill contracts added')
