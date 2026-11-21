package me.skywars2.game;

import io.netty.buffer.Unpooled;
import java.util.*;
import java.util.function.Predicate;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;

/** Synchronous native packet/physics regression scene; use only the disposable qualification world. */
public final class RotatedPhysicsChecks {
    private RotatedPhysicsChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Rotated physics: "+why);}
    public static void run(GameController game){
        try(var fixture=new Fixture(game)){
            for(int turns=1;turns<=3;turns++){
                fixture.frame(turns);fixture.packets(turns);fixture.falling(turns);fixture.suffocation(turns);
            }
        }
        SkyWars2.LOGGER.info("[check] PASS rotated-physics (all3 frames; actual pairing/dirty/exit metadata codecs and precise correction; authoritative item/snowball/egg/falling gravity; native placed/unsupported sand and landing; support-safe cow and real embedded-head suffocation)");
    }
    private static final class Wire implements ServerEntity.Synchronizer {
        final List<Packet<? super ClientGamePacketListener>> packets=new ArrayList<>();
        @Override public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet){packets.add(packet);}
        @Override public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet){packets.add(packet);}
        @Override public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet,Predicate<ServerPlayer> predicate){packets.add(packet);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerLevel level;final ServerPlayer viewer;final BlockPos at;
        final List<BobaRegionPhysics.Region> regions;
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final List<ScheduledTick<Block>> blockTicks=new ArrayList<>();
        final List<ScheduledTick<Fluid>> fluidTicks=new ArrayList<>();
        final List<Entity> owned=new ArrayList<>();
        final BoundingBox bounds;final LbcIndirectKills.TerrainProvenance provenance;
        Fixture(GameController game){
            this.game=game;level=game.maps().level();
            require(game.playing()&&!game.maps().busy()&&!game.vanity().busy()&&!game.vanity().hasSnapshot()
                    &&!game.items().timeStop().stopped(level),"stable exclusive native match");
            viewer=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            require(viewer.level()==level,"viewer in map dimension");
            at=new BlockPos(Math.floorDiv(viewer.getBlockX(),16)*16+8,
                    Math.clamp(viewer.getBlockY()+12,level.getMinY()+8,level.getMaxY()-9),Math.floorDiv(viewer.getBlockZ(),16)*16+8);
            var chunk=level.getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4);
            require(chunk!=null,"resident fixture chunk; never synchronously load terrain");
            regions=game.maps().rotationPhysics().capture();
            bounds=new BoundingBox(at.getX()-5,at.getY()-5,at.getZ()-5,at.getX()+5,at.getY()+5,at.getZ()+5);
            ((LevelChunkTicks<Block>)chunk.getBlockTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(blockTicks::add);
            ((LevelChunkTicks<Fluid>)chunk.getFluidTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(fluidTicks::add);
            for(var pos:BlockPos.betweenClosed(at.offset(-4,-4,-4),at.offset(4,4,4)))terrain.put(pos.immutable(),game.maps().snapshot(level,pos));
            provenance=game.items().indirectKills().captureTerrain(level,terrain.keySet());
        }
        void set(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
            require(terrain.containsKey(pos),"write belongs to fixture snapshot "+pos);level.setBlock(pos,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        }
        void clear(){
            for(var entity:List.copyOf(owned)){if(entity instanceof LivingEntity living)game.items().indirectKills().clear(living);entity.discard();}
            owned.clear();
            for(var pos:terrain.keySet())set(pos,Blocks.AIR.defaultBlockState());
            level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);
        }
        void frame(int turns){
            clear();game.maps().rotationPhysics().restore(List.of(new BobaRegionPhysics.Region(
                    at.getX()-4,at.getY()-4,at.getZ()-4,at.getX()+5,at.getY()+5,at.getZ()+5,turns)));
        }
        boolean gravity(List<? extends Packet<?>> packets,Entity entity,boolean expected){
            // Polymer's metadata writer requires the actual recipient's scoped registry context.
            return PacketContext.supplyWithContext((PacketContextProvider)(Object)viewer.connection,()->{
            int id=((BobaRegionPhysics.ClientGravityData)entity).boba$gravityData(false).id();boolean found=false;
            for(var packet:packets)if(packet instanceof ClientboundSetEntityDataPacket metadata&&metadata.id()==entity.getId()){
                var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),level.registryAccess());
                try{
                    ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,metadata);
                    var decoded=ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);
                    for(var value:decoded.packedItems())if(value.id()==id){require(Objects.equals(value.value(),expected),"wire NoGravity="+expected+" for "+entity.getType());found=true;}
                }finally{buffer.release();}
            }
            return found;
            });
        }
        void packets(int turns){
            var actors=List.<Entity>of(new ItemEntity(level,at.getX()+.5,at.getY()+.5,at.getZ()+.5,new ItemStack(Items.DIAMOND)),
                    new Snowball(EntityTypes.SNOWBALL,level),new ThrownEgg(EntityTypes.EGG,level),new FallingBlockEntity(EntityTypes.FALLING_BLOCK,level));
            for(var entity:actors){
                owned.add(entity);entity.setPos(Vec3.atCenterOf(at));entity.setDeltaMovement(Vec3.ZERO);entity.setNoGravity(false);
                require(BobaRegionPhysics.turns(entity)==turns,"regional actor frame");
                var wire=new Wire();var tracker=new ServerEntity(level,entity,UpdateInterval.periodic(20),true,wire);
                var pairing=new ArrayList<Packet<ClientGamePacketListener>>();tracker.sendPairingData(viewer,pairing::add);
                require(pairing.stream().anyMatch(ClientboundAddEntityPacket.class::isInstance)&&gravity(pairing,entity,true),"actual late pairing overrides default gravity");
                require(!entity.isNoGravity(),"pairing preserves authoritative gravity flag");
                entity.getEntityData().packDirty();entity.setSilent(true);wire.packets.clear();tracker.sendChanges();
                require(gravity(wire.packets,entity,true)&&!entity.isNoGravity(),"dirty unrelated metadata cannot restore client gravity");
                require(wire.packets.stream().anyMatch(ClientboundEntityPositionSyncPacket.class::isInstance)
                        &&wire.packets.stream().anyMatch(ClientboundSetEntityMotionPacket.class::isInstance),"native precise position and velocity correction bypass slow tracking interval");
                wire.packets.clear();tracker.sendChanges();
                require(wire.packets.stream().anyMatch(ClientboundEntityPositionSyncPacket.class::isInstance),"correction continues after the first tracking tick");
                entity.tick();var local=BobaRegionPhysics.rotate(entity.getDeltaMovement(),-turns);
                require(!entity.isNoGravity()&&local.y<-.01&&Math.abs(local.x)<1e-7&&Math.abs(local.z)<1e-7,"authoritative native gravity survives client-only NoGravity for "+entity.getType());
                entity.setPos(Vec3.atCenterOf(at.offset(8,0,0)));wire.packets.clear();tracker.sendChanges();
                require(gravity(wire.packets,entity,false)&&!entity.isNoGravity(),"leaving region restores native client gravity");
                entity.setNoGravity(true);entity.setPos(Vec3.atCenterOf(at));tracker.sendChanges();
                entity.setPos(Vec3.atCenterOf(at.offset(8,0,0)));wire.packets.clear();tracker.sendChanges();
                require(gravity(wire.packets,entity,true)&&entity.isNoGravity(),"leaving preserves independently owned native NoGravity");
                entity.discard();
            }
            for(var player:game.server().getPlayerList().getPlayers())require(BobaRegionPhysics.turns(player)==0,"player physics excluded");
        }
        List<FallingBlockEntity> fallingAtSource(){
            var found=new ArrayList<FallingBlockEntity>();
            for(var entity:level.getAllEntities())if(entity instanceof FallingBlockEntity falling&&!falling.isRemoved()&&falling.getStartPos().equals(at))found.add(falling);
            return found;
        }
        void falling(int turns){
            clear();var down=BobaRegionPhysics.rotate(Direction.DOWN,turns);var sand=Blocks.SAND.defaultBlockState();
            set(at.relative(down),Blocks.STONE.defaultBlockState());
            require(level.setBlock(at,sand,Block.UPDATE_ALL),"actual newly placed sand");
            require(level.getBlockTicks().hasScheduledTick(at,Blocks.SAND),"native placement schedules its support tick");
            sand.tick(level,at,level.getRandom());
            require(level.getBlockState(at).is(Blocks.SAND)&&fallingAtSource().isEmpty(),"rotated support holds sand despite free vanilla DOWN");
            level.getBlockTicks().clearArea(bounds); // The direct native tick did not consume the scheduler entry.
            set(at.below(),Blocks.STONE.defaultBlockState());set(at.relative(down,2),Blocks.STONE.defaultBlockState());
            require(level.setBlock(at.relative(down),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL),"remove actual rotated support");
            require(level.getBlockTicks().hasScheduledTick(at,Blocks.SAND),"native neighbor update retains scheduled support check");
            sand.tick(level,at,level.getRandom());var spawned=fallingAtSource();owned.addAll(spawned);
            require(spawned.size()==1&&level.getBlockState(at).isAir(),"one native fall starts after rotated support removal despite solid vanilla DOWN");
            var falling=spawned.getFirst();
            require(BlockPos.containing(falling.getBoundingBox().getCenter()).equals(at),"spawn body remains in source cell");
            for(int tick=0;tick<40&&!falling.isRemoved();tick++)falling.tick();
            require(falling.isRemoved()&&level.getBlockState(at.relative(down)).is(Blocks.SAND),"native falling block lands and places against rotated support");
            set(at.relative(down),Blocks.AIR.defaultBlockState());
            level.getBlockTicks().clearArea(bounds);
            require(level.setBlock(at,sand,Block.UPDATE_ALL)&&level.getBlockTicks().hasScheduledTick(at,Blocks.SAND),"fresh unsupported placement schedules without vanilla-down removal");
            sand.tick(level,at,level.getRandom());spawned=fallingAtSource();owned.addAll(spawned);
            require(spawned.size()==1&&level.getBlockState(at).isAir(),"fresh sand falls directly toward rotated DOWN");
        }
        void suffocation(int turns){
            clear();var down=BobaRegionPhysics.rotate(Direction.DOWN,turns);
            for(int depth=1;depth<=2;depth++)for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
                var offset=BobaRegionPhysics.rotate(new Vec3(x,-depth,z),turns);
                set(at.offset((int)offset.x,(int)offset.y,(int)offset.z),Blocks.STONE.defaultBlockState());
            }
            var cow=new Cow(EntityTypes.COW,level);owned.add(cow);cow.setNoAi(true);
            cow.setPos(Vec3.atCenterOf(at).subtract(BobaRegionPhysics.rotate(new Vec3(0,.5,0),turns)));
            cow.setDeltaMovement(Vec3.ZERO);cow.move(MoverType.SELF,new Vec3(down.getStepX()*.2,down.getStepY()*.2,down.getStepZ()*.2));
            require(cow.onGround()&&cow.verticalCollisionBelow&&!cow.isInWall(),"supporting rotated wall grounds cow without suffocation");
            float health=cow.getHealth();cow.baseTick();require(cow.getHealth()==health,"native living baseTick does not damage wall-supported cow");
            var eye=cow.position().add(BobaRegionPhysics.rotate(new Vec3(0,cow.getEyeHeight(),0),turns));
            set(BlockPos.containing(eye),Blocks.STONE.defaultBlockState());
            require(cow.isInWall(),"real embedded rotated head still suffocates");
            cow.setInvulnerableTime(0);cow.damageCooldownTime=0;cow.baseTick();
            require(cow.getHealth()<health,"native suffocation damage remains enabled for actual head obstruction");
        }
        @Override public void close(){
            clear();game.maps().rotationPhysics().restore(regions);
            for(var entry:terrain.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
            level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);
            blockTicks.forEach(level.getBlockTicks()::schedule);fluidTicks.forEach(level.getFluidTicks()::schedule);
            game.items().indirectKills().restoreTerrain(provenance);
        }
    }
}
