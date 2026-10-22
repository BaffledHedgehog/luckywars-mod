# Ordinary and expanded shooter acquisition - QUALIFIED, ready without installation

Sources: [pool graph](SHOOTER_POOLS_SOURCE.md),
[presentation/native IDs](SHOOTER_POOLS_RESOURCES.md),
[existing acquisition review](SHOOTER_ACQUISITION_REVIEW.md).

## Scope and actual behavior

Both original tables were already packaged and reachable from native Reforge and
Ultra Reforge. The suggestion catalog never disabled those acquisition paths.
This batch replaces their remaining legacy vanilla carriers with the individually
qualified native Polymer payloads. It does not create a new Reforge engine or
change selection chances. Previous notes describing all shared shooter pools as
'gated' were imprecise: only public suggestions were hidden, not these native calls.

Seven original tables are normalized: transformables_shooter, _all,
razorpine_with_leaves, stalin, tuntijaa_terra_razor, helicopter_boss and slr.
The fourteen weapon leaves include thirteen distinct native item models and the
separate conditional Mobgun leaf. Every item matches its qualified ported payload,
including the previously authorized Mobgun 2.5-second lore and Minigun 2.5 damage
and movement/jump attributes. Razor Pine retains its separate 16-seed output.

Original table nesting, entry order, weights, quality, conditions and pool modifiers
are preserved. The ordinary pool keeps its Mobgun weight10 leaf and the separate
weight10000 leaf at fixed final_luck_modifier/lbc.math ==7. Expanded keeps the
ordinary table at weight7 and each of its six other branches at weight1.
Native COMMAND loot context retains its existing luck behavior.

No new luck setting is introduced. Native code does not yet select/reset
final_luck_modifier from LuckyWars config; this external scoreboard condition is
preserved and tested at0/7 with the original value and existence restored afterward.
That broader game-mode integration remains a separate task.

## Reuse and public testing

Importer tools/import_lbc_shooter_pools.py reuses existing schema migration,
qualified payloads, Minigun changes and read-only resource-pack checks. The general
Reforge importer calls the same normalizer, preserving all seven native tables
and refreshing already published pool aliases without publishing new ones.
Legacy value_check is bridged through the existing native int-condition migration. It compares
all source selection metadata and accepts only the established item adjustments.
Native LbcBolts/LbcLoadouts remain the only Reforge/loot owners; no gameplay
engine changes are needed for native shooter identity and presentation.

The two qualified public pool aliases are packaged as
lbc:ported/transformables_shooter and lbc:ported/transformables_shooter_all.
Original IDs remain for internal callers. Unrelated Lucky, armor, wand or sword
pools are not declared test-ready by this shooter-specific qualification.

## Final verification

Build and four native matches passed on SHA-256
`61a1b083b9a053b67ca6cbf0cf45bb205c324f9c6a662767625c90925f18e075`. Ready archive:
`build/verification/skywars2-shooter-pools-61a1b083.jar`; evidence:
`build/verification/shooter-pools-final/`. Not installed on the main server.

Two shooter-pool matches passed in 48.18 seconds, including startup/shutdown.
Native ShooterPoolChecks uses registered LootTable.DIRECT_CODEC to isolate every
actual entry deterministically and compare full item components/counts to the
qualified reference. It checks all 14 weapon leaves / 13 models, source weights,
fixed score 0/7, Razor 16 seeds, 1024 seeded complete graph rolls plus 128 native
LbcLoadouts rolls. Both public aliases equal the actual registered acquisition
tables and appear in the test-ready catalog. Real normal/Ultra casts cover stacks,
repeat-cast deduplication, unchanged inventory/zero mana, cancellation and actual
match-stop/epoch cleanup through the existing queue.

Two further Infbolt regression matches passed on the same final JAR in 58.67
seconds. They cover a real empty-loot override, unsupported inputs, full-stack
queue, exact2x2/3x3 crafting, egg/lightning ownership and ally behavior,
offhand/observer/time-stop gates, stop sentinel and actual cleanup. The existing
egg fixture was corrected to snapshot created entities before discarding them;
mutating the native entity lookup during iteration had raised a cleanup NPE.
The PASS marker now follows successful fixture cleanup.

Source graph, read-only RU/EN/RPR/model dependency checks and generic importer
normalization passed. The test reports record matching source/installed hashes.
No graphical-client presentation claim is made. Existing GUI loot-context warnings
and the known fake-player Polymer packet-context diagnostic remain in test logs;
this is not a claim of globally warning-free startup. The observed main JAR is
332415d6, byte-identical to the previous qualified shooter archive; this batch
did not install or stop the main server. RP/template are unchanged. The isolated
server stopped normally and released its native world lock.

ZIP CRC passed. All 2638 packaged source resources byte-match; fabric.mod.json
version expansion and bundled SGUI are verified separately. The build excludes
1977 source resources and contains 117 Java source files. Public pool aliases are
byte-identical to original acquisition tables; generic reimport normalization of
all seven tables reproduces the qualified payloads.

## Next work

After this closure, review remaining acquisition entrypoints separately:
legacy lightning-item transformation, the reroll machine, Tiny Teleport ground
Ultra Reforge, and LuckyWars random/config integration. These source routes are
not proven implemented by testing the existing two wand/tool entrypoints.
The separately audited hostile Helicopter Boss encounter remains a later native
entity/lifecycle stage; the boss-named weapon is already qualified.
