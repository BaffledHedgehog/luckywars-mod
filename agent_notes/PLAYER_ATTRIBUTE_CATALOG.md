# Minecraft 26.3 player attribute catalog

Source checked: local Minecraft 26.3 named/deobfuscated jar at
`C:/Users/yarik_temp/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.3/minecraft-merged-deobf-26.3.jar`, plus the project verification source snapshot.

`Player.createAttributes()` calls `LivingEntity.createLivingAttributes()` and then adds player attributes. The 36 vanilla keys below are therefore supported on a player and should each have their own `AttributeHud.icon(String id)` case (bare path, without `minecraft:`):

```text
air_drag_modifier
armor
armor_toughness
attack_damage
attack_knockback
attack_speed
below_name_distance
block_break_speed
block_interaction_range
bounciness
burning_time
camera_distance
entity_interaction_range
explosion_knockback_resistance
fall_damage_multiplier
friction_modifier
gravity
jump_strength
knockback_resistance
luck
max_absorption
max_health
mining_efficiency
movement_efficiency
movement_speed
name_tag_distance
oxygen_bonus
safe_fall_distance
scale
sneaking_speed
step_height
submerged_mining_speed
sweeping_damage_ratio
water_movement_efficiency
waypoint_receive_range
waypoint_transmit_range
```

The vanilla attribute registry also contains these four attributes, but they are absent from both `LivingEntity.createLivingAttributes()` and `Player.createAttributes()`. They are not player-supported defaults and should not be treated as supported player attribute HUD cases:

```text
flying_speed
follow_range
spawn_reinforcements
tempt_range
```

In particular, `camera_distance`, `waypoint_transmit_range`, and `waypoint_receive_range` are player-supported. `tempt_range` is registered but not included in living/player defaults. The `Player` source also sets both waypoint ranges to `6.0E7`.

Evidence paths and locations:

- `build/verification/mcsource/net/minecraft/world/entity/player/Player.java:207-220` — player builder, inherited living builder, player-specific additions, and waypoint defaults.
- `LivingEntity.createLivingAttributes()` in the local jar above — its bytecode adds 26 attributes, including armor, armor toughness, camera distance, and waypoint transmit range.
- `Attributes.bootstrap(Registry)` in the same jar — vanilla registry keys and exact paths, including `tempt_range` and `spawn_reinforcements`.
- `src/main/java/me/skywars2/game/AttributeHud.java` — HUD iterates `BuiltInRegistries.ATTRIBUTE`; icons are looked up from attribute path.

This is the vanilla 26.3 catalog. A third-party mod can register additional attributes at runtime; no such registration was found in this project's `src/main/java` search.
