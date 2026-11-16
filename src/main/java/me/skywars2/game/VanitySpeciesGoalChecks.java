package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.DefendVillageTargetGoal;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.equine.*;
import net.minecraft.world.entity.animal.golem.*;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Native owner/task transitions, followed by real server-clock anger replay. */
final class VanitySpeciesGoalChecks implements AutoCloseable {
    private final GameController game;
    private final WoolBucketChecks.Fixture f;
    private final MatchActorCheckpoint before;
    private final MatchPlayerCheckpoint player;
    private IronGolem neutral;
    private MatchActorCheckpoint anger;
    private long capturedWorld;
    private int when,cycles;
    private boolean closed;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity species goals: "+why);}
    private static int count(Object value,String name){return VanityActionGoalChecks.count(value,name);}
    private static GoalSelector selector(Mob mob){return VanityActionGoalChecks.selector(mob);}
    private static void restore(GameController game,MatchActorCheckpoint image,MatchPlayerCheckpoint player,Set<UUID> omitted){
        VanityActionGoalChecks.restore(game,image,player,omitted);
    }
    VanitySpeciesGoalChecks(GameController game,WoolBucketChecks.Fixture f){
        this.game=game;this.f=f;player=new MatchPlayerCheckpoint(game,f.owner);before=MatchActorCheckpoint.captureRegistered(game);
        try{
            shoulder();horses();caravan();flower();defend();VanityRaidChecks.verify(game,f);
            neutral=add(new IronGolem(EntityTypes.IRON_GOLEM,f.level),origin(36,80));
            neutral.setPersistentAngerEndTime(f.level.getGameTime()+200);capturedWorld=f.level.getGameTime();anger=MatchActorCheckpoint.captureRegistered(game);when=game.server().getTickCount();
        }catch(Throwable failure){try{close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}throw failure;}
    }
    private Vec3 origin(int x,int z){return Vec3.atBottomCenterOf(BlockPos.containing(f.home).above(4).offset(x,0,z));}
    private <T extends Mob>T add(T mob,Vec3 at){return VanityActionGoalChecks.add(f,mob,at);}
    boolean step(){
        f.client.acknowledge();f.audience.acknowledge();if(game.server().getTickCount()-when<5)return false;
        require(f.level.getGameTime()>capturedWorld,"actual server time advanced before restoring absolute anger deadline");
        restore(game,anger,null,Set.of());
        require(neutral.getPersistentAngerEndTime()-f.level.getGameTime()==200,"saved anger duration rebases onto current native world clock");
        if(++cycles<3){when=game.server().getTickCount();return false;}close();return true;
    }
    private void shoulder(){
        var at=origin(0,76);platform(at,2);f.move(f.owner,at);f.owner.setOnGround(true);require(!f.owner.getAbilities().flying&&f.owner.getShoulderEntityLeft().isEmpty()&&f.owner.getShoulderEntityRight().isEmpty(),"eligible actual owner has empty shoulders");
        var bird=add(new Parrot(EntityTypes.PARROT,f.level),at);bird.tame(f.owner);bird.setOrderedToSit(false);
        var goal=new LandOnOwnersShoulderGoal(bird);selector(bird).addGoal(1,goal);
        for(int i=0;i<86;i++)bird.tick();int saved=count(bird,"rideCooldownCounter");require(saved==86&&!goal.canUse(),"native shoulder cooldown has not expired");
        var owner=new MatchPlayerCheckpoint(game,f.owner);var image=MatchActorCheckpoint.captureRegistered(game);
        for(int replay=0;replay<3;replay++){
            int waited=0;while(!goal.canUse()&&waited<120){bird.tick();waited++;}
            require(waited==101-saved,"same remaining native bird ticks to shoulder admission");
            // Cooldown ticks may move the bird. Enter actual contact only once the native gate is ready.
            bird.setPos(f.owner.position());bird.setDeltaMovement(Vec3.ZERO);f.owner.setOnGround(true);
            require(bird.getBoundingBox().intersects(f.owner.getBoundingBox())&&f.owner.onGround()&&!f.owner.isPassenger()&&!f.owner.isInWater()&&!f.owner.isInPowderSnow,"actual native shoulder placement prerequisites");
            var wrapper=selector(bird).getAvailableGoals().iterator().next();for(int tick=0;tick<8&&!wrapper.isRunning();tick++)selector(bird).tick();
            require(bird.isRemoved()&&(Boolean)WoolBucketChecks.read(goal,"isSittingOnShoulder")&&!f.owner.getShoulderEntityLeft().isEmpty(),"native task really serialized bird onto player shoulder: running="+wrapper.isRunning()+",ground="+f.owner.onGround()+",contact="+bird.getBoundingBox().intersects(f.owner.getBoundingBox())+",bird="+bird.position()+",owner="+f.owner.position());
            restore(game,image,owner,Set.of());f.client.acknowledge();
            require(!bird.isRemoved()&&f.owner.getShoulderEntityLeft().isEmpty()&&count(bird,"rideCooldownCounter")==saved&&!(Boolean)WoolBucketChecks.read(goal,"isSittingOnShoulder"),"original world bird, empty shoulder and saved cooldown/task flag restore");
        }
    }
    private void horses(){
        var at=origin(14,76);platform(at,10);
        var horse=add(new Horse(EntityTypes.HORSE,f.level),at);var stand=new RandomStandGoal(horse);selector(horse).addGoal(1,stand);
        var wrapper=selector(horse).getAvailableGoals().iterator().next();
        for(int attempt=0;attempt<500000&&!wrapper.isRunning();attempt++)selector(horse).tick();
        require(wrapper.isRunning()&&horse.isStanding(),"actual native random-stand admission raised horse");
        horse.handleStartJump(1);for(int i=0;i<3;i++)horse.tick();int standing=count(horse,"standCounter"),interval=count(stand,"nextStand");
        require(standing==17,"native owner rearing countdown advanced");var image=MatchActorCheckpoint.captureRegistered(game);
        for(int replay=0;replay<2;replay++){
            int left=0;while(horse.isStanding()&&left<32){horse.tick();left++;}require(left==standing,"native horse stops rearing after saved remaining ticks");
            for(int i=0;i<7;i++)stand.canUse();restore(game,image,null,Set.of());
            require(horse.isStanding()&&count(horse,"standCounter")==standing&&count(stand,"nextStand")==interval,"native rearing and random-admission phase restore together");
        }
        var untamed=add(new Horse(EntityTypes.HORSE,f.level),at.add(2,0,3));require(f.owner.startRiding(untamed,true,false),"real player riding untamed native horse");
        var run=new RunAroundLikeCrazyGoal(untamed,1);selector(untamed).addGoal(1,run);var running=selector(untamed).getAvailableGoals().iterator().next();
        for(int attempt=0;attempt<1024&&!running.isRunning();attempt++)selector(untamed).tick();require(running.isRunning()&&untamed.getNavigation().getPath()!=null,"native wild horse route admission");
        // Admission also ticks the task: it can already eject this rider and raise temper.
        // Establish an untamed, zero-temper rider scene before capturing the actual future ejection.
        require(!untamed.isTamed(),"fresh horse did not tame during its initial native admission");untamed.modifyTemper(-untamed.getTemper());
        if(!untamed.isVehicle())require(f.owner.startRiding(untamed,true,false),"restore real rider after early native admission ejection");
        require(f.owner.getVehicle()==untamed,"native wild-horse image starts with its actual rider");
        var route=untamed.getNavigation().getPath();var pos=List.of(WoolBucketChecks.read(run,"posX"),WoolBucketChecks.read(run,"posY"),WoolBucketChecks.read(run,"posZ"));
        int temper=untamed.getTemper();var owner=new MatchPlayerCheckpoint(game,f.owner);var riding=MatchActorCheckpoint.captureRegistered(game);
        for(int replay=0;replay<2;replay++){
            for(int ticks=0;untamed.isVehicle()&&ticks<1000;ticks++)run.tick();require(!untamed.isVehicle()&&untamed.getTemper()==temper+5,"actual native wild-horse tick ejects rider and changes temper");
            for(int attempt=0;attempt<128;attempt++)if(run.canUse()&&!List.of(WoolBucketChecks.read(run,"posX"),WoolBucketChecks.read(run,"posY"),WoolBucketChecks.read(run,"posZ")).equals(pos))break;
            restore(game,riding,owner,Set.of());f.client.acknowledge();
            require(f.owner.getVehicle()==untamed&&untamed.getTemper()==temper&&untamed.getNavigation().getPath()==route&&List.of(WoolBucketChecks.read(run,"posX"),WoolBucketChecks.read(run,"posY"),WoolBucketChecks.read(run,"posZ")).equals(pos),"original rider, temper, route and wild-horse destination restore");
        }
        f.owner.stopRiding();
    }
    private void platform(Vec3 center,int radius){
        var floor=BlockPos.containing(center).below();for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            var p=floor.offset(x,0,z);f.set(p,Blocks.STONE.defaultBlockState());for(int y=1;y<=4;y++)f.set(p.above(y),Blocks.AIR.defaultBlockState());
        }
    }
    private void caravan(){
        var at=origin(0,92);for(int x=-1;x<=37;x++)for(int z=-1;z<=1;z++)f.set(BlockPos.containing(at).offset(x,-1,z),Blocks.STONE.defaultBlockState());
        var holder=add(new Cow(EntityTypes.COW,f.level),at.add(0,0,2));var first=add(new Llama(EntityTypes.LLAMA,f.level),at);first.setLeashedTo(holder,true);
        var middle=add(new Llama(EntityTypes.LLAMA,f.level),at.add(4,0,0));var last=add(new Llama(EntityTypes.LLAMA,f.level),at.add(8,0,0));
        var follow=new LlamaFollowCaravanGoal(middle,2.1);var tail=new LlamaFollowCaravanGoal(last,2.1);
        var running=VanityActionGoalChecks.admit(middle,follow);var following=VanityActionGoalChecks.admit(last,tail);
        require(middle.getCaravanHead()==first&&last.getCaravanHead()==middle,"native admission formed actual three-member caravan");
        middle.setPos(at.add(35,0,0));for(int i=0;i<3;i++)require(follow.canContinueToUse(),"native far follower accelerates and starts timeout");follow.tick();
        double speed=(Double)WoolBucketChecks.read(follow,"speedModifier");int timeout=count(follow,"distCheckCounter");require(speed>3&&timeout>0,"real native catch-up speed and remaining timeout");
        var image=MatchActorCheckpoint.captureRegistered(game);int left=caravanExpiry(follow);running.stop();
        for(int replay=0;replay<2;replay++){
            restore(game,image,null,Set.of());
            require(middle.getCaravanHead()==first&&last.getCaravanHead()==middle&&WoolBucketChecks.read(first,"caravanTail")==middle&&WoolBucketChecks.read(middle,"caravanTail")==last,"both original caravan edges restore without join callback");
            require((Double)WoolBucketChecks.read(follow,"speedModifier")==speed&&count(follow,"distCheckCounter")==timeout&&caravanExpiry(follow)==left,"same native acceleration phase and expiry interval");running.stop();
        }
        restore(game,image,null,Set.of(middle.getUUID()));require(!first.hasCaravanTail()&&!last.inCaravan()&&!following.isRunning(),"omitted middle member leaves no active dangling edges/task");
        restore(game,image,null,Set.of(first.getUUID()));require(!middle.inCaravan()&&!last.inCaravan()&&!running.isRunning()&&!following.isRunning(),"omitted root retires downstream tasks without join/leave replay");
        restore(game,image,null,Set.of());require(middle.inCaravan()&&last.inCaravan()&&running.isRunning()&&following.isRunning(),"same full caravan image remains reusable");
    }
    private static int caravanExpiry(LlamaFollowCaravanGoal goal){int ticks=0;while(goal.canContinueToUse()&&ticks<64)ticks++;require(ticks<64,"bounded native caravan timeout");return ticks;}
    private void flower(){
        var at=origin(28,76);platform(at,6);var golem=add(new IronGolem(EntityTypes.IRON_GOLEM,f.level),at);
        var recipient=VanityActionGoalChecks.add(f,Objects.requireNonNull(EntityTypes.COPPER_GOLEM.create(f.level,EntitySpawnReason.COMMAND)),at.add(2,0,0));
        require(f.level.isBrightOutside(),"real daytime gift admission");var goal=new OfferFlowerGoal(golem);selector(golem).addGoal(1,goal);var wrapper=selector(golem).getAvailableGoals().iterator().next();
        for(int attempt=0;attempt<500000&&!wrapper.isRunning();attempt++)selector(golem).tick();
        require(wrapper.isRunning()&&WoolBucketChecks.read(goal,"entity")==recipient,"native random gift search selected actual copper golem");
        golem.aiStep();var victim=add(new Cow(EntityTypes.COW,f.level),at.add(3,0,2));victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);victim.setHealth(100);victim.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        require(golem.doHurtTarget(f.level,victim),"genuine native golem attack animation");for(int i=0;i<2;i++)golem.aiStep();
        int attack=count(golem,"attackAnimationTick"),offering=count(golem,"offerFlowerTick"),remaining=count(goal,"tick");var image=MatchActorCheckpoint.captureRegistered(game);
        for(int replay=0;replay<3;replay++){
            for(int tick=0;tick<remaining;tick++){goal.tick();golem.aiStep();golem.aiStep();}wrapper.stop();
            require(recipient.getItemBySlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA).is(Items.POPPY)&&count(golem,"offerFlowerTick")==0,"actual native gift completion equips recipient");
            restore(game,image,null,Set.of());
            require(recipient.getItemBySlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA).isEmpty()&&WoolBucketChecks.read(goal,"entity")==recipient&&count(goal,"tick")==remaining&&count(golem,"offerFlowerTick")==offering&&count(golem,"attackAnimationTick")==attack,"gift removed and both native owner/task animation phases restored");
        }
        wrapper.stop();require(count(goal,"tick")==remaining&&count(golem,"offerFlowerTick")==0&&recipient.getItemBySlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA).isEmpty(),"native early stop retains task counter and cancels flower without a gift");
        restore(game,image,null,Set.of(recipient.getUUID()));require(!wrapper.isRunning()&&WoolBucketChecks.read(goal,"entity")==null&&count(goal,"tick")==remaining&&count(golem,"offerFlowerTick")==0,"absent recipient matches native early-stop data without gift side effect");restore(game,image,null,Set.of());
    }
    private void defend(){
        var at=origin(40,76);platform(at,4);f.move(f.owner,at.add(2,0,0));
        var villager=add(new Villager(EntityTypes.VILLAGER,f.level),at.add(1,0,0));villager.getGossips().add(f.owner.getUUID(),GossipType.MAJOR_NEGATIVE,100);
        var golem=add(new IronGolem(EntityTypes.IRON_GOLEM,f.level),at);var goal=new DefendVillageTargetGoal(golem);
        var targets=(GoalSelector)WoolBucketChecks.read(golem,"targetSelector");targets.addGoal(1,goal);targets.tick();var wrapper=targets.getAvailableGoals().iterator().next();
        require(wrapper.isRunning()&&WoolBucketChecks.read(goal,"potentialTarget")==f.owner&&golem.getTargetUnchecked()==f.owner,"actual negative gossip drives native defense target selection");
        var image=MatchActorCheckpoint.captureRegistered(game);for(int replay=0;replay<2;replay++){targets.removeAllGoals(g->true);restore(game,image,null,Set.of());require(wrapper.isRunning()&&WoolBucketChecks.read(goal,"potentialTarget")==f.owner&&golem.getTargetUnchecked()==f.owner,"native village-defense cache and raw target restore");}
    }
    @Override public void close(){if(closed)return;closed=true;restore(game,before,player,Set.of());f.client.acknowledge();}
}
