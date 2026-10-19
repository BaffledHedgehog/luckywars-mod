# Аудит сброса атрибутов игрока

> «все все аттрибуты возвращали базовые значения игроку (и все attribute modifiers удалялись) при старте (до выдачи перков и применения конфига естественно иначе они заигнорятся) и в конце катки»

## Текущее состояние реализации

В `GameController` добавлен registry-wide `resetAttributeValues`: он обходит зарегистрированные атрибуты, удаляет modifiers с каждого materialized instance и восстанавливает base через `AttributeMap.resetBaseValue(holder)`, используя registry default только как fallback. После этого очищается HUD атрибутов. Это учитывает не только известные LBC modifier IDs, но и прочие modifiers на зарегистрированных атрибутах. Источник: [GameController.java:712](../src/main/java/me/skywars2/game/GameController.java#L712).

Полный сброс вызывается в `resetPlayer(..., true)` после очистки предметов/effects и известных item/LBC состояний, но до config bases: `MAX_HEALTH` получает `config.health`, `ATTACK_SPEED` получает значение режима. `begin` использует этот путь до отсчёта и выдачи perks. Respawn намеренно вызывает `resetPlayer(..., false)`, сохраняя прежний lifecycle и не стирая матчевые значения как при новом старте. Далее `startPlaying` применяет loadout. Источники: [GameController.java:355](../src/main/java/me/skywars2/game/GameController.java#L355), [GameController.java:524](../src/main/java/me/skywars2/game/GameController.java#L524), [GameController.java:692](../src/main/java/me/skywars2/game/GameController.java#L692), [GameController.java:460](../src/main/java/me/skywars2/game/GameController.java#L460).

В `finish` после остановки предметных систем, LBC clear, очистки inventory/effects полный reset вызывается для каждого online игрока до перевода победителя в creative и остальных в spectator. В `restoreLobby` порядок такой: `items.stop()`, `spectators.clear()`, LBC clear, остановка events, переход в loading, затем loading handling, снятие effects и полный reset; он выполняется до асинхронного callback карты и включает developer-игроков. Lobby callback позже выполняет `lobbyPlayer`, где повторный полный reset задаёт lobby config. Источники: [GameController.java:551](../src/main/java/me/skywars2/game/GameController.java#L551), [GameController.java:588](../src/main/java/me/skywars2/game/GameController.java#L588), [GameController.java:624](../src/main/java/me/skywars2/game/GameController.java#L624).

LBC `clear` по-прежнему удаляет собственные catalog modifiers и эффекты адресно; общий reset не заменяет эту уборку её локальными lifecycle-задачами. LBC `apply` остаётся после старта и применяет новые perks поверх нормализованных bases. Nanobot snapshots восстанавливаются через `items.resetAttributes`/`items.stop` до общего reset на границах матча, поэтому сохранённый snapshot не задаёт итоговую базу поверх канонического сброса. Источники: [LbcLoadouts.java:220](../src/main/java/me/skywars2/game/LbcLoadouts.java#L220), [LbcLoadouts.java:276](../src/main/java/me/skywars2/game/LbcLoadouts.java#L276), [LbcItems.java:696](../src/main/java/me/skywars2/game/LbcItems.java#L696), [LbcItems.java:830](../src/main/java/me/skywars2/game/LbcItems.java#L830).

## Предыдущие findings — до исправления

Ниже кратко сохранены первоначальные выводы аудита, которые больше не описывают текущий GameController:

- Ранее `resetPlayer` сбрасывал явно только max health и attack speed, `LbcLoadouts.clear` удалял только catalog modifiers, а экипировка очищалась после этих helper-вызовов. Это было недостаточно для всех attributes/modifiers.
- Ранее полный reset на finish/stop ожидался только после async lobby callback, а developer path в `lobbyPlayer` возвращался до reset.
- Ранее `Spectators.detach` отдельно сбрасывал только max-health base; теперь его ограниченность компенсируется общим reset на границах матча, а не расширением `detach`.
- Предостережение про nanobot snapshots сохраняет смысл как свойство snapshot: он сам по себе хранит runtime bases; теперь на границах матча следующий registry-wide reset задаёт каноническое состояние.

Источники поведения до исправления: прежние точки теперь изменены в [GameController.java](../src/main/java/me/skywars2/game/GameController.java); `detach` остаётся частичным в [Spectators.java:83](../src/main/java/me/skywars2/game/Spectators.java#L83).

## Проверки и доказательность

Добавлены lifecycle fixtures `attribute-reset-start`, `attribute-reset-playing`, `attribute-reset-cancel`, `attribute-reset-finish`, `attribute-reset-lobby`. Они загрязняют все registry attributes persistent и transient modifiers/base values и проверяют сброс, config values, perk application, cancel/finish и developer inclusion. См. [GameChecks.java:4791](../src/main/java/me/skywars2/game/GameChecks.java#L4791), [GameChecks.java:4839](../src/main/java/me/skywars2/game/GameChecks.java#L4839).

Финальный адресный прогон `smoke_ui_gameplay.py --attribute-reset-only`
прошёл за **50,85 с: два матча / 11 check-вызовов**. Включены все пять
attribute-reset suites, повторный старт, native смерть/respawn и HUD.
SHA: `c54bb3140202e98de507d4f4ac70310c49d18732353f2d415796c710debf222d`.
[Отчёт](../build/verification/attribute-reset-results.json),
[лог](../build/verification/attribute-reset.log),
[сводка прогонов и установки](../SERVER_TESTS.md).

Ранние прогоны уже подтверждали сброс, но завершались в дополнительной
HUD-проверке: после пользовательского переноса шрифтов в `lbc` в тесте
оставались старый namespace и неверный путь. Ожидание исправлено на
`lbc:attribute_hud/row_*`, само пользовательское изменение Java сохранено.
Первый неуспешный отчёт сохранён отдельно. Применённые шрифты не воссоздавались;
старые TTF-ссылки пользователь исправил сам, временный скрипт удалён.

На том же JAR прошла регрессия UI/gameplay: **46,78 с, два матча / 19 check-групп**,
включая лобби, мановые эффекты, Overload, BrBad/кислоту и наблюдателей.
[Отчёт](../build/verification/ui-gameplay-results.json).

Существующие checks, чьи ожидания пересекаются с новым reset:

- `lbc-cleanup`: lobby max health 20 и attack damage без LBC бонуса — [GameChecks.java:617](../src/main/java/me/skywars2/game/GameChecks.java#L617).
- `lbc-advanced-cleanup`: lobby max health 20, armor base 0, toughness base 0 после stop — [GameChecks.java:3402](../src/main/java/me/skywars2/game/GameChecks.java#L3402).
- Nanobot tests проверяют сохранение custom fractional/negative bases в ходе trap и базовые значения после stop — [GameChecks.java:3192](../src/main/java/me/skywars2/game/GameChecks.java#L3192), [GameChecks.java:3412](../src/main/java/me/skywars2/game/GameChecks.java#L3412). Fixture важно запускать на игровом участке до полного lifecycle reset, иначе проверяется уже граница матча, а не snapshot механика.

Ограничение аудита: не проводил собственного запуска/сборки и не переизучал 26.3 native implementation `AttributeMap.sources`; текущий вывод о runtime опирается на переданный статус suites. Код использует публично видимый `resetBaseValue` и registry-wide итерацию, но точная внутренняя структура native sources отдельно здесь не подтверждена.
