package me.skywars2.game;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** One outgoing boundary covers direct sounds and native world broadcasts without changing their audience. */
public final class SoundNormalization {
    private static final Identifier PIPE=Identifier.parse("minecraft:lbcsounds.metal_pipe");
    private record Profile(float cap,int channels){}
    private static final Map<Identifier,Profile> PROFILES=readProfiles();
    private static final ThreadLocal<Integer> EXEMPT=ThreadLocal.withInitial(()->0);
    private static final Map<UUID,Integer> CAMERAS=new ConcurrentHashMap<>();
    private static final Map<UUID,Vec3> VIEWS=new ConcurrentHashMap<>();
    private SoundNormalization(){}

    private static Map<Identifier,Profile> readProfiles(){
        try(var input=SoundNormalization.class.getResourceAsStream("/skywars2/sound_levels.json")){
            if(input==null)throw new IllegalStateException("Missing measured custom sound profiles");
            var data=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            var result=new HashMap<Identifier,Profile>();
            for(var entry:data.getAsJsonObject("events").entrySet()){
                var value=entry.getValue().getAsJsonObject();float cap=value.get("maxVolume").getAsFloat();int channels=value.get("channels").getAsInt();
                if(!Float.isFinite(cap)||cap<=0||cap>1||channels<1)throw new IllegalStateException("Invalid measured sound profile "+entry.getKey());
                result.put(Identifier.parse(entry.getKey()),new Profile(cap,channels));
            }
            return Map.copyOf(result);
        }catch(Exception error){throw new ExceptionInInitializerError(error);}
    }
    public static float volumeCap(Identifier event){var profile=PROFILES.get(event);return profile==null||event.equals(PIPE)?1:profile.cap;}
    /** The Psycho beam is the one context-specific loud exception; other uses of scream are normalized. */
    public static void sendExempt(ServerPlayer target,Packet<?> packet){
        int previous=EXEMPT.get();EXEMPT.set(previous+1);
        try{target.connection.send(packet);}finally{if(previous==0)EXEMPT.remove();else EXEMPT.set(previous);}
    }
    private static Vec3 view(ServerPlayer player){
        if(!player.level().getServer().isSameThread())return VIEWS.get(player.getUUID());
        // FPV keeps the native camera on self to preserve unrestricted mouse packets.
        // Its packet-only seat is not in the world and cannot be resolved via CAMERAS.
        var fpv=LbcFpvDrone.audioViewpoint(player);
        if(fpv!=null){VIEWS.put(player.getUUID(),fpv);return fpv;}
        var camera=player.getCamera();var id=CAMERAS.get(player.getUUID());
        if(id!=null){var actual=player.level().getEntity(id);if(actual!=null)camera=actual;}
        var at=camera.getEyePosition();VIEWS.put(player.getUUID(),at);return at;
    }
    /** Publish immutable poses for occasional network-thread sends; no world/entity lookup runs there. */
    public static void captureViews(MinecraftServer server){
        var online=new java.util.HashSet<UUID>();
        for(var player:server.getPlayerList().getPlayers()){online.add(player.getUUID());view(player);}
        VIEWS.keySet().retainAll(online);CAMERAS.keySet().retainAll(online);
    }
    private static Profile profile(Identifier event){return event.equals(PIPE)?null:PROFILES.get(event);}
    private static float gain(float original,Profile profile){return Math.min(Math.min(original,1),profile.cap);}
    /** Same bearing and original attenuation at launch, using the virtual-origin technique of /playsound minVolume.
     * Vanilla has no live gain/range update: moving listeners during a long mono clip remain an approximation. */
    private static Vec3 origin(ServerPlayer player,Vec3 source,float original,Profile profile){
        if(profile.channels>1||original<=1)return source;
        var listener=view(player);return listener==null?null:listener.add(source.subtract(listener).scale(1d/original));
    }
    public static Packet<?> project(ServerPlayer player,Packet<?> packet){
        if(packet instanceof ClientboundSetCameraPacket camera){
            if(player.level().getServer().isSameThread()){
                var entity=camera.getEntity(player.level());if(entity!=null)CAMERAS.put(player.getUUID(),entity.getId());
            }
            return packet;
        }
        if(EXEMPT.get()>0)return packet;
        if(packet instanceof ClientboundBundlePacket bundle){
            var children=new ArrayList<Packet<? super ClientGamePacketListener>>();boolean changed=false;
            for(var child:bundle.subPackets()){
                var next=project(player,child);changed|=next!=child;
                @SuppressWarnings("unchecked") var typed=(Packet<? super ClientGamePacketListener>)next;children.add(typed);
            }
            return changed?new ClientboundBundlePacket(children):packet;
        }
        if(packet instanceof ClientboundSoundPacket sound){
            var profile=profile(sound.getSound().value().location());float volume=sound.getVolume();
            if(profile==null||profile.cap>=1||!Float.isFinite(volume)||volume<=0||volume<=profile.cap)return packet;
            var at=origin(player,new Vec3(sound.getX(),sound.getY(),sound.getZ()),volume,profile);
            if(at==null)return packet;
            return new ClientboundSoundPacket(sound.getSound(),sound.getSource(),at.x,at.y,at.z,gain(volume,profile),sound.getPitch(),sound.getSeed());
        }
        if(packet instanceof ClientboundSoundEntityPacket sound){
            var profile=profile(sound.getSound().value().location());float volume=sound.getVolume();
            if(profile==null||profile.cap>=1||!Float.isFinite(volume)||volume<=0||volume<=profile.cap)return packet;
            if(!player.level().getServer().isSameThread())return packet;
            Entity source=player.level().getEntity(sound.getId());if(source==null)return packet;
            if(profile.channels>1||volume<=1)return new ClientboundSoundEntityPacket(sound.getSound(),sound.getSource(),source,gain(volume,profile),sound.getPitch(),sound.getSeed());
            var at=origin(player,source.position(),volume,profile);
            return at==null?packet:new ClientboundSoundPacket(sound.getSound(),sound.getSource(),at.x,at.y,at.z,gain(volume,profile),sound.getPitch(),sound.getSeed());
        }
        return packet;
    }
}
