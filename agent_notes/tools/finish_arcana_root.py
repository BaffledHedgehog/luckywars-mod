from pathlib import Path
import json

notes = Path('agent_notes')
p = notes/'lbc_items_arcana_trinkets.md'
s = p.read_text(encoding='utf8')
# Replace the obsolete duplicate, retaining its useful links in the accurate section.
a = s.index('`eyelaser_pre` не списывает mana')
b = s.index('\n\nSun Arcanum',a)
s = s[:a]+'Eyelaser запускается без расхода mana. Обычная и Mellstroy-ветки, их точные пределы и ошибка остановки варианта1 описаны в [отдельном разделе ниже](#eyelaser-обычная-и-mellstroy-ветки).'+s[b:]
edits = {
 'acid при tmp 1 использует chance15, при 2..13 — формулу (22..99%), а при 14+ гарантирован': 'acid при tmp1 использует chance15, при14+ гарантирован. На2..13 рассчитывается22..99%, но helper записывает storage `tmp`, а macro читает `tmpd`: эта ветка не использует рассчитанный шанс. Без внешнего `tmpd` макрос не выполняется; при наличии такого значения результат зависит от него',
 'helper записывает owner/team и целевые координаты, дальнейший удар задаёт дочерняя функция': 'активация сохраняет ID/team во временные score holders, поднимает точку выпуска на3 блока, создаёт prismcheck marker и запускает внешний цветной raycast. При подходящей цели выполняются две отдельные попытки: луч к ближайшей цели ниже игрока и луч вверх. Локальные helpers не копируют ID/team в marker; полёт/урон выполняет `lbcr:raycast_preloop7_2_upgraded`',
 '`select` считывает `spellid` из `custom_data.libspell` ближайшего snowball в 10 блоках': '`select` пытается считать `spellid` у ближайшего snowball без ограничения дальности и без фильтра libspell в selector; проверка `libspell:1` стоит только в NBT-пути. Снежок для удаления выбирается отдельно в10 блоках с фильтром libspell. Поэтому читаемый и удаляемый снежки могут различаться. При неудачном чтении новый spell ID не устанавливается',
 '`select_wand` использует текущий scoreboard `spell`': '`select_wand` сначала читает `SelectedItem.custom_data.spell$(currspell)` из предмета в главной руке, записывая scoreboard `spell`,',
 '`rtp_gas2` переносит игрока `spreadplayers` в центр карты (0,0) в радиус 6': '`rtp_gas2` переносит игрока `spreadplayers` на поверхность в квадрат X/Z±6 вокруг(0,0)',
 '`rtp_gas1` и `rtp_gas21` оставлены runnable helper-ами с радиусом 50 вокруг текущей координаты': '`rtp_gas1` и `rtp_gas21` оставлены runnable helper-ами с квадратом X/Z±50 вокруг текущей координаты',
 'затем поворачивается и перемещается на 1 блок к ближайшей допустимой цели в 40 блоках': 'затем ищет ближайшую допустимую цель в40 блоках и вычисляет направление из точки к цели, прежней Rotation и двух локальных смещений0,1/1; переносит marker на1 блок по полученному направлению',
 '`find_pos` призывает `evoker_fangs`, над motion-blocking поверхностью через 1000 блоков либо в текущей позиции': '`find_pos` проверяет поверхность через временную точку на1000 блоков выше неё: если исходный marker попадает вdistance≤1007 от этой точки, призывает `evoker_fangs` на поверхности; иначе — в исходной позиции',
}
for old,new in edits.items():
    assert old in s, old
    s = s.replace(old,new)
s = s.replace('scoreboard `spell`, и маршрутизирует', 'scoreboard `spell`, и маршрутизирует')
audit = json.loads((notes/'inventory/arcana_routes_chances_audit.json').read_text(encoding='utf8'))
a = s.index('| Spell ID |') if '| Spell ID |' in s else s.index('| ID |')
b = s.index('\n## ',a)
table = ['| ID | Функция | Дополнительный контекст выполнения |','|---|---|---|']
for row in audit['spell_routes']['select_wand']:
    table.append(f"| {row['range']} | `{row['target']}` | `{row['context']}` |" if row['context'] else f"| {row['range']} | `{row['target']}` | исходный |")
table += ['', 'Сверены все141 маршрута `select_wand` и140 маршрутов `select`: отличаются только ID189, который присутствует у wand и отсутствует у snowball. В прежней таблице были пропущены ID179 (bridge) и228 (The End со смещением8); здесь они восстановлены. Обе функции до ID119 обнуляют `check`; после dispatch снимают caster/raycaster. [Полные команды и22 шанс-модели](inventory/arcana_routes_chances_audit.json).', '']
s = s[:a]+'\n'.join(table)+s[b:]
p.write_text(s,encoding='utf8')
print('Arcana prose, both spell routes, and acid macro contract corrected')
