# Player feedback and canonical loot — 31 October 2026

Implementation and native qualification are complete. This batch does not release the unfinished Chaos reward chain or install a main-server JAR.

## Gameplay changes

- Antimagic Sword has one source definition, `lbc:ported/antimagic_sword`, with `lbc:tier_s`. The duplicate `_loli` table is removed. Existing inventory items with the antimagic flag normalize only their tooltip style.
- Academy Duplicating Black Hole restores the source strong-pull radius of 7 and damage radius of 4, independently of its radius-7 terrain erosion. The weaker outer influence and bounded additive velocity remain.
- Nuclear mushrooms use the revised fade encoding while retaining legacy total visible duration: small Exoperso 200 ticks (120 main + 80 fade), large nuclear 400 (160 + 240). Delivery retention is invisible slack. See [source details](NUCLEAR_LEGACY_TOTAL_2026_10_31.md).
- Nausea I has no custom post-effect. Custom distortion begins at II, including reconciliation of stale saved chains.
- Chef can recover reachable water sources through native bucket pickup, traverses actual stair half-steps without requesting jumps, retains a usable route prefix during replanning, and clears overhead blocks before pillaring. Search slices retain their caches; cumulative target movement and adjacent footing at block edges invalidate the right work. Combat hand preparation and aiming proceed together. Pitch now uses bounded linear interpolation: wrapped pitch previously trapped the exact roof-to-floor turn at −90°.
- Negative Blindness keeps invisibility revelation and adds increasing contrast, grain and dithering, with 16 bounded visual tiers. Its full signed server level remains intact.
- `lbc:nexus` is reconciled from the actual dimension. Native dimension transfer, join/respawn/rollback and ordinary ticks remove stale projection outside `minecraft:nexus`.
- Black-hole shader rows wait through the old collapse tail before a returning/replacement handle can use another slot. Checkpoint replacement uses the same retirement rule.
- Hunger/Nausea II+ restore independent legacy 5% reaction rolls every two active ticks, respecting legacy mode and frozen/polymorphed admission. Honey/chicken particles, sounds and hidden one-second vomiting debuffs use the original payloads.

Chef world queries remain on the server thread. Async planning would require an immutable terrain snapshot and main-thread validation; retaining the frontier and local route avoids unsafe concurrent world access. Native sprint-jump impulse is preserved.

## Loot definitions

The refactor factors 8,016 item entries across 947 existing tables into 3,614 new `lbc:item_defs/...` definitions and existing standalone owners. Existing public table IDs remain usable. Eighteen preexisting duplicate standalone definitions become aliases. Genuine payload variants retain separate definitions where a shared prefix would reorder modifiers or leave identity components copied in callers.

Caller weight, quality, active `condition`, plural legacy fields, pool rolls/bonus rolls, entry order and modifier prefix/suffix order are preserved. The ignored old `functions` field stays verbatim on the caller; this refactor does not activate legacy functions. Native before/after codec witnesses check chance 0/1, active count 3 and ignored legacy count 99.

The first pass checks all 2,218 valid source tables; the additive pass checks all 5,812 then-existing valid tables. Final replay checks 5,832 valid tables with zero rewrites, zero skipped item entries and idempotence. The original empty, invalid `reroll_9999/material_block.json` is retained; it is outside packaged resources.

`tools/canonicalize_lbc_loot.py` is an offline staging/review tool, not a runtime generator. Edit the resulting JSON definitions directly. Additional singleton calls preserve the output distribution; fixed-seed identity is not promised. Exactly 98 existing singleton resources are additionally packaged, without enabling their unrelated source pools.

Most legacy items still use a vanilla carrier plus components/custom-data tags, recognized by Java callbacks. Separately registered Polymer items use their own server IDs and client substitutes. Native loot tables can issue either form; Polymer is not a second loot engine.

Live author-owned grimoire overrides are unchanged and continue to take datapack priority over bundled tables.

## Evidence and limits

Evidence root: `build/verification/player-feedback-2026-10-31/`. Canonical expansion, source hashes, staging and replay reports: `build/verification/canonical-loot/`, including the additive `opaque-followup/` report. Each pass verifies all source hashes before applying its staged files.

The final JAR has 5,183 packaged loot tables, 3,614 new definitions and 7,074 checked local table references with no missing targets. All production classes are byte-identical to the preceding 725f43dc build. That preceding build changes only Chef production code relative to b15e0dae; all other production classes and resources used by the earlier successful gameplay groups are retained.

Initial failed runs remain archived. Corrections include real Chef pitch/footing/roof-clearance bugs and test ownership/API issues: borrowed Gamma is preserved during Vanity tests, indirect damage is a negative sword control, authored Homing curse fixtures use long ticks, and accessory acquisition runs against the real source pool rather than the unrelated Tiny/InfBolt probe override. Stair assertions exclude deliberate combat hops after the ascent; roof tests keep their target outside melee until the required escape height.

GPU evidence `fried-gpu-results.json` records RTX 4080 SUPER compilation/linking, preserved alpha, visible grain and increasing contrast for tiers 1/2/4/8/16. These checks do not establish actual client rendering, camera/network latency or peak-load TPS.

Final e60cd86b passes seven actual mode1 matches: canonical-loot, ordinary-accessories, popit, chef-navigation, chef-recovery, chef and vanity-rollback. The child exits normally, exact snapshots are restored, and no locks or failure/cleanup records remain. The other eleven gameplay groups have passing evidence in the retained b15e0dae/725f43dc stages; production-code continuity and exact loot expansion are recorded separately. Protected baseline: 199 author paths, 31 live override files, main JAR SHA `d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087`. No main restart, installation, push or question-file edits.
