package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

/** The existing authored kit leaves use native item identities with their original Polymer carriers. */
public final class LbcKitUtilityItems {
    public static final Item DOUBLE=tablet(2),TRIPLE=tablet(3),QUADRUPLE=tablet(4),QUINTUPLE=tablet(5);
    public static final Item PALM=palm();
    private LbcKitUtilityItems(){}
    public static void register(){}
    private static Item.Properties properties(String model){return new Item.Properties(){@Override public Identifier effectiveModel(){return Identifier.parse(model);}};}
    private static Item tablet(int number){
        var data=new CompoundTag();data.putInt("multiply",number);if(number==5)data.putInt("magicwand",1);
        var model="lbc:x"+number;var properties=properties(model);if(number==5)properties.durability(25);
        return item("multiply_"+number,number==5?Items.CARROT_ON_A_STICK:Items.REPEATING_COMMAND_BLOCK,properties,data,
                model,"multiply_by_"+number,List.of(Component.translatable("multiply_by_"+number+".lore").withStyle(s->s.withItalic(false)),
                        Component.translatable(number==5?"multiply1.lore":"multiply.lore").withStyle(s->s.withItalic(false))));
    }
    private static Item palm(){
        var data=new CompoundTag();data.putInt("banana_palm",1);data.putInt("magicwand",1);
        var attributes=ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,new AttributeModifier(Identifier.withDefaultNamespace("base_attack_damage"),5,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,new AttributeModifier(Identifier.withDefaultNamespace("base_attack_speed"),-2.4,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build();
        return item("banana_palm",Items.CARROT_ON_A_STICK,properties("lbc:palm_small").durability(64).component(DataComponents.ATTRIBUTE_MODIFIERS,attributes),
                data,"lbc:palm_small","lbckit.23",List.of(Component.translatable("lbckit.23.lore.2").withColor(0xffffff).withStyle(s->s.withItalic(false))));
    }
    private static Item item(String id,Item carrier,Item.Properties properties,CompoundTag data,String model,String name,List<Component> lore){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+id));
        properties.setId(key).overrideDescription(name).component(DataComponents.CUSTOM_DATA,CustomData.of(data))
                .component(DataComponents.ITEM_MODEL,Identifier.parse(model)).component(DataComponents.CUSTOM_NAME,Component.translatable(name).withStyle(s->s.withItalic(false)))
                .component(DataComponents.LORE,new ItemLore(lore));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,carrier,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                if(data.getIntOr("multiply",0)>=2&&data.getIntOr("multiply",0)<=4)return super.use(level,player,hand);
                var game=me.skywars2.SkyWars2.game();
                return game!=null&&player instanceof ServerPlayer p&&game.items().wands().use(p,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
}
