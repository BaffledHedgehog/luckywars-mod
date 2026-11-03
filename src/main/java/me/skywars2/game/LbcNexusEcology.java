package me.skywars2.game;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Environmental Nexus actors. Admission is event driven; each scheduled source step is atomic. */
public final class LbcNexusEcology {
    static final String TAG="skywars2.nexus_ecology";
    private static final TicketType TICKET=new TicketType(0,TicketType.FLAG_LOADING|TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private static final int[][] TREE_AXES={{-1,0,0},{1,0,0},{0,0,1},{0,0,-1},{0,-1,0},{0,1,0}};
    private static final int[][] SPREAD_AXES={{0,1,0},{0,-1,0},{0,0,1},{0,0,-1},{1,0,0},{-1,0,0}};
    enum Kind {
        CENTER("center_cube"),TREE("nexus_tree"),SPREAD("explose_wired");
        final String tag;
        Kind(String tag){this.tag=tag;}
    }
    @FunctionalInterface interface CellPolicy {
        boolean allowed(ServerLevel level,Vec3 position,UUID owner);
    }
    interface Callbacks {
        /** Commit the green reward; return false when construction/output must wait. */
        boolean emitGreen(ServerLevel level,Vec3 position,LbcIndirectKills.Owner cause);
        void emitBlack(ServerLevel level,Vec3 position,LbcIndirectKills.Owner cause);
        boolean nearCage(ServerLevel level,Vec3 position,double minimum,double maximum);
        void createCage(ServerLevel level,Vec3 position,int color,LbcIndirectKills.Owner cause);
        /** Author-selected tree_end_all cleanup; called only for its independent five-percent draw. */
        void finishTree(ServerLevel level,Vec3 position,LbcIndirectKills.Owner cause);
        void clearSpreadDisplays(ServerLevel level,Vec3 position);
    }
    static final class Actor {
        final UUID id;final ServerLevel level;final Kind kind;final LbcIndirectKills.Owner cause;
        Entity body;Vec3 position;int born;long lifetime;Step pending;
        Actor(UUID id,ServerLevel level,Kind kind,LbcIndirectKills.Owner cause,Entity body,Vec3 position,int born,long lifetime,Step pending){
            this.id=id;this.level=level;this.kind=kind;this.cause=cause;this.body=body;this.position=position;this.born=born;this.lifetime=lifetime;this.pending=pending;
        }
    }
    private record Step(int axis,int length){}
    private record Chunk(ServerLevel level,ChunkPos pos){}
    private static final class Loading {
        final CompletableFuture<?> future;final Set<UUID> actors=new HashSet<>();
        Loading(CompletableFuture<?> future){this.future=future;}
    }
    private record Image(UUID id,ServerLevel level,Kind kind,LbcIndirectKills.Owner cause,Vec3 position,long lifetime,Step pending,boolean loaded){}
    private final GameController game;private final CellPolicy policy;private final Callbacks callbacks;
    private final Map<UUID,Actor> actors=new LinkedHashMap<>();
    private final Deque<UUID> queue=new ArrayDeque<>();
    private final Map<Chunk,Loading> chunks=new HashMap<>();
    private final Map<UUID,Set<Chunk>> waits=new HashMap<>();
    // An unloaded actor cancelled by a dimension-wide stop must not revive on its next load.
    private final Set<UUID> retired=new HashSet<>();
    private boolean restoring;private int lastTick=Integer.MIN_VALUE;

    LbcNexusEcology(GameController game,CellPolicy policy,Callbacks callbacks){this.game=game;this.policy=Objects.requireNonNull(policy);this.callbacks=Objects.requireNonNull(callbacks);}
    public static void register(){Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.parse("skywars2:nexus_ecology"),TICKET);}
    List<Actor> snapshot(){return List.copyOf(actors.values());}
    int count(){return actors.size();}
    int count(Kind kind){int count=0;for(var a:actors.values())if(a.kind==kind)count++;return count;}
    int loadedCount(){int count=0;for(var a:actors.values())if(a.body!=null&&!a.body.isRemoved())count++;return count;}
    int unloadedCount(){return count()-loadedCount();}
    int queuedSteps(){int count=0;for(var a:actors.values())if(a.pending!=null)count++;return count;}
    int loadingChunks(){return chunks.size();}
    boolean busy(){return !waits.isEmpty();}
    private static boolean nexus(ServerLevel level){return level.dimension().equals(LbcHedgehogDimensions.NEXUS);}
    private static Kind kind(Entity entity){for(var kind:Kind.values())if(entity.entityTags().contains(kind.tag))return kind;return null;}
    private static String value(Entity entity,String key){String prefix=TAG+"."+key+".";for(var tag:entity.entityTags())if(tag.startsWith(prefix))return tag.substring(prefix.length());return "";}
    private static void tag(Entity entity,String key,String value){entity.entityTags().add(TAG+"."+key+"."+value);}
    private void install(Actor actor){actors.put(actor.id,actor);queue.addLast(actor.id);}

    Actor center(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause){return create(level,point,cause,Kind.CENTER);}
    Actor tree(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause){return create(level,point,cause,Kind.TREE);}
    Actor spread(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause){return create(level,point,cause,Kind.SPREAD);}
    private Actor create(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause,Kind kind){
        if(restoring||!nexus(level))return null;
        var marker=new Marker(EntityTypes.MARKER,level);marker.setPos(point);marker.entityTags().add(TAG);marker.entityTags().add("nexus_rc");marker.entityTags().add(kind.tag);
        if(cause!=null){tag(marker,"owner",cause.player().toString());tag(marker,"order",Long.toString(cause.order()));}
        var actor=new Actor(marker.getUUID(),level,kind,cause,marker,point,game.server().getTickCount(),0,null);install(actor);
        if(!level.addFreshEntity(marker)){remove(actor);return null;}return actor;
    }
    void loaded(Entity body){
        if(restoring||!(body instanceof Marker)||!(body.level() instanceof ServerLevel level)||!nexus(level))return;
        if(retired.remove(body.getUUID())){body.discard();return;}
        Kind kind=kind(body);if(kind==null||!body.entityTags().contains("nexus_rc"))return;
        var previous=actors.get(body.getUUID());if(previous!=null){previous.body=body;previous.position=body.position();return;}
        LbcIndirectKills.Owner cause=null;
        try{String owner=value(body,"owner");if(!owner.isEmpty())cause=new LbcIndirectKills.Owner(UUID.fromString(owner),Long.parseLong(value(body,"order")),true,false);}
        catch(IllegalArgumentException malformed){/* Legacy actors without valid ownership remain environmental. */}
        body.entityTags().add(TAG);
        install(new Actor(body.getUUID(),level,kind,cause,body,body.position(),game.server().getTickCount(),0,null));
    }
    void unloaded(Entity body){
        var a=actors.get(body.getUUID());if(a==null||a.body!=body)return;
        release(a.id);
        if(body.getRemovalReason()!=null&&body.getRemovalReason()!=Entity.RemovalReason.UNLOADED_TO_CHUNK){actors.remove(a.id);return;}
        a.position=body.position();a.body=null;
    }
    private void release(UUID id){var keys=waits.remove(id);if(keys==null)return;for(var key:keys){var loading=chunks.get(key);if(loading!=null&&loading.actors.remove(id)&&loading.actors.isEmpty()){key.level().getChunkSource().removeTicketWithRadius(TICKET,key.pos(),0);chunks.remove(key);}}}
    private void remove(Actor a){if(actors.remove(a.id)!=a)return;release(a.id);if(a.body!=null)a.body.discard();else retired.add(a.id);}
    private boolean resident(Actor actor,double radius){
        Vec3 point=actor.position;boolean ready=true;
        for(int x=(int)Math.floor((point.x-radius)/16);x<=(int)Math.floor((point.x+radius)/16);x++)for(int z=(int)Math.floor((point.z-radius)/16);z<=(int)Math.floor((point.z+radius)/16);z++){
            var key=new Chunk(actor.level,new ChunkPos(x,z));
            var loading=chunks.get(key);
            if(loading==null){
                // A resident chunk needs neither a ticket nor an asynchronous completion round trip.
                if(actor.level.getChunkSource().getChunkNow(x,z)!=null)continue;
                loading=new Loading(key.level().getChunkSource().addTicketAndLoadWithRadius(TICKET,key.pos(),0));chunks.put(key,loading);
            }else if(loading.future.isCompletedExceptionally()||loading.future.isCancelled()){
                // Failed requests are retriable construction work, not a permanent gameplay stall.
                key.level().getChunkSource().removeTicketWithRadius(TICKET,key.pos(),0);
                var replacement=new Loading(key.level().getChunkSource().addTicketAndLoadWithRadius(TICKET,key.pos(),0));
                replacement.actors.addAll(loading.actors);chunks.put(key,replacement);loading=replacement;
            }
            loading.actors.add(actor.id);waits.computeIfAbsent(actor.id,k->new HashSet<>()).add(key);
            if(actor.level.getChunkSource().getChunkNow(x,z)==null)ready=false;
        }return ready;
    }
    private boolean nearby(Actor a,Kind kind,double radius){
        // Bounded native lookup keeps arbitrarily many distant islands out of per-step work.
        for(var body:a.level.getEntitiesOfClass(Marker.class,new AABB(a.position,a.position).inflate(radius),e->e.entityTags().contains(kind.tag)&&e.position().distanceToSqr(a.position)<=radius*radius))
            if(actors.containsKey(body.getUUID()))return true;
        return false;
    }
    private void write(ServerLevel level,BlockPos position,net.minecraft.world.level.block.Block block){if(!level.isOutsideBuildHeight(position))LbcDropBlocks.setBlock(level,position,block.defaultBlockState(),true);}
    private boolean playerNear(Actor a){for(var player:a.level.players())if(player.position().distanceToSqr(a.position)<=22500)return true;return false;}
    private void treeStep(Actor a){
        if(a.pending==null){if(!playerNear(a))return;a.pending=new Step(a.level.getRandom().nextInt(6),1+a.level.getRandom().nextInt(6));}
        if(!resident(a,7))return;
        // Killing @s does not abort the remaining source function commands.
        if(callbacks.nearCage(a.level,a.position,.01,5))remove(a);
        var step=a.pending;var axis=TREE_AXES[step.axis()];Vec3 from=a.position,end=from.add(axis[0]*step.length(),axis[1]*step.length(),axis[2]*step.length());
        a.body.setYRot(switch(step.axis()){case 0->90;case 1->-90;case 3->180;default->0;});
        a.body.setXRot(step.axis()==4?90:step.axis()==5?-90:0);
        game.items().indirectKills().capturedWorldChanges(a.level,a.cause,()->{
            for(int i=0;i<=step.length();i++){var pos=BlockPos.containing(from.add(axis[0]*i,axis[1]*i,axis[2]*i));if(a.level.getBlockState(pos).is(Blocks.AIR))write(a.level,pos,Blocks.CONCRETE.pick(DyeColor.LIME));}
        });
        a.body.teleportTo(end.x,end.y,end.z);a.position=end;a.pending=null;release(a.id);
        if(a.level.getRandom().nextFloat()<.10f){
            if(!nearby(a,Kind.CENTER,15)&&!callbacks.nearCage(a.level,end,.01,5))callbacks.createCage(a.level,end,1+a.level.getRandom().nextInt(4),a.cause);
            if(a.level.getRandom().nextFloat()<.05f)callbacks.finishTree(a.level,end,a.cause);
            remove(a);
        }
        // The source draws this independently after tree_end; preserve its command-context position.
        if(a.level.getRandom().nextFloat()<.20f)tree(a.level,end,a.cause);
    }
    private void centerStep(Actor a){
        if(!resident(a,0))return;
        if(a.level.getBlockState(BlockPos.containing(a.position)).is(Blocks.AIR)&&callbacks.emitGreen(a.level,a.position,a.cause)){
            spread(a.level,a.position,a.cause);remove(a);
        }else release(a.id);
    }
    private void spreadStep(Actor a){
        if(!resident(a,4))return;
        game.items().indirectKills().capturedWorldChanges(a.level,a.cause,()->{
            write(a.level,BlockPos.containing(a.position),Blocks.CONCRETE.pick(DyeColor.BLACK));
            for(var axis:SPREAD_AXES){var next=a.position.add(axis[0],axis[1],axis[2]);if(!a.level.getBlockState(BlockPos.containing(next)).is(Blocks.CONCRETE.pick(DyeColor.BLACK))&&a.level.getRandom().nextFloat()<.8f)spread(a.level,next,a.cause);}
            for(var axis:SPREAD_AXES)if(a.level.getRandom().nextFloat()<.05f)spread(a.level,a.position.add(axis[0]*4,axis[1]*4,axis[2]*4),a.cause);
        });
        callbacks.clearSpreadDisplays(a.level,a.position);
        var duplicates=a.level.getEntitiesOfClass(Marker.class,new AABB(a.position,a.position).inflate(.01),body->body.entityTags().contains(Kind.SPREAD.tag)&&body.position().distanceToSqr(a.position)<=.0001);
        if(duplicates.size()>=2)for(var body:duplicates){var other=actors.get(body.getUUID());if(other!=null)remove(other);}
        if(a.level.getRandom().nextFloat()<.00015625f)callbacks.emitBlack(a.level,a.position,a.cause);
        if(a.level.getRandom().nextFloat()<.005f)clearSpread(a.level);
        release(a.id);
    }
    void clearSpread(ServerLevel level){for(var a:List.copyOf(actors.values()))if(a.level==level&&a.kind==Kind.SPREAD)remove(a);}
    void clearTrees(ServerLevel level,Vec3 point,double radius){for(var a:List.copyOf(actors.values()))if(a.level==level&&a.kind==Kind.TREE&&a.position.distanceToSqr(point)<=radius*radius)remove(a);}
    public void tick(){
        int tick=game.server().getTickCount();if(restoring||lastTick==tick)return;lastTick=tick;
        if(!game.playing()||game.stage()==GameController.Stage.ENDING||!game.server().tickRateManager().runsNormally()||game.items().wands().legacyTimeStopped())return;
        int remaining=queue.size(),work=0;long now=System.nanoTime();
        long until=Math.max(now+200_000,Math.min(EffectTerrain.deadlineNanos(),now+2_000_000));
        while(remaining-->0&&!queue.isEmpty()&&work<256&&(work<4||System.nanoTime()<until)){
            work++;
            UUID id=queue.removeFirst();var a=actors.get(id);if(a==null)continue;var body=a.body;
            if(body!=null){
                if(body.isRemoved()){remove(a);continue;}
                a.position=body.position();
                if(a.born!=tick&&!game.items().timeStop().stopped(a.level)&&!body.entityTags().contains("time_stopped")&&policy.allowed(a.level,a.position,a.cause==null?null:a.cause.player())){
                    a.lifetime++;switch(a.kind){case CENTER->centerStep(a);case TREE->treeStep(a);case SPREAD->spreadStep(a);}
                }else release(a.id);
            }
            if(actors.containsKey(id))queue.addLast(id);
        }
    }
    private void releaseTickets(){for(var key:chunks.keySet())key.level().getChunkSource().removeTicketWithRadius(TICKET,key.pos(),0);chunks.clear();waits.clear();}
    private void clearDerived(){releaseTickets();actors.clear();queue.clear();}
    /** Match boundaries pause the persistent environmental world; they do not erase its actors. */
    public void stop(){releaseTickets();restoring=false;}
    /** Explicit permanent cleanup, also used by isolated fixtures. */
    public void clear(){for(var a:List.copyOf(actors.values()))remove(a);clearDerived();restoring=false;}
    void prepareFrozen(){clearDerived();retired.clear();restoring=true;}
    void publishFrozen(){restoring=false;lastTick=game.server().getTickCount();}
    Checkpoint checkpoint(){return new Checkpoint();}
    final class Checkpoint {
        final List<Image> images=actors.values().stream().filter(a->a.body==null||!a.body.isRemoved()).map(a->new Image(a.id,a.level,a.kind,a.cause,a.body==null?a.position:a.body.position(),a.lifetime,a.pending,a.body!=null)).toList();
        final Set<UUID> imageIds=images.stream().map(Image::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
        final List<UUID> order=queue.stream().filter(imageIds::contains).toList();final Set<UUID> cancelled=Set.copyOf(retired);
        MatchFieldCheckpoint.Plan prepare(Function<UUID,Entity> lookup){
            var missing=new HashSet<UUID>();var restored=new LinkedHashMap<UUID,Actor>();
            for(var image:images){var body=lookup.apply(image.id());if(image.loaded()&&!(body instanceof Marker)){missing.add(image.id());continue;}
                restored.put(image.id(),new Actor(image.id(),image.level(),image.kind(),image.cause(),body,image.position(),-1,image.lifetime(),image.pending()));}
            return new MatchFieldCheckpoint.Plan(Set.copyOf(missing),()->{clearDerived();actors.putAll(restored);queue.addAll(order);retired.clear();retired.addAll(cancelled);});
        }
    }
}
