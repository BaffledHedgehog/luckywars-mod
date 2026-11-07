package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.component.Consumables;

/** Typed native potion delivery. The potion contents and its existing effect owners remain authoritative. */
public final class LbcTmtManaPotions {
    private LbcTmtManaPotions(){}
    enum Delivery { DRINK, SPLASH, LINGERING }

    private static Item selected(ItemStack stack){
        var nativeItem=LbcTmtAbilities.nativePredicatesActive()?LbcTmtAbilities.nativePredicateItem(stack):null;
        return nativeItem==null?stack.getItem():nativeItem;
    }
    /** Empty registered transports are not a dose or a separate sampled ability. */
    static Delivery delivery(ItemStack stack){
        if(stack.isEmpty())return null;
        var contents=stack.get(DataComponents.POTION_CONTENTS);if(contents==null||!contents.hasEffects())return null;
        var item=selected(stack);
        // Most specific first: thrown potion classes inherit PotionItem. Call the actual
        // selected subclass factory below; native ChemicalPotionItem remains a chemical.
        if(item instanceof LingeringPotionItem)return Delivery.LINGERING;
        if(item instanceof SplashPotionItem)return Delivery.SPLASH;
        if(item instanceof PotionItem)return Delivery.DRINK;
        return null;
    }
    static boolean thrownCandidate(ItemStack stack){var delivery=delivery(stack);return delivery==Delivery.SPLASH||delivery==Delivery.LINGERING;}

    /** Effect identities define a family; full ordered doses/levels/colors/scales remain private variants. */
    static String family(ItemStack stack){
        var delivery=delivery(stack);if(delivery==null)return null;
        var identities=new java.util.TreeSet<String>();
        for(var effect:stack.get(DataComponents.POTION_CONTENTS).getAllEffects()){
            String id=BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString();
            // Legacy Raid Omen levels encode different operations, not stronger doses of one effect.
            if(effect.is(MobEffects.RAID_OMEN)&&effect.getAmplifier()>=16)id+="/"+effect.getAmplifier();
            identities.add(id);
        }
        String identity=String.join("+",identities);
        if(identity.length()>80)try{identity="effects/"+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
            catch(java.security.NoSuchAlgorithmException error){throw new IllegalStateException(error);}
        return "potion/"+delivery.name().toLowerCase(java.util.Locale.ROOT)+"/"+identity;
    }

    /**
     * Native thrown-potion constructor source hook. Preserve its actual private dose/native item,
     * never sibling abilities, generated carrier stats or a borrowed active-scope token.
     * Copies made by the native factory retain its selected predicate context during this scope.
     */
    public static ItemStack projectileSource(GameController game,ItemStack stack){
        if(game==null||!LbcTmtAbilities.nativePredicatesActive()||LbcTmtAbilities.nativePredicateItem(stack)==null||!thrownCandidate(stack))return stack;
        String selected=ItemData.stringValue(stack,LbcTmtAbilities.ACTIVE,"");
        for(var property:LbcTmtAbilities.read(stack))if(property.id().equals(selected)){
            var source=LbcTmtComponents.source(game,property);
            var data=source.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            data.remove(LbcTmtAbilities.NATIVE);data.remove(LbcTmtComponents.KEY);
            source.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));return source;
        }
        return stack;
    }

    /** Called inside the chosen private scope against the actual used-hand stack, never a synthetic throw. */
    static boolean use(GameController game,ServerPlayer player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!LbcTmtAbilities.active(stack)||!thrownCandidate(stack)||!game.items().tmt().admitted(player))return false;
        return selected(stack).use(player.level(),player,hand).consumesAction();
    }

    /**
     * Real Consumable.onConsume already owns the criterion/stat/event and the one physical debit.
     * Its native PotionContents listener owns potion delivery. Invoke that exact listener for the
     * selected private drink here, before the outer consumable resumes with its original components.
     * Do not call another Consumable.onConsume/finishUsingItem or manually enumerate effects.
     */
    static void consumed(GameController game,ServerPlayer player,ItemStack stack){
        if(!LbcTmtAbilities.active(stack)||delivery(stack)!=Delivery.DRINK||!game.items().tmt().admitted(player))return;
        var contents=stack.get(DataComponents.POTION_CONTENTS);
        var drink=selected(stack).getDefaultInstance().getOrDefault(DataComponents.CONSUMABLE,Consumables.DEFAULT_DRINK);
        contents.onConsume(player.level(),player,stack,drink);
    }
}
