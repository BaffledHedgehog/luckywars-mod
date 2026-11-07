# Native clock-fragment simulation: component verified, not installed

7 November 2026. This component was written with raw reads only: no JVM,
compiler, JDT, Gradle, server, generator, commit or protected author-file edits.
Root owns shared registration, dispatch, release metadata and native runs.

## Author intent and source

Authoritative current-conversation answer to NIGHT_QUESTIONS_2026_11_04.md #32:
«сейчас этот предмет отключен, ну попробуй конечно его перенести но там со стороны
производительности у меня чувство что это невозможно сделать нормально. А так да,
симуляция мгновенного прыжка».

That question explicitly distinguished mod handlers from mobs, fluids and native
Minecraft processes. The requested implementation uses real whole-server native
simulation. Prior AGENTS public-OFF entries and TMT_CLOCK_FRAGMENTS_GAP note are
historical qualification status, not an author prohibition or a requirement to
reproduce every old datapack schedule replacement.

Legacy lbc/function/workingitems/spell1_default:70 and _left:55 select clocks:1.
clock_fragment/select and select_left read actual-hand STRING type. Their outer
items/spells:7,13 requires magicwand:1, suppresses MAG holders and the20-block MAG
aura, and applies offhand admission. AGENTS jammer decisions forbid inventing a
TECH exemption/gate from names; this implementation preserves existing shared
MAG admission and adds no TECH restriction, mana payment, item debit or cooldown.

Sixteen native profiles retain the exact original loot chooser, modifiers, names,
lore, model, flags, type and chooser weight/quality. Only their native item ID
changes to skywars2:clock_fragment. The unrelated time_clock__6b7715f03a34 leaf
is a time_stopper and remains untouched. ported/clock_fragment delegates lbc:clocks.

Native durations:1t/2t/5t/10t =>1/2/5/10;1s/2s/3s/4s/5s/10s/15s =>
20/40/60/80/100/200/300; jump1s/2s/5s/10s =>20/40/100/200.
The authored selector named30s actually invokes tick90s; its duration is1800,
explicitly preserving that discrepancy rather than silently normalizing its name.
The bare registered item has clocks/magicwand flags but NO type and is inert.

## Native ownership

LbcClockFragments.use validates the actual source, shared gameplay/role/offhand
admission and MAG, then queues immutable epoch/duration. Unscoped TMT aggregate
flags cannot execute. No native simulation runs inside the input stack frame.
endTick runs after all real END_SERVER_TICK maintenance, including Vanity.
Accepted requests serialize without replacing one another or imposing an extra
duration cap. Each request uses ServerTickRateManager.requestGameToSprint.
Minecraft's main loop owns worlds, mobs, physical effects, packets and mod hooks.

Cached26.3 raw classfile evidence:

- requestGameToSprint(I)Z replaces scheduled/remaining counts, resets measurement,
  saves isFrozen and calls setFrozen(false). Return means prior remaining>0.
- checkShouldSprintThisTick decrements remaining before the real native iteration.
- isSprinting tests scheduledCurrentSprintTicks>0, including final remaining0.
- stopSprinting returns false at remaining0; the next outer-loop check normally
  finishes. The owner waits for settlement before starting the next request.
- finishTickSprint()V clears scheduled/remaining and restores previousIsFrozen.

ClockFragmentSprintMixin intercepts setFrozen(true) and a foreign
requestGameToSprint BEFORE their native bodies. The owner settles only its own
sprint through ClockFragmentSprintAccess's private-finish invoker, including
remaining0, and clears its accepted future before another owner takes freeze or
sprint state. It never settles an externally owned sprint. stop/epoch/map/ending
also cancel its own continuation. A real rewind cancels this transient simulation
request; it does not resurrect future sprint work from the saved point.

TimeStop remains canonical. A frozen input cannot cast through the existing
action guards; an already accepted request may advance the existing TimeStop
countdown while native bodies remain frozen and later thaw. No second freeze
engine or blanket ban on all worlds' TimeStop is added.

## Root integration

- Bootstrap LbcClockFragments.register(), e.g. from LbcGadgetItems.register.
- LbcItems field/constructor/getter clockFragments(); existing stop =>stop().
- LAST SkyWars2 END_SERVER_TICK statement, after vanity.tick():
  game.items().clockFragments().endTick(). This is not LbcItems.tick work.
- Register mixins ClockFragmentSprintAccess and ClockFragmentSprintMixin.
- LbcTmtExtraUses: candidate duration(stack)>0, CLOCK input and dispatch to
  game.items().clockFragments().use(player,hand), using normal selected-private
  source dispatch. Native class Item.use already calls that same owner.
- Catalog: invalid/missing clock type is an inert default only; authored valid
  profiles are native used-hand USE, with STRING type preserved per private slot.
  Remove clocks legacy-owner gap only after qualification and regeneration.
- GameChecks suite clock-fragments =>ClockFragmentChecks.run/tick/cancel.

No fabricated Apple initial-use event: active old datapack source does not provide
a generic Apple spell entry. The existing self/consume event domains remain.
Clock checks use real native carrot/snowball and bow/crossbow shot events in both
hands; Apple retains the private property while consuming through its actual
native32tick pipeline without an invented clock callback.

## Qualification scope

ClockFragmentChecks is authored, not run here. It checks all16 complete source
profiles in both hands, exact real world/match/server/connection advancement up to
1800ticks, native item/projectile gravity and effects, native scheduled block/fluid
work, packet output, private TMT events, rejected forged aggregate flags, MAG/TECH,
offhand, immutable FIFO requests, TimeStop thaw, final0 and mid-flight freeze,
foreign sprint replacement, and the actual whole coordinator capture/rewind.
The bounded resident scene cannot qualify peak TPS, arbitrary maps, all client
presentation, or constant-time instant simulation: native work remains O(ticks).

## Root native qualification and carry-forward

Sealed component: build/verification/tmt-clock-fragments-2026-11-07/component-verification.json.
Final test ae8e96c8 / production88402d2e carry the full722cc054/232a4cce parent.
Eleven focused native suites pass. The authored16-profile/two-hand matrix alone
accounts for5956 actual accelerated iterations; the real TimeStop interaction
adds100 more, alongside real four-carrier TMT events, FIFO, external sprint/freeze
and complete actual Vanity restore. This is a bounded resident scene, not a
large-map/peak-TPS or physical-client presentation/input-compression guarantee.

Root added shared ordinary-dimension and protected-role intake checks. Registration,
actual real END boundary after Vanity, stop lifecycle, native source canonicalization,
bare-default service classification, mixin list and ready resource are integrated.
All16 source leaves are proved identical except the native item ID. The original
chooser, unrelated time_stopper leaf, full authored payloads and costs remain.
Eleven Java families/twenty resources are explicit overlays;10436 other inputs and
282 author paths remain exact. The original HUD wrapper stays in the worktree;
only its one unqualified author line is excluded from the filtered compile and
commit. Source/full-archive/retained-member/protocol-hotfix proofs are recorded.

The initial uniform-pool regression exposed an overly strict check boundary:
source shock_potion deliberately emits raw3..6 quantities on a max-stack1 item.
The final check records the actual native error (raw5/max1), validates identical
components separately and tests actual LootTable.createStackSplitter output for
every raw index. Every final stack must be strict-valid with exact item/components
and no lost quantity. The original source count/modifier and production behavior
are unchanged;64/99 native outer-wrapper checks remain strict. Initial failed
receipts are retained; full archive equality confines the final change to the
UniformItemPoolChecks family, allowing explicit reuse of other passing suites.

One512MiB/one-worker compiler then one2GiB fixture run sequentially. All fixture
children exited normally, restored their tracked files and retained no locks.
Main/libs/RP unchanged, no main start and no partial installation.

Full TMT stays active/unreleased. Three actual legacy owners remain (Gayradar,
Steampunk Rocket, Troll Box); their launch jammer policies await an already-pending
async author question. Additional native/loose/charged/ammo domains, acquisition,
prepared atlas integration and coherent final release remain. Fresh native-domain
snapshot is reconciled separately; its125 rows/48 IDs are not ability counts or a
completion percentage. Next concrete adapter is retained native LbcLooseRewards
copier/tablet2/3/4 replay, using the existing owner and approved suppression rules.
