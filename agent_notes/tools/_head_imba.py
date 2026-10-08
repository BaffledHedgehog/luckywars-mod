from pathlib import Path
s=Path('agent_notes/lbc_other_imba.md').read_text(encoding='utf-8')
for i,l in enumerate(s.splitlines()[:120],1): print(i,repr(l[:200]))
