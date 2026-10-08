"""One-time editorial integration of the Java research reports (not a validator)."""
from pathlib import Path

NOTES = Path(__file__).resolve().parents[1]


def replace(name, old, new):
    path = NOTES / name
    raw = path.read_text(encoding="utf-8")
    if old not in raw:
        raise ValueError(f"Expected passage missing: {name}")
    if "(user)" in old:
        raise ValueError("Cannot rewrite a passage containing the user marker")
    path.write_text(raw.replace(old, new, 1), encoding="utf-8")


replace("README.md", "Источник истины: файлы в `world/datapacksdisabled/LBC`, `maps` и `swarogskywars`.\nДокументируем поведение этих исходников, а не уже перенесённого Java-плагина.",
        "Исходники datapack: `world/datapacksdisabled/LBC`, `maps` и `swarogskywars`.\nИх поведение описано отдельно от Java-реализаций старого сервера. Для команд,\nпредметных слушателей и native/legacy runtime также изучены `plugins/luckywars`\nи `mods/luckywarsignite`; эти реализации могут расходиться с datapack.")
replace("README.md", "## Как читать\n", "## Как читать\n\nНовые обзоры Java: [команды](COMMAND_IMPLEMENTATIONS.md),\n[предметы и слушатели](PLUGIN_ITEMS_AND_HOOKS.md),\n[нативная игра и legacy runtime](NATIVE_RUNTIME_OVERVIEW.md).\nИх приёмка и границы — [JAVA_EXTENSION_REVIEW.md](JAVA_EXTENSION_REVIEW.md).\n")
replace("INDEX.md", "Документация снимка принята полностью: 35 заданий, 10 800 источников.",
        "Документация datapack-снимка принята полностью: 35 заданий, 10 800 источников.\nОбзоры Paper/Ignite описаны отдельно ниже; число 35/35 к Java не относится.")
replace("INDEX.md", "## Тематические документы\n", "## Реализации Paper и Ignite\n\n| Тема | Документ |\n|---|---|\n| Регистрация, аргументы, селекторы, permissions и эффекты команд | [COMMAND_IMPLEMENTATIONS.md](COMMAND_IMPLEMENTATIONS.md) |\n| Предметы без команд, мана, UI, BedWars и mixin hooks | [PLUGIN_ITEMS_AND_HOOKS.md](PLUGIN_ITEMS_AND_HOOKS.md) |\n| Запуск плагина, native/legacy игра, карты и встроенные ресурсы | [NATIVE_RUNTIME_OVERVIEW.md](NATIVE_RUNTIME_OVERVIEW.md) |\n| Сверка результатов и сохранность источников | [JAVA_EXTENSION_REVIEW.md](JAVA_EXTENSION_REVIEW.md) |\n\nАвторские уточнения и требования с пометкой `(user)` важнее агентского текста.\nПравила сохранения — [AGENTS.md](AGENTS.md).\n\n## Тематические документы\n")
replace("COMMAND_BRIDGES.md", "Наличие регистрации в коде отличается от проверки работающего сервера.",
        "Наличие регистрации в коде отличается от проверки работающего сервера.\n\nПредметы, запускаемые без команд через события и задачи плагина, описаны в\n[PLUGIN_ITEMS_AND_HOOKS.md](PLUGIN_ITEMS_AND_HOOKS.md): Nexus Compass, Rocket Armor,\nMetal Pipe, Malevich и Terra Wand. Native/legacy запуск и ограничения загрузки\nресурсов — в [NATIVE_RUNTIME_OVERVIEW.md](NATIVE_RUNTIME_OVERVIEW.md).")
replace("WORKFLOW.md", "## Роли пакетов\n", "## Java-реализации старого сервера\n\nЭтот workflow описывает datapack. Параллельно в Paper-плагине есть собственные\nсостояния игры, GUI, мана и слушатели предметов. Их запуск не равнозначен вызову\nстарых функций. В обоих просмотренных `config.yml` стоит `legacy.enabled: false`:\nэта настройка пропускает регистрацию LegacyFunctionRuntime и SwrgNativeService,\nа нативные сервисы продолжают создаваться.\n\nКомандные входы datapack связаны с [COMMAND_IMPLEMENTATIONS.md](COMMAND_IMPLEMENTATIONS.md).\nПредметы через события/задачи — с [PLUGIN_ITEMS_AND_HOOKS.md](PLUGIN_ITEMS_AND_HOOKS.md).\nЦикл нативного матча и незавершённые участки — с\n[NATIVE_RUNTIME_OVERVIEW.md](NATIVE_RUNTIME_OVERVIEW.md).\nЭто описания исходников; запуск совместной конфигурации на сервере не проверялся.\n\n## Роли пакетов\n")
replace("RESEARCH_STATUS.md", "Исследуются обработчики и слушатели старого Paper-плагина `plugins/luckywars`\nи Ignite-мода `mods/luckywarsignite`. Их результаты будут храниться отдельно\nот принятого datapack-снимка: commands, items/hooks, native/runtime.",
        "Обработчики и слушатели старого Paper-плагина `plugins/luckywars`\nи Ignite-мода `mods/luckywarsignite` изучены тремя агентами `gpt-6-luna`.\nРезультаты хранятся отдельно от принятого datapack-снимка:\n[commands](COMMAND_IMPLEMENTATIONS.md), [items/hooks](PLUGIN_ITEMS_AND_HOOKS.md),\n[native/runtime](NATIVE_RUNTIME_OVERVIEW.md). Координатор сверил регистрации,\nспорные условия, ссылки и SHA источников; основания и границы —\n[JAVA_EXTENSION_REVIEW.md](JAVA_EXTENSION_REVIEW.md).")
replace("COMPLETION_AUDIT.md", "Пользователь добавил Java-источники Paper-плагина и Ignite-мода; их отдельный\nразбор продолжается. Старые «35/35» не означают завершённую проверку Java\nили полный рабочий перенос в Fabric.",
        "Пользователь добавил Java-источники Paper-плагина и Ignite-мода. Их отдельные\nобзоры команд, слушателей и runtime собраны и связаны в\n[JAVA_EXTENSION_REVIEW.md](JAVA_EXTENSION_REVIEW.md). Старые «35/35» относятся\nтолько к datapack и не означают полный рабочий перенос в Fabric.")
