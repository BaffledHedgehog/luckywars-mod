package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.skywars2.mixin.CombatCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Bounded native material jobs retain their accepted objects through two current-body replays. */
final class VanityMaterialScene {
    final GameController game;final ChefChecks.Fixture base;final LbcCorrosionDrops corrosion;
    final Object erosion;final LbcStonekivi.Work corrosionJob;final LbcTesla.Instant tesla;
    final LbcWands.ElectroTrace trace;final DamageSource acceptedDamage;
    final BlockPos erased,collider;final List<Object> jobs;
    final LbcIndirectKills.Owner traceCause;final LbcSpellSource.Source tracePolicy;
    List<LbcStonekivi.Work> order;List<Integer> placed;BitSet occupied;
    int chunkX,chunkZ,sectionY,cell,phase,follow,face,beam,sample,traceAge;
    BlockPos scratch,neighbor;long randomNext;Vec3 point,tracePosition;
    float yaw,pitch,traceYaw,tracePitch;ItemStack trialValue;boolean armed;int thaws;

    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("Vanity materials: "+detail);}
    private static Field field(Object target,String name){
        for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{
            var field=type.getDeclaredField(name);field.setAccessible(true);return field;
        }catch(NoSuchFieldException absent){}
        throw new IllegalStateException("Missing material fixture field "+target.getClass().getName()+"."+name);
    }
    @SuppressWarnings("unchecked") static <T>T get(Object target,String name){
        try{return (T)field(target,name).get(target);}catch(IllegalAccessException error){throw new IllegalStateException(error);}
    }
    static void set(Object target,String name,Object value){
        try{field(target,name).set(target,value);}catch(IllegalAccessException error){throw new IllegalStateException(error);}
    }
    private static void invoke(Object target,String name){
        for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{
            var method=type.getDeclaredMethod(name);method.setAccessible(true);method.invoke(target);return;
        }catch(NoSuchMethodException absent){}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        throw new IllegalStateException("Missing material fixture method "+name);
    }
    ArrayDeque<LbcStonekivi.Work> queue(){return get(game.items().stonekivi(),"work");}
    Set<Object> registry(){return get(corrosion,"jobs");}
    void write(BlockPos at,Block block){
        require(base.blocks.containsKey(at),"all source writes stay in saved terrain "+at);
        base.owner.level().setBlock(at,block.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
    }
    long nextRandom(){
        var random=VanityMaterialScene.<RandomSource>get(erosion,"random");var state=MatchRandomState.capture(random);
        long value=random.nextLong();state.restore(random);return value;
    }
    List<Integer> writes(){return new ArrayList<>(VanityMaterialScene.<IntArrayList>get(erosion,"placed"));}
    Object newCorrosion(ServerPlayer gamma){
        try{
            var type=Arrays.stream(LbcCorrosionDrops.class.getDeclaredClasses()).filter(c->c.getSimpleName().equals("Job")).findFirst().orElseThrow();
            var constructor=type.getDeclaredConstructor(LbcCorrosionDrops.class,ServerLevel.class,Vec3.class,LbcCorrosionDropItem.Kind.class,ServerPlayer.class,long.class);
            constructor.setAccessible(true);return constructor.newInstance(corrosion,gamma.level(),Vec3.atCenterOf(erased),LbcCorrosionDropItem.Kind.NORMAL,gamma,12345L);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
    }

    VanityMaterialScene(GameController game,ChefChecks.Fixture base,ServerPlayer gamma){
        this.game=game;this.base=base;corrosion=game.items().corrosionDrops();require(corrosion.count()==0,"exclusive corrosion registry");
        erased=base.center.offset(-15,9,12);collider=base.center.offset(-15,10,-8);
        for(var direction:Direction.values()){var at=erased.relative(direction);require(base.blocks.containsKey(at),"native shape halo remains bounded");write(at,Blocks.AIR);}
        write(erased,Blocks.STONE);erosion=newCorrosion(gamma);corrosionJob=(LbcStonekivi.Work)erosion;
        set(erosion,"chunkX",erased.getX()>>4);set(erosion,"chunkZ",erased.getZ()>>4);set(erosion,"sectionY",erased.getY()>>4);
        set(erosion,"cell",((erased.getY()&15)<<8)|((erased.getZ()&15)<<4)|(erased.getX()&15));
        // One actual source erase advances the real sparse list/bitmap and native private RNG.
        game.items().indirectKills().worldChanges(gamma.level(),gamma,true,()->invoke(erosion,"erase"));
        require(gamma.level().getBlockState(erased).isAir()&&writes().size()==1,"one deterministic actual sparse erase");
        set(erosion,"phase",1);invoke(erosion,"edge");invoke(erosion,"edge");
        require(VanityMaterialScene.<Integer>get(erosion,"face")==2,"real edge traversal is partially complete");
        registry().add(erosion);game.items().stonekivi().queuedWork(corrosionJob);
        require(corrosionJob.ready(),"source48-cell halo is already resident without fixture chunk loads");

        // Keep the independent terminal collider behind the older live snake/sculk region.
        var originalPosition=gamma.position();float originalYaw=gamma.getYRot(),originalPitch=gamma.getXRot();
        try{
            gamma.setPos(base.at.add(-15,10-gamma.getEyeHeight(),-5));gamma.setYRot(180);gamma.setXRot(0);
            for(int z=-7;z<=-5;z++)write(base.center.offset(-15,10,z),Blocks.AIR);write(collider,Blocks.BEDROCK);
            tesla=new LbcTesla.Instant(game,gamma);require(!tesla.step()&&tesla.sample>0&&tesla.beam==0,"real Tesla source step leaves partial samples");
            require(base.blocks.containsKey(BlockPos.containing(tesla.point)),"partial Tesla remains inside bounded corridor");
            tesla.beam=4;tesla.sample=399;
        }finally{gamma.setPos(originalPosition);gamma.setYRot(originalYaw);gamma.setXRot(originalPitch);}
        trace=tesla.trace;acceptedDamage=tesla.damage;
        tracePolicy=game.items().spellSources().source(gamma);require(tracePolicy!=null,"preceding source scene supplies original policy alias");
        traceCause=tracePolicy.cause();trace.sourcePolicy=tracePolicy;trace.cause=traceCause;trace.explicitEmitter=gamma;
        trace.trialOwner=gamma;trace.trialHand=InteractionHand.OFF_HAND;trace.trialStack=gamma.getOffhandItem();
        require(!trace.trialStack.isEmpty(),"real current offhand is the trace stack root");
        game.items().stonekivi().queuedWork(tesla);jobs=List.of(erosion,tesla);
    }

    List<Object> extraJobs(){return jobs;}
    void arm(){
        order=List.copyOf(queue());require(order.containsAll(jobs)&&registry().contains(erosion),"both same accepted jobs remain queued during stopped time");
        placed=writes();occupied=(BitSet)VanityMaterialScene.<BitSet>get(erosion,"occupied").clone();
        chunkX=get(erosion,"chunkX");chunkZ=get(erosion,"chunkZ");sectionY=get(erosion,"sectionY");cell=get(erosion,"cell");
        phase=get(erosion,"phase");follow=get(erosion,"follow");face=get(erosion,"face");
        scratch=VanityMaterialScene.<BlockPos.MutableBlockPos>get(erosion,"pos").immutable();neighbor=VanityMaterialScene.<BlockPos.MutableBlockPos>get(erosion,"neighbor").immutable();randomNext=nextRandom();
        point=tesla.point;yaw=tesla.currentYaw;pitch=tesla.currentPitch;beam=tesla.beam;sample=tesla.sample;
        tracePosition=trace.position;traceYaw=trace.yaw;tracePitch=trace.pitch;traceAge=trace.age;trialValue=trace.trialStack.copy();armed=true;
        require(phase==1&&face==2&&placed.size()==1&&beam==4&&sample==399,"exact source material partial phases survive initial Time Stop");
    }
    void preflight(MatchDamageSources sources){
        require(armed,"arm before material negative capture");int depth=get(corrosion,"inProgress");boolean busyRejected=false,extensionRejected=false;
        set(corrosion,"inProgress",depth+1);
        try{new MatchQueuedWorkCheckpoint(game,sources);}catch(IllegalStateException expected){busyRejected=expected.getMessage().contains("corrosion native callbacks");}
        finally{set(corrosion,"inProgress",depth);}
        set(trace,"npcSource",base.other);
        try{new MatchQueuedWorkCheckpoint(game,sources);}catch(IllegalStateException expected){extensionRejected=expected.getMessage().contains("Unsupported Tesla instant trace extension");}
        finally{set(trace,"npcSource",null);}
        require(busyRejected&&extensionRejected&&List.copyOf(queue()).equals(order)&&writes().equals(placed)
                &&nextRandom()==randomNext&&tesla.point.equals(point)&&tesla.sample==sample&&acceptedDamage==tesla.damage,
                "live/native extension rejection leaves queues, cursor, RNG and accepted source untouched");
    }
    void verify(ServerPlayer gamma){
        require(List.copyOf(queue()).equals(order)&&registry().equals(Set.of(erosion)),"original material queue and private registry memberships restore");
        require(get(erosion,"cause")==gamma&&!VanityMaterialScene.<Boolean>get(erosion,"cancelled")&&writes().equals(placed)
                &&VanityMaterialScene.<BitSet>get(erosion,"occupied").equals(occupied)&&nextRandom()==randomNext,"current sparse author plus independent ordered writes/bitmap/native RNG");
        require(VanityMaterialScene.<Integer>get(erosion,"chunkX")==chunkX&&VanityMaterialScene.<Integer>get(erosion,"chunkZ")==chunkZ
                &&VanityMaterialScene.<Integer>get(erosion,"sectionY")==sectionY&&VanityMaterialScene.<Integer>get(erosion,"cell")==cell
                &&VanityMaterialScene.<Integer>get(erosion,"phase")==phase&&VanityMaterialScene.<Integer>get(erosion,"follow")==follow
                &&VanityMaterialScene.<Integer>get(erosion,"face")==face&&get(erosion,"pos").equals(scratch)&&get(erosion,"neighbor").equals(neighbor),"all original sparse phase and scratch cursors restore independently");
        require(tesla.owner==gamma&&tesla.trace==trace&&tesla.damage==acceptedDamage&&acceptedDamage.getEntity()==gamma&&acceptedDamage.getDirectEntity()==gamma,
                "same accepted Tesla DamageSource and trace use CURRENT canonical Gamma without reconstructing the source");
        require(tesla.point.equals(point)&&tesla.currentYaw==yaw&&tesla.currentPitch==pitch&&tesla.beam==beam&&tesla.sample==sample,"exact accepted Tesla beam/sample geometry restores");
        require(trace.owner==gamma&&trace.explicitEmitter==gamma&&trace.trialOwner==gamma&&trace.trialHand==InteractionHand.OFF_HAND
                &&trace.trialStack==gamma.getOffhandItem()&&ItemStack.matches(trace.trialStack,trialValue)&&trace.cause==traceCause&&trace.sourcePolicy==tracePolicy,
                "same trace preserves current body, immutable source policy/cause and actual native hand stack aliases");
        require(trace.position.equals(tracePosition)&&trace.yaw==traceYaw&&trace.pitch==tracePitch&&trace.age==traceAge
                &&trace.mechanism&&!trace.teslaChain&&!trace.statusWild&&!trace.hostileSauvojen&&trace.npcSource==null&&trace.erecto==null,
                "all supported mutable trace values restore with the original schema");
        require(base.owner.level().getBlockState(erased).isAir()&&base.owner.level().getBlockState(collider).is(Blocks.BEDROCK),"restore performs no material factory, erase or finishing callback");
    }
    void mutate(){
        corrosion.stop();set(erosion,"phase",3);set(erosion,"cell",999);set(erosion,"face",5);set(erosion,"follow",99);
        VanityMaterialScene.<IntArrayList>get(erosion,"placed").add(7);VanityMaterialScene.<BitSet>get(erosion,"occupied").set(7);
        VanityMaterialScene.<BlockPos.MutableBlockPos>get(erosion,"pos").set(base.center);VanityMaterialScene.<BlockPos.MutableBlockPos>get(erosion,"neighbor").set(base.center);
        VanityMaterialScene.<RandomSource>get(erosion,"random").nextLong();
        tesla.point=base.at.add(-15,10,-6);tesla.currentYaw=99;tesla.currentPitch=99;tesla.beam=99;tesla.sample=99;
        trace.owner=base.other;trace.explicitEmitter=base.other;trace.trialOwner=base.other;trace.trialHand=InteractionHand.MAIN_HAND;
        trace.trialStack=new ItemStack(Items.STICK,3);trace.cause=null;trace.sourcePolicy=null;trace.position=base.at;trace.yaw=99;trace.pitch=99;trace.age=99;
        trace.mechanism=false;trace.teslaChain=true;trace.statusWild=true;trace.hostileSauvojen=true;
        var source=(CombatCheckpointAccess.Source)acceptedDamage;source.skywars2$direct(base.other);source.skywars2$cause(base.other);
    }
    void pending(){
        require(corrosion.count()==0&&VanityMaterialScene.<Integer>get(erosion,"phase")==3&&VanityMaterialScene.<Integer>get(erosion,"follow")==99
                &&writes().equals(List.of(7))&&tesla.beam==99&&tesla.sample==99&&trace.age==99&&trace.owner==base.other,
                "offline Gamma defers all material job/trace metadata without partial installation");
    }
    boolean ready(){return jobs.stream().noneMatch(queue()::contains)&&!registry().contains(erosion);}
    void thawed(){
        require(ready()&&VanityMaterialScene.<Integer>get(erosion,"phase")==4&&VanityMaterialScene.<Boolean>get(erosion,"cancelled")
                &&writes().isEmpty()&&VanityMaterialScene.<BitSet>get(erosion,"occupied").isEmpty()&&nextRandom()==randomNext,
                "real saved sparse edge/update/finish completes once without replaying erase or private RNG");
        require(tesla.beam==5&&tesla.damage==acceptedDamage&&tesla.trace==trace&&base.owner.level().getBlockState(erased).isAir()
                &&base.owner.level().getBlockState(collider).is(Blocks.BEDROCK),"one real final Tesla terminal uses the accepted source; original material geometry stays bounded: beam="+tesla.beam+" sample="+tesla.sample+" removed="+tesla.owner.isRemoved()+" alive="+tesla.owner.isAlive()+" usable="+game.canUseGameplayItems(tesla.owner)+" epoch="+game.items().currentEpoch(tesla.epoch)+" erased="+base.owner.level().getBlockState(erased)+" collider="+base.owner.level().getBlockState(collider));
        require(++thaws<=3,"bounded original/replayed source terminal rounds");
    }
}
