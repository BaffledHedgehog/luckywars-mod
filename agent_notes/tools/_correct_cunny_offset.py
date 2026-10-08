from pathlib import Path
p=Path('agent_notes/lbc_other_imba.md'); s=p.read_text(encoding='utf-8')
s=s.replace('`cunny_slab/tick` выбирает correction marker; `correction_cunny` смещается на 10 блоков и удаляет marker после работы.', '`cunny_slab/tick` выбирает correction marker; `correction_cunny` преобразует X/Z каждого marker-а в целые координаты, затем добавляет независимо выбранное смещение−10..10 по каждой оси (Y остаётся прежним) и удаляет marker после работы.')
t=p.with_suffix('.md.tmp'); t.write_text(s,encoding='utf-8',newline='\n'); t.replace(p)
