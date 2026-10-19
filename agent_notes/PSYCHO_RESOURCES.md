# Psychowand и Psychoblind: оформление

Сверка 18 октября 2026. Исходный datapack и реальный ресурс-пак прочитаны без
изменений. Правила: [RESOURCE_PACK.md](RESOURCE_PACK.md).

`tools/import_lbc_psycho.py` использует существующий импортер Fireblaze и общий
переход loot codec. Генерируются только два базовых предмета:

| Native ID | Loot table | Исходная таблица | Item model | Tooltip |
| --- | --- | --- | --- | --- |
| `skywars2:psychowand` | `lbc:ported/psychowand` | `transformables_staff.json`, маркер `psychowand:1` | `lbc:psychowand` | `lbc:tier_c` |
| `skywars2:psychoblind_wand` | `lbc:ported/psychoblind_wand` | `tuntijaa_blind_psycho.json`, маркер `psychoblind_wand:1` | `lbc:psychoblind` | `lbc:tier_s` |

Результирующие entries сравнены структурно с исходными после общего перехода
codec: единственное дополнительное отличие — внешний native item ID. Сохранены
имя, lore, порядок и вложенность компонентов, score-компоненты, стили и custom data.
Psychowand содержит десять строк lore, Psychoblind — четыре.

Все 15 используемых translate keys уже имеются в обоих языках реального пака
`assets/minecraft/lang/en_us.json` и `ru_ru.json`: `type`, `staff`,
`wand_effect_count`, `mana_on_activate`, `cast_radius`, `craft_with_blind`,
`psycho_wand`, `psycho_wand.lore.1`–`psycho_wand.lore.5`,
`psychoblind_wand`, `psychoblind_wand.lore.1`, `psychoblind_wand.lore.2`.
Отсутствующих переводов нет; дополнения языков или template не требуются.

Оба item definition существуют в реальном паке и ссылаются соответственно на
`lbc:item/psychowand` и `lbc:item/psychoblind`.

Стоимость в lore остаётся исходным score: `psycho_wand_mana_use lbc.math` и
`psychoblind_wand_mana_use lbc.math`. В `function/config.mcfunction:36,91`
значения равны 600 и 500. И базовые, и Academy касты списывают эти же значения.
Native выдача должна разрешать эти score-компоненты существующим общим способом.
Исходное описание Psychowand явно указывает радиус 30 и три случайных эффекта;
переводы также сохраняют предупреждение о призраке при цели с 1 HP.
Psychoblind сохраняет описание проходящих сквозь стены фантомных снарядов.

Отдельные Academy loot tables не добавляются: повышение обслуживает общий helper.
Исходные upgrade-функции применяют `tiers/b` для Psychowand и `tiers/ss` для
Psychoblind; эти варианты сохраняют исходные имена/модели и добавляют общий
upgrade lore по существующему контракту Academy.

Проверка: `python tools/import_lbc_psycho.py`, сравнение обоих entries с исходником
после `migrate_loot_schema`, наличие всех translate keys в RU/EN и обоих item
definitions реального пака. Игровая проверка в этот ресурсный этап не входит.
