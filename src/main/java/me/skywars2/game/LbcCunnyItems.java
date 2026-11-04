package me.skywars2.game;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;

/** Exact source prototypes decoded once, never translated through client display text. */
public final class LbcCunnyItems {
    static final JsonObject SOURCE=source();
    public static final List<Item> SLABS=slabs();
    public static final Item NAHIDA=register("nahida_dakimakura",SOURCE.getAsJsonObject("imba/nahida_dakimakura")
            .getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject(),true);
    private LbcCunnyItems(){}
    public static void register(){}
    private static JsonObject source(){try(var in=LbcCunnyItems.class.getResourceAsStream("/skywars2/cunny_items.json")){
        if(in==null)throw new IllegalStateException("Missing Cunny prototypes");
        return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
    }catch(Exception e){throw new IllegalStateException("Invalid Cunny prototypes",e);}}
    private static List<Item> slabs(){var result=new ArrayList<Item>();var entries=SOURCE.getAsJsonObject("imba/cunny_slabs")
            .getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
        if(entries.size()!=22)throw new IllegalStateException("Expected22 authored Cunny outcomes");
        for(int n=0;n<22;n++)result.add(register("cunny_"+n,entries.get(n).getAsJsonObject(),false));return List.copyOf(result);}
    private static Item register(String id,JsonObject entry,boolean nahida){try{
        Item carrier=BuiltInRegistries.ITEM.getValue(Identifier.parse(entry.get("name").getAsString()));
        Identifier model=null;for(var element:entry.getAsJsonArray("functions")){var row=element.getAsJsonObject();
            if(row.get("function").getAsString().equals("minecraft:set_components")){
                var components=row.getAsJsonObject("components");if(components.has("minecraft:item_model"))model=Identifier.parse(components.get("minecraft:item_model").getAsString());}}
        if(model==null)throw new IllegalStateException("Missing source model");final var nativeModel=model;
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+id));
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return nativeModel;}}.setId(key);
        for(var element:entry.getAsJsonArray("functions")){var row=element.getAsJsonObject();switch(row.get("function").getAsString()){
            case "minecraft:set_count"->{if(row.get("count").getAsInt()!=1)throw new IllegalStateException("Unexpected prototype count");}
            case "minecraft:set_components"->{for(var component:row.getAsJsonObject("components").entrySet()){
                String name=component.getKey();if(name.startsWith("!")){if(!name.equals("!minecraft:max_damage"))throw new IllegalStateException("Unknown removed component");continue;}
                if(!name.contains(":"))name="minecraft:"+name;
                var type=BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse(name));if(type==null)throw new IllegalStateException(name);component(properties,type,component.getValue());}}
            case "minecraft:set_name"->properties.component(DataComponents.CUSTOM_NAME,ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,row.get("name")).getOrThrow());
            case "minecraft:set_custom_data"->properties.component(DataComponents.CUSTOM_DATA,CustomData.of(TagParser.parseCompoundFully(row.get("tag").getAsString())));
            case "minecraft:set_lore"->{if(!row.get("mode").getAsString().equals("replace_all"))throw new IllegalStateException("Unknown lore operation");var lines=new ArrayList<Component>();for(var line:row.getAsJsonArray("lore"))lines.add(ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,line).getOrThrow());properties.component(DataComponents.LORE,new ItemLore(lines));}
            default->throw new IllegalStateException("Unknown Cunny prototype modifier");}}
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,carrier,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){var game=me.skywars2.SkyWars2.game();
                if(game==null||!(player instanceof ServerPlayer p))return InteractionResult.FAIL;
                if(nahida){if(!game.items().cunny().nahidaAllowed(p,hand))return InteractionResult.FAIL;p.startUsingItem(hand);return InteractionResult.CONSUME;}
                return game.items().cunny().use(p,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;}
            @Override public void onUseTick(Level level,LivingEntity living,ItemStack stack,int remaining){
                if(nahida&&living instanceof ServerPlayer p&&p.getUseItem()==stack){var game=me.skywars2.SkyWars2.game();if(game!=null)game.items().cunny().nahidaUseTick(p);}}
        });
    }catch(Exception e){throw new IllegalStateException("Invalid Cunny prototype "+id,e);}}
    private static <T> void component(Item.Properties p,DataComponentType<T> type,JsonElement value){var frozen=value.deepCopy();p.delayedComponent(type,lookup->type.codec().parse(lookup.createSerializationContext(JsonOps.INSTANCE),frozen).getOrThrow());}
    public static int variant(ItemStack stack){var data=LbcFartus.data(stack);return !stack.isEmpty()&&data.contains("cunny")?data.getIntOr("cunny",-1):-1;}
    public static boolean slab(ItemStack stack){int n=variant(stack);return n>=0&&n<22;}
    public static boolean nahida(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("nahida_dakimakura",0)==1;}
}
