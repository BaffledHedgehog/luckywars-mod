package me.skywars2.game.players.teams;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.skywars2.game.GameController;
import me.skywars2.game.match.checkpoints.MatchPlayerCheckpoint;

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
        boolean vanilla;
        net.minecraft.world.entity.Interaction returnProxy;
        SimpleGui inventory;
        boolean inventoryChanged;
        SimpleGui containerGui;
        net.minecraft.world.Container container;
        net.minecraft.core.BlockPos containerPos;
        net.minecraft.server.level.ServerLevel containerLevel;
        net.minecraft.world.level.block.entity.BlockEntity containerEntity;
        Entity containerBody;
        java.util.List<net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity> randomContainers=java.util.List.of();
        final Map<Holder<MobEffect>, EffectSnapshot> effects = new LinkedHashMap<>();
    }
    private final GameController game;
    private final Map<UUID, View> viewers = new LinkedHashMap<>();

    public Spectators(GameController game) { this.game = game; }
    public Set<UUID> checkpointMembers() { return Set.copyOf(viewers.keySet()); }
    /** Membership phase precedes native body/effect restoration and camera reattachment. */
    public void restoreMembers(Set<UUID> saved, Set<UUID> capturedOnline) {
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
        me.skywars2.game.gui.hud.TabPresentation.refreshNames(game.server(),java.util.List.of(player));
    }

    public void remove(ServerPlayer player) {
        View view = viewers.remove(player.getUUID());
        if (view == null) return;
        player.closeContainer();
        clearEffects(player, view);
        clearReturnProxy(player, view);
        player.setCamera(player);
        player.removeAllEffects();
        player.getInventory().clearContent();
        player.setInvisible(false);
        player.noPhysics = false;
        me.skywars2.game.gui.hud.TabPresentation.refreshNames(game.server(),java.util.List.of(player));
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
        clearReturnProxy(player, view);
        view.vanilla = false;
        view.target = null;
        view.inventory = null;
        clearContainer(view);
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
        ItemStack toggle = new ItemStack(Items.ENDER_EYE);
        toggle.set(DataComponents.CUSTOM_NAME, Component.translatable("skywars2.spectator.vanilla")
                .withStyle(s -> s.withColor(ChatFormatting.AQUA).withItalic(false)));
        player.getInventory().setItem(8, toggle);
        player.getInventory().setSelectedSlot(4);
        player.connection.send(new ClientboundSetHeldSlotPacket(4));
        player.inventoryMenu.sendAllDataToRemote();
    }

    /** Observer controls are admitted before dimension/item restrictions. */
    public boolean useTool(ServerPlayer player, net.minecraft.world.InteractionHand hand) {
        if (!contains(player)) return false;
        var held = player.getItemInHand(hand);
        if (held.is(Items.COMPASS)) game.menus().open(player);
        else if (held.is(Items.PLAYER_HEAD)) openTargets(player);
        else if (held.is(Items.ENDER_EYE)) toggleVanilla(player);
        return true;
    }

    public boolean vanilla(ServerPlayer player) {
        var view = viewers.get(player.getUUID());
        return view != null && view.vanilla;
    }

    public void toggleVanilla(ServerPlayer player) {
        var view = viewers.get(player.getUUID());
        if (view == null) return;
        if (view.vanilla) { detach(player); return; }
        detach(player);
        if (player.isDeadOrDying()) return;
        view.vanilla = true;
        player.getInventory().clearContent();
        player.setGameMode(GameType.SPECTATOR);
        player.setCamera(player);
        updateReturnProxy(player, view);
        player.sendSystemMessage(Component.translatable("skywars2.spectator.observer"));
    }

    /** Private packet-only hitbox: no physical world entity, AI or save entry. */
    private void updateReturnProxy(ServerPlayer player, View view) {
        var proxy = view.returnProxy;
        boolean spawn = proxy == null || proxy.level() != player.level();
        if (spawn) {
            clearReturnProxy(player, view);
            proxy = new net.minecraft.world.entity.Interaction(net.minecraft.world.entity.EntityTypes.INTERACTION, player.level());
            proxy.setWidth(1.5f); proxy.setHeight(1.5f); proxy.setResponse(true);
            view.returnProxy = proxy;
        }
        proxy.setPos(player.getEyePosition().add(player.getLookAngle().scale(1.5)).add(0, -.75, 0));
        if (spawn) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(proxy.getId(), proxy.getUUID(),
                    proxy.getX(), proxy.getY(), proxy.getZ(), 0, 0, proxy.getType(), 0, net.minecraft.world.phys.Vec3.ZERO, 0));
            var data = proxy.getEntityData().getNonDefaultValues();
            if (data != null && !data.isEmpty()) player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(proxy.getId(), data));
        } else player.connection.send(net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket.of(proxy));
    }

    private void clearReturnProxy(ServerPlayer player, View view) {
        if (view.returnProxy == null) return;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket(view.returnProxy.getId()));
        view.returnProxy = null;
    }

    public boolean returnFromVanilla(ServerPlayer player, int entityId) {
        var view = viewers.get(player.getUUID());
        if (view == null || !view.vanilla || view.returnProxy == null || view.returnProxy.getId() != entityId) return false;
        detach(player);
        return true;
    }

    /** Legacy free-camera actionbar follows the nearest live player in this dimension. */
    public Component nearestHud(ServerPlayer viewer) {
        if (!contains(viewer)) return Component.empty();
        var target = game.alivePlayers().stream().filter(other -> other != viewer && other.level() == viewer.level()
                        && !contains(other) && other.isAlive() && game.allowDamage(other))
                .min(java.util.Comparator.comparingDouble(viewer::distanceToSqr)).orElse(null);
        if (target == null) return Component.empty();
        return target.getDisplayName().copy().append(" ")
                .append(Component.literal(Integer.toString((int)Math.ceil(target.getHealth() + target.getAbsorptionAmount())))
                        .withStyle(ChatFormatting.RED)).append(Component.literal(" ♥").withStyle(ChatFormatting.RED));
    }

    /** ПКМ переводит только зарегистрированного наблюдателя в настоящую камеру spectator. */
    public void watch(ServerPlayer player, Entity target) {
        View view = viewers.get(player.getUUID());
        if (view == null || target == player || !target.isAlive() || target.isRemoved()
                || target.level() != player.level()
                || target instanceof ServerPlayer other && (!game.allowDamage(other) || contains(other))) return;
        player.closeContainer();
        clearEffects(player, view);
        clearReturnProxy(player, view);
        view.vanilla = false;
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

    /** Inspect copies without startOpen/stopOpen, opener counts, loot rolls or block events. */
    public boolean openContainer(ServerPlayer player,net.minecraft.core.BlockPos pos){
        var view=viewers.get(player.getUUID());var level=player.level();
        if(view==null||!player.isWithinBlockInteractionRange(pos,1)||!level.hasChunkAt(pos))return false;
        var state=level.getBlockState(pos);var provider=state.getMenuProvider(level,pos);
        boolean ender=state.getBlock() instanceof net.minecraft.world.level.block.EnderChestBlock;
        if(provider==null&&!ender)return false;
        net.minecraft.world.Container contents;
        if(state.getBlock() instanceof net.minecraft.world.level.block.EnderChestBlock)contents=player.getEnderChestInventory();
        else if(state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock chest)
            contents=net.minecraft.world.level.block.ChestBlock.getContainer(chest,state,level,pos,true);
        else contents=level.getBlockEntity(pos) instanceof net.minecraft.world.Container container?container:null;
        if(contents==null||contents.getContainerSize()<1)return false;
        int rows=Math.clamp((contents.getContainerSize()+8)/9,1,6);
        var types=java.util.List.of(MenuType.GENERIC_9x1,MenuType.GENERIC_9x2,MenuType.GENERIC_9x3,MenuType.GENERIC_9x4,MenuType.GENERIC_9x5,MenuType.GENERIC_9x6);
        var gui=new SimpleGui(types.get(rows-1),player,false);gui.setTitle(ender?Component.translatable("container.enderchest"):provider.getDisplayName());gui.setLockPlayerInventory(true);
        view.containerGui=gui;view.container=contents;view.containerPos=pos.immutable();view.containerLevel=level;
        view.containerEntity=level.getBlockEntity(pos);view.containerBody=null;
        var randoms=new java.util.ArrayList<net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity>();
        if(view.containerEntity instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity random)randoms.add(random);
        if(state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock
                &&state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE)!=net.minecraft.world.level.block.state.properties.ChestType.SINGLE){
            var other=level.getBlockEntity(pos.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(state)));
            if(other instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity random)randoms.add(random);
        }
        view.randomContainers=java.util.List.copyOf(randoms);
        fillContainer(view);gui.open();return true;
    }
    public boolean openContainer(ServerPlayer player,Entity body){
        var view=viewers.get(player.getUUID());
        if(view==null||body.isRemoved()||body.level()!=player.level()
                ||!player.isWithinEntityInteractionRange(body.getBoundingBox(),1)
                ||!(body instanceof net.minecraft.world.Container contents)||!(body instanceof net.minecraft.world.MenuProvider provider))return false;
        int rows=Math.clamp((contents.getContainerSize()+8)/9,1,6);
        var types=java.util.List.of(MenuType.GENERIC_9x1,MenuType.GENERIC_9x2,MenuType.GENERIC_9x3,MenuType.GENERIC_9x4,MenuType.GENERIC_9x5,MenuType.GENERIC_9x6);
        var gui=new SimpleGui(types.get(rows-1),player,false);gui.setTitle(provider.getDisplayName());gui.setLockPlayerInventory(true);
        view.containerGui=gui;view.container=contents;view.containerBody=body;view.containerLevel=player.level();
        view.containerEntity=null;view.containerPos=null;view.randomContainers=java.util.List.of();
        fillContainer(view);gui.open();return true;
    }
    private static void clearContainer(View view){
        view.containerGui=null;view.container=null;view.containerBody=null;view.containerEntity=null;
        view.containerLevel=null;view.containerPos=null;view.randomContainers=java.util.List.of();
    }
    private static void fillContainer(View view){
        boolean unopened=view.containerBody instanceof net.minecraft.world.entity.vehicle.ContainerEntity vehicle
                ?vehicle.getContainerLootTable()!=null:view.randomContainers.stream().anyMatch(random->random.getLootTable()!=null);
        for(int i=0;i<view.container.getContainerSize()&&i<view.containerGui.getSize();i++){
            // Spectators must not initialize an unopened random-loot container.
            var stack=unopened?ItemStack.EMPTY:view.container.getItem(i);
            var previous=view.containerGui.getGuiElement(i);
            if(previous==null||!ItemStack.matches(previous.getItemStack(),stack))
                view.containerGui.setSlot(i,new GuiElementBuilder(stack.copy()));
        }
    }

    public void tick() {
        // Обходим только наблюдателей; копируем изменившиеся стаки, не весь мир и не все профили.
        for (var entry : viewers.entrySet()) {
            ServerPlayer player = game.server().getPlayerList().getPlayer(entry.getKey());
            if (player == null) continue;
            View view = entry.getValue();
            if(view.containerGui!=null){
                if(eu.pb4.sgui.api.SguiUtils.getCurrentGui(player)!=view.containerGui){clearContainer(view);}
                else if(player.level()!=view.containerLevel||(view.containerBody!=null
                        ?view.containerBody.isRemoved()||!player.isWithinEntityInteractionRange(view.containerBody.getBoundingBox(),1)
                        :!player.isWithinBlockInteractionRange(view.containerPos,1)||!player.level().hasChunkAt(view.containerPos)
                            ||player.level().getBlockEntity(view.containerPos)!=view.containerEntity)){
                    view.containerGui.close();clearContainer(view);
                }else fillContainer(view);
            }
            if (view.target != null) {
                if (!view.target.isAlive() || view.target.isRemoved() || player.getCamera() != view.target
                        || view.target.level() != player.level()
                        || view.target instanceof ServerPlayer other && !game.allowDamage(other)) {
                    detach(player);
                } else {
                    mirror(player, view.target);
                    if (view.inventoryChanged && view.inventory != null && eu.pb4.sgui.api.SguiUtils.getCurrentGui(player) == view.inventory
                            && view.target instanceof ServerPlayer target) fillInventory(view.inventory, target);
                }
            } else if (view.vanilla) {
                if (player.gameMode() != GameType.SPECTATOR || player.getCamera() != player) detach(player);
                else updateReturnProxy(player, view);
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
