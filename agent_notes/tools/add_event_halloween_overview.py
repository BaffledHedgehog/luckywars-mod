from pathlib import Path
p=Path('agent_notes/lbc_other_events.md'); s=p.read_text(encoding='utf8')
anchor='\n## Файлы:'
block='''
## Жизненный цикл Halloween

`start` выдаёт всем advancement `lbc:true_advancements/event/halloween` и включает глобальный звук. Употребление еды учитывается отдельно: `eat_halloween_food` увеличивает `pumpkin_counter` на 1 и по значению 1..100 выдаёт соответствующий под-ID advancement; сами критерии еды находятся в advancement JSON, а не в этой функции. Фоновая `player_tick_1s` начисляет случайно 1..50 `swrg.memcoins`, показывает число игроку и затем выбирает scare-сцену; исходник задаёт random 1..7, хотя после него перечислены восемь веток, поэтому ветка 8 из этого входа недостижима.

Семейство `jack_o_lantern_*` создаёт летящий block_display с тегом `jack_o_lantern`, передаёт ему направление, ID игрока и команду. Проверка на траектории запускает взрывную функцию, когда в кубе selector `dx=0,dy=0,dz=0` есть незатегованная цель-моб; это куб 1×1×1 в точке исполнения, а не радиусная проверка. Взрыв создаёт фейерверк, наносит всем мобам в радиусе 3,5 урона 6 типа arrow (с Owner игрока, если удалось найти `damager`), снимает временный тег и удаляет display.

Ссылки на опорные этапы: [запуск события](../world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/start.mcfunction), [учёт еды](../world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction), [начисление валюты и scare-развилка](../world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/screamer_activate.mcfunction), [создание фонаря](../world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/jack_o_lantern_spawn.mcfunction), [проверка цели](../world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/jack_o_lantern_check.mcfunction), [взрыв и урон](../world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/jack_o_lantern_explode.mcfunction).
'''
if '## Жизненный цикл Halloween' not in s:
 i=s.index(anchor); s=s[:i]+'\n'+block+s[i:]
p.write_text(s,encoding='utf8')
