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
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

/** Native tool components retain vanilla digging/weapon behavior; Polymer supplies the authored carriers. */
public final class LbcMechanicalItems {
    public static final Item BREAKER=register("steampunk_pickaxe",Items.NETHERITE_PICKAXE,
            properties("lbc:steampunk_pickaxe").pickaxe(ToolMaterial.NETHERITE,1,-2.8f).fireResistant(),"steampunk_pickaxe","lbc:steampunk_pickaxe",List.of("steampunk_pickaxe.lore"));
    public static final Item GLOVE=register("steampunk_sword",Items.IRON_SWORD,
            properties("lbc:steampunk_sword").sword(ToolMaterial.IRON,3,-2.4f).component(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_SPEED,new AttributeModifier(Identifier.withDefaultNamespace("hdtnkbuycthfngkhgspeed"),-2,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_DAMAGE,new AttributeModifier(Identifier.withDefaultNamespace("genebyngkucfyvdbffhe"),3,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build()),
            "steampunk_sword","lbc:steampunk_sword",List.of("steampunk_sword.lore.1","steampunk_sword.lore.2"));
    public static final Item HOOK=register("grapping_hook",Items.CARROT_ON_A_STICK,properties("lbc:deagle").durability(32),
            "grapping_hook","lbc:deagle",List.of("grapping_hook.lore"));
    private LbcMechanicalItems(){}
    public static void register(){}
    private static Item.Properties properties(String model){return new Item.Properties(){@Override public Identifier effectiveModel(){return Identifier.parse(model);}};}
    private static Item register(String flag,Item carrier,Item.Properties properties,String name,String model,List<String> lore){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+flag));var data=new CompoundTag();data.putInt(flag,1);
        if(flag.equals("grapping_hook"))data.putInt("magicwand",1); // Legacy presentation flag; admission is TECH, not MAG.
        properties.setId(key).overrideDescription(name).component(DataComponents.ITEM_MODEL,Identifier.parse(model))
                .component(DataComponents.CUSTOM_NAME,Component.translatable(name).withStyle(s->s.withItalic(false)))
                .component(DataComponents.CUSTOM_DATA,CustomData.of(data))
                .component(DataComponents.LORE,new ItemLore(lore.stream().map(k->(Component)Component.translatable(k).withStyle(s->s.withItalic(false))).toList()));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,carrier,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                if(!flag.equals("grapping_hook"))return super.use(level,player,hand);
                var game=me.skywars2.SkyWars2.game();
                return game!=null&&player instanceof ServerPlayer p&&game.items().wands().use(p,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
}
