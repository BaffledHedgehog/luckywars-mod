package me.skywars2.game;

import java.util.LinkedHashMap;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Real native collision iteration, including the two build-height boundary blocks. */
public final class BlockCollisionHeightChecks {
    private BlockCollisionHeightChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Block collision height: "+why);}
    public static void run(GameController game){
        var level=game.maps().level();var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(player!=null,"actual participant");int x=player.blockPosition().getX(),z=player.blockPosition().getZ();
        var bottom=new BlockPos(x,level.getMinY(),z);var roof=new BlockPos(x,level.getMaxY(),z);
        require(level.getChunkSource().getChunkNow(x>>4,z>>4)!=null,"resident column only");
        var saved=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
        for(var pos:java.util.List.of(bottom,roof))saved.put(pos,game.maps().snapshot(level,pos));
        try{
            level.setBlock(bottom,Blocks.STONE.defaultBlockState(),2|16|32);level.setBlock(roof,Blocks.STONE.defaultBlockState(),2|16|32);
            // A huge horizontal footprint must be empty before any chunk query or overflowing Cursor3D volume.
            for(var box:java.util.List.of(new AABB(-1e9,level.getMaxY()+100, -1e9,1e9,1e10,1e9),
                    new AABB(-1e9,-1e10,-1e9,1e9,level.getMinY()-100,1e9))){
                var collision=new BlockCollisions<>(level,CollisionContext.empty(),box,false,(pos,shape)->pos.immutable());
                require(!collision.hasNext(),"entirely absent vertical terrain has no block collisions");
            }
            var crossing=new BlockCollisions<>(level,CollisionContext.empty(),new AABB(x+.1,-1e10,z+.1,x+.9,1e10,z+.9),false,(pos,shape)->pos.immutable());
            boolean floor=false,ceiling=false;int count=0;
            while(crossing.hasNext()){var pos=crossing.next();floor|=pos.equals(bottom);ceiling|=pos.equals(roof);require(++count<=level.getHeight(),"clipped collision iterator stays within finite native column");}
            require(floor&&ceiling,"cross-boundary sweep retains both initial world segment and roof block");
            level.setBlock(roof,Blocks.OAK_FENCE.defaultBlockState(),2|16|32);
            var fence=new BlockCollisions<>(level,CollisionContext.empty(),new AABB(x+.4,level.getMaxY()+1.1,z+.4,x+.6,level.getMaxY()+1.4,z+.6),false,(pos,shape)->pos.immutable());
            require(fence.hasNext()&&fence.next().equals(roof),"topmost large fence collision protruding above build height is retained");
        }finally{saved.forEach((pos,state)->game.maps().restoreSnapshot(level,pos,state));}
        SkyWars2.LOGGER.info("[check] PASS block-collision-height (native above/below huge empty volume, crossing10b vertical column includes min/max blocks, protruding top fence retained; entity/worldborder paths unchanged)");
    }
}
