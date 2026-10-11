# Аудит источников дизайна lobby/gui/gameplay

Снимок выполнен по read-only старому серверу `E:/servers_mew/cringe1_21_4_skywars` и указанному в `RESOURCE_PACK.md` пользовательскому паку. Машиночитаемая база точных исходников находится в [design_source_manifest.json](inventory/design_source_manifest.json). В ней сохранены `exactRaw` и разобранные JSON всех 275 файлов slot loot tables, 130 вызовов item modifiers с их источниками, кнопки `swrg:gui/back|next|reset`, записи переводов storage `en/ru`, строки live render functions, условия динамических слотов и файлы пак-моделей для найденных `item_model` IDs. Это baseline исходников, не утверждение о финальных пикселях клиента.

## Что старая документация действительно зафиксировала

- [skywars_gui.md:3](skywars_gui.md#L3) прямо ограничивает свой scope datapack `swarogskywars`, а не текущий Java GUI. На [строке 9](skywars_gui.md#L9) указана модель datapack GUI: 27 `EnderItems` slots и cursor handler вместо отдельного открываемого контейнера. [Строка 21](skywars_gui.md#L21) отмечает общий язык `#lang` и `storage swrg:lang page1000.s*`; [строка 25](skywars_gui.md#L25) отсылает за названиями map-кнопок к отдельному каталогу.
- [review_skywars_gui.md:3](review_skywars_gui.md#L3) говорит, что инвентаризированы 397 datapack ресурсов и перечисляет их SHA/размеры. Это подтверждает прочтение source files, но не реестр экранов, точных slot item components или resolved client appearance.
- [skywars_gui_catalog.md:7](skywars_gui_catalog.md#L7) приводит названия map-select карточек, а [lbc_kit_interface_catalog.md](lbc_kit_interface_catalog.md) индексирует selector/page functions и условия. Они помогают восстановить некоторые точные labels и поведение конкретных карточек, но не объединяют фактические menu layouts с item JSON.
- [lbc_kit_interface.md:13](lbc_kit_interface.md#L13) уже содержит важную проверенную оговорку: LBC `lang_ru`/`lang_en` состоят из закомментированных `#data modify` команд, поэтому этим маршрутом тексты не загружаются; для `lbckit.N`, `lbcskill.N`, `lbclevelup.N` точный рендер зависит от внешнего provider/resource pack. Документ также отделяет advancement unlock gates от текста карточек.
- [RESOURCE_PACK.md:88-89](RESOURCE_PACK.md#L88) прямо говорит, что серверные проверки не подтверждают совпадение на графическом клиенте и полная визуальная сверка не сделана.

## Что в старых заметках не было каталогизировано

Предыдущий GUI audit намеренно исключал Paper GUI renderer/container handler и LBC namespace ([review_skywars_gui.md](review_skywars_gui.md), секция «Границы»). Поэтому в одном машиночитаемом месте не было: реальных открываемых container sizes и title components; runtime slot → loot table/item/components mapping; literal component JSON и exact language storage; последовательности live loot functions / modifiers; programmatic page content и Java selection overlays; различий пустых, отсутствующих и advancement-gated items; client pack model/font dependencies. Фразы о slots в prose нельзя использовать как карту 54-slot реального меню.

Также не был сделан агрегатный перечень tellraw/title/bossbar exact raw commands для перенесённого игрового flow. GUI-документ объясняет логику кнопок и местами цитирует заметные сообщения, но не служит двуязычным text baseline. Отдельные advancement описаны в [lbc_advancements.md](lbc_advancements.md) по критериям, наградам и revoke/grant; это покрытие не подменяет slot-specific связь advancement → locked/unlocked item → click response.

## Фактическая цепочка внешнего вида

Для live Paper lobby GUI исходный renderer — `plugins/luckywars/src/main/java/me/luckywars/gui/GuiManager.java`: создаётся inventory размером 54, title `Меню` для root и `Меню »` для submenu. Слоты 0–44 — область содержимого; back=45, reset=52, второй reset challenge=51, next=53. Root page задаёт карту (`slot 2` динамически), start, settings, shop, cosmetics, kits/keys/skills, BedWars и LBC routes. Страница может быть программной (perk/map/shop) или loot-table-backed.

`GuiItems.java` генерирует slot item из `swrg:gui/page/<page>/<slot>`, затем использует LBC namespace как fallback. Loot table выполняется в command loot context с THIS_ENTITY. Это значит, что итоговый компонент складывается из item entry и упорядоченных pool/entry `functions`; `minecraft:set_name`/`set_lore` могут читать и интерпретировать JSON text component из `storage swrg:lang`. После выдачи Java добавляет perk PDC identity, а выбранному варианту может включить glint и добавить строку `gui.select`. Варианты зависят от player, scores, tags, advancements, ranked/event mode и статистики. Порядок фактической цепочки и raw inputs сохранены в JSON-manifest.

`BedwarsShopGui.java` открывает отдельный inventory размером 54 с title translate key `bedwars.shop.title`; верхние слоты 0–8 — category tabs, 9–17 — separator, остаток динамически строится по категории/команде/игроку. Вкладка lucky может отсутствовать; slot 6 использует `item_model=lbc:lucky_block`.

Отдельно существует datapack-представление `EnderItems[0..26]` с `slot_id` и cursor selection; его нельзя выдавать за размер/title экрана Paper renderer. Для root кнопок и интерфейса дополнительные live outputs программно строятся Java-кодом.

Приоритет сверки item component для тестов восстановления:

1. Фактически вызванный live page renderer и вычисленный им page/slot/variant.
2. Raw JSON loot table (или Java item builder для программных слотов).
3. Упорядоченные loot functions и модификаторы, включая storage-backed component JSON и runtime условий.
4. Java post-processing и динамическое выделение выбранного/заблокированного состояния.
5. Resource-pack model/font/translation resolution в клиенте.

Прямой `item_model` component не отменяет последующую функцию, которая меняет тот же component; проверять нужно итог после полного порядка. Сырые JSON компонентов, включая `custom_data`, `item_model`, `profile`, name/lore, оставлены без нормализации, пригодной для проверки точных стилей/значений.

## Существенные динамические и визуальные fixtures

- Старый datapack root slot 8 создаёт `minecraft:player_head` и вызывает `minecraft:fill_player_head`: результат зависит от entity. Page 1100 задаёт два отдельных `minecraft:profile` payload для кнопок `Русский` и `English`; это нельзя заменять generic head.
- Root/menu, settings и cosmetics прямо используют модели `lbc:cases`, `lbc:build_manager_icon`, `lbc:arrow_right`, `lbc:arrow_left`, `lbc:tweak`, `lbc:lucky_block`, `lbc:kapusta`, `lbc:shop`, `lbc:cosmetics`, `lbc:runes_common/rare/epic/legendary`; кейс-страницы меняют `lbc:unknown_key` на `lbc:halloween_key`, а SWRG kit fixture включает `lbc:golden_pumpkin`. Manifest содержит точные `assets/lbc/items/*.json` файлы для найденных IDs. У записи `lbc:build_manager_icon` сама модель/ресурс-пак остаётся зависимостью экрана, даже если build dialog затем передаёт управление Paper.
- Пустые предметы имеют несколько разных причин: JSON loot table с `minecraft:empty`, несуществующая/невыданная slot table, условие по тегу/score/advancement, пустой результат loot generation. В manifest оставлены exact slot JSON и исходные строки page builder conditions.
- Визуальные результаты advancements (дерево/иконка/background) не равны обычной menu slot. Прошлая advancement справка фиксирует содержимое JSON, а manifest фиксирует конкретные GUI unlock interactions в пределах перенесённых страниц. Ванильный экран достижений отдельным клиентом не осматривался.

## Ограничение визуального вывода

Исходники подтверждают component baseline, возможные модели и runtime branches, но не подтверждают отрисованный результат Minecraft client. Не проверены фактический пакет, выбранный клиентский language, рендер масштаба/шрифта, текстуры модели, сортировка и взаимодействие пользователя на живом графическом клиенте. Поэтому вывод о полном визуальном совпадении не сделан.

## Отдельная сверка текста GameController / MatchEvents

Сверка продолжения переноса выявила одно точное расхождение: старый `swrg:lang.title.player_win` в `core/lang/en.mcfunction:57` целиком собирал `Player ` + выделенное имя игрока + ` wins the game` (без завершающего знака). Новый [GameController.java](../src/main/java/me/skywars2/game/GameController.java) собирает это из `swrg.title.player_win` и `swrg.title.player_win2`; клиентский `en_us.json:5747` задаёт вторую часть как `wins the game!`, добавляя `!`. Русское значение `player_win2` остаётся `побеждает в игре` без добавленной пунктуации. Это точечный source/style drift для рассмотрения при сверке переводов.

Проверенные соседние компоненты совпадают по исходным источникам: `MatchEvents.stageAnnouncement` использует существующие `swrg.title.stage1.event1..8` / `stage2.event1..7` ключи с зелёным/красным цветом; старые stage selection функции объявляют соответственно 8 и 7 вариантов. `rewardText` сохраняет gold и `italic=false` для `swrg.reward.kill`, `.start_game`, `.stage1`, `.stage2`, `.win`. Новый bossbar собирает gold prefix, light-purple счётчик и gold `]`, как старый `title.gametimer` JSON компонент. Эти проверки — по текстовым/компонентным исходникам, без запуска игрового сервера.


## Проверка импорта GUI

`python tools/check_design_sources.py` сравнивает `skywars2/gui_design.json` и импортированные loot tables с независимым снимком старых исходников. Проверяются контейнер на 27 слотов, заголовок, номера слотов, ссылки на таблицы и строки функций, прямые предметы, модели и компоненты. Сравнение loot JSON учитывает преобразование формата в 26.3; исходное поведение при этом не должно меняться. Проверка не анализирует Java-код и сама по себе не доказывает внешний вид реального меню.

Статически сверены 20 страниц, 870 ссылок и соответствующих строк исходников, 241 loot table и 10 моделей. В сравнении присутствуют кейсы `swrg:5000` со слотом 4, магазин `swrg:6000` со слотами 12–14, страница `lbc:12000` со слотом 0 и reset-кнопками 24/25. Несуществующая страница `lbc:11100` не объявляется перенесённой. Статистика, выбранные карточки, содержимое профиля и условные варианты требуют отдельного экспорта фактических предметов через `/sw check design-export`. Оформление на графическом клиенте отдельно не проверялось.

## Lobby entity text source check

Read-only inspection of `world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/lobby.mcfunction`, both `core/lang/{en,ru}.mcfunction` helpers, and `world/datapacksdisabled/maps/data/swrg/structure/lobby.nbt` confirms the source form. The `swrg.lobby` entity receives `CustomName` from `swrg:lang entity.lobby`; EN is `{"text":"SkyWars Menu","color":"green","italic":false}`, RU is `{"text":"Меню СкайВарса","color":"green","italic":false}`. These are fixed JSON text components selected by language, not translatable components. The `swrg.quickstart` structure contains one interaction and three text displays; none has `CustomName`. The fixed text displays are centered with fixed billboard and opacity -1, at relative `pos` [9.5, 2.5, 9.5] (`text1`, title), [9.5, 2.25, 9.5] (`text2`, map), and [9.5, 2.0, 9.5] (`text3`, hint). Their old fixed NBT `text` values are `{"text":"Быстрая игра","color":"yellow"}`, `"Случайная карта"`, and `"ПКМ для старта"` (the latter two are JSON string components). The datapack sends a separate translatable/source-storage `title.quickstart` message when starting. The new authored requirement localizes the three structure labels with existing translation keys; that is an intentional behavior change from the fixed Russian NBT strings above, while preserving source positions, ordering, color, and display flags.


The manifest's `languageStorageSources[].exactRaw` values are copied from the corresponding old-server helpers as UTF-8, including the RU text; this matters because prior generated captures had replacement characters in some RU lines. Exact source-language component checks use those helper records and the existing read-only resource-pack translation tables.

## Runtime menu snapshot validation

`python tools/check_design_sources.py --snapshot menu-design-snapshot.json` adds checks for the GameChecks `pages` export. It reads the serialized stacks and independently resolves imported source paths against the manifest's UTF-8 old-language helpers and the read-only resource-pack language files. The export must retain the 27-slot Ender Chest title; populated source fixtures are compared for material, model, count, literal component text, translation key, color and style. Dynamic player/head and mutually exclusive score rows are checked using their source conditions.
