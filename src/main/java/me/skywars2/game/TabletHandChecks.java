package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Actual hand admission and a real scheduled countdown, independent of the boundary math suite. */
public final class TabletHandChecks {
    private TabletHandChecks(){}
    private static State running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Tablet hand: "+why);}
    private static final class State implements AutoCloseable {
        final GameController game;final AllGrimoireChecks.Fixture base;final ServerPlayer p;
        final RegenerationBeltChecks.ScheduledClient client;final boolean offhand;
        final Map<Item,Integer> stats=new IdentityHashMap<>();final List<Marker> fields=new ArrayList<>();
        int started,lastDuration;boolean closed;
        State(GameController game){this.game=game;base=new AllGrimoireChecks.Fixture(game);base.reset();p=base.base.owner;
            offhand=game.config().offhandWands;game.config().offhandWands=true;
            for(var item:List.of(LbcKitUtilityItems.QUINTUPLE,LbcKitUtilityItems.PALM,Items.CARROT_ON_A_STICK))stats.put(item,p.getStats().getValue(Stats.ITEM_USED.get(item)));
            client=new RegenerationBeltChecks.ScheduledClient(game,p);
        }
        ItemStack loot(boolean palm){var result=game.lbc().rollTable(p.level(),p.position(),p,"lbc:ported/"+(palm?"banana_palm":"multiply_5"));require(result.size()==1,"source leaf");return result.getFirst();}
        void hands(){p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);}
        void effects(){game.items().ownedEffects().forget(p);p.removeAllEffects();}
        boolean input(InteractionHand hand){client.acknowledge();var stack=p.getItemInHand(hand);
            if(stack.is(Items.CARROT_ON_A_STICK))return net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(p,p.level(),hand).consumesAction();
            return p.gameMode.useItem(p,p.level(),stack,hand).consumesAction();}
        Marker field(String tag){var m=new Marker(EntityTypes.MARKER,p.level());m.setPos(p.position());m.entityTags().add(tag);require(p.level().addFreshEntity(m),"native field");fields.add(m);game.items().magic().loaded(m);return m;}
        void remove(Marker m){game.items().magic().unloaded(m);m.discard();fields.remove(m);}
        void direct(){
            hands();effects();var effect=new MobEffectInstance(MobEffects.GLOWING,13,2,true,false,false);p.addEffect(effect);
            p.addEffect(new MobEffectInstance(MobEffects.GLOWING,100,-2,false,true,true));
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,-1,0));
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,40,0));p.setAbsorptionAmount(1);
            var active=p.getEffect(MobEffects.GLOWING);var hidden=((EffectLayers.Access)active).skywars2$layerHidden();
            var source=loot(false);p.setItemInHand(InteractionHand.OFF_HAND,source);
            int mana=game.items().mana().get(p);require(input(InteractionHand.OFF_HAND),"actual offhand native use");
            require(source.isEmpty()&&p.getMainHandItem().isEmpty()&&game.items().mana().get(p)==mana,"one own-hand debit/no mana");
            require(p.getEffect(MobEffects.GLOWING)==active&&active.getDuration()==65&&hidden.getDuration()==500&&active.isAmbient()&&!active.isVisible()&&!active.showIcon(),"exact clocks/identities/flags");
            require(p.getEffect(MobEffects.NIGHT_VISION).isInfiniteDuration()&&p.getAbsorptionAmount()==1,"infinite/no yellow refill");
            hands();effects();p.addEffect(new MobEffectInstance(MobEffects.GLOWING,7,0));
            p.setItemInHand(InteractionHand.MAIN_HAND,loot(false));p.setItemInHand(InteractionHand.OFF_HAND,loot(false));
            require(input(InteractionHand.MAIN_HAND)&&p.getMainHandItem().isEmpty()&&p.getOffhandItem().isEmpty()&&p.getEffect(MobEffects.GLOWING).getDuration()==175,"two actual hands each multiply once x25");
            hands();effects();p.addEffect(new MobEffectInstance(MobEffects.GLOWING,11,0));
            var nativeSource=loot(false);var raw=nativeSource.transmuteCopy(Items.CARROT_ON_A_STICK);
            raw.set(DataComponents.CUSTOM_DATA,nativeSource.get(DataComponents.CUSTOM_DATA));p.setItemInHand(InteractionHand.MAIN_HAND,raw);
            require(input(InteractionHand.MAIN_HAND)&&raw.isEmpty()&&p.getEffect(MobEffects.GLOWING).getDuration()==55,"actual raw legacy callback");
            hands();effects();p.setItemInHand(InteractionHand.MAIN_HAND,loot(false));require(input(InteractionHand.MAIN_HAND)&&p.getMainHandItem().isEmpty(),"source empty-effect debit");
        }
        void policies(){
            hands();effects();p.addEffect(new MobEffectInstance(MobEffects.GLOWING,30,0));var stack=loot(false);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var mag=field("stopper_magic");require(game.items().magic().suppressed(p),"MAG positive control");
            require(!input(InteractionHand.MAIN_HAND)&&!stack.isEmpty()&&p.getEffect(MobEffects.GLOWING).getDuration()==30,"MAG no debit or effect mutation");remove(mag);
            p.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();
            require(game.items().magic().suppressed(p),"actual artifact inventory positive control");
            require(!input(InteractionHand.MAIN_HAND)&&!stack.isEmpty(),"artifact denies");p.getInventory().setItem(9,ItemStack.EMPTY);game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();
            var tech=field("stopper");require(game.items().magic().mechanismSuppressed(p),"TECH positive control");
            require(input(InteractionHand.MAIN_HAND)&&stack.isEmpty()&&p.getEffect(MobEffects.GLOWING).getDuration()==150,"TECH permits tablet");remove(tech);
            p.setItemInHand(InteractionHand.MAIN_HAND,loot(false));game.items().timeStop().begin(p.level(),20);
            try{require(!input(InteractionHand.MAIN_HAND)&&!p.getMainHandItem().isEmpty(),"Time Stop denies without debit");}finally{game.items().timeStop().end(p.level());}
            boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
            try{require(!input(InteractionHand.MAIN_HAND)&&!p.getMainHandItem().isEmpty(),"global freeze denies");}finally{game.server().tickRateManager().setFrozen(frozen);}
            hands();game.config().offhandWands=false;p.setItemInHand(InteractionHand.OFF_HAND,loot(false));require(!input(InteractionHand.OFF_HAND)&&!p.getOffhandItem().isEmpty(),"configured offhand disabled");game.config().offhandWands=true;
        }
        void palm(){
            hands();effects();var mag=field("stopper_magic");var tech=field("stopper");p.entityTags().add("nomagic_active");
            var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());p.level().getAllEntities().forEach(before::add);
            var stack=loot(true);p.setItemInHand(InteractionHand.MAIN_HAND,stack);int mana=game.items().mana().get(p);
            require(input(InteractionHand.MAIN_HAND)&&stack.isEmpty()&&game.items().mana().get(p)==mana,"ordinary palm under MAG/TECH/artifact, one actual palm/no mana");
            var drops=new ArrayList<ItemEntity>();for(var entity:p.level().getAllEntities())if(!before.contains(entity)&&entity instanceof ItemEntity item)drops.add(item);
            int count=drops.stream().mapToInt(i->i.getItem().getCount()).sum();require(count>=3&&count<=6&&!drops.isEmpty(),"source banana quantity");
            for(var drop:drops){require(LbcFartus.data(drop.getItem()).getIntOr("eat_banana",0)==1&&drop.getItem().has(DataComponents.CONSUMABLE)&&drop.getOwner()==p&&game.items().currentEpoch(drop)&&Math.abs(drop.getY()-p.getY()-1)<1e-8,"native existing food/owner/epoch/source height");drop.discard();}
            hands();p.setItemInHand(InteractionHand.OFF_HAND,loot(true));require(!input(InteractionHand.OFF_HAND)&&!p.getOffhandItem().isEmpty(),"source main-hand-only palm retained");
            p.entityTags().remove("nomagic_active");remove(mag);remove(tech);
        }
        void beginClock(){hands();effects();p.addEffect(new MobEffectInstance(MobEffects.GLOWING,6,0));
            p.setItemInHand(InteractionHand.MAIN_HAND,loot(false));require(input(InteractionHand.MAIN_HAND),"clock tablet used");
            lastDuration=p.getEffect(MobEffects.GLOWING).getDuration();require(lastDuration==30,"exact native30");started=game.server().getTickCount();}
        boolean tick(){client.acknowledge();int elapsed=game.server().getTickCount()-started;require(elapsed<60,"bounded real clock");
            var effect=p.getEffect(MobEffects.GLOWING);if(effect==null){require(elapsed>=29&&elapsed<=31,"native expires at scaled boundary, elapsed="+elapsed);return true;}
            require(effect.getDuration()<=lastDuration&&effect.getDuration()>=Math.max(0,30-elapsed-1),"native scheduled clock progression");lastDuration=effect.getDuration();return false;}
        public void close(){if(closed)return;closed=true;try{for(var m:List.copyOf(fields))remove(m);game.items().timeStop().end(p.level());game.config().offhandWands=offhand;
            for(var entry:stats.entrySet())p.getStats().setValue(p,Stats.ITEM_USED.get(entry.getKey()),entry.getValue());}finally{try{client.close();}finally{base.close();}}}
    }
    public static void start(GameController game){require(running==null,"exclusive");var state=new State(game);try{state.direct();state.policies();state.palm();state.beginClock();running=state;SkyWars2.LOGGER.info("[check] START tablet-hand");}catch(Throwable error){state.close();throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.tick())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS tablet-hand (native/raw used-hand debit, two-hand x25, flags/hidden/infinite, absorption/no mana, MAG/artifact/TECH/freeze/offhand gates, unjammed physical palm/source bananas, actual scheduled expiry)");}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL tablet-hand",error);}}
}
