package me.skywars2.game;

import java.security.MessageDigest;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Real media and scheduled native input/coordinator checks; no physical-client or audio timing claim. */
public final class BadAppleChecks {
    private BadAppleChecks() { }
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Bad Apple: "+why);}
    public static void run(GameController game){start(game,false);}
    public static void runRewind(GameController game){start(game,true);}
    private static void start(GameController game,boolean rewind){
        require(running==null,"exclusive scheduled fixture");media();var fixture=new Fixture(game,rewind);running=fixture;
        try{fixture.begin();}catch(Throwable error){running=null;try{fixture.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game){
        if(running==null)return;var fixture=running;
        try{require(game==fixture.game,"same fixture controller");if(!fixture.step())return;running=null;fixture.close();SkyWars2.LOGGER.info("[check] PASS {} (source BAF independent hashes/bits, real typed loot and scheduled drop/display; native queue and typed coordinator checks)",fixture.name());}
        catch(Throwable error){running=null;try{fixture.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL "+fixture.name(),error);}
    }
    /** Expected values came from a separate Python decoder of the read-only legacy file. */
    static void media(){
        var media=LbcBadAppleMedia.instance();require(media.width()==228&&media.height()==171&&media.frameCount()==2191&&media.frameBytes()==4874&&media.pixelCount()==38988,"exact finite source dimensions");
        int[] indices={0,1,100,1095,2190};
        String[] hashes={"096fa0bd19ca328749d311c84ba3d54297c49682557196369fdd1929ae887450","096fa0bd19ca328749d311c84ba3d54297c49682557196369fdd1929ae887450","70210b82d6d4534ddce4c14d1554ef8fca52385eed289bbc8c34f4b25e02658d","ef06ed21aeaa1d4ef0fcc08796947f5a95fed956b866fa5154932341036c688a","096fa0bd19ca328749d311c84ba3d54297c49682557196369fdd1929ae887450"};
        int[] counts={0,0,22727,5628,0};
        for(int n=0;n<indices.length;n++){
            var frame=media.newFrame(indices[n]);require(sha(frame).equals(hashes[n]),"independent absolute source frame hash "+indices[n]);int white=0;for(int bit=0;bit<media.pixelCount();bit++)if(media.white(frame,bit))white++;
            require(white==counts[n]&&(frame[frame.length-1]&0xf0)==0,"independent pixel count/padding "+indices[n]);
        }
        var sample=media.newFrame(1095);require(!media.white(sample,16647)&&media.white(sample,16648)&&media.white(sample,38934)&&!media.white(sample,38935),"independent LSB row-major boundary bits");
        var firstPaint=media.newFrame(14);require(sha(firstPaint).equals("2b452ab789583a4c8a5d5e7aeba8f192e8d942149d2c117169f2234ea2e720c0")&&media.white(firstPaint,17555)&&!media.white(firstPaint,17554)&&!media.white(media.newFrame(116),17555),"independent first nonempty frame14/column and later black transition used by supporting checks");
        var moving=media.newFrame(100);media.moveFrame(100,1095,moving);require(Arrays.equals(moving,sample),"forward XOR seek");media.moveFrame(1095,100,moving);require(sha(moving).equals(hashes[2]),"reverse XOR seek");
        var detached=media.newFrame(100);detached[0]^=127;require(sha(media.newFrame(100)).equals(hashes[2]),"caller buffers cannot mutate immutable source");
    }
    private static String sha(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
    private static java.lang.reflect.Field field(Object target,String name){try{var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f;}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    private static Object get(Object target,String name){try{return field(target,name).get(target);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    private static void put(Object target,String name,Object value){try{field(target,name).set(target,value);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    /** A controlled logical clock avoids waiting for a whole clip; media and work remain production objects. */
    private static LbcStonekivi.Work seek(LbcBadApple service,int frame){var current=get(service,"current");require(current!=null,"native playback exists before logical seek");put(current,"logical",frame*2);put(current,"nextDesired",frame);return (LbcStonekivi.Work)current;}

    private static final class Fixture implements AutoCloseable {
        final GameController game;final boolean rewind;final ServerPlayer owner,other;final ServerLevel level;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;final RegenerationBeltChecks.ScheduledClient client,audience;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();final Set<UUID> before=new HashSet<>();
        final LbcBadApple service;final LbcBadApple.Policy originalPolicy;
        final Vec3 origin;final int started;boolean closed,stopped,restoring;int stage,when;
        ItemEntity token;UUID display;LbcBadApple.View captured;Object capturedPlayback;int capturedSound;boolean skills;Map<BlockPos,BlockState> capturedTerrain;
        LbcBadApple supporting;BlockPos testTop;long firstWrites;int peerProgress,supportStage;Object previousPlayback;
        Fixture(GameController game,boolean rewind){
            this.game=game;this.rewind=rewind;owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));other=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));level=owner.level();
            require(game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two real active participants");
            require(!game.vanity().busy()&&!game.vanity().hasSnapshot()&&!game.items().timeStop().stopped(level),"exclusive whole coordinator and source clock");
            service=game.items().badApple();require(service.count()==0&&!service.view().running(),"no unrelated Bad Apple owner");originalPolicy=(LbcBadApple.Policy)get(service,"policy");
            saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,other);
            level.getAllEntities().forEach(e->before.add(e.getUUID()));origin=new Vec3(owner.getX(),Math.min(240,level.getMaxY()-40),owner.getZ());started=game.server().getTickCount();
        }
        String name(){return rewind?"badapple-rewind":"badapple-imba";}
        void begin(){
            skills=game.config().lbcSkills;game.config().lbcSkills=false;
            for(var p:List.of(owner,other)){game.lbc().clear(p);p.closeContainer();p.getInventory().clearContent();p.removeAllEffects();p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);}
            require(owner.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true),"native scene teleport");client.acknowledge();
            // This transparent observer preserves the installed policy and saves precisely the native writes for cleanup.
            put(service,"policy",new LbcBadApple.Policy(){
                public boolean admittedLaunch(ServerPlayer p,ServerLevel l,Vec3 at){return originalPolicy.admittedLaunch(p,l,at);}
                public boolean admittedCell(ServerPlayer p,ServerLevel l,BlockPos at){return originalPolicy.admittedCell(p,l,at);}
                public boolean write(ServerPlayer p,ServerLevel l,BlockPos at,BlockState state,LbcIndirectKills.Owner cause){require(l==level,"fixture projects into original world");blocks.putIfAbsent(at.immutable(),game.maps().snapshot(l,at));return originalPolicy.write(p,l,at,state,cause);}
            });
            var loot=game.lbc().rollTable(level,origin,owner,"lbc:ported/bad_apple_imba");require(loot.size()==1&&loot.getFirst().getCount()==1&&LbcBadApple.recognized(loot.getFirst()),"actual canonical ready typed IMBA loot");
            require(loot.getFirst().is(LbcBadAppleItem.TOKEN),"actual native typed item singleton");
            require(net.minecraft.resources.Identifier.parse("lbc:badapple1").equals(loot.getFirst().get(net.minecraft.core.component.DataComponents.ITEM_MODEL))&&net.minecraft.resources.Identifier.parse("lbc:tier_k").equals(loot.getFirst().get(net.minecraft.core.component.DataComponents.TOOLTIP_STYLE)),"actual source model and tier survive typed acquisition");
            require(!LbcBadApple.recognized(new ItemStack(Items.APPLE)),"ordinary edible apple is separate");
            token=new ItemEntity(level,origin.x+2,origin.y,origin.z,loot.getFirst());token.setNoGravity(true);token.setDeltaMovement(Vec3.ZERO);token.setPickUpDelay(32767);token.setThrower(owner);require(level.addFreshEntity(token),"actual dropped source publication");next(0);
        }
        void next(int value){stage=value;when=game.server().getTickCount();}
        int soundCount(){@SuppressWarnings("unchecked") var packets=(List<net.minecraft.network.protocol.Packet<?>>)get(get(client,"connection"),"packets");int n=0;for(var packet:packets)if(packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket sound&&sound.getSound().value().location().toString().equals("minecraft:lbcsounds.bad_apple"))n++;return n;}
        void pause(){game.items().timeStop().begin(level,10000);stopped=true;}
        void thaw(){game.items().timeStop().end(level);stopped=false;}
        boolean step(){
            client.acknowledge();audience.acknowledge();require(game.server().getTickCount()-started<800,"bounded scheduled fixture stage="+stage);int elapsed=game.server().getTickCount()-when;
            if(restoring&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            switch(stage){
                case 0->{if(!LbcBadApple.managed(token)){require(elapsed<30,"scheduled actual drop dispatch");return false;}require(service.count()==1&&token.getItem().getCount()==1,"entry owns full original body once");next(1);}
                case 1->{
                    if(token.isRemoved()){require(elapsed>=45,"source display waits for age60");var bodies=service.snapshot();require(bodies.size()==1&&bodies.getFirst() instanceof net.minecraft.world.entity.Display.ItemDisplay,"native display replaces original item");display=bodies.getFirst().getUUID();next(2);return false;}
                    if(elapsed>=12)require(token.isNoGravity()&&token.isCurrentlyGlowing(),"source age10 floating glow");
                    require(elapsed<70,"bounded source60 drop aging");
                }
                case 2->{
                    if(!service.view().running()){require(elapsed<8,"display starts installed singleton");return false;}
                    require(level.getEntity(display)==null&&service.count()==0,"display consumed once by playback owner");
                    require(service.view().soundStarted()&&service.view().writes()==0&&soundCount()==1,"source baseline latches exactly one song without invented initial paint");
                    var work=seek(service,100);for(int n=0;n<32&&(service.view().acceptedFrame()<0||service.view().columnCursor()==0);n++)work.step();
                    require(service.view().acceptedFrame()==100&&service.view().columnCursor()>0&&service.view().columnCursor()<38988,"actual full-media accepted pending column cursor");
                    pause();captured=service.view();capturedPlayback=get(service,"current");capturedTerrain=new LinkedHashMap<>();for(var p:blocks.keySet())capturedTerrain.put(p,level.getBlockState(p));capturedSound=soundCount();
                    if(rewind){require(game.vanity().requestCapture(owner,false),"whole coordinator accepts actual nonempty movie/cursor");next(10);}else next(3);
                }
                case 3->{require(service.view().equals(captured),"real TimeStop freezes movie clock and pending accepted writes");if(elapsed<4)return false;thaw();service.stop();support();next(20);}
                case 10->{require(service.view().equals(captured),"capture pause retains exact accepted cursor and latch");if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;thaw();next(11);}
                case 11->{if(service.view().columnCursor()==captured.columnCursor()){require(elapsed<10,"future native accepted work advances");return false;}pause();require(game.vanity().requestRestoreForCheck(),"whole coordinator restores actual pending movie");restoring=true;next(12);}
                case 12->{
                    if(game.vanity().busy())return false;require(service.view().equals(captured)&&get(service,"current")==capturedPlayback,"typed coordinator restores private logical/frame/cursor/latch and owner identity");
                    for(var e:capturedTerrain.entrySet())require(level.getBlockState(e.getKey()).equals(e.getValue()),"actual pre-capture painted terrain restored");
                    for(var e:blocks.entrySet())if(!capturedTerrain.containsKey(e.getKey()))require(level.getBlockState(e.getKey()).equals(e.getValue().state()),"whole coordinator removes future-only terrain writes");
                    require(game.items().timeStop().stopped(level),"whole coordinator retains source pause");require(soundCount()==capturedSound+1,"restore publication restarts one native song packet from zero (no seek-offset claim)");capturedSound=soundCount();game.vanity().stop();thaw();next(13);
                }
                case 13->{require(service.view().soundStarted()&&soundCount()==capturedSound,"replay retains first-sound latch without another sound publication");if(service.view().columnCursor()==captured.columnCursor()){require(elapsed<10,"replayed accepted writes resume from pending cursor");return false;}return true;}
                case 20->{return supportStep(elapsed);}
                default->throw new IllegalStateException("Bad Apple fixture stage "+stage);
            }
            return false;
        }
        void support(){
            // A separate real-media kernel uses a narrow policy; it does not replace the actual item path above.
            testTop=new BlockPos(113,(int)origin.y,-9);require(level.hasChunkAt(testTop),"resident independent source frame14 column");
            for(int dy=0;dy<=12;dy++)set(testTop.below(dy),dy%3==0?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
            supporting=new LbcBadApple(game,new LbcBadApple.Policy(){
                public boolean admittedLaunch(ServerPlayer p,ServerLevel l,Vec3 at){return true;}
                public boolean admittedCell(ServerPlayer p,ServerLevel l,BlockPos at){return at.getX()==testTop.getX()&&at.getZ()==testTop.getZ()&&at.getY()>=testTop.getY()-12;}
                public boolean write(ServerPlayer p,ServerLevel l,BlockPos at,BlockState state,LbcIndirectKills.Owner cause){set(at,state);return true;}
            });
            require(supporting.start(owner,level,new Vec3(0,origin.y,0),game.items().indirectKills().wildCause(owner)),"supporting source-centered movie start");supporting.tick();
            require(supporting.view().soundStarted()&&supporting.view().writes()==0,"supporting baseline publishes no terrain");
            var work=seek(supporting,14);for(int n=0;n<150&&supporting.view().writes()==0;n++)work.step();
            require(supporting.view().acceptedFrame()==14&&supporting.view().writes()==4,"first known nonzero source frame paints exactly four floors");
            for(int dy:List.of(0,3,6,9))require(level.getBlockState(testTop.below(dy)).is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE)),"source four separated surfaces white "+dy);
            require(level.getBlockState(testTop.below(12)).is(Blocks.STONE),"fifth surface excluded");
            previousPlayback=get(supporting,"current");require(supporting.start(owner,level,new Vec3(0,origin.y,0),game.items().indirectKills().wildCause(owner)),"singleton restart accepts successor");
            require(!supporting.view().soundStarted(),"successor awaits accepted predecessor writes");supportStage=0;
        }
        boolean supportStep(int elapsed){
            require(elapsed<150,"bounded supporting finite-media stages "+supportStage);
            if(supportStage==0){
                if(!(boolean)get(previousPlayback,"closed"))return false;supporting.tick();require(supporting.view().soundStarted()&&supporting.view().logicalTicks()==0,"singleton drains prior accepted frame before fresh baseline");
                seek(supporting,14);supportStage=1;return false;
            }
            if(supportStage==1){
                if(supporting.view().appliedFrame()!=14)return false;
                // Change floors inside this same playback, then use its next independently known black transition.
                set(testTop,Blocks.AIR.defaultBlockState());set(testTop.below(3),Blocks.STONE.defaultBlockState());set(testTop.below(6),Blocks.STONE.defaultBlockState());set(testTop.below(9),Blocks.STONE.defaultBlockState());
                seek(supporting,116);supportStage=4;return false;
            }
            if(supportStage==4){
                if(supporting.view().appliedFrame()!=116)return false;
                require(level.getBlockState(testTop).isAir(),"changed native top remains removed");
                for(int dy:List.of(3,6,9,12))require(level.getBlockState(testTop.below(dy)).is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK)),"same-playback fresh changed-column scan finds new four floors "+dy);
                supporting.requestStop();supportStage=2;return false;
            }
            if(supportStage==2){
                if(supporting.view().running()||get(supporting,"current")!=null)return false;
                require(level.getBlockState(testTop.below(12)).is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK)),"ordinary stop preserves completed terrain paint");
                int chunkX=60000;while(level.getChunkSource().getChunkNow(chunkX,60000)!=null)chunkX+=128;
                var launch=new Vec3(chunkX*16+114,origin.y,60000*16+85);require(supporting.start(owner,level,launch,game.items().indirectKills().wildCause(owner)),"unresident supporting movie accepts native preparation");supporting.tick();var work=seek(supporting,14);
                for(int n=0;n<60&&supporting.view().acceptedFrame()<0;n++)work.step();
                require(supporting.view().acceptedFrame()==14,"unresident first changed frame accepted before chunk work");
                int cursor=supporting.view().columnCursor();for(int n=0;n<60&&supporting.view().columnCursor()<17555;n++)work.step();
                require(supporting.view().columnCursor()==17555,"unresident changed pixel remains accepted and unadvanced");
                game.items().stonekivi().queuedWork(new LbcStonekivi.Work(){public boolean step(){peerProgress++;return true;}});supportStage=3;return false;
            }
            require(peerProgress==1,"shared queue serves independent work while accepted movie waits for native chunks");
            if(supporting.view().appliedFrame()!=14)return false;
            require(supporting.view().acceptedFrame()<0,"accepted unloaded frame drains rather than being dropped");supporting.requestStop();return true;
        }
        void set(BlockPos p,BlockState state){blocks.putIfAbsent(p.immutable(),game.maps().snapshot(level,p));require(level.setBlock(p,state,818)||level.getBlockState(p).equals(state),"fixture native terrain write");}
        @Override public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(()->{if(rewind)game.vanity().stop();},()->{service.stop();if(supporting!=null)supporting.stop();put(service,"policy",originalPolicy);},()->{if(stopped)game.items().timeStop().end(level);},
                    ()->{for(var e:level.getAllEntities())if(!before.contains(e.getUUID())&&e instanceof ItemEntity)e.discard();},
                    ()->{for(var entry:blocks.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue(),818);},
                    ()->{game.config().lbcSkills=skills;saved.restoreAttributes();otherSaved.restoreAttributes();saved.restore(game);otherSaved.restore(game);saved.restoreUse();otherSaved.restoreUse();},client::close,audience::close))
                try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Bad Apple scoped restoration",failure);
        }
    }
}
