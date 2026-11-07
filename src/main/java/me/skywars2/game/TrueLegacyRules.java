package me.skywars2.game;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/** Immutable vanilla data; changing legacy mode never reloads global registries. */
public final class TrueLegacyRules {
    private record Vanilla(Map<Identifier,Set<Identifier>> tags,Map<Identifier,DamageType> types) {}
    private static final Vanilla VANILLA=load();
    private TrueLegacyRules() {}
    public static boolean kit(int id) { return id>0 && id<=21; } // Through Half Slabs, including the source ID gap19.
    public static boolean skill(int id) { return id>0 && id<=13 && id!=11 && id!=13; } // Heavyweight12 remains available.
    public static void warmup() { if(VANILLA.tags.isEmpty()||VANILLA.types.isEmpty())throw new IllegalStateException("Empty vanilla legacy damage snapshot"); }
    private static Vanilla load() {
        try(var input=TrueLegacyRules.class.getResourceAsStream("/skywars2/vanilla_damage_26_3.json")) {
            if(input==null)throw new IllegalStateException("Missing vanilla legacy damage snapshot");
            var root=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            var tags=new HashMap<Identifier,Set<Identifier>>();
            for(var row:root.getAsJsonObject("tags").entrySet()) {
                var values=new HashSet<Identifier>();for(var value:row.getValue().getAsJsonArray())values.add(Identifier.parse(value.getAsString()));
                tags.put(Identifier.parse(row.getKey()),Set.copyOf(values));
            }
            var types=new HashMap<Identifier,DamageType>();
            for(var row:root.getAsJsonObject("damageTypes").entrySet())types.put(Identifier.parse(row.getKey()),DamageType.DIRECT_CODEC.parse(JsonOps.INSTANCE,row.getValue()).getOrThrow());
            return new Vanilla(Map.copyOf(tags),Map.copyOf(types));
        }catch(java.io.IOException error){throw new IllegalStateException("Cannot read vanilla legacy damage",error);}
    }
    public static boolean vanillaTag(DamageSource source,TagKey<DamageType> tag) {
        var key=source.typeHolder().unwrapKey().orElse(null);
        return key!=null&&VANILLA.tags.getOrDefault(tag.location(),Set.of()).contains(key.identifier());
    }
    public static DamageType vanillaType(DamageSource source) {
        var key=source.typeHolder().unwrapKey().orElse(null);
        return key==null?null:VANILLA.types.get(key.identifier());
    }
}
