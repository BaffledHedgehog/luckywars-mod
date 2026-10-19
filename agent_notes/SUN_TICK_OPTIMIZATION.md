# Sun Arcanum tick updates

`LbcKiuas` still checks mana and runs the Sun combat/visual pulse on its
existing five-tick cadence. A successful pulse opens a four-tick follow-up
window. While the Sun is held during that paid window, the template origin is
reconciled on every server tick, including when the pulse spent the holder's
last mana. Main hand and offhand mana branches remain separate; their identical
terrain work still shares one `BlockAreaWork` queue.

Small movement cannot be handled by the six new slabs alone. On an overlap,
the same world cell can map to a different local Sun template state, so each
one-block shift needs the changed local cells from the new template compared
with the old template at `local + delta`. These 26 shift lists are prepared
with the detached `MapManager.BlockPattern` in the existing
`prepareBlockPattern(...).thenCompose(...computeFromSnapshot(...))` worker continuation. The lists, map,
and local positions are immutable after preparation.

At runtime, unchanged bounds do not call `move` or detach/reindex. A one-block
shift calls `sun.move(nextBounds, false)`, preserving queued overlap work and
adding only the new bands; it then enqueues cached changed-local cells. A shift
larger than one block on any axis uses `sun.move(nextBounds, true)` for a full
rescan. Dirty block events enqueue corrections on each service tick during the
paid window. Once the window ends, item changes, removal, or pause do not plan
new structure work; already queued `BlockAreaWork` continues under the existing
budget when the aura is scheduled again. `stop()` still clears the queues.

The existing structure state application, including explicit template air,
and the original Sun item NBT remain unchanged. The `kiuResult` and
`kiuEligible` package-static helpers are retained for the neighboring
Fireblaze/Kiuaskivi work. No changes were made to `MapManager`,
`BlockAreaWork`, `GameChecks`, resources, or the build configuration. Per the
task boundary, the implementation agent performed no build or server run.
Root integration checks and artifact status are recorded in [SERVER_TESTS.md](../SERVER_TESTS.md).

Root review: paid windows belong to their respective hands; removing/changing
that hand clears its window. A different unpaid Sun cannot inherit it.
Stationary reactivation after a pause or expired window performs a fresh
coverage check, including changes that happened while indexing was inactive.
