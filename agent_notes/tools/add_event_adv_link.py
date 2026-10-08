from pathlib import Path
p=Path('agent_notes/lbc_other_events.md'); s=p.read_text(encoding='utf8').replace('Значит пищу определяет trigger JSON, а количество/прогресс — эта команда и advancement family.', 'Значит пищу определяет [trigger JSON](../world/datapacksdisabled/LBC/data/lbc/advancement/eat_halloween_food.json), а количество/прогресс — эта команда и advancement family.')
p.write_text(s,encoding='utf8')
