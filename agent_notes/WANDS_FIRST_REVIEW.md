# Review: Tiny Fire/Blind wand и cast-trinkets

Сверены `LbcWands`, `LbcTrinkets.castWand` и `LbcItems.castWandDynamite` с OLD LBC `workingitems/sauvojen_staffs_tiny/{fire,fire_2,blind,blind_2,blind_end}.mcfunction`, `items/trinket_effects/{cast_wand,tnt,tnt_chance,mana_gem,charge_tnt}.mcfunction` и `config.mcfunction`.

- Tiny Fire: нативный `SmallFireball` получает направление взгляда ×2.5, стартует у глаз + направление и имеет owner. Локальный 26.3 `AbstractHurtingProjectile` подтверждает `accelerationPower = 0.1` в базовом конструкторе; старый `fire_2` не записывает `acceleration_power` в NBT, поэтому одна лишь функция не доказывает значение старого runtime. Старый marker `Pos` от world origin кодирует именно offset-вектор 2.5, не цель полёта. Owner — осознанная friendly/attribution policy. Перепроверка вызова из `spell1_default` показывает, что legacy `fire` уже выполняется с позицией и anchor глаз; его внешний `playsound ... ~ ~ ~` тоже звучит от глаз, как и `LbcWands.fire`.
- Tiny Blind: шаг луча 0.35 и 101 позиция (индексы 0–100), одна пространственная выборка всех entity-кандидатов, проверка первого допустимого кандидата на точке и остановка по `minecraft:airs` соответствуют рекурсивному source `blind_2`. Source chance30 squid-ink помечен `force`; вызов 26.3 передаёт `longDistance=true`. Blindness 80 тиков равна source 4 секундам; урон — 0.25 с arrow damage type. Две руки, отсутствие cooldown, gate magic suppression и отсутствие gate на счётчике countdown соответствуют проверенной политике этапа. Удаление союзных/observer целей — целевая friendly-fire/observer policy.
- Mana Gem hook добавляет `60 + 15*(copies-1)` настоящей mana; Blood Anal Boom сохраняет source chance: 25% при одной копии, `20% + 5% * copies` для 2–15, гарантировано с 16. Оба hooks запускаются после любого Tiny wand cast, как source `cast_wand`; кап `lbcTrinketLimit=0` их отключает, как source global gate. TNT owner в native cast задан игроком; старый TNT сохранял `lbcID2` и `team_number`. Это ожидаемая новая атрибуция/дружеская обработка, а не потерянный cast hook.

End-to-end проверки для обоих wand, обеих рук, Mage bonus, mana/trinket hooks, ray wall/target filtering, insufficient mana, Overload debt rule и cleanup уже перечислены в `GameChecks` группе wands. Полный server verification status остаётся источником истины в [SERVER_TESTS.md](../SERVER_TESTS.md); этот review не объявляет общий full run PASS.

У стены source `blind_2` после не-air проверки вызывает `blind_end` с execution origin на 0.35 блока назад, где selector повторно проверяет сущности в предыдущей ячейке. Это было отличие от ранней версии `LbcWands.blindRay`; текущий код уже вызывает `blindEnd` с `rayTarget` на `point - step`, в том числе на первой точке `origin - step`, и сохраняет source endpoint-поведение. Это теперь включено в перечисленные `GameChecks`; runtime status по-прежнему отслеживается в `SERVER_TESTS.md`.

Local 26.3 source проверен отдельно: `AbstractHurtingProjectile` no-shooter constructor оставляет поле `accelerationPower` равным `0.1`; `SmallFireball(EntityType, Level)` делегирует именно ему. Следовательно native fireball после установки velocity `look × 2.5` получает стандартное acceleration при последующих тиках. Старый summon не задаёт `acceleration_power`; полного сравнения с OLD runtime в этой проверке нет.

Исправлено позиционное отличие у Blood Anal Boom: Tiny cast наследует origin
глаз из `spell1_default`/`spell1_default_left`, а `charge_tnt` ещё раз применяет
`anchored eyes`. Текущий код явно передаёт castOrigin и прибавляет eye-height,
сохраняя `feet + 2 × eyeHeight` для Tiny. Новый fixture проверяет координаты
и нативного владельца TNT; окончательный runtime-результат записывается в
[SERVER_TESTS.md](../SERVER_TESTS.md).
