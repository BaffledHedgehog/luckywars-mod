# Infinite Quiver and Regeneration Belt source preparation - 2026-10-27

Read-only static audit; no build, runtime qualification, server, RP or implementation changes. Paths below are relative to legacy `world/datapacksdisabled/LBC/data/lbc` unless prefixed Paper/native/RP. Source text is not proof of observed historical runtime. Current native role, owner attribution, Time Stop, signed-effect/item-cap and allied-player protection rules remain authoritative.

## Outcome and complete batch boundary

Neither current accessory is implemented merely because its vanilla leaf and description exist. Belt has a registered Paper consumer, not an active datapack healing implementation. Quiver depends on the entire generated-arrow pool: shipping ordinary arrows while leaving the special entries inert would be incomplete. Recommended smaller coherent next family is **current Regeneration Belt plus its original advancement/reward**, after recording the concrete legacy inventory-count and global-return choices below. Old Belt is a separate legacy-mode potion leaf, not a current-item alias. Quiver is a larger complete arrow-engine batch, not an incidental timer addition.

## Current and old leaves / acquisition

| Leaf | Exact source contract |
|---|---|
| Current Belt | `loot_table/transformables_accessory.json:132-194`, entry2 (zero based), weight10 quality9, count1 repeating_command_block; model `lbc:regenerationbelt`, tooltip `lbc:tier_c`; name regeneration_belt color #FF3BB7 italic false. Lore type+accessory white, regeneration_belt.lore.1/.2 #FFBFBF, effect_can_stack white; all italic false. |
| Belt payload | transformable1, accessory1, heall1; PublicBukkitValues lws:regen nested value0.04d, operation add, slot inventory, stackable true, id regenbelt1242. No potion effect component. |
| Old Belt | `loot_table/legacy_luckyblock.json:3195-3252`, entry128: same carrier/model/name and two pink lore rows; protection1 hidden enchantment; only heall_old1, no PDC regen, accessory or transformable flags. |
| Quiver | `loot_table/transformables_accessory.json:400-459`, entry6, weight10 quality8 count1 repeating_command_block; model lbc:quiver tier_b; name infinite_quiver italicfalse; lore type+accessory, infinite_quiver.lore, effect_can_stack white italicfalse; transformable1 accessory1 arrowgenerator1. No old arrowgenerator leaf found by whole legacy flag search. |

Original ordinary routes: `loot_table/lucky_items.json:863,886` and `lucky_items_high_quality.json:221,244` reference transformables_accessory; transformables_accessory_all:8 references it and is referenced by random_item:8 /27_acessories:8. Keep existing complete raw pool and reforge semantics; do not enable an unresolved whole pool simply to add these items. No dedicated craft recipe/extra input found by whole heall/arrowgenerator flag search. Existing native build.gradle:324,442 includes the accessory tables; this alone supplies no gameplay. `arrows.json` is not explicitly included by the inspected build configuration, so packaging its entire exact table and stand-arrow reference requires deliberate verification.

## Quiver cadence and admission

`tick15s:1,4` reschedules every15s (300 game ticks), all nonspectator players except nexus/imprinted, at owner. `players_15s:1-2` has TWO independent if-items branches: container.* and weapon.*; each excludes marker stopper_magic within spherical20. A main-hand copy is also a container item, so both branches can run. Do not silently merge these calls without a recorded fidelity decision. No mana, cooldown, consumption, active-input stun check or nomagic_active gate appears here.

`other/arrowgive_pre:1-4` uses `/clear ... 0` to count ALL matching stack units into shared fake-player arrowgens, spawns lbc:arrows at owner, detects Age0 stand_arrow dropped item within0.01 to grant infinite_quiver, then plays zombie.infect master @a volume1 pitch2 minVolume0. This is source command range/dimension admission, not unconditional all-client sound. Count uses full clear scope (inventory/equipment, personal craft and cursor), unlike trigger container/hands. `other/arrowgive` decrements/re-enters itself but is not the invoked producer; do not grant one arrow per helper recursion.

One pool in `loot_table/arrows.json`, rolls score arrowgens / lbc.math; 29 entries, total weight281 (28 entries weight10; Stand weight1). Every roll independently chooses an entry, then its own count. Two ordinary-arrow and two spectral-arrow entries are intentional literal duplicates.

| Index | Produced stack / special payload | Count / weight |
|---|---|---|
|0..8|tipped potion slowness,long_slowness,weakness,long_weakness,harming,strong_harming,poison,strong_poison,long_poison|uniform1..8 /10 each|
|9,11|ordinary arrow|uniform1..8 /10 each|
|10,14|spectral arrow|uniform1..8 /10 each|
|12|strong_arrow tipped color6710881 tier_a|uniform1..8 /10|
|13|weak_arrow tipped color6710882 tier_f|uniform1..8 /10|
|15|poison_arrow color2883328 tier_d; poison160t amp2 + wither40t amp2 hidden|uniform1..8 /10|
|16|reactive_arrow color6710883 tier_d + reactive_arrow.lore|uniform1..8 /10|
|17|explosive_arrow color16746496 tier_b; raid_omen20t amp101 hidden|uniform1..8 /10|
|18|evoker_arrow color6184020 tier_b; raid_omen20t amp102 hidden, two lore rows|uniform1..8 /10|
|19|paralyzing_arrow color65493 tier_d; slowness/mining_fatigue40t amp127 hidden|uniform1..8 /10|
|20|electric_arrow color52479 tier_b; raid_omen20t amp103 hidden, electric_arrow.lore|uniform1..8 /10|
|21|lightshoot_arrow color16777215 tier_a; raid_omen400t amp112|1 /10|
|22|kiuaskivi_arrow color16763161 tier_a; raid_omen300t amp111 visible|uniform1..3 /10|
|23|trapping_arrow color2236962 tier_b; raid_omen300t amp113|uniform1..4 /10|
|24|reference lbc:stand_arrow|referenced leaf /1|
|25..28|tipped potion oozing,infested,wind_charged,weaving|uniform1..8 /10 each|

All translated custom names are italicfalse. Potion tooltip is explicitly hidden for raid-omen special entries. Exact component/lore order remains authoritative in the hashed JSON; do not reconstruct presentation from the labels above.

## Generated-arrow full behavior closure and native gaps

`projectiles/pre:1-3` recognizes colors6710881/2/3 on count1 arrow item: damage5 / damage1 / NoGravity true, respectively. Its data merge replaces tags with updated for these three. `pre:16-17` runs inair for raid_omen arrows, then prein when grounded. `projectiles/arrows/inair:1-6` reads amp, increments Lifetime, executes Kiuas small terrain conversion for111, emits full-strength delayed light/trapper marker each in-air tick for112/113, kills arrow at Lifetime300. These are persistent flight effects, not only living-hit effects.

`projectiles/prein` -> `projectiles/arrows` -> `arrows_f` performs once-only ground action and kills arrow. Living hits instead transfer raid_omen, `mobs_tick:1` -> arrows2 reads remaining duration (2 becomes1), adds to named counter, then clears carrier effect. `arrows2:25-27,35-37,55-60`: mappings101 small_explosion,102 evoker,103 shock,111 kiuaskivi,112 lightarrow,113 evokerarrow; skill19 nodebuff clearing precedes lucky-effect tags. Source ownership is often absent, but native must retain original shooter through counters, entities, terrain and later kills.

| Transport | Complete source action / target semantics | Native live boundary |
|---|---|---|
|101|arrows_f:1 radius1 instant silent/invulnerable creeper, suppressed by mystical1/type2 player<=12 or stopper<=20. effects/effect_small_explosion repeats pulse while counter positive; decrement despite suppression.|LbcBabylonEffects:22-25,46-55 accepts only managed current-epoch Babylon arrows. It is NOT an ordinary tipped-arrow handler. Native radius1 ownership service reusable after explicit producer integration.|
|102|arrows_f:2-3 at nonplayers<=8 with DeathTime0 and nonspec players<=8, unless stopper_magic20; nine fangs per target. effect_evoker repeats all entities<=8 except fangs/spectators (broader than ground). Nine positions: center, diagonal +/-1, cardinal +/-1. No explicit source fang owner.|No amp102/counter producer in production search. Existing wand/fang services do not establish this closure. Default allied-player protection and roles supersede raw wide selector.|
|103|arrows_f:4 / effect_shock unless stopper20: nonspec players<=5 get nausea+blindness60t amp1 hidden; all nonspec entities<=5 get speed60t amp1 and wither60t amp10 hidden.|No amp103 live handler. ChainLightningStatus:31 is amp124, a different family; Shock/turn curses are not this pulse.|
|111|inair Kiuas clears convertible blocks in3x2x3 strict then fire replacements and flame3 force; ground kiuaskivi2arr1 clears +/-15 cube strict, fire, then +/-1 lava; effect_kiuaskivi runs timed variant.|LbcKiuas:64-67 + LbcColorEffectsMixin:55-56 accept raid111 with source/owner. Native shared Kiuas is genuine reuse, but quiver flight/ground producer still needs explicit integration audit.|
|112|inair emits marker every tick; living timer chance25 per tick emits marker. Markers slowraycast/other/magic lifetime200. inairtrail end_rod1 normal each tick, dust_transition10 force at50 white-yellow,100 yellow-red,150 red-black; at199 invokes full TNT/lightning column, then lifetime200 cleanup.|No effect_lightarrow/amp112 producer found. Do not substitute Babylon lightning101/109 service; ground arrows_f has no112 action.|
|113|inair emits marker every tick; living timer chance35 emits marker lifetime400. inairtrappertrail requires chance25, any player<=80, age>=120; evoker_rand summons fang if #mobs nonspec/non-strict within5, optionally faces nearest #minecraft:mobs<=15 and moves1 if none<=1.5 +chance50; then independently integer-snaps each coordinate and adds uniform-1..1; kills at390.|No effect_evokerarrow/amp113 producer found. No113 ground action. Source #mobs resolves to minecraft:mobs, as does #minecraft:mobs; preserve the actual tag membership rather than replacing it with arbitrary LivingEntity membership.|
|Stand|lbc:stand_arrow original referenced leaf plus its established Stand item mechanics.|LbcStand:129-130 registered, isArrow:174 accepts stand_arrow flag; preserve exact original loot identity, not a generic ordinary arrow.|

`effects/tick` excludes sin_greed_active; timers decrement and retain have_lucky_effects tag. `tick_not_timestopped:3` dispatches slowraycasters; raycastselector:1 increments age, :8 dispatches other, :12-13 kills at data.lifetime. Ground/flight suppression uses exact stopper vs stopper_magic distinctions above. No native all-family timer hooks were found for102/103/112/113 after searching score aliases, potion amps, names and colors across production Java. Vanilla potion/arrow/spectral behavior remains vanilla; native signed layers and caps still apply.

`inairtrail2:1-45` full column is finite but large: TNT at Y0,3,-1,-2,-3,-4,-5,-7,-9,-11,-13,-15,-17,-19,-21,-23,-25,-27,-29,-31,-33,-35,-37,-39,-41,-43,-45; lightning at0,3,-1,-2,-3,-4,-5,-6,-7,-9,-13,-17,-21,-25,-33,-45. weak_v returns after first TNT+lightning; generated arrow markers omit weak_v, so full column applies. Native blast/fang/lightning ownership, loaded terrain and team policy are necessary; never reintroduce invisible creeper crutches or unowned explosions.

## Belt actual registered Paper consumer

Paper paths below are relative to server source root, not the datapack.
`plugins/luckywars/src/main/java/me/luckywars/SigmaWarsMain.java:130-135` constructs Regeneration, registers listener and passes command instance; disable calls its cleanup. This proves registered source wiring, not that a particular historical binary actually ran. No corresponding regen consumer was found in `mods/luckywarsignite/src` or native production Java by PDC/id/heall searches.

`plugins/luckywars/src/main/java/me/luckywars/command/Regeneration.java`:

| Lines | Exact contract |
|---|---|
|100-136|Constructs namespace aliases (plugin, lws, lucky_wars/plain fields) and starts task.|
|140-188|Runs every tick, first delay1. Cached positive player regen only; read actual MAX_HEALTH, setHealth(min(health+regen,max)). Flat HP/tick, NOT fraction of max and NOT Regen potion. No absorption alteration, cost, particle or sound.|
|144-145,675+|Encountering any online player in minecraft:imprinted returns from entire runnable, skipping later players and the mob loop. Source ordering is Bukkit online iteration.|
|195-202|Suppressed by marker tag stopper_magic in world.getNearbyEntities AABB20 on all axes; no spherical distance test. No beacon-stopper/stopper/nomagic_active branch here.|
|205-232|Recompute starts0, applies active command modifiers sorted createdAt (SET/MULT/ADD), expires by wall clock, then item modifiers; caches final double. Belt-only result .04 times grouped counted units.|
|242-282|Gather mainhand, offhand; loop inventory.getContents through getSize excluding ONLY held hotbar slot; then separately four armor slots. Nonarmor groups first, armor groups second. Thus offhand can contribute twice, armor inventory-slot modifiers can contribute once through inventory and again through armor. Cursor/craft are never read.|
|300-384|Read nested PDC double/op/slot/byte stackable/id; inventory slot accepted for every gather slotKey. Same-id grouping uses first group's type/value; stackable adds stack.amount, nonstackable1. ADD applies value*count. Missing/invalid payload is ignored.|
|443-476,498-513|Join recalculates immediately; click/drag/swap/close/held/drop/pickup/Paper armor event queue recalculation next tick. Programmatic changes without these events may leave cached values until another recalculation.|
|479-495|EntityDeath/EntityRemove clears player/entity regen cache and command effects. No explicit respawn reconstruction in this class.|
|670-673|Disable cancels task. No own Time Stop/stun/Greed/nomagic checks in tick, no separate HUD counter.|

Belt effect1 = .04 HP each tick per ordinary counted unit: 0.8HP/second at20TPS. A64 stack adds2.56 HP/tick before max clamp. This is logical game-tick schedule, not wall-clock20TPS compensation. No native generic health-regeneration consumer matches this contract: `LbcMagic:914-921` adds a Regen potion for a different skill; SignedEffectMath's potion interval is different; mana regeneration is unrelated. Fractional native healing must preserve actual existing health/absorption/layer semantics and avoid healing dead/removed owners.

Datapack `players_5s:1-2` / `workingitems/regeneration_belt:1,9` merely counts clear-scope heall and grants advancement at score y<=-100000 and count>=10. All former heal recursion lines2-8 are comments. `player_speed:36` writes y from PosY*100 when not time_stopped_lol; check_pos_changes:9 can overwrite unscaled, so record **score condition** exactly rather than claiming a single universal coordinate threshold.

Old `other/new_game/legacy_tick_5s_player:1`, scheduled every5s by legacy_tick_5s:1,3: Inventory NBT contains repeating_command_block heall_old1 -> Regeneration20ticks amp2 hidden. Single presence effect, not stack-scaled; no stopper gate in that helper. Do not interpret current Belt PDC as old potion or silently enable old legacy game mode.

## Advancement and native reward boundary

Both original advancements are impossible task criteria, visible toast/chat, reward `swrg_kit_integration/gain/levelup`; Quiver parent devilstomach, Belt parent sand_amulet. Preserve original task IDs `lbc:true_advancements/accesories/infinite_quiver` / regeneration_belt. Source gate page8000 slots0/2 respectively (`gui/page/8000:11,13,120,122`; `gui/select/8000:7,9`), selected profile stores slot+1 via8000_prex_2. Native loadouts.json:116-129 currently includes Belt levelup id3/page8000/slot2 with max_health +.15 add_multiplied_total and exact original gate. Quiver slot0/id1 is absent from this native catalog; reward route must be audited/restored with its actual source attribute consumer, not invented. Unlock notification metadata exists for both; notification is not functional levelup implementation.

## Existing author presentation

- `infinite_quiver`: Infinite quiver
- `infinite_quiver.lore`: Creates random arrows every 15 seconds
- `regeneration_belt`: Regeneration belt
- `regeneration_belt.lore.1`: Increases the regeneration (in inventory)
- `regeneration_belt.lore.2`: The regeneration stacks! Works in any slot
English author language SHA256: `assets/minecraft/lang/en_us.json 7971188ae0d2e5f16f7253c6495168650451599e2826eeb46f7cfcb4a0e507f7`.

## Questions to record, not silently rewrite

1. Belt: preserve offhand/armor double-counting, or count each physical stack once? This materially changes .04 output. Preserve source slot trigger/count distinctions separately.
2. Belt: preserve whole-task imprinted return and event-stale cache, or make a native per-owner tick/recompute policy? Both are real source defects, not existing author approval to fix them. Death/respawn rebuild must be deliberate.
3. Quiver: preserve two independent container/hand procs and full clear-count scope? These are finite literal behavior, not evidence of observed duplicate grants.
4. Quiver: full112 TNT/lightning column and113 source tag-selection must retain terrain/ownership policy; existing default ally protection resolves friendly players without a new WILD request. No authorization exists for broad unowned harm.

No family was implemented or qualified by this audit. Belt is the next coherent small family after those exact decisions; Quiver requires full special-arrow producer/flight/ground/living-timer/cleanup/reward closure and full-pool qualification.

## SHA-256 ledger

Full hashes below bind inspected source and current native snapshots. Line references above use original files; JSON leaves remain authoritative for exact formatting/components.

```text
loot_table/arrows.json 1feb6e0b7ae6f30e31487bff7c2cfeb7ff6bf97353f9be7b4d6ccfcac1f8670f
loot_table/transformables_accessory.json 3792e70b859e718276c7000dcc81bd055462cf8600b5833389efd2a5f53e1524
loot_table/legacy_luckyblock.json bd7a1a5aa859362d96e93b7261ed160b48686829ffcd8be33a24daa93818ef01
function/tick15s.mcfunction 71066ab6c5e3474aaabf8260635548426134d9cb2d37fca57c1c093b14d72b02
function/tick5s.mcfunction c8c8d67a75fd72a6b10c54d6af30645641eb32e8257a7dfce6fb80a4953039c5
function/players_15s.mcfunction c05c8439938cae53e84ebb671eba52e3a3aacd40d9178671aba35b2dd8be4ef1
function/players_5s.mcfunction e7a99c77a03c1ae2d294bd3e3e9e5bfff971609a7543e799ffc7e1660f674c7b
function/other/arrowgive_pre.mcfunction e0b5fcd6846f9dc9711c11b07a8d6ccddbaab2fc633737cd22eea1778b06deb0
function/other/arrowgive.mcfunction c20d4e94ef9303b07f6ea0c26c0762c1a4a6cbcaab80e65f3ab2a549a4b219af
function/workingitems/regeneration_belt.mcfunction f021a0a31657378d56835ee0dfb954e4ada383fd101269f6f1c02363d98964c4
function/other/new_game/legacy_tick_5s_player.mcfunction 4dee97a29ac905b7982893681b11f6c204038b69f62d7a7e47982c23e579d8e0
function/projectiles/pre.mcfunction e4f47beebf2095493dfacb8318c03376947939b4a9cdc248835102032be74315
function/projectiles/prein.mcfunction d008be5bad4db7eb0baefd3c74536a5306d208af45daa2c7707eff4888ebfc4b
function/projectiles/arrows.mcfunction 54b915ff5256940ebe0376b0cb2dbb28290a4bdf7b435c86ce2dfc6d815bfcef
function/projectiles/arrows_f.mcfunction f8aa8b56be9b2583261c8404888336923c3d2b5069f11362dbd31a80de47a72b
function/projectiles/arrows2.mcfunction cb77e532d70439251d0d0046445a61d31fc487bd070abcc4b9354953d846d76f
function/projectiles/arrows2_nodebuff.mcfunction 1fff3f006bc2983304b9630b61f2422aefc974be17f0621a3f4bdb3654338cef
function/projectiles/arrows/inair.mcfunction 2e258c4d2454cfaac7991152b22608e70a70931429a3087c304d324639900557
function/projectiles/arrows/evoker.mcfunction 1723f5896b94e2bb02eeed4ca914668f4896a6508b7e19997b0fa58830156084
function/projectiles/arrows/shock.mcfunction 146d8940435d312e68107696c2bba765944810cadcac8b2fdb22407286218691
function/projectiles/arrows/inairtrail.mcfunction d001f93eeddb64024d215bb57eeff8a2f4a3e8ef5287c5ab46812a5126932857
function/projectiles/arrows/inairtrail2.mcfunction 8409f577b5bb4d0ce2e3a1c3678c2d1e9a105c4725a92f616470387d95deff98
function/projectiles/arrows/inairtrappertrail.mcfunction e7a6f9f5767875c623451a3f83859337f6152323b5d3bb710896137340261312
function/projectiles/arrows/evoker_rand.mcfunction d4afd0c7688589550449210190512c4ee74930b042f939e71b8a272c506b53f2
function/universal_pos_razb_1.mcfunction 0716dc1a2508560fdb886e249020f5280c829197450ac94eb3bda53dac0f18b6
function/raycastselector.mcfunction 7b4951e7c1ac5aae5fd606e2d8ea53db0bbd8f9125fd77cd1ff4b0836f0c62b6
function/raycastselector_other.mcfunction ee7896f9f8b1c7003ce732487f7aa6f0b75b07edffad6eb307fa1ba269679dfe
function/raycastselector_kills.mcfunction b9b983d5c0f1f8b2a8df8cd9012b3cba88d90f974801cb54c093461de7b0e070
function/tick_not_timestopped.mcfunction 55faddfdca07e3a486d0f826e9593969ae5fa05260cb2392c162569efa8cf5cc
function/items/arcanums/kiuaskivi2arr.mcfunction 6611b250cc7d14691a328f74612f6a9a7f3f47e455ccc0129da7f42f18a58fbd
function/items/arcanums/kiuaskivi2arr1.mcfunction ef25913081fb1122c859f4e74c8d4a8b9f1a9e91f980b974321ac7581e6f97b8
function/effects/tick.mcfunction 2c3777fe53bf6182953855996c1669227701c0fd6e9d2a950fe21d3dd8bfe2a6
advancement/true_advancements/accesories/infinite_quiver.json b3e91a67943f55b3d393f88e2e5c602a7ecc2b00df63828683ad061ebaa69519
advancement/true_advancements/accesories/regeneration_belt.json 274b5fc2d3f8fce2bd4aed1f133cad719101854067799a316c61efb4d338427d
function/effects/effect_small_explosion.mcfunction 99bf1a2f8594f1dc42739673c4b5e6d0bbd2ffe483085c4c5b7d100730dbd4f5
function/effects/effect_evoker.mcfunction cf237c5c2fc98e689fb12c60287824e698b008f424875896507fa9d5ae506285
function/effects/effect_shock.mcfunction ca4b82538f87dd792ce41054b774b3ed28b0fba87b7c4d17c99e1aef0b778a5a
function/effects/effect_kiuaskivi.mcfunction 95447159cc32999078a6efe7b96e276edd387e7c8f4911ff3692572c08592821
function/effects/effect_lightarrow.mcfunction c307aa2154f5b72788a66abfd9179c5087f1874ea1676383216e7a48ae9a8a4c
function/effects/effect_evokerarrow.mcfunction 9a028740c90213342853abb4de18b0feb55f2f13eab7107bcd6f5ed88b2aaf21
E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/command/Regeneration.java bb685b0f6047091f053ed7aa54caea1a2a4d2dca649716ade250f0996275d7c1
E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/SigmaWarsMain.java e2ff679a9f920cad2eb3ae97389a5adf2777a3fa04e76f6e21c16b52bfd3f934
src/main/java/me/skywars2/game/LbcBabylonEffects.java c88de796264b80cd48df988776846210117adf83448223885004c664a746921f
src/main/java/me/skywars2/game/LbcKiuas.java 8f4c9d443fc8da19e6c6ab8fede85dfea4a7c155bb09ed2822c1d518f39ddefe
src/main/java/me/skywars2/mixin/LbcColorEffectsMixin.java 548ab45b4da7ee070488e18aa7e134e22cb37a54af5b41adea85b860c9168399
src/main/resources/skywars2/lbc_loadouts.json 99aad58d57f2215005b5aaad9a3b6dcdbdf21d80ecac06020443b1f96ba7555f
RP/assets/lbc/items/quiver.json fa2a59920d7a026780bd3b616803d6e73a9c9b3030a497df65187dcdca949b33
RP/assets/lbc/models/item/quiver.json c30c2e3909ae4ee61a70099dfb3842d0dde6badc135ac96b1f3f5b38e7e4a192
RP/assets/lbc/items/regenerationbelt.json b3e9b6543e6b3f96ce5a3312d1a91b10cdfeaba0202da0354c3b1bcaf1c4846a
RP/assets/lbc/models/item/regenerationbelt.json 7725aeac238c7083e34d5caa105522ad6db0fa1329c701f7329569a1ffb21abc
RP/assets/minecraft/lang/en_us.json 7971188ae0d2e5f16f7253c6495168650451599e2826eeb46f7cfcb4a0e507f7
RP/assets/minecraft/lang/ru_ru.json 9ba24ccecc189950fe970f525ff45b6e40df5b375fabd333cfa6786ca8dac2fc
```

Existing author RP models/styles/translations are reused; no new presentation is requested. Physical client pixels/audio remain unobserved. Frozen source preparation only.
