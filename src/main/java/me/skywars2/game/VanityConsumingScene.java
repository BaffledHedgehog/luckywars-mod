package me.skywars2.game;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

/** Bounded real Consuming children; broad initializers are covered by their ordinary native suites. */
final class VanityConsumingScene {
    final GameController game;final ChefChecks.Fixture base;final LbcConsuming consuming;final LbcStonekivi stone;
    final ServerPlayer initialGamma;
    final BlockPos eraseFirst,eraseLast,pendingCell,futureCell,portalFirst,portalLast,futurePortal,scanFirst,scanLast,pasteFirst,pasteLast;
    final Vec3 impact;
    final Object terrainBatch,pendingBatch,partition,area,erasure,summonState;
    final Object terrainChild,templateChild,silenceChild,disposeChild,avariceChild;
    final BlockAreaWork eraseScan,pendingScan,silenceScan;
    final ItemEntity disposedPrefix,disposedTarget,movedPrefix,movedTarget;
    final Skeleton summon;
    final List<Object> jobs,states;
    List<Object> order;Object corpus,resourceFuture;
    List<BlockPos> portalOrder;int eraseRemaining;
    ServerPlayer currentGamma;

    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Vanity consuming: "+why);}
    private static Field field(Object target,String name){
        for(var type=target.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field;}catch(NoSuchFieldException absent){}
        throw new IllegalStateException("Missing consuming fixture field "+name);
    }
    @SuppressWarnings("unchecked") static <T>T get(Object target,String name){try{return (T)field(target,name).get(target);}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}}
    static void set(Object target,String name,Object value){try{field(target,name).set(target,value);}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}}
    static Object invoke(Object target,String name,Class<?>[] types,Object...args){
        for(var type=target.getClass();type!=null;type=type.getSuperclass())try{var method=type.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(target,args);}
        catch(NoSuchMethodException absent){}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure instanceof InvocationTargetException wrapped?wrapped.getCause():failure);}
        throw new IllegalStateException("Missing consuming fixture method "+name);
    }
    Object create(String name,Object...args){
        try{
            var type=Class.forName(LbcConsuming.class.getName()+"$"+name);var values=new ArrayList<Object>();
            if(!Modifier.isStatic(type.getModifiers()))values.add(consuming);values.addAll(Arrays.asList(args));
            for(var constructor:type.getDeclaredConstructors()){
                var types=constructor.getParameterTypes();if(types.length!=values.size())continue;boolean matches=true;
                for(int i=0;i<types.length;i++)if(values.get(i)!=null&&!types[i].isInstance(values.get(i)))matches=false;
                if(matches){constructor.setAccessible(true);return constructor.newInstance(values.toArray());}
            }
            throw new IllegalStateException("Missing consuming fixture constructor "+name);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure instanceof InvocationTargetException wrapped?wrapped.getCause():failure);}
    }
    @SuppressWarnings("unchecked") Map<Object,Object> registry(String name){return get(consuming,name);}
    @SuppressWarnings("unchecked") List<Object> actors(String name){return get(consuming,name);}
    ArrayDeque<LbcStonekivi.Work> queue(){return get(stone,"work");}
    Map<BlockPos,net.minecraft.world.level.block.state.BlockState> portals(Object batch){return get(batch,"portals");}
    static BlockAreaWork.Bounds bounds(BlockPos a,BlockPos b){return new BlockAreaWork.Bounds(a.getX(),a.getY(),a.getZ(),b.getX(),b.getY(),b.getZ());}
    void write(BlockPos position,Block block){require(base.blocks.containsKey(position),"bounded native write "+position);base.owner.level().setBlock(position,block.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);}
    void erase(ServerPlayer owner,BlockPos position){invoke(consuming,"erase",new Class<?>[]{ServerPlayer.class,Vec3.class,double.class},owner,Vec3.atCenterOf(position),.01);}
    Object enqueue(ServerPlayer owner,Object child){invoke(consuming,"queue",new Class<?>[]{ServerPlayer.class,LbcStonekivi.Work.class},owner,child);return queue().getLast();}
    ItemEntity item(Vec3 position){
        var item=new ItemEntity(base.owner.level(),position.x,position.y,position.z,new ItemStack(Items.STICK));item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(10000);item.entityTags().addAll(Set.of("sin_gluttony","have_stand"));
        require(base.owner.level().addFreshEntity(item),"isolated registered native item");return item;
    }

    VanityConsumingScene(GameController game,ChefChecks.Fixture base,ServerPlayer gamma){
        this.game=game;this.base=base;initialGamma=gamma;currentGamma=gamma;consuming=game.items().consuming();stone=game.items().stonekivi();
        require(consuming.count()==0&&registry("terrain").isEmpty(),"exclusive consuming source state");
        eraseFirst=base.center.offset(-12,8,-14);eraseLast=eraseFirst.east(2);pendingCell=base.center.offset(-8,8,-14);futureCell=pendingCell.east();
        portalFirst=eraseFirst.south();portalLast=portalFirst.east(4);futurePortal=portalLast.east();
        // Native neighbor-shape validation requires a real frame for the Nether output.
        // Keep one missing interior cell as the accepted pending source placement.
        for(int x=-1;x<=2;x++)for(int y=-1;y<=3;y++){
            var at=portalFirst.offset(x,y,0);boolean edge=x==-1||x==2||y==-1||y==3;
            write(at,edge?Blocks.OBSIDIAN:at.equals(portalFirst)?Blocks.AIR:Blocks.NETHER_PORTAL);
        }
        write(portalLast,Blocks.AIR);write(futurePortal,Blocks.AIR);
        for(var position:BlockPos.betweenClosed(eraseFirst,eraseLast)){write(position,Blocks.BEDROCK);erase(gamma,position.immutable());}
        terrainBatch=registry("terrain").get(gamma);eraseScan=get(terrainBatch,"scan");
        try{eraseScan.step(game,gamma,1,Long.MAX_VALUE);}finally{game.items().indirectKills().finishSupportRemoval();}
        require(eraseScan.queuedBlockCount()==2,"actual native source erase advanced one saved bitmap cell");
        portals(terrainBatch).put(portalFirst,Blocks.NETHER_PORTAL.defaultBlockState());portals(terrainBatch).put(portalLast,Blocks.END_PORTAL.defaultBlockState());
        invoke(consuming,"flushTerrain",new Class<?>[]{});var terrainJob=queue().getLast();terrainChild=get(terrainJob,"child");require(get(terrainChild,"batch")==terrainBatch,"actual terrain wrapper shares original batch");
        write(pendingCell,Blocks.BEDROCK);write(futureCell,Blocks.BEDROCK);erase(gamma,pendingCell);pendingBatch=registry("terrain").get(gamma);pendingScan=get(pendingBatch,"scan");
        require(registry("terrain").remove(gamma)==pendingBatch,"retain accepted unflushed batch outside the earlier setup ticks");

        var source=base.center.offset(-12,10,4);write(source,Blocks.STONE);write(source.east(),Blocks.DIAMOND_BLOCK);
        var nativeTemplate=new StructureTemplate();nativeTemplate.fillFromWorld(gamma.level(),source,new Vec3i(2,1,1),false,List.of());
        pasteFirst=source.south(2);pasteLast=pasteFirst.east();write(pasteFirst,Blocks.AIR);write(pasteLast,Blocks.AIR);
        var part=new MapManager.TemplateSlice(nativeTemplate,new BoundingBox(0,0,0,1,0,0));
        templateChild=create("TemplateWork",gamma,part,pasteFirst,new BoundingBox(pasteFirst.getX(),pasteFirst.getY(),pasteFirst.getZ(),pasteLast.getX(),pasteLast.getY(),pasteLast.getZ()));var templateJob=enqueue(gamma,templateChild);

        scanFirst=base.center.offset(-9,7,-14);scanLast=scanFirst.east();write(scanFirst,Blocks.BEDROCK);write(scanLast,Blocks.BEDROCK);
        erasure=create("SilenceErase",gamma,game.items().epoch(),Vec3.atCenterOf(scanFirst));set(erasure,"settle",17);
        silenceChild=create("SilenceTerrainWork",erasure);silenceScan=get(silenceChild,"scan");silenceScan.clear();silenceScan.move(bounds(scanFirst,scanLast),true);var silenceJob=enqueue(gamma,silenceChild);
        disposedPrefix=item(base.at.add(-9,9,-14));disposedTarget=item(base.at.add(-8,9,-14));game.items().mobgun().dispose(disposedPrefix);
        disposeChild=create("SilenceDisposeWork",erasure,new ArrayList<Entity>(List.of(disposedPrefix,disposedTarget)));set(disposeChild,"cursor",1);var disposeJob=enqueue(gamma,disposeChild);
        impact=base.at.add(-10,10,4);movedPrefix=item(base.at.add(-14,10,4));movedTarget=item(base.at.add(-13,10,4));game.items().mobgun().dispose(movedPrefix);
        avariceChild=create("AvariceWork",gamma,impact,new ArrayList<Entity>(List.of(movedPrefix,movedTarget)));set(avariceChild,"cursor",1);var avariceJob=enqueue(gamma,avariceChild);

        partition=create("Partition",gamma,game.items().epoch(),base.at.add(-10,10,-10),new Vec3(.4,.2,.8));set(partition,"age",99);actors("partitions").add(partition);
        area=create("Area",gamma,game.items().epoch(),base.at.add(-10,10,-9));set(area,"age",5999);actors("areas").add(area);
        summon=new Skeleton(EntityTypes.SKELETON,gamma.level());summon.setPos(base.at.add(-12,9,-6));summon.setNoAi(true);summon.setNoGravity(true);summon.setDeltaMovement(Vec3.ZERO);summon.entityTags().addAll(Set.of("sin_gluttony","have_stand",LbcConsuming.TRIAL_ACTOR));game.items().markCurrentItemEpoch(summon);
        summonState=create("Owned",gamma,game.items().epoch());registry("summons").put(summon,summonState);require(gamma.level().addFreshEntity(summon),"canonical registered native trial skeleton");
        jobs=List.of(terrainJob,templateJob,silenceJob,disposeJob,avariceJob);var all=new ArrayList<Object>(List.of(partition,area,erasure,summonState));for(var job:jobs)all.add(get(job,"state"));states=List.copyOf(all);
    }
    List<Object> extraJobs(){return jobs;}
    void arm(){
        require(registry("terrain").isEmpty(),"no unrelated pending terrain at frozen input boundary");registry("terrain").put(initialGamma,pendingBatch);
        require(actors("silences").isEmpty(),"admit bounded silence guard only after all scene bodies exist");actors("silences").add(erasure);
        order=List.copyOf(queue());corpus=get(consuming,"prepared");resourceFuture=get(consuming,"preparation");portalOrder=List.copyOf(portals(terrainBatch).keySet());eraseRemaining=eraseScan.queuedBlockCount();
        require(order.containsAll(jobs)&&eraseRemaining==2&&pendingScan.queuedBlockCount()==1&&silenceScan.queuedBlockCount()==2,"all source children and separate partial/pending native terrain admitted");
    }
    void preflight(MatchDamageSources sources){
        int depth=get(consuming,"callbackDepth");boolean rejected=false;set(consuming,"callbackDepth",depth+1);
        try{consuming.checkpoint(new MatchQueuedWorkCheckpoint.Graph(sources));}catch(IllegalStateException expected){rejected=expected.getMessage().contains("consuming cast");}finally{set(consuming,"callbackDepth",depth);}
        require(rejected&&List.copyOf(queue()).equals(order)&&eraseScan.queuedBlockCount()==eraseRemaining,"source callback checkpoint rejection is side-effect free");
        var original=get(consuming,"preparation");rejected=false;set(consuming,"preparation",new CompletableFuture<>());
        try{consuming.checkpoint(new MatchQueuedWorkCheckpoint.Graph(sources));}catch(IllegalStateException expected){rejected=expected.getMessage().contains("resource preparation");}finally{set(consuming,"preparation",original);}
        require(rejected&&get(consuming,"prepared")==corpus,"genuinely pending immutable-input resource work rejects without ready/join or future reset");
        var detached=new ItemEntity(base.owner.level(),impact.x,impact.y,impact.z,new ItemStack(Items.STICK));var probe=(LbcStonekivi.Work)create("AvariceWork",initialGamma,impact,new ArrayList<Entity>(List.of(detached)));rejected=false;
        try{probe.checkpoint(new MatchQueuedWorkCheckpoint.Graph(sources));}catch(IllegalStateException expected){rejected=expected.getMessage().contains("Pending consuming target");}
        require(rejected&&detached.position().equals(impact)&&!detached.isRemoved()&&List.copyOf(queue()).equals(order),"mutable detached pending target rejects before teleport/damage/disposal");
    }
    void verify(ServerPlayer gamma){
        currentGamma=gamma;
        require(List.copyOf(queue()).equals(order)&&registry("work").keySet().equals(new HashSet<>(jobs)),"same original wrapper identities and dispatcher membership/order");
        for(var job:jobs)require(registry("work").get(job)==get(job,"state"),"source wrapper registry retains same original authority-state alias");
        for(var state:states)require(get(state,"owner")==gamma&&!VanityConsumingScene.<Boolean>get(state,"cancelled"),"current Gamma and independent accepted state cancellation restored");
        require(actors("partitions").equals(List.of(partition))&&VanityConsumingScene.<Integer>get(partition,"age")==99&&get(partition,"point").equals(base.at.add(-10,10,-10))&&get(partition,"direction").equals(new Vec3(.4,.2,.8)),"same partition identity/direction/age/point");
        require(actors("areas").equals(List.of(area))&&VanityConsumingScene.<Integer>get(area,"age")==5999&&get(area,"point").equals(base.at.add(-10,10,-9)),"same independent Area identity and terminal private age");
        require(actors("silences").equals(List.of(erasure))&&VanityConsumingScene.<Integer>get(erasure,"pending")==2&&VanityConsumingScene.<Integer>get(erasure,"settle")==17,"same Silence guard alias and independent pending/settle counters");
        require(eraseScan.queuedBlockCount()==eraseRemaining&&pendingScan.queuedBlockCount()==1&&silenceScan.queuedBlockCount()==2&&get(terrainBatch,"owner")==gamma&&get(pendingBatch,"owner")==gamma,"independent terrain/silence bitmaps with current-owner scan callbacks");
        require(registry("terrain").equals(Map.of(gamma,pendingBatch))&&new ArrayList<>(portals(terrainBatch).keySet()).equals(portalOrder)&&portals(terrainBatch).get(portalFirst).is(Blocks.NETHER_PORTAL)&&portals(terrainBatch).get(portalLast).is(Blocks.END_PORTAL),"unflushed native authority map and ordered original portal metadata");
        require(get(terrainChild,"batch")==terrainBatch&&get(silenceChild,"erasure")==erasure&&get(disposeChild,"erasure")==erasure&&get(templateChild,"owner")==gamma&&get(avariceChild,"owner")==gamma,"same graph child/owner/erasure aliases after current-player replacement");
        require(VanityConsumingScene.<Integer>get(disposeChild,"cursor")==1&&VanityConsumingScene.<List<Entity>>get(disposeChild,"doomed").equals(List.of(disposedPrefix,disposedTarget))&&disposedPrefix.isRemoved()&&!disposedTarget.isRemoved(),"processed historical disposal prefix and pending canonical body resume same cursor");
        require(VanityConsumingScene.<Integer>get(avariceChild,"cursor")==1&&VanityConsumingScene.<List<Entity>>get(avariceChild,"entities").equals(List.of(movedPrefix,movedTarget))&&movedPrefix.isRemoved()&&!movedTarget.isRemoved()&&movedTarget.position().equals(base.at.add(-13,10,4)),"processed historical teleport prefix and untouched pending native target");
        require(registry("summons").equals(Map.of(summon,summonState))&&!summon.isRemoved()&&invoke(consuming,"owner",new Class<?>[]{Entity.class},summon)==gamma&&consuming.canTrialTarget(summon,gamma),"same canonical native trial skeleton binds current caster without spawn factory");
        require(get(consuming,"prepared")==corpus&&get(consuming,"preparation")==resourceFuture,"resource corpus pointer and original completed/lazy future remain unchanged");
        require(base.owner.level().getBlockState(pasteFirst).isAir()&&base.owner.level().getBlockState(pasteLast).isAir()&&base.owner.level().getBlockState(pendingCell).is(Blocks.BEDROCK)&&base.owner.level().getBlockState(futureCell).is(Blocks.BEDROCK),"preflight/restore execute no pending/future native terrain or template placement");
    }
    void beforeThaw(){
        require(actors("silences").remove(erasure)&&actors("partitions").remove(partition)&&actors("areas").remove(area),"withdraw fixture-only broad admission guard/rays after frozen full-state proof");
        require(!VanityConsumingScene.<Boolean>get(erasure,"cancelled")&&VanityConsumingScene.<Integer>get(erasure,"pending")==2,"accepted bounded Silence scan/disposal children remain live after guard withdrawal");
    }
    boolean ready(){return jobs.stream().noneMatch(queue()::contains)&&consuming.pendingWorkCount()==0&&registry("terrain").isEmpty()&&disposedTarget.isRemoved()&&!movedTarget.isRemoved()&&movedTarget.position().equals(impact);}
    void thawed(){
        require(ready(),"all bounded source children resume and retire original wrappers");
        for(var position:BlockPos.betweenClosed(eraseFirst,eraseLast))require(base.owner.level().getBlockState(position).isAir(),"resumed original erase bitmap "+position);
        require(base.owner.level().getBlockState(pendingCell).isAir()&&base.owner.level().getBlockState(scanFirst).isAir()&&base.owner.level().getBlockState(scanLast).isAir(),"current-owner pending and Silence scan callbacks complete accepted native cells");
        require(base.owner.level().getBlockState(portalFirst).is(Blocks.NETHER_PORTAL)&&base.owner.level().getBlockState(portalLast).is(Blocks.END_PORTAL)&&base.owner.level().getBlockState(futurePortal).isAir(),"ordered original portals publish without future portal insertion: nether="+base.owner.level().getBlockState(portalFirst)+" end="+base.owner.level().getBlockState(portalLast)+" future="+base.owner.level().getBlockState(futurePortal));
        require(base.owner.level().getBlockState(pasteFirst).is(Blocks.STONE)&&base.owner.level().getBlockState(pasteLast).is(Blocks.DIAMOND_BLOCK)&&base.owner.level().getBlockState(futureCell).is(Blocks.BEDROCK),"actual tiny native template places only accepted cells; abandoned terrain extension stays intact");
        require(VanityConsumingScene.<Integer>get(erasure,"pending")==0&&VanityConsumingScene.<Integer>get(erasure,"settle")==17&&disposedTarget.isRemoved()&&movedTarget.position().equals(impact),"both same Silence pending aliases settle exactly and Avarice executes saved native teleport suffix");
        require(invoke(consuming,"owner",new Class<?>[]{Entity.class},summon)==currentGamma,"native summon owner remains current after accepted source continuations");consuming.stop();
    }
    void mutate(){
        for(var state:states)set(state,"cancelled",true);set(partition,"age",701);set(partition,"direction",Vec3.ZERO);set(partition,"point",impact);set(area,"age",702);set(area,"point",impact);set(erasure,"pending",703);set(erasure,"settle",704);
        eraseScan.include(bounds(futureCell,futureCell));pendingScan.include(bounds(futureCell,futureCell));silenceScan.include(bounds(futureCell,futureCell));portals(terrainBatch).clear();portals(terrainBatch).put(futurePortal,Blocks.END_PORTAL.defaultBlockState());
        VanityConsumingScene.<List<Entity>>get(disposeChild,"doomed").clear();VanityConsumingScene.<List<Entity>>get(avariceChild,"entities").clear();set(disposeChild,"cursor",705);set(avariceChild,"cursor",706);
        set(consuming,"prepared",create("Prepared",Map.of(),List.of()));
    }
    void pending(){require(consuming.count()==0&&VanityConsumingScene.<Integer>get(partition,"age")==701&&VanityConsumingScene.<Integer>get(erasure,"pending")==703&&VanityConsumingScene.<Integer>get(disposeChild,"cursor")==705&&get(consuming,"prepared")!=corpus,"offline Gamma defers every registry/cursor/resource-pointer install atomically");}
}
