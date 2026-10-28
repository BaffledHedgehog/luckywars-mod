# Attribute food consumer slice - 29 October 2026

Status: native implementation staged; no build, server qualification, packaging or public readiness claim. The full original Chaos pool remains gated. This assignment owns only `LbcAttributeFood.java` and this note. Protected author paths, resources, RP, main installation and build configuration were not edited.

## Source closure and bridge

Read-only original root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

The actual top-level `advancement/*.json` consume criteria name eight fixed foods and three random foods. Each tests a custom-data integer equal to 1; each explicitly excludes `minecraft:nexus` and `minecraft:imprinted`. None specifies a jammer, item identity, player mode, role, global food counter or cooldown. Flags below deliberately preserve spelling: there is **no underscore before `attribute`**.

Root integration: construct `new LbcAttributeFood(game)` and call public `void consumed(ServerPlayer, ItemStack)` from the existing native consumption callback. It does not debit a stack, grant nutrition, apply the carrier's vanilla effects, create an edible component, poll inventories, or require lifecycle/tick callbacks. Independent matching flags execute independently, as independent original advancement criteria do. Native repeated consumption replaces the hidden advancement/revoke loop.

Current controller eligibility follows the existing Aimbob consumed-item bridge: active non-ending match, live present player in actual game level, gameplay permission, ordinary dimension, nonobserver valid role, and all shared Time Stop/input admission checks. These role/Time Stop restrictions are current project policy; the eleven original criteria themselves only have the two dimension exclusions. No MAG/TECH check is added to base arithmetic. Nausea/blindness use the existing native shared effect admission and `ItemEffectLevels`.

## Eight exact base updates

Let `I(x)` mean the native attribute command result `(int)x`: truncation toward zero, Java saturation outside int range, and zero for NaN. Addition below is signed scoreboard int addition, including overflow. Every original update macro runs `attribute @s <attribute> base set <value>`. There are **no attribute modifier IDs or ADD_VALUE/ADD_MULTIPLIED layers** in this closure. Existing equipment, effects, attribute modifiers and KFC rewards remain separate; native `AttributePolicy` and `AttributeLimitsMixin` own effective bounds.

| Custom-data flag | Root advancement | Native base attribute | New raw base value | Particle at wearer Y+1 |
|---|---|---|---|---|
| armorattribute | armorattr | armor | `(I(base)+2)*1` | iron_block block,100,speed0 |
| armor_toughnessattribute | armor_toughnessattr | armor_toughness | `(I(base)+1)*1` | iron_block block,100,speed0 |
| attack_damageattribute | attack_damageattr | attack_damage | `(I(base)+1)*1` | sweep_attack,100,speed0 |
| attack_speedattribute | attack_speedattr | attack_speed | `(I(base*5)+1)*.2` | crit,100,speed0 |
| knockback_resistanceattribute | knockback_resistanceattr | knockback_resistance | `(I(base*10)+1)*.1` | smoke,100,speed0 |
| luckattribute | luckattr | luck | `(I(base)+1)*1` | happy_villager,100,speed0 |
| max_healthattribute | max_healthattr | max_health | `(I(base)+2)*1` | heart,10,speed0 |
| movement_speedattribute | movement_speedattr | movement_speed | `(I(base*100000)+2000)*.00001` | end_rod,10,speed1 |

All particles retain normal delivery, offsets .5,.5,.5. Every fixed reward plays `minecraft:entity.player.levelup`, MASTER, wearer position, volume1,pitch1, original `@a` audience/range/minVolume semantics through shared `LbcOverload.sound`. Max-health base writes do not explicitly heal, clear absorption or clamp current health: the source macro does none of these.

Functions read in full: `function/other/attributes/{armor,armor_toughness,attack_damage,attack_speed,knockback_resistance,luck,max_health,movement_speed}.mcfunction`, and each corresponding `_update.mcfunction`. The root function paths are the corresponding advancement reward functions. The exact native 26.3 `AttributeCommand.getAttributeBase` implementation was read from the existing mapped sources JAR: it returns `(int)(result*scale)`, not `Math.floor`. Existing `LbcKfcFood` has a private floor-based helper and source-specific different increments/feedback; it is neither called nor changed by this slice. In particular, armor and max-health foods add2, not KFC's add1. No duplicate KFC application is introduced.

## Three exact random edible updates

| Flag | Root advancement | Base attribute | Inclusive upper bound |
|---|---|---|---|
| random_damage | eat_damage_penis | attack_damage | `I(base*3000)` |
| random_speed | eat_fast_penis | movement_speed | `I(base*4000)` |
| random_health | eat_healthy_penis | max_health | `I(base*2500)` |

Read full `other/attributes/random_damage.mcfunction`, `random_speed.mcfunction`, `random_health.mcfunction`, shared `gen_rand.mcfunction`, and the same three `_update` macros above. Valid range draws use native **world** RNG, uniform1..upper inclusive. They store the actual shared `#random_out lbc.math` scoreboard result, then set raw base to `result*.001`. No player RNG, percentage approximation, heal or extra draw is substituted. The source's scratch storages only transport the macro number; no parallel private storage/stat engine is introduced.

Source edge behavior is retained using the actual shared result score: upper1 is a syntactically valid `1..1`, but native `RandomCommand` throws `range_too_small` at execution, so execute-store records0 without drawing. Upper0/negative creates a reversed range and fails macro instantiation; the prior shared result survives. Reading an absent score through source execute-store yields0, matching a newly created actual result score. These are native failure paths, not a new minimum-bound clamp. The existing native mapped `RandomCommand` and range parser distinguish runtime equal-bound failure from reversed-range parsing failure.

All three functions then grant nausea3seconds (60ticks), blindness2seconds (40ticks), amplifier0, hidden particles/icon. The implementation uses capped native `MobEffectInstance` creation and `addEffect` with wearer source. Carrier nutrition and pre-existing vanilla consumable effects (including golden apple) remain owned by vanilla. No fixed-food levelup sound/particles are present on random variants.

## Original producers and remaining dependencies

`loot_table/attributefood.json` is an8-roll pool with native uniform0..8 bonus rolls multiplied by native luck. Every entry count1, weight10. Original qualities: apple/max_health5, cooked_beef/attack_damage10, glow_berries/luck5, sweet_berries/attack_speed10, dried_kelp/armor3, cookie/armor_toughness2, golden_carrot/knockback_resistance5, melon_slice/movement_speed10. Original names/lore/tier and carrier components must be packaged through the root's existing native loot migration; this service does not synthesize replacement items or filter entries.

`loot_table/sussy_molten_penis.json` is8rolls; each count1,weight10: dried_kelp/random_health quality5, poisonous_potato/random_damage quality10, golden_apple/random_speed quality10. Preserve complete original producer payload and native weighted/luck behavior. Both producer tables remain root-owned packaging/readiness work; no resource edits were authorized here.

The bounded transitive consumer closure is20functions: eleven top-level rewards, eight shared base-update macros and one random macro. It has no summon/death/structure/drop child dependency. `advancement/pills/*.json` and `other/attributes/pills/` are a separate broad pill consumer family, outside the explicitly assigned eight fixed plus three random flags. They must not be silently claimed implemented by this service. Other rootadv foods, `inf`/`inf_old`/general food counters and wider lucky_items_1 transitive consumers remain outside this bounded slice and under the full-pool gate.

No pending author choice is needed for the eleven ordinary source payloads. A deliberately manufactured multi-flag stack can match multiple independent consume advancements; this service grants all matches in deterministic fixed-family order, then random_damage,random_speed,random_health. Ordinary source leaves carry one of these flags each, so they do not depend on a registry-specific simultaneous advancement ordering.

## Validation limits

Read all eleven live criteria and all20 transitive source functions, existing KFC/Aimbob consumption bridges, `ItemEffectLevels`, native attribute cap policy/mixin and mapped command implementations. No build or server was run by this agent. Native fixture qualification, source producer migration/packaging, root integration and the full Chaos dependency release gate remain root-owned.
