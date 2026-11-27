package me.skywars2.game;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import me.skywars2.SkyWars2;
import me.skywars2.game.items.drops.LbcFeedbackDropItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

/** Real collision before the shared END tick: expired pickup delay cannot beat delayed activation. */
public final class DropActivationChecks {
    private static Scene running;private static boolean registered;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Drop activation: "+why);}
    public static void run(GameController game){
        require(running==null,"exclusive scene");
        if(!registered){registered=true;ServerTickEvents.START_SERVER_TICK.register(server->{var s=running;if(s!=null&&s.game.server()==server)s.beforeTick();});}
        running=new Scene(game);
    }
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;
        ItemEntity token;ItemStack source;int phase,when,frames;boolean closed;Throwable failure;
        final java.util.List<ItemEntity> backlog=new java.util.ArrayList<>();
        int backlogStarted;long initialDispatchNanos;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);f.prepareParticipants();begin();}
        void begin(){
            f.owner.getInventory().clearContent();
            source=new ItemStack(LbcFeedbackDropItem.Kind.YAMETE.item(),6);
            var at=f.owner.position().add(6,0,0);token=new ItemEntity(f.level,at.x,at.y,at.z,source.copy());
            token.setNoGravity(true);token.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);token.setPickUpDelay(0);
            require(f.level.addFreshEntity(token),"actual activator insertion");f.own(token);f.pause();
            trackingBounce();
            phase=0;when=game.server().getTickCount();
        }
        void trackingBounce(){
            var manager=((me.skywars2.mixin.entities.ActorCheckpointAccess.LevelEntities)f.level).skywars2$manager();
            try{
                var stop=manager.getClass().getDeclaredMethod("stopTracking",net.minecraft.world.level.entity.EntityAccess.class);
                var start=manager.getClass().getDeclaredMethod("startTracking",net.minecraft.world.level.entity.EntityAccess.class);
                stop.setAccessible(true);start.setAccessible(true);
                for(int n=0;n<128;n++){
                    stop.invoke(manager,token);require(!token.isRemoved(),"native tracking-end preserves the same live Java body");
                    start.invoke(manager,token);require(f.level.getEntity(token.getUUID())==token,"native tracking-start restores exactly that body");
                }
                var queue=(java.util.Collection<?>)WoolBucketChecks.read(game.items().specialDrops(),"activationQueue");
                require(queue.stream().filter(entry->entry==token).count()==1,"128 real native visibility bounces retain one fair-queue slot for the same body");
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }
        int picked(){int result=0;for(int i=0;i<f.owner.getInventory().getContainerSize();i++){
            var stack=f.owner.getInventory().getItem(i);if(ItemStack.isSameItemSameComponents(stack,source))result+=stack.getCount();
        }return result;}
        void beforeTick(){
            if(failure!=null||phase!=1)return;
            try{token.playerTouch(f.owner);}catch(Throwable error){failure=error;}
        }
        boolean step(){
            if(failure!=null)throw new IllegalStateException("native pre-tick collision",failure);
            require(++frames<400,"bounded native activation/pickup and overload queue");
            if(phase==3){
                if(backlog.stream().allMatch(ItemEntity::isRemoved)){
                    require(game.server().getTickCount()>backlogStarted,"overload defers across real native frames");
                    SkyWars2.LOGGER.info("[check] drop burst drained {} native activators in {} ticks; initialDispatchMs={}",backlog.size(),game.server().getTickCount()-backlogStarted,initialDispatchNanos/1_000_000.0);
                    return true;
                }
                return false;
            }
            if(phase==0){
                token.playerTouch(f.owner);
                require(!token.isRemoved()&&token.getItem().getCount()==6&&picked()==0,"expired raw delay cannot bypass pending TimeStop readiness");
                if(game.server().getTickCount()-when<8)return false;
                f.thaw();require(game.items().specialDrops().spawn(token),"first eligible tick admits first unit");
                require(token.getItem().getCount()==5,"one initial debit");
                token.playerTouch(f.owner);require(picked()==0,"same-tick collision cannot activate or pick a second unit");
                phase=1;when=game.server().getTickCount();return false;
            }
            int elapsed=game.server().getTickCount()-when;
            if(elapsed<5){require(picked()==0&&!token.isRemoved(),"first five real activations retain pickup protection");return false;}
            require(token.isRemoved()&&picked()==1,"sixth tick picks the Fortune remainder");
            for(int i=0;i<128;i++){
                var stack=new ItemStack(LbcFeedbackDropItem.Kind.YAMETE.item());var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                data.putInt("check_burst_identity",i);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
                var at=f.owner.position().add(6,0,0);var item=new ItemEntity(f.level,at.x,at.y,at.z,stack);
                item.setNoGravity(true);item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);item.setPickUpDelay(0);
                require(f.level.addFreshEntity(item),"real independent burst token");f.own(item);backlog.add(item);
            }
            backlogStarted=game.server().getTickCount();long started=System.nanoTime();int admitted=0;
            for(var item:backlog)if(game.items().specialDrops().spawn(item))admitted++;
            initialDispatchNanos=System.nanoTime()-started;
            require(admitted<=16&&backlog.stream().anyMatch(item->!item.isRemoved()),"one real frame cannot dispatch the entire burst");
            var waiting=backlog.stream().filter(item->!item.isRemoved()).findFirst().orElseThrow();
            require(game.items().specialDrops().blocksPickup(waiting),"overloaded token retains pickup protection");
            phase=3;return false;
        }
        public void close(){if(closed)return;closed=true;f.close();}
    }
    public static void tick(GameController game){if(running==null)return;var s=running;
        try{if(!s.step())return;s.close();running=null;SkyWars2.LOGGER.info("[check] PASS drop-activation (delayed readiness, first eligible activation, one per native tick, Fortune6 remainder and real pre-dispatch pickup)");}
        catch(Throwable error){try{s.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL drop-activation",error);}
    }
}
