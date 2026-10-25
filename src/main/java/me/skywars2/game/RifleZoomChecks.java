package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.ArrayList;

/** Explicit native input and post-effect lifecycle regression. No client rendering claim. */
public final class RifleZoomChecks {
    private static final Identifier ZOOM=Identifier.parse("lbc:zoom");
    private static final Identifier ZOOM_OUT=Identifier.parse("lbc:zoom_out");
    private static final Identifier OTHER=Identifier.parse("minecraft:blur");
    private static ServerPlayer stopStandOwner;
    private static boolean stopAddedStand;
    private RifleZoomChecks() { }
    private static void require(boolean value,String message) {
        if(!value) throw new IllegalStateException("Rifle zoom: "+message);
    }
    private static ServerPlayer player(GameController game) {
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(player!=null,"requires TestAlpha"); return player;
    }
    private static boolean zoomed(ServerPlayer player) { return player.getPostEffects().contains(ZOOM); }
    private static final class Recording extends Connection {
        final ServerPlayer player; final List<Packet<?>> packets=new ArrayList<>();
        Recording(ServerPlayer player){super(PacketFlow.SERVERBOUND);this.player=player;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public PacketListener getPacketListener(){return player.connection;}
        @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);}
        @Override public void flushChannel(){ }
        ClientboundPlayerAbilitiesPacket latest(int start){for(int i=packets.size()-1;i>=start;i--)if(packets.get(i) instanceof ClientboundPlayerAbilitiesPacket packet)return packet;throw new IllegalStateException("Rifle zoom: missing native outbound abilities packet");}
    }
    private static final class Swap implements AutoCloseable {
        final ServerPlayer player;final ServerGamePacketListenerImpl previous;final Recording recording;
        Swap(GameController game,ServerPlayer player){this.player=player;previous=player.connection;recording=new Recording(player);player.connection=new ServerGamePacketListenerImpl(game.server(),recording,player,new CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());}
        public void close(){player.connection=previous;previous.resetPosition();}
    }
    private static void flags(ClientboundPlayerAbilitiesPacket actual,ClientboundPlayerAbilitiesPacket expected){
        require(actual.isInvulnerable()==expected.isInvulnerable()&&actual.isFlying()==expected.isFlying()
                &&actual.canFly()==expected.canFly()&&actual.canInstabuild()==expected.canInstabuild()
                &&actual.getFlyingSpeed()==expected.getFlyingSpeed(),"native abilities flags and flying speed preserved");
    }
    private static void restored(ServerPlayer player,Recording recording,int start){
        var expected=new ClientboundPlayerAbilitiesPacket(player.getAbilities());var actual=recording.latest(start);flags(actual,expected);
        require(actual.getWalkingSpeed()==expected.getWalkingSpeed(),"exit packet restores latest authoritative walking speed");
    }
    // Exact 26.3 AbstractClientPlayer arithmetic without item use; FOV Effects is a client preference.
    private static float clientFov(ServerPlayer player,ClientboundPlayerAbilitiesPacket packet,float effects){
        float factor=packet.isFlying()?1.1f:1f;
        if(packet.getWalkingSpeed()!=0)factor*=((float)player.getAttributeValue(Attributes.MOVEMENT_SPEED)/packet.getWalkingSpeed()+1)/2;
        return 1+effects*(factor-1);
    }
    private static void projected(ServerPlayer player,ClientboundPlayerAbilitiesPacket packet,ClientboundPlayerAbilitiesPacket nativePacket){
        flags(packet,nativePacket);
        require(Math.abs(clientFov(player,packet,1)-(packet.isFlying()?.275f:.25f))<.00001f,"native client FOV math gives fourfold scope with flying multiplier preserved");
        require(clientFov(player,packet,0)==1,"client FOV Effects zero disables native scope projection");
    }
    private static void projection(GameController game,ServerPlayer player,Recording recording){
        var zoom=game.items().rifleZoom();var abilities=player.getAbilities();var packed=abilities.pack();
        var attribute=player.getAttribute(Attributes.MOVEMENT_SPEED);double base=attribute.getBaseValue();
        var position=player.position();var velocity=player.getDeltaMovement();float movement=player.getSpeed();
        try{
            zoom.forget(player);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcShooters.SLR));
            abilities.invulnerable=false;abilities.flying=false;abilities.mayfly=false;abilities.instabuild=false;
            var original=new ClientboundPlayerAbilitiesPacket(abilities);var unscoped=abilities.pack();float walking=abilities.getWalkingSpeed();
            int start=recording.packets.size();zoom.punch(player,game.server().getTickCount()+100);
            projected(player,recording.latest(start),original);
            require(abilities.getWalkingSpeed()==walking&&abilities.pack().equals(unscoped),"scope leaves authoritative abilities untouched");
            require(attribute.getBaseValue()==base&&player.getSpeed()==movement&&player.position().equals(position)&&player.getDeltaMovement().equals(velocity),"scope leaves movement attribute/speed/position/velocity untouched");
            abilities.invulnerable=true;abilities.flying=true;abilities.mayfly=true;abilities.instabuild=true;abilities.setFlyingSpeed(.137f);abilities.setWalkingSpeed(.073f);
            var latestNative=new ClientboundPlayerAbilitiesPacket(abilities);var latestPacked=abilities.pack();start=recording.packets.size();player.onUpdateAbilities();
            projected(player,recording.latest(start),latestNative);
            attribute.setBaseValue(base==0?.13:base*1.6);float changedMovement=player.getSpeed();start=recording.packets.size();zoom.tick(game.server().getTickCount()+101);
            projected(player,recording.latest(start),latestNative);
            require(attribute.getBaseValue()==(base==0?.13:base*1.6)&&abilities.pack().equals(latestPacked)&&player.getSpeed()==changedMovement&&player.position().equals(position)&&player.getDeltaMovement().equals(velocity),"refresh changes only outbound FOV denominator");
            start=recording.packets.size();zoom.forget(player);var restored=recording.latest(start);flags(restored,latestNative);
            require(restored.getWalkingSpeed()==latestNative.getWalkingSpeed()&&!zoomed(player)&&!player.getPostEffects().contains(ZOOM_OUT),"hard cleanup restores latest native abilities and removes both overlays");
            require(zoom.projectAbilities(player.getUUID(),latestNative)==latestNative,"inactive projection preserves original packet identity");
        }finally{zoom.forget(player);abilities.apply(packed);attribute.setBaseValue(base);}
    }
    private static void attackAnimation(GameController game,ServerPlayer player,ItemStack gun,Recording recording) {
        require(gun.getAttackAnimation().equals(LbcRifleZoom.SCOPE_ATTACK_ANIMATION),
                "rifle prototype and loot provide vanilla NONE attack animation");
        var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)recording;
        net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->{
            var projected=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(gun,
                    net.minecraft.world.item.TooltipFlag.NORMAL,provider.getPacketContext(),player.registryAccess());
            require(projected.getAttackAnimation().equals(LbcRifleZoom.SCOPE_ATTACK_ANIMATION),
                    "actual vanilla Polymer item retains NONE before client predicts LMB swing");
        });
        player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
        require(player.getCurrentSwing()!=null && player.getCurrentSwing().animation()
                .equals(LbcRifleZoom.SCOPE_ATTACK_ANIMATION),"native punch uses NONE and still reaches scope toggle");
        // Fixture players need not be enrolled in ChunkMap.entityMap in the same command tick.
        // Send the native handler's actual swing descriptor through the real connection explicitly.
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSwingAnimationPacket(
                player,InteractionHand.MAIN_HAND,player.getCurrentSwing().animation()));
        var packet=recording.packets.stream().filter(net.minecraft.network.protocol.game.ClientboundSwingAnimationPacket.class::isInstance)
                .map(net.minecraft.network.protocol.game.ClientboundSwingAnimationPacket.class::cast)
                .reduce((a,b)->b).orElseThrow();
        var bytes=io.netty.buffer.Unpooled.buffer();
        try {
            net.minecraft.network.protocol.game.ClientboundSwingAnimationPacket.STREAM_CODEC.encode(bytes,packet);
            var decoded=net.minecraft.network.protocol.game.ClientboundSwingAnimationPacket.STREAM_CODEC.decode(bytes);
            require(decoded.entityId()==player.getId() && decoded.hand()==InteractionHand.MAIN_HAND
                    && decoded.animation().equals(LbcRifleZoom.SCOPE_ATTACK_ANIMATION),
                    "native handler swing descriptor retains NONE through actual connection and packet codec");
        } finally {bytes.release();}
        var victim=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,player.level());
        victim.setNoAi(true);victim.setNoGravity(true);victim.setPos(player.position().add(0,0,1.5));
        require(player.level().addFreshEntity(victim),"native melee regression victim admitted");
        try {
            float health=victim.getHealth();
            player.connection.handleAttack(new net.minecraft.network.protocol.game.ServerboundAttackPacket(victim.getId()));
            require(victim.getHealth()<health,"NONE scope animation preserves genuine native entity attack damage");
        } finally {victim.discard();}
        game.items().rifleZoom().forget(player);
    }
    public static void run(GameController game) {
        var player=player(game); var zoom=game.items().rifleZoom();
        require(game.playing() && game.allowDamage(player),"requires active match participant");
        var saved=new GameChecks.WandThirdPlayerState(player,game.items().mana());
        var effects=List.copyOf(player.getPostEffects());
        boolean hadStand=player.entityTags().remove("have_stand");
        try(var swap=new Swap(game,player)) {
            zoom.forget(player); player.removePostEffect(ZOOM); player.addPostEffect(OTHER);
            player.getInventory().clearContent();
            projection(game,player,swap.recording);
            for(String table:List.of("lbc:ported/slr","lbc:ported/magnum")) {
                var gun=game.lbc().rollTable(player.level(),player.position(),player,table).getFirst();
                player.setItemInHand(InteractionHand.MAIN_HAND,gun);
                attackAnimation(game,player,gun,swap.recording);
                player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
                require(zoomed(player),"native punch enables "+table);
                player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
                require(zoomed(player),"same-tick duplicate cannot flip zoom twice");
                int now=game.server().getTickCount();
                for(int tick=1;tick<=20;tick++) zoom.punch(player,now+tick);
                require(zoomed(player),"held mining packet stream stays enabled");
                int exitPacket=swap.recording.packets.size();zoom.punch(player,now+23);restored(player,swap.recording,exitPacket);
                require(!zoomed(player) && player.getPostEffects().contains(ZOOM_OUT) && player.getPostEffects().contains(OTHER),
                        "next separate click starts zoom-out and preserves other effects");
                zoom.tick(now+28);require(player.getPostEffects().contains(ZOOM_OUT),"zoom-out remains for first five ticks");
                zoom.tick(now+29);require(!player.getPostEffects().contains(ZOOM_OUT),"zoom-out expires exactly after six ticks");
                zoom.punch(player,now+32);
                require(zoomed(player),"a later click enables again");
                zoom.punch(player,now+35);require(player.getPostEffects().contains(ZOOM_OUT),"second exit starts animation");
                zoom.punch(player,now+38);require(zoomed(player)&&!player.getPostEffects().contains(ZOOM_OUT),"reenter cancels pending exit animation");
                player.setItemInHand(InteractionHand.MAIN_HAND,gun.copy()); zoom.tick(now+39);
                require(!zoomed(player)&&player.getPostEffects().contains(ZOOM_OUT),"replacing even an identical gun starts zoom-out");
                zoom.tick(now+45);require(!player.getPostEffects().contains(ZOOM_OUT),"unhold zoom-out expires after six ticks");
                // Existing consumable/model components are not needed to enable a scope.
                require(gun.get(DataComponents.ITEM_MODEL)!=null,"native loot keeps its original model");
            }
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
            zoom.punch(player);
            require(!zoomed(player),"unrelated main hand cannot enable zoom");
            player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(LbcMagnum.ITEM));
            player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
            require(zoomed(player),"rifle in offhand supports the same left-click toggle");
            player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY); zoom.tick();
            require(!zoomed(player)&&player.getPostEffects().contains(ZOOM_OUT),"unholding offhand rifle starts zoom-out");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcShooters.SLR));
            player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
            require(zoomed(player),"native give prototype is supported");
            zoom.forget(player);
            require(!zoomed(player) && !player.getPostEffects().contains(ZOOM_OUT) && player.getPostEffects().contains(OTHER),"disconnect cleanup clears both rifle effects and retains other effects");
            game.items().timeStop().begin(player.level(),40);
            require(game.items().actionsBlocked(player),"native time stop really blocks the fixture player");
            player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
            require(!zoomed(player),"blocked input cannot enable zoom");
            game.items().timeStop().end(player.level());
            player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
            require(zoomed(player),"unblocked input works again");
            game.items().timeStop().begin(player.level(),40);int blockedPacket=swap.recording.packets.size();zoom.tick();restored(player,swap.recording,blockedPacket);
            require(!zoomed(player),"blocking actions also clears an existing scope");
            game.items().timeStop().end(player.level());
            zoom.punch(player,game.server().getTickCount()+3);
            require(zoomed(player),"separate click after unblock enters scope before death");
            float health=player.getHealth();
            player.setHealth(0);int deathPacket=swap.recording.packets.size();zoom.tick();restored(player,swap.recording,deathPacket);
            require(!zoomed(player)&&!player.getPostEffects().contains(ZOOM_OUT),"death clears both rifle effects");
            player.setHealth(health);
            SkyWars2.LOGGER.info("[check] PASS rifle-zoom");
        } finally {
            zoom.forget(player); game.items().timeStop().end(player.level()); saved.restore(game);
            if(hadStand) player.entityTags().add("have_stand");
            player.clearPostEffects(); for(var effect:effects) player.addPostEffect(effect);
        }
    }
    public static void stopProbe(GameController game) {
        var player=player(game); game.items().rifleZoom().forget(player);
        require(stopStandOwner==null,"previous stop probe restored its temporary Stand tag");
        boolean stopped=game.items().timeStop().stopped(player.level());
        stopStandOwner=player;stopAddedStand=player.entityTags().add("have_stand");
        try {
            require(!game.items().actionsBlocked(player),"native Stand immunity permits scope input while pending probes remain frozen");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcShooters.SLR));
            player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
            require(zoomed(player),"stop probe enters with active scope");
            require(game.items().timeStop().stopped(player.level())==stopped,"scope probe preserves the existing dimension stop");
            SkyWars2.LOGGER.info("[check] PASS rifle-zoom-stop-probe");
        } catch(RuntimeException|Error error) {
            restoreStopStand();game.items().rifleZoom().forget(player);throw error;
        }
    }
    private static void restoreStopStand() {
        if(stopStandOwner!=null&&stopAddedStand)stopStandOwner.entityTags().remove("have_stand");
        stopStandOwner=null;stopAddedStand=false;
    }
    public static void cleanup(GameController game) {
        try {
            require(game.inLobby() && !zoomed(player(game))&&!player(game).getPostEffects().contains(ZOOM_OUT),"actual match stop removes both rifle effects");
            SkyWars2.LOGGER.info("[check] PASS rifle-zoom-cleanup");
        } finally {restoreStopStand();}
    }
}
