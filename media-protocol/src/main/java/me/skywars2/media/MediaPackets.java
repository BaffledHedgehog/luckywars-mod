package me.skywars2.media;

import java.util.Objects;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Small versioned play protocol shared by the optional client and the dedicated server. */
public final class MediaPackets {
    public static final int VERSION=1, JPEG=1, PCM=2, FEATURES=JPEG|PCM, FPS=20;
    public static final int HTTPS_PLAYBACK = 4, DIRECT_MP4 = 8;
    public static final int MAX_LINK_LENGTH = 4096;
    public static final int MAX_JPEG=131072, MAX_PCM=16384, MAX_WIDTH=720, MAX_HEIGHT=1280;
    private static boolean registered;
    private MediaPackets(){}
    public static synchronized void register(){
        if(registered)return;
        PayloadTypeRegistry.serverboundPlay().register(Hello.TYPE,Hello.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Fallback.TYPE,Fallback.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Frame.TYPE,Frame.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Stop.TYPE,Stop.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Link.TYPE,Link.CODEC);
        registered=true;
    }
    public record Hello(int protocol,int features) implements CustomPacketPayload {
        public static final Type<Hello> TYPE=new Type<>(Identifier.parse("skywars2:media/hello_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Hello> CODEC=new StreamCodec<>(){
            public Hello decode(RegistryFriendlyByteBuf b){return new Hello(b.readVarInt(),b.readVarInt());}
            public void encode(RegistryFriendlyByteBuf b,Hello p){b.writeVarInt(p.protocol);b.writeVarInt(p.features);}
        };
        @Override public Type<Hello> type(){return TYPE;}
        public boolean supported(){return protocol==VERSION&&(features&FEATURES)==FEATURES;}
    }
    public record Frame(UUID session,long serial,int width,int height,int rate,int channels,byte[] jpeg,byte[] pcm) implements CustomPacketPayload {
        public static final Type<Frame> TYPE=new Type<>(Identifier.parse("skywars2:media/frame_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Frame> CODEC=new StreamCodec<>(){
            public Frame decode(RegistryFriendlyByteBuf b){return new Frame(b.readUUID(),b.readVarLong(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readByteArray(MAX_JPEG),b.readByteArray(MAX_PCM));}
            public void encode(RegistryFriendlyByteBuf b,Frame p){b.writeUUID(p.session);b.writeVarLong(p.serial);b.writeVarInt(p.width);b.writeVarInt(p.height);b.writeVarInt(p.rate);b.writeVarInt(p.channels);b.writeByteArray(p.jpeg);b.writeByteArray(p.pcm);}
        };
        public Frame {
            Objects.requireNonNull(session);Objects.requireNonNull(jpeg);Objects.requireNonNull(pcm);
            if(serial<0||width<1||height<1||width>MAX_WIDTH||height>MAX_HEIGHT
                    ||!((rate==24000&&channels==1)||(rate==48000&&channels==2))
                    ||jpeg.length<4||jpeg.length>MAX_JPEG||pcm.length!=rate/FPS*channels*2||pcm.length>MAX_PCM)
                throw new IllegalArgumentException("media_frame_bounds");
            jpeg=jpeg.clone();pcm=pcm.clone();
        }
        @Override public Type<Frame> type(){return TYPE;}
    }
    public record Stop(UUID session) implements CustomPacketPayload {
        public static final Type<Stop> TYPE=new Type<>(Identifier.parse("skywars2:media/stop_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Stop> CODEC=new StreamCodec<>(){
            public Stop decode(RegistryFriendlyByteBuf b){return new Stop(b.readUUID());}
            public void encode(RegistryFriendlyByteBuf b,Stop p){b.writeUUID(p.session);}
        };
        public Stop{Objects.requireNonNull(session);}
        @Override public Type<Stop> type(){return TYPE;}
    }

    /** Start with the CDN URL or deliver a relay URL for the same session. */
    public record Link(UUID session, String cdn, String relay, boolean relayMp4) implements CustomPacketPayload {
        public static final Type<Link> TYPE = new Type<>(Identifier.parse("skywars2:media/link_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Link> CODEC = new StreamCodec<>() {
            @Override public Link decode(RegistryFriendlyByteBuf buffer) {
                return new Link(buffer.readUUID(), buffer.readUtf(MAX_LINK_LENGTH), buffer.readUtf(MAX_LINK_LENGTH), buffer.readBoolean());
            }

            @Override public void encode(RegistryFriendlyByteBuf buffer, Link payload) {
                buffer.writeUUID(payload.session);
                buffer.writeUtf(payload.cdn, MAX_LINK_LENGTH);
                buffer.writeUtf(payload.relay, MAX_LINK_LENGTH);
                buffer.writeBoolean(payload.relayMp4);
            }
        };

        public Link {
            Objects.requireNonNull(session);
            if (!validLink(cdn) || !validLink(relay) || (cdn.isEmpty() && relay.isEmpty()) || relayMp4 && relay.isEmpty()) {
                throw new IllegalArgumentException("media_link_bounds");
            }
        }

        private static boolean validLink(String value) {
            return value != null && value.length() <= MAX_LINK_LENGTH
                    && value.chars().allMatch(character -> character > 32 && character < 127);
        }

        @Override public Type<Link> type() { return TYPE; }
    }

    public record Fallback(UUID session, boolean relayFailed) implements CustomPacketPayload {
        public static final Type<Fallback> TYPE = new Type<>(Identifier.parse("skywars2:media/fallback_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Fallback> CODEC = new StreamCodec<>() {
            @Override public Fallback decode(RegistryFriendlyByteBuf buffer) {
                return new Fallback(buffer.readUUID(), buffer.readBoolean());
            }
            @Override public void encode(RegistryFriendlyByteBuf buffer, Fallback payload) {
                buffer.writeUUID(payload.session);
                buffer.writeBoolean(payload.relayFailed);
            }
        };
        @Override public Type<Fallback> type() { return TYPE; }
    }
    /** Validate JPEG dimensions before invoking a native decoder, including against allocation bombs. */
    public static boolean jpegMatches(byte[] data,int width,int height){
        if(data.length<4||data.length>MAX_JPEG||(data[0]&255)!=255||(data[1]&255)!=216)return false;
        int at=2;
        while(at+1<data.length){
            if((data[at++]&255)!=255)return false;
            while(at<data.length&&(data[at]&255)==255)at++;
            if(at>=data.length)return false;int marker=data[at++]&255;
            if(marker==217||marker==218)return false;
            if(marker==1||marker>=208&&marker<=215)continue;
            if(at+2>data.length)return false;
            int length=(data[at]&255)<<8|(data[at+1]&255);
            if(length<2||at+length>data.length)return false;
            if(marker>=192&&marker<=207&&marker!=196&&marker!=200&&marker!=204){
                if(length<8)return false;int h=(data[at+3]&255)<<8|(data[at+4]&255),w=(data[at+5]&255)<<8|(data[at+6]&255);
                return w==width&&h==height&&w>0&&h>0&&w<=MAX_WIDTH&&h<=MAX_HEIGHT;
            }
            at+=length;
        }
        return false;
    }
}
