# Antigravity input and floor repair,27 October2026

## Confirmed cause

Minecraft26.3 ClientPacketListener.handleTeleportEntity enables interpolation
when any relative position axis is set. The previous self transport used
Relative.X/Z, so it fed a complete position/rotation target into the player's
SteppedInterpolationHandler. Its two-step interpolation writes both setPos and
setRot; repeated targets lag the rising block and interfere with local input.
Calculating the right relative coordinates alone did not test this handler path.

The old support lookup also capped surfaces against the player's current Y.
When already a block inside a stacked piece, it could select the lower layer or
lose support. Admitted players now retain their surface's local height, independent
of delayed client Y, with bounded recovery. Initial contact, footprint departure,
jump/ceiling/role/Greed/Time Stop and epoch guards remain.

## Transport

The owner receives a raw ClientboundPlayerPositionPacket with absoluteY and zero
Yvelocity, relative-zero X/Z, DELTA_X/Z and yaw/pitch. The actual native client
handler applies this packet immediately, without entity-pose interpolation, and
preserves the client's newer horizontal position/momentum/rotation. The server
still moves vertically through native collision and clears fall debt.

Native connection.teleport is deliberately not called: no awaiting position is
installed, so ordinary XZ packets are never gated on the lift ACK. Reserved ID
Integer.MIN_VALUE is outside native nonnegative teleport IDs. Native ACK handling
ignores this ID; delayed/out-of-order lift ACKs cannot change position or consume
a genuine pending teleport. A narrow listener mixin exposes only whether a real
teleport is pending. Pending/loading players detach before any lift packet.
Existing native movement/collision validation is retained, without a blanket
anti-cheat or wall-collision bypass.

## Known native-client limitation

Player-position handling also calls BlockStatePredictionHandler.onTeleport and
gameMode.stopDestroyBlock unconditionally. Continuous client mining on the moving
piece may therefore be interrupted. This side effect was communicated before
qualification; no continuous-mining preservation is claimed. The immediate packet
was selected to fulfill free XZ/camera and precise vertical support without a
client mod or native ACK lock. Actual Minecraft render frames remain unobserved.

## Native verification

Build7a747d86 passed32 recorded suite results in two actual mode1 matches
(60.42s). Each match includes the strengthened support checks, full30-second
storm/native mob/ceiling/no-hover/debit/terrain checks, actual-stop display cleanup,
player rules, Time Stop/contacts, TPS compensation, rifle accuracy/FOV and effect
layers, plus real native menus. One initial focused support run also passed.

The new support fixture retains the SAME native listener for100 explicit native
listener/client-tick boundaries: changing X/Z and yaw/pitch, alternating PosRot and
Pos+Rot packets, clientY up to1.25blocks stale, and delayed/out-of-order lift ACKs.
Feet stay on the highest captured surface, horizontal/look requests are accepted,
no genuine correction or pending ACK gate appears. Wire round trips cover arbitrary
newer client state. A stacked18-cell piece recovers a whole-block penetration.
A genuine teleport remains untouched by lift ticks/negative ACKs, and its ordinary
positive ACK still completes normally. These are native explicit fixture steps,
not100 wall-clock frames or a physical-client recording.

Exact tested artifact: `build/verification/skywars2-antigravity-input-7a747d86.jar`.
SHA256: `7a747d8654dab4780967abcf9fb77f4eba24a329f50fe52901027c1b979e3542`.
All506sources/1444classes/3215resources match the tested JAR. All175protected
author paths and31live grimoire override files retain exact hashes/deletions.
No RP or attribute-policy changes. Owned test JVMs stopped normally and restored
original fixture snapshots exactly. No peakTPS or physical-client claim.

Evidence: `build/verification/antigravity-input/qualification.json`,
`build/verification/antigravity-input-gameplay*` and focused evidence in that folder.

## Installation and retention

The safe installer adopts this release instead of map-only8624ee1c; prior fixes
are included. Main735d9905 is running, so installation waits for normal shutdown
and world-lock release, without forced stop/restart. Receipt:
`build/verification/antigravity-input/main-install.json`.
Keep current ready7a747d86, preceding qualified8624ee1c and exact installed735d9905
rollback. Obsolete16502e73/25d7466b/0c952772 cleanup is scheduled only after verified
installation and exact parent/non-reparse/SHA/size/mtime checks. Small proof logs
and manifests remain. Earlier entity-transport/no-mining-reset claims in
ANTIGRAVITY_SUPPORT.md are superseded by this source-verified contract.
