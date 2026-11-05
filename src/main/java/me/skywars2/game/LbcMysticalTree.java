package me.skywars2.game;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Source six-axis random walk; accepted segments and the terminal crown share finite terrain work. */
public final class LbcMysticalTree {
    private static final String TAG="skywars2.mystical_tree";
    private static final Direction[] DIRECTIONS={Direction.WEST,Direction.EAST,Direction.SOUTH,Direction.NORTH,Direction.DOWN,Direction.UP};
    public interface Policy {boolean admission(ServerPlayer author,Vec3 at);boolean continues(ServerLevel level,Vec3 at);}
    private final GameController game;private final Policy policy;
    private final Map<UUID,Actor> actors=new LinkedHashMap<>();private final Set<Segment> work=Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean restoring;private int callbacks,lastTick=Integer.MIN_VALUE;
    private static final class Actor {
        Marker body;final ServerLevel level;final String epoch;final LbcIndirectKills.Owner cause;
        long random,steps,writes;int lastPulse;Segment pending;
        Actor(Marker body,String epoch,LbcIndirectKills.Owner cause,long random){this.body=body;level=(ServerLevel)body.level();this.epoch=epoch;this.cause=cause;this.random=random;}
    }
    public LbcMysticalTree(GameController game,Policy policy){this.game=game;this.policy=policy;}
    public static boolean matches(ItemStack stack){return LbcImbaPlaceableItems.tagged(stack,"mystical_tree");}
    public static boolean managed(Entity body){return body instanceof Marker&&body.entityTags().contains(TAG);}
    private boolean running(){return !restoring&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&!game.maps().busy();}
    private boolean paused(ServerLevel level){return !game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped(level)||game.items().wands().timeStopped();}
    public boolean use(ServerPlayer player,InteractionHand hand,Vec3 at){
        var level=player.level();var stack=player.getItemInHand(hand);var pos=BlockPos.containing(at);
        if(!running()||!matches(stack)||!game.canUseGameplayItems(player)||game.items().actionsBlocked(player)||game.items().wands().inputTimeStopped(player)
                ||paused(level)||!LbcDimensionItems.ordinary(level)||!LbcBedwarsRoleGuards.publication(level)||!level.isInValidBounds(pos)||!level.hasChunkAt(pos)||!policy.admission(player,at))return false;
        var marker=new Marker(EntityTypes.MARKER,level);marker.setPos(at);marker.entityTags().addAll(Set.of(TAG,"slowraycast","mystical_tree","instant"));
        var actor=new Actor(marker,game.items().epoch(),game.items().indirectKills().wildCause(player),player.getRandom().nextLong());actor.lastPulse=game.server().getTickCount();actors.put(marker.getUUID(),actor);
        game.items().markCurrentItemEpoch(marker);game.items().spellSources().bind(marker,new LbcSpellSource.Source(marker.getUUID(),player.getUUID(),actor.epoch,actor.cause,Set.of(),true));game.items().timeStop().prepareFrozen(marker);
        if(!level.addFreshEntity(marker)){actors.remove(marker.getUUID());marker.discard();return false;}
        stack.consume(1,player);player.inventoryMenu.broadcastChanges();return true;
    }
    private static long next(Actor actor){long v=(actor.random+=0x9e3779b97f4a7c15L);v=(v^(v>>>30))*0xbf58476d1ce4e5b9L;v=(v^(v>>>27))*0x94d049bb133111ebL;return v^(v>>>31);}
    public void tick(){
        int now=game.server().getTickCount();if(!running()||lastTick==now)return;lastTick=now;callbacks++;
        try{for(var actor:List.copyOf(actors.values())){
            if(!game.items().currentEpoch(actor.epoch)){retire(actor);continue;}
            if(actor.body.isRemoved()){if(actor.body.getRemovalReason()!=Entity.RemovalReason.UNLOADED_TO_CHUNK)retire(actor);continue;}
            if(paused(actor.level)||game.items().timeStop().frozen(actor.body)||!actor.level.hasChunkAt(actor.body.blockPosition()))continue;
            if(!policy.continues(actor.level,actor.body.position())){retire(actor);continue;}
            if(actor.pending!=null||actor.lastPulse==now)continue;actor.lastPulse=now;
            // The command reads height before its independent1% termination decision.
            int height=(int)Math.floor(actor.body.getY());boolean chance=(next(actor)>>>11)*0x1.0p-53<.01;
            boolean terminal=chance||height<12||height>308;
            var direction=terminal?Direction.UP:DIRECTIONS[(int)Long.remainderUnsigned(next(actor),6)];int length=terminal?0:1+(int)Long.remainderUnsigned(next(actor),10);
            var segment=new Segment(actor,actor.body.position(),direction,length,terminal);actor.pending=segment;work.add(segment);game.items().stonekivi().queuedWork(segment);
        }}finally{callbacks--;}
    }
    private final class Segment implements LbcStonekivi.Work {
        final Actor actor;final Vec3 origin;final Direction direction;final int length;final boolean terminal;
        int cursor;boolean closed;
        Segment(Actor actor,Vec3 origin,Direction direction,int length,boolean terminal){this.actor=actor;this.origin=origin;this.direction=direction;this.length=length;this.terminal=terminal;}
        public boolean ready(){return closed||!restoring&&!paused(actor.level)&&!actor.body.isRemoved()&&!game.items().timeStop().frozen(actor.body)&&actor.level.hasChunkAt(actor.body.blockPosition());}
        public boolean snapshotIdle(){return true;}
        public boolean step(){
            if(closed)return true;if(!game.items().currentEpoch(actor.epoch)||actors.get(actor.body.getUUID())!=actor){cancel();return true;}if(!ready())return false;
            if(!policy.continues(actor.level,actor.body.position())){retire(actor);return true;}
            callbacks++;game.items().stonekivi().beginNativeWork();try{
                long deadline=Math.min(EffectTerrain.deadlineNanos(),System.nanoTime()+350_000);int total=terminal?125:length+1;
                while(cursor<total&&System.nanoTime()<deadline){var center=BlockPos.containing(origin);var pos=terminal?center.offset(cursor%5-2,cursor/25-2,cursor/5%5-2):center.relative(direction,cursor);
                    if(actor.level.isInValidBounds(pos)&&!actor.level.hasChunkAt(pos)){game.maps().prepareChunk(actor.level,new ChunkPos(pos.getX()>>4,pos.getZ()>>4));return false;}
                    if(actor.level.isInValidBounds(pos)&&policy.continues(actor.level,Vec3.atCenterOf(pos))&&game.items().writeOwnedTerrain(actor.level,pos,terminal?Blocks.AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true):Blocks.OAK_WOOD.defaultBlockState(),actor.cause,true))actor.writes++;
                    cursor++;
                }
                if(cursor<total)return false;
                if(terminal){
                    var pos=BlockPos.containing(origin);if(actor.level.isInValidBounds(pos)&&policy.continues(actor.level,origin)&&game.items().writeOwnedTerrain(actor.level,pos,Blocks.CHEST.defaultBlockState(),actor.cause)){
                        if(actor.level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity chest)chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:infinity_upgraded_luck_noadv")),0);
                    }
                    spawnCreeper(actor,origin.add(0,-1.2,0));close();retire(actor);
                }else{actor.steps++;actor.body.setPos(origin.relative(direction,length));actor.body.setYRot(switch(direction){case WEST->90;case EAST->-90;case NORTH->180;default->0;});actor.body.setXRot(direction==Direction.UP?-90:direction==Direction.DOWN?90:0);close();}
                return true;
            }finally{game.items().stonekivi().endNativeWork();callbacks--;}
        }
        void close(){closed=true;work.remove(this);if(actor.pending==this)actor.pending=null;}
        public void cancel(){close();}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){int saved=cursor;boolean ended=closed;return links->()->{cursor=saved;closed=ended;};}
    }
    private void spawnCreeper(Actor actor,Vec3 at){
        if(!actor.level.isInValidBounds(BlockPos.containing(at))||!actor.level.hasChunkAt(BlockPos.containing(at)))return;
        var creeper=new Creeper(EntityTypes.CREEPER,actor.level);var data=new CompoundTag();data.putShort("Fuse",(short)1000);data.putByte("ExplosionRadius",(byte)20);data.putBoolean("ignited",true);data.putBoolean("Invulnerable",true);
        creeper.load(TagValueInput.create(ProblemReporter.DISCARDING,actor.level.registryAccess(),data));creeper.setPos(at);creeper.setNoGravity(true);creeper.setPersistenceRequired();
        creeper.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY,1000,0,false,false,false));
        creeper.entityTags().add("skywars2.mystical_tree.creeper");game.items().markCurrentItemEpoch(creeper);game.items().spellSources().bind(creeper,new LbcSpellSource.Source(actor.body.getUUID(),actor.cause.player(),actor.epoch,actor.cause,Set.of(),true));game.items().timeStop().prepareFrozen(creeper);
        if(!actor.level.addFreshEntity(creeper))creeper.discard();
    }
    private void retire(Actor actor){actors.remove(actor.body.getUUID());if(actor.pending!=null)actor.pending.cancel();if(!actor.body.isRemoved())actor.body.discard();}
    public void loaded(Entity body){if(restoring||MatchActorCheckpoint.restoring(body)||!managed(body))return;var actor=actors.get(body.getUUID());if(actor==null||!game.items().currentEpoch(actor.epoch)){body.discard();return;}actor.body=(Marker)body;game.items().timeStop().prepareFrozen(body);}
    public void unloaded(Entity body){if(managed(body)&&body.getRemovalReason()!=Entity.RemovalReason.UNLOADED_TO_CHUNK){var actor=actors.get(body.getUUID());if(actor!=null)retire(actor);}}
    public void stop(){for(var actor:List.copyOf(actors.values()))retire(actor);actors.clear();for(var segment:List.copyOf(work))segment.cancel();restoring=false;lastTick=Integer.MIN_VALUE;}
    public int count(){return actors.size();}
    public record View(Marker body,long steps,long writes,long random,int cursor,boolean terminal,Direction direction,int length,LbcIndirectKills.Owner cause){}
    public List<View> views(){return actors.values().stream().map(a->new View(a.body,a.steps,a.writes,a.random,a.pending==null?-1:a.pending.cursor,a.pending!=null&&a.pending.terminal,a.pending==null?null:a.pending.direction,a.pending==null?0:a.pending.length,a.cause)).toList();}
    static boolean cloneRecognizes(Entity entity){return managed(entity);}
    LbcCloneSkills.ActorImage cloneImage(Entity source){
        var actor=actors.get(source.getUUID());if(actor==null)return null;long random=actor.random,steps=actor.steps,writes=actor.writes;var pending=actor.pending;var pose=source.position();int cursor=pending==null?0:pending.cursor;
        return new LbcCloneSkills.ActorImage(){public void install(Entity ignored,Entity clone,Map<Entity,Entity> relatives){var copy=new Actor((Marker)clone,actor.epoch,actor.cause,random);copy.steps=steps;copy.writes=writes;copy.lastPulse=game.server().getTickCount();actors.put(clone.getUUID(),copy);if(pending!=null){var segment=new Segment(copy,pending.origin.add(clone.position().subtract(pose)),pending.direction,pending.length,pending.terminal);segment.cursor=cursor;copy.pending=segment;work.add(segment);game.items().stonekivi().queuedWork(segment);}}public void rollback(Entity clone){var copy=actors.get(clone.getUUID());if(copy!=null)retire(copy);}};
    }
    public void checkpointBoundary(){if(callbacks!=0)throw new IllegalStateException("Finish mystical tree callbacks before capture");}
    public void prepareFrozen(){checkpointBoundary();restoring=true;actors.clear();work.clear();lastTick=Integer.MIN_VALUE;}
    public void publishFrozen(){restoring=false;}
    public void cancelFrozen(){stop();}
    MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
        checkpointBoundary();record Saved(Actor actor,MatchDamageSources.Ref body,Marker dormant,long random,long steps,long writes,Segment pending){}
        var saved=actors.values().stream().map(a->new Saved(a,a.body.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK?null:graph.links.key(a.body),a.body,a.random,a.steps,a.writes,a.pending)).toList();var queued=List.copyOf(work);queued.forEach(graph::capture);
        return links->{var bodies=new IdentityHashMap<Actor,Marker>();for(var image:saved){MatchAuthorityCheckpoint.world(game,image.actor.level);bodies.put(image.actor,image.body==null?image.dormant:(Marker)links.get(image.body));}return ()->{actors.clear();for(var image:saved){var a=image.actor;a.body=bodies.get(a);a.random=image.random;a.steps=image.steps;a.writes=image.writes;a.pending=image.pending;a.lastPulse=game.server().getTickCount();actors.put(a.body.getUUID(),a);}work.clear();work.addAll(queued);};};
    }
}
