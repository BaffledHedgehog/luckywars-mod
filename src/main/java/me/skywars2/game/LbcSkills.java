package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.world.item.component.TooltipDisplay;
import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import com.mojang.datafixers.util.Pair;

import java.util.ArrayList;
import java.util.List;

/** Native handlers for the small set of migrated LBC skills. */
public final class LbcSkills {
    private static final String UNKNOWN_ITEM = "????????????????????????????";
    private static boolean registered;
    private static final EquipmentSlot[] EQUIPMENT_SLOTS = EquipmentSlot.values();
    private static final List<String> ARTIFACT_KEYS = List.of(
            "artifact_1", "artifact_2", "artifact_3", "artifact_4", "artifact_5", "artifact_6", "artifact_7", "artifact_8",
            "artifact_9", "artifact_10", "artifact_11", "artifact_12", "artifact_13", "artifact_14", "artifact_15", "artifact_16",
            "artifact_17", "artifact_18", "artifact_19", "artifact_20", "artifact_21", "artifact_22", "artifact_23");
    private LbcSkills() { }

    /** Global Polymer projection also covers vanilla stacks, without changing authoritative components. */
    public static void register() {
        if (registered) return;
        registered = true;
        PolymerItemUtils.CONTEXT_ITEM_CHECK.register((stack, context) -> hidesKnowledge(context));
        PolymerItemUtils.ITEM_MODIFICATION_EVENT.register((original, client, context) -> {
            if (client.isEmpty() || !hidesKnowledge(context)) return client;
            // A callback may receive the original vanilla stack: never mutate either supplied reference.
            var projected = client.copy();
            projected.set(DataComponents.ITEM_NAME, Component.literal(UNKNOWN_ITEM));
            projected.set(DataComponents.CUSTOM_NAME, Component.literal(UNKNOWN_ITEM));
            projected.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(UNKNOWN_ITEM))));
            // Polymer has already rendered component tooltips into lore. Keep those components hidden,
            // replace every rendered line, and show only the source modifier's question-mark line.
            var display = projected.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
            display = new TooltipDisplay(false, display.hiddenComponents())
                    .withHidden(DataComponents.LORE, false)
                    .withHidden(DataComponents.ATTRIBUTE_MODIFIERS, true)
                    .withHidden(DataComponents.CAN_BREAK, true)
                    .withHidden(DataComponents.CAN_PLACE_ON, true)
                    .withHidden(DataComponents.DYED_COLOR, true)
                    .withHidden(DataComponents.ENCHANTMENTS, true)
                    .withHidden(DataComponents.JUKEBOX_PLAYABLE, true)
                    .withHidden(DataComponents.STORED_ENCHANTMENTS, true)
                    .withHidden(DataComponents.TRIM, true)
                    .withHidden(DataComponents.UNBREAKABLE, true);
            projected.set(DataComponents.TOOLTIP_DISPLAY, display);
            return projected;
        });
    }

    private static boolean hidesKnowledge(PacketContext context) {
        var player = PolymerCommonUtils.getPlayer(context);
        var game = SkyWars2.game();
        // Packet encoding may run on Netty; the owner exposes only its published cosmetic snapshot.
        return player != null && game != null && game.lbc().hidesKnowledge(player);
    }

    /** Косметический доступ получателя: наблюдатели также могут смотреть свои выбранные билды. */
    public static boolean canReadArtifacts(ServerPlayer player) {
        var game = SkyWars2.game();
        return player != null && game != null && game.lbc().readsArtifacts(player);
    }

    /** Polymer строит отдельный tooltip для получателя, не меняя серверный предмет или его NBT. */
    public static void artifactTooltip(List<Component> tooltip, ItemStack stack, PacketContext context) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        CompoundTag data = customData.copyTag();
        int mystical = data.getInt("mystical").orElse(0);
        int type = data.getInt("type").orElse(0);
        int artifact = artifactIndex(mystical, type);
        ItemLore currentLore = stack.get(DataComponents.LORE);
        if (artifact == 0 || currentLore == null || currentLore.lines().size() < 2) return;
        var known = canReadArtifacts(PolymerCommonUtils.getPlayer(context));
        var replacement = known ? Component.translatable(ARTIFACT_KEYS.get(artifact - 1))
                .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withItalic(false))
                : Component.translatable("mystical_artifact.lore")
                .withStyle(style -> style.withColor(ChatFormatting.RED).withItalic(false));
        var original = currentLore.styledLines().get(1);
        // В нативном tooltip могут быть и строки атрибутов/отладки: заменяем только исходную строку lore.
        for (int i = 0; i < tooltip.size(); i++) if (tooltip.get(i).equals(original)) {
            tooltip.set(i, replacement); return;
        }
    }

    /** Повторная отправка неизменённых данных нужна только при смене перка/настройки получателя. */
    public static void refreshViewer(ServerPlayer player) {
        refreshViewer(player, false);
    }

    /** Challenge visibility changes resend every tracked presentation only to this viewer. */
    public static void refreshKnowledgeViewer(ServerPlayer player) {
        refreshViewer(player, true);
    }

    private static void refreshViewer(ServerPlayer player, boolean allItems) {
        player.inventoryMenu.sendAllDataToRemote();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.sendAllDataToRemote();
        // Редкая смена доступа, не тик: полный список загруженных сущностей покрывает и крайние высоты Creative.
        for (var entity : player.level().getAllEntities()) {
            if (entity.isRemoved()) continue;
            if (entity instanceof ItemEntity item && (allItems || item.getItem().is(LbcMagic.MYSTICAL_ARTIFACT))) {
                if (!PlayerLookup.tracking(entity).contains(player)) continue;
                var data = entity.getEntityData().getNonDefaultValues();
                if (data != null) player.connection.send(new ClientboundSetEntityDataPacket(entity.getId(), data));
            } else if (entity instanceof LivingEntity living) {
                ArrayList<Pair<EquipmentSlot, ItemStack>> equipment = null;
                for (var slot : EQUIPMENT_SLOTS) {
                    var stack = living.getItemBySlot(slot);
                    if (!stack.isEmpty() && (allItems || stack.is(LbcMagic.MYSTICAL_ARTIFACT))) {
                        if (equipment == null) equipment = new ArrayList<>();
                        equipment.add(Pair.of(slot, stack));
                    }
                }
                if (equipment != null && PlayerLookup.tracking(entity).contains(player))
                    player.connection.send(new ClientboundSetEquipmentPacket(entity.getId(), equipment));
            }
        }
    }

    private static int artifactIndex(int mystical, int type) {
        if (mystical == 1 && type >= 1 && type <= 8) return type;
        if (mystical == 2 && type >= 1 && type <= 6) return 8 + type;
        if (mystical == 3 && type >= 1 && type <= 3) return 14 + type;
        if (mystical == 4 && type >= 1 && type <= 6) return 17 + type;
        return 0;
    }
}
