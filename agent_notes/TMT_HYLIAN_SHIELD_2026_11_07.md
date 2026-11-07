# TMT Hylian Shield — native adapter authored, not JVM qualified

Owned new files only: `LbcTmtShield.java`, `TmtShieldChecks.java`, this note.
No shared catalog/runtime/components/items/registration edits; root owns wiring.
No JVM, semantic/JDT/index, Gradle, server, installation or commits. Previous
mana helper/check files remain frozen. This replaces the earlier **design gap**
with source-authored adapter work, not an executed qualification claim.

## Real source and native composition

`LbcMeleeItems.Kind.HYLIAN_SHIELD` is a real ShieldItem with source custom data
`hylianshield:1`, unlike `hylian:1` on the sword. Source is
`data/lbc/loot_table/item_defs/minecraft/shield__01ad7522ef57.json`, also reached
through `ported/hylian_shield`. It has native BLOCKS_ATTACKS, authored
unbreakability and its own banner/presentation. No invented sword aura is used.

Cached26.3 mapped classfiles were parsed read-only, without a Java process.
ShieldItem inherits the real Item.use implementation. Its native priority is
CONSUMABLE, swappable EQUIPPABLE, then BLOCKS_ATTACKS. Item.getUseDuration and
getUseAnimation also prefer the consumable. BowItem and CrossbowItem keep their
own duration, charging and release implementations. LivingEntity.applyItemBlocking
reads the actual used stack, checks BlocksAttacks.bypassedBy, piercing projectiles,
head-yaw/source-position angle, resolveBlockedDamage, native item wear and the
ordinary blocking callbacks. Player.blockUsingItem obtains the attacker's actual
getSecondsToDisableBlocking and calls BlocksAttacks.disable.

The approved composition keeps generated TMT physical durability, unbreakability,
food, attributes and presentation independent, as the existing Holy adapter does.
Only the source **BLOCKS_ATTACKS behavior component** is attached. The ordinary
Hylian source still remains unbreakable; the generated carrier can wear/break
according to its own native physical components. No UNBREAKABLE is copied from
the source recipe and no damage cancellation/interception is added.

An edible carrier retains its real32tick consuming window, blocks after the
native delay during that window, then performs its one actual consumption and
stops using. It is not converted into an infinite hold, and food is not hidden
or stripped. Bows/crossbows preserve their actual ammo/charging/release behavior;
without ammo they can start native ShieldItem use as a fallback. Loaded crossbow
shot precedence remains native. Carrot/snowball use can enter the existing shared
native source coordinator and sustained path. No fake ranged event is introduced.

## Root wiring

Add standard `DataComponents.BLOCKS_ATTACKS` to LbcTmtComponents.PAYLOADS so
nondefault source components are retained in the existing private typed codec.
This is a native standard component, not a new pseudo-tag.

`LbcTmtShield` APIs:

- `candidate(source)` requires a real selected ShieldItem, actual BLOCKS_ATTACKS
  and native Hylian identity or exact hylianshield flag. It never reads hylian.
- `sustained(outer)` recognizes the aggregated actual private trait. Include it
  in LbcTmtContinuous.sustained before generation computes EQUIPPABLE.swappable,
  so native equipment swapping cannot intercept sustained input.
- `apply(game,outer)` reconstructs each actual private source with the existing
  native component codec and attaches only its BLOCKS_ATTACKS. Call from the
  shared sustained applier at generation/rehydration.
- `use(game,owner,hand)` belongs in ExtraUses' private selected-source routing.
  It invokes the selected **actual native ShieldItem/Item.use** on the real hand.
  If that exact stack/hand is already using, it returns without restarting it.
  This is required because USING source groups replay on ordinary ticks; resetting
  native use every tick would prevent both blocking delay and food completion.
- `input(game,owner,hand)` is the native carrier fallback, returning InteractionResult.
  It selects an actual private source and invokes `use` inside its existing scope.
  Native Bow/Crossbow.use should try their ordinary super use first, then this
  fallback only for PASS/FAIL; an already loaded shot remains first. Existing
  carrot/snowball sustained routing can delegate to this helper for Hylian so it
  retains the same native consumable/equipment/block priority.

Root should admit the actual native source-use family with USING/held-blocking
metadata as appropriate, retire the earlier explicit Hylian component pending
finding only after real wiring, and register operator admission/poll/cancel with
`TmtShieldChecks.run/tick/cancel`. A source name alone is not routing proof.

No duration/animation/release overrides, second blocking service, player state
map, cooldown ledger, client food stripping or manual HP rule is required.
Native damage, criterion/stat, sound, directional guard, bypass, wear, break,
disable and cooldown all continue through their existing vanilla owners.

## Authored qualification

The fixture first uses the real authored unbreakable source, then all five actual
registered TMT carriers with a retained craft-only sibling. Five actual generator
outputs also check BLOCKS_ATTACKS and EQUIPPABLE.swappable=false. Real gameMode
input starts each use without fabricated ammunition. Actual immediate damage
passes the native blocking delay; later native front damage produces zero HP
loss, source-component physical wear, DAMAGE_BLOCKED_BY_SHIELD statistics and
native sound feedback. Rear damage and a real member of the component's bypass
set still reach HP. The two private source traits retain their immutable data,
and the sword's aura flag never appears.

Ordinary scheduled ticks verify the real golden-apple completion/debit after its
native consuming window, rather than manually completing food or advancing a
shield counter. An actual offhand block against an equipped native iron axe
disables through the real Weapon/component path; actual reuse fails on cooldown,
then succeeds only after its natural scheduler expires. A final block at the
physical carrier's real max damage breaks that actual item while blocking the hit.

Loaded-crossbow gameMode use additionally checks one real owned shot, an emptied
native chamber and retained private traits. A real bow draw with actual offhand
arrows reaches its blocking delay and native release fires one real owned arrow,
debiting exactly one actual ammo. Empty-ammo releases never fabricate shots.

Shared fixture snapshots/causal cleanup plus scoped native item/custom-damage
statistics restore the touched scene; all own projectiles are adopted before
assertions and retired. No fixture owner tick, blocking age, cooldown or resource
clock is manually advanced. Physical-client visuals, every damage type and full
public TMT release are not claimed. **No compiler or native fixture was run by
this task.** Root must retain the authored/unqualified boundary until authorized
component verification.
