# Native IMBA arrows - 6 November 2026

Qualified and installed without starting/restarting the main server.
Test `ee25e74d614ba205e89f06d91af604d0b03e6e0baaf3d5ab2cc6f1e75898f05e`; production `2d654998c2bcf61da320b0f07a0d33c8aeaedc88075698113a5e0de503943a90`.
Evidence: `build/verification/native-imba-arrows-2026-11-06/qualification.json`.

Ready leaves (one canonical definition each):

- `lbc:ported/imba_strong_arrow`
- `lbc:ported/imba_weak_arrow`
- `lbc:ported/imba_poison_arrow`
- `lbc:ported/imba_reactive_arrow`
- `lbc:ported/imba_explosive_arrow`
- `lbc:ported/imba_evoker_arrow`
- `lbc:ported/imba_paralyzing_arrow`
- `lbc:ported/imba_electric_arrow`

Original1..10 quantities, components, lore, tooltip styles and effect payloads are preserved.
Only the carrier item ID changes in the eight canonical definitions. The common native
item prototype reader binds registry-dependent components; no per-tick JSON/NBT prototype scan.
Native arrow selection tags now include the eight arrows and the existing Stand Arrow.

Strong/weak arrow damage is configured before native swept collision. The native critical
path saturates its integer damage safely. Poison retains infinite poison/wither and source
3600/8=450 seconds of delayed death, assigned rather than stacked; this uses the existing
acid delayed-death authority without unrelated acid cast visuals. Generic Raid120 intake
also shares this owner when consumed through the existing PhD pill.

Reactive arrows use real zero-fuse, zero-motion native TNT, source back3 placement and
owner/team14-block safety, through the common TECH explosion policy and child authority.
Nuclear, vacuum, paralysis and electrical terminals reuse their existing owners. Per-carrier
terminal claims prevent removed arrows or retained beam/shulker contexts from replaying.
Shulker bullets retain the author-approved4HP damage. All accepted ammo effects and
composable trinket traits stay attached through normal bows, crossbows, Shulker and beam
conversion; no replacement potion/color engine or duplicate explosion engine was added.

Author decision: Evo's block contact pulls all non-player entities within100 blocks,
including allied/unowned mobs, items and projectiles. Service/map objects are excluded;
ability visual children move through their canonical origin transformers. This is shared
with the old vacuum status. Danmaku paired visuals and consuming caches now participate.
Personal paralysis remains until death; mobs use saved native NoAI/invulnerability/gravity/
silent flags and effect clearing, while causal ownership tags are retained.

Validation: 12 actual isolated native matches, exact archive hashes,
source component oracle, real both-hand bow input and elapsed crossbow charging,
payloads/TimeStop/TECH, shared virtual visuals and nuclear/electric terminals, plus the
related existing suites. Exact fixture snapshots restored; no physical client or peakTPS
claim.21 Java families/17 resources overlaid on the prior qualified archives;10199 unchanged
source inputs and282 author paths guarded. Original RP and legacy source remain untouched.

One heavy job at a time; Gradle1GiB/one worker, fixture2GiB/256MiB initial, explicit collection
between isolated matches. No user Minecraft, proxy, editor or unrelated tools stopped.
Full IMBA pool, TMT-last/custom+mixed+typed catalogs, Bucks, new game mode and broad rewind
release gates remain unchanged. Player-owned weapon paths are qualified here; actual unowned
dispenser terminal payloads still need a separate shared-Quiver authority migration and contact
checks before the broad ammunition gate can be lifted. Existing typed carrier copy/checkpoint fields retain the
new terminal/reactive scheduling state; this is not a new full-rewind qualification.
