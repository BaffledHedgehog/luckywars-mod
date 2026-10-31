package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Only Gate of Babylon's three arrow transports; each FIFO contribution retains its original cause. */
public final class LbcBabylonEffects {
    private static final String[] COUNTERS={"effect_small_explosion","effect_lightning_bolt","effect_firebreak"};
    private static final int[] AMPS={101,109,110};
    private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<CompoundTag> DATA=
        net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(Identifier.parse("skywars2:babylon_effects"),b->b.persistent(CompoundTag.CODEC));
    private final GameController game;
    private final LbcMana mana;
    private final Map<LivingEntity,List<ArrayDeque<Contribution>>> pending=new LinkedHashMap<>();
    private final Map<Entity,LbcIndirectKills.Owner> secondary=new WeakHashMap<>();
    private final Set<LivingEntity> retainedTargets=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final class Contribution {
        int remaining; final LbcIndirectKills.Owner cause;
        Contribution(int ticks,LbcIndirectKills.Owner cause){remaining=ticks;this.cause=cause;}
    }
    public static final class OwnerDamageSource extends DamageSource {
        final LbcIndirectKills.Owner cause;
        final String epoch;
        OwnerDamageSource(ServerLevel level,Entity direct,ServerPlayer owner,LbcIndirectKills.Owner cause,String epoch){
            super(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.PLAYER_EXPLOSION),direct,owner);this.cause=cause;this.epoch=epoch;
        }
    }
    public LbcBabylonEffects(GameController game,LbcMana mana){this.game=game;this.mana=mana;}
    private static int index(int amp){for(int i=0;i<AMPS.length;i++)if(AMPS[i]==amp)return i;return -1;}
    public static boolean isEffect(MobEffectInstance effect){return effect.is(MobEffects.RAID_OMEN)&&index(effect.getAmplifier())>=0;}
    private static List<ArrayDeque<Contribution>> queues(){return List.of(new ArrayDeque<>(),new ArrayDeque<>(),new ArrayDeque<>());}
    public boolean accept(LivingEntity target,Projectile arrow,MobEffectInstance effect){
        if(!isEffect(effect)||!LbcBabylon.managed(arrow)||!game.items().currentEpoch(arrow)
                ||target.entityTags().contains("sin_greed_active")||!valid(target)||!target.canBeAffected(effect)
                ||target instanceof ServerPlayer p&&game.items().magic().rejectCustomEffect(p))return false;
        var owner=LbcIndirectKills.playerSource(arrow);if(owner==null)return false;
        if(skillImmune(target)){clear(target);return true;}
        int duration=effect.getDuration();if(duration==2)duration=1;if(duration<=0)return true;
        var cause=game.items().indirectKills().coldCause(owner,true);
        cause=new LbcIndirectKills.Owner(cause.player(),cause.order(),true,true);
        int kind=index(effect.getAmplifier());pending.computeIfAbsent(target,k->queues()).get(kind).addLast(new Contribution(duration,cause));
        sync(target);return true;
    }
    /** Both ordinary Quiver and Babylon 101 feed the same source counter, retaining their own policy. */
    public boolean acceptQuiver101(LivingEntity target,MobEffectInstance effect,net.minecraft.world.entity.projectile.arrow.Arrow arrow){
        if(effect.getAmplifier()!=101||!isEffect(effect)||!game.items().quiver().validEffectArrow(arrow,101)
                ||target.entityTags().contains("sin_greed_active")||!valid(target)||!target.canBeAffected(effect)
                ||target instanceof ServerPlayer p&&game.items().magic().rejectCustomEffect(p))return false;
        var owner=LbcIndirectKills.playerSource(arrow);if(owner==null)return false;
        if(skillImmune(target)){clear(target);return true;}
        int duration=effect.getDuration();if(duration==2)duration=1;if(duration<=0)return true;
        pending.computeIfAbsent(target,k->queues()).get(0).addLast(new Contribution(duration,
                game.items().indirectKills().coldCause(owner,true)));
        sync(target);return true;
    }
    private boolean valid(LivingEntity target){return game.playing()&&target.level()==game.maps().level()&&game.items().wands().validTarget(target);}
    private boolean skillImmune(LivingEntity target){return mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("#lbcskill"),"swrg.math").get()==1 && mana.score(target,"lbc.skill").get()==19;}
    /** Legacy in-ground arrows fire once, independent of living-target duration queues. */
    void blockImpact(Projectile arrow,int amp,Vec3 at){
        int kind=index(amp);var owner=LbcIndirectKills.playerSource(arrow);
        if(kind<0||owner==null||!LbcBabylon.managed(arrow)||!game.items().currentEpoch(arrow)
                ||!game.playing()||arrow.level()!=game.maps().level()||!arrow.level().hasChunkAt(BlockPos.containing(at))
                ||!game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped((ServerLevel)arrow.level())
                ||game.items().timeStop().frozen(arrow)||arrow.entityTags().contains("time_stopped"))return;
        var level=(ServerLevel)arrow.level();
        if(game.items().magic().luckyEffectWarded(level,at)
                ||(kind==0?game.items().magic().mechanismSuppressedAt(level,at):game.items().magic().suppressedAt(level,at)))return;
        var cause=game.items().indirectKills().coldCause(owner,true);
        actAt(level,at,kind,new LbcIndirectKills.Owner(cause.player(),cause.order(),true,true));
    }
    public void tick(){for(var target:List.copyOf(pending.keySet()))tickTarget(target);}
    public void tickTarget(LivingEntity target){
        if(retainedTargets.contains(target))return;
        if(game.items().polymorph().masked(target))return;
        var all=pending.get(target);if(all==null)return;
        if(target.entityTags().contains("sin_greed_active"))return;
        if(!valid(target)||skillImmune(target)||target instanceof ServerPlayer p&&game.items().magic().rejectCustomEffect(p)){clear(target);return;}
        if(!game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped((ServerLevel)target.level())||game.items().timeStop().frozen(target)
                ||target.entityTags().contains("time_stopped")||game.items().frost().isFrozen(target)||target.entityTags().contains("sin_greed_active"))return;
        for(int kind=0;kind<3;kind++){
            var queue=all.get(kind);var next=queue.peekFirst();if(next==null)continue;
            var level=(ServerLevel)target.level();
            boolean ward=game.items().magic().luckyEffectWarded(level,target.position());
            boolean suppressed=kind==0?game.items().magic().mechanismSuppressedAt(level,target.position()):game.items().magic().suppressedAt(level,target.position());
            if(!ward&&!suppressed)act(target,kind,next.cause);
            if(--next.remaining<=0)queue.removeFirst();
        }
        if(all.stream().allMatch(Deque::isEmpty))clear(target);else sync(target);
    }
    private void act(LivingEntity target,int kind,LbcIndirectKills.Owner cause){
        actAt((ServerLevel)target.level(),target.position(),kind,cause);
    }
    void quiverExplosionAt(ServerLevel level,Vec3 at,LbcIndirectKills.Owner cause){actAt(level,at,0,cause);}
    /** Ordinary explosive darts retain native TNT strength and two-tick fuse in the shared child lifecycle. */
    void primeExplosionAt(ServerLevel level,Vec3 at,LbcIndirectKills.Owner cause){
        var owner=game.server().getPlayerList().getPlayer(cause.player());
        var tnt=new net.minecraft.world.entity.item.PrimedTnt(level,at.x,at.y,at.z,owner);tnt.setFuse(2);
        inheritChild(cause,tnt);level.addFreshEntity(tnt);
    }
    /** Source bigexplode has seven native MOB explosions, sharing accepted WILD damage and terrain ownership. */
    void bigExplosionAt(ServerLevel level,Vec3 at,LbcIndirectKills.Owner cause){
        var owner=game.server().getPlayerList().getPlayer(cause.player());
        for(var offset:List.of(Vec3.ZERO,new Vec3(0,0,4),new Vec3(0,0,-4),new Vec3(-4,0,0),new Vec3(4,0,0),new Vec3(0,-4,0),new Vec3(0,4,0))){
            var point=at.add(offset);if(!level.hasChunkAt(BlockPos.containing(point)))continue;
            var damage=new OwnerDamageSource(level,null,owner,cause,game.items().epoch());
            game.items().indirectKills().trapChanges(level,cause,()->level.explode(null,damage,null,point,10,false,Level.ExplosionInteraction.MOB));
        }
    }
    private void actAt(ServerLevel level,Vec3 at,int kind,LbcIndirectKills.Owner cause){
        var owner=game.server().getPlayerList().getPlayer(cause.player());
        if(owner==null&&!cause.friendlyFire())return;
        if(kind==0){
            var damage=new OwnerDamageSource(level,null,owner,cause,game.items().epoch());
            game.items().indirectKills().trapChanges(level,cause,()->level.explode(null,damage,null,at,1,false,Level.ExplosionInteraction.MOB));
            return;
        }
        if(kind==1){
            var bolt=new LightningBolt(EntityTypes.LIGHTNING_BOLT,level);bolt.setPos(at);if(owner!=null)bolt.setCause(owner);
            secondary.put(bolt,cause);bolt.entityTags().add("skywars2.babylon.effect");game.items().markCurrentItemEpoch(bolt);level.addFreshEntity(bolt);return;
        }
        var lo=BlockPos.containing(at.x-3,at.y-3,at.z-3);var hi=BlockPos.containing(at.x+3,at.y+3,at.z+3);
        game.items().indirectKills().withSupportCandidates(level,new AABB(Vec3.atLowerCornerOf(lo),Vec3.atLowerCornerOf(hi.offset(1,1,1))),()->game.items().indirectKills().trapChanges(level,cause,()->{
            for(var pos:BlockPos.betweenClosed(lo,hi))if(level.hasChunkAt(pos)&&level.getBlockState(pos).isAir())level.setBlock(pos,Blocks.FIRE.defaultBlockState(),3);
            var innerLo=BlockPos.containing(at.x-1.5,at.y-1.5,at.z-1.5);var innerHi=BlockPos.containing(at.x+1.5,at.y+1.5,at.z+1.5);
            for(var pos:BlockPos.betweenClosed(innerLo,innerHi))if(level.hasChunkAt(pos))level.destroyBlock(pos,true);
        }));
    }
    public LbcIndirectKills.Owner cause(Entity entity){
        var saved=secondary.get(entity);if(saved==null)return null;
        var author=owner(entity);
        if(author!=null&&!author.getUUID().equals(saved.player())){
            var changed=game.items().indirectKills().coldCause(author,saved.projectile());
            saved=new LbcIndirectKills.Owner(changed.player(),changed.order(),saved.friendlyFire(),changed.projectile());secondary.put(entity,saved);
        }return saved;
    }
    public ServerPlayer owner(Entity entity){
        var saved=secondary.get(entity);if(saved==null)return null;
        if(game.items().electro().reflectedOwner(entity) instanceof ServerPlayer player)return player;
        if(entity.isRemoved())return game.server().getPlayerList().getPlayer(saved.player());
        if(entity instanceof net.minecraft.world.entity.item.PrimedTnt tnt&&tnt.getOwner() instanceof ServerPlayer player)return player;
        return game.server().getPlayerList().getPlayer(saved.player());
    }
    public boolean canAffect(Entity source,Entity victim){
        var saved=cause(source);if(saved==null)return true;
        var author=owner(source);
        return game.items().wands().validTarget(victim)&&(saved.friendlyFire()
                ||author!=null&&(victim==author||game.items().wands().projectileTarget(author,victim)));
    }
    public void inheritChild(Entity source,Entity child){inheritChild(cause(source),child);}
    public void inheritExplosionChild(DamageSource source,Entity child){
        if(source instanceof OwnerDamageSource)inheritChild(game.items().indirectKills().babylonCause(source),child);
    }
    private void inheritChild(LbcIndirectKills.Owner cause,Entity child){
        if(cause==null||!(child instanceof net.minecraft.world.entity.item.PrimedTnt))return;
        secondary.put(child,cause);child.entityTags().add("skywars2.babylon.effect");game.items().markCurrentItemEpoch(child);
        game.items().timeStop().prepareFrozen(child);
    }
    public boolean managedSecondary(Entity entity){return secondary.containsKey(entity);}
    public boolean currentSource(DamageSource source){return source instanceof OwnerDamageSource owned&&game.items().currentEpoch(owned.epoch);}
    public ServerPlayer explicitOwner(DamageSource source){return source instanceof OwnerDamageSource owned
            ?game.server().getPlayerList().getPlayer(owned.cause.player()):null;}
    public int queued(LivingEntity target,int amp){var all=pending.get(target);int kind=index(amp);return all==null||kind<0?0:all.get(kind).stream().mapToInt(c->c.remaining).sum();}
    public int pendingCount(){return pending.size();}
    private void sync(LivingEntity target){
        var tag=new CompoundTag();tag.putString("epoch",game.items().epoch());var entries=new ListTag();
        for(int i=0;i<3;i++){mana.score(target,COUNTERS[i]).set(queued(target,AMPS[i]));for(var c:pending.get(target).get(i)){
            var entry=new CompoundTag();entry.putInt("amp",AMPS[i]);entry.putInt("ticks",c.remaining);entry.putString("owner",c.cause.player().toString());entry.putLong("order",c.cause.order());entry.putBoolean("wild",c.cause.friendlyFire());entry.putBoolean("projectile",c.cause.projectile());entries.add(entry);
        }}tag.put("entries",entries);target.setAttached(DATA,tag);
    }
    public void clear(LivingEntity target){retainedTargets.remove(target);pending.remove(target);target.removeAttached(DATA);for(var name:COUNTERS)mana.score(target,name).set(0);}
    public void loaded(Entity entity){
        if(entity.entityTags().contains("skywars2.babylon.effect")&&(!secondary.containsKey(entity)||!game.items().currentEpoch(entity))){entity.discard();return;}
        if(!(entity instanceof LivingEntity target))return;var tag=target.getAttached(DATA);if(tag==null)return;
        if(!game.items().currentEpoch(tag.getStringOr("epoch",""))){clear(target);return;}
        var all=queues();for(var value:tag.getListOrEmpty("entries"))if(value instanceof CompoundTag entry)try{
            int kind=index(entry.getIntOr("amp",0)),ticks=entry.getIntOr("ticks",0);if(kind<0||ticks<=0)continue;
            var restored=game.items().indirectKills().restoreColdCause(UUID.fromString(entry.getStringOr("owner","")),entry.getLongOr("order",0),entry.getBooleanOr("projectile",true));
            all.get(kind).add(new Contribution(ticks,new LbcIndirectKills.Owner(restored.player(),restored.order(),entry.getBooleanOr("wild",true),restored.projectile())));
        }catch(IllegalArgumentException ignored){}
        if(all.stream().allMatch(Deque::isEmpty)){clear(target);return;}pending.put(target,all);sync(target);
    }
    public void unloaded(Entity entity){if(entity instanceof LivingEntity target)pending.remove(target);
        // Native TNT discards before explode(); its final explosion still needs the saved cause.
        if(!(entity instanceof net.minecraft.world.entity.item.PrimedTnt&&entity.isRemoved()))secondary.remove(entity);
    }
    public void forget(ServerPlayer player){clear(player);}
    public void stop(){for(var target:List.copyOf(pending.keySet()))clear(target);for(var entity:List.copyOf(secondary.keySet()))if(!entity.isRemoved())entity.discard();secondary.clear();retainedTargets.clear();}
    void adoptRawFoodStatuses(ServerPlayer player){
        if(!valid(player)||skillImmune(player)||game.items().magic().rejectCustomEffect(player))return;
        int[] desired=new int[3];for(int i=0;i<3;i++)desired[i]=mana.hudValue(player,COUNTERS[i]);
        for(int i=0;i<3;i++){long delta=(long)desired[i]-queued(player,AMPS[i]);if(delta>0)
            pending.computeIfAbsent(player,k->queues()).get(i).addLast(new Contribution((int)Math.min(Integer.MAX_VALUE,delta),game.items().indirectKills().wildCause(player)));}
        if(pending.containsKey(player))sync(player);
    }
    public void addPlayerCounter(ServerPlayer player,String name){for(int i=0;i<3;i++)if(COUNTERS[i].equals(name)){mana.score(player,name).set(queued(player,AMPS[i]));return;}}

    ResidentCheckpoint residentCheckpoint(MatchSpellCheckpoint.Links links){return new ResidentCheckpoint(links);}
    void prepareResidentFrozen(){pending.clear();secondary.clear();retainedTargets.clear();}
    final class ResidentCheckpoint {
        private record ContributionImage(int remaining,LbcIndirectKills.Owner cause){}
        private record TargetImage(MatchDamageSources.Ref body,List<List<ContributionImage>> queues){}
        private final List<TargetImage> targets=new ArrayList<>();
        private final Map<MatchDamageSources.Ref,LbcIndirectKills.Owner> children=new LinkedHashMap<>();
        ResidentCheckpoint(MatchSpellCheckpoint.Links links){
            pending.forEach((body,all)->targets.add(new TargetImage(links.key(body),all.stream()
                    .map(queue->queue.stream().map(c->new ContributionImage(c.remaining,c.cause)).toList()).toList())));
            secondary.forEach((body,cause)->{if(!body.isRemoved()&&!links.omitted(body))children.put(links.key(body),cause);});
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            var next=new LinkedHashMap<LivingEntity,List<ArrayDeque<Contribution>>>();var offline=Collections.newSetFromMap(new IdentityHashMap<LivingEntity,Boolean>());
            for(var target:targets){var body=(LivingEntity)links.get(target.body);var all=queues();
                for(int kind=0;kind<3;kind++)for(var row:target.queues.get(kind))all.get(kind).add(new Contribution(row.remaining,row.cause));
                next.put(body,all);if(MatchOfflinePlayers.retained(body))offline.add(body);}
            var nextChildren=new WeakHashMap<Entity,LbcIndirectKills.Owner>();children.forEach((ref,cause)->nextChildren.put(links.get(ref),cause));
            return ()->{pending.clear();pending.putAll(next);secondary.clear();secondary.putAll(nextChildren);retainedTargets.clear();retainedTargets.addAll(offline);};
        }
    }

}
