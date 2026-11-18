package me.skywars2.game;

import java.util.*;
import java.util.concurrent.*;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;

/** Per-viewer 20 FPS clock, one latest read per viewer, recipient-only native item projection. */
public final class TikTokPlayback implements AutoCloseable {
    static final Identifier VIDEO_MODEL=Identifier.parse("lbc:zaika_video");
    static final Identifier CLIENT_MODEL=Identifier.parse("skywars2:phone_client");
    private static final long FRAME_NANOS=50_000_000L, COOLDOWN=5_000_000_000L;
    private record Read(long serial, int pixelStep, CustomModelData colors) {}
    private static final class View {
        final TikTokMedia clip;
        final long started;
        final boolean enhanced;
        final TikTokOutput output;
        long lastRead = -1;
        Future<Read> pending;
        volatile List<Integer> slots;
        volatile boolean active = true;

        View(TikTokMedia clip, long now, boolean enhanced, TikTokOutput output, List<Integer> slots) {
            this.clip = clip;
            started = now;
            this.enhanced = enhanced;
            this.output = output;
            this.slots = slots;
        }
    }
    private final MinecraftServer server;
    private final TikTokCache cache;
    private final Map<ServerPlayer,View> views=new HashMap<>();
    private final Map<ServerPlayer,Long> cooldowns=new WeakHashMap<>();
    private final Map<ServerPlayer,String> history=new WeakHashMap<>();
    private final Set<UUID> enhancedViewers = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String,Integer> inFlight=new ConcurrentHashMap<>();
    private final Set<String> retire=new HashSet<>();
    private final ExecutorService reader=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(64),
            r->{var t=new Thread(r,"skywars-tiktok-frames");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    private boolean closed;
    private final TikTokSoundClock audioClock=new TikTokSoundClock(id->inFlight.merge(id,1,Integer::sum),this::release);
    private final TikTokClientClock clientClock=new TikTokClientClock(id->inFlight.merge(id,1,Integer::sum),this::release);
    TikTokPlayback(MinecraftServer server,TikTokCache cache){this.server=server;this.cache=cache;}
    boolean enhanced(ServerPlayer player) { return enhancedViewers.contains(player.getUUID()); }
    static boolean phone(ItemStack stack){return stack.is(LbcSpecialArtifactItems.Kind.ADMIN_ZAIKA.item);}

    boolean use(ServerPlayer player){
        if(closed||server.getPlayerList().getPlayer(player.getUUID())!=player)return false;
        var held = slots(player);
        var output = TikTokOutput.forPlayer(player);
        if (held.isEmpty() || output == null) return false;
        long now=System.nanoTime();var old=views.get(player);
        if(now<cooldowns.getOrDefault(player,Long.MIN_VALUE)){loading(player);return true;}
        var clip=cache.next(history.getOrDefault(player,""));
        if(clip==null){loading(player);return true;}
        var view = new View(clip, now, TikTokClientSupport.enhanced(player), output, held);
        if (old != null) old.active = false;
        clientClock.stop(player);
        audioClock.stop(player);
        views.put(player, view);
        if (view.enhanced) {
            enhancedViewers.add(player.getUUID());
            clientClock.play(player, clip, now, output);
        } else {
            enhancedViewers.remove(player.getUUID());
            audioClock.play(player, clip, now, output);
        }
        history.put(player, clip.id);
        cooldowns.put(player, now + COOLDOWN);
        publish(player, held);
        if (old != null && old.clip != clip) retire.add(old.clip.id);
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
    private static ItemStack project(ServerPlayer player, ItemStack stack) {
        var context = ((PacketContextProvider) player.connection).getPacketContext();
        return PolymerItemUtils.getPolymerItemStack(stack, TooltipFlag.NORMAL, context, player.registryAccess());
    }

    static ItemStack frameStack(ServerPlayer player, ItemStack stack, CustomModelData colors) {
        var proxy = project(player, stack).copy();
        proxy.set(DataComponents.ITEM_MODEL, VIDEO_MODEL);
        proxy.set(DataComponents.CUSTOM_MODEL_DATA, colors);
        return proxy;
    }

    private static void publish(ServerPlayer player, List<Integer> slots) {
        for (int slot : slots) {
            var stack = player.getInventory().getItem(slot);
            if (phone(stack)) player.connection.send(new ClientboundSetPlayerInventoryPacket(slot, project(player, stack)));
        }
    }

    private static void publishFrame(ServerPlayer player, View view, Read frame) {
        // Ordinary inventory and equipment replication use the small phone model. Only this
        // recipient-specific path can attach pixels, and every write consumes media credit.
        if (frame.pixelStep != view.output.vanillaPixelStep()) return;
        var slots = view.slots;
        var packets = new ArrayList<Packet<? super ClientGamePacketListener>>(slots.size());
        for (int slot : slots) {
            var stack = player.getInventory().getItem(slot);
            if (phone(stack)) packets.add(new ClientboundSetPlayerInventoryPacket(slot, frameStack(player, stack, frame.colors)));
        }
        if (packets.isEmpty()) return;
        var packet = packets.size() == 1 ? packets.getFirst() : new ClientboundBundlePacket(packets);
        view.output.offerVanillaFrame(packet, view.started + (frame.serial + 3) * FRAME_NANOS,
                () -> view.active && view.slots == slots && !TimeStopView.frozen(player), frame.pixelStep);
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
            if (server.getPlayerList().getPlayer(player.getUUID()) != player || !player.isAlive() || player.isRemoved()
                    || !view.output.active() || current.isEmpty() || !cache.ids().contains(view.clip.id)) {
                view.active = false;
                clientClock.stop(player);audioClock.stop(player);enhancedViewers.remove(player.getUUID());
                for (int slot : view.slots) original(player, slot);
                retire.add(view.clip.id);iterator.remove();continue;
            }
            if(!view.slots.equals(current)){
                var prior=view.slots;view.slots=current;
                for(int slot:prior)if(!current.contains(slot))original(player,slot);
                publish(player,current);
            }
            if (view.enhanced || TimeStopView.frozen(player) || !view.output.vanillaFrameReady()) continue;
            long serial=Math.max(0,now-view.started)/FRAME_NANOS;
            if (view.pending != null && view.pending.isDone()) {
                try {
                    var result = view.pending.get();
                    if (serial - result.serial <= 2) publishFrame(player, view, result);
                } catch (ExecutionException | CancellationException ignored) {
                    // Failed or retired reads drop a frame.
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
                view.pending = null;
            }
            // Prefetch at most one newer frame while the sent frame awaits its receipt.
            if(view.pending==null&&serial!=view.lastRead){
                view.lastRead=serial;int frame=(int)(serial%view.clip.frames);
                int pixelStep=view.output.vanillaPixelStep();
                // One pending read per active view; a hard queue cap also bounds rapid view replacements during stalled I/O.
                inFlight.merge(view.clip.id,1,Integer::sum);
                try{
                    view.pending=reader.submit(()->{
                        try{return new Read(serial,pixelStep,TikTokPixels.encode(view.clip.frame(frame,false).map(),pixelStep));}
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
        views.values().forEach(view -> view.active = false);
        clientClock.clear();
        audioClock.clear();
        enhancedViewers.clear();
        for (var entry : views.entrySet()) publish(entry.getKey(), entry.getValue().slots);
        views.clear();cooldowns.clear();history.clear();retire.clear();
    }
    @Override public void close(){if(closed)return;closed=true;reset();clientClock.close();audioClock.close();reader.shutdownNow();}
}
