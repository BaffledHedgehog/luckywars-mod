package me.skywars2.game;

import java.util.*;
import java.util.function.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;

/** Source player white-arrow markers; the boss and player use the same precise native ray step. */
public final class LbcWhiteArrows {
    @FunctionalInterface public interface Policy {boolean activation(ServerPlayer author,InteractionHand hand,Vec3 origin);}
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private final GameController game;private final Policy policy;
    private final NavigableMap<Long,Shot> shots=new TreeMap<>();
    private long sequence,turn,cloneCursor,launched,steps,kills;private boolean restoring;
    private int lastTick=Integer.MIN_VALUE,callbacks;
    private static final class Shot {
        final long id;ServerPlayer author;final ServerLevel level;final String epoch,team;
        final LbcSpellSource.Source source;Vec3 point,direction;int age,mask,lastStepTick=Integer.MIN_VALUE;
        Shot(long id,ServerPlayer author,ServerLevel level,Vec3 point,Vec3 direction,String epoch,String team,LbcSpellSource.Source source){this.id=id;this.author=author;this.level=level;this.point=point;this.direction=direction;this.epoch=epoch;this.team=team;this.source=source;}
    }
    public LbcWhiteArrows(GameController game){this(game,(p,hand,at)->!game.items().magic().suppressed(p)&&!game.items().magic().suppressedAt(p.level(),at));}
    public LbcWhiteArrows(GameController game,Policy policy){this.game=game;this.policy=Objects.requireNonNull(policy);}
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"whitearrow",0)==1;}
    public boolean use(ServerPlayer p,InteractionHand hand){
        if(restoring||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands||!matches(p.getItemInHand(hand))||!game.playing()||game.maps().busy()||!p.isAlive()||p.isRemoved()||p.isSpectator()||game.isObserver(p)||!game.canUseGameplayItems(p)
                ||!LbcDimensionItems.ordinary(p.level())||!LbcBedwarsRoleGuards.publication(p.level())||game.items().actionsBlocked(p)||game.items().wands().inputTimeStopped(p))return false;
        var direction=Vec3.directionFromRotation(p.getXRot(),p.getYRot());var origin=p.getEyePosition();if(hand==InteractionHand.OFF_HAND)origin=origin.add(direction.scale(.5));
        var mana=game.items().mana();if(mana.maximum(p)<500||!policy.activation(p,hand,origin))return false;
        // These are the source's two independent scoreboard debits; current mana may become negative.
        var cause=game.items().indirectKills().coldCause(p,true);String epoch=game.items().epoch();
        var source=new LbcSpellSource.Source(p.getUUID(),p.getUUID(),epoch,cause,Set.of("god_hedgehog"));
        var shot=new Shot(++sequence,p,p.level(),origin,direction,epoch,p.getTeam()==null?null:p.getTeam().getName(),source);
        shots.put(shot.id,shot);launched++;mana.maximum(p,mana.maximum(p)-500);mana.set(p,mana.get(p)-500);
        LbcOverload.sound(p.level(),origin,"minecraft:block.beacon.power_select",1,2,0,0);
        p.level().sendParticles(new DustParticleOptions(0xCCCCCC,1),origin.x,origin.y,origin.z,30,.2,.2,.2,1);
        return true;
    }
    /** Existing boss ray semantics: pass walls, exact hitbox contacts, 21 .125 samples and 2.5 motion.
     * Callers retain their own target policy and execution ownership; this is the only tracing code. */
    static Vec3 traceStep(ServerLevel level,Vec3 from,Vec3 direction,Predicate<Entity> target,Consumer<LivingEntity> execute,Predicate<Vec3> resident){
        var to=from.add(direction.scale(2.5));
        for(var entity:level.getEntities((Entity)null,new AABB(from,to).inflate(1e-7),target))
            if(entity instanceof LivingEntity living&&LbcBeamGeometry.intersects(living.getBoundingBox(),from,to))execute.accept(living);
        boolean sounded=false;
        try(var particles=new LbcParticleBatch.Group(level,ParticleTypes.END_ROD,.01,0,true)){
            for(int n=0;n<=20;n++){var point=from.add(direction.scale(n*.125));if(!resident.test(point))continue;
                particles.add(point);
                if(!sounded){LbcOverload.sound(level,point,"minecraft:block.beacon.deactivate",.02f,2,0,0);sounded=true;}
            }
        }return to;
    }
    private static boolean resident(ServerLevel level,Vec3 point){var pos=BlockPos.containing(point);return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    private boolean target(Shot shot,Entity target){
        if(!(target instanceof LivingEntity)||!target.getType().builtInRegistryHolder().is(MOBS)&&!LbcWtf.managed(target)
                ||!game.items().spellSources().target(shot.source,target)||target.entityTags().contains(LbcGodHedgehog.TAG)
                ||game.items().mana().score(target,"void_resistance").get()>=1||game.items().timeStop().frozen(target))return false;
        if(shot.team!=null&&target.getTeam()!=null&&shot.team.equals(target.getTeam().getName()))return false;
        return !game.items().blaze().friendly(shot.author,target)&&!game.items().summons().friendly(shot.author,target);
    }
    public void tick(){
        int tick=game.server().getTickCount();if(restoring||lastTick==tick||!game.playing()||game.maps().busy()||game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally())return;lastTick=tick;
        long deadline=Math.min(EffectTerrain.deadlineNanos(),System.nanoTime()+2_000_000);int available=Math.min(128,shots.size());long accepted=sequence;
        callbacks++;try{for(int visited=0;visited<available&&!shots.isEmpty()&&System.nanoTime()<deadline;visited++){
            var next=shots.higherEntry(turn);if(next==null||next.getKey()>accepted)next=shots.firstEntry();turn=next.getKey();var shot=next.getValue();
            if(!game.items().currentEpoch(shot.epoch)){shots.remove(shot.id);continue;}
            if(shot.lastStepTick==tick||game.items().timeStop().stopped(shot.level)||game.items().timeStop().frozen(shot.author))continue;
            if(!resident(shot.level,shot.point)){shots.remove(shot.id);continue;}
            shot.lastStepTick=tick;
            try(var scope=game.items().spellSources().scope(shot.source,shot.level)){
                shot.point=traceStep(shot.level,shot.point,shot.direction,e->target(shot,e),living->{if(game.items().indirectKills().inheritedExecution(living,shot.source.cause()))kills++;},p->resident(shot.level,p));
            }
            steps++;if(++shot.age>=20)shots.remove(shot.id);
        }}finally{callbacks--;}
    }
    public record View(long id,UUID author,Vec3 point,Vec3 direction,int age,int cloneMask){}
    public record State(int active,long launched,long steps,long kills){}
    public State state(){return new State(shots.size(),launched,steps,kills);}
    public List<View> views(){return shots.values().stream().map(s->new View(s.id,s.author.getUUID(),s.point,s.direction,s.age,s.mask)).toList();}
    public void stop(){checkpointBoundary();shots.clear();sequence=turn=cloneCursor=launched=steps=kills=0;lastTick=Integer.MIN_VALUE;restoring=false;}
    public void prepareFrozen(){checkpointBoundary();restoring=true;shots.clear();}
    public void publishFrozen(){restoring=false;lastTick=game.server().getTickCount();}
    public void cancelFrozen(){stop();}
    public void checkpointBoundary(){if(callbacks!=0)throw new IllegalStateException("Finish white-arrow ray callbacks before replay");}
    MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){
        checkpointBoundary();record Saved(Shot shot,MatchDamageSources.Ref author,Vec3 point,int age,int mask){}
        var saved=shots.values().stream().map(s->new Saved(s,links.key(s.author),s.point,s.age,s.mask)).toList();
        long ids=sequence,cursor=turn,cloner=cloneCursor,launches=launched,frames=steps,executions=kills;
        return resolved->{checkpointBoundary();var plans=new ArrayList<Runnable>();for(var image:saved){var owner=(ServerPlayer)resolved.get(image.author);MatchAuthorityCheckpoint.world(game,image.shot.level);plans.add(()->{var shot=image.shot;shot.author=owner;shot.point=image.point;shot.age=image.age;shot.mask=image.mask;shot.lastStepTick=game.server().getTickCount();});}
            return ()->{plans.forEach(Runnable::run);shots.clear();for(var image:saved)shots.put(image.shot.id,image.shot);sequence=ids;turn=cursor;cloneCursor=cloner;launched=launches;steps=frames;kills=executions;};};
    }
    record CloneResult(int scanned,int operations,int bodies){}
    CloneResult cloneNearby(ServerPlayer holder,int skill,int scanBudget,int bodyBudget,long deadline){
        int count=CloneVirtualAbilityStates.count(skill),seen=0,made=0,operations=0;if(restoring||skill!=15&&skill!=17||bodyBudget<count)return new CloneResult(0,0,0);
        int available=Math.min(scanBudget,shots.size());long accepted=sequence;
        while(seen<available&&!shots.isEmpty()&&bodyBudget-made>=count&&System.nanoTime()<deadline){
            var entry=shots.higherEntry(cloneCursor);if(entry==null||entry.getKey()>accepted)entry=shots.firstEntry();cloneCursor=entry.getKey();seen++;var source=entry.getValue();
            if(!game.items().currentEpoch(source.epoch)||game.items().timeStop().frozen(source.author)||!CloneVirtualAbilityStates.near(game,holder,source.level,source.point,source.mask,skill))continue;
            var positions=CloneVirtualAbilityStates.positions(holder,source.level,source.point,skill);if(positions==null)continue;int flags=source.mask|CloneVirtualAbilityStates.bit(skill);
            for(var point:positions){var shot=new Shot(++sequence,source.author,source.level,point,source.direction,source.epoch,source.team,source.source);shot.age=source.age;shot.mask=flags;shot.lastStepTick=source.lastStepTick;shots.put(shot.id,shot);}source.mask=flags;made+=count;operations++;
        }return new CloneResult(seen,operations,made);
    }
}
