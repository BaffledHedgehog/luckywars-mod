package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Real lobby requests supersede both decoding and partially applied native map work. */
public final class MapReplacementChecks {
    private static Scene active;
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException("Map replacement: "+message);}
    private static Object field(Object object,String name){try{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    public static void run(GameController game){require(active==null&&game.inLobby()&&!game.maps().busy(),"exclusive ready lobby");active=new Scene(game);active.start();}
    private static final class Scene {
        final GameController game;final ServerPlayer owner;final String oldMap;final boolean oldBroken;final int started;
        int phase;
        Scene(GameController game){this.game=game;owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));oldMap=game.config().mapId;oldBroken=game.config().broken;started=game.server().getTickCount();}
        void select(String id){require(game.selectMap(owner,id,false),"actual native selection accepted "+id);}
        void start(){select("60");var first=field(game.maps(),"preparation");select("61");require(first!=field(game.maps(),"preparation"),"second click replaces the in-flight preparation immediately");select("1");require(game.maps().busy()&&!game.selectedMapReady(),"latest request is the sole active operation");}
        boolean tick(){
            require(game.inLobby()&&game.server().getTickCount()-started<3600,"bounded lobby scene");
            if(phase==0){
                if(game.maps().streamingChangedChunks().isEmpty())return false;
                require(game.maps().busy(),"cancel after real native blocks were changed, before completion");
                select("61");phase=1;return false;
            }
            if(!game.selectedMapReady())return false;
            if(phase==1){require(game.config().mapId.equals("61"),"last requested map is the only ready selection");imprinted(game);require(game.selectMap(owner,oldMap,oldBroken),"restore actual original selection");phase=2;return false;}
            require(game.config().mapId.equals(oldMap)&&game.config().broken==oldBroken,"original selection restored through the public loader");return true;
        }
    }
    public static void tick(GameController game){if(active==null)return;try{if(!active.tick())return;active=null;SkyWars2.LOGGER.info("[check] PASS map-replacement (actual rapid selections replace decoding and partial native writes; final readiness, Imprinted mirror and original selection restored)");}catch(Throwable error){active=null;SkyWars2.LOGGER.error("[check] FAIL map-replacement",error);}}
    @SuppressWarnings("unchecked") public static void imprinted(GameController game){
        var level=game.maps().level();var target=Objects.requireNonNull(game.server().getLevel(LbcHedgehogDimensions.IMPRINTED));
        require(!game.maps().busy(),"copy completes before ready callback");
        var bounds=(List<BoundingBox>)field(game.maps(),"mapBounds");int retained=0,missing=0,other=0,sampled=0;
        search:for(var box:bounds)for(int y=box.maxY();y>=box.minY();y--)for(int x=box.minX();x<=box.maxX();x++)for(int z=box.minZ();z<=box.maxZ();z++){
            var at=new BlockPos(x,y,z);var original=level.getBlockState(at);if(original.isAir())continue;
            // Starting play releases temporary loading tickets. Read the actual persisted native
            // chunk when it has already unloaded; an absent copy still fails the block comparison.
            var chunk=target.getChunkSource().getChunkNow(x>>4,z>>4);if(chunk==null)chunk=target.getChunk(x>>4,z>>4);
            var copy=chunk.getBlockState(at);if(copy.isAir())missing++;else if(copy.getBlock()==original.getBlock())retained++;else other++;
            if(++sampled==5000)break search;
        }
        require(sampled>=250&&retained>sampled*.5&&missing>sampled*.1&&missing<sampled*.5&&other<sampled*.15,"current native map reproduced with broken integrity: sampled="+sampled+",retained="+retained+",missing="+missing+",other="+other);
        SkyWars2.LOGGER.info("[check] PASS map-imprinted (actual current map in Imprinted, native block comparison: sampled={}, retained={}, missing={}, other={})",sampled,retained,missing,other);
    }
}
