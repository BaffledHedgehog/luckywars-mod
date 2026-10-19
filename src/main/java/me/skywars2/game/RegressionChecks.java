package me.skywars2.game;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.SguiUtils;
import eu.pb4.sgui.api.gui.SlotBasedGui;
import me.skywars2.SkyWars2;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Живые проверки диспетчеризации GUI, исходных названий, режимов и наблюдателей. */
public final class RegressionChecks {
    private RegressionChecks() {}

    public static void air(GameController game) {
        ServerPlayer player = bot(game, "TestAlpha");
        require(game.playing() && game.allowDamage(player), "Air skill requires a live participant");
        var profile = game.players().get(player.getUUID());
        int skill = profile.skill;
        boolean enabled = game.config().vanillaSkills;
        boolean ranked = game.config().ranked;
        boolean sprint = player.isSprinting(), ground = player.onGround();
        float yaw = player.getYRot();
        Vec3 motion = player.getDeltaMovement();
        try {
            profile.skill = 12;
            game.config().vanillaSkills = true;
            game.config().ranked = true;
            player.setSprinting(true);
            player.setOnGround(false);
            player.setYRot(0);
            player.setDeltaMovement(Vec3.ZERO);
            game.tick();
            require(player.getDeltaMovement().z > 0.09 && player.getDeltaMovement().y < -0.009,
                    "Wind-rider applies the forward/up-axis native motion impulse while sprinting in air");
            player.setDeltaMovement(Vec3.ZERO);
            player.setOnGround(true);
            game.tick();
            require(player.getDeltaMovement().equals(Vec3.ZERO), "Wind-rider does not accelerate a grounded player");
        } finally {
            profile.skill = skill;
            game.config().vanillaSkills = enabled;
            game.config().ranked = ranked;
            player.setSprinting(sprint);
            player.setOnGround(ground);
            player.setYRot(yaw);
            player.setDeltaMovement(motion);
        }
    }

    public static void victory(GameController game) {
        require(game.stage() == GameController.Stage.ENDING, "Victory generator requires the ending stage");
        int rockets = 0;
        for (var entity : game.maps().level().getAllEntities())
            if (entity instanceof net.minecraft.world.entity.projectile.FireworkRocketEntity rocket) {
                var design = rocket.getItem().get(net.minecraft.core.component.DataComponents.FIREWORKS);
                require(design != null && design.explosions().size() == 2,
                        "Victory rocket retains both source explosions in its actual item component");
                rockets++;
            }
        require(rockets >= 4, "Victory emits four real firework entities instead of particle placeholders");
    }

    /** Native attribute and saved-motion checks; every probe entity stays out of the live world. */
    public static void entityLimits(GameController game) {
        final double extreme = 2_000_000.0;
        int attributes = 0;
        for (var attribute : net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE) {
            var holder = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
            var instance = new net.minecraft.world.entity.ai.attributes.AttributeInstance(holder, ignored -> {});
            instance.setBaseValue(extreme);
            require(instance.getValue() == extreme,
                    "Attribute base exceeds the former upper bound: "
                            + net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getKey(attribute));
            instance.setBaseValue(-extreme);
            require(instance.getValue() == -extreme,
                    "Attribute base exceeds the former lower bound: "
                            + net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getKey(attribute));

            instance.setBaseValue(attribute.getDefaultValue());
            var modifier = new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("regression", "entity_limits"),
                    extreme, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
            instance.addTransientModifier(modifier);
            require(instance.getValue() == attribute.getDefaultValue() + extreme,
                    "Attribute modifier exceeds the former bound: "
                            + net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getKey(attribute));
            instance.removeModifier(modifier.id());
            instance.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    modifier.id(), -extreme,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
            require(instance.getValue() == attribute.getDefaultValue() - extreme,
                    "Negative attribute modifier exceeds the former bound: "
                            + net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getKey(attribute));
            attributes++;
        }
        require(attributes > 0, "Native attribute registry contains probeable attributes");

        var level = game.maps().level();
        var cow = new net.minecraft.world.entity.animal.cow.Cow(
                net.minecraft.world.entity.EntityTypes.COW, level);
        var movement = cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        require(movement != null, "Cow exposes a native movement-speed attribute");
        double oldMovement = movement.getBaseValue();
        var cowModifier = new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("regression", "cow_extreme_speed"),
                extreme, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
        try {
            movement.setBaseValue(extreme);
            require(cow.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) == extreme,
                    "A real mob accepts a movement-speed base beyond the former range");
            movement.setBaseValue(oldMovement);
            movement.addTransientModifier(cowModifier);
            require(cow.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                            == oldMovement + extreme,
                    "A real mob accepts a movement-speed modifier beyond the former range");
        } finally {
            movement.removeModifier(cowModifier.id());
            movement.setBaseValue(oldMovement);
            cow.discard();
        }

        checkSavedMotion(new net.minecraft.world.entity.animal.cow.Cow(
                        net.minecraft.world.entity.EntityTypes.COW, level),
                () -> new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW, level),
                "cow", extreme);
        checkSavedMotion(new net.minecraft.world.entity.projectile.arrow.Arrow(
                        net.minecraft.world.entity.EntityTypes.ARROW, level),
                () -> new net.minecraft.world.entity.projectile.arrow.Arrow(
                        net.minecraft.world.entity.EntityTypes.ARROW, level), "arrow", extreme);
        checkSavedMotion(new ItemEntity(level, 0, 0, 0, new ItemStack(Items.STONE)),
                () -> new ItemEntity(level, 0, 0, 0, new ItemStack(Items.STONE)), "item", extreme);
        checkMovementPackets(game);
        SkyWars2.LOGGER.info("[check] PASS regressions/entity-limits/attributes/saved-motion/player-vehicle-packets ({})",
                attributes);
    }

    private static void checkMovementPackets(GameController game) {
        ServerPlayer player = bot(game, "TestAlpha");
        require(player != null && game.inLobby(), "Movement packet fixture uses the lobby TestAlpha bot");
        ServerLevel level = (ServerLevel) player.level();
        Vec3 savedPosition = player.position();
        Vec3 savedVelocity = player.getDeltaMovement();
        float savedYaw = player.getYRot(), savedPitch = player.getXRot();
        net.minecraft.world.level.GameType savedMode = player.gameMode.getGameModeForPlayer();
        boolean savedNoGravity = player.isNoGravity();
        var oldConnection = player.connection;
        var recorder = new PacketRecordingConnection();
        var cookie = new net.minecraft.server.network.CommonListenerCookie(
                player.getGameProfile(), 0, player.clientInformation(), false);
        var listener = new net.minecraft.server.network.ServerGamePacketListenerImpl(
                game.server(), recorder, player, cookie);
        net.minecraft.world.entity.vehicle.boat.Boat boat = null;
        BlockPos wall = null;
        net.minecraft.world.level.block.state.BlockState oldWall = null;
        boolean changedWall = false;
        try {
            player.connection = listener;
            player.stopRiding();
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.setNoGravity(true);
            player.setDeltaMovement(Vec3.ZERO);
            listener.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());

            double startX = Math.floor(savedPosition.x) + 0.5;
            double startZ = Math.floor(savedPosition.z) + 0.5;
            ensureChunk(level, startX + 40, startZ);
            listener.teleport(startX, 280, startZ, 0, 0);
            acknowledgeLatestTeleport(listener, recorder, startX, 280, startZ, 0, 0);
            listener.resetPosition();
            listener.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                    startX + 40, 280, startZ, false, false));
            require(Math.abs(player.getX() - (startX + 40)) < 0.01 && Math.abs(player.getY() - 280) < 0.01,
                    "Native Creative free-air player movement packet accepts a 40-block displacement without rollback");

            double boatX = startX + 100;
            double boatZ = startZ + 8;
            ensureChunk(level, boatX, boatZ);
            ensureChunk(level, boatX + 40, boatZ);
            ensureChunk(level, boatX + 80, boatZ);
            listener.teleport(boatX, 280, boatZ, 0, 0);
            acknowledgeLatestTeleport(listener, recorder, boatX, 280, boatZ, 0, 0);
            player.setDeltaMovement(Vec3.ZERO);
            boat = new net.minecraft.world.entity.vehicle.boat.Boat(
                    net.minecraft.world.entity.EntityTypes.OAK_BOAT, level, () -> Items.OAK_BOAT);
            boat.setPos(boatX, 280, boatZ);
            boat.setNoGravity(true);
            require(level.addFreshEntity(boat) && player.startRiding(boat, true, true),
                    "Native oak boat fixture spawns and accepts TestAlpha as its passenger");
            listener.tick(); // Native tick snapshots lastVehicle and the vehicle's good position.
            require(player.getVehicle() == boat && boat.getControllingPassenger() == player,
                    "Native player tick retains TestAlpha as the oak boat's controlling passenger");
            // startRiding itself sends a native position correction; accept its latest exact packet ID.
            acknowledgeLatestTeleport(listener, recorder, player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot());
            require(listener.hasClientLoaded() && player.getVehicle() == boat
                            && boat.getControllingPassenger() == player,
                    "Vehicle handler preconditions: loaded=" + listener.hasClientLoaded()
                            + ", passenger=" + (player.getVehicle() == boat)
                            + ", controller=" + (boat.getControllingPassenger() == player)
                            + ", position=" + player.position());
            double freeVehicleX = boatX + 40;
            listener.handleMoveVehicle(new net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket(
                    net.minecraft.core.PositionAndRotation.of(new Vec3(freeVehicleX, 280, boatZ), 0, 0), false));
            require(Math.abs(boat.getX() - freeVehicleX) < 0.01,
                    "Native vehicle movement packet accepts a 40-block displacement without speed rollback");

            double blockedVehicleX = boatX + 80;
            wall = BlockPos.containing(blockedVehicleX, 280, boatZ);
            level.getChunkAt(wall);
            oldWall = level.getBlockState(wall);
            require(oldWall.isAir(), "Vehicle collision probe position is open air before the temporary obstacle");
            level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            changedWall = true;
            listener.handleMoveVehicle(new net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket(
                    net.minecraft.core.PositionAndRotation.of(new Vec3(blockedVehicleX, 280, boatZ), 0, 0), false));
            require(Math.abs(boat.getX() - freeVehicleX) < 0.01,
                    "Native vehicle collision still rejects movement into a solid block after speed validation is disabled");
        } finally {
            if (changedWall) level.setBlockAndUpdate(wall, oldWall);
            player.stopRiding();
            if (boat != null) boat.discard();
            // Restore through the probe listener, then restore the original bot connection and its native movement baseline.
            player.absSnapTo(savedPosition.x, savedPosition.y, savedPosition.z, savedYaw, savedPitch);
            player.connection = oldConnection;
            oldConnection.resetPosition();
            player.setGameMode(savedMode);
            player.setNoGravity(savedNoGravity);
            player.setDeltaMovement(savedVelocity);
        }
    }

    private static void ensureChunk(ServerLevel level, double x, double z) {
        level.getChunkAt(BlockPos.containing(x, 280, z));
    }

    private static void acknowledgeLatestTeleport(
            net.minecraft.server.network.ServerGamePacketListenerImpl listener,
            PacketRecordingConnection recorder, double x, double y, double z, float yaw, float pitch) {
        net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket sent = null;
        for (net.minecraft.network.protocol.Packet<?> packet : recorder.sent) {
            if (packet instanceof net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket position) {
                sent = position;
            }
        }
        require(sent != null, "Native teleport emits a capturable position packet with its acknowledgement id");
        listener.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(
                sent.id(), x, y, z, yaw, pitch));
    }

    private static final class PacketRecordingConnection extends net.minecraft.network.Connection {
        private final java.util.List<net.minecraft.network.protocol.Packet<?>> sent = new java.util.ArrayList<>();

        private PacketRecordingConnection() {
            super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        }

        @Override public boolean isMemoryConnection() { return true; }
        @Override public boolean isConnected() { return true; }
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet,
                                   io.netty.channel.ChannelFutureListener listener, boolean flush) {
            sent.add(packet);
        }
    }

    private static void checkSavedMotion(net.minecraft.world.entity.Entity entity,
                                         java.util.function.Supplier<net.minecraft.world.entity.Entity> freshEntity,
                                         String label, double extreme) {
        net.minecraft.world.entity.Entity restored = null;
        try {
            entity.setDeltaMovement(extreme, -extreme, extreme + 1.0);
            var saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                    net.minecraft.util.ProblemReporter.DISCARDING, entity.registryAccess());
            entity.saveWithoutId(saved);
            restored = freshEntity.get();
            restored.load(net.minecraft.world.level.storage.TagValueInput.create(
                    net.minecraft.util.ProblemReporter.DISCARDING, entity.registryAccess(), saved.buildResult()));
            Vec3 motion = restored.getDeltaMovement();
            require(motion.x == extreme && motion.y == -extreme && motion.z == extreme + 1.0,
                    "Saved " + label + " motion survives native load beyond the former velocity bound");
        } finally {
            entity.discard();
            if (restored != null) restored.discard();
        }
    }

    public static void run(GameController game) {
        require(game.inLobby(), "Regression suite requires the ready lobby");
        require(game.selectedMapReady(), "Regression suite requires the selected map to be ready");
        var advancements = game.server().getAdvancements().getAllAdvancements();
        require(advancements.size() == 357 && advancements.stream().allMatch(a ->
                a.id().getNamespace().equals("lbc") && a.id().getPath().startsWith("true_advancements/")),
                "Only the 357 native LBC tree advancements are registered; vanilla listeners are absent");
        ServerPlayer alpha = bot(game, "TestAlpha");
        ServerPlayer beta = bot(game, "TestBeta");
        require(alpha != null && beta != null, "Run the existing /sw check bots fixture first");
        require(!game.isObserver(alpha) && !game.isObserver(beta), "Regression bots must begin as lobby players");

        int oldDay = game.config().day;
        int oldMode = game.config().gameMode;
        int oldPvp = game.config().pvpMode;
        PlayerStore.Profile betaProfile = game.players().get(beta.getUUID());
        require(betaProfile != null, "TestBeta has a loaded profile");
        ProfileSnapshot profile = ProfileSnapshot.capture(betaProfile);
        boolean observerEntered = false;
        try {
            checkDoubleClickDispatch(game, alpha);
            checkMissingSourceNames(game, alpha);
            checkGameModeVisibility(game, alpha);
            checkUnsupportedModeStart(game, alpha);
            checkObserverMenusAndTargeting(game, beta, alpha);
            SkyWars2.LOGGER.info("[check] PASS regressions/gui-doubleclick/source-names/game-mode/observer");
        } finally {
            game.config().day = oldDay;
            game.config().pvpMode = oldPvp;
            game.config().gameMode = oldMode;
            game.config().save();
            if (game.spectators().contains(beta)) {
                game.spectators().remove(beta);
                observerEntered = true;
            }
            beta.closeContainer();
            alpha.closeContainer();
            if (observerEntered) game.join(beta);
            profile.restore(game.players().get(beta.getUUID()));
            game.players().save();
        }
    }

    private static void checkUnsupportedModeStart(GameController game, ServerPlayer player) {
        int oldMode = game.config().gameMode;
        String oldMap = game.config().mapId;
        boolean oldBroken = game.config().broken;
        try {
            game.config().gameMode = 2;
            game.config().save();
            require(!game.canStart(player), "Unsupported mode 2 cannot start a match");
            GameController.Stage before = game.stage();
            game.requestStart(player, false);
            require(game.stage() == before && game.inLobby(), "Rejected mode 2 start leaves the game in its lobby stage");
            require(game.config().gameMode == 2 && game.config().mapId.equals(oldMap)
                            && game.config().broken == oldBroken,
                    "Rejected mode 2 start does not mutate mode or map selection");
        } finally {
            game.config().gameMode = oldMode;
            game.config().save();
        }
    }

    /** Actual GUI selection followed by one start click while the selected map is still preparing. */
    public static void startMenuSelection(GameController game) {
        require(game.inLobby() && game.selectedMapReady() && game.config().gameMode == 1,
                "Menu start check requires a ready normal-mode lobby");
        ServerPlayer player = bot(game, "TestAlpha");
        require(player != null && bot(game, "TestBeta") != null, "Run the check bots fixture first");
        String current = game.config().mapId;
        String target = current.equals("1") ? "2" : "1";
        MapManager.MapInfo info = game.maps().info(target);
        require(info != null && info.available() && info.playerCapacity() >= game.server().getPlayerList().getPlayers().size(),
                "Alternate GUI map supports all online regression bots");

        game.menus().open(player, GameMenus.Page.MAPS);
        OriginalGuiDesign.SourceMapButton source = OriginalGuiDesign.sourceMapButton(player, target, false);
        require(source != null, "Target map appears on the initial map-selection page");
        inventoryClick(player, source.slot());
        require(game.maps().busy() && !game.selectedMapReady() && game.config().mapId.equals(target),
                "Native map GUI selection begins asynchronous preparation");
        inventoryClick(player, source.slot());
        require(game.config().mapId.equals(target), "Repeating the selected map keeps the same pending selection");

        game.menus().open(player, GameMenus.Page.HUB);
        require(game.maps().busy() && currentGui(player).getGuiElement(1) != null,
                "Lobby menu remains usable while the selected map prepares");
        inventoryClick(player, 1);
        require(game.inLobby() && game.maps().busy() && !game.selectedMapReady(),
                "One native Start click queues the match while preparation is still in progress");
        SkyWars2.LOGGER.info("[check] ARMED map-start-menu: queued one-click start for map {}", target);
    }

    /** Invoke the registered native entity-use callback once while a selected map is preparing. */
    public static void quickstartDuringPreparation(GameController game) {
        require(game.inLobby() && game.selectedMapReady() && game.config().gameMode == 1,
                "Quickstart check requires a ready normal-mode lobby");
        ServerPlayer player = bot(game, "TestAlpha");
        require(player != null && bot(game, "TestBeta") != null, "Run the check bots fixture first");
        String target = game.config().mapId.equals("1") ? "2" : "1";
        require(game.selectMap(player, target, game.config().broken), "Begin preparing an available map");
        require(game.maps().busy() && !game.selectedMapReady(), "Map preparation is in progress before quickstart use");

        net.minecraft.world.entity.Entity quickstart = null;
        for (var entity : game.maps().level().getAllEntities()) {
            if (entity.getType() == net.minecraft.world.entity.EntityTypes.INTERACTION
                    && entity.entityTags().contains("swrg.quickstart")) { quickstart = entity; break; }
        }
        require(quickstart != null, "A tagged quickstart entity exists in the loaded lobby");
        InteractionResult result = UseEntityCallback.EVENT.invoker().interact(
                player, player.level(), InteractionHand.MAIN_HAND, quickstart, null);
        require(result == InteractionResult.SUCCESS, "Native right-click callback accepts quickstart during preparation");
        require(game.inLobby() && game.maps().busy() && !game.selectedMapReady(),
                "Quickstart remains pending in lobby until the map preparation callback completes");
        SkyWars2.LOGGER.info("[check] ARMED map-start-quickstart: accepted native quickstart during preparation");
    }

    /** Match-only observer and arena checks; invoke while PLAYING after the cage has been removed. */
    public static void match(GameController game) {
        require(game.playing(), "Observer match checks require PLAYING");
        require(game.config().gameMode == 1, "Observer match checks require normal SkyWars mechanics");
        ServerPlayer alpha = bot(game, "TestAlpha");
        ServerPlayer beta = bot(game, "TestBeta");
        require(alpha != null && beta != null, "Run the existing /sw check bots fixture first");
        require(game.allowDamage(alpha) && game.allowDamage(beta), "Match probes must be alive participants");
        require(!alpha.getInventory().getItem(GameController.MENU_COMPASS_SLOT).is(Items.COMPASS)
                        && !beta.getInventory().getItem(GameController.MENU_COMPASS_SLOT).is(Items.COMPASS),
                "Playing participants do not retain the lobby menu compass");
        for (MapManager.Spawn spawn : game.maps().spawns()) {
            var below = BlockPos.containing(spawn.position()).below();
            var state = game.maps().level().getBlockState(below);
            require(!state.is(net.minecraft.world.level.block.Blocks.GLASS)
                            && !state.is(net.minecraft.world.level.block.Blocks.BARRIER),
                    "Match spawn is not left on cage glass/barrier at " + below);
        }
        for (String id : LbcLoadouts.unlockIds()) {
            require(id != null && id.startsWith("lbc:true_advancements/")
                            && id.length() > "lbc:true_advancements/".length(),
                    "LBC unlock whitelist contains a full source advancement ID");
        }

        ItemStack savedTargetStack = alpha.getInventory().getItem(34).copy();
        MobEffectInstance currentGlow = alpha.getEffect(MobEffects.GLOWING);
        MobEffectInstance savedGlow = currentGlow == null ? null : new MobEffectInstance(currentGlow);
        ServerLevel savedLevel = (ServerLevel) beta.level();
        Vec3 savedPosition = beta.position();
        float savedYaw = beta.getYRot();
        float savedPitch = beta.getXRot();
        ProfileSnapshot savedObserverProfile = ProfileSnapshot.capture(game.players().get(beta.getUUID()));
        boolean hadSkill2Tag = beta.entityTags().contains("swrg.skill2");
        boolean registered = false;
        try {
            beta.closeContainer();
            game.spectators().enter(beta);
            registered = true;
            require(game.isObserver(beta) && beta.isSpectator()
                            && beta.gameMode.getGameModeForPlayer() == GameType.ADVENTURE,
                    "Registered observer uses the native spectator predicate while remaining in Adventure");
            require(!TargetingConditions.forCombat().test((ServerLevel) alpha.level(), alpha, beta),
                    "Native combat targeting excludes the observer from the live match");

            if (!hadSkill2Tag) beta.entityTags().add("swrg.skill2");
            game.players().get(beta.getUUID()).skill = 0;
            game.players().get(beta.getUUID()).lbcKit = 0;
            game.menus().open(beta, GameMenus.Page.VANILLA_SKILLS);
            inventoryClick(beta, 1);
            require(game.players().get(beta.getUUID()).skill == 2,
                    "An eliminated observer can change a vanilla build during the match");
            game.menus().open(beta, GameMenus.Page.LBC_KITS);
            inventoryClick(beta, 23);
            require(game.players().get(beta.getUUID()).lbcKit == 23 && !game.allowDamage(beta),
                    "Observer LBC selection is allowed without re-enrolling the observer in the match");

            game.spectators().openTargets(beta);
            // Other checks may leave additional live bots; find the intended player by its real profile.
            int targetSlot = -1;
            SlotBasedGui targets = currentGui(beta);
            for (int slot = 0; slot < 27; slot++) {
                var element = targets.getGuiElement(slot);
                if (element == null) continue;
                var profile = element.getItemStack().get(net.minecraft.core.component.DataComponents.PROFILE);
                require(profile != null, "Every target card contains a native player profile");
                UUID id = profile.partialProfile().id();
                ServerPlayer candidate = game.server().getPlayerList().getPlayer(id);
                require(candidate != null && game.allowDamage(candidate) && !id.equals(beta.getUUID()),
                        "Target menu excludes observers and inactive participants");
                if (id.equals(alpha.getUUID())) targetSlot = slot;
            }
            require(targetSlot >= 0, "Living-player menu contains the intended active player");
            inventoryClick(beta, targetSlot);
            require(beta.distanceToSqr(alpha) < 0.01 && !game.spectators().attached(beta),
                    "Native head-menu click teleports outside the target camera");

            ItemStack targetStack = new ItemStack(Items.DIAMOND, 7);
            alpha.getInventory().setItem(34, targetStack);
            alpha.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0));

            beta.teleportTo((ServerLevel) alpha.level(), alpha.getX() + 2.0, alpha.getY(), alpha.getZ(),
                    Set.of(), beta.getYRot(), beta.getXRot(), true);
            beta.connection.handleInteract(new ServerboundInteractPacket(
                    alpha.getId(), InteractionHand.MAIN_HAND, Vec3.ZERO, false));
            require(game.spectators().attached(beta),
                    "Native interact packet attaches the registered observer to a live target");
            require(beta.getCamera() == alpha && beta.gameMode.getGameModeForPlayer() == GameType.SPECTATOR,
                    "Watching uses the target camera and native spectator game mode");
            int observerManaBeforeHud = game.items().mana().get(beta);
            game.items().refreshHud(beta);
            require(alpha.getUUID().equals(game.items().hudSource(beta))
                            && game.items().mana().get(beta) == observerManaBeforeHud,
                    "Camera HUD reads the target's real statuses without changing observer mana");
            require(beta.getInventory().getItem(34).is(Items.DIAMOND)
                            && beta.getInventory().getItem(34).getCount() == 7,
                    "Watch tick mirrors target inventory into a copy");
            int mirroredWithGlow = game.spectators().mirroredEffects(beta);
            require(mirroredWithGlow >= 1, "Watch view tracks target effects for client-only packets");
            require(!beta.hasEffect(MobEffects.GLOWING) && alpha.hasEffect(MobEffects.GLOWING),
                    "Target effects remain client-view state and do not mutate server-side observer effects");

            game.spectators().openInventory(beta);
            SlotBasedGui inventory = currentGui(beta);
            require(inventory.getGuiElement(34) != null
                            && inventory.getGuiElement(34).getItemStack().getCount() == 7,
                    "Read-only inventory GUI shows the target's current stack");
            inventory.getGuiElement(34).getItemStack().setCount(2);
            require(alpha.getInventory().getItem(34).getCount() == 7,
                    "Changing a displayed inventory copy cannot mutate the target inventory");

            alpha.getInventory().setItem(34, new ItemStack(Items.EMERALD, 5));
            alpha.removeEffect(MobEffects.GLOWING);
            game.spectators().tick();
            require(inventory.getGuiElement(34).getItemStack().is(Items.EMERALD)
                            && inventory.getGuiElement(34).getItemStack().getCount() == 5,
                    "Read-only target inventory follows later changes");
            require(!beta.hasEffect(MobEffects.GLOWING) && !alpha.hasEffect(MobEffects.GLOWING),
                    "Removed target effects leave no server-side effect on the observer");
            require(game.spectators().mirroredEffects(beta) == mirroredWithGlow - 1,
                    "Removed target effect is cleared from the observer's mirrored-effect cache");

            InteractionResult attack = AttackEntityCallback.EVENT.invoker().interact(
                    beta, beta.level(), InteractionHand.MAIN_HAND, alpha, null);
            require(attack == InteractionResult.FAIL, "Observer entity attacks are canceled");
            InteractionResult blockAttack = AttackBlockCallback.EVENT.invoker().interact(
                    beta, beta.level(), InteractionHand.MAIN_HAND, BlockPos.ZERO, Direction.UP);
            require(blockAttack == InteractionResult.FAIL, "Observer block attacks are canceled");
            BlockHitResult hit = new BlockHitResult(Vec3.ZERO, Direction.UP, BlockPos.ZERO, false);
            InteractionResult blockUse = UseBlockCallback.EVENT.invoker().interact(
                    beta, beta.level(), InteractionHand.MAIN_HAND, hit);
            require(blockUse == InteractionResult.FAIL, "Observer block use is canceled");

            ItemEntity dropped = new ItemEntity(beta.level(), beta.getX(), beta.getY(), beta.getZ(),
                    new ItemStack(Items.AMETHYST_SHARD));
            try {
                dropped.setNoPickUpDelay();
                dropped.playerTouch(beta);
                require(!dropped.isRemoved() && dropped.getItem().getCount() == 1,
                        "Observer cannot pick up dropped items");
            } finally { dropped.discard(); }
            game.spectators().detach(beta);
            game.items().refreshHud(beta);
            require(game.items().hudSource(beta) == null,
                    "Detached observer no longer receives the target actionbar");
            SkyWars2.LOGGER.info("[check] PASS regressions/observer-match/spawns/inventory/effects/actions/advancements");
        } finally {
            alpha.getInventory().setItem(34, savedTargetStack);
            alpha.removeEffect(MobEffects.GLOWING);
            if (savedGlow != null) alpha.addEffect(savedGlow);
            beta.closeContainer();
            if (registered && game.spectators().contains(beta)) {
                game.spectators().remove(beta);
                beta.setGameMode(GameType.SURVIVAL);
                beta.teleportTo(savedLevel, savedPosition.x, savedPosition.y, savedPosition.z,
                        Set.of(), savedYaw, savedPitch, true);
            }
            savedObserverProfile.restore(game.players().get(beta.getUUID()));
            if (!hadSkill2Tag) beta.entityTags().remove("swrg.skill2");
            game.players().save();
        }
    }

    private static void checkDoubleClickDispatch(GameController game, ServerPlayer player) {
        game.config().day = 1;
        game.config().save();
        game.menus().open(player, GameMenus.Page.SETTINGS);
        SlotBasedGui gui = currentGui(player);
        require(gui.getGuiElement(1) != null, "Day setting has a live SGui element");

        // Call the real SGui dispatch entry point: each PICKUP must run the button callback,
        // while the client's follow-up PICKUP_ALL collection packet must not run it again.
        gui.click(1, ClickType.MOUSE_LEFT, ContainerInput.PICKUP);
        require(game.config().day == 2, "First native PICKUP advances the day once");
        gui.click(1, ClickType.MOUSE_LEFT, ContainerInput.PICKUP);
        require(game.config().day == 3, "Second native PICKUP advances the day a second time");
        gui.click(1, ClickType.MOUSE_DOUBLE_CLICK, ContainerInput.PICKUP_ALL);
        require(game.config().day == 3, "Synthetic PICKUP_ALL does not execute a third action");
    }

    private static void checkMissingSourceNames(GameController game, ServerPlayer player) {
        game.menus().open(player, GameMenus.Page.SETTINGS);
        SlotBasedGui gui = currentGui(player);
        for (int slot = 8; slot <= 11; slot++) {
            var element = gui.getGuiElement(slot);
            require(element != null && !element.getItemStack().isEmpty(), "Settings slot " + slot + " has a rendered source item");
            String key = switch (slot) {
                case 8 -> "swrg.lefthand_on";
                case 9 -> switch (game.config().lives) {
                    case 2 -> "swrg.lives_2";
                    case 3 -> "swrg.lives_3";
                    case 4 -> "swrg.lives_4";
                    case 5 -> "swrg.lives_5";
                    default -> "swrg.lives_1";
                };
                case 10 -> game.config().noBorders ? "swrg.no_borders_on" : "swrg.no_borders_off";
                default -> "swrg.cfg_lucky_modifier_0";
            };
            Component name = element.getItemStack().getHoverName();
            require(name.getContents() instanceof TranslatableContents translated && translated.getKey().equals(key),
                    "Settings slot " + slot + " retains source translation " + key);
        }
    }

    private static void checkGameModeVisibility(GameController game, ServerPlayer player) {
        game.config().gameMode = 1;
        game.config().save();
        game.menus().open(player, GameMenus.Page.HUB);
        require(currentGui(player).getGuiElement(9) == null, "BedWars entry is absent in normal SkyWars mode");

        game.config().gameMode = 2;
        game.config().save();
        game.menus().open(player, GameMenus.Page.SETTINGS);
        SlotBasedGui settings = currentGui(player);
        settings.click(2, ClickType.MOUSE_LEFT, ContainerInput.PICKUP);
        require(game.config().gameMode == 3, "Settings slot 2 selects BedWars mode 3");
        game.menus().open(player, GameMenus.Page.HUB);
        require(currentGui(player).getGuiElement(9) != null, "BedWars entry appears in mode 3");

        game.config().gameMode = 4;
        game.config().save();
        game.menus().open(player, GameMenus.Page.HUB);
        require(currentGui(player).getGuiElement(9) != null, "BedWars entry appears in mode 4");
    }

    private static void checkObserverMenusAndTargeting(GameController game, ServerPlayer observer, ServerPlayer target) {
        require(observer.gameMode.getGameModeForPlayer() == GameType.ADVENTURE,
                "Probe begins in Adventure before observer registration");
        game.spectators().enter(observer);
        game.menus().open(observer, GameMenus.Page.HUB);
        require(game.isObserver(observer) && !game.spectators().attached(observer),
                "Opening HUB detaches the observer from the watched entity without removing observer status");
        require(observer.gameMode.getGameModeForPlayer() == GameType.ADVENTURE,
                "Observer remains in Adventure while the native spectator predicate is active");
        require(observer.isSpectator(), "Registered observer satisfies native isSpectator predicate");

        observer.closeContainer();
        observer.getInventory().setSelectedSlot(GameController.MENU_COMPASS_SLOT);
        observer.connection.handleUseItem(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(
                InteractionHand.MAIN_HAND, 0, observer.getYRot(), observer.getXRot()));
        require(currentGui(observer).getGuiElement(6) != null,
                "Native compass use opens the observer's personal HUB in Adventure");
        observer.closeContainer();
        observer.getInventory().setSelectedSlot(6);
        observer.connection.handleUseItem(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(
                InteractionHand.MAIN_HAND, 1, observer.getYRot(), observer.getXRot()));
        require(currentGui(observer) != null, "Native head-item use opens the living-player menu");
        game.menus().open(observer, GameMenus.Page.HUB);

        SlotBasedGui hub = currentGui(observer);
        require(hub.getGuiElement(0) == null && hub.getGuiElement(1) == null && hub.getGuiElement(2) == null,
                "Observer HUB hides settings, start, and map selection");
        game.menus().open(observer, GameMenus.Page.SETTINGS);
        require(currentGui(observer) == hub, "Observer cannot directly open settings");
        inventoryClick(observer, 14);
        SlotBasedGui skills = currentGui(observer);
        require(skills.getGuiElement(0) != null && !skills.getGuiElement(0).getItemStack().is(Items.COMPARATOR),
                "Observer can open a personal build page");
        require(game.isObserver(observer) && observer.isSpectator(),
                "Observer registration and native spectator predicate survive personal-menu navigation");

        require(!observer.canBeSeenAsEnemy(), "Observer cannot be seen as a native combat enemy");
        require(!TargetingConditions.forCombat().test((ServerLevel) target.level(), target, observer),
                "Native combat targeting excludes registered observers as targets");
    }

    private static ServerPlayer bot(GameController game, String name) {
        UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        return game.server().getPlayerList().getPlayer(id);
    }

    private static SlotBasedGui currentGui(ServerPlayer player) {
        var gui = SguiUtils.getCurrentGui(player);
        require(gui instanceof SlotBasedGui, "Expected an open SGui slot menu for " + player.getScoreboardName());
        return (SlotBasedGui) gui;
    }

    private static void inventoryClick(ServerPlayer player, int slot) {
        var menu = player.containerMenu;
        player.connection.handleContainerClick(new net.minecraft.network.protocol.game.ServerboundContainerClickPacket(
                menu.containerId, menu.getStateId(), (short) slot, (byte) 0, ContainerInput.PICKUP,
                new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>(), net.minecraft.network.HashedStack.EMPTY));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Regression check failed: " + message);
    }

    private record ProfileSnapshot(String username, int coins, int memcoins, int games, int wins, int kills,
                                   int deaths, int[] keys, int kit, int skill, int killSound, int suffix,
                                   int winEffect, int cage, int lbcKit, int lbcSkill, int lbcTrinket,
                                   int lbcChallenge, int[] levelups, int lbcLevelupSlot, Set<String> lbcUnlocks) {
        static ProfileSnapshot capture(PlayerStore.Profile p) {
            return new ProfileSnapshot(p.username, p.coins, p.memcoins, p.games, p.wins, p.kills, p.deaths,
                    p.keys.clone(), p.kit, p.skill, p.killSound, p.suffix, p.winEffect, p.cage,
                    p.lbcKit, p.lbcSkill, p.lbcTrinket, p.lbcChallenge, p.levelups.clone(),
                    p.lbcLevelupSlot, new HashSet<>(p.lbcUnlocks));
        }

        void restore(PlayerStore.Profile p) {
            p.username = username;
            p.coins = coins; p.memcoins = memcoins; p.games = games; p.wins = wins;
            p.kills = kills; p.deaths = deaths;
            System.arraycopy(keys, 0, p.keys, 0, Math.min(keys.length, p.keys.length));
            p.kit = kit; p.skill = skill; p.killSound = killSound; p.suffix = suffix;
            p.winEffect = winEffect; p.cage = cage; p.lbcKit = lbcKit; p.lbcSkill = lbcSkill;
            p.lbcTrinket = lbcTrinket; p.lbcChallenge = lbcChallenge;
            System.arraycopy(levelups, 0, p.levelups, 0, Math.min(levelups.length, p.levelups.length));
            p.lbcLevelupSlot = lbcLevelupSlot;
            p.lbcUnlocks.clear(); p.lbcUnlocks.addAll(lbcUnlocks);
        }
    }
}
