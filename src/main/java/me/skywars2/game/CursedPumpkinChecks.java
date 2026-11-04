package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.phys.Vec3;

/** Observes real scheduled inventory pulses, accepted melee and full coordinator cursor rewind. */
public final class CursedPumpkinChecks {
    private CursedPumpkinChecks() { }
    private static Fixture running;
    private static void require(boolean ok, String why) { if (!ok) throw new IllegalStateException("Cursed pumpkin: " + why); }
    public static void run(GameController game) {
        require(running == null, "exclusive scheduled fixture");
        running = new Fixture(game);
        try { running.init(); } catch (Throwable failure) {
            try { running.close(); } catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            running = null; throw new IllegalStateException(failure);
        }
    }
    public static void tick(GameController game) {
        if (running == null) return;
        try {
            if (!running.step()) return;
            running.close(); running = null;
            SkyWars2.LOGGER.info("[check] PASS cursed-pumpkin (scheduled source35 topology/within-pass armor/offhand, exact native stored stacks and persistence/Polymer isolation; protected inventory/MAG/TECH/real TimeStop; actual sword target-only melee; empty unpack/no phantom stone; 300 distinct outputs under MAG with partial-cursor full coordinator rewind)");
        } catch (Throwable failure) {
            try { running.close(); } catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            running = null; SkyWars2.LOGGER.error("[check] FAIL cursed-pumpkin", failure);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final LbcCursedPumpkin service;
        final ElementalGrimoireChecks.Fixture base;
        final ServerPlayer owner, other;
        final RegenerationBeltChecks.Snapshot ownerSaved, otherSaved;
        final LbcCursedPumpkin.Checkpoint saved;
        final Set<UUID> before = new HashSet<>();
        final List<ItemStack> payload = new ArrayList<>();
        final Map<Integer, ItemStack> victims = new LinkedHashMap<>();
        final boolean skills, borders;
        RegenerationBeltChecks.ScheduledClient client, otherClient;
        Marker jammer;
        UUID sourceId, emptyId;
        ItemStack original;
        Map<Integer, ItemStack> captured;
        int stage, at, started;
        boolean closed;

        Fixture(GameController game) {
            this.game = game; service = game.items().cursedPumpkin();
            require(game.playing() && !game.vanity().busy() && !game.vanity().hasSnapshot(), "isolated actual match and empty coordinator");
            owner = Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            other = Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            ownerSaved = new RegenerationBeltChecks.Snapshot(game, owner); otherSaved = new RegenerationBeltChecks.Snapshot(game, other);
            saved = service.checkpoint();
            require(saved.items.isEmpty() && saved.savedHolders.isEmpty(), "no unrelated native curse holders or unpack jobs");
            skills = game.config().lbcSkills; borders = game.config().noBorders;
            base = new ElementalGrimoireChecks.Fixture(game);
            owner.level().getAllEntities().forEach(e -> before.add(e.getUUID()));
        }
        void init() {
            base.initialize(); game.config().lbcSkills = true; game.config().noBorders = true;
            game.players().get(owner.getUUID()).lbcSkill = 0; game.players().get(other.getUUID()).lbcSkill = 0;
            owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000); owner.setHealth(1000);
            other.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000); other.setHealth(1000);
            other.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); other.getAbilities().invulnerable = false;
            other.teleportTo(owner.level(), owner.getX()+2, owner.getY(), owner.getZ(), Set.of(), 180, 0, true);
            other.setNoGravity(true); other.getInventory().clearContent(); other.removeAllEffects();
            client = new RegenerationBeltChecks.ScheduledClient(game, owner); otherClient = new RegenerationBeltChecks.ScheduledClient(game, other);
            started = game.server().getTickCount();
            codecAndProjection(); melee();
            fill(35); game.players().get(owner.getUUID()).lbcSkill = 2;
            original = LbcCursedPumpkin.get(owner, 35).copy(); begin(1);
        }
        ItemStack loot(String name) {
            var stacks = game.lbc().rollTable(owner.level(), owner.position(), owner, "lbc:" + name);
            require(stacks.size() == 1, "one canonical source stack " + name); return stacks.getFirst();
        }
        ItemStack curse() {
            var stack = loot("halloween/pumpkin_curse");
            require(stack.is(LbcCursedPumpkinItems.CURSE) && LbcCursedPumpkinItems.curse(stack), "canonical native cursed pumpkin"); return stack;
        }
        ItemStack specimen(int index) {
            var stack = new ItemStack(index % 2 == 0 ? Items.DIAMOND : Items.EMERALD, 1 + index % 7);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("stored " + index));
            stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("original lore " + index))));
            stack.set(DataComponents.REPAIR_COST, index + 3);
            var data = new CompoundTag(); data.putInt("pumpkin_fixture_index", index); data.putString("private", "keep "+index);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); return stack;
        }
        void codecAndProjection() {
            var stack = curse(); for (int i = 0; i < 300; i++) payload.add(specimen(i));
            stack.set(LbcCursedPumpkinItems.CONTENTS, payload.stream().map(ItemStackTemplate::fromNonEmptyStack).toList());
            var decoded = net.fabricmc.fabric.api.networking.v1.context.PacketContext.supplyWithoutContext(() -> {
                var ops = owner.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                var encoded = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
                return ItemStack.CODEC.parse(ops, encoded).getOrThrow();
            });
            require(ItemStack.matches(stack, decoded) && decoded.get(LbcCursedPumpkinItems.CONTENTS).size() == 300,
                    "native persistent ItemStack codec retains all300 templates without container truncation");
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                @Override public net.minecraft.network.PacketListener getPacketListener() { return owner.connection; }
            };
            var provider = (net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider) connection;
            net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider, () -> {
                var proxy = PolymerItemUtils.getPolymerItemStack(stack, TooltipFlag.NORMAL, provider.getPacketContext(), owner.registryAccess());
                require(proxy.get(LbcCursedPumpkinItems.CONTENTS) == null, "server-only unbounded contents excluded from real recipient projection");
                require(ItemStack.matches(stack, PolymerItemUtils.getRealItemStack(proxy, provider.getPacketContext(), owner.registryAccess())),
                        "actual authoritative Polymer roundtrip retains every stored component");
            });
        }
        int curses(ServerPlayer player) {
            int n = 0; for (int slot = 0; slot < 41; slot++) if (LbcCursedPumpkinItems.curse(LbcCursedPumpkin.get(player, slot))) n++; return n;
        }
        void hit(DamageSource source) {
            other.setInvulnerableTime(0); other.damageCooldownTime = 0;
            require(other.hurtServer(other.level(), source, 1), "actual native accepted damage");
        }
        void melee() {
            owner.getInventory().clearContent(); other.getInventory().clearContent();
            var sword = loot("item_defs/lbc/pumpkin_sword__b77205d8f101");
            require(sword.is(LbcCursedPumpkinItems.SWORD), "canonical native Pumpkin King sword"); owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
            require(game.items().wands().projectileTarget(owner, other), "actual opposite eligible participants");
            game.players().get(other.getUUID()).lbcSkill = 2;
            hit(other.damageSources().playerAttack(owner)); require(curses(other) == 0, "actual melee respects protected victim inventory");
            game.players().get(other.getUUID()).lbcSkill = 0;
            jammer(true);
            try { hit(other.damageSources().playerAttack(owner)); require(curses(other) == 0, "actual melee curse is MAG suppressed"); }
            finally { unjam(); }
            hit(other.damageSources().indirectMagic(owner, owner)); require(curses(other) == 0, "nonmelee attributed spell cannot curse inventory");
            hit(other.damageSources().playerAttack(owner));
            require(curses(other) == 1 && curses(owner) == 0, "actual sword melee delivers canonical curse only to victim");
            other.getInventory().clearContent(); owner.getInventory().clearContent(); service.inventoryChanged(other);
        }
        void fill(int slot) {
            owner.getInventory().clearContent(); victims.clear();
            for (int i = 0; i < 41; i++) { var stack = specimen(1000+i); victims.put(i, stack.copy()); LbcCursedPumpkin.set(owner, i, stack); }
            var pumpkin = curse(); if (slot >= 37) pumpkin.set(DataComponents.EQUIPPABLE, Equippable.builder(equipment(slot)).build());
            LbcCursedPumpkin.set(owner, slot, pumpkin); service.inventoryChanged(owner);
        }
        EquipmentSlot equipment(int slot) { return switch(slot) { case 37 -> EquipmentSlot.HEAD; case 38 -> EquipmentSlot.CHEST; case 39 -> EquipmentSlot.LEGS; case 40 -> EquipmentSlot.FEET; default -> EquipmentSlot.OFFHAND; }; }
        int location() { int result = -1; for (int slot = 0; slot < 41; slot++) if (LbcCursedPumpkinItems.curse(LbcCursedPumpkin.get(owner, slot))) { require(result < 0, "one original moving pumpkin"); result = slot; } return result; }
        boolean movedFrom(int slot) {
            var stack = location() < 0 ? ItemStack.EMPTY : LbcCursedPumpkin.get(owner, location());
            return !stack.getOrDefault(LbcCursedPumpkinItems.CONTENTS, List.of()).isEmpty();
        }
        void jammer(boolean magic) {
            unjam(); jammer = new Marker(EntityTypes.MARKER, owner.level()); jammer.setPos(owner.position());
            jammer.entityTags().add(magic ? "stopper_magic" : "stopper"); require(owner.level().addFreshEntity(jammer), "real jammer marker"); game.items().magic().loaded(jammer);
            require(magic ? game.items().magic().suppressed(owner) : game.items().magic().mechanismSuppressed(owner), "real jammer positive control");
        }
        void unjam() {
            if (jammer == null) return;
            var rebound = owner.level().getEntity(jammer.getUUID());
            if (rebound != null && rebound != jammer) { game.items().magic().unloaded(rebound); rebound.discard(); }
            game.items().magic().unloaded(jammer); jammer.discard(); jammer = null;
        }
        void begin(int next) { stage = next; at = game.server().getTickCount(); }
        ItemEntity drop(ItemStack stack) {
            var at = owner.position().add(6, 0, 0); var entity = new ItemEntity(owner.level(), at.x, at.y, at.z, stack);
            entity.setNoGravity(true); entity.setDeltaMovement(Vec3.ZERO); entity.setPickUpDelay(32767); entity.setThrower(owner);
            require(owner.level().addFreshEntity(entity), "actual dropped curse publication"); return entity;
        }
        Map<Integer, ItemStack> outputs() {
            var result = new LinkedHashMap<Integer, ItemStack>();
            for (var item : owner.level().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(24), e -> !before.contains(e.getUUID()))) {
                item.setNoGravity(true); item.setDeltaMovement(Vec3.ZERO);
                int index = ItemData.intValue(item.getItem(), "pumpkin_fixture_index", -1);
                if (index < 0 || index >= 300) continue;
                require(!result.containsKey(index), "no duplicate stored output index " + index); result.put(index, item.getItem().copy());
            }
            return result;
        }
        boolean step() {
            client.acknowledge(); otherClient.acknowledge();
            int now = game.server().getTickCount(), elapsed = now-at;
            require(now-started < 2400, "bounded scheduled fixture timeout stage " + stage);
            if (stage == 1) {
                require(ItemStack.matches(original, LbcCursedPumpkin.get(owner, 35)), "skill2 protects actual scheduled inventory");
                if (elapsed < 12) return false;
                game.players().get(owner.getUUID()).lbcSkill = 0; jammer(true); begin(2); return false;
            }
            if (stage == 2) {
                require(ItemStack.matches(original, LbcCursedPumpkin.get(owner, 35)), "MAG blocks actual scheduled intake");
                if (elapsed < 12) return false;
                jammer(false); begin(3); return false;
            }
            if (stage == 3) {
                if (!movedFrom(35)) { require(elapsed <= 12, "source ten-active-tick walk executes under TECH"); return false; }
                int slot = location(); require(Set.of(26,34,8).contains(slot), "source slot35 selects only26/34/8");
                var stored = LbcCursedPumpkin.get(owner, slot).get(LbcCursedPumpkinItems.CONTENTS);
                require(stored.size() == 1 && ItemStack.matches(stored.getFirst().create(), victims.get(slot)), "one complete original destination stack preserved, first entry retained");
                require(LbcCursedPumpkin.get(owner, 35).is(Items.PUMPKIN_SEEDS), "source origin replaced with seeds");
                unjam(); fill(37); begin(4); return false;
            }
            if (stage == 4) {
                if (!movedFrom(37)) { require(elapsed <= 12, "scheduled armor walk"); return false; }
                int slot = location(); var pumpkin = LbcCursedPumpkin.get(owner, slot); var stored = pumpkin.get(LbcCursedPumpkinItems.CONTENTS);
                require(ItemStack.matches(stored.getFirst().create(), victims.get(38)) && stored.size() >= 2, "head always eats chest then repeats movement within same source pass");
                if (slot >= 37) require(pumpkin.get(DataComponents.EQUIPPABLE).slot() == equipment(slot), "moved pumpkin uses actual destination armor slot");
                fill(36); begin(5); return false;
            }
            if (stage == 5) {
                if (!movedFrom(36)) { require(elapsed <= 12, "scheduled offhand walk"); return false; }
                var stored = LbcCursedPumpkin.get(owner, location()).get(LbcCursedPumpkinItems.CONTENTS);
                require(ItemStack.matches(stored.getFirst().create(), victims.get(40)) || ItemStack.matches(stored.getFirst().create(), victims.get(13)), "source offhand enters boots or slot13 only");
                fill(35); original = LbcCursedPumpkin.get(owner, 35).copy(); game.items().timeStop().begin(owner.level(), 200); begin(6); return false;
            }
            if (stage == 6) {
                require(ItemStack.matches(original, LbcCursedPumpkin.get(owner, 35)), "real world TimeStop pauses whole inventory walk");
                if (elapsed < 22) return false;
                game.items().timeStop().end(owner.level()); owner.getInventory().clearContent(); service.inventoryChanged(owner);
                jammer(true); emptyId = drop(curse()).getUUID(); var stack = curse();
                stack.set(LbcCursedPumpkinItems.CONTENTS, payload.stream().map(ItemStackTemplate::fromNonEmptyStack).toList());
                stack.setCount(3);sourceId = drop(stack).getUUID(); begin(7); return false;
            }
            if (stage == 7) {
                var actual = outputs(); if (actual.isEmpty()) { require(elapsed <= 25, "source global-second drop intake"); return false; }
                require(actual.size() < 300 && owner.level().getEntity(sourceId) != null, "bounded worker retains nonempty partial cursor");
                require(owner.level().getEntity(emptyId) == null, "empty pumpkin discarded without phantom stone");
                captured = actual; game.items().timeStop().begin(owner.level(), 500);
                require(game.vanity().requestCapture(owner, false), "real full coordinator captures partial native unpack cursor"); begin(8); return false;
            }
            if (stage == 8) {
                require(outputs().size() == captured.size(), "TimeStop pauses partial dropped unpack");
                if (game.vanity().busy() || !game.vanity().hasSnapshot()) return false;
                game.items().timeStop().end(owner.level()); begin(9); return false;
            }
            if (stage == 9) {
                if (outputs().size() <= captured.size()) { require(elapsed <= 5, "partial native cursor really advances after capture"); return false; }
                game.items().timeStop().begin(owner.level(), 500);
                require(game.vanity().requestRestoreForCheck(), "real full coordinator restores partial cursor and emitted actor set"); begin(10); return false;
            }
            if (stage == 10) {
                if (game.vanity().busy() || !game.vanity().status().equals("IDLE")) return false;
                var actual = outputs(); require(actual.keySet().equals(captured.keySet()), "full rewind restores exact previously published output prefix");
                for (var entry : captured.entrySet()) require(ItemStack.matches(entry.getValue(), actual.get(entry.getKey())), "rewound output complete components");
                require(owner.level().getEntity(sourceId) instanceof ItemEntity, "full rewind rebinds original pending dropped item identity");
                game.vanity().stop(); game.items().timeStop().end(owner.level()); begin(11); return false;
            }
            if (stage == 11) {
                require(game.items().magic().suppressed(owner), "drop unpack remains under positive MAG after whole coordinator rewind");
                var actual = outputs(); if (actual.size() < 300) { require(elapsed <= 35, "bounded queue drains all300 source entries"); return false; }
                require(owner.level().getEntity(sourceId) == null, "source item retired only after final output");
                for (int i = 0; i < 300; i++) require(ItemStack.matches(payload.get(i), actual.get(i)), "all original count/components survive cursor rewind index " + i);
                for (var item : owner.level().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(24), e -> !before.contains(e.getUUID())))
                    require(!item.getItem().is(Items.STONE), "no source empty/terminal phantom stone");
                return true;
            }
            return false;
        }
        @Override public void close() {
            if (closed) return; closed = true; Throwable failure = null;
            for (Runnable action : List.<Runnable>of(() -> game.vanity().stop(), () -> game.items().timeStop().end(owner.level()), this::unjam,
                    () -> { for (var actor : owner.level().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(32), e -> !before.contains(e.getUUID()))) actor.discard(); },
                    () -> { if (otherClient != null) otherClient.close(); if (client != null) client.close(); }, base::close,
                    () -> { ownerSaved.restore(game); ownerSaved.restoreAttributes(); ownerSaved.restoreUse(); otherSaved.restore(game); otherSaved.restoreAttributes(); otherSaved.restoreUse(); },
                    () -> { service.prepareFrozen(); saved.prepare(id -> owner.level().getEntity(id)).install().run(); service.publishFrozen(); },
                    () -> { game.config().lbcSkills = skills; game.config().noBorders = borders; game.items().magic().inventoryChanged(owner); game.items().magic().inventoryChanged(other); game.items().magic().flushCleansing(); }))
                try { action.run(); } catch (Throwable error) { if (failure == null) failure = error; else failure.addSuppressed(error); }
            if (failure != null) throw new IllegalStateException("cursed pumpkin scoped cleanup", failure);
        }
    }
}
