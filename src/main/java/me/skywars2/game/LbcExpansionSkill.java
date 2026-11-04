package me.skywars2.game;

import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Skill24: native, directional source markers on the shared fair terrain queue. */
public final class LbcExpansionSkill {
    /** The source kills MAG/artifact-suppressed frontier markers; it does not suppress owner input. */
    public interface Policy { boolean cell(ServerPlayer owner,ServerLevel level,BlockPos position); }
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs_only"));
    private static final TicketType TICKET=new TicketType(0,TicketType.FLAG_LOADING|TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_SUPPRESS_DROPS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
    private static final int[][] DIRECTIONS={{1,0},{-1,0},{0,1},{0,-1}};
    public static void register(){net.minecraft.core.Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.parse("skywars2:expansion"),TICKET);}
    private final GameController game;
    private final Policy policy;
    private final LinkedHashMap<UUID,State> owners=new LinkedHashMap<>();
    private final Map<ServerLevel,Long2IntOpenHashMap> positions=new IdentityHashMap<>();
    private final Map<Chunk,Load> loading=new LinkedHashMap<>();
    private List<BlockState> palette;
    private Dispatcher queued;
    private boolean restoring;
    private int lastTick=Integer.MIN_VALUE,turn;
    private record Chunk(ServerLevel level,ChunkPos pos){}
    private static final class Load { final CompletableFuture<?> future;int touched;Load(CompletableFuture<?> f,int t){future=f;touched=t;} }
    private static final class Cell {
        final long position;final int direction;final boolean main;
        Cell(long position,int direction,boolean main){this.position=position;this.direction=direction;this.main=main;}
    }
    private static final class State {
        final UUID owner;final ServerLevel level;
        int score;boolean active,cooling,initial;
        long random;
        ArrayDeque<Cell> current=new ArrayDeque<>(),next=new ArrayDeque<>();
        State(ServerPlayer p){owner=p.getUUID();level=p.level();random=p.getRandom().nextLong();}
        State(State s){owner=s.owner;level=s.level;score=s.score;active=s.active;cooling=s.cooling;initial=s.initial;random=s.random;
            current.addAll(s.current);next.addAll(s.next);}
        int random(int bound){long u,r;do{random+=0x9e3779b97f4a7c15L;long z=random;
            z=(z^(z>>>30))*0xbf58476d1ce4e5b9L;z=(z^(z>>>27))*0x94d049bb133111ebL;
            u=(z^(z>>>31))>>>1;r=u%bound;}while(u-r+(bound-1)<0);return (int)r;}
    }
    public LbcExpansionSkill(GameController game,Policy policy){this.game=game;this.policy=Objects.requireNonNull(policy);}
    private boolean paused(ServerPlayer p){return game.items().timeStop().stopped(p.level())
            ||game.items().timeStop().frozen(p)||game.items().wands().inputTimeStopped(p);}
    private boolean admitted(ServerPlayer p){return p.isAlive()&&!p.isRemoved()&&!p.isSpectator()&&!game.isObserver(p)
            &&game.canUseGameplayItems(p)&&LbcDimensionItems.ordinary(p.level())&&game.lbc().hasSkill(p,24);}
    public void tick(){
        int tick=game.server().getTickCount();
        if(restoring||lastTick==tick||!game.playing()||game.maps().busy()||!game.server().tickRateManager().runsNormally())return;
        lastTick=tick;releaseUnusedTickets(tick);
        for(var id:List.copyOf(owners.keySet())){
            var p=game.server().getPlayerList().getPlayer(id);var s=owners.get(id);
            if(p==null||!admitted(p)||p.level()!=s.level)clear(id);
        }
        for(var p:game.server().getPlayerList().getPlayers()){
            if(!admitted(p)||paused(p))continue;
            var s=owners.computeIfAbsent(p.getUUID(),id->new State(p));
            if(!p.isShiftKeyDown()){
                if(!s.active&&!s.cooling)s.score=0;
                if(s.score>0&&s.random(2)==0)s.score--;
                if(s.score==0)s.cooling=false;
                if(s.active)end(s);
                continue;
            }
            if(s.active){
                // A selector generation must finish before the next source tick is admitted.
                // Shared CPU pressure stretches logical time, never truncates a frontier.
                if(!s.current.isEmpty())continue;
                var q=s.current;s.current=s.next;s.next=q;
                s.score++;
                if(s.current.isEmpty())end(s);
                p.level().sendParticles(ParticleTypes.REVERSE_PORTAL,p.getX(),p.getY()+1,p.getZ(),30,.1,.3,.1,1);
                if(s.score>=80&&s.random(5)==0)end(s);
            }else if(!s.cooling&&++s.score==60){
                s.active=s.cooling=true;s.initial=true;
                var origin=BlockPos.containing(p.getX(),p.getY()-.5,p.getZ());
                // A special constant-size initial transaction runs through the same queue.
                s.current.add(new Cell(origin.asLong(),0,true));addIndex(s.level,origin.asLong());
            }
        }
        ensureQueued();
    }
    private void ensureQueued(){
        if(queued==null&&owners.values().stream().anyMatch(s->s.active&&!s.current.isEmpty())){
            queued=new Dispatcher();game.items().stonekivi().queuedWork(queued);
        }
    }
    private final class Dispatcher implements LbcStonekivi.Work {
        boolean cancelled;
        @Override public boolean ready(){return !restoring&&(cancelled||snapshotIdle()||owners.values().stream().anyMatch(s->{
            var p=game.server().getPlayerList().getPlayer(s.owner);return s.active&&!s.current.isEmpty()&&p!=null&&!paused(p);}));}
        @Override public boolean snapshotIdle(){return owners.values().stream().noneMatch(s->s.active&&!s.current.isEmpty());}
        @Override public boolean step(){
            if(cancelled)return true;
            var states=new ArrayList<>(owners.values());
            for(int i=0;i<states.size();i++){
                var s=states.get(Math.floorMod(turn++,states.size()));var p=game.server().getPlayerList().getPlayer(s.owner);
                if(!s.active||s.current.isEmpty()||p==null||paused(p))continue;
                var cell=s.current.peek();boolean complete;
                try(var scope=game.items().spellSources().scope(game.items().spellSources().hostile(p,p,Set.of()),s.level)){
                    complete=s.initial?start(p,s,cell):propagate(p,s,cell);
                }
                if(complete){s.current.remove();removeIndex(s.level,cell.position);}
                break;
            }
            if(owners.values().stream().noneMatch(s->s.active&&!s.current.isEmpty())){if(queued==this)queued=null;return true;}
            return false;
        }
        @Override public void cancel(){cancelled=true;if(queued==this)queued=null;}
        @Override public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            boolean saved=cancelled;
            return links->()->{cancelled=saved;if(!cancelled)queued=this;};
        }
    }
    private boolean inside(ServerLevel level,BlockPos p){return p.getY()>=level.getMinY()&&p.getY()<level.getMaxY();}
    private boolean resident(ServerLevel level,BlockPos p){
        if(!inside(level,p)||level.hasChunkAt(p))return true;
        var key=new Chunk(level,new ChunkPos(p.getX()>>4,p.getZ()>>4));var old=loading.get(key);
        int tick=game.server().getTickCount();
        if(old!=null&&(old.future.isCancelled()||old.future.isCompletedExceptionally())){
            level.getChunkSource().removeTicketWithRadius(TICKET,key.pos,0);loading.remove(key);old=null;
        }
        if(old!=null)old.touched=tick;
        else if(loading.size()<8)loading.put(key,new Load(level.getChunkSource().addTicketAndLoadWithRadius(TICKET,key.pos,0),tick));
        return false;
    }
    private boolean start(ServerPlayer p,State s,Cell cell){
        var origin=BlockPos.of(cell.position);boolean ready=resident(s.level,origin);
        for(var d:DIRECTIONS)ready=resident(s.level,origin.offset(d[0],0,d[1]))&&ready;
        if(!ready)return false;
        if(!inside(s.level,origin)){s.initial=false;return true;}
        if(s.level.getBlockState(origin).is(AIRS))write(p,s.level,origin,new MapManager.BlockSnapshot(Blocks.STONE.defaultBlockState(),null));
        // Native Lucky Blocks must be normalized before even the first four copies. The source
        // proxy states were ordinary mushrooms and were normalized on their first marker tick.
        var snapshot=normalize(p,s.level,origin);
        for(var d:DIRECTIONS)copy(p,s.level,origin.offset(d[0],0,d[1]),snapshot);
        for(int d=0;d<4;d++)claim(s,origin,d,true,false,s.next);
        s.initial=false;return true;
    }
    private boolean propagate(ServerPlayer p,State s,Cell cell){
        var pos=BlockPos.of(cell.position);
        if(!inside(s.level,pos)||!policy.cell(p,s.level,pos))return true;
        var dir=DIRECTIONS[cell.direction];var forward=pos.offset(dir[0],0,dir[1]);var below=pos.below();
        var sideA=cell.direction<2?pos.offset(0,0,1):pos.offset(1,0,0);
        var sideB=cell.direction<2?pos.offset(0,0,-1):pos.offset(-1,0,0);
        boolean ready=resident(s.level,pos)&resident(s.level,forward)&resident(s.level,below);
        if(cell.main)ready=resident(s.level,sideA)&resident(s.level,sideB)&ready;
        if(!ready)return false;
        var snapshot=normalize(p,s.level,pos);
        // The same one-percent roll precedes every one of the eight direction functions.
        if(s.random(100)==0){snapshot=new MapManager.BlockSnapshot(palette().get(s.random(palette().size())),null);write(p,s.level,pos,snapshot);}
        if(cell.main){
            claim(s,forward,cell.direction,true,false,s.next);
            claim(s,sideA,cell.direction<2?2:0,false,true,s.next);
            claim(s,sideB,cell.direction<2?3:1,false,true,s.next);
            copy(p,s.level,forward,snapshot);copy(p,s.level,sideA,snapshot);copy(p,s.level,sideB,snapshot);
            copy(p,s.level,below,snapshot);
            if(s.random(10)==0)claim(s,below,cell.direction,true,true,s.next);
        }else{
            claim(s,forward,cell.direction,false,true,s.next);
            copy(p,s.level,forward,snapshot);
            if(s.random(10)==0)claim(s,below,cell.direction,false,true,s.next);
            // Unconditional downward COPY: only its marker is probabilistic.
            copy(p,s.level,below,snapshot);
        }
        return true;
    }
    private MapManager.BlockSnapshot normalize(ServerPlayer owner,ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);var kind=LbcLuckyBlocks.kind(state);
        Block normalized=kind==null?state.getBlock():switch(kind){case NORMAL->Blocks.RED_MUSHROOM_BLOCK;case NEKO->Blocks.BROWN_MUSHROOM_BLOCK;case LOLI->Blocks.MUSHROOM_STEM;};
        if(kind!=null||((state.is(Blocks.RED_MUSHROOM_BLOCK)||state.is(Blocks.BROWN_MUSHROOM_BLOCK)||state.is(Blocks.MUSHROOM_STEM))
                &&state.getValues().allMatch(v->Boolean.FALSE.equals(v.value())))){
            var all=normalized.defaultBlockState();
            for(var face:List.of(HugeMushroomBlock.DOWN,HugeMushroomBlock.UP,HugeMushroomBlock.NORTH,HugeMushroomBlock.SOUTH,HugeMushroomBlock.EAST,HugeMushroomBlock.WEST))all=all.setValue(face,true);
            var snapshot=new MapManager.BlockSnapshot(all,null);write(owner,level,pos,snapshot);return snapshot;
        }
        // Ordinary terrain stays entirely typed: only actual block entities need one snapshot.
        if(!state.hasBlockEntity())return new MapManager.BlockSnapshot(state,null);
        var saved=game.maps().snapshot(level,pos);var nbt=saved.entity();
        if(nbt!=null&&nbt.contains("Items")){
            nbt=nbt.copy();nbt.remove("Items");saved=new MapManager.BlockSnapshot(state,nbt);
            // Clear the live source before publishing any copy, preserving all other components.
            write(owner,level,pos,saved);
        }
        return saved;
    }
    private void copy(ServerPlayer owner,ServerLevel level,BlockPos target,MapManager.BlockSnapshot snapshot){
        if(inside(level,target)&&level.getBlockState(target).is(AIRS))write(owner,level,target,snapshot);
    }
    private void write(ServerPlayer owner,ServerLevel level,BlockPos target,MapManager.BlockSnapshot snapshot){
        if(level==game.maps().level())game.maps().recordBlockChange(target);
        game.items().indirectKills().supportRemoved(level,owner,target);
        game.maps().restoreSnapshot(level,target,snapshot,FLAGS);
    }
    private List<BlockState> palette(){
        if(palette!=null)return palette;
        var result=new ArrayList<BlockState>();
        try(var input=Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("skywars2/expansion_blocks.json"),"Missing Expansion source palette")){
            var json=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var value:json.getAsJsonArray("blocks")){
                var id=Identifier.parse(value.getAsString());
                var block=BuiltInRegistries.BLOCK.getOptional(id).orElseThrow(()->new IllegalStateException("Missing Expansion source block: "+id));
                result.add(block.defaultBlockState());
            }
            if(result.size()!=1106)throw new IllegalStateException("Expansion must retain every one of1106 source slots");
            if(json.get("append_missing_vanilla_defaults").getAsBoolean()){
                var seen=new HashSet<>(result);
                BuiltInRegistries.BLOCK.listElements().sorted(Comparator.comparing(h->h.key().identifier().toString()))
                        .filter(h->h.key().identifier().getNamespace().equals("minecraft"))
                        .map(h->h.value().defaultBlockState()).filter(seen::add).forEach(result::add);
            }
        }catch(java.io.IOException e){throw new IllegalStateException("Read Expansion finite palette",e);}
        return palette=List.copyOf(result);
    }
    private Long2IntOpenHashMap index(ServerLevel level){return positions.computeIfAbsent(level,k->new Long2IntOpenHashMap());}
    private void addIndex(ServerLevel level,long pos){index(level).addTo(pos,1);}
    private void removeIndex(ServerLevel level,long pos){var map=index(level);if(map.addTo(pos,-1)==1)map.remove(pos);}
    private void claim(State s,BlockPos pos,int direction,boolean main,boolean exclusive,ArrayDeque<Cell> queue){
        if(!inside(s.level,pos)||exclusive&&index(s.level).get(pos.asLong())>0)return;
        queue.add(new Cell(pos.asLong(),direction,main));addIndex(s.level,pos.asLong());
    }
    private void end(State s){
        if(!s.active)return;s.active=false;s.cooling=true;s.initial=false;
        for(var cell:s.current)removeIndex(s.level,cell.position);
        for(var cell:s.next)removeIndex(s.level,cell.position);
        s.current.clear();s.next.clear();
    }
    public int score(ServerPlayer p){var s=owners.get(p.getUUID());return s==null?0:s.score;}
    public void clear(ServerPlayer p){if(restoring||MatchPlayerCheckpoint.restoring(p))return;clear(p.getUUID());}
    private void clear(UUID id){var s=owners.remove(id);if(s!=null)end(s);}
    private void releaseUnusedTickets(int tick){
        for(var key:List.copyOf(loading.keySet()))if(loading.get(key).touched<tick-1){
            key.level.getChunkSource().removeTicketWithRadius(TICKET,key.pos,0);loading.remove(key);
        }
    }
    private void releaseTickets(){for(var key:loading.keySet())key.level.getChunkSource().removeTicketWithRadius(TICKET,key.pos,0);loading.clear();}
    public void stop(){if(queued!=null)queued.cancel();releaseTickets();owners.clear();positions.clear();turn=0;restoring=false;lastTick=Integer.MIN_VALUE;}
    public void prepareFrozen(){restoring=true;if(queued!=null)queued.cancel();releaseTickets();owners.clear();positions.clear();}
    public void publishFrozen(){restoring=false;lastTick=game.server().getTickCount();}
    Checkpoint checkpoint(){return new Checkpoint();}
    final class Checkpoint {
        final List<State> saved=owners.values().stream().map(State::new).toList();final int savedTurn=turn;
        MatchFieldCheckpoint.Plan prepare(Function<UUID,Entity> lookup){
            var restored=new LinkedHashMap<UUID,State>();for(var s:saved)restored.put(s.owner,new State(s));
            return new MatchFieldCheckpoint.Plan(Set.of(),()->{
                owners.clear();owners.putAll(restored);positions.clear();turn=savedTurn;
                for(var s:owners.values()){
                    for(var c:s.current)addIndex(s.level,c.position);
                    for(var c:s.next)addIndex(s.level,c.position);
                }
            });
        }
    }
    int frontier(ServerPlayer p){var s=owners.get(p.getUUID());return s==null?0:s.current.size()+s.next.size();}
    boolean active(ServerPlayer p){var s=owners.get(p.getUUID());return s!=null&&s.active;}
    int paletteSize(){return palette().size();}
    List<BlockState> paletteForCheck(){return palette();}
    /** Read-only diagnostic cursor; tests snapshot the next writes before actual registered ticks. */
    List<BlockPos> frontierPositionsForCheck(ServerPlayer p){
        var s=owners.get(p.getUUID());if(s==null)return List.of();
        var result=new LinkedHashSet<BlockPos>();s.current.forEach(c->result.add(BlockPos.of(c.position)));
        s.next.forEach(c->result.add(BlockPos.of(c.position)));return List.copyOf(result);
    }
}
