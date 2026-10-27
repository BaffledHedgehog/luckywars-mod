# Antigravity velocity carry ? 27 October 2026

## User contract

> ?????????? ???????????? - ????? (user).

Carry must use upward velocity with gravity/drag/latency compensation, preserve
horizontal input and free camera rotation, slightly compensate horizontal drag,
catch a descending player on an already moving piece, run for60seconds, and retire
an exhausted source center without waiting for future terrain (user).

This supersedes the earlier relative-position transport in
[the previous repair](ANTIGRAVITY_INPUT_FIX_2026_10_27.md). That historical document
is evidence of the old implementation, not the current transport contract.

## Implementation and native cause

`LbcAntigravity` now sends ordinary `ClientboundSetEntityMotionPacket` to the
carried player. It never changes player position, rotation, native teleport ACK
state, or movement packet coordinates. Native air travel moves first, then applies
gravity and drag. A bounded feedback controller predicts the age of positional
reports from one-way RTT and compensates gravity/drag without snapping feet.
Known movement is accepted client displacement, not a second absolute velocity.
The horizontal estimate restores a small part of air drag (factor1.04), avoids
compounding the service's old impulse, and preserves stronger new external motion.

Player carry adds no server-only collision walls: the client cannot see those
walls, and delayed positions inside them caused XZ rejection. NPCs retain actual
moving shape collision. Native pending teleports, jump input, Greed, frost,
roles, lifecycle and Time Stop remain independent guards. Carry clears fall
accumulation and retains existing slow-fall protection and author attribution.

A native move-listener observation hook sees the original movement packet and
records swept relative crossings against real captured top surfaces. It returns
the same packet object, so XYZ and camera input remain unchanged. Falling contact
may recover a measured below-surface debt by upward velocity. Leaving one piece
and entering another is processed in the same report; old upward carry cannot
cancel admission before the new piece applies its first impulse.

Duration is1200active ticks. Sparse resident-column terrain indexing is rebuilt
once before confirming exhaustion; stale entries and new source columns are
handled without loading chunks. Only the source32x32footprint keeps a center
alive. Already flying pieces have their own lifecycle after center retirement.

## Verification

Final artifact and native qualification results are recorded after the run in
`build/verification/antigravity-velocity/qualification.json` and
`build/verification/antigravity-velocity-gameplay-results.json`.
Native checks exercise actual listener handling and encoded/decoded wire motion,
original XYZ/look acceptance, free partial/full footprint departure, descending
contact, fast below-surface reports, two-platform transfer, shared guards and
1200-tick duration/source exhaustion. Persistent100-tick motion models cover
zero and200ms constant RTT using the native move-before-drag order.

These are bounded native-listener/wire models, not physical-client rendering,
variable-latency or peakTPS measurements. No client mod or RP change is needed.

Build `ca04e049` passed 32 results in two actual mode1 matches in
74.05 seconds. Normal child shutdown and exact snapshot restoration passed.
Exact artifact comparison:517Java sources,1465classes,3221processed resources;
175protected author paths and31live grimoire overrides unchanged. Automatic
installer adopted this cumulative build; main6890ae59 still waits for normal
shutdown. Superseded0a6a5f3fready archive was removed after confirmed adoption.
