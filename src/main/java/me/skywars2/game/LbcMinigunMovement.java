package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.ArrayList;

/** Native hand attributes plus the extra total multiplier needed to lock two miniguns. */
public final class LbcMinigunMovement {
    public static final Identifier MINIGUN_DUAL_LOCK = Identifier.parse("skywars2:laser_minigun_dual_lock");

    private static final ItemAttributeModifiers ATTRIBUTES=buildAttributes();

    private LbcMinigunMovement() { }

    public static ItemAttributeModifiers attributes() { return ATTRIBUTES; }

    private static ItemAttributeModifiers buildAttributes() {
        var builder = ItemAttributeModifiers.builder();
        builder.add(Attributes.ATTACK_SPEED, modifier("minecraft:generijfyjyjted", -1), EquipmentSlotGroup.MAINHAND);
        builder.add(Attributes.ATTACK_SPEED, modifier("minecraft:gjcjgfcjhjgjgjed", -1), EquipmentSlotGroup.OFFHAND);
        builder.add(Attributes.MOVEMENT_SPEED, modifier("skywars2:laser_minigun_movement_mainhand", -.5), EquipmentSlotGroup.MAINHAND);
        builder.add(Attributes.MOVEMENT_SPEED, modifier("skywars2:laser_minigun_movement_offhand", -.5), EquipmentSlotGroup.OFFHAND);
        builder.add(Attributes.JUMP_STRENGTH, modifier("skywars2:laser_minigun_jump_mainhand", -1), EquipmentSlotGroup.MAINHAND);
        builder.add(Attributes.JUMP_STRENGTH, modifier("skywars2:laser_minigun_jump_offhand", -1), EquipmentSlotGroup.OFFHAND);
        return builder.build();
    }

    private static AttributeModifier modifier(String id, double amount) {
        return new AttributeModifier(Identifier.parse(id), amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    /** Upgrade old source/Reforge stacks without replacing their other attributes or presentation. */
    public static void ensureStack(ItemStack stack) {
        if (!LbcLaserMinigun.isMinigun(stack)) return;
        var existing = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        var merged = new ArrayList<>(existing.modifiers());
        for (var required : attributes().modifiers()) {
            if (merged.stream().noneMatch(entry -> entry.modifier().id().equals(required.modifier().id())))
                merged.add(required);
        }
        if (merged.size() != existing.modifiers().size())
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(merged));
    }

    /** Call after inventory/equipment changes; ordinary hand effects belong to native equipment. */
    public static void refresh(ServerPlayer owner) {
        var main = owner.getMainHandItem();
        var off = owner.getOffhandItem();
        ensureStack(main);
        ensureStack(off);
        var movement = owner.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) return;
        boolean dual = owner.isAlive() && LbcLaserMinigun.isMinigun(main) && LbcLaserMinigun.isMinigun(off);
        if (dual) {
            if (!movement.hasModifier(MINIGUN_DUAL_LOCK))
                movement.addTransientModifier(new AttributeModifier(MINIGUN_DUAL_LOCK, -1,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            movement.removeModifier(MINIGUN_DUAL_LOCK);
        }
    }

    public static void clear(ServerPlayer owner) {
        var movement = owner.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) movement.removeModifier(MINIGUN_DUAL_LOCK);
    }
}
