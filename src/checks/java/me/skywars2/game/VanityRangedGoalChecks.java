package me.skywars2.game;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import me.skywars2.SkyWars2;
import me.skywars2.game.entities.mob_ai.MatchMobGoals;
import me.skywars2.game.match.checkpoints.MatchEntityState;
import me.skywars2.mixin.mobs.MobGoalCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** A stopped ranged goal must not resume as running with its native cached target cleared. */
public final class VanityRangedGoalChecks {
    private static Fixture running;
    private VanityRangedGoalChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity ranged goals: "+why);}
    public static void run(GameController game){
        require(MatchMobGoals.enabled(),"candidate replay explicitly enabled");start(game,false);
    }
    public static void runGate(GameController game){
        require(!MatchMobGoals.enabled(),"unfinished replay is OFF by default");start(game,true);
    }
    private static void start(GameController game,boolean gate){
        require(running==null,"one ranged scene");var f=new Fixture(game,gate);running=f;
        try{f.begin();}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game){
        if(running==null)return;var f=running;
        try{require(f.game==game,"same match");if(!f.step())return;running=null;f.close();
            SkyWars2.LOGGER.info("[check] PASS {} (native snow-golem ranged goal; stop clears cached target; actual native scheduler transition)",f.gate?"vanity-goal-gate":"vanity-ranged-goals");
        }catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL vanity-ranged-goals",error);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final boolean gate;SnowGolem golem;Cow target;
        GoalSelector selector;RangedAttackGoal goal;WrappedGoal wrapper;int began,phase,when,delay,visibility;
        MatchEntityState image;final Set<UUID> shots=new HashSet<>();boolean closed;
        MobGoalCheckpointAccess.MobAccess selectors(){return (MobGoalCheckpointAccess.MobAccess)golem;}
        MobGoalCheckpointAccess.RangedAttackGoalAccess ranged(){return (MobGoalCheckpointAccess.RangedAttackGoalAccess)goal;}
        Fixture(GameController game,boolean gate){this.game=game;this.gate=gate;f=new WoolBucketChecks.Fixture(game,false);}
        void begin(){
            f.prepareParticipants();var floor=BlockPos.containing(f.home).above(3);
            for(int x=0;x<14;x++)for(int z=-2;z<=2;z++){
                var p=floor.offset(x,0,z);f.set(p,Blocks.STONE.defaultBlockState());
                for(int y=1;y<=3;y++)f.set(p.above(y),Blocks.AIR.defaultBlockState());
            }
            var at=Vec3.atCenterOf(floor.offset(4,0,0)).add(0,.5,0);
            target=new Cow(EntityTypes.COW,f.level);target.setPos(at.add(3,0,0));target.setNoAi(true);target.setOnGround(true);f.own(target);
            golem=new SnowGolem(EntityTypes.SNOW_GOLEM,f.level);golem.setPos(at);golem.setOnGround(true);f.own(golem);
            selector=selectors().skywars2$goalGoalSelector();selector.removeAllGoals(g->true);
            selectors().skywars2$goalTargetSelector().removeAllGoals(g->true);
            goal=new RangedAttackGoal(golem,1,20,20,10);selector.addGoal(1,goal);
            wrapper=selector.getAvailableGoals().iterator().next();golem.setTarget(target);
            require(f.level.addFreshEntity(target)&&f.level.addFreshEntity(golem),"real native ranged actors");began=game.server().getTickCount();
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-began<100,"bounded real ranged activation");
            if(phase>0){
                int elapsed=game.server().getTickCount()-when;observeShots();
                if(elapsed<delay-1){require(shots.isEmpty(),"no native shot before restored attack delay");return false;}
                if(shots.isEmpty())return false;
                require(shots.size()==1&&wrapper.isRunning()&&ranged().skywars2$goalTarget()==target,"exactly one actual native volley at restored target");
                if(phase==2)return true;
                removeShots();restore();phase=2;when=game.server().getTickCount();return false;
            }
            if(!wrapper.isRunning()||ranged().skywars2$goalSeeTime()<2)return false;
            require(ranged().skywars2$goalTarget()==target,"native goal really acquired its cached target");
            delay=ranged().skywars2$goalAttackTime();visibility=ranged().skywars2$goalSeeTime();
            require(delay>8,"saved delayed native volley");image=new MatchEntityState(game,golem);
            if(gate)wrapper.stop();else selector.removeAllGoals(g->true);
            require(ranged().skywars2$goalTarget()==null&&!wrapper.isRunning(),"native stop actually cleared the cache");
            boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
            try{image.load(golem);}finally{game.server().tickRateManager().setFrozen(frozen);}
            if(gate){
                require(!wrapper.isRunning(),"default image does not enable unfinished task replay with an empty cache");
                selector.tick();require(wrapper.isRunning()&&ranged().skywars2$goalTarget()==target,"native admission initializes stopped task safely");
                return true;
            }
            require(wrapper.isRunning(),"saved native running wrapper restored");
            verify();selector.tickRunningGoals(false);
            require(ranged().skywars2$goalAttackTime()==delay-1,"native half-step advances restored countdown exactly once");
            observeShots();require(shots.isEmpty(),"native half-step does not fire a fresh immediate volley");
            phase=1;when=game.server().getTickCount();return false;
        }
        void restore(){
            boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
            try{image.load(golem);}finally{game.server().tickRateManager().setFrozen(frozen);}
            verify();selector.tickRunningGoals(false);
        }
        void verify(){
            require(wrapper.isRunning()&&ranged().skywars2$goalTarget()==target,"cached target restored before any native task tick");
            require(ranged().skywars2$goalAttackTime()==delay&&ranged().skywars2$goalSeeTime()==visibility,"restore native ranged delay and accumulated visibility");
        }
        void observeShots(){for(var shot:f.level.getEntitiesOfClass(Projectile.class,golem.getBoundingBox().inflate(48),p->p.getOwner()==golem)){shots.add(shot.getUUID());f.own(shot);}}
        void removeShots(){for(var shot:f.level.getEntitiesOfClass(Projectile.class,golem.getBoundingBox().inflate(48),p->p.getOwner()==golem))shot.discard();shots.clear();}
        @Override public void close(){if(closed)return;closed=true;
            if(golem!=null)for(var shot:f.level.getEntitiesOfClass(Projectile.class,golem.getBoundingBox().inflate(48),p->p.getOwner()==golem))f.own(shot);
            f.close();}
    }
}
