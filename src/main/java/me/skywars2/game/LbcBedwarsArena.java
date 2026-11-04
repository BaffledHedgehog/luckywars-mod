package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Original-material protection and placed arena topology; terrain remains owned by MapManager. */
public final class LbcBedwarsArena {
    public enum MarkerKind { BED,SPAWN,SHOP,COPPER,IRON,GOLD,DIAMOND,STAR,LOOK }
    public enum Exemptions { STRICT_IGNITE,BUKKIT_PLANTS_FLUIDS }
    public enum Lifecycle { MAP_PLACEMENT,MAP_RESET,MAP_RENEWAL,COORDINATOR_REPLAY }
    public record Bounds(int minX,int minY,int minZ,int maxX,int maxY,int maxZ) {
        public Bounds {if(minX>maxX||minY>maxY||minZ>maxZ)throw new IllegalArgumentException("Ordered arena bounds required");}
        public boolean contains(BlockPos pos){return pos.getX()>=minX&&pos.getX()<=maxX&&pos.getY()>=minY&&pos.getY()<=maxY&&pos.getZ()>=minZ&&pos.getZ()<=maxZ;}
    }
    public record Marker(UUID id,ServerLevel level,BlockPos pos,MarkerKind kind,int team) {
        public Marker {Objects.requireNonNull(id);Objects.requireNonNull(level);pos=pos.immutable();Objects.requireNonNull(kind);}
    }
    public record Bed(int team,UUID marker,BlockPos foot,BlockPos head,BlockPos spawn) {
        public Bed {Objects.requireNonNull(marker);foot=foot.immutable();head=head.immutable();spawn=spawn.immutable();if(team<1||team>16||foot.equals(head))throw new IllegalArgumentException("Native team bed required");}
    }
    public record Topology(ServerLevel level,List<Bounds> bounds,List<Marker> markers,List<Bed> beds) {
        public Topology {Objects.requireNonNull(level);bounds=List.copyOf(bounds);markers=List.copyOf(markers);beds=List.copyOf(beds);}
    }
    public interface Policy {
        boolean active();
        boolean canBreakBed(ServerPlayer player,int team);
        void bedChanged(Bed bed);
        boolean lifecycleAllowed(Lifecycle reason);
    }
    /** Native copied-cell preflight rejection; caught only by the enclosing native transaction. */
    public static final class Denied extends RuntimeException {
        private Denied(){super("Protected original BedWars block",null,false,false);}
    }
    private record Permit(ServerLevel level,Map<Long,Block> cells,List<Bounds> bounds,Lifecycle lifecycle,Permit previous) {
        boolean includes(BlockPos pos,Block wanted){return cells!=null&&(cells.get(pos.asLong())==wanted||cells.containsKey(pos.asLong())&&wanted==Blocks.BARRIER)
                ||bounds!=null&&bounds.stream().anyMatch(b->b.contains(pos));}
    }
    private final GameController game;
    private final Policy policy;
    private Topology topology;
    private String epoch;
    private Long2ObjectMap<Block> original=Long2ObjectMaps.emptyMap();
    private Long2ObjectMap<Bed> bedCells=Long2ObjectMaps.emptyMap();
    private LongSet unlocked=LongSets.EMPTY_SET;
    private ServerLevel capturingLevel;
    private List<Bounds> capturingBounds=List.of();
    private final Long2ObjectMap<Block> capturing=new Long2ObjectOpenHashMap<>();
    private Permit permit;
    private int transactions;
    private final Set<Bed> deferredBeds=new LinkedHashSet<>();
    private boolean restoring,breaking;
    private Exemptions exemptions=Exemptions.STRICT_IGNITE;
    private static final TagKey<Block> FLOWERS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:flowers"));
    private static final TagKey<Block> SMALL_FLOWERS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:small_flowers"));
    public LbcBedwarsArena(GameController game,Policy policy){this.game=Objects.requireNonNull(game);this.policy=Objects.requireNonNull(policy);}
    public static LbcBedwarsArena at(Object level){var game=SkyWars2.game();return game!=null&&level instanceof ServerLevel&&game.bedwars()!=null?game.bedwars().arena():null;}
    private void thread(){if(!game.server().isSameThread())throw new IllegalStateException("Native arena policy requires the match thread");}
    public boolean active(){return topology!=null&&game.items().currentEpoch(epoch)&&policy.active();}
    public Topology topology(){return topology;}
    public Exemptions exemptions(){return exemptions;}
    public void exemptions(Exemptions value){thread();exemptions=Objects.requireNonNull(value);}
    public void beginCapture(ServerLevel level,List<Bounds> bounds){thread();capturingLevel=Objects.requireNonNull(level);capturingBounds=List.copyOf(bounds);capturing.clear();}
    /** Feed FINAL completed placement cells, including AIR overwrites; never scans a world volume. */
    public void captureCell(BlockPos pos,BlockState state){
        thread();if(capturingLevel==null)throw new IllegalStateException("Begin arena capture before feeding cells");
        if(capturingBounds.stream().noneMatch(b->b.contains(pos)))throw new IllegalArgumentException("Captured cell outside active arena bounds");
        if(state.isAir())capturing.remove(pos.asLong());else capturing.put(pos.asLong(),state.getBlock());
    }
    private boolean capturingAt(ServerLevel level,BlockPos pos){return capturingLevel==level&&capturingBounds.stream().anyMatch(b->b.contains(pos));}
    /** A native no-op write still represents an authored final cell during placement. */
    public void captureUnchanged(Object world,BlockPos pos,BlockState wanted){
        if(world instanceof ServerLevel level&&capturingAt(level,pos)&&level.getBlockState(pos)==wanted)captureCell(pos,wanted);
    }
    public void activate(Topology value){
        thread();Objects.requireNonNull(value);
        if(capturingLevel!=value.level||!capturingBounds.equals(value.bounds))throw new IllegalStateException("Complete the same placed arena capture before activation");
        var indexed=index(value);topology=value;bedCells=indexed;original=Long2ObjectMaps.unmodifiable(new Long2ObjectOpenHashMap<>(capturing));epoch=game.items().epoch();unlocked=LongSets.EMPTY_SET;
        capturing.clear();capturingLevel=null;capturingBounds=List.of();
    }
    private Long2ObjectMap<Bed> index(Topology value){
        var indexed=new Long2ObjectOpenHashMap<Bed>();var teams=new HashSet<Integer>();var ids=new HashSet<UUID>();
        for(var marker:value.markers){if(marker.level!=value.level||!ids.add(marker.id)||value.bounds.stream().noneMatch(b->b.contains(marker.pos)))throw new IllegalArgumentException("Marker realm, identity or bounds mismatch");}
        for(var bed:value.beds){if(!teams.add(bed.team)||indexed.putIfAbsent(bed.foot.asLong(),bed)!=null||indexed.putIfAbsent(bed.head.asLong(),bed)!=null)throw new IllegalArgumentException("Duplicate native bed team/part");
            if(value.bounds.stream().noneMatch(b->b.contains(bed.foot))||value.bounds.stream().noneMatch(b->b.contains(bed.head)))throw new IllegalArgumentException("Bed outside active arena");}
        return Long2ObjectMaps.unmodifiable(indexed);
    }
    public Block originalTypeAt(ServerLevel level,BlockPos pos){return topology!=null&&topology.level==level&&!unlocked.contains(pos.asLong())?original.get(pos.asLong()):null;}
    private boolean exempt(Block block){return exemptions==Exemptions.BUKKIT_PLANTS_FLUIDS&&(block==Blocks.WATER||block==Blocks.LAVA||block==Blocks.POWDER_SNOW
            ||block==Blocks.SHORT_GRASS||block==Blocks.TALL_GRASS||block==Blocks.FERN||block==Blocks.LARGE_FERN||block.builtInRegistryHolder().is(FLOWERS)||block.builtInRegistryHolder().is(SMALL_FLOWERS));}
    public boolean allowsType(Object world,BlockPos pos,BlockState wanted){
        if(!(world instanceof ServerLevel level)||!active()||topology.level!=level)return true;
        for(var next=permit;next!=null;next=next.previous)if(next.level==level&&next.includes(pos,wanted.getBlock()))return true;
        return originalAllows(level,pos,wanted);
    }
    private boolean originalAllows(ServerLevel level,BlockPos pos,BlockState wanted){
        if(!active()||topology.level!=level)return true;
        for(var next=permit;next!=null;next=next.previous)if(next.lifecycle!=null&&next.level==level&&next.includes(pos,wanted.getBlock()))return true;
        var saved=unlocked.contains(pos.asLong())?null:original.get(pos.asLong());
        if(!game.config().bedwarsDirectBedBreaking&&bedCells.containsKey(pos.asLong()))return true;
        return saved==null||saved==wanted.getBlock()||exempt(saved)||exempt(wanted.getBlock());
    }
    public boolean allowsRemoval(Object level,BlockPos pos){return allowsType(level,pos,Blocks.AIR.defaultBlockState());}
    /** Only actual successful native writes notify team authority; lifecycle publication is silent. */
    public void blockChanged(ServerLevel level,BlockPos pos,BlockState state){
        if(capturingAt(level,pos))captureCell(pos,level.getBlockState(pos));
        if(!active()||topology.level!=level||restoring||breaking)return;
        for(var next=permit;next!=null;next=next.previous)if(next.lifecycle!=null)return;
        var bed=bedCells.get(pos.asLong());if(bed!=null){if(transactions>0)deferredBeds.add(bed);else changedBed(bed);}
    }

    private boolean intact(Bed bed){
        var foot=topology.level.getBlockState(bed.foot);var head=topology.level.getBlockState(bed.head);
        return foot.getBlock() instanceof net.minecraft.world.level.block.BedBlock&&head.getBlock()==foot.getBlock()
                &&foot.getValue(net.minecraft.world.level.block.BedBlock.PART)==net.minecraft.world.level.block.state.properties.BedPart.FOOT
                &&head.getValue(net.minecraft.world.level.block.BedBlock.PART)==net.minecraft.world.level.block.state.properties.BedPart.HEAD
                &&foot.getValue(net.minecraft.world.level.block.BedBlock.FACING)==head.getValue(net.minecraft.world.level.block.BedBlock.FACING)
                &&bed.foot.relative(foot.getValue(net.minecraft.world.level.block.BedBlock.FACING)).equals(bed.head);
    }
    private void changedBed(Bed bed){
        if(intact(bed))return; // Occupancy and other non-destructive state changes retain the team's bed.
        var removed=new LongOpenHashSet(unlocked);removed.add(bed.foot.asLong());removed.add(bed.head.asLong());
        var beacon=bed.spawn.below(2);removed.add(beacon.asLong());unlocked=LongSets.unmodifiable(removed);
        if(topology.level.getBlockState(beacon).is(Blocks.BEACON))topology.level.setBlock(beacon,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SUPPRESS_DROPS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
        policy.bedChanged(bed);
    }
    public void lifecycle(Lifecycle reason,ServerLevel level,List<Bounds> exactBounds,Runnable action){
        thread();if(!policy.lifecycleAllowed(reason))throw new IllegalStateException("Arena lifecycle permission rejected: "+reason);
        var previous=permit;permit=new Permit(level,null,List.copyOf(exactBounds),reason,previous);
        try{action.run();}finally{permit=previous;}
    }
    /** One synchronous native clone/template call; accepted cells cannot escape its finally scope. */
    public final class Transaction implements AutoCloseable {
        private final Permit previous=permit;private boolean closed;
        private Transaction(){thread();transactions++;}
        public void close(){if(!closed){permit=previous;transactions--;closed=true;
            if(transactions==0&&!deferredBeds.isEmpty()){var changed=List.copyOf(deferredBeds);deferredBeds.clear();if(active()&&!restoring)changed.forEach(LbcBedwarsArena.this::changedBed);}
        }}
    }
    public Transaction nativeTransaction(){return new Transaction();}
    /** Admit final types first; only those exact cells may use the native temporary BARRIER stage. */
    public void admitCopy(Object world,Map<BlockPos,BlockState> changes){
        thread();if(!(world instanceof ServerLevel level)||!active()||topology.level!=level)return;
        if(transactions==0)throw new IllegalStateException("Native copy preflight outside its transaction");
        for(var entry:changes.entrySet())if(!originalAllows(level,entry.getKey(),entry.getValue()))throw new Denied();
        var cells=new HashMap<Long,Block>();changes.forEach((pos,state)->cells.put(pos.asLong(),state.getBlock()));
        permit=new Permit(level,Map.copyOf(cells),null,null,permit);
    }
    /** null means ordinary native break; false is a denied protected edit; true is an owned bed transaction. */
    public Boolean playerBreak(ServerPlayer player,BlockPos pos){
        thread();if(!active()||topology.level!=player.level())return null;
        var bed=bedCells.get(pos.asLong());if(bed==null||originalTypeAt(player.level(),bed.foot)==null&&originalTypeAt(player.level(),bed.head)==null)return allowsRemoval(player.level(),pos)?null:false;
        var level=player.level();
        if(!policy.canBreakBed(player,bed.team)||!level.hasChunkAt(bed.foot)||!level.hasChunkAt(bed.head))return false;
        var foot=level.getBlockState(bed.foot);var head=level.getBlockState(bed.head);
        if(!(foot.getBlock() instanceof net.minecraft.world.level.block.BedBlock)||foot.getBlock()!=head.getBlock()
                ||foot.getValue(net.minecraft.world.level.block.BedBlock.PART)!=net.minecraft.world.level.block.state.properties.BedPart.FOOT
                ||head.getValue(net.minecraft.world.level.block.BedBlock.PART)!=net.minecraft.world.level.block.state.properties.BedPart.HEAD
                ||head.getValue(net.minecraft.world.level.block.BedBlock.FACING)!=foot.getValue(net.minecraft.world.level.block.BedBlock.FACING)
                ||!bed.foot.relative(foot.getValue(net.minecraft.world.level.block.BedBlock.FACING)).equals(bed.head))return false;
        var removed=new LongOpenHashSet(unlocked);removed.add(bed.foot.asLong());removed.add(bed.head.asLong());
        var beacon=bed.spawn.below(2);boolean removeBeacon=level.getBlockState(beacon).is(Blocks.BEACON);
        if(removeBeacon)removed.add(beacon.asLong());unlocked=LongSets.unmodifiable(removed);
        breaking=true;
        try{
            int flags=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS|Block.UPDATE_SKIP_ON_PLACE;
            if(!level.setBlock(bed.foot,Blocks.AIR.defaultBlockState(),flags)||!level.setBlock(bed.head,Blocks.AIR.defaultBlockState(),flags)
                    ||removeBeacon&&!level.setBlock(beacon,Blocks.AIR.defaultBlockState(),flags))throw new IllegalStateException("Admitted native bed transaction rejected during publication");
            level.playSound(null,bed.foot,net.minecraft.sounds.SoundEvents.WOOL_BREAK,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        }finally{breaking=false;}
        policy.bedChanged(bed);return true;
    }
    public void stop(){thread();if(transactions!=0||permit!=null)throw new IllegalStateException("Finish native arena transaction before retiring match");topology=null;epoch=null;original=Long2ObjectMaps.emptyMap();bedCells=Long2ObjectMaps.emptyMap();unlocked=LongSets.EMPTY_SET;capturingLevel=null;capturingBounds=List.of();capturing.clear();deferredBeds.clear();restoring=false;}
    public void prepareFrozen(){thread();if(transactions!=0||permit!=null)throw new IllegalStateException("Finish native arena transaction before rewind");restoring=true;}
    public void publishFrozen(){thread();restoring=false;}
    public Checkpoint checkpoint(){thread();if(transactions!=0||permit!=null||capturingLevel!=null)throw new IllegalStateException("Finish native arena preparation before checkpoint");return new Checkpoint();}
    public final class Checkpoint {
        private final Topology saved=topology;private final String savedEpoch=epoch;private final Long2ObjectMap<Block> savedTypes=original;private final LongSet savedUnlocked=unlocked;private final Exemptions savedExemptions=exemptions;
        public MatchFieldCheckpoint.Plan prepare(Function<UUID,Entity> lookup){
            return new MatchFieldCheckpoint.Plan(Set.of(),()->{topology=saved;epoch=savedEpoch;original=savedTypes;unlocked=savedUnlocked;exemptions=savedExemptions;bedCells=saved==null?Long2ObjectMaps.emptyMap():index(saved);deferredBeds.clear();capturingLevel=null;capturingBounds=List.of();capturing.clear();});
        }
    }
}
