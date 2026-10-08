from pathlib import Path
s=Path('agent_notes/lbc_other_misc.md').read_text(encoding='utf8'); a=s.index('## Freeze block:'); print(s[a:a+700])
