package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Native accepted terminals; only bounded partial progress is explicitly seeded by the fixture. */
final class VanityTerminalScene {
    final GameController game;final ChefChecks.Fixture base;final LbcAdvancedSnow snow;final LbcChargedSwords charged;final LbcLibraryRandom library;
    final List<Object> rays;final Object clip,pulse,emitter,lava;final BlockAreaWork scan;
    final LbcChargedSwords.ThrownSword sword;final Cow donor;
    final BlockPos brush,lightning,rawWall,lavaFirst,lavaLast,lavaFuture,stackChest;
    Set<UUID> visualViewers;float visualYaw,visualPitch;Vec3 visualOffset;int visualTeleport;
    final UUID sampled=UUID.fromString("d72fc686-a270-4fb7-8cf2-aac51742b5df");
    final Object cause;final Vec3 clipDestination;
    List<Object> order;List<Vec3> positions;List<Double> remaining;List<LbcAdvancedSnow.Cloud> clouds;
    int verifies;int sample,clock,visualClock,emitterAge,charge;boolean entered;int finishingRemaining;
    Vec3 finishingPosition;LbcChargedSwords.FinishingView oldVisual;
    ItemStack theftResult;
    final Set<LightningBolt> terminalBolts=Collections.newSetFromMap(new IdentityHashMap<>());
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity terminals: "+why);}
    static <T>T get(Object object,String field){return VanitySpellScene.get(object,field);}
    static void set(Object object,String field,Object value){VanitySpellScene.set(object,field,value);}
    static Object invoke(Object object,String method,Class<?>[] types,Object...args){return VanityBatchScene.invoke(object,method,types,args);}
    ArrayDeque<Object> queue(){return get(game.items().exasperato(),"searches");}
    void write(BlockPos pos,Block block){require(base.blocks.containsKey(pos),"bounded source cell "+pos);base.owner.level().setBlock(pos,block.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);}
    ItemStack item(String flag,int count){var stack=new ItemStack(Items.CARROT_ON_A_STICK,count);var tag=new CompoundTag();tag.putInt(flag,1);tag.putInt("raycastspell",1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;}
    void select(ServerPlayer p,int slot,ItemStack stack){p.getInventory().setSelectedSlot(slot);p.setItemInHand(InteractionHand.MAIN_HAND,stack);}
    void aim(ServerPlayer p,float pitch){p.setYRot(0);p.setXRot(pitch);}
    void cursor(Object ray,Vec3 at,double left){require(base.bounds.isInside(BlockPos.containing(at)),"bounded pending cursor");set(ray,"position",at);set(ray,"remaining",left);}
    Object newLava(ServerPlayer author,BlockPos center){try{var type=Arrays.stream(LbcLibraryRandom.class.getDeclaredClasses()).filter(c->c.getSimpleName().equals("Lava")).findFirst().orElseThrow();var ctor=type.getDeclaredConstructor(LbcLibraryRandom.class,ServerPlayer.class,BlockPos.class);ctor.setAccessible(true);return ctor.newInstance(library,author,center);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}

    VanityTerminalScene(GameController game,ChefChecks.Fixture base,ServerPlayer gamma){
        this.game=game;this.base=base;snow=game.items().advancedSnow();charged=game.items().chargedSwords();library=game.items().libraryRandom();
        require(snow.snapshot().isEmpty()&&charged.count()==0&&charged.pulseSnapshot().isEmpty()&&library.count()==0,"exclusive additional terminal producers");
        var beta=base.other;var savedClock=beta.getMainHandItem();float yaw=beta.getYRot(),pitch=beta.getXRot();game.items().mana().set(beta,100000);
        var list=new ArrayList<Object>();
        // Ordinary Ender is an accepted cast with a short saved miss, avoiding its harmful arrival.
        select(beta,3,new ItemStack(LbcWands.ADVANCED_ENDER));aim(beta,-90);require(snow.use(beta,InteractionHand.MAIN_HAND,beta.getMainHandItem(),false,false),"real ordinary Ender admission");var ordinary=queue().getLast();list.add(ordinary);cursor(ordinary,base.at.add(-15,10,14),.5);
        select(beta,5,new ItemStack(LbcWands.ACADEMY_ADVANCED_ENDER));var mode=LbcFartus.data(beta.getMainHandItem());mode.putInt("mode",3);beta.getMainHandItem().set(DataComponents.CUSTOM_DATA,CustomData.of(mode));aim(beta,0);
        require(snow.use(beta,InteractionHand.MAIN_HAND,beta.getMainHandItem(),false,true),"real academic wall-clip admission");var academic=queue().getLast();list.add(academic);clip=get(academic,"replay");
        cursor(academic,base.at.add(-14,3,-10),2);set(clip,"entered",true);clipDestination=Vec3.atCenterOf(BlockPos.containing(base.at.add(-14,3,-10)));
        select(beta,7,new ItemStack(LbcWands.ACADEMY_SNOW));require(snow.use(beta,InteractionHand.MAIN_HAND,beta.getMainHandItem(),true,true),"real aimed Academy snow admission");var aimedSnow=queue().getLast();list.add(aimedSnow);
        var snowWall=base.center.offset(0,6,15);write(snowWall,Blocks.BEDROCK);cursor(aimedSnow,base.at.add(0,6.5,12),4);
        select(beta,4,new ItemStack(LbcWands.SNOW));require(snow.use(beta,InteractionHand.MAIN_HAND,beta.getMainHandItem(),true,false),"real ordinary snow creates native cloud");
        var initialCloud=snow.snapshot().getFirst().entity();initialCloud.setPos(base.at.add(0,10,14));initialCloud.entityTags().add("sin_gluttony");
        // A live source wand moved into a physical chest must bind to that restored BE item, not a copy.
        stackChest=base.center.offset(13,2,-12);write(stackChest,Blocks.CHEST);
        ((net.minecraft.world.Container)beta.level().getBlockEntity(stackChest)).setItem(0,beta.getInventory().getItem(4));
        beta.getInventory().setItem(4,new ItemStack(Items.STONE));
        brush=base.center.offset(-14,6,12);lightning=base.center.offset(12,7,12);rawWall=base.center.offset(-16,11,16);
        for(var cell:List.of(brush,lightning,rawWall))write(cell,Blocks.BEDROCK);
        select(beta,1,item("shetka",2));require(LbcRewardTools.use(game,beta,InteractionHand.MAIN_HAND),"real toothbrush producer");var brushRay=queue().getLast();list.add(brushRay);cursor(brushRay,base.at.add(-14,6.5,9),4);
        select(beta,2,item("lightning_bolt",2));require(LbcRewardTools.use(game,beta,InteractionHand.MAIN_HAND),"real lightning tool producer");var boltRay=queue().getLast();list.add(boltRay);cursor(boltRay,base.at.add(12,7.5,9),4);
        donor=new Cow(EntityTypes.COW,beta.level());donor.setPos(base.at.add(-12,1,3));donor.setNoAi(true);donor.setNoGravity(true);donor.entityTags().addAll(Set.of("have_stand","sin_gluttony"));donor.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.GOLD_INGOT,7));require(beta.level().addFreshEntity(donor),"dedicated native donor carrying a complete stack");
        select(beta,0,item("krazha",2));require(LbcRewardTools.use(game,beta,InteractionHand.MAIN_HAND),"real retained launch-hand glove producer");var theft=queue().getLast();list.add(theft);cursor(theft,base.at.add(-12,2,0),5);
        // Whole-inventory raw debit resolves CURRENT Gamma despite the existing FPV input block.
        var raw=item("heal1",2);var rawData=LbcFartus.data(raw);rawData.putInt("spell",1);raw.set(DataComponents.CUSTOM_DATA,CustomData.of(rawData));gamma.getInventory().setItem(6,raw);
        float gammaYaw=gamma.getYRot(),gammaPitch=gamma.getXRot();aim(gamma,0);library.ray(gamma,19,"heal1",raw.getItem());var rawRay=queue().getLast();list.add(rawRay);cursor(rawRay,Vec3.atCenterOf(rawWall).add(0,0,-3),4);
        // The safe library emitter expires before its next ten-tick phantom-production phase.
        library.impact(gamma,base.at.add(-15,9,-15),17);emitter=VanityTerminalScene.<List<Object>>get(library,"emitters").getFirst();set(emitter,"age",1599);
        // Instantiate the production Lava adapter, restricting only its fixture geometry; no ghast wrapper.
        lavaFirst=base.center.offset(14,3,-2);lavaLast=lavaFirst.east(2);lavaFuture=lavaFirst.east(1).above();
        lava=newLava(gamma,base.center.offset(40,3,40));scan=get(lava,"scan");scan.clear();scan.include(new BlockAreaWork.Bounds(lavaFirst.getX(),lavaFirst.getY(),lavaFirst.getZ(),lavaLast.getX(),lavaLast.getY(),lavaLast.getZ()));
        for(var cell:BlockPos.betweenClosed(lavaFirst,lavaLast))write(cell,Blocks.BEDROCK);write(lavaFuture,Blocks.BEDROCK);
        scan.step(game,gamma,1,Long.MAX_VALUE);require(scan.queuedBlockCount()==2,"explicit source Lava partial cursor leaves exactly2/3 bounded cells");game.items().stonekivi().queuedWork((LbcStonekivi.Work)lava);
        // The pulse's original source sample is remote from every existing health control.
        var gammaAt=gamma.position();gamma.setPos(base.at.add(-15,3,12));aim(gamma,-90);game.items().mana().score(gamma,"swordcharged9").set(3);
        invoke(charged,"pulse",new Class<?>[]{ServerPlayer.class},gamma);pulse=VanityTerminalScene.<Set<Object>>get(charged,"pulses").iterator().next();var pulseRay=queue().getLast();list.add(pulseRay);
        set(pulse,"nextSample",3);VanityTerminalScene.<Set<UUID>>get(pulse,"hit").add(sampled);cause=get(pulse,"cause");
        var origin=VanityTerminalScene.<Vec3>get(pulse,"origin");cursor(pulseRay,origin.add(0,3,0),.5);
        gamma.setPos(gammaAt);gamma.setYRot(gammaYaw);gamma.setXRot(gammaPitch);
        // Native thrown body is embedded safely and never reaches its source break/explosion terminal.
        var embedded=base.center.offset(-14,8,8);write(embedded,Blocks.BEDROCK);
        invoke(charged,"spawn",new Class<?>[]{ServerPlayer.class,Vec3.class,float.class,float.class},gamma,Vec3.atCenterOf(embedded),0f,0f);
        sword=charged.snapshot().getFirst().entity();sword.age=17;sword.grounded=true;sword.embeddedBlock=embedded;sword.setDeltaMovement(Vec3.ZERO);sword.entityTags().add("sin_gluttony");sword.entityTags().add("have_stand");
        rays=List.copyOf(list);require(rays.size()==8,"eight distinct additional real producer rays");
        // Keep the source glove in its accepted hand, with the original clocks in the other hand.
        beta.getInventory().setSelectedSlot(0);beta.setItemInHand(InteractionHand.OFF_HAND,savedClock);beta.setYRot(yaw);beta.setXRot(pitch);
    }

    int extraRays(){return rays.size();}
    void arm(){
        // Finish a second real thrown body only at capture: its two-tick display is still live.
        var gamma=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestGamma"));
        invoke(charged,"spawn",new Class<?>[]{ServerPlayer.class,Vec3.class,float.class,float.class},gamma,base.at.add(-14,8,10),0f,0f);
        var ending=charged.snapshot().stream().map(LbcChargedSwords.View::entity).filter(e->e!=sword).findFirst().orElseThrow();invoke(charged,"breakSword",new Class<?>[]{LbcChargedSwords.ThrownSword.class,ServerPlayer.class},ending,gamma);
        oldVisual=charged.finishingSnapshot().getFirst();visualViewers=oldVisual.holder().getWatchingPlayers().stream().map(c->c.player.getUUID()).collect(java.util.stream.Collectors.toSet());visualYaw=oldVisual.display().getYaw();visualPitch=oldVisual.display().getPitch();visualOffset=oldVisual.display().getOffset();visualTeleport=oldVisual.display().getTeleportDuration();finishingRemaining=oldVisual.remainingTicks();finishingPosition=oldVisual.position();
        order=List.copyOf(queue());positions=rays.stream().map(r->VanityTerminalScene.<Vec3>get(r,"position")).toList();remaining=rays.stream().map(r->VanityTerminalScene.<Double>get(r,"remaining")).toList();
        entered=get(clip,"entered");sample=get(pulse,"nextSample");clock=get(charged,"clock");visualClock=get(charged,"visualClock");emitterAge=get(emitter,"age");charge=charged.blazeCharges(gamma);clouds=snow.snapshot();
        require(order.containsAll(rays)&&scan.queuedBlockCount()==2&&charge==3,"all accepted partial source states survive actual Time Stop");
    }
    void preflight(MatchDamageSources sources){
        int depth=get(library,"inProgress");set(library,"inProgress",depth+1);boolean rejected=false;
        try{new MatchQueuedWorkCheckpoint(game,sources);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("LibraryRandom callbacks");}finally{set(library,"inProgress",depth);}
        require(rejected&&List.copyOf(queue()).equals(order)&&scan.queuedBlockCount()==2,"explicit active library callback boundary rejects without sampling/debit or queue mutation");
    }
    void verify(ServerPlayer gamma){
        require(List.copyOf(queue()).equals(order),"actual entire fair-scheduler queue order and membership restored");
        for(int i=0;i<rays.size();i++){var r=rays.get(i);require(get(r,"owner")== (i<6?base.other:gamma)&&!VanityTerminalScene.<Boolean>get(r,"finished")&&positions.get(i).equals(get(r,"position"))&&remaining.get(i).equals(get(r,"remaining")),"independent cursor/remaining/current author "+i);}
        require(VanityTerminalScene.<Boolean>get(clip,"entered")==entered&&get(clip,"stack")==base.other.getInventory().getItem(5),"independent wall-clip entered state and canonical inventory stack alias");
        require(snow.snapshot().size()==clouds.size()&&snow.snapshot().getFirst().entity()==clouds.getFirst().entity()&&snow.snapshot().getFirst().stack()==((net.minecraft.world.Container)base.other.level().getBlockEntity(stackChest)).getItem(0),"same canonical cloud body with actual restored native chest source stack");
        require(get(get(rays.get(5),"replay"),"stack")==base.other.getMainHandItem()&&base.other.getMainHandItem().getCount()==2&&donor.getMainHandItem().getCount()==7,"native theft source glove aliases current launch hand; no restore transfer");
        require(gamma.getInventory().getItem(6).getCount()==2&&library.pendingRayCount()==1,"current Gamma raw inventory restored with no terminal debit");
        require(VanityTerminalScene.<List<?>>get(library,"emitters").equals(List.of(emitter))&&get(emitter,"owner")==gamma&&VanityTerminalScene.<Integer>get(emitter,"age")==emitterAge,"original private emitter identity/current owner/age restore");
        require(get(lava,"owner")==gamma&&scan.queuedBlockCount()==2&&VanityTerminalScene.<ArrayDeque<?>>get(game.items().stonekivi(),"work").contains(lava),"same bounded Lava adapter aliases the shared dispatcher and current author");
        require(charged.snapshot().size()==1&&charged.snapshot().getFirst().entity()==sword&&sword.getOwner()==gamma&&sword.age==17&&sword.grounded&&sword.getDeltaMovement().equals(Vec3.ZERO),"same native charged body with private age/embedded state");
        require(VanityTerminalScene.<Set<?>>get(charged,"pulses").equals(Set.of(pulse))&&get(pulse,"owner")==gamma&&get(pulse,"cause")==cause&&VanityTerminalScene.<Set<UUID>>get(pulse,"hit").equals(Set.of(sampled))&&VanityTerminalScene.<Integer>get(pulse,"nextSample")==sample&&!VanityTerminalScene.<Boolean>get(pulse,"cancelled")&&charged.blazeCharges(gamma)==charge,"original pulse source-cause alias, independent samples/hits and exact charge before terminal");
        require(VanityTerminalScene.<Integer>get(charged,"clock")==clock&&VanityTerminalScene.<Integer>get(charged,"visualClock")==visualClock,"both charged cadence and finishing clock restored without consuming publication wait");
        var visuals=charged.finishingSnapshot();require(visuals.size()==1&&visuals.getFirst().owner()==gamma&&visuals.getFirst().position().equals(finishingPosition)&&visuals.getFirst().remainingTicks()==finishingRemaining,"finishing presentation recreated for current body with exact remaining lifetime");
        var shown=visuals.getFirst();if(++verifies>1)require(shown.holder()!=oldVisual.holder()&&shown.display()!=oldVisual.display(),"retired future holder/display are recreated after actual rewind");require(shown.display().getYaw()==visualYaw&&shown.display().getPitch()==visualPitch&&shown.display().getOffset().equals(visualOffset)&&shown.display().getTeleportDuration()==visualTeleport,"destroyed future visual is recreated with the accepted source pose/interpolation");
        require(shown.holder().getWatchingPlayers().stream().map(c->c.player.getUUID()).collect(java.util.stream.Collectors.toSet()).equals(visualViewers)&&shown.holder().getWatchingPlayers().stream().allMatch(c->game.server().getPlayerList().getPlayer(c.player.getUUID())==c.player),"finishing visual actually republishes its saved observers against CURRENT connections");
        require(base.owner.level().getBlockState(brush).is(Blocks.BEDROCK),"toothbrush terminal never replayed during restore");
    }
    boolean ready(){
        for(var entity:base.owner.level().getAllEntities())if(entity instanceof LightningBolt bolt&&bolt.position().distanceToSqr(Vec3.atLowerCornerOf(lightning))<3)terminalBolts.add(bolt);
        return rays.stream().allMatch(r->VanityTerminalScene.<Boolean>get(r,"finished"))&&charged.pulseSnapshot().isEmpty()&&library.pendingRayCount()==0&&!scan.hasWork()&&charged.finishingModels()==0;
    }
    void thawed(){
        var gamma=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestGamma"));
        require(ready()&&gamma.getInventory().getItem(6).getCount()==1&&base.other.getMainHandItem().getCount()==1&&donor.getMainHandItem().isEmpty(),"real resumed raw debit and full-stack theft execute once: raw="+gamma.getInventory().getItem(6).getCount()+" glove="+base.other.getMainHandItem().getCount()+" donor="+donor.getMainHandItem().getCount());
        var acquired=base.other.getInventory().getItem(6);require(acquired.is(Items.GOLD_INGOT)&&acquired.getCount()==7||ItemStack.matches(acquired,LbcRewardTools.substitute()),"source theft transfers the entire seven-item stack or its unchanged five-percent prank");
        if(theftResult==null)theftResult=acquired.copy();else require(ItemStack.matches(theftResult,acquired),"native stolen components/count and source prank replay identically");
        require(charged.blazeCharges(gamma)==charge-3&&charged.pulseSnapshot().isEmpty(),"real pulse settles exactly one original three-charge debit");
        var ticket=VanityTerminalScene.<LbcExasperato.RayTicket>get(pulse,"ticket");ticket.cancel();require(charged.blazeCharges(gamma)==charge-3,"already completed typed pulse cannot settle twice");
        require(base.owner.level().getBlockState(brush).isAir()&&snow.snapshot().size()==clouds.size()+1,"real toothbrush terminal and one Academy impact cloud");
        require(terminalBolts.size()==1&&terminalBolts.iterator().next().getCause()==base.other,"exactly one native reward lightning with the current source author");
        require(base.other.position().distanceToSqr(clipDestination)<.01,"real restored wall-clip resumes after the saved entered phase");
        require(library.count()==0&&charged.finishingModels()==0&&base.owner.level().getBlockState(lavaFuture).is(Blocks.BEDROCK),"bounded emitter/finishing expiration and abandoned Lava extension discarded");
        snow.stop();library.stop();charged.stop();
    }
    void mutate(){
        terminalBolts.clear();
        set(clip,"entered",false);set(pulse,"nextSample",999);VanityTerminalScene.<Set<UUID>>get(pulse,"hit").clear();set(pulse,"cancelled",true);set(emitter,"age",99);sword.age=99;sword.grounded=false;
        scan.include(new BlockAreaWork.Bounds(lavaFuture.getX(),lavaFuture.getY(),lavaFuture.getZ(),lavaFuture.getX(),lavaFuture.getY(),lavaFuture.getZ()));set(charged,"clock",999);set(charged,"visualClock",999);
    }
    void pending(){
        require(library.pendingRayCount()==0&&library.count()==0&&charged.pulseSnapshot().isEmpty()&&charged.finishingModels()==0&&snow.snapshot().isEmpty(),"offline Gamma defers all new private owners including Beta source cloud");
        require(VanityTerminalScene.<Integer>get(pulse,"nextSample")==999&&VanityTerminalScene.<Integer>get(emitter,"age")==99&&!VanityTerminalScene.<Boolean>get(clip,"entered"),"offline retry installs no earlier hidden sample/emitter/clip values");
    }
}
