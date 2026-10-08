"""Registry support: membership, vanilla overrides, world definitions, dialogs."""
import json
import re
from collections import defaultdict
from pathlib import Path

from document_advancements import code
from document_loot import describe_entry, function_description, provider

ROOT = Path(__file__).resolve().parents[2]
NOTES = ROOT / 'agent_notes'
DAMAGE_TAGS = {
    'always_hurts_ender_dragons': 'указывает урон, который может повредить Ender Dragon независимо от обычного ограничения источника',
    'always_most_significant_fall': 'помечает урон, приоритетный для выбора варианта сообщения о падении',
    'always_triggers_silverfish': 'помечает урон, вызывающий реакцию и пробуждение silverfish',
    'avoids_guardian_thorns': 'помечает урон, который не вызывает ответные шипы guardian',
    'burns_armor_stands': 'помечает урон, сжигающий armor stand',
    'bypasses_armor': 'помечает урон, обходящий расчёт защиты брони',
    'bypasses_blocking': 'помечает урон, обходящий блокирование',
    'bypasses_cooldown': 'помечает урон, обходящий промежуток неуязвимости между попаданиями',
    'bypasses_effects': 'помечает урон, обходящий защитные эффекты',
    'bypasses_enchantments': 'помечает урон, обходящий защиту зачарований',
    'bypasses_invulnerability': 'помечает урон, обходящий флаг неуязвимости сущности',
    'bypasses_resistance': 'помечает урон, обходящий Resistance',
    'damages_helmet': 'помечает урон, учитывающий защиту шлема и повреждающий его',
    'ignites_armor_stands': 'помечает урон, поджигающий armor stand',
    'is_drowning': 'относит урон к утоплению', 'is_explosion': 'относит урон к взрывам',
    'is_fall': 'относит урон к падению', 'is_fire': 'относит урон к огню',
    'is_freezing': 'относит урон к замерзанию', 'is_lightning': 'относит урон к молнии',
    'is_projectile': 'относит урон к снарядам',
    'no_anger': 'помечает урон, который не запускает обычную агрессию пострадавшего',
    'no_impact': 'помечает урон без обычной реакции попадания',
    'no_knockback': 'отключает отбрасывание, создаваемое самим получением урона; отдельно рассчитанное отбрасывание взрыва этим не запрещается',
    'witch_resistant_to': 'указывает типы урона, к которым ведьма имеет особую устойчивость',
    'wither_immune_to': 'указывает типы урона, к которым Wither имеет особую невосприимчивость',
}


def main():
    rows = json.loads((NOTES / 'inventory/files.json').read_text(encoding='utf-8'))
    jobs = json.loads((NOTES / 'inventory/research_jobs.json').read_text(encoding='utf-8'))
    refs = defaultdict(set)
    for row in rows:
        if row['suffix'] not in ('.mcfunction', '.json'):
            continue
        text = (ROOT / row['source']).read_text(encoding='utf-8-sig')
        if row['suffix'] == '.mcfunction':
            text = '\n'.join(l for l in text.splitlines() if not l.lstrip().startswith('#'))
        for rid in set(re.findall(r'(?<![\w./\-])[\w.\-]+:[\w./\-]+', text)):
            refs[rid].add(row['source'])
    doc = 'agent_notes/lbc_registry_support.md'
    lines = ['# Теги, миры и глобальные настройки LBC', '',
             'Эти ресурсы задают состав групп, типы урона, измерения, структуры и оформление. '
             'Они загружаются вместе с datapack; файл в namespace `minecraft` может менять ванильные данные. '
             'Параметры представлены полностью, а ссылки показывают буквальное использование ID в исследуемых JSON/функциях. '
             'Финальный результат также зависит от ванильных данных и других активных пакетов.', '',
             'Tag с `replace:false` или без `replace` добавляет values к тегу нижележащего пакета; '
             '`replace:true` заменяет его состав. `#id` включает другой тег, а объект с `required:false` '
             'делает конкретную ссылку необязательной. Список в этой справке — вклад исследуемого JSON, '
             'а не вычисленный состав всех одновременно загруженных пакетов.', '',
             'Про смысл `no_knockback` и отдельное отбрасывание взрыва: '
             '[официальные изменения Minecraft 1.20.2](https://feedback.minecraft.net/hc/en-us/articles/19703470383757-Minecraft-Java-Edition-1-20-2). '
             'Типы урона задаются registry, а их дополнительные свойства определяются тегами: '
             '[официальное описание 23w06a](https://feedback.minecraft.net/hc/en-us/articles/12944917768589-Minecraft-Java-Edition-Snapshot-23w06a).', '']
    coverage, unknown_files = [], []
    unknown = {'entries': set(), 'functions': set(), 'components': set()}
    for source in jobs['lbc_registry_support']['sources']:
        tail = source.split('/LBC/', 1)[-1]
        heading = code(tail)
        lines.extend(['## ' + heading, '', 'Источник: [файл](../' + source + ').'])
        status = 'documented'
        rid = None
        raw = (ROOT / source).read_text(encoding='utf-8-sig')
        if tail == 'update.txt':
            lines.append('Исторический changelog разработки с датами, добавлениями и заявленными исправлениями. Не исполняется сервером и не определяет нынешние параметры предметов; для текущего поведения используются команды и JSON.')
            status = 'non_runtime'
        else:
            value = json.loads(raw)
            if tail == 'pack.mcmeta':
                lines.append('Метаданные datapack: минимальный и максимальный формат ' + code(value['pack'].get('min_format')) + '/' + code(value['pack'].get('max_format')) + '; описание ' + code(value['pack'].get('description')) + '. Наличие JSON с этим диапазоном не доказывает совместимость команд с другой версией Minecraft. Полные поля: ' + code(value) + '.')
            elif tail in ('.vscode/settings.json', 'datapack.config.json', 'spyglass.json'):
                desc = {'spyglass.json': 'Настройка редактора/анализатора Spyglass: gameVersion=1.21.11 и permissionLevel=4; сама версию сервера не меняет.',
                        'datapack.config.json': 'Настройка инструмента разработки: отключено правило scoreboard-fake-player-missing-hash, задан шаблон пути/имени для executeGroup.',
                        '.vscode/settings.json': 'Локальная настройка VS Code; не является ресурсом Minecraft.'}[tail]
                lines.append(desc + ' Все параметры: ' + code(value) + '.')
                status = 'non_runtime'
            else:
                parts = tail.split('/')
                namespace = parts[1]
                if parts[2] == 'tags':
                    kind = parts[3]
                    rid = namespace + ':' + '/'.join(parts[4:])[:-5]
                    lines.append('Тег ' + code(kind + ' #' + rid) + ': ' + ('заменяет состав нижележащего тега' if value.get('replace', False) else 'добавляет значения к нижележащему тегу') + '. Состав JSON: ' + code(value.get('values', [])) + '.')
                    if kind == 'damage_type' and rid.split(':')[-1] in DAMAGE_TAGS:
                        lines.append('Назначение: ' + DAMAGE_TAGS[rid.split(':')[-1]] + '.')
                    elif kind == 'function':
                        lines.append('Специальный тег Minecraft: ' + ('при загрузке/reload вызывает перечисленные функции' if rid == 'minecraft:load' else 'каждый игровой тик вызывает перечисленные функции') + '. Их фактические dispatcher описаны в `lbc_runtime.md`.')
                    else:
                        lines.append('Сам по себе не изменяет мир: команды и predicates используют эту группу для выбора блоков, предметов, сущностей или измерений. Ссылки на использование приведены ниже.')
                    extra = {k: v for k, v in value.items() if k not in ('replace', 'values')}
                    if extra:
                        lines.append('Дополнительные поля тега: ' + code(extra) + '.')
                else:
                    kind = parts[2]
                    path_start = 4 if kind == 'worldgen' else 3
                    subtype = parts[3] if kind == 'worldgen' else kind
                    rid = namespace + ':' + '/'.join(parts[path_start:])[:-5]
                    if kind == 'damage_type':
                        scaling = {'never': 'урон не масштабируется сложностью', 'always': 'урон масштабируется сложностью', 'when_caused_by_living_non_player': 'урон масштабируется сложностью, когда его вызывает живая сущность, кроме игрока'}.get(value.get('scaling'), code(value.get('scaling')))
                        lines.append('Тип урона ' + code(rid) + ': ' + scaling + '; exhaustion ' + code(value.get('exhaustion')) + ' (истощение игрока при получении урона), message_id ' + code(value.get('message_id')) + ' (ключ сообщения о смерти). Полные параметры: ' + code(value) + '.')
                        lines.append('Этот JSON не задаёт величину каждого попадания. Её задаёт источник/команда damage; обход брони, иммунитет, отбрасывание и другие свойства дополнительно определяются damage_type tags.')
                    elif kind == 'painting_variant':
                        lines.append('Вариант картины ' + code(rid) + ': ширина ' + code(value.get('width')) + ', высота ' + code(value.get('height')) + ' блоков; изображение и все остальные поля ' + code(value) + '. Ресурс определяет доступный вариант и внешний вид, а не игровую способность.')
                    elif kind == 'dimension':
                        settings = value.get('generator', {}).get('settings', {})
                        layers = settings.get('layers', [])
                        layer_text = 'Основа пустая (air высотой 0).' if all(layer.get('height', 0) == 0 for layer in layers) else 'Основа: ' + '; '.join(str(layer['height']) + ' слоёв ' + layer['block'] for layer in layers) + '.'
                        lines.append('Измерение ' + code(rid) + ': тип ' + code(value.get('type')) + '. ' + layer_text + ' Постройки/площадки добавляются командами LBC; телепорт игрока самим JSON не выполняется. Все параметры генератора: ' + code(value.get('generator')) + '.')
                    elif kind == 'dimension_type':
                        lines.append('Правила измерения ' + code(rid) + ': диапазон Y от ' + str(value.get('min_y')) + ' включительно до ' + str(value.get('min_y', 0) + value.get('height', 0)) + ' не включительно; координатный множитель ' + code(value.get('coordinate_scale')) + '. Кровати работают=' + code(value.get('bed_works')) + ', якоря работают=' + code(value.get('respawn_anchor_works')) + ', рейды=' + code(value.get('has_raids')) + '. Полный набор света, неба, температуры и правил спавна: ' + code(value) + '.')
                    elif kind == 'enchantment':
                        lines.append('Переопределяет ванильный Multishot: добавляет 2×уровень к количеству снарядов и задаёт spread=15. На I это добавка 2 к обычному одному снаряду; max_level=1 относится к обычному получению зачарования. Активен в mainhand на #minecraft:enchantable/crossbow, несовместим с #minecraft:exclusive_set/crossbow. Все стоимость/вес/эффекты: ' + code(value) + '.')
                    elif kind == 'dialog':
                        if tail.endswith('build_maker.json'):
                            lines.append('Открываемый вручную диалог управления сборкой: одна кнопка показывает `stats:stats build_1.name`, основное действие выполняет `trigger build_gui_pointer set 101`; во вложенном hover-тексте записана команда `set 1`. Четыре колонки, закрывается после действия, Escape разрешён, игра не паузится. Полное описание кнопки и выхода: ' + code(value) + '.')
                        else:
                            lines.append('Диалог сохранения сборки: текстовое поле `name`, кнопка save отправляет событие `dynamic/custom` с id `1` и пустым additions; это не команда, сохраняющая поле в storage. Выход выполняет `trigger build_gui_pointer set 201`; Escape разрешён, после действия закрывается, pause=false. Все поля: ' + code(value) + '.')
                        lines.append('Создание objective build_gui_pointer, enable и вызов диспетчера в load/players_tick закомментированы; в исследуемых datapacks нет найденного вызова открытия этого диалога и обработчика dynamic/custom id=1. Поэтому полноценное сохранение сборки этими JSON не доказано.')
                    elif kind == 'loot_table':
                        lines.append('Переопределение ванильной loot table. Контекст: ' + code(value.get('type', 'не указан')) + '.')
                        for i, pool in enumerate(value.get('pools', []), 1):
                            lines.append('Пул ' + str(i) + ': выборов ' + provider(pool.get('rolls', 1)) + '; параметры/условия ' + code({k: v for k, v in pool.items() if k not in ('entries', 'functions')}) + '.')
                            lines.append('')
                            for k, entry in enumerate(pool.get('entries', []), 1):
                                describe_entry(entry, str(k), lines, unknown)
                            for func in pool.get('functions', []):
                                lines.append('Изменение пула: ' + function_description(func, unknown) + '.')
                        for func in value.get('functions', []):
                            lines.append('Изменение таблицы: ' + function_description(func, unknown) + '.')
                        if not value.get('pools'):
                            lines.append('Пулов нет: эта таблица не создаёт предметы.')
                        extra = {k: v for k, v in value.items() if k not in ('type', 'pools', 'functions')}
                        if extra:
                            lines.append('Остальные поля: ' + code(extra) + '.')
                    elif subtype == 'biome':
                        lines.append('Биом ' + code(rid) + ': temperature=' + code(value.get('temperature')) + ', downfall=' + code(value.get('downfall')) + ', осадки=' + code(value.get('has_precipitation')) + '. Spawners/carvers/features пусты: собственные естественные мобы, пещеры и декорации этим JSON не добавлены. Цвета/звук и все поля: ' + code(value) + '.')
                    elif subtype == 'noise':
                        lines.append('Параметры шума для генераторов: ' + code(value) + '. Ресурс задаёт частоты/амплитуды, но не запускает генерацию сам по себе.')
                    elif subtype == 'structure':
                        lines.append('Jigsaw-данж: стартовый pool lbc:grimoire_dungeon/pool_start, глубина size=20, случайная стартовая высота 20..70, проекция на MOTION_BLOCKING, максимальное расстояние от центра 128. Фильтр biome=minecraft:nexus; это не телепорт в Nexus. В изученных источниках запускается через `drops/shadow_citadel` командой `place structure`; structure_set для естественной генерации здесь нет. Полные параметры: ' + code(value) + '.')
                    elif subtype == 'template_pool':
                        lines.append('Взвешенный набор фрагментов jigsaw ' + code(rid) + ': fallback=' + code(value.get('fallback')) + '. Rigid сохраняет вертикальное размещение соединённых частей без подгонки каждого блока к рельефу; processors=minecraft:empty не добавляет преобразование блоков. Состав и все веса: ' + code(value) + '.')
                        lines.append('Вес используется при выборе подходящего соединения; он не означает фиксированный процент появления комнаты во всём данже. Соответствующие NBT и маркеры описаны в maps_catalog.md.')
                    else:
                        unknown_files.append(source)
                        lines.append('Параметры ресурса: ' + code(value) + '.')
        if rid:
            related = sorted(refs[rid] - {source})
            if related:
                lines.append('Буквальные ссылки на ID: ' + ', '.join('[' + p.split('/data/', 1)[-1] + '](../' + p + ')' for p in related) + '.')
            else:
                lines.append('Буквальных ссылок на ID в исследуемых функциях/JSON не найдено; для встроенных minecraft tags/типов урона использование самим движком не требует команды datapack.')
        lines.append('')
        coverage.append({'source': source, 'document': doc, 'section': heading, 'status': status,
                         'note': 'Complete registry membership/settings and behavior described; literal references and inactive dialog boundary recorded.'})
    (ROOT / doc).write_text('\n'.join(lines) + '\n', encoding='utf-8')
    (NOTES / 'coverage/lbc_registry_support.json').write_text(json.dumps(coverage, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'sources': len(coverage), 'unknown_files': unknown_files, 'unknown': {k: sorted(v) for k, v in unknown.items()}}))


if __name__ == '__main__':
    main()
