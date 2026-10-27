# Standalone Pop-It - complete source audit, 27 October 2026

Read-only research. S=`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`. Only this new note written; no code/test/build/git/server/RP mutation. Source text inspected, old runtime unobserved. Existing native signed-effect layers/caps, Time Stop/input/Greed/role and original-owner lifecycle policy apply. Parent implements Traveller independently; no combined-release completion is claimed here.

## Genuine gap and exact input

No actual Java popit flag/item/model or workingitems/popit handler exists (checks excluded). Existing compiled transformables_accessory contains the raw source Popit leaf; JSON availability alone is not gameplay. Existing shared effect/counter services can perform much of its action but need selective owned-status reset APIs, not global purify.

`workingitems/spell1_default.mcfunction:44` mainhand SelectedItem popit1 and `spell1_default_left:21` offhandpopit1 -> workingitems/popit. Both enter ordinary magicwand `items/spells` via rightclickable interaction advancement, then spell1/left default unless upgraded. Owner !nomagic_active and no nonspectator nomagic_active player within20; offhand gate lefthand swrg.math !=1. NO branch-local stopper_magic marker check, mana debit, required charge, cooldown/sneak gate, item count clear, consume or durability damage command. It is reusable each admitted source input; either hand; both hands may independently proc if dual-input pipeline permits. Do not invent one-second cooldown or eat the accessory. Source overloaded wand temporary mana plumbing should not charge Popit merely because it is magicwand.

Native usable-item registration should keep source carrot_on_a_stick proxy/model and route both hands through existing wand admission/inputTimeStopped/config offhand policy, but avoid calling unrelated paid spell branch. Native direct use should not depend on spawning the legacy rc_able interaction entity. No separate projectile/carrier/timer/HUD/terrain action exists.

## One activation: sound then exactly one inclusive random1..27

`workingitems/popit.mcfunction:1` plays bubble_column.bubble_pop MASTER ONLY owner, atowner, volume100000/pitch1. No particle grant, ambient sound to others, delayed task or child damage. :2 chooses inclusive uniform1..27 once. Draw may do nothing if chosen status absent; do not reroll until successful or bias to active effects. Multiple activations independent, no random bag. Effect-clear removes entire chosen type and all hidden signed amplifier layers; don't erase unrelated types.

|Draw|Exact action|Pre-clear advancement condition|
|---|---|---|
|1|clear instant_damage|active instance duration==-1|
|2|clear levitation|none|
|3|clear mining_fatigue|none|
|4|clear nausea|none|
|5|clear poison|active instance duration==-1|
|6|clear slowness|none|
|7|clear unluck|none|
|8|clear WATER_BREATHING (beneficial, literalsource)|none|
|9|clear weakness|none|
|10|clear wither|active instance duration==-1|
|11|clear bad_omen, NOT raid_omen|none|
|12|clear blindness|none|
|13|clear glowing|none|
|14|clear hunger|none|
|15|clear darkness|none|
|16|set delayed_death0|scorewas>=1|
|17|set dimensional_shifting0|none|
|18|set lookback0|none|
|19|set nomagic0|none|
|20|set piss0|none|
|21|set reverse_mana0|none|
|22|set scared0|none|
|23|set shock0|none|
|24|set skill_issue0|none|
|25|set target0|none|
|26|set twitching0|none|
|27|remove greed_cursed tag|tagwaspresent|

Advancement is lbc:true_advancements/accesories/popit. Conditions are checked BEFORE clearing in same draw. Infinite-effect checks inspect active_effects entry only, not recursive hidden layers; native hidden infinite layer under finite stronger active effect should not incorrectly award. Drawing missing status still succeeds as input and sounds. Negative custom score is set0 too but draw16 advances only positive; durationinfinite -1 exact. No status damage is inflicted by this helper. Already-granted effects can be reset while native metadata/side-effects must detach coherently.

## Comparison with Traveller's popit_2

`popit_2:1-22` has sound owner volume100000/pitch1, then inclusive1..20. Draws1..15 same effect-clear list as standalone;16=lookback0,17=piss0,18=scared0,19=shock0,20=twitching0. It has NO standalone advancement conditions/grants; NO delayed_death/dimensional_shifting/nomagic/reverse_mana/skill_issue/target/greed_cursed removal. Traveller calls it SIX times (including sound SIX times), independent draws WITH replacement, then separate buff draw. Standalone calls1..27 once, sounds once. Do not call Traveller helper six times to implement Popit or substitute standalone27-way helper for Traveller20-way cleanse.

The default effect count and sound are shared semantic primitives, but RNG tables/advancement conditional ordering are distinct. Parent's native Traveller implementation may expose `clearRoll(player,roll)` for1..20 with named mapping and `effectClear(player,type)`; avoid making standalone's draw16 mean lookback. Names should distinguish source27 and20 action tables.

## Native selective cleanup APIs and boundaries

Native effect removal: player.removeEffect(effectHolder) removes complete native layers and invokes existing FullInvisibility/signed-effect/visual hooks. No custom synthetic clearing of only strongest layer. Use active effect `isInfiniteDuration()` before remove for draws1/5/10 advancement checks. Instant Harm infinite status can be absent in ordinary native instant-effect delivery; preserve condition rather than force an achievement.

Custom cleanup needs exact selective service adapters:

- delayed_death: LbcAcidCorruption.clear(target) already clears timer AND owned death ledger. Do not call broad acid purify that erases unrelated curses.
- nomagic: LbcMana.noMagic(player,0) existing exact counter reset; reverse_mana: LbcMana.reverse(player,0) additionally clears reverse owner ledger. Neither should refill mana or clear all resource effects.
- lookback/piss/scared/twitching: LbcPsycho tracks native Affliction fields/forced use/follow range. Raw scoreboard set0 alone is not effective native cleanup. Existing `clearEffects(player)` clears ALL psycho afflictions and ray, too broad for one draw. Expose per-status reset restoring only state owned by that status, leaving other active afflictions/timers/rays intact.
- shock: LbcElectro owns shock pulses; raw shock score reset is insufficient. Expose selective remove-shock keeping charges/other electrical effects; full reset/clearEffects may clear too much. Reuse current owner ledger removal.
- dimensional_shifting/skill_issue/target: source score resets exact names; no dedicated live gameplay producer was established by exact quoted search beyond KFC shared-score catalogue, so clear actual existing score without claiming an implemented phantom service. Do not confuse dimensional_shifting with other effect_dimshifting namespace without evidence.
- greed_cursed: remove tag only, do not remove active Greed artifact/gen/sin state. Existing other producers already remove this same tag; this helper isn't global Greed purge.

Native status-cleanse admission still follows established artifact/Greed policy. A self item removing beneficial water_breathing or a debuff is source table behavior, not reason to change signed-effect caps. Original-owner state must be removed with status so canceled damage cannot fire later; don't grant kill attribution for cleansing.

## Exact leaf, old variant absence, acquisition and advancement

Full source loot/helper flag search found ONE Popit definition: `loot_table/transformables_accessory.json`. No popit_old flag or alternate legacy Popit leaf exists in legacy_luckyblock/otherloot. Current base carrot_on_a_stick/count1/weight10/quality5; tooltip lbc:tier_c; item_model lbc:popit; NO attribute/durability/enchantment override. Name is literal component array [{text:"Pop-It",italic:false,type:"text"}], no color/translate. Lore exactly4 rows: type+accessory array WHITE italicfalse; popit.lore.1/.2/.3 WHITE italicfalse. Flags transformable1/accessory1/magicwand1/popit1. Don't replace literal name with invented translation key.

Ordinary accessory transformation/reforge source routes supply leaf; no dedicated ingredient recipe or crafting helper found. Compiled raw leaf exists but source pool enablement vs correct native usability are separate. Leaf consumes nothing onuse. No companion kit grants Popit itself: Popit advancement gates native/source kit2 (page9000/slot1), granttable kit/3_purify_potion. This is the cleansing potion kit, not Popit acquisition.

Displayed Popit advancement task/impossible parent accesories/infinite_quiver, icon carrot_on_a_stick modelpopit; rewards swrg_kit_integration/gain/kit. Only actual helper conditional grants listed above unlock it; merely inventory-changed, usecount or removing ANY debuff must not award. It parents mystical_artifact advancement but is not required for gameplay use unless existing global catalog admission saysso. No Looper explanation or extra HUD in standalone closure.

## Independent viability and pending scope

Source RNG/admission/leaf is fully bounded and has no HurtTime or friendly-fire ambiguity. Main implementation work is selective status cleanup APIs (Psycho/Electro) and achievement pre-clear tests; do not ship raw scoreboard-only fake cleansing. Source extreme owner-only sound100000 is literal, also shared by Traveller; preserve or record author-approved audio change consistently. Parent may combine only after these complete27 outcomes and unconsumed twohand native input work; Traveller alone remains independent. No whole unresolved artifact pool activation or runtime qualification claimed.

## Source original-byte SHA256

- `function/workingitems/popit.mcfunction` `166dabd38c06aa73fe7b0c9bd2879316545731d11167e45d6c743aa64213006f`
- `function/workingitems/popit_2.mcfunction` `a4716fe075b9dbd8fc479b94a9f4a543c5ead06ac7f8d0cc504d14ffbc2917af`
- `function/armor/traveller_set_bonus.mcfunction` `371b9b2f67620ce099707ac027efd81d9762621337d21dc4767a0517aaa08103`
- `function/workingitems/spell1_default.mcfunction` `f96b0af185d52fd8a18a6f15c1d9ecefd7678e725d0449673b3e39f9793f9773`
- `function/workingitems/spell1_default_left.mcfunction` `1fc3c74097023eafe4a7aedd15fb1c45e53efee3c6f8308efbd2e5207be83fe7`
- `function/items/spells.mcfunction` `80ab5607eee4b352b1701391c643ea052b60fa834b2ba9a709287f6dc2af652e`
- `advancement/interaction_rightclickable.json` `d06ced365ba81fdde1cb1abd0027970a1879cc636efca698d1bc072c3512a0bb`
- `loot_table/transformables_accessory.json` `3792e70b859e718276c7000dcc81bd055462cf8600b5833389efd2a5f53e1524`
- `advancement/true_advancements/accesories/popit.json` `31e3c23ed1c1e8cc696ecb523dc0f5e8fba701010890aeaca4b96f74e045b255`
- `loot_table/kit/3_purify_potion.json` `8f269bc33a879d3015239a18f6cc583b4c873ec08bce1f3f1a32e7e2fd725f6d`

## Read-only RP exact presentation evidence

`en_us` SHA256 `7971188ae0d2e5f16f7253c6495168650451599e2826eeb46f7cfcb4a0e507f7`

```json
{
  "popit.lore.1": "On use can remove random negative effect.",
  "popit.lore.2": "CAN means it also can do nothing (spam it)",
  "popit.lore.3": "Can even remove curses :o",
  "lbc.adv.accessories.popit": "Seriously, how did you do that?!",
  "lbc.adv.accessories.popit.lore": "Remove any deadly curse of impending doom or eternal greed from yourself with the Pop-It"
}
```

`ru_ru` SHA256 `9ba24ccecc189950fe970f525ff45b6e40df5b375fabd333cfa6786ca8dac2fc`

```json
{
  "popit.lore.1": "При использовании (ПКМ) может",
  "popit.lore.2": "убрать случайный негативный",
  "popit.lore.3": "эффект",
  "lbc.adv.accessories.popit": "Серьёзно, как ты это сделал?!",
  "lbc.adv.accessories.popit.lore": "Снимите с себя проклятие жадности с помощью Pop-It"
}
```

`assets/lbc/items/popit.json` SHA256 `6d74e43ced9f881ee201a0ba7182132dc4ba60810de1f1f4ddd930813d916bf6`; {     "model": {         "type": "model",         "model": "lbc:item/popit"     } }

`assets/lbc/models/item/popit.json` SHA256 `173e5fb5b60398d9d9b3975fe3b0fa4fb51121633bdc709781d11a8c64d9eae2`; {   "parent": "minecraft:item/generated",   "textures": {     "layer0": "minecraft:item/popit"   } }
