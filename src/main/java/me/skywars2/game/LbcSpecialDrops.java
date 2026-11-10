package me.skywars2.game;

import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared dropped-token dispatcher: source order, one activation/debit per tick and epoch. */
public final class LbcSpecialDrops {
    private final GameController game;
    private static final String OWNER = "skywars2.special_drop_owner.";
    private record Pending(String epoch, LivingEntity owner) { }
    private final Map<ItemEntity, Pending> pending = new IdentityHashMap<>();
    private final Map<java.util.UUID, LivingEntity> authors = new java.util.HashMap<>();
    private final Map<ItemEntity, Integer> lastAttempt = new IdentityHashMap<>();
    private int lastTick = Integer.MIN_VALUE;

    public LbcSpecialDrops(GameController game) { this.game = game; }

    private static boolean recognized(net.minecraft.world.item.ItemStack stack) {
        return LbcTmtDrops.matches(stack)||!externalDrop(stack)&&nativeRecognized(stack);
    }
    /** Ordinary tokens retain their existing acquisition queues; only TMT composes these owners here. */
    private static boolean externalDrop(net.minecraft.world.item.ItemStack stack){
        return LbcAcademy.recognizedDrop(stack)||LbcHelicopterEncounter.isTrigger(stack)||LbcCrocodildo.trigger(stack);
    }
    static boolean nativeRecognized(net.minecraft.world.item.ItemStack stack) {
        if(LbcTmtItems.token(stack))return true;
        return LbcAcademy.recognizedDrop(stack)||LbcHelicopterEncounter.isTrigger(stack)||LbcCrocodildo.trigger(stack)||LbcBalance.isDrop(stack) || LbcEternalWealthItems.isDrop(stack) || LbcLuckyBlocks.silkToken(stack) || LbcFamilyDrops.matches(stack) || LbcGrimoireDungeon.matches(stack) || LbcMagicBean.recognized(stack) || LbcInfinity.recognized(stack) || LbcChaosTrapActors.matches(stack) || LbcSauvojenBoss.recognized(stack) || LbcChestDropItem.kind(stack) != null || LbcTntDropItem.matches(stack) || LbcCreeperDropItem.kind(stack) != null || LbcCageDropItem.kind(stack) != null || LbcHoleDropItem.kind(stack) != null || LbcCorrosionDropItem.kind(stack) != null || LbcZombieTntDropItem.matches(stack) || LbcFallingNetheriteDropItem.kind(stack) != null || LbcHomeItems.matchesDrop(stack) || LbcScareDropItem.kind(stack) != null || LbcLuckyPigDropItem.matches(stack) || LbcLevitationDropItem.matches(stack) || LbcLibraryDropItem.matches(stack) || LbcBeeDropItem.matches(stack) || LbcOpAnvilDropItem.kind(stack) != null || LbcSlabDropItem.kind(stack) != null || LbcFeedbackDropItem.matches(stack) || LbcCreatureDropItem.matches(stack) || LbcPotionSpawnerDropItem.matches(stack) || LbcIslandDropItem.matches(stack) || LbcDroneDropItem.matches(stack) || LbcAntDropItem.matches(stack) || LbcPortalDropItem.matches(stack) || LbcLuckyRoomDropItem.matches(stack) || LbcPisaDropItem.matches(stack) || LbcAntigravityDropItem.matches(stack) || LbcUltraponosDropItem.matches(stack) || LbcNpcDropItems.isTransformator(stack) || LbcStalinDropItem.isTrigger(stack);
    }

    public void loaded(Entity entity) {
        // /give creates a temporary visual item and marks it fake after ENTITY_LOAD.
        // Do not place blocks from inside the entity insertion callback.
        if (entity instanceof ItemEntity token && recognized(token.getItem()) && active(token) && !stale(token)) {
            // Persist admission on the entity too: unloaded tokens cannot join a later match as fresh drops.
            game.items().markCurrentItemEpoch(token);
            var cause = owner(token);
            if (cause != null) {
                authors.put(cause.getUUID(), cause);
                token.entityTags().removeIf(tag -> tag.startsWith(OWNER));
                token.entityTags().add(OWNER + cause.getUUID());
            }
            pending.putIfAbsent(token, new Pending(game.items().epoch(), cause));
        }
    }

    /** Actual thrower, retained at admission independently of the pickup target. */
    public LivingEntity owner(ItemEntity token) {
        if (token.getOwner() instanceof LivingEntity living) return living;
        var entry = pending.get(token);
        if (entry != null && entry.owner() != null) return entry.owner();
        for (var tag : token.entityTags()) if (tag.startsWith(OWNER)) try {
            var id = java.util.UUID.fromString(tag.substring(OWNER.length()));
            var online = game.server().getPlayerList().getPlayer(id);
            if (online != null) return online;
            var cause = authors.get(id);
            if (cause != null) return cause;
        } catch (IllegalArgumentException ignored) { }
        return null;
    }

    /** Only causal trap tokens reach this native merge guard; no world or tick scan. */
    public boolean canMerge(ItemEntity first, ItemEntity second) {
        var sourceA=game.items().spellSources().source(first);var sourceB=game.items().spellSources().source(second);
        if(sourceA!=null||sourceB!=null)return sourceA!=null&&sourceB!=null&&sourceA.epoch().equals(sourceB.epoch())&&java.util.Objects.equals(sourceA.cause(),sourceB.cause());
        var a = owner(first);
        var b = owner(second);
        return java.util.Objects.equals(a == null ? null : a.getUUID(), b == null ? null : b.getUUID());
    }

    public void unloaded(Entity entity) {
        pending.remove(entity);
        lastAttempt.remove(entity);
    }

    private boolean stale(ItemEntity token) {
        for (var tag : token.entityTags())
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return true;
        return false;
    }

    public LbcIndirectKills.Owner cause(ItemEntity token){
        var source=game.items().spellSources().source(token);if(source!=null)return source.cause();
        var author=LbcIndirectKills.playerSource(owner(token));return author==null?null:game.items().indirectKills().wildCause(author);
    }
    private boolean active(ItemEntity token) {
        return game.playing() && game.stage() != GameController.Stage.ENDING && (token.level() == game.maps().level()||game.items().spellSources().persistent(token));
    }

    public void tick() {
        int now = game.server().getTickCount();
        if (lastTick == now) return;
        lastTick = now;
        for (var token : List.copyOf(pending.keySet())) {
            if (token.isRemoved() || token.getAge() >= 5999 || stale(token) || !active(token)
                    || !game.items().currentEpoch(pending.get(token).epoch()) || !recognized(token.getItem())) {
                unloaded(token);
                continue;
            }
            spawn(token);
            if (token.isRemoved() || token.getItem().isEmpty()) unloaded(token);
        }
    }

    /** Returns whether an active loaded attempt was dispatched, even when native placement fails. */
    boolean spawn(ItemEntity token) {
        try(var scope=game.items().spellSources().lootScope(token)){
            if(LbcTmtDrops.matches(token.getItem())&&!LbcTmtAbilities.active(token.getItem()))return spawnTmt(token);
            return spawnScoped(token,true,true,true);
        }
    }
    private boolean spawnTmt(ItemEntity token){
        int now=game.server().getTickCount();if(lastAttempt.getOrDefault(token,Integer.MIN_VALUE)==now)return false;
        var stack=token.getItem();var admitted=new java.util.ArrayList<LbcTmtAbilities.Ability>();
        for(var property:LbcTmtDrops.properties(stack))if(LbcTmtAbilities.invoke(stack,property.id(),()->spawnScoped(token,false,false,false)))admitted.add(property);
        if(admitted.isEmpty())return false;
        lastAttempt.put(token,now);boolean dispatched=false;
        for(var property:admitted){if(token.isRemoved()||token.getItem()!=stack||stack.isEmpty())break;
            dispatched|=LbcTmtAbilities.invoke(stack,property.id(),()->spawnScoped(token,true,false,false));
        }
        if(dispatched&&!token.isRemoved()&&!token.getItem().isEmpty())consume(token);
        return dispatched;
    }
    private boolean spawnScoped(ItemEntity token,boolean perform,boolean debit,boolean stamp) {
        if (token.isRemoved() || token.getAge() >= 5999 || stale(token) || !active(token)
                || !(token.level() instanceof ServerLevel level) || game.items().timeStop().stopped(level)) return false;
        if (pending.containsKey(token) && !game.items().currentEpoch(pending.get(token).epoch())) return false;
        if(LbcTmtItems.token(token.getItem())){
            int now=game.server().getTickCount();if(lastAttempt.getOrDefault(token,Integer.MIN_VALUE)==now||!resident(level,token.blockPosition(),0))return false;
            if(!perform)return true;
            lastAttempt.put(token,now);if(!game.items().tmt().acquire(token))return false;
            if(debit)consume(token);return true;
        }
        boolean academy=LbcAcademy.recognizedDrop(token.getItem());
        boolean helicopter=LbcHelicopterEncounter.isTrigger(token.getItem());
        boolean crocodildo=LbcCrocodildo.trigger(token.getItem());
        boolean wealth=LbcEternalWealthItems.isDrop(token.getItem());
        boolean silkToken=LbcLuckyBlocks.silkToken(token.getItem());
        boolean family=LbcFamilyDrops.matches(token.getItem());
        if(family&&!game.items().familyDrops().ready(token))return false;
        boolean balanceDrop=LbcBalance.isDrop(token.getItem());
        boolean chaosActors = LbcChaosTrapActors.matches(token.getItem());
        boolean sauvojenBoss = LbcSauvojenBoss.recognized(token.getItem());
        boolean infinity = LbcInfinity.recognized(token.getItem());
        boolean magicBean = LbcMagicBean.recognized(token.getItem());
        boolean chest = LbcChestDropItem.kind(token.getItem()) != null;
        boolean tnt = LbcTntDropItem.matches(token.getItem());
        boolean creeper = LbcCreeperDropItem.kind(token.getItem()) != null;
        boolean cage = LbcCageDropItem.kind(token.getItem()) != null;
        boolean hole = LbcHoleDropItem.kind(token.getItem()) != null;
        boolean corrosion = LbcCorrosionDropItem.kind(token.getItem()) != null;
        boolean zombieTnt = LbcZombieTntDropItem.matches(token.getItem());
        boolean falling = LbcFallingNetheriteDropItem.kind(token.getItem()) != null;
        boolean home = LbcHomeItems.matchesDrop(token.getItem());
        boolean scare = LbcScareDropItem.kind(token.getItem()) != null;
        boolean library = LbcLibraryDropItem.matches(token.getItem());
        boolean opAnvil = LbcOpAnvilDropItem.kind(token.getItem()) != null;
        boolean slab = LbcSlabDropItem.kind(token.getItem()) != null;
        boolean feedback = LbcFeedbackDropItem.matches(token.getItem());
        boolean vex = LbcCreatureDropItem.matches(token.getItem(),LbcCreatureDropItem.Kind.OLD_VEX);
        boolean normalVex = LbcCreatureDropItem.matches(token.getItem(),LbcCreatureDropItem.Kind.VEX);
        boolean fatSkeleton = LbcCreatureDropItem.matches(token.getItem(),LbcCreatureDropItem.Kind.FAT_SKELETON);
        boolean skeleton = LbcCreatureDropItem.matches(token.getItem(),LbcCreatureDropItem.Kind.OLD_SKELETON);
        boolean bullets = LbcCreatureDropItem.matches(token.getItem(),LbcCreatureDropItem.Kind.BULLETS);
        boolean potionSpawner = LbcPotionSpawnerDropItem.matches(token.getItem());
        boolean island = LbcIslandDropItem.matches(token.getItem());
        boolean drone = LbcDroneDropItem.matches(token.getItem());
        boolean portal = LbcPortalDropItem.matches(token.getItem());
        boolean luckyRoom = LbcLuckyRoomDropItem.matches(token.getItem());
        boolean pisa = LbcPisaDropItem.matches(token.getItem());
        boolean antigravity = LbcAntigravityDropItem.matches(token.getItem());
        boolean grimoireDungeon=LbcGrimoireDungeon.matches(token.getItem());
        boolean ultraponos = LbcUltraponosDropItem.matches(token.getItem());
        boolean transformator = LbcNpcDropItems.isTransformator(token.getItem());
        boolean stalin = LbcStalinDropItem.isTrigger(token.getItem());
        boolean ants = LbcAntDropItem.matches(token.getItem());
        boolean bob = LbcCreatureDropItem.matches(token.getItem(),LbcCreatureDropItem.Kind.OLD_BOB);
        boolean bees = LbcBeeDropItem.matches(token.getItem());
        boolean pigs = LbcLuckyPigDropItem.matches(token.getItem());
        boolean enzan = LbcLevitationDropItem.matches(token.getItem());
        if (!academy && !helicopter && !crocodildo && !balanceDrop && !wealth && !silkToken && !family && !grimoireDungeon && !magicBean && !infinity && !chaosActors && !sauvojenBoss && !chest && !tnt && !creeper && !cage && !hole && !corrosion && !zombieTnt && !falling && !home && !scare && !pigs && !enzan && !library && !bees && !opAnvil && !slab && !feedback && !vex && !normalVex && !fatSkeleton && !skeleton && !bullets && !potionSpawner && !island && !drone && !bob && !ants && !portal && !luckyRoom && !pisa && !antigravity && !ultraponos && !transformator && !stalin) return false;
        if(academy&&!game.items().academy().readyDrop(token))return false;
        if(helicopter&&!game.items().helicopterEncounter().ready(token))return false;
        if(crocodildo&&!game.items().crocodildo().ready(token))return false;
        if(wealth&&!game.items().eternalWealth().ready(token))return false;
        if (chaosActors && !game.items().chaosTrapActors().ready(token)) return false;
        if (sauvojenBoss && !game.items().sauvojenBoss().ready(token)) return false;
        if (infinity && !game.items().infinity().ready(token)) return false;
        if (magicBean && !game.items().magicBean().ready(token)) return false;
        int now = game.server().getTickCount();
        if (stamp&&lastAttempt.getOrDefault(token, Integer.MIN_VALUE) == now) return false;
        var pos = BlockPos.containing(token.position());
        if (!resident(level, pos, chest ? 1 : 0)) return false;
        for (var kind : LbcOpAnvilDropItem.Kind.values())
            if (LbcOpAnvilDropItem.matches(token.getItem(), kind) && !game.items().opAnvilDrops().ready(token, kind)) return false;
        for (var kind : LbcSlabDropItem.Kind.values())
            if (LbcSlabDropItem.matches(token.getItem(), kind) && !game.items().slabDrops().ready(token, kind)) return false;
        for (var kind : LbcFeedbackDropItem.Kind.values())
            if (LbcFeedbackDropItem.matches(token.getItem(), kind) && !game.items().feedbackDrops().ready(token, kind)) return false;
        for(var kind:LbcPotionSpawnerDropItem.Kind.values())
            if(LbcPotionSpawnerDropItem.matches(token.getItem(),kind) && !game.items().potionSpawnerDrops().ready(token,kind)) return false;
        if(vex && !game.items().creatureDrops().ready(token,LbcCreatureDrops.Kind.VEX_OLD)) return false;
        if(normalVex && !game.items().creatureDrops().ready(token,LbcCreatureDrops.Kind.VEX)) return false;
        if(fatSkeleton && !game.items().creatureDrops().ready(token,LbcCreatureDrops.Kind.FAT_SKELETON)) return false;
        if(skeleton && !game.items().creatureDrops().ready(token,LbcCreatureDrops.Kind.SKELETON_OLD)) return false;
        if(bullets && !game.items().creatureDrops().ready(token,LbcCreatureDrops.Kind.BULLETS)) return false;
        if (bees && !game.items().beeDrops().ready(token)) return false;
        if (library && !game.items().libraryDrop().ready(token)) return false;
        if (island && !game.items().islandDrops().ready(token)) return false;
        if (drone && !game.items().droneDrops().ready(token)) return false;
        if (portal && !game.items().portalDrops().ready(token)) return false;
        if (luckyRoom && !game.items().luckyRoomDrops().ready(token)) return false;
        if (pisa && !game.items().pisaDrops().ready(token)) return false;
        if (antigravity && !game.items().antigravity().ready(token)) return false;
        if(grimoireDungeon&&!game.items().grimoireDungeon().ready(token))return false;
        if (ultraponos && !game.items().ultraponos().ready(token)) return false;
        if (transformator && !game.items().transformator().ready(token)) return false;
        if (stalin && !game.items().stalin().ready(token)) return false;
        for (var kind : LbcAntDropItem.Kind.values())
            if (LbcAntDropItem.matches(token.getItem(),kind) && !game.items().antDrops().ready(token,kind)) return false;
        if (bob && !game.items().creatureDrops().ready(token,LbcCreatureDrops.Kind.BOB_OLD)) return false;
        if (pigs && !game.items().luckyPigs().ready(token)) return false;
        for (var kind : LbcCreeperDropItem.Kind.values()) {
            if (LbcCreeperDropItem.matches(token.getItem(), kind) && !game.items().creeperDrops().ready(token, kind)) return false;
        }
        for (var kind : LbcCorrosionDropItem.Kind.values()) {
            if (LbcCorrosionDropItem.matches(token.getItem(), kind) && !game.items().corrosionDrops().ready(token, kind)) return false;
        }
        if (zombieTnt && !game.items().zombieTntDrops().ready(token)) return false;
        if (home && !game.items().homes().ready(token)) return false;
        for (var kind : LbcFallingNetheriteDropItem.Kind.values()) {
            if (LbcFallingNetheriteDropItem.matches(token.getItem(), kind) && !game.items().fallingNetheriteDrops().ready(token, kind)) return false;
        }
        var target = cage || hole ? game.items().cageDrops().target(token) : null;
        if (target != null) for (var kind : LbcCageDropItem.Kind.values()) {
            if (LbcCageDropItem.matches(token.getItem(), kind) && !game.items().cageDrops().ready(token, kind, target)) return false;
        }
        if (target != null) for (var kind : LbcHoleDropItem.Kind.values()) {
            if (LbcHoleDropItem.matches(token.getItem(), kind) && !game.items().holeDrops().ready(token, kind, target)) return false;
        }
        if(balanceDrop&&!game.items().balance().canDrop(token))return false;
        if(!perform)return true;
        if(balanceDrop&&!game.items().balance().drop(token))return false;
        if(stamp)lastAttempt.put(token, now);
        // The source dispatcher executes every matching flag in order, then debits once.
        for (var kind : LbcCreeperDropItem.Kind.values()) {
            if (LbcCreeperDropItem.matches(token.getItem(), kind)) game.items().creeperDrops().spawn(token, kind);
        }
        if (!level.isOutsideBuildHeight(pos) && !level.isDebug()) {
            for (var kind : LbcChestDropItem.Kind.values()) if (LbcChestDropItem.matches(token.getItem(), kind)) {
                place(level, pos, kind);
            }
        }
        if (tnt) game.items().tntDrops().spawn(token);
        for (var kind : LbcCorrosionDropItem.Kind.values()) {
            if (LbcCorrosionDropItem.matches(token.getItem(), kind)) game.items().corrosionDrops().spawn(token, kind);
        }
        if (zombieTnt) game.items().zombieTntDrops().spawn(token);
        if (home) game.items().homes().spawn(token);
        for (var kind : LbcFallingNetheriteDropItem.Kind.values()) {
            if (LbcFallingNetheriteDropItem.matches(token.getItem(), kind)) game.items().fallingNetheriteDrops().spawn(token, kind);
        }
        if (infinity) game.items().infinity().spawn(token);
        if (chaosActors) game.items().chaosTrapActors().spawn(token, "forest");
        if (sauvojenBoss) game.items().sauvojenBoss().spawn(token);
        if (target != null) for (var kind : LbcHoleDropItem.Kind.values()) {
            if (LbcHoleDropItem.matches(token.getItem(), kind)) game.items().holeDrops().spawn(token, kind, target);
        }
        if (target != null) for (var kind : LbcCageDropItem.Kind.values()) {
            if (kind != LbcCageDropItem.Kind.OBSIDIAN && LbcCageDropItem.matches(token.getItem(), kind))
                game.items().cageDrops().spawn(token, kind, target);
        }
        if (chaosActors) game.items().chaosTrapActors().spawn(token, "evilluckyblock");
        if (target != null && LbcCageDropItem.matches(token.getItem(), LbcCageDropItem.Kind.OBSIDIAN))
            game.items().cageDrops().spawn(token, LbcCageDropItem.Kind.OBSIDIAN, target);
        if (scare) {
            // This trap chooses any eligible nearest player, independently of allegiance.
            var victim = game.items().cageDrops().target(token);
            if (victim != null) for (var kind : LbcScareDropItem.Kind.values()) {
                if (LbcScareDropItem.matches(token.getItem(), kind))
                    game.items().screamer(victim, owner(token) instanceof net.minecraft.server.level.ServerPlayer player ? player : null,
                            kind == LbcScareDropItem.Kind.OLD);
            }
        }
        if (drone) game.items().droneDrops().spawn(token);
        for(var kind:LbcPotionSpawnerDropItem.Kind.values())
            if(LbcPotionSpawnerDropItem.matches(token.getItem(),kind)) game.items().potionSpawnerDrops().spawn(token,kind);
        if(wealth)game.items().eternalWealth().spawn(token);
        if (library) game.items().libraryDrop().spawn(token);
        if (luckyRoom) game.items().luckyRoomDrops().spawn(token);
        for(var kind:LbcIslandDropItem.Kind.values())
            if(LbcIslandDropItem.matches(token.getItem(),kind)) game.items().islandDrops().spawn(token,kind);
        if (pigs) game.items().luckyPigs().spawn(token);
        if (portal) game.items().portalDrops().spawn(token);
        if (bees) game.items().beeDrops().spawn(token);
        if (magicBean) game.items().magicBean().spawn(token);
        if(bullets) game.items().creatureDrops().spawn(token,LbcCreatureDrops.Kind.BULLETS);
        if(vex) game.items().creatureDrops().spawn(token,LbcCreatureDrops.Kind.VEX_OLD);
        if(normalVex) game.items().creatureDrops().spawn(token,LbcCreatureDrops.Kind.VEX);
        if (LbcFeedbackDropItem.matches(token.getItem(), LbcFeedbackDropItem.Kind.YAMETE))
            game.items().feedbackDrops().spawn(token, LbcFeedbackDropItem.Kind.YAMETE);
        for (var kind : LbcOpAnvilDropItem.Kind.values())
            if (LbcOpAnvilDropItem.matches(token.getItem(), kind)) game.items().opAnvilDrops().spawn(token, kind);
        if (chaosActors) game.items().chaosTrapActors().spawn(token, "aimbob");
        for (var kind : LbcSlabDropItem.Kind.values())
            if (LbcSlabDropItem.matches(token.getItem(), kind)) game.items().slabDrops().spawn(token, kind);
        if(fatSkeleton) game.items().creatureDrops().spawn(token,LbcCreatureDrops.Kind.FAT_SKELETON);
        if(skeleton) game.items().creatureDrops().spawn(token,LbcCreatureDrops.Kind.SKELETON_OLD);
        if (chaosActors) game.items().chaosTrapActors().spawn(token, "bob");
        if(bob) game.items().creatureDrops().spawn(token,LbcCreatureDrops.Kind.BOB_OLD);
        if (pisa) game.items().pisaDrops().spawn(token);
        if (ultraponos) game.items().ultraponos().spawn(token);
        if (enzan) game.items().enzanDrop().spawn(token);
        for (var kind : new LbcAntDropItem.Kind[]{LbcAntDropItem.Kind.ANTS,LbcAntDropItem.Kind.ACID_BURST})
            if (LbcAntDropItem.matches(token.getItem(),kind)) game.items().antDrops().spawn(token,kind);
        if (antigravity) game.items().antigravity().spawn(token);
        if(grimoireDungeon)game.items().grimoireDungeon().spawn(token);
        if (LbcFeedbackDropItem.matches(token.getItem(), LbcFeedbackDropItem.Kind.DEATH_LUST))
            game.items().feedbackDrops().spawn(token, LbcFeedbackDropItem.Kind.DEATH_LUST);
        if(silkToken)LbcLuckyBlocks.silkToken(token);
        if(family)game.items().familyDrops().spawn(token);
        if (transformator) game.items().transformator().spawn(token);
        if (stalin) game.items().stalin().spawn(token);
        if(academy&&!game.items().academy().spawnDrop(token,false))return false;
        if(helicopter&&!game.items().helicopterEncounter().spawn(token,false))return false;
        if(crocodildo&&!game.items().crocodildo().spawn(token,false))return false;
        if(debit)consume(token);
        return true;
    }
    private static void consume(ItemEntity token){
        var remaining = token.getItem().copy();
        remaining.shrink(1);
        if (remaining.isEmpty()) token.discard(); else token.setItem(remaining);
    }

    private static boolean resident(ServerLevel level, BlockPos pos, int border) {
        // Shape and neighbour updates may inspect blocks on the other side of a chunk border.
        int minX = (pos.getX() - border) >> 4, maxX = (pos.getX() + border) >> 4;
        int minZ = (pos.getZ() - border) >> 4, maxZ = (pos.getZ() + border) >> 4;
        for (int chunkX = minX; chunkX <= maxX; chunkX++) for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
            if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) return false;
        }
        return true;
    }

    private static void place(ServerLevel level, BlockPos pos, LbcChestDropItem.Kind kind) {
        placeLootChest(level, pos, kind.table());
    }

    /** Shared native replacement for dropped chests and growing Magic Bean rewards. */
    static void placeLootChest(ServerLevel level, BlockPos pos, net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> table) {
        if (!replaceContainer(level, pos)) return;
        var oldState = level.getBlockState(pos);
        var tag = new CompoundTag();
        tag.putString("LootTable", table.identifier().toString());
        // Same default-state shape resolution and flags as native /setblock ... replace.
        var input = new BlockInput(Blocks.CHEST.defaultBlockState(), Set.of(), tag);
        if (input.place(level, pos, 258)) level.updateNeighboursOnBlockSet(pos, oldState);
    }

    private static boolean replaceContainer(ServerLevel level, BlockPos pos) {
        var old = level.getBlockEntity(pos);
        // Author: eject the replaced inventory. Only this block entity is used,
        // never the combined double-chest inventory or its neighbour's contents.
        if (old instanceof RandomizableContainer randomizable) randomizable.unpackLootTable(null);
        if (old instanceof Container container) {
            // Native splitting retains complete components and empties every stack.
            Containers.dropContents(level, pos, container);
            container.clearContent();
        }
        if (old instanceof ChestBlockEntity) {
            // Same-state setBlock can skip BE creation. Replace the whole payload,
            // including old name/lock/components/seed, before installing the new table.
            var state = level.getBlockState(pos);
            level.removeBlockEntity(pos);
            level.setBlockEntity(new ChestBlockEntity(pos, state));
        }
        return true;
    }

    /** Native body replay suppresses load callbacks; rebuild only the derived admission index. */
    void reindexRestored(Iterable<Entity> bodies) {
        if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())
            throw new IllegalStateException("Reindex restored drops on the frozen native thread");
        stop();
        for(var body:bodies)if(body instanceof ItemEntity)loaded(body);
    }

    public void stop() {
        pending.clear();
        authors.clear();
        lastAttempt.clear();
        lastTick = Integer.MIN_VALUE;
    }

    int pendingCount() { return pending.size(); }
}
