package me.skywars2.game;

import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Rifle projection zoom plus independent scope-overlay entry/exit effects. */
public final class LbcRifleZoom {
    /** Native 26.3 client reads this before predicting each LMB swing; attack damage remains native. */
    public static final net.minecraft.world.item.component.SwingAnimation SCOPE_ATTACK_ANIMATION=
            new net.minecraft.world.item.component.SwingAnimation(net.minecraft.world.item.SwingAnimationType.NONE,6);
    private enum Scope {
        SLG("slg","super_rifle"), MAGNUM("magnum","magnum");
        final Identifier effect,out,model;
        Scope(String name,String model) {
            effect=Identifier.parse("lbc:zoom_"+name);out=Identifier.parse("lbc:zoom_"+name+"_out");
            this.model=Identifier.parse("lbc:"+model+"_scoped");
        }
        static Scope of(ItemStack stack) { return LbcShooters.isSlr(stack)?SLG:LbcMagnum.isMagnum(stack)?MAGNUM:null; }
    }
    private static final java.util.List<Identifier> OWNED_EFFECTS=java.util.List.of(
            Identifier.parse("lbc:zoom"),Identifier.parse("lbc:zoom_out"),
            Scope.SLG.effect,Scope.SLG.out,Scope.MAGNUM.effect,Scope.MAGNUM.out);
    private static boolean projectionRegistered;
    private static final int EXIT_TICKS=6;
    private final GameController game;
    private final Map<UUID,State> states=new HashMap<>();
    // Outbound packets may originate outside the simulation thread. Publish only immutable FOV inputs.
    private final Map<UUID,Float> scopedMovement=new ConcurrentHashMap<>();
    private final Map<UUID,Float> movementViews=new ConcurrentHashMap<>();
    private final Map<UUID,Scope> modelViews=new ConcurrentHashMap<>();
    enum SpeedFovPolicy { GAIN_FIVE_PERCENT, CLAMP_FIVE_PERCENT }
    static final SpeedFovPolicy SPEED_FOV_POLICY=SpeedFovPolicy.GAIN_FIVE_PERCENT;

    /** Attenuates only the speed term; flight and held-item FOV remain client-native. */
    static double speedFov(double speed,double walking,SpeedFovPolicy policy) {
        if(!Double.isFinite(speed)||!Double.isFinite(walking)||walking==0)return 1;
        double original=(speed/walking+1)/2;
        if(!Double.isFinite(original))return 1;
        double delta=original-1;
        return 1+(policy==SpeedFovPolicy.CLAMP_FIVE_PERCENT?Math.clamp(delta,-.05,.05):delta*.05);
    }

    static float projectedWalking(float speed,float walking,boolean scoped,SpeedFovPolicy policy) {
        if(!Float.isFinite(speed))return walking;
        // Client skips its entire speed ratio for walkingSpeed0; also handles speed0 scope safely.
        if(speed==0)return 0;
        double target=scoped?.25:speedFov(speed,walking,policy);
        double divisor=2*target-1;
        if(!Double.isFinite(divisor)||divisor==0)return walking;
        double result=speed/divisor;
        if(!Double.isFinite(result)||Math.abs(result)>Float.MAX_VALUE)return walking;
        float projected=(float)result;
        return projected==0||!Float.isFinite(projected)?walking:projected;
    }

    private static final class State {
        final ServerPlayer player;
        final ServerLevel level;
        final InteractionHand hand;
        final ItemStack gun;
        final Scope scope;
        int lastPunch;
        int exitUntil;
        boolean active;
        State(ServerPlayer player,InteractionHand hand) {
            this.player=player; this.level=player.level(); this.hand=hand;
            this.gun=player.getItemInHand(hand);
            this.scope=Scope.of(gun);
        }
    }

    LbcRifleZoom(GameController game) { this.game=game; registerProjection(); }
    private static Identifier projectedModel(ItemStack original,net.fabricmc.fabric.api.networking.v1.context.PacketContext context) {
        var kind=Scope.of(original);
        if(kind==null)return null;
        var viewer=eu.pb4.polymer.common.api.PolymerCommonUtils.getPlayer(context);
        var controller=me.skywars2.SkyWars2.game();
        if(viewer==null||controller==null)return null;
        var scope=controller.items().rifleZoom().modelViews.get(viewer.getUUID());
        return scope!=null&&kind==scope?scope.model:null;
    }
    private static void registerProjection() {
        if(projectionRegistered)return;projectionRegistered=true;
        eu.pb4.polymer.core.api.item.PolymerItemUtils.CONTEXT_ITEM_CHECK.register(
                (stack,context)->stack instanceof ItemStack item&&projectedModel(item,context)!=null);
        eu.pb4.polymer.core.api.item.PolymerItemUtils.ITEM_MODIFICATION_EVENT.register((original,client,context)->{
            var model=projectedModel(original,context);
            if(model==null||client.isEmpty())return client;
            var result=client.copy();result.set(DataComponents.ITEM_MODEL,model);return result;
        });
    }
    private static void clearEffects(ServerPlayer player) {
        for(var effect:OWNED_EFFECTS)player.removePostEffect(effect);
    }
    private static void refreshModels(ServerPlayer player) {
        // Native menu snapshots; no authoritative stack/slot is replaced for presentation.
        player.inventoryMenu.sendAllDataToRemote();
        if(player.containerMenu!=player.inventoryMenu)player.containerMenu.sendAllDataToRemote();
    }
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
            clearEffects(player);
            player.addPostEffect(state.scope.effect);
            modelViews.put(player.getUUID(),state.scope);
            scopedMovement.put(player.getUUID(),movement(player));
            syncAbilities(player);
            refreshModels(player);
        }
    }
    boolean scoped(ServerPlayer player) {
        var state=states.get(player.getUUID());
        return state!=null && state.player==player && state.active && current(state);
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
        // Unmodified clients still sanitize this synchronized attribute to vanilla26.3 range.
        // Mirror their value for projection even when server-side limits permit larger speeds.
        return (float)Math.clamp(player.getAttributeValue(Attributes.MOVEMENT_SPEED),0,1024);
    }
    private static void syncAbilities(ServerPlayer player) {
        player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
    }
    private void leave(State state,int now,boolean animate) {
        state.active=false;
        scopedMovement.remove(state.player.getUUID());
        modelViews.remove(state.player.getUUID());
        clearEffects(state.player);
        syncAbilities(state.player);
        refreshModels(state.player);
        if(animate) {
            state.player.addPostEffect(state.scope.out);
            state.exitUntil=now+EXIT_TICKS;
        } else {
            state.exitUntil=0;
        }
    }

    /** Client-only walking-speed denominator. Native movement uses MOVEMENT_SPEED, not this packet field. */
    public ClientboundPlayerAbilitiesPacket projectAbilities(ServerPlayer player,ClientboundPlayerAbilitiesPacket packet) {
        // Login can send abilities before PlayerList publishes this player to the global list.
        if(game.server().isSameThread())movementViews.put(player.getUUID(),movement(player));
        return projectAbilities(player.getUUID(),packet);
    }
    public ClientboundPlayerAbilitiesPacket projectAbilities(UUID player,ClientboundPlayerAbilitiesPacket packet) {
        // Join/native sync can precede the first global tick. Read entities only on the server thread.
        if(game.server().isSameThread()) {
            var current=game.server().getPlayerList().getPlayer(player);
            if(current!=null)movementViews.put(player,movement(current));
        }
        Float speed=movementViews.get(player);
        if(speed==null)return packet;
        float walking=projectedWalking(speed,packet.getWalkingSpeed(),scopedMovement.containsKey(player),SPEED_FOV_POLICY);
        if(Float.floatToIntBits(walking)==Float.floatToIntBits(packet.getWalkingSpeed()))return packet;
        var view=new Abilities();
        view.invulnerable=packet.isInvulnerable(); view.flying=packet.isFlying();
        view.mayfly=packet.canFly(); view.instabuild=packet.canInstabuild();
        view.setFlyingSpeed(packet.getFlyingSpeed()); view.setWalkingSpeed(walking);
        // 26.3 AbstractClientPlayer: (MOVEMENT_SPEED/walkingSpeed+1)/2.
        // The client's FOV Effects preference remains authoritative; no gameplay ability is modified.
        return new ClientboundPlayerAbilitiesPacket(view);
    }
    public void tick() { tick(game.server().getTickCount()); }
    void tick(int now) {
        var online=new java.util.HashSet<UUID>();
        for(var player:game.server().getPlayerList().getPlayers()) {
            online.add(player.getUUID());
            float speed=movement(player);
            Float previous=movementViews.put(player.getUUID(),speed);
            if(previous==null||Float.floatToIntBits(previous)!=Float.floatToIntBits(speed))syncAbilities(player);
        }
        movementViews.keySet().retainAll(online);
        for(var iterator=states.values().iterator();iterator.hasNext();) {
            State state=iterator.next();
            if(!state.player.isAlive() || state.player.isRemoved() || state.player.level()!=state.level
                    || !game.canUseGameplayItems(state.player)) {
                if(state.active) leave(state,now,false);
                else clearEffects(state.player);
                iterator.remove();
                continue;
            }
            if(state.active && !current(state))
                leave(state,now,state.player.getItemInHand(state.hand)==state.gun && state.player.isAlive() && !state.player.isRemoved()
                        && state.player.level()==state.level && game.canUseGameplayItems(state.player));
            if(state.active) {
                float speed=movement(state.player);
                Float previous=scopedMovement.put(state.player.getUUID(),speed);
                if(previous==null || Float.floatToIntBits(previous)!=Float.floatToIntBits(speed))
                    syncAbilities(state.player);
            }
            if(state.exitUntil!=0 && (now>=state.exitUntil || state.player.getItemInHand(state.hand)!=state.gun)) {
                clearEffects(state.player); state.exitUntil=0;
            }
            if(!state.active && state.exitUntil==0 && !current(state)) iterator.remove();
        }
    }
    public void joined(ServerPlayer player) {
        // Native player data persists post_effects, including saves made before LEAVE.
        // A new connection never resumes an old scope or its six-tick exit animation.
        forget(player);
        player.sendPostEffects();
    }
    public void forget(ServerPlayer player) {
        State state=states.remove(player.getUUID());
        scopedMovement.remove(player.getUUID());
        modelViews.remove(player.getUUID());
        clearEffects(player);
        if(state!=null && state.active) { syncAbilities(player);refreshModels(player); }
    }
    public void stop() {
        for(State state:states.values()) {
            scopedMovement.remove(state.player.getUUID());
            modelViews.remove(state.player.getUUID());
            clearEffects(state.player);
            if(state.active) { syncAbilities(state.player);refreshModels(state.player); }
        }
        states.clear(); scopedMovement.clear();modelViews.clear();
        // Global speed attenuation continues in the lobby after a match stops.
    }
    TraceView tracer(ServerPlayer owner) { return new TraceView(owner,scoped(owner)); }
    static final class TraceView {
        private final ServerPlayer owner;
        private final Vec3 eye,down;
        private final boolean scoped;
        TraceView(ServerPlayer owner,boolean scoped) {
            this.owner=owner;this.scoped=scoped;eye=owner.getEyePosition();down=owner.getUpVector(1).normalize().scale(-1);
        }
        void send(net.minecraft.core.particles.ParticleOptions particle,Vec3 point,int count,double spread,double speed) {
            var level=owner.level();double distance=eye.distanceToSqr(point);
            if(!scoped||distance>=64) {
                level.sendParticles(particle,true,false,point.x,point.y,point.z,count,spread,spread,spread,speed);return;
            }
            double fade=1-Math.sqrt(distance)/8;
            var local=point.add(down.scale(.22*fade*fade));
            for(var viewer:level.players()) {
                var at=viewer==owner?local:point;
                level.sendParticles(viewer,particle,true,false,at.x,at.y,at.z,count,spread,spread,spread,speed);
            }
        }
    }
}
