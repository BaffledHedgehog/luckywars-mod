package me.skywars2.game.items.tmt;

import java.util.List;

import me.skywars2.game.items.ItemData;
import net.minecraft.world.item.ItemStack;

/** A bounded native dropped-item contract; the existing shared dispatcher owns every gameplay action. */
public final class LbcTmtDrops {
    private LbcTmtDrops(){}
    static final String FLAG="tmt_drop";
    public static boolean matches(ItemStack stack){return LbcTmtItems.matches(stack)&&ItemData.intValue(stack,FLAG,0)==1;}
    public static List<LbcTmtAbilities.Ability> properties(ItemStack stack){return LbcTmtAbilities.read(stack).stream()
            .filter(property->((ItemData.IntReader)(Object)property.data()).skywars2$readInt(FLAG,0)==1).toList();}
}
