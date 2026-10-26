package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native server-tick latency fixture. Setup and restoration also yield between bounded batches. */
public final class EffectTerrainChecks {
    private EffectTerrainChecks() {}
    private static Fixture running;
    private static void require(boolean value,String message) {
        if(!value)throw new IllegalStateException("Effect terrain: "+message);
    }
    private static final class Fixture {
        final GameController game;
        final ServerLevel level;
        final BlockPos center;
        final Map<BlockPos,MapManager.BlockSnapshot> saved=new LinkedHashMap<>();
        final List<Cow> actors=new ArrayList<>();
        final List<BlockPos> chamber=new ArrayList<>();
        final List<BlockPos> dense=new ArrayList<>();
        final long initialVisited=SignedEnvironmentalEffects.terrainVisited();
        final long initialWrites=SignedEnvironmentalEffects.terrainWrites();
        Cow source,secondSource;
        int phase,cursor,started,lastTick=-1,nearTick=-1,edgeTick=-1,webTick=-1;
        int fireNear,fireEdge,movingEdge,waterNear,waterEdge;
        int denseFirst=-1,denseFull=-1,denseSources,denseQueueBase,sealCursor;
        long maxNanos,denseInitialWrites,denseWrites;
        Throwable failure;
        Iterator<Map.Entry<BlockPos,MapManager.BlockSnapshot>> restoring;
        Fixture(GameController game) {
            this.game=game;level=game.maps().level();
            require(game.playing()&&!level.players().isEmpty(),"active match and resident player required");
            var player=level.players().getFirst();
            center=new BlockPos(player.blockPosition().getX(),Math.min(level.getMaxY()-65,player.blockPosition().getY()+75),player.blockPosition().getZ());
            require(center.getY()-51>=level.getMinY(),"fixture inside native height range");
            // Every possible field write is inside this stone chamber, including alternating movement.
            // Isolated air probes keep fluid propagation finite without changing the native water path.
            for(int x=-17;x<=17;x++)for(int z=-17;z<=17;z++) {
                require(level.hasChunkAt(center.offset(x,0,z)),"entire chamber already resident; never force terrain");
                for(int y=-17;y<=17;y++) {
                    var pos=center.offset(x,y,z);chamber.add(pos);
                    if(x*x+y*y+z*z<=225)dense.add(pos);
                }
            }
        }
        void save(BlockPos pos) {saved.computeIfAbsent(pos.immutable(),p->game.maps().snapshot(level,p));}
        void put(BlockPos pos,BlockState state) {save(pos);level.setBlock(pos,state,2);}
        Cow cow(Vec3 at,boolean resident) {
            var actor=EntityTypes.COW.create(level,EntitySpawnReason.TRIGGERED);
            require(actor!=null,"native cow creation");actors.add(actor);
            actor.setNoAi(true);actor.setNoGravity(true);actor.snapTo(at.x,at.y,at.z,0,0);
            if(resident)require(level.addFreshEntity(actor),"native cow admission");return actor;
        }
        void cancel() {
            for(var actor:actors){SignedEnvironmentalEffects.cancelTerrain(actor);actor.removeAllEffects();actor.discard();}
            source=secondSource=null;
        }
        // A large aura legitimately reaches existing match actors. Protect only those actors for
        // this synchronous invocation, then restore their exact tags before yielding the tick.
        void fire(Cow actor) {
            var protectedActors=new ArrayList<LivingEntity>();
            for(var e:level.getAllEntities())if(e instanceof LivingEntity living&&!actors.contains(living)&&living.entityTags().add("sin_greed_active"))protectedActors.add(living);
            try{actor.tickCount=0;SignedEnvironmentalEffects.fireTick(actor);}
            finally{for(var e:protectedActors)e.entityTags().remove("sin_greed_active");}
        }
        void beginField(boolean water) {
            cancel();nearTick=edgeTick=-1;
            // The same native probes exercise immediate neighbors and the exact spherical boundary.
            for(var pos:List.of(center.offset(2,0,0),center.offset(15,0,0),center.offset(15,15,15)))put(pos,Blocks.AIR.defaultBlockState());
            source=cow(Vec3.atCenterOf(center),false);
            source.addEffect(new MobEffectInstance(water?MobEffects.CONDUIT_POWER:MobEffects.FIRE_RESISTANCE,2000,226));
            started=game.server().getTickCount();
        }
        void sample(boolean water,boolean moving) {
            int age=game.server().getTickCount()-started;
            var wanted=water?Blocks.WATER:Blocks.FIRE;
            if(nearTick<0&&level.getBlockState(center.offset(2,0,0)).is(wanted))nearTick=age;
            if(edgeTick<0&&level.getBlockState(center.offset(15,0,0)).is(wanted))edgeTick=age;
            require(level.getBlockState(center.offset(15,15,15)).isAir(),"off-sphere cube corner remains untouched");
            if(moving)source.setPos(Vec3.atCenterOf(center.offset(age%2,0,0)));
            if(water)SignedEnvironmentalEffects.conduitFill(source,15);else fire(source);
        }
        void aura() {
            // Compare the actual native damage path on equal resident actors, including resistance.
            // These actors exist only during this synchronous method; the stone chamber prevents
            // incidental field writes and they never receive a native suffocation/world tick.
            var at=Vec3.atCenterOf(center);
            var target=cow(at.add(2,0,0),true);var control=cow(at.add(3,0,0),true);
            var emitter=cow(at,false);
            for(int amplifier:new int[]{3,226,Integer.MAX_VALUE}) {
                emitter.removeEffect(MobEffects.FIRE_RESISTANCE);
                emitter.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,2000,amplifier));
                target.removeAllEffects();target.entityTags().removeAll(Set.of("sin_greed_active","time_stopped"));
                reset(target);reset(control);
                float before=target.getHealth();fire(emitter);
                float aura=before-target.getHealth();
                reset(control);float nativeBefore=control.getHealth();
                require(control.hurtServer(level,level.damageSources().inFire(),1),"native inFire comparison accepted");
                require(Math.abs(aura-1)<.001&&Math.abs(aura-(nativeBefore-control.getHealth()))<.001,"level-independent native standing-in-fire damage1, amplifier="+amplifier);
                float repeatedBefore=target.getHealth(),nativeRepeatedBefore=control.getHealth();
                fire(emitter);control.hurtServer(level,level.damageSources().inFire(),1);
                require(target.getHealth()==repeatedBefore&&control.getHealth()==nativeRepeatedBefore,"repeated aura preserves native damage cooldown, amplifier="+amplifier);
                target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,200,0));reset(target);before=target.getHealth();fire(emitter);
                require(Math.abs(before-target.getHealth()-.8)<.001,"aura respects native resistance");
                target.removeAllEffects();target.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,200,0));reset(target);before=target.getHealth();fire(emitter);
                require(target.getHealth()==before,"aura respects native fire immunity");
                target.removeAllEffects();target.entityTags().add("sin_greed_active");reset(target);before=target.getHealth();fire(emitter);
                require(target.getHealth()==before,"aura Greed guard");target.entityTags().remove("sin_greed_active");
                target.entityTags().add("time_stopped");reset(target);before=target.getHealth();fire(emitter);
                require(target.getHealth()==before,"aura paused target guard");target.entityTags().remove("time_stopped");
                SignedEnvironmentalEffects.cancelTerrain(emitter);
            }
            cancel();
        }
        static void reset(Cow cow) {cow.setHealth(cow.getMaxHealth());cow.setInvulnerableTime(0);cow.damageCooldownTime=0;cow.clearFire();}
        boolean step() {
            int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;
            maxNanos=Math.max(maxNanos,SignedEnvironmentalEffects.terrainTickNanos());
            require(game.playing()&&game.maps().level()==level,"match remains active during asynchronous fixture");
            if(phase==0) {
                int end=Math.min(chamber.size(),cursor+1024);
                for(;cursor<end;cursor++)put(chamber.get(cursor),Blocks.STONE.defaultBlockState());
                if(cursor==chamber.size()){cursor=0;phase=1;beginField(false);}
            } else if(phase==1||phase==2||phase==3) {
                boolean water=phase==3,moving=phase==2;sample(water,moving);
                int age=now-started;
                if(nearTick>=0&&edgeTick>=0) {
                    require(nearTick<=3,"near terrain latency <=3 native ticks, observed="+nearTick);
                    require(edgeTick<=30,"radial terrain latency <=30 native ticks, observed="+edgeTick);
                    if(phase==1){fireNear=nearTick;fireEdge=edgeTick;phase=2;beginField(false);}
                    else if(phase==2){movingEdge=edgeTick;phase=3;beginField(true);}
                    else {
                        waterNear=nearTick;waterEdge=edgeTick;cancel();phase=8;cursor=0;
                        // No native source may exist while carving the dense air sphere.
                        for(var pos:List.of(center.offset(2,0,0),center.offset(15,0,0)))put(pos,Blocks.STONE.defaultBlockState());
                    }
                } else require(age<=30,"field stalled, phase="+phase+", near="+nearTick+", edge="+edgeTick+", ticks="+age);
            } else if(phase==8) {
                int end=Math.min(dense.size(),cursor+1024);
                for(;cursor<end;cursor++)put(dense.get(cursor),Blocks.AIR.defaultBlockState());
                if(cursor==dense.size()) {
                    denseQueueBase=SignedEnvironmentalEffects.queuedTerrain();
                    denseInitialWrites=SignedEnvironmentalEffects.terrainWrites();
                    source=cow(Vec3.atCenterOf(center),false);secondSource=cow(Vec3.atCenterOf(center),false);
                    for(var actor:List.of(source,secondSource)) {
                        actor.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER,2000,226));
                        SignedEnvironmentalEffects.conduitFill(actor,15);
                    }
                    if(level.getBlockState(center).is(Blocks.WATER))denseFirst=0;
                    started=now;phase=9;
                }
            } else if(phase==9) {
                int age=now-started;
                require(SignedEnvironmentalEffects.queuedTerrain()<=denseQueueBase+2,"two dense fields keep a bounded queue");
                if(denseFirst<0&&level.getBlockState(center).is(Blocks.WATER))denseFirst=age;
                boolean edges=true;
                for(var p:List.of(center.offset(15,0,0),center.offset(-15,0,0),center.offset(0,15,0),center.offset(0,-15,0),center.offset(0,0,15),center.offset(0,0,-15)))edges&=level.getBlockState(p).is(Blocks.WATER);
                if(edges&&SignedEnvironmentalEffects.queuedTerrain()<=denseQueueBase) {
                    int water=0;
                    for(var p:dense)if(level.getBlockState(p).is(Blocks.WATER)) {
                        water++;if(level.getBlockState(p).getFluidState().isSource())denseSources++;
                    }
                    require(water==dense.size(),"complete native dense sphere water coverage, filled="+water+" expected="+dense.size());
                    denseFull=age;denseWrites=SignedEnvironmentalEffects.terrainWrites()-denseInitialWrites;
                    require(denseFirst<=3&&denseFull<=40,"dense radius15 first/full native latency, first="+denseFirst+" full="+denseFull);
                    require(denseWrites>0,"dense completion includes measured native scheduler block writes");
                    cancel();phase=4;cursor=0;
                } else require(age<=40,"two dense radius15 fields complete within40 native ticks, observed="+age);
            } else if(phase==4) {
                // Negative radius50 weaving can touch existing webs outside the chamber. Snapshot
                // only those cells, in resident chunks, before submitting its native death event.
                int total=101*101*101,end=Math.min(total,cursor+8192);
                for(;cursor<end;cursor++) {
                    var p=center.offset(cursor%101-50,cursor/(101*101)-50,cursor/101%101-50);
                    if(p.getY()>=level.getMinY()&&p.getY()<level.getMaxY()&&level.hasChunkAt(p)&&level.getBlockState(p).is(Blocks.COBWEB))save(p);
                }
                if(cursor==total) {
                    put(center,Blocks.COBWEB.defaultBlockState());source=cow(Vec3.atCenterOf(center),false);
                    source.addEffect(new MobEffectInstance(MobEffects.WEAVING,200,Integer.MIN_VALUE));
                    source.setHealth(0);source.remove(Entity.RemovalReason.KILLED);
                    started=now;phase=5;
                }
            } else if(phase==5) {
                if(!level.getBlockState(center).is(Blocks.COBWEB)) {
                    webTick=now-started;require(webTick<=2,"maxradius50 first web <=2 native ticks");cancel();phase=6;
                } else require(now-started<=2,"maxradius50 near web stalled behind remote corner");
            } else if(phase==6) {
                aura();phase=7;
                SkyWars2.LOGGER.info("[check] effect-terrain measured fire near={} edge={} movingEdge={} water near={} edge={} weaving={} nativeTicks; maxSchedulerNanos={} visited={} writes={}",fireNear,fireEdge,movingEdge,waterNear,waterEdge,webTick,maxNanos,SignedEnvironmentalEffects.terrainVisited()-initialVisited,SignedEnvironmentalEffects.terrainWrites()-initialWrites);
                SkyWars2.LOGGER.info("[check] effect-terrain dense two radius15 fields cells={} first={} full={} nativeTicks schedulerWrites={} nativeSources={} maxSchedulerNanos={}",dense.size(),denseFirst,denseFull,denseWrites,denseSources,maxNanos);
                beginRestore(null);
            }
            return false;
        }
        void beginRestore(Throwable error) {
            failure=error;cancel();
            // Reseal the complete dense sphere before opening any wall. Native flowing cells and
            // delayed fluid ticks must never escape through a partially restored chamber.
            sealCursor=0;
            restoring=saved.entrySet().iterator();
        }
        boolean restore() {
            if(sealCursor<chamber.size()) {
                int end=Math.min(chamber.size(),sealCursor+1024);
                for(;sealCursor<end;sealCursor++) {
                    var pos=chamber.get(sealCursor);
                    if(saved.containsKey(pos)&&!level.getBlockState(pos).is(Blocks.STONE))level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
                }
                return false;
            }
            // Keep a large finite chamber from becoming a teardown-induced single-tick stall.
            for(int n=0;n<1024&&restoring.hasNext();n++) {
                var e=restoring.next();game.maps().restoreSnapshot(level,e.getKey(),e.getValue(),2);
            }
            return !restoring.hasNext();
        }
    }
    public static void run(GameController game) {
        require(running==null,"one fixture at a time");
        try{running=new Fixture(game);SkyWars2.LOGGER.info("[check] effect-terrain started (bounded native setup, ticks, restore)");}
        catch(RuntimeException|Error error){SkyWars2.LOGGER.error("[check] FAIL effect-terrain",error);throw error;}
    }
    public static void tick(GameController game) {
        if(running==null)return;var f=running;
        try {
            if(f.restoring!=null) {
                if(f.restore()){running=null;if(f.failure==null)SkyWars2.LOGGER.info("[check] PASS effect-terrain (native sparse/dense radius15 latency, two-field bounded queue, movement, radius50 weaving, fixed inFire aura/cooldown; owned actors and block snapshots restored); denseFirst={} denseFull={} cells={} schedulerWrites={} maxSchedulerNanos={}",f.denseFirst,f.denseFull,f.dense.size(),f.denseWrites,f.maxNanos);else SkyWars2.LOGGER.error("[check] FAIL effect-terrain",f.failure);}
            } else f.step();
        } catch(Throwable error) {
            if(f.restoring!=null){running=null;SkyWars2.LOGGER.error("[check] FAIL effect-terrain restoration",error);}
            else{f.beginRestore(error);SkyWars2.LOGGER.error("[check] effect-terrain restoring after failure",error);}
        }
    }
}
