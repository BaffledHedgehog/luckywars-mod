package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Real input, XP boundaries, native item/loot/XP conversion and ordinary-world Nexus routes. */
public final class NexusOrbChecks {
    private static Fixture running;
    private NexusOrbChecks(){}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException("Nexus Orb: "+message);}
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.init();}catch(Throwable t){try{running.close();}catch(Throwable c){t.addSuppressed(c);}running=null;throw new IllegalStateException(t);}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS nexus-orb (native main/offhand input, exact XP0/progress debit/miss, one joint entity/block impact, WILD allied damage/native killer credit, positive MAG/artifact immunity, all659+9 cached selectors, entire stacks/custom Polymer base/stew ingredients plus XP, no output recatch, native TimeStop, Nexus XP-to-wire chain, ordinary Roadgun/Corrupter and exact cleanup)");}catch(Throwable t){try{running.close();}catch(Throwable c){t.addSuppressed(c);}running=null;SkyWars2.LOGGER.error("[check] FAIL nexus-orb",t);}}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture base;final ServerLevel home,nexus;
        final LbcNexusCombat combat;final LbcNexusUncrafting uncraft;final BlockPos block;final AABB room;
        final Map<BlockPos,MapManager.BlockSnapshot> nexusTerrain=new HashMap<>();
        final Set<UUID> original=new HashSet<>(),originalNexus=new HashSet<>();final Map<String,Integer> scores=new HashMap<>();
        final int xpLevel,xpTotal;final float xpProgress;final boolean offhand;
        final Vec3 nexusPoint=new Vec3(8.5,188.5,8.5);
        RegenerationBeltChecks.ScheduledClient client,audience;Cow cow;Marker mag;ItemEntity iron,custom,stew,barriers;Entity road;
        int stage,wait,age,units,wireBefore;long before;boolean closed;
        Fixture(GameController game){this.game=game;base=new ChefChecks.Fixture(game);home=base.owner.level();nexus=game.server().getLevel(LbcHedgehogDimensions.NEXUS);combat=game.items().nexusCombat();uncraft=combat.uncrafting();block=base.center.offset(0,5,0);room=new AABB(base.center).inflate(18);
            xpLevel=base.owner.experienceLevel;xpTotal=base.owner.totalExperience;xpProgress=base.owner.experienceProgress;offhand=game.config().offhandWands;
            for(String key:List.of("nexus_roadgun_cd","nexus_corrupter_cd","pulsar_cd","nexus_tools_phase"))scores.put(key,game.items().mana().hudValue(base.owner,key));}
        void begin(int value){stage=value;wait=0;}
        int points(){return net.minecraft.util.Mth.floor(base.owner.experienceProgress*base.owner.getXpNeededForNextLevel());}
        void xp(int level,int points){var p=base.owner;p.experienceLevel=level;p.experienceProgress=(points+.25f)/p.getXpNeededForNextLevel();p.totalExperience=level*level+6*level+points;}
        void aim(){var p=base.owner;var at=Vec3.atCenterOf(block);p.teleportTo(home,at.x,at.y-p.getEyeHeight(),at.z-5,Set.of(),0,0,true);client.acknowledge();p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);}
        InteractionResult use(InteractionHand hand){var p=base.owner;return net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(p,p.level(),hand);}
        ItemEntity drop(ServerLevel level,Vec3 at,ItemStack stack){var item=new ItemEntity(level,at.x,at.y,at.z,stack);item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(Integer.MAX_VALUE);require(level.addFreshEntity(item),"real native input stack");return item;}
        void init(){base.init();client=new RegenerationBeltChecks.ScheduledClient(game,base.owner);audience=new RegenerationBeltChecks.ScheduledClient(game,base.other);game.config().offhandWands=true;
            for(String key:scores.keySet())game.items().mana().score(base.owner,key).set(0);
            require(uncraft.tableCount()==659&&uncraft.effectTableCount()==9&&uncraft.pending()==0&&combat.pending()==0,"whole fixed native lookup loaded and clean");
            for(var e:home.getEntities((Entity)null,room))original.add(e.getUUID());
            for(var e:nexus.getEntities((Entity)null,new AABB(nexusPoint,nexusPoint).inflate(4)))originalNexus.add(e.getUUID());
            base.owner.getAbilities().invulnerable=true;base.move(base.other,Vec3.atCenterOf(block).add(7,0,-2));audience.acknowledge();
            base.other.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(base.other);
            mag=new Marker(EntityTypes.MARKER,home);mag.setPos(Vec3.atCenterOf(block).add(-6,0,0));mag.entityTags().add("stopper_magic");home.addFreshEntity(mag);game.items().magic().loaded(mag);
            base.block(block,Blocks.STONE);aim();
            cow=base.cow(Vec3.atCenterOf(block).add(0,0,1));cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(10);cow.setHealth(10);cow.noPhysics=true;game.server().getScoreboard().addPlayerToTeam(cow.getScoreboardName(),base.team);
            var input=drop(home,Vec3.atCenterOf(block).add(0,-.125,-.375),new ItemStack(Items.STONE));
            game.server().getScoreboard().addPlayerToTeam(base.owner.getScoreboardName(),base.team);
            require(game.items().magic().suppressed(base.owner),"actual MAG positive control");
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcNexusToolItems.ORB_GUN));base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);xp(5,4);before=combat.bursts();
            begin(-1);
            SkyWars2.LOGGER.info("[check] START nexus-orb");}
        void clearLoot(){for(var e:home.getEntities((Entity)null,room,e->(e instanceof ItemEntity||e instanceof ExperienceOrb)&&!original.contains(e.getUUID())))e.discard();}
        int amount(Item item){int count=0;for(var e:home.getEntitiesOfClass(ItemEntity.class,room,e->!original.contains(e.getUUID())))if(e.getItem().is(item))count+=e.getItem().getCount();return count;}
        int wires(){int count=0;for(var e:nexus.getEntitiesOfClass(ItemEntity.class,new AABB(nexusPoint,nexusPoint).inflate(3),e->!originalNexus.contains(e.getUUID())))if(LbcNexusWireItems.isRed(e.getItem()))count+=e.getItem().getCount();return count;}
        boolean step(){require(++age<600,"bounded phase="+stage+", wait="+wait);wait++;client.acknowledge();audience.acknowledge();var p=base.owner;
            if(stage==-1){if(!game.items().magic().artifactActive(base.other))return false;
                require(game.items().magic().suppressed(p)&&use(InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS&&p.experienceLevel==4&&points()==14,"native4progress shot debits5 points across level boundary despite positive artifact/MAG");begin(0);
            }else if(stage==0){if(combat.pending()!=0)return false;require(combat.bursts()==before+1&&cow.getHealth()==8,"one true joint-collision area and exact2HP WILD same-team damage: bursts="+combat.bursts()+", before="+before+", HP="+cow.getHealth());xp(15,20);before=combat.bursts();require(use(InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS,"second source shot");begin(1);
            }else if(stage==1){if(combat.pending()!=0)return false;require(!cow.isAlive()&&cow.getKillCredit()==p&&combat.bursts()==before+1,"native lethal10HP WILD hit credits captured owner");
                clearLoot();p.teleportTo(home,p.getX(),home.getMaxY()-8,p.getZ(),Set.of(),0,-90,true);client.acknowledge();p.setDeltaMovement(Vec3.ZERO);
                p.experienceLevel=5;p.experienceProgress=0;p.totalExperience=55;before=combat.bursts();require(use(InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS&&combat.pending()==0&&p.experienceLevel==4&&points()==14,"source zero-progress failure still spends exactly1 native XP point");
                xp(5,4);require(use(InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS,"real upward finite miss");begin(2);
            }else if(stage==2){if(combat.pending()!=0)return false;require(combat.bursts()==before&&p.experienceLevel==4&&points()==14,"miss preserves debit and creates no area");aim();p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(LbcNexusToolItems.ORB_GUN));
                var at=Vec3.atCenterOf(block);iron=drop(home,at.add(.3,0,0),new ItemStack(Items.IRON_BLOCK,3));custom=drop(home,at.add(-.3,0,0),new ItemStack(LbcNexusToolItems.ORB_GUN));var meal=new ItemStack(Items.SUSPICIOUS_STEW);meal.set(DataComponents.SUSPICIOUS_STEW_EFFECTS,new SuspiciousStewEffects(List.of(new SuspiciousStewEffects.Entry(MobEffects.POISON,160))));stew=drop(home,at.add(0,0,.3),meal);
                require(use(InteractionHand.OFF_HAND)==InteractionResult.SUCCESS,"actual offhand uncraft input");begin(3);
            }else if(stage==3){if(uncraft.pending()==0)return false;require(uncraft.pending()==3&&uncraft.pendingUnits()==5,"initial complete cohort includes three stacks and native Polymer item");units=uncraft.pendingUnits();game.items().timeStop().begin(home,20);begin(4);
            }else if(stage==4&&wait>=3){require(uncraft.pendingUnits()==units&&iron.getItem().getCount()==3&&!custom.isRemoved()&&!stew.isRemoved(),"native world TimeStop pauses entire pending conversion");game.items().timeStop().end(home);begin(5);
            }else if(stage==5){if(uncraft.pending()!=0)return false;require(iron.isRemoved()&&custom.isRemoved()&&stew.isRemoved(),"every original source unit consumed once");
                require(amount(Items.IRON_INGOT)==27&&amount(Items.CARROT)==1&&amount(Items.FISHING_ROD)==1,"native source counts and shared Polymer vanilla-base mapping: iron="+amount(Items.IRON_INGOT)+", carrot="+amount(Items.CARROT)+", rod="+amount(Items.FISHING_ROD));require(amount(Items.BOWL)==1&&amount(Items.RED_MUSHROOM)==1&&amount(Items.BROWN_MUSHROOM)==1&&amount(Items.LILY_OF_THE_VALLEY)==1,"stew retains source effect ingredients");
                require(!home.getEntitiesOfClass(ExperienceOrb.class,room,e->!original.contains(e.getUUID())).isEmpty(),"stew also retains native fallback XP");clearLoot();begin(9);
            }else if(stage==9){var chunk=new net.minecraft.world.level.ChunkPos(0,0);if(nexus.getChunkSource().getChunkNow(0,0)==null||!nexus.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(0,0))){game.maps().prepareChunk(nexus,chunk);return false;}
                var origin=new BlockPos(12,188,8);for(var pos:List.of(origin,origin.east())){nexusTerrain.put(pos,game.maps().snapshot(nexus,pos));require(nexus.getBlockState(pos).isAir(),"isolated foreign-world component");LbcDropBlocks.setBlock(nexus,pos,Blocks.STONE.defaultBlockState(),true);}
                combat.pulsar().start(nexus,origin,game.items().indirectKills().wildCause(p),LbcNexusCombat.PULSAR_DELAYS);
                wireBefore=wires();barriers=drop(nexus,nexusPoint.add(0,-.125,0),new ItemStack(Items.BARRIER,2));p.teleportTo(nexus,nexusPoint.x,nexusPoint.y-p.getEyeHeight(),nexusPoint.z-4,Set.of(),0,0,true);client.acknowledge();p.setDeltaMovement(Vec3.ZERO);p.setShiftKeyDown(true);game.items().timeStop().begin(home,40);
                require(use(InteractionHand.OFF_HAND)==InteractionResult.SUCCESS,"real Nexus input remains live while map world is stopped");begin(6);
            }else if(stage==6){if(combat.pending()!=0||uncraft.pending()!=0)return false;require(barriers.isRemoved()&&wires()==wireBefore+2,"two real XP entities convert into exactly two floating red wires before merge");
                require(game.items().timeStop().stopped(home)&&combat.pulsar().age()>=0&&nexus.getBlockState(new BlockPos(12,188,8)).is(Blocks.CONCRETE.pick(DyeColor.WHITE)),"Nexus ray, conversion and detached terrain advance independently of stopped map world");
                combat.pulsar().stop();game.items().timeStop().end(home);for(var e:nexusTerrain.entrySet())game.maps().restoreSnapshot(nexus,e.getKey(),e.getValue());nexusTerrain.clear();
                base.other.getInventory().setItem(9,ItemStack.EMPTY);game.items().magic().inventoryChanged(base.other);game.items().magic().flushCleansing();p.setShiftKeyDown(false);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcNexusToolItems.ROADGUN));aim();
                begin(10);
            }else if(stage==10){if(game.items().magic().artifactActive(base.other))return false;
                require(use(InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS&&p.getVehicle()!=null,"ordinary-world Roadgun route restored");road=p.getVehicle();p.setShiftKeyDown(true);begin(7);
            }else if(stage==7){if(!road.isRemoved())return false;require(p.getVehicle()==null,"early Roadgun exit finishes without remote painting");p.setShiftKeyDown(false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcNexusToolItems.CORRUPTER));aim();require(use(InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS,"ordinary-world Corrupter route restored");begin(8);
            }else if(stage==8){if(!home.getBlockState(block).is(Blocks.CONCRETE.pick(DyeColor.BLACK)))return false;require(game.items().mana().hudValue(p,"nexus_corrupter_cd")>0,"ordinary-world field and source cooldown advance");game.items().nexusTools().clearCells(home);return true;
            }return false;
        }
        @Override public void close(){if(closed)return;closed=true;try{game.items().timeStop().end(home);combat.stop();game.items().nexusTools().stop();base.owner.stopRiding();if(mag!=null){game.items().magic().unloaded(mag);mag.discard();}if(cow!=null)cow.discard();clearLoot();
                if(nexus!=null)for(var e:nexus.getEntities((Entity)null,new AABB(nexusPoint,nexusPoint).inflate(4),e->(e instanceof ItemEntity||e instanceof ExperienceOrb)&&!originalNexus.contains(e.getUUID())))e.discard();
                for(var e:nexusTerrain.entrySet())game.maps().restoreSnapshot(nexus,e.getKey(),e.getValue());
                if(base.owner.level()!=home){base.owner.teleportTo(home,base.at.x,base.at.y,base.at.z,Set.of(),0,0,true);if(client!=null)client.acknowledge();}
                base.close();base.owner.experienceLevel=xpLevel;base.owner.experienceProgress=xpProgress;base.owner.totalExperience=xpTotal;game.config().offhandWands=offhand;for(var e:scores.entrySet())game.items().mana().score(base.owner,e.getKey()).set(e.getValue());
            }finally{if(client!=null)client.close();if(audience!=null)audience.close();}}
    }
}
