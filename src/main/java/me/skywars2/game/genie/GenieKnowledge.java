package me.skywars2.game.genie;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.game.AttributePolicy;
import me.skywars2.game.GenieNative;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/** Immutable classpath/registry knowledge. Building this index never rolls loot or creates world entities. */
public final class GenieKnowledge {
    private GenieKnowledge(){}
    private record Profile(JsonObject value,String search){}
    private record Snapshot(String prefix,String scriptPrefix,List<Profile> profiles){}
    private static volatile Snapshot cached;

    /** Called by the model worker, not by the server tick. Full compact index plus relevant prototype details. */
    public static String forWish(String wish,Collection<GenieBlueprint.TwistKind> recent){
        return forWish(wish,recent.stream().map(Enum::name).toList(),null);
    }

    static String forScriptWish(String wish, Collection<String> recent, HolderLookup.Provider lookup) {
        return forWish(wish, recent, lookup);
    }

    private static String forWish(String wish,Collection<String> recent,HolderLookup.Provider lookup){
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
        String index = lookup == null ? snapshot.prefix : snapshot.scriptPrefix + GenieContentIndex.fields(lookup);
        return index+",\"related_source_prototypes\":"+related
                +",\"recent_drawback_kinds\":"+strings(recent)+"}";
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

        var entities=new JsonArray();var launchable=new JsonArray();var standaloneTypes=new JsonArray();
        for(var id:BuiltInRegistries.ENTITY_TYPE.keySet().stream().sorted().toList()){
            var type=BuiltInRegistries.ENTITY_TYPE.getValue(id);var row=new JsonArray();row.add(id.toString());
            row.add(GenieEntities.category(type));entities.add(row);
            if(GenieEntities.launchable(type)){
                standaloneTypes.add(id.toString());
                var limits=new JsonObject();limits.addProperty("id",id.toString());limits.addProperty("minimum_cooldown",GenieEntities.minimumCooldown(type));
                limits.addProperty("native_power",GenieEntities.power(type));launchable.add(limits);
            }
        }
        root.addProperty("entity_format","[registry id, native class category]; living includes mobs and avatars, not only ballistic projectiles");root.add("entities",entities);
        root.add("native_entity_launch",launchable);
        root.addProperty("native_entity_launch_contract","ENTITY_LAUNCH uses these exact native factories: amount is initial speed 0..3 blocks/tick, ticks lifetime (0=200); body retains native AI, physics and attack power. Target SELF or LOOK_POINT means camera origin, never one mob per nearby victim. Add 2*native_power*max(1,lifetime/cooldown) to the ordinary power budget, and respect minimum_cooldown. New combinations are allowed, e.g. bow + SHOT + creeper; statistical author does not replace the physical attacking mob.");
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
        root.remove("constructor_schema");root.remove("native_entity_launch");root.remove("native_entity_launch_contract");
        root.addProperty("index_contract", "Registry IDs and source prototypes are reference data. Use only operations named in this request's attached item program contract.");
        root.add("script_launchable_entity_types",standaloneTypes);
        root.add("script_grimoires",strings(GenieNative.grimoires().keySet().stream().sorted().map(Object::toString).toList()));
        root.add("script_directed_casts",strings(GenieNative.casts().stream().sorted().map(Object::toString).toList()));
        root.add("script_gameplay_commands",strings(GenieCommandAccess.commands()));
        String scriptEncoded=root.toString();
        return new Snapshot(encoded.substring(0,encoded.length()-1),scriptEncoded.substring(0,scriptEncoded.length()-1),List.copyOf(profiles));
    }
}
