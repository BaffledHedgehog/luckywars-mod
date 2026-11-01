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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

/** Native pursuit-kit identities with the original authored Polymer carriers. */
public final class LbcPursuitItems {
    public static final Item BOUNCING_POOP=register("bouncing_poop",Items.SNOWBALL,
            properties("lbc:3dpoop").stacksTo(16),"lbc:3dpoop","govno_raskid",List.of(),true);
    public static final Item DEUS=register("deus",Items.CARROT_ON_A_STICK,
            properties("lbc:deus").durability(25),"lbc:deus","deus",
            List.of(Component.translatable("deus.lore").withStyle(s->s.withItalic(false))),false);
    private LbcPursuitItems(){}
    public static void register(){}
    private static Item.Properties properties(String model){return new Item.Properties(){@Override public Identifier effectiveModel(){return Identifier.parse(model);}};}
    private static Item register(String id,Item carrier,Item.Properties properties,String model,String name,List<Component> lore,boolean poop){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+id));
        var data=new CompoundTag();
        if(poop){data.putInt("gadget",1);data.putInt("govno_raskid",1);data.putInt("govnoq",1);}
        else{data.putInt("deus",1);data.putInt("magicwand",1);}
        properties.setId(key).overrideDescription(name).component(DataComponents.ITEM_MODEL,Identifier.parse(model))
                .component(DataComponents.CUSTOM_NAME,Component.translatable(name).withStyle(s->s.withItalic(false)))
                .component(DataComponents.CUSTOM_DATA,CustomData.of(data));
        if(!lore.isEmpty())properties.component(DataComponents.LORE,new ItemLore(lore));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,carrier,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                var game=me.skywars2.SkyWars2.game();
                if(game==null||!(player instanceof ServerPlayer p))return InteractionResult.FAIL;
                boolean used=poop?game.items().bouncingPoop().use(p,hand):game.items().wands().use(p,hand);
                return used?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
}
