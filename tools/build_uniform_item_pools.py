"""Build auditable logical slots for native uniform item loot; never rewrite source tables.

Uses canonicalize_lbc_loot's identity/modifier reader. Every authored item leaf is
accounted for, with reference/pool/table modifier and condition boundaries retained.
Roll multiplicity and branch weights do not create logical identity slots.
"""
from __future__ import annotations
import argparse
from collections import defaultdict
from copy import deepcopy
import hashlib
import json
from pathlib import Path
import re
import subprocess
from canonicalize_lbc_loot import identity, modifiers, packed, standalone

ROOT = Path(__file__).resolve().parents[1]
TABLES = ROOT / 'src/main/resources/data/lbc/loot_table'
OUTPUT = ROOT / 'src/main/resources/skywars2/all_item_pools.json'
EXCLUDED_NATIVE = {'skywars2:dev_tools': 'Developer UI'}
TMT_NATIVE = {'skywars2:tmt_token', 'skywars2:tmt_carrot_on_a_stick', 'skywars2:tmt_bow',
              'skywars2:tmt_snowball', 'skywars2:tmt_crossbow', 'skywars2:tmt_golden_apple'}
QUALIFIED_ACADEMY = '4aa9601b:src/main/java/me/skywars2/game/LbcAcademy.java'
ACADEMY_EXTRAS = {'skywars2:blaze_wand': ('skywars2:academy_blaze_wand', 'a'),
                  'skywars2:fireblaze_wand': ('skywars2:academy_fireblaze_wand', 's'),
                  'skywars2:psychowand': ('skywars2:academy_psychowand', 'b'),
                  'skywars2:psychoblind_wand': ('skywars2:academy_psychoblind_wand', 'ss')}
DEFAULT_NATIVE = {'skywars2:oguzok_sword': {
    'owner': 'LbcWtf.SWORD/registerSword/sword',
    'reason': 'Complete native wooden sword/model prototype intentionally constructed by sword(); no deferred dose, mode or payload is required.'},
    'skywars2:consumed_block': {
    'owner': 'LbcConsumedBlock.ITEM/LbcBlockModels.CubeItem',
    'reason': 'Real native playable block item with complete registered block/model prototype; generated-terrain use is not a service exclusion from all items.'},
    'skywars2:steampunk_rocket': {
    'owner': 'LbcSteampunkRocket.TOKEN/registerItem/Token.use',
    'reason': 'Complete native rocket prototype includes its real gadget flag, model, name and lore; native launch needs no deferred dose, mode or entity payload. The former egg transport has no authored loot leaf.'}}
TYPE_FLAGS = {name: name for name in ('accessory', 'axe', 'bow', 'hoe', 'pickaxe', 'potion', 'shield', 'shooter', 'shovel', 'staff', 'sword', 'totem', 'trident', 'arcanum', 'helmet', 'chestplate', 'leggings', 'boots')}
TYPE_FLAGS.update(spells='spellitem', summons='summoner')
ALL_TYPES = ('accessory', 'arcanum', 'axe', 'boots', 'bow', 'hoe', 'shooter', 'shovel', 'staff', 'summons', 'sword')
SUBTYPES = {'rofl': ('lbc:rofls',), 'slab': ('lbc:slabs', 'lbc:imba/cunny_slabs'), 'grimoire': ('lbc:spells_library',)}
HELPER_PRIMITIVES = {
    'lbc:item_defs/skywars2/bucks': 'lbc:imba/random_bucks',
    'lbc:item_defs/skywars2/academy_token': 'lbc:kit/academy_token',
    'lbc:item_defs/skywars2/blood_anal_boom': 'lbc:trinket/blood_anal_boom',
    'lbc:item_defs/skywars2/mana_gem': 'lbc:trinket/mana_gem'}
FUNCTIONAL = {
    '27_acessories': ('accessory_all', 'totem'), '27_bows': ('bow_all',),
    '27_grimoires': ('grimoire',), '27_instruments': ('axe_all', 'hoe_all', 'shovel', 'pickaxe', 'trident', 'shield'),
    '27_potions': ('potion',), '27_random_items': (), '27_rofls': ('rofl',),
    '27_shooters': ('shooter_all',), '27_slabs': ('slab',), '27_staffs': ('staff_all',),
    '27_summons': ('summons_all', 'arcanum_all'), '27_swords': ('sword_all',)}


def logical_identity(entry):
    parts = identity(entry)
    if ':' not in parts['carrier']:
        parts['carrier'] = 'minecraft:' + parts['carrier']
    # These dedicated native helpers are implementation primitives. Minecraft's
    # text codec decodes [component] and component identically; retain both raw
    # profiles but never add a second ticket for that serialization difference.
    if parts['carrier'] in {name.removeprefix('lbc:item_defs/').replace('/', ':', 1) for name in HELPER_PRIMITIVES}:
        name = parts.get('name')
        if isinstance(name, list) and len(name) == 1: parts['name'] = name[0]
    return parts


def root_fields(value):
    """Read only actual top-level custom-data members, never nested flag names."""
    if isinstance(value, dict):
        return value
    if not isinstance(value, str) or not value.strip().startswith('{'):
        return {}
    text = value.strip()[1:-1]; parts = []; start = depth = 0; quote = None; escaped = False
    for index, char in enumerate(text):
        if quote:
            if escaped: escaped = False
            elif char == '\\': escaped = True
            elif char == quote: quote = None
        elif char in '\"\'': quote = char
        elif char in '[{': depth += 1
        elif char in ']}': depth -= 1
        elif char == ',' and depth == 0: parts.append(text[start:index]); start = index + 1
    parts.append(text[start:]); result = {}
    for part in parts:
        match = re.fullmatch(r'\s*([a-zA-Z0-9_]+)\s*:\s*(.*?)\s*', part, re.S)
        if match: result[match[1]] = match[2]
    return result


def semantic_types(chain):
    data = {}
    for row in chain:
        kind = node_kind(row)
        if kind == 'set_custom_data': data.update(root_fields(row.get('tag')))
        elif kind == 'set_components':
            components = row.get('components', {})
            if '!minecraft:custom_data' in components: data = {}
            if 'minecraft:custom_data' in components: data = root_fields(components['minecraft:custom_data'])
    def one(value): return value is True or value == 1 or isinstance(value, str) and re.fullmatch(r'1[bBsSlL]?', value.strip()) is not None
    return {name for name, flag in TYPE_FLAGS.items() if one(data.get(flag))}


def flat_leaf(entry, chain):
    base = entry
    while node_kind(base) == 'group': base = base['children'][0]
    return {'type': 'minecraft:item', 'name': base['name'], 'modifier': chain}


def service_table(name):
    return name.startswith('lbc:gui/')


def table_references(node):
    if isinstance(node, list):
        for child in node:
            yield from table_references(child)
    elif isinstance(node, dict):
        if node_kind(node) == 'loot_table' and isinstance(node.get('value'), str):
            yield node['value']
        for key in ('pools', 'entries', 'children', 'value'):
            if key in node:
                yield from table_references(node[key])


def promoted_profiles(entry, chain, origin, qualified_source):
    """Finite prototype transformation from the qualified native Academy owner.

    Never reads the protected working-tree Academy, never executes a function.
    Full base components are retained, just as native transmuteCopy does.
    """
    base = entry
    while node_kind(base) == 'group':
        base = base['children'][0]
    recipe = ACADEMY_EXTRAS.get(base['name'])
    if recipe is None:
        return []
    for required in ('tag.putInt("upgraded", 1); tag.putInt("mode", 1);',
                     'stack.transmuteCopy(wand.upgraded)',
                     'tag.putInt("upgrade_available", 1); tag.putInt("upgradable", 0);',
                     'withLineAdded(Component.translatable("upgraded_item")'):
        if required not in qualified_source:
            raise ValueError('Qualified Academy transformation changed: ' + required)
    tags = [row.get('tag') for row in chain if node_kind(row) == 'set_custom_data']
    if len(tags) != 1 or not isinstance(tags[0], str):
        raise ValueError('Academy extra requires one full literal source custom-data prototype: ' + base['name'])
    raw = tags[0]
    if not re.fullmatch(r'\{\s*(?:[a-zA-Z0-9_]+\s*:\s*-?\d+[bBsSlL]?\s*,?\s*)*\}', raw):
        raise ValueError('Academy extra source data needs explicit richer parser: ' + raw)
    data = dict((key, int(value)) for key, value in re.findall(r'([a-zA-Z0-9_]+)\s*:\s*(-?\d+)', raw))
    data.update(upgradable=0, upgrade_available=1, upgraded=1, mode=1)
    tag = '{' + ','.join(key + ':' + str(value) for key, value in data.items()) + '}'
    target, tier = recipe
    extra = [{'type': 'minecraft:set_custom_data', 'tag': tag},
             {'type': 'minecraft:set_components', 'components': {'minecraft:tooltip_style': 'lbc:tier_' + tier}},
             {'type': 'minecraft:set_lore', 'mode': 'append', 'lore': [{'translate': 'upgraded_item', 'color': 'green', 'italic': False}]}]
    copied = deepcopy(entry)
    leaf = copied
    while node_kind(leaf) == 'group':
        leaf = leaf['children'][0]
    leaf['name'] = target
    return [(boundary(copied, {'modifier': extra}), chain + extra, origin)]


def node_kind(node):
    return node.get('type', '').removeprefix('minecraft:')


def boundary(child, owner):
    """A native singleton group keeps exact local condition/modifier ordering."""
    extra = {k: deepcopy(owner[k]) for k in ('condition', 'modifier') if k in owner}
    return {'type': 'minecraft:group', 'children': [child], **extra} if extra else child


def build(table_root=TABLES):
    docs, invalid, findings = {}, [], []
    for path in sorted(table_root.rglob('*.json')):
        # Public stable-slot commands reuse this catalog; they are outputs, not new source profiles.
        if path.relative_to(table_root).parts[0] == 'items':
            continue
        name = 'lbc:' + path.relative_to(table_root).as_posix()[:-5]
        try:
            docs[name] = json.loads(path.read_text(encoding='utf-8-sig'))
        except (ValueError, OSError) as error:
            invalid.append({'table': name, 'reason': str(error)})
    selection_sources = {}
    prior = None
    for name in ('lbc:random_item', 'lbc:random_glack_1', 'lbc:infinity_no_shulker'):
        doc = docs.get(name)
        if doc is not None and 'skywars2:uniform_item' in packed(doc):
            if prior is None:
                prior = json.loads(OUTPUT.read_text(encoding='utf-8'))
            source = prior.get('selectionSources', {}).get(name)
            if source is None:
                raise ValueError('Bridged selection root needs its original source snapshot: ' + name)
            docs[name] = deepcopy(source)
        if name in docs:
            selection_sources[name] = deepcopy(docs[name])
    functional_sources = {}
    for short in FUNCTIONAL:
        name = 'lbc:' + short; doc = docs[name]
        if 'skywars2:uniform_item' in packed(doc):
            if prior is None: prior = json.loads(OUTPUT.read_text(encoding='utf-8'))
            if name not in prior.get('functionalSources', {}): raise ValueError('Bridged functional selector needs its source snapshot: ' + name)
            docs[name] = deepcopy(prior['functionalSources'][name])
        functional_sources[name] = deepcopy(docs[name])
    memo, active = {}, set()
    blocked = []
    reverse = defaultdict(set)
    for name, doc in docs.items():
        for ref in table_references(doc):
            reverse[ref].add(name)
    def usage_owners(name, seen=None):
        seen = set() if seen is None else seen
        if name in seen:
            return set()
        seen.add(name)
        result = set()
        for parent in reverse[name]:
            if parent.startswith('lbc:item_defs/'):
                result |= usage_owners(parent, seen)
            else:
                result.add(parent)
        return result
    excluded_source_roots = {}
    for name in docs:
        owners = usage_owners(name) if name.startswith('lbc:item_defs/') else set()
        if service_table(name) or owners and all(service_table(owner) for owner in owners):
            excluded_source_roots[name] = {'reason': 'GUI preview/service-only canonical owner', 'usageOwners': sorted(owners)}
    qualified_source = subprocess.run(['git', 'show', QUALIFIED_ACADEMY], cwd=ROOT,
                                     check=True, capture_output=True).stdout.decode('utf-8')
    default_native = deepcopy(DEFAULT_NATIVE)
    sword_owner = '4aa9601b:src/main/java/me/skywars2/game/LbcWtf.java'
    sword_source = subprocess.run(['git', 'show', sword_owner], cwd=ROOT,
                                  check=True, capture_output=True).stdout.decode('utf-8')
    if 'static ItemStack sword(){return new ItemStack(SWORD);}' not in sword_source:
        raise ValueError('Complete Oguzok prototype policy no longer matches qualified native factory')
    default_native['skywars2:oguzok_sword'].update(qualifiedOwner=sword_owner,
                                                 ownerSha256=hashlib.sha256(sword_source.encode()).hexdigest())
    consumed_owner = '4aa9601b:src/main/java/me/skywars2/game/LbcConsumedBlock.java'
    models_owner = '4aa9601b:src/main/java/me/skywars2/game/LbcBlockModels.java'
    consumed_source = subprocess.run(['git', 'show', consumed_owner], cwd=ROOT,
                                     check=True, capture_output=True).stdout.decode('utf-8')
    models_source = subprocess.run(['git', 'show', models_owner], cwd=ROOT,
                                   check=True, capture_output=True).stdout.decode('utf-8')
    if 'new LbcBlockModels.CubeItem(BLOCK,' not in consumed_source or '.component(DataComponents.ITEM_MODEL,MODEL)' not in consumed_source or 'class CubeItem extends PolymerBlockItem' not in models_source:
        raise ValueError('Complete consumed-block policy no longer matches the qualified native block-item factory')
    default_native['skywars2:consumed_block'].update(qualifiedOwner=consumed_owner,
        ownerSha256=hashlib.sha256(consumed_source.encode()).hexdigest(), transportOwner=models_owner,
        transportOwnerSha256=hashlib.sha256(models_source.encode()).hexdigest())
    native_evidence = {'skywars2:cunny_' + str(index): {
        'source': 'lbc:imba/cunny_slabs', 'owner': 'LbcCunnyItems.SOURCE/SLABS/register',
        'reason': 'Native registration fully decodes the exact indexed cunny_items.json authored prototype.'}
        for index in range(22)}
    for base, (target, tier) in ACADEMY_EXTRAS.items():
        native_evidence[target] = {'sourceCarrier': base, 'owner': QUALIFIED_ACADEMY,
                                   'ownerSha256': hashlib.sha256(qualified_source.encode()).hexdigest(),
                                   'recipe': 'full source base components + native transmuteCopy + ready/upgraded/mode flags + tier + appended source upgraded lore'}

    def issue(table, path, reason):
        row = {'table': table, 'path': path, 'reason': reason}
        if row not in blocked:
            blocked.append(row)

    def expand_table(name):
        if name in memo:
            return memo[name]
        if name in active:
            issue(name, [], 'recursive reference'); return []
        if name not in docs:
            issue(name, [], 'missing or unparsed referenced table'); return []
        active.add(name)
        rows = []
        doc = docs[name]
        if not isinstance(doc, dict) or 'pools' not in doc:
            issue(name, [], 'table has no native pools')
        else:
            for pi, pool in enumerate(doc['pools']):
                for ei, entry in enumerate(pool.get('entries', [])):
                    for leaf, chain, origin in expand_entry(entry, name, ['pools', pi, 'entries', ei]):
                        rows.append((boundary(boundary(leaf, pool), doc),
                                     chain + modifiers(pool) + modifiers(doc), origin))
        active.remove(name)
        memo[name] = rows
        return rows

    def expand_entry(entry, table, path):
        kind = node_kind(entry)
        # Legacy plural functions/conditions are ignored by the current native
        # codecs. Retain their exact source as an explicit finding, never enable them.
        for key in ('functions', 'conditions'):
            if key in entry:
                row = {'table': table, 'path': path, 'field': key, 'value': entry[key],
                       'reason': 'ignored legacy field retained in source; not made active by this catalog'}
                if row not in findings:
                    findings.append(row)
        if kind == 'item':
            if not isinstance(entry.get('name'), str):
                issue(table, path, 'nonliteral item'); return []
            allowed = {'type', 'name', 'weight', 'quality', 'modifier', 'condition', 'functions', 'conditions'}
            if set(entry) - allowed:
                issue(table, path, 'unknown item fields: ' + str(sorted(set(entry) - allowed)))
            leaf = {k: deepcopy(v) for k, v in entry.items() if k not in {'weight', 'quality', 'functions', 'conditions'}}
            leaf['type'] = 'minecraft:item'
            return [(leaf, modifiers(entry), {'table': table, 'path': path})]
        if kind == 'loot_table':
            ref = entry.get('value', entry.get('name'))
            if isinstance(ref, str) and ref.startswith('lbc:'):
                return [(boundary(leaf, entry), chain + modifiers(entry), origin)
                        for leaf, chain, origin in expand_table(ref)]
            if isinstance(ref, dict) and 'pools' in ref:
                rows = []
                for pi, pool in enumerate(ref['pools']):
                    for ei, child in enumerate(pool.get('entries', [])):
                        for leaf, chain, origin in expand_entry(child, table, path + ['value', 'pools', pi, 'entries', ei]):
                            rows.append((boundary(boundary(boundary(leaf, pool), ref), entry),
                                         chain + modifiers(pool) + modifiers(ref) + modifiers(entry), origin))
                return rows
            issue(table, path, 'nonliteral/non-LBC table reference'); return []
        if kind in {'group', 'sequence', 'alternatives'}:
            children = entry.get('children', [])
            # Alternatives/sequence may impose sibling success/failure semantics.
            # Preserve the real subtree for single-logical-item composites only.
            flattened = []
            prior = []
            for ci, child in enumerate(children):
                if kind != 'group' and node_kind(child) not in {'item', 'loot_table'}:
                    issue(table, path, kind + ' has non-singleton sibling success semantics')
                gate = None
                if kind == 'alternatives' and any(condition is None for condition in prior):
                    findings.append({'table': table, 'path': path + ['children', ci], 'reason': 'unreachable after unconditional alternatives sibling'})
                    continue
                terms = [({'type': 'minecraft:inverted', 'term': condition} if kind == 'alternatives' else condition)
                         for condition in prior if condition is not None]
                if kind != 'group' and terms:
                    gate = {'condition': {'type': 'minecraft:all_of', 'terms': terms}}
                for leaf, chain, origin in expand_entry(child, table, path + ['children', ci]):
                    flattened.append((boundary(leaf, gate) if gate else leaf, chain, origin))
                prior.append(child.get('condition'))
            return [(boundary(leaf, entry), chain + modifiers(entry), origin) for leaf, chain, origin in flattened]
        if kind == 'empty':
            findings.append({'table': table, 'path': path, 'reason': 'explicit empty selection is not a logical item'})
            return []
        issue(table, path, 'unsupported entry type: ' + str(kind)); return []

    # Catalog authority is the real all-item selection closure plus canonical
    # definitions. An arbitrary chest/structure caller does not create a new
    # global all-item profile: its wrapper stays on that caller.
    slots = {}
    accounting = {}
    pure_vanilla_profiles = defaultdict(set)
    source_hashes = {}
    roots = sorted({'lbc:random_item', 'lbc:random_glack_1', 'lbc:imba/cunny_slabs'} | set(HELPER_PRIMITIVES.values()) |
                   {name for name, doc in docs.items()
                    if name not in excluded_source_roots and name not in HELPER_PRIMITIVES and
                    (name.startswith('lbc:item_defs/') or standalone(doc) is not None)})
    for name in roots:
        source_rows = expand_table(name)
        rows = []
        for entry, chain, origin in source_rows:
            rows.append((entry, chain, origin))
            rows.extend(promoted_profiles(entry, chain, origin, qualified_source))
        for entry, chain, origin in rows:
            base = entry
            while node_kind(base) == 'group':
                base = base['children'][0]
            flat = {'type': 'minecraft:item', 'name': base['name'], 'modifier': chain}
            parts = logical_identity(flat)
            carrier = parts['carrier']
            key = packed(parts)
            record = {'source': name, 'leaf': origin, 'identity': parts}
            # The original TMT token/carriers are one factory-backed item.
            is_tmt = carrier in TMT_NATIVE or any(
                isinstance(row, dict) and node_kind(row) == 'set_custom_data'
                and any(flag in str(row.get('tag', '')).replace(' ', '') for flag in ('tmt:1', 'tmt_item:1'))
                for row in chain)
            if is_tmt:
                accounting[packed([name, origin, key])] = {**record, 'disposition': 'tmt_factory'}
                continue
            if carrier in EXCLUDED_NATIVE:
                accounting[packed([name, origin, key])] = {**record, 'disposition': 'excluded', 'reason': EXCLUDED_NATIVE[carrier]}
                continue
            if carrier == 'minecraft:air':
                accounting[packed([name, origin, key])] = {**record, 'disposition': 'air_not_item'}
                continue
            if len(parts) == 1 and carrier.startswith('minecraft:'):
                pure_vanilla_profiles[carrier].add(packed(entry))
                accounting[packed([name, origin, key])] = {**record, 'disposition': 'vanilla_identity'}
                continue
            slot = slots.setdefault(key, {'id': 'custom/' + hashlib.sha256(key.encode()).hexdigest()[:20],
                                          'identity': parts, 'profiles': {}, 'sources': set(), 'semanticTypes': set()})
            slot['semanticTypes'].update(semantic_types(chain))
            # Equal distinct contextual profiles inside one outer slot. Source
            # aliases and count variants never add an outer lottery ticket.
            signature = packed(entry)
            slot['profiles'].setdefault(signature, {'entry': entry, 'origins': set()})['origins'].add(packed(origin))
            slot['sources'].add(name)
            accounting[packed([name, origin, key])] = {**record, 'disposition': slot['id']}
    typed_views = {}
    typed_findings = []
    typed_sources = {name: ('lbc:transformables_' + name,) for name in TYPE_FLAGS}
    typed_sources.update({name + '_all': ('lbc:transformables_' + name + '_all',) for name in ALL_TYPES})
    typed_sources.update(SUBTYPES)
    for name, source_tables in typed_sources.items():
        members = defaultdict(dict); tmt = False
        for table in source_tables:
            for entry, chain, origin in expand_table(table):
                flat = flat_leaf(entry, chain); parts = logical_identity(flat); key = packed(parts)
                if parts['carrier'] in TMT_NATIVE or any(flag in str(parts.get('custom_data', '')).replace(' ', '') for flag in ('tmt:1', 'tmt_item:1')):
                    tmt = True; continue
                if parts['carrier'] in EXCLUDED_NATIVE or parts['carrier'] == 'minecraft:air': continue
                slot = slots.get(key)
                if slot is None:
                    if len(parts) == 1 and parts['carrier'].startswith('minecraft:'):
                        slot_id = 'vanilla/' + parts['carrier']
                    else:
                        issue(table, origin['path'], 'typed logical identity absent from global catalog: ' + key); continue
                else: slot_id = slot['id']
                members[slot_id][packed(entry)] = entry
        source_ids = set(members)
        if name.endswith('_all'):
            category = name[:-4]
            for slot in slots.values():
                if category in slot['semanticTypes'] and slot['id'] not in members:
                    for profile in slot['profiles'].values(): members[slot['id']][packed(profile['entry'])] = profile['entry']
            if category == 'sword': members['native/skywars2:oguzok_sword'] = {}
        if tmt: members['factory/tmt'] = {}
        typed_views[name] = {'sources': list(source_tables), 'policy': 'exact authored closure' if not name.endswith('_all') else 'original broader closure profiles take precedence; new semantic category members use their full global profiles',
                             'members': members, 'sourceMembers': sorted(source_ids), 'tmtFactory': tmt}
    for slot in slots.values():
        slot['sources'] = sorted(slot['sources'])
        slot['semanticTypes'] = sorted(slot['semanticTypes'])
        slot['profiles'] = [{**row, 'origins': [json.loads(x) for x in sorted(row['origins'])]}
                            for _, row in sorted(slot['profiles'].items())]
    # Intern entry trees and leaf origins; repeated caller paths must not copy
    # thousands of component-rich canonical definitions into a giant artifact.
    entry_nodes = {}
    origins = {}
    def intern(node):
        value = {k: deepcopy(v) for k, v in node.items() if k != 'children'}
        if 'children' in node:
            value['children'] = [intern(child) for child in node['children']]
        code = hashlib.sha256(packed(value).encode()).hexdigest()[:24]
        entry_nodes.setdefault(code, value)
        return code
    def origin_id(origin):
        key = packed(origin)
        code = hashlib.sha256(key.encode()).hexdigest()[:20]
        origins.setdefault(code, origin)
        return code
    for slot in slots.values():
        slot['profiles'] = [{'entry': intern(profile['entry']),
                             'origins': [origin_id(origin) for origin in profile['origins']]}
                            for profile in slot['profiles']]
    for view in typed_views.values():
        view['members'] = {slot: [intern(entry) for _, entry in sorted(entries.items())]
                           for slot, entries in sorted(view['members'].items())}
    vanilla_source_members = defaultdict(set)
    for entry, chain, origin in expand_table('lbc:infinity_no_shulker'):
        item = flat_leaf(entry, chain)['name']; item = item if ':' in item else 'minecraft:' + item
        if not item.startswith('minecraft:') or item == 'minecraft:air':
            issue('lbc:infinity_no_shulker', origin['path'], 'non-vanilla item in vanilla source selection: ' + item); continue
        vanilla_source_members['vanilla/' + item].add(intern(entry))
    compact_accounting = sorted({(row['source'], origin_id(row['leaf']), row['disposition'])
                                 for row in accounting.values()})
    source_hashes = {name: hashlib.sha256(packed(docs[name]).encode()).hexdigest()
                     for name in sorted(memo) if name in docs}
    result = {'schema': 1, 'complete': not blocked,
              'policy': 'Uniform logical outer slots; equal distinct source-preserving profiles inside a slot; no source roll multiplicity or branch weight promoted to slot multiplicity.',
              'domains': {'random_item': 'custom', 'random_glack_1': 'union',
                          'random_glack': 'existing count1..64/max_stack99 wrapper around union'},
              'custom': sorted(slots.values(), key=lambda row: row['id']),
              'tmt': {'id': 'factory/tmt', 'factory': 'LbcTmtRuntime.generate', 'variants': 5, 'outerSlots': 1},
              'excludedNative': EXCLUDED_NATIVE, 'tmtNative': sorted(TMT_NATIVE),
              'allowedDefaultNative': default_native, 'nativeEvidence': native_evidence,
              'typedViews': typed_views, 'typedFindings': typed_findings,
              'typedCombinations': [list(types) for types in sorted(set(FUNCTIONAL.values())) if types],
              'typedLegacyTables': {**{'lbc:transformables_' + name: 'lbc:uniform/' + name for name in typed_views if name not in SUBTYPES},
                                    'lbc:rofls': 'lbc:uniform/rofl', 'lbc:slabs': 'lbc:uniform/slab', 'lbc:spells_library': 'lbc:uniform/grimoire'},
              'functionalLegacyTables': {'lbc:' + name: 'lbc:uniform/' + name for name in FUNCTIONAL},
              'vanillaSourceViews': {'no_recursive_shulker': {'sources': ['lbc:infinity_no_shulker'], 'policy': 'source per-item profiles; plain vanilla empty shulker boxes remain eligible; no custom recursive container_loot factory',
                                                            'members': {key: sorted(values) for key, values in sorted(vanilla_source_members.items())}}},
              'functionalSources': functional_sources,
              'excludedSourceRoots': excluded_source_roots,
              'helperPrimitives': HELPER_PRIMITIVES,
              'pureVanillaSourceProfiles': {key: [intern(json.loads(x)) for x in sorted(values)] for key, values in sorted(pure_vanilla_profiles.items())},
              'entryNodes': entry_nodes, 'origins': origins,
              'accounting': compact_accounting,
              'roots': roots, 'sourceHashes': source_hashes, 'blocked': blocked,
              'selectionSources': selection_sources,
              'findings': findings, 'unparsedSourceResources': invalid}
    return result


def selector_resources(result):
    def entry(domain, types=()):
        return {'type': 'skywars2:uniform_item', 'domain': domain, **({'types': list(types)} if types else {})}
    def table(domain, types=(), modifier=None):
        value = entry(domain, types)
        if modifier is not None: value['modifier'] = modifier
        return {'pools': [{'rolls': 1, 'entries': [value]}]}
    resources = {name: table('custom', (name,)) for name in result['typedViews']}
    for domain in ('custom', 'vanilla', 'union'):
        resources[domain] = table(domain)
        for count in (64, 99):
            resources[domain + '_' + str(count)] = table(domain, modifier=[
                {'type': 'minecraft:set_count', 'count': count},
                {'type': 'minecraft:set_components', 'components': {'minecraft:max_stack_size': 99, '!minecraft:max_damage': {}}},
                {'type': 'minecraft:limit_count', 'limit': {'max': 99}}])
    resources['vanilla_no_recursive_shulker'] = table('vanilla', ('no_recursive_shulker',))
    for short, types in FUNCTIONAL.items():
        document = deepcopy(result['functionalSources']['lbc:' + short])
        for pool in document['pools']:
            original = pool['entries']
            extras = [{key: value for key, value in row.items() if key not in {'type', 'name', 'value', 'weight', 'quality'}} for row in original]
            if any(value != extras[0] for value in extras[1:]):
                raise ValueError('Functional category branches have different caller wrappers: ' + short)
            pool['entries'] = [{**entry('union' if short == '27_random_items' else 'custom', types), **extras[0]}]
        resources[short] = document
    return resources


def self_test():
    a = {'type': 'minecraft:item', 'name': 'minecraft:stick', 'modifier': [
        {'type': 'minecraft:set_custom_data', 'tag': '{wand:1,mode:2}'},
        {'type': 'minecraft:set_count', 'count': 64}]}
    b = deepcopy(a); b['modifier'][-1]['count'] = 99
    assert identity(a) == identity(b)
    b['modifier'][0]['tag'] = '{wand:1,mode:3}'
    assert identity(a) != identity(b)
    inner = {'type': 'minecraft:set_count', 'count': 64}
    outer = {'type': 'minecraft:set_count', 'count': 99}
    assert boundary({'type': 'minecraft:item', 'name': 'minecraft:stick', 'modifier': [inner]}, {'modifier': [outer]})['modifier'] == [outer]
    assert semantic_types([{'type': 'minecraft:set_custom_data', 'tag': '{nested:{sword:1},sword:0,spellitem:1b}'}]) == {'spells'}
    assert semantic_types([{'type': 'minecraft:set_custom_data', 'tag': '{sword:1}'}, {'type': 'minecraft:set_components', 'components': {'minecraft:custom_data': {'axe': 1}}}]) == {'axe'}
    assert semantic_types([{'type': 'minecraft:set_custom_data', 'tag': '{sword:1}'}, {'type': 'minecraft:set_components', 'components': {'!minecraft:custom_data': {}}}]) == set()
    print('PASS logical identity count/mode and native modifier-boundary synthetic checks')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--self-test', action='store_true')
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    if args.self_test:
        self_test()
    else:
        result = build()
        output = json.dumps(result, ensure_ascii=False, separators=(',', ':')) + '\n'
        if args.check:
            assert OUTPUT.read_text(encoding='utf-8') == output, 'Regenerate all_item_pools.json'
        else:
            OUTPUT.write_text(output, encoding='utf-8')
        for name, document in selector_resources(result).items():
            path = TABLES / 'uniform' / (name + '.json')
            content = json.dumps(document, ensure_ascii=False, indent=2) + '\n'
            if args.check: assert path.read_text(encoding='utf-8') == content, 'Regenerate uniform/' + name
            elif not path.is_file() or path.read_text(encoding='utf-8') != content:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(content, encoding='utf-8')
        print(json.dumps({'customSlots': len(result['custom']), 'accountedLeaves': len(result['accounting']),
                          'blocked': result['blocked'], 'unparsed': result['unparsedSourceResources']}, ensure_ascii=False))
