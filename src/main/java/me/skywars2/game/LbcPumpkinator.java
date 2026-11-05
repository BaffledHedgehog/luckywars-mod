package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Persistent source marker state, without a ticking marker entity or entity-data polling. */
public final class LbcPumpkinator {
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private final GameController game;
    private final NavigableMap<Long,Actor> actors=new TreeMap<>();
    private long sequence,cloneCursor;
    private int lastTick=Integer.MIN_VALUE,callbacks;
    private boolean restoring;

    private static final class Actor {
        final long id;final UUID author;final ServerLevel level;final String epoch;final LbcIndirectKills.Owner cause;
        Vec3 point,aim;long random,pulses,explosions,writes;int mask,lastStep;
        Actor(long id,UUID author,ServerLevel level,String epoch,LbcIndirectKills.Owner cause,Vec3 point,long random,int tick){
            this.id=id;this.author=author;this.level=level;this.epoch=epoch;this.cause=cause;this.point=point;this.random=random;
            aim=Vec3.directionFromRotation(0,0);lastStep=tick;
        }
    }
    public LbcPumpkinator(GameController game){this.game=game;}
    public static boolean matches(ItemStack stack){
        if(stack.isEmpty())return false;var data=stack.get(DataComponents.ENTITY_DATA);
        if(data==null||data.type()!=EntityTypes.MARKER)return false;
        var tags=data.getUnsafe().getListOrEmpty("Tags");
        return tags.contains(StringTag.valueOf("pumpkinator"))&&tags.contains(StringTag.valueOf("rare"))&&tags.contains(StringTag.valueOf("slowraycast"));
    }
    private boolean suppressed(ServerLevel level,Vec3 point){return game.items().magic().suppressedAt(level,point);}
    private boolean paused(ServerLevel level){return game.items().wands().timeStopped()||game.items().timeStop().stopped(level);}
    public boolean use(ServerPlayer owner,InteractionHand hand,Vec3 markerPosition){
        var level=owner.level();var stack=owner.getItemInHand(hand);
        if(restoring||!matches(stack)||!game.playing()||game.stage()==GameController.Stage.ENDING||game.maps().busy()
                ||!owner.isAlive()||owner.isRemoved()||owner.isSpectator()||game.isObserver(owner)||!game.canUseGameplayItems(owner)
                ||game.awaitingRespawn(owner.getUUID())||!game.items().wands().validRoleTarget(owner,false)
                ||!LbcDimensionItems.ordinary(level)||!LbcBedwarsRoleGuards.publication(level)
                ||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner)||paused(level)
                ||game.items().timeStop().frozen(owner)||game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()
                ||!level.hasChunkAt(BlockPos.containing(markerPosition))||!level.isInValidBounds(BlockPos.containing(markerPosition))
                ||suppressed(level,owner.position())||suppressed(level,owner.getEyePosition())||suppressed(level,markerPosition)||suppressed(level,dispatchOrigin(markerPosition)))return false;
        var actor=new Actor(++sequence,owner.getUUID(),level,game.items().epoch(),game.items().indirectKills().wildCause(owner),markerPosition,owner.getRandom().nextLong(),game.server().getTickCount());
        actor.aim=owner.getLookAngle();actors.put(actor.id,actor);
        // Source eggs consume the actual hand once, including the ordinary creative exception.
        stack.consume(1,owner);owner.inventoryMenu.broadcastChanges();return true;
    }
    /** The dispatch lowers only the command origin. Pos edits below retain the marker's original Y. */
    static Vec3 dispatchOrigin(Vec3 markerPosition){return markerPosition.add(0,-1.5,0);}
    static Vec3 wander(Vec3 markerPosition,int dx,int dz){return new Vec3(Math.floor(markerPosition.x)+dx,markerPosition.y,Math.floor(markerPosition.z)+dz);}
    private static long mix(long value){value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;value=(value^(value>>>27))*0x94d049bb133111ebL;return value^(value>>>31);}
    private static int random(Actor actor,int bound){
        // A saved independent stream prevents rewinding a marker from advancing global world RNG.
        long bits;long value;do{actor.random+=0x9e3779b97f4a7c15L;bits=mix(actor.random)>>>1;value=bits%bound;}while(bits-value+(bound-1)<0);
        return (int)value;
    }
    static boolean explosionSeed(long seed){return (mix(seed+0x9e3779b97f4a7c15L)>>>1)%100==0;}
    public void tick(){
        int tick=game.server().getTickCount();
        if(restoring||lastTick==tick||!game.playing()||game.maps().busy()||game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally())return;
        lastTick=tick;callbacks++;
        try{for(var actor:List.copyOf(actors.values())){
            if(!game.items().currentEpoch(actor.epoch)){actors.remove(actor.id);continue;}
            if(!actor.level.hasChunkAt(BlockPos.containing(actor.point)))continue;
            if(paused(actor.level)||actor.lastStep==tick||!LbcBedwarsRoleGuards.publication(actor.level))continue;
            // MAG and the enhanced artifact destroy accepted actors rather than pausing growth.
            if(suppressed(actor.level,actor.point)||suppressed(actor.level,dispatchOrigin(actor.point))){actors.remove(actor.id);continue;}
            actor.lastStep=tick;pulse(actor);
        }}finally{callbacks--;}
    }
    private void pulse(Actor actor){
        var origin=dispatchOrigin(actor.point);var center=BlockPos.containing(origin);
        fill(actor,center.offset(-1,-1,-1),center.offset(1,1,1),Blocks.PUMPKIN.defaultBlockState());
        fill(actor,center.offset(-2,0,-2),center.offset(2,0,2),Blocks.PUMPKIN.defaultBlockState());
        actor.pulses++;
        if(random(actor,100)==0)explode(actor,origin,center);
        actor.point=wander(actor.point,random(actor,5)-2,random(actor,5)-2);
    }
    private void fill(Actor actor,BlockPos first,BlockPos last,BlockState wanted){
        for(var pos:BlockPos.betweenClosed(first,last))
            if(game.items().writeOwnedTerrain(actor.level,pos,wanted,actor.cause))actor.writes++;
    }
    private void explode(Actor actor,Vec3 origin,BlockPos center){
        actor.explosions++;
        var targets=actor.level.getEntitiesOfClass(LivingEntity.class,new AABB(origin,origin).inflate(5),
                target->target.getType().builtInRegistryHolder().is(MOBS)&&target.position().distanceToSqr(origin)<=25
                        &&game.items().wands().validRoleTarget(target,false)&&!game.items().polymorph().concealed(target)
                        &&!suppressed(actor.level,target.position()));
        for(var target:targets){
            var source=game.items().indirectKills().captureDamage(new DamageSource(actor.level.damageSources().cactus().typeHolder(),origin),actor.cause);
            target.hurtServer(actor.level,source,10);
        }
        actor.level.sendParticles(ParticleTypes.LAVA,origin.x,origin.y+2,origin.z,50,.5,2,.5,1);
        actor.level.playSound(null,origin.x,origin.y,origin.z,SoundEvents.GENERIC_EXPLODE.value(),SoundSource.MASTER,1,1.5f);
        fill(actor,center.offset(-1,2,0),center.offset(1,2,0),Blocks.PUMPKIN.defaultBlockState());
        fill(actor,center.offset(0,3,0),center.offset(0,6,0),Blocks.PUMPKIN.defaultBlockState());
        fill(actor,center.offset(0,7,0),center.offset(0,7,0),Blocks.CARVED_PUMPKIN.defaultBlockState());
    }
    public record ActorView(long id,UUID author,LbcIndirectKills.Owner cause,ServerLevel level,Vec3 position,int mask,long pulses,long explosions,long rngState,Vec3 aim,long writes){}
    public int count(){return actors.size();}
    public List<ActorView> actorViews(int limit){var result=new ArrayList<ActorView>();for(var actor:actors.values()){
        if(result.size()>=Math.max(0,limit))break;result.add(new ActorView(actor.id,actor.author,actor.cause,actor.level,actor.point,actor.mask,actor.pulses,actor.explosions,actor.random,actor.aim,actor.writes));
    }return List.copyOf(result);}
    public void planetoid(ServerPlayer holder){
        if(restoring)return;for(var actor:actors.values())if(!paused(actor.level)&&LbcPlanetoidSkill.near(game,holder,actor.level,actor.point))
            actor.aim=LbcPlanetoidSkill.direction(holder,actor.point,82);
    }
    /** A source marker travels with native map transforms; the lowered dispatch remains relative. */
    void transformOrigins(ServerLevel level,java.util.function.UnaryOperator<Vec3> transform){
        checkpointBoundary();callbacks++;try{actors.values().removeIf(actor->{
            if(actor.level!=level)return false;var point=transform.apply(actor.point);
            if(point==null)return true;actor.point=point;return false;
        });}finally{callbacks--;}
    }
    record CloneResult(int scanned,int operations,int bodies){}
    CloneResult cloneNearby(ServerPlayer holder,int skill,int scanBudget,int bodyBudget,long deadline){
        int count=CloneVirtualAbilityStates.count(skill),seen=0,made=0,operations=0;
        if(restoring||skill!=15&&skill!=17||bodyBudget<count)return new CloneResult(0,0,0);
        int available=Math.min(scanBudget,actors.size());long accepted=sequence;
        while(seen<available&&!actors.isEmpty()&&bodyBudget-made>=count&&System.nanoTime()<deadline){
            var entry=actors.higherEntry(cloneCursor);if(entry==null||entry.getKey()>accepted)entry=actors.firstEntry();cloneCursor=entry.getKey();seen++;var source=entry.getValue();
            if(!game.items().currentEpoch(source.epoch)||paused(source.level)||!CloneVirtualAbilityStates.near(game,holder,source.level,source.point,source.mask,skill))continue;
            var positions=CloneVirtualAbilityStates.positions(holder,source.level,source.point,skill);if(positions==null)continue;
            int flags=source.mask|CloneVirtualAbilityStates.bit(skill);
            for(var point:positions){var actor=new Actor(++sequence,source.author,source.level,source.epoch,source.cause,point,source.random,source.lastStep);
                actor.aim=source.aim;actor.pulses=source.pulses;actor.explosions=source.explosions;actor.writes=source.writes;actor.mask=flags;actors.put(actor.id,actor);}
            source.mask=flags;made+=count;operations++;
        }return new CloneResult(seen,operations,made);
    }
    public void checkpointBoundary(){if(callbacks!=0)throw new IllegalStateException("Finish Pumpkinator callbacks before checkpoint");}
    public void stop(){checkpointBoundary();actors.clear();sequence=cloneCursor=0;lastTick=Integer.MIN_VALUE;restoring=false;}
    public void prepareFrozen(){checkpointBoundary();restoring=true;actors.clear();}
    public void publishFrozen(){checkpointBoundary();restoring=false;lastTick=game.server().getTickCount();}
    public void cancelFrozen(){stop();}
    MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){
        checkpointBoundary();record Saved(Actor actor,Vec3 point,Vec3 aim,long random,long pulses,long explosions,long writes,int mask){}
        var saved=actors.values().stream().map(a->new Saved(a,a.point,a.aim,a.random,a.pulses,a.explosions,a.writes,a.mask)).toList();long ids=sequence,cursor=cloneCursor;
        return resolved->{checkpointBoundary();for(var image:saved)MatchAuthorityCheckpoint.world(game,image.actor.level);
            return ()->{actors.clear();for(var image:saved){var actor=image.actor;actor.point=image.point;actor.aim=image.aim;actor.random=image.random;actor.pulses=image.pulses;actor.explosions=image.explosions;actor.writes=image.writes;actor.mask=image.mask;actor.lastStep=game.server().getTickCount();actors.put(actor.id,actor);}sequence=ids;cloneCursor=cursor;};};
    }
}
