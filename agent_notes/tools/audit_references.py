"""Literal resource boundaries, preserving macros instead of truncating them."""
import json
import re
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NOTES = ROOT / 'agent_notes'
TOKEN = re.compile(r'#?[a-z0-9_.-]+:[a-z0-9_./-]+\Z')


def tokens(command):
    """Keep JSON/SNBT, selector brackets and quoted text as single arguments."""
    result, start, depth, quote, escaped = [], None, 0, None, False
    for index, character in enumerate(command):
        if start is None:
            if character.isspace():
                continue
            start = index
        if quote:
            if escaped:
                escaped = False
            elif character == '\\':
                escaped = True
            elif character == quote:
                quote = None
        elif character in ('"', "'"):
            quote = character
        elif character in '[{':
            depth += 1
        elif character in ']}':
            depth -= 1
        elif character.isspace() and depth == 0:
            result.append(command[start:index])
            start = None
    if start is not None:
        result.append(command[start:])
    if result:
        result[0] = result[0].removeprefix('$')
    return result


def command_references(command):
    args = tokens(command)
    for index, arg in enumerate(args[:-1]):
        after = args[index + 1]
        if arg == 'function':
            yield 'function', after
        elif arg == 'schedule' and after == 'clear' and len(args) > index + 2:
            yield 'function', args[index + 2]
        elif arg == 'predicate' and index and args[index - 1] in ('if', 'unless'):
            yield 'predicate', after
        elif arg == 'loot' and (':' in after or '$(' in after) and not after.startswith(('{', '[')):
            yield 'loot_table', after
        elif arg == 'place' and after in ('template', 'structure', 'jigsaw') and len(args) > index + 2:
            kind = {'template': 'structure', 'structure': 'worldgen/structure', 'jigsaw': 'worldgen/template_pool'}[after]
            yield kind, args[index + 2]
        elif arg == 'item' and after == 'modify':
            offset = 5 if args[index + 2] == 'entity' else 7
            if len(args) > index + offset:
                yield 'item_modifier', args[index + offset]


def main():
    inventory = json.loads((NOTES / 'inventory/files.json').read_text(encoding='utf-8'))
    resources = defaultdict(list)
    for row in inventory:
        parts = row['relative'].split('/')
        if len(parts) < 4 or parts[0] != 'data':
            continue
        category = parts[2]
        if category in ('worldgen', 'tags'):
            category += '/' + parts[3]
            tail = '/'.join(parts[4:])
        else:
            tail = '/'.join(parts[3:])
        rid = parts[1] + ':' + tail.removesuffix(row['suffix'])
        resources[(category, rid)].append(row['source'])
    references = []

    def add(kind, rid, source, number, context):
        if rid.startswith(('{', '[')):
            status = 'inline_definition'
        elif '$(' in rid:
            status = 'dynamic_macro'
        elif not TOKEN.fullmatch(rid if ':' in rid else 'minecraft:' + rid.lstrip('#')):
            status = 'unparsed_token'
        else:
            if ':' not in rid:
                rid = ('#' if rid.startswith('#') else '') + 'minecraft:' + rid.lstrip('#')
            lookup_kind = 'tags/function' if kind == 'function' and rid.startswith('#') else kind
            status = 'present' if (lookup_kind, rid.lstrip('#')) in resources else 'outside_studied_packs'
        references.append({'kind': kind, 'target': rid, 'source': source, 'line': number,
                           'status': status, 'context': context})

    for row in inventory:
        source = row['source']
        if row['suffix'] == '.mcfunction':
            for number, line in enumerate((ROOT / source).read_text(encoding='utf-8-sig').splitlines(), 1):
                command = line.strip()
                if not command or command.startswith('#'):
                    continue
                for kind, rid in command_references(command):
                    add(kind, rid, source, number, command)
        elif '/advancement/' in source and row['suffix'] == '.json':
            value = json.loads((ROOT / source).read_text(encoding='utf-8-sig'))
            reward = value.get('rewards', {}).get('function')
            if reward:
                add('function', reward, source, None, 'advancement rewards.function')
    grouped = defaultdict(list)
    for ref in references:
        if ref['status'] not in ('present', 'inline_definition'):
            grouped[(ref['kind'], ref['target'], ref['status'])].append(ref)
    boundary = [{'kind': key[0], 'target': key[1], 'status': key[2], 'callers': callers}
                for key, callers in sorted(grouped.items())]
    output = {'scope': 'Literal command references and advancement reward functions. No automatic source changes.',
              'warning': 'Absence in these three packs does not prove absence in the whole server; dynamic macros are kept unresolved expressions. Quoted chat/dialog commands and NBT references are outside this command-token scan.',
              'counts': dict(Counter(ref['status'] for ref in references)), 'references': references,
              'boundaries': boundary}
    (NOTES / 'inventory/resource_reference_audit.json').write_text(json.dumps(output, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    compact = {k: output[k] for k in ('scope', 'warning', 'counts')}
    compact['unique_boundaries'] = dict(Counter(ref['status'] for ref in boundary))
    compact['unparsed_examples'] = [ref for ref in boundary if ref['status'] == 'unparsed_token'][:10]
    print(json.dumps(compact, ensure_ascii=False))


if __name__ == '__main__':
    main()
