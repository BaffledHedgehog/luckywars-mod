package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Geometry and native actors only; LbcSpecialDrops owns admission and one shared debit. */
public final class LbcCageDrops {
    private static final String ACTOR = "skywars2.cage_drop";
    private static final CompoundTag ANVIL = preset("{BlockState:\"minecraft:anvil\",Time:1,DropItem:1b,HurtEntities:1b,FallHurtMax:35,FallHurtAmount:2.0f,Motion:[0.0d,0.0d,0.0d]}");
    private static final CompoundTag EMPTY_ANVIL = preset("{BlockState:\"minecraft:anvil\",Time:1,DropItem:1b,HurtEntities:1b,FallHurtMax:1,FallHurtAmount:1.0f,Motion:[0.0d,0.0d,0.0d]}");
    private static final CompoundTag FIREBALL = preset("{ExplosionPower:4,Motion:[0.0d,-0.05d,0.0d]}");
    private static final CompoundTag[] POTIONS = {potion(0), potion(1)};
    private final GameController game;
    private final Set<Entity> tracked = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Entity, LivingEntity> causes = new IdentityHashMap<>();
    private final Map<UUID, LivingEntity> unloadedCauses = new HashMap<>();

    public LbcCageDrops(GameController game) { this.game = game; }

    private static CompoundTag preset(String snbt) {
        try { return TagParser.parseCompoundFully(snbt); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) { throw new ExceptionInInitializerError(exception); }
    }

    private static CompoundTag potion(int amplifier) {
        return preset("{Item:{id:\"minecraft:splash_potion\",components:{\"minecraft:potion_contents\":{custom_color:7543569,custom_effects:[{id:\"minecraft:instant_damage\",amplifier:"
                + amplifier + "b,duration:20}]}},count:1}}");
    }

    /** Native nearest-player semantics: self/allies and Creative players remain eligible. */
    public ServerPlayer target(ItemEntity token) {
        if (!(token.level() instanceof ServerLevel level)) return null;
        ServerPlayer nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (var player : level.players()) {
            if (!game.items().wands().validTarget(player)) continue;
            double candidate = player.distanceToSqr(token);
            if (candidate < distance) { nearest = player; distance = candidate; }
        }
        return nearest;
    }

    public boolean ready(ItemEntity token, LbcCageDropItem.Kind kind, ServerPlayer target) {
        if (token.isRemoved() || token.getAge() >= 5999 || !LbcCageDropItem.matches(token.getItem(), kind)
                || !game.playing() || game.stage() == GameController.Stage.ENDING
                || token.level() != game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level)) return false;
        for (var tag : token.entityTags()) {
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        }
        // With no eligible player the legacy execute does nothing, but still consumes its token.
        if (target == null) return true;
        if (target.level() != level || !game.items().wands().validTarget(target)) return false;
        return resident(level,BlockPos.containing(target.position()));
    }

    private static boolean resident(ServerLevel level,BlockPos center){
        // Include one neighbour around the three-wide shape for native shape/block updates.
        for (int x = (center.getX() - 2) >> 4; x <= (center.getX() + 2) >> 4; x++) {
            for (int z = (center.getZ() - 2) >> 4; z <= (center.getZ() + 2) >> 4; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
            }
        }
        return true;
    }

    public boolean spawn(ItemEntity token, LbcCageDropItem.Kind kind, ServerPlayer target) {
        if (!ready(token, kind, target)) return false;
        if (target == null) return true;
        var level = (ServerLevel) token.level();
        var owner = game.items().specialDrops().owner(token);
        return spawnAt(level,target.position(),kind,owner,null,target,false,ANVIL);
    }

    /** Accepted source contexts choose their exact position; this method owns no token or debit. */
    public boolean spawn(ServerLevel level,Vec3 at,LbcCageDropItem.Kind kind,ServerPlayer causalAuthor,LbcIndirectKills.Owner capturedCause){
        return spawnSource(level,at,kind,causalAuthor,capturedCause,ANVIL);
    }

    /** Troll Box's empty_cage shares geometry but its two anvils have source damage1/max1. */
    public boolean spawnEmptyAnvilCage(ServerLevel level,Vec3 at,ServerPlayer causalAuthor,LbcIndirectKills.Owner capturedCause){
        return spawnSource(level,at,LbcCageDropItem.Kind.ANVIL,causalAuthor,capturedCause,EMPTY_ANVIL);
    }

    private boolean spawnSource(ServerLevel level,Vec3 at,LbcCageDropItem.Kind kind,ServerPlayer causalAuthor,
                                LbcIndirectKills.Owner capturedCause,CompoundTag anvilPreset){
        if(level==null||at==null||kind==null||level.getServer()!=game.server()||!game.server().isSameThread()
                ||!Double.isFinite(at.x)||!Double.isFinite(at.y)||!Double.isFinite(at.z)
                ||!game.playing()||game.stage()==GameController.Stage.ENDING
                ||!game.server().tickRateManager().runsNormally()||game.server().tickRateManager().isFrozen()
                ||game.items().timeStop().stopped(level)||!resident(level,BlockPos.containing(at)))return false;
        var inherited=game.items().spellSources().current();
        if(inherited!=null&&!game.items().currentEpoch(inherited.epoch()))return false;
        var cause=capturedCause!=null?capturedCause:inherited!=null?inherited.cause()
                :causalAuthor==null?null:game.items().indirectKills().wildCause(causalAuthor);
        // Troll Box's global selectors include non-map dimensions. Explicit accepted source
        // authority keeps these children alive there; ordinary token admission remains map-only.
        var source=inherited==null?new LbcSpellSource.Source(UUID.randomUUID(),cause!=null?cause.player():causalAuthor==null?null:causalAuthor.getUUID(),
                game.items().epoch(),cause,Set.of(),true)
                :new LbcSpellSource.Source(inherited.emitter(),inherited.author(),inherited.epoch(),cause,inherited.excluded(),true);
        try(var scope=game.items().spellSources().scope(source,level)){
            if(!LbcBedwarsRoleGuards.publication(level))return false;
            return spawnAt(level,at,kind,causalAuthor,cause,null,true,anvilPreset);
        }
    }

    private boolean spawnAt(ServerLevel level,Vec3 at,LbcCageDropItem.Kind kind,LivingEntity owner,
                            LbcIndirectKills.Owner capturedCause,ServerPlayer presentation,boolean sourceContext,CompoundTag anvilPreset){
        var center=BlockPos.containing(at);
        Runnable writes = () -> geometry(level, center, kind);
        if(capturedCause!=null)game.items().indirectKills().trapChanges(level,capturedCause,writes);
        else if (!sourceContext&&owner instanceof ServerPlayer player) game.items().indirectKills().trapChanges(level, player, true, writes);
        else writes.run();
        double x = center.getX() + .5, y = at.y, z = center.getZ() + .5;
        switch (kind) {
            case ANVIL -> {
                insert(level, EntityTypes.FALLING_BLOCK.create(level, EntitySpawnReason.COMMAND), anvilPreset, x, y + 40, z, owner);
                insert(level, EntityTypes.FALLING_BLOCK.create(level, EntitySpawnReason.COMMAND), anvilPreset, x, y + 42, z, owner);
            }
            case FIREBALL -> insert(level, EntityTypes.FIREBALL.create(level, EntitySpawnReason.COMMAND), FIREBALL, x, y + 70, z, owner);
            case POTIONS -> {
                for (int i = 1; i <= 40; i++) {
                    int amplifier = i >= 10 && i <= 20 || i >= 30 ? 1 : 0;
                    insert(level, EntityTypes.SPLASH_POTION.create(level, EntitySpawnReason.COMMAND), POTIONS[amplifier], x, y + i * 50, z, owner);
                }
            }
            default -> { }
        }
        if (presentation!=null&&kind != LbcCageDropItem.Kind.OBSIDIAN) {
            presentation.connection.send(new ClientboundSetTitleTextPacket(Component.literal(" ")));
            presentation.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("look_up")));
        }
        return true;
    }

    private static void geometry(ServerLevel level, BlockPos center, LbcCageDropItem.Kind kind) {
        if (level.isDebug()) return;
        if (kind == LbcCageDropItem.Kind.OBSIDIAN) {
            LbcDropBlocks.fill(level, center.offset(-1,-1,-1), center.offset(1,2,1), Blocks.OBSIDIAN, true);
            LbcDropBlocks.fill(level, center, center.above(), Blocks.WATER, false);
            LbcDropBlocks.fill(level, center.offset(1,1,0), center.offset(1,1,0), Blocks.GLASS, true);
            LbcDropBlocks.fill(level, center.offset(0,1,1), center.offset(0,1,1), Blocks.GLASS, true);
            LbcDropBlocks.fill(level, center.offset(-1,1,0), center.offset(-1,1,0), Blocks.GLASS, true);
            LbcDropBlocks.fill(level, center.offset(0,1,-1), center.offset(0,1,-1), Blocks.GLASS, true);
        } else {
            LbcDropBlocks.fill(level, center.offset(-1,-1,-1), center.offset(1,-1,1), Blocks.STONE_BRICKS, false);
            LbcDropBlocks.fill(level, center.offset(-1,0,-1), center.offset(1,3,1), Blocks.IRON_BARS, false);
            if (kind == LbcCageDropItem.Kind.LAVA) {
                LbcDropBlocks.fill(level, center, center.above(2), Blocks.AIR, false);
                LbcDropBlocks.fill(level, center.above(3), center.above(3), Blocks.LAVA, false);
            } else {
                // The shaft only opens upward; the old absolute255 pass could erase the floor.
                if (center.getY() <= level.getMaxY())
                    LbcDropBlocks.fill(level, center, new BlockPos(center.getX(),level.getMaxY(),center.getZ()), Blocks.AIR, false);
            }
        }
    }

    private void insert(ServerLevel level, Entity actor, CompoundTag preset, double x, double y, double z, LivingEntity owner) {
        if (actor == null) return;
        actor.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), preset.copy()));
        if(actor instanceof FallingBlockEntity falling&&(preset==ANVIL||preset==EMPTY_ANVIL))
            falling.setHurtsEntities(preset==EMPTY_ANVIL?1.0f:2.0f,preset==EMPTY_ANVIL?1:35);
        actor.setPos(x,y,z);
        if (actor instanceof Projectile projectile) projectile.setOwner(owner);
        actor.entityTags().add(ACTOR);
        game.items().markCurrentItemEpoch(actor);
        causes.put(actor,owner);
        tracked.add(actor);
        game.items().timeStop().prepareFrozen(actor);
        if (!level.addFreshEntity(actor)) { tracked.remove(actor); causes.remove(actor); }
    }

    public static boolean managed(Entity entity) {
        return entity != null && (entity instanceof FallingBlockEntity || entity instanceof Projectile || entity instanceof PrimedTnt)
                && entity.entityTags().contains(ACTOR);
    }

    public LivingEntity owner(Entity actor) {
        if (actor instanceof ItemEntity token && LbcCageDropItem.kind(token.getItem()) != null)
            return game.items().specialDrops().owner(token);
        if (!managed(actor)) return null;
        if (actor instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity current) return current;
        var reflected = game.items().electro().reflectedOwner(actor);
        if (reflected != null) return reflected;
        if (actor instanceof PrimedTnt tnt && tnt.getOwner() != null) return tnt.getOwner();
        var retained=causes.get(actor);
        return retained!=null?retained:game.items().spellSources().owner(actor);
    }

    /** Native TNT getOwner/save hooks use this non-recursive fallback. */
    public LivingEntity retainedOwner(Entity actor) {
        if (!managed(actor)) return null;
        var reflected = game.items().electro().reflectedOwner(actor);
        if(reflected!=null)return reflected;
        var retained=causes.get(actor);
        return retained!=null?retained:game.items().spellSources().owner(actor);
    }

    public void inheritChild(Entity parent, Entity child) {
        if (!managed(parent) || !active(parent) || !(child instanceof PrimedTnt)) return;
        child.entityTags().add(ACTOR);
        game.items().spellSources().inherit(parent,child);
        game.items().markCurrentItemEpoch(child);
        causes.put(child, owner(parent));
        tracked.add(child);
        game.items().timeStop().prepareFrozen(child);
    }

    public boolean wild(Entity source) {
        return managed(source) || source instanceof ItemEntity token && LbcCageDropItem.kind(token.getItem()) != null;
    }

    public boolean canAffect(Entity source, Entity target) {
        if (!managed(source)) return true;
        if (!active(source) || !game.items().wands().validTarget(target)) return false;
        return wild(source) || !(owner(source) instanceof ServerPlayer player)
                || target == player || game.items().wands().projectileTarget(player, target);
    }

    private boolean active(Entity actor) {
        return game.playing() && game.stage() != GameController.Stage.ENDING
                && (actor.level() == game.maps().level()||game.items().spellSources().persistent(actor)) && game.items().currentEpoch(actor);
    }

    public void loaded(Entity actor) {
        if (!managed(actor)) return;
        if (!active(actor)) { unloadedCauses.remove(actor.getUUID()); actor.discard(); return; }
        if (!causes.containsKey(actor)) causes.put(actor,unloadedCauses.remove(actor.getUUID()));
        tracked.add(actor);
        game.items().timeStop().prepareFrozen(actor);
    }

    public void unloaded(Entity actor) {
        if (!managed(actor)) return;
        tracked.remove(actor);
        if (active(actor) && actor.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK)
            unloadedCauses.put(actor.getUUID(), owner(actor));
        // Retain the instance cause through synchronous native explosion/landing callbacks.
    }

    public void tick() {
        for (var actor : List.copyOf(tracked)) {
            if (actor.isRemoved()) tracked.remove(actor);
            else if (!active(actor)) { tracked.remove(actor); actor.discard(); }
        }
        causes.keySet().removeIf(actor -> actor.isRemoved() || !active(actor) || !tracked.contains(actor));
        if (!game.playing() || game.stage() == GameController.Stage.ENDING) unloadedCauses.clear();
    }

    public void stop() {
        unloadedCauses.clear();
        var detached = List.copyOf(tracked);
        tracked.clear();
        for (var actor : detached) if (!actor.isRemoved()) actor.discard();
        causes.clear();
    }

    public int trackedCount() { return tracked.size(); }
    public List<Entity> snapshot() { return List.copyOf(tracked); }
}
