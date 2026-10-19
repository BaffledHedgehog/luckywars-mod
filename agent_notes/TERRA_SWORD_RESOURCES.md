# Terra Sword item resource import

Источник — read-only datapack `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC`.
Для проверки оформления использован read-only пакет `all_in_one_1.21.8`, путь
которого указан в [`RESOURCE_PACK.md`](RESOURCE_PACK.md). В нём уже есть все
используемые ниже ключи для `ru_ru` и `en_us`; новые переводы и модели не нужны.

| Новый item ID / таблица | Legacy entry | Proxy | Модель / стиль |
| --- | --- | --- | --- |
| `skywars2:terra_sword` / `ported/terra_sword.json` | `lbc:transformables_sword`, `/pools/0/entries/13` | `minecraft:carrot_on_a_stick` | `lbc:terrasword`, `lbc:tier_b` |
| `skywars2:true_terra_sword` / `ported/true_terra_sword.json` | `lbc:tuntijaa_terra_awakened`, `/pools/0/entries/0` | `minecraft:carrot_on_a_stick` | `lbc:terrasword_true`, `lbc:tier_s` |

Обычный меч сохраняет `+7` attack damage и `-2.4` attack speed, имя
`terra_sword` (green, `italic=false`) и шесть исходных строк lore в исходном
порядке: `type` + `sword`, `on_damage`, `terra_sword.lore.1`, `RCability`,
`terra_sword.lore.2`, `razorpine.lore.1`. В custom data сохранены все исходные
маркеры: `craftusable`, `transformable`, `sword`, `terrasword`, `raycastspell`,
`swordwithability`. Исходная запись не задаёт `damage` или `max_damage`, поэтому
для proxy сохраняются начальная прочность 0 и стандартный максимум carrot on a
stick (25), без дополнительного `set_components` для прочности.

Улучшенный меч сохраняет `max_damage: 9000`, `+9` attack damage и `-2.5` attack
speed, имя `true_terra` (green, `italic=false`) и семь исходных строк lore:
`type` + `sword`, `7 / 0.5 ` + `magic_damage`, score + `mana_on_activate`, затем
`true_terra.lore.1`, `.2`, `.3` и `craft_with_star_wand`. Custom data:
`craftusable`, `transformable`, `sword`, `magicwand`, `true_terra`. Исходная
запись не задаёт `damage`; она явно переопределяет `max_damage` значением 9000.

Строка стоимости маны у улучшенного меча использует прежний глобальный score
holder `true_terra_mana_use` из `lbc.math`: конфигурация задаёт ему значение
`1000`, а способность проверяет и списывает именно это значение. Таблица
сохраняет score-компонент и `mana_on_activate`; значение lore разрешает штатный
текстовый компонент score по scoreboard. Это тот же нативный паттерн, что уже
используется у импортированных `tiny_terra_wand` и `academy_terra_wand`; отдельный
helper и новые переводы не нужны.

Символ заряда `\uE103` существует в RP: `assets/minecraft/font/icons.json`
сопоставляет его `minecraft:font/icons/sword_charges.png`.

`tools/import_lbc_weapons.py --terra-swords` импортирует только эти две записи,
применяя общий `migrate_loot_schema` и замену item ID по существующему шаблону.
Команда отказывается перезаписывать существующую целевую таблицу. Обычный запуск
импортера оставлен без изменений.
