package me.skywars2.game;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;

/** Authored item identities share vanilla projectile factories and dispenser configuration. */
public final class LbcImbaNativeVariants {
    private LbcImbaNativeVariants(){}
    public static boolean firework(ItemStack stack){return stack.is(Items.FIREWORK_ROCKET)||stack.is(LbcImbaNativeItems.FIREWORK)||privateFirework(stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA))!=null;}
    public static boolean firework(ItemStackTemplate stack){return stack.is(Items.FIREWORK_ROCKET)||stack.is(LbcImbaNativeItems.FIREWORK)||privateFirework(stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA))!=null;}
    private static LbcTmtAbilities.Ability privateFirework(net.minecraft.world.item.component.CustomData data){
        if(data==null||((ItemData.IntReader)(Object)data).skywars2$readInt("tmt_item",0)!=1)return null;
        // Immutable component cache, at most seven slots; no NBT copying in selection predicates.
        for(var property:((LbcTmtAbilities.CachedSlots)(Object)data).skywars2$tmtSlots())
            if(LbcTmtComponents.nativeItem(property.data())==LbcImbaNativeItems.FIREWORK)return property;
        return null;
    }
    /** Factory argument only: native selection/draw own the actual carrier's sole debit. */
    public static ItemStack fireworkAmmo(ItemStack stack){
        var property=privateFirework(stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA));
        var game=me.skywars2.SkyWars2.game();
        return property==null||game==null?stack:LbcTmtComponents.source(game,property);
    }
    /** Called after the native item constants exist. No vanilla behavior lookup/bootstrap dependency. */
    public static void register(){
        DispenserBlock.registerProjectileBehavior(LbcImbaNativeItems.INFINITE_ARROW);
        DispenserBlock.registerProjectileBehavior(LbcImbaNativeItems.HOMING_ARROW);
        DispenserBlock.registerProjectileBehavior(LbcImbaNativeItems.FIREWORK);
        for(var arrow:LbcImbaNativeItems.specialArrows())DispenserBlock.registerProjectileBehavior(arrow);
        for(var arrow:LbcImbaNativeItems.quiverArrows())DispenserBlock.registerProjectileBehavior(arrow);
    }
}
