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

    @Override
    public void onInitializeServer() {
        // Здесь связываются жизненный цикл сервера, игровые события и команды; состояние матча хранит контроллер.
        MapManager.registerLoadingTicket();
        LbcHomes.registerTicket();
        LbcPortalDrops.registerTicket();
        LbcItems.register();
        LbcPortalDropItem.register();
        LbcLuckyRoomDropItem.register();
        me.skywars2.game.LbcPisaDropItem.register();
        me.skywars2.game.LbcUltraponosDropItem.register();
        me.skywars2.game.LbcReactiveItems.register();
        me.skywars2.game.LbcNpcDropItems.register();
        me.skywars2.game.LbcStalinDropItem.register();
        me.skywars2.game.LbcAntigravityDropItem.register();
        LbcCraftingRecipe.register();
        FullInvisibility.register();
        PolymerResourcePackUtils.addModAssets(MOD_ID);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var config = GameConfig.load();
            var players = PlayerStore.load();
            var maps = new MapManager(server);
            game = new GameController(server, config, players, maps);
            game.menus(new GameMenus(server, game, config, players, maps));
            game.startServer();
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
        ServerTickEvents.START_SERVER_TICK.register(server -> me.skywars2.game.SignedEnvironmentalEffects.beginTick());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (game != null) { game.tick(); GameChecks.tick(game); }
            me.skywars2.game.SignedEnvironmentalEffects.tick();
            me.skywars2.game.SignedEffectVisuals.tick(server);
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if(game!=null) game.cancelQueuedEntityUnload(entity);
            // Вход с сохранённым эффектом: active_effects загружается без onEffectAdded.
            if (entity instanceof ServerPlayer player) FullInvisibility.effectsChanged(player);
            if (game != null) game.items().entityLoaded(entity);
            if (game != null && game.inLobby()) game.maps().queueLobbyEntity(entity);
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (game != null && entity instanceof net.minecraft.world.entity.LivingEntity living)
                game.items().indirectKills().clear(living);
            if (game != null) game.queueEntityUnload(entity);
        });
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
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
            me.skywars2.game.SignedEnvironmentalEffects.clear();
            me.skywars2.game.SignedEffectVisuals.stop(server);
            if (game != null) { game.shutdown(); game = null; }
        });
        ServerPlayerEvents.JOIN.register(player -> { if (game != null) game.join(player); me.skywars2.game.SignedEffectVisuals.joined(player); });
        ServerPlayerEvents.LEAVE.register(player -> {
            FullInvisibility.cameraChanging(player, null);
            me.skywars2.game.SignedEffectVisuals.clear(player);
            if (game != null) game.leave(player);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, player, alive) -> {
            // 26.3 assigns the listener after PlayerList.respawn returns; callbacks teleport now.
            player.connection.player = player;
            // При респауне объект игрока заменяется, а UUID остаётся: старый доступ камеры не переносится.
            FullInvisibility.cameraChanging(oldPlayer, null);
            if (game != null) game.respawned(player);
            me.skywars2.game.SignedEffectVisuals.joined(player);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damage) -> {
            // Fabric пропускает AFTER_DAMAGE при смертельном ударе; испытания должны учитывать и такие попадания.
            if (game != null) game.items().stand().onAcceptedDamage(entity, damage);
            if (game != null) game.items().academy().damaged(entity, damage);
            if (game != null) game.items().vexSword().damaged(entity, damage, 1);
            if (game != null) game.items().fevgaleaStar().damaged(entity, damage);
            if (game != null) game.items().chargedSwords().damaged(entity, damage);
            if (game != null) game.items().karaSlabs().damaged(entity, damage);
            if (game != null) game.items().classicMelee().damaged(entity, damage);
            if (game != null) game.items().devilSword().damaged(entity, damage);
            if (game != null) game.items().pendingCombat().damaged(entity, damage);
            if (game != null) game.items().pendingSwords().damaged(entity, damage);
            if (game != null) game.items().incomingArtifacts().damaged(entity, damage);
            if (game != null) game.items().reactiveArtifacts().damaged(entity, damage);
            if (game != null && damage.getEntity() instanceof ServerPlayer owner) game.items().fartus().melee(owner,entity,damage);
            if (game != null) game.items().wtf().melee(entity,damage);
            if (game != null) { game.items().specialTools().melee(entity,damage); game.items().specialTools().died(entity,damage); }
            if (game != null) game.items().exoperso().died(entity, damage);
            if (game != null) game.items().helicopterEncounter().died(entity, damage);
            if (game != null && entity instanceof ServerPlayer player) game.items().magic().clear(player);
            if (game != null) game.items().frost().clear(entity);
            if (game != null && entity instanceof ServerPlayer player) game.died(player, damage);
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
            if (game != null && !blocked && damage > 0) game.items().academy().damaged(entity, source);
            if (game != null && !blocked && damage > 0) game.items().indirectKills().damaged(entity, source);
            if (game != null && !blocked && damage > 0) game.items().damaged(entity, source);
            if (game != null && !blocked && damage > 0) game.items().vexSword().damaged(entity, source, damage);
            if (game != null && !blocked && damage > 0 && source.getEntity() instanceof ServerPlayer owner) game.items().fartus().melee(owner,entity,source);
            if (game != null && !blocked && damage > 0) game.items().wtf().melee(entity,source);
            if (game != null && !blocked && damage > 0) game.items().trinkets().damaged(entity, source);
            if (game != null && !blocked && entity instanceof ServerPlayer player) game.hurtForSkill(player);
            if (game != null && !blocked && damage > 0 && entity instanceof ServerPlayer player)
                { game.items().ak47().receivedDamage(player,damage); game.damaged(player, source, damage); }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, damage, amount) -> {
            boolean allowed=game==null ? LbcDamage.allowDamage(entity,damage) : game.items().allowDamage(entity,damage);
            if(allowed && game!=null && amount>0) game.items().academy().beforeDamage(entity,damage);
            return allowed;
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            // Original accessory/reforge stacks keep their vanilla carrier and enter the native two-hand dispatcher.
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.LbcPendingCombat.isPiston(player.getItemInHand(hand)))
                return game.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.LbcPopit.matches(player.getItemInHand(hand)))
                return game.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.LbcChargedSwords.matches(player.getItemInHand(hand))
                    &&!player.getItemInHand(hand).is(me.skywars2.game.LbcChargedSwords.TRIPLE)&&!player.getItemInHand(hand).is(me.skywars2.game.LbcChargedSwords.BLAZE))
                return game.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(game!=null&&player instanceof ServerPlayer owner&&me.skywars2.game.LbcFevgaleaStar.isSword(player.getItemInHand(hand))
                    &&!player.getItemInHand(hand).is(me.skywars2.game.LbcFevgaleaStar.SWORD))
                return game.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && (me.skywars2.game.LbcSpecialTools.isLegacyScythe(player.getItemInHand(hand)) || me.skywars2.game.LbcSpecialTools.isLegacyHammer(player.getItemInHand(hand))))
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
                    && me.skywars2.game.LbcMobgun.isMobgun(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.LbcMobgun.ITEM))
                return game.items().wands().use(owner,hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner
                    && me.skywars2.game.LbcWands.isLifestealer(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.LbcWands.LIFESTEALER)
                    && !player.getItemInHand(hand).is(me.skywars2.game.LbcWands.ACADEMY_LIFESTEALER))
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
            if (game != null && player instanceof ServerPlayer owner && me.skywars2.game.LbcGovnomet.isGovnomet(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.LbcGovnomet.ITEM))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && me.skywars2.game.LbcMagnum.isMagnum(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.LbcMagnum.ITEM))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && me.skywars2.game.LbcAk47.isAk47(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.LbcAk47.ITEM))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if(game!=null && player instanceof ServerPlayer owner && me.skywars2.game.LbcHohlopitek.isItem(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.LbcHohlopitek.ITEM))
                return game.items().wands().use(owner,hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer owner && me.skywars2.game.LbcLaserMinigun.isMinigun(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.LbcLaserMinigun.ITEM))
                return game.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if(game!=null && player instanceof ServerPlayer owner && me.skywars2.game.LbcKfcRewards.isCultus(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.LbcKfcRewards.PENIS_CULTUS))
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
                    && me.skywars2.game.LbcTerra.isSword(player.getItemInHand(hand))
                    && !player.getItemInHand(hand).is(me.skywars2.game.LbcTerra.SWORD)
                    && !player.getItemInHand(hand).is(me.skywars2.game.LbcTerra.TRUE_SWORD))
                return game.items().terra().useSword(serverPlayer, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer serverPlayer
                    && (LbcItems.isDynamite(player.getItemInHand(hand)) || LbcItems.isNanobot(player.getItemInHand(hand))
                        || LbcItems.isChemicalPotion(player.getItemInHand(hand)))
                    && !game.canUseGameplayItems(serverPlayer))
                return InteractionResult.FAIL;
            return InteractionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (game != null && game.items().actionsBlocked(player)) return InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer serverPlayer && game.isObserver(serverPlayer)) return InteractionResult.FAIL;
            if (game != null && game.inLobby() && player instanceof ServerPlayer serverPlayer
                    && !game.isDeveloper(serverPlayer)
                    && level.getBlockState(pos).is(Blocks.ENDER_CHEST)) {
                game.menus().open(serverPlayer); return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (game != null && game.items().actionsBlocked(player)) return InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer serverPlayer && game.isObserver(serverPlayer)) return InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer serverPlayer
                    && LbcItems.isNanobot(player.getItemInHand(hand)) && !game.canUseGameplayItems(serverPlayer))
                return InteractionResult.FAIL;
            if (game != null && game.inLobby() && player instanceof ServerPlayer serverPlayer
                    && !game.isDeveloper(serverPlayer)
                    && level.getBlockState(hit.getBlockPos()).is(Blocks.ENDER_CHEST)) {
                game.menus().open(serverPlayer); return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (game != null && game.items().actionsBlocked(player)) return InteractionResult.FAIL;
            // Matching-mob spawn eggs reach vanilla offspring interaction before Item.useOn.
            if (me.skywars2.game.LbcSpecialEggs.matches(player.getItemInHand(hand))
                    && !me.skywars2.game.LbcSpecialEggs.allowed(game,player)) return InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer serverPlayer && game.isObserver(serverPlayer)) {
                game.spectators().watch(serverPlayer, entity); return InteractionResult.SUCCESS;
            }
            // A summon order takes priority over feeding, sitting, mounting or opening a mob UI.
            if(game!=null && player instanceof ServerPlayer serverPlayer && player.isShiftKeyDown()
                    && me.skywars2.game.LbcSummons.commandItem(player.getItemInHand(hand)))
                return game.items().wands().use(serverPlayer,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if (game != null && player instanceof ServerPlayer serverPlayer
                    && game.items().academy().interact(serverPlayer, hand, entity)) return InteractionResult.SUCCESS;
            if (game != null && game.inLobby() && player instanceof ServerPlayer serverPlayer
                    && entity.entityTags().contains("swrg.quickstart")) {
                game.requestStart(serverPlayer, true); return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if(game==null)return InteractionResult.PASS;
            if(game.items().actionsBlocked(player)||player instanceof ServerPlayer observer&&game.isObserver(observer))return InteractionResult.FAIL;
            if(player instanceof ServerPlayer serverPlayer&&game.items().attackIceCrystal(serverPlayer,entity))return InteractionResult.SUCCESS;
            return InteractionResult.PASS;
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> {
            var root = Commands.literal("sw").executes(ctx -> {
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
            root.then(Commands.literal("stop").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
                if (game != null) game.stopMatch(); return 1;
            }));
            root.then(Commands.literal("check").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.argument("suite", StringArgumentType.word()).executes(ctx -> {
                        if (game != null) GameChecks.run(game, StringArgumentType.getString(ctx, "suite")); return 1;
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
            dispatcher.register(Commands.literal("settings").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
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
                                    "lives", "health", "countdownSeconds", "stageOneSeconds", "stageTwoSeconds", "endingSeconds",
                                    "stageOneEvent", "stageTwoEvent", "pvpMode", "tntProtection", "luckyModifier", "cfg_lucky_modifier", "day", "ranked", "vanillaKits", "vanillaSkills", "lbcKits", "lbcSkills", "lbcLevelups", "lbcLevelupLimit", "lbcTrinketLimit", "lbcChallenges", "offhandWands", "lefthandWands", "noBorders", "gameMode", "map", "broken"), builder))
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
            var command = dispatcher.register(root);
            dispatcher.register(Commands.literal("skywars").executes(command.getCommand()).redirect(command));
        });
        LOGGER.info("SkyWars game cycle registered");
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> settingValueSuggestions(
            String parameter, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        if (game == null) return builder.buildFuture();
        String key = parameter.toLowerCase(java.util.Locale.ROOT);
        if (List.of("ranked", "vanillakits", "vanillaskills", "lbckits", "lbcskills", "lbclevelups", "lbcchallenges", "offhandwands", "lefthandwands", "noborders", "broken").contains(key))
            return SharedSuggestionProvider.suggest(List.of("true", "false"), builder);
        if (key.equals("map") || key.equals("mapid"))
            return SharedSuggestionProvider.suggest(game.maps().catalog().stream().filter(MapManager.MapInfo::available)
                    .map(MapManager.MapInfo::id), builder);
        List<String> values = switch (key) {
            case "lives" -> List.of("1", "2", "3", "4", "5");
            case "health" -> List.of("2", "10", "20", "40", "60");
            case "countdown", "countdownseconds" -> List.of("0", "3", "5", "10", "30", "60");
            case "stageoneseconds", "stage1seconds" -> List.of("0", "60", "120", "180", "300", "600", "1200", "3600");
            case "stagetwoseconds", "stage2seconds" -> List.of("0", "120", "240", "360", "600", "1200", "3600", "7200");
            case "ending", "endingseconds" -> List.of("0", "5", "10", "30", "60", "120");
            case "stageoneevent", "stage1event" -> List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
            case "stagetwoevent", "stage2event" -> List.of("0", "1", "2", "3", "4", "5", "6", "7", "8");
            case "pvp", "pvpmode" -> List.of("1", "2");
            case "tntprotection" -> List.of("0", "1", "2", "3", "4", "5");
            case "luckymodifier", "cfg_lucky_modifier" -> List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
            case "day" -> List.of("1", "2", "3");
            default -> List.of();
        };
        return SharedSuggestionProvider.suggest(values, builder);
    }
}
