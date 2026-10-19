package me.skywars2.game;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Снаряды Stblind и отложенные серии Gatling/Rain; оплата и выбор режима принадлежат LbcWands. */
public final class LbcBlind {
    public static final EntityType<Charge> TYPE=registerType();
    private static final int ORDINARY_LIFETIME=30, RAIN_LIFETIME=40, CLOUD_LIFETIME=200;
    private final GameController game;
    private final Set<Charge> charges=new LinkedHashSet<>();
    private final ArrayList<Charge> batch=new ArrayList<>();
    private final Map<ServerPlayer,GatlingQueue> gatlings=new LinkedHashMap<>();
    private final ArrayList<ServerPlayer> gatlingBatch=new ArrayList<>();
    private final Set<RainCloud> clouds=new LinkedHashSet<>();
    private final ArrayList<RainCloud> cloudBatch=new ArrayList<>();
    private int tick;

    private static EntityType<Charge> registerType() {
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:blind_charge"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,
                EntityType.Builder.<Charge>of(Charge::new,MobCategory.MISC)
                        .sized(.1f,.1f).clientTrackingRange(8).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type);
        return type;
    }
    static void register() { }

    /** Проектиль нужен как нативный источник урона; клиенту показываем только marker. */
    public static final class Charge extends Projectile implements PolymerEntity {
        int age;
        int lifetime=ORDINARY_LIFETIME;
        boolean phantom;
        InteractionHand trialHand;
        ServerPlayer trialOwner;
        boolean trial;
        Charge(EntityType<? extends Charge> type,Level level) { super(type,level); }
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
        @Override public EntityType<?> getPolymerEntityType(PacketContext context) { return EntityTypes.MARKER; }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean isPushedByFluid() { return false; }
    }

    private static final class GatlingQueue {
        final String epoch;
        long remaining=20;
        int phase;
        GatlingQueue(String epoch) { this.epoch=epoch; }
    }
    private static final class RainCloud {
        final ServerPlayer owner;
        final net.minecraft.server.level.ServerLevel level;
        final Vec3 position;
        final String epoch;
        int age;
        RainCloud(ServerPlayer owner,Vec3 position,String epoch) {
            this.owner=owner; this.level=(net.minecraft.server.level.ServerLevel)owner.level();
            this.position=position; this.epoch=epoch;
        }
    }

    LbcBlind(GameController game) { this.game=game; }
    public int count() { return charges.size(); }
    List<Charge> snapshot() { return List.copyOf(charges); }
    public int queued(ServerPlayer owner) {
        var queue=gatlings.get(owner);
        return queue==null ? 0 : (int)Math.min(Integer.MAX_VALUE,queue.remaining);
    }
    int cloudCount(ServerPlayer owner) { return (int)clouds.stream().filter(cloud->cloud.owner==owner).count(); }
    void loaded(Entity entity) {
        if(entity instanceof Charge charge) {
            if(!game.items().currentEpoch(charge) || charge.level()!=game.maps().level()
                    || !(charge.getOwner() instanceof ServerPlayer)) charge.discard();
            else charges.add(charge);
        }
    }
    void unloaded(Entity entity) {
        if(entity instanceof Charge charge) {
            charges.remove(charge);
            if(!charge.isRemoved()) charge.discard();
        }
    }

    /** Режимы 0..4: обычный, Academy default, phantom, rain и Gatlingator. */
    public void cast(ServerPlayer owner,InteractionHand hand,int mode,ItemStack stack) {
        if(!validOwner(owner)) return;
        Vec3 origin=owner.getEyePosition();
        LbcOverload.sound(owner,origin,"minecraft:entity.squid.squirt",1,1,0);
        switch(mode) {
            case 0 -> spread(owner,hand,origin,5,false,30,20,LbcAcademy.trial(stack));
            case 1 -> spread(owner,hand,origin,7,false,30,20,false);
            case 2 -> spread(owner,hand,origin,4,true,30,20,false);
            case 3 -> rain(owner,origin);
            case 4 -> gatlings.compute(owner,(player,queue)-> {
                if(queue==null || !game.items().currentEpoch(queue.epoch))
                    queue=new GatlingQueue(game.items().epoch());
                else queue.remaining+=20;
                return queue;
            });
            default -> { }
        }
    }

    private void spread(ServerPlayer owner,InteractionHand hand,Vec3 origin,int count,
                        boolean phantom,int yawSpread,int pitchSpread,boolean trial) {
        Vec3 look=owner.getLookAngle();
        Vec3 right=new Vec3(look.z,0,-look.x).normalize();
        if(right.lengthSqr()<1e-8) right=new Vec3(1,0,0);
        Vec3 up=look.cross(right).normalize();
        List<Vec3> offsets=new ArrayList<>(count);
        offsets.add(Vec3.ZERO);
        if(count>=4) { offsets.add(right.scale(.1)); offsets.add(right.scale(-.1)); offsets.add(up.scale(.1)); }
        if(count==5 || count>=7) offsets.add(up.scale(-.1));
        if(count>=7) { offsets.add(look.scale(.1)); offsets.add(look.scale(-.1)); }
        for(Vec3 offset:offsets) {
            float yaw=owner.getYRot()+(owner.getRandom().nextFloat()*2-1)*yawSpread;
            float pitch=owner.getXRot()+(owner.getRandom().nextFloat()*2-1)*pitchSpread;
            spawn(owner,origin.add(offset),Vec3.directionFromRotation(pitch,yaw).scale(1.8),
                    phantom,ORDINARY_LIFETIME,trial?hand:null,trial);
        }
    }

    private void rain(ServerPlayer owner,Vec3 origin) {
        Vec3 end=origin.add(owner.getLookAngle().scale(100));
        if(!owner.level().hasChunkAt(BlockPos.containing(origin))) return;
        HitResult hit=game.items().wands().firstHit(owner,origin,end);
        if(hit==null) return;
        if(hit instanceof BlockHitResult wall && !owner.level().hasChunkAt(wall.getBlockPos())) return;
        Vec3 point=hit.getLocation();
        // Дождь ослепляет игроков у первой естественной точки контакта, сохраняя фильтр союзников.
        for(ServerPlayer target:owner.level().getEntitiesOfClass(ServerPlayer.class,
                new AABB(point,point).inflate(5),player->player!=owner && game.items().wands().target(owner,player)
                        && player.position().distanceToSqr(point)<=25))
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,200,0,false,false),owner);
        LbcOverload.sound(owner,point,"minecraft:entity.squid.squirt",1,1,0);
        var cloud=new RainCloud(owner,point.add(0,10,0),game.items().epoch());
        clouds.add(cloud);
        LbcOverload.sound(owner,cloud.position,"minecraft:entity.squid.squirt",1,1,0);
    }

    private void spawn(ServerPlayer owner,Vec3 origin,Vec3 velocity,boolean phantom,int lifetime,
                       InteractionHand hand,boolean trial) {
        if(!owner.level().hasChunkAt(BlockPos.containing(origin))) return;
        Charge charge=new Charge(TYPE,owner.level());
        charge.setOwner(owner); charge.setPos(origin); charge.setNoGravity(true);
        charge.setDeltaMovement(velocity); charge.setYRot(owner.getYRot()); charge.setXRot(owner.getXRot());
        charge.phantom=phantom; charge.lifetime=lifetime; charge.trialHand=hand;
        charge.trialOwner=trial?owner:null; charge.trial=trial;
        charge.entityTags().add("magic"); charge.entityTags().add("shield_blockable2");
        game.items().markCurrentItemEpoch(charge);
        if(owner.level().addFreshEntity(charge)) charges.add(charge);
    }

    void tick() {
        tick++;
        tickCharges();
        tickClouds();
        tickGatlings();
    }

    private void tickCharges() {
        if(charges.isEmpty()) return;
        batch.addAll(charges);
        try {
            for(Charge charge:batch) {
                if(!charges.contains(charge)) continue;
                if(charge.isRemoved() || !(charge.getOwner() instanceof ServerPlayer owner)
                        || !game.items().currentEpoch(charge) || charge.level()!=game.maps().level()
                        || !validOwner(owner) || owner.level()!=charge.level()) {
                    charges.remove(charge); charge.discard(); continue;
                }
                if(paused(owner)) continue;
                Vec3 from=charge.position(),to=from.add(charge.getDeltaMovement());
                if(!charge.level().hasChunkAt(BlockPos.containing(from))) { charges.remove(charge); charge.discard(); continue; }
                if(game.items().magic().suppressedAt((net.minecraft.server.level.ServerLevel)charge.level(),from)) {
                    charges.remove(charge); charge.discard(); continue;
                }
                var boundary=charge.phantom
                        ? unloadedBoundary((net.minecraft.server.level.ServerLevel)charge.level(),from,to) : null;
                Vec3 collisionEnd=boundary==null?to:boundary.getLocation();
                HitResult hit=charge.phantom ? phantomHit(owner,from,collisionEnd)
                        : game.items().wands().firstHit(owner,from,collisionEnd);
                var shield=game.items().wands().shieldHit(owner,from,hit==null?collisionEnd:hit.getLocation());
                // Legacy shield_block_2 only removes shieldable markers while their contact point is in air.
                if(shield!=null && !charge.level().getBlockState(BlockPos.containing(shield.getLocation())).isAir()) shield=null;
                boolean shieldBlocked=shield!=null
                        && (hit==null || from.distanceToSqr(shield.getLocation())<from.distanceToSqr(hit.getLocation()));
                if(shieldBlocked) hit=shield;
                if(hit instanceof BlockHitResult wall && !charge.level().hasChunkAt(wall.getBlockPos())) {
                    charges.remove(charge); charge.discard(); continue;
                }
                Vec3 end=hit==null?collisionEnd:hit.getLocation();
                charge.setPos(end);
                double distance=from.distanceTo(end);
                owner.level().sendParticles(ParticleTypes.SQUID_INK,true,false,from.x,from.y,from.z,1,.1,.1,.1,0);
                for(double step=.3;step<=distance+1e-7;step+=.3) {
                    Vec3 particle=from.add(end.subtract(from).scale(Math.min(1,step/distance)));
                    owner.level().sendParticles(ParticleTypes.SQUID_INK,true,false,particle.x,particle.y,particle.z,1,.1,.1,.1,0);
                }
                if(!shieldBlocked && hit instanceof EntityHitResult entityHit)
                    blindImpact(charge,owner,entityHit.getEntity(),end);
                if(hit!=null || boundary!=null || ++charge.age>=charge.lifetime) { charges.remove(charge); charge.discard(); }
            }
        } finally { batch.clear(); }
    }

    private HitResult phantomHit(ServerPlayer owner,Vec3 from,Vec3 to) {
        return ProjectileUtil.getEntityHitResult(owner.level(),owner,from,to,new AABB(from,to).inflate(1),
                entity->game.items().lifetimes().target(owner,entity),0);
    }

    /** DDA-проверка короткого шага обрывает траекторию у первого незагруженного блока без загрузки чанков. */
    private BlockHitResult unloadedBoundary(net.minecraft.server.level.ServerLevel level,Vec3 from,Vec3 to) {
        return BlockGetter.traverseBlocks(from,to,level,
                (world,pos)->world.hasChunkAt(pos)?null:Shapes.block().clip(from,to,pos),world->null);
    }

    private void blindImpact(Charge charge,ServerPlayer owner,Entity entity,Vec3 point) {
        if(!(entity instanceof LivingEntity living) || !game.items().lifetimes().target(owner,entity)) return;
        boolean previouslyBlind=living.hasEffect(MobEffects.BLINDNESS);
        var damage=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.GENERIC),charge,owner);
        living.hurtServer(owner.level(),damage,.5f);
        living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,200,0),owner);
        living.addEffect(new MobEffectInstance(MobEffects.WITHER,80,0),owner);
        if(charge.trial && charge.trialHand!=null && charge.trialOwner==owner)
            game.items().academy().blindTrial(owner,charge.trialHand,previouslyBlind);
    }

    private void tickClouds() {
        if(clouds.isEmpty()) return;
        cloudBatch.addAll(clouds);
        try {
            for(RainCloud cloud:cloudBatch) {
                if(!clouds.contains(cloud)) continue;
                if(!validOwner(cloud.owner) || cloud.owner.level()!=cloud.level
                        || !game.items().currentEpoch(cloud.epoch) || !cloud.level.hasChunkAt(BlockPos.containing(cloud.position))
                        || game.items().magic().suppressedAt(cloud.level,cloud.position)) {
                    clouds.remove(cloud); continue;
                }
                if(paused(cloud.owner)) continue;
                if((tick&1)==0) {
                    int x=cloud.owner.getRandom().nextInt(21)-10;
                    int z=cloud.owner.getRandom().nextInt(21)-10;
                    Vec3 drop=new Vec3(Math.floor(cloud.position.x)+x,cloud.position.y,Math.floor(cloud.position.z)+z);
                    cloud.level.sendParticles(ParticleTypes.SQUID_INK,cloud.position.x,cloud.position.y,cloud.position.z,30,8,.1,8,0);
                    LbcOverload.sound(cloud.owner,cloud.position,"minecraft:entity.squid.squirt",1,1,0);
                    spawn(cloud.owner,drop,new Vec3(0,-1.8,0),false,RAIN_LIFETIME,null,false);
                }
                if(++cloud.age>=CLOUD_LIFETIME) clouds.remove(cloud);
            }
        } finally { cloudBatch.clear(); }
    }

    private void tickGatlings() {
        if(gatlings.isEmpty()) return;
        gatlingBatch.addAll(gatlings.keySet());
        try {
            for(ServerPlayer owner:gatlingBatch) {
                var queue=gatlings.get(owner);
                if(queue==null) continue;
                if(!validOwner(owner) || !game.items().currentEpoch(queue.epoch)) { gatlings.remove(owner); continue; }
                if(paused(owner)) continue;
                if(++queue.phase<2) continue;
                queue.phase=0;
                LbcOverload.sound(owner,owner.getEyePosition(),"minecraft:entity.squid.squirt",1,1,0);
                Vec3 direction=Vec3.directionFromRotation(owner.getXRot()+(owner.getRandom().nextFloat()*2-1)*8,
                        owner.getYRot()+(owner.getRandom().nextFloat()*2-1)*8);
                spawn(owner,owner.getEyePosition(),direction.scale(1.8),false,ORDINARY_LIFETIME,null,false);
                if(--queue.remaining<=0) gatlings.remove(owner);
            }
        } finally { gatlingBatch.clear(); }
    }

    private boolean validOwner(ServerPlayer owner) {
        return owner!=null && !owner.isRemoved() && owner.isAlive() && owner.level()==game.maps().level()
                && game.canUseGameplayItems(owner) && !game.isObserver(owner) && game.stage()!=GameController.Stage.ENDING;
    }
    private boolean paused(ServerPlayer owner) {
        return game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped");
    }

    /** Disconnect удаляет только очереди владельца; уже выпущенные заряды снимет обычная owner-проверка. */
    void forget(ServerPlayer owner) {
        gatlings.remove(owner);
        clouds.removeIf(cloud->cloud.owner==owner);
        cloudBatch.removeIf(cloud->cloud.owner==owner);
        gatlingBatch.removeIf(player->player==owner);
    }

    void stop() {
        for(Charge charge:List.copyOf(charges)) charge.discard();
        charges.clear(); batch.clear(); gatlings.clear(); gatlingBatch.clear(); clouds.clear(); cloudBatch.clear();
    }
}
