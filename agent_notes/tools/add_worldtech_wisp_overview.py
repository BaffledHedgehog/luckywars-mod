from pathlib import Path
p=Path('agent_notes/lbc_other_world_tech.md'); s=p.read_text(encoding='utf8')
block='''
## Wisp book: принадлежность, круг и пассивные ветки

Функции в `wisp_book` работают с шестью типами display-wisp: air, fire, water, nature, light и shadow. Их владельца определяют по scoreboard `lbcID2`; при перестройке круга `realign_wisps/start` считает только display с ID игрока, вычисляет угол `360 / числу wisps`, назначает каждому равный шаг, затем снимает временный `check_id`. Тик каждой wisp отвечает за соответствующую частицу, а пассивные ветки разнесены по периодам: игрокам вызываются отдельные elemental handlers в `players_1s`, `players_2s` и `players_2t`; это не один одинаковый эффект для каждого духа. При выходе/смерти `leave_death` вызывает очистку `all` для всех шести типов, поэтому привязанные к игроку wisps удаляются. Сами эффекты и пороги описаны по конкретным elemental handlers ниже; [перестройка круга](../world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/start.mcfunction), [частицы на wisps](../world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/tick.mcfunction), [очистка при выходе](../world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction).
'''
if '## Wisp book: принадлежность, круг и пассивные ветки' not in s:
 i=s.index('\n## Файлы:'); s=s[:i]+'\n'+block+s[i:]
p.write_text(s,encoding='utf8')
