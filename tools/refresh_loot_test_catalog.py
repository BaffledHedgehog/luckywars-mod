"""Expose each logical gameplay item once; preserve internal source tables and aggregate pools.

The existing all-item manifest owns identity/profile/TMT policy. New singleton
commands use the same native uniform entry with a stable slot ID, never a second
item factory. --check is read-only and rejects missing/stale generated outputs.
"""
from __future__ import annotations
import argparse
from collections import defaultdict
import hashlib
import json
from pathlib import Path
import re
from build_uniform_item_pools import logical_identity
from canonicalize_lbc_loot import packed

ROOT = Path(__file__).resolve().parents[1]
TABLES = ROOT / 'src/main/resources/data/lbc/loot_table'
CATALOG = ROOT / 'src/main/resources/skywars2/loot_test_catalog.json'
MANIFEST = ROOT / 'src/main/resources/skywars2/all_item_pools.json'


def encoded(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode('utf-8')


def build():
    old = json.loads(CATALOG.read_text(encoding='utf-8-sig'))
    manifest = json.loads(MANIFEST.read_text(encoding='utf-8-sig'))
    preferred = set(old.get('preferred_tables', old['tables']))
    preferred.update(manifest['typedLegacyTables'])
    preferred.update(manifest['functionalLegacyTables'])
    preferred.update('lbc:' + name for name in manifest['domains'])
    custom = {row['id']: row for row in manifest['custom']}
    represented = {node.get('name') for node in manifest['entryNodes'].values()
                   if node.get('type') == 'minecraft:item'}
    for item in manifest['allowedDefaultNative']:
        if item not in represented:
            custom['native/' + item] = {'identity': {'carrier': item}, 'sources': []}
    custom['factory/tmt'] = {'identity': {'carrier': 'factory/tmt'}, 'sources': []}
    by_identity = {packed(row['identity']): key for key, row in custom.items()}
    by_source = defaultdict(set)
    for key, row in custom.items():
        for table in row['sources']:
            by_source[table].add(key)
    cache, active = {}, set()

    def closure(table):
        if table in cache:
            return cache[table]
        if table in active:
            raise ValueError('Cyclic public loot source: ' + table)
        if not table.startswith('lbc:'):
            return {'external/' + table}
        path = TABLES / (table.removeprefix('lbc:') + '.json')
        if not path.is_file():
            raise ValueError('Missing public loot candidate: ' + table)
        active.add(table)
        result = set(by_source[table])

        def visit(value):
            if isinstance(value, list):
                for child in value:
                    visit(child)
            elif isinstance(value, dict):
                kind = value.get('type', '').removeprefix('minecraft:')
                if kind == 'item':
                    item = value['name']
                    identity = logical_identity(value)
                    key = by_identity.get(packed(identity))
                    if key:
                        result.add(key)
                    elif item in manifest['tmtNative']:
                        result.add('factory/tmt')
                    elif identity['carrier'].startswith('minecraft:'):
                        result.add('vanilla/' + identity['carrier'])
                    return
                if kind == 'loot_table' and isinstance(value.get('value'), str):
                    result.update(closure(value['value']))
                    return
                if kind == 'skywars2:uniform_item':
                    # These remain distinct aggregate lotteries, including typed
                    # and count64/99 variants. Never flatten their distribution.
                    result.update(('lottery/' + table + '/0', 'lottery/' + table + '/1'))
                    return
                for key in ('pools', 'entries', 'children', 'value'):
                    if key in value:
                        visit(value[key])
        visit(json.loads(path.read_text(encoding='utf-8-sig')))
        active.remove(table)
        cache[table] = result
        return result

    guaranteed_cache = {}

    def guaranteed(table):
        """A public item command must not be an actor's conditional loot drop.

        Preserve those original probabilities privately; fall back to the shared
        stable-slot factory when no unconditional positive source root exists.
        """
        if table in guaranteed_cache:
            return guaranteed_cache[table]
        guaranteed_cache[table] = False
        if not table.startswith('lbc:'):
            return False
        document = json.loads((TABLES / (table[4:] + '.json')).read_text(encoding='utf-8-sig'))

        def positive(value):
            if isinstance(value, (int, float)):
                return value > 0
            if isinstance(value, dict):
                kind = value.get('type', '').removeprefix('minecraft:')
                if kind == 'uniform':
                    return positive(value.get('min', 0))
                if kind == 'constant':
                    return positive(value.get('value', 0))
            return False

        def owner_safe(value):
            if value.get('condition'):
                return False
            functions = value.get('modifier', [])
            if not isinstance(functions, list):
                functions = [functions]
            for function in functions:
                if not isinstance(function, dict):
                    return False
                kind = function.get('type', '').removeprefix('minecraft:')
                if kind == 'set_count' and not positive(function.get('count')):
                    return False
                if kind in {'apply_bonus', 'filtered', 'sequence', 'reference'}:
                    return False
                if kind == 'limit_count':
                    limit = function.get('limit')
                    if isinstance(limit, (int, float)) and not positive(limit):
                        return False
                    if isinstance(limit, dict) and 'max' in limit and not positive(limit['max']):
                        return False
            return True

        def entry_safe(value):
            if not isinstance(value, dict) or not owner_safe(value):
                return False
            kind = value.get('type', '').removeprefix('minecraft:')
            if kind == 'item':
                return True
            if kind == 'loot_table' and isinstance(value.get('value'), str):
                return guaranteed(value['value'])
            if kind in {'group', 'alternatives'}:
                return any(entry_safe(child) for child in value.get('children', []))
            return False

        result = owner_safe(document) and any(owner_safe(pool) and positive(pool.get('rolls', 1))
                and bool(pool.get('entries')) and all(entry_safe(entry) for entry in pool['entries'])
                for pool in document.get('pools', []))
        guaranteed_cache[table] = result
        return result

    ports = {'lbc:ported/' + p.relative_to(TABLES / 'ported').as_posix()[:-5]
             for p in (TABLES / 'ported').rglob('*.json')}
    candidates = sorted(preferred | ports)
    choices = defaultdict(list)
    aggregates = []
    for table in candidates:
        members = closure(table)
        if len(members) == 1:
            member = next(iter(members))
            if member in custom:
                choices[member].append(table)
        elif table in preferred and len(members) > 1:
            aggregates.append(table)

    def label(row):
        name = row['identity'].get('name')
        if isinstance(name, list):
            name = next((part for part in name if isinstance(part, dict) and 'translate' in part), None)
        value = name.get('translate') if isinstance(name, dict) else None
        value = value or row['identity'].get('minecraft:item_model') or row['identity']['carrier']
        value = value.removeprefix('lbc:').removeprefix('skywars2:').removeprefix('minecraft:')
        return re.sub('[^a-z0-9_.-]+', '_', value.lower()).strip('_') or 'item'

    generated, records, used, aliases = {}, [], set(aggregates), {}
    labels = defaultdict(list)
    for key, row in custom.items():
        labels[label(row)].append(key)
    for key, row in sorted(custom.items()):
        options = sorted((table for table in choices[key] if guaranteed(table)),
                         key=lambda table: (table not in preferred, len(table), table))
        # The native factory is one public item. The requested kit projectile is
        # discoverable by its original govno_raskid name rather than its carrier.
        force_generated = key == 'factory/tmt' or row['identity']['carrier'] == 'skywars2:bouncing_poop'
        if options and not force_generated:
            table = options[0]
            is_generated = False
        else:
            stem = 'tmt' if key == 'factory/tmt' else label(row)
            if len(labels[stem]) > 1:
                stem += '__' + key.rsplit('/', 1)[-1][:10]
            table = 'lbc:items/' + stem
            if table in used:
                raise ValueError('Conflicting public item label: ' + table)
            generated[table] = {'pools': [{'rolls': 1, 'entries': [{
                'type': 'skywars2:uniform_item', 'domain': 'custom', 'slot': key,
                'modifier': [{'type': 'minecraft:set_count', 'count': 1}]}]}]}
            is_generated = True
        if table in used:
            raise ValueError('Two logical items share one public table: ' + table)
        used.add(table)
        records.append({'table': table, 'slot': key, 'carrier': row['identity']['carrier'], 'generated': is_generated})
        for alias in choices[key]:
            if alias != table:
                aliases[alias] = table
    for table in aggregates:
        if table in aliases:
            raise ValueError('Aggregate and singleton alias conflict: ' + table)
    catalog = {'managed_namespaces': old['managed_namespaces'], 'id_prefixes': [],
               'tables': sorted(used), 'generated_roots': [], 'preferred_tables': sorted(preferred),
               'aggregate_tables': sorted(aggregates), 'items': records, 'hidden_aliases': dict(sorted(aliases.items())),
               'packaged_tables': sorted(ports | preferred | used | set(aliases) | set(cache)),
               'all_item_manifest_sha256': hashlib.sha256(MANIFEST.read_bytes()).hexdigest()}
    return old, catalog, generated


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    old, catalog, generated = build()
    outputs = {CATALOG: encoded(catalog)}
    outputs.update({TABLES / (name.removeprefix('lbc:') + '.json'): encoded(body)
                    for name, body in generated.items()})
    previous = {row['table'] for row in old.get('items', []) if row['generated']}
    stale = previous - generated.keys()
    for name in stale:
        assert name.startswith('lbc:items/')
    mismatches = [str(path.relative_to(ROOT)) for path, raw in outputs.items()
                  if not path.is_file() or path.read_bytes() != raw]
    if args.check:
        assert not stale and not mismatches, f'Stale public catalog: {len(stale)} removals, {len(mismatches)} writes needed; run refresh_loot_test_catalog.py'
    else:
        for name in stale:
            path = (TABLES / (name.removeprefix('lbc:') + '.json')).resolve()
            path.relative_to((TABLES / 'items').resolve())
            path.unlink()
        for path, raw in outputs.items():
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(raw)
    print(f'Public logical items: {len(catalog["items"])}; aggregates: {len(catalog["aggregate_tables"])}; generated singleton tables: {len(generated)}; hidden aliases: {len(catalog["hidden_aliases"])}; check={args.check}')


if __name__ == '__main__':
    main()
