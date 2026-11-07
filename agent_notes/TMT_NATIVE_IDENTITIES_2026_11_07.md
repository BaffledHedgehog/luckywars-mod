# TMT native identity normalization — authored, not JVM qualified

Bounded source work; no semantic/JDT startup, JVM, Gradle, server, artifact
installation or commits. Owned new files only: `LbcTmtNativeIdentities.java`,
`TmtNativeIdentityChecks.java`, this note. Root owns shared catalog/rules/check
registration. Existing author changes and the stopped main remain untouched.

## Exact routing evidence

| Domain | Actual owner predicate and retained behavior | Required catalog action |
|---|---|---|
|Native Ice Sword|`LbcItems.isIceSword`: native identity OR `icesword:1`; accepted native melee tail invokes existing `LbcFrost.hitTargets`, including its actual recently-hurt/nonallied7block selector and MAG guard|Normalize native default to `icesword:1`; existing rule unchanged|
|Four native Overload pieces|`LbcMana.matches` reads raw CUSTOM_DATA `overload:1`; source `ported/overload_{helmet,chestplate,leggings,boots}` grants exactly this flag. Native defaults omit it. Mana counts actual HEAD/CHEST/LEGS/FEET bits; full mask15 alone bypasses wand mana admission|Normalize these four identities to `overload:1`; existing rule unchanged; **never add mage:1**|
|Native Rocket Boots|`LbcItems.isRocketBoots`: native identity OR `explosive_feets:1` OR retained Paper PDC; feet equip initializes charges; native fall-flying input invokes existing boost owner, motion, charges/cooldown/TECH checks|Normalize native default to `explosive_feets:1`; existing rule unchanged|
|Source night-vision iron helmet|`LbcAimbobRewards.tickPlayer` reads raw `night_vision:1` on actual HEAD every20 admitted ticks; grants300tick Night Vision0, removes Darkness/Blindness/Nausea; TECH blocks new grants and TimeStop pauses|Rule `night_vision`, EQUIPMENT/EQUIPPED, owner LbcAimbobRewards; no fabricated native identity required|
|Source TNT leather chestplate|`LbcBalanceExtras.damaged` reads raw `dynamite:1` on actual CHEST; actual accepted damage dispatch creates one native owned TNT/fuse80 under its original role/action/TimeStop/TECH guards|Rule with distinct family `dynamite_chestplate`, INCOMING/EQUIPPED, owner LbcBalanceExtras; do not conflate with snowball throw owner|

Existing TMT persisted aggregate flags expose the two raw head/chest predicates;
their owners do not require an artificial call or a new owner scheduler.
`LbcTmtAbilities` still owns selected source scopes at melee intake; normalization
only changes the catalog's private data copy and preserves private mode/type.
No ordinary native item default, owner class or TMT runtime is changed here.

Hylian Shield is separate: actual native `LbcMeleeItems.Kind.HYLIAN_SHIELD`
extends ShieldItem and carries `hylianshield:1`; no custom Java consumer of this
flag exists. `hylian:1` belongs to the **sword aura**, so aliasing it is wrong.
The native shield's BLOCKS_ATTACKS, held-use duration and directional incoming
blocking require a real sustained-use/component adapter on TMT carriers. Existing
`LbcTmtContinuous` supports Holy Shield/Nahida, and its Hylian extension is still
pending. Keep an explicit component/native owner finding; no inert exclusion or
support claim follows from `hylianshield` alone.

## Root wiring API

Call `LbcTmtNativeIdentities.normalize(stack,data)` after taking the mutable
private CUSTOM_DATA copy in `LbcTmtCatalog.collect`, before the probe/known-input
comparison and RULES matching. It returns void and changes only the provided
compound. Native source stacks remain untouched. The helper's
`hylianShield(ItemStack)` identifies the native identity or legacy flag for a
specific pending component finding; it grants no behavior.

Add the two catalog rules described above. Register operator check admission
with `TmtNativeIdentityChecks.run(game)`, normal check polling with `tick(game)`
and cleanup with `cancel(game)`. The authored fixture resolves actual catalog
variants by their native source item plus exact semantic flag, rather than
manufacturing a property or invoking private owner tick methods.

## Authored qualification and limits

The fixture checks six real default prototypes without mutating them; compares
all four Overload normalizations to actual source loot; preserves mode/type;
rejects ordinary iron sword identity; and keeps Hylian Shield distinct.
Actual accepted player damage reaches the existing Ice Sword/Frost route.
Four legally slotted generated pieces combine Overload with canonical source
night-vision, TNT and Rocket traits; three pieces fail native overdraft while
four permit the exact debit, and neither invents Mage protection. A native
START_FALL_FLYING packet spends one rocket charge and applies real motion.
Actual incoming damage creates exactly one owned fuse80 TNT, immediately cleaned
up before it can explode. Ordinary scheduled equipment ticks test head-only
Night Vision/three-effect cleansing, real TimeStop pause and inventory-versus-head
exclusion. Complete private slots remain retained after callbacks.

Shared fixture snapshots restore player/core/terrain/attribution/RNG state;
the check additionally restores the helmet owner's phase map/epoch, clears its
own frost and rocket state, and rejects an already active borrowed rocket state.
These tests are **source authored only**. No compiler or native fixture was run;
root must retain that qualification boundary. This does not qualify Hylian
blocking, full TMT release or physical client behavior.
