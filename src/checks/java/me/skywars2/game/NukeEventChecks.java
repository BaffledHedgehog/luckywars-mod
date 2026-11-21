package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Bounded real-tick nuclear warning/beacon/schedule trial; deliberately never reaches detonation. */
public final class NukeEventChecks {
    private NukeEventChecks(){}
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Nuclear event: "+why);}
    private static java.lang.reflect.Field field(Object object,String name){for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{
        var f=type.getDeclaredField(name);f.setAccessible(true);return f;
    }catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing nuclear fixture field "+name);}
    private static Object read(Object object,String name){try{return field(object,name).get(object);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    private static boolean key(Component text,String key){if(text.getContents() instanceof TranslatableContents contents&&contents.getKey().equals(key))return true;
        for(var child:text.getSiblings())if(key(child,key))return true;return false;}
    private static final Block[] MATERIALS={Blocks.NETHERITE_BLOCK,Blocks.DIAMOND_BLOCK,Blocks.EMERALD_BLOCK,Blocks.GOLD_BLOCK,Blocks.IRON_BLOCK};
    private static final Set<Block> PANES=Set.of(Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.WHITE),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.RED),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.BLACK),
            Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.BLUE),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.LIGHT_BLUE),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.MAGENTA),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.PURPLE),
            Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.YELLOW),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.PINK),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.BROWN),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.ORANGE),
            Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.GREEN),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.LIME),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.CYAN),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.GRAY),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY));
    private static final Set<String> MUSIC=Set.of("nuke","ponos","backrooms","burning_balance","penis_bossfight","kazahstan");
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcNukeEvent service;
        ElementalGrimoireChecks.Fixture base;ServerLevel level;Vec3 first,second;
        final Map<BlockPos,MapManager.BlockSnapshot> frame=new LinkedHashMap<>();final Set<BlockPos> owned=new HashSet<>();
        final Map<java.lang.reflect.Field,Object> bookkeeping=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Object> profile=new LinkedHashMap<>();
        Collection<LbcStonekivi.Work> queue;List<LbcStonekivi.Work> beforeQueue=List.of();
        SauvojenMeditationChecks.Attribution attribution;
        List<LbcNukeEvent.View> frozenViews=List.of();List<Marker> stoppedMarkers=List.of();
        long initialWarnings,initialSchedules,initialBeacons,initialDetonations,initialCenters,initialBlasts,pauseBeacons;
        int start,phase,stageTick,pausePending,oldDeadlineRemaining,secondPackets;
        boolean stoppedByFixture,closed,ownsStop;
        Fixture(GameController game){this.game=game;service=game.items().nukeEvent();}
        @SuppressWarnings("unchecked") void initialize(){
            require(game.playing()&&service.prepared()&&service.markerCount()==0&&service.jobCount()==0&&service.pendingTicks()==-1,"prepared exclusive empty nuclear service");
            for(String name:List.of("actors","unloaded","sources","unloadedChildren"))require(((Map<?,?>)read(service,name)).isEmpty(),"no existing nuclear lineage "+name);
            require(((List<?>)read(service,"visuals")).isEmpty(),"no existing nuclear visual tail");
            queue=(Collection<LbcStonekivi.Work>)read(game.items().stonekivi(),"work");beforeQueue=new ArrayList<>(queue);require(beforeQueue.isEmpty(),"exclusive empty native terrain queue");
            require(!game.items().timeStop().stopped(game.maps().level()),"fixture does not borrow an existing Time Stop");
            for(String name:List.of("warnings","schedules","detonations","centers","blasts","beacons"))bookkeeping.put(field(service,name),read(service,name));
            initialWarnings=service.warnings();initialSchedules=service.schedules();initialBeacons=service.beacons();initialDetonations=service.detonations();initialCenters=service.centers();initialBlasts=service.blasts();
            base=new ElementalGrimoireChecks.Fixture(game,true);base.retain=true;
            var owner=base.owner;attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner));
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);stats.put(stat,owner.getStats().getValue(stat));}
            for(var advancement:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(done::add);advancements.put(advancement,done);}
            try{var data=game.players().get(owner.getUUID());for(var f:data.getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){
                var value=f.get(data);profile.put(f,value instanceof int[] array?array.clone():value instanceof Set<?> set?new HashSet<>(set):value);
            }}catch(IllegalAccessException error){throw new IllegalStateException(error);}
            base.initialize();level=owner.level();acknowledge();first=owner.position().add(3,0,0);second=owner.position().add(-3,0,0);
            require(level.getMaxY()>=319,"native map contains source absolute319 clear boundary");
            for(var at:BlockPos.betweenClosed(owner.blockPosition().offset(-9,-5,-6),new BlockPos(owner.blockPosition().getX()+9,319,owner.blockPosition().getZ()+6))){
                require(level.hasChunkAt(at)&&level.getBlockState(at).isAir(),"resident pre-air nuclear frame through source319");frame.put(at.immutable(),game.maps().snapshot(level,at));
            }
            footprint(first);footprint(second);
            require(!game.items().magic().mechanismSuppressedAt(level,first)&&!game.items().magic().mechanismSuppressedAt(level,second),"isolated mechanical-marker trial outside real TECH");
            base.recording.packets.clear();start=game.server().getTickCount();
            require(service.start(owner,first,false),"actual source nuclear start admitted");
            require(service.markerCount()==1&&service.pendingTicks()==-1,"new native marker waits for actual2tick dispatch");
            var marker=service.markers().getFirst();require(marker.entityTags().containsAll(Set.of("nuke_bruh","rc_1sec","marker_2t","mecha"))&&!marker.entityTags().contains("zelensky"),"actual ordinary warning marker source tags");
        }
        void footprint(Vec3 origin){var at=BlockPos.containing(origin);for(int layer=0;layer<5;layer++){int r=layer+1;for(var pos:BlockPos.betweenClosed(at.offset(-r,-layer,-r),at.offset(r,-layer,r)))owned.add(pos.immutable());}owned.add(at.above());owned.add(at.above(2));}
        void acknowledge(){var owner=base.owner;owner.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            if(read(owner.connection,"awaitingPositionFromClient")!=null){ClientboundPlayerPositionPacket packet=null;
                for(var p:base.recording.packets)if(p instanceof ClientboundPlayerPositionPacket position)packet=position;
                require(packet!=null,"actual native teleport emits acknowledgement payload");owner.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(packet.id(),owner.getX(),owner.getY(),owner.getZ(),owner.getYRot(),owner.getXRot()));}
            require(owner.connection.hasClientLoaded()&&!owner.isChangingDimension(),"native fixture player loaded and acknowledged");
        }
        void warning(int from,boolean zelensky){var packets=base.recording.packets.subList(from,base.recording.packets.size());
            var title=packets.stream().filter(ClientboundSetTitleTextPacket.class::isInstance).map(ClientboundSetTitleTextPacket.class::cast).toList();
            var subtitle=packets.stream().filter(ClientboundSetSubtitleTextPacket.class::isInstance).map(ClientboundSetSubtitleTextPacket.class::cast).toList();
            require(title.size()==1&&title.getFirst().text().getString().equals(" ")&&subtitle.size()==1&&key(subtitle.getFirst().text(),"nuke_lol_activated"),"actual single source blank title and original warning translation");
            if(zelensky)require(subtitle.getFirst().text().getString().startsWith("\u0412\u043e\u043b\u043e\u0434\u0438\u043c\u0438\u0440 \u0417\u0435\u043b\u0435\u043d\u0441\u044c\u043a\u0438\u0439 "),"exact source Zelensky warning name");
            var sounds=packets.stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast)
                    .filter(p->MUSIC.stream().anyMatch(id->p.getSound().value().location().toString().equals("minecraft:lbcsounds."+id))).toList();
            var expectedNames=new ArrayList<String>();var expectedPitch=new ArrayList<Float>();
            for(float pitch:new float[]{1,.5f,1.5f,2}){expectedNames.add("nuke");expectedPitch.add(pitch);}
            for(float pitch:new float[]{1,1.5f,2,.5f}){expectedNames.add("ponos");expectedPitch.add(pitch);}
            for(float pitch:new float[]{1,2,0})for(var name:List.of("backrooms","burning_balance","penis_bossfight","kazahstan")){expectedNames.add(name);expectedPitch.add(pitch);}
            require(sounds.size()==20,"all20 actual source warning packets");for(int i=0;i<20;i++){var sound=sounds.get(i);
                require(sound.getSound().value().location().toString().equals("minecraft:lbcsounds."+expectedNames.get(i))&&sound.getPitch()==expectedPitch.get(i)
                        &&sound.getVolume()==.5f&&sound.getSource()==net.minecraft.sounds.SoundSource.MASTER,"exact native warning sound sequence "+i);}
        }
        boolean beaconReady(){var at=BlockPos.containing(first);return level.getBlockState(at.above()).is(Blocks.BEACON)&&PANES.contains(level.getBlockState(at.above(2)).getBlock())&&service.jobCount()==0;}
        void beacon(){var at=BlockPos.containing(first);for(int layer=0;layer<5;layer++){int r=layer+1;
                for(var pos:BlockPos.betweenClosed(at.offset(-r,-layer,-r),at.offset(r,-layer,r)))require(level.getBlockState(pos).is(MATERIALS[layer]),"actual native five-layer source beacon block "+pos);}
            require(level.getBlockEntity(at.above())!=null&&level.getBlockState(at.above()).is(Blocks.BEACON),"actual native vanilla beacon block entity");
            require(PANES.contains(level.getBlockState(at.above(2)).getBlock()),"native random draw produces one source16 pane");neighbors();
        }
        void neighbors(){for(var e:frame.entrySet())if(!owned.contains(e.getKey()))require(game.maps().snapshot(level,e.getKey()).equals(e.getValue()),"exact neighboring frame outside source writes "+e.getKey());}
        boolean step(){acknowledge();int now=game.server().getTickCount();require(now-start<180,"bounded fixture cancels well before700tick deadline");
            require(service.detonations()==initialDetonations&&service.centers()==initialCenters&&service.blasts()==initialBlasts,"no target selection/terrain nuke/burst before actual global deadline");
            if(phase==0){if(service.warnings()==initialWarnings)return false;
                require(service.warnings()==initialWarnings+1&&service.schedules()==initialSchedules+1&&service.pendingTicks()==700,"actual2tick activation starts one700tick global schedule");
                warning(0,false);phase=1;stageTick=now;return false;}
            if(phase==1){require(service.warnings()==initialWarnings+1,"armed marker does not warn or schedule repeatedly");if(!beaconReady())return false;
                require(service.beacons()>initialBeacons&&now-stageTick<=30,"actual shared20tick phase built source beacon");beacon();
                pausePending=service.pendingTicks();pauseBeacons=service.beacons();frozenViews=service.snapshot();game.items().timeStop().begin(level,40);ownsStop=true;
                require(game.items().timeStop().stopped(level),"real native Time Stop admitted");phase=2;stageTick=now;return false;}
            if(phase==2){require(game.items().timeStop().stopped(level)&&service.pendingTicks()==pausePending&&service.beacons()==pauseBeacons&&service.snapshot().equals(frozenViews),"actual Time Stop pauses global deadline, native Lifetime and beacon phase");
                if(now-stageTick<8)return false;game.items().timeStop().end(level);ownsStop=false;phase=3;stageTick=now;return false;}
            if(phase==3){if(now-stageTick<4)return false;require(service.pendingTicks()==pausePending-(now-stageTick),"native global deadline resumes once per actual server tick");
                oldDeadlineRemaining=service.pendingTicks();secondPackets=base.recording.packets.size();require(service.start(base.owner,second,true),"second actual source marker starts independently");
                require(service.markerCount()==2,"both physical source markers coexist before global replace");phase=4;stageTick=now;return false;}
            if(phase==4){if(service.warnings()==initialWarnings+1)return false;
                require(service.warnings()==initialWarnings+2&&service.schedules()==initialSchedules+2&&service.pendingTicks()==700
                        &&service.pendingTicks()>oldDeadlineRemaining&&service.snapshot().stream().allMatch(LbcNukeEvent.View::armed),"second source activation REPLACES one global deadline with700; both markers remain armed");
                warning(secondPackets,true);stoppedMarkers=service.markers();service.stop();stoppedByFixture=true;
                require(service.markerCount()==0&&service.jobCount()==0&&service.pendingTicks()==-1&&stoppedMarkers.stream().allMatch(Entity::isRemoved),"explicit native stop cancels both source markers, all queued writes and one pending schedule");
                phase=5;stageTick=now;return false;}
            if(phase==5){if(now<=stageTick)return false;require(service.markerCount()==0&&service.jobCount()==0&&service.pendingTicks()==-1&&new ArrayList<>(queue).equals(beforeQueue),"natural terrain scheduler drains cancelled fixture work after stop");neighbors();return true;}
            throw new IllegalStateException("Unknown native nuclear fixture phase "+phase);
        }
        @Override public void close(){if(closed)return;closed=true;Throwable failure=null;
            try{if(ownsStop&&level!=null)game.items().timeStop().end(level);}catch(Throwable error){failure=error;}
            try{if(base!=null){service.stop();stoppedByFixture=true;}if(queue!=null){for(var task:List.copyOf(queue))if(!beforeQueue.contains(task))task.cancel();queue.clear();queue.addAll(beforeQueue);}}
            catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            try{if(level!=null){for(var e:frame.entrySet())if(!game.maps().snapshot(level,e.getKey()).equals(e.getValue()))game.maps().restoreSnapshot(level,e.getKey(),e.getValue());
                for(var e:frame.entrySet())require(game.maps().snapshot(level,e.getKey()).equals(e.getValue()),"all source writes and exact block-entity neighbors restored "+e.getKey());}}
            catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            try{if(base!=null)base.close();if(attribution!=null)attribution.close();if(base!=null){var owner=base.owner;stats.forEach((stat,value)->owner.getStats().setValue(owner,stat,value));
                advancements.forEach((adv,done)->{for(var key:adv.value().criteria().keySet())if(done.contains(key))owner.getAdvancements().award(adv,key);else owner.getAdvancements().revoke(adv,key);});
                var data=game.players().get(owner.getUUID());for(var e:profile.entrySet())e.getKey().set(data,e.getValue());}
                for(var e:bookkeeping.entrySet())e.getKey().set(service,e.getValue());
                if(queue!=null)require(new ArrayList<>(queue).equals(beforeQueue),"exact native shared terrain queue restored");
            }catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Nuclear bounded fixture restoration failed",failure);
        }
    }
    public static void run(GameController game){require(running==null,"exclusive asynchronous nuclear fixture");var fixture=new Fixture(game);running=fixture;
        try{fixture.initialize();SkyWars2.LOGGER.info("[check] START nuke-event (real ticks; cancellation before700tick deadline)");}
        catch(Throwable problem){try{fixture.close();}catch(Throwable cleanup){problem.addSuppressed(cleanup);}finally{running=null;}throw problem;}}
    public static void tick(GameController game){if(running==null)return;try{if(running.step()){running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS nuke-event (native source marker/warning20sounds,20tick beacon and real block entity/pane, global700tick schedule replacement,8tick TimeStop pause/resume and explicit stop; exact terrain/queue/player restoration; NO full deadline/detonation/epoch/performance qualification)");}}
        catch(Throwable problem){try{running.close();}catch(Throwable cleanup){problem.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL nuke-event",problem);}}
    public static void cancel(GameController game){if(running==null)return;try{running.close();}finally{running=null;}}
}
