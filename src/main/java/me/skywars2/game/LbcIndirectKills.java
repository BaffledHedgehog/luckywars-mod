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
        var level=game.items().spellSources().level(owner);
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
    record Owner(UUID player, long order, boolean friendlyFire, boolean projectile) { }
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
    private record Arrival(Owner owner,ServerLevel level,Map<BlockPos,BlockState> cells) { }
    private final GameController game;
    // Ссылки на выгруженных мобов не удерживают сущности/чанки в памяти.
    private final Map<LivingEntity,Fall> falls = new WeakHashMap<>();
    private final Map<LivingEntity,Map<Holder<MobEffect>,Status>> statuses = new WeakHashMap<>();
    private final Map<LivingEntity,Status> pendingMana = new WeakHashMap<>();
    private final Map<LivingEntity,Owner> manaDrains = new WeakHashMap<>();
    private final Map<LivingEntity,Owner> manaDebts = new WeakHashMap<>();
    private final Map<ServerPlayer,Owner> hungerDebts = new WeakHashMap<>();
    private final Map<LivingEntity,Support> removedSupports = new WeakHashMap<>();
    private final Map<LivingEntity,Owner> laterAttacks = new WeakHashMap<>();
    private final Map<ServerLevel,Map<BlockPos,Owner>> stone = new HashMap<>();
    private final Map<ServerLevel,Map<BlockPos,Hazard>> hazards = new HashMap<>();
    private final Map<LivingEntity,Owner> burning = new WeakHashMap<>();
    private final Map<LivingEntity,Arrival> arrivals = new WeakHashMap<>();
    private long order;
    private LivingEntity causeTarget;
    private Owner activeCause;
    private static final class CapturedDamage extends DamageSource {
        final Owner cause;final DamageSource original;
        CapturedDamage(DamageSource source,Owner cause){super(source.typeHolder(),source.getDirectEntity(),source.getEntity());this.cause=cause;this.original=source;}
        @Override public net.minecraft.world.phys.Vec3 getSourcePosition(){return original.getSourcePosition();}
        @Override public boolean scalesWithDifficulty(){return original.scalesWithDifficulty();}
        @Override public net.minecraft.network.chat.Component getLocalizedDeathMessage(LivingEntity victim){return original.getLocalizedDeathMessage(victim);}
    }
    public void withCause(LivingEntity target,Owner cause,Runnable action){
        var previousTarget=causeTarget;var previousCause=activeCause;causeTarget=target;activeCause=cause;
        try{action.run();}finally{causeTarget=previousTarget;activeCause=previousCause;}
    }
    public DamageSource inheritDamage(LivingEntity target,DamageSource source){
        if(source instanceof CapturedDamage)return source;
        var accepted=causeTarget==target&&activeCause!=null?activeCause:babylonCause(source);
        return accepted==null?source:new CapturedDamage(source,accepted);
    }
    /** Detached spell segments retain the original accepted effect's author and ordering. */
    public DamageSource captureDamage(DamageSource source,Owner cause){return cause==null?source:new CapturedDamage(source,cause);}
    /** Preserve source-specific behavior when native attribution has wrapped a damage source. */
    static DamageSource originalDamage(DamageSource source){
        while(source instanceof CapturedDamage captured)source=captured.original;
        return source;
    }
    /** Read-only parent for the native combat checkpoint; accepted causes are never regenerated. */
    static DamageSource checkpointWrappedSource(DamageSource source){return source instanceof CapturedDamage captured?captured.original:null;}
    /** Native ServerPlayer friendship admission runs before LivingEntity's damage callbacks. */
    public static boolean isWildCaptured(DamageSource source){return source instanceof CapturedDamage captured&&captured.cause.friendlyFire();}
    public void inheritedEffect(LivingEntity target,MobEffectInstance effect,Owner cause){
        var owner=cause==null?null:game.server().getPlayerList().getPlayer(cause.player());
        if(!target.addEffect(effect,owner)||cause==null||target==owner)return;
        var active=target.getEffect(effect.getEffect());
        if(active!=null&&active.getAmplifier()==effect.getAmplifier()
                &&(effect.is(MobEffects.POISON)||effect.is(MobEffects.WITHER)||effect.is(MobEffects.BLINDNESS)))
            statuses.computeIfAbsent(target,k->new HashMap<>()).put(effect.getEffect(),new Status(cause,active));
    }
    /** Preserve accepted native effect causes across a legitimate replacement-player respawn. */
    java.util.Map<Holder<MobEffect>,Owner> effectCauses(LivingEntity target){
        var result=new java.util.HashMap<Holder<MobEffect>,Owner>();var active=statuses.get(target);
        if(active!=null)active.forEach((type,status)->{if(target.getEffect(type)==status.effect)result.put(type,status.owner);});
        return java.util.Map.copyOf(result);
    }
    private LivingEntity effectTarget, manaTarget;
    private LivingEntity altitudeTarget;
    private Owner altitudeCause;
    private Holder<MobEffect> tickingEffect;
    private Owner hazardWriter;
    private ServerLevel hazardLevel;
    private TrapFrame trapFrame;
    private record SupportCandidates(ServerLevel level,AABB blocks,java.util.List<LivingEntity> entities,SupportCandidates previous) { }
    private SupportCandidates supportCandidates;

    LbcIndirectKills(GameController game) { this.game=game; }
    private Owner owner(ServerPlayer player) { return owner(player,false); }
    private Owner owner(ServerPlayer player,boolean friendlyFire) {
        var source=game.items().spellSources().current();if(source!=null&&(player==null||source.author().equals(player.getUUID())))return source.cause();
        return player==null?null:new Owner(player.getUUID(),++order,friendlyFire,false);
    }

    /** WILD totem Wither keeps its author and allegiance policy through real effect ticks. */
    void wildEffect(LivingEntity target,MobEffectInstance effect,Owner cause){
        var player=cause==null?null:game.server().getPlayerList().getPlayer(cause.player);
        if(target.addEffect(effect,player)&&cause!=null){
            var active=target.getEffect(effect.getEffect());
            if(active!=null)statuses.computeIfAbsent(target,ignored->new HashMap<>()).put(effect.getEffect(),new Status(cause,active));
        }
    }

    /** Deferred Frost processing retains the original impact's order, rather than becoming a new hit. */
    Owner wildCause(ServerPlayer player){return owner(player,true);}
    /** The original cause travels with Time Stop's saved DamageSource; later accepted hits can supersede it. */
    private static final class InheritedKill extends DamageSource {
        final Owner cause;
        InheritedKill(LivingEntity target,Owner cause){super(target.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.GENERIC_KILL));this.cause=cause;}
    }
    /** Explicit execution intent is distinct from ordinary, even enormous, damage. */
    boolean inheritedExecution(LivingEntity target,Owner cause){return game.items().wtf().erase(target)||inheritedKill(target,cause);}
    boolean inheritedKill(LivingEntity target,Owner cause){return target.hurtServer((ServerLevel)target.level(),new InheritedKill(target,cause),Float.MAX_VALUE);}
    void manaLost(LivingEntity target,Owner cause){if(cause!=null)manaDebts.put(target,newer(manaDebts.get(target),cause));}
    Owner coldCause(ServerPlayer player,boolean projectile){return new Owner(player.getUUID(),++order,false,projectile);}
    Owner restoreColdCause(UUID player,long stamp,boolean projectile){
        if(stamp<=0)stamp=++order;else order=Math.max(order,stamp);
        return new Owner(player,stamp,false,projectile);
    }
    void coldEffect(LivingEntity target,MobEffectInstance effect,Owner cause){
        var player=cause==null?null:game.server().getPlayerList().getPlayer(cause.player);
        if(target.addEffect(effect,player)&&cause!=null&&target!=player&&effect.is(MobEffects.BLINDNESS)){
            var active=target.getEffect(effect.getEffect());
            if(active!=null)statuses.computeIfAbsent(target,ignored->new HashMap<>()).put(effect.getEffect(),new Status(cause,active));
        }
    }

    /** Нативное зелье передаёт source как игрока, снаряд или облако — разворачиваем владельца. */
    public static ServerPlayer playerSource(Entity source) {
        var original=source;
        var controller=me.skywars2.SkyWars2.game();
        if(controller!=null && controller.items().communism().owner(source) instanceof ServerPlayer player)return player;
        var game=me.skywars2.SkyWars2.game();
        if(game!=null && game.items().spellSources().owner(source) instanceof ServerPlayer player) return player;
        if(game!=null && game.items().chaosTrapActors().owner(source) instanceof ServerPlayer player) return player;
        if(game!=null && game.items().magicBean().owner(source) instanceof ServerPlayer player) return player;
        if(game!=null && game.items().edibleSpells().owner(source) instanceof ServerPlayer player) return player;
        if(game!=null && game.items().nukeEvent().owner(source) instanceof ServerPlayer player) return player;
        if(game!=null && game.items().oreshek().owner(source) instanceof ServerPlayer player) return player;
        if(game!=null && game.items().foodLifecycle().owner(source) instanceof ServerPlayer player) return player;
        if(game!=null && game.items().incomingArtifacts().owner(source) instanceof ServerPlayer player) return player;
        if(game!=null && game.items().moiseiBoots().owner(source) instanceof ServerPlayer player) return player;
        if(game!=null && game.items().quiver().owner(source) instanceof ServerPlayer player) return player;
        if(game!=null && game.items().cageDrops().owner(source) instanceof ServerPlayer player) return player;
        if(game!=null && game.items().clouds().sourceAcidOwner(source) instanceof ServerPlayer player) return player;
        if(game!=null && game.items().electro().reflectedOwner(source)==null
                && game.items().inventoryHazards().owner(source) instanceof ServerPlayer player) return player;
        if(source instanceof Projectile projectile) source=projectile.getOwner();
        if(source instanceof AreaEffectCloud cloud) source=cloud.getOwner();
        if(source instanceof PrimedTnt tnt) source=tnt.getOwner();
        if(source instanceof net.minecraft.world.entity.LightningBolt bolt) source=bolt.getCause();
        if(source instanceof ServerPlayer player) return player;
        var owner=summonedOwner(source);
        // Native traps may intentionally have no projectile owner so they can hit their thrower.
        return owner!=null ? owner : source==null ? summonedOwner(original) : null;
    }

    /** Общая атрибуция призванных мобов; используется также до нативного смертельного удара. */
    private static ServerPlayer summonedOwner(Entity source) {
        var effectOwner=SignedEnvironmentalEffects.spawnedOwner(source);if(effectOwner!=null)return effectOwner;
        var game=me.skywars2.SkyWars2.game();
        if(game==null) return null;
        if(game.items().wtf().owner(source) instanceof ServerPlayer player) return player;
        var totemOwner=game.items().totems().owner(source);if(totemOwner!=null)return totemOwner;
        if(game.items().soulless().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().danmaku().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().sheep().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().hedgehog().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().godHedgehog().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().hedgehogSpikes().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().creeperDrops().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().luckyPigs().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().beeDrops().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().slabDrops().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().creatureDrops().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().droneDrops().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().antDrops().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().potionSpawnerDrops().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().opAnvilDrops().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().zombieTntDrops().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().fallingNetheriteDrops().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().transformator().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().stalin().owner(source) instanceof ServerPlayer player) return player;
        if(game.items().helicopterEncounter().owner(source) instanceof ServerPlayer player) return player;
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
        if(source instanceof net.minecraft.world.entity.monster.warden.Warden && LbcCreatureDrops.managed(source)
                && effect.value().getCategory()==net.minecraft.world.effect.MobEffectCategory.HARMFUL)
            return game.items().creatureDrops().canAffect(source,target);
        if(LbcPotionSpawnerDrops.managed(source)) return game.items().potionSpawnerDrops().canAffect(source,target);
        if(LbcClouds.sourceAcidManaged(source)) return game.items().clouds().sourceAcidCanAffect(source,target);
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
        if(effect.is(MobEffects.HUNGER)&&target instanceof ServerPlayer p){
            hungerDebts.remove(p);
            if(player!=null&&player!=target)hungerDebts.put(p,owner(player));
        }
        if(effect.is(MobEffects.LEVITATION)) {
            levitationChanged(target);
            if(player!=null && player!=target) levitated(target,player);
        }
        if(effect.is(MobEffects.POISON) || effect.is(MobEffects.WITHER) || effect.is(MobEffects.BLINDNESS)
                ||effect.is(MobEffects.CONDUIT_POWER)||effect.is(MobEffects.INSTANT_DAMAGE)||effect.is(MobEffects.INSTANT_HEALTH)) {
            effectRemoved(target,effect);
            if(player!=null && player!=target) statuses.computeIfAbsent(target,ignored->new HashMap<>())
                    .put(effect.getEffect(),new Status(effect.is(MobEffects.CONDUIT_POWER)?
                            new Owner(player.getUUID(),++order,false,true):owner(player,LbcTotems.managed(source)),effect));
        }
        if(effect.is(MobEffects.RAID_OMEN) && effect.getAmplifier()==20) {
            pendingMana.remove(target);
            if(player!=null && player!=target) pendingMana.put(target,new Status(owner(player),effect));
        }
    }
    public void effectRemoved(LivingEntity target, MobEffectInstance effect) {
        // Natural expiry can leave an empty food bar; an explicit cleanse ends that causal debt.
        if(effect.is(MobEffects.HUNGER)&&target instanceof ServerPlayer p&&effect.getDuration()!=0)hungerDebts.remove(p);
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
        supportRemoved(game.items().spellSources().level(player),player,pos);
    }
    public void supportRemoved(ServerLevel level,ServerPlayer player,BlockPos pos) {
        supportRemoved(level,owner(player),pos);
    }
    /** The write scope already owns the cause, even if its author has since left. */
    void supportRemoved(ServerLevel level,Owner owner,BlockPos pos) {
        var state=level.getBlockState(pos);
        if(state.getCollisionShape(level,pos).isEmpty()) return;
        var area=new AABB(pos).inflate(.001, .0625, .001).expandTowards(0,4,0);
        var batch=supportCandidates;
        var targets=batch!=null && batch.level==level && batch.blocks.intersects(new AABB(pos))
                ? batch.entities : level.getEntitiesOfClass(LivingEntity.class,area,e->e.isAlive() && e.onGround());
        for(int index=0;index<targets.size();index++) {
            var target=targets.get(index);
            if(target.level()!=level || !target.isAlive() || !target.onGround()
                    || !target.getBoundingBox().intersects(area)) continue;
            var feet=feet(target);
            var shape=state.getCollisionShape(level,pos,CollisionContext.of(target)).move(pos.getX(),pos.getY(),pos.getZ());
            if(shape.toAabbs().stream().anyMatch(feet::intersects))
                removedSupports.put(target,new Support(owner,level));
        }
    }

    /** A bounded synchronous terrain operation reuses candidates, retaining live shape checks. */
    public void withSupportCandidates(ServerLevel level,AABB blocks,Runnable writes) {
        var previous=supportCandidates;
        supportCandidates=new SupportCandidates(level,blocks,new java.util.ArrayList<>(level.getEntitiesOfClass(
                LivingEntity.class,blocks.inflate(.001,.0625,.001).expandTowards(0,4,0),Entity::isAlive)),previous);
        try { writes.run(); } finally { supportCandidates=previous; }
    }
    /** Native block callbacks can admit a new living actor while the operation is executing. */
    public void supportEntityLoaded(Entity actor) {
        if(!(actor instanceof LivingEntity living)) return;
        for(var batch=supportCandidates;batch!=null;batch=batch.previous)
            if(actor.level()==batch.level && !batch.entities.contains(living)) batch.entities.add(living);
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
        trapChanges(game.items().spellSources().level(owner),owner,writes);
    }
    public void trapChanges(ServerLevel level,ServerPlayer owner,Runnable writes) {
        trapChanges(level,owner,false,writes);
    }
    /** A WILD trap retains its friendly-fire policy in suffocation and placed hazards. */
    public void trapChanges(ServerLevel level,ServerPlayer owner,boolean friendlyFire,Runnable writes) {
        trapChanges(level,owner(owner,friendlyFire),writes);
    }
    public void trapChanges(ServerLevel level,Owner cause,Runnable writes){
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
    /** Opaque rollback token: only exact terrain cells and potentially touched support leases. */
    static final class TerrainProvenance {
        private final ServerLevel level;
        private final java.util.Set<BlockPos> cells;
        private final Map<BlockPos,Owner> stoneBefore=new HashMap<>();
        private final Map<BlockPos,Hazard> hazardsBefore=new HashMap<>();
        private final java.util.Set<LivingEntity> supportTargets=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        private final Map<LivingEntity,Support> supportBefore=new java.util.IdentityHashMap<>();
        private final Map<LivingEntity,Fall> fallsBefore=new java.util.IdentityHashMap<>();
        private TerrainProvenance(ServerLevel level,java.util.Collection<BlockPos> cells){this.level=level;this.cells=java.util.Set.copyOf(cells);}
    }
    TerrainProvenance captureTerrain(ServerLevel level,java.util.Collection<BlockPos> cells){
        var saved=new TerrainProvenance(level,cells);
        var oldStone=stone.get(level);var oldHazards=hazards.get(level);
        for(var pos:saved.cells){
            if(oldStone!=null&&oldStone.containsKey(pos))saved.stoneBefore.put(pos,oldStone.get(pos));
            if(oldHazards!=null&&oldHazards.containsKey(pos))saved.hazardsBefore.put(pos,oldHazards.get(pos));
        }
        // A diagonal chain must not query every entity chunk in its enclosing rectangle.
        // Bound each query to one actual chain chunk and its own vertical extent; deduplicate edge witnesses.
        var chunkBounds=new java.util.LinkedHashMap<net.minecraft.world.level.ChunkPos,net.minecraft.world.level.levelgen.structure.BoundingBox>();
        for(var pos:saved.cells){
            var chunk=new net.minecraft.world.level.ChunkPos(pos.getX()>>4,pos.getZ()>>4);
            var bounds=chunkBounds.get(chunk);
            if(bounds==null)chunkBounds.put(chunk,new net.minecraft.world.level.levelgen.structure.BoundingBox(pos));
            else bounds.encapsulate(pos);
        }
        for(var box:chunkBounds.values()){
            var area=new AABB(box.minX(),box.minY(),box.minZ(),box.maxX()+1,box.maxY()+1,box.maxZ()+1)
                    .inflate(.001,.0625,.001).expandTowards(0,4,0);
            saved.supportTargets.addAll(level.getEntitiesOfClass(LivingEntity.class,area,Entity::isAlive));
        }
        for(var target:saved.supportTargets){
            var support=removedSupports.get(target);if(support!=null)saved.supportBefore.put(target,support);
            var fall=falls.get(target);if(fall!=null)saved.fallsBefore.put(target,fall);
        }
        return saved;
    }
    /** Run after trapChanges finally: that finalizer can itself republish suffocation ownership. */
    void restoreTerrain(TerrainProvenance saved){
        var oldStone=stone.get(saved.level);if(oldStone!=null){saved.cells.forEach(oldStone::remove);if(oldStone.isEmpty())stone.remove(saved.level);}
        var oldHazards=hazards.get(saved.level);if(oldHazards!=null){saved.cells.forEach(oldHazards::remove);if(oldHazards.isEmpty())hazards.remove(saved.level);}
        if(!saved.stoneBefore.isEmpty())stone.computeIfAbsent(saved.level,ignored->new HashMap<>()).putAll(saved.stoneBefore);
        if(!saved.hazardsBefore.isEmpty())hazards.computeIfAbsent(saved.level,ignored->new HashMap<>()).putAll(saved.hazardsBefore);
        saved.supportTargets.forEach(removedSupports::remove);removedSupports.putAll(saved.supportBefore);
        saved.supportTargets.forEach(falls::remove);falls.putAll(saved.fallsBefore);
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
                || game.items().zombieTntDrops().wild(source) || game.items().droneDrops().wild(source)
                || game.items().creeperDrops().wild(source) || LbcTransformator.managed(source) || SauvojenMeditation.managed(source),writes);
    }
    /** Terrain traps retain WILD consequences after their source item has been consumed. */
    public void worldChanges(ServerLevel level, Entity source, boolean friendlyFire, Runnable writes) {
        var player=playerSource(source);
        var inherited=SauvojenMeditation.cause(source);
        if(inherited==null)inherited=game.items().babylonEffects().cause(source);
        blockWrites(level,inherited!=null ? inherited : player==null ? null : new Owner(player.getUUID(),++order,friendlyFire,
                source instanceof Projectile || source instanceof net.minecraft.world.entity.LightningBolt),writes);
    }
    /** Moving terrain effects retain the original cast order across their child columns. */
    void capturedWorldChanges(ServerLevel level,Owner source,Runnable writes){blockWrites(level,source,writes);}
    private void blockWrites(ServerLevel level, Owner source, Runnable writes) {
        var previous=hazardWriter; var previousLevel=hazardLevel;
        hazardWriter=source; hazardLevel=level;
        try { writes.run(); } finally {
            finishSupportRemoval();
            hazardWriter=previous; hazardLevel=previousLevel;
        }
    }
    /** A continuing native effect keeps its original influence order when creating terrain hazards. */
    void effectWorldChanges(LivingEntity target,Holder<MobEffect> effect,Runnable writes){
        var entries=statuses.get(target);var status=entries==null?null:entries.get(effect);
        var cause=status!=null&&status.effect==target.getEffect(effect)?status.owner:null;
        blockWrites((ServerLevel)target.level(),cause,writes);
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
        if(target==altitudeTarget && source.is(DamageTypes.DROWN)) return true;
        var cause=environmentOwner(target,source);
        if(cause==null) return true;
        var owner=game.server().getPlayerList().getPlayer(cause.player);
        return owner==null || owner==target || (cause.friendlyFire ? game.items().wands().target(owner,target,true)
                : cause.projectile ? game.items().wands().projectileTarget(owner,target) : game.items().wands().target(owner,target));
    }
    void clearHunger(ServerPlayer target){hungerDebts.remove(target);}
    private Owner environmentOwner(LivingEntity target, DamageSource source) {
        if(source.is(DamageTypes.STARVE)&&target instanceof ServerPlayer p)return hungerDebts.get(p);
        if(target==altitudeTarget && source.is(DamageTypes.DROWN)) return altitudeCause;
        var arrival=arrivalOwner(target,source);
        if(source.is(DamageTypes.IN_WALL)) return newer(suffocationOwner(target),arrival);
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
            cause=newer(cause,arrival);
            if(fire) { if(cause!=null) burning.put(target,cause); else burning.remove(target); }
        }
        return cause;
    }
    private static Owner newer(Owner first,Owner second) {
        return first==null || second!=null && second.order>first.order ? second : first;
    }
    private static AABB eyes(LivingEntity target) {
        double width=target.getBbWidth()*.8;
        return AABB.ofSize(target.getEyePosition(),width,1e-6,width);
    }
    /** Record only hazards actually touching the arrival; never claim unchanged terrain globally. */
    public void teleported(LivingEntity target,ServerPlayer player,boolean friendlyFire) {
        displaced(target,player,friendlyFire);
        arrivals.remove(target);
        if(target==player || !(target.level() instanceof ServerLevel level)) return;
        var cells=new HashMap<BlockPos,BlockState>();
        var body=target.getBoundingBox().inflate(.001);
        var eye=eyes(target);
        var area=body.minmax(eye).expandTowards(0,-.2,0);
        for(var pos:BlockPos.betweenClosed(area)) {
            var state=arrivalState(level,pos);
            if(state==null) continue;
            if(hazardous(state) && new AABB(pos).intersects(area)
                    || state.isSuffocating(level,pos) && state.getCollisionShape(level,pos,CollisionContext.of(target))
                        .move(pos.getX(),pos.getY(),pos.getZ()).toAabbs().stream().anyMatch(eye::intersects))
                cells.put(pos.immutable(),state);
        }
        if(!cells.isEmpty())arrivals.put(target,new Arrival(laterAttacks.get(target),level,cells));
    }
    private Owner arrivalOwner(LivingEntity target,DamageSource source) {
        var arrival=arrivals.get(target);
        if(arrival==null || target.level()!=arrival.level) return null;
        boolean wall=source.is(DamageTypes.IN_WALL);
        if(!wall && !source.is(DamageTypes.LAVA) && !source.is(DamageTypes.IN_FIRE)
                && !source.is(DamageTypes.DROWN) && !source.is(DamageTypes.HOT_FLOOR)) return null;
        var area=wall ? eyes(target) : source.is(DamageTypes.HOT_FLOOR)
                ? target.getBoundingBox().move(0,-.2,0) : target.getBoundingBox().inflate(.001);
        for(var entry:arrival.cells.entrySet()) {
            var pos=entry.getKey();
            if(!area.intersects(new AABB(pos))) continue;
            var state=arrivalState(arrival.level,pos);
            if(!entry.getValue().equals(state)) continue;
            if(wall ? state.isSuffocating(arrival.level,pos) && state.getCollisionShape(arrival.level,pos,CollisionContext.of(target))
                        .move(pos.getX(),pos.getY(),pos.getZ()).toAabbs().stream().anyMatch(area::intersects)
                    : matchingHazard(state,source)) return arrival.owner;
        }
        return null;
    }
    private static BlockState arrivalState(ServerLevel level,BlockPos pos) {
        var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        return chunk==null ? null : chunk.getBlockState(pos);
    }
    private Owner suffocationOwner(LivingEntity target) {
        if(!(target.level() instanceof ServerLevel level)) return null;
        var blocks=stone.get(level);
        if(blocks==null) return null;
        // Тонкая область глаз совпадает с vanilla-проверкой удушения; тип созданного блока произволен.
        var eyes=eyes(target);
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
    public void inheritedIgnited(LivingEntity target,Owner cause) {
        if(cause!=null && target.isAlive() && target.isOnFire()) burning.put(target,newer(burning.get(target),cause));
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

    public Owner babylonCause(DamageSource source) {
        if (source instanceof CapturedDamage captured) return captured.cause;
        var event=game.items().apocalypseEvents().cause(source.getDirectEntity());if(event!=null)return event;
        var shish=game.items().shishEncounter().cause(source.getDirectEntity());if(shish!=null)return shish;
        var food=game.items().foodLifecycle().cause(source.getDirectEntity());if(food!=null)return food;
        var nut=game.items().oreshek().cause(source.getDirectEntity());if(nut!=null)return nut;
        var nuclear=game.items().nukeEvent().cause(source.getDirectEntity());if(nuclear!=null)return nuclear;
        var edible=game.items().edibleSpells().cause(source.getDirectEntity());if(edible!=null)return edible;
        var spell=game.items().spellSources().source(source.getDirectEntity());
        if(spell!=null)return spell.cause();
        if(LbcIncomingArtifacts.wild(source.getDirectEntity())){var author=game.items().incomingArtifacts().owner(source.getDirectEntity());if(author!=null)return wildCause(author);}
        if (source instanceof LbcBabylonEffects.OwnerDamageSource owned) return owned.cause;
        if(source.getEntity()==null && game.items().electro().reflectedOwner(source.getDirectEntity())==null) {
            var carried=game.items().inventoryHazards().cause(source.getDirectEntity());
            if(carried!=null) return carried;
        }
        var trialCause=SauvojenMeditation.cause(source.getDirectEntity());
        if(trialCause!=null)return trialCause;
        var quiverCause=game.items().quiver().cause(source.getDirectEntity());
        if(quiverCause!=null)return quiverCause;
        var bossCause=game.items().godHedgehog().cause(source.getDirectEntity());
        return bossCause!=null ? bossCause : game.items().babylonEffects().cause(source.getDirectEntity());
    }
    public void damaged(LivingEntity target, DamageSource source) {
        var inherited=causeTarget==target&&activeCause!=null ? activeCause : babylonCause(source);
        if(inherited!=null) { displaced(target,inherited); return; }
        var player=playerSource(source.getEntity());
        if(player==null && source.getEntity()==null)player=playerSource(source.getDirectEntity());
        if(player!=null && player!=target) {
            displaced(target,player,source instanceof LbcVoidArcanum.WildDamage || LbcTotems.managed(source.getEntity()));
        }
    }
    /** Перемещение жезлом без прямого урона также может окончиться падением; более поздний удар перебьёт автора. */
    public void displaced(LivingEntity target,ServerPlayer player) {
        displaced(target,player,false);
    }
    /** WILD displacement preserves the same policy as its terrain hazards. */
    public void displaced(LivingEntity target,ServerPlayer player,boolean friendlyFire) {
        if(target==player) return;
        var cause=owner(player,friendlyFire);
        laterAttacks.put(target,cause);
        falls.put(target,new Fall(cause,false));
    }
    /** Repeated sustained forces retain their original causal order. */
    void displaced(LivingEntity target,Owner cause) {
        if(cause==null) return;
        var accepted=newer(laterAttacks.get(target),cause);
        laterAttacks.put(target,accepted);
        var old=falls.get(target);
        falls.put(target,new Fall(newer(old==null?null:old.owner,accepted),false));
    }

    private Owner altitudeOwner(LivingEntity target) {
        var fall=falls.get(target);
        return fall==null?null:newer(fall.owner,laterAttacks.get(target));
    }
    public ServerPlayer altitudeAuthor(LivingEntity target) {
        var cause=altitudeOwner(target);
        return cause==null?null:game.server().getPlayerList().getPlayer(cause.player);
    }
    /** Only this scoped DROWN is lack of air at height; normal water drowning keeps its own cause. */
    public boolean altitudeDamage(LivingEntity target,BooleanSupplier damage) {
        var previousTarget=altitudeTarget; var previousCause=altitudeCause;
        altitudeTarget=target; altitudeCause=altitudeOwner(target);
        try {
            boolean applied=damage.getAsBoolean();
            if(applied && target.level() instanceof ServerLevel level) credit(target,level,altitudeCause);
            return applied;
        } finally { altitudeTarget=previousTarget; altitudeCause=previousCause; }
    }

    public void tick() {
        hungerDebts.keySet().removeIf(p->!p.isAlive()||p.isRemoved()||!p.hasEffect(MobEffects.HUNGER)&&p.getFoodData().getFoodLevel()>0);
        arrivals.entrySet().removeIf(entry-> {
            var target=entry.getKey();var arrival=entry.getValue();
            if(!target.isAlive() || target.isRemoved() || target.level()!=arrival.level) return true;
            var area=target.getBoundingBox().inflate(.001).minmax(eyes(target)).expandTowards(0,-.2,0);
            return arrival.cells.entrySet().stream().noneMatch(cell->area.intersects(new AABB(cell.getKey()))
                    && cell.getValue().equals(arrivalState(arrival.level,cell.getKey())));
        });
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
        var babylon=causeTarget==target&&activeCause!=null ? activeCause : babylonCause(damage);
        if(babylon!=null && target.level() instanceof ServerLevel level) { credit(target,level,babylon); return; }
        if(damage instanceof InheritedKill inherited&&target.level() instanceof ServerLevel level){credit(target,level,inherited.cause);return;}
        var duelCause=game.items().corruptedGlyphs().belialCause(target,damage);
        if(duelCause!=null&&target.level() instanceof ServerLevel duelLevel){credit(target,duelLevel,duelCause);return;}
        var summoner=summonedOwner(damage.getEntity());
        if(summoner==null && damage.getEntity()==null)summoner=summonedOwner(damage.getDirectEntity());
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
            // Accepted blindness can hide a ledge; cleansing/expiry must end that attribution.
            var effects=statuses.get(target);
            var blindness=effects==null ? null : effects.get(MobEffects.BLINDNESS);
            if(blindness!=null && target.getEffect(MobEffects.BLINDNESS)==blindness.effect
                    && (cause==null || blindness.owner.order>cause.order)) cause=blindness.owner;
        } else if(damage.is(DamageTypes.IN_WALL)) cause=suffocationOwner(target);
        else if(effectTarget==target && (damage.is(DamageTypes.MAGIC) && (tickingEffect==MobEffects.POISON||tickingEffect==MobEffects.INSTANT_DAMAGE||tickingEffect==MobEffects.INSTANT_HEALTH)
                || damage.is(DamageTypes.WITHER) && tickingEffect==MobEffects.WITHER)) {
            var effects=statuses.get(target);
            var status=effects==null ? null : effects.get(tickingEffect);
            if(status!=null && target.getEffect(tickingEffect)==status.effect) cause=status.owner;
        }
        if(damage.is(DamageTypes.IN_WALL)) cause=newer(cause,arrivalOwner(target,damage));
        credit(target,level,cause);
    }

    private void credit(LivingEntity target, ServerLevel level, Owner cause) {
        if(cause==null) return;
        var later=laterAttacks.get(target);
        // A newer accepted player action remains authoritative beyond vanilla's short hurt cache.
        if(later!=null && later.order>cause.order) cause=later;
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
        arrivals.remove(target);
        pendingMana.remove(target); manaDrains.remove(target); manaDebts.remove(target);
        burning.remove(target); hungerDebts.remove(target);
    }
    public void stop() {
        falls.clear(); statuses.clear(); removedSupports.clear(); laterAttacks.clear(); stone.clear();
        pendingMana.clear(); manaDrains.clear(); manaDebts.clear();
        hazards.clear(); burning.clear(); arrivals.clear(); hungerDebts.clear();
    }
}

