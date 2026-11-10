package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FoodOnAStickItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** A server-only one-shot on a passive client carrier still permits the vanilla offhand shield. */
public final class LbcUseInteractions {
    private LbcUseInteractions(){}
    /** The passive legacy carrier never swung the hand for a successful server-side cast. */
    public static boolean quietUse(ServerPlayer player,ItemStack stack){
        Item carrier=stack.getItem();
        if(carrier instanceof PolymerItem polymer)carrier=polymer.getPolymerItem(stack,player.connection.getPacketContext());
        return carrier==Items.CARROT_ON_A_STICK;
    }
    public static InteractionResult quietResult(boolean quiet,InteractionResult result){
        return quiet&&result instanceof InteractionResult.Success success&&success.shouldSwing()
                ?new InteractionResult.Success(InteractionResult.SwingSource.NONE,success.itemContext()):result;
    }
    public static InteractionResult airHands(ServerPlayer player,InteractionHand initial){
        if(player.isUsingItem())return InteractionResult.SUCCESS;
        for(var hand:initial==InteractionHand.MAIN_HAND?InteractionHand.values():new InteractionHand[]{initial}){
            var item=player.getItemInHand(hand);if(item.isEmpty()||!item.isItemEnabled(player.level().enabledFeatures()))continue;
            var result=player.gameMode.useItem(player,player.level(),item,hand);
            if(result instanceof InteractionResult.Success)return result;
        }
        // An interaction entity already consumed the client click; do not ask Polymer to replay it.
        return InteractionResult.SUCCESS;
    }
    public static void shieldAfter(ServerPlayerGameMode mode,ServerPlayer player,Level level,ItemStack before,
            InteractionHand hand,InteractionResult result){
        if(hand!=InteractionHand.MAIN_HAND||!(result instanceof InteractionResult.Success)
                ||player.isUsingItem()||player.isPassenger()||before.has(DataComponents.CONSUMABLE)
                ||before.has(DataComponents.EQUIPPABLE)||!player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS))return;
        Item carrier=before.getItem();
        if(carrier instanceof PolymerItem polymer)carrier=polymer.getPolymerItem(before,player.connection.getPacketContext());
        if(carrier.getClass()!=Item.class&&!(carrier instanceof FoodOnAStickItem))return;
        mode.useItem(player,level,player.getOffhandItem(),InteractionHand.OFF_HAND);
    }
}
