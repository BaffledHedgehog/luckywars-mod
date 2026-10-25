# Attribute limit review - user selection received

The user requested questions for every attribute before changing the global
limit bypass. Forty native attributes were enumerated from the exact cached
Minecraft26.3 `Attributes.java`, not a version-independent remembered list.
The three asynchronous question groups use the numbered order below.

V means native lower/upper bounds; P means retain the native minimum and remove
the maximum; N means remove both. The user may instead choose numeric bounds.
The proposal column records the original suggestions, not the later approvals.
The selected policy and extended armor curve are recorded in the linked final
decision notes.

**All decisions received and implemented in the selective policy.** The table
below preserves the original questions and proposals; the actual decisions are
recorded in [ATTRIBUTE_POLICY_ARMOR.md](ATTRIBUTE_POLICY_ARMOR.md),
[ATTRIBUTE_DECISIONS_2026_10_25.md](ATTRIBUTE_DECISIONS_2026_10_25.md) and
[NEGATIVE_MINING.md](NEGATIVE_MINING.md). Unknown/modded attributes now retain
their own native bounds. Runtime qualification is recorded in the combined
gameplay review note after testing.

| Question | Native identifier | Native minimum | Native maximum | Proposal |
| --- | --- | --- | --- | --- |
| 1 | `movement_speed` | 0.0 | 1024.0 | P |
| 2 | `flying_speed` | 0.0 | 1024.0 | P |
| 3 | `jump_strength` | 0.0 | 32.0 | P |
| 4 | `gravity` | -1.0 | 1.0 | V |
| 5 | `scale` | 0.0625 | 16.0 | V |
| 6 | `step_height` | 0.0 | 10.0 | V |
| 7 | `sneaking_speed` | 0.0 | 1.0 | P |
| 8 | `movement_efficiency` | 0.0 | 1.0 | V |
| 9 | `water_movement_efficiency` | 0.0 | 1.0 | V |
| 10 | `friction_modifier` | 0.0 | 2048.0 | V |
| 11 | `air_drag_modifier` | 0.0 | 2048.0 | V |
| 12 | `bounciness` | 0.0 | 1.0 | V |
| 13 | `max_health` | 1.0 | 1024.0 | P |
| 14 | `max_absorption` | 0.0 | 2048.0 | P |
| 15 | `armor` | 0.0 | 30.0 | P / custom ceiling |
| 16 | `armor_toughness` | 0.0 | 20.0 | P |
| 17 | `attack_damage` | 0.0 | 2048.0 | P |
| 18 | `attack_speed` | 0.0 | 1024.0 | P |
| 19 | `attack_knockback` | 0.0 | 5.0 | P |
| 20 | `knockback_resistance` | -2.0 | 1.0 | V |
| 21 | `explosion_knockback_resistance` | 0.0 | 1.0 | V |
| 22 | `sweeping_damage_ratio` | 0.0 | 1.0 | P |
| 23 | `fall_damage_multiplier` | 0.0 | 100.0 | P |
| 24 | `safe_fall_distance` | -1024.0 | 1024.0 | N |
| 25 | `burning_time` | 0.0 | 1024.0 | P |
| 26 | `oxygen_bonus` | 0.0 | 1024.0 | P |
| 27 | `spawn_reinforcements` | 0.0 | 1.0 | V |
| 28 | `block_break_speed` | 0.0 | 1024.0 | P |
| 29 | `mining_efficiency` | 0.0 | 1024.0 | P |
| 30 | `submerged_mining_speed` | 0.0 | 20.0 | P |
| 31 | `block_interaction_range` | 0.0 | 64.0 | V |
| 32 | `entity_interaction_range` | 0.0 | 64.0 | V |
| 33 | `follow_range` | 0.0 | 2048.0 | V |
| 34 | `tempt_range` | 0.0 | 2048.0 | V |
| 35 | `luck` | -1024.0 | 1024.0 | V |
| 36 | `name_tag_distance` | 0.0 | 512.0 | V |
| 37 | `below_name_distance` | 0.0 | 512.0 | V |
| 38 | `camera_distance` | 0.0 | 32.0 | V |
| 39 | `waypoint_transmit_range` | 0.0 | 6.0E7 | V |
| 40 | `waypoint_receive_range` | 0.0 | 6.0E7 | V |

## Confirmed mechanisms and caveats

- Player attack recovery divides20 by attack_speed. Negative speeds do not
  describe a sensible recovery interval. Movement and sneaking use signed
  multipliers, so negative values can reverse their effect.
- Armor toughness enters `2+toughness/4` in the divisor; toughness=-8 is a division
  boundary. Armor's ordinary formula assumes meaningful finite positive bounds.
- Knockback commonly multiplies by `1-resistance`; some paths clamp that result
  while others do not. Explosion resistance above1 can reverse its impulse.
- Bounciness multiplies reflected velocity; values above1 inject energy at each
  bounce. Water efficiency extrapolates interpolation coefficients beyond1.
- Friction and air-drag attributes feed a separate native0..1 clamp; removing
  their declared bounds does not remove that internal clamp.
- Scale changes entity dimensions and broad-phase collision cost. Very large
  interaction/follow/tempt ranges enlarge searches; neither unlimited attributes
  nor ray range implies loading absent chunks.
- Server-only attribute changes do not replace the vanilla client's registered
  bounds. Sync packets convey attribute values/modifiers, not new registry
  definitions. Do not claim client agreement from reconstruction under the
  server's own mixins.

Primary implementation evidence: the exact26.3 cached source JAR,
`net/minecraft/world/entity/ai/attributes/{Attributes,RangedAttribute}.java`,
`LivingEntity.java`, `player/Player.java`, `Entity.java`,
`world/damagesource/CombatRules.java`, `world/level/ServerExplosion.java`,
`client/player/LocalPlayer.java`. The unusually high HP requested earlier for
Psychowand's vex remains a reason to consider removing max_health's upper bound
while retaining a positive minimum.
