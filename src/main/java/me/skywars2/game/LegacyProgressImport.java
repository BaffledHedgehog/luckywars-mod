package me.skywars2.game;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.scores.Scoreboard;

/** Import persistent legacy choices and scores without reissuing already spent currency. */
final class LegacyProgressImport {
    private static final int NO_SELECTION=100_000_000;
    private static final Pattern PLAYER_NAME=Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final List<String> OBJECTIVES=List.of("swrg.coins","swrg.memcoins","swrg.stat_games","swrg.stat_wins",
            "swrg.stat_kills","swrg.stat_death","swrg.keys0","swrg.keys1","swrg.keys2","swrg.keys3","swrg.keys4",
            "swrg.kit","swrg.skill","swrg.sound","swrg.suffix","swrg.death_effect","swrg.bars","swrg.kill_effect",
            "lbc.kit","lbc.skill","lbc.trinket1","lbc.trinket2","lbc.trinket3","lbc.levelup1","lbc.levelup2","lbc.levelup3",
            "lbc.challenge","challenge_score","lbc.challenge.kit","overheaven");
    record Result(int imported,int unresolved) {}
    private LegacyProgressImport() {}

    static Result importScoreboard(GameController game) throws IOException {
        var server=game.server();
        var store=game.players();
        var idsByName=new LinkedHashMap<String,UUID>();
        var ambiguous=new LinkedHashSet<String>();
        store.knownProfiles().forEach((id,profile)->{
            String name=profile.username.toLowerCase(Locale.ROOT);
            var previous=idsByName.putIfAbsent(name,id);
            if(previous!=null&&!previous.equals(id))ambiguous.add(name);
        });
        for(var player:server.getPlayerList().getPlayers()){
            String name=player.getScoreboardName().toLowerCase(Locale.ROOT);
            idsByName.put(name,player.getUUID());ambiguous.remove(name);
        }
        return importScoreboard(server.getScoreboard(),store,name->{
            String key=name.toLowerCase(Locale.ROOT);
            if(ambiguous.contains(key))return null;
            var existing=idsByName.get(key);
            return existing!=null?existing:resolve(server,name);
        });
    }

    static Result importScoreboard(Scoreboard board,PlayerStore store,java.util.function.Function<String,UUID> resolve) throws IOException {
        var scores=new LinkedHashMap<String,Map<String,Integer>>();
        for(var name:OBJECTIVES){
            var objective=board.getObjective(name);
            if(objective==null)continue;
            for(var score:board.listPlayerScores(objective))
                if(score.value()>0&&PLAYER_NAME.matcher(score.owner()).matches())
                    scores.computeIfAbsent(score.owner(),ignored->new LinkedHashMap<>()).put(name,score.value());
        }
        if(scores.isEmpty())return new Result(0,0);
        var before=store.checkpointProfiles();
        int imported=0,unresolved=0;
        try{
            for(var entry:scores.entrySet()){
                var id=resolve.apply(entry.getKey());
                if(id==null){unresolved++;continue;}
                var profile=store.get(id);
                if(profile.legacyScoreboardImported)continue;
                if(profile.username.isBlank())profile.username=entry.getKey();
                merge(profile,key->entry.getValue().getOrDefault(key,0));
                profile.legacyScoreboardImported=true;
                imported++;
            }
            if(imported>0)store.saveAtomic();
        }catch(IOException|RuntimeException error){store.restoreProfiles(before);throw error;}
        return new Result(imported,unresolved);
    }

    private static UUID resolve(MinecraftServer server,String name){
        if(!server.usesAuthentication())return NameAndId.createOffline(name).id();
        var known=BulkPlayerProfiles.cached(server,name);
        return known==null?null:known.fallbackId();
    }

    private static void merge(PlayerStore.Profile p,ToIntFunction<String> score){
        p.coins=Math.max(p.coins,score.applyAsInt("swrg.coins"));
        p.memcoins=Math.max(p.memcoins,score.applyAsInt("swrg.memcoins"));
        p.games=Math.max(p.games,score.applyAsInt("swrg.stat_games"));
        p.wins=Math.max(p.wins,score.applyAsInt("swrg.stat_wins"));
        p.kills=Math.max(p.kills,score.applyAsInt("swrg.stat_kills"));
        p.deaths=Math.max(p.deaths,score.applyAsInt("swrg.stat_death"));
        for(int slot=0;slot<p.keys.length;slot++)p.keys[slot]=Math.max(p.keys[slot],score.applyAsInt("swrg.keys"+slot));
        p.kit=choice(p.kit,score,"swrg.kit");
        p.skill=choice(p.skill,score,"swrg.skill");
        p.killSound=choice(p.killSound,score,"swrg.sound");
        p.suffix=choice(p.suffix,score,"swrg.suffix");
        p.winEffect=choice(p.winEffect,score,"swrg.death_effect");
        p.cage=choice(p.cage,score,"swrg.bars");
        p.killEffect=Math.min(3,choice(p.killEffect,score,"swrg.kill_effect"));
        p.lbcKit=choice(p.lbcKit,score,"lbc.kit");
        p.lbcSkill=choice(p.lbcSkill,score,"lbc.skill");
        p.lbcChallenge=Math.min(1,choice(p.lbcChallenge,score,"lbc.challenge"));
        p.lbcChallengeProgress=Math.min(19,Math.max(p.lbcChallengeProgress,score.applyAsInt("challenge_score")));
        p.lbcChallengeKit=Math.min(1,choice(p.lbcChallengeKit,score,"lbc.challenge.kit"));
        if(p.standOverheavenMatches<0&&score.applyAsInt("overheaven")>0)
            p.standOverheavenMatches=score.applyAsInt("overheaven");
        for(int slot=0;slot<3;slot++){
            p.trinkets[slot]=choice(p.trinkets[slot],score,"lbc.trinket"+(slot+1));
            int levelup=score.applyAsInt("lbc.levelup"+(slot+1));
            if(levelup>0&&levelup<NO_SELECTION&&p.levelups[slot]==0
                    &&Arrays.stream(p.levelups).noneMatch(existing->existing==levelup))p.levelups[slot]=levelup;
        }
        if(p.lbcTrinket==0)p.lbcTrinket=Arrays.stream(p.trinkets).filter(id->id>0).findFirst().orElse(0);
    }

    private static int choice(int current,ToIntFunction<String> score,String key){
        int legacy=score.applyAsInt(key);
        return current==0&&legacy<NO_SELECTION?legacy:current;
    }
}
