package me.skywars2.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Actual full-sized native transforms and complete coordinator rewind between source cases. */
public final class MapRotationChecks {
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Map rotation: "+why);}
    private static Fixture running;
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ElementalGrimoireChecks.Fixture base;
        final MatchCoreCheckpoint core;
        final MapManager.SpawnState anchors;
        final List<MatchPlayerCheckpoint> players;
        final Map<BlockPos,MapManager.BlockSnapshot> original=new LinkedHashMap<>();
        final CompletableFuture<?> prepared;
        final List<BlockPos> probes=List.of(new BlockPos(4,123,9),new BlockPos(5,80,11),new BlockPos(-159,0,-159),new BlockPos(158,319,158),new BlockPos(160,100,4));
        final List<BlockPos> origins=List.of(new BlockPos(0,160,0),new BlockPos(3,160,5));
        Marker marker;Cow vehicle,rider;
        Vec3 markerAt;int phase,index,frames,stat;
        boolean closed,sawPublication;
        Fixture(GameController game){
            this.game=game;require(!game.vanity().hasSnapshot()&&!game.vanity().busy()&&!game.maps().rotationBusy(),"exclusive native world fixture");
            core=new MatchCoreCheckpoint(game);anchors=game.maps().spawnState();players=game.server().getPlayerList().getPlayers().stream().map(p->new MatchPlayerCheckpoint(game,p)).toList();
            base=new ElementalGrimoireChecks.Fixture(game);base.initialize();
            stat=base.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(Items.CARROT_ON_A_STICK));
            var loads=new ArrayList<CompletableFuture<?>>();
            for(int x=-11;x<=11;x++)for(int z=-11;z<=11;z++)loads.add(game.maps().prepareChunk(base.owner.level(),new ChunkPos(x,z)));
            prepared=CompletableFuture.allOf(loads.toArray(CompletableFuture[]::new));
            BobaChecks.geometry();
        }
        void ack(){for(var p:game.server().getPlayerList().getPlayers())((MatchPlayerCheckpoint.SimulatedAcknowledgement)p.connection).skywars2$acknowledgeRestore();}
        void setup(){
            var level=base.owner.level();
            for(var pos:probes){original.put(pos,game.maps().snapshot(level,pos));for(var origin:origins){var target=MapRotation.blockDestination(pos,origin);original.putIfAbsent(target,game.maps().snapshot(level,target));}}
            for(int i=0;i<probes.size();i++)level.setBlock(probes.get(i),(i==1?Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.WEST):i==2?Blocks.GOLD_BLOCK.defaultBlockState():i==3?Blocks.DIAMOND_BLOCK.defaultBlockState():Blocks.OBSIDIAN.defaultBlockState()),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
            var chest=(ChestBlockEntity)level.getBlockEntity(probes.get(1));var item=new ItemStack(Items.DIAMOND,17);chest.setItem(0,item);chest.setChanged();
            marker=new Marker(EntityTypes.MARKER,level);marker.setPos(13.25,140.5,19.75);marker.setYRot(37);marker.setXRot(-24);marker.setDeltaMovement(new Vec3(.3,-.2,.5));require(level.addFreshEntity(marker),"native marker with nonzero retained momentum");markerAt=marker.position();
            vehicle=new Cow(EntityTypes.COW,level);vehicle.setNoAi(true);vehicle.setNoGravity(true);vehicle.setPos(20,150,23);require(level.addFreshEntity(vehicle),"native vehicle");
            rider=new Cow(EntityTypes.COW,level);rider.setNoAi(true);rider.setNoGravity(true);rider.setPos(20,151,23);require(level.addFreshEntity(rider)&&rider.startRiding(vehicle,true,false),"native passenger graph");
            require(game.vanity().requestCapture(base.owner,false),"native full-world capture before transform");
        }
        boolean step(){
            require(++frames<1800,"bounded source rotation/coordinator "+phase+" "+game.vanity().status());ack();
            var level=base.owner.level();
            if(phase==0){if(!prepared.isDone())return false;prepared.join();setup();phase=1;return false;}
            if(phase==1){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;
                BobaChecks.rejectedUses(game,base.owner);ack();
                if(index==0){base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);var token=BobaChecks.token(3);base.owner.setItemInHand(InteractionHand.MAIN_HAND,token);
                    require(base.owner.gameMode.useItem(base.owner,level,token,InteractionHand.MAIN_HAND).consumesAction()&&token.getCount()==2&&game.maps().rotationBusy(),"actual source Boba input starts full-sized fixed-center transform and debits once");}
                else require(game.maps().rotateMap(base.owner,origins.get(index)),"shared engine accepts shifted unaligned source cube");
                BobaChecks.busyUses(game,base.owner);base.owner.setDeltaMovement(Vec3.ZERO);
                require(!game.items().stonekivi().captureSettled(),"accepted transform prevents saving half of its terrain");
                phase=2;return false;
            }
            if(phase==2){
                if(game.maps().rotationWrites()>0&&!sawPublication){
                    sawPublication=true;
                    for(int i=0;i<probes.size();i++){
                        var p=probes.get(i);var origin=origins.get(index);
                        var target=MapManager.rotationContains(Vec3.atCenterOf(p),origin)?MapRotation.blockDestination(p,origin):p;
                        var expected=i==1?Blocks.CHEST:i==2?Blocks.GOLD_BLOCK:i==3?Blocks.DIAMOND_BLOCK:Blocks.OBSIDIAN;
                        require(level.getBlockState(target).is(expected),"first visible writes already contain complete region publication");
                    }
                    require(marker.position().distanceToSqr(MapRotation.entityDestination(markerAt,origins.get(index)))<1e-12,"entity transport follows complete block publication in same commit tick");
                }
                if(game.maps().rotationBusy())return false;
                require(game.maps().rotationVisits()==32_768_000L&&game.maps().rotationWrites()>0,"all source cube cells accounted for, native writes performed");
                for(int i=0;i<probes.size();i++){
                    var from=probes.get(i);boolean inside=from.getX()>=origins.get(index).getX()-160&&from.getX()<origins.get(index).getX()+160&&from.getZ()>=origins.get(index).getZ()-160&&from.getZ()<origins.get(index).getZ()+160;
                    var target=inside?MapRotation.blockDestination(from,origins.get(index)):from;
                    var wanted=i==1?Blocks.CHEST:i==2?Blocks.GOLD_BLOCK:i==3?Blocks.DIAMOND_BLOCK:Blocks.OBSIDIAN;
                    require(level.getBlockState(target).is(wanted),"native block transpose/clipped edge "+from+" -> "+target);
                    if(i==1){require(level.getBlockState(target).getValue(ChestBlock.FACING)==Direction.WEST,"source preserves block facing, not rotated state");
                        var chest=(ChestBlockEntity)level.getBlockEntity(target);require(chest!=null&&chest.getItem(0).is(Items.DIAMOND)&&chest.getItem(0).getCount()==17&&chest.getBlockPos().equals(target),"native chest payload and new BE coordinates");}
                }
                require(marker.position().distanceToSqr(MapRotation.entityDestination(markerAt,origins.get(index)))<1e-12&&marker.getDeltaMovement().equals(MapRotation.rotateVector(new Vec3(.3,-.2,.5)))
                        &&Math.abs(marker.getYRot()-MapRotation.rotateLook(37,-24)[0])<1e-5&&Math.abs(marker.getXRot()-MapRotation.rotateLook(37,-24)[1])<1e-5,"native actor uses source pivot with rotated yaw/pitch/momentum");
                require(rider.getVehicle()==vehicle&&vehicle.getPassengers().equals(List.of(rider)),"native passenger graph retained without duplicate members");
                var moved=game.maps().spawnState();require(!anchors.spawns().isEmpty()&&!anchors.islands().isEmpty(),"real map spawn/island reference exists");
                for(int n=0;n<anchors.spawns().size();n++){
                    var before=anchors.spawns().get(n);var expected=MapManager.rotatedSpawn(before,origins.get(index));
                    require(moved.spawns().get(n).equals(expected),"current spawn marker cache follows source entity transform");
                    var island=anchors.islands().get(BlockPos.containing(before.position()));
                    if(island==null)continue;var after=moved.islands().get(BlockPos.containing(expected.position()));
                    require(after!=null&&after.size()==island.size(),"pristine island reference follows spawn anchor");
                    for(var cell:island.entrySet()){
                        var target=MapManager.rotationContains(Vec3.atCenterOf(cell.getKey()),origins.get(index))?MapRotation.blockDestination(cell.getKey(),origins.get(index)):cell.getKey();
                        require(after.containsKey(target)&&after.get(target).state()==cell.getValue().state(),"pristine terrain follows exact clipped block transpose");
                    }
                }
                if(index==0)BobaChecks.nativeRegionPhysics(game);
                require(game.items().stonekivi().captureSettled()&&game.vanity().requestRestoreForCheck(),"actual complete transform rewinds through canonical owners");phase=3;return false;
            }
            if(phase==3){if(game.vanity().busy()||!game.vanity().status().equals("IDLE"))return false;
                require(marker.position().equals(markerAt)&&rider.getVehicle()==vehicle&&level.getBlockState(probes.get(1)).is(Blocks.CHEST),"actual native world/actor/vehicle rewind");
                require(game.maps().spawnState().equals(anchors),"existing core rewind restores current spawn and pristine island reference");
                if(++index<origins.size()){sawPublication=false;phase=1;return false;}
                BobaChecks.bedwars(game,base.owner);
                require(game.vanity().requestRestoreForCheck(),"actual Bedwars loot/target replay");phase=4;return false;
            }
            if(phase==4)return !game.vanity().busy()&&game.vanity().status().equals("IDLE");
            return false;
        }
        @Override public void close(){if(closed)return;closed=true;game.vanity().stop();boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
            try{game.items().stonekivi().stop();for(var entry:original.entrySet())game.maps().restoreSnapshot(base.owner.level(),entry.getKey(),entry.getValue());
                base.close();core.restoreFrozen();for(var player:players)player.restoreFrozen();base.owner.getStats().setValue(base.owner,net.minecraft.stats.Stats.ITEM_USED.get(Items.CARROT_ON_A_STICK),stat);
            }finally{game.server().tickRateManager().setFrozen(frozen);}}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS boba (two actual 320-cube transforms, fixed and unaligned origins, raw native two-hand/busy/debit admission, full BE payload/state, rotated entity pivot/look/momentum/passenger graph and bounded native item/mob/fluid physics, completed-construction guard and real full-world rewinds)");}
        catch(Throwable failure){try{running.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL boba",failure);}}
}
