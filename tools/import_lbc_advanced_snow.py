"""Extract Advanced Ender/Snow source leaves and audit the read-only author pack."""
from pathlib import Path
import json
from import_lbc_loadouts import ROOT,SOURCE
from import_lbc_wands import import_wand_family

def main():
 dest=ROOT/'src/main/resources/data/lbc/loot_table/ported'
 for name,flag,tier in [('advanced_ender_wand','advancedteleport','s'),('snow_wand','snow_wand','b')]:
  import_wand_family(SOURCE/'loot_table/transformables_staff.json',dest/f'{name}.json',dest/f'academy_{name}.json',name,f'skywars2:{name}',f'skywars2:academy_{name}',data_marker=flag+':1',academy_modifiers=('add_upgrade_desc','tiers/'+tier))
 keys={'upgrade_condition_advancedteleport','upgrade_succesful_advancedteleport','upgrade_condition_snow','upgrade_succesful_snow','mode_default','mode_uppest_block','mode_wall_clip'}
 def scan(v):
  if isinstance(v,list):
   for c in v:scan(c)
  elif isinstance(v,dict):
   if 'translate' in v:keys.add(v['translate'])
   for c in v.values():scan(c)
 for n in ['advanced_ender_wand','academy_advanced_ender_wand','snow_wand','academy_snow_wand']:scan(json.loads((dest/(n+'.json')).read_text(encoding='utf8')))
 rp=Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
 for lang in ['ru_ru','en_us']:
  data=json.loads((rp/f'assets/minecraft/lang/{lang}.json').read_text(encoding='utf-8-sig'));assert not keys-data.keys(),(lang,keys-data.keys())
 for model in ['advancedenderwand','snow_wand']:assert (rp/f'assets/lbc/items/{model}.json').is_file()
 assert (rp/'assets/minecraft/sounds/lbc/snow_wand.ogg').is_file() or 'lbcsounds.snow_wand' in (rp/'assets/minecraft/sounds.json').read_text(encoding='utf-8-sig')
 print(f'Four source leaves, {len(keys)} existing RU/EN keys and source models/sound verified. No RP writes.')

if __name__=='__main__':main()
