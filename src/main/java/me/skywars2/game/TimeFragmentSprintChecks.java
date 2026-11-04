package me.skywars2.game;

import me.skywars2.SkyWars2;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;

/** OFF-only operator fixture. No fragment item, production input or custom world-tick loop exists. */
public final class TimeFragmentSprintChecks {
    private static final int TICKS=20;
    private static Fixture running;
    private static GameController requested;
    private static long hookCalls;
    private TimeFragmentSprintChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Time fragment native experiment: "+why);}
    /** Command admission only queues work: the first simulation request belongs to the real END tick. */
    public static void run(GameController game){
        require(requested==null&&running==null,"one exclusive experimental request");stable(game);requested=game;
    }
    public static void tick(GameController game){
        hookCalls++;
        if(requested!=null){var next=requested;requested=null;try{require(next==game,"same server owner");running=new Fixture(game);running.prepare();}
            catch(Throwable failure){if(running!=null){running.failure=failure;running.cancelSprint();}else SkyWars2.LOGGER.error("[check] FAIL time-fragment-native (disabled experiment)",failure);}}
        if(running==null)return;
        var active=running;
        try{if(active.failure==null)active.step();else active.cancelSprint();
            if(!active.finished)return;
        }catch(Throwable failure){active.failure=failure;active.cancelSprint();if(!active.finished)return;}
        try{active.close();if(active.failure==null)SkyWars2.LOGGER.info(
                "[check] PASS time-fragment-native (OFF-only genuine native sprint; actualTicks={} nativeHooks={} matchTicks={} worldTicks={} elapsedMs={} packets={} connectionTicks={}; actual item/projectile gravity, scheduled block/fluid and effect progression; isolated small scene only, no instant/peak-TPS/client qualification)",
                TICKS,active.measuredHooks,active.measuredMatch,active.measuredWorld,active.wallNanos/1_000_000d,active.packetCount,active.connectionTicks);
            else SkyWars2.LOGGER.error("[check] FAIL time-fragment-native (disabled experiment)",active.failure);
        }catch(Throwable cleanup){if(active.failure!=null)cleanup.addSuppressed(active.failure);SkyWars2.LOGGER.error("[check] FAIL time-fragment-native cleanup",cleanup);}
        finally{running=null;}
    }
    private static void stable(GameController game){var manager=game.server().tickRateManager();
        require(game.server().isSameThread()&&game.playing()&&game.stage()!=GameController.Stage.ENDING,"main-thread actual stable match");
        require(!game.maps().busy()&&!game.supernovaMapTransition()&&!game.cunnyRestartPending()&&!game.vanity().busy()&&!game.vanity().hasSnapshot()&&!game.vanity().capturePreparing(),"no map/restart/rewind work");
        require(!manager.isFrozen()&&!manager.isSprinting()&&!manager.isSteppingForward()&&manager.runsNormally()&&manager.tickrate()==20,"exclusive unfrozen ordinary20TPS scheduler");
        require(!game.items().wands().timeStopped(),"no legacy/shared TimeStop");
        for(var level:game.server().getAllLevels())require(!game.items().timeStop().stopped(level),"no TimeStop in any world");
    }
    private static int packets(RegenerationBeltChecks.ScheduledClient client){
        // Reuse the existing native scheduler fixture and its scoped recording connection.
        try{Object connection=client.connection;var field=connection.getClass().getDeclaredField("packets");field.setAccessible(true);return ((List<?>)field.get(connection)).size();}
        catch(ReflectiveOperationException error){throw new IllegalStateException("existing native outbound recording",error);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerLevel level;final String epoch;final GameController.Stage stage;
        final List<ServerPlayer> players;final Map<ServerPlayer,RegenerationBeltChecks.Snapshot> saved=new LinkedHashMap<>();
        final Map<ServerPlayer,RegenerationBeltChecks.ScheduledClient> clients=new LinkedHashMap<>();
        final Map<ServerPlayer,Integer> updates=new LinkedHashMap<>(),packetBaselines=new LinkedHashMap<>();
        final SauvojenMeditationChecks.Attribution attribution;
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final List<ScheduledTick<Block>> blockTicks=new ArrayList<>();final List<ScheduledTick<Fluid>> fluidTicks=new ArrayList<>();
        final List<Entity> actors=new ArrayList<>();final Set<UUID> existing=new HashSet<>();
        final BlockPos center,lamp,waterSource,waterDestination;final BoundingBox bounds;final long chunk;
        final boolean oldSkills,oldRanked,oldFrozen;final float oldRate;final int oldStep;
        Cow effectWitness;ItemEntity falling;Arrow arrow;Vec3 fallingStart,arrowStart;
        int phase,preparedAt,startTick,matchStart;long worldStart,hooksStart,startNanos,wallNanos,measuredHooks,measuredWorld;
        int measuredMatch,packetCount,connectionTicks;boolean prepared,closed,finished,forced;Throwable failure;
        Fixture(GameController game){stable(game);this.game=game;level=game.maps().level();epoch=game.items().epoch();stage=game.stage();
            require(stage==GameController.Stage.PLAYING&&game.matchTicks()+80<Math.max(1,game.config().stageOneSeconds)*20,"before any automatic phase/event boundary");
            players=List.copyOf(game.server().getPlayerList().getPlayers());require(players.size()>=2&&players.size()<=4,"isolated two-to-four participant scene");
            for(var player:players)require(player.level()==level&&game.canUseGameplayItems(player)&&!game.isObserver(player)&&player.isAlive()&&player.containerMenu==player.inventoryMenu&&!player.isPassenger(),"ordinary live participants, closed menus, no vehicle");
            var anchor=players.getFirst().blockPosition();int y=Math.min(level.getMaxY()-20,Math.max(220,anchor.getY()+24));
            center=new BlockPos((anchor.getX()>>4)*16+8,y,(anchor.getZ()>>4)*16+8);chunk=net.minecraft.world.level.ChunkPos.pack(center);
            bounds=new BoundingBox(center.getX()-6,y-2,center.getZ()-6,center.getX()+6,y+13,center.getZ()+6);
            lamp=center.offset(4,1,-3);waterSource=center.offset(4,3,3);waterDestination=waterSource.below();
            oldSkills=game.config().lbcSkills;oldRanked=game.config().ranked;var manager=game.server().tickRateManager();oldFrozen=manager.isFrozen();oldRate=manager.tickrate();oldStep=manager.frozenTicksToRun();
            for(var player:players)saved.put(player,new RegenerationBeltChecks.Snapshot(game,player));attribution=new SauvojenMeditationChecks.Attribution(game,players);
            level.getAllEntities().forEach(entity->existing.add(entity.getUUID()));
        }
        void prepare(){
            prepared=true;preparedAt=game.server().getTickCount();
            require(level.hasChunkAt(center)&&level.hasChunkAt(new BlockPos(bounds.minX(),bounds.minY(),bounds.minZ()))&&level.hasChunkAt(new BlockPos(bounds.maxX(),bounds.maxY(),bounds.maxZ())),"small fixture chunk already resident");
            for(var pos:BlockPos.betweenClosed(new BlockPos(bounds.minX(),bounds.minY(),bounds.minZ()),new BlockPos(bounds.maxX(),bounds.maxY(),bounds.maxZ())))terrain.put(pos.immutable(),game.maps().snapshot(level,pos));
            var resident=level.getChunk(center);((LevelChunkTicks<Block>)resident.getBlockTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(blockTicks::add);
            ((LevelChunkTicks<Fluid>)resident.getFluidTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(fluidTicks::add);
            level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);
            forced=level.setChunkForced(center.getX()>>4,center.getZ()>>4,true);
            for(var pos:terrain.keySet())set(pos,pos.getY()==center.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
            // Fully enclosed3x4x3 chamber has exactly one native downflow cell.
            var base=waterSource.below(2);for(var pos:BlockPos.betweenClosed(base.offset(-1,0,-1),base.offset(1,3,1))){
                boolean inside=pos.getX()==base.getX()&&pos.getZ()==base.getZ()&&(pos.getY()==base.getY()+1||pos.getY()==base.getY()+2);
                set(pos,inside?Blocks.AIR.defaultBlockState():Blocks.STONE.defaultBlockState());}
            game.config().lbcSkills=false;game.config().ranked=false;
            int n=0;for(var player:players){clients.put(player,new RegenerationBeltChecks.ScheduledClient(game,player));
                player.stopUsingItem();player.getInventory().clearContent();player.inventoryMenu.getCraftSlots().clearContent();player.containerMenu.setCarried(ItemStack.EMPTY);
                player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);player.setNoGravity(false);player.setDeltaMovement(Vec3.ZERO);player.setShiftKeyDown(false);
                var profile=game.players().get(player.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);
                require(player.teleportTo(level,center.getX()-4.5+(n%2)*3,center.getY(),center.getZ()-4.5+(n/2)*3,Set.of(),0,0,true),"native grounded participant teleport");
                player.setDeltaMovement(Vec3.ZERO);clients.get(player).acknowledge();game.items().magic().inventoryChanged(player);n++;}
            game.items().magic().flushCleansing();
        }
        void set(BlockPos pos,BlockState state){require(terrain.containsKey(pos),"only saved fixture cells mutate");level.setBlock(pos,state,2);}
        void scene(){
            for(var player:players){var client=clients.get(player);client.acknowledge();
                var support=BlockPos.containing(player.getX(),player.getY()-.01,player.getZ());
                require(level.getBlockState(support).is(Blocks.STONE)&&!level.noCollision(player,player.getBoundingBox().move(0,-.05,0)),"actual solid platform supports stationary participant");
                // A headless scheduled client must report its real support through the ordinary packet handler.
                // ACK/native doTick may clear onGround; this is client input, never a manual entity tick.
                client.listener.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly(true,false));
                require(!player.isNoGravity()&&player.onGround(),"native grounded status packet accepted on actual support");}
            effectWitness=new Cow(EntityTypes.COW,level);effectWitness.setPos(Vec3.atBottomCenterOf(center.offset(-4,0,3)));effectWitness.setNoAi(true);effectWitness.setNoGravity(true);add(effectWitness);
            effectWitness.addEffect(ItemEffectLevels.create(MobEffects.GLOWING,200,0),players.getFirst());
            falling=new ItemEntity(level,center.getX()+.5,center.getY()+9,center.getZ()+.5,new ItemStack(Items.STONE));falling.setPickUpDelay(200);falling.setNoGravity(false);falling.setDeltaMovement(.04,.05,0);add(falling);fallingStart=falling.position();
            arrow=new Arrow(EntityTypes.ARROW,level);arrow.setOwner(players.getFirst());arrow.setPos(Vec3.atBottomCenterOf(center.offset(-2,10,4)));arrow.setNoGravity(false);arrow.setDeltaMovement(.08,.2,0);add(arrow);arrowStart=arrow.position();
            set(lamp,Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT,true));
            set(waterSource,Blocks.WATER.defaultBlockState());level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);
            level.scheduleTick(lamp,Blocks.REDSTONE_LAMP,3);level.scheduleTick(waterSource,Fluids.WATER,2);
            require(level.getBlockTicks().hasScheduledTick(lamp,Blocks.REDSTONE_LAMP)&&level.getFluidTicks().hasScheduledTick(waterSource,Fluids.WATER),"real block/fluid queues populated");
            require(level.getBlockState(lamp).getValue(RedstoneLampBlock.LIT)&&level.getFluidState(waterDestination).isEmpty(),"unadvanced scheduled block/fluid witnesses");
        }
        void add(Entity entity){require(level.addFreshEntity(entity),"actual native fixture entity inserted");actors.add(entity);}
        void step(){
            require(game.server().isSameThread()&&game.items().currentEpoch(epoch)&&game.stage()==stage&&!game.maps().busy()&&!game.supernovaMapTransition()&&!game.cunnyRestartPending()&&!game.vanity().busy(),"stable unchanged match throughout experiment");
            for(var world:game.server().getAllLevels())require(!game.items().timeStop().stopped(world),"TimeStop interrupts experimental sprint");
            for(var client:clients.values())client.acknowledge();
            if(phase==0){stable(game);require(game.server().getTickCount()-preparedAt<60,"bounded native readiness");
                if(!level.isPositionTickingWithEntitiesLoaded(chunk)||clients.values().stream().anyMatch(client->!client.ready()))return;
                scene();startTick=game.server().getTickCount();matchStart=game.matchTicks();worldStart=level.getGameTime();hooksStart=hookCalls;
                for(var entry:clients.entrySet()){updates.put(entry.getKey(),entry.getValue().updates());packetBaselines.put(entry.getKey(),packets(entry.getValue()));}
                startNanos=System.nanoTime();boolean interrupted=game.server().tickRateManager().requestGameToSprint(TICKS);
                require(!interrupted&&game.server().tickRateManager().isSprinting(),"exclusive genuine native sprint request accepted");phase=1;return;}
            if(phase==1){int elapsed=game.server().getTickCount()-startTick;require(elapsed>0&&elapsed<=TICKS,"exact bounded actual server iterations");
                require(game.server().tickRateManager().isSprinting(),"native sprint remains owned through final iteration");
                for(var entry:clients.entrySet())require(entry.getValue().updates()-updates.get(entry.getKey())==elapsed,"real connection/player scheduler follows every native iteration");
                if(elapsed<TICKS)return;
                wallNanos=System.nanoTime()-startNanos;measuredHooks=hookCalls-hooksStart;measuredMatch=game.matchTicks()-matchStart;measuredWorld=level.getGameTime()-worldStart;
                require(measuredHooks==TICKS&&measuredMatch==TICKS&&measuredWorld==TICKS,"20 real Fabric/game/world ticks, not a timer shortcut");
                require(!falling.isRemoved()&&!falling.isNoGravity()&&falling.tickCount==TICKS&&falling.getY()<fallingStart.y-1&&falling.getX()>fallingStart.x,"real item motion and gravity advanced20 updates");
                require(!arrow.isRemoved()&&!arrow.isNoGravity()&&arrow.tickCount==TICKS&&arrow.getY()<arrowStart.y-1&&arrow.getX()>arrowStart.x&&arrow.getDeltaMovement().y<0,"real native projectile motion/gravity advanced20 updates");
                var effect=effectWitness.getEffect(MobEffects.GLOWING);require(effect!=null&&effect.getDuration()==200-TICKS&&effectWitness.tickCount==TICKS,"native MobEffect decremented on actual entity ticks");
                require(level.getBlockState(lamp).is(Blocks.REDSTONE_LAMP)&&!level.getBlockState(lamp).getValue(RedstoneLampBlock.LIT)&&!level.getBlockTicks().hasScheduledTick(lamp,Blocks.REDSTONE_LAMP),"native scheduled BLOCK tick executed and changed state");
                require(!level.getFluidState(waterDestination).isEmpty()&&level.getFluidState(waterDestination).getType().isSame(Fluids.WATER),"native scheduled FLUID tick produced actual enclosed downflow");
                for(var entry:clients.entrySet()){packetCount+=packets(entry.getValue())-packetBaselines.get(entry.getKey());connectionTicks+=entry.getValue().updates()-updates.get(entry.getKey());require(!entry.getKey().isRemoved(),"participant survives native connection scheduler");}
                require(packetCount>0&&connectionTicks==players.size()*TICKS,"actual outbound packet volume and20 connection ticks per participant recorded");
                phase=2;return; // Vanilla remaining0 still reports isSprinting until the next main-loop boundary.
            }
            if(phase==2){require(game.server().getTickCount()==startTick+TICKS+1,"one ordinary completion-boundary tick only");
                require(!game.server().tickRateManager().isSprinting(),"native final remaining0 sprint state naturally finished");finished=true;}
        }
        void cancelSprint(){var manager=game.server().tickRateManager();if(manager.isSprinting())manager.stopSprinting();
            // stopSprinting() deliberately returns false at remaining0. Let the real next loop finish it.
            finished=!manager.isSprinting();}
        public void close(){if(closed)return;closed=true;Throwable cleanup=null;
            for(Runnable action:List.<Runnable>of(
                ()->{var manager=game.server().tickRateManager();if(manager.isSprinting())manager.stopSprinting();require(!manager.isSprinting(),"exclusive sprint manager settled before fixture release");},
                ()->{for(var entity:actors){if(entity instanceof net.minecraft.world.entity.LivingEntity living)game.items().indirectKills().clear(living);game.server().getScoreboard().resetAllPlayerScores(entity);entity.discard();}},
                ()->{if(prepared){level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);for(var entry:terrain.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue(),2);level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);blockTicks.forEach(level.getBlockTicks()::schedule);fluidTicks.forEach(level.getFluidTicks()::schedule);if(forced)level.setChunkForced(center.getX()>>4,center.getZ()>>4,false);}},
                ()->{game.config().lbcSkills=oldSkills;game.config().ranked=oldRanked;for(var snapshot:saved.values()){snapshot.restoreAttributes();snapshot.restore(game);snapshot.restoreUse();}for(var player:players)game.items().magic().inventoryChanged(player);game.items().magic().flushCleansing();game.players().save();},
                attribution::close,
                ()->{for(var client:clients.values())client.close();},
                ()->{var manager=game.server().tickRateManager();if(manager.tickrate()!=oldRate)manager.setTickRate(oldRate);manager.setFrozenTicksToRun(oldStep);if(manager.isFrozen()!=oldFrozen)manager.setFrozen(oldFrozen);}
            ))try{action.run();}catch(Throwable error){if(cleanup==null)cleanup=error;else cleanup.addSuppressed(error);}
            if(cleanup!=null)throw new IllegalStateException("native sprint fixture restoration",cleanup);
        }
    }
}
