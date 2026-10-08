"""Inventory the frozen datapack sources; no third-party dependencies."""
from __future__ import annotations

import hashlib
import json
import re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'agent_notes' / 'inventory'
PACKS = ('LBC', 'maps', 'swarogskywars')
CALL = re.compile(r'\bfunction\s+(#?[a-z0-9_.-]+:[a-z0-9_./-]+)')


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    inventory, functions = [], []
    summary = {}
    for pack in PACKS:
        base = ROOT / 'world' / 'datapacksdisabled' / pack
        counts = Counter()
        for file in sorted(base.rglob('*')):
            if not file.is_file():
                continue
            rel = file.relative_to(base).as_posix()
            parts = file.relative_to(base).parts
            category = '/'.join(parts[:3]) if parts[0] == 'data' else 'metadata'
            group = '/'.join(parts[:4]) if len(parts) > 4 else category
            row = {'source': file.relative_to(ROOT).as_posix(), 'pack': pack,
                   'relative': rel, 'category': category, 'group': group,
                   'suffix': file.suffix, 'bytes': file.stat().st_size,
                   'sha256': hashlib.sha256(file.read_bytes()).hexdigest()}
            if len(parts) >= 4 and parts[0] == 'data':
                row['resource_id'] = parts[1] + ':' + '/'.join(parts[3:]).removesuffix(file.suffix)
            if file.suffix == '.mcfunction':
                text = file.read_text(encoding='utf-8-sig', errors='replace')
                lines = [line.strip() for line in text.splitlines()
                         if line.strip() and not line.lstrip().startswith('#')]
                row['command_lines'] = len(lines)
                row['calls'] = sorted(set(CALL.findall('\n'.join(lines))))
                functions.append({'source': row['source'], 'id': row['resource_id'],
                                  'calls': row['calls'], 'command_lines': len(lines)})
            inventory.append(row)
            counts[group] += 1
        summary[pack] = {'files': sum(counts.values()), 'groups': dict(sorted(counts.items()))}
    for name, data in [('files.json', inventory), ('functions.json', functions), ('summary.json', summary)]:
        (OUT / name).write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({pack: info['files'] for pack, info in summary.items()}, ensure_ascii=False))
    for pack in ('LBC', 'swarogskywars'):
        print(pack)
        for group, count in summary[pack]['groups'].items():
            print(f'  {count:5} {group}')


if __name__ == '__main__':
    main()
