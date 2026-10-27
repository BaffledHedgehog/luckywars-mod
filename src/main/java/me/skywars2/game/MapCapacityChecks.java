package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Catalog capacity policy plus scoped real sixteen/twenty-four-player match fixtures. */
public final class MapCapacityChecks {
    private MapCapacityChecks() { }
    private static Crowd crowd;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Map capacity: "+why);}
    public static void run(GameController game){
        require(game.inLobby(),"candidate checks run in lobby");var maps=game.maps();
        for(boolean broken:new boolean[]{false,true})for(int count=1;count<=25;count++){
            var expected=new LinkedHashSet<String>();
            for(var map:maps.catalog())if(map.id().matches("[0-9]+")&&map.available()&&map.playerCapacity()>=count&&(!broken||maps.brokenAvailable(map.id())))expected.add(map.id());
            var actual=new LinkedHashSet<String>();for(var map:maps.quickStartCandidates(count,broken))actual.add(map.id());
            require(actual.equals(expected),"actual catalog candidate set count="+count+" broken="+broken+" expected="+expected+" actual="+actual);
            require(actual.stream().noneMatch(id->id.endsWith("b")||id.equals("63")),"BedWars and wholly absent63 excluded");
        }
        for(int count:new int[]{9,10})require(maps.quickStartCandidates(count,false).stream().anyMatch(m->m.id().equals("39")),"native ten-spawn map39 admitted at "+count);
        for(int count=13;count<=16;count++)require(maps.quickStartCandidates(count,false).stream().anyMatch(m->m.id().equals("54")),"native sixteen-spawn map54 admitted at "+count);
        require(maps.quickStartCandidates(0,false).isEmpty()&&maps.quickStartCandidates(-1,true).isEmpty(),"nonpositive participant counts rejected");
        SkyWars2.LOGGER.info("[check] PASS map-capacity");
    }
    public static void crowd(GameController game){begin(game,16);}
    /** Native source-card callbacks; caller awaits the asynchronous map54 preparation afterward. */
    @SuppressWarnings("unchecked") public static void menu(GameController game){
        require(game.inLobby()&&!game.maps().busy()&&game.config().gameMode<=2,"idle SkyWars menu");
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(player!=null,"actual menu actor");
        var previous=player.connection;var recording=new ElementalGrimoireChecks.Recording(player);boolean admin=player.entityTags().contains("admin");
        Map<UUID,Integer> pages;try{var field=GameMenus.class.getDeclaredField("mapPages");field.setAccessible(true);pages=(Map<UUID,Integer>)field.get(game.menus());}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        var oldPage=pages.get(player.getUUID());
        try{
            player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),recording,player,new net.minecraft.server.network.CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
            player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());player.entityTags().add("admin");pages.put(player.getUUID(),0);
            game.menus().open(player,GameMenus.Page.MAPS);
            var formerlyBlocked=game.maps().info("23");require(formerlyBlocked!=null&&formerlyBlocked.available(),"intentional unused-reference map23 is playable");
            var button=OriginalGuiDesign.sourceMapButton(player,"23",false);require(button!=null,"source map23 card");
            click(player,button.slot());
            require(game.config().mapId.equals("23")&&game.maps().busy(),"formerly blocked map23 card begins actual native preparation");
            click(player,26);click(player,26);
            var large=OriginalGuiDesign.sourceMapButton(player,"54",game.config().mapId.equals("54"));require(large!=null,"source map54 card");click(player,large.slot());
            require(game.config().mapId.equals("54")&&(game.maps().busy()||game.selectedMapReady()),"actual source-card callback selects and prepares map54");
        }finally{
            player.closeContainer();if(oldPage==null)pages.remove(player.getUUID());else pages.put(player.getUUID(),oldPage);
            if(!admin)player.entityTags().remove("admin");player.connection=previous;previous.resetPosition();
        }
        SkyWars2.LOGGER.info("[check] PASS map-capacity-menu");
    }
    private static void click(ServerPlayer player,int slot){
        require(eu.pb4.sgui.api.SguiUtils.getCurrentGui(player) instanceof eu.pb4.sgui.api.gui.SimpleGui,"actual native SimpleGui");
        var gui=(eu.pb4.sgui.api.gui.SimpleGui)eu.pb4.sgui.api.SguiUtils.getCurrentGui(player);var button=gui.getGuiElement(slot);require(button!=null,"native card slot "+slot);
        button.getGuiCallback().click(slot,eu.pb4.sgui.api.ClickType.MOUSE_LEFT,net.minecraft.world.inventory.ContainerInput.PICKUP,gui);
    }
    public static void begin(GameController game,int count){
        require(crowd==null&&game.inLobby()&&!game.maps().busy()&&count>=3&&count<=24,"exclusive idle lobby fixture");
        var online=game.server().getPlayerList().getPlayers();
        require(online.size()==2&&online.stream().allMatch(p->p.getUUID().equals(GameChecks.offlineUuid("TestAlpha"))||p.getUUID().equals(GameChecks.offlineUuid("TestBeta"))),"isolated original Alpha/Beta only");
        crowd=new Crowd(game,count);
        try{crowd.create();}catch(RuntimeException|Error error){cleanup(game);throw error;}
        SkyWars2.LOGGER.info("[check] PASS map-capacity-crowd players={}",count);
    }
    public static void match(GameController game){
        require(crowd!=null&&game.playing(),"real quick-start reached playing");
        require(game.server().getPlayerList().getPlayers().size()==crowd.count,"all real players remain connected");
        try{
            var field=GameController.class.getDeclaredField("participants");field.setAccessible(true);var participants=(Map<?,?>)field.get(game);
            require(participants.size()==crowd.count,"real participant enrollment count");var positions=new HashSet<Object>();
            for(var player:game.server().getPlayerList().getPlayers()){
                require(game.allowDamage(player)&&!game.isObserver(player),"actual active participant "+player.getGameProfile().name());
                var participant=participants.get(player.getUUID());require(participant!=null,"native participant entry");
                var spawn=participant.getClass().getDeclaredField("spawn");spawn.setAccessible(true);var assigned=(MapManager.Spawn)spawn.get(participant);
                require(positions.add(assigned.position()),"distinct actual assigned spawn");
            }
            require(game.maps().info(game.config().mapId).playerCapacity()>=crowd.count,"chosen actual map fits crowd");
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        SkyWars2.LOGGER.info("[check] PASS map-capacity-match players={} map={}",crowd.count,game.config().mapId);
    }
    public static void cleanup(GameController game){
        if(crowd==null)return;require(game.inLobby(),"normal match stop precedes crowd teardown");
        var saved=crowd;try{saved.close();}finally{crowd=null;}
        SkyWars2.LOGGER.info("[check] PASS map-capacity-cleanup");
    }
    private static final class Crowd {
        final GameController game;final int count;final List<ServerPlayer> created=new ArrayList<>();
        final List<GameChecks.WandThirdPlayerState> originals=new ArrayList<>();
        final Map<UUID,PlayerStore.Profile> profiles;final GameConfig config;
        final com.google.gson.Gson gson=new com.google.gson.Gson();
        @SuppressWarnings("unchecked") Map<UUID,PlayerStore.Profile> profiles(){try{var field=PlayerStore.class.getDeclaredField("profiles");field.setAccessible(true);return (Map<UUID,PlayerStore.Profile>)field.get(game.players());}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
        Crowd(GameController game,int count){this.game=game;this.count=count;
            config=gson.fromJson(gson.toJson(game.config()),GameConfig.class);profiles=new LinkedHashMap<>();profiles().forEach((id,p)->profiles.put(id,gson.fromJson(gson.toJson(p),PlayerStore.Profile.class)));
            for(var player:game.server().getPlayerList().getPlayers())originals.add(new GameChecks.WandThirdPlayerState(player,game.items().mana()));
            for(int i=3;i<=count;i++)require(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid(String.format(java.util.Locale.ROOT,"TestMap%02d",i)))==null,"fresh crowd identity");
        }
        void create(){for(int i=3;i<=count;i++)created.add(GameChecks.createBot(game,String.format(java.util.Locale.ROOT,"TestMap%02d",i)));require(game.server().getPlayerList().getPlayers().size()==count,"native PlayerList crowd enrollment");}
        void close(){
            for(var player:created)if(game.server().getPlayerList().getPlayer(player.getUUID())==player)player.connection.onDisconnect(new net.minecraft.network.DisconnectionDetails(net.minecraft.network.chat.Component.literal("Map capacity fixture complete")));
            for(var original:originals)original.restore(game);
            profiles().clear();profiles().putAll(profiles);game.players().save();
            try{for(var field:GameConfig.class.getFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())&&!java.lang.reflect.Modifier.isFinal(field.getModifiers()))field.set(game.config(),field.get(config));}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            game.config().save();game.luckyModifiers().syncConfig();
        }
    }
}
