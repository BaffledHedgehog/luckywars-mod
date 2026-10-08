from pathlib import Path
p=Path('agent_notes/lbc_other_imba.md'); s=p.read_text(encoding='utf-8')
s=s.replace('216 `fill replace` команд (215 уникальных block IDs; `nether_bricks` указан дважды)', '216 `fill replace` команд: 215 block IDs и дополнительная селекторная палитра `#minecraft:leaves`, которая суммируется в выход `stick`')
s=s.replace('217 give-команд (216 разных предметных ID; `bamboo_mosaic` выдаётся двумя командами) возвращают соответствующие предметы, включая дополнительный `stick` и специальную выдачу для грибных Lucky Blocks.', '217 give-команд покрывают216 разных item ID (`bamboo_mosaic` указан дважды); счётчик всех листьев возвращается как `stick`, а три грибных блока получают отдельные имена/lore/model при выдаче.')
s=s.replace('217 `give @s`-команд: 216 уникальных item IDs для 215 уникальных fill targets плюс дополнительный `stick`; `bamboo_mosaic` встречается дважды в выдаче.', '217 `give @s`-команд: 216 уникальных item IDs; `bamboo_mosaic` встречается дважды. Palette состоит из215 конкретных block IDs и отдельного `#minecraft:leaves`, которое конвертируется в счетчик `stick`.')
s=s.replace('два специально оформленных Lucky Block предмета из грибных блоков', 'три специально оформленных Lucky Block предмета из red/brown mushroom blocks и mushroom stem')
t=p.with_suffix('.md.tmp'); t.write_text(s,encoding='utf-8',newline='\n'); t.replace(p)
