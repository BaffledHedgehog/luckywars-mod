package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;

/** Exact November source carriers: the colored blades retain ordinary shield blocking. */
public final class LbcNovemberLightsaberItems {
    public static final Item SELECTOR = create("lightsaber_empty", "lightsaber_no_color", false);
    public static final Item JEDI = create("lightsaber_jedi", "lightsaber_blue", true);
    public static final Item SITH = create("lightsaber_sith", "lightsaber_red", true);
    private LbcNovemberLightsaberItems() { }
    public static void register() { }

    private static Item create(String model, String translation, boolean blade) {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:" + model));
        var flags = new CompoundTag();
        flags.putInt("transformable", 1); flags.putInt("sword", 1);
        flags.putInt(blade ? "lightsaber" : "lightsaberrc", 1);
        if (!blade) flags.putInt("magicwand", 1);
        var properties = new Item.Properties() {
            @Override public Identifier effectiveModel() { return Identifier.parse("lbc:" + model); }
        }.setId(key).durability(blade ? 336 : 25).overrideDescription(translation)
                .component(DataComponents.ITEM_MODEL, Identifier.parse("lbc:" + model))
                .component(DataComponents.CUSTOM_DATA, CustomData.of(flags))
                .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Identifier.withDefaultNamespace("base_attack_damage"),
                                blade ? 7 : 0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED, new AttributeModifier(Identifier.withDefaultNamespace("base_attack_speed"),
                                -1.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build())
                .component(DataComponents.CUSTOM_NAME, Component.translatable(translation).withStyle(s -> s.withColor(ChatFormatting.AQUA).withItalic(false)))
                .component(DataComponents.LORE, new ItemLore(List.of(
                        Component.translatable("type").withStyle(s -> s.withColor(ChatFormatting.AQUA).withItalic(false))
                                .append(Component.translatable("sword").withStyle(s -> s.withColor(ChatFormatting.AQUA).withItalic(false))),
                        Component.translatable(translation + ".lore.1").withStyle(s -> s.withColor(ChatFormatting.WHITE).withItalic(false)),
                        Component.translatable(translation + ".lore.2").withStyle(s -> s.withColor(ChatFormatting.WHITE).withItalic(false)))));
        if (blade) properties
                .component(DataComponents.BANNER_PATTERNS, net.minecraft.world.level.block.entity.BannerPatternLayers.EMPTY)
                .repairable(net.minecraft.tags.ItemTags.WOODEN_TOOL_MATERIALS)
                .equippableUnswappable(EquipmentSlot.OFFHAND)
                .delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(.25f, 1,
                        List.of(new BlocksAttacks.DamageReduction(90, Optional.empty(), 0, 1)),
                        new BlocksAttacks.ItemDamageFunction(3, 1, 1),
                        Optional.of(context.getOrThrow(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)),
                        Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK), Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BREAK)))
                .component(DataComponents.BREAK_SOUND, net.minecraft.sounds.SoundEvents.SHIELD_BREAK);
        return Registry.register(BuiltInRegistries.ITEM, key,
                new SimplePolymerItem(properties, blade ? Items.SHIELD : Items.CARROT_ON_A_STICK, true));
    }

    public static boolean selector(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(SELECTOR) || LbcFartus.data(stack).getIntOr("lightsaberrc", 0) == 1);
    }
    public static boolean blade(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(JEDI) || stack.is(SITH) || LbcFartus.data(stack).getIntOr("lightsaber", 0) == 1);
    }
}
