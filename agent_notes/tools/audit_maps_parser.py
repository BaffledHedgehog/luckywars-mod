"""Audit NBT consumption, string decoding and catalogue invariants, read-only."""
import collections
import gzip
import hashlib
import json
from pathlib import Path
from maps_nbt import Reader

ROOT = Path(__file__).resolve().parents[2]
NOTES = ROOT/'agent_notes'

class AuditReader(Reader):
    def __init__(self, data):
        super().__init__(data)
        self.tags = collections.Counter()
        self.byte_arrays = 0
        self.high_bytes = 0

    def string(self):
        # Fail rather than silently replacing source text.
        return self.take(self.num('H')).decode('utf-8', 'strict')

    def payload(self, tag):
        self.tags[tag] += 1
        result = super().payload(tag)
        if tag == 7:
            self.byte_arrays += 1
            self.high_bytes += sum(value >= 128 for value in result)
        return result

def main():
    snapshot = json.loads((NOTES/'inventory/files.json').read_text(encoding='utf-8'))
    records = json.loads((NOTES/'inventory/maps_structures.json').read_text(encoding='utf-8'))['structures']
    catalog = {record['source']:record for record in records}
    checked, errors, tags = 0, [], collections.Counter()
    byte_arrays = high_bytes = 0
    for row in snapshot:
        if row['suffix'] != '.nbt':
            continue
        source = row['source']
        try:
            raw = (ROOT/source).read_bytes()
            assert hashlib.sha256(raw).hexdigest() == row['sha256'], 'source changed'
            reader = AuditReader(gzip.decompress(raw) if raw[:2] == b'\x1f\x8b' else raw)
            name, data = reader.root()
            assert reader.i == len(reader.data), 'unconsumed trailing bytes'
            record = catalog[source]
            assert 'error' not in record, 'catalogue error'
            assert data.get('size') == record['size'], 'size differs'
            assert len(data.get('blocks',[])) == record['block_entries'], 'block count differs'
            assert len(data.get('entities',[])) == record['entity_entries'], 'entity count differs'
            palette = data.get('palette',[])
            assert all(0 <= block['state'] < len(palette) for block in data.get('blocks',[])), 'bad palette index'
            tags.update(reader.tags)
            byte_arrays += reader.byte_arrays
            high_bytes += reader.high_bytes
            checked += 1
        except Exception as exc:
            errors.append({'source':source,'error':str(exc)})
    report = {'checked':checked,'errors':errors,'tag_counts':dict(tags),
              'byte_arrays':byte_arrays,'unsigned_byte_array_values_ge128':high_bytes,
              'scope':'All inventoried NBT consumed fully; strings strict UTF-8; original SHA, catalogue sizes/counts and palette indices checked.'}
    (NOTES/'inventory/maps_parser_audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({key:report[key] for key in ('checked','errors','byte_arrays','unsigned_byte_array_values_ge128')}))

if __name__ == '__main__':
    main()
