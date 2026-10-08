"""Read-only SHA-256 audit against the original three-datapack inventory."""
import hashlib
import json
from pathlib import Path

from doc_paths import NOTES, SOURCE_ROOT as ROOT

def main():
    snapshot = json.loads((NOTES/'inventory/files.json').read_text(encoding='utf-8'))
    expected = {row['source']: row['sha256'] for row in snapshot}
    current = set()
    for pack in ('LBC', 'maps', 'swarogskywars'):
        current.update(p.relative_to(ROOT).as_posix()
                       for p in (ROOT/'world/datapacksdisabled'/pack).rglob('*') if p.is_file())
    changes = []
    for source, sha in expected.items():
        path = ROOT/source
        if not path.is_file():
            changes.append({'source':source,'change':'missing'})
        elif hashlib.sha256(path.read_bytes()).hexdigest() != sha:
            changes.append({'source':source,'change':'modified'})
    changes.extend({'source':s,'change':'added'} for s in sorted(current-set(expected)))
    report = {'sources':len(snapshot),'changed_since_snapshot':changes}
    (NOTES/'inventory/source_snapshot_audit.json').write_text(
        json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'sources':len(snapshot),'changes':len(changes)}))

if __name__ == '__main__':
    main()
