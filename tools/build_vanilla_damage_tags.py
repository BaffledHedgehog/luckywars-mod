"""Freeze unmodified Minecraft damage data for the runtime true-legacy projection."""
from pathlib import Path
import argparse,hashlib,json,zipfile

def build(jar,output):
    with zipfile.ZipFile(jar) as z:
        tags={"minecraft:"+n.removeprefix('data/minecraft/tags/damage_type/').removesuffix('.json'):json.loads(z.read(n))
              for n in z.namelist() if n.startswith('data/minecraft/tags/damage_type/') and n.endswith('.json')}
        types={"minecraft:"+n.removeprefix('data/minecraft/damage_type/').removesuffix('.json'):json.loads(z.read(n))
               for n in z.namelist() if n.startswith('data/minecraft/damage_type/') and n.endswith('.json')}
    resolved={}
    def resolve(name,ancestry=()):
        if name in resolved:return resolved[name]
        if name in ancestry:raise ValueError('Cyclic vanilla damage tag '+name)
        result=set()
        for value in tags[name]['values']:
            if isinstance(value,dict):value=value['id']
            if value.startswith('#'):result.update(resolve(value[1:],(*ancestry,name)))
            else:
                if ':' not in value:value='minecraft:'+value
                if value not in types:raise ValueError('Missing vanilla damage type '+value)
                result.add(value)
        resolved[name]=sorted(result);return result
    for name in tags:resolve(name)
    with jar.open('rb') as stream:digest=hashlib.file_digest(stream,'sha256').hexdigest()
    data={'minecraftVersion':'26.3','sourceJarSha256':digest,'tags':dict(sorted(resolved.items())),'damageTypes':dict(sorted(types.items()))}
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    print(json.dumps({'tags':len(tags),'types':len(types),'output':str(output)}))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('jar',type=Path)
    parser.add_argument('--output',type=Path,default=Path('src/main/resources/skywars2/vanilla_damage_26_3.json'))
    args=parser.parse_args();build(args.jar,args.output)
