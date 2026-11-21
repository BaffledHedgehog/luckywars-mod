package me.skywars2;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.skywars2.game.*;
import me.skywars2.game.combat.LbcDamage;
import me.skywars2.game.commands.DeveloperTools;
import me.skywars2.game.core.config.GameConfig;
import me.skywars2.game.effects.FullInvisibility;
import me.skywars2.game.entities.CoreEntityMerging;
import me.skywars2.game.entities.mobs.summons.LbcStand;
import me.skywars2.game.gui.GameMenus;
import me.skywars2.game.gui.loadouts.LbcLoadouts;
import me.skywars2.game.items.drops.LbcHomeItems;
import me.skywars2.game.items.drops.LbcHomes;
import me.skywars2.game.items.drops.LbcLuckyRoomDropItem;
import me.skywars2.game.items.drops.LbcPortalDropItem;
import me.skywars2.game.items.drops.LbcPortalDrops;
import me.skywars2.game.items.tools.LbcCraftingRecipe;
import me.skywars2.game.items.wands.LbcElectro;
import me.skywars2.game.items.wands.LbcTerra;
import me.skywars2.game.items.weapons.ranged.LbcShooters;
import me.skywars2.game.match.checkpoints.MatchActorCheckpoint;
import me.skywars2.game.match.events.MatchEvents;
import me.skywars2.game.modes.legacy.LbcBalanceItems;
import me.skywars2.game.players.PlayerStore;
import me.skywars2.game.players.inventory.LbcUseInteractions;
import me.skywars2.game.presentation.effects.AtlasGlitchSignal;
import me.skywars2.game.world.maps.MapManager;
import me.skywars2.game.world.nexus.LbcNexusCombat;
import me.skywars2.game.world.nexus.LbcNexusToolItems;
import me.skywars2.game.world.nexus.LbcNexusTools;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import me.skywars2.game.modes.bedwars.LbcBedwarsAuxiliaries;

/** Fabric wiring. Match state and behavior live in GameController. */
public final class SkyWars2 implements DedicatedServerModInitializer {
    public static final String MOD_ID = "skywars2";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    // Polymer читает косметический снимок также при кодировании пакетов вне серверного потока.
    private static volatile GameController game;
    public static GameController game() { return game; }
    public static MutableComponent text(String key, Object... args) {
        return Component.translatable(MOD_ID + "." + key, args);
    }

    private static InteractionResult bedwarsUse(net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        if(game==null||!(player instanceof ServerPlayer owner))return InteractionResult.PASS;
        var auxiliary=game.bedwars().auxiliaries();
        if(hit!=null&&auxiliary.open(owner,hand,owner.level(),hit.getBlockPos()))return InteractionResult.SUCCESS;
        if(auxiliary.useZombie(owner,hand,hit==null?null:hit.getBlockPos(),hit==null?null:hit.getDirection()))return InteractionResult.SUCCESS;
        if(me.skywars2.game.modes.bedwars.LbcBedwarsAuxiliaries.chestItem(owner.getItemInHand(hand))&&!game.bedwars().canUse(owner))return InteractionResult.FAIL;
        return InteractionResult.PASS;
    }
    private static InteractionResult seasonalUse(net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        if(game==null||!(player instanceof ServerPlayer owner))return InteractionResult.PASS;
        var stack=owner.getItemInHand(hand);
        if(me.skywars2.game.items.drops.LbcKapusta.jack(stack))return game.vanity().blocked(owner)?InteractionResult.FAIL:
                game.items().kapusta().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
        if(me.skywars2.game.items.weapons.melee.LbcNovemberLightsaberItems.selector(stack)){
            if(game.vanity().blocked(owner))return InteractionResult.FAIL;
            return game.items().lightsabers().use(owner,hand);
        }
        if(me.skywars2.game.items.ItemData.intValue(stack,"pozdman_bot",0)==1){
            if(game.vanity().blocked(owner))return InteractionResult.FAIL;
            return game.items().pozdmanBot().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }
    private static InteractionResult touhouUse(net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        if(game==null||!(player instanceof ServerPlayer owner)||!me.skywars2.game.items.gadgets.LbcTouhouCollectibles.consumable(owner.getItemInHand(hand)))return InteractionResult.PASS;
        return game.items().touhouCollectibles().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
    }
    private static InteractionResult nexusToolUse(net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        if(game==null||!(player instanceof ServerPlayer owner))return InteractionResult.PASS;
        var held=player.getItemInHand(hand);boolean compass=LbcNexusToolItems.isCompass(held);
        if(!compass&&!LbcNexusTools.matches(held)&&!LbcNexusCombat.matches(held))return InteractionResult.PASS;
        if(game.items().actionsBlocked(owner))return InteractionResult.FAIL;
        return (compass?game.items().nexusCompass().use(owner,hand):LbcNexusCombat.matches(held)?game.items().nexusCombat().use(owner,hand):game.items().nexusTools().use(owner,hand))?InteractionResult.SUCCESS:InteractionResult.FAIL;
    }
    @Override
    public void onInitializeServer() {
        // Здесь связываются жизненный цикл сервера, игровые события и команды; состояние матча хранит контроллер.
        var nativeChecks = NativeCheckHooks.get();
        CoreEntityMerging.load();
        MapManager.registerLoadingTicket();
        DeveloperTools.registerItem();
        LbcHomes.registerTicket();
        MatchEvents.registerAirdropTicket();
        LbcPortalDrops.registerTicket();
        me.skywars2.game.items.wands.LbcBlaze.registerTrialTicket();
        LbcItems.register();
        me.skywars2.game.modes.legacy.TrueLegacyRules.registerItemProjection();
        LbcPortalDropItem.register();
        LbcLuckyRoomDropItem.register();
        me.skywars2.game.items.drops.LbcPisaDropItem.register();
        me.skywars2.game.items.drops.LbcUltraponosDropItem.register();
        me.skywars2.game.items.gadgets.LbcReactiveItems.register();
        me.skywars2.game.items.drops.LbcNpcDropItems.register();
        me.skywars2.game.entities.mobs.LbcStalinDropItem.register();
        me.skywars2.game.items.drops.LbcAntigravityDropItem.register();
        LbcCraftingRecipe.register();
        FullInvisibility.register();
        me.skywars2.game.items.phone.TikTokAccounts.register();
        me.skywars2.game.genie.GenieService.registerCommands();
        PolymerResourcePackUtils.addModAssets(MOD_ID);
        // Load the vanilla remote-slot implementation before the first player's inventory opens.
        ServerLifecycleEvents.SERVER_STARTING.register(server ->
                new net.minecraft.world.inventory.RemoteSlot.Synchronized(component -> 0));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var config = GameConfig.load();
            var players = PlayerStore.load();
            var maps = new MapManager(server,()->game!=null&&game.server()==server?game.gameMode():config.gameMode);
            game = new GameController(server, config, players, maps);
            game.menus(new GameMenus(server, game, config, players, maps));
            game.itemQueue().initialize();
            game.startServer();
            game.items().nexusCompass().reload();game.items().nexusCombat().reload();game.items().allItemPools().initialize();
            game.items().academy().prepare();
            game.items().stone().prepare();
            game.items().bedrock().prepare();
            game.items().wands().prepare();
            game.items().stonekivi().prepare();
            game.items().kiuas().prepare();
            game.items().ruby().prepare();
            game.items().sauvojen().prepare();
            game.items().consuming().prepare();
            game.items().duplicating().prepare();
            game.items().bolts().prepare();
            game.items().mobgun().prepare();
            game.items().magic().refreshEntityIndexes();
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success && game != null) {
                game.items().nexusCompass().reload();game.items().nexusCombat().reload();
                game.items().tmt().reload();game.items().allItemPools().reload();
                game.items().magic().refreshEntityIndexes();
                game.items().academy().prepare();
                game.items().stone().prepare();
                game.items().bedrock().prepare();
                game.items().wands().prepare();
                game.items().stonekivi().prepare();
                game.items().kiuas().prepare();
                game.items().ruby().prepare();
                game.items().sauvojen().prepare();
                game.items().consuming().prepare();
                game.items().duplicating().prepare();
                game.items().bolts().prepare();
                game.items().mobgun().prepare();
            }
        });
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            me.skywars2.game.combat.LbcAcceptedHits.clear();
            me.skywars2.game.net.metrics.TpsCompensation.beginTick(server);
            me.skywars2.game.effects.SignedEnvironmentalEffects.beginTick();
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> me.skywars2.game.net.ServerNetworkTransport.endTick(server,()->{
            if (game != null) {
                game.tick();
                if (nativeChecks != null) nativeChecks.tick(game);
                game.items().psycho().audioMaintenance();
            }
            me.skywars2.game.effects.SignedEnvironmentalEffects.tick();
            me.skywars2.game.effects.SignedEffectVisuals.tick(server);
            me.skywars2.game.presentation.projectiles.WeaponSpreadVisuals.tick(server);
            if(game!=null)game.vanity().tick();
            if(game!=null)game.itemQueue().endTick();
            AtlasGlitchSignal.tick(server);
            me.skywars2.game.net.PlayerLatency.tick(server);
        }));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (MatchActorCheckpoint.restoring(entity)) return;
            if (game != null && game.items().polymorph().nativeTransition(entity)) return;
            if(game!=null) game.cancelQueuedEntityUnload(entity);
            // Вход с сохранённым эффектом: active_effects загружается без onEffectAdded.
            if (entity instanceof ServerPlayer player) FullInvisibility.effectsChanged(player);
            if (game != null) { game.entityLoaded(entity); game.itemQueue().loaded(entity); }
            if (game != null && game.inLobby()) game.maps().queueLobbyEntity(entity);
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (MatchActorCheckpoint.restoring(entity)) return;
            if (game != null && game.items().polymorph().nativeTransition(entity)) return;
            if (game != null && entity instanceof net.minecraft.world.entity.LivingEntity living)
                game.items().indirectKills().clear(living);
            if (game != null) { game.itemQueue().unloaded(entity); game.queueEntityUnload(entity); }
        });
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
            if(game!=null)game.developerTools().gallery().chunkLoaded(level,chunk);
            if(game!=null)game.vanity().chunkLoaded(chunk);
            if (game != null && level == game.maps().level()) game.items().magic().chunkLoaded(chunk);
            if (game != null && level == game.maps().level()) game.items().bedrock().chunkLoaded(chunk);
            if (game != null && level == game.maps().level()) game.items().academy().chunkLoaded(chunk);
            if (game != null && level == game.maps().level()) game.items().tanner().chunkLoaded(chunk);
            if (game != null && level == game.maps().level()) game.items().kiuas().chunkLoaded(chunk);
        });
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            if (game != null && level == game.maps().level()) game.items().magic().chunkUnloaded(chunk);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            me.skywars2.game.effects.SignedEnvironmentalEffects.clear();
            me.skywars2.game.effects.SignedEffectVisuals.stop(server);
            if (game != null) { game.shutdown(); game = null; }
            AtlasGlitchSignal.stop(server);
            me.skywars2.game.presentation.projectiles.WeaponSpreadVisuals.stop();
            me.skywars2.game.net.PlayerLatency.stop();
        });
        ServerPlayerEvents.JOIN.register(player -> { if (game != null) { game.items().mana().initializeResources(player); game.join(player); } me.skywars2.game.effects.SignedEffectVisuals.joined(player); });
        ServerPlayerEvents.LEAVE.register(player -> {
            AtlasGlitchSignal.leaving(player);
            me.skywars2.game.net.PlayerLatency.forget(player);
            me.skywars2.game.presentation.projectiles.WeaponSpreadVisuals.forget(player);
            FullInvisibility.cameraChanging(player, null);
            me.skywars2.game.effects.SignedEffectVisuals.clear(player);
            if (game != null) game.leave(player);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, player, alive) -> {
            me.skywars2.game.presentation.projectiles.WeaponSpreadVisuals.forget(oldPlayer);
            me.skywars2.game.presentation.projectiles.WeaponSpreadVisuals.forget(player);
            // 26.3 assigns the listener after PlayerList.respawn returns; callbacks teleport now.
            player.connection.player = player;
            // При респауне объект игрока заменяется, а UUID остаётся: старый доступ камеры не переносится.
            FullInvisibility.cameraChanging(oldPlayer, null);
            if (game != null) { game.items().fpvDrone().forget(oldPlayer); game.items().fpvDrone().forget(player); game.respawned(player); }
            me.skywars2.game.effects.SignedEffectVisuals.joined(player);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damage) -> {
            if(game!=null)game.items().radar().forget(entity);
            if(game!=null&&entity instanceof ServerPlayer player)game.items().fpvDrone().forget(player);
            me.skywars2.game.combat.LbcAcceptedHits.lethal(entity,damage);
            if (game != null) game.items().specialTools().died(entity,damage);
            if (game != null) game.items().exoperso().died(entity, damage);
            if (game != null) game.items().helicopterEncounter().died(entity, damage);
            if (game != null && entity instanceof ServerPlayer player) game.items().witchWrath().died(player);
            if (game != null && entity instanceof ServerPlayer player) game.items().magic().clear(player);
            if (game != null) game.items().frost().clear(entity);
            if (game != null && entity instanceof ServerPlayer player) { game.items().steampunkHook().reset(player);game.items().steampunkGlove().reset(player);game.died(player, damage); }
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity,source,baseDamage,damage,blocked) ->
                me.skywars2.game.combat.LbcAcceptedHits.nonlethal(entity,source,damage,blocked));
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, damage, amount) -> {
            if(me.skywars2.game.entities.LbcDimensionItems.nexus(entity.level())
                    &&damage.is(net.minecraft.tags.DamageTypeTags.IS_FALL))return false;
            if(game!=null&&LbcDamage.isElectricalContact(damage)&&game.items().fpvDrone().tryShortCircuit(entity))return false;
            boolean allowed=game==null ? LbcDamage.allowDamage(entity,damage) : game.items().allowDamage(entity,damage);
            if(allowed && game!=null && amount>0) game.items().academy().beforeDamage(entity,damage);
            return allowed;
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if(player instanceof ServerPlayer owner&&me.skywars2.game.entities.LbcDimensionItems.nexus(level)){
                if(game!=null&&(game.vanity().blocked(owner)||game.items().actionsBlocked(owner)||game.isObserver(owner)))return InteractionResult.FAIL;
                var tool=nexusToolUse(owner,hand);
                if(tool!=InteractionResult.PASS)return tool;
                return me.skywars2.game.entities.LbcDimensionItems.allowed(owner,owner.getItemInHand(hand))?InteractionResult.PASS:InteractionResult.FAIL;
            }
            if(game!=null&&player instanceof ServerPlayer owner
                    &&!me.skywars2.game.players.inventory.LbcUseInteractions.forcedUse(owner)
                    &&!me.skywars2.game.players.inventory.LbcUseInteractions.shieldHandoff(owner,hand)
                    &&game.items().academy().tryUse(owner,hand))return InteractionResult.SUCCESS;
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.items.tmt.LbcTmtItems.matches(owner.getItemInHand(hand))&&!me.skywars2.game.items.tmt.LbcTmtItems.active(owner.getItemInHand(hand))){
                if(game.vanity().blocked(owner)||!game.items().tmt().admitted(owner))return InteractionResult.FAIL;
                return game.items().genie().runtime().replaceUse(owner,hand);
            }
            var bw=bedwarsUse(player,hand,null);if(bw!=InteractionResult.PASS)return bw;
            var touhou=touhouUse(player,hand);if(touhou!=InteractionResult.PASS)return touhou;
            var nx=nexusToolUse(player,hand);if(nx!=InteractionResult.PASS)return nx;
            var seasonal=seasonalUse(player,hand);if(seasonal!=InteractionResult.PASS)return seasonal;
            if(game!=null&&player instanceof ServerPlayer owner){
                if(me.skywars2.game.world.terrain.LbcCunnyItems.nahida(owner.getItemInHand(hand)))return game.vanity().blocked(owner)?InteractionResult.FAIL:InteractionResult.PASS;
                if(me.skywars2.game.world.terrain.LbcCunnyItems.slab(owner.getItemInHand(hand))||me.skywars2.game.items.ItemData.intValue(owner.getItemInHand(hand),"cunny_slab",0)==1){
                    if(game.vanity().blocked(owner))return InteractionResult.FAIL;
                    return game.items().useCunny(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
                }
            }
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.items.ItemData.intValue(owner.getItemInHand(hand),"mellstroy_mask",0)==1){
                if(game.vanity().blocked(owner))return InteractionResult.FAIL;
                return game.items().useMellstroy(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
            if(game!=null&&player instanceof ServerPlayer owner&&LbcBalanceItems.matches(owner.getItemInHand(hand)))
                return game.items().balance().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner&&game.vanity().blocked(owner))return InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner){
                var scripted=game.items().genie().runtime().replaceUse(owner,hand);
                if(scripted!=InteractionResult.PASS)return scripted;
            }
            if(game!=null){var utility=me.skywars2.game.items.gadgets.LbcImbaTerrainUtilityItems.legacyAir(level,player,hand);if(utility!=InteractionResult.PASS)return utility;}
            if (game != null && player instanceof ServerPlayer owner && game.developerTools().useItem(owner, hand)) return InteractionResult.SUCCESS;
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.items.gadgets.LbcWoolTrailItems.matches(player.getItemInHand(hand))
                    &&!me.skywars2.game.items.gadgets.LbcWoolTrailItems.allowed(game,owner,hand))return InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.items.gadgets.LbcThrownUtilities.matches(player.getItemInHand(hand)))
                return game.items().thrownUtilities().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.items.gadgets.LbcGodDice.matches(player.getItemInHand(hand)))
                return me.skywars2.game.items.gadgets.LbcGodDice.use(game,owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.entities.projectiles.LbcWhiteArrows.matches(player.getItemInHand(hand)))
                return game.items().whiteArrows().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.items.gadgets.LbcTeleportGadgets.matches(player.getItemInHand(hand)))
                return game.items().teleportGadgets().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.entities.projectiles.LbcBouncingPoop.matches(player.getItemInHand(hand)))
                return game.items().bouncingPoop().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.skills.kits.LbcClownKit.matches(player.getItemInHand(hand)))
                return game.items().clownKit().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner&&(me.skywars2.game.items.weapons.melee.LbcDeus.matches(player.getItemInHand(hand))||me.skywars2.game.items.tools.LbcSteampunkHook.matches(player.getItemInHand(hand))||me.skywars2.game.items.gadgets.LbcKitUtilities.matches(player.getItemInHand(hand))))
                return game.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner&&player.getItemInHand(hand).getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getIntOr("supply_beacon",0)==1){
                var eye=player.getEyePosition();var end=eye.add(player.getViewVector(1).scale(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE)));
                var hit=level.clip(new net.minecraft.world.level.ClipContext(eye,end,net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY,player));
                if(hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&level.getBlockState(hit.getBlockPos()).getBlock() instanceof net.minecraft.world.level.block.LiquidBlock){
                    if(!level.mayInteract(player,hit.getBlockPos())||!player.mayUseItemAt(hit.getBlockPos(),hit.getDirection(),player.getItemInHand(hand)))return InteractionResult.FAIL;
                    if(game.items().shishEncounter().useBeacon(owner,hand,net.minecraft.world.phys.Vec3.atBottomCenterOf(hit.getBlockPos()))){player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(player.getItemInHand(hand).getItem()));return InteractionResult.SUCCESS;}
                    return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            }
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.entities.mobs.LbcShishEncounter.matches(player.getItemInHand(hand)))
                return game.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(game!=null && player instanceof ServerPlayer owner && me.skywars2.game.items.weapons.melee.LbcMetalPipe.divider(player.getItemInHand(hand)))
                return game.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(player instanceof ServerPlayer owner&&!me.skywars2.game.entities.LbcDimensionItems.allowed(owner,player.getItemInHand(hand)))return InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner&&(me.skywars2.game.entities.mobs.bobs.LbcBigBob.matches(player.getItemInHand(hand))
                    ||me.skywars2.game.items.wands.LbcUniwand.matches(player.getItemInHand(hand))||me.skywars2.game.world.terrain.LbcBoba.matches(player.getItemInHand(hand))||me.skywars2.game.items.gadgets.LbcImbaRunes.matches(player.getItemInHand(hand))||me.skywars2.game.items.drops.LbcChaosRewardItems.matches(player.getItemInHand(hand))))
                return game.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            // Authored raw spell leaves retain their vanilla carrier, so enter the same two-hand dispatcher here.
            if(game!=null&&player instanceof ServerPlayer owner&&(me.skywars2.game.items.wands.LbcLibrarySpells.matchesRawRaycast(player.getItemInHand(hand))||me.skywars2.game.items.tools.LbcRewardTools.matches(player.getItemInHand(hand))||me.skywars2.game.items.loot.LbcLooseRewards.luckyWand(player.getItemInHand(hand))||me.skywars2.game.entities.drones.LbcFpvDrone.matches(player.getItemInHand(hand))||me.skywars2.game.skills.witch.LbcWitchGluttony.matches(player.getItemInHand(hand))||me.skywars2.game.skills.witch.LbcWitchIndolence.matches(player.getItemInHand(hand))||me.skywars2.game.skills.witch.LbcWitchClaims.matches(player.getItemInHand(hand))||me.skywars2.game.skills.witch.LbcWitchLust.matches(player.getItemInHand(hand))))
                return game.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.entities.mobs.hedgehogs.LbcHedgehogSpikes.legacyThrow(player.getItemInHand(hand))!=0)
                return game.items().hedgehogSpikes().useLegacy(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            // Original accessory/reforge stacks keep their vanilla carrier and enter the native two-hand dispatcher.
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.combat.LbcPendingCombat.isPiston(player.getItemInHand(hand)))
                return game.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.items.gadgets.LbcPopit.matches(player.getItemInHand(hand)))
                return game.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.items.weapons.melee.LbcChargedSwords.matches(player.getItemInHand(hand))
                    &&!player.getItemInHand(hand).is(me.skywars2.game.items.weapons.melee.LbcChargedSwords.TRIPLE)&&!player.getItemInHand(hand).is(me.skywars2.game.items.weapons.melee.LbcChargedSwords.BLAZE))
                return game.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.items.wands.LbcFevgaleaStar.isSword(player.getItemInHand(hand))
                    &&!player.getItemInHand(hand).is(me.skywars2.game.items.wands.LbcFevgaleaStar.SWORD))
                return game.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && (me.skywars2.game.items.tools.LbcSpecialTools.isLegacyScythe(player.getItemInHand(hand)) || me.skywars2.game.items.tools.LbcSpecialTools.isLegacyHammer(player.getItemInHand(hand))))
                return game.items().wands().use(owner,hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && game.items().actionsBlocked(player)) return InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer serverPlayer && game.canUseMenus(serverPlayer)
                    && player.getItemInHand(hand).is(Items.COMPASS)) {
                game.menus().open(serverPlayer); return InteractionResult.SUCCESS;
            }
            if (game != null && player instanceof ServerPlayer serverPlayer && game.isObserver(serverPlayer)) {
                if (player.getItemInHand(hand).is(Items.PLAYER_HEAD)) game.spectators().openTargets(serverPlayer);
                return InteractionResult.FAIL;
            }
            if (game != null && player instanceof ServerPlayer owner && LbcShooters.isPine(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(LbcShooters.PINE))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && LbcShooters.isPhantomKnives(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(LbcShooters.PHANTOM_KNIVES))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && LbcStand.isClocks(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(LbcStand.CLOCKS))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && LbcHomeItems.isMirror(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(LbcHomeItems.MIRROR))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && LbcStand.isOraora(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(LbcStand.ORAORA))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && LbcStand.isHomingLightning(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(LbcStand.HOMING_LIGHTNING))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner
                    && me.skywars2.game.items.weapons.firearms.LbcMobgun.isMobgun(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.items.weapons.firearms.LbcMobgun.ITEM))
                return game.items().wands().use(owner,hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner
                    && me.skywars2.game.items.wands.LbcWands.isLifestealer(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.items.wands.LbcWands.LIFESTEALER)
                    && !player.getItemInHand(hand).is(me.skywars2.game.items.wands.LbcWands.ACADEMY_LIFESTEALER))
                return game.items().wands().use(owner,hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            // Старые стеки из перековки используют тот же ввод, что и нативная Polymer-винтовка.
            if (game != null && player instanceof ServerPlayer owner && LbcShooters.isKfcGun(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(LbcShooters.KFC_GUN))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && LbcShooters.isSlr(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(LbcShooters.SLR))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && LbcShooters.isWoodificator(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(LbcShooters.WOODIFICATOR))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && LbcShooters.isHelicopter(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(LbcShooters.HELICOPTER))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && LbcShooters.isBossHelicopter(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(LbcShooters.BOSS_HELICOPTER))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && me.skywars2.game.entities.projectiles.LbcGovnomet.isGovnomet(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.entities.projectiles.LbcGovnomet.ITEM))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && me.skywars2.game.items.weapons.firearms.LbcMagnum.isMagnum(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.items.weapons.firearms.LbcMagnum.ITEM))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && me.skywars2.game.items.weapons.firearms.LbcAk47.isAk47(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.items.weapons.firearms.LbcAk47.ITEM))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if(game!=null && player instanceof ServerPlayer owner && me.skywars2.game.entities.mobs.LbcHohlopitek.isItem(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.entities.mobs.LbcHohlopitek.ITEM))
                return game.items().wands().use(owner,hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && me.skywars2.game.items.weapons.firearms.LbcLaserMinigun.isMinigun(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.items.weapons.firearms.LbcLaserMinigun.ITEM))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if(game!=null && player instanceof ServerPlayer owner && me.skywars2.game.items.food.LbcKfcRewards.isCultus(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.items.food.LbcKfcRewards.PENIS_CULTUS))
                return game.items().wands().use(owner,hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if(game!=null && player instanceof ServerPlayer owner && LbcElectro.isAnyKatana(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(LbcElectro.CYBERKATANA)
                    && !player.getItemInHand(hand).is(LbcElectro.SHIT_KATANA))
                return game.items().wands().use(owner,hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            // Перенесённая способность на другом предмете использует тот же обработчик, что native Polymer-меч.
            if(game!=null && player instanceof ServerPlayer serverPlayer && LbcTerra.isRazor(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(LbcTerra.RAZOR))
                return game.items().terra().useRazor(serverPlayer,hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer serverPlayer
                    && me.skywars2.game.items.wands.LbcTerra.isSword(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.items.wands.LbcTerra.SWORD)
                    && !player.getItemInHand(hand).is(me.skywars2.game.items.wands.LbcTerra.TRUE_SWORD))
                return game.items().terra().useSword(serverPlayer, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer serverPlayer
                    && (LbcItems.isDynamite(player.getItemInHand(hand)) || LbcItems.isNanobot(player.getItemInHand(hand))
                        || LbcItems.isChemicalPotion(player.getItemInHand(hand)))
                    && !game.canUseGameplayItems(serverPlayer))
                return InteractionResult.FAIL;
            return InteractionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if(player instanceof ServerPlayer owner&&me.skywars2.game.entities.LbcDimensionItems.ordinary(level))
                me.skywars2.game.items.tmt.LbcTmtTriggers.input(owner,me.skywars2.game.items.tmt.LbcTmtTriggers.Input.LEFT_CLICK);
            if(player instanceof ServerPlayer owner&&!me.skywars2.game.entities.LbcDimensionItems.allowed(owner,player.getItemInHand(hand)))return InteractionResult.FAIL;
            if (game != null && game.items().actionsBlocked(player)) return InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer serverPlayer && game.isObserver(serverPlayer)) return InteractionResult.FAIL;
            if (game != null && game.inLobby() && player instanceof ServerPlayer serverPlayer
                    && !game.isDeveloper(serverPlayer)
                    && level.getBlockState(pos).is(Blocks.ENDER_CHEST)) {
                game.menus().openFromChest(serverPlayer, pos, false); return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if(player instanceof ServerPlayer owner&&me.skywars2.game.entities.LbcDimensionItems.nexus(level))
                return game!=null&&(game.items().actionsBlocked(owner)||game.isObserver(owner))?InteractionResult.FAIL:InteractionResult.PASS;
            if(game!=null&&player instanceof ServerPlayer owner&&game.items().academy().tryUse(owner,hand))return InteractionResult.SUCCESS;
            var bw=bedwarsUse(player,hand,hit);if(bw!=InteractionResult.PASS)return bw;
            // Air-use abilities run from UseItemCallback. Fabric invokes this callback
            // before the clicked block can open its menu or handle its own interaction.
            if (game != null && player instanceof ServerPlayer owner && game.developerTools().useBlock(owner, hand, hit)) return InteractionResult.SUCCESS;
            if(player instanceof ServerPlayer owner&&!me.skywars2.game.entities.LbcDimensionItems.allowed(owner,player.getItemInHand(hand)))return InteractionResult.FAIL;
            if (game != null && game.items().actionsBlocked(player)) return InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer serverPlayer && game.isObserver(serverPlayer)) return InteractionResult.FAIL;
            if(game!=null){var slab=me.skywars2.game.modes.legacy.LbcLegacySlabs.legacyUseOn(new net.minecraft.world.item.context.UseOnContext(player,hand,hit));if(slab!=InteractionResult.PASS)return slab;}
            if(game!=null){var utility=me.skywars2.game.items.gadgets.LbcImbaTerrainUtilityItems.legacyBlock(new net.minecraft.world.item.context.UseOnContext(player,hand,hit));if(utility!=InteractionResult.PASS){
                if(player instanceof ServerPlayer owner)LbcUseInteractions.shieldAfter(owner.gameMode,owner,level,hand,utility);
                return utility;
            }}
            if(game!=null&&player instanceof ServerPlayer owner&&player.getItemInHand(hand).getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getIntOr("supply_beacon",0)==1){
                var pos=hit.getBlockPos();if(!level.getBlockState(pos).getCollisionShape(level,pos).isEmpty())pos=pos.relative(hit.getDirection());
                var result=game.items().shishEncounter().useBeacon(owner,hand,net.minecraft.world.phys.Vec3.atBottomCenterOf(pos))?InteractionResult.SUCCESS:InteractionResult.FAIL;
                LbcUseInteractions.shieldAfter(owner.gameMode,owner,level,hand,result);
                return result;
            }
            if (game != null && player instanceof ServerPlayer serverPlayer
                    && LbcItems.isNanobot(player.getItemInHand(hand)) && !game.canUseGameplayItems(serverPlayer))
                return InteractionResult.FAIL;
            if (game != null && game.inLobby() && player instanceof ServerPlayer serverPlayer
                    && !game.isDeveloper(serverPlayer)
                    && level.getBlockState(hit.getBlockPos()).is(Blocks.ENDER_CHEST)) {
                game.menus().openFromChest(serverPlayer, hit.getBlockPos(), false); return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if(player instanceof ServerPlayer owner&&me.skywars2.game.entities.LbcDimensionItems.nexus(level)){
                if(game!=null&&(game.vanity().blocked(owner)||game.items().actionsBlocked(owner)||game.isObserver(owner)))return InteractionResult.FAIL;
                if(game!=null){var cage=game.items().nexusAcquisition().use(owner,hand,entity);if(cage!=InteractionResult.PASS)return cage;}
                return me.skywars2.game.entities.LbcDimensionItems.allowed(owner,owner.getItemInHand(hand))?InteractionResult.PASS:InteractionResult.FAIL;
            }
            if(game!=null&&player instanceof ServerPlayer owner&&game.items().academy().interact(owner,hand,entity))return InteractionResult.SUCCESS;
            if(player instanceof ServerPlayer owner)LbcUseInteractions.entityPress(owner,hand,entity);
            if(me.skywars2.game.modes.bedwars.LbcBedwars.shopRole(entity))return game!=null&&player instanceof ServerPlayer owner&&hand==net.minecraft.world.InteractionHand.MAIN_HAND&&game.bedwars().shop().open(owner,entity)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            var bw=bedwarsUse(player,hand,null);if(bw!=InteractionResult.PASS)return bw;
            if(game!=null&&player instanceof ServerPlayer owner){var result=game.items().nexusAcquisition().use(owner,hand,entity);if(result!=InteractionResult.PASS)return result;}
            if (game != null && player instanceof ServerPlayer owner && game.developerTools().useEntity(owner, entity)) return InteractionResult.SUCCESS;
            if(player instanceof ServerPlayer owner&&!me.skywars2.game.entities.LbcDimensionItems.allowed(owner,player.getItemInHand(hand)))return InteractionResult.FAIL;
            if (game != null && game.items().actionsBlocked(player)) return InteractionResult.FAIL;
            // Matching-mob spawn eggs reach vanilla offspring interaction before Item.useOn.
            if (me.skywars2.game.entities.mobs.summons.LbcSpecialEggs.matches(player.getItemInHand(hand))
                    && !me.skywars2.game.entities.mobs.summons.LbcSpecialEggs.allowed(game,player)) return InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer serverPlayer && game.isObserver(serverPlayer)) {
                game.spectators().watch(serverPlayer, entity); return InteractionResult.SUCCESS;
            }
            if(game!=null&&player instanceof ServerPlayer owner&&game.items().shishEncounter().interact(owner,entity,hand))return InteractionResult.SUCCESS;
            // A summon order takes priority over feeding, sitting, mounting or opening a mob UI.
            if(game!=null && player instanceof ServerPlayer serverPlayer && player.isShiftKeyDown()
                    && me.skywars2.game.entities.mobs.summons.LbcSummons.commandItem(player.getItemInHand(hand)))
                return game.items().wands().use(serverPlayer,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if (game != null && game.inLobby() && player instanceof ServerPlayer serverPlayer
                    && entity.entityTags().contains("swrg.quickstart")) {
                game.requestStart(serverPlayer, true); return InteractionResult.SUCCESS;
            }
            if(game!=null&&player instanceof ServerPlayer owner){
                var crystal=game.items().interactIceCrystal(owner,hand,entity,hit==null?null:hit.getLocation());
                if(crystal!=InteractionResult.PASS)return crystal;
            }
            return InteractionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (game != null && game.developerTools().gallery().protectEntity(entity)) return InteractionResult.FAIL;
            if(player instanceof ServerPlayer owner&&!me.skywars2.game.entities.LbcDimensionItems.allowed(owner,player.getItemInHand(hand)))return InteractionResult.FAIL;
            if(game==null)return InteractionResult.PASS;
            if(game.items().actionsBlocked(player)||player instanceof ServerPlayer observer&&game.isObserver(observer))return InteractionResult.FAIL;
            if(player instanceof ServerPlayer serverPlayer&&game.items().attackIceCrystal(serverPlayer,entity))return InteractionResult.SUCCESS;
            return InteractionResult.PASS;
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> {
            DeveloperTools.registerCommands(dispatcher);
            me.skywars2.game.commands.BulkPlayerCommand.register(dispatcher);
            var root = Commands.literal("skywars").executes(ctx -> {
                if (game != null) game.menus().open(ctx.getSource().getPlayerOrException()); return 1;
            });
            root.then(Commands.literal("status").executes(ctx -> {
                ctx.getSource().sendSuccess(() -> game == null ? text("message.starting") : game.status(), false); return 1;
            }));
            root.then(Commands.literal("inventory").executes(ctx -> {
                if (game != null) game.spectators().openInventory(ctx.getSource().getPlayerOrException()); return 1;
            }));
            root.then(Commands.literal("detach").executes(ctx -> {
                if (game != null) game.spectators().detach(ctx.getSource().getPlayerOrException()); return 1;
            }));
            root.then(Commands.literal("start").executes(ctx -> {
                if (game != null) game.requestStart(ctx.getSource().getPlayer(), false); return 1;
            }));
            root.then(Commands.literal("quick").executes(ctx -> {
                if (game != null) game.requestStart(ctx.getSource().getPlayer(), true); return 1;
            }));
            root.then(Commands.literal("map").then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> game != null && game.selectMap(ctx.getSource().getPlayer(), StringArgumentType.getString(ctx, "id"), false) ? 1 : 0)
                    .then(Commands.literal("broken").executes(ctx -> game != null && game.selectMap(ctx.getSource().getPlayer(), StringArgumentType.getString(ctx, "id"), true) ? 1 : 0))));
            root.then(Commands.literal("clearmap").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes(ctx->game==null?0:game.clearArena(ctx.getSource())));
            root.then(Commands.literal("stop").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
                if (game != null) game.stopMatch(); return 1;
            }));
            if (nativeChecks != null) root.then(Commands.literal("check").requires(source->source.getEntity()==null&&Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source))
                    .then(Commands.argument("suite", StringArgumentType.word()).executes(ctx -> {
                        if (game != null) nativeChecks.run(game, StringArgumentType.getString(ctx, "suite")); return 1;
                    })));
            root.then(Commands.literal("lbc-unlock").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                    .then(Commands.argument("advancement", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(LbcLoadouts.unlockIds(), builder))
                    .executes(ctx -> {
                        var target = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                        String id = StringArgumentType.getString(ctx, "advancement");
                        if (game == null || !game.lbc().unlock(target, id)) {
                            ctx.getSource().sendFailure(text("settings.unknown", id)); return 0;
                        }
                        ctx.getSource().sendSuccess(() -> text("settings.saved", "lbcUnlock", id), false);
                        return 1;
                    }))));
            me.skywars2.game.commands.SkyWarsCommandTree.add(dispatcher,Commands.literal("settings").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes(ctx -> {
                        if (game == null) { ctx.getSource().sendFailure(text("message.starting")); return 0; }
                        ctx.getSource().sendSuccess(() -> game.settingsSummary(), false);
                        return 1;
                    })
                    .then(Commands.literal("event")
                            .then(Commands.argument("event", StringArgumentType.word())
                                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                            List.of("halloween", "november", "tournament"), builder))
                                    .then(Commands.argument("enabled", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                            .executes(ctx -> {
                                                if (game == null) { ctx.getSource().sendFailure(text("message.starting")); return 0; }
                                                try {
                                                    Component result = game.configureLobbyEvent(StringArgumentType.getString(ctx, "event"),
                                                            Boolean.toString(com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "enabled")));
                                                    ctx.getSource().sendSuccess(() -> result, false);
                                                    return 1;
                                                } catch (GameConfig.SettingError e) {
                                                    ctx.getSource().sendFailure(e.component());
                                                    return 0;
                                                }
                                            }))))
                    .then(Commands.argument("parameter", StringArgumentType.word())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of(
                                    "lives", "health", "healthDisplay", "imbaDropChance", "prefixes", "countdownSeconds", "stageOneSeconds", "stageTwoSeconds", "endingSeconds",
                                    "stageOneEvent", "stageTwoEvent", "pvpMode", "tntProtection", "luckyModifier", "day", "ranked", "synchroRanked", "vanillaKits", "vanillaSkills", "lbcKits", "lbcSkills", "lbcLevelups", "lbcLevelupLimit", "lbcTrinketLimit", "lbcChallenges", "offhandWands", "noBorders", "gameMode", "bedwarsDirectBedBreaking", "map", "broken"), builder))
                            .then(Commands.argument("value", StringArgumentType.word())
                                    .suggests((ctx, builder) -> settingValueSuggestions(
                                            StringArgumentType.getString(ctx, "parameter"), builder))
                                    .executes(ctx -> {
                                        if (game == null) { ctx.getSource().sendFailure(text("message.starting")); return 0; }
                                        try {
                                            Component result = game.configureLobbySetting(ctx.getSource().getPlayer(),
                                                    StringArgumentType.getString(ctx, "parameter"), StringArgumentType.getString(ctx, "value"));
                                            ctx.getSource().sendSuccess(() -> result, false);
                                            return 1;
                                        } catch (GameConfig.SettingError e) {
                                            ctx.getSource().sendFailure(e.component());
                                            return 0;
                                        }
                                    }))));
            dispatcher.register(root);
            me.skywars2.game.commands.SkyWarsFeedbackCommands.register(dispatcher);
            me.skywars2.game.net.metrics.ServerTickMetrics.register(dispatcher);
            me.skywars2.game.net.PlayerLatency.register(dispatcher);
        });
        LOGGER.info("SkyWars game cycle registered");
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> settingValueSuggestions(
            String parameter, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        if (game == null) return builder.buildFuture();
        String key = parameter.toLowerCase(java.util.Locale.ROOT);
        if (List.of("ranked", "synchroranked", "vanillakits", "vanillaskills", "lbckits", "lbcskills", "lbclevelups", "lbcchallenges", "offhandwands", "noborders", "bedwarsdirectbedbreaking", "broken").contains(key))
            return SharedSuggestionProvider.suggest(List.of("true", "false"), builder);
        if (key.equals("map"))
            return SharedSuggestionProvider.suggest(game.maps().catalog().stream().filter(MapManager.MapInfo::available)
                    .map(MapManager.MapInfo::id), builder);
        List<String> values = switch (key) {
            case "lives" -> List.of("1", "2", "3", "4", "5");
            case "health" -> List.of("2", "10", "20", "40", "60");
            case "healthdisplay" -> List.of("0", "1", "2", "3");
            case "imbadropchance" -> List.of("0", "1", "2", "3", "4", "5", "6");
            case "prefixes" -> List.of("0", "1", "2", "3", "4");
            case "countdownseconds" -> List.of("0", "3", "5", "10", "30", "60");
            case "stageoneseconds" -> List.of("0", "60", "120", "180", "300", "600", "1200", "3600");
            case "stagetwoseconds" -> List.of("0", "120", "240", "360", "600", "1200", "3600", "7200");
            case "endingseconds" -> List.of("0", "5", "10", "30", "60", "120");
            case "stageoneevent" -> List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
            case "stagetwoevent" -> List.of("0", "1", "2", "3", "4", "5", "6", "7", "8");
            case "pvpmode" -> List.of("1", "2");
            case "tntprotection" -> List.of("0", "1", "2", "3", "4", "5");
            case "luckymodifier" -> List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
            case "day" -> List.of("1", "2", "3");
            default -> List.of();
        };
        return SharedSuggestionProvider.suggest(values, builder);
    }
}
