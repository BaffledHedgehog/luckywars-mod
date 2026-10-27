# Remaining ordinary accessories - bounded next-group audit

27 October2026 read-only preparation. S=`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`. Only this note written; no code/build/test/server/git/RP changes. Current native resource table uses modifier/type schema; legacy source uses functions/function. Both inspected; source table presence is not live handler proof. Excludes qualified Popit/Traveller/jump artifacts/Looper and previously deferred Sand/reactive/incoming artifact families.

## Exact current raw table shortlist

`src/main/resources/data/lbc/loot_table/transformables_accessory.json` pool0 entries0..10:

|Index|Family|Native state / decision|
|---|---|---|
|0|gaara Sand Amulet|previous full audit, pending choices; excluded|
|1|save Safety Platform|no live flag/model/helper/native alias found; selected finite complete family|
|2|heall Regeneration Belt|no heall callback; current leaf also PublicBukkitValues lws:regen inventory stackable0.04; source helper onlycount/adv. Requires original attribute bridge ownership audit, do NOT implement as simple regen potion|
|3|manabelt Mana Regeneration Belt|already actual LbcMana.BELT:72/equipment/regen/adv support; not missing|
|4|magnet Magnet|no live flag/model/helper or alternate native behavior found; selected complete family|
|5|demonkishki Devil Stomach|no live flag/model/helper or native health-reactive alias found; selected complete family|
|6|arrowgenerator Infinite Quiver|missingentry; other/arrowgive_pre uses count-driven lbc:arrows pool withspecialarrow closures. Needs full arrowproducer loot support, not chosen as tiny fake arrow grant|
|7,8|artifact pool links|mixed qualified/pending families; do NOT activate wholesale|
|9|popit|currently qualified/being completed by parent; excluded|
|10|runic_ice_shard|already LbcIceMaterials full recognition/craft; excluded|

Native exact quoted save/heall/magnet/demonkishki/arrowgenerator searches exclude checks: no flag handlers; alias/model checks saveplatform/magnet/kishki/infinite_quiver found no substitute. Generic regeneration/food/void/magnet-like distant services do not prove these passives. LbcMana manabelt actualeffect and LbcIceMaterials actualsourceflag support are deliberately excluded from proposed work.

Best coherent next group: **Safety Platform +Magnet +Devil Stomach**, all ordinary current pool entries with self/item/terrain finite actions and complete source closure below. No HurtTime target ambiguity or WILD exception. Same flags also recognize two exact legacy leaves (platform/magnet); no separate oldlogic branch. No new author choice is required by default native role/TimeStop/resident/ownership policy; preserve source constants rather than invent safety redesign.

## Common inventory snapshot, cadence and cleanup

Source advancement penis inventory_changed -> items/changed_inventory/sel. save lines2/4 add save_platform_active for container.* or weapon.*; absentboth line24 removes. magnet8/9 adds magnet_active, absentboth25removes. demonkishki90/91 adds active, absentboth103removes. Armor-only/cursor/crafting slots are not literal source scope; existing user-approved broader artifact inventory scope should not automatically extend nonartifact accessories without an explicit established platform rule. Duplicate oneflag items do not multiply taggedpassive activation. Own service can cache booleans/counts on inventory changes, with full stop/reset removal; don't resurrect legacy globaltag loops.

players_1s:2 Safety Platform and:4 Magnet run per scheduledsecond for nonspectator players outside nexus/imprinted via tick1s->mobs_1s->players_1s; players_5t:11 Stomach per5scheduledticks (4Hz). These scheduled sourcefunctions have no blanketglobalTimeStop guard. Native established active-time TimeStop/match/role admission applies; do not confuse source schedulerphase with privatecooldown. Source local stopper versus stopper_magic guards differ and are specifiedbelow. NO nearby nomagic_active artifact player check on any selectedhelper. No mana payment, charge, newRMB or privateHUD.

Inventory removal stops future refresh/proc; alreadygranted ordinary effects expire naturally and existing worldplacement/drop states remain native. No helper-family score/lifetime/saveowner ledger beyond nativeeffects/loots is defined. Full matchcleanup invalidates queuedeffects/ownership and cached inventories through commonnative rules. Original item entities retain owner/thrower/causal metadata when moved byMagnet; teleport does not transfer itemownership or strip dangerouscustomflags.

## Safety Platform - all source behavior

`players_1s:2`: owner save_platform_active, selector y=-10,dy=-5000000 atowner, no stopper markerwithin20 -> workingitems/platform. This is an absolute below-Y threshold selector, NOT standard minbuildheight or actualfallvelocity/falltimer; source playerbbox intersects verticalbox from-5000010 through-9. It includes rising/hovering owner inthatvolume. Native implementation should use exact selector-equivalent boundingbox criterion or declare precise boundary adaptation, not arbitraryvoidY. Source has an extremefinite lower bound; retain source contract without arena-dependent guessedlevels.

`workingitems/platform:1-6`: teleport owner SAME X/Z to absoluteY2; self Levitation20ticks amp99 hidden; self SlowFalling400ticks amp0 VISIBLE; fill5x1x5 SLIME_BLOCK plane at absoluteY1, ownerX/Z +/-2 STRICT (overwrites existingblocks, noAIRfilter/destroydrops); clearONE inventoryitem matching save1; grant accesories/save_platform. Order is teleport/effects/fill/consume/award. No helper sound/particle/manualvelocity/terrainexpiry. Native effect/physics supplies upwardmotion overnextticks; no teleport to platformcenter or newheightfromcurrentY. Source fillcoordinates retain originalcommandX/Z; ownerteleport onlychangesY. Item is consumed once evenif several copies; >1copy can procnextsecond if owner still qualifies. Do not removeallcopies or require handholding.

Potential commandfailure semantics (unloaded/protectedworld) are oldengineunobserved; native residentguard should prepare consistentplane/teleport/consume operation and avoidloading. Do not silentlyclone blocks/restore oldterrain afterarbitrarytimer. Native platformplacement supports/fallcauses inherit owner; fallrescue is selfaction, noWILD harmpermission. Source advancement task/impossible parentlivinglegend? Exactparentrecordedbelow; native grantmust use actual importedid. No dedicatedrecipe found.

## Magnet - full source helper (inventory enables, HAND activates)

`players_1s:4`: magnet_active and no stopper marker20 -> magnethelper. `workingitems/magnet:1` teleports ALL itementities distance<=30 toowner **ONLY if weapon.* containsmagnet1**. Thus merely backpackcarrying enablescheck but DOESNOTpullitems; mainoroffhand holding required. No livingmobs/XP/projectiles/containers pulled, noLOSfilter/terrainray/motionlerp. Oneinstantnativeitemteleport persecond, preserve itemage/pickupdelay/thrower/owner/customdata/causalmetadata, noinventoryinsertion or forcedpickup. Radius30sphere basedentitypositions. Residentnativeentitylist only; no chunkloading.

Lines2/3 independently check ANY itementitydistance<=1 matching penis_bomb1 or malevich1 and grant accesories/magnet. These checks happen AFTERpull, regardlesswhethermagnet wasinhand (helperstillrunsfrominventorytag). ItemneednotAge0, notconsumed, notrequiredpickedup; don'tgrant fromrandomnormalitem. No sourceparticles/sound/mana/cooldown/itemconsumption. Native dangerousdropbehaviors remain, itemteleport doesnotarm/disarmthem. Source globalitemselection means otherplayers'items can move; authordefault protection doesnotinvent itemownershipfilter absent existingcommonitempolicy. This is not entityfriendlyfiredamage.

## Devil Stomach - native health/hunger reactive passive

`players_5t:11`: demonkishki_active AND sourcefoodlvl>=16 AND hplvl<=4, no stopper_magic marker20 -> other/extraheal. `load:22-23` foodlvl criteriaFOOD and hplvl criteriaHEALTH. Native26.3 ServerPlayer:694 HEALTHscore=ceil(Health+Absorption), :699 FOODscore=foodlevel. Therefore admission is NOT just HP<=4 ignoringabsorption; sourcecriteria can lag serverupdatephase, but implementation should reuseauthor-approved realhealth+absorptionscore-equivalent and avoid broadstalereadredesign. ExampleHP1+absorption4=>ceil5reject; HP3.5=>ceil4admit; positiveHP.5=>ceil1admit.

`other/extraheal:1-4`: if hplvlEXACT1 grant accesories/devilstomach BEFOREeffects; selfRegeneration40ticks amp10 HIDDEN; selfHunger200ticks amp255 HIDDEN; wither.ambient MASTERgloballyatowner volume1/pitch0. No directhealthset or fooddebit. Native hunger/regeneration signedlevels and independentlayers determineheal/foodloss overactualticks; donotreplace with instantheal/nosaturationcommand. Itmayrefresh every5activeticks whilethresholdshold. No manualcooldown or consumeaccessory, duplicatesdon'tmultiplygrant. OnceHP/risingfoodconditionfails, nativeeffectremainderscontinue; don'tpurifylayerswhenitemremoved. Capturedoriginalowner for hunger/nativecurse bookkeeping should avoid inventing killcredit forselfrescue.

Advancement task/impossible parentmagnet, rewardgain/kit; nativecatalog alreadykit1 broken_totem gate devilstomach. Actualhelpergrant isthresholdhplvl1, notanyheal/hold/use. Oncestomachcomplete nativeachievementunlocks existingcatalog, don'tgrant kitimmediately or alterselectedkit. No dedicatedingredientrecipe/oldstomachleaf found.

## Exact current and legacy leaves/presentation

Allcurrentselectedleaves repeating_command_block count1, transformable1/accessory1 plusfamilyflag. Noattributes/enchantments/durabilityoverride unlesslegacyleafbelow. Currentpoolindex1 save:weight10/quality9/tier_b/modellbc:saveplatform; name save_platform green italicfalse; lore rowtype+accessory WHITEitalicfalse, then save_platform.loreGREENitalicfalse. Index4 magnetweight10/quality0/tier_d/modellbc:magnet; name magnet_item italicfalse COLOROMITTED; type/accessoryWHITE plus magnet_item.lore.1/.2/.3 WHITEitalicfalse. Index5 stomachweight10/quality10/tier_b/model lbc:kishki; name devil_stomach italicfalse COLOROMITTED; type/accessoryWHITE plus devil_stomach.lore.1..5 WHITEitalicfalse.

Legacy_luckyblock entry112 Platform: samebase/model/name, no tier/type row; one save_platform.lore greenitalicfalse, customdatasave1only, protection1 enchantment hiddenvia tooltip_display hidden_components enchantments. Entry143 Magnet: samebase/model/name, no tier/type row; threeWHITElores, magnet1only. Samecurrentflaghelperlogic applies; do notinvent old separateability. Nolegacystomach found bycompleteflag/modelsearch.

Native compiled transformables_accessory usesmodifier schema but preserves theseexactentries/currentindices. Ordinary accessorytransform/reforge givescurrentleaf; native LbcBolts normal/ultra/lightning accessoryrules reachsourcepackagedtable. Legacyexactleaves maybeindividualoperatorcatalogimports; don'tenableentirelegacyrandompool. Existingbuildincludescurrentpoolandtrue_advancements; rawsourcefunctionengineisnotpackaged/live. NoleafrequiresnewRP; reuseexistingmodels/equipmentnone/translationstyles. NoLooper forselectedfamilies.

## Unselected dependency findings and source-only boundaries

InfiniteQuiver source players_15s checkscontainer.* ANDweapon.* independently, so sameinv+handpresencecanexecutehelperTWICE. arrowgive_pre countsALLarrowgeneratorcopies via clear0 and setsarrowgens, then lbc:arrows table **rolls=arrowgensscore**, so producesoneweightedarrowentry percopyperhelpercall (notonevanillaarrow). StandarrowAge0nearby conditiongrantsinfinite_quiver, zombie.infectglobally1/2. Emptyother/arrowgive recursion isnotactualsourceproduction; lootrollsimplementscount. Fullspecialarrowpool/gameplayclosure mustbeauditedbeforeclaimingcompletefamily; notchosenhere.

RegenerationBelt helper nohealcommand: commented-outhealinglogic, activeclear0count theny score<=-100000 and>=10belts grantsregeneration_belt. Currentleaf PDC lws:regen value.04 add inventory stackabletrue idregenbelt1242 ispluginattributechannel. No nativePublicBukkitValueslws:regen consumerfound; cannotcallitimplementedfromhelperorordinarynativeRegeneration. Separateplugin/authorregenattributebridgeauditrequired, notgenericregenpotionreplacement.

Selectedthreefamiliesrequireestablishedresident/ally/role/TimeStop/ownershippolicybutnonehasHurtTimeorunresolvedfan/overlapbranch. Sourceoddities(absoluteYplatform,hand-onlymagnet,beneficialselfHunger255)areexplicitfinitecontract, notexcuseforpartialport. Noimplementationorqualificationclaimed. Anyremainingchoiceaboutplatformbboxboundary/groupfailure shouldberecordedparent, noquestionsinchat.

## SHA256 and exact advancement parents

- `function/items/changed_inventory/sel.mcfunction` `0b9c6a85afe888ab288ccb32822c7aece0cb4903c2ebbee37bf24e48ea99e7d5`
- `function/players_1s.mcfunction` `4e354224511c16f697d04089b43875b28547a24bec54c0e6190b56b4dcdff905`
- `function/players_5t.mcfunction` `6ebbf9d5b67e221f0cec67fee494963e9d2b22c11830370f1249274ab224d1c1`
- `function/workingitems/platform.mcfunction` `8a864b471c914e6177bc6c92b11d382b7505b8a2402484596e4d5e21192e6834`
- `function/workingitems/magnet.mcfunction` `f4c37bfc3122e284b0828f490dbb707034a022fe410a257e5221bc6627f84822`
- `function/other/extraheal.mcfunction` `90c39c0afb47a065920702cf25b9b26a83b3fe18673159de2123d4c5099b07cb`
- `function/load.mcfunction` `b2b4c8d0f4fd6d8a9464262368188d4a26d71bf29f7f931b423658c7a763f6e1`
- `loot_table/transformables_accessory.json` `3792e70b859e718276c7000dcc81bd055462cf8600b5833389efd2a5f53e1524`
- `loot_table/legacy_luckyblock.json` `bd7a1a5aa859362d96e93b7261ed160b48686829ffcd8be33a24daa93818ef01`
- `advancement/true_advancements/accesories/save_platform.json` `244be8c38c97961b61904ac93129654c7c044fd1c4cbf32094db603d401a7867`
- `advancement/true_advancements/accesories/magnet.json` `78eb4a96d7a29d9982a9ecb89b419690f2406c1ad99e63ebe7ae88b1003031ec`
- `advancement/true_advancements/accesories/devilstomach.json` `c0b127c204e14be28265c07b0e0be1e7017593bcb74d78a7f0a369ee364a9e42`
`save_platform` parent `lbc:true_advancements/accesories`, criterion `minecraft:impossible`, reward `{'function': 'lbc:swrg_kit_integration/gain/levelup'}`.

`magnet` parent `lbc:true_advancements/accesories/mana_belt`, criterion `minecraft:impossible`, reward `{'function': 'lbc:swrg_kit_integration/gain/skill'}`.

`devilstomach` parent `lbc:true_advancements/accesories/magnet`, criterion `minecraft:impossible`, reward `{'function': 'lbc:swrg_kit_integration/gain/kit'}`.

## Read-only RP exact values/models

`en_us` SHA256 `7971188ae0d2e5f16f7253c6495168650451599e2826eeb46f7cfcb4a0e507f7`

```json
{
  "save_platform": "Save platform",
  "save_platform.lore": "Will save you from the void once (works in any slot)",
  "magnet_item": "Magnet",
  "magnet_item.lore.1": "Teleports items",
  "magnet_item.lore.2": "to the owner in a range of 30 blocks.",
  "magnet_item.lore.3": "Works in any hand.",
  "devil_stomach": "Devil stomach",
  "devil_stomach.lore.1": "On very low HP and,",
  "devil_stomach.lore.2": "nearly full hunger,",
  "devil_stomach.lore.3": "greatly heals you,",
  "devil_stomach.lore.4": "but drains saturation.",
  "devil_stomach.lore.5": "(works in inventory)"
}
```

`ru_ru` SHA256 `9ba24ccecc189950fe970f525ff45b6e40df5b375fabd333cfa6786ca8dac2fc`

```json
{
  "save_platform": "Спасательная платформа",
  "save_platform.lore": "1 раз спасет от падения в пустоту",
  "magnet_item": "Магнит",
  "magnet_item.lore.1": "Телепортирует предметы",
  "magnet_item.lore.2": "к владельцу в радиусе 30 блоков,",
  "magnet_item.lore.3": "когда в руке.",
  "devil_stomach": "Демонический желудок",
  "devil_stomach.lore.1": "При здоровье меньше 2 сердец",
  "devil_stomach.lore.2": "и сытости выше 16 единиц,",
  "devil_stomach.lore.3": "полностью восстанавливает здоровье,",
  "devil_stomach.lore.4": "за счёт всей полоски голода.",
  "devil_stomach.lore.5": "(работает в инвентаре)"
}
```

`assets/lbc/items/saveplatform.json` SHA256 `c21ba999ec5ffd80d291966cfa01b14b305a17fef75cc067cddcb135e0522735`; {     "model": {         "type": "model",         "model": "lbc:item/saveplatform"     } }

`assets/lbc/models/item/saveplatform.json` SHA256 `9ebc0b022a0221ad533755ba8e50e4e651bbeae4187c1106a5eaf448130a8453`; {   "parent": "minecraft:item/generated",   "textures": {     "layer0": "minecraft:item/saveplatform"   } }

`assets/lbc/items/magnet.json` SHA256 `6e195fccbb7d3825d2f9bda1fb8a16c1be7acabd7566f4919d97368230883252`; {     "model": {         "type": "model",         "model": "lbc:item/magnet"     } }

`assets/lbc/models/item/magnet.json` SHA256 `f56735a60658f1e6f1fa655f4f4be78500b2c9e43165f3ea0bfe87bc1b9ab83d`; {   "parent": "minecraft:item/generated",   "textures": {     "layer0": "minecraft:item/magnet"   } }

`assets/lbc/items/kishki.json` SHA256 `78051cbad5192e62f3a26ba90caa86c043839780b5f1c2f1ad7c06af71e667fb`; {     "model": {         "type": "model",         "model": "lbc:item/kishki"     } }

`assets/lbc/models/item/kishki.json` SHA256 `ef934b59fbb466e5cc1de57b1a2fdca2734e17dd85c158a3bf9c7cd1e7066ea2`; {   "parent": "minecraft:item/generated",   "textures": {     "layer0": "minecraft:item/kishki"   } }
