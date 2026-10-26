# Метка на шефе — достижение, 26 октября 2026

Объедини ключи из `updates/chef_label.ru_ru.json` и `updates/chef_label.en_us.json`
с соответствующими файлами языка своего пака. Это только новые ключи названия и
описания достижения; файлы языка целиком заменять не нужно. Иконка использует
существующий предмет `wtf_item` с моделью `lbc:banan`; новые текстуры не нужны.

# Банхаммер — 26 октября 2026

Объедини `updates/banhammer.ru_ru.json` и `updates/banhammer.en_us.json` с соответствующими файлами языка. Это новый ключ причины отключения; прежние модели и тексты предмета уже есть в паке.

# Гримуары: семь цветов Gaycaster и снежный жезл — 26 октября 2026

[Модели, текстуры и инструкция по объединению переводов](EXTRA_GRIMOIRES_README.md).
Новые ключи находятся в `updates/extra_grimoires.*.json`; их нужно объединить
с соответствующими файлами языка.
Все девять призывных гримуаров удалены. Новые цветовые книги используют
`lbc:spelllib/gaycaster_{red,orange,yellow,green,cyan,blue,purple}`.
Для снежного жезла подготовлены отдельные книги `lbc:spelllib/snow`
и `lbc:spelllib/snow_academy`: каст у игрока и каст в точку прицеливания.

# Скин Шефа — 25 октября 2026

Скопируй `assets/skywars2/textures/entity/chef.png` из template в свой ресурспак
по тому же пути и перезагрузи ресурсы (F3+T). Это исходный PNG твоего скина,
без изменений пикселей. Он также входит в серверный Polymer-пак: если этот
пак загружен клиентом, отдельное копирование не требуется. Одного наличия PNG
в JAR недостаточно для клиента, который использует только ручной ресурспак.
Исходный ресурспак агент не изменял.

# Испытание Gaycaster — 24 октября 2026

В файлах языка подготовлена правка существующего ключа
`gaycaster_meditate_no_wand`: испытание проверяет ту руку, которой оно начато,
включая вторую руку. Объедини этот ключ с файлами языка своего пака,
заменив прежнее значение. Модели, остальные переводы и оформление сохранены.
Исходный пак агент не изменял.

# Сингулярность Абсурда — 24 октября 2026

В `assets/minecraft/lang/ru_ru.json` и `en_us.json` подготовлены две правки
существующих ключей `upgrade_condition_omegaflag` и
`upgrade_succesful_omegaflag`: улучшенная версия теперь защищает владельца
и всех его союзников. Объедини эти ключи с файлами языка своего пака,
заменив прежние значения. Остальные переводы и оформление сохранены.
Исходный пак агент не изменял.

# Прицелы SLR/Magnum — 23 октября 2026

Зум перенесён в проекцию камеры через клиентский FOV. Включённый `lbc:zoom`
остаётся для перекрестия и круглой тени, а `lbc:zoom_out` включается на 6 тиков
(0,3 секунды) при выходе из прицела. Эти эффекты не должны дополнительно
увеличивать картинку: иначе приближение от FOV и старого шейдера перемножится.

В template подготовлены два JSON на основе текущего авторского пака:

```text
assets/lbc/post_effect/zoom.json
assets/lbc/post_effect/zoom_out.json
```

В обоих `ZoomConfig.ZoomFactor` установлен в `1.0`, `Duration` — в `0.3`.
Направления, проходы и история анимации сохранены. Если ты уже добавил туда
перекрестие/тень, перенеси только эти два значения в каждый JSON, сохранив свои
остальные изменения. Иначе можно скопировать файлы целиком, затем нажать F3+T.
Исходный ресурспак агент не менял.

Нативный FOV-зум зависит от настройки клиента «Изменение поля зрения»:
при 100% используется четверть обычного FOV; при 0% клиент отключает этот
механизм. Положение камеры, скорость движения и серверные атрибуты не меняются.

# Шрифты атрибутов: исходные PNG — 16 октября 2026

Скопируй из template в свой ресурспак **одну папку**:

```text
assets/lbc/font/attribute_hud/
```

В ней 18 полных `row_*.json`, `glyphs.ttf` и `spacing.json`.
Замени соответствующие JSON целиком и добавь два отсутствующих файла,
затем нажми **F3+T** на клиенте. Менять JAR и перезапускать сервер не нужно.

Все 36 иконок ссылаются прямо на твои исходные
`minecraft:font/icons/*.png` и прежние item-текстуры меча/кирки из default.
PNG не создаются, не изменяются и не перемещаются. Размер на экране
одинаковый — 9 px через `height`; исходное разрешение файлов сохраняется.
`default.json` менять не требуется. Два столбика по 18 строк остаются.

Панель смещена целиком на **54 GUI px вниз**: bitmap-провайдер Minecraft
требует `ascent <= height`, поэтому прежние верхние строки нельзя сохранить
при размере 9 px и запрете на дополнительные поля/атласы PNG.
Сохраняются имена шрифтов `row_m6`…`row_0`…`row_p11`, которые использует
уже установленный мод; вертикальный сдвиг задан только их JSON.

TTF содержит цифры/знаки, точка — 1×1 px с шагом 2 px. `spacing.json`
задаёт шаг U+E000 для горизонтальных отступов. Маленькие невидимые глифы
в каждом row JSON дополняют ширину иконки до слота 10 px.

Причина квадратов подтверждена клиентским логом: прежние row JSON ссылались
на отсутствующие `lbc:textures/font/attribute_hud/native_*.png`, а также
на отсутствующий `lbc:font/attribute_hud/glyphs.ttf`. Копии PNG нашлись
в другом namespace; новая правка использует исходные текстуры напрямую.

Настоящий RP только прочитан. Генератор
`python tools/build_attribute_hud_font.py --import-icons` теперь готовит
только JSON/TTF и пропускает уже применённые файлы. После применения
удаляй перенесённые файлы template как обычно.
[Отчёт](../build/verification/attribute-hud-icons.json),
[каталог символов](../agent_notes/PLAYER_ATTRIBUTE_CATALOG.md).

## Защита TNT 100% — 20 октября 2026

В языках `assets/minecraft/lang/ru_ru.json` и `en_us.json` добавлен только
недостающий ключ `skywars2.gui.source.page1000.s12-5.part0` для нового пятого
уровня защиты. Объедини его со своим lang JSON; уровни 0..4 используют прежние
ключи и оформление. Настоящий ресурспак не изменён.

## Языковая строка admin — предыдущая правка

`assets/minecraft/lang/ru_ru.json` и `en_us.json` содержат
`skywars2.game.admin_only`. Объедини нужные ключи с языками своего пака,
не заменяя весь lang JSON. Это отдельная правка, не часть исправления шрифта.

## Обновление сообщений карт и режимов — 14 октября 2026

`updates/map_messages.ru_ru.json` и `updates/map_messages.en_us.json` содержат
точечную замену значения уже существующего ключа `skywars2.game.map_selected`.
Найди этот ключ в `assets/minecraft/lang/` и замени только его значение на
значение из соответствующего updates-файла. Не добавляй вторую запись и не
копируй updates-файл целиком как новый lang-файл. Сообщение больше не говорит,
что выбранная карта загрузится при старте матча: карты подготавливаются сразу.

Ключ `skywars2.game.mode_unavailable` уже есть в исходном ресурспаке и удалён
из template вместе с тремя ранее перенесёнными ключами интерфейса ниже.

## Условие испытания Exasperato — 16 октября 2026

`updates/academy_trial.ru_ru.json` и `updates/academy_trial.en_us.json` —
точечная **замена значения** существующего `upgrade_condition_exasperato`.
Обнови эту запись в соответствующих lang-файлах RP: считаются 10 разных
задетых целей одной серии, а не убийства. Не добавляй второй такой ключ.
Остальной текст и оформление сохраняются; после применения updates можно удалить.
Новые дубликаты переводов Academy в `assets/*/lang` не добавлялись.

# Добавления для ручного переноса

## Сообщения о смерти — 16 октября 2026

`assets/minecraft/lang/en_us.json` и `ru_ru.json` содержат по 131 новому death
сообщения: отсутствовавшие vanilla-ключи, дополнительные `.player`/`.item`
варианты, генерируемые из message ID без английского vanilla-перевода,
специальные `death.fell.*`/bad-respawn сообщения и три сообщения
`death.attack.skywars2.electric` (обычное, с игроком и с предметом). Объедини
записи с файлами пака; не заменяй их целиком. Добавлены принадлежащие моду
варианты `.player` для cactus, even_more_magic, genericKill и sulfurCubeHot;
недостающие `.item` тоже добавлены. 11 существующих death overrides сохранены
и не дублируются. `.player` содержит `%1$s`/`%2$s`, `.item` — все три аргумента.

Ключи меню наблюдателя `skywars2.spectator.players` и
`skywars2.spectator.inventory` уже есть в исходном паке; из template удалены.

Ключ `skywars2.lbc.pending` также уже находится в исходном паке и отсутствует
в template. Имя и lore элемента LBC не менялись.

Копируй содержимое `assets/` в `all_in_one_1.21.8`, сохраняя пути. Если файл
языка уже существует, объедини записи с ним. Сам исходный ресурс-пак агентами
не изменяется. `KEY_REUSE.json` — отчёт сверки, его копировать в пак не нужно.

Актуально на 9 октября 2026: все добавления уже применены пользователем,
включая последние 217 записей каждого языка. Недостающих переводов нет;
языковые JSON удалены из template и не восстанавливаются в моде.
Повторные названия карт под другим префиксом также удалены; используются
выбранные исходные ключи. Новые шаблоны появятся только для новых изменений.

Эти JSON содержат **добавления**. Если соответствующий языковой файл уже
существует в паке, объедини записи; не заменяй им весь существующий файл.
Имена, строки lore, пробелы, зачёркнутые цены и прочие исходные компоненты
не переписываются своими словами. Для нескольких RU-only исходных подписей
EN пока содержит тот же русский текст; это отмечено в
`agent_notes/GUI_DESIGN_RESTORE.md`.

После переноса можно удалить этот template. При следующей работе учитываются
актуальные файлы исходного пака; перенесённые записи повторно не создаются.

## Панель атрибутов столбиком — 15 октября 2026

Исходные шрифты уже перенесены пользователем в `assets/lbc/font/attribute_hud/`,
код использует `lbc:attribute_hud/*`. Их прежний template не восстанавливается;
новое дополнение иконок описано в начале этого файла.
Исходный ресурс-пак агентом не изменён.

Пользователь также исправил внутренние TTF-ссылки на
`lbc:attribute_hud/glyphs.ttf`. Вспомогательный скрипт удалён по его просьбе.
Языки и остальные ресурсы не меняются; новые копии шрифтов не создаются.

До правки точки выше `glyphs.ttf` содержал временные Unicode-значки и цифры
с одинаковым шагом 6 GUI px. У новой точки шаг 2 px, у остальных символов
по-прежнему 6. `row_*.json` смещают строки по Y на 9 px, `spacing.json` задаёт
положительный шаг U+E000. Возврат X использует уже существующие в твоём
default font символы U+F800…F80F / U+F820…F82F. Одна строка scoreboard
содержит весь столбик, поэтому клиент не отрезает атрибуты после первых 15.

Обычный режим `tools/build_attribute_hud_font.py` не воссоздаёт исходный
template при наличии шрифтов в паке. Новый режим `--import-icons` создаёт
только отличия для авторских значков; их слот теперь 10 px, как описано выше.

## Tiny Teleport random crafting - 21 October 2026

Merge only the two new skywars2.crafting.tiny_reforge keys from
assets/minecraft/lang/ru_ru.json and en_us.json into your actual resource pack.
The crafting result deliberately displays a generic preview. The real random
reward is rolled only when taking it, together with ingredient consumption.
The current mod includes an English fallback until these keys are merged.
The original resource pack has not been modified.

## Reroll Machine random crafting - 21 October 2026

Merge the single new skywars2.crafting.reroll_machine.lore key per locale into
your current language files. Existing lbckit.24 item name and both original lore
lines are reused unchanged; no duplicate source translations are provided.
Only the neutral crafting preview uses this new line. An English fallback is
available until it is merged. The original resource pack remains read-only.


## One-use Academy token

`academy_token` now uses the separate label key `welcome_to_single_use_academy`.
Add that key to your resource-pack language files with your own text. The server
keeps the original aqua, nonitalic display style and supplies an English fallback.
The original `welcome_to_academy` remains the label of the reusable base Academy.
After one successful promotion the token Academy removes its own text/interaction
and changes its eight blue background cells to white; trial assignment and
readiness do not spend it. No original RP files or textures were modified.
