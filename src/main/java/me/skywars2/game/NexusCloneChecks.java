package me.skywars2.game;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Native Stonekivi scheduling, with both full chunk columns journaled before admission resumes. */
public final class NexusCloneChecks {
    private static Fixture running;
    private NexusCloneChecks() { }
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Nexus clone: "+why);}
    public static void run(GameController game){
        require(running==null,"exclusive scheduled fixture");running=new Fixture(game);
        try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw new IllegalStateException(error);}
    }
    public static void tick(GameController game){
        if(game==null||running==null)return;
        try{if(!running.observe())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS nexus-clone (native async/full-height Stonekivi MOVE, two-world TimeStop, source deduplication/capture barrier, exact chest stack and AIR replacement, UUID/entity/passenger delta, player/marker exclusions, callback3centers/16trees and scoped restoration)");}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL nexus-clone",error);}
    }

    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ChefChecks.Fixture base;
        final MatchCoreCheckpoint originalCore;
        final MatchWorldRandomCheckpoint originalRandom;
        final LbcIndirectKills.Checkpoint originalLedger;
        final ServerLevel source,destination;
        final ChunkPos sourceChunk=new ChunkPos(2048,2048);
        final Map<BlockPos,MapManager.BlockSnapshot> sourceOriginal=new LinkedHashMap<>(),destinationOriginal=new LinkedHashMap<>();
        final Map<BlockPos,MapManager.BlockSnapshot> sample=new LinkedHashMap<>();
        final Map<UUID,Vec3> positions=new LinkedHashMap<>();
        final Map<UUID,Entity> originalBodies=new LinkedHashMap<>();
        final Map<UUID,Vec3> lastSourcePositions=new LinkedHashMap<>();
        final Set<UUID> owned=new LinkedHashSet<>(),destinationBefore=new HashSet<>();
        final LbcNexusClone clone;
        RegenerationBeltChecks.ScheduledClient client,audience;
        CompletableFuture<?> sourceLoad,destinationLoad;
        LbcNexusClone.View view;
        LbcIndirectKills.Owner cause;
        Marker marker;
        Cow vehicle,passenger;
        UUID itemId;
        BlockPos chest,airProbe;
        Vec3 playerPosition,delta;
        List<Vec3> centers=List.of(),trees=List.of();
        int stage,when,callbacks,lastCompleted;
        int sourceContainerItems;
        boolean closed,sourceCaptured,destinationCaptured,sourceStopped,destinationStopped,sawProgress;

        Fixture(GameController game){
            this.game=game;
            originalCore=new MatchCoreCheckpoint(game);originalRandom=new MatchWorldRandomCheckpoint(game);
            originalLedger=game.items().indirectKills().checkpoint(new MatchDamageSources(game,List.of()));
            base=new ChefChecks.Fixture(game);source=base.owner.level();destination=game.server().getLevel(LbcHedgehogDimensions.NEXUS);
            require(destination!=null&&source!=destination,"ordinary source and native Nexus");
            require(!game.items().timeStop().stopped(destination),"no borrowed Nexus TimeStop");
            clone=new LbcNexusClone(game,(level,tx,ty,tz,roots,forest,owner)->{
                require(level==destination&&owner==cause,"completion retains native dimension and exact causal owner");
                require(view!=null&&tx-8==view.targetMin().getX()&&ty-8==view.targetMin().getY()&&tz-8==view.targetMin().getZ(),"callback source target geometry");
                callbacks++;centers=List.copyOf(roots);trees=List.copyOf(forest);
                for(var entry:originalBodies.entrySet()){
                    var old=entry.getValue();var moved=destination.getEntity(entry.getKey());
                    SkyWars2.LOGGER.info("[check] nexus-clone transfer uuid={} type={} passenger={} baseline={} lastSource={} oldPosition={} oldLevel={} oldRemoved={} oldMotion={} actual={} expected={} delta={} sourceChunk={} targetMin={}",entry.getKey(),old.getType(),old==passenger,positions.get(entry.getKey()),lastSourcePositions.get(entry.getKey()),old.position(),old.level().dimension(),old.isRemoved(),old.getDeltaMovement(),moved==null?null:moved.position(),positions.get(entry.getKey()).add(delta),delta,sourceChunk,view.targetMin());
                }
            });
        }
        void begin(int next){stage=next;when=game.server().getTickCount();}
        void init(){
            base.init();client=new RegenerationBeltChecks.ScheduledClient(game,base.owner);audience=new RegenerationBeltChecks.ScheduledClient(game,base.other);
            game.config().noBorders=true;base.owner.getAbilities().invulnerable=true;
            sourceLoad=game.maps().prepareChunk(source,sourceChunk);begin(0);
            SkyWars2.LOGGER.info("[check] START nexus-clone (real ticks; reserved source {},{})",sourceChunk.x(),sourceChunk.z());
        }
        AABB box(ServerLevel level,ChunkPos chunk){return new AABB(chunk.getMinBlockX(),level.getMinY(),chunk.getMinBlockZ(),chunk.getMinBlockX()+16,level.getMaxY(),chunk.getMinBlockZ()+16);}
        List<Entity> entities(ServerLevel level,ChunkPos chunk){var area=box(level,chunk);return level.getEntities((Entity)null,area,e->area.contains(e.position()));}
        boolean ready(CompletableFuture<?> future,ServerLevel level,ChunkPos chunk){
            require(!future.isCompletedExceptionally()&&!future.isCancelled(),"native async chunk load succeeds");
            return future.isDone()&&level.getChunkSource().getChunkNow(chunk.x(),chunk.z())!=null;
        }
        /** Empty sections need no per-cell snapshots; AIR is restored by clearing the owned column first. */
        Map<BlockPos,MapManager.BlockSnapshot> column(ServerLevel level,ChunkPos chunk){
            var result=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
            var nativeChunk=level.getChunkSource().getChunkNow(chunk.x(),chunk.z());require(nativeChunk!=null,"only inspect already resident native chunks");
            var sections=nativeChunk.getSections();
            for(int s=0;s<sections.length;s++){
                var section=sections[s];if(section.hasOnlyAir())continue;int y0=nativeChunk.getSectionYFromSectionIndex(s)<<4;
                for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++)if(!section.getBlockState(x,y,z).isAir()){
                    var pos=new BlockPos(chunk.getMinBlockX()+x,y0+y,chunk.getMinBlockZ()+z);result.put(pos,game.maps().snapshot(level,pos));
                }
            }
            return result;
        }
        void restoreColumn(ServerLevel level,ChunkPos chunk,Map<BlockPos,MapManager.BlockSnapshot> original){
            int flags=Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
            for(var pos:column(level,chunk).keySet())game.maps().restoreSnapshot(level,pos,MapManager.BlockSnapshot.air(),flags);
            original.forEach((pos,image)->game.maps().restoreSnapshot(level,pos,image,flags));
        }
        void seed(){
            sourceOriginal.putAll(column(source,sourceChunk));sourceCaptured=true;
            require(sourceOriginal.isEmpty(),"reserved full-height source chunk is genuinely AIR before fixture writes");
            require(entities(source,sourceChunk).isEmpty(),"no foreign source actors");
            int x=sourceChunk.getMinBlockX(),z=sourceChunk.getMinBlockZ();
            chest=new BlockPos(x+5,Math.min(100,source.getMaxY()-12),z+5);
            source.setBlock(new BlockPos(x+1,source.getMinY()+1,z+1),Blocks.BEDROCK.defaultBlockState(),2);
            source.setBlock(new BlockPos(x+14,source.getMaxY()-2,z+14),Blocks.GOLD_BLOCK.defaultBlockState(),2);
            source.setBlock(chest.below(),Blocks.STONE.defaultBlockState(),2);source.setBlock(chest,Blocks.CHEST.defaultBlockState(),2);
            require(source.getBlockEntity(chest) instanceof Container,"real source chest block entity");
            ((Container)source.getBlockEntity(chest)).setItem(4,new ItemStack(Items.DIAMOND,7));
            sourceContainerItems=0;var nativeInventory=(Container)source.getBlockEntity(chest);
            for(int slot=0;slot<nativeInventory.getContainerSize();slot++)sourceContainerItems+=nativeInventory.getItem(slot).getCount();
            require(sourceContainerItems==7,"native pre-MOVE container count is seven");
            sample.putAll(column(source,sourceChunk));
            playerPosition=new Vec3(x+12.5,chest.getY()+4,z+12.5);base.move(base.owner,playerPosition);client.acknowledge();
            game.items().timeStop().begin(source,2000);sourceStopped=true;
            game.items().timeStop().begin(destination,2000);destinationStopped=true;
            marker=new Marker(EntityTypes.MARKER,source);marker.setPos(x+10.5,chest.getY()+4,z+10.5);publish(marker);
            vehicle=cow(new Vec3(x+3.5,chest.getY()+4,z+3.5));passenger=cow(vehicle.position());
            require(passenger.startRiding(vehicle,true,true),"native passenger family mounted");
            var item=new ItemEntity(source,x+7.5,chest.getY()+4,z+7.5,new ItemStack(Items.EMERALD,13));
            item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(32767);publish(item);itemId=item.getUUID();
            for(var entity:List.of(vehicle,passenger,item)){positions.put(entity.getUUID(),entity.position());originalBodies.put(entity.getUUID(),entity);}
            cause=game.items().indirectKills().wildCause(base.owner);
            require(clone.request(source,Vec3.atCenterOf(chest),cause),"native clone accepts while both worlds are stopped");
            require(!clone.request(source,Vec3.atCenterOf(chest.offset(9,0,9)),cause),"whole source chunk is deduplicated");
            require(clone.pending()==1&&!clone.captureReady()&&clone.sourceInFlight(source,sourceChunk),"accepted construction holds its capture barrier");
            view=clone.snapshot().getFirst();
            require(view.sourceLevel()==source&&view.sourceChunk().equals(sourceChunk)&&view.destination()==destination&&view.count()==256*source.getHeight()&&view.completed()==0,"native full-height source footprint");
            delta=new Vec3(view.targetMin().getX()-x,view.targetMin().getY()-source.getMinY(),view.targetMin().getZ()-z);
            destinationLoad=game.maps().prepareChunk(destination,view.destinationChunk());begin(1);
        }
        void publish(Entity entity){require(source.addFreshEntity(entity),"owned native actor published");owned.add(entity.getUUID());}
        Cow cow(Vec3 pos){var cow=new Cow(EntityTypes.COW,source);cow.setPos(pos);cow.setNoAi(true);cow.setNoGravity(true);cow.setDeltaMovement(Vec3.ZERO);publish(cow);return cow;}
        BlockPos target(BlockPos pos){return pos.offset((int)delta.x,(int)delta.y,(int)delta.z);}
        void hold(){require(clone.pending()==1&&!clone.captureReady()&&clone.snapshot().getFirst().completed()==0&&callbacks==0,"native scheduler does not write through either TimeStop");}

        boolean observe(){
            int elapsed=game.server().getTickCount()-when;require(game.playing()&&elapsed<1200,"bounded real-tick phase "+stage+" completed="+lastCompleted);
            if(stage==0){if(ready(sourceLoad,source,sourceChunk))seed();return false;}
            if(stage==1){
                hold();if(!ready(destinationLoad,destination,view.destinationChunk()))return false;
                destinationOriginal.putAll(column(destination,view.destinationChunk()));destinationCaptured=true;
                for(var entity:entities(destination,view.destinationChunk()))destinationBefore.add(entity.getUUID());
                require(destinationBefore.isEmpty(),"random destination has no foreign actors");
                airProbe=target(chest.offset(3,0,0));require(source.getBlockState(chest.offset(3,0,0)).isAir(),"AIR probe source is empty");
                destination.setBlock(airProbe,Blocks.IRON_BLOCK.defaultBlockState(),2);begin(2);return false;
            }
            if(stage==2){
                hold();require(destination.getBlockState(airProbe).is(Blocks.IRON_BLOCK)&&source.getBlockState(chest).is(Blocks.CHEST),"both frozen footprints retain original content");
                if(elapsed<3)return false;
                game.items().timeStop().end(source);sourceStopped=false;begin(3);return false;
            }
            if(stage==3){
                hold();if(elapsed<3)return false;
                game.items().timeStop().begin(source,2000);sourceStopped=true;
                game.items().timeStop().end(destination);destinationStopped=false;begin(4);return false;
            }
            if(stage==4){
                hold();if(elapsed<3)return false;
                for(var id:positions.keySet())positions.put(id,Objects.requireNonNull(source.getEntity(id)).position());
                game.items().timeStop().end(source);sourceStopped=false;begin(5);return false;
            }
            if(clone.pending()!=0){
                var current=clone.snapshot().getFirst();require(!clone.captureReady()&&current.completed()>=lastCompleted,"construction barrier remains closed through monotonic native progress");
                sawProgress|=current.completed()>0;lastCompleted=current.completed();
                require(callbacks==0&&source.getEntity(itemId)!=null&&source.getEntity(vehicle.getUUID())!=null,"native entity transfer waits for the entire terrain MOVE");
                for(var id:positions.keySet())lastSourcePositions.put(id,Objects.requireNonNull(source.getEntity(id)).position());
                return false;
            }
            verify();return true;
        }
        void verify(){
            game.items().timeStop().begin(source,2000);sourceStopped=true;
            game.items().timeStop().begin(destination,2000);destinationStopped=true;
            require(sawProgress&&clone.captureReady()&&!clone.sourceInFlight(source,sourceChunk)&&clone.snapshot().isEmpty()&&callbacks==1,"one fully completed native operation releases its construction barrier");
            require(column(source,sourceChunk).isEmpty(),"entire full-height source becomes AIR without container drops");
            require(destination.getBlockState(airProbe).isAir(),"source AIR overwrites pre-existing destination blocks");
            for(var entry:sample.entrySet())require(destination.getBlockState(target(entry.getKey())).equals(entry.getValue().state()),"full-height translated distinct native block "+entry.getKey());
            require(destination.getBlockEntity(target(chest)) instanceof Container,"container moves as an actual native block entity");
            var inventory=(Container)destination.getBlockEntity(target(chest));int diamonds=0;
            for(int i=0;i<inventory.getContainerSize();i++){var stack=inventory.getItem(i);require(stack.isEmpty()||i==4&&stack.is(Items.DIAMOND)&&stack.getCount()==7,"exact chest slot/stack, no duplicated inventory");diamonds+=stack.getCount();}
            require(diamonds==sourceContainerItems&&source.getBlockEntity(chest)==null,"native container item count is conserved only at destination");
            for(var entry:positions.entrySet()){
                var moved=destination.getEntity(entry.getKey());require(moved!=null&&source.getEntity(entry.getKey())==null,"native entity UUID moved once across dimensions");
                var old=originalBodies.get(entry.getKey());var expected=entry.getValue().add(delta);
                require(moved.position().distanceToSqr(expected)<1e-8,"entity uses exact chunk-minimum delta, never absolute origin; uuid="+entry.getKey()+" type="+moved.getType()+" passenger="+(entry.getKey().equals(passenger.getUUID()))+" sourceOrigin="+entry.getValue()+" lastSource="+lastSourcePositions.get(entry.getKey())+" oldPosition="+old.position()+" oldLevel="+old.level().dimension()+" oldRemoved="+old.isRemoved()+" actual="+moved.position()+" expected="+expected+" difference="+moved.position().subtract(expected)+" delta="+delta+" sourceChunk="+sourceChunk+" targetMin="+view.targetMin());
            }
            var movedVehicle=destination.getEntity(vehicle.getUUID());var movedPassenger=destination.getEntity(passenger.getUUID());
            require(movedPassenger.getVehicle()==movedVehicle&&movedVehicle.getPassengers().contains(movedPassenger),"native passenger links survive cross-world transfer");
            var movedItem=destination.getEntity(itemId);require(movedItem instanceof ItemEntity item&&item.getItem().is(Items.EMERALD)&&item.getItem().getCount()==13,"actual thirteen-item entity stack retained");
            var destinationItems=entities(destination,view.destinationChunk()).stream().filter(e->e instanceof ItemEntity).map(e->(ItemEntity)e).toList();
            require(destinationItems.size()==1&&destinationItems.getFirst().getUUID().equals(itemId)&&entities(source,sourceChunk).stream().noneMatch(e->e instanceof ItemEntity),"one native item stack, no container drops or duplicate source items");
            require(base.owner.level()==source&&base.owner.position().distanceToSqr(playerPosition)<1e-8&&source.getEntity(marker.getUUID())==marker&&destination.getEntity(marker.getUUID())==null,"players and source marker stay behind; playerLevel="+base.owner.level().dimension()+", source="+source.dimension()+", playerPos="+base.owner.position()+", expected="+playerPosition+", marker="+source.getEntity(marker.getUUID())+", markerRef="+marker+", markerRemoved="+marker.isRemoved()+", markerLevel="+marker.level().dimension());
            require(centers.size()==3&&trees.size()==16,"completion supplies exactly three red centers and sixteen tree roots without seeding ecology");
            for(var center:centers){var pos=BlockPos.containing(center);require(new ChunkPos(pos.getX()>>4,pos.getZ()>>4).equals(view.destinationChunk()),"center stays in translated chunk");}
            for(int i=0;i<trees.size();i++)require(trees.get(i).equals(new Vec3(view.targetMin().getX()+8.5,destination.getMinY()+(destination.getHeight()-1)*i/15.0+.5,view.targetMin().getZ()+8.5)),"sixteen full-height source tree roots");
        }
        @Override public void close(){
            if(closed)return;closed=true;boolean wasFrozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
            Throwable failure=null;
            for(Runnable action:List.<Runnable>of(
                    clone::stop,
                    ()->{for(var level:List.of(source,destination))for(var id:owned){var entity=level.getEntity(id);if(entity!=null){if(entity instanceof LivingEntity living)game.items().indirectKills().clear(living);entity.discard();}}},
                    ()->{if(sourceCaptured)restoreColumn(source,sourceChunk,sourceOriginal);},
                    ()->{if(destinationCaptured)restoreColumn(destination,view.destinationChunk(),destinationOriginal);},
                    ()->{if(sourceStopped)game.items().timeStop().end(source);if(destinationStopped)game.items().timeStop().end(destination);},
                    base::close,
                    originalCore::restoreFrozen,
                    ()->require(originalLedger.restoreFrozen(id->game.server().getPlayerList().getPlayer(id)).isEmpty(),"original native causal ledger restored"),
                    originalRandom::restoreFrozen,
                    ()->{if(client!=null)client.close();if(audience!=null)audience.close();})){
                try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            }
            game.server().tickRateManager().setFrozen(wasFrozen);if(failure!=null)throw new IllegalStateException("Nexus clone fixture restoration",failure);
        }
    }
}
