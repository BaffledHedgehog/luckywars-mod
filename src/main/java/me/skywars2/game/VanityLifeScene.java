package me.skywars2.game;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.Vec3;

/** One native bean step and bounded native totem consequences through the complete replay fixture. */
final class VanityLifeScene {
    final GameController game;final ChefChecks.Fixture base;final LbcMagicBean beans;final LbcTotems totems;
    final Marker marker;final Object bean,growth,lava,refund,debt;final WitherSkeleton skeleton;
    final LbcIndirectKills.Owner beanCause,lavaCause,debtCause;
    final BlockPos origin,basin;final List<BlockPos> cells;final Vec3 refundAt;
    final ItemStack refundValue;final List<Object> jobs;
    int stepAge,lastAge,clock;boolean lastSentinel,performed;
    long steps,completions;Vec3 expectedMove,grownAt;float[] draws;
    Map<Integer,float[]> trailDraws;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity life: "+why);}
    static Field field(Object target,String name){
        for(var type=target.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field;}catch(NoSuchFieldException absent){}
        throw new IllegalStateException("Missing native fixture field "+target.getClass().getName()+"."+name);
    }
    @SuppressWarnings("unchecked") static <T>T get(Object target,String name){try{return (T)field(target,name).get(target);}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}}
    static void set(Object target,String name,Object value){try{field(target,name).set(target,value);}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}}
    static Object invoke(Object target,String name,Class<?>[] types,Object...args){
        for(var type=target.getClass();type!=null;type=type.getSuperclass())try{var method=type.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(target,args);}catch(NoSuchMethodException absent){}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        throw new IllegalStateException("Missing native fixture method "+name);
    }
    static Object construct(Object owner,String name,Class<?>[] types,Object...args){try{
        var type=Arrays.stream(owner.getClass().getDeclaredClasses()).filter(c->c.getSimpleName().equals(name)).findFirst().orElseThrow();
        var parameters=new ArrayList<Class<?>>();var values=new ArrayList<Object>();if(!Modifier.isStatic(type.getModifiers())){parameters.add(owner.getClass());values.add(owner);}parameters.addAll(List.of(types));values.addAll(Arrays.asList(args));
        var ctor=type.getDeclaredConstructor(parameters.toArray(Class<?>[]::new));ctor.setAccessible(true);return ctor.newInstance(values.toArray());
    }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}}
    ArrayDeque<LbcStonekivi.Work> queue(){return get(game.items().stonekivi(),"work");}
    boolean refund(ItemStack stack){return LbcFartus.data(stack).getIntOr("vanity_life_refund",0)==1;}
    int outputs(){int total=0;for(var entity:base.owner.level().getAllEntities())if(entity instanceof ItemEntity item&&!item.isRemoved()&&refund(item.getItem()))total+=item.getItem().getCount();return total;}
    void write(BlockPos pos,Block block){require(base.blocks.containsKey(pos),"bounded native fixture cell "+pos);base.owner.level().setBlock(pos,block.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);}

    VanityLifeScene(GameController game,ChefChecks.Fixture base,ServerPlayer gamma){
        this.game=game;this.base=base;beans=game.items().magicBean();totems=game.items().totems();
        require(beans.count()==0&&totems.actors()==0&&totems.work()==0&&totems.pendingDeaths()==0&&totems.pendingRefunds()==0,"exclusive bean/totem source owners");
        basin=base.center.offset(-6,10,14);cells=List.of(basin,basin.east(),basin.east(2));
        for(var pos:BlockPos.betweenClosed(basin.offset(-1,-1,-1),basin.offset(3,1,1)))write(pos,Blocks.BEDROCK);
        for(var cell:cells)write(cell,Blocks.AIR);
        lavaCause=game.items().indirectKills().wildCause(gamma);
        lava=construct(totems,"Lava",new Class<?>[]{ServerLevel.class,Collection.class,LbcIndirectKills.Owner.class,String.class},gamma.level(),cells,lavaCause,game.items().epoch());
        // Explicit accepted partial cursor: cell0 has the exact native falling-lava value already.
        gamma.level().setBlock(cells.getFirst(),Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL,8),Block.UPDATE_ALL);set(lava,"cursor",1);
        VanityLifeScene.<Set<Object>>get(totems,"lava").add(lava);game.items().stonekivi().queuedWork((LbcStonekivi.Work)lava);
        refundAt=base.at.add(-6,7,14);var payload=new ItemStack(Items.PAPER);var tag=new CompoundTag();tag.putInt("vanity_life_refund",1);tag.putString("accepted","source");payload.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));refundValue=payload.copy();
        refund=construct(totems,"Refund",new Class<?>[]{ServerLevel.class,Vec3.class,ItemStack.class,int.class,String.class},gamma.level(),refundAt,payload,3,game.items().epoch());VanityLifeScene.<ArrayDeque<Object>>get(totems,"refunds").addLast(refund);
        invoke(totems,"spawn",new Class<?>[]{ServerPlayer.class,Vec3.class},gamma,base.at.add(-14,6,0));skeleton=totems.snapshot().getFirst();skeleton.setNoAi(true);skeleton.setNoGravity(true);skeleton.setCanPickUpLoot(false);skeleton.entityTags().addAll(Set.of("sin_gluttony","have_stand"));
        // A private accepted death debt tests current-body keys and phase, then is withdrawn after
        // frozen verification so the earlier Gamma health control remains an independent witness.
        debtCause=game.items().indirectKills().wildCause(base.other);debt=construct(totems,"Debt",new Class<?>[]{LbcIndirectKills.Owner.class,int.class},debtCause,1);
        VanityLifeScene.<Map<ServerPlayer,Object>>get(totems,"deaths").put(gamma,debt);game.items().mana().score(gamma,"death_timer").set(5);

        origin=base.center.offset(0,9,0);var at=Vec3.atCenterOf(origin);var loot=game.lbc().rollTable(gamma.level(),at,gamma,"lbc:drops/magic_bean");require(loot.size()==1&&LbcMagicBean.recognized(loot.getFirst()),"actual source Magic Bean leaf");
        var token=new ItemEntity(gamma.level(),at.x,at.y,at.z,loot.getFirst().copyWithCount(2));token.setThrower(gamma);token.setNoGravity(true);token.setOnGround(true);token.setDeltaMovement(Vec3.ZERO);game.items().markCurrentItemEpoch(token);
        require(gamma.level().addFreshEntity(token)&&beans.ready(token)&&game.items().specialDrops().spawn(token)&&token.getItem().getCount()==1,"actual shared source dispatcher debits one of two bean tokens");token.discard();
        marker=beans.snapshot().getFirst();bean=VanityLifeScene.<Map<Marker,Object>>get(beans,"beans").get(marker);beanCause=get(bean,"cause");require(beanCause!=null&&beanCause.player().equals(gamma.getUUID())&&beanCause.friendlyFire(),"real bean retains accepted WILD source cause");
        set(bean,"lastStepTick",game.server().getTickCount()-1);set(beans,"lastTick",Integer.MIN_VALUE);beans.tick();growth=get(bean,"pending");require(growth instanceof LbcStonekivi.Work&&queue().getLast()==growth,"actual source service creates its original pending Growth alias");
        jobs=List.of(lava,growth);
    }
    List<Object> extraJobs(){return jobs;}
    Vec3 moved(float[] values){return new Vec3(origin.getX()+(values[0]<.5f?1:0)-(values[1]<.5f?1:0),origin.getY()+(values[4]<.5f?1:0),origin.getZ()+(values[2]<.5f?1:0)-(values[3]<.5f?1:0));}
    void seedGrowthEntropy(){
        // Select a real world-RNG state before the common random checkpoint. Preview is restored;
        // TimeStop.end consumes one native sound-seed nextLong per trail: four initially, three
        // after rewind. Both real branches must remain nonterminal without replacing the RNG.
        var random=base.owner.level().getRandom();trailDraws=null;
        for(int attempt=0;attempt<256;attempt++){
            var image=MatchRandomState.capture(random);var candidates=new LinkedHashMap<Integer,float[]>();boolean safe=true;
            for(int trails:new int[]{3,4}){
                image.restore(random);for(int i=0;i<trails;i++)random.nextLong();var values=new float[6];for(int i=0;i<6;i++)values[i]=random.nextFloat();candidates.put(trails,values);safe&=values[5]>.008f;
            }
            image.restore(random);if(safe){trailDraws=Map.copyOf(candidates);break;}
            // Rejected candidates advance only this explicit initial fixture phase.
            for(int i=0;i<6;i++)random.nextFloat();
        }
        require(trailDraws!=null,"native source RNG has bounded nonterminal states after both real trail counts");
    }
    void arm(){
        seedGrowthEntropy();
        int frame=game.server().getTickCount();stepAge=frame-VanityLifeScene.<Integer>get(bean,"lastStepTick");int last=get(beans,"lastTick");lastSentinel=last==Integer.MIN_VALUE;lastAge=frame-last;
        steps=beans.steps();completions=beans.completions();set(totems,"clock",41);clock=41;
        require(outputs()==0&&List.copyOf(queue()).containsAll(jobs),"saved original jobs exist without any refund or growth callback");
    }
    void preflight(MatchDamageSources sources){
        var before=List.copyOf(queue());var gamma=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestGamma"));totems.begin(gamma,gamma.damageSources().generic());boolean rejected=false;
        try{new MatchQueuedWorkCheckpoint(game,sources);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("totem damage/reward callbacks");}finally{totems.end(gamma);}
        require(rejected&&List.copyOf(queue()).equals(before)&&outputs()==0&&beans.steps()==steps&&VanityLifeScene.<Integer>get(lava,"cursor")==1,"live native totem damage context rejects capture without consuming earlier work or rewards");
    }
    void verify(ServerPlayer gamma){
        require(VanityLifeScene.<Map<Marker,Object>>get(beans,"beans").equals(Map.of(marker,bean))&&get(bean,"marker")==marker&&base.owner.level().getEntity(marker.getUUID())==marker&&get(bean,"cause")==beanCause&&get(bean,"pending")==growth&&get(growth,"bean")==bean&&!VanityLifeScene.<Boolean>get(growth,"cancelled")&&queue().contains(growth),"same Bean/Growth registry/dispatcher aliases and canonical marker/source return");
        int frame=game.server().getTickCount();require(frame-VanityLifeScene.<Integer>get(bean,"lastStepTick")==stepAge&&(lastSentinel?VanityLifeScene.<Integer>get(beans,"lastTick")==Integer.MIN_VALUE:frame-VanityLifeScene.<Integer>get(beans,"lastTick")==lastAge)&&beans.steps()==steps&&beans.completions()==completions,"publication wait preserves native bean frame ages, dedup sentinel and exact counters");
        require(marker.position().equals(Vec3.atCenterOf(origin))&&beans.owner(marker)==gamma,"current source author resolves without replaying growth or a spawn factory");
        require(VanityLifeScene.<Set<Object>>get(totems,"lava").equals(Set.of(lava))&&queue().contains(lava)&&VanityLifeScene.<List<BlockPos>>get(lava,"cells").equals(cells)&&VanityLifeScene.<Integer>get(lava,"cursor")==1&&!VanityLifeScene.<Boolean>get(lava,"cancelled")&&get(lava,"cause")==lavaCause,"same ordered native lava job/private registry/source and independent partial cursor");
        require(base.owner.level().getBlockState(cells.getFirst()).is(Blocks.LAVA)&&base.owner.level().getBlockState(cells.get(1)).isAir()&&base.owner.level().getBlockState(cells.get(2)).isAir(),"only acknowledged first lava cell exists before replay");
        require(VanityLifeScene.<ArrayDeque<Object>>get(totems,"refunds").stream().toList().equals(List.of(refund))&&VanityLifeScene.<Integer>get(refund,"remaining")==3&&ItemStack.matches(get(refund,"copy"),refundValue)&&totems.pendingRefunds()==3&&outputs()==0,"original refund object/order and independent private payload/count restore without native output");
        require(totems.snapshot().equals(List.of(skeleton))&&base.owner.level().getEntity(skeleton.getUUID())==skeleton&&totems.owner(skeleton)==gamma&&skeleton.isNoAi()&&skeleton.isNoGravity()&&!skeleton.canPickUpLoot(),"native skeleton body/registry and current author restore together");
        require(VanityLifeScene.<Map<ServerPlayer,Object>>get(totems,"deaths").equals(Map.of(gamma,debt))&&get(debt,"cause")==debtCause&&VanityLifeScene.<Integer>get(debt,"phase")==1&&game.items().mana().hudValue(gamma,"death_timer")==5&&VanityLifeScene.<Integer>get(totems,"clock")==clock,"current-player death-debt key, original accepted cause, phase, native score and source clock restore without callbacks");
    }
    void beforeThaw(){
        var gamma=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestGamma"));require(VanityLifeScene.<Map<ServerPlayer,Object>>get(totems,"deaths").get(gamma)==debt,"only this fixture's verified debt is withdrawn");totems.clear(gamma);require(totems.pendingDeaths()==0,"native source clear removes the saved debt before health-control thaw");
    }
    void afterThaw(){
        require(!performed&&game.server().tickRateManager().isFrozen()&&!game.items().timeStop().stopped(base.owner.level()),"one original bean work step at the real stopped-to-live boundary");
        var random=base.owner.level().getRandom();var entropy=MatchRandomState.capture(random);var next=new float[6];try{for(int i=0;i<6;i++)next[i]=random.nextFloat();}finally{entropy.restore(random);}
        require((Arrays.equals(next,trailDraws.get(3))||Arrays.equals(next,trailDraws.get(4)))&&next[5]>.008f,"native growth RNG follows one selected real trail branch before any write: three="+Arrays.toString(trailDraws.get(3))+" four="+Arrays.toString(trailDraws.get(4))+" actual="+Arrays.toString(next));
        draws=next;expectedMove=moved(draws);
        require(queue().remove(growth)&&((LbcStonekivi.Work)growth).ready()&&((LbcStonekivi.Work)growth).step(),"actual original dispatcher-owned Growth executes once");
        grownAt=marker.position();require(beans.steps()==steps+1&&beans.completions()==completions&&grownAt.equals(expectedMove)&&!marker.isRemoved()&&get(bean,"pending")==null&&VanityLifeScene.<Boolean>get(growth,"cancelled"),"native six-draw movement, one growth counter and original terminal without completion");
        for(var cell:BlockPos.betweenClosed(origin.offset(-1,0,-1),origin.offset(1,0,1)))require(base.owner.level().getBlockState(cell).is(Blocks.OAK_WOOD),"all nine real source oak cells "+cell);
        beans.stop();performed=true;require(beans.count()==0&&marker.isRemoved(),"retire source bean after its one actual step to prevent unrelated recurring growth");
    }
    boolean ready(){return performed&&!queue().contains(lava)&&totems.work()==0&&totems.pendingRefunds()==0&&outputs()==3;}
    void thawed(){
        require(ready()&&beans.steps()==steps+1&&beans.completions()==completions&&grownAt.equals(expectedMove),"one original native growth and exactly three resumed native refund items per replay");
        for(var cell:cells)require(base.owner.level().getBlockState(cell).is(Blocks.LAVA),"remaining native lava cell resumes "+cell);
        require(VanityLifeScene.<Integer>get(lava,"cursor")==cells.size()&&VanityLifeScene.<Integer>get(refund,"remaining")==0&&VanityLifeScene.<Integer>get(totems,"clock")>clock&&totems.snapshot().equals(List.of(skeleton)),"actual lava cursor/refund debt finish and logical held-pulse clock resumes");
        var counter=beans.steps();require(((LbcStonekivi.Work)growth).step()&&beans.steps()==counter,"completed/cancelled original Growth cannot write or increment a second time");totems.stop();
    }
    void mutate(){
        performed=false;set(bean,"lastStepTick",999);set(beans,"lastTick",999);set(beans,"steps",steps+99);set(beans,"completions",completions+99);set(growth,"cancelled",false);set(bean,"pending",null);
        set(lava,"cursor",0);set(lava,"cancelled",true);set(refund,"remaining",99);var payload=VanityLifeScene.<ItemStack>get(refund,"copy");payload.setCount(19);var tag=LbcFartus.data(payload);tag.putString("accepted","future");payload.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));set(totems,"clock",99);
    }
    void pending(){require(beans.count()==0&&totems.actors()==0&&totems.work()==0&&totems.pendingRefunds()==0&&totems.pendingDeaths()==0&&VanityLifeScene.<Integer>get(bean,"lastStepTick")==999&&VanityLifeScene.<Integer>get(refund,"remaining")==99&&VanityLifeScene.<Integer>get(lava,"cursor")==0,"offline Gamma defers entire life owner while future private counters/payload remain uninstalled");}
}
