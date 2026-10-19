# Academy Terra verification

`tools/smoke_terra.py --server-dir <isolated-fabric-server> --academy` runs the
Academy Terra menu and match behavior checks. `--both` runs the regular and
Academy suites in separate matches on the same isolated server. Both paths
reuse the existing native fixture and projectile assertions in `GameChecks`.

The Academy menu check validates the `academy_terra_wand` loot table, native
item identity, shared `lbc:st/terra` item model, tier A, the upgraded Terra
custom data, the appended green `upgraded_item` lore line, Polymer restoration,
and the shared 1000-mana score. The match check verifies the upgraded wand uses
its native callback, then applies the same piercing, damage target, ally,
observer, shield, wall, pause, suppression, chunk, and lifecycle checks as
regular Terra. Variant-specific assertions cover 7.5-block movement, 17 cactus
damage, Academy projectile model, and native flower growth.

`tools/benchmark_terra.py --server-dir <isolated-fabric-server> --academy`
selects the Academy load fixture. It waits for 120 real movement ticks at
7.5 blocks/tick before each native 100-tick MSPT sample. The corridor and its
forward target are extended to keep charges in flight through the sample.
Reports use variant-specific names: `terra-academy-results.json` for smoke and
`terra-academy-benchmark-results.json` for the benchmark. Regular runs retain
their existing report names; `--both` writes `terra-both-results.json`.

These scripts restore server configuration and profiles and require an
isolated stopped server.

## Адресный прогон — 15 октября 2026

Root выполнил `gradlew build` и `smoke_terra.py --both`:
**90,14 с, два матча**, regular/Academy payload, combat и cleanup плюс bots.
SHA-256: `5cc324db83245ed4f407b6e1585e4c8246d1e1999e2b85a6d59217c237c76b2d`.
Отчёт: `build/verification/terra-both-results.json`.

Проверены обе реальные скорости (1,8 и 7,5), урон (8 и 17), Polymer
round-trip, модель/матрица, source lore/tier A, общий расход и Overload,
native callbacks и legacy-маска `upgraded:1`, трава/цветы, пробивание,
near miss, союзники/наблюдатели, щиты, пауза/подавление, owner/epoch/stop.
Оба заряда долетают до цели в ~940 блоках в FULL вне entity-ticking.
Промах удаляется внутри доступного коридора; незагруженный промежуточный
участок не подгружается даже при доступных концах отрезка.

Проверка X≈32000 выявила и закрыла баг общего полёта: доступная часть шага
раньше полностью отменялась при недоступном конце. Теперь единый native DDA
обрезает путь на первом недоступном чанке, сохраняет попадания до границы
и не читает состояние блока без `hasChunkAt`. Новый артефакт прошёл обе
проверки; ошибка тестом не обходилась дополнительной подгрузкой конца.

## Нагрузка на том же JAR

Обычный вариант: **89,20 с**; Academy: **108,71 с**.
Native `/tick query`, окна по 100 тиков, 20 TPS во всех сценариях.
Проверены точное число снарядов и минимум 120 настоящих тиков движения.

| Вариант | Зарядов | Среднее MSPT | P95 | P99 |
|---|---:|---:|---:|---:|
| Regular, 1,8 | 0 | 1,8 | 2,8 | 3,6 |
| Regular, 1,8 | 200 | 2,1 | 3,1 | 3,7 |
| Regular, 1,8 | 300 | 1,7 | 2,2 | 4,0 |
| Academy, 7,5 | 0 | 1,4 | 2,1 | 3,2 |
| Academy, 7,5 | 200 | 1,3 | 1,5 | 1,9 |
| Academy, 7,5 | 300 | 1,5 | 2,2 | 2,4 |

Основной сервер параллельно работал на том же ПК. Это одиночные измерения
с разной фоновой нагрузкой и размером загруженного коридора: разность строк
не показывает стоимость одного снаряда или преимущество одного варианта.
Модель и частицы включены; два fake-подключения стоят возле запуска,
далёкая цель — Z=850/2200. Плотные массовые попадания не измерялись.
Реальный клиентский визуал, сеть и FPS этими подключениями не проверяются.
Известные legacy loot warnings и Polymer packet-context ERROR остаются.

Отчёты: `build/verification/terra-benchmark-results.json`,
`build/verification/terra-academy-benchmark-results.json`.
Оба стенда штатно остановлены; добавленные force-load билеты сняты,
config/profiles восстановлены. Предыдущий замер на скорости 2,1 хранится
в отдельном архиве `terra-benchmark-results-341c6293.json`.
