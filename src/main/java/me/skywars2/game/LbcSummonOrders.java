package me.skywars2.game;

import me.skywars2.mixin.SummonTargetAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import java.util.*;

/** Immediate player orders; native target goals keep the chosen enemy through vanilla AI updates. */
public final class LbcSummonOrders {
    private final GameController game;
    private final LbcSummons summons;
    private record Assignment(UUID owner,LivingEntity target){}
    private IdentityHashMap<Mob,Assignment> assignments=new IdentityHashMap<>();
    private int ticks;
    LbcSummonOrders(GameController game,LbcSummons summons){this.game=game;this.summons=summons;}
    private boolean enemy(ServerPlayer owner,LivingEntity target){
        return target.level()==owner.level()&&game.items().wands().target(owner,target)
                &&!game.items().magic().suppressedAt(owner.level(),target.position())&&!target.entityTags().contains("nomagic_active");
    }
    private boolean fighter(Mob mob){return !(mob instanceof net.minecraft.world.entity.ambient.Bat)
            &&!(mob instanceof net.minecraft.world.entity.animal.equine.SkeletonHorse);}
    private List<Mob> troops(ServerPlayer owner){return summons.snapshot().stream().filter(m->fighter(m)&&m.isAlive()&&!m.isRemoved()
            &&m.level()==owner.level()&&summons.owner(m)==owner&&game.items().currentEpoch(m))
            .sorted(Comparator.comparingInt(Entity::getId)).toList();}
    LivingEntity assigned(Mob mob){var order=assignments.get(mob);
        return order!=null&&summons.owner(mob)!=null&&summons.owner(mob).getUUID().equals(order.owner)
                &&summons.canAttack(mob,order.target)?order.target:null;
    }
    /** Loaded entity hitboxes, nearest intersection and native block outlines; no arbitrary aim range. */
    LivingEntity lookedAt(ServerPlayer owner){
        var start=owner.getEyePosition();var direction=owner.getLookAngle();LivingEntity closest=null;Vec3 impact=null;double distance=Double.POSITIVE_INFINITY;
        for(var target:game.items().lifetimes().loadedTargets()){
            if(!enemy(owner,target))continue;
            var box=target.getBoundingBox().inflate(target.getPickRadius());
            double reach=box.getCenter().distanceTo(start)+box.getSize()+1;
            var hit=box.contains(start)?Optional.of(start):box.clip(start,start.add(direction.scale(reach)));
            if(hit.isPresent()&&hit.get().distanceToSqr(start)<distance){closest=target;impact=hit.get();distance=impact.distanceToSqr(start);}
        }
        if(closest==null)return null;
        var end=impact;var level=owner.level();var context=CollisionContext.of(owner);
        var wall=BlockGetter.traverseBlocks(start,end,level,(world,pos)->{
            if(!world.hasChunkAt(pos))return Shapes.block().clip(start,end,pos);
            return world.getBlockState(pos).getShape(world,pos,context).clip(start,end,pos);
        },world->null);
        return wall==null?closest:null;
    }
    /** No mana admission/debit and no new summon; an aimed enemy explicitly overrides spreading. */
    boolean command(ServerPlayer owner){
        var troops=troops(owner);var focus=lookedAt(owner);
        if(focus!=null){for(var mob:troops)assign(owner,mob,focus);}
        else distribute(owner,troops,true);
        return true;
    }
    private void assign(ServerPlayer owner,Mob mob,LivingEntity target){
        if(target==null){assignments.remove(mob);mob.setTarget(null);return;}
        assignments.put(mob,new Assignment(owner.getUUID(),target));mob.setTarget(target);
    }
    private double radius(Mob mob){return mob.entityTags().contains("summactivated")||mob instanceof net.minecraft.world.entity.monster.Vex?200:100;}
    /** Distance stays primary in sparse terrain; nearby equal choices spread across available enemies. */
    private void distribute(ServerPlayer owner,List<Mob> troops,boolean replace){
        var loads=new IdentityHashMap<LivingEntity,Integer>();
        if(!replace)for(var mob:troops){var chosen=assigned(mob);if(chosen!=null)loads.merge(chosen,1,Integer::sum);}
        var candidates=game.items().lifetimes().loadedTargets().stream().filter(e->enemy(owner,e)).toList();
        for(var mob:troops){
            if(!replace&&assigned(mob)!=null)continue;
            LivingEntity best=null;double score=Double.POSITIVE_INFINITY;
            for(var target:candidates){double distance=mob.distanceToSqr(target);if(distance>radius(mob)*radius(mob))continue;
                double weighted=(distance+16)*(1+loads.getOrDefault(target,0));
                if(weighted<score){score=weighted;best=target;}
            }
            assign(owner,mob,best);if(best!=null)loads.merge(best,1,Integer::sum);
        }
    }
    void tick(){
        if(++ticks%20!=0)return;
        var owners=new LinkedHashSet<ServerPlayer>();for(var mob:summons.snapshot()){var owner=summons.owner(mob);if(owner!=null)owners.add(owner);}
        for(var owner:owners)if(game.items().mana().visible(owner))distribute(owner,troops(owner),false);
    }
    void attach(Mob mob){((SummonTargetAccess)mob).skywars2$summonTargets().addGoal(0,new AssignedTargetGoal(mob));}
    void changedOwner(Entity entity){if(entity instanceof Mob mob)assignments.remove(mob);}
    void converted(Mob original,Mob converted){var previous=assignments.remove(original);if(previous!=null)assignments.put(converted,previous);attach(converted);}
    void removed(Mob mob){assignments.remove(mob);}
    int count(){return assignments.size();}
    void stop(){assignments.clear();}
    private final class AssignedTargetGoal extends Goal {
        private final Mob mob;
        AssignedTargetGoal(Mob mob){this.mob=mob;setFlags(EnumSet.of(Flag.TARGET));}
        @Override public boolean canUse(){return assigned(mob)!=null;}
        @Override public boolean canContinueToUse(){return canUse();}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void start(){tick();}
        @Override public void tick(){var target=assigned(mob);if(target!=null&&mob.getTarget()!=target)mob.setTarget(target);}
        @Override public void stop(){mob.setTarget(null);}
    }
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){return new Checkpoint(links);}
    final class Checkpoint {
        private record Order(MatchDamageSources.Ref actor,UUID owner,MatchDamageSources.Ref target) { }
        private final List<Order> image=new ArrayList<>();private final int cadence=ticks;
        Checkpoint(MatchSpellCheckpoint.Links links){assignments.forEach((actor,order)->image.add(new Order(links.key(actor),order.owner,links.key(order.target))));}
        Runnable prepare(MatchSpellCheckpoint.Links links){var restored=new IdentityHashMap<Mob,Assignment>();
            for(var order:image){if(!(links.get(order.actor) instanceof Mob mob)||!(links.get(order.target) instanceof LivingEntity target))throw new IllegalStateException("Restore native summon/order targets first");restored.put(mob,new Assignment(order.owner,target));}
            return ()->{assignments=restored;ticks=cadence;};}
    }

    LbcCloneSkills.ActorImage cloneImage(Entity source){
        if(!(source instanceof Mob mob))return null;
        var order=assignments.get(mob);if(order==null)return null;
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){
                var target=family.getOrDefault(order.target,order.target);
                assignments.put((Mob)clone,new Assignment(order.owner,(LivingEntity)target));
            }
            public void rollback(Entity clone){assignments.remove(clone);}
        };
    }

}
