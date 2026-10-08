from pathlib import Path
import json
import re

notes = Path('agent_notes')
p = notes/'lbc_items_arcana_trinkets.md'
s = p.read_text(encoding='utf8')
s = s.replace('При неудачном чтении новый spell ID не устанавливается, ставит игроку `caster` и `raycaster`, сбрасывает `Distance` и удаляет использованный snowball.',
              'Если ближайший снежок не содержит нужные данные, этот путь не получает корректный ID из него. Далее функция ставит caster/raycaster, сбрасывает Distance и удаляет отдельно выбранный libspell-snowball.')
s = s.replace('`60 + 15×tmp`', '`45 + 15×tmp`').replace('`60+15×tmp`', '`45+15×tmp`')
s = s.replace('выбирает мобов в пределах 24 блоков, исключая игроков/союзников и специальные сущности, затем передаёт цель во внешний `lbcr:raycast_end8_mob` либо `..._sheep_blackhole` если у цели есть `{transformable:1,potion:1,succubus:1}`',
              'случайно выбирает одну сущность не-player без тега magicalsheep в24 блоках. Фильтра команды/владельца или требования типа mobs здесь нет: подходят также item/display/marker. Передаёт её во внешний `lbcr:raycast_end8_mob`; только предметная сущность со всеми тремя custom_data `{transformable:1,potion:1,succubus:1}` идёт в `..._sheep_blackhole`')
p.write_text(s,encoding='utf8')

report = '''# Проверка координатором: интеграция наборов и арканумы

Приняты два раздела: `lbc_kit_interface` (146) и `lbc_items_arcana_trinkets` (201).
Использованы независимые проходы исследователей, чтение основных человеческих
описаний координатором и прямое сопоставление спорных цепочек с исходниками.
Это приёмка локальных контрактов; дочерние workingitems/loot/raycast разделы
имеют собственные статусы проверки.

В интерфейсе исправлены включение challenge вместо toggle, недостижимый reset
после установки challenge=1, исходная позиция fill после spreadplayers,
advancement-входы эффектов молота и крови, slot5→skill33 без unlock gate,
условия входа Touhou, износ крюков в обеих руках, диапазон кирки1..17,
округление multiplier2. Все146 SHA сверены с read-manifest, полный шаблон
25 команд multiply2 и оба JSON death-reward проверены повторно.
[Манифест исследователя](inventory/lbc_kit_interface_read_manifest.json),
[проверка координатора](inventory/kit_interface_root_audit.json),
[основной интерфейс](lbc_kit_interface.md).

В арканумах исправлены Oraora по двум периодам, повторные конверсии Neutral
Ball, смещение Sun, прекращение win schedule, оба Eyelaser и ошибка kill=201
у варианта с пределом684. Удалён противоречивый старый абзац Eyelaser.
Дополнительно исправлены Mana Gem=45+15n, отсутствие ally/mobs-фильтра у
Polymorph, выбор снежка для чтения отдельно от удаления, Sunprism с двумя
попытками raycast и фактическая геометрия spreadplayers.

Полные command-модели проверяют22 вычислителя вероятности,6 отдельных
chance-macro и универсальный macro. Промежуточная acid-ветка записывает tmp,
но читает tmpd: рассчитанный шанс22..99% не применяется, что явно отражено.
Оба spell-dispatcher разобраны целиком:140/141 маршрутов, все служебные
команды и контексты; ID189 есть только у wand. Восстановлены пропущенные
ID179/228. [Полные маршруты и модели](inventory/arcana_routes_chances_audit.json),
[основной разбор](lbc_items_arcana_trinkets.md).

Исполняемые datapack-файлы не редактировались. Поведение Java-переноса и
совместимость этих команд с новым сервером26.3 здесь не заявлены.
'''
(notes/'review_kit_arcana_root.md').write_text(report,encoding='utf8')
jobs_path = notes/'inventory/research_jobs.json'
jobs = json.loads(jobs_path.read_text(encoding='utf8'))
for key in ['lbc_kit_interface','lbc_items_arcana_trinkets']:
    jobs[key]['content_reviewed'] = True
    jobs[key]['review_pass_status'] = 'accepted_by_root'
    jobs[key]['root_review_document'] = 'agent_notes/review_kit_arcana_root.md'
jobs['lbc_other_misc']['review_document'] = 'agent_notes/review_other_misc.md'
jobs['lbc_other_misc']['review_pass_status'] = 'completed_awaiting_root_acceptance'
jobs['lbc_workingitems_main']['review_document'] = 'agent_notes/review_workingitems_main.md'
jobs['lbc_workingitems_main']['review_pass_status'] = 'completed_awaiting_root_acceptance'
jobs['lbc_workingitems_other']['review_document'] = 'agent_notes/review_workingitems_other.md'
jobs['lbc_workingitems_other']['review_pass_status'] = 'completed_awaiting_root_acceptance'
jobs_path.write_text(json.dumps(jobs,ensure_ascii=False,indent=2)+'\n',encoding='utf8')

p = notes/'REVIEW.md'
s = p.read_text(encoding='utf8')
s += '''

## Приёмка интерфейса наборов и арканумов

Приняты146 источников kit_interface и201 arcana/trinkets после независимых
проходов и дополнительных исправлений координатора. Ключевые результаты:
challenge-button не toggle; death advancements подключают молот/кровь;
Mana Gem=45+15n; acid macro читает иной storage key; ID189 отсутствует у
snowball, ID179/228 восстановлены в таблице. Полные модели22 chance helpers
и140/141 spell routes проверены без расхождений с источниками.
[Отчёт и границы приёмки](review_kit_arcana_root.md).
'''
p.write_text(s,encoding='utf8')

p = notes/'RESEARCH_STATUS.md'
s = p.read_text(encoding='utf8').replace('содержание тринадцати','содержание пятнадцати')
s = re.sub(r'^\| `lbc_drop_effects` \|.*$', '| `lbc_drop_effects` | Сверены world/tech, attributes/pills, consumables/craft, magic и дополнен misc; ожидают приёмки. Workingitems other (288) повторно сверены; продолжает generators (324). |',s,flags=re.M)
s = re.sub(r'^\| `skywars_game` \|.*$', '| `skywars_game` | Runtime, drops и kit_interface приняты. Awaken, GUI и workingitems_main (271) сверены; продолжает other_main (215), затем сквозную интеграцию наборов. |',s,flags=re.M)
s = re.sub(r'^\| `maps_catalogue` \|.*$', '| `maps_catalogue` | Arcana/trinkets приняты. Weapons, kit/game и LBCR сверены; завершает полный семантический проход other_imba (293). |',s,flags=re.M)
s = s.replace('Принял13 разделов:', 'Принял15 разделов:').replace('события LBC (137). Отдельно', 'события LBC (137), kit_interface (146), arcana/trinkets (201). Отдельно')
p.write_text(s,encoding='utf8')
print('Accepted kit interface and arcana; total reviewed',sum(j.get('content_reviewed',False) for j in jobs.values()))
