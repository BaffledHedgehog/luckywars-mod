from pathlib import Path
p=Path('agent_notes/lbc_other_world_tech.md'); s=p.read_text(encoding='utf8')
needle='[перестройка круга](../world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/start.mcfunction), [частицы на wisps]'
repl='[fire-формула](../world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/fire/count.mcfunction), [water-формула](../world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/water/count.mcfunction), [nature-порог](../world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/nature/count.mcfunction), [nature roll и семь outcomes](../world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/nature/give_buff.mcfunction), [light roll](../world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/light/run.mcfunction), [перестройка круга](../world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/start.mcfunction), [частицы на wisps]'
s=s.replace(needle,repl)
p.write_text(s,encoding='utf8')
