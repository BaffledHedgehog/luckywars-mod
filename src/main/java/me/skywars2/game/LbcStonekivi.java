package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Каменные слои, Земля и магматификация делят очередь с бюджетом CPU; шаблоны разбираются до боя. */
public final class LbcStonekivi {
    private static final Identifier[] LAYERS={id("stone_sphere_r10"),id("iron_sphere_r8"),
            id("obsidian_sphere_r6"),id("deepslate_sphere_r4"),id("bedrock_sphere_r2")};
    private static final Identifier EARTH=id("earth");
    private static final BlockParticleOption STONE=new BlockParticleOption(ParticleTypes.BLOCK,Blocks.STONE.defaultBlockState());
    private static final BlockParticleOption BEDROCK=new BlockParticleOption(ParticleTypes.BLOCK,Blocks.BEDROCK.defaultBlockState());
    private final GameController game;
    private final ArrayDeque<Work> work=new ArrayDeque<>();
    private volatile Map<Identifier,List<MapManager.TemplateSlice>> templates=Map.of();
    private volatile int preparation;

    /** Шаг не должен читать мир в другом потоке; ready=false означает только ожидание чистого вычисления. */
    interface Work {
        boolean step();
        default boolean ready() { return true; }
        default void cancel() { }
    }
    LbcStonekivi(GameController game) { this.game=game; }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("lbc",path); }
    public void prepare() {
        LbcMagmify.prepare();
        int generation=++preparation;
        templates=Map.of();
        var ids=new ArrayList<>(List.of(LAYERS)); ids.add(EARTH);
        game.maps().prepareSpellTemplates(ids).whenComplete((value,error)-> {
            if(generation!=preparation) return;
            if(error!=null) SkyWars2.LOGGER.error("Stonekivi template preparation failed",error);
            else templates=value;
        });
    }
    public boolean ready() { return templates.size()==6; }
    public int count() { return work.size(); }
    void queuedWork(Work task) { work.add(task); }

    /** Режимы и цены принадлежат прежнему dispatch жезлов; сам луч завершается только на первом hit. */
    void cast(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean upgraded) {
        int mode=upgraded?Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY)
                .copyTag().getIntOr("mode",1),1,4):1;
        castMode(owner,hand,stack,mode);
    }
    void castMode(ServerPlayer owner,InteractionHand hand,ItemStack stack,int mode) {
        Vec3 from=owner.getEyePosition(),direction=Vec3.directionFromRotation(owner.getXRot(),owner.getYRot());
        Vec3 far=from.add(direction.scale(mode==1?150:250));
        double fraction=game.items().magic().suppressionFraction(owner.level(),from,far);
        Vec3 limit=Double.isFinite(fraction)?from.lerp(far,fraction):far;
        var hit=game.items().wands().firstHit(owner,from,limit);
        Vec3 point=hit==null?limit:hit.getLocation();
        double distance=from.distanceTo(point);
        var primary=mode==2?new BlockParticleOption(ParticleTypes.BLOCK,Blocks.GRASS_BLOCK.defaultBlockState())
                :mode==3?new BlockParticleOption(ParticleTypes.BLOCK,Blocks.LAVA.defaultBlockState()):mode==4?BEDROCK:STONE;
        var secondary=mode==2?new BlockParticleOption(ParticleTypes.BLOCK,Blocks.DIRT.defaultBlockState())
                :mode==3?new BlockParticleOption(ParticleTypes.BLOCK,Blocks.MAGMA_BLOCK.defaultBlockState()):BEDROCK;
        for(double d=0;d<=distance;d+=.5) {
            Vec3 p=from.add(direction.scale(d));
            owner.level().sendParticles(primary,true,false,p.x,p.y,p.z,2,.1,.1,.1,0);
            owner.level().sendParticles(secondary,true,false,p.x,p.y,p.z,1,.1,.1,.1,0);
        }
        LbcOverload.sound(owner,"minecraft:block.deepslate.place",1,1,0);
        if(hit==null || hit instanceof BlockHitResult block && !owner.level().hasChunkAt(block.getBlockPos())
                || Double.isFinite(fraction) && point.distanceToSqr(from)>=limit.distanceToSqr(from)-1e-6) return;
        BlockPos center=hit instanceof BlockHitResult block?block.getBlockPos():BlockPos.containing(point);
        if(mode==3 && hit instanceof EntityHitResult)
            center=new BlockPos(center.getX(),owner.level().getHeight(Heightmap.Types.MOTION_BLOCKING,center.getX(),center.getZ())-1,center.getZ());
        switch(mode) {
            case 1 -> {
                var sequence=new ArrayDeque<Work>();
                for(int i=0;i<LAYERS.length;i++) {
                    int radius=10-i*2;
                    sequence.add(template(owner,LAYERS[i],center.offset(-radius,-radius,-radius),.678f));
                }
                BlockPos impact=center;
                sequence.add(()-> { game.items().academy().stonekiviTrial(owner,hand,stack,impact); return true; });
                work.add(new Sequence(sequence));
            }
            case 2 -> work.add(template(owner,EARTH,center.offset(-22,-22,-22),1));
            case 3 -> work.add(LbcMagmify.create(game,owner,center));
            case 4 -> comets(owner,center);
        }
        for(float pitch:new float[]{1,0}) LbcOverload.sound(owner,point,"minecraft:block.deepslate_bricks.break",mode==1?4:7,pitch,0);
    }

    private void comets(ServerPlayer owner,BlockPos center) {
        var sequence=new ArrayDeque<Work>();
        var point=Vec3.atCenterOf(center);
        // (user) Сохраняются также предметы, снаряды и союзники; служебные роли и наблюдатели не являются целями.
        var centers=new java.util.LinkedHashSet<BlockPos>();
        centers.add(center);
        for(var entity:owner.level().getEntities(owner,new AABB(point,point).inflate(15),entity->
                game.items().wands().target(owner,entity,true) && entity.position().distanceToSqr(point)<=225
                        && !entity.entityTags().contains("swrg.center") && !entity.entityTags().contains("swrg.default")
                        && !entity.entityTags().contains("swrg.look") && !entity.entityTags().contains("swrg.midle")
                        && !entity.entityTags().contains("swrg.spawn")))
            centers.add(entity.blockPosition().immutable());
        for(var pos:centers) sequence.add(template(owner,LAYERS[4],pos.offset(-2,-2,-2),1));
        work.add(new Sequence(sequence));
    }

    /** Уже разбитые шаблоны используют прежнюю native-вставку, включая BlockEntity и подавление дропа. */
    private Work template(ServerPlayer owner,Identifier id,BlockPos origin,float integrity) {
        var parts=templates.getOrDefault(id,List.of());
        var placements=new ArrayDeque<Work>();
        long seed=owner.getRandom().nextLong();
        for(var part:parts) {
            var b=part.bounds();
            int minX=origin.getX()+b.minX(),maxX=origin.getX()+b.maxX();
            int minZ=origin.getZ()+b.minZ(),maxZ=origin.getZ()+b.maxZ();
            int minY=Math.max(owner.level().getMinY(),origin.getY()+b.minY());
            int maxY=Math.min(owner.level().getMaxY(),origin.getY()+b.maxY());
            if(minY>maxY) continue;
            for(int cx=minX>>4;cx<=maxX>>4;cx++) for(int cz=minZ>>4;cz<=maxZ>>4;cz++) {
                var bounds=new BoundingBox(Math.max(minX,cx*16),minY,Math.max(minZ,cz*16),
                        Math.min(maxX,cx*16+15),maxY,Math.min(maxZ,cz*16+15));
                placements.add(()-> {
                    if(owner.level()!=game.maps().level() || !owner.level().hasChunkAt(new BlockPos(bounds.minX(),minY,bounds.minZ()))) return true;
                    var settings=new StructurePlaceSettings().setIgnoreEntities(true).setKnownShape(true).setBoundingBox(bounds);
                    if(integrity<1) settings.addProcessor(new BlockRotProcessor(integrity));
                    game.items().indirectKills().trapChanges(owner,
                            ()->game.maps().placeMapSlice(owner.level(),part.template(),origin,settings,seed));
                    return true;
                });
            }
        }
        return new Sequence(placements);
    }
    private static final class Sequence implements Work {
        final ArrayDeque<Work> children;
        Sequence(ArrayDeque<Work> children) { this.children=children; }
        @Override public boolean ready() { return children.isEmpty() || children.peek().ready(); }
        @Override public boolean step() {
            if(children.isEmpty()) return true;
            if(children.peek().step()) children.remove();
            return children.isEmpty();
        }
        @Override public void cancel() { children.forEach(Work::cancel); children.clear(); }
    }
    /** Накопленные касты не теряются: ограничивается работа за тик, а не размер очереди. */
    public void tick() {
        if(game.stage()==GameController.Stage.LOADING || game.stage()==GameController.Stage.ENDING) { stop(); return; }
        if(game.items().wands().timeStopped()) return;
        long deadline=System.nanoTime()+3_000_000;
        int idle=0;
        for(int steps=0;steps<128 && !work.isEmpty() && System.nanoTime()<deadline;steps++) {
            var job=work.remove();
            if(!job.ready()) {
                work.add(job); if(++idle>=work.size()) break;
            } else {
                idle=0;
                if(!job.step()) work.add(job);
            }
        }
    }
    public void stop() { work.forEach(Work::cancel); work.clear(); }
}
