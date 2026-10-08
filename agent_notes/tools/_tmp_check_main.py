from pathlib import Path
s=Path('agent_notes/lbc_other_main.md').read_text(encoding='utf8')
for bad in ['пустая функция','команд нет','служебная проверка','не видно','эффектов не видно','передаёт выполнение']:
 print(bad, s.lower().count(bad))
