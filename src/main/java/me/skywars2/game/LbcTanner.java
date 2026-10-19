package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Источниковый арканум: превращает подходящие блоки и снаряды в землю вокруг носителя. */
public final class LbcTanner {
    private static final String ITEM_ID = "tannerkivi";
    private static final int ACTIVATION = 500;
    private static final int UPKEEP = 15;
    private static final int WORK_BUDGET = 65536;
    private static final int BATCH_SIZE = 256;
    private static final long WORK_NANOS = 3_000_000L;
    private static final int BLOCK_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
            | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS | Block.UPDATE_SKIP_ON_PLACE;

    private static final TagKey<EntityType<?>> PROJECTILES = TagKey.create(
            Registries.ENTITY_TYPE, Identifier.parse("minecraft:projectiles_tannerkivi"));
    private static final Set<String> EXCLUDED_PROJECTILE_TAGS = Set.of(
            "swrg.center", "swrg.default", "swrg.look", "swrg.midle", "swrg.spawn",
            "magic_academy", "transformator", "danmaku_bomb", "danmaku_bomb_fast",
            "crocodildo", "crocodildo_hitbox", "helicopter_boss", "helicopter_boss_hitbox",
            "grimoire_boss_hitbox", "grimoire_boss_visual");
    private static final CompoundTag ITEM_DATA = itemData();
    private static final CompoundTag TANNER_FLAG = tannerFlag();

    public static final Item ITEM = registerItem();

    private static Item registerItem() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:tannerkivi"));
        var properties = new Item.Properties() {
            @Override public Identifier effectiveModel() { return Identifier.parse("lbc:tannerkivi"); }
        }.setId(key).overrideDescription(ITEM_ID)
                .component(DataComponents.ITEM_MODEL, Identifier.parse("lbc:tannerkivi"))
                .component(DataComponents.TOOLTIP_STYLE, Identifier.parse("lbc:tier_c"))
                .component(DataComponents.CUSTOM_DATA, CustomData.of(ITEM_DATA));
        return Registry.register(BuiltInRegistries.ITEM, key,
                new SimplePolymerItem(properties, Items.REPEATING_COMMAND_BLOCK, true));
    }

    private static CompoundTag itemData() {
        var data = new CompoundTag();
        data.putInt("craftusable", 1);
        data.putInt("transformable", 1);
        data.putInt("arcanum", 1);
        data.putInt("arcanumt", 1);
        data.putInt(ITEM_ID, 1);
        return data;
    }

    private static CompoundTag tannerFlag() {
        var data = new CompoundTag();
        data.putInt(ITEM_ID, 1);
        return data;
    }

    public static void register() { }

    private final GameController game;
    private final Map<UUID, Aura> auras = new HashMap<>();
    private final Map<Long, Set<Aura>> aurasByChunk = new HashMap<>();
    private final ArrayDeque<Aura> work = new ArrayDeque<>();
    private final Set<Aura> scheduled = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final ArrayList<Aura> auraBatch = new ArrayList<>();
    private boolean stopped;

    LbcTanner(GameController game) { this.game = game; }

    public static boolean isTanner(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(ITEM)) return true;
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.matchedBy(TANNER_FLAG);
    }

    /** Автоматически обновляет обе руки; удержание предмета не требует клика. */
    public void tick() {
        if (stopped) return;
        var present = new HashSet<UUID>();
        for (var player : game.server().getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            present.add(id);
            boolean main = isTanner(player.getMainHandItem());
            boolean off = isTanner(player.getOffhandItem());
            Aura aura = auras.get(id);

            if (aura != null && aura.level != player.level()) {
                remove(aura);
                aura = null;
            }

            if (!player.isAlive() || !game.items().mana().visible(player)) {
                if (aura != null) remove(aura);
                continue;
            }
            if (!main && !off && aura == null) continue;
            if (aura == null) {
                aura = new Aura(player, (ServerLevel) player.level());
                auras.put(id, aura);
            }
            aura.owner = player;
            aura.level = (ServerLevel) player.level();
            if (!main) aura.mainActive = false;
            if (!off) aura.offActive = false;
            aura.paused = game.items().magic().suppressed(player) || game.items().wands().timeStopped()
                    || player.entityTags().contains("time_stopped") || game.items().frost().isFrozen(player);
            if (!main && !off) {
                detach(aura);
                if (!hasWork(aura)) remove(aura);
                else schedule(aura);
                continue;
            }

            // Магическое подавление, остановка времени и заморозка ставят ауру на паузу,
            // сохраняя флаги активных рук до возобновления или смены предмета.
            if (aura.paused) continue;

            int mainResult = main ? updateHand(player, aura.mainActive) : 0;
            int offResult = off ? updateHand(player, aura.offActive) : 0;
            aura.mainActive = (mainResult & 2) != 0;
            aura.offActive = (offResult & 2) != 0;
            int emittingHands = ((mainResult & 1) != 0 ? 1 : 0) | ((offResult & 1) != 0 ? 2 : 0);
            if (emittingHands != 0) {
                boolean dual = emittingHands == 3;
                updateBounds(aura, dual);
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, dual ? 40 : 20, 0), player);
                player.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.DIRT.defaultBlockState()),
                        player.getX(),player.getY()+1,player.getZ(),dual?20:10,.5,1,.5,0);
                transmuteProjectiles(aura, dual ? 20 : 6);
            }
            if (!aura.anyActive()) detach(aura);
            if (!aura.anyActive() && !hasWork(aura)) remove(aura);
            schedule(aura);
        }

        auraBatch.addAll(auras.values());
        for (Aura aura : auraBatch) if (!present.contains(aura.owner.getUUID())) remove(aura);
        auraBatch.clear();
        drainWork();
    }

    private int updateHand(ServerPlayer player, boolean active) {
        if (active) return game.items().mana().trySpend(player, UPKEEP) ? 3 : 0;
        if (!game.items().mana().trySpend(player, ACTIVATION)) return 0;
        activationFeedback(player);
        return game.items().mana().trySpend(player, UPKEEP) ? 3 : 1;
    }

    private static void activationFeedback(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.STONE_BREAK, SoundSource.MASTER, 1.0f, 0.0f);
        player.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                player.getX(), player.getY() + 1, player.getZ(), 40, 1, 1, 1, 0);
    }

    private void updateBounds(Aura aura, boolean dual) {
        BlockPos center = aura.owner.blockPosition().immutable();
        BlockAreaWork.Bounds next = dual
                ? new BlockAreaWork.Bounds(center.getX() - 15, center.getY() - 15, center.getZ() - 15,
                        center.getX() + 15, center.getY() + 15, center.getZ() + 15)
                : new BlockAreaWork.Bounds(center.getX() - 3, center.getY() - 3, center.getZ() - 3,
                        center.getX() + 3, center.getY() + 4, center.getZ() + 3);
        aura.dual = dual;
        // A paid pulse can still be draining when a new activation occurs at the same position.
        // Reattach its event index and rescan edits made while the hands were inactive.
        boolean detached = aura.chunkKeys.isEmpty();
        if (next.equals(aura.bounds) && !detached) return;
        aura.bounds = next;
        reindex(aura, next);
        aura.terrain.move(next, detached);
        schedule(aura);
    }

    private void reindex(Aura aura, BlockAreaWork.Bounds bounds) {
        for (long key : aura.chunkKeys) {
            var indexed = aurasByChunk.get(key);
            if (indexed == null) continue;
            indexed.remove(aura);
            if (indexed.isEmpty()) aurasByChunk.remove(key);
        }
        aura.chunkKeys.clear();
        for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) {
            for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
                long key = ChunkPos.pack(x, z);
                aurasByChunk.computeIfAbsent(key, ignored -> java.util.Collections.newSetFromMap(new IdentityHashMap<>()))
                        .add(aura);
                aura.chunkKeys.add(key);
            }
        }
    }

    private void transmuteProjectiles(Aura aura, int radius) {
        var owner = aura.owner;
        var level = aura.level;
        var box = owner.getBoundingBox().inflate(radius);
        double radiusSquared = radius * (double) radius;
        for (Entity entity : level.getEntities(owner, box, candidate ->
                !candidate.isRemoved() && (candidate instanceof Projectile
                        || candidate.getType().builtInRegistryHolder().is(PROJECTILES))
                        && !excluded(candidate) && isPhysicalSourceEntity(candidate))) {
            if (entity.position().distanceToSqr(owner.position()) > radiusSquared) continue;
            if (entity.position().distanceToSqr(owner.position()) <= 36) grantSourceAdvancement(owner);
            transmuteProjectile(entity, owner, level);
        }
    }

    private void grantSourceAdvancement(ServerPlayer player) {
        var advancement = game.server().getAdvancements().get(
                Identifier.parse("lbc:true_advancements/arcanums/tannerkivi"));
        if (advancement != null) player.getAdvancements().award(advancement, "requirement");
    }

    private static boolean isPhysicalSourceEntity(Entity entity) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        return !id.equals("marker") && !id.equals("item_display") && !id.equals("block_display")
                && !id.equals("interaction");
    }

    private static boolean excluded(Entity entity) {
        for (String tag : EXCLUDED_PROJECTILE_TAGS) if (entity.entityTags().contains(tag)) return true;
        return false;
    }

    private void transmuteProjectile(Entity entity, ServerPlayer owner, ServerLevel level) {
        if (entity.isRemoved() || entity.level() != level) return;
        BlockPos pos = entity.blockPosition().immutable();
        if (!level.hasChunkAt(pos)) return;
        Runnable writeDirt = () -> level.setBlock(pos, Blocks.DIRT.defaultBlockState(), BLOCK_FLAGS);
        if (owner.isAlive() && !game.isObserver(owner)) game.items().indirectKills().trapChanges(owner, writeDirt);
        else game.items().indirectKills().worldChanges(level, entity, writeDirt);
        level.playSound(null, pos, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 20, .25, .25, .25, 1);
        entity.discard();
    }

    /** ArenaBlockMixin сообщает готовое новое состояние; здесь только ставим подходящую клетку в очередь. */
    public void blockChanged(BlockPos pos, BlockState state) {
        if (stopped || !convertible(state)) return;
        Set<Aura> candidates = aurasByChunk.get(ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4));
        if (candidates == null) return;
        for (Aura aura : List.copyOf(candidates)) {
            if (aura.anyActive() && aura.level == game.maps().level() && aura.terrainBounds() != null && aura.terrainBounds().contains(pos)) {
                aura.terrain.add(pos);
                schedule(aura);
            }
        }
    }

    /** Повторно ставим в очередь только часть загруженного чанка, попавшую в текущую область. */
    public void chunkLoaded(LevelChunk chunk) {
        if (stopped) return;
        long key = ChunkPos.pack(chunk.getPos().x(), chunk.getPos().z());
        Set<Aura> candidates = aurasByChunk.get(key);
        if (candidates == null) return;
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        int maxX = minX + 15, maxZ = minZ + 15;
        for (Aura aura : List.copyOf(candidates)) {
            if (!aura.anyActive() || aura.bounds == null || aura.level != game.maps().level()) continue;
            BlockAreaWork.Bounds bounds = aura.terrainBounds().intersection(minX, aura.level.getMinY(), minZ,
                    maxX, aura.level.getMaxY(), maxZ);
            if (bounds != null) {
                aura.terrain.add(bounds);
                schedule(aura);
            }
        }
    }

    private static boolean convertible(BlockState state) {
        return !state.isAir() && !state.is(Blocks.LIGHT) && !state.is(Blocks.STRUCTURE_VOID)
                && !state.is(Blocks.DIRT);
    }

    private void drainWork() {
        long deadline = System.nanoTime() + WORK_NANOS;
        int budget = WORK_BUDGET;
        while (budget > 0 && !work.isEmpty() && System.nanoTime() < deadline) {
            Aura aura = work.removeFirst();
            scheduled.remove(aura);
            if (auras.get(aura.owner.getUUID()) != aura || aura.bounds == null || aura.paused) continue;
            if (aura.terrain.hasWork()) {
                int inspected = aura.terrain.step(game, aura.owner, Math.min(BATCH_SIZE, budget), deadline);
                budget -= Math.max(1, inspected);
            }
            if (!aura.anyActive() && !hasWork(aura)) remove(aura);
            schedule(aura);
        }
    }

    private void schedule(Aura aura) {
        if (auras.get(aura.owner.getUUID()) == aura && !aura.paused && hasWork(aura) && scheduled.add(aura))
            work.addLast(aura);
    }

    private static boolean hasWork(Aura aura) { return aura.terrain.hasWork(); }

    private void detach(Aura aura) {
        for (long key : aura.chunkKeys) {
            var indexed = aurasByChunk.get(key);
            if (indexed != null) {
                indexed.remove(aura);
                if (indexed.isEmpty()) aurasByChunk.remove(key);
            }
        }
        aura.chunkKeys.clear();
    }

    private void remove(Aura aura) {
        auras.remove(aura.owner.getUUID(), aura);
        detach(aura);
        aura.terrain.clear();
        scheduled.remove(aura);
    }

    public int count() { return (int) auras.values().stream().filter(Aura::anyActive).count(); }
    public int pendingCount() { return (int) auras.values().stream().filter(aura -> !aura.anyActive() && hasWork(aura)).count(); }

    int queuedBlockCount() {
        int count=0;
        for(var aura:auras.values()) count += aura.terrain.queuedBlockCount();
        return count;
    }

    public void stop() {
        stopped = true;
        auras.clear();
        aurasByChunk.clear();
        work.clear();
        scheduled.clear();
        auraBatch.clear();
        stopped = false;
    }

    private static final class Aura {
        ServerPlayer owner;
        ServerLevel level;
        boolean mainActive, offActive, dual, paused;
        BlockAreaWork.Bounds bounds;
        final Set<Long> chunkKeys = new HashSet<>();
        final BlockAreaWork terrain;
        Aura(ServerPlayer owner, ServerLevel level) {
            this.owner = owner; this.level = level;
            this.terrain = new BlockAreaWork(level, LbcTanner::convertible,
                    (pos, state) -> Blocks.DIRT.defaultBlockState(), BLOCK_FLAGS);
        }
        BlockAreaWork.Bounds terrainBounds() { return bounds; }
        boolean anyActive() { return mainActive || offActive; }
    }
}
