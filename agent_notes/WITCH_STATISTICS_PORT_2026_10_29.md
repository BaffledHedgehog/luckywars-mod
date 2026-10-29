# Witch native progress and Lust melee stage — 29 October 2026

Staged source consumers; not complete Witch Gens or a Chaos release. Main remains
`d9087f0a`. The original full25 transitive release gate remains closed. No staged
JAR installed, queued, or main server restarted.

## Production

`LbcWitchStatistics` binds `dmg_dealt` to native DAMAGE_DEALT and `lolidefecation`
to native BLOCK_MINED/MUSHROOM_STEM. LbcMana creates new objectives with these
actual statistic criteria. WitchStatisticsMixin prepares a missing objective
before ServerPlayer.awardStat's native forAllObjectives, so the first event is
not lost. For preexisting dummy objectives only, the same native event adds its
delta once afterward. Native criteria are not incremented twice; definitions
with a different non-dummy criterion are not rewritten.

Native resetStat resets the matching dummy too. Original item/food/reset score
writes remain authoritative. There is no polling or restoring of lifetime stat
totals, cached last-stat count, second mining event handler or migration that
deletes scoreboard objectives. A custom Witch reset remains reset until the next
real native award. Native stat events retain vanilla units and integer arithmetic.

Sleep progression already belongs to LbcShishEncounter.sampleMovement. Reused it;
no additional movement sampler. Native tests verify stationary100->101 and then
moving101->86 on consecutive real ticks, with original position quantization.

Lust's accepted damage callback uses the shared real-melee predicate, eligible
role/ordinary dimension, native source6block range and friendship/Greed rules.
Caster MAG suppression blocks it; TECH permits it. It consumes the real hit target,
including each accepted sweep target, without scanning unrelated HurtTime10 mobs.
Attributed NPC/spell/projectile damage cannot activate the owner's melee bonus.
This target-policy modernization and possible target-side MAG protection remain
explicitly pending author confirmation; no full release claim.

Shared transferHealth implements source integer base arithmetic: attacker floor
base+2, victim floor base-2, with no direct healing. A depleted victim base triggers
existing inheritedKill and its causal owner; original void-resistance and absolute
Greed protections remain. The helper is reusable by the later flesh spell rather
than inventing a separate life-steal engine. Flesh/dragon/random-mob closure is
still unimplemented, not aliased to this melee callback.

## Native proof

Candidate `e24a23e4`, SHA256
`e24a23e4f5c5934be784ed99ff27f9378663b9b35fd18dee7d3b05465fcf42f5`,
`build/verification/skywars2-witch-statistics-e24a23e4.jar`.

Offline build passed.10 gameplay suites passed across two isolated actual mode1
matches on this exact artifact: the9-suite progress group31.71seconds and the
classic-melee group27.55seconds. Normal owned-child shutdown/exact snapshotted
disk restoration in both; no child, fixture locks, check failures or cleanup
errors retained. Evidence: `build/verification/witch-statistics/`.

WitchProgressChecks exercises actual native/dummy award and reset, missing-objective
first mining via gameMode.destroyBlock, mutable score resets, unrelated-stat
exclusion, native player attack and attributed NPC/arrow hurt, integer base
transfer/no heal, real suppression markers, depleted-base kill credit and void
protection. Stat objective definitions, all score holders/values/locks/display
overrides and scoreboard display slots are snapshotted/restored in the isolated
fixture; mined native stats are separately restored. Existing native participant
state/listener/terrain/provenance helpers are reused.

Neighbor suites: witch-envy, witch-passives, kfc-counters, kfc-scores,
signed-effects-math, effect-layers, projectile-self-hits and time-stop. The separate
classic-melee suite retains its original source presentation/effects/RNG/team/
role/advancement checks. Its obsolete expectations that Feeding/Vampire should
activate from attributed MAGIC damage were corrected to the already approved
real-melee rule; Guardian/offhand/suppression controls now use PLAYER_ATTACK.
No production friendship or damage admission was weakened for old assertions.

Rejected runs retained: KFC payloads violate the classic fixture's empty-service
precondition if run before it; an older classic test required indirect triggers;
the old Greed fixture assumed the full footprint atY240 was always empty. It now
selects a verified resident air volume before taking snapshots. A prior candidate
also recorded one thaw assertion without sufficient state diagnostics; added
explicit stop/role/anchor diagnostics, and the final native stop/thaw test passed.
That earlier assertion's precise cause is not established; do not claim otherwise.

5770 frozen inputs:607 Java sources,1751 classes,3411 processed resources and
build.gradle. Packaged bytes match, except the established separately verified
Loom fabric.mod.json nested-SGUI transformation.23 original source anchors hashed.
198 author dirty paths,31 live grimoire overrides and main JAR remain byte-exact.
Progress diagnostics are the same666 normalized WARN/ERROR lines as69b0bca4.
The separate melee startup has those666 plus a pre-match2.487second overload
warning during Polymer generation and a lobby label-count warning (6 versus3).
These are recorded, not a peakTPS or clean-startup claim; cause of label duplication
is not established here. No check failed on the final candidate.

No complete acquisition, all authority/actor lifecycle, physical client/render,
real socket/Carpet integration, network latency or peakTPS qualification.
`qualified=false`, `releaseEligible=false`.

(user) Returned; subsequent questions go in chat. Existing pending answers are
being prepared. Finish this patch before receiving those answers; do not append
new questions to the file under the superseded sleep instruction.
