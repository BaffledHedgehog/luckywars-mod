from pathlib import Path
p=Path('agent_notes/lbc_other_imba.md')
s=p.read_text(encoding='utf-8').replace('[все цветовые helpers](../world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors)', '[цветовой helper, например yellow](../world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/yellow.mcfunction)')
p2=p.with_suffix('.md.tmp'); p2.write_text(s,encoding='utf-8',newline='\n'); p2.replace(p)
