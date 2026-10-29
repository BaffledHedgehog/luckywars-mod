package me.skywars2.game;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Воздушные ракеты: нативная геометрия полёта и исходные TNT; ввод и расход остаются в LbcWands. */
public final class LbcAir {
    public static final EntityType<Charge> TYPE=registerType();
    private static final DustColorTransitionOptions AIR_PARTICLE=new DustColorTransitionOptions(0x5e5e5e,0xff7f00,3);
    private final GameController game;
    private final Set<Charge> charges=new LinkedHashSet<>();
    private final ArrayList<Charge> batch=new ArrayList<>();

    private static EntityType<Charge> registerType() {
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:air_charge"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,
                EntityType.Builder.<Charge>of(Charge::new,MobCategory.MISC)
                        .sized(.1f,.1f).clientTrackingRange(8).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type);
        return type;
    }
    public static void register() { }

    public static final class Charge extends Projectile implements PolymerEntity {
        int age;
        int sourceLifetime;boolean flyingStyle,emitterStyle;
        int mode;
        InteractionHand trialHand;
        Vec3 origin;
        ServerPlayer trialOwner;
        boolean trial;
        Charge(EntityType<? extends Charge> type,Level level) { super(type,level); }
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
        @Override public EntityType<?> getPolymerEntityType(PacketContext context) { return EntityTypes.MARKER; }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean isPushedByFluid() { return false; }
    }

    LbcAir(GameController game) { this.game=game; }
    public int count() { return charges.size(); }
    List<Charge> snapshot() { return List.copyOf(charges); }

    void loaded(Entity entity) {
        if(entity instanceof Charge charge) {
            if(!game.items().currentEpoch(charge) || !boundSource(charge)&&(charge.level()!=game.maps().level()
                    || !(charge.getOwner() instanceof ServerPlayer))) { cleanup(charge); return; }
            charges.add(charge);
            if(charge.sourceLifetime==0) game.items().lifetimes().track(charge,true);
        }
    }
    void unloaded(Entity entity) {
        if(entity instanceof Charge charge) { charges.remove(charge); cleanup(charge); }
    }

    public void cast(ServerPlayer owner,InteractionHand hand,int mode,ItemStack stack) {
        if(!validOwner(owner) || mode<0 || mode>3) return;
        Vec3 origin=owner.getEyePosition();
        sourceSound(owner,origin,"minecraft:entity.generic.explode",1,1.3f,0);
        if(mode==1) {
            for(int i=0;i<4;i++) {
                float yaw=owner.getYRot()+(owner.getRandom().nextFloat()*2-1)*30;
                float pitch=owner.getXRot()+(owner.getRandom().nextFloat()*2-1)*20;
                spawn(owner,hand,mode,stack,origin,Vec3.directionFromRotation(pitch,yaw));
            }
        } else {
            spawn(owner,hand,mode,stack,origin,owner.getLookAngle());
        }
    }

    void emitAt(ServerPlayer owner,InteractionHand hand,ItemStack stack,int mode,Vec3 origin,Vec3 direction){
        sourceSound(owner,origin,"minecraft:entity.generic.explode",1,1.3f,0);
        if(mode==1)for(int i=0;i<4;i++){
            float yaw=owner.getYRot()+(owner.getRandom().nextFloat()*2-1)*30;
            float pitch=owner.getXRot()+(owner.getRandom().nextFloat()*2-1)*20;
            spawn(owner,hand,mode,ItemStack.EMPTY,origin,Vec3.directionFromRotation(pitch,yaw),200);
        }else spawn(owner,hand,mode,ItemStack.EMPTY,origin,direction,200);
    }
    Charge flying(ServerPlayer owner,InteractionHand hand,ItemStack stack,Vec3 origin,Vec3 direction) {
        return spawn(owner,hand,0,ItemStack.EMPTY,origin,direction,120);
    }
    Charge sourceEmitter(ServerPlayer owner,Vec3 origin,Vec3 direction){var charge=spawn(owner,InteractionHand.MAIN_HAND,0,ItemStack.EMPTY,origin,direction,120);if(charge!=null)charge.emitterStyle=true;return charge;}
    void spawn(ServerPlayer owner,InteractionHand hand,int mode,ItemStack stack,Vec3 origin,Vec3 direction) {
        spawn(owner,hand,mode,stack,origin,direction,0);
    }
    private Charge spawn(ServerPlayer owner,InteractionHand hand,int mode,ItemStack stack,Vec3 origin,Vec3 direction,int lifetime) {
        if(!sourceLevel(owner).hasChunkAt(BlockPos.containing(origin))) return null;
        Charge charge=new Charge(TYPE,sourceLevel(owner));
        charge.setOwner(nativeSourceOwner(owner)); charge.setPos(origin); charge.setNoGravity(true);
        charge.setDeltaMovement(direction.normalize().scale(lifetime==120?.8:speed(mode))); charge.setYRot(lifetime>0?(float)Math.toDegrees(Math.atan2(-direction.x,direction.z)):owner.getYRot());
        charge.setXRot(lifetime>0?(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())):owner.getXRot());
        charge.mode=mode; charge.origin=origin; charge.sourceLifetime=lifetime;charge.flyingStyle=lifetime==120;
        if(mode==0 && LbcAcademy.trial(stack)) {
            charge.trial=true; charge.trialHand=hand; charge.trialOwner=owner;
        }
        charge.entityTags().add("magic");
        game.items().markCurrentItemEpoch(charge);
        if(sourceLevel(owner).addFreshEntity(charge)) {
            charges.add(charge);
            if(lifetime==0) game.items().lifetimes().track(charge,true);
        }
        return charge;
    }

    private double speed(int mode) { return switch(mode) { case 2 -> 6.3; case 3 -> 1.6; default -> 1; }; }

    void tick() {
        if(charges.isEmpty()) return;
        batch.addAll(charges);
        try {
            // Прямой взрыв может завершить матч и очистить batch прямо из damage callback.
            for(int index=0;index<batch.size();index++) {
                Charge charge=batch.get(index);
                try(var sourceScope=game.items().spellSources().scope(charge)){
                if(!charges.contains(charge)) continue;
                var owner=game.items().spellSources().author(charge);
                if(!liveSourceCharge(charge,owner)) { remove(charge); continue; }
                if(game.items().timeStop().stopped((net.minecraft.server.level.ServerLevel)charge.level()) || (game.items().spellSources().current()!=null?charge:owner).entityTags().contains("time_stopped")) continue;
                Vec3 from=charge.position();
                if(!sourceLevel(owner).hasChunkAt(BlockPos.containing(from))
                        || game.items().magic().suppressedAt((net.minecraft.server.level.ServerLevel)charge.level(),from)) {
                    remove(charge); continue;
                }
                Vec3 to=from.add(charge.getDeltaMovement());
                HitResult hit=game.items().wands().firstHit(owner,from,to);
                if(hit instanceof BlockHitResult wall && !charge.level().hasChunkAt(wall.getBlockPos())) { remove(charge); continue; }
                Vec3 end=hit==null?to:hit.getLocation();
                charge.setPos(end);
                emitTrail(owner,charge,from,end);
                if(hit!=null) {
                    // Отсоединяем заряд до payload: callbacks взрыва не повторяют попадание в ту же цель.
                    remove(charge); detonate(charge,owner,end); continue;
                }
                if(charge.mode==3) home(charge,owner);
                charge.age++;
                if(charge.sourceLifetime>0) { if(charge.age>=charge.sourceLifetime) remove(charge); }
                else game.items().lifetimes().advance(charge,charge.age);
                }
            }
        } finally { batch.clear(); }
    }

    private void emitTrail(ServerPlayer owner,Charge charge,Vec3 from,Vec3 end) {
        if(charge.flyingStyle) {
            double distance=from.distanceTo(end);Vec3 direction=end.subtract(from).normalize();
            for(double step=0;step<=distance+1e-7;step+=.2) {
                Vec3 point=from.add(direction.scale(Math.min(step,distance)));
                sourceLevel(owner).sendParticles(ParticleTypes.SMOKE,true,false,point.x,point.y,point.z,1,.1,.1,.1,0);
            }
            return;
        }
        if(charge.mode==0 || charge.mode==1) {
            sourceLevel(owner).sendParticles(ParticleTypes.EXPLOSION,true,false,end.x,end.y,end.z,1,0,0,0,0);
        } else {
            double step=charge.mode==2?.7:.4, distance=from.distanceTo(end);
            for(double d=0;d<=distance+1e-7;d+=step) {
                Vec3 p=distance<1e-9?from:from.add(end.subtract(from).scale(Math.min(1,d/distance)));
                sourceLevel(owner).sendParticles(AIR_PARTICLE,true,false,p.x,p.y,p.z,1,0,0,0,0);
                sourceSound(owner,p,"minecraft:block.moss_carpet.step",.2f,1,0);
            }
        }
        if(charge.mode==0 || charge.mode==1)
            sourceSound(owner,end,"minecraft:block.moss_carpet.step",.05f,1,0);
    }

    private void home(Charge charge,ServerPlayer owner) {
        Vec3 at=charge.position();
        LivingEntity nearest=null;
        double best=100;
        // Один локальный native query; куб broad phase уточняется до исходного радиуса 10.
        for(var target:sourceLevel(owner).getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(10),
                target->!target.entityTags().contains("strict_map_object") && game.items().wands().target(owner,target)
                        && game.items().lifetimes().target(owner,target))) {
            double distance=target.position().distanceToSqr(at);
            if(distance<=best) { best=distance; nearest=target; }
        }
        if(nearest!=null) charge.setDeltaMovement(nearest.getEyePosition().subtract(at).normalize().scale(1.6));
    }

    private void detonate(Charge charge,ServerPlayer owner,Vec3 point) {
        if(!validOwner(owner) || !game.items().currentEpoch(charge) || !sourceLevel(owner).hasChunkAt(BlockPos.containing(point))) return;
        if(charge.emitterStyle){
            for(int i=0;i<3;i++){
                var tnt=new net.minecraft.world.entity.item.PrimedTnt(sourceLevel(owner),point.x,point.y,point.z,owner);
                tnt.setFuse(1);tnt.setDeltaMovement(Vec3.ZERO);
                if(game.items().spellSources().current()!=null){game.items().markCurrentItemEpoch(tnt);game.items().spellSources().capture(tnt);game.items().timeStop().prepareFrozen(tnt);sourceLevel(owner).addFreshEntity(tnt);}
                else game.items().incomingArtifacts().trackWild(owner,tnt);
            }
            return;
        }
        if(charge.flyingStyle) {
            for(int i=0;i<3;i++) {
                if(!validOwner(owner)||!game.items().currentEpoch(charge))break;
                game.items().exasperato().protectedBlast(owner,sourceLevel(owner),point,4,source->{});
            }
            return;
        }
        float[] offsets=charge.mode==2?new float[]{1,0,-1}:new float[]{0};
        var trial=charge.mode==0 ? game.items().academy().airTrial(owner,charge) : null;
        for(float y:offsets) {
            if(!game.items().currentEpoch(charge) || game.stage()==GameController.Stage.ENDING || !validOwner(owner)) return;
            Vec3 at=point.add(0,y,0);
            if(!sourceLevel(owner).hasChunkAt(BlockPos.containing(at))) continue;
            // Все режимы Sarvatra Mula учитывают защиту TNT; испытание получает тот же источник урона.
            game.items().exasperato().protectedBlast(owner,sourceLevel(owner),at,4,
                    source->game.items().academy().blastSource(source,trial));
        }
    }

    private boolean validOwner(ServerPlayer owner) {
        if(persistentSource())return true;
        return owner!=null && !owner.isRemoved() && owner.isAlive() && sourceLevel(owner)==game.maps().level()
                && game.canUseGameplayItems(owner) && !game.isObserver(owner) && game.stage()!=GameController.Stage.ENDING;
    }
    private void remove(Charge charge) { charges.remove(charge); cleanup(charge); }
    private void cleanup(Charge charge) {
        game.items().lifetimes().unloaded(charge);
        if(!charge.isRemoved()) charge.discard();
    }
    public void stop() {
        for(Charge charge:List.copyOf(charges)) cleanup(charge);
        charges.clear(); batch.clear();
    }
    Charge sourceEmitter(net.minecraft.server.level.ServerLevel level,Vec3 origin,Vec3 direction,LbcSpellSource.Source source){try(var scope=game.items().spellSources().scope(source,level)){return sourceEmitter(game.server().getPlayerList().getPlayer(source.author()),origin,direction);}}
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
