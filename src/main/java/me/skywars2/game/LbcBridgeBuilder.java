package me.skywars2.game;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Native most_imba function family; unrelated to the retired projegadget+most wool trail. */
public final class LbcBridgeBuilder {
    private static final String ACTOR="skywars2.bridgebuilder_imba";
    private static final Identifier[] TEMPLATES={id("most"),id("most_column_1"),id("most_column_2"),
            id("most_column_3"),id("most_column_4"),id("most_column_5"),id("most_column_6")};
    private static final MapManager.BlockSnapshot CHISELED=cell(Blocks.CHISELED_QUARTZ_BLOCK),
            BRICKS=cell(Blocks.QUARTZ_BRICKS),PILLAR=cell(Blocks.QUARTZ_PILLAR),LANTERN=cell(Blocks.SEA_LANTERN);
    interface Policy {
        boolean launch(LivingEntity author,ServerLevel level,Vec3 origin);
        /** Canonical mode/protected-role checks, native journal and capturedWorldChanges belong here.
         * Return actual changed-cell success; recursive source supports depend on this result. */
        boolean write(LivingEntity author,ServerLevel level,BlockPos at,MapManager.BlockSnapshot state,LbcIndirectKills.Owner cause);
    }
    record Cell(BlockPos relative,MapManager.BlockSnapshot snapshot) { }
    record View(UUID actor,int lifetime,boolean dormant,boolean pending,Vec3 position,int queued,long visits,long writes) { }
    private final GameController game;private final Policy policy;
    private volatile List<List<Cell>> templates=List.of();
    private CompletableFuture<?> preparation;
    private LinkedHashMap<UUID,Actor> actors=new LinkedHashMap<>();
    private Set<UUID> acceptedProjectiles=new HashSet<>();
    private int callbacks,lastTick=Integer.MIN_VALUE;private boolean restoring;
    LbcBridgeBuilder(GameController game,Policy policy){this.game=game;this.policy=policy;}
    private static Identifier id(String path){return Identifier.fromNamespaceAndPath("lbc",path);}
    private static MapManager.BlockSnapshot cell(net.minecraft.world.level.block.Block block){return new MapManager.BlockSnapshot(block.defaultBlockState(),null);}
    static boolean recognized(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"most_imba",0)==1;}
    static boolean managed(Entity entity){return entity!=null&&entity.entityTags().contains(ACTOR);}

    /** Decode the exact seven existing native templates once; no placement-time NBT/IO. */
    void prepare(){
        if(preparation!=null)return;
        var blocks=game.server().registryAccess().lookupOrThrow(Registries.BLOCK);
        preparation=game.maps().prepareSpellTemplates(List.of(TEMPLATES)).thenAccept(parts->{
            var decoded=new ArrayList<List<Cell>>();
            for(var id:TEMPLATES){
                var rows=new ArrayList<Cell>();
                for(var part:parts.getOrDefault(id,List.of())){
                    var nbt=part.template().save(new CompoundTag());
                    if(!nbt.getListOrEmpty("entities").isEmpty()||!nbt.getListOrEmpty("palettes").isEmpty())
                        throw new IllegalStateException("Bridge source template shape needs a reviewed adapter: "+id);
                    var palette=nbt.getListOrEmpty("palette");
                    for(var raw:nbt.getListOrEmpty("blocks")){
                        var row=(CompoundTag)raw;var pos=row.getListOrEmpty("pos");
                        var state=NbtUtils.readBlockState(blocks,(CompoundTag)palette.get(row.getIntOr("state",0)));
                        if(state.is(Blocks.STRUCTURE_VOID))continue;
                        if(row.contains("nbt"))throw new IllegalStateException("Bridge block-entity template requires an immutable cell adapter: "+id);
                        rows.add(new Cell(new BlockPos(pos.getIntOr(0,0),pos.getIntOr(1,0),pos.getIntOr(2,0)),new MapManager.BlockSnapshot(state,null)));
                    }
                }
                if(rows.isEmpty())throw new IllegalStateException("Missing Bridge source template "+id);
                decoded.add(List.copyOf(rows));
            }
            templates=List.copyOf(decoded);
        });
    }
    boolean ready(){return templates.size()==7;}
    private final class Actor {
        Marker body;LivingEntity author;final LbcIndirectKills.Owner cause;final String epoch;
        int lifetime;Generation pending;
        Actor(Marker body,LivingEntity author,LbcIndirectKills.Owner cause,String epoch,int lifetime){this.body=body;this.author=author;this.cause=cause;this.epoch=epoch;this.lifetime=lifetime;}
    }
    /** Called once for actual owned snowball insertion. Native projectile remains independently alive.
     * The source caller is the thrower's player context at FEET, not the snowball impact location. */
    boolean launch(Entity projectile,LivingEntity author){
        if(restoring||author==null||!(projectile.level() instanceof ServerLevel level)||!ready()||!game.playing()
                ||!LbcDimensionItems.ordinary(level)||acceptedProjectiles.contains(projectile.getUUID())
                ||!policy.launch(author,level,author.position()))return false;
        callbacks++;try{
            var marker=new Marker(EntityTypes.MARKER,level);marker.setPos(author.position());marker.setYRot(author.getYRot());marker.setXRot(author.getXRot());
            marker.entityTags().addAll(List.of(ACTOR,"imba_rc","slowraycast","imba_most","mecha"));game.items().markCurrentItemEpoch(marker);
            game.items().spellSources().inherit(projectile,marker);
            var source=game.items().spellSources().source(projectile);
            var cause=source==null?null:source.cause();
            if(cause==null&&author instanceof ServerPlayer player)cause=game.items().indirectKills().wildCause(player);
            var actor=new Actor(marker,author,cause,game.items().epoch(),0);actors.put(marker.getUUID(),actor);
            game.items().timeStop().prepareFrozen(marker);
            if(!level.addFreshEntity(marker)){actors.remove(marker.getUUID());marker.discard();return false;}
            acceptedProjectiles.add(projectile.getUUID());return true;
        }finally{callbacks--;}
    }
    private boolean nearby(Marker marker){for(var player:((ServerLevel)marker.level()).players())if(!player.isSpectator()&&player.distanceToSqr(marker)<=40_000)return true;return false;}
    void tick(){
        if(restoring)return;callbacks++;try{
            int now=game.server().getTickCount();if(lastTick==now)return;lastTick=now;
            for(var actor:List.copyOf(actors.values())){
                var body=actor.body;
                if(!game.playing()||!game.items().currentEpoch(actor.epoch)){remove(actor,true);continue;}
                if(body.isRemoved()){
                    if(body.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)continue;
                    remove(actor,false);continue;
                }
                var level=(ServerLevel)body.level();if(!LbcDimensionItems.ordinary(level)||game.items().timeStop().stopped(level)||game.items().timeStop().frozen(body))continue;
                // TECH retires future production; an accepted segment still drains to completion.
                if(game.items().magic().mechanismSuppressedAt(level,body.position())){remove(actor,true);continue;}
                // The real common raycast selector increments Lifetime even outside the 200 radius.
                actor.lifetime++;
                if(actor.pending!=null||!nearby(body))continue;
                Vec3 next=local(body.position(),body.getYRot(),body.getXRot(),0,0,4);
                body.setPos(next);
                actor.pending=new Generation(actor,next,actor.lifetime);game.items().stonekivi().queuedWork(actor.pending);
            }
        }finally{callbacks--;}
    }
    /** All nodes are immutable typed values; dynamic recursion pushes finite resumable native nodes. */
    private sealed interface Op permits Emit,Template,Fill,Branches,Colon,Side,Support,Columns { }
    private record Emit(BlockPos at,MapManager.BlockSnapshot cell) implements Op { }
    private record Template(int kind,BlockPos origin,int cursor) implements Op { }
    private record Fill(BlockPos min,int width,int depth,int cursor,MapManager.BlockSnapshot cell) implements Op { }
    private record Branches(int phase) implements Op { }
    private record Colon(Vec3 at,int phase) implements Op { }
    private record Side(Vec3 at,int yaw,int counter,int phase) implements Op { }
    private record Support(BlockPos at,int cursor,boolean changedZ) implements Op { }
    private record Columns(Vec3 at,int counter) implements Op { }

    private final class Generation implements LbcStonekivi.Work {
        final Actor actor;final ServerLevel level;final Vec3 origin;final int lifetime;final float yaw;
        final RandomSource random;
        ArrayDeque<Op> ops=new ArrayDeque<>();boolean closed,blocked;long visits,writes;
        Generation(Actor actor,Vec3 origin,int lifetime){
            this.actor=actor;this.level=(ServerLevel)actor.body.level();this.origin=origin;this.lifetime=lifetime;this.yaw=actor.body.getYRot();
            random=RandomSource.create(actor.body.getRandom().nextLong());
            push(new Template(0,BlockPos.containing(origin.add(-4,-10,-4)),0),new Branches(0));
        }
        void push(Op... additions){for(int i=additions.length-1;i>=0;i--)ops.addFirst(additions[i]);}
        boolean chance(float value){return random.nextFloat()<value;}
        int range(int min,int max){return min+random.nextInt(max-min+1);}
        public boolean ready(){return !restoring&&(closed||!game.items().timeStop().stopped(level));}
        public boolean snapshotIdle(){return true;}
        public boolean step(){
            if(restoring)return false;callbacks++;game.items().stonekivi().beginNativeWork();try{
                if(closed)return true;
                if(!game.playing()||!game.items().currentEpoch(actor.epoch)){cancel();return true;}
                if(!ready())return false;
                long deadline=Math.min(EffectTerrain.deadlineNanos(),System.nanoTime()+350_000);blocked=false;
                for(int count=0;count<256&&!ops.isEmpty()&&!blocked;count++){
                    if((count&15)==0&&System.nanoTime()>=deadline)break;
                    visits++;execute(ops.removeFirst());
                }
                if(!ops.isEmpty())return false;
                closed=true;if(actor.pending==this)actor.pending=null;return true;
            }finally{game.items().stonekivi().endNativeWork();callbacks--;}
        }
        private boolean write(BlockPos at,MapManager.BlockSnapshot state){
            if(level.isOutsideBuildHeight(at))return false;
            if(level.getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4)==null){
                game.maps().prepareChunk(level,new ChunkPos(at.getX()>>4,at.getZ()>>4));blocked=true;return false;
            }
            if(level.getBlockState(at).equals(state.state())&&state.entity()==null)return false;
            boolean changed=policy.write(currentAuthor(actor),level,at,state,actor.cause);if(changed)writes++;return changed;
        }
        private void execute(Op op){
            if(op instanceof Emit emit){write(emit.at,emit.cell);if(blocked)push(op);}
            else if(op instanceof Template template){
                var rows=templates.get(template.kind);
                if(template.cursor<rows.size()){
                    var cell=rows.get(template.cursor);
                    push(new Emit(template.origin.offset(cell.relative),cell.snapshot),new Template(template.kind,template.origin,template.cursor+1));
                }
            }else if(op instanceof Fill fill){
                if(fill.cursor<fill.width*fill.depth){
                    var pos=fill.min.offset(fill.cursor%fill.width,0,fill.cursor/fill.width);
                    push(new Emit(pos,fill.cell),new Fill(fill.min,fill.width,fill.depth,fill.cursor+1,fill.cell));
                }
            }else if(op instanceof Branches branch){
                if(branch.phase==0){
                    push(new Branches(1));
                    if(chance(.5f)){
                        int lateral=range(1,2)==1?range(8,16):range(-16,-8),vertical=range(-20,10);
                        var at=local(origin,yaw,0,lateral,0,0).add(0,vertical,0);
                        push(new Colon(at.add(1,0,0),0),new Colon(at.add(0,2,1),0),new Colon(at.add(-1,4,0),0),new Colon(at.add(0,2,-1),0),new Colon(at.add(0,-5,0),0));
                    }
                }else if(branch.phase==1){
                    push(new Branches(2));
                    if(chance(.05f)){
                        int yaw=range(1,2)==1?0:90;
                        var at=origin.add(range(-10,10),range(-20,20),range(-10,10));
                        push(new Side(at,yaw,0,0),new Side(at,yaw+180,0,0));
                    }
                }else if(Math.floorMod(lifetime,7)==0){
                    int y=net.minecraft.util.Mth.floor(origin.y);
                    if(y>=10&&y<=319)push(new Columns(origin,y));
                }
            }else if(op instanceof Colon colon){
                if(colon.phase==0)push(new Emit(BlockPos.containing(colon.at.add(0,-1,0)),PILLAR),new Emit(BlockPos.containing(colon.at),LANTERN),new Colon(colon.at,1));
                else if(chance(.9f)&&colon.at.distanceToSqr(origin)<=400)push(new Colon(colon.at.add(0,1,0),0));
            }else if(op instanceof Columns column){
                int kind=range(1,6),offset=kind==1?-1:-2;
                int counter=column.counter-4;
                if(counter>=10&&counter<=319)push(new Columns(column.at.add(0,-5,0),counter));
                push(new Template(kind,BlockPos.containing(column.at.add(offset,-15,offset)),0));
            }else if(op instanceof Side side){
                if(side.phase==0){
                    push(new Fill(BlockPos.containing(side.at.add(-2,0,-2)),5,5,0,CHISELED),
                            new Fill(BlockPos.containing(side.at.add(-1,-1,-1)),3,3,0,BRICKS),
                            new Emit(BlockPos.containing(local(side.at,side.yaw,0,-2,1,1)),BRICKS),
                            new Emit(BlockPos.containing(local(side.at,side.yaw,0,2,1,1)),BRICKS),
                            new Emit(BlockPos.containing(local(side.at,side.yaw,0,-2,1,-1)),BRICKS),
                            new Emit(BlockPos.containing(local(side.at,side.yaw,0,2,1,-1)),BRICKS),new Side(side.at,side.yaw,side.counter,1));
                }else if(side.phase==1){
                    push(new Side(side.at,side.yaw,side.counter,2));if(chance(.5f))push(new Emit(BlockPos.containing(side.at),LANTERN));
                }else{
                    int counter=side.counter+1;
                    boolean support=counter>=2&&chance(.8f);
                    if(side.at.distanceToSqr(origin)<=40_000)push(new Side(local(side.at,side.yaw,0,0,0,5),side.yaw,support?0:counter,0));
                    if(support)push(new Support(BlockPos.containing(side.at.add(0,-2,0)),0,false));
                }
            }else if(op instanceof Support support){
                // First source fill is X cross, second Z cross. Only the SECOND fill success
                // controls recursion, after seeing the actual X fill and prior accepted writes.
                if(support.cursor==6){if(support.changedZ)push(new Support(support.at.below(),0,false));return;}
                int i=support.cursor;
                BlockPos pos=i<3?support.at.offset(i-1,0,0):support.at.offset(0,0,i-4);
                boolean changed=write(pos,PILLAR);
                if(blocked)push(support);
                else push(new Support(support.at,i+1,support.changedZ||i>=3&&changed));
            }
        }
        public void cancel(){closed=true;ops.clear();if(actor.pending==this)actor.pending=null;}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            // Accepted terrain is independent of the producer body's lifetime. Its immutable
            // origin/yaw/cause and explicit operation cursor suffice after a bomb removes it.
            // Live producer bodies are rebound by the service image, not required by this job.
            checkpointBoundary();var author=graph.links.key(currentAuthor(actor));
            var saved=List.copyOf(ops);var rng=MatchRandomState.capture(random);boolean done=closed;long seen=visits,changed=writes;
            return links->{checkpointBoundary();MatchAuthorityCheckpoint.world(game,level);var owner=(LivingEntity)links.get(author);
                return ()->{actor.author=owner;ops=new ArrayDeque<>(saved);rng.restore(random);closed=done;blocked=false;visits=seen;writes=changed;};};
        }
    }
    private static Vec3 local(Vec3 origin,float yaw,float pitch,double left,double up,double forwards){
        // Same float/Mth basis as vanilla LocalCoordinates, including block-floor behavior
        // near axis-aligned integer boundaries; ordinary double trig can choose another cell.
        float radians=(float)(Math.PI/180),f=net.minecraft.util.Mth.cos((yaw+90)*radians),g=net.minecraft.util.Mth.sin((yaw+90)*radians);
        float h=net.minecraft.util.Mth.cos(-pitch*radians),i=net.minecraft.util.Mth.sin(-pitch*radians);
        float j=net.minecraft.util.Mth.cos((-pitch+90)*radians),k=net.minecraft.util.Mth.sin((-pitch+90)*radians);
        var forward=new Vec3(f*h,i,g*h);var vertical=new Vec3(f*j,k,g*j);var lateral=forward.cross(vertical).scale(-1);
        return origin.add(lateral.scale(left)).add(vertical.scale(up)).add(forward.scale(forwards));
    }
    int count(){return actors.size();}
    List<Entity> snapshot(){return actors.values().stream().map(a->(Entity)a.body).toList();}
    List<View> views(){return actors.values().stream().map(a->new View(a.body.getUUID(),a.lifetime,!nearby(a.body),a.pending!=null,a.body.position(),a.pending==null?0:a.pending.ops.size(),a.pending==null?0:a.pending.visits,a.pending==null?0:a.pending.writes)).toList();}
    private LivingEntity currentAuthor(Actor actor){
        if(actor.author instanceof ServerPlayer player){var current=game.server().getPlayerList().getPlayer(player.getUUID());if(current!=null)return current;}
        return actor.author;
    }
    LivingEntity owner(Entity entity){var actor=entity==null?null:actors.get(entity.getUUID());return actor==null?null:currentAuthor(actor);}
    LbcIndirectKills.Owner cause(Entity entity){var actor=entity==null?null:actors.get(entity.getUUID());return actor==null?null:actor.cause;}
    void loaded(Entity entity){
        if(restoring||MatchActorCheckpoint.restoring(entity))return;
        if(!restoring&&entity instanceof Snowball ball&&recognized(ball.getItem())&&ball.getOwner() instanceof LivingEntity author){launch(ball,author);return;}
        if(!managed(entity))return;
        var actor=actors.get(entity.getUUID());if(actor==null||!(entity instanceof Marker marker)||!game.items().currentEpoch(actor.epoch)){entity.discard();return;}
        actor.body=marker;game.items().timeStop().prepareFrozen(marker);
    }
    void unloaded(Entity entity){/* Retained UUID entry is rebound without a second launch. */}
    void removed(Entity entity){
        if(restoring||MatchActorCheckpoint.restoring(entity))return;
        // Destroyed native inputs cannot reload. Chunk/dimension unload keeps its UUID admission
        // so a surviving snowball never creates a second logical builder when it returns.
        var reason=entity.getRemovalReason();
        if(reason!=null&&reason.shouldDestroy())acceptedProjectiles.remove(entity.getUUID());
        var actor=actors.get(entity.getUUID());if(actor!=null&&reason!=null&&reason.shouldDestroy())remove(actor,false);
    }
    private void remove(Actor actor,boolean discard){
        actors.remove(actor.body.getUUID());
        // A bomb/removal stops future production. The already accepted native generation drains
        // through its queue adapter; only match epoch teardown cancels its writes.
        if(discard&&!actor.body.isRemoved())actor.body.discard();
    }
    void stop(){for(var actor:List.copyOf(actors.values())){if(actor.pending!=null)actor.pending.cancel();remove(actor,true);}acceptedProjectiles.clear();lastTick=Integer.MIN_VALUE;restoring=false;}
    void checkpointBoundary(){if(callbacks!=0)throw new IllegalStateException("Finish Bridge Builder native quantum before capture/replay");}
    void prepareFrozen(){checkpointBoundary();restoring=true;actors=new LinkedHashMap<>();acceptedProjectiles=new HashSet<>();}
    void publishFrozen(){checkpointBoundary();restoring=false;lastTick=game.server().getTickCount();}
    void cancelFrozen(){checkpointBoundary();restoring=false;}
    Checkpoint checkpoint(MatchQueuedWorkCheckpoint.Graph graph){checkpointBoundary();return new Checkpoint(graph);}
    final class Checkpoint {
        private record Image(Actor actor,MatchDamageSources.Ref body,MatchDamageSources.Ref author,int lifetime,Generation pending) { }
        final List<Image> saved=new ArrayList<>();final Set<UUID> projectiles=Set.copyOf(acceptedProjectiles);
        final MatchAuthorityCheckpoint.Clock clock=new MatchAuthorityCheckpoint.Clock(game,lastTick,0);
        Checkpoint(MatchQueuedWorkCheckpoint.Graph graph){for(var actor:actors.values()){
            saved.add(new Image(actor,graph.links.key(actor.body),graph.links.key(currentAuthor(actor)),actor.lifetime,actor.pending));
            if(actor.pending!=null)graph.capture(actor.pending);
        }}
        Runnable prepare(MatchSpellCheckpoint.Links links){checkpointBoundary();var restored=new LinkedHashMap<UUID,Actor>();var plans=new ArrayList<Runnable>();
            for(var image:saved){var body=(Marker)links.get(image.body);var author=(LivingEntity)links.get(image.author);var actor=image.actor;
                restored.put(body.getUUID(),actor);plans.add(()->{actor.body=body;actor.author=author;actor.lifetime=image.lifetime;actor.pending=image.pending;});}
            return ()->{plans.forEach(Runnable::run);actors=restored;acceptedProjectiles=new HashSet<>(projectiles);lastTick=clock.last(game);};
        }
    }
    boolean cloneRecognizes(Entity entity){return actors.containsKey(entity.getUUID());}
    /** Logical marker is the single x2/x4 family boundary. Its retained vanilla transport must
     * be excluded from cloning, otherwise one activation would multiply twice. */
    boolean protectedCloneActor(Entity entity){return acceptedProjectiles.contains(entity.getUUID());}
    LbcCloneSkills.ActorImage cloneImage(Entity source){
        var actor=actors.get(source.getUUID());if(actor==null)return null;
        // A native body copy forks FUTURE generation. Already accepted terrain belongs to its
        // original causal producer and is never replayed merely by cloning the marker.
        int lifetime=actor.lifetime;var author=currentAuthor(actor);var cause=actor.cause;var epoch=actor.epoch;
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){
                var linked=family.get(author);var owner=linked instanceof LivingEntity living?living:author;
                actors.put(clone.getUUID(),new Actor((Marker)clone,owner,cause,epoch,lifetime));
            }
            public void rollback(Entity clone){var value=actors.get(clone.getUUID());if(value!=null)remove(value,true);}
        };
    }
}
