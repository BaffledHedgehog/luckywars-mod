package me.skywars2.game;

import java.util.*;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

/** Actual terrain factories and native chest roots; only fixture geometry/progress is explicitly seeded. */
final class VanityTerrainScene {
    final GameController game;final ChefChecks.Fixture base;final LbcStonekivi stone;final LbcBolts bolts;
    final BlockPos chest,otherChest,stoneFirst,stoneLast,stoneFuture,magFirst,magLast,magFuture,areaFirst,areaLast,areaFuture,pasteFirst,pasteLast;
    final Object stoneJob,stoneSlices,trial,magmify,ownedJob,reforgeJob,futureStone;
    final ObservedWork placedSlice,trialCall;
    final Object blockJob,boltTemplate;final BlockAreaWork area;
    final LbcBolts.Owned state;final LbcBolts.ReforgeInput input;final ItemEntity arrows;
    final Cow chainTarget;final LbcBolts.Chain chain;final LbcBolts.Meteor meteor;
    final List<LbcBolts.Bolt> boltStates;final List<LbcBolts.Water> waterStates;
    final List<Object> jobs,stoneChildren,sliceChildren,ownedChildren;
    final List<ItemEntity> output;final List<ItemStack> outputValues;
    List<Object> order;Map<Long,Object> originalCells,expectedCells;Set<Long> discovered,surface;
    long[] frontier,reservoir;long randomNext;Object magPhase;int seen,boltTicks,reforgeRemaining,reforgeUnits,reforgeConverted,chainDelay;
    ItemStack chestWand;int sourceMenuSlot;
    final Object chainStatus;final List<Object> admittedStatus;
    final List<LbcIndirectKills.Owner> statusCauses;CompoundTag statusAttachment;
    List<Object> currentStatus;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity terrain: "+why);}
    static <T>T get(Object target,String name){return VanitySpellScene.get(target,name);}
    static void set(Object target,String name,Object value){VanitySpellScene.set(target,name,value);}
    static Object invoke(Object target,String name,Class<?>[] types,Object...args){return VanityBatchScene.invoke(target,name,types,args);}
    ArrayDeque<LbcStonekivi.Work> queue(){return get(stone,"work");}
    ArrayDeque<LbcStonekivi.Work> children(Object sequence){return get(sequence,"children");}
    ArrayDeque<LbcStonekivi.Work> ownedChildren(Object sequence){
        try{var type=sequence.getClass();while(type!=null){try{var field=type.getDeclaredField("jobs");field.setAccessible(true);return (ArrayDeque<LbcStonekivi.Work>)field.get(sequence);}catch(NoSuchFieldException absent){type=type.getSuperclass();}}throw new NoSuchFieldException("jobs");}
        catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
    }
    void write(BlockPos pos,Block block){require(base.blocks.containsKey(pos),"bounded native terrain "+pos);base.owner.level().setBlock(pos,block.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);}
    static BlockAreaWork.Bounds bounds(BlockPos first,BlockPos last){return new BlockAreaWork.Bounds(first.getX(),first.getY(),first.getZ(),last.getX(),last.getY(),last.getZ());}
    Object template(ServerPlayer owner,Identifier id,BlockPos origin){return invoke(stone,"template",new Class<?>[]{ServerPlayer.class,Identifier.class,BlockPos.class,float.class},owner,id,origin,1f);}
    Object boltTemplate(LbcBolts.Owned owner,Identifier id,BlockPos origin){return invoke(bolts,"template",new Class<?>[]{LbcBolts.Owned.class,Identifier.class,BlockPos.class},owner,id,origin);}

    /** Observe actual source work at its boundary, before independent WILD effects can overwrite its result. */
    final class ObservedWork implements LbcStonekivi.Work {
        final LbcStonekivi.Work actual;final BlockPos placed;int completions;
        ObservedWork(LbcStonekivi.Work actual,BlockPos placed){this.actual=actual;this.placed=placed;}
        public boolean ready(){return actual.ready();}
        public void cancel(){actual.cancel();}
        public boolean step(){
            if(placed==null){var current=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestGamma"));require(get(trial,"owner")==current&&get(trial,"stack")==current.getOffhandItem(),"actual trial callback uses current canonical player and hand");}
            boolean done=actual.step();if(done){completions++;if(placed!=null)require(base.owner.level().getBlockState(placed).is(Blocks.DIAMOND_BLOCK),"actual native slice places its diamond before later WILD terrain work");}
            return done;
        }
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){graph.capture(actual);int count=completions;return links->()->completions=count;}
    }

    VanityTerrainScene(GameController game,ChefChecks.Fixture base,ServerPlayer gamma){
        this.game=game;this.base=base;stone=game.items().stonekivi();bolts=game.items().bolts();
        require(bolts.count()==0,"exclusive bolt private actors and reforge producers");
        chest=base.center.offset(13,2,-12);otherChest=chest.east();
        require(base.other.level().getBlockEntity(chest) instanceof Container,"preceding terminal scene owns the native source chest");
        var left=Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH).setValue(ChestBlock.TYPE,ChestType.LEFT);
        var right=left.setValue(ChestBlock.TYPE,ChestType.RIGHT);
        base.owner.level().setBlock(chest,left,Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);write(otherChest,Blocks.CHEST);base.owner.level().setBlock(otherChest,right,Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        chestWand=((Container)base.owner.level().getBlockEntity(chest)).getItem(0);require(chestWand.is(LbcWands.SNOW),"physical source wand survives native chest joining");
        ((Container)base.owner.level().getBlockEntity(otherChest)).setItem(8,new ItemStack(Items.EMERALD,11));

        // Two real native template slices keep source metadata while restricting all writes to two cells.
        var source=base.center.offset(8,8,8);write(source,Blocks.STONE);write(source.east(),Blocks.DIAMOND_BLOCK);
        var nativeTemplate=new StructureTemplate();nativeTemplate.fillFromWorld(gamma.level(),source,new Vec3i(2,1,1),false,List.of());
        var parts=List.of(new MapManager.TemplateSlice(nativeTemplate,new BoundingBox(0,0,0,0,0,0)),new MapManager.TemplateSlice(nativeTemplate,new BoundingBox(1,0,0,1,0,0)));
        var id=Identifier.parse("lbc:vanity_terrain_fixture");
        var prepared=VanityTerrainScene.<Map<Identifier,List<MapManager.TemplateSlice>>>get(stone,"templates");
        var trialStack=new ItemStack(LbcWands.STONEKIVI);var data=LbcFartus.data(trialStack);data.putInt("upgradable",1);trialStack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));gamma.setItemInHand(InteractionHand.OFF_HAND,trialStack);
        var impact=base.center.offset(7,3,0);write(impact,Blocks.BEDROCK);
        // Positive native control: a real opponent in a solid block completes this exact source trial.
        // A fresh independent stack then owns the pending replay test, where WILD snake work can
        // legitimately change the blocks/target before TrialWork runs.
        var pendingStack=trialStack.copy();var feet=base.owner.blockPosition();var before=gamma.level().getBlockState(feet);
        try{
            write(feet,Blocks.STONE);game.items().academy().stonekiviTrial(gamma,InteractionHand.OFF_HAND,trialStack,impact);
            require(LbcFartus.data(trialStack).getIntOr("upgrade_available",0)==1&&!LbcAcademy.trial(trialStack),"actual source trial grants upgrade with a real trapped hostile player");
        }finally{gamma.level().setBlock(feet,before,Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);}
        trialStack=pendingStack;gamma.setItemInHand(InteractionHand.OFF_HAND,trialStack);
        try{
            // The actual source produces its own TrialWork; its empty template phases are replaced
            // by bounded source factory phases before any native dispatcher turn can run.
            set(stone,"templates",Map.of());
            invoke(stone,"castAt",new Class<?>[]{ServerPlayer.class,InteractionHand.class,ItemStack.class,int.class,Vec3.class,Vec3.class,boolean.class},gamma,InteractionHand.OFF_HAND,trialStack,1,Vec3.atCenterOf(impact).add(0,0,-3),new Vec3(0,0,1),true);
            stoneJob=queue().getLast();trial=children(stoneJob).getLast();require(trial.getClass().getSimpleName().equals("TrialWork"),"actual accepted Stonekivi trial terminal");
            set(stone,"templates",Map.of(id,parts));
            stoneLast=base.center.offset(11,1,0);stoneFirst=stoneLast.west();stoneFuture=base.center.offset(-15,9,-2);
            stoneSlices=template(gamma,id,stoneFirst);
            require(children(stoneSlices).size()==2,"two genuine native Stonekivi template placements");
            require(children(stoneSlices).removeFirst().step(),"actual first source slice completed before capture");
            require(gamma.level().getBlockState(stoneFirst).is(Blocks.STONE)&&gamma.level().getBlockState(stoneLast).isAir(),"only first explicit partial slice was placed");
            placedSlice=new ObservedWork(children(stoneSlices).removeFirst(),stoneLast);children(stoneSlices).add(placedSlice);
            trialCall=new ObservedWork((LbcStonekivi.Work)trial,null);
            futureStone=template(gamma,id,stoneFuture);
            children(stoneJob).clear();children(stoneJob).add((LbcStonekivi.Work)stoneSlices);children(stoneJob).add(trialCall);
        }finally{set(stone,"templates",prepared);}
        stoneChildren=List.copyOf(children(stoneJob));sliceChildren=List.copyOf(children(stoneSlices));

        // Advance one real flood node in an isolated two-cell original topology. Its halo is air,
        // so this native cast cannot grow spikes or reach a prior terrain/body control.
        magFirst=base.center.offset(8,9,12);magLast=magFirst.east();magFuture=magLast.east(2);
        for(var p:BlockPos.betweenClosed(magFirst.offset(-1,-1,-1),magLast.offset(1,1,1)))write(p,Blocks.AIR);
        write(magFirst,Blocks.BEDROCK);write(magLast,Blocks.BEDROCK);write(magFuture,Blocks.BEDROCK);
        magmify=LbcMagmify.create(game,gamma,magFirst);var flood=VanityTerrainScene.<LongArrayFIFOQueue>get(magmify,"frontier");require(flood.dequeueLong()==magFirst.asLong(),"actual first native flood FIFO node");
        game.items().indirectKills().trapChanges(gamma.level(),gamma,()->invoke(magmify,"transformNode",new Class<?>[]{long.class},magFirst.asLong()));
        require(VanityTerrainScene.<Integer>get(magmify,"seen")==1&&flood.size()==1,"actual partial native flood retains one original queued node");stone.queuedWork((LbcStonekivi.Work)magmify);

        areaFirst=base.center.offset(3,6,-14);areaLast=areaFirst.east(2);areaFuture=areaFirst.above();
        for(var p:BlockPos.betweenClosed(areaFirst,areaLast))write(p,Blocks.BEDROCK);write(areaFuture,Blocks.BEDROCK);
        area=new BlockAreaWork(gamma.level(),s->s.is(Blocks.BEDROCK),(p,s)->Blocks.GOLD_BLOCK.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        area.include(bounds(areaFirst,areaLast));area.step(game,gamma,1,Long.MAX_VALUE);require(area.queuedBlockCount()==2,"actual bounded owned-work partial bitmap leaves two cells");
        state=new LbcBolts.Owned(gamma,Vec3.atCenterOf(areaFirst),game.items().epoch());
        blockJob=invoke(bolts,"blockWork",new Class<?>[]{LbcBolts.Owned.class,BlockAreaWork.class},state,area);
        pasteFirst=base.center.offset(-3,8,-14);pasteLast=pasteFirst.east();
        var boltPrepared=VanityTerrainScene.<Map<Identifier,List<MapManager.TemplateSlice>>>get(bolts,"templates");
        try{set(bolts,"templates",Map.of(id,parts));boltTemplate=boltTemplate(state,id,pasteFirst);}finally{set(bolts,"templates",boltPrepared);}
        var ownChildren=new ArrayDeque<LbcStonekivi.Work>();ownChildren.add((LbcStonekivi.Work)blockJob);ownChildren.add((LbcStonekivi.Work)boltTemplate);
        ownedJob=invoke(bolts,"sequence",new Class<?>[]{LbcBolts.Owned.class,ArrayDeque.class},state,ownChildren);ownedChildren=List.copyOf(ownChildren);stone.queuedWork((LbcStonekivi.Work)ownedJob);

        // The real Ultra source captures64 arrows once. Consume its first unit natively, retaining
        // the same input job and convertedArrowStack bit: the remaining63 must not roll again.
        var at=base.at.add(9,7,-14);arrows=new ItemEntity(gamma.level(),at.x,at.y,at.z,new ItemStack(Items.ARROW,64));arrows.setNoGravity(true);arrows.setPickUpDelay(10000);arrows.entityTags().add("sin_gluttony");require(gamma.level().addFreshEntity(arrows),"native Ultra64 input entity");
        invoke(bolts,"reforge",new Class<?>[]{ServerPlayer.class,Vec3.class,boolean.class},gamma,at,true);reforgeJob=queue().getLast();require(reforgeJob.getClass().getSimpleName().equals("ReforgeJob"),"actual native Ultra reforge producer");
        input=(LbcBolts.ReforgeInput)ownedChildren(reforgeJob).getFirst();require(!input.step()&&input.remaining==63&&input.converted==1&&input.convertedArrowStack,"actual first Ultra roll consumes exactly one arrow");
        var products=new ArrayList<ItemEntity>();for(var entity:gamma.level().getAllEntities())if(entity instanceof ItemEntity item&&item!=arrows&&item.position().distanceToSqr(at)<1){products.add(item);item.setPos(at.add(3,0,0));item.setNoGravity(true);item.setPickUpDelay(10000);item.entityTags().add("sin_gluttony");}
        output=List.copyOf(products);require(!output.isEmpty(),"native exact64 ammo roll emitted real items");outputValues=output.stream().map(e->e.getItem().copy()).toList();

        // Native producers populate all private Bolts actor families. Explicit terminal ages keep
        // their resumed lifecycle proof bounded and prevent an unrelated lightning/meteor attack.
        bolts.homingField(gamma,20);boltStates=bolts.snapshot();for(var actor:boltStates){actor.position=base.at.add(0,10,-14);actor.age=20;}
        invoke(bolts,"wateringAt",new Class<?>[]{ServerPlayer.class,Vec3.class},gamma,base.at.add(0,6,-12));waterStates=bolts.waterSnapshot();for(var actor:waterStates){actor.position=base.at.add(0,6,-12);actor.age=200;}
        invoke(bolts,"meteor",new Class<?>[]{ServerPlayer.class,Vec3.class},gamma,base.at.add(0,10,-14));meteor=bolts.meteorSnapshot().getFirst();meteor.age=17;meteor.impactVisualTicks=2;meteor.display.setYRot(13);meteor.display.setXRot(23);
        chainTarget=new Cow(EntityTypes.COW,gamma.level());chainTarget.setPos(base.at.add(10,3,-4));chainTarget.setNoAi(true);chainTarget.setNoGravity(true);chainTarget.entityTags().addAll(Set.of("sin_gluttony","have_stand"));require(gamma.level().addFreshEntity(chainTarget),"isolated native chain target");
        bolts.libraryChainArea(gamma,chainTarget.position(),.6);chain=bolts.chainSnapshot().getFirst();chain.untilNext=100;
        require(bolts.acceptStatus(chainTarget,new MobEffectInstance(MobEffects.RAID_OMEN,3,124),gamma)&&bolts.acceptStatus(chainTarget,new MobEffectInstance(MobEffects.RAID_OMEN,7,124),gamma),"two actual ordered native chain-status admissions");
        chainStatus=get(bolts,"chainStatus");admittedStatus=List.copyOf(statusQueue());currentStatus=admittedStatus;statusCauses=admittedStatus.stream().map(c->VanityTerrainScene.<LbcIndirectKills.Owner>get(c,"cause")).toList();
        jobs=List.of(stoneJob,magmify,ownedJob,reforgeJob);
    }

    ArrayDeque<Object> statusQueue(){return VanityTerrainScene.<Map<Object,ArrayDeque<Object>>>get(chainStatus,"pending").get(chainTarget);}
    @SuppressWarnings("unchecked") CompoundTag attachment(){return chainTarget.getAttached(VanityTerrainScene.<net.fabricmc.fabric.api.attachment.v1.AttachmentType<CompoundTag>>get(chainStatus,"DATA"));}

    List<Object> extraJobs(){return jobs;}
    void arm(){arm(List.of());}
    void arm(List<Object> laterJobs){
        base.other.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE).setBaseValue(64);
        // Native range keeps this real distant chest valid during current-body pairing.
        // The normal native menu/session and physical BE roots remain in use.
        var provider=base.other.level().getBlockState(chest).getMenuProvider(base.other.level(),chest);
        require(provider!=null&&base.other.openMenu(provider).isPresent()&&base.other.containerMenu instanceof ChestMenu,"native double chest menu opens");
        var menu=(ChestMenu)base.other.containerMenu;require(menu.getContainer().getContainerSize()==54,"native joined54-slot backing");
        sourceMenuSlot=-1;for(int slot=0;slot<54;slot++)if(menu.getContainer().getItem(slot)==((Container)base.other.level().getBlockEntity(chest)).getItem(0)){require(sourceMenuSlot==-1,"one physical source slot in the joined menu");sourceMenuSlot=slot;}
        require(sourceMenuSlot==0||sourceMenuSlot==27,"native menu/physical chest source slot already aliases");
        order=List.copyOf(queue());int end=order.size()-laterJobs.size();require(order.subList(end-jobs.size(),end).equals(jobs)&&order.subList(end,order.size()).equals(laterJobs),"native terrain prefix and later job membership/order appended exactly");
        originalCells=new HashMap<>();expectedCells=new HashMap<>();VanityTerrainScene.<Map<Long,Object>>get(magmify,"observed").forEach((key,cell)->{originalCells.put(key,get(cell,"original"));expectedCells.put(key,get(cell,"expected"));});
        discovered=Set.copyOf(VanityTerrainScene.<Set<Long>>get(magmify,"discovered"));surface=Set.copyOf(VanityTerrainScene.<Set<Long>>get(magmify,"surfaceBlocks"));frontier=(long[])invoke(get(magmify,"frontier"),"image",new Class<?>[]{});reservoir=VanityTerrainScene.<long[]>get(magmify,"centerSample").clone();
        seen=get(magmify,"seen");magPhase=get(magmify,"phase");var random=VanityTerrainScene.<RandomSource>get(magmify,"random");var entropy=MatchRandomState.capture(random);randomNext=random.nextLong();entropy.restore(random);
        boltTicks=get(bolts,"ticks");reforgeRemaining=input.remaining;reforgeUnits=input.units;reforgeConverted=input.converted;chainDelay=chain.untilNext;
        statusAttachment=Objects.requireNonNull(attachment()).copy();require(bolts.statusCount(chainTarget)==10,"accepted native FIFO total captured");
    }
    void preflight(MatchDamageSources sources){
        stone.beginNativeWork();boolean rejected=false;try{new MatchQueuedWorkCheckpoint(game,sources);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("shared terrain callbacks");}finally{stone.endNativeWork();}
        require(rejected&&List.copyOf(queue()).equals(order)&&input.converted==1&&area.queuedBlockCount()==2,"active native terrain callback rejects without placement/reforge/job mutation");
        var detached=new ItemEntity(base.owner.level(),base.at.x,base.at.y+8,base.at.z,new ItemStack(Items.ARROW,64));
        var probe=(LbcBolts.ReforgeInput)invoke(bolts,"reforgeInput",new Class<?>[]{ItemEntity.class,LbcBolts.ReforgeFamily.class,int.class,ServerPlayer.class,java.util.function.Consumer.class},detached,LbcBolts.ReforgeFamily.ULTRA,64,base.other,null);rejected=false;
        try{probe.checkpoint(new MatchQueuedWorkCheckpoint.Graph(sources));}catch(IllegalStateException expected){rejected=expected.getMessage().contains("Detached reforge input");}
        require(rejected&&detached.getItem().getCount()==64&&probe.converted==0&&List.copyOf(queue()).equals(order),"detached native input rejects before rolling/debit or changing registered jobs");
    }
    void verify(ServerPlayer gamma){
        require(List.copyOf(queue()).equals(order)&&children(stoneJob).stream().toList().equals(stoneChildren)&&children(stoneSlices).stream().toList().equals(sliceChildren)&&ownedChildren(ownedJob).stream().toList().equals(ownedChildren),"same whole dispatcher and independent original nested sequence cursors");
        require(get(trial,"owner")==gamma&&get(trial,"stack")==gamma.getOffhandItem()&&LbcAcademy.trial(gamma.getOffhandItem())&&get(placedSlice.actual,"owner")==gamma&&placedSlice.completions==0&&trialCall.completions==0,"actual trial and native template bind current author/canonical hand without completion replay");
        require(base.owner.level().getBlockState(stoneFirst).is(Blocks.STONE)&&base.owner.level().getBlockState(stoneLast).isAir()&&base.owner.level().getBlockState(stoneFuture).isAir(),"captured first slice remains; pending/future slices did not execute on restoration");
        require(get(magmify,"owner")==gamma&&get(magmify,"phase")==magPhase&&VanityTerrainScene.<Integer>get(magmify,"seen")==seen&&Arrays.equals((long[])invoke(get(magmify,"frontier"),"image",new Class<?>[]{}),frontier)&&Arrays.equals(VanityTerrainScene.<long[]>get(magmify,"centerSample"),reservoir),"same flood owner, phase, FIFO and independent partial reservoir");
        var cells=VanityTerrainScene.<Map<Long,Object>>get(magmify,"observed");require(cells.keySet().equals(originalCells.keySet())&&cells.entrySet().stream().allMatch(e->get(e.getValue(),"original")==originalCells.get(e.getKey())&&get(e.getValue(),"expected")==expectedCells.get(e.getKey()))&&VanityTerrainScene.<Set<Long>>get(magmify,"discovered").equals(discovered)&&VanityTerrainScene.<Set<Long>>get(magmify,"surfaceBlocks").equals(surface),"original observations/acknowledged expected writes and flood membership detach from future mutations");
        var random=VanityTerrainScene.<RandomSource>get(magmify,"random");var entropy=MatchRandomState.capture(random);try{require(random.nextLong()==randomNext,"private cast RNG resumes exact saved stream");}finally{entropy.restore(random);}
        require(state.owner==gamma&&area.queuedBlockCount()==2&&get(blockJob,"work")==area&&ownedChildren(boltTemplate).size()==2,"same Bolts owner/bitmap/template aliases restore");
        require(input.item==arrows&&input.input==arrows.getItem()&&input.recipient==gamma&&input.remaining==reforgeRemaining&&input.units==reforgeUnits&&input.converted==reforgeConverted&&input.convertedArrowStack&&arrows.getItem().getCount()==63&&ownedChildren(reforgeJob).getFirst()==input,"original registered Ultra job and canonical native input/recipient restore exact partial64 state");
        require(VanityTerrainScene.<Map<Object,ServerPlayer>>get(bolts,"reforgeJobs").equals(Map.of(reforgeJob,gamma))&&VanityTerrainScene.<Integer>get(bolts,"ticks")==boltTicks,"native owner registry alias and private Bolts cadence restored");
        require(bolts.snapshot().equals(boltStates)&&boltStates.stream().allMatch(s->s.owner==gamma&&s.age==20&&s.position.equals(base.at.add(0,10,-14)))&&bolts.waterSnapshot().equals(waterStates)&&waterStates.stream().allMatch(s->s.owner==gamma&&s.age==200),"all native Bolt/Water private memberships/current authors/terminal ages restore");
        require(bolts.meteorSnapshot().equals(List.of(meteor))&&meteor.owner==gamma&&meteor.age==17&&meteor.impactVisualTicks==2&&!meteor.display.isRemoved()&&meteor.display.getYRot()==13&&meteor.display.getXRot()==23,"same canonical native meteor display/private impact phase");
        require(bolts.chainSnapshot().equals(List.of(chain))&&chain.owner==gamma&&chain.current.equals(Set.of(chainTarget))&&chain.untilNext==chainDelay,"same native chain/current target membership and independent remaining countdown");
        currentStatus=List.copyOf(statusQueue());require(currentStatus.size()==2&&VanityTerrainScene.<Integer>get(currentStatus.get(0),"remaining")==3&&VanityTerrainScene.<Integer>get(currentStatus.get(1),"remaining")==7&&get(currentStatus.get(0),"cause")==statusCauses.get(0)&&get(currentStatus.get(1),"cause")==statusCauses.get(1)&&statusCauses.stream().allMatch(c->c.player().equals(gamma.getUUID())&&c.friendlyFire())&&statusCauses.get(0).order()<statusCauses.get(1).order(),"independent FIFO quantities/order and original accepted source-cause aliases restore without new admission");
        require(bolts.statusCount(chainTarget)==10&&game.items().mana().hudValue(chainTarget,"effect_chain_lightning")==10&&statusAttachment.equals(attachment()),"private chain FIFO matches canonical native attachment/HUD without restore sync or pulse");
        require(base.other.containerMenu instanceof ChestMenu&&base.other.containerMenu.containerId>0,"native double-chest reopens through its fresh published window id: "+base.other.containerMenu.getClass().getName()+"/"+base.other.containerMenu.containerId);
        var menu=(ChestMenu)base.other.containerMenu;var physical=(Container)base.other.level().getBlockEntity(chest);require(menu.getContainer().getContainerSize()==54&&menu.getContainer().getItem(sourceMenuSlot)==physical.getItem(0)&&game.items().advancedSnow().snapshot().getFirst().stack()==physical.getItem(0)&&((Container)base.other.level().getBlockEntity(otherChest)).getItem(8).getCount()==11,"private source, actual54-slot menu and physical BE slot share one current mutable stack");
        for(int i=0;i<output.size();i++)require(!output.get(i).isRemoved()&&ItemStack.matches(output.get(i).getItem(),outputValues.get(i)),"restoration performs no duplicate reforge roll/output "+i);
    }
    boolean ready(){return jobs.stream().noneMatch(queue()::contains)&&input.converted==64&&bolts.meteorCount()==0;}
    void beforeThaw(){require(bolts.statusCount(chainTarget)==10,"full saved chain FIFO verified before each native thaw");bolts.clearChainStatus(chainTarget);require(bolts.statusCount(chainTarget)==0&&attachment()==null,"withdraw fixture-only chain status after frozen proof before global damaging pulse");}
    void thawed(){
        var gamma=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestGamma"));
        require(ready()&&placedSlice.completions==1&&trialCall.completions==1&&get(trial,"stack")==gamma.getOffhandItem(),"native placement and actual current-hand trial callback each execute exactly once per original/replayed timeline");
        int upgrade=LbcFartus.data(gamma.getOffhandItem()).getIntOr("upgrade_available",0);
        require((upgrade==0&&LbcAcademy.trial(gamma.getOffhandItem()))||(upgrade==1&&!LbcAcademy.trial(gamma.getOffhandItem())),"source trial state stays coherent under later legitimate WILD terrain interference");
        require(input.converted==64&&input.remaining==0&&input.units==0&&arrows.isRemoved()&&bolts.pendingReforgeCount()==0,"actual Ultra consumes remaining63 without a second ammo roll");
        require(base.owner.level().getBlockState(magLast).is(Blocks.REINFORCED_DEEPSLATE)&&get(magmify,"phase").toString().equals("FINISHED")&&base.owner.level().getBlockState(magFuture).is(Blocks.BEDROCK),"actual remaining flood node finishes and abandoned future discovery never executes: first="+base.owner.level().getBlockState(magFirst)+", last="+base.owner.level().getBlockState(magLast)+", future="+base.owner.level().getBlockState(magFuture)+", phase="+get(magmify,"phase")+", seen="+get(magmify,"seen"));
        for(var pos:BlockPos.betweenClosed(areaFirst,areaLast))require(base.owner.level().getBlockState(pos).is(Blocks.GOLD_BLOCK),"resumed original owned bitmap cell "+pos);
        require(base.owner.level().getBlockState(areaFuture).is(Blocks.BEDROCK)&&base.owner.level().getBlockState(stoneFuture).isAir()&&base.owner.level().getBlockState(pasteFirst).is(Blocks.STONE)&&base.owner.level().getBlockState(pasteLast).is(Blocks.DIAMOND_BLOCK),"native owned templates finish only accepted cells, discarding future extensions");
        require(bolts.snapshot().isEmpty()&&bolts.waterSnapshot().isEmpty()&&bolts.chainSnapshot().equals(List.of(chain))&&chain.untilNext<chainDelay,"native bounded actor expiry/countdown resumes saved private values");
        for(int i=0;i<output.size();i++)require(ItemStack.matches(output.get(i).getItem(),outputValues.get(i)),"saved exact64 ammo output remains unchanged without replaying the first roll");
        var inputAt=base.at.add(9,7,-14);for(var entity:base.owner.level().getAllEntities())require(!(entity instanceof ItemEntity)||entity.position().distanceToSqr(inputAt)>=1,"remaining63 Ultra steps emit no second native ammo output");
        base.other.closeContainer();bolts.stop();
    }
    void mutate(){
        placedSlice.completions=99;trialCall.completions=99;
        children(stoneJob).add((LbcStonekivi.Work)futureStone);children(stoneSlices).clear();ownedChildren(ownedJob).clear();ownedChildren(boltTemplate).clear();area.include(bounds(areaFuture,areaFuture));
        var cells=VanityTerrainScene.<Map<Long,Object>>get(magmify,"observed");set(cells.get(magFirst.asLong()),"expected",Blocks.DIRT.defaultBlockState());VanityTerrainScene.<long[]>get(magmify,"centerSample")[0]=magFuture.asLong();var fifo=VanityTerrainScene.<LongArrayFIFOQueue>get(magmify,"frontier");fifo.clear();fifo.enqueue(magFuture.asLong());VanityTerrainScene.<RandomSource>get(magmify,"random").nextLong();set(magmify,"seen",99);
        input.remaining=99;input.units=99;input.converted=99;input.convertedArrowStack=false;chain.untilNext=999;meteor.age=999;meteor.impactVisualTicks=99;
        for(var actor:boltStates)actor.age=999;for(var actor:waterStates)actor.age=999;
        for(var row:currentStatus)set(row,"remaining",99);var future=new ArrayDeque<>(currentStatus);future.addFirst(currentStatus.getLast());VanityTerrainScene.<Map<Object,ArrayDeque<Object>>>get(chainStatus,"pending").put(chainTarget,future);
        ((Container)base.owner.level().getBlockEntity(chest)).setItem(0,new ItemStack(Items.STICK,5));((Container)base.owner.level().getBlockEntity(otherChest)).setItem(8,new ItemStack(Items.DIAMOND,64));
    }
    void pending(){require(placedSlice.completions==99&&trialCall.completions==99&&bolts.count()==0&&input.converted==99&&VanityTerrainScene.<Integer>get(magmify,"seen")==99&&chain.untilNext==999&&statusQueue().size()==3&&VanityTerrainScene.<Integer>get(statusQueue().getFirst(),"remaining")==99,"offline current Gamma defers ALL native terrain/bolt metadata without restoring any partial cursor or source callback");}
}
