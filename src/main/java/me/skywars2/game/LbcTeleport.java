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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Заряды телепорта и облако на нативной стреле; расход, ввод и медитация остаются в общих сервисах. */
public final class LbcTeleport {
    public static final String CLOUD = "skywars2.teleport.cloud";
    public static final String WIND_RACER = "skywars2.boltbolt.racer";
    public static final EntityType<Charge> TYPE=registerType();
    private final GameController game;
    private final Set<Charge> charges=new LinkedHashSet<>();
    private final ArrayList<Charge> batch=new ArrayList<>();
    private final Set<Arrow> clouds=new LinkedHashSet<>();

    private static EntityType<Charge> registerType() {
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:teleport_charge"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,
                EntityType.Builder.<Charge>of(Charge::new,MobCategory.MISC)
                        .sized(.1f,.1f).clientTrackingRange(8).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type);
        return type;
    }
    public static void register() { }

    public static final class Charge extends Projectile implements PolymerEntity {
        int age;
        int sourceLifetime;boolean flyingStyle;
        int mode;
        Charge(EntityType<? extends Charge> type,Level level) { super(type,level); }
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
        @Override public EntityType<?> getPolymerEntityType(PacketContext context) { return EntityTypes.MARKER; }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean isPushedByFluid() { return false; }
    }

    LbcTeleport(GameController game) { this.game=game; }
    public int count() { return charges.size(); }
    List<Charge> snapshot() { return List.copyOf(charges); }
    public int cloudCount() { return clouds.size(); }
    List<Arrow> cloudSnapshot() { return List.copyOf(clouds); }

    void loaded(Entity entity) {
        if(entity instanceof Charge charge) {
            if(!game.items().currentEpoch(charge) || !boundSource(charge)&&(charge.level()!=game.maps().level()
                    || !(charge.getOwner() instanceof ServerPlayer))) { cleanup(charge); return; }
            charges.add(charge);
            if(charge.sourceLifetime==0) game.items().lifetimes().track(charge);
        }
        if(entity instanceof Arrow arrow && arrow.entityTags().contains(CLOUD)) {
            if(!game.items().currentEpoch(arrow) || arrow.level()!=game.maps().level()
                    || !(arrow.getOwner() instanceof ServerPlayer)) { cleanupCloud(arrow); return; }
            clouds.add(arrow);
        }
    }
    void unloaded(Entity entity) {
        if(entity instanceof Charge charge) { charges.remove(charge); cleanup(charge); }
        if(entity instanceof Arrow arrow && arrow.entityTags().contains(CLOUD)) {
            clouds.remove(arrow);
            if(!arrow.isRemoved()) cleanupCloud(arrow);
        }
    }

    public void cast(ServerPlayer owner,InteractionHand hand,int mode,ItemStack stack) {
        if(!validOwner(owner) || mode<0 || mode>3) return;
        if(mode==3) { castCloud(owner); return; }
        castAt(owner,hand,mode,stack,owner.getEyePosition(),owner.getLookAngle());
    }
    void emitAt(ServerPlayer owner,InteractionHand hand,int mode,Vec3 origin,Vec3 direction){
        castAt(owner,hand,mode,ItemStack.EMPTY,origin,direction,200);
    }
    Charge flying(ServerPlayer owner,InteractionHand hand,ItemStack stack,Vec3 origin,Vec3 direction){
        return castAt(owner,hand,0,ItemStack.EMPTY,origin,direction,60);
    }
    Charge sourceEmitter(ServerPlayer owner,Vec3 origin,Vec3 direction){return castAt(owner,InteractionHand.MAIN_HAND,0,ItemStack.EMPTY,origin,direction,60);}
    void castAt(ServerPlayer owner,InteractionHand hand,int mode,ItemStack stack,Vec3 origin,Vec3 direction){
        castAt(owner,hand,mode,stack,origin,direction,0);
    }
    private Charge castAt(ServerPlayer owner,InteractionHand hand,int mode,ItemStack stack,Vec3 origin,Vec3 direction,int lifetime){
        if(game.items().spellSources().current()==null)sourceSound(owner,origin,"minecraft:entity.lightning_bolt.thunder",1,2,0);
        Charge charge=new Charge(TYPE,sourceLevel(owner));
        charge.setOwner(nativeSourceOwner(owner)); charge.setPos(origin); charge.setNoGravity(true);
        charge.setYRot(lifetime>0?(float)Math.toDegrees(Math.atan2(-direction.x,direction.z)):owner.getYRot());
        charge.setXRot(lifetime>0?(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())):owner.getXRot());
        charge.setDeltaMovement(direction.normalize().scale(lifetime==60?1.2:mode==0?1.8:3.0));
        charge.mode=mode; charge.sourceLifetime=lifetime;charge.flyingStyle=lifetime==60;
        game.items().markCurrentItemEpoch(charge);
        if(sourceLevel(owner).addFreshEntity(charge)) {
            charges.add(charge);
            if(lifetime==0) game.items().lifetimes().track(charge);
        }
        return charge;
    }

    private void castCloud(ServerPlayer owner) { castRideableArrow(owner,.5,false); }

    /** One native carrier for Teleport cloud and Boltbolt racer; steering precedes native physics. */
    void castRideableArrow(ServerPlayer owner,double speed,boolean racer) {
        Vec3 origin=owner.position().add(0,.01,0);
        if(!sourceLevel(owner).hasChunkAt(BlockPos.containing(origin))) return;
        Arrow arrow=new Arrow(EntityTypes.ARROW,sourceLevel(owner));
        arrow.setOwner(owner); arrow.pickup=AbstractArrow.Pickup.DISALLOWED;
        arrow.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,sourceLevel(owner).registryAccess(),cloudPreset()));
        arrow.setOwner(owner); arrow.setNoGravity(true); arrow.setBaseDamage(0);
        arrow.setSoundEvent(SoundEvents.WOOL_STEP);
        arrow.setPos(origin);
        arrow.setDeltaMovement(owner.getLookAngle().normalize().scale(speed));
        LbcShooters.orient(arrow);
        arrow.entityTags().add(CLOUD);
        if(racer) arrow.entityTags().add(WIND_RACER);
        game.items().markCurrentItemEpoch(arrow);
        owner.stopRiding();
        if(!sourceLevel(owner).addFreshEntity(arrow)) { arrow.discard(); return; }
        clouds.add(arrow);
        owner.startRiding(arrow,true,true);
    }

    void tick() {
        if(!charges.isEmpty()) {
            batch.addAll(charges);
            try {
                for(Charge charge:batch) {
                    try(var sourceScope=game.items().spellSources().scope(charge)){
                    if(!charges.contains(charge)) continue;
                    var owner=game.items().spellSources().author(charge);
                if(!liveSourceCharge(charge,owner)) { remove(charge); continue; }
                    if(game.items().spellSources().current()!=null?game.items().timeStop().stopped((net.minecraft.server.level.ServerLevel)charge.level())||charge.entityTags().contains("time_stopped"):paused(owner)) continue;
                    Vec3 from=charge.position();
                    if(!sourceLevel(owner).hasChunkAt(BlockPos.containing(from))
                            || game.items().magic().suppressedAt((net.minecraft.server.level.ServerLevel)charge.level(),from)) {
                        remove(charge); continue;
                    }
                    Vec3 to=from.add(charge.getDeltaMovement());
                    HitResult hit=charge.flyingStyle?LbcTerra.wallHit(sourceLevel(owner),from,to):game.items().wands().firstHit(owner,from,to);
                    var shield=game.items().wands().shieldHit(owner,from,hit==null?to:hit.getLocation());
                    if(shield!=null && !charge.level().getBlockState(BlockPos.containing(shield.getLocation())).isAir()) shield=null;
                    boolean shieldBlocked=shield!=null && (hit==null
                            || from.distanceToSqr(shield.getLocation())<from.distanceToSqr(hit.getLocation()));
                    if(shieldBlocked) hit=shield;
                    if(hit instanceof BlockHitResult wall && !charge.level().hasChunkAt(wall.getBlockPos())) { remove(charge); continue; }
                    Vec3 end=hit==null?to:hit.getLocation();
                    charge.setPos(end);
                    if(charge.flyingStyle) flyingTrail(owner,from,end,shieldBlocked);
                    else emitTrail(owner,charge,from,end);
                    if(hit!=null) {
                        remove(charge);
                        if(hit instanceof EntityHitResult entityHit && !shieldBlocked)
                            impact(charge,owner,entityHit.getEntity(),end);
                        continue;
                    }
                    charge.age++;
                    if(charge.sourceLifetime>0) { if(charge.age>=charge.sourceLifetime) remove(charge); }
                    else game.items().lifetimes().advance(charge,charge.age);
                    }
                }
            } finally { batch.clear(); }
        }
        tickClouds();
    }

    private void flyingTrail(ServerPlayer owner,Vec3 from,Vec3 end,boolean shieldBlocked) {
        double distance=from.distanceTo(end);Vec3 direction=end.subtract(from).normalize();
        for(double step=0;step<=distance+1e-7;step+=.3) {
            Vec3 point=from.add(direction.scale(Math.min(step,distance)));
            sourceLevel(owner).sendParticles(ParticleTypes.ANGRY_VILLAGER,true,false,point.x,point.y,point.z,2,.1,.1,.1,0);
            if(shieldBlocked&&step>=distance-1e-7)break;
            var area=new AABB(point,point.add(1,1,1));
            for(var target:sourceLevel(owner).getEntitiesOfClass(LivingEntity.class,area,
                    candidate->candidate.position().distanceToSqr(point)<=225&&game.items().lifetimes().target(owner,candidate))) {
                if(target.addEffect(ItemEffectLevels.create(MobEffects.LEVITATION,20,40),owner))
                    game.items().indirectKills().levitated(target,owner);
            }
        }
    }

    private void emitTrail(ServerPlayer owner,Charge charge,Vec3 from,Vec3 end) {
        double distance=from.distanceTo(end);
        for(double step=0;step<=distance+1e-7;step+=.3) {
            Vec3 point=distance<1e-9?from:from.add(end.subtract(from).scale(Math.min(1,step/distance)));
            sourceLevel(owner).sendParticles(ParticleTypes.SCULK_CHARGE_POP,true,false,point.x,point.y,point.z,
                    0,0,charge.mode==2?-1:1,0,charge.mode==1?1:.5);
            sourceLevel(owner).sendParticles(ParticleTypes.CLOUD,true,false,point.x,point.y,point.z,1,.1,.1,.1,.05);
        }
    }

    private void impact(Charge charge,ServerPlayer owner,Entity first,Vec3 point) {
        if(!validOwner(owner) || !game.items().currentEpoch(charge)) return;
        var targets=new ArrayList<LivingEntity>(2);
        if(first instanceof LivingEntity target && game.items().lifetimes().target(owner,target)) targets.add(target);
        AABB near=new AABB(point,point).inflate(1e-5);
        for(var target:sourceLevel(owner).getEntitiesOfClass(LivingEntity.class,near,
                candidate->candidate!=first && game.items().lifetimes().target(owner,candidate))) {
            if(targets.size()==2) break;
            targets.add(target);
        }
        for(var target:targets) {
            if(charge.mode==2) { inverted(owner,target); continue; }
            if(target.addEffect(ItemEffectLevels.create(MobEffects.LEVITATION,20,50),owner))
                game.items().indirectKills().levitated(target,owner);
            game.items().electro().lightning(owner,target.position());
            placeCobweb(owner,target.position());
            sourceLevel(owner).sendParticles(ParticleTypes.FALLING_WATER,target.getX(),target.getY()+15,target.getZ(),300,.8,4,.8,0);
        }
    }
    private void placeCobweb(ServerPlayer owner,Vec3 point) {
        var level=sourceLevel(owner);
        BlockPos center=BlockPos.containing(point).above(10);
        game.items().indirectKills().trapChanges(owner,()-> {
            for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
                BlockPos pos=center.offset(x,0,z);
                if(level.hasChunkAt(pos) && level.getBlockState(pos).isAir())
                    level.setBlock(pos,Blocks.COBWEB.defaultBlockState(),net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                            | net.minecraft.world.level.block.Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            }
        });
    }

    private void inverted(ServerPlayer owner,LivingEntity target) {
        var level=sourceLevel(owner);
        BlockPos center=BlockPos.containing(target.position()), source=center.offset(-1,-1,-1), dest=source.below();
        if(level.isOutsideBuildHeight(source) || level.isOutsideBuildHeight(dest)) return;
        for(var pos:BlockPos.betweenClosed(dest,dest.offset(2,1,2))) if(!level.hasChunkAt(pos)) return;
        var template=new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate();
        // Сначала сохраняем все компоненты контейнеров, затем убираем исходные блоки без вторичного дропа.
        template.fillFromWorld(level,source,new net.minecraft.core.Vec3i(3,1,3),false,List.of());
        game.items().indirectKills().trapChanges(owner,()-> {
            for(var pos:BlockPos.betweenClosed(source,source.offset(2,0,2)))
                level.setBlock(pos,Blocks.COBWEB.defaultBlockState(),net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                        | net.minecraft.world.level.block.Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            game.maps().placeMapSlice(level,template,dest,
                    new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings()
                            .setIgnoreEntities(true).setKnownShape(true),owner.getRandom().nextLong());
        });
        Vec3 original=target.position();
        Vec3 velocity=target.getDeltaMovement();
        target.teleportTo(original.x,original.y-.5,original.z);
        target.setDeltaMovement(velocity);
        target.needsSync=true;
        if(target instanceof ServerPlayer player)
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(target));
        game.items().indirectKills().displaced(target,owner);
        game.items().electro().lightning(owner,original);
        level.sendParticles(ParticleTypes.CLOUD,original.x,original.y-1,original.z,100,.8,4,.8,2);
    }

    private void tickClouds() {
        for(Arrow arrow:List.copyOf(clouds)) {
            if(arrow.isRemoved() || !(arrow.getOwner() instanceof ServerPlayer owner)
                    || !validOwner(owner) || sourceLevel(owner)!=arrow.level() || arrow.level()!=game.maps().level()
                    || !game.items().currentEpoch(arrow)) { removeCloud(arrow); continue; }
            if(!arrow.level().hasChunkAt(BlockPos.containing(arrow.position()))) { removeCloud(arrow); continue; }
            if(paused(owner)) continue;
            Vec3 here=arrow.position(), next=here.add(arrow.getDeltaMovement());
            if(!arrow.level().hasChunkAt(BlockPos.containing(next))) {
                removeCloud(arrow);
                continue;
            }
            var level=(net.minecraft.server.level.ServerLevel)arrow.level();
            boolean racer=arrow.entityTags().contains(WIND_RACER);
            level.sendParticles(ParticleTypes.CLOUD,true,false,here.x,here.y,here.z,racer?50:60,2,racer?1.5:1,2,0);
            if(racer) {
                if(owner.getRandom().nextFloat()<.3f) {
                    sourceSound(owner,here,"minecraft:entity.firework_rocket.twinkle",1,1,0);
                    sourceSound(owner,here,"minecraft:entity.lightning_bolt.thunder",10,1,0);
                    game.items().wands().racerLightning(owner,here);
                }
            } else level.sendParticles(ParticleTypes.FALLING_WATER,here.x,here.y-.5,here.z,20,1,.1,1,0);
            boolean nearby=!level.getEntitiesOfClass(ServerPlayer.class,new AABB(here,here).inflate(3),
                    player->validOwner(player) && !player.isSpectator()
                            && player.position().distanceToSqr(here)<=9).isEmpty();
            if(!nearby) arrow.setNoGravity(false);
        }
    }

    public boolean canHit(Projectile projectile,Entity target) {
        return projectile==null || !projectile.entityTags().contains(CLOUD);
    }

    void forget(ServerPlayer owner) {
        for(Arrow arrow:List.copyOf(clouds)) if(arrow.getOwner()==owner) removeCloud(arrow);
    }

    private static net.minecraft.nbt.CompoundTag cloudPreset() {
        var tag=new net.minecraft.nbt.CompoundTag(); tag.putByte("PierceLevel",(byte)127); return tag;
    }
    public boolean beforeCloudTick(Arrow arrow) {
        if(!(arrow.getOwner() instanceof ServerPlayer owner) || !validOwner(owner)
                || !game.items().currentEpoch(arrow) || arrow.level()!=sourceLevel(owner)) {
            removeCloud(arrow); return false;
        }
        if(paused(owner)) return false;
        if(arrow.entityTags().contains(WIND_RACER) && owner.getVehicle()==arrow) {
            arrow.setDeltaMovement(owner.getLookAngle().normalize().scale(2));
            LbcShooters.orient(arrow);
            // Vanilla arrows sync every 20 ticks. Steering changes velocity every tick,
            // so send both position and motion each tick instead of letting the rider's
            // client keep simulating the old direction and drag until the next correction.
            arrow.needsSync=true;
            arrow.syncVelocity=true;
        }
        Vec3 from=arrow.position(),to=from.add(arrow.getDeltaMovement());
        boolean loaded=net.minecraft.world.level.BlockGetter.traverseBlocks(from,to,sourceLevel(owner),
                (world,pos)->world.hasChunkAt(pos)?null:Boolean.FALSE,world->Boolean.TRUE);
        if(!loaded) { removeCloud(arrow); return false; }
        return true;
    }
    public void cloudLanded(Arrow arrow) { removeCloud(arrow); }

    private boolean validOwner(ServerPlayer owner) {
        if(persistentSource())return true;
        return owner!=null && !owner.isRemoved() && owner.isAlive() && sourceLevel(owner)==game.maps().level()
                && game.canUseGameplayItems(owner) && !game.isObserver(owner) && game.stage()!=GameController.Stage.ENDING;
    }
    private boolean paused(ServerPlayer owner) {
        return game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped");
    }
    private void remove(Charge charge) { charges.remove(charge); cleanup(charge); }
    private void cleanup(Charge charge) {
        game.items().lifetimes().unloaded(charge);
        if(!charge.isRemoved()) charge.discard();
    }
    private void removeCloud(Arrow arrow) { clouds.remove(arrow); cleanupCloud(arrow); }
    private void cleanupCloud(Arrow arrow) {
        if(!arrow.isRemoved()) {
            arrow.ejectPassengers();
            arrow.discard();
        }
    }
    public void stop() {
        for(Charge charge:List.copyOf(charges)) cleanup(charge);
        for(Arrow arrow:List.copyOf(clouds)) cleanupCloud(arrow);
        charges.clear(); batch.clear(); clouds.clear();
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


    void cancelTransient(){var old=List.copyOf(charges);charges.clear();batch.clear();for(var charge:old)cleanup(charge);}
}
