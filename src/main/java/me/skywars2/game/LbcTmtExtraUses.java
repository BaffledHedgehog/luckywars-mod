package me.skywars2.game;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Pure native input admission and real-hand adapters outside the common wand dispatcher. */
final class LbcTmtExtraUses {
    private LbcTmtExtraUses(){}
    private enum Input { TOUHOU, NEXUS_COMPASS, NEXUS_COMBAT, NEXUS_TOOLS, POZDMAN, BALANCE, SUPPLY,
                         TERRA_RAZOR, TERRA_SWORD, KAPUSTA, CRUX, FIREBALL, WIN, POTION, SHIELD }

    private static Input input(ItemStack stack){
        if(stack.isEmpty())return null;
        // These are the existing UseItemCallback's native admission predicates,
        // in its order. Source owner methods still decide actual runtime admission.
        if(LbcTouhouCollectibles.consumable(stack))return Input.TOUHOU;
        if(LbcNexusToolItems.isCompass(stack))return Input.NEXUS_COMPASS;
        if(LbcNexusCombat.matches(stack))return Input.NEXUS_COMBAT;
        if(LbcNexusTools.matches(stack))return Input.NEXUS_TOOLS;
        if(ItemData.intValue(stack,"pozdman_bot",0)==1)return Input.POZDMAN;
        if(LbcBalanceItems.matches(stack))return Input.BALANCE;
        if(ItemData.intValue(stack,"supply_beacon",0)==1)return Input.SUPPLY;
        if(LbcTerra.isRazor(stack))return Input.TERRA_RAZOR;
        if(LbcTerra.isSword(stack))return Input.TERRA_SWORD;
        // Their ordinary Item.use overrides call these same owners directly.
        if(LbcKapusta.matches(stack))return Input.KAPUSTA;
        if(LbcOverload.isCrux(stack))return Input.CRUX;
        if(LbcSpecialFireballs.variant(stack)!=0)return Input.FIREBALL;
        if(LbcKfcRewards.winButton(stack))return Input.WIN;
        if(LbcTmtManaPotions.thrownCandidate(stack))return Input.POTION;
        if(LbcTmtShield.candidate(stack))return Input.SHIELD;
        return null;
    }

    static boolean candidate(ItemStack stack){return input(stack)!=null;}
    /** Ordinary callback positions precede the native Item.use-only continuations. */
    static int order(ItemStack stack){
        var input=input(stack);if(input==null)return Integer.MAX_VALUE;
        return switch(input){
            case TOUHOU->0;
            case NEXUS_COMPASS->10;
            case NEXUS_COMBAT->11;
            case NEXUS_TOOLS->12;
            case POZDMAN->20;
            case BALANCE->40;
            case SUPPLY->100;
            case TERRA_RAZOR->190;
            case TERRA_SWORD->200;
            case KAPUSTA->2000;
            case CRUX->2001;
            case FIREBALL->2002;
            case WIN->2003;
            case POTION->2004;
            case SHIELD->2005;
        };
    }

    /** Called only inside the selected property's scope against the real held stack. */
    static boolean use(GameController game,ServerPlayer player,InteractionHand hand){
        var selected=input(player.getItemInHand(hand));if(selected==null)return false;
        return switch(selected){
            // Keep source callback order, permission checks and supply fluid raycast.
            // No block context, collectible intake or held-tick action is fabricated.
            case TOUHOU,NEXUS_COMPASS,NEXUS_COMBAT,NEXUS_TOOLS,POZDMAN,BALANCE,SUPPLY->
                    UseItemCallback.EVENT.invoker().interact(player,player.level(),hand).consumesAction();
            // Public ordinary Terra entry points also dispatch the other hand.
            // TMT's coordinator already owns that continuation; use the shared cast body.
            case TERRA_RAZOR->game.items().terra().castRazor(player,hand);
            case TERRA_SWORD->game.items().terra().castSword(player,hand);
            case KAPUSTA->game.items().kapusta().use(player,hand);
            case CRUX->game.items().mana().overload().use(player,hand);
            case FIREBALL->game.items().specialFireballs().use(player,hand);
            case WIN->game.items().kfcRewards().useWin(player,hand);
            case POTION->LbcTmtManaPotions.use(game,player,hand);
            case SHIELD->LbcTmtShield.use(game,player,hand);
        };
    }
}
