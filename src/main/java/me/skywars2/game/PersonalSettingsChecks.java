package me.skywars2.game;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.SguiUtils;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.skywars2.SkyWars2;
import net.minecraft.world.inventory.ContainerInput;

/** Native GUI input, durable UUID profile and vanilla-client FOV arithmetic. */
public final class PersonalSettingsChecks {
    private PersonalSettingsChecks() { }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Personal settings: "+message);}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var profile=game.players().get(f.owner.getUUID());int previous=profile.fovEffects;
            var stage=GameController.class.getDeclaredField("stage");stage.setAccessible(true);
            var ready=GameController.class.getDeclaredField("lobbyReady");ready.setAccessible(true);
            var oldStage=game.stage();boolean oldReady=ready.getBoolean(game);
            try{
                require(new com.google.gson.Gson().fromJson("{}",PlayerStore.Profile.class).fovEffects==5,"missing preference defaults to five percent");
                profile.fovEffects=5;stage.set(game,GameController.Stage.LOBBY);ready.setBoolean(game,true);
                game.menus().open(f.owner,GameMenus.Page.COSMETICS);
                var gui=(SimpleGui)SguiUtils.getCurrentGui(f.owner);require(gui.getGuiElement(22)!=null,"cosmetic hub contains personal tab");
                gui.click(22,ClickType.MOUSE_LEFT,ContainerInput.PICKUP);
                gui=(SimpleGui)SguiUtils.getCurrentGui(f.owner);require(gui.getGuiElement(13)!=null,"native personal FOV button rendered");
                gui.click(13,ClickType.MOUSE_LEFT,ContainerInput.PICKUP);require(profile.fovEffects==5,"unrequested left click does not change preference");
                gui.click(13,ClickType.MOUSE_RIGHT,ContainerInput.PICKUP);require(profile.fovEffects==6,"right click adds one");
                gui.click(13,ClickType.MOUSE_RIGHT_SHIFT,ContainerInput.QUICK_MOVE);require(profile.fovEffects==16,"shift right adds ten");
                profile.fovEffects=99;gui.click(13,ClickType.MOUSE_RIGHT,ContainerInput.PICKUP);require(profile.fovEffects==100,"one hundred is selectable");
                gui.click(13,ClickType.MOUSE_RIGHT,ContainerInput.PICKUP);require(profile.fovEffects==0,"wrap one hundred to zero");
                profile.fovEffects=95;gui.click(13,ClickType.MOUSE_RIGHT_SHIFT,ContainerInput.QUICK_MOVE);require(profile.fovEffects==4,"shift wraps modulo101");
                require(PlayerStore.load().get(f.owner.getUUID()).fovEffects==4,"real player store retains selected percent");
                for(int percent:new int[]{0,5,100})for(double multiplier:new double[]{1,1.1,.85,.935})for(float speed:new float[]{.05f,.1f,.13f,.4f,1024}){
                    float walking=LbcRifleZoom.projectedWalking(speed,.1f,false,percent,multiplier);
                    double actual=multiplier*(speed/walking+1)/2;
                    double expected=1+(multiplier*(speed/(double).1f+1)/2-1)*percent/100.0;
                    require(Math.abs(actual-expected)<Math.max(1e-5,Math.abs(expected)*2e-7),"speed, sprint, flight and bow use native preference curve");
                    float scope=LbcRifleZoom.projectedWalking(speed,.1f,true,percent,multiplier);
                    require(Math.abs((speed/scope+1)/2-.25)<1e-5,"weapon scopes keep full strength at every preference");
                }
                SkyWars2.LOGGER.info("[check] PASS personal-settings (native cosmetic tab/clicks, modulo101, durable UUID preference, default5, speed/sprint/flight/bow curves and full scopes)");
            }finally{
                profile.fovEffects=previous;game.players().save();game.items().rifleZoom().personalSettingsChanged(f.owner);
                var gui=SguiUtils.getCurrentGui(f.owner);if(gui!=null)gui.close();stage.set(game,oldStage);ready.setBoolean(game,oldReady);
            }
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
}
