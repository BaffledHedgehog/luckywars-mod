from pathlib import Path
p=Path('agent_notes/lbc_other_world_tech.md'); s=p.read_text(encoding='utf8')
block='''
## Nexus: weapon dispatcher и cooldown

`other/nexus/rc` — dispatcher: он сначала сбрасывает `RC_Raycast` в 0, затем по одному зажатому `custom_data`-ключу вызывает roadgun, corrupter, orb gun или pulsar. Для orb gun `pre` начинает с `Distance=0`, ставит тег `caster` и выбирает ровно одну из двух веток: при sneak запускает разбор предметов лучом, иначе стреляет; после ветки снимает тег caster. Pulsar также начинает луч из глаз игрока и ставит `pulsar_cd=15`. Ежесекундный `tick_1s_player` уменьшает cooldown у roadgun, corrupter и pulsar, то есть 15 — это 15 секунд; для каждого предмета отдельная функция задаёт свой лимит и сброс. [Dispatcher](../world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rc.mcfunction), [orb gun](../world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/pre.mcfunction), [pulsar](../world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/pulsar/pre.mcfunction), [секундное обслуживание cooldown](../world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tick_1s_player.mcfunction).
'''
if '## Nexus: weapon dispatcher и cooldown' not in s:
 i=s.index('\n## Файлы:'); s=s[:i]+'\n'+block+s[i:]
p.write_text(s,encoding='utf8')
