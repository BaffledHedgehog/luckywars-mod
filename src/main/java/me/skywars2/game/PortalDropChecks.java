package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native endpoint, portal-processor and bounded asynchronous admission fixtures. */
public final class PortalDropChecks {
    private PortalDropChecks() { }
    private static State running;
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static int stopTick = Integer.MIN_VALUE;
    private static void require(boolean ok, String message) {
        if (!ok) throw new IllegalStateException("Portal drops: " + message);
    }
    private static ItemEntity token(ServerLevel level, Vec3 at, int count,
                                    ServerPlayer author, LbcPortalDropItem.Kind kind) {
        var item = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(kind.item(), count));
        item.setThrower(author); item.setNoGravity(true); item.setDeltaMovement(Vec3.ZERO);
        item.setPickUpDelay(1000); require(level.addFreshEntity(item), "native token admission");
        return item;
    }
    private static final class State implements AutoCloseable {
        final GameController game;
        final ServerLevel level;
        final ServerPlayer alpha, beta;
        final GameChecks.WandThirdPlayerState alphaState, betaState;
        final boolean noBorders;
        final Map<BlockPos, MapManager.BlockSnapshot> terrain = new LinkedHashMap<>();
        final Set<Long> forced = new HashSet<>();
        final List<Entity> actors = new ArrayList<>();
        final List<ItemEntity> tokens = new ArrayList<>();
        final Map<BlockPos,ItemEntity> activeTokens = new LinkedHashMap<>();
        final Map<BlockPos,Vec3> sourcePositions = new LinkedHashMap<>();
        final List<LbcPortalDrops.Plan> observed = new ArrayList<>();
        final Vec3 origin = new Vec3(-200.63, 270.7, -200.19);
        ItemEntity paused, unloaded;
        int phase, wait, lastTick, initialObserved;
        boolean checkingSpread;
        State(GameController game) {
            this.game = game; level = game.maps().level(); noBorders = game.config().noBorders;
            alpha = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            beta = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && alpha != null && beta != null && game.allowDamage(beta),
                    "two active actual match participants");
            alphaState = new GameChecks.WandThirdPlayerState(alpha, game.items().mana());
            betaState = new GameChecks.WandThirdPlayerState(beta, game.items().mana());
        }
        void resident(BlockPos at) {
            int x = at.getX() >> 4, z = at.getZ() >> 4;
            long key = net.minecraft.world.level.ChunkPos.pack(x, z);
            if (!level.getForceLoadedChunks().contains(key)) {
                forced.add(key); level.setChunkForced(x, z, true);
            }
            level.getChunkAt(at);
        }
        void remember(BlockPos at) {
            if (!level.isOutsideBuildHeight(at))
                terrain.computeIfAbsent(at.immutable(), p -> game.maps().snapshot(level, p));
        }
        void remember(LbcPortalDrops.Plan plan) {
            for (var base : List.of(plan.sourceBase(), plan.remoteBase()))
                for (int dy : new int[]{-1, 3, 4}) remember(base.above(dy));
        }
        void beforePlace(LbcPortalDrops.Plan plan) {
            if (checkingSpread) {
                var support = level.getBlockState(plan.remoteBase().below());
                require(!support.liquid() && support.is(net.minecraft.tags.BlockTags.ENTITIES_CAN_TELEPORT_TO)
                        && level.getBlockState(plan.remoteBase()).isAir()
                        && level.getBlockState(plan.remoteBase().above()).isAir(),
                        "actual random placement selects source-safe support with two air blocks");
            }
            remember(plan); observed.add(plan);
        }
        void initialize() {
            require(game.items().portalDrops().idle(), "no earlier portal job");
            game.items().portalDrops().beforePlace = this::beforePlace;
            for (var player : List.of(alpha, beta)) {
                player.closeContainer(); player.getInventory().clearContent(); player.removeAllEffects();
                player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true);
            }
            resident(BlockPos.containing(origin));
            alpha.teleportTo(level, origin.x + 30, origin.y, origin.z, Set.of(), 0, 0, true);
            beta.teleportTo(level, origin.x + 35, origin.y, origin.z, Set.of(), 0, 0, true);
            presentation(); geometry(); spawnHeights(); guards();
            game.command("give TestAlpha skywars2:random_portal_drop 1");
            lastTick = game.server().getTickCount();
        }
        void presentation() {
            for (var kind : LbcPortalDropItem.Kind.values()) {
                var stack = new ItemStack(kind.item());
                var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                require(data.size() == 2 && data.getIntOr("portall", 0) == 1 && data.getIntOr("drop", 0) == 1
                        && LbcPortalDropItem.matches(stack, kind), "exact source trigger " + kind);
                if (kind == LbcPortalDropItem.Kind.NORMAL) {
                    require(Identifier.parse("lbc:drop_not_trap").equals(stack.get(DataComponents.ITEM_MODEL))
                            && Identifier.parse("lbc:tier_d").equals(stack.get(DataComponents.TOOLTIP_STYLE)), "source normal model/tier");
                    var name = stack.getHoverName();
                    require(name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                            && text.getKey().equals("drop_portals") && !name.getStyle().isItalic()
                            && name.getStyle().getColor().getValue() == 0xffff55, "source yellow nonitalic translated name");
                    var lore = stack.getOrDefault(DataComponents.LORE, net.minecraft.world.item.component.ItemLore.EMPTY).lines();
                    require(lore.size() == 1 && lore.getFirst().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                            && text.getKey().equals("trap_desc") && !lore.getFirst().getStyle().isItalic()
                            && lore.getFirst().getStyle().getColor().getValue() == 0xffffff, "source white nonitalic lore");
                } else require(Identifier.parse("minecraft:barrier").equals(stack.get(DataComponents.ITEM_MODEL))
                        && !stack.has(DataComponents.CUSTOM_NAME) && !stack.has(DataComponents.TOOLTIP_STYLE)
                        && stack.getOrDefault(DataComponents.LORE, net.minecraft.world.item.component.ItemLore.EMPTY).lines().isEmpty(), "plain old barrier");
                var loot = game.lbc().rollTable(level, origin, alpha,
                        kind == LbcPortalDropItem.Kind.NORMAL ? "lbc:ported/random_portal_drop" : "lbc:ported/old_random_portal_drop");
                require(loot.size() == 1 && loot.getFirst().getCount() == 1
                        && ItemStack.isSameItemSameComponents(stack, loot.getFirst()), "complete public leaf/prototype equality " + kind);
                var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                    @Override public net.minecraft.network.PacketListener getPacketListener() { return alpha.connection; }
                };
                var provider = (net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider) connection;
                net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider, () -> {
                    var proxy = eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(stack,
                            net.minecraft.world.item.TooltipFlag.NORMAL, provider.getPacketContext(), alpha.registryAccess());
                    require(proxy.is(kind == LbcPortalDropItem.Kind.NORMAL ? Items.REPEATING_COMMAND_BLOCK : Items.BARRIER)
                            && stack.get(DataComponents.ITEM_MODEL).equals(proxy.get(DataComponents.ITEM_MODEL)), "actual Polymer source carrier/model");
                    require(ItemStack.isSameItemSameComponents(stack,
                            eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy, provider.getPacketContext(), alpha.registryAccess())), "Polymer native roundtrip");
                });
            }
            for (var item : List.of(Items.BARRIER, Items.REPEATING_COMMAND_BLOCK, Items.PAPER))
                require(!LbcPortalDropItem.matches(new ItemStack(item)), "vanilla lookalike rejected");
        }
        void endpoint(BlockPos base, BlockPos exit) {
            require(level.getBlockState(base.below()).is(Blocks.BEDROCK)
                    && level.getBlockState(base.above(4)).is(Blocks.BEDROCK)
                    && level.getBlockState(base.above(3)).is(Blocks.END_GATEWAY), "exact source supports and gateway offsets " + base);
            require(level.getBlockEntity(base.above(3)) instanceof TheEndGatewayBlockEntity, "native gateway block entity");
            var gateway = (TheEndGatewayBlockEntity) level.getBlockEntity(base.above(3));
            var tag = gateway.saveCustomOnly(level.registryAccess());
            require(tag.getBooleanOr("ExactTeleport", false)
                    && tag.read("exit_portal", BlockPos.CODEC).orElseThrow().equals(exit), "native exact integer XYZ target");
            require(Vec3.atBottomCenterOf(exit).equals(gateway.getPortalPosition(level, base.above(3))), "native exact teleport uses bottom center, with no safe-height rewrite");
            var provenance = ((LbcPortalDrops.StateAccess)gateway).skywars2$portalMetadata();
            require(provenance.getStringOr("epoch", "").equals(game.items().epoch())
                    && provenance.getStringOr("owner", "").equals(alpha.getUUID().toString()), "native endpoint retains author and admission epoch");
            var restored = new TheEndGatewayBlockEntity(base.above(3), Blocks.END_GATEWAY.defaultBlockState());
            restored.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
                    net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),tag));
            require(((LbcPortalDrops.StateAccess)restored).skywars2$portalMetadata().equals(provenance)
                    && Vec3.atBottomCenterOf(exit).equals(restored.getPortalPosition(level,base.above(3))),
                    "native BE codec roundtrip retains exact target, author and epoch");
        }
        void pair(LbcPortalDrops.Plan plan) {
            endpoint(plan.sourceBase(), plan.sourceExit()); endpoint(plan.remoteBase(), plan.remoteExit());
        }
        Cow cow(Vec3 at) {
            var cow = new Cow(EntityTypes.COW, level); cow.setPos(at); cow.setNoAi(true); cow.setNoGravity(true);
            require(level.addFreshEntity(cow), "native disposable portal traveler"); actors.add(cow); return cow;
        }
        void travel(LbcPortalDrops.Plan plan, boolean reverse) {
            var from = reverse ? plan.remoteBase() : plan.sourceBase();
            var to = reverse ? plan.remoteExit() : plan.sourceExit();
            var traveler = cow(Vec3.atBottomCenterOf(from.above(3)));
            traveler.setAsInsidePortal((Portal) Blocks.END_GATEWAY, from.above(3));
            traveler.tick();
            require(traveler.position().distanceTo(Vec3.atBottomCenterOf(to)) < 1e-8,
                    "native portal processor performs actual " + (reverse ? "reverse" : "forward") + " teleport to source XYZ");
            game.items().indirectKills().dying(traveler,traveler.damageSources().fall());
            require(traveler.getKillCredit() == alpha, "accepted native portal arrival retains thrower fall credit");
            game.items().indirectKills().dying(traveler,traveler.damageSources().fellOutOfWorld());
            require(traveler.getKillCredit() == alpha, "accepted native portal arrival retains thrower void credit");
            traveler.hurtServer(level,traveler.damageSources().playerAttack(beta),1);
            game.items().indirectKills().dying(traveler,traveler.damageSources().fall());
            require(traveler.getKillCredit() == beta, "newer native attacker overrides portal arrival cause");
            game.items().indirectKills().clear(traveler); traveler.discard();
        }
        void gatewayGuards(LbcPortalDrops.Plan plan) {
            var block = (net.minecraft.world.level.block.EndGatewayBlock)Blocks.END_GATEWAY;
            var pos = plan.sourceBase().above(3);
            for (String tag : List.of("strict_map_object", "spectator", "rcdenyalways", "sauvojen_tuntija", "magic_academy", "transformator")) {
                var actor = cow(Vec3.atBottomCenterOf(pos)); actor.entityTags().add(tag);
                require(block.getPortalDestination(level,actor,pos) == null, "native gateway denies protected role " + tag);
                actor.discard();
            }
            var actor = cow(Vec3.atBottomCenterOf(pos));
            game.items().timeStop().begin(level,200);
            try { require(block.getPortalDestination(level,actor,pos) == null, "native gateway travel pauses with time stop"); }
            finally { game.items().timeStop().end(level); }
            var state = (LbcPortalDrops.StateAccess)level.getBlockEntity(pos);
            var data = state.skywars2$portalMetadata(); String epoch = data.getStringOr("epoch", "");
            data.putString("epoch", "skywars2.items.stale-fixture");
            try { require(block.getPortalDestination(level,actor,pos) == null, "native loaded stale endpoint cannot teleport"); }
            finally { data.putString("epoch",epoch); }
            require(block.getPortalDestination(level,actor,pos) != null, "current native drop endpoint admits ordinary living actor");
            var technicalPassenger = cow(Vec3.atBottomCenterOf(pos)); technicalPassenger.entityTags().add("strict_map_object");
            require(technicalPassenger.startRiding(actor,true,false), "native technical passenger force mounts ordinary traveler");
            require(block.getPortalDestination(level,actor,pos) == null, "protected passenger blocks whole native vehicle teleport");
            technicalPassenger.stopRiding();
            require(block.getPortalDestination(level,actor,pos) != null, "dismounting protected passenger restores ordinary vehicle eligibility");
            var middlePassenger = cow(Vec3.atBottomCenterOf(pos));
            require(middlePassenger.startRiding(actor,true,false) && technicalPassenger.startRiding(middlePassenger,true,false),
                    "native nested passenger chain mounts");
            require(block.getPortalDestination(level,actor,pos) == null, "protected nested descendant blocks whole native vehicle teleport");
            technicalPassenger.stopRiding(); middlePassenger.stopRiding(); technicalPassenger.discard(); middlePassenger.discard();
            actor.discard();
            beta.setGameMode(GameType.SPECTATOR);
            var spectatorVehicle = cow(Vec3.atBottomCenterOf(pos));
            try {
                require(block.getPortalDestination(level,beta,pos) == null, "native spectator denied");
                require(beta.startRiding(spectatorVehicle,true,false), "native spectator force mounts ordinary traveler");
                require(block.getPortalDestination(level,spectatorVehicle,pos) == null, "spectator passenger cannot bypass native gateway role guard");
                beta.stopRiding();
                require(block.getPortalDestination(level,spectatorVehicle,pos) != null, "dismounting spectator restores ordinary vehicle eligibility");
            } finally { beta.stopRiding(); spectatorVehicle.discard(); beta.setGameMode(GameType.SURVIVAL); beta.setNoGravity(true); }
            game.spectators().enter(beta);
            try {
                beta.setGameMode(GameType.SURVIVAL);
                require(block.getPortalDestination(level,beta,pos) == null, "explicit observer denied in Survival");
            } finally { game.spectators().remove(beta); beta.setGameMode(GameType.SURVIVAL); beta.setNoGravity(true); }
            var board = game.server().getScoreboard(); var priorA = alpha.getTeam(); var priorB = beta.getTeam();
            var team = board.addPlayerTeam("codex_portal_probe"); team.setAllowFriendlyFire(false);
            try {
                board.addPlayerToTeam(alpha.getScoreboardName(),team); board.addPlayerToTeam(beta.getScoreboardName(),team);
                beta.setGameMode(GameType.CREATIVE);
                require(block.getPortalDestination(level,beta,pos) != null && block.getPortalDestination(level,alpha,pos) != null,
                        "native gateway remains WILD for allied Creative and thrower self");
                var allyVehicle = cow(Vec3.atBottomCenterOf(pos));
                board.addPlayerToTeam(allyVehicle.getScoreboardName(),team);
                try {
                    require(beta.startRiding(allyVehicle,true,false), "native allied ordinary passenger mounts");
                    require(block.getPortalDestination(level,allyVehicle,pos) != null,
                            "allied ordinary passenger remains eligible for whole native vehicle teleport");
                } finally { beta.stopRiding(); allyVehicle.discard(); }
            } finally {
                board.removePlayerTeam(team); if (priorA != null) board.addPlayerToTeam(alpha.getScoreboardName(),priorA);
                if (priorB != null) board.addPlayerToTeam(beta.getScoreboardName(),priorB);
                beta.setGameMode(GameType.SURVIVAL); beta.setNoGravity(true);
            }
            var vanillaPos = pos.offset(2,0,0); remember(vanillaPos);
            level.setBlock(vanillaPos,Blocks.END_GATEWAY.defaultBlockState(),818);
            ((TheEndGatewayBlockEntity)level.getBlockEntity(vanillaPos)).setExitPosition(plan.remoteBase(),true);
            var vanillaActor = cow(Vec3.atBottomCenterOf(vanillaPos)); vanillaActor.entityTags().add("strict_map_object");
            require(block.getPortalDestination(level,vanillaActor,vanillaPos) != null, "unowned native gateway retains vanilla travel");
            vanillaActor.discard();
        }
        void geometry() {
            var first = LbcPortalDrops.plan(new Vec3(-200.63, 250.7, -200.19), new Vec3(-220.5, 240, -220.5));
            var second = LbcPortalDrops.plan(new Vec3(-205.17, 250.9, -205.83), new Vec3(-225.5, 240, -225.5));
            require(first.sourceBase().equals(new BlockPos(-201, 250, -201))
                    && first.remoteBase().equals(new BlockPos(-221, 240, -221))
                    && first.sourceExit().equals(first.remoteBase()) && first.remoteExit().equals(first.sourceBase()),
                    "floor negative/fractional source and remote; reciprocal exits remain base XYZ rather than gateway Y+3");
            for (var plan : List.of(first, second)) {
                resident(plan.sourceBase()); resident(plan.remoteBase()); remember(plan);
                for (var base : List.of(plan.sourceBase(), plan.remoteBase())) {
                    for (int dy : new int[]{0, 1, 2}) {
                        remember(base.above(dy)); level.setBlock(base.above(dy), Blocks.AIR.defaultBlockState(), 818);
                    }
                }
                game.items().portalDrops().place(level, plan, alpha);
                pair(plan);
            }
            pair(first); pair(second); gatewayGuards(first); travel(first, false); travel(first, true); travel(second, false); travel(second, true);
            for (var source : List.of(new Vec3(-230.3, level.getMaxY()-3+.7, -230.6),
                                     new Vec3(-230.3, level.getMinY()+.7, -230.6))) {
                var bad = LbcPortalDrops.plan(source, new Vec3(-240.5, 240, -240.5));
                resident(bad.sourceBase()); resident(bad.remoteBase()); remember(bad);
                var prior = new LinkedHashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
                for (var base : List.of(bad.sourceBase(), bad.remoteBase())) for (int dy : new int[]{-1, 3, 4})
                    if (!level.isOutsideBuildHeight(base.above(dy))) prior.put(base.above(dy), level.getBlockState(base.above(dy)));
                game.items().portalDrops().place(level, bad, alpha);
                for (var entry : prior.entrySet()) require(level.getBlockState(entry.getKey()).equals(entry.getValue()),
                        "invalid full-height pair leaves both endpoint columns intact");
            }
            observed.clear();
        }
        void spawnHeights() {
            var column = new BlockPos(-235, 90, -235); resident(column);
            for (int y = level.getMinY(); y < level.getMaxY(); y++) {
                var pos = new BlockPos(column.getX(),y,column.getZ()); remember(pos);
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),818);
            }
            level.setBlock(column,Blocks.STONE.defaultBlockState(),818);
            level.setBlock(column.atY(118),Blocks.STONE.defaultBlockState(),818);
            level.setBlock(column.atY(130),Blocks.STONE.defaultBlockState(),818);
            require(LbcPortalDrops.spawnY(level,column,120) == 119
                    && LbcPortalDrops.spawnY(level,column,level.getMaxY()+1) == 131,
                    "native Nether under120 ignores upper roof; ordinary spread selects highest two-air surface");
            level.setBlock(column.atY(119),Blocks.STONE.defaultBlockState(),818);
            require(LbcPortalDrops.spawnY(level,column,120) == 120, "Nether source admits support119 and arrival120");
            level.setBlock(column.atY(120),Blocks.STONE.defaultBlockState(),818);
            require(LbcPortalDrops.spawnY(level,column,120) == 91, "two-air requirement skips blocked high surface");
            level.setBlock(column,Blocks.AIR.defaultBlockState(),818);
            require(LbcPortalDrops.spawnY(level,column,120) == 121, "native unsuccessful search returns maxHeight+1");
        }
        ItemEntity drop(Vec3 at, int count, LbcPortalDropItem.Kind kind) {
            resident(BlockPos.containing(at)); var item = token(level, at, count, alpha, kind); tokens.add(item); return item;
        }
        void guards() {
            var stale = drop(origin, 2, LbcPortalDropItem.Kind.NORMAL); stale.entityTags().add("skywars2.items.stale-fixture");
            require(!game.items().specialDrops().spawn(stale) && stale.getItem().getCount() == 2, "stale token cannot debit or queue"); stale.discard();
            var fake = drop(origin, 2, LbcPortalDropItem.Kind.NORMAL); fake.makeFakeItem();
            require(!game.items().specialDrops().spawn(fake) && fake.getItem().getCount() == 2, "native give visual cannot debit"); fake.discard();
            var remote = drop(origin, 2, LbcPortalDropItem.Kind.OLD);
            int x = 4096, z = 4096; while (level.getChunkSource().getChunkNow(x,z) != null) x += 128;
            remote.setPos(x * 16 + 8, origin.y, z * 16 + 8);
            require(!game.items().specialDrops().spawn(remote) && remote.getItem().getCount() == 2
                    && level.getChunkSource().getChunkNow(x,z) == null, "unresident source neither loads nor debits"); remote.discard();
            require(game.items().portalDrops().idle(), "all rejected fixtures leave no asynchronous jobs");
        }
        void dispatch(boolean free) {
            game.config().noBorders = free; initialObserved = observed.size(); checkingSpread = true;
            for (var kind : LbcPortalDropItem.Kind.values()) {
                // Keep the real random search centered near the map's reachable terrain in both modes.
                var spreadOrigin = free ? origin.add(200,8,200) : origin;
                var at = spreadOrigin.add(kind == LbcPortalDropItem.Kind.NORMAL ? 0 : 10,0,0);
                var item = drop(at, 2, kind);
                require(!game.items().specialDrops().spawn(item) && item.getItem().getCount() == 2,
                        "unready asynchronous admission preserves source stack " + kind);
                activeTokens.put(BlockPos.containing(at), item);
                sourcePositions.put(BlockPos.containing(at),at);
            }
            require(game.items().portalDrops().pendingCount() == 2, "two simultaneous isolated pending activations");
            wait = 0;
        }
        void drainCompleted() {
            for (var plan : observed.subList(initialObserved, observed.size())) {
                var item = activeTokens.remove(plan.sourceBase());
                if (item == null) continue;
                require(!item.isRemoved() && item.getItem().getCount() == 1,
                        "real native tick places one pair and debits one source unit");
                require(!game.items().specialDrops().spawn(item) && item.getItem().getCount() == 1,
                        "same-tick duplicate rejects second placement/debit");
                item.discard();
            }
        }
        void verifySpread(boolean free) {
            checkingSpread = false;
            require(observed.size() == initialObserved + 2, "two independently completed native random pairs");
            for (var plan : observed.subList(initialObserved, observed.size())) {
                pair(plan);
                var source = sourcePositions.get(plan.sourceBase());
                require(source != null, "random pair retains exact fractional activation origin");
                double x = free ? (float)source.x : .5F;
                double z = free ? (float)source.z : .5F;
                int radius = free ? 200 : 170;
                require(plan.remoteBase().getX() >= Math.floor(x-radius)
                        && plan.remoteBase().getX() <= Math.floor(x+radius)
                        && plan.remoteBase().getZ() >= Math.floor(z-radius)
                        && plan.remoteBase().getZ() <= Math.floor(z+radius),
                        "native center-corrected absolute/float relative square spread bounds " + free + " " + plan);
            }
        }
        boolean step() {
            require(game.playing(), "actual match retained"); int now = game.server().getTickCount();
            if (now == lastTick) return false; lastTick = now;
            switch (phase) {
                case 0 -> {
                    require(ItemStack.isSameItemSameComponents(alpha.getInventory().getItem(0), new ItemStack(LbcPortalDropItem.Kind.NORMAL.item())), "native give exact prototype");
                    alpha.getInventory().clearContent(); dispatch(false); phase++;
                }
                case 1 -> {
                    require(++wait < 600, "bounded border-mode random pair completion");
                    drainCompleted();
                    if (game.items().portalDrops().idle()) { verifySpread(false); dispatch(true); phase++; }
                }
                case 2 -> {
                    require(++wait < 600, "bounded unbordered random pair completion");
                    drainCompleted();
                    if (game.items().portalDrops().idle()) {
                        verifySpread(true); initialObserved = observed.size(); game.items().timeStop().begin(level,200);
                        paused = drop(origin.add(20,0,0), 2, LbcPortalDropItem.Kind.NORMAL); wait = 0; phase++;
                    }
                }
                case 3 -> {
                    require(paused.getItem().getCount() == 2 && game.items().portalDrops().idle()
                            && observed.size() == initialObserved && !game.items().specialDrops().spawn(paused),
                            "time stop pauses admission, debit and asynchronous placement");
                    if (++wait == 3) {
                        paused.discard(); game.items().timeStop().end(level);
                        unloaded = drop(origin,2,LbcPortalDropItem.Kind.OLD);
                        require(!game.items().specialDrops().spawn(unloaded) && game.items().portalDrops().pendingCount() == 1,
                                "queued native source awaiting asynchronous readiness");
                        game.items().specialDrops().unloaded(unloaded); game.items().portalDrops().unloaded(unloaded); phase++;
                    }
                }
                case 4 -> {
                    require(!unloaded.isRemoved() && unloaded.getItem().getCount() == 2
                            && game.items().portalDrops().idle() && observed.size() == initialObserved,
                            "unloaded queued source cannot replay or place endpoints"); unloaded.discard(); return true;
                }
                default -> throw new IllegalStateException("Unknown portal phase " + phase);
            }
            return false;
        }
        public void close() {
            game.items().portalDrops().beforePlace = null;
            game.items().portalDrops().stop(); game.items().timeStop().end(level);
            tokens.forEach(Entity::discard);
            for (var actor : actors) { if (actor instanceof net.minecraft.world.entity.LivingEntity living) game.items().indirectKills().clear(living); actor.discard(); }
            for (var entry : terrain.entrySet()) {
                if (level.getBlockEntity(entry.getKey()) instanceof net.minecraft.world.Container container) container.clearContent();
                game.maps().restoreSnapshot(level, entry.getKey(), entry.getValue());
            }
            for (long key : forced) level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key), net.minecraft.world.level.ChunkPos.getZ(key), false);
            game.config().noBorders = noBorders; alphaState.restore(game); betaState.restore(game);
        }
    }
    private static void failure(State state, Throwable error) {
        try { state.close(); } catch (Throwable cleanup) { if (error != cleanup) error.addSuppressed(cleanup); }
        finally { running = null; }
    }
    public static void run(GameController game) {
        require(running == null && stopTick == Integer.MIN_VALUE, "already running"); running = new State(game);
        try { running.initialize(); } catch (RuntimeException | Error error) { failure(running,error); throw error; }
    }
    public static void tick(GameController game) {
        if (stopTick != Integer.MIN_VALUE) {
            if (game.server().getTickCount() == stopTick) return;
            stopTick = Integer.MIN_VALUE;
            try {
                var level = game.maps().level(); var alpha = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
                stopEpoch = game.items().epoch(); game.items().timeStop().end(level);
                stopToken = token(level,alpha.position().add(0,4,0),3,alpha,LbcPortalDropItem.Kind.NORMAL);
                require(!game.items().specialDrops().spawn(stopToken) && game.items().portalDrops().pendingCount() == 1,
                        "actual-stop probe admits asynchronous source without premature debit");
                game.items().timeStop().begin(level,200);
                require(!game.items().specialDrops().spawn(stopToken) && stopToken.getItem().getCount() == 3
                        && game.items().portalDrops().pendingCount() == 1, "actual-stop queued job is frozen without debit");
                SkyWars2.LOGGER.info("[check] PASS portal-drops-stop-probe");
            } catch (Throwable error) {
                if (stopToken != null) stopToken.discard(); SkyWars2.LOGGER.error("[check] FAIL portal-drops-stop-probe",error);
            }
            return;
        }
        if (running == null) return; var state = running;
        try {
            if (state.step()) { state.close(); running = null; SkyWars2.LOGGER.info("[check] PASS portal-drops"); }
        } catch (Throwable error) { failure(state,error); SkyWars2.LOGGER.error("[check] FAIL portal-drops",error); }
    }
    public static void stopProbe(GameController game) {
        require(game.playing() && running == null && stopTick == Integer.MIN_VALUE, "actual stop probe");
        game.items().timeStop().begin(game.maps().level(),200); stopTick = game.server().getTickCount();
    }
    public static void cleanup(GameController game) {
        require(stopEpoch != null && !game.items().currentEpoch(stopEpoch) && stopToken != null
                && stopToken.entityTags().contains(stopEpoch) && stopToken.getItem().getCount() == 3
                && !game.items().specialDrops().spawn(stopToken) && game.items().portalDrops().idle(),
                "actual match stop invalidates pending token/epoch, releases jobs and cannot replay");
        stopToken.discard(); stopToken = null; stopEpoch = null;
        SkyWars2.LOGGER.info("[check] PASS portal-drops-cleanup");
    }
}
