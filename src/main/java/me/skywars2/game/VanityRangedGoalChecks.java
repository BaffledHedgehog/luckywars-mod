package me.skywars2.game;

import me.skywars2.SkyWars2;
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
        GoalSelector selector;RangedAttackGoal goal;WrappedGoal wrapper;int began;boolean closed;
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
            selector=(GoalSelector)WoolBucketChecks.read(golem,"goalSelector");selector.removeAllGoals(g->true);
            ((GoalSelector)WoolBucketChecks.read(golem,"targetSelector")).removeAllGoals(g->true);
            goal=new RangedAttackGoal(golem,1,20,20,10);selector.addGoal(1,goal);
            wrapper=selector.getAvailableGoals().iterator().next();golem.setTarget(target);
            require(f.level.addFreshEntity(target)&&f.level.addFreshEntity(golem),"real native ranged actors");began=game.server().getTickCount();
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-began<100,"bounded real ranged activation");
            if(!wrapper.isRunning()||(Integer)WoolBucketChecks.read(goal,"seeTime")<2)return false;
            require(WoolBucketChecks.read(goal,"target")==target,"native goal really acquired its cached target");
            var image=new MatchEntityState(game,golem);
            if(gate)wrapper.stop();else selector.removeAllGoals(g->true);
            require(WoolBucketChecks.read(goal,"target")==null&&!wrapper.isRunning(),"native stop actually cleared the cache");
            boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
            try{image.load(golem);}finally{game.server().tickRateManager().setFrozen(frozen);}
            if(gate){
                require(!wrapper.isRunning(),"default image does not enable unfinished task replay with an empty cache");
                selector.tick();require(wrapper.isRunning()&&WoolBucketChecks.read(goal,"target")==target,"native admission initializes stopped task safely");
                return true;
            }
            require(wrapper.isRunning(),"saved native running wrapper restored");
            // This is the actual branch Mob.serverAiStep uses on alternate ticks: no canContinue refresh first.
            selector.tickRunningGoals(false);
            require(WoolBucketChecks.read(goal,"target")==target,"restored cached target remains the original real entity");
            return true;
        }
        @Override public void close(){if(closed)return;closed=true;
            if(golem!=null)for(var shot:f.level.getEntitiesOfClass(Projectile.class,golem.getBoundingBox().inflate(48),p->p.getOwner()==golem))f.own(shot);
            f.close();}
    }
}
