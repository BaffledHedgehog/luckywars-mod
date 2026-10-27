package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real native spawn, merge, persistence, pickup and Mending checks. */
public final class CoreEntityMergeChecks {
    private CoreEntityMergeChecks() { }
    private static void require(boolean value, String why) {
        if (!value) throw new IllegalStateException("Core entity merging: " + why);
    }
    private static Fixture running;
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ElementalGrimoireChecks.Fixture base;
        final ServerPlayer owner;
        final double itemRadius, xpRadius;
        final int xp, level, delay;
        final float progress;
        final Set<Entity> owned = Collections.newSetFromMap(new IdentityHashMap<>());
        int warm;
        boolean ready, closed;
        Fixture(GameController game) {
            this.game = game;
            base = new ElementalGrimoireChecks.Fixture(game);
            owner = base.owner;
            xp = owner.totalExperience; level = owner.experienceLevel;
            delay = owner.takeXpDelay; progress = owner.experienceProgress;
            var settings = CoreEntityMerging.settings();
            itemRadius = settings.itemMergeRadius; xpRadius = settings.experienceMergeRadius;
            settings.itemMergeRadius = 2.5; settings.experienceMergeRadius = 3;
            base.initialize();
        }
        Vec3 at() { return owner.position().add(6, 0, 0); }
        void clear() { owned.forEach(Entity::discard); owned.clear(); }
        void recordNearby() {
            for (var e : owner.level().getEntities((Entity) null, owner.getBoundingBox().inflate(12),
                    e -> !base.before.contains(e) && (e instanceof ItemEntity || e instanceof ExperienceOrb))) owned.add(e);
        }
        ItemEntity item(ItemStack stack, Vec3 pos) {
            var e = new ItemEntity(owner.level(), pos.x, pos.y, pos.z, stack);
            e.setNoGravity(true); e.setDeltaMovement(Vec3.ZERO);
            require(owner.level().addFreshEntity(e), "item publication"); owned.add(e); return e;
        }
        ExperienceOrb orb(int value, Vec3 pos) {
            var e = new ExperienceOrb(owner.level(), pos, Vec3.ZERO, value);
            e.setNoGravity(true); e.setDeltaMovement(Vec3.ZERO);
            require(owner.level().addFreshEntity(e), "XP publication or absorption"); owned.add(e); return e;
        }
        void merge(ItemEntity e) {
            try { var method = ItemEntity.class.getDeclaredMethod("mergeWithNeighbours"); method.setAccessible(true); method.invoke(e); }
            catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        }
        CompoundTag save(Entity e) {
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, owner.registryAccess());
            e.saveWithoutId(output); return output.buildResult();
        }
        ExperienceOrb load(CompoundTag tag) {
            var orb = new ExperienceOrb(EntityTypes.EXPERIENCE_ORB, owner.level());
            orb.load(TagValueInput.create(ProblemReporter.DISCARDING, owner.registryAccess(), tag));
            owned.add(orb); return orb;
        }
        List<ExperienceOrb> nearby() {
            return owner.level().getEntitiesOfClass(ExperienceOrb.class, owner.getBoundingBox().inflate(12),
                    e -> !base.before.contains(e) && !e.isRemoved());
        }
        void itemTests() {
            var a = item(new ItemStack(Items.COBBLESTONE, 40), at());
            var b = item(new ItemStack(Items.COBBLESTONE, 24), at().add(2, 1, 0));
            merge(a);
            require(a.isRemoved() != b.isRemoved(), "native extended XYZ merge");
            require((a.isRemoved() ? b : a).getItem().getCount() == 64, "exact stack count conserved");
            clear();
            a = item(new ItemStack(Items.COBBLESTONE, 40), at());
            b = item(new ItemStack(Items.COBBLESTONE, 40), at().add(2, 0, 0));
            merge(a);
            require(!a.isRemoved() && !b.isRemoved() && a.getItem().getCount() + b.getItem().getCount() == 80, "native stack limit");
            clear();
            a = item(new ItemStack(Items.STONE, 1), at());
            var named = new ItemStack(Items.STONE, 1);
            named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("distinct"));
            b = item(named, at().add(1, 0, 0)); merge(a);
            require(!a.isRemoved() && !b.isRemoved(), "different components never merge");
            clear();
            a = item(new ItemStack(Items.STONE, 1), at()); a.setTarget(owner.getUUID());
            b = item(new ItemStack(Items.STONE, 1), at().add(1, 0, 0)); b.setTarget(UUID.randomUUID()); merge(a);
            require(!a.isRemoved() && !b.isRemoved(), "native pickup ownership preserved");
            clear();
            a = item(new ItemStack(Items.STONE, 1), at());
            b = item(new ItemStack(Items.STONE, 1), at().add(1, 0, 0)); b.setNeverPickUp(); merge(a);
            require(!a.isRemoved() && !b.isRemoved(), "never-pickup display remains separate");
            clear();
            a = item(new ItemStack(Items.STONE, 1), at());
            b = item(new ItemStack(Items.STONE, 1), at().add(4, 0, 0)); merge(a);
            require(!a.isRemoved() && !b.isRemoved(), "outside configured item radius");
            clear();
            CoreEntityMerging.settings().itemMergeRadius = 0;
            a = item(new ItemStack(Items.STONE, 1), at());
            b = item(new ItemStack(Items.STONE, 1), at().add(2, 0, 0)); merge(a);
            require(!a.isRemoved() && !b.isRemoved(), "zero radius restores vanilla item search");
            CoreEntityMerging.settings().itemMergeRadius = 2.5; clear();
        }
        void experienceTests() {
            var a = orb(7, at()); var b = orb(37, at().add(2, 0, 0));
            require(!a.isRemoved() && b.isRemoved() && a.getValue() == 44 && nearby().size() == 1, "different denominations merge before publication");
            ExperienceOrb.award(owner.level(), at(), 100_000); recordNearby();
            require(nearby().size() == 1 && a.getValue() == 100_044, "one award, complete XP, no native denomination splitting");
            var loaded = load(save(a));
            require(loaded.getValue() == 100_044, "native persistence exceeds short limit");
            var tag = save(a); tag.putShort("Value", (short) 7); tag.putInt("Count", 9);
            var legacy = load(tag);
            require(((CoreEntityMerging.Orb) legacy).skywars2$totalExperience() == 63, "legacy short and multiplicity read");
            require(owner.level().addFreshEntity(legacy) && legacy.isRemoved() && a.getValue() == 100_107, "legacy grouped XP merges without loss");
            clear();
            a = orb(Integer.MAX_VALUE - 5, at()); b = orb(10, at());
            require(!a.isRemoved() && !b.isRemoved() && (long) a.getValue() + b.getValue() == (long) Integer.MAX_VALUE + 5, "overflow keeps independent XP rather than wrapping or loss");
            loaded = load(save(a)); require(loaded.getValue() == Integer.MAX_VALUE - 5, "large persisted int XP");
            clear();
            a = orb(5, at()); b = orb(7, at().add(5, 0, 0));
            require(!a.isRemoved() && !b.isRemoved(), "outside configured XP radius");
            b.setPos(at().add(1, 0, 0));
            try { var scan = ExperienceOrb.class.getDeclaredMethod("scanForMerges"); scan.setAccessible(true); scan.invoke(a); }
            catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
            require(a.getValue() == 12 && b.isRemoved(), "native periodic scan merges existing mixed-value orbs");
            clear();
            long started = System.nanoTime();
            for (int n = 0; n < 1_000; n++) orb(1 + n % 7, at());
            require(nearby().size() == 1 && nearby().getFirst().getValue() == 3997, "1000 immediate spawns retain exactly one orb and all XP");
            SkyWars2.LOGGER.info("[check] core XP burst1000: {}ms; one indexed orb", (System.nanoTime() - started) / 1_000_000d);
            clear();
            CoreEntityMerging.settings().experienceMergeRadius = 0;
            a = orb(7, at()); b = orb(37, at());
            require(!a.isRemoved() && !b.isRemoved(), "zero XP radius restores native spawn");
            CoreEntityMerging.settings().experienceMergeRadius = 3; clear();
        }
        void pickupTests() {
            owner.getInventory().clearContent(); owner.takeXpDelay = 0;
            var tag = save(new ExperienceOrb(owner.level(), at(), Vec3.ZERO, 7)); tag.putInt("Count", 9);
            var old = load(tag);
            int before = owner.totalExperience; old.playerTouch(owner);
            require(old.isRemoved() && owner.totalExperience - before == 63 && owner.takeXpDelay == 2, "one-touch pickup consumes complete old group without altering native cooldown");
            var pick = new ItemStack(Items.DIAMOND_PICKAXE);
            pick.enchant(owner.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING), 1);
            pick.setDamageValue(1500); owner.getInventory().setItem(0, pick); owner.getInventory().setSelectedSlot(0);
            var big = orb(50_000_000, at()); before = owner.totalExperience; owner.takeXpDelay = 0; big.playerTouch(owner);
            require(big.isRemoved() && pick.getDamageValue() == 0 && owner.totalExperience - before == 49_999_250, "Mending repairs first, wide multiplication and exact remaining XP");
            clear();
        }
        boolean step() {
            require(++warm <= 200, "resident entity sections ready");
            for (var p : List.of(at(), at().add(5, 0, 0), at().add(-4, 0, 0)))
                if (!owner.level().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(net.minecraft.util.Mth.floor(p.x) >> 4, net.minecraft.util.Mth.floor(p.z) >> 4))) return false;
            ready = true; itemTests(); experienceTests(); pickupTests(); return true;
        }
        @Override public void close() {
            if (closed) return; closed = true;
            try { recordNearby(); clear(); }
            finally {
                CoreEntityMerging.settings().itemMergeRadius = itemRadius;
                CoreEntityMerging.settings().experienceMergeRadius = xpRadius;
                owner.totalExperience = xp; owner.experienceLevel = level;
                owner.experienceProgress = progress; owner.takeXpDelay = delay;
                base.retain = true; base.close();
            }
        }
    }
    public static void run(GameController game) { require(running == null, "exclusive fixture"); running = new Fixture(game); }
    public static void tick(GameController game) {
        if (running == null) return;
        try {
            if (running.step()) {
                running.close(); running = null;
                SkyWars2.LOGGER.info("[check] PASS entity-merging (native radius/stack/components/ownership; instant mixed XP, grouped legacy, int persistence/overflow; burst1000; complete pickup and Mending)");
            }
        } catch (Throwable error) {
            try { running.close(); } catch (Throwable cleanup) { error.addSuppressed(cleanup); }
            finally { running = null; }
            SkyWars2.LOGGER.error("[check] FAIL entity-merging", error);
        }
    }
}
