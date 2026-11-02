# Skills 1 / 12 / 13 — 2 November 2026

Completed, qualified and installed on the test server; no server restart.
Source chains and original slots/unlocks are in [the source audit](NEXT_SKILLS_SOURCE_2026_11_02.md).

## Author policy

- Frozen Heart and VIP are not suppressed by either jammer or the strong artifact.
- Frozen Heart converts harmful Slowness into Resistance for the same remaining
  ticks, preserving infinity. Resistance matches the Slowness level, capped at IV.
  Useful negative Slowness is retained. Hidden harmful layers convert individually.
- VIP counts accepted damage from other players, including actual inherited
  ownership, within one server tick. A fatal other-player hit refunds the normal
  life debit if that tick's qualifying sum is **at least** maximum HP. Explicit
  self damage is excluded. The author's subsequent clarification also admits
  native last-attacker kill credit: a player-caused fall counts while the game
  attributes it to that other player. Unattributed environment damage is excluded.
  No old physical damage-type filter.
  Native death, killer accounting and replacement-body respawn remain intact.
- Falling hedgehogs are destroyed inside MAG or TECH fields, including the existing
  strong-artifact magic field. Explosions remain WILD, affect the owner/allies and
  destroy terrain with native explosion power 7. Period is 100 active ticks.

## Ownership and performance

The native `CombatTracker.recordDamage` boundary receives post-mitigation,
post-absorption, unclamped HP damage before death. The existing direct-HP bridge
uses the same boundary, so no second event synthesis or health-delta approximation
is added. VIP receipts are selected-owner-only, expire by actual server tick,
are consumed once and are cancelled by core rewind/lifecycle cleanup.

Slowness admission occurs before its first action. Selection, raw/forced chains
and native effect ticks reconcile existing layers without polling inventories.
Converted leases cannot later reintroduce the consumed slowdown. Reversed helpful
layers retain independent clocks.

Falling actors are ordinary native Silverfish, indexed by load/unload events.
Immutable causal tags and the existing source graph carry player attribution;
the phase lives in the native scoreboard and rewinds with it. The native actor
checkpoint re-registers their original identities, allowing load adoption without
a parallel private actor snapshot. No chunk forcing, ticking creepers, item scans
or per-tick full NBT copies. Native explosion propagation remains shared.

## Verification

Final test artifact `0971bd4f`, full SHA-256
`0971bd4ff26a2a8473b328ccff7cf81972ad30e73fd9e828163a265572df06e0`;
production `be0863df`, full SHA-256
`be0863dff2f3116e79dfa7d622398c1d0311a7db72f48010c2a85c3218d2884f`.
All eight scoped groups passed on this exact artifact in eight actual mode1
matches: contained-skills, contained-vip-respawn, contained-vip-sum-respawn,
contained-vip-fall-respawn, parasite, reactive-skills, special-tools,
signed-environment. Actual ordinary native deaths and replacement bodies cover
direct overkill, native40 + direct-HP60 equal to maximum HP in the same tick,
and a player hit followed in a later tick by lethal fall damage. Each replacement
remains an ordinary participant for twenty subsequent match ticks.

9805 frozen inputs, 2497 compiled classes, and all 7354 processed resources
(including separately verified Loom Fabric metadata) match. Unchanged resources
are byte-identical to the previously qualified `e11faf22` artifact; the two changed
JSON resources match current processed outputs directly. Production/test common
entries match, and all eight map-authoring/deletion classes remain absent from
production. All 236 protected author paths and 31 live grimoire override files
remain unchanged. Fixture port25577 only; exact player/config/OP/level/ticket
snapshots restored, child stopped normally and no retained locks.

Evidence is retained under `build/verification/contained-skills-2026-11-02/`.
The first compile exposed the renamed 26.3 permanent-invulnerability setter;
use `setPermanentlyInvulnerable`, not the removed `setInvulnerable`.
Earlier `596d5c50` passed the initial six-group scope before the author's last-
attacker clarification. `4b019f82` then passed three groups but its fall fixture
hit the actual match's spawn-grace FALL_DAMAGE=false rule. The retained failure
was fixed by enabling that native rule only for the scoped fall probe and restoring
it exactly; runtime grace and damage admission were not weakened. A separate
review removed the temporary, never-registered explosion carrier from the source
index after synchronous dispatch, preventing a retained phantom source.

The normal-stop installer confirms main `0971bd4f` is installed, preserving
`e11faf22` in `build/verification/main-rollback-e11faf22.jar`; restarted:false.
No main stop/restart/push, physical-client pixel/audio test or peakTPS claim.

Separate RP translation fragments update the old Resistance-II and VIP
single-hit wording. Existing author RP and main template translations remain untouched.
