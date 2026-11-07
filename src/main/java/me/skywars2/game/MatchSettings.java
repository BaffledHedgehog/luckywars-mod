package me.skywars2.game;

import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/** Shared publication for command edits, GUI edits, resets and initial startup. */
final class MatchSettings {
    private static final java.util.Map<Integer,java.util.List<Component>> PREFIXES=prefixes();
    private MatchSettings() {}
    private static java.util.Map<Integer,java.util.List<Component>> prefixes(){
        try(var input=MatchSettings.class.getResourceAsStream("/skywars2/team_prefixes.json")){
            if(input==null)throw new IllegalStateException("Missing source team prefixes");
            var root=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var groups=new java.util.HashMap<Integer,java.util.List<Component>>();
            for(int n=0;n<2;n++){
                var entries=new java.util.ArrayList<Component>();
                for(var value:root.getAsJsonArray(n==0?"sfw":"nsfw"))entries.add(net.minecraft.network.chat.ComponentSerialization.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,value).getOrThrow());
                groups.put(n,java.util.List.copyOf(entries));
            }
            var mixed=new java.util.ArrayList<>(groups.get(0));mixed.addAll(groups.get(1));groups.put(2,java.util.List.copyOf(mixed));
            return java.util.Map.copyOf(groups);
        }catch(java.io.IOException error){throw new IllegalStateException("Cannot read source team prefixes",error);}
    }
    static void prefixes(GameController game,java.util.Collection<net.minecraft.world.scores.PlayerTeam> teams){
        var pool=PREFIXES.getOrDefault(game.balanceLegacy()?3:game.config().prefixes,java.util.List.of());
        var random=game.maps().level().getRandom();
        for(var team:new java.util.LinkedHashSet<>(teams))team.setPlayerPrefix(pool.isEmpty()?Component.empty():pool.get(random.nextInt(pool.size())).copy());
    }
    static void sync(GameController game){
        var config=game.config();var mana=game.items().mana();
        mana.score(ScoreHolder.forNameOnly("#imbadrop_chance"),"swrg.math").set(config.imbaDropChance);
        mana.score(ScoreHolder.forNameOnly("imbadrop_chance"),"lbc.math").set(config.imbaDropChance);
        mana.score(ScoreHolder.forNameOnly("#hp"),"swrg.math").set(config.healthDisplay);
        mana.score(ScoreHolder.forNameOnly("#prefixes"),"swrg.math").set(config.prefixes);
        var board=game.server().getScoreboard();var health=board.getObjective("swrg.hp");
        if(health==null)health=board.addObjective("swrg.hp",ObjectiveCriteria.HEALTH,Component.literal("❤"),ObjectiveCriteria.RenderType.HEARTS,false,null);
        board.setDisplayObjective(DisplaySlot.BELOW_NAME,(config.healthDisplay&1)!=0?health:null);
        board.setDisplayObjective(DisplaySlot.LIST,(config.healthDisplay&2)!=0?health:null);
    }
}
