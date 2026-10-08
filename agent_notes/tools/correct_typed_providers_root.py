from pathlib import Path
p=Path('agent_notes/lbc_predicates_modifiers.md')
s=p.read_text(encoding='utf-8')
paras=s.split('\n\n')
for i,v in enumerate(paras):
    if v.startswith('Для атрибутов в этих JSON'):
        paras[i]='Для атрибутов в этих JSON указана операция `add_value`: она прибавляет заданное число напрямую. В 36 шагах `set_attributes` всего 58 amount: 37 providers `minecraft:constant`, 14 `minecraft:score`, один `minecraft:storage` и 6 буквальных чисел. Случайных `uniform` providers здесь нет. Score/storage берут значение указанного источника и применяют `scale`, если он задан. В 34 шагах `replace` отсутствует, в двух задано `true`; все 36 заменяют текущий список модификаторов атрибутов предмета. У `set_count` и `set_enchantments` поле `add` отдельно определяет прибавление вместо установки; в карточках сохранены буквальные параметры.'
p.write_text('\n\n'.join(paras),encoding='utf-8')
p=Path('agent_notes/review_lbc_predicates_modifiers.md');s=p.read_text(encoding='utf-8')
s=s.replace('Во всех 34 `set_attributes` поле `replace` опущено (default `true`). Amount providers: 37 uniform, 15 score/storage, 6 literal.', 'Координатор исправил сводку providers: 36 шагов `set_attributes`, в 34 `replace` опущено (default `true`), в двух явно true. Amount: 37 constant, 14 score, один storage, 6 literal; uniform отсутствует.')
p.write_text(s,encoding='utf-8')
print('Corrected constant providers and 36 set_attributes steps')
