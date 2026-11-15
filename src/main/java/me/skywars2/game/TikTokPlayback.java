package me.skywars2.game;

import java.util.*;
import java.util.concurrent.*;
import me.skywars2.mixin.MediaTransportAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/** Per-viewer 20 FPS clock, one latest read per viewer, recipient-only native item projection. */
public final class TikTokPlayback implements AutoCloseable {
    static final Identifier VIDEO_MODEL=Identifier.parse("lbc:zaika_video");
    static final Identifier CLIENT_MODEL=Identifier.parse("skywars2:phone_client");
    private static final long FRAME_NANOS=50_000_000L, COOLDOWN=5_000_000_000L;
    public record Screen(CustomModelData colors,int frame,long serial,boolean enhanced){
        Screen(CustomModelData colors,int frame,long serial){this(colors,frame,serial,false);}
    }
    private record Read(long serial,int frame,CustomModelData colors){}
    private static final class View {
        final TikTokMedia clip;final long started;final boolean enhanced;
        long lastRead=-1;Future<Read> pending;
        List<Integer> slots=List.of();
        View(TikTokMedia clip,long now,boolean enhanced){this.clip=clip;started=now;this.enhanced=enhanced;}
    }
    private final MinecraftServer server;
    private final TikTokCache cache;
    private final Map<ServerPlayer,View> views=new HashMap<>();
    private final Map<ServerPlayer,Long> cooldowns=new WeakHashMap<>();
    private final Map<ServerPlayer,String> history=new WeakHashMap<>();
    private final ConcurrentMap<UUID,Screen> screens=new ConcurrentHashMap<>();
    private final ConcurrentMap<String,Integer> inFlight=new ConcurrentHashMap<>();
    private final Set<String> retire=new HashSet<>();
    private final ExecutorService reader=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(64),
            r->{var t=new Thread(r,"skywars-tiktok-frames");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    private boolean closed;
    private final TikTokSoundClock audioClock=new TikTokSoundClock(id->inFlight.merge(id,1,Integer::sum),this::release);
    private final TikTokClientClock clientClock=new TikTokClientClock(id->inFlight.merge(id,1,Integer::sum),this::release);
    TikTokPlayback(MinecraftServer server,TikTokCache cache){this.server=server;this.cache=cache;}
    Screen screen(ServerPlayer player){return screens.get(player.getUUID());}
    static boolean phone(ItemStack stack){return stack.is(LbcSpecialArtifactItems.Kind.ADMIN_ZAIKA.item);}

    boolean use(ServerPlayer player){
        if(closed||server.getPlayerList().getPlayer(player.getUUID())!=player)return false;
        var held=slots(player);if(held.isEmpty())return false;
        long now=System.nanoTime();var old=views.get(player);
        if(now<cooldowns.getOrDefault(player,Long.MIN_VALUE)){loading(player);return true;}
        var clip=cache.next(history.getOrDefault(player,""));
        if(clip==null){loading(player);return true;}
        var view=new View(clip,now,TikTokClientSupport.enhanced(player));view.slots=held;
        clientClock.stop(player);audioClock.stop(player);views.put(player,view);
        if(view.enhanced)clientClock.play(player,clip,now);else audioClock.play(player,clip,now);
        history.put(player,clip.id);cooldowns.put(player,now+COOLDOWN);
        screens.put(player.getUUID(),view.enhanced?new Screen(null,0,-1,true):new Screen(TikTokPixels.encode(new byte[TikTokMedia.PIXELS]),0,-1));
        publish(player,view.slots);if(old!=null&&old.clip!=clip)retire.add(old.clip.id);
        return true;
    }
    private static void loading(ServerPlayer player){
        player.sendSystemMessage(Component.translatable("tiktok.loading").withStyle(style->style.withColor(ChatFormatting.RED).withItalic(false)));
    }
    private static List<Integer> slots(ServerPlayer player){
        var slots=new ArrayList<Integer>(2);int selected=player.getInventory().getSelectedSlot();
        if(phone(player.getMainHandItem()))slots.add(selected);
        if(phone(player.getOffhandItem()))slots.add(net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND);
        return slots;
    }
    private static boolean writable(ServerPlayer player){
        var connection=((MediaTransportAccess.Listener)(Object)player.connection).skywars2$mediaConnection();
        var channel=((MediaTransportAccess.Transport)(Object)connection).skywars2$mediaChannel();
        return channel!=null&&channel.isActive()&&channel.isWritable();
    }
    private static void publish(ServerPlayer player,List<Integer> slots){
        publish(player,slots,null,0);
    }
    private static void publish(ServerPlayer player,List<Integer> slots,byte[] bands,long serial){
        var context=((net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)player.connection).getPacketContext();
        var packets=new ArrayList<net.minecraft.network.protocol.Packet<? super net.minecraft.network.protocol.game.ClientGamePacketListener>>();
        for(int slot:slots){
            var stack=player.getInventory().getItem(slot);if(!phone(stack))continue;
            // Freeze the recipient projection now: a later Netty encode must not change an older frame's pixels.
            var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(stack,net.minecraft.world.item.TooltipFlag.NORMAL,context,player.registryAccess());
            packets.add(new ClientboundSetPlayerInventoryPacket(slot,proxy));
        }
        // Sound has its own 60 Hz wall clock and never waits for a video publication.
        if(packets.size()==1)player.connection.send(packets.getFirst());
        else if(!packets.isEmpty())player.connection.send(new ClientboundBundlePacket(packets));
    }
    private static void original(ServerPlayer player,int slot){
        var stack=player.getInventory().getItem(slot);
        var context=((net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)player.connection).getPacketContext();
        var proxy=stack.isEmpty()?ItemStack.EMPTY:phone(stack)
                ?eu.pb4.polymer.core.api.item.PolymerItemUtils.createItemStack(stack,net.minecraft.world.item.TooltipFlag.NORMAL,context,player.registryAccess())
                :eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(stack,net.minecraft.world.item.TooltipFlag.NORMAL,context,player.registryAccess());
        player.connection.send(new ClientboundSetPlayerInventoryPacket(slot,proxy));
    }

    void tick(){
        if(closed)return;
        long now=System.nanoTime();
        var iterator=views.entrySet().iterator();
        while(iterator.hasNext()){
            var entry=iterator.next();var player=entry.getKey();var view=entry.getValue();
            var current=slots(player);
            if(server.getPlayerList().getPlayer(player.getUUID())!=player||!player.isAlive()||player.isRemoved()||current.isEmpty()||!cache.ids().contains(view.clip.id)){
                clientClock.stop(player);audioClock.stop(player);screens.remove(player.getUUID());for(int slot:view.slots)original(player,slot);
                retire.add(view.clip.id);iterator.remove();continue;
            }
            if(!view.slots.equals(current)){
                var prior=view.slots;view.slots=current;
                for(int slot:prior)if(!current.contains(slot))original(player,slot);
                publish(player,current);
            }
            if(!writable(player))continue; // Neither frame work nor stale packets accumulate behind a slow channel.
            if(view.enhanced)continue;
            long serial=Math.max(0,now-view.started)/FRAME_NANOS;
            if(view.pending!=null&&view.pending.isDone()){
                try{
                    var result=view.pending.get();
                    if(serial-result.serial<=2){
                        screens.put(player.getUUID(),new Screen(result.colors,result.frame,result.serial));
                        publish(player,current);
                    }
                }catch(Exception error){screens.remove(player.getUUID());publish(player,current);}
                view.pending=null;
            }
            if(view.pending==null&&serial!=view.lastRead){
                view.lastRead=serial;int frame=(int)(serial%view.clip.frames);
                // One pending read per active view; a hard queue cap also bounds rapid view replacements during stalled I/O.
                inFlight.merge(view.clip.id,1,Integer::sum);
                try{
                    view.pending=reader.submit(()->{
                        try{return new Read(serial,frame,TikTokPixels.encode(view.clip.frame(frame,false).map()));}
                        finally{release(view.clip.id);}
                    });
                }catch(RejectedExecutionException busy){release(view.clip.id);}
            }
        }
        Set<String> pins=pins();
        var expired=retire.iterator();while(expired.hasNext()){
            String id=expired.next();if(pins.contains(id))continue;cache.watched(id,pins);expired.remove();
        }
    }
    Set<String> pins(){
        var pins=new HashSet<>(inFlight.keySet());for(var view:views.values())pins.add(view.clip.id);return pins;
    }
    private void release(String id){inFlight.computeIfPresent(id,(key,count)->count==1?null:count-1);}
    void reset(){
        clientClock.clear();audioClock.clear();screens.clear();for(var entry:views.entrySet())publish(entry.getKey(),entry.getValue().slots);
        views.clear();cooldowns.clear();history.clear();retire.clear();
    }
    @Override public void close(){if(closed)return;closed=true;reset();clientClock.close();audioClock.close();reader.shutdownNow();}
}
