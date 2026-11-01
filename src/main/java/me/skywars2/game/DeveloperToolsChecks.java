package me.skywars2.game;

import eu.pb4.sgui.api.SguiUtils;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.skywars2.SkyWars2;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native callback checks. The fixture operator is required; no production op-list mutation. */
public final class DeveloperToolsChecks {
    private DeveloperToolsChecks(){}
    private static void require(boolean ok,String text){if(!ok)throw new IllegalStateException("Developer tools: "+text);}
    public static void run(GameController game){
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(player!=null&&DeveloperTools.authorized(player),"TestAlpha is an actual fixture operator");
        var tools=game.developerTools();var saved=new GameChecks.WandThirdPlayerState(player,game.items().mana());
        var created=new ArrayList<Entity>();var dispatcher=game.server().getCommands().getDispatcher();
        var before=new HashSet<>(tools.ownedMarkers(player));
        var teamNames=new HashSet<String>();for(var team:game.server().getScoreboard().getPlayerTeams())teamNames.add(team.getName());
        try{
            player.closeContainer();player.getInventory().clearContent();
            require(DeveloperTools.ITEM instanceof eu.pb4.polymer.core.api.item.SimplePolymerItem,"registered native Polymer item");
            var denied=player.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS);
            var permitted=player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS);
            for(var command:List.of("gallery","developer","mapsave")){
                var root=dispatcher.getRoot().getChild(command);
                require(root!=null&&!root.canUse(denied)&&root.canUse(permitted),"OP2 command boundary "+command);
            }
            var visitor=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(visitor!=null&&!DeveloperTools.authorized(visitor),"TestBeta is an actual nonoperator fixture");
            var hand=visitor.getMainHandItem().copy();
            try{
                visitor.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(DeveloperTools.ITEM));
                var menu=visitor.containerMenu;
                require(tools.useItem(visitor,InteractionHand.MAIN_HAND)&&visitor.containerMenu==menu,"nonoperator tool interaction is consumed without opening");
                var egg=DeveloperTools.MarkerKind.SPAWN.stack();visitor.setItemInHand(InteractionHand.MAIN_HAND,egg);
                var markerCount=tools.ownedMarkers(visitor).size();var hit=new BlockHitResult(visitor.position(),Direction.UP,visitor.blockPosition(),false);
                require(tools.useBlock(visitor,InteractionHand.MAIN_HAND,hit)&&egg.getCount()==1&&tools.ownedMarkers(visitor).size()==markerCount,"nonoperator marker cannot spawn or debit");
            }finally{visitor.setItemInHand(InteractionHand.MAIN_HAND,hand);}
            require(DeveloperTools.MarkerKind.values().length==15,"all fifteen original marker eggs");
            tools.giveTools(player);tools.giveTools(player);
            int count=0;for(int i=0;i<player.getInventory().getContainerSize();i++)if(DeveloperTools.isTools(player.getInventory().getItem(i)))count+=player.getInventory().getItem(i).getCount();
            require(count==1,"repeat tool grants do not duplicate");
            var raw=new ItemStack(Items.PAPER);var flag=new net.minecraft.nbt.CompoundTag();flag.putByte("developer_menu",(byte)1);flag.putString("fixture","untouched");raw.set(DataComponents.CUSTOM_DATA,CustomData.of(flag));
            player.setItemInHand(InteractionHand.MAIN_HAND,raw);var original=raw.copy();
            require(tools.useItem(player,InteractionHand.MAIN_HAND),"raw source paper opens native menu");
            require(ItemStack.matches(original,player.getMainHandItem()),"opening preserves original source stack");
            require(SguiUtils.getCurrentGui(player) instanceof SimpleGui,"actual native GUI opened");
            var gui=(SimpleGui)SguiUtils.getCurrentGui(player);
            for(int slot:new int[]{0,9,18,1,10,2,11,20,17,26})require(gui.getGuiElement(slot)!=null,"source tool button slot "+slot);
            player.closeContainer();
            // Placement does not alter blocks. All created fixtures are explicitly retained for cleanup.
            var origin=player.blockPosition().above(8);int index=0;
            for(var kind:DeveloperTools.MarkerKind.values())if(kind!=DeveloperTools.MarkerKind.DELETE){
                var stack=kind.stack();stack.setCount(2);require(DeveloperTools.kind(stack)==kind,"native marker identity "+kind);
                var legacy=stack.copy();legacy.remove(DataComponents.CUSTOM_DATA);require(DeveloperTools.kind(legacy)==kind,"raw entity_data compatibility "+kind);
                player.setItemInHand(InteractionHand.MAIN_HAND,stack);
                var block=origin.offset(index++*4,0,0);var hit=new BlockHitResult(Vec3.atCenterOf(block),Direction.UP,block,false);
                int expected=player.isCreative()?2:1;
                require(tools.useBlock(player,InteractionHand.MAIN_HAND,hit),"actual placement callback "+kind);
                var fresh=tools.ownedMarkers(player).stream().filter(e->!before.contains(e)&&!created.contains(e)).toList();require(fresh.size()==1,"exactly one native actor "+kind);
                var actor=fresh.getFirst();created.add(actor);
                require(kind==DeveloperTools.MarkerKind.SHOP?actor.getType()==net.minecraft.world.entity.EntityTypes.VILLAGER:actor instanceof Marker,"actual actor type "+kind);
                require(actor.entityTags().containsAll(kind.tags),"exact authored marker tags "+kind);
                require(actor.getYRot()==player.getYRot(),"native marker yaw retained "+kind);
                require(player.getMainHandItem().getCount()==expected,"single successful marker debit "+kind);
            }
            var owned=created.getFirst();var at=owned.position();
            var foreign=new Marker(net.minecraft.world.entity.EntityTypes.MARKER,player.level());foreign.setPos(at);foreign.entityTags().add("stopper_magic");foreign.entityTags().add("developer_owner:"+UUID.randomUUID());
            require(player.level().addFreshEntity(foreign),"foreign marker fixture");created.add(foreign);
            var imported=new Marker(net.minecraft.world.entity.EntityTypes.MARKER,player.level());imported.setPos(at);imported.entityTags().add("swrg.look");require(player.level().addFreshEntity(imported),"imported authored marker fixture");created.add(imported);
            var delete=DeveloperTools.MarkerKind.DELETE.stack();tools.place(player,DeveloperTools.MarkerKind.DELETE,at,delete);
            require(owned.isRemoved()&&imported.isRemoved()&&!foreign.isRemoved(),"radius-three authored map deletion preserves unrelated spell markers");
            require(created.stream().filter(e->e!=foreign&&!e.isRemoved()).allMatch(e->e.position().distanceToSqr(at)>9),"delete confined to source radius");
            SkyWars2.LOGGER.info("[check] PASS developer-tools");
        }finally{player.closeContainer();for(var entity:created){if(entity.getTeam()!=null)game.server().getScoreboard().removePlayerFromTeam(entity.getScoreboardName(),entity.getTeam());entity.discard();}for(var team:List.copyOf(game.server().getScoreboard().getPlayerTeams()))if(!teamNames.contains(team.getName())&&team.getPlayers().isEmpty())game.server().getScoreboard().removePlayerTeam(team);saved.restore(game);}
    }
}
