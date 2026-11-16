package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Native selector and melee cooldown must resume, without replaying Goal.start/stop during load. */
public final class VanityGoalChecks {
    private static Fixture running;
    private VanityGoalChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity goals: "+why);}
    public static void run(GameController game){
        require(MatchMobGoals.enabled(),"native goal replay experiment explicitly enabled in isolated fixture");
        require(running==null,"one native goal scene");var f=new Fixture(game);running=f;
        try{f.begin();}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game){
        if(running==null)return;var f=running;
        try{
            require(f.game==game,"same match");if(!f.step())return;VanityTargetGoalChecks.verify(game,f.f);running=null;f.close();
            SkyWars2.LOGGER.info("[check] PASS vanity-goals (actual native melee hit; selector/lock/flag/wrapper identity; no start/stop replay; saved melee cooldown/path/world age; repeated delayed attacks; native target search, sight-memory expiry, hurt deduplication; actual repeated breeding/birth timing/future removal and absent partner)");
        }catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL vanity-goals",error);}
    }
    private static int count(Object goal,String name){return (Integer)WoolBucketChecks.read(goal,name);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;
        Zombie zombie;Cow cow;GoalSelector selector,targets;MeleeAttackGoal melee;
        WrappedGoal original,future;List<WrappedGoal> order;Set<WrappedGoal> available;
        Map<?,?> locks;EnumSet<?> disabled;EnumSet<Goal.Flag> flags;
        MatchEntityState image,victim;Path goalPath;int cooldown,phase,when;long capturedAt,lastCheck;
        VanityBreedingGoalChecks breeding;
        VanityCommonGoalChecks common;
        VanityRetainedGoalChecks retained;
        VanityCreeperGoalChecks creeper;
        VanityDoorGoalChecks doors;
        VanitySpeciesGoalChecks species;
        float health;boolean closed;
        Fixture(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);}
        void begin(){
            f.prepareParticipants();BlockPos floor=BlockPos.containing(f.home).above(3);
            for(int x=0;x<18;x++)for(int z=-2;z<=2;z++){
                var p=floor.offset(x,0,z);f.set(p,Blocks.STONE.defaultBlockState());
                for(int y=1;y<=3;y++)f.set(p.above(y),Blocks.AIR.defaultBlockState());
            }
            var at=Vec3.atCenterOf(floor.offset(5,0,0)).add(0,.5,0);
            cow=new Cow(EntityTypes.COW,f.level);cow.setPos(at.add(1.15,0,0));cow.setNoAi(true);cow.setOnGround(true);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);
            cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            zombie=new Zombie(EntityTypes.ZOMBIE,f.level);zombie.setPos(at);zombie.setOnGround(true);zombie.setBaby(false);
            zombie.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.LEATHER_HELMET));
            zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            selector=(GoalSelector)WoolBucketChecks.read(zombie,"goalSelector");targets=(GoalSelector)WoolBucketChecks.read(zombie,"targetSelector");
            selector.removeAllGoals(goal->true);targets.removeAllGoals(goal->true);
            selector.addGoal(1,new FloatGoal(zombie));melee=new MeleeAttackGoal(zombie,1,true);selector.addGoal(2,melee);
            zombie.setTarget(cow);
            require(f.level.addFreshEntity(cow)&&f.level.addFreshEntity(zombie),"ordinary native actors added");f.own(cow);f.own(zombie);
            when=game.server().getTickCount();
        }
        boolean step(){
            if(species!=null)return species.step();
            if(doors!=null){if(!doors.step())return false;species=new VanitySpeciesGoalChecks(game,f);return false;}
            if(creeper!=null){if(!creeper.step())return false;doors=new VanityDoorGoalChecks(game,f);return false;}
            if(retained!=null){if(!retained.step())return false;creeper=new VanityCreeperGoalChecks(game,f);return false;}
            if(common!=null){if(!common.step())return false;retained=new VanityRetainedGoalChecks(game,f);return false;}
            if(breeding!=null){if(!breeding.step())return false;common=new VanityCommonGoalChecks(game,f);return false;}
            f.client.acknowledge();f.audience.acknowledge();int elapsed=game.server().getTickCount()-when;
            require(elapsed<100,"bounded native scene phase="+phase+", HP="+cow.getHealth());
            if(phase==0){
                if(cow.getHealth()==100)return false;
                available=selector.getAvailableGoals();order=List.copyOf(available);
                original=available.stream().filter(g->g.getGoal()==melee).findFirst().orElseThrow();
                require(original.isRunning(),"native selector started melee before actual health loss");
                cooldown=count(melee,"ticksUntilNextAttack");require(cooldown>8,"capture a pending native attack cooldown");
                flags=melee.getFlags().clone();locks=(Map<?,?>)WoolBucketChecks.read(selector,"lockedFlags");
                disabled=(EnumSet<?>)WoolBucketChecks.read(selector,"disabledFlags");
                goalPath=(Path)WoolBucketChecks.read(melee,"path");lastCheck=(Long)WoolBucketChecks.read(melee,"lastCanUseCheck");
                capturedAt=f.level.getGameTime();image=new MatchEntityState(game,zombie);victim=new MatchEntityState(game,cow);
                phase=1;when=game.server().getTickCount();return false;
            }
            if(phase==1){
                if(elapsed<5)return false;
                require(count(melee,"ticksUntilNextAttack")<cooldown,"native running task advanced its future timer");
                selector.removeAllGoals(goal->true);selector.addGoal(0,new MeleeAttackGoal(zombie,2,true));selector.tick();
                future=selector.getAvailableGoals().iterator().next();require(future.isRunning(),"future native task really started");
                selector.disableControlFlag(Goal.Flag.MOVE);targets.disableControlFlag(Goal.Flag.TARGET);
                melee.setFlags(EnumSet.of(Goal.Flag.JUMP));
                load();verify();health=cow.getHealth();phase=2;when=game.server().getTickCount();return false;
            }
            if(phase==2||phase==3){
                if(elapsed<cooldown-1){require(cow.getHealth()==health,"load must not restart the attack early");return false;}
                if(cow.getHealth()==health)return false;
                require(original.isRunning()&&cow.getHealth()<health,"restored original task inflicts a later actual native hit");
                if(phase==3){zombie.setNoAi(true);breeding=new VanityBreedingGoalChecks(game,f);return false;}
                load();verify();health=cow.getHealth();phase=3;when=game.server().getTickCount();return false;
            }
            throw new IllegalStateException("Unknown native goal phase");
        }
        void load(){
            boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
            try{victim.load(cow);float before=cow.getHealth();image.load(zombie);require(cow.getHealth()==before,"image load has no attack side effect");}
            finally{game.server().tickRateManager().setFrozen(frozen);}
        }
        void verify(){
            require(selector.getAvailableGoals()==available&&List.copyOf(available).equals(order),"original ordered native wrappers restored in the same selector set");
            require(original.isRunning()&&!future.isRunning(),"captured task runs and future task is retired without callbacks");
            require(WoolBucketChecks.read(selector,"lockedFlags")==locks&&locks.get(Goal.Flag.MOVE)==original&&locks.get(Goal.Flag.LOOK)==original,
                    "same native flag-lock map points to original wrapper");
            require(WoolBucketChecks.read(selector,"disabledFlags")==disabled&&!disabled.contains(Goal.Flag.MOVE)&&
                    !((Set<?>)WoolBucketChecks.read(targets,"disabledFlags")).contains(Goal.Flag.TARGET),"both selectors recover enabled controls");
            require(melee.getFlags().equals(flags),"original goal flags restored");
            require(count(melee,"ticksUntilNextAttack")==cooldown,"native melee cooldown restored without start replay");
            require(WoolBucketChecks.read(melee,"path")==goalPath,"original goal-owned path reference restored");
            require((Long)WoolBucketChecks.read(melee,"lastCanUseCheck")==lastCheck+f.level.getGameTime()-capturedAt,"native can-use timestamp retains world-clock age");
        }
        @Override public void close(){if(closed)return;closed=true;try{if(species!=null)species.close();}finally{try{if(doors!=null)doors.close();}finally{f.close();}}}
    }
}
