# Old Bob and old TNT Drone

22 October 2026. Offline build and two actual isolated mode1 matches qualified.
[Exact source audit](BOB_DRONE_DROPS_SOURCE.md).

## Own-host shutdown (user, 22 October 2026)

> Отключать после гибели своего хоглина

The drone's own hoglin and cart carry the same persistent host identity. Actual
host death closes its producer immediately; the original per-second cleanup
removes the cart. Nearby foreign hoglins/zoglins cannot keep it alive. A living
dismounted host or chunk sleep does not count as death. Native conversion carries
the identity, and a remembered death also closes a later same-match cart reload.
Already emitted TNT and pigs keep their independent native lifetimes.

## Native closures

Two standalone public leaves: `lbc:ported/old_bob_drop` and
`lbc:ported/old_tnt_drone_drop`. Both retain the source's plain barrier token,
flags and existing translations. They do not open normal Bob/drone reward pools.
The actual RP and resourcepack template need no edits.

Bob is a persistent adult vanilla zombie, with its exact six equipment stacks,
independent 50% drop chances, native enchantment maps and follow range400.
Native health, AI, damage, equipment effects, loot and conversion remain. Its
source-author/epoch tags reuse the creature-drop service and survive native
Drowned conversion without replacing converted equipment or attributes.
Native Ignite enchantment effects from managed creatures record Fire Aspect's
causal author, preserving delayed burning credit beyond native hurt references;
newer accepted player actions retain priority.

The drone uses native recursive entity loading: invisible40HP bat at tokenY+20,
spawner cart, then persistent30HP Weakness99 hoglin. Native cart configuration
and activation produce fuse100 TNT carrying35HP pigs with empty death loot.
The native-ground check accelerates fuse to2 once, without an invented timer.
The original literal names, effects, delay/count/range/cap and particle feedback
are retained. The TNT uses common configurable explosion damage protection and
the existing native block-discovery optimization, with normal blast application.

All actor and producer payloads carry persistent cause and match epoch tags.
Native TNT owner save/get hooks preserve the actual author and reflection owner;
chain TNT and native mob conversions keep provenance. Drone traps are WILD:
self/allied players can take damage, while role/service NPC exclusions apply
before native explosion damage and knockback. Ordinary Bob follows the existing
creature ally policy. Common block changes retain blast/fall/hazard authorship.
Native candidate collision checks use resident chunks without adding tickets.
No replacement spawner, arbitrary TTL, owner-death removal or world scan is added.

Common dispatcher source order is drone before potion spawners, and old Bob after
old Skeleton; combined flags debit one unit. Admission, time stop, unload/reload,
stale epoch rejection and quiet actual match-stop removal use existing services.

## Qualification

Offline Gradle build and `python tools/smoke_bob_drone_batch.py --server-dir
build/verification/key-reuse-server` passed. There are130 recorded suite results
across two actual mode1 matches,88.84s, including all previous special-drop,
island, potion-producer, advanced-anvil and enchantment-rule regressions.
New stop probes run last and post-stop cleanup runs first, retaining actual
native actors and a paused pending token until the real match stop.

Ready artifact: `build/verification/skywars2-bob-drone-batch-b6ce92ce.jar`.
SHA256: `b6ce92cea1e47b829931245b6dca13398580991fbd7c0860ddc798a21badc7b8`.
All216 Java sources match the source JAR byte-for-byte;608 compiled classes and
2694 processed resources match the tested release. Evidence/manifest are under
`build/verification/bob-drone-batch-final/`. Main JAR96ee50a7 and the user's
Reroll source6ef80947 remain unchanged. Not installed. The isolated server is
stopped, its original files restored and fixture locks released. Only the known
fake-bot Polymer packet-context ERROR pair remains; no check failures.

Native fixtures verify both leaves/presentation, source order/debit/admission,
Bob's exact six stacks, enchants/drop chances/native health/AI/persistence,
actual melee damage/Fire Aspect/knockback, role/allied protection, native deaths
and all six equipment outputs, Drowned conversion and owner/epoch continuity.
They clear native hurt references before burning deaths and verify both source
credit and a newer player's priority. Source-specific changes preserve ordinary
vanilla loot generation and native equipment damage/drop behavior.

Drone checks use actual recursive passengers and native BaseSpawner emission,
source configuration/status/name/health, required-player activation/delay,
generated TNT/pig author and epoch, source fuse100, real falling collision and
one-time fuse2 landing acceleration, empty pig item loot, protected TNT damage,
WILD self/allied player damage with team friendly fire disabled, role exclusion
and an actual TNT kill/stat. Own-host native death closes the actual cart tick
while a foreign hoglin is present; dismount and service unload are not deaths.
Same-match reload, stale actor/pending-token rejection, actual freeze of fuse,
motion and native producer delay, real sequential stack dispatch and quiet stop
of Bob/bat/cart/hoglin/TNT/pig plus pending epoch are exercised.

Retained development evidence: `attempt1/` exposed a fixture assertion using20HP
for an untouched vanilla cow; the guard now compares pre-hit health while
retaining the actual denied melee/no-ignition checks. Native source review also
moved the pig's custom-empty-loot assertion before death, since vanilla consumes
that override during loot processing; the actual no-item-drop check remains.
`attempt2/` exposed a production omission: the native TNT owner getter already
resolved the drone author, but the existing owner-save hook's managed-source
list omitted drone TNT. Its save predicate now includes drone lineage, and the
final run checks persisted owner as well as actual owner/credit.

Limits: no real client rendering/audio, actual disk restart/chunk unloading/
disconnection, natural human movement or long-running autonomous mob/spawner
population, offline coin rewards or peak capacity benchmark. Lifecycle reload
checks use native saved payload inspection plus service callbacks, not a server
restart. Bob's melee and conversion are invoked through native endpoints;
spawner emission is invoked through the actual native spawner with controlled
delay. The shared native explosion optimization is reused; this88.84s elapsed
run includes boot/maps/regressions and is not an MSPT benchmark. Native
reflection/chain integration is source-reviewed and shares prior hooks; the new
fixture does not claim a separate shield reflection or chain-capacity benchmark.
No additional TTL/cap is invented for the source producer.

Next source group: [Acid Burst / Ant Swarm audit](NEXT_DROPS_AFTER_BOB_DRONE_SOURCE.md).
Those mechanics and the optional portal candidates remain unimplemented.
