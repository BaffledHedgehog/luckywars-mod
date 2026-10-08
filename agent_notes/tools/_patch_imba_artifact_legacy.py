from pathlib import Path
p=Path('agent_notes/lbc_other_imba.md'); s=p.read_text(encoding='utf-8')
needle='''### Cunny Slab: выбор номера, расход и полный набор исходов'''
block='''### Legacy-артефакты, no-arrow/no-TNT и обработчики блокировки

Это самостоятельные вызовы с указанными периодическими входами; не сливаю их со `mystical:*` таблицей, потому что у них другие `custom_data` и selectors.

| Handler | Реальный вход и эффект |
|---|---|
| `shield` | `items/damaged_by_player` на событии урона от игрока, пока executor не spectator и глобальный `time_stopped` score не положительный: Resistance V2s + две barrier particles и anvil sound. |
| `noarrows` | Вызывается от `no_arrows_active` в `players_tick` и от shulker set bonus. На каждом проходе визуализирует и убивает все entity типа `#minecraft:arrows` в16; предметы-стрелы не возвращаются. |
| `notnt` | `players_2t` каждый2t при `notnt_active`: visual/sound и удаление всех `#minecraft:unstable` в24; для `tag=nuke` в24 запускает отдельный `stoppernuke`, затем убивает marker-ы нестабильных объектов. |
| `noeffects1` / `nohp` | `mystical:1,type=5/6` на passive-цикле5t: первый очищает список в верхней таблице, второй снимает все активные effects. |
| `noeffects1_old` / `nohp_old` | `mystical_old:1,type=1/3` в legacy 1s tick: первый очищает blindness, slowness, weakness, strength, levitation, slow_falling, haste и mining_fatigue; второй — regeneration, wither, poison, instant_damage, instant_health, absorption, health_boost. |
| `nomagic_old` | Legacy5t при `mystical_old:1,type=2`: убивает area-effect-cloud, evoker-fangs, experience-bottle, experience-orb и dragon-fireball в4. |
| `notnt_old` | Legacy5t при `mystical_old:1,type=5`: возвращает item TNT/fire charge/end crystal для соответствующих entity в радиусах5/6/7 и убивает creeper в5 вместе со всеми этими explosive entities. |
| `noarrows_old` | Legacy player tick при `mystical_old:1,type=4`: превращает стрелы в4 в dropped-arrow items, затем убивает исходные стрелы. |

[schedule старых предметов 1s](../world/datapacksdisabled/LBC/data/lbc/function/other/new_game/legacy_tick_1s_player.mcfunction), [5t](../world/datapacksdisabled/LBC/data/lbc/function/other/new_game/legacy_tick_5t_player.mcfunction), [каждый tick](../world/datapacksdisabled/LBC/data/lbc/function/other/new_game/legacy_tick_player.mcfunction), [shield вход](../world/datapacksdisabled/LBC/data/lbc/function/items/damaged_by_player.mcfunction).

### Cunny Slab: выбор номера, расход и полный набор исходов'''
if needle not in s: raise SystemExit('needle missing')
s=s.replace(needle,block)
t=p.with_suffix('.md.tmp'); t.write_text(s,encoding='utf-8',newline='\n'); t.replace(p)
