# Regular Terra verification

Это исторический прогон JAR `341c6293…` со скоростью 2,1.
Последующая сверка исправила обычный Terra на исходные 1,8 блока/тик.
Актуальные проверки обоих вариантов — [TERRA_ACADEMY_CHECKS.md](TERRA_ACADEMY_CHECKS.md).

`tools/smoke_terra.py --server-dir <isolated-fabric-server>` runs the focused
regular Terra checks against the built `skywars2-0.1.0.jar`. The path is
required and the script refuses the project's adjacent server directory. It
copies the artifact into the isolated server's `mods/skywars2.jar`, records the
installed JAR SHA-256 and elapsed time in
`build/verification/terra-regular-results.json`, and restores the original
`config/luckywars/game.json` and `players.json` bytes in `finally`.

The smoke flow waits for both `LOADING -> LOBBY` and `Selected map ready: 1`,
checks the Terra item payload in the lobby, starts an actual match for the
projectile behavior check, then stops the match and checks cleanup after the
selected map is ready again. The focused Java checks are registered as
`terra-regular-menu`, `terra-regular`, and `terra-regular-cleanup` for
operator-only `/sw check` use.

## Результаты — 15 октября 2026

Root выполнил `gradlew build` и адресный smoke: **87,93 с, один матч,
три Terra-группы и создание двух нативных тестовых игроков**. Проверенный
JAR: `341c62937bdb6a042e44cf08074859af8c70084e46a021855a25faac7df68626`.

Подтверждены source payload и Polymer round-trip; расход 1000 в Survival,
Creative/developer и настоящий долг Overload; модель с нормализацией
однородной матрицы по `m33=3`; исходные травяные patches; swept piercing,
near miss, союзники/наблюдатели, стена и граница щита; остановка времени,
подавление, удаление при смене владельца/эпохи и stop cleanup.

Проверены попадание на X≈32000 при `noBorders`, полёт больше 421 рабочего
тика до цели в ~940 блоках в FULL-чанках вне entity-ticking, удаление
промаха в заранее доступном коридоре и незагруженный промежуточный чанк
при доступных концах отрезка. Подготовка использует существующий асинхронный
`MapManager.prepareChunk`; native ENTITY_LOAD получает время на активацию
до исполнения проверки. В самом полёте загрузчик не вызывается.

## Нагрузочный стенд

`tools/benchmark_terra.py --server-dir build/verification/key-reuse-server`
прошёл на том же JAR. Настоящий полёт 2,1 блока/тик, модель и исходные частицы
включены. Для каждого сценария проверено точное число снарядов и минимум
120 реальных тиков движения. Native `/tick query`: окно 100 тиков, 20 TPS.

| Снарядов | Среднее MSPT | P50 | P95 | P99 |
|---|---:|---:|---:|---:|
| 0 | 1,1 | 1,1 | 1,3 | 1,8 |
| 200 | 1,2 | 1,1 | 1,5 | 4,0 |
| 300 | 1,2 | 1,1 | 1,7 | 2,5 |

Это одиночные измерения на текущем компьютере с 16 логическими CPU;
линейное масштабирование и стоимость одного снаряда из них не выводятся.
Два fake ServerPlayer стоят возле запуска, одна цель далеко впереди;
плотные массовые попадания, реальная сеть и клиентский FPS не измерялись.
Принудительная загрузка коридора существует только в операторском стенде;
cleanup снимает лишь добавленные им билеты. Scripts восстанавливают config
и profiles и завершают собственный дочерний сервер. Перед изменениями
`require_stopped` проверяет Windows world lock, включая фазу сохранения.

Архивы этих результатов: `build/verification/terra-regular-results-341c6293.json`,
`build/verification/terra-benchmark-results-341c6293.json`. В логах остаются прежнее
предупреждение Polymer packet context для fake-подключений и предупреждения
legacy loot; отсутствие всех ERROR не заявляется. Настоящий клиентский вид
модели остаётся ручной проверкой с пользовательским ресурс-паком.
