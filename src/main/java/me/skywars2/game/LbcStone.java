package me.skywars2.game;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Каменные заряды и четыре Academy-режима; расход маны и ввод остаются в LbcWands. */
public final class LbcStone {
    public static final EntityType<Charge> TYPE=registerType();
    private static final Identifier[] ROCKS={Identifier.parse("lbc:air11x11"),Identifier.parse("lbc:stone9x9"),
            Identifier.parse("lbc:iron_ore7x7"),Identifier.parse("lbc:obsidian5x5")};
    private static final Identifier SPIKES=Identifier.parse("lbc:spikes10x10x10");
    private static final Block[] BRIDGE={Blocks.CRYING_OBSIDIAN,Blocks.OBSIDIAN,Blocks.BLACKSTONE,
            Blocks.SMOOTH_BASALT,Blocks.BASALT,Blocks.DEEPSLATE_GOLD_ORE,Blocks.DEEPSLATE_IRON_ORE,
            Blocks.DEEPSLATE,Blocks.TUFF,Blocks.GOLD_ORE,Blocks.IRON_ORE,Blocks.STONE,
            Blocks.DRIPSTONE_BLOCK,Blocks.CALCITE,Blocks.DIORITE};
    private static final float[] INTEGRITY={.25f,.2f,.3f,.3f};
    private static final BlockParticleOption STONE_PARTICLES=new BlockParticleOption(ParticleTypes.BLOCK,Blocks.OBSIDIAN.defaultBlockState());
    private static final BlockParticleOption SPIKE_PARTICLES=new BlockParticleOption(ParticleTypes.BLOCK,Blocks.DRIPSTONE_BLOCK.defaultBlockState());
    private final GameController game;
    private final Set<Charge> charges=new LinkedHashSet<>();
    private final ArrayList<Charge> batch=new ArrayList<>();

    private static EntityType<Charge> registerType() {
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:stone_charge"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Charge>of(Charge::new,MobCategory.MISC)
                .sized(.1f,.1f).clientTrackingRange(8).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type); return type;
    }
    static void register() { }
    /** Нативный Projectile хранит owner и velocity, поэтому электроотражение не требует адаптера. */
    public static final class Charge extends Projectile implements PolymerEntity {
        int age;
        int lifetime=200;
        Charge(EntityType<? extends Charge> type,Level level) { super(type,level); }
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
        @Override public EntityType<?> getPolymerEntityType(PacketContext context) { return EntityTypes.MARKER; }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean isPushedByFluid() { return false; }
    }
    LbcStone(GameController game) { this.game=game; }
    /** DFU и чтение NBT прогреваются при загрузке ресурсов, а не при первом попадании в бою. */
    public void prepare() {
        var templates=game.server().getStructureTemplateManager();
        for(var id:ROCKS) templates.get(id);
        templates.get(SPIKES);
    }
    public int count() { return charges.size(); }
    List<Charge> snapshot() { return List.copyOf(charges); }
    void loaded(Entity entity) {
        if(entity instanceof Charge charge) {
            if(!game.items().currentEpoch(charge) || !boundSource(charge)&&(charge.level()!=game.maps().level()
                    || !(charge.getOwner() instanceof ServerPlayer))) charge.discard();
            else charges.add(charge);
        }
    }
    void unloaded(Entity entity) {
        if(entity instanceof Charge charge) {
            charges.remove(charge);
            if(!charge.isRemoved()) charge.discard();
        }
    }
    public void cast(ServerPlayer owner,int mode) {
        if(!game.canUseGameplayItems(owner) || sourceLevel(owner)!=game.maps().level()) return;
        sourceSound(owner,owner.getEyePosition(),"minecraft:block.amethyst_cluster.break",1,1,0);
        switch(mode) {
            case 0 -> {
                castAt(owner,owner.getEyePosition(),owner.getLookAngle(),true);
            }
            case 2 -> shield(owner);
            case 3 -> bridge(owner);
            default -> ray(owner,mode==4);
        }
    }
    void emitAt(ServerPlayer owner,int mode,Vec3 origin,Vec3 direction){
        switch(mode){
            case 0->castAt(owner,origin,direction,false,200);
            case 1->rayAt(owner,origin,direction,false);
            case 2->bridgeAt(owner,origin,direction);
            case 3->shield(owner);
            case 4->rayAt(owner,origin,direction,true);
            default->throw new IllegalArgumentException("Stone producer mode "+mode);
        }
    }
    void castAt(ServerPlayer owner,Vec3 origin,Vec3 direction){castAt(owner,origin,direction,false);}
    Charge flying(ServerPlayer owner,Vec3 origin,Vec3 direction) {
        return castAt(owner,origin,direction,false,80);
    }
    private void castAt(ServerPlayer owner,Vec3 origin,Vec3 direction,boolean playerPose){
        castAt(owner,origin,direction,playerPose,200);
    }
    private Charge castAt(ServerPlayer owner,Vec3 origin,Vec3 direction,boolean playerPose,int lifetime){
                var charge=new Charge(TYPE,sourceLevel(owner));
                charge.setOwner(nativeSourceOwner(owner)); charge.setPos(origin); charge.lifetime=lifetime;
                charge.setYRot(playerPose?owner.getYRot():(float)Math.toDegrees(Math.atan2(-direction.x,direction.z))); charge.setXRot(playerPose?owner.getXRot():(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())));
                charge.setNoGravity(true); charge.setDeltaMovement(lifetime==80?direction.normalize().scale(1.6):direction);
                charge.entityTags().add("magic"); game.items().markCurrentItemEpoch(charge);
                if(sourceLevel(owner).addFreshEntity(charge)) charges.add(charge);
                return charge;
    }
    void shield(ServerPlayer owner) {
        owner.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,100,4),owner);
        sourceLevel(owner).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.BEDROCK.defaultBlockState()),
                owner.getX(),owner.getY()+1,owner.getZ(),30,.3,.6,.4,0);
    }
    void ray(ServerPlayer owner,boolean spike) {rayAt(owner,owner.getEyePosition(),owner.getLookAngle(),spike);}
    private void rayAt(ServerPlayer owner,Vec3 from,Vec3 direction,boolean spike) {
        Vec3 end=from.add(direction.scale(150));
        double boundary=game.items().magic().suppressionFraction(sourceLevel(owner),from,end);
        if(Double.isFinite(boundary))end=from.lerp(end,boundary);
        Vec3 limit=end;
        var hit=game.items().wands().firstHit(owner,from,end);
        if(hit!=null) end=hit.getLocation();
        double distance=from.distanceTo(end);
        for(double step=0;step<=distance;step+=.5) {
            Vec3 point=from.add(direction.scale(step));
            sourceLevel(owner).sendParticles(spike ? SPIKE_PARTICLES : STONE_PARTICLES,true,false,point.x,point.y,point.z,1,.05,.05,.05,0);
            sourceSound(owner,point,"minecraft:block.amethyst_cluster.break",1,.1f,0);
        }
        if(hit!=null && (!Double.isFinite(boundary)||from.distanceToSqr(hit.getLocation())<from.distanceToSqr(limit)-1e-9)
                && (!(hit instanceof BlockHitResult wall) || sourceLevel(owner).hasChunkAt(wall.getBlockPos()))) impact(owner,end,spike);
    }
    /** Проверка по всему отрезку за тик; частота частиц не задаёт геометрию попадания. */
    void tick() {
        batch.addAll(charges);
        try {
            for(var charge:batch) {
                try(var sourceScope=game.items().spellSources().scope(charge)){
                var owner=game.items().spellSources().author(charge);
                if(!liveSourceCharge(charge,owner)) {
                    charge.discard(); continue;
                }
                if(game.items().timeStop().stopped((net.minecraft.server.level.ServerLevel)charge.level()) || (game.items().spellSources().current()!=null?charge:owner).entityTags().contains("time_stopped")) continue;
                Vec3 from=charge.position(),to=from.add(charge.getDeltaMovement());
                if(!sourceLevel(owner).hasChunkAt(BlockPos.containing(from)) || game.items().magic().suppressedAt(sourceLevel(owner),from)) {
                    charge.discard(); continue;
                }
                var hit=game.items().wands().firstHit(owner,from,to);
                if(hit!=null) to=hit.getLocation();
                charge.setPos(to);
                if(charge.lifetime==80) {
                    double distance=from.distanceTo(to);Vec3 direction=to.subtract(from).normalize();
                    for(double step=0;step<=distance+1e-7;step+=.4) {
                        Vec3 point=from.add(direction.scale(Math.min(step,distance)));
                        sourceLevel(owner).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK_MARKER,Blocks.STONE.defaultBlockState()),
                                true,false,point.x,point.y,point.z,1,.1,.1,.1,0);
                    }
                } else {
                    sourceLevel(owner).sendParticles(STONE_PARTICLES,true,false,to.x,to.y,to.z,1,.2,.2,.2,0);
                    sourceLevel(owner).sendParticles(STONE_PARTICLES,to.x,to.y,to.z,10,.2,.2,.2,0);
                    sourceSound(owner,to,"minecraft:block.stone.break",.1f,1,0);
                }
                if(hit!=null) {
                    charge.discard(); // Повторное попадание в собственные созданные блоки исключено.
                    if(!(hit instanceof BlockHitResult wall) || sourceLevel(owner).hasChunkAt(wall.getBlockPos())) {
                        if(charge.lifetime==80) {
                            game.items().indirectKills().fillTrap(owner,BlockPos.containing(to.add(-1.5,-1.5,-1.5)),
                                    BlockPos.containing(to.add(1.5,1.5,1.5)),Blocks.STONE.defaultBlockState());
                            sourceSound(owner,to,"minecraft:block.stone.place",1,1,0);
                        } else impact(owner,to,false);
                    }
                } else if(++charge.age>=charge.lifetime) charge.discard();
                }
            }
        } finally { batch.clear(); charges.removeIf(Entity::isRemoved); }
    }
    /** Камни сохраняют исходную integrity, шипы ставятся целиком; автор блоков учитывается общим helper. */
    private void impact(ServerPlayer owner,Vec3 point,boolean spike) {
        var pos=BlockPos.containing(point);
        game.items().indirectKills().trapChanges(owner,()-> {
            if(spike) game.maps().placeLoadedTemplate(owner,SPIKES,pos.offset(-4,-1,-4),1f);
            else for(int i=0;i<ROCKS.length;i++) game.maps().placeLoadedTemplate(owner,ROCKS[i],pos.offset(-5+i,-5+i,-5+i),INTEGRITY[i]);
        });
        for(float pitch:new float[]{0,.5f,1,1.5f,2}) sourceSound(owner,point,"minecraft:block.stone.place",2,pitch,0);
        if(spike) {
            var damage=new DamageSource(sourceLevel(owner).registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),owner);
            for(var target:sourceLevel(owner).getEntitiesOfClass(LivingEntity.class,new AABB(point,point).inflate(5),
                    target->target.position().distanceToSqr(point)<=25 && game.items().lifetimes().target(owner,target)))
                target.hurtServer(sourceLevel(owner),damage,5);
        }
    }
    void bridge(ServerPlayer owner) {bridgeAt(owner,owner.position().add(0,-1,0),owner.getLookAngle());}
    private void bridgeAt(ServerPlayer owner,Vec3 origin,Vec3 forward) {
        float yaw=(float)Math.toDegrees(Math.atan2(-forward.x,forward.z));
        Vec3 left=Vec3.directionFromRotation(0,yaw-90);
        for(int i=0;i<BRIDGE.length;i++) {
            var a=BlockPos.containing(origin.add(left.scale(-1)).add(forward.scale(i*2)));
            var b=BlockPos.containing(origin.add(left).add(forward.scale(i*2+2.9)));
            game.items().indirectKills().fillTrap(owner,a,b,BRIDGE[i].defaultBlockState());
        }
    }
    public void stop() { var pending=List.copyOf(charges); charges.clear(); pending.forEach(Entity::discard); }
    Charge sourceEmitter(net.minecraft.server.level.ServerLevel level,Vec3 origin,Vec3 direction,LbcSpellSource.Source source){try(var scope=game.items().spellSources().scope(source,level)){return flying(game.items().spellSources().authorPlayer(source),origin,direction);}}
    private void sourceSound(ServerPlayer owner,Vec3 at,String sound,float volume,float pitch,double radius){
        if(game.items().spellSources().current()==null)LbcOverload.sound(owner,at,sound,volume,pitch,radius);
        else LbcOverload.sound(sourceLevel(owner),at,sound,volume,pitch,radius,0);
    }
    private net.minecraft.server.level.ServerLevel sourceLevel(ServerPlayer owner){return game.items().spellSources().level(owner);}
    private boolean persistentSource(){var source=game.items().spellSources().current();return source!=null&&source.persistent()&&game.items().currentEpoch(source.epoch())&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&sourceLevel(null)!=null&&LbcDimensionItems.ordinary(sourceLevel(null));}
    private boolean boundSource(Entity entity){var source=game.items().spellSources().source(entity);return source!=null&&game.items().currentEpoch(source.epoch())&&LbcDimensionItems.ordinary(entity.level());}
    private Entity nativeSourceOwner(ServerPlayer owner){var source=game.items().spellSources().current();return source==null?owner:sourceLevel(owner).getEntity(source.emitter());}
    private boolean liveSourceCharge(Entity charge,ServerPlayer owner){return !charge.isRemoved()&&game.items().currentEpoch(charge)&&game.stage()!=GameController.Stage.ENDING&&(persistentSource()||owner!=null&&!owner.isRemoved()&&owner.isAlive()&&owner.level()==charge.level()&&charge.level()==game.maps().level()&&game.canUseGameplayItems(owner)&&!game.isObserver(owner));}

}
