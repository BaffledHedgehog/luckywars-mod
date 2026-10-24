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
    private final Scoreboard board;
    private final Map<String,Assignment> assignments=new LinkedHashMap<>();
    private final Set<PlayerTeam> temporaryTeams=new LinkedHashSet<>();

    LbcTeamColors(Scoreboard board){this.board=board;}
    static TeamColor color(int number){return PALETTE.get(Math.floorMod(number-1,PALETTE.size()));}
    void ensurePalette(){
        for(int number=1;number<=32;number++){
            String name=Integer.toString(number);var team=board.getPlayerTeam(name);
            if(team==null){team=board.addPlayerTeam(name);sourceMetadata(team);}
            team.setColor(Optional.of(color(number)));
        }
    }
    private static void sourceMetadata(PlayerTeam team){
        team.setSeeFriendlyInvisibles(false);
        team.setDeathMessageVisibility(Team.Visibility.ALWAYS);
        // Source leaves friendly fire, collision and name tags at vanilla defaults.
    }
    void begin(List<ServerPlayer> participants){
        stop();ensurePalette();
        for(int i=0;i<participants.size();i++){
            var player=participants.get(i);String name=player.getScoreboardName();var original=board.getPlayersTeam(name);
            var team=i<32?board.getPlayerTeam(Integer.toString(i+1)):null;
            // Never put a solo participant beside an unrelated player or entity.
            if(team==null||!team.getPlayers().isEmpty()){
                String base="swrg_s"+player.getUUID().toString().replace("-","").substring(0,8),candidate=base;
                int suffix=0;while(board.getPlayerTeam(candidate)!=null)candidate=base+(++suffix);
                team=board.addPlayerTeam(candidate);sourceMetadata(team);team.setColor(Optional.of(color(i+1)));temporaryTeams.add(team);
            }
            assignments.put(name,new Assignment(original,team));board.addPlayerToTeam(name,team);
        }
    }
    PlayerTeam assigned(ServerPlayer player){var assignment=assignments.get(player.getScoreboardName());return assignment==null?null:assignment.temporary();}
    PlayerTeam original(ServerPlayer player){var assignment=assignments.get(player.getScoreboardName());return assignment==null?null:assignment.original();}
    boolean active(){return !assignments.isEmpty();}
    void leave(ServerPlayer player){restore(player.getScoreboardName());removeEmptyTemporaryTeams();}
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
    void stop(){for(String name:List.copyOf(assignments.keySet()))restore(name);removeEmptyTemporaryTeams();temporaryTeams.clear();}
}
