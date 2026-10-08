"""Add convenient entity id/tag columns to a catalogue built before schema v1.1."""
import json
from pathlib import Path
path=Path(__file__).resolve().parents[1]/'inventory/maps_structures.json'
data=json.loads(path.read_text(encoding='utf-8'))
for row in data['structures']:
    for entity in row.get('entities',[]):
        wrapper=entity.get('raw',entity.get('nbt',{}))
        actual=wrapper.get('nbt',wrapper)
        entity['id']=actual.get('id')
        entity['tags']=actual.get('Tags',[])
        entity['blockPos']=wrapper.get('blockPos')
        entity['raw']=wrapper
        entity['nbt']=actual
path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f"enriched entities in {len(data['structures'])} structures")
