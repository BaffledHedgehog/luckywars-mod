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
    private static final int ORDINARY_LIFETIME=30, RAIN_LIFETIME=20, CLOUD_LIFETIME=200;
    private static final double RAIN_SPEED=1.8, RAIN_RADIUS=10;
    private static final Vec3 RAIN_VELOCITY=new Vec3(0,-RAIN_SPEED,0);
    private static final int RAIN_TARGET_REFRESH=5;
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
        boolean rainDrop;
        int age;
        int lifetime=ORDINARY_LIFETIME;
        boolean phantom;
        boolean emitterStyle;
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
        Vec3 position;
        final String epoch;
        int age;
        int nextTargetTick;
        final ArrayList<LivingEntity> targets=new ArrayList<>();
        RainCloud(ServerPlayer owner,Vec3 position,String epoch) {
            this.owner=owner; this.level=(net.minecraft.server.level.ServerLevel)owner.level();
            this.position=position; this.epoch=epoch;
        }
    }

    LbcBlind(GameController game) { this.game=game; }
    void transformOrigins(net.minecraft.server.level.ServerLevel level,java.util.function.UnaryOperator<Vec3> transform) {
        clouds.removeIf(cloud->{
            if(cloud.level!=level) return false;
            Vec3 next=transform.apply(cloud.position);
            if(next==null) return true;
            cloud.position=next; cloud.targets.clear(); cloud.nextTargetTick=0; return false;
        });
    }

    public int count() { return charges.size(); }
    List<Charge> snapshot() { return List.copyOf(charges); }
    public int queued(ServerPlayer owner) {
        var queue=gatlings.get(owner);
        return queue==null ? 0 : (int)Math.min(Integer.MAX_VALUE,queue.remaining);
    }
    /** One source gatling_fovos point is one queued volley, including an idle player. */
    void addPlayerCounters(ServerPlayer owner) {
        var queue=gatlings.get(owner);
        if(queue==null) { queue=new GatlingQueue(game.items().epoch()); queue.remaining=0; gatlings.put(owner,queue); }
        queue.remaining++;game.items().mana().score(owner,"gatling_fovos").set(queued(owner));
    }
    void adoptRawFoodStatuses(ServerPlayer owner){
        int remaining=game.items().mana().hudValue(owner,"gatling_fovos");if(remaining<=0||!validOwner(owner))return;
        var queue=gatlings.computeIfAbsent(owner,p->new GatlingQueue(game.items().epoch()));queue.remaining=remaining;
    }
    int cloudCount(ServerPlayer owner) { return (int)clouds.stream().filter(cloud->cloud.owner==owner).count(); }
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

    /** Режимы 0..4: обычный, Academy default, phantom, rain и Gatlingator. */
    public void cast(ServerPlayer owner,InteractionHand hand,int mode,ItemStack stack) {
        if(!validOwner(owner)) return;
        Vec3 origin=owner.getEyePosition();
        sourceSound(owner,origin,"minecraft:entity.squid.squirt",1,1,0);
        switch(mode) {
            case 0 -> spread(owner,hand,origin,5,false,30,20,LbcAcademy.trial(stack));
            case 1 -> spread(owner,hand,origin,7,false,30,20,false);
            case 2 -> spread(owner,hand,origin,4,true,30,20,false);
            case 3 -> rain(owner,origin);
            case 4 -> queueGatling(owner);
            default -> { }
        }
    }

    void emitAt(ServerPlayer owner,InteractionHand hand,int mode,Vec3 origin,Vec3 direction){
        sourceSound(owner,origin,"minecraft:entity.squid.squirt",1,1,0);
        if(mode==2)rainAt(owner,origin,direction);
        else spreadAt(owner,hand,origin,mode==1?4:5,mode==1,30,20,false,direction,owner.getLookAngle());
    }
    void queueGatling(ServerPlayer owner) {
        gatlings.compute(owner,(player,queue)-> {
            if(queue==null || !game.items().currentEpoch(queue.epoch))queue=new GatlingQueue(game.items().epoch());
            else queue.remaining+=20;
            return queue;
        });
        game.items().mana().score(owner,"gatling_fovos").set(queued(owner));
    }

    private void spread(ServerPlayer owner,InteractionHand hand,Vec3 origin,int count,
                        boolean phantom,int yawSpread,int pitchSpread,boolean trial) {
        spreadAt(owner,hand,origin,count,phantom,yawSpread,pitchSpread,trial,owner.getLookAngle(),owner.getLookAngle());
    }
    private void spreadAt(ServerPlayer owner,InteractionHand hand,Vec3 origin,int count,
            boolean phantom,int yawSpread,int pitchSpread,boolean trial,Vec3 look,Vec3 aim){
        float aimYaw=(float)Math.toDegrees(Math.atan2(-aim.x,aim.z));
        float aimPitch=(float)-Math.toDegrees(Math.atan2(aim.y,aim.horizontalDistance()));
        Vec3 right=new Vec3(look.z,0,-look.x).normalize();
        if(right.lengthSqr()<1e-8) right=new Vec3(1,0,0);
        Vec3 up=look.cross(right).normalize();
        List<Vec3> offsets=new ArrayList<>(count);
        offsets.add(Vec3.ZERO);
        if(count>=4) { offsets.add(right.scale(.1)); offsets.add(right.scale(-.1)); offsets.add(up.scale(.1)); }
        if(count==5 || count>=7) offsets.add(up.scale(-.1));
        if(count>=7) { offsets.add(look.scale(.1)); offsets.add(look.scale(-.1)); }
        for(Vec3 offset:offsets) {
            float yaw=aimYaw+(owner.getRandom().nextFloat()*2-1)*yawSpread;
            float pitch=aimPitch+(owner.getRandom().nextFloat()*2-1)*pitchSpread;
            spawn(owner,origin.add(offset),Vec3.directionFromRotation(pitch,yaw).scale(1.8),
                    phantom,ORDINARY_LIFETIME,trial?hand:null,trial);
        }
    }

    private void rain(ServerPlayer owner,Vec3 origin) {rainAt(owner,origin,owner.getLookAngle());}
    private void rainAt(ServerPlayer owner,Vec3 origin,Vec3 direction) {
        Vec3 end=origin.add(direction.scale(100));
        if(!sourceLevel(owner).hasChunkAt(BlockPos.containing(origin))) return;
        HitResult hit=game.items().wands().firstHit(owner,origin,end);
        // Source rain preloop draws ink every .4 blocks, including an aim with no terminal hit.
        double distance=origin.distanceTo(hit==null?end:hit.getLocation());
        for(double step=0;step<=distance+1e-7;step+=.4) {
            var trail=origin.add(direction.scale(Math.min(step,distance)));
            if(!sourceLevel(owner).hasChunkAt(BlockPos.containing(trail)))break;
            sourceLevel(owner).sendParticles(ParticleTypes.SQUID_INK,true,false,trail.x,trail.y,trail.z,1,0,0,0,0);
        }
        if(hit==null) return;
        if(hit instanceof BlockHitResult wall && !sourceLevel(owner).hasChunkAt(wall.getBlockPos())) return;
        Vec3 point=hit.getLocation();
        // Дождь ослепляет игроков у первой естественной точки контакта, сохраняя фильтр союзников.
        for(ServerPlayer target:sourceLevel(owner).getEntitiesOfClass(ServerPlayer.class,
                new AABB(point,point).inflate(5),player->player!=owner && game.items().wands().target(owner,player)
                        && player.position().distanceToSqr(point)<=25))
            target.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,200,0,false,false),owner);
        sourceSound(owner,point,"minecraft:entity.squid.squirt",1,1,0);
        var cloud=new RainCloud(owner,point.add(0,10,0),game.items().epoch());
        clouds.add(cloud);
        sourceSound(owner,cloud.position,"minecraft:entity.squid.squirt",1,1,0);
    }

    void spawn(ServerPlayer owner,Vec3 origin,Vec3 velocity,boolean phantom,int lifetime,
                       InteractionHand hand,boolean trial) {
        if(!sourceLevel(owner).hasChunkAt(BlockPos.containing(origin))) return;
        Charge charge=new Charge(TYPE,sourceLevel(owner));
        charge.setOwner(nativeSourceOwner(owner)); charge.setPos(origin); charge.setNoGravity(true);
        charge.setDeltaMovement(velocity); charge.setYRot(owner.getYRot()); charge.setXRot(owner.getXRot());
        charge.phantom=phantom; charge.lifetime=lifetime; charge.trialHand=hand;
        charge.rainDrop=lifetime==RAIN_LIFETIME;
        charge.trialOwner=trial?owner:null; charge.trial=trial;
        charge.entityTags().add("magic"); charge.entityTags().add("shield_blockable2");
        game.items().markCurrentItemEpoch(charge);
        if(sourceLevel(owner).addFreshEntity(charge)) charges.add(charge);
    }
    void sourceEmitter(ServerPlayer owner,Vec3 origin,Vec3 direction){
        var charge=new Charge(TYPE,sourceLevel(owner));charge.setOwner(nativeSourceOwner(owner));charge.setPos(origin);charge.setNoGravity(true);
        charge.setDeltaMovement(direction.normalize().scale(.99));LbcShooters.orient(charge);charge.lifetime=120;charge.emitterStyle=true;
        charge.entityTags().addAll(List.of("magic","shield_blockable"));game.items().markCurrentItemEpoch(charge);
        if(sourceLevel(owner).addFreshEntity(charge))charges.add(charge);
    }

    /** Volos uses one ordinary native charge, with its authored35 active ticks. No wand admission/payment. */
    void spawnFromTrinket(Projectile arrow,Vec3 origin,Vec3 velocity){
        var level=(net.minecraft.server.level.ServerLevel)arrow.level();
        if(!level.hasChunkAt(BlockPos.containing(origin)))return;
        var charge=new Charge(TYPE,level);charge.setOwner(arrow.getOwner());charge.setPos(origin);charge.setNoGravity(true);
        charge.setDeltaMovement(velocity);LbcShooters.orient(charge);charge.lifetime=35;
        charge.entityTags().addAll(List.of("magic","shield_blockable2"));game.items().markCurrentItemEpoch(charge);
        game.items().spellSources().inherit(arrow,charge);game.items().timeStop().prepareFrozen(charge);
        if(level.addFreshEntity(charge))charges.add(charge);
        LbcOverload.sound(level,origin,"minecraft:entity.squid.squirt",1,1,0,0);
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
            // Damage callbacks can stop the match and clear the reusable batch during an impact.
            for(int index=0;index<batch.size();index++) {
                Charge charge=batch.get(index);
                try(var sourceScope=game.items().spellSources().scope(charge)){
                if(!charges.contains(charge)) continue;
                var owner=game.items().spellSources().author(charge);
                if(!liveSourceCharge(charge,owner)) {
                    charges.remove(charge); charge.discard(); continue;
                }
                if(game.items().spellSources().current()!=null?game.items().timeStop().stopped((net.minecraft.server.level.ServerLevel)charge.level())||charge.entityTags().contains("time_stopped"):paused(owner)) continue;
                Vec3 from=charge.position(),to=from.add(charge.getDeltaMovement());
                if(!charge.level().hasChunkAt(BlockPos.containing(from))) { charges.remove(charge); charge.discard(); continue; }
                if(game.items().magic().suppressedAt((net.minecraft.server.level.ServerLevel)charge.level(),from)) {
                    charges.remove(charge); charge.discard(); continue;
                }
                if(charge.emitterStyle){if(!emitterStep(charge,owner)||++charge.age>=charge.lifetime){charges.remove(charge);charge.discard();}continue;}
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
                Vec3 travel=end.subtract(from);
                sourceLevel(owner).sendParticles(ParticleTypes.SQUID_INK,true,false,from.x,from.y,from.z,1,.1,.1,.1,0);
                for(double step=.3;step<=distance+1e-7;step+=.3) {
                    double fraction=Math.min(1,step/distance);
                    sourceLevel(owner).sendParticles(ParticleTypes.SQUID_INK,true,false,
                            from.x+travel.x*fraction,from.y+travel.y*fraction,from.z+travel.z*fraction,1,.1,.1,.1,0);
                }
                if(!shieldBlocked && hit instanceof EntityHitResult entityHit)
                    blindImpact(charge,owner,entityHit.getEntity(),end);
                if(hit!=null || boundary!=null || ++charge.age>=charge.lifetime) { charges.remove(charge); charge.discard(); }
                }
            }
        } finally { batch.clear(); }
    }

    private HitResult phantomHit(ServerPlayer owner,Vec3 from,Vec3 to) {
        return ProjectileUtil.getEntityHitResult(sourceLevel(owner),owner,from,to,new AABB(from,to).inflate(1),
                entity->game.items().lifetimes().target(owner,entity),0);
    }
    private boolean emitterStep(Charge charge,ServerPlayer owner){
        var from=charge.position();var direction=charge.getDeltaMovement().normalize();
        var shield=game.items().wands().shieldHit(owner,from,from.add(direction.scale(.99)));
        for(int i=0;i<4;i++){
            var at=from.add(direction.scale(i*.33));var cell=BlockPos.containing(at);
            if(!sourceLevel(owner).hasChunkAt(cell)||game.items().magic().suppressedAt(sourceLevel(owner),at))return false;
            if(shield!=null&&from.distanceToSqr(shield.getLocation())<=from.distanceToSqr(at))return false;
            sourceLevel(owner).sendParticles(ParticleTypes.SQUID_INK,true,false,at.x,at.y,at.z,1,.1,.1,.1,0);
            for(var target:sourceLevel(owner).getEntitiesOfClass(LivingEntity.class,new AABB(at,at.add(1,1,1)),e->e.position().distanceToSqr(at)<=225&&game.items().wands().target(owner,e))){
                target.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,80,0),owner);target.addEffect(ItemEffectLevels.create(MobEffects.WITHER,80,1),owner);
            }
            if(!sourceLevel(owner).getBlockState(cell).isAir())return false;
        }
        charge.setPos(from.add(direction.scale(.99)));return true;
    }

    /** DDA-проверка короткого шага обрывает траекторию у первого незагруженного блока без загрузки чанков. */
    private BlockHitResult unloadedBoundary(net.minecraft.server.level.ServerLevel level,Vec3 from,Vec3 to) {
        return BlockGetter.traverseBlocks(from,to,level,
                (world,pos)->world.hasChunkAt(pos)?null:Shapes.block().clip(from,to,pos),world->null);
    }

    private void blindImpact(Charge charge,ServerPlayer owner,Entity entity,Vec3 point) {
        if(!(entity instanceof LivingEntity living) || !game.items().lifetimes().target(owner,entity)) return;
        boolean previouslyBlind=living.hasEffect(MobEffects.BLINDNESS);
        var damage=new DamageSource(sourceLevel(owner).registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.GENERIC),charge,owner);
        living.hurtServer(sourceLevel(owner),damage,.5f);
        living.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,200,0),owner);
        living.addEffect(ItemEffectLevels.create(MobEffects.WITHER,80,0),owner);
        if(charge.trial && charge.trialHand!=null && charge.trialOwner==owner)
            game.items().academy().blindTrial(owner,charge.trialHand,previouslyBlind);
    }

    private void tickClouds() {
        if(clouds.isEmpty()) return;
        cloudBatch.addAll(clouds);
        try {
            for(int index=0;index<cloudBatch.size();index++) {
                RainCloud cloud=cloudBatch.get(index);
                if(!clouds.contains(cloud)) continue;
                if(!validOwner(cloud.owner) || cloud.owner.level()!=cloud.level
                        || !game.items().currentEpoch(cloud.epoch) || !cloud.level.hasChunkAt(BlockPos.containing(cloud.position))
                        || game.items().magic().suppressedAt(cloud.level,cloud.position)) {
                    clouds.remove(cloud); continue;
                }
                if(paused(cloud.owner)) continue;
                if((tick&1)==0) {
                    cloud.level.sendParticles(ParticleTypes.SQUID_INK,cloud.position.x,cloud.position.y,cloud.position.z,30,8,.1,8,0);
                    LbcOverload.sound(cloud.owner,cloud.position,"minecraft:entity.squid.squirt",1,1,0);
                    Vec3 drop=rainDropOrigin(cloud);
                    if(drop!=null) spawn(cloud.owner,drop,RAIN_VELOCITY,false,RAIN_LIFETIME,null,false);
                }
                if(++cloud.age>=CLOUD_LIFETIME) clouds.remove(cloud);
            }
        } finally { cloudBatch.clear(); }
    }

    /** Cache the local native query; validate roles and use the target's current position for every drop. */
    private Vec3 rainDropOrigin(RainCloud cloud) {
        if(tick>=cloud.nextTargetTick) {
            cloud.nextTargetTick=tick+RAIN_TARGET_REFRESH;
            cloud.targets.clear();
            Vec3 at=cloud.position;
            var area=new AABB(at.x-RAIN_RADIUS,at.y-RAIN_SPEED*RAIN_LIFETIME,at.z-RAIN_RADIUS,
                    at.x+RAIN_RADIUS,at.y,at.z+RAIN_RADIUS);
            cloud.targets.addAll(cloud.level.getEntitiesOfClass(LivingEntity.class,area,
                    target->rainTarget(cloud,target)));
        } else cloud.targets.removeIf(target->!rainTarget(cloud,target));
        if(cloud.targets.isEmpty()) return null;
        var target=cloud.targets.get(cloud.owner.getRandom().nextInt(cloud.targets.size()));
        // (user) Rain is concentrated around enemies, retaining a small spread instead of homing.
        double x=target.getX()+(cloud.owner.getRandom().nextDouble()*2-1)*.6;
        double z=target.getZ()+(cloud.owner.getRandom().nextDouble()*2-1)*.6;
        return new Vec3(Math.clamp(x,cloud.position.x-RAIN_RADIUS,cloud.position.x+RAIN_RADIUS),cloud.position.y,
                Math.clamp(z,cloud.position.z-RAIN_RADIUS,cloud.position.z+RAIN_RADIUS));
    }

    private boolean rainTarget(RainCloud cloud,LivingEntity target) {
        return target.level()==cloud.level && game.items().lifetimes().target(cloud.owner,target)
                && Math.abs(target.getX()-cloud.position.x)<=RAIN_RADIUS
                && Math.abs(target.getZ()-cloud.position.z)<=RAIN_RADIUS
                && target.getBoundingBox().minY<cloud.position.y
                && target.getBoundingBox().maxY>=cloud.position.y-RAIN_SPEED*RAIN_LIFETIME;
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
                sourceSound(owner,owner.getEyePosition(),"minecraft:entity.squid.squirt",1,1,0);
                Vec3 direction=Vec3.directionFromRotation(owner.getXRot()+(owner.getRandom().nextFloat()*2-1)*8,
                        owner.getYRot()+(owner.getRandom().nextFloat()*2-1)*8);
                spawn(owner,owner.getEyePosition(),direction.scale(1.8),false,ORDINARY_LIFETIME,null,false);
                if(--queue.remaining<=0) gatlings.remove(owner);
                game.items().mana().score(owner,"gatling_fovos").set(queued(owner));
            }
        } finally { gatlingBatch.clear(); }
    }

    private boolean validOwner(ServerPlayer owner) {
        if(persistentSource())return true;
        return owner!=null && !owner.isRemoved() && owner.isAlive() && sourceLevel(owner)==game.maps().level()
                && game.canUseGameplayItems(owner) && !game.isObserver(owner) && game.stage()!=GameController.Stage.ENDING;
    }
    private boolean paused(ServerPlayer owner) {
        return game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped");
    }

    /** Disconnect удаляет только очереди владельца; уже выпущенные заряды снимет обычная owner-проверка. */
    void forget(ServerPlayer owner) {
        gatlings.remove(owner);game.items().mana().score(owner,"gatling_fovos").set(0);
        clouds.removeIf(cloud->cloud.owner==owner);
        cloudBatch.removeIf(cloud->cloud.owner==owner);
        gatlingBatch.removeIf(player->player==owner);
    }

    void stop() {
        for(Charge charge:List.copyOf(charges)) charge.discard();
        charges.clear(); batch.clear(); gatlings.clear(); gatlingBatch.clear(); clouds.clear(); cloudBatch.clear();
    }
    void sourceEmitter(net.minecraft.server.level.ServerLevel level,Vec3 origin,Vec3 direction,LbcSpellSource.Source source){try(var scope=game.items().spellSources().scope(source,level)){sourceEmitter(game.server().getPlayerList().getPlayer(source.author()),origin,direction);}}
    private void sourceSound(ServerPlayer owner,Vec3 at,String sound,float volume,float pitch,double radius){
        if(game.items().spellSources().current()==null)LbcOverload.sound(owner,at,sound,volume,pitch,radius);
        else LbcOverload.sound(sourceLevel(owner),at,sound,volume,pitch,radius,0);
    }
    private net.minecraft.server.level.ServerLevel sourceLevel(ServerPlayer owner){return game.items().spellSources().level(owner);}
    private boolean persistentSource(){var source=game.items().spellSources().current();return source!=null&&source.persistent()&&game.items().currentEpoch(source.epoch())&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&sourceLevel(null)!=null&&LbcDimensionItems.ordinary(sourceLevel(null));}
    private boolean boundSource(Entity entity){var source=game.items().spellSources().source(entity);return source!=null&&game.items().currentEpoch(source.epoch())&&LbcDimensionItems.ordinary(entity.level());}
    private Entity nativeSourceOwner(ServerPlayer owner){var source=game.items().spellSources().current();return source==null?owner:sourceLevel(owner).getEntity(source.emitter());}
    private boolean liveSourceCharge(Entity charge,ServerPlayer owner){return !charge.isRemoved()&&game.items().currentEpoch(charge)&&game.stage()!=GameController.Stage.ENDING&&(persistentSource()||owner!=null&&!owner.isRemoved()&&owner.isAlive()&&owner.level()==charge.level()&&charge.level()==game.maps().level()&&game.canUseGameplayItems(owner)&&!game.isObserver(owner));}


    boolean transientCaptureReady(){return clouds.isEmpty();}
    void cancelTransient(){
        var old=List.copyOf(charges);charges.clear();batch.clear();gatlings.clear();gatlingBatch.clear();
        for(var charge:old)if(!charge.isRemoved())charge.discard();
    }
}
