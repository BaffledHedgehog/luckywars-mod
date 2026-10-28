> (user, 28 October 2026) `scale` now keeps BOTH vanilla bounds: **0.0625..16**. This supersedes the earlier request to unbind scale; other attribute policies remain unchanged.

# Attribute decisions, 25 October 2026

These user decisions supersede the former blanket attribute limit removal.
The earlier proposal table remains an audit record, not the selected policy.

`П`: native minimum, no maximum. `Н`: neither bound. `В`: native bounds.

## User answers (verbatim)

> movement_speed, flying_speed, jump_strength, step_height, sneaking_speed, friction_modifier, air_drag_modifier, bounciness - П
> gravity, scale, movement_efficiency - Н
> water_movement_efficiency - В

> max_health, max_absorption - П
> armor - Н (кстати, можешь ли поправить формулу чтоб рост защиты был и выше 40 брони? там же формула даунская ванильная не дает защите выше 40 что то добавлять)
> armor_toughness - Н (добавь обработчик деления на 0)
> attack_damage - Н и сделай чтоб отрицательный урон реально лечил, во умора будет
> attack_speed - Н (если не будет крашить мир, иначе П)
> attack_knockback - пусть будет в пределах 0..10
> knockback_resistance - Н
> explosion_knockback_resistance  - оставь верхнюю границу в 1, убери нижнюю чтоб можно было отрицательный эффект копить
> sweeping_damage_ratio - Н
> fall_damage_multiplier - Н и пусть отрицательный урон лечит
> safe_fall_distance - Н
> burning_time - П
> oxygen_bonus - Н и пусть отрицательный аттрибут быстрее воздух тратит
> spawn_reinforcements - В

> block_break_speed - Н и пусть отрицательные значения начнут клонировать блок который пытается сломать игрок (в сторону игрока) с частотой зависящей от величины отрицательного аттрибута, во ржака
> mining_efficiency - Н
> submerged_mining_speed - Н (та же схема с отрицательным ломанием)
> block_interaction_range, entity_interaction_range, follow_range, tempt_range - В
> luck - Н
> name_tag_distance - П
> camera_distance - Н
> waypoint_transmit_range, waypoint_receive_range   - П

`below_name_distance` was omitted from the answer and retains the proposed
native bounds, as stated to the user. Native floating attack-delay arithmetic
supports negative speed; the zero-underflow case is stabilized separately.

## Follow-up choices

For armor above40, compute the ordinary outcome at armor40 and multiply the
remaining damage by `40/armor`. Examples: if the baseline blocks80%, armor80
blocks90% and armor160 blocks95%.

User: «40/armor для оставшегося урона — как в примере».

Negative mining copies into one adjacent free cell toward the player, keeps
the original and emits no item drop; its cadence follows effective negative
native mining progress. Containers and other block entities copy their full
contents and components independently.

User: «Да, копировать блок и содержимое».

Implementation and checks: [ATTRIBUTE_POLICY_ARMOR.md](ATTRIBUTE_POLICY_ARMOR.md),
[NEGATIVE_MINING.md](NEGATIVE_MINING.md). These server mechanics do not change
the unmodified client's own attribute range calculations.
