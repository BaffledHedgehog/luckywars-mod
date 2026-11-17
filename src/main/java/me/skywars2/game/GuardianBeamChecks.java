package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Real scheduled summon AI: distant moving targets, charge persistence and occlusion. */
public final class GuardianBeamChecks {
    private GuardianBeamChecks() {}
    private static Fixture running;
    private static void require(boolean value,String why) {
        if(!value)throw new IllegalStateException("Guardian beams: "+why);
    }
    public static void run(GameController game) {
        require(running==null,"exclusive fixture");
        var f=new Fixture(game);running=f;
        try {f.begin();} catch(Throwable error) {running=null;f.close();throw error;}
    }
    public static void tick(GameController game) {
        if(running==null)return;
        var f=running;
        try {
            if(!f.step())return;
            running=null;f.close();
            SkyWars2.LOGGER.info("[check] PASS guardian-beams (scheduled native AI at48/96/160 blocks, moving128-block boundary, occlusion/reacquisition, repeated commands, mid-beam retarget, no hidden damage)");
        } catch(Throwable error) {
            running=null;
            try {f.close();} catch(Throwable cleanup) {error.addSuppressed(cleanup);}
            SkyWars2.LOGGER.error("[check] FAIL guardian-beams",error);
        }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final WoolBucketChecks.Fixture base;
        final Set<Long> forced=new LinkedHashSet<>();
        final List<BlockPos> wall=new ArrayList<>();
        final List<Integer> hitTicks=new ArrayList<>();
        final Vec3 origin;
        Guardian guardian;
        Cow target,previousTarget;
        int phase,when,firstAge,beamFrames,beamStreak,longestBeam,visibleFrames,resets,previousBeam;
        float health,previousHealth,previousTargetHealth;
        boolean closed,ownsSummons;
        Fixture(GameController game) {
            this.game=game;base=new WoolBucketChecks.Fixture(game,false);
            origin=base.home.add(0,48,0);
        }
        void begin() {
            require(game.items().summons().count()==0,"no existing user summons");ownsSummons=true;
            base.prepareParticipants();
            var min=BlockPos.containing(origin.add(-3,0,-10));
            var max=BlockPos.containing(origin.add(3,0,170));
            for(int x=min.getX()>>4;x<=max.getX()>>4;x++)for(int z=min.getZ()>>4;z<=max.getZ()>>4;z++) {
                long key=ChunkPos.pack(x,z);
                if(!base.level.getForceLoadedChunks().contains(key)) {
                    forced.add(key);base.level.setChunkForced(x,z,true);
                }
                base.level.getChunk(x,z);
            }
            base.move(base.owner,origin.add(0,0,-6));
            base.move(base.other,origin.add(6,0,0));
            target=cow(origin.add(0,0,48));
            next(0,48);
        }
        Cow cow(Vec3 at) {
            var result=new Cow(EntityTypes.COW,base.level);result.setNoAi(true);result.setNoGravity(true);
            result.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);result.setHealth(1000);
            result.setPos(at);base.own(result);
            require(base.level.addFreshEntity(result),"published ordinary nonplayer enemy");return result;
        }
        void command() {
            var delta=target.getBoundingBox().getCenter().subtract(base.owner.getEyePosition());
            base.owner.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));
            base.owner.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));
            require(game.items().summons().orders().lookedAt(base.owner)==target,"real focused enemy");
            game.items().summons().orders().command(base.owner);
        }
        void formation() {
            require(game.items().summons().castOrdinary(base.owner,LbcSummonerItems.kinds().get(6)),"canonical formation");
            guardian=(Guardian)game.items().summons().ordinarySnapshot("guardians").getFirst();
            for(var mob:game.items().summons().snapshot())if(mob!=guardian)mob.setNoAi(true);
            guardian.setPos(origin);guardian.setDeltaMovement(Vec3.ZERO);guardian.setOnGround(false);
            require(guardian.isNoGravity()&&!guardian.isNoAi(),"authored flying Guardian with native AI");
        }
        void next(int value,double distance) {
            phase=value;when=game.server().getTickCount();
            if(value<4)formation();
            target.setPos(origin.add(value==6?10:0,0,distance));target.setHealth(1000);
            target.setInvulnerableTime(0);target.damageCooldownTime=0;
            command();
            require(clear(),"native COLLIDER/Fluid.NONE corridor is empty at"+distance);
            firstAge=guardian.tickCount;beamFrames=beamStreak=longestBeam=visibleFrames=resets=previousBeam=0;
            health=base.other.getHealth();previousHealth=1000;hitTicks.clear();
        }
        boolean clear() {
            return base.level.clip(new ClipContext(guardian.getEyePosition(),target.getEyePosition(),
                    ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,guardian)).getType()==HitResult.Type.MISS;
        }
        String diagnostics() {
            return " phase="+phase+" elapsed="+(game.server().getTickCount()-when)+" nativeTicks="+(guardian.tickCount-firstAge)
                    +" hp="+target.getHealth()+" distance="+guardian.distanceTo(target)+" clear="+clear()
                    +" LOS="+guardian.hasLineOfSight(target)+" target="+(guardian.getTarget()==target)
                    +" assigned="+(game.items().summons().orders().assigned(guardian)==target)
                    +" active="+(guardian.getActiveAttackTarget()==target)+" beamFrames="+beamFrames
                    +" longestBeam="+longestBeam+" visibleFrames="+visibleFrames+" resets="+resets+" hits="+hitTicks;
        }
        boolean step() {
            base.client.acknowledge();base.audience.acknowledge();
            int elapsed=game.server().getTickCount()-when;
            require(guardian.isAlive()&&!guardian.isRemoved()&&target.isAlive(),"live native actors"+diagnostics());
            require(base.other.getHealth()==health,"command retained despite closer player"+diagnostics());
            if(phase!=4) {
                require(clear(),"unobstructed moving-target lane"+diagnostics());
                if(guardian.hasLineOfSight(target))visibleFrames++;
            }
            if(target.getHealth()<previousHealth) {
                int now=game.server().getTickCount();
                require(hitTicks.isEmpty()||now-hitTicks.getLast()>=guardian.getAttackDuration(),
                        "each damage cycle requires another full native charge"+diagnostics()+" now="+now);
                hitTicks.add(now);
            }
            previousHealth=target.getHealth();
            boolean beam=guardian.getActiveAttackTarget()==target;
            if(beam) {beamFrames++;longestBeam=Math.max(longestBeam,++beamStreak);}
            else {if(previousBeam==1&&target.getHealth()==1000)resets++;beamStreak=0;}
            previousBeam=beam?1:0;
            if(phase<3) {
                target.setPos(origin.add(Math.sin(elapsed*.04)*2,0,phase==0?48:phase==1?96:160));
                if(elapsed<120)return false;
                require(guardian.tickCount-firstAge>=110&&target.getHealth()<1000&&longestBeam>=70&&resets==0,
                        "far native beam completes without spurious charge reset"+diagnostics());
                SkyWars2.LOGGER.info("[check] guardian-beams distance witness{}",diagnostics());
                next(phase+1,phase==0?96:phase==1?160:128);return false;
            }
            if(phase==3) {
                command(); // Repeated player orders must not bypass native beam reset.
                target.setPos(origin.add(Math.sin(elapsed*.04)*2,0,128+Math.sin(elapsed*.05)*8));
                require(elapsed<=320,"eventual active beam before wall control"+diagnostics());
                if(elapsed<220||!guardian.hasActiveAttackTarget())return false;
                require(target.getHealth()<990&&visibleFrames==elapsed&&resets==0,
                        "crossing vanilla128-block visibility limit never interrupts a valid summon beam"+diagnostics());
                base.move(base.other,origin.add(6,300,0));
                next(4,96);
                require(guardian.hasActiveAttackTarget(),"occluding an already active native beam");
                var middle=guardian.getEyePosition().lerp(target.getEyePosition(),.5);
                for(var p:BlockPos.betweenClosed(BlockPos.containing(middle.add(-3,-3,-1)),BlockPos.containing(middle.add(3,3,1)))) {
                    wall.add(p.immutable());base.set(p,Blocks.STONE.defaultBlockState());
                }
                require(!clear()&&!guardian.hasLineOfSight(target),"real wall immediately hides target");
                return false;
            }
            if(phase==4) {
                require(!clear()&&!guardian.hasLineOfSight(target),"wall never treated as visible");
                require(target.getHealth()==1000,"no damage through wall"+diagnostics());
                if(elapsed<110)return false;
                require(guardian.getActiveAttackTarget()!=target,"occluded beam really stopped");
                for(var p:wall)base.set(p,Blocks.AIR.defaultBlockState());wall.clear();
                next(5,96);return false;
            }
            if(phase==5) {
                if(elapsed<120)return false;
                require(target.getHealth()<1000&&longestBeam>=70,"native beam resumes after obstruction removed"+diagnostics());
                require(guardian.hasActiveAttackTarget(),"retargeting an already charged beam");
                previousTarget=target;previousTargetHealth=target.getHealth();
                target=cow(origin.add(10,0,96));next(6,96);return false;
            }
            require(previousTarget.getHealth()==previousTargetHealth,"old focus receives no further beam damage");
            require(elapsed>=80||target.getHealth()==1000,"new focus must receive a fresh full charge"+diagnostics());
            if(elapsed<120)return false;
            require(target.getHealth()<1000&&longestBeam>=70,"new focus receives correct beam after full charge"+diagnostics());
            return true;
        }
        @Override public void close() {
            if(closed)return;closed=true;
            try {if(ownsSummons)game.items().summons().stop();}
            finally {
                try {base.close();}
                finally {for(long key:forced)base.level.setChunkForced(ChunkPos.getX(key),ChunkPos.getZ(key),false);}
            }
        }
    }
}
