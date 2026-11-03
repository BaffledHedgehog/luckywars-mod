package me.skywars2.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** New ordinary item abilities are separate from participant roles and accepted consequences. */
public final class LbcDimensionItems {
    private LbcDimensionItems(){}
    public static boolean ordinary(Level level){return ordinary(level.dimension().identifier().toString());}
    static boolean ordinary(String dimension){return !dimension.equals("minecraft:nexus")&&!dimension.equals("minecraft:imprinted");}
    public static boolean ordinary(ServerPlayer player){return player!=null&&ordinary(player.level());}
    public static boolean blocked(Level level){return !ordinary(level);}
    public static boolean normal(Level level){return ordinary(level);}
    /** Source vanilla carriers keep custom_data; registered native items also retain their namespace. */
    public static boolean custom(ItemStack stack){
        if(stack.isEmpty())return false;
        var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("skywars2")
                ||data!=null&&!data.isEmpty()
                ||stack.get(net.minecraft.core.component.DataComponents.ITEM_MODEL)!=null
                    &&stack.get(net.minecraft.core.component.DataComponents.ITEM_MODEL).getNamespace().equals("lbc");
    }
    public static boolean allowed(ServerPlayer player,ItemStack stack){return player!=null&&(!custom(stack)||ordinary(player)||nexusInput(player,stack));}
    /** Exact live Nexus-specific RC flags. Recognition supplies no missing tool implementation. */
    public static boolean nexusTool(ItemStack stack){
        if(stack.isEmpty())return false;var d=LbcFartus.data(stack);
        return d.getIntOr("nexus_roadgun",0)==1||d.getIntOr("nexus_corrupter",0)==1
                ||d.getIntOr("nexus_orb_gun",0)==1||d.getIntOr("nexus_pulsar",0)==1||LbcNexusToolItems.isCompass(stack);
    }
    /** For a dedicated Nexus RC router only; ordinary input must use ordinary(player). */
    public static boolean nexusInput(ServerPlayer player,ItemStack stack){return player!=null&&player.level().dimension().identifier().toString().equals("minecraft:nexus")&&nexusTool(stack);}
}
