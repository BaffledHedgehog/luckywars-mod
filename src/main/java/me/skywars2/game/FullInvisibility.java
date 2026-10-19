package me.skywars2.game;

import com.mojang.datafixers.util.Pair;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;

/** Невидимость III+: скрываем представление, сохраняя настоящий ID игрока и обычные попадания. */
public final class FullInvisibility implements PolymerEntity {
    private record View(boolean hidden, float width, float height,
                        List<SynchedEntityData.DataValue<?>> metadata) { }
    private static final View NORMAL = new View(false, 0, 0, List.of());
    // Кодировщик passenger packet читает готовые overlay по ID, не обращаясь к миру из Netty.
    private static final ConcurrentHashMap<Integer, FullInvisibility> HIDDEN = new ConcurrentHashMap<>();
    private final ServerPlayer owner;
    // Кодировщик Polymer может работать вне server thread: ему доступен только неизменяемый снимок.
    private volatile View view = NORMAL;
    private volatile Set<UUID> cameraViewers = Set.of();

    private FullInvisibility(ServerPlayer owner) { this.owner = owner; }

    public static void register() {
        PolymerEntityUtils.registerPolymerEntityConstructor(EntityTypes.PLAYER,
                player -> player instanceof ServerPlayer serverPlayer ? new FullInvisibility(serverPlayer) : null);
    }

    /** Добавление, ослабление, истечение, молоко и очистка эффектов используют один переход. */
    public static void effectsChanged(LivingEntity entity) {
        if (entity instanceof ServerPlayer player && PolymerEntity.get(player) instanceof FullInvisibility overlay) {
            var effect = player.getEffect(MobEffects.INVISIBILITY);
            overlay.update(effect != null && effect.getAmplifier() >= 2);
        }
    }

    /** Поза и scale меняют клиентскую область попадания; постоянный опрос игроков не нужен. */
    public static void dimensionsChanged(Entity entity) {
        if (entity instanceof ServerPlayer player && PolymerEntity.get(player) instanceof FullInvisibility overlay
                && overlay.view.hidden) overlay.update(true);
    }

    public static void removed(Entity entity) {
        if (entity instanceof ServerPlayer && PolymerEntity.get(entity) instanceof FullInvisibility overlay)
            HIDDEN.remove(entity.getId(), overlay);
    }

    /** Клиент жёстко приводит сборщика к LivingEntity, а внешний proxy — Interaction. */
    public static int pickupCollectorId(int collectorId, PacketContext context) {
        var overlay = HIDDEN.get(collectorId);
        // Отсутствующий сборщик отключает только анимацию полёта к нему. Сам пакет нужен:
        // клиент по нему уменьшает оставшуюся стопку и удаляет подобранные стрелы/предметы.
        return overlay != null && overlay.hiddenFor(PolymerCommonUtils.getPlayer(context)) ? -1 : collectorId;
    }

    /** Interaction имеет другую точку посадки, поэтому снаружи следует серверной позиции без client mount. */
    public static int[] visiblePassengers(int[] passengers, PacketContext context) {
        ServerPlayer viewer = PolymerCommonUtils.getPlayer(context);
        int[] filtered = null;
        int size = 0;
        for (int index = 0; index < passengers.length; index++) {
            var overlay = HIDDEN.get(passengers[index]);
            if (overlay != null && overlay.hiddenFor(viewer)) {
                if (filtered == null) {
                    filtered = passengers.clone();
                    size = index;
                }
            } else if (filtered != null) filtered[size++] = passengers[index];
        }
        return filtered == null ? passengers : java.util.Arrays.copyOf(filtered, size);
    }

    /** Вызывается перед native camera packet: цель должна уже существовать как PLAYER на этом клиенте. */
    public static void cameraChanging(ServerPlayer viewer, Entity camera) {
        Entity previous = viewer.getCamera();
        Entity next = camera == null ? viewer : camera;
        if (previous == next) return;
        cameraViewer(previous, viewer, false);
        if (next != viewer) cameraViewer(next, viewer, true);
    }

    private static void cameraViewer(Entity target, ServerPlayer viewer, boolean attached) {
        if (!(PolymerEntity.get(target) instanceof FullInvisibility overlay)) return;
        var viewers = new HashSet<>(overlay.cameraViewers);
        boolean changed = attached ? viewers.add(viewer.getUUID()) : viewers.remove(viewer.getUUID());
        if (!changed) return;
        overlay.cameraViewers = Set.copyOf(viewers);
        if (overlay.view.hidden) PolymerEntityUtils.refreshEntity(viewer, target);
    }

    private void update(boolean hidden) {
        View previous = view;
        if (hidden) HIDDEN.put(owner.getId(), this);
        else HIDDEN.remove(owner.getId(), this);
        float width = owner.getBbWidth(), height = owner.getBbHeight();
        if (previous.hidden == hidden && (!hidden || previous.width == width && previous.height == height)) return;
        if (hidden) {
            // Нативный шаблон задаёт правильные metadata ID/serializers без копирования player-полей.
            // Interaction доступна для ЛКМ, не имеет модели даже у союзников/наблюдателей;
            // invisible также исключает её из ванильного F3+B. Огонь, glow и частицы не копируются.
            var proxy = new Interaction(EntityTypes.INTERACTION, owner.level());
            proxy.setInvisible(true);
            proxy.setWidth(width);
            proxy.setHeight(height);
            proxy.setResponse(true);
            var metadata = new ArrayList<SynchedEntityData.DataValue<?>>();
            for (var field : PolymerEntityUtils.getDefaultSynchedEntityData(EntityTypes.INTERACTION)) {
                if (field != null) metadata.add(value(proxy, field.getAccessor()));
            }
            // Отправляем и default=1: иначе прежняя ширина/высота сохранится при возврате к единице.
            view = new View(true, width, height, List.copyOf(metadata));
        } else view = NORMAL;

        if (previous.hidden != hidden) {
            // Тип сущности меняется только при пересечении порога III, а не при каждом обновлении зелья.
            // Пересоздание сущности, за которой уже следит камера, сбросило бы client camera reference.
            for (var viewer : List.copyOf(PlayerLookup.tracking(owner))) {
                if (viewer != owner && !cameraViewers.contains(viewer.getUUID()))
                    PolymerEntityUtils.refreshEntity(viewer, owner);
            }
        } else {
            var packet = PolymerEntityUtils.setEntityContext(
                    new ClientboundSetEntityDataPacket(owner.getId(), view.metadata), owner);
            for (var viewer : PlayerLookup.tracking(owner)) if (hiddenFor(viewer)) viewer.connection.send(packet);
        }
    }

    private boolean hiddenFor(ServerPlayer viewer) {
        return view.hidden && viewer != owner && (viewer == null || !cameraViewers.contains(viewer.getUUID()));
    }

    private static <T> SynchedEntityData.DataValue<T> value(Interaction proxy, EntityDataAccessor<T> accessor) {
        return SynchedEntityData.DataValue.create(accessor, proxy.getEntityData().get(accessor));
    }

    @Override public EntityType<?> getPolymerEntityType(PacketContext context) {
        return hiddenFor(PolymerCommonUtils.getPlayer(context)) ? EntityTypes.INTERACTION : EntityTypes.PLAYER;
    }

    @Override public List<Pair<EquipmentSlot, ItemStack>> getPolymerVisibleEquipment(
            List<Pair<EquipmentSlot, ItemStack>> items, ServerPlayer viewer) {
        return hiddenFor(viewer) ? List.of() : items;
    }

    @Override public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data,
                                               ServerPlayer viewer, boolean initial) {
        View snapshot = view;
        if (snapshot.hidden && viewer != owner && (viewer == null || !cameraViewers.contains(viewer.getUUID()))) {
            data.clear();
            data.addAll(snapshot.metadata);
        }
    }

    @Override public boolean isPolymerEntityInteraction(ServerPlayer viewer, InteractionHand hand,
            ItemStack stack, ServerLevel level, InteractionResult result) {
        // Interaction потребляет клиентский ПКМ. Готовый fallback Polymer выполняет обычное
        // использование предмета, если настоящий Player.interactOn вернул PASS (лук/зелье/жезл).
        return hiddenFor(viewer);
    }

    @Override public void onBeforeSpawnPacket(ServerPlayer viewer, Consumer<Packet<?>> sender) {
        // Перенос между измерениями удаляет старый tracker, но эффект и overlay могут сохраниться.
        if (view.hidden) HIDDEN.put(owner.getId(), this);
    }

    @Override public void onEntityTrackerTick(Set<ServerPlayerConnection> listeners) {
        if (!view.hidden || !owner.isPassenger()) return;
        // Нативный tracker пассажира передаёт только поворот. Внешнему proxy нужна позиция
        // настоящего игрока каждый tracker tick; client riding offset к нему не применяется.
        var packet = PolymerEntityUtils.setEntityContext(ClientboundEntityPositionSyncPacket.of(owner), owner);
        for (var listener : listeners) {
            var viewer = listener.getPlayer();
            if (hiddenFor(viewer)) viewer.connection.send(packet);
        }
    }

    // Даже клиент с Polymer должен получать то же невидимое представление, а не настоящий тип.
    @Override public boolean canSynchronizeToPolymerClient(ServerPlayer viewer) { return false; }
}
