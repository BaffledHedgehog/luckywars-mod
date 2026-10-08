from pathlib import Path
p=Path('agent_notes/lbc_other_imba.md')
s=p.read_text(encoding='utf-8')
for token in ['### `lbc:other/imba/cunny_slab/pasta/1`','### `lbc:other/imba/cunny_slab/pasta/10`','### `lbc:other/imba/cunny_slab/pasta/100`']:
 i=s.find(token); print('\n',repr(s[i:i+600]))
