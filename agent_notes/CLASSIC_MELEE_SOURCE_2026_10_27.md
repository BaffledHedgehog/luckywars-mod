# Classic accepted-hit melee - source preparation, 27 October 2026

Read-only prep, separate from late-swords qualification. No builds, gameplay tests, installation or source/RP changes. Existing AGENTS/RESOURCE_PACK user requirements apply. Source root S=`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`. Paths below relative to S. Source text is inspected, legacy engine behavior is not observed.

## Current native gap matrix

| Flag/family | Source dispatcher items/swords.mcfunction | Current live Java finding | Next scope |
|---|---|---|---|
| feeding |3 feedingsword | no item flag/name/model handler | clear small gap |
| cool |5 coolsword | no flag handler |17-way random attack closure, defer |
| cool_old |6 coolsword_old | no flag handler | different17-way legacy inline table, defer |
| fragmentsword |8 fragmentsword | no flag handler | cooltype0..16 deterministic current table, defer |
| fragmentsword_old |9 fragmentsword_old | no flag handler | separate old attack mapping, defer |
| vampire |11 inline regeneration | no vampire flag/model handler; LbcLifestealer is a wand, not this sword | clear small gap |
| ender |13 endersword | no ender flag/model handler; existing Ender wand is separate | spreadplayers semantics need bounded terrain decision |
| heal |23 devilsword | no heal item flag/model handler; ShadowAbsorptionMixin heal method injection is unrelated | strength-dependent multiple independent branches need own closure |
| seaguardian |25 seaguardian | no flag/model handler | clear two-branch effect gap |
| gravitationsword |27 gravitationsword | no flag/model/curse handler | bounded attribute curse closure below |

Evidence: exact quoted flag searches across current src/main/java (excluding checks) find none of these flags as item keys; only quoted heal is a mixin method name. Searches for steaksword/vampiresword/guardiansword/gravitysword and effect_gravity_curse find no live Java handler. Generic shared effect, food_count and native attribute support exist; these do not activate item semantics. No claim that an imported JSON or independent wand proves sword support. Ice/Electro/Mortis/Fevgalea are outside this prep.

## Common admission and required native precedence

`advancement/gowno.json:1-60`: player_hurt_entity, blocked=false, damage type outside minecraft:arrow_only, victim not interaction, attacking player outside nexus/imprinted. Reward calls items/swords; dispatch checks current SelectedItem. It is accepted non-arrow damage, including attributable indirect and fatal events, not just a mouse click or physical melee swing. Source dispatcher revokes the advancement for repeat operation. Initial `items/swords.mcfunction:2` copies owner team_number into fake *tempt lbcID2; same_team predicate compares target team_number with that number.

No charge, mana payment, explicit proc cooldown or separate RMB input is coded for the four detailed candidates. Feeding has no antimagic guard. Vampire/Guardian/Gravity each reject a nonspectator nomagic_active player within20 or stopper_magic marker within20. Guardian/Gravity targets use source same_team exclusion; native allied-player and technical-role protection remain mandatory. The established friendly-mob exception concerns projectiles and does not itself authorize removing these area effects' source team exclusion. Resolve each target guard against the actual source and shared target API. Preserve native Time Stop/input admission and owner attribution for delayed effects/falls/deaths. Do not copy global scratch storage/tags into native concurrent state.

Retain prior signed-effect and attribute-layer rules: exact amplifier/duration, no blanket positive-only clamp, independent hidden layers, native instant/effect semantics and author item-specific caps. Do not globally alter effect/attribute caps to fit this batch. Gravity source ranges include negative multipliers; existing signed attribute policy must be reused. Unknown failure/leak behavior below requires a pending author decision, not a silent repair.

## Four bounded missing contracts

### Feeding / Steak Sword

`workingitems/feedingsword.mcfunction:1-4`: saturation1 second amplifier127 hidden particles, generic.eat and player.burp master to all at owner volume1/default pitch, then food_count+1. Self effect only; no target scan or damage child. This is a real saturation effect grant, not a synthetic fixed hunger refill. Shared food_count is already native in LbcColors/LbcKfcFood/LbcMana; use same score. `resets.mcfunction:74` zeros it; load:139 creates objective. No private timer/HUD/lifetime. Native owner attribution of original hit suffices.

Steak advancement is item_durability_changed with durability<=0, matching transformable/sword/feeding1 and excluding spectator, parent swords/epic. It is not a food_count threshold. Source has current transformable and legacy untagged-to-transform variants; preserve both exact presentation if enabling both. No dedicated ingredient craft was found by complete flag/helper search; ordinary sword transformation/reforge supplies current leaf.

### Vampire Fang

`items/swords.mcfunction:11`: admitted hit gives owner regeneration1 second amplifier4 (Regeneration V), hide particles true. No extra damage, direct heal command, lifesteal fraction, charge, sound, particle or temporary entity is coded. Do not replace regeneration with immediate HP injection merely because duration is short. Shared layered effects and ordinary native heal behavior apply. Current+legacy loot use iron sword with source attack_damage modifier+1, leaving omitted default components intact.

`advancement/true_advancements/swords/vampire.json`: player-killed-entity task requiring mainhand transformable/sword/vampire1 (details ledger below). Source inline regen has no separate retained state/reset. No new signed-effect cap exception is implied.

### Sea Guardian Sword

`workingitems/seaguardian.mcfunction:1-2`: if owner is_swimming, always branch1; otherwise25% branch2. Predicate is swimming flag, not simply wet/underwater/in-water. Mutually exclusive branches; random must not be rolled for swimming path.

Branch1 (`seaguardian_1:1-6`): all entities distance0.01..7 excluding spectator tag and same_team get mining_fatigue60 seconds amplifier127 plus weakness10 seconds amplifier1; owner water_breathing10 seconds amplifier2. Branch2 (`seaguardian_2:1-5`): same targets mining_fatigue20 seconds amplifier127 plus water_breathing2 seconds amplifier1 (beneficial breath on enemies is literal source). Both play elder_guardian.curse master globally volume1/pitch1, and10 forced elder_guardian particles spread5/5/5 speed1 targeted at players distance0.01..7 excluding spectator/same_team. It scans a radius, not only the hurt victim; it does not require target HurtTime. Source includes nonliving selector entities although effect commands may fail on them; native eligible living targets retain role guards.

Guardian task advancement requires player_hurt_entity with target player and swimming owner holding transformable/sword/seaguardian1. No charge/private lifetime/HUD. Ordinary native effect expiry/cleanup and inherited curse consequences apply.

### Gravity Sword

`workingitems/gravitationsword.mcfunction:1-9`: phantom.flap master globally volume1/pitch2; six independent inclusive integer random draws scaled.01, once per activation and shared by all matched targets. Targets #minecraft:mobs distance0.01..8, !spectator, HurtTime10, !same_team. This is wider legacy nearby-recent-hurt targeting, not exact event-victim selection; native role/ally policy still applies. Source HurtTime predicate may miss some valid event targets, especially fatal/indirect; record this as source artifact rather than claiming observed native parity.

Macro targets get have_lucky_effects_1s tag and effect_gravity_curse=10. Removes gravity slamslamslaaaaaaamslaaam and removes six gravity_curse attribute modifiers, then adds each of six with add_multiplied_total. Ranges:

| Attribute | Random integer /100 |
|---|---|
|fall_damage_multiplier |-100..400 (-1..4) |
|gravity |-120..200 (-1.2..2) |
|step_height |-400..100 (-4..1) |
|safe_fall_distance |-100..100 (-1..1) |
|jump_strength |-400..400 (-4..4) |
|movement_speed |-100..100 (-1..1) |

Finally adds knockback_resistance gravity_curse -2 add_multiplied_total WITHOUT removing that modifier first. Source end routine removes only the six random attributes, **not knockback_resistance**. Repeated applications can therefore encounter duplicate modifier failure; curse -2 may leak past timer/leave_death cleanup. Do not label this intended permanent design. Record a specific pending decision before implementing persistence. No additional cap is specified by this item; apply already approved attribute policy.

`effects/tick_1s.mcfunction:3` runs curse only for !sin_greed_active and score>=1. effect_gravity_curse:1 ends when score1, then decrements1; score10 means10 one-second pulses subject to scheduler phase and Greed stalls. End removes six random modifiers. leave_death:399-410 removes six and zeroes score. HUD ui/get_data:71 adds red E162 score while positive. Source lacks owner tracking; native must retain original cause for altered fall/motion/death. No carrier/raycast/projectile/craft payment.

## Loot, acquisition and exact presentation

Current four leaves are in `loot_table/transformables_sword.json`, each weight10/count1, flags transformable1/sword1 plus family flag. All type row is component array type+sword aqua italicfalse; lore/name styles described below, no empty lines. Ordinary routes: function/other/transform:12, reforge:11 and reforge_crafting_0..3:11 select transformables_sword. Ultra pool/source luckyblock routes must be kept distinct from whether native currently imports them. No dedicated recipe matches these four flags/helper names.

|Family|Base / quality / model / tooltip|Name and lore|
|---|---|---|
|Feeding|wooden_sword /5 /lbc:steaksword /tier_d|steak_sword #780000 italicfalse; type row then steak_sword.lore white italicfalse|
|Vampire|iron_sword /10 /lbc:vampiresword /tier_c; attack_damage+1 add_value mainhand id minecraft:base_attack_damage|vampire_fang dark_red italicfalse; type row then vampire_fang.lore.1/.2 red italicfalse|
|Gravity|iron_sword /9 /lbc:gravitysword /tier_c|gravity_sword italicfalse (color omitted); type row then gravity_sword.lore.1/.2 white italicfalse|
|Guardian|iron_sword /5 /lbc:guardiansword /tier_b|guardian_sword italicfalse (color omitted); type row then guardian_sword.lore.1/.2 white italicfalse|

Only Vampire defines an attribute rewrite among these four; do not invent missing speed/damage/durability overrides. Legacy_luckyblock also contains Feeding/Vampire with no type row or tooltip_style, only feeding1/vampire1 flags; legacy Vampire modifier id is vu89qjduxlsodkiogaisrepog, amount1 mainhand/add_value. These are distinct exact source payloads.

## Source references and hashes

Small helper contents are authoritative at the referenced lines; hashes are original-byte SHA256. No runtime result is inferred solely from text.

- `advancement/gowno.json` SHA256 `b65994157053db257f20ffdd9304e2a0efb204114d2b73e4eb7da9f58b9a2e2f`
- `function/items/swords.mcfunction` SHA256 `36162bd3115e12c5d8b0626f619d0eb88b408505282519a191ea7d1bdeb33f1f`
- `function/workingitems/feedingsword.mcfunction` SHA256 `f9337c237099a5ad8a0e0be68e9f18fd40b2efc1bdda7578259d2ac91c8bf6c7`
- `function/workingitems/seaguardian.mcfunction` SHA256 `89e4ea31779141fe653cf1b9718e57f278b328395de93630b1341dc76e2496db`
- `function/workingitems/seaguardian_1.mcfunction` SHA256 `58381477c546d938146ef433e07823417fc9d0848c08415b85c2a6b991c9a927`
- `function/workingitems/seaguardian_2.mcfunction` SHA256 `c326697ebfe2fb782f7d8d9b92d5f75005b940ebfa71f4b63ce481601fc66042`
- `function/workingitems/gravitationsword.mcfunction` SHA256 `017c17bb0b6cafb2fa0cc47690760b82e9c65f8f45029d310a98337121e979f4`
- `function/workingitems/gravitationsword_atmacro.mcfunction` SHA256 `72eabcc1596043c1c8a5a529c2fea2412f7e01d34cbfae6d094d89fc8928807c`
- `function/effects/tick_1s.mcfunction` SHA256 `d7cfdae2fa1ff746f6eee857e247ff9181ee2865aa04b8895ca1a79bfb837b75`
- `function/effects/effect_gravity_curse.mcfunction` SHA256 `aae7cf4965dffdd73bd16dca395cc5d97f9e9e6bd57285510437b1aa0466be64`
- `function/effects/effect_gravity_curse_end.mcfunction` SHA256 `508b90c73dcce5e5fe439d9fdaeb2f7903c992ae931444d479351b6a897457ab`
- `predicate/swimming.json` SHA256 `c5b44c48af45cdb7f52e108f35484a3a980c5ebf2488e0529079af61d81d0b52`
- `predicate/same_team.json` SHA256 `db24246d2c53902d7e5ee09839db69d9d2ea4d781a4c589ce266b44cf502a7a4`
- `loot_table/transformables_sword.json` SHA256 `9f4de28a3a531f37ad0884ca5cd59fce602e15f363e86846d21805032d71d997`
- `loot_table/legacy_luckyblock.json` SHA256 `bd7a1a5aa859362d96e93b7261ed160b48686829ffcd8be33a24daa93818ef01`
- `advancement/true_advancements/swords/steak.json` SHA256 `f5c20b4810f9b164dbb461e68c7084af9df510b71410a1fd94104475744445c0`
- `advancement/true_advancements/swords/vampire.json` SHA256 `89f1bd22c443f897fbc301512dadfcdf218bba26d6a8bd6d79f7507119defacf`
- `advancement/true_advancements/swords/guardian.json` SHA256 `d296cd8c7d0ea05e4fe1fb9b56f600cbf7f9137470d4729588c35feeb886f881`
- `advancement/true_advancements/swords/gravity.json` SHA256 `4be41f469ffbe34daf7c682f40055f35e28539060bb5abcfe99433696e1fbc9f`

Advancement criteria details (source JSON, unchanged):

`vampire`: {"parent":"lbc:true_advancements/swords/devil","criteria":{"requirement":{"trigger":"minecraft:player_killed_entity","conditions":{"player":[{"condition":"minecraft:entity_properties","entity":"this","predicate":{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable:1,sword:1,vampire:1}"}}}}}],"entity":[{"condition":"minecraft:entity_properties","entity":"this","predicate":{"type":"#minecraft:no_blood"}}]}}}}

`gravity`: {"parent":"lbc:true_advancements/swords/ender","criteria":{"requirement":{"trigger":"minecraft:player_killed_entity","conditions":{"player":[{"condition":"minecraft:entity_properties","entity":"this","predicate":{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,gravitationsword: 1}"}}}}}],"entity":[{"condition":"minecraft:entity_properties","entity":"this","predicate":{"type":"minecraft:player"}}]}}}}

## Read-only resource validation

`assets/minecraft/lang/en_us.json` SHA256 `7971188ae0d2e5f16f7253c6495168650451599e2826eeb46f7cfcb4a0e507f7`

```json
{
  "steak_sword": "Steak sword",
  "steak_sword.lore": "Eat your sword",
  "vampire_fang": "Vampire's Claw",
  "vampire_fang.lore.1": "Gives strong health regen",
  "vampire_fang.lore.2": "with hits",
  "guardian_sword": "Guardian sword",
  "guardian_sword.lore.1": "With a 15%% chance gives mining fatigue and prohibits breathing to the target",
  "guardian_sword.lore.2": "+ Grants you a Water Breathing III if you sprinting underwater on hit",
  "gravity_sword": "Sword of Dragon King Kajakut",
  "gravity_sword.lore.1": "Changes the gravity and all movement stats of the target",
  "gravity_sword.lore.2": "This gravity curse lasts for 15 seconds and rerolls every hit"
}
```

`assets/minecraft/lang/ru_ru.json` SHA256 `9ba24ccecc189950fe970f525ff45b6e40df5b375fabd333cfa6786ca8dac2fc`

```json
{
  "steak_sword": "Стейковый меч",
  "steak_sword.lore": "Восстанавливает сытость при ударе",
  "vampire_fang": "Клык вампира",
  "vampire_fang.lore.1": "Даёт сильную регенерацию",
  "vampire_fang.lore.2": "при ударах",
  "guardian_sword": "Меч стража",
  "guardian_sword.lore.1": "С шансом 15%% накладывает на цель утомление и запрещает дышать",
  "guardian_sword.lore.2": "+ Если вы ещё и бежите в воде, на вас наложит водное дыхание 3",
  "gravity_sword": "Меч Короля Драконов Каджакут",
  "gravity_sword.lore.1": "Изменяет влияние гравитации на цель",
  "gravity_sword.lore.2": "Один из семи мечей Бога Мечей"
}
```

- `assets/lbc/items/steaksword.json` SHA256 `9a86c5fda1840497d454829943a92e5fc14f771c41d72a9a8da40267fe50567b`
- `assets/lbc/models/item/steaksword.json` SHA256 `7d54816e5fc6555d919f35b4ed8bad965efb0e7340110475a30a7bfcace157c8`
- `assets/lbc/items/vampiresword.json` SHA256 `af3ced1ae4e59f962226db02579f6bc464f46f392e570e78bcddb9c777157038`
- `assets/lbc/models/item/vampiresword.json` SHA256 `48298c6ce3e23930b88c0c9500f47afb04b7d6bd50468f8207a32a618325c7ab`
- `assets/lbc/items/guardiansword.json` SHA256 `e987a32850d6bef9200af604a4ec22064e3d7442835ee5b16903d67911929efc`
- `assets/lbc/models/item/guardiansword.json` SHA256 `73c9928230c9fe07b8d55a8f8aef0850f6dc1fbe26b19eb99d3b05bd9e4a99f3`
- `assets/lbc/items/gravitysword.json` SHA256 `4865b26bd8c50f4aa7d3ed787ee569a7f7d4fa501093f79c3faa402a0818fc3e`
- `assets/lbc/models/item/gravitysword.json` SHA256 `01dac2d13ce779242d7a9c5d2445ebacfc69ed925272ef8f2fe0c91146494409`

Existing exact keys/models must be reused; no RP writes or template changes made. Native signed-effect/layer/cap behavior is a precondition, not newly qualified here. Gravity modifier leak/duplicate knockback and legacy HurtTime target scan are pending uncertainties for parent; no chat questions sent.
