from pathlib import Path
p=Path('agent_notes/lbc_other_imba.md'); s=p.read_text(encoding='utf-8')
s=s.replace('`correction_cunny` сначала смещается на 10 блоков через `universal_pos_razb_10`, запускает сборку знака и затем удаляет свой marker.', '`correction_cunny` использует `universal_pos_razb_10`: X и Z независимо округляются до целого, затем к каждому добавляется случайное целое−10..10; Y не меняется. После смещения строит barrel/sign и удаляет marker.')
s=s.replace('каждый сдвигается на10 блоков, ставит barrel-случай', 'каждый независимо смещает X/Z случайными целыми−10..10, ставит barrel-случай')
needle='''Цвета, не перечисленные как наносители статуса владельцу (green/purple/gray), не получают скрытого бонуса из имени: применяются только прямо указанные команды. Подробности отдельных веток: [цветовой helper, например yellow](../world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/yellow.mcfunction).

### Cunny Slab'''
insert='''Цвета, не перечисленные как наносители статуса владельцу (green/purple/gray), не получают скрытого бонуса из имени: применяются только прямо указанные команды. Цветной effect повторно выдаётся каждые5 tick, пока игрок стоит на поддерживаемом `#minecraft:colored` блоке или в `#minecraft:in_colored`; это не одноразовый use с cooldown. Подробности отдельных веток: [цветовой helper, например yellow](../world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/yellow.mcfunction).

### Остальные mystical-артефакты: триггер и полная ветвь

| Вход | Условие и фактический эффект |
|---|---|
| `mystical:1` passive | `players_5t` каждые5t вызывает dispatcher только при `afkartifact5t_active` и без `stopper_magic` в20. Type3 (`nofluid`) заменяет water→frosted ice, lava→basalt, powder snow→snow_block в кубе25³ и передаёт falling blocks в16 helper-у замены. Type7 — цветовая таблица выше; type8 при `hplvl<MaxHealth` выдаёт Hunger III + Regeneration II по1s; type5 раз за проход очищает blindness/slowness/weakness/mining fatigue/glowing/hunger/instant damage/nausea/poison/unluck/wither, а levitation очищает только если ни один перечисленный reactive/techno/custom item не защищает её; type6 снимает все эффекты.
| `mystical:3` jump | `items/armor` проверяет предмет в inventory, отсутствие stopper_magic в20 и отсутствие игрока с `nomagic_active` в20. Type1 даёт Speed VI владельцу3s и Slowness III врагам3s в7; type2 — Levitation XI врагам1s в7, Slow Falling10s владельцу и облако; type3 конвертирует текущие Jump Boost amplification 32/16 в Slow Falling30/20s и очень высокий Jump Boost8/10s, либо снижает 8/4/2/1 на один шаг; затем безусловно добавляет Jump Boost I на10s.
| `mystical:4` hurt | `items/damaged` запускает dispatcher при таком custom_data где-либо в inventory и stopper_magic отсутствует в20. Type2 даёт Resistance V всем игрокам в7 на1s, Slow Falling5s владельцу и TNT fuse0; type3 place template `lbc:air11x11` integrity0.5 со смещением(−5,−8,−5) + портал/звук; type4 Weakness X на3s сущностям non-spectator в7; type5 даёт конверсию status-effects по таблице ниже; type6 summon-ит wolf с Owner UUID владельца. Type1 в dispatcher отсутствует и не запускает helper.
| `mystical:2` hit | `items/swords` требует предмет в inventory и отсутствие `nomagic_active` игрока и stopper_magic в20. Type1 — Regeneration III + Saturation V владельцу на2s, `food_count+1`, Wither V + Hunger100 врагам в0.01..7 с `HurtTime:10s`. Type2 — Poison V20s им в0.01..8; type3 — smoke, Blindness I5s игрокам и mob follow_range base3 в0.01..7; type4 — обоим hit-целям и владельцу запускает horizontal move4 (поворот перед helper-ом принудительно pitch0); type5 разворачивает всех найденных целей на180° в7; type6 добавляет владельцу1000 mana. Кулдауна в dispatcher нет.

У `speed` conversion helper-а проверки последовательны, поэтому эффекты накапливаются вместе с безусловным Strength I5s: Fire Resistance→Regeneration II10s; Resistance→Fire Resistance I40s и Fire Resistance II10s; Speed→Resistance I5s; Strength→Speed III5s. [Damage ветви мистического4](../world/datapacksdisabled/LBC/data/lbc/function/other/artifact/damaged.mcfunction), [sword-hit ветви мистического2](../world/datapacksdisabled/LBC/data/lbc/function/other/artifact/ondamage.mcfunction), [jump ветви](../world/datapacksdisabled/LBC/data/lbc/function/other/artifact/when_jump.mcfunction), [5t passive dispatcher](../world/datapacksdisabled/LBC/data/lbc/function/other/artifact/afkartifact5t.mcfunction).

### Cunny Slab'''
if needle not in s: raise SystemExit('artifact insertion point missing')
s=s.replace(needle,insert)
t=p.with_suffix('.md.tmp'); t.write_text(s,encoding='utf-8',newline='\n'); t.replace(p)
