package me.skywars2.game;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import me.skywars2.mixin.ChefMannequinAccess;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The real Villager keeps its brain and ordinary interactions. Only its client body is a mannequin. */
public final class LbcFakePlayer implements PolymerEntity {
    private static final String TAG = "fake_player";
    private static final String SAVE_KEY = "skywars2:fake_player_snapshot";

    private record Snapshot(ResolvableProfile profile, HumanoidArm arm, byte layers, int selected, List<ItemStack> inventory) {
        private static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResolvableProfile.CODEC.fieldOf("profile").forGetter(Snapshot::profile),
            HumanoidArm.CODEC.fieldOf("arm").forGetter(Snapshot::arm),
            Codec.BYTE.fieldOf("layers").forGetter(Snapshot::layers),
            Codec.INT.fieldOf("selected").forGetter(Snapshot::selected),
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("inventory").forGetter(Snapshot::inventory)
        ).apply(instance, Snapshot::new));

        static Snapshot capture(ServerPlayer player) {
            var inventory = player.getInventory();
            byte layers = 0;
            for (var part : PlayerModelPart.values()) {
                if (player.isModelPartShown(part)) layers |= (byte) part.getMask();
            }
            return new Snapshot(ResolvableProfile.createResolved(player.getGameProfile()), player.getMainArm(),
                layers, inventory.getSelectedSlot(), List.of()).updateInventory(inventory);
        }

        static Snapshot absent(UUID expected) {
            return new Snapshot(expected == null ? Mannequin.DEFAULT_PROFILE : ResolvableProfile.createUnresolved(expected),
                HumanoidArm.RIGHT, (byte) 127, 0, List.of());
        }

        ItemStack slot(int index) {
            return index >= 0 && index < inventory.size() ? inventory.get(index) : ItemStack.EMPTY;
        }

        Snapshot updateInventory(Inventory source) {
            int size = source.getContainerSize();
            int nextSelected = source.getSelectedSlot();
            if (size != inventory.size()) {
                var items = new ArrayList<ItemStack>(size);
                for (int slot = 0; slot < size; slot++) items.add(source.getItem(slot).copy());
                return new Snapshot(profile, arm, layers, nextSelected, List.copyOf(items));
            }
            ArrayList<ItemStack> changed = null;
            for (int slot = 0; slot < size; slot++) {
                // Stack consumption and component edits can happen without an Inventory change signal.
                var current = source.getItem(slot);
                if (ItemStack.matches(current, inventory.get(slot))) continue;
                if (changed == null) changed = new ArrayList<>(inventory);
                changed.set(slot, current.copy());
            }
            if (changed == null && selected == nextSelected) return this;
            return new Snapshot(profile, arm, layers, nextSelected, changed == null ? inventory : List.copyOf(changed));
        }
    }

    private static final class Model extends Mannequin {
        private static final int FIRST_AVATAR_FIELD = Math.min(DATA_PLAYER_MAIN_HAND.id(), DATA_PLAYER_MODE_CUSTOMISATION.id());

        Model(Level level, Snapshot snapshot) {
            super(level);
            applyImplicitComponent(DataComponents.PROFILE, snapshot.profile());
            setMainArm(snapshot.arm());
            getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, snapshot.layers());
            ((ChefMannequinAccess)(Object)this).skywars2$hideDescription(true);
        }
    }

    private Snapshot snapshot;
    private final UUID sourceId;
    private final List<SynchedEntityData.DataValue<?>> metadata;
    private final EnumMap<EquipmentSlot, ItemStack> visible = new EnumMap<>(EquipmentSlot.class);

    private LbcFakePlayer(Villager actor, Snapshot snapshot) {
        this.snapshot = snapshot;
        sourceId = snapshot.profile().partialProfile().id();
        var model = new Model(actor.level(), snapshot);
        metadata = model.getEntityData().getNonDefaultValues().stream()
            .filter(value -> value.id() >= Model.FIRST_AVATAR_FIELD).toList();
        visible.put(EquipmentSlot.MAINHAND, snapshot.slot(snapshot.selected()));
        Inventory.EQUIPMENT_SLOT_MAPPING.forEach((index, slot) -> visible.put(slot, snapshot.slot(index)));
    }

    public static boolean matches(Entity actor) {
        return actor instanceof Villager && actor.entityTags().contains(TAG);
    }

    public static boolean matches(ItemStack stack) {
        return stack.is(Items.VILLAGER_SPAWN_EGG) && tagged(stack.get(DataComponents.ENTITY_DATA));
    }

    private static boolean tagged(TypedEntityData<EntityType<?>> data) {
        return data != null && data.type() == EntityTypes.VILLAGER
            && data.getUnsafe().getList("Tags").map(tags -> tags.contains(StringTag.valueOf(TAG))).orElse(false);
    }

    public static TypedEntityData<EntityType<?>> spawnerData(ItemStack stack, TypedEntityData<EntityType<?>> data,
                                                             HolderLookup.Provider registries) {
        var name = stack.get(DataComponents.CUSTOM_NAME);
        if (name == null || !tagged(data)) return data;
        // A spawner has no item name component when it creates the stored mob.
        var payload = data.copyTagWithoutId();
        payload.put("CustomName", ComponentSerialization.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), name).getOrThrow());
        return TypedEntityData.of(data.type(), payload);
    }

    public static void adopt(Entity actor, LivingEntity user, ItemStack egg) {
        if (!matches(actor) || !matches(egg)) return;
        var name = egg.get(DataComponents.CUSTOM_NAME);
        capture((Villager) actor, name == null ? actor.getCustomName() : name, user instanceof ServerPlayer player ? player : null);
    }

    public static void read(Villager actor, ValueInput input) {
        if (!matches(actor)) {
            if (PolymerEntity.get(actor) instanceof LbcFakePlayer) PolymerEntityUtils.setPolymerEntity(actor, null);
            return;
        }
        var saved = input.read(SAVE_KEY, Snapshot.CODEC);
        if (saved.isPresent()) install(actor, saved.get());
        else capture(actor, actor.getCustomName(), null);
    }

    public static void write(Villager actor, ValueOutput output) {
        if (matches(actor) && PolymerEntity.get(actor) instanceof LbcFakePlayer fake)
            output.store(SAVE_KEY, Snapshot.CODEC, fake.snapshot);
    }

    private static void capture(Villager actor, Component name, ServerPlayer activator) {
        if (!(actor.level() instanceof ServerLevel level)) return;
        var players = level.getServer().getPlayerList();
        UUID expected = name != null && name.getStyle().getHoverEvent() instanceof HoverEvent.ShowEntity hover
                && hover.entity().type == EntityTypes.PLAYER ? hover.entity().uuid : null;
        ServerPlayer source = expected == null ? null : players.getPlayer(expected);
        if (expected == null && name != null) {
            String label = name.getString();
            for (var player : level.players()) {
                if (label.equals(player.getScoreboardName()) || label.equals(player.getDisplayName().getString())) {
                    source = player;
                    break;
                }
            }
        }
        if (expected == null && source == null) source = activator;
        if (source != null) name = LbcNameplates.frozen(source.getDisplayName());
        if (name != null) actor.setCustomName(name);
        install(actor, source == null ? Snapshot.absent(expected) : Snapshot.capture(source));
    }

    private static void install(Villager actor, Snapshot snapshot) {
        actor.setCustomNameVisible(true);
        PolymerEntityUtils.setPolymerEntity(actor, new LbcFakePlayer(actor, snapshot));
    }

    public static ItemStack renderEquipment(LivingEntity actor, EquipmentSlot slot, ItemStack ordinary) {
        return actor instanceof Villager && PolymerEntity.get(actor) instanceof LbcFakePlayer fake
            ? fake.visible.getOrDefault(slot, ItemStack.EMPTY) : ordinary;
    }

    /** Called by the native entity tracker. Inventory is copied, never installed as Villager AI equipment. */
    public static ClientboundSetEquipmentPacket updateEquipment(Entity actor) {
        if (!(actor instanceof Villager) || !(actor.level() instanceof ServerLevel level)
                || !(PolymerEntity.get(actor) instanceof LbcFakePlayer fake) || fake.sourceId == null) return null;
        var source = level.getServer().getPlayerList().getPlayer(fake.sourceId);
        if (source == null || source.isRemoved()) return null;
        var next = fake.snapshot.updateInventory(source.getInventory());
        if (next == fake.snapshot) return null;
        fake.snapshot = next;
        var changes = new ArrayList<Pair<EquipmentSlot, ItemStack>>();
        fake.refreshEquipment(EquipmentSlot.MAINHAND, next.slot(next.selected()), changes);
        Inventory.EQUIPMENT_SLOT_MAPPING.forEach((index, slot) -> fake.refreshEquipment(slot, next.slot(index), changes));
        return changes.isEmpty() ? null : new ClientboundSetEquipmentPacket(actor.getId(), changes);
    }

    private void refreshEquipment(EquipmentSlot slot, ItemStack stack, List<Pair<EquipmentSlot, ItemStack>> changes) {
        if (ItemStack.matches(stack, visible.getOrDefault(slot, ItemStack.EMPTY))) return;
        visible.put(slot, stack);
        changes.add(Pair.of(slot, stack));
    }

    // Package-visible snapshot query for server-side checks, never an accessible Mob container.
    public static ItemStack copiedSlot(Villager actor, int index) {
        return PolymerEntity.get(actor) instanceof LbcFakePlayer fake ? fake.snapshot.slot(index).copy() : ItemStack.EMPTY;
    }

    public static int selectedSlot(Villager actor) {
        return PolymerEntity.get(actor) instanceof LbcFakePlayer fake ? fake.snapshot.selected() : -1;
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) {
        return EntityTypes.MANNEQUIN;
    }

    @Override
    public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data, ServerPlayer viewer, boolean initial) {
        // Villager-only flags share numeric IDs with the client's avatar fields.
        data.removeIf(value -> value.id() >= Model.FIRST_AVATAR_FIELD);
        if (initial) data.addAll(metadata);
    }

    @Override
    public List<Pair<EquipmentSlot, ItemStack>> getPolymerVisibleEquipment(
            List<Pair<EquipmentSlot, ItemStack>> items, ServerPlayer viewer) {
        return items.stream().map(pair -> Pair.of(pair.getFirst(),
            visible.getOrDefault(pair.getFirst(), ItemStack.EMPTY))).toList();
    }
}
