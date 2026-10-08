from pathlib import Path
p=Path('agent_notes/lbc_other_magic.md'); s=p.read_text(encoding='utf8')
block='''
## Академия магии: проверка и применение апгрейда

`can_upgrade` — короткая выдача статуса, а не единое условие покупки: если в руке не `unstable_spellbook`, она передаёт работу в `can_upgrade_book`, который проигрывает звук, показывает `can_upgrade`, временно переносит предмет через блок-хранилище, ставит `upgrade_available:1` и сбрасывает `upgradable:0`. Другие функции вызывают этот сигнал при своих условиях (например, пороги очков/scoreboard, шанс или действие конкретной палочки), поэтому проверку конкретного предмета следует читать в caller.

`interacted_upgrade` — dispatcher по `custom_data` предмета в руке: выставляет `upgraded:1` и `mode:1`, добавляет описание апгрейда, обновляет challenge progress и вызывает соответствующий handler по ключу предмета. В папке `upgrade/` лежат 53 предметных обработчика; они не взаимозаменяемы, их фактический эффект приведён у каждого соответствующего источника ниже. Сами эти функции не задают единую цену/расход ресурсов. [Проверка статуса](../world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade_book.mcfunction), [dispatcher](../world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction).
'''
if '## Академия магии: проверка и применение апгрейда' not in s:
 i=s.index('\n## Файлы:'); s=s[:i]+'\n'+block+s[i:]
p.write_text(s,encoding='utf8')
