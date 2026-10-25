# Selective native attributes and extended armor

The user replaces the previous global RangedAttribute bypass with an explicit
policy for the forty Minecraft26.3 native attributes. AttributePolicy identifies
attributes by their registered `minecraft` identity, not description text.
Unregistered, modded and future unknown attributes retain their native bounds.

Upper bound removed, native minimum preserved: movement_speed, flying_speed,
jump_strength, step_height, sneaking_speed, friction_modifier, air_drag_modifier,
bounciness, max_health, max_absorption, burning_time, name_tag_distance,
waypoint_transmit_range, waypoint_receive_range.

Both endpoints removed: gravity, scale, movement_efficiency, armor,
armor_toughness, attack_damage, attack_speed, knockback_resistance,
sweeping_damage_ratio, fall_damage_multiplier, safe_fall_distance, oxygen_bonus,
block_break_speed, mining_efficiency, submerged_mining_speed, luck, camera_distance.
Native Player attack speed uses floating division `1 / speed * 20`: zero gives
infinite delay and negative speed gives negative delay; no integer division
exception requires a minimum-zero exception.

Native bounds preserved: water_movement_efficiency, spawn_reinforcements,
block_interaction_range, entity_interaction_range, follow_range, tempt_range,
below_name_distance. Attack knockback uses0..10. Explosion knockback resistance
has no lower bound and retains upper1. Removed endpoints expose actual infinite
bounds rather than invented finite limits. Finite sanitize obeys the selected
endpoints. NaN and either infinity retain vanilla sanitize behavior using the
original private native fields: NaN/-infinity select original minimum and
+infinity selects original maximum, even for otherwise unbounded attributes.

## Armor behavior authorized by the user

Stable native armor calculations at armor<=40 retain the original CombatRules
implementation, including equipment enchantment effects. Above40, calculate
the native outcome at armor40, then multiply remaining damage by40/armor.
Where armor40 yields80% protection, armor80 yields90% and armor160 yields95%.
Damage pressure/toughness and enchantment effects still affect that baseline.

ArmorCalculation handles the `2 + toughness/4 == 0` singularity at toughness-8
without division-generated NaN. Positive damage selects the minimum armor
contribution at this exact singularity; zero damage stays zero. Values on both
sides retain their authored native floating formula. Negative armor retains
native amplification where the unarmed damage source permits it. Defense never
introduces negative damage from positive incoming damage. Finite negative
incoming damage keeps its sign; this is separate from the user's attack/fall
healing change and introduces no global negative-hurt interception.

Nonfinite direct CombatRules inputs/results receive a finite representable
fallback; defensive multiplication saturates at the float representable limit
instead of producing NaN/infinite HP changes. This bounds calculation output,
not attribute values. Existing native int/float consumers, such as getArmorValue,
retain their Java numeric domains. The source2-billion max HP is within the
native float domain and remains accepted.

## Evidence and integration

Read cached Minecraft26.3 sources: RangedAttribute, Attributes, CombatRules,
LivingEntity.getDamageAfterArmorAbsorb/getArmorValue, Player attack-speed delay,
AttributeInstance modifier APIs. No source server or author resource pack edit.
Server-side attribute acceptance does not replace the vanilla client's own
attribute registry/range calculations; client movement, camera and presentation
behavior above native client limits is not qualified by these server checks.

Root integration must register ArmorCalculationMixin and GameChecks commands
for AttributePolicyChecks.run and ArmorCalculationChecks.run. The existing
AttributeLimitsMixin registration remains. Attribute checks cover all40 actual
registry entries, nonfinite native fallbacks, an unknown description-spoofed
attribute, native2b HP, preserved speed minimum, negative gravity/scale and
native player zero/negative attack delay with full modifier restoration.
Armor checks cover native CombatRules and LivingEntity's armor pipeline,
40/80/160 and2b armor, negative armor, both sides of toughness-8, zero damage,
nonfinite inputs and finite actual hurt HP. Tests are prepared; no build,
server run, actual client pixels or runtime PASS is claimed by this agent.

Final root qualification: build `b4cdfa8d`,148 recorded results across two actual
mode1 matches and actual stop cleanup. See [combined review](GAMEPLAY_REVIEW_2026_10_25.md)
for the tested artifact, deployment state and verification boundaries.
