package me.skywars2.game;

import eu.pb4.sgui.api.SguiUtils;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Native chest ownership/audio and Fabric inputs, without opening personal storage. */
public final class GameMenuChestChecks {
    private GameMenuChestChecks(){}
    private static void require(boolean condition,String why){if(!condition)throw new IllegalStateException("Menu chest: "+why);}
    private static long sounds(RegenerationBeltChecks.ScheduledClient client,String id){
        return client.packets().stream().filter(ClientboundSoundPacket.class::isInstance)
                .map(ClientboundSoundPacket.class::cast).filter(p->p.getSound().value().location().toString().equals(id)).count();
    }
    private static int count(EnderChestBlockEntity chest){return ((ContainerOpenersCounter)WoolBucketChecks.read(chest,"openersCounter")).getOpenerCount();}
    private static void lid(WoolBucketChecks.Fixture f,BlockPos pos,int count){
        require(((java.util.Collection<?>)WoolBucketChecks.read(f.level,"blockEvents")).stream()
                .anyMatch(e->e instanceof BlockEventData event&&event.pos().equals(pos)&&event.block()==Blocks.ENDER_CHEST
                        &&event.paramA()==1&&event.paramB()==count),"native queued lid event "+count);
    }
    public static void run(GameController game) {
        try { runNative(game); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException("Native chest fixture state",error); }
    }
    private static void runNative(GameController game) throws ReflectiveOperationException {
        var stage=GameController.class.getDeclaredField("stage");stage.setAccessible(true);var original=game.stage();
        var ready=GameController.class.getDeclaredField("lobbyReady");ready.setAccessible(true);boolean originalReady=ready.getBoolean(game);
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            try{
                f.prepareParticipants();f.owner.setGameMode(GameType.SURVIVAL);f.other.setGameMode(GameType.SURVIVAL);
                f.move(f.other,f.home.add(0,0,2));
                var pos=BlockPos.containing(f.home).east();f.set(pos,Blocks.ENDER_CHEST.defaultBlockState());
                var chest=(EnderChestBlockEntity)f.level.getBlockEntity(pos);
                require(chest!=null&&count(chest)==0,"resident native ender chest");
                stage.set(game,GameController.Stage.LOBBY);ready.setBoolean(game,true);require(game.inLobby(),"real ready lobby admission");
                f.client.packets().clear();f.audience.packets().clear();
                var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
                require(UseBlockCallback.EVENT.invoker().interact(f.owner,f.level,InteractionHand.MAIN_HAND,hit).consumesAction(),"real RMB callback opens hub");
                require(SguiUtils.getCurrentGui(f.owner) instanceof SimpleGui&&count(chest)==1
                        &&f.owner.getEnderChestInventory().isActiveChest(chest),"native opener owned by actual GUI");
                lid(f,pos,1);chest.recheckOpen();require(count(chest)==1,"native five-tick recheck retains GUI opener");
                var menu=f.owner.containerMenu;game.menus().open(f.owner,GameMenus.Page.STATS);
                require(f.owner.containerMenu==menu&&count(chest)==1,"page navigation retains one native opener");
                require(AttackBlockCallback.EVENT.invoker().interact(f.other,f.level,InteractionHand.MAIN_HAND,pos,Direction.UP).consumesAction(),"real LMB callback opens second viewer");
                require(count(chest)==2,"two players share native opener count");chest.recheckOpen();require(count(chest)==2,"native recheck sees both GUI viewers");
                for(var client:java.util.List.of(f.client,f.audience))require(sounds(client,"minecraft:block.ender_chest.open")==1,"one positional opening sound shared by viewers");
                var old=(SimpleGui)SguiUtils.getCurrentGui(f.owner);f.owner.closeContainer();
                require(count(chest)==1&&!f.owner.getEnderChestInventory().isActiveChest(chest),"first close releases only that viewer");
                require(sounds(f.audience,"minecraft:block.ender_chest.close")==0,"chest stays open for other viewer");
                f.other.closeContainer();require(count(chest)==0,"last native close releases chest");lid(f,pos,0);
                old.onRemoved();require(count(chest)==0,"repeated SGui removal cannot underflow native openers");
                for(var client:java.util.List.of(f.client,f.audience))require(sounds(client,"minecraft:block.ender_chest.close")==1,"one native close sound at last viewer");
                game.menus().open(f.owner);require(count(chest)==0,"command-only GUI does not animate an unrelated chest");f.owner.closeContainer();
                stage.set(game,original);ready.setBoolean(game,originalReady);game.menus().openFromChest(f.owner,pos,true);
                require(game.menus().debugAccess(f.owner)&&count(chest)==1,"gallery debug entry uses same native chest lifecycle during match");
                game.menus().openFromChest(f.other,pos,true);require(count(chest)==1,"nonoperator cannot attach to debug chest");
                f.move(f.owner,f.home.add(12,0,0));((SimpleGui)SguiUtils.getCurrentGui(f.owner)).onTick();
                require(f.owner.containerMenu==f.owner.inventoryMenu&&count(chest)==0
                        &&!f.owner.getEnderChestInventory().isActiveChest(chest),"leaving native interaction range closes GUI and releases active chest");
            }finally{f.owner.closeContainer();f.other.closeContainer();stage.set(game,original);ready.setBoolean(game,originalReady);}
        }
        SkyWars2.LOGGER.info("[check] PASS menu-chest (native RMB/LMB, queued lid events, native rechecks/two viewers, positional open/last-close audio, navigation/removal/range and debug permissions)");
    }
}
