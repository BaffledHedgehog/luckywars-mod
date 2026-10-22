package me.skywars2.game;

import me.skywars2.mixin.BeeDropMixin;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Native bee rewards, including their native hive/item serialization lifecycle. */
public final class LbcBeeDrop {
    private static final String ACTOR = "skywars2.bee_drop";
    private static final String OWNER = "skywars2.bee_owner.";
    private static final int[][] OFFSETS = {{0,1,1},{0,1,-1},{1,1,0},{-1,1,0},{0,2,0},{0,2,0},{0,2,0}};
    private static final CompoundTag PRESET = preset();
    private final GameController game;
    private final Set<Bee> tracked = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<BeehiveBlockEntity> hives = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<UUID,ServerPlayer> authors = new HashMap<>();

    public LbcBeeDrop(GameController game) { this.game = game; }

    private static CompoundTag preset() {
        try { return TagParser.parseCompoundFully("{DeathLootTable:\"lbc:honey\",HasNectar:1b}"); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) { throw new ExceptionInInitializerError(exception); }
    }

    public boolean ready(ItemEntity token) {
        if (token.isRemoved() || token.getAge() >= 5999 || !LbcBeeDropItem.matches(token.getItem())
                || !game.playing() || game.stage() == GameController.Stage.ENDING
                || token.level() != game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level)) return false;
        for (var tag : token.entityTags()) {
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        }
        var center = BlockPos.containing(token.position());
        // Native neighbour-shape updates and the seven offset summons need resident neighbours.
        for (int x = (center.getX()-2)>>4; x <= (center.getX()+2)>>4; x++) {
            for (int z = (center.getZ()-2)>>4; z <= (center.getZ()+2)>>4; z++) {
                if (level.getChunkSource().getChunkNow(x,z) == null) return false;
            }
        }
        return true;
    }

    public boolean spawn(ItemEntity token) {
        if (!ready(token)) return false;
        var level = (ServerLevel)token.level();
        var origin = token.position();
        var owner = game.items().specialDrops().owner(token);
        var author = LbcIndirectKills.playerSource(owner);
        if (author != null) authors.put(author.getUUID(),author);
        game.items().indirectKills().worldChanges(level, owner, () -> {
            setBlock(level, BlockPos.containing(origin), Blocks.OAK_FENCE.defaultBlockState(), false);
            setBlock(level, BlockPos.containing(origin.add(0,1,0)),
                    Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL,5), true);
        });
        for (var offset : OFFSETS) {
            var pos = origin.add(offset[0],offset[1],offset[2]);
            if (!Level.isInSpawnableBounds(BlockPos.containing(pos))) continue;
            var bee = EntityTypes.BEE.create(level, EntitySpawnReason.COMMAND);
            if (bee == null) continue;
            bee.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), PRESET.copy()));
            bee.snapTo(pos.x,pos.y,pos.z,bee.getYRot(),bee.getXRot());
            bee.entityTags().add(ACTOR);
            if (author != null) bee.entityTags().add(OWNER+author.getUUID());
            game.items().markCurrentItemEpoch(bee);
            tracked.add(bee);
            game.items().timeStop().prepareFrozen(bee);
            if (!level.addFreshEntity(bee)) { tracked.remove(bee); bee.discard(); }
        }
        var nearest = game.items().cageDrops().target(token);
        var announcement = Component.empty();
        if (nearest != null) announcement.append(nearest.getDisplayName().copy().withStyle(ChatFormatting.GOLD));
        announcement.append(Component.translatable("now_beekeeper").withStyle(ChatFormatting.YELLOW));
        game.server().getPlayerList().broadcastSystemMessage(announcement,false);
        return true;
    }

    private static void setBlock(ServerLevel level, BlockPos pos, BlockState state, boolean destroy) {
        if (level.isDebug() || level.isOutsideBuildHeight(pos)) return;
        if (destroy) level.destroyBlock(pos,true);
        var old = level.getBlockState(pos);
        if (new BlockInput(state,Set.of(),null).place(level,pos,258)) level.updateNeighboursOnBlockSet(pos,old);
    }

    public static boolean managed(Entity actor) { return actor instanceof Bee && actor.entityTags().contains(ACTOR); }

    public LivingEntity owner(Entity actor) {
        if (actor instanceof ItemEntity token && LbcBeeDropItem.matches(token.getItem()))
            return game.items().specialDrops().owner(token);
        if (!managed(actor)) return null;
        for (var tag : actor.entityTags()) {
            if (!tag.startsWith(OWNER)) continue;
            try {
                var id = UUID.fromString(tag.substring(OWNER.length()));
                var online = game.server().getPlayerList().getPlayer(id);
                return online == null ? authors.get(id) : online;
            }
            catch (IllegalArgumentException ignored) { }
        }
        return null;
    }

    public boolean canAffect(Entity source, Entity target) {
        if (!managed(source)) return true;
        if (!active(source) || target == null || !game.items().wands().validTarget(target)) return false;
        return !(owner(source) instanceof ServerPlayer player)
                || target == player || game.items().wands().target(player,target);
    }

    public boolean canTarget(Entity source, LivingEntity target) { return target == null || canAffect(source,target); }

    private boolean active(Entity actor) {
        return game.playing() && game.stage() != GameController.Stage.ENDING
                && actor.level() == game.maps().level() && game.items().currentEpoch(actor);
    }

    public void loaded(Entity actor) {
        if (!managed(actor)) return;
        if (!active(actor)) { actor.discard(); return; }
        tracked.add((Bee)actor);
        game.items().timeStop().prepareFrozen(actor);
    }

    public void unloaded(Entity actor) { if (managed(actor)) tracked.remove(actor); }

    /** Hive NBT and the BEES item component preserve both custom loot and these entity tags. */
    public boolean acceptStored(BeehiveBlockEntity.Occupant occupant) {
        var data = occupant.entityData().getUnsafe();
        if (!managedData(data)) return true;
        if (!game.playing() || game.stage() == GameController.Stage.ENDING) return false;
        for (var tag : tags(data)) if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        return tags(data).stream().anyMatch(game.items()::currentEpoch);
    }

    private static List<String> tags(CompoundTag data) {
        var tags = data.getListOrEmpty("Tags");
        return java.util.stream.IntStream.range(0,tags.size()).mapToObj(i -> tags.getStringOr(i,"")).toList();
    }

    private static boolean managedData(CompoundTag data) { return tags(data).contains(ACTOR); }

    public void stored(BeehiveBlockEntity hive) {
        if (((BeeDropMixin.HiveAccess)hive).skywars2$getBees().stream().anyMatch(o -> managedData(o.entityData().getUnsafe())))
            hives.add(hive);
    }

    private void prune(BeehiveBlockEntity hive, boolean all) {
        var access = (BeeDropMixin.HiveAccess)hive;
        var occupants = access.skywars2$getBees();
        var stored = access.skywars2$stored();
        boolean changed = false;
        for (int i=occupants.size()-1;i>=0;i--) {
            var occupant = occupants.get(i);
            if (managedData(occupant.entityData().getUnsafe()) && (all || !acceptStored(occupant))) {
                stored.remove(i);
                changed = true;
            }
        }
        // Mark persistence dirty directly; hive.setChanged may emergency-release bees near fire.
        if (changed && hive.getLevel() instanceof ServerLevel level) {
            var pos = hive.getBlockPos();
            var chunk = level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
            if (chunk != null) chunk.markUnsaved();
        }
    }

    public void tick() {
        for (var bee : List.copyOf(tracked)) {
            if (bee.isRemoved()) tracked.remove(bee);
            else if (!active(bee)) { tracked.remove(bee); bee.discard(); }
        }
        for (var hive : List.copyOf(hives)) {
            if (hive.isRemoved()) hives.remove(hive);
            else { prune(hive,false); if (hive.isEmpty()) hives.remove(hive); }
        }
    }

    public void stop() {
        var bees = List.copyOf(tracked);
        tracked.clear();
        for (var bee : bees) if (!bee.isRemoved()) bee.discard();
        var occupied = List.copyOf(hives);
        hives.clear();
        for (var hive : occupied) if (!hive.isRemoved()) prune(hive,true);
        authors.clear();
    }

    public int trackedCount() { return tracked.size(); }
    public List<Bee> snapshot() { return List.copyOf(tracked); }
    public List<BeehiveBlockEntity> snapshotHives() { return List.copyOf(hives); }
    public int storedCount() {
        int count = 0;
        for (var hive : hives) for (var occupant : ((BeeDropMixin.HiveAccess)hive).skywars2$getBees())
            if (managedData(occupant.entityData().getUnsafe())) count++;
        return count;
    }
}
