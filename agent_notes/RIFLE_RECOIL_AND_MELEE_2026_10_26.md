# Firearm recoil, stationary accuracy and SLG bayonet input

Status: QUALIFIED AND INSTALLED on the main server, without restart.

Author requirements (user): rotating the camera must never add spread to Magnum,
SLG or either AK variant. Stationary scoped Magnum burst shots stay accurate.
Hitting an entity with SLG must not open its scope; preserve the bayonet attack.

## Cause and correction

The existing accuracy policy reads translational known movement and native
velocity, with grounded contact gravity excluded. It has no angular-speed term.
However, the three firearm engines implemented recoil as a same-position
`ServerPlayer.teleportTo`. In native 26.3, `handleAcceptTeleportPacket` calls the
position handler with `onGround=false`. Ordinary downward contact velocity can
then look airborne to accuracy: 0.0784 blocks/tick produces about 5.23 degrees
of movement spread. Teleports also replace velocity and temporarily gate native
movement packets behind the outstanding teleport acknowledgement.

Magnum, SLG and AK now use `ServerPlayer.forceSetRotation`, which emits the
native `ClientboundPlayerRotationPacket`. Recoil retains its original angle,
random selection and placement after the ray. It does not change coordinates,
velocity or ground contact and creates no teleport acknowledgement. No change
to five-degree rifle hip spread, speed-dependent movement spread through twenty
degrees, AK movement-only spread, scope projection, damage or ammunition.

The 26.3 client sends `ServerboundAttackPacket` followed by
`ServerboundPunchPacket` for one entity-melee click. `ObserverPacketMixin`
remembers the entity-attack intent when SLG is the selected scope weapon, and
consumes only that following punch's scope-toggle side effect. Native damage,
swing and attack timing remain. The marker expires at native client tick end
if no matching punch arrives. Main/offhand selection follows the existing
scope priority; a main-hand Magnum keeps priority over an offhand SLG.
SLG entity attacks preserve both an open and a closed scope, including rejected
damage. Air and block-mining clicks retain the existing toggle/debounce rules.

## Native checks

`GunAccuracyChecks` reproduces the actual setup-teleport acknowledgement clearing
ground contact, then exercises both hands of all four guns with native rotation
input. Every shot emits one camera-only packet, preserves position/velocity/
known movement/ground contact, and emits no positional teleport. Both complete
five-shot Magnum bursts follow the current aim while the shooter turns sharply.
The preceding sixty speed/scope uses, queued-shot motion changes and cone-bound
checks remain.

`RifleZoomChecks` exercises native Attack+Punch pairs for both hands with scope
open/closed, accepted/rejected damage, the next ordinary air click, and expiry
of an unmatched marker at native client tick end. Its prior attack-animation
fixture now sends the complete vanilla attack/swing pair. Existing native damage,
scope overlay/model, FOV, gun payloads/sounds and actual-stop regressions remain.
These checks observe server state and packet payloads, not rendered client frames
or live mouse capture.

Build `1265875f1caa011076850dd413f7366d06d3c6e870f96d9ab44d5e13b726b0d7`
passed 34 recorded suite results in two fresh mode1 matches (62.30 seconds,
including startup/map preparation). Exact 401 Java sources, 1130 classes and
3138 resources match the tested release. No suite or cleanup failure; only the
known fake-player packet-context diagnostic pair. Protected author changes,
their deleted applied RP fragments and live grimoire overrides are preserved.

While the isolated checks ran, the previous safe installer completed the
already-qualified Chef `385f9f11` update after normal main-server shutdown.
The original main baseline `7da14e96` therefore advanced legitimately. Its
installation receipt, immutable qualification, ready artifact and actual main
hash were verified before advancing this release's baseline to `385f9f11`.
The initial and revised baselines are retained in local evidence. No gameplay
artifact or test result was changed by this baseline adjustment.

Evidence: `build/verification/rifle-input-final/qualification-1265875f.json`,
native log/results, final build log and `main-install.json`. The safe installer
then confirmed normal main shutdown/world unlock and installed `1265875f`,
retaining `385f9f11` as rollback. It did not restart the server. Ready artifact:
`build/verification/skywars2-rifle-input-1265875f.jar`. The Chef, native gear and
totem changes are included in this cumulative release.
