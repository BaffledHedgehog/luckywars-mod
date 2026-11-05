package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Source Funny TNT markers. Accepted strict fills share the Stonekivi terrain queue. */
public final class LbcFunnyTnt {
    private static final String ACTOR="skywars2.funny_tnt";
    private static final int[][] CHILDREN={{0,3,0},{0,-3,0},{0,0,3},{0,0,-3},{3,0,0},{-3,0,0}};
    public interface Policy {
        boolean admission(ServerPlayer author,ServerLevel level,Vec3 origin);
        boolean continues(ServerLevel level,Vec3 origin);
        /** Source default: a successful chance roll kills all family markers in this dimension. */
        default boolean globalStop(){return true;}
    }
    private final GameController game;
    private final Policy policy;
    private final Map<UUID,Actor> actors=new LinkedHashMap<>();
    // Populated by entity lifecycle hooks, including foreign source markers; TNT entities never enter.
    private final Map<UUID,Marker> family=new LinkedHashMap<>();
    private final Set<Fill> fills=Collections.newSetFromMap(new IdentityHashMap<>());
    private int lastTick=Integer.MIN_VALUE,callbacks;
    private boolean restoring;
    private static final class Actor {
        Marker body;final ServerLevel level;final String epoch;final LbcIndirectKills.Owner cause;
        long age,pulses,writes,random;int lastPulse=Integer.MIN_VALUE;Fill pending;
        Actor(Marker body,String epoch,LbcIndirectKills.Owner cause,long random){this.body=body;level=(ServerLevel)body.level();this.epoch=epoch;this.cause=cause;this.random=random;}
    }
    public LbcFunnyTnt(GameController game,Policy policy){this.game=game;this.policy=policy;}
    public static boolean matches(ItemStack stack){
        if(stack.isEmpty())return false;var data=stack.get(DataComponents.ENTITY_DATA);
        return data!=null&&data.type()==EntityTypes.MARKER&&data.getUnsafe().getListOrEmpty("Tags").contains(StringTag.valueOf("funny_tnt"));
    }
    public static boolean managed(Entity body){return body instanceof Marker&&body.entityTags().contains(ACTOR);}
    private static boolean sourceMarker(Entity body){return body instanceof Marker&&body.entityTags().contains("funny_tnt");}
    private boolean paused(ServerLevel level){return game.items().wands().timeStopped()||game.items().timeStop().stopped(level);}
    private boolean running(){return !restoring&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&!game.maps().busy()
            &&!game.server().tickRateManager().isFrozen()&&game.server().tickRateManager().runsNormally();}
    public boolean use(ServerPlayer author,InteractionHand hand,Vec3 origin){
        var level=author.level();var stack=author.getItemInHand(hand);var pos=BlockPos.containing(origin);
        if(!running()||!matches(stack)||!author.isAlive()||author.isRemoved()||author.isSpectator()||game.isObserver(author)||!game.canUseGameplayItems(author)
                ||game.awaitingRespawn(author.getUUID())||!game.items().wands().validRoleTarget(author,false)
                ||!LbcDimensionItems.ordinary(level)||!LbcBedwarsRoleGuards.publication(level)||game.items().actionsBlocked(author)
                ||game.items().wands().inputTimeStopped(author)||paused(level)||game.items().timeStop().frozen(author)
                ||!level.hasChunkAt(pos)||!level.isInValidBounds(pos)||!policy.admission(author,level,origin))return false;
        if(spawn(level,origin,game.items().epoch(),game.items().indirectKills().wildCause(author),author.getRandom().nextLong())==null)return false;
        stack.consume(1,author);author.inventoryMenu.broadcastChanges();return true;
    }
    private Actor spawn(ServerLevel level,Vec3 origin,String epoch,LbcIndirectKills.Owner cause,long seed){
        if(!level.isInValidBounds(BlockPos.containing(origin)))return null;
        var marker=new Marker(EntityTypes.MARKER,level);marker.setPos(origin);marker.entityTags().addAll(Set.of(ACTOR,"slowraycast","funny_tnt","instant"));
        var actor=new Actor(marker,epoch,cause,seed);actors.put(marker.getUUID(),actor);family.put(marker.getUUID(),marker);
        game.items().markCurrentItemEpoch(marker);
        game.items().spellSources().bind(marker,new LbcSpellSource.Source(marker.getUUID(),cause.player(),epoch,cause,Set.of(),true));
        game.items().timeStop().prepareFrozen(marker);
        if(!level.addFreshEntity(marker)){actors.remove(marker.getUUID());family.remove(marker.getUUID());marker.discard();return null;}
        // A child created while draining a fill cannot join another selector pass this tick.
        actor.lastPulse=game.server().getTickCount();return actor;
    }
    public void tick(){
        int now=game.server().getTickCount();if(!running()||lastTick==now)return;lastTick=now;callbacks++;
        try{for(var actor:List.copyOf(actors.values())){
            if(!game.items().currentEpoch(actor.epoch)||actor.body.level()!=actor.level){retire(actor);continue;}
            if(actor.body.isRemoved()){if(actor.body.getRemovalReason()!=Entity.RemovalReason.UNLOADED_TO_CHUNK)retire(actor);continue;}
            if(paused(actor.level)||game.items().timeStop().frozen(actor.body)||!actor.level.hasChunkAt(actor.body.blockPosition()))continue;
            var origin=actor.body.position();
            var center=actor.body.blockPosition();
            if(!actor.level.isInValidBounds(center.offset(-1,-1,-1))||!actor.level.isInValidBounds(center.offset(1,1,1))
                    ||!LbcBedwarsRoleGuards.publication(actor.level)||!policy.continues(actor.level,origin)){retire(actor);continue;}
            if(actor.pending!=null||actor.lastPulse==now)continue;
            actor.lastPulse=now;actor.age++;
            var fill=new Fill(actor,origin);actor.pending=fill;fills.add(fill);game.items().stonekivi().queuedWork(fill);
        }}finally{callbacks--;}
    }
    private static long mix(long z){z=(z^(z>>>30))*0xbf58476d1ce4e5b9L;z=(z^(z>>>27))*0x94d049bb133111ebL;return z^(z>>>31);}
    private long next(Actor actor){actor.random+=0x9e3779b97f4a7c15L;return mix(actor.random);}
    private void finish(Actor actor,Vec3 origin){
        actor.pulses++;
        for(var offset:CHILDREN){var child=origin.add(offset[0],offset[1],offset[2]);spawn(actor.level,child,actor.epoch,actor.cause,next(actor));}
        var nearby=new ArrayList<Marker>();
        for(var body:family.values())if(!body.isRemoved()&&body.level()==actor.level&&body.position().distanceToSqr(origin)<=1)nearby.add(body);
        if(nearby.size()>=2)for(var body:nearby)remove(body);
        // The roll still executes after the duplicate command removed the executing marker.
        if((next(actor)>>>11)*0x1.0p-53<.1){
            if(policy.globalStop()){
                for(var body:List.copyOf(family.values()))if(body.level()==actor.level&&!body.isRemoved())remove(body);
            }else retire(actor);
        }
    }
    private void remove(Marker body){var actor=actors.get(body.getUUID());if(actor!=null)retire(actor);else{family.remove(body.getUUID());body.discard();}}
    private void retire(Actor actor){actors.remove(actor.body.getUUID());family.remove(actor.body.getUUID());if(actor.pending!=null)actor.pending.cancel();actor.body.discard();}
    private final class Fill implements LbcStonekivi.Work {
        final Actor actor;final Vec3 origin;final BlockPos min;int cursor;boolean closed;
        Fill(Actor actor,Vec3 origin){this.actor=actor;this.origin=origin;min=BlockPos.containing(origin).offset(-1,-1,-1);}
        public boolean ready(){return closed||running()&&!actor.body.isRemoved()&&!paused(actor.level)&&!game.items().timeStop().frozen(actor.body)&&actor.level.hasChunkAt(actor.body.blockPosition());}
        public boolean snapshotIdle(){return true;}
        public boolean step(){
            if(closed)return true;
            if(!game.items().currentEpoch(actor.epoch)||actors.get(actor.body.getUUID())!=actor){cancel();return true;}
            if(!ready())return false;
            if(actor.body.isRemoved()||!policy.continues(actor.level,actor.body.position())){retire(actor);return true;}
            callbacks++;game.items().stonekivi().beginNativeWork();try{
                long deadline=Math.min(EffectTerrain.deadlineNanos(),System.nanoTime()+350_000);
                while(cursor<27&&System.nanoTime()<deadline){
                    var pos=min.offset(cursor%3,cursor/9,cursor/3%3);
                    if(actor.level.isInValidBounds(pos)&&!actor.level.hasChunkAt(pos)){game.maps().prepareChunk(actor.level,new ChunkPos(pos.getX()>>4,pos.getZ()>>4));return false;}
                    cursor++;if(actor.level.isInValidBounds(pos)&&game.items().writeOwnedTerrain(actor.level,pos,Blocks.TNT.defaultBlockState(),actor.cause,true))actor.writes++;
                }
                if(cursor==27){
                    for(var offset:CHILDREN){var child=BlockPos.containing(origin.add(offset[0],offset[1],offset[2]));
                        if(actor.level.isInValidBounds(child)&&!actor.level.hasChunkAt(child)){game.maps().prepareChunk(actor.level,new ChunkPos(child.getX()>>4,child.getZ()>>4));return false;}
                    }
                    close();finish(actor,origin);
                }return closed;
            }finally{game.items().stonekivi().endNativeWork();callbacks--;}
        }
        private void close(){closed=true;fills.remove(this);if(actor.pending==this)actor.pending=null;}
        public void cancel(){close();}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){int saved=cursor;boolean ended=closed;return links->{MatchAuthorityCheckpoint.world(game,actor.level);return ()->{cursor=saved;closed=ended;};};}
    }
    public int count(){return actors.size();}
    public int pendingFills(){return fills.size();}
    public record ActorView(Marker body,long age,long pulses,long writes,long rngState,int cursor,LbcIndirectKills.Owner cause){}
    public List<ActorView> actorViews(int limit){return actors.values().stream().limit(Math.max(0,limit)).map(a->new ActorView(a.body,a.age,a.pulses,a.writes,a.random,a.pending==null?-1:a.pending.cursor,a.cause)).toList();}
    public void loaded(Entity body){
        if(restoring||MatchActorCheckpoint.restoring(body)||!sourceMarker(body))return;
        if(!managed(body)){family.put(body.getUUID(),(Marker)body);return;}
        var actor=actors.get(body.getUUID());if(actor==null||!game.items().currentEpoch(actor.epoch)){body.discard();return;}
        actor.body=(Marker)body;family.put(body.getUUID(),(Marker)body);game.items().timeStop().prepareFrozen(body);
    }
    public void unloaded(Entity body){if(sourceMarker(body))family.remove(body.getUUID());}
    static boolean cloneRecognizes(Entity body){return managed(body);}
    LbcCloneSkills.ActorImage cloneImage(Entity source){
        var original=actors.get(source.getUUID());if(original==null)return null;
        long age=original.age,pulses=original.pulses,writes=original.writes,random=original.random;
        Vec3 sourcePose=source.position(),fillOrigin=original.pending==null?null:original.pending.origin;
        int cursor=original.pending==null?0:original.pending.cursor;
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity ignored,Entity clone,Map<Entity,Entity> relatives){
                var a=new Actor((Marker)clone,original.epoch,original.cause,random);a.age=age;a.pulses=pulses;a.writes=writes;a.lastPulse=game.server().getTickCount();actors.put(clone.getUUID(),a);family.put(clone.getUUID(),a.body);
                if(fillOrigin!=null){var fill=new Fill(a,fillOrigin.add(clone.position().subtract(sourcePose)));fill.cursor=cursor;a.pending=fill;fills.add(fill);game.items().stonekivi().queuedWork(fill);}
            }
            public void rollback(Entity clone){var actor=actors.get(clone.getUUID());if(actor!=null)retire(actor);}
        };
    }
    public void checkpointBoundary(){if(callbacks!=0)throw new IllegalStateException("Finish Funny TNT callbacks before checkpoint");}
    public void stop(){checkpointBoundary();for(var fill:List.copyOf(fills))fill.cancel();var old=List.copyOf(actors.values());actors.clear();old.forEach(a->a.body.discard());family.clear();lastTick=Integer.MIN_VALUE;restoring=false;}
    public void prepareFrozen(){checkpointBoundary();restoring=true;actors.clear();family.clear();fills.clear();}
    public void publishFrozen(){checkpointBoundary();restoring=false;lastTick=game.server().getTickCount();}
    public void cancelFrozen(){stop();}
    MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
        checkpointBoundary();record Saved(Actor actor,MatchDamageSources.Ref body,Marker inactiveBody,long age,long pulses,long writes,long random,Fill pending){}
        // Region-owned unloaded markers have no captured native body to resolve. Keep their
        // inactive identity and scalar continuation; the normal UUID load hook rebinds them.
        var saved=actors.values().stream().map(a->new Saved(a,
                a.body.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK?null:graph.links.key(a.body),
                a.body,a.age,a.pulses,a.writes,a.random,a.pending)).toList();
        var foreign=new LinkedHashMap<UUID,MatchDamageSources.Ref>();for(var body:family.values())if(!body.isRemoved()&&!actors.containsKey(body.getUUID()))foreign.put(body.getUUID(),graph.links.key(body));
        var pending=List.copyOf(fills);pending.forEach(graph::capture);
        return links->{var bodies=new IdentityHashMap<Actor,Marker>();for(var image:saved){MatchAuthorityCheckpoint.world(game,image.actor.level);bodies.put(image.actor,image.body==null?image.inactiveBody:(Marker)links.get(image.body));}
            var others=new LinkedHashMap<UUID,Marker>();foreign.forEach((id,ref)->others.put(id,(Marker)links.get(ref)));
            return ()->{actors.clear();family.clear();family.putAll(others);for(var image:saved){var a=image.actor;a.body=bodies.get(a);a.age=image.age;a.pulses=image.pulses;a.writes=image.writes;a.random=image.random;a.pending=image.pending;a.lastPulse=game.server().getTickCount();actors.put(a.body.getUUID(),a);if(image.body!=null&&!a.body.isRemoved())family.put(a.body.getUUID(),a.body);}fills.clear();fills.addAll(pending);};};
    }
}
