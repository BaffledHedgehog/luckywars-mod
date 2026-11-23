package me.skywars2.game.items.phone;

import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import me.skywars2.SkyWars2;
import me.skywars2.game.*;
import me.skywars2.game.items.gadgets.LbcSpecialArtifactItems;
import me.skywars2.media.MediaPackets;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Real Fabric negotiation and native packet recipients, with an isolated metadata-only feed. */
public final class TikTokDirectChecks {
    private static Scene running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Direct phone: "+why);}
    public static void start(GameController game){require(running==null,"exclusive fixture");try{running=new Scene(game);}catch(Exception error){throw new IllegalStateException(error);}}
    public static void tick(GameController game){var s=running;if(s==null||s.game!=game)return;
        try{if(!s.step())return;running=null;s.close();SkyWars2.LOGGER.info("[check] PASS tiktok-direct (actual Fabric handshake, metadata prefetch without prepared media, independent two hands, exact wire codec, per-hand stop, no frame/audio packets and recipient isolation)");}
        catch(Throwable failure){running=null;try{s.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tiktok-direct",failure);}}
    private static <T> List<T> media(List<Packet<?>> packets,Class<T> type){return SmokeVolumeChecks.flatten(packets).stream()
            .filter(ClientboundCustomPayloadPacket.class::isInstance).map(ClientboundCustomPayloadPacket.class::cast)
            .map(ClientboundCustomPayloadPacket::payload).filter(type::isInstance).map(type::cast).toList();}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final Path directory;final TikTokCache cache;
        final TikTokDirectPlayback playback;final TikTokLinkFeed feed;final AtomicInteger fetched=new AtomicInteger(),published=new AtomicInteger();
        final int began;int phase;UUID main,off;boolean closed;
        Scene(GameController game)throws Exception{
            this.game=game;f=new WoolBucketChecks.Fixture(game,false);f.prepareParticipants();began=game.server().getTickCount();
            directory=Files.createTempDirectory("skywars-direct-phone-");var cookies=directory.resolve("cookies.json");Files.writeString(cookies,"[]");
            cache=new TikTokCache(directory.resolve("cache"),cookies,false);
            feed=new TikTokLinkFeed(cache,excluded->{int batch=fetched.getAndIncrement();var result=new ArrayList<TikTokMedia.CdnSource>();long now=Instant.now().getEpochSecond();
                for(int i=0;i<20;i++){String id=Long.toString(12000000L+batch*20+i);String url="https://v16-webapp.tiktokcdn-us.com/video/tos/test-region/"+id+"/?expire="+(now+1800);
                    var source=TikTokMedia.sourceDescriptor(id,url,now+900,now);require(source!=null,"synthetic valid descriptor");result.add(source);}return result;});
            playback=new TikTokDirectPlayback(game.server(),cache,p->published.incrementAndGet(),feed,null);
            var addon=net.fabricmc.fabric.impl.networking.server.ServerNetworkingImpl.getAddon(f.owner().connection);
            addon.handle(new net.fabricmc.fabric.impl.networking.RegistrationPayload(net.fabricmc.fabric.impl.networking.RegistrationPayload.REGISTER,
                    List.of(MediaPackets.Frame.TYPE.id(),MediaPackets.Stop.TYPE.id(),MediaPackets.Link.TYPE.id(),MediaPackets.HandLink.TYPE.id(),MediaPackets.Prefetch.TYPE.id())));
            addon.handle(new MediaPackets.Hello(MediaPackets.VERSION,MediaPackets.FEATURES|MediaPackets.HTTPS_PLAYBACK|MediaPackets.DIRECT_MP4|MediaPackets.INDEPENDENT_HANDS));
            require(TikTokClientSupport.independentHands(f.owner()),"actual registered channels and Hello");
            var phone=new ItemStack(LbcSpecialArtifactItems.Kind.ADMIN_ZAIKA.item);f.owner().getInventory().setItem(35,phone.copy());
        }
        boolean step(){
            require(game.server().getTickCount()-began<150,"bounded async fixture");playback.tick();
            if(phase==0){
                var prefetch=media(f.client().packets(),MediaPackets.Prefetch.class);if(prefetch.isEmpty())return false;
                require(cache.sizeReady()==0&&prefetch.getLast().clips().size()==3&&fetched.get()==1,"three upcoming links before first use and no full server media");
                require(!playback.active(f.owner()),"inventory prefetch does not start playback");
                var phone=new ItemStack(LbcSpecialArtifactItems.Kind.ADMIN_ZAIKA.item);f.owner().setItemInHand(InteractionHand.MAIN_HAND,phone.copy());f.owner().setItemInHand(InteractionHand.OFF_HAND,phone.copy());
                require(playback.use(f.owner(),InteractionHand.MAIN_HAND),"native hand use accepted");
                var links=media(f.client().packets(),MediaPackets.HandLink.class);require(links.size()==2,"both hands start");
                var a=links.stream().filter(x->x.hand()==0).findFirst().orElseThrow();var b=links.stream().filter(x->x.hand()==1).findFirst().orElseThrow();main=a.link().session();off=b.link().session();
                require(!main.equals(off)&&!a.link().cdn().equals(b.link().cdn()),"different sessions and video URLs");
                var bytes=new RegistryFriendlyByteBuf(Unpooled.buffer(),f.owner().registryAccess());try{MediaPackets.HandLink.CODEC.encode(bytes,b);require(b.equals(MediaPackets.HandLink.CODEC.decode(bytes))&&!bytes.isReadable(),"actual exact hand payload codec");}finally{bytes.release();}
                require(playback.active(f.owner())&&published.get()==1,"publish enhanced projection once");
                f.owner().setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);phase=1;return false;
            }
            var stops=media(f.client().packets(),MediaPackets.Stop.class);
            require(stops.stream().anyMatch(x->x.session().equals(off))&&stops.stream().noneMatch(x->x.session().equals(main)),"Q/removal stops only corresponding hand");
            require(playback.active(f.owner()),"other hand keeps playing");
            require(media(f.client().packets(),MediaPackets.Frame.class).isEmpty()&&media(f.client().packets(),MediaPackets.Segment.class).isEmpty(),"no video/audio payload on game connection");
            require(media(f.audience().packets(),MediaPackets.HandLink.class).isEmpty(),"no private links to other player");
            playback.close();require(!playback.active(f.owner())&&media(f.client().packets(),MediaPackets.Stop.class).stream().anyMatch(x->x.session().equals(main)),"end/reset stops remaining session");return true;
        }
        public void close()throws Exception{if(closed)return;closed=true;playback.close();cache.close();try{f.close();}finally{try(var paths=Files.walk(directory)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}}
    }
}
