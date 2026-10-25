package me.skywars2.game;

import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Rifle projection zoom plus independent scope-overlay entry/exit effects. */
public final class LbcRifleZoom {
    /** Native 26.3 client reads this before predicting each LMB swing; attack damage remains native. */
    public static final net.minecraft.world.item.component.SwingAnimation SCOPE_ATTACK_ANIMATION=
            new net.minecraft.world.item.component.SwingAnimation(net.minecraft.world.item.SwingAnimationType.NONE,6);
    private static final Identifier ZOOM=Identifier.parse("lbc:zoom");
    private static final Identifier ZOOM_OUT=Identifier.parse("lbc:zoom_out");
    private static final int EXIT_TICKS=6;
    private final GameController game;
    private final Map<UUID,State> states=new HashMap<>();
    // Outbound packets may originate outside the simulation thread. Publish only immutable FOV inputs.
    private final Map<UUID,Float> scopedMovement=new ConcurrentHashMap<>();

    private static final class State {
        final ServerPlayer player;
        final ServerLevel level;
        final InteractionHand hand;
        final ItemStack gun;
        int lastPunch;
        int exitUntil;
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
            state.lastPunch=tick;
            return;
        }
        state.lastPunch=tick;
        if(state.active) leave(state,tick,true); else {
            state.active=true; state.exitUntil=0;
            player.removePostEffect(ZOOM_OUT);
            player.addPostEffect(ZOOM);
            scopedMovement.put(player.getUUID(),movement(player));
            syncAbilities(player);
        }
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
    private static float movement(ServerPlayer player) {
        return (float)player.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }
    private static void syncAbilities(ServerPlayer player) {
        player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
    }
    private void leave(State state,int now,boolean animate) {
        state.active=false;
        scopedMovement.remove(state.player.getUUID());
        state.player.removePostEffect(ZOOM);
        syncAbilities(state.player);
        if(animate) {
            state.player.addPostEffect(ZOOM_OUT);
            state.exitUntil=now+EXIT_TICKS;
        } else {
            state.player.removePostEffect(ZOOM_OUT);
            state.exitUntil=0;
        }
    }

    /** Client-only walking-speed denominator. Native movement uses MOVEMENT_SPEED, not this packet field. */
    public ClientboundPlayerAbilitiesPacket projectAbilities(UUID player,ClientboundPlayerAbilitiesPacket packet) {
        Float speed=scopedMovement.get(player);
        if(speed==null || !Float.isFinite(speed) || speed==0f) return packet;
        double denominator=-2d*speed;
        if(!Double.isFinite(denominator) || Math.abs(denominator)>Float.MAX_VALUE) return packet;
        float walking=(float)denominator;
        if(walking==0 || !Float.isFinite(walking)) return packet;
        var view=new Abilities();
        view.invulnerable=packet.isInvulnerable(); view.flying=packet.isFlying();
        view.mayfly=packet.canFly(); view.instabuild=packet.canInstabuild();
        view.setFlyingSpeed(packet.getFlyingSpeed()); view.setWalkingSpeed(walking);
        // 26.3 AbstractClientPlayer: (MOVEMENT_SPEED/walkingSpeed+1)/2 = .25.
        // The client's FOV Effects preference remains authoritative; no gameplay ability is modified.
        return new ClientboundPlayerAbilitiesPacket(view);
    }
    public void tick() { tick(game.server().getTickCount()); }
    void tick(int now) {
        for(var iterator=states.values().iterator();iterator.hasNext();) {
            State state=iterator.next();
            if(!state.player.isAlive() || state.player.isRemoved() || state.player.level()!=state.level
                    || !game.canUseGameplayItems(state.player)) {
                if(state.active) leave(state,now,false);
                else state.player.removePostEffect(ZOOM_OUT);
                iterator.remove();
                continue;
            }
            if(state.active && !current(state))
                leave(state,now,state.player.isAlive() && !state.player.isRemoved()
                        && state.player.level()==state.level && game.canUseGameplayItems(state.player));
            if(state.active) {
                float speed=movement(state.player);
                Float previous=scopedMovement.put(state.player.getUUID(),speed);
                if(previous==null || Float.floatToIntBits(previous)!=Float.floatToIntBits(speed))
                    syncAbilities(state.player);
            }
            if(state.exitUntil!=0 && now>=state.exitUntil) {
                state.player.removePostEffect(ZOOM_OUT); state.exitUntil=0;
            }
            if(!state.active && state.exitUntil==0 && !current(state)) iterator.remove();
        }
    }
    public void forget(ServerPlayer player) {
        State state=states.remove(player.getUUID());
        scopedMovement.remove(player.getUUID());
        player.removePostEffect(ZOOM); player.removePostEffect(ZOOM_OUT);
        if(state!=null && state.active) syncAbilities(player);
    }
    public void stop() {
        for(State state:states.values()) {
            scopedMovement.remove(state.player.getUUID());
            state.player.removePostEffect(ZOOM); state.player.removePostEffect(ZOOM_OUT);
            if(state.active) syncAbilities(state.player);
        }
        states.clear(); scopedMovement.clear();
    }
}
