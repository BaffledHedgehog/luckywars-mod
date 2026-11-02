# Cloning skills 15 and 17 ? 2 November 2026

Author contract: x2 copies projectiles, never combat mobs (reconfirmed in chat this turn).
The agreed nonliving list also includes boats/carts, XP and clouds. x4 additionally
copies mobs, including bosses and Chef. Self/allied/enemy/unowned sources are valid;
causal author stays unchanged. Independent doubled/quadried marks compose to x8,
without repeated application of the same skill. Source radius10/16 and motion/position
rounding remain explicit. Hedgehog tanks/snipers and converted sheep are x4-only.
FPV remains a projectile: a copied drone flies autonomously; camera/session/player
control is never duplicated. Player bodies and player-controlled form proxies are protected.

## Implementation

LbcCloneSkills is now installed in LbcItems and original catalog slots10000:14/16
with the Double Bow/Sickle achievement gates. CloneActorStates composes reviewed
native factories with owner-specific images, rather than serializing Java object graphs.
All custom factories must produce a fresh exact native class/ID/UUID. Unsupported
factories reject the transaction; no inert vanilla shell is published.

Native NBT/components/metadata/attributes and reviewed runtime values are independent.
Dynamic attributes outside a type's vanilla supplier are retained (Chef transient attack
attributes included). Passenger families are reconstructed before publication, internal
shooter/target/leash links remapped, external links retained. Human passengers and
protected map/shop/trial/visual children are omitted. Visual owners recreate their own
children once. Native primary fishing ownership is protected by a clone-tagged hook
mixin, including removal of the copied hook.

The explicit Clone*States groups cover native/custom projectiles, summons/controllers,
encounters, effects/leases/causal queues, logical virtual attacks and reversible forms.
A copied temporary sheep owns a separate suspended original and saved pre-transform
state. Suspended native effects preserve their causal authors and pending mana cause
across ordinary restoration, copying and checkpoint restoration. A positive-health
removed sheep returns its original; native death/KILLED still kills the original.

No idle NBT scan, chunk loading, inventory polling or second ray scheduler was added.
One admitted holder is rotated per tick, with256 resident candidates,48 logical family-copy
units and a soft4ms boundary between atomic operations. Complete families must fit the
remaining body budget. Owner images capture only admitted, not-yet-copied actors.
Regenerated presentation children and suspended form storage are additional native
objects. These are structural limits, not a measured peakTPS or worst-case latency guarantee.

## Rewind and packaging

Persistent native once flags and existing owner checkpoints retain copied long abilities.
Final logical Drone/Hammer/Sphere state has typed checkpoint images (Sphere cloning
itself stays dormant: its original producer lacks slowraycast). Short moving electrical
traces/stars are cancelled only on full rewind, not on real-player rebind. Boss/Poop
loaded indexes are rebuilt from canonical captured bodies without a world/chunk scan.

The previously unused projectiles_double tag is explicitly packaged; the XP test caught
its absence from the first candidate. Modern boats/carts use their actual native base
classes; physical potion clouds are admitted independently of old cloud-marker misuse.
No RP assets or translations are needed for this batch. Author templates/overrides and
four preexisting author Java edits are preserved.

## Verification record

Final qualification and installation receipts are in
build/verification/clone-aura-2026-11-02/. Final testc68da1b2 / productiond2e582c3 passed10 scoped native groups in10 actual mode1
matches on the exact test artifact.9834 inputs/2613 classes/7355 resources match,
236 author paths/31 live overrides are unchanged; native children stopped normally,
all snapshots/level/tickets/OP bytes restored, fixture25577 only and no retained locks.
The normal-stop installation receipt currently records installed. Source inputs are frozen before
build/native children, both production and test roles are compared to compiled outputs,
and the8 test-only editing classes remain absent from production.

Retained failures include an obsolete Skeleton import, invalid simultaneous native
passenger/leash fixture assumptions, immediate-removal rather than next-tick form
expectations, the actual positive-health form-removal defect, and the missing packaged
projectile tag. Stale rewind fixtures now read the fair ray scheduler through its
Iterable and identify accepted requests by identity, not global append position.

An additional broad vanity-resident-spells diagnostic remains unqualified: after its old
queue assumptions were repaired it fails the Stonekivi trial/current-hand assertion in
VanityTerrainScene.thawed. It does not establish the full coordinator as ready; that gate
was already closed and is not opened here. The author-modified LbcAcademy source was
not changed. Preserve the failing5f126a93 trace for that separate closure. A cross-suite
boss contact/cadence failure on a89590fd was retained; same-artifact isolation passed,
and subsequent full runs passed without altering boss cadence.

TMT stays last; skill2/Nexus/full-kit and all other existing migration gates are unchanged.
No main-server stop/restart, push, physical client rendering or peakTPS claim.

Final fixture refinements also isolate real hazards before waiting a tick and use
Poison301 ticks so the native first Poison tick does not alter the HP restoration
probe. The exact-radius test covers the broadphase boundary plus unchanged spherical
admission; only its broadphase has a1e-6 numerical tolerance.
