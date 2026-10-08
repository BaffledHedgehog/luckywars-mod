#!/usr/bin/env python3
"""Print concise structural facts for selected NBT samples."""
import collections, json, sys
from maps_nbt import load
for filename in sys.argv[1:]:
    _, root=load(filename)
    palette=root.get('palette',[]); blocks=root.get('blocks',[])
    counts=collections.Counter(palette[b.get('state',-1)].get('Name','?') for b in blocks if isinstance(b.get('state'),int) and 0<=b['state']<len(palette))
    tile=[]
    for b in blocks:
        if b.get('nbt'):
            i=b.get('state',-1); tile.append({'pos':b.get('pos'),'block':palette[i].get('Name','?') if 0<=i<len(palette) else '?','nbt':b['nbt']})
    print(json.dumps({'source':filename,'size':root.get('size'),'DataVersion':root.get('DataVersion'),'block_entries':len(blocks),'palette_size':len(palette),'block_counts':counts.most_common(15),'entity_count':len(root.get('entities',[])),'block_entities':tile[:20]},ensure_ascii=False))
