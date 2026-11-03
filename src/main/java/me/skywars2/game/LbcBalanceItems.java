package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;

/** Explicit old item identities. Other old carriers keep their own native item owners. */
public final class LbcBalanceItems {
    public enum Wand {
        EVOKER("evocator_old",961), BASS("bassboost_old",961), ENDER("enderwand_old",761),
        CORRUPTER("corrupter_old",901), VAMPIRE("lifestealer_old",971), FOXUS("foxusing_old",921),
        BLOOD("bloodmagic_old",701), WIKI("badwiki_old",941);
        final String flag; final int threshold;
        Wand(String flag,int threshold){this.flag=flag;this.threshold=threshold;}
    }
    public static final Item BALANCE=register("balance","lbc:balance","item_balance",true);
    private LbcBalanceItems(){}
    public static void register(){}
    static boolean flag(ItemStack stack,String flag){
        if(stack.isEmpty())return false;
        var tag=new CompoundTag();tag.putInt(flag,1);
        return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).matchedBy(tag);
    }
    public static boolean isBalance(ItemStack stack){return stack.is(BALANCE)||flag(stack,"item_balance");}
    static Wand wand(ItemStack stack){for(var wand:Wand.values())if(flag(stack,wand.flag))return wand;return null;}
    public static boolean matches(ItemStack stack){return isBalance(stack)||wand(stack)!=null||LbcBalanceRays.kind(stack)!=null;}
    private static Item register(String id,String model,String name,boolean balance){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+id));
        var flags=new CompoundTag();flags.putInt("magicwand",1);flags.putInt(name,1);
        var lines=new ArrayList<Component>();for(int i=1;i<=8;i++)lines.add(Component.translatable("item_balance.lore."+i).withStyle(s->s.withColor(ChatFormatting.WHITE).withItalic(false)));
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return Identifier.parse(model);}}
            .setId(key).stacksTo(1).component(DataComponents.ITEM_MODEL,Identifier.parse(model))
            .component(DataComponents.CUSTOM_DATA,CustomData.of(flags)).component(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_x"))
            .component(DataComponents.ITEM_NAME,Component.translatable(name).withStyle(s->s.withColor(ChatFormatting.GOLD).withItalic(false)))
            .component(DataComponents.LORE,new ItemLore(lines));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,Items.CARROT_ON_A_STICK));
    }
}
