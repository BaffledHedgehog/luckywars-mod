from pathlib import Path
s=Path('agent_notes/lbc_other_imba.md').read_text(encoding='utf-8')
for line in s.splitlines():
 if line.startswith('Macro uses') or '217 `give @s`' in line or '216 уникальных item IDs;' in line:
  print(line)
