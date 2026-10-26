# Signed status-effect levels - core only, gameplay decisions pending

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
