# Reactive inventory artifacts and Sand Amulet - 27 October 2026

Read-only source preparation; only this new note written. S=`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`. Paths below relative to S. No code/build/server/git/RP mutations. Source command text inspected, legacy runtime unobserved. Scope is mystical2/current+old and gaara/current+old only, not another audit or port of mystical1 passive artifacts.

## Native gaps and reusable boundaries

`LbcMagic.artifact():384-395` recognizes exact mystical1 DATA constants only; inventory refresh/bitset, suppression, cleansing, elemental/liquid support are already live. It does NOT implement mystical2 accepted-hit callbacks. `LbcSkills.artifactIndex:139-143` maps mystical2 types1..6 to descriptions artifact_9..14, but this is Looper explanation only, not an effect producer. No live Java gaara/gaara_old/mystical_old item match was found. Thus all six current reactive types, their old reactive variants and both Sand Amulet hit entries are real missing entrypoints. Do not replace already-functional mystical1 services.

Native reusable mechanisms: ItemEffectLevels/native addEffect for effects and signed layers; existing food_count and mana APIs; LbcIndirectKills for poison/wither/hunger, movement/terrain/falling-sand and deferred deaths; resident native world mutation/falling-block tracking; shared Time Stop, role/ally/Greed admission and lifecycle. LbcMagic passive inventory cache is useful infrastructure but does not imply every reactive type can become an existing bit. No exact native Sand Amulet formation producer was established; generic terrain/falling blocks are reuse primitives, not proof of item support.

## Accepted-hit inventory admission

`advancement/gowno.json` accepts unblocked non-arrow_only player_hurt_entity, victim!=interaction, owner outside nexus/imprinted -> items/swords. Unlike sword flags, these are inventory predicates; holding any weapon can trigger them. Source Inventory NBT covers real inventory, not arbitrary menu cursor/crafting storage; preserve explicitly approved native inventory scope separately. Duplicate copies of one type do not multiply a matching execute; distinct types can run sequentially and all owned families may proc on the same event.

`function/items/swords.mcfunction:15-19`: current mystical2 rejects nearby nonspectator nomagic_active within20 and stopper_magic marker20; old mystical_old2 no suppression check. Current gaara1 adds same suppression gates AND requires any !spectator HurtTime10 entity within10, without teammate restriction for this admission; old gaara_old1 triggers every accepted event. Native shared role/ally/Time Stop and inherited owner policy supersede unsafe source gaps. No item-specific mana payment, charge, private cooldown or new RMB input.

## Current mystical2 all six types

`other/artifact/ondamage.mcfunction:1-14` executes at attacking owner. Owner !spectator required per current branch; fake *tempt lbcID2 gets owner's team. Types checked independently against inventory.

|Type/helper|Exact action and feedback|
|---|---|
|1 vampirism:1-8|self regeneration2 seconds amp2 hidden; saturation argument2 amp4 hidden (**instant effect duration2 TICKS**, not40). Enemy !spectator entities distance.01..7 HurtTime10 receive wither2s amp4 hidden and hunger2s amp99 hidden. food_count+1;5 heart normal y1 spread.5/1/.5 speed0; cat.hiss master globally volume1/pitch2.|
|2 ondamage:5|Enemy !spectator entities .01..8 HurtTime10 receive poison20s amp4, default visible particles/icons. No extra feedback/payment.|
|3 smoke:1-4|campfire_signal_smoke500 normal y1 spread5/5/5 speed.1 to players distance.01.. with NO maximum; nonspectator players .01..7 receive blindness5s amp0 hidden, **no team filter**; nonplayer #mobs .01..7 get follow_range BASE SET3, **permanent**, no team/spectator filter. Owner sees smoke10 normal y1 spread.3/.3/.3 speed.05.|
|4 speedup:1-5|For each enemy !spectator HurtTime10 entity .01..8, execute at target with target yaw/pitch0, then also owner yaw/pitch0: firework5 normal at local forward0/1/2/3 y1 spread.5/.7/.5 speed.02, teleport entity local forward4. Wall/headroom sweep absent source.|
|5 ondamage:12|Each enemy !spectator HurtTime10 entity .01..7 teleports in place with yaw+180, pitch unchanged; no new effects/feedback.|
|6 reducecooldown:1-3|self mana ADD1000, NOT cooldown reduction and no max check in helper; witch.ambient master globally1/1; witch20 normal y1 spread.5/1/.5 speed.01.|

Native ordinary mana item-cap rules must determine overflow behavior; do not globally widen caps or silently clamp to normal max contrary to already-approved signed/add behavior. Source helper name reducecooldown is misleading. Types1/2/4/5 depend on nearby recent HurtTime scans rather than exact event victim: requires existing pending author selection choice. Type3's permanent hostile/allied mob follow_range base mutation and teamless blind need explicit decision; native technical/allied-player guards remain default. Type4 teleport-through-walls is suspicious, not authorization for arbitrary unsafe native movement. No custom timer/HUD is defined for mystical2 effects; normal effect expiry and existing mana/food_count HUD apply. Type3 base mutation has no timer/restoration anywhere in this closure.

## Old mystical_old2 - different six-type semantics

`other/artifact/ondamage_old.mcfunction:1-8`, inventory match only, no branch !spectator or magic gate:

|Type|Exact old behavior|
|---|---|
|1|self regeneration1s amp1 hidden; saturation argument1 amp1 hidden =1 instant-effect tick; no enemy curse, food_count or feedback|
|2|self strength2s amp0 hidden, not poison|
|3|campfire_signal_smoke200 FORCE y1 spread.3/.3/.3 speed.2 to players distance2.. no maximum; no blindness/follow-range mutation|
|4|owner eye-anchored forward3 teleport with current pitch; then all nearby8 nonplayer/nonarmorstand/non-area_effect_cloud entities teleport forward3 in THEIR current pose, feet anchor. No team, living, HurtTime or spectator filter|
|5|ALL entities within8 including owner receive wither4s amp10 hidden; source no team/type filter|
|6|ALL entities within8 including owner receive regeneration4s amp2 hidden, not mana gain|

Do not substitute current helpers or alias old variants. Native ordinary effect/instant duration rules and shared owner attribution apply; legacy missing owner is a migration gap. No old timer/HUD beyond ordinary native effects; potentially recursive non-arrow consequences need established producer-recursion policy.

## Sand Amulet current - complete helper closure

`workingitems/gaara1.mcfunction:1-9`: inclusive random1..3 once, one branch, anchored eyes; afterwards TWO block.sand.place master sounds globally volume1/pitch1. Random is not tied to an event victim. Each branch reads current owner position/yaw and helper-specific pitch rules.

|Branch|Complete action|
|---|---|
|1 gaara1_1:1-14|14 native falling sand blocks, pitch flattened0, Y owner+6; forward2.5 has left offsets+/-1 (no center); forward3.5 offsets-2..2; forward4.5 offsets-3..3. Time1, DropItem0, HurtEntities1, FallHurtMax5, fall_distance5, FallHurtAmount5, Motion(0,-5,0). Initial yaw of owner determines rows; no owner field or magic tag. Source native falling-block physics, contact damage, placement and normal lifetime apply, not a hand-drawn projectile.|
|2 gaara1_2:1-4|Sandstone strict fill owner +/-8 X/Z and Y-1..+4 (17x6x17), then AIR strict fill +/-7 X/Z and Y0..+4 (15x5x15): hollow roofless interior, floor and perimeter retained. No destroy/drop mode. If any enemy nonspectator player .01..8 present, grant accesories/sand_amulet.|
|3 gaara1_3:1-3|At every enemy !spectator #mobs distance2.2..10 run31. Independently if any enemy !spectator #mobs .01..2.2, run32 once at owner. Boundary2.2 can satisfy BOTH.|

31:1-3 at each distant target fills single column Y0..3 RED_SAND DESTROY, support Y-1 CHISELED_RED_SANDSTONE DESTROY, summons marker at target data.lifetime210 with tags slowraycast/sand_blow/magic. No owner/team or other tag is copied. Native destroy drops plus support/fall causes must inherit original owner.

32:1-5 at owner first RED_SANDSTONE replace #minecraft:solid strict cube +/-4 (9x9x9); then RED_SAND replace #solid strict box +/-2 X/Z, Y0..3 (5x4x5). Enemy !spectator #mobs .01..5 as each target (WITHOUT at target) calls321: blindness5s amp0, damage5 arrow by nearest nonspectator player, sand.break master globally1/1 at retained OWNER command position. This nearest-player damage attribution is not necessarily original caster and must be repaired. Then falling_dust sand100 normal at owner y1 spread.5/.5/.5 speed.5. Friendly mobs remain eligible under established native rules; technical/allied-player defaults apply.

### Delayed sand marker ambiguity

`raycastselector_other.mcfunction:18` invokes other/sand_blow only at Lifetime100. That helper:1-2 chooses small fill if current cell #airs, big otherwise; small clears sameXZ Y0..+3 AIR DESTROY; big clears sameXZ **absolute Y0 through relative+3** AIR DESTROY, then kills marker. This is a potentially huge destroy column when target aboveground, not local +/-3. BOTH independent block conditions can run if first fill changes the condition? First small creates air so second unlessair false; big is chosen when initial nonair.

However gaara1_31 marker tags omit `other`, while central raycastselector routes other selector only for tagother. No other producer/tag repair for sand_blow found. Therefore this marker's intended age100 cleanup path appears **unreachable by its literal tags**; generic data.lifetime210 still discards it. Do not claim terrain cleanup actually occurs at100 without resolving this source bug. Native family port requires author decision on missing routing tag and big absoluteY0 clear; owner-scoped native delayed tasks should not reproduce global unowned markers.

## Old Sand Amulet

`gaara1_old.mcfunction`:16 falling sand blocks at owner local up5 with current pitch (dispatcher feet anchor), world/no explicit motion; forward1.5 center, forward2.5 offsets-1..1, forward3.5 offsets-2..2, forward4.5 offsets-3..3. Each Time1/DropItem0/HurtEntities1/FallHurtMax5/fall_distance5/FallHurtAmount2. Then SEVEN identical block.sand.place master sounds to players within15, volume10/pitch1. No random walls/red cages, no current helper2 unlock. Native physics and inherited contact/placement/drop/support attribution are required; source sets no owner/tag/custom age.

## Loot, Looper, acquisition and advancement

Current artifact.json six mystical2 types are repeating_command_block/count1/model lbc:mysticalartifact/tier_u; name mystical_artifact #C28CFF italicfalse; lore row type+accessory aqua italicfalse then mystical_artifact.lore red italicfalse. Flags transformable1/accessory1/unresolved_artifact1/mystical2/typeN. Weight10 all; qualities type1/2/3=10,4=0,5/6=-5. Old artifact_old.json same weights/qualities/base/model/name but no tier or type row; only mystical_old2/typeN flags, lore mysterious red single row. No new attributes/durability override.

Current gaara leaf transformables_accessory:1-70 repeating_command_block/count1/weight10/quality10, model lbc:gaara/tier_c; sand_amulet gold italicfalse; type+accessory WHITE italicfalse row then sand_amulet.lore.1/.2/.3 yellow italicfalse; flags transformable1/accessory1/gaara1. Legacy luckyblock:1530-1572 samebase/model/name no tier/type row; lore .1 + sand_amulet_old.lore.2 + .3 yellow italicfalse; only gaara_old1.

Current artifact table is referenced by transformables_accessory:464 and485; legacy luckyblock:3197 references artifact_old; amulet is current accessory transform/reforge pool or legacy luckyblock leaf. No dedicated ingredient craft was found for these flags; source kit3 grants `kit/gaara`, independently inspected: minecraft:repeating_command_block x1, minecraft:sand x64, minecraft:red_sand x64. Its sand_pot carries gaara_tikva1, not gaara1; it is a different active consumable and not an acquisition route for the passive amulet. Sand advancement task/impossible parent save_platform has rewards gain/kit; producer is current branch2 enemy-player check above. Do not activate kit3 merely by importing the passive amulet.

Looper already maps mystical2 -> artifact_9..14 via LbcSkills; exact source helper replaces explanation lore and removes unresolved flag. Reuse its existing current native explanation rather than re-porting it as combat. No family-specific additional score/HUD/timer beyond effects/food_count/mana and delayed marker.

## Independent viable work versus deferred choices

Current mystical2 type6 is smallest independent no-HurtTime branch: native mana signed add1000 + exact feedback, inventory matching/suppression. Old types1/2/3/6 have straightforward independent self/buff/visual semantics but oldtype6 area+allies must respect existing author policy. Entire six-type variants should NOT be silently enabled partially. Current Sand branches1/2 are explicit spatial producers not dependent on exact HurtTime recipient (entry still has recent-hurt guard); source delayed red-sand routing/absoluteY0 and nearest-player attribution need decisions before complete family release. Types3/4 smoke/follow-range/unsafe teleports need author clarification; preserve native allied/role, Time Stop and all ownership requirements. Source no-question subset is research staging, not authorization for partial gameplay.

## SHA256 source ledger

- `advancement/gowno.json` `b65994157053db257f20ffdd9304e2a0efb204114d2b73e4eb7da9f58b9a2e2f`
- `function/items/swords.mcfunction` `36162bd3115e12c5d8b0626f619d0eb88b408505282519a191ea7d1bdeb33f1f`
- `function/other/artifact/ondamage.mcfunction` `86d606296e35ad5419e97d540b2569c235eca767e263ae7c025a92099038c44a`
- `function/other/artifact/ondamage_old.mcfunction` `25a8f628528c4ed6bf0be0099fb9de1fd86075e48b9b2121bed915e6ddbb0195`
- `function/other/artifact/vampirism.mcfunction` `b54ef83dc3230e114342049c4f2482955c9dd95c4a6e59e9c02bba764dee6a3f`
- `function/other/artifact/smoke.mcfunction` `e966bc3831bc8210c48a9c44572a33efa8db0b0cefb8b712e7651d5a93a5ecb0`
- `function/other/artifact/speedup.mcfunction` `251643f7a1068af0509809ce7b9dd3abf1838426681f8d3036a6d242fae0f77a`
- `function/other/artifact/reducecooldown.mcfunction` `93357776972f92b87b3234d1264c17aec85b7e1ce6a824224cbf2900c12830e8`
- `function/workingitems/gaara1.mcfunction` `8fc1abceb031b638a80002f56bf5f1d2abad3dd3e21e9c4a20bf29f5b48fd3e5`
- `function/workingitems/gaara1_old.mcfunction` `c6157630901036924b45fbdb8988297d0af19689424f52fb590ed46c857dfcbf`
- `function/workingitems/gaara1_1.mcfunction` `4d51d16b90893a5b83903223a48c16bb721955a09809059121844ffc277ae886`
- `function/workingitems/gaara1_2.mcfunction` `8bf07c82b40695dc39898a356e3586ccb5d9d235789db75a1e11206bff191848`
- `function/workingitems/gaara1_3.mcfunction` `dcf6b9d0df54b11d96505e237752e2a5c4f7139bb0fa5a930f47b0eb1d64657e`
- `function/workingitems/gaara1_31.mcfunction` `2ff21a85990eb87f18329726058abd5380fe960c55cd0822ae7bf6570dd4c360`
- `function/workingitems/gaara1_32.mcfunction` `b376e03821051db00cae94ffc7f9619fd8dc0642087bfcc7be187ddc987f0b9b`
- `function/workingitems/gaara1_321.mcfunction` `b835ad8e4ecbfccf5efde68f6be668b58be2241cc82d7339d46eb1f0a23f574e`
- `function/other/sand_blow.mcfunction` `00c721418541dc5857ae51ec82667210c3ae613adefa19a64823452b6b40f8c9`
- `function/other/sand_blow_fill_small.mcfunction` `d2e7bb7e3c82c17e307c55d7ae57e49deb8504742944d2b7931ffe1341837224`
- `function/other/sand_blow_fill_big.mcfunction` `415722c35f32107c7b220a54fb3c39916e3add8ffbf5fada1ce3ac668f26a0ef`
- `function/raycastselector.mcfunction` `7b4951e7c1ac5aae5fd606e2d8ea53db0bbd8f9125fd77cd1ff4b0836f0c62b6`
- `function/raycastselector_other.mcfunction` `ee7896f9f8b1c7003ce732487f7aa6f0b75b07edffad6eb307fa1ba269679dfe`
- `loot_table/artifact.json` `04783e32c1a8935a3849a493fda7b885967835bf8749068965cb09cfc84052db`
- `loot_table/artifact_old.json` `5e75b1c35d90eacfb847757d6533f4cafc38919923de2d9e8003f2864bf1bbfd`
- `loot_table/transformables_accessory.json` `3792e70b859e718276c7000dcc81bd055462cf8600b5833389efd2a5f53e1524`
- `loot_table/legacy_luckyblock.json` `bd7a1a5aa859362d96e93b7261ed160b48686829ffcd8be33a24daa93818ef01`
- `advancement/true_advancements/accesories/sand_amulet.json` `535634e98576877ff63c92c72805a02c80b2d622f5bb033f2ceb24dd3ffc5c74`

## Exact read-only RP text/model evidence

`en_us` SHA256 `7971188ae0d2e5f16f7253c6495168650451599e2826eeb46f7cfcb4a0e507f7`

```json
{
  "mystical_artifact": "Mystical Artifact",
  "mystical_artifact.lore": "Unknown effect",
  "artifact_9": "Vampirism",
  "artifact_10": "Poisonous hit",
  "artifact_11": "Blinding smoke on hit",
  "artifact_12": "Teleport everyone forward on hit",
  "artifact_13": "Rotate target on hit",
  "artifact_14": "+1000 mana on hit",
  "sand_amulet": "Sand spirit",
  "sand_amulet.lore.1": "When in inventory:",
  "sand_amulet.lore.2": "Gives Durability Effect",
  "sand_amulet.lore.3": "Sprinkles the enemy with sand when hit",
  "sand_amulet_old.lore.2": "Resistance 2"
}
```

`ru_ru` SHA256 `9ba24ccecc189950fe970f525ff45b6e40df5b375fabd333cfa6786ca8dac2fc`

```json
{
  "mystical_artifact": "Мистический Артефакт",
  "mystical_artifact.lore": "Неизвестный эффект",
  "artifact_9": "Вампиризм",
  "artifact_10": "Ядовитый удар",
  "artifact_11": "Ослепляющий дымовой удар",
  "artifact_12": "Телепортация всего вперёд при ударе",
  "artifact_13": "Переворачивающий удар",
  "artifact_14": "Ведьма: +1000 маны при ударе",
  "sand_amulet": "Дух песка",
  "sand_amulet.lore.1": "Когда в инвентаре:",
  "sand_amulet.lore.2": "Даёт эффект Сопротивления",
  "sand_amulet.lore.3": "Обсыпает врага песком при ударе",
  "sand_amulet_old.lore.2": "Сопротивление 2"
}
```

`assets/lbc/items/mysticalartifact.json` SHA256 `de862c559b494df5271be0f9a59a4c69f3804d56a0b574d33298800aa246a562`; {     "model": {         "type": "model",         "model": "lbc:item/mysticalartifact"     } }

`assets/lbc/models/item/mysticalartifact.json` SHA256 `f8bfd2b5ea27191c6f1553cbb596956645298949df83be2c179678f0db37517b`; {   "parent": "minecraft:item/generated",   "textures": {     "layer0": "minecraft:item/mysticalartifact"   } }

`assets/lbc/items/gaara.json` SHA256 `a0229c62902e3a6687b3fe9badb1e757942fa096a7bbfd2b0fabb0b0784da79b`; {     "model": {         "type": "model",         "model": "lbc:item/gaara"     } }

`assets/lbc/models/item/gaara.json` SHA256 `e87bbc8fd5cdf723db3d7ac9bbdb2cb37c08315d83e28065c4684ec8b4cd58e1`; {   "parent": "minecraft:item/generated",   "textures": { "layer0": "minecraft:item/gaara"   } }


Kit exclusion evidence: `loot_table/kit/gaara.json` SHA256 `2a085cf48a80520d286b7ee9613d072635dcf20ce7f4b5176d705569396f9cc1`. Separate gaara_tikva helper fills +/-10 sand replace tannerkivi, self levitation2s amp15/slow_falling20s amp0, consumes its token; not part of passive family closure.

## Sand Amulet inventory Resistance - completion correction

The passive is a separate genuine native gap; the hit-helper closure alone is NOT the whole amulet. Full literal gaara/gaara_old search found current inventory activation in `function/items/changed_inventory/sel.mcfunction:1,3,23`: any container.* or weapon.* item with gaara1 adds gaara_active; absence from BOTH removes it. Armor.* is not checked. `advancement/penis.json` inventory_changed reward calls this selector; `resets.mcfunction:127` refreshes it too. Duplicate current copies do not multiply the tag/passive. No gaara_old1 passive match or gaara_old_active producer was found: old amulet only has the audited hit volley, despite shared flavor text.

`function/players_1s.mcfunction:1` at gaara_active player, unless stopper_magic MARKER within20, calls `workingitems/gaara.mcfunction:1-2`: self RESISTANCE duration2 seconds (=40 native ticks), amplifier0, hideParticles true; then5 forced falling_dust sand particles at owner Y+.81, spread.3/.5/.3, speed1. Refreshes once per scheduled second, providing overlapping40-tick grants while carried. No nomagic_active player-aura check, mana cost, HurtTime requirement or Greed filter exists in this passive branch. Do not import hit proc's broader antimagic admission into this source passive without identifying policy precedence.

Cadence chain: tick1s:1 self-schedules1s; :11 iterates #mobs outside nexus/imprinted -> mobs_1s:7 invokes players_1s only for nonspectator players. This scheduled path has no global Time Stop guard and gaara branch has no time_stopped check; source can refresh during a stop. Native established Time Stop/pause and suppression policy remains authoritative, and ordinary native Resistance layers/timers determine expiry. Removing the last gaara item removes gaara_active on inventory refresh; source does NOT explicitly clear Resistance, so existing grant naturally expires (up to40 ticks). No dedicated passive HUD/score or cleanup besides tag refresh and ordinary effects. No permanent Resistance attribute rewrite.

`LbcMagic.artifact` exact DATA remains mystical1 only, with no gaara match or generic Resistance grant. LbcSkills explanation does not fill this gap. A complete future current Sand Amulet implementation must include inventory scope/tag-equivalent lifecycle,1-second Resistance refresh and dust feedback as well as all hit branches; legacy gaara_old must not receive this current-only passive by aliasing.

Additional original-byte SHA256:

- `function/items/changed_inventory/sel.mcfunction` `0b9c6a85afe888ab288ccb32822c7aece0cb4903c2ebbee37bf24e48ea99e7d5`
- `advancement/penis.json` `c46cb712f96397af9b4b1d5a736176729e6cbcc518cf932fc54a76b5394f301f`
- `function/players_1s.mcfunction` `4e354224511c16f697d04089b43875b28547a24bec54c0e6190b56b4dcdff905`
- `function/workingitems/gaara.mcfunction` `52be89f1e47e466ee882644b3d1e824627ce6dd7a518e4f87f608cbfac924e5f`
- `function/tick1s.mcfunction` `da799ec32ca58bb89f77cd22e84e85ecae6e9eebccce5e032851aa9bc799d1b4`
- `function/mobs_1s.mcfunction` `d081be87d3841977d6067f2a3d2e48fabb79c4af828a8ad8c5608a6386ff2638`
