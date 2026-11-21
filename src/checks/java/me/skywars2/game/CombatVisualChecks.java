package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.Items;
import java.util.*;

/** Native outbound packets and codec round trip; called inside the fully saved Fartus fixture. */
final class CombatVisualChecks {
    private CombatVisualChecks(){}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException("Combat visuals: "+message);}
    private static final class Recording extends Connection {
        final ServerPlayer player;final List<Packet<?>> sent=new ArrayList<>();
        Recording(ServerPlayer player){super(PacketFlow.SERVERBOUND);this.player=player;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public PacketListener getPacketListener(){return player.connection;}
        @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){sent.add(packet);}
        @Override public void flushChannel(){}
    }
    private static final class Swap implements AutoCloseable {
        final ServerPlayer player;final ServerGamePacketListenerImpl previous;final Recording recording;
        Swap(GameController game,ServerPlayer player){this.player=player;previous=player.connection;recording=new Recording(player);player.connection=new ServerGamePacketListenerImpl(game.server(),recording,player,new CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));}
        public void close(){player.connection=previous;previous.resetPosition();}
    }
    private static List<ClientboundLevelParticlesPacket> items(Recording recording){return recording.sent.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).filter(p->p.particle().getType()==ParticleTypes.ITEM).toList();}
    static void movementPacket(ServerPlayer player,double expected){
        var attribute=player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        var packet=new net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket(player.getId(),List.of(attribute));
        var raw=io.netty.buffer.Unpooled.buffer();
        try{
            var buffer=new RegistryFriendlyByteBuf(raw,player.registryAccess());
            net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket.STREAM_CODEC.encode(buffer,packet);
            var decoded=net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket.STREAM_CODEC.decode(buffer);
            var snapshot=decoded.getValues().getFirst();
            var received=new net.minecraft.world.entity.ai.attributes.AttributeInstance(snapshot.attribute(),ignored->{});
            received.setBaseValue(snapshot.base());snapshot.modifiers().forEach(received::addTransientModifier);
            require(raw.readableBytes()==0&&decoded.getEntityId()==player.getId()&&snapshot.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)&&Math.abs(received.getValue()-expected)<1e-9,"native movement attribute packet reconstructs safe expected client speed");
        }finally{raw.release();}
    }
    static void piss(GameController game,ServerPlayer owner,ServerPlayer victim){
        for(var player:List.of(owner,victim))for(var name:List.of("piss","scared","lookback","twitching"))require(game.items().psycho().effectValue(player,name)==0,"fixture owns clean Psycho state");
        var point=victim.position();
        try(var a=new Swap(game,owner);var b=new Swap(game,victim)){
            owner.teleportTo(owner.level(),point.x+8,point.y,point.z,Set.of(),0,0,true);
            for(float[] rotation:new float[][]{{0,0},{90,-30},{-75,60}}){
                victim.teleportTo(victim.level(),point.x,point.y,point.z,Set.of(),rotation[0],rotation[1],true);
                var expected=victim.getEyePosition().add(victim.getLookAngle().scale(.3));
                a.recording.sent.clear();b.recording.sent.clear();
                require(game.items().psycho().yellow(victim,owner,1),"native one-tick piss admitted");game.items().psycho().tick();
                var self=items(b.recording);var other=items(a.recording);
                require(self.size()==1&&other.size()==1,"one particle batch per audience");
                for(var packet:List.of(self.getFirst(),other.getFirst())){
                    var raw=io.netty.buffer.Unpooled.buffer();
                    try{
                        var buffer=new RegistryFriendlyByteBuf(raw,victim.registryAccess());
                        ClientboundLevelParticlesPacket.STREAM_CODEC.encode(buffer,packet);
                        var decoded=ClientboundLevelParticlesPacket.STREAM_CODEC.decode(buffer);
                        require(raw.readableBytes()==0&&decoded.x()==expected.x&&decoded.y()==expected.y&&decoded.z()==expected.z,"encoded particle source follows eye position and full yaw/pitch plus.3");
                        require(decoded.overrideLimiter()&&!decoded.alwaysShow()&&decoded.xMaxSpeed()==0&&decoded.yMaxSpeed()==0&&decoded.zMaxSpeed()==0&&decoded.randomizationType()==ClientboundLevelParticlesPacket.RandomizationType.DEFAULT,"source force and zero-speed flags survive native encoding");
                        var item=((ItemParticleOption)decoded.particle()).getItem().create();
                        if(packet==self.getFirst())require(decoded.count()==200&&decoded.xDist()==.3f&&decoded.yDist()==.2f&&decoded.zDist()==.3f&&item.is(Items.STICK)&&Identifier.parse("lbc:piss").equals(item.get(DataComponents.ITEM_MODEL)),"private200 source model particles and exact spread");
                        else require(decoded.count()==3&&decoded.xDist()==.2f&&decoded.yDist()==.2f&&decoded.zDist()==.2f&&item.is(Items.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.YELLOW)),"bystanders receive three yellow-glass particles without private model");
                    }finally{raw.release();}
                }
                require(game.items().psycho().effectValue(victim,"piss")==0,"one native active counter tick consumed");
            }
        }finally{game.items().psycho().clearEffects(victim);}
    }
}
