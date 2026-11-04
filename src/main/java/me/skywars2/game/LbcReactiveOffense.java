package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Source outgoing trinket reactions; native damage and the existing spell owners deliver payloads. */
public final class LbcReactiveOffense {
    private final GameController game;
    private final Set<Soul> souls=new LinkedHashSet<>();
    private final Set<UUID> reacting=new HashSet<>();
    private int callbacks;
    private boolean restoring;

    public LbcReactiveOffense(GameController game){this.game=game;}

    /** Final inventory counts are supplied by the one native player_hurt_entity callback. */
    public void outgoing(ServerPlayer attacker,LivingEntity victim,DamageSource source,boolean blocked,
                         int acidCount,int bassCount,int soulCount){
        outgoing(attacker,victim,source,blocked,acidCount,bassCount,soulCount,null);
    }
    void outgoing(ServerPlayer attacker,LivingEntity victim,DamageSource source,boolean blocked,
                         int acidCount,int bassCount,int soulCount,Runnable existing){
        if(blocked||source==null||LbcDamage.isArrowDamage(source)||source.getEntity()!=attacker
                ||(!game.playing()&&!game.items().debugItemUseWindow())||restoring
                ||!LbcDimensionItems.ordinary(attacker)||attacker.level()!=game.maps().level()
                ||victim.level()!=attacker.level()||!game.canUseGameplayItems(attacker)
                ||!LbcBedwarsRoleGuards.canEmitFrom(attacker)||LbcBedwarsRoleGuards.shop(victim))return;
        if(acidCount<=0&&bassCount<=0&&soulCount<=0&&existing==null)return;
        // The source reward revokes gowno only at the END of items/swords. Nested
        // hits by this author cannot regrant its still-completed advancement.
        if(!reacting.add(attacker.getUUID()))return;
        callbacks++;
        // A parent spell's WILD scope must not turn a newly emitted ordinary trinket into WILD.
        try(var scope=game.items().spellSources().scope((LbcSpellSource.Source)null)){
            if(roll(attacker,acidCount,150,70,14))acid(attacker);
            if(!game.items().magic().suppressed(attacker)&&roll(attacker,bassCount,150,50,0))game.items().evokerBass().trinketCast(attacker);
            if(existing!=null)existing.run();
            if(roll(attacker,soulCount,40,10,0))soul(attacker,victim);
        }finally{callbacks--;reacting.remove(attacker.getUUID());}
    }

    /** These three identities have no incoming-damage route in damaged_get_trinket. */
    public void incoming(LivingEntity victim,DamageSource source,int acidCount,int bassCount,int soulCount){}

    private static boolean roll(ServerPlayer owner,int count,int first,int extra,int guaranteed){
        if(count<=0)return false;
        if(guaranteed>0&&count>=guaranteed)return true;
        double chance=(first+(double)extra*(count-1))*.001;
        return owner.getRandom().nextFloat()<chance;
    }

    private void acid(ServerPlayer owner){
        float yaw=(Mth.floor(owner.getYRot()*100)+owner.getRandom().nextInt(6001)-3000)*.01f;
        float pitch=(Mth.floor(owner.getXRot()*100)+owner.getRandom().nextInt(4001)-2000)*.01f;
        var shot=game.items().clouds().trinketAcid(owner,owner.getEyePosition(),yaw,pitch);
        if(!shot.isRemoved())game.items().clouds().sourceSound(owner.level(),owner.position(),
                net.minecraft.sounds.SoundEvents.ZOMBIE_VILLAGER_CURE,1,2);
    }

    private void soul(ServerPlayer owner,LivingEntity victim){
        if(!(victim instanceof Mob mob)||mob.isNoAi()||!game.items().wands().validRoleTarget(mob,true)
                ||owner.isAlliedTo(mob)||LbcGreed.active(mob)
                ||owner.getEyePosition().distanceToSqr(mob.position())>576)return;
        // The approved actual-victim correction replaces source HurtTime/nearby selection.
        mob.setNoAi(true);
        var cause=game.items().indirectKills().coldCause(owner,false);
        var visual=new Soul(owner.level(),owner.getUUID(),game.items().epoch(),cause,mob.getEyePosition(),
                (owner.getRandom().nextInt(36001)-18000)*.01f);
        souls.add(visual);game.items().stonekivi().queuedWork(visual);
        LbcOverload.sound(owner,mob.position(),"minecraft:particle.soul_escape",1,1,0);
    }

    /** Queue delivery owns visual timing. This only retires stale ownership; no world/idle player scan. */
    public void tick(){
        if(restoring)return;
        for(var soul:List.copyOf(souls))if(!current(soul))soul.cancel();
    }
    private boolean current(Soul soul){return soul.active&&game.items().currentEpoch(soul.epoch)
            &&game.playing()&&game.stage()!=GameController.Stage.ENDING&&soul.level==game.maps().level();}
    public void forget(ServerPlayer player){
        if(restoring||MatchPlayerCheckpoint.restoring(player))return;
        for(var soul:List.copyOf(souls))if(soul.author.equals(player.getUUID()))soul.cancel();
    }
    public void stop(){for(var soul:List.copyOf(souls))soul.cancel();souls.clear();restoring=false;}
    public int visualCount(){return souls.size();}
    public record Visual(UUID author,Vec3 position,float yaw,int age){}
    public List<Visual> visualSnapshot(){return souls.stream().map(s->new Visual(s.author,s.position,s.yaw,s.age)).toList();}

    private final class Soul implements LbcStonekivi.Work {
        final ServerLevel level;final UUID author;final String epoch;final LbcIndirectKills.Owner cause;
        Vec3 position;float yaw;int age,lastTick;boolean active=true;
        Soul(ServerLevel level,UUID author,String epoch,LbcIndirectKills.Owner cause,Vec3 position,float yaw){
            this.level=level;this.author=author;this.epoch=epoch;this.cause=cause;this.position=position;this.yaw=yaw;
            lastTick=game.server().getTickCount();
        }
        public boolean ready(){
            return !current(this)||!restoring&&lastTick!=game.server().getTickCount()
                    &&!game.items().timeStop().stopped(level)&&level.hasChunkAt(BlockPos.containing(position));
        }
        public boolean step(){
            if(!current(this)){cancel();return true;}
            if(restoring||lastTick==game.server().getTickCount()||game.items().timeStop().stopped(level))return false;
            callbacks++;
            try{
                lastTick=game.server().getTickCount();age++;
                level.sendParticles(ParticleTypes.SOUL,position.x,position.y,position.z,1,0,0,0,0);
                // Source local (.5,.15,-.3), followed by yaw+30 and pitch0.
                double radians=Math.toRadians(yaw);
                position=position.add(.5*Math.cos(radians)+.3*Math.sin(radians),.15,
                        .5*Math.sin(radians)-.3*Math.cos(radians));
                yaw+=30;
                if(age==100)level.sendParticles(ParticleTypes.SOUL,position.x,position.y,position.z,100,0,0,0,1);
                if(age>=100){cancel();return true;}
                return false;
            }finally{callbacks--;}
        }
        public void cancel(){active=false;souls.remove(this);}
        public boolean snapshotIdle(){return callbacks==0;}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            checkpointBoundary();var savedPosition=position;float savedYaw=yaw;int savedAge=age;boolean savedActive=active;
            return links->{checkpointBoundary();MatchAuthorityCheckpoint.world(game,level);
                return ()->{position=savedPosition;yaw=savedYaw;age=savedAge;active=savedActive;lastTick=game.server().getTickCount();};};
        }
    }

    void checkpointBoundary(){
        if(!game.server().isSameThread()||callbacks!=0)throw new IllegalStateException("Capture reactive offense between native callbacks");
    }
    void prepareFrozen(){checkpointBoundary();restoring=true;for(var soul:List.copyOf(souls))soul.cancel();}
    void publishFrozen(){checkpointBoundary();for(var soul:souls)soul.lastTick=game.server().getTickCount();restoring=false;}
    void cancelFrozen(){checkpointBoundary();stop();}
    Checkpoint checkpoint(MatchQueuedWorkCheckpoint.Graph graph){checkpointBoundary();return new Checkpoint(graph);}
    final class Checkpoint {
        private final List<Soul> saved=List.copyOf(souls);
        private final int bassClock=game.items().evokerBass().checkpointClock();
        Checkpoint(MatchQueuedWorkCheckpoint.Graph graph){saved.forEach(graph::capture);}
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();saved.forEach(s->MatchAuthorityCheckpoint.world(game,s.level));
            return ()->{souls.clear();souls.addAll(saved);game.items().evokerBass().restoreClock(bassClock);};
        }
    }
}
