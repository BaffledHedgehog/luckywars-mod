import collections, json
from pathlib import Path
x=json.loads((Path(__file__).resolve().parents[1]/'inventory/maps_structures.json').read_text(encoding='utf-8'))
ids=collections.Counter(); tags=collections.Counter(); commands=[]; loot=collections.Counter(); item_stacks=0; custom=[]
for row in x['structures']:
    for ent in row['entities']:
        n=ent.get('nbt',{}).get('nbt',ent.get('nbt',{})); ids[n.get('id','?')]+=1; tags.update(n.get('Tags',[]))
    for b in row['markers']:
        n=b['nbt']
        if 'Command' in n: commands.append((row['source'],b['pos'],n.get('Command')))
        if 'LootTable' in n: loot[n['LootTable']]+=1
        if 'Items' in n and n['Items']: item_stacks+=len(n['Items'])
        if 'custom_data' in json.dumps(n): custom.append((row['source'],b['pos']))
print('entity ids',ids.most_common(20)); print('entity tags',tags.most_common(50))
print('command blocks',len(commands)); print('command examples',commands[:20])
print('loot tables',loot.most_common(50)); print('nonempty item stacks',item_stacks,'custom_data block records',len(custom),custom[:20])
