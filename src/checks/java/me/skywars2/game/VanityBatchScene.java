package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real accepted Gluttony cast, explicit partial cell/ray cursors and native FPV tracker/seat replay. */
final class VanityBatchScene {
    final GameController game;final ChefChecks.Fixture base;final LbcWitchGluttony gluttony;final LbcFpvDrone fpv;
    final Entity rabbit;final LbcFpvDrone.Drone drone;final Object ray,work;final BlockAreaWork area;
    final BlockPos region,witness;final Vec3 cursor;final double left;final LbcSpellSource.Source policy;
    int age,cadence,frame,bornAge,lastAge;long blasts;boolean attached;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity batch: "+why);}
    static <T>T get(Object object,String field){return VanitySpellScene.get(object,field);}
    static void set(Object object,String field,Object value){VanitySpellScene.set(object,field,value);}
    static Object invoke(Object object,String method,Class<?>[] types,Object...args){try{var m=object.getClass().getDeclaredMethod(method,types);m.setAccessible(true);return m.invoke(object,args);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    static ItemStack item(String flag){var stack=new ItemStack(Items.CARROT_ON_A_STICK,2);var data=new CompoundTag();data.putInt(flag,1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;}
    VanityBatchScene(GameController game,ChefChecks.Fixture base,ServerPlayer gamma){
        this.game=game;this.base=base;gluttony=game.items().witchGluttony();fpv=game.items().fpvDrone();
        require(gluttony.snapshot().isEmpty()&&fpv.count()==0&&fpv.ticketCount()==0&&game.items().exasperato().pendingSearches()==0,"exclusive Gluttony/ray/FPV source owners");
        gamma.entityTags().add("sin_gluttony");gamma.setYRot(0);gamma.setXRot(-25);
        // A genuine source cast owns the typed terminal. Only its partial cursor is explicitly seeded.
        gamma.setItemInHand(InteractionHand.MAIN_HAND,item("snake"));require(gluttony.use(gamma,InteractionHand.MAIN_HAND),"actual target-free snake cast accepted");
        ray=searches().getFirst();var direction=VanityBatchScene.<Vec3>get(ray,"direction");
        cursor=VanityBatchScene.<Vec3>get(ray,"position").add(direction.scale(1.25));left=148.75;set(ray,"position",cursor);set(ray,"remaining",left);
        for(int y=3;y<=6;y++)for(int x=4;x<=6;x++)write(base.center.offset(x,y,5),Blocks.BEDROCK);
        rabbit=gluttony.spawn(LbcWitchGluttony.Kind.RABBIT,gamma.level(),base.at.add(-12,2,-12),0,0,gamma.getUUID(),game.items().epoch(),game.items().indirectKills().wildCause(gamma));
        require(rabbit instanceof Mob,"native registered rabbit");((Mob)rabbit).setNoAi(true);rabbit.setNoGravity(true);policy=game.items().spellSources().source(rabbit);
        var state=VanityBatchScene.<Map<Entity,Object>>get(gluttony,"actors").get(rabbit);set(state,"ticks",3);
        region=base.center.offset(-10,2,-8);witness=base.center.offset(-13,2,-8);
        for(var pos:BlockPos.betweenClosed(region.offset(-1,-1,-1),region.offset(1,1,1)))write(pos,Blocks.BEDROCK);write(witness,Blocks.BEDROCK);
        invoke(gluttony,"fill",new Class<?>[]{Entity.class,state.getClass(),Vec3.class,int.class,boolean.class,boolean.class},rabbit,state,Vec3.atCenterOf(region),1,false,true);
        work=VanityBatchScene.<Set<?>>get(gluttony,"terrain").iterator().next();area=get(work,"area");
        area.step(game,null,policy.cause(),5,Long.MAX_VALUE);require(area.queuedBlockCount()==22,"explicit partial native cell cursor leaves exactly22/27 cells");
        gamma.setItemInHand(InteractionHand.MAIN_HAND,item("drone_pult"));var stack=gamma.getMainHandItem();
        boolean accepted=fpv.use(gamma,InteractionHand.MAIN_HAND);require(accepted&&stack.getCount()==1,"real pult creates independently controlled20HP actor and debits once: accepted="+accepted+" count="+stack.getCount()+" alive="+gamma.isAlive()+" role="+game.canUseGameplayItems(gamma)+" controlled="+fpv.controlling(gamma)+" passenger="+gamma.isPassenger()+" actionsBlocked="+game.items().actionsBlocked(gamma)+" stopped="+game.items().timeStop().stopped(gamma.level())+" at="+gamma.position());drone=fpv.snapshot().getFirst();
        gamma.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
    }
    void write(BlockPos pos,Block block){require(base.blocks.containsKey(pos),"bounded native source terrain "+pos);base.owner.level().setBlock(pos,block.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);}
    boolean ready(ServerPlayer gamma){var s=VanityBatchScene.<Map<UUID,Object>>get(fpv,"controllers").get(gamma.getUUID());return s!=null&&VanityBatchScene.<Boolean>get(s,"attached");}
    void arm(ServerPlayer gamma){
        require(ready(gamma),"native tracker paired drone before initial camera capture");require(fpv.look(gamma,new ServerboundMovePlayerPacket.Rot(87,-17,false,false)),"real controller consumes look independently of body");
        age=get(VanityBatchScene.<Map<Entity,Object>>get(gluttony,"actors").get(rabbit),"ticks");cadence=get(gluttony,"phase");frame=game.server().getTickCount();
        var s=VanityBatchScene.<Map<UUID,Object>>get(fpv,"controllers").get(gamma.getUUID());bornAge=frame-VanityBatchScene.<Integer>get(s,"born");lastAge=frame-VanityBatchScene.<Integer>get(fpv,"lastTick");blasts=fpv.explosions();attached=get(s,"attached");
    }
    void preflight(ServerPlayer gamma,MatchDamageSources sources){
        var queue=VanityBatchScene.<ArrayDeque<LbcStonekivi.Work>>get(game.items().stonekivi(),"work");LbcStonekivi.Work opaque=()->true;queue.addLast(opaque);boolean rejected=false;
        try{new MatchQueuedWorkCheckpoint(game,sources);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("Missing shared-work checkpoint adapter");}
        require(rejected&&queue.getLast()==opaque&&area.queuedBlockCount()==22,"unsupported callback rejects before any job mutation");require(queue.removeLast()==opaque,"remove only fixture-owned opaque callback");
        var future=game.items().exasperato().searchSpell(gamma,gamma.getEyePosition(),new Vec3(0,1,0),5,false,(pos,state)->false,e->false,null);
        var searches=searches();var untyped=searches.stream().filter(ray->VanityBatchScene.<Object>get(ray,"result")==future).findFirst().orElseThrow();rejected=false;
        try{new MatchQueuedWorkCheckpoint(game,sources);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("Missing typed continuation");}
        require(rejected&&!future.isDone()&&searches().contains(untyped)&&area.queuedBlockCount()==22,"opaque external future rejects without dropping/completing it or mutating earlier jobs");
        // The current scheduler has owner lanes, rather than one ArrayDeque.
        // Remove just this fixture's fresh search without servicing or reordering saved work.
        var scheduler=get(game.items().exasperato(),"searches");
        var lane=VanityBatchScene.<Map<UUID,Object>>get(scheduler,"owners").get(gamma.getUUID());
        var fresh=VanityBatchScene.<ArrayDeque<?>>get(lane,"fresh");
        require(fresh.remove(untyped)&&!fresh.isEmpty(),"remove only fixture-owned untyped search; retain original owner lane");
        set(scheduler,"size",VanityBatchScene.<Integer>get(scheduler,"size")-1);future.cancel(false);
    }
    private List<?> searches(){return java.util.stream.StreamSupport.stream(VanityBatchScene.<Iterable<?>>get(game.items().exasperato(),"searches").spliterator(),false).toList();}
    void verify(ServerPlayer gamma){verify(gamma,0);}
    void verify(ServerPlayer gamma,int extra){verify(gamma,extra,List.of());}
    void verify(ServerPlayer gamma,int extra,List<Object> extraWork){
        var actors=VanityBatchScene.<Map<Entity,Object>>get(gluttony,"actors");require(actors.size()==1&&actors.containsKey(rabbit)&&VanityBatchScene.<Integer>get(actors.get(rabbit),"ticks")==age&&VanityBatchScene.<Object>get(actors.get(rabbit),"source")==policy,"Gluttony actor source identity and age restored");
        require(VanityBatchScene.<Integer>get(gluttony,"phase")==cadence&&VanityBatchScene.<Set<?>>get(gluttony,"pending").equals(Set.of(LbcWitchGluttony.Kind.SNAKE)),"logical cadence and pending cast restored");
        require(VanityBatchScene.<Set<?>>get(gluttony,"terrain").equals(Set.of(work))&&area.queuedBlockCount()==22&&!VanityBatchScene.<Boolean>get(work,"cancelled"),"same owned terrain job and independent22-cell bitmap return");
        var queue=VanityBatchScene.<ArrayDeque<?>>get(game.items().stonekivi(),"work");var expectedWork=new ArrayList<Object>();expectedWork.add(work);expectedWork.addAll(extraWork);require(List.copyOf(queue).equals(expectedWork),"dispatcher membership aliases exactly the original owner jobs in captured order");
        var searches=searches();require(searches.size()==1+extra&&searches.getFirst()==ray&&VanityBatchScene.<Vec3>get(ray,"position").equals(cursor)&&VanityBatchScene.<Double>get(ray,"remaining")==left&&!VanityBatchScene.<Boolean>get(ray,"finished")&&VanityBatchScene.<Object>get(ray,"owner")==gamma,"typed ray terminal, current caster and exact partial position replay without completed future reset: count="+searches.size()+" expected="+(1+extra)+" index="+searches.indexOf(ray)+" position="+VanityBatchScene.get(ray,"position")+" expectedPosition="+cursor+" left="+VanityBatchScene.get(ray,"remaining")+" expectedLeft="+left+" finished="+VanityBatchScene.get(ray,"finished")+" sameOwner="+(VanityBatchScene.get(ray,"owner")==gamma));
        require(fpv.snapshot().equals(List.of(drone))&&!drone.retired&&fpv.controlling(gamma)&&fpv.ticketCount()==1&&fpv.explosions()==blasts,"FPV actor/private retirement/current-body controller and unique body ticket restore");
        var s=VanityBatchScene.<Map<UUID,Object>>get(fpv,"controllers").get(gamma.getUUID());require(VanityBatchScene.<Object>get(s,"player")==gamma&&VanityBatchScene.<Object>get(s,"drone")==drone&&VanityBatchScene.<Float>get(s,"yaw")==87&&VanityBatchScene.<Float>get(s,"pitch")==-17,"saved control aim is separate from restored physical body pose");
        require(game.server().getTickCount()-VanityBatchScene.<Integer>get(s,"born")==bornAge&&game.server().getTickCount()-VanityBatchScene.<Integer>get(fpv,"lastTick")==lastAge,"publication wait does not consume saved attach/dedup clocks");
    }
    void published(ServerPlayer gamma,List<Packet<?>> packets){
        int seatId=fpv.cameraSeatId(gamma);
        var seat=packets.stream().filter(ClientboundSetPassengersPacket.class::isInstance).map(ClientboundSetPassengersPacket.class::cast).filter(p->p.getVehicle()==seatId).toList();
        require(seatId>=0&&gamma.level().getEntity(seatId)==null&&!seat.isEmpty()&&Arrays.equals(seat.getLast().getPassengers(),new int[]{gamma.getId()})&&packets.stream().anyMatch(p->p instanceof ClientboundSetCameraPacket c&&c.getEntity(gamma.level())==gamma),"republished transient camera seat uses CURRENT player ID and free self-camera");
        require(gamma.getPostEffects().contains(LbcFpvDrone.HUD)&&packets.stream().anyMatch(p->p instanceof ClientboundGameEventPacket event&&event.getEvent()==ClientboundGameEventPacket.CHANGE_GAME_MODE&&event.getParam()==3),"FPV checkpoint republishes owned HUD and client-only clean view");
        require(!gamma.isPassenger()&&gamma.getCamera()==gamma,"packet-only FPV never replaces native physical vehicle/camera ownership");
    }
    void thawed(){
        require(VanityBatchScene.<Set<?>>get(gluttony,"pending").isEmpty()&&game.items().exasperato().pendingSearches()==0&&VanityBatchScene.<Boolean>get(ray,"finished"),"actual resumed DDA reaches exactly one terminal");
        var snakes=gluttony.snapshot().stream().filter(e->e.entityTags().contains("black_snake")&&!e.entityTags().contains("fragm")).toList();require(snakes.size()==1&&game.items().spellSources().source(snakes.getFirst()).author().equals(drone.author),"one source snake with original author per replay");
        for(var pos:BlockPos.betweenClosed(region.offset(-1,-1,-1),region.offset(1,1,1)))require(base.owner.level().getBlockState(pos).isAir(),"remaining native cells complete "+pos);
        require(base.owner.level().getBlockState(witness).is(Blocks.BEDROCK),"abandoned future extension never reappears in bitmap");
        require(fpv.count()==1&&fpv.ticketCount()==1&&!drone.retired,"native FPV resumes after publication");
        gluttony.stop();fpv.stop();require(fpv.ticketCount()==0,"actual final controller release drops body lease exactly once");
    }
    void mutate(){area.include(new BlockAreaWork.Bounds(witness.getX(),witness.getY(),witness.getZ(),witness.getX(),witness.getY(),witness.getZ()));set(fpv,"explosions",blasts+90);require(drone.retired,"future source retirement occurred");}
    void pending(){require(fpv.count()==0&&fpv.ticketCount()==0&&VanityBatchScene.<Set<?>>get(gluttony,"pending").isEmpty(),"offline caster defers both owners without partial install");}
}
