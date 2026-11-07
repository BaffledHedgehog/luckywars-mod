package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;

/** Canonical projectile weapons share vanilla input/physics and the existing ability owners. */
public final class LbcProjectileWeaponItems {
    public enum Kind {
        HARPOON("harpoon","ported/harpoon",Items.TRIDENT),
        SEA_LEGEND("sea_legend","item_defs/minecraft/trident__145e126e880c",Items.TRIDENT),
        SHULKER_BOW("shulker_bow","item_defs/lbc/shulkerbow__31b28b65ce39",Items.BOW),
        MINIGUN_CROSSBOW("minigun_crossbow","item_defs/minecraft/crossbow__9d0c2c339729",Items.CROSSBOW),
        MINIGUN_CROSSBOW_OLD("minigun_crossbow_old","item_defs/minecraft/crossbow__1e188044c7a2",Items.CROSSBOW),
        DOUBLE_BOW("double_bow","item_defs/lbc/doublebow__ce108b3ec6f0",Items.BOW),
        BLAZE_CROSSBOW("blaze_crossbow","item_defs/minecraft/crossbow__9eb0e01f7539",Items.CROSSBOW),
        GHAST_CROSSBOW("ghast_crossbow","item_defs/minecraft/crossbow__55f5ba9c167d",Items.CROSSBOW),
        DILDOMET("dildomet","dildomet",Items.CROSSBOW);
        public final String id,definition;
        public final Item carrier,item;
        Kind(String id,String definition,Item carrier){
            this.id=id;this.definition="/data/lbc/loot_table/"+definition+".json";this.carrier=carrier;
            var properties=LbcUtilityItemPrototypes.deferredProperties("skywars2:"+id,this.definition,carrier,1);
            item=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+id),
                    carrier==Items.BOW?bow(properties):carrier==Items.CROSSBOW?new NativeCrossbow(properties):new NativeTrident(properties));
        }
    }
    private LbcProjectileWeaponItems(){}
    public static void register(){LbcLegacyRanged.register();Kind.values();}
    static Item bow(Item.Properties properties){return new NativeBow(properties);}
    static List<Item> items(){
        var out=new ArrayList<Item>();for(var kind:Kind.values())out.add(kind.item);
        out.add(LbcChargedBowItems.RAILGUN);out.add(LbcChargedBowItems.SHISHGUN);
        for(var kind:LbcImbaCrossbows.Kind.values())out.add(kind.item);return List.copyOf(out);
    }
    private static final class NativeBow extends BowItem implements PolymerItem {
        NativeBow(Properties properties){super(properties);}
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.BOW;}
    }
    static class NativeCrossbow extends CrossbowItem implements PolymerItem {
        NativeCrossbow(Properties properties){super(properties);}
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.CROSSBOW;}
    }
    private static final class NativeTrident extends TridentItem implements PolymerItem {
        NativeTrident(Properties properties){super(properties);}
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.TRIDENT;}
    }
}
