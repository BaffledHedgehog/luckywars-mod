package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.List;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;

/** Native kit identities; Polymer retains the authored model and equipment components. */
public final class LbcClownItems {
    public static final Item POT=register("gaara_pot",Items.REPEATING_COMMAND_BLOCK,properties("lbc:sand_pot"),
            "gaara_tikva",null,"lbc:sand_pot","sand_tikva",List.of(),false);
    public static final Item MASK=register("clown_mask",Items.LEATHER_HELMET,
            properties("lbc:armors/clownmask").humanoidArmor(ArmorMaterials.LEATHER,ArmorType.HELMET).durability(25)
                    .component(DataComponents.EQUIPPABLE,Equippable.builder(EquipmentSlot.HEAD)
                            .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID,Identifier.parse("lbc:clown_mask"))).build())
                    .component(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.builder()
                            .add(Attributes.ARMOR,new AttributeModifier(Identifier.withDefaultNamespace("fisjgidsjg4givibhjeswh"),3,
                                    AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.HEAD).build()),
            "clown_mask",null,"lbc:armors/clownmask","clown_mask",List.of("clown_mask.lore.1","clown_mask.lore.2"),false);
    public static final Item SMOKE_BOMB=register("clown_smoke_bomb",Items.SNOWBALL,properties("lbc:smoke_bomb").stacksTo(16),
            "smoke_bomb","special","lbc:smoke_bomb","smoke_bomb",List.of("smoke_bomb.lore"),true);
    public static final Item FALSE_GRAVE=register("false_grave",Items.SNOWBALL,properties("lbc:false_tumba").stacksTo(16),
            "false_tumba","gadget","lbc:false_tumba","false_tumba",List.of("false_tumba.lore.1","false_tumba.lore.2"),true);
    private LbcClownItems(){}
    public static void register(){}
    private static Item.Properties properties(String model){return new Item.Properties(){@Override public Identifier effectiveModel(){return Identifier.parse(model);}};}
    private static Item register(String id,Item carrier,Item.Properties properties,String flag,String extraFlag,String model,String name,List<String> lore,boolean throwable){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+id));
        var data=new CompoundTag();data.putInt(flag,1);if(extraFlag!=null)data.putInt(extraFlag,1);
        var title=Component.translatable(name).withStyle(s->s.withItalic(false));
        if(!id.equals("gaara_pot"))title.withStyle(ChatFormatting.WHITE);
        properties.setId(key).overrideDescription(name).component(DataComponents.ITEM_MODEL,Identifier.parse(model))
                .component(DataComponents.CUSTOM_NAME,title).component(DataComponents.CUSTOM_DATA,CustomData.of(data));
        if(!lore.isEmpty())properties.component(DataComponents.LORE,new ItemLore(lore.stream()
                .map(k->(Component)Component.translatable(k).withStyle(ChatFormatting.GRAY).withStyle(s->s.withItalic(false))).toList()));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,carrier,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                if(!throwable)return super.use(level,player,hand);
                var game=me.skywars2.SkyWars2.game();
                return game!=null&&player instanceof ServerPlayer p&&game.items().clownKit().use(p,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
}
