# FPV drone native stage

Implements the author's target-free FPV rework of `drone_pult`. Original model
`lbc:drone`,16-float display matrix, native power8 explosion and sound IDs are
reused. Launch keeps the world-point reference `(0,100,0)`, flattened yaw; with
no target cursor the origin is now the caster. Offset is backwards48/up7 instead
of45/up5. No mana price or invented timeout. Each successful launch consumes1.

`LbcFpvDrone` owns a native20HP invisible nonpushing actor and its model. Flight
is1block/tick; the total three-dimensional turn is capped at3degrees/tick.
Time Stop pauses translation/steering. Shift detaches control and leaves straight
flight. Death causes exactly one shared native power8 blast, with captured WILD
author/epoch including delayed consequences. TECH destroys the actor without a
blast; MAG does not suppress mechanical flight. Sealed dimensions pause flight.

The actual ServerPlayer remains its body, with native gravity/knockback. A
controller-only passenger packet keeps vanilla look input flowing while its
client camera watches the drone; no fake server player or spectator conversion.
Camera attachment waits for actual native entity pairing. Voluntary movement,
item/attack/block/inventory actions are blocked while controlling. The existing
movement interceptor owns look/input; a competing interceptor found by the first
native regression was removed. Shift/death/forget/stop release owned control and
body tickets. Removed/replaced player references cannot re-add an obsolete native
player ticket or send an old entity ID to a replacement client.

Native ChunkMap tickets/views and entity tracking prefetch around the drone.
An independently reference-counted ticket keeps the physical body simulated.
Audible/visible range remains at the body until the camera attaches, then follows
the camera: vanilla sounds, particles and explosion packets, shared item sounds,
nearest black-hole/nuclear shader slots. Physical damage still uses body position.
Launch sound retains PLAYERS category; flight retains HOSTILE. No RP edits or new
assets required. Native actor NBT stores author/order/epoch; model reconstruction
on reload is implemented but the chunk-unload roundtrip is not runtime-qualified.

Exact candidate: `ce14cb5ea099ed176e37941c8a9119d45e0c52e86cdc4b84fda34ff703c39cca`.
`build/verification/author-decisions/fpv-inputs-ce14cb5e.json` freezes612sources,
1768classes,3411resources and build.gradle =5792inputs. Packaged bytes match,
except verified normal Loom nested-JAR metadata. `fpv-ce14cb5e-native.log`,
`fpv-ce14cb5e-native-results.json`, `fpv-post-native-audit.json` preserve proof.
6suites passed in1actualmode1match/31.05s: fpv-drone,time-stop,momentum-teleports,
item-sounds,void-arcanum,nuclear-visual. Native FPV checks cover pairing-before-
camera, separate body, remote view/tickets, real listener inputs,3degree turns,
paused movement, Shift,20HP shootdown/one blast, source sound categories/ranges,
particle range, forget and stop. Normal child shutdown/exact snapshotted disk
restoration.198authorpaths/31liveoverrides/questionfile/main d9087f0a unchanged.
Rejected candidates/logs remain available.

Limits: no physical-client FPV/latency/render qualification, actual login/player
replacement or chunk-unload roundtrip test; no peakTPS claim. First confirmed
correction stage is separately qualified on f94e5c58 (9suites), not represented as
15suites on one artifact. Full Witch acquisition/actors/Vanity rollback remain
unimplemented; original full25 Chaos release gate remains closed. No main install.
