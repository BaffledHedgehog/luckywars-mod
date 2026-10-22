package me.skywars2.game;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Toggle only the rifle post effect; preserve all other client post effects. */
public final class LbcRifleZoom {
    private static final Identifier ZOOM=Identifier.parse("lbc:zoom");
    private final GameController game;
    private final Map<UUID,State> states=new HashMap<>();

    private static final class State {
        final ServerPlayer player;
        final ServerLevel level;
        final InteractionHand hand;
        final ItemStack gun;
        int lastPunch;
        boolean active;
        State(ServerPlayer player,InteractionHand hand) {
            this.player=player; this.level=player.level(); this.hand=hand;
            this.gun=player.getItemInHand(hand);
        }
    }

    LbcRifleZoom(GameController game) { this.game=game; }

    public void punch(ServerPlayer player) { punch(player,game.server().getTickCount()); }

    // Explicit tick permits deterministic checks of duplicate/continuous mining packets.
    void punch(ServerPlayer player,int tick) {
        if(!allowed(player)) return;
        InteractionHand hand=rifle(player.getMainHandItem()) ? InteractionHand.MAIN_HAND
                : rifle(player.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
        if(hand==null) return;
        State state=states.get(player.getUUID());
        if(state!=null && !current(state)) { forget(player); state=null; }
        if(state==null) {
            state=new State(player,hand);
            states.put(player.getUUID(),state);
        } else if(tick-state.lastPunch<=1) {
            // A held attack on a block sends punches every tick, unlike a click in air.
            // Treat that stream as one press, including duplicate packets in one tick.
            state.lastPunch=tick;
            return;
        }
        state.lastPunch=tick;
        state.active=!state.active;
        if(state.active) player.addPostEffect(ZOOM); else player.removePostEffect(ZOOM);
    }

    private static boolean rifle(ItemStack stack) {
        return LbcShooters.isSlr(stack) || LbcMagnum.isMagnum(stack);
    }

    private boolean allowed(ServerPlayer player) {
        return player.isAlive() && !player.isRemoved() && !game.isObserver(player)
                && game.canUseGameplayItems(player) && !game.items().actionsBlocked(player);
    }

    private boolean current(State state) {
        return allowed(state.player) && state.player.level()==state.level
                && state.player.getItemInHand(state.hand)==state.gun && rifle(state.gun);
    }

    public void tick() {
        for(var iterator=states.values().iterator();iterator.hasNext();) {
            State state=iterator.next();
            if(!current(state)) {
                if(state.active) state.player.removePostEffect(ZOOM);
                iterator.remove();
            }
        }
    }

    public void forget(ServerPlayer player) {
        State state=states.remove(player.getUUID());
        if(state!=null && state.active) state.player.removePostEffect(ZOOM);
    }

    public void stop() {
        for(State state:states.values()) if(state.active) state.player.removePostEffect(ZOOM);
        states.clear();
    }
}
