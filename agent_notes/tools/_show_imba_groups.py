from pathlib import Path
s=Path('agent_notes/lbc_other_imba.md').read_text(encoding='utf-8')
for i,l in enumerate(s.splitlines(),1):
 if l.startswith('### ') and ('Rune' in l or 'рун' in l.lower() or 'Области' in l): print(i,l)
