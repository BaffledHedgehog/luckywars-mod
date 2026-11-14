package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Stationary Delta centers; all payload physics remain in their existing native owners. */
public final class LbcDeltaTnt {
    private static final String TAG="skywars2.delta_tnt";
    private static final int[][] OFFSETS={
        {0,0,0},{1,0,0},{2,0,0},{0,1,0},{0,2,0},{0,0,1},{0,0,2},
        {-1,0,0},{-2,0,0},{0,-1,0},{0,-2,0},{0,0,-1},{0,0,-2},
        {1,1,1},{1,-1,-1},{1,1,-1},{-1,-1,-1},{-1,-1,1},{-1,1,1},{-1,1,-1},{1,-1,1},
        {-1,-1,0},{-1,1,0},{-1,0,-1},{-1,0,1},{1,-1,0},{1,1,0},{1,0,-1},{1,0,1},
        {0,-1,1},{0,-1,-1},{0,1,1},{0,1,-1}};
    public interface Policy {
        boolean admission(ServerPlayer author,ServerLevel level,Vec3 origin);
        boolean continues(ServerLevel level,Vec3 origin);
        void exoperso(LbcSpellSource.Source source,ServerLevel level,Vec3 origin,Vec3 direction);
        default boolean globalFireballs(){return true;}
    }
    private final GameController game;private final Policy policy;
    private final Map<UUID,Actor> actors=new LinkedHashMap<>();
    private final Set<Activation> work=Collections.newSetFromMap(new IdentityHashMap<>());
    private int lastTick=Integer.MIN_VALUE,callbacks;private boolean restoring;
    private static final class Actor {
        Marker body;final ServerLevel level;final String epoch;final LbcIndirectKills.Owner cause;
        final UUID activation;final boolean root;long random,rolls,payloads;int age,lastPulse;Activation pending;
        Actor(Marker body,String epoch,LbcIndirectKills.Owner cause,UUID activation,boolean root,long random,int now){
            this.body=body;level=(ServerLevel)body.level();this.epoch=epoch;this.cause=cause;this.activation=activation;this.root=root;this.random=random;lastPulse=now;
        }
        LbcSpellSource.Source source(){return new LbcSpellSource.Source(body.getUUID(),cause.player(),epoch,cause,Set.of(),true);}
    }
    public LbcDeltaTnt(GameController game,Policy policy){this.game=game;this.policy=Objects.requireNonNull(policy);}
    public static boolean matches(ItemStack stack){var data=stack.get(DataComponents.ENTITY_DATA);return !stack.isEmpty()&&data!=null&&data.type()==EntityTypes.MARKER&&data.getUnsafe().getListOrEmpty("Tags").contains(StringTag.valueOf("delta_tnt"));}
    public static boolean managed(Entity body){return body instanceof Marker&&body.entityTags().contains(TAG);}
    private static boolean horizontal(BlockPos pos){return pos.getX()>=-30000000&&pos.getX()<30000000&&pos.getZ()>=-30000000&&pos.getZ()<30000000;}
    private boolean running(){return !restoring&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&!game.maps().busy()&&game.server().tickRateManager().runsNormally();}
    private boolean paused(ServerLevel level){return game.items().wands().timeStopped()||game.items().timeStop().stopped(level);}
    public boolean use(ServerPlayer author,InteractionHand hand,Vec3 origin){
        var level=author.level();var stack=author.getItemInHand(hand);var at=BlockPos.containing(origin);
        if(!running()||!matches(stack)||!author.isAlive()||author.isRemoved()||author.isSpectator()||game.isObserver(author)||!game.canUseGameplayItems(author)
                ||game.awaitingRespawn(author.getUUID())||!game.items().wands().validRoleTarget(author,false)||!LbcDimensionItems.ordinary(level)
                ||!LbcBedwarsRoleGuards.publication(level)||game.items().actionsBlocked(author)||game.items().wands().inputTimeStopped(author)
                ||paused(level)||game.items().timeStop().frozen(author)||!horizontal(at)||!level.hasChunkAt(at)||!policy.admission(author,level,origin))return false;
        var root=spawn(level,origin,game.items().epoch(),game.items().indirectKills().wildCause(author),UUID.randomUUID(),true,author.getRandom().nextLong());if(root==null)return false;
        // marker_2t was datapack polling, not a charge animation. Native input queues activation directly.
        root.pending=new Activation(root);work.add(root.pending);game.items().stonekivi().queuedWork(root.pending);
        stack.consume(1,author);author.inventoryMenu.broadcastChanges();return true;
    }
    private Actor spawn(ServerLevel level,Vec3 at,String epoch,LbcIndirectKills.Owner cause,UUID activation,boolean root,long seed){
        if(!horizontal(BlockPos.containing(at))||!level.hasChunkAt(BlockPos.containing(at)))return null;
        var body=new Marker(EntityTypes.MARKER,level);body.setPos(at);body.entityTags().addAll(Set.of(TAG,"mecha",root?"delta_tnt":"delta_ex",root?"marker_2t":"slowraycast"));
        if(!root)body.entityTags().add("imba_rc");
        var actor=new Actor(body,epoch,cause,activation,root,seed,game.server().getTickCount());actors.put(body.getUUID(),actor);
        game.items().markCurrentItemEpoch(body);game.items().spellSources().bind(body,actor.source());game.items().timeStop().prepareFrozen(body);
        if(!level.addFreshEntity(body)){actors.remove(body.getUUID());body.discard();return null;}return actor;
    }
    private static long mix(long z){z=(z^(z>>>30))*0xbf58476d1ce4e5b9L;z=(z^(z>>>27))*0x94d049bb133111ebL;return z^(z>>>31);}
    private long next(Actor a){a.random+=0x9e3779b97f4a7c15L;return mix(a.random);}
    public void tick(){
        int now=game.server().getTickCount();if(!running()||lastTick==now)return;lastTick=now;callbacks++;
        try{if(!actors.isEmpty())game.items().cloneSkills().beforeOwnedStep(actors.values().stream().map(a->a.body).toList());for(var a:List.copyOf(actors.values())){
            if(!game.items().currentEpoch(a.epoch)||a.body.level()!=a.level){retire(a);continue;}
            if(a.body.isRemoved()){if(a.body.getRemovalReason()!=Entity.RemovalReason.UNLOADED_TO_CHUNK)retire(a);continue;}
            if(paused(a.level)||game.items().timeStop().frozen(a.body)||!a.level.hasChunkAt(a.body.blockPosition()))continue;
            if(!horizontal(a.body.blockPosition())||!LbcBedwarsRoleGuards.publication(a.level)||!policy.continues(a.level,a.body.position())){retire(a);continue;}
            if(a.lastPulse==now||a.pending!=null)continue;a.lastPulse=now;
            if(a.root){a.pending=new Activation(a);work.add(a.pending);game.items().stonekivi().queuedWork(a.pending);continue;}
            a.age++;
            for(int branch=0;branch<20;branch++){
                a.rolls++;if((next(a)>>>11)*0x1.0p-53>=.01)continue;
                if(branch<19){var child=game.items().specialFireballs().spawnDelta(a.source(),a.level,a.body.position(),branch);if(child!=null){child.entityTags().add("skywars2.delta_activation."+a.activation);a.payloads++;}}
                else {
                    var candidates=actors.values().stream().filter(other->!other.root&&!other.body.isRemoved()&&other.level==a.level&&other.body.distanceToSqr(a.body)<=100).toList();
                    if(!candidates.isEmpty()){
                        var target=candidates.get((int)Long.remainderUnsigned(next(a),candidates.size()));
                        // CommandSourceStack.facing uses yaw -90 for atan2(0,0): self faces +X.
                        var delta=target.body.position().subtract(a.body.position());var direction=delta.lengthSqr()<1.0e-12?new Vec3(1,0,0):delta.normalize();
                        policy.exoperso(a.source(),a.level,a.body.position(),direction);a.payloads++;
                    }
                }
            }
            if(a.age==1000){steer(a);retire(a);}
        }}finally{callbacks--;}
    }
    private void steer(Actor a){
        for(var entity:a.level.getAllEntities())if(entity instanceof LargeFireball&&!entity.isRemoved()){
            if(!policy.globalFireballs()&&!entity.entityTags().contains("skywars2.delta_activation."+a.activation))continue;
            var vector=a.body.position().subtract(entity.position());var direction=vector.lengthSqr()<1.0e-12?new Vec3(1,0,0):vector.normalize();
            entity.setDeltaMovement(new Vec3(Math.floor(direction.x*10000)*.0004,Math.floor(direction.y*10000)*.0004,Math.floor(direction.z*10000)*.0004));
        }
    }
    private void retire(Actor a){actors.remove(a.body.getUUID());if(a.pending!=null)a.pending.cancel();a.body.discard();}
    private final class Activation implements LbcStonekivi.Work {
        final Actor actor;final Vec3 origin;int cursor;boolean closed;
        Activation(Actor actor){this.actor=actor;origin=actor.body.position().add(0,.2,0);}
        public boolean ready(){return closed||running()&&!actor.body.isRemoved()&&!paused(actor.level)&&!game.items().timeStop().frozen(actor.body)&&actor.level.hasChunkAt(actor.body.blockPosition());}
        public boolean snapshotIdle(){return true;}
        public boolean step(){
            if(closed)return true;if(!game.items().currentEpoch(actor.epoch)||actors.get(actor.body.getUUID())!=actor){cancel();return true;}
            if(!ready())return false;if(actor.body.isRemoved()||!policy.continues(actor.level,origin)){retire(actor);return true;}
            callbacks++;try{
                while(cursor<OFFSETS.length){var o=OFFSETS[cursor];var pos=BlockPos.containing(origin.add(o[0],o[1],o[2]));
                    if(horizontal(pos)&&!actor.level.hasChunkAt(pos)){game.maps().prepareChunk(actor.level,new ChunkPos(pos.getX()>>4,pos.getZ()>>4));return false;}cursor++;
                }
                game.items().stonekivi().beginNativeWork();try{
                    var pos=BlockPos.containing(origin);var oldState=actor.level.getBlockState(pos);
                    if(game.items().writeOwnedTerrain(actor.level,pos,Blocks.REDSTONE_BLOCK.defaultBlockState(),actor.cause))
                        // Delta uses ordinary setblock; notify adjacent TNT with the original creator.
                        game.items().indirectKills().capturedWorldChanges(actor.level,actor.cause,()->actor.level.updateNeighboursOnBlockSet(pos,oldState));
                }finally{game.items().stonekivi().endNativeWork();}
                for(var o:OFFSETS)spawn(actor.level,origin.add(o[0],o[1],o[2]),actor.epoch,actor.cause,actor.activation,false,next(actor));
                for(float pitch:new float[]{1,0,2,1.5f,.5f,.2f})LbcOverload.sound(actor.level,origin,"minecraft:entity.tnt.primed",net.minecraft.sounds.SoundSource.MASTER,1,pitch,0,.5f);
                close();retire(actor);return true;
            }finally{callbacks--;}
        }
        private void close(){closed=true;work.remove(this);if(actor.pending==this)actor.pending=null;}
        public void cancel(){close();}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){int saved=cursor;boolean ended=closed;return links->{MatchAuthorityCheckpoint.world(game,actor.level);return ()->{cursor=saved;closed=ended;};};}
    }
    public int count(){return actors.size();}
    public record ActorView(Marker body,boolean root,int age,long rolls,long payloads,long rngState,LbcIndirectKills.Owner cause){}
    public List<ActorView> actorViews(int limit){return actors.values().stream().limit(Math.max(0,limit)).map(a->new ActorView(a.body,a.root,a.age,a.rolls,a.payloads,a.random,a.cause)).toList();}
    public void loaded(Entity body){if(restoring||MatchActorCheckpoint.restoring(body)||!managed(body))return;var a=actors.get(body.getUUID());if(a==null||!game.items().currentEpoch(a.epoch)){body.discard();return;}a.body=(Marker)body;game.items().timeStop().prepareFrozen(body);}
    public void unloaded(Entity body){}
    static boolean cloneRecognizes(Entity body){return managed(body);}
    LbcCloneSkills.ActorImage cloneImage(Entity source){
        var a=actors.get(source.getUUID());if(a==null)return null;int age=a.age;long random=a.random,rolls=a.rolls,payloads=a.payloads;boolean pending=a.pending!=null;
        return new LbcCloneSkills.ActorImage(){public void install(Entity ignored,Entity clone,Map<Entity,Entity> relatives){var copy=new Actor((Marker)clone,a.epoch,a.cause,a.activation,a.root,random,game.server().getTickCount());copy.age=age;copy.rolls=rolls;copy.payloads=payloads;actors.put(clone.getUUID(),copy);game.items().spellSources().bind(clone,copy.source());
            if(pending){copy.pending=new Activation(copy);work.add(copy.pending);game.items().stonekivi().queuedWork(copy.pending);}
        }public void rollback(Entity clone){var copy=actors.get(clone.getUUID());if(copy!=null)retire(copy);}};
    }
    public void checkpointBoundary(){if(callbacks!=0)throw new IllegalStateException("Finish Delta TNT callbacks before checkpoint");}
    public void stop(){checkpointBoundary();for(var job:List.copyOf(work))job.cancel();var old=List.copyOf(actors.values());actors.clear();old.forEach(a->a.body.discard());lastTick=Integer.MIN_VALUE;restoring=false;}
    public void prepareFrozen(){checkpointBoundary();restoring=true;actors.clear();work.clear();}
    public void publishFrozen(){checkpointBoundary();restoring=false;lastTick=game.server().getTickCount();}
    public void cancelFrozen(){stop();}
    MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
        checkpointBoundary();record Saved(Actor actor,MatchDamageSources.Ref body,Marker dormant,int age,long random,long rolls,long payloads,Activation pending){}
        var saved=actors.values().stream().map(a->new Saved(a,a.body.isRemoved()&&a.body.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK?null:graph.links.key(a.body),a.body,a.age,a.random,a.rolls,a.payloads,a.pending)).toList();var pending=List.copyOf(work);pending.forEach(graph::capture);
        return links->{var bodies=new IdentityHashMap<Actor,Marker>();for(var image:saved){MatchAuthorityCheckpoint.world(game,image.actor.level);bodies.put(image.actor,image.body==null?image.dormant:(Marker)links.get(image.body));}
            return ()->{actors.clear();for(var image:saved){var a=image.actor;a.body=bodies.get(a);a.age=image.age;a.random=image.random;a.rolls=image.rolls;a.payloads=image.payloads;a.pending=image.pending;a.lastPulse=game.server().getTickCount();actors.put(a.body.getUUID(),a);}work.clear();work.addAll(pending);};};
    }
}
