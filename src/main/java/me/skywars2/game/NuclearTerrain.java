package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** One bounded, section-aware terrain operation per nuclear payload, shared by all producers. */
public final class NuclearTerrain {
    private NuclearTerrain() {}
    /** Implemented by the existing native-explosion mixin; never invokes discovery, damage or packets. */
    public interface BlockEffects {
        void skywars2$applyNuclearBlocks(java.util.List<BlockPos> positions);
        java.util.List<BlockPos> skywars2$nativeNuclearPositions();
    }
    private static final ThreadLocal<Entity> DAMAGE_WAVE=new ThreadLocal<>();
    private static final ThreadLocal<Occlusion> WALLS=new ThreadLocal<>(),EXPOSURE=new ThreadLocal<>();
    /** Preserve actual pre-crater collision for distant targets; the immediate core uses live terrain. */
    static final class Occlusion implements net.minecraft.world.level.BlockGetter {
        private final Vec3 origin;private final double coreSquared;
        private ServerLevel level;
        private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<net.minecraft.world.level.block.state.BlockState> states=new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
        Occlusion(Vec3 origin,double core){this.origin=origin;coreSquared=core*core;}
        Occlusion copy(){var value=new Occlusion(origin,Math.sqrt(coreSquared));value.level=level;value.states.putAll(states);return value;}
        void remember(ServerLevel level,BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
            this.level=level;if(!state.getCollisionShape(level,pos).isEmpty())states.putIfAbsent(pos.asLong(),state);
        }
        public net.minecraft.world.level.block.state.BlockState getBlockState(BlockPos pos){var state=states.get(pos.asLong());return state==null?level.getBlockState(pos):state;}
        public net.minecraft.world.level.material.FluidState getFluidState(BlockPos pos){return getBlockState(pos).getFluidState();}
        public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos pos){return level.getBlockEntity(pos);}
        public int getHeight(){return level.getHeight();}
        public int getMinY(){return level.getMinY();}
    }
    static void withOcclusion(Occlusion value,Runnable action){var previous=WALLS.get();if(value==null)WALLS.remove();else WALLS.set(value);try{action.run();}finally{if(previous==null)WALLS.remove();else WALLS.set(previous);}}
    /** An unrelated explosion nested in a nuclear hit must never borrow the outer snapshot. */
    public static void exposureScope(net.minecraft.world.level.ServerExplosion blast,Runnable action){
        var previous=EXPOSURE.get();var value=damageOnly(blast)?WALLS.get():null;
        if(value==null)EXPOSURE.remove();else EXPOSURE.set(value);
        try{action.run();}finally{if(previous==null)EXPOSURE.remove();else EXPOSURE.set(previous);}
    }
    public static net.minecraft.world.phys.BlockHitResult clip(net.minecraft.world.level.Level level,net.minecraft.world.level.ClipContext context){
        var value=EXPOSURE.get();return value!=null&&value.level==level&&!value.states.isEmpty()
                &&context.getFrom().distanceToSqr(value.origin)>value.coreSquared?value.clip(context):null;
    }
    static final int CELLS_PER_STEP=4096;
    public static void damageWave(Entity emitter,Runnable nativeExplosion){
        var previous=DAMAGE_WAVE.get();DAMAGE_WAVE.set(emitter);
        try{nativeExplosion.run();}finally{if(previous==null)DAMAGE_WAVE.remove();else DAMAGE_WAVE.set(previous);}
    }
    /** Exact emitter identity keeps nested ordinary chain reactions on their own native terrain path. */
    public static boolean damageOnly(net.minecraft.world.level.ServerExplosion blast){
        var expected=DAMAGE_WAVE.get();return expected!=null&&(expected==blast.getDirectSourceEntity()
                ||blast.getDirectSourceEntity()==null&&expected==blast.getDamageSource().getDirectEntity());
    }
    static Point point(Vec3 at){return point(at,0,false,BlockPos.containing(at).asLong());}
    static Point point(Vec3 at,float yaw,boolean marker,long seed){return new Point(at,yaw,marker,seed);}
    // The actual legacy32-creeper surface crater has a solid inner18 and its
    // transition centred near23. The former40x45 surrogate enlarged that bowl.
    static Shape wand(Vec3 at){return new Shape(at,at,23,23,20,true);}
    /** Source AIR templates are separate from the native creeper lobes; they ignore blast resistance. */
    static final class Point {
        private record Cube(BlockPos low,float integrity){}
        final Vec3 origin;final float power;final boolean marker;final java.util.List<Cube> cubes;
        private final long seed;private java.util.concurrent.CompletableFuture<Geometry> preparation;
        private Geometry geometry;private int section,word;private long bits;private boolean wordLoaded;
        int visited;boolean done;
        Point(Vec3 origin,float yaw,boolean marker,long seed){
            this.origin=origin;this.marker=marker;power=marker?16:25;this.seed=seed;
            var definitions=new java.util.ArrayList<Cube>();
            for(int angle=0;angle<360;angle+=60)definitions.add(cube(origin,yaw+angle,25,.5f));
            int[] outer=marker?new int[]{0,30,90,150,210,270,330}:new int[]{0,60,120,180,240,300};
            for(int angle:outer)definitions.add(cube(origin,yaw+angle,60,.25f));
            cubes=java.util.List.copyOf(definitions);
        }
        private static Cube cube(Vec3 origin,float yaw,int distance,float integrity){
            // /execute positioned ^ ^ ^distance, then /place template ~-24 ~-24 ~-24.
            var forward=Vec3.directionFromRotation(0,yaw);
            return new Cube(BlockPos.containing(origin.add(forward.scale(distance)).add(-24,-24,-24)),integrity);
        }
        void prepare(GameController game){
            if(preparation!=null)return;
            var at=origin;var definitions=cubes;long randomSeed=seed;
            preparation=game.maps().preparePure(()->prepareMask(at,definitions,randomSeed));
        }
        private static Geometry prepareMask(Vec3 at,java.util.List<Cube> cubes,long seed){
            var masks=new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<java.util.BitSet>();
            for(int i=0;i<cubes.size();i++){
                var cube=cubes.get(i);long salt=seed^((i+1)*0x9e3779b97f4a7c15L);
                for(int y=0;y<48;y++)for(int z=0;z<48;z++)for(int x=0;x<48;x++){
                    int bx=cube.low.getX()+x,by=cube.low.getY()+y,bz=cube.low.getZ()+z;
                    if(unit(salt^BlockPos.asLong(bx,by,bz))>=cube.integrity)continue;
                    long key=net.minecraft.core.SectionPos.asLong(bx>>4,by>>4,bz>>4);
                    var mask=masks.get(key);if(mask==null){mask=new java.util.BitSet(4096);masks.put(key,mask);}
                    mask.set((bx&15)|((bz&15)<<4)|((by&15)<<8));
                }
            }
            var sections=new java.util.ArrayList<Geometry.Section>();
            // Packed section order is stable; no world, block-state, chunk or NBT snapshots.
            long[] keys=masks.keySet().toLongArray();java.util.Arrays.sort(keys);
            for(long key:keys)sections.add(new Geometry.Section(net.minecraft.core.SectionPos.x(key),net.minecraft.core.SectionPos.y(key),net.minecraft.core.SectionPos.z(key),masks.get(key).toLongArray()));
            return new Geometry(at,sections);
        }
        boolean ready(){return done||preparation!=null&&preparation.isDone();}
        boolean step(GameController game,ServerLevel level,LbcIndirectKills.Owner cause){
            visited=0;if(done)return true;prepare(game);
            if(!preparation.isDone())return false;if(geometry==null)geometry=preparation.join();
            var coverage=LbcExplosionSuppression.coverage(game,level);
            if(coverage.contains(origin)){done=true;return true;}
            game.items().indirectKills().withSupportCandidates(level,new net.minecraft.world.phys.AABB(origin,origin).inflate(85,25,85),
                    ()->game.items().indirectKills().trapChanges(level,cause,()->clear(level,coverage)));
            return done;
        }
        private void clear(ServerLevel level,LbcExplosionSuppression.Coverage coverage){
            var probe=new BlockPos.MutableBlockPos();
            while(section<geometry.sections.size()&&visited<CELLS_PER_STEP){
                var part=geometry.sections.get(section);
                if((part.y<<4)+15<level.getMinY()||(part.y<<4)>=level.getMaxY()){advanceSection();continue;}
                var chunk=level.getChunkSource().getChunkNow(part.x,part.z);
                if(chunk==null){advanceSection();continue;}
                var slice=chunk.getSection(level.getSectionIndex(part.y<<4));
                if(slice.hasOnlyAir()){advanceSection();continue;}
                if(word>=part.mask.length){advanceSection();continue;}
                if(!wordLoaded){bits=part.mask[word];wordLoaded=true;}
                if(bits==0){word++;wordLoaded=false;continue;}
                int cell=(word<<6)+Long.numberOfTrailingZeros(bits);bits&=bits-1;visited++;
                int x=(part.x<<4)+(cell&15),y=(part.y<<4)+(cell>>8),z=(part.z<<4)+((cell>>4)&15);
                probe.set(x,y,z);
                if(!slice.getBlockState(x&15,y&15,z&15).isAir()&&!coverage.blocks(probe))
                    LbcDropBlocks.setBlock(level,probe,Blocks.AIR.defaultBlockState(),true);
            }
            done=section>=geometry.sections.size();
        }
        private void advanceSection(){section++;word=0;bits=0;wordLoaded=false;}
        void cancel(){if(preparation!=null)preparation.cancel(false);done=true;}
    }
    /** Pure countdown preparation: no world copies and no32 overlapping native blast calculations. */
    static final class WandPreparation {
        final Vec3 at;final long seed;
        java.util.concurrent.CompletableFuture<Geometry> future;volatile boolean cancelled;
        WandPreparation(ServerLevel level,Vec3 at){this(level,at,level.getRandom().nextLong());}
        WandPreparation(ServerLevel level,Vec3 at,long seed){this.at=at;this.seed=seed;}
        void tick(GameController game,ServerLevel level){if(future==null)future=game.maps().preparePure(this::calculate);}
        boolean ready(){return future!=null&&future.isDone();}
        net.minecraft.world.level.block.state.BlockState original(BlockPos pos){return Blocks.AIR.defaultBlockState();}
        private Geometry calculate(){return calculate(true);}
        private Geometry calculate(boolean mayCancel){var shape=wand(at);shape.seed=seed;return Geometry.prepare(shape,()->mayCancel&&cancelled);}
        Geometry finish(ServerLevel level){
            if(future!=null&&future.isDone()&&!future.isCompletedExceptionally())return future.getNow(null);
            cancelled=true;if(future!=null)future.cancel(false);return calculate(false);
        }
        void cancel(){cancelled=true;if(future!=null)future.cancel(false);}
    }
    /** Immutable geometry only: safe for the shared preparation worker, with no world reads. */
    static final class Geometry {
        private record Section(int x,int y,int z,long[] mask){}
        final Vec3 origin;private final java.util.List<Section> sections;
        final WandPreparation snapshot;final it.unimi.dsi.fastutil.longs.LongSet selected;long seed;
        private Geometry(Vec3 origin,java.util.List<Section> sections){this(origin,sections,null,null);}
        private Geometry(Vec3 origin,java.util.List<Section> sections,WandPreparation snapshot,it.unimi.dsi.fastutil.longs.LongSet selected){
            this.origin=origin;this.sections=java.util.List.copyOf(sections);this.snapshot=snapshot;this.selected=selected;
        }
        static Geometry prepare(Shape shape){return prepare(shape,()->false);}
        static Geometry prepare(Shape shape,java.util.function.BooleanSupplier cancelled){
            var result=new java.util.ArrayList<Section>();
            for(int x=shape.lowX>>4;x<=shape.highX>>4;x++)for(int z=shape.lowZ>>4;z<=shape.highZ>>4;z++)for(int y=shape.lowY>>4;y<=shape.highY>>4;y++){
                if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
                var mask=new java.util.BitSet(4096);
                for(int cell=0;cell<4096;cell++){
                    int bx=(x<<4)+(cell&15),by=(y<<4)+(cell>>8),bz=(z<<4)+((cell>>4)&15);
                    if(bx<shape.lowX||bx>shape.highX||by<shape.lowY||by>shape.highY||bz<shape.lowZ||bz>shape.highZ)continue;
                    double dx=(bx+.5-shape.center.x)/shape.horizontal,dy=(by+.5-shape.center.y)/shape.vertical,dz=(bz+.5-shape.center.z)/shape.horizontal;
                    if(shape.contains(dx,dy,dz,Math.sqrt(dx*dx+dy*dy+dz*dz)))mask.set(cell);
                }
                if(!mask.isEmpty())result.add(new Section(x,y,z,mask.toLongArray()));
            }
            var geometry=new Geometry(shape.origin,result);geometry.seed=shape.seed;return geometry;
        }
    }
    /** Direction-coherent wand bowl; point nukes retain their separate source template/ray composition. */
    static final class Shape {
        final Vec3 origin,center;final double horizontal,vertical;final float power;long seed;final boolean exterior;
        final int lowX,highX,lowY,highY,lowZ,highZ;
        int chunkX,chunkZ,sectionY,cell,visited;boolean done;
        net.minecraft.world.level.ServerExplosion nativeApplication;
        Occlusion walls;
        Shape(Vec3 center,double horizontal,double vertical,float power){
            this(center,center,horizontal,vertical,power);
        }
        private Shape(Vec3 origin,Vec3 center,double horizontal,double vertical,float power){this(origin,center,horizontal,vertical,power,false);}
        private Shape(Vec3 origin,Vec3 center,double horizontal,double vertical,float power,boolean exterior){
            this.exterior=exterior;double extent=exterior?3.5:1.12;
            this.origin=origin;this.center=center;this.horizontal=horizontal;this.vertical=vertical;this.power=power;seed=BlockPos.containing(center).asLong();
            lowX=(int)Math.floor(center.x-horizontal*extent);highX=(int)Math.ceil(center.x+horizontal*extent);
            lowY=(int)Math.floor(center.y-vertical*extent);highY=(int)Math.ceil(center.y+vertical*extent);
            lowZ=(int)Math.floor(center.z-horizontal*extent);highZ=(int)Math.ceil(center.z+horizontal*extent);
            walls=new Occlusion(origin,horizontal*.3);
            chunkX=lowX>>4;chunkZ=lowZ>>4;sectionY=lowY>>4;
        }
        boolean contains(double dx,double dy,double dz,double radius){
            if(NuclearTerrain.contains(seed,dx,dy,dz,radius))return true;
            if(!exterior||radius>=3.5)return false;
            double density=.85*Math.exp(-1.15*Math.max(0,radius-.8));
            if(radius>3)density*=(3.5-radius)/.5;
            int x=(int)Math.floor(center.x+dx*horizontal),y=(int)Math.floor(center.y+dy*vertical),z=(int)Math.floor(center.z+dz*horizontal);
            return unit(seed^BlockPos.asLong(x,y,z)^0x52dce729L)<density;
        }
        Shape copy(){var next=new Shape(origin,center,horizontal,vertical,power,exterior);next.seed=seed;next.chunkX=chunkX;next.chunkZ=chunkZ;
            next.sectionY=sectionY;next.cell=cell;next.visited=visited;next.done=done;next.walls=walls.copy();return next;}
        /** Read current block states at the deadline, including blocks placed after preparation started. */
        java.util.List<BlockPos> preparedPositions(GameController game,ServerLevel level,Geometry geometry){
            if(!origin.equals(geometry.origin))throw new IllegalArgumentException("Prepared blast belongs to another origin");
            if(geometry.snapshot==null)seed=geometry.seed;
            var positions=new java.util.ArrayList<BlockPos>();var coverage=LbcExplosionSuppression.coverage(game,level);
            if(coverage.contains(origin))return positions;
            var probe=new BlockPos.MutableBlockPos();
            for(var part:geometry.sections){
                if((part.y<<4)+15<level.getMinY()||(part.y<<4)>=level.getMaxY())continue;
                var chunk=level.getChunkSource().getChunkNow(part.x,part.z);if(chunk==null)continue;
                var section=chunk.getSection(level.getSectionIndex(part.y<<4));if(section.hasOnlyAir())continue;
                for(int word=0;word<part.mask.length;word++)for(long bits=part.mask[word];bits!=0;bits&=bits-1){
                    int cell=(word<<6)+Long.numberOfTrailingZeros(bits);int x=(part.x<<4)+(cell&15),y=(part.y<<4)+(cell>>8),z=(part.z<<4)+((cell>>4)&15);
                    probe.set(x,y,z);var state=section.getBlockState(x&15,y&15,z&15);
                    boolean accept;
                    if(geometry.snapshot==null)accept=admitted(level,probe,state,coverage);
                    else if(state==geometry.snapshot.original(probe))accept=geometry.selected.contains(probe.asLong())&&!state.isAir()&&!coverage.blocks(probe)&&state.getDestroySpeed(level,probe)>=0;
                    else accept=admitted(level,probe,state,coverage); // Newly placed blocks retain current resistance and core destruction.
                    if(accept){walls.remember(level,probe,state);positions.add(probe.immutable());}
                }
            }
            return positions;
        }
        /** One native block-interaction call; discovery was prepared and there is no extra entity damage. */
        void applyPrepared(GameController game,ServerLevel level,LbcIndirectKills.Owner cause,java.util.List<BlockPos> positions,
                java.util.function.Supplier<net.minecraft.world.level.ServerExplosion> factory,java.util.function.Consumer<Runnable> sourceContext){
            var coverage=LbcExplosionSuppression.coverage(game,level);if(coverage.contains(origin))return;
            if(nativeApplication==null)nativeApplication=factory.get();
            if(nativeApplication.getBlockInteraction()==net.minecraft.world.level.Explosion.BlockInteraction.KEEP)return;
            var bounds=new net.minecraft.world.phys.AABB(lowX,lowY,lowZ,highX+1,highY+1,highZ+1);
            for(var pos:positions)bounds=bounds.minmax(new net.minecraft.world.phys.AABB(pos));
            var area=bounds;
            sourceContext.accept(()->game.items().indirectKills().withSupportCandidates(level,area,
                    ()->game.items().indirectKills().trapChanges(level,cause,()->{
                        var current=LbcExplosionSuppression.coverage(game,level);if(current.contains(origin))return;
                        positions.removeIf(current::blocks);if(!positions.isEmpty())((BlockEffects)nativeApplication).skywars2$applyNuclearBlocks(positions);
                    })));
            done=true;
        }
        private boolean admitted(ServerLevel level,BlockPos pos,net.minecraft.world.level.block.state.BlockState state,LbcExplosionSuppression.Coverage coverage){
            if(state.isAir()||coverage.blocks(pos)||state.getDestroySpeed(level,pos)<0)return false;
            double dx=(pos.getX()+.5-center.x)/horizontal,dy=(pos.getY()+.5-center.y)/vertical,dz=(pos.getZ()+.5-center.z)/horizontal;
            double radius=Math.sqrt(dx*dx+dy*dy+dz*dz);if(!contains(dx,dy,dz,radius))return false;
            float resistance=Math.max(state.getBlock().getExplosionResistance(),state.getFluidState().getExplosionResistance());
            return unit(seed^pos.asLong())<breakChance(resistance,radius,power);
        }
        /** No tickets, no forced chunks, no block snapshots/NBT, one read/write per admitted cell. */
        boolean step(GameController game,ServerLevel level,LbcIndirectKills.Owner cause){
            return step(game,level,cause,null,Runnable::run);
        }
        /** The restored job lazily creates one new application object from rebound sources. */
        boolean step(GameController game,ServerLevel level,LbcIndirectKills.Owner cause,
                java.util.function.Supplier<net.minecraft.world.level.ServerExplosion> factory,
                java.util.function.Consumer<Runnable> sourceContext){
            var coverage=LbcExplosionSuppression.coverage(game,level);
            if(coverage.contains(origin)){done=true;return true;}
            if(factory!=null&&nativeApplication==null)nativeApplication=factory.get();
            if(nativeApplication!=null&&nativeApplication.getBlockInteraction()==net.minecraft.world.level.Explosion.BlockInteraction.KEEP){done=true;return true;}
            boolean[] complete={done};
            sourceContext.accept(()->game.items().indirectKills().withSupportCandidates(level,new net.minecraft.world.phys.AABB(lowX,lowY,lowZ,highX+1,highY+1,highZ+1),
                    ()->game.items().indirectKills().trapChanges(level,cause,()->{
                        java.util.List<BlockPos> admitted=nativeApplication==null?null:new java.util.ArrayList<>();
                        complete[0]=stepBlocks(level,admitted,coverage);
                        if(admitted!=null&&!admitted.isEmpty()){
                            // Native application can trigger callbacks. Re-snapshot current TECH fields at its actual boundary.
                            var application=LbcExplosionSuppression.coverage(game,level);
                            if(application.contains(origin)){done=true;complete[0]=true;}
                            else{admitted.removeIf(application::blocks);if(!admitted.isEmpty())((BlockEffects)nativeApplication).skywars2$applyNuclearBlocks(admitted);}
                        }
                    })));
            return complete[0];
        }
        private boolean stepBlocks(ServerLevel level,java.util.List<BlockPos> admitted,LbcExplosionSuppression.Coverage coverage){
            if(done)return true;
            var probe=new BlockPos.MutableBlockPos();int budget=CELLS_PER_STEP;visited=0;
            while(budget>0&&!done){
                var chunk=level.getChunkSource().getChunkNow(chunkX,chunkZ);
                int y=sectionY<<4;
                if(chunk==null||y+15<level.getMinY()||y>level.getMaxY()) {advanceSection();continue;}
                var section=chunk.getSection(level.getSectionIndex(Math.max(y,level.getMinY())));
                if(section.hasOnlyAir()){advanceSection();continue;}
                while(cell<4096&&budget-->0){
                    int x=(chunkX<<4)+(cell&15),by=y+((cell>>8)&15),z=(chunkZ<<4)+((cell>>4)&15);cell++;visited++;
                    if(x<lowX||x>highX||by<lowY||by>highY||z<lowZ||z>highZ||by<level.getMinY()||by>level.getMaxY())continue;
                    var state=section.getBlockState(x&15,by&15,z&15);probe.set(x,by,z);
                    if(!admitted(level,probe,state,coverage))continue;
                    walls.remember(level,probe,state);
                    if(admitted==null)LbcDropBlocks.setBlock(level,probe,Blocks.AIR.defaultBlockState(),true);
                    else admitted.add(probe.immutable());
                }
                if(cell>=4096)advanceSection();
            }
            return done;
        }
        private void advanceSection(){cell=0;if(++sectionY>highY>>4){sectionY=lowY>>4;
            if(++chunkZ>highZ>>4){chunkZ=lowZ>>4;if(++chunkX>highX>>4)done=true;}}}
    }
    /** A direction keeps one cutoff: ray-length variation roughens the boundary, not its interior. */
    static boolean contains(long seed,double dx,double dy,double dz,double radius){
        if(radius<=.80)return true;if(radius>1.12)return false;
        int x=(int)Math.floor(dx/radius*13),y=(int)Math.floor(dy/radius*13),z=(int)Math.floor(dz/radius*13);
        double density=1/(1+Math.exp((radius-1)*28));
        return unit(seed^BlockPos.asLong(x,y,z))<density;
    }
    static double breakChance(float resistance,double radius,float power){
        // Native rays spend this resistance before admitting a block. Even their
        // strongest possible initial energy cannot break a block above this bound.
        if((Math.max(0,resistance)+.3f)*.3f>=power*1.3f)return 0;
        // Geometry already chooses each ray's endpoint. An independent coin toss
        // per ordinary block would perforate the entire bowl again.
        if(resistance<=6)return 1;
        return 1/(1+Math.max(0,resistance)/(96*Math.max(.2,1-radius)));
    }
    static double unit(long value){
        value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;value=(value^(value>>>27))*0x94d049bb133111ebL;
        return ((value^(value>>>31))>>>11)*0x1.0p-53;
    }
}
