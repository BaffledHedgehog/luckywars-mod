# Независимая сверка оружия: итоговый проход

## Что проверено в этом проходе

Набор `lbc_items_weapons` содержит 256 точных источников. Каждый был лично прочитан в предыдущем независимом pass, отражённом в [review_weapons_arcana.md](review_weapons_arcana.md); новый [manifest](inventory/review_weapons_final_manifest.json) связывает каждый путь с coverage и фиксирует SHA-256, размер, строки и число команд. В этом refinement-pass повторно прочитаны 3 источника из точного job и 12 внешних файлов поддержки — advancement-ы, player loops и root dispatchers, которые определяют cadence и обе руки. Здесь зафиксированы только новые корректировки cadence/dispatcher, не повтор всего ранее проведённого аудита.

## Исправления в основном документе

В [lbc_items_weapons.md](lbc_items_weapons.md) добавлено объяснение входов и cadence. Раньше формулировка Banhammer могла прочитываться как заряд от каждого тика удержания: теперь явно указано, что `gowno` вызывает `items/swords` от `player_hurt_entity`, фильтрует arrow-only damage и отзывается для следующего события. Banhammer, Blazefire и Fevgalea получают charge по обработанным ударам; активный Blazefire-проход вызывается `players_2t` раз в 2 тика и тратит 3 заряда.

Луки и арбалеты описаны раздельно: bow pull использует `using_item`, crossbow firing — `shot_crossbow`, а charging — отдельный `using_item` advancement. Дальнейшая работа со счётчиками идёт в `players_tick`; cadence не приписывается одному только удержанию предмета без привязки к advancement-у.

## Перекрёстная проверка обеих рук

`items/spells` вызывается advancement-ом правого взаимодействия с сущностью `rc_able`. Правую руку он выбирает через `SelectedItem`; левую проверяет отдельно через `weapon.offhand` и вызывает `spell_left`, `spell1_left` или `spellsummoner_left`. Левый каст пропускается при `lefthand=1`; `nomagic_active` и `stopper_magic` применяются по отдельным маршрутам. В арканум-dispatcher `players_tick` независимо вызывает `select` для `weapon.mainhand` и `select2` для `weapon.offhand`, с отдельными `arcanum_use`/`arcanum_use2` cleanup ветками. Проверенные offhand helpers spells/arcana проверяют сам слот `weapon.offhand`; не следует автоматически переносить это на дочерние функции, где источник выбирается через `SelectedItem`.

У этих двух root-маршрутов разная частота: `items/spells` — событие взаимодействия, дополнительно разрешённое из player loop при `RC_Raycast>=1`; `arcanums/select` и `select2` — регулярная обработка из `players_tick`, пока предмет с `arcanumt:1` находится в соответствующем слоте и рядом нет `nomagic_active`/`stopper_magic`. Поэтому offhand spell-cast и offhand arcanum-passive нельзя описывать одним общим «кликом». Root вызывает main/offhand обработчики отдельно; если предметы есть в обеих руках, обе ветви могут выполниться за один тик.

Ключевое исключение вне этих selectors — `danmaku_staff/tick`: ветки для обеих рук существуют, а upgraded-проверка mainhand дополнительно использует инвертированный `SelectedItem` NBT. Поэтому этот helper требует отдельной ручной оценки при проверке upgrade-режима, даже когда слот-условие указывает руку.

В дочерних функциях семейства `items/arcanums` проверено, что `SelectedItem` встречается только в mainhand-selector `select`; `select2` и его локальные вызовы используют `weapon.offhand`. В `items/spells` left-hand helpers также сохраняют offhand-слот, а `items/wands/danmaku_staff/tick` использует `SelectedItem` только в базовой mainhand-ветке; обе offhand-ветки проверяют непосредственно `weapon.offhand`. Координатор сверил полный файл: утверждать проверку upgrade левой руки по SelectedItem здесь неправильно. Это вывод только о просмотренных root/helper цепочках; внешние `workingitems/**` и `lbcr:*` сохраняются как отдельная граница исполнения.

## Пределы текущего отчёта

Этот refinement ограничен вопросами частоты срабатывания и маршрутизации рук; полная исходная независимая проверка 256 источников зафиксирована в предыдущем отчёте. Здесь не менялись research job/content_reviewed статусы.

## Ручные опорные источники

- [advancement/gowno.json](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/gowno.json) — критерий sword-hit и reward.
- [function/items/swords.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords.mcfunction) — выбор эффекта меча при попадании.
- [function/items/crossbows.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/crossbows.mcfunction) — запуск при выстреле.
- [function/players_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction) и [function/players_2t.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_2t.mcfunction) — периодические циклы и dispatcher арканумов.
- [advancement/interaction_rightclickable.json](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/interaction_rightclickable.json), [function/items/spells.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/spells.mcfunction), [function/items/arcanums/select2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/select2.mcfunction) — right-click входы и offhand selectors.
