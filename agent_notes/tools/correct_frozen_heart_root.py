from pathlib import Path
N=Path('agent_notes');p=N/'lbc_kit_game.md';s=p.read_text(encoding='utf-8')
lines=s.splitlines();matches=[i for i,l in enumerate(lines) if l.startswith('`Frozen Heart` is an advancement')];assert len(matches)==1
i=matches[0];refs=lines[i].split('Источники:',1)[1]
lines[i]='Навык 1 `Frozen Heart` входит через advancement `slowness`: нужен эффект Slowness, `lbc.skill=1` и нахождение вне Nexus/Imprinted; функция дополнительно проверяет `#lbcskill=1`. Она читает оставшуюся длительность Slowness в тиках и передаёт это же число в секунды `/effect give resistance ... 1`, затем снимает Slowness и отзывает advancement. Поэтому обычная положительная длительность Resistance II получается в 20 раз длиннее исходной: 20 тиков замедления дают 20 секунд сопротивления. Фиксированного cooldown нет, повторный вход требует нового события advancement; отрицательное значение бесконечного NBT duration не преобразуется в `infinite` и не является допустимым аргументом секунд. Источники:'+refs
p.write_text('\n'.join(lines)+'\n',encoding='utf-8')
p=N/'SOURCE_FINDINGS.md';s=p.read_text(encoding='utf-8')
s+='''
## Frozen Heart: тики передаются как секунды

`frozen_heart_2` читает `active_effects[{id:"minecraft:slowness"}].duration`
с коэффициентом 1, а `frozen_heart_3` подставляет число прямо в `/effect give`.
Нормальная положительная длительность Resistance II поэтому увеличивается
в 20 раз: NBT 20 тиков превращается в 20 секунд; затем Slowness снимается.
Это фактическая ошибка единиц в датапаке, а не исправленное поведение мода.
Источники: [чтение и очистка](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/frozen_heart_2.mcfunction),
[макро-эффект](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/frozen_heart_3.mcfunction),
[условия advancement](../world/datapacksdisabled/LBC/data/lbc/advancement/slowness.json).
'''
p.write_text(s,encoding='utf-8')
print('Frozen Heart source units documented: ticks passed as seconds, skill=1 advancement gate')
