# Проверки полной невидимости

Последняя проверка — **16 октября 2026**, исправление pickup crash:
build и isolated smoke прошли за **41,48 с** на
`e7f12760f1914677a1178c1bcfe9db1b4599ed60ac3a193c7f72d42d7345809d`.
Проверяются native pickup codec для внешнего зрителя/владельца/камеры,
возврат обычного collector ID при снятии полной невидимости и настоящий
подбор в инвентарь. Оба набора full-invisibility/observer-match и cleanup
прошли. Графического клиента не было; основной работающий сервер не обновлён.
Результат `build/verification/full-invisibility-results.json` относится
к этой сборке; ниже сохранены результаты предыдущих этапов.

После правки немедленной очистки артефактами тот же сценарий повторно
прошёл за **56,99 с, один матч** на `67c7a14aff86aedd82592f88019b1af24336e204ef0f52a1224fa1a61b52120c`.
Отчёт `build/verification/full-invisibility-results.json` теперь относится
к этой сборке; описание ниже сохраняет результат исходного этапа невидимости.
Подробности новой проверки и готового JAR — [SERVER_TESTS.md](../SERVER_TESTS.md).

Фактический результат root: build и isolated-прогон прошли **15 октября 2026**,
**66,91 с, один матч**, JAR SHA-256
`05e563330502031fef9c6b2ef06e38c0e6e3c2537d59a4d03342d66940078dd7`.
Отчёт: `build/verification/full-invisibility-results.json`.
Основной JAR не заменён. Ниже перечислено покрытие серверных фикстур;
визуального теста с подключёнными клиентами не было.

Финальный прогон также проверяет ровно одно использование vanilla снежка
через штатный Polymer fallback после ПКМ по proxy и native client-tick-end.
Отдельный `observer-match` выполняется с эффектом III на цели: настоящий
наблюдатель входит ПКМ, получает обычный вид камеры, копии инвентаря/эффектов
и выходит без изменения игровой роли цели. Оба набора и lobby cleanup прошли.

Реальный игрок сидит в native minecart во время проверок урона и естественного
истечения. Passenger packet проходит encode/decode: внешний зритель не
получает client mount для Interaction, владелец и камера получают настоящий
rider ID; серверная связь с транспортом сохраняется во всех трёх случаях.

`sw check full-invisibility` is an operator-only live-match check. It exercises the registered Polymer overlay on the existing `TestAlpha`/`TestBeta` native `ServerPlayer` fixtures. It covers the level III threshold for self and another viewer, Polymer-context spawn and metadata codec roundtrips retaining the real player entity id, decoded Interaction visibility/pickability/fire/glow and dimensions, hidden equipment, camera spectating, saved-effect entity reload, downgrade to level II, clear, standing/crouching and scale refreshes, a native `ServerboundAttackPacket`, projectile damage-source damage, generic damage, and natural effect expiry.

Run the isolated smoke runner with:

```powershell
python tools/smoke_invisibility.py --server-dir E:\path\to\isolated-server
```

The runner requires a stopped isolated Fabric server with `fabric-server-launch.jar`, uses the existing built mod artifact, saves/restores `config/luckywars/game.json` and `players.json`, starts the server, creates the test players, starts a match, runs the check, stops the match, checks cleanup, and shuts down. Its result record is `build/verification/full-invisibility-results.json` and includes the tested jar SHA-256.

The fixtures have no client sockets. They exercise the native spawn/metadata codecs under Polymer `PacketContext` and invoke the native attack packet handler directly. They do not prove that a connected vanilla client decodes those packets. The projectile check covers the native projectile damage source, not collision tracing.
