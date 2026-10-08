"""One-shot final family notes and grouping; no datapack mutations."""
from pathlib import Path
r=Path(__file__).resolve().parents[2];p=r/'agent_notes/lbc_other_events.md';s=p.read_text(encoding='utf-8')
head='## Торговец Bucks\n\n'
a=s.index(head);b=s.index('\n\n',a+len(head))
note=s[a+len(head):b]
assert note.startswith('Для Pumpkin Curse')
s=s[:a]+note+'\n\n'+head+s[b+2:]
anchor='## Файлы: other / troll_box'
table='''## Troll Box: параметры шести масок

Select выбирает1..6 равновероятно. Каждая выдаёт один unbreakable leather helmet с собственными моделью, tier и trollmask-ключом. Модификаторы действуют в head-slot; нижеприведённые проценты относятся к **базовому атрибуту** (add_multiplied_base), итог с другими modifiers отдельно не гарантируется.

| Mask | Модификаторы при ношении | Однократный исход select |
|---|---|---|
|1|HP+400%, speed+400%|trollhunter1 и общий титр.|
|2|attack+50%, attack-speed+400%|darkness1, darkness_player автору; очищает/немедленно запускает tick90s.|
|3|attack+100%, armor+400%|powerfall1 и титр.|
|4|speed+100%; knockback-resistance/attack/armor/toughness/HP+200%; luck−1000 add_value; Binding Curse|Ставит empty_cage всем non-spectator вне Nexus (наковальни/абсолютная вертикаль описаны в drops).|
|5|armor/toughness−80%; attack-speed+100%; HP−10%; attack+75%|По25 размещений walls с integrity0.995 вокруг каждого non-spectator вне Nexus; звук backrooms.|
|6|speed−25%|abyss1 и титр; при ношении Troll Mask6 дополнительно вызывает Eyelaser из player tick.|

Глобальные флаги остаются после select/снятия маски до внешнего reset; end_resets обнуляет trollhunter/darkness/powerfall/abyss. Когда runtime вызывает spawn_trollge_pre,5%-ная проверка выбирает случайного non-spectator и создаёт Trollge: persistent wither skeleton с20HP/8attack, долгой SpeedII/Invisibility и без equipment drops. Spawn размещает его наY+6 и через spreadplayers с maxRange10 перемещает вокруг исходной точки (Nether under120). Sound helper равновероятно выбирает9 комбинаций трёх scream-звуков/трёх pitches. Таймера удаления моба в этих файлах нет. Действие дополнительных hunter/tp-тегов находится в общем runtime.

'''
assert anchor in s
s=s.replace(anchor,table+anchor,1);p.write_text(s,encoding='utf-8')
print('events families completed')
