package me.skywars2.game;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Native source centers; terrain is advanced by the existing fair Stonekivi queue. */
public final class LbcDiamondifier {
    private static final String ACTOR="skywars2.diamondifier";
    private static final TagKey<Block> REPLACE=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:tannerkivi"));
    private static final BlockParticleOption DUST=new BlockParticleOption(ParticleTypes.BLOCK,Blocks.DIAMOND_BLOCK.defaultBlockState());
    public interface Policy {
        boolean admission(ServerPlayer author,ServerLevel level,Vec3 origin);
        /** False retires the source center without a refund. */
        boolean tick(ServerLevel level,Vec3 origin);
        /** Owns permanent Chef removal and whole-family deletion; false means protected. */
        boolean deletion(Entity target,LbcIndirectKills.Owner cause);
    }
    private final GameController game;
    private final Supplier<ItemStack> egg;
    private final Policy policy;
    private final Map<UUID,Actor> actors=new LinkedHashMap<>();
    private final Set<Fill> fills=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<ServerLevel,Integer> phases=new IdentityHashMap<>();
    private int lastTick=Integer.MIN_VALUE,callbacks;
    private boolean restoring;
    private static final class Actor {
        Marker body;final ServerLevel level;final String epoch;final LbcIndirectKills.Owner cause;
        int lifetime;boolean active;long pulses,consumed,writes;
        Actor(Marker body,String epoch,LbcIndirectKills.Owner cause){this.body=body;this.level=(ServerLevel)body.level();this.epoch=epoch;this.cause=cause;}
    }
    public LbcDiamondifier(GameController game,Supplier<ItemStack> egg,Policy policy){this.game=game;this.egg=egg;this.policy=policy;}
    public static boolean matches(ItemStack stack){
        if(stack.isEmpty())return false;var data=stack.get(DataComponents.ENTITY_DATA);
        return data!=null&&data.type()==EntityTypes.MARKER&&data.getUnsafe().getListOrEmpty("Tags").contains(StringTag.valueOf("diamondificator"));
    }
    public static boolean managed(Entity body){return body instanceof Marker&&body.entityTags().contains(ACTOR);}
    private boolean paused(ServerLevel level){return game.items().wands().timeStopped()||game.items().timeStop().stopped(level);}
    public boolean use(ServerPlayer author,InteractionHand hand,Vec3 origin){
        var level=author.level();var stack=author.getItemInHand(hand);var pos=BlockPos.containing(origin);
        if(restoring||!matches(stack)||!game.playing()||game.stage()==GameController.Stage.ENDING||game.maps().busy()
                ||!author.isAlive()||author.isRemoved()||author.isSpectator()||game.isObserver(author)||!game.canUseGameplayItems(author)
                ||game.awaitingRespawn(author.getUUID())||!game.items().wands().validRoleTarget(author,false)
                ||!LbcDimensionItems.ordinary(level)||!LbcBedwarsRoleGuards.publication(level)
                ||game.items().actionsBlocked(author)||game.items().wands().inputTimeStopped(author)||paused(level)
                ||game.items().timeStop().frozen(author)||game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()
                ||!level.hasChunkAt(pos)||!level.isInValidBounds(pos)||!policy.admission(author,level,origin))return false;
        if(spawn(level,origin,game.items().epoch(),game.items().indirectKills().wildCause(author))==null)return false;
        stack.consume(1,author);author.inventoryMenu.broadcastChanges();return true;
    }
    private Actor spawn(ServerLevel level,Vec3 origin,String epoch,LbcIndirectKills.Owner cause){
        var marker=new Marker(EntityTypes.MARKER,level);marker.setPos(origin);
        marker.entityTags().addAll(Set.of(ACTOR,"diamondificator","magic","rc_1sec"));
        var actor=new Actor(marker,epoch,cause);actors.put(marker.getUUID(),actor);
        game.items().markCurrentItemEpoch(marker);
        game.items().spellSources().bind(marker,new LbcSpellSource.Source(marker.getUUID(),cause.player(),epoch,cause,Set.of(),true));
        game.items().timeStop().prepareFrozen(marker);
        if(!level.addFreshEntity(marker)){actors.remove(marker.getUUID());marker.discard();return null;}
        phases.putIfAbsent(level,Math.floorMod(game.server().getTickCount(),20));return actor;
    }
    public void tick(){
        int now=game.server().getTickCount();
        if(restoring||lastTick==now||!game.playing()||game.maps().busy()||game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally())return;
        lastTick=now;callbacks++;
        try{
            if(!actors.isEmpty())game.items().cloneSkills().beforeOwnedStep(actors.values().stream().map(a->a.body).toList());
            var pulse=Collections.newSetFromMap(new IdentityHashMap<ServerLevel,Boolean>());
            for(var level:List.copyOf(phases.keySet()))if(!paused(level)){int phase=(phases.get(level)+1)%20;phases.put(level,phase);if(phase==0)pulse.add(level);}
            // New children are selected on the following global second, as in the source selector.
            for(var actor:List.copyOf(actors.values())){
                if(!game.items().currentEpoch(actor.epoch)||actor.body.level()!=actor.level){retire(actor);continue;}
                if(actor.body.isRemoved()){if(actor.body.getRemovalReason()!=Entity.RemovalReason.UNLOADED_TO_CHUNK)actors.remove(actor.body.getUUID());continue;}
                if(paused(actor.level)||game.items().timeStop().frozen(actor.body)||!actor.level.hasChunkAt(actor.body.blockPosition()))continue;
                var origin=Vec3.atCenterOf(actor.body.blockPosition());
                if(!LbcBedwarsRoleGuards.publication(actor.level)||!policy.tick(actor.level,origin)){retire(actor);continue;}
                if(!pulse.contains(actor.level))continue;
                actor.lifetime+=20;pulse(actor,origin);
            }
        }finally{callbacks--;}
    }
    private void pulse(Actor actor,Vec3 origin){
        var pos=BlockPos.containing(origin);
        if(!actor.active){
            if(!game.items().writeOwnedTerrain(actor.level,pos,Blocks.DIAMOND_BLOCK.defaultBlockState(),actor.cause))return;
            actor.active=true;actor.writes++;
        }
        // raycasters_1s increments Lifetime by 20 BEFORE dispatch: only Lifetime=20 fills.
        if(actor.lifetime>=15&&actor.lifetime<=25){var fill=new Fill(actor,pos);fills.add(fill);game.items().stonekivi().queuedWork(fill);}
        actor.pulses++;actor.level.sendParticles(DUST,origin.x,origin.y,origin.z,30,1,1,1,0);
        for(var entity:actor.level.getEntities(actor.body,new AABB(origin,origin).inflate(15),e->candidate(e,origin))){
            if(entity.isRemoved())continue;var targetPosition=entity.position();var child=Vec3.atCenterOf(entity.blockPosition());
            // Admission belongs to deletion: protected family parts must never seed a child.
            if(!policy.deletion(entity,actor.cause))continue;
            actor.consumed++;
            boolean nearby=false;for(var other:actors.values())if(other.level==actor.level&&!other.body.isRemoved()&&other.body.position().distanceToSqr(targetPosition)<=9){nearby=true;break;}
            if(!nearby)spawn(actor.level,child,actor.epoch,actor.cause);
        }
        if(!actor.level.getBlockState(pos).is(Blocks.DIAMOND_BLOCK)){
            var stack=egg.get();if(!stack.isEmpty()){
                var item=new ItemEntity(actor.level,origin.x,origin.y,origin.z,stack.copy());
                game.items().markCurrentItemEpoch(item);game.items().spellSources().bind(item,new LbcSpellSource.Source(actor.body.getUUID(),actor.cause.player(),actor.epoch,actor.cause,Set.of(),true));
                actor.level.addFreshEntity(item);
            }
            retire(actor);
        }
    }
    private boolean candidate(Entity entity,Vec3 origin){
        if(entity instanceof Player||entity.isRemoved()||entity.position().distanceToSqr(origin)>225||entity.entityTags().contains("diamondificator")
                ||!game.items().wands().validRoleTarget(entity,false)||game.items().timeStop().frozen(entity))return false;
        if(entity instanceof ItemEntity item){var stack=item.getItem();if(stack.is(Items.DIAMOND)||stack.is(Items.DIAMOND_BLOCK)||matches(stack))return false;}
        return true;
    }
    private void retire(Actor actor){actors.remove(actor.body.getUUID());actor.body.discard();}
    private final class Fill implements LbcStonekivi.Work {
        final Actor actor;final BlockPos min;int cursor;boolean closed;
        Fill(Actor actor,BlockPos center){this.actor=actor;min=center.offset(-15,-15,-15);}
        public boolean ready(){return closed||!restoring&&!paused(actor.level)&&(actor.body.isRemoved()||!game.items().timeStop().frozen(actor.body));}
        public boolean snapshotIdle(){return true;}
        public boolean step(){
            if(closed)return true;if(!game.items().currentEpoch(actor.epoch)){cancel();return true;}
            if(!ready()||game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally())return false;
            game.items().stonekivi().beginNativeWork();try{
                long deadline=Math.min(EffectTerrain.deadlineNanos(),System.nanoTime()+350_000);
                for(int quantum=0;quantum<128&&cursor<29791&&System.nanoTime()<deadline;quantum++){
                    var pos=min.offset(cursor%31,cursor/(31*31),cursor/31%31);
                    // Accepted finite fills must complete; reuse the existing asynchronous chunk preparation.
                    if(actor.level.isInValidBounds(pos)&&!actor.level.hasChunkAt(pos)){
                        game.maps().prepareChunk(actor.level,new net.minecraft.world.level.ChunkPos(pos.getX()>>4,pos.getZ()>>4));return false;
                    }
                    cursor++;if(actor.level.isInValidBounds(pos)&&actor.level.getBlockState(pos).is(REPLACE)
                            &&game.items().writeOwnedTerrain(actor.level,pos,Blocks.DIAMOND_BLOCK.defaultBlockState(),actor.cause))actor.writes++;
                }
                if(cursor>=29791){closed=true;fills.remove(this);}return closed;
            }finally{game.items().stonekivi().endNativeWork();}
        }
        public void cancel(){closed=true;fills.remove(this);}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            int saved=cursor;boolean ended=closed;return links->{MatchAuthorityCheckpoint.world(game,actor.level);return ()->{cursor=saved;closed=ended;};};
        }
    }
    public int count(){return actors.size();}
    public int pendingFills(){return fills.size();}
    public record ActorView(Marker body,int lifetime,boolean active,long pulses,long consumed,long writes,LbcIndirectKills.Owner cause){}
    public List<ActorView> actorViews(int limit){return actors.values().stream().limit(Math.max(0,limit)).map(a->new ActorView(a.body,a.lifetime,a.active,a.pulses,a.consumed,a.writes,a.cause)).toList();}
    public void loaded(Entity body){
        if(restoring||MatchActorCheckpoint.restoring(body)||!managed(body))return;var actor=actors.get(body.getUUID());
        if(actor==null||!game.items().currentEpoch(actor.epoch)){body.discard();return;}actor.body=(Marker)body;game.items().timeStop().prepareFrozen(body);
    }
    public void unloaded(Entity body){/* Accepted scalar state remains until native body rebind. */}
    static boolean cloneRecognizes(Entity body){return managed(body);}
    LbcCloneSkills.ActorImage cloneImage(Entity source){
        var original=actors.get(source.getUUID());if(original==null)return null;
        int age=original.lifetime;boolean active=original.active;long pulses=original.pulses,consumed=original.consumed,writes=original.writes;
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity ignored,Entity clone,Map<Entity,Entity> family){
                var actor=new Actor((Marker)clone,original.epoch,original.cause);actor.lifetime=age;actor.active=active;actor.pulses=pulses;actor.consumed=consumed;actor.writes=writes;actors.put(clone.getUUID(),actor);
            }
            public void rollback(Entity clone){var actor=actors.remove(clone.getUUID());if(actor!=null)clone.discard();}
        };
    }
    public void checkpointBoundary(){if(callbacks!=0)throw new IllegalStateException("Finish Diamondifier callbacks before checkpoint");}
    public void stop(){checkpointBoundary();for(var fill:List.copyOf(fills))fill.cancel();var old=List.copyOf(actors.values());actors.clear();old.forEach(a->a.body.discard());phases.clear();lastTick=Integer.MIN_VALUE;restoring=false;}
    public void prepareFrozen(){checkpointBoundary();restoring=true;actors.clear();fills.clear();phases.clear();}
    public void publishFrozen(){checkpointBoundary();restoring=false;lastTick=game.server().getTickCount();}
    public void cancelFrozen(){stop();}
    MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
        checkpointBoundary();record Saved(Actor actor,MatchDamageSources.Ref body,int lifetime,boolean active,long pulses,long consumed,long writes){}
        var all=Collections.newSetFromMap(new IdentityHashMap<Actor,Boolean>());all.addAll(actors.values());for(var fill:fills)all.add(fill.actor);
        var saved=all.stream().map(a->new Saved(a,actors.containsKey(a.body.getUUID())?graph.links.key(a.body):null,a.lifetime,a.active,a.pulses,a.consumed,a.writes)).toList();
        var live=Set.copyOf(actors.keySet());var pending=List.copyOf(fills);pending.forEach(graph::capture);var clocks=new IdentityHashMap<>(phases);
        return links->{var bodies=new IdentityHashMap<Actor,Marker>();for(var image:saved){MatchAuthorityCheckpoint.world(game,image.actor.level);if(image.body!=null)bodies.put(image.actor,(Marker)links.get(image.body));}
            return ()->{actors.clear();for(var image:saved){var a=image.actor;if(image.body!=null)a.body=bodies.get(a);a.lifetime=image.lifetime;a.active=image.active;a.pulses=image.pulses;a.consumed=image.consumed;a.writes=image.writes;if(image.body!=null&&live.contains(a.body.getUUID()))actors.put(a.body.getUUID(),a);}fills.clear();fills.addAll(pending);phases.clear();phases.putAll(clocks);};};
    }
}
