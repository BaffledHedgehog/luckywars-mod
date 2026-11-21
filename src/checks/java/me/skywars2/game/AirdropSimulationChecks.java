package me.skywars2.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import me.skywars2.game.match.checkpoints.MatchPlayerCheckpoint;
import me.skywars2.game.world.maps.MapManager;
import me.skywars2.game.world.maps.NativeChunkLoad;
import me.skywars2.validation.mixin.GameCheckAccess;
import me.skywars2.validation.mixin.NativeFixtureAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.Ticket;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Actual stage-two airdrops in a resident column with no player simulation ticket. */
final class AirdropSimulationChecks {
    private static Fixture active;
    private static final Identifier FLIGHT=Identifier.parse("skywars2:airdrop_flight");
    private static final Identifier TERRAIN=Identifier.parse("skywars2:map_loading");

    static void run(GameController game) {
        require(active==null,"exclusive fixture");
        var fixture=new Fixture(game);
        try { fixture.begin();active=fixture; }
        catch(Throwable error) {
            try { fixture.close(); } catch(Throwable cleanup) { error.addSuppressed(cleanup); }
            throw error;
        }
    }

    static void tick(GameController game) {
        var fixture=active;
        if(fixture==null||fixture.game!=game)return;
        try {
            if(!fixture.step())return;
            active=null;fixture.close();
            SkyWars2.LOGGER.info("[check] PASS airdrop-simulation (native stage-two graphs, no arena players, shared level31 ticket, natural landing, full airborne Vanity replay and canonical stop release)");
        } catch(Throwable error) {
            active=null;
            try { fixture.close(); } catch(Throwable cleanup) { error.addSuppressed(cleanup); }
            SkyWars2.LOGGER.error("[check] FAIL airdrop-simulation",error);
        }
    }

    private static void require(boolean test,String why) {
        if(!test)throw new IllegalStateException("Airdrop simulation: "+why);
    }

    private record Flight(Chicken chicken,MinecartChest chest,double startY) {
        Flight(Chicken chicken,MinecartChest chest){this(chicken,chest,chicken.getY());}
        void discard(){if(!chest.isRemoved())chest.discard();if(!chicken.isRemoved())chicken.discard();}
    }

    private record FlightPosition(Flight flight,Vec3 position,int ticks) {
        FlightPosition(Flight flight){this(flight,flight.chicken.position(),flight.chicken.tickCount);}
    }

    private static final class Fixture {
        final GameController game;
        final ServerLevel arena,end;
        final ServerPlayer owner,other;
        final GameChecks.WandThirdPlayerState savedOwner,savedOther;
        final RegenerationBeltChecks.ScheduledClient client,audience;
        final Map<BlockPos,MapManager.BlockSnapshot> pad=new LinkedHashMap<>();
        final List<Flight> owned=new ArrayList<>();
        final ChunkPos column;
        final double angle,radius;
        final int x,z,start;
        CompletableFuture<?> terrainLoading;
        BlockPos support;
        List<Flight> wave;
        List<FlightPosition> captured;
        Flight landing;
        int phase,when;
        boolean checkpoint;

        Fixture(GameController game) {
            this.game=game;arena=game.maps().level();end=Objects.requireNonNull(game.server().getLevel(Level.END));
            owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            other=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            require(game.playing()&&!game.maps().busy()&&arena!=end&&owner.level()==arena&&other.level()==arena
                    &&game.alivePlayers().containsAll(List.of(owner,other))&&pending().isEmpty(),"exclusive stage-two native arena");
            require(!game.vanity().busy()&&!game.vanity().hasSnapshot()&&!game.items().timeStop().stopped(arena),
                    "exclusive native Time Stop and Vanity replay");
            require(game.server().getPlayerList().getPlayers().size()==2,"two players and no unrelated simulation center");
            savedOwner=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
            savedOther=new GameChecks.WandThirdPlayerState(other,game.items().mana());
            double centerX=game.maps().spawns().stream().mapToDouble(spawn->spawn.position().x).average().orElse(0);
            double centerZ=game.maps().spawns().stream().mapToDouble(spawn->spawn.position().z).average().orElse(0);
            // Stay inside the actual event radius and away from permanent spawn-region tickets.
            column=new ChunkPos(((int)Math.floor(centerX)>>4)+6,(int)Math.floor(centerZ)>>4);
            x=column.getMinBlockX()+8;z=column.getMinBlockZ()+8;
            double dx=x+.5-centerX,dz=z+.5-centerZ;
            double azimuth=Math.atan2(dz,dx)/(2*Math.PI);
            angle=azimuth<0?azimuth+1:azimuth;radius=Math.hypot(dx,dz)/192;
            start=game.server().getTickCount();
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);
            audience=new RegenerationBeltChecks.ScheduledClient(game,other);
        }

        Map<Chicken,ServerLevel> pending(){
            return ((GameCheckAccess.Airdrops)(Object)game.events()).validation$pending();
        }

        void begin() {
            terrainLoading=game.maps().prepareChunk(arena,column);
            for(var player:List.of(owner,other)){
                require(player.teleportTo(end,.5,160,.5,Set.of(),0,0,true),"actual native transfer outside the arena");
                player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);
            }
            client.acknowledge();audience.acknowledge();
        }

        List<Ticket> tickets(Identifier type) {
            var storage=((NativeFixtureAccess.Chunks)(Object)arena.getChunkSource()).skywars2$validationTicketStorage();
            return storage.getTickets(column.pack()).stream()
                    .filter(ticket->type.equals(BuiltInRegistries.TICKET_TYPE.getKey(ticket.getType()))).toList();
        }

        void prepareSupport() {
            int y=180;
            for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++)
                y=Math.max(y,arena.getHeight(Heightmap.Types.MOTION_BLOCKING,x+dx,z+dz)+1);
            support=new BlockPos(x,y,z);
            require(y+23<=arena.getMaxY(),"controlled support and ordinary 21-block fall fit the native world");
            for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++){
                var cell=support.offset(dx,0,dz);
                pad.put(cell,game.maps().snapshot(arena,cell));
                require(arena.setBlock(cell,Blocks.STONE.defaultBlockState(),2|16|32),"native supported landing pad cell "+cell);
            }
            require(support.equals(game.events().airdropSurface(x,z)),"actual heightmap selects the controlled support");
        }

        void stageAirdrops() {
            require(pending().isEmpty(),"no unrelated pending airdrops before stage event");
            var original=arena.getRandom();
            class GeometryRandom extends ItemFixtureChecks.DelegatingRandom {
                int draws;
                GeometryRandom() { super(original); }
                @Override public double nextDouble() { return draws++%2==0 ? angle : radius; }
            }
            // Fix only the two geometry draws. All native loot and entity randomness remains normal.
            var controlled=new GeometryRandom();
            try { ItemFixtureChecks.random(arena,controlled);game.events().stageTwo(8); }
            finally { ItemFixtureChecks.random(arena,original); }
            var flights=new ArrayList<Flight>();
            for(var chicken:pending().keySet()){
                require(chicken.chunkPosition().equals(column)&&chicken.getPassengers().size()==1
                        &&chicken.getPassengers().getFirst() instanceof MinecartChest,"stage prepared the real mounted airdrop in the chosen column");
                flights.add(new Flight(chicken,(MinecartChest)chicken.getPassengers().getFirst()));
            }
            var expectedLoot=MapManager.chestLootTable(game.gameMode(),"center",false);
            for(var flight:flights)require(expectedLoot.equals(flight.chest.getContainerLootTable()),"airdrop uses the active mode's center table: mode="+game.gameMode());
            wave=List.copyOf(flights);owned.addAll(wave);
            require(wave.size()==10&&controlled.draws==20,"ten native graphs use their real supported-position callback");
            when=game.server().getTickCount();
        }

        boolean descending(int now) {
            if(!pending().isEmpty()){
                require(now-when<60,"already loaded native graph must publish");
                return false;
            }
            for(var flight:wave){
                var chicken=flight.chicken;
                require(arena.getEntity(chicken.getUUID())==chicken&&arena.getEntity(flight.chest.getUUID())==flight.chest
                        &&flight.chest.getVehicle()==chicken&&!chicken.isNoAi()&&!chicken.isNoGravity(),
                        "same normal chicken and mounted native chest were published");
                if(game.gameMode()==2){
                    boolean lucky=false;
                    for(int slot=0;slot<flight.chest.getContainerSize();slot++)
                        lucky|=Identifier.parse("skywars2:lucky_block").equals(BuiltInRegistries.ITEM.getKey(flight.chest.getItem(slot).getItem()));
                    require(lucky,"each Lucky SkyWars airdrop materializes actual lucky blocks");
                }
            }
            if(wave.stream().anyMatch(flight->flight.chicken.tickCount<4||flight.startY-flight.chicken.getY()<.5)){
                require(now-when<35,"published normal chickens hover without native simulation: ticks="+wave.getFirst().chicken.tickCount
                        +" deltaY="+(wave.getFirst().startY-wave.getFirst().chicken.getY())
                        +" full="+(arena.getChunkSource().getChunkNow(column.x(),column.z())!=null)
                        +" entityTicking="+arena.isPositionEntityTicking(support));
                return false;
            }
            var flightTickets=tickets(FLIGHT);
            require(flightTickets.size()==1&&flightTickets.getFirst().getTicketLevel()==31
                    &&arena.isPositionEntityTicking(support),"ten native descending carriers share one level31 flight ticket");
            return true;
        }

        boolean step() {
            client.acknowledge();audience.acknowledge();int now=game.server().getTickCount();
            require(now-start<1050,"bounded real fixture phase="+phase+" stage="+game.stage());
            if(phase<8)require(owner.level()==end&&other.level()==end,"no player approaches the arena flight");
            if(phase==0){
                if(!terrainLoading.isDone()||arena.getChunkSource().getChunkNow(column.x(),column.z())==null
                        ||arena.isPositionEntityTicking(new BlockPos(x,180,z)))return false;
                require(!NativeChunkLoad.failed(terrainLoading)&&tickets(TERRAIN).size()==1&&tickets(FLIGHT).isEmpty(),
                        "before the event the arena column is FULL with only a map loading lease");
                prepareSupport();stageAirdrops();phase=1;return false;
            }
            if(phase==1){
                if(!descending(now))return false;
                landing=wave.getFirst();
                for(var flight:wave.subList(1,wave.size()))flight.discard();
                phase=2;when=now;return false;
            }
            if(phase==2){
                var chicken=landing.chicken;
                require(!chicken.isRemoved()&&landing.chest.getVehicle()==chicken,"remaining native pair survives peer removal and descent");
                if(!chicken.onGround()){
                    require(now-when<260&&tickets(FLIGHT).size()==1,"final airborne carrier retains the shared lease");
                    return false;
                }
                require(Math.abs(chicken.getY()-(support.getY()+1))<.01,"natural vanilla contact with the actual stone pad");
                if(!tickets(FLIGHT).isEmpty()){
                    require(now-when<265,"landed carrier releases its temporary simulation lease");
                    return false;
                }
                require(tickets(TERRAIN).size()==1&&arena.getChunkSource().getChunkNow(column.x(),column.z())!=null,
                        "landing leaves the independent FULL terrain lease intact");
                landing.discard();stageAirdrops();phase=3;return false;
            }
            if(phase==3){
                if(!descending(now))return false;
                require(wave.stream().noneMatch(flight->flight.chicken.onGround()),"capture the second native wave during flight");
                captured=wave.stream().map(FlightPosition::new).toList();
                game.items().timeStop().begin(arena,10000);checkpoint=true;
                require(game.vanity().requestCapture(owner,false),"actual whole-match coordinator captures flying pairs while the arena is stopped");
                phase=4;return false;
            }
            if(phase==4){
                require(captured.stream().allMatch(saved->saved.flight.chicken.position().equals(saved.position)
                        &&saved.flight.chicken.tickCount==saved.ticks),"real arena Time Stop holds the native flight state during capture");
                if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;
                wave.forEach(Flight::discard);phase=5;return false;
            }
            if(phase==5){
                if(!tickets(FLIGHT).isEmpty())return false;
                require(wave.stream().allMatch(flight->flight.chicken.isRemoved()&&flight.chest.isRemoved()),
                        "post-capture native teardown removes the abandoned flight graph and its lease");
                require(game.vanity().requestRestoreForCheck(),"actual whole-match Vanity rewind accepted");
                phase=6;return false;
            }
            if(phase==6){
                for(var player:game.server().getPlayerList().getPlayers())
                    if(player.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
                if(game.vanity().busy())return false;
                for(var saved:captured){
                    var flight=saved.flight;
                    require(arena.getEntity(flight.chicken.getUUID())==flight.chicken
                            &&arena.getEntity(flight.chest.getUUID())==flight.chest&&flight.chest.getVehicle()==flight.chicken
                            &&flight.chicken.position().equals(saved.position)&&flight.chicken.tickCount==saved.ticks,
                            "canonical rewind restores the same native pair, mounted link, flight position and ticker");
                }
                var restoredTickets=tickets(FLIGHT);
                require(restoredTickets.size()==1&&restoredTickets.getFirst().getTicketLevel()==31
                        &&game.items().timeStop().stopped(arena),"restored paused flight recovers its one native level31 lease");
                game.vanity().stop();game.items().timeStop().end(arena);checkpoint=false;
                phase=7;when=now;return false;
            }
            if(phase==7){
                if(captured.stream().anyMatch(saved->saved.flight.chicken.tickCount<=saved.ticks
                        ||saved.flight.chicken.getY()>=saved.position.y)){
                    require(now-when<20,"restored native airdrop resumes falling without any arena players");
                    return false;
                }
                require(wave.stream().noneMatch(flight->flight.chicken.onGround()),"replayed wave remains airborne at the stop boundary");
                restorePad();game.stopMatch();
                require(!game.playing()&&tickets(FLIGHT).isEmpty(),"canonical stop releases the still-airborne native simulation ticket");
                phase=8;return false;
            }
            return game.inLobby()&&tickets(FLIGHT).isEmpty();
        }

        void restorePad(){
            for(var entry:pad.entrySet())game.maps().restoreSnapshot(arena,entry.getKey(),entry.getValue());
            pad.clear();
        }

        void restorePlayer(GameChecks.WandThirdPlayerState saved) {
            if(saved.player.level()!=arena){
                var position=saved.position;
                require(saved.player.teleportTo(arena,position.x,position.y,position.z,Set.of(),saved.yaw,saved.pitch,true),
                        "fixture failure restores the original arena dimension");
            }
            saved.restore(game);
        }

        void close() {
            try {
                if(checkpoint){
                    game.vanity().stop();
                    if(game.items().timeStop().stopped(arena))game.items().timeStop().end(arena);
                }
                for(var flight:owned){pending().remove(flight.chicken);flight.discard();}
                restorePad();
                if(phase<8&&game.playing()) { restorePlayer(savedOwner);restorePlayer(savedOther); }
                else { owner.setNoGravity(savedOwner.noGravity);other.setNoGravity(savedOther.noGravity); }
            } finally { try { client.close(); } finally { audience.close(); } }
        }
    }
}
