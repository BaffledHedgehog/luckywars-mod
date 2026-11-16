package me.skywars2.game;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/** Explicit native task schemas; never traverses arbitrary goal, server or callback object graphs. */
final class MatchGoalFields {
    private final Mob owner;
    final Goal goal;
    boolean wasRunning;
    private final Map<Field,Object> values=new LinkedHashMap<>();
    private final Map<Field,MatchDamageSources.Ref> references=new LinkedHashMap<>();
    private final Map<Path,MatchMobNavigation.Route> routes=new IdentityHashMap<>();
    private final Map<BlockPos.MutableBlockPos,BlockPos> positions=new IdentityHashMap<>();
    private final Map<List<BlockPos>,List<BlockPos>> positionLists=new IdentityHashMap<>();
    private boolean referencesCaptured;
    private final net.minecraft.world.entity.animal.fish.AbstractSchoolingFish flockLeader;
    private MatchDamageSources.Ref flockLeaderRef;

    static MatchGoalFields capture(Mob owner,Goal goal){
        var image=new MatchGoalFields(owner,goal);
        if(goal instanceof LookAtPlayerGoal)image.save(LookAtPlayerGoal.class,"lookAt","lookTime");
        if(goal instanceof RandomLookAroundGoal)image.save(RandomLookAroundGoal.class,"relX","relZ","lookTime");
        if(goal instanceof FollowParentGoal)image.save(FollowParentGoal.class,"parent","timeToRecalcPath");
        if(goal instanceof FollowOwnerGoal)image.save(FollowOwnerGoal.class,"owner","timeToRecalcPath","oldWaterCost");
        if(goal instanceof FollowMobGoal)image.save(FollowMobGoal.class,"followingMob","timeToRecalcPath","oldWaterCost");
        if(goal instanceof TemptGoal)image.save(TemptGoal.class,"px","py","pz","pRotX","pRotY","player","calmDown","isRunning");
        if(goal instanceof AvoidEntityGoal<?>)image.save(AvoidEntityGoal.class,"toAvoid","path");
        if(goal instanceof RangedBowAttackGoal<?>)image.save(RangedBowAttackGoal.class,"attackIntervalMin","attackTime","seeTime","strafingClockwise","strafingBackwards","strafingTime");
        if(goal instanceof RangedCrossbowAttackGoal<?>)image.save(RangedCrossbowAttackGoal.class,"crossbowState","seeTime","attackDelay","updatePathDelay");
        if(goal instanceof RandomStrollGoal)image.save(RandomStrollGoal.class,"wantedX","wantedY","wantedZ","interval","forceTrigger");
        if(goal instanceof PanicGoal)image.save(PanicGoal.class,"posX","posY","posZ","isRunning");
        if(goal instanceof FleeSunGoal)image.save(FleeSunGoal.class,"wantedX","wantedY","wantedZ");
        if(goal instanceof MoveTowardsTargetGoal)image.save(MoveTowardsTargetGoal.class,"target","wantedX","wantedY","wantedZ");
        if(goal instanceof MoveTowardsRestrictionGoal)image.save(MoveTowardsRestrictionGoal.class,"wantedX","wantedY","wantedZ");
        if(goal instanceof LeapAtTargetGoal)image.save(LeapAtTargetGoal.class,"target");
        if(goal instanceof SwellGoal)image.save(SwellGoal.class,"target");
        if(goal instanceof EatBlockGoal)image.save(EatBlockGoal.class,"eatAnimationTick");
        if(goal instanceof OcelotAttackGoal)image.save(OcelotAttackGoal.class,"target","attackTime");
        if(goal instanceof DoorInteractGoal)image.save(DoorInteractGoal.class,"mob","doorPos","hasDoor","passed","doorOpenDirX","doorOpenDirZ");
        if(goal instanceof OpenDoorGoal)image.save(OpenDoorGoal.class,"forgetTime");
        if(goal instanceof BreakDoorGoal)image.save(BreakDoorGoal.class,"breakTime","lastBreakProgress","doorBreakTime");
        if(goal instanceof MoveToBlockGoal)image.save(MoveToBlockGoal.class,"nextStartTick","tryTicks","maxStayTicks","blockPos","reachedTarget","verticalSearchStart");
        if(goal instanceof MoveThroughVillageGoal){image.save(MoveThroughVillageGoal.class,"path","poiPos");image.savePositions(MoveThroughVillageGoal.class,"visited");}
        if(goal instanceof OwnerHurtByTargetGoal)image.save(OwnerHurtByTargetGoal.class,"ownerLastHurtBy","timestamp");
        if(goal instanceof OwnerHurtTargetGoal)image.save(OwnerHurtTargetGoal.class,"ownerLastHurt","timestamp");
        if(goal instanceof ResetUniversalAngerTargetGoal<?>)image.save(ResetUniversalAngerTargetGoal.class,"lastHurtByPlayerTimestamp");
        if(goal instanceof NearestHealableRaiderTargetGoal<?>)image.save(NearestHealableRaiderTargetGoal.class,"cooldown");
        if(goal instanceof NearestAttackableWitchTargetGoal<?>)image.save(NearestAttackableWitchTargetGoal.class,"canAttack");
        if(goal instanceof FollowFlockLeaderGoal)image.save(FollowFlockLeaderGoal.class,"timeToRecalcPath","nextStartTick");
        return image.values.isEmpty()?null:image;
    }
    private MatchGoalFields(Mob owner,Goal goal){
        this.owner=owner;this.goal=goal;
        flockLeader=goal instanceof FollowFlockLeaderGoal&&owner instanceof net.minecraft.world.entity.animal.fish.AbstractSchoolingFish fish?MatchFishSchools.leader(fish):null;
    }
    private void save(Class<?> type,String... names){
        for(String name:names){
            Field field=field(type,name);Class<?> valueType=field.getType();Object value=read(field);
            if(!valueType.isPrimitive()&&!valueType.isEnum()&&!Entity.class.isAssignableFrom(valueType)&&valueType!=Path.class&&!BlockPos.class.isAssignableFrom(valueType)&&valueType!=Vec3.class)
                throw new IllegalStateException("Review native task field value semantics: "+field);
            if(value instanceof BlockPos pos)position(pos);
            if(value instanceof Vec3&&value.getClass()!=Vec3.class)throw new IllegalStateException("Review mutable native vector subtype: "+field);
            values.put(field,value);
            if(value instanceof Path path)routes.computeIfAbsent(path,MatchMobNavigation.Route::new);
        }
    }
    private void position(BlockPos value){
        if(value instanceof BlockPos.MutableBlockPos mutable)positions.computeIfAbsent(mutable,BlockPos::immutable);
        else if(value.getClass()!=BlockPos.class)throw new IllegalStateException("Review native position subtype: "+value.getClass());
    }
    @SuppressWarnings("unchecked") private void savePositions(Class<?> type,String name){
        Field field=field(type,name);Object value=read(field);
        if(!(value instanceof List<?> list))throw new IllegalStateException("Review native position history: "+field);
        for(var item:list)if(item instanceof BlockPos pos)position(pos);else if(item!=null)throw new IllegalStateException("Review native position history element: "+field);
        var original=(List<BlockPos>)list;positionLists.put(original,Collections.unmodifiableList(new ArrayList<>(original)));values.put(field,original);
    }
    void restore(){
        positions.forEach((position,saved)->position.set(saved));
        positionLists.forEach((list,saved)->{list.clear();list.addAll(saved);});
        routes.values().forEach(MatchMobNavigation.Route::restore);
        values.forEach((field,value)->{
            if(Modifier.isFinal(field.getModifiers())){if(read(field)!=value)throw new IllegalStateException("Final native task value identity changed: "+field);}
            else write(field,value);
        });
    }
    void captureReferences(MatchDamageSources sources){
        values.forEach((field,value)->{if(Entity.class.isAssignableFrom(field.getType()))references.put(field,sources.bodyRef((Entity)value));});
        flockLeaderRef=sources.dependencyRef(flockLeader);
        referencesCaptured=true;
    }
    boolean restoreReferences(Function<UUID,Entity> lookup,Set<UUID> pending){
        if(!referencesCaptured)return false;
        boolean absent=false;
        if(flockLeaderRef!=null){
            var leader=MatchDamageSources.resolve(flockLeaderRef,lookup,pending);
            if(leader==null&&MatchDamageSources.intentionallyAbsent(flockLeaderRef.id()))absent=true;
        }
        for(var entry:references.entrySet()){
            var ref=entry.getValue();Entity resolved=MatchDamageSources.resolve(ref,lookup,pending);
            if(ref!=null&&resolved==null){
                if(!MatchDamageSources.intentionallyAbsent(ref.id()))continue;
                absent=true;
            }
            if(resolved!=null&&!entry.getKey().getType().isInstance(resolved))throw new IllegalStateException("Native task reference resolved to the wrong body type: "+entry.getKey());
            write(entry.getKey(),resolved);
        }
        return absent;
    }
    void absent(){
        references.keySet().forEach(field->write(field,null));
        // Match native stop's values directly. Inactive cached references must not cancel another task's route.
        if(goal instanceof TemptGoal){
            write(field(TemptGoal.class,"isRunning"),false);write(field(TemptGoal.class,"calmDown"),50);
            if(wasRunning){
                if(goal instanceof TemptGoal.ForNonPathfinders)owner.getMoveControl().setWait();
                else owner.getNavigation().stop();
            }
        }
        if(wasRunning&&(goal instanceof FollowOwnerGoal||goal instanceof FollowMobGoal)){
            owner.getNavigation().stop();
            Class<?> type=goal instanceof FollowOwnerGoal?FollowOwnerGoal.class:FollowMobGoal.class;
            owner.setPathfindingMalus(PathType.WATER,(Float)values.get(field(type,"oldWaterCost")));
        }
        if(wasRunning&&goal instanceof OcelotAttackGoal)owner.getNavigation().stop();
    }
    private static Field field(Class<?> type,String name){
        try{var result=type.getDeclaredField(name);result.setAccessible(true);return result;}
        catch(ReflectiveOperationException error){throw new IllegalStateException("Review native task schema "+type.getName()+"."+name,error);}
    }
    private Object read(Field field){try{return field.get(goal);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    private void write(Field field,Object value){try{field.set(goal,value);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
}
