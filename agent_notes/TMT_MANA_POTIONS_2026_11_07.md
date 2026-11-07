# Native potion payload domain — authored, not JVM qualified

Owned new files: `LbcTmtManaPotions.java`, `TmtManaPotionChecks.java`, this note.
No shared owner/catalog/component/registration files changed by this task. No
semantic/JDT/index startup, JVM, Gradle, server, installation or commits. Root
owns integration into its carried1929bf7b component contract and preserves the
protected author WIP. Tests below are authored source, **not executed proof**.

## Actual source domain and native authority

The current canonical `skywars2/tmt_use_profiles.json` contains these35 distinct
authored mana payloads, verified by a read-only JSON pass:

| Native transport | Exact authored Raid Omen amplifier/duration profiles |
|---|---|
|mana_potion:14|16/4800;17,18,19 each/1;16..20 each/3600 and/9600|
|mana_splash_potion:10|16..20 each/3600 and/9600|
|mana_lingering_potion:11|20/20;16..20 each/3600 and/9600|

These three registered defaults are EMPTY transports, not three additional
independent doses. `LbcMana` consumes sentinel16..20 into its existing counters:
temporary boost, permanent maximum, regeneration, addition, destructive mana.
It retains the source duration2-to1 normalization, actual recipient/time-stop/
custom-effect guards, stat order, Greed exception and cause/award ownership.
This adapter never writes those resources itself.

There are also18 authored raw vanilla potion:1 profiles: OP drink, gvozd
lingering mixture, Succubus drink; sentinel40/43 splash,74 drink,104/126
lingering; ten infinite harmful splash effects (Blindness, Darkness, Hunger,
Mining Fatigue, Nausea, Poison, Slowness, Unluck, Weakness, Wither). The whole
native PotionContents is retained, including color, effect order, duplicate
layers, strength, duration and display flags. Succubus is Regeneration II12000
plus Saturation XI20, and remains the exact `LbcSheep.potion` ingredient through
its actual transformable+potion+succubus flags. It is not a mana sentinel.

Four additional BR/Bad native identities are real chemical payloads, not empty
carriers: Termit234, Sulfur235, Phosphin236, Gremuch237, each Raid Omen20ticks.
`LbcItems.ChemicalPotionItem` extends LingeringPotionItem and overrides only its
Polymer carrier. It uses the same inherited native throw factory. `LbcClouds`
already recognizes its actual source and cloud sentinel, owns mayRun,
suppression, lifetime, effects/terrain and particles; `ChemicalCloudVisualMixin`
owns its presentation. The adapter preserves the actual selected native subclass
and payload so those owners remain authoritative. Their default and canonical
rows were pending in the parent catalog, not already reconciled.

Classifying the real PotionItem/SplashPotionItem/LingeringPotionItem hierarchy
keeps these chemical sources intact. It does not classify arbitrary item flags
as potions. `hasEffects()` is a required meaningful custom-dose predicate.
Water/empty potions can have environmental native use, but are not claimed as
an independent custom effect dose by this batch; that wider vanilla component
domain remains separate. No blanket inert-item conclusion follows.

## Exact native consumption boundary

The cached26.3 mapped classfiles were inspected read-only with a small Python
constant-pool/method parser; no Java process was started. In this version,
PotionItem has no finishUsingItem override. `Consumable.onConsume` awards its
single criterion/stat, invokes every native ConsumableListener component, then
consumes one physical item. PotionContents implements ConsumableListener with:

`onConsume(Level, LivingEntity, ItemStack, Consumable): void`.

Its native body reads POTION_DURATION_SCALE from the supplied stack and invokes
its own applyToLivingEntity. `LbcTmtManaPotions.consumed` calls that exact native
listener only inside a selected active private DRINK scope at the real outer
consumption callback. It does not call another Consumable/finishUsingItem,
award another consume criterion/stat, debit another item, add a bottle, enumerate
effects or implement a resource fanout. Ordinary native potion consumption is
untouched, so its stock listener runs once. Actual carrier finish remains the
prerequisite: a bow/carrot does not receive an invented consume event.

## Root integration contract

Package APIs:

- `delivery(stack)` gives DRINK/SPLASH/LINGERING for a real effect-bearing native
  potion class, including selected native predicates inside an active scope.
- `family(stack)` groups delivery and sorted effect identities. Ordinary
  strength/duration/color/count remain variants. Raid Omen levels>=16 encode
  different semantic operations, so that selector is part of effect identity.
  Long multi-effect sets use SHA256 to stay within the existing128character
  ability-ID bound. The actual payload order is never changed.
- `thrownCandidate(stack)` and `use(game,player,hand)` integrate with the existing
  direct-use source coordinator. `use` invokes the selected actual native Item.use
  against the actual hand stack inside its private scope. No held-stack swap or
  generic thrown entity is created. Outer source-group offhand policy remains
  with UseProfiles; this helper adds no wand-only offhand restriction.
- `consumed(game,player,stack)` belongs inside each real private consumption
  invocation, after nativeOwners.run and before that selected scope closes.

Root must decode literal canonical row.components in UseProfiles before dose
candidacy/capture, as Catalog already does. Its source-order probe must use
`LbcTmtComponents.source(game,variant)` so typed dose components are retained.
Drink-only admission belongs in the catalog as CONSUME/USED_HAND. Throws belong
to actual native USE; classified event names alone do not execute either path.

Existing `LbcTmtComponents.PAYLOADS` already whitelists the complete required
literal payloads: `minecraft:potion_contents` and
`minecraft:potion_duration_scale`. No potion_data pseudo-component, NBT command
interpreter or new component whitelist is required. Native default scale0.25
for Mana Lingering/chemical sources remains a native default; nondefault source
differences are captured by the existing persistent patch codec.

Public mixin API:
`LbcTmtManaPotions.projectileSource(GameController,ItemStack)` reconstructs the
selected **native** source from its private native component snapshot, strips
TMT routing blobs, and retains the complete dose. It accepts the factory's source
copy during that native scope as well as the actual held source. Ordinary
potions return the original object unchanged. It never rewrites physical drops.

Verified cached26.3 hook: public
`net/minecraft/world/entity/projectile/throwableitemprojectile/ThrowableItemProjectile.setItem(Lnet/minecraft/world/item/ItemStack;)V`.
Its coordinate constructor
`(EntityType, double, double, double, Level, ItemStack)` calls setItem. Its
living-owner constructor delegates there then sets owner. AbstractThrownPotion
extends that class; both concrete Splash/Lingering living-owner and coordinate
constructors delegate through it. Root can modify setItem's ItemStack argument
at HEAD, gated to an AbstractThrownPotion and the helper's active native source.
This keeps the native projectile's real potion type/render payload and prevents
borrowed outer TMT metadata/sibling traits from becoming its saved item.

## Authored qualification and limits

`TmtManaPotionChecks.run/tick/cancel` needs root operator/check-loop wiring.
The fixture resolves actual catalog payloads and asserts all35 authored mana
profiles, empty transport rejection and the four chemical subclass selectors.
It compares stock native potion finish to the real TMT golden-apple use/finish:
one physical debit, one food event, same exact native dose, ordinary next-tick
mana-owner consequence, full outer component restoration. It compares complete
Succubus regeneration/saturation outcomes against its stock source drink.

Real main/offhand native use creates one exact splash projectile per accepted
use. The actual native impact callback delivers the exact direct-hit dose.
The native lingering factory/impact produces its real owned AreaEffectCloud;
24 ordinary scheduled ticks observe the first native quarter-duration dose,
before its default20tick reapplication can add a second dose. No owner tick,
cloud age or resource timer is manually advanced. Private source type,
contents/color/flags/scale, caster and clean ItemStack codec roundtrip are checked;
the retained mixed physical carrier keeps its independent source slots.

Impact callbacks are invoked explicitly by the fixture; flight collision
geometry, chemical terrain hazards, all-carrier client input and physical
client presentation are not claimed. Shared fixture cleanup plus scoped native
stats and own witness scores/actors restores the touched scene. No JVM compile
or actual fixture run occurred, so root must preserve the authored/unqualified
boundary until its authorized verification stage.
