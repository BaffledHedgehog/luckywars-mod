"""Source-exact Ender/Bad Wiki leaves and effect tables; author RP is read only."""
from pathlib import Path
import json,re
from import_lbc_loadouts import ROOT,SOURCE
from import_lbc_wands import import_wand_family

def main():
 dest=ROOT/'src/main/resources/data/lbc/loot_table/ported'
 for name,flag in [('ender_wand','enderwand'),('bad_wiki','badwiki')]:
  import_wand_family(SOURCE/'loot_table/transformables_staff.json',dest/f'{name}.json',dest/f'academy_{name}.json',name,f'skywars2:{name}',f'skywars2:academy_{name}',data_marker=flag+':1',academy_tag_fields=('upgradable:0','upgrade_available:1','upgraded:1','mode:1','tier:1'))
 lines=(SOURCE/'function/workingitems/badwiki.mcfunction').read_text(encoding='utf-8-sig')
 random=[]
 for index,effect,seconds,amplifier,hidden in re.findall(r'#random84 lbc.math matches (\d+) run effect give @s minecraft:(\w+) (\d+) (\d+)( true)?',lines):
  random.append(dict(index=int(index),effect=effect,ticks=int(seconds)*(1 if effect in ('instant_damage','instant_health','saturation') else 20),amplifier=int(amplifier),hidden=bool(hidden)))
 assert [r['index'] for r in random]==list(range(1,85))
 upgraded=[]
 for mode,effect,seconds,hidden in re.findall(r'#mode lbc.math matches (\d+) run effect give @s minecraft:(\w+) (\d+) \$\(tier\)( true)?',(SOURCE/'function/workingitems/badwiki_upgraded.mcfunction').read_text(encoding='utf-8-sig')):
  upgraded.append(dict(mode=int(mode),effect=effect,ticks=int(seconds)*(1 if effect in ('instant_damage','instant_health','saturation') else 20),hidden=bool(hidden)))
 assert [r['mode'] for r in upgraded]==list(range(1,29))
 output=ROOT/'src/main/resources/skywars2/bad_wiki_effects.json'
 output.write_text(json.dumps(dict(random=random,academy=upgraded),indent=2)+'\n',encoding='utf8')
 keys={'upgrade_condition_enderwand','upgrade_succesful_enderwand','upgrade_condition_badwiki','upgrade_succesful_badwiki'}|{'lbc_'+r['effect'] for r in upgraded if r['effect']!='raid_omen'}|{'lbc_random_teleport'}
 def scan(v):
  if isinstance(v,list):
   for c in v:scan(c)
  elif isinstance(v,dict):
   if 'translate' in v:keys.add(v['translate'])
   for c in v.values():scan(c)
 for n in ['ender_wand','bad_wiki','academy_ender_wand','academy_bad_wiki']:scan(json.loads((dest/(n+'.json')).read_text(encoding='utf8')))
 rp=Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
 for lang in ['ru_ru','en_us']:
  data=json.loads((rp/f'assets/minecraft/lang/{lang}.json').read_text(encoding='utf-8-sig'));assert not keys-data.keys(),(lang,keys-data.keys())
 for model in ['enderwand','badwiki']:assert (rp/f'assets/lbc/items/{model}.json').is_file()
 print(f'Four source leaves, 84 random outcomes/28 selectable effects, {len(keys)} existing RU/EN keys. No RP writes.')

if __name__=='__main__':main()
