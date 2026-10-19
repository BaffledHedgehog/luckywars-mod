# Map start bug audit

## Evidence from the live server

The inspected live config at `E:/servers_mew/skywars2_last/config/luckywars/game.json` had `gameMode: 2`, `mapId: "3"`, and `broken: false`. The log showed successful preparations for maps 4 and 22, followed by `skywars2.game.start_requirements`; map 3 later reached `Selected map ready: 3 broken=false`. The logs also showed two connected players, and map 22 was available with capacity 8. The start failure was therefore consistent with the mode guard, though the log did not record each `canStart` predicate.

The read-only client pack still translated `skywars2.game.map_selected` as
`Выбрана карта %s (%s). Загрузка при старте.` That caption contradicted the
successful asynchronous preparations recorded in the server log.

## Coverage gap

The old `tools/smoke_regressions.py` fixture forced `gameMode=1` and tested `sw start` only after preparation. Existing checks covered map loading and busy-state rejection, but not a start request made while preparation was in progress, nor the quickstart entity callback during that interval. The live server's `gameMode=2` condition was therefore absent from the reproducible regression.

## Current regression coverage

`RegressionChecks.run` now checks that mode 2 rejects `canStart` and that a rejected request preserves lobby stage, mode, and map selection. `RegressionChecks.startMenuSelection` drives the real map GUI, confirms the map is busy, keeps the menu usable, and makes one Start click. `RegressionChecks.quickstartDuringPreparation` invokes the registered `UseEntityCallback` on a tagged quickstart entity while a map is loading. `tools/smoke_regressions.py` waits for each queued operation to reach `COUNTDOWN -> PLAYING`, then stops the match before the next case.

Root ran these checks on the built JAR: PASS in 134.51 seconds, seven runner
groups / three matches. The repeated GUI selection placed map 2 only once;
both one-click requests reached PLAYING without a second request.
The quickstart test uses the actual tagged INTERACTION entity, not a text display.
Implementation keeps menus open during preparation and cancels a queued start
when the user manually changes the selection. Unsupported modes remain gated;
their dedicated error no longer misdiagnoses the map or player count.
[Current verification and deployment](../SERVER_TESTS.md).
[Manual resource-pack message replacement](MAP_MESSAGES_PATCH.md).
