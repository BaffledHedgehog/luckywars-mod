# Armored Inventory (Magnet reward skill2) - source/native audit 2026-10-27

Static preparation only. Legacy source and author RP are read-only; no gameplay, catalog, tests, build, server or qualification changes. References are legacy `data/lbc/function/*.mcfunction` unless stated otherwise. Source text is distinct from observed historical execution. Existing native attribution, roles, signed caps, allied-player protection, passive-under-stun and Time Stop rules remain authoritative.

## Decision

Do not add skill2 to the catalog on the strength of the existing Jaakivi check. The whole perk changes an inventory dispatcher, including an **already live Electroblocker**. Several other item engines are absent, not merely missing skill aliases. There is no existing complete Infinity/Nexus/Eternal Wealth engine hidden under a native alias in the inspected production code. A complete skill port is a compatibility integration across those engines, not a new generic inventory immunity flag.

The finite independently complete next item from this audit is **Liquid Abyss (`liquidvoid`)**: a full self-damage inventory passive, with exact40-tick cadence, original vanilla loot/presentation, and skill2 hands admission; no Belt/Quiver choice is involved. This is one complete item family, not an incomplete subset of Infinity or a claim to finish skill2. Its native source loot is already packaged via voidman. Keep skill2 unavailable until all promised currently supported consumers and newly introduced complete families obey its actual dispatch. Red Nexus String is also finite but its source rewards include seven separate sword charge counters/nanobots, so a complete native reward contract must precede enabling it.

## Exact selection and reward wiring

`advancement/true_advancements/accesories/magnet.json` rewards `swrg_kit_integration/gain/skill`, not an item or attribute. `swrg_kit_integration/gui/page/10000:12,63,93` gates slot1 by Magnet advancement; `gui/select/10000:5` writes lbc.skill2. Ranked page highlights skill2 at10000_ranked:27. Original GUI leaf `loot_table/gui/page/10000/1.json` uses iron_block and translation lbcskill.1, seven lore rows lore.lbcskill.1.1..7 gray italicfalse, plus original pick/win statistics. No source skill2 attribute modifier is attached by this selection.

Author RP English name: Armored inventory. Lore promises immunity to some inventory-active items (Penis-bomb, Malevich Square), then says Infinity/Eternal Wealth/Liquid Void/Jaakivi/Nexus Strings activate only in hand. **Literal source is narrower/more specific:** Infinity/Strings/Wealth require selected MAIN hand; Liquid Abyss/Jaakivi allow either weapon hand; Electroblocker and Touhou inputs disappear from the instant switch even though not listed in lore. Kiuaskivi is unrelated: Jaakivi is runic_ice_shard, not the lava arcanum kiuaskivi.

Existing native `LbcLoadouts:301,325,366-367` selects profile skill, checks allowed/config, and applies catalog data. No current lbc_loadouts.json skill2 entry/gate exists. Paper `SwrgNativeService:612` independently confirms source slot1/id2/Magnet gate. Native skill configuration disabled means ordinary dispatch, not immunity. Original notification can remain available independently of selectable gameplay.

## Full skill2 switch

`items/changed_inventory/sel:38-40,54` tracks instantitem_active from container.*, weapon.*, armor.* and removes only when absent from all. `players_tick:15` calls instants/select only while this tag exists. `tick -> tick_not_timestopped:4 -> mobs_tick:4 -> players_tick` excludes spectator/time_stopped players and nexus/imprinted dimensions. No payment or general stopper gate wraps the switch. Individual helpers have their own gates. This is periodic inventory activation, **not right-click use**; a main-hand native item button is not an equivalent pre-existing implementation.

`other/instants/select:1-3`: skills enabled and skill2 -> select2; otherwise -> select_old. No legacy-mode exemption appears in this switch. No own timer/HUD/lifetime, effect owner or cleanup belongs to skill2 itself. Dirty inventory state must reflect held-item changes and removal; current configuration/profile changes must take effect without stale immunity.

| Family | Ordinary source branch | With skill2 | Actual native boundary |
|---|---|---|---|
|Electroblocker|select_old:1 Inventory electroblocker1, unless marker stopper<=20 -> electroblock/tick|Entire branch omitted, even main hand|LbcMagic:367-370 recognizes registered/native or flagged vanilla blocker across inventory/equipment. LbcElectro:188-191 caches carriers; :449 activates with magic.suppressed gate but no skill2 check. This is a genuine existing compatibility gap, not a new missing blocker engine.|
|Infinity|select_old:2 Inventory infinity_rand1 -> random_infinity|select2:1 requires SelectedItem infinity_rand1|No flag/name/14-way attribute/tag engine found in production. An inventory-absent engine is not already hands-only compatible.|
|Six Penis Bomb variants|select_old:3 Inventory penis_bomb1 -> player_select|Entire branch omitted, even in hand|LbcKfcRewards:156-184 produces exact flagged dropped items; LbcOrdinaryAccessories:104 recognizes nearby bait for Magnet task. Neither executes the six inventory penalties. No native dispatcher found.|
|Malevich|select_old:4 is commented, so datapack branch is inactive|No datapack execution either way|Actual registered Paper consumer separately checks skill2. Native has no corresponding full PDC Malevich engine.|
|Black, blue/green, red Nexus Strings|select_old:5-7 each Inventory flag -> helper|select2:2-4 SelectedItem exact matching color|No native wire flags/models/reward/portal engine found. White string is not in either dispatcher and must not be silently added as another skill2 branch.|
|Eternal Wealth|select_old:8 Inventory greedstone1 -> greed/pickup|select2:5 SelectedItem greedstone1|No native pickup/prestige/avarice terrain engine. LbcGreed is sin_greed_active combat policy, **not** greed_cursed/greedstone. Popit removes greed_cursed and spike reset clears prestige, but neither implements this curse.|
|Touhou10 inputs|select_old:9 Inventory[].custom_data.touhou_item -> select|Entire branch omitted, including hands|No native touhou_power/time_orbs/bomb_fragments/life_fragments input engine found. Other mana/coins/lives systems do not automatically consume these items.|
|Liquid Abyss|players_2s:4-5 independent container/weapon probes -> liquidvoiddamage|liquidvoiddamage:1: only CURRENT/nonlegacy, enabled skill2 requires either weapon.* liquidvoid; count still all /clear scope|No native liquidvoid/liquidabyss producer. LbcVoidArcanum is a different paid arcanum; it does not establish this inventory hazard.|
|Jaakivi|players_10t:6-7 independent container/weapon probes, unless stopper_magic20 -> iceshard|iceshard:1 requires either weapon.* when enabled skill2, no legacy exemption|LbcIceMaterials:54-77 genuinely implements both branches and hand admission; LbcFrost supplies cold/freeze/Resistance/task closure. Already live; retain it, do not re-port.|
|Kiuaskivi|arcanum dispatch by kiuaskivi flag|No source skill2 override|Existing LbcKiuas is independent and must keep its existing rules.|

## Missing item engine boundaries

### Infinity - whole14-way family, not next tiny implementation

`other/instants/random_infinity:1` uniform1..14; branch2 can show existing Looper explanation when skill3 (not skill2). Branches:

| Draw | Source action |
|---|---|
|1,2|Set manamax or manaregen100000000.|
|3|Set armor/toughness10000000 and burning_time -1000000.|
|4|Attack_damage base100000000.|
|5|Attack_speed10000000 and block_break_speed214708364.|
|6|Knockback/explosion resistance bases10000000.|
|7|Max_health base214708364.|
|8,9,10|Tag infinite_resistance / infinite_heal / infinite_saturation. players_15s:4-6 later refresh infinite Resistanceamp4, Instant Healthamp99, Saturationamp50; resets:100-102 and remove_tags:36-38 clear tags.|
|11|Give original infinite lucky-block carrot_on_a_stick (magicwand/luckyblockwand/craftusable, infinity enchantment, lbc:lucky_block tier_u). No full native luckyblockwand producer found; this is a dependency, not permission to replace with a finite lucky block.|
|12|Fall multiplier-1; movement/water efficiency, step height, sneaking speed, safe fall, oxygen bases10000000.|
|13|Named infinity_scale modifiers: +16 total on most listed stats; +32 total interaction ranges; +16 knockback value, +1 sweeping value; fall multiplier-.9375 total; gravity+16 total. Exact18 attribute lines34-52 remain authoritative.|
|14|infinity_scale1 scale-.9375 total, safe_fall+10000, fall multiplier-1 total; gravity modifier is named infinity_scale (literal inconsistent ID), -.8125 total.|

Finally :59 clears one matching item (full clear scope), blank title + infinity_feel subtitle, grants legends/infinity. Base changes/modifiers outlive consumed item and need native persistent owner/reset semantics, existing signed cap policy, and named-modifier duplicate behavior. No source sound/particle here. Acquisition: drops/infinity:1 original glowing/no-gravity repeating_command_block item; loot infinity_tuntija:29 flag; random_glack_1:58 references that table. No separate old infinity_rand variant found.

### Nexus Strings

Each matching source helper clears1 with full clear scope before rewards, then owner tellraw. Black: random X/Z[-2000,2000], Y[0,319] -> macro black_at_pre -> black_at forces chunk, replaces block with exact end_gateway and malformed/beacon id fields, exit portal[1,15000000,1], then removes forceload. This external-world operation cannot be called resident-only equivalent without an explicit implementation decision. Blue flag's helper is literally named green; spawns random_glack loot. Red gives20 XP points, mana+5000, swordcharged1..7 each+5, nanobots_tier+100. Charge caps/current signed rules still apply; do not reward only XP/mana and call complete. Each color original loot table has its own model, name and two lore rows; flags preserve names (blue model nexus_wire_green; black model nexus_wire_onyx). random_item:148-163 references all four wire tables; white has no instantitem flag and no skill2 switch. No _old wire flags found.

### Eternal Wealth and Greed curse

`other/greed/pickup:1-7`: clear1 greedstone, prestige+1, add greed_cursed; blank title/gold greed_curse_activate subtitle; owner enchantment_table.use1/pitch0; spawn avarice diamond marker, random coordinates X/Z[-130,130], Y[16,300], scheduled20tick placement of template lbc:avarice_diamond at-7 offsets. `players_1s:32` greed_cursed -> summon; marker cube/terrain transformation and avarice cure/reward callbacks are ongoing, not a harmless score. Popit27 removing curse does not implement these producers or terrain ownership. Acquisition drops/greedcurse places glowing item frame containing nether_star Eternal Wealth greedstone1/instantitem1; no _old flag counterpart found. Existing native original-player cause and terrain support-fall tracking would be needed; do not identify sin_greed_active with this family.

### Suppressed dangerous inventory families

Six bomb helpers are called once per distinct variant presence, in1..6 order; each clears1 for players and emits original global tellraw/ranged @a sound. Normal: self6 arrow, suction sound, cloud10. Dirty: self4 arrow, Slowness160t amp5, Weakness80t amp5, Nausea120t amp5, dirt100. Variant3: TNT fuse2 and spank sound. Holy: Blindness160t amp1, Levitation20t amp15 hidden, lightning, woo sound, flash100. Horse: Slowness20t amp127 hidden, self10 arrow, teleport local behind5 then up5, shityeah sound, redstone-block100. Multicast runs self0.1 arrow once **at every nonspectator entity** (executor remains owner), weewee sound and wool100. No own mana/cooldown. These engines need full ownership/roles/Time Stop parity before enabling, not an easy inert-protection skill claim.

Touhou input select consumes ALL matching counts:1 power+count;2 power+10count;3 sets150 if any, max150; power milestones add named damage/maxHP modifiers at10/25/40/55/70/85/100/115/130/150. Type4 mana100count via helper;5 lives+count tellraw;6 time_orbs count and helper;7 bomb_fragments count then removes5 and gives one bomb if>=5 (not full while-loop);8 life_fragments removes10 and grants1 life if>=10;9 manaregen+count;10 coins+2count with owner coin sound/tellraw. Source ordinary switch cannot be omitted for skill2 without understanding those routes, but this audit does not authorize partial Touhou input delivery.

### Paper Malevich compatibility (registered, not observed runtime)

`plugins/luckywars/src/main/java/me/luckywars/SigmaWarsMain.java:124-125` constructs/registers Malevich. `item/Malevich.java:67-100` runs after1 tick/every5: skips postconsume cooldown, excluded dimension, marker stopper_magic AABB20, and main scoreboard lbc.skill==2 (no #lbcskill config check). With inventory PDC Malevich, schedules atomic fill next tick. Ground-item conversion :103-144 remains independent of skill2, once20ticks, item AABB3, converts nearby non-Malevich drops; >100 gathered items may explode when nothing converts. Skill2 is not world immunity. Current Malevich leaf has instantitem1/malevich1 and PublicBukkitValues lws:item/lws:type malevich; do not revive commented datapack fill or replace Paper PDC engine with it. Any future port needs full consume/atomic inventory/cursor/dropped conversion closure separately.

## Complete independent Liquid Abyss contract

- Entry: `tick2s:1-2 -> tick2smobs:6 -> players_2s:4-5`, once40 scheduled ticks, nonspectator player/mob selector and ordinary source dimension exclusions. Two independent calls; selected mainhand counts container and hand. No marker/mana/nomagic/stun suppression in the item helper. Native authoritative Time Stop pauses it; do not invent right-click input.
- `other/liquidvoiddamage:1`: CURRENT only (!legacy1), enabled skills +skill2 returns unless either hand has liquidvoid. Legacy1 bypasses ONLY this guard. Ordinary profile inventories remain damaging even if skill2 is disabled.
- :2 `/clear @s *[custom_data~{liquidvoid:1}] 0`, stores result as int scale4. This counts full clear scope, including armor/craft/cursor, but trigger itself is container/weapon only. Once admitted via a hand, extra carried stacks still count. No consume or stack-size cap. :3 macro -> liquidvoiddamage_recur:1 applies `damage @s count*4 outside_border` unless void_resistance>=1. This is one attempted vanilla damage event per call, not a loop or guaranteed additive second hit; retain native damage acceptance semantics.
- Damage source has no attacker/direct entity in source. Do not fabricate shooter/player attribution; native source item provenance may be tracked without converting environmental self-damage into a player attack. Shared Greed/roles/death/caps and existing native environmental rules apply.
- No private score, HUD, sound, particle, advancement, recipe, cooldown, item removal or delayed task. Existing void_resistance is shared; helper checks it but does not decrement/reset it. Existing LbcAcademy:295-297 demonstrates explicit native score guard plus OUTSIDE_BORDER damage; generic DamageSource does not imply automatic void-resistance support.
- Original `loot_table/voidman.json:8-48` first pool guarantees count1 repeating_command_block, tier_ff, lbc:liquidabyss; dark_purple nonitalic name liquid_abyss and one dark_purple nonitalic liquid_abyss.lore; custom_data liquidvoid1 only. Other voidman pools are separate original mob loot and must remain unchanged. No old flag variant found. `entities:2` and consuming_wand_upgrade attach DeathLootTable lbc:voidman; random_glack_1:83 references table. Native build.gradle:254 includes exact voidman. Native consuming_presets.json:4 retains the exact Void Man DeathLootTable; LbcConsuming:104-125 loads presets. Do not infer that every historical source summon route is live merely from packaged loot/presets; verify one actual existing producer route when qualifying acquisition. This acquisition gate must be resolved before calling the item fully restored.
- Suggested native integration: inventory snapshot carries trigger scopes; at shared40tick phase perform two source-ordered admission attempts, count exact matching physical scopes as native /clear0 does, check void_resistance and pass one environmental OUTSIDE_BORDER hurt through existing native damage entrypoint. No Regen potion, arbitrary damage cap, optional collateral, or custom active-use item.

## Pending boundaries and proposed next batch

No Belt/Quiver choice blocks complete Liquid Abyss. Skill2 itself remains deferred until full affected-family integration; do not make a temporarily selectable perk with only Jaakivi/Electroblocker behavior. Black String's explicit remote forceload/portal behavior and Infinity's persistent extreme attributes/lucky-block-wand dependency need complete item-family contracts; their answers are not inferred from the existence of this source audit. Existing defaults settle normal allies/roles and Time Stop; do not open new WILD questions.

## SHA-256 evidence

Legacy source root above; full hashes preserve exact source/lore/helper bytes. Closure digests below are SHA256 of sorted `relativePath SHA256` records for recursively referenced existing lbc functions, not claims that every referenced item engine is already ported.

```text
other/instants/select closure-files=37 digest=5e71bc5db68d8c595d3bdc21461a7e84f7620fb0e7dd90aa9285be7b68fcc267
other/instants/select_old closure-files=28 digest=fac3d837caafb50151bb7f947b66fdafb3922b4f45e17951a0d5eab77d5cd599
other/instants/select2 closure-files=13 digest=e56bd437871b5379967e294bf57d27368c14dae6085f700d74aec5f1a1a54dfc
other/instants/random_infinity closure-files=2 digest=f2151476f2b8e808a1efb2def3eb4b1f9dc15358c9c2af4cef6c62936f0ed0b4
other/nexus/wires_other_worlds/black closure-files=3 digest=00d50159dfcdaa3e751c6fa6aca94ed323683056779bfde05671b7fff2f76bff
other/nexus/wires_other_worlds/green closure-files=1 digest=95ca85e4cd7a80dc49679f1cc656c71eef3ffff78f0ef42823ca33da4f4f4d2c
other/nexus/wires_other_worlds/red closure-files=1 digest=18b18be827e98c9714ec384daccb68ac77f9a13a8dfcf12793495ad1b4eeefdc
other/greed/pickup closure-files=5 digest=f1704b84b0f63fbcef18e57fa714720fa7bc85cf99a1ba17c1f339b423cb8e32
other/greed/summon closure-files=2 digest=26c1bc1b7e4b23b0a873537dd81c74e1a453f3f2399d0ce31d970174151c8ae8
other/penises/player_select closure-files=7 digest=1ff21290b13ce04f183c09e64a7910205019b76bfe8c32de7df9c117098fef5b
swrg_kit_integration/items/touhou_items/select closure-files=7 digest=e797957a80fbabd087819e34f1afda2e93014aaf9eb99dd5b85e886961365cc1
workingitems/awaken_tuntija_wands/electroblock/tick closure-files=7 digest=0b664d6e0e3677dcdc6c3c36263f7743a56946f89885425bad27a9de6ac9d273
workingitems/iceshard closure-files=15 digest=36ed0d33c85e47c71d59931f4bb70d131be1a0b47e87600c4b4239dda96ac2e4
other/liquidvoiddamage closure-files=2 digest=0a0dfc4906548142e666da8b9f5d35e2c821817ed8559f7bf389068bccf96edb
function/other/instants/select.mcfunction f94c2ff79adac83d4d409d301612b94aeb53652acd925086e20c8654171b56fd
function/other/instants/select_old.mcfunction 372dd17ad2ac40c31010a65b7a48461281faf1769789f0f9520c04c00b37cb22
function/other/instants/select2.mcfunction 254889e7307dffb382c0ddbad0f219233a28367b27fa5183f706a38693da2d84
function/players_2s.mcfunction abdb09f5ca07c52fa5adf335311d011d8ef3f643242d17d41905db521d9a1a5e
function/players_10t.mcfunction 0d43573e79c672783938b72793c5f5c912a4f5dbcdb691c05fc0de4fbf80795e
function/other/liquidvoiddamage.mcfunction f73ac66cc77c8125b3ba1279895f86beb5c9af3a9b283a9865f615659b370e01
function/other/liquidvoiddamage_recur.mcfunction d385a3ad7034f342bbcf99a57a0572ad76057ad7932ed9c849e6e7debd83cc16
loot_table/voidman.json 3eb5b1ad6c981fbbeea0247ea573a4bf1a87c370d6a6f66c7e7f18e084bc1b4a
loot_table/infinity_tuntija.json e54bebecd3be26f3d56278279e9651ec35af36a50ec7e0ae6d269596dce3cec8
loot_table/nexus_wire_black.json 29eedf4540aecd2dedab35c63f53d21cb876adeeafe710ba86b5f29aa2ff6629
loot_table/nexus_wire_blue.json 3617b04f2eefeecad1419a4b56cbf70f5f1f1d8a76f524b42ca12195fe358089
loot_table/nexus_wire_red.json ace7814e9ca55ea95b914e412fa1ed0047bb985f8fd6aa96f60aa0c37c91b0a7
loot_table/nexus_wire_white.json 14157209b9db32225bd0220547cc67c16185439dbae14a1e3cb6f4c14f18998e
loot_table/malevich_rnd.json 7bcc4a6a01ccd67ea9a07e908ed29df6306576a110a54c43b90ee680b83f6cd7
advancement/true_advancements/accesories/magnet.json 78eb4a96d7a29d9982a9ecb89b419690f2406c1ad99e63ebe7ae88b1003031ec
loot_table/gui/page/10000/1.json 327a0fe2504181ae3fb61bb4c0f33e7590e31ea464e8ffca65a872ba7de267cd
E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/item/Malevich.java 7669319f573463e92a3be011afd70b91caae74b9c0261bd6ed4fe58d8e2a2afc
src/main/java/me/skywars2/game/LbcElectro.java cf636209b951e9cab9ef6479390078cc432540236837da2f535c9001751c842a
src/main/java/me/skywars2/game/LbcIceMaterials.java 504dc2ded886cec1b00cab8a5765adbc010fe08679420332032e25bdc595bc22
src/main/java/me/skywars2/game/LbcMagic.java ea98c36afe09d77712fc4f04cbb1f4c837c5594bf9e74907f9b3136fa5ffb3bb
src/main/java/me/skywars2/game/LbcLoadouts.java aca0e966dfb0168b4849e665c5f3456eb9524e547f517c7f47320f47ce03c2db
RP/assets/minecraft/lang/en_us.json 7971188ae0d2e5f16f7253c6495168650451599e2826eeb46f7cfcb4a0e507f7
RP/assets/minecraft/lang/ru_ru.json 9ba24ccecc189950fe970f525ff45b6e40df5b375fabd333cfa6786ca8dac2fc
RP/assets/lbc/items/liquidabyss.json 70754effa433a67f6e399f67c7dd79d3dacc9116e3181204b1c2363f70fe08ae
RP/assets/lbc/models/item/liquidabyss.json 0af9b224ac135b1111c1b043aa966fb17d456f5adbefbd75b995137baa1808b5
```

Frozen preparation. No implementation/qualification or physical client/audio observation is claimed.
