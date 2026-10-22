# Normal Fat Skeleton port - qualified

Status: qualified and installed with [the combined batch](UI_WEAPON_BATCH_PORT.md).
The OLD skeleton remains a distinct acquisition/preset.

## Source and scope

Read-only legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

- `function/drops/specialdrops.mcfunction:56`: normal actor; line57 is the distinct old actor.
- `loot_table/traps.json:314-357`: normal token, weight20/quality-10, count1.
- `loot_table/fatskeleton.json`: three native reward pools.
- `loot_table/random_sauvojen_wand.json`: nine equal-weight real wand entries.
- [Next source audit](NEXT_DROPS_AFTER_ACID_ANT_SOURCE.md) and [RP contract](RESOURCE_PACK.md).

No raw server, user RP, template, RerollCrafting, whole reward pool or portal work
is included. No new mob AI, actor lifetime, reward placeholder or owner-death
cancellation is introduced.

## Native acquisition and actor

`LbcCreatureDropItem.Kind.FAT_SKELETON` registers
`skywars2:normal_fat_skeleton_drop`, flag `{skeleton:1,drop:1}`. Its Polymer
projection is a repeating command block with existing `lbc:drop_boss` item model,
`lbc:tier_ff` tooltip, yellow/nonitalic `trap_fat` name and one white/nonitalic
`trap_desc` lore line. Public leaf: `lbc:ported/normal_fat_skeleton_drop`.

`LbcCreatureDrops.Kind.FAT_SKELETON` uses native skeleton creation/load at the
actual token fractional coordinates without finalizeSpawn. The source preset
is copied literally except the internal death-table resource key:

- Persistent, visible nonitalic translated `fat_skeleton`, tag `fat_skeleton`;
  ordinary native20HP, native skeleton melee/AI and conversion remain.
- Bone mainhand: Knockback1 and nonitalic `omega_bone`; damage+7, speed-3.5,
  knockback-resistance+1 (`minecraft:dhesfubugbrgbffgg`), movement-.7
  ADD_MULTIPLIED_BASE (`minecraft:ughvshfg7udfhgud`). The first two IDs remain
  `minecraft:base_attack_damage` and `minecraft:base_attack_speed`.
- Bone meal64 feet, bone32 legs, iron chestplate, skeleton skull; offhand empty.
  ALL SIX equipment drop chances are explicitly0, including offhand.
- Actor base movement_speed.6, armor20, attack_knockback100,
  armor_toughness5 and attack_damage7. The held bone adds its independent damage
  modifier; do not mistake actor base7 for its complete native attack value.

Existing resident-chunk admission, token epoch/shared debit, native role/ally
policy, actual thrower fallback cache, native conversion tags and quiet stop
tracking are reused. This is the established ordinary owned-mob policy, including
native author retaliation eligibility, rather than a new WILD projectile policy.

## Complete real native rewards

The actor uses `lbc:fat_skeleton_rewards`, adapted to the26.3 singular `modifier`
and `condition` fields and `type` discriminators. Source counts, modifier order,
conditions, component values and pools remain:

1. One guaranteed nested `lbc:fat_skeleton_wands` roll (source random_chance1).
2. Five equal choices of bone32, bone_meal64 or bone_block16. These are ordinary
   native stacks and native stack merging/splitting is retained.
3. One native Omega bone relic; this is distinct from the actor's held bone.

`lbc:fat_skeleton_wands` retains source entry order and weight1 for each existing
working native leaf:

| Source table | Native leaf |
| --- | --- |
| sauvojen_tuntija_swargia | lbc:ported/ruby_wand |
| sauvojen_tuntija_terra | lbc:ported/terra_wand |
| sauvojen_tuntija_electro | lbc:ported/electro_wand |
| sauvojen_tuntija_air | lbc:ported/air_wand |
| stacid_wand | lbc:ported/acid_wand |
| sauvojen_tuntija_teleport | lbc:ported/teleport_wand |
| sauvojen_tuntija_ananta | lbc:ported/stone_wand |
| sauvojen_tuntija_fotja | lbc:ported/fire_wand |
| sauvojen_tuntija_blind | lbc:ported/blind_wand |

The relic is ordinary `minecraft:bone`, count1, Knockback1, `lbc:tier_c`, gold/
nonitalic `omega_bone`, one yellow/nonitalic `fat_skeleton_relic` lore line. No
custom model, ability flag, extra enchantment or synthesized tooltip is added.
All modifiers are mainhand:

| Attribute | Amount | Operation | ID |
| --- | --- | --- | --- |
| attack_damage | 7 | add_value | minecraft:base_attack_damage |
| attack_speed | -3 | add_value | minecraft:base_attack_speed |
| knockback_resistance | 1 | add_value | minecraft:minughv8irghgegvrvdeg |
| movement_speed | -.7 | add_multiplied_total | same source ID |
| scale | .5 | add_multiplied_total | same source ID |
| step_height | 1 | add_value | same source ID |
| fall_damage_multiplier | -.1 | add_multiplied_total | same source ID |
| safe_fall_distance | 3 | add_value | same source ID |
| jump_strength | .5 | add_multiplied_total | same source ID |
| block_interaction_range | .5 | add_multiplied_total | same source ID |
| entity_interaction_range | .5 | add_multiplied_total | same source ID |

Repeated IDs across different attribute types are source-authored and preserved.
The source's unqualified identifier is read by the native codec in the minecraft
namespace; the table itself retains its original spelling.

## Native conversion qualification

26.3 `ConversionType.convertCommon` copies entity tags, name/customdata, selected
native state and equipment/drop chances; it does not copy custom DeathLootTable.
A native skeleton-to-Stray conversion therefore retains causal identity/epoch,
source-name/gear and zero equipment-drop chances but uses native Stray death loot.
The port does not manually transfer the normal reward override or reset converted
attributes. Qualification should check this native distinction rather than assume
that all source actor NBT survives conversion unchanged.

## RP and verification status

Read the actual user's26.3(1) pack in place, read-only. Existing RU/RPR/EN keys
`trap_fat`, `trap_desc`, `fat_skeleton`, `omega_bone`, `fat_skeleton_relic` are reused
with their exact source components/styles. EN values include `Very fat skeleton`,
`Omega bone`, `Fat Skeleton Relic` and `Trap: Very Fat Skeleton`. Existing
`assets/lbc/items/drop_boss.json` and tier_c/tier_ff tooltip sprites are present.
No new translations, textures, models or template files are needed.

Prepared implementation/resource review only; root qualification is still required
for actual native death drops, all-nine wand use, relic attribute equip/unequip,
melee/knockback attribution and newer-attacker priority, role/ally protection,
conversion, actual ticks/freeze/unload/epoch and quiet global stop. No build,
server start, install or commit was performed by this subtask. Client rendering,
disk restart and natural AI/peak-capacity remain unqualified unless separately
recorded by the root combined run.

## Current qualification

Qualified and installed with [the combined UI/weapon batch](UI_WEAPON_BATCH_PORT.md):
artifact68387e1d,142 native suite results across two actual matches. The initial
delegated preparation status above is historical; see the combined contract for
artifact identity, native fixture scope, installation and limits.
