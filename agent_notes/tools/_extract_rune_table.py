from pathlib import Path
s=Path('agent_notes/lbc_other_imba.md').read_text(encoding='utf-8')
i=s.index('### Рунные предметы'); j=s.index('\n### Области',i); print(s[i:j])
