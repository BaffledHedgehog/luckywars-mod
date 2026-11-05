package me.skywars2.game;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** The source's 28 ordered piston rays, repeated fifteen times against the preceding mutations. */
public final class LbcImbaShovel {
    public static final int RAYS=420,SAMPLES=201;
    private static final int SAMPLE_BATCH=16;
    private static final int[][] OFFSETS={{0,0},{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1},
            {-1,2},{1,2},{0,2},{2,2},{2,1},{2,0},{2,-1},{-2,2},{-2,1},{-2,0},{-2,-1},
            {3,2},{3,1},{3,0},{3,-1},{-3,2},{-3,1},{-3,0},{-3,-1}};
    private static final BlockParticleOption PARTICLE=new BlockParticleOption(ParticleTypes.BLOCK,Blocks.REDSTONE_BLOCK.defaultBlockState());
    public interface Policy {
        default boolean allowsActivation(ServerPlayer owner){return true;}
        default boolean allowsActive(ServerPlayer owner,ServerLevel level,Vec3 from,Vec3 at){return true;}
    }
    public record Progress(UUID author,Vec3 origin,Vec3 direction,int ray,int sample,Vec3 point,long rng){}
    private final GameController game;
    private Policy policy=new Policy(){};
    public LbcImbaShovel(GameController game){this.game=game;}
    public void policy(Policy policy){this.policy=Objects.requireNonNull(policy);}
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"chaos_sho",0)==1;}
    /** Inspection traverses only the existing shared work queue, on demand. There is no second job index. */
    public List<Progress> snapshot(){return game.items().stonekivi().queuedSnapshot().stream()
            .filter(Stroke.class::isInstance).map(Stroke.class::cast).map(Stroke::progress).toList();}
    private boolean active(ServerLevel level,String epoch){return game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&!game.maps().busy()&&level==game.maps().level()&&game.items().currentEpoch(epoch)
            &&LbcDimensionItems.ordinary(level)&&LbcBedwarsRoleGuards.publication(level);}
    private boolean admitted(ServerPlayer owner){return active(owner.level(),game.items().epoch())&&owner.isAlive()&&!owner.isRemoved()
            &&!owner.isSpectator()&&!game.isObserver(owner)&&!game.awaitingRespawn(owner.getUUID())
            &&game.canUseGameplayItems(owner)&&game.items().wands().validRoleTarget(owner,false)
            &&!game.items().actionsBlocked(owner)&&!game.items().wands().inputTimeStopped(owner)
            &&!game.items().timeStop().stopped(owner.level())&&!game.server().tickRateManager().isFrozen()
            &&game.server().tickRateManager().runsNormally();}
    /** Called for the actual block break or melee tool action, with the pre-damage tool snapshot. */
    public boolean used(ServerPlayer owner,ItemStack stack){
        return usedAt(owner,stack,owner.getEyePosition(),owner.getYRot(),owner.getXRot());
    }
    /** Accepted action pose stays fixed while its queued terrain work advances. */
    boolean usedAt(ServerPlayer owner,ItemStack stack,Vec3 eye,float yaw,float pitch){
        if(!matches(stack)||!admitted(owner)||!policy.allowsActivation(owner))return false;
        game.items().stonekivi().queuedWork(new Stroke(owner,eye,yaw,pitch));return true;
    }
    private static boolean within(ServerLevel level,BlockPos pos){return !level.isOutsideBuildHeight(pos)&&level.getWorldBorder().isWithinBounds(pos);}
    private static boolean resident(ServerLevel level,BlockPos pos){return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    private final class Stroke implements LbcStonekivi.Work {
        ServerPlayer owner;
        final ServerLevel level;
        final String epoch;
        final LbcIndirectKills.Owner cause;
        final Vec3 origin,direction,left,up,step;
        final Vec3[] starts=new Vec3[OFFSETS.length];
        int ray,sample;
        Vec3 point;
        long rng;
        boolean cancelled;
        Stroke(ServerPlayer owner,Vec3 eye,float yaw,float pitch){
            this.owner=owner;level=owner.level();epoch=game.items().epoch();cause=game.items().indirectKills().coldCause(owner,false);
            origin=eye;direction=Vec3.directionFromRotation(pitch,yaw);step=direction.scale(.33);
            left=Vec3.directionFromRotation(0,yaw-90);up=Vec3.directionFromRotation(pitch-90,yaw);
            for(int i=0;i<OFFSETS.length;i++)starts[i]=origin.add(left.scale(OFFSETS[i][0])).add(up.scale(OFFSETS[i][1]));
            rng=owner.getRandom().nextLong();point=rayOrigin();
        }
        Vec3 rayOrigin(){return starts[ray%OFFSETS.length];}
        Progress progress(){return new Progress(cause.player(),origin,direction,ray,sample,point,rng);}
        @Override public boolean snapshotIdle(){return true;}
        @Override public boolean ready(){
            if(cancelled||!active(level,epoch))return true;
            if(game.items().timeStop().stopped(level)||game.server().tickRateManager().isFrozen()
                    ||!game.server().tickRateManager().runsNormally())return false;
            return true;
        }
        @Override public boolean step(){
            if(cancelled||!active(level,epoch)){cancel();return true;}
            for(int budget=0;budget<SAMPLE_BATCH;budget++){
                var pos=BlockPos.containing(point);
                if(!policy.allowsActive(owner,level,rayOrigin(),point)){cancel();return true;}
                if(chance15())level.sendParticles(PARTICLE,point.x,point.y,point.z,1,0,0,0,0);
                // An unavailable source block condition fails, but the following source
                // recursion still advances. Never load or indefinitely wait on that sample.
                if(within(level,pos)&&resident(level,pos)&&!level.getBlockState(pos).is(Blocks.AIR)){
                    // This is the existing atomic container/provenance/rollback owner. No jammer or piston sound is added.
                    game.items().pendingCombat().shift(level,cause,pos,direction);
                    if(nextRay())return true;
                }else if(++sample==SAMPLES){if(nextRay())return true;}
                else point=point.add(step);
            }
            return false;
        }
        private boolean nextRay(){if(++ray==RAYS){cancel();return true;}sample=0;point=rayOrigin();return false;}
        private boolean chance15(){
            long bits,result;
            do{long z=(rng+=0x9e3779b97f4a7c15L);z=(z^(z>>>30))*0xbf58476d1ce4e5b9L;z=(z^(z>>>27))*0x94d049bb133111ebL;
                bits=(z^(z>>>31))>>>1;result=bits%100;
            }while(bits-result+99<0);
            return result<15;
        }
        @Override public void cancel(){cancelled=true;}
        @Override public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            game.items().stonekivi().checkpointBoundary();var author=graph.links.key(owner);
            int savedRay=ray,savedSample=sample;Vec3 savedPoint=point;long savedRng=rng;boolean savedCancelled=cancelled;
            return links->{
                game.items().stonekivi().checkpointBoundary();MatchAuthorityCheckpoint.world(game,level);
                var restored=(ServerPlayer)links.get(author);
                return ()->{owner=restored;ray=savedRay;sample=savedSample;point=savedPoint;rng=savedRng;cancelled=savedCancelled;};
            };
        }
    }
}
