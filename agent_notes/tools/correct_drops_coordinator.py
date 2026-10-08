"""One-shot corrections after direct boss/antigravity source review."""
from pathlib import Path
root=Path(__file__).resolve().parents[2]
p=root/'agent_notes/lbc_drops.md'
s=p.read_text(encoding='utf-8')
s=s.replace('находит все падающие блоки нетеритового блока в мире', 'находит все загруженные падающие блоки нетеритового блока во всех измерениях (selector без distance/world-limit)')
s=s.replace('на 80 вверх и 150 вперёд от направления предмета', 'на80 вверх и150 назад относительно горизонтального направления к0,0 (facing0,0,0; rotated~0; local ^ ^80 ^-150)')
old='Отдельно `tick15s` каждые 15 секунд вызывает `disappear`, только когда нигде нет turtle с тегом crocodildo; она удаляет bossbar и звук, затем создаёт `nuke_pig` с passenger display-тегами `crocodildo_hitbox,nuke_pig_hitbox,slowraycast,rare`; функция вызывается без `at` и `as`, поэтому позиция берётся от scheduler context, а не от удалённой turtle. Поскольку в `tick15s` нет запрета повторить вызов, если turtle не появляется, он создаёт очередной pig каждые 15 секунд.'
new='Отдельно `tick15s` каждые15 секунд вызывает `disappear`, когда среди загруженных сущностей нет turtle с тегом crocodildo. Она **только** удаляет bossbar и останавливает звук; новых pig не создаёт. Pig создаёт именно `crocodildo/5s`, вызываемая на turtle, с passenger display-тегами `crocodildo_hitbox,nuke_pig_hitbox,slowraycast,rare`; точка появления — контекст живого босса.'
assert old in s
s=s.replace(old,new)
s=s.replace('время `tick15s` отключается при time stop, но после выхода `load_aftertimestop` этот scheduler явно не перезапускается.', 'time_stop очищает расписание tick15s, а после выхода load_aftertimestop явно вызывает tick15s и восстанавливает цепочку.')
s=s.replace('длительность зависит от внешнего удаления marker-а или выгрузки/перезапуска.', 'выгрузка приостанавливает обработку, а удаление marker-а завершает поле. Gravity modifier может пережить удаление/выгрузку: cleanup происходит только при отдельном вызове refresh_attribute, не автоматически после исчезновения marker-а.')
p.write_text(s,encoding='utf-8')
p=root/'agent_notes/review_drops.md'
p.write_text('''# Проверка LBC drops

Исследователь сообщил независимое чтение всех135 источников job lbc_drop_effects:
134 функции с командами и пустая startarrow. [Манифест](inventory/lbc_drops_read_manifest.json),
[человеческое описание](lbc_drops.md). Этот отчёт восстановлен координатором:
прежний русский текст был испорчен передачей через PowerShell pipe.

Координатор прочитал весь основной документ и проверяет сложные переходы по
исходникам. Уже исправлены ошибочное продолжение после kill, направление
crocodildo spawn, выдуманное создание pig в disappear, пропущенный рестарт
tick15s и сохранение gravity modifier после исчезновения поля. Pig появляется
из 5s, disappear только убирает звук/bossbar. Подробности непосредственно в MD.

Первичные источники: [spawn](../world/datapacksdisabled/LBC/data/lbc/function/drops/crocodildo/spawn.mcfunction),
[5s](../world/datapacksdisabled/LBC/data/lbc/function/drops/crocodildo/5s.mcfunction),
[disappear](../world/datapacksdisabled/LBC/data/lbc/function/drops/crocodildo/disappear.mcfunction),
[tick15s](../world/datapacksdisabled/LBC/data/lbc/function/tick15s.mcfunction),
[возобновление](../world/datapacksdisabled/LBC/data/lbc/function/load_aftertimestop.mcfunction),
[gravity cleanup](../world/datapacksdisabled/LBC/data/lbc/function/drops/antigravity/refresh_attribute.mcfunction).

Полная приёмка этого раздела пока не завершена. Содержимое loot/templates и
конечные действия вызываемых предметных подсистем остаются отдельными заданиями;
наличие manifest/coverage не подтверждает их содержательную проверку.
''',encoding='utf-8')
print('drops prose and damaged report corrected')
