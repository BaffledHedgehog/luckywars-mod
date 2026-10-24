package me.skywars2.game;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import me.skywars2.SkyWars2;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.TeamColor;

/** Native palette, live match membership and restoration boundaries. */
public final class TeamColorChecks {
    private TeamColorChecks(){}
    private static final String[] EXPECTED={"aqua","black","blue","dark_aqua","dark_blue","dark_gray","dark_green","dark_purple","dark_red","gold","gray","green","light_purple","red","white","yellow"};
    private record Saved(PlayerTeam original,PlayerTeam temporary){}
    private static Map<String,Saved> pending;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Team colors: "+why);}
    private static void palette(Scoreboard board){
        for(int number=1;number<=32;number++){
            var team=board.getPlayerTeam(Integer.toString(number));
            require(team!=null&&team.getColor().isPresent()&&team.getColor().get().getSerializedName().equals(EXPECTED[(number-1)%16]),"exact source palette team "+number);
        }
        require(!board.getPlayerTeam("1").isAlliedTo(board.getPlayerTeam("17")),"equal colors never create alliances");
    }
    public static void run(GameController game){
        require(game.playing()&&game.config().gameMode==1,"actual mode1 match");palette(game.server().getScoreboard());
        var participants=game.alivePlayers();require(participants.size()>=2,"two actual participants");var unique=new HashSet<PlayerTeam>();
        for(var player:participants){
            var assigned=game.teamColors().assigned(player);
            require(assigned!=null&&player.getTeam()==assigned&&unique.add(assigned),"each live participant has its own native team");
            require(assigned.getPlayers().size()==1&&assigned.getPlayers().contains(player.getScoreboardName()),"solo team does not merge unrelated members");
            require(PlayerTeam.formatNameForTeam(assigned,Component.literal(player.getScoreboardName())).getStyle().getColor().equals(assigned.getColor().orElseThrow().textColor()),"native name formatting carries team color");
        }
        for(var first:participants)for(var second:participants)if(first!=second)require(!first.isAlliedTo(second),"solo participants remain enemies");

        // Native isolated scoreboard exercises collisions and restoration without changing match teams.
        var board=new Scoreboard();var occupied=board.addPlayerTeam("1");board.addPlayerToTeam("unrelated_member",occupied);
        occupied.setSeeFriendlyInvisibles(true);occupied.setNameTagVisibility(Team.Visibility.NEVER);
        var custom=board.addPlayerTeam("user_custom");var alpha=participants.getFirst();var beta=participants.get(1);
        board.addPlayerToTeam(alpha.getScoreboardName(),custom);var colors=new LbcTeamColors(board);
        colors.begin(List.of(alpha,beta));palette(board);
        require(board.getPlayerTeam("1")==occupied&&occupied.getPlayers().contains("unrelated_member")&&occupied.canSeeFriendlyInvisibles()&&occupied.getNameTagVisibility()==Team.Visibility.NEVER,"existing numeric membership and noncolor metadata preserved");
        var temporary=colors.assigned(alpha);require(temporary!=occupied&&temporary.getColor().orElseThrow()==TeamColor.AQUA&&custom.getPlayers().isEmpty(),"occupied numeric team uses independent temporary source color");
        require(!temporary.canSeeFriendlyInvisibles()&&temporary.getDeathMessageVisibility()==Team.Visibility.ALWAYS&&temporary.getNameTagVisibility()==Team.Visibility.ALWAYS&&temporary.isAllowFriendlyFire(),"source new-team metadata and vanilla friendly-fire default");
        colors.leave(alpha);require(board.getPlayersTeam(alpha.getScoreboardName())==custom&&board.getPlayerTeam(temporary.getName())==null,"leave restores custom membership and removes empty fallback");
        colors.stop();require(board.getPlayersTeam(beta.getScoreboardName())==null&&!colors.active(),"stop restores originally unteamed participant");
        colors.begin(List.of(alpha));board.addPlayerToTeam(alpha.getScoreboardName(),occupied);colors.stop();require(board.getPlayersTeam(alpha.getScoreboardName())==occupied,"stop preserves newer external assignment");
        SkyWars2.LOGGER.info("[check] PASS team-colors");
    }
    public static void stopProbe(GameController game){
        require(game.playing()&&game.teamColors().active(),"active match colors before actual stop");pending=new LinkedHashMap<>();
        for(var player:game.alivePlayers())pending.put(player.getScoreboardName(),new Saved(game.teamColors().original(player),game.teamColors().assigned(player)));
        require(pending.size()>=2,"two participant restoration probes");SkyWars2.LOGGER.info("[check] PASS team-colors-stop-probe");
    }
    public static void cleanup(GameController game){
        require(pending!=null&&!game.teamColors().active(),"actual match stop releases color assignments");var board=game.server().getScoreboard();
        for(var entry:pending.entrySet())require(board.getPlayersTeam(entry.getKey())==entry.getValue().original(),"actual stop restores original native membership "+entry.getKey());
        palette(board);pending=null;SkyWars2.LOGGER.info("[check] PASS team-colors-cleanup");
    }
}
