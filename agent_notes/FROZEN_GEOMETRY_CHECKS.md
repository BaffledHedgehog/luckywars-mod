# Frozen geometry and action checks

## Target behavior (user)

Frozen entities are centered on their anchor block in X/Z (`floor(x)+0.5`,
`floor(z)+0.5`). The temporary ice shell covers the translated hitbox, with at
least one block on each axis. This gives small entities a one-block height,
players two blocks, and wider entities the number of cells their hitbox reaches.
While frozen, right-click and left-click gameplay actions are blocked; inventory
rearrangement remains available.

## Runtime checks

`/sw check frost` is the meaningful runtime fixture in
[`GameChecks.java`](../src/main/java/me/skywars2/game/GameChecks.java). It now
checks centering at negative coordinates, the ice envelope against the entity's
actual post-centering AABB for a small rabbit and a ravager, and the large
entity's save/unload/reload/clear lifecycle. The original shared cow cage still
checks that overlapping leases preserve container contents and restore the
original blocks only after the last owner clears. Frozen-player callback probes
exercise registered item, block, and entity use/attack callbacks. Direct packet
and native method probes cover item/block use, block destruction, entity attack,
release-use stopping, and an inventory swap that remains usable while frozen;
attack and destruction also have thawed positive controls.

The implementation and persistent lease state are in
[`LbcFrost.java`](../src/main/java/me/skywars2/game/LbcFrost.java); the action
guards are registered in [`SkyWars2.java`](../src/main/java/me/skywars2/SkyWars2.java)
and packet handling is in [`LobbyInventoryPacketMixin.java`](../src/main/java/me/skywars2/mixin/LobbyInventoryPacketMixin.java).
Root integration reused the existing native container-click packet helper for
inventory rearrangement and added direct `Player.attack`/`destroyBlock` positive
controls after thaw. `gradlew build` and `--frost-only` passed: 52.36 seconds,
one match, five unique groups. Full regression passed on the same JAR in
385.50 seconds: 26 matches, 74 completed scenarios, 78 unique check groups.
SHA-256 build/focused/full/install is
`d2072ac0d013a0f68256d3715aa4b406484cd15e992aef29dbc789b082d1b563`.
The main-server JAR was replaced after checking both servers were stopped;
the previous installed electric build is backed up. Both servers were left stopped.
Actual final results and installation: [SERVER_TESTS.md](../SERVER_TESTS.md).
