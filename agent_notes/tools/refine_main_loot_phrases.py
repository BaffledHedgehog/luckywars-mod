from pathlib import Path
import re
p=Path('agent_notes/lbc_other_main.md'); s=p.read_text(encoding='utf8')
for m in list(re.finditer(r'### `lbc:other/([^`]+)`\n\n\*\*Фактическая работа\.\*\* ([^\n]*)',s)):
 name=m.group(1); src=Path('world/datapacksdisabled/LBC/data/lbc/function/other')/(name+'.mcfunction')
 try: raw=src.read_text(encoding='utf8')
 except: continue
 if 'выдаёт/создаёт предметы или loot' not in m.group(2): continue
 modes=[]
 for line in raw.splitlines():
  q=re.search(r'\bloot (give|spawn|replace) .*?\bloot (lbc:[a-z0-9_./-]+)',line)
  if q: modes.append((q.group(1),q.group(2)))
  q=re.search(r'\bitem replace (entity|block) .*? with (minecraft:[a-z0-9_./-]+)',line)
  if q:modes.append(('item',q.group(2)))
 verbs={'spawn':'создаёт предметы на месте выполнения из loot table','give':'выдаёт игроку результат loot table','replace':'заполняет указанное место результатом loot table','item':'создаёт или заменяет item-стек'}
 if not modes: continue
 phrase='; '.join(f'{verbs[a]} `{b}`' for a,b in dict.fromkeys(modes))
 start=m.start(2); end=m.end(2); line=m.group(2)
 line=re.sub(r'выдаёт/создаёт предметы или loot `lbc:[^`]+`',phrase,line)
 s=s[:start]+line+s[end:]
p.write_text(s,encoding='utf8')
