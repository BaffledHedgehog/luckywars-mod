"""Full-command checks of repeated Nuke placements and Magnum shots."""
from pathlib import Path
import hashlib
import json
import sys

ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/'world/datapacksdisabled/LBC/data/lbc/function/other'
sys.path.insert(0,str(ROOT/'agent_notes/tools'))
import maps_nbt

def commands(path):
    return [line.strip() for line in path.read_text(encoding='utf8').splitlines() if line.strip() and not line.lstrip().startswith('#')]

def templates(distance, integrity, angles):
    return [f'execute rotated ~{angle if angle else ""} 0 positioned ^ ^ ^{distance} run place template lbc:air ~-24 ~-24 ~-24 none none {integrity}' for angle in angles]

def creepers(radius):
    rows=[]
    for offset in [39,18]:
        for height in [0,40,-40]:
            y='~'+(str(height) if height else '')
            for x,z in [(offset,0),(-offset,0),(0,offset),(0,-offset)]:
                coords=f'~{x if x else ""} {y} ~{z if z else ""}'
                payload='{PersistenceRequired:1b,ExplosionRadius:'+str(radius)+'b,Fuse:1,ignited:1b,NoGravity:1b,Invulnerable:1b,CustomNameVisible:0b,CustomName:{"italic":false,"translate":"killed_by_nuke"}}'
                rows.append('summon minecraft:creeper '+coords+' '+payload)
    return rows

kill='kill @e[type=!minecraft:creeper,type=!minecraft:player,distance=..14,tag=!slowparticlesexplode_2,tag=!strict_map_object]'
effect='effect give @e[distance=..45,tag=!spectator] minecraft:wither 20 30 true'
angles=[0,60,120,180,240,300]
normal=['summon minecraft:marker ~ ~ ~ {Tags:["slowraycast","slowparticlesexplode_2","player","magic"],data:{lifetime:400}}']
normal+=templates(25,'0.5',angles)+templates(60,'0.25',angles)+[effect]+creepers(25)
normal+=['stopsound @a master minecraft:lbcsounds.nuke',kill]
bruh=templates(25,'0.5',angles)+templates(60,'0.25',[0,30,90,150,210,270,330])+[effect]+creepers(16)+[kill]
assert commands(BASE/'nuke/nuke1_at.mcfunction')==normal
assert commands(BASE/'nuke/nuke1_at_bruh_2.mcfunction')==bruh

shots={}
for name,cooldown,recoil in [('standard',25,3),('incendiary',25,2),('electric',15,2),('acid',20,2),('void',15,1),('explosive',40,3)]:
    folder=BASE/'techno/magnum/shoot'/name
    loop=[f'function lbc:other/techno/magnum/shoot/{name}/check',
          f'execute if score @s Distance matches ..400 positioned ^ ^ ^.5 unless entity @e[type=minecraft:marker,distance=..20,tag=stopper,limit=1] run function lbc:other/techno/magnum/shoot/{name}/loop',
          'execute if score @s Distance matches 401.. run tag @e[tag=hit] remove hit']
    assert commands(folder/'loop.mcfunction')==loop,name
    cast=commands(folder/'cast.mcfunction')
    gameplay=[line for line in cast if not line.startswith(('playsound ','particle '))]
    expected=[f'function lbc:other/techno/magnum/shoot/{name}/loop',
              f'execute at @s run tp @s ~ ~ ~ ~ ~-{recoil}',
              f'clear @s[scores={{coldownMagnum=..0}}] *[minecraft:custom_data~{{{name}_round:1}}] 1',
              'scoreboard players set @s[scores={coldownMagnum=..0}] MagnumNyauwu 4',
              f'scoreboard players set @s coldownMagnum {cooldown}',
              'advancement grant @s only lbc:true_advancements/shooter/magnum']
    assert gameplay==expected,name
    shots[name]={'cooldown':cooldown,'recoil_degrees':recoil,'all_cast_commands':cast,'complete_loop_commands':loop}

sizes={}
for name,expected in [('white_concrete_7x7',[7,7,7]),('white_concrete_pad_5x1x5',[5,1,5])]:
    p=ROOT/'world/datapacksdisabled/LBC/data/lbc/structure'/(name+'.nbt')
    _,nbt=maps_nbt.load(p)
    assert nbt['size']==expected
    assert nbt['palette']==[{'Name':'minecraft:white_concrete'}]
    sizes[name]={'size':nbt['size'],'blocks':len(nbt['blocks']),'entities':len(nbt['entities']),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
result={'nuke_complete_command_counts':{'normal':len(normal),'bruh':len(bruh)},
        'creepers_each':24,'magnum_shots':shots,'roadgun_structures':sizes,'errors':[]}
(ROOT/'agent_notes/inventory/worldtech_patterns_audit.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps({'nuke_commands':result['nuke_complete_command_counts'],'magnum_variants':6,'nbt_structures':2,'errors':0}))
