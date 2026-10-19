# Mana core: исходный datapack

Источник: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function`.

## Живая цепочка и число прибавок

`mobs_tick` вызывает `players_tick` для игрока, который не в spectator и не `time_stopped`; в `players_tick` единственный основной вход регена — `other/manaregen` (если не legacy/nomagic и не действует stopper_magic). `manaregen` пересчитывает `manaregen_visual` из `manaregen`, прибавляет бонусы ремня и mage/nyauwu-брони, умножает regen при `effect_boost_mana`, затем при положительном regen ниже cap зовёт `manaregen_true`. Там ровно одна прибавка `mana += manaregen_visual`, разрешённая лишь при `reverse_mana <= 0`, затем cap до `manamax`.

Так что обычная живая цепочка даёт **одну реген-прибавку за вызов/tick**, а не две. Второй реальный путь — `manaregen_true_pre`: он делает `mana += 1`, затем ещё `+= manaregen_visual`, но вызывается только из `manaregen_mage_armor`, когда надет Nyauwu chest, `mana >= manamax` и блок под игроком входит в `#minecraft:water`. Это отдельный спецэффект (добавляет 1 и позволяет превысить cap), а не повторная штатная прибавка. Ещё один отдельный источник — Nyauwu chest при ударе: `items/swords` вызывает `manaregen_hit`, который умножает текущий visual regen на 70 и зовёт `manaregen_true`.

Mana belt добавляет к `manaregen_visual` по 2 за каждый пояс в инвентаре. Mage pieces дают +4/+6/+7/+4 по слотам. Nyauwu legs удваивают visual regen при отсутствии мобов в радиусе 10. Эти бонусы меняют размер одной штатной прибавки; они сами по себе не запускают ещё один обычный вызов.

## Cap, смерть, reverse mana

- `manaregen_true` после прибавки жёстко ставит mana в `manamax`, если mana выше cap. Условие вызова штатной регенерации — `mana < manamax`; при нулевом/отрицательном regen действует отдельная ветка. `manaregen_true_pre` cap не применяет. Функция `other/maxmana` сравнивает дефицит с `manaregen_visual` и ставит mana в cap, но в datapack не найден её вызывающий код; не считать её живой частью без дополнительного источника.
- Первичная инициализация в `resets`: `manamax=5000`, `mana=5000`, `manaregen=10` (или 1 для SurvivalVersion). `other/idgive` вызывает `resets` для нового player ID. `leave_death` очищает временные mana-эффекты и ставит `reverse_mana=0`; обычный `items/on_death` лишь обрабатывает survival-ветку и сбрасывает счётчик death. Проверенного отдельного сброса mana/manamax в момент смерти не нашёл; новые базовые значения выставляет именно reset/инициализация.
- При `reverse_mana >= 1` в `manaregen` сначала выполняется `mana -= manaregen_visual`. Положительный реген затем может вызвать `manaregen_true`, но её selector фильтрует прибавку (`reverse_mana=..0`), поэтому при reverse mana положительной прибавки нет. Таймер уменьшается в `players_1s` на 1/сек. При нуле/отрицательном значении обычная прибавка разрешена.

## Nyauwu и вопросы для переноса

Nyauwu armor passive: helmet добавляет `manamax +20` каждый tick, пока рядом (0.01–10) есть игрок не-spectator; chest в воде на полном mana вызывает `true_pre`; legs при отсутствии mobs в радиусе 10 удваивают regen; boots меняют скорость через `speed_up`. Английские lore keys подтверждают intent: helmet max mana nearby, chest ignores mana cap in water, leggings быстрее без игроков/mobs рядом, boots ускоряют при неполной mana. В коде helmet `+20` выполняется **без временного отката** при исчезновении игрока/снятии брони, то есть это постоянный рост max mana — вероятный дефект, если ожидался временный бонус. Chest water path добавляет базовую 1 и весь regen без cap, согласуется с lore, но условие `mana >= manamax` означает срабатывание только на полном mana.

Специальные Nyauwu-пассивы не входят в текущий перенос ядра. Перед их переносом
нужно отдельно сверить накопительный рост `manamax` от шлема с желаемым поведением
и проверить over-cap chest path. Отсутствующие в источнике термины не являются
новыми требованиями пользователя.

Основные ссылки: `mobs_tick.mcfunction`, `players_tick.mcfunction`, `players_1s.mcfunction`, `other/manaregen.mcfunction`, `other/manaregen_true.mcfunction`, `other/manaregen_true_pre.mcfunction`, `other/maxmana.mcfunction`, `other/manaregen_mage_armor.mcfunction`, `other/manaregen_mana_belts.mcfunction`, `items/swords.mcfunction`, `items/on_death.mcfunction`, `leave_death.mcfunction`, `resets.mcfunction`.
