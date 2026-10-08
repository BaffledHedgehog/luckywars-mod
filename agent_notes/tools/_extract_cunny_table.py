from pathlib import Path
s=Path('agent_notes/lbc_other_imba.md').read_text(encoding='utf-8')
i=s.index('| ID | Реальное действие после выбора |')
j=s.index('\n### Руны',i) if '\n### Руны' in s[i:] else s.index('\n### Основные руны',i) if '\n### Основные руны' in s[i:] else s.index('\n###',i+3)
print(s[i:j])
