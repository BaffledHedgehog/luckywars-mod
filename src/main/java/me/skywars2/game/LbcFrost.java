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
        LivingEntity entity;
        boolean retainedOffline;
        int cold, cooldown, frozen;
        LbcIndirectKills.Owner source;
        BlockPos anchor;
        List<BlockPos> cells = List.of();
        // Неизменяемый снимок оболочки переиспользуется при сохранении секундных таймеров.
        ListTag savedIce = new ListTag();
        State(LivingEntity entity) { this.entity = entity; }
    }
    private static final class Ice {
        CompoundTag original;
        final Set<UUID> owners = new HashSet<>();
        Ice(CompoundTag original) { this.original = original; }
    }
    private final GameController game;
    private String epoch = UUID.randomUUID().toString();
    private final Map<LivingEntity, State> affected = new LinkedHashMap<>();
    private final Set<State> frozen = new java.util.LinkedHashSet<>();
    private final Map<BlockPos, Ice> ice = new HashMap<>();
    private final ArrayList<State> batch = new ArrayList<>();
    private int statusCallbacks,secondOffset,restoredPhase;
    private boolean statusRestoring;

    LbcFrost(GameController game) { this.game = game; }
    static void register() { }

    private boolean eligible(LivingEntity entity) {
        if(game.items().polymorph().masked(entity))return false;
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
    public void add(LivingEntity entity, ServerPlayer source) { add(entity,source,false); }
    private void add(LivingEntity entity,ServerPlayer source,boolean projectile) {
        statusCallbacks++;try {if(statusRestoring)return;
            if (!eligible(entity) || source != null && !game.canUseGameplayItems(source)) return;
            State state = affected.computeIfAbsent(entity, State::new);
            var slow = entity.getEffect(MobEffects.SLOWNESS);
            int multiplier = state.cold > 0 || slow == null ? 1 : slow.getAmplifier() + 1;
            state.source=source==null?null:game.items().indirectKills().coldCause(source,projectile);
            state.cold += 10 * multiplier;
            state.cooldown = 5;
            check(state, source);
            var pos = entity.blockPosition();
            level().sendParticles(ParticleTypes.SNOWFLAKE, pos.getX()+.5, pos.getY()+1.31, pos.getZ()+.5, 20, .5, .5, .5, .01);
            save(state);
        }finally{statusCallbacks--;}
    }

    /** Ice Wand adds cold only while thawed; another hit never refreshes an existing freeze. */
    boolean iceHit(LivingEntity entity,ServerPlayer source){
        if(!eligible(entity))return false;
        if(!isFrozen(entity))add(entity,source,true);
        return isFrozen(entity);
    }

    /** Shattering uses the same native block restoration as a normal thaw. */
    boolean shatter(LivingEntity entity){
        if(!isFrozen(entity))return false;
        clear(entity);entity.removeEffect(MobEffects.SLOWNESS);return true;
    }

    /** Source Runic Ice Shard pulse; frozen holders retain the resistance reward. */
    void runicShard(ServerPlayer player){
        statusCallbacks++;try {if(statusRestoring)return;
            if(!eligible(player))return;
            var state=affected.computeIfAbsent(player,State::new);
            if(state.frozen==0){
                var slow=player.getEffect(MobEffects.SLOWNESS);
                if(slow!=null&&slow.getAmplifier()>=1)state.cold+=5;
                if(state.cold<10)state.cold++;
                state.cooldown=2;check(state,null);
            }
            if(state.frozen>0){
                game.items().mana().award(player,"lbc:true_advancements/accesories/jaakivi");
                player.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,500,4),player);
            }
            save(state);
        }finally{statusCallbacks--;}
    }

    /** Snow-cloud source: +2 cold (initial Slowness multiplier), cooldown2, no frozen refresh. */
    boolean snow(LivingEntity entity,ServerPlayer source){
        statusCallbacks++;try {if(statusRestoring)return false;
            if(!eligible(entity)||source==null||!game.canUseGameplayItems(source))return false;
            var state=affected.computeIfAbsent(entity,State::new);
            if(state.frozen>0)return true;
            var slow=entity.getEffect(MobEffects.SLOWNESS);
            int multiplier=state.cold>0||slow==null?1:slow.getAmplifier()+1;
            state.source=game.items().indirectKills().coldCause(source,true);state.cold+=2*multiplier;state.cooldown=2;
            save(state);return true;
        }finally{statusCallbacks--;}
    }

    /** Existing frost counters retain their native stored units (seconds for both timers). */
    void addPlayerCounters(ServerPlayer player) {
        var state=affected.get(player);
        if(state==null || !eligible(player)) return;
        if(state.cold>0) state.cold++;
        if(state.cooldown>0) state.cooldown++;
        if(state.frozen>0) state.frozen++;
        save(state);
    }

    void adoptRawFoodStatuses(ServerPlayer player){
        statusCallbacks++;try {if(statusRestoring)return;
            if(!eligible(player))return;var mana=game.items().mana();
            int cold=mana.hudValue(player,"cold"),cooldown=mana.hudValue(player,"cold_cd"),duration=mana.hudValue(player,"frozen");
            if(cold<=0&&duration<=0)return; // A bare cold_cd is dormant until source cold exists.
            var state=affected.computeIfAbsent(player,State::new);
            state.cold=cold;state.cooldown=cooldown;state.source=game.items().indirectKills().wildCause(player);
            if(duration>0){
                if(state.anchor==null){state.anchor=player.blockPosition();captureIce(state);}
                state.frozen=duration;frozen.add(state);hold(state);
            }
            save(state);
        }finally{statusCallbacks--;}
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
            if(state.source!=null)game.items().indirectKills().trapChanges(level(),state.source,()->hold(state));else hold(state);
        } else if (state.cold > 0) {
            int tier = (state.cold-1)/10;
            effect(entity, MobEffects.SLOWNESS, 40, tier,state.source);
            if (tier >= 1) effect(entity, MobEffects.MINING_FATIGUE, 40, tier-1,state.source);
            if (tier >= 2) effect(entity, MobEffects.WEAKNESS, 40, tier-2,state.source);
            if (tier >= 3) effect(entity, MobEffects.UNLUCK, 40, tier >= 8 ? 2 : tier >= 6 ? 1 : 0,state.source);
            if (tier >= 5) effect(entity, MobEffects.BLINDNESS, 40, 0,state.source);
        }
    }

    private void effect(LivingEntity entity, Holder<MobEffect> effect, int ticks, int amplifier) {
        effect(entity,effect,ticks,amplifier,null);
    }
    private void effect(LivingEntity entity,Holder<MobEffect> effect,int ticks,int amplifier,LbcIndirectKills.Owner source){
        game.items().indirectKills().coldEffect(entity,ItemEffectLevels.create(effect,ticks,amplifier,false,false,false),source);
    }

    void tick() {
        statusCallbacks++;try {
            if(statusRestoring)return;
            // Cold без frozen требует только секундной обработки; инвентарь здесь не читается.
            batch.addAll(frozen);
            for (State state : batch) {
                var entity = state.entity;
                if(state.retainedOffline)continue;
                if(game.items().polymorph().masked(entity))continue;
                if (!eligible(entity)) { clear(entity); continue; }
                hold(state);
                effect(entity, MobEffects.MINING_FATIGUE, 20, 127);
                effect(entity, MobEffects.WEAKNESS, 20, 127);
                if (entity instanceof ServerPlayer) effect(entity, MobEffects.RESISTANCE, 20, 1);
                level().sendParticles(ParticleTypes.SNOWFLAKE, entity.getX(), entity.getY()+.81, entity.getZ(), 1, 1, 1, 1, 0);
                if (state.cold != 0) { state.cold = 0; save(state); }
            }
            batch.clear();
            if (Math.floorMod(game.server().getTickCount()+secondOffset,20) == 0) second();
        }finally{statusCallbacks--;}
    }

    /** Общая секундная точка входа пригодится и предметам, ускоряющим исходный tick1s. */
    void second() {
        statusCallbacks++;try {
            if(statusRestoring)return;
            batch.addAll(affected.values());
            for (State state : batch) {
                if(state.retainedOffline)continue;
                if(game.items().polymorph().masked(state.entity))continue;
                if (!eligible(state.entity)) { clear(state.entity); continue; }
                if (state.cold > 0) {
                    if (state.frozen == 0) check(state,null);
                    if (state.cooldown > 0) state.cooldown--;
                    if (state.cooldown == 0) state.cold = Math.max(0, state.cold-1);
                }
                if (state.frozen > 0 && --state.frozen == 0) thaw(state, true);
                if (state.cold == 0 && state.frozen == 0) clear(state.entity);
                else save(state);
            }
            batch.clear();
        }finally{statusCallbacks--;}
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
        var mana=game.items().mana();mana.score(state.entity,"cold").set(state.cold);
        mana.score(state.entity,"cold_cd").set(state.cooldown);mana.score(state.entity,"frozen").set(state.frozen);
        var tag = new CompoundTag();
        tag.putString("epoch", epoch); tag.putInt("cold", state.cold); tag.putInt("cooldown", state.cooldown); tag.putInt("frozen", state.frozen);
        if(state.source!=null){tag.putString("source",state.source.player().toString());tag.putLong("source_order",state.source.order());tag.putBoolean("source_projectile",state.source.projectile());}
        if (state.anchor != null) {
            tag.putLong("anchor", state.anchor.asLong()); tag.put("ice", state.savedIce);
        }
        state.entity.setAttached(DATA, tag);
    }

    void loaded(Entity entity) {
        statusCallbacks++;try {
            if(statusRestoring)return;
            if (!(entity instanceof LivingEntity living)) return;
            var tag = living.getAttached(DATA);
            if (tag == null) return;
            if (!epoch.equals(tag.getStringOr("epoch", ""))) { living.removeAttached(DATA); return; }
            if (!eligible(living)) { clear(living); return; }
            var state = new State(living);
            try{state.source=game.items().indirectKills().restoreColdCause(UUID.fromString(tag.getStringOr("source","")),tag.getLongOr("source_order",0),tag.getBooleanOr("source_projectile",false));}catch(IllegalArgumentException ignored){}
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
        }finally{statusCallbacks--;}
    }

    void unloaded(Entity entity) {
        statusCallbacks++;try {
            if(statusRestoring)return;
            if (!(entity instanceof LivingEntity living)) return;
            var state = affected.remove(living);
            if (state != null) frozen.remove(state);
            // Attachment и принадлежность льда переживают unload; UUID не удерживает ссылку на сущность.
        }finally{statusCallbacks--;}
    }

    public void clear(LivingEntity entity) {
        statusCallbacks++;try {
            if(statusRestoring)return;
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
            var mana=game.items().mana();for(String name:List.of("cold","cold_cd","frozen"))mana.score(entity,name).set(0);
        }finally{statusCallbacks--;}
    }

    void stop() {
        statusCallbacks++;try {
            for (var state : List.copyOf(affected.values())) clear(state.entity);
            for (var entry : List.copyOf(ice.entrySet())) restore(entry.getKey(), entry.getValue().original);
            ice.clear(); frozen.clear(); affected.clear(); batch.clear();
            epoch = UUID.randomUUID().toString();statusRestoring=false;secondOffset=0;
        }finally{statusCallbacks--;}
    }

    void checkpointBoundary(){if(statusCallbacks!=0)throw new IllegalStateException("Finish Frost status callbacks before checkpointing");}
    /** Native bodies/effects and terrain are restored by their owners; never thaw or restore blocks here. */
    void prepareFrozen(){checkpointBoundary();statusRestoring=true;affected.clear();frozen.clear();ice.clear();batch.clear();}
    void publishFrozen(){checkpointBoundary();secondOffset=Math.floorMod(restoredPhase-game.server().getTickCount(),20);statusRestoring=false;}
    void cancelFrozen(){checkpointBoundary();statusRestoring=false;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();return new Checkpoint(links);}
    final class Checkpoint {
        private record StateImage(State original,MatchDamageSources.Ref entity,int cold,int cooldown,int frozen,LbcIndirectKills.Owner cause,
                BlockPos anchor,List<BlockPos> cells,ListTag saved) { }
        private record IceImage(Ice original,CompoundTag block,Set<UUID> owners) { }
        private final List<StateImage> states;
        private final List<State> frozenOrder=List.copyOf(frozen);
        private final Map<BlockPos,IceImage> blocks=new HashMap<>();
        private final String savedEpoch=epoch;
        private final int phase=Math.floorMod(game.server().getTickCount()+secondOffset,20);
        Checkpoint(MatchSpellCheckpoint.Links links){
            states=affected.values().stream().map(state->new StateImage(state,links.key(state.entity),state.cold,state.cooldown,state.frozen,state.source,
                    state.anchor==null?null:state.anchor.immutable(),List.copyOf(state.cells),state.savedIce.copy())).toList();
            ice.forEach((pos,block)->blocks.put(pos.immutable(),new IceImage(block,block.original.copy(),Set.copyOf(block.owners))));
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var plans=new ArrayList<Runnable>();
            for(var image:states){var entity=(LivingEntity)links.get(image.entity);boolean offline=MatchOfflinePlayers.retained(entity);var saved=image.saved.copy();
                plans.add(()->{var state=image.original;state.entity=entity;state.retainedOffline=offline;state.cold=image.cold;state.cooldown=image.cooldown;
                    state.frozen=image.frozen;state.source=image.cause;state.anchor=image.anchor;state.cells=image.cells;state.savedIce=saved;affected.put(entity,state);});}
            var nextIce=new HashMap<BlockPos,Ice>();
            blocks.forEach((pos,image)->{var baseline=image.block.copy();plans.add(()->{var block=image.original;block.original=baseline;block.owners.clear();block.owners.addAll(image.owners);});nextIce.put(pos,image.original);});
            return ()->{statusRestoring=true;affected.clear();frozen.clear();ice.clear();batch.clear();plans.forEach(Runnable::run);frozen.addAll(frozenOrder);ice.putAll(nextIce);
                epoch=savedEpoch;restoredPhase=phase;secondOffset=Math.floorMod(phase-game.server().getTickCount(),20);};
        }
    }

    public int cold(LivingEntity entity) { var state = affected.get(entity); return state == null ? 0 : state.cold; }
    public int cooldown(LivingEntity entity) { var state = affected.get(entity); return state == null ? 0 : state.cooldown; }
    public int frozenSeconds(LivingEntity entity) { var state = affected.get(entity); return state == null ? 0 : state.frozen; }
    void suspendForm(LivingEntity entity){
        statusCallbacks++;try {if(statusRestoring)return;    var state=affected.get(entity);if(state!=null&&state.anchor!=null)thaw(state,false);
        }finally{statusCallbacks--;}
    }
    void resumeForm(LivingEntity entity){
        statusCallbacks++;try {if(statusRestoring)return;    var state=affected.get(entity);if(state!=null&&state.frozen>0&&entity.level()==level()){
            state.anchor=entity.blockPosition();captureIce(state);frozen.add(state);hold(state);save(state);
        }
        }finally{statusCallbacks--;}
    }
    public boolean isFrozen(LivingEntity entity) { return !game.items().polymorph().masked(entity)&&frozenSeconds(entity) > 0; }
    public int trackedCount() { return affected.size(); }
    public int iceBlockCount() { return ice.size(); }
    private ServerLevel level() { return game.maps().level(); }
}
