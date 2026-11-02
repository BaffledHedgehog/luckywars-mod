package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Raw specials.mcfunction fireballs. Native bodies own flight, hits, clouds and fuse clocks. */
public final class LbcSpecialFireballs {
    private static final String TAG="skywars2.special_fireball", CHILD="skywars2.special_fireball.child";
    private static final String OWNER="skywars2.special_fireball.owner.";
    private static final String[] FLAGS={"smallfireball","fireball","dragfireball","smallfireball_imba","fireball_imba","dragfireball_imba"};
    private final GameController game;
    private final Set<Entity> actors=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Snowball> pending=new LinkedHashSet<>();
    public LbcSpecialFireballs(GameController game){this.game=game;}
    public static int variant(ItemStack stack){
        if(!stack.is(Items.SNOWBALL))return 0;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(data.getIntOr("special",0)!=1)return 0;
        for(int i=0;i<FLAGS.length;i++)if(data.getIntOr(FLAGS[i],0)==1)return i+1;
        return 0;
    }
    public static boolean managed(Entity entity){return entity!=null&&entity.entityTags().contains(TAG);}
    private boolean resident(ServerLevel level,Vec3 at){var pos=BlockPos.containing(at);return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    private boolean active(Entity e){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&e.level()==game.maps().level()&&game.items().currentEpoch(e);}
    private boolean frozen(Entity e){return !game.server().tickRateManager().runsNormally()||game.items().timeStop().frozen(e)||e.entityTags().contains("time_stopped");}
    public void loaded(Entity entity){
        if(managed(entity)){if(active(entity)){actors.add(entity);game.items().timeStop().prepareFrozen(entity);}else entity.discard();return;}
        if(entity instanceof Snowball ball&&variant(ball.getItem())!=0&&ball.getOwner() instanceof ServerPlayer p
                &&game.playing()&&game.stage()!=GameController.Stage.ENDING&&p.isAlive()&&!game.isObserver(p)
                &&game.canUseGameplayItems(p)&&LbcDimensionItems.ordinary(p)&&!game.items().actionsBlocked(p)&&entity.level()==game.maps().level()){
            game.items().markCurrentItemEpoch(ball);pending.add(ball);
        }
    }
    private <T extends Entity>T insert(ServerPlayer p,T actor,Vec3 at,Vec3 velocity,boolean child){
        actor.setPos(at);actor.setDeltaMovement(velocity);actor.entityTags().add(TAG);if(p!=null)actor.entityTags().add(OWNER+p.getUUID());
        if(child)actor.entityTags().add(CHILD);game.items().markCurrentItemEpoch(actor);
        if(actor instanceof Projectile projectile){projectile.setOwner(p);LbcShooters.orient(projectile);}
        actors.add(actor);game.items().timeStop().prepareFrozen(actor);
        if(!actor.level().addFreshEntity(actor)||actor.isRemoved()){actors.remove(actor);actor.discard();return null;}return actor;
    }
    private static double scaled(double value,double read,double write){return Math.floor(value*read)*write;}
    private static Vec3 scaled(Vec3 v,double read,double write){return new Vec3(scaled(v.x,read,write),scaled(v.y,read,write),scaled(v.z,read,write));}
    /** Use the actual snowball's position, rotation and velocity, including any native throw spread. */
    public List<Entity> convert(Snowball ball){
        int kind=variant(ball.getItem());if(kind==0||ball.isRemoved()||!(ball.getOwner() instanceof ServerPlayer p)||!active(ball)||frozen(ball))return List.of();
        var direction=Vec3.directionFromRotation(ball.getXRot(),ball.getYRot());var at=ball.position().add(direction.scale(kind==3||kind==6?0:1.5));
        if(!resident(p.level(),at)||!resident(p.level(),at.add(0,0,3)))return List.of();
        var result=new ArrayList<Entity>();var motion=ball.getDeltaMovement();
        switch(kind){
            case 1->{var fire=new SmallFireball(EntityTypes.SMALL_FIREBALL,p.level());fire.entityTags().add("can_deleted");result.add(insert(p,fire,at,scaled(motion,10000,.0006),false));}
            case 2,5->{var fire=new LargeFireball(p.level(),p,Vec3.ZERO,kind==5?127:2);fire.setNoGravity(true);fire.setPermanentlyInvulnerable(kind==5);result.add(insert(p,fire,at,scaled(motion,100000,.00000075),false));}
            case 3,6->{var velocity=scaled(motion,100000,.0000005);var fire=new DragonFireball(EntityTypes.DRAGON_FIREBALL,p.level());fire.setNoGravity(true);result.add(insert(p,fire,at,velocity,false));
                if(kind==6)for(int i=0;i<16;i++){var child=new DragonFireball(EntityTypes.DRAGON_FIREBALL,p.level());
                    var v=new Vec3((Math.floor(velocity.x*1000)+p.getRandom().nextInt(61)-30)*.001,(Math.floor(velocity.y*1000)+p.getRandom().nextInt(11)-5)*.001,(Math.floor(velocity.z*1000)+p.getRandom().nextInt(61)-30)*.001);
                    result.add(insert(p,child,at,v,true));}}
            case 4->{var velocity=scaled(motion,10000,.0006);var tnt=new PrimedTnt(p.level(),at.x,at.y,at.z,p);tnt.setFuse(100);tnt.entityTags().addAll(List.of("can_deleted","groundbam","puknul"));result.add(insert(p,tnt,at,velocity,false));
                // Summoned TNT has default rotation [0,0]; the helper's local ^^^3 is world +Z.
                var childAt=at.add(0,0,3);for(int i=0;i<32;i++){var child=new PrimedTnt(p.level(),childAt.x,childAt.y,childAt.z,p);child.setFuse(100);child.setPermanentlyInvulnerable(true);child.entityTags().addAll(List.of("groundbam","evoker","fire","electro","strong"));
                    var v=new Vec3((Math.floor(velocity.x*10)+p.getRandom().nextInt(21)-10)*.1,(Math.floor(velocity.y*10)+p.getRandom().nextInt(21)-10)*.1,(Math.floor(velocity.z*10)+p.getRandom().nextInt(21)-10)*.1);
                    result.add(insert(p,child,childAt,v,true));}}
            default->{}
        }
        result.removeIf(Objects::isNull);
        if(!result.isEmpty()&&ball.getItem().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("tmt_item",0)!=1)ball.discard();
        return List.copyOf(result);
    }
    public LivingEntity owner(Entity e){
        if(!managed(e))return null;
        if(e instanceof Projectile projectile&&projectile.getOwner() instanceof LivingEntity living)return living;
        if(e instanceof PrimedTnt tnt&&tnt.getOwner()!=null)return tnt.getOwner();
        if(e instanceof EvokerFangs fang&&fang.getOwner()!=null)return fang.getOwner();
        if(e instanceof LightningBolt bolt&&bolt.getCause()!=null)return bolt.getCause();
        if(e instanceof AreaEffectCloud cloud&&cloud.getOwner()!=null)return cloud.getOwner();
        for(var tag:e.entityTags())if(tag.startsWith(OWNER))try{return game.server().getPlayerList().getPlayer(UUID.fromString(tag.substring(OWNER.length())));}catch(IllegalArgumentException ignored){}
        return null;
    }
    public boolean canAffect(Entity source,Entity target){return !managed(source)||active(source)&&game.items().wands().validTarget(target);}
    public boolean explosionTarget(Entity source,Entity target){return canAffect(source,target);}
    /** Native DragonFireball constructs its cloud; retain its physical policy before the load callback. */
    public void inherit(Entity parent,Entity child){
        if(!managed(parent)||!active(parent)||!(child instanceof AreaEffectCloud))return;
        child.entityTags().add(TAG);child.entityTags().add(CHILD);
        for(var tag:parent.entityTags())if(tag.startsWith(OWNER))child.entityTags().add(tag);
        game.items().markCurrentItemEpoch(child);
    }
    public void tick(){
        for(var ball:List.copyOf(pending)){
            if(ball.isRemoved()||!active(ball)){pending.remove(ball);if(!ball.isRemoved())ball.discard();continue;}
            if(frozen(ball))continue;pending.remove(ball);convert(ball);
        }
        for(var actor:List.copyOf(actors)){
            if(actor.isRemoved()){actors.remove(actor);continue;}if(!active(actor)){actors.remove(actor);actor.discard();continue;}
            if(frozen(actor)||!resident((ServerLevel)actor.level(),actor.position()))continue;
            if(actor instanceof PrimedTnt tnt&&tnt.onGround()&&tnt.entityTags().remove("groundbam")){
                tnt.setFuse(2);if(tnt.entityTags().contains(CHILD))landing(tnt);
            }
        }
    }
    private <T extends Entity>T carried(Entity parent,T child){
        child.entityTags().add(TAG);
        for(var tag:parent.entityTags())if(tag.startsWith(OWNER))child.entityTags().add(tag);
        return child;
    }
    private void landing(PrimedTnt tnt){
        var p=owner(tnt) instanceof ServerPlayer player?player:null;var level=(ServerLevel)tnt.level();var at=tnt.position();
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(10),e->canAffect(tnt,e)))if(target.distanceToSqr(at)<=100)
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)insert(p,carried(tnt,new EvokerFangs(level,target.getX()+x,target.getY(),target.getZ()+z,0,0,null)),new Vec3(target.getX()+x,target.getY(),target.getZ()+z),Vec3.ZERO,true);
        game.items().indirectKills().worldChanges(level,tnt,true,()->{
            var center=BlockPos.containing(at);for(var pos:BlockPos.betweenClosed(center.offset(-5,-5,-5),center.offset(5,5,5)))
                if(resident(level,Vec3.atCenterOf(pos))&&level.getBlockState(pos).is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.Identifier.withDefaultNamespace("airs"))))level.setBlock(pos,Blocks.FIRE.defaultBlockState(),3);
        });
        // The real carrier keeps its two-tick fuse; strong adds a separate immediate explosion.
        var bolt=new LightningBolt(EntityTypes.LIGHTNING_BOLT,level);bolt.setCause(p);insert(p,carried(tnt,bolt),at,Vec3.ZERO,true);
        game.items().exasperato().encounterBlast(p,level,at,4,blast->carried(tnt,blast));
    }
    public void unloaded(Entity e){actors.remove(e);pending.remove(e);}
    /** Released native actors retain their own clocks and owner UUID when their thrower leaves. */
    public void forget(ServerPlayer p){}
    public void stop(){var old=new ArrayList<Entity>(actors);old.addAll(pending);actors.clear();pending.clear();old.forEach(Entity::discard);}
    public int pendingCount(){return pending.size();}
    public int trackedCount(){return actors.size();}
    public List<Entity> snapshot(){return List.copyOf(actors);}

    static boolean cloneRecognizes(Entity e){return managed(e)||e instanceof Snowball b&&variant(b.getItem())!=0;}
    LbcCloneSkills.ActorImage cloneImage(Entity source) {
        boolean actor=actors.contains(source),waiting=pending.contains(source);
        if(!actor&&!waiting)return null;
        // CHILD marks damaging released siblings too; it is not a visual/protected role tag.
        return CloneComplexProjectileStates.image(e->{if(actor)actors.add(e);if(waiting)pending.add((Snowball)e);},
                e->{actors.remove(e);pending.remove(e);});
    }

}
