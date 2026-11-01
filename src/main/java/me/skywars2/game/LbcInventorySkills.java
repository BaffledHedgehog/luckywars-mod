package me.skywars2.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Finite equipment component mutations; the caller owns skill/config/lifecycle admission and publication. */
public final class LbcInventorySkills {
    private LbcInventorySkills() { }
    private record ArmorSlot(EquipmentSlot slot,EquipmentSlotGroup group,Identifier toughnessId) { }
    // Canonical source IDs from skills/tanner_resist_change_inv_{head,chest,legs,feet}.
    private static final List<ArmorSlot> ARMOR=List.of(
            new ArmorSlot(EquipmentSlot.HEAD,EquipmentSlotGroup.HEAD,Identifier.withDefaultNamespace("nfw9ru2u38987y1ris")),
            new ArmorSlot(EquipmentSlot.CHEST,EquipmentSlotGroup.CHEST,Identifier.withDefaultNamespace("nfwfifadiwieriseris")),
            new ArmorSlot(EquipmentSlot.LEGS,EquipmentSlotGroup.LEGS,Identifier.withDefaultNamespace("newrwere11fis")),
            new ArmorSlot(EquipmentSlot.FEET,EquipmentSlotGroup.FEET,Identifier.withDefaultNamespace("nfwf2ifadi4123is")));
    private static final List<ResourceKey<Enchantment>> ARMORY_ENCHANTMENTS=List.of(
            Enchantments.BLAST_PROTECTION,Enchantments.FEATHER_FALLING,Enchantments.FIRE_PROTECTION,
            Enchantments.PROJECTILE_PROTECTION,Enchantments.PROTECTION,Enchantments.RESPIRATION,
            Enchantments.SOUL_SPEED,Enchantments.SWIFT_SNEAK,Enchantments.THORNS,Enchantments.UNBREAKING);

    public static boolean apply(ServerPlayer player,int skill) {
        if(player==null)return false;
        return switch(skill) {
            case 4 -> tanner(player);
            case 8 -> armory(player);
            case 11 -> hands(player);
            default -> false;
        };
    }
    private static boolean tanner(ServerPlayer player) {
        boolean changed=false;
        // Deliberately use each actual slot: source chest/legs guards were swapped; author approved the correction.
        for(var slot:ARMOR) {
            ItemStack stack=player.getItemBySlot(slot.slot);
            if(stack.isEmpty())continue;
            if(stack.has(DataComponents.DAMAGE)&&!stack.has(DataComponents.UNBREAKABLE)) {
                stack.set(DataComponents.UNBREAKABLE,Unit.INSTANCE);changed=true;
            }
            if(marked(stack,"tanner_upgraded"))continue;
            // ItemStack.get includes actual native item defaults. Starting from EMPTY would erase vanilla armor stats.
            var attributes=stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);
            var entries=new ArrayList<>(attributes.modifiers());
            boolean present=entries.stream().anyMatch(entry->entry.attribute().equals(Attributes.ARMOR_TOUGHNESS)
                    &&entry.modifier().id().equals(slot.toughnessId)&&entry.modifier().amount()==3
                    &&entry.modifier().operation()==AttributeModifier.Operation.ADD_VALUE&&entry.slot()==slot.group);
            if(!present) {
                Set<Identifier> occupied=new HashSet<>();
                for(var entry:entries)occupied.add(entry.modifier().id());
                Identifier id=freeId(slot.toughnessId,occupied);
                entries.add(new ItemAttributeModifiers.Entry(Attributes.ARMOR_TOUGHNESS,
                        new AttributeModifier(id,3,AttributeModifier.Operation.ADD_VALUE),slot.group));
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(List.copyOf(entries)));
            }
            mark(stack,"tanner_upgraded");changed=true;
        }
        return changed;
    }
    private static boolean armory(ServerPlayer player) {
        var registry=player.registryAccess().lookup(Registries.ENCHANTMENT).orElse(null);
        if(registry==null)return false;
        var enchantments=new ArrayList<Holder<Enchantment>>(ARMORY_ENCHANTMENTS.size());
        // Resolve the complete bounded source set before changing anything: failed registry lookup never marks an item.
        for(var key:ARMORY_ENCHANTMENTS) {
            var holder=registry.get(key).orElse(null);
            if(holder==null)return false;
            enchantments.add(holder);
        }
        boolean changed=false;
        for(var slot:ARMOR) {
            ItemStack stack=player.getItemBySlot(slot.slot);
            if(stack.isEmpty()||marked(stack,"armory_upgraded"))continue;
            boolean parkedProtection=LbcErosionSuppression.upgradeParkedProtection(stack);
            var original=stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);
            var upgraded=new ItemEnchantments.Mutable(original);
            for(var enchantment:enchantments) {
                if(parkedProtection&&enchantment.is(Enchantments.PROTECTION))continue;
                int current=original.getLevel(enchantment);
                // Keep the project's native full-positive-int policy; never reproduce the legacy NBT byte wrap.
                // Mutable.set also retains the active runtime's storage cap; ordinary definition maxima do not apply.
                upgraded.set(enchantment,current==Integer.MAX_VALUE?Integer.MAX_VALUE:current+1);
            }
            var result=upgraded.toImmutable();
            if(!result.equals(original))stack.set(DataComponents.ENCHANTMENTS,result);
            mark(stack,"armory_upgraded");changed=true;
        }
        return changed;
    }
    private static boolean hands(ServerPlayer player) {
        // Both actual held identities are updated; no inventory traversal or persistent producer is needed.
        boolean main=master(player.getItemBySlot(EquipmentSlot.MAINHAND));
        boolean off=master(player.getItemBySlot(EquipmentSlot.OFFHAND));
        return main||off;
    }
    private static boolean master(ItemStack stack) {
        if(stack.isEmpty()||marked(stack,"offhand_mastered"))return false;
        var original=stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);
        boolean eligible=original.modifiers().stream().anyMatch(entry->entry.slot()==EquipmentSlotGroup.MAINHAND
                ||entry.slot()==EquipmentSlotGroup.OFFHAND||splitLeftPenalty(entry));
        // Source accepts any explicit attribute carrier; only its implicit-vanilla fallback listed weapon types.
        // Native component defaults also cover current copper/mace/spear without hardcoded historical weapon stats.
        if(!eligible)return false;
        Set<Identifier> occupied=new HashSet<>();
        for(var entry:original.modifiers())occupied.add(entry.modifier().id());
        var entries=new ArrayList<ItemAttributeModifiers.Entry>(original.modifiers().size()*2);
        int index=0;
        for(var entry:original.modifiers()) {
            if(splitLeftPenalty(entry)) {
                // A shared HAND/ANY entry cannot be weakened globally: split its exact native slot coverage.
                // Preserve its original ID/value/display in every non-left slot, including BODY and SADDLE.
                for(var slot:EquipmentSlot.values())if(entry.slot().test(slot)) {
                    var exact=EquipmentSlotGroup.bySlot(slot);
                    var modifier=entry.modifier();
                    if(slot==EquipmentSlot.OFFHAND)modifier=new AttributeModifier(derivedId(entry,exact,index,occupied),
                            modifier.amount(),modifier.operation());
                    entries.add(leftSpeed(new ItemAttributeModifiers.Entry(entry.attribute(),modifier,exact,entry.display())));
                }
            }else entries.add(leftSpeed(entry));
            index++;
        }
        index=0;
        // Read ONLY the original list: OFFHAND->MAINHAND retains the full original coefficient before weakening left.
        for(var entry:original.modifiers()) {
            EquipmentSlotGroup destination=entry.slot()==EquipmentSlotGroup.MAINHAND?EquipmentSlotGroup.OFFHAND
                    :entry.slot()==EquipmentSlotGroup.OFFHAND?EquipmentSlotGroup.MAINHAND:null;
            if(destination!=null) {
                var modifier=new AttributeModifier(derivedId(entry,destination,index,occupied),
                        entry.modifier().amount(),entry.modifier().operation());
                entries.add(leftSpeed(new ItemAttributeModifiers.Entry(entry.attribute(),modifier,destination,entry.display())));
            }
            index++;
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(List.copyOf(entries)));
        mark(stack,"offhand_mastered");return true;
    }
    private static boolean splitLeftPenalty(ItemAttributeModifiers.Entry entry) {
        return (entry.slot()==EquipmentSlotGroup.HAND||entry.slot()==EquipmentSlotGroup.ANY)
                &&entry.attribute().equals(Attributes.ATTACK_SPEED)&&entry.modifier().amount()<0;
    }
    private static Identifier derivedId(ItemAttributeModifiers.Entry entry,EquipmentSlotGroup destination,int index,
                                         Set<Identifier> occupied) {
        Identifier attribute=entry.attribute().unwrapKey().map(ResourceKey::identifier)
                .orElse(Identifier.fromNamespaceAndPath("skywars2","direct_attribute"));
        Identifier source=entry.modifier().id();
        Identifier desired=Identifier.fromNamespaceAndPath("skywars2","inventory_skills/offhand/"
                +destination.getSerializedName()+"/"+attribute.getNamespace()+"/"+attribute.getPath()
                +"/"+source.getNamespace()+"/"+source.getPath()+"/"+index);
        return freeId(desired,occupied);
    }
    private static ItemAttributeModifiers.Entry leftSpeed(ItemAttributeModifiers.Entry entry) {
        var modifier=entry.modifier();
        if(entry.slot()!=EquipmentSlotGroup.OFFHAND||!entry.attribute().equals(Attributes.ATTACK_SPEED)
                ||modifier.amount()>=0)return entry;
        // Final author ruling: reduce the left penalty by70%, retaining30% for all three operation coefficients.
        // MAINHAND copies keep the original coefficient; shared groups are split above, and all other attributes stay exact.
        return new ItemAttributeModifiers.Entry(entry.attribute(),new AttributeModifier(modifier.id(),
                modifier.amount()*.3,modifier.operation()),entry.slot(),entry.display());
    }
    private static boolean marked(ItemStack stack,String key) {
        return ItemData.contains(stack,key);
    }
    private static void mark(ItemStack stack,String key) {
        CustomData.update(DataComponents.CUSTOM_DATA,stack,data->data.putInt(key,1));
    }
    private static Identifier freeId(Identifier desired,Set<Identifier> occupied) {
        Identifier id=desired;
        for(int suffix=1;occupied.contains(id);suffix++)
            id=Identifier.fromNamespaceAndPath(desired.getNamespace(),desired.getPath()+"_"+suffix);
        occupied.add(id);return id;
    }
}
