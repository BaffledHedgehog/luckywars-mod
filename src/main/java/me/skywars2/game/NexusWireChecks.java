package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.*;

/** Real inventory scheduler, authoritative resource grants and native cross-world XP production. */
public final class NexusWireChecks {
    private NexusWireChecks(){}
    private static Fixture running;
    static void require(boolean v,String why){if(!v)throw new IllegalStateException("Nexus red wire: "+why);}
    static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.init();}catch(Throwable t){running.close();running=null;throw t;}}
    static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS nexus-red-wire (native Polymer loot, protected inventory/mainhand, one debit, all7 real charges and untouched progress, XP/mana/nanobots, native Nexus XP before merge, per-orb values, paused checkpoint continuation, excluded dimensions, native black gateway/temporary tickets, actual high-altitude Nexus entry and cleanup)");}
        catch(Throwable t){try{running.close();}catch(Throwable c){t.addSuppressed(c);}running=null;SkyWars2.LOGGER.error("[check] FAIL nexus-red-wire",t);}}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture base;final LbcNexusWires wires;final LbcSourceCounters counters;
        final Map<String,Integer> savedCharges=new LinkedHashMap<>();
        final Set<Entity> owned=new HashSet<>();
        final int xp,xpLevel,xpDelay;final float xpProgress;
        RegenerationBeltChecks.ScheduledClient client;
        final ServerLevel original;ServerLevel nexus;Vec3 origin;ExperienceOrb paused;int phase,age,wait;
        net.minecraft.core.BlockPos gatewayPosition;
        boolean closed;
        Fixture(GameController game){this.game=game;base=new ChefChecks.Fixture(game);wires=game.items().nexusWires();counters=new LbcSourceCounters(game);
            for(String name:List.of("swordcharged1","swordcharged2","swordcharged5","swordcharging1","swordcharging5"))savedCharges.put(name,counters.read(base.owner,name));
            var p=base.owner;original=p.level();xp=p.totalExperience;xpLevel=p.experienceLevel;xpDelay=p.takeXpDelay;xpProgress=p.experienceProgress;}
        void init(){base.init();client=new RegenerationBeltChecks.ScheduledClient(game,base.owner);game.config().lbcSkills=true;game.players().get(base.owner.getUUID()).lbcSkill=2;
            require(LbcNexusWireItems.RED instanceof eu.pb4.polymer.core.api.item.PolymerItem,"native Polymer item");
            base.mana.baseRegen(base.owner,0);base.mana.set(base.owner,100);base.mana.score(base.owner,"nanobots_tier").set(0);
            for(int i=1;i<=7;i++)base.mana.score(base.owner,"swordcharged"+i).set(10+i);
            game.items().electro().sourceCharge(base.owner,false,false,11);game.items().electro().sourceCharge(base.owner,true,false,15);game.items().terra().sourceCharges(base.owner,12);
            game.items().electro().sourceCharge(base.owner,false,true,3);game.items().electro().sourceCharge(base.owner,true,true,4);
            base.owner.getInventory().setItem(9,new ItemStack(LbcNexusWireItems.RED));base.mana.inventoryChanged(base.owner);
            SkyWars2.LOGGER.info("[check] START nexus-red-wire");}
        void transfer(ServerLevel level){var p=base.owner;p.teleportTo(level,base.at.x,Math.min(240,level.getMaxY()-20),base.at.z,Set.of(),0,0,true);client.acknowledge();p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);}
        ExperienceOrb orb(int value,Vec3 at,boolean stop){var e=new ExperienceOrb(nexus,at,Vec3.ZERO,value);e.setNoGravity(true);if(stop)e.entityTags().add("time_stopped");owned.add(e);require(nexus.addFreshEntity(e),"native XP publication accepted");return e;}
        int redDrops(){var items=nexus.getEntitiesOfClass(ItemEntity.class,new AABB(origin,origin).inflate(14),e->LbcNexusWireItems.isRed(e.getItem()));owned.addAll(items);return items.stream().mapToInt(e->e.getItem().getCount()).sum();}
        boolean step(){require(++age<240,"bounded native phases "+phase);wait++;client.acknowledge();var p=base.owner;
            if(phase==0&&wait>=3){require(p.getInventory().getItem(9).getCount()==1,"protected inventory does not eat carried wire");p.getInventory().setItem(9,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(LbcNexusWireItems.RED));base.mana.inventoryChanged(p);phase=1;wait=0;
            }else if(phase==1&&wait>=3){require(!p.getOffhandItem().isEmpty(),"main-hand-only source guard");p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcNexusWireItems.RED,2));p.getInventory().setItem(9,new ItemStack(LbcNexusWireItems.RED));base.mana.inventoryChanged(p);phase=2;wait=0;
            }else if(phase==2){
                require(p.getMainHandItem().getCount()==1&&p.getInventory().getItem(9).getCount()==1,"exact one global debit per native tick");
                require(p.totalExperience==xp+20&&base.mana.get(p)==5100,"actual20 XP and5000 mana");
                for(int i=1;i<=7;i++)require(counters.read(p,"swordcharged"+i)==15+i,"real charge owner "+i);
                require(game.items().electro().charging(p)==3&&game.items().electro().yellowCharging(p)==4,"charging progress remains distinct");
                require(game.items().armorSets().nanobots().tier(p)==100,"native nanobot tier grant");
                p.getInventory().clearContent();base.mana.inventoryChanged(p);nexus=game.server().getLevel(LbcHedgehogDimensions.NEXUS);require(nexus!=null,"actual Nexus world");transfer(nexus);origin=p.position().add(7,0,0);phase=3;wait=0;
            }else if(phase==3){
                boolean ready=true;for(int x=(int)Math.floor((origin.x-14)/16);x<=(int)Math.floor((origin.x+14)/16);x++)for(int z=(int)Math.floor((origin.z-14)/16);z<=(int)Math.floor((origin.z+14)/16);z++)if(nexus.getChunkSource().getChunkNow(x,z)==null||!nexus.areEntitiesLoaded(ChunkPos.pack(x,z))){game.maps().prepareChunk(nexus,new ChunkPos(x,z));ready=false;}
                if(!ready)return false;
                require(!CoreEntityMerging.enabled(nexus),"Nexus retains individual source XP awards");
                var one=orb(1,origin,false);var large=orb(1000000,origin.add(.1,0,0),false);
                require(one.isRemoved()&&large.isRemoved()&&redDrops()==2,"immediate one wire per entity, independent of XP value/merge radius: one="+one.isRemoved()+",large="+large.isRemoved()+",drops="+redDrops());
                for(var e:owned)if(e instanceof ItemEntity)require(e.isNoGravity(),"Nexus reward remains afloat");
                paused=orb(7,origin.add(0,0,2),true);require(!paused.isRemoved(),"frozen XP waits without merging");phase=4;wait=0;
            }else if(phase==4&&wait>=3){
                require(!paused.isRemoved()&&redDrops()==2,"native paused producer retains its body");
                var cp=wires.checkpoint();wires.prepareFrozen();var plan=cp.prepare(nexus::getEntity);require(plan.pending().isEmpty(),"checkpoint rebinds actual pending XP");plan.install().run();wires.publishFrozen();paused.entityTags().remove("time_stopped");phase=5;wait=0;
            }else if(phase==5){
                if(!paused.isRemoved())return false;require(redDrops()==3,"resumed pending XP converts once");
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcNexusWireItems.RED));base.mana.inventoryChanged(p);phase=6;wait=0;
            }else if(phase==6&&wait>=3){
                require(p.getMainHandItem().getCount()==1,"wire remains an ingredient inside Nexus");
                var imprinted=game.server().getLevel(LbcHedgehogDimensions.IMPRINTED);require(imprinted!=null,"actual Imprinted world");transfer(imprinted);phase=7;wait=0;
            }else if(phase==7&&wait>=3){
                require(p.getMainHandItem().getCount()==1,"wire cannot activate inside Imprinted");p.getInventory().clearContent();base.mana.inventoryChanged(p);transfer(original);
                var e=new ExperienceOrb(p.level(),p.position().add(8,0,0),Vec3.ZERO,7);owned.add(e);require(!wires.convert(e),"ordinary XP is not a Nexus producer");
                base.move(p,base.at.add(-12,0,-12));p.getInventory().setItem(9,new ItemStack(LbcNexusWireItems.BLACK));base.mana.inventoryChanged(p);phase=8;wait=0;
            }else if(phase==8&&wait>=3){
                require(p.getInventory().getItem(9).getCount()==1,"protected carried black wire remains intact");p.getInventory().setItem(9,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcNexusWireItems.BLACK));base.mana.inventoryChanged(p);phase=9;wait=0;
            }else if(phase==9){
                require(p.getMainHandItem().isEmpty()&&wires.pendingGateways().size()==1,"one black wire accepts one native gateway job");
                var job=wires.pendingGateways().getFirst();var at=job.position;
                require(at.getX()>=-2000&&at.getX()<=2000&&at.getY()>=0&&at.getY()<320&&at.getZ()>=-2000&&at.getZ()<=2000,"original current-world random portal coordinates");
                gatewayPosition=base.center.offset(0,3,0);job.position=gatewayPosition;
                require(!wires.captureReady(),"full save point waits for accepted terrain mutation");phase=10;wait=0;
            }else if(phase==10){
                if(!wires.captureReady())return false;
                require(wires.loadingChunks()==0&&original.getBlockEntity(gatewayPosition) instanceof net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity,"real gateway and released temporary load tickets");
                var transition=((net.minecraft.world.level.block.EndGatewayBlock)net.minecraft.world.level.block.Blocks.END_GATEWAY).getPortalDestination(original,p,gatewayPosition);
                require(transition!=null&&transition.position().equals(Vec3.atBottomCenterOf(new net.minecraft.core.BlockPos(1,15000000,1))),"native exact high-altitude exit");
                require(p.teleport(transition)==p,"actual native gateway transfer");client.acknowledge();require(p.getY()==15000000,"gateway preserves intentionally extreme height");phase=11;wait=0;
            }else if(phase==11){
                if(p.level()!=nexus)return false;
                require(p.getY()==2332&&p.getX()>=-4800&&p.getX()<=4800&&p.getZ()>=-4800&&p.getZ()<=4800,"source altitude entry uses actual random Nexus destination");
                return true;
            }return false;
        }
        @Override public void close(){if(closed)return;closed=true;try{owned.forEach(Entity::discard);wires.stop();
                if(base.owner.level()!=original)transfer(original);base.owner.getInventory().clearContent();
                for(var entry:savedCharges.entrySet()){String n=entry.getKey();int v=entry.getValue()==null?0:entry.getValue();if(n.endsWith("2"))game.items().terra().sourceCharges(base.owner,v);else game.items().electro().sourceCharge(base.owner,n.endsWith("5"),n.startsWith("swordcharging"),v);}
                base.close();var p=base.owner;p.totalExperience=xp;p.experienceLevel=xpLevel;p.experienceProgress=xpProgress;p.takeXpDelay=xpDelay;
            }finally{if(client!=null)client.close();}}
    }
}
