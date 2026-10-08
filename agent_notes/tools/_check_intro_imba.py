from pathlib import Path
for i,line in enumerate(Path('agent_notes/lbc_other_imba.md').read_text(encoding='utf-8').splitlines(),1):
 if 'машинный' in line or line.startswith('Эта часть сводит'): print(i,repr(line))
