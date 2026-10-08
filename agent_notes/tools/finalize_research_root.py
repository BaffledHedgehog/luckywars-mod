"""Record root's completed semantic acceptance after final document review."""
import json
from pathlib import Path
N=Path('agent_notes')
for name in ['review_final_root.md','review_final_text_groups.md','review_final_other_groups.md','INTEGRATION_REVIEW.md']:
    assert (N/name).stat().st_size>1000,name
p=N/'inventory/research_jobs.json';jobs=json.loads(p.read_text(encoding='utf-8'))
pending={k:v for k,v in jobs.items() if not v.get('content_reviewed')}
assert len(pending)==7 and sum(len(v['sources']) for v in pending.values())==3155
for v in pending.values():
    v.update(status='accepted',content_reviewed=True,review_pass_status='accepted_by_root',final_review_document='agent_notes/review_final_root.md')
assert len(jobs)==35 and all(v.get('content_reviewed') for v in jobs.values())
p.write_text(json.dumps(jobs,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

(N/'RESEARCH_STATUS.md').write_text('''# Состояние исследования

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
''',encoding='utf-8')

(N/'COMPLETION_AUDIT.md').write_text('''# Проверка требований к итоговой документации

Требования к исследованию исходного снимка выполнены. Приняты все 35 заданий
по 10 800 файлам; это приёмка содержания вместе с проверяемым coverage.

| Требование | Подтверждение |
|---|---|
| Исследовать три datapack целиком | [files.json](inventory/files.json), [research_jobs.json](inventory/research_jobs.json), [RESEARCH_PROGRESS.md](RESEARCH_PROGRESS.md): 35/35 приняты |
| Делегировать дешёвым исследователям и хранить отдельные MD | [RESEARCH_STATUS.md](RESEARCH_STATUS.md), тематические документы и peer reports; gpt-6-luna |
| Кратко и по-русски описать поведение предметов/механик | [INDEX.md](INDEX.md), [ITEM_LOOKUP.md](ITEM_LOOKUP.md); получение, событие, условия, эффект, числа, расход/откат, остановка и reset |
| Связать игровой workflow и предметные события | [WORKFLOW.md](WORKFLOW.md), [INTEGRATION_REVIEW.md](INTEGRATION_REVIEW.md): обе руки, выдача, смерть, новая игра, mapload |
| Описать карты/NBT | [maps_catalog.md](maps_catalog.md), [maps_details.json](inventory/maps_details.json): все 1 456 NBT, палитры, блоки и сущности |
| Отделить реальные границы, ошибки и внешние команды | [SOURCE_FINDINGS.md](SOURCE_FINDINGS.md), [RESOURCE_BOUNDARIES.md](RESOURCE_BOUNDARIES.md), [COMMAND_BRIDGES.md](COMMAND_BRIDGES.md) |
| Проверяемое покрытие и ссылки | [coverage_report.json](inventory/coverage_report.json), [behavior_section_audit.json](inventory/behavior_section_audit.json), [markdown_link_audit.json](inventory/markdown_link_audit.json); точные заголовки и непустые разделы |
| Сохранить исходный datapack | [source_snapshot_audit.json](inventory/source_snapshot_audit.json): 10 800 SHA, изменений=0 |
| Назвать место datapack в моде | [DATAPACK_EMBEDDING.md](DATAPACK_EMBEDDING.md): resources/data нового Fabric-проекта |

Найденный пустой Ice Wand раздел заполнен и проверен. Последние исправления
и основания приёмки: [review_final_root.md](review_final_root.md).
Одинаковые проверенные варианты объединены в семейства с полными таблицами;
общие helper-ы связаны с конечными действиями соседних тематических документов.

Полнота относится к имеющимся исходникам. Реализацию отсутствующих host-команд,
поведение действующего сервера и совместимость переноса в Minecraft 26.3
этот результат не подменяет предположениями или runtime-тестами.
''',encoding='utf-8')

p=N/'REVIEW.md';s=p.read_text(encoding='utf-8');a=s.index('## Текущий итог приёмки');b=s.index('## История замечаний',a)
s=s[:a]+'''## Итоговая приёмка

Координатор принял **35 из 35 заданий** по 10 800 источникам. Итоговые
основания: [root report](review_final_root.md), [четыре other-группы](review_final_other_groups.md),
[семь текстовых групп](review_final_text_groups.md), [сквозная интеграция](INTEGRATION_REVIEW.md).
Текущий счётчик — [RESEARCH_PROGRESS.md](RESEARCH_PROGRESS.md).

Пустой Ice Wand раздел заполнен и проверен по всем 28 функциям; сверены
Motion/Health, cadence, barrier, area damage и cleanup. Исправлены оставшиеся
английские описания навыков и ошибка Frozen Heart (тики передаются как секунды).
Исходные datapacks не менялись. Проверки ссылок и непустого тела раздела
дополняют source-проходы; они не служили единственным основанием приёмки.

Ниже сохранена история проверок. Упоминания незавершённости и старые результаты
в исторических записях относятся к моменту соответствующего прохода.

'''+s[b:];p.write_text(s,encoding='utf-8')

p=N/'INDEX.md';s=p.read_text(encoding='utf-8');a=s.index('Текущая документация ещё');b=s.index('## Тематические документы',a)
s=s[:a]+'''Документация снимка принята полностью: 35 заданий, 10 800 источников.
Сквозные предметные пути — [INTEGRATION_REVIEW.md](INTEGRATION_REVIEW.md).
Полноту отражают [RESEARCH_PROGRESS.md](RESEARCH_PROGRESS.md), организация —
[RESEARCH_STATUS.md](RESEARCH_STATUS.md), аудит — [COMPLETION_AUDIT.md](COMPLETION_AUDIT.md).

'''+s[b:];p.write_text(s,encoding='utf-8')

p=N/'WORKFLOW.md';s=p.read_text(encoding='utf-8')
s=s.replace('отдельные документы ещё проходят проверку.', 'все задания снимка приняты координатором.')
p.write_text(s,encoding='utf-8')
p=N/'README.md';s=p.read_text(encoding='utf-8')
s=s.replace('## Как читать','Приёмка завершена: **35 заданий, 10 800 источников**. Подтверждения —\n[COMPLETION_AUDIT.md](COMPLETION_AUDIT.md).\n\n## Как читать',1)
s=s.replace('пополняемая схема игры', 'схема игры').replace('текущие задания и очередь незавершённых участков','организация завершённого исследования и границы результата')
p.write_text(s,encoding='utf-8')
print('Root semantic acceptance recorded: 35/35 jobs, 10800 sources')
