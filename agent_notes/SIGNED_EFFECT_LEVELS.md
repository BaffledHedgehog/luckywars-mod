# Signed status-effect levels - qualified and installed

## User decisions received on 26 October 2026

The three groups below are approved; the historical pending paragraphs after
this section describe the earlier core-only candidate, not current permission.
Amplifier remains zero-based; compute L=(long)amplifier+1 before arithmetic.
No client movement correction is authorized for levitation/slow falling.

1-6,11-12: native signed attribute coefficients; haste/conduit select only
present effects; fatigue uses finite power calculation. Jump safe falling
covers actual discrete ballistic jump height plus an extra block below takeoff,
on top of the normal safe-drop allowance. Player levitation simulation clamps
amplifier0..255, mobs retain signed formula. Negative slow falling remains inert
for players. Dolphin levels use synchronized swimming attributes, without
corrective movement packets.

13-19: instant health/harm reverse at negative levels, preserve undead inversion,
and saturate finite numbers without shifts. Regeneration/poison/wither reverse
at negative levels; native low-level cadence stays, amounts grow linearly after
the first every-tick tier. Poison cannot reduce health below1. Resistance aboveV
turns the negative incoming damage calculation into healing; negative resistance
increases damage. Health boost stays a signed attribute with max-health minimum1.
Item grants (including future ports) cap regeneration/wither amplifier5, poison4,
resistance4, blindness/darkness2. Explicit commands and persistent authoritative
effects keep full levels. ItemEffectLevels is the grant policy, not a global
constructor limit. Review new item paths against that factory and potion hooks.

20: negative absorption conceals an independently healable segment of maximum
health, additive across independently expiring signed levels; positive levels
counterweight negatives. Hidden hearts never absorb damage. Restore their actual
healed amount on expiration. See SHADOW_ABSORPTION.md.
21-22: negative hunger reverses exhaustion then restores saturation at the same
slow four-exhaustion-per-saturation rate. Signed saturation calculations stop
at meaningful bar capacity; food/saturation retain native0..20 bounds.

23-26: extend legacy fire/water/conduit II/III; aboveIII block areas grow with
sqrt and radius caps15. Fire IV+ adds WILD damage with level-dependent radius.
Negative fire resistance amplifies fire damage. Water breathing II+ reverses
air use: outside drains (III=one/tick), underwater refills; old debuffs only
after bubbles empty. Positive nautilus refills underwater. Negative aquatic
effects accelerate air use; signed conduit participates in digging speed.
See SIGNED_ENVIRONMENTAL_EFFECTS.md for bounded work and exact implementation.

27-32: negative invisibility glows; II hides particles, III+ fully hides player
rendering, IV+ same asIII. Negative blindness magnitude reduces invisibility
tiers per viewer, without glowing as a reveal substitute. Positive blindness,
darkness, nausea and signed night vision use stronger shader variants; negative
darkness brightens/contrasts, negative night vision inverts. Visual calculations
may saturate at screen limits, stored effect levels never do. User prefers
individual glow bloom and proposed a client-only item_display carrier; inspect
existing shader markers before adding it. Author RP stays read-only; deliver
changed assets through template. See SIGNED_EFFECT_VISUALS.md.

33-36: nonpositive omens do not trigger their native events; positive stored
levels remain full, raid mechanics capV. Signed village hero discounts/surcharges
stay within item-stack price bounds. Preserve custom Raid Omen transport markers.
37-40: signed wind force/radius pushes or pulls; weaving adds/removes webs with
sqrt radius capped50; oozing uses two slimes or negative magma cubes with size
depending on magnitude; infestation scales chance, whole100% overflow adds
guaranteed mobs, negatives create endermites. Keep native ordinary fractional
spawn batches and bound work per tick, not amplifier storage. Preserve authors,
Greed, Time Stop, roles and god_hedgehog silverfish blockade.

Completed: build1a8a3825 passed68 results in two actual matches (123.13s),
plus83 standalone RTX4080SUPER GPU checks. Exact433 sources/1203 classes/3139
resources match the tested artifact. Installer19140 installed it after normal
shutdown, without restart, retaining b2c91dcf rollback. Evidence and immutable
qualification: build/verification/signed-effects-gameplay/. Author RP unchanged;
apply resourcepack_template/SIGNED_EFFECTS_README.md assets manually. Current
limits: vanilla client amplifier simulation, neutral negative player slow falling,
eight nearest analytic glow halos and existing player-only III+ proxy scope.

## Historical core-only checkpoint

The user requests negative and full signed-int status-effect amplifiers,
including /effect, and explicitly requests agreement for each effect before
formula/gameplay changes. Forty questions were sent as three grouped questions;
no answers have arrived yet. Do not infer approval from elapsed time.

Current implementation changes only the base MobEffectInstance constructor,
its recursive persistent Details codec (UNSIGNED_BYTE -> Codec.INT), and both
finite/infinite /effect amplifier argument branches. Amplifier remains the
native zero-based value:0 -> level1; -1 -> level0; -2 -> level-1. Full native
range is Integer.MIN_VALUE..Integer.MAX_VALUE. Duration limits/infinite syntax,
stacking/hidden-effect numerical precedence and effect-specific formulas are
unchanged. The constructor injection names the complete primary descriptor;
a generic <init> WrapOperation selector matched no constructors at runtime and
was replaced before qualification.

The native instance stream codec and effect-update packets already use signed
VarInt, so their protocol needs no redesign. Vanilla clients still clamp the
received instance amplifier0..255 locally. Server authoritative storage and
server formulas can be extended, but vanilla HUD and local-player movement/
visual formulas cannot be assumed to obey signed/large levels. The user was
told this; levitation/slow-falling player compensation and visual policies are
among the pending choices.

Effect-specific formulas are NOT safe for arbitrary int levels yet: instant
health/damage use int bit shifts, regeneration/poison/wither use masked shifts,
attribute templates use int(amplifier+1), resistance multiplies ints, hunger/
saturation/absorption and levitation have int additions/products, and omen
conversion has integer duration multiplication. Repair these according to the
user's answers before deploying a release. Do not install the core-only build
or let an unrelated install silently publish these unfinished semantics.
Main remains qualified directional-knockback build875fed3a.

Decision numbering (every entry pending):
1 speed;2 slowness;3 haste;4 mining_fatigue;5 strength;6 weakness;7 jump_boost;
8 levitation;9 slow_falling;10 dolphins_grace;11 luck;12 unluck;
13 instant_health;14 instant_damage;15 regeneration;16 poison;17 wither;
18 resistance;19 health_boost;20 absorption;21 hunger;22 saturation;
23 fire_resistance;24 water_breathing;25 breath_of_the_nautilus;26 conduit_power;
27 invisibility;28 glowing;29 blindness;30 darkness;31 night_vision;32 nausea;
33 bad_omen;34 raid_omen;35 trial_omen;36 hero_of_the_village;
37 wind_charged;38 weaving;39 oozing;40 infested.

Cached official 26.3 effect classes, command and packet sources plus per-effect
consumer references are in build/verification/effect-level-audit/. Pending IDs,
protected-author/live-grimoire/main baselines and core evidence belong in
build/verification/signed-effects-final/. Full gameplay release qualification
and installation must wait for user-selected formulas and their tests.

Core evidence: buildd7687511 passed34 recorded suite results in two fresh mode1
matches (118.05s). Exact407 sources /1142 classes /3139 resources match the tested
JAR. Real finite/infinite /effect commands cover all ten amplifier boundary
values; all forty effects pass storage/codecs. Native entity save/load, clear,
negative hidden-effect restoration and selected old effect-marker/combat suites
pass. No gameplay formula approval or extended-formula qualification is implied.

The initial constructor selector failed safely during isolated bootstrap and
was fixed to the exact primary constructor descriptor. Nested performPrefixedCommand
queued commands until after fixture disposal; actual commands now execute through
the native Brigadier dispatcher synchronously. Babylon passes independently and
before the long naturally ticking Gaycaster fixture; the earlier reverse ordering
failed its vanilla-lightning HP assertion. Its assertion now records actual HP,
effects and invulnerability for diagnosis; no production lightning behavior or
assertion was changed. This cross-fixture order dependence is not claimed fixed.

No installer was launched and no release archive was added. Main stays875fed3a;
existing ready archives875fed3a/6e648580/1265875f remain. Protected author changes,
all four intentional template deletions and live grimoire overrides are preserved.
Current build/libs contains the core-only candidate, not a live-ready release.

Operator-settings releaseb2c91dcf deliberately unregisters the three pending
signed-effect mixins, while retaining their source preparation. Main is safely
updated for operator controls with native0..255 effect behavior. Future completed
signed-effect integration must register the policies and update the temporary
native-limit release assertion in OperatorSettingsChecks before qualification.
