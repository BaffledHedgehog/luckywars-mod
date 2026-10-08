"""Attach every inventoried NBT file to its exact maps documentation section."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[2]
inventory=json.loads((root/'agent_notes/inventory/files.json').read_text(encoding='utf-8'))
rows=[]
for source in sorted(x['source'] for x in inventory if x['source'].endswith('.nbt') and
                     (x['source'].startswith('world/datapacksdisabled/maps/') or
                      x['source'].startswith('world/datapacksdisabled/LBC/data/lbc/structure/'))):
    is_map=source.startswith('world/datapacksdisabled/maps/')
    rows.append({'source':source,'document':'agent_notes/maps_catalog.md',
      'section':'Карты SkyWars: `maps/data/swrg/structure`' if is_map else 'Шаблоны LBC: `LBC/data/lbc/structure`','status':'documented',
      'note':'Содержимое, размер файла, SHA-256 и декодированные NBT-поля перечислены по этому пути в inventory/maps_structures.json.'})
pack_source='world/datapacksdisabled/maps/pack.mcmeta'
if any(x['source']==pack_source for x in inventory):
    rows.append({'source':pack_source,'document':'agent_notes/maps_structures.md','section':'Метаданные maps pack',
      'status':'documented','note':'pack_format 81 и описание пакета зафиксированы; функций в maps нет, шаблоны размещаются внешним maploader.'})
out=root/'agent_notes/coverage/maps_catalogue.json'
out.write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'{len(rows)} coverage records')
