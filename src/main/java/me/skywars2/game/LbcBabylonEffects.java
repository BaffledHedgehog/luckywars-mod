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
    private static final class Contribution {
        int remaining; final LbcIndirectKills.Owner cause;
        Contribution(int ticks,LbcIndirectKills.Owner cause){remaining=ticks;this.cause=cause;}
    }
    public static final class OwnerDamageSource extends DamageSource {
        final LbcIndirectKills.Owner cause;
        OwnerDamageSource(ServerLevel level,Entity direct,ServerPlayer owner,LbcIndirectKills.Owner cause){
            super(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.PLAYER_EXPLOSION),direct,owner);this.cause=cause;
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
    private void actAt(ServerLevel level,Vec3 at,int kind,LbcIndirectKills.Owner cause){
        var owner=game.server().getPlayerList().getPlayer(cause.player());
        if(kind==0){
            var carrier=new net.minecraft.world.entity.monster.Creeper(EntityTypes.CREEPER,level);carrier.setPos(at);secondary.put(carrier,cause);
            var damage=new OwnerDamageSource(level,carrier,owner,cause);
            game.items().indirectKills().trapChanges(level,cause,()->level.explode(carrier,damage,null,at,1,false,Level.ExplosionInteraction.MOB));
            carrier.discard();return;
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
    public LbcIndirectKills.Owner cause(Entity entity){return secondary.get(entity);}
    public boolean managedSecondary(Entity entity){return secondary.containsKey(entity);}
    public int queued(LivingEntity target,int amp){var all=pending.get(target);int kind=index(amp);return all==null||kind<0?0:all.get(kind).stream().mapToInt(c->c.remaining).sum();}
    public int pendingCount(){return pending.size();}
    private void sync(LivingEntity target){
        var tag=new CompoundTag();tag.putString("epoch",game.items().epoch());var entries=new ListTag();
        for(int i=0;i<3;i++){mana.score(target,COUNTERS[i]).set(queued(target,AMPS[i]));for(var c:pending.get(target).get(i)){
            var entry=new CompoundTag();entry.putInt("amp",AMPS[i]);entry.putInt("ticks",c.remaining);entry.putString("owner",c.cause.player().toString());entry.putLong("order",c.cause.order());entries.add(entry);
        }}tag.put("entries",entries);target.setAttached(DATA,tag);
    }
    public void clear(LivingEntity target){pending.remove(target);target.removeAttached(DATA);for(var name:COUNTERS)mana.score(target,name).set(0);}
    public void loaded(Entity entity){
        if(!(entity instanceof LivingEntity target))return;var tag=target.getAttached(DATA);if(tag==null)return;
        if(!game.items().currentEpoch(tag.getStringOr("epoch",""))){clear(target);return;}
        var all=queues();for(var value:tag.getListOrEmpty("entries"))if(value instanceof CompoundTag entry)try{
            int kind=index(entry.getIntOr("amp",0)),ticks=entry.getIntOr("ticks",0);if(kind<0||ticks<=0)continue;
            var restored=game.items().indirectKills().restoreColdCause(UUID.fromString(entry.getStringOr("owner","")),entry.getLongOr("order",0),true);
            all.get(kind).add(new Contribution(ticks,new LbcIndirectKills.Owner(restored.player(),restored.order(),true,true)));
        }catch(IllegalArgumentException ignored){}
        if(all.stream().allMatch(Deque::isEmpty)){clear(target);return;}pending.put(target,all);sync(target);
    }
    public void unloaded(Entity entity){if(entity instanceof LivingEntity target)pending.remove(target);secondary.remove(entity);}
    public void forget(ServerPlayer player){clear(player);}
    public void stop(){for(var target:List.copyOf(pending.keySet()))clear(target);for(var entity:List.copyOf(secondary.keySet()))if(!entity.isRemoved())entity.discard();secondary.clear();}
    public void addPlayerCounter(ServerPlayer player,String name){for(int i=0;i<3;i++)if(COUNTERS[i].equals(name)){mana.score(player,name).set(queued(player,AMPS[i]));return;}}
}
