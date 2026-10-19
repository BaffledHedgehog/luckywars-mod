package me.skywars2.game;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Общий cold/frozen для ледяных предметов: таймеры обходят только затронутые сущности. */
public final class LbcFrost {
    private static final AttachmentType<CompoundTag> DATA = AttachmentRegistry.create(
            Identifier.parse("skywars2:frost"), builder -> builder.persistent(CompoundTag.CODEC));
    // Как source fill strict: без соседних обновлений; контейнеры сохраняются без выпадения содержимого.
    private static final int ICE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
            | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS | Block.UPDATE_SKIP_ON_PLACE;
    private static final class State {
        final LivingEntity entity;
        int cold, cooldown, frozen;
        BlockPos anchor;
        List<BlockPos> cells = List.of();
        // Неизменяемый снимок оболочки переиспользуется при сохранении секундных таймеров.
        ListTag savedIce = new ListTag();
        State(LivingEntity entity) { this.entity = entity; }
    }
    private static final class Ice {
        final CompoundTag original;
        final Set<UUID> owners = new HashSet<>();
        Ice(CompoundTag original) { this.original = original; }
    }
    private final GameController game;
    private String epoch = UUID.randomUUID().toString();
    private final Map<LivingEntity, State> affected = new LinkedHashMap<>();
    private final Set<State> frozen = new java.util.LinkedHashSet<>();
    private final Map<BlockPos, Ice> ice = new HashMap<>();
    private final ArrayList<State> batch = new ArrayList<>();

    LbcFrost(GameController game) { this.game = game; }
    static void register() { }

    private boolean eligible(LivingEntity entity) {
        return (game.playing() || game.items().debugItemUseWindow())
                && entity.level() == game.maps().level() && entity.isAlive() && !entity.isRemoved()
                && !entity.entityTags().contains("spectator")
                && (!(entity instanceof ServerPlayer player) || game.allowDamage(player)
                        && !game.items().magic().rejectCustomEffect(player));
    }

    /** Общая исходная область Ice Sword / Snow Pearl: все недавно раненые чужие цели вокруг владельца. */
    void hitTargets(ServerPlayer owner) {
        if (!game.canUseGameplayItems(owner) || owner.level() != level()) return;
        for (var target : level().getEntitiesOfClass(LivingEntity.class,
                new AABB(owner.position(), owner.position()).inflate(7), entity ->
                        eligible(entity) && entity.hurtTime == 10 && !owner.isAlliedTo(entity))) {
            double distance = owner.distanceToSqr(target);
            if (distance >= .000001 && distance <= 49) add(target, owner);
        }
        level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.SNOW_FALL, SoundSource.MASTER, 1, 0);
    }

    /** Первый приём cold учитывает прежний Slowness; повторные добавляют базовые десять. */
    public void add(LivingEntity entity, ServerPlayer source) {
        if (!eligible(entity) || source != null && !game.canUseGameplayItems(source)) return;
        State state = affected.computeIfAbsent(entity, State::new);
        var slow = entity.getEffect(MobEffects.SLOWNESS);
        int multiplier = state.cold > 0 || slow == null ? 1 : slow.getAmplifier() + 1;
        state.cold += 10 * multiplier;
        state.cooldown = 5;
        check(state, source);
        var pos = entity.blockPosition();
        level().sendParticles(ParticleTypes.SNOWFLAKE, pos.getX()+.5, pos.getY()+1.31, pos.getZ()+.5, 20, .5, .5, .5, .01);
        save(state);
    }

    private void check(State state, ServerPlayer source) {
        var entity = state.entity;
        level().sendParticles(ParticleTypes.SNOWFLAKE, entity.getX(), entity.getY()+.81, entity.getZ(), state.cold/2, .3, .4, .3, 0);
        if (state.cold >= 100) {
            if (state.anchor == null) {
                state.anchor = entity.blockPosition();
                captureIce(state);
            }
            state.cold = state.cooldown = 0;
            state.frozen = 10;
            frozen.add(state);
            level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.MASTER, 1, 0);
            if (source != null && entity instanceof ServerPlayer && entity.hurtTime == 10 && source.distanceToSqr(entity) <= 49) {
                var advancement = game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/swords/ice"));
                if (advancement != null) for (String criterion : advancement.value().criteria().keySet()) source.getAdvancements().award(advancement, criterion);
            }
            hold(state);
        } else if (state.cold > 0) {
            int tier = (state.cold-1)/10;
            effect(entity, MobEffects.SLOWNESS, 40, tier);
            if (tier >= 1) effect(entity, MobEffects.MINING_FATIGUE, 40, tier-1);
            if (tier >= 2) effect(entity, MobEffects.WEAKNESS, 40, tier-2);
            if (tier >= 3) effect(entity, MobEffects.UNLUCK, 40, tier >= 8 ? 2 : tier >= 6 ? 1 : 0);
            if (tier >= 5) effect(entity, MobEffects.BLINDNESS, 40, 0);
        }
    }

    private static void effect(LivingEntity entity, Holder<MobEffect> effect, int ticks, int amplifier) {
        entity.addEffect(new MobEffectInstance(effect, ticks, amplifier, false, false, false));
    }

    void tick() {
        // Cold без frozen требует только секундной обработки; инвентарь здесь не читается.
        batch.addAll(frozen);
        for (State state : batch) {
            var entity = state.entity;
            if (!eligible(entity)) { clear(entity); continue; }
            hold(state);
            effect(entity, MobEffects.MINING_FATIGUE, 20, 127);
            effect(entity, MobEffects.WEAKNESS, 20, 127);
            if (entity instanceof ServerPlayer) effect(entity, MobEffects.RESISTANCE, 20, 1);
            level().sendParticles(ParticleTypes.SNOWFLAKE, entity.getX(), entity.getY()+.81, entity.getZ(), 1, 1, 1, 1, 0);
            if (state.cold != 0) { state.cold = 0; save(state); }
        }
        batch.clear();
        if (game.server().getTickCount()%20 == 0) second();
    }

    /** Общая секундная точка входа пригодится и предметам, ускоряющим исходный tick1s. */
    void second() {
        batch.addAll(affected.values());
        for (State state : batch) {
            if (!eligible(state.entity)) { clear(state.entity); continue; }
            if (state.cold > 0) {
                if (state.frozen == 0) check(state, null);
                if (state.cooldown > 0) state.cooldown--;
                if (state.cooldown == 0) state.cold = Math.max(0, state.cold-1);
            }
            if (state.frozen > 0 && --state.frozen == 0) thaw(state, true);
            if (state.cold == 0 && state.frozen == 0) clear(state.entity);
            else save(state);
        }
        batch.clear();
    }

    private CompoundTag acquire(BlockPos pos, UUID owner) {
        Ice block = ice.computeIfAbsent(pos, key -> new Ice(snapshot(key)));
        block.owners.add(owner);
        return block.original;
    }

    /** Покрываем реальный хитбокс после центрирования; верхние грани не захватывают соседний блок. */
    private void captureIce(State state) {
        var entity = state.entity;
        var anchor = state.anchor;
        var bounds = entity.getBoundingBox().move(anchor.getX()+.5-entity.getX(),
                anchor.getY()-entity.getY(), anchor.getZ()+.5-entity.getZ());
        var first = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        var last = BlockPos.containing(Math.max(bounds.minX, Math.nextDown(bounds.maxX)),
                Math.max(bounds.minY, Math.nextDown(bounds.maxY)), Math.max(bounds.minZ, Math.nextDown(bounds.maxZ)));
        var cells = new ArrayList<BlockPos>();
        for (var cursor : BlockPos.betweenClosed(first, last)) {
            var pos = cursor.immutable();
            var entry = new CompoundTag();
            entry.putLong("pos", pos.asLong());
            entry.put("original", acquire(pos, entity.getUUID()));
            state.savedIce.add(entry);
            cells.add(pos);
        }
        state.cells = List.copyOf(cells);
    }

    private void hold(State state) {
        var entity = state.entity;
        var anchor = state.anchor;
        double x = anchor.getX()+.5, y = anchor.getY(), z = anchor.getZ()+.5;
        if (entity.getX() != x || entity.getY() != y || entity.getZ() != z) entity.teleportTo(x, y, z);
        // Прерываем еду/лук/щит, начатые до заморозки, не закрывая инвентарь.
        if (entity.isUsingItem()) entity.stopUsingItem();
        for (var pos : state.cells) ensureIce(pos);
    }

    private CompoundTag snapshot(BlockPos pos) {
        var tag = new CompoundTag();
        tag.put("state", NbtUtils.writeBlockState(level().getBlockState(pos)));
        var blockEntity = level().getBlockEntity(pos);
        if (blockEntity != null) tag.put("entity", blockEntity.saveWithFullMetadata(level().registryAccess()));
        return tag;
    }

    private void ensureIce(BlockPos pos) {
        if (!level().getBlockState(pos).is(Blocks.ICE)) level().setBlock(pos, Blocks.ICE.defaultBlockState(), ICE_FLAGS);
    }

    private void release(BlockPos pos, UUID owner) {
        Ice block = ice.get(pos);
        if (block == null || !block.owners.remove(owner) || !block.owners.isEmpty()) return;
        ice.remove(pos);
        restore(pos, block.original);
    }

    private void restore(BlockPos pos, CompoundTag tag) {
        var original = NbtUtils.readBlockState(level().registryAccess().lookupOrThrow(Registries.BLOCK), tag.getCompoundOrEmpty("state"));
        level().setBlock(pos, original, ICE_FLAGS);
        var blockEntity = level().getBlockEntity(pos);
        if (blockEntity != null && tag.getCompound("entity").isPresent()) {
            blockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level().registryAccess(), tag.getCompoundOrEmpty("entity")));
            blockEntity.setChanged();
            level().sendBlockUpdated(pos, original, original, Block.UPDATE_CLIENTS);
        }
    }

    private void thaw(State state, boolean particles) {
        if (state.anchor == null) return;
        for (var pos : state.cells) release(pos, state.entity.getUUID());
        if (particles) {
            level().sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.ICE),
                    state.entity.getX(), state.entity.getY()+1, state.entity.getZ(), 100, 0, 0, 0, .3);
            level().sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.SNOW),
                    state.entity.getX(), state.entity.getY()+1, state.entity.getZ(), 30, 0, 0, 0, .8);
        }
        frozen.remove(state); state.anchor = null; state.cells = List.of(); state.savedIce = new ListTag();
    }

    private void save(State state) {
        var tag = new CompoundTag();
        tag.putString("epoch", epoch); tag.putInt("cold", state.cold); tag.putInt("cooldown", state.cooldown); tag.putInt("frozen", state.frozen);
        if (state.anchor != null) {
            tag.putLong("anchor", state.anchor.asLong()); tag.put("ice", state.savedIce);
        }
        state.entity.setAttached(DATA, tag);
    }

    void loaded(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;
        var tag = living.getAttached(DATA);
        if (tag == null) return;
        if (!epoch.equals(tag.getStringOr("epoch", ""))) { living.removeAttached(DATA); return; }
        if (!eligible(living)) { clear(living); return; }
        var state = new State(living);
        state.cold = tag.getIntOr("cold", 0); state.cooldown = tag.getIntOr("cooldown", 0); state.frozen = tag.getIntOr("frozen", 0);
        if (state.frozen > 0) {
            state.anchor = BlockPos.of(tag.getLongOr("anchor", 0));
            state.savedIce = tag.getListOrEmpty("ice");
            var cells = new ArrayList<BlockPos>();
            for (var entry : state.savedIce) if (entry instanceof CompoundTag block) {
                var pos = BlockPos.of(block.getLongOr("pos", 0));
                ice.computeIfAbsent(pos, key -> new Ice(block.getCompoundOrEmpty("original"))).owners.add(living.getUUID());
                cells.add(pos);
            }
            state.cells = List.copyOf(cells);
            frozen.add(state);
        }
        affected.put(living, state);
    }

    void unloaded(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;
        var state = affected.remove(living);
        if (state != null) frozen.remove(state);
        // Attachment и принадлежность льда переживают unload; UUID не удерживает ссылку на сущность.
    }

    public void clear(LivingEntity entity) {
        var state = affected.remove(entity);
        if (state != null) thaw(state, false);
        else {
            var tag = entity.getAttached(DATA);
            if (tag != null && epoch.equals(tag.getStringOr("epoch", "")) && tag.getIntOr("frozen", 0) > 0) {
                for (var entry : tag.getListOrEmpty("ice")) if (entry instanceof CompoundTag block)
                    release(BlockPos.of(block.getLongOr("pos", 0)), entity.getUUID());
            }
        }
        entity.removeAttached(DATA);
    }

    void stop() {
        for (var state : List.copyOf(affected.values())) clear(state.entity);
        for (var entry : List.copyOf(ice.entrySet())) restore(entry.getKey(), entry.getValue().original);
        ice.clear(); frozen.clear(); affected.clear(); batch.clear();
        epoch = UUID.randomUUID().toString();
    }

    public int cold(LivingEntity entity) { var state = affected.get(entity); return state == null ? 0 : state.cold; }
    public int cooldown(LivingEntity entity) { var state = affected.get(entity); return state == null ? 0 : state.cooldown; }
    public int frozenSeconds(LivingEntity entity) { var state = affected.get(entity); return state == null ? 0 : state.frozen; }
    public boolean isFrozen(LivingEntity entity) { return frozenSeconds(entity) > 0; }
    public int trackedCount() { return affected.size(); }
    public int iceBlockCount() { return ice.size(); }
    private ServerLevel level() { return game.maps().level(); }
}
