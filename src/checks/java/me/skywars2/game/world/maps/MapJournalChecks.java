package me.skywars2.game.world.maps;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import me.skywars2.SkyWars2;
import me.skywars2.game.GameController;
import me.skywars2.game.world.maps.MapManager;
import me.skywars2.game.world.terrain.BoundsJournalWriter;
import me.skywars2.game.world.terrain.BoundsJournalWriter.Sink;
import me.skywars2.validation.mixin.MapValidationAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import me.skywars2.game.GameChecks;

/** Holds the actual loader's file sink while native server frames and rapid selections continue. */
public final class MapJournalChecks {
    private static final BlockPos LATE_CHANGE = new BlockPos(4240, 100, 4096);
    private static Scene active;
    private static void require(boolean ok, String message) { if (!ok) throw new IllegalStateException("Map journal: " + message); }
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
        final MapValidationAccess.Manager mapState;
        final ServerPlayer owner;
        final BoundsJournalWriter writer;
        final MapValidationAccess.Journal journalState;
        final BoundsJournalWriter.Sink original;
        final String oldMap;
        final boolean oldBroken;
        final int started;
        final Path journal;
        final AtomicBoolean failNext=new AtomicBoolean();
        volatile Gate gate;
        CompletableFuture<Boolean> normal,coalesced,failed;
        int phase,heldAt=-1,enteredAt=-1;
        long selectionGeneration;
        boolean observedFirstSlice;
        Scene(GameController game) {
            this.game=game;maps=game.maps();
            mapState=(MapValidationAccess.Manager)(Object)maps;
            owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            oldMap=game.config().mapId;oldBroken=game.config().broken;started=game.server().getTickCount();
            writer=mapState.validation$writer();
            journalState=(MapValidationAccess.Journal)(Object)writer;
            original=journalState.validation$sink();
            journal=mapState.validation$journal();
        }
        void start() {
            gate=new Gate();
            journalState.validation$sink((path,json)->{
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
        int queued() { synchronized(writer){return journalState.validation$pending().size();} }
        CompletableFuture<Boolean> latest(){return mapState.validation$lastBoundsWrite();}
        void change(int x) {
            maps.recordBlockChange(new BlockPos(x,100,4096));
            mapState.validation$flushChangedBlockBounds();
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
                    require(maps.waitingForBoundsJournal()&&maps.streamingChangedChunks().isEmpty()&&mapState.validation$completedSlices()==0,
                            "no world clearing/pasting before recovery write completes");
                    if(tick-heldAt<15)return false;
                    select("61",false);selectionGeneration=mapState.validation$preparationGeneration();phase=6;
                }
                case 6 -> {
                    require(maps.streamingChangedChunks().isEmpty(),"superseded loading cannot mutate blocks while IO held");
                    require(queued()<=2,"rapid map selection keeps one pending snapshot per dimension");
                    if(!maps.waitingForBoundsJournal())return false;
                    require(mapState.validation$preparationGeneration()==selectionGeneration,"latest preparation stays authoritative");
                    maps.recordBlockChange(LATE_CHANGE);
                    gate.release.countDown();enteredAt=tick;phase=7;
                }
                case 7 -> {
                    if(!observedFirstSlice && mapState.validation$completedSlices()>0 && maps.busy()) {
                        require("61".equals(mapState.validation$pendingMap()),"discarded journal continuation never places the old map");
                        require(contains(LATE_CHANGE.getX()),"outer writes arriving during IO committed before first placement slice");
                        observedFirstSlice = true;
                    }
                    if(!game.selectedMapReady())return false;
                    require(observedFirstSlice && game.config().mapId.equals("61") && tick>enteredAt,
                            "latest selection alone places slices and becomes ready");
                    require(!maps.arenaTerrainAt(LATE_CHANGE) && !contains(LATE_CHANGE.getX()),
                            "finished placement retires the cleared outer bound from memory and the durable journal");
                    gate=null;failNext.set(true);select(oldMap,oldBroken);phase=8;
                }
                case 8 -> {
                    if(maps.busy())return false;
                    require(!game.selectedMapReady()&&maps.streamingChangedChunks().isEmpty()&&mapState.validation$completedSlices()==0,
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
        public void close() { if(gate!=null)gate.release.countDown();gate=null;journalState.validation$sink(original); }
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
