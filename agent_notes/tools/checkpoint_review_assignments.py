"""Record only evidence-backed review state and current delegation."""
from pathlib import Path
import json

path = Path('agent_notes/inventory/research_jobs.json')
jobs = json.loads(path.read_text(encoding='utf-8'))
maps_audit = json.loads(Path('agent_notes/inventory/maps_details_audit.json').read_text(encoding='utf-8'))
assert maps_audit['checked'] == 1456 and not maps_audit['errors']
jobs['maps_catalogue'].update(content_reviewed=True, reviewed_by='/root',
    review_note='All 1456 NBT decoded and source-SHA checked; full palettes, root metadata, block/entity NBT preserved; all embedded commands reviewed. Placement functions are a separate review job.')
for key in ['lbc_workingitems_awaken', 'lbc_workingitems_main', 'lbc_workingitems_generators']:
    rows = json.loads((Path('agent_notes/coverage') / (key + '.json')).read_text(encoding='utf-8'))
    assert len(rows) == len(jobs[key]['sources'])
    assert {r['source'] for r in rows} == set(jobs[key]['sources'])
    assert all(r['status'] != 'unresolved' for r in rows)
    jobs[key]['status'] = 'awaiting_integration_review'
for key in ['lbc_items_weapons', 'lbc_items_arcana_trinkets']:
    jobs[key]['reviewer_agent'] = '/root/maps_catalogue'
    jobs[key]['review_document'] = 'agent_notes/review_weapons_arcana.md'
for key in ['lbc_other_main', 'lbc_other_misc', 'lbc_other_events']:
    jobs[key]['reviewer_agent'] = '/root/lbc_drop_effects'
    jobs[key]['review_document'] = 'agent_notes/review_other_main_misc_events.md'
jobs['lbc_runtime']['reviewer_agent'] = '/root/skywars_game'
jobs['lbc_runtime']['review_document'] = 'agent_notes/review_runtime.md'
path.write_text(json.dumps(jobs, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('Content-reviewed jobs:', sum(bool(v.get('content_reviewed')) for v in jobs.values()))
