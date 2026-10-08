from pathlib import Path
p=Path('agent_notes/lbc_other_imba.md'); s=p.read_text(encoding='utf-8')
s=s.replace('| 5 | Выдаёт loot `lbc:imba/nahida_dakimakura`, сообщение, расход одного `cunny:5`. |', '| 5 | Выдаёт loot `lbc:imba/nahida_dakimakura`, сообщает и тратит slab. Если затем использовать item с `nahida_dakimakura:1` вне Nexus/imprinted, advancement `use_nahida` запускает временный `plapper` цикл и один из26 attribute-up outcomes; стоп handler снимает четыре ×0 debuff modifiers, когда advancement стал false. |')
s=s.replace('| 4 | Вызвает cultist helper ровно20 раз на 16 уникальных позициях радиуса2: 8 соседей на радиусе1, 4 диагонали радиуса1, затем 4 cardinal позиции радиуса2, каждая вызвана дважды. Каждый helper делает равновероятный бросок1..6 и создаёт одного из шести описанных ниже cultist; расходует предмет. |', '| 4 | Вызывает cultist helper ровно20 раз на 16 уникальных позициях радиуса2: 8 соседей на радиусе1, 4 диагонали радиуса1, затем 4 cardinal позиции радиуса2, каждая вызвана дважды. Каждый helper делает равновероятный бросок1..6; если исполнитель в team1..32, моб наследует его team, `lbcID2` и team_number, затем `summactivated`; при активном trinket handler запускается `on_summon`. |')
needle='''Шанс/геометрия в пунктах 4 и 21 — это повторные команды в исходниках, не «первые несколько» результатов. Функции с точными условиями и всеми SNBT: [выбор Cunny](../world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction) и [cultist template](../world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/4_spawn_cunny_spawn.mcfunction).'''
insert=needle+'''

`cunnys/4_spawn_cunny_spawn` выбирает шесть непересекающихся 1/6 ветвей; все summon требуют PersistenceRequired, имеют пустую таблицу смерти и экипировку с drop chance0. Отличия, значимые для боя:

| random | Моб / tag | HP и оружие | Особые атрибуты |
|---:|---|---|---|
| 1 | wither skeleton `strangeness` | 30; bow | follow_range100, scale0.7, fall damage0 |
| 2 | skeleton `chaos` | 50; heart of the sea | follow_range100, scale0.55, fall damage0 |
| 3 | skeleton `luck`,`klee` | 40; totem | follow_range100, knockback resistance10, scale0.65, explosion knockback resistance100, fall damage0; boots blast protection99 |
| 4 | skeleton `omnipresence` | 40; bow | follow_range100, scale0.65, fall damage0 |
| 5 | skeleton `obsession` | 10; diamond axe | follow_range100, attack damage11, speed0.25, scale0.6, fall damage0 |
| 6 | zombie `hypocrisy` | 60; two iron swords | follow_range100, speed0.2, scale0.6, fall damage0 |

[Helper с подбором команды/score и вызовом on_summon](../world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/4_spawn_cunny.mcfunction). Item `nahida_dakimakura` после выдачи имеет самостоятельный use-advancement: [trigger](../world/datapacksdisabled/LBC/data/lbc/advancement/use_nahida.json), [handler](../world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/5_plapping.mcfunction), [stop](../world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/5_plapping_stop.mcfunction).'''
if needle not in s: raise SystemExit('cunny note not found')
s=s.replace(needle,insert)
t=p.with_suffix('.md.tmp'); t.write_text(s,encoding='utf-8',newline='\n'); t.replace(p)
