"""Generate a per-job checkpoint; never infer semantic completion from counts."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NOTES = ROOT / 'agent_notes'


def main():
    jobs = json.loads((NOTES / 'inventory/research_jobs.json').read_text(encoding='utf-8'))
    records = {}
    for file in (NOTES / 'coverage').glob('*.json'):
        try:
            rows = json.loads(file.read_text(encoding='utf-8-sig'))
        except (ValueError, OSError):
            continue  # A researcher may still be writing its file.
        for row in rows:
            records.setdefault(row['source'], []).append(row)
    lines = ['# Покрытие по заданиям', '',
             'Это счётчик заявленных записей исследователей. Он не подтверждает правильность',
             'или полноту поведения; ссылки проверяет `check_coverage.py`, содержание — координатор.', '',
             '| Задание | Файлов | Описаны по coverage | Unresolved | Без записи | Проверка содержания |',
             '|---|---:|---:|---:|---:|---|']
    snapshot = {}
    for name, job in jobs.items():
        covered = unresolved = missing = 0
        documents = set()
        for source in job['sources']:
            rows = records.get(source, [])
            if not rows:
                missing += 1
            elif any(row['status'] != 'unresolved' for row in rows):
                covered += 1
                documents.update(row['document'] for row in rows)
            else:
                unresolved += 1
        reviewed = job.get('content_reviewed', False)
        review = 'проверено' if reviewed else 'ожидается'
        lines.append(f'| `{name}` | {len(job["sources"])} | {covered} | {unresolved} | {missing} | {review} |')
        snapshot[name] = {'sources': len(job['sources']), 'covered_claims': covered,
                          'unresolved': unresolved, 'missing': missing,
                          'documents': sorted(documents), 'content_reviewed': reviewed}
    (NOTES / 'RESEARCH_PROGRESS.md').write_text('\n'.join(lines) + '\n', encoding='utf-8')
    (NOTES / 'inventory/research_progress.json').write_text(json.dumps(snapshot, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'jobs': len(jobs), 'claimed_described_sources': sum(row['covered_claims'] for row in snapshot.values()),
                      'unresolved_sources': sum(row['unresolved'] for row in snapshot.values()),
                      'missing_sources': sum(row['missing'] for row in snapshot.values()),
                      'content_reviewed_jobs': sum(row['content_reviewed'] for row in snapshot.values())}, ensure_ascii=False))


if __name__ == '__main__':
    main()
