"""Check documentation links and report remaining source coverage honestly."""
from __future__ import annotations

import json
import re
from collections import Counter
from pathlib import Path

from doc_paths import NOTES, SOURCE_ROOT as ROOT, document_path
STATUSES = {'documented', 'generated_variant', 'asset', 'non_runtime', 'unresolved'}


def main():
    inventory = json.loads((NOTES / 'inventory/files.json').read_text(encoding='utf-8'))
    sources = {row['source']: row for row in inventory}
    records, errors = {}, []
    heading_cache = {}
    for file in sorted((NOTES / 'coverage').glob('*.json')):
        try:
            rows = json.loads(file.read_text(encoding='utf-8-sig'))
            if not isinstance(rows, list):
                raise ValueError('coverage must be a list')
        except (ValueError, OSError) as exc:
            errors.append(f'{file.name}: {exc}')
            continue
        for row in rows:
            source = row.get('source', '')
            document = row.get('document', '')
            section = row.get('section', '')
            status = row.get('status', '')
            problems = []
            if '\ufffd' in row.get('note', '') or re.search(r'\?{4,}', row.get('note', '')):
                problems.append('corrupted or unreadable note')
            if source not in sources:
                problems.append('unknown source')
            if status not in STATUSES:
                problems.append('invalid status')
            path = document_path(document)
            if not document.startswith('agent_notes/') or not path.is_file():
                problems.append('missing document')
            else:
                if document not in heading_cache:
                    text = path.read_text(encoding='utf-8-sig')
                    heading_cache[document] = {match.group(1).strip() for match in re.finditer(r'^#{1,6}\s+(.+)$', text, re.M)}
                headings = heading_cache[document]
                if not section or section not in headings:
                    problems.append('section is not an exact Markdown heading')
            if problems:
                errors.append(f'{file.name}: {source}: {", ".join(problems)}')
                continue
            records.setdefault(source, []).append(dict(row, coverage_file=file.name))
    missing, unresolved, grouped = [], [], Counter()
    for source, row in sources.items():
        entries = records.get(source, [])
        if not entries:
            missing.append(source)
            grouped[row['pack'] + '/' + row['group']] += 1
        elif all(entry['status'] == 'unresolved' for entry in entries):
            unresolved.append(source)
    report = {
        'total_sources': len(sources),
        'validly_linked_sources': len(records),
        'missing_count': len(missing),
        'unresolved_count': len(unresolved),
        'link_errors': errors,
        'missing_by_group': dict(sorted(grouped.items())),
        'missing_sources': missing,
        'unresolved_sources': unresolved,
        'warning': 'Coverage links prove traceability, not semantic completeness. Human review required.'
    }
    output = NOTES / 'inventory/coverage_report.json'
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    result = {key: report[key] for key in ('total_sources', 'validly_linked_sources',
              'missing_count', 'unresolved_count')}
    result['link_error_count'] = len(errors)
    result['first_link_errors'] = errors[:5]
    print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__':
    main()
