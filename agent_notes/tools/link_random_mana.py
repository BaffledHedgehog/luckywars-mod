from pathlib import Path
p=Path('agent_notes/lbc_other_consumables_craft.md'); s=p.read_text(encoding='utf8')
start=s.index('### Рецепт с 64 исходами: `crafted_mana` и `crafted_mana_inv`')
end=s.index('\n### Неодинаковый расход:',start)
new='''### Рецепт `crafted_mana` и `crafted_mana_inv`

Обе функции делают равновероятный выбор одного из 64 исходов. Полные таблица эффектов/scoreboard изменений, ground/inventory callers, проверок предметов и расхода входов вынесены в отдельный ручной разбор [случайного mana-рецепта](lbc_craft_random_mana.md). Здесь оставлена только ссылка, чтобы не дублировать неполную таблицу.
'''
s=s[:start]+new+s[end:]
s=s.replace('`stfire_tiny:1` + any second item entity/slot (ground path additionally requires NBT `Thrower`)','`stfire_tiny:1` плюс предметный вход согласно caller; подробные условия и расход см. [разбор 64 исходов](lbc_craft_random_mana.md)')
p.write_text(s,encoding='utf8')
