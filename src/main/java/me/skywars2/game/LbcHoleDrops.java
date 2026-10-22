package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;

/** Terrain only; LbcSpecialDrops owns target selection, admission and one shared debit. */
public final class LbcHoleDrops {
    private final GameController game;

    public LbcHoleDrops(GameController game) { this.game = game; }

    public boolean ready(ItemEntity token, LbcHoleDropItem.Kind kind, ServerPlayer target) {
        if (token.isRemoved() || token.getAge() >= 5999 || !LbcHoleDropItem.matches(token.getItem(), kind)
                || !game.playing() || game.stage() == GameController.Stage.ENDING
                || token.level() != game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level)) return false;
        for (var tag : token.entityTags()) {
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        }
        // An empty nearest-player selector still consumes the source token.
        if (target == null) return true;
        if (target.level() != level || !game.items().wands().validTarget(target)) return false;
        var center = BlockPos.containing(target.position());
        // Keep the footprint and one neighbour resident for native block updates.
        for (int x = (center.getX() - 2) >> 4; x <= (center.getX() + 2) >> 4; x++) {
            for (int z = (center.getZ() - 2) >> 4; z <= (center.getZ() + 2) >> 4; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
            }
        }
        return true;
    }

    public boolean spawn(ItemEntity token, LbcHoleDropItem.Kind kind, ServerPlayer target) {
        if (!ready(token, kind, target)) return false;
        if (target == null) return true;
        var level = (ServerLevel) token.level();
        var center = BlockPos.containing(target.position());
        Runnable writes = () -> geometry(level, center, kind);
        if (game.items().specialDrops().owner(token) instanceof ServerPlayer player)
            game.items().indirectKills().trapChanges(level, player, true, writes);
        else writes.run();
        return true;
    }

    private static void geometry(ServerLevel level, BlockPos center, LbcHoleDropItem.Kind kind) {
        if (level.isDebug()) return;
        boolean strict = kind == LbcHoleDropItem.Kind.NORMAL;
        LbcDropBlocks.fill(level, center.offset(1, 0, 1), center.offset(-1, -15, -1), Blocks.AIR, strict);
        LbcDropBlocks.fill(level, center.offset(1, -16, 1), center.offset(-1, -16, -1), Blocks.COBWEB, strict);
        if (strict) {
            LbcDropBlocks.fill(level, center.offset(1, -18, 1), center.offset(-1, -18, -1), Blocks.OAK_LOG, true);
            LbcDropBlocks.fill(level, center.offset(1, -19, 1), center.offset(-1, -19, -1), Blocks.COBBLESTONE, true);
            LbcDropBlocks.fill(level, center.offset(1, -17, 1), center.offset(-1, -17, -1), Blocks.FIRE, true);
        } else {
            LbcDropBlocks.fill(level, center.offset(1, -18, 1), center.offset(-1, -18, -1), Blocks.SOUL_SOIL, false);
            LbcDropBlocks.fill(level, center.offset(1, -17, 1), center.offset(-1, -17, -1), Blocks.SOUL_FIRE, false);
        }
    }
}
