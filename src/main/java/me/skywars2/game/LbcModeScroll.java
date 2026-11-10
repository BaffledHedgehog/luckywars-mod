package me.skywars2.game;

import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Wheel input reuses the real Shift-use path; gameplay state remains owned by each item. */
public final class LbcModeScroll {
    private static final ThreadLocal<Integer> STEPS=new ThreadLocal<>();
    private static final WeakHashMap<ServerGamePacketListenerImpl,Input> INPUTS=new WeakHashMap<>();
    private static int serial=0x53570000;
    private static final class Input {
        final ServerPlayer player;final int held;int raw,tick;final Set<Integer> barriers=new HashSet<>();
        Input(ServerPlayer player,int tick){this.player=player;held=raw=player.getInventory().getSelectedSlot();this.tick=tick;}
    }
    private LbcModeScroll(){}
    public static boolean scrolling(){return STEPS.get()!=null;}
    static int next(int mode,int count){return Math.floorMod(mode-1+steps(1),count)+1;}
    static int nextZero(int mode,int count){return Math.floorMod(mode+steps(1),count);}
    static int steps(int ordinary){var value=STEPS.get();return value==null?ordinary:value;}
    static <T> T withSteps(int steps,Supplier<T> action){
        var previous=STEPS.get();STEPS.set(steps);
        try{return action.get();}finally{if(previous==null)STEPS.remove();else STEPS.set(previous);}
    }
    static int delta(int previous,int next){return Math.floorMod(next-previous+4,9)-4;}
    private static boolean supports(GameController game,ItemStack stack){
        if(LbcShooters.isSlr(stack)||LbcMagnum.isMagnum(stack)||LbcWitchClaims.matches(stack)
                ||LbcHedgehogItems.isAcademy(stack)||LbcHalloweenWind.academy(stack)||LbcSoulless.isAcademy(stack))return true;
        var data=LbcFartus.data(stack);
        return game.items().wands().hasSelectableMode(stack)
                ||data.getIntOr("upgraded",0)==1&&LbcChaosRewardItems.book(stack);
    }
    /** Called after the native server-thread barrier, before its selected-slot mutation. */
    public static boolean carried(ServerGamePacketListenerImpl listener,int slot){
        var game=SkyWars2.game();var player=listener.player;
        if(slot<0||slot>=9||game==null||!player.isShiftKeyDown()
                ||!game.players().get(player.getUUID()).modeScroll||!supports(game,player.getMainHandItem())){
            INPUTS.remove(listener);return false;
        }
        if(game.vanity().blocked(player)||game.items().actionsBlocked(player))return false;
        int now=game.server().getTickCount();var input=INPUTS.get(listener);
        if(input==null||input.player!=player||input.held!=player.getInventory().getSelectedSlot()||now-input.tick>200){
            input=new Input(player,now);INPUTS.put(listener,input);
        }
        int steps=delta(input.raw,slot);input.raw=slot;input.tick=now;
        if(steps!=0){
            withSteps(steps,()->player.gameMode.useItem(player,player.level(),player.getMainHandItem(),InteractionHand.MAIN_HAND));
            player.containerMenu.broadcastChanges();player.resetLastActionTime();
        }
        listener.send(new ClientboundSetHeldSlotPacket(input.held));
        // The client's ordered Pong identifies exactly when its raw wheel anchor was reset.
        int barrier=++serial;input.barriers.add(barrier);
        if(input.barriers.size()>256)input.barriers.clear();
        listener.send(new ClientboundPingPacket(barrier));
        return true;
    }
    public static void pong(ServerGamePacketListenerImpl listener,int id){
        var input=INPUTS.get(listener);
        if(input!=null&&input.barriers.remove(id))input.raw=input.held;
    }
    public static void forget(ServerPlayer player){INPUTS.entrySet().removeIf(e->e.getValue().player==player);}
}
