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


## ??????????? ???????? GUI-import

`python tools/check_design_sources.py` ??????? ????? `skywars2/gui_design.json` ? ???????????? ????????? loot JSON ? ???? source baseline; Java-??? ? ?????? ??? ?? ?????? ? ?? ?????????. ??????????? 27-slot ????????? ? title translation, source function/slot/table/direct-item/modifier refs, ?????? ???????? ?????, ???????, ?????????? item components, storage bindings, JSON-????????? loot tables ????? ???????????? ???????? ??????? ????? ????????, ? ????? ????????? item-model IDs. `exact` ? ???????? ?????? ???????? ????????? ??????????????? ????????? ? ???????????? ?????????, ? ?? ???????? ????????? JSON.

??????? ?????? ???????????????? scope: 20 ???????, 870 source refs ? source-line refs (870 ???????), 241/241 ????????? loot JSON ??????? ????? ????????????, 10/10 ????????? item-model IDs ??????; storage binding `swrg:lang` ??????????? ????? ?????? ??????? `gui/page/1000-lang`. ???????? ????? ? ????? ??????????? ????????? ??????????? ???????? ???????? `swrg:5000` slot 4 ? `swrg:6000` slots 12?14, ???????? `lbc:12000` slot 0 ? reset-?????? slots 24/25. ?????????????? ?????? ???????? `lbc:11100` ????????? ?? ????????????? ???????. ????????? source comparator ?????????? `design source check passed`. ???????????? ???????? scores/tags/advancements ? `fill_player_head` ??????????? ???????? ??? ???????; ??? ????????????? refs, ?? ?????? ???? ?? ???????. ?????? ????????? 27 slots ? datapack title `container.enderchest`; ??? ?? ??????????? ? ?????????? ????????? ?? ???????.
