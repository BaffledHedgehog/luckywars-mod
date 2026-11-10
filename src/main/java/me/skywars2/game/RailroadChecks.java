package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Genuine item-on-block input, native scheduled marker/cart, pause and powered track. */
public final class RailroadChecks {
    private static Scene active;
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("Railroad: "+detail);}
    private static int age(Marker marker){for(var tag:marker.entityTags())if(tag.startsWith("skywars2.railroad.age."))return Integer.parseInt(tag.substring("skywars2.railroad.age.".length()));return -1;}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final NativeMeleeChecks.NativeStats stats;final Set<UUID> original=new HashSet<>();
        Marker marker;BlockPos origin;int when,phase,started;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);stats=new NativeMeleeChecks.NativeStats(f.owner,List.of(LbcImbaPlaceableItems.RAILROAD));started=game.server().getTickCount();f.level.getAllEntities().forEach(e->original.add(e.getUUID()));}
        void begin(){
            f.prepareParticipants();origin=new BlockPos((int)Math.floor(f.home.x),Math.min(f.level.getMaxY()-8,300),(int)Math.floor(f.home.z));
            for(int x=(origin.getX()-3)>>4;x<=(origin.getX()+3)>>4;x++)for(int z=(origin.getZ()-3)>>4;z<=(origin.getZ()+16)>>4;z++)f.level.getChunk(x,z);
            for(var pos:BlockPos.betweenClosed(origin.offset(-3,-1,-2),origin.offset(3,4,16)))f.remember(pos);
            for(int y=f.level.getMinY();y<origin.getY()-1;y++)f.remember(new BlockPos(origin.getX(),y,origin.getZ()+6));
            f.move(f.owner,Vec3.atBottomCenterOf(origin).add(0,2,-3));f.owner.setYRot(0);f.owner.setXRot(0);
            f.set(origin.below(),Blocks.STONE.defaultBlockState());f.set(origin,Blocks.AIR.defaultBlockState());
            var stack=f.loot("lbc:item_defs/lbc/railroad__56741b6d7155").copyWithCount(2);
            require(stack.is(LbcImbaPlaceableItems.RAILROAD),"canonical old loot leaf resolves the actual builder item");f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var hit=new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin.below(),false);
            require(f.owner.gameMode.useItemOn(f.owner,f.level,stack,InteractionHand.MAIN_HAND,hit).consumesAction()&&stack.getCount()==1,"actual right click consumes exactly one builder");
            for(var entity:f.level.getAllEntities())if(LbcRailroad.managed(entity)&&!original.contains(entity.getUUID())){require(marker==null,"one native marker");marker=(Marker)entity;f.own(entity);}
            require(marker!=null&&age(marker)==0,"native builder starts at source railtime0");f.pause();when=game.server().getTickCount();
        }
        boolean tick(){
            f.client.acknowledge();f.audience.acknowledge();require(game.playing()&&game.server().getTickCount()-started<100,"bounded active native scene");
            if(phase==0){require(age(marker)==0,"TimeStop pauses the complete builder");if(game.server().getTickCount()-when<3)return false;f.thaw();phase=1;return false;}
            for(var entity:f.level.getAllEntities())if(entity instanceof Minecart&&!original.contains(entity.getUUID()))f.own(entity);
            if(phase==1){
                if(age(marker)<12)return false;
                require(!marker.isRemoved()&&marker.blockPosition().getZ()>=origin.getZ()+10,"actual scheduler builds forward every active tick");
                require(f.level.getBlockState(origin).is(Blocks.POWERED_RAIL)&&f.level.getBlockState(origin.below()).is(Blocks.OAK_PLANKS),"first actual track and wooden foundation");
                require(f.level.getBlockState(origin.offset(0,-1,6)).is(Blocks.REDSTONE_BLOCK),"source every seventh segment powers the track");
                long carts=0;for(var entity:f.level.getAllEntities())if(entity instanceof Minecart&&!original.contains(entity.getUUID()))carts++;require(carts==1,"source initial minecart created once");
                marker.entityTags().removeIf(t->t.startsWith("skywars2.railroad.age."));marker.entityTags().add("skywars2.railroad.age.199");phase=2;return false;
            }
            require(marker.isRemoved()&&game.items().railroad().count()==0,"native source railtime200 terminates the builder");
            require(f.level.getBlockState(marker.blockPosition()).is(Blocks.OAK_WOOD)&&f.level.getBlockState(marker.blockPosition().above()).is(Blocks.OAK_WOOD),"source terminal two-block bumper");return true;
        }
        public void close(){try{f.close();}finally{stats.close();}}
    }
    public static void run(GameController game){require(active==null,"exclusive fixture");var scene=new Scene(game);active=scene;try{scene.begin();}catch(Throwable error){active=null;scene.close();throw error;}}
    public static void tick(GameController game){if(active==null)return;var scene=active;try{if(!scene.tick())return;active=null;scene.close();SkyWars2.LOGGER.info("[check] PASS railroad (real loot, right click/debit, native scheduled marker and cart, powered track, TimeStop pause, source200 boundary and bumper)");}catch(Throwable error){active=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL railroad",error);}}
}
