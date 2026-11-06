package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/** Private native payload components, separate from generated stats, equipment and appearance. */
final class LbcTmtComponents {
    private LbcTmtComponents(){}
    static final String KEY="tmt_native_components";
    private static final ThreadLocal<java.util.IdentityHashMap<ItemStack,Scope>> ACTIVE=new ThreadLocal<>();
    private static final List<DataComponentType<?>> PAYLOADS=List.of(DataComponents.ENTITY_DATA,DataComponents.BUCKET_ENTITY_DATA,
            DataComponents.BLOCK_ENTITY_DATA,DataComponents.BLOCK_STATE,DataComponents.POTION_CONTENTS,
            DataComponents.POTION_DURATION_SCALE,DataComponents.FIREWORKS,DataComponents.CONTAINER);
    static Item nativeItem(CustomData data){
        var id=Identifier.tryParse(((ItemData.IntReader)(Object)data).skywars2$readString(LbcTmtAbilities.NATIVE,""));
        return id==null?Items.AIR:BuiltInRegistries.ITEM.getValue(id);
    }
    /** Only non-default authored payload differences need storage; normal native prototypes remain compact. */
    static void capture(GameController game,ItemStack source,CompoundTag data){capture(game,source,source.getItem(),data);}
    static void capture(GameController game,ItemStack source,Item nativeItem,CompoundTag data){
        var defaults=nativeItem.getDefaultInstance();var patch=DataComponentPatch.builder();boolean changed=false;
        for(var type:PAYLOADS)if(!Objects.equals(source.get(type),defaults.get(type))){change(patch,type,source.get(type));changed=true;}
        if(changed)data.put(KEY,DataComponentPatch.CODEC.encodeStart(game.server().registryAccess().createSerializationContext(NbtOps.INSTANCE),patch.build()).getOrThrow());
        else data.remove(KEY);
    }
    private static DataComponentPatch patch(GameController game,CustomData data){
        if(!((ItemData.IntReader)(Object)data).skywars2$contains(KEY))return DataComponentPatch.EMPTY;
        var tag=data.copyTag().getCompoundOrEmpty(KEY);
        return DataComponentPatch.CODEC.parse(game.server().registryAccess().createSerializationContext(NbtOps.INSTANCE),tag).getOrThrow();
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void change(DataComponentPatch.Builder builder,DataComponentType type,Object value){if(value==null)builder.remove(type);else builder.set(type,value);}
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void set(ItemStack stack,DataComponentType type,Object value){if(value==null)stack.remove(type);else stack.set(type,value);}
    static ItemStack source(GameController game,LbcTmtAbilities.Ability property){
        var item=nativeItem(property.data());if(item==Items.AIR)return ItemStack.EMPTY;
        var result=item.getDefaultInstance().copyWithCount(1);var patch=patch(game,property.data());
        var defaults=item.getDefaultInstance().getComponents();for(var type:PAYLOADS)set(result,type,patch.get(defaults,type));
        result.set(DataComponents.CUSTOM_DATA,property.data());return result;
    }
    /** Projection borrows the actual stack; source copies keep their own resolved payload for later owners. */
    static Scope project(GameController game,ItemStack stack,CustomData property){
        var item=nativeItem(property);if(game==null||item==Items.AIR)return new Scope(game,stack,item,List.of());
        var patch=patch(game,property);var defaults=item.getDefaultInstance().getComponents();var saved=new ArrayList<Value>();
        for(var type:PAYLOADS)saved.add(new Value(type,stack.get(type),patch.get(defaults,type)));
        var scope=new Scope(game,stack,item,List.copyOf(saved));
        try{for(var value:saved)if(!Objects.equals(value.previous,value.next))set(stack,value.type,value.next);
            var active=ACTIVE.get();if(active==null){active=new java.util.IdentityHashMap<>();ACTIVE.set(active);}active.put(stack,scope);return scope;
        }catch(Throwable error){scope.close();throw error;}
    }
    static void restoreOuterCopy(ItemStack source,ItemStack result){var active=ACTIVE.get();var scope=active==null?null:active.get(source);if(scope!=null)for(var value:scope.saved)set(result,value.type,value.previous);}
    private record Value(DataComponentType<?> type,Object previous,Object next){}
    static final class Scope implements AutoCloseable {
        private final ItemStack stack;private final List<Value> saved;
        private final GameController game;private final Item item;
        private Scope(GameController game,ItemStack stack,Item item,List<Value> saved){this.game=game;this.stack=stack;this.item=item;this.saved=saved;}
        void remember(CompoundTag payload){if(!stack.isEmpty()&&game!=null)for(var value:saved)if(!Objects.equals(stack.get(value.type),value.next)){capture(game,stack,item,payload);break;}}
        public void close(){try{for(var value:saved)if(!Objects.equals(stack.get(value.type),value.previous))set(stack,value.type,value.previous);}
            finally{var active=ACTIVE.get();if(active!=null){active.remove(stack);if(active.isEmpty())ACTIVE.remove();}}}
    }
}
