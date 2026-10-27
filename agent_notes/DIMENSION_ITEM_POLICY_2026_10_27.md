# Dimension item policy ? current native inventory audit

27 October2026, read-only source audit plus isolated policy/check classes. No
GameController/LbcItems/shared-hook, server, build, git or protected files changed
by this child. Root owns shared integration and integrated native qualification.

## Conclusion and integration

GameController.canUseGameplayItems and allowDamage currently describe match
participant/observer/developer state, not source item dimensions. They are also
used by native damage targets, actor ownership and accepted dimension transfers.
Keep those role predicates unchanged. New ordinary item activation and carried
passive production must independently reject minecraft:nexus and
minecraft:imprinted. Vanilla damage/projectiles must remain targetable.

LbcDimensionItems exposes normal(Level), blocked(Level), custom(ItemStack), and
allowed(ServerPlayer, ItemStack). It recognizes authoritative skywars2 registered
items, original nonempty custom_data and lbc item models. Ordinary vanilla items
remain ordinary. The dedicated Nexus input classifier recognizes exactly the
four active source flags below; it does not supply missing gameplay engines.
Normal custom input still requires original role/stun/Time Stop/owner/epoch gates.
The new class changes no shared hooks by itself.

Root's proposed callback gates on Fabric UseItem/UseBlock/UseEntity/Attack cover
new packet input. LbcWands.cast and common actionsBlocked owner input guards cover
direct native producer calls. Arena-only mana.visible and passive-level checks
already exclude transferred players today; explicit dimension guards also remain
correct if a future arena aliases one of the special dimensions. Do not make
LbcWands.validRoleTarget or the global damage/projectile target policy depend on
ordinary item activation.

Concrete additional new-proc paths to guard:

- LbcTrinkets.damaged outgoing Snow Gem/Intercal reads only participant admission,
  with neither a dimension/arena nor actionsBlocked guard in that branch. Reject
  new custom procs for excluded owner dimensions while preserving accepted native
  vanilla damage and existing owned consequences.
- LbcHedgehogSpikes.allowed has no arena/dimension test but calls actionsBlocked;
  the proposed common owner input dimension guard covers it.
- LbcSheep.potionHolder is an inventory resource witness with participant checks
  and no arena/dimension condition. A normal-dimension witness check is appropriate;
  do not treat it as a global damage target filter.
- FinishUsingItem after a dimension transfer can complete without a fresh UseItem
  callback. KfcFood.valid already checks the arena; review other custom consumed
  callbacks separately rather than canceling vanilla food completion wholesale.
- Stand, Shooters, Pop-It, current inventory hazards/passives reviewed here already
  have actual arena-level guards; their normal callbacks should also use the new
  policy for future map aliases. Parent hooks must not erase accepted actors merely
  because their author later teleports or becomes an observer.

## Exact legacy dimension boundaries and exceptions

Source tick_not_timestopped.mcfunction1..5 excludes BOTH dimensions for ordinary
items, raid-omen clouds, slow raycasters, mobs and projectiles. tick5t/tick10t/
tick1s/tick2s/tick5s/tick10s/tick15s repeat the exclusion on their ordinary paths.
Thus source item/passive dispatch is separate from ordinary vanilla engine behavior.
The registered Paper Regeneration task's broad Imprinted return is an audited bug,
corrected by approved choice20 per-player skip; it does not authorize ordinary
Nexus gameplay. Source ordinary inventory Belt task excludes both dimensions.

Nexus has its own source tick_nexus, tick_nexus_player and
other/nexus/tick_1s_player. Exact ACTIVE other/nexus/rc.mcfunction flags:

|Flag|Source handler|
|---|---|
|nexus_roadgun|other/nexus/click/roadgun, cooldown<=0|
|nexus_corrupter|other/nexus/click/corrupter, cooldown<=0|
|nexus_orb_gun|other/nexus/click/orb_gun/pre|
|nexus_pulsar|other/nexus/click/pulsar/pre, pulsar_cd<=0|

nexus_compass RC invocation is COMMENTED and must not be classified as a live
exception. These four source Nexus tools are not current complete native engines;
recognizing their input flags must not expose unimplemented tools or whole pools.
Nexus wire crafting (red+blue+black dropped items -> white wire), wire-converter
frame task, and dedicated nexus_rc actor tags nexus_tree, center_cube,
explose_wired, roadgun and sim_cube are separate systems. Their presence does not
allow ordinary current weapons/accessories to work in Nexus.

## Accepted cross-dimension consequences

LbcHedgehogDimensions explicitly maps FROM_IMPRINTED to Nether/End/Nexus/normal
arena according to the accepted source dispatcher; it has no FROM_NEXUS handler.
It checks current owner and target participant roles before actual native transfer.
Those checks must keep working after the item-input dimension policy is added.
The source's accepted Hedgehog/God-Hedgehog effects and LbcDuplicating's explicit
cross-world cloning are consequence/terrain paths, not permission to start new
ordinary item abilities inside the forbidden dimensions. Do not apply a blanket
canUseGameplayItems dimension restriction to those shared role checks.

## Scope and exhaustive static manifest

The manifest below enumerates every current Lbc-prefixed production game Java
file found in this workspace. It is a static boundary inventory, not a claim each
family has independent runtime coverage. D means a literal dimension reference
(any purpose, including destination handling); M means maps().level() reference;
R means canUseGameplayItems role reference; I means actionsBlocked input reference;
V means mana.visible reference; F means item factory/definition code. A missing
marker is not proof of a bypass: factories and shared helpers inherit admission
from callers, and a present marker is not proof every method is protected.
Non-Lbc orchestration GameController, SkyWars2/Fabric callbacks, native mixins and
SignedEnvironmentalEffects were reviewed as shared participant/input/target edges.
Root should test real dimension entry/return and accepted Hedgehog transitions.

Scanned 196 Lbc production game Java files.

|File|Boundary references|
|---|---|
|LbcAcademy|D,M,R,F|
|LbcAcidCorruption|R,I,V|
|LbcAdvancedSnow|I,V|
|LbcAir|M,R|
|LbcAk47|M,R,I,F|
|LbcAntDropItem|F|
|LbcAntDrops|M|
|LbcAntigravity|M|
|LbcAntigravityDropItem|F|
|LbcAwakenedAcid|I,V|
|LbcBabylon|V,F|
|LbcBabylonEffects|M|
|LbcBedrock|M,R,V|
|LbcBeeDrop|M|
|LbcBeeDropItem|F|
|LbcBlaze|V|
|LbcBlind|M,R|
|LbcBloodConfuse|I,V|
|LbcBolts|M,R,V|
|LbcCageDropItem|F|
|LbcCageDrops|M|
|LbcCannedFood|D,M,R|
|LbcCausticOmega|I,V|
|LbcChainLightningStatus|M|
|LbcChargedSwords|D,M,R,I,F|
|LbcChefBrain|-|
|LbcChefSurvival|-|
|LbcChestDropItem|F|
|LbcClassicMelee|D,M,R,I|
|LbcClouds|M,R|
|LbcColors|M,R|
|LbcCommunism|I,V,F|
|LbcConsuming|M,R|
|LbcCorrosionDropItem|F|
|LbcCorrosionDrops|M|
|LbcCorruptedGlyphs|M,R,I,F|
|LbcCorruption|I,V|
|LbcCraftingRecipe|M,R|
|LbcCreatureDropItem|F|
|LbcCreatureDrops|M|
|LbcCreeperDropItem|F|
|LbcCreeperDrops|M|
|LbcDamage|-|
|LbcDanmaku|M,R,I,V,F|
|LbcDefensiveArcanums|V,F|
|LbcDevilSword|D,M,R,I|
|LbcDimensionItems|D|
|LbcDroneDropItem|F|
|LbcDroneDrops|M|
|LbcDropBlocks|-|
|LbcDuplicating|D,M,R|
|LbcElectro|M,R,F|
|LbcElementalGrimoires|M,R,I,F|
|LbcEnderWiki|I,V|
|LbcEnzanDrop|-|
|LbcErecto|M,R,I,V|
|LbcEvokerBass|M,I,V|
|LbcExasperato|M,R|
|LbcExoperso|M,R|
|LbcExtraGrimoires|-|
|LbcFallingNetheriteDropItem|F|
|LbcFallingNetheriteDrops|M|
|LbcFartus|M,R,I,F|
|LbcFeedbackDropItem|F|
|LbcFeedbackDrops|D,M|
|LbcFevgaleaStar|D,M,R,I,F|
|LbcFrost|M,R|
|LbcGaycaster|I,V|
|LbcGaycasterAcademy|-|
|LbcGlyphAi|-|
|LbcGodHedgehog|R|
|LbcGovnomet|M,R,I,F|
|LbcGreed|-|
|LbcGunAccuracy|-|
|LbcHalloweenWind|I,V,F|
|LbcHedgehog|M,R,I|
|LbcHedgehogDimensions|D,M,R|
|LbcHedgehogItems|F|
|LbcHedgehogSpikes|R,I|
|LbcHelicopterEncounter|M|
|LbcHelicopterTriggerItem|F|
|LbcHohlopitek|I,V,F|
|LbcHoleDropItem|F|
|LbcHoleDrops|M|
|LbcHolyShield|D,R,I|
|LbcHomeItems|F|
|LbcHomes|M,R,I|
|LbcHoming|M,V|
|LbcIceMaterials|V,F|
|LbcIceWand|M,R,I,V|
|LbcIncomingArtifacts|D,M,R,I|
|LbcIndirectKills|-|
|LbcInventoryHazards|D,M,R|
|LbcIslandDropItem|F|
|LbcIslandDrops|M|
|LbcItems|D,M,R,I,V,F|
|LbcJumpArtifacts|D,M,R,I|
|LbcKaraSlabs|D,M,R,I,F|
|LbcKfcFood|M,R,F|
|LbcKfcRewards|M,R,I,V,F|
|LbcKiuas|M,V,F|
|LbcLaserMinigun|M,R,I,F|
|LbcLevitationDropItem|F|
|LbcLibraryDrop|M|
|LbcLibraryDropItem|F|
|LbcLibraryRandom|R|
|LbcLibrarySpells|M,R,I,F|
|LbcLifestealer|M,V|
|LbcLightningAcquisition|D,R|
|LbcLoadouts|R,I|
|LbcLuckyPigDropItem|F|
|LbcLuckyPigs|M|
|LbcLuckyRoomDropItem|F|
|LbcLuckyRoomDrops|M|
|LbcMagic|M,R,F|
|LbcMagmify|-|
|LbcMagnum|M,R,I,F|
|LbcMana|M,R,F|
|LbcMinigunMovement|-|
|LbcMobgun|M,R,I,F|
|LbcMoiseiBoots|D,M,R|
|LbcNeutral|I,V|
|LbcNpcDropItems|F|
|LbcOpAnvilDrop|M|
|LbcOpAnvilDropItem|F|
|LbcOrdinaryAccessories|D,M,R|
|LbcOverload|R,V,F|
|LbcOverloadArmor|M,R,V|
|LbcPendingCombat|D,M,R,I|
|LbcPendingSwords|D,M,R,I|
|LbcPisaDropItem|F|
|LbcPisaDrops|M|
|LbcPopit|M,R,I|
|LbcPortalDropItem|F|
|LbcPortalDrops|M|
|LbcPotionSpawnerDropItem|F|
|LbcPotionSpawnerDrops|M|
|LbcPrism|M,R,I,V|
|LbcPsycho|M,R,V|
|LbcQuiver|D,M,R|
|LbcRandomStatLevelup|-|
|LbcReactive|I,V|
|LbcReactiveArtifacts|D,M,R,I|
|LbcReactiveItems|F|
|LbcReforgeCrafting|M,R|
|LbcReforgeMachineItem|F|
|LbcRegenerationBelt|D,M,R|
|LbcRerollCrafting|-|
|LbcRifleZoom|R,I|
|LbcRuby|M,R|
|LbcSauvojen|M,R|
|LbcScareDropItem|F|
|LbcSheep|M,R,I,F|
|LbcShooters|M,R,F|
|LbcSkills|-|
|LbcSkyHazards|D,R|
|LbcSlabDropItem|F|
|LbcSlabDrops|M|
|LbcSoulhunter|D,R,I,V,F|
|LbcSoulless|M,R,I,F|
|LbcSpecialDrops|M|
|LbcSpecialEggs|M,I,F|
|LbcSpecialTools|I,V,F|
|LbcStalin|M|
|LbcStalinDropItem|F|
|LbcStand|M,R,I,F|
|LbcStone|M,R|
|LbcStonekivi|M|
|LbcSummonerItems|F|
|LbcSummonOrders|V|
|LbcSummons|I,V|
|LbcSurfaceTeleport|-|
|LbcSyringe|M,V,F|
|LbcTanner|M,V,F|
|LbcTeamColors|-|
|LbcTeleport|M,R|
|LbcTerra|M,R,F|
|LbcTesla|R,I,V|
|LbcTimeStop|D,R,I|
|LbcTimeStopSchedules|-|
|LbcTinyReforge|-|
|LbcTntDropItem|F|
|LbcTntDrops|M|
|LbcTotems|D,R|
|LbcTransformator|M|
|LbcTraveller|M,R|
|LbcTrinkets|M,R,F|
|LbcUltraponos|M|
|LbcUltraponosDropItem|F|
|LbcVexSword|M,I,V,F|
|LbcVoidArcanum|M,R,V,F|
|LbcWall|V|
|LbcWands|M,R,I,V,F|
|LbcWtf|M,R,I|
|LbcZombieTntDropItem|F|
|LbcZombieTntDrops|M|

DimensionItemChecks now uses real registered native connection scheduling for
actual Imprinted and Nexus transfers with loaded/teleport ACK at both arrivals
and return. Each destination exercises the shared Fabric UseItem callback and
direct native wand dispatcher refusal without mana/item mutation, while actual
native ordinary Arrow DamageSource hurt remains accepted for that same player.
It also checks exact source Nexus exception classification only (unported tools),
vanilla stack classification, and scoped profile/inventory/attribute/advancement/
score restoration. Suite log key is dimension-items. These prepared native checks
are not an observed runtime pass or proof of every production family callback.

## Additional non-Lbc production boundary inventory

- BlackHoleVisuals: shared/factory/consumer; caller admission
- BlockAreaWork: shared/factory/consumer; caller admission
- DirectionalKnockback: shared/factory/consumer; caller admission
- EffectTerrain: M
- EnchantmentRules: shared/factory/consumer; caller admission
- FullInvisibility: shared/factory/consumer; caller admission
- GameController: R
- GameLuckyModifiers: shared/factory/consumer; caller admission
- GameMenus: shared/factory/consumer; caller admission
- MapManager: shared/factory/consumer; caller admission
- MatchEvents: shared/factory/consumer; caller admission
- NegativeAttributeEffects: M,I
- NegativeMining: M,I
- NuclearVisuals: shared/factory/consumer; caller admission
- OriginalGuiDesign: shared/factory/consumer; caller admission
- ProjectileLifetimes: M
- SauvojenMeditation: M,R
- SauvojenStars: M,R
- ShadowAbsorption: shared/factory/consumer; caller admission
- SignedEffectMath: shared/factory/consumer; caller admission
- SignedEnvironmentalEffects: M,R
- SignedGlowVisuals: shared/factory/consumer; caller admission
- Spectators: M
- ZoneBarrier: M

Frozen initial integration SHA256:

- LbcDimensionItems.java: `62bcd30c3583dcdcc4763c58ccab6fe2b952aaa1b614fe345b65883107c1a56d`
- DimensionItemChecks.java: `22dea16842158288486cd221b342ea3cb8304612469f65106e4ad2ae320f9f9e`
