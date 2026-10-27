package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import java.util.*;

/** Actual shared actionbar packets from native status state, without compatibility-score bridges. */
public final class SharedStatusHudChecks {
    private SharedStatusHudChecks() { }
    private static void require(boolean ok,String why) { if(!ok)throw new IllegalStateException("Shared status HUD: "+why); }
    private static Component packet(GameController game,net.minecraft.server.level.ServerPlayer player,ElementalGrimoireChecks.Recording recording) {
        recording.packets.clear();game.items().invalidateHud(player);game.items().refreshHud(player);
        var rows=new ArrayList<Component>();
        for(var packet:recording.packets) {
            if(packet instanceof ClientboundSetActionBarTextPacket row)rows.add(row.text());
            else if(packet instanceof ClientboundBundlePacket bundle)for(var child:bundle.subPackets())
                if(child instanceof ClientboundSetActionBarTextPacket row)rows.add(row.text());
        }
        require(rows.size()==1,"exactly one actual outgoing actionbar");return rows.getFirst();
    }
    public static void run(GameController game) {
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(player!=null&&game.playing()&&game.canUseGameplayItems(player),"actual active participant");
        var mana=game.items().mana();var psycho=game.items().psycho();
        require(psycho.effectCount()==0&&psycho.rayCount(player)==0&&!game.items().timeStop().stopped(player.level())
                &&!game.items().timeStop().frozen(player)&&!LbcGreed.active(player),"exclusive inactive Psycho and Time Stop fixture");
        var saved=new GameChecks.WandThirdPlayerState(player,mana);
        var scores=new LinkedHashMap<net.minecraft.world.scores.Objective,Integer>();
        var board=game.server().getScoreboard();
        var objectives=new HashSet<>(board.getObjectives());
        for(var objective:board.getObjectives()){var value=board.getPlayerScoreInfo(player,objective);if(value!=null)scores.put(objective,value.value());}
        var advancements=new LinkedHashMap<net.minecraft.advancements.AdvancementHolder,Set<String>>();
        for(var advancement:game.server().getAdvancements().getAllAdvancements()) {
            var done=new HashSet<String>();player.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(done::add);advancements.put(advancement,done);
        }
        var previous=player.connection;var recording=new ElementalGrimoireChecks.Recording(player);
        boolean stopped=false;
        try {
            player.connection=new ServerGamePacketListenerImpl(game.server(),recording,player,new CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            mana.score(player,"scared").set(9999);mana.score(player,"void_resistance").set(7);
            for(var name:List.of("dimensional_shifting","all_seeing_eye","living_legend"))mana.score(player,name).set(9999);
            psycho.afflict(player,player,13,0,0,0);
            require(psycho.hudValue(player,"scared")==13,"real native fear grant accepted");
            var row=packet(game,player,recording);var text=row.getString();
            require(text.contains("\uE119 13")&&!text.contains("\uE119 9999"),"native fear wins over stale compatibility score");
            require(text.contains("\uE126 7"),"void resistance displays source seconds");
            final boolean[] green={false};row.visit((style,value)->{if(value.equals("7")&&style.getColor()!=null&&style.getColor().getValue()==0x55ff55)green[0]=true;return Optional.<Object>empty();},net.minecraft.network.chat.Style.EMPTY);
            require(green[0],"void count retains source GREEN style");
            require(!text.contains("\uE125")&&!text.contains("\uE127")&&!text.contains("\uE128"),"unported historical scores create no stub icons");
            game.items().timeStop().begin(player.level(),40);stopped=true;
            mana.voidProtectionSecond(player);
            require(mana.hudValue(player,"void_resistance")==7,"actual Time Stop pauses native void timer");
            game.items().timeStop().end(player.level());stopped=false;
            mana.voidProtectionSecond(player);
            require(mana.hudValue(player,"void_resistance")==6&&packet(game,player,recording).getString().contains("\uE126 6"),"native source second changes actual packet");
            psycho.clearStatus(player,"scared");
            require(!packet(game,player,recording).getString().contains("\uE119"),"native selective cleanse removes fear icon");
            mana.score(player,"void_resistance").set(1);mana.voidProtectionSecond(player);
            require(mana.hudValue(player,"void_resistance")==0&&!packet(game,player,recording).getString().contains("\uE126"),"native expiration clears void icon");
        } finally {
            if(stopped)game.items().timeStop().end(player.level());
            psycho.clearStatus(player,"scared");
            for(var objective:List.copyOf(board.getObjectives()))if(scores.containsKey(objective))board.getOrCreatePlayerScore(player,objective).set(scores.get(objective));else board.resetSinglePlayerScore(player,objective);
            for(var objective:List.copyOf(board.getObjectives()))if(!objectives.contains(objective))board.removeObjective(objective);
            advancements.forEach((advancement,done)->{for(var key:advancement.value().criteria().keySet())if(done.contains(key))player.getAdvancements().award(advancement,key);else player.getAdvancements().revoke(advancement,key);});
            saved.restore(game);player.connection=previous;previous.resetPosition();game.items().invalidateHud(player);
        }
        SkyWars2.LOGGER.info("[check] PASS shared-status-hud");
    }
}
