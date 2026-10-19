# Tiny Electro checks

Уточнение 16 октября: отдельный native fixture проверяет шесть граней блока,
косое попадание (нормаль грани, а не обратный вектор стрелка), старт на блок
наружу и первый рабочий тик без уничтожения об исходную стену. Новая стена
по пути по-прежнему прекращает полёт. Контакт ровно на конце шага луча
проверяется теми же точными координатами граней. Отдельный entity endpoint
сохраняет точные yaw/pitch владельца и прежнее смещение точки старта.

Старые изолированные проверки проводимости, длины пересечения и щитов явно
задают прежнее направление после создания трассы; их значения урона,
исключения команд и остальные проверки не ослабляются. Проверки нового
block/entity запуска используют настоящий item callback без изменения трассы.
Результаты сборки и прогона: [SERVER_TESTS.md](../SERVER_TESTS.md).

The focused `--wands-electro-only` runner and full loadout smoke sequence include
Tiny Electro after Tiny Acid. Its three operator checks cover the source loot
payload, native item/model/Polymer roundtrip and 100-mana score; Survival,
Creative and developer Creative costs; empty-ray and insufficient-mana
behavior; water conductor pulses on owner and teammate despite disabled
scoreboard friendly-fire; observer immunity/rejection; native swept hitbox
damage and allied direct-hit immunity; solid stone termination, copper
conductor passthrough; the source arrow-only exclusion from Snow Gem procs,
with a positive melee control against the same target; hostile shield cancellation,
and a full Overload set
that still debits exactly 100 mana. They also check owner `time_stopped` pause
across 45 ticks, expiration within the 40 unpaused tick bound, global time-stop
cast rejection, wand rejection inside a real Polymer beacon without mana debit,
continued flight of an already emitted nonmagical arc inside that beacon, and removal of a live trace after
match stop. The fixture restores player state, team settings, temporary
entities, and bounded block edits.

Focused runtime verification passed on 15 October 2026: 55.24 seconds,
one match, three Electro checks plus the shared bot check (four unique groups).
The exact tested JAR SHA-256 is
`27bdb0dd319c5ca82a5d0cd9ee86ab204fff7744aae428f97fe109977b4b37fd`.
Artifacts: `build/verification/wands-electro.log` and
`build/verification/wands-electro-results.json`. Full regression and deployment
are recorded separately in [SERVER_TESTS.md](../SERVER_TESTS.md).

Full regression also passed on this exact final JAR: 388.65 seconds,
25 matches, 71 completed scenarios and 75 unique check groups.

The first runtime exposed packaging omissions: the conductor tag and narrow
damage tags must be included in the JAR. An additional fixture correction
uses a temporary stone contact before water: `#airs` makes water transparent
to the initial targeting ray. Water alone must not serve as the contact for
the cleanup sentinel. The suppression check creates a live trace before
adding its suppressor. Temporary trace diagnostics were removed before the
passing build. No artifact from a failed run was deployed.

Shield checks use yaw 180 / pitch 0 and remove the temporary contact wall
before ticking the trace: cancellation cannot pass just because of that wall.
The tests confirm four separate half-point hits against a cow before the
protective boundary, no direct hit against a cow behind it, and one conductor
pulse that originates before the boundary. This implements the user's
explicit first-boundary choice rather than legacy advancement/tick ordering.
