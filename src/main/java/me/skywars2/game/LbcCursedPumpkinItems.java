package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;

/** Native immutable storage has no 256-slot limit; Polymer owns projection and exact item recovery. */
public final class LbcCursedPumpkinItems {
    public static final DataComponentType<List<ItemStackTemplate>> CONTENTS=Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,Identifier.parse("skywars2:cursed_pumpkin_contents"),
            DataComponentType.<List<ItemStackTemplate>>builder().persistent(ItemStackTemplate.CODEC.listOf()
                    .xmap(List::copyOf,List::copyOf)).cacheEncoding().build());
    public static final DataComponentType<Long> PROGRESS=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.parse("skywars2:cursed_pumpkin_progress"),DataComponentType.<Long>builder().persistent(com.mojang.serialization.Codec.LONG).build());
    public static final Item CURSE=register("cursed_pumpkin",false);
    public static final Item SWORD=register("pumpkin_king_sword",true);
    private LbcCursedPumpkinItems(){}
    public static void register(){PolymerComponent.registerDataComponent(CONTENTS,PROGRESS);}
    public static boolean curse(ItemStack stack){return ItemData.intValue(stack,"pumpkin_curse",0)==1;}
    public static boolean sword(ItemStack stack){return ItemData.intValue(stack,"pumpkin_king",0)==1;}
    private static Item register(String id,boolean sword){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+id));
        var model=Identifier.parse(sword?"lbc:pumpkin_sword":"minecraft:carved_pumpkin");
        var flags=new CompoundTag();flags.putInt(sword?"pumpkin_king":"pumpkin_curse",1);
        if(sword){flags.putInt("transformable",1);flags.putInt("sword",1);}
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}
                .setId(key).overrideDescription(sword?"pumpkin_king_sword":"pumpkin_curse").component(DataComponents.ITEM_MODEL,model).component(DataComponents.CUSTOM_DATA,CustomData.of(flags))
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse(sword?"lbc:tier_a":"lbc:tier_ff"));
        if(sword){
            properties.sword(ToolMaterial.WOOD,7,-2.4f);
            properties.component(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE,new AttributeModifier(Identifier.withDefaultNamespace("base_attack_damage"),7,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED,new AttributeModifier(Identifier.withDefaultNamespace("base_attack_speed"),-2.4,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build());
        }else properties.component(CONTENTS,List.of());
        return Registry.register(BuiltInRegistries.ITEM,key,new NativeItem(properties,sword?Items.WOODEN_SWORD:Items.REPEATING_COMMAND_BLOCK));
    }
    private static final class NativeItem extends Item implements PolymerItem{
        private final Item proxy;
        NativeItem(Properties properties,Item proxy){super(properties);this.proxy=proxy;}
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context){return proxy;}
    }
}
