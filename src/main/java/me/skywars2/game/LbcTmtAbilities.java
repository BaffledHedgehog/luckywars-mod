package me.skywars2.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Bounded, persistent TMT property slots. Native owners run against the actual held stack. */
final class LbcTmtAbilities {
    private LbcTmtAbilities(){}
    static final String SLOTS="tmt_abilities",ACTIVE="tmt_active_ability";
    private static final List<String> PARAMETERS=List.of("upgraded","mode","type");
    private static final List<String> IDENTITY=List.of(SLOTS,ACTIVE,"tmt_item","tmt_texture_seed");
    // Captured source stacks retain ACTIVE as context. Runtime reentry is never a serialized lock.
    private static final ThreadLocal<java.util.Set<ItemStack>> SCOPES=new ThreadLocal<>();
    static boolean active(ItemStack stack){var scopes=SCOPES.get();return scopes!=null&&scopes.contains(stack);}

    record Ability(String id,CustomData data) {
        Ability {
            if(id==null||id.isBlank()||id.length()>128||data==null)throw new IllegalArgumentException("Invalid TMT ability");
            var payload=data.copyTag();
            for(var key:IDENTITY)payload.remove(key);
            for(var key:PARAMETERS)if(!payload.contains(key))payload.putInt(key,0);
            data=CustomData.of(payload);
        }
    }

    /** Last occurrence wins without giving repeat draws a second execution slot. */
    static void write(CompoundTag root,List<Ability> rolls) {
        if(rolls.size()>7)throw new IllegalArgumentException("Too many TMT ability rolls");
        var unique=new LinkedHashMap<String,Ability>();
        for(var ability:rolls){unique.remove(ability.id);unique.put(ability.id,ability);}
        var list=new ListTag();
        for(var ability:unique.values()){
            var payload=ability.data.copyTag();var merged=payload.copy();
            for(var key:PARAMETERS)merged.remove(key);
            root.merge(merged);
            list.add(entry(ability.id,payload));
        }
        for(var key:PARAMETERS)root.remove(key);
        root.remove(ACTIVE);root.put(SLOTS,list);
    }
    static List<Ability> read(ItemStack stack) {
        if(stack.isEmpty()||ItemData.intValue(stack,"tmt_item",0)!=1)return List.of();
        return read(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag());
    }
    static List<Ability> read(CompoundTag root) {
        var list=root.getListOrEmpty(SLOTS);if(list.size()>7)return List.of();
        var unique=new LinkedHashMap<String,Ability>();
        for(int i=0;i<list.size();i++){
            var slot=list.getCompoundOrEmpty(i);String id=slot.getStringOr("id","");
            if(id.isBlank()||id.length()>128||!slot.contains("data"))continue;
            var ability=new Ability(id,CustomData.of(slot.getCompoundOrEmpty("data")));
            unique.remove(id);unique.put(id,ability);
        }
        return List.copyOf(unique.values());
    }

    /** No hand replacement or copied-item execution. Only this callback's property parameters are projected. */
    static boolean invoke(ItemStack stack,String id,BooleanSupplier action) {
        if(stack.isEmpty()||active(stack))return false;
        var original=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        var slots=read(original);Ability selected=null;
        for(var ability:slots)if(ability.id.equals(id)){selected=ability;break;}
        if(selected==null)return false;
        var payload=selected.data.copyTag();var effective=original.copy();
        // The aggregate flags classify the outer item, but cannot impersonate a sibling
        // inside a native owner's first-match dispatch or self-effect branch.
        for(var ability:slots)for(var key:ability.data.copyTag().keySet())
            if(!IDENTITY.contains(key))effective.remove(key);
        for(var key:PARAMETERS)effective.remove(key);
        effective.merge(payload);effective.putString(ACTIVE,id);
        var before=effective.copy();
        var scopes=SCOPES.get();
        if(scopes==null){scopes=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());SCOPES.set(scopes);}
        scopes.add(stack);
        try{stack.set(DataComponents.CUSTOM_DATA,CustomData.of(effective));return action.getAsBoolean();}
        finally{
            try{
            if(!stack.isEmpty()){
                var after=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                // Native mode/version/counter writes belong to this property, not its siblings.
                for(var key:after.keySet())if(!IDENTITY.contains(key)
                        &&(payload.contains(key)||!Objects.equals(before.get(key),after.get(key))))
                    payload.put(key,after.get(key).copy());
                for(var key:new ArrayList<>(payload.keySet()))if(!after.contains(key))payload.remove(key);
                var list=new ListTag();
                for(var ability:slots)list.add(entry(ability.id,ability.id.equals(id)?payload:ability.data.copyTag()));
                original.put(SLOTS,list);original.remove(ACTIVE);
                for(var key:PARAMETERS)original.remove(key);
                stack.set(DataComponents.CUSTOM_DATA,CustomData.of(original));
            }
            }finally{scopes.remove(stack);if(scopes.isEmpty())SCOPES.remove();}
        }
    }
    private static CompoundTag entry(String id,CompoundTag payload) {
        var entry=new CompoundTag();entry.putString("id",id);entry.put("data",payload);return entry;
    }
}
