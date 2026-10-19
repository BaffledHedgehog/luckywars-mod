# Pinegun native check fixture

First-frame orientation regression (16 October): both ammo tiers fire toward
four yaw/pitch pairs, including upward/downward and diagonal shots. Every arrow
is inspected at tickCount=0: render rotations at partial ticks 0/0.5/1 must
match its actual spread velocity. The initial native spawn packet must encode
the same angles within the vanilla byte-angle quantization tolerance. These
checks run before an Arrow.tick can correct a bad initial angle. Client visual
confirmation remains a separate check; current runtime results are recorded in
[SERVER_TESTS.md](../SERVER_TESTS.md).

`GameChecks` exposes three operator suites for the Pinegun port:

- `pinegun-menu` runs in the ready lobby. It rolls `lbc:ported/razorpine` and checks the one Pinegun plus sixteen wheat seeds, including the Polymer client representation.
- `pinegun` runs in a live match. It starts each shot through the real use-item packet, then advances the shooter's public tick method to check the six-volley schedule. It covers best and ordinary ammo, ammo preference and consumption, repeated right-click and punch while a burst is active, two held Pineguns, mana neutrality (including a negative balance), offhand configuration, missing ammo, Creative/developer, frozen, time-stopped, suppressed, and observer states. A native arrow is sent through vanilla entity collision to check health loss and owner attribution.
- `pinegun-cleanup` runs after match stop and checks that no burst queue or tracked Pinegun arrow remains.

The checked behavior follows the agreed port contract: one round starts six volleys at two-tick intervals; ordinary arrows use base damage `0.5` and velocity components around the aim vector at speed `2`, best-ammo arrows use base damage `1.5` and speed `5`, and two held Pineguns double each volley while spending one round. Spruce leaves/saplings take priority over ordinary tagged ammo. No mana is spent, and Creative mode or the `developer` tag uses the same ammo and volley rules.

Use the isolated runner with the newly built mod jar:

```powershell
python tools/smoke_terra.py --server-dir <isolated-server-directory> --pine
```

The runner refuses the main server directory, starts the isolated match, executes the menu/combat/cleanup checks, and restores the isolated server's game and player settings. Run the menu and combat checks only after the Pinegun native implementation has been compiled into `build/libs/skywars2-0.1.0.jar`.

Root integration extended the fixture to cover native team/observer predicates,
an actual Arrow.tick entity hit with owner attribution, and native block-hit
retirement. A second real Punch-driven normal burst confirms 36 arrows for
two held guns. The fixture leaves 42 real arrows and an unfinished paid burst
for the following actual match stop, so cleanup is checked with live state.

Polymer 0.18.2 retains an interaction result until
`ServerboundClientTickEndPacket`, not `ServerGamePacketListenerImpl.tick()`.
The first synchronous fixture failed on subsequent UseItem packets because
only the weapon queue clock had advanced. `pineUsePacket` now sends the same
client tick-end packet used by existing projectile fixtures before the next
native UseItem. Production input processing and Polymer guards were not
bypassed. The serialization of an arrow's base damage is test-only; runtime
does not serialize arrow NBT every tick. Graphical client visuals and a new
CPU benchmark are outside this server verification.
