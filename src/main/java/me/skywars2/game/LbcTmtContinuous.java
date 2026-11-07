package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Native sustained-use prerequisites. The original owners still handle every real using tick. */
final class LbcTmtContinuous {
    private LbcTmtContinuous(){}
    static boolean sustained(ItemStack stack){return LbcHolyShield.matches(stack)||LbcCunnyItems.nahida(stack)||LbcTmtShield.sustained(stack);}
    static void apply(ItemStack stack){
        var game=me.skywars2.SkyWars2.game();if(game!=null)LbcTmtShield.apply(game,stack);
        if(LbcHolyShield.matches(stack))stack.set(DataComponents.BLOCKS_ATTACKS,
                LbcMeleeItems.Kind.HOLY_SHIELD.item.getDefaultInstance().get(DataComponents.BLOCKS_ATTACKS));
        if(LbcCunnyItems.nahida(stack)){
            var source=LbcCunnyItems.NAHIDA.getDefaultInstance();
            // Keep independently generated edible-carrier properties when present.
            if(!stack.has(DataComponents.CONSUMABLE))stack.set(DataComponents.CONSUMABLE,source.get(DataComponents.CONSUMABLE));
            if(!stack.has(DataComponents.USE_EFFECTS)&&source.has(DataComponents.USE_EFFECTS))stack.set(DataComponents.USE_EFFECTS,source.get(DataComponents.USE_EFFECTS));
        }
    }
    /** Carrot/snowball native overrides otherwise never enter the generic Item sustained-use path. */
    static InteractionResult use(GameController game,ServerPlayer owner,InteractionHand hand){
        var stack=owner.getItemInHand(hand);if(!sustained(stack))return InteractionResult.PASS;
        if(LbcTmtShield.sustained(stack))return LbcTmtShield.input(game,owner,hand);
        if(!game.items().tmt().admitted(owner)||LbcCunnyItems.nahida(stack)&&!game.items().cunny().nahidaAllowed(owner,hand))return InteractionResult.FAIL;
        owner.startUsingItem(hand);return InteractionResult.CONSUME;
    }
    static void using(GameController game,ServerPlayer owner,ItemStack stack){
        if(LbcCunnyItems.nahida(stack)&&owner.getUseItem()==stack)game.items().cunny().nahidaUseTick(owner);
    }
    /** Generic vanilla Item predicts the same component-driven action; procedural model is unchanged. */
    static net.minecraft.world.item.Item clientCarrier(ItemStack stack,net.minecraft.world.item.Item ordinary){
        return sustained(stack)?Items.STICK:ordinary;
    }
}
