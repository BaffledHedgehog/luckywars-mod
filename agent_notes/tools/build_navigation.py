"""Small readable index of researcher documents and their current scope."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NOTES = ROOT / 'agent_notes'
TITLES = {
    'maps_catalogue': 'Все NBT: карты, сундуки, сущности и маркеры',
    'skywars_game': 'Цикл матча, лобби, этапы, смерть, победа и сброс',
    'skywars_gui': 'Меню, кнопки, цены, покупки, роллы и настройки',
    'skywars_maps_admin': 'Выбор/загрузка карт и инструменты редактора',
    'skywars_support': 'События SkyWars, суффиксы, теги и вспомогательные данные',
    'lbc_runtime': 'Load/tick, остановка времени, owner, общие helpers',
    'lbc_drop_effects': 'Дропы лаки-блоков, ловушки, постройки и боссы',
    'lbc_items_weapons': 'Луки, арбалеты, щиты, мечи, SLR и жезлы',
    'lbc_items_arcana_trinkets': 'Арканумы, тринкеты и прочие предметы',
    'lbc_projectiles': 'Снаряды и гаджеты: попадания и owner',
    'lbc_raycast': 'Старые эмиттеры Sauvojen и усиление чёрной дыры',
    'lbc_effects': 'Периодические положительные/отрицательные эффекты',
    'lbc_armor': 'Броня, комплекты и их активные эффекты',
    'lbc_bedwars': 'Ветка BedWars в LBC',
    'lbc_ui': 'Предметный интерфейс и индикаторы LBC',
    'lbc_kit_interface': 'Меню выбора наборов, талантов, улучшений и тринкетов',
    'lbc_kit_game': 'Действие наборов, талантов, улучшений и эффектов убийства',
    'lbc_lbcr': 'Namespace lbcr: лучи, урон и завершение попаданий',
    'lbc_workingitems_main': 'Основные обработчики рабочих предметов',
    'lbc_workingitems_other': 'Вспомогательные предметные подсистемы и боссы',
    'lbc_workingitems_generators': 'Генераторы и Sauvojen staffs',
    'lbc_workingitems_awaken': 'Пробуждённые жезлы Tuntija',
    'lbc_other_main': 'Прочие предметы, наземный крафт и преобразования',
    'lbc_other_attributes': 'Атрибуты, таблетки, выброс инвентаря и сброс игры',
    'lbc_other_consumables_craft': 'Еда, мана, крафт и разборка предметов',
    'lbc_other_magic': 'Магическая академия, гримуар и Sauvojen Tuntija',
    'lbc_other_imba': 'Imba-предметы, артефакты и God Hedgehog',
    'lbc_other_events': 'События, Halloween, bucks и troll box',
    'lbc_other_world_tech': 'Nexus, технологии, Wisp, железная дорога и nuke',
    'lbc_other_misc': 'Остальные самостоятельные механики other',
    'lbc_advancements': 'Все события/критерии предметов и достижения',
    'lbc_loot_definitions': 'Состав выдаваемых предметов и случайные таблицы',
    'lbc_loot_uncrafted': 'Все результаты разборки предметов',
    'lbc_predicates_modifiers': 'Проверки условий и изменения компонентов',
    'lbc_registry_support': 'Теги, урон, измерения, worldgen и диалоги',
}


def main():
    jobs = json.loads((NOTES / 'inventory/research_jobs.json').read_text(encoding='utf-8'))
    lines = ['# Указатель документации SkyWars', '',
             'Начните с [WORKFLOW.md](WORKFLOW.md): он связывает матч, LBC и карты. '
             'Для поиска предмета по имени, модели или custom_data используйте '
             '[ITEM_LOOKUP.md](ITEM_LOOKUP.md). Место данных в новом моде: '
             '[DATAPACK_EMBEDDING.md](DATAPACK_EMBEDDING.md).', '',
             'Текущая документация ещё пополняется. Наличие файла в таблице означает сохранённый '
             'документ исследователя, который может оставаться черновиком. Полноту отражают '
             '[RESEARCH_PROGRESS.md](RESEARCH_PROGRESS.md), назначения — '
             '[RESEARCH_STATUS.md](RESEARCH_STATUS.md), аудит — [REVIEW.md](REVIEW.md).', '',
             '## Тематические документы', '',
             '| Тема | Источников в задании | Документы |', '|---|---:|---|']
    for key in sorted(jobs, key=lambda key: (0 if key.startswith('skywars_') else 1 if key == 'maps_catalogue' else 2, key)):
        job = jobs[key]
        documents = list(dict.fromkeys(job.get('documents', []) + ['agent_notes/' + key + '.md']))
        present = [Path(document) for document in documents if (ROOT / document).exists()]
        links = ', '.join('[' + path.name + '](' + path.name + ')' for path in present) or 'В очереди; файл ещё не создан'
        lines.append('| ' + TITLES.get(key, key) + ' | ' + str(len(job['sources'])) + ' | ' + links + ' |')
    lines.extend(['', '## Проверенные границы исходников', '',
                  '- [SOURCE_FINDINGS.md](SOURCE_FINDINGS.md): пустые JSON, время, контекст, геометрия и округление.',
                  '- [RESOURCE_BOUNDARIES.md](RESOURCE_BOUNDARIES.md): отсутствующие определения и динамические вызовы.',
                  '- [COMMAND_BRIDGES.md](COMMAND_BRIDGES.md): подстановка команд плагинов и граница внешней реализации.',
                  '- [maps_catalog.md](maps_catalog.md): каждый NBT и сопоставление с placement-командами.', '',
                  'Машинные индексы и coverage находятся в `inventory/` и `coverage/`. '
                  'Инструменты в `tools/` позволяют повторить инвентаризацию и проверку ссылок; '
                  'они не запускают игровую механику и не изменяют datapacks.', ''])
    (NOTES / 'INDEX.md').write_text('\n'.join(lines), encoding='utf-8')
    print(json.dumps({'jobs': len(jobs), 'documents_present': sum(bool(job.get('documents') and any((ROOT / d).exists() for d in job['documents'])) for job in jobs.values())}))


if __name__ == '__main__':
    main()
