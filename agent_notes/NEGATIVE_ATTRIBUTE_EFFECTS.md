# Explicit negative attribute mechanics

25 October2026. User choices: negative attack damage heals the actual melee
target; negative fall-damage multiplier turns an otherwise damaging fall into
healing; negative oxygen bonus spends air faster. Negative attack speed remains
allowed while its native floating-point math stays defined. This bounded work
adds mechanics and native checks; attribute range policy is owned separately.

Exact API evidence is the local26.3 source JAR under
`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/`.
No web assumptions, source-server/resource-pack edits, builds or server actions
are part of this subtask. Runtime results belong to the parent verification.

## Actual melee

Native `Player.attack` checks `cannotAttack`, calculates cooldown-scaled base
damage and enchanted bonus, calls `onAttack`, then gates damage on a positive
base/bonus. Merely changing `hurtServer` cannot make a negative ordinary player
attack heal because that gate prevents the call. `NegativePlayerAttackMixin`
injects at the native `onAttack` invocation, after Fabric attack-event admission
and native target eligibility. It receives the actual native base/bonus/strength
locals, includes the item attack bonus and ordinary critical multiplier, and
handles only a negative resulting amount from negative attack damage. It calls
native `onAttack` exactly once and completes native piercing cleanup, replacing
the hurt branch with direct health restoration capped at actual missing HP.

Native `Mob.doHurtTarget` already calculates enchantment/item-adjusted damage.
`NegativeMobAttackMixin` uses that final amount immediately before the native
`Entity.hurtServer` call. A negative attack heals directly, returns whether HP
actually increased, and performs native piercing cleanup. It creates no fake
damage, armor wear, knockback or hostile kill-credit event. Native undead heal
normally here; this does not invoke Instant Health/Damage inversion.

Support checks retain active arena/living roles, native player/mob melee reach,
minimum attack charge and line of sight, world-border admission, observer,
Greed, frost, Time Stop and full-action stun guards. Allied players can receive
this healing; it never relies on bypassing native friendly-fire damage rules.
Ordinary positive damage and its admission/hooks continue through vanilla.

Additional source audit found that piercing/kinetic stabs and several custom mob
overrides bypass `Player.attack`/`Mob.doHurtTarget`. The narrow
`NegativeDirectMeleeMixin` therefore intercepts native `hurtServer` before hurt
admission, on both LivingEntity and ServerPlayer, only when all are true:
amount<0; the attacker has negative attack damage; source entity and direct
entity are the same LivingEntity; and the native damage type is PLAYER_ATTACK,
MOB_ATTACK, MOB_ATTACK_NO_AGGRO, STING or SPEAR. It applies the same support
guards/healing and returns false to stop fake hurt and conditional post-hurt
consequences. Existing tested ordinary attack hooks remain in place.

The exact source identities are verified in26.3: `PiercingWeapon.attack` reads
effective attack damage and calls `stabAttack`; `KineticWeapon` combines base
attack damage with its floored speed bonus and calls the same method. Both obtain
the held stack’s native damage source. `Item.Properties.spear` explicitly assigns
the SPEAR damage-type component; ordinary stacks fall back to player/mob attack.
Bee uses STING, golem/hoglin/slime/charge use direct mob attack, and ram uses the
no-aggro variant. Negative potion, explosion and projectile damage are excluded
by type and direct-entity identity, rather than being globally converted into
healing. Geometry and any component-level target exclusions remain native.

## Fall, oxygen and signed attack speed

Native `LivingEntity.causeFallDamage` first adjusts effective fall distance for
the current impulse context and propagates the fall through its normal parent
path, then invokes `calculateFallDamage`. That method returns the floored signed
product; the caller’s positive-only branch prevents a negative value from doing
anything. `NegativeLivingAttributesMixin` wraps that exact calculation call for
a negative multiplier. It retains fall-damage-immune types, safe distance and
positive block multiplier, heals the magnitude of the floored signed product
once, and returns zero so no hurt/cooldown/sound branch runs. Falls below safe
distance remain zero, rather than becoming damage from multiplying two negative
terms. Player flight immunity and the ordinary landing hook remain native.

For example, distance10, safe distance3 and multiplier-1 produce
`floor(-(10+1e-6-3))=-8`, hence8healing. This preserves the actual26.3 floor
convention, including its epsilon. Healing saturates at missing health without
floating/integer overflow and cannot revive dead entities.

Negative oxygen bonus uses per-underwater-tick loss `1+abs(bonus)` with a
Bernoulli fractional remainder; integer subtraction saturates safely. Zero and
positive bonuses retain vanilla. Native26.3 drowning tests `air<=-20`, not exact
equality, so large depletion cannot skip drowning. The native tick resets air
to zero and applies its ordinary single2damage drowning consequence.

Native attack delay is `float(20/attack_speed)`. Ordinary negative values yield
a negative delay and a cooldown strength clamped to zero; zero yields infinity
and also zero strength. Neither crashes. Extreme finite magnitudes can underflow
the returned float delay to signed zero, making a native0/0 strength NaN.
The player mixin replaces only that zero return with signed `Float.MIN_NORMAL`;
the negative attribute itself remains unchanged. A NaN delay fallback is positive
infinity. Thus the user’s negative-speed policy can remain enabled safely.

## Integration and native checks

Register these four new mixins:

- `NegativePlayerAttackMixin`
- `NegativeMobAttackMixin`
- `NegativeLivingAttributesMixin`
- `NegativeDirectMeleeMixin`

Check entry point: `NegativeAttributeChecks.run(game)`.

The fixture uses real `Player.attack`, actual `Zombie.doHurtTarget`, native
`causeFallDamage`, and native underwater `baseTick` in a resident water tank.
It additionally calls real Player/inherited-LivingEntity stabs, a native wooden
spear stab at its weapon range, IronGolem’s custom attack and Bee’s native sting.
ServerPlayer target healing is asserted once despite both native hurt overrides;
negative arrow and indirect-magic calls are asserted to produce no healing.
It checks normal/undead/allied healing, actual cooldown consumption, cap, roles,
Greed, range and Time Stop; native negative/zero/extreme attack-speed math and
weak-attack healing; one fall consequence and safe short falls; and zero,
integer-negative, fractional-negative and extreme-negative oxygen, including
real native drowning damage. It does not merely call the new helpers.

The fixture requires initially empty player attribution, saves relevant native
attributes with permanent/transient modifiers, profile fields, native combat
credit/timers, fall stat and player state, then removes only its fixture actors,
attribution and resident terrain changes before restoration. Explicit method
boundaries do not claim elapsed gameplay time, client pixels or peak load.

Final root qualification: build `b4cdfa8d`,148 recorded results across two actual
mode1 matches and actual stop cleanup. See [combined review](GAMEPLAY_REVIEW_2026_10_25.md)
for the tested artifact, deployment state and verification boundaries.
