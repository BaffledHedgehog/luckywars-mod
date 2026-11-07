package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;

/** The genuine use-on context and native factory own placement geometry, permissions and the sole debit. */
final class LbcTmtBlockUses {
    private LbcTmtBlockUses(){}
    static boolean supports(Item item){return shulkerItem(item)||BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("skywars2")
            &&(item instanceof BlockItem||item instanceof LbcMarkerEggItem||item instanceof LbcPumpkinatorItem||item instanceof SpawnEggItem);}
    static Item canonical(ItemStack stack){
        if(shulkerItem(stack.getItem()))return authoredShulker(stack)?stack.getItem():null;
        if(supports(stack.getItem()))return stack.getItem();
        var legacy=LbcImbaFollowupItems.legacyTarget(stack);if(legacy!=null&&supports(legacy))return legacy;
        if(LbcImbaTerrainUtilities.dirter(stack))return LbcImbaTerrainUtilityItems.DIRTER;
        if(LbcImbaTerrainUtilities.bastion(stack))return LbcImbaTerrainUtilityItems.BASTION;
        var template=LbcImbaPlaceableItems.templateKind(stack);
        if(template==LbcImbaTerrainUtilities.Kind.END_PORTAL)return LbcImbaPlaceableItems.END_PORTAL;
        if(template==LbcImbaTerrainUtilities.Kind.NETHER_PORTAL)return LbcImbaPlaceableItems.NETHER_PORTAL;
        return null;
    }
    private static boolean shulkerItem(Item item){return item==net.minecraft.world.item.Items.DYED_SHULKER_BOX.pick(net.minecraft.world.item.DyeColor.BLACK)
            ||item==net.minecraft.world.item.Items.DYED_SHULKER_BOX.pick(net.minecraft.world.item.DyeColor.YELLOW);}
    /** Admission requires the exact authored flag/table pair, never an ordinary vanilla container. */
    static boolean authoredShulker(ItemStack stack){
        if(!shulkerItem(stack.getItem()))return false;
        var loot=stack.get(net.minecraft.core.component.DataComponents.CONTAINER_LOOT);if(loot==null)return false;
        var id=loot.lootTable().identifier().toString();
        return stack.getItem()==net.minecraft.world.item.Items.DYED_SHULKER_BOX.pick(net.minecraft.world.item.DyeColor.BLACK)
                &&(id.equals("lbc:infinity")&&ItemData.intValue(stack,"inf_shulker",0)==1
                ||id.equals("lbc:infinity_upgraded")&&ItemData.intValue(stack,"inf_inf_shulker",0)==1)
                ||stack.getItem()==net.minecraft.world.item.Items.DYED_SHULKER_BOX.pick(net.minecraft.world.item.DyeColor.YELLOW)
                &&id.equals("lbc:infinity_upgraded_luck")&&ItemData.intValue(stack,"sigma_shulker",0)==1;
    }
    private static Item selected(ItemStack stack){var item=LbcTmtAbilities.nativePredicatesActive()?LbcTmtAbilities.nativePredicateItem(stack):null;return item==null?stack.getItem():item;}
    static boolean airCandidate(ItemStack stack){var item=selected(stack);return supports(item)&&!(item instanceof BlockItem)||LbcTmtNativeThrows.canonical(stack)==item;}
    static boolean air(GameController game,ServerPlayer player,InteractionHand hand){var stack=player.getItemInHand(hand);var item=selected(stack);return (supports(item)&&!(item instanceof BlockItem)||LbcTmtNativeThrows.canonical(stack)==item)&&item.use(player.level(),player,hand).consumesAction();}
    static int use(GameController game,UseOnContext context){
        if(!(context.getPlayer() instanceof ServerPlayer player))return 0;var stack=context.getItemInHand();int accepted=0;
        for(var property:LbcTmtAbilities.read(stack)){
            if(stack.isEmpty()||player.getItemInHand(context.getHand())!=stack)break;
            var item=LbcTmtComponents.nativeItem(property.data());if(!supports(item))continue;
            if(LbcTmtAbilities.invoke(stack,property.id(),()->item.useOn(context).consumesAction()))accepted++;
        }
        return accepted;
    }
    static InteractionResult input(UseOnContext context){var game=SkyWars2.game();return game==null?InteractionResult.PASS:game.items().tmt().useOn(context);}
}
