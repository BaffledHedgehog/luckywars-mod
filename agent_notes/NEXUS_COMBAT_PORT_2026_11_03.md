# Nexus Pulsar and Orb Gun — 3 November 2026

## Author answers

The Pulsar keeps the registered Paper command's actual independent deadlines
`0/40/60/70/75` active ticks. It is entirely unjammable, including accepted waves.
Both Orb Gun modes are also entirely unjammable, including the enhanced artifact.
Orb Gun keeps current-level XP progress plus one point as its debit, half the
queried progress as integer damage, and the one-point debit without a shot at zero
progress. Custom dropped stacks remain eligible by their original vanilla carrier.
Suspicious stew retains both its effect-specific ingredients and fallback XP.

## Source priority and corrected world admission

`NEXUS_PULSAR_SOURCE_2026_11_03.md` and `NEXUS_ORB_GUN_SOURCE_2026_11_03.md`
record the active closure. The registered `PulsarCommand` replaces the dormant
marker-based Pulsar graph. Comments describing sequential two-second waves are
not the executable timings. Orb Gun has no registered Paper replacement.

The active ordinary `workingitems/spell1_default` and `_left` also route Roadgun,
Corrupter, Orb Gun and Pulsar. They therefore work in ordinary worlds and Nexus,
with the existing Imprinted input exclusion. The previous tools batch incorrectly
restricted Roadgun/Corrupter to Nexus; this batch corrects that admission, their
actor continuation and cooldown progress. Their original outside-Nexus artifact
gate is retained. This is separate from the author's new explicit unjammable
policy for Orb Gun and Pulsar. The normal Nexus classifier remains narrow.

## Native implementation

- `LbcNexusCombat` shares public air/block input and the existing exact fair ray
  queue. Pulsar hits blocks only within 200 blocks and commits its 15-second
  cooldown on a miss too. Orb Gun shoots up to 198 blocks or disassembles dropped
  items near the first item/block hit within 8 blocks. Each cast has one terminal
  outcome; the old independent entity-plus-block duplicate execution is not copied.
- The source Orb area is WILD, excluding its caster, with outside-border damage,
  captured ownership, source sonic-boom sound and thirty curved colored tentacles.
  XP uses the native player's current progress and native subtraction rather than
  a separate XP ledger. Particle spacing follows the original ray independently
  of the scheduling segment size.
- `LbcNexusPulsar` owns five detached breadth-first traversals. It preserves the
  source phase material masks, six-neighbor order and probing from already-admitted
  cells whose material later changes. Each layer advances on later active ticks.
  Work is CPU-budgeted without a radius, lifetime or component-size gameplay cap.
  Visited positions use primitive packed keys. Temporary async chunk tickets are
  retained by pending frontier/probe references and released after their last use;
  visited-only terrain does not remain loaded. No tick NBT, file IO, source-function
  interpreter or world entity scan is added.
- `LbcNexusUncrafting` caches 659 normal selectors and nine stew-effect selectors.
  The closed source graph has 594 output tables and 261 transitive dependencies;
  the existing canonical definitions and references remain intact. Static generator
  `agent_notes/tools/nexus_uncrafting_static.py` verifies expanded source equivalence
  for all 1,027 item entries and 396 count modifiers. Runtime uses native loot-table
  rolls and the shared `LbcPhdCrafting.legacyItem` carrier resolver.
- Each initial ground stack is captured once. A per-unit native debit and all its
  item/XP outputs form one server-thread operation. Picked/changed stacks cancel
  safely, later merges cannot enlarge the original scope, and produced ingredients
  are not reselected. Unknown items and the retained stew fallthrough create actual
  XP entities. In Nexus, the existing before-merge XP producer consequently creates
  one red wire per source XP entity. Uncommitted short conversion jobs cancel on
  rewind; native entity snapshots own committed inputs and outputs.

World TimeStop pauses each affected world's waves, conversion and cooldowns.
The multiworld services run before the map world's early stop return; accepted
Nexus work and its shared ray route remain live while Overworld is stopped.
Global native freeze and legacy global pause still stop their work. Pulsar's
deep typed frontier/visited/deadline/owner checkpoint joins the full coordinator;
it does not delay a save until a potentially huge destructive wave finishes.

Both new items retain source tier-X presentation and models. Only native item IDs
change in the canonical Orb entry and canonical Pulsar definition. The factored
white reward table is not expanded back into duplicate inline entries. No original
resource-pack edits are needed. Full Nexus acquisition/green reward graph, TMT-last,
skill2 and broad full-rewind release gates remain separate.

Successful Orb shots, actual Pulsar starts and nonempty disassembly admissions use
the existing successful-wand bonus handler; failures do not award cast bonuses.
Pulsar visited coordinates use primitive packed longs; frontier chunk counts and
cached loot references avoid per-tick NBT, disk reads or global entity scans.

The first native checks caught two resource migration errors: the exact airs_only
tag was absent from the JAR, and legacy count functions were silently ignored by
the 26.3 loot codec. The tag is now included. The existing schema importer upgrades
292 selected disassembly tables while preserving all396 count modifiers. The source
oracle expands the factored855-resource closure and compares every normalized tree
with the original594 tables (1027 entries). Shared item definitions are unchanged.
See [codec audit](NEXUS_UNCRAFT_CODEC_AUDIT_2026_11_03.md).

## Verification status

Evidence is under `build/verification/nexus-combat-2026-11-03/`.
QUALIFIED and INSTALLED: test `2c4679ee`, production `1f1ce28f`. All9 planned
groups completed in9 real mode1 matches on the exact final artifact: nexus-pulsar,
nexus-orb, vanity-rollback, nexus-tools, nexus-red-wire, dimension-items,
ray-queue-fairness, raw-raycast-spells and time-stop. Both native packet paths and
real scheduled connection ticks are exercised. Polymer requires the simulated
client's real tick-end packet between actions; the fixture now sends it. Vanity's
owned Nexus test terrain is prepared asynchronously before capture.

The candidate retains9018 guarded other inputs and all272 preexisting author
paths. Sixteen freshly compiled Java families overlay the prior qualified Loom
archives;863 exact resources include the855-table dependency closure. All archive
entries were read back and verified, and map-authoring classes remain absent from
the production artifact. Isolated server25577 shut down normally; controlled player,
config,level/ticket bytes restored exactly; no live child or retained locks.

Both build/libs variants are published. The stopped main test server received the
test variant with the previous7d241372 JAR backed up; restarted:false. No main
stop/start, resource-pack edits, physical-client rendering or peak-TPS claim.
Native failures and their corrections remain in the verification directory.
