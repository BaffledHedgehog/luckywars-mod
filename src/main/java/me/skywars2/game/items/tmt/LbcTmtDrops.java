package me.skywars2.game.items.tmt;

import java.util.List;

import net.minecraft.world.item.ItemStack;

/** A bounded native dropped-item contract; the existing shared dispatcher owns every gameplay action. */
public final class LbcTmtDrops {
    private LbcTmtDrops(){}
    static final String FLAG="tmt_drop";
    // Loose TMT stacks no longer dispatch borrowed capabilities on every item tick.
    // Ordinary standalone drop items continue to use their own scheduler paths.
    public static boolean matches(ItemStack stack){return false;}
    public static List<LbcTmtAbilities.Ability> properties(ItemStack stack){return List.of();}
}
