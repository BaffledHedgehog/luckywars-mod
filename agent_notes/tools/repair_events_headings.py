from pathlib import Path
p=Path(__file__).resolve().parents[1]/'lbc_other_events.md'
s=p.read_text(encoding='utf-8')
for prefix,name in [
 ('Находит игрока с совпадающим lbcID2','jack_o_lantern_check'),
 ('Создаёт короткоживущую ракету-фейерверк','jack_o_lantern_explode'),
 ('В каждом тике передвигает луч','jack_o_lantern_tick'),
 ('Выбирает случайное целое 1..7 и через','screamer_activate')]:
 heading=f'### `lbc:other/event/halloween/{name}`'
 if heading not in s:
  assert s.count(prefix)==1
  s=s.replace(prefix,heading+'\n\n'+prefix,1)
p.write_text(s,encoding='utf-8')
print('four source headings restored')
