package me.skywars2.game;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import me.skywars2.mixin.ArenaChunkAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

/** Author's loaded-world axe harvest. Only copied palettes leave the server thread;
 * the shared terrain dispatcher owns admission, validation, native loot and publication. */
public final class LbcImbaAxe {
    public enum Trigger { BLOCK_BREAK, REAL_MELEE }
    private static final int WRITE_FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS;
    private static final ThreadLocal<Capture> DROP_CAPTURE=new ThreadLocal<>();
    private record Capture(ServerLevel level,List<ItemStack> output) { }
    /** A narrow ServerLevel.addFreshEntity HEAD adapter returns true when this accepts a native
     * inventory drop. The body is never published; the accepted stack joins this job's output. */
    public static boolean captureDrop(ServerLevel level,Entity body){
        var capture=DROP_CAPTURE.get();
        if(capture==null||capture.level()!=level||!(body instanceof ItemEntity item))return false;
        if(!item.getItem().isEmpty())capture.output().add(item.getItem().copy());
        item.discard();return true;
    }
    private static final int VALIDATIONS=64, OUTPUTS=4;
    private final GameController game;
    private LinkedHashSet<Harvest> jobs=new LinkedHashSet<>();
    // Chunk keys are weak: this optimization must never keep an unloaded chunk resident.
    private final IdentityHashMap<ServerLevel,WeakHashMap<LevelChunk,Map<Integer,Stamp>>> caches=new IdentityHashMap<>();
    private final AtomicLong workerGeneration=new AtomicLong();
    private CompletableFuture<List<Candidate>> worker;
    private Harvest workerOwner;
    private int callbacks;
    private boolean restoring;
    private long copiedSections,workerScans,cacheHits,writes,delivered,mergedStacks,publishedEntities;
    private long terrainGeneration;

    public LbcImbaAxe(GameController game){this.game=Objects.requireNonNull(game);}
    public static boolean recognized(ItemStack tool){return !tool.isEmpty()&&tool.is(LbcImbaAxeItem.TOKEN);}
    boolean effective(BlockState state){
        if(state.is(BlockTags.MINEABLE_WITH_AXE)||state.is(LbcConsumedBlock.BLOCK))return true;
        for(var kind:LbcLuckyBlocks.Kind.values())if(state.is(LbcLuckyBlocks.block(kind)))return true;
        return false;
    }
    private Set<Block> effectiveBlocks(){
        var result=Collections.newSetFromMap(new IdentityHashMap<Block,Boolean>());
        for(var block:BuiltInRegistries.BLOCK)if(effective(block.defaultBlockState()))result.add(block);
        return Set.copyOf(result);
    }
    private void thread(){if(!game.server().isSameThread())throw new IllegalStateException("Axe world authority is server-thread only");}
    private boolean admitted(ServerPlayer owner){return owner!=null&&owner.isAlive()&&!owner.isRemoved()
            &&game.playing()&&game.stage()!=GameController.Stage.ENDING&&!game.maps().busy()
            &&game.canUseGameplayItems(owner)&&!game.isObserver(owner)&&LbcDimensionItems.ordinary(owner.level());}
    /** Wire only from the confirmed real break/melee owner. No durability/mending callback. */
    public boolean activate(ServerPlayer owner,ItemStack actualTool,Trigger trigger){
        thread();Objects.requireNonNull(trigger);
        if(restoring||!recognized(actualTool)||!admitted(owner)||game.items().actionsBlocked(owner)
                ||game.items().wands().inputTimeStopped(owner))return false;
        callbacks++;
        try{
            var level=owner.level();var selected=new LinkedHashSet<Long>();
            // Snapshot the finite resident roster once. Palette copying is bounded in shared work.
            for(var holder:((ArenaChunkAccess)level.getChunkSource().chunkMap).skywars2$visibleChunks().values())
                if(holder.getLatestChunk() instanceof LevelChunk chunk)selected.add(chunk.getPos().pack());
            for(var existing:jobs)if(!existing.closed&&existing.owner.equals(owner.getUUID())&&existing.level==level){
                existing.refreshChunks.addAll(selected);
                if(existing.refreshTool==null){existing.refreshTool=actualTool.copy();existing.refreshEligible=effectiveBlocks();existing.refreshCause=game.items().indirectKills().wildCause(owner);}
                return true;
            }
            long[] chunks=new long[selected.size()];int index=0;for(long key:selected)chunks[index++]=key;
            var job=new Harvest(owner,level,chunks,actualTool.copy(),effectiveBlocks(),game.items().indirectKills().wildCause(owner));
            jobs.add(job);game.items().stonekivi().queuedWork(job);return true;
        }finally{callbacks--;}
    }
    private static final class Stamp {long revision;boolean clean;}
    private Stamp stamp(ServerLevel level,LevelChunk chunk,int section){
        return caches.computeIfAbsent(level,l->new WeakHashMap<>()).computeIfAbsent(chunk,c->new HashMap<>())
                .computeIfAbsent(section,s->new Stamp());
    }
    /** Both successful Level.setBlock and direct LevelChunk.setBlockState writes notify this.
     * A non-effective replacement cannot introduce targets into a proven-clean section. */
    public void blockChanged(ServerLevel level,BlockPos at,BlockState state){
        thread();if(restoring||!effective(state))return;
        var cache=caches.get(level);if(cache==null)return;
        var chunk=level.getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4);if(chunk==null)return;
        var sections=cache.get(chunk);if(sections==null)return;
        var stamp=sections.get(chunk.getSectionIndex(at.getY()));if(stamp==null)return;
        stamp.revision++;stamp.clean=false;
    }
    /** Whole-section/palette replacement paths call this instead of inventing per-cell events. */
    public void invalidate(ServerLevel level){thread();caches.remove(level);terrainGeneration++;}
    private record Candidate(int cell,BlockState expected) { }
    private record Request(LevelChunk chunk,int section,int sectionY,long revision,long terrainGeneration,LevelChunkSection palette) { }
    /** Pure computation: no ServerLevel, chunk, block entity, NBT, entity or callback is reachable. */
    static List<Candidate> scan(LevelChunkSection palette,Set<Block> eligible,AtomicLong token,long generation){
        var found=new ArrayList<Candidate>();
        for(int cell=0;cell<4096;cell++){
            if((cell&255)==0&&token.get()!=generation)return List.of();
            var state=palette.getBlockState(cell&15,cell>>8,(cell>>4)&15);
            if(eligible.contains(state.getBlock()))found.add(new Candidate(cell,state));
        }
        return List.copyOf(found);
    }
    private void submit(Harvest job){
        if(workerOwner!=null)throw new IllegalStateException("One Axe palette worker at a time");
        var palette=job.request.palette();var eligible=job.eligible;var token=workerGeneration;long generation=token.get();
        workerOwner=job;workerScans++;
        worker=game.maps().computeFromSnapshot(()->scan(palette,eligible,token,generation));
    }
    private void invalidateWorker(){
        workerGeneration.incrementAndGet();if(worker!=null)worker.cancel(false);worker=null;workerOwner=null;
    }
    private static List<ItemStack> copies(List<ItemStack> stacks){return stacks.stream().map(ItemStack::copy).toList();}
    /** Count-independent native item/component identity; keys borrow only untouched input stacks. */
    private static final class StackKey {
        final ItemStack sample;final int hash;
        StackKey(ItemStack sample){this.sample=sample;hash=ItemStack.hashItemAndComponents(sample);}
        @Override public int hashCode(){return hash;}
        @Override public boolean equals(Object other){return other instanceof StackKey key&&ItemStack.isSameItemSameComponents(sample,key.sample);}
    }
    /** Quantum-local packing. No encoding, component loss, count truncation or quadratic search
     * through prior container contents; only the current partial stack of an exact kind is indexed. */
    static List<ItemStack> compactLoot(List<ItemStack> loot){
        var result=new ArrayList<ItemStack>();var partial=new HashMap<StackKey,ItemStack>();
        for(var stack:loot){
            if(stack.isEmpty())continue;
            int remaining=stack.getCount(),maximum=Math.max(1,stack.getMaxStackSize());
            var key=maximum>1?new StackKey(stack):null;
            var open=key==null?null:partial.get(key);
            if(open!=null){
                int moved=Math.min(remaining,maximum-open.getCount());open.grow(moved);remaining-=moved;
                if(open.getCount()==maximum)partial.remove(key);
            }
            while(remaining>0){
                int amount=Math.min(remaining,maximum);var packed=stack.copyWithCount(amount);result.add(packed);remaining-=amount;
                if(key!=null&&amount<maximum)partial.put(key,packed);
            }
        }
        return List.copyOf(result);
    }
    public record View(UUID owner,int chunk,int chunks,int section,int candidate,int candidates,int output,int outputs,long removed) { }
    public record Metrics(long copiedSections,long workerScans,long cacheHits,long writes,long delivered,long mergedStacks,long publishedEntities) { }
    public Metrics metrics(){thread();return new Metrics(copiedSections,workerScans,cacheHits,writes,delivered,mergedStacks,publishedEntities);}
    public int count(){thread();return jobs.size();}
    public List<View> views(){thread();return jobs.stream().map(Harvest::view).toList();}

    private final class Harvest implements LbcStonekivi.Work {
        ServerPlayer author;
        final UUID owner;final ServerLevel level;final String epoch;
        final long[] chunks;final ItemStack tool;final Set<Block> eligible;final LbcIndirectKills.Owner cause;
        int chunkCursor,sectionCursor,candidateCursor,outputCursor;
        long removed;
        Request request;List<Candidate> candidates;List<ItemStack> outputs=List.of();
        boolean unresolved,closed,stepping;
        final LinkedHashSet<Long> refreshChunks=new LinkedHashSet<>();
        ItemStack refreshTool;Set<Block> refreshEligible;LbcIndirectKills.Owner refreshCause;
        Harvest(ServerPlayer author,ServerLevel level,long[] chunks,ItemStack tool,Set<Block> eligible,LbcIndirectKills.Owner cause){
            this.author=author;owner=author.getUUID();this.level=level;this.chunks=chunks;this.tool=tool;this.eligible=eligible;this.cause=cause;epoch=game.items().epoch();
        }
        ServerPlayer currentAuthor(){var player=game.server().getPlayerList().getPlayer(owner);return player!=null&&player.isAlive()&&!player.isRemoved()&&!game.isObserver(player)?player:null;}
        boolean expired(){return closed||!game.playing()||game.stage()==GameController.Stage.ENDING||!game.items().currentEpoch(epoch);}
        public boolean ready(){
            if(restoring)return false;if(expired())return true;
            if(game.maps().busy()||game.items().timeStop().stopped(level)||currentAuthor()==null)return false;
            return request==null||candidates!=null||workerOwner==null||workerOwner==this&&worker.isDone();
        }
        public boolean step(){
            thread();if(stepping)throw new IllegalStateException("Reentrant Axe harvest quantum");
            if(restoring)return false;if(expired()){cancel();return true;}if(!ready())return false;
            stepping=true;callbacks++;
            try{
                author=currentAuthor();if(author==null)return false;
                if(outputCursor<outputs.size()){deliver();return false;}
                outputs=List.of();outputCursor=0;
                if(request!=null){
                    if(candidates==null){
                        if(workerOwner==null){submit(this);return false;}
                        if(workerOwner!=this||!worker.isDone())return false;
                        candidates=worker.join();worker=null;workerOwner=null;
                    }
                    validate();return false;
                }
                // One copied section OR a bounded skip pass, never a whole-world synchronous scan.
                for(int skip=0;skip<64&&chunkCursor<chunks.length;skip++){
                    long packed=chunks[chunkCursor];var key=new ChunkPos((int)packed,(int)(packed>>32));
                    var chunk=level.getChunkSource().getChunkNow(key.x(),key.z());
                    if(chunk==null){chunkCursor++;sectionCursor=0;continue;}
                    if(sectionCursor>=chunk.getSectionsCount()){chunkCursor++;sectionCursor=0;continue;}
                    var stamp=stamp(level,chunk,sectionCursor);
                    if(stamp.clean){cacheHits++;sectionCursor++;continue;}
                    var section=chunk.getSection(sectionCursor);
                    if(section.hasOnlyAir()||!section.maybeHas(state->eligible.contains(state.getBlock()))){stamp.clean=true;sectionCursor++;continue;}
                    request=new Request(chunk,sectionCursor,chunk.getSectionYFromSectionIndex(sectionCursor),stamp.revision,terrainGeneration,section.copy());
                    copiedSections++;candidateCursor=0;unresolved=false;
                    if(workerOwner==null)submit(this);return false;
                }
                if(chunkCursor==chunks.length){
                    closed=true;jobs.remove(this);
                    if(refreshTool!=null){
                        long[] selected=new long[refreshChunks.size()];int n=0;for(long key:refreshChunks)selected[n++]=key;
                        var next=new Harvest(author,level,selected,refreshTool,refreshEligible,refreshCause);
                        jobs.add(next);game.items().stonekivi().queuedWork(next);
                    }
                    return true;
                }
                return false;
            }finally{callbacks--;stepping=false;}
        }
        private void validate(){
            long deadline=System.nanoTime()+1_000_000L;
            var collected=new ArrayList<ItemStack>();long unmerged=0;
            for(int accepted=0;accepted<VALIDATIONS&&candidateCursor<candidates.size()&&System.nanoTime()<deadline;accepted++){
                var candidate=candidates.get(candidateCursor++);
                var key=request.chunk().getPos();var current=level.getChunkSource().getChunkNow(key.x(),key.z());
                if(current==null){unresolved=true;continue;}
                int cell=candidate.cell();var at=new BlockPos((key.x()<<4)+(cell&15),(request.sectionY()<<4)+(cell>>8),(key.z()<<4)+((cell>>4)&15));
                var actual=level.getBlockState(at);
                // Even a matching revision is not authority to write a stale snapshot value.
                if(actual!=candidate.expected()){if(eligible.contains(actual.getBlock()))unresolved=true;continue;}
                var loot=harvest(author,level,at,actual,tool,cause);
                if(loot==null){unresolved=true;continue;}
                removed++;writes++;collected.addAll(loot);
                for(var stack:loot)if(!stack.isEmpty())unmerged+=(stack.getCount()-1L)/Math.max(1,stack.getMaxStackSize())+1;
            }
            outputs=compactLoot(collected);outputCursor=0;mergedStacks+=unmerged-outputs.size();
            if(candidateCursor==candidates.size()){
                var key=request.chunk().getPos();var current=level.getChunkSource().getChunkNow(key.x(),key.z());
                if(current==request.chunk()){
                    var stamp=stamp(level,current,request.section());
                    if(terrainGeneration==request.terrainGeneration()&&stamp.revision==request.revision()&&!unresolved)stamp.clean=true;
                }
                request=null;candidates=null;candidateCursor=0;sectionCursor++;
            }
        }
        private void deliver(){
            var recipient=currentAuthor();if(recipient==null||!LbcBedwarsRoleGuards.publication(recipient.level()))return;
            for(int n=0;n<OUTPUTS&&outputCursor<outputs.size();n++){
                var stack=outputs.get(outputCursor);if(stack.isEmpty()){outputCursor++;continue;}
                var at=recipient.position();var drop=new ItemEntity(recipient.level(),at.x,at.y,at.z,stack.copy());
                // A transported native block drop is not a new user throw. In particular it
                // must not activate dropped-item consumables merely by acquiring a thrower.
                drop.setDeltaMovement(Vec3.ZERO);drop.setDefaultPickUpDelay();
                game.items().markCurrentItemEpoch(drop);game.items().timeStop().prepareFrozen(drop);
                boolean[] accepted={false};game.items().indirectKills().capturedWorldChanges(recipient.level(),cause,()->accepted[0]=recipient.level().addFreshEntity(drop));
                if(!accepted[0]){drop.discard();return;}
                delivered+=stack.getCount();publishedEntities++;outputCursor++;
            }
        }
        public boolean snapshotIdle(){return !stepping;}
        public void cancel(){closed=true;jobs.remove(this);if(workerOwner==this)invalidateWorker();}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            checkpointBoundary();var rebound=graph.links.key(currentAuthor()==null?author:currentAuthor());
            int chunk=chunkCursor,section=sectionCursor,candidate=candidateCursor,output=outputCursor;
            long count=removed;var palette=request;var accepted=candidates;var pending=copies(outputs);boolean blocked=unresolved,stopped=closed;
            var refresh=List.copyOf(refreshChunks);var nextTool=refreshTool==null?null:refreshTool.copy();var nextEligible=refreshEligible;var nextCause=refreshCause;
            return links->{checkpointBoundary();MatchAuthorityCheckpoint.world(game,level);var current=(ServerPlayer)links.get(rebound);
                return ()->{author=current;chunkCursor=chunk;sectionCursor=section;candidateCursor=candidate;outputCursor=output;
                    removed=count;request=palette;candidates=accepted;outputs=copies(pending);unresolved=blocked;closed=stopped;
                    refreshChunks.clear();refreshChunks.addAll(refresh);refreshTool=nextTool==null?null:nextTool.copy();refreshEligible=nextEligible;refreshCause=nextCause;};};
        }
        View view(){return new View(owner,chunkCursor,chunks.length,sectionCursor,candidateCursor,candidates==null?0:candidates.size(),outputCursor,outputs.size(),removed);}
    }
    /** Null means no mutation/output was accepted. Empty loot is still a successful removal. */
    List<ItemStack> harvest(ServerPlayer author,ServerLevel level,BlockPos at,BlockState expected,ItemStack tool,LbcIndirectKills.Owner cause){
        return harvest(author,level,at,expected,tool,cause,this::effective,false);
    }
    /** Other native mining owners supply eligibility while sharing the exact native removal/loot transaction. */
    List<ItemStack> harvest(ServerPlayer author,ServerLevel level,BlockPos at,BlockState expected,ItemStack tool,
            LbcIndirectKills.Owner cause,java.util.function.Predicate<BlockState> eligible){
        return harvest(author,level,at,expected,tool,cause,eligible,true);
    }
    private List<ItemStack> harvest(ServerPlayer author,ServerLevel level,BlockPos at,BlockState expected,ItemStack tool,
            LbcIndirectKills.Owner cause,java.util.function.Predicate<BlockState> eligible,boolean correctToolDrops){
        thread();if(!level.isInValidBounds(at)||!LbcBedwarsRoleGuards.publication(level)||!eligible.test(expected)
                ||level.getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4)==null||level.getBlockState(at)!=expected)return null;
        var arena=LbcBedwarsArena.at(level);if(arena!=null&&!arena.allowsRemoval(level,at))return null;
        // Capture the original native payload BEFORE opening or clearing a container.
        if(arena!=null)arena.captureUnchanged(level,at,Blocks.AIR.defaultBlockState());
        game.maps().noteMapBlockWrite(level,at);game.items().indirectKills().beforeBlockChange(level,at,Blocks.AIR.defaultBlockState());
        var entity=level.getBlockEntity(at);
        if(entity instanceof RandomizableContainer randomizable)randomizable.unpackLootTable(author);
        boolean mayDrop=!correctToolDrops||!expected.requiresCorrectToolForDrops()||tool.isCorrectToolForDrops(expected);
        var loot=mayDrop?new ArrayList<>(Block.getDrops(expected,level,at,entity,author,tool.copy())):new ArrayList<ItemStack>();
        if(!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.BLOCK_DROPS))loot.clear();
        boolean[] changed={false};
        var previous=DROP_CAPTURE.get();DROP_CAPTURE.set(new Capture(level,loot));
        try{
            game.items().indirectKills().capturedWorldChanges(level,cause,()->{
                // Native removal invokes preRemoveSideEffects exactly once. Its own chest,
                // lectern, campfire and future container semantics decide which items eject.
                // Shulker preRemove is empty: its one native block drop already contains items.
                changed[0]=level.setBlock(at,Blocks.AIR.defaultBlockState(),WRITE_FLAGS);
                if(changed[0]){
                    if(level==game.maps().level())game.maps().recordBlockChange(at);
                    game.items().indirectKills().supportRemoved(level,cause,at);
                    expected.spawnAfterBreak(level,at,tool,mayDrop);
                }
            });
        }finally{if(previous==null)DROP_CAPTURE.remove();else DROP_CAPTURE.set(previous);}
        return changed[0]?copies(loot):null;
    }
    public void checkpointBoundary(){thread();if(callbacks!=0)throw new IllegalStateException("Finish Axe native quantum before checkpointing");}
    public void stop(){checkpointBoundary();invalidateWorker();for(var job:List.copyOf(jobs))job.cancel();caches.clear();terrainGeneration++;restoring=false;}
    public void prepareFrozen(){checkpointBoundary();invalidateWorker();caches.clear();terrainGeneration++;jobs=new LinkedHashSet<>();restoring=true;}
    public void publishFrozen(){checkpointBoundary();restoring=false;}
    public void cancelFrozen(){stop();}
    Checkpoint checkpoint(MatchQueuedWorkCheckpoint.Graph graph){checkpointBoundary();return new Checkpoint(graph);}
    final class Checkpoint {
        final List<Harvest> saved=List.copyOf(jobs);
        final Metrics metrics=LbcImbaAxe.this.metrics();
        Checkpoint(MatchQueuedWorkCheckpoint.Graph graph){saved.forEach(graph::capture);}
        Runnable prepare(){checkpointBoundary();return ()->{jobs=new LinkedHashSet<>(saved);copiedSections=metrics.copiedSections();workerScans=metrics.workerScans();cacheHits=metrics.cacheHits();writes=metrics.writes();delivered=metrics.delivered();mergedStacks=metrics.mergedStacks();publishedEntities=metrics.publishedEntities();};}
    }
}
