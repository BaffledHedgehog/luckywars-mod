# Состояние исследования

## Расширение на Java и новое место хранения

Документация перенесена в `E:/servers_mew/skywars2_last/luckywars-mod/agent_notes`.
Обработчики и слушатели старого Paper-плагина `plugins/luckywars`
и Ignite-мода `mods/luckywarsignite` изучены тремя агентами `gpt-6-luna`.
Результаты хранятся отдельно от принятого datapack-снимка:
[commands](COMMAND_IMPLEMENTATIONS.md), [items/hooks](PLUGIN_ITEMS_AND_HOOKS.md),
[native/runtime](NATIVE_RUNTIME_OVERVIEW.md). Координатор сверил регистрации,
спорные условия, ссылки и SHA источников; основания и границы —
[JAVA_EXTENSION_REVIEW.md](JAVA_EXTENSION_REVIEW.md).
Пометки `(user)` важнее агентских описаний; [AGENTS.md](AGENTS.md) закрепляет
их сохранность и отличие желаемого поведения от уже реализованного.

## Первоначальный datapack-снимок

Итоговая приёмка завершена: **35 из 35 заданий, 10 800 источников**.
Описано поведение снимка отключённых LBC, maps и swarogskywars по исходникам.
Полный счётчик — [RESEARCH_PROGRESS.md](RESEARCH_PROGRESS.md), основания
приёмки — [REVIEW.md](REVIEW.md), проверка требований —
[COMPLETION_AUDIT.md](COMPLETION_AUDIT.md).

## Организация

Исследование делегировано дешёвым агентам `gpt-6-luna`. Каждый сохранял
отдельные тематические Markdown и coverage; координатор распределял задания,
проверял результаты и возвращал конкретные ошибки на исправление.

| Исполнитель финального прохода | Результат |
|---|---|
| lbc_drop_effects | Четыре other-группы: consumables/craft, magic, misc, main; все приняты. |
| maps_catalogue | Семь kit/LBCR/imba/workingitems групп; все приняты с последующими правками координатора. |
| skywars_game | Сквозные выбор/выдача/события/смерть/reset и отдельная сверка длительностей main/generators/LBCR. |
| Координатор | Приёмка всех 35 заданий, исправление остаточного Ice Wand текста и Frozen Heart, указатель и итоговые проверки. |

Предыдущие проходы и их исполнители отражены в тематических review reports
и manifests в inventory. [research_jobs.json](inventory/research_jobs.json)
делит источники на непересекающиеся задания; все имеют `content_reviewed=true`.

## Результат и пределы

Снимок: 6 453 функции, 2 874 JSON и 1 456 NBT, всего 10 800 файлов.
Каждый источник связан с точным непустым разделом или проверенным общим
описанием семейства/варианта. Таблицы сохраняют различающиеся условия и числа.
Полное существенное содержимое NBT: [maps_details.json](inventory/maps_details.json).

Найденный пустой Ice Wand раздел заполнен по всем 28 функциям и принят.
Ошибки единиц времени, округления, селекторов и reset исправлены в описаниях;
датапаки сохранены без изменений (SHA всех 10 800 совпадают со снимком).

Неизвестные внешние команды и отсутствующие ресурсы перечислены в
[COMMAND_BRIDGES.md](COMMAND_BRIDGES.md) и [RESOURCE_BOUNDARIES.md](RESOURCE_BOUNDARIES.md).
Это границы имеющегося кода, а не пропущенные локальные источники.
Исследование не переносит код в мод и не утверждает результат игровых
испытаний на действующем сервере.

## Проверка

`check_coverage.py`, `check_behavior_sections.py`, `check_markdown_links.py`
и `audit_source_snapshot.py` находятся в tools; результаты сохраняются
в inventory. Счётчики дополняют содержательные отчёты, не заменяя их.
