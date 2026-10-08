"""Document command macros as source contracts, without claiming plugin effects."""
from pathlib import Path
from collections import defaultdict
import json
import re
from audit_references import tokens, command_references

ROOT = Path(__file__).resolve().parents[2]
NOTES = ROOT / 'agent_notes'


def main():
    lines = []
    for path in (ROOT / 'world/datapacksdisabled').rglob('*.mcfunction'):
        for number, raw in enumerate(path.read_text(encoding='utf-8-sig').splitlines(), 1):
            text = raw.strip()
            if text and not text.startswith('#'):
                lines.append((path.relative_to(ROOT).as_posix(), number, text))
    callers_by_id = defaultdict(list)
    assignments_by_field = defaultdict(list)
    parsed = []
    for source, number, command in lines:
        args = tokens(command)
        parsed.append((source, number, command, args))
        for kind, fid in command_references(command):
            if kind == 'function':
                callers_by_id[fid].append(dict(source=source, line=number, command=command))
        for i in range(max(0, len(args)-7)):
            if args[i:i+3] == ['data', 'modify', 'storage'] and args[i+5:i+7] == ['set', 'value']:
                assignments_by_field[args[i+4]].append(dict(source=source, line=number, storage=args[i+3], field=args[i+4], value=' '.join(args[i+7:]), command=command))
    bridges = []
    for source, number, command, args in parsed:
        roots = [args[0]] + [args[i+1] for i, value in enumerate(args[:-1]) if value == 'run']
        selected = [root for root in roots if '$(' in root or root == 'buildmanager']
        if not selected:
            continue
        suffix = source.split('/data/', 1)[1]
        namespace, _, tail = suffix.split('/', 2)
        fid = namespace + ':' + tail.removesuffix('.mcfunction')
        parameters = sorted(set(re.findall(r'\$\(([^)]+)\)', ' '.join(selected))))
        callers = callers_by_id[fid]
        assignments = [a for parameter in parameters for a in assignments_by_field[parameter]]
        bridges.append(dict(source=source, line=number, function=fid, command=command,
                            roots=selected, parameters=parameters, callers=callers, assignments=assignments))
    (NOTES/'inventory/command_bridges.json').write_text(json.dumps(bridges, ensure_ascii=False, indent=2)+'\n', encoding='utf8')
    out = ['# Команды, передаваемые за пределы функций datapack', '',
           'Некоторые функции подставляют в начало команды строку из macro storage. Ниже перечислены все такие строки и отдельный буквальный вызов `buildmanager` в трёх исследуемых пакетах. Это входы в серверные команды, а не определения их реализации.', '',
           'Например, `mapclear includespecial`, `motion`, `clonetonexus`, `rotatemap`, `regen`, `badapple`, `antigravity` и `bedwars` не объясняются телом macro helper: он только передаёт команду. Их фактический эффект нельзя подтвердить одним datapack. Нужен соответствующий обработчик сервера. `transfer` и `kick` также используются через подстановку; форма вызова сохранена буквально.', '',
           'Список `set value` показывает возможные присваивания поля во всех функциях. Совпадение имени поля не доказывает, что именно это значение попадёт в конкретный вызов: проверяйте storage, caller и порядок команд. Другие способы записи, inline-аргументы и состояние, оставшееся от прошлых вызовов, автоматически здесь не вычисляются.', '',
           '[Полный контракт с callers и строками присваивания](inventory/command_bridges.json). Исходники не изменены.', '']
    for row in bridges:
        out += ['## `'+row['function']+'`', '',
                '`'+row['command']+'`', '',
                f"Источник: [{Path(row['source']).name}](../{row['source']}) — строка {row['line']}."]
        values = list(dict.fromkeys(a['field']+' = '+a['value'] for a in row['assignments']))
        if values:
            out += ['Найденные буквальные присваивания: '+ '; '.join('`'+v+'`' for v in values)+'.']
        out += ['Прямых function callers: '+str(len(row['callers']))+'.', '']
    (NOTES/'COMMAND_BRIDGES.md').write_text('\n'.join(out)+'\n', encoding='utf8')
    print(json.dumps({'bridges': len(bridges), 'literal_assignments': sum(len(x['assignments']) for x in bridges)}))


if __name__ == '__main__':
    main()
