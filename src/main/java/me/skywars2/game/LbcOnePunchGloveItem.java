package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;

/** Native source stack; the shared held-item consumer owns health refresh admission. */
public final class LbcOnePunchGloveItem {
    private static final Identifier BASE_DAMAGE=Identifier.withDefaultNamespace("base_attack_damage");
    public static final Item ITEM=registerItem();
    private LbcOnePunchGloveItem(){}
    public static void register(){}
    private static Item registerItem(){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:one_punch_glove"));
        var data=new CompoundTag();for(var flag:List.of("transformable","sword","one_punch"))data.putInt(flag,1);
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return Identifier.parse("lbc:one_punch_glove");}}
                .setId(key).sword(ToolMaterial.IRON,0,-2.4f).overrideDescription("one_punch_glove")
                .component(DataComponents.MAX_DAMAGE,1)
                // Source set_attributes replaces the complete earlier list with hplvl damage (zero if absent).
                .component(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE,new AttributeModifier(BASE_DAMAGE,0,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build())
                .component(DataComponents.CUSTOM_DATA,CustomData.of(data))
                .component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:one_punch_glove"))
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_b"))
                .component(DataComponents.CUSTOM_NAME,Component.translatable("one_punch_glove").withStyle(s->s.withColor(0x55ffff).withItalic(false)))
                .component(DataComponents.LORE,new ItemLore(List.of(
                        Component.translatable("type").append(Component.translatable("sword")).withStyle(s->s.withColor(0x55ffff).withItalic(false)),
                        Component.literal("GONZO").withStyle(s->s.withColor(0x55ff55).withItalic(false))
                                .append(Component.literal(" Beta Server Harem event").withStyle(s->s.withColor(0x555555).withItalic(false))))));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,Items.IRON_SWORD,true));
    }
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&(stack.is(ITEM)||LbcFartus.data(stack).getIntOr("one_punch",0)==1);}
    /** Replaces only source base damage, preserving all other stack data and added modifiers. */
    public static void updateDamage(ItemStack stack,int health){
        var attributes=stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);
        for(var entry:attributes.modifiers())
            if(entry.attribute().equals(Attributes.ATTACK_DAMAGE)&&entry.modifier().id().equals(BASE_DAMAGE)
                    &&entry.modifier().amount()==health&&entry.modifier().operation()==AttributeModifier.Operation.ADD_VALUE
                    &&entry.slot()==EquipmentSlotGroup.MAINHAND)return;
        var replacement=new ItemAttributeModifiers.Entry(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_DAMAGE,health,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND);
        var entries=new ArrayList<>(attributes.modifiers());
        for(int i=0;i<entries.size();i++){
            var entry=entries.get(i);
            if(entry.attribute().equals(Attributes.ATTACK_DAMAGE)&&entry.modifier().id().equals(BASE_DAMAGE)){
                if(entry.equals(replacement))return;
                entries.set(i,replacement);
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(entries));
                return;
            }
        }
        entries.add(replacement);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(entries));
    }
}
