package me.skywars2.game;

import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import me.skywars2.SkyWars2;
import me.skywars2.game.items.drops.LbcFeedbackDropItem;
import me.skywars2.game.items.tmt.LbcTmtAbilities;
import me.skywars2.game.items.tmt.LbcTmtItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
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
        ItemEntity token;ItemStack source;int phase,when,round,frames;boolean closed;Throwable failure;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);f.prepareParticipants();begin(false);}
        void begin(boolean reusable){
            f.owner.getInventory().clearContent();
            if(!reusable)source=new ItemStack(LbcFeedbackDropItem.Kind.YAMETE.item(),6);
            else{
                source=LbcTmtItems.prototype(0).copyWithCount(6);var root=new CompoundTag();
                root.putInt("tmt_item",1);root.putInt("tmt_consumable",0);root.putInt("tmt_drop",1);
                var property=new CompoundTag();property.putInt("tmt_drop",1);property.putInt("drop",1);property.putInt("yameteluckyblock",1);
                LbcTmtAbilities.write(root,List.of(new LbcTmtAbilities.Ability("check/yamete_drop",CustomData.of(property))));
                source.set(DataComponents.CUSTOM_DATA,CustomData.of(root));
            }
            var at=f.owner.position().add(6,0,0);token=new ItemEntity(f.level,at.x,at.y,at.z,source.copy());
            token.setNoGravity(true);token.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);token.setPickUpDelay(0);
            require(f.level.addFreshEntity(token),"actual activator insertion");f.own(token);f.pause();
            phase=0;when=game.server().getTickCount();
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
            require(++frames<100,"bounded native activation/pickup");
            if(phase==0){
                token.playerTouch(f.owner);
                require(!token.isRemoved()&&token.getItem().getCount()==6&&picked()==0,"expired raw delay cannot bypass pending TimeStop readiness");
                if(game.server().getTickCount()-when<8)return false;
                f.thaw();require(game.items().specialDrops().spawn(token),"first eligible tick admits first unit");
                require(token.getItem().getCount()==(round==0?5:6),"one initial debit, reusable TMT keeps its count");
                token.playerTouch(f.owner);require(picked()==0,"same-tick collision cannot activate or pick a second unit");
                phase=1;when=game.server().getTickCount();return false;
            }
            int elapsed=game.server().getTickCount()-when;
            if(elapsed<5){require(picked()==0&&!token.isRemoved(),"first five real activations retain pickup protection");return false;}
            require(token.isRemoved()&&picked()==(round==0?1:6),"sixth tick picks the Fortune remainder or intact reusable TMT");
            if(round++==0){begin(true);return false;}return true;
        }
        public void close(){if(closed)return;closed=true;f.close();}
    }
    public static void tick(GameController game){if(running==null)return;var s=running;
        try{if(!s.step())return;s.close();running=null;SkyWars2.LOGGER.info("[check] PASS drop-activation (delayed readiness, first eligible activation, one per native tick, Fortune6 remainder, reusable TMT and real pre-dispatch pickup)");}
        catch(Throwable error){try{s.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL drop-activation",error);}
    }
}
