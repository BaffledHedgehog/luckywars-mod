"""Extract exact loot-table item definitions as evidence, not behavior summaries."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NOTES = ROOT / 'agent_notes'


def inspect(node, inherited_functions, inherited_conditions, pointer, results):
    if not isinstance(node, dict):
        return
    # Entry functions apply before enclosing pool/table functions.
    functions = node.get('functions', []) + inherited_functions
    conditions = inherited_conditions + node.get('conditions', [])
    entry_type = node.get('type', '').removeprefix('minecraft:')
    if entry_type in {'item', 'loot_table'}:
        results.append({'pointer': pointer, 'type': entry_type, 'name': node.get('name', node.get('value')),
                        'weight': node.get('weight', 1), 'quality': node.get('quality', 0),
                        'functions': functions, 'conditions': conditions})
    for key in ('pools', 'entries', 'children'):
        for index, child in enumerate(node.get(key, [])):
            inspect(child, functions, conditions, f'{pointer}/{key}/{index}', results)


def main():
    files = json.loads((NOTES / 'inventory/files.json').read_text(encoding='utf-8'))
    index, errors, json_count = [], [], 0
    for row in files:
        if row['suffix'] != '.json':
            continue
        json_count += 1
        source = ROOT / row['source']
        try:
            content = json.loads(source.read_text(encoding='utf-8-sig'))
        except (ValueError, OSError) as exc:
            errors.append({'source': row['source'], 'error': str(exc)})
            continue
        if '/loot_table/' not in row['source']:
            continue
        entries = []
        inspect(content, [], [], '', entries)
        index.append({'source': row['source'], 'id': row['resource_id'], 'pack': row['pack'],
                      'context_type': content.get('type'), 'pools': len(content.get('pools', [])),
                      'entries': entries})
    (NOTES / 'inventory/loot_item_index.json').write_text(json.dumps(index, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    (NOTES / 'inventory/json_parse_report.json').write_text(json.dumps({'json_sources': json_count, 'errors': errors}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'json_sources': json_count, 'parse_errors': len(errors), 'loot_tables': len(index),
                      'item_or_redirect_entries': sum(len(row['entries']) for row in index)}, ensure_ascii=False))


if __name__ == '__main__':
    main()
