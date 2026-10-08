from pathlib import Path
p=Path('agent_notes/lbc_other_misc.md'); s=p.read_text(encoding='utf8')
needle='\n## Shock effect:'
block='''
## Freeze block: сохранённая позиция и эффекты

`freeze/block/create` записывает Pos[0..2] цели в индивидуальное storage `minecraft:<UUID>`; команды, принудительно отключающей AI, в исходнике закомментированы. Периодическая ветка `tick` передаёт UUID в macro, возвращает сущность на сохранённые координаты, ставит лёд в столбце высотой 2 блока и накладывает Mining Fatigue II? Проверенное значение эффекта — amplifier 127 на 1 секунду, а также Weakness amplifier 127 на 1 секунду; только игроку дополнительно Resistance amplifier 1 на 1 секунду. В конце `cold` сбрасывается в 0, цель получает тег `have_lucky_effects`, появляется снежинка. `destroy` заменяет этот столбец воздухом с drops и проигрывает ice/snow particles; это отдельная очистка, не часть tick. [Сохранение позиции](../world/datapacksdisabled/LBC/data/lbc/function/other/freeze/block/create.mcfunction), [tick](../world/datapacksdisabled/LBC/data/lbc/function/other/freeze/block/tick_3.mcfunction), [разрушение](../world/datapacksdisabled/LBC/data/lbc/function/other/freeze/block/destroy.mcfunction).
'''
block=block.replace('Mining Fatigue II? Проверенное значение эффекта — amplifier 127 на 1 секунду','Mining Fatigue и Weakness по 1 секунде с amplifier 127')
if '## Freeze block:' not in s: s=s.replace(needle,'\n'+block+needle)
p.write_text(s,encoding='utf8')
