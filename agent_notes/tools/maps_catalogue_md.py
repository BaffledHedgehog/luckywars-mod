"""Render a compact, linked row for every analyzed structure."""
import collections,json
from pathlib import Path
root=Path(__file__).resolve().parents[2]
notes=root/'agent_notes'
catalog=json.loads((notes/'inventory/maps_structures.json').read_text(encoding='utf-8'))
containers={'minecraft:chest','minecraft:trapped_chest','minecraft:barrel','minecraft:hopper','minecraft:dispenser','minecraft:dropper','minecraft:furnace','minecraft:blast_furnace','minecraft:smoker','minecraft:brewing_stand','minecraft:shulker_box'}
for color in ('white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black'):
    containers.add('minecraft:'+color+'_shulker_box')
def compact(row):
    c=row.get('block_counts',{})
    nonair=row.get('block_entries',0)-c.get('minecraft:air',0)
    ents=row.get('entities',[])
    tags=collections.Counter(t for e in ents if e.get('id')=='minecraft:marker' for t in e.get('tags',[]))
    if not tags: tags=collections.Counter(t for e in ents for t in e.get('tags',[]))
    tagtext=', '.join(f'{t}×{n}' for t,n in tags.most_common(4)) or '—'
    if len(tags)>4: tagtext+=f', +{len(tags)-4}'
    ct=collections.Counter({k:v for k,v in c.items() if k in containers and v})
    containertext=', '.join(f'{k.removeprefix("minecraft:")}×{v}' for k,v in ct.most_common()) or '—'
    loot=collections.Counter(m['nbt']['LootTable'] for m in row.get('markers',[]) if m.get('nbt',{}).get('LootTable'))
    loottext=', '.join(f'{k}×{v}' for k,v in loot.most_common(3)) or '—'
    if len(loot)>3: loottext+=f', +{len(loot)-3}'
    cmd=sum('Command' in m.get('nbt',{}) for m in row.get('markers',[]))
    if cmd: containertext+=f'; command blocks×{cmd}'
    if row['group']=='maps':
        callers=sorted({c['function'] for c in row.get('maploader_calls',[])})
    else:
        callers=sorted({c['function'] for c in row.get('lbc_callers',[])})
    callerlinks=[]
    for f in callers:
        display=f.split('/function/',1)[1] if '/function/' in f else f
        callerlinks.append(f'[{display}](../{f})')
    callertext=', '.join(callerlinks) or 'не найден'
    label=row['source'].split('/structure/',1)[1]
    link='../'+row['source']
    return f"| [{label}]({link}) | {'×'.join(map(str,row.get('size') or []))} | {nonair} | {tagtext} | {containertext} | {loottext} | {callertext} |"
unlinked=[Path(s).name for s in catalog.get('maploader_summary',{}).get('map_files_without_literal_calls',[])]
lbc_without=[]
for row in catalog['structures']:
    if row['group']=='lbc' and not row.get('lbc_callers'):
        lbc_without.append(Path(row['source'].split('/structure/',1)[1]).with_suffix('').as_posix())
entities=sum(row.get('entity_entries',0) for row in catalog['structures'])
markers=sum(1 for row in catalog['structures'] for e in row.get('entities',[]) if e.get('id')=='minecraft:marker')
be=sum(len(row.get('markers',[])) for row in catalog['structures'])
cmd=sum('Command' in m.get('nbt',{}) for row in catalog['structures'] for m in row.get('markers',[]))
loot_count=sum(bool(m.get('nbt',{}).get('LootTable')) for row in catalog['structures'] for m in row.get('markers',[]))
summary=catalog.get('maploader_summary',{})
function_status=collections.Counter(x['status'] for x in summary.get('functions',[]))
family_status=collections.Counter(x['status'] for x in summary.get('families',[]))
lines=['# Каталог структур: краткий указатель','',
       'Одна строка на каждый NBT. Число блоков исключает `minecraft:air`; ID и теги сущностей, контейнеры, loot tables и прямые вызовы взяты из NBT и datapack. Полный NBT-разбор, палитры и SHA-256 — в [машинном каталоге](inventory/maps_structures.json).',
       '',f"В 1456 структурах найдено {entities} сущностей (из них {markers} `minecraft:marker`), {be} значимых записей block-entity NBT, включая {cmd} командных блоков и {loot_count} полей `LootTable`.",
       f"Для карт найдено буквальное соответствие ID в maploader у {catalog.get('maploader_summary',{}).get('linked_map_files',0)}/1296 файлов. Без такого вызова остались: {', '.join(f'`{n}`' for n in unlinked)}.",
       f"Среди {len(summary.get('functions',[]))} функций maploader: {function_status['complete']} с полным совпадением всех статических ID, {function_status['partial']} с частичным и {function_status['zero']} без совпадений. После объединения normal/broken-вариантов по имени семейства: {family_status['complete']} полных, {family_status['partial']} частичных, {family_status['zero']} с нулём совпадений. Полнота означает только, что буквальные ID найдены среди 1296 NBT.",
       f"Из {catalog.get('maploader_summary',{}).get('template_ids',0)} буквальных `swrg:` ID {len(catalog.get('maploader_summary',{}).get('referenced_template_ids_without_nbt',[]))} не имеют одноимённого NBT в трёх исследуемых datapack; это не доказывает их отсутствия в `world/generated` или во время работы. Полный список хранится в JSON.",
       f"У LBC прямые callers найдены для {catalog.get('lbc_summary',{}).get('linked_structure_files',0)}/160 NBT. У {len(lbc_without)} NBT буквального caller нет; кроме того, {len(catalog.get('lbc_summary',{}).get('referenced_ids_without_snapshot_nbt',[]))} разных literal LBC ID не имеют одноимённого NBT в трёх исследуемых datapack. Полные списки сохранены в JSON; это не утверждение об отсутствии структур в сохранениях мира или во время работы.",
       '', '## Карты SkyWars: `maps/data/swrg/structure`','',
       '| Файл / ID | size | блоков без воздуха | теги marker (частые) | контейнеры / особые блоки | LootTable | вызывающие функции maploader |',
       '|---|---:|---:|---|---|---|---|']
for row in catalog['structures']:
    if row['group']=='maps': lines.append(compact(row))
lines+=['','## Шаблоны LBC: `LBC/data/lbc/structure`','',
        '| Файл / ID | size | блоков без воздуха | теги marker (частые) | контейнеры / особые блоки | LootTable | прямые функции вызова |',
        '|---|---:|---:|---|---|---|---|']
for row in catalog['structures']:
    if row['group']=='lbc': lines.append(compact(row))
status_name={'complete':'полная по снимку NBT','partial':'частичная по снимку NBT','zero':'ноль совпадений NBT'}
lines+=['','## Сверка функций maploader','',
        'Статус относится только к буквальным ID `place template swrg:<id>` и наличию одноимённого NBT среди исследованных 1296 файлов: «полная» — все ID найдены; «частичная» — найдена часть; «ноль» — совпадений нет. Это не предполагает заранее известный ожидаемый состав карты. Числа и макросы не попали в ID-разбор; динамических/неразобранных вызовов: '+str(len(summary.get('dynamic_or_unparsed_calls',[])))+'.',
        '',f"В снимке найдено {summary.get('literal_calls',0)} вызовов, {summary.get('template_ids',0)} уникальных ID, из них {len(summary.get('referenced_template_ids_without_nbt',[]))} не найдены среди NBT. Полный список этих ID и построчные вызовы сохранены в JSON.",
        '', '| Семейство функции | Файлы функции | найдено / ID | статус | нет NBT (число; до 6 примеров) |',
        '|---|---|---:|---|---|']
for family in summary.get('families',[]):
    paths=[]
    for f in family['functions']:
        display=f.split('/map/',1)[-1] if '/map/' in f else Path(f).name
        paths.append(f'[{display}](../{f})')
    missing=family['unmatched_nbt_ids']
    miss=(str(len(missing))+': '+', '.join(f'`{x}`' for x in missing[:6])+('…' if len(missing)>6 else '')) if missing else '—'
    lines.append(f"| `{family['family']}` | {', '.join(paths)} | {family['matched_nbt_ids']} / {family['unique_template_ids']} | {status_name[family['status']]} | {miss} |")
lines+=['','### Отдельные файлы функций','',
        '| Функция | семейство | вызовы / ID | статус | нет NBT (число; до 6 примеров) |',
        '|---|---|---:|---|---|']
for item in summary.get('functions',[]):
    display=item['function'].split('/map/',1)[-1] if '/map/' in item['function'] else Path(item['function']).name
    missing=item['unmatched_nbt_ids']
    miss=(str(len(missing))+': '+', '.join(f'`{x}`' for x in missing[:6])+('…' if len(missing)>6 else '')) if missing else '—'
    lines.append(f"| [{display}](../{item['function']}) | `{item['family']}` | {item['matched_nbt_ids']} / {item['unique_template_ids']} | {status_name[item['status']]} | {miss} |")
out=notes/'maps_catalog.md'; out.write_text('\n'.join(lines)+'\n',encoding='utf-8')
print(f"wrote {out} with {sum(r['group']=='maps' for r in catalog['structures'])} map and {sum(r['group']=='lbc' for r in catalog['structures'])} LBC rows")
