package me.skywars2.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.ActorCheckpointAccess;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Persistent Nexus bodies cross an actual round reset, a lobby, and the next match. */
final class NexusLobbyChecks {
    private static Fixture active;

    static void run(GameController game) {
        require(active == null, "exclusive fixture");
        var fixture = new Fixture(game);
        try {
            fixture.begin();
            active = fixture;
        } catch (Throwable error) {
            try { fixture.close(); } catch (Throwable cleanup) { error.addSuppressed(cleanup); }
            throw error;
        }
    }

    static void tick(GameController game) {
        var fixture = active;
        if (fixture == null || fixture.game != game) return;
        try {
            if (!fixture.step()) return;
            active = null;
            fixture.close();
            SkyWars2.LOGGER.info("[check] PASS nexus-lobby (native lobby return and new match, intact terrain/cow/item/cell/tree, same hidden cage graph, resumed cell/XP/thread recipe, cage exchange, native Loli and shulker loot, inert Q-dropped trap)");
        } catch (Throwable error) {
            active = null;
            try { fixture.close(); } catch (Throwable cleanup) { error.addSuppressed(cleanup); }
            SkyWars2.LOGGER.error("[check] FAIL nexus-lobby", error);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Nexus lobby: " + message);
    }

    private static final class Fixture {
        private static final Vec3 ARRIVAL = new Vec3(8.5, 180, 10.5);
        private static final Vec3 CAGE = new Vec3(8.5, 180, 12.5);
        private static final Vec3 COLD = new Vec3(2304.5, 180, 2304.5);
        private static final Vec3 THREADS = new Vec3(3.5, 180, 3.5);
        private static final Vec3 EXPERIENCE = new Vec3(12.5, 180, 3.5);
        private static final BlockPos CELL = new BlockPos(11, 180, 8);
        private static final BlockPos LANDSCAPE = new BlockPos(2, 180, 8);
        private static final BlockPos LUCKY_BLOCK = new BlockPos(7, 180, 10);
        private static final BlockPos SHULKER = new BlockPos(10, 180, 10);
        private static final ChunkPos STAGED_COLUMN = new ChunkPos(0, 0);
        private static final int SIMULATION_RADIUS = 3;
        private static final String PAUSED = "time_stopped";
        final GameController game;
        final ServerLevel nexus;
        final ServerPlayer owner, other;
        final RegenerationBeltChecks.ScheduledClient client, audience;
        final GameChecks.WandThirdPlayerState ownerStart;
        final MatchValueSnapshot ownerProfile, otherProfile;
        final int started, originalCountdown;
        final boolean originalNoGravity;
        final Map<BlockPos, MapManager.BlockSnapshot> terrain = new LinkedHashMap<>();
        final List<Entity> owned = new ArrayList<>();
        final List<ItemEntity> threads = new ArrayList<>();
        final Set<UUID> tntBefore = new HashSet<>();
        Cow cow;
        ItemEntity ordinary;
        ItemEntity trap;
        ExperienceOrb xp;
        LbcNexusAcquisition.Cage cage;
        List<Entity> assembly;
        ItemStack cageLoot;
        LbcNexusTools.Actor cell;
        LbcNexusEcology.Actor tree;
        int phase, phaseStarted;
        boolean forced, crossedReset;

        Fixture(GameController game) {
            this.game = game;
            nexus = Objects.requireNonNull(game.server().getLevel(LbcHedgehogDimensions.NEXUS));
            owner = Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            other = Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            require(game.playing() && game.allowDamage(owner) && game.allowDamage(other)
                    && !game.bedwars().mode() && !game.maps().busy() && !game.vanity().hasSnapshot()
                    && !game.items().timeStop().stopped(nexus), "settled ordinary two-player match and running Nexus");
            ownerStart = new GameChecks.WandThirdPlayerState(owner, game.items().mana());
            ownerProfile = new MatchValueSnapshot(game.players().get(owner.getUUID()));
            otherProfile = new MatchValueSnapshot(game.players().get(other.getUUID()));
            originalNoGravity = owner.isNoGravity();
            originalCountdown = game.config().countdownSeconds;
            started = game.server().getTickCount();
            client = new RegenerationBeltChecks.ScheduledClient(game, owner);
            audience = new RegenerationBeltChecks.ScheduledClient(game, other);
        }

        void begin() {
            // Level 30 is independent of the vanilla /forceload level-31 lease.
            // Keep the exact same native entities available after both players leave Nexus.
            nexus.getChunkSource().addTicketWithRadius(TicketType.FORCED, STAGED_COLUMN, SIMULATION_RADIUS);
            forced = true;
            moveToNexus();
        }

        void moveToNexus() {
            owner.closeContainer();
            owner.stopUsingItem();
            owner.setNoGravity(true);
            owner.setDeltaMovement(Vec3.ZERO);
            require(owner.teleportTo(nexus, ARRIVAL.x, ARRIVAL.y, ARRIVAL.z, Set.of(), 0, 0, true),
                    "native player transfer into Nexus");
            client.acknowledge();
        }

        boolean visible(Entity entity) {
            return !entity.isRemoved() && nexus.getEntity(entity.getUUID()) == entity;
        }

        boolean registered(Entity entity) {
            return ((ActorCheckpointAccess.LevelEntities) nexus).skywars2$manager().isLoaded(entity.getUUID());
        }

        boolean sceneReady() {
            return nexus.getChunkSource().getChunkNow(STAGED_COLUMN.x(), STAGED_COLUMN.z()) != null
                    && nexus.areEntitiesActuallyLoadedAndTicking(STAGED_COLUMN)
                    && nexus.isPositionEntityTicking(LANDSCAPE);
        }

        String state(Entity body) {
            var lookup = nexus.getEntity(body.getUUID());
            return "uuid=" + body.getUUID() + ", reason=" + body.getRemovalReason()
                    + ", registered=" + registered(body) + ", lookup=" + (lookup == body ? "same" : lookup)
                    + ", position=" + body.position() + ", world=" + body.level().dimension()
                    + ", sceneReady=" + sceneReady();
        }

        boolean cold() {
            var chunk = ChunkPos.containing(BlockPos.containing(COLD));
            return nexus.getChunkSource().getChunkNow(chunk.x(), chunk.z()) == null
                    && !nexus.areEntitiesLoaded(chunk.pack());
        }

        void set(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            terrain.computeIfAbsent(pos, key -> game.maps().snapshot(nexus, key));
            nexus.setBlock(pos, state, 2);
        }

        <T extends Entity> T add(T actor, Vec3 position) {
            actor.setPos(position);
            actor.setNoGravity(true);
            actor.setDeltaMovement(Vec3.ZERO);
            owned.add(actor);
            require(nexus.addFreshEntity(actor) && visible(actor), "fixture actor is genuinely registered: " + actor);
            return actor;
        }

        ItemEntity drop(Item item, Vec3 point, boolean paused) {
            var entity = new ItemEntity(nexus, point.x, point.y, point.z, new ItemStack(item));
            entity.setPickUpDelay(1200);
            if (paused) entity.entityTags().add(PAUSED);
            return add(entity, point);
        }

        void install() {
            require(cold() && game.items().nexusTools().snapshot().stream().noneMatch(a -> a.body.level() == nexus),
                    "isolated Nexus cell cohort and hidden column");
            var site = new AABB(0, 177, 0, 16, 184, 16);
            require(nexus.getEntitiesOfClass(ItemEntity.class, site, e -> true).isEmpty()
                    && game.items().nexusAcquisition().cages().stream().noneMatch(c -> c.level == nexus && site.contains(c.position)),
                    "no foreign drops or cages share the fixture scene");
            for (var pos : BlockPos.betweenClosed(BlockPos.containing(CAGE).offset(-1, -1, -1),
                    BlockPos.containing(CAGE).offset(1, 1, 1))) terrain.put(pos.immutable(), game.maps().snapshot(nexus, pos));
            set(LANDSCAPE, Blocks.DIAMOND_BLOCK.defaultBlockState());
            set(CELL, Blocks.SMOOTH_STONE.defaultBlockState());
            for (var face : Direction.values()) set(CELL.relative(face), Blocks.CONCRETE.pick(DyeColor.BLACK).defaultBlockState());

            cow = add(Objects.requireNonNull(EntityTypes.COW.create(nexus, EntitySpawnReason.TRIGGERED)), new Vec3(2.5, 181, 12.5));
            cow.setNoAi(true);
            ordinary = drop(Items.ECHO_SHARD, new Vec3(2.5, 180, 11.5), false);
            var cause = game.items().indirectKills().wildCause(owner);
            cell = Objects.requireNonNull(game.items().nexusTools().cube(nexus, Vec3.atCenterOf(CELL), cause));
            owned.add(cell.body);
            cell.body.entityTags().add(PAUSED);
            tree = Objects.requireNonNull(game.items().nexusAcquisition().ecology().tree(nexus, new Vec3(3.5, 182, 12.5), cause));
            owned.add(tree.body);
            tree.body.entityTags().add(PAUSED);

            var acquisition = game.items().nexusAcquisition();
            var old = acquisition.cages().stream().map(c -> c.id).collect(java.util.stream.Collectors.toSet());
            acquisition.createCage(nexus, CAGE, 4, cause);
            var created = acquisition.cages().stream().filter(c -> !old.contains(c.id)).toList();
            require(created.size() == 1, "actual native white cage and its rolled reward");
            cage = created.getFirst();
            assembly = List.of(cage.body, cage.reward, cage.frame);
            cageLoot = cage.reward.getItemStack().copy();
            owned.addAll(assembly);
            require(!cageLoot.isEmpty() && assembly.stream().allMatch(this::visible), "three visible linked cage bodies");

            threads.add(drop(LbcNexusWireItems.RED, THREADS, true));
            threads.add(drop(LbcNexusWireItems.BLUE, THREADS.add(1, 0, 0), true));
            threads.add(drop(LbcNexusWireItems.BLACK, THREADS.add(0, 0, 1), true));
            xp = new ExperienceOrb(nexus, EXPERIENCE, Vec3.ZERO, 13);
            xp.entityTags().add(PAUSED);
            add(xp, EXPERIENCE);
        }

        void requirePreserved() {
            require(visible(cow), "original ordinary Nexus cow survives: " + state(cow) + ", health=" + cow.getHealth());
            require(visible(ordinary) && ordinary.getItem().is(Items.ECHO_SHARD) && ordinary.getItem().getCount() == 1,
                    "original ordinary Nexus item/count survives: " + state(ordinary) + ", stack=" + ordinary.getItem());
            require(nexus.getBlockState(LANDSCAPE).is(Blocks.DIAMOND_BLOCK),
                    "original Nexus terrain survives: actual=" + nexus.getBlockState(LANDSCAPE));
            require(visible(cell.body) && nexus.getBlockState(CELL).is(Blocks.SMOOTH_STONE),
                    "original paused Nexus Corrupter cell survives without running early: " + state(cell.body)
                            + ", block=" + nexus.getBlockState(CELL));
            require(visible(tree.body) && game.items().nexusAcquisition().ecology().snapshot().contains(tree),
                    "exact paused Nexus tree remains visible and indexed: " + state(tree.body));
            require(visible(xp), "original pending XP survives: " + state(xp));
            for (var thread : threads) require(visible(thread), "original thread survives: " + state(thread));
        }

        void fallPolicy() {
            var ordinaryWorld = other.level();
            require(ordinaryWorld != nexus, "ordinary-world control remains outside Nexus");
            var comparison = Objects.requireNonNull(EntityTypes.COW.create(ordinaryWorld, EntitySpawnReason.TRIGGERED));
            comparison.setNoAi(true);
            comparison.setNoGravity(true);
            comparison.setPos(other.position().add(3, 1, 0));
            owned.add(comparison);
            require(ordinaryWorld.addFreshEntity(comparison), "actual ordinary-world fall damage control admitted");
            var rules = game.server().getGameRules();
            boolean old = rules.get(GameRules.FALL_DAMAGE);
            rules.set(GameRules.FALL_DAMAGE, true, game.server());
            try {
                float nexusHealth = cow.getHealth(), ordinaryHealth = comparison.getHealth();
                require(comparison.causeFallDamage(9, 1, comparison.damageSources().fall())
                        && comparison.getHealth() < ordinaryHealth, "ordinary native fall is still damaging with the shared rule enabled");
                require(!cow.causeFallDamage(9, 1, cow.damageSources().fall()) && cow.getHealth() == nexusHealth,
                        "the same real fall leaves the Nexus cow unharmed with ordinary fall enabled");
            } finally {
                rules.set(GameRules.FALL_DAMAGE, old, game.server());
                comparison.discard();
            }
        }

        List<ItemEntity> itemsAt(Vec3 point, double radius, java.util.function.Predicate<ItemStack> test) {
            return nexus.getEntitiesOfClass(ItemEntity.class, new AABB(point, point).inflate(radius),
                    item -> !item.isRemoved() && test.test(item.getItem()));
        }

        BlockHitResult top(BlockPos block) {
            return new BlockHitResult(Vec3.atCenterOf(block).add(0, .5, 0), Direction.UP, block, false);
        }

        void place(BlockPos block, ItemStack stack) {
            owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
            owner.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            require(owner.gameMode.useItemOn(owner, nexus, stack, InteractionHand.MAIN_HAND, top(block.below())).consumesAction()
                    && owner.getMainHandItem().isEmpty(), "native placement consumes one authored block: " + block);
        }

        List<ItemEntity> breakLoli(boolean silk) {
            var at = Vec3.atCenterOf(LUCKY_BLOCK);
            var source = game.lbc().rollTable(nexus, at, owner, "lbc:ported/lucky_block_loli");
            var kind = LbcLuckyBlocks.Kind.LOLI;
            require(source.size() == 1 && source.getFirst().is(LbcLuckyBlocks.item(kind)), "authored native Loli BlockItem");
            place(LUCKY_BLOCK, source.getFirst());
            require(nexus.getBlockState(LUCKY_BLOCK).is(LbcLuckyBlocks.block(kind)), "actual Nexus block is the registered Loli");
            var tool = new ItemStack(Items.DIAMOND_PICKAXE);
            if (silk) tool.enchant(owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.SILK_TOUCH), 1);
            owner.setItemInHand(InteractionHand.MAIN_HAND, tool);
            require(owner.gameMode.destroyBlock(LUCKY_BLOCK) && nexus.getBlockState(LUCKY_BLOCK).isAir(),
                    "real native Loli break in Nexus");
            var emitted = itemsAt(at, .95, stack -> true);
            owned.addAll(emitted);
            return emitted;
        }

        void blockAndTrapPolicy() {
            require(nexus.getBlockState(LUCKY_BLOCK).isAir() && nexus.getBlockState(SHULKER).isAir()
                    && nexus.getBlockState(SHULKER.above()).isAir()
                    && itemsAt(Vec3.atCenterOf(LUCKY_BLOCK), .95, stack -> true).isEmpty(),
                    "isolated native block and container sites");
            for (var block : List.of(LUCKY_BLOCK, SHULKER)) {
                set(block.below(), Blocks.STONE.defaultBlockState());
                set(block, Blocks.AIR.defaultBlockState());
            }
            var silk = breakLoli(true);
            require(silk.size() == 1 && silk.getFirst().getItem().getCount() == 1
                    && silk.getFirst().getItem().is(LbcLuckyBlocks.item(LbcLuckyBlocks.Kind.LOLI)),
                    "Loli's native Silk Touch table yields exactly one canonical Loli stack");
            silk.forEach(Entity::discard);
            List<ItemEntity> rewards = List.of();
            for (int attempt = 0; attempt < 3 && rewards.isEmpty(); attempt++) rewards = breakLoli(false);
            require(!rewards.isEmpty(), "ordinary unenchanted Nexus Loli breaks publish authored loot");
            rewards.forEach(Entity::discard);

            var shell = game.lbc().rollTable(nexus, Vec3.atCenterOf(SHULKER), owner, "lbc:inf_shulker");
            require(shell.size() == 1 && shell.getFirst().is(Items.DYED_SHULKER_BOX.pick(DyeColor.BLACK)), "authored infinite shulker BlockItem");
            var stack = shell.getFirst();
            var loot = Objects.requireNonNull(stack.get(DataComponents.CONTAINER_LOOT));
            require(loot.lootTable().identifier().toString().equals("lbc:infinity"), "exact source deferred loot table");
            stack.set(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(loot.lootTable(), 174329L));
            place(SHULKER, stack);
            require(nexus.getBlockEntity(SHULKER) instanceof ShulkerBoxBlockEntity, "native placed Nexus shulker has a block entity");
            var container = (ShulkerBoxBlockEntity) nexus.getBlockEntity(SHULKER);
            require(loot.lootTable().equals(container.getLootTable()), "placement keeps the original unopened infinite table");
            require(owner.gameMode.useItemOn(owner, nexus, ItemStack.EMPTY, InteractionHand.MAIN_HAND, top(SHULKER)).consumesAction()
                    && owner.containerMenu != owner.inventoryMenu && container.getLootTable() == null,
                    "ordinary native opening unpacks the real Nexus shulker loot");
            int filled = 0;
            for (int slot = 0; slot < container.getContainerSize(); slot++) if (!container.getItem(slot).isEmpty()) filled++;
            require(filled == 27, "seeded infinite Nexus shulker yields all 27 occupied slots, actual=" + filled);
            owner.closeContainer();

            var trapStack = new ItemStack(LbcTntDropItem.TOKEN);
            var area = owner.getBoundingBox().inflate(6);
            require(nexus.getEntitiesOfClass(ItemEntity.class, area, e -> LbcTntDropItem.matches(e.getItem())).isEmpty(),
                    "no unrelated TNT trap token in the drop scene");
            nexus.getEntitiesOfClass(PrimedTnt.class, owner.getBoundingBox().inflate(16), tnt -> true)
                    .forEach(tnt -> tntBefore.add(tnt.getUUID()));
            owner.setItemInHand(InteractionHand.MAIN_HAND, trapStack);
            owner.drop(false);
            var thrown = nexus.getEntitiesOfClass(ItemEntity.class, area, e -> LbcTntDropItem.matches(e.getItem()));
            owned.addAll(thrown);
            require(thrown.size() == 1 && owner.getMainHandItem().isEmpty(),
                    "standalone trap uses the real Q-drop and publishes one inert candidate");
            trap = thrown.getFirst();
            trap.setDeltaMovement(Vec3.ZERO);
            trap.setNoGravity(true);
        }

        void requireInertTrap() {
            require(visible(trap) && trap.getItem().is(LbcTntDropItem.TOKEN) && trap.getItem().getCount() == 1,
                    "native Q-dropped TNT token remains intact after simulation: " + state(trap));
            require(nexus.getEntitiesOfClass(PrimedTnt.class, owner.getBoundingBox().inflate(16),
                            tnt -> !tntBefore.contains(tnt.getUUID())).isEmpty(),
                    "no TNT actor was spawned by the inert ordinary trap in Nexus");
        }

        boolean step() {
            client.acknowledge();
            audience.acknowledge();
            int tick = game.server().getTickCount();
            require(tick - started < 1400, "bounded native test, phase=" + phase + " stage=" + game.stage());
            switch (phase) {
                case 0 -> {
                    if (!client.ready() || !sceneReady()) return false;
                    install();
                    requirePreserved();
                    game.stopMatch();
                    crossedReset = !game.playing();
                    require(crossedReset, "actual stop requests lobby load");
                    phase = 1;
                }
                case 1 -> {
                    if (!game.inLobby()) return false;
                    require(owner.level() == game.maps().level() && owner.level() != nexus, "canonical lobby transfers player out");
                    require(cow.getRemovalReason() != Entity.RemovalReason.DISCARDED
                            && ordinary.getRemovalReason() != Entity.RemovalReason.DISCARDED,
                            "real lobby does not discard ordinary Nexus bodies: cow " + state(cow) + "; item " + state(ordinary));
                    if (!sceneReady()) return false;
                    requirePreserved();
                    require(assembly.stream().allMatch(this::visible), "original cage graph survives while lobby is open");
                    assembly.getFirst().setPos(COLD);
                    require(cold() && registered(assembly.getFirst()) && !visible(assembly.getFirst()),
                            "real native cage tracking ends without unregistering the body");
                    phaseStarted = tick;
                    phase = 2;
                }
                case 2 -> {
                    if (tick - phaseStarted < 2) return false;
                    var body = assembly.getFirst();
                    require(!body.isRemoved() && registered(body) && !visible(body) && cold(),
                            "deferred native visibility loss retains the registered cage, without loading the cold column");
                    body.setPos(CAGE);
                    require(assembly.stream().allMatch(this::visible) && cage.body == body
                            && cage.reward == assembly.get(1) && cage.frame == assembly.get(2),
                            "tracking rebinds the original cage and both displays without another table roll");
                    game.config().countdownSeconds = 1;
                    game.requestStart(null, false);
                    phase = 3;
                }
                case 3 -> {
                    if (!game.playing() || !game.allowDamage(owner) || game.maps().busy()) return false;
                    if (!sceneReady()) return false;
                    requirePreserved();
                    require(owner.level() != nexus && !owner.hasEffect(MobEffects.NIGHT_VISION)
                            && owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200)),
                            "ordinary native effect is present before the actual Nexus transfer");
                    fallPolicy();
                    moveToNexus();
                    cow.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200));
                    phaseStarted = tick;
                    phase = 4;
                }
                case 4 -> {
                    if (!client.ready() || tick == phaseStarted) return false;
                    require(!owner.hasEffect(MobEffects.NIGHT_VISION) && !cow.hasEffect(MobEffects.NIGHT_VISION),
                            "Nexus purifies imported player and ordinary native mob effects before gameplay resumes");
                    owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(LbcNexusWireItems.WHITE, 2));
                    owner.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                    require(owner.isWithinEntityInteractionRange(cage.body, 0)
                            && UseEntityCallback.EVENT.invoker().interact(owner, nexus, InteractionHand.MAIN_HAND, cage.body, null).consumesAction(),
                            "normal post-reentry converter interaction accepts its matching white thread");
                    var released = itemsAt(CAGE, 1.2, stack -> ItemStack.matches(stack, cageLoot));
                    owned.addAll(released);
                    require(released.size() == 1 && owner.getMainHandItem().getCount() == 1 && assembly.stream().allMatch(Entity::isRemoved),
                            "one thread debited, same saved stack dropped, and exact cage graph retired");
                    for (var actor : threads) actor.entityTags().remove(PAUSED);
                    cell.body.entityTags().remove(PAUSED);
                    xp.entityTags().remove(PAUSED);
                    phaseStarted = tick;
                    phase = 5;
                }
                case 5 -> {
                    require(tick - phaseStarted < 100, "scheduled original cell, wire recipe and XP resume after re-entry");
                    var white = itemsAt(THREADS, 2, stack -> stack.is(LbcNexusWireItems.WHITE));
                    var red = itemsAt(EXPERIENCE, 1.2, LbcNexusWireItems::isRed);
                    if (white.isEmpty() || red.isEmpty() || !cell.body.isRemoved() || !xp.isRemoved()) return false;
                    owned.addAll(white);
                    owned.addAll(red);
                    require(white.size() == 1 && white.getFirst().getItem().getCount() == 1
                            && threads.stream().allMatch(Entity::isRemoved), "three original threads craft exactly one white output");
                    require(red.size() == 1 && red.getFirst().getItem().getCount() == 1, "original queued XP makes one red thread");
                    require(nexus.getBlockState(CELL).is(Blocks.CONCRETE.pick(DyeColor.BLACK)) && nexus.getBlockState(LANDSCAPE).is(Blocks.DIAMOND_BLOCK)
                            && visible(cow) && visible(ordinary) && visible(tree.body) && cold(),
                            "original cell actually advances while permanent world contents and the cold column remain intact");
                    blockAndTrapPolicy();
                    phaseStarted = tick;
                    phase = 6;
                }
                case 6 -> {
                    if (tick - phaseStarted < 8) return false;
                    requireInertTrap();
                    return true;
                }
                default -> throw new IllegalStateException("Unexpected Nexus lobby phase " + phase);
            }
            return false;
        }

        void close() {
            try {
                owner.closeContainer();
                if (terrain.containsKey(SHULKER) && nexus.getBlockEntity(SHULKER) instanceof ShulkerBoxBlockEntity box) {
                    box.setLootTable(null);
                    box.clearContent();
                }
                if (!terrain.isEmpty()) {
                    owned.addAll(itemsAt(THREADS, 2, stack -> stack.is(LbcNexusWireItems.WHITE)));
                    owned.addAll(itemsAt(EXPERIENCE, 1.2, LbcNexusWireItems::isRed));
                }
                owned.forEach(Entity::discard);
                terrain.forEach((pos, state) -> game.maps().restoreSnapshot(nexus, pos, state));
                if (!crossedReset && game.playing()) ownerStart.restore(game);
                else owner.setNoGravity(originalNoGravity);
                game.config().countdownSeconds = originalCountdown;
                ownerProfile.restore();
                otherProfile.restore();
                game.players().saveDeferred();
            } finally {
                if (forced) nexus.getChunkSource().removeTicketWithRadius(TicketType.FORCED, STAGED_COLUMN, SIMULATION_RADIUS);
                try { client.close(); } finally { audience.close(); }
            }
        }
    }
}
