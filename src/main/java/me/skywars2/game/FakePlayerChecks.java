package me.skywars2.game;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.UpdateInterval;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class FakePlayerChecks {
    private FakePlayerChecks() {}

    private static void require(boolean condition, String why) {
        if (!condition) throw new IllegalStateException("Fake player: " + why);
    }

    private static void wire(WoolBucketChecks.Fixture f, Villager actor) {
        var packets = new ArrayList<Packet<ClientGamePacketListener>>();
        var sync = new ServerEntity.Synchronizer() {
            public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet) {}
            public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet) {}
            public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet,
                                                       Predicate<net.minecraft.server.level.ServerPlayer> filter) {}
        };
        new ServerEntity(f.level, actor, UpdateInterval.periodic(3), true, sync).sendPairingData(f.owner, packets::add);
        var spawn = packets.stream().filter(ClientboundAddEntityPacket.class::isInstance)
            .map(ClientboundAddEntityPacket.class::cast).findFirst().orElseThrow();
        var metadata = packets.stream().filter(ClientboundSetEntityDataPacket.class::isInstance)
            .map(ClientboundSetEntityDataPacket.class::cast).findFirst().orElseThrow();
        var equipment = packets.stream().filter(ClientboundSetEquipmentPacket.class::isInstance)
            .map(ClientboundSetEquipmentPacket.class::cast).findFirst().orElseThrow();
        PacketContext.runWithContext((PacketContextProvider)(Object) f.client.connection, () -> {
            var raw = Unpooled.buffer();
            try {
                var buffer = new RegistryFriendlyByteBuf(raw, f.owner.registryAccess());
                ClientboundAddEntityPacket.STREAM_CODEC.encode(buffer, spawn);
                var decoded = ClientboundAddEntityPacket.STREAM_CODEC.decode(buffer);
                require(decoded.getType() == EntityTypes.MANNEQUIN && decoded.getId() == actor.getId(),
                    "pairing encodes the same actor as a native mannequin");
                buffer.clear();
                ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer, metadata);
                var model = new Mannequin(EntityTypes.MANNEQUIN, f.level);
                model.getEntityData().assignValues(ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer).packedItems());
                require(model.getProfile().equals(ResolvableProfile.createResolved(f.other.getGameProfile()))
                        && model.isCustomNameVisible() && actor.getCustomName().equals(model.getCustomName()),
                    "vanilla mannequin decodes the represented skin and styled name without villager data");
                require(model.getMainArm() == f.other.getMainArm(), "copied dominant hand");
                buffer.clear();
                ClientboundSetEquipmentPacket.STREAM_CODEC.encode(buffer, equipment);
                var received = ClientboundSetEquipmentPacket.STREAM_CODEC.decode(buffer);
                for (var slot : List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD)) {
                    var actual = received.getSlots().stream().filter(p -> p.getFirst() == slot).findFirst().orElseThrow();
                    require(ItemStack.matches(actual.getSecond(), f.other.getItemBySlot(slot)), "paired equipment " + slot);
                }
            } finally {
                raw.release();
            }
        });
    }

    private static ServerEntity equipmentTracker(WoolBucketChecks.Fixture f, Villager actor,
                                                  List<ClientboundSetEquipmentPacket> sent) {
        var sync = new ServerEntity.Synchronizer() {
            public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet) {
                if (packet instanceof ClientboundSetEquipmentPacket equipment) sent.add(equipment);
            }
            public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet) {
                sendToTrackingPlayers(packet);
            }
            public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet,
                                                       Predicate<net.minecraft.server.level.ServerPlayer> filter) {
                if (filter.test(f.owner)) sendToTrackingPlayers(packet);
            }
        };
        return new ServerEntity(f.level, actor, UpdateInterval.periodic(3), true, sync);
    }

    private static void mirror(WoolBucketChecks.Fixture f, Villager actor, Villager restored, ItemStack carried) {
        List<ClientboundSetEquipmentPacket> sent = new ArrayList<>();
        var tracker = equipmentTracker(f, actor, sent);
        var source = f.other;
        var selected = new ItemStack(Items.DIAMOND_AXE);
        selected.set(DataComponents.CUSTOM_NAME, Component.literal("Live selected item"));
        source.getInventory().setItem(6, selected);
        source.connection.handleSetCarriedItem(new ServerboundSetCarriedItemPacket(6));
        require(source.getInventory().getSelectedSlot() == 6, "real held-slot packet updates the represented player");
        source.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        tracker.sendChanges();
        require(LbcFakePlayer.selectedSlot(actor) == 6 && ItemStack.matches(LbcFakePlayer.copiedSlot(actor, 6), selected)
                && ItemStack.matches(LbcFakePlayer.copiedSlot(actor, 24), carried)
                && actor.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty(),
            "native tracking mirrors exact selected and private inventory slots without changing Villager equipment");
        require(sent.size() == 1 && sent.getFirst().getEntity() == actor.getId() && sent.getFirst().getSlots().size() == 2
                && sent.getFirst().getSlots().stream().anyMatch(p -> p.getFirst() == EquipmentSlot.MAINHAND
                        && ItemStack.matches(p.getSecond(), selected))
                && sent.getFirst().getSlots().stream().anyMatch(p -> p.getFirst() == EquipmentSlot.OFFHAND && p.getSecond().isEmpty()),
            "existing viewers receive the selected item and the cleared offhand, with no unrelated slots");
        tracker.sendChanges();
        carried.shrink(1);
        var name = Component.literal("In-place component change");
        carried.set(DataComponents.CUSTOM_NAME, name);
        tracker.sendChanges();
        require(sent.size() == 1 && ItemStack.matches(LbcFakePlayer.copiedSlot(actor, 24), carried),
            "in-place hidden stack count and component updates mirror without equipment spam");
        LbcFakePlayer.copiedSlot(actor, 24).setCount(1);
        require(LbcFakePlayer.copiedSlot(actor, 24).getCount() == carried.getCount(),
            "callers cannot mutate the source or the stored copy through a copied slot");

        sent.clear();
        equipmentTracker(f, restored, sent).sendChanges();
        require(LbcFakePlayer.selectedSlot(restored) == 6 && ItemStack.matches(LbcFakePlayer.copiedSlot(restored, 24), carried),
            "native saved disguise rebinds the same source UUID and refreshes selected and hidden slots");
    }

    private static BlockHitResult top(BlockPos block) {
        return new BlockHitResult(Vec3.atCenterOf(block).add(0, .5, 0), Direction.UP, block, false);
    }

    private static List<UUID> drops(WoolBucketChecks.Fixture f, Vec3 origin) {
        var result = new ArrayList<UUID>();
        for (var entity : f.level.getEntities((Entity) null, AABB.ofSize(origin, 10, 10, 10),
                e -> e instanceof ItemEntity || e.getType() == EntityTypes.EXPERIENCE_ORB)) {
            result.add(entity.getUUID());
        }
        return result;
    }

    private static void checkSpawner(WoolBucketChecks.Fixture f, BlockPos position, ItemStack egg,
                                     ItemStack carried, String admission) {
        f.set(position, Blocks.SPAWNER.defaultBlockState());
        var spawner = (SpawnerBlockEntity) Objects.requireNonNull(f.level.getBlockEntity(position));
        f.owner.setItemInHand(InteractionHand.MAIN_HAND, egg);
        require(f.owner.gameMode.useItemOn(f.owner, f.level, egg, InteractionHand.MAIN_HAND, top(position)).consumesAction()
                    && egg.isEmpty(),
            admission + " egg use configures the spawner and consumes one egg");
        var next = spawner.getSpawner().getOrCreateDisplayEntity(f.level, position);
        require(next instanceof Villager villager && LbcFakePlayer.matches(villager)
                    && Objects.equals(villager.getCustomName(), f.other.getDisplayName())
                    && ItemStack.matches(LbcFakePlayer.copiedSlot(villager, 24), carried),
            admission + " spawner resolves the copied identity from stored data");
    }

    static void run(GameController game) {
        try (var f = new WoolBucketChecks.Fixture(game,false)) {
            f.prepareParticipants();
            var source = f.other;
            var at = BlockPos.containing(f.resident(false));
            for (var cell : BlockPos.betweenClosed(at.offset(-3,-1,-3), at.offset(3,2,3))) {
                f.set(cell.immutable(), cell.getY() < at.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            var point = Vec3.atBottomCenterOf(at);
            f.move(f.owner, point.add(-2, 0, 0));
            f.move(source, point.add(2, 0, 0));
            var team = game.server().getScoreboard().addPlayerTeam("fake_player_check");
            team.setPlayerPrefix(Component.literal("[Copy] ").withStyle(ChatFormatting.AQUA));
            team.setColor(java.util.Optional.of(net.minecraft.world.scores.TeamColor.GOLD));
            game.server().getScoreboard().addPlayerToTeam(source.getScoreboardName(), team);
            var rule = f.level.getGameRules();
            boolean originalDrops = rule.get(GameRules.MOB_DROPS);
            rule.set(GameRules.MOB_DROPS, true, game.server());
            try {
                source.getInventory().setSelectedSlot(3);
                source.getInventory().setItem(3, new ItemStack(Items.IRON_SWORD));
                source.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                source.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                var carried = new ItemStack(Items.EMERALD, 11);
                carried.set(DataComponents.CUSTOM_NAME, Component.literal("Snapshot witness"));
                source.getInventory().setItem(24, carried);

                var key = "lbc:item_defs/minecraft/villager_spawn_egg__b73a1e192ddc";
                var egg = game.lbc().rollTable(f.level, source.position(), source, key).getFirst();
                require(LbcFakePlayer.matches(egg) && egg.get(DataComponents.CUSTOM_NAME).getString().contains("[Copy]"),
                    "actual loot resolves the authored nearest player and their prefix");
                egg.setCount(2);
                f.owner.setItemInHand(InteractionHand.MAIN_HAND, egg);
                require(f.owner.gameMode.useItemOn(f.owner, f.level, egg, InteractionHand.MAIN_HAND, top(at.below())).consumesAction()
                        && egg.getCount() == 1,
                    "native use spawns one copy and debits exactly one egg");
                var actors = f.level.getEntitiesOfClass(Villager.class, AABB.ofSize(point, 4, 4, 4), LbcFakePlayer::matches);
                require(actors.size() == 1, "one real Villager for the disguise");
                var actor = actors.getFirst();
                f.own(actor);
                require(actor.getClass() == Villager.class && !actor.isNoAi() && actor.getBrain().isActive(Activity.CORE),
                    "real native Villager brain and core activity");
                require(actor.getCustomName().equals(source.getDisplayName())
                        && ItemStack.matches(LbcFakePlayer.copiedSlot(actor, 24), carried)
                        && actor.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty(),
                    "styled name and complete inventory come from the named player, with display-only equipment");
                wire(f, actor);

                // A spawner stores all of the egg's payload and can recover the same represented player.
                var operators = game.server().getPlayerList().getOps();
                var account = f.owner.nameAndId();
                var op = Objects.requireNonNull(operators.get(account), "native operator fixture");
                checkSpawner(f, at.north(2), egg.copy(), carried, "operator");
                require(operators.remove(account) && !game.server().getPlayerList().isOp(account), "native non-operator fixture");
                try {
                    checkSpawner(f, at.south(2), egg, carried, "non-operator");
                } finally {
                    operators.add(op);
                }

                var snapshot = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, f.level.registryAccess());
                actor.saveWithoutId(snapshot);
                carried.shrink(5);
                var restored = Objects.requireNonNull(EntityTypes.VILLAGER.create(f.level, EntitySpawnReason.LOAD));
                restored.load(TagValueInput.create(ProblemReporter.DISCARDING, f.level.registryAccess(), snapshot.buildResult()));
                require(PolymerEntity.get(restored) != null
                        && PolymerEntity.get(restored).getPolymerEntityType(PacketContext.get()) == EntityTypes.MANNEQUIN
                        && LbcFakePlayer.copiedSlot(restored,24).getCount() == 11,
                    "saved disguise restores the original independent inventory snapshot");
                mirror(f, actor, restored, carried);

                actor.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND));
                actor.setGuaranteedDrop(EquipmentSlot.MAINHAND);
                var before = drops(f, point);
                require(actor.hurtServer(f.level, actor.damageSources().playerAttack(f.owner), 100) && actor.isDeadOrDying(),
                    "a real player can kill the disguised Villager");
                require(drops(f, point).equals(before), "disguise publishes no items or experience on death");

                var ordinary = Objects.requireNonNull(EntityTypes.VILLAGER.create(f.level, EntitySpawnReason.COMMAND));
                ordinary.setPos(point);
                ordinary.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND));
                ordinary.setGuaranteedDrop(EquipmentSlot.MAINHAND);
                require(f.level.addFreshEntity(ordinary), "ordinary native Villager control");
                f.own(ordinary);
                require(PolymerEntity.get(ordinary) == null && ordinary.hurtServer(f.level, ordinary.damageSources().playerAttack(f.owner),100),
                    "ordinary Villager keeps its vanilla type and death");
                var extra = new ArrayList<>(drops(f, point));
                extra.removeAll(before);
                for (var id : extra) f.own(Objects.requireNonNull(f.level.getEntity(id)));
                require(extra.stream().map(f.level::getEntity)
                        .anyMatch(entity -> entity instanceof ItemEntity drop && drop.getItem().is(Items.DIAMOND)),
                    "ordinary native equipment still drops with guaranteed drop chance");
            } finally {
                rule.set(GameRules.MOB_DROPS, originalDrops, game.server());
                game.server().getScoreboard().removePlayerTeam(team);
            }
        }
        SkyWars2.LOGGER.info("[check] PASS fake-player (real survival egg and spawner payload, Villager brain, vanilla mannequin metadata, live selected slot, full inventory and equipment mirroring, saved source UUID, no death loot)");
    }
}
