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
    private static final class State implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final List<Entity> originals=new ArrayList<>();
        Vec3 origin;Entity outside,behind;ThrownEnderpearl pearl;int start,expiryOffset;boolean closed,gravity;
        State(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);}
        Cow cow(Vec3 at){var c=new Cow(EntityTypes.COW,f.level);c.setPos(at);c.setNoAi(true);c.setNoGravity(true);f.level.addFreshEntity(c);f.own(c);return c;}
        void begin(){
            f.prepareParticipants();origin=new Vec3(f.home.x,Math.min(f.level.getMaxY()-20,f.home.y+50),f.home.z);gravity=f.owner.isNoGravity();f.move(f.owner,origin);f.move(f.other,origin.add(12,0,0));
            f.owner.setYRot(0);f.owner.setXRot(0);f.owner.setNoGravity(true);
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(origin.add(-4,0,0)),BlockPos.containing(origin.add(4,4,13))))f.set(pos,Blocks.AIR.defaultBlockState());
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(origin.add(-4,0,10)),BlockPos.containing(origin.add(4,4,10))))f.set(pos,Blocks.STONE.defaultBlockState());
            originals.add(cow(origin.add(2.2,.3,3)));originals.add(cow(origin.add(0,.3,8)));
            outside=cow(origin.add(2.6,.3,6));behind=cow(origin.add(0,.3,12));
            pearl=new ThrownEnderpearl(f.level,f.owner,new ItemStack(Items.ENDER_PEARL));pearl.setPos(f.owner.getEyePosition().add(0,0,6));pearl.setNoGravity(true);pearl.setDeltaMovement(Vec3.ZERO);f.level.addFreshEntity(pearl);f.own(pearl);originals.add(pearl);
            var ball=new net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball(f.level,f.owner,new ItemStack(Items.SNOWBALL));ball.setPos(f.owner.getEyePosition().add(0,0,4));ball.setNoGravity(true);ball.setDeltaMovement(Vec3.ZERO);f.level.addFreshEntity(ball);f.own(ball);originals.add(ball);
            var loot=game.lbc().rollTable(f.level,origin,f.owner,"lbc:ported/sheep_wand");require(loot.size()==1,"canonical wand");
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,loot.getFirst());game.items().mana().set(f.owner,500);
            require(f.owner.gameMode.useItem(f.owner,f.level,f.owner.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction(),"native wand input");
            for(var e:originals)require(game.items().polymorph().active(e)&&game.items().polymorph().remaining(e)==1200,"all pierced near-hit bodies receive1200 ticks: "+e.getType());
            require(!game.items().polymorph().active(outside)&&!game.items().polymorph().active(behind),"fixed margin excludes outside body and solid wall excludes rear body");
            for(var e:f.level.getAllEntities())if(game.items().polymorph().body(e)){e.setNoGravity(true);if(e instanceof net.minecraft.world.entity.Mob mob)mob.setNoAi(true);}
            start=game.server().getTickCount();
            try{var clock=LbcPolymorph.class.getDeclaredField("lastTick");clock.setAccessible(true);
                expiryOffset=clock.getInt(game.items().polymorph())==start?1200:1199;
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        boolean step(){
            int age=game.server().getTickCount()-start;require(age<1210,"finite native duration");
            require(!pearl.isRemoved()&&f.owner.position().distanceToSqr(origin)<1,"suspended native pearl never impacts or teleports its owner");
            if(game.items().polymorph().active(pearl)){
                require(age<=1200&&game.items().polymorph().remaining(pearl)>0,"remaining clock follows native ticks");
                if(age==300)require(game.items().polymorph().remaining(pearl)>=899,"wand outlasts old ten-second duration");
                return false;
            }
            require(age==expiryOffset,"exact1200 native updates, including the command tick when its owner has not run yet: age="+age+" expected="+expiryOffset);
            for(var e:originals)require(!game.items().polymorph().active(e)&&!e.isRemoved(),"same original identity restored");return true;
        }
        public void close(){if(closed)return;closed=true;game.items().polymorph().stop();f.owner.setNoGravity(gravity);f.close();}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var s=new State(game);try{s.begin();running=s;SkyWars2.LOGGER.info("[check] START polymorph-wand");}catch(Throwable e){s.close();throw e;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS polymorph-wand (actual near-hit/piercing, unpickable native pearl and snowball, solid-wall limit, native1200tick suspension and identity restoration)");}catch(Throwable e){running.close();running=null;SkyWars2.LOGGER.error("[check] FAIL polymorph-wand",e);}}
}
