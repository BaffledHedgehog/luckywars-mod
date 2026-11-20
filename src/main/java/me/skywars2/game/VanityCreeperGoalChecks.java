package me.skywars2.game;

import me.skywars2.mixin.EntityRuntimeCheckpointAccess;
import me.skywars2.mixin.MobGoalCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

/** Real native fuse completion/removal, repeated with the saved remaining duration. */
final class VanityCreeperGoalChecks {
    private final GameController game;
    private final WoolBucketChecks.Fixture f;
    private final Creeper creeper;
    private MatchActorCheckpoint image;
    private int phase,when,swell,oldSwell,remaining;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity creeper goals: "+why);}
    private EntityRuntimeCheckpointAccess.CreeperAccess nativeFuse(){return (EntityRuntimeCheckpointAccess.CreeperAccess)creeper;}
    VanityCreeperGoalChecks(GameController game,WoolBucketChecks.Fixture f){
        this.game=game;this.f=f;Vec3 at=Vec3.atCenterOf(BlockPos.containing(f.home).above(3)).add(3,8,-3);
        var target=new Cow(EntityTypes.COW,f.level);target.setPos(at.add(2,0,0));target.setNoAi(true);target.setNoGravity(true);
        creeper=new Creeper(EntityTypes.CREEPER,f.level);creeper.setPos(at);creeper.setNoGravity(true);creeper.setTarget(target);
        // Native spawn configuration, not a replacement explosion or a fabricated removal.
        nativeFuse().skywars2$runtimeMaxSwell(12);
        nativeFuse().skywars2$runtimeExplosionRadius(0);
        var selectors=(MobGoalCheckpointAccess.MobAccess)creeper;
        var goals=selectors.skywars2$goalGoalSelector();goals.removeAllGoals(g->true);goals.addGoal(1,new SwellGoal(creeper));
        selectors.skywars2$goalTargetSelector().removeAllGoals(g->true);
        require(f.level.addFreshEntity(target)&&f.level.addFreshEntity(creeper),"ordinary native fuse actors");f.own(target);f.own(creeper);creeper.ignite();when=game.server().getTickCount();
    }
    boolean step(){
        f.client.acknowledge();f.audience.acknowledge();int now=game.server().getTickCount();require(now-when<40,"bounded actual native fuse phase="+phase);
        if(phase==0){
            if(nativeFuse().skywars2$runtimeSwell()<5)return false;
            require(!creeper.isRemoved()&&nativeFuse().skywars2$runtimeSwell()<10,"capture a genuine partially elapsed fuse");
            swell=nativeFuse().skywars2$runtimeSwell();oldSwell=nativeFuse().skywars2$runtimeOldSwell();image=MatchActorCheckpoint.captureRegistered(game);phase=1;when=now;return false;
        }
        if(!creeper.isRemoved()){
            if(phase>1)require(now-when<remaining,"restored creeper completes fuse at saved remaining time");return false;
        }
        require(nativeFuse().skywars2$runtimeSwell()==12,"native fuse really reached its configured limit before native explosion/removal");
        if(phase==1){remaining=now-when;require(remaining>2,"nonzero saved fuse interval");}
        else require(now-when==remaining,"same native remaining fuse interval after replay");
        boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
        try{image.restoreBodiesFrozen();require(image.restoreLinksFrozen().isEmpty(),"native swelling target resolves");}
        finally{game.server().tickRateManager().setFrozen(frozen);}
        require(!creeper.isRemoved()&&creeper.isAlive()&&nativeFuse().skywars2$runtimeSwell()==swell&&nativeFuse().skywars2$runtimeOldSwell()==oldSwell&&nativeFuse().skywars2$runtimeMaxSwell()==12,
                "original creeper body and exact native fuse phase restored after real explosion");
        if(phase++<3){when=now;return false;}creeper.discard();return true;
    }
}
