package me.skywars2.game.items.phone;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import me.skywars2.media.MediaPackets;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;

/** New clients own two players/decoders; this server sends only bounded, expiring metadata. */
final class TikTokDirectPlayback implements AutoCloseable {
    private static final class Clip {
        final TikTokMedia.CdnSource source;final UUID session=UUID.randomUUID();
        CompletableFuture<TikTokVelocityRelay.Delivery> relay;
        Clip(TikTokMedia.CdnSource source,CompletableFuture<TikTokVelocityRelay.Delivery> relay){this.source=source;this.relay=relay;}
    }
    private static final class Session {final Clip clip;boolean fallback;int retries;Session(Clip clip){this.clip=clip;}}
    private static final class Viewer {
        final ServerPlayer player;final ServerGamePacketListenerImpl listener;
        final Session[] playing=new Session[2];final boolean[] requested=new boolean[2];final long[] nextClick=new long[2];
        final ArrayDeque<Clip> upcoming=new ArrayDeque<>();List<MediaPackets.Link> prefetched=List.of();
        Viewer(ServerPlayer player){this.player=player;listener=player.connection;}
    }
    private final MinecraftServer server;private final TikTokCache cache;private final Consumer<ServerPlayer> publish;
    private final Map<UUID,Viewer> viewers=new HashMap<>();private TikTokLinkFeed feed;
    private final TikTokVelocityRelay relay;
    TikTokDirectPlayback(MinecraftServer server,TikTokCache cache,Consumer<ServerPlayer> publish){this(server,cache,publish,new TikTokLinkFeed(cache),TikTokVelocityRelay.configured());}
    TikTokDirectPlayback(MinecraftServer server,TikTokCache cache,Consumer<ServerPlayer> publish,TikTokLinkFeed feed,TikTokVelocityRelay relay){this.server=server;this.cache=cache;this.publish=publish;this.feed=feed;this.relay=relay;}
    int ready(){return feed.size();}
    boolean active(ServerPlayer p){var v=viewers.get(p.getUUID());return v!=null&&v.player==p&&v.listener==p.connection&&(v.playing[0]!=null||v.playing[1]!=null);}
    private Viewer viewer(ServerPlayer p){var old=viewers.get(p.getUUID());if(old!=null&&(old.player!=p||old.listener!=p.connection)){remove(old);old=null;}if(old==null){old=new Viewer(p);viewers.put(p.getUUID(),old);}return old;}
    private boolean current(Viewer v){return server.getPlayerList().getPlayer(v.player.getUUID())==v.player&&v.player.connection==v.listener;}
    boolean use(ServerPlayer p,InteractionHand hand){
        if(!TikTokClientSupport.independentHands(p)||!TikTokPlayback.phone(p.getItemInHand(hand)))return false;
        var v=viewer(p);int index=hand==InteractionHand.MAIN_HAND?0:1;long now=System.nanoTime();
        if(now<v.nextClick[index])return true;v.nextClick[index]=now+TimeUnit.MILLISECONDS.toNanos(250);v.requested[index]=true;
        int other=1-index;if(TikTokPlayback.phone(p.getItemInHand(other==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND))&&v.playing[other]==null)v.requested[other]=true;
        advance(v);return true;
    }
    private Clip reserve(TikTokMedia.CdnSource source){return new Clip(source,relay==null?CompletableFuture.completedFuture(null):relay.prepareSource(source));}
    private static TikTokVelocityRelay.Delivery delivery(Clip clip){try{return clip.relay.getNow(null);}catch(CompletionException|CancellationException ignored){return null;}}
    private static MediaPackets.Link link(Clip clip,boolean fallback){var ready=delivery(clip);return new MediaPackets.Link(clip.session,
            fallback?"":clip.source.uri().toASCIIString(),ready==null?"":ready.uri().toASCIIString(),ready!=null);}
    private void advance(Viewer v){
        long now=Instant.now().getEpochSecond();v.upcoming.removeIf(c->c.source.expiresAt()<now+30);
        while(v.upcoming.size()<3){var source=feed.take();if(source==null)break;v.upcoming.addLast(reserve(source));}
        boolean changed=false;
        for(int hand=0;hand<2;hand++)if(v.requested[hand]&&!v.upcoming.isEmpty()){
            var next=v.upcoming.removeFirst();stop(v,hand);v.playing[hand]=new Session(next);v.requested[hand]=false;
            ServerPlayNetworking.send(v.player,new MediaPackets.HandLink(hand,link(next,false)));changed=true;
        }
        if(changed)publish.accept(v.player);
        var upcoming=new ArrayList<MediaPackets.Link>();for(var clip:v.upcoming)upcoming.add(link(clip,false));
        if(!upcoming.equals(v.prefetched)){v.prefetched=List.copyOf(upcoming);ServerPlayNetworking.send(v.player,new MediaPackets.Prefetch(v.prefetched));}
        for(int hand=0;hand<2;hand++){
            var session=v.playing[hand];if(session==null||!session.fallback||!session.clip.relay.isDone())continue;
            var available=delivery(session.clip);session.fallback=false;
            if(available!=null)ServerPlayNetworking.send(v.player,new MediaPackets.HandLink(hand,link(session.clip,true)));
        }
    }
    boolean fallback(ServerPlayer p,MediaPackets.Fallback request){
        var v=viewers.get(p.getUUID());if(v==null||v.player!=p||v.listener!=p.connection)return false;
        for(var session:v.playing)if(session!=null&&session.clip.session.equals(request.session())){
            if(relay!=null&&session.retries++<3){
                if(request.relayFailed()||session.clip.relay.isCompletedExceptionally())relay.forgetSources();
                session.clip.relay=relay.prepareSource(session.clip.source);session.fallback=true;
            }
            return true;
        }return false;
    }
    void tick(){
        boolean demand=false;var alive=new HashSet<UUID>();
        for(var p:server.getPlayerList().getPlayers())if(TikTokClientSupport.independentHands(p)&&p.isAlive()&&!p.isRemoved()
                &&p.getInventory().contains(TikTokPlayback::phone)){
            demand=true;alive.add(p.getUUID());var v=viewer(p);
            for(int hand=0;hand<2;hand++)if(!TikTokPlayback.phone(p.getItemInHand(hand==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND))){v.requested[hand]=false;if(v.playing[hand]!=null){stop(v,hand);publish.accept(p);}}
            advance(v);
        }
        for(var v:List.copyOf(viewers.values()))if(!alive.contains(v.player.getUUID())||!current(v))remove(v);
        feed.tick(demand);
    }
    private void stop(Viewer v,int hand){var session=v.playing[hand];if(session==null)return;v.playing[hand]=null;if(current(v))ServerPlayNetworking.send(v.player,new MediaPackets.Stop(session.clip.session));}
    private void remove(Viewer v){viewers.remove(v.player.getUUID(),v);stop(v,0);stop(v,1);v.upcoming.clear();if(current(v)){ServerPlayNetworking.send(v.player,new MediaPackets.Prefetch(List.of()));publish.accept(v.player);}}
    void reset(){for(var v:List.copyOf(viewers.values()))remove(v);feed.close();feed=new TikTokLinkFeed(cache);}
    @Override public void close(){for(var v:List.copyOf(viewers.values()))remove(v);feed.close();if(relay!=null)relay.close();}
}
