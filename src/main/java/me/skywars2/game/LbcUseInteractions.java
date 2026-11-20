package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.FoodOnAStickItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A server-only one-shot on a passive client carrier still permits the vanilla offhand shield. */
public final class LbcUseInteractions {
    private static final ScopedValue<ServerPlayer> FORCED_ITEM_USE = ScopedValue.newInstance();
    private static final ScopedValue<ServerPlayer> SHIELD_HANDOFF = ScopedValue.newInstance();
    private static final ScopedValue<EntityEggUse> ENTITY_EGG_USE = ScopedValue.newInstance();
    private LbcUseInteractions(){}
    public interface BlockInput {
        void skywars2$finishBlockInput();
        void skywars2$entityPress(ServerPlayer player, InteractionHand hand, Entity target);
        void skywars2$rememberEntityPass(ServerPlayer player, InteractionHand hand, Entity target, ItemStack stack);
    }
    public record TargetClick(Level level, HitResult hit, InteractionHand hand, ItemStack stack, boolean awaitingAir) {
        public boolean pairsAir(Level level, InteractionHand hand, ItemStack stack) {
            return awaitingAir && this.level==level && this.hand==hand && this.stack==stack;
        }

        private boolean followsMain(Level level, ItemStack main) {
            return hand==InteractionHand.MAIN_HAND && this.level==level && stack==main;
        }

        public boolean pairsOffhand(Level level, BlockHitResult hit, ItemStack main) {
            return followsMain(level,main) && this.hit instanceof BlockHitResult before
                    && before.getBlockPos().equals(hit.getBlockPos()) && before.getDirection()==hit.getDirection()
                    && before.getLocation().equals(hit.getLocation());
        }

        public boolean pairsOffhand(Level level, Entity entity, ItemStack main) {
            return followsMain(level,main) && hit instanceof EntityHitResult before && before.getEntity()==entity;
        }

        public BlockHitResult blockHit() { return hit instanceof BlockHitResult block ? block : null; }
        public TargetClick afterAir() { return new TargetClick(level,hit,hand,stack,false); }
    }

    public record AirClick(Level level, ItemStack main, ItemStack offhand) {
        public boolean pairsOffhand(Level level, ItemStack main, ItemStack offhand) {
            return this.level==level && this.main==main && this.offhand==offhand;
        }
    }

    public static void endClientInput(ServerPlayer player){
        if(player.gameMode instanceof BlockInput input)input.skywars2$finishBlockInput();
    }
    public static void entityPress(ServerPlayer player, InteractionHand hand, Entity target){
        if(player.gameMode instanceof BlockInput input)input.skywars2$entityPress(player,hand,target);
    }
    public static void rememberEntityPass(ServerPlayer player, InteractionHand hand, Entity target, ItemStack stack){
        if(player.gameMode instanceof BlockInput input)input.skywars2$rememberEntityPass(player,hand,target,stack);
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
    public static boolean holdingOffhandShield(ServerPlayer player){
        return player.isUsingItem()
                &&player.getUsedItemHand()==InteractionHand.OFF_HAND
                &&usesAsShield(player.getUseItem());
    }
    public static boolean usesAsShield(ItemStack stack){
        var equipment=stack.get(DataComponents.EQUIPPABLE);
        return stack.has(DataComponents.BLOCKS_ATTACKS)&&stack.getUseAnimation()==ItemUseAnimation.BLOCK
                &&(equipment==null||!equipment.swappable());
    }
    public static boolean shieldHandoff(ServerPlayer player,InteractionHand hand){
        return hand==InteractionHand.OFF_HAND&&SHIELD_HANDOFF.isBound()&&SHIELD_HANDOFF.get()==player;
    }

    private static final class EntityEggUse {
        final ServerPlayer player;
        final ItemStack stack;
        boolean accepted;

        EntityEggUse(ServerPlayer player, ItemStack stack) {
            this.player = player;
            this.stack = stack;
        }
    }

    public static InteractionResult nativeEntityItem(ServerPlayer player, InteractionHand hand, ItemStack stack,
            Supplier<InteractionResult> action) {
        var item = stack.getItem();
        if (hand != InteractionHand.MAIN_HAND || !(item instanceof SpawnEggItem) || !(item instanceof PolymerItem)
                || !usesAsShield(player.getOffhandItem())) return action.get();
        var use = new EntityEggUse(player, stack);
        var result = ScopedValue.where(ENTITY_EGG_USE, use).call(action::get);
        if (use.accepted) shieldAfter(player.gameMode, player, player.level(), hand, result);
        return result;
    }

    public static void acceptedEntityEgg(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        if (!ENTITY_EGG_USE.isBound()) return;
        var use = ENTITY_EGG_USE.get();
        if (use.player == player && use.stack == stack) use.accepted = true;
    }

    public static boolean forcedUse(ServerPlayer player){
        return FORCED_ITEM_USE.isBound()&&FORCED_ITEM_USE.get()==player;
    }
    static InteractionResult forceItem(ServerPlayer player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(stack.isEmpty()||usesAsShield(stack))return InteractionResult.PASS;
        return ScopedValue.where(FORCED_ITEM_USE,player)
                .call(()->player.gameMode.useItem(player,player.level(),stack,hand));
    }
    public static void shieldAfter(ServerPlayerGameMode mode,ServerPlayer player,Level level,
            InteractionHand hand,InteractionResult result){
        if(hand!=InteractionHand.MAIN_HAND||!(result instanceof InteractionResult.Success)
                ||player.isUsingItem()||player.isPassenger()
                ||forcedUse(player)
                ||!usesAsShield(player.getOffhandItem()))return;
        ScopedValue.where(SHIELD_HANDOFF,player)
                .run(()->mode.useItem(player,level,player.getOffhandItem(),InteractionHand.OFF_HAND));
    }
}
