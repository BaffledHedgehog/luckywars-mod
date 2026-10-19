# Порт четырёх trinket-эффектов

## Состав и исходные правила

Переносимая партия: ID **1 Blaze**, **2 Ghast**, **10 Intercal**, **16 Gneiss Stone**. Идентификатор в loot table — `custom_data.trinket`; предметы считаются по инвентарю в момент события, дубликаты меняют шанс. Source gate — глобальное `#lbctrinket >= 1`; исходные bow dispatch gates — `lbc:true_advancements/bows/blaze` и `lbc:true_advancements/bows/ghast`. Текущий мод проверяет `config.lbcTrinketLimit != 0` и активность участника матча. Bow/Crossbow procs получают один вызов на успешно созданный залп; multishot остаётся одним залпом и одним шансом.

| ID | Предмет мода; имя / модель; dispatch key | Действие и шанс |
|---:|---|---|
| 1 Blaze | `skywars2:blaze_trinket`; `lbctrinket.0` / `lbc:trinket_0`; `blaze` | На залп: 15% при одной копии; для 2–10 — 32,5% при двух копиях, затем +7,5 п.п. за каждую копию сверх второй (формула `17,5% + 7,5% × n`); от 11 гарантировано. Создаёт нативный SmallFireball. |
| 2 Ghast | `skywars2:ghast_trinket`; `lbctrinket.1` / `lbc:trinket_1`; `ghast` | На залп: 25% при одной копии; для 2–15 — 20% при двух копиях, затем +5 п.п. за каждую копию сверх второй (формула `10% + 5% × n`); от 16 гарантировано. Создаёт нативную FireworkRocket с пятью burst-взрывами. |
| 10 Intercal | `skywars2:intercal`; `lbctrinket.9` / `lbc:trinket_9`; `intercal` | На исходящем `player_hurt_entity` без `arrow_only`: `10% + 5% × копии` (15% с одной). Даёт Wither на 10 с подходящим не-союзным целям в радиусе 10; skill 15 добавляет исходный усиленный эффект. |
| 16 Gneiss Stone | `skywars2:gneiss_stone`; `lbctrinket.15` / обычный `minecraft:stone` без кастомной модели; `gneiss_stone` | При получении урона: `8% + 2% × копии` (10% с одной). Resistance V на 2 с, звук и частицы. |

ID 18 Snow Gem не входил в эту снарядную пачку: source запускает freeze по исходящему попаданию игрока не стрелой. Следующая пачка реализует его вместе с Ice Gem через общий cold/frozen: [FROST_PORT.md](FROST_PORT.md).

## Залп, снаряды и очистка

[`TrinketBowMixin.java`](../src/main/java/me/skywars2/mixin/TrinketBowMixin.java) оборачивает `Projectile.spawnProjectile` внутри `ProjectileWeaponItem.shoot`. `@Share LocalBooleanRef` запоминает успешное добавление хотя бы одного entity; `@Inject RETURN` вызывает `game.items().trinkets().shot(player)` один раз. Поэтому несколько стрел Multishot дают одну попытку Blaze и одну Ghast, а не несколько срабатываний по количеству снарядов или звуков. Детали сигнатур и descriptor зафиксированы в [PROJECTILE_API](TRINKETS_PROJECTILE_API.md).

Blaze задаёт начальную скорость как `floor(look × 1000) × 0.0016`, затем оставляет SmallFireball нативную физику: acceleration power `0.1`, воздушную инерцию `0.95`, без гравитации. Владельцем назначается стрелок для авторства попадания и native collision/damage.

> (user) «Да, назначать стрелка владельцем»

Ghast берёт готовый native preset из [`ghast_rocket.snbt`](../src/main/resources/skywars2/ghast_rocket.snbt): `LifeTime:100`, `ShotAtAngle:1b`, ровно пять burst-взрывов. Владельцем назначается игрок. Motion задаётся покомпонентно как `floor(look × 10000) × 0.0002`. Private lifetime задаётся загрузкой native NBT; accessor/reflection не нужен. Существующий сохранение/загрузка ValueInput описаны в API audit.

Полёт, столкновения, урон/взрыв и завершение жизни обоих снарядов нативные. Кастомной projectile-физики и тикового опроса инвентаря нет. [`LbcItems.java`](../src/main/java/me/skywars2/game/LbcItems.java) передаёт entity load/unload в trinket tracker, ставит текущую epoch-метку, снимает ссылку при unload и вызывает `trinkets.stop()` при очистке матча. Tracker хранит только загруженные trinket projectiles для очистки на stop; снаряды старой эпохи отбрасываются при повторной загрузке.

## Предметы и клиентская проверка

Реализации и Polymer registration находятся в [`LbcTrinkets.java`](../src/main/java/me/skywars2/game/LbcTrinkets.java); source-сопоставление ID, моделей, ключей и исходных шансов — в [TRINKETS_SOURCE_AUDIT](TRINKETS_SOURCE_AUDIT.md). Шаблоны resource pack не менялись. Сопоставление исходных имён, моделей и данных с datapack проверено. Отображение на настоящем Minecraft client пока не проверялось.

Финальный JAR прошёл полную серверную регрессию за 146,46 с: восемь матчей,
21 группа runner, включая настоящие выстрелы, Multishot, наблюдателей,
столкновения, сохранение возраста ракеты, респаун и очистку старой эпохи.
Сборка установлена с резервной копией; SHA и точные логи —
[SERVER_TESTS.md](../SERVER_TESTS.md). CPU под нагрузкой не измерялся.





