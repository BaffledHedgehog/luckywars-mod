# Transformator encounter and Magic Academy acquisition

Source: [exact audit](TRANSFORMATOR_ACADEMY_SOURCE.md), with the earlier
[candidate audit](NEXT_NPC_DROPS_SOURCE.md) retained as historical research.

## Ready acquisitions

- `lbc:ported/transformator_drop` yields `skywars2:transformator_drop`.
- `lbc:ported/magic_academy_drop` yields `skywars2:magic_academy_drop`.

Both retain their source repeating-command-block proxy, default stack size,
translated yellow/nonitalic name, white/nonitalic description, models and tiers.
The original resource pack already supplies all assets and translations. No RP
file or language key is changed. Full parent Lucky/Loli/Imba pools remain gated;
these two public leaves do not flatten their parent conditions.

## Transformator

The dropped token creates the source 500-HP adult NoAI zombie and its own text
display at floor(token)+(0.5,-0.1,0.5), with the label three blocks above. The
invisible zombie wears the actual source helmet model; its initial spawn metadata
already hides the body. Name, birthday credit, label settings, zero helmet drop
chance and knockback-resistance base100 are retained. It is a combat encounter:
only the managed actor is exempted from the generic transformator service-role
target exclusion. Untagged/legacy service NPCs remain protected.

Every five active ticks the source independent chances and integer health
thresholds run. The health clarification is explicit (user):

> Без instant_health - просто напрямую наноси урон зомби

Each original 5% roll now directly calls native magic damage for 6 or 12 HP,
respectively. No Instant Health effect or artificial healing is applied. Native
damage defenses and cooldown behavior remain in the native damage pipeline.

The 10% attack creates seven rays aimed at the same nearest eligible living
target in the source 30-block sphere; it does not replace them with Paper's seven
distinct-target cosmetic lightning hits. Rays retain 30 active ticks, 19 samples
spaced0.2 per tick, source refraction and particles. They share LbcWands' native
hitbox damage, wall/shield clipping, conductor pulses, reflection and Curse.
Direct-ray source exclusions are retained; conductivity remains WILD. As required
by the user's global electricity rules, these are electric bolt/conduction damage
types, with metal scaling, Techno immunity and the shared cooldown/knockback tags,
instead of duplicating legacy generic damage and scoreboard armor exceptions.
Mechanism suppression disables production and destroys a ray at the boundary;
magic-only suppression does not destroy this nonmagical electricity.

Warning smoke preserves the source fifteen independent directional emissions in
local coordinates. Low-health fire replaces source #airs in the3x3x3 cube. At
integer health<=440 one native power12 fireball launches from actorY+2 at velocity
(0,-10,0), dirt is placed below, and the actor has its source native death. Only
its own label is removed; concurrent encounters remain intact. No special reward
table is invented; native zombie death drops are retained.

The fireball keeps its causal author separately from its native owner: returning
a removed owner would make vanilla delete the fireball before its first movement.
Explicit reflection/Curse ownership takes precedence over the original token
thrower. Actual primed TNT children inherit author, WILD targeting, epoch and
Time Stop/quiet-stop cleanup through the existing native TntBlock explosion hook.
Fire/terrain consequences use the shared indirect-kill machinery. Native flight
and explosion reads require their surrounding chunk columns to be resident;
neither a ray nor a fireball requests chunks just to continue its attack.

LbcTransformator owns pairs and native attack lifecycle; LbcWands owns shared ray
movement/damage. Dimension Time Stop pauses the producer, rays and native actors.
Unloading/old epochs/match end close the owned pair quietly. Accepted source rays
can finish after the actor's self-destruction; match stop still removes all work.

## Existing Academy service, completed acquisition

LbcAcademy remains the sole owner of Academy interaction and all existing wand
trials. The newly public base token uses the original placement offset(-6,0,-6)
and consumes one item. Existing academy_token retains offset(-6,-1,-6), its
particles/sound and whole-stack consumption. Full height and resident chunk halo
are checked before debit. No extra building service or synchronous chunk request
is introduced.

Native template rows remain incremental. Epoch and level are captured; Time Stop
pauses admission and pending rows. Building NPCs cannot grant trials early. Base
teleport occurs only after all rows finish and an actual Academy actor exists,
using the captured fractional source position+(0,1,-2), nearest eligible player
within12 and the current role guards. A native actor that fails to spawn cannot
trigger a premature teleport. Ownership survives unload records until explicit
stop/old-epoch cleanup.

## Verification status

Offline build `112316bc` passed two actual isolated mode1 matches: 24 recorded
suite results in 49.09 seconds. All 249 Java sources, 680 compiled classes and
2715 resources exactly match the tested release/source artifacts. Runner:
[smoke_npc_acquisition.py](../tools/smoke_npc_acquisition.py). Evidence:
`build/verification/transformator-final/qualification.json`, `installation.json`,
build/native logs and `npc-acquisition-results.json`.

New native suites exercise real public loot/flag admission and debit, fake-item
rejection, real template/NPC interaction, deferred teleport, native role guards,
seven real rays, source wall collision, actual lethal damage/thrower kill credit,
actual reflected damage/reflector credit, producer and ray Time Stop, mechanism
versus magic suppression, health/fire/own-label isolation and native fireball
flight after its actor is removed. The actual smaller explosion primes a TNT
block; native child owner, epoch, tracking and tickNonPassenger freeze/resume
are asserted. Actual match stop closes a live actor and frozen pending token.
Existing Wands Electro, Electro Gear and Exahoming/Curse regression suites also
pass in both matches, including their cleanup checks.

Installed exact verified JAR to main `mods/skywars2.jar` while world locks and its
listening port were absent. Main was not started by the agent. Rollback:
`build/verification/main-rollback/skywars2-before-npc-acquisition-6e98549e.jar`.
Three existing user sources (RerollCrafting, AcidAntDropChecks and
HelicopterEncounterChecks) keep their previous hashes and are excluded from commit.
The known fake-player Polymer packet-context warning remains; no final suite failed.

Early attempts are retained under `build/verification/transformator-final/attempt*/`.
Their resolved fixture/runner assumptions were live-match Exahoming-menu ordering,
checking NPC kill credit before death, forgetting to mirror pitch in a reflected
ray target, testing unsupported native fire in an all-air fixture and calling
TNT.tick directly instead of the real ServerLevel freeze gate. Initial invisible
metadata was deferred until the first living tick; production now sets the initial
spawn flag. Review also fixed removed-owner fireball deletion, explicit reflected
author priority, inherited TNT lifecycle, explosion chunk admission and inclusive
Academy build-height validation. These checks were rerun on the final artifact.

Native fixture checks do not qualify real-client GPU pixels, sustained spawn
capacity, actual disk restart/unload/disconnect or the exact destruction pattern
of a power12 detonation. These limits must remain distinct from source review,
native fireball flight and the actual smaller explosion/TNT-chain test.
