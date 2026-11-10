package me.skywars2.game;

import eu.pb4.sgui.api.SguiUtils;
import eu.pb4.sgui.api.gui.SimpleGui;
import eu.pb4.sgui.api.ClickType;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.world.inventory.ContainerInput;

/** Actual hub button, native dialog codec/input, authenticated connection and durable personal slots. */
public final class GameBuildChecks {
    private GameBuildChecks() {}
    private static void require(boolean value,String reason){if(!value)throw new IllegalStateException("Build manager: "+reason);}
    private static MultiActionDialog dialog(RegenerationBeltChecks.ScheduledClient client){
        var packets=client.packets().stream().filter(ClientboundShowDialogPacket.class::isInstance)
                .map(ClientboundShowDialogPacket.class::cast).toList();
        require(!packets.isEmpty(),"native dialog packet sent");var packet=packets.getLast();
        var bytes=Unpooled.buffer();try{
            ClientboundShowDialogPacket.CONTEXT_FREE_STREAM_CODEC.encode(bytes,packet);
            return (MultiActionDialog)ClientboundShowDialogPacket.CONTEXT_FREE_STREAM_CODEC.decode(bytes).dialog().value();
        }finally{bytes.release();}
    }
    private static Identifier id(MultiActionDialog dialog,int index){
        var action=dialog.actions().get(index).action().orElseThrow();
        if(action instanceof CustomAll custom)return custom.id();
        return ((ClickEvent.Custom)((StaticAction)action).value()).id();
    }
    private static void click(WoolBucketChecks.Fixture f,Identifier id,CompoundTag payload){
        f.client.listener.handleCustomClickAction(new ServerboundCustomClickActionPacket(id,Optional.ofNullable(payload)));
    }
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var stage=GameController.class.getDeclaredField("stage");stage.setAccessible(true);
            var ready=GameController.class.getDeclaredField("lobbyReady");ready.setAccessible(true);
            var oldStage=game.stage();boolean oldReady=ready.getBoolean(game);
            try{
                stage.set(game,GameController.Stage.LOBBY);ready.setBoolean(game,true);
                var p=game.players().get(f.owner.getUUID());p.savedBuilds="";game.config().ranked=true;
                game.config().lbcTrinketLimit=4;game.config().lbcLevelupLimit=2;
                p.kit=0;p.skill=0;p.lbcKit=1;p.lbcSkill=0;p.trinkets=new int[]{1,1,2,0,0,0,0,0,0,0,0,0};
                p.levelups=new int[]{1,3,0,0,0,0,0,0,0,0,0,0};
                game.menus().open(f.owner);var menu=(SimpleGui)SguiUtils.getCurrentGui(f.owner);
                require(menu.getGuiElement(17)!=null,"original hub slot17 retains its design");
                menu.click(17,ClickType.MOUSE_LEFT,ContainerInput.PICKUP);
                var main=dialog(f.client);require(main.actions().size()==48&&main.columns()==6,"24 source slots and delete buttons, six columns");
                var open=id(main,0);click(f,open,null);var save=dialog(f.client);
                require(save.common().inputs().size()==1&&save.common().inputs().getFirst().key().equals("build_name"),"empty slot opens native naming input");
                var saveId=id(save,0);var payload=new CompoundTag();payload.putString("build_name","Fixture build");
                f.audience.listener.handleCustomClickAction(new ServerboundCustomClickActionPacket(saveId,Optional.of(payload)));
                require(game.players().get(f.other.getUUID()).savedBuilds.isEmpty()&&p.savedBuilds.isEmpty(),"foreign connection cannot use owner's token");
                click(f,saveId,payload);require(GameBuilds.read(p)[0].name().equals("Fixture build"),"native save response persisted named build");
                require(PlayerStore.load().get(f.owner.getUUID()).savedBuilds.equals(p.savedBuilds),"canonical atomic player store reloads same personal slots");
                String saved=p.savedBuilds;click(f,saveId,payload);require(p.savedBuilds.equals(saved),"replayed naming packet cannot save twice");
                p.lbcKit=0;Arrays.fill(p.trinkets,0);Arrays.fill(p.levelups,0);
                click(f,id(dialog(f.client),0),null);
                require(p.lbcKit==1&&p.trinkets[0]==1&&p.trinkets[1]==1&&p.trinkets[2]==2
                        &&p.levelups[0]==1&&p.levelups[1]==3,"actual load restores repeated trinkets and distinct upgrades");
                game.config().lbcTrinketLimit=1;game.config().lbcLevelupLimit=1;click(f,id(dialog(f.client),0),null);
                require(p.trinkets[0]==1&&p.trinkets[1]==0&&p.levelups[0]==1&&p.levelups[1]==0,"live configured caps applied at load");
                click(f,id(dialog(f.client),1),null);require(GameBuilds.read(p)[0]==null,"delete frees precisely the original personal slot");
                var stale=id(dialog(f.client),0);stage.set(game,GameController.Stage.PLAYING);f.owner.closeContainer();
                click(f,stale,null);require(GameBuilds.read(p)[0]==null,"a lobby-issued action cannot mutate a running non-debug match");
            }finally{stage.set(game,oldStage);ready.setBoolean(game,oldReady);}
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Build manager fixture",error);}
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS build-manager (actual hub button/native dialog codec, authenticated input, personal nonce/replay, atomic disk reload, repeated trinkets/unique upgrades, live caps/delete and running-match denial)");
    }
}
