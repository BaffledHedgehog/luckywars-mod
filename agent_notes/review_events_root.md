# Приёмка событий LBC координатором

Приняты137 источников lbc_other_events. Координатор прочитал95 нешаблонных
функций, включая пустые, и проверил целиком41 slot-function и101-command
food-dispatcher по строгим параметризованным моделям. [Все переходы](inventory/pumpkin_curse_slots.md),
[машинный аудит](inventory/events_variants_audit.json). Группы точно покрывают
job; исходные datapacks не редактировались.

Дополнены простые человеческие описания всех семейств: Bucks trader;
Halloween/screamer/фонарь/еда; Pumpkin Curse; Soul Corruption и6 Metasola;
Vex Wand; две булавы; November/Tournament; Chorus Pumpkin;6 Troll masks/Trollge.
Числа проверялись по исполняемым командам, не по комментариям или названию.

Исправлены ошибочное безусловное начисление memcoins,8 вместо16 scare-веток,
выдуманное удаление screamer в jack-check и назначение damager в spawn,
«100 отдельных advancements» вместо100 criteria одного achievement.
Отдельно проверено JSON rewards/requirements: default kit после всех100 критериев;
consume-trigger отзывается players_tick. Явно описаны friendly fire,
Owner-compound вместо UUID у wind charges, неоднозначное self-facing после as,
cooldown булав в секундах, совпадающие team/ID ветки Metasola и unconditional
equippable=feet в slot9. Строгий аудит41 полных command body: расхождений0.

Функции с motion и другими нестандартными командами фиксируют контракт внешней
регистрации из [COMMAND_BRIDGES.md](COMMAND_BRIDGES.md). Loot, templates,
полные ghosting-эффекты и общий runtime находятся в отдельных разделах.
Приёмка собственных137 файлов не означает приёмку всех зависимостей или
запуск текущего исходника на26.3.
