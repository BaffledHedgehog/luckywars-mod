package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** End-to-end native match transaction. Only the disposable qualification server invokes it. */
public final class SupernovaChecks {
    private SupernovaChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Supernova: "+why);}
    public static void run(GameController game){
        require(running==null,"exclusive fixture");running=new Fixture(game);
        try{running.begin();}catch(Throwable e){running.close();running=null;throw new IllegalStateException(e);}
    }
    public static void tick(GameController game){
        if(running==null)return;
        try{if(!running.step())return;running.close();running=null;
            SkyWars2.LOGGER.info("[check] PASS supernova-chain (real grid exact pair, active-event exclusion, native TimeStop, complete map renewal, same epoch/inventory/XP/lives/effects, Nexus return, owner-only Balance reward and real full legacy transition)");
        }catch(Throwable e){try{running.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL supernova-chain",e);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,other;final boolean borders;
        final String epoch;int state,at,start,clock,lives,otherLives,xp;boolean closed;
        Fixture(GameController game){
            this.game=game;require(game.playing(),"actual active match");
            owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            other=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            borders=game.config().noBorders;epoch=game.items().epoch();start=at=game.server().getTickCount();
        }
        ItemStack loot(String name){var items=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/"+name);require(items.size()==1,"canonical "+name);return items.getFirst();}
        void move(ServerPlayer p,net.minecraft.server.level.ServerLevel level,Vec3 at){p.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);}
        void begin(){
            game.config().noBorders=true;for(var p:List.of(owner,other)){p.closeContainer();p.inventoryMenu.getCraftSlots().clearContent();p.getInventory().clearContent();p.removeAllEffects();}
            move(owner,game.maps().level(),new Vec3(1024.5,1000,1024.5));
            var nexus=game.server().getLevel(LbcHedgehogDimensions.NEXUS);require(nexus!=null,"native Nexus dimension");move(other,nexus,new Vec3(1,2332,1));
            owner.getInventory().setItem(8,new ItemStack(Items.DIAMOND,7));other.getInventory().setItem(8,new ItemStack(Items.EMERALD,9));
            owner.setExperienceLevels(17);xp=owner.experienceLevel;lives=game.lives(owner.getUUID());otherLives=game.lives(other.getUUID());
            owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION,12000,1,false,false),owner);
            owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,12000,2,false,false),owner);
            owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,12000,30,false,false),owner);
            var katana=loot("shit_katana");katana.setCount(2);var gun=loot("shishgun");gun.setCount(3);
            require(gun.is(LbcChargedBowItems.SHISHGUN)&&net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(owner.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY),gun)==1,"source native Shishgun Infinity retained");
            owner.inventoryMenu.getCraftSlots().setItem(0,katana);owner.inventoryMenu.getCraftSlots().setItem(1,gun);game.items().supernova().craftChanged(owner);
            game.items().timeStop().begin(owner.level(),200);clock=game.items().supernova().activeTicks();
        }
        boolean step(){
            int now=game.server().getTickCount();var service=game.items().supernova();require(now-start<2200,"bounded full chain scheduler timeout: fixture="+state+", event="+service.phase()+", map="+game.supernovaMapTransition());
            if(state==0){if(now-at<8)return false;require(service.phase()==LbcSupernova.Phase.IDLE&&service.activeTicks()==clock,"stopped activation and timer");require(owner.inventoryMenu.getCraftSlots().getItem(0).getCount()==2,"stopped recipe debit absent");game.items().timeStop().end(owner.level());state=1;at=now;return false;}
            if(state==1){if(service.phase()==LbcSupernova.Phase.IDLE){require(now-at<80,"event admitted from real crafting slots");return false;}
                require(owner.inventoryMenu.getCraftSlots().getItem(0).getCount()==1&&owner.inventoryMenu.getCraftSlots().getItem(1).getCount()==2,"one katana and one gun only");
                var id=service.eventId();service.craftChanged(owner);require(Objects.equals(id,service.eventId()),"busy service retains single event");
                owner.inventoryMenu.getCraftSlots().clearContent();service.craftChanged(owner);clock=service.activeTicks();game.items().timeStop().begin(owner.level(),200);state=2;at=now;return false;}
            if(state==2){if(now-at<8)return false;require(service.activeTicks()==clock,"running event paused by real TimeStop");game.items().timeStop().end(owner.level());state=3;at=now;return false;}
            if(state==3){if(service.phase()!=LbcSupernova.Phase.IDLE||game.supernovaMapTransition())return false;
                require(game.items().epoch().equals(epoch),"same match epoch after Supernova");require(game.lives(owner.getUUID())==lives&&game.lives(other.getUUID())==otherLives,"same lives");
                require(owner.experienceLevel==xp&&owner.getInventory().getItem(8).is(Items.DIAMOND)&&owner.getInventory().getItem(8).getCount()==7,"XP and inventory retained");
                require(other.level()==game.maps().level()&&other.getInventory().getItem(8).is(Items.EMERALD),"Nexus player returned with items");
                require(owner.hasEffect(MobEffects.REGENERATION)&&ShadowAbsorption.state(owner).entries.containsKey(2)&&ShadowAbsorption.state(owner).entries.containsKey(30),"foreign native effects and same-amp absorption baseline retained");
                int reward=0;for(var stack:java.util.stream.IntStream.range(0,owner.getInventory().getContainerSize()).mapToObj(owner.getInventory()::getItem).toList())if(LbcBalanceItems.isBalance(stack))reward+=stack.getCount();require(reward==1,"one owner reward");
                for(var stack:java.util.stream.IntStream.range(0,other.getInventory().getContainerSize()).mapToObj(other.getInventory()::getItem).toList())require(!LbcBalanceItems.isBalance(stack),"no foreign reward");
                owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcBalanceItems.BALANCE));
                require(net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(owner,owner.level(),InteractionHand.MAIN_HAND).consumesAction(),"real Balance item callback");state=4;at=now;return false;}
            if(state==4){if(!game.balanceLegacy()||game.supernovaMapTransition())return false;
                require(!game.items().epoch().equals(epoch)&&game.maps().spawns().size()==12,"new original overlay with exact twelve source spawns");
                require(owner.level()==game.maps().level()&&other.level()==game.maps().level()&&game.lives(owner.getUUID())==1&&game.lives(other.getUUID())==1,"old map cohorts and source one life");
                require(owner.getInventory().getItem(8).isEmpty()&&!owner.hasEffect(MobEffects.REGENERATION),"explicit Balance inventory/effect wipe");
                require(owner.experienceLevel==xp,"source Balance keeps existing XP");return true;}
            return false;
        }
        public void close(){if(closed)return;closed=true;game.config().noBorders=borders;game.items().timeStop().end(game.maps().level());}
    }
}
