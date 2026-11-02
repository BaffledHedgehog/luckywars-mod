package me.skywars2.game;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/** Cached editable source constants. The same scoreboard operands drive mechanics and tooltips. */
public final class LbcSourceConfig {
    public static final Path FILE=Path.of("config","luckywars","source-values.json");
    private final MinecraftServer server;
    private final JsonObject defaults;
    private final Set<String> keys=new HashSet<>();
    LbcSourceConfig(MinecraftServer server){
        this.server=server;
        try(var input=LbcSourceConfig.class.getResourceAsStream("/skywars2/source_config_defaults.json")){
            if(input==null)throw new IOException("Missing source config defaults");
            defaults=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(IOException e){throw new IllegalStateException(e);}
        keys.addAll(defaults.getAsJsonObject("values").keySet());
        keys.addAll(defaults.getAsJsonObject("derived").keySet());
        reload();
    }
    boolean owns(String objective,String holder){return objective.equals("lbc.math")&&keys.contains(holder);}
    public int reload(){
        try{
            JsonObject document;
            if(Files.exists(FILE)){
                try(var reader=Files.newBufferedReader(FILE,StandardCharsets.UTF_8)){document=JsonParser.parseReader(reader).getAsJsonObject();}
            }else{
                document=new JsonObject();var migrated=defaults.getAsJsonObject("values").deepCopy();
                var board=server.getScoreboard();var objective=board.getObjective("lbc.math");
                if(objective!=null)for(String key:migrated.keySet()){
                    var value=board.getPlayerScoreInfo(ScoreHolder.forNameOnly(key),objective);
                    if(value!=null)migrated.addProperty(key,value.value());
                }
                document.add("values",migrated);
                Files.createDirectories(FILE.getParent());
                Files.writeString(FILE,new GsonBuilder().setPrettyPrinting().create().toJson(document)+"\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
            }
            // Validate the complete candidate before changing live mechanics.
            var configured=document.has("values")?document.getAsJsonObject("values"):document;
            var candidate=new LinkedHashMap<String,Integer>();
            for(var entry:defaults.getAsJsonObject("values").entrySet())candidate.put(entry.getKey(),entry.getValue().getAsInt());
            for(var entry:configured.entrySet()){
                if(!keys.contains(entry.getKey()))throw new IllegalArgumentException("Unknown source setting: "+entry.getKey());
                var number=entry.getValue().getAsBigDecimal();
                candidate.put(entry.getKey(),number.intValueExact());
            }
            for(var entry:defaults.getAsJsonObject("derived").entrySet()){
                var rule=entry.getValue().getAsJsonObject();
                // Derived display prices cannot diverge from their actual charge cadence.
                candidate.put(entry.getKey(),Math.multiplyExact(candidate.get(rule.get("source").getAsString()),rule.get("multiplier").getAsInt()));
            }
            var board=server.getScoreboard();var objective=board.getObjective("lbc.math");
            if(objective==null)objective=board.addObjective("lbc.math",ObjectiveCriteria.DUMMY,Component.literal("lbc.math"),ObjectiveCriteria.RenderType.INTEGER,false,null);
            for(var entry:candidate.entrySet())board.getOrCreatePlayerScore(ScoreHolder.forNameOnly(entry.getKey()),objective).set(entry.getValue());
            return candidate.size();
        }catch(IOException|RuntimeException e){throw new IllegalStateException("Cannot reload "+FILE+": "+e.getMessage(),e);}
    }
}
