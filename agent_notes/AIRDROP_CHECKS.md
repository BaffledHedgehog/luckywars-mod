# Airdrop checks

`GameChecks` adds an isolated live phase-two check for the source center airdrop. The setup stores a `GameConfigSnapshot`, then uses `stageOneSeconds=1`, `stageTwoSeconds=2`, and `stageTwoEvent=8` so the real controller transition selects event 7. The check verifies ten tagged native chicken carriers at source Y=110, one glowing native chest minecart passenger per chicken, source center/chest tags, nonempty native center loot with at least one stack count greater than one, and absence of the synthetic `minecraft:count` attribute modifier. A `SimpleContainer` fixture directly exercises `MapManager.normalizeLootCounts(Container)` and checks that it removes the synthetic modifier while preserving the sword's genuine attribute modifiers.

Root integration also calls native map-chest refill while the carts are present, checks ordinary generated chest counts and verifies no block chest appears at a cart's tagged position. The deterministic normalizer fixture asserts the actual count as well as retained attributes.

`airdrop-stop` calls the normal asynchronous match stop path. The runner waits for lobby and selected-map readiness before `airdrop-cleanup`, which asserts all carriers/carts are gone and probes the late-loaded stale-drop lobby whitelist. `restore` restores saved timings; the runner restores isolated settings and profiles in `finally`. Run with `python tools/smoke_airdrop.py --server-dir build/verification/key-reuse-server`. It reuses `smoke_game_cycle.Server`, copies the built JAR, records its SHA and waits for bounded async drop preparation.

Root build and final live verification passed: 58.73 seconds, one match,
seven unique groups. Both entities carry the existing item epoch tag; the
common load hook rejects an old epoch. Build/test/installed JAR SHA-256 is
`7f136cdbd3ec53c7795883cc713e8c2acdd0c593f0e8931095367f5f36440a4d`.
Both servers were left stopped. Detailed limits and the known fake-client
Polymer warning are recorded in [SERVER_TESTS.md](../SERVER_TESTS.md).
