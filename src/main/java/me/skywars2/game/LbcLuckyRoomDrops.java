package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;

/** Ten authored strict writes; the shared dispatcher owns admission and the unit debit. */
public final class LbcLuckyRoomDrops {
    private final GameController game;
    public LbcLuckyRoomDrops(GameController game) { this.game = game; }

    public boolean ready(ItemEntity token) {
        if (token.isRemoved() || token.getAge() >= 5999 || !LbcLuckyRoomDropItem.matches(token.getItem())
                || !game.playing() || game.stage() == GameController.Stage.ENDING
                || token.level() != game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level)) return false;
        for (var tag : token.entityTags())
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        var target = game.items().cageDrops().target(token);
        if (target == null) return true; // Source execute does nothing but still debits the token.
        var center = BlockPos.containing(target.position());
        // No chunk loads: include the one-block native shape-update neighbourhood.
        for (int x=(center.getX()-3)>>4; x<=(center.getX()+3)>>4; x++)
            for (int z=(center.getZ()-3)>>4; z<=(center.getZ()+3)>>4; z++)
                if (level.getChunkSource().getChunkNow(x,z)==null) return false;
        return true;
    }

    public boolean spawn(ItemEntity token) {
        if (!ready(token)) return false;
        var target = game.items().cageDrops().target(token);
        if (target == null) return true;
        var level = (ServerLevel) token.level();
        var center = BlockPos.containing(target.position());
        Runnable writes = () -> geometry(level,center);
        var author = LbcIndirectKills.playerSource(game.items().specialDrops().owner(token));
        if (author != null) game.items().indirectKills().trapChanges(level,author,true,writes);
        else writes.run();
        return true;
    }

    private static void geometry(ServerLevel level, BlockPos center) {
        if (level.isDebug()) return;
        fill(level,center.offset(-2,-1,-2),center.offset(2,1,2),Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.YELLOW).defaultBlockState());
        fill(level,center.offset(-1,0,-1),center.offset(1,1,1),Blocks.AIR.defaultBlockState());
        for (int x : new int[]{-2,2}) for (int z : new int[]{-2,2})
            place(level,center.offset(x,2,z),Blocks.GOLD_BLOCK.defaultBlockState());
        var mushroom = LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NORMAL).defaultBlockState();
        for (int x : new int[]{-1,1}) for (int z : new int[]{-1,1})
            place(level,center.offset(x,0,z),mushroom);
    }

    private static void fill(ServerLevel level, BlockPos from, BlockPos to, net.minecraft.world.level.block.state.BlockState state) {
        if (level.isOutsideBuildHeight(from) || level.isOutsideBuildHeight(to)) return;
        long volume=((long)Math.abs(to.getX()-from.getX())+1)*((long)Math.abs(to.getY()-from.getY())+1)*((long)Math.abs(to.getZ()-from.getZ())+1);
        if (volume>level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS)) return;
        for (var pos:BlockPos.betweenClosed(from,to)) place(level,pos,state);
    }

    /** Strict818 suppresses native BE removal side effects; retain actual overwritten contents explicitly. */
    private static void place(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (level.isDebug() || level.isOutsideBuildHeight(pos)) return;
        var old=level.getBlockState(pos);
        var container=level.getBlockEntity(pos) instanceof net.minecraft.world.Container contents ? contents : null;
        var input=new net.minecraft.commands.arguments.blocks.BlockInput(state,java.util.Set.of(),null);
        if (input.place(level,pos,818) && container!=null && !state.shouldChangedStateKeepBlockEntity(old)) {
            net.minecraft.world.Containers.dropContents(level,pos,container);
            container.clearContent();
        }
    }
}
