# Указатель документации SkyWars

Начните с [WORKFLOW.md](WORKFLOW.md): он связывает матч, LBC и карты. Для поиска предмета по имени, модели или custom_data используйте [ITEM_LOOKUP.md](ITEM_LOOKUP.md). Место данных в новом моде: [DATAPACK_EMBEDDING.md](DATAPACK_EMBEDDING.md).

Ресурс-пак пользователя, правила работы через template и требование сохранять
оформление предметов один в один: [RESOURCE_PACK.md](RESOURCE_PACK.md).

Исходные тексты, оформление и выявленные пробелы прежней документации:
[DESIGN_SOURCE_AUDIT.md](DESIGN_SOURCE_AUDIT.md). Точные исходники и модели —
[design_source_manifest.json](inventory/design_source_manifest.json);
восстановление игровых сообщений — [GAME_TEXT_DESIGN_RESTORE.md](GAME_TEXT_DESIGN_RESTORE.md).

Документация datapack-снимка принята полностью: 35 заданий, 10 800 источников.
Обзоры Paper/Ignite описаны отдельно ниже; число 35/35 к Java не относится.
Сквозные предметные пути — [INTEGRATION_REVIEW.md](INTEGRATION_REVIEW.md).
Полноту отражают [RESEARCH_PROGRESS.md](RESEARCH_PROGRESS.md), организация —
[RESEARCH_STATUS.md](RESEARCH_STATUS.md), аудит — [COMPLETION_AUDIT.md](COMPLETION_AUDIT.md).

## Реализации Paper и Ignite

| Тема | Документ |
|---|---|
| Регистрация, аргументы, селекторы, permissions и эффекты команд | [COMMAND_IMPLEMENTATIONS.md](COMMAND_IMPLEMENTATIONS.md) |
| Предметы без команд, мана, UI, BedWars и mixin hooks | [PLUGIN_ITEMS_AND_HOOKS.md](PLUGIN_ITEMS_AND_HOOKS.md) |
| Запуск плагина, native/legacy игра, карты и встроенные ресурсы | [NATIVE_RUNTIME_OVERVIEW.md](NATIVE_RUNTIME_OVERVIEW.md) |
| Сверка результатов и сохранность источников | [JAVA_EXTENSION_REVIEW.md](JAVA_EXTENSION_REVIEW.md) |

Авторские уточнения и требования с пометкой `(user)` важнее агентского текста.
Правила сохранения — [AGENTS.md](AGENTS.md).

## Тематические документы

| Тема | Источников в задании | Документы |
|---|---:|---|
| Цикл матча, лобби, этапы, смерть, победа и сброс | 123 | [skywars_game.md](skywars_game.md) |
| Меню, кнопки, цены, покупки, роллы и настройки | 397 | [skywars_gui.md](skywars_gui.md) |
| Выбор/загрузка карт и инструменты редактора | 234 | [skywars_maps_admin.md](skywars_maps_admin.md) |
| События SkyWars, суффиксы, теги и вспомогательные данные | 83 | [skywars_support.md](skywars_support.md) |
| Все NBT: карты, сундуки, сущности и маркеры | 1457 | [maps_catalog.md](maps_catalog.md) |
| Все события/критерии предметов и достижения | 656 | [lbc_advancements.md](lbc_advancements.md) |
| Броня, комплекты и их активные эффекты | 77 | [lbc_armor.md](lbc_armor.md) |
| Ветка BedWars в LBC | 27 | [lbc_bedwars.md](lbc_bedwars.md) |
| Дропы лаки-блоков, ловушки, постройки и боссы | 135 | [lbc_drops.md](lbc_drops.md) |
| Периодические положительные/отрицательные эффекты | 46 | [lbc_effects.md](lbc_effects.md) |
| Арканумы, тринкеты и прочие предметы | 201 | [lbc_items_arcana_trinkets.md](lbc_items_arcana_trinkets.md) |
| Луки, арбалеты, щиты, мечи, SLR и жезлы | 256 | [lbc_items_weapons.md](lbc_items_weapons.md) |
| Действие наборов, талантов, улучшений и эффектов убийства | 1355 | [lbc_kit_game.md](lbc_kit_game.md) |
| Меню выбора наборов, талантов, улучшений и тринкетов | 146 | [lbc_kit_interface.md](lbc_kit_interface.md) |
| Namespace lbcr: лучи, урон и завершение попаданий | 347 | [lbc_lbcr.md](lbc_lbcr.md) |
| Состав выдаваемых предметов и случайные таблицы | 669 | [lbc_loot_definitions.md](lbc_loot_definitions.md) |
| Все результаты разборки предметов | 625 | [lbc_loot_uncrafted.md](lbc_loot_uncrafted.md) |
| Атрибуты, таблетки, выброс инвентаря и сброс игры | 331 | [lbc_other_attributes.md](lbc_other_attributes.md) |
| Еда, мана, крафт и разборка предметов | 221 | [lbc_other_consumables_craft.md](lbc_other_consumables_craft.md), [lbc_craft_random_mana.md](lbc_craft_random_mana.md) |
| События, Halloween, bucks и troll box | 137 | [lbc_other_events.md](lbc_other_events.md) |
| Imba-предметы, артефакты и God Hedgehog | 293 | [lbc_other_imba.md](lbc_other_imba.md) |
| Магическая академия, гримуар и Sauvojen Tuntija | 247 | [lbc_other_magic.md](lbc_other_magic.md) |
| Прочие предметы, наземный крафт и преобразования | 215 | [lbc_other_main.md](lbc_other_main.md), [lbc_mana.md](lbc_mana.md) |
| Остальные самостоятельные механики other | 214 | [lbc_other_misc.md](lbc_other_misc.md) |
| Nexus, технологии, Wisp, железная дорога и nuke | 253 | [lbc_other_world_tech.md](lbc_other_world_tech.md) |
| Проверки условий и изменения компонентов | 230 | [lbc_predicates_modifiers.md](lbc_predicates_modifiers.md) |
| Снаряды и гаджеты: попадания и owner | 70 | [lbc_projectiles.md](lbc_projectiles.md) |
| Старые эмиттеры Sauvojen и усиление чёрной дыры | 20 | [lbc_raycast.md](lbc_raycast.md) |
| Теги, урон, измерения, worldgen и диалоги | 314 | [lbc_registry_support.md](lbc_registry_support.md) |
| Load/tick, остановка времени, owner, общие helpers | 250 | [lbc_runtime.md](lbc_runtime.md) |
| Предметный интерфейс и индикаторы LBC | 11 | [lbc_ui.md](lbc_ui.md) |
| Пробуждённые жезлы Tuntija | 277 | [lbc_workingitems_awaken.md](lbc_workingitems_awaken.md) |
| Генераторы и Sauvojen staffs | 324 | [lbc_workingitems_generators.md](lbc_workingitems_generators.md) |
| Основные обработчики рабочих предметов | 271 | [lbc_workingitems_main.md](lbc_workingitems_main.md) |
| Вспомогательные предметные подсистемы и боссы | 288 | [lbc_workingitems_other.md](lbc_workingitems_other.md) |

## Проверенные границы исходников

- [SOURCE_FINDINGS.md](SOURCE_FINDINGS.md): пустые JSON, время, контекст, геометрия и округление.
- [RESOURCE_BOUNDARIES.md](RESOURCE_BOUNDARIES.md): отсутствующие определения и динамические вызовы.
- [COMMAND_BRIDGES.md](COMMAND_BRIDGES.md): подстановка команд плагинов и граница внешней реализации.
- [maps_catalog.md](maps_catalog.md): каждый NBT и сопоставление с placement-командами.

Машинные индексы и coverage находятся в `inventory/` и `coverage/`. Инструменты в `tools/` позволяют повторить инвентаризацию и проверку ссылок; они не запускают игровую механику и не изменяют datapacks.
