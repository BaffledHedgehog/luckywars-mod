# Native random-state components for Vanity

## Contract and release boundary

The [full author rewind contract](VANITY_ACTOR_CHECKPOINT_2026_10_30.md) remains
mandatory: FULL world/map and all players including dead/offline/newcomers,
items/effects/attributes/statuses/tags/NBT/counters/queues, native/private state,
core/map parameters, dimensions/positions/motion and current authority ownership.
This extends the canonical terrain/core/body/actor/combat/view owners. It is NOT
the full Vanity transaction. Acquisition remains disabled and the full original25
Chaos gate stays CLOSED. No main installation/restart/push, reduced pool,
RP/template or pending-question edits. Questions belong in CHAT.

## Exact native RNG images

MatchRandomState captures native LegacyRandomSource, ThreadSafeLegacyRandomSource,
SingleThreadedRandomSource and XoroshiroRandomSource, plus WorldgenRandom delegates
and the native RandomSequences.DirtyMarkingRandomSource wrapper. It reads state
through typed native accessors and invokes no random draw while capturing/replaying.
Unknown algorithms and mismatched target algorithms are rejected, not approximated.

Legacy raw 48-bit state, both Xoroshiro engine longs and the Gaussian helper's
nextNextGaussian/haveNextNextGaussian values are copied independently. The lazy
SingleThreaded Gaussian's ABSENCE is also restored; a replacement RNG gets its own
correctly bound native Gaussian helper. WorldgenRandom retains its inherited cache,
delegate state and full int count, including native overflow behavior. Fork and
positional-factory behavior remains implemented by the native algorithms.
The image contains primitive values rather than mutable seed/cache aliases.

Replay writes the raw state into the CURRENT source objects. It never uses setSeed
as an approximation: native reseeding may mix state, reset a cache or, for some
implementations, leave that cache intact. Repeated replay and replay into replacement
sources therefore preserve exact native continuation. Thread-safe RNG support is
not a claim of an atomic snapshot while other threads concurrently consume it;
the full coordinator must quarantine all writers before using these components.

## Canonical entity/player/menu ownership

MatchEntityState owns common RNG state for BOTH players and other entities. Capture
precedes native NBT serialization; restoration follows load and transient-state
reconciliation. MatchActorCheckpoint reapplies the same image after native entity
registration. MatchPlayerCheckpoint reapplies it after body sync/registration work.
This accounts for native load/construction work that may itself consume a random
value, and writes into the replacement player's own native RNG after real respawn.
No second player-only RNG model or old player RNG object is installed.

MatchWorkstations also restores EnchantmentMenu's private scratch RNG through the
same helper. The prior [workstation stage](VANITY_WORKSTATIONS_CHECKPOINT_2026_10_30.md)
excluded the NEXT seed drawn after actual enchanting from repeated-consumption
comparison. That exclusion has now been REMOVED: every native menu data value,
including the next seed, must match after repeated actual enchanting.
This supersedes the earlier note's limitation for these supported native RNGs.

## World and named sequence ownership

MatchWorldRandomCheckpoint is a separate GLOBAL component. It is deliberately not
attached to a partial-chunk terrain restore, which must not rewind unrelated global
streams. The future full coordinator must explicitly compose this owner with the
existing body/actor/world phases. It captures each existing native dimension's main
RNG, sound-seed RNG and Level.randValue used by getBlockRandomPos. Replay checks
same server/match/epoch, frozen main thread, stable dimension instances and source
identities before applying the values. It does not load/generate dimensions.

MinecraftServer owns ONE shared RandomSequences service across dimensions. Its
native map object, each saved RandomSequence and its RandomSource remain the
canonical original objects. The image independently saves every source's state,
salt/includeWorldSeed/includeSequenceId defaults and saved key set. Replay removes
future/reset replacement entries, reinstates the saved entries into the SAME map,
and restores their source values. Existing borrowed native wrappers still point to
the original sources and resume correctly. Subsequent creation of a new sequence
also uses the restored defaults. The native service is marked dirty for persistence.

Native RandomSequence/Xoroshiro codecs omit cached Gaussian state, so a codec-only
round trip would not prove exact in-memory continuation. This component retains the
complete supported cache in memory. Native dirty publication is not a claim of
durable checkpoint serialization of unsaved caches or of completed storage/IO
reconciliation; those remain coordinator/persistence work.

## Exact native evidence

Final immutable artifact: `build\verification\skywars2-vanity-random-98b4ecfe.jar`.
SHA256: `98b4ecfe9c68cbf93637b9505889ce877959e38d617f052f859358ee8567b7bd`.
11,246 frozen inputs, including runners/prepared probe; all
5,393 processed class/resource entries matched the exact JAR.
Source receipts cover 152 native archive entries plus the legacy
and canonical owners, 189 total. Native archive SHA remains
`1ad6e7adb36253216767c99b8fccc48da3a1b1eb53aef0bddc51936d5e873a6a`.

An initial compile found only two test-factory constant names changed in 26.3;
native registry lookup fixed those. Build2 passed. Initial predecessor artifact
521fafdb passed RNG/workstations/player/actors/bound-views in one actual mode1 match
(37.03 seconds). Test coverage was then strengthened to require zero draw during
capture and correct fresh-sequence generation from restored defaults. Build3 passed.
The FINAL artifact passed TWENTY-FOUR suites in ONE actual mode1 match:
`vanity-random`, `vanity-bound-views`, `vanity-workstations`, `vanity-storage`, `vanity-views`, `vanity-actors`, `vanity-player`, `vanity-core`, `vanity-terrain`, `map-lighting`, `time-stop`, `witch-acquisition`, `witch-lifecycle`, `polymorph`, `effect-layers`, `projectile-body-collision`, `entity-merging`, `momentum-teleports`, `glass-armor`, `indirect-kills`, `enchantment-rules`, `advanced-anvil`, `tiny-reforge`, `reroll-machine`.
Runner elapsed 67.9 seconds. Normal child exit, exact tracked file
snapshots restored, no suite/cleanup failures or retained locks. Focus mode purposely
leaves qualified:false; the explicit completed suite list defines this stage's proof.
No native suite failed in this stage. Pre-existing bootstrap warnings remain; no
physical-client, arbitrary-map or peak-TPS claim is made.

VanityRandomChecks exercises six actual algorithm/delegate configurations, each
with empty and populated Gaussian caches. A separate identically seeded native
control proves capture consumes no values. Three replay rounds after reseeding and
Gaussian consumption, plus different current target objects, must reproduce mixed
Gaussian/int/long/float/double/boolean/triangle draws, forks and positional factories.
It checks lazy cache absence, Worldgen int-counter wrap, and rejection without
mutation of unknown/mismatched algorithms.

The actual match fixture composes canonical core/player/actor images with the new
world RNG owner. Native player/Cow RNGs, actual Arrow.shoot spread, all dimension
main/sound/block-position streams, an already-borrowed named wrapper, a fresh wrapper,
NEW sequence creation and an actual weighted native LootTable must reproduce exact
outputs. Native Player.onEnchantmentPerformed must give the same NEXT seed and XP.
It mutates/reset/replaces streams/defaults, performs genuine player death/respawn,
repeats after four real world ticks, then disconnects and creates a genuine new login
body before replay. Projectile ownership must bind that current player. The dirty
wrapper is separately rewound through its own adapter. Fixture cleanup restores
original actors, player RNGs, world/named state and existing tracked snapshots.
The unchanged broad regressions include STRICT full enchanting-menu data equality.

Expanded Tiny/Reroll checks reuse the existing native temporary loot probe and
forced crafting chunk preparation. Startup auto-enables that explicit test resource
pack. Probe, level metadata and native chunk-ticket files are restored after normal
shutdown. The initial five-suite predecessor used original resources.

All 199 pre-existing author dirty paths and all 31 live override files remain
byte-identical. Main JAR remains
`d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087`.
Evidence: `build/verification/vanity-random/{baseline,candidate,sources,final-audit}.json`,
SHA-named frozen input manifests, initial/final logs/results, numbered build logs,
prepared-fixture.json and bounded local commit receipt. Previous root-documentation
tails remain byte-identical.

## Required follow-up

This covers the supported CURRENT native source instances and named-source creation
defaults. It does NOT capture all private application ThreadLocalRandom/JDK sources,
the static unique-seed/time entropy allocator, arbitrary future factories, worldgen
worker jobs, or every private AI/ability RNG reference. Do not label all randomness
complete. World clocks/weather/borders/game events/listeners/metadata, private BE/
AI/navigation/sensor/multipart state, ability/attribution/FPV/TimeStop controllers,
custom/private GUI, registry reloads, entity-region/pending IO, complete map jobs/
baselines, full input/writer/lifecycle coordination and automatic offline replay/
persistence remain unfinished. Preserve the canonical owners and add the global
RNG owner only at the full transaction's appropriate frozen phase. Vanity stays gated.
