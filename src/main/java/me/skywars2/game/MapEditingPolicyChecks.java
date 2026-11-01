package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.server.permissions.PermissionSet;
import java.nio.file.Path;

/** The same native probe runs against the physically different production and test artifacts. */
public final class MapEditingPolicyChecks {
    private MapEditingPolicyChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Map editing policy: "+why);}
    public static void run(GameController game){
        var dispatcher=game.server().getCommands().getDispatcher();
        var save=dispatcher.getRoot().getChild("mapsave");
        require(save!=null&&save.getChild("tools")!=null,"shared developer tool command retained");
        boolean enabled=MapEditing.allowedServer(game.server());
        for(String child:new String[]{"pos","saveskywars","savebedwars","delete"})
            require((save.getChild(child)!=null)==enabled,"editing command flavor "+child);
        require((dispatcher.getRoot().getChild("mapdelete")!=null)==enabled,"delete alias flavor");
        require(!MapEditing.allowedDirectory(Path.of("E:/servers_mew/cringe1_21_4_skywars")),"legacy server directory cannot enable map editing");
        if(!MapEditing.testBuildAvailable()){
            require(!enabled&&!game.developerTools().authoring().busy(),"production disabled implementation initializes normally");
            for(String name:new String[]{"MapAuthoring","MapAuthoringChecks","MapDeletion","MapDeletionChecks"}){
                try{Class.forName("me.skywars2.game."+name);throw new IllegalStateException("Unexpected production class "+name);}
                catch(ClassNotFoundException expected){}
            }
        }else{
            require(enabled,"qualified test artifact runs only in the allowlisted fixture");
            for(var player:game.server().getPlayerList().getPlayers()){
                boolean named=player.getGameProfile().name().equals("BaffledHedgehog");
                require(MapEditing.allowedDeletion(player,game.server())==(named&&GameController.operator(player)),"exact actual player identity plus permission");
                var source=player.createCommandSourceStack();var deletion=save.getChild("delete");
                require(!deletion.canUse(source.withPermission(PermissionSet.NO_PERMISSIONS)),"source permission cannot be bypassed");
                require(deletion.canUse(source.withPermission(PermissionSet.ALL_PERMISSIONS))==MapEditing.allowedDeletion(player,game.server()),"source privilege alone cannot impersonate authorized player");
            }
        }
        SkyWars2.LOGGER.info("[check] PASS map-editing-policy testBuild={} enabledHere={}",MapEditing.testBuildAvailable(),enabled);
    }
}
