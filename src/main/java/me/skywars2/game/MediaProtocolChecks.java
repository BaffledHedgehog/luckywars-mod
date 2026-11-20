package me.skywars2.game;

import io.netty.buffer.Unpooled;
import java.util.*;
import me.skywars2.media.MediaPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;

/** Actual shared wire codecs and Fabric channel negotiation; no network account or client required. */
final class MediaProtocolChecks {
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("media protocol: "+why);}
    static void negotiate(net.minecraft.server.level.ServerPlayer player){
        var addon=net.fabricmc.fabric.impl.networking.server.ServerNetworkingImpl.getAddon(player.connection);
        addon.handle(new MediaPackets.Hello(1,3));require(!TikTokClientSupport.enhanced(player),"hello without advertised receive channels stays vanilla");
        addon.handle(new net.fabricmc.fabric.impl.networking.RegistrationPayload(net.fabricmc.fabric.impl.networking.RegistrationPayload.REGISTER,List.of(MediaPackets.Frame.TYPE.id(),MediaPackets.Stop.TYPE.id())));
        addon.handle(new MediaPackets.Hello(999,3));require(!TikTokClientSupport.enhanced(player),"unknown protocol stays vanilla");
        addon.handle(new MediaPackets.Hello(1,1));require(!TikTokClientSupport.enhanced(player),"partial capabilities stay vanilla");
        addon.handle(new MediaPackets.Hello(1,3));require(TikTokClientSupport.enhanced(player),"actual Fabric registration plus handler enables exact connection");
    }
    static void negotiateLinks(ServerPlayer player){
        var addon=net.fabricmc.fabric.impl.networking.server.ServerNetworkingImpl.getAddon(player.connection);
        var hello=new MediaPackets.Hello(MediaPackets.VERSION,
                MediaPackets.FEATURES|MediaPackets.HTTPS_PLAYBACK|MediaPackets.DIRECT_MP4);
        addon.handle(new net.fabricmc.fabric.impl.networking.RegistrationPayload(
                net.fabricmc.fabric.impl.networking.RegistrationPayload.REGISTER,
                List.of(MediaPackets.Frame.TYPE.id(),MediaPackets.Stop.TYPE.id())));
        addon.handle(hello);
        require(TikTokClientSupport.enhanced(player)&&!TikTokClientSupport.httpsPlayback(player),
                "feature bits without the Link channel stay on JPEG/PCM");
        addon.handle(new net.fabricmc.fabric.impl.networking.RegistrationPayload(
                net.fabricmc.fabric.impl.networking.RegistrationPayload.REGISTER,List.of(MediaPackets.Link.TYPE.id())));
        addon.handle(hello);
        require(TikTokClientSupport.httpsPlayback(player)&&TikTokClientSupport.mp4(player),
                "advertised Link channel and exact connection negotiate MP4 transport");
    }
    static void fallback(ServerPlayer player,UUID session,boolean relayFailed){
        var bytes=new RegistryFriendlyByteBuf(Unpooled.buffer(),player.registryAccess());
        try{
            MediaPackets.Fallback.CODEC.encode(bytes,new MediaPackets.Fallback(session,relayFailed));
            var request=MediaPackets.Fallback.CODEC.decode(bytes);
            require(request.session().equals(session)&&request.relayFailed()==relayFailed&&!bytes.isReadable(),
                    "exact native fallback payload round-trip");
            new ServerboundCustomPayloadPacket(request).handle(player.connection);
        }finally{bytes.release();}
    }
    static MediaPackets.Link linkWire(ServerPlayer player,MediaPackets.Link original){
        var bytes=new RegistryFriendlyByteBuf(Unpooled.buffer(),player.registryAccess());
        try{
            MediaPackets.Link.CODEC.encode(bytes,original);
            var result=MediaPackets.Link.CODEC.decode(bytes);
            require(result.equals(original)&&!bytes.isReadable(),"exact native HTTPS Link payload round-trip");
            return result;
        }finally{bytes.release();}
    }
    static void codecs(net.minecraft.server.level.ServerPlayer player)throws Exception{
        var image=new java.awt.image.BufferedImage(720,1280,java.awt.image.BufferedImage.TYPE_INT_RGB);
        image.setRGB(30,40,0xffee22);var output=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"jpg",output);
        byte[] jpeg=output.toByteArray(),pcm=new byte[9600];new Random(129).nextBytes(pcm);
        require(MediaPackets.jpegMatches(jpeg,720,1280)&&!MediaPackets.jpegMatches(jpeg,480,854),"real JPEG SOF checked before native allocation");
        var frame=new MediaPackets.Frame(UUID.randomUUID(),65432,720,1280,48000,2,jpeg,pcm);
        var bytes=new RegistryFriendlyByteBuf(Unpooled.buffer(),player.registryAccess());
        try{
            MediaPackets.Frame.CODEC.encode(bytes,frame);var copy=MediaPackets.Frame.CODEC.decode(bytes);
            require(copy.session().equals(frame.session())&&copy.serial()==65432&&Arrays.equals(copy.jpeg(),jpeg)&&Arrays.equals(copy.pcm(),pcm)&&!bytes.isReadable(),"actual wire roundtrip including every stereo sample");
            bytes.clear();MediaPackets.Stop.CODEC.encode(bytes,new MediaPackets.Stop(frame.session()));require(MediaPackets.Stop.CODEC.decode(bytes).session().equals(frame.session()),"session-scoped Stop codec");
            bytes.clear();MediaPackets.Hello.CODEC.encode(bytes,new MediaPackets.Hello(1,3));require(MediaPackets.Hello.CODEC.decode(bytes).supported(),"Hello codec");
            bytes.clear();bytes.writeUUID(frame.session());bytes.writeVarLong(0);for(int value:new int[]{720,1280,48000,2})bytes.writeVarInt(value);bytes.writeVarInt(MediaPackets.MAX_JPEG+1);
            boolean rejected=false;try{MediaPackets.Frame.CODEC.decode(bytes);}catch(RuntimeException expected){rejected=true;}require(rejected,"oversized JPEG rejected by codec before allocation");
        }finally{bytes.release();}
        byte[] bomb=jpeg.clone();for(int i=2;i+8<bomb.length;i++)if((bomb[i]&255)==255&&(bomb[i+1]&255)==192){bomb[i+5]=(byte)255;bomb[i+6]=(byte)255;break;}
        require(!MediaPackets.jpegMatches(bomb,720,1280)&&!MediaPackets.jpegMatches(new byte[]{-1,-40,-1,-39},720,1280),"malformed and giant claimed dimensions rejected");
    }
}
