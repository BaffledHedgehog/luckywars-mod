import json,re
from pathlib import Path
N=Path('agent_notes');jobs=json.loads((N/'inventory/research_jobs.json').read_text(encoding='utf-8'))
accepted=[k for k,v in jobs.items() if v.get('content_reviewed')]
remaining=[k for k,v in jobs.items() if not v.get('content_reviewed')]
p=N/'RESEARCH_STATUS.md';s=p.read_text(encoding='utf-8')
s=re.sub(r'Проверено содержание [^\n]+\nразделов из 35;',f'Проверено содержание {len(accepted)}\nразделов из 35;',s)
rows={
'lbc_drop_effects':'Полные peer-проходы сохранены. Финальный аудит четырёх групп other: consumables/craft, magic, misc и main; отчёт review_final_other_groups.md.',
'skywars_game':'Игра, GUI и runtime приняты. Проверяет сквозные связи выбора, выдачи, событий и reset; итог INTEGRATION_REVIEW.md.',
'maps_catalogue':'Weapons приняты. Финальная проверка текстов kit_game, LBCR, other_imba и четырёх workingitems-групп; итог review_final_text_groups.md.',
'Координатор':f'Принял {len(accepted)} из {len(jobs)} разделов. Последние: world/tech, GUI, пять JSON-групп, weapons, attributes. Исправления и основания: review_world_tech_root, review_gui_root, review_typed_json_root, review_weapons_root, review_attributes_root. Принимает оставшиеся {len(remaining)} текстовых групп и сквозную интеграцию.'}
for k,v in rows.items():
    s,n=re.subn(r'^\| `'+re.escape(k)+r'` \|.*$',f'| `{k}` | {v} |',s,flags=re.M);assert n==1,k
s=s.replace('Исследователи используют дешёвую модель `gpt-6-luna`. Одновременно работают\nтри исследователя;', 'Исследователи используют дешёвую модель `gpt-6-luna`. Задачи распределены между\nтремя исследователями; лимит сервиса может временно отклонять дополнительные проходы;')
p.write_text(s,encoding='utf-8')
p=N/'lbc_core_items.md';s=p.read_text(encoding='utf-8')
s=s.replace('диспетчер `drops/specialdrops` и часть файлов `load`.', 'диспетчер `drops/specialdrops`; полный lifecycle вынесен в [lbc_runtime.md](lbc_runtime.md).')
s=s.replace('лежащие в исключённых из этого участка', 'лежащие в других тематических участках')
s=s.replace('namespace/scoreboard `swrg.math`, команды `#lang`, `#gamemode`, `#developer`', 'objective `swrg.math`, его fake-player holders `#lang`, `#gamemode`, `#developer`')
s=s.replace('Эта часть `load` длинная; перечисленное не означает, что все её строки (включая полный список objectives и сбросы) уже сверены. Полный разбор lifecycle верхнего уровня остаётся незавершённым.', 'Полный разбор load, objectives, буферов, периодических циклов, timestop и reset находится в [lbc_runtime.md](lbc_runtime.md); он принят по отдельному аудиту. Здесь оставлен краткий вход в диспетчер предметов.')
start=s.find('## Зависимости и пробелы')
assert start>=0
s=s[:start]+'''## Где искать конечное действие

- Тики, мана, смерть/выход и cleanup: [runtime](lbc_runtime.md), [mana](lbc_mana.md).
- Дропы, постройки, ловушки и TMT: [drops](lbc_drops.md).
- Преобразование/крафт предметов: [other main](lbc_other_main.md), [consumables/craft](lbc_other_consumables_craft.md).
- Imba-предметы и артефакты: [other imba](lbc_other_imba.md), [workingitems other](lbc_workingitems_other.md).
- Multiply: [kit interface](lbc_kit_interface.md); способности/выдача: [kit game](lbc_kit_game.md).
- Все входные JSON: [advancements](lbc_advancements.md), [loot](lbc_loot_definitions.md), [predicates/modifiers](lbc_predicates_modifiers.md), [registry](lbc_registry_support.md).

Это обзор маршрутов, а не отдельный незавершённый участок. Приёмка конкретных
документов отражена в [RESEARCH_PROGRESS.md](RESEARCH_PROGRESS.md).
Реально отсутствующие ресурсы и внешние команды перечислены в
[RESOURCE_BOUNDARIES.md](RESOURCE_BOUNDARIES.md) и [COMMAND_BRIDGES.md](COMMAND_BRIDGES.md).
'''
p.write_text(s,encoding='utf-8')
print(f'Checkpoint {len(accepted)}/{len(jobs)}; stale core status replaced by actual document links')
