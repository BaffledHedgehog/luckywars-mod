package me.skywars2.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

/** Real gallery random-chest rolls contain only SkyWars catalog identities. */
public final class FeedbackGalleryChecks {
    private FeedbackGalleryChecks() {}
    public static void run(GameController game,ServerPlayer player){
        for(var table:java.util.List.of("lbc:27_random_items","lbc:uniform/27_random_items")){
            var stacks=game.lbc().rollTable(player.level(),player.position(),player,table);
            if(stacks.isEmpty())throw new IllegalStateException("Empty gallery random chest: "+table);
            for(var stack:stacks){
                var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
                if(id.getNamespace().equals("minecraft")&&ItemStack.isSameItemSameComponents(stack.copyWithCount(1),new ItemStack(stack.getItem())))
                    throw new IllegalStateException("Unmodified vanilla registry ticket in "+table+": "+id);
            }
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS feedback-gallery");
    }
}
