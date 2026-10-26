# Banhammer collision, mob bans and Carpet removal - 27 October2026

User requests: the ultimate must also remove mobs without drops, stop at the
first surface, keep the display above the ground and disconnect Carpet players.
Normal players already disconnect correctly. This extends the prior port's
player-only impact; it does not reproduce the legacy client-hanging particles.

## Source and corrected behavior

Legacy `lbc/function/items/swords/banhammer/rc_end.mcfunction:3` floors the target
Y and spawns the scale4 identity-transform display200 blocks higher.
`tick_fall.mcfunction:1-3` moves2 blocks while lifetime<=99, impacts at100 and
removes at200. The previous port moved on all100 ticks with no terrain sweep.
Legacy impact only handled players; no-drop mob bans are the new user request.

The author's read-only `assets/lbc/models/item/banhammer3d.json` has a head from
[0,0,4] to[16,7,12], with no display transform. Native NONE item rendering centers
at[8,8,8]; scale4 places its head at x[-2,2], y[-2,-0.25], z[-1,1] relative to the
display. Its bottom is two blocks below the display pivot. The previous final
pivot at the target floor buried the head. No model/RP adjustment is needed.

Each falling tick now sweeps that head AABB down2 blocks against native collision
shapes with `Shapes.collide(Axis.Y, ...)`. A tiny query extension detects exact
endpoint contact without moving farther than2. Slabs, thin surfaces and contact
at the head's edge stop the fall at their real shape height. The effect center
becomes the actual ground contact and the display pivot stays2 above it. Impact
occurs once on first contact; later ticks do not move the landed display. The
original aimed height no longer triggers an explosion through an earlier roof.
If terrain disappears, fall continues until a real contact or ordinary expiry.

Only resident chunks beneath the whole head footprint are queried. Missing
columns pause the clock without generating terrain. Dimension Time Stop still
pauses movement and age; viewers can track while paused. Match stop/epoch guards
cancel pending hammers; absolute age200 remains the display expiry.

Impact keeps its WILD spherical radius5.5. Eligible mobs in `minecraft:mobs` are
discarded directly, including allied and damage-invulnerable mobs: no health
change, death/loot/XP or kill rewards. Technical/spectator roles and absolute
active Greed remain protected. Players keep the existing normal disconnect,
skill12 and Greed protections, translated reason and achievement behavior.

The exact installed `fabric-carpet-26.3+v260915.jar` exposes
`carpet.patches.EntityPlayerMPFake.kill(Component)`. Its fake connection drops
send/disconnection callbacks, so sending a disconnect packet is insufficient.
For this optional fake-player class the mod invokes Carpet's own API: shake off
riders, then schedule native `onDisconnect(DisconnectionDetails)`. If that API
cannot be called, the fallback performs native disconnect cleanup for the fake.
Ordinary players continue through the existing network disconnect method.

## Verification

`BanhammerChecks` covers native charged input, full/slab/carpet surfaces inside
a two-block step, elevated first contact, head-edge contact, exact model bottom,
Time Stop and no further motion/repeated ban after landing. Direct mob checks
use an equipped, invulnerable million-HP cow and an allied cow; no death state,
loot, XP or kill rewards may change. Greed/technical/radius exclusions remain.
It creates a disposable real Carpet fake with the installed public API and real
FakeClientConnection, enrolls it through the native gamemode return path, then
requires native player-list/world removal after three actual server ticks.
The two ordinary fixture players are never disconnected by this check.

`SpecialToolChecks` retains normal native disconnect packets, translated reason,
self/ally impacts, skills, Greed, charge/mana and expiry assertions. Its former
fixed100tick contact premise is replaced by actual contact with the existing
source fall speed. The separate runner `tools/smoke_banhammer.py` also exercises
related native weapons, summon orders, player/projectile guards, Time Stop,
the previous visual-owner death repair and actual match-stop cleanup.

Local build/test/qualification evidence belongs to `build/verification/banhammer/`.
Native packets and server geometry do not establish physical client pixels or
real audio playback. Author RP/template and live grimoire overrides are preserved.

## Qualified release

Buildcc460bf8 (`cc460bf8fda209da0b2d24ff0a8915238e399b084374103d8ecad913c03666e4`)
passed28 recorded results in two actual mode1 matches (54.76s). Exact441 sources,
1220 classes and3140 resources match the tested JAR. Read-only source/RP checks
also validate all12 special-tool tables, four pools,25 keys and six models.
Native Carpet logs confirm two translated Banhammer disconnects; the fixture
also requires absence from both player list and level. No full main restart,
real-network client disconnect, rendered pixels/audio or peak TPS was observed.

Ready: `build/verification/skywars2-banhammer-cc460bf8.jar`. Immutable qualification,
logs/results, live `main-install.json` and cleanup receipt are in
`build/verification/banhammer/`. The isolated JVM stopped normally and restored
its original settings/profiles/scoreboard/JAR before qualification. Installer11424
adopted this release and waits for normal main shutdown/world unlock; main disk
remains1a8a3825. No forced stop/restart. All175 protected author paths and31 live
grimoire overrides remain unchanged; no RP/template change. Keep newestcc460bf8,
previous1b68c82a and installed1a8a3825; obsoletecbbb82ee retired after verifying
all retained hashes. Cumulative cleanup327 JARs /11667821568 bytes.
