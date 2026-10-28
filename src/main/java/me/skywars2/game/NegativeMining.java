package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.Vec3;

/** A held negative native mining rate grows one adjacent copy instead of destroying its source. */
public final class NegativeMining {
    private NegativeMining() { }
    public static final class Session {
        private final ServerLevel level;
        private final BlockPos source;
        private final BlockState state;
        private final String epoch;
        private final ItemStack tool;
        private double progress;
        private final TpsCompensation.Clock clock = new TpsCompensation.Clock();
        private int ticks;
        private double elapsed;
        private Session(ServerLevel level,BlockPos source,BlockState state,String epoch,ItemStack tool) {
            this.level=level;this.source=source.immutable();this.state=state;this.epoch=epoch;
            this.tool=tool.copy();
            clock.reset(0);
        }
        public BlockPos source() {return source;}
    }
    private static boolean resident(ServerLevel level,BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;
    }
    /** Native outline intersection, terminated at unloaded terrain without acquiring chunks. */
    public static BlockHitResult aimedBlock(ServerPlayer player) {
        double range=player.blockInteractionRange();
        if(!Double.isFinite(range) || range<=0)return null;
        var level=player.level();
        Vec3 from=player.getEyePosition(),to=from.add(player.getLookAngle().scale(range));
        var hit=BlockGetter.traverseBlocks(from,to,level,(world,pos)-> {
            if(!resident(world,pos))return Shapes.block().clip(from,to,pos);
            return world.getBlockState(pos).getShape(world,pos,CollisionContext.of(player)).clip(from,to,pos);
        },world->null);
        return hit!=null && resident(level,hit.getBlockPos())?hit:null;
    }
    /** The final destruction guard deliberately does not depend on gameplay/session admission. */
    public static boolean negativeRate(ServerPlayer player,BlockPos pos) {
        var level=player.level();
        if(!mayHaveNegativeRate(player) || !resident(level,pos))return false;
        var state=level.getBlockState(pos);
        if(state.isAir())return false;
        float progress=state.getDestroyProgress(player,level,pos);
        // Unbreakable hardness returns zero before consulting signed speed; creative must not bypass the prohibition.
        return progress<0 || progress==0 && player.getDestroySpeed(state)<0;
    }
    /** Cheap sign prefilter only; exact native per-block progress remains authoritative. */
    public static boolean mayHaveNegativeRate(ServerPlayer player) {
        var haste=player.getEffect(net.minecraft.world.effect.MobEffects.HASTE);
        var conduit=player.getEffect(net.minecraft.world.effect.MobEffects.CONDUIT_POWER);
        return player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_BREAK_SPEED)<0
                || player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MINING_EFFICIENCY)<0
                || (player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)
                    && player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SUBMERGED_MINING_SPEED)<0)
                || (haste!=null && haste.getAmplifier()<0) || (conduit!=null && conduit.getAmplifier()<0);
    }
    private static boolean allowed(GameController game,ServerPlayer player,ServerLevel level,BlockPos source) {
        return game!=null && game.playing() && game.stage()!=GameController.Stage.ENDING
                && player.level()==level && level==game.maps().level() && player.isAlive() && !player.isRemoved()
                && game.items().wands().validTarget(player) && !LbcGreed.active(player)
                && !game.items().actionsBlocked(player) && !game.items().timeStop().stopped(level)
                && !player.isSpectator() && resident(level,source)
                && player.isWithinBlockInteractionRange(source,1.0)
                && level.getWorldBorder().isWithinBounds(source)
                && level.mayInteract(player,source)
                && !level.getServer().isUnderSpawnProtection(level,source,player)
                && !player.blockActionRestricted(level,source,player.gameMode.getGameModeForPlayer());
    }
    /** Called only after vanilla accepted START and established its normal destruction target. */
    public static Session begin(GameController game,ServerPlayer player,BlockPos source) {
        var level=player.level();
        if(!allowed(game,player,level,source))return null;
        var hit=aimedBlock(player);
        if(hit==null || !hit.getBlockPos().equals(source))return null;
        var state=level.getBlockState(source);
        if(state.getBlock() instanceof net.minecraft.world.level.block.GameMasterBlock && !player.canUseGameMasterBlocks())return null;
        if(!player.getMainHandItem().canDestroyBlock(state,level,source,player))return null;
        return !state.isAir() && state.getDestroyProgress(player,level,source)<0
                ?new Session(level,source,state,game.items().epoch(),player.getMainHandItem()):null;
    }
    /** False ends the held action. No global registry, timer, chunk ticket or queued copies survive it. */
    public static boolean tick(GameController game,ServerPlayer player,Session session) {
        if(!allowed(game,player,session.level,session.source) || !game.items().currentEpoch(session.epoch)
                || session.level.getBlockState(session.source)!=session.state
                || !ItemStack.isSameItemSameComponents(session.tool,player.getMainHandItem()))return false;
        var hit=aimedBlock(player);
        if(hit==null || !hit.getBlockPos().equals(session.source))return false;
        float rate=session.state.getDestroyProgress(player,session.level,session.source);
        if(!(rate<0))return false;
        // Native progress includes hardness, tool/efficiency, effects, water and airborne penalties.
        // Saturating credit handles even float overflow without a burst loop or an unbounded backlog.
        double elapsed=session.clock.elapsed(player,++session.ticks);
        double delta=Math.max(0,elapsed-session.elapsed);
        session.elapsed=elapsed;
        session.progress=Math.min(1,session.progress+Math.min(1,-(double)rate*delta));
        if(session.progress<1)return true;
        session.progress=0;
        var target=session.source.relative(hit.getDirection());
        if(!resident(session.level,target) || !session.level.getWorldBorder().isWithinBounds(target)
                || !session.level.mayInteract(player,target)
                || session.level.getServer().isUnderSpawnProtection(session.level,target,player)
                || player.blockActionRestricted(session.level,target,player.gameMode.getGameModeForPlayer())
                || !session.level.getBlockState(target).isAir())return true;
        // Snapshot/restore makes an independent BE, including inventory/components, as explicitly requested.
        // The target is air: neither an existing container nor the source can be cleared or spilled here.
        var snapshot=game.maps().snapshot(session.level,session.source);
        if(snapshot.entity()!=null) {
            snapshot.entity().putInt("x",target.getX());snapshot.entity().putInt("y",target.getY());snapshot.entity().putInt("z",target.getZ());
        }
        game.items().indirectKills().trapChanges(session.level,player,true,()->
                game.maps().restoreSnapshot(session.level,target,snapshot,818));
        return true;
    }
}
