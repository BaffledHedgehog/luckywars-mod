"""Conservatively stage canonical item tables; never edits the source tree.

Run with --self-test for synthetic checks, or without arguments to stage resources.
The proof expands *only newly introduced* singleton tables, then compares every
original JSON tree exactly (including list order and absent/default fields).
Additional singleton selections consume RNG: this is distribution preservation,
not a claim that fixed seeds continue to yield the same result.
"""
from __future__ import annotations

import argparse
from collections import defaultdict
from copy import deepcopy
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
CANON = 'lbc:item_defs/'
KNOWN = {'set_components', 'set_count', 'set_name', 'set_lore',
         'set_custom_data', 'set_enchantments', 'set_attributes',
         'enchant_with_levels', 'toggle_tooltips', 'set_damage',
         'explosion_decay', 'copy_components', 'fill_player_head'}
# `condition` is the active native26.3 gate. The legacy plural/`functions`
# fields stay verbatim on the caller and remain ignored by both entry codecs.
SELECTION = {'weight', 'quality', 'conditions', 'condition', 'functions'}


def packed(value):
    return json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(',', ':'))


def digest(value):
    return hashlib.sha256(packed(value).encode()).hexdigest()[:12]


def visit(value, path=()):
    if isinstance(value, dict):
        yield value, path
        for key, child in list(value.items()):
            yield from visit(child, path + (key,))
    elif isinstance(value, list):
        for index, child in enumerate(value):
            yield from visit(child, path + (index,))


def modifiers(entry, legacy=False):
    value = entry.get('modifier', entry.get('functions', []) if legacy else [])
    return value if isinstance(value, list) else [value]


def item_entries(value, path=()):
    """Visit entry trees, never item components or function-owned nested entries."""
    if isinstance(value, list):
        for index, child in enumerate(value):
            yield from item_entries(child, path + (index,))
    elif isinstance(value, dict):
        if value.get('type', '').removeprefix('minecraft:') == 'item':
            yield value, path
            return
        for key in ('pools', 'entries', 'children', 'value'):
            if key in value:
                yield from item_entries(value[key], path + (key,))


def eligible(entry):
    if set(entry) - ({'type', 'name', 'modifier'} | SELECTION):
        return 'unknown item entry fields'
    if not isinstance(entry.get('name'), str):
        return 'dynamic or missing item identifier'
    for modifier in modifiers(entry):
        if not isinstance(modifier, dict):
            return 'referenced or malformed modifier'
        kind = modifier.get('type', '')
        if kind.removeprefix('minecraft:') not in KNOWN:
            return 'unknown or recursive modifier: ' + str(kind)
        # No nested modifiers/tables can cross the new recursion boundary.
        for obj, _ in visit(modifier):
            if obj is not modifier and any(k in obj for k in ('modifier', 'functions')):
                return 'nested modifier payload'
            if obj.get('type', '').removeprefix('minecraft:') in {
                    'loot_table', 'set_contents', 'reference', 'filtered', 'sequence'}:
                return 'nested or referenced function/context'
    return None


def identity(entry, legacy=False):
    identity_parts = {'carrier': entry['name']}
    for modifier in modifiers(entry, legacy=legacy):
        if not isinstance(modifier, dict):
            continue
        # Conditional identity changes are retained as separate full variants.
        if modifier.get('conditions'):
            continue
        kind = modifier.get('type', modifier.get('function', '')).removeprefix('minecraft:')
        if kind == 'set_components':
            components = modifier.get('components', {})
            for key in ('minecraft:item_model', 'minecraft:custom_data', 'minecraft:custom_name'):
                if key in components:
                    identity_parts[key] = components[key]
        elif kind == 'set_custom_data':
            identity_parts['custom_data'] = modifier.get('tag')
        elif kind == 'set_name':
            identity_parts['name'] = modifier.get('name')
    return identity_parts


def label(parts):
    value = parts.get('minecraft:item_model', parts['carrier'])
    name = parts.get('name')
    if value == parts['carrier'] and isinstance(name, dict):
        value = name.get('translate', value)
    return re.sub('[^a-z0-9_/.-]+', '_', str(value).lower().replace(':', '/')).strip('/')


def common_prefix(entries):
    arrays = [modifiers(entry) for entry in entries]
    prefix = []
    for values in zip(*arrays):
        if any(value != values[0] for value in values[1:]):
            break
        prefix.append(deepcopy(values[0]))
    return prefix


def singleton(payload):
    return {'pools': [{'rolls': 1, 'entries': [payload]}]}


def standalone(document):
    """A pre-existing definition whose only reachable output is one item."""
    if not isinstance(document, dict) or set(document) - {'pools', 'type'}:
        return None
    pools = document.get('pools', [])
    if len(pools) != 1:
        return None
    pool = pools[0]
    if set(pool) - {'rolls', 'entries'} or pool.get('rolls') != 1:
        return None
    entries = pool.get('entries', [])
    if len(entries) != 1:
        return None
    entry = entries[0]
    # An ignored legacy payload or an active gate never defines an owner.
    if 'functions' in entry or 'condition' in entry:
        return None
    if entry.get('type', '').removeprefix('minecraft:') != 'item' or eligible(entry):
        return None
    if entry.get('conditions') or entry.get('quality', 0) != 0 or entry.get('weight', 1) <= 0:
        return None
    return {key: deepcopy(value) for key, value in entry.items() if key not in SELECTION}


def transform(documents):
    """Mutate copied documents; return generated tables and a review manifest."""
    groups = defaultdict(list)
    skipped = []
    existing = defaultdict(list)
    standalone_ids = set()
    for resource, document in sorted(documents.items()):
        payload = standalone(document)
        if payload is not None:
            standalone_ids.add(resource)
            existing[packed(normalize(payload))].append(resource)
    for resource, document in sorted(documents.items()):
        if resource.startswith(CANON) or resource in standalone_ids:
            continue
        for entry, path in item_entries(document):
            reason = eligible(entry)
            if reason:
                skipped.append({'table': resource, 'path': list(path), 'reason': reason,
                                'entry_keys': sorted(entry),
                                'audit_identity': identity(entry, legacy=True) if 'name' in entry else None})
            else:
                groups[packed(identity(entry))].append((resource, path, entry))
    generated = {}
    reused = {}
    definitions = []
    rewritten = 0
    aliases = []
    owners = {}
    for payload_json, candidates in sorted(existing.items()):
        owner = min(candidates, key=lambda value: (not value.startswith('lbc:ported/'), len(value), value))
        owners[owner] = candidates
        for resource in candidates:
            if resource == owner:
                continue
            entry = documents[resource]['pools'][0]['entries'][0]
            replacement = {'type': 'minecraft:loot_table', 'value': owner}
            replacement.update({key: deepcopy(entry[key]) for key in SELECTION if key in entry})
            entry.clear()
            entry.update(replacement)
            reused[owner] = singleton(json.loads(payload_json))
            rewritten += 1
            aliases.append({'table': resource, 'canonical': owner,
                            'path': ['pools', 0, 'entries', 0]})
    for identity_json, occurrences in sorted(groups.items()):
        parts = json.loads(identity_json)
        entries = [record[2] for record in occurrences]
        prefix = common_prefix(entries)
        # A carrier/count-only common prefix would leave model/name/lore
        # definitions copied in callers. Use exact variants unless every tail
        # is solely a count/enchantment/durability/output-context operation.
        tail_kinds = {'set_count', 'set_enchantments', 'enchant_with_levels',
                      'set_damage', 'explosion_decay'}
        shared_definition = (identity({'name': parts['carrier'], 'modifier': prefix}) == parts
                             and all(mod.get('type', '').removeprefix('minecraft:') in tail_kinds
                                     for entry in entries for mod in modifiers(entry)[len(prefix):]))
        # Factoring an empty prefix is safe but would leave repeated identity
        # definitions in callers. In that case use exact full variant leaves.
        variants = defaultdict(list)
        if (prefix and shared_definition) or all(not modifiers(entry) for entry in entries):
            variants['shared'].extend(occurrences)
        else:
            for record in occurrences:
                variants[packed(modifiers(record[2]))].append(record)
        for variant, records in sorted(variants.items()):
            mods = prefix if variant == 'shared' else modifiers(records[0][2])
            table_id = CANON + label(parts) + '__' + digest(parts)
            if variant != 'shared':
                table_id += '/variant_' + digest(mods)
            payload = {'type': records[0][2]['type'], 'name': records[0][2]['name']}
            if mods:
                payload['modifier'] = deepcopy(mods)
            table = singleton(payload)
            candidates = existing.get(packed(normalize(payload)), [])
            if candidates:
                table_id = min(candidates, key=lambda value: (not value.startswith('lbc:ported/'), len(value), value))
                reused[table_id] = table
            if table_id in documents and documents[table_id] != table:
                if table_id not in reused:
                    # Additive passes may discover a different exact prefix for
                    # an existing logical identity. Never replace its old leaf.
                    table_id += '/payload_' + digest(payload)
                    if table_id in documents and documents[table_id] != table:
                        raise ValueError('Canonical payload destination collision: ' + table_id)
            if table_id not in reused:
                generated[table_id] = table
            for resource, path, entry in records:
                old_mods = modifiers(entry)
                replacement = {'type': 'minecraft:loot_table', 'value': table_id}
                for key in SELECTION:
                    if key in entry:
                        replacement[key] = deepcopy(entry[key])
                tail = old_mods[len(mods):]
                if tail:
                    replacement['modifier'] = deepcopy(tail)
                entry.clear()
                entry.update(replacement)
                rewritten += 1
            definitions.append({'table': table_id, 'identity': parts,
                                'occurrences': len(records), 'prefix_modifiers': len(mods),
                                'full_variant': variant != 'shared', 'reused_existing': table_id in reused,
                                'callers': [{'table': r, 'path': list(p)} for r, p, _ in records]})
    return generated, {'item_entries_rewritten': rewritten, 'logical_identities': len(groups),
                       'canonical_tables': len(generated), 'definitions': definitions,
                       'existing_definition_aliases': aliases, 'existing_definition_owners': owners,
                       'reused_tables': len(reused), 'existing_standalone_tables': len(standalone_ids),
                       '_reused_expansion': reused, 'skipped_entries': skipped}


def expand(value, generated):
    if isinstance(value, list):
        return [expand(child, generated) for child in value]
    if not isinstance(value, dict):
        return value
    if value.get('type') == 'minecraft:loot_table' and value.get('value') in generated:
        table = generated[value['value']]
        assert set(table) == {'pools'}
        pool, = table['pools']
        assert set(pool) == {'rolls', 'entries'} and pool['rolls'] == 1
        payload, = pool['entries']
        result = deepcopy(payload)
        for key in SELECTION:
            if key in value:
                result[key] = deepcopy(value[key])
        mods = modifiers(result) + modifiers(value)
        if mods:
            result['modifier'] = mods
        return result
    return {key: expand(child, generated) for key, child in value.items()}


def normalize(value):
    """Only normalize native namespace defaults and modifier list syntax."""
    if isinstance(value, list):
        return [normalize(child) for child in value]
    if not isinstance(value, dict):
        return value
    result = {key: normalize(child) for key, child in value.items()}
    if result.get('type') in {'item', 'loot_table'}:
        result['type'] = 'minecraft:' + result['type']
    if 'modifier' in result:
        mods = result['modifier']
        mods = mods if isinstance(mods, list) else [mods]
        if mods:
            result['modifier'] = mods
        else:
            result.pop('modifier')
    return result


def resource_path(resource):
    namespace, name = resource.split(':', 1)
    return Path('data') / namespace / 'loot_table' / (name + '.json')


def self_test():
    model = {'type': 'minecraft:set_components', 'components': {'minecraft:item_model': 'lbc:test'}}
    item = {'type': 'minecraft:item', 'name': 'minecraft:stick', 'weight': 7, 'quality': -2,
            'conditions': [{'condition': 'minecraft:random_chance', 'chance': .2}],
            'modifier': [model, {'type': 'minecraft:set_count', 'count': 3}]}
    other = deepcopy(item)
    other['modifier'][-1]['count'] = 9
    nested = {'pools': [{'rolls': 25, 'entries': [item, {'type': 'minecraft:alternatives',
              'children': [other]}], 'conditions': [{'condition': 'minecraft:killed_by_player'}]}]}
    originals = {'lbc:test': nested}
    docs = deepcopy(originals)
    generated, report = transform(docs)
    assert report['canonical_tables'] == 1
    assert normalize(expand(docs['lbc:test'], generated)) == normalize(nested)
    docs.update(generated)
    generated2, report2 = transform(docs)
    assert not generated2 and report2['item_entries_rewritten'] == 0
    blocked = deepcopy(item)
    blocked['modifier'].append({'type': 'minecraft:set_contents', 'entries': [deepcopy(item)]})
    assert eligible(blocked)
    blocked_docs = {'lbc:blocked': singleton(blocked)}
    blocked_before = deepcopy(blocked_docs)
    blocked_generated, _ = transform(blocked_docs)
    assert not blocked_generated and blocked_docs == blocked_before
    opaque = deepcopy(item)
    opaque['functions'] = [{'function': 'minecraft:set_count', 'count': 99},
                           {'function': 'minecraft:set_contents', 'entries': [deepcopy(item)]}]
    opaque['condition'] = {'type': 'minecraft:random_chance', 'chance': .25}
    opaque_docs = {'lbc:opaque': singleton(opaque)}
    opaque_before = deepcopy(opaque_docs)
    opaque_generated, opaque_report = transform(opaque_docs)
    assert not opaque_report['skipped_entries']
    assert opaque_report['item_entries_rewritten'] == 1  # Never descend into ignored set_contents.
    opaque_caller = opaque_docs['lbc:opaque']['pools'][0]['entries'][0]
    assert opaque_caller['functions'] == opaque['functions'] and opaque_caller['condition'] == opaque['condition']
    assert normalize(expand(opaque_docs['lbc:opaque'], opaque_generated)) == normalize(opaque_before['lbc:opaque'])
    assert standalone(opaque_before['lbc:opaque']) is None
    opaque_docs.update(opaque_generated)
    opaque_again, opaque_rerun = transform(opaque_docs)
    assert not opaque_again and not opaque_rerun['item_entries_rewritten']
    collision_item = deepcopy(item)
    collision_parts = identity(collision_item)
    occupied = CANON + label(collision_parts) + '__' + digest(collision_parts)
    old_payload = {'type': 'minecraft:item', 'name': item['name'],
                   'modifier': [deepcopy(model), {'type': 'minecraft:set_count', 'count': 7}]}
    collision_docs = {occupied: singleton(old_payload), 'lbc:new_source': singleton(collision_item)}
    collision_before = deepcopy(collision_docs)
    collision_generated, _ = transform(collision_docs)
    assert collision_docs[occupied] == collision_before[occupied]
    assert len(collision_generated) == 1 and next(iter(collision_generated)).startswith(occupied + '/payload_')
    assert normalize(expand(collision_docs['lbc:new_source'], collision_generated)) == normalize(collision_before['lbc:new_source'])
    a, b = deepcopy(item), deepcopy(other)
    a['modifier'].reverse()
    b['modifier'].reverse()
    docs = {'lbc:variants': singleton(a), 'lbc:variants2': singleton(b)}
    before = deepcopy(docs)
    generated, report = transform(docs)
    assert report['canonical_tables'] == 2
    assert all(normalize(expand(v, generated)) == normalize(before[k]) for k, v in docs.items())
    definition = {'type': 'minecraft:generic', **singleton({'type': 'minecraft:item',
                  'name': 'minecraft:stick', 'weight': 17, 'modifier': [model]})}
    caller = {'type': 'minecraft:chest', 'modifier': [{'type': 'minecraft:explosion_decay'}],
              'pools': [{'rolls': 25, 'entries': [deepcopy(item), deepcopy(other)]}]}
    docs = {'lbc:ported/test': definition, 'lbc:caller': caller}
    before = deepcopy(docs)
    generated, report = transform(docs)
    assert not generated and report['reused_tables'] == 1
    assert docs['lbc:ported/test'] == definition
    expansion_tables = report['_reused_expansion']
    assert all(normalize(expand(v, expansion_tables)) == normalize(expand(before[k], expansion_tables))
               for k, v in docs.items())
    assert standalone(singleton(item)) is None  # Nonzero quality / conditional gate.
    conditioned = deepcopy(definition)
    conditioned['pools'][0]['conditions'] = [{'condition': 'minecraft:killed_by_player'}]
    assert standalone(conditioned) is None
    # A recursive source table remains recursive; generated leaves cannot call
    # the source or any modifier-owned nested loot table.
    cyclic = {'lbc:cycle': {'pools': [{'rolls': 1, 'entries': [deepcopy(item),
              {'type': 'minecraft:loot_table', 'value': 'lbc:cycle'}]}]}}
    cyclic_before = deepcopy(cyclic)
    generated, _ = transform(cyclic)
    assert normalize(expand(cyclic['lbc:cycle'], generated)) == normalize(cyclic_before['lbc:cycle'])
    assert all(not any(obj.get('type') == 'minecraft:loot_table' for obj, _ in visit(table))
               for table in generated.values())
    print('self-test: native/opaque selection metadata, nested alternatives, modifier order, variants, additive collision safety, recursion rejection and idempotence passed')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=ROOT / 'src/main/resources')
    parser.add_argument('--output', type=Path, default=ROOT / 'build/verification/canonical-loot')
    parser.add_argument('--self-test', action='store_true')
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return
    docs, files, invalid, source_hashes = {}, {}, [], {}
    for path in sorted((args.source / 'data').glob('*/loot_table/**/*.json')):
        relative = path.relative_to(args.source)
        namespace = relative.parts[1]
        resource = namespace + ':' + '/'.join(relative.parts[3:])[:-5]
        source_bytes = path.read_bytes()
        source_hashes[relative.as_posix()] = hashlib.sha256(source_bytes).hexdigest()
        try:
            docs[resource] = json.loads(source_bytes.decode('utf-8-sig'))
            files[resource] = path
        except (ValueError, UnicodeError) as error:
            invalid.append({'path': relative.as_posix(), 'reason': str(error)})
    original = deepcopy(docs)
    generated, report = transform(docs)
    expansion_tables = {**generated, **report.pop('_reused_expansion')}
    for resource, document in docs.items():
        if normalize(expand(document, expansion_tables)) != normalize(expand(original[resource], expansion_tables)):
            raise AssertionError('Expansion mismatch: ' + resource)
    changed = {resource: doc for resource, doc in docs.items() if doc != original[resource]}
    staged_documents = {**changed, **generated}
    stage_root = (args.output / 'staged').resolve()
    wanted = {(stage_root / resource_path(resource)).resolve() for resource in staged_documents}
    # Delete only obsolete JSON artifacts inside this explicitly resolved stage.
    for old_path in stage_root.rglob('*.json'):
        resolved = old_path.resolve()
        if not resolved.is_relative_to(stage_root):
            raise ValueError('Staged artifact escaped output root')
        if resolved not in wanted:
            old_path.unlink()
    for resource, document in sorted(staged_documents.items()):
        path = args.output / 'staged' / resource_path(resource)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(document, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    replay = deepcopy(docs)
    replay.update(generated)
    regenerated, rerun = transform(replay)
    assert not regenerated and not rerun['item_entries_rewritten'], 'Non-idempotent transformation'
    report.update({'source_tables': len(original), 'changed_tables': len(changed),
                   'invalid_json_preserved': invalid, 'expansion_checks_passed': len(original),
                   'idempotence_passed': True,
                   'source_sha256': {resource_path(k).as_posix(): source_hashes[resource_path(k).as_posix()]
                                     for k in sorted(changed)},
                   'audited_source_sha256': source_hashes,
                   'staged_files': sorted(resource_path(resource).as_posix() for resource in staged_documents),
                   'source_table_references': [
                       {'table': resource, 'path': list(path), 'target': obj.get('value', obj.get('name'))}
                       for resource, document in sorted(original.items()) for obj, path in visit(document)
                       if obj.get('type', '').removeprefix('minecraft:') == 'loot_table'],
                   'preservation': [
                       'Caller weights, qualities, conditions, order, pool rolls, bonus rolls and root fields remain unchanged.',
                       'New singleton pools have exactly one unconditional entry, rolls=1, default weight=1 and quality=0.',
                       'Reused singleton tables have no root/pool modifiers, gates, bonus rolls or random_sequence; entry quality=0 and weight>0.',
                       'Every function keeps its exact order. Prefix functions execute inside the leaf using the same LootContext parameters; tails remain on the caller.',
                       'Only allowlisted native modifiers move. Legacy functions stay verbatim on callers and remain ignored; active singular condition stays on the caller. No extraction descends into ignored legacy function payloads.',
                       'New leaves never call tables or nested loot; reused leaves contain only one nonrecursive item. Existing cycles and recursion guard behavior remain at their existing references.',
                       'Native explosion_decay and copy_components read unchanged context parameters; no context is constructed or replaced by an entry reference.',
                       'The expansion checker compares every original table, normalizing only item/table type namespace aliases and modifier list syntax.'],
                   'rng_note': 'Extra singleton draws preserve mathematical output distribution; fixed seed outputs may change.'})
    args.output.mkdir(parents=True, exist_ok=True)
    (args.output / 'manifest.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    summary = {key: report[key] for key in ('source_tables', 'changed_tables', 'logical_identities',
               'canonical_tables', 'reused_tables', 'existing_standalone_tables', 'item_entries_rewritten', 'expansion_checks_passed', 'idempotence_passed')}
    summary['skipped_entries'] = len(report['skipped_entries'])
    summary['invalid_json_preserved'] = invalid
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
