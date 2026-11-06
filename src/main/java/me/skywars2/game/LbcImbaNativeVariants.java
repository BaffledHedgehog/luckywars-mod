package me.skywars2.game;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;

/** Authored item identities share vanilla projectile factories and dispenser configuration. */
public final class LbcImbaNativeVariants {
    private LbcImbaNativeVariants(){}
    public static boolean firework(ItemStack stack){return stack.is(Items.FIREWORK_ROCKET)||stack.is(LbcImbaNativeItems.FIREWORK);}
    public static boolean firework(ItemStackTemplate stack){return stack.is(Items.FIREWORK_ROCKET)||stack.is(LbcImbaNativeItems.FIREWORK);}
    /** Called after the native item constants exist. No vanilla behavior lookup/bootstrap dependency. */
    public static void register(){
        DispenserBlock.registerProjectileBehavior(LbcImbaNativeItems.INFINITE_ARROW);
        DispenserBlock.registerProjectileBehavior(LbcImbaNativeItems.HOMING_ARROW);
        DispenserBlock.registerProjectileBehavior(LbcImbaNativeItems.FIREWORK);
        for(var arrow:LbcImbaNativeItems.specialArrows())DispenserBlock.registerProjectileBehavior(arrow);
        for(var arrow:LbcImbaNativeItems.quiverArrows())DispenserBlock.registerProjectileBehavior(arrow);
    }
}
