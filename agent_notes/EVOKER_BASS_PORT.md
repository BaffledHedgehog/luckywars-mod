# Evoker and Bass wand families

Two ordinary wands and their complete Academy modes use the existing wand/mana,
Academy, targeting, Time Stop and item-epoch services. Four scoped public leaves:
`lbc:ported/evoker_wand`, `bass_wand`, `academy_evoker_wand`, `academy_bass_wand`.
Broad source loot pools are not newly qualified by these leaves.

## Source and behavior

Legacy namespace root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
- `function/workingitems/evokercast{,_pre,_upg_check,_upgraded}.mcfunction`,
  `fangs1.mcfunction`, `fangs2.mcfunction`, `fangs3.mcfunction`.
- `function/workingitems/basscast{,_pre,_upg_check,_upgraded}.mcfunction` and
  `basscast_combo{,_true,_start,_second,_more,_succesful}.mcfunction`.
- `function/other/magic_academy/{can_upgrade,upgrade}/{evocator,bassboost}.mcfunction`.
- `function/config.mcfunction`, `tick_not_timestopped.mcfunction`, `leave_death.mcfunction`.
- Source presentation comes from the Evoker/Bass entries in
  `loot_table/transformables_staff.json`, already converted in project resources.
  Name/lore/model/tier/flags remain intact; scoped upgraded leaves append the same
  green upgraded-item line and tier B used by the actual Academy promotion.

Evoker:700 mana; random cross/ring/fan, or explicit formation1/2/3 after Academy.
Cross:five fangs per eligible nearby mob, distance1.5..17, offsets1.2 blocks.
Ring:12 points on each of Y+0,+2,+4 (36 fangs). Fan:19 center points at local
forward2..20,16 points on each side, half-block lateral steps (51 fangs).
Pitch and feet anchor are preserved. No terrain snap or replacement attack clock.
Vanilla EvokerFangs owns animation, eighth-tick6 magic damage and natural expiry.
A narrow mixin for tagged wand fangs supplies shared physical targeting (including
friendly mobs), target antimagic checks and real owner attribution. Unrelated
vanilla fangs and Chaos cultist fangs remain native. Caster/allied players and
technical roles remain protected. The legacy near-Evoker advancement remains.
Trial retains the actual source check:40 existing fangs near eligible mobs, even
though the source RP description says50. No invented distinct-victim requirement.

Bass:800 mana; ordinary cast picks one of Weakness I8s, Slowness IV12s,
Blindness I8s, Mining Fatigue I9s within25 blocks. The upgraded item chooses the
effect and retains source player-only targeting of its Blindness mode. Ordinary
debuffs exclude allies; successful rhythm's sonic burst retains the source broad
nearby-mob selection, excluding caster and protected roles. Existing effect
admission/cleansing and damage policy still apply; no new stun system is added.
Rhythm uses native player scores `bass_combo`, `bass_combo_ticker`, `bass_combo_len`
and one active service clock. First cast starts, second establishes interval;
third onward accepts interval +/-1 tick, with actual interval >4 ticks. Success
increases combo, refunds500 mana and deals combo sonic damage. At combo>=10 the
source advancement is awarded. Failure starts a new combo immediately, matching
function execution order. No artificial cooldown, timeout or extra KFC counter:
these three scores are absent from the author's exact KFC +1 source list.

## Accepted global user rules

- PKM entry and configured parallel hand dispatch are reused, with independent
  costs. No LKM casts. Mode switches precede mana admission, including negative
  mana, as explicitly requested for all Academy wands (user).
- Real causal player attribution is required for all items (user); unlike the
  source's ownerless fangs these native fangs carry the actual caster.
- Friendly mobs can receive projectile hits, allied players stay protected
  (user). Native fang timing/6 damage and enchantment post-hit hooks are retained.
- No magical-projectile explosion physics, artificial TTL or cap is introduced.
- Existing resources in the author's read-only RP supply every reused key/model;
  no RP/template edits or copied translations are necessary.

## Lifecycle and verification scope

Managed fangs are native entities indexed by the existing loaded/unloaded, epoch,
forget and stop paths. Vanilla Time Stop pauses their real tick. Their service
clock pauses with the arena; combo counters reset on forget/match stop. Deferred
native unload cleanup remains the controller's existing safe boundary.

`EvokerBassChecks` exercises actual server item use, exact source geometry/cost,
native damage timing and lethal causal credit, friendly-mob impact, source
presentation, random debuffs/trial, rhythm refund/damage/reset, independent hands,
negative-mana switching, antimagic, Time Stop, Academy promotion and actual match
stop. `tools/smoke_evoker_bass.py` runs two actual mode1 matches and related
Lifestealer/Academy/KFC regressions. Final artifact evidence is recorded in
`build/verification/evoker-bass-final/` after qualification. Manual native ticks
are used for synchronous damage/rhythm boundary fixtures; this does not qualify
real-client pixels, disk restart/unload/disconnect or peak-load performance.

## Qualified release - 23 October 2026

Build63c4f9a3 passed16 recorded suite results in two actual mode1 matches
(53.72s). Exact264sources/718classes/2738resources match the tested JAR. Installed
to the stopped main server; rollback6b5b6fde retained, main not started. Three
preexisting user source hashes remain intact and are excluded from the commit.
Evidence:build/verification/evoker-bass-final/qualification.json and installation.json.
The only final ERROR lines are the known fake-player Polymer packet-context pair.
All final suites passed. Rendering, real restart/unload/disconnect and peak load
remain outside this qualification.
