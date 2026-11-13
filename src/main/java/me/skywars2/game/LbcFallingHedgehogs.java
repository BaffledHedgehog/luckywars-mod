package me.skywars2.game;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

/** Skill13: ordinary native falling silverfish, with immutable WILD causal authority. */
public final class LbcFallingHedgehogs {
    private static final String TAG="skywars2.falling_hedgehog";
    private static final String AUTHOR="skywars2.falling_hedgehog.author:";
    private static final String ORDER="skywars2.falling_hedgehog.order:";
    private static final ScoreHolder CLOCK=ScoreHolder.forNameOnly("#falling_hedgehogs");
    private final GameController game;
    private final Set<Silverfish> actors=Collections.newSetFromMap(new IdentityHashMap<>());

    public LbcFallingHedgehogs(GameController game){this.game=game;}
    public static boolean managed(Entity entity){return entity!=null&&entity.entityTags().contains(TAG);}
    public int trackedCount(){return actors.size();}
    List<Silverfish> snapshot(){return List.copyOf(actors);}

    private LbcIndirectKills.Owner cause(Entity actor){
        UUID author=null;long order=0;
        for(var tag:actor.entityTags())try{
            if(tag.startsWith(AUTHOR))author=UUID.fromString(tag.substring(AUTHOR.length()));
            else if(tag.startsWith(ORDER))order=Long.parseLong(tag.substring(ORDER.length()));
        }catch(IllegalArgumentException ignored){}
        return author==null||order<=0?null:new LbcIndirectKills.Owner(author,order,true,false);
    }
    private LbcSpellSource.Source source(Entity actor){
        var cause=cause(actor);
        return cause==null?null:new LbcSpellSource.Source(actor.getUUID(),cause.player(),game.items().epoch(),cause,Set.of(),true);
    }
    private boolean live(Entity actor){return !actor.isRemoved()&&game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&game.lbc().skillsEnabled()&&game.items().currentEpoch(actor)&&LbcDimensionItems.ordinary(actor.level());}
    private boolean suppressed(ServerLevel level,Vec3 at){return game.items().magic().suppressedAt(level,at)
            ||game.items().magic().mechanismSuppressedAt(level,at);}
    private boolean resident(ServerLevel level,Vec3 at){var pos=BlockPos.containing(at);
        return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    private boolean residentBlast(ServerLevel level,Vec3 at){
        // Covers native damage exposure (radius*2), as well as the shorter shared block rays.
        for(int x=net.minecraft.util.Mth.floor(at.x-15)>>4;x<=net.minecraft.util.Mth.floor(at.x+15)>>4;x++)
            for(int z=net.minecraft.util.Mth.floor(at.z-15)>>4;z<=net.minecraft.util.Mth.floor(at.z+15)>>4;z++)
                if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }
    private boolean paused(Entity actor){return game.items().timeStop().stopped((ServerLevel)actor.level())
            ||game.items().timeStop().frozen(actor)||actor.entityTags().contains("time_stopped");}

    /** The native scoreboard clock is already included in full match rewind. */
    public void tick(){
        if(game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally())return;
        for(var actor:List.copyOf(actors)){
            if(!live(actor)||cause(actor)==null){retire(actor);continue;}
            if(paused(actor))continue;
            var level=(ServerLevel)actor.level();
            if(!resident(level,actor.position()))continue;
            if(suppressed(level,actor.position())){retire(actor);continue;}
            if(actor.onGround())impact(actor);
        }
        if(!game.playing()||game.stage()==GameController.Stage.ENDING||!game.lbc().skillsEnabled()
                ||game.items().timeStop().stopped(game.maps().level()))return;
        var clock=game.items().mana().score(CLOCK,"lbc.math");int next=Math.floorMod(clock.get(),100)+1;
        clock.set(next==100?0:next);
        if(next!=100)return;
        for(var player:game.alivePlayers())if(game.lbc().hasSkill(player,13)&&!paused(player))spawn(player);
    }

    Silverfish spawn(ServerPlayer owner){
        if(!game.lbc().hasSkill(owner,13)||game.server().tickRateManager().isFrozen()
                ||!game.server().tickRateManager().runsNormally()||paused(owner)||suppressed(owner.level(),owner.position()))return null;
        var level=owner.level();
        // Existing spreadplayers ground/two-air rules; every candidate is resident before reading terrain.
        var ground=LbcSurfaceTeleport.destination(level,owner.position(),70,level.getMaxY()+1,owner.getRandom(),
                at->resident(level,at.add(0,100,0))&&!suppressed(level,at.add(0,100,0)));
        if(ground==null)return null;
        var actor=new Silverfish(EntityTypes.SILVERFISH,level);var at=ground.add(0,100,0);
        // Legacy leaves native AI enabled. NoAI also stops Silverfish travel/gravity.
        actor.setPos(at);actor.setPermanentlyInvulnerable(true);actor.setPersistenceRequired();actor.setOnGround(false);
        // Accept this hazard independently of any unrelated spell currently calling the spawn hook.
        var accepted=game.items().indirectKills().coldCause(owner,false);
        actor.entityTags().addAll(List.of(TAG,"angry_hedgehog","lucky_mob",AUTHOR+accepted.player(),ORDER+accepted.order()));
        game.items().markCurrentItemEpoch(actor);
        game.items().spellSources().bind(actor,source(actor));game.items().timeStop().prepareFrozen(actor);
        if(!level.addFreshEntity(actor)){actor.discard();return null;}
        actors.add(actor);
        level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH,0xff000000),true,false,at.x,at.y,at.z,2,0,0,0,0);
        LbcOverload.sound(level,at,"minecraft:entity.firework_rocket.large_blast",7,.8f,0,0);
        return actor;
    }

    void impact(Silverfish actor){
        if(!actors.contains(actor)||!live(actor)||paused(actor))return;
        var level=(ServerLevel)actor.level();var at=actor.position();var accepted=source(actor);
        if(!resident(level,at))return;
        if(accepted==null||suppressed(level,at)){retire(actor);return;}
        if(!residentBlast(level,at))return;
        // Detach first: a synchronous damage/death callback cannot explode this identity twice.
        actors.remove(actor);actor.setOnGround(false);
        net.minecraft.world.entity.item.PrimedTnt[] temporary={null};
        try(var scope=game.items().spellSources().scope(accepted,level)){
            game.items().indirectKills().trapChanges(level,accepted.cause(),()->
                    game.items().exasperato().encounterBlast(actor,level,at,7,
                            tnt->{temporary[0]=tnt;tnt.setCustomName(net.minecraft.network.chat.Component.translatable("angry_baffled"));game.items().spellSources().bind(tnt,accepted);}));
        }finally{
            // The synchronous explosion carrier never joins the world and has no unload event.
            if(temporary[0]!=null)game.items().spellSources().unloaded(temporary[0]);
            actor.discard();
        }
    }

    public void loaded(Entity entity){
        if(!(entity instanceof Silverfish actor)||!managed(actor))return;
        if(cause(actor)==null){if(!MatchActorCheckpoint.restoring(actor))actor.discard();return;}
        if(!live(actor)&&!MatchActorCheckpoint.restoring(actor)){actor.discard();return;}
        actors.add(actor);game.items().spellSources().bind(actor,source(actor));game.items().timeStop().prepareFrozen(actor);
    }
    public void unloaded(Entity entity){actors.remove(entity);}
    private void retire(Silverfish actor){actors.remove(actor);actor.discard();}
    public void stop(){for(var actor:List.copyOf(actors))retire(actor);actors.clear();
        var board=game.server().getScoreboard();var objective=board.getObjective("lbc.math");
        if(objective!=null&&board.getPlayerScoreInfo(CLOCK,objective)!=null)board.getOrCreatePlayerScore(CLOCK,objective).set(0);
    }
}
