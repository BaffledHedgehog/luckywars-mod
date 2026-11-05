package me.skywars2.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** The source's discrete 301-position hoe trace, serviced by the shared fair ray queue. */
public final class LbcImbaHoe {
    public static final String TNT="skywars2.imba_hoe_tnt";
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    @FunctionalInterface public interface Policy {
        boolean activation(ServerPlayer owner);
        default boolean sample(ServerPlayer owner,ServerLevel level,Vec3 position){return true;}
    }
    private final GameController game;
    private Policy policy=owner->true;
    private final Map<Long,Ray> pending=new LinkedHashMap<>();
    private final Set<Ray> replayCancelled=new LinkedHashSet<>();
    private long sequence,uses,samples,tnt,destroyed;
    private int callbacks;
    private boolean restoring;

    public LbcImbaHoe(GameController game){this.game=game;}
    public void configure(Policy policy){checkpointBoundary();this.policy=java.util.Objects.requireNonNull(policy);}
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"chaos_pic",0)==1;}
    /** The native tool's successful break/melee callback supplies the actual tool, never a durability event. */
    public boolean used(ServerPlayer owner,ItemStack actualStack){
        if(restoring||!matches(actualStack)||!game.playing()||game.stage()==GameController.Stage.ENDING||game.maps().busy()
                ||!owner.isAlive()||owner.isRemoved()||owner.isSpectator()||game.isObserver(owner)||!game.canUseGameplayItems(owner)
                ||game.awaitingRespawn(owner.getUUID())||!game.items().wands().validRoleTarget(owner,false)
                ||!LbcDimensionItems.ordinary(owner.level())||!LbcBedwarsRoleGuards.publication(owner.level())
                ||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner)||game.items().timeStop().frozen(owner)
                ||game.items().timeStop().stopped(owner.level())||game.items().wands().timeStopped()
                ||game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()||!policy.activation(owner))return false;
        callbacks++;try{
            owner.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,20,4,false,false,false));
            uses++;var ray=new Ray(++sequence,owner);
            game.items().exasperato().replayableSamples(owner,ray.origin,ray.direction,300,ray);return true;
        }finally{callbacks--;}
    }
    private final class Ray implements LbcExasperato.ReplayableRay {
        final long id;final UUID author;final ServerLevel level;final String epoch;final Vec3 origin,direction;
        final LbcIndirectKills.Owner cause;
        LbcExasperato.RayTicket ticket;
        long visited,spawned,removed;
        boolean cancelled;
        Ray(long id,ServerPlayer owner){this.id=id;author=owner.getUUID();level=owner.level();epoch=game.items().epoch();
            origin=owner.getEyePosition();direction=owner.getLookAngle().normalize();cause=game.items().indirectKills().wildCause(owner);}
        public void submitted(LbcExasperato.RayTicket ticket){this.ticket=ticket;pending.put(id,this);}
        public double sampleStep(){return 1;}
        public boolean block(BlockPos position,BlockState state){return !state.is(AIRS);}
        public boolean target(Entity entity){return false;}
        public void trail(Vec3 from,Vec3 to){}
        public void complete(HitResult hit){pending.remove(id,this);}
        private HitResult terminal(Vec3 point){return BlockHitResult.miss(point,Direction.UP,BlockPos.containing(point));}
        public HitResult sampleAt(Vec3 point){
            callbacks++;try{
                var owner=game.server().getPlayerList().getPlayer(author);var at=BlockPos.containing(point);
                if(cancelled||restoring||owner==null||owner.level()!=level||!owner.isAlive()||owner.isRemoved()
                        ||!game.items().currentEpoch(epoch)||!level.hasChunkAt(at)||!level.isInValidBounds(at)
                        ||!LbcBedwarsRoleGuards.publication(level)||!policy.sample(owner,level,point))return terminal(point);
                visited++;samples++;
                if(level.getBlockState(at).is(AIRS))return null;
                // This remains a native actor until its first native tick. In particular, do not
                // explode synchronously or stamp the epoch tag that identifies thrown dynamites.
                var charge=new PrimedTnt(level,point.x,point.y,point.z,owner);charge.setFuse(-1);charge.setDeltaMovement(Vec3.ZERO);charge.entityTags().add(TNT);
                var authority=new LbcSpellSource.Source(charge.getUUID(),author,epoch,cause,Set.of());
                game.items().spellSources().bind(charge,authority);
                try(var scope=game.items().spellSources().scope((LbcSpellSource.Source)null,level)){
                    if(level.addFreshEntity(charge)){spawned++;tnt++;}
                    else{charge.discard();game.items().spellSources().unloaded(charge);}
                }
                // Failure to destroy is terminal. A successful native removal is re-read before
                // advancing, so a wall may cause several TNT actors in one accepted source trace.
                if(owner.getRandom().nextFloat()<.7f&&destroy(owner,level,at,cause)){removed++;destroyed++;}
                return level.getBlockState(at).is(AIRS)?null:terminal(point);
            }finally{callbacks--;}
        }
        void cancel(){cancelled=true;pending.remove(id,this);if(ticket!=null)ticket.cancel();}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){
            // Author permits canceling short accepted continuations on rewind. Spawned native TNT
            // and its immutable source authority belong to the canonical body/spell checkpoints.
            return resolved->()->{cancelled=true;pending.remove(id,this);replayCancelled.add(this);};
        }
    }
    private boolean destroy(ServerPlayer owner,ServerLevel level,BlockPos at,LbcIndirectKills.Owner cause){
        if(!level.hasChunkAt(at)||!LbcBedwarsRoleGuards.publication(level))return false;
        var arena=LbcBedwarsArena.at(level);if(arena!=null&&!arena.allowsRemoval(level,at))return false;
        if(arena!=null)arena.captureUnchanged(level,at,Blocks.AIR.defaultBlockState());
        game.maps().noteMapBlockWrite(level,at);game.items().indirectKills().beforeBlockChange(level,at,Blocks.AIR.defaultBlockState());
        boolean[] changed={false};game.items().indirectKills().capturedWorldChanges(level,cause,()->{
            // destroyBlock uses the source command's empty-tool native loot/container semantics.
            changed[0]=level.destroyBlock(at,true,owner);
            if(changed[0]){
                if(level==game.maps().level())game.maps().recordBlockChange(at);
                game.items().indirectKills().supportRemoved(level,cause,at);
            }
        });return changed[0];
    }
    public record State(int active,long uses,long samples,long tnt,long destroyed){}
    public record View(long id,UUID author,Vec3 origin,Vec3 direction,long samples,long tnt,long destroyed){}
    public State state(){return new State(pending.size(),uses,samples,tnt,destroyed);}
    public List<View> views(){var result=new ArrayList<View>();for(var ray:pending.values())result.add(new View(ray.id,ray.author,ray.origin,ray.direction,ray.visited,ray.spawned,ray.removed));return List.copyOf(result);}
    public void checkpointBoundary(){if(callbacks!=0)throw new IllegalStateException("Finish Imba Hoe callbacks before checkpoint");}
    private void cancelPending(){for(var ray:List.copyOf(pending.values()))ray.cancel();pending.clear();}
    public void stop(){checkpointBoundary();cancelPending();for(var ray:List.copyOf(replayCancelled))ray.cancel();replayCancelled.clear();sequence=uses=samples=tnt=destroyed=0;restoring=false;}
    public void prepareFrozen(){checkpointBoundary();restoring=true;cancelPending();}
    public void publishFrozen(){checkpointBoundary();for(var ray:List.copyOf(replayCancelled))ray.cancel();replayCancelled.clear();restoring=false;}
    public void cancelFrozen(){stop();}
    MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){
        checkpointBoundary();long ids=sequence,accepted=uses,visited=samples,spawned=tnt,removed=destroyed;
        return resolved->{checkpointBoundary();return ()->{cancelPending();sequence=ids;uses=accepted;samples=visited;tnt=spawned;destroyed=removed;};};
    }
}
