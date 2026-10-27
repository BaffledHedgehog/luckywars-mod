# Antimagic and Omen melee - source preparation, 27 October 2026

Read-only prep; only this new note written. Source root S=`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`. Source paths below relative to S. No native build/test/server/RP/git operations. Source text inspected; old runtime unobserved. Native attribution, Time Stop, signed effect layers/caps and allied-player/technical-role rules remain authoritative.

## Actual native gaps

Exact quoted item flags antimagic/holocaust and item models antimagic_stick/op_flint_funny have no live Java item handlers (checks excluded). Thus both accepted-hit entry points are missing. Shared native **mechanisms already exist**: LbcMana.applyNoMagic implements15-second-add/mana0 and HUD/timers, and LbcKiuas.acceptCurse implements raid_omen amplifier111 transport plus owned terrain curse. Do not duplicate those mechanisms or confuse nomagic_active artifact aura with nomagic counter. No third helper/family is needed for this bounded prep.

## Shared source input and selection ambiguity

`lbc/advancement/gowno.json`: accepted unblocked player_hurt_entity, non-arrow_only damage, victim not interaction, player outside nexus/imprinted; reward items/swords inspects mainhand. Dispatcher starts by copying owner team_number to fake *tempt lbcID2. Neither of these branches has a local mana payment, cooldown, antimagic/stopper guard, charge or RMB/sneak branch. They can proc from attributable non-arrow indirect/fatal events, not solely a click.

`lbc/function/items/swords.mcfunction:46`: antimagic1 -> as ALL nonspectator players distance0.01..7, HurtTime10, !same_team -> effect_nomagic. No living-mob targets; original hit victim can be a mob while an unrelated recently hurt player receives passive. Source helper only uses target @s, so retaining owner command origin rather than at target has no gameplay effect.

`:47`: holocaust1 -> effect give ALL #minecraft:mobs distance0.01..7, HurtTime10, gamemode!=spectator, !same_team raid_omen20 amplifier111. Source gamemode selector and custom mobs type set must not be silently generalized to every entity. Native role/ally defaults supersede unsafe source selectors. Neither is exact accepted-event-victim routing: fatal target may be dead/removed or not expose HurtTime10; other nearby victims can match. Parent should defer selection choice, just as Ender, rather than silently calling event-victim behavior source-exact.

## Antimagic passive and complete timer closure

`lbc/function/other/effect_nomagic.mcfunction:1-2`: target nomagic +=15, mana=0. ADDITIVE timer, not max(current,15); mana is actually emptied on each proc. No immediate damage/effect/sound/particle/nomagic_active tag. It suppresses mana regeneration, not a blanket personal wand cast/aura gate; an independently refilled pool can still be spent according to existing native admission. It does not remove all buffs or destroy magic carriers.

Source `players_tick:33` skips ordinary manaregen when nomagic>=1, except legacy mode does not run this regen route anyway; `players_1s:20` subtracts1 if positive. `players_2s:14` grants potions/effect_no_magic while positive. `ui/get_data:30` shows red score with E115. `other/purify:38`, resets:36 and leave_death:300 set0. Shared KFC all_up_scores_no_mana:77 adds1 (existing native shared support). No item-private lifetime/reset.

Native `LbcMana:130-140` applyNoMagic accepts ServerPlayer only, respects rejectCustomEffect, extends timer then set0. `tickPlayer`/mana regeneration path:298-300 early return while counterpositive; `secondPlayer:337-339` decrements and grants effect_no_magic; HUD LbcItems:492/LbcMana:377. clearEffects/resetResources clear same counter. Reuse this existing API, not a new nominal blindness/weakness effect. Existing API has no owner ledger for the timer itself; if downstream mana-loss consequences require kill attribution, attach original-owner cause through shared attribution instead of pretending helper contained it. Source integer overflow/add semantics versus native guard need explicit integration only if near counter limits; do not introduce arbitrary gameplay cap.

Antimagic advancement: task/impossible parent swords/cyberkatanamk2. Actual producer is players_1s:36: hold antimagic1, armor_stand tagmom within4, limit1, stand lbcID2 != player's lbcID2 -> grant swords/antimagic. It is NOT simply hitting a player or receiving nomagic. This ancillary proximity unlock is a separate live integration gap, not satisfied by importing advancement JSON.

## Omen passive is the existing Kiuaskivi terrain curse

raid_omen20 seconds means400 native ticks, amplifier111. `lbc/function/mobs_tick:1` sees active raid_omen on !spectator/!time_stopped actor -> projectiles/arrows2. arrows2:1-3 reads amplifier/duration (duration2 specialcases1); :35 adds duration to effect_kiuaskivi; tail adds have_lucky_effects/1s/5t and clears raid_omen. It is a transport sentinel, not a vanilla raid or direct damage command. Command grant and next mob tick mean the precise transferred duration may be399/400 depending scheduling; don't hard-code an alleged observed legacy value. Native existing immediate transport is already the platform's consistent sentinel contract.

`effects/tick.mcfunction:11` runs every actor tick while effect_kiuaskivi>=1 and !sin_greed_active. `effects/effect_kiuaskivi:2-5`: unless stopper_magic marker within20, run kiuaskivi2arr; subtract1 even while suppressed; retain have_lucky_effects. **Greed pauses both action and decrement**, stopper suppresses only action.

`items/arcanums/kiuaskivi2arr:1-3`: axis-aligned3x2x3 volume X/Z +/-1, Y0..1 relative target: first AIR replace #minecraft:kiuaskivi_to_air strict, then FIRE replace #minecraft:kiuaskivi_to_fire; flame particle3 FORCE at target spread.2/.2/.2 speed0. No explicit target damage or entity fire timer is added; native fire/terrain consequences cause damage. Tag to_air includes water, #snow, #ice, water_cauldron, powder_snow_cauldron, cobweb. Tag to_fire includes #mineable/axe, #mineable/hoe, #replaceable, #flowers, #saplings, #wool, #campfires, #wool_carpets. Ordering matters because to_fire includes replaceable states after AIR conversion. Source strict first fill suppresses normal updates, second fill standard. No destroy drops mode.

HUD `ui/get_data:55` red E146 score. purify:83 and leave_death:375 zero curse. Existing LbcKiuas:64-73 recognizes exactly111, adds effect.duration to same score and saves original coldCause owner/epoch; native addEffect mixin LbcColorEffectsMixin:55-56 intercepts transport. tickCurses:90-106 applies resident-only native kiuResult3x2x3 changes in trapChanges ownership, schedules FIRE ticks, emits3 flames, decrements per active tick; pauses native Time Stop/frost/Greed and respects suppression. load/unload restores persisted cause; clearCurse/purify/stop clear owned state. Native code is reusable unchanged, but melee producer must pass owner to addEffect so attribution is inherited; direct raw score addition would bypass lifecycle/ownership. Do not call arcanum wand use/payment or create a second curse service.

Native existing acceptCurse uses validTarget/canBeAffected and player rejectCustomEffect; melee target admission should also use established projectile/ally target guard. Transport reapplication is additive through score; it is not an ordinary max-duration raid_omen layer. Preserve already established sentinel policy rather than altering independent ordinary potion layering globally.

## Loot, presentation and acquisition

|Item source|Exact identity/components|
|---|---|
|loliluckyblock antimagic|iron_sword count1 weight10; model lbc:antimagic_stick; tooltip lbc:tier_a; knockback255; attack_damage+4/speed-2 add_value/mainhand ids minecraft:base_attack_damage/base_attack_speed; flags transformable1/sword1/antimagic1|
|transformables_sword_all antimagic|same fields but weight1 and tooltip lbc:tier_s (intentional source difference to retain)|
|imba/lucky_items_imba holocaust|flint_and_steel count1 weight10/quality0; NO item_model override, tooltip lbc:tier_b, fire_aspect255; attack_damage+9 add_value/mainhand id minecraft:base_attack_damage; flags holocaust1/imba1|

Antimagic name array [{translate:antimagic_stick,color:aqua,italic:false}]. Lore exactly3 rows: type+sword array aqua italicfalse; antimagic_stick.lore yellow italicfalse; array text nischtosnektem green italicfalse,type:text then literal ` Happy Birthday 14.10.2023!` dark_gray italicfalse,type:text (leading space retained). Omen name array op_flint_funny gold italicfalse; single lore imba_version italicfalse with COLOR OMITTED. Preserve vanilla iron sword/flint-and-steel item semantics, default durability and native use; no extra RMB spell implied.

Antimagic source acquisition: loli luckyblock and ultra-reforge sword_all; NOT ordinary transformables_sword. Omen source via imba/lucky_items_imba from luckyblock_imba/random_glack_1 paths. No separate old variant or dedicated ingredient recipe/kit was found by full item flag/model search. No standalone Omen advancement found. Importing a leaf alone does not enable whole source IMBA/loli pools; explicit catalog acquisition and gameplay handlers must be separately declared. No RP edits are needed for present names/models. Omen retains vanilla flint_and_steel model; don't invent op_flint_funny custom model.

## Viable next group and pending author decisions

Both entry points are genuine remaining gaps with reusable complete shared mechanisms; likely small two-passive group after Devil. However source nearby HurtTime selection is a known crutch: event-victim replacement vs radius-recent-hurt behavior requires the author's pending choice. Antimagic remains PLAYER-only; do not broaden it to friendly mobs without instruction. Preserve default protected allied players and technical-role guards. Consider whether source personal nomagic only blocking regen matches desired design; existing native API explicitly preserves this interpretation. No chat questions asked. Do not enable partial whole loot pools or claim runtime completion.

## Source original-byte SHA256

- `lbc/advancement/gowno.json` `b65994157053db257f20ffdd9304e2a0efb204114d2b73e4eb7da9f58b9a2e2f`
- `lbc/function/items/swords.mcfunction` `36162bd3115e12c5d8b0626f619d0eb88b408505282519a191ea7d1bdeb33f1f`
- `lbc/function/other/effect_nomagic.mcfunction` `5e804ac78e64489883f92c179fe82f5b286a4d95cd7b283e0cd3d95228fbc276`
- `lbc/function/players_tick.mcfunction` `851491714a1ed2831411a19a829d362e051fdc4b2d5b3db581318e01d383c483`
- `lbc/function/players_1s.mcfunction` `4e354224511c16f697d04089b43875b28547a24bec54c0e6190b56b4dcdff905`
- `lbc/function/players_2s.mcfunction` `abdb09f5ca07c52fa5adf335311d011d8ef3f643242d17d41905db521d9a1a5e`
- `lbc/function/projectiles/arrows2.mcfunction` `cb77e532d70439251d0d0046445a61d31fc487bd070abcc4b9354953d846d76f`
- `lbc/function/mobs_tick.mcfunction` `70fa3e4075cb14b3f9607c59b06fcdfaa23f6cd101d1b3b72e7fa49a8dc5d9c8`
- `lbc/function/effects/tick.mcfunction` `2c3777fe53bf6182953855996c1669227701c0fd6e9d2a950fe21d3dd8bfe2a6`
- `lbc/function/effects/effect_kiuaskivi.mcfunction` `95447159cc32999078a6efe7b96e276edd387e7c8f4911ff3692572c08592821`
- `lbc/function/items/arcanums/kiuaskivi2arr.mcfunction` `6611b250cc7d14691a328f74612f6a9a7f3f47e455ccc0129da7f42f18a58fbd`
- `lbc/advancement/true_advancements/swords/antimagic.json` `dc4c9b5c1d5e4d1a924b8663c679c1a9a9c3764ae7fb34c7ec04771390d88b53`
- `lbc/loot_table/loliluckyblock.json` `bf8709a096eb4870b8554e143f71813afd6c444c513c98bb4865f9a0e9ca17f3`
- `lbc/loot_table/transformables_sword_all.json` `81f3d3501daeeb6058534c1bbfa0960192aea8029689c3755c94d3509d592a0d`
- `lbc/loot_table/imba/lucky_items_imba.json` `8ea91c2b75f8f6e01dd43b3becf178c5c581955d4d749c75f3e87fc4bb086e04`
- `minecraft/tags/block/kiuaskivi_to_air.json` `f226006b4bafee71f6c6874271d03f90071d2f7bd6e53b39fba4dd69fb69a5c5`
- `minecraft/tags/block/kiuaskivi_to_fire.json` `a24427d774fa420debce73422a098b1770a577c821b1b0185c4910476c7cb888`

## Read-only RP exact text and model

`en_us` SHA256 `7971188ae0d2e5f16f7253c6495168650451599e2826eeb46f7cfcb4a0e507f7`

```json
{
  "antimagic_stick": "Antimagic Stick",
  "antimagic_stick.lore": "Prevents the use of magic when hitting!",
  "op_flint_funny": "Burn them all the fuck oh yeah",
  "imba_version": "This item exudes an unimaginable sense of humor"
}
```

`ru_ru` SHA256 `9ba24ccecc189950fe970f525ff45b6e40df5b375fabd333cfa6786ca8dac2fc`

```json
{
  "antimagic_stick": "Антимагический жезл",
  "antimagic_stick.lore": "Запрещает использовать магию при ударе!",
  "op_flint_funny": "Сожги их всех нахуй оо да",
  "imba_version": "От этого предмета веет невообразимым чувстом юмора"
}
```

`assets/lbc/items/antimagic_stick.json` SHA256 `344e93d652723337fcd83759b4c910b23c8b075a920c773fa0c570f9314b8c82`; {     "model": {         "type": "model",         "model": "lbc:item/antimagic_stick"     } }

`assets/lbc/models/item/antimagic_stick.json` SHA256 `a4ef2a4e29b0304bd9c4244e514e714e3542584e76a5bb5fc424ebd76c4c5674`; {     "parent": "minecraft:item/handheld_rod",     "textures": {       "layer0": "minecraft:item/antimagic_stick"     }   }
