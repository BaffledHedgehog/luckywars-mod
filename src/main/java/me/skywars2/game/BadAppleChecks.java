package me.skywars2.game;

import java.security.MessageDigest;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Real media and scheduled native input/coordinator checks; no physical-client or audio timing claim. */
public final class BadAppleChecks {
    private BadAppleChecks() { }
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Bad Apple: "+why);}
    public static void run(GameController game){start(game,false,false);}
    public static void runRewind(GameController game){start(game,true,false);}
    public static void runTmt(GameController game){start(game,false,true);}
    public static void runTmtRewind(GameController game){start(game,true,true);}
    private static void start(GameController game,boolean rewind,boolean tmt){
        require(running==null,"exclusive scheduled fixture");media();var fixture=new Fixture(game,rewind,tmt);running=fixture;
        try{fixture.begin();}catch(Throwable error){running=null;try{fixture.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game){
        if(running==null)return;var fixture=running;
        try{require(game==fixture.game,"same fixture controller");if(!fixture.step())return;running=null;fixture.close();
            if(fixture.tmt)SkyWars2.LOGGER.info("[check] PASS {} (all5 actual private-source ENTITY_LOAD admissions/rootflag0/unchanged whole bodies; mixed snowball count3/full sibling components through native age60 display/movie; source BAF, terrain/TimeStop/restoration and native queue; playback coordinator rewind only when requested, entry-body rewind not exercised)",fixture.name());
            else SkyWars2.LOGGER.info("[check] PASS {} (source BAF independent hashes/bits, real typed loot and scheduled drop/display; native queue and typed coordinator checks)",fixture.name());}
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
        final GameController game;final boolean rewind,tmt;final ServerPlayer owner,other;final ServerLevel level;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;final RegenerationBeltChecks.ScheduledClient client,audience;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();final Set<UUID> before=new HashSet<>();
        final LbcBadApple service;final LbcBadApple.Policy originalPolicy;
        final Vec3 origin;final int started;boolean closed,stopped,restoring;int stage,when;
        ItemEntity token;ItemStack bodySource;LbcTmtAbilities.Ability privateApple,sibling;UUID display;LbcBadApple.View captured;Object capturedPlayback;int capturedSound,capturedVisual;boolean skills;Map<BlockPos,BlockState> capturedTerrain;
        Map<Long,MapManager.BlockSnapshot> capturedOriginals;
        LbcBadApple supporting;BlockPos testTop;long firstWrites;int peerProgress,supportStage,visualWait;Object previousPlayback;
        Fixture(GameController game,boolean rewind,boolean tmt){
            this.game=game;this.rewind=rewind;this.tmt=tmt;owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));other=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));level=owner.level();
            require(game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two real active participants");
            require(!game.vanity().busy()&&!game.vanity().hasSnapshot()&&!game.items().timeStop().stopped(level),"exclusive whole coordinator and source clock");
            service=game.items().badApple();require(service.count()==0&&!service.view().running(),"no unrelated Bad Apple owner");originalPolicy=(LbcBadApple.Policy)get(service,"policy");
            saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,other);
            level.getAllEntities().forEach(e->before.add(e.getUUID()));origin=new Vec3(owner.getX(),Math.min(240,level.getMaxY()-40),owner.getZ());started=game.server().getTickCount();
        }
        String name(){return tmt?(rewind?"tmt-badapple-rewind":"tmt-badapple"):(rewind?"badapple-rewind":"badapple-imba");}
        void begin(){
            skills=game.config().lbcSkills;game.config().lbcSkills=false;
            for(var p:List.of(owner,other)){game.lbc().clear(p);p.closeContainer();p.getInventory().clearContent();p.removeAllEffects();p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);}
            require(owner.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true),"native scene teleport");client.acknowledge();
            // This transparent observer preserves the installed policy and saves precisely the native writes for cleanup.
            put(service,"policy",new LbcBadApple.Policy(){
                public boolean admittedLaunch(ServerPlayer p,ServerLevel l,Vec3 at){return originalPolicy.admittedLaunch(p,l,at);}
                public boolean admittedCell(ServerPlayer p,ServerLevel l,BlockPos at){return originalPolicy.admittedCell(p,l,at);}
                public boolean write(ServerPlayer p,ServerLevel l,BlockPos at,BlockState state,LbcIndirectKills.Owner cause){require(l==level,"fixture projects into original world");blocks.putIfAbsent(at.immutable(),game.maps().snapshot(l,at));return originalPolicy.write(p,l,at,state,cause);}
                public boolean restore(ServerPlayer p,ServerLevel l,BlockPos at,MapManager.BlockSnapshot original,LbcIndirectKills.Owner cause){return originalPolicy.restore(p,l,at,original,cause);}
            });
            var loot=game.lbc().rollTable(level,origin,owner,"lbc:ported/bad_apple_imba");require(loot.size()==1&&loot.getFirst().getCount()==1&&LbcBadApple.recognized(loot.getFirst()),"actual canonical ready typed IMBA loot");
            require(loot.getFirst().is(LbcBadAppleItem.TOKEN),"actual native typed item singleton");
            require(net.minecraft.resources.Identifier.parse("lbc:badapple1").equals(loot.getFirst().get(net.minecraft.core.component.DataComponents.ITEM_MODEL))&&net.minecraft.resources.Identifier.parse("lbc:tier_k").equals(loot.getFirst().get(net.minecraft.core.component.DataComponents.TOOLTIP_STYLE)),"actual source model and tier survive typed acquisition");
            require(!LbcBadApple.recognized(new ItemStack(Items.APPLE)),"ordinary edible apple is separate");
            if(tmt)tmtAdmissions();
            var source=tmt?mixed(2):loot.getFirst();bodySource=source.copy();token=drop(source,2);next(0);
        }
        ItemEntity drop(ItemStack source,double offset){var item=new ItemEntity(level,origin.x+offset,origin.y,origin.z,source);item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(32767);item.setThrower(owner);require(level.addFreshEntity(item),"actual dropped source publication");return item;}
        ItemStack mixed(int carrier){
            var stack=LbcTmtItems.prototype(carrier);stack.setCount(carrier==2||carrier==4?3:1);
            var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(data,List.of(sibling,privateApple));data.putInt("bad_apple_imba",0);data.putString("badapple_outer_only","complete physical source");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            stack.set(DataComponents.CUSTOM_NAME,Component.literal("whole mixed Bad Apple source "+carrier));stack.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("retained procedural source lore"))));
            var nested=new ItemStack(Items.PAPER,7);nested.set(DataComponents.CUSTOM_NAME,Component.literal("independent retained container"));stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(nested)));
            return stack;
        }
        void whole(ItemEntity item,ItemStack expected){
            require(ItemStack.matches(item.getItem(),expected),"whole native claimed source retains exact physical count/components/private sibling until replacement");
            require(!LbcTmtAbilities.active(item.getItem())&&!ItemData.contains(item.getItem(),LbcTmtAbilities.ACTIVE)&&!LbcTmtAbilities.nativePredicatesActive(),"readonly producer admission leaves no active/projection data or native identity scope");
        }
        void tmtAdmissions(){
            var family=game.items().tmt().catalog().families().stream().filter(row->row.id().equals("native/drop/badapple")).findFirst().orElseThrow();
            require(family.events().equals(Set.of(LbcTmtCatalog.Event.DROP))&&family.locations().equals(Set.of(LbcTmtCatalog.Location.LOOSE_ITEM)),"actual catalog admits Bad Apple as a native loose whole-body producer");
            privateApple=family.variants().stream().filter(property->((ItemData.IntReader)(Object)property.data()).skywars2$readInt("bad_apple_imba",0)==1).findFirst().orElseThrow();
            sibling=game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream()).filter(property->((ItemData.IntReader)(Object)property.data()).skywars2$readInt("zaika",0)==1).findFirst().orElseThrow();
            var probes=new ArrayList<ItemEntity>();int sound=soundCount();require(blocks.isEmpty(),"admission probe starts before any terrain writes");
            for(int carrier=0;carrier<5;carrier++){
                var stack=mixed(carrier);var expected=stack.copy();require(ItemData.intValue(stack,"bad_apple_imba",-1)==0&&LbcBadApple.recognized(stack),"private source alone admits carrier "+carrier+" despite rootflag0");
                var item=drop(stack,4+carrier*2);probes.add(item);require(LbcBadApple.managed(item)&&service.count()==carrier+1&&service.owner(item)==owner,"genuine synchronous ENTITY_LOAD claims exact actual native body/owner "+carrier);whole(item,expected);
            }
            // No private Bad Apple source: even a forged positive aggregate flag cannot start a producer.
            var forged=LbcTmtItems.prototype(0);var data=new CompoundTag();data.putInt("tmt_item",1);LbcTmtAbilities.write(data,List.of(sibling));data.putInt("bad_apple_imba",1);forged.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            var rejected=drop(forged,16);require(!LbcBadApple.recognized(forged)&&!LbcBadApple.managed(rejected)&&service.count()==5,"outer-only forgery is not a private native source");rejected.discard();
            require(!service.view().running()&&service.view().writes()==0&&blocks.isEmpty()&&soundCount()==sound,"all five admission probes initialize no movie/terrain/song before real native aging");
            service.stop();require(probes.stream().allMatch(Entity::isRemoved)&&service.count()==0&&!service.view().running()&&blocks.isEmpty()&&soundCount()==sound,"stop claimed probes before any age/playback; no hidden debit or terrain changes");
        }
        void next(int value){stage=value;when=game.server().getTickCount();}
        int soundCount(){@SuppressWarnings("unchecked") var packets=(List<net.minecraft.network.protocol.Packet<?>>)get(get(client,"connection"),"packets");int n=0;for(var packet:packets)if(packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket sound&&sound.getSound().value().location().toString().equals("minecraft:lbcsounds.bad_apple"))n++;return n;}
        void pause(){game.items().timeStop().begin(level,10000);stopped=true;}
        void thaw(){game.items().timeStop().end(level);stopped=false;}
        boolean step(){
            client.acknowledge();audience.acknowledge();require(game.server().getTickCount()-started<800,"bounded scheduled fixture stage="+stage);int elapsed=game.server().getTickCount()-when;
            if(restoring&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            switch(stage){
                case 0->{if(!LbcBadApple.managed(token)){require(elapsed<30,"scheduled actual drop dispatch");return false;}require(service.count()==1&&token.getItem().getCount()==bodySource.getCount(),"entry owns full original body once");if(tmt)whole(token,bodySource);next(1);}
                case 1->{
                    if(token.isRemoved()){require(elapsed>=45,"source display waits for age60");var bodies=service.snapshot();require(bodies.size()==1&&bodies.getFirst() instanceof net.minecraft.world.entity.Display.ItemDisplay,"native display replaces original item");display=bodies.getFirst().getUUID();next(2);return false;}
                    if(tmt)whole(token,bodySource);
                    if(elapsed>=12)require(token.isNoGravity()&&token.isCurrentlyGlowing(),"source age10 floating glow");
                    require(elapsed<70,"bounded source60 drop aging");
                }
                case 2->{
                    if(!service.view().running()){require(elapsed<8,"display starts installed singleton");return false;}
                    require(level.getEntity(display)==null&&service.count()==0,"display consumed once by playback owner");
                    require(service.view().soundStarted()&&service.view().writes()==0&&soundCount()==1,"source baseline latches exactly one song without invented initial paint");
                    // A nonzero bitmap cursor can visit only unchanged/empty columns. Seed one
                    // real source-white column so world/visual replay owns an actual admitted write.
                    var playback=get(service,"current");var media=LbcBadAppleMedia.instance();var image=media.newFrame(100);int white=0;
                    while(white<media.pixelCount()&&!media.white(image,white))white++;require(white<media.pixelCount(),"source100 has white pixels");
                    var support=new BlockPos((int)get(playback,"minX")+white%media.width(),(int)origin.y-1,(int)get(playback,"minZ")+white/media.width());
                    level.getChunk(support);set(support,Blocks.STONE.defaultBlockState());
                    var work=seek(service,100);for(int n=0;n<512&&service.view().writes()==0;n++)work.step();
                    require(service.view().writes()>0,"actual movie admits seeded source-white terrain write");
                    require(service.view().acceptedFrame()==100&&service.view().columnCursor()>0&&service.view().columnCursor()<38988,"actual full-media accepted pending column cursor");
                    if(rewind){
                        @SuppressWarnings("unchecked") var paint=(Map<Long,BlockState>)get(get(service,"current"),"painted");require(!paint.isEmpty(),"actual movie has owned painted footing");var floor=BlockPos.of(paint.keySet().iterator().next());
                        require(owner.teleportTo(level,floor.getX()+.5,floor.getY()+1,floor.getZ()+.5,Set.of(),0,0,true),"real participant enters saved movie visual");client.acknowledge();owner.setOnGround(true);service.tickVisuals();
                        @SuppressWarnings("unchecked") var views=(Map<UUID,?>)get(service,"visuals");require(views.containsKey(owner.getUUID()),"actual saved movie visual exists");capturedVisual=(int)get(views.get(owner.getUUID()),"elapsed");
                    }
                    pause();captured=service.view();capturedPlayback=get(service,"current");capturedTerrain=new LinkedHashMap<>();for(var p:blocks.keySet())capturedTerrain.put(p,level.getBlockState(p));capturedSound=soundCount();
                    @SuppressWarnings("unchecked") var journal=(Map<Long,MapManager.BlockSnapshot>)get(capturedPlayback,"originals");capturedOriginals=new LinkedHashMap<>(journal);
                    if(rewind){require(game.vanity().requestCapture(owner,false),"whole coordinator accepts actual nonempty movie/cursor");next(10);}else next(3);
                }
                case 3->{require(service.view().equals(captured),"real TimeStop freezes movie clock and pending accepted writes");if(elapsed<4)return false;thaw();service.stop();support();next(20);}
                case 10->{require(service.view().equals(captured),"capture pause retains exact accepted cursor and latch");if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;thaw();next(11);}
                case 11->{if(service.view().columnCursor()==captured.columnCursor()){require(elapsed<10,"future native accepted work advances");return false;}pause();require(game.vanity().requestRestoreForCheck(),"whole coordinator restores actual pending movie");restoring=true;next(12);}
                case 12->{
                    if(game.vanity().busy())return false;require(service.view().equals(captured)&&get(service,"current")==capturedPlayback,"typed coordinator restores private logical/frame/cursor/latch and owner identity");
                    require(get(capturedPlayback,"originals").equals(capturedOriginals),"whole coordinator restores exact original-before-first-paint journal");
                    @SuppressWarnings("unchecked") var views=(Map<UUID,?>)get(service,"visuals");require(views.containsKey(owner.getUUID())&&(int)get(views.get(owner.getUUID()),"elapsed")==capturedVisual,"whole coordinator restores active half-second presentation clock");
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
            set(testTop,Blocks.CHEST.defaultBlockState());var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(testTop);
            chest.setItem(0,new ItemStack(Items.DIAMOND,7));chest.setChanged();
            supporting=new LbcBadApple(game,new LbcBadApple.Policy(){
                public boolean admittedLaunch(ServerPlayer p,ServerLevel l,Vec3 at){return true;}
                public boolean admittedCell(ServerPlayer p,ServerLevel l,BlockPos at){return at.getX()==testTop.getX()&&at.getZ()==testTop.getZ()&&at.getY()>=testTop.getY()-12;}
                public boolean write(ServerPlayer p,ServerLevel l,BlockPos at,BlockState state,LbcIndirectKills.Owner cause){set(at,state);return true;}
                public boolean restore(ServerPlayer p,ServerLevel l,BlockPos at,MapManager.BlockSnapshot original,LbcIndirectKills.Owner cause){game.maps().restoreSnapshot(l,at,original,818);return true;}
            });
            require(supporting.start(owner,level,new Vec3(0,origin.y,0),game.items().indirectKills().wildCause(owner)),"supporting source-centered movie start");supporting.tick();
            require(supporting.view().soundStarted()&&supporting.view().writes()==0,"supporting baseline publishes no terrain");
            var work=seek(supporting,14);for(int n=0;n<150&&supporting.view().writes()==0;n++)work.step();
            require(supporting.view().acceptedFrame()==14&&supporting.view().writes()==4,"first known nonzero source frame paints exactly four floors");
            for(int dy:List.of(0,3,6,9))require(level.getBlockState(testTop.below(dy)).is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE)),"source four separated surfaces white "+dy);
            require(level.getBlockState(testTop.below(12)).is(Blocks.STONE),"fifth surface excluded");
            require(owner.teleportTo(level,testTop.getX()+.5,testTop.getY()+1,testTop.getZ()+.5,Set.of(),0,0,true),"real participant on movie-owned white floor");client.acknowledge();owner.setOnGround(true);supporting.tickVisuals();
            require(owner.getPostEffects().contains(Identifier.parse("lbc:badapple/movie"))&&owner.getPostEffects().contains(Identifier.parse("lbc:badapple/control_1"))&&!owner.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"white projects actual client lightmap without granting authoritative nightvision");
            var removeVision=new net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket(owner.getId(),net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            require(SignedEffectVisuals.projectPacket(owner,removeVision) instanceof net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket effect&&effect.getEffectDurationTicks()==-1,"native self nightvision removal cannot overwrite movie cosmetic projection");
            set(testTop.east(),Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState());
            require(owner.teleportTo(level,testTop.getX()+1.5,testTop.getY()+1,testTop.getZ()+.5,Set.of(),0,0,true),"participant on unrelated white concrete");client.acknowledge();owner.setOnGround(true);supporting.tickVisuals();
            require(SignedEffectVisuals.projectPacket(owner,removeVision)==removeVision&&owner.getPostEffects().contains(Identifier.parse("lbc:badapple/movie")),"unowned concrete releases native lighting but keeps exit blend alive");
            previousPlayback=get(supporting,"current");require(supporting.start(owner,level,new Vec3(0,origin.y,0),game.items().indirectKills().wildCause(owner)),"singleton restart accepts successor");
            require(!supporting.view().soundStarted(),"successor awaits accepted predecessor writes");supportStage=0;
        }
        boolean supportStep(int elapsed){
            supporting.tickVisuals();
            require(elapsed<150,"bounded supporting finite-media stages "+supportStage);
            if(supportStage==0){
                if(elapsed<12)return false;
                require(owner.getPostEffects().stream().noneMatch(id->id.getNamespace().equals("lbc")&&id.getPath().startsWith("badapple/")),"exit chain retires after ten active ticks");
                if(!(boolean)get(previousPlayback,"closed"))return false;
                require(level.getBlockEntity(testTop) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest&&chest.getItem(0).is(Items.DIAMOND)&&chest.getItem(0).getCount()==7,"predecessor restores original block-entity inventory before successor begins");
                supporting.tick();require(supporting.view().soundStarted()&&supporting.view().logicalTicks()==0,"singleton drains prior accepted frame before fresh baseline");
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
                var black=testTop.below(3);require(owner.teleportTo(level,black.getX()+.5,black.getY()+1,black.getZ()+.5,Set.of(),0,0,true),"real participant on movie-owned black floor");client.acknowledge();owner.setOnGround(true);supporting.tickVisuals();
                require(owner.getPostEffects().contains(Identifier.parse("lbc:badapple/control_2")),"black selects lightmap plus inversion target");
                pause();visualWait=game.server().getTickCount();supportStage=5;return false;
            }
            if(supportStage==5){
                @SuppressWarnings("unchecked") var views=(Map<UUID,?>)get(supporting,"visuals");var visual=views.get(owner.getUUID());
                require(visual!=null&&(int)get(visual,"elapsed")==0&&owner.getPostEffects().contains(Identifier.parse("lbc:badapple/control_2_paused")),"TimeStop holds the half-second visual clock and control state");
                if(game.server().getTickCount()-visualWait<3)return false;thaw();owner.setOnGround(false);supporting.requestStop();supportStage=2;return false;
            }
            if(supportStage==2){
                if(supporting.view().running()||get(supporting,"current")!=null)return false;
                require(level.getBlockEntity(testTop) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest&&chest.getItem(0).getCount()==7,"ordinary stop restores original chest metadata");
                for(int dy:List.of(3,6,9,12))require(level.getBlockState(testTop.below(dy)).is(Blocks.STONE),"ordinary stop restores original movie cells "+dy);
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
                    ()->{var actors=new ArrayList<Entity>();for(var e:level.getAllEntities())if(e!=null)actors.add(e);for(var e:actors)if(!before.contains(e.getUUID())&&e instanceof ItemEntity)e.discard();},
                    ()->{for(var entry:blocks.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue(),818);},
                    ()->{game.config().lbcSkills=skills;saved.restoreAttributes();otherSaved.restoreAttributes();saved.restore(game);otherSaved.restore(game);saved.restoreUse();otherSaved.restoreUse();},client::close,audience::close))
                try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Bad Apple scoped restoration",failure);
        }
    }
}
