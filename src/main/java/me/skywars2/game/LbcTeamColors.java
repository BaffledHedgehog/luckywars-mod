package me.skywars2.game;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import net.minecraft.world.scores.TeamColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

/** Source scoreboard colors and temporary, mutually hostile solo memberships. */
final class LbcTeamColors {
    private static final List<TeamColor> PALETTE=List.of(TeamColor.AQUA,TeamColor.BLACK,
            TeamColor.BLUE,TeamColor.DARK_AQUA,TeamColor.DARK_BLUE,TeamColor.DARK_GRAY,
            TeamColor.DARK_GREEN,TeamColor.DARK_PURPLE,TeamColor.DARK_RED,TeamColor.GOLD,
            TeamColor.GRAY,TeamColor.GREEN,TeamColor.LIGHT_PURPLE,TeamColor.RED,
            TeamColor.WHITE,TeamColor.YELLOW);
    private record Assignment(PlayerTeam original,PlayerTeam temporary){}
    private record Departure(Assignment assignment,PlayerTeam actual){}
    private final Scoreboard board;
    private boolean bedwars;
    private final Map<String,Assignment> assignments=new LinkedHashMap<>();
    private final Map<String,Departure> departed=new LinkedHashMap<>();
    private final Set<PlayerTeam> temporaryTeams=new LinkedHashSet<>();

    LbcTeamColors(Scoreboard board){this.board=board;}
    record Checkpoint(Map<String,Assignment> assignments,Map<String,Departure> departed,Set<PlayerTeam> temporaryTeams,boolean bedwars){}
    Checkpoint checkpoint(){return new Checkpoint(new LinkedHashMap<>(assignments),new LinkedHashMap<>(departed),new LinkedHashSet<>(temporaryTeams),bedwars);}
    void restore(Checkpoint image){assignments.clear();assignments.putAll(image.assignments);departed.clear();departed.putAll(image.departed);temporaryTeams.clear();temporaryTeams.addAll(image.temporaryTeams);bedwars=image.bedwars;}
    static TeamColor color(int number){return PALETTE.get(Math.floorMod(number-1,PALETTE.size()));}
    void ensurePalette(){
        for(int number=1;number<=32;number++){
            String name=Integer.toString(number);var team=board.getPlayerTeam(name);
            if(team==null){team=board.addPlayerTeam(name);sourceMetadata(team);}
            if(!active())team.setColor(Optional.of(color(number)));
        }
    }
    private static void sourceMetadata(PlayerTeam team){
        team.setSeeFriendlyInvisibles(false);
        team.setDeathMessageVisibility(Team.Visibility.ALWAYS);
        // Source leaves friendly fire, collision and name tags at vanilla defaults.
    }
    void begin(List<ServerPlayer> participants){
        stop();ensurePalette();
        var colors=new java.util.ArrayList<>(PALETTE);
        var random=participants.isEmpty()?null:participants.getFirst().level().getRandom();
        for(int i=0;i<participants.size();i++){
            if(i%colors.size()==0)for(int j=colors.size()-1;j>0;j--){
                int other=random.nextInt(j+1);var value=colors.get(j);colors.set(j,colors.get(other));colors.set(other,value);
            }
            var player=participants.get(i);String name=player.getScoreboardName();var original=board.getPlayersTeam(name);
            var team=i<32?board.getPlayerTeam(Integer.toString(i+1)):null;
            // Never put a solo participant beside an unrelated player or entity.
            if(team==null||!team.getPlayers().isEmpty()){
                String base="swrg_s"+player.getUUID().toString().replace("-","").substring(0,8),candidate=base;
                int suffix=0;while(board.getPlayerTeam(candidate)!=null)candidate=base+(++suffix);
                team=board.addPlayerTeam(candidate);sourceMetadata(team);team.setColor(Optional.of(color(i+1)));temporaryTeams.add(team);
            }
            team.setColor(Optional.of(colors.get(i%colors.size())));
            assignments.put(name,new Assignment(original,team));board.addPlayerToTeam(name,team);
        }
        prefixes();
    }
    /** Real BedWars participants may share the same source numeric team; lobby choices remain restorable. */
    void beginBedwars(List<ServerPlayer> participants,Map<java.util.UUID,Integer> selected){
        for(var player:participants){var number=selected.get(player.getUUID());if(number==null||number<1||number>16)
            throw new IllegalArgumentException("BedWars requires a complete source team assignment");}
        stop();ensurePalette();bedwars=true;
        for(var player:participants){String name=player.getScoreboardName();var original=board.getPlayersTeam(name);
            var team=board.getPlayerTeam(Integer.toString(selected.get(player.getUUID())));
            assignments.put(name,new Assignment(original,team));board.addPlayerToTeam(name,team);}
        prefixes();
    }
    private void prefixes(){
        var game=me.skywars2.SkyWars2.game();
        if(game!=null)MatchSettings.prefixes(game,assignments.values().stream().map(Assignment::temporary).toList());
    }
    PlayerTeam assigned(ServerPlayer player){var assignment=assignments.get(player.getScoreboardName());return assignment==null?null:assignment.temporary();}
    PlayerTeam original(ServerPlayer player){var assignment=assignments.get(player.getScoreboardName());return assignment==null?null:assignment.original();}
    boolean active(){return !assignments.isEmpty();}
    void leave(ServerPlayer player){
        String name=player.getScoreboardName();
        if(!assignments.isEmpty()||!departed.isEmpty())
            departed.put(name,new Departure(assignments.get(name),board.getPlayersTeam(name)));
        restore(name);removeEmptyTemporaryTeams();
    }
    /** Reattach the match team after elimination or a deliberate operator return after reconnect. */
    void rejoin(ServerPlayer player){
        String name=player.getScoreboardName();var previous=departed.remove(name);
        var assignment=previous==null?assignments.get(name):previous.assignment();
        PlayerTeam wanted=previous==null?board.getPlayersTeam(name):previous.actual();
        if(wanted==null&&assignment!=null)wanted=assignment.temporary();
        if(wanted!=null){
            PlayerTeam restored;
            if(bedwars){
                // A whole-world rewind may replace a native team object. Keep the canonical number,
                // never create a mutually hostile solo fallback or a suffixed BedWars team.
                if(!wanted.getName().matches("[1-9]|1[0-6]"))return;
                ensurePalette();restored=board.getPlayerTeam(wanted.getName());
            }else restored=residentTeam(wanted);
            board.addPlayerToTeam(name,restored);
            if(assignment!=null&&wanted==assignment.temporary())
                assignments.put(name,new Assignment(assignment.original(),restored));
            return;
        }
        if(bedwars)return; // Root match membership must assign a BedWars team before an operator return.
        // A late operator has no previous solo team; allocate one without joining another player.
        ensurePalette();PlayerTeam team=null;
        for(int number=1;number<=32;number++)if(board.getPlayerTeam(Integer.toString(number)).getPlayers().isEmpty()){
            team=board.getPlayerTeam(Integer.toString(number));break;
        }
        if(team==null){
            String base="swrg_s"+player.getUUID().toString().replace("-","").substring(0,8),candidate=base;
            for(int suffix=1;board.getPlayerTeam(candidate)!=null;suffix++)candidate=base+suffix;
            team=board.addPlayerTeam(candidate);sourceMetadata(team);team.setColor(Optional.of(color(assignments.size()+1)));temporaryTeams.add(team);
        }
        assignments.put(name,new Assignment(null,team));board.addPlayerToTeam(name,team);
    }
    private PlayerTeam residentTeam(PlayerTeam previous){
        if(board.getPlayerTeam(previous.getName())==previous)return previous;
        String name=previous.getName(),base=name;
        for(int suffix=1;board.getPlayerTeam(name)!=null;suffix++)name=base+"_"+suffix;
        var restored=board.addPlayerTeam(name);
        restored.setColor(previous.getColor());restored.setDisplayName(previous.getDisplayName());
        restored.setPlayerPrefix(previous.getPlayerPrefix());restored.setPlayerSuffix(previous.getPlayerSuffix());
        restored.setAllowFriendlyFire(previous.isAllowFriendlyFire());restored.setSeeFriendlyInvisibles(previous.canSeeFriendlyInvisibles());
        restored.setDeathMessageVisibility(previous.getDeathMessageVisibility());restored.setNameTagVisibility(previous.getNameTagVisibility());
        restored.setCollisionRule(previous.getCollisionRule());
        temporaryTeams.add(restored);return restored;
    }
    private void restore(String name){
        var assignment=assignments.remove(name);if(assignment==null)return;
        // Preserve a deliberate external reassignment made during the match.
        if(board.getPlayersTeam(name)!=assignment.temporary())return;
        board.removePlayerFromTeam(name,assignment.temporary());
        var original=assignment.original();
        if(original!=null&&board.getPlayerTeam(original.getName())==original)board.addPlayerToTeam(name,original);
    }
    private void removeEmptyTemporaryTeams(){
        for(var team:List.copyOf(temporaryTeams))if(team.getPlayers().isEmpty()){
            if(board.getPlayerTeam(team.getName())==team)board.removePlayerTeam(team);temporaryTeams.remove(team);
        }
    }
    void stop(){for(String name:List.copyOf(assignments.keySet()))restore(name);removeEmptyTemporaryTeams();temporaryTeams.clear();departed.clear();bedwars=false;}
}
