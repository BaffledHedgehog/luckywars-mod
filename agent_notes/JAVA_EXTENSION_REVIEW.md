# Дополнение: Paper-плагин и Ignite-мод

Документация продолжается в `E:/servers_mew/skywars2_last/luckywars-mod/agent_notes`.
Источники остаются на старом сервере: `plugins/luckywars` и `mods/luckywarsignite`
в `E:/servers_mew/cringe1_21_4_skywars`. Исследование не меняло игровой код,
настройки или datapack и не запускало сервер.

## Отдельные отчёты исследователей

| Агент (`gpt-6-luna`) | Результат | Источники и SHA |
|---|---|---|
| plugin_commands | [COMMAND_IMPLEMENTATIONS.md](COMMAND_IMPLEMENTATIONS.md): обработчики команд, аргументы, контекст и побочные эффекты | [manifest](inventory/plugin_commands_review.json) |
| plugin_listeners | [PLUGIN_ITEMS_AND_HOOKS.md](PLUGIN_ITEMS_AND_HOOKS.md): предметы без команд, ресурсы, UI, BedWars и Ignite hooks | [manifest](inventory/plugin_items_review.json) |
| plugin_runtime | [NATIVE_RUNTIME_OVERVIEW.md](NATIVE_RUNTIME_OVERVIEW.md): запуск, игра, GUI, карты, legacy и встроенные данные | [manifest](inventory/plugin_runtime_review.json) |

Общая инвентаризация — [java_extension_sources.json](inventory/java_extension_sources.json).
Она содержит 91 Java-файл и 10 файлов настроек/сборки/ресурсов; сама по себе
не доказывает изученное поведение. Проверка отдельных manifests и сохранности
снимка — [java_extension_review.json](inventory/java_extension_review.json).
В manifests перечислены все 101 файл общего снимка и ещё восемь прямых
зависимостей: четыре loot table для связи PDC-предметов и четыре файла настройки
сборки Ignite. Всего 109 разных источников; SHA совпадают, непривязанных файлов
общего снимка нет. Эти числа подтверждают состав просмотренных источников,
а не заменяют чтение и проверку описаний поведения.
Описания группируют связанные классы; это обзор имеющейся реализации по коду,
а не проверка завершённого игрового порта.

## Что уточнено при приёмке

- `COMMAND_BRIDGES.md` теперь связывает macro-вызовы с найденными Java-командами.
  `transfer`, `kick`, `restart` не объявлены в просмотренном реестре LuckyWars;
  разрешение этих имён остаётся за серверной платформой и окружением.
- Предметные listeners регистрируются из `SigmaWarsMain`, независимо от включения
  legacy runtime. Пустой или иной обработчик в datapack не описывает их полный эффект.
- В обоих просмотренных `config.yml` стоит `legacy.enabled: false`. Это выключает
  регистрацию LegacyFunctionRuntime и SwrgNativeService, сохраняя запуск нативных
  сервисов. В нативной игре найдены TODO: их нельзя считать выполненным поведением.
- `buildmanager` читает первый аргумент как цель и не проверяет переданный datapack
  пароль; `mapclear` игнорирует аргумент мира; permission `bedwars` в executor
  отличается от объявленного в `plugin.yml`.
- `/regen` прибавляет положительную скорость прямо к HP каждый тик. Игрок в
  `minecraft:imprinted` вызывает `return` из всей tick-задачи: позже перечисленные
  игроки и мобы в этом тике не получают обработку. Отрицательная скорость не наносит урон.
- Nexus Compass исключает только `minecraft:imprinted`. Metal Pipe, напротив,
  исключает Nexus и Imprinted; урон не исключает владельца или союзников.
- Ignite защита сравнивает новый Material с исходным снимком региона, а не с
  текущим блоком. Наличие injection в исходнике не подтверждает применение mixin.
- Paper-плагин собирается для `1.21.11-R0.1-SNAPSHOT`, а Ignite-проект задаёт
  Paper `1.21.1-R0.1-SNAPSHOT` через version catalog. Совместимость этих
  исходников и установленного бинарного файла не проверялась.
- `src/main/resources/legacy-datapacks` отсутствует, но в `build/resources/main`
  лежит прежний снимок из 10 800 файлов. Найденные build-ресурсы не подтверждают,
  что свежая сборка повторно вложит эти данные; сборка здесь не запускалась.

## Перенос документации и пользовательские правки

После переноса исправлены 23 274 ссылки на исходный сервер в 48 документах:
[relocation_audit.json](inventory/relocation_audit.json). Coverage сохраняет
первоначальные идентификаторы; поддерживаемые проверки берут корни из
[source_roots.json](inventory/source_roots.json).

Пометка `(user)` делает авторский абзац/блок приоритетным. Он сохраняется дословно.
Если это желаемое изменение, документация фиксирует его как требование и отдельно
поясняет фактический текущий код. Правила для следующих агентов закреплены в
[AGENTS.md](AGENTS.md) и [AGENTS проекта](../AGENTS.md). Исторические генераторы
не следует повторно запускать поверх пользовательских исправлений.
