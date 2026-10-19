# Review Tiny Air / Stone / Teleport

Сверил текущие `LbcWands.java` и route регистрации с `WANDS_SECOND_SOURCE.md`, datapack source и локальным `minecraft-merged-...-26.3-sources.jar`. Это code review, не build/server runtime PASS.

## Результат

- В текущем `ray()` уже есть user-requested single terminal: найденная сущность вызывает end на точке луча и сразу `break`; только при отсутствии попадания проверяется стена, её end ставится на предыдущей свободной точке. Так исчезает старый двойной endpoint. Для Stone это оставляет ровно один endpoint fill.
- Source-note надо читать с поправкой на порядок `execute`: в команде `execute unless block ~ ~ ~ #minecraft:airs positioned ^ ^ ^-0.35 run function ...` block-check проверяет **текущую** ray-точку, а `positioned` меняет позицию вызываемой wall-end функции на предыдущую. Поэтому Java-порядок «проверить текущий block → выполнить wall-end на `previous`» верен. В прежнем source note фраза о проверке блока позади была неточна.
- `STRICT_FLAGS` равен `2|16|32|256|512 = 818`. В 26.3 `FillCommand strict` вызывает `block.place(level,pos,2|(strict?816:256))`, то есть те же флаги 818. Для обычного stone без block-entity данные прямой `setBlock` соответствует этому вызову; оставшаяся оговорка — Java пропускает координаты, для которых `hasChunkAt(pos)==false`, а datapack `fill` сам это условие не добавляет. На незагруженной границе команда может заполнить меньше 27 блоков; нужна проверка, что игровые арены держат весь локальный куб загруженным, либо решение о намеренном skip.
- Tiny Teleport объединяет два source selector-а. Проверки entity position в радиусах 2/12 и bbox-пересечения со второй селекторной ячейкой `point..point+(1,1,1)` соответствуют их геометрии; один `addEffect` вместо двух идентичных записей даёт тот же итоговый статус (20 ticks, amplifier 30). `target()` дополнительно фильтрует союзных/неактивных игроков — это применённая friendly-protection политика, не доказательство полной поддержки будущих Teams-режимов.
- Tiny Air NBT-предустановка содержит исходные `Life:0`, `LifeTime:0`, `ShotAtAngle:1b` и один fireworks burst с исходными цветами/trail/twinkle. Native 26.3 `FireworkRocketEntity.readAdditionalSaveData` считывает эти поля, а `tick()` увеличивает `life` и взрывает при `life > lifetime`; с `LifeTime=0` это короткий native projectile, не самописный тикер. `FireworkRocketEntity.dealExplosionDamage` создаёт fireworks damage с rocket как direct entity и владельцем как attacker. Реестр `WandProjectileMixin` фильтрует столкновения до native hit; Fabric `ALLOW_DAMAGE` отдельно ограничивает splash-вред: owner пропускается явно, союзники блокируются. Owner установлен намеренно для атрибуции/защиты; не следует считать командную политику завершённой для всех будущих режимов.
- Статический binding цикла не обнаружил: `TINY_*` зарегистрированы до `Spell`/`SPELLS`; enum лишь строит локальные флаги и score holders. `LbcWands.register()` вызывается в регистрации предметов, но не обращается к `SkyWars2.game()`; runtime `game` запрашивается только в use callback. Оснований считать это initialization cycle нет.
- Existing Tiny Blind route остаётся в том же `Spell.BLIND` switch. Учитывает новую общую фильтрацию цели, останавливается на стене и применяет blindness 80 ticks + 0.25 arrow damage на hit; этот damage source создаётся с атакующим игроком. При текущей фильтрации союзники и наблюдатели не считаются попаданиями, поэтому луч проходит через них. В тестовом коде есть отдельные проверки wall stop, ally exclusion и observer transparency; их наличие не заменяет runtime PASS.

## Замечание по стоимости луча

Ray собирает кандидатов одним broad-phase AABB, затем до 101 раза линейно перебирает весь список. Это ограничивает повторные world-query, но стоимость всё равно `O(101 × candidates)` при длинном свободном коридоре; при очень плотном заполнении сущностями cast может дать заметный main-thread spike. Из кода нельзя вывести реальный лаг без измерения, поэтому это performance risk, а не подтверждённый дефект. `ray()` также шлёт до 101 particle packet-ов; Tiny Blind — в среднем около 30.

## User annotation

> погоди ка, независимое исполнение жезла от стены и сущности это баг, надо исправить. Когда видишь странное поведение - спрашивай меня все таки

Это уточнение определяет целевой single-terminal порядок. На момент review он уже отражён в `LbcWands.java`; старый datapack и `WANDS_SECOND_SOURCE.md` остаются свидетельством фактической исходной команды, не целевого поведения.

## Проверенные первоисточники

- Старый datapack: `function/workingitems/sauvojen_staffs_tiny/{air,air_2,air_end,stone,stone_2,stone_end,teleport,teleport_2,teleport_2_end}.mcfunction`, `function/items/spells.mcfunction`, `function/workingitems/spell1.mcfunction`, `spell1_default[_left].mcfunction`.
- Новый код: `src/main/java/me/skywars2/game/LbcWands.java`, `LbcTrinkets.java`, `GameController.java`; `src/main/java/me/skywars2/mixin/WandProjectileMixin.java`, `src/main/java/me/skywars2/SkyWars2.java`.
- Minecraft 26.3 sources: `net/minecraft/server/commands/FillCommand.java`, `net/minecraft/world/entity/projectile/Projectile.java`, `net/minecraft/world/entity/projectile/FireworkRocketEntity.java`, `net/minecraft/world/level/block/Block.java`.
