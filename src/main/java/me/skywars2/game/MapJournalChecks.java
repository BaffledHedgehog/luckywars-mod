package me.skywars2.game;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** Holds the actual loader's file sink while native server frames and rapid selections continue. */
public final class MapJournalChecks {
    private static Scene active;
    private static void require(boolean ok, String message) { if (!ok) throw new IllegalStateException("Map journal: " + message); }
    private static Field field(Object object, String name) {
        try { var field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field; }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    private static Object get(Object object, String name) {
        try { return field(object,name).get(object); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    private static void set(Object object, String name, Object value) {
        try { field(object,name).set(object,value); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    private static final class Gate {
        final CountDownLatch entered=new CountDownLatch(1), release=new CountDownLatch(1);
        final AtomicInteger calls=new AtomicInteger();
    }
    public static void run(GameController game) {
        require(active==null&&game.inLobby()&&!game.maps().busy(),"exclusive ready lobby");
        active=new Scene(game);
        try { active.start(); }
        catch (Throwable error) { active.close();active=null;throw error; }
    }
    private static final class Scene implements AutoCloseable {
        final GameController game;
        final MapManager maps;
        final ServerPlayer owner;
        final BoundsJournalWriter writer;
        final BoundsJournalWriter.Sink original;
        final String oldMap;
        final boolean oldBroken;
        final int started;
        final Path journal;
        final AtomicBoolean failNext=new AtomicBoolean();
        volatile Gate gate;
        CompletableFuture<Boolean> normal,coalesced,failed;
        int phase,heldAt=-1,enteredAt=-1,selectionGeneration;
        Scene(GameController game) {
            this.game=game;maps=game.maps();
            owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            oldMap=game.config().mapId;oldBroken=game.config().broken;started=game.server().getTickCount();
            writer=(BoundsJournalWriter)get(maps,"journalWriter");original=(BoundsJournalWriter.Sink)get(writer,"sink");
            journal=(Path)get(maps,"boundsJournal");
        }
        void start() {
            gate=new Gate();
            set(writer,"sink",(BoundsJournalWriter.Sink)(path,json)->{
                require(!game.server().isSameThread(),"file IO must never run on the server thread");
                Gate held=gate;
                if (held!=null) {
                    held.calls.incrementAndGet();held.entered.countDown();
                    try { if(!held.release.await(60,TimeUnit.SECONDS))throw new IOException("test IO gate deadline"); }
                    catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IOException(interrupted);}
                }
                if(path.getFileName().toString().equals("overworld-bounds.json")&&failNext.getAndSet(false))
                    throw new IOException("expected injected journal-write failure");
                original.write(path,json);
            });
            change(4096);normal=latest();
            require(!normal.isDone(),"submission returns before its held file write");
        }
        @SuppressWarnings("unchecked") int queued() { synchronized(writer){return ((Map<Path,?>)get(writer,"pending")).size();} }
        @SuppressWarnings("unchecked") CompletableFuture<Boolean> latest(){return (CompletableFuture<Boolean>)get(maps,"lastBoundsWrite");}
        void change(int x) {
            maps.recordBlockChange(new BlockPos(x,100,4096));
            try { var method=MapManager.class.getDeclaredMethod("flushChangedBlockBounds");method.setAccessible(true);method.invoke(maps); }
            catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }
        boolean contains(int x) {
            try {
                var root=JsonParser.parseString(Files.readString(journal)).getAsJsonObject();
                require(root.get("version").getAsInt()==2&&root.get("dimension").getAsString().equals("minecraft:overworld"),"native durable journal schema/dimension");
                for(var entry:root.getAsJsonArray("bounds")) {
                    var box=entry.getAsJsonArray();
                    if(box.get(0).getAsInt()<=x&&box.get(3).getAsInt()>=x&&box.get(1).getAsInt()<=100&&box.get(4).getAsInt()>=100
                            &&box.get(2).getAsInt()<=4096&&box.get(5).getAsInt()>=4096)return true;
                }
                return false;
            } catch(IOException error){throw new IllegalStateException(error);}
        }
        void select(String map, boolean broken) { require(game.selectMap(owner,map,broken),"actual selection accepted "+map); }
        boolean tick() {
            int tick=game.server().getTickCount();
            require(game.inLobby()&&tick-started<4800,"bounded actual lobby scenario");
            switch(phase) {
                case 0 -> {
                    if(gate.entered.getCount()!=0)return false;
                    heldAt=tick;
                    for(int i=0;i<100;i++) {
                        change(4097+i);
                        var next=latest();
                        if(coalesced==null)coalesced=next;else require(coalesced==next,"one shared pending completion per path");
                        require(queued()<=2,"bounded pending snapshots under100writes");
                    }
                    phase=1;
                }
                case 1 -> {
                    require(!normal.isDone()&&!coalesced.isDone()&&!maps.busy(),"normal IO stall does not block lobby frames");
                    if(tick-heldAt<30)return false;
                    gate.release.countDown();phase=2;
                }
                case 2 -> {
                    if(!coalesced.isDone())return false;
                    require(normal.getNow(false)&&coalesced.getNow(false)&&contains(4196),"latest coalesced snapshot reached the real file");
                    require(gate.calls.get()==2,"100pending revisions need only two real writes");
                    gate=null;failNext.set(true);change(4220);failed=latest();phase=3;
                }
                case 3 -> {
                    if(!failed.isDone())return false;
                    require(!failed.getNow(true),"injected write failed");
                    if(latest()==failed||!latest().isDone())return false;
                    require(latest().getNow(false)&&contains(4220),"ordinary failed snapshot retried from retained runtime bounds");
                    gate=new Gate();select("60",false);phase=4;
                }
                case 4 -> {
                    if(gate.entered.getCount()!=0)return false;
                    heldAt=tick;phase=5;
                }
                case 5 -> {
                    require(get(maps,"journalWait")!=null&&maps.streamingChangedChunks().isEmpty()&&(int)get(maps,"completedSlices")==0,
                            "no world clearing/pasting before recovery write completes");
                    if(tick-heldAt<15)return false;
                    select("61",false);selectionGeneration=((Long)get(maps,"preparationGeneration")).intValue();phase=6;
                }
                case 6 -> {
                    require(maps.streamingChangedChunks().isEmpty(),"superseded loading cannot mutate blocks while IO held");
                    require(queued()<=2,"rapid map selection keeps one pending snapshot per dimension");
                    if(get(maps,"journalWait")==null)return false;
                    require(((Long)get(maps,"preparationGeneration")).intValue()==selectionGeneration,"latest preparation stays authoritative");
                    maps.recordBlockChange(new BlockPos(4240,100,4096));
                    gate.release.countDown();enteredAt=tick;phase=7;
                }
                case 7 -> {
                    if((int)get(maps,"completedSlices")>0&&maps.busy()) {
                        require(Objects.equals(get(maps,"pendingMap"),"61"),"discarded journal continuation never places the old map");
                        require(contains(4240),"outer writes arriving during IO committed before first placement slice");
                    }
                    if(!game.selectedMapReady())return false;
                    require(game.config().mapId.equals("61")&&tick>enteredAt,"latest selection alone becomes ready");
                    gate=null;failNext.set(true);select(oldMap,oldBroken);phase=8;
                }
                case 8 -> {
                    if(maps.busy())return false;
                    require(!game.selectedMapReady()&&maps.streamingChangedChunks().isEmpty()&&(int)get(maps,"completedSlices")==0,
                            "failed recovery write rejects selection before world changes");
                    select(oldMap,oldBroken);phase=9;
                }
                case 9 -> {
                    if(!game.selectedMapReady())return false;
                    require(game.config().mapId.equals(oldMap)&&game.config().broken==oldBroken,"original map restored via actual public loader");
                    MapReplacementChecks.imprinted(game);
                    return true;
                }
                default -> throw new IllegalStateException("phase"+phase);
            }
            return false;
        }
        public void close() { if(gate!=null)gate.release.countDown();gate=null;set(writer,"sink",original); }
    }
    public static void tick(GameController game) {
        if(active==null)return;
        try {
            if(!active.tick())return;
            active.close();active=null;
            SkyWars2.LOGGER.info("[check] PASS map-journal (30actual frames during held IO;100coalesced writes; retry, pre-clear barrier, supersession, late outer bounds and Imprinted restoration)");
        } catch(Throwable error) {
            active.close();active=null;SkyWars2.LOGGER.error("[check] FAIL map-journal",error);
        }
    }
}
