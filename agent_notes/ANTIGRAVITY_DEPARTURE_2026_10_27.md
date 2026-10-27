# Antigravity: remove lingering Slow Falling after departure

The author reported a slow-fall feeling after jumping/walking off a lifted
piece, and mentioned occasional missed pickup as a secondary issue. The cause
of the slow fall was explicit: every successfully carried entity received a
hidden Slow Falling I effect for30ticks on every carrier update. It could remain
for another1.5seconds at20TPS after detachment. The earlier velocity repair kept
this inherited behavior; this patch supersedes that part of its contract.

`LbcAntigravity.tick` now resets fall distance directly while an entity is carried,
instead of granting any potion. Players already had a reset in their motion
handler; the common reset keeps NPC protection too. Departure preserves native
motion/gravity/drag and ordinary accumulated falling distance thereafter. No
effect is forcibly removed, so legitimate Slow Falling from potions, commands
or other items remains active. Existing effect layers are not touched.

Carrier/admission geometry, latency compensation, jump/release guards, XZ/input,
camera, lifetime and Time Stop are unchanged. The secondary report of rare
non-pickup has no verified reproducer here and is not claimed fixed by this patch.
There are no new teleports, client collision walls, queues, scans or RP assets.

## Native evidence

Offline build and32recorded suite results passed in two actual mode1matches,
65.35seconds. Native rider tests verify no hidden Slow Falling during carry,
complete native-listener departure without carry packets and preservation of an
external200tick effect. A real cow rides40native movement steps, leaves the
footprint and immediately resumes ordinary falling: first Y velocity
`-0.08*0.98`, then the corresponding downward displacement next native tick.
Existing catch/transfer,0/200ms bounded latency, free input, pending teleport,
Greed/frost, physical ceiling, source exhaustion and Time Stop tests still pass.
The run also covers player rules, gun accuracy, TPS/FOV and effect layers.

Build **46034d3c**, full SHA256:
`46034d3c05f98e1b8793488b488234d7c3becfe01409ade528f16d0fc9ac434a`.
Artifact: `build/verification/skywars2-antigravity-departure-46034d3c.jar`.
Exact523Java sources,1482classes,3224resources;195preexisting dirty author paths
and31live grimoire overrides retain their bytes/existence. Child shutdown and
exact JAR/config/player/scoreboard restoration passed. This remains bounded
native/wire simulation, not a physical-client or arbitrary network/TPS claim.
Evidence is in `build/verification/antigravity-departure/`.

Installer PID15932 adopts this cumulative build in place of queued23c9e577 and
waits for the running main23a25d43server's normal shutdown. No stop or restart is
forced. The current receipt is `build/verification/antigravity-departure/main-install.json`.
No RP update is needed for this patch; the previous warning-gradient shader
instructions still apply to that separate feature.
