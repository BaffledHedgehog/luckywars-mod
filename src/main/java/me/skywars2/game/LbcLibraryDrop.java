package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Exact ordered Library commands; shared dispatch owns source ordering and the token debit. */
public final class LbcLibraryDrop {
    private static final String REWARD = "skywars2.library_reward";
    private static final CompoundTag LAPIS = preset("{Item:{id:\"minecraft:lapis_lazuli\",count:32}}");
    private static final CompoundTag XP = preset("{Value:1395}");
    private final GameController game;
    private final Set<Entity> tracked = Collections.newSetFromMap(new IdentityHashMap<>());

    public LbcLibraryDrop(GameController game) { this.game = game; }

    private static CompoundTag preset(String snbt) {
        try { return TagParser.parseCompoundFully(snbt); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    public boolean ready(ItemEntity token) {
        if (token.isRemoved() || token.getAge() >= 5999 || !LbcLibraryDropItem.matches(token.getItem())
                || !game.playing() || game.stage() == GameController.Stage.ENDING
                || token.level() != game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level)) return false;
        for (var tag : token.entityTags()) {
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        }
        var target = game.items().cageDrops().target(token);
        // Legacy execute still consumes the token if its selector has no eligible player.
        if (target == null) return true;
        var center = BlockPos.containing(target.position());
        // One neighbouring block around the authored footprint covers native placement reads.
        for (int x = (center.getX() - 2) >> 4; x <= (center.getX() + 4) >> 4; x++) {
            for (int z = (center.getZ() - 3) >> 4; z <= (center.getZ() + 3) >> 4; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
            }
        }
        return true;
    }

    public boolean spawn(ItemEntity token) {
        if (!ready(token)) return false;
        var target = game.items().cageDrops().target(token);
        if (target == null) return true;
        var level = (ServerLevel) token.level();
        var origin = target.position();
        game.items().indirectKills().worldChanges(level, game.items().specialDrops().owner(token), true,
                () -> geometry(level, origin));
        var rewardPos = origin.add(1, 1, 0);
        if (Level.isInSpawnableBounds(BlockPos.containing(rewardPos))) {
            insert(level, EntityTypes.ITEM.create(level, EntitySpawnReason.COMMAND), LAPIS, rewardPos);
            insert(level, EntityTypes.EXPERIENCE_ORB.create(level, EntitySpawnReason.COMMAND), XP, rewardPos);
            insert(level, EntityTypes.EXPERIENCE_BOTTLE.create(level, EntitySpawnReason.COMMAND), new CompoundTag(), rewardPos);
        }
        return true;
    }

    private static BlockPos at(Vec3 origin, int x, int y, int z) {
        return BlockPos.containing(origin.add(x, y, z));
    }

    private static void geometry(ServerLevel level, Vec3 p) {
        if (level.isDebug()) return;
        LbcDropBlocks.fill(level, at(p,0,0,2), at(p,2,2,2), Blocks.BOOKSHELF, true);
        LbcDropBlocks.fill(level, at(p,0,0,-2), at(p,2,2,-2), Blocks.BOOKSHELF, true);
        LbcDropBlocks.fill(level, at(p,3,0,1), at(p,3,2,-1), Blocks.BOOKSHELF, true);
        LbcDropBlocks.fill(level, at(p,-1,0,1), at(p,-1,2,-1), Blocks.BOOKSHELF, true);
        LbcDropBlocks.fill(level, at(p,3,-1,2), at(p,-1,-1,-2), Blocks.OAK_PLANKS, true);
        LbcDropBlocks.setBlock(level, at(p,1,0,0), Blocks.ENCHANTING_TABLE.defaultBlockState(), true);
        LbcDropBlocks.fill(level, at(p,3,3,2), at(p,-1,3,-2), Blocks.OAK_WOOD, true);
        LbcDropBlocks.setBlock(level, at(p,1,2,0), Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.DOWN), true);
        LbcDropBlocks.fill(level, at(p,3,0,2), at(p,3,2,2), Blocks.OAK_WOOD, true);
        LbcDropBlocks.fill(level, at(p,-1,0,2), at(p,-1,2,2), Blocks.OAK_WOOD, true);
        LbcDropBlocks.fill(level, at(p,3,0,-2), at(p,3,2,-2), Blocks.OAK_WOOD, true);
        LbcDropBlocks.fill(level, at(p,-1,0,-2), at(p,-1,2,-2), Blocks.OAK_WOOD, true);
        LbcDropBlocks.fill(level, at(p,3,0,0), at(p,3,1,0), Blocks.AIR, true);
        LbcDropBlocks.fill(level, at(p,1,0,2), at(p,1,1,2), Blocks.AIR, true);
        LbcDropBlocks.fill(level, at(p,-1,0,0), at(p,-1,1,0), Blocks.AIR, true);
        LbcDropBlocks.fill(level, at(p,1,0,-2), at(p,1,1,-2), Blocks.AIR, true);
    }

    private void insert(ServerLevel level, Entity actor, CompoundTag preset, Vec3 pos) {
        if (actor == null) return;
        // Loading the source NBT reproduces summon defaults, including zero motion and rotation.
        // Value is natively converted to short1395; award() would split it into several orbs.
        actor.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), preset.copy()));
        actor.snapTo(pos.x, pos.y, pos.z, actor.getYRot(), actor.getXRot());
        actor.entityTags().add(REWARD);
        game.items().markCurrentItemEpoch(actor);
        tracked.add(actor);
        game.items().timeStop().prepareFrozen(actor);
        if (!level.addFreshEntity(actor)) {
            tracked.remove(actor);
            actor.discard();
        }
    }

    public static boolean managed(Entity actor) {
        return (actor instanceof ItemEntity || actor instanceof ExperienceOrb || actor instanceof ThrownExperienceBottle)
                && actor.entityTags().contains(REWARD);
    }

    private boolean active(Entity actor) {
        return game.playing() && game.stage() != GameController.Stage.ENDING
                && actor.level() == game.maps().level() && game.items().currentEpoch(actor);
    }

    public void loaded(Entity actor) {
        if (!managed(actor)) return;
        if (!active(actor)) { actor.discard(); return; }
        tracked.add(actor);
        game.items().timeStop().prepareFrozen(actor);
    }

    public void unloaded(Entity actor) {
        if (!managed(actor)) return;
        tracked.remove(actor);
        // Persisted reward/epoch tags admit same-match reloads and reject stale rewards.
    }

    public void tick() {
        for (var actor : List.copyOf(tracked)) {
            if (actor.isRemoved()) tracked.remove(actor);
            else if (!active(actor)) { tracked.remove(actor); actor.discard(); }
        }
    }

    public void stop() {
        var old = List.copyOf(tracked);
        tracked.clear();
        // Quiet discard never breaks bottles or grants XP during match cleanup.
        for (var actor : old) if (!actor.isRemoved()) actor.discard();
    }

    public int trackedCount() { return tracked.size(); }
    public List<Entity> snapshot() { return List.copyOf(tracked); }
}
