# Nexus uncrafting native count codec audit — 3 November 2026

Stage 5's expected 27 iron ingots, one carrot and one fishing rod is correct.
The exact likely observed mismatch is three ingots: `uncrafted/iron_ingot_9`
retained legacy entry `functions` and function discriminator `function`, which
Minecraft 26.3 ignores. `fishing_rod_carrot` and its singleton definitions produce
plain vanilla items. The native Orb Gun's SimplePolymerItem projects
CARROT_ON_A_STICK through the existing `LbcPhdCrafting.legacyItem` helper.

Evidence is the local official 26.3 sources JAR under `.gradle/loom-cache/`:

- `UniformContainerBase.commonFields` reads singular `modifier` via
  `LootItemFunctions.CODEC`; it does not read legacy entry `functions`.
- `LootItemFunctions.TYPED_CODEC` uses default codec dispatch discriminator `type`.
  Its DIRECT_CODEC accepts a typed object or an inline SequenceFunction array.
- `SequenceFunction.MAP_CODEC` itself uses `functions`; this current sequence
  payload must remain intact when modernizing legacy entry fields.
- Existing `LootDefinitionChecks` has a native codec witness proving active
  modifier count 3 and ignored legacy functions count 99 on the same entry.

The generator's explicit `--modernize` reuses
`tools/import_game_resources.py:migrate_loot_schema(..., preserve_imba=True)`.
Only the 594 selected uncrafted root resources are staged. After recursively
expanding native singleton references, all 594 trees must exactly equal their
codec-normalized original source trees before any resource is written. This
preserves entry ordering, authored counts/providers and all other tree values.
Shared item_defs resources are audited and never written.

A read-only dry run replaced Path.write_text with an in-memory recorder:
292 planned root changes; 855 total closure resources; all 1,027 original entry
occurrences and all 396 set_count occurrences passed. No resource writes, build
or server operations were performed during this audit. Root owns execution and
native qualification; the dry run alone does not establish live output counts.

The runtime snapshots original entity identity, components, count and position;
rechecks before every debit; and caps consumption at the original count. Produced
entities are never independently reselected. Native merges/pickups/removal may
cancel or reduce remaining work, but cannot increase the captured count. There
is no rollback inventory or XP ledger to duplicate committed outputs. Loot is
rolled before debit, then debit and complete outputs execute on one server thread
without a checkpoint/yield boundary. Admission failures or exceptions after debit
could lose output because addFreshEntity results are not transactional; they do
not cause retry duplication. The fixed audited tables only produce item stacks
with set_count, so arbitrary loot-function side effects are outside this closure.
