from pathlib import Path
files={
'agent_notes/lbc_other_events.md':{
'### `lbc:other/bucks/rc`':'Snowball с `custom_data.bucks:1` запускает генератор: он дважды выдаёт `lbc:imba/random_bucks`, очищает и заново ставит жёлтый shulker box в координатах (10241024,177,10241024), затем создаёт рядом постоянного жителя luck_trader с 10 заглушками-торговлями. Следующая функция вызывается только для ближайшего ещё не отмеченного villager с тегом luck_trader в радиусе 0,01; после настройки ему ставят `spawned`, поэтому повторная инициализация не проходит. [Источник](../world/datapacksdisabled/LBC/data/lbc/function/other/bucks/rc.mcfunction)',
'### `lbc:other/bucks/gen_trades`':'Для каждого из 10 рецептов функция по очереди кладёт `lbc:random_glack` и `lbc:imba/random_bucks` в слоты 0 и 1 временного shulker box, читает созданные предметы и записывает пару в Offers.Recipes нужного торгового слота. Рецепты 0–6 покупают предмет из слота 1 и продают предмет из 0; последние 3 направлены наоборот. Таким образом, реальные цены/товары выбираются loot tables при генерации, а не задаются фиксированным списком в самой функции. [Источник](../world/datapacksdisabled/LBC/data/lbc/function/other/bucks/gen_trades.mcfunction)',
'### `lbc:other/grimoire_dungeon/room_reward_random`':'После того как `room_reward` находит воздух в точке награды, эта функция выбирает равномерное целое 1–12 и вызывает ровно соответствующую ветку: мини-босс; обычный или grimoire-сундук; гнездо ежей; скелеты, зомби, pillagers, creepers или breezes; lucky-мобы; lucky-блоки; шахта. Затем исходный marker удаляется. Это выбор одного варианта, а не последовательный запуск всех двенадцати помощников. [Источник](../world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_random.mcfunction)',
'### `lbc:other/grimoire_dungeon/room_reward`':'Группа вызывается из dungeon raycast по marker-тегу `room_reward`. В пустом блоке под точкой запускается случайная награда (одна из 12 веток); если там barrier и игрок не в spectator находится в пределах 8 блоков, запускается подготовка босса. Оба условия независимые `execute if`, поэтому нельзя трактовать вторую ветку как альтернативный `else`; фактические последствия зависят от блока и присутствия игрока. [Источник](../world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward.mcfunction)'
}}
for fn, edits in files.items():
 p=Path(fn); s=p.read_text(encoding='utf8')
 for heading, paragraph in edits.items():
  if heading not in s: print('MISSING',heading); continue
  start=s.index(heading)+len(heading); end=s.find('\n### ',start)
  if end<0: end=s.find('\n## ',start)
  if end<0: end=len(s)
  s=s[:start]+'\n\n'+paragraph+s[end:]
 p.write_text(s,encoding='utf8')
