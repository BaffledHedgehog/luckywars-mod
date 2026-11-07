package me.skywars2.game;

import java.util.List;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;

/** Native stack components which compose without replacing the carrier's input, stats or appearance. */
final class LbcTmtEquipment {
    private LbcTmtEquipment(){}
    static final String KEY="tmt_equipment_components";
    record Property(String name,DataComponentType<?> type) {
        String id(){return "native/component/"+name;}
        boolean present(ItemStack source){return source.has(type);}
    }
    static final List<Property> PROPERTIES=List.of(
            new Property("glider",DataComponents.GLIDER),
            new Property("unbreakable",DataComponents.UNBREAKABLE),
            new Property("damage_resistant",DataComponents.DAMAGE_RESISTANT),
            new Property("tool",DataComponents.TOOL));

    /** Bootstrap only: retain the actual value, including authored non-default component patches. */
    static CompoundTag capture(GameController game,ItemStack source,Property property){
        var patch=DataComponentPatch.builder();capture(patch,source,property.type());var data=new CompoundTag();
        data.put(KEY,DataComponentPatch.CODEC.encodeStart(game.server().registryAccess().createSerializationContext(NbtOps.INSTANCE),patch.build()).getOrThrow());
        return data;
    }
    private static <T>void capture(DataComponentPatch.Builder patch,ItemStack source,DataComponentType<T> type){patch.set(type,source.get(type));}

    /** Generation only. Native equipment/mining/fire checks consume the real component afterward. */
    static void apply(GameController game,ItemStack stack){
        for(var selected:LbcTmtAbilities.read(stack))for(var property:PROPERTIES)if(selected.id().equals(property.id())){
            var data=(ItemData.IntReader)(Object)selected.data();if(!data.skywars2$contains(KEY))continue;
            var patch=DataComponentPatch.CODEC.parse(game.server().registryAccess().createSerializationContext(NbtOps.INSTANCE),selected.data().copyTag().get(KEY)).getOrThrow();
            copy(stack,patch,property.type());
        }
    }
    private static <T>void copy(ItemStack target,DataComponentPatch patch,DataComponentType<T> type){var value=patch.get(DataComponentMap.EMPTY,type);if(value!=null)target.set(type,value);}
}
