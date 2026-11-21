package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Actual unpickable projectiles, fixed hit margin, native suspension and the full sixty-second clock. */
public final class PolymorphWandChecks {
    private static State running;
    private PolymorphWandChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Polymorph wand: "+why);}
    static void absent(Entity entity){
        var level=(net.minecraft.server.level.ServerLevel)entity.level();
        require(level.getEntity(entity.getUUID())==null&&level.getEntity(entity.getId())==null,"original absent from native UUID/id lookup");
        var game=SkyWars2.game();require(!game.items().wands().validRoleTarget(entity,false,true),"offworld body excluded from cached gameplay targets");
        if(entity instanceof net.minecraft.world.entity.LivingEntity)require(!game.items().lifetimes().loadedTargets().contains(entity),"offworld body removed from shared homing snapshot");
        var manager=((me.skywars2.mixin.ActorCheckpointAccess.LevelEntities)level).skywars2$manager();
        var storage=((me.skywars2.mixin.ActorCheckpointAccess.Sections)manager).skywars2$sections();
        for(long chunk:storage.getAllChunksWithExistingSections().toLongArray())storage.getExistingSectionsInChunk(chunk)
                .forEach(section->section.getEntities().forEach(actor->require(actor!=entity,"original absent from every native section")));
        try{var field=net.minecraft.server.level.ServerLevel.class.getDeclaredField("entityTickList");field.setAccessible(true);
            require(!((net.minecraft.world.level.entity.EntityTickList)field.get(level)).contains(entity),"original absent from native ticking list");
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    private static final class State implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final List<Entity> originals=new ArrayList<>();
        net.minecraft.world.entity.Display.ItemDisplay companion;
        Object review;
        Object field(Object owner,String name){try{var field=owner.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(owner);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
        Vec3 origin;Entity outside,behind;ThrownEnderpearl pearl;int start,expiryOffset,pearlTicks;boolean closed,gravity;
        State(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);}
        Cow cow(Vec3 at){var c=new Cow(EntityTypes.COW,f.level);c.setPos(at);c.setNoAi(true);c.setNoGravity(true);f.level.addFreshEntity(c);f.own(c);return c;}
        void begin(){
            f.prepareParticipants();origin=new Vec3(f.home.x,Math.min(f.level.getMaxY()-20,f.home.y+50),f.home.z);gravity=f.owner.isNoGravity();f.move(f.owner,origin);f.move(f.other,origin.add(12,0,0));
            f.owner.setYRot(0);f.owner.setXRot(0);f.owner.setNoGravity(true);
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(origin.add(-4,0,0)),BlockPos.containing(origin.add(4,4,13))))f.set(pos,Blocks.AIR.defaultBlockState());
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(origin.add(-4,0,10)),BlockPos.containing(origin.add(4,4,10))))f.set(pos,Blocks.STONE.defaultBlockState());
            originals.add(cow(origin.add(2.2,.3,3)));originals.add(cow(origin.add(0,.3,8)));
            companion=new net.minecraft.world.entity.Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,f.level);companion.setPos(originals.getFirst().position());
            companion.setItemStack(new ItemStack(Items.STONE));require(f.level.addFreshEntity(companion)&&companion.startRiding(originals.getFirst(),true,true),"real mounted visual companion");f.own(companion);
            outside=cow(origin.add(2.6,.3,6));behind=cow(origin.add(0,.3,12));
            pearl=new ThrownEnderpearl(f.level,f.owner,new ItemStack(Items.ENDER_PEARL));pearl.setPos(f.owner.getEyePosition().add(0,0,6));pearl.setNoGravity(true);pearl.setDeltaMovement(Vec3.ZERO);f.level.addFreshEntity(pearl);f.own(pearl);originals.add(pearl);
            var ball=new net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball(f.level,f.owner,new ItemStack(Items.SNOWBALL));ball.setPos(f.owner.getEyePosition().add(0,0,4));ball.setNoGravity(true);ball.setDeltaMovement(Vec3.ZERO);f.level.addFreshEntity(ball);f.own(ball);originals.add(ball);
            pearl.setDeltaMovement(new Vec3(.2,.1,-.1));pearlTicks=pearl.tickCount;
            game.items().lifetimes().track(pearl);game.items().lifetimes().advance(pearl,1000);
            review=((java.util.Map<?,?>)field(game.items().lifetimes(),"tracked")).get(pearl);require(review!=null,"real accepted lifetime review before transformation");
            var loot=game.lbc().rollTable(f.level,origin,f.owner,"lbc:ported/sheep_wand");require(loot.size()==1,"canonical wand");
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,loot.getFirst());game.items().mana().set(f.owner,500);
            require(f.owner.gameMode.useItem(f.owner,f.level,f.owner.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction(),"native wand input");
            for(var e:originals)require(game.items().polymorph().active(e)&&game.items().polymorph().remaining(e)==1200,"all pierced near-hit bodies receive1200 ticks: "+e.getType());
            originals.forEach(PolymorphWandChecks::absent);
            for(var entity:originals)if(entity instanceof net.minecraft.world.entity.LivingEntity living){
                require(!living.canBeSeenByAnyone()&&!((net.minecraft.world.entity.LivingEntity)outside).canAttack(living),"native AI rejects cached offworld targets");
                require(!living.hurtServer(f.level,living.damageSources().playerAttack(f.owner),1),"stale direct references cannot damage the absent original or its sheep");
            }
            absent(companion);
            require(!game.items().polymorph().active(outside)&&!game.items().polymorph().active(behind),"fixed margin excludes outside body and solid wall excludes rear body");
            var bodies=new ArrayList<net.minecraft.world.entity.animal.sheep.Sheep>();
            for(var e:f.level.getAllEntities())if(game.items().polymorph().body(e)){
                var body=(net.minecraft.world.entity.animal.sheep.Sheep)e;bodies.add(body);
                require(body.getHealth()==100&&body.getMaxHealth()==100&&body.isPushable()
                        &&body.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)==0,"native100HP pushable sheep without knockback immunity");
                if(body.position().equals(pearl.position()))require(body.getDeltaMovement().equals(pearl.getDeltaMovement()),"pearl's actual entry momentum transfers to its sheep");
                e.setNoGravity(true);body.setNoAi(true);e.setDeltaMovement(Vec3.ZERO);
            }
            require(bodies.size()==originals.size(),"exactly one native body per absent original");
            var pushed=bodies.getFirst();var saved=outside.position();outside.setPos(pushed.position().add(.3,0,0));outside.setDeltaMovement(Vec3.ZERO);
            pushed.push(outside);require(pushed.getDeltaMovement().lengthSqr()>0&&outside.getDeltaMovement().lengthSqr()>0,"real native collision pushes both sheep and neighbor");
            pushed.setDeltaMovement(Vec3.ZERO);outside.setPos(saved);outside.setDeltaMovement(Vec3.ZERO);
            start=game.server().getTickCount();
            try{var clock=LbcPolymorph.class.getDeclaredField("lastTick");clock.setAccessible(true);
                expiryOffset=clock.getInt(game.items().polymorph())==start?1200:1199;
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        boolean step(){
            int age=game.server().getTickCount()-start;require(age<1210,"finite native duration");
            require(!pearl.isRemoved()&&f.owner.position().distanceToSqr(origin)<1,"suspended native pearl never impacts or teleports its owner");
            if(game.items().polymorph().active(pearl)){
                require(pearl.tickCount==pearlTicks,"unregistered pearl receives no native ticks");
                require(((java.util.Map<?,?>)field(game.items().lifetimes(),"tracked")).get(pearl)==review
                        &&((java.util.Collection<?>)field(game.items().lifetimes(),"queue")).contains(review),"accepted projectile review stays parked without being cancelled or rescanned");
                for(var e:originals)require(f.level.getEntity(e.getUUID())==null&&f.level.getEntity(e.getId())==null,"original stays absent for full active duration");
                require(f.level.getEntity(companion.getUUID())==null&&f.level.getEntity(companion.getId())==null,"original visual stays absent with its owner");
                require(age<=1200&&game.items().polymorph().remaining(pearl)>0,"remaining clock follows native ticks");
                if(age==300)require(game.items().polymorph().remaining(pearl)>=899,"wand outlasts old ten-second duration");
                return false;
            }
            require(age==expiryOffset,"exact1200 native updates, including the command tick when its owner has not run yet: age="+age+" expected="+expiryOffset);
            for(var e:originals)require(!game.items().polymorph().active(e)&&!e.isRemoved()&&f.level.getEntity(e.getUUID())==e&&f.level.getEntity(e.getId())==e,"same original identity re-registered");
            require(f.level.getEntity(companion.getUUID())==companion&&companion.getVehicle()==originals.getFirst(),"same visual and its native riding link restored");return true;
        }
        public void close(){if(closed)return;closed=true;game.items().polymorph().stop();f.owner.setNoGravity(gravity);f.close();}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var s=new State(game);try{s.begin();running=s;SkyWars2.LOGGER.info("[check] START polymorph-wand");}catch(Throwable e){s.close();throw e;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS polymorph-wand (actual near-hit/piercing, unpickable native pearl and snowball, solid-wall limit, native1200tick suspension and identity restoration)");}catch(Throwable e){running.close();running=null;SkyWars2.LOGGER.error("[check] FAIL polymorph-wand",e);}}
}
