package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Native outbound packet checks; no claim about client options or rendered pixels. */
public final class SpeedFovChecks {
    private SpeedFovChecks(){}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException("Speed FOV: "+message);}
    private static double speedFactor(double speed,float walking){return walking==0?1:(speed/walking+1)/2;}
    private static void close(double actual,double expected,String message){
        require(Double.isFinite(actual)&&Math.abs(actual-expected)<Math.max(1e-5,Math.abs(expected)*1e-5),message+": "+actual+" expected "+expected);
    }
    private static final class Recording extends Connection {
        final ServerPlayer player;final List<Packet<?>> packets=new ArrayList<>();
        Recording(ServerPlayer player){super(PacketFlow.SERVERBOUND);this.player=player;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public PacketListener getPacketListener(){return player.connection;}
        @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);}
        @Override public void flushChannel(){}
        ClientboundPlayerAbilitiesPacket latest(int start){
            for(int i=packets.size()-1;i>=start;i--)if(packets.get(i) instanceof ClientboundPlayerAbilitiesPacket p)return p;
            throw new IllegalStateException("Speed FOV: missing native abilities packet");
        }
    }
    private static void flags(ClientboundPlayerAbilitiesPacket packet,ClientboundPlayerAbilitiesPacket nativePacket){
        require(packet.isInvulnerable()==nativePacket.isInvulnerable()&&packet.isFlying()==nativePacket.isFlying()
                &&packet.canFly()==nativePacket.canFly()&&packet.canInstabuild()==nativePacket.canInstabuild()
                &&packet.getFlyingSpeed()==nativePacket.getFlyingSpeed(),"all flags and flying speed unchanged");
    }
    private static void math(){
        for(var policy:LbcRifleZoom.SpeedFovPolicy.values())for(float speed:new float[]{-1000,-.1f,0,.1f,.2f,1000}){
            float walk=LbcRifleZoom.projectedWalking(speed,.1f,false,policy);
            close(speedFactor(speed,walk),speed==0?1:LbcRifleZoom.speedFov(speed,.1,policy),"signed/zero/normal/extreme pure projection "+policy);
            require(Float.isFinite(walk),"finite denominator");
            float scoped=LbcRifleZoom.projectedWalking(speed,.1f,true,policy);
            close(speedFactor(speed,scoped),speed==0?1:.25,"scope overrides both speed policies");
        }
        require(LbcRifleZoom.projectedWalking(Float.NaN,.1f,false,LbcRifleZoom.SPEED_FOV_POLICY)==.1f,"nonfinite movement preserves native denominator");
    }
    public static void run(GameController game){
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(player!=null&&game.playing()&&game.allowDamage(player),"requires active TestAlpha participant");
        math();var zoom=game.items().rifleZoom();var saved=new GameChecks.WandThirdPlayerState(player,game.items().mana());
        var abilities=player.getAbilities();var packed=abilities.pack();var attr=player.getAttribute(Attributes.MOVEMENT_SPEED);double base=attr.getBaseValue();
        var previous=player.connection;var recording=new Recording(player);
        player.connection=new ServerGamePacketListenerImpl(game.server(),recording,player,new CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        try{
            zoom.forget(player);abilities.flying=false;abilities.mayfly=true;abilities.invulnerable=true;abilities.instabuild=false;
            abilities.setWalkingSpeed(.1f);abilities.setFlyingSpeed(.137f);
            var nativePacket=new ClientboundPlayerAbilitiesPacket(abilities);var snapshot=abilities.pack();
            var position=player.position();var velocity=player.getDeltaMovement();
            for(double input:new double[]{0,.1,.2,1000,1024,1_000_000_000}){
                attr.setBaseValue(input);double speed=player.getAttributeValue(Attributes.MOVEMENT_SPEED);float movement=player.getSpeed();
                int start=recording.packets.size();zoom.tick(game.server().getTickCount());player.onUpdateAbilities();
                var projected=recording.latest(start);flags(projected,nativePacket);
                float clientSpeed=(float)Math.clamp(speed,0,1024);
                double target=clientSpeed==0?1:LbcRifleZoom.speedFov(clientSpeed,.1f,LbcRifleZoom.SPEED_FOV_POLICY);
                close(speedFactor(clientSpeed,projected.getWalkingSpeed()),target,"actual native baseline packet including client attribute limit");
                // The native client applies flight1.1 and a fully drawn bow0.85 after the speed term.
                close(speedFactor(clientSpeed,projected.getWalkingSpeed())*1.1*.85,target*1.1*.85,"flight/bow multipliers remain independent");
                require(attr.getBaseValue()==input&&player.getSpeed()==movement&&abilities.pack().equals(snapshot)
                        &&player.position().equals(position)&&player.getDeltaMovement().equals(velocity),"projection leaves authoritative movement/abilities/position/velocity unchanged");
            }
            attr.setBaseValue(.2);zoom.tick();player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcShooters.SLR));
            int start=recording.packets.size();zoom.punch(player,game.server().getTickCount()+100);
            close(speedFactor((float)player.getAttributeValue(Attributes.MOVEMENT_SPEED),recording.latest(start).getWalkingSpeed()),.25,"scope has full strength at client FOV Effects100%");
            start=recording.packets.size();zoom.forget(player);var outside=recording.latest(start);
            close(speedFactor((float)player.getAttributeValue(Attributes.MOVEMENT_SPEED),outside.getWalkingSpeed()),LbcRifleZoom.speedFov((float)player.getAttributeValue(Attributes.MOVEMENT_SPEED),.1f,LbcRifleZoom.SPEED_FOV_POLICY),"scope exit restores attenuated baseline");
            // Off-thread projection must need only the immutable tick-published speed snapshot.
            var concurrent=java.util.concurrent.CompletableFuture.supplyAsync(()->zoom.projectAbilities(player.getUUID(),nativePacket)).join();
            require(concurrent.getWalkingSpeed()==outside.getWalkingSpeed(),"off-thread ability resend uses published immutable speed");
            SkyWars2.LOGGER.info("[check] PASS speed-fov");
        }finally{
            zoom.forget(player);abilities.apply(packed);attr.setBaseValue(base);saved.restore(game);
            player.connection=previous;previous.resetPosition();zoom.tick();player.onUpdateAbilities();
        }
    }
}
