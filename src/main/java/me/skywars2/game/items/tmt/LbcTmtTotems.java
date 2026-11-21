package me.skywars2.game.items.tmt;

import me.skywars2.game.GameController;
import me.skywars2.game.items.ItemData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Native death protection supplies the real selection, consumption and criterion. */
public final class LbcTmtTotems {
    private LbcTmtTotems(){}
    private static boolean trait(LbcTmtAbilities.Ability property){
        var data=(ItemData.IntReader)(Object)property.data();
        return data.skywars2$readInt("deviltotem",0)==1||data.skywars2$readInt("broken_totem",0)==1
                ||data.skywars2$readInt("totem",0)==1;
    }
    public static void apply(ItemStack stack){
        if(LbcTmtAbilities.read(stack).stream().anyMatch(LbcTmtTotems::trait))
            stack.set(DataComponents.DEATH_PROTECTION,Items.TOTEM_OF_UNDYING.getDefaultInstance().get(DataComponents.DEATH_PROTECTION));
    }
    public static boolean used(GameController game,ServerPlayer owner,ItemStack consumed){
        if(!LbcTmtItems.matches(consumed)||LbcTmtAbilities.active(consumed))return false;
        for(var property:LbcTmtAbilities.read(consumed))if(trait(property))
            LbcTmtAbilities.invoke(consumed,property.id(),()->{game.items().usedTotem(owner,consumed);return true;});
        return true;
    }
    public static boolean selectedInfinite(ItemStack stack){
        return LbcTmtItems.matches(stack)&&LbcTmtAbilities.active(stack)
                &&ItemData.stringValue(stack,LbcTmtAbilities.NATIVE,"").equals("skywars2:infinite_totem");
    }
}
