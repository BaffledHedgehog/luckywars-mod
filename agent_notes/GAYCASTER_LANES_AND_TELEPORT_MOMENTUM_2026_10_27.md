# Gaycaster electric lanes and position-only movement

## Author requirements (user, 27 October 2026)

Gaycaster meditation: reduce the parallel electric wall/laser lanes by two,
increase their spacing by one block, and show a warning at every lane instead
of only the central one. The preceding nine lanes at spacing3.5 become seven
at spacing4.5, centered at offsets -13.5,-9,-4.5,0,4.5,9,13.5. The independently
random number of attack waves is not changed by this request.

Fear and other camera/coordinate changes must not zero player velocity. A
rotation must not rotate the momentum vector either. Deliberate changes to
velocity, such as knockback, remain explicit motion operations.

## Implementation

`LbcGaycasterAcademy` derives damage, particles, paint and preview positions from
the same lane offset. Every electric hazard owns seven distinct warning planes,
retaining the existing30x2 geometry, travel orientation, seven-block lead,
one-tick interpolation,80tick duration/fade and0.5block/tick movement. The middle
plane remains the hazard's representative warning for existing inspection code.
All planes participate in the existing trial warning list, Time Stop, epoch,
viewer replay and close/forget/stop lifecycle. No shader or resource-pack update
is needed; existing warning assets are reused.

`LbcPsycho.rotate` and `LbcErecto.rotate` use native `forceSetRotation` and its
rotation-only packet. They no longer teleport for a camera edit, so there is
no needless teleport acknowledgement or native airborne-state side effect.

`TeleportMomentumMixin` supplies one common coordinate-edit policy:

- Full `Entity.teleportTo` preserves motion for players and mobs, including
  relative coordinate/angle arguments, without rotating their velocity vector.
- The connection's canonical position teleport handles all player overloads.
  A default zero-motion destination carries relative DELTA_X/Y/Z and no
  ROTATE_DELTA, keeping the client's own local motion. The server's actual
  getDeltaMovement is restored independently because native teleport placement
  reads lastKnownClientMovement, which can differ from that server value.
- Explicit nonzero transition payloads retain native motion/flags, including
  portal exit transformations. Explicit setDeltaMovement before/after a
  positional teleport is not intercepted.
- `/tp` no longer performs its second vertical-velocity reset or forces an
  airborne entity onto the ground after the coordinate edit. Native permission,
  target selection, destination checks and mob navigation stop remain.

The audit also removed copied post-command Y resets in Ordinary Accessories'
rescue platform/magnet, Inventory Hazards' horse displacement, and Hedgehog
dimension transfer. Independent native `/tp` and platform tests now compare
actual server movement rather than substituting last-known client movement.
The horse is additionally checked by itself: its full bomb sequence first
applies native damage/knockback, so the pre-bomb velocity is not the correct
expected post-bomb vector. No dash, knockback, Time Stop freeze or other explicit
motion operation is suppressed.

The read-only audit used local26.3 native sources in the Loom sources JAR:
Entity.teleportTo/teleportSetPosition/forceSetRotation,
ServerPlayer.teleport/getKnownMovement, PositionMoveRotation.calculateAbsolute,
ServerGamePacketListenerImpl.teleport and TeleportCommand.performTeleport.

## Verification

Qualification evidence is retained in `build/verification/gaycaster-wall-lanes/`.
The first native run stopped Gaycaster before meditation because its historical
cyan assertions still required magic electricity. The previous author-approved
jammer change makes all released electricity TECH-intercepted; both ordinary
and Academy fixture assertions now follow that rule, without changing their
production behavior. Initial logs/results are retained separately.

An initial momentum startup exposed a Mixin package restriction: a regular flag
helper cannot be loaded from the declared mixin package. It now lives in the
ordinary `game.TeleportMomentum` class, shared by the two injected paths.
That failed native attempt was shut down normally and its logs/snapshot result
are retained; it was not qualified or installed.

A command-fixture issue was also corrected: `performPrefixedCommand` queues a
nested command while the check command is still running. The check now uses
the native Brigadier dispatcher synchronously, just like the independent
platform `/tp` control. Earlier failure evidence remains retained.

The updated Gaycaster fixture probes all seven actual native damage lanes, an
intermediate gap and the outside edge, seven separate advancing preview planes,
natural trial warning synchronization, shared Time Stop and complete cleanup.
`TeleportMomentumChecks` uses a recording native connection, differing server,
last-known and simulated client vectors, real packet encoding/decoding and native
client position calculation. It checks all player teleport overloads, actual ACK,
native `/tp`, repeated real Psycho fear/rotation code, Erecto recoil, explicit
nonzero transition/impulses and mob coordinate edits. This does not claim a
physical-client capture, latency stress measurement or all portal destinations.

Final build, native results and installation status are recorded below after
the owned isolated server exits normally and exact fixture snapshots restore.

## Additional author requirements (user, 28 October 2026)

Restore native physical self/friendly impacts for darts, bows, Shulker bullets,
Baffled spikes and smoke grenades. Homing acquisition remains separate from an
accidental collision. Native owner departure still protects the initial launch;
a returning projectile can hit its shooter after leaving. Greed remains an
absolute reflector and observers/service actors remain protected.

Incarnations must heal16HP/second without stacking between fields or owners.

The physical collision/accepted-damage policy is shared by `LbcWands`, tracked
shooter arrows, Baffled helper/spike overrides, Shulker shells, the native arrow
player-team gate and the final player damage gate. The reflected-owner gate
uses the same physical eligibility. Enemy homing selection is unchanged.

Six ordinary darts and both current/old smoke-grenade carriers had loot JSON but
no native throw conversion. Their vanilla Snowball compatibility input now uses
the existing `LbcHedgehogSpikes` arrow transport/epoch/Time Stop/cleanup machinery.
Native launch motion/sound/stat/one-item consumption are retained; no snowball
carrier entity is published. Existing item names/lore/models/rarities remain.
Source arrow damage coefficients are3/2.5/2/4/1.8/2.5 for confuse/blind/boom/
standard/wither/poison, and.01 for smoke. Native tint values and potion durations
come from `LBC/data/lbc/function/projectiles/specials.mcfunction`. Blindness120,
PoisonIII100 and WitherIII100 use existing owned native effects. Confuse reuses
the resident surface search with source140/190 bounds and Nether120 ceiling.
Explosive darts prime native TNT with fuse2 through the existing shared child
cause/Time Stop/stop lifecycle, retaining source wards instead of a creeper actor.

Grenade smoke reuses the same owned temporary smoke mechanism as reactive
artifacts, with source radius5 and BlindnessII. Its native in-ground arrow
lifetime is1200ticks; source smoke_active is added on the first in-ground call,
so the source's later `tag=!smoke_active,life:100` shortening is unreachable.
Current gray-wool marker and old poof visuals are retained. Mixed-strength
smoke preserves independent preexisting blindness layers; exiting restores
players and mob follow range. The artifact's radius7/BlindnessI/100tick contract
is unchanged. The kit's separate `smoke_bomb` is not this grenade item.

Incarnations removes its duplicate owner heal and shares one20active-tick
cooldown per target across all source fields/owners. Replacing a source does
not reset that cooldown. Its existing native Instant HealthIII semantics,
including undead inversion/attribution, remain; clocks pause and lifecycle
cleanup follows the owning service. `SoulhunterChecks` exercises16HP exactly,
independent targets, overlaps/staggered pulses, source replacement and Time Stop.

The first expanded focus run passed actual momentum packets, physical native
hurts/payloads/Greed, carrier callbacks, smoke entry/exit, accessories and shells.
It exposed fixture combat-attribution leakage into Soulhunter and the horse's
incorrect pre-damage velocity expectation. Those fixtures now restore their
owned attribution entries and test the actual displacement boundary separately;
failed logs/results are retained. No failed artifact was qualified or installed.

The first complete expanded run passed40results in two actual matches on
319dabdb (182.03seconds), with all owned snapshots restored. Final audit then
found the only remaining direct `PositionMoveRotation.of(player)` pose
correction in `LbcShooters.correctStun`: its nonzero last-known movement payload
could be mistaken for an intentional transition. It now uses the coordinate
overload and common momentum policy. The complete prior run is retained under
`before-stun-correction-full-*`; the final artifact must pass again after this
last change. No further behavior was broadened after that audit.


## Final qualification and installation (28 October 2026)

Final build **ccac368f**: `build/verification/skywars2-gameplay-fixes-ccac368f.jar`, SHA256
`ccac368f79168271f9fc1627706019f6f6793c70067935d8f0019ea9e57e8f23`. Gradle offline build passed. Exact534sources/1515classes/
3226resources;198preexisting author paths and29live grimoire override JSONs
retain their initial bytes/existence. No actual RP or template asset was changed.

The final native run passed40results in two actual mode1matches,
180.71seconds, plus the two scoped native third-player setup/cleanup
checks. The owned child stopped normally and all exact fixture snapshots
restored. Momentum, native self/friendly physical damage/payloads/Greed,
eight real dart/grenade callbacks, shared16HP healing, accessories, inventory
hazards, shells, both complete meditations, minigun, Time Stop/contact, TPS,
effect layering and FOV suites passed. Evidence is in
`build/verification/gaycaster-wall-lanes/`, including the complete log/result,
artifact proof and qualification. No physical-client visual/latency/TPS stress
claim is made. The canonical log/result names retain their runner history.

Installer16596 initially waited for the main server's ordinary shutdown, then
installed this exact tested JAR over746e62aa without forcing stop or restarting.
Main hash equals the qualified artifact; `main-install.json` records the backup.
Retention: ccac368fcurrent,746e62aa immediate rollback,154cc3a7 fallback and the
separate network-audit tool. The superseded a4683fe2 JAR was path/hash verified
and removed (40697431bytes); see `cleanup.json`. An initial cleanup guard noticed
the installer had just changed the main hash and aborted before deleting anything;
the successful cleanup verified the newly installed hash and retained rollback.
