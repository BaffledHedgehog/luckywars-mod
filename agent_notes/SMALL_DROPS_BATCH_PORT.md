# Small drops batch and unrestricted enchantment combinations

Later user requirements supersede this build's anvil price/maximum-level policy: [current anvil contract](ANVIL_CREATURE_BATCH_PORT.md). This document retains the qualification evidence for its original build.

22 October 2026. Four complete mechanics / six public leaves share one build
and two-match qualification. [Source audit](SMALL_DROPS_BATCH_SOURCE.md),
[OP Anvil source](NEXT_DROPS_AFTER_BEES_SOURCE.md).

| Public loot leaf (`lbc:ported/`) | Behavior |
| --- | --- |
| `op_anvil_drop` | Harmless falling anvil, 1395 XP orb and randomized Book of Absurdity |
| `op_anvil_drop_old` | Original fractional anvil position and fixed legacy book |
| `slab_drop` | Persistent invisible baby zombie carrying the +6 melee slab reward |
| `slab_drop_old` | Nonpersistent variant, original literal name and +3 reward |
| `yamete_drop` | Original water/spell particles and positional custom sound |
| `death_lust_drop` | Nearest eligible player receives original titles, sound, fake death broadcast and Twitching |

## Runtime contract

The shared dispatcher admits only active-match, loaded, current-epoch tokens.
Multiple flags execute in source order and consume one unit together. Time stop
pauses admission. Old and normal item presentation remain distinct. Only these
complete leaves are exposed; whole reward pools and unfinished modes stay gated.
Existing RP models, translations, sounds and styles are reused without RP writes.

User addition: enchanted books may apply their enchantments to any nonempty
item in an anvil. Different enchantments have no mutual exclusivity, including
all protection types together; the common compatibility predicate also covers
enchanting-table selection. Identical enchantments still use the ordinary level
merge and are excluded from duplicate table selection. Item applicability is
relaxed specifically for book application in the anvil; the enchanting table
retains its ordinary eligible-item and enchantment pools. Native XP costs,
maximum enchantment levels and Survival's anvil expense cap remain.

OP Anvil retains independent native command outcomes, strict versus ordinary
air clearing, native falling physics, settling and fallback item, and native XP
pickup. Its two internal book tables are explicitly whitelisted and adapted to the
26.3 modifier/type/condition schema, preserving source enchantments and rolls. Normal book has 43 independently selected enchantments, including
incompatible combinations; old book has its original 28 stored enchantments.
Vanilla anvil expense rules remain. Falling impact damage is disabled as authored;
terrain changes carry the actual source through the common indirect-kill scope.

Slabs retain native baby-zombie AI, health, invisibility, equipment drops and
despawn distinction. The reward modifier applies in the player's main hand, not
the zombie's head slot. Role/ally checks and actual author inheritance cover
melee and later fall/void deaths, with newer-attacker priority. Native conversion
retains author/epoch cleanup state without replacing Drowned's native loot rules.
Author UUID tags and the per-match author cache handle service unload/reload;
ordinary mobs do not become managed actors.

Feedback uses native outbound packets and the existing Psycho effect pipeline.
Yamete has 100 water / 10 spell particles and volume-1 positional MASTER sound.
Death from Lust filters eligibility before nearest selection; self/allied players
remain eligible. It sends the single U+E203 glyph twice and the original private
volume-10000000 sound. The fake death announcement does not kill or damage its
recipient. Twitching adds 1000 to the existing counter, retaining its established
cadence, cleansing policy, source and time-stop behavior. Rejected affliction does
not suppress independent feedback.

Actual match stop quietly discards managed anvils/rewards/slabs and clears
Psycho counters through their normal lifecycle. No additional tick actor, world
scan, chunk ticket or recurring task is added for feedback drops.

## Qualification

Build and two actual isolated mode1 matches passed on `c9a33498`: **104 recorded suite results**, 87.69s. All prior special-drop regressions run in both matches. Ready artifact: `build/verification/skywars2-small-drops-batch-c9a33498.jar`; evidence: `build/verification/small-drops-batch-final/qualification.json`, results and server log. 197 sources / 563 classes / 2684 processed resources match the tested artifacts. The isolated server is stopped, files restored and locks released.

Fixtures cover source metadata and real Polymer round trips, native book tables,
physical falling/settling/fallback, XP pickup, native slab reward death/drop,
weapon modifiers, attribution and conversion. Feedback checks capture actual
native outbound particle/sound/title/chat packets, payloads, recipient isolation,
ordering, target filtering, cleansing, native counter cadence and freeze.
Enchantment rules use actual Survival AnvilMenu takes and EnchantmentMenu clicks, including native XP/lapis/book debit, arbitrary-item damage effects, protection combinations, book merge and same-enchantment upgrades.
All drop suites also check shared admission/debit/order/epoch and actual stop;
all previous special-drop regressions run in both matches.

During qualification, attempt1 exposed ignored legacy loot-function fields (fixed by schema conversion); attempt2 exposed a fixture assumption about the old native Creative exclusivity winner; attempt3 exposed a recorder-only null-channel flush crash (fixed by no-op transport flush in the recording fixture). Attempt4 then exposed shared probe terrain: OP replaced the retained Bee hive block entity. Its fixture moved to Y300.7, disjoint from the retained hive; gameplay behavior is unchanged. Failed evidence is retained under attempt1/2/3/4. Enchantment fixtures were added after the user changed compatibility rules.

Limits: no real client pixels/audio or natural input-perception qualification;
no actual disk restart/unload/disconnect or offline coin-framework test. OP book
application uses a native AnvilMenu in Creative to test component transfer, not
Survival expense bypass. Literal Nexus-dimension filtering and private Psycho
author storage are source-reviewed. Native AI timing and worst-case capacity
are not benchmarked; total wall time includes startup, maps and regressions.

Main server JAR and user-owned Reroll edits remain untouched; installation is
deferred under the standing instruction to leave the checked build ready.
