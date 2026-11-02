package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Skill 7's finite item-point reserve. The accepted-damage caller owns live skill admission. */
public final class LbcArmorReserveSkill {
    public static final double ARMOR_HP_PER_POINT=2.0;
    public static final double TOUGHNESS_HP_PER_POINT=5.0;
    private static final List<EquipmentSlot> WORN=List.of(
            EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);

    private LbcArmorReserveSkill() { }

    /** Shared native/raw-HP entry; equipment changes use the native cache and attribute publication. */
    public static float apply(GameController game,ServerPlayer player,DamageSource source,float damage){
        if(!game.lbc().hasSkill(player,7)||game.server().tickRateManager().isFrozen()
                ||!game.server().tickRateManager().runsNormally()||game.items().wands().timeStopped()
                ||game.items().wands().inputTimeStopped(player)||game.items().timeStop().stopped(player.level()))return damage;
        float remaining=absorb(player,source,damage);
        if(remaining!=damage){
            ((me.skywars2.mixin.ArmorReserveMixin.Equipment)(Object)player).skywars2$refreshArmorReserveEquipment();
            player.getInventory().setChanged();
        }
        return remaining;
    }

    /**
     * Call once for a genuinely accepted positive hit, after ordinary mitigation and before HP/absorption debit.
     * A positive reserve blocks the whole hit, including its terminal overflow. Returning zero must NOT cancel
     * native hit reactions, knockback, criteria, or invulnerability bookkeeping in the surrounding damage call.
     * This method neither selects the skill nor grants/clears any Resistance effect.
     */
    public static float absorb(ServerPlayer player,DamageSource source,float postMitigationDamage) {
        if(player==null||source==null||!(postMitigationDamage>0)||!Float.isFinite(postMitigationDamage)
                ||source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)||source.is(DamageTypes.OUTSIDE_BORDER)
                ||MatchPlayerCheckpoint.restoring(player))
            return postMitigationDamage;
        var pieces=new ArrayList<Piece>(WORN.size());
        boolean positive=false;
        for(var slot:WORN) {
            ItemStack stack=player.getItemBySlot(slot);
            if(stack.isEmpty())continue;
            // Includes the actual item's native defaults without a historical armor-type lookup table.
            var attributes=stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);
            var piece=new Piece(stack,slot,attributes);
            pieces.add(piece);
            positive|=piece.armor>0||piece.toughness>0;
        }
        if(!positive)return postMitigationDamage;
        double remaining=drain(pieces,Attributes.ARMOR_TOUGHNESS,postMitigationDamage/TOUGHNESS_HP_PER_POINT)
                *TOUGHNESS_HP_PER_POINT;
        if(remaining>0)drain(pieces,Attributes.ARMOR,remaining/ARMOR_HP_PER_POINT);
        for(var piece:pieces)piece.publish();
        return 0;
    }

    private static final class Piece {
        final ItemStack stack;
        final EquipmentSlot slot;
        final ItemAttributeModifiers original;
        final double armor,toughness;
        double spentArmor,spentToughness;

        Piece(ItemStack stack,EquipmentSlot slot,ItemAttributeModifiers original) {
            this.stack=stack;this.slot=slot;this.original=original;
            armor=points(original,slot,Attributes.ARMOR);
            toughness=points(original,slot,Attributes.ARMOR_TOUGHNESS);
        }
        double capacity(Holder<Attribute> attribute) {
            return attribute.equals(Attributes.ARMOR)?armor:toughness;
        }
        void spend(Holder<Attribute> attribute,double amount) {
            if(attribute.equals(Attributes.ARMOR))spentArmor=amount;else spentToughness=amount;
        }
        void publish() {
            if(!(spentArmor>0||spentToughness>0))return;
            var entries=new ArrayList<ItemAttributeModifiers.Entry>(original.modifiers().size());
            for(var entry:original.modifiers()) {
                double capacity=capacity(entry.attribute());
                double spent=entry.attribute().equals(Attributes.ARMOR)?spentArmor:spentToughness;
                if(eligible(entry,slot,Attributes.ARMOR)||eligible(entry,slot,Attributes.ARMOR_TOUGHNESS)) {
                    if(spent>0) {
                        // Each piece pays proportionally across ALL its matching positive point entries.
                        // Full exhaustion is exact zero, preventing fractional immortal residue.
                        double amount=spent>=capacity?0:entry.modifier().amount()*(1-spent/capacity);
                        var modifier=new AttributeModifier(entry.modifier().id(),amount,entry.modifier().operation());
                        entries.add(new ItemAttributeModifiers.Entry(entry.attribute(),modifier,entry.slot(),entry.display()));
                        continue;
                    }
                }
                // Percentage coefficients, penalties, other slots/attributes and displays survive unchanged.
                entries.add(entry);
            }
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(List.copyOf(entries)));
        }
    }

    private static boolean eligible(ItemAttributeModifiers.Entry entry,EquipmentSlot slot,Holder<Attribute> attribute) {
        return entry.attribute().equals(attribute)&&entry.slot().test(slot)
                &&entry.modifier().operation()==AttributeModifier.Operation.ADD_VALUE
                &&entry.modifier().amount()>0&&Double.isFinite(entry.modifier().amount());
    }
    private static double points(ItemAttributeModifiers attributes,EquipmentSlot slot,Holder<Attribute> attribute) {
        double sum=0;
        for(var entry:attributes.modifiers())if(eligible(entry,slot,attribute))sum+=entry.modifier().amount();
        return sum;
    }

    /** Equal share across eligible pieces; exhausted pieces' unpaid share flows to those still holding points. */
    private static double drain(List<Piece> pieces,Holder<Attribute> attribute,double requested) {
        double remaining=requested;
        var active=new ArrayList<Piece>(pieces.size());
        for(var piece:pieces)if(piece.capacity(attribute)>0)active.add(piece);
        while(remaining>0&&!active.isEmpty()) {
            double share=remaining/active.size();
            boolean exhausted=false;
            // Pay only fully exhausted pieces first, then redistribute the remainder equally.
            for(int i=active.size()-1;i>=0;i--) {
                var piece=active.get(i);
                if(piece.capacity(attribute)<=share) {
                    double amount=piece.capacity(attribute);
                    piece.spend(attribute,amount);
                    remaining=Math.max(0,remaining-amount);
                    active.remove(i);exhausted=true;
                }
            }
            if(!exhausted) {
                for(var piece:active)piece.spend(attribute,share);
                return 0;
            }
        }
        return remaining;
    }
}
