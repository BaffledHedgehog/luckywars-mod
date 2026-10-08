"""One-shot fixes verified independently of the peer prose."""
from pathlib import Path
r=Path(__file__).resolve().parents[2]
p=r/'agent_notes/lbc_items_arcana_trinkets.md';s=p.read_text(encoding='utf-8')
s=s.replace('с поворотом −13°','со смещением−13 по X/Y/Z (rotation/mirror none)')
anchor='## Боевые, статусные и перемещенческие hooks'
text='''## Eyelaser: обычная и Mellstroy-ветки

Mainhand arcanum с eyelaser и Troll Mask6 через trollmasksel запускают eyelaser_pre: raycaster-tag, Distance0 и луч из глаз, без списания mana в этих helpers. Основной eyelaser делает синхронные шаги0.5: при исходном0 обрабатывает201 точку (0..100 блоков). На каждом шаге наносит0.05 generic одной mob-цели limit1 без sort, исключая raycaster/spectator. Дополнительные две куб-проверки с offset−0.99 ограничивают попадание пересечением hitbox. Союзники здесь не исключены. Не-air вызывает eyelaser_kill, который **только ставит Distance201**; это прекращает дальнейшую рекурсию основного варианта. При окончании pre снимает raycaster.

Отдельный eyelaser1 вызывает **Mellstroy armor tick**, а не Troll Mask6. Он исключает same_team/same_id по временным holders, проверяет stopper в20 блоках и при свободной траектории обрабатывает685 точек (до342 блоков). Важный край: общий kill-helper на блоке тоже ставит201, а предел этого варианта684. Поэтому блок **не останавливает** этот луч; при последовательных solid-блоках score каждый раз возвращается201, и дальность может превысить342, пока не прервётся по stopper/лимиту выполнения команд. Это поведение исходника, не обещание успешного бесконечного луча. Урон0.05 за подходящий шаг, отдельного расхода mana нет.

Источники: [pre](../world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/eyelaser_pre.mcfunction), [base](../world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/eyelaser.mcfunction), [вариант1](../world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/eyelaser1.mcfunction), [kill](../world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/eyelaser_kill.mcfunction), [Mellstroy caller](../world/datapacksdisabled/LBC/data/lbc/function/armor/mellstroy_mask/tick.mcfunction), [Troll Mask caller](../world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/trollmasksel.mcfunction).

'''
assert anchor in s and '## Eyelaser:' not in s
s=s.replace(anchor,text+anchor,1);p.write_text(s,encoding='utf-8')
p=r/'agent_notes/review_weapons_arcana.md';s=p.read_text(encoding='utf-8')
s+='''

## Дополнительная проверка координатора: arcana

Координатор прочитал весь arcana/trinkets MD и вернул ошибки Oraora, Neutral
Ball, Sun и win-cycle исследователю; затем проверил поправки напрямую.
Выявлены ещё ошибочный «поворот−13°» (это XYZ offset) и заявленный, но
отсутствовавший раздел Eyelaser. Координатор добавил его по четырём helpers
и обоим callers. Eyelaser1 принадлежит Mellstroy; общий kill-helper ставит201,
поэтому не завершает его вариант с пределом684. Прежний вывод отчёта про
eyelaser1 от Troll Mask и прекращение на блоке отозван. Приёмка201 пока
не завершена; чтение/исправление собственного MD не означает приёмку deps.
'''
p.write_text(s,encoding='utf-8');print('arcana source context and Eyelaser repaired')
