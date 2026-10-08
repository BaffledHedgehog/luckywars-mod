from pathlib import Path
p=Path('agent_notes/lbc_other_imba.md'); s=p.read_text(encoding='utf-8')
start=s.index('### Рунные предметы')
end=s.index('\n\n### Области',start)
new='''### Рунные предметы

Обычный вход для кастов — `workingitems/raycast_spells/select` по `SelectedItem.components.minecraft:custom_data.rune_*`; cast-функции ниже сразу тратят ровно один matching item или делают это при попадании. Fehu/Gifu продолжают луч только пока нет локального stopper-а. Для проверок с `dx=0,dy=0,dz=0` размер куба остаётся1×1×1; `distance` пересекается с этой областью, не расширяет её до сферы.

| Rune | Полное действие, цель, расход и reset |
|---|---|
| Ansuz | Удаляет одну `rune_ansuz` и сразу переключает исполнителя в creative; другого reset в helper нет. |
| Fehu | Рекурсивный луч шагает1 блоком, счётчик ограничен200; останавливается на non-air либо non-spectator mob в совмещённом 1×1×1 cube/distance15. Продолжение также блокирует игрок с `nomagic_active` в16 и stopper marker в20. На конце ставит `lbc:item_gallery` со сдвигом(−19,0,−19), удаляет одну rune и завершает Distance. Закомментированный summon `itemkiller` не входит в cast; standalone helper `fehu_itemkiller` лишь считает item entities в20 и убивает их, если их100+. |
| Gifu | Луч шагает0.33, предел Distance200; конец — первый non-air (команда смещает точку проверки назад0.33) либо non-spectator mob в совместном кубе1×1×1 и `distance..7`. Его рекурсию также блокируют `nomagic_active` игрок в7 и stopper в20. При конце rune расходуется и в текущей точке выполняются20 отдельных `loot spawn lbc:random_glack_1`. |
| Hagalaz | Тратит rune, вызывает внешний `swrg:maploader/clear`, убивает все dropped item entities и пишет всем красный subtitle. Четыре одинаковые kill-команды повторены, результат после первой одинаков. |
| Kenaz | Тратит rune и телепортирует все entity в измерение Nether с сохранением координат/угла через `execute as @e[distance=0.01..,tag=!core_marker,tag=!strict_map_object] at @s in minecraft:the_nether run tp @s ~ ~ ~ ~ ~`; нет type/team/range cap. |
| Nautiz | Тратит rune, рассылает всем subtitle, даёт Resistance V на10s, глобально выключает fall damage, переносит всех игроков в (0,1000,0), запускает `swrg:game/quickstart/1` и четыре раза убивает all entities кроме игроков/marker-ов. `keep_inventory` и fall damage — глобальные gamerules, эта функция их не возвращает. |
| Raido | Тратит rune, ставит fake-player score `bottom_nexus=1` и показывает всем subtitle; сам helper не переносит игроков и не вызывает maploader. |
| Thurisaz | Сначала сохраняет команду caster-а, уменьшает всем mobs (кроме союзников, spectator и `strict_map_object`) в0.01..50 десять характеристик почти до нуля и ставит tag `submissive`; затем тратит rune и владельцу добавляет modifiers: scale/attack/max-health/speed ×2, блок/сущность interaction range ×3, остальные перечисленные движения/высота/копание ×2; fall damage ×0.5. Tag `gigantic` включает player-tick helper: mobs в1 блоке получают100 falling-anvil damage на каждом проходе. Общий `resets.mcfunction` удаляет owner и negative modifiers, `gigantic` и связанные tags. |
| Uruz | Тратит rune, заменяет owner base attack damage на огромное literal integer (значение в source) и даёт Resistance V infinite. Сброс идёт через общий reset/death lifecycle, не локальный timer. |
| Wunjo | Тратит rune, показывает subtitle, добавляет владельцу16 `swrg.lives`, глобально включает `keep_inventory=true`; helper сам не возвращает gamerule в прежнее значение. |

Источники: [selector](../world/datapacksdisabled/LBC/data/lbc/function/workingitems/raycast_spells/select.mcfunction), [Fehu hit](../world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/fehu_cast_end.mcfunction), [Gifu reward](../world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/gifu_cast_end.mcfunction), [Thurisaz owner](../world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/thurisaz_cast.mcfunction), [Thurisaz targets](../world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/thurisaz_cast_others.mcfunction), [per-pass stomp](../world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/thurisaz_gigant_tick.mcfunction).'''
s=s[:start]+new+s[end:]
# Add lingering tag caveat to time-stop paragraph after its existing leave_death statement.
s=s.replace('`leave_death` removes the tag, ending repeat teleports. This tick does not restore saved pose or other effects.', '`leave_death` is the only removal of the tag found in the studied function graph; expiry of the effect counter alone does not end repeat teleports, so absent that cleanup the tag can persist. This tick does not restore saved pose or other effects.')
t=p.with_suffix('.md.tmp'); t.write_text(s,encoding='utf-8',newline='\n'); t.replace(p)
