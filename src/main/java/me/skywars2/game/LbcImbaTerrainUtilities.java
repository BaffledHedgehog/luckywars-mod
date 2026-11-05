package me.skywars2.game;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;

/** Finite source Dirter fills and instant Bastion share the native fair terrain queue. */
public final class LbcImbaTerrainUtilities {
    public enum Kind { DIRTER, BASTION, DEBRIS, END_PORTAL, NETHER_PORTAL }
    @FunctionalInterface public interface Policy {boolean activation(ServerPlayer author,Kind kind,Vec3 origin);}
    private static final Identifier BASTION=Identifier.parse("lbc:bedrock_bastion"),DEBRIS=Identifier.parse("lbc:swastika"),END_PORTAL=Identifier.parse("lbc:end_portal"),NETHER_PORTAL=Identifier.parse("lbc:nether_portal");
    private static final TicketType TICKET=new TicketType(0,TicketType.FLAG_LOADING|TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private final GameController game;private final Policy policy;
    private final NavigableMap<Long,Job> jobs=new TreeMap<>();
    private volatile List<Cell> bastion=List.of(),debris=List.of(),endPortal=List.of(),netherPortal=List.of();private CompletableFuture<?> preparation;
    private long sequence,cloneCursor,completedVisits,completedWrites,completedJobs;private boolean restoring;
    private record Cell(BlockPos offset,MapManager.BlockSnapshot snapshot){}
    private record Fill(BlockPos min,int width,int height,int depth,Block block){long count(){return (long)width*height*depth;}}
    public LbcImbaTerrainUtilities(GameController game,Policy policy){this.game=game;this.policy=Objects.requireNonNull(policy);}
    public static void register(){Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.parse("skywars2:imba_terrain_utilities"),TICKET);LbcImbaTerrainUtilityItems.register();}
    public void prepare(){
        if(preparation!=null)return;var blocks=game.server().registryAccess().lookupOrThrow(Registries.BLOCK);
        preparation=game.maps().prepareSpellTemplates(List.of(BASTION,DEBRIS,END_PORTAL,NETHER_PORTAL)).thenAccept(parts->{
            for(var id:List.of(BASTION,DEBRIS,END_PORTAL,NETHER_PORTAL)){
                var cells=new ArrayList<Cell>();for(var part:parts.getOrDefault(id,List.of())){
                    var tag=part.template().save(new CompoundTag());
                    if(!tag.getListOrEmpty("entities").isEmpty()||!tag.getListOrEmpty("palettes").isEmpty())throw new IllegalStateException("Source terrain template needs a reviewed entity/palette adapter: "+id);
                    var palette=tag.getListOrEmpty("palette");for(var raw:tag.getListOrEmpty("blocks")){
                        var row=(CompoundTag)raw;var pos=row.getListOrEmpty("pos");var state=NbtUtils.readBlockState(blocks,(CompoundTag)palette.get(row.getIntOr("state",0)));
                        if(state.is(Blocks.STRUCTURE_VOID))continue;
                        cells.add(new Cell(new BlockPos(pos.getIntOr(0,0),pos.getIntOr(1,0),pos.getIntOr(2,0)),new MapManager.BlockSnapshot(state,row.getCompound("nbt").map(CompoundTag::copy).orElse(null))));
                    }
                }
                if(cells.isEmpty())throw new IllegalStateException("Missing exact source terrain template "+id);
                if(id.equals(BASTION))bastion=List.copyOf(cells);else if(id.equals(DEBRIS))debris=List.copyOf(cells);else if(id.equals(END_PORTAL))endPortal=List.copyOf(cells);else netherPortal=List.copyOf(cells);
            }
        });
    }
    public boolean ready(){return !bastion.isEmpty()&&!debris.isEmpty()&&!endPortal.isEmpty()&&!netherPortal.isEmpty();}
    public static boolean dirter(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"dirter",0)==1;}
    public static boolean bastion(ItemStack stack){
        if(stack.isEmpty())return false;var data=stack.get(DataComponents.ENTITY_DATA);if(data==null||data.type()!=EntityTypes.MARKER)return false;
        var tags=data.getUnsafe().getListOrEmpty("Tags");return tags.contains(StringTag.valueOf("bedrock_bastion"))&&tags.contains(StringTag.valueOf("instant"))&&tags.contains(StringTag.valueOf("slowraycast"));
    }
    public boolean canActivate(ServerPlayer p,Kind kind,Vec3 at){
        return !restoring&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&!game.maps().busy()&&p.isAlive()&&!p.isRemoved()&&!p.isSpectator()&&!game.isObserver(p)&&game.canUseGameplayItems(p)
                &&LbcDimensionItems.ordinary(p.level())&&LbcBedwarsRoleGuards.publication(p.level())&&!game.items().actionsBlocked(p)&&!game.items().wands().inputTimeStopped(p)
                &&(kind==Kind.DIRTER||ready())&&policy.activation(p,kind,at);
    }
    public boolean debrisPlaced(ServerPlayer player,Vec3 origin){if(!canActivate(player,Kind.DEBRIS,origin))return false;enqueue(player,Kind.DEBRIS,origin);return true;}
    private Job enqueue(ServerPlayer p,Kind kind,Vec3 origin){var job=new Job(++sequence,p,kind,origin,game.items().indirectKills().wildCause(p));jobs.put(job.id,job);game.items().stonekivi().queuedWork(job);return job;}
    /** Native BlockItem.place already accepted the physical placement and owns its sole used-hand debit. */
    public boolean dirterPlaced(ServerPlayer p,InteractionHand hand,ItemStack original,Vec3 playerOrigin){
        if(!dirter(original)||restoring)return false;enqueue(p,Kind.DIRTER,playerOrigin);return true;
    }
    /** Called only after the custom native egg has resolved the ordinary spawn-egg target position. */
    public boolean useBastion(ServerPlayer p,InteractionHand hand,Vec3 origin){return useTemplate(p,hand,origin,Kind.BASTION);}
    public boolean useTemplate(ServerPlayer p,InteractionHand hand,Vec3 origin,Kind kind){
        var stack=p.getItemInHand(hand);boolean matches=kind==Kind.BASTION?bastion(stack):LbcImbaPlaceableItems.templateKind(stack)==kind;
        if(!matches||!canActivate(p,kind,origin))return false;
        enqueue(p,kind,origin);stack.consume(1,p);p.inventoryMenu.broadcastChanges();return true;
    }
    private static boolean templateActor(Kind kind){return kind==Kind.BASTION||kind==Kind.END_PORTAL||kind==Kind.NETHER_PORTAL;}
    private static Fill fill(BlockPos a,BlockPos b,Block block){return new Fill(new BlockPos(Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ())),Math.abs(a.getX()-b.getX())+1,Math.abs(a.getY()-b.getY())+1,Math.abs(a.getZ()-b.getZ())+1,block);}
    private static List<Fill> dirterFills(Vec3 origin){var center=BlockPos.containing(origin);int x=center.getX(),y=center.getY(),z=center.getZ();return List.of(
            fill(new BlockPos(x+4,y-1,z+4),new BlockPos(x-4,y-1,z-4),Blocks.GRASS_BLOCK),
            fill(new BlockPos(x+4,y-2,z+4),new BlockPos(x-4,y-5,z-4),Blocks.DIRT),
            fill(new BlockPos(x+4,y-6,z+4),new BlockPos(x-4,50,z-4),Blocks.STONE),
            fill(new BlockPos(x+4,49,z+4),new BlockPos(x-4,1,z-4),Blocks.DEEPSLATE),
            fill(new BlockPos(x+4,0,z+4),new BlockPos(x-4,0,z-4),Blocks.BEDROCK));}
    private static List<Fill> boundedFills(Vec3 origin,ServerLevel level){var result=new ArrayList<Fill>();for(var fill:dirterFills(origin)){int min=Math.max(fill.min.getY(),level.getMinY()),max=(int)Math.min((long)fill.min.getY()+fill.height-1,level.getMaxY()-1L);if(min<=max)result.add(new Fill(new BlockPos(fill.min.getX(),min,fill.min.getZ()),fill.width,max-min+1,fill.depth,fill.block));}return List.copyOf(result);}
    private final class Job implements LbcStonekivi.Work {
        final long id;ServerPlayer author;final ServerLevel level;final String epoch;final Kind kind;final Vec3 origin;final LbcIndirectKills.Owner cause;final List<Fill> fills;
        final Map<ChunkPos,CompletableFuture<?>> tickets=new LinkedHashMap<>();
        int phase,mask;long cursor,visits,writes;boolean closed,actorLive,waiting;
        Job(long id,ServerPlayer author,Kind kind,Vec3 origin,LbcIndirectKills.Owner cause){this.id=id;this.author=author;level=author.level();epoch=game.items().epoch();this.kind=kind;this.origin=origin;this.cause=cause;fills=kind==Kind.DIRTER?boundedFills(origin,level):List.of();actorLive=templateActor(kind);}
        public boolean ready(){return closed||!restoring&&!game.items().timeStop().stopped(level)&&!game.items().timeStop().frozen(author);}
        public boolean snapshotIdle(){return true;}
        public boolean postgame(){return true;} // Admission was already charged; finite terrain must drain.
        boolean resident(BlockPos at){
            if(level.hasChunkAt(at))return true;var key=new ChunkPos(at.getX()>>4,at.getZ()>>4);var future=tickets.get(key);
            if(future!=null&&(future.isCancelled()||future.isCompletedExceptionally())){level.getChunkSource().removeTicketWithRadius(TICKET,key,0);tickets.remove(key);}
            if(!tickets.containsKey(key)&&tickets.size()<8)tickets.put(key,level.getChunkSource().addTicketAndLoadWithRadius(TICKET,key,0));waiting=true;return false;
        }
        void release(){for(var key:tickets.keySet())level.getChunkSource().removeTicketWithRadius(TICKET,key,0);tickets.clear();}
        public boolean step(){
            if(closed)return true;if(restoring||game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()||!ready())return false;
            if(!game.items().currentEpoch(epoch)){cancel();return true;}
            game.items().stonekivi().beginNativeWork();try{
                waiting=false;actorLive=false;
                long deadline=Math.min(EffectTerrain.deadlineNanos(),System.nanoTime()+350_000);
                for(int quantum=0;quantum<128&&!closed&&!waiting&&System.nanoTime()<deadline;quantum++){
                    if(kind==Kind.DIRTER)advanceFill();else advanceTemplate();
                }
                // Completed chunk leases may be released between finite publication quanta.
                if(!waiting)release();return closed;
            }finally{game.items().stonekivi().endNativeWork();}
        }
        boolean write(BlockPos at,MapManager.BlockSnapshot snapshot){
            if(!level.isInValidBounds(at))return true;if(!resident(at))return false;
            visits++;if(game.items().writeOwnedTerrain(level,at,snapshot.state(),cause)){
                if(snapshot.entity()!=null)game.items().indirectKills().capturedWorldChanges(level,cause,()->game.maps().restoreSnapshot(level,at,snapshot,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS));writes++;
            }return true;
        }
        void advanceFill(){
            if(phase>=fills.size()){finish();return;}var fill=fills.get(phase);
            if(cursor>=fill.count()){cursor=0;phase++;return;}
            int x=(int)(cursor%fill.width),z=(int)(cursor/fill.width%fill.depth),y=(int)(cursor/(fill.width*fill.depth));
            if(write(fill.min.offset(x,y,z),new MapManager.BlockSnapshot(fill.block.defaultBlockState(),null)))cursor++;
        }
        void advanceTemplate(){
            var template=switch(kind){case DEBRIS->debris;case END_PORTAL->endPortal;case NETHER_PORTAL->netherPortal;default->bastion;};
            if(cursor>=template.size()){finish();return;}var cell=template.get((int)cursor);
            if(write(BlockPos.containing(switch(kind){case DEBRIS->origin.add(-5,-1,-5);case END_PORTAL->origin.add(-2,0,-2);case NETHER_PORTAL->origin.add(-1.5,-1,0);default->origin.add(-8,-1,-8);}).offset(cell.offset),cell.snapshot))cursor++;
        }
        void finish(){
            if(kind==Kind.DIRTER||kind==Kind.DEBRIS)LbcOverload.sound(level,origin,"minecraft:block.stone.place",4,1,0,0);
            else if(kind==Kind.BASTION){LbcOverload.sound(level,origin,"minecraft:block.stone.break",4,1,0,0);level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.STONE.defaultBlockState()),origin.x,origin.y+4,origin.z,1000,7,7,7,0);}
            closed=true;actorLive=false;completedVisits+=visits;completedWrites+=writes;completedJobs++;release();jobs.remove(id,this);
        }
        public void cancel(){closed=true;actorLive=false;release();jobs.remove(id,this);}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            var player=graph.links.key(author);int layer=phase,flags=mask;long at=cursor,seen=visits,changed=writes;boolean ended=closed,live=actorLive;
            return links->{MatchAuthorityCheckpoint.world(game,level);var owner=(ServerPlayer)links.get(player);return ()->{release();author=owner;phase=layer;mask=flags;cursor=at;visits=seen;writes=changed;closed=ended;actorLive=live;waiting=false;};};
        }
    }
    public record JobView(long id,Kind kind,UUID author,Vec3 origin,int phase,long cursor,long visits,long writes,boolean cloneable,int cloneMask){}
    public record State(int jobs,int actors,long visits,long writes,long finished){}
    public State state(){long visits=completedVisits,writes=completedWrites;int actors=0;for(var job:jobs.values()){visits+=job.visits;writes+=job.writes;if(job.actorLive)actors++;}return new State(jobs.size(),actors,visits,writes,completedJobs);}
    public List<JobView> views(){return jobs.values().stream().map(j->new JobView(j.id,j.kind,j.author.getUUID(),j.origin,j.phase,j.cursor,j.visits,j.writes,j.actorLive,j.mask)).toList();}
    public int templateCells(){return bastion.size();}
    public record Bounds(BlockPos min,BlockPos max){}
    public Bounds bastionBounds(Vec3 origin){if(!ready())throw new IllegalStateException("Prepare exact Bastion first");int x=Integer.MAX_VALUE,y=x,z=x,maxX=Integer.MIN_VALUE,maxY=maxX,maxZ=maxX;var base=BlockPos.containing(origin.add(-8,-1,-8));for(var cell:bastion){var at=base.offset(cell.offset);x=Math.min(x,at.getX());y=Math.min(y,at.getY());z=Math.min(z,at.getZ());maxX=Math.max(maxX,at.getX());maxY=Math.max(maxY,at.getY());maxZ=Math.max(maxZ,at.getZ());}return new Bounds(new BlockPos(x,y,z),new BlockPos(maxX,maxY,maxZ));}
    /** Canonical shared clone owner supplies all remaining budgets and the proximity holder. */
    record CloneResult(int scanned,int operations,int bodies){}
    CloneResult cloneNearby(ServerPlayer holder,int skill,int scanBudget,int bodyBudget,long deadline){
        int count=CloneVirtualAbilityStates.count(skill),seen=0,made=0,operations=0;
        if(skill!=15&&skill!=17||restoring||bodyBudget<count)return new CloneResult(0,0,0);
        int available=Math.min(scanBudget,jobs.size());long last=sequence;
        while(seen<available&&bodyBudget-made>=count&&System.nanoTime()<deadline&&!jobs.isEmpty()){
            var entry=jobs.higherEntry(cloneCursor);if(entry==null||entry.getKey()>last)entry=jobs.firstEntry();cloneCursor=entry.getKey();seen++;
            var source=entry.getValue();if(!source.actorLive||source.closed||!templateActor(source.kind)||!game.items().currentEpoch(source.epoch)||game.items().timeStop().frozen(source.author)||!CloneVirtualAbilityStates.near(game,holder,source.level,source.origin,source.mask,skill))continue;
            var positions=CloneVirtualAbilityStates.positions(holder,source.level,source.origin,skill);if(positions==null)continue;int flags=source.mask|CloneVirtualAbilityStates.bit(skill);
            for(var origin:positions){var job=new Job(++sequence,source.author,source.kind,origin,source.cause);job.mask=flags;jobs.put(job.id,job);game.items().stonekivi().queuedWork(job);}source.mask=flags;made+=count;operations++;
        }return new CloneResult(seen,operations,made);
    }
    public void prepareRestart(){} // Every admitted operation is finite and must drain unchanged.
    public boolean restartSettled(){return jobs.isEmpty();}
    public void stop(){for(var job:List.copyOf(jobs.values()))job.cancel();jobs.clear();sequence=cloneCursor=completedVisits=completedWrites=completedJobs=0;restoring=false;}
    public void prepareFrozen(){restoring=true;for(var job:jobs.values())job.release();jobs.clear();}
    public void publishFrozen(){restoring=false;}
    public void cancelFrozen(){stop();}
    public void checkpointBoundary(){if(!game.server().isSameThread())throw new IllegalStateException("Native terrain utility boundary required");}
    MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){checkpointBoundary();var saved=List.copyOf(jobs.values());saved.forEach(graph::capture);long ids=sequence,cursor=cloneCursor,seen=completedVisits,written=completedWrites,finished=completedJobs;return links->()->{jobs.clear();for(var job:saved)jobs.put(job.id,job);sequence=ids;cloneCursor=cursor;completedVisits=seen;completedWrites=written;completedJobs=finished;};}
}
