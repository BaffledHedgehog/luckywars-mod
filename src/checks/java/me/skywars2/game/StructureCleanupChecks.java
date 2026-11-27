package me.skywars2.game;

import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import me.skywars2.validation.mixin.MapValidationAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

/** Place a sparse multi-chunk runtime template, then actually replace the map twice. */
public final class StructureCleanupChecks {
    private static Scene running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Structure cleanup: "+why);}
    public static void run(GameController game){require(running==null&&game.inLobby()&&!game.maps().busy(),"ready lobby");running=new Scene(game);}
    private static ListTag tags(Tag... entries){var list=new ListTag();for(var tag:entries)list.add(tag);return list;}
    private static final class Scene {
        final GameController game;final String oldMap;final boolean oldBroken;
        final net.minecraft.server.level.ServerPlayer player;
        final BlockPos first=new BlockPos(464,180,464),last=first.offset(31,0,15);
        final CompletableFuture<?> ready;CompletableFuture<Boolean> saved;
        int phase,frames;
        Scene(GameController game){this.game=game;oldMap=game.config().mapId;oldBroken=game.config().broken;
            player=java.util.Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            require(!game.maps().arenaTerrainAt(first)&&!game.maps().arenaTerrainAt(last),"both witnesses outside the original map journal");
            ready=CompletableFuture.allOf(game.maps().prepareChunk(game.maps().level(),net.minecraft.world.level.ChunkPos.containing(first)),
                    game.maps().prepareChunk(game.maps().level(),net.minecraft.world.level.ChunkPos.containing(last)));
        }
        boolean step(){
            require(++frames<2400,"bounded real map replacement");var maps=game.maps();var level=maps.level();
            if(phase==0){
                if(!ready.isDone())return false;ready.join();
                require(level.getBlockState(first).isAir()&&level.getBlockState(last).isAir(),"clean native source cells");
                var data=new CompoundTag();data.put("size",tags(IntTag.valueOf(32),IntTag.valueOf(1),IntTag.valueOf(16)));
                data.put("palette",tags(NbtUtils.writeBlockState(Blocks.CHEST.defaultBlockState()),NbtUtils.writeBlockState(Blocks.DIAMOND_BLOCK.defaultBlockState())));
                var a=new CompoundTag();a.put("pos",tags(IntTag.valueOf(0),IntTag.valueOf(0),IntTag.valueOf(0)));a.putInt("state",0);
                var b=new CompoundTag();b.put("pos",tags(IntTag.valueOf(31),IntTag.valueOf(0),IntTag.valueOf(15)));b.putInt("state",1);
                data.put("blocks",tags(a,b));data.put("entities",new ListTag());
                var template=new StructureTemplate();template.load(level.registryAccess().lookupOrThrow(Registries.BLOCK),data);
                maps.placeMapSlice(level,template,first,new StructurePlaceSettings().setIgnoreEntities(true).setKnownShape(true),1234);
                require(level.getBlockState(first).is(Blocks.CHEST)&&level.getBlockState(last).is(Blocks.DIAMOND_BLOCK),"native template really spans chunk boundary");
                var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(first);
                chest.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,64));
                var access=(MapValidationAccess.Manager)(Object)maps;access.validation$flushChangedBlockBounds();saved=access.validation$lastBoundsWrite();
                require(maps.arenaTerrainAt(first)&&maps.arenaTerrainAt(last),"runtime placement registered its complete recovery bounds");
                phase=1;return false;
            }
            if(phase==1){
                if(!saved.isDone())return false;require(saved.getNow(false),"structure cleanup bounds durable before clear");
                require(game.selectMap(player,oldMap.equals("60")?"61":"60",false),"real next map selection");phase=2;return false;
            }
            if(phase==2){
                if(!game.selectedMapReady()||maps.busy())return false;
                require(level.getBlockState(first).isAir()&&level.getBlockState(last).isAir()&&level.getBlockEntity(first)==null,
                        "both distant runtime structure cells and container removed by real map replacement");
                require(game.selectMap(player,oldMap,oldBroken),"restore original selected map");phase=3;return false;
            }
            return game.selectedMapReady()&&!maps.busy()&&game.config().mapId.equals(oldMap)&&game.config().broken==oldBroken;
        }
    }
    public static void tick(GameController game){
        if(running==null)return;
        try{if(!running.step())return;running=null;SkyWars2.LOGGER.info("[check] PASS structure-cleanup (real sparse32x16 runtime template, durable bounds and native next-map cleanup)");}
        catch(Throwable error){running=null;SkyWars2.LOGGER.error("[check] FAIL structure-cleanup",error);}
    }
}
