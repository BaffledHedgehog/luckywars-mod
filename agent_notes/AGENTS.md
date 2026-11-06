## Eleven native Quiver arrows - QUALIFIED, INSTALLED (6 November 2026)

[Canonical identities, nullable owners and exact proof](NATIVE_QUIVER_ARROWS_2026_11_06.md).
11 ready native arrows/19 source count profiles, common TypedArrow/Polymer/dispenser factory.
Remaining ordinary ownerless101/102/103/111/112/113 and Stand ground return share existing owners.
Nullable attachments/children/clones; actual carrier world; separate WILD/projectile reload flags.
Original29-entry/281-weight Quiver pool and authored counts/styles preserved.
Test4f897dd6/production8c24ee85:10 isolated mode1/mode3 matches pass.
12 families/31 resources;10213 unchanged inputs/282 author paths/RP preserved.
Main/both libs installed without restart; Gradle512MiB/one worker, fixture2GiB only after RAM recovered.
Full IMBA/TMT-last/mode/skill2/broad rewind gates unchanged.

## Eight ownerless IMBA arrows - QUALIFIED, INSTALLED (6 November 2026)

[Shared native authorities and exact proof](OWNERLESS_IMBA_ARROWS_2026_11_06.md).
Existing eight native IMBA arrows retain full payloads from true native dispensers.
Nullable provenance shares Quiver/TNT/nuclear/vacuum/death/electrical owners; no fake player.
Clone age/terminal state, actual owner transfer and raw pending-death adoption retained.
Test08972332/production6670ffe1:8 isolated mode1/mode3 matches pass.
9 families/0 resources;10235 unchanged inputs/282 author paths/RP preserved.
Main/both libs installed without restart; full IMBA/TMT-last/mode/skill2/rewind gates unchanged.
Other ordinary ownerless Quiver variants remain separate.

## Native utility inputs - QUALIFIED, INSTALLED (6 November 2026)

[Source, shared factories and exact proof](NATIVE_UTILITY_INPUTS_2026_11_06.md).
Four ready inputs: Shadow Glove, Toothbrush, IMBA Lightning spell and nuclear bomb.
Six canonical definitions retain all original functions/counts; Bridge/BadApple leaves share them.
Common wand use/reward rays and native marker/nuclear owners; no duplicate ability engines.
Removed duplicate prototype parsers; restored BadApple EPIC and correct block/fluid use stats.
Testdb689c9c/production1d5452ef:12 isolated mode1/mode3 matches pass.
12 families/12 resources;282 author paths/RP preserved; main/both libs installed without restart.
Full IMBA/TMT-last/mode/skill2/broad-rewind gates unchanged.

## Eight native IMBA arrows - QUALIFIED, INSTALLED (6 November 2026)

[Source, shared owners and exact proof](NATIVE_IMBA_ARROWS_2026_11_06.md).
Eight ready leaves; source1..10 counts/components/styles retained; native ammo tags.
Strong/weak, delayed death, reactive TNT, nuclear/vacuum/paralysis/electric payloads.
Evo retains author-confirmed WILD100-block nonplayer pull; canonical visuals follow.
Shared physical/Shulker4HP/beam/trinket owners; TimeStop/TECH; no duplicate engines.
Testee25e74d/production2d654998:12 isolated native matches;21 families/17 resources.
Main/both libs installed without restart;282 author paths/RP preserved.
Full IMBA/TMT-last/mode/skill2/broad-rewind gates unchanged.

# Работа с документацией SkyWars

Актуальное место документации: эта папка `agent_notes` в новом Fabric-проекте.
Исходный сервер остаётся в `E:/servers_mew/cringe1_21_4_skywars`.

Дополнительный источник оформления предметов — ресурс-пак пользователя.
Его путь и обязательные правила чтения, шаблонов и сохранения дизайна находятся
в [RESOURCE_PACK.md](RESOURCE_PACK.md). Этот источник разрешено только читать;
его содержимое не входило в ранее принятый аудит трёх datapack.

## Приоритет пользовательских пометок

- Текст с пометкой `(user)` — авторское уточнение или требование пользователя.
  Он важнее выводов и сгенерированных описаний исследователей.
- Перед изменением документа прочитай его пользовательские пометки и связанный
  контекст. Сохраняй их дословно; не удаляй, не переписывай и не заменяй
  автоматически при повторной генерации документа.
- Если пользователь меняет желаемое поведение, веди его как целевое требование.
  Отдельно укажи, что текущий код пока делает иначе. Нельзя объявлять пожелание
  уже реализованным лишь потому, что оно появилось в документации.
- При конфликте исправляй агентский текст вокруг пометки или добавляй пояснение
  о расхождении. Саму пометку не «исправляй» по старому коду.
- Специальный формат кроме пометки `(user)` не требуется: учитывай относящийся
  к ней авторский абзац/блок. Если границы неоднозначны, сохраняй весь фрагмент.

## Источники и пределы выводов

Наследование автора убийства обязательно для всех предметов (user):
по пояснению пользователя, в старом датапаке оно отсутствовало из-за
невозможности реализации. При исследовании фиксируй источник прямого урона
и отложенных последствий; отсутствие атрибуции в исходнике отмечай как пробел
для исправления при переносе, а не как желаемое поведение.
Общий контракт и фактическое покрытие: [INDIRECT_KILLS.md](INDIRECT_KILLS.md).

Исследуются исходные datapack, Paper-плагин `plugins/luckywars` и Ignite-мод
`mods/luckywarsignite` старого сервера. Различай определение класса, его живую
регистрацию, наблюдаемую логику и фактически проверенный запуск. Наличие кода
само по себе не означает завершённую или работающую реализацию.

Не изменяй исходный сервер или код нового мода в ходе исследования документации.
Каждый исследователь сохраняет свой Markdown и точные ссылки на источники.
Используй UTF-8. Не запускай старые генераторы поверх исправленных/user-текстов:
исторические генераторы могут перезаписать ручные уточнения.

Поддерживаемые проверки после переноса: `check_coverage.py`,
`check_behavior_sections.py`, `check_markdown_links.py`, `audit_source_snapshot.py`
и `update_research_progress.py`. Java manifests и их SHA проверяет
`audit_java_extension.py`; эта проверка не оценивает полноту поведения.
Корни источников заданы в
`inventory/source_roots.json`; исходные идентификаторы coverage не переименовывай.
