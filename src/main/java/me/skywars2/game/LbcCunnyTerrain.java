package me.skywars2.game;

import java.util.*;
import java.lang.reflect.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

/** Source virtual markers. Every persistent cursor and RNG belongs to the shared work graph. */
public final class LbcCunnyTerrain {
    private static final Identifier[] IDS={id("sob"),id("angry"),id("splurt"),id("sweat")};
    private static final TicketType TICKET=new TicketType(0,TicketType.FLAG_LOADING|TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private static final String[] WOODS={"oak","birch","spruce","dark_oak","pale_oak","jungle","acacia","warped","crimson","cherry","mangrove","bamboo"};
    private static final net.minecraft.tags.TagKey<Block> AIRS=net.minecraft.tags.TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final int[][] DIRECTIONS={{1,0},{-1,0},{0,1},{0,-1}};
    private final GameController game;
    private final Set<Job> jobs=Collections.newSetFromMap(new IdentityHashMap<>());
    private volatile List<List<Cell>> templates=List.of();
    private volatile List<List<String>> pasta=List.of();
    private CompletableFuture<?> preparation;
    private boolean retiring;
    private final NavigableMap<Long,Mark> actors=new TreeMap<>();
    private long actorSequence,cloneCursor;
    private final class Mark {final long id;final Job job;int mask;boolean alive=true;Op op;Mark(Job job){this(job,++actorSequence,0,true);}Mark(Job job,long id,int mask,boolean alive){this.job=job;this.id=id;this.mask=mask;this.alive=alive;}}
    private Mark marker(Op op){return op instanceof Grass g?g.mark:op instanceof Jungle j?j.mark:op instanceof Spread a?a.mark:null;}
    private void track(Op op){var mark=marker(op);if(mark!=null&&mark.alive){mark.op=op;actors.put(mark.id,mark);}}
    private void untrack(Op op){var mark=marker(op);if(mark!=null&&mark.op==op)actors.remove(mark.id);}
    /** Source defaults: outer input is MAG, autonomous grass/jungle have no extra stopper gate. */
    public interface Suppression { boolean blocked(ServerPlayer author,ServerLevel level,Vec3 at,boolean jungle); }
    private Suppression suppression=(author,level,at,jungle)->false;
    public LbcCunnyTerrain(GameController game){this.game=game;}
    private static Identifier id(String path){return Identifier.fromNamespaceAndPath("lbc",path);}
    public static void register(){Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.parse("skywars2:cunny_terrain"),TICKET);LbcCunnyJungleItem.register();}
    public void suppression(Suppression policy){suppression=Objects.requireNonNull(policy);}
    private record Cell(BlockPos offset,MapManager.BlockSnapshot value){}
    public void prepare(){
        if(preparation!=null)return;
        var blocks=game.server().registryAccess().lookupOrThrow(Registries.BLOCK);
        preparation=game.maps().prepareSpellTemplates(List.of(IDS)).thenAccept(parts->{
            var result=new ArrayList<List<Cell>>();
            for(var id:IDS){var rows=new ArrayList<Cell>();
                for(var part:parts.getOrDefault(id,List.of())){
                    var nbt=part.template().save(new CompoundTag());var palette=nbt.getListOrEmpty("palette");
                    if(!nbt.getListOrEmpty("entities").isEmpty()||!nbt.getListOrEmpty("palettes").isEmpty())throw new IllegalStateException("Cunny source template format: "+id);
                    for(var raw:nbt.getListOrEmpty("blocks")){var row=(CompoundTag)raw;var pos=row.getListOrEmpty("pos");
                        var state=NbtUtils.readBlockState(blocks,(CompoundTag)palette.get(row.getIntOr("state",0)));
                        if(!state.is(Blocks.STRUCTURE_VOID))rows.add(new Cell(new BlockPos(pos.getIntOr(0,0),pos.getIntOr(1,0),pos.getIntOr(2,0)),new MapManager.BlockSnapshot(state,row.getCompound("nbt").map(CompoundTag::copy).orElse(null))));
                    }
                }if(rows.isEmpty())throw new IllegalStateException("Missing Cunny template "+id);result.add(List.copyOf(rows));
            }templates=List.copyOf(result);
        });
        try(var stream=LbcCunnyTerrain.class.getResourceAsStream("/data/lbc/cunny/pasta.json")){
            if(stream==null)throw new IllegalStateException("Missing source Cunny pasta");
            var array=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();
            var rows=new ArrayList<List<String>>();for(var row:array){var lines=new ArrayList<String>();row.getAsJsonArray().forEach(s->lines.add(s.getAsString()));if(lines.size()!=4)throw new IllegalStateException("Cunny sign row");rows.add(List.copyOf(lines));}
            if(rows.size()!=100)throw new IllegalStateException("Cunny needs source100 pasta");pasta=List.copyOf(rows);
        }catch(java.io.IOException error){throw new IllegalStateException(error);}
    }
    public boolean ready(){return templates.size()==4&&pasta.size()==100;}
    private boolean admitted(ServerPlayer p){return !retiring&&game.playing()&&!game.maps().busy()&&p.isAlive()&&!p.isRemoved()&&!p.isSpectator()&&!game.isObserver(p)&&game.canUseGameplayItems(p)&&LbcDimensionItems.ordinary(p.level())&&LbcBedwarsRoleGuards.publication(p.level())&&!game.items().wands().inputTimeStopped(p);}
    private Job start(ServerPlayer p){var job=new Job(p);jobs.add(job);game.items().stonekivi().queuedWork(job);return job;}
    public boolean grass(ServerPlayer p){if(!admitted(p))return false;var job=start(p);job.claim(p.blockPosition());return true;}
    public boolean teleport(ServerPlayer p){if(!admitted(p))return false;var job=start(p);job.ops.add(new Teleport(job.range(-1_000_000,1_000_000),job.range(-1_000_000,1_000_000)));return true;}
    public boolean jungle(ServerPlayer p){if(!admitted(p)||!ready())return false;var job=start(p);for(var d:DIRECTIONS)job.add(new Jungle(p.getEyePosition().add(5*d[0],0,5*d[1]),0,0,new Mark(job)));return true;}
    public boolean castJungle(ServerPlayer p,InteractionHand hand){
        if(hand!=InteractionHand.MAIN_HAND||ItemData.intValue(p.getItemInHand(hand),"cunny_slab",0)!=1||!jungle(p))return false;
        for(int i=0;i<p.getInventory().getContainerSize();i++)if(ItemData.intValue(p.getInventory().getItem(i),"cunny_slab",0)==1)p.getInventory().setItem(i,net.minecraft.world.item.ItemStack.EMPTY);
        p.inventoryMenu.broadcastChanges();return true;
    }
    private sealed interface Op permits Grass,Teleport,Platform,Jungle,Tree,Template,Emit,Corrector,Spread,Schedule{}
    private record Grass(BlockPos at,int due,Mark mark)implements Op{}
    private record Teleport(int x,int z)implements Op{}
    private record Platform(BlockPos center,int cursor,boolean changed)implements Op{}
    private record Jungle(Vec3 at,int phase,int due,Mark mark)implements Op{}
    private record Tree(BlockPos at)implements Op{}
    private record Template(BlockPos origin,int kind,Rotation rotation,int cursor)implements Op{}
    private record Emit(BlockPos at,MapManager.BlockSnapshot cell)implements Op{}
    private record Corrector(BlockPos at,int wood,int rot,int front,int back,int phase)implements Op{}
    private record Spread(Vec3 at,int attempts,Mark mark)implements Op{}
    private record Schedule(BlockPos at,Object type,int delay,net.minecraft.world.ticks.TickPriority priority)implements Op{}
    private final class Job implements LbcStonekivi.Work {
        ServerPlayer author;final ServerLevel level;final String epoch;final LbcIndirectKills.Owner cause;
        RandomSource random;ArrayDeque<Op> ops=new ArrayDeque<>();Set<BlockPos> claims=new HashSet<>();
        final Map<ChunkPos,CompletableFuture<?>> tickets=new LinkedHashMap<>();boolean closed,blocked,waitingChunk;
        long visits,writes;int logicalTick,lastStepTick=Integer.MIN_VALUE;
        Job(ServerPlayer p,Job source){author=p;level=source.level;epoch=source.epoch;cause=source.cause;random=RandomSource.create(source.random.nextLong());}
        Job(ServerPlayer p){author=p;level=p.level();epoch=game.items().epoch();cause=game.items().indirectKills().wildCause(p);random=RandomSource.create(p.getRandom().nextLong());}
        int range(int min,int max){return min+random.nextInt(max-min+1);}
        void add(Op op){ops.add(op);track(op);}
        void push(Op... values){for(int i=values.length-1;i>=0;i--){ops.addFirst(values[i]);track(values[i]);}}
        void claim(BlockPos at){claim(at,new Mark(this));}
        void claim(BlockPos at,Mark mark){if(level.isInValidBounds(at)&&claims.add(at.immutable()))add(new Grass(at.immutable(),logicalTick+1,mark));}
        boolean resident(BlockPos at){if(level.hasChunkAt(at))return true;var chunk=new ChunkPos(at.getX()>>4,at.getZ()>>4);var pending=tickets.get(chunk);if(pending!=null&&(pending.isCancelled()||pending.isCompletedExceptionally())){level.getChunkSource().removeTicketWithRadius(TICKET,chunk,0);tickets.remove(chunk);}if(!tickets.containsKey(chunk)&&tickets.size()<32)tickets.put(chunk,level.getChunkSource().addTicketAndLoadWithRadius(TICKET,chunk,0));blocked=waitingChunk=true;return false;}
        void release(){for(var chunk:tickets.keySet())level.getChunkSource().removeTicketWithRadius(TICKET,chunk,0);tickets.clear();}
        public boolean ready(){
            if(closed)return true;
            if(game.items().timeStop().stopped(level)||game.items().timeStop().frozen(author))return false;
            if(lastStepTick!=game.server().getTickCount())return true;
            var head=ops.peek();
            return !(head instanceof Grass g&&g.due>logicalTick||head instanceof Jungle j&&j.due>logicalTick);
        }
        public boolean snapshotIdle(){return true;}
        public boolean step(){
            if(game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped(level)||game.items().timeStop().frozen(author))return false;
            if(closed)return true;if(!game.items().currentEpoch(epoch)||!game.playing()){cancel();return true;}if(!ready())return false;
            game.items().stonekivi().beginNativeWork();try{
                if(lastStepTick!=game.server().getTickCount()){logicalTick++;lastStepTick=game.server().getTickCount();}
                blocked=waitingChunk=false;long deadline=Math.min(EffectTerrain.deadlineNanos(),System.nanoTime()+350_000);
                for(int n=0;n<128&&!ops.isEmpty()&&!blocked&&System.nanoTime()<deadline;n++){
                    var op=ops.remove();untrack(op);visits++;
                    if(retiring&&(op instanceof Grass||op instanceof Jungle j&&j.phase==0||op instanceof Spread)){if(op instanceof Grass g)claims.remove(g.at);continue;}
                    execute(op);
                }
                // Retain only tickets still needed by deferred writes; each load batch has a finite lease.
                if(!waitingChunk)release();
                if(ops.isEmpty()){closed=true;jobs.remove(this);release();return true;}return false;
            }finally{game.items().stonekivi().endNativeWork();}
        }
        boolean write(BlockPos at,MapManager.BlockSnapshot cell){
            if(!level.isInValidBounds(at))return false;if(!resident(at))return false;
            boolean result=game.items().writeOwnedTerrain(level,at,cell.state(),cause);
            if(result&&cell.entity()!=null){game.items().indirectKills().capturedWorldChanges(level,cause,()->game.maps().restoreSnapshot(level,at,cell,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS));}
            if(result)writes++;return result;
        }
        void execute(Op op){
            if(op instanceof Grass g){
                if(g.due>logicalTick){add(g);blocked=true;return;}
                var at=g.at;if(!resident(at)||!resident(at.above())){push(g);return;}
                if(suppression.blocked(author,level,Vec3.atCenterOf(at),false)){push(g);blocked=true;return;}
                claims.remove(at);
                if(level.getBlockState(at).is(AIRS)){claim(at.below(),g.mark);return;}
                if(!level.getBlockState(at.above()).is(AIRS)){claim(at.above(),g.mark);return;}
                if(level.getBlockState(at).is(Blocks.GRASS_BLOCK))return;
                if(write(at,cell(Blocks.GRASS_BLOCK.defaultBlockState()))){for(var d:DIRECTIONS)claim(at.offset(d[0],0,d[1]));}
            }else if(op instanceof Teleport t){
                var at=new BlockPos(t.x,100,t.z);if(!resident(at)){push(t);return;}
                if(!author.isAlive()||author.isRemoved()||author.level()!=level)return;
                if(!author.teleportTo(level,t.x,320,t.z,Set.of(),author.getYRot(),author.getXRot(),true)){push(t);blocked=true;return;}
                game.items().indirectKills().teleported(author,author,true);
                author.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,1200,0,false,false));
                for(int i=0;i<23;i++)for(var item:game.lbc().rollTable(level,author.position(),author,"lbc:imba/cunny_slabs"))author.getInventory().placeItemBackInInventory(item,net.minecraft.util.Prediction.SERVER_ONLY);
                push(new Platform(at,0,false));
            }else if(op instanceof Platform p){
                if(p.cursor==1089){if(!p.changed&&!retiring)push(new Platform(p.center,0,false));return;}
                var at=p.center.offset(p.cursor%33-16,0,p.cursor/33-16);if(!resident(at)){push(p);return;}boolean different=!level.getBlockState(at).is(Blocks.QUARTZ_BLOCK);boolean changed=write(at,cell(Blocks.QUARTZ_BLOCK.defaultBlockState()))&&different;
                if(blocked)push(p);else push(new Platform(p.center,p.cursor+1,p.changed||changed));
            }else if(op instanceof Emit e){write(e.at,e.cell);if(blocked)push(e);
            }else if(op instanceof Jungle j){
                if(j.due>logicalTick){add(j);blocked=true;return;}
                if(suppression.blocked(author,level,j.at,true)){push(j);blocked=true;return;}
                if(j.phase==0){push(new Tree(BlockPos.containing(j.at)),new Jungle(j.at,1,j.due,j.mark));return;}
                int roll=range(1,16),kind=roll<=4?0:(roll-5)%3+1;Rotation rot=roll<=7?Rotation.NONE:roll<=10?Rotation.CLOCKWISE_90:roll<=13?Rotation.COUNTERCLOCKWISE_90:Rotation.CLOCKWISE_180;
                var correctors=new ArrayList<Op>();correctors.add(new Template(BlockPos.containing(j.at),kind,rot,0));
                for(int i=0;i<4;i++)correctors.add(new Corrector(new BlockPos(net.minecraft.util.Mth.floor(j.at.x)+range(-10,10),net.minecraft.util.Mth.floor(j.at.y),net.minecraft.util.Mth.floor(j.at.z)+range(-10,10)),range(0,11),range(0,15),range(0,99),range(0,99),0));
                correctors.add(new Spread(j.at,0,j.mark));push(correctors.toArray(Op[]::new));
            }else if(op instanceof Template t){
                var rows=templates.get(t.kind);if(t.cursor>=rows.size())return;var row=rows.get(t.cursor);var at=t.origin.offset(StructureTemplate.transform(row.offset,Mirror.NONE,t.rotation,BlockPos.ZERO));
                var value=new MapManager.BlockSnapshot(row.value.state().rotate(t.rotation),row.value.entity());write(at,value);
                if(blocked)push(t);else push(new Template(t.origin,t.kind,t.rotation,t.cursor+1));
            }else if(op instanceof Corrector c){
                if(c.phase==0){if(!resident(c.at.below())){push(c);return;}if(level.getBlockState(c.at.below()).is(AIRS)){
                    var nbt=new CompoundTag();nbt.putString("id","minecraft:barrel");nbt.putString("LootTable","lbc:imba/cunny_slabs");write(c.at.below(),new MapManager.BlockSnapshot(Blocks.BARREL.defaultBlockState(),nbt));if(blocked){push(c);return;}}
                    push(new Corrector(c.at,c.wood,c.rot,c.front,c.back,1));return;
                }
                var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:"+WOODS[c.wood]+"_sign"));
                if(write(c.at,cell(block.defaultBlockState().setValue(StandingSignBlock.ROTATION,c.rot)))){
                    if(level.getBlockEntity(c.at)instanceof SignBlockEntity sign){
                        sign.setText(text(pasta.get(c.back)),SignTextSlot.FRONT);sign.setText(text(pasta.get(c.front)),SignTextSlot.BACK);sign.setWaxed(true);sign.setChanged();
                        level.sendBlockUpdated(c.at,level.getBlockState(c.at),level.getBlockState(c.at),Block.UPDATE_CLIENTS);
                    }
                }if(blocked)push(c);
            }else if(op instanceof Spread s){
                // Source chance1 kills ALL jungle markers, including those from other owners.
                var destination=LbcSurfaceTeleport.destination(level,s.at,30,300,random);
                if(range(1,100)==1){stopJungles();return;}
                if(s.mark.alive&&!retiring)add(new Jungle(destination==null?s.at:destination,0,logicalTick+1,s.mark));
            }else if(op instanceof Schedule s){
                if(!resident(s.at)){push(s);return;}
                if(s.type instanceof Block block&&level.getBlockState(s.at).is(block))level.scheduleTick(s.at,block,s.delay,s.priority);
                else if(s.type instanceof net.minecraft.world.level.material.Fluid fluid&&level.getFluidState(s.at).getType()==fluid)level.scheduleTick(s.at,fluid,s.delay,s.priority);
            }else if(op instanceof Tree t){
                // Vanilla's finite feature plans against a read-your-writes overlay; publication is queued.
                for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(!resident(t.at.offset(x*16,0,z*16))){push(t);return;}
                var overlay=new LinkedHashMap<BlockPos,BlockState>();var scheduled=new ArrayList<Op>();
                WorldGenLevel virtual=(WorldGenLevel)Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),new Class<?>[]{WorldGenLevel.class},(proxy,method,args)->{
                    String name=method.getName();
                    if(name.equals("getRandom"))return random;
                    if(name.equals("setBlock")){overlay.put(((BlockPos)args[0]).immutable(),(BlockState)args[1]);return true;}
                    if(name.equals("removeBlock")||name.equals("destroyBlock")){overlay.put(((BlockPos)args[0]).immutable(),Blocks.AIR.defaultBlockState());return true;}
                    if(name.equals("getBlockState"))return overlay.getOrDefault((BlockPos)args[0],level.getBlockState((BlockPos)args[0]));
                    if(name.equals("getFluidState"))return overlay.getOrDefault((BlockPos)args[0],level.getBlockState((BlockPos)args[0])).getFluidState();
                    if(name.equals("isStateAtPosition"))return ((java.util.function.Predicate<BlockState>)args[1]).test(overlay.getOrDefault((BlockPos)args[0],level.getBlockState((BlockPos)args[0])));
                    if(name.equals("isFluidAtPosition"))return ((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>)args[1]).test(overlay.getOrDefault((BlockPos)args[0],level.getBlockState((BlockPos)args[0])).getFluidState());
                    if(name.equals("addFreshEntity"))throw new IllegalStateException("Tree feature attempted entity publication");
                    if(name.equals("scheduleTick")){scheduled.add(new Schedule(((BlockPos)args[0]).immutable(),args[1],(int)args[2],args.length==4?(net.minecraft.world.ticks.TickPriority)args[3]:net.minecraft.world.ticks.TickPriority.NORMAL));return null;}
                    if(name.equals("blockUpdated")||name.equals("levelEvent")||name.equals("gameEvent")||name.equals("setCurrentlyGenerating"))return null;
                    if(method.isDefault())return InvocationHandler.invokeDefault(proxy,method,args==null?new Object[0]:args);
                    try{return method.invoke(level,args);}catch(InvocationTargetException error){throw error.getCause();}
                });
                var feature=level.registryAccess().lookupOrThrow(Registries.FEATURE).getValue(Identifier.parse("minecraft:mega_jungle_tree"));
                if(feature==null)throw new IllegalStateException("Missing native mega_jungle_tree");
                feature.place(virtual,level.getChunkSource().getGenerator(),random,t.at);
                var changes=new ArrayList<Op>();overlay.forEach((at,state)->changes.add(new Emit(at,cell(state))));changes.addAll(scheduled);push(changes.toArray(Op[]::new));
            }
        }
        public void cancel(){closed=true;release();ops.forEach(LbcCunnyTerrain.this::untrack);ops.clear();claims.clear();jobs.remove(this);}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            var owner=graph.links.key(author);var marks=new IdentityHashMap<Mark,Mark>();var saved=ops.stream().map(op->copyOp(op,this,marks)).toList();var claimed=Set.copyOf(claims);var rng=MatchRandomState.capture(random);boolean done=closed;long seen=visits,count=writes;int phase=logicalTick;
            return links->{var p=(ServerPlayer)links.get(owner);MatchAuthorityCheckpoint.world(game,level);return ()->{release();author=p;ops.forEach(LbcCunnyTerrain.this::untrack);var rebound=new IdentityHashMap<Mark,Mark>();ops=new ArrayDeque<>(saved.stream().map(op->copyOp(op,this,rebound)).toList());ops.forEach(LbcCunnyTerrain.this::track);claims=new HashSet<>(claimed);rng.restore(random);closed=done;blocked=waitingChunk=false;visits=seen;writes=count;logicalTick=phase;lastStepTick=game.server().getTickCount();if(!closed)jobs.add(this);};};
        }
    }
    private static MapManager.BlockSnapshot cell(BlockState state){return new MapManager.BlockSnapshot(state,null);}
    private static SignText text(List<String> rows){var result=SignText.EMPTY.asMutable();for(int i=0;i<4;i++)result.setLine(i,Component.literal(rows.get(i)));return result.setTextGlowing(true).setColor(net.minecraft.world.item.DyeColor.WHITE).asImmutable();}
    private void stopJungles(){for(var mark:List.copyOf(actors.values()))if(!(mark.op instanceof Grass)){mark.alive=false;actors.remove(mark.id);}for(var job:jobs)job.ops.removeIf(op->op instanceof Jungle j&&j.phase==0||op instanceof Spread);}
    /** Stop future expansion while retaining all accepted finite tree/template/sign/platform writes. */
    public void prepareRestart(){retiring=true;actors.values().forEach(mark->mark.alive=false);actors.clear();for(var job:jobs){job.ops.removeIf(op->op instanceof Grass||op instanceof Jungle j&&j.phase==0||op instanceof Spread);job.claims.clear();}}
    public boolean restartSettled(){return jobs.stream().allMatch(j->j.closed||j.ops.isEmpty());}
    public void stop(){for(var job:List.copyOf(jobs))job.cancel();jobs.clear();actors.clear();actorSequence=cloneCursor=0;retiring=false;}
    public void prepareFrozen(){for(var job:List.copyOf(jobs))job.release();jobs.clear();actors.clear();}
    public void publishFrozen(){}
    public void cancelFrozen(){stop();}
    public void checkpointBoundary(){if(!game.server().isSameThread())throw new IllegalStateException("Cunny terrain checkpoint requires native thread");}
    MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){checkpointBoundary();var saved=List.copyOf(jobs);saved.forEach(graph::capture);boolean retired=retiring;long sequence=actorSequence,cursor=cloneCursor;return links->()->{jobs.clear();jobs.addAll(saved);actors.clear();for(var job:saved)job.ops.forEach(this::track);retiring=retired;actorSequence=sequence;cloneCursor=cursor;};}
    public record View(int jobs,int grass,int jungle,int pending,long visits,long writes){}
    public View view(){int grass=0,jungle=0,pending=0;long visits=0,writes=0;for(var job:jobs){grass+=job.claims.size();jungle+=(int)job.ops.stream().filter(op->op instanceof Jungle||op instanceof Spread).count();pending+=job.ops.size();visits+=job.visits;writes+=job.writes;}return new View(jobs.size(),grass,jungle,pending,visits,writes);}

    private Op copyOp(Op op,Job job,IdentityHashMap<Mark,Mark> marks){
        var old=marker(op);if(old==null)return op;var mark=marks.computeIfAbsent(old,m->new Mark(job,m.id,m.mask,m.alive));
        return op instanceof Grass g?new Grass(g.at,g.due,mark):op instanceof Jungle j?new Jungle(j.at,j.phase,j.due,mark):new Spread(((Spread)op).at,((Spread)op).attempts,mark);
    }
    private Vec3 position(Op op){return op instanceof Grass g?Vec3.atCenterOf(g.at):op instanceof Jungle j?j.at:((Spread)op).at;}
    record CloneResult(int scanned,int operations,int bodies){}
    record ActorView(long id,UUID author,LbcIndirectKills.Owner cause,boolean jungle,int mask,Vec3 position){}
    List<ActorView> actorViews(int limit){var result=new ArrayList<ActorView>();for(var mark:actors.values()){if(result.size()>=Math.max(0,limit))break;result.add(new ActorView(mark.id,mark.job.author.getUUID(),mark.job.cause,!(mark.op instanceof Grass),mark.mask,position(mark.op)));}return List.copyOf(result);}
    /** Canonical clone scanner supplies its remaining shared scan/body/time budgets. */
    CloneResult cloneNearby(ServerPlayer holder,int skill,int scanBudget,int bodyBudget,long deadline){
        int count=CloneVirtualAbilityStates.count(skill),seen=0,made=0,operations=0;
        if(retiring||skill!=15&&skill!=17||bodyBudget<count)return new CloneResult(0,0,0);
        // IDs form this family's stable marker index; at most scanBudget entries are visited.
        int available=Math.min(scanBudget,actors.size());
        while(seen<available&&bodyBudget-made>=count&&System.nanoTime()<deadline&&!actors.isEmpty()){
            var entry=actors.higherEntry(cloneCursor);if(entry==null)entry=actors.firstEntry();cloneCursor=entry.getKey();seen++;
            var mark=entry.getValue();var job=mark.job;var op=mark.op;var at=position(op);
            if(!mark.alive||job.closed||!game.items().currentEpoch(job.epoch)||game.items().timeStop().frozen(job.author)
                    ||!CloneVirtualAbilityStates.near(game,holder,job.level,at,mark.mask,skill))continue;
            var destinations=CloneVirtualAbilityStates.positions(holder,job.level,at,skill);if(destinations==null)continue;
            int flags=mark.mask|CloneVirtualAbilityStates.bit(skill);
            for(var destination:destinations){
                // Causal owner and source order belong to the original terrain author, never holder.
                var copy=new Job(job.author,job);jobs.add(copy);game.items().stonekivi().queuedWork(copy);
                copy.logicalTick=job.logicalTick;copy.lastStepTick=job.lastStepTick;
                var next=new Mark(copy);next.mask=flags;
                if(op instanceof Grass g){var cell=BlockPos.containing(destination);copy.claims.add(cell);copy.add(new Grass(cell,g.due,next));}
                else copy.add(new Jungle(destination,0,job.logicalTick+1,next));
            }
            mark.mask=flags;made+=count;operations++;
        }
        return new CloneResult(seen,operations,made);
    }
}
