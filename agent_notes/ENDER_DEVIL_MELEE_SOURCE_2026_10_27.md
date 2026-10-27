# Ender and Devil melee - source preparation, 27 October 2026

Source-only audit, no code/build/server/staging/RP changes. S=`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`. Relative paths below refer to S unless explicitly native. Legacy1.21.4 runtime unobserved; native26.3 engine source is inspected, not proof of old runtime. Neither ender/heal item handler exists in current Java; Ender wand and generic healing support do not implement these swords. This is next-family preparation, separate from Feeding/Vampire/Guardian qualification.

## Admission

`advancement/gowno.json:1-60` accepted unblocked non-arrow player_hurt_entity (outside arrow_only), victim not interaction, player outside nexus/imprinted -> `function/items/swords.mcfunction`. Mainhand ender1 at line13 calls endersword; heal1 at line23 calls devilsword. This includes attributable non-arrow indirect/fatal hits, not an added RMB/sneak input. Both have stopper_magic marker within20 rejection. Ender unusually checks nonspectator nomagic_active players only within2 (limit10); Devil uses20 (limit1). No charge, mana payment, private cooldown or item-specific carrier. Source advance reward is revoked for repeated operation.

Native required precedence remains shared Time Stop/input/role/match guards, allied-player protection with friendly mobs targetable, original-owner inheritance for teleport/fall/self-curse consequences and all signed effect/layer/cap requirements. Source nearby HurtTime scans and missing ownership are not already-correct native event targeting. No current item behavior is claimed implemented.

## Ender Sword - exact sequence

`function/workingitems/endersword.mcfunction:1-10`, executed at owner pre-teleport position:

|Line|Operation|
|---|---|
|1|enderman.teleport master to players within10 of owner, volume10/pitch1|
|2|at EACH entity within10 with HurtTime10, same sound to players within10 of THAT entity, volume10/pitch1|
|3|100 forced portal particles at owner +(0,0,1), spread.2/.5/.2 speed.5|
|4|at EACH entity within8 HurtTime10, same portal particles at its +(0,0,1)|
|5|spreadplayers ownerX/Z min separation0 maxRange10 respectTeamsfalse, all entities within10 HurtTime10 with !the_nether predicate|
|6|same selector reevaluated after line5; Nether entities use under120, maxRange10|
|7|at EACH entity within11 HurtTime10 after earlier teleports,100 forced reverse_portal particles at its +(0,0,1)|
|8|owner Nether spreadplayers centered ORIGINAL ownerX/Z min0/max10 under120 false|
|9|owner non-Nether spreadplayers same original center min0/max10 false|
|10|100 forced reverse_portal particles at ORIGINAL owner +(0,0,1)|

`predicate/the_nether.json` is location dimension minecraft:the_nether. Entity selectors do NOT exclude allies, spectator-tagged entities, strict_map_object, nonliving entities or owner. HurtTime NBT naturally applies to entities exposing it. Owner with HurtTime10 can be selected in first phase and then be teleported again in explicit owner phase. Each function command's execution origin remains original owner position; changing entity position does not mutate command context. Lines5/6/7 selectors are evaluated independently, not a frozen event-victim list. Effects may hit unrelated recently hurt entities. No direct damage/effect command, forward-local coordinate, rotation change or velocity modification is specified here.

Important: source maxRange is **10**, not20. The existing Epic random teleport branch uses20; do not transfer that constant. Spread area is an X/Z square centered owner, not a10-block disk or a random directional offset. Source no artificial native-height +.5 is specified. Nether ceiling is120, not the wand125. Target and owner searches are separate calls; failures of an earlier command do not mean later function lines stop automatically.

## Native spreadplayers semantics and resident implementation boundary

Inspected native26.3 archive `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`, entry `net/minecraft/server/commands/SpreadPlayersCommand.java`:

- lines102-126: reject maxHeight below minY; random candidates in center +/-maxRange; false respectTeams makes one candidate per entity.
- lines154-232: at most10000 relaxation/safety passes. With min separation0, no distance repulsion is needed, but unsafe columns are rerolled; command fails when unable to find a complete safe assignment. This is not guaranteed success on empty sky islands.
- lines259-260: teleport to floor(candidateX)+.5, getSpawnY, floor(candidateZ)+.5, preserve entity yaw/pitch, empty relative movement flags, teleport booleantrue. Source function cannot justify an extra +.5Y.
- lines337-354: scan downward from maxHeight+1, find nonair block with TWO air blocks directly above; spawnY=blockY+1, fallback=maxHeight+1.
- lines357-360: support must lie strictly below maxHeight, not liquid and in ENTITIES_CAN_TELEPORT_TO. Native command uses block reads without a resident-only guard; directly executing it is not proof of no chunk loads.

Current `LbcPortalDrops.spawnY:155-166` reproduces the native downward/two-air algorithm on a resident column. `LbcSurfaceTeleport.destination:15-30` searches finite square columns in random permutation, skips nonresident chunks, checks WORLD_SURFACE and native safe support, returns centered X/Z and integerY ornull. This is a reusable resident safety primitive, **not exact random distribution/all-or-nothing multi-entity spreadplayers semantics**. It does not enforce whole entity collision shape/headroom beyond native two-air predicate, and should not add a stronger rule without identifying the change. It searches width*depth unique columns rather than10000 continuous candidate attempts.

Current `LbcEnderWiki.ender:74-88` is not an exact sword shortcut: it spends wand mana through use, supports Academy slow falling, uses centerzero unless noBorders, radius100, Nether ceiling125, teleports Y+.5, emits extra owner-local very loud sound and awards wand advancements. Reuse only explicit resident search/helper pieces with sword constants and lifecycle/attribution. For exact current native default ceiling inspect the registered command branch (archive lines30-95); source syntax omits under outside Nether. Do not silently hardcode120 or125 for every dimension.

Pending decision: legacy ALL nearby HurtTime targets versus precise accepted victim; native default allied-player protection remains authoritative. Resident finite search failure/partial versus group atomic result needs explicit contract, particularly owner potentially teleported twice and post-move reverse particles. No user question sent in chat.

## Devil Sword - all strength-dependent branches

`function/workingitems/devilsword.mcfunction:1-15` checks independently and in this exact descending order: active strength amplifier5 ->helper6; amplifier4 ->6; amplifier3 ->5; amplifier2 ->4; amplifier1 ->3; ANY active strength ->2; finally selected heal1 and NO active strength ->1. It is not an else-if switch. Source NBT active_effects matching does not recursively inspect hidden lower layers. Amplifier0 and amplifiers outside1..5 fall through generic ANY branch2; signed negative amplifiers must not be clamped into the normal ladder.

E=`function/workingitems/devilswordattacks/`. All effects are self-target, duration command10 means200 ticks for ordinary effects. No hide-particles argument, ambient or icon override is supplied. No sounds/particles/targetscan/projectile/durability mutation is added by these helpers.

|Helper lines|Exact ordered effects|
|---|---|
|1:2|strength200 ticks amplifier0|
|2:1-3|speed200 amp1, nausea200 amp1, strength200 amp1|
|3:1-3|nausea200 amp1, strength200 amp2, speed200 amp2|
|4:2-5|nausea200 amp1, instant_damage duration argument1 amp0, strength200 amp3, speed200 amp3|
|5:1-5|wither200 amp3, nausea200 amp1, instant_damage argument1 amp1, strength200 amp4, speed200 amp4|
|6:1-6|grant lbc:true_advancements/swords/devil; wither200 amp5; nausea200 amp1; instant_damage argument1 amp2; strength200 amp5; speed200 amp5|

Instant_damage is a native instant effect, provided duration1 is1 TICK by EffectCommands semantics, not20; do not substitute generic raw damage or ignore undead reversal/signed native effect policy. Actual HP loss depends on native rules and modifiers; text alone does not prove survival. Source helper has no explicit damage attacker field. Native addEffect must retain owner and inherited curse cause, without causing uncontrolled recursion from newly attributed self-damage.

Because later ANY-strength branch2 is still evaluated after an upper helper, a hit at active strength1 runs3 then2; strength2 runs4 then2; strength3 runs5 then2; strength4/5 runs6 then2. Later weaker strength/speed grants do not overwrite the stronger active effect under native layering but DO retain lower layers/timers; don't omit them as visually redundant. No-strength hit reaches helper1 at the end, and cannot retroactively run earlier branch2. Strength0/negative/>=6 run only2 and preserve stronger active layers where appropriate. If an instant self-hit kills owner, later commands may still be attempted by source function; do not claim observed engine result or intentionally resurrect owner.

Stepped helper6 explicitly grants impossible-trigger task devil. `advancement/true_advancements/swords/devil.json` parent slimeball, icon wooden_sword/lbc:devilsword, task. `devil_full.json` is separate inventory challenge parent devil: nonspectator with current transformable heal sword + deviltotem + bloodmagic staff + summonerghast + demonish accessory (exact flag demonkishki1). This does not change sword combat behavior.

## Acquisition and exact presentation

Current Ender leaf `loot_table/transformables_sword.json`: weight10/quality9/count1, netherite_sword/model lbc:endersword/tier_d; name ender_sword dark_purple italicfalse; lore row type+sword aqua italicfalse, then ender_sword.lore.1/.2 light_purple italicfalse. Custom data transformable1/sword1/ender1. No attribute/durability rewrite. Legacy_luckyblock Ender: same vanilla base/model/name and two descriptive lore rows, no type row/tier style; only ender1 flag.

Current Devil leaf same current pool: weight10/quality5/count1, wooden_sword/model lbc:devilsword/tier_f; attack_damage amount0 add_value mainhand id minecraft:base_attack_damage (source overrides damage component; do not infer heal-on-hit from flag name). Name devil_sword dark_red italicfalse; type+sword aqua italicfalse plus devil_sword.lore.1/.2/.3/.4 red italicfalse. Custom data transformable1/sword1/heal1. Legacy: wooden_sword/model devilsword, attack_damage0 mainhand/add_value id edkoedkoedkodaerisaiwisjfijf; no tier/type row; devil_sword_old.lore.1/.2/.3 red italicfalse; heal1 only. No direct healing branch exists despite flag heal.

Source transformation/reforge sword pool routes supply current leaves (`function/other/transform:12`, reforge:11, reforge_crafting_0..3:11); legacy luckyblock supplies old variants. No dedicated ingredient recipe/helper was found for either family. Native compiled transformation pool availability must be distinguished from actual new handler import. Source Ender advancement task parent steak requires player kill while current mainhand transformable/sword/ender1. Existing definitions can be reused; native availability must be checked by final implementation qualification.

## Source SHA256 ledger

- `advancement/gowno.json` `b65994157053db257f20ffdd9304e2a0efb204114d2b73e4eb7da9f58b9a2e2f`
- `function/items/swords.mcfunction` `36162bd3115e12c5d8b0626f619d0eb88b408505282519a191ea7d1bdeb33f1f`
- `function/workingitems/endersword.mcfunction` `82e854ae4c6e6bb794d7478c6e311f8d68203b9c469f195e6b288cee3ed98038`
- `function/workingitems/devilsword.mcfunction` `dabd253734481626423df7707cc8f8b63e71e8644f9baf1f225570c4a5eda478`
- `predicate/the_nether.json` `e0eb79c82c822b0315b682510b2284830b6b17fb1de6c64a85cdda6236d20962`
- `loot_table/transformables_sword.json` `9f4de28a3a531f37ad0884ca5cd59fce602e15f363e86846d21805032d71d997`
- `loot_table/legacy_luckyblock.json` `bd7a1a5aa859362d96e93b7261ed160b48686829ffcd8be33a24daa93818ef01`
- `function/workingitems/devilswordattacks/1.mcfunction` `2721ad65b693fbd149afa1ca4a28f3248fa5b7ab44759a71fdda3f5b75f4dc3b`
- `function/workingitems/devilswordattacks/2.mcfunction` `f4646f1036f0c929db0419d2e8b71ac2e75658883349511960663c14993fa254`
- `function/workingitems/devilswordattacks/3.mcfunction` `e0d615257463cee4c07ebd85e40569224b3c63fb672692ed5d4f099e1bb4bc08`
- `function/workingitems/devilswordattacks/4.mcfunction` `d45e7d9e1f4b302a212e652f41657095366f3548d86c4c1572b39b308dd3ad50`
- `function/workingitems/devilswordattacks/5.mcfunction` `503d699bd69a0379ceacaba614da77e27120a0f646902f37742f97dd44f6623f`
- `function/workingitems/devilswordattacks/6.mcfunction` `a80e2e1dfc569f4200180c4d983619372c0dfd2047f6a27cbaad9382d41bc405`
- `advancement/true_advancements/swords/ender.json` `cdcf604724edae6a31d08f870f2b4effa07ba968c8551a409aacf595f3482297`
- `advancement/true_advancements/swords/devil.json` `2dfbca0375d67abe159377983d12991e06c4292bfa42dcfa41b767216ad2eca3`
- `advancement/true_advancements/swords/devil_full.json` `d6a06784b07b8d056811223da7bc370375ef39b3bdc0117e58de58e1f491f743`

Native archive SpreadPlayersCommand.java entry SHA256 `2db5b24f8c775d06edd68158c2830dc709cd95e12be210e29fdfcbe69adab350`. This is26.3 code evidence, not legacy runtime verification.

Native registration default-ceiling expressions: L66: c.getSource().getLevel().getMaxY() + 1,

## Read-only RP audit

`en_us.json` SHA256 `7971188ae0d2e5f16f7253c6495168650451599e2826eeb46f7cfcb4a0e507f7`

```json
{
  "ender_sword": "Ender sword",
  "ender_sword.lore.1": "Randomly teleports you and",
  "ender_sword.lore.2": "the target on hit",
  "devil_sword": "Evil's sword",
  "devil_sword.lore.1": "Becomes stronger every hit,",
  "devil_sword.lore.2": "sacrifizing owner's health. If",
  "devil_sword.lore.3": "use too frequently,",
  "devil_sword.lore.4": "it can easily kill you!",
  "devil_sword_old.lore.1": "Getting stronger",
  "devil_sword_old.lore.2": "with every blow",
  "devil_sword_old.lore.3": "Slowly kills the owner"
}
```

`ru_ru.json` SHA256 `9ba24ccecc189950fe970f525ff45b6e40df5b375fabd333cfa6786ca8dac2fc`

```json
{
  "ender_sword": "Меч края",
  "ender_sword.lore.1": "Случайно телепортирует",
  "ender_sword.lore.2": "владельца и цель при ударе",
  "devil_sword": "Меч дьявола",
  "devil_sword.lore.1": "Становится сильнее с каждым ударом,",
  "devil_sword.lore.2": "жертвуя здоровьем владельца. Если",
  "devil_sword.lore.3": "использовать слишком часто,",
  "devil_sword.lore.4": "может с лёгкостью убить вас!",
  "devil_sword_old.lore.1": "Становится сильнее",
  "devil_sword_old.lore.2": "с каждым ударом",
  "devil_sword_old.lore.3": "Медленно убивает владельца"
}
```

`assets/lbc/items/endersword.json` SHA256 `870623238f77224776c1e5aebd70a74fd50c2a82428918eccd52cd55f91e0078`; {     "model": {         "type": "model",         "model": "lbc:item/endersword"     } }

`assets/lbc/models/item/endersword.json` SHA256 `36120c99f993731997ba3cd922578eccd3e91e69347be7a7fa6ebca4841a6847`; {   "parent": "minecraft:item/handheld",   "textures": {     "layer0": "minecraft:item/endersword"   } }

`assets/lbc/items/devilsword.json` SHA256 `17b2b756fd5b75afd4b944d9398e60e4f6c2de88fa9295e48279f7e807e82a9d`; {     "model": {         "type": "model",         "model": "lbc:item/devilsword"     } }

`assets/lbc/models/item/devilsword.json` SHA256 `550ef3da4ea6e6a7a64c85d6b474686a9346e46bdee616e07a4439efd84f5f1a`; {   "parent": "minecraft:item/handheld",   "textures": {     "layer0": "minecraft:item/devilsword"   } }

Pending native qualification must cover signed/high strength, hidden layer sequencing, self-instant damage attribution/recursion, owner death and per-command teleport selector reevaluation. Epic/Fragment remains a separate full audit with pending fang/feedback choices; no implementation of it is claimed here.
