"""Build immutable TMT text/sound inputs; legacy functions and author RP are read-only."""
import argparse
import hashlib
import json
from pathlib import Path
from extract_tmt_source import extract


def build(source, resource_pack):
    original = extract(source)
    names = dict.fromkeys(original['names'])
    sounds = dict.fromkeys('minecraft:' + value if ':' not in value else value
                           for value in original['sounds'])
    receipts = dict(original['sha256'])
    receipts['set_basics.mcfunction'] = hashlib.sha256((source / 'set_basics.mcfunction').read_bytes()).hexdigest()
    for language in ('en_us', 'ru_ru'):
        path = resource_pack / 'assets/minecraft/lang' / (language + '.json')
        raw = path.read_bytes()
        names.update(dict.fromkeys(json.loads(raw.decode('utf-8-sig'))))
        receipts[path.relative_to(resource_pack).as_posix()] = hashlib.sha256(raw).hexdigest()
    for path in sorted((resource_pack / 'assets').glob('*/sounds.json')):
        raw = path.read_bytes()
        namespace = path.parent.name
        sounds.update(dict.fromkeys(namespace + ':' + key if ':' not in key else key
                                    for key in json.loads(raw.decode('utf-8-sig'))))
        receipts[path.relative_to(resource_pack).as_posix()] = hashlib.sha256(raw).hexdigest()
    return {'source': 'Full datapack TMT literal properties plus current author translations/sounds',
            'sha256': receipts, 'baseFlags': original['baseFlags'],
            'names': list(names), 'sounds': list(sounds)}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    parser.add_argument('resource_pack', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    if any(args.output.resolve().is_relative_to(root.resolve()) for root in (args.source, args.resource_pack)):
        parser.error('Output must stay outside the read-only source and author resource pack')
    result = build(args.source, args.resource_pack)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'names': len(result['names']), 'sounds': len(result['sounds']),
                      'readOnlyInputFiles': len(result['sha256'])}))
