package me.skywars2.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.util.ArrayList;
import java.util.List;

/** One-time migration of authored defaults. Never call from tick, attack or inventory hooks. */
public final class SourceKnockbackBalance {
    public static final double MAX_COMBINED = 5;
    // First stationary cardinal native full-hit saturation in1.21.4: .4 + .6*6 >=3.9.
    // This is a source balance approximation; native runtime velocity/enchantments stay unrestricted.
    public static final int MAX_SOURCE_PUNCH = 6;
    private SourceKnockbackBalance() { }

    /** Mutates a freshly parsed/DFU-converted ordinary source entity before construction. */
    public static CompoundTag entity(CompoundTag entity) {
        var equipment=entity.getCompoundOrEmpty("equipment");
        var main=equipment.getCompoundOrEmpty("mainhand");
        var mainComponents=main.getCompoundOrEmpty("components");
        boolean imba=imba(mainComponents);
        if(!imba) {
            var bases=new ArrayList<CompoundTag>();
            for(var tag:entity.getListOrEmpty("attributes"))
                if(tag instanceof CompoundTag attribute && attribute.getStringOr("id", "").equals("minecraft:attack_knockback")) bases.add(attribute);
            balance(mainComponents,bases);
        }
        for(String slot:equipment.keySet()) {
            var item=equipment.getCompoundOrEmpty(slot);
            var components=item.getCompoundOrEmpty("components");
            if(!slot.equals("mainhand")&&!imba(components)) balance(components,List.of());
            // Source eggs inside equipment retain their independent source marker.
            if(!imba(components)&&components.get("minecraft:entity_data") instanceof CompoundTag nested) entity(nested);
        }
        for(var passenger:entity.getListOrEmpty("Passengers"))
            if(passenger instanceof CompoundTag child) entity(child);
        return entity;
    }

    private static boolean imba(CompoundTag components) {
        return components.getCompoundOrEmpty("minecraft:custom_data").getIntOr("imba",0)==1;
    }

    private static void balance(CompoundTag components,List<CompoundTag> bases) {
        var enchantments=components.getCompoundOrEmpty("minecraft:enchantments");
        // Support pre/post1.21.5 source encodings at this explicit migration boundary.
        if(enchantments.get("levels") instanceof CompoundTag levels) enchantments=levels;
        int punch=enchantments.getIntOr("minecraft:punch",0);
        if(punch>MAX_SOURCE_PUNCH) enchantments.putInt("minecraft:punch",MAX_SOURCE_PUNCH);
        int enchant=enchantments.getIntOr("minecraft:knockback",0);
        var modifiers=new ArrayList<CompoundTag>();
        var encoded=components.get("minecraft:attribute_modifiers");
        ListTag list=encoded instanceof ListTag entries?entries:
                encoded instanceof CompoundTag old?old.getListOrEmpty("modifiers"):new ListTag();
        for(var tag:list) if(tag instanceof CompoundTag modifier
                &&modifier.getStringOr("type", "").equals("minecraft:attack_knockback")
                &&List.of("mainhand","hand","any").contains(modifier.getStringOr("slot","any"))) {
            if(!modifier.getStringOr("operation", "").equals("add_value"))
                throw new IllegalArgumentException("Review non-additive authored attack_knockback before porting");
            modifiers.add(modifier);
        }
        double attribute=bases.stream().mapToDouble(a->a.getDoubleOr("base",0)).sum()
                +modifiers.stream().mapToDouble(a->a.getDoubleOr("amount",0)).sum();
        double excess=Math.max(0,attribute+enchant-MAX_COMBINED);
        // Authored mob base, then held modifiers, then the enchantment. Never turn a positive attribute negative.
        for(var base:bases) excess=reduce(base,"base",excess);
        for(var modifier:modifiers) excess=reduce(modifier,"amount",excess);
        if(excess>0&&enchant>0) enchantments.putInt("minecraft:knockback",Math.max(0,enchant-(int)Math.ceil(excess)));
    }

    private static double reduce(CompoundTag value,String key,double excess) {
        double original=value.getDoubleOr(key,0);
        double reduction=Math.min(Math.max(0,original),excess);
        if(reduction>0) value.putDouble(key,original-reduction);
        return excess-reduction;
    }
}
