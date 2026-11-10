package me.skywars2.game;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;

/** Exact raw legacy operands at whole-match boundaries; native services own their resident cleanup. */
final class LbcMatchReset {
    private static final Set<String> TAGS;
    private static final Map<String,Integer> SCORES;
    static {
        try(var input=Objects.requireNonNull(LbcMatchReset.class.getResourceAsStream("/skywars2/match_reset.json"));
                var reader=new InputStreamReader(input,StandardCharsets.UTF_8)){
            var root=JsonParser.parseReader(reader).getAsJsonObject();var tags=new HashSet<String>();
            root.getAsJsonArray("tags").forEach(value->tags.add(value.getAsString()));TAGS=Set.copyOf(tags);
            var scores=new HashMap<String,Integer>();
            root.getAsJsonObject("existingScores").entrySet().forEach(entry->scores.put(entry.getKey(),entry.getValue().getAsInt()));
            SCORES=Map.copyOf(scores);
        }catch(java.io.IOException error){throw new ExceptionInInitializerError(error);}
    }
    private LbcMatchReset(){}
    static void player(GameController game,ServerPlayer player){
        player.entityTags().removeAll(TAGS);
        var board=game.server().getScoreboard();
        for(var entry:SCORES.entrySet()){
            var objective=board.getObjective(entry.getKey());
            if(objective!=null&&board.getPlayerScoreInfo(player,objective)!=null)
                board.getOrCreatePlayerScore(player,objective,true).set(entry.getValue());
        }
    }
    static void all(GameController game){for(var player:game.server().getPlayerList().getPlayers())player(game,player);}
}
