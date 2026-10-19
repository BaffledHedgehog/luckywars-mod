# Tiny Air, Stone, and Teleport checks

`GameChecks` adds `wands-second-menu`, `wands-second`, and `wands-second-cleanup`. The menu check rolls `lbc:ported/tiny_air_wand`, `tiny_stone_wand`, and `tiny_teleport_wand`, checks native IDs, markers, source model/tier metadata and Polymer carrot-on-a-stick conversion, and verifies the three `lbc.math` prices (250, 500, 750).

The match case calls the registered native item callbacks. Air checks insufficient funds, the previous-sample wall endpoint, the separate entity-plus-wall same-sample regression (one rocket only), source Fireworks component colors/fade/trail/twinkle and zero lifetime, then ticks the native rocket to verify hostile-mob damage while the allied player is protected. Stone checks no-op on insufficient funds and a single strict 27-block placement at a hostile target. Teleport checks insufficient funds, the source Levitation duration/amplifier on the mob, observer transparency/caster rejection, and the exact -750 Overload debit with a full native armor set.

The fixture limits block snapshots to the local test volume, requires its chunks already loaded, and restores each captured block state in `finally`. It discards newly created cows/fireworks and restores the caster's inventory/crafting/cursor/armor, both players' hands, position and motion, effects, health, fire/damage state, teams, observer abilities, mana, timers, and changed settings. The observer transition replaces the ally's remaining inventory and resets secondary player stats; this fixture runs immediately before match stop, which resets those test-player states. The cleanup group checks stop clears tracked projectiles and Levitation.

Source contract: `agent_notes/WANDS_SECOND_SOURCE.md`, derived from the read-only LBC files `function/workingitems/sauvojen_staffs_tiny/{air,air_2,air_end,stone,stone_2,stone_end,teleport,teleport_2,teleport_2_end}.mcfunction` and `function/workingitems/spell1_default[_left].mcfunction`. The implementation keeps the team's current friendly-target protection and one entity-first terminal callback when hit and wall coincide.

`tools/smoke_lbc_loadouts.py --wands-second-only` runs bots, menu, one match and cleanup in an isolated server; the default full runner adds this match after first-Wands cleanup. Static Python syntax validation does not establish runtime PASS; the parent agent will run the build and isolated server test.

Итоговая проверка: сборка прошла; адресный сценарий — 53,23 с, один матч,
четыре уникальные check-группы. Полный прогон — 330,89 с, 22 матча,
62 завершённых сценария / 66 уникальных check-групп, без FAIL.
Оба прогона и установка используют JAR SHA-256
`629a336f8cca9bc3b742453fc36ec1d0180f97bdfcb182ade3919e1cbeff7f23`.
Логи, промежуточная ошибка fixture и ограничения — [SERVER_TESTS.md](../SERVER_TESTS.md).
