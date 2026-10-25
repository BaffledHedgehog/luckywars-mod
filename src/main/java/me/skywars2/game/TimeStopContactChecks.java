package me.skywars2.game;

import me.skywars2.SkyWars2;
import me.skywars2.mixin.TimeStopNativeMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.UpdateInterval;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Predicate;

/** Actual native contacts, tracker metadata and actual-match cleanup; client pixels remain unclaimed. */
public final class TimeStopContactChecks {
    private TimeStopContactChecks() { }
    private static State running;
    private static StopProbe pending;
    private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS;
    private static void require(boolean value,String message) {
        if(!value) throw new IllegalStateException("Time stop contacts: "+message);
    }
    private record StopProbe(ServerLevel level,BlockPos pos,MapManager.BlockSnapshot saved,
            BlockPos anchor,MapManager.BlockSnapshot savedAnchor,PrimedTnt tnt,Vec3 motion) { }
    private static final class State implements AutoCloseable {
        final GameController game; final ServerLevel level; final LbcTimeStop service;
        final ServerPlayer viewer;
        final BlockPos origin;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final List<Entity> actors=new ArrayList<>();
        final Set<Entity> originalItems=new HashSet<>();
        final Vec3 motion=new Vec3(.23,.17,-.31);
        PrimedTnt tnt,overlap; FallingBlockEntity falling;
        int ticks,lastTick;
        State(GameController game) {
            this.game=game;level=game.maps().level();service=game.items().timeStop();
            require(game.playing() && !service.stopped(level),"actual unstopped match");
            viewer=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(viewer!=null,"native tracking viewer");
            origin=new BlockPos(viewer.blockPosition().getX(),280,viewer.blockPosition().getZ());
            for(int x=-2;x<=7;x++)for(int y=-1;y<=2;y++)for(int z=-1;z<=1;z++) {
                var pos=origin.offset(x,y,z);blocks.put(pos,game.maps().snapshot(pos));
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),FLAGS);
            }
        }
        PrimedTnt tnt(BlockPos pos,int fuse) {
            var entity=new PrimedTnt(level,pos.getX()+.5,pos.getY(),pos.getZ()+.5,null);
            entity.setFuse(fuse);entity.setDeltaMovement(motion);actors.add(entity);
            require(level.addFreshEntity(entity),"native TNT admission");return entity;
        }
        FallingBlockEntity falling(BlockPos pos) {
            var entity=new FallingBlockEntity(EntityTypes.FALLING_BLOCK,level);
            entity.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);entity.setDeltaMovement(motion);
            actors.add(entity);require(level.addFreshEntity(entity),"native falling block admission");return entity;
        }
        void contacts(BlockPos pos) {
            // A freely moving native mob is blocked horizontally and can stand on the frozen payload.
            var cow=new Cow(EntityTypes.COW,level);actors.add(cow);cow.setNoAi(true);cow.entityTags().add("have_stand");
            cow.setPos(pos.getX()-1,pos.getY()+.1,pos.getZ()+.5);require(level.addFreshEntity(cow),"contact actor admission");
            cow.move(MoverType.SELF,new Vec3(3,0,0));
            require(cow.horizontalCollision && cow.getBoundingBox().maxX<=pos.getX()+1e-6,"native horizontal body contact");
            cow.setPos(pos.getX()+.5,pos.getY()+2,pos.getZ()+.5);cow.move(MoverType.SELF,new Vec3(0,-2,0));
            require(cow.onGround() && Math.abs(cow.getY()-(pos.getY()+1))<1e-6,"native support contact");cow.discard();
            var arrow=new Arrow(level,pos.getX()-1,pos.getY()+.5,pos.getZ()+.5,new ItemStack(Items.ARROW),null);
            actors.add(arrow);arrow.entityTags().add("have_stand");arrow.setNoGravity(true);arrow.setDeltaMovement(3,0,0);
            require(level.addFreshEntity(arrow),"contact arrow admission");arrow.tick();
            require(arrow.getX()<pos.getX()+.01 && arrow.getDeltaMovement().length()<.01,"native arrow sticks at barrier face");arrow.discard();
        }
        void tracker() {
            var packets=new ArrayList<Packet<? super ClientGamePacketListener>>();
            var sync=new ServerEntity.Synchronizer() {
                public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet){packets.add(packet);}
                public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet){packets.add(packet);}
                public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet,Predicate<ServerPlayer> predicate){packets.add(packet);}
            };
            var tracker=new ServerEntity(level,tnt,UpdateInterval.periodic(1),true,sync);
            tracker.sendPairingData(viewer,packets::add);
            requirePacket(packets,"late pairing");packets.clear();
            tnt.setFuse(2);tracker.sendChanges();requirePacket(packets,"native dirty metadata");tnt.setFuse(1);
        }
        void requirePacket(List<Packet<? super ClientGamePacketListener>> packets,String context) {
            var accessor=TimeStopNativeMixin.TntData.skywars2$fuseData();
            var data=packets.stream().filter(p->p instanceof ClientboundSetEntityDataPacket)
                    .map(p->(ClientboundSetEntityDataPacket)p).filter(p->p.id()==tnt.getId()).findFirst().orElseThrow();
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
            var decoded=new ClientboundSetEntityDataPacket[1];
            try {net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(
                    (net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)viewer.connection,()->{
                        ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,data);
                        decoded[0]=ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);
                    });}
            finally {buffer.release();}
            int fuse=(Integer)decoded[0].packedItems().stream().filter(value->value.id()==accessor.id()).findFirst().orElseThrow().value();
            require(fuse>30000,context+" keeps client TNT alive through the entire stop");
            boolean lit=(int)((fuse+1f)/5f)%2==0,nextLit=(int)((fuse-4f)/5f)%2==0;
            require(lit!=nextLit,context+" preserves vanilla five-tick flash alternation");
        }
        void persistence() {
            var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());
            tnt.saveWithoutId(output);var saved=output.buildResult();
            var restored=new PrimedTnt(EntityTypes.TNT,level);
            restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),saved));
            require(restored.getFuse()==1 && !restored.isNoGravity() && restored.getDeltaMovement().equals(motion),
                    "actual native entity save/load preserves original motion/gravity and unchanged fuse");
            require(tnt.getFuse()==1 && tnt.isNoGravity() && tnt.getDeltaMovement().equals(Vec3.ZERO),
                    "save never mutates live frozen TNT");
            var chunk=level.getChunkSource().getChunkNow(origin.getX()>>4,origin.getZ()>>4);
            var snapshot=net.minecraft.world.level.chunk.storage.SerializableChunkData.copyOf(level,chunk);
            var section=snapshot.sectionData().stream().filter(row->row.y()==(origin.getY()>>4)).findFirst().orElseThrow().chunkSection();
            var savedState=section.getBlockState(origin.getX()&15,origin.getY()&15,origin.getZ()&15);
            require(savedState.is(Blocks.CAVE_AIR),
                    "native chunk serialization restores cave air; actual="+savedState+", live="+level.getBlockState(origin)
                            +", chunk="+chunk.getPos()+", section="+(origin.getY()>>4)+", leases="+service.contactCount());
            require(snapshot.heightmaps().isEmpty() && !snapshot.lightCorrect() && level.getBlockState(origin).is(Blocks.BARRIER),
                    "saved snapshot primes native height/light again while live collision remains intact");
            var other=new net.minecraft.world.level.chunk.LevelChunk(level,chunk.getPos());
            require(service.persistedChunk(level,other,snapshot)==snapshot,
                    "same coordinates with different chunk identity never claim another chunk snapshot");
        }
        void initialize() {
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(origin).inflate(10)).forEach(originalItems::add);
            // Native chunks skip all air writes in an empty section; retain a solid anchor for exact air variants.
            level.setBlock(origin.below(),Blocks.STONE.defaultBlockState(),FLAGS);
            level.setBlock(origin,Blocks.CAVE_AIR.defaultBlockState(),FLAGS);
            require(level.getBlockState(origin).is(Blocks.CAVE_AIR),"native fixture installs cave air before lease");
            tnt=tnt(origin,1);overlap=tnt(origin,47);falling=falling(origin.offset(3,0,0));
            service.begin(level,200);
            require(service.contactCount()>=2 && level.getBlockState(origin).is(Blocks.BARRIER)
                    && level.getBlockState(origin.offset(3,0,0)).is(Blocks.BARRIER),"TNT and falling blocks acquire native solids");
            contacts(origin);contacts(origin.offset(3,0,0));tracker();persistence();
            require(tnt.getFuse()==1 && overlap.getFuse()==47,"presentation never overwrites gameplay fuses");
            service.begin(level,200);require(tnt.getFuse()==1,"extension keeps original frozen fuse");
            // Existing solid containers are never replaced; their independent BE inventory stays exact.
            var chestPos=origin.offset(6,0,0);level.setBlock(chestPos,Blocks.CHEST.defaultBlockState(),FLAGS);
            var chest=(ChestBlockEntity)level.getBlockEntity(chestPos);chest.setItem(0,new ItemStack(Items.DIAMOND,7));
            var saved=chest.saveWithFullMetadata(level.registryAccess());var inside=tnt(chestPos,19);service.prepareFrozen(inside);
            require(level.getBlockEntity(chestPos)==chest && saved.equals(chest.saveWithFullMetadata(level.registryAccess())),"preexisting block entity untouched");
            inside.discard();
            // Overlapping holder removal cannot release the cell still owned by the other payload.
            overlap.discard();
        }
        boolean step() {
            int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;
            require(game.playing() && !tnt.isRemoved() && tnt.getFuse()==1 && falling.time==0,"actual native ticks preserve fuse-one TNT and falling age");
            require(tnt.position().equals(Vec3.atBottomCenterOf(origin)) && tnt.getDeltaMovement().equals(Vec3.ZERO),"actual frozen native position and velocity");
            require(level.getBlockState(origin).is(Blocks.BARRIER),"overlap survives removed holder");
            if(++ticks<45)return false;
            tracker();service.end(level);
            require(level.getBlockState(origin).is(Blocks.CAVE_AIR) && level.getBlockState(origin.offset(3,0,0)).is(Blocks.AIR),"thaw restores exact prior airs");
            require(tnt.getFuse()==1 && tnt.getDeltaMovement().equals(motion) && !tnt.isNoGravity()
                    && falling.getDeltaMovement().equals(motion) && !falling.isNoGravity(),"thaw restores original gameplay physics without consuming fuse");
            // New gameplay changes win over a lease at release; no stale restoration or item spill.
            tnt.discard();falling.discard();var changed=tnt(origin,80);service.begin(level,20);
            level.setBlock(origin,Blocks.CHEST.defaultBlockState(),FLAGS);var chest=(ChestBlockEntity)level.getBlockEntity(origin);
            chest.setItem(0,new ItemStack(Items.EMERALD,3));var saved=chest.saveWithFullMetadata(level.registryAccess());service.end(level);
            require(level.getBlockEntity(origin)==chest && saved.equals(chest.saveWithFullMetadata(level.registryAccess())),"new block and BE replacement preserved");changed.discard();
            require(service.contactCount()==0,"all leases released");
            require(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(origin).inflate(10))
                    .stream().allMatch(originalItems::contains),"contact ownership/thaw/replacement creates no item drops");return true;
        }
        public void close() {
            service.end(level);actors.forEach(Entity::discard);
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(entry.getKey(),entry.getValue());
        }
    }
    public static void run(GameController game) {
        require(running==null,"one fixture at a time");running=new State(game);
        try {running.initialize();}catch(RuntimeException|Error error){try {running.close();}finally {running=null;}throw error;}
    }
    public static void tick(GameController game) {
        if(running==null)return;
        try {if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS time-stop-contacts");}}
        catch(Throwable error){try {running.close();}finally {running=null;}SkyWars2.LOGGER.error("[check] FAIL time-stop-contacts",error);}
    }
    public static void stopProbe(GameController game) {
        require(game.playing(),"actual match stop probe");var level=game.maps().level();var service=game.items().timeStop();
        // Earlier actual-stop probes may leave a borrowed world pause; this fixture owns its next one.
        service.end(level);
        require(!service.stopped(level),"stop probe begins unpaused");
        var viewer=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        var pos=new BlockPos(viewer.blockPosition().getX()+5,282,viewer.blockPosition().getZ());var saved=game.maps().snapshot(pos);
        var anchor=pos.below();var savedAnchor=game.maps().snapshot(anchor);
        level.setBlock(anchor,Blocks.STONE.defaultBlockState(),FLAGS);
        level.setBlock(pos,Blocks.VOID_AIR.defaultBlockState(),FLAGS);
        require(level.getBlockState(pos).is(Blocks.VOID_AIR),"native stop fixture installs void air before lease");
        var tnt=new PrimedTnt(level,pos.getX()+.5,pos.getY(),pos.getZ()+.5,null);var motion=new Vec3(.21,.13,.15);
        tnt.setFuse(19);tnt.setDeltaMovement(motion);require(level.addFreshEntity(tnt),"stop probe TNT admission");
        pending=new StopProbe(level,pos,saved,anchor,savedAnchor,tnt,motion);service.begin(level,200);
        require(level.getBlockState(pos).is(Blocks.BARRIER),"stop probe owns contact barrier");
        SkyWars2.LOGGER.info("[check] PASS time-stop-contacts-stop-probe");
    }
    public static void cleanup(GameController game) {
        require(pending!=null,"pending real stop probe");var p=pending;
        try {
            require(!game.items().timeStop().stopped(p.level()) && game.items().timeStop().contactCount()==0,"actual match stop clears stop and leases");
            require(p.level().getBlockState(p.pos()).is(Blocks.VOID_AIR),"actual stop restores exact prior void air");
            require(p.tnt().isRemoved() || p.tnt().getFuse()==19 && !p.tnt().isNoGravity() && p.tnt().getDeltaMovement().equals(p.motion()),"actual stop quietly restores or removes frozen TNT");
        } finally {
            p.tnt().discard();game.maps().restoreSnapshot(p.level(),p.pos(),p.saved());
            game.maps().restoreSnapshot(p.level(),p.anchor(),p.savedAnchor());pending=null;
        }
        SkyWars2.LOGGER.info("[check] PASS time-stop-contacts-cleanup");
    }
}
