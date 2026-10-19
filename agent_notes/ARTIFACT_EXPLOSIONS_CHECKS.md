# Artifact 1:2 explosion native checks

`GameChecks` exposes three operator suites for the source type-2 mystical artifact:

- `artifact-explosions-menu` verifies the source loot table, exact `mystical:1` / `type:2` custom data, tooltip defaults, Polymer projection, and menu access in the lobby.
- `artifact-explosions` runs during a live match. It confirms the native inventory index tracks inventory and offhand state, the native cursor and both crafting grids, and immediate loss after the final copy leaves those locations. It spawns every entity in the loaded `minecraft:unstable` tag and asserts the tag has eleven entries. Primed TNT uses a long fuse, and all probes have no gravity and zero motion so the fixture can call the interceptor directly without allowing a fuse to expire. Assertions preserve native `kill()` behavior: living types are expected to be dead, while nonliving types are expected to be removed; Creepers may still be present for their death animation.
- `artifact-explosions-cleanup` verifies the match stop removes artifact copies, holder state, and the tracked unstable-entity index.

The source type-2 behavior ignores the `time_stopped` tag, so the fixture verifies cleanup continues while that tag is present. The active fixture checks all eleven native unstable types at the shared radius, including the inclusive 24-block boundary, a vertical same-chunk entity, an out-of-range entity that moves into range, and a second-world entity. Arrow and snowball controls verify the native tag boundary. It exercises the per-holder query with many tagged entities and the snapshot/player-predicate query with one tagged entity and one eligible holder. Its eligibility cases cover a projectile owner, a shared scoreboard team, a type-1 artifact holder, a live Polymer beacon, an eligible living Creative participant, and an excluded Creative developer observer.

Run only this slice through `tools/smoke_lbc_loadouts.py --server-dir <isolated-server> --artifact-explosions-only`. The fixture intentionally keeps a real type-2 item on a bot until the runner stops the match, so the cleanup suite is non-vacuous.
