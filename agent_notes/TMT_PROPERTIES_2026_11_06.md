# TMT property kernel — component verified, 6 November 2026

Current continuation: [private property state/common wands](TMT_WANDS_2026_11_06.md)
supersedes the flat-write API and carry-forward candidate below. The user chose
separate version/mode per ability. This note retains the original kernel evidence;
the current internal archives are032c8fba/1783ca96, still not installed.

This is the first completed component of the final TMT work, not a released TMT
item. Creation, meaningful ability selection/composition, current native carrier
registration and the procedural item-atlas renderer remain gated. No token,
reward table, mode, kit, trader or random-item pool was enabled. Main and both
build/libs archives remain the qualified match-mode build `b8d1066e` / `256dc67b`.

## Source and author decisions

The source is the full datapack under
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/tmt/`,
not its simplified Paper replacement. The old server and the actual resource
pack are read-only. User decisions remain: creation is unjammed; TMT is one
logical item in selection pools; useful abilities come from all finished content;
ignore ineffective tags; current domains are uniform; arbitrary functions are
unsupported; the future procedural visual samples only the item atlas.

`tools/build_tmt_properties.py` reuses the literal source extractor. The resource
contains all source text and sound identities plus current author-pack translation
keys and sound definitions: 13,505 distinct text entries and 1,772 sound IDs.
The five repeated source text identities have one slot each. No strings are
translated or rewritten. Source array and base-flag SHA receipts and the three
author-pack input receipts are embedded. CLI output inside either read-only input
root is rejected before reading or writing output.

## Implemented boundary

Package-private `LbcTmtProperties` is independent of acquisition/dispatch. A future
composition owner supplies a native carrier factory and seven resolved, meaningful
ability writes. This kernel merges those writes in order; it does **not** choose
random abilities from the old 1,049 raw tags or claim they all work.

The kernel implements these source property families:

- Five uniform carrier choices; one output and preserved carrier defaults.
- Base TMT/category flags, ordered overwrites and isolated output metadata.
- Item name rather than custom-name override; six independent lore lines;
  literal/translatable choice, six hexadecimal color digits and all five source
  style probabilities.
- Golden-apple 20 nutrition, 20 saturation, always edible, two-second use with
  current random sound/animation and an explicitly replaced consumable component.
- Four equip slots and independent authored equipment/model ranges 1–707 / 1–12,758.
  Existing assets are the temporary source oracle, not the proposed new renderer.
- 29 attribute opportunities (1 guaranteed, 4×20%, 8×10%, 16×5%). Amount overrides
  remain independent and sequential, including the ±10,000,000 extreme.
- 21 enchantment opportunities (4×20%, 17×10%). The 1–255 and 1–15 overrides remain
  independent and ordered; duplicate enchantments overwrite in source order.
- Persistent visual seed for the upcoming atlas renderer, separate from names,
  position, camera, time and use state.

Current registries augment the immutable domains at construction. The tested
native registry generation yields 40 attributes, 43 enchantments, 13,878 text
entries, 2,112 sound IDs, 12 animations and 11 attribute slot groups. Every current
attribute/enchantment index is reachable; the old off-by-one range is gone.
The future owner must retain one kernel per registry generation and rebuild it
on registry reload. All per-output state is local. JSON parsing and registry
enumeration stay out of each random draw; generation has bounded source attempts,
no per-tick polling, world scan, command storage or function interpreter. Native
CompoundTag merge supplies copy isolation without a redundant pre-merge copy.

## Verification and limits

`sw check tmt-properties` exists only in the internal candidate. It checks every
current attribute/enchantment index, exact probability/overwrite branches and
extremes, all five carriers, actual component codecs, equipment/model endpoints,
food/consumable data, text/style branches, prototype isolation, seven ordered
writes and 64 pairs of repeatable generated stacks. The suite runs without
altering player inventory, granting items or spawning TMT entities.

Final candidate `b6468008` / production-shaped `880b1cc2` passes one actual isolated
mode1 match, its existing lobby checks and actual stop/cleanup. The child exits
normally; fixture files are restored exactly. Three fresh Java families and one
resource are overlaid on the qualified parent. All 10,302 other guarded source
inputs and 282 protected author paths remain unchanged. Retained 3,446 classes
and 27 overlaid classes have no unresolved removed-member references.

The first candidate `21b2be89` also passed; the final rerun covers removal of the
redundant merge copy, the output-root guard and explicit generation-only logging.
Gradle uses 512MiB/one worker; only after it exits does the isolated 2GiB fixture
run. Main SkyWars, TerraFirmaGreg, Velocity and the author resource pack are not
started, stopped or modified by this batch.

This is not full TMT, public mode2/4, client rendering, GPU, peakTPS or whole-rewind
qualification. There is no installer qualification or deployment from this batch.

Evidence: `build/verification/tmt-properties-2026-11-06/component-verification.json`,
`property-input-proof.json`, `linkage-proof.json`, `frozen-inputs.json`, and final
`b6468008-tmt-properties-results.json` / `.log`.

## Carry forward before the next TMT compile

These inputs are committed but **not present in the installed qualified parent**:

- `src/main/java/me/skywars2/game/LbcTmtProperties.java`
- `src/main/java/me/skywars2/game/TmtPropertyChecks.java`
- the `tmt-properties` registration in `GameChecks.java`
- `src/main/resources/skywars2/tmt_properties.json`

Continue internal TMT overlays from sealed candidate `b6468008`, or explicitly
carry these three families and resource into overlays based on installed
`b8d1066e`. Do not derive that carry-forward set only from git's uncommitted diff.
Keep the qualified parent/main/build/libs untouched until the full agreed chain
is ready. The archived activation drafts still require an event/owner audit;
they contain obsolete infinite-arrow charging behavior and are not ready to apply.
