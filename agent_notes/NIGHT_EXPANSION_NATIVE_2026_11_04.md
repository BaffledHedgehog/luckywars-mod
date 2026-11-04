# Expansion skill24 native port — 4 November 2026

Owned files: `LbcExpansionSkill.java`, `ExpansionSkillChecks.java`,
`src/main/resources/skywars2/expansion_blocks.json`,
`tools/prepare_expansion_palette.py`, this note. Root owns registration,
HUD/catalog, coordinator integration, compilation and live scheduled evidence.
No legacy source, main server, build scripts or resource pack changed here.

## Source closure

Read-only source root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/`.
All fourteen `expansion*.mcfunction`, `place_random_block.mcfunction`,
`place_random_block_storage.mcfunction`, all numbered `blocks/1..1106.mcfunction`
and `data/minecraft/tags/block/airs_only.json` were inspected/extracted.
The generator reads actual numbered files; `blockgen.py` is historical evidence,
not authority over later source edits.

* Exactly60 sneaking ticks charge. Release resets incomplete charge. Active
  sneaking advances source score. At80+, each admitted generation rolls20% end.
  No owner MAG/artifact gate was invented. Non-sneak cooldown rolls50% decrement;
  holding sneak preserves cooldown. Natural no-marker end preserves cooldown.
* Start makes air-under-owner stone, then cardinal clones in x+,x-,z+,z- order,
  then four directional main seeds at the same origin. Main seeds deliberately
  overlap; no position deduplication removes these source actors.
* Each marker clears `Items`, normalizes only exact all-six-faces-false red/brown/
  stem states to all-true, then independently rolls1% random default block.
* Each main direction creates forward main unconditionally, then two side markers
  only when no marker of any owner occupies that position; clones forward, sideA,
  sideB, down into `airs_only`; finally10% down-main marker if unoccupied. x mains
  use z+,z- side order; z mains use x+,x- side order. All four directions read.
* Each side creates forward marker if unoccupied, clones forward, rolls10% down
  marker if unoccupied, then **unconditionally clones down** into air. All four
  direction files read; down-copy probability was not confused with marker chance.
* Occupancy includes both current and next selector snapshots, all owners, and
  the marker being processed until its last operation. A counted position index
  preserves overlapping main markers; there are no per-block entities or scans.

## Palette and current-version standing instruction

Every numbered file is exactly one `setblock ~ ~ ~ default_block_id` (no authored
NBT or state properties). Extraction fails closed on any different grammar.
Source digest (ordered file number + NUL + exact bytes):
`a2d15ce9a768e8bc0f19d19d365792c375793905489ebc6605153ec7d7507171`.
There are1106 source slots and1104 distinct IDs: `test_block` and
`test_instance_block` each occur twice. Slot165 uses the updated
`minecraft:iron_chain`; the historical generator still says chain. Preserve actual
source slots and duplicate weights, without re-sorting or deduplicating them.

Root supplied the standing user authorization to update full old enumerations
for new content after1.21.11. Accordingly, native initialization retains those
1106 exact slots and appends every missing current **minecraft namespace** block's
default state, sorted by ID. No new native LBC Lucky Block enters the pool.
Registry traversal happens once, lazily, never on subsequent ticks. The scheduled
suite verifies complete current vanilla coverage and logs exact appended IDs/count.
Generator normal run and `--check` passed. Root owns runtime ID/count evidence.

## Native lifecycle / necessary adaptation

Shared `LbcStonekivi.Work` runs one bounded marker transaction per dispatch with
fair owner rotation. `MapManager` typed snapshots/restore own cloning and BE
components; ordinary blocks never serialize NBT. Container source `Items` is
removed and restored to the live source **before** publishing copies. Remaining
BE components and source LootTable fields survive, as in source `data remove Items`.
Writes use shared attribution scope, support-removal ownership and arena journal.

New native normal/neko/loli Lucky Blocks replace old mushroom proxy states.
They normalize to their corresponding ordinary all-six-faces-true mushroom before
the initial clones as well as every propagation operation. This intentionally
closes the source's one-tick initial proxy-copy window; native family blocks can
never multiply indefinitely while waiting for their next marker tick. Exact
ordinary mushroom false-state normalization is retained; other face states remain.

There is no total accepted-node limit. Shared CPU pressure stretches a selector
generation until all its accepted markers complete; only source end/release,
actual frontier suppression, skill loss or match cleanup retire accepted nodes.
Chunk cache holds at most8 temporary loading tickets and retries rather than
dropping nodes. Tickets are released after use and on stop/restore. No world scan,
command runner, function interpreter, background world access or per-cell entity.

`Policy.cell(owner,level,position)` is exclusively source frontier admission;
root supplies actual MAG+artifact position policy. TECH has no effect. TimeStop
freezes input, RNG, accepted work and source generation advancement.

Typed field checkpoint stores owner/level, exact score/phase/RNG, both ordered
frontier queues and fair cursor. The shared Dispatcher implements its own typed
queued-work image, retaining the registered job identity through full coordinator
restore. Native terrain/bodies are restored by the shared whole-match owners,
without private undo.

## Scheduled proof entry

`ExpansionSkillChecks.start/tick`, suite `expansion-skill`, uses the registered
`items().expansionSkill()` and real server ticks, sneak/release input and native
chests. It covers partial release reset;60 charge; real80+20% natural end;
air-only cardinal/down copies;64-diamond source cleared before any copy while
custom name persists; TimeStop and actual nonempty whole-coordinator replay;
cooldown hold/release; MAG owner charge continuing; real MAG/artifact frontier kill;
TECH positive control; all three native Lucky Block carriers and skill-loss cleanup.
The test snapshots accepted frontier neighborhoods before the next registered
tick, so natural unconditional main propagation can extend outside the original
Chef fixture without a production population limit. Extra chunk scheduled ticks
and terrain are restored during fixture cleanup. This diagnostic iteration is
qualification-only and is never called by production ticks.

Compilation/live suite execution and any resulting revisions are root-owned and
must be reported separately; this note does not claim those checks passed.
