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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Exasperato: взрывной заряд; мана и редкий пересмотр дальности общие с остальными жезлами. */
public final class LbcExasperato {
    public static final String EXPLOSIVE = "skywars2.wand.exasperato";
    public static final EntityType<Charge> TYPE = registerType();
    private final GameController game;
    private final Set<Charge> charges = new LinkedHashSet<>();
    private final ArrayList<Charge> batch = new ArrayList<>();
    private static final int SEARCH_SEGMENT=16, SEARCH_BUDGET=64;
    private static final long BLAST_BUDGET_NANOS=4_000_000;
    private static final int BLASTS_PER_TICK=8;
    private static final double[] BLAST_RAYS=blastRays();
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> AIRS=
            net.minecraft.tags.TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final net.minecraft.core.particles.DustColorTransitionOptions NUKE_PARTICLE=
            new net.minecraft.core.particles.DustColorTransitionOptions(0xFFDC40,0,4);
    private static final class Search {
        final ServerPlayer owner;
        final ServerLevel level;
        final String epoch;
        final Vec3 direction;
        Vec3 position;
        double remaining;
        final boolean walls;
        final boolean blocksOnly;
        final java.util.concurrent.CompletableFuture<net.minecraft.world.phys.HitResult> result=new java.util.concurrent.CompletableFuture<>();
        final java.util.function.BiConsumer<Vec3,Vec3> trail;
        Search(ServerPlayer owner,String epoch,Vec3 from,Vec3 direction,double remaining,boolean walls,
                java.util.function.BiConsumer<Vec3,Vec3> trail,boolean blocksOnly) {
            this.owner=owner; this.level=owner.level(); this.epoch=epoch;
            this.direction=direction; this.position=from; this.remaining=remaining; this.walls=walls; this.trail=trail;
            this.blocksOnly=blocksOnly;
        }
    }
    private final ArrayDeque<Search> searches=new ArrayDeque<>();
    private static final class BlastBatch {
        final ServerPlayer owner;
        final ServerLevel level;
        final Vec3 origin;
        final String epoch;
        java.util.function.Consumer<PrimedTnt> sourceInit;
        Vec3[] offsets=NUKE_OFFSETS;
        float radius=20;
        boolean protectedDamage;
        boolean cosmos;
        int next;
        BlastBatch(ServerPlayer owner,Vec3 origin,String epoch) {
            this(owner,owner.level(),origin,epoch);
        }
        BlastBatch(ServerPlayer owner,ServerLevel level,Vec3 origin,String epoch) {
            this.owner=owner; this.level=level; this.origin=origin; this.epoch=epoch;
        }
    }
    // (user) Сохраняем все серии без отказов в касте; одна запись хранит всю серию, а не 32 задачи.
    private final ArrayDeque<BlastBatch> blasts=new ArrayDeque<>();

    private static EntityType<Charge> registerType() {
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:exasperato_charge"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,
                EntityType.Builder.<Charge>of(Charge::new,MobCategory.MISC)
                        .sized(.1f,.1f).clientTrackingRange(8).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type);
        return type;
    }
    static void register() { }

    /** Нативный owner позволяет электрощиту отражать заряд без отдельного адаптера. */
    public static final class Charge extends Projectile implements PolymerEntity {
        int age;
        int mode;
        int lifetime;
        boolean academyTrial;
        Charge(EntityType<? extends Charge> type,Level level) { super(type,level); }
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
        @Override public EntityType<?> getPolymerEntityType(PacketContext context) { return EntityTypes.MARKER; }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean isPushedByFluid() { return false; }
    }

    LbcExasperato(GameController game) { this.game=game; }
    public int count() { return charges.size(); }
    public int pendingSearches() { return searches.size(); }
    public int mushroomCount() { return game.items().nuclearVisuals().count(); }
    public int pendingBlasts() {
        int remaining=0;
        for(var batch:blasts) remaining+=batch.offsets.length-batch.next;
        return remaining;
    }
    List<Charge> snapshot() { return List.copyOf(charges); }
    void loaded(Entity entity) {
        if(entity instanceof Charge charge) {
            if(!game.items().currentEpoch(charge) || !(charge.getOwner() instanceof ServerPlayer)
                    || charge.level()!=game.maps().level()) charge.discard();
            else charges.add(charge);
        }
    }
    void unloaded(Entity entity) {
        if(entity instanceof Charge charge) {
            charges.remove(charge);
            // Переход в скрытую секцию не всегда ставит removed: незагруженный заряд не должен ждать возврата чанка.
            if(!charge.isRemoved()) charge.discard();
        }
    }

    public void cast(ServerPlayer owner) {
        if(owner.level()!=game.maps().level() || !game.canUseGameplayItems(owner)) return;
        Charge charge=TYPE.create(owner.level(),net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if(charge==null) return;
        charge.setOwner(owner); charge.setPos(owner.getEyePosition());
        charge.academyTrial=LbcAcademy.trial(owner.getMainHandItem());
        charge.entityTags().add("magic");
        charge.setNoGravity(true); charge.setDeltaMovement(owner.getLookAngle());
        charge.setYRot(owner.getYRot()); charge.setXRot(owner.getXRot());
        game.items().markCurrentItemEpoch(charge);
        if(owner.level().addFreshEntity(charge)) {
            charges.add(charge); game.items().lifetimes().track(charge,true);
            for(int pitch=0;pitch<=2;pitch++)
                LbcOverload.sound(owner,owner.getEyePosition(),"minecraft:entity.generic.explode",1,pitch,0);
        }
    }

    /** Academy variants share the ordinary projectile's owner, epoch and target checks. */
    public void cast(ServerPlayer owner,int mode) {
        if(mode==0) { cast(owner); return; }
        if(owner.level()!=game.maps().level() || !game.canUseGameplayItems(owner)
                || game.items().magic().suppressedAt(owner.level(),owner.getEyePosition())
                || game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped")) return;
        switch(mode) {
            case 1 -> {
                spawnCharge(owner,1,50,6);
                spawnCharge(owner,1,100,3);
                spawnCharge(owner,1,0,0);
                launchSound(owner);
            }
            case 2 -> { spawnCharge(owner,2,200,0); launchSound(owner); }
            case 3 -> chaos(owner);
            case 4 -> castNuke(owner);
            default -> cast(owner);
        }
    }

    private void launchSound(ServerPlayer owner) {
        for(int pitch=0;pitch<=2;pitch++)
            LbcOverload.sound(owner,owner.getEyePosition(),"minecraft:entity.generic.explode",1,pitch,0);
    }

    private void spawnCharge(ServerPlayer owner,int mode,int lifetime,double offset) {
        Charge charge=TYPE.create(owner.level(),net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if(charge==null) return;
        Vec3 direction=owner.getLookAngle();
        charge.mode=mode; charge.lifetime=lifetime;
        Vec3 origin=owner.getEyePosition(),start=origin.add(direction.scale(offset));
        // Передние заряды не появляются за стеной: сначала ограничиваем сдвиг ближайшим препятствием.
        var wall=offset>0 ? LbcTerra.wallHit(owner.level(),origin,start) : null;
        if(wall!=null) start=wall.getLocation();
        var shield=offset>0 ? game.items().wands().shieldHit(owner,origin,start) : null;
        if(shield!=null) start=shield.getLocation();
        charge.setOwner(owner); charge.setPos(start); charge.entityTags().add("magic");
        charge.setNoGravity(true); charge.setDeltaMovement(direction.scale(mode==2 ? 4.5 : 1));
        charge.setYRot(owner.getYRot()); charge.setXRot(owner.getXRot());
        game.items().markCurrentItemEpoch(charge);
        if(owner.level().addFreshEntity(charge)) {
            charges.add(charge);
            if(lifetime==0) game.items().lifetimes().track(charge,true);
        }
    }

    private void castNuke(ServerPlayer owner) {
        String epoch=game.items().epoch();
        searchLoadedRay(owner,owner.getEyePosition(),owner.getLookAngle(),true,null)
                .thenAccept(hit->{ if(hit!=null) spawnNuke(owner,hit.getLocation(),epoch); });
    }

    /** Общая порционная очередь прицеливания: дальность ограничена геометрией и загруженными чанками. */
    java.util.concurrent.CompletableFuture<net.minecraft.world.phys.HitResult> searchLoadedRay(ServerPlayer owner,
            Vec3 from,Vec3 direction,boolean walls,java.util.function.BiConsumer<Vec3,Vec3> trail) {
        return searchLoadedRay(owner,from,direction,walls,trail,false);
    }
    /** Terrain-only spells share the budgeted DDA, without treating entities or shields as terrain. */
    java.util.concurrent.CompletableFuture<net.minecraft.world.phys.HitResult> searchLoadedBlocks(ServerPlayer owner,
            Vec3 from,Vec3 direction,java.util.function.BiConsumer<Vec3,Vec3> trail) {
        return searchLoadedRay(owner,from,direction,true,trail,true);
    }
    private java.util.concurrent.CompletableFuture<net.minecraft.world.phys.HitResult> searchLoadedRay(ServerPlayer owner,
            Vec3 from,Vec3 direction,boolean walls,java.util.function.BiConsumer<Vec3,Vec3> trail,boolean blocksOnly) {
        direction=direction.normalize();
        // Высота строительства ограничивает лишь наличие блоков. Мобы выше/ниже неё тоже остаются целями.
        double reach=blocksOnly?0:game.items().lifetimes().livingRayExtent(owner,from,direction);
        if(walls && Math.abs(direction.y)>1e-12) {
            double buildExit=Math.max((owner.level().getMinY()-from.y)/direction.y,
                    (owner.level().getMaxY()+1.0-from.y)/direction.y);
            reach=Math.max(reach,buildExit);
        } else if(walls && !owner.level().isOutsideBuildHeight(BlockPos.containing(from))) reach=Double.POSITIVE_INFINITY;
        var search=new Search(owner,game.items().epoch(),from,direction,reach,walls,trail,blocksOnly);
        if(reach>0) searches.addLast(search); else search.result.complete(null);
        return search.result;
    }

    /** Ограничиваем работу, а не дальность: каждый незавершённый луч продолжится в следующих тиках. */
    private void searchTick() {
        if(game.items().wands().timeStopped()) return;
        long deadline=System.nanoTime()+3_000_000;
        for(int work=0;work<SEARCH_BUDGET && !searches.isEmpty() && System.nanoTime()<deadline;work++) {
            var search=searches.removeFirst(); var owner=search.owner; var level=search.level;
            if(search.result.isDone()) continue;
            if(!valid(owner,search.epoch) || owner.level()!=level || game.items().magic().suppressed(owner)) { search.result.complete(null); continue; }
            if(owner.entityTags().contains("time_stopped")) { searches.addLast(search); continue; }
            Vec3 from=search.position;
            if(search.remaining<=1e-7 || !level.hasChunkAt(BlockPos.containing(from))
                    || game.items().magic().suppressedAt(level,from)) { search.result.complete(null); continue; }
            double length=Math.min(SEARCH_SEGMENT,search.remaining);
            Vec3 end=from.add(search.direction.scale(length));
            if(Math.abs(end.x)>30_000_000 || Math.abs(end.z)>30_000_000) { search.result.complete(null); continue; }
            var wall=search.walls?LbcTerra.wallHit(level,from,end):LbcTerra.wallHit(level,from,end,state->false);
            if(wall!=null) end=wall.getLocation();
            var shield=search.blocksOnly?null:game.items().wands().shieldHit(owner,from,end);
            if(shield!=null) end=shield.getLocation();
            double suppression=game.items().magic().suppressionFraction(level,from,end);
            if(Double.isFinite(suppression)) end=from.lerp(end,suppression);
            var hit=search.blocksOnly?null:ProjectileUtil.getEntityHitResult(level,owner,from,end,new AABB(from,end).inflate(1),
                    entity->game.items().lifetimes().target(owner,entity),0);
            if(search.trail!=null) search.trail.accept(from,hit==null?end:hit.getLocation());
            if(hit!=null) search.result.complete(hit);
            else if(shield!=null || Double.isFinite(suppression)) search.result.complete(null);
            else if(wall!=null) search.result.complete(level.hasChunkAt(wall.getBlockPos())?wall:null);
            else {
                search.position=end; search.remaining-=length; searches.addLast(search);
            }
        }
    }

    private void spawnNuke(ServerPlayer owner,Vec3 impact,String epoch) {
        if(!valid(owner,epoch)) return;
        var charge=TYPE.create(owner.level(),net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if(charge==null) return;
        charge.mode=4; charge.setOwner(owner); charge.setPos(impact.add(0,500,0));
        charge.setDeltaMovement(0,-1,0); charge.setNoGravity(true); charge.entityTags().add("magic");
        game.items().markCurrentItemEpoch(charge);
        if(owner.level().addFreshEntity(charge)) {
            charges.add(charge);
            owner.level().sendParticles(new net.minecraft.core.particles.DustParticleOptions(0xFF0000,4),
                    true,false,impact.x,impact.y,impact.z,1000,1,100,1,0);
            LbcOverload.sound(owner,impact,"minecraft:lbcsounds.nuke",10,1.2f,0);
        }
    }

    private boolean valid(ServerPlayer owner,String epoch) {
        return game.items().currentEpoch(epoch) && owner.level()==game.maps().level() && owner.isAlive()
                && !owner.isRemoved() && game.canUseGameplayItems(owner) && game.stage()!=GameController.Stage.ENDING;
    }

    void tick() {
        batch.addAll(charges);
        try {
            for(var charge:batch) {
                if(!charges.contains(charge)) continue;
                if(charge.isRemoved() || !(charge.getOwner() instanceof ServerPlayer owner)
                        || !owner.isAlive() || owner.isRemoved() || owner.level()!=charge.level()
                        || charge.level()!=game.maps().level() || !game.canUseGameplayItems(owner)
                        || !game.items().currentEpoch(charge) || game.stage()==GameController.Stage.ENDING) {
                    charge.discard(); continue;
                }
                if(game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped")) continue;
                if(charge.mode==4) nukeStep(charge,owner);
                else if(charge.mode==2) electroRayStep(charge,owner);
                else step(charge,owner);
                if(!charge.isRemoved() && ++charge.age>=charge.lifetime && charge.lifetime>0) charge.discard();
                if(!charge.isRemoved()) game.items().lifetimes().advance(charge,charge.age);
            }
        } finally { batch.clear(); }
        searchTick();
        blastTick();
    }

    /** Один нативный взрыв неделим; бюджет проверяется между взрывами, до следующего. */
    void blastTick() {
        if(blasts.isEmpty()) return;
        long started=System.nanoTime();
        for(int work=0;work<BLASTS_PER_TICK && !blasts.isEmpty();work++) {
            var burst=blasts.removeFirst();
            if(!game.items().currentEpoch(burst.epoch) || game.stage()==GameController.Stage.ENDING
                    || !burst.cosmos && burst.level!=game.maps().level()) continue;
            if(game.items().timeStop().stopped(burst.level)) { blasts.addLast(burst); continue; }
            Vec3 offset=burst.offsets[burst.next];
            var cell=BlockPos.containing(burst.origin.add(offset));
            if(burst.cosmos ? burst.level.getChunkSource().getChunkNow(cell.getX()>>4,cell.getZ()>>4)==null
                    : !burst.level.hasChunkAt(cell)) {
                // Временно недоступная точка остаётся в очереди: не теряем взрыв и не загружаем чанк ради него.
                blasts.addLast(burst); continue;
            }
            burst.next++;
            if(burst.cosmos) cosmosBlast(burst);
            else nativeBlast(burst.owner,burst.level,burst.origin.add(offset),burst.radius,burst.epoch,
                        burst.protectedDamage,burst.sourceInit);
            if(burst.next<burst.offsets.length && game.items().currentEpoch(burst.epoch)
                    && game.stage()!=GameController.Stage.ENDING) blasts.addLast(burst);
            // Round-robin даёт прогресс каждой серии, даже при непрерывных новых попаданиях.
            if(System.nanoTime()-started>=BLAST_BUDGET_NANOS) break;
        }
    }

    /** The source's instant radius100 creeper, using the existing round-robin CPU budget. */
    void queueCosmos(ServerLevel level,Vec3 position,ServerPlayer author,String epoch) {
        var burst=new BlastBatch(author,level,position,epoch);
        burst.cosmos=true; burst.radius=100; burst.offsets=new Vec3[]{Vec3.ZERO};
        blasts.addLast(burst);
    }
    int pendingCosmos() {
        int count=0; for(var batch:blasts) if(batch.cosmos) count+=batch.offsets.length-batch.next;
        return count;
    }
    private void cosmosBlast(BlastBatch batch) {
        var source=EntityTypes.CREEPER.create(batch.level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if(source==null) return;
        source.setPos(batch.origin);
        source.setCustomName(net.minecraft.network.chat.Component.translatable("cosmos"));
        source.entityTags().add(LbcSkyHazards.COSMOS); game.items().markCurrentItemEpoch(source);
        var damage=batch.level.damageSources().explosion(source,batch.owner==null?source:batch.owner);
        // The source is a mob explosion: retain mobGriefing and do not apply the TNT protection setting.
        batch.level.explode(source,damage,null,batch.origin.x,batch.origin.y,batch.origin.z,
                batch.radius,false,Level.ExplosionInteraction.MOB);
    }

    private void nukeStep(Charge charge,ServerPlayer owner) {
        var level=owner.level(); Vec3 from=charge.position(),end=from.add(charge.getDeltaMovement());
        if(!level.hasChunkAt(BlockPos.containing(from)) || game.items().magic().suppressedAt(level,from)) {
            charge.discard(); return;
        }
        // Проверка по всему отрезку: ракета не пропускает тонкую поверхность между тиками.
        var wall=LbcTerra.wallHit(level,from,end);
        var shield=game.items().wands().shieldHit(owner,from,wall==null ? end : wall.getLocation());
        if(shield!=null) { charge.discard(); return; }
        Vec3 collisionEnd=wall==null ? end : wall.getLocation();
        var hits=ProjectileUtil.getManyEntityHitResult(level,owner,from,collisionEnd,new AABB(from,collisionEnd).inflate(1),
                entity->game.items().lifetimes().target(owner,entity),0,ClipContext.Block.COLLIDER,true,false);
        Vec3 entityHit=null; double nearest=Double.POSITIVE_INFINITY;
        for(var hit:hits) {
            double distance=from.distanceToSqr(hit.getLocation());
            if(distance<nearest) { nearest=distance; entityHit=hit.getLocation(); }
        }
        if(entityHit!=null) { charge.discard(); nuclearBurst(owner,entityHit); return; }
        if(wall!=null) {
            charge.discard();
            if(level.hasChunkAt(wall.getBlockPos())) nuclearBurst(owner,wall.getLocation());
            return;
        }
        charge.setPos(end);
        level.sendParticles(NUKE_PARTICLE,true,false,end.x,end.y,end.z,3,0,0,0,.2);
        // Пустота без цели не удерживает ракету бесконечно; это геометрия мира, а не lifetime.
        if(end.y<level.getMinY()-1 && charge.getDeltaMovement().y<=0) charge.discard();
    }

    /** Исходные 32 точки и мощность 20. Fuse=0 исполняется сразу без 32 сущностей-взрывчаток. */
    private void nuclearBurst(ServerPlayer owner,Vec3 origin) {
        nuclearBurst(owner,origin,null,false);
    }
    /** Exoperso использует ту же очередь 32 взрывов без потери серий и копии движка взрыва. */
    void exopersoBurst(ServerPlayer owner,Vec3 origin,java.util.function.Consumer<PrimedTnt> sourceInit) {
        nuclearBurst(owner,origin,sourceInit,true);
    }
    private void nuclearBurst(ServerPlayer owner,Vec3 origin,java.util.function.Consumer<PrimedTnt> sourceInit,boolean exoperso) {
        String epoch=game.items().epoch();
        game.items().nuclearVisuals().show(owner.level(),origin,12,exoperso);
        String[] sounds={"minecraft:entity.dragon_fireball.explode","minecraft:entity.generic.explode",
                "minecraft:item.firecharge.use","minecraft:entity.firework_rocket.large_blast","minecraft:entity.firework_rocket.large_blast_far"};
        for(var sound:sounds) LbcOverload.sound(owner,origin,sound,100,0,0);
        var batch=new BlastBatch(owner,origin,epoch); batch.sourceInit=sourceInit;
        blasts.addLast(batch);
    }

    private static final Vec3[] NUKE_OFFSETS={
        new Vec3(0,0,0),new Vec3(5,0,0),new Vec3(-5,0,0),new Vec3(0,0,5),new Vec3(0,0,-5),
        new Vec3(5,0,5),new Vec3(-5,0,-5),new Vec3(5,0,-5),new Vec3(-5,0,5),
        new Vec3(0,5,0),new Vec3(0,10,0),new Vec3(0,15,0),new Vec3(0,20,0),
        new Vec3(0,-5,0),new Vec3(0,-10,0),new Vec3(0,-15,0),
        new Vec3(7,5,0),new Vec3(-7,5,0),new Vec3(0,5,7),new Vec3(0,5,-7),
        new Vec3(7,5,7),new Vec3(-7,5,-7),new Vec3(7,5,-7),new Vec3(-7,5,7),
        new Vec3(7,-5,0),new Vec3(-7,-5,0),new Vec3(0,-5,7),new Vec3(0,-5,-7),
        new Vec3(7,-5,7),new Vec3(-7,-5,-7),new Vec3(7,-5,-7),new Vec3(-7,-5,7)
    };

    private void electroRayStep(Charge charge,ServerPlayer owner) {
        ServerLevel level=owner.level();
        Vec3 origin=charge.position(),direction=charge.getDeltaMovement().normalize();
        if(!level.hasChunkAt(BlockPos.containing(origin)) || game.items().magic().suppressedAt(level,origin)) { charge.discard(); return; }
        Vec3 end=origin.add(direction.scale(4.5));
        var wall=LbcTerra.wallHit(level,origin,end);
        // Стена здесь проницаема до случайного затухания; граница загруженных чанков — всегда terminal.
        if(wall!=null && !level.hasChunkAt(wall.getBlockPos())) { charge.discard(); return; }
        var shield=game.items().wands().shieldHit(owner,origin,end);
        for(int i=0;i<=9;i++) {
            Vec3 point=origin.add(direction.scale(i*.5));
            if(shield!=null && origin.distanceToSqr(point)>=origin.distanceToSqr(shield.getLocation())) {
                charge.discard(); LbcOverload.sound(owner,shield.getLocation(),"minecraft:item.shield.block",1,1,0); return;
            }
            if(!level.hasChunkAt(BlockPos.containing(point))) { charge.discard(); return; }
            if(game.items().magic().suppressedAt(level,point)) { charge.discard(); return; }
            level.sendParticles(ParticleTypes.EXPLOSION,true,false,point.x,point.y,point.z,1,0,0,0,0);
            level.sendParticles(ParticleTypes.LAVA,point.x,point.y,point.z,2,.1,.1,.1,.4);
            if(!level.getBlockState(BlockPos.containing(point)).is(AIRS)) {
                if(level.getRandom().nextFloat()<.2f) { charge.discard(); return; }
            }
        }
        if(shield!=null) { charge.discard(); return; }
        charge.setPos(end);
        electroPayload(charge,owner,end);
    }

    private void electroPayload(Charge charge,ServerPlayer owner,Vec3 at) {
        ServerLevel level=owner.level(); String epoch=game.items().epoch();
        if(!charge.isRemoved() && game.items().currentEpoch(charge) && game.items().currentEpoch(epoch)
                && game.stage()!=GameController.Stage.ENDING && owner.isAlive() && game.canUseGameplayItems(owner))
            game.items().electro().lightning(owner,at);
        Vec3[] places={at,at.add(1,0,0),at.add(-1,0,0),at.add(0,0,1),at.add(0,0,-1)};
        for(int group=0;group<5;group++) for(int j=0;j<(group==0?1:3);j++) {
            if(charge.isRemoved() || !game.items().currentEpoch(charge) || !game.items().currentEpoch(epoch)
                    || game.stage()==GameController.Stage.ENDING || !owner.isAlive() || !game.canUseGameplayItems(owner)) return;
            Vec3 point=places[group]; if(!level.hasChunkAt(BlockPos.containing(point))) continue;
            int fuse=group==0?50:60+j;
            PrimedTnt tnt=new PrimedTnt(level,point.x,point.y,point.z,owner);
            tnt.setNoGravity(true); tnt.setDeltaMovement(Vec3.ZERO); tnt.setFuse(fuse);
            tnt.entityTags().add(EXPLOSIVE); game.items().markCurrentItemEpoch(tnt); level.addFreshEntity(tnt);
        }
    }

    private void chaos(ServerPlayer owner) {
        ServerLevel level=owner.level(); String epoch=game.items().epoch();
        Vec3 target=owner.getEyePosition().add(owner.getLookAngle().scale(60));
        Vec3 base=new Vec3((int)target.x,(int)target.y,(int)target.z);
        for(int i=0;i<80;i++) {
            if(!game.items().currentEpoch(epoch) || game.stage()==GameController.Stage.ENDING || !owner.isAlive() || !game.canUseGameplayItems(owner)) return;
            Vec3 at=base.add(level.getRandom().nextInt(71)-35,level.getRandom().nextInt(71)-35,level.getRandom().nextInt(71)-35);
            instantBlast(owner,at,4);
        }
        for(int i=0;i<40;i++) {
            if(!game.items().currentEpoch(epoch) || game.stage()==GameController.Stage.ENDING || !owner.isAlive() || !game.canUseGameplayItems(owner)) return;
            Vec3 at=base.add(level.getRandom().nextInt(71)-35,level.getRandom().nextInt(71)-35,level.getRandom().nextInt(71)-35);
            if(level.hasChunkAt(BlockPos.containing(at))) game.items().electro().lightning(owner,at);
        }
    }

    void instantBlast(ServerPlayer owner,Vec3 at,float radius) {
        instantBlast(owner,owner.level(),at,radius,game.items().epoch());
    }

    /** Данные только непустых блоков одного взрыва: воздух не создаёт тысячи записей/BlockPos. */
    private static final class BlastCell {
        final BlockPos pos;
        final net.minecraft.world.level.block.state.BlockState state;
        final float loss;
        boolean added;
        BlastCell(BlockPos pos,net.minecraft.world.level.block.state.BlockState state,float loss) {
            this.pos=pos; this.state=state; this.loss=loss;
        }
    }

    private static double[] blastRays() {
        double[] steps=new double[(16*16*16-14*14*14)*3]; int next=0;
        for(int x=0;x<16;x++) for(int y=0;y<16;y++) for(int z=0;z<16;z++) {
            if(x!=0 && x!=15 && y!=0 && y!=15 && z!=0 && z!=15) continue;
            double dx=x/15.0*2-1,dy=y/15.0*2-1,dz=z/15.0*2-1;
            double scale=.3/Math.sqrt(dx*dx+dy*dy+dz*dz);
            steps[next++]=dx*scale; steps[next++]=dy*scale; steps[next++]=dz*scale;
        }
        return steps;
    }

    /** Идеи Paper/Moonrise: готовые лучи, локальные чтения и кэш сопротивления.
     * (user) Точная случайная форма кратера не обязательна: сила каждого луча равна radius.
     * Меняется только поиск блоков; native damage, owner, loot и применение разрушений остаются снаружи. */
    public static List<BlockPos> explosionBlocks(net.minecraft.world.level.ServerExplosion blast,
            net.minecraft.world.level.ExplosionDamageCalculator calculator) {
        var level=blast.level(); Vec3 origin=blast.center();
        var result=new it.unimi.dsi.fastutil.objects.ObjectArrayList<BlockPos>();
        var cells=new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<BlastCell>();
        var chunks=new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<net.minecraft.world.level.chunk.LevelChunk>();
        // Фиксированный direct-mapped кэш хранит и воздух, но не создаёт объект на каждую пустую клетку.
        long[] recentKeys=new long[512]; java.util.Arrays.fill(recentKeys,Long.MIN_VALUE);
        BlastCell[] recentCells=new BlastCell[512];
        var probe=new BlockPos.MutableBlockPos();
        net.minecraft.world.level.chunk.LevelChunk lastChunk=null;
        int lastChunkX=Integer.MIN_VALUE,lastChunkZ=Integer.MIN_VALUE;
        for(int ray=0;ray<BLAST_RAYS.length;ray+=3) {
            double x=origin.x,y=origin.y,z=origin.z;
            double dx=BLAST_RAYS[ray],dy=BLAST_RAYS[ray+1],dz=BLAST_RAYS[ray+2];
            float power=blast.radius(),loss=0;
            long previous=Long.MIN_VALUE; BlastCell cell=null;
            while(power>0) {
                int bx=net.minecraft.util.Mth.floor(x),by=net.minecraft.util.Mth.floor(y),bz=net.minecraft.util.Mth.floor(z);
                long key=BlockPos.asLong(bx,by,bz);
                if(key!=previous) {
                    probe.set(bx,by,bz);
                    if(!level.isInWorldBounds(probe)) break;
                    int slot=(bx&7)|((by&7)<<3)|((bz&7)<<6);
                    if(recentKeys[slot]==key) {
                        cell=recentCells[slot]; loss=cell==null ? 0 : cell.loss;
                    } else {
                        int cx=bx>>4,cz=bz>>4;
                        if(cx!=lastChunkX || cz!=lastChunkZ) {
                            long chunkKey=net.minecraft.world.level.ChunkPos.pack(cx,cz);
                            lastChunk=chunks.get(chunkKey);
                            if(lastChunk==null) { lastChunk=level.getChunk(cx,cz); chunks.put(chunkKey,lastChunk); }
                            lastChunkX=cx; lastChunkZ=cz;
                        }
                        var section=lastChunk.getSection(level.getSectionIndex(by));
                        // Пустая секция не требует обращения к palette, сопротивлению или созданию позиции.
                        var state=section.hasOnlyAir() ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                                : section.getBlockState(bx&15,by&15,bz&15);
                        var fluid=state.getFluidState();
                        if(state.isAir() && fluid.isEmpty()) { cell=null; loss=0; }
                        else {
                            cell=cells.get(key);
                            if(cell==null) {
                                BlockPos pos=probe.immutable();
                                var resistance=calculator.getBlockExplosionResistance(blast,level,pos,state,fluid);
                                float attenuation=resistance.isEmpty() ? 0 : (resistance.get()+.3F)*.3F;
                                cell=new BlastCell(pos,state,attenuation); cells.put(key,cell);
                            }
                            loss=cell.loss;
                        }
                        recentKeys[slot]=key; recentCells[slot]=cell;
                    }
                    previous=key;
                }
                power-=loss;
                if(power>0 && cell!=null && !cell.added && !cell.state.isAir()
                        && calculator.shouldBlockExplode(blast,level,cell.pos,cell.state,power)) {
                    cell.added=true; result.add(cell.pos);
                }
                power-=.22500001F;
                x+=dx; y+=dy; z+=dz;
            }
        }
        return result;
    }

    void instantBlast(ServerPlayer owner,ServerLevel level,Vec3 at,float radius,String epoch) {
        instantBlast(owner,level,at,radius,epoch,null);
    }
    private void instantBlast(ServerPlayer owner,ServerLevel level,Vec3 at,float radius,String epoch,
            java.util.function.Consumer<PrimedTnt> sourceInit) {
        nativeBlast(owner,level,at,radius,epoch,false,sourceInit);
    }

    /** Короткие fuse=1/2 были способом выдать защиту, не игровой задержкой. Сущность миру не нужна. */
    void protectedBlast(LivingEntity owner,ServerLevel level,Vec3 at,float radius,
            java.util.function.Consumer<PrimedTnt> sourceInit) {
        nativeBlast(owner,level,at,radius,game.items().epoch(),true,sourceInit);
    }

    private void nativeBlast(LivingEntity owner,ServerLevel level,Vec3 at,float radius,String epoch,
            boolean protectedDamage,java.util.function.Consumer<PrimedTnt> sourceInit) {
        if(level!=game.maps().level() || !level.hasChunkAt(BlockPos.containing(at))
                || !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES)
                || !game.items().currentEpoch(epoch) || game.stage()==GameController.Stage.ENDING) return;
        // A queued blast can outlive its living author; native EntityReference drops removed owners.
        PrimedTnt source=new PrimedTnt(level,at.x,at.y,at.z,owner) {
            @Override public LivingEntity getOwner() { return owner; }
        };
        source.setDeltaMovement(Vec3.ZERO); source.setFuse(0);
        if(owner instanceof ServerPlayer) source.entityTags().add(EXPLOSIVE);
        if(!protectedDamage) source.entityTags().add(LbcDamage.UNPROTECTED_TNT);
        game.items().markCurrentItemEpoch(source);
        if(sourceInit!=null) sourceInit.accept(source);
        try { level.explode(source,at.x,at.y,at.z,radius,Level.ExplosionInteraction.TNT); }
        finally {
            game.items().academy().blastFinished(source);
            game.items().exoperso().blastFinished(source);
        }
    }

    private void step(Charge charge,ServerPlayer owner) {
        ServerLevel level=owner.level();
        Vec3 from=charge.position(),to=from.add(charge.getDeltaMovement());
        if(!level.hasChunkAt(BlockPos.containing(from)) || game.items().magic().suppressedAt(level,from)
                || charge.getDeltaMovement().lengthSqr()<1e-12) { charge.discard(); return; }
        var wall=LbcTerra.wallHit(level,from,to);
        var shield=game.items().wands().shieldHit(owner,from,wall==null ? to : wall.getLocation());
        boolean shieldFirst=shield!=null && (wall==null
                || from.distanceToSqr(shield.getLocation())<=from.distanceToSqr(wall.getLocation()));
        Vec3 end=shieldFirst ? shield.getLocation() : wall==null ? to : wall.getLocation();
        var hits=ProjectileUtil.getManyEntityHitResult(level,owner,from,end,new AABB(from,end).inflate(1),
                entity->game.items().lifetimes().target(owner,entity),
                0,ClipContext.Block.COLLIDER,true,false);
        double nearest=Double.POSITIVE_INFINITY;
        boolean entityHit=false;
        for(var hit:hits) {
            double distance=from.distanceToSqr(hit.getLocation());
            if(distance<nearest) { nearest=distance; end=hit.getLocation(); entityHit=true; }
        }
        charge.setPos(end);
        level.sendParticles(ParticleTypes.EXPLOSION,true,false,end.x,end.y,end.z,1,0,0,0,0);
        level.sendParticles(ParticleTypes.LAVA,end.x,end.y,end.z,2,.1,.1,.1,.4);
        if(entityHit || wall!=null && !shieldFirst && level.hasChunkAt(wall.getBlockPos())) {
            // Удаляем до создания payload: native damage/stop callbacks не могут повторить взрыв.
            charge.discard(); burst(owner,end,charge.academyTrial);
        } else if(wall!=null || shieldFirst) {
            charge.discard();
            if(shieldFirst) LbcOverload.sound(owner,end,"minecraft:item.shield.block",1,1,0);
        }
    }

    /** (user) Именно 85 TNT и 50 молний; не заменять одним взрывом с другой геометрией/уроном. */
    private void burst(ServerPlayer owner,Vec3 origin,boolean academyTrial) {
        var level=owner.level(); String epoch=game.items().epoch();
        var trial=game.items().academy().blastTrial(owner,academyTrial);
        var offsets=new ArrayList<Vec3>(85);
        Vec3[] axes={new Vec3(0,0,1),new Vec3(0,1,0),new Vec3(1,0,0),
                new Vec3(0,0,-1),new Vec3(0,-1,0),new Vec3(-1,0,0)};
        for(int axis=0;axis<axes.length;axis++) for(int i=axis==0 ? 0 : 1;i<=14;i++) {
            if(!game.items().currentEpoch(epoch) || game.stage()==GameController.Stage.ENDING) return;
            Vec3 at=origin.add(axes[axis].scale(i*.5));
            if(!level.hasChunkAt(BlockPos.containing(at))) continue;
            offsets.add(at.subtract(origin));
        }
        if(!offsets.isEmpty()) {
            var burst=new BlastBatch(owner,origin,epoch);
            burst.offsets=offsets.toArray(Vec3[]::new); burst.radius=4; burst.protectedDamage=true;
            burst.sourceInit=source->game.items().academy().blastSource(source,trial);
            blasts.addLast(burst);
        }
        for(int i=0;i<Math.min(50,offsets.size());i++) {
            if(!game.items().currentEpoch(epoch) || game.stage()==GameController.Stage.ENDING) return;
            game.items().academy().blastSource(game.items().electro().lightning(owner,origin.add(offsets.get(i))),trial);
        }
    }

    int pendingShortBlasts() {
        int count=0;
        for(var burst:blasts) if(burst.protectedDamage) count+=burst.offsets.length-burst.next;
        return count;
    }

    /** Общая политика для всего native взрыва, до урона, knockback и расчёта exposure. */
    public boolean explosionTarget(Entity source,Entity target) {
        if(!(source instanceof PrimedTnt tnt) || !source.entityTags().contains(EXPLOSIVE)) return true;
        return tnt.getOwner() instanceof ServerPlayer owner && game.items().currentEpoch(tnt)
                && game.items().wands().projectileTarget(owner,target);
    }
    void stop() {
        for(var search:searches) search.result.cancel(false);
        searches.clear(); game.items().nuclearVisuals().stop(); blasts.clear();
        var detached=List.copyOf(charges); charges.clear();
        for(var charge:detached) charge.discard();
    }
}
