from pathlib import Path
p=Path('agent_notes/lbc_other_attributes.md');s=p.read_text(encoding='utf-8')
s=s.replace('результат `base get S` сохраняется как целый score с округлением вниз (`floor`), поэтому шаг `delta/S` точен для самой дельты, но дробное исходное base квантируется вниз при каждом изменении.', 'результат `base get S` — `trunc(base × S)`, то есть дробная часть отбрасывается к нулю. Для неотрицательной базы это совпадает с floor, для отрицательной — нет. Шаг `delta/S` относится к изменению уже квантованного числа, а не гарантирует такую абсолютную дельту от прежней дробной базы.')
s=s.replace('`floor(base×3000)`, `floor(base×2500)` и `floor(base×4000)`', '`trunc(base×3000)`, `trunc(base×2500)` и `trunc(base×4000)`')
s=s.replace('дробной исходной базы. Рандомные', 'дробной исходной базы; `base get` отбрасывает дробную часть к нулю. Рандомные')
s=s.replace('обновления базы: `floor`', 'обновления базы: `trunc`')
marker='## Таблетки: trigger, входной предмет и результат\n'
assert marker in s
s=s.replace(marker,marker+'\nВ таблице `effect ... D amp=A`: D задано в секундах, `infinite` — без конечного таймера; amplifier A означает уровень A+1. Для мгновенного эффекта D не означает непрерывное действие столько секунд.\n',1)
p.write_text(s,encoding='utf-8')
p=Path('agent_notes/review_attributes.md');s=p.read_text(encoding='utf-8').replace('integer `floor` при `base get S`', 'integer `trunc` при `base get S` (поправка координатора: дробная часть отбрасывается к нулю, а не вниз)');p.write_text(s,encoding='utf-8')
p=Path('agent_notes/SOURCE_FINDINGS.md');s=p.read_text(encoding='utf-8')
s+='\n## Attribute get: отдельное правило округления\n\n`attribute ... get/base get S` возвращает `trunc(value × S)` к нулю. Это отличается от `data get`, где числовой NBT использует floor. Например, −1,7 при S=1 даёт −1 у attribute get и −2 у data get. Проверено по `dmul; d2i` в [AttributeCommand](inventory/AttributeCommand.javap.txt) локального 26.3 jar. В описании LBC attributes исправлена прежняя общая формулировка floor; это проверка семантики команды, а не запуск датапака в 26.3.\n'
p.write_text(s,encoding='utf-8')
print('Attribute result truncation documented')
