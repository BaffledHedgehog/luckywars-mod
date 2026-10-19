# Порт зелий маны: фактический контракт

Область переноса: пять семейств и три нативные формы доставки. Crux, отрицательные боевые стадии, перегруженная/Nyauwu-броня и заклинания остаются следующими этапами; обычные mage-бонусы регенерации уже входят в [ядро маны](MANA_PORT.md).

## Предметы и payload

В `LbcMana` зарегистрированы три серверных Polymer-типа: `skywars2:mana_potion`, `skywars2:mana_splash_potion`, `skywars2:mana_lingering_potion`. Они используют обычные vanilla `PotionItem`, `SplashPotionItem`, `LingeringPotionItem` и показываются клиенту как соответствующие vanilla items. Drink сохраняет обычное consumable-поведение и возвращает стеклянную бутылку; lingering несёт стандартный scale `0.25`. Реальные эффекты лежат в `minecraft:raid_omen`, типизированы amplifier, а не отдельными кастомными potion effect IDs.

В 26.3 модель задаётся через `Properties.effectiveModel()`: финальная инициализация Minecraft перезаписывает ранний `ITEM_MODEL`. Поэтому три типа получают прежние модели vanilla-зелий, а не отсутствующие `skywars2:*` модели. Условия удачи обычных записей переведены в эквивалентный `int_value_check` с `test: 10` (равенство); тексты, lore, цвет, оформление и веса сохранены.

| Amplifier | Семейство / pulse | Таймер |
|---:|---|---|
| 16 | temporary mana boost: `manaregen_visual = floorDiv(visual * 3, 2)` | `effect_boost_mana` |
| 17 | `manamax += 2500` | `effect_add_manamax` |
| 18 | `manaregen += 5` | `effect_add_manaregen` |
| 19 | `mana += 10000` | `effect_add_mana` |
| 20 | всегда `mana -= 2000`; общий случайный roll: при 1–8 ещё `manamax -= 1000`, при 1–6 ещё `manaregen -= 1` | `effect_remove_mana` |

Ресурсный каталог содержит 15 таблиц: пять семейств × drink/splash/lingering. В них 39 записей: 30 imba-записей (по длительностям 3600 и 9600 ticks для каждого семейства и формы) плюс все 9 реальных ordinary-записей из источника. Эти 9 записей дают пять payload-вариантов: у четырёх drink-семейств сохранены обе записи (weight 10 и weight 1000 с source condition), у `remove_mana` — одна ordinary lingering запись. Отсутствующие ordinary формы и варианты не синтезированы. Все 15 путей включены в `processResources` allowlist.

## Захват эффекта и порядок пульсов

Mixin слушает native добавление/обновление/удаление `RAID_OMEN` на `LivingEntity`; callback также подключён к trigger исходного `effects_changed`. Для amplifier 16–20 сущность попадает в `pendingPotions`. Если обычный stat sentinel длится один tick, callback немедленно переносит его остаток в scoreboard до истечения vanilla-эффекта. Иначе перевод делается на mana tick. Остаток `duration == 2` нормализуется до 1, как у короткого native/cloud эффекта; native `RAID_OMEN` после перевода снимается. Повторные применения добавляют ticks к соответствующему таймеру.

Для игроков каждый `LbcMana.tick()` сначала вызывает `tickPlayer`: `consumePotion`, затем `tickPotions`, затем обычную regen-часть. Порядок `tickPotions` задан индексами `{3,1,2,4}` и соответствует **19 → 17 → 18 → 20**. Поэтому постоянные изменения статов и удаление маны происходят до regen этого же игрового tick. Каждый пульс расходует 1 timer tick. Amplifier 16 применяется в regen-части и его счётчик также теряет 1 за активный mana tick. Для мобов pending effects переводятся после player loop, а stat timers обрабатываются в этом же `LbcMana.tick()` после игроков.

## Паузы, отмена и очистка

- `time_stopped` останавливает импорт native-сентинела, custom pulse timers, player regen и секундный `reverse_mana`. Vanilla duration самого `RAID_OMEN` продолжает убывать: при callback на последнем тике импорт отвергается, а после нативного истечения pending-запись очищается. Поэтому эффект может истечь во время time stop и не обязан восстановиться после него.
- `sin_greed_active` ставит на паузу только amplifier 20: плохой pulse и уменьшение его таймера пропускаются. Таймеры 16–19 продолжают идти.
- При `lbcSkills` и skill 19 импорт любого sentinel после обработки сбрасывает `effect_remove_mana` в ноль. Это отмена всего его активного timer, не пауза.
- Greed не ставит amp 16 на паузу. Его timer учитывается в regen-пути, поэтому magic suppression/beacon останавливает его преобразование regen и расход timer; custom pulse 17–20 проходит отдельно от этого regen gate.
- Bad-mana family использует один roll на оба вероятностных изменения: потеря regen (roll ≤6) всегда сопровождается потерей max mana (roll ≤8); вероятности не независимы.
- `clearEffects` обнуляет пять timer scores и `reverse_mana`, удаляет pending/tracked ссылки, не откатывая `mana`, `manamax` и base `manaregen`. Death/respawn и полная очистка эффектов идут через `LbcItems.clear`; матчевый `resetResources` отдельно выставляет стартовые ресурсы. Не считать временный timer и постоянную потерю max/regen одним очищаемым статусом.

## Сущности и предел CPU-выводов

Runtime-hook принимает `LivingEntity`, а `effectActor` ограничивает pulse живой, не удалённой сущностью игровой арены; для игрока дополнительно требуется gameplay participant, для spectator ресурсы не обрабатываются. `pendingPotions` и `statEffects` — отдельные множества; tick обходит всех онлайн-игроков и только отслеживаемые сущности для дополнительных timer-работ, без нового полного сканирования мира. Загрузка и выгрузка сущностей связаны: `LbcItems.entityLoaded/entityUnloaded` вызывают `LbcMagic.loaded/unloaded`, которые передают living entity в `LbcMana.loaded/unloaded`; `LbcMagic.refreshEntityIndexes` также повторно индексирует живых сущностей в `LbcMana`. **CPU benchmark не выполнялся.**

Регенерация и расход boost 16 выполняются для игроков. У мобов обрабатываются статтаймеры 17–20; отдельный цикл регенерации маны для всех мобов не добавлен.

## Ссылки

- Реализация: [`LbcMana.java`](../src/main/java/me/skywars2/game/LbcMana.java), [`LbcItems.java`](../src/main/java/me/skywars2/game/LbcItems.java), [`LbcColorEffectsMixin.java`](../src/main/java/me/skywars2/mixin/LbcColorEffectsMixin.java), [`LbcEffectTriggerMixin.java`](../src/main/java/me/skywars2/mixin/LbcEffectTriggerMixin.java), [`GameController.java`](../src/main/java/me/skywars2/game/GameController.java), `build.gradle`.
- Payload и source mapping: [`MANA_ITEMS_RESOURCES.md`](MANA_ITEMS_RESOURCES.md), [`MANA_ITEMS_SOURCE.md`](MANA_ITEMS_SOURCE.md), [`MANA_POTIONS_CHECKS.md`](MANA_POTIONS_CHECKS.md).
- Источники LBC: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/imba/potions.json` и `transformables_potion.json`; runtime-порядок `data/lbc/function/projectiles/arrows2.mcfunction`, `mobs_tick.mcfunction`, `effects/tick.mcfunction`, `effects/effect_add_manamax.mcfunction`, `effect_add_manaregen.mcfunction`, `effect_add_mana.mcfunction`, `effect_remove_mana.mcfunction`, `other/mana_potions/bad.mcfunction`.

**Проверки:** адресный сценарий `--mana-potions-only` прошёл за 57,20 с: четыре группы, один матч. Полный прогон на том же JAR прошёл за 313,34 с: 53 группы, 19 матчей. Точные компоненты 39 исходных записей сверены отдельно. SHA, логи и состояние установки — [SERVER_TESTS.md](../SERVER_TESTS.md). Графический клиент и CPU benchmark не проверялись; внешний ресурспак, переводы и template не изменялись.
