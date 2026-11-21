package me.skywars2.game;

import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import me.skywars2.game.match.checkpoints.MatchTerrainCheckpoint;
import me.skywars2.game.world.maps.MapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

/** Pride includes outside-arena terrain and later chunks, while leaving Nexus alone. */
public final class ArenaBoundsChecks {
    private static Scene running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Arena bounds: "+why);}
    public static void run(GameController game){require(running==null,"exclusive scene");running=new Scene(game);}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final ServerLevel nexus;
        final BlockPos inside,outside,later,nexusAt;final CompletableFuture<?> ready;
        CompletableFuture<?> laterReady;MatchTerrainCheckpoint image;
        MapManager.BlockSnapshot oldNexus;MatchTerrainCheckpoint.Restore restore;
        int phase,frames;long maxStep,captureNanos;boolean closed;
        Scene(GameController game){
            this.game=game;f=new WoolBucketChecks.Fixture(game,false);f.prepareParticipants();
            inside=new BlockPos(MapManager.OUTER_ARENA_LIMIT,f.level.getMaxY()-20,0);outside=inside.east();
            later=outside.east(512);
            nexus=game.server().getLevel(me.skywars2.game.entities.mobs.hedgehogs.LbcHedgehogDimensions.NEXUS);
            require(nexus!=null,"native Nexus exists");nexusAt=new BlockPos(8,64,8);
            ready=CompletableFuture.allOf(game.maps().prepareChunk(f.level,ChunkPos.containing(inside)),game.maps().prepareChunk(nexus,ChunkPos.containing(nexusAt)));
        }
        boolean step(){
            require(++frames<1000,"bounded restore");
            if(phase==0){
                if(!ready.isDone())return false;ready.join();
                game.server().tickRateManager().setFrozen(true);
                f.set(inside,Blocks.STONE.defaultBlockState());f.set(outside,Blocks.DIAMOND_BLOCK.defaultBlockState());
                f.level.scheduleTick(outside,Blocks.DIAMOND_BLOCK,60);
                oldNexus=game.maps().snapshot(nexus,nexusAt);nexus.setBlock(nexusAt,Blocks.STONE.defaultBlockState(),2|16|32);
                long start=System.nanoTime();image=MatchTerrainCheckpoint.captureLoaded(game);captureNanos=System.nanoTime()-start;
                f.level.setBlock(inside,Blocks.GOLD_BLOCK.defaultBlockState(),2|16|32);
                f.level.setBlock(outside,Blocks.EMERALD_BLOCK.defaultBlockState(),2|16|32);
                clearOutsideTicks();f.level.scheduleTick(outside,Blocks.EMERALD_BLOCK,120);
                nexus.setBlock(nexusAt,Blocks.GOLD_BLOCK.defaultBlockState(),2|16|32);
                laterReady=game.maps().prepareChunk(f.level,ChunkPos.containing(later));phase=1;return false;
            }
            if(phase==1){
                if(!laterReady.isDone())return false;laterReady.join();
                f.set(later,Blocks.CHEST.defaultBlockState());
                var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)f.level.getBlockEntity(later);
                chest.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,4));
                int chunks=image.chunks();image.include(f.level.getChunkAt(later));
                require(image.chunks()==chunks+1,"later outside chunk extends the snapshot");
                chest.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD,9));
                restore=image.restore(game);phase=2;
            }
            long start=System.nanoTime();boolean done=restore.step(131072);maxStep=Math.max(maxStep,System.nanoTime()-start);
            if(!done)return false;
            require(f.level.getBlockState(inside).is(Blocks.STONE),"inside cell rewinds");
            require(f.level.getBlockState(outside).is(Blocks.DIAMOND_BLOCK),"outside cell in the SAME chunk rewinds");
            require(f.level.getBlockTicks().hasScheduledTick(outside,Blocks.DIAMOND_BLOCK)
                    &&!f.level.getBlockTicks().hasScheduledTick(outside,Blocks.EMERALD_BLOCK),"outside scheduled ticks rewind");
            var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)f.level.getBlockEntity(later);
            require(chest!=null&&chest.getItem(0).is(net.minecraft.world.item.Items.DIAMOND)&&chest.getItem(0).getCount()==4,
                    "later outside chunk restores same-state chest contents");
            require(nexus.getBlockState(nexusAt).is(Blocks.GOLD_BLOCK),"other dimension stays changed");
            SkyWars2.LOGGER.info("[check] arena-bounds timing capture={}ms maxStep={}ms frames={}",captureNanos/1e6,maxStep/1e6,frames);
            return true;
        }
        void clearOutsideTicks(){f.level.getBlockTicks().clearArea(new net.minecraft.world.level.levelgen.structure.BoundingBox(
                outside.getX(),outside.getY(),outside.getZ(),outside.getX(),outside.getY(),outside.getZ()));}
        public void close(){if(closed)return;closed=true;try{if(restore!=null)restore.close();clearOutsideTicks();if(oldNexus!=null)game.maps().restoreSnapshot(nexus,nexusAt,oldNexus,2|16|32);}finally{f.close();}}
    }
    public static void tick(GameController game){
        if(running==null)return;var scene=running;
        try{if(!scene.step())return;scene.close();running=null;SkyWars2.LOGGER.info("[check] PASS arena-bounds");}
        catch(Throwable failure){try{scene.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL arena-bounds",failure);}
    }
}
