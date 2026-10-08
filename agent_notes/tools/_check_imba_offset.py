from pathlib import Path
s=Path('agent_notes/lbc_other_imba.md').read_text(encoding='utf-8')
for i,line in enumerate(s.splitlines(),1):
 if 'correction' in line.lower() or 'коррект' in line.lower(): print(i, line[:320])
