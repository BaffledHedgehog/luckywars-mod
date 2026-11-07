# Native bucks merchant component, 7 November 2026

Implementation only; not compiled, qualified, installed or publicly released.
User memory restriction: no semantic navigation/JDT/JVM/Gradle/server was launched.
The mandatory Serena initial instruction endpoint was read once; navigation used rg,
bounded file reads and Python source-archive reads.

Owned inputs: `LbcBucksTrader.java`, `BucksTraderChecks.java`, this note.
Main server, installed libraries, protected source baseline and author RP untouched.

Corrected immediate-RMB fidelity (supersedes the earlier unqualified flight design):
read-only `players_tick.mcfunction` line7 calls `items/snowballs` when this player's
snowball-use score is positive. `items/snowballs.mcfunction` line4 calls
`projectiles/eggs` without changing executor or position; separate earlier commands
rebind only their own commands. `projectiles/eggs.mcfunction` line29 invokes
`other/bucks/rc.mcfunction` immediately. Its two loot commands and villager summon
use executor-relative `~ ~ ~`; there is no collision wait, raycast or projectile
position relocation. The snowball was solely the RMB detection workaround.
`other/bucks/gen_trades.mcfunction` supplies offers as/at the new villager.
Two independent `imba/random_bucks` rewards; each currency draw has source count1..64.
Mason/swamp villager, level99, health/max-health1000000, persistence, translated
yellow `baffled` name, Xp9999999, LastRestock1, `luck_trader`/`spawned` tags,
death table `lbc:imba/random_bucks`. Ten offers: first seven sell a shared
`random_glack` choice for random bucks; last three buy a shared choice for bucks.
Every offer begins uses1/maxUses2147483647/xp1/rewardExp true/priceMultiplier0.
No suppression is applied, matching the explicit user override.

A native Item.use now synchronously creates the two currency rewards and trader
at the player's actual use position, then owns one item-used stat and used-hand
one-unit debit. There is no SnowballItem, Snowball, aim/throw sound, persistent
attachment, flight map, collision callback or special replay state. Outputs remain
inside any caller's existing inherited spell-source scope. Two initial reward
tables use the player as THIS_ENTITY; ten offer pairs use the new villager, matching
the original execute-as/at gen_trades boundary. `spawned` is applied after offer
generation. Complete rewards/offers are prepared before publication; failed child
publication discards this call's already-published children without consuming.
Currency entities use `setDefaultPickUpDelay()`, matching native LootCommand's
`loot spawn` publication rather than making executor-position rewards instantly
pickable. The exact cached target command's `dropInWorld` was inspected read-only.
Native merchant/entity NBT continues to own persistent offers and actor state.
No world scan, periodic NBT decode or registry walk is added to gameplay.

Selection deliberately delegates to shared `LbcLoadouts.rollTable("lbc:random_glack")`.
The checked-in historical mixed pool was not already the complete uniform union;
parent work owns its reconciliation and structural equal-slot checks. The trader
must remain gated until that prerequisite and the full TMT release are qualified.
No selection catalog, weights, count wrappers or resource roots were edited here.

Both sides retain the chosen complete positive components: native ItemCost uses
DataComponentExactPredicate.allOf(chosen.getComponents()), output is a copied stack.
Native ItemCost CODEC has no absent-component predicate: a selected template which
removes an item's default component requires extra treatment if found by the shared
pool oracle. Current tests round-trip actual chosen offer costs/results and fail
on an observable component loss, rather than claiming lossless arbitrary templates.

Follow-up exact source/bytecode audit confirms this is a real cached-cost loss:
ItemCost.CODEC and STREAM_CODEC decode through the three-argument constructor.
The cached26.3 classfile has constructor MethodHandle kind8 targeting
`(Holder,int,DataComponentExactPredicate)`, whose code calls `createStack` and
then the four-argument constructor. ItemStack's `(Holder,int,DataComponentPatch)`
bytecode calls `Holder.components()` and `PatchedDataComponentMap.fromPatch`.
Thus defaults plus positive patch restore a deliberately removed MAX_DAMAGE.
Source ItemStack strict validation rejects MAX_DAMAGE together with max-stack99.
No JVM/javap was run; Python read the exact cached classfiles and constant pool.
The added `removedDefaultWeapon` regression uses diamond_sword/count17/max99,
explicitly removes MAX_DAMAGE, round-trips MerchantOffers.CODEC and requires a
legal exact cached cost. It also verifies native positive matching accepts an
unspecified extra component (CUSTOM_DATA, with an explicit absence precondition).
Default LORE.EMPTY is already a specified positive source key, so changing LORE
is not a valid unspecified-component control; the first native fixture exposed
that oracle error and the test was corrected without changing production matching.
This regression is expected to fail before a
targeted native persistence adapter is supplied. A client disconnect is not
claimed without native packet/client evidence.

Narrow adapter now supplied as new `mixin/ItemCostCacheMixin.java`; root owns its
registration and native qualification. It injects RETURN of the exact three-arg
constructor descriptor `(Holder,int,DataComponentExactPredicate)`, reads the
public native `itemStack()` record accessor, and removes cached MAX_DAMAGE only
if the cache is stackable and no positive MAX_DAMAGE requirement exists. In26.3
the exact patch query is `components.asPatch().get(DataComponentMap.EMPTY,
DataComponents.MAX_DAMAGE)`; the one-argument old API does not exist. EMPTY makes
this a positive-patch query instead of default inheritance.

The four-arg live chosen stack, native predicate, CODEC/STREAM schemas, normal
max-one weapons and explicit positive MAX_DAMAGE requirements are unchanged.
Other removed default components are not reconstructed by this narrow adapter;
native positive predicates still cannot require absence. An input with additional
unspecified components, including additional MAX_DAMAGE, still matches when the
original positive predicate permits it. This does not claim full absence matching.
Additional tests cover native MerchantOffers stream round-trip/zero unread bytes,
unchanged max-one default weapons, intentional explicit MAX_DAMAGE requirements,
positive predicate object identity, extra-component acceptance and untouched
registered item defaults. No new JVM was run; root must compile/include the new
mixin and qualify the explicit native checks.

Additional native merchant qualification assertions now require real health and
max-health1000000, base max-health1000000, mason/swamp/level99, Xp9999999,
LastRestock1/default RestocksToday0 and the native saved DeathLootTable owner.
The exact cached26.3 Mob/Villager source still reads/writes those camel-case
source keys. After checking the ten offers, the fixture sets its own trader's
health to1 and kills it through actual `hurtServer(genericKill, Float.MAX_VALUE)`
without player attribution. ItemEntity UUID differences must show exactly one
new native bucks reward with canonical components and count1..64; the original
two immediate use rewards remain separately checked. Both used-hand merchants
exercise this path; runtime outcome of the corrected input is root-owned.

Added runnable check exercises real native hand use/debit/stats in the same tick,
no native projectile publication, exact executor-position currency/trader outputs,
two currency rewards, one merchant/ten authored directions,
native take/satisfiedBy/assemble and native offer CODEC. A nested-container witness
rejects a plain matching item and mutated nested components. This check explicitly
does not substitute draw counts for the shared pool's logical-slot uniformity proof.
Only whitespace/static source API checks were performed by this component owner.

Parent wiring retained: TOKEN registration, service/accessor, canonical currency
leaf/native token mapping, native factory composition and explicit disposable check.
Remove the former BucksImpactMixin registration/class inclusion and the bucks
loaded/unloaded/stop/reindex lifecycle hooks. Those methods and all flight state
have been removed rather than left as misleading no-op compatibility APIs.
Do not alter ordinary native villager/item actor checkpoint ownership.

The former all-five-carrier immediate-RMB oracle was incorrect and is replaced
by ten asynchronous **legitimate source-event** cases, both hands per carrier.
Exact read-only source evidence: items/bows line15 calls trigger_smth_not_bow
after the real bow event; items/crossbows line25 calls trigger_smth_not_crossbow
after the real crossbow event. Using-item/tmt_item/trigger_smth runs self/armor/
charging groups and does not cast the spells group. Therefore ordinary Bow/Cross
RMB and Apple using/finish cannot fabricate money. No production fallback was added.

The test selects the real Bucks useProfiles property by native item ID and a real
catalog reroll_machine sibling. Carrot RMB emits money once. Snowball RMB emits
money plus its genuine base snowball, retaining two independent debits/stat uses.
Bow and Crossbow no-ammo input is negative; actual native charging ticks followed
by real release/charged fire must create one native arrow plus money. The native
ammo identity/one inventory debit and base-shot/carrier-stat separation are checked.
Crossbow release and subsequent firing RMB are separate genuine native input
events/ticks: cached LivingEntity.releaseUsingItem calls updatingUsingItem for
Crossbow.useOnRelease, which intentionally claims that tick's USING group without
spells. The release is explicitly checked to emit nothing and retain charged
chamber/ammo/count/durability; only a later actual RMB fires. No guard is reset.
The former same-call release+fire oracle incorrectly conflated those two inputs.
Ranged inputs use legal count-one damageable weapons; the money property consumes
that one carrier. A fixture-only ENTITY_LOAD observer captures read-only outerCopy
when the real new merchant is published, proving the native source already has
damage1/full private sibling immediately before the money debit. Apple really
starts using, ticks to native finish, consumes one food, preserves private data,
applies its actual nutrition and food counter, and never emits money/projectiles.

Only actual gameMode.useItem and owner.releaseUsingItem enter event bodies; no
synthetic accepted-shot callback or manual money/useProfiles execution is used.
START_LEVEL_TICK executes queued native inputs before that tick's using-item
continuation; END-tick BucksTraderChecks.tick advances/asserts the scene. These
fixture listeners retain no scene after completion. No per-shot latch clearing
masks the source event order. Each accepted native action removes its own new
outputs immediately, before projectile physics can hit a freshly spawned trader.

PASS is emitted only when all ten legitimate-event cases complete. The existing
WoolBucket fixture restores native participants/configuration/attribution; explicit
carrier/TOKEN/AIR stats and level RNG restore in cleanup. AIR is included to avoid
leaking a stat if the real crossbow's post-shot getItem sees a consumed weapon.
That edge is a potential production continuation-order bug, not an excuse to alter
the source event oracle. This owner made no production changes for these cases and
ran no JVM; native qualification remains root-owned.
