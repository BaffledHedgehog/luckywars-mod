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
public final class LbcTmtComponents {
    private LbcTmtComponents(){}
    static final String KEY="tmt_native_components";
    private static final ThreadLocal<java.util.IdentityHashMap<ItemStack,Scope>> ACTIVE=new ThreadLocal<>();
    private static final List<DataComponentType<?>> PAYLOADS=List.of(DataComponents.ENTITY_DATA,DataComponents.BUCKET_ENTITY_DATA,
            DataComponents.BLOCK_ENTITY_DATA,DataComponents.BLOCK_STATE,DataComponents.POTION_CONTENTS,
            DataComponents.POTION_DURATION_SCALE,DataComponents.FIREWORKS,DataComponents.CONTAINER,DataComponents.CONTAINER_LOOT);
    static Item nativeItem(CustomData data){
        var id=Identifier.tryParse(((ItemData.IntReader)(Object)data).skywars2$readString(LbcTmtAbilities.NATIVE,""));
        return id==null?Items.AIR:BuiltInRegistries.ITEM.getValue(id);
    }
    /** Only non-default authored payload differences need storage; normal native prototypes remain compact. */
    static void capture(GameController game,ItemStack source,CompoundTag data){capture(game,source,source.getItem(),data);}
    static void capture(GameController game,ItemStack source,Item nativeItem,CompoundTag data){capture(game.server().registryAccess(),source,nativeItem,data);}
    private static void capture(net.minecraft.core.RegistryAccess registries,ItemStack source,Item nativeItem,CompoundTag data){
        var defaults=nativeItem.getDefaultInstance();var patch=DataComponentPatch.builder();boolean changed=false;
        for(var type:PAYLOADS)if(!Objects.equals(source.get(type),defaults.get(type))){change(patch,type,source.get(type));changed=true;}
        if(changed)data.put(KEY,DataComponentPatch.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE),patch.build()).getOrThrow());
        else data.remove(KEY);
    }
    private static DataComponentPatch patch(GameController game,CustomData data){return patch(game.server().registryAccess(),data);}
    private static DataComponentPatch patch(net.minecraft.core.RegistryAccess registries,CustomData data){
        if(!((ItemData.IntReader)(Object)data).skywars2$contains(KEY))return DataComponentPatch.EMPTY;
        var tag=data.copyTag().getCompoundOrEmpty(KEY);
        return DataComponentPatch.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE),tag).getOrThrow();
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void change(DataComponentPatch.Builder builder,DataComponentType type,Object value){if(value==null)builder.remove(type);else builder.set(type,value);}
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void set(ItemStack stack,DataComponentType type,Object value){if(value==null)stack.remove(type);else stack.set(type,value);}
    static ItemStack source(GameController game,LbcTmtAbilities.Ability property){return source(game.server().registryAccess(),property);}
    private static ItemStack source(net.minecraft.core.RegistryAccess registries,LbcTmtAbilities.Ability property){
        var item=nativeItem(property.data());if(item==Items.AIR)return ItemStack.EMPTY;
        var result=item.getDefaultInstance().copyWithCount(1);var patch=patch(registries,property.data());
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
    private static final String PLACED_SOURCE="tmt_placed_source",PLACED_ABILITY="tmt_placed_ability";
    /** Vanilla stores unused item components on the block entity. Persist the outer source, never its borrowed view. */
    public static ItemStack placedComponents(net.minecraft.world.level.block.entity.BlockEntity block,ItemStack stack){
        if(!(block instanceof net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity)||!LbcTmtAbilities.active(stack))return stack;
        var nativeItem=LbcTmtAbilities.nativePredicateItem(stack);
        if(!(nativeItem instanceof net.minecraft.world.item.BlockItem blockItem)
                ||!(blockItem.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock))return stack;
        var outer=LbcTmtAbilities.physicalDrop(stack).copyWithCount(1);
        var clean=outer.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        var active=ItemData.stringValue(stack,LbcTmtAbilities.ACTIVE,"");
        var ops=block.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        clean.put(PLACED_SOURCE,ItemStack.CODEC.encodeStart(ops,outer).getOrThrow());clean.putString(PLACED_ABILITY,active);
        var result=stack.copy();result.set(DataComponents.CUSTOM_DATA,CustomData.of(clean));return result;
    }
    /** Keep the native container's current contents/key in its private property while restoring the physical carrier. */
    public static ItemStack placedDrop(net.minecraft.world.level.block.entity.BlockEntity block,ItemStack shell){
        if(!(block instanceof net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity)||block.getLevel()==null)return shell;
        var data=block.components().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(!data.contains(PLACED_SOURCE))return shell;
        var registries=block.getLevel().registryAccess();var ops=registries.createSerializationContext(NbtOps.INSTANCE);
        var result=ItemStack.CODEC.parse(ops,data.get(PLACED_SOURCE)).getOrThrow().copyWithCount(shell.getCount());
        String selected=data.getStringOr(PLACED_ABILITY,"");var slots=new ArrayList<LbcTmtAbilities.Ability>();
        var current=block.collectComponents();
        for(var ability:LbcTmtAbilities.read(result)){
            if(!ability.id().equals(selected)){slots.add(ability);continue;}
            var nativeSource=source(registries,ability);
            set(nativeSource,DataComponents.CONTAINER,current.get(DataComponents.CONTAINER));
            set(nativeSource,DataComponents.CONTAINER_LOOT,current.get(DataComponents.CONTAINER_LOOT));
            var payload=ability.data().copyTag();capture(registries,nativeSource,nativeItem(ability.data()),payload);
            slots.add(new LbcTmtAbilities.Ability(ability.id(),CustomData.of(payload)));
        }
        var root=result.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        LbcTmtAbilities.write(root,slots);result.set(DataComponents.CUSTOM_DATA,CustomData.of(root));return result;
    }
    private record Value(DataComponentType<?> type,Object previous,Object next){}
    static final class Scope implements AutoCloseable {
        private final ItemStack stack;private final List<Value> saved;
        private final GameController game;private final Item item;
        private Scope(GameController game,ItemStack stack,Item item,List<Value> saved){this.game=game;this.stack=stack;this.item=item;this.saved=saved;}
        boolean changed(){if(stack.isEmpty())return false;for(var value:saved)if(!Objects.equals(stack.get(value.type),value.next))return true;return false;}
        void remember(CompoundTag payload){if(game!=null&&changed())capture(game,stack,item,payload);}
        public void close(){try{for(var value:saved)if(!Objects.equals(stack.get(value.type),value.previous))set(stack,value.type,value.previous);}
            finally{var active=ACTIVE.get();if(active!=null){active.remove(stack);if(active.isEmpty())ACTIVE.remove();}}}
    }
}
