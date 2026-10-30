# Native block and actor window bindings for Vanity

## Contract and release boundary

The [full author rewind contract](VANITY_ACTOR_CHECKPOINT_2026_10_30.md) remains
mandatory: FULL world/map and all players including killed/offline/newcomers,
all gameplay/private state, inventory/effects/attributes/tags/NBT/counters/queues,
core/map parameters, dimensions/positions/motion and authority ownership.
This extends the canonical [native views](VANITY_VIEWS_CHECKPOINT_2026_10_30.md),
[storage](VANITY_STORAGE_CHECKPOINT_2026_10_30.md) and
[workstations](VANITY_WORKSTATIONS_CHECKPOINT_2026_10_30.md) components. It is NOT
the full rewind transaction. Vanity acquisition remains disabled and the full
original25 Chaos gate stays CLOSED. No main installation/restart/push, reduced
pool, RP/template or pending-question edits. Questions belong in CHAT.

## Native bound windows and canonical contents

MatchBoundViews adds BeaconMenu, LecternMenu, MerchantMenu, HorseInventoryMenu and
NautilusInventoryMenu. The generic storage binding now also accepts native
ContainerEntity inventories, including chest/hopper minecarts and chest boats.
Every actor binding records its saved UUID, exact native class, level and shape;
replay resolves it through the ACTOR owner's current identity resolver. It never
looks up the nearest replacement entity or creates another inventory contents owner.
Pending actor loot tables are rejected rather than rolled by screen reconstruction.
Compound block storage cannot silently combine arbitrary entity inventories.

The physical Beacon and Lectern windows require CURRENT block/data/book-access
objects. Lectern page/book contents stay exclusively with terrain NBT; its native
book-access wrapper exposes the original block owner, so capture needs no world
scan. Beacon payment is an independent one-slot menu-local image, separate from
the BE's levels/primary/secondary data. Two players may have independent payments
for the same BE while observing the same canonical native data. Unfinished future
payment slots are cleared before native close, without dropping or refunding them.

BeaconBlockEntity writes Levels to native NBT but does NOT read it in loadAdditional.
MatchTerrainCheckpoint now restores that saved integer into the CURRENT native BE
data object immediately after NBT replay and before publishing/reopening windows.
This belongs to terrain restoration; the menu does not overwrite the BE with its
own copied array. Ordinary beacon loading outside checkpoint replay is unchanged.
Other private BE caches/animation fields still need their separate runtime owners.

## Merchant links and trade semantics

The actor owner already restores each AbstractVillager's saved offers, prices,
demand, usage counts and XP via native NBT. The window image records the CURRENT
merchant actor UUID, three independent input/result stacks, selectionHint, futureXp,
active-offer ordinal and result-slot removeCount. Replay binds activeOffer to that
ordinal in the freshly restored native offers list, never to the stale pre-load
MerchantOffer object. tradingPlayer binds to the actual replacement player body.
Replay calls neither trade completion nor updateSellItem/notifyTradeUpdated and
does not increase offer usage or award experience/achievements. Future trade buffers
are silently discarded before native close rather than returned to restored players.

Native server menu level/progress/restock fields are independent of the metadata
actually sent by ServerPlayer.sendMerchantOffers. A small publication observer
records the latter's window ID/level/progress/restock metadata without changing
ordinary gameplay behavior. After replay, the real native offers packet republishes
those values with the canonical restored actor offers/XP. Nonentity or detached
merchants still require their own adapters and fail explicitly; no offers are
randomly generated to fill a missing checkpoint graph.

## Mount and vehicle inventories

Native horse/nautilus opening bypasses generic openMenu and sends a mount-ID packet;
it must not depend on generic title capture or getType (mount menus have no type).
Replay uses the actual native openHorseInventory/openNautilusInventory path, the
CURRENT mount SimpleContainer, restored inventory columns and current player slots.
Equipment-slot containers are recreated by the native constructor against the
restored actor. Equipment/storage contents remain exclusively actor-body state.

Before sending a mount-ID screen packet, replay waits for native chunk and entity
tracking to pair that actor to the current player. The same readiness predicate is
shared with camera restoration. Missing pairing stays an explicit pending actor
UUID; finishViewsFrozen retries mount completion during frozen publication frames.
This does not teleport the player, replace body motion, or send an unknown entity ID.
Already-correct native windows retain their object/ID and do not reopen on retry.

Generic storage sessions resolve ContainerEntity by UUID/class/level/size and bind
the current actor container. They keep the existing native chest/hopper constructors,
slot restrictions, title/cursor/drag and current player inventory binding. Storage
matching now handles a valid future workspace/bound-window family returning no
generic storage image: it reconstructs the saved storage instead of dereferencing
null. Unsupported/private UI graphs remain explicit unfinished adapters.

## Exact native evidence

Immutable artifact: `build\verification\skywars2-vanity-bound-views-4e7196d4.jar`.
SHA256: `4e7196d46681438a052ea8e5c63faefe9d12280198d26616658c69717149494d`.
11,223 frozen inputs, including runners/prepared probe; all
5,374 processed class/resource entries matched the exact JAR.
Offline build passed on its first attempt. Source receipts cover
131 native source-archive entries plus the legacy and canonical
damage/view/workstation/body owners, 168 total. Native archive SHA remains
`1ad6e7adb36253216767c99b8fccc48da3a1b1eb53aef0bddc51936d5e873a6a`.

The new bound-window fixture first passed with workstations/storage/views/terrain
in one actual mode1 match (39.82 seconds). The SAME final artifact then passed
TWENTY-THREE suites in ONE actual mode1 match: `vanity-bound-views`, `vanity-workstations`, `vanity-storage`, `vanity-views`, `vanity-actors`, `vanity-player`, `vanity-core`, `vanity-terrain`, `map-lighting`, `time-stop`, `witch-acquisition`, `witch-lifecycle`, `polymorph`, `effect-layers`, `projectile-body-collision`, `entity-merging`, `momentum-teleports`, `glass-armor`, `indirect-kills`, `enchantment-rules`, `advanced-anvil`, `tiny-reforge`, `reroll-machine`.
Runner elapsed 66.0 seconds. Normal child exit, exact tracked
snapshots restored, no suite/cleanup failures or retained locks. Focus mode purposely
leaves qualified:false; the explicit completed suite list defines this proof.
No physical-client rendering, arbitrary-map, every private NPC/BE state or peak-TPS
claim is made. Pre-existing bootstrap warnings remain in the logs.

VanityBoundViewChecks runs eleven native rounds: Beacon, Lectern, Villager,
WanderingTrader, Horse, chested Donkey, three-column chested Llama, Nautilus,
chest minecart, hopper minecart and oak chest boat. It captures real native screens,
mutates payments/buffers/offers/equipment/storage, destroys physical BEs and retires
the actual backing actor, then causes genuine player death. Canonical terrain/core/
body/actor/link phases must produce a new player body and current native BE/data/
inventory/offer/customer references. It verifies independent personal buffers and
cursors, shared block/actor inventories, independent Beacon payments, exact Levels/
Page, offer uses/active identity and distinct native/published trader metadata.

Native action checks consume the restored Beacon payment, navigate and take the
Lectern book, execute the restored trade, and transfer native saddle/vehicle items.
Another full replay repeats each operation with exact slots/resources/XP and trade
usage/actor-XP comparisons. Offline/reconnect replay is exercised for both a trader
and a chested Donkey. Horse replay also performs actual Nether/client-world
replacement; recorded packets must show native actor pairing before the mount-ID
screen packet, and after respawn in that world-replacement round. Memory bots ACK
only genuinely emitted native chunk batches through the real packet handler.
Repeated link reconciliation must keep the correct native menu and emit no extra
open-screen packet. Vehicle rounds explicitly insert a different future CraftingMenu
before saved storage replay to exercise the cross-family null-match repair.

Expanded Tiny/Reroll checks reuse the prior native temporary loot probe and forced
crafting chunk preparation. Startup auto-enables that explicit test resource pack;
the focused five-suite run used original resources. The probe, level metadata and
native chunk-ticket files are restored after normal child shutdown. No gameplay
assertion was weakened and no rejected Java/native run was needed in this stage.

All 199 pre-existing author dirty paths and all 31 live override files remain
byte-identical. Main JAR remains
`d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087`.
Evidence: `build/verification/vanity-bound-views/{baseline,candidate,sources,final-audit}.json`,
SHA-named frozen input manifest and focused/final logs/results, build-1.log,
prepared-fixture.json and bounded local commit receipt. Previous root-documentation
tails remain byte-identical.

## Required follow-up

The ordinary native menu families now have explicit adapters for their supported
native block/actor backing graphs. This does not cover custom game/plugin/private
menus, arbitrary detached containers or private GUI controllers. Full input/writer/
lifecycle coordination, automatic offline replay/persistence, native/private BE
runtime, AI/navigation/sensors/RNG, multipart links, ability/attribution/FPV state,
TimeStop, world clocks/game events/listeners/metadata, entity-region/pending IO,
registry reloads and complete map jobs/baselines remain unfinished. Preserve all
canonical terrain/core/body/actor/combat/view owners. Full Vanity remains gated.
