package me.skywars2.game;

import com.google.gson.JsonParser;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;

/** Food identity/presentation only; native consumption and LbcNamedFoods retain every existing effect. */
public final class LbcNamedFoodItems {
    record Entry(String id,String flag,String definition,Item carrier,Item item){}
    private static final List<Entry> ENTRIES=load();
    private static final Map<Item,Item> CARRIERS=carriers();
    private LbcNamedFoodItems(){}
    public static void register(){}
    static List<Entry> entries(){return ENTRIES;}
    public static Item carrier(ItemStack stack){return CARRIERS.getOrDefault(stack.getItem(),stack.getItem());}
    private static Map<Item,Item> carriers(){var map=new IdentityHashMap<Item,Item>();for(var entry:ENTRIES)map.put(entry.item(),entry.carrier());return Collections.unmodifiableMap(map);}
    private static List<Entry> load(){
        try(var stream=LbcNamedFoodItems.class.getResourceAsStream("/skywars2/named_food_items.json")){
            if(stream==null)throw new IllegalStateException("Missing named food manifest");
            var rows=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonArray();
            var entries=new ArrayList<Entry>();var names=new HashSet<String>();
            for(var value:rows){
                var row=value.getAsJsonObject();var id=row.get("id").getAsString();
                if(!names.add(id))throw new IllegalStateException("Duplicate named food "+id);
                var carrier=BuiltInRegistries.ITEM.getValue(Identifier.parse(row.get("carrier").getAsString()));
                if(carrier!=Items.GOLDEN_APPLE&&carrier!=Items.STICK)throw new IllegalStateException("Unexpected named food carrier "+id);
                var definition=row.get("definition").getAsString();
                var properties=LbcUtilityItemPrototypes.deferredProperties("skywars2:food/"+id,"/"+definition,carrier,64);
                var item=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:food/"+id),new SimplePolymerItem(properties,carrier,true));
                entries.add(new Entry(id,row.get("flag").getAsString(),definition,carrier,item));
            }
            if(entries.size()!=40)throw new IllegalStateException("Incomplete named food manifest");
            return List.copyOf(entries);
        }catch(java.io.IOException e){throw new IllegalStateException("Invalid named food manifest",e);}
    }
}
