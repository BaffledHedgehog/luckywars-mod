# Проверка LBC drops

Исследователь сообщил независимое чтение всех135 источников job lbc_drop_effects:
134 функции с командами и пустая startarrow. [Манифест](inventory/lbc_drops_read_manifest.json),
[человеческое описание](lbc_drops.md). Этот отчёт восстановлен координатором:
прежний русский текст был испорчен передачей через PowerShell pipe.

Координатор прочитал весь основной документ и проверяет сложные переходы по
исходникам. Уже исправлены ошибочное продолжение после kill, направление
crocodildo spawn, выдуманное создание pig в disappear, пропущенный рестарт
tick15s и сохранение gravity modifier после исчезновения поля. Pig появляется
из 5s, disappear только убирает звук/bossbar. Подробности непосредственно в MD.

Первичные источники: [spawn](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/crocodildo/spawn.mcfunction),
[5s](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/crocodildo/5s.mcfunction),
[disappear](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/crocodildo/disappear.mcfunction),
[tick15s](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick15s.mcfunction),
[возобновление](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/load_aftertimestop.mcfunction),
[gravity cleanup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/antigravity/refresh_attribute.mcfunction).

Координатор завершил приёмку135 source-файлов после чтения всего человеческого
описания и дополнительной прямой сверки boss tick/phase/owner, диспетчера,
gravity, cage, portall и TMT. Исправлены21 enchant-attempt вместо20, абсолютные
Y=255/319 у клеток, обратный portal к исходному item (at не меняет @s),
отсутствующий индекс32 и сохранение старого attrname. [Полный аудит TMT-массивов](inventory/drops_tmt_audit.json)
прочитал все14 556 append-values и проверил распределение29/21 попыток;
1049tags,11682names,16hex,1735sound,32attributes,42enchantments, ошибок0.

Содержимое loot/templates и конечные действия вызываемых предметных подсистем
остаются отдельными заданиями. Приёмка собственных135 файлов не означает
приёмку их зависимостей или подтверждённый запуск на26.3.
