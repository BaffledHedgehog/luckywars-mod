from pathlib import Path
p=Path('agent_notes/lbc_other_magic.md'); s=p.read_text(encoding='utf8')
edits={
'### `lbc:other/grimoire_dungeon/room_reward_random`':'После того как `room_reward` находит воздух в точке награды, эта функция выбирает равномерное целое 1–12 и вызывает ровно соответствующую ветку: мини-босс; обычный или grimoire-сундук; гнездо ежей; скелеты, зомби, pillagers, creepers или breezes; lucky-мобы; lucky-блоки; шахта. Затем исходный marker удаляется. Это выбор одного варианта, а не последовательный запуск всех двенадцати помощников. [Источник](../world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_random.mcfunction)',
'### `lbc:other/grimoire_dungeon/room_reward`':'Группа вызывается из dungeon raycast по marker-тегу `room_reward`. В пустом блоке под точкой запускается случайная награда (одна из 12 веток); если там barrier и игрок не в spectator находится в пределах 8 блоков, запускается подготовка босса. Условия оформлены двумя независимыми `execute if`, а не конструкцией `else`. [Источник](../world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward.mcfunction)'
}
for h,t in edits.items():
 a=s.index(h)+len(h); b=s.find('\n### ',a); b=b if b>=0 else s.find('\n## ',a); b=b if b>=0 else len(s); s=s[:a]+'\n\n'+t+s[b:]
p.write_text(s,encoding='utf8')
