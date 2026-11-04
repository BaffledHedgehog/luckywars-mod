package me.skywars2.game;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;

/** Exact source jungle slab prototype, decoded once at item registration. */
public final class LbcCunnyJungleItem {
    public static final Item ITEM=create();
    private LbcCunnyJungleItem(){}
    public static void register(){}
    private static Item create(){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:cunny_jungle_slab"));
        var model=Identifier.parse("minecraft:jungle_slab");
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}.setId(key).durability(64).overrideDescription("just_a_jungle_slab_imba");
        try(var input=LbcCunnyJungleItem.class.getResourceAsStream("/data/lbc/cunny/jungle_prototype.json")){
            if(input==null)throw new IllegalStateException("Missing Cunny jungle source prototype");
            var entry=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
            if(!entry.get("name").getAsString().equals("minecraft:carrot_on_a_stick"))throw new IllegalStateException("Cunny source carrier changed");
            for(var raw:entry.getAsJsonArray("modifier")){var row=raw.getAsJsonObject();switch(row.get("type").getAsString()){
                case "minecraft:set_count"->{if(row.get("count").getAsInt()!=1)throw new IllegalStateException("Cunny source count changed");}
                case "minecraft:set_name"->properties.component(DataComponents.CUSTOM_NAME,ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,row.get("name")).getOrThrow());
                case "minecraft:set_custom_data"->properties.component(DataComponents.CUSTOM_DATA,CustomData.of(TagParser.parseCompoundFully(row.get("tag").getAsString())));
                case "minecraft:set_lore"->{var lines=new ArrayList<Component>();for(var line:row.getAsJsonArray("lore"))lines.add(ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,line).getOrThrow());properties.component(DataComponents.LORE,new ItemLore(lines));}
                case "minecraft:set_components"->{for(var component:row.getAsJsonObject("components").entrySet()){if(component.getKey().startsWith("!"))continue;var type=BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse(component.getKey()));if(type==null)throw new IllegalStateException("Cunny source component changed");component(properties,type,component.getValue());}}
                default->throw new IllegalStateException("Unsupported source Cunny prototype operation");
            }}
            return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,Items.CARROT_ON_A_STICK,true));
        }catch(Exception error){throw new IllegalStateException("Invalid source Cunny jungle item",error);}
    }
    private static <T> void component(Item.Properties properties,DataComponentType<T> type,JsonElement source){var value=source.deepCopy();properties.delayedComponent(type,lookup->type.codec().parse(lookup.createSerializationContext(JsonOps.INSTANCE),value).getOrThrow());}
    public static ItemStack normalize(ItemStack source){if(source.isEmpty()||source.is(ITEM)||ItemData.intValue(source,"cunny_slab",0)!=1)return source;var result=source.transmuteCopy(ITEM);result.applyComponents(source.getComponents());return result;}
}
