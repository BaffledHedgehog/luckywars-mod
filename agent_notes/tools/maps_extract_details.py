"""Preserve full palettes and every block NBT omitted by the older summary.

Reads source NBT; does not rebuild or overwrite the existing catalogue.
The fast path skips only exactly encoded {state:int,pos:[int,int,int]} blocks.
Any compound with other fields is decoded by the ordinary Reader.
"""
import collections
import gzip
import hashlib
import json
import re
from pathlib import Path
from maps_nbt import Reader

ROOT = Path(__file__).resolve().parents[2]
NOTES = ROOT / 'agent_notes'
STATE = rb'\x03\x00\x05state.{4}'
POS = rb'\x09\x00\x03pos\x03\x00\x00\x00\x03.{12}'
COMMON = re.compile(rb'(?:(?:' + STATE + POS + rb'|' + POS + STATE + rb')\x00)+', re.DOTALL)

class DetailsReader(Reader):
    def string(self):
        return self.take(self.num('H')).decode('utf-8', 'strict')

    def root_details(self):
        assert self.num('b') == 10
        name = self.string()
        root = {}
        blocks = []
        count = 0
        while True:
            tag = self.num('b')
            if not tag:
                break
            key = self.string()
            if key != 'blocks':
                root[key] = self.payload(tag)
                continue
            assert tag == 9 and self.num('b') == 10, 'blocks is not compound list'
            count = self.num('i')
            assert count >= 0
            index = 0
            while index < count:
                match = COMMON.match(self.data, self.i)
                if match:
                    span = match.end() - self.i
                    assert span % 36 == 0 and index + span // 36 <= count
                    self.i = match.end()
                    index += span // 36
                else:
                    blocks.append({'index': index, 'raw': self.payload(10)})
                    index += 1
        assert self.i == len(self.data), 'trailing bytes'
        return name, root, count, blocks

def main():
    catalog = json.loads((NOTES / 'inventory/maps_structures.json').read_text(encoding='utf-8'))
    details = []
    errors = []
    missing_old = 0
    for index, row in enumerate(catalog['structures'], 1):
        try:
            raw = (ROOT / row['source']).read_bytes()
            assert hashlib.sha256(raw).hexdigest() == row['sha256'], 'source SHA differs'
            unpacked = gzip.decompress(raw) if raw[:2] == b'\x1f\x8b' else raw
            name, root, count, blocks = DetailsReader(unpacked).root_details()
            assert count == row['block_entries'] and root.get('size') == row['size']
            assert name == row['root_name']
            entities = root.pop('entities', [])
            assert entities == [e['raw'] for e in row['entities']], 'entity data differs'
            palette = root.get('palette', [])
            assert len(palette) == row['palette_size']
            assert dict(collections.Counter(p['Name'] for p in palette)) == row['palette']
            assert root.get('DataVersion') == row['format_notes']['data_version']
            old = {tuple(v['pos']): v['nbt'] for v in row['markers']}
            actual = {tuple(v['raw']['pos']): v['raw']['nbt'] for v in blocks if 'nbt' in v['raw']}
            assert all(actual.get(k) == v for k, v in old.items()), 'old block NBT differs'
            missing_old += len(actual.keys() - old.keys())
            # Independently compare fast-path output with a complete ordinary parse
            # in both packs, rather than treating agreement with summary counts as enough.
            if index == 1 or row['source'].endswith('/gallery.nbt'):
                ordinary_name, ordinary = Reader(unpacked).root()
                ordinary_blocks = ordinary.pop('blocks', [])
                ordinary.pop('entities', None)
                assert ordinary_name == name and ordinary == root
                assert [b for b in ordinary_blocks if set(b) != {'state', 'pos'}] == [b['raw'] for b in blocks]
            details.append({'source': row['source'], 'sha256': row['sha256'],
                            'root_metadata': root, 'block_data': blocks})
        except Exception as exc:
            errors.append({'source': row['source'], 'error': str(exc)})
        if index % 100 == 0:
            print('read', index, '/', len(catalog['structures']), flush=True)
    report = {'checked': len(details), 'errors': errors, 'previously_omitted_block_nbt': missing_old,
              'scope': 'Full palette properties and root metadata; all block compounds with extra fields. Entity NBT compared against existing raw catalogue; source SHA unchanged.'}
    (NOTES / 'inventory/maps_details_audit.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    if not errors:
        (NOTES / 'inventory/maps_details.json').write_text(json.dumps({'schema': 'maps-details-v1', 'structures': details}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, ensure_ascii=False), flush=True)

if __name__ == '__main__':
    main()
