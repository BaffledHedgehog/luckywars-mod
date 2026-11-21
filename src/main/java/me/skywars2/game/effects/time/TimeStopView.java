package me.skywars2.game.effects.time;

import io.netty.channel.ChannelFutureListener;
import me.skywars2.mixin.transport.MediaTransportAccess;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundPostEffectsPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Server-thread publication; packet filtering reads UUIDs only, including asynchronous media sends. */
public final class TimeStopView {
    public static final Identifier EFFECT=Identifier.parse("lbc:time_stop_freeze");
    private static final Identifier START=Identifier.parse("minecraft:lbcsounds.zawardo");
    private static final Set<UUID> FROZEN=ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Boolean> START_SOUND=new ThreadLocal<>();
    private TimeStopView(){}
    public static boolean frozen(ServerPlayer player){return FROZEN.contains(player.getUUID());}
    public static void publish(ServerPlayer player,boolean stopped){
        if(player.connection==null)return;
        stopped&=player.isAlive()&&!player.isRemoved();
        if(stopped){
            if(!FROZEN.add(player.getUUID()))return;
            player.addPostEffect(EFFECT);player.sendPostEffects();
        }else clear(player);
    }
    public static void clear(ServerPlayer player){
        boolean changed=FROZEN.remove(player.getUUID());boolean effect=player.removePostEffect(EFFECT);
        if((changed||effect)&&player.connection!=null)player.sendPostEffects();
    }
    static void activation(Runnable sound){var previous=START_SOUND.get();START_SOUND.set(true);try{sound.run();}finally{if(previous==null)START_SOUND.remove();else START_SOUND.set(previous);}}
    private static boolean start(Identifier id){return START.equals(id)&&Boolean.TRUE.equals(START_SOUND.get());}
    @SuppressWarnings("unchecked")
    public static Packet<?> project(ServerPlayer player,Packet<?> packet){
        if(packet instanceof ClientboundPostEffectsPacket effects&&effects.postEffects().contains(EFFECT)){
            var ordered=new ArrayList<>(effects.postEffects());ordered.remove(EFFECT);ordered.add(EFFECT);
            return new ClientboundPostEffectsPacket(List.copyOf(ordered));
        }
        if(!frozen(player))return packet;
        if(packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom&&custom.payload() instanceof me.skywars2.media.MediaPackets.Frame)return null;
        if(packet instanceof ClientboundSoundPacket sound)return start(sound.getSound().value().location())?packet:null;
        if(packet instanceof ClientboundSoundEntityPacket sound)return start(sound.getSound().value().location())?packet:null;
        if(packet instanceof ClientboundLevelEventPacket)return null; // Native block events also play sounds locally.
        if(packet instanceof ClientboundGameEventPacket event&&event.getEvent()==ClientboundGameEventPacket.PLAY_ARROW_HIT_SOUND)return null;
        if(packet instanceof ClientboundBundlePacket bundle){
            var kept=new ArrayList<Packet<? super ClientGamePacketListener>>();boolean changed=false;
            for(var child:bundle.subPackets()){var projected=project(player,child);changed|=projected!=child;if(projected!=null)kept.add((Packet<? super ClientGamePacketListener>)projected);}
            return !changed?packet:kept.isEmpty()?null:new ClientboundBundlePacket(kept);
        }
        return packet;
    }
    public static void completed(Connection connection,ChannelFutureListener listener){
        if(listener==null)return;var channel=((MediaTransportAccess.Transport)connection).skywars2$mediaChannel();
        if(channel==null)return;
        channel.eventLoop().execute(()->{try{listener.operationComplete(channel.newSucceededFuture());}catch(Exception error){me.skywars2.SkyWars2.LOGGER.error("Time Stop sound completion callback failed",error);}});
    }
}
