package me.skywars2.game;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
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
    private int checkpointDepth;
    private boolean rewinding,checkpointProjection;
    private final Map<UUID,ScopeCheckpoint> checkpointPublication=new java.util.LinkedHashMap<>();
    private final Map<UUID,State> states=new HashMap<>();
    // Outbound packets may originate outside the simulation thread. Publish only immutable FOV inputs.
    private final Map<UUID,Float> scopedMovement=new ConcurrentHashMap<>();
    private record MovementView(float speed,float base) {}
    private final Map<UUID,MovementView> movementViews=new ConcurrentHashMap<>();
    private final Map<UUID,ClientboundPlayerAbilitiesPacket> abilityViews=new ConcurrentHashMap<>();
    private final Map<UUID,Scope> modelViews=new ConcurrentHashMap<>();
    enum SpeedFovPolicy { GAIN_FIVE_PERCENT, CLAMP_FIVE_PERCENT }
    static final SpeedFovPolicy SPEED_FOV_POLICY=SpeedFovPolicy.GAIN_FIVE_PERCENT;
    private static final double SPEED_FOV_LIMIT=.15;

    /** Attenuates only the speed term; flight and held-item FOV remain client-native. */
    static double speedFov(double speed,double walking,SpeedFovPolicy policy) {
        if(!Double.isFinite(speed)||!Double.isFinite(walking)||walking==0)return 1;
        double original=(speed/walking+1)/2;
        if(!Double.isFinite(original))return 1;
        double delta=original-1;
        double contribution=policy==SpeedFovPolicy.CLAMP_FIVE_PERCENT?Math.clamp(delta,-.05,.05):delta*.05;
        return 1+Math.clamp(contribution,-SPEED_FOV_LIMIT,SPEED_FOV_LIMIT);
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
        ServerPlayer player;
        final ServerLevel level;
        final InteractionHand hand;
        ItemStack gun;
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
        checkpointDepth++;try{
        if(!allowed(player)) return;
        InteractionHand hand=rifle(player.getMainHandItem()) ? InteractionHand.MAIN_HAND : null;
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

        }finally{checkpointDepth--;}
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
        return state.hand==InteractionHand.MAIN_HAND && allowed(state.player) && state.player.level()==state.level
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

    /** Reproduce the unmodified client's synchronized movement value, including packet modifiers. */
    static float packetMovement(ClientboundUpdateAttributesPacket.AttributeSnapshot snapshot) {
        double base=snapshot.base();
        for(var modifier:snapshot.modifiers())if(modifier.operation()==net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE)
            base+=modifier.amount();
        double result=base;
        for(var modifier:snapshot.modifiers())if(modifier.operation()==net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
            result+=base*modifier.amount();
        for(var modifier:snapshot.modifiers())if(modifier.operation()==net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            result*=1+modifier.amount();
        return Double.isNaN(result)?0:(float)Math.clamp(result,0,1024);
    }
    private boolean scopeView(ServerPlayer player) {
        // Gameplay admission is quarantined during restore; packet-only FOV still follows the saved macro state.
        return checkpointProjection||!game.server().isSameThread()?scopedMovement.containsKey(player.getUUID()):scoped(player);
    }
    private static ClientboundPlayerAbilitiesPacket project(ClientboundPlayerAbilitiesPacket packet,float speed,boolean scoped) {
        float walking=projectedWalking(speed,packet.getWalkingSpeed(),scoped,SPEED_FOV_POLICY);
        if(Float.floatToIntBits(walking)==Float.floatToIntBits(packet.getWalkingSpeed()))return packet;
        var view=new Abilities();
        view.invulnerable=packet.isInvulnerable(); view.flying=packet.isFlying();
        view.mayfly=packet.canFly(); view.instabuild=packet.canInstabuild();
        view.setFlyingSpeed(packet.getFlyingSpeed()); view.setWalkingSpeed(walking);
        return new ClientboundPlayerAbilitiesPacket(view);
    }
    /** Client-only denominator always matches the last movement attribute sent to that client. */
    public ClientboundPlayerAbilitiesPacket projectAbilities(ServerPlayer player,ClientboundPlayerAbilitiesPacket packet) {
        abilityViews.put(player.getUUID(),packet);
        var movement=movementViews.get(player.getUUID());
        return movement==null?packet:project(packet,movement.speed(),scopeView(player));
    }
    public ClientboundPlayerAbilitiesPacket projectAbilities(UUID id,ClientboundPlayerAbilitiesPacket packet) {
        if(game.server().isSameThread()) {
            var player=game.server().getPlayerList().getPlayer(id);
            if(player!=null)return projectAbilities(player,packet);
        }
        abilityViews.put(id,packet);
        var movement=movementViews.get(id);
        return movement==null?packet:project(packet,movement.speed(),scopedMovement.containsKey(id));
    }
    /** Bundle movement snapshots with their denominator so a rendered frame never sees only one half. */
    @SuppressWarnings("unchecked")
    public Packet<?> projectPacket(ServerPlayer player,Packet<?> packet) {
        if(packet instanceof net.minecraft.network.protocol.game.ClientboundLoginPacket
                ||packet instanceof net.minecraft.network.protocol.game.ClientboundRespawnPacket) {
            var previous=movementViews.get(player.getUUID());
            boolean login=packet instanceof net.minecraft.network.protocol.game.ClientboundLoginPacket;
            float base=login||previous==null?.1f:previous.base();
            float speed=!login&&previous!=null
                    &&((net.minecraft.network.protocol.game.ClientboundRespawnPacket)packet).shouldKeep(
                            net.minecraft.network.protocol.game.ClientboundRespawnPacket.KEEP_ATTRIBUTE_MODIFIERS)
                    ?previous.speed():base;
            movementViews.put(player.getUUID(),new MovementView(speed,base));
            // Native respawn sends this before connection.player is rebound. Follow the client's
            // actual base/modifier copy flags instead of injecting the removed player's attributes.
            var source=login?null:abilityViews.get(player.getUUID());
            if(source==null&&game.server().isSameThread())source=new ClientboundPlayerAbilitiesPacket(player.getAbilities());
            if(source==null)return packet;
            abilityViews.put(player.getUUID(),source);
            return new ClientboundBundlePacket(java.util.List.of(
                    (Packet<? super ClientGamePacketListener>)packet,project(source,speed,scopeView(player))));
        }
        if(packet instanceof ClientboundPlayerAbilitiesPacket abilities)return projectAbilities(player,abilities);
        if(packet instanceof ClientboundBundlePacket bundle) {
            var children=new java.util.ArrayList<Packet<? super ClientGamePacketListener>>();
            boolean changed=false;
            for(var child:bundle.subPackets()) {
                var projected=projectPacket(player,child);changed|=projected!=child;
                if(projected instanceof ClientboundBundlePacket nested)for(var entry:nested.subPackets())children.add(entry);
                else children.add((Packet<? super ClientGamePacketListener>)projected);
            }
            return changed?new ClientboundBundlePacket(children):packet;
        }
        if(!(packet instanceof ClientboundUpdateAttributesPacket attributes)||attributes.getEntityId()!=player.getId())return packet;
        for(var snapshot:attributes.getValues())if(snapshot.attribute().equals(Attributes.MOVEMENT_SPEED)) {
            float speed=packetMovement(snapshot);
            float base=Double.isNaN(snapshot.base())?0:(float)Math.clamp(snapshot.base(),0,1024);
            movementViews.put(player.getUUID(),new MovementView(speed,base));
            var source=game.server().isSameThread()?new ClientboundPlayerAbilitiesPacket(player.getAbilities()):abilityViews.get(player.getUUID());
            if(source==null)return packet;
            abilityViews.put(player.getUUID(),source);
            return new ClientboundBundlePacket(java.util.List.of(attributes,project(source,speed,scopeView(player))));
        }
        return packet;
    }
    public void tick() { tick(game.server().getTickCount()); }
    void tick(int now) {
        checkpointDepth++;try{
        if(rewinding)return;checkpointProjection=false;
        var online=new java.util.HashSet<UUID>();
        for(var player:game.server().getPlayerList().getPlayers()) {
            online.add(player.getUUID());
            abilityViews.put(player.getUUID(),new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
        }
        movementViews.keySet().retainAll(online);
        abilityViews.keySet().retainAll(online);
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

        }finally{checkpointDepth--;}
    }
    public void joined(ServerPlayer player) {
        checkpointDepth++;try{
        // Native player data persists post_effects, including saves made before LEAVE.
        // A new connection never resumes an old scope or its six-tick exit animation.
        forget(player);
        player.sendPostEffects();

        }finally{checkpointDepth--;}
    }
    public void forget(ServerPlayer player) {
        checkpointDepth++;try{
        State state=states.remove(player.getUUID());
        scopedMovement.remove(player.getUUID());
        modelViews.remove(player.getUUID());
        clearEffects(player);
        if(state!=null && state.active) { syncAbilities(player);refreshModels(player); }

        }finally{checkpointDepth--;}
    }
    public void stop() {
        checkpointDepth++;try{
        rewinding=false;checkpointProjection=false;checkpointPublication.clear();
        for(State state:states.values()) {
            scopedMovement.remove(state.player.getUUID());
            modelViews.remove(state.player.getUUID());
            clearEffects(state.player);
            if(state.active) { syncAbilities(state.player);refreshModels(state.player); }
        }
        states.clear(); scopedMovement.clear();modelViews.clear();
        // Global speed attenuation continues in the lobby after a match stops.

        }finally{checkpointDepth--;}
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
    void checkpointBoundary(){if(checkpointDepth!=0)throw new IllegalStateException("Finish scope input/tick/login callbacks before checkpointing");}
    void prepareCheckpointFrozen(){
        checkpointBoundary();rewinding=true;checkpointProjection=true;checkpointPublication.clear();
        // No leave()/punch(): discard future overlays without an exit animation or any weapon operation.
        for(var player:game.server().getPlayerList().getPlayers())clearEffects(player);
        states.clear();scopedMovement.clear();modelViews.clear();movementViews.clear();abilityViews.clear();
    }
    void publishCheckpointFrozen(){
        checkpointBoundary();int now=game.server().getTickCount();
        for(var row:checkpointPublication.values()){
            var state=states.get(row.id);
            if(state==null||game.server().getPlayerList().getPlayer(row.id)!=state.player||state.player.getItemInHand(state.hand)!=state.gun)
                throw new IllegalStateException("Restore the current scope player and native hand before publication");
        }
        for(var player:game.server().getPlayerList().getPlayers())if(!states.containsKey(player.getUUID()))clearEffects(player);
        for(var row:checkpointPublication.values()){
            var state=states.get(row.id);var player=state.player;clearEffects(player);
            state.lastPunch=now-row.punchAge;state.exitUntil=row.exitTicks==0?0:now+row.exitTicks;
            if(state.active)player.addPostEffect(state.scope.effect);else if(state.exitUntil!=0)player.addPostEffect(state.scope.out);
            // Native player images restore raw abilities/attributes. Existing projection recomputes the current FOV.
            syncAbilities(player);refreshModels(player);
        }
        checkpointPublication.clear();rewinding=false;
        // Keep packet projection authoritative until the first ordinary tick, after the global input barrier ends.
    }
    void cancelCheckpointFrozen(){
        checkpointBoundary();checkpointPublication.clear();states.clear();scopedMovement.clear();modelViews.clear();
        for(var player:game.server().getPlayerList().getPlayers())clearEffects(player);rewinding=false;checkpointProjection=false;
    }
    MatchAimCheckpoint checkpoint(MatchDamageSources sources){return new MatchAimCheckpoint(game,sources);}
    java.util.List<ScopeCheckpoint> checkpointScopes(MatchDamageSources sources){
        checkpointBoundary();if(rewinding)throw new IllegalStateException("Publish scope state before capture");
        var rows=new java.util.ArrayList<ScopeCheckpoint>();
        for(var state:states.values()){
            // A stale old-body/swapped-hand view would be closed by the next ordinary scope tick.
            if(game.server().getPlayerList().getPlayer(state.player.getUUID())!=state.player||state.player.isRemoved()||!state.player.isAlive()
                    ||state.player.level()!=state.level||state.player.getItemInHand(state.hand)!=state.gun||Scope.of(state.gun)!=state.scope)continue;
            rows.add(new ScopeCheckpoint(state,sources));
        }
        return java.util.List.copyOf(rows);
    }
    final class ScopeCheckpoint {
        final State state;final UUID id;final MatchSpellCheckpoint.Links links;final MatchDamageSources.Ref player;
        final ItemStack value;final int punchAge,exitTicks;final boolean active;final MovementView movement;
        ScopeCheckpoint(State state,MatchDamageSources sources){
            this.state=state;id=state.player.getUUID();links=new MatchSpellCheckpoint.Links(sources);player=links.key(state.player);
            if(player.historical()!=null)throw new IllegalStateException("Scope image requires its real current native player");
            value=state.gun.copy();int now=game.server().getTickCount();punchAge=now-state.lastPunch;
            exitTicks=state.exitUntil==0?0:Math.max(0,state.exitUntil-now);active=state.active;movement=movementViews.get(id);
        }
        java.util.Set<UUID> resolve(java.util.function.Function<UUID,net.minecraft.world.entity.Entity> lookup){
            var real=game.server().getPlayerList().getPlayer(id);
            if(real==null||real.isRemoved()||lookup.apply(id)!=real)return java.util.Set.of(id);
            var absent=links.resolve(lookup);if(!absent.isEmpty())return absent;
            return links.get(player)==real?java.util.Set.of():java.util.Set.of(id);
        }
        Runnable prepare(){
            var real=(ServerPlayer)links.get(player);var gun=real.getItemInHand(state.hand);
            // This source retains only the used hand identity: bind that canonical native slot directly.
            // Global stack-root discovery would add unrelated offline-player dependencies to this one view row.
            if(!ItemStack.matches(value,gun)||Scope.of(gun)!=state.scope)throw new IllegalStateException("Restore canonical native scope hand values before private view state");
            var view=movementViews.get(id);if(view==null)view=movement;
            if(view==null){var attribute=real.getAttribute(Attributes.MOVEMENT_SPEED);float base=attribute==null?.1f:(float)Math.clamp(attribute.getBaseValue(),0,1024);view=new MovementView(LbcRifleZoom.movement(real),base);}
            var currentView=view;
            return ()->{state.player=real;state.gun=gun;state.active=active;int now=game.server().getTickCount();state.lastPunch=now-punchAge;state.exitUntil=exitTicks==0?0:now+exitTicks;
                states.put(id,state);movementViews.put(id,currentView);abilityViews.put(id,new ClientboundPlayerAbilitiesPacket(real.getAbilities()));
                if(active){modelViews.put(id,state.scope);scopedMovement.put(id,LbcRifleZoom.movement(real));}else{modelViews.remove(id);scopedMovement.remove(id);}
                checkpointProjection=true;checkpointPublication.put(id,this);};
        }
    }

}
