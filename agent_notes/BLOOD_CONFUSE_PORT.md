# Blood and Confuse wand families

Four scoped ready entrances after qualification: `lbc:ported/blood_wand`,
`academy_blood_wand`, `confuse_wand`, `academy_confuse_wand`. Both normal and
Academy variants reuse LbcWands dispatch/costs/hooks and LbcAcademy admission,
completion and promotion. LbcBloodConfuse is stateless: it owns no entities,
per-tick jobs, duplicated effect timers or persistent counters.

## Source

Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
- `function/workingitems/bloodmagic{,_pre,_can_upgrade,_upgraded,_upgraded_pre}.mcfunction`.
- `function/workingitems/foxuscast{,_pre,_upgraded,_upgraded_pre,_upgraded_pre_left}.mcfunction`.
- `function/other/magic_academy/{can_upgrade,upgrade}/{bloodmagic,foxusing}.mcfunction`.
- `function/config.mcfunction`:Blood5000 mana, Confuse900 mana.
- `loot_table/transformables_staff.json`, `item_modifier/add_upgrade_desc.json`,
  `item_modifier/tiers/a.json`:exact names/lore/styles/flags/models and tier C
  base ->tier A Academy. `tools/import_lbc_blood_confuse.py` extracts only these
  four leaves and audits28 existing RU/EN keys and both models against the
  read-only author RP. No RP/template files need changing.

## Blood

Source radius10, excluding caster, allies and protected technical roles.
Entities in the original mobs tag receive18 cactus damage attributed to the
actual caster. Surviving enemies receive Wither II30s; enemy players also receive
Hunger256 for20s. Native damage acceptance/cooldown remains authoritative.
Normal caster receives Instant Damage II(one native tick), Wither II15s,
Poison II15s, Hunger256 for2s. Original sound,1500 redstone-block particles and
red warning remain. Resistance V (exact amplifier4) grants the source Blood
advancement. Academy version omits all self damage/debuffs, with unchanged
outgoing attack and cost; it has no mode selector.

Trial:Shift+PKM of an admitted normal wand performs the ordinary cast, upgrades
self Poison/Wither to infinite level V and marks the used stack ready. The normal
cleansing/effect-admission paths still apply. No new immunity, bypass cooldown,
conditional trial or cap is introduced.

## Confuse

Normal casts randomly select one of four actions, Academy chooses mode1..4:
1. Invisibility I10s and original mirror smoke/sound.
2. Slowness51 for3s to enemies at0.1..10 blocks, original warnings/smoke/sound,
   then random safe-surface teleport in the square +/-10 around the cast origin.
3. Speed IV10s +Jump Boost IV10s, original rods/sound.
4. Blindness I6s to enemies at0.01..10 blocks, Levitation XI1s and Slow Falling I8s
   to caster, original poof/sound. Academy Blindness remains player-only.
Self buffs and enemy debuffs are native effects with original hidden flags.

Trial retains the actual legacy check on mode2:caster has Slow Falling, Speed,
Jump Boost, Invisibility and a nearby mob has both Blindness and Slowness. The
nearby query remains anchored at the original function execution origin, even
after teleport. Source >=50 integer-block upward displacement grants the Confuse
advancement. Failed safe-surface selection leaves the caster in place; effects,
cost and subsequent trial checks still run, as for a failed spreadplayers command.

## Bounded native surface selection

Vanilla SpreadPlayersCommand can resample the same void columns up to10000 times
and synchronously read/load chunks. Here the single-target conditional selection
visits each resident column once (at most441). Empty columns are rejected from
WORLD_SURFACE height data; the existing LbcPortalDrops.spawnY implements the
native two-air-block scan, starting below the known empty space. The original
safe-block/liquid check and Nether upper height120 remain. Weighted reservoir
selection accounts for partial edge columns, preserving the distribution of a
successful source spread over the resident safe columns. Native cell centering,
Y, yaw/pitch and teleport flags are retained. No chunk requests, arbitrary retry
cap, moving entity, queued teleport or special world tick is added. RNG sequence
and the chance of failing despite an existing safe column intentionally differ.

## Global accepted rules and lifecycle

Both hands use the existing configured parallel PKM dispatch, pay separately and
retain their actual stack identity. Confuse mode changes require no mana,
including negative balances (user). No LKM cast or projectile physics is added.
Existing role/action/antimagic/Time Stop guards reject casts before debit.
Damage, blindness, Wither and Poison pass the existing native effect-admission,
cleanse and LbcIndirectKills service. All item effects must inherit their author
(user); a later real player hit overrides old effect attribution (user). Blood
self harm remains a self-originating native effect. Existing player/match cleanup
owns native effects; no parallel effect/kill-tracking framework is introduced.
Broad staff/dust/encounter pools remain gated; this batch does not expose the
unclosed Corruption ->Corrupted Dust summoning chain.

## Verification

BloodConfuseChecks tests real native item use, source presentation/costs, native
self harm, enemy/ally/technical/radius behavior, actual Wither death and later-hit
override, actual offhand trial/infinite curse, Confuse effects and trial anchored
at the old origin after a real >=50-block upward teleport, native safe-surface
coordinates/rotation, unloaded-column rejection, both hands, free switching,
antimagic/Time Stop, actual Academy promotion on successive server ticks and
actual stop cleanup. Native effect boundary fixtures include manual native ticks.
`tools/smoke_blood_confuse.py` runs two actual mode1 matches plus previous
Evoker/Bass, Lifestealer, Academy and KFC regressions. Final release evidence:
`build/verification/blood-confuse-final/`. Client pixels, disk restart/unload/
disconnect and peak-load behavior are not claimed by these fixtures.

## Qualified release - 23 October 2026

Build46db086d passed18 recorded suite results in two actual mode1 matches
(53.36s), exact266sources/721classes/2742resources match the tested JAR. Installed
main while stopped; rollback63c4f9a3 retained and main not started. The three
preexisting user source hashes remain unchanged and excluded from the commit.
The only final ERROR pair is known fake-player Polymer packet-context output.
All final suites passed. The Blood/Confuse and prior Evoker/Bass fixtures now
wait for native entity visibility before their actor queries: real fixture
composition exposed the old immediate-after-force-loading assumption. This is a
test-only correction. Final evidence:build/verification/blood-confuse-final/.
