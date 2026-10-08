from pathlib import Path

p = Path('agent_notes/lbc_kit_interface.md')
s = p.read_text(encoding='utf8')
edits = {
    'Slot0 переключает challenge state только при': 'Slot0 устанавливает `lbc.challenge=1` (не переключает обратно) только при',
    'при включении также сбрасывает `challenge_score`': 'следующая строка пытается сбросить `challenge_score` только при `challenge!=1`, но после установки1 её условие уже ложно, поэтому обычное включение score не сбрасывает',
    'цель оказывается на случайном расстоянии от 0 до 15 блоков от исходной позиции, где ставятся chiseled bookshelf и barrel': 'цель переносится на поверхность в случайную точку квадратной области X/Z±15 блоков. Следующие `fill` ставят chiseled bookshelf и barrel в исходной позиции выполнения: `spreadplayers` перемещает сущность, но не переносит контекст команд',
    'Реально selector маршрутизирует только 1→soul и 3→bloodshed; значение 2 не имеет ветки. Значение 4+ также ничего не делает в этом selector. Это согласуется с UI score 1–3 не полностью и является функциональной дырой: возможный fourth behavior должен приходить из другого event path, которого в назначении не найдено.': 'Сам function-selector маршрутизирует только1→soul и3→bloodshed; значение2 здесь пропущено, поскольку молот подключён отдельно через advancement смерти игрока. Значение4+ ничего не делает в этом selector.',
    '`bloodshed_player` выдаёт отдельный текст/particle и revokes `lbc:killed_by_bloodshed`, но найденного вызова этого helper нет.': '`bloodshed_player` вызывается advancement `lbc:killed_by_bloodshed`, когда игрок убит другим игроком с `swrg.kill_effect=3`; executor reward — жертва. Он выдаёт текст/particle и отзывает advancement для повторного срабатывания. [Вход по JSON](../world/datapacksdisabled/LBC/data/lbc/advancement/killed_by_bloodshed.json).',
    '`wagnerhammer` вручную создаёт carrot-on-a-stick item display выше исходного игрока': '`wagnerhammer` создаёт carrot-on-a-stick item display на11 блоков выше погибшего игрока',
    'Однако в 146 источниках нет вызова `wagnerhammer`, а `kill_effects/select` его не выбирает; `hammerfalling`/`wagnerhammer` — неиспользуемая ветка для обычного kill-effect маршрута.': 'Это активная отдельная ветка: advancement `lbc:killed_by_wagnerkilleffect` вызывает её при смерти игрока от игрока с `swrg.kill_effect=2`. Reward выполняется как жертва и в конце отзывает advancement; display затем обслуживает `raycastselector_player`. [Advancement-вход](../world/datapacksdisabled/LBC/data/lbc/advancement/killed_by_wagnerkilleffect.json).',
    'Если у игрока есть предмет с `custom_data.instantitem`, общий tick-маршрут вызывает `select`.': 'Общий `players_tick` вызывает instant-dispatcher при теге `instantitem_active`; обычная ветка `select_old` при наличии `Inventory[].custom_data.touhou_item` вызывает Touhou `select`. Режим `#lbcskill=1` с `lbc.skill=2` направляется в отдельную `select2`, поэтому этот gate нельзя заменить простым наличием instantitem.',
    'каждый успешный запуск уменьшает прочность активного крюка': 'каждый успешный запуск уменьшает прочность всех подходящих крюков в обеих руках',
}
for old, new in edits.items():
    assert old in s, old
    s = s.replace(old, new)
p.write_text(s, encoding='utf8')
