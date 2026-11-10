package me.skywars2.game;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;

/** Статус наблюдателя независим от gamemode: игровые механики проверяют его явно. */
public final class Spectators {
    private static final EquipmentSlot[] CAMERA_EQUIPMENT = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private record EffectSnapshot(MobEffectInstance effect, int sentAt) {}
    private static final class View {
        Entity target;
        SimpleGui inventory;
        boolean inventoryChanged;
        final Map<Holder<MobEffect>, EffectSnapshot> effects = new LinkedHashMap<>();
    }
    private final GameController game;
    private final Map<UUID, View> viewers = new LinkedHashMap<>();

    public Spectators(GameController game) { this.game = game; }
    Set<UUID> checkpointMembers() { return Set.copyOf(viewers.keySet()); }
    /** Membership phase precedes native body/effect restoration and camera reattachment. */
    void restoreMembers(Set<UUID> saved, Set<UUID> capturedOnline) {
        clear();
        for (var player : game.server().getPlayerList().getPlayers())
            if (saved.contains(player.getUUID()) || !capturedOnline.contains(player.getUUID())) enter(player);
    }
    public boolean contains(ServerPlayer player) { return viewers.containsKey(player.getUUID()); }
    public boolean attached(ServerPlayer player) {
        View view = viewers.get(player.getUUID());
        return view != null && view.target != null;
    }
    public int mirroredEffects(ServerPlayer player) {
        View view = viewers.get(player.getUUID());
        return view == null ? 0 : view.effects.size();
    }

    public void enter(ServerPlayer player) {
        viewers.computeIfAbsent(player.getUUID(), ignored -> new View());
        detach(player);
    }

    public void remove(ServerPlayer player) {
        View view = viewers.remove(player.getUUID());
        if (view == null) return;
        player.closeContainer();
        clearEffects(player, view);
        player.setCamera(player);
        player.removeAllEffects();
        player.getInventory().clearContent();
        player.setInvisible(false);
        player.noPhysics = false;
    }

    public void clear() {
        for (UUID id : new ArrayList<>(viewers.keySet())) {
            ServerPlayer player = game.server().getPlayerList().getPlayer(id);
            if (player != null) remove(player);
        }
        viewers.clear();
    }

    public void detach(ServerPlayer player) {
        View view = viewers.get(player.getUUID());
        if (view == null) return;
        // Keep native death/respawn intact. Healing this body makes PERFORM_RESPAWN
        // fail its health check while the listener still rejects every gameplay packet.
        if (player.isDeadOrDying() || ((MatchPlayerCheckpoint.SimulatedAcknowledgement)player.connection).skywars2$waitingForRespawn()) return;
        clearEffects(player, view);
        view.target = null;
        view.inventory = null;
        player.closeContainer();
        player.setCamera(player);
        player.setGameMode(GameType.ADVENTURE);
        player.removeAllEffects();
        player.setRemainingFireTicks(0);
        player.setTicksFrozen(0);
        var max = player.getAttribute(Attributes.MAX_HEALTH);
        if (max != null) max.setBaseValue(20);
        player.setHealth(player.getMaxHealth());
        player.setAbsorptionAmount(0);
        player.setExperienceLevels(0);
        player.setExperiencePoints(0);
        player.getFoodData().setFoodLevel(20);
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.getAbilities().invulnerable = true;
        player.onUpdateAbilities();
        player.setInvisible(true);
        player.noPhysics = true;
        giveTools(player);
    }

    private void giveTools(ServerPlayer player) {
        player.getInventory().clearContent();
        ItemStack compass = new ItemStack(Items.COMPASS);
        compass.set(DataComponents.CUSTOM_NAME, Component.translatable("menu")
                .withStyle(s -> s.withColor(ChatFormatting.YELLOW).withItalic(false)));
        ItemStack targets = new ItemStack(Items.PLAYER_HEAD);
        targets.set(DataComponents.CUSTOM_NAME, Component.translatable("skywars2.spectator.players")
                .withStyle(s -> s.withColor(ChatFormatting.YELLOW).withItalic(false)));
        player.getInventory().setItem(4, compass);
        player.getInventory().setItem(6, targets);
        player.getInventory().setSelectedSlot(4);
        player.connection.send(new ClientboundSetHeldSlotPacket(4));
        player.inventoryMenu.sendAllDataToRemote();
    }

    /** ПКМ переводит только зарегистрированного наблюдателя в настоящую камеру spectator. */
    public void watch(ServerPlayer player, Entity target) {
        View view = viewers.get(player.getUUID());
        if (view == null || target == player || !target.isAlive() || target.isRemoved()
                || target.level() != game.maps().level()
                || target instanceof ServerPlayer other && (!game.allowDamage(other) || contains(other))) return;
        player.closeContainer();
        clearEffects(player, view);
        player.getInventory().clearContent();
        view.inventory = null;
        view.target = target;
        player.setGameMode(GameType.SPECTATOR);
        player.setCamera(target);
        mirror(player, target);
    }

    public void openTargets(ServerPlayer player) {
        if (!contains(player)) return;
        detach(player);
        // Зарегистрированный наблюдатель может ещё иметь participant-запись, но целью меню быть не должен.
        var alive = game.alivePlayers().stream().filter(game::allowDamage).toList();
        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
        gui.setTitle(Component.translatable("skywars2.spectator.players"));
        gui.setLockPlayerInventory(true);
        for (int i = 0; i < Math.min(27, alive.size()); i++) {
            UUID id = alive.get(i).getUUID();
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(alive.get(i).getGameProfile()));
            head.set(DataComponents.CUSTOM_NAME, alive.get(i).getDisplayName().copy().withStyle(s -> s.withItalic(false)));
            gui.setSlot(i, new GuiElementBuilder(head).setCallback((slot, type, action, menu) -> {
                if (action == ContainerInput.PICKUP_ALL || type == ClickType.MOUSE_DOUBLE_CLICK || !contains(player)) return;
                ServerPlayer target = game.server().getPlayerList().getPlayer(id);
                if (target == null || !game.allowDamage(target)) { openTargets(player); return; }
                player.closeContainer();
                player.teleportTo(target.level(), target.getX(), target.getY(), target.getZ(), Set.of(),
                        player.getYRot(), player.getXRot(), true);
            }));
        }
        gui.open();
    }

    /** Дополнительный просмотр /sw inventory доступен и если клиент не открывает E в spectator. */
    public void openInventory(ServerPlayer player) {
        View view = viewers.get(player.getUUID());
        if (view == null || !(view.target instanceof ServerPlayer target)) return;
        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x6, player, false);
        gui.setTitle(Component.translatable("skywars2.spectator.inventory", target.getDisplayName()));
        gui.setLockPlayerInventory(true);
        view.inventory = gui;
        fillInventory(gui, target);
        gui.open();
    }

    private static void fillInventory(SimpleGui gui, ServerPlayer target) {
        for (int i = 0; i < target.getInventory().getContainerSize(); i++)
            gui.setSlot(i, new GuiElementBuilder(target.getInventory().getItem(i).copy()));
    }

    public void tick() {
        // Обходим только наблюдателей; копируем изменившиеся стаки, не весь мир и не все профили.
        for (var entry : viewers.entrySet()) {
            ServerPlayer player = game.server().getPlayerList().getPlayer(entry.getKey());
            if (player == null) continue;
            View view = entry.getValue();
            if (view.target != null) {
                if (!view.target.isAlive() || view.target.isRemoved() || player.getCamera() != view.target
                        || view.target.level() != game.maps().level()
                        || view.target instanceof ServerPlayer other && !game.allowDamage(other)) {
                    detach(player);
                } else {
                    mirror(player, view.target);
                    if (view.inventoryChanged && view.inventory != null && eu.pb4.sgui.api.SguiUtils.getCurrentGui(player) == view.inventory
                            && view.target instanceof ServerPlayer target) fillInventory(view.inventory, target);
                }
            } else if (player.gameMode() != GameType.ADVENTURE) detach(player);
            player.setInvisible(true);
            player.getAbilities().invulnerable = true;
        }
    }

    private void mirror(ServerPlayer viewer, Entity entity) {
        if (!(entity instanceof LivingEntity target)) return;
        View view = viewers.get(viewer.getUUID());
        view.inventoryChanged = false;
        if (target instanceof ServerPlayer player) {
            var source = player.getInventory();
            var dest = viewer.getInventory();
            for (int i = 0; i < dest.getContainerSize(); i++)
                if (!ItemStack.matches(dest.getItem(i), source.getItem(i))) {
                    dest.setItem(i, source.getItem(i).copy());
                    view.inventoryChanged = true;
                }
            if (dest.getSelectedSlot() != source.getSelectedSlot()) {
                dest.setSelectedSlot(source.getSelectedSlot());
                viewer.connection.send(new ClientboundSetHeldSlotPacket(dest.getSelectedSlot()));
            }
            if (viewer.getFoodData().getFoodLevel() != player.getFoodData().getFoodLevel())
                viewer.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel());
            if (viewer.getFoodData().getSaturationLevel() != player.getFoodData().getSaturationLevel())
                viewer.getFoodData().setSaturation(player.getFoodData().getSaturationLevel());
            if (viewer.experienceLevel != player.experienceLevel || viewer.experienceProgress != player.experienceProgress) {
                viewer.setExperienceLevels(player.experienceLevel);
                viewer.setExperiencePoints((int) (player.experienceProgress * player.getXpNeededForNextLevel()));
            }
            if (view.inventoryChanged) viewer.inventoryMenu.broadcastChanges();
        } else {
            // У моба нет PlayerInventory, но его руки и броня тоже видны из камеры.
            for (var slot : CAMERA_EQUIPMENT) {
                if (!ItemStack.matches(viewer.getItemBySlot(slot), target.getItemBySlot(slot))) {
                    viewer.setItemSlot(slot, target.getItemBySlot(slot).copy());
                    view.inventoryChanged = true;
                }
            }
            if (view.inventoryChanged) viewer.inventoryMenu.broadcastChanges();
        }
        var max = viewer.getAttribute(Attributes.MAX_HEALTH);
        if (max != null && max.getBaseValue() != target.getMaxHealth()) max.setBaseValue(target.getMaxHealth());
        if (viewer.getHealth() != target.getHealth()) viewer.setHealth(target.getHealth());
        if (viewer.getAbsorptionAmount() != target.getAbsorptionAmount()) viewer.setAbsorptionAmount(target.getAbsorptionAmount());
        if (viewer.getRemainingFireTicks() != target.getRemainingFireTicks()) viewer.setRemainingFireTicks(target.getRemainingFireTicks());
        if (viewer.getTicksFrozen() != target.getTicksFrozen()) viewer.setTicksFrozen(target.getTicksFrozen());
        if (viewer.getAirSupply() != target.getAirSupply()) viewer.setAirSupply(target.getAirSupply());
        // Эффекты зеркалим только пакетами для HUD/камеры: серверный Raid Omen и другие callbacks наблюдателю не нужны.
        var iter = view.effects.keySet().iterator();
        while (iter.hasNext()) {
            var type = iter.next();
            if (!target.hasEffect(type)) { removeEffectPackets(viewer, view, type); iter.remove(); }
        }
        int now = game.server().getTickCount();
        for (var effect : target.getActiveEffects()) {
            var snapshot = view.effects.get(effect.getEffect());
            var old = snapshot == null ? null : snapshot.effect();
            if (old == null || old.getAmplifier() != effect.getAmplifier()
                    || old.isInfiniteDuration() != effect.isInfiniteDuration()
                    || !effect.isInfiniteDuration() && Math.abs(old.getDuration() - (now - snapshot.sentAt()) - effect.getDuration()) > 1
                    || old.isVisible() != effect.isVisible() || old.isAmbient() != effect.isAmbient()
                    || old.showIcon() != effect.showIcon()) {
                viewer.connection.send(new ClientboundUpdateMobEffectPacket(viewer.getId(), effect, false));
                viewer.connection.send(new ClientboundUpdateMobEffectPacket(target.getId(), effect, false));
                view.effects.put(effect.getEffect(), new EffectSnapshot(new MobEffectInstance(effect), now));
            }
        }
    }

    private static void removeEffectPackets(ServerPlayer viewer, View view, Holder<MobEffect> effect) {
        viewer.connection.send(new ClientboundRemoveMobEffectPacket(viewer.getId(), effect));
        // При выходе из камеры действующий эффект цели должен остаться видимым на её сущности.
        if (view.target != null && (!(view.target instanceof LivingEntity living) || !living.hasEffect(effect)))
            viewer.connection.send(new ClientboundRemoveMobEffectPacket(view.target.getId(), effect));
    }

    private static void clearEffects(ServerPlayer viewer, View view) {
        for (var effect : view.effects.keySet()) removeEffectPackets(viewer, view, effect);
        view.effects.clear();
    }
}
