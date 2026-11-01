package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Native outbound packet checks. Spatial checks concern the initial listener pose only;
 * they do not assert live reprojection, audio seeking, or physical client output. */
public final class SoundNormalizationChecks {
    private static final Identifier MONO=Identifier.parse("minecraft:lbcsounds.shitting_2");
    private static final Identifier STEREO=Identifier.parse("minecraft:lbcsounds.helicopter");
    private static final Identifier SCREAM=Identifier.parse("minecraft:lbcsounds.scream");
    private static final Identifier PIPE=Identifier.parse("minecraft:lbcsounds.metal_pipe");
    private SoundNormalizationChecks() { }
    private static void require(boolean ok,String why) {
        if(!ok)throw new IllegalStateException("Sound normalization: "+why);
    }
    private static void near(double actual,double expected,double tolerance,String why) {
        require(Double.isFinite(actual)&&Math.abs(actual-expected)<=tolerance,
                why+": "+actual+" expected "+expected);
    }
    private static Holder<SoundEvent> custom(Identifier id) {
        return Holder.direct(SoundEvent.createVariableRangeEvent(id));
    }
    private static ClientboundSoundPacket packet(Holder<SoundEvent> sound,Vec3 at,float volume) {
        return new ClientboundSoundPacket(sound,SoundSource.MASTER,at.x,at.y,at.z,volume,.75f,735164L);
    }
    private static Vec3 position(ClientboundSoundPacket sound) {
        return new Vec3(sound.getX(),sound.getY(),sound.getZ());
    }
    private static void flatten(Packet<?> packet,List<Packet<?>> result) {
        if(packet instanceof ClientboundBundlePacket bundle)for(var child:bundle.subPackets())flatten(child,result);
        else result.add(packet);
    }
    private static final class Swap implements AutoCloseable {
        final ServerPlayer player;
        final ServerGamePacketListenerImpl previous;
        final ElementalGrimoireChecks.Recording recording;
        final Vec3 before;
        Swap(GameController game,ServerPlayer player) {
            this.player=player;previous=player.connection;before=player.position();
            recording=new ElementalGrimoireChecks.Recording(player);
            player.connection=new ServerGamePacketListenerImpl(game.server(),recording,player,
                    new CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            clear();
        }
        void clear(){recording.packets.clear();}
        List<Packet<?>> packets() {
            var result=new ArrayList<Packet<?>>();
            for(var packet:recording.packets)flatten(packet,result);
            return result;
        }
        Packet<?> send(Packet<?> packet) {
            clear();player.connection.send(packet);
            var result=packets();require(result.size()==1,"one native outbound payload");
            return result.getFirst();
        }
        ClientboundSoundPacket coordinate(ClientboundSoundPacket packet) {
            var result=send(packet);require(result instanceof ClientboundSoundPacket,"coordinate payload retained");
            return (ClientboundSoundPacket)result;
        }
        @Override public void close() {
            player.setPos(before.x,before.y,before.z);
            player.connection=previous;previous.resetPosition();
        }
    }
    private static void identity(ClientboundSoundPacket actual,ClientboundSoundPacket original,String why) {
        require(actual.getSound()==original.getSound()&&actual.getSource()==original.getSource()
                &&actual.getPitch()==original.getPitch()&&actual.getSeed()==original.getSeed(),why+" preserves holder/category/pitch/seed");
    }
    private static void coordinates(ClientboundSoundPacket actual,ClientboundSoundPacket original,String why) {
        require(position(actual).equals(position(original)),why+" preserves exact packet coordinates");
    }
    private static void projected(ClientboundSoundPacket actual,ClientboundSoundPacket original,Vec3 eye,float cap,String why) {
        identity(actual,original,why);near(actual.getVolume(),cap,1e-7,why+" capped gain");
        var oldOffset=position(original).subtract(eye);
        var newOffset=position(actual).subtract(eye);
        var expected=eye.add(oldOffset.scale(1/original.getVolume()));
        require(position(actual).distanceTo(expected)<=.22,why+" inverse requested-volume distance with native eighth-block quantization");
        require(newOffset.normalize().dot(oldOffset.normalize())>.999,why+" source bearing");
        double oldAttenuation=Math.max(0,1-oldOffset.length()/(16*original.getVolume()));
        double newAttenuation=Math.max(0,1-newOffset.length()/16);
        near(newAttenuation,oldAttenuation,.014,why+" initial attenuation retains old R=16P envelope");
        near(actual.getVolume()*newAttenuation,cap*oldAttenuation,.014*cap,why+" initial audible gain is capped");
    }
    private static void coordinateChecks(Swap alpha,Swap beta) {
        float cap=SoundNormalization.volumeCap(MONO);
        require(cap>0&&cap<1,"measured loud mono profile has finite reducing cap");
        var eye=alpha.player.getCamera().getEyePosition();
        var original=packet(custom(MONO),eye.add(32,8,16),4);
        var normalized=alpha.coordinate(original);
        projected(normalized,original,eye,cap,"loud mono");
        require(original.getVolume()==4,"original immutable packet gain");
        coordinates(original,packet(original.getSound(),eye.add(32,8,16),4),"original immutable packet");
        require(alpha.send(normalized)==normalized,"normalized coordinate packet is idempotent");
        for(float volume:new float[]{cap/2,cap,.5f,1}) {
            var input=packet(custom(MONO),eye.add(5,2,-3),volume);
            var actual=alpha.coordinate(input);
            identity(actual,input,"mono P<=1");coordinates(actual,input,"mono P<=1");
            near(actual.getVolume(),Math.min(volume,cap),1e-7,"quiet requests retain requested gain, loud requests cap");
        }
        var fixed=packet(Holder.direct(SoundEvent.createFixedRangeEvent(MONO,48)),eye.add(5,2,-3),.5f);
        var fixedOut=alpha.coordinate(fixed);
        identity(fixedOut,fixed,"fixed-range direct event");coordinates(fixedOut,fixed,"fixed-range direct event");
        near(fixedOut.getVolume(),cap,1e-7,"profile resolves event identifier with custom range retained");
        var stereo=packet(custom(STEREO),eye.add(32,8,16),4);
        var stereoOut=alpha.coordinate(stereo);
        identity(stereoOut,stereo,"stereo");coordinates(stereoOut,stereo,"stereo");
        near(stereoOut.getVolume(),SoundNormalization.volumeCap(STEREO),1e-7,"stereo gain cap");
        var pipe=packet(custom(PIPE),eye.add(32,8,16),4);
        require(alpha.send(pipe)==pipe&&SoundNormalization.volumeCap(PIPE)==1,"metal pipe event identity excluded");
        var vanilla=packet(SoundEvents.UI_BUTTON_CLICK,eye.add(32,8,16),4);
        require(alpha.send(vanilla)==vanilla,"registered native vanilla event untouched");
        var scream=packet(custom(SCREAM),eye.add(10,0,0),4);
        var reduced=alpha.coordinate(scream);
        require(reduced.getVolume()<1,"ordinary original scream reduced");
        coordinates(reduced,scream,"stereo original scream");
        alpha.clear();SoundNormalization.sendExempt(alpha.player,scream);
        require(alpha.packets().size()==1&&alpha.packets().getFirst()==scream,"explicit Psycho original scream exemption");
        require(alpha.coordinate(scream).getVolume()<1,"exemption scope ends after one send");
        require(beta.packets().isEmpty(),"private direct sounds never send packets to TestBeta");

        // New sends use the new eye position; already emitted payloads stay immutable.
        var saved=alpha.player.position();
        try {
            alpha.player.setPos(saved.x+8,saved.y,saved.z-4);
            var changed=alpha.coordinate(original);
            projected(changed,original,alpha.player.getCamera().getEyePosition(),cap,"changed listener pose");
            require(!position(changed).equals(position(normalized)),"new packet reprojects from current listener pose");
            projected(normalized,original,eye,cap,"previous packet retains its initial projection");
        } finally {alpha.player.setPos(saved.x,saved.y,saved.z);}
    }
    private static void entityChecks(Swap alpha,Swap beta) {
        var holder=custom(MONO);float cap=SoundNormalization.volumeCap(MONO);
        var source=beta.player;var eye=alpha.player.getCamera().getEyePosition();
        var saved=source.position();
        try {
            source.setPos(eye.x+32,eye.y+8,eye.z+16);
            var loud=new ClientboundSoundEntityPacket(holder,SoundSource.PLAYERS,source,4,.6f,926L);
            var result=alpha.send(loud);
            require(result instanceof ClientboundSoundPacket,"loud mono entity becomes projected coordinate sound");
            var equivalent=new ClientboundSoundPacket(holder,SoundSource.PLAYERS,source.getX(),source.getY(),source.getZ(),4,.6f,926L);
            projected((ClientboundSoundPacket)result,equivalent,eye,cap,"loud mono entity");
            require(loud.getId()==source.getId()&&loud.getVolume()==4,"original entity packet immutable");
            for(float volume:new float[]{cap/2,.5f,1}) {
                var bound=new ClientboundSoundEntityPacket(holder,SoundSource.PLAYERS,source,volume,.6f,926L);
                var actual=alpha.send(bound);
                require(actual instanceof ClientboundSoundEntityPacket,"mono P<=1 retains native entity binding");
                var entity=(ClientboundSoundEntityPacket)actual;
                require(entity.getId()==source.getId()&&entity.getSound()==holder&&entity.getSource()==bound.getSource()
                        &&entity.getPitch()==bound.getPitch()&&entity.getSeed()==bound.getSeed(),"entity identity/category/pitch/seed retained");
                near(entity.getVolume(),Math.min(volume,cap),1e-7,"entity gain cap respects quieter requests");
            }
            var stereo=new ClientboundSoundEntityPacket(custom(STEREO),SoundSource.PLAYERS,source,4,.6f,926L);
            var stereoResult=alpha.send(stereo);
            require(stereoResult instanceof ClientboundSoundEntityPacket,"stereo retains native entity binding");
            near(((ClientboundSoundEntityPacket)stereoResult).getVolume(),SoundNormalization.volumeCap(STEREO),1e-7,"stereo entity gain cap");
            var scream=new ClientboundSoundEntityPacket(custom(SCREAM),SoundSource.MASTER,source,4,1,41L);
            alpha.clear();SoundNormalization.sendExempt(alpha.player,scream);
            require(alpha.packets().size()==1&&alpha.packets().getFirst()==scream,"Psycho exemption preserves original entity packet/binding/gain");
            require(beta.packets().isEmpty(),"private entity sounds never send packets to TestBeta");
        } finally {source.setPos(saved.x,saved.y,saved.z);}
    }
    private static void bundleChecks(Swap alpha) {
        var eye=alpha.player.getCamera().getEyePosition();
        var mono=packet(custom(MONO),eye.add(32,8,16),4);
        var quiet=packet(custom(MONO),eye.add(2,1,0),SoundNormalization.volumeCap(MONO)/2);
        var pipe=packet(custom(PIPE),eye.add(32,8,16),4);
        var bundle=new ClientboundBundlePacket(List.<Packet<? super ClientGamePacketListener>>of(mono,quiet,pipe));
        alpha.clear();alpha.player.connection.send(bundle);
        require(alpha.recording.packets.size()==1&&alpha.recording.packets.getFirst() instanceof ClientboundBundlePacket,
                "one outer native bundle retained");
        var payload=alpha.packets();require(payload.size()==3,"bundle child count/order retained");
        projected((ClientboundSoundPacket)payload.get(0),mono,eye,SoundNormalization.volumeCap(MONO),"bundled mono normalized once");
        require(((ClientboundSoundPacket)payload.get(1)).getVolume()==quiet.getVolume()&&payload.get(2)==pipe,
                "bundle preserves quiet gain and exempt metal pipe");
        var output=alpha.recording.packets.getFirst();alpha.clear();alpha.player.connection.send(output);
        var again=alpha.packets();
        require(position((ClientboundSoundPacket)again.getFirst()).equals(position((ClientboundSoundPacket)payload.getFirst()))
                &&((ClientboundSoundPacket)again.getFirst()).getVolume()==((ClientboundSoundPacket)payload.getFirst()).getVolume(),
                "resending normalized bundle cannot project or reduce twice");
        require(mono.getVolume()==4,"bundle processing leaves original child immutable");
    }
    private static void audienceChecks(Swap alpha,Swap beta) {
        var at=alpha.player.position().add(60,0,0);var saved=beta.player.position();
        try {
            beta.player.setPos(at.x+100,at.y,at.z);
            alpha.clear();beta.clear();
            LbcOverload.sound(alpha.player.level(),at,MONO.toString(),4,1,61,0);
            require(alpha.packets().size()==1&&alpha.packets().getFirst() instanceof ClientboundSoundPacket,
                    "already selected distant listener still receives original R64 payload beyond new R16");
            var heard=(ClientboundSoundPacket)alpha.packets().getFirst();
            near(heard.getVolume(),SoundNormalization.volumeCap(MONO),1e-7,"distant selected payload is normalized");
            require(beta.packets().isEmpty(),"normalization does not broaden original selector audience");
        } finally {beta.player.setPos(saved.x,saved.y,saved.z);}
    }
    public static void run(GameController game) {
        var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        var beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(game.server().isSameThread()&&game.playing()&&alpha!=null&&beta!=null,"synchronous native two-player match fixture");
        require(game.server().getPlayerList().getPlayers().size()==2&&alpha.level()==beta.level(),"exclusive two-player dimension fixture");
        require(alpha.getCamera()==alpha&&beta.getCamera()==beta&&!game.items().fpvDrone().controlling(alpha)
                &&!game.items().fpvDrone().controlling(beta),"fixture starts with ordinary player cameras");
        try(var own=new Swap(game,alpha);var other=new Swap(game,beta)) {
            coordinateChecks(own,other);entityChecks(own,other);bundleChecks(own);audienceChecks(own,other);
        } finally {SoundNormalization.captureViews(game.server());}
        SkyWars2.LOGGER.info("[check] PASS sound-normalization (native packets; initial-pose spatial approximation)");
    }
}
