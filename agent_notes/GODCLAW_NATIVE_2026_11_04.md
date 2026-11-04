# God Claw native owner — implementation handoff, 4 November 2026

Parent integration, author decisions and actual executed qualification: [ADVANCED_SKILLS_PORT_2026_11_04.md](ADVANCED_SKILLS_PORT_2026_11_04.md). Worker handoff below is historical; its unrun tests are not runtime evidence.

This note describes newly authored code, **not a qualified runtime release**. No build,
server operation, commit or resource-pack edit was performed by this implementation
agent. Parent owns shared registration, author-policy answers, exact-artifact validation
and deployment. Source closure: [GODCLAW_SOURCE_2026_11_04.md](GODCLAW_SOURCE_2026_11_04.md).

## Native implementation

`LbcGodClawSkill` holds sparse UUID owner states and per-world primitive packed-position
claim indexes. Each cell is a typed `(position, written, neighborCursor)`; current/next
queues preserve selector generations and cross-owner first-claim competition. There
are no markers, tick NBT, global entity scans, persistent per-cell entities or general
function interpreter. Exact source terrain is the literal vanilla red mushroom state
with UP true and the five other faces false; no custom-block/RP registration is added.
Containers are overwritten without refunds; existing terrain hooks own checkpointing.

Six neighbors always qualify for sampling; the other20 receive independent40percent
draws. Accepted cells recheck policy, not original block eligibility, when consumed.
Queued neighbor discovery resumes after asynchronous chunk admission with a retained
cursor. A pool of at most8 temporary chunk tickets is shared; processing uses at most
1024 attempts per tick,16 per owner quantum and the existing EffectTerrain deadline.
There is no total accepted-frontier cap and no budget-driven cell deletion.

Under pressure, one logical active step waits until the previous generation drains.
This can lengthen the real-time active duration beyond30 ticks. It preserves exactly
one attribute draw per source generation, avoiding load-dependent extra rewards or
silently discarded accepted terrain. The parent must review this explicit scheduling
tradeoff against source's synchronous per-tick markers. Frontier count is the retained
next-generation queue size, including deferred accepted nodes; native virtual work
has no vanilla loaded-marker lifecycle. Native lifecycle cleanup intentionally removes
old-world/offline/dead/skill-swapped owners rather than preserving source orphan races.

Exact26 source attributes/scales/signs reuse `LbcAttributeChanges.change`; the distinct
`LbcAttributeGains` reward map is not reused. Charge60, subsequent active61..90,
zero-frontier final quantization, random20percent non-sneak cooldown and sneak freeze
are explicit. A checkpointed rejection-sampled SplitMix64 owns random state. Attribute
base changes are permanent match effects and use the existing match attribute reset;
the skill owns no additional temporary attribute modifiers.

Item conversion rolls the existing exact singleton leaf
`lbc:item_defs/lbc/liquidabyss__6e31baa5f9d5`, consumes the entire original stack,
and source-binds one native output. Newly created liquid items remain eligible for
later source contacts. Mobs delegate to the existing Consuming native VoidMan preset,
which already owns40HP/10attack/source equipment/full voidman loot and attribution.
Ordinary living contacts use source-scoped captured `genericKill`, avoiding accidental
direct-melee callbacks; ordinary nonliving contacts use native kill. Native death/drop
hooks remain active. Complete families and Chef decisions delegate to canonical owners.

## Integration contract

Construct with `(GameController, Policy)`. Policy separates owner input/gain admission,
cell frontier admission and target admission, allowing source/author jammer decisions
without conflating charge, frontier destruction and target protection. Target policy
must protect administration/shop/helper roles and route coherent native families.
`removeFamily(owner,target)` returns true when handled, including an intentional Chef
survival decision; false invokes generic native kill. It must complete synchronously.
`summonVoidMan(owner,position)` returns true only after actual canonical native spawn.
Chef is routed directly through family removal rather than ordinary mob transformation;
if the author wants Chef transformation, that callback must explicitly summon then erase.
No author suppression or Chef policy is hardcoded in this implementation.

Public lifecycle: `tick()`, `clear(ServerPlayer)`, `stop()`, `score(ServerPlayer)`,
`prepareFrozen()`, `publishFrozen()`. Tick and clocks pause under native TimeStop and
global freeze. No loaded/unloaded callbacks are required: this service owns no entities.
Its package-private `checkpoint().prepare(lookup)` returns the existing
`MatchFieldCheckpoint.Plan`. Images retain owner UUID, world, source clock flags,
RNG, ordered current/next queues and partial cursor. Missing owners are reported in
the plan; install does not emit terrain/entities/effects. Temporary tickets are derived
and reacquired after restore, with no separate rollback system.

## Scheduled checks

`GodClawSkillChecks.start(game)` / `tick(game)` use the existing Chef fixture with
actual registered sneak input and real server ticks. A consumed-state perimeter fences
a finite stone strip inside snapshotted terrain. Cases cover partial-charge release,
60tick activation under a real positive TECH jammer, original64stack conversion,
one canonical transformed VoidMan, strict-map-object protection, nonempty frontier
TimeStop pause and typed replay, finite end, held-sneak cooldown freeze, stochastic
release debit and skill-loss cleanup. Fixture restores bases, terrain/ticks, players,
new outputs and new native summons. Root still owns nonempty **whole-coordinator**
rewind, concrete MAG/artifact/Chef author controls and x2/x4 shared regressions.

These cases have been authored but not executed by this agent; root must compile and
run them on the exact integrated artifact before any success claim.
