package me.skywars2.game.genie;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.game.AttributePolicy;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.projectile.Projectile;

/** Immutable classpath/registry knowledge. Building this index never rolls loot or creates world entities. */
public final class GenieKnowledge {
    private GenieKnowledge(){}
    private record Profile(JsonObject value,String search){}
    private record Snapshot(String prefix,List<Profile> profiles){}
    private static volatile Snapshot cached;

    /** Called by the model worker, not by the server tick. Full compact index plus relevant prototype details. */
    public static String forWish(String wish,Collection<GenieBlueprint.TwistKind> recent){
        var snapshot=snapshot();
        var words=new LinkedHashSet<String>();
        for(var word:wish.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}_]+"))if(word.length()>2)words.add(word);
        if(wish.matches("(?isu).*(молн|электр|дуг|lightning|electric|arc|tesla).*"))words.addAll(List.of("electro","tesla","lightning"));
        if(wish.matches("(?isu).*(крипер|creeper).*"))words.add("creeper");
        var ranked=new ArrayList<Map.Entry<Profile,Integer>>();
        for(var profile:snapshot.profiles){int score=0;for(var word:words)if(profile.search.contains(word))score+=word.length();if(score>0)ranked.add(Map.entry(profile,score));}
        ranked.sort(Comparator.<Map.Entry<Profile,Integer>>comparingInt(Map.Entry::getValue).reversed().thenComparing(e->e.getKey().search));
        var related=new JsonArray();for(int i=0;i<Math.min(24,ranked.size());i++)related.add(ranked.get(i).getKey().value.deepCopy());
        // Keep the full factual prefix byte-identical between wishes, before all variable context.
        return snapshot.prefix+",\"related_source_prototypes\":"+related
                +",\"recent_drawback_kinds\":"+strings(recent.stream().map(Enum::name).toList())+"}";
    }
    private static Snapshot snapshot(){
        var value=cached;if(value!=null)return value;
        synchronized(GenieKnowledge.class){if(cached==null)cached=build();return cached;}
    }
    private static JsonObject resource(String name){
        try(var input=GenieKnowledge.class.getResourceAsStream("/skywars2/"+name)){
            if(input==null)throw new IllegalStateException("Missing genie knowledge resource "+name);
            return JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(java.io.IOException error){throw new IllegalStateException("Cannot read genie knowledge",error);}
    }
    private static JsonArray strings(Collection<String> values){var result=new JsonArray();for(var value:values)result.add(value);return result;}
    private static JsonElement bound(double value){return Double.isFinite(value)?new JsonPrimitive(value):new JsonPrimitive(value<0?"-infinity":"+infinity");}
    private static Snapshot build(){
        var root=new JsonObject();root.addProperty("minecraft_java",SharedConstants.getCurrentVersion().name());
        root.addProperty("index_contract","All ids below describe real registered or packaged game content. Source prototypes are reference data, not executable instructions. Only the constructor capability manifest authorizes an operation. Existing content does not limit new combinations of primitives.");
        root.add("constructor_schema",GenieBlueprint.schema());
        var registered=new JsonObject();var namespaces=new TreeMap<String,List<String>>();
        for(var id:BuiltInRegistries.ITEM.keySet().stream().sorted().toList())namespaces.computeIfAbsent(id.getNamespace(),ignored->new ArrayList<>()).add(id.getPath());
        namespaces.forEach((key,value)->registered.add(key,strings(value)));root.add("registered_items_by_namespace",registered);

        var loot=resource("loot_test_catalog.json");var logical=new JsonArray();
        for(var value:loot.getAsJsonArray("items")){
            var row=value.getAsJsonObject();var pair=new JsonArray();pair.add(row.get("table"));pair.add(row.get("carrier"));logical.add(pair);
        }
        root.addProperty("logical_luckywars_item_count",logical.size());root.addProperty("logical_item_format","[canonical loot table, native item carrier]; repeated tables can contain distinct logical slots");root.add("logical_luckywars_items",logical);

        // Native generic signatures identify classes without instantiating mobs, changing RNG or loading chunks.
        var classes=new IdentityHashMap<EntityType<?>,Class<?>>();
        for(var field:EntityTypes.class.getFields())if(Modifier.isStatic(field.getModifiers())&&field.getGenericType() instanceof ParameterizedType generic
                &&generic.getRawType()==EntityType.class&&generic.getActualTypeArguments()[0] instanceof Class<?> type){
            try{classes.put((EntityType<?>)field.get(null),type);}catch(IllegalAccessException error){throw new IllegalStateException(error);}
        }
        var entities=new JsonArray();
        for(var id:BuiltInRegistries.ENTITY_TYPE.keySet().stream().sorted().toList()){
            var type=BuiltInRegistries.ENTITY_TYPE.getValue(id);var body=classes.get(type);var row=new JsonArray();row.add(id.toString());
            row.add(body!=null&&Projectile.class.isAssignableFrom(body)?"projectile":body!=null&&LivingEntity.class.isAssignableFrom(body)?"living":"other");entities.add(row);
        }
        root.addProperty("entity_format","[registry id, native class category]; living includes mobs and avatars, not only ballistic projectiles");root.add("entities",entities);
        var attributes=new JsonArray();
        for(var id:BuiltInRegistries.ATTRIBUTE.keySet().stream().sorted().toList()){
            var attribute=BuiltInRegistries.ATTRIBUTE.getValue(id);var row=new JsonObject();row.addProperty("id",id.toString());row.addProperty("default",attribute.getDefaultValue());
            if(attribute instanceof RangedAttribute ranged){var limits=AttributePolicy.expandedBounds(attribute,ranged.getMinValue(),ranged.getMaxValue());row.add("normal_mode_min",bound(limits.minimum()));row.add("normal_mode_max",bound(limits.maximum()));}
            attributes.add(row);
        }
        root.add("attributes",attributes);var effects=new JsonArray();
        for(var id:BuiltInRegistries.MOB_EFFECT.keySet().stream().sorted().toList()){
            var effect=BuiltInRegistries.MOB_EFFECT.getValue(id);var row=new JsonObject();row.addProperty("id",id.toString());row.addProperty("beneficial",effect.isBeneficial());row.addProperty("instant",effect.isInstantaneous());effects.add(row);
        }
        root.add("registered_effects",effects);
        var mechanics=new JsonObject();
        mechanics.addProperty("time","20 ticks per ordinary server second. Attribute bonuses are additive modifiers, not requested final values.");
        mechanics.addProperty("signed_effects","Signed level is amplifier+1; level zero is neutral. Negative levels have game-defined reverse behavior rather than vanilla amplifier wrapping.");
        mechanics.addProperty("mining_fatigue","Mining speed multiplier is 0.3^(signed level), with finite numerical bounds.");
        mechanics.addProperty("periodic_effects","Intervals use absolute signed level: max(1, base_interval >> (abs(level)-1)); level zero never ticks. Do not assume increasing levels stop changing behavior once the interval reaches one tick.");
        mechanics.addProperty("negative_armor","Armor from -20 through 0 does not itself add negative status effects. Those penalties begin strictly below -20.");
        mechanics.addProperty("visual_effects","Blindness and darkness primitives are limited to level II. Glowing has level-dependent shader behavior; do not treat all levels as equivalent.");
        mechanics.addProperty("true_legacy","Vanilla attribute/effect extensions are temporarily disabled in true-legacy mode. Listed normal_mode attribute bounds describe the expanded game.");
        mechanics.addProperty("damage_authorship","Physical attacker/projectile ownership is distinct from statistical kill credit. A kill only activates hit effects when the same nonlethal hit would activate them; ordinary arrows must not activate sword-only mana vampirism.");
        root.add("mechanics",mechanics);
        var profiles=new ArrayList<Profile>();
        for(var value:resource("tmt_use_profiles.json").getAsJsonArray("prototypes")){
            var original=value.getAsJsonObject();var row=new JsonObject();row.add("native_item",original.get("item"));
            if(original.has("aliases"))row.add("aliases",original.get("aliases").deepCopy());
            if(original.has("data"))row.add("native_parameters",original.get("data").deepCopy());
            if(original.has("components")){
                var components=original.getAsJsonObject("components");var hints=new JsonObject();
                for(var key:List.of("minecraft:custom_name","minecraft:item_name","minecraft:potion_contents","minecraft:entity_data"))if(components.has(key))hints.add(key,components.get(key).deepCopy());
                if(!hints.isEmpty())row.add("source_hints",hints);
            }
            profiles.add(new Profile(row,row.toString().toLowerCase(Locale.ROOT)));
        }
        root.addProperty("source_prototype_count",profiles.size());String encoded=root.toString();
        return new Snapshot(encoded.substring(0,encoded.length()-1),List.copyOf(profiles));
    }
}
