# Приёмка attributes координатором

Приняты 331 источник: полный [peer-проход](review_attributes.md), человеческий
обзор, таблица всех base handlers, все отдельные pill-результаты и legacy/reset
контракты дополнены риск-ориентированной сверкой координатора.

[attribute_tables_root_audit.json](inventory/attribute_tables_root_audit.json)
фиксирует исходные команды и SHA всех 331 источника. Отдельно сверены все
76 effect-команд таблеток с ID, длительностью и amplifier и 54 вызова
attribute handlers с их строками. Все 17 специальных scoreboard/tag-веток
выведены и просмотрены: совпадают mana/manamax/manaregen и effect counters,
включая три обслуживающих флага shock. В таблице пояснены секунды и amplifier.

Руками прочитаны burning_time_up_godclaw/update, random_health,
reduce_hp_armor и raid_omen_103; подтверждены отрицательный знак Godclaw,
вызов armor_update после чтения toughness, float/double storage scale,
advancement revoke и флаги periodic effects. Count marker-ов у Godclaw
не заменён boolean. Drop_inv описан как копирование без очистки helper-ом;
очистка относится к honey_pl caller. Ручные генераторы описаны по своим
действиям, не по предполагаемому эквивалентному результату.

Исправлена дополнительная ошибка округления, найденная при проверке движка:
`attribute get/base get` использует Java `d2i` после умножения, то есть trunc
к нулю. Это не floor для отрицательного значения. В частности, general
формулировку нельзя переносить с `data get`. Доказательство сохранено в
[AttributeCommand.javap.txt](inventory/AttributeCommand.javap.txt) локального
26.3 jar; игровой datapack не запускался. Таблица описывает дельту после
квантования, а не гарантирует такую дельту от произвольного дробного исходника.

Функции pills взводят счётчики, их конечные действия связаны с уже принятым
effects. Legacy-сchedulers и отмена их расписаний отличены от отмены изменений
мира. Внешние SWRG операции имеют соответствующие документы game/maps_admin.
