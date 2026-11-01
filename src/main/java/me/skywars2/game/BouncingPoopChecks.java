package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Finite real-scheduler observations; clocks/positions are read from native actors. */
public final class BouncingPoopChecks {
    private BouncingPoopChecks(){}
    private static State running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Bouncing poop: "+why);}
    private static final class State implements AutoCloseable {
        final GameController game;final AllGrimoireChecks.Fixture f;final ServerPlayer p,beta;
        final RegenerationBeltChecks.Snapshot saved,betaSaved;final RegenerationBeltChecks.ScheduledClient client,audience;
        final List<net.minecraft.world.entity.Marker> fields=new ArrayList<>();final Map<net.minecraft.world.entity.Marker,MapManager.BlockSnapshot> fieldBlocks=new IdentityHashMap<>();
        final LbcBouncingPoop service;Display.ItemDisplay flight;Vec3 start;int phase,started;boolean closed;
        final Map<net.minecraft.world.item.Item,Integer> stats=new IdentityHashMap<>();final boolean skills;
        State(GameController game){this.game=game;f=new AllGrimoireChecks.Fixture(game);p=f.base.owner;beta=f.beta;
            saved=new RegenerationBeltChecks.Snapshot(game,p);betaSaved=new RegenerationBeltChecks.Snapshot(game,beta);
            client=new RegenerationBeltChecks.ScheduledClient(game,p);audience=new RegenerationBeltChecks.ScheduledClient(game,beta);service=game.items().bouncingPoop();skills=game.config().lbcSkills;
            for(var item:List.of(LbcPursuitItems.BOUNCING_POOP,Items.SNOWBALL))stats.put(item,p.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(item)));}
        void position(ServerPlayer player,Vec3 at){player.teleportTo(f.level,at.x,at.y,at.z,Set.of(),0,0,true);player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);if(player==p)client.acknowledge();else audience.acknowledge();}
        void clear(){service.stop();f.reset();position(beta,f.at.add(10,0,0));p.getInventory().clearContent();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            game.players().get(p.getUUID()).lbcSkill=0;for(var pos:BlockPos.betweenClosed(BlockPos.containing(f.at).offset(-5,0,-5),BlockPos.containing(f.at).offset(5,6,7)))f.level.setBlock(pos,Blocks.AIR.defaultBlockState(),2|16|32);}
        ItemStack held(InteractionHand hand,boolean raw){var s=LbcPursuitItems.BOUNCING_POOP.getDefaultInstance().copyWithCount(2);if(raw){var copy=new ItemStack(Items.SNOWBALL,2);copy.set(DataComponents.CUSTOM_DATA,s.get(DataComponents.CUSTOM_DATA));copy.set(DataComponents.ITEM_MODEL,s.get(DataComponents.ITEM_MODEL));copy.set(DataComponents.CUSTOM_NAME,s.get(DataComponents.CUSTOM_NAME));s=copy;}p.setItemInHand(hand,s);return s;}
        boolean input(InteractionHand hand,boolean raw){client.acknowledge();return raw?net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(p,f.level,hand).consumesAction():p.gameMode.useItem(p,f.level,p.getItemInHand(hand),hand).consumesAction();}
        void begin(){clear();require(service.count()==0,"exclusive actor index");
            var loot=game.lbc().rollTable(f.level,f.at,p,"lbc:ported/bouncing_poop");require(loot.size()==1&&loot.getFirst().is(LbcPursuitItems.BOUNCING_POOP)&&LbcBouncingPoop.matches(loot.getFirst()),"canonical native source leaf");
            for(var hand:InteractionHand.values())for(boolean raw:new boolean[]{false,true}){clear();var s=held(hand,raw);var other=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;p.setItemInHand(other,LbcPursuitItems.BOUNCING_POOP.getDefaultInstance().copyWithCount(7));
                require(input(hand,raw)&&s.getCount()==1&&p.getItemInHand(other).getCount()==7&&service.count()==1,"native/raw exactly actual hand debit "+hand+" "+raw);
                var a=service.snapshot().getFirst();var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,a.registryAccess());a.saveWithoutId(output);var nativeData=output.buildResult();var item=nativeData.getCompoundOrEmpty("item");
                require(a.getClass()==Display.ItemDisplay.class&&item.getStringOr("id","").equals("minecraft:snowball")&&item.getCompoundOrEmpty("components").getStringOr("minecraft:item_model","").equals("lbc:3dpoop"),"plain native display source model");
                require(nativeData.getIntOr("interpolation_duration",-1)==1&&nativeData.getIntOr("teleport_duration",-1)==1&&nativeData.getStringOr("item_display","").equals("fixed"),"native interpolation/fixed pose source values");
                require(a.position().distanceTo(p.getEyePosition())<1e-8&&a.getXRot()==p.getXRot()-20&&a.getYRot()==p.getYRot(),"eyes/source initial pose");
                var image=new MatchEntityState(game,a);var savedPosition=a.position();var changed=LbcBouncingPoop.state(a).copy();changed.putInt("age",321);setState(a,changed);a.setPos(savedPosition.add(2,3,4));
                image.load(a);require(a.position().equals(savedPosition)&&LbcBouncingPoop.state(a).getIntOr("age",-1)==0,"canonical native actor image restores attached clock and pose");
                service.unloaded(a);require(service.count()==0,"unload removes only derived index");service.loaded(a);require(service.count()==1&&!a.isRemoved(),"native attachment reload rebuilds index");}
            clear();
            for(var tag:List.of("stopper_magic","stopper")){var m=new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityTypes.MARKER,f.level);m.setPos(p.position());m.entityTags().add(tag);fieldBlocks.put(m,game.maps().snapshot(f.level,m.blockPosition()));require(f.level.addFreshEntity(m),"real jammer");fields.add(m);game.items().magic().loaded(m);}
            p.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();
            require(game.items().magic().suppressed(p)&&game.items().magic().mechanismSuppressed(p)&&game.items().magic().artifactActive(p),"actual simultaneous MAG/TECH/artifact controls");
            held(InteractionHand.MAIN_HAND,false);require(input(InteractionHand.MAIN_HAND,false),"physical launch through all jammers");flight=service.snapshot().getFirst();start=flight.position();
            p.entityTags().add("time_stopped");game.items().timeStop().begin(f.level,100);started=game.server().getTickCount();phase=0;
        }
        boolean step(){client.acknowledge();audience.acknowledge();int elapsed=game.server().getTickCount()-started;require(elapsed<80,"bounded scheduler stage="+phase);
            if(phase==0){if(elapsed<3)return false;require(flight.position().equals(start)&&LbcBouncingPoop.state(flight).getIntOr("age",-1)==0,"world TimeStop freezes native flight and attached age");game.items().timeStop().end(f.level);p.entityTags().remove("time_stopped");phase=1;started=game.server().getTickCount();return false;}
            if(phase==1){if(elapsed<1)return false;require(flight.position().distanceTo(start)>.39&&LbcBouncingPoop.state(flight).getIntOr("age",0)>0,"real scheduler advances .4 curve and persisted clock even under all jammers");
                removeFields();p.getInventory().setItem(9,ItemStack.EMPTY);game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();
                service.stop();clear();held(InteractionHand.MAIN_HAND,false);require(input(InteractionHand.MAIN_HAND,false),"bounce launch");flight=service.snapshot().getFirst();
                flight.setPos(new Vec3(Math.floor(f.at.x)+.5,Math.floor(f.at.y)+2,Math.floor(f.at.z)+.75));flight.setYRot(0);flight.setXRot(0);start=flight.position();
                f.level.setBlock(BlockPos.containing(start.add(0,0,.8)),Blocks.STONE.defaultBlockState(),2|16|32);
                f.level.setBlock(BlockPos.containing(start.add(1,1,-1)),Blocks.ROOTED_DIRT.defaultBlockState(),2|16|32);
                f.level.setBlock(BlockPos.containing(start.add(-1,1,-1)),Blocks.PODZOL.defaultBlockState(),2|16|32);
                phase=2;started=game.server().getTickCount();return false;}
            if(phase==2){if(elapsed<1)return false;require(!flight.isRemoved()&&flight.getYRot()==-180,"scheduled wall bounce retains actor and reflects yaw");
                require(f.level.getBlockState(BlockPos.containing(start.add(1,1,-1))).is(Blocks.PODZOL)&&f.level.getBlockState(BlockPos.containing(start.add(-1,1,-1))).is(Blocks.COARSE_DIRT),"strict soil ladder advances each cell once");
                service.stop();clear();held(InteractionHand.MAIN_HAND,false);require(input(InteractionHand.MAIN_HAND,false),"body launch");flight=service.snapshot().getFirst();flight.setPos(f.at.add(0,2,0));flight.setYRot(0);flight.setXRot(0);
                position(beta,f.at.add(.5,2,.8));beta.removeAllEffects();phase=3;started=game.server().getTickCount();return false;}
            if(phase==3){if(!flight.isRemoved())return false;require(beta.hasEffect(MobEffects.NAUSEA)&&beta.hasEffect(MobEffects.WITHER),"actual one-cell body contact applies both native effects");
                require(beta.getEffect(MobEffects.WITHER).getAmplifier()==ItemEffectLevels.create(MobEffects.WITHER,60,10).getAmplifier(),"shared Wither cap");
                clear();game.config().lbcSkills=true;game.players().get(p.getUUID()).lbcSkill=9;held(InteractionHand.MAIN_HAND,false);require(input(InteractionHand.MAIN_HAND,false),"mega source skill launch");flight=service.snapshot().getFirst();require(LbcBouncingPoop.state(flight).getBooleanOr("mega",false),"attached accepted mega skill");
                flight.setPos(f.at.add(0,2,0));flight.setYRot(0);flight.setXRot(0);position(beta,f.at.add(.5,2,.8));beta.removeAllEffects();phase=4;started=game.server().getTickCount();return false;}
            if(phase==4){if(!flight.isRemoved())return false;require(beta.hasEffect(MobEffects.RESISTANCE)&&beta.getEffect(MobEffects.RESISTANCE).getAmplifier()==3,"source mega resistanceIV before native TNT explosion");
                var rubble=new ArrayList<net.minecraft.world.entity.item.FallingBlockEntity>();for(var entity:f.level.getAllEntities())if(entity instanceof net.minecraft.world.entity.item.FallingBlockEntity block&&block.entityTags().contains("skywars2.bouncing_poop.dirt"))rubble.add(block);
                require(rubble.size()==10&&rubble.stream().allMatch(b->b.getBlockState().is(Blocks.DIRT)),"ten shared native dirt children, one terminal mega impact");
                clear();held(InteractionHand.MAIN_HAND,false);require(input(InteractionHand.MAIN_HAND,false),"lifetime launch");flight=service.snapshot().getFirst();var d=LbcBouncingPoop.state(flight).copy();d.putInt("age",599);setState(flight,d);phase=5;started=game.server().getTickCount();return false;}
            if(!flight.isRemoved())return false;require(service.count()==0,"exact attached600 active ticks retire actor");return true;
        }
        void removeFields(){for(var m:fields){game.items().magic().unloaded(m);m.discard();var old=fieldBlocks.remove(m);if(old!=null&&f.level.getBlockState(m.blockPosition()).is(LbcMagic.MECHANISM_BEACON_BLOCK))game.maps().restoreSnapshot(f.level,m.blockPosition(),old);game.items().magic().blockChanged(m.blockPosition(),f.level.getBlockState(m.blockPosition()));}fields.clear();}
        void setState(Display.ItemDisplay actor,net.minecraft.nbt.CompoundTag d){try{var field=LbcBouncingPoop.class.getDeclaredField("DATA");field.setAccessible(true);@SuppressWarnings("unchecked") var type=(net.fabricmc.fabric.api.attachment.v1.AttachmentType<net.minecraft.nbt.CompoundTag>)field.get(null);actor.setAttached(type,d);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
        public void close(){if(closed)return;closed=true;try{removeFields();game.items().timeStop().end(f.level);p.entityTags().remove("time_stopped");service.stop();client.close();audience.close();f.close();}finally{game.config().lbcSkills=skills;saved.restore(game);saved.restoreAttributes();saved.restoreUse();betaSaved.restore(game);betaSaved.restoreAttributes();betaSaved.restoreUse();game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();stats.forEach((item,n)->p.getStats().setValue(p,net.minecraft.stats.Stats.ITEM_USED.get(item),n));}}
    }
    public static void start(GameController game){require(running==null,"exclusive fixture");var s=new State(game);try{s.begin();running=s;SkyWars2.LOGGER.info("[check] START bouncing-poop");}catch(Throwable e){s.close();throw e;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS bouncing-poop (native/raw actual hands, source visual, attachment unload/reload, scheduler TimeStop/curve/wall/strict soil/body/cap/lifetime)");}catch(Throwable e){try{running.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL bouncing-poop",e);}}
}
