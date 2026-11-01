# One Punch and the infinite shulker upgrade chain,1 November2026

Author answers in chat: One Punch and the entire shulker chain are unjammed.
One Punch updates only its own damage, retaining all other stack properties.
No RP changes or function interpreter are introduced.

## One Punch

Native Polymer iron sword keeps the exact original name/lore/model/tier/flags,
one durability and effective source modifier list. The source table declares an
enormous attack-speed modifier first, but its final set_attributes uses replace=true
and leaves only the health damage modifier; native defaults preserve that effective
behavior rather than silently reviving the discarded modifier. Original canonical loot keeps its
count/weight/functions and score-derived initial damage, but now yields the
native item. Existing sword/loli references already point to that canonical root;
the new ported alias does the same, with no duplicated item definition.

The existing ClassicMelee held-item clock refreshes the selected MAINHAND every
active ordinary-world tick from ceil(native HP+absorption), matching the health
criterion. It does not trust or rewrite a stale dummy hplvl score. No inventory
replacement, full loot reroll or own runtime timer is used. Unchanged health
avoids component/list allocation; other damage modifiers and all other components
survive. Hylian keeps its existing5tick cadence in the same loop. Offhand/stored
items, spectators, technical roles, Nexus/Imprinted and paused holders do not
refresh. MAG, TECH and strong antimagic do not prevent the physical refresh.
Native melee, durability, damage attribution and stats own actual attacks.

## Infinite shulkers

Exact unopened donors require both original flag and CONTAINER_LOOT identity.
Two base shells -> improved; improved shell + infinite lucky block -> lucky.
Personal2x2 and workbench3x3 use normal output slots and vanilla unit debit/Shift
handling. Extra ingredients and already-opened/wrong-table donors do not match.
Cached output admission rechecks role, ordinary dimension, active clock and menu.
Matching/preview consumes nothing, emits no feedback and advances no shared RNG.
The two constant output tables use an explicit local seed for pure preview.

Ground recipes reuse the existing Nyauwu/technical fusion item index, native
loaded/unloaded hooks and20active-tick cadence. Distinct eligible donors within
.001..1block are chosen once; output publication precedes one-unit debit from
each. Source black/gold100forced dust and enchantment-table MASTER sound remain.
This removes the legacy whole-stack deletion bug while keeping recipe costs.
No second item scan engine, global per-block scan or private rewind engine exists.

Native placement, container loot fill, open, break and loot storage remain vanilla.
Base and improved fills include their own unopened replacement inside contents.
Lucky fills need the old generate_shulker continuation: after actual native fill
returns and its loot key was cleared, one lucky shell is spawned at the opening
player with owner/epoch/normal pickup. No hidden criterion state is needed. Reopen
of a filled container, null-player reads, unrelated tables, spectators, protected
dimensions and checkpoint player restoration cannot create an extra shell.
Completed native fill and its replacement form one synchronous event; a clock
pause cannot strand the reward after contents have already been generated.

The callback wraps RandomizableContainer.unpackLootTable, not an arbitrary legacy
function. The native advancement filter deliberately excludes legacy internal
function-driving criteria, so generate_shulker remains unshipped and unchanged.
There is no reason to re-enable it: actual native generation already gives the
exactly-once boundary. The unshipped onepunch_get tick advancement is also unchanged
and unnecessary for native refresh.

The source audit found five main contents/output roots existed in source but
were omitted by the build allowlist. These exact5 roots are now included AND their old functions/function fields are
converted to native modifier/type fields. Otherwise26.3 silently ignores those
functions and returns bare shells without a loot binding. The
3643-table reachable graph has no other missing packaged references. Original
loot distributions, modifiers, native splitting, count64 rewards and context luck
remain untouched. Lucky runs its nested source table26times; that table has extra
pools and can return multiple rewards per roll, so a fixed26-stack/1664-unit
assertion is wrong. The final check validates count64/maxstack64/no durability on
ALL raw rewards and compares every actual slot against a separately seeded native
fill with the same CHEST params/player/luck/origin. In the observed fixture61raw
stacks/3904units become27filled stacks/1728units under vanilla capacity/overflow.
That pre-existing native overflow behavior is retained, not replaced with an
invented reward distribution. Base/improved keep their source27-slot outputs.

Full LuckyWars/kit6 acquisition stays gated; this does not implement deferred TMT
or claim every behavior in that broad reward pool is finished. Existing exact
operator/staged roots are usable. Actual resource pack/template are untouched.

## Evidence

Verification is in build/verification/infinite-gear-2026-11-01/. Native checks are
scheduled through the real connection/player/controller clocks and actual grids,
placed block entities, loot generation and accepted melee. They do not simulate
callback success or tick private gameplay clocks. Final receipt follows below.

Retained preliminary failures:7664e6db exposed the new26.3 hidden criterion codec
(loot_tables list plus typed all_of player conditions), corrected without changing
its meaning. cd6813c4 exposed the five unconverted loot roots and an incorrect
fixture assumption that the overwritten attack-speed modifier remained active.
These failures are not qualified. Compilation also caught a missing closing parenthesis,
modern dyed-block registry names and private fixture connection access; fixes are
limited to the new definitions/checks, not weakened production hit/clock behavior.

3152bd28's native checks reached/passed the real crafting/preview paths. Its raw
control needed explicit legacy custom_data/model/lore components because native
transmuteCopy retains patches, not native default flags (the same distinction
already documented for Evolution). Its obsolete hidden-criterion assertion also
exposed the intentional advancement filter. Removing that unnecessary callback
progress state leaves only the physical native generation transition. This failed
receipt is retained, not qualified.

5ebf88ab passes the complete One Punch scenario, including actual native attack
and durability/stat debit, plus all requested unjammed controls. Its shulker
check's invented constant reward total failed; aa38d77f corrects that fixture to
the exact seeded source/native-container comparison and passes infinite-shulker,
classic-melee, mage-nyauwu, phd-crafting and full vanity-rollback in5real matches.
Final code removes only the obsolete words "revoked criterion" from the check's
PASS log. Assertions and all gameplay/resources remain byte-identical to the
scoped native evidence; final-log-only-edit.json records the exact edit.

Final qualified artifact970eef844d5d142960c1c5f757d1ce56ddeafaf1679014907491e17b0b1cbb20.
9770frozen inputs,2435compiled classes,7350processed resources match final JAR.
All226author paths and31live overrides unchanged; no RP/template edits. Six
scoped native groups are mapped individually by qualification.json; One Punch's
retained5ebf88ab and five final aa38d77f groups have identical production and
resources. Only the InfiniteShulkerChecks fixture differs, and its final edit
removes obsolete log wording without changing assertions. Six qualified match
observations, seven total including a retained failing fixture;110.85s total.
Children exit normally; exact snapshots and additional level/ticket files restored,
no locks. No physical client/peakTPS claim. Shared normal-stop installer only.

Installed970eef84 through the shared normal-stop worker (PID33696); main hash
verified, prior5b79a46f retained as rollback, restarted:false. main-install.json
records the receipt. Main was not stopped or restarted by this task. No push.
