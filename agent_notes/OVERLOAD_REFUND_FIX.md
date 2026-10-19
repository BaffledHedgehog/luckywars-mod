# Исправление возврата маны Overload

У полного комплекта возврат маны появлялся не из прыжка. В `LbcOverload.afterRegen()` при долге ≤−20001 каждый проход вызывает луч; при полном комплекте он выбирает full-set вариант. В `LbcOverload.ray()` попадание луча при полном комплекте добавляло владельцу 300 маны за каждую задетую цель на каждой точке луча. Поэтому долг мог быстро уменьшаться без прыжка.

## Источник и расхождение с требованием

Старый datapack содержит такой же возврат: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_damage_restore_hp.mcfunction` после восстановления здоровья применяет `scoreboard players add @s mana 300`. Функция вызывается из `tentacle_damage.mcfunction`, куда луч передаёт успешную цель; полный комплект запускает луч через `tentacle_full_set.mcfunction` и `tentacle_full_set_2.mcfunction`. Это не побочный результат прыжка, а исходная награда попадания tentacle.

Требование пользователя для этого исправления: возврата маны от этих попаданий быть не должно. В `src/main/java/me/skywars2/game/LbcOverload.java` удалено `mana.add(p,300)` из обработчика попадания. Урон tentacle, эффекты насыщения/редкого лечения, лучи долга, прыжок поножей и slam сапог не менялись. Это намеренное отклонение от старого datapack ради заданного поведения.

## Проверки и пределы результата

Финальный адресный `ui-gameplay` smoke прошёл: 55,66 с, 2 матча, 19 именованных check-групп; SHA-256 JAR `afb96b4ec69ddd27499eed92cf2f6e5ba7cb1738df7d8e45fde9a77650c13b61`. Overload assertions проверили mana −25000: урон от двух лучей и три регенерационных тика без прыжка не уменьшили долг.

GUI/quickstart smoke PASS: 126,58 с, 3 матча, 7 сценариев; тот же JAR SHA `afb96b4ec69ddd27499eed92cf2f6e5ba7cb1738df7d8e45fde9a77650c13b61`, артефакт `build/verification/skywars2-ui-lobby-acid-afb96b4e.jar`. Общая 78-групповая регрессия не запускалась; JAR не установлен, на основном сервере активен `341c6293`. Реальный клиент не проверялся. См. [SERVER_TESTS.md](../SERVER_TESTS.md).
