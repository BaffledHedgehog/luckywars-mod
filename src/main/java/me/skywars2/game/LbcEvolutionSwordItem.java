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
import net.minecraft.util.Unit;
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

/** The sword stack itself owns growth; accepted-hit admission belongs to ClassicMelee. */
public final class LbcEvolutionSwordItem {
    private static final Identifier BASE_DAMAGE=Identifier.withDefaultNamespace("base_attack_damage");
    public static final Item ITEM=registerItem();
    private LbcEvolutionSwordItem(){}
    public static void register(){}
    private static Item registerItem(){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:evolution_sword"));
        var data=new CompoundTag();for(var flag:List.of("transformable","sword","evolution"))data.putInt(flag,1);
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return Identifier.parse("lbc:evolutionsword");}}
                .setId(key).sword(ToolMaterial.GOLD,0,-2.4f).overrideDescription("evolution_sword")
                .component(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_DAMAGE,1,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build())
                .component(DataComponents.CUSTOM_DATA,CustomData.of(data)).component(DataComponents.UNBREAKABLE,Unit.INSTANCE)
                .component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:evolutionsword"))
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_f"))
                .component(DataComponents.CUSTOM_NAME,Component.translatable("evolution_sword").withStyle(s->s.withItalic(false)))
                .component(DataComponents.LORE,new ItemLore(List.of(
                        Component.translatable("type").append(Component.translatable("sword")).withStyle(s->s.withColor(0x55ffff).withItalic(false)),
                        Component.translatable("evolution_sword.lore.1").withStyle(s->s.withColor(0xffffff).withItalic(false)),
                        Component.translatable("evolution_sword.lore.2").withStyle(s->s.withColor(0xffffff).withItalic(false)))));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,Items.GOLDEN_SWORD,true));
    }
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&(stack.is(ITEM)||LbcFartus.data(stack).getIntOr("evolution",0)==1);}
    /** Matches /data get's floor and the old signed integer score's saturating ceiling. */
    static int nextScore(double amount){return (int)Math.floor(amount*10200d);}
    static Identifier tier(int score){
        return Identifier.parse(score<40000?"lbc:tier_f":score<90000?"lbc:tier_d":score<120000?"lbc:tier_c":
                score<150000?"lbc:tier_b":score<200000?"lbc:tier_a":score<500000?"lbc:tier_s":score<1000000?"lbc:tier_ss":"lbc:tier_x");
    }
    static void grow(ItemStack stack){
        var attributes=stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);
        var entries=new ArrayList<>(attributes.modifiers());int index=-1;
        // Canonical source modifier wins over independently added attack damage. Legacy raw stacks
        // without that ID retain the first source-style damage modifier's identity and slot.
        for(int i=0;i<entries.size();i++)if(entries.get(i).attribute().equals(Attributes.ATTACK_DAMAGE)){
            if(index<0)index=i;if(entries.get(i).modifier().id().equals(BASE_DAMAGE)){index=i;break;}
        }
        if(index<0)return;
        var entry=entries.get(index);var modifier=entry.modifier();
        if(!Double.isFinite(modifier.amount()))return;
        int score=nextScore(modifier.amount());
        entries.set(index,new ItemAttributeModifiers.Entry(entry.attribute(),new AttributeModifier(modifier.id(),score*.0001d,modifier.operation()),entry.slot()));
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(entries));
        stack.set(DataComponents.TOOLTIP_STYLE,tier(score));
    }
}
