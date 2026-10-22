# Shooter acquisition review — 21 October 2026

Bounded read-only code review; no native server executed. Source server was read only. Pool leaves/resource CRCs belong to the separate pool audit.

## Live acquisition and visibility

- `LbcBolts.java:62,85,386–445`: normal `shooter:1` selects `lbc:transformables_shooter`; Ultra selects `lbc:transformables_shooter_all`. Rules match independent exact integer flags; transformable dropped entities within sphere R1 are captured. Source equivalents: `lbc/function/reforge.mcfunction:8`, `ultra_reforge.mcfunction:8`.
- `build.gradle:149,274` already includes BOTH shared pools. Therefore previous documentation saying these pools are disabled is false for current build configuration. Changing autocomplete alone cannot enable/disable acquisition.
- `LootTestCatalog.java:39–43` filters suggestions only. `skywars2/loot_test_catalog.json` already publishes every `lbc:ported/` ID by prefix. A closed, qualified shared pool can be added as explicit table IDs for manual testing; registry inclusion and nested references continue working independently.
- Resource references also exist from `lucky_items`, `lucky_items_high_quality`, `random_item`, `27_shooters`. Their presence does not prove native LuckyWars/random acquisition callers are implemented. Keep that as a separate boundary.
- `LbcCraftingRecipe` has no shooter recipe. Its INFBOLT recipe makes the tool, while its Razor recipe consumes a shooter donor; neither is another random shooter-pool route.
- `LbcShooters.java:532` rolls `lbc:kfc_yumyumyummy` with player source for chicken rewards. `LbcKfcRewards` supplies runtime reward item behavior, not a shared shooter pool grant.

## Conditions and ownership gap

`LbcLoadouts.java:396–403` resolves loot through native reloadable registries using COMMAND context, ORIGIN and optional THIS_ENTITY. Reforge passes the dropped ItemEntity, consistent with legacy `@s`; KFC passes the owner. The Mobgun luck condition uses a FIXED score holder and therefore does not require player THIS_ENTITY.

`final_luck_modifier` / `lbc.math` has no native owning integration: Java references are only `LbcMobgun.java:124` (read) and `GameChecks.java:7357` (temporary test manipulation). `LbcLoadouts.rollTable` synchronizes only ranked; `GameConfig` has no luck setting. Fixed score ==7 can be exercised by explicit scoreboard state, but native match/config does not select/reset that value. Broader LuckyWars luck selection remains unimplemented; avoid silently inventing it in shooter closure. Tests must save/restore this score, including across two matches.

## Loss and duplication assessment

`LbcBolts.java:394–445` snapshots inputs once; each unit validates entity, stack identity/components/count and loaded chunk. Repeated casts capturing the same input cannot duplicate it because the second job fails validation after mutation. Fresh outputs are excluded from that cast. No matching categories preserves input. Entirely empty rolls preserve remaining input. Mixed flags intentionally produce one independent roll per matching category.

Exact64 arrows roll magnum ammo once and then consume remaining63 without more rolls, matching legacy recursion. Owner/epoch cancellation preserves unprocessed remainder. These are already covered by meaningful native Infbolt checks.

Residual edge: outputs are spawned AFTER shrinking input and `addFreshEntity` return value is ignored (lines424–428). A rejected item spawn can lose consumed input. This is not demonstrated by ordinary gameplay and is shared with many grants; do not claim tested atomic acquisition. More important for current integration is preventing incomplete or empty referenced loot from entering the pool.

## Existing verification and recommended additions

Existing `/skywars check` cases in `GameChecks.java:701–713`: `infbolt-menu`, `infbolt-crafting`, `infbolt`, `infbolt-empty`, `infbolt-stop-probe`, `infbolt-cleanup`, plus `boltbolt` normal Reforge checks. `tools/smoke_infbolt.py` supplies a real zero-pool override and two matches. `infbolt` tests mixed-category counts, repeated cast deduplication, unsupported preservation, exact63/64 arrows, armor exclusions, and source presentation. `infbolt-empty` exercises real empty loot for BOTH normal and Ultra. KFC integration cases: `kfc`, `kfc-rewards`, `kfc-scores`, stop/cleanup.

Add one bounded shooter acquisition check using actual native dropped shooter input through normal and Ultra casting, drain shared queue, verify exactly expected conversion count and only known native shooter identities/components; repeat cast before drain. Test fixed luck holder values 0 and7 using native condition evaluation and restore original score. Exercise nested expanded grants through actual tables, rather than only standalone ported items. Keep leaf coverage deterministic (controlled isolated loot override or seeded context), since random sample counts cannot establish complete leaf coverage. Existing per-weapon tests remain necessary for behavior; acquisition checks establish route/shape and conditions only.

Root recommendations: close actual shared pool references against qualified native leaves; expose the two shared table IDs if desired; update stale disabled-pool documentation; add shooter acquisition/luck-condition checks and run final artifact in two matches. Do not add unrelated LuckyWars mechanics or promote unrelated rare acquisition callers solely because shared shooter pools are ready.
