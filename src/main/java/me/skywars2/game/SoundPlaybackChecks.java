package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import me.skywars2.SkyWars2;
import net.minecraft.core.Holder;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Actual outgoing listener path must preserve authored gain, source, seed and native packet type. */
public final class SoundPlaybackChecks {
    private SoundPlaybackChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Sound playback: "+why);}
    private static final class Wire extends Connection {
        final ServerPlayer player;final List<Packet<?>> sent=new ArrayList<>();
        Wire(ServerPlayer player){super(PacketFlow.SERVERBOUND);this.player=player;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public PacketListener getPacketListener(){return player.connection;}
        @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){sent.add(packet);}
        @Override public void flushChannel(){}
    }
    private static void flatten(Packet<?> packet,List<Packet<?>> out){if(packet instanceof ClientboundBundlePacket bundle)for(var child:bundle.subPackets())flatten(child,out);else out.add(packet);}
    public static void run(GameController game,String label){
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(player!=null,"native recipient");
        var previous=player.connection;var wire=new Wire(player);
        try{
            player.connection=new ServerGamePacketListenerImpl(game.server(),wire,player,new CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());wire.sent.clear();
            for(var event:List.of("minecraft:lbcsounds.shitting_2","minecraft:lbcsounds.metal_pipe","minecraft:lbcsounds.scream")){
                var sound=Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse(event)));
                for(float gain:new float[]{.04f,1,4,100}){
                    var at=player.position().add(31,7,-19);
                    var source=new ClientboundSoundPacket(sound,SoundSource.MASTER,at.x,at.y,at.z,gain,.75f,1719L);
                    player.connection.send(source);
                    var heard=wire.sent.getLast();require(heard instanceof ClientboundSoundPacket,"position sound stays native type");
                    var actual=(ClientboundSoundPacket)heard;
                    require(actual.getVolume()==gain&&actual.getPitch()==.75f&&actual.getSeed()==1719L&&actual.getSource()==SoundSource.MASTER
                            &&actual.getX()==source.getX()&&actual.getY()==source.getY()&&actual.getZ()==source.getZ(),"authored sound is neither attenuated nor relocated "+event+" gain="+gain);
                    var attached=new ClientboundSoundEntityPacket(sound,SoundSource.PLAYERS,player,gain,1.25f,1720L);player.connection.send(attached);
                    require(wire.sent.getLast() instanceof ClientboundSoundEntityPacket entity&&entity.getVolume()==gain&&entity.getPitch()==1.25f&&entity.getSeed()==1720L&&entity.getId()==player.getId(),"entity sound retains attachment and original gain");
                    wire.sent.clear();player.connection.send(new ClientboundBundlePacket(List.of(source,attached)));var all=new ArrayList<Packet<?>>();wire.sent.forEach(packet->flatten(packet,all));
                    require(all.stream().anyMatch(packet->packet instanceof ClientboundSoundPacket positional&&positional.getVolume()==gain&&positional.getSeed()==1719L)
                            &&all.stream().anyMatch(packet->packet instanceof ClientboundSoundEntityPacket entity&&entity.getVolume()==gain&&entity.getSeed()==1720L),"bundled authored sounds unchanged");
                }
            }
        }finally{player.connection=previous;previous.resetPosition();}
        SkyWars2.LOGGER.info("[check] PASS {} (native positional/entity/bundled sounds retain authored gain, pitch, origin and seed; no global normalization)",label);
    }
}
