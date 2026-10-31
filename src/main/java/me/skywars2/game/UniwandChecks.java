package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Finite native source composition and actual raw two-hand dispatch, with shared payload evidence. */
public final class UniwandChecks {
    private UniwandChecks(){}
    private static State running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Uni Wand: "+why);}
    private static final class State implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final ServerPlayer p;
        final List<ItemEntity> drops=new ArrayList<>();final int rawStats,nativeStats;final MatchRandomState random,playerRandom;
        ItemEntity wand,ruby,fire,competitor;int started;
        final Map<String,Boolean> criteria=new LinkedHashMap<>();
        State(GameController game){this.game=game;base=new ElementalGrimoireChecks.Fixture(game);p=base.owner;
            rawStats=p.getStats().getValue(Stats.ITEM_USED.get(Items.CARROT_ON_A_STICK));nativeStats=p.getStats().getValue(Stats.ITEM_USED.get(LbcUniwand.ITEM));
            random=MatchRandomState.capture(p.level().getRandom());playerRandom=MatchRandomState.capture(p.getRandom());
            var advancement=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/legends/uniwand"));
            require(advancement!=null,"original legend is packaged");
            var progress=p.getAdvancements().getOrStartProgress(advancement);
            for(var criterion:advancement.value().criteria().keySet())criteria.put(criterion,progress.getCriterion(criterion).isDone());
        }
        ItemStack raw(){var loot=game.lbc().rollTable(p.level(),p.position(),p,"lbc:uniwand");require(loot.size()==1&&LbcUniwand.matches(loot.getFirst()),"actual public source loot");
            var result=loot.getFirst();require(result.get(DataComponents.ITEM_MODEL).equals(Identifier.parse("lbc:uniwand"))&&result.get(DataComponents.LORE).lines().size()==5,"source model/five lore rows");return result;}
        ItemEntity drop(ItemStack stack,Vec3 at){var actor=new ItemEntity(p.level(),at.x,at.y,at.z,stack);actor.setNoGravity(true);actor.setDeltaMovement(Vec3.ZERO);actor.setPickUpDelay(1000);actor.setThrower(p);
            game.items().markCurrentItemEpoch(actor);require(p.level().addFreshEntity(actor),"native drop admission");drops.add(actor);return actor;}
        void initialize(){base.initialize();started=game.server().getTickCount();
            var at=p.position().add(3,0,0);wand=drop(raw(),at);
            var a=base.loot(LbcElementalGrimoires.Kind.RUBY);a.setCount(10);ruby=drop(a,at.add(.15,0,0));
            fire=drop(base.loot(LbcElementalGrimoires.Kind.FIRE),at.add(.7,0,0));
            // This wand shares the same books: actual source tick must consume each unit once.
            competitor=drop(raw(),at.add(.3,0,.2));
        }
        boolean input(InteractionHand hand){
            var teleports=base.recording.packets.stream().filter(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::isInstance)
                    .map(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::cast).toList();
            if(!teleports.isEmpty())p.connection.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(teleports.getLast().id(),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot()));
            p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());return base.input(hand);
        }
        void casts(ItemStack assembled){
            for(boolean legacy:new boolean[]{false,true})for(var hand:InteractionHand.values()){
                base.clear();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                p.setItemInHand(hand,legacy?assembled.transmuteCopy(Items.CARROT_ON_A_STICK):assembled.copy());var held=p.getItemInHand(hand).copy();int cost=LbcFartus.data(held).getIntOr("mana",-1);
                game.items().mana().set(p,cost);require(input(hand),"actual native raw hand equality input "+hand);
                require(game.items().mana().get(p)==0&&ItemStack.matches(held,p.getItemInHand(hand)),"one total debit and persistent wand "+hand);
                require(game.items().ruby().count()==10&&base.fire().size()==3,"all eleven stored slots reach existing Ruby/Fire owners "+hand);
                for(var bolt:game.items().ruby().snapshot())require(bolt.getOwner()==p&&game.items().currentEpoch(bolt),"actual native caster attribution "+hand);
            }
            base.clear();p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,assembled.copy());
            int cost=LbcFartus.data(assembled).getIntOr("mana",0);game.items().mana().maximum(p,cost);game.items().mana().set(p,cost-1);
            require(!input(InteractionHand.MAIN_HAND)&&game.items().mana().get(p)==cost-1&&base.count()==0,"ordinary insufficient mana rejected when maximum covers cost");
            game.items().mana().maximum(p,cost-1);game.items().mana().set(p,17);
            require(input(InteractionHand.MAIN_HAND)&&game.items().mana().get(p)==17-cost,"documented source maximum overdraft retains real signed debit");
            var advancement=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/legends/uniwand"));
            require(p.getAdvancements().getOrStartProgress(advancement).isDone(),"original overdraft legend awarded");base.clear();
            require(input(InteractionHand.MAIN_HAND)&&game.items().mana().get(p)==17-2*cost,"source overdraft also casts from an already negative balance");base.clear();
            p.entityTags().add("time_stopped");try{int before=game.items().mana().get(p);require(!input(InteractionHand.MAIN_HAND)&&game.items().mana().get(p)==before&&base.count()==0,"frozen input keeps mana/slots");}finally{p.entityTags().remove("time_stopped");}
            var empty=raw();p.setItemInHand(InteractionHand.MAIN_HAND,empty);game.items().mana().set(p,0);
            require(input(InteractionHand.MAIN_HAND)&&base.count()==0&&game.items().mana().get(p)==0,"empty wand has no stale slot effect");
        }
        boolean step(){
            require(game.playing(),"actual match retained");int elapsed=game.server().getTickCount()-started;
            int total=LbcFartus.data(wand.getItem()).getIntOr("spellcount",0)+LbcFartus.data(competitor.getItem()).getIntOr("spellcount",0);
            if(total==0){require(elapsed<25,"actual one-second composition hook ran");return false;}
            require(total==11&&ruby.isRemoved()&&fire.isRemoved(),"adjacent wands consume eleven book units once");
            var filled=LbcFartus.data(wand.getItem()).getIntOr("spellcount",0)>0?wand:competitor;
            var data=LbcFartus.data(filled.getItem());require(data.getIntOr("spellcount",0)==11,"one source carrier absorbs full finite neighborhood");
            // max(1,floor(n/5)): n1..9 Ruby:1200; n10 Ruby:600; n11 Fire:100.
            require(data.getIntOr("mana",0)==11500&&data.getIntOr("value",0)==110,"independent integer divisor-at-five/ten and tier arithmetic");
            require(filled.getItem().get(DataComponents.TOOLTIP_STYLE).equals(Identifier.parse("lbc:tier_b")),"namespaced native tier transition");
            for(int n=1;n<=10;n++)require(data.getIntOr("spell"+n,0)==160,"nearest-first native slot "+n);
            require(data.getIntOr("spell11",0)==181,"last Fire slot preserved");
            // The native ItemStack copy is the same representation saved by item entities, inventories and rewind.
            casts(filled.getItem().copy());return true;
        }
        public void close(){for(var drop:drops){game.items().entityUnloaded(drop);drop.discard();}
            base.clear();var a=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/legends/uniwand"));
            for(var entry:criteria.entrySet())if(entry.getValue())p.getAdvancements().award(a,entry.getKey());else p.getAdvancements().revoke(a,entry.getKey());
            random.restore(p.level().getRandom());playerRandom.restore(p.getRandom());
            p.getStats().setValue(p,Stats.ITEM_USED.get(Items.CARROT_ON_A_STICK),rawStats);p.getStats().setValue(p,Stats.ITEM_USED.get(LbcUniwand.ITEM),nativeStats);base.close();}
    }
    public static void run(GameController game){require(running==null,"already running");var state=new State(game);running=state;try{state.initialize();}catch(RuntimeException|Error e){running=null;state.close();throw e;}}
    public static void tick(GameController game){if(running==null)return;var state=running;try{if(state.step()){state.close();running=null;SkyWars2.LOGGER.info("[check] PASS uniwand (actual one-second adjacent-wand composition; native raw two-hand casts; eleven shared payloads; equality; signed maximum overdraft/legend; frozen and empty inputs)");}}
        catch(Throwable error){running=null;try{state.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL uniwand",error);}}
}
