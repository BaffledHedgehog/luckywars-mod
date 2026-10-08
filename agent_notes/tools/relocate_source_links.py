"""One-time repair of old relative source links after moving agent_notes."""
from pathlib import Path
import re,json
from doc_paths import NOTES, SOURCE_ROOT

changed=[];count=0
for p in NOTES.glob('*.md'):
    s=p.read_text(encoding='utf-8')
    # Do not perform a bulk edit of an author-annotated document.
    if '(user)' in s.lower():
        continue
    replacements=[0]
    def replace(match):
        raw=match.group(1)
        if not raw.startswith(('../world/','../plugins/','../mods/')):
            return match.group(0)
        replacements[0]+=1
        return ']('+SOURCE_ROOT.as_posix()+'/'+raw[3:]+')'
    new=re.sub(r'\]\(([^\s)]+)\)',replace,s)
    if new!=s:
        p.write_text(new,encoding='utf-8');changed.append(p.name);count+=replacements[0]
report={'documents':changed,'source_links_repaired':count,
        'source_root':SOURCE_ROOT.as_posix(),'scope':'Markdown link targets only; source identifiers and source files unchanged.'}
(NOTES/'inventory/relocation_audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'changed_documents':len(changed),'source_links_repaired':count}))
