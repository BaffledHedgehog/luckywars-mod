package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

/** Автор отложенного урона: храним причину, а нативный DamageSource оставляем прежним. */
public final class LbcIndirectKills {
    /** Общий fill strict для ловушек с авторством последующего удушения от их блоков. */
    void fillTrap(ServerPlayer owner, BlockPos first, BlockPos last, BlockState state) {
        var level=owner.level();
        int flags=net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE
                | net.minecraft.world.level.block.Block.UPDATE_SUPPRESS_DROPS
                | net.minecraft.world.level.block.Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS
                | net.minecraft.world.level.block.Block.UPDATE_SKIP_ON_PLACE;
        for(var pos:BlockPos.betweenClosed(first,last)) {
            if(!level.hasChunkAt(pos)) continue;
            var old=level.getBlockState(pos);
            if(old==state) continue;
            if(level.setBlock(pos,state,flags) && state.isSuffocating(level,pos)
                    && !old.isSuffocating(level,pos)) stonePlaced(level,pos,owner);
        }
    }
    private record Owner(UUID player, long order, boolean friendlyFire, boolean projectile) { }
    private record TrapFrame(Owner owner, ServerLevel level, Map<BlockPos,BlockState> candidates) { }
    private static final class Fall {
        final Owner owner;
        final boolean levitation;
        int grace = 2;
        Fall(Owner owner, boolean levitation) { this.owner=owner; this.levitation=levitation; }
    }
    private record Status(Owner owner, MobEffectInstance effect) { }
    private record Support(Owner owner, ServerLevel level) { }
    private record Hazard(Owner owner, BlockState state) { }
    private final GameController game;
    // Ссылки на выгруженных мобов не удерживают сущности/чанки в памяти.
    private final Map<LivingEntity,Fall> falls = new WeakHashMap<>();
    private final Map<LivingEntity,Map<Holder<MobEffect>,Status>> statuses = new WeakHashMap<>();
    private final Map<LivingEntity,Status> pendingMana = new WeakHashMap<>();
    private final Map<LivingEntity,Owner> manaDrains = new WeakHashMap<>();
    private final Map<LivingEntity,Owner> manaDebts = new WeakHashMap<>();
    private final Map<LivingEntity,Support> removedSupports = new WeakHashMap<>();
    private final Map<LivingEntity,Owner> laterAttacks = new WeakHashMap<>();
    private final Map<ServerLevel,Map<BlockPos,Owner>> stone = new HashMap<>();
    private final Map<ServerLevel,Map<BlockPos,Hazard>> hazards = new HashMap<>();
    private final Map<LivingEntity,Owner> burning = new WeakHashMap<>();
    private long order;
    private LivingEntity effectTarget, manaTarget;
    private Holder<MobEffect> tickingEffect;
    private Owner hazardWriter;
    private ServerLevel hazardLevel;
    private TrapFrame trapFrame;

    LbcIndirectKills(GameController game) { this.game=game; }
    private Owner owner(ServerPlayer player) { return owner(player,false); }
    private Owner owner(ServerPlayer player,boolean friendlyFire) { return new Owner(player.getUUID(),++order,friendlyFire,false); }

    /** Нативное зелье передаёт source как игрока, снаряд или облако — разворачиваем владельца. */
    public static ServerPlayer playerSource(Entity source) {
        var game=me.skywars2.SkyWars2.game();
        if(game!=null && game.items().cageDrops().owner(source) instanceof ServerPlayer player) return player;
        if(source instanceof Projectile projectile) source=projectile.getOwner();
        if(source instanceof AreaEffectCloud cloud) source=cloud.getOwner();
        if(source instanceof PrimedTnt tnt) source=tnt.getOwner();
        if(source instanceof net.minecraft.world.entity.LightningBolt bolt) source=bolt.getCause();
        if(source instanceof ServerPlayer player) return player;
        return summonedOwner(source);
    }

    /** Общая атрибуция призванных мобов; используется также до нативного смертельного удара. */
    private static ServerPlayer summonedOwner(Entity source) {
        var game=me.skywars2.SkyWars2.game();
        if(game==null) return null;
        if(game.items().creeperDrops().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().zombieTntDrops().owner(source) instanceof ServerPlayer player) return player;
        var summoner=game.items().blaze().owner(source);
        if(summoner==null) summoner=game.items().shooters().owner(source);
        if(summoner==null) summoner=game.items().mobgun().owner(source);
        if(summoner==null) summoner=game.items().kfcRewards().owner(source);
        if(summoner!=null) return summoner;
        summoner=game.items().psycho().owner(source);
        if(summoner==null) summoner=game.items().summons().owner(source);
        return summoner!=null ? summoner : game.items().consuming().owner(source);
    }

    /** Only harmful potion/cloud effects use projectile allegiance; beneficial effects retain vanilla behavior. */
    public boolean allowProjectileEffect(LivingEntity target,MobEffectInstance effect,Entity source) {
        return allowProjectileEffect(target,effect.getEffect(),source);
    }
    public boolean allowProjectileEffect(LivingEntity target,Holder<MobEffect> effect,Entity source) {
        if(effect.value().getCategory()!=net.minecraft.world.effect.MobEffectCategory.HARMFUL
                || !(source instanceof net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion)
                    && !(source instanceof AreaEffectCloud)) return true;
        if(LbcCageDrops.managed(source)) return game.items().cageDrops().canAffect(source,target);
        var player=playerSource(source);
        return player==null || player==target && game.items().wands().validTarget(target)
                && (source instanceof Projectile projectile && projectile.getOwner()==player
                    || source instanceof AreaEffectCloud cloud && cloud.getOwner()==player)
                || game.items().wands().projectileTarget(player,target);
    }
    /** A duration-only grant preserves the existing cause instead of becoming a fresh self-cast. */
    void extendEffectDuration(LivingEntity target,MobEffectInstance effect) {
        if(target.getEffect(effect.getEffect())!=effect || effect.isInfiniteDuration()
                || effect.getDuration()==Integer.MAX_VALUE) return;
        var effects=statuses.get(target);
        var status=effects==null?null:effects.get(effect.getEffect());
        var pending=pendingMana.get(target);
        var fall=effect.is(MobEffects.LEVITATION)?falls.get(target):null;
        int amplifier=effect.getAmplifier(),duration=effect.getDuration();
        boolean updated=target.addEffect(new MobEffectInstance(effect.getEffect(),duration+1,amplifier,
                effect.isAmbient(),effect.isVisible(),effect.showIcon()));
        // Native addEffect updates this exact instance and sends the usual effect packet. A cleanse
        // or replacement during its callbacks must never resurrect the removed effect's attribution.
        if(!updated || target.getEffect(effect.getEffect())!=effect
                || effect.getAmplifier()!=amplifier || effect.getDuration()!=duration+1) return;
        if(status!=null && status.effect==effect)
            statuses.computeIfAbsent(target,ignored->new HashMap<>()).put(effect.getEffect(),status);
        if(pending!=null && pending.effect==effect) pendingMana.put(target,pending);
        if(fall!=null) falls.put(target,fall);
    }

    public void effectChanged(LivingEntity target, MobEffectInstance effect, Entity source) {
        var player=playerSource(source);
        if(effect.is(MobEffects.LEVITATION)) {
            levitationChanged(target);
            if(player!=null && player!=target) levitated(target,player);
        }
        if(effect.is(MobEffects.POISON) || effect.is(MobEffects.WITHER)) {
            effectRemoved(target,effect);
            if(player!=null && player!=target) statuses.computeIfAbsent(target,ignored->new HashMap<>())
                    .put(effect.getEffect(),new Status(owner(player),effect));
        }
        if(effect.is(MobEffects.RAID_OMEN) && effect.getAmplifier()==20) {
            pendingMana.remove(target);
            if(player!=null && player!=target) pendingMana.put(target,new Status(owner(player),effect));
        }
    }
    public void effectRemoved(LivingEntity target, MobEffectInstance effect) {
        var effects=statuses.get(target);
        if(effects!=null) {
            effects.remove(effect.getEffect());
            if(effects.isEmpty()) statuses.remove(target);
        }
        if(effect.is(MobEffects.RAID_OMEN)) pendingMana.remove(target);
    }
    public void manaPotionConsumed(LivingEntity target) {
        var source=pendingMana.get(target);
        if(source!=null && source.effect==target.getEffect(MobEffects.RAID_OMEN)) manaDrains.put(target,source.owner);
        else manaDrains.remove(target);
    }
    public void manaLost(LivingEntity target) {
        var owner=manaDrains.get(target);
        if(owner!=null) manaDebts.put(target,owner);
        else manaDebts.remove(target);
    }
    public void manaEffectCleared(LivingEntity target) { manaDrains.remove(target); pendingMana.remove(target); }
    public void manaRecovered(ServerPlayer target) {
        if(game.items().mana().get(target)>=0 && game.items().mana().baseRegen(target)>=0
                && game.items().mana().maximum(target)>=0) manaDebts.remove(target);
    }
    /** Перегрузка использует тот же source/урон: контекст отличает её от настоящей пустоты и /kill. */
    public void manaDamage(ServerPlayer target, Runnable damage) {
        var previous=manaTarget; manaTarget=target;
        try { damage.run(); } finally { manaTarget=previous; }
    }

    public void levitated(LivingEntity target, ServerPlayer player) {
        if(target.hasEffect(MobEffects.LEVITATION)) falls.put(target,new Fall(owner(player),true));
    }

    /** До удаления собираем только стоящих именно на этой collision shape, не всех соседей. */
    public void supportRemoved(ServerPlayer player, BlockPos pos) {
        supportRemoved(player.level(),player,pos);
    }
    public void supportRemoved(ServerLevel level,ServerPlayer player,BlockPos pos) {
        supportRemoved(level,owner(player),pos);
    }
    /** The write scope already owns the cause, even if its author has since left. */
    private void supportRemoved(ServerLevel level,Owner owner,BlockPos pos) {
        var state=level.getBlockState(pos);
        if(state.getCollisionShape(level,pos).isEmpty()) return;
        var area=new AABB(pos).inflate(.001, .0625, .001).expandTowards(0,4,0);
        for(var target:level.getEntitiesOfClass(LivingEntity.class,area,e->e.isAlive() && e.onGround())) {
            var feet=feet(target);
            var shape=state.getCollisionShape(level,pos,CollisionContext.of(target)).move(pos.getX(),pos.getY(),pos.getZ());
            if(shape.toAabbs().stream().anyMatch(feet::intersects))
                removedSupports.put(target,new Support(owner,level));
        }
    }

    /** После всего луча проверяем реальную потерю опоры; неудачный destroy не даёт авторства. */
    public void finishSupportRemoval() {
        removedSupports.forEach((target,support)-> {
            if(target.isAlive() && target.level()==support.level && !supported(target))
                falls.put(target,new Fall(support.owner,false));
        });
        removedSupports.clear();
    }

    private static AABB feet(LivingEntity target) {
        var box=target.getBoundingBox();
        return new AABB(box.minX+1e-5,box.minY-.0625,box.minZ+1e-5,
                box.maxX-1e-5,box.minY+1e-5,box.maxZ-1e-5);
    }
    private static boolean supported(LivingEntity target) {
        return target.level().getBlockCollisions(target,feet(target)).iterator().hasNext();
    }

    public void stonePlaced(ServerLevel level, BlockPos pos, ServerPlayer player) {
        stone.computeIfAbsent(level,ignored->new HashMap<>()).put(pos.immutable(),owner(player));
    }

    /** События собирают только новые удушающие блоки этой серии, без обхода всего объёма. */
    public void trapChanges(ServerPlayer owner, Runnable writes) {
        trapChanges(owner.level(),owner,writes);
    }
    public void trapChanges(ServerLevel level,ServerPlayer owner,Runnable writes) {
        trapChanges(level,owner,false,writes);
    }
    /** A WILD trap retains its friendly-fire policy in suffocation and placed hazards. */
    public void trapChanges(ServerLevel level,ServerPlayer owner,boolean friendlyFire,Runnable writes) {
        var cause=owner(owner,friendlyFire);
        var previous=trapFrame;
        var frame=new TrapFrame(cause,level,new HashMap<>());
        trapFrame=frame;
        try {
            blockWrites(level,cause,writes);
        } finally {
            try {
                frame.candidates.forEach((pos,expected)-> {
                    var current=level.getBlockState(pos);
                    if(current.equals(expected) && current.isSuffocating(level,pos)) {
                        stone.computeIfAbsent(level,ignored->new HashMap<>()).put(pos.immutable(),frame.owner);
                        if(previous!=null && previous.level==level) previous.candidates.remove(pos);
                    }
                });
            } finally {
                trapFrame=previous;
            }
        }
    }
    public void blockChanged(ServerLevel level, BlockPos pos, BlockState state) {
        var blocks=stone.get(level);
        if(blocks!=null) {
            blocks.remove(pos);
            if(blocks.isEmpty()) stone.remove(level);
        }
        var damageBlocks=hazards.get(level);
        if(damageBlocks!=null) { damageBlocks.remove(pos); if(damageBlocks.isEmpty()) hazards.remove(level); }
        if(hazardWriter!=null && hazardLevel==level && hazardous(state))
            hazards.computeIfAbsent(level,ignored->new HashMap<>()).put(pos.immutable(),new Hazard(hazardWriter,state));
    }
    private static boolean hazardous(BlockState state) {
        return state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.WATER);
    }
    public void hazardPlaced(ServerLevel level, BlockPos pos, ServerPlayer player) {
        var state=level.getBlockState(pos);
        if(hazardous(state)) hazards.computeIfAbsent(level,ignored->new HashMap<>())
                .put(pos.immutable(),new Hazard(owner(player),state));
    }
    /** Нативное растекание лавы/распространение огня переносит автора на реально созданные блоки. */
    public void hazardTick(ServerLevel level, BlockPos pos, Runnable tick) {
        var blocks=hazards.get(level); var source=blocks==null ? null : blocks.get(pos);
        blockWrites(level,source==null ? null : source.owner,tick);
    }
    public void worldChanges(ServerLevel level, Entity source, Runnable writes) {
        worldChanges(level,source,source instanceof net.minecraft.world.entity.LightningBolt || game.items().cageDrops().wild(source)
                || game.items().zombieTntDrops().wild(source),writes);
    }
    /** Terrain traps retain WILD consequences after their source item has been consumed. */
    public void worldChanges(ServerLevel level, Entity source, boolean friendlyFire, Runnable writes) {
        var player=playerSource(source);
        blockWrites(level,player==null ? null : new Owner(player.getUUID(),++order,friendlyFire,
                source instanceof Projectile || source instanceof net.minecraft.world.entity.LightningBolt),writes);
    }
    private void blockWrites(ServerLevel level, Owner source, Runnable writes) {
        var previous=hazardWriter; var previousLevel=hazardLevel;
        hazardWriter=source; hazardLevel=level;
        try { writes.run(); } finally {
            finishSupportRemoval();
            hazardWriter=previous; hazardLevel=previousLevel;
        }
    }
    public void beforeBlockChange(ServerLevel level, BlockPos pos, BlockState state) {
        var frame=trapFrame;
        if(frame!=null && frame.level==level) {
            var key=pos.immutable();
            if(!state.isSuffocating(level,pos)) frame.candidates.remove(key);
            else {
                var old=level.getBlockState(pos);
                if(frame.candidates.containsKey(key) || !old.isSuffocating(level,pos))
                    frame.candidates.put(key,state);
            }
        }
        if(hazardWriter==null || hazardLevel!=level || !state.getCollisionShape(level,pos).isEmpty()) return;
        supportRemoved(level,hazardWriter,pos);
    }
    public void environmentDamage(LivingEntity target, DamageSource source) {
        var cause=environmentOwner(target,source);
        if(target.level() instanceof ServerLevel level) credit(target,level,cause);
    }
    /** Огонь/лава игрока не вредят союзникам и защищённым NPC; природные блоки остаются ванильными. */
    public boolean allowEnvironmentDamage(LivingEntity target, DamageSource source) {
        var cause=environmentOwner(target,source);
        if(cause==null) return true;
        var owner=game.server().getPlayerList().getPlayer(cause.player);
        return owner==null || owner==target || (cause.friendlyFire ? game.items().wands().target(owner,target,true)
                : cause.projectile ? game.items().wands().projectileTarget(owner,target) : game.items().wands().target(owner,target));
    }
    private Owner environmentOwner(LivingEntity target, DamageSource source) {
        if(source.is(DamageTypes.IN_WALL)) return suffocationOwner(target);
        boolean fire=source.is(DamageTypes.LAVA) || source.is(DamageTypes.IN_FIRE);
        boolean floor=source.is(DamageTypes.HOT_FLOOR);
        if(!fire && !floor && !source.is(DamageTypes.DROWN) && !source.is(DamageTypes.ON_FIRE)) return null;
        Owner cause=null;
        if(source.is(DamageTypes.ON_FIRE)) cause=burning.get(target);
        else if(target.level() instanceof ServerLevel level) {
            var blocks=hazards.get(level);
            if(blocks!=null) {
                var area=floor ? target.getBoundingBox().move(0,-.2,0) : target.getBoundingBox().inflate(.001);
                // Для огромного scale дешевле проверить индекс опасных блоков, чем объём хитбокса.
                if(area.getXsize()*area.getYsize()*area.getZsize()>blocks.size()) {
                    for(var entry:blocks.entrySet()) if(area.intersects(new AABB(entry.getKey()))) {
                        var hazard=entry.getValue();
                        if(matchingHazard(hazard.state,source) && (cause==null || hazard.owner.order>cause.order)) cause=hazard.owner;
                    }
                } else for(var pos:BlockPos.betweenClosed(area)) {
                    var hazard=blocks.get(pos);
                    if(hazard!=null && matchingHazard(hazard.state,source)
                            && (cause==null || hazard.owner.order>cause.order)) cause=hazard.owner;
                }
            }
            if(fire) { if(cause!=null) burning.put(target,cause); else burning.remove(target); }
        }
        return cause;
    }
    private Owner suffocationOwner(LivingEntity target) {
        if(!(target.level() instanceof ServerLevel level)) return null;
        var blocks=stone.get(level);
        if(blocks==null) return null;
        // Тонкая область глаз совпадает с vanilla-проверкой удушения; тип созданного блока произволен.
        double width=target.getBbWidth()*.8;
        var eyes=AABB.ofSize(target.getEyePosition(),width,1e-6,width);
        Owner cause=null;
        for(var pos:BlockPos.betweenClosed(eyes)) {
            var owner=blocks.get(pos);
            BlockState state=level.getBlockState(pos);
            if(owner!=null && state.isSuffocating(level,pos)
                    && state.getCollisionShape(level,pos).move(pos.getX(),pos.getY(),pos.getZ())
                    .toAabbs().stream().anyMatch(eyes::intersects)
                    && (cause==null || owner.order>cause.order)) cause=owner;
        }
        return cause;
    }
    private static boolean matchingHazard(BlockState state, DamageSource source) {
        return source.is(DamageTypes.DROWN) ? state.is(Blocks.WATER)
                : source.is(DamageTypes.LAVA) ? state.is(Blocks.LAVA)
                : source.is(DamageTypes.HOT_FLOOR) ? state.is(Blocks.MAGMA_BLOCK)
                : state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE);
    }
    /** Нативное поджигание сущностью, которому не предшествовал контакт с принадлежащим игроку блоком огня. */
    public void ignited(LivingEntity target,ServerPlayer player) {
        ignited(target,player,false);
    }
    public void ignited(LivingEntity target,ServerPlayer player,boolean friendlyFire) {
        if(target.isAlive() && target.isOnFire()) burning.put(target,owner(player,friendlyFire));
    }
    public void projectileIgnited(LivingEntity target,ServerPlayer player) {
        projectileIgnited(target,player,false);
    }
    public void projectileIgnited(LivingEntity target,ServerPlayer player,boolean friendlyFire) {
        if(target.isAlive() && target.isOnFire()) burning.put(target,new Owner(player.getUUID(),++order,friendlyFire,true));
    }
    public void poisoned(LivingEntity target, ServerPlayer player) {
        var effect=target.getEffect(MobEffects.POISON);
        if(effect!=null) effectChanged(target,effect,player);
    }
    /** Наложение/замена постороннего яда не наследует владельца кислоты. */
    public void poisonChanged(LivingEntity target) {
        var effects=statuses.get(target);
        if(effects!=null) { effects.remove(MobEffects.POISON); if(effects.isEmpty()) statuses.remove(target); }
    }
    public void levitationChanged(LivingEntity target) {
        var fall=falls.get(target);
        if(fall!=null && fall.levitation) falls.remove(target);
    }

    public void damaged(LivingEntity target, DamageSource source) {
        var player=playerSource(source.getEntity());
        if(player!=null && player!=target) {
            displaced(target,player);
        }
    }
    /** Перемещение жезлом без прямого урона также может окончиться падением; более поздний удар перебьёт автора. */
    public void displaced(LivingEntity target,ServerPlayer player) {
        if(target==player) return;
        var cause=owner(player);
        laterAttacks.put(target,cause);
        falls.put(target,new Fall(cause,false));
    }

    public void tick() {
        burning.keySet().removeIf(target->!target.isAlive() || target.isRemoved() || !target.isOnFire());
        var iterator=falls.entrySet().iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next();
            var target=entry.getKey(); var fall=entry.getValue();
            if(!target.isAlive() || target.isRemoved()) { iterator.remove(); continue; }
            if(fall.levitation && target.hasEffect(MobEffects.LEVITATION)) continue;
            if(fall.grace > 0) { fall.grace--; continue; }
            boolean safe=target.isInWater() || target.onClimbable() || target.isFallFlying()
                    || target instanceof ServerPlayer player && player.getAbilities().flying;
            if(safe || target.onGround() && supported(target)) iterator.remove();
        }
        statuses.entrySet().removeIf(entry-> {
            var target=entry.getKey();
            if(!target.isAlive() || target.isRemoved()) return true;
            entry.getValue().entrySet().removeIf(effect->target.getEffect(effect.getKey())!=effect.getValue().effect);
            return entry.getValue().isEmpty();
        });
    }

    /** Вызывается до нативного die: mob loot/XP, vanilla kill score и статистика мода видят автора. */
    public void dying(LivingEntity target, DamageSource damage) {
        var summoner=summonedOwner(damage.getEntity());
        if(summoner!=null && target.level() instanceof ServerLevel level) {
            credit(target,level,owner(summoner)); return;
        }
        if(damage.getEntity()!=null || !(target.level() instanceof ServerLevel level)) return;
        environmentDamage(target,damage);
        Owner cause=null;
        if(manaTarget==target) cause=manaDebts.get(target);
        else if(damage.is(DamageTypes.FALL) || damage.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            var fall=falls.get(target);
            if(fall!=null) cause=fall.owner;
        } else if(damage.is(DamageTypes.IN_WALL)) cause=suffocationOwner(target);
        else if(effectTarget==target && (damage.is(DamageTypes.MAGIC) && tickingEffect==MobEffects.POISON
                || damage.is(DamageTypes.WITHER) && tickingEffect==MobEffects.WITHER)) {
            var effects=statuses.get(target);
            var status=effects==null ? null : effects.get(tickingEffect);
            if(status!=null && target.getEffect(tickingEffect)==status.effect) cause=status.owner;
        }
        credit(target,level,cause);
    }

    private void credit(LivingEntity target, ServerLevel level, Owner cause) {
        if(cause==null) return;
        var later=laterAttacks.get(target);
        if(later!=null && later.order>cause.order && !later.player.equals(cause.player)) return;
        var player=game.server().getPlayerList().getPlayer(cause.player);
        if(player==null || player==target) return;
        target.setLastHurtByPlayer(player,100);
        target.setLastHurtByMob(player);
    }

    /** Контекст только нативного poison tick: другой magic-урон не становится кислотным. */
    public boolean poisonDamage(LivingEntity target, BooleanSupplier damage) {
        return statusDamage(target,MobEffects.POISON,damage);
    }
    public boolean statusDamage(LivingEntity target, Holder<MobEffect> effect, BooleanSupplier damage) {
        LivingEntity previous=effectTarget; var previousEffect=tickingEffect;
        effectTarget=target; tickingEffect=effect;
        try {
            boolean applied=damage.getAsBoolean();
            var effects=statuses.get(target);
            var status=effects==null ? null : effects.get(effect);
            if(applied && status!=null && target.getEffect(effect)==status.effect
                    && target.level() instanceof ServerLevel level) credit(target,level,status.owner);
            return applied;
        } finally { effectTarget=previous; tickingEffect=previousEffect; }
    }

    public void clear(LivingEntity target) {
        falls.remove(target); statuses.remove(target); removedSupports.remove(target); laterAttacks.remove(target);
        pendingMana.remove(target); manaDrains.remove(target); manaDebts.remove(target);
        burning.remove(target);
    }
    public void stop() {
        falls.clear(); statuses.clear(); removedSupports.clear(); laterAttacks.clear(); stone.clear();
        pendingMana.clear(); manaDrains.clear(); manaDebts.clear();
        hazards.clear(); burning.clear();
    }
}

