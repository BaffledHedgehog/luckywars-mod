from pathlib import Path
p=Path('agent_notes/lbc_other_imba.md'); s=p.read_text(encoding='utf8')
block='''
## Генерация Cunny sign и макро-случайные тексты

`correction_cunny` сначала смещает координаты на 10 блоков через `universal_pos_razb_10`, запускает сборку знака и затем удаляет свой marker. В `correction_cunny_uoh` равномерно выбирается древесина из 12 значений и rotation 0..15; отдельно дважды выбирается номер 1..100. Каждый номер передаётся как macro `$(t)` в соответствующий literal-файл pasta, который задаёт четыре строки; первая четвёрка сохраняется в `t12..t42`, чтобы второй выбор стал текстом обратной стороны. Макрос ставит один вощёный знак с подсветкой и обеими текстовыми сторонами, создаёт loot-barrel под исходной точкой и четыре marker-луча на расстоянии 5 по сторонам света. Все 100 файлов `pasta/1..100` — варианты текста, а не 100 самостоятельных входов; таблица каждого варианта расположена ниже. [Сборка](../world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/correction_cunny_uoh.mcfunction), [выбор макроса](../world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/correction_cunny_uoh_pasta_select.mcfunction), [установка знака](../world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/correction_cunny_uoh_kanikani.mcfunction), [переход к сборке](../world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/correction_cunny.mcfunction).
'''
if '## Генерация Cunny sign и макро-случайные тексты' not in s:
 i=s.index('\n## Файлы:'); s=s[:i]+'\n'+block+s[i:]
p.write_text(s,encoding='utf8')
