"""One-shot prose corrections from coordinator's direct source review."""
from pathlib import Path

root=Path(__file__).resolve().parents[2]
path=root/'agent_notes/lbc_runtime.md'
text=path.read_text(encoding='utf-8')

def paragraph(prefix,replacement):
    global text
    start=text.index(prefix)
    end=text.index('\n\n',start)
    text=text[:start]+replacement+text[end:]

paragraph('`raycast_vpered*` —', '`raycast_vpered*` вычисляют вектор вперёд по Rotation через временный marker: сначала позиция исполнения сбрасывается в 0, затем marker ставится на локальной оси `^ ^ ^N`. Его координаты — направленный вектор от нуля, без мировой позиции владельца. `data get Pos[]` и запись Motion имеют два масштаба, которые перемножаются: базовая скорость получается8, `_3` —3.5, `_big` —7, `_slow` —2, `_slow1` —1, `_slower` —0.5. Подробные варианты и шум перечислены в таблице «Полные параметры общих helper-семейств». `_pre` перед обычным расчётом меняет ID при совпадении с выбранным `@a[...limit=1]`, без `sort=nearest`. `_data` пишет пользовательское `data.motion`, `_storage` получает длину `tmp1` и вызывает storage-разброс.')
paragraph('Варианты `*_vect` складывают', 'Варианты `*_vect` складывают прежнюю Motion с направленным вектором, затем при указанных порогах уменьшают все компоненты рекурсивно. Они тоже создают marker относительно нуля. `_slow_vect` начинает уменьшение только при компоненте за ±1.5, а внутри helper-а уменьшает до ±1; значение1.4 поэтому остаётся1.4. Проверки препятствий сверху есть только у `_slower_vect`, не у `_slow_vect`. Полная таблица суммирования и порогов приведена ниже. `_big_with_tag` копирует Owner/owner из `@a[tag=owner,limit=1]`, без отдельного ID-сопоставления; `_slow_dragon` берёт Rotation у ближайшего `sin_lust`.')
paragraph('`universal_razbros` добавляет', '`universal_razbros` добавляет к yaw/pitch ±30°/±20°; `_big` —±50°/±50°, `_small` —±8°/±8°, `_smaller` —±6°/±6°. `_withpre` сначала телепортирует исполнителя на позицию/поворот командного контекста и добавляет ±30°/±30°, `_withpre_small` —±30°/±5°. Это телепорт на прежнее место только если caller уже задал позицию/Rotation самого исполнителя. `_up_active` прямо задаёт yaw−180..180°, pitch−60..10° и тег `actived`.')
paragraph('`reduce_motion_to_10x` читает', '`reduce_motion_to_10x` читает Motion с масштабом100000; при компоненте вне ±1000000 запускает `_rec`. Каждый шаг делает floor(score/10)×9 для всех трёх осей и повторяет проверку **X, Y и Z**, пока все не окажутся в диапазоне. Это примерно90% прежней скорости за шаг; запись0.00001 возвращает исходные единицы Motion, итоговый предел —±10 на каждую ось. `sqrt_loop` увеличивает `counter` и делает шаг Ньютона floor((floor(x/speed)+speed)/2), повторяя пока counter≤16; при исходном counter0 это17 шагов. Начальные x/speed/counter задаёт caller, нулевой делитель не обрабатывается специально. `get_random` увеличивает #random_max, выдаёт advanced_random loot, считывает amount в #random_out и удаляет все подходящие randomselect items; выбран limit1 без собственного радиуса, поэтому наличие других таких items влияет на выбор.')
text=text.replace('разброс ±`tmp2` из макро-storage','разброс ±`tmp2`/10000 из макро-storage')
text=text.replace('`_storage` использует макро `tmp2`','`_storage` использует макро `tmp2` как целое число с итоговым шумом ±tmp2/10000')
text=text.replace('Motion округляется до десятых','Motion берётся как floor(Motion×10)/10')
text=text.replace('Motion до десятых','floor(Motion×10)/10')
text=text.replace('Motion до тысячных','floor(Motion×1000)/1000')
text=text.replace('Motion до 1/10000','floor(Motion×10000)/10000')
text=text.replace('`abyss_end` сообщает игроку и наносит 100000 void/outside-border damage при Y≤−2,000,000 и ещё 1e12 при Y≤−20,000,000.', '`abyss_end` сообщает всем игрокам о текущем исполнителе и безусловно наносит ему100000 outside_border damage; дополнительный1e12 применяется при score `y≤−20000000`, то есть мировой Y≤−200000 при масштабе×100. Порог входа из player-loop — score y≤−2000000, примерно мировая Y≤−20000; он задан caller-ом, не самой функцией.')
text=text.replace('`_slow_vect` и `_slower_vect` могут задать принудительное вертикальное падение, если блоки мешают сверху;', '`_slower_vect` задаёт принудительную вертикальную скорость при препятствиях в блоках Y+1..5; у `_slow_vect` такой проверки нет;')
text=text.replace('и рекурсирует по условию X вне ±1,000,000', 'и рекурсирует по условиям X, Y либо Z вне ±1,000,000')
text=text.replace('marker-позицию нельзя считать чистым вектором', 'marker-позиция задаёт чистый направленный вектор от нуля, затем к нему добавляется прежняя Motion')
path.write_text(text,encoding='utf-8')

review=root/'agent_notes/review_runtime.md'
s=review.read_text(encoding='utf-8').replace('внешний caller проверяет оси отдельно, внутренняя рекурсия — только X', 'внешний caller и внутренняя рекурсия проверяют все три оси')
review.write_text(s,encoding='utf-8')

drops=root/'agent_notes/lbc_drops.md'
s=drops.read_text(encoding='utf-8').replace('если отдельная ветка убивает источник, последующие проверки, которым нужен живой `@s`, могут его уже не выбрать.', '`kill @s` не равен `return`: текущий command source сохраняет ссылку на исполнителя, а обычный `@s` не добавляет проверку alive. Последующие NBT/tag-ветки могут сработать также после удаления источника; отдельно нужно учитывать изменение его данных вызываемыми функциями.')
drops.write_text(s,encoding='utf-8')
print('runtime and dispatcher prose corrected')
