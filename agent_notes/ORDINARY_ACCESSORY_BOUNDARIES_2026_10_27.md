# Safety Platform, Magnet and Devil Stomach - independent native boundaries

27 October2026 read-only audit. Only this new note written, no source/code/oldnote/build/test/server/git/RP edits. Exact full family source contracts: [ACCESSORY_REMAINING_SOURCE_2026_10_27.md](ACCESSORY_REMAINING_SOURCE_2026_10_27.md). This follow-up independently verifies critical native selector/fill/inventory/metadata/guard semantics; source runtime1.21.4 remains unobserved. Native26.3 engine code inspected from existing loom sources archive, not a new build.

## Native source inventory and consume scope differ

Source `lbc/function/items/changed_inventory/sel.mcfunction:2,4,24` save, :8,9,25 magnet, :90,91,103 demonkishki cache matching **container.* OR weapon.***, remove tag when absentboth. These cover player inventory slots exposed as container slots and main/offhand; they are not generic equipmentarmor.*, menu cursor or crafting-grid scans. Container indices beyond playerinventory simply fail slot lookup; do not interpret container.* as all loaded containers. Native Player.getSlot resolves supplied entity slots, not nearbyworldcontainers. Duplicate stacks/copies do not multiply passive execution. Inventorychanged advancement reward plus resets:127 refreshes cache; native event/dirtycache replacement can avoid technicaltags.

Platform `clear @s *[custom_data~{save:1}] 1` has a **broader consume matcher** than detection: native ClearInventoryCommands:76 calls Inventory.clearOrCountMatchingItems with player.inventoryMenu.getCraftSlots; Inventory:182-188 searches playerinventory THEN personalcraftslots THEN currentcarriedstack, max total1. Detection still requires qualifying carried inventory/hand so a cursor-onlysave should not enable rescue. If severalmatches exist, vanilla scanorder and max1 apply; do not blindly decrement an arbitrary cached slot after inventory changed. Native exact clear primitive or equivalent consistent inventory scan should preserve these source semantics. No explicit item durability call; platform consumes1 stack unit, Magnet/Stomach consume0.

Armor-only customflags and cursor/crafting detection must not be broadened merely because author explicitly broadened **mysticalartifact** scope elsewhere. If platform/magnet/stomach use shared general inventory API, document actual slotchoice and preserve ordinary accessory policy. No hidden perpiece equipmentattribute exists on currentthreeleaves; exact oldPlatform enchantment/tooltip difference remains in source audit.

## Platform selector exact AABB - x/z defaults are one block

Source `players_1s.mcfunction:2`: `at @s ... @s[y=-10,dy=-5000000]`. Native26.3 EntitySelectorParser:125-133 if ANYdelta present buildsAABB and defaults missing dx/dz0; :139-140 positional transform replacesYonly, keeping executionownerX/Z. createAabb:160-170 turnsnegative dy into min=-5000000,max=1; dx/dz0 become0..1.

Therefore selector worldbox is **[ownerX,ownerX+1] x [-5000010,-9] x [ownerZ,ownerZ+1]**. EntitySelector uses boundingbox INTERSECTION, not feetpoint inclusion. For normal selfbbox X/Z always overlaps, but precisely player.boundingBox.minY < -9 AND maxY > -5000010, strictintersection. FeetY=-9 exactly doesNOTmatch; -9.001 normallydoes. It is NOT simply feetY<=-10, not centeratworld0XZ, and not an infiniteXZ plane. Giant/changedscale bounds still use actualnativebbox. No fallvelocity, falltimer, health, onground or blockbelow requirement exists.

Do not substitute dimensionminY/voiddamageY or currentYrelativeplatform. Sourcehelper teleports to absoluteY2 and builds atY1. Approved native worldbounds/residentchecks are required; source constants are explicit design, not guessedcap. Effective playerX/Z is currentownerposition when scheduled1secondcheckfires, not stalejumporigin.

## Platform strict fill and failure ordering

`workingitems/platform:1-6`: teleport sameX/Z Y2; selfLevitation20ticks amp99hidden; selfSlowFalling400ticks amp0visible;5x1x5 SLIME fill absoluteY1, ownerX/Z +/-2 floored; clear1save; grantaccesories/save_platform. Independent commands do NOT returnonfillfailure. If Y1 outofbuildrange or fillfails, source laterconsume/achievement stillattempted afterteleport/effects. Do not falsely describe legacytransactionrollback. Native preserves source command failure order: attempt teleport/effects first; resident/height/debug/max-limit preflight controls only the fill. Clear1 and advancement still run when fill fails; no atomic cancellation of rescue.

Native FillCommand:158-181 invokes BlockInput.place with **818** forstrict (2|816); Block.java:91-104 defines clients2,knownshape16,suppressdrops32,skipblockentitysideeffects256,skiponplace512. Non-strict neighbourpass :185-186 is NOTrun. No DESTROYaffector used: no block loot or containercontentsdrops. BlockInput.place:68 skips neighbourshapeadjustment whenknownshape16, thenlevel.setBlock(update818). Do not emulate with destroyBlock(true), setBlockAndUpdate or manually spilloldcontainercontents. Existing native **LbcDropBlocks.fill:15-29** already uses BlockInput and818/258, strictno-neighbourpass, maxmodificationlimit and heightguard; promising exactprimitive, but it DOESNOTpreflight wholefootprintresidentchunks and its fill debugguard differs from command. Caller must guardresidentfootprint and bounds before invoking.

Native source command registration FillCommand:49-82 uses getLoadedBlockPos forcorners. BlockPosArgument:37-44 checks hasChunkAt and isInWorldBounds; FillCommand:141-153 checksmaxmodifications/debugworld. Cornerchecks alone do not prove everyintermediatechunkresident; Native preflights the full 5x5 footprint for the fill, without loading chunks or placing a partial plane. A failed fill does not cancel the preceding teleport/effects or subsequent clear/award. Each command preserves its own height failure; do not invent a replacement Y. Preserve planeoverwrites ofexistingblocks withsuppressedloot; noAIRonlyfilter and notemporaryrestoretask.

Shared original-owner terraincause: use LbcIndirectKills.capturedWorldChanges/trapChanges accordingtoexistingownedblockpolicy, with **default** friendlyfire (notWILD). Trapframe:356-383 recordsplacedhazards/supportstate with cause; no directtargetdamage is added. Selflaunch/rescue keepsnativelevitationmovement and signedcaprules, no manualnewvelocity. Existing matchstop/epoch cleanup cancelsqueuedaction; placedslimeworldblocks themselves follow establishedterrainlifecycle.

## Two distinct beacon guards, no artifact aura veto

Source Platform/Magnet players_1s guards `type=marker,tag=stopper,distance<=20`: **mechanism beacon**, notantimagic. Native LbcMagic.mechanismSuppressedAt:455-456 checks realmechanismbeacon only inmaplevel; beaconSuppressed(position,true):465-474 includesloadedbeaconregion andcompatlegacy stoppertagmarkers spherical20.

Stomach players_5t:11 checks marker stopper_magic20: **antimagicbeacon** only. Native LbcMagic.beaconSuppressed(position):464 delegatesfalse andchecks correspondingreal/legacymagicbeacon. DoNOTuse LbcMagic.suppressedAt:409-414 here: that additionally vetoes nearbyactive antimagicARTIFACTplayers, absentthesehelpers. No source ownernomagic_active guard onselectedthreefamilies. Currentauthor rules alreadydistinguish paidwand/aura andbeacon; no newpermissionquestion required fordetectorchoice. Compatiblelegacy markers retainmatchingtype/sphere, notcubicbounds orgeneric alltags.

Platform/Magnet scanmechanism atowner CURRENTposition beforehelper; platformdestinationY2neednotbequeriedagain assourceprecondition. Native resident/suppression workmustnotaddharmfulcross-familynewadmission silently. Native established TimeStop/roles/Greed apply independently; scheduledsourcepaths canrunwhileglobalTimeStop, so nativeactivepause intentionallysupersedes schedulerquirk.

## Magnet teleport metadata and no hidden damage

Source workingitems/magnet:1 pulls ItemEntity radius30 onlywhenmagnetweapon.*; lines2/3 near1dangerousitemadvchecks canrunfrominventorycarryevenwithoutpull. Native ItemEntity.setPos/teleportTo in samelevel preserves stack/components/count, age,lifespan,pickupdelay,owner/throwerUUID,invulnerability,damage/hazardtags andownedcauseattachments. Do notrespawnitem or invokeplayerTouch/insertinventory/resetAge/clearowner, do not change pickupdelay to0. Source tp has command-level motion postconditions: the complete native transition resets ALL ItemEntity movement components, then sets onGroundtrue (verified chain below). Do not preserve old X/Z velocity from an incomplete reading of the final Y-multiply. No steering/LOS/XP/mobpull or per-itemdirectionalvelocity iscoded. Existingdangerousdrop ticks mayreact tonextposition; no additionalarming/disarming cleanup.

Potentialcross-owneritemmovement is sourceareaquery, notsourceitemownershiptransfer. Keep native role/dimension/entityvalidity andloadedentityguard, but doNOTadd arbitraryitempermissionfilter asifsourcecontainsone. No directhealthdamage; indirecthazard/dropconsequences mustretainoriginalitemcause, not reattributeallitemstoMagnetuser. Owner's movementcause is distinct from carrieditem's originalexplosioncredit. Magnet consumption0/noadditionalfeedback.

## Stomach health resource and global audio

Load:22 FOODfoodlvl, :23 HEALTHhplvl. Native26.3 ServerPlayer:694 computesceil(health+absorption), :699 updatesfoodlevelcriterion; sourcecriteria updatephasecanlag buttargetimplementation shouldreusealreadyapprovedactualhealth+absorption admission. Conditionfood>=16 andceil(health+absorption)<=4, every5activeticks; exactceil1grantsdevilstomach. Don'tusefloorHealth, ignoreabsorption, requireexactintegerHP or maxhealthpercentage. This isa1..4healthscorethreshold, nothearts4. NativeordinarypositiveHP andvalidowner arecommonadmission.

Extraheal:2 Regeneration40ticks amp10hidden, :3 Hunger200ticks amp255hidden; native signedamplifier/layer/cap policy applies, no artificialfoodloss/instantHPset. Hunger255 positive native effectisnotanintentionalnegativeamplifier. Hiddengrantdoesnotremoveotherlevels. Whileconditionshold thesamelevellayerisrefreshed; conditionalhealthhungerupdates shouldnotresetotherstatusledger. UseownerasnativeaddEffectsource forconsistency; doNOTinvokeexternalattacker'sdamagecause or grantnewkillcreditmerelyforhealing.

Extraheal:4 **wither.ambient MASTERtoALLplayers**, atownerposition, volume1/pitch0, no source16/20radiusselector. Existing LbcOverload.sound(owner,id,1,0,0) globalaudience/positionalnativeattenuation isappropriate; don'tprivatelyplay@selforomit soundbecauseitcanrepeat4timespersecond. No sourceparticlesorfoodconsumption/accessoryconsumption/privatecooldown. NativeGreed/cleansing rejectionrules remainauthoritative, but doNOTdisableallbeneficialselfeffects bynewoptionalGreedcheck without existingpolicy.

## Leaves, source routes and native advancement/kit integration

Current raw transformables_accessory pool0 entries1save,4magnet,5demonkishki arepresentnative resources withallsourcecomponents. build.gradle:441 includesnormalpool; :323includesaccessory_all whichreferencesnormalpoolweight10. LbcBolts normalaccessoryrule57/ultra80/lightning447 reachthosepackagedtables. No separateingredientrecipe/RMB/itemregistryrequired for passiveflagrecognition. ExactoldPlatform/Magnet sameflags leaves existlegacy_luckyblock entries112/143, but enablingentireoldpool isnotauthorized. No oldStomachfound. Dedicatedexactportedleaves can beaddedbyparent withoutchangingauthorRP; noregistry-IDrewrite needed.

Native source copiedadvancements omitoriginallegacyrewardfunctions; LbcLoadouts catalogs useactualnativeachievementcompletion or savedunlock. Stomachgrant accesories/devilstomach unlocksexistingkit1/page9000/slot0/tablekit/broken_totem (cataloglines3-9), normalgated/rankedbypass, noimmediategrantorsetselectedkit. Platformgrant unlocksexistinglevelup4knockbackresistance.4 sourcekey/catalognativeentry, notakit; don'tmisreportplatformreward askit1. Magnetnativeunlocknotification alreadynew_skill permap (exactresourcehashbelow); verifymappedperkavailability ratherthaninventnewreward. HelpergrantpreconditionandoriginalIDsremainexact; normalnativeawardhookemitsnewunlocktitleonce.

No sourcecraftedvariantsorprivateartifacttimers; callbacks/debounce mustretainoneprocperphase independentofduplicatecopies. Nativecommonfullstop/forget refreshinventorysnapshot andqueuedwork; don'tremoveexistingnativeeffectlayers/teleporteditemflagsbroadly. Allthreefamiliescomplete sourceclosure exists; Platform fill preflight preserves source ordered failure: even a limit24 failure skips only the 25-block plane, then consumption/award proceed. No authorchoice required to inventnewheight, beaconkind, metadatareset or allyharm.

## Primary-source and native-code hashes

- `lbc/function/players_1s.mcfunction` `4e354224511c16f697d04089b43875b28547a24bec54c0e6190b56b4dcdff905`
- `lbc/function/players_5t.mcfunction` `6ebbf9d5b67e221f0cec67fee494963e9d2b22c11830370f1249274ab224d1c1`
- `lbc/function/items/changed_inventory/sel.mcfunction` `0b9c6a85afe888ab288ccb32822c7aece0cb4903c2ebbee37bf24e48ea99e7d5`
- `lbc/function/workingitems/platform.mcfunction` `8a864b471c914e6177bc6c92b11d382b7505b8a2402484596e4d5e21192e6834`
- `lbc/function/workingitems/magnet.mcfunction` `f4c37bfc3122e284b0828f490dbb707034a022fe410a257e5221bc6627f84822`
- `lbc/function/other/extraheal.mcfunction` `90c39c0afb47a065920702cf25b9b26a83b3fe18673159de2123d4c5099b07cb`
- `lbc/function/load.mcfunction` `b2b4c8d0f4fd6d8a9464262368188d4a26d71bf29f7f931b423658c7a763f6e1`
- `lbc/loot_table/transformables_accessory.json` `3792e70b859e718276c7000dcc81bd055462cf8600b5833389efd2a5f53e1524`
- `lbc/loot_table/legacy_luckyblock.json` `bd7a1a5aa859362d96e93b7261ed160b48686829ffcd8be33a24daa93818ef01`
- Native `src/main/java/me/skywars2/game/LbcDropBlocks.java` `36ec2af4e4a8d2a62cf0e66cf07d492cd6c17357884918676cf5cb35c09a4434`
- Native `src/main/java/me/skywars2/game/LbcMagic.java` `ea98c36afe09d77712fc4f04cbb1f4c837c5594bf9e74907f9b3136fa5ffb3bb`
- Native `src/main/java/me/skywars2/game/LbcIndirectKills.java` `f8a2a10cdf104cd133c1c2b57eba9916e2d15dc66b5f5e8a85055f90b54f75fc`
- Native `src/main/java/me/skywars2/game/LbcLoadouts.java` `aca0e966dfb0168b4849e665c5f3456eb9524e547f517c7f47320f47ce03c2db`
- Native `src/main/resources/skywars2/lbc_loadouts.json` `99aad58d57f2215005b5aaad9a3b6dcdbdf21d80ecac06020443b1f96ba7555f`
- Native `src/main/resources/skywars2/lbc_unlock_notifications.json` `47af6ee6bc870c75a34f53608b2df312807ba0a81243861fc1caf923b1fa5e2b`
- Existing native26.3 sources.jar entry `net/minecraft/commands/arguments/selector/EntitySelectorParser.java` `0594d77d70c80b252b02e61017392c17b7d428126b04ef77e6b9c6b4e54f514c`
- Existing native26.3 sources.jar entry `net/minecraft/server/commands/FillCommand.java` `e1ee9a5b16cc028d1d956cfba82ebfb3915ab64e13cd38007f5c5b0d7b6f7db7`
- Existing native26.3 sources.jar entry `net/minecraft/commands/arguments/blocks/BlockInput.java` `b3dfe08d66b740ec6ed5e731b4b2cc013148ccb7452920a1edee792d374834bf`
- Existing native26.3 sources.jar entry `net/minecraft/commands/arguments/coordinates/BlockPosArgument.java` `f4eab68c29f004ccb08dd8bdfc6e4f027720263dab4c098af9e61d05285dc0d4`
- Existing native26.3 sources.jar entry `net/minecraft/world/level/block/Block.java` `1b7038e00822fe76e022f9a5b81278d56d83de7fa0e572909a2a4a8e1e4d0046`
- Existing native26.3 sources.jar entry `net/minecraft/server/commands/ClearInventoryCommands.java` `5846e52a71825c0fab15d057712abb8f6f2a2fb059e874e05c480360b840feb5`
- Existing native26.3 sources.jar entry `net/minecraft/world/entity/player/Inventory.java` `ebdafbb3a089fc657e23d4a7d5a487b02cf10304e9021cfb4d20759014c5dccd`
- Existing native26.3 sources.jar entry `net/minecraft/server/level/ServerPlayer.java` `f5677c657385af1516ee3c18ff589ce33bb950ef70b08948892d13e4ae4ca128`

## Verified complete teleport transition chain - corrected result

The earlier inference from only TeleportCommand's final Y-multiply was incomplete. Native26.3 **TeleportCommand.teleportToEntity:145-156** passes EnumSet.noneOf(Relative.class) for source Magnet `/tp @e @s`. **Entity.teleportTo:3438** constructs TeleportTransition with Vec3.ZERO movement; Entity.teleportSetPosition:3259-3267 calculates absolute destination then setsDeltaMovement. **PositionMoveRotation.calculateAbsolute:55-64** preserves old movement components ONLY when corresponding DELTA_X/Y/Z flags are present; absent flags return the transition's zero delta. Therefore Magnet resets ALL THREE motion axes tozero before TeleportCommand:233-235 multiplies Y byzero and setsOnGroundtrue. Bare setPos or preserving old X/Z velocity is incorrect for this exact command.

Platform uses coordinate `/tp ~ 2 ~`, a different path: TeleportCommand:180 getRelatives, :193-197 combines position and direction flags; Relative.direction:68-79 adds DELTA_X andDELTA_Z for relativeX/Z. Therefore horizontal velocity survives, Y becomeszero; source yaw/pitch remain unchanged and non-fallflying owner becomesonGroundtrue. Do not use empty relatives for Platform if preserving exact native source motion. Current fall-flying LivingEntity skips only TeleportCommand final zeroY/groundstep; transition delta rules still apply. Native engine evidence is26.3, not observed1.21.4runtime.

## Verified sound recipients - @a is not a global packet broadcast

Native26.3 PlaySoundCommand.java:151 creates a variable-range SoundEvent; :152 uses getRange(volume)^2; :158 rejects players in other levels; :165-167 skips distances beyond range when minVolume<=0. Stomach source supplies @a, volume1, pitch0 and omitted minVolume(default0): **only same-level players at 3D distance<=16 receive packets** (boundary included). Far players and cross-dimension players receive none. The earlier wording "to ALL players" describes the target selector, not packet delivery. LbcOverload.sound(owner,"minecraft:entity.wither.ambient",1,0,0) already matches: :249-264 performs same-level/range/minimum-volume checks; radius0 means no extra selector radius. Do not manually broadcast to all connections. This is current26.3 engine evidence; original1.21.4 runtime remains unobserved.

## Exact detection versus consumption slot order - verified completion

Player.getSlot:1720-1721 exposes only non-equipment inventory0..35 to container.*. Mainhand and offhand are added by weapon.* source checks. Therefore armor-only/crafting-only/cursor-only Save does not enable source rescue. However `/clear ... 1` is deliberately broader: ClearInventoryCommands:76 passes full Inventory and personalcraftslots; Inventory.clearOrCountMatchingItems:182-188 checks Inventory first, then personalcraft, then currentcarriedcursor. Inventory.getContainerSize:409-410 is36 plus7equipment slots; getItem:431-437 maps36feet,37legs,38chest,39head,40offhand,41body,42saddle. ContainerHelper scans ascending index and remaining budget1; after the first matching unit the remaining budget iszero.

Consequences: normal backpack/hotbar Save at0..35 is consumed before armor, even if offhand alsoqualifies. If ONLY offhand enables rescue while an armor Save alsoexists, lower equipment index36..39 Save is consumed BEFORE offhand40. A stale/simplified consume iterator over0..35+40 silently narrows literalclear behavior. Craft/cursor are reached only after no earlier inventory/equipment match remains; source detection still doesn't let them enable rescue alone. Nativeimplementation should preserve separate activation and consumption scopes and vanilla scanorder. This is a concrete source/native semantic boundary, not a request to broaden accessory activation.

## Magnet reward boundary verified after native catalog check

Source advancement `advancement/true_advancements/accesories/magnet.json:30-32` rewards gain/skill. `function/swrg_kit_integration/gui/page/10000:12,63,93` gates slot1 by this advancement; `gui/select/10000:5` sets lbc.skill2. `loot_table/gui/page/10000/1.json` names lbcskill.1, translated by author RP as Armored inventory. Its seven lore rows promise immunity to selected inventory-active hazards and hands-only Infinity/Eternal Wealth/Liquid Void/Jaakivi/Nexus Strings. Source `other/instants/select:1-3` selects select2 for skill2, whose complete five lines inspect SelectedItem infinity_rand, nexus_wire_black/blue/red, greedstone. `other/liquidvoiddamage:1` and `workingitems/iceshard:1` have their own hands-only gates.

Native `LbcIceMaterials.java:63` already recognizes skill2 for hands-only material behavior, but no other full corresponding skill2 closure was found by production lbcSkill comparisons. Current lbc_loadouts.json has no Magnet skill gate. This is a deferred whole skill with partial native consumers, not a catalog omission for an already complete perk. Preserve original advancement and skill notification; do not imply the skill becomes selectable or add a partially functional catalog entry. Platform levelup and Stomach kit remain distinct, already cataloged reward routes.

## First native probe: known player movement is the relative baseline

The first focused native run was rejected before the timed Stomach probe: its fixture set server delta(.2,.5,-.3), while the setup had explicitly left known client movement zero. PositionMoveRotation.of(Entity):25-27 reads getKnownMovement(), and ServerPlayer.getKnownMovement:2204-2206 returns lastKnownClientMovement. Therefore preserving relative X/Z correctly produced zero from that fixture input. Entity.teleportSetPosition:3256 passes this exact baseline into calculateAbsolute; the source command and helper use the same path. This finding does not justify changing production or weakening the expected horizontal preservation. The corrected positive setup supplies the actual known-client baseline and compares with an independently executed native /tp command. Raw rejected logs, exact stopped/restored receipt, artifact identity and primary-source excerpts are retained in build/verification/ordinary-accessories/attempt1/. Subsequent runtime qualification remains pending at this audit checkpoint.
