# Slow virtual ability clone owners ? 2 November 2026

Implementation only. No build/native process/test fixture was run. Scoped
`git diff --check` passes. This is not a complete-coverage or qualification claim.

Public API: `CloneVirtualAbilityStates.duplicateNear(game,holder,skill,budget,deadlineNanos)`
returns actual copied main logical bodies; `protectedActor(Entity)` excludes owner-driven
Void Body and SauvojenStars carriers from independent native cloning.
`recognizes(Entity)` / `captureNative(game,entity)` cover native Hedgehog Fish,
HelperArrow and owned native children; these remain on the native clone path.

Each virtual owner snapshots its existing list, tests source position in sphere10/16,
respects shared remaining body budget/deadline, creates1/3 independent bodies, and
sets bit1/bit2 only after successful publication. Copies inherit both prior mask bits,
so15/17 compose without same-skill recursion. Destinations use the shared source
position-spread helper and resident chunks. There are no owner/team/jammer admission
filters; TimeStop follows the actual actor owner's pause policy. Constructors do not
spend mana, debit/recast, regenerate initial rewards or replay prior emitted attacks.

Implemented:

- LbcWands.ElectroTrace: age/lifetime/position/yaw/pitch, mechanism/Tesla/meditation/
  statusWild flags, immutable cause/source policy, exact external npcSource/owner/
  explicitEmitter, hostileSauvojen, independent Erecto.Path runtime (including focus,
  side and terrain cause). Reject trialOwner and Erecto trial-stack authority.
  Clone17 spreads current direction and original direction receives small spread.
- LbcVoidArcanum.Hole: new exact Body marker, independent contribution list, level,
  wild flag, original epoch/causal contributors, velocity/tilt, terrain source and
  current relative erosion scan. Create one derived packet-only black-hole Handle
  when original had one. Protect Body/tag skywars2.void_arcanum independently.
  Clone17 copies velocity with spread; original receives small spread. Normal active
  contribution/cost/merge behavior remains canonical; no activation cost is replayed.
  Merge ORs cloneMask so merged bodies cannot reopen an already-used skill bit.
- LbcHoming.Effect rains/destinies: original owner, epoch/level/position and remaining
  active lifetime. No previous rain fireballs or blast histories are copied.
  No velocity field exists in these logical states; future targeting remains native.
- SauvojenStars Actor: exact live kind/flavour/lifetime/age, original external owner,
  accepted hand/independent item value/cause, direction/left geometry, fresh logical
  Marker and carrier tags. Trial stacks excluded. No initial cast or already-emitted
  raindrops are replayed. Native marker is protected independently; it is the logical
  main carrier, not an extra clone candidate. Clone17 and original receive motion
  direction spreads; left geometry is recomputed. Beam/playerBeam are instant.
- LbcDuplicating MarkerState modes1/2/4: age/lifetime/epoch/direction, original owner,
  once mask and erosion seed. Mode4 copies its current relative HoleTerrain erosion
  cursor/clock into an independent queued body-local terrain worker; shader recreated
  once. Modes1/2 create independent future emissions on their next normal update;
  existing LiveShuffle/Seed jobs are already-emitted original consequences and are
  not replayed. Divide(mode3) is an instant operation, never cloned.
- LbcHedgehog Fish/HelperArrow: explicit private same-level constructors preserving
  exact original author UUID, helper UUID/kind and immutable cause; Fish timer,
  retained-owner state and private fish/silverfish indexes; arrow registry and native
  immutable Child cause binding. No spawn/summon API or source-cost recreation.

Source eligibility proof (original world/datapacksdisabled/LBC/data):

- lbc/function/items/arcanums/void_curse_spawn{,2}: slowraycast void_curse marker.
- lbc/function/other/transformator_shoot: slowraycast raycastelectro marker; other
  Wands electrical variants share the moving trace engine, not instant tool searches.
- lbcr/function/raycast_end5_upg_rain: slowraycast blazestorm marker lifetime100.
- lbc/function/workingitems/awaken_tuntija_wands/exahoming_click_upgraded_mode_destiny:
  slowraycast destiny marker lifetime600.
- lbc/function/items/swords/fevgalea_star/rain_projectile_spawn and
  star_rain_waterdrops_spawn: slowraycast star_rain/star_raindrop markers;
  hit_shoot explicitly creates slowraycast star_charge flavour1/2/3 markers.
- lbc/function/workingitems/awaken_tuntija_wands/avarice/click2,
  click2_upgraded_expand, click2_upgraded_black_hole: slowraycast dublicating,
  dublicating_2 and dublicated_hole markers respectively.

ChargedSwords.ThrownSword already has an actual registered native Projectile and is
handled by the native owner; its separate Pulse is replayableTool instant work and
is deliberately excluded. The source blazefire_sword/tick_5t_rc directly recurses
through one-block samples, with no separately admitted slowraycast body. FevgaleaStar has no private pending state: it delegates
slow actors to SauvojenStars and executes beam/focus damage immediately.

Rewind/lifecycle handoff:

Long-state once bits are included in actual existing LbcVoidArcanum.Checkpoint,
LbcHoming.Checkpoint and LbcDuplicating.Checkpoint; the last also saves newly mutable
motion direction. MatchBlackHoleCheckpoint already delegates to the Void owner, so
no coordinator file edit is required. BlackHoleErosion.copyTranslated was explicitly
allowed by root: it independently copies relative scan cursors/seed/geometry/arrays,
translates current center/bounds, and never restores prior terrain writes.

Root explicitly permits cancelling short electrical/star actors at rewind instead
of inventing a new checkpoint protocol. Existing Wands.stop and SauvojenStars.stop
clear those actors/masks, but this audit found no checkpoint/GameController.restoreCore
call to either. ROOT MUST wire those narrow cancellations into rewind cleanup.
Full LbcItems.stop already calls wands.stop and sauvojen.stop. Native Hedgehog tags
use the normal native once-bit/checkpoint path.

Other observed owners outside this assignment still require their own source/runtime
review: LbcErecto.Sphere, SauvojenScareFaces, LbcSauvojen.Drone and other source slow
raycaster families. No statement here certifies their source eligibility or coverage.
Shared status/native transient fidelity and aggregate presentation performance remain
root integration/qualification responsibilities.
