# Witch Gluttony ? native stage, not released (29 October 2026)

The entire Gluttony actor family is staged together: killer rabbits, black snake
heads/fragments/sculk contact, and native white-whale Ghasts/clones/fog. This is
not qualification of seven-sin acquisition, remaining authorities, full Vanity
rewind, or the full original25 Chaos transitive release graph. Main remains
`../mods/skywars2.jar` SHA256 d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087.
No staged artifact was installed and no main-server restart was performed.

## Author decisions applied

(user) Black Snake rare facing selects a RANDOM eligible target.
(user) Rabbit and snake rays summon exactly ONE creature per use, including an
overlapping entity/block terminal.
(user) Preserve the snake's ZERO initial rotation and its RANDOM HEAD rotation
when producing the FIRST fragment. The first child keeps its zero rotation and
unactivated state; a later child emission activates its unactivated parent, as
in the old helper. Activated parents randomize their newly produced child.

Prior supplied decisions still apply: live authority transfer changes the real
controller, teams/counters and future attacks. Already accepted native projectile
causes remain immutable. Antimagic/stronger artifact stop new casts; snake/fog
are magical actors, physical rabbits/Ghasts remain physical. Time Stop pauses
actor clocks and work. No fabricated mana price, item debit or cooldown was added.

Sources: [actor source audit](WITCH_ACTORS_SOURCE_2026_10_29.md),
[Witch graph audit](WITCH_GENS_SOURCE_2026_10_29.md), and
[prior author decisions](AUTHOR_DECISIONS_2026_10_29.md). Both `lbc` and `lbcr`
front doors matter; checking only lbc would miss rabbit/snake input.

## Implementation

`LbcWitchGluttony` enters the existing two-hand `LbcWands` dispatcher from native
raw carrot-on-a-stick use. Existing authored `rabbit`, `snake`, and `whale` flags
remain the front door. Real native selectors retain GLOBAL no-distance actor
counts/existence; they are not replaced with per-player/per-dimension quotas.
Rabbit/snake use the shared resident Exasperato DDA and one terminal future.
They retain 150-block extent, source trails and a one-block backward spawn offset
on block impact. The accepted ray direction is captured before queuing. The
approved snake starts at rotation0 independently of that ray direction.

Rabbits use native persistent Type99 killer-bunny AI and original movement1 /
follow100, ordinary HP/damage/loot. Reproduction checks global count<=99 in the
20-active-tick callback, sequentially allowing100. There is no actor TTL.

Snake heads have no TTL. Fragment lifetime20, movement1, original integer yaw /
pitch draws, global cap79, source6percent branching, shared `snake_storona`
scoreboard state, independent1percent side writes, altitude corrections and
0.25percent random facing are present. Original strict3/9 cubes and tagged7/15
cubes use common `BlockAreaWork`/`LbcStonekivi.Work`, coalescing accepted cells
without reading unloaded chunks. There is no duplicate terrain mutation engine
or private replacement of common provenance. WILD generic0.5 contact and eight
sculk probes reuse native damage/effect layers and inherited cause. Current item
caps apply to Poison/Wither/Blindness/Darkness; other source levels stay intact.

Whales are actual persistent Ghasts: initial HP5400/MAX54000, follow200,
ExplosionPower30, authored translated name and native AI/fireballs. Initial
motion is look*7 with source floor-to-thousandth quantization. Source wool
particles, three independent random sound draws, strict11cube clearing and the
positive native item-removal box remain. Native playsound in26.3 filters the
execution dimension even when its selector is @a; existing `LbcOverload.sound`
already implements that and minVolume1, including FPV viewpoints.

The low-health transition creates THREE real MAX15000/HP15000 Ghasts, teleports
the original to0,150,0 and applies Y velocity10. Clones copy the actual main's
numeric HP in their second callback; source drive/fog chances and main-versus-
clone Wrath exclusion differ as before. No private flying actor or fake player
replaces native Ghast behavior.

Fog advances1 for100 active native ticks. It kills eligible entities within2,
uses radius5 on every solid impact and CONTINUES after impact. Allied teams /
team_number, Gluttony/Wrath, Greed, void resistance and protected roles remain
excluded. Living kills use the inherited-kill path (including Carpet behavior),
nonliving entities use native kill. There is no helper Creeper/explosion.

`LbcCommunism` and `LbcSpellSource` remain the common control/cause owners.
`LbcSummons.loaded` now leaves persistent source-bound native children with that
owner, instead of replacing the physical Ghast owner with a player and enrolling
them in a player-cleanup queue. Ordinary summon ownership is unchanged. Loaded
actor state supports in-memory unload/reload by UUID, while epoch/stop terminates
it. No fresh-process actor-state migration or old raw-tag adoption is claimed.

## Exact native verification

Candidate SHA256:
`5ed6d2dd0932233e9aa0d46cc0f2dee5dc7baec3370113cda803a1a720d9c490`.
Offline JDK25 build: `build/verification/witch-gluttony/build-8.log`.
Frozen inputs: `inputs-5ed6d2dd.json` in that directory:10986 paths,
617Java sources,1781classes,5176source resources,3411processed resources,
and build.gradle. `5ed6d2dd-input-audit.json` confirms exact input and JAR bytes;
only normal Loom fabric.mod.json nested-jars metadata is normalized.

The SAME artifact passes12 gameplay suites in TWO separate actual mode1 matches:

- Witch match: `witch-gluttony`, `witch-wrath`, `witch-passives`, `witch-envy`,
  `witch-progress`, `time-stop`;58.52seconds including startup/setup/teardown.
- Neighbors: `ordinary-tools`, `ordinary-summoners`, `apocalypse-actors`,
  `projectile-self-hits`, `shish-encounters`, `classic-melee`;38.94seconds.

Exact logs/results: `5ed6d2dd-native.log`, `5ed6d2dd-results.json`,
`5ed6d2dd-neighbors-native.log`, `5ed6d2dd-neighbors-results.json` in the same
verification directory. Both runs shut down their owned child normally, restore
exact fixture snapshots and release their world/runner locks. No live child or
recovery archive remains. Previous artifacts/logs are retained.

The Gluttony fixture executes real raw main/offhand callbacks and the actual
scheduler, without advancing private actor clocks or forcing chance draws. It
checks native payload/AI identity, rabbit reproduction,100-rabbit cap, native
foreign-dimension existence gate,79-snake cohort, random eligible-target pool,
zero head rotation and first-child activation, fragment20 lifetime, scheduled
terrain and capped sculk effects, MAG, whale initial motion/clones/escape/HP sync,
old and new real Ghast-child causes across authority handover/cleanup, and fog
kill/impact/allies/roles/TimeStop/natural100 lifetime. The fragment producer edge
is invoked directly for deterministic first-activation coverage; the6percent
admission probability itself is not inferred from a statistical sample.

Scope limits: this is behavior qualification, not peak-TPS or physical-client
proof. The fixture snapshots119712 air-footprint cells synchronously before its
START marker, causing a roughly14-second setup stall in the isolated child;
this is test snapshot overhead, not a production Gluttony tick measurement.
Native bodies are held stationary for bounded terrain/clone assertions; natural
long-range flight, unloaded-region roundtrips, actual replacement login, full
acquisition and the complete Chaos graph remain separate work. Neighbor suites'
existing pending cadence/presentation limits remain. The known pre-suite Polymer
chunk-context diagnostic also remains; no clean-global-log claim is made.

199 starting dirty paths (including the actively edited question file),31 live
grimoire overrides and main JAR retain their exact starting bytes/deletions.
Protected Java/RP edits were not staged or overwritten. Source line endings were
retained on unchanged lines; no template regeneration or old-JAR deletion ran.

Next: Indolence/Lust active families, seven-sin acquisition/remaining authority
closure, and FULL Vanity world+all-player+core snapshot/rewind. Never substitute
a player-NBT-only checkpoint or a filtered25-trap pool.
