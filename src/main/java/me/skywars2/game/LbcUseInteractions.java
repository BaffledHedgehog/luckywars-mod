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
    public interface BlockInput { void skywars2$finishBlockInput(); }
    public static void endClientInput(ServerPlayer player){
        if(player.gameMode instanceof BlockInput input)input.skywars2$finishBlockInput();
    }
    /** Passive client carriers send main-on, main-air, then offhand-on for one click. */
    public static boolean deferAirToOffhand(ServerPlayer player,ItemStack stack,InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit){
        if(hand!=InteractionHand.MAIN_HAND||hit==null||stack.has(DataComponents.CONSUMABLE)
                ||stack.has(DataComponents.EQUIPPABLE)||stack.has(DataComponents.BLOCKS_ATTACKS))return false;
        Item carrier=stack.getItem();
        if(carrier instanceof PolymerItem polymer)carrier=polymer.getPolymerItem(stack,player.connection.getPacketContext());
        return (carrier.getClass()==Item.class||carrier instanceof FoodOnAStickItem)&&canPlaceOffhand(player,hit);
    }
    public static boolean canPlaceOffhand(ServerPlayer player,net.minecraft.world.phys.BlockHitResult hit){
        var stack=player.getOffhandItem();
        if(stack.isEmpty()||!(stack.getItem() instanceof net.minecraft.world.item.BlockItem block)
                ||player.isSpectator()||player.getCooldowns().isOnCooldown(stack)
                ||!stack.isItemEnabled(player.level().enabledFeatures()))return false;
        var context=new net.minecraft.world.item.context.BlockPlaceContext(player,InteractionHand.OFF_HAND,stack,hit);
        if(!player.level().hasChunkAt(context.getClickedPos())||!context.canPlace())return false;
        context=block.updatePlacementContext(context);
        if(context==null||!player.level().hasChunkAt(context.getClickedPos())
                ||!player.level().getWorldBorder().isWithinBounds(context.getClickedPos())
                ||!player.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),stack)
                ||!player.level().mayInteract(player,context.getClickedPos()))return false;
        return ((me.skywars2.mixin.BlockPlacementAccess)block).skywars2$placementState(context)!=null;
    }
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
