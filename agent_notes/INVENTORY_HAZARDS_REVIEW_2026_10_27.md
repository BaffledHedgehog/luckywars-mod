# Inventory hazards independent production review — 27 October 2026

Read-only review of the frozen production implementation in `luckywars-mod`.
The sole write by this reviewer is this new note. No production, fixture, RP,
build, server or Git mutation was performed. This review does not claim native
runtime qualification; the parent owns the separate acquisition and regression
runs and their evidence.

## Decision

No unresolved material production finding remains in the six fingerprinted
files below. This is a source and integration assessment, conditional on the
parent's build and native qualification, rather than a tested-release approval.
The review is limited to Liquid Abyss, all six carried bombs, their actual
tracked pickup integration and the shared hooks changed for these families.
Historical unrelated protected author changes were not audited again.

Contracts read: [inventory source](INVENTORY_HAZARDS_SOURCE_2026_10_27.md),
[Liquid damage](LIQUID_ABYSS_DAMAGE_2026_10_27.md),
[Liquid acquisition](LIQUID_ABYSS_ACQUISITION_2026_10_27.md), and the current
[port contract](INVENTORY_HAZARDS_PORT_2026_10_27.md). The original Liquid
helper and bomb player-select, horse and holy helpers were also read directly
under `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.

## Findings resolved before this snapshot

- The native loaded hook now calls `inventoryHazards.loaded`. Initial TNT and
  lightning subclasses are not saved. Ordinary native TNT chain children can
  serialize vanilla state, but their private managed tag rejects unsupported
  detached reloads. This intentionally ephemeral lifecycle does not claim full
  save/rehydration of arbitrary active casts.
- Plain chain TNT retains its managed cause through native discard-before-
  explode callbacks. The next service tick reclaims removed entries. The
  `LbcIndirectKills.babylonCause` fallback exposes carried causes for unowned
  native damage without replacing explicit native or reflected authors, so the
  existing explosion wrapper retains captured trap/support attribution.
- Carried self-origin harmful effects now have a local Greed guard. This closes
  the gap where the shared effect admission intentionally permits a player's
  own effects. Damage, teleport and native explosion impulse retain their
  existing absolute Greed guards. Native actors still spawn where required, so
  protection of the holder does not erase consequences for other entities.
- Managed TNT originally kept its pre-reflection owner in carried filtering
  and terrain capture. `authority` now resolves the existing Electro reflector,
  then native TNT owner, and replaces origin/cause only when the actual author
  UUID changes. Owner filtering, actor world writes and inherited children use
  that authority together. Its local variable is explicitly `LivingEntity`,
  avoiding the initially inferred `ServerPlayer` assignment error.

Electro's real shield/blocker reflectable set includes TNT, but excludes native
lightning. Production `assignOwner` callers add no ordinary bolt-reflection
route. No new global reflection mechanism was required. Existing reflected
damage-source precedence remains ahead of carried capture.

## Source fidelity and shared boundaries

Liquid uses two independent probes on each 40-active-tick phase: physical
container slots and hands. Selected main-hand overlap can submit twice. Only
after activation does the native clear/count utility include broader equipment,
personal crafting and cursor scope. The helper uses count-only mode, saturating
double-to-int scale four, positive void-resistance suppression and a genuine
environmental outside-border source. Current enabled skill2 requires a hand;
the existing legacy score is read only if objective and entry already exist.
No attacker or new legacy-mode state is manufactured.

The bomb outer gates inspect slots 0..40 independently for instant and bomb
flags, then inspect live variant flags in source order 1..6. The consumed unit
and its provenance follow native broader clear-one order. Each helper retains
announcement, clear and feedback ordering. Position and rotation are captured
before the six helpers, preserving horse's local-coordinate teleport and later
feedback origin. Variant3 uses genuine fuse-two, zero-initial-motion TNT;
variant4 retains the author rule that native lightning is WILD. Variant6 uses
the declared existing finite bounded work policy, with native per-attempt hurt
acceptance rather than one multiplied damage hit or an uncapped blocking loop.

Actual tracked native pickup preserves ordinary successful enemy contact.
Friendly/creator contact and enabled skill2 can reach `super.playerTouch` after
normal admission. Producer UUID and epoch are attached for actual insertion;
an unchanged failed insertion restores the prior custom-data component.
Raw matching stacks do not acquire an invented creator. Existing component
matching, pickup capacity, native inventory callbacks and entity subtype remain
authoritative. Proving this route for all six variants belongs to the parent's
native fixture, not to this read-only assessment.

Non-lightning known-player consequences use default allied protection with the
explicit self-creator exception and shared native role guards. Service maps
scope the new global damage, explosion-target, lightning ignition and chain
hooks; unrelated entities pass through. Known captured authors survive native
consequences, while raw environmental items remain unowned. Epoch/map/ENDING,
observer roles and authoritative Time Stop guard new dispatch. Native Time Stop
entity scheduling also covers ordinary inherited TNT. Inventory passives do
not introduce generic active-input stun or ice suppression. Stop/forget/unload
release service actors and jobs, and unsupported reload cannot resurrect a
detached managed chain.

## Frozen SHA-256 fingerprints

Paths are relative to `E:/servers_mew/skywars2_last/luckywars-mod`. Fingerprints
were read after the final authority variable-type fix. Mutable fixture files
and later release documentation are deliberately outside this production
snapshot.

| Path | SHA-256 |
| --- | --- |
| `src/main/java/me/skywars2/game/LbcInventoryHazards.java` | `1fda15d3d5640a2c191e1561edbb2ced7ae80b5ca67584ac0da979e9dcdca768` |
| `src/main/java/me/skywars2/game/LbcKfcRewards.java` | `2bd6588ac0d0d03c98b24d3e2e723a1a88939010682574923286ae2340fc2bf1` |
| `src/main/java/me/skywars2/game/LbcItems.java` | `04c3693097a33fad69d44ccd1c930d70ff541c066ed90f5df439b883e5418d43` |
| `src/main/java/me/skywars2/game/LbcIndirectKills.java` | `0c8e472df75e33f76384ccfc0a4cc82b89cc524415f17a2910251653bdb3c2f5` |
| `src/main/java/me/skywars2/mixin/ElectroNativeMixins.java` | `c76a3c040d7c819ad84359c09d0146502d41c15102eb24603805cae60489e045` |
| `src/main/java/me/skywars2/mixin/HazardPropagationMixins.java` | `d0c9549e87d0f481355b122b15a1b13806eb2d5992172219d439604f90a92ee1` |

Physical client pixels/audio, latency, peak TPS, complete Armored Inventory,
Touhou, unresolved whole pools and indefinite actor persistence are not claims
of this review.
