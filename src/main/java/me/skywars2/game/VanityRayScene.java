package me.skywars2.game;

import java.util.*;
import me.skywars2.mixin.DroneSpawnerCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.vehicle.minecart.MinecartSpawner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Accepted native producers, explicitly seeded partial cursors, and the same queue/owner aliases. */
final class VanityRayScene {
    final GameController game;final ChefChecks.Fixture base;
    final LbcHohlopitek hohl;final LbcReactive reactive;final LbcHoming homing;final LbcDroneDrops drops;
    final List<Object> rays;final Object bomberAim,rain,destiny,rawCurse;
    final LargeFireball fire;final Cow victim;final MinecartSpawner cart;final LbcExasperato.RayTicket curseTicket;
    final List<Entity> tree;final Set<String> dead;
    List<Object> order;List<Entity> tracked;List<Vec3> cursors;List<Double> left;
    int phase,cooldown,ticks,fireAge,dronePhase,delay;double spin,oldSpin,walked,sample;
    long curseRemaining;String spawnWitness;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity rays: "+why);}
    static <T>T get(Object object,String field){return VanitySpellScene.get(object,field);}
    static void set(Object object,String field,Object value){VanitySpellScene.set(object,field,value);}
    ArrayDeque<Object> queue(){return get(game.items().exasperato(),"searches");}
    DroneSpawnerCheckpointAccess nativeSpawner(){return (DroneSpawnerCheckpointAccess)cart.getSpawner();}
    static Object invoke(Object object,String name,Class<?>[] types,Object...args){return VanityBatchScene.invoke(object,name,types,args);}
    void bounded(Vec3 point){require(base.bounds.isInside(BlockPos.containing(point)),"source remains inside explicit Chef bounds "+point);}
    void protectedActor(Entity actor){bounded(actor.position());actor.setNoGravity(true);actor.entityTags().add("sin_gluttony");actor.entityTags().add("have_stand");if(actor instanceof Mob mob)mob.setNoAi(true);}

    VanityRayScene(GameController game,ChefChecks.Fixture base,ServerPlayer gamma){
        this.game=game;this.base=base;hohl=game.items().hohlopitek();reactive=game.items().reactive();homing=game.items().homing();drops=game.items().droneDrops();
        require(hohl.pendingRayCount()==0&&reactive.pendingRayCount()==0&&homing.pendingRayCount()==0&&homing.count()==0&&drops.snapshot().isEmpty(),"exclusive native ray/ability owners");
        var beta=base.other;var held=beta.getMainHandItem().copy();float yaw=beta.getYRot(),pitch=beta.getXRot();
        beta.setYRot(0);beta.setXRot(-90);beta.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcHohlopitek.ITEM));
        require(hohl.use(beta,InteractionHand.MAIN_HAND),"real Beta technology cast admitted despite Gamma FPV controller");
        var tech=queue().getLast();bomberAim=get(tech,"replay");
        require(reactive.castWall(beta,false),"real unpaid source wall ray admitted");var wall=queue().getLast();
        beta.setItemInHand(InteractionHand.MAIN_HAND,held);beta.setYRot(yaw);beta.setXRot(pitch);
        var miss=base.at.add(-14,8,-14);bounded(miss);
        homing.emitAt(gamma,InteractionHand.MAIN_HAND,ItemStack.EMPTY,1,miss,new Vec3(0,1,0));var launch=queue().getLast();
        victim=new Cow(EntityTypes.COW,gamma.level());victim.setPos(base.at.add(10,2,-7));protectedActor(victim);
        require(gamma.level().addFreshEntity(victim),"native dedicated non-damaging curse target");
        var origin=base.at.add(10,3,-10);bounded(origin);
        homing.emitAt(gamma,InteractionHand.MAIN_HAND,ItemStack.EMPTY,4,origin,new Vec3(0,0,1));var curse=queue().getLast();
        rays=List.of(tech,wall,launch,curse);
        curseTicket=VanityRayScene.<Set<LbcExasperato.RayTicket>>get(homing,"pendingRays").stream().filter(t->get(t,"search")==curse).findFirst().orElseThrow();
        // Accepted casts retain their actual policies/directions. Only pending progress is seeded.
        for(var ray:List.of(tech,wall,launch)){set(ray,"position",miss);set(ray,"remaining",.75d);}
        set(curse,"remaining",5d);set(bomberAim,"walked",2.25d);set(bomberAim,"nextSample",3d);
        VanityRayScene.<Map<ServerPlayer,Integer>>get(hohl,"phases").put(beta,3);
        reactive.setShishCounter(beta,19);VanityRayScene.<Map<ServerPlayer,Integer>>get(reactive,"cooldowns").put(beta,93);
        // A raw source curse is independent of the queued targeted curse and follows CURRENT Gamma.
        game.items().mana().score(gamma,"target").set(17);homing.adoptRawFoodStatuses(gamma);
        rawCurse=VanityRayScene.<Map<Object,Object>>get(homing,"curses").get(gamma);
        require(rawCurse!=null,"actual raw status adoption");set(rawCurse,"remaining",337L);
        fire=(LargeFireball)invoke(homing,"fire",new Class<?>[]{ServerPlayer.class,Vec3.class,int.class,Vec3.class,boolean.class,boolean.class,boolean.class},gamma,miss,0,Vec3.ZERO,true,false,false);
        require(fire!=null,"native zero-power guided fire producer");protectedActor(fire);
        VanityRayScene.<Map<LargeFireball,Integer>>get(homing,"ages").put(fire,7);
        VanityRayScene.<Map<LargeFireball,Object>>get(homing,"steeringTargets").put(fire,gamma);
        // Native mode3 creates the rain. Rebase only its test progress before any native tick.
        invoke(homing,"stars",new Class<?>[]{ServerPlayer.class,Vec3.class,int.class},gamma,base.at,3);
        rain=VanityRayScene.<List<Object>>get(homing,"rains").getFirst();set(rain,"position",base.at.add(0,10,0));set(rain,"remaining",1);
        // This virtual source point writes no terrain. Three ticks expire before it can approach
        // the source's30-block blast distance; all actual native actors stay inside Chef bounds.
        homing.emitAt(gamma,InteractionHand.MAIN_HAND,ItemStack.EMPTY,3,base.at.add(0,50,0),new Vec3(0,1,0));
        destiny=VanityRayScene.<List<Object>>get(homing,"destinies").getFirst();set(destiny,"remaining",3);
        set(homing,"ticks",7);
        // A real token creates the exact bat/cart/host tree. Its source spawn offset is accounted for.
        var at=base.at.add(12,-12,-12);var token=new ItemEntity(gamma.level(),at.x,at.y,at.z,new ItemStack(LbcDroneDropItem.ITEM));token.setThrower(gamma);
        require(drops.spawn(token),"native authored drone token source");token.discard();
        tree=drops.snapshot();require(tree.size()==3,"actual three-body source host graph");tree.forEach(this::protectedActor);
        cart=(MinecartSpawner)tree.stream().filter(MinecartSpawner.class::isInstance).findFirst().orElseThrow();
        require(cart.getVehicle()!=null&&cart.getFirstPassenger()!=null,"native original passenger identities");
        nativeSpawner().skywars2$delay(1003);nativeSpawner().skywars2$spin(12.25);nativeSpawner().skywars2$oldSpin(10.5);
        nativeSpawner().skywars2$next().getEntityToSpawn().putString("vanity_ray_witness","accepted");
        dead=Set.copyOf(VanityRayScene.<Set<String>>get(drops,"deadHosts"));
    }

    int extraRays(){return rays.size();}
    void arm(){
        // The explicit partial cadence belongs at capture, after variable FPV handshake waits.
        set(homing,"ticks",7);
        order=List.copyOf(queue());require(order.containsAll(rays),"all four accepted pending producers survive native freeze");
        cursors=rays.stream().map(r->VanityRayScene.<Vec3>get(r,"position")).toList();left=rays.stream().map(r->VanityRayScene.<Double>get(r,"remaining")).toList();
        phase=VanityRayScene.<Map<ServerPlayer,Integer>>get(hohl,"phases").get(base.other);cooldown=reactive.cooldownTicks(base.other);
        walked=get(bomberAim,"walked");sample=get(bomberAim,"nextSample");ticks=get(homing,"ticks");
        fireAge=VanityRayScene.<Map<LargeFireball,Integer>>get(homing,"ages").get(fire);curseRemaining=get(rawCurse,"remaining");
        tracked=drops.snapshot();dronePhase=Math.floorMod(game.server().getTickCount()-VanityRayScene.<Integer>get(drops,"cadenceOffset"),20);
        delay=nativeSpawner().skywars2$delay();spin=nativeSpawner().skywars2$spin();oldSpin=nativeSpawner().skywars2$oldSpin();spawnWitness=nativeSpawner().skywars2$next().getEntityToSpawn().getStringOr("vanity_ray_witness","");
    }

    void preflight(MatchDamageSources sources){
        var before=List.copyOf(queue());nativeSpawner().skywars2$display(victim);boolean rejected=false;
        try{new MatchQueuedWorkCheckpoint(game,sources);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("Drone display cache");}finally{nativeSpawner().skywars2$display(null);}
        require(rejected&&List.copyOf(queue()).equals(before)&&homing.curseTicks(victim)==0&&nativeSpawner().skywars2$delay()==delay,"unsupported native display cache rejects before replay, countdown or queue mutation");
        drops.checkpointNativeEnter(cart.getSpawner());rejected=false;
        try{new MatchQueuedWorkCheckpoint(game,sources);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("Finish drone");}finally{drops.checkpointNativeExit(cart.getSpawner());}
        require(rejected&&List.copyOf(queue()).equals(before),"native producer callback boundary rejects without changing pending native work");
    }

    void verify(ServerPlayer gamma){
        require(List.copyOf(queue()).equals(order),"actual captured fair-scheduler queue order and exact membership return");
        for(int i=0;i<rays.size();i++){var r=rays.get(i);require(VanityRayScene.<Vec3>get(r,"position").equals(cursors.get(i))&&VanityRayScene.<Double>get(r,"remaining").equals(left.get(i))&&!VanityRayScene.<Boolean>get(r,"finished")&&get(r,"owner")== (i<2?base.other:gamma),"independent pending cursor/remaining/current author "+i);}
        var tech=VanityRayScene.<Map<LbcExasperato.RayTicket,ServerPlayer>>get(hohl,"rays");var wall=VanityRayScene.<Map<LbcExasperato.RayTicket,ServerPlayer>>get(reactive,"rays");
        require(tech.size()==1&&wall.size()==1&&tech.get(get(bomberAim,"ticket"))==base.other&&wall.get(get(get(rays.get(1),"replay"),"ticket"))==base.other,"original typed tickets alias producer registries");
        require(homing.pendingRayCount()==2&&VanityRayScene.<Map<ServerPlayer,Integer>>get(hohl,"phases").equals(Map.of(base.other,phase))&&reactive.cooldownTicks(base.other)==cooldown,"private sub-unit cooldown and complete pending owner membership");
        require(VanityRayScene.<Double>get(bomberAim,"walked")==walked&&VanityRayScene.<Double>get(bomberAim,"nextSample")==sample,"independent technology sampling progress restored");
        require(homing.snapshot().equals(List.of(fire))&&fire.getOwner()==gamma&&VanityRayScene.<Map<LargeFireball,Integer>>get(homing,"ages").get(fire)==fireAge&&VanityRayScene.<Map<LargeFireball,Object>>get(homing,"steeringTargets").get(fire)==gamma,"same native guided body, private age and CURRENT steering target");
        require(VanityRayScene.<List<?>>get(homing,"rains").equals(List.of(rain))&&get(rain,"owner")==gamma&&VanityRayScene.<Integer>get(rain,"remaining")==1&&VanityRayScene.<Vec3>get(rain,"position").equals(base.at.add(0,10,0)),"original rain identity and bounded partial effect progress return");
        require(VanityRayScene.<List<?>>get(homing,"destinies").equals(List.of(destiny))&&get(destiny,"owner")==gamma&&VanityRayScene.<Integer>get(destiny,"remaining")==3&&VanityRayScene.<Vec3>get(destiny,"position").equals(base.at.add(0,50,0)),"independent original destiny source/position/remainder restore without a blast");
        require(VanityRayScene.<Map<Object,Object>>get(homing,"curses").equals(Map.of(gamma,rawCurse))&&get(rawCurse,"owner")==gamma&&VanityRayScene.<Long>get(rawCurse,"remaining")==curseRemaining&&VanityRayScene.<Boolean>get(rawCurse,"rawWild")&&VanityRayScene.<Integer>get(homing,"ticks")==ticks,"raw curse current-body key/author, remainder and native cadence restored without terminal replay");
        require(drops.snapshot().equals(tracked)&&VanityRayScene.<Map<Object,Object>>get(drops,"producers").equals(Map.of(cart.getSpawner(),cart))&&VanityRayScene.<Map<UUID,ServerPlayer>>get(drops,"authors").equals(Map.of(gamma.getUUID(),gamma))&&VanityRayScene.<Set<String>>get(drops,"deadHosts").equals(dead),"tracked host order, exact native producer alias, current author and host ledger");
        require(tree.stream().allMatch(e->drops.owner(e)==gamma)&&cart.getFirstPassenger()!=null&&cart.getVehicle()!=null,"source tree follows the current native author and canonical passenger graph");
        require(nativeSpawner().skywars2$delay()==delay&&nativeSpawner().skywars2$spin()==spin&&nativeSpawner().skywars2$oldSpin()==oldSpin&&nativeSpawner().skywars2$next().getEntityToSpawn().getStringOr("vanity_ray_witness","").equals(spawnWitness)&&Math.floorMod(game.server().getTickCount()-VanityRayScene.<Integer>get(drops,"cadenceOffset"),20)==dronePhase,"independent native spawn data/unsaved spin and producer phase return");
        require(homing.curseTicks(victim)==0,"restoration invokes no queued targeted curse terminal");
    }

    boolean ready(){return rays.stream().allMatch(r->VanityRayScene.<Boolean>get(r,"finished"))&&homing.curseTicks(victim)>0&&homing.destinyCount()==0;}
    void thawed(){
        require(ready()&&hohl.pendingRayCount()==0&&reactive.pendingRayCount()==0&&homing.pendingRayCount()==0,"actual resumed dispatcher removes every saved native typed ticket");
        int remaining=homing.curseTicks(victim);require(remaining>1180&&remaining<=1200,"exactly one1200-tick native curse terminal after replay");
        var curse=VanityRayScene.<Map<Object,Object>>get(homing,"curses").get(victim);require(get(curse,"owner")==game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestGamma")),"resumed terminal uses current Gamma author");
        curseTicket.cancel();require(homing.curseTicks(victim)==remaining,"completed typed terminal cannot apply a second curse");
        require(homing.rainCount()==0&&nativeSpawner().skywars2$delay()<delay&&VanityRayScene.<Double>get(bomberAim,"walked")>walked,"native rain, producer countdown and technology trail resume their saved progress");
        hohl.stop();reactive.stop();homing.stop();drops.stop();
    }
    void mutate(){
        set(bomberAim,"walked",900d);set(bomberAim,"nextSample",901d);set(rawCurse,"remaining",999L);set(rain,"remaining",99);set(rain,"position",Vec3.ZERO);set(destiny,"remaining",99);set(destiny,"position",Vec3.ZERO);set(homing,"ticks",999);
        VanityRayScene.<Map<ServerPlayer,Integer>>get(hohl,"phases").put(base.other,4);VanityRayScene.<Map<ServerPlayer,Integer>>get(reactive,"cooldowns").put(base.other,777);
        nativeSpawner().skywars2$delay(1);nativeSpawner().skywars2$spin(900);nativeSpawner().skywars2$next().getEntityToSpawn().putString("vanity_ray_witness","future");
        VanityRayScene.<Set<String>>get(drops,"deadHosts").add("future-host");
    }
    void pending(){
        require(homing.pendingRayCount()==0&&homing.count()==0&&drops.snapshot().isEmpty()&&hohl.pendingRayCount()==0&&reactive.pendingRayCount()==0,"offline Gamma defers the ENTIRE combined private owner, including Beta pending producers");
        require(VanityRayScene.<Long>get(rawCurse,"remaining")==999L&&VanityRayScene.<Integer>get(rain,"remaining")==99&&VanityRayScene.<Double>get(bomberAim,"walked")==900d,"offline preparation never partially installs hidden source progress");
    }
}
