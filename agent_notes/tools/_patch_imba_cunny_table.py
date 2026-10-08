from pathlib import Path
p=Path('agent_notes/lbc_other_imba.md'); s=p.read_text(encoding='utf-8')
start=s.index('| ID | Реальное действие после выбора |')
end=s.index('\n\nШанс/геометрия',start)
table='''| ID | Полное действие / условие и окончание |
|---:|---|
| 0 | Вызывает `swrg:game/start/0`, пишет всем `cunny_slab_0`; снимает один предмет с `cunny:0`. |
| 1 | Сохраняет команду исполнителя и удаляет четыре armor slots у каждого другого игрока с несовпадающей командой: selector `@a[distance=0.01..]` без верхней границы и без фильтра spectator. Потом частицы, сообщение и расход одного `cunny:1`. |
| 2 | Добавляет шесть attribute modifiers: armor +6, toughness +3, attack damage +6, scale ×0.5, max health ×1.5, movement speed ×1.2; выдаёт три loot (`inf_shulker`, `inf_steak`, `evolution_sword`), сообщает и тратит один предмет. Modifiers снимает общий `resets.mcfunction`, значит эффект не вечен. |
| 3 | Даёт владельцу Regeneration III infinite и ставит grassifier marker снизу. Тик marker-ов опускается по воздуху, поднимается если сверху блок; переводит клетку в grass, размножается на четыре грани пока там нет травы и заканчивает себя при уже существующей/поставленной траве. |
| 4 | Вызвает cultist helper ровно20 раз на 16 уникальных позициях радиуса2: 8 соседей на радиусе1, 4 диагонали радиуса1, затем 4 cardinal позиции радиуса2, каждая вызвана дважды. Каждый helper делает равновероятный бросок1..6 и создаёт одного из шести описанных ниже cultist; расходует предмет. |
| 5 | Выдаёт loot `lbc:imba/nahida_dakimakura`, сообщение, расход одного `cunny:5`. |
| 6 | Случайный другой не-spectator, не союзник получает `love_girl`; владелец получает `girl_seeker`, а выбранному игроку и владельцу рассылается сообщение. Если цель отсутствует, owner всё равно получает tag и предмет расходуется. В игровом цикле seeker запускает эффекты цели через players_2t, airborne target получает импульс в сторону seeker через players_tick; leave_death снимает оба тега. |
| 7 | Временно ставит `cstr` владельцу и создаёт whip loot для каждого другого non-spectator игрока без `cstr` (без фильтра команды); подхватывает соответствующий item entity и записывает UUID получателя в `teleportid`. Убирает временный tag, сообщает и тратит предмет. |
| 8 | Один раз выдаёт loot `cunny_slabs`, затем отдельно для каждого игрока прибавляет `twitching +60` и `cumming +160` и случайно выбирает ровно один из шести звуков; сообщение и расход. |
| 9 | Выбирает X и Z независимо из−1,000,000..1,000,000, телепортирует владельца на Y=320, ставит platform marker на100 выше, даёт Slow Falling60s и выполняет23 команды loot `cunny_slabs`; item consumed. Marker пытается fill-нуть площадку quartz 33×33 и удаляется только при успешном fill. |
| 10 | Три выдачи loot (`luckyblock_all_stacks`, `op_hoe`, `god_dice`), затем убивает одно случайное `@e` по миру без фильтра типа/команды, так что игроки тоже попадают в выбор; сообщает и тратит предмет. |
| 11–20 | Функции содержат только TODO-комментарий, сообщение владельцу и расход одного предмета соответствующего ID; игрового эффекта в командах нет. |
| 21 | До64 случайных `@e` без type/team/range фильтра получают вызов того же cultist helper в своей позиции; каждый успешный выбор создаёт отдельного случайного cultist. При пустом selector вызовов нет, но сообщение и расход выполняются. |'''
s=s[:start]+table+s[end:]
# Correct introductory prose for caller table and source link.
s=s.replace('Caller выбирает `cunny` score, каждая функция `cunnys/0..21` в конце очищает один предмет соответствующего custom_data;', 'Caller `workingitems/cunny_item/select` читает `SelectedItem.components.minecraft:custom_data.cunny`, диспетчер выбирает соответствующую функцию `select1`, а ветвь `cunnys/0..21` очищает до одного предмета с данным ID;')
s=s.replace('[cunnys/0..21](../world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/0.mcfunction)', '[выбор Cunny](../world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction) и [cultist template](../world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/4_spawn_cunny_spawn.mcfunction)')
# Fix marker-target semantics and no-op cards in the summary.
s=s.replace('каждый сдвигается на10 блоков, ставит barrel-случай `cunny_slabs` и waxed sign с двумя независимо выбранными строками (полная таблица 100×4 строк выше)', 'каждый сдвигает X/Z на10, ставит barrel-случай `cunny_slabs` и waxed sign с двумя независимо выбранными наборами по четыре строки (полная таблица 100×4 строк выше)')
t=p.with_suffix('.md.tmp'); t.write_text(s,encoding='utf-8',newline='\n'); t.replace(p)
