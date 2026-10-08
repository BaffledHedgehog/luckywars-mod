from pathlib import Path
p=Path('agent_notes/lbc_other_world_tech.md'); s=p.read_text(encoding='utf8')
needle='Сами эффекты и пороги описаны по конкретным elemental handlers ниже;'
addition='''Ключевые проверенные числа различаются по стихиям: fire-wisp даёт attack_damage +4/+7/+9/+10 при 1/2/3/4 духах; при 5+ формула `floor((count+16)/2)`. Water-wisp даёт max_health multiplier-base +0,4/+0,7/+0,9/+1,0 при 1/2/3/4; при 5+ значение `0,8 + 0,05×count`. Nature chance равен 20, 30, 35, 38, 40 для 1–5 и растёт на 2 за каждый следующий дух; раз в секунду выполняется roll 1..100, при успехе один из семи результатов (mana boost, resistance, regeneration, fire resistance, absorption, night vision, saturation). Light делает roll 0..99 и при 25..99 (75%) вызывает снятие debuff. Air наращивает charge на число wisps за каждый 2-tick проход; на 100 запускает zap с заданным `air_wisp_chains=3`. Shadow накапливает charge по количеству духов, только при cooldown 0, максимум до 500; расход/откат запускается отдельными ветками столкновения. '''
if addition not in s: s=s.replace(needle,addition+needle)
p.write_text(s,encoding='utf8')
