from pathlib import Path
p=Path('agent_notes/lbc_other_main.md'); s=p.read_text(encoding='utf8')
needle='### `lbc:other/manaregen`'
intro='''### Цикл маны: подробный разбор

Точный разбор автоматического восстановления, отрицательной маны, порогов, overload-веток, эффектов и вложенных tentacles вынесен в [ручной аудит mana handlers](lbc_mana.md). Ниже оставлены исходники из этого job; таблицы расходящихся условий и количеств следует читать по указанному разбору.

'''
if '### Цикл маны: подробный разбор' not in s: s=s.replace(needle,intro+needle)
p.write_text(s,encoding='utf8')
p=Path('agent_notes/lbc_other_consumables_craft.md'); s=p.read_text(encoding='utf8')
needle='### `lbc:other/mana_potions/bad`'
if needle in s and 'ручной аудит mana handlers' not in s:
 s=s.replace(needle,'### Зелье и цикл маны\n\nПодробные расчёты для `manaregen*`, `negative_mana*`, `damage_with_tmp_void`, `speed_up` и `mana_potions/bad` собраны в [ручном аудите mana handlers](lbc_mana.md); этот job сохраняет отдельные item/craft-входы и ссылки на их исходники.\n\n'+needle)
p.write_text(s,encoding='utf8')
