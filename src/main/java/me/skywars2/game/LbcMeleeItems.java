package me.skywars2.game;

import eu.pb4.polymer.core.api.item.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;

/** Canonical blades and shields reuse the existing combat, shield and wand authorities. */
public final class LbcMeleeItems {
    public enum Kind {
        ANTIMAGIC_SWORD("antimagic_sword","ported/antimagic_sword",Items.IRON_SWORD),
        CHAOS_SWORD("chaos_sword","ported/chaos_sword",Items.GOLDEN_SWORD),
        DEVIL_SWORD("devil_sword","item_defs/lbc/devilsword__9f31b811efe8",Items.WOODEN_SWORD),
        ENDER_SWORD("ender_sword","item_defs/lbc/endersword__2389b133c862",Items.NETHERITE_SWORD),
        EPIC_SWORD("epic_sword","item_defs/lbc/epicsword__989e0fb94f1b",Items.IRON_SWORD),
        FEEDING_SWORD("feeding_sword","item_defs/lbc/steaksword__6fe8d7bbd1c9",Items.WOODEN_SWORD),
        FRAGMENT_SWORD("fragment_sword","item_defs/lbc/fragmentsword__ef030698c9e6",Items.IRON_SWORD),
        GRAVITY_SWORD("gravity_sword","item_defs/lbc/gravitysword__58ba82d16933",Items.IRON_SWORD),
        GUARDIAN_SWORD("guardian_sword","item_defs/lbc/guardiansword__1725f947f844",Items.IRON_SWORD),
        HOLY_SHIELD("holy_shield","item_defs/minecraft/shield__cb65b398d6f6",Items.SHIELD),
        HYLIAN_SHIELD("hylian_shield","item_defs/minecraft/shield__01ad7522ef57",Items.SHIELD),
        HYLIAN_SWORD("hylian_sword","item_defs/lbc/hylian_sword__1c647afaca30",Items.NETHERITE_SWORD),
        METAL_PIPE_DIVIDER("metal_pipe_divider","ported/metal_pipe_divider",Items.CARROT_ON_A_STICK),
        PISTON_SWORD("piston_sword","ported/piston_sword",Items.CARROT_ON_A_STICK),
        VAMPIRE_FANG("vampire_fang","item_defs/lbc/vampiresword__3945e27bc9d4",Items.IRON_SWORD);
        public final String id,definition;
        public final Item carrier,item;
        Kind(String id,String definition,Item carrier){
            this.id=id;this.definition="/data/lbc/loot_table/"+definition+".json";this.carrier=carrier;
            var properties=LbcUtilityItemPrototypes.deferredProperties("skywars2:"+id,this.definition,carrier,64);
            item=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+id),
                    carrier==Items.SHIELD?new NativeShield(properties):carrier==Items.CARROT_ON_A_STICK?
                            LbcWands.nativeTool(properties,carrier):new SimplePolymerItem(properties,carrier,true));
        }
    }
    private LbcMeleeItems(){}
    public static void register(){Kind.values();}
    private static final class NativeShield extends ShieldItem implements PolymerItem {
        NativeShield(Properties properties){super(properties);}
        @Override public Item getPolymerItem(ItemStack stack,net.fabricmc.fabric.api.networking.v1.context.PacketContext context){return Items.SHIELD;}
    }
}
