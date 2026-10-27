package me.skywars2.game;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Заряды Ruby и Academy-лучи; медитацию обрабатывает LbcAcademy. */
public final class LbcRuby {
    public static final EntityType<Charge> TYPE=registerType();
    private static final Identifier HOLE=Identifier.parse("lbc:air_sphere_7x7x7");
    static final Identifier MEDITATION=Identifier.parse("lbc:air_sphere_11x11x11");
    private static final int DRILL_SAMPLES_PER_TICK=6;
    private static final double DRILL_STEP=.3;
    private static final double DRILL_HALF=.6;
    private static final double HOLE_RANGE=100;
    private static final double HOLE_SAMPLE=.5;
    private static final double RAY_RANGE=60;
    private static final double RAY_SAMPLE=.2;
    private static final DustParticleOptions DRILL_PARTICLE=new DustParticleOptions(0xD400FF,1);
    private static final DustParticleOptions HOLE_PARTICLE=new DustParticleOptions(0x5C2672,1);
    private static final DustParticleOptions RAY_PARTICLE=new DustParticleOptions(0xCE76F1,1);

    private final GameController game;
    private final Set<Charge> charges=new LinkedHashSet<>();
    private final ArrayList<Charge> batch=new ArrayList<>();

    private static EntityType<Charge> registerType() {
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:ruby_charge"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Charge>of(Charge::new,MobCategory.MISC)
                .sized(.1f,.1f).clientTrackingRange(8).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type);
        return type;
    }
    public static void register() { }

    public static final class Charge extends Projectile implements PolymerEntity {
        int age;
        int sourceLifetime;
        Charge(EntityType<? extends Charge> type,Level level) { super(type,level); }
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
        @Override public EntityType<?> getPolymerEntityType(PacketContext context) { return EntityTypes.MARKER; }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean isPushedByFluid() { return false; }
    }

    LbcRuby(GameController game) { this.game=game; }
    public void prepare() {
        game.server().getStructureTemplateManager().get(HOLE);
        game.server().getStructureTemplateManager().get(MEDITATION);
    }
    public int count() { return charges.size(); }
    List<Charge> snapshot() { return List.copyOf(charges); }

    void loaded(Entity entity) {
        if(entity instanceof Charge charge) {
            if(!game.items().currentEpoch(charge) || charge.level()!=game.maps().level()
                    || !(charge.getOwner() instanceof ServerPlayer)) cleanup(charge);
            else {
                charges.add(charge);
                if(charge.sourceLifetime==0) game.items().lifetimes().track(charge,true);
            }
        }
    }
    void unloaded(Entity entity) {
        if(entity instanceof Charge charge) { charges.remove(charge); cleanup(charge); }
    }

    /** 0 — обычный Ruby; Academy mode 1 использует тот же заряд. */
    public void cast(ServerPlayer owner,int mode) {
        if(!validOwner(owner)) return;
        if(mode<=1) { castDrill(owner); return; }
        if(mode==2) holeRay(owner);
        else if(mode==3) cuttingRay(owner);
    }

    void emitAt(ServerPlayer owner,int mode,Vec3 origin,Vec3 direction){
        switch(mode){
            case 0->castAt(owner,origin,direction,false,70);
            case 1->holeRay(owner,origin,direction.normalize());
            case 2->cuttingRay(owner,origin,direction.normalize());
            default->throw new IllegalArgumentException("Ruby producer mode "+mode);
        }
    }
    private void castDrill(ServerPlayer owner) { castAt(owner,owner.getEyePosition(),owner.getLookAngle(),true); }
    void castAt(ServerPlayer owner,Vec3 origin,Vec3 direction){castAt(owner,origin,direction,false);}
    Charge flying(ServerPlayer owner,Vec3 origin,Vec3 direction) {
        return castAt(owner,origin,direction,false,50);
    }
    private void castAt(ServerPlayer owner,Vec3 origin,Vec3 direction,boolean playerPose){
        castAt(owner,origin,direction,playerPose,0);
    }
    private Charge castAt(ServerPlayer owner,Vec3 origin,Vec3 direction,boolean playerPose,int lifetime){
        if(!owner.level().hasChunkAt(BlockPos.containing(origin))) return null;
        LbcOverload.sound(owner,origin,"minecraft:block.amethyst_cluster.break",1,1,0);
        var charge=new Charge(TYPE,owner.level());
        charge.setOwner(owner); charge.setPos(origin); charge.setNoGravity(true); charge.sourceLifetime=lifetime;
        charge.setYRot(playerPose?owner.getYRot():(float)Math.toDegrees(Math.atan2(-direction.x,direction.z))); charge.setXRot(playerPose?owner.getXRot():(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())));
        charge.setDeltaMovement(direction.normalize().scale(DRILL_SAMPLES_PER_TICK*DRILL_STEP));
        charge.entityTags().add("magic");
        game.items().markCurrentItemEpoch(charge);
        if(owner.level().addFreshEntity(charge)) {
            charges.add(charge);
            if(lifetime==0) game.items().lifetimes().track(charge,true);
        }
        else charge.discard();
        return charge;
    }

    void tick() {
        if(charges.isEmpty()) return;
        batch.addAll(charges);
        try {
            for(Charge charge:batch) {
                if(!charges.contains(charge)) continue;
                if(charge.isRemoved() || !(charge.getOwner() instanceof ServerPlayer owner)
                        || !game.items().currentEpoch(charge) || charge.level()!=game.maps().level()
                        || !validOwner(owner) || owner.level()!=charge.level()) { remove(charge); continue; }
                if(game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped")) continue;
                if(game.items().magic().suppressedAt((net.minecraft.server.level.ServerLevel)charge.level(),charge.position())) {
                    remove(charge); continue;
                }
                Vec3 from=charge.position(),movement=charge.getDeltaMovement();
                double distance=movement.length();
                var visited=new LongOpenHashSet(64);
                boolean loaded=true;
                try {
                    for(int sample=0;sample<=Math.ceil(distance/DRILL_STEP);sample++) {
                        Vec3 point=distance<1e-9?from:from.add(movement.scale(Math.min(1,sample*DRILL_STEP/distance)));
                        if(!owner.level().hasChunkAt(BlockPos.containing(point))) { loaded=false; break; }
                        owner.level().sendParticles(DRILL_PARTICLE,true,false,point.x,point.y,point.z,1,.1,.1,.1,0);
                        digCube(owner,point,DRILL_HALF,visited);
                    }
                } finally { game.items().indirectKills().finishSupportRemoval(); }
                if(!loaded) { remove(charge); continue; }
                charge.setPos(from.add(charge.getDeltaMovement()));
                if(charge.sourceLifetime>0) { if(++charge.age>=charge.sourceLifetime) remove(charge); }
                else game.items().lifetimes().advance(charge,++charge.age);
            }
        } finally { batch.clear(); }
    }

    private void digCube(ServerPlayer owner,Vec3 point,double half,LongOpenHashSet visited) {
        var level=owner.level();
        BlockPos first=BlockPos.containing(point.x-half,point.y-half,point.z-half);
        BlockPos last=BlockPos.containing(point.x+half,point.y+half,point.z+half);
        for(var mutable:BlockPos.betweenClosed(first,last)) {
            if(!visited.add(mutable.asLong()) || !level.hasChunkAt(mutable)) continue;
            if(!level.getBlockState(mutable).isAir()) game.items().wands().destroyRubyCell(owner,mutable.immutable());
        }
    }

    /** Academy mode 2: нативный первый hit, а .5-блоковые samples задают trail/длину. */
    private void holeRay(ServerPlayer owner) {holeRay(owner,owner.getEyePosition(),owner.getLookAngle().normalize());}
    private void holeRay(ServerPlayer owner,Vec3 from,Vec3 direction) {
        var level=owner.level();
        Vec3 limit=from.add(direction.scale(HOLE_RANGE));
        double boundary=game.items().magic().suppressionFraction(level,from,limit);
        if(Double.isFinite(boundary))limit=from.lerp(limit,boundary);
        HitResult hit=game.items().wands().firstHit(owner,from,limit);
        LbcOverload.sound(owner,from,"minecraft:block.amethyst_cluster.break",1,.1f,0);
        Vec3 end=hit==null?limit:hit.getLocation();
        double distance=from.distanceTo(end);
        for(double step=0;step<=distance+1e-7;step+=HOLE_SAMPLE) {
            Vec3 point=from.add(direction.scale(Math.min(distance,step)));
            if(!level.hasChunkAt(BlockPos.containing(point))) return;
            level.sendParticles(HOLE_PARTICLE,true,false,point.x,point.y,point.z,1,0,0,0,0);
        }
        if(hit==null || Double.isFinite(boundary)&&from.distanceToSqr(hit.getLocation())>=from.distanceToSqr(limit)-1e-9
                || (hit instanceof BlockHitResult wall && !level.hasChunkAt(wall.getBlockPos()))) return;
        // На положительной грани floor(hit) указывает уже на соседний воздух; центр нужен внутри встретившегося блока.
        createHole(owner,hit instanceof BlockHitResult ? end.add(direction.scale(1e-5)) : end);
    }

    private void createHole(ServerPlayer owner,Vec3 point) {
        var level=owner.level();
        BlockPos center=BlockPos.containing(point);
        game.items().indirectKills().trapChanges(owner,()-> {
            game.maps().placeLoadedTemplate(owner,HOLE,center.offset(-3,-3,-3),1);
            if(level.hasChunkAt(center)) level.setBlock(center,Blocks.BEDROCK.defaultBlockState(),
                    Block.UPDATE_CLIENTS|Block.UPDATE_SUPPRESS_DROPS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
        });
        level.sendParticles(ParticleTypes.PORTAL,point.x,point.y,point.z,300,1,1,1,0);
        DamageSource damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.OUTSIDE_BORDER),owner);
        var area=new net.minecraft.world.phys.AABB(center).inflate(3);
        for(var target:level.getEntitiesOfClass(LivingEntity.class,area,
                candidate->candidate!=owner && candidate.position().distanceToSqr(point)<=9 && validEnemy(owner,candidate))) {
            if(!validOwner(owner)) break;
            Vec3 destination=point;
            target.teleportTo(destination.x,destination.y,destination.z);
            game.items().indirectKills().displaced(target,owner);
            if(game.items().mana().hudValue(target,"void_resistance")<=0)
                target.hurtServer(level,damage,1);
        }
    }

    /** Academy mode 3 удаляет все блоки по лучу, включая bedrock, без урона существам. */
    private void cuttingRay(ServerPlayer owner) {cuttingRay(owner,owner.getEyePosition(),owner.getLookAngle().normalize());}
    private void cuttingRay(ServerPlayer owner,Vec3 origin,Vec3 direction) {
        var level=owner.level();
        LbcOverload.sound(owner,origin,"minecraft:block.amethyst_cluster.break",1,.1f,0);
        var visited=new LongOpenHashSet(256);
        double boundary=game.items().magic().suppressionFraction(level,origin,origin.add(direction.scale(RAY_RANGE)))*RAY_RANGE;
        try {
            for(double step=0;step<=RAY_RANGE+1e-7&&step<boundary;step+=RAY_SAMPLE) {
                Vec3 point=origin.add(direction.scale(step));
                if(!level.hasChunkAt(BlockPos.containing(point))) break;
                level.sendParticles(RAY_PARTICLE,true,false,point.x,point.y,point.z,1,0,0,0,0);
                digCube(owner,point,.1,visited);
            }
        } finally { game.items().indirectKills().finishSupportRemoval(); }
    }

    private boolean validEnemy(ServerPlayer owner,LivingEntity target) {
        return target.isAlive() && !target.entityTags().contains("strict_map_object")
                && game.items().lifetimes().target(owner,target);
    }
    private boolean validOwner(ServerPlayer owner) {
        return owner!=null && !owner.isRemoved() && owner.isAlive() && owner.level()==game.maps().level()
                && game.canUseGameplayItems(owner) && !game.isObserver(owner) && game.stage()!=GameController.Stage.ENDING;
    }
    private void remove(Charge charge) { charges.remove(charge); cleanup(charge); }
    private void cleanup(Charge charge) {
        game.items().lifetimes().unloaded(charge);
        if(!charge.isRemoved()) charge.discard();
    }
    public void stop() {
        var pending=List.copyOf(charges); charges.clear(); batch.clear(); pending.forEach(this::cleanup);
    }
}
