package me.skywars2.game;

import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Raid Omen124: native arrow-damage chain, distinct from Thunder's repeating native bolts. */
public final class LbcChainLightningStatus {
    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> MOBS=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final String COUNTER="effect_chain_lightning";
    private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<CompoundTag> DATA=
            net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(Identifier.parse("skywars2:chain_lightning_status"),b->b.persistent(CompoundTag.CODEC));
    private static final class Contribution {
        int remaining;final LbcIndirectKills.Owner cause;
        Contribution(int remaining,LbcIndirectKills.Owner cause){this.remaining=remaining;this.cause=cause;}
    }
    private final GameController game;
    private int callbackDepth;
    private boolean rewinding;
    private final Map<LivingEntity,ArrayDeque<Contribution>> pending=new LinkedHashMap<>();
    LbcChainLightningStatus(GameController game){this.game=game;}
    public static boolean isMarker(MobEffectInstance effect){return effect.is(MobEffects.RAID_OMEN)&&effect.getAmplifier()==124;}
    private boolean role(LivingEntity target){return target.level()==game.maps().level()&&game.items().wands().validTarget(target);}
    private boolean immune(LivingEntity target){return target instanceof ServerPlayer player&&(game.items().magic().rejectCustomDebuff(player));}
    boolean accept(LivingEntity target,MobEffectInstance effect,Entity source){
        callbackDepth++;try{
        if(!isMarker(effect)||!game.playing()||!role(target)||LbcGreed.active(target)||!target.canBeAffected(effect)||immune(target))return false;
        int duration=effect.getDuration()==2?1:effect.getDuration();if(duration<=0)return true;
        var owner=LbcIndirectKills.playerSource(source);var cause=owner==null?null:game.items().indirectKills().wildCause(owner);
        pending.computeIfAbsent(target,k->new ArrayDeque<>()).addLast(new Contribution(duration,cause));sync(target);return true;

        }finally{callbackDepth--;}
    }
    int queued(LivingEntity target){var queue=pending.get(target);return queue==null?0:(int)Math.min(Integer.MAX_VALUE,queue.stream().mapToLong(c->c.remaining).sum());}
    void observe(LivingEntity target){
        callbackDepth++;try{
        int external=game.items().mana().hudValue(target,COUNTER),stored=queued(target);
        if(external<=0){if(stored>0)clear(target);return;}
        if(external>stored&&!LbcGreed.active(target)&&!immune(target)&&role(target)){
            var cause=target instanceof ServerPlayer player?game.items().indirectKills().wildCause(player):null;
            pending.computeIfAbsent(target,k->new ArrayDeque<>()).addLast(new Contribution(external-stored,cause));sync(target);
        }

        }finally{callbackDepth--;}
    }
    private boolean paused(LivingEntity target){return !game.server().tickRateManager().runsNormally()
            ||game.items().timeStop().stopped((ServerLevel)target.level())||game.items().timeStop().frozen(target)
            ||target.entityTags().contains("time_stopped")||game.items().frost().isFrozen(target)||LbcGreed.active(target);}
    void tick(){
        callbackDepth++;try{
        if(rewinding)return;
        for(var target:List.copyOf(pending.keySet())){
            if(game.items().polymorph().masked(target))continue;
            if(LbcGreed.active(target))continue;
            if(!game.playing()||!role(target)||immune(target)){clear(target);continue;}
            if(paused(target))continue;
            observe(target);var queue=pending.get(target);if(queue==null||queue.isEmpty())continue;var contribution=queue.peekFirst();
            var level=(ServerLevel)target.level();
            // The source consumes the counter even when a stopper prevents this activation.
            if(!game.items().magic().mechanismSuppressedAt(level,target.position())&&!game.items().magic().suppressedAt(level,target.position()))pulse(target,contribution.cause);
            if(pending.get(target)!=queue)continue;
            if(--contribution.remaining<=0)queue.removeFirst();if(queue.isEmpty())clear(target);else sync(target);
        }

        }finally{callbackDepth--;}
    }
    private void pulse(LivingEntity emitter,LbcIndirectKills.Owner cause){
        callbackDepth++;try{
        var level=(ServerLevel)emitter.level();var owner=cause==null?null:game.server().getPlayerList().getPlayer(cause.player());
        var candidates=game.items().lifetimes().loadedTargets().stream().filter(this::role).filter(target->target.getType().builtInRegistryHolder().is(MOBS))
                .filter(target->!LbcGreed.active(target)&&!target.entityTags().contains("nomagic_active")
                        &&!game.items().magic().suppressedAt(level,target.position())).toList();
        propagate(level,emitter.position(),emitter,owner,cause,candidates,null);

        }finally{callbackDepth--;}
    }
    /** Source IMBA projectile chain excludes its caster/team at acquisition; its three child arcs remain WILD. */
    void arrowPulse(ServerPlayer owner,Entity projectile,Vec3 point,LbcIndirectKills.Owner accepted){
        callbackDepth++;try{
        var level=(ServerLevel)projectile.level();
        var candidates=game.items().lifetimes().loadedTargets().stream().filter(this::role)
                .filter(target->target.getType().builtInRegistryHolder().is(MOBS)&&(owner==null?!LbcGreed.active(target):game.items().wands().projectileTarget(owner,target))).toList();
        propagate(level,point,owner,owner,accepted,candidates,projectile);
        }finally{callbackDepth--;}
    }
    private void propagate(ServerLevel level,Vec3 start,LivingEntity emitter,ServerPlayer owner,LbcIndirectKills.Owner cause,List<LivingEntity> candidates,Entity projectile){
        var visited=Collections.newSetFromMap(new IdentityHashMap<LivingEntity,Boolean>());visited.add(emitter);
        var frontier=new ArrayDeque<Vec3>();frontier.add(start);
        while(!frontier.isEmpty()){
            var from=frontier.removeFirst();
            var next=candidates.stream().filter(target->!visited.contains(target)&&target.position().distanceToSqr(from)<=10000)
                    .sorted(Comparator.comparingDouble(target->target.position().distanceToSqr(from))).limit(10).toList();
            for(var target:next){
                if(!visited.add(target))continue;frontier.addLast(target.position());sparks(level,from,target.position());
                if(game.items().fpvDrone().tryShortCircuit(target))continue;
                var damage=game.items().indirectKills().captureDamage(new DamageSource(level.registryAccess()
                        .lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),owner),cause);
                game.items().indirectKills().withCause(target,cause,()->target.hurtServer(level,damage,16));
                level.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH,0xff0000ff),target.getX(),target.getY(),target.getZ(),1,0,0,0,0);
                if(projectile==null)game.items().wands().chainStatusElectro(emitter,owner,target.position(),cause);else game.items().wands().arrowChainElectro(projectile,owner,target.position(),cause);
            }
        }
    }
    private static void sparks(ServerLevel level,Vec3 from,Vec3 to){
        double length=from.distanceTo(to);if(length<1e-6)return;var direction=to.subtract(from).scale(1/length);
        for(double distance=0;distance<=length;distance+=.5){var point=from.add(direction.scale(distance));
            if(!level.hasChunkAt(net.minecraft.core.BlockPos.containing(point)))break;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,point.x,point.y+1.2,point.z,1,0,0,0,0);
        }
    }
    void clear(LivingEntity target){
        callbackDepth++;try{pending.remove(target);target.removeAttached(DATA);if(game.items().mana().hudValue(target,COUNTER)!=0)game.items().mana().score(target,COUNTER).set(0);
        }finally{callbackDepth--;}
    }
    private void sync(LivingEntity target){
        game.items().mana().score(target,COUNTER).set(queued(target));var tag=new CompoundTag();tag.putString("epoch",game.items().epoch());var list=new ListTag();
        for(var contribution:pending.get(target)){var row=new CompoundTag();row.putInt("remaining",contribution.remaining);var cause=contribution.cause;
            if(cause!=null){row.putString("owner",cause.player().toString());row.putLong("order",cause.order());}list.add(row);}
        tag.put("contributions",list);target.setAttached(DATA,tag);
    }
    void loaded(LivingEntity target){
        callbackDepth++;try{
        if(rewinding)return;
        if(immune(target)){clear(target);return;}
        var tag=target.getAttached(DATA);if(tag==null){observe(target);return;}
        if(!game.items().currentEpoch(tag.getStringOr("epoch",""))){clear(target);return;}
        var queue=new ArrayDeque<Contribution>();for(var value:tag.getListOrEmpty("contributions"))if(value instanceof CompoundTag row){
            LbcIndirectKills.Owner cause=null;try{var restored=game.items().indirectKills().restoreColdCause(UUID.fromString(row.getStringOr("owner","")),row.getLongOr("order",0),false);cause=new LbcIndirectKills.Owner(restored.player(),restored.order(),true,false);}catch(IllegalArgumentException ignored){}
            int remaining=row.getIntOr("remaining",0);if(remaining>0)queue.addLast(new Contribution(remaining,cause));
        }
        if(queue.isEmpty())clear(target);else{pending.put(target,queue);sync(target);}

        }finally{callbackDepth--;}
    }
    void unloaded(LivingEntity target){
        callbackDepth++;try{
        if(rewinding)return;if(!target.isAlive())clear(target);else pending.remove(target);
        }finally{callbackDepth--;}
    }
    void forget(ServerPlayer owner){
        callbackDepth++;try{for(var target:List.copyOf(pending.keySet()))if(target==owner||pending.get(target).stream().anyMatch(c->c.cause!=null&&c.cause.player().equals(owner.getUUID())))clear(target);
        }finally{callbackDepth--;}
    }
    void stop(){
        callbackDepth++;try{rewinding=false;for(var target:List.copyOf(pending.keySet()))clear(target);
        }finally{callbackDepth--;}
    }
    void checkpointBoundary(){if(callbackDepth!=0)throw new IllegalStateException("Finish native chain-status admission/hit/load callbacks before checkpointing");}
    void prepareFrozen(){checkpointBoundary();rewinding=true;}
    void publishFrozen(){checkpointBoundary();rewinding=false;}
    void cancelFrozen(){checkpointBoundary();rewinding=false;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){return new Checkpoint(links);}
    final class Checkpoint {
        private record Amount(int remaining,LbcIndirectKills.Owner cause) { }
        private record Queue(MatchDamageSources.Ref target,List<Amount> amounts) { }
        private final List<Queue> targets=new ArrayList<>();
        Checkpoint(MatchSpellCheckpoint.Links links){
            checkpointBoundary();if(rewinding)throw new IllegalStateException("Publish native chain-status bodies before capture");
            pending.forEach((target,queue)->targets.add(new Queue(links.key(target),queue.stream().map(c->new Amount(c.remaining,c.cause)).toList())));
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var restored=new LinkedHashMap<LivingEntity,ArrayDeque<Contribution>>();
            for(var image:targets){var target=(LivingEntity)links.get(image.target);var queue=new ArrayDeque<Contribution>();
                for(var amount:image.amounts)queue.addLast(new Contribution(amount.remaining,amount.cause));restored.put(target,queue);}
            // Native effects, HUD scores and persistent DATA attachments have their canonical native owner.
            // Never call accept/loaded/observe/sync: these paths grant new causal order or rewrite native state.
            return ()->{pending.clear();pending.putAll(restored);};
        }
    }

}
