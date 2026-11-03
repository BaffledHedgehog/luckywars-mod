package me.skywars2.game;

import java.util.*;
import java.nio.file.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Native public input, source compass cache and real moving/branching Nexus actors. */
public final class NexusToolsChecks {
    private static Fixture running;
    private NexusToolsChecks(){}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException("Nexus tools: "+message);}
    static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.init();}catch(Throwable t){running.close();running=null;if(t instanceof Error e)throw e;throw new IllegalStateException(t);}}
    static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS nexus-tools (native air/block callbacks, Paper compass cache/reload/same-Nexus/mainhand/MAG/TimeStop, exact Corrupter ray and owned WILD concrete, cross-owner cohort clear, typed paused actors, complete525block Roadgun/39ticks/pad/sparse distribution and cleanup)");}catch(Throwable t){try{running.close();}catch(Throwable c){t.addSuppressed(c);}running=null;SkyWars2.LOGGER.error("[check] FAIL nexus-tools",t);}}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture base;final ServerLevel home,nexus;final LbcNexusTools tools;
        final Path override=Path.of("config","luckywars","nexus_coords.json");final byte[] savedOverride;
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();final Set<Entity> owned=new HashSet<>();final Set<UUID> originalNearby=new HashSet<>();
        RegenerationBeltChecks.ScheduledClient client;Marker jammer;Cow cow;Entity road;Vec3 roadStart;int phase,wait,age,savedRoadAge;
        boolean closed;
        Fixture(GameController game){this.game=game;base=new ChefChecks.Fixture(game);home=base.owner.level();nexus=game.server().getLevel(LbcHedgehogDimensions.NEXUS);tools=game.items().nexusTools();require(nexus!=null&&tools.count()==0,"clean real Nexus actors");
            try{savedOverride=Files.exists(override)?Files.readAllBytes(override):null;}catch(java.io.IOException e){throw new IllegalStateException(e);}}
        void config(double x){try{Files.createDirectories(override.getParent());Files.writeString(override,"[{\"x\":"+x+",\"y\":160,\"z\":40.5}]");}catch(java.io.IOException e){throw new IllegalStateException(e);}}
        void init()throws Exception{base.init();client=new RegenerationBeltChecks.ScheduledClient(game,base.owner);var p=base.owner;p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);p.setHealth(10000);p.getAbilities().invulnerable=true;for(String key:List.of("nexus_roadgun_cd","nexus_corrupter_cd","nexus_tools_phase"))game.items().mana().score(p,key).set(0);config(40.5);game.items().nexusCompass().reload();
            var random=net.minecraft.util.RandomSource.create(77);int hits=0;for(int n=0;n<20000;n++)for(int i=LbcNexusTools.nextSparse(random);i<343;i+=1+LbcNexusTools.nextSparse(random))hits++;
            require(Math.abs(hits/20000d-.686)<.03,"source .002 independent sparse occupancy");
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(LbcNexusToolItems.COMPASS));
            require(use(InteractionHand.OFF_HAND)==InteractionResult.FAIL&&p.level()==home,"Paper mainhand restriction through actual public callback");
            p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcNexusToolItems.COMPASS));
            jammer=new Marker(EntityTypes.MARKER,home);jammer.setPos(p.position());jammer.entityTags().add("stopper_magic");home.addFreshEntity(jammer);game.items().magic().loaded(jammer);
            require(game.items().magic().suppressed(p)&&use(InteractionHand.MAIN_HAND)==InteractionResult.FAIL&&p.level()==home,"actual MAG protects source compass use");unjam();
            var motion=new Vec3(.3,.2,-.4);p.setDeltaMovement(motion);p.setYRot(31);p.setXRot(-12);
            require(use(InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS&&p.level()==nexus&&p.position().equals(new Vec3(40.5,160,40.5)),"actual cached saved destination");client.acknowledge();
            require(p.getDeltaMovement().equals(motion)&&p.getYRot()==31&&p.getXRot()==-12&&game.items().nexusCompass().cooldownSeconds(p)==30,"teleport retains motion/look and native30second cooldown");
            config(64.5);game.items().mana().score(p,"nexus_compass_cd").set(0);
            require(net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(p,p.level(),InteractionHand.MAIN_HAND,new BlockHitResult(p.position(),net.minecraft.core.Direction.UP,p.blockPosition(),false))==InteractionResult.SUCCESS&&p.getX()==40.5,"block callback uses cache, same-Nexus teleport remains available");client.acknowledge();
            require(game.server().getCommands().getDispatcher().execute("skywars reloadconfig",game.server().createCommandSourceStack())>0,"real reload command updates compass table");
            game.items().mana().score(p,"nexus_compass_cd").set(0);
            require(use(InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS&&p.getX()==64.5,"reloaded exact destination");client.acknowledge();
            game.items().mana().score(p,"nexus_compass_cd").set(3);game.items().mana().score(p,"nexus_compass_phase").set(0);game.items().nexusCompass().inventoryChanged(p);p.entityTags().add("time_stopped");p.setDeltaMovement(Vec3.ZERO);
            SkyWars2.LOGGER.info("[check] START nexus-tools");}
        void unjam(){if(jammer!=null){game.items().magic().unloaded(jammer);jammer.discard();jammer=null;}}
        InteractionResult use(InteractionHand hand){var p=base.owner;return net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(p,p.level(),hand);}
        boolean ready(){boolean ready=true;for(int x=2;x<=38;x++){if(nexus.getChunkSource().getChunkNow(x,2)==null||!nexus.areEntitiesLoaded(ChunkPos.pack(x,2))){game.maps().prepareChunk(nexus,new ChunkPos(x,2));ready=false;}}return ready;}
        void saveTerrain(){for(var pos:BlockPos.betweenClosed(new BlockPos(34,154,34),new BlockPos(610,167,46)))terrain.put(pos.immutable(),game.maps().snapshot(nexus,pos));}
        boolean step()throws Exception{require(++age<600,"bounded native phase="+phase);wait++;client.acknowledge();var p=base.owner;
            if(phase==0&&wait>=4){require(game.items().nexusCompass().cooldownSeconds(p)==3&&game.items().mana().hudValue(p,"nexus_compass_phase")==0,"personal TimeStop freezes compass clock");p.entityTags().remove("time_stopped");phase=1;wait=0;
            }else if(phase==1&&wait>=21){require(game.items().nexusCompass().cooldownSeconds(p)==2,"twenty active ticks consume one second");phase=2;wait=0;
            }else if(phase==2){if(!ready())return false;saveTerrain();p.teleportTo(nexus,40.5,160,40.5,Set.of(),-90,0,true);client.acknowledge();p.setDeltaMovement(Vec3.ZERO);p.setNoGravity(true);
                var target=new BlockPos(46,161,40);LbcDropBlocks.setBlock(nexus,target,Blocks.STONE.defaultBlockState(),true);
                for(var entity:nexus.getEntities((Entity)null,new AABB(target).inflate(3)))originalNearby.add(entity.getUUID());cow=EntityTypes.COW.create(nexus,EntitySpawnReason.COMMAND);require(cow!=null,"native owned target");cow.setPos(Vec3.atCenterOf(target));cow.setNoGravity(true);cow.setNoAi(true);cow.noPhysics=true;owned.add(cow);nexus.addFreshEntity(cow);
                game.server().getScoreboard().addPlayerToTeam(cow.getScoreboardName(),base.team);game.server().getScoreboard().addPlayerToTeam(p.getScoreboardName(),base.team);
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcNexusToolItems.CORRUPTER));var click=use(InteractionHand.MAIN_HAND);require(click==InteractionResult.SUCCESS,"actual Nexus Corrupter request accepted: result="+click+", playing="+game.playing()+", admitted="+game.canUseGameplayItems(p)+", observer="+game.isObserver(p)+", alive="+p.isAlive()+", removed="+p.isRemoved()+", blocked="+game.items().actionsBlocked(p)+", frozen="+game.items().wands().inputTimeStopped(p)+", worldFrozen="+game.items().timeStop().stopped(nexus)+", cd="+game.items().mana().hudValue(p,"nexus_corrupter_cd")+", item="+p.getMainHandItem()+", compassMatch="+LbcNexusToolItems.isCompass(p.getMainHandItem()));phase=3;wait=0;
            }else if(phase==3){
                if(cow.isAlive())return false;for(var entity:nexus.getEntities((Entity)null,new AABB(new BlockPos(46,161,40)).inflate(3),e->e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof ExperienceOrb))if(!originalNearby.contains(entity.getUUID()))owned.add(entity);
                require(game.items().mana().hudValue(p,"nexus_corrupter_cd")==20&&cow.getKillCredit()==p,"native WILD death credits source and commits20second cooldown: cd="+game.items().mana().hudValue(p,"nexus_corrupter_cd")+", credit="+cow.getKillCredit());
                require(nexus.getBlockState(new BlockPos(46,161,40)).is(Blocks.CONCRETE.pick(DyeColor.BLACK)),"source endpoint becomes black concrete");tools.clearCells(nexus);
                var one=tools.cube(nexus,new Vec3(42.5,160.5,40.5),game.items().indirectKills().wildCause(p));var other=tools.cube(nexus,new Vec3(44.5,160.5,40.5),game.items().indirectKills().wildCause(base.other));
                require(one!=null&&other!=null,"two real owners in one source cohort");one.body.entityTags().add("time_stopped");other.body.entityTags().add("time_stopped");
                var image=tools.checkpoint();tools.prepareFrozen();var plan=image.prepare(nexus::getEntity);require(plan.pending().isEmpty(),"typed paused fields bind real native markers");plan.install().run();tools.publishFrozen();require(tools.count()==2,"both owners restored");tools.clearCells(nexus);require(tools.count()==0&&one.body.isRemoved()&&other.body.isRemoved(),"shared source cancellation crosses owners");
                p.teleportTo(nexus,40.5,160,40.5,Set.of(),-90,0,true);client.acknowledge();p.setDeltaMovement(Vec3.ZERO);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcNexusToolItems.ROADGUN));
                require(use(InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS&&p.getVehicle()!=null,"native Roadgun mounts one real display");road=p.getVehicle();owned.add(road);roadStart=road.position();phase=4;wait=0;
            }else if(phase==4){
                var a=tools.snapshot().stream().filter(x->x.body==road).findFirst().orElse(null);require(a!=null,"functional rider owner");if(a.age<3)return false;
                road.entityTags().add("time_stopped");savedRoadAge=a.age;var cp=tools.checkpoint();tools.prepareFrozen();var plan=cp.prepare(nexus::getEntity);require(plan.pending().isEmpty(),"typed moving source binds native display");plan.install().run();tools.publishFrozen();require(p.getVehicle()==road&&tools.snapshot().getFirst().age==savedRoadAge,"native rider/age survive private state publication");phase=5;wait=0;
            }else if(phase==5&&wait>=3){
                require(tools.snapshot().getFirst().age==savedRoadAge,"paused Roadgun does not advance");road.entityTags().remove("time_stopped");phase=6;wait=0;
            }else if(phase==6){if(!road.isRemoved())return false;
                require(p.getVehicle()==null&&Math.abs(road.getX()-roadStart.x-525)<.001,"complete35x15block source travel and natural dismount");
                require(nexus.getBlockState(p.blockPosition().below(2)).is(Blocks.CONCRETE.pick(DyeColor.WHITE)),"source finish pad lies below the actual player");
                require(game.items().mana().hudValue(p,"nexus_roadgun_cd")>0,"source cooldown retained");return true;
            }return false;}
        @Override public void close(){if(closed)return;closed=true;try{unjam();tools.stop();owned.forEach(Entity::discard);base.owner.entityTags().remove("time_stopped");base.owner.stopRiding();
                for(var e:terrain.entrySet())game.maps().restoreSnapshot(nexus,e.getKey(),e.getValue());
                if(base.owner.level()!=home){base.owner.teleportTo(home,base.at.x,base.at.y,base.at.z,Set.of(),0,0,true);if(client!=null)client.acknowledge();}
                base.close();
            }finally{try{if(savedOverride==null)Files.deleteIfExists(override);else Files.write(override,savedOverride);game.items().nexusCompass().reload();}catch(java.io.IOException e){throw new IllegalStateException(e);}finally{if(client!=null)client.close();}}}
    }
}
