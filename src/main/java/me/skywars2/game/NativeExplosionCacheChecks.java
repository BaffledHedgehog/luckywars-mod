package me.skywars2.game;

import java.lang.reflect.Method;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.phys.Vec3;

/** Independent vanilla method path and RNG oracle for ordinary native TNT's optimized collector. */
public final class NativeExplosionCacheChecks {
    private NativeExplosionCacheChecks() { }
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Native explosion cache: "+why);}
    /** Observe real ray crossings of an independent Euclidean shell, rather than testing the ray table itself. */
    private static void sphericalContract(){
        int[] bins=new int[8*16];int[] crossings={0};var origin=new Vec3(.5,.5,.5);
        var random=net.minecraft.util.RandomSource.create(73);var oracle=net.minecraft.util.RandomSource.create(73);
        var blocks=BlastRayTrace.traceSpherical(origin,36,random,new BlastRayTrace.Cells(){
            public boolean inside(BlockPos pos){
                var relative=Vec3.atCenterOf(pos).subtract(origin);if(relative.lengthSqr()<12*12)return true;
                var direction=relative.normalize();int band=Math.min(7,(int)((direction.y+1)*4));
                int sector=Math.min(15,(int)((Math.atan2(direction.z,direction.x)+Math.PI)/(Math.PI*2)*16));
                bins[band*16+sector]++;crossings[0]++;return false;
            }
            public net.minecraft.world.level.block.state.BlockState state(BlockPos pos){return Blocks.AIR.defaultBlockState();}
            public float loss(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){return 0;}
            public boolean destroy(BlockPos pos,net.minecraft.world.level.block.state.BlockState state,float power){throw new AssertionError("Air must never enter native block callbacks");}
        });
        require(blocks.isEmpty()&&crossings[0]==BlastRayTrace.rayCount(),"every spherical ray reaches the Euclidean shell; air emits no block callbacks");
        int min=Arrays.stream(bins).min().orElseThrow(),max=Arrays.stream(bins).max().orElseThrow();
        require(min>=3&&max<=20,"equal-solid-angle shell coverage avoids cube-corner concentration: min="+min+" max="+max);
        for(int i=0;i<BlastRayTrace.rayCount();i++)oracle.nextFloat();
        require(random.nextLong()==oracle.nextLong(),"spherical collection preserves the native one-energy-draw-per-ray RNG budget");
        SkyWars2.LOGGER.info("[check] spherical-blast geometry: {} shell crossings,128 equal-solid-angle bins,min={},max={}",crossings[0],min,max);
    }
    /** Real resident terrain, conservative section bounds, exact block/RNG controls in both directions. */
    private static void heightContract(WoolBucketChecks.Fixture f){
        var home=BlockPos.containing(f.home);
        var floor=new BlockPos((home.getX()>>4)*16+8,f.level.getMaxY()-81,(home.getZ()>>4)*16+8);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)f.set(floor.offset(x,0,z),Blocks.STONE.defaultBlockState());
        f.set(floor.offset(2,1,0),Blocks.WATER.defaultBlockState());
        f.set(floor.offset(-2,0,0),Blocks.OBSIDIAN.defaultBlockState());
        long plainReads=0,boundedReads=0;int cases=0;
        for(int delta:new int[]{1,12,-8}){
            var at=Vec3.atCenterOf(floor.above(delta));float radius=12;
            var height=LbcExasperato.blastHeightBounds(f.level,at,radius,true);
            require(height.minY()<=floor.getY()&&height.maxY()>=floor.getY()+1,"resident section envelope includes stone, water and high resistance");
            require(height.minY()>floor.getY()-24&&height.maxY()<floor.getY()+25,"actual sparse high scene has a finite occupied height envelope");
            var blast=new ServerExplosion(f.level,null,null,null,at,radius,false,Explosion.BlockInteraction.DESTROY);
            var calculator=new ExplosionDamageCalculator();
            class HeightView implements BlastRayTrace.Cells {
                final boolean bounded;long reads;
                HeightView(boolean bounded){this.bounded=bounded;}
                public int minY(){return bounded?height.minY():Integer.MIN_VALUE;}
                public int maxY(){return bounded?height.maxY():Integer.MAX_VALUE;}
                public boolean inside(BlockPos pos){return f.level.isInWorldBounds(pos);}
                public net.minecraft.world.level.block.state.BlockState state(BlockPos pos){
                    reads++;var chunk=f.level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
                    return chunk==null?Blocks.AIR.defaultBlockState():chunk.getBlockState(pos);
                }
                public float loss(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
                    var resistance=calculator.getBlockExplosionResistance(blast,f.level,pos,state,state.getFluidState());
                    return resistance.isEmpty()?0:(resistance.get()+.3F)*.3F;
                }
                public boolean destroy(BlockPos pos,net.minecraft.world.level.block.state.BlockState state,float power){return calculator.shouldBlockExplode(blast,f.level,pos,state,power);}
            }
            for(int seed:new int[]{73,91}){
                var originalRandom=net.minecraft.util.RandomSource.create(seed);var actualRandom=net.minecraft.util.RandomSource.create(seed);
                var originalView=new HeightView(false);var actualView=new HeightView(true);
                var original=BlastRayTrace.traceSpherical(at,radius,originalRandom,originalView);
                var actual=BlastRayTrace.traceSpherical(at,radius,actualRandom,actualView);
                require(!original.isEmpty()&&new HashSet<>(actual).equals(new HashSet<>(original)),"exact sparse-scene blocks for upward/downward/inside origins delta="+delta+" seed="+seed);
                require(originalRandom.nextLong()==actualRandom.nextLong(),"early air termination preserves every native ray's random draw");
                plainReads+=originalView.reads;boundedReads+=actualView.reads;cases++;
            }
        }
        require(boundedReads<plainReads*.8,"at least20percent fewer state reads in the real sparse height scene: "+plainReads+" -> "+boundedReads);
        SkyWars2.LOGGER.info("[check] blast-height bounds: {} exact sparse-terrain block/RNG cases, stateReads={} -> {}",cases,plainReads,boundedReads);
    }

    private record FireballCollection(List<?> positions, long nextRandom, int nextInt, long nanos) { }

    private static FireballCollection collectFireball(Method collector, WoolBucketChecks.Fixture f, Vec3 at,
                                                       int power, long seed, boolean sisyphus)
            throws ReflectiveOperationException {
        var fireball = new LargeFireball(f.level, f.owner, Vec3.ZERO, power);
        fireball.setPos(at);
        if (sisyphus) fireball.entityTags().add(LbcHoming.SLOW_TAG);
        var blast = new ServerExplosion(f.level, fireball, null, null, at, power, true, Explosion.BlockInteraction.DESTROY);
        var random = f.level.getRandom();
        random.setSeed(seed);
        long start = System.nanoTime();
        var positions = (List<?>) collector.invoke(blast);
        long nanos = System.nanoTime() - start;
        return new FireballCollection(positions, random.nextLong(), random.nextInt(), nanos);
    }

    /** Keep a full-power native collector inside one warm chunk without applying terrain or entity damage. */
    private static void sisyphusContract(Method collector, WoolBucketChecks.Fixture f, BlockPos terrain)
            throws ReflectiveOperationException {
        var center = new BlockPos((terrain.getX() >> 4) * 16 + 8, terrain.getY() + 20,
                (terrain.getZ() >> 4) * 16 + 8);
        require(!f.level.isDebug() && f.level.isInWorldBounds(center.above(2))
                && f.level.isInWorldBounds(center.below(2))
                && f.level.getChunkSource().getChunkNow(center.getX() >> 4, center.getZ() >> 4) != null,
                "Sisyphus collector scene is entirely inside one resident ordinary chunk");
        var randomState = MatchRandomState.capture(f.level.getRandom());
        try {
            for (var pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
                int shell = Math.max(Math.abs(pos.getX() - center.getX()),
                        Math.max(Math.abs(pos.getY() - center.getY()), Math.abs(pos.getZ() - center.getZ())));
                f.set(pos, (shell == 2 ? Blocks.OBSIDIAN : Blocks.AIR).defaultBlockState());
            }
            var slab = Blocks.OAK_SLAB.defaultBlockState();
            f.set(center.east(), slab.setValue(SlabBlock.WATERLOGGED, true));
            f.set(center.west(), slab);
            f.set(center.below(), Blocks.STONE.defaultBlockState());
            var at = Vec3.atCenterOf(center);
            for (int power : new int[]{4, 100}) {
                long ordinaryNanos = 0, sisyphusNanos = 0;
                for (long seed : new long[]{73, 91}) {
                    var ordinary = collectFireball(collector, f, at, power, seed, false);
                    var sisyphus = collectFireball(collector, f, at, power, seed, true);
                    require(ordinary.positions.contains(center.above()) && ordinary.positions.contains(center.below())
                                    && ordinary.positions.contains(center.west())
                                    && ordinary.positions.contains(center.east()) == (power == 100)
                                    && !ordinary.positions.contains(center.above(2)),
                            "native Sisyphus witness keeps ignition air, cuts stone and distinguishes fluid absorption power=" + power);
                    require(ordinary.positions.equals(sisyphus.positions) && ordinary.nextRandom == sisyphus.nextRandom
                                    && ordinary.nextInt == sisyphus.nextInt,
                            "Sisyphus preserves vanilla fireball positions in order and subsequent RNG power=" + power + " seed=" + seed);
                    ordinaryNanos += ordinary.nanos;
                    sisyphusNanos += sisyphus.nanos;
                }
                SkyWars2.LOGGER.info("[check] Sisyphus native collector power={} seeds=2 ordinaryTotalUs={} taggedTotalUs={}",
                        power, ordinaryNanos / 1_000d, sisyphusNanos / 1_000d);
            }
        } finally {
            randomState.restore(f.level.getRandom());
        }
    }

    public static void run(GameController game){
        sphericalContract();
        String previous=System.getProperty("skywars2.disableExplosionCache");
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var center=BlockPos.containing(f.home.add(0,55,0));var at=Vec3.atCenterOf(center);
            var method=ServerExplosion.class.getDeclaredMethod("calculateExplodedPositions");method.setAccessible(true);
            var tnt=new PrimedTnt(EntityTypes.TNT,f.level);tnt.setPos(at);
            for(int x=(center.getX()-24)>>4;x<=(center.getX()+24)>>4;x++)
                for(int z=(center.getZ()-24)>>4;z<=(center.getZ()+24)>>4;z++)f.level.getChunk(x,z);
            long begin=LbcExasperato.optimizedBlockSearches(),slowNanos=0,fastNanos=0;int cases=0,boundedNativeCases=0;
            for(int scene=0;scene<3;scene++){
                for(var pos:BlockPos.betweenClosed(center.offset(-6,-6,-6),center.offset(6,6,6))) {
                    int pick=Math.floorMod(pos.getX()*17+pos.getY()*31+pos.getZ()*7,7);
                    var block=scene==0?Blocks.AIR:scene==1?Blocks.STONE:pick==0?Blocks.OBSIDIAN:pick<3?Blocks.WATER:pick==3?Blocks.OAK_PLANKS:Blocks.STONE;
                    f.set(pos,block.defaultBlockState());
                }
                for(float radius:new float[]{4,12})for(int seed=0;seed<5;seed++){
                    System.setProperty("skywars2.disableExplosionCache","true");f.level.getRandom().setSeed(seed);
                    long start=System.nanoTime();
                    var original=(List<BlockPos>)method.invoke(new ServerExplosion(f.level,tnt,null,null,at,radius,false,Explosion.BlockInteraction.DESTROY));
                    slowNanos+=System.nanoTime()-start;long expectedRandom=f.level.getRandom().nextLong();
                    System.setProperty("skywars2.disableExplosionCache","false");f.level.getRandom().setSeed(seed);start=System.nanoTime();
                    var actual=(List<BlockPos>)method.invoke(new ServerExplosion(f.level,tnt,null,null,at,radius,false,Explosion.BlockInteraction.DESTROY));
                    fastNanos+=System.nanoTime()-start;
                    if(LbcExasperato.blastHeightBounds(f.level,at,radius,false).minY()!=Integer.MIN_VALUE)boundedNativeCases++;
                    require(new HashSet<>(actual).equals(new HashSet<>(original)),"exact native selected blocks in air/stone/fluids/high-resistance scene="+scene+" seed="+seed);
                    require(f.level.getRandom().nextLong()==expectedRandom,"native random stream consumption preserved");cases++;
                }
            }
            require(boundedNativeCases>=10,"large ordinary native TNT really exercises the finite-height branch");
            heightContract(f);
            require(LbcExasperato.optimizedBlockSearches()-begin==cases,"ordinary TNT really enters optimized collection, including lightshoot column TNT");
            sisyphusContract(method, f, center);
            SkyWars2.LOGGER.info("[check] native finite-height oracle cases={}",boundedNativeCases);
            SkyWars2.LOGGER.info("[check] PASS native-explosion-cache ({} seeded native/original block sets+RNG identical; block-search benchmark native={}ms optimized={}ms; damage/drop pipeline unchanged)",cases,slowNanos/1e6,fastNanos/1e6);
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        finally{if(previous==null)System.clearProperty("skywars2.disableExplosionCache");else System.setProperty("skywars2.disableExplosionCache",previous);}
    }
}
