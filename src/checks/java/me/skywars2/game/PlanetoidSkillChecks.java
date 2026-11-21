package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Invoke inside the ordinary isolated two-client real-match fixture after selecting/unlocking skill21. */
public final class PlanetoidSkillChecks {
    private PlanetoidSkillChecks(){}
    private static Flight running;
    public static boolean active(){return running!=null;}
    private static void require(boolean value,String label){if(!value)throw new IllegalStateException("Planetoid: "+label);}
    private static void near(Vec3 got,Vec3 expected,String label){require(got.distanceTo(expected)<1e-5,label+" got="+got+" expected="+expected);}
    // Native float Euler angles and Mth's sine LUT add a small angular roundtrip error.
    // This tolerance applies only to reconstructed unit directions, never position or Motion.
    private static void nearDirection(Vec3 got,Vec3 expected,String label){require(got.distanceTo(expected)<3e-4,label+" got="+got+" expected="+expected);}

    private static Object read(Object value,String name){
        for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(value);}
        catch(NoSuchFieldException ignored){}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        throw new IllegalStateException("Missing fixture field "+name);
    }
    private static Vec3 motion(Vec3 aim,double scale){return new Vec3(Math.floor(aim.x*10000)*scale,Math.floor(aim.y*10000)*scale,Math.floor(aim.z*10000)*scale);}

    public static void run(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        var other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(owner!=null&&other!=null&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two actual participants");
        var saved=new RegenerationBeltChecks.Snapshot(game,owner);var otherSaved=new RegenerationBeltChecks.Snapshot(game,other);
        boolean skills=game.config().lbcSkills,ranked=game.config().ranked;
        try{
            game.config().lbcSkills=true;game.config().ranked=true;
            game.players().get(owner.getUUID()).lbcSkill=21;game.players().get(other.getUUID()).lbcSkill=0;
            require(owner.level()==other.level(),"one real match dimension");
            // High resident air avoids touching terrain; the isolated match driver loads this region.
            var at=new Vec3(owner.getX(),Math.min(owner.level().getMaxY()-32,owner.getY()+64),owner.getZ());
            owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);
            other.teleportTo(other.level(),at.x,at.y,at.z+7,Set.of(),0,0,true);
            require(owner.level().hasChunkAt(net.minecraft.core.BlockPos.containing(at.add(-8,0,-8)))
                    &&owner.level().hasChunkAt(net.minecraft.core.BlockPos.containing(at.add(8,0,8))),"fixture region resident");
            try(var client=new RegenerationBeltChecks.ScheduledClient(game,owner);var audience=new RegenerationBeltChecks.ScheduledClient(game,other)){
                client.acknowledge();audience.acknowledge();runPrepared(game,owner,other);
            }
        }finally{
            game.config().lbcSkills=skills;game.config().ranked=ranked;
            saved.restore(game);otherSaved.restore(game);saved.restoreAttributes();otherSaved.restoreAttributes();
            saved.restoreUse();otherSaved.restoreUse();game.items().magic().inventoryChanged(owner);
            game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();game.players().save();
        }
        require(running==null,"exclusive registered continuation fixture");
        var flight=new Flight(game,owner,other);running=flight;
        try{flight.start();}catch(RuntimeException|Error failure){running=null;try{flight.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}throw failure;}
    }

    public static void tick(GameController game){
        if(running==null)return;
        try{if(running.advance()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS planetoid-skills (source phases and boundaries, protected native versus logical raycaster, shared family controllers, MAG/artifact/TECH, actual world TimeStop, registered skull/Shulker/Air/controlled-FPV continuation)");}}
        catch(RuntimeException|Error failure){try{running.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL planetoid-skills",failure);}
    }

    private static final class Flight implements AutoCloseable {
        final GameController game;final ServerPlayer owner,other;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;
        final boolean skills,ranked;final List<Entity> bodies=new ArrayList<>();
        final Map<Entity,Vec3> positions=new IdentityHashMap<>(),directions=new IdentityHashMap<>();
        final Set<Entity> neutralBefore;final Set<LbcFpvDrone.Drone> droneBefore;
        RegenerationBeltChecks.ScheduledClient client,audience;int started,steps;boolean closed;
        Flight(GameController game,ServerPlayer owner,ServerPlayer other){
            this.game=game;this.owner=owner;this.other=other;saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);
            skills=game.config().lbcSkills;ranked=game.config().ranked;neutralBefore=new HashSet<>(game.items().neutral().snapshot());droneBefore=Set.copyOf(game.items().fpvDrone().snapshot());
        }
        void start(){
            game.config().lbcSkills=true;game.config().ranked=true;
            game.players().get(owner.getUUID()).lbcSkill=21;game.players().get(other.getUUID()).lbcSkill=0;
            var level=owner.level();var at=new Vec3(owner.getX(),Math.min(level.getMaxY()-32,owner.getY()+64),owner.getZ());
            owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);other.teleportTo(level,at.x,at.y,at.z+7,Set.of(),0,0,true);
            owner.setNoGravity(true);other.setNoGravity(true);owner.setDeltaMovement(Vec3.ZERO);other.setDeltaMovement(Vec3.ZERO);
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,other);client.acknowledge();audience.acknowledge();
            game.items().neutral().cast(other,1);var skull=game.items().neutral().snapshot().stream().filter(e->!neutralBefore.contains(e)).findFirst().orElseThrow();skull.setPos(at.add(5,0,0));bodies.add(skull);
            var air=game.items().air().flying(other,InteractionHand.MAIN_HAND,ItemStack.EMPTY,at.add(0,1,5),new Vec3(0,0,1));require(air!=null,"actual Air producer");bodies.add(air);
            var bullet=new net.minecraft.world.entity.projectile.ShulkerBullet(level,other,owner,net.minecraft.core.Direction.Axis.Y);bullet.setPos(at.add(0,1,-5));require(level.addFreshEntity(bullet),"actual native Shulker bullet");bodies.add(bullet);
            var controller=new ItemStack(net.minecraft.world.item.Items.CARROT_ON_A_STICK);var data=new net.minecraft.nbt.CompoundTag();data.putInt("drone_pult",1);data.putInt("raycastspell",1);controller.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));other.setItemInHand(InteractionHand.MAIN_HAND,controller);
            require(game.items().fpvDrone().use(other,InteractionHand.MAIN_HAND),"real controlled FPV producer (source spawn region must be resident)");
            var drone=game.items().fpvDrone().snapshot().stream().filter(d->!droneBefore.contains(d)).findFirst().orElseThrow();drone.setPos(at.add(-5,1,0));bodies.add(drone);
            require(game.items().fpvDrone().controlling(other),"actual pilot controller retained");
            game.items().planetoidSkill().tick();capture();started=game.server().getTickCount();
        }
        void capture(){for(var body:bodies){positions.put(body,body.position());directions.put(body,body instanceof LbcFpvDrone.Drone?body.getLookAngle():body.getDeltaMovement().normalize());}}
        boolean advance(){
            client.acknowledge();audience.acknowledge();int elapsed=game.server().getTickCount()-started;if(elapsed<1)return false;require(elapsed<10,"bounded registered continuation");
            for(var body:bodies){require(!body.isRemoved(),"registered native body survived "+body.getClass().getSimpleName());
                Vec3 moved=body.position().subtract(positions.get(body));require(moved.lengthSqr()>1e-6&&moved.normalize().dot(directions.get(body))>.9,"registered next-tick flight uses retained orbit direction "+body.getClass().getSimpleName());
                if(body instanceof LbcFpvDrone.Drone){require(game.items().fpvDrone().controlling(other),"orbit does not steal pilot authority");
                    var visual=((Map<?,?>)read(game.items().fpvDrone(),"actors")).get(body);require(visual instanceof Entity,"same owned native drone visual");near(((Entity)visual).position(),body.position(),"controlled drone visual follows redirected flight");}
            }
            capture();started=game.server().getTickCount();return ++steps==2;
        }
        @Override public void close(){if(closed)return;closed=true;
            try{game.items().fpvDrone().release(other);
                for(var body:bodies){game.items().air().unloaded(body);game.items().fpvDrone().unloaded(body);body.discard();}
                for(var shot:game.items().neutral().snapshot())if(!neutralBefore.contains(shot))game.items().neutral().unloaded(shot);
                for(var drone:game.items().fpvDrone().snapshot())if(!droneBefore.contains(drone)){game.items().fpvDrone().unloaded(drone);drone.discard();}
            }finally{if(client!=null)client.close();if(audience!=null)audience.close();game.config().lbcSkills=skills;game.config().ranked=ranked;
                saved.restore(game);otherSaved.restore(game);saved.restoreAttributes();otherSaved.restoreAttributes();saved.restoreUse();otherSaved.restoreUse();game.players().save();}
        }
    }

    private static void verifyGrimoireWarning(GameController game,ServerPlayer owner,ServerPlayer other){
        var service=game.items().grimoireBoss();var before=Set.copyOf(service.snapshot());
        try{
            var boss=service.spawn(owner.level(),owner.position().add(8,0,0),other);
            require(boss!=null,"native Grimoire controller");
            var produce=service.getClass().getDeclaredMethod("danmaku",net.minecraft.world.entity.monster.warden.Warden.class);produce.setAccessible(true);produce.invoke(service,boss);
            var card=service.snapshot().stream().filter(e->!before.contains(e)&&e.entityTags().contains("boss_prism")).findFirst().orElseThrow();
            card.setPos(owner.position().add(4,1,4));
            var advance=service.getClass().getDeclaredMethod("tickChild",Entity.class);advance.setAccessible(true);advance.invoke(service,card);
            var warnings=(Map<?,?>)read(service,"warnings");var initial=warnings.get(card);require(initial!=null,"actual boss-prism warning created");
            var visual=(TrialWarningVisual)read(initial,"visual");int age=visual.age(),life=visual.lifetime();
            double length=visual.origin().distanceTo(visual.end());
            visual.direction(new Vec3(3,4,12));
            require(Math.abs(visual.origin().distanceTo(visual.end())-length)<1e-10,"pure rotation preserves beam length for nonunit input");
            require(visual.age()==age&&visual.lifetime()==life,"pure rotation preserves warning clock");
            game.items().planetoidSkill().pulse(owner);
            var redirected=warnings.get(card);require(read(redirected,"visual")==visual,"orbit retains Grimoire warning holder");
            near((Vec3)read(redirected,"from"),visual.origin(),"cached warning origin agrees with visual");
            near((Vec3)read(redirected,"to"),visual.end(),"cached warning end agrees with visual");
            require((int)read(redirected,"age")==1&&visual.age()==age,"retarget preserves metadata and visual ages");
            advance.invoke(service,card);var continued=warnings.get(card);
            require(read(continued,"visual")==visual&&(int)read(continued,"age")==2&&visual.age()==age+1,
                    "next actual native boss-prism handler reuses holder and advances existing clock once");
        }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        finally{for(var actor:service.snapshot())if(!before.contains(actor)){service.unloaded(actor);actor.discard();}}
    }

    public static void runPrepared(GameController game,ServerPlayer owner,ServerPlayer other){
        require(game.lbc().hasSkill(owner,21),"actual persisted unlocked skill21 selection");
        require(!game.items().magic().suppressed(owner),"unsuppressed fixture");
        var service=game.items().planetoidSkill();var level=owner.level();var origin=owner.position();
        var owned=new ArrayList<Entity>();var originalNeutral=Set.copyOf(game.items().neutral().snapshot());
        var originalFists=Set.copyOf(game.items().stand().fistSnapshot());
        var originalGod=Set.copyOf(game.items().godHedgehog().snapshot());
        var originalTraces=new ArrayList<LbcWands.ElectroTrace>();
        try{
            var cow=new Cow(EntityTypes.COW,level);cow.setPos(origin.add(2.5,0,0));cow.setNoAi(true);cow.setNoGravity(true);owned.add(cow);
            require(level.addFreshEntity(cow),"real native Cow admitted");
            var protectedCow=new Cow(EntityTypes.COW,level);protectedCow.setPos(origin.add(-2,0,0));protectedCow.entityTags().add("strict_map_object");owned.add(protectedCow);level.addFreshEntity(protectedCow);
            service.pulse(owner);require(cow.entityTags().contains("affected_by_gravity"),"inclusive2.5 capture");
            near(cow.getDeltaMovement(),motion(LbcPlanetoidSkill.direction(owner,cow.position(),-87),.0002),"source floored slow Motion");
            require(!protectedCow.entityTags().contains("affected_by_gravity"),"map service never captured");
            cow.setPos(origin.add(15,0,0));cow.setDeltaMovement(Vec3.ZERO);service.pulse(owner);
            require(!cow.entityTags().contains("affected_by_gravity")&&cow.getDeltaMovement().equals(Vec3.ZERO),"inclusive15 release happens before mob motion");
            cow.setPos(origin.add(20.01,0,0));cow.entityTags().add("affected_by_gravity");service.pulse(owner);
            require(cow.entityTags().contains("affected_by_gravity"),"source does not release beyond20");

            game.items().neutral().cast(other,1);
            var neutral=game.items().neutral().snapshot().stream().filter(e->!originalNeutral.contains(e)).findFirst().orElseThrow();
            neutral.setPos(origin.add(5,0,0));var author=neutral.getOwner();
            require(ProjectileLifetimes.protectedWandProjectile(neutral),"actual external-knockback-protected native skull");
            service.pulse(owner);near(neutral.getDeltaMovement(),motion(LbcPlanetoidSkill.direction(owner,neutral.position(),80),.00035),"protected native source projectile receives3.5,+80");
            require(neutral.getOwner()==author,"orbit never reflects ownership");
            var skullAt=neutral.position();var skullDirection=neutral.getDeltaMovement().normalize();neutral.tick();
            require(neutral.position().subtract(skullAt).dot(skullDirection)>0,"actual native protected skull tick follows redirected motion");
            neutral.setPos(origin.add(5,0,0));
            neutral.setDeltaMovement(Vec3.ZERO);service.tick();require(!neutral.getDeltaMovement().equals(Vec3.ZERO),"actual selected skill tick reaches protected projectile");

            var ice=game.items().iceWand().spawn(other,InteractionHand.MAIN_HAND,ItemStack.EMPTY);owned.add(ice);
            ice.setPos(origin.add(0,1,5));var display=ice.display;double speed=ice.getDeltaMovement().length();var position=ice.position();
            service.pulse(owner);var aim=LbcPlanetoidSkill.direction(owner,position,82);
            near(ice.getDeltaMovement(),aim.scale(speed),"source raycaster keeps authored speed,+82");
            require(ice.display==display&&ice.getOwner()==other,"one existing visual family and causal owner");
            near(display.position(),ice.position(),"display follows actual gameplay body immediately");
            nearDirection(Vec3.directionFromRotation(display.getXRot(),display.getYRot()),aim,"visual and logical direction agree");
            var iceAt=ice.position();ice.tick();
            require(ice.position().subtract(iceAt).dot(aim)>0,"actual custom-raycaster native tick follows orbit direction");
            require(ice.display==display,"native continuation retains one family");

            var trace=new LbcWands.ElectroTrace(other,origin.add(-5,1,0),game.items().epoch(),50);originalTraces.add(trace);
            game.items().wands().higherElectro(trace);int age=trace.age;
            service.pulse(owner);nearDirection(Vec3.directionFromRotation(trace.pitch,trace.yaw),LbcPlanetoidSkill.direction(owner,trace.position,82),"actual virtual marker direction");
            require(trace.owner==other&&trace.age==age,"virtual redirect retains author and lifetime clock");

            try{var producer=game.items().stand().getClass().getDeclaredMethod("createFist",ServerPlayer.class);producer.setAccessible(true);producer.invoke(game.items().stand(),other);}
            catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            var fist=game.items().stand().fistSnapshot().stream().filter(e->!originalFists.contains(e)).findFirst().orElseThrow();
            fist.setPos(origin.add(0,1,-5));service.pulse(owner);
            near(game.items().stand().fistDirection(fist),LbcPlanetoidSkill.direction(owner,fist.position(),82),"Stand owning controller direction changes with body");

            var boss=game.items().godHedgehog().summon(level,origin.add(0,-10,0),other);
            require(boss!=null,"actual God Hedgehog controller");
            boss.setPos(origin.add(2,0,0));service.pulse(owner);
            require(boss.entityTags().contains("affected_by_gravity"),"WILD admits the actual protected boss body");
            near(boss.getDeltaMovement(),motion(LbcPlanetoidSkill.direction(owner,boss.position(),-87),.0002),"boss receives source mob motion");
            var white=game.items().godHedgehog().whiteBoundary(boss,origin.add(4,1,4),new Vec3(0,0,1),10);
            require(white!=null,"actual white-arrow family");
            var actor=((List<?>)read(game.items().godHedgehog(),"actors")).stream().filter(a->read(a,"display")==white).findFirst().orElseThrow();
            var warning=read(actor,"warning");service.pulse(owner);
            near((Vec3)read(actor,"direction"),LbcPlanetoidSkill.direction(owner,white.position(),82),"Hedgehog native actor direction retained by owner");
            require(read(actor,"warning")==warning&&(int)read(actor,"age")==10,"warning family retained without replacement or clock reset");

            verifyGrimoireWarning(game,owner,other);
            var probe=new Arrow(EntityTypes.ARROW,level);probe.setPos(origin.add(-5,0,0));probe.setOwner(other);level.addFreshEntity(probe);owned.add(probe);
            for(String tag:List.of("stopper_magic","stopper")){
                var jammer=new Marker(EntityTypes.MARKER,level);jammer.setPos(origin);jammer.entityTags().add(tag);level.addFreshEntity(jammer);game.items().magic().loaded(jammer);
                try{probe.setDeltaMovement(Vec3.ZERO);service.tick();require(probe.getDeltaMovement().equals(Vec3.ZERO)==tag.equals("stopper_magic"),"MAG only suppresses Planetoid; TECH does not");}
                finally{game.items().magic().unloaded(jammer);jammer.discard();}
            }
            // Caller fixture restores participant inventories. This unit only owns the temporary slot.
            var item=other.getInventory().getItem(9).copy();other.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();
            try{require(game.items().magic().suppressed(owner),"real artifact positive control");probe.setDeltaMovement(Vec3.ZERO);service.tick();near(probe.getDeltaMovement(),Vec3.ZERO,"artifact suppresses Planetoid");}
            finally{other.getInventory().setItem(9,item);game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();}
            require(!game.items().timeStop().stopped(level),"exclusive TimeStop fixture");
            probe.setDeltaMovement(Vec3.ZERO);game.items().timeStop().begin(level,20);
            try{service.tick();near(probe.getDeltaMovement(),Vec3.ZERO,"actual world TimeStop freezes orbit");}
            finally{game.items().timeStop().end(level);}
            probe.setDeltaMovement(Vec3.ZERO);service.tick();require(!probe.getDeltaMovement().equals(Vec3.ZERO),"orbit resumes after actual thaw");
            SkyWars2.LOGGER.info("[check] PASS planetoid prepared native body/family/controller/suppression checks");
        }finally{
            for(var trace:originalTraces){@SuppressWarnings("unchecked") var traces=(Collection<LbcWands.ElectroTrace>)read(game.items().wands(),"electroTraces");traces.remove(trace);}
            for(var fist:game.items().stand().fistSnapshot())if(!originalFists.contains(fist))game.items().stand().unloaded(fist);
            for(var actor:game.items().godHedgehog().snapshot())if(!originalGod.contains(actor))game.items().godHedgehog().unloaded(actor);
            for(var shot:game.items().neutral().snapshot())if(!originalNeutral.contains(shot))game.items().neutral().unloaded(shot);
            for(var actor:owned){game.items().iceWand().unloaded(actor);actor.discard();}
        }
    }
}
