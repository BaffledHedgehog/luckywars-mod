# Normal Vex acquisition and sword source contract

23 October 2026. Implementation in progress; this file does not claim runtime
qualification. Root owns integration, sword mechanics, checks and final status.

Legacy D = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
The source server and actual resource pack were read only.

## Acquisition and native actors

D `loot_table/traps.json:357-400`: normal token is count1 repeating command
block, weight10 quality-10, tier_ff, model lbc:drop_boss; yellow nonitalic
trap_vex name and one white nonitalic trap_desc lore. Custom data is
{vex:1,drop:1}. The new native item skywars2:vex_drop and scoped
lbc:ported/vex_drop preserve this authored presentation; broad traps remain gated.

D `function/drops/specialdrops.mcfunction` dispatches vex to `drops/vex`.
The latter has exactly ten summon commands at the token's unchanged position.
Each actor is a native vex with Health1/max_health1 and DeathLootTable
lbc:vex_sword. No extra persistence, equipment, native owner, lifetime or cap
is authored. Native Vex defaults and AI remain native. The shared dispatcher
owns one debit; this service does not debit independently.

LbcCreatureDrops.Kind.VEX uses the existing creature admission, causal author,
resident-chunk readiness, epoch, Time Stop and quiet cleanup. A persisted
skywars2.normal_vex_drop tag distinguishes normal WILD targeting from existing
variants. Native Vex.getOwner is not reassigned to the thrower. Existing
VEX_OLD retains its five rotated forward spawns and original actor payload and
target policy. Protected roles still pass through validTarget.

## Real reward closure

D `loot_table/vex_sword.json` is one pool with a 0.1 random-chance condition,
then one lbc:vexsword roll. Each actual native actor death invokes that table;
quiet removal does not synthesize a reward. Its imported wrapper now uses
the current namespaced minecraft:loot_table entry type; probability is unchanged.

D `loot_table/vexsword.json` defines the green nonitalic vex_sword,
lbc:vex_sword model, tier_b, +2 mainhand attack damage with source modifier ID,
five lore rows and source transformable/sword/vexsword/magicwand/
swordwithability flags. The existing converted table retains all modifiers and
only replaces its item ID with skywars2:vex_sword, supplied by LbcVexSword.
The scoped lbc:ported/vex_sword forwards to this same real reward table.

Existing RP assets lbc:items/vex_sword.json and lbc:items/drop_boss.json were
confirmed present. Existing language keys are reused; no RP/template edits.

## Sword behavior requiring the complete integration

Raw source `workingitems/vexsword`, `vexsword_arrowl`, `vexsword_arrowr`
defines hit charge, one random flank native arrow, native piercing and target
selection. Raw source input is `spell1_default:51` and
`spell1_default_left:38`; historical generated prose calling it crouch is not
authority. `vex_ulta`, `vexsword_ulta`, `other/phantomarrows/1..8` define
the charged ultimate and 70-tick zone. The zone selects up to five random
eligible targets from the asymmetric source volume and creates real
groundbreaker arrows. Sword target and collision decisions belong to the
root's complete implementation and author clarification; this acquisition
change does not resolve them by introducing new limits.

Build/resource inclusion and actual native reward/input/lifecycle checks were
completed for both scoped leaves in the qualification below.

## Author's confirmed corrections (23 October 2026)

Only an actual PLAYER_ATTACK whose causing and direct entity is the sword holder
can charge and spawn the side arrow. Other attributed non-arrow damage never
qualifies. The one new side arrow is aimed at the actual victim, including an
accepted lethal strike, instead of selecting arbitrary HurtTime entities or
redirecting old/foreign arrows. All eight ultimate forms use PierceLevel100.
The rest of source offsets, base damage, native flight and terrain behavior is
preserved. Source swordcharging4/swordcharged4 scores drive HUD/KFC/purification.
No new translation, original RP change or texture is needed.

The independently inspected native26.3 LootPool codec uses singular condition
with type, not the source's plural conditions/condition discriminator. The
wrapper is converted deliberately so the native10% chance remains operative.
Qualification includes actual resource parsing and seeded native actor death.

## Native integration and qualification scope

LbcVexSword is owned by LbcItems and is registered before registry freeze.
The ordinary native damage callback handles positive accepted hits; Fabric skips
AFTER_DAMAGE on lethal hits, so AFTER_DEATH also forwards the actual source.
Shared role/team policy has a narrow allow-dying target overload; existing calls
still require a live target. Native arrows protect allied players while physical
hits may affect allied mobs; automatic field selection only chooses enemies.
Already launched genuine arrows retain author after the owner becomes inactive;
current-epoch managed identity gates the damage exception. Admission remains
restricted to an actual active player. Native owner/reflection wins over creator.
Purification only clears counters, matching source; stop/disconnect owns cleanup.
The antimagic field cancels the anchored magic zone, not already-fired arrows.
Resident checks never request new chunks. No arbitrary native-arrow TTL was added.

The source counter HUD uses E103, full charges and gray partial /10 in either
hand. KFC's source ledger already adds one raw unit to both counters, so no
second compensating increment is introduced. Existing source strings/models
were verified read-only. White-wool markers retain the source caster exclusion.

Early retained fixtures corrected native ServerPlayer.doTick equipment
application (tickNonPassenger is not the full player update) and the preexisting
Stalin test's mutation of a live entity iterator during cleanup. They did not
change game damage or source attributes.

## Final qualification and installation

Build43634ed9d7e33339f034a0fbf9cb5b6b763a097696a0d1877b94243c094f4586
passed two actual isolated mode1 matches:14 recorded suite results in50.89s.
All255 Java sources,697 classes and2719 resources match tested artifacts exactly.
The runner covers normal/old acquisition and debit, seeded native death reward,
real melee and lethal hits, native side flight/reflection, observer-role author
retention, all eight ultimate directions and actual groundbreaker collision,
field expiry, Time Stop, offhand guards, KFC/creature/Stalin regression and actual
stop cleanup. The old Stalin fixture now snapshots entities before quiet removal.

Evidence:build/verification/vex-family-final/qualification.json,
vex-family-results.json, vex-family.log and installation.json. The fake-player Polymer context error
pair is retained; no final suite failed. Both scoped leaves are ready for testing.
Installed to the stopped main server; previous42ad616b JAR is preserved under
build/verification/main-rollback/. Agent did not start main. Three preexisting
user sources remained byte-identical and outside this change's commit.
No actual disk restart/unload/disconnect, real-client pixels/full natural
trajectories or peak-capacity claim.
