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
    private static final int SEARCH_SEGMENT=16, SEARCH_BUDGET=80, SEARCH_HEADROOM_BUDGET=96;
    private static final long BLAST_BUDGET_NANOS=4_000_000;
    private static final int BLASTS_PER_TICK=8;
    private static final double[] BLAST_RAYS=blastRays();
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> AIRS=
            net.minecraft.tags.TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final net.minecraft.core.particles.DustColorTransitionOptions NUKE_PARTICLE=
            new net.minecraft.core.particles.DustColorTransitionOptions(0xFFDC40,0,4);
    private static final class Search {
        ServerPlayer owner;
        final ServerLevel level;
        final String epoch;
        Vec3 direction;
        Vec3 position;
        double remaining;
        final boolean walls;
        final boolean blocksOnly;
        final boolean technology;
        final java.util.function.Predicate<Entity> target;
        java.util.function.BiPredicate<BlockPos,net.minecraft.world.level.block.state.BlockState> blockRule;
        boolean shieldBlockable=true,finishAtRange,nexus;
        ReplayableRay replay;boolean finished,started;
        final java.util.concurrent.CompletableFuture<net.minecraft.world.phys.HitResult> result=new java.util.concurrent.CompletableFuture<>();
        final java.util.function.BiConsumer<Vec3,Vec3> trail;
        Search(ServerPlayer owner,String epoch,Vec3 from,Vec3 direction,double remaining,boolean walls,
                java.util.function.BiConsumer<Vec3,Vec3> trail,boolean blocksOnly) {
            this(owner,epoch,from,direction,remaining,walls,trail,blocksOnly,false,null);
        }
        Search(ServerPlayer owner,String epoch,Vec3 from,Vec3 direction,double remaining,boolean walls,
                java.util.function.BiConsumer<Vec3,Vec3> trail,boolean blocksOnly,boolean technology,
                java.util.function.Predicate<Entity> target) {
            this.owner=owner; this.level=owner.level(); this.epoch=epoch;
            this.direction=direction; this.position=from; this.remaining=remaining; this.walls=walls; this.trail=trail;
            this.blocksOnly=blocksOnly;
            this.technology=technology; this.target=target;
        }
    }
    /** Each author gets one quantum per turn, regardless of the number of accepted rays.
     * Cycle newest first work, oldest first work and resumed work; every accepted lane progresses. */
    private static final class SearchQueue implements Iterable<Search> {
        private static final class Lane {
            final ArrayDeque<Search> fresh=new ArrayDeque<>(),resumed=new ArrayDeque<>();
            int turn;
        }
        private final java.util.LinkedHashMap<java.util.UUID,Lane> owners=new java.util.LinkedHashMap<>();
        private int size;
        int size(){return size;}
        boolean isEmpty(){return size==0;}
        void clear(){owners.clear();size=0;}
        void addLast(Search search){
            var lane=owners.computeIfAbsent(search.owner.getUUID(),ignored->new Lane());
            (search.started?lane.resumed:lane.fresh).addLast(search);size++;
        }
        Search removeFirst(){
            var iterator=owners.entrySet().iterator();var entry=iterator.next();
            var lane=entry.getValue();iterator.remove();
            int service=lane.turn;
            for(int skipped=0;skipped<3;skipped++,service=(service+1)%3){
                if(service==2?!lane.resumed.isEmpty():!lane.fresh.isEmpty())break;
            }
            var search=service==2?lane.resumed.removeFirst():service==0?lane.fresh.removeLast():lane.fresh.removeFirst();
            lane.turn=(service+1)%3;size--;
            if(!lane.fresh.isEmpty()||!lane.resumed.isEmpty())owners.put(entry.getKey(),lane);
            return search;
        }
        java.util.Map<java.util.UUID,Integer> turns(){
            var turns=new java.util.HashMap<java.util.UUID,Integer>();
            owners.forEach((owner,lane)->turns.put(owner,lane.turn));return java.util.Map.copyOf(turns);
        }
        void restoreTurns(java.util.Map<java.util.UUID,Integer> turns){owners.forEach((owner,lane)->lane.turn=turns.get(owner));}
        public java.util.Iterator<Search> iterator(){
            return owners.values().stream().flatMap(lane->java.util.stream.Stream.concat(lane.fresh.stream(),lane.resumed.stream())).iterator();
        }
    }
    private final SearchQueue searches=new SearchQueue();
    private boolean searching,searchRewind;
    /** Implementations contain reviewed immutable accepted-cast data; no captured mutable player/future. */
    interface ReplayableRay {
        boolean block(BlockPos pos,net.minecraft.world.level.block.state.BlockState state);
        boolean target(Entity entity);
        void trail(Vec3 from,Vec3 to);
        void complete(net.minecraft.world.phys.HitResult hit);
        default void submitted(RayTicket ticket) { }
        /** Nonzero only for an original discrete selector ray. The same queue owns its cursor. */
        default double sampleStep(){return 0;}
        default Vec3 directionAt(Vec3 point,Vec3 direction){return direction;}
        default net.minecraft.world.phys.HitResult sampleAt(Vec3 point){throw new UnsupportedOperationException();}
        default boolean sampleContinues(Vec3 point){return true;}
        /** An owned asynchronous chunk lease may pause the cursor without blocking the tick. */
        default boolean readyAt(Vec3 point){return true;}
        default MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){return ignored->()->{};}
    }
    /** The original request identity survives replay; its completion bit belongs to Search, not a JDK future. */
    static final class RayTicket {
        private final Search search;
        private RayTicket(Search search){this.search=search;}
        boolean done(){return LbcExasperato.done(search);}
        void cancel(){LbcExasperato.complete(search,null);}
    }
    enum RayMode { SPELL, TERRAIN, PHASED, TECHNOLOGY }
    /** Original recursive sample count, without a second scheduler or mutable cursor in the caller. */
    void replayableSamples(ServerPlayer owner,Vec3 from,Vec3 direction,int advances,ReplayableRay replay){
        double step=replay.sampleStep();
        if(!(step>0)||!Double.isFinite(step)||advances<0)throw new IllegalArgumentException("Discrete ray bounds");
        var search=new Search(owner,game.items().epoch(),from,direction.normalize(),advances*step,false,null,false,false,replay::target);
        search.replay=replay;search.shieldBlockable=false;replay.submitted(new RayTicket(search));
        if(direction.lengthSqr()>1e-12)searches.addLast(search);else complete(search,null);
    }
    /** Typed form of searchTool; custom source predicates and finite-miss policy are accepted once. */
    void replayableTool(ServerPlayer owner,Vec3 from,Vec3 direction,double range,boolean blocksOnly,ReplayableRay replay,boolean finishAtRange){
        replayableTool(owner,from,direction,range,blocksOnly,replay,finishAtRange,false);
    }
    /** Physical tools share the same custom geometry queue while selecting the existing TECH admission. */
    void replayableTool(ServerPlayer owner,Vec3 from,Vec3 direction,double range,boolean blocksOnly,ReplayableRay replay,boolean finishAtRange,boolean technology){
        direction=direction.normalize();double reach=range;
        if(!Double.isFinite(reach)){
            reach=loadedRayExtent(owner,from,direction,true,blocksOnly);
            if(!blocksOnly)reach=Math.max(reach,game.items().lifetimes().livingRayExtent(owner,from,direction,replay::target));
        }
        var search=new Search(owner,game.items().epoch(),from,direction,reach,true,replay::trail,blocksOnly,technology,replay::target);
        search.blockRule=replay::block;search.shieldBlockable=false;search.finishAtRange=finishAtRange;search.replay=replay;
        replay.submitted(new RayTicket(search));
        if(reach>0&&direction.lengthSqr()>1e-12)searches.addLast(search);else complete(search,null);
    }
    /** Native collision/shield/target policies are identical to the corresponding original loaded-ray APIs. */
    void replayableLoaded(ServerPlayer owner,Vec3 from,Vec3 direction,RayMode mode,double maxRange,ReplayableRay replay){
        direction=direction.normalize();boolean walls=mode!=RayMode.PHASED,blocks=mode==RayMode.TERRAIN,tech=mode==RayMode.TECHNOLOGY;
        double reach=Math.min(maxRange,loadedRayExtent(owner,from,direction,walls,blocks));
        var search=new Search(owner,game.items().epoch(),from,direction,reach,walls,replay::trail,blocks,tech,tech?replay::target:null);
        search.replay=replay;
        // Registration must precede even an immediate empty-range completion, otherwise owners retain dead requests.
        replay.submitted(new RayTicket(search));
        if(reach>0&&direction.lengthSqr()>1e-12)searches.addLast(search);else complete(search,null);
    }
    private static boolean done(Search s){return s.replay==null?s.result.isDone():s.finished;}
    private static void complete(Search s,net.minecraft.world.phys.HitResult hit){if(done(s))return;if(s.replay==null)s.result.complete(hit);else{s.finished=true;s.replay.complete(hit);}}
    void replayableSpell(ServerPlayer owner,Vec3 from,Vec3 direction,double range,ReplayableRay replay){
        var search=new Search(owner,game.items().epoch(),from,direction.normalize(),range,true,replay::trail,false,false,replay::target);search.blockRule=replay::block;search.shieldBlockable=false;search.replay=replay;
        if(range>0&&direction.lengthSqr()>1e-12)searches.addLast(search);else complete(search,null);
    }
    private static final class BlastBatch {
        ServerPlayer owner;
        final ServerLevel level;
        final Vec3 origin;
        final String epoch;
        java.util.function.Consumer<PrimedTnt> sourceInit;
        Vec3[] offsets=NUKE_OFFSETS;
        float radius=20;
        boolean protectedDamage;
        boolean cosmos;
        boolean nuclear;
        NuclearTerrain.Shape terrain;
        LbcIndirectKills.Owner terrainCause;
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
    private boolean blastAdvancing,blastRewind;private int nativeBlasts;

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
    public int count() { charges.removeIf(Entity::isRemoved); return charges.size(); }
    public int pendingSearches() { return searches.size(); }
    /** Teleportit observes resident virtual rays through their canonical scheduler, without spawning probes. */
    boolean hostileRayNear(ServerPlayer observer,double radius){
        double reach=radius*radius;
        for(var search:searches){
            if(search.finished||search.blocksOnly||search.level!=observer.level()
                    ||search.position.distanceToSqr(observer.position())>reach
                    ||!game.items().currentEpoch(search.epoch))continue;
            var owner=search.owner;
            if(owner!=observer&&owner.isAlive()&&!owner.isRemoved()&&!owner.isAlliedTo(observer)
                    &&!game.items().wands().inputTimeStopped(owner))return true;
        }
        return false;
    }
    public int mushroomCount() { return game.items().nuclearVisuals().count(); }
    public int pendingBlasts() {
        int remaining=0;
        for(var batch:blasts) remaining+=batch.offsets.length-batch.next;
        return remaining;
    }
    List<Charge> snapshot() { charges.removeIf(Entity::isRemoved); return List.copyOf(charges); }
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
        cast(owner,LbcAcademy.trial(owner.getMainHandItem()));
    }

    void cast(ServerPlayer owner,boolean academyTrial) {
        if(owner.level()!=game.maps().level() || !game.canUseGameplayItems(owner)) return;
        Charge charge=TYPE.create(owner.level(),net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if(charge==null) return;
        charge.setOwner(owner); charge.setPos(owner.getEyePosition());
        charge.academyTrial=academyTrial;
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

    /** Direct helper producers retain source counts and lifetimes, without input/trial hooks. */
    void emitAt(ServerPlayer owner,net.minecraft.world.InteractionHand hand,net.minecraft.world.item.ItemStack stack,int mode,Vec3 origin,Vec3 direction){
        Vec3 aim=direction.normalize();
        float yaw=(float)Math.toDegrees(Math.atan2(-aim.x,aim.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(aim.y,aim.horizontalDistance()));
        switch(mode){
            case 0 -> spawnChargeAt(owner,0,200,0,origin,aim,yaw,pitch);
            // This helper explicitly copies the marked player's Rotation, unlike ordinary click2.
            case 1 -> {for(int lifetime:new int[]{200,100,50})spawnChargeAt(owner,1,lifetime,0,origin,owner.getLookAngle(),owner.getYRot(),owner.getXRot());}
            case 2 -> spawnChargeAt(owner,2,200,0,origin,aim,yaw,pitch);
            case 3 -> chaosAt(owner,origin,aim);
            case 4 -> castNukeAt(owner,origin,aim);
            default -> throw new IllegalArgumentException("Detached exasperato mode "+mode);
        }
        if(mode<=2)for(int soundPitch=0;soundPitch<=2;soundPitch++)
            LbcOverload.sound(owner,origin,"minecraft:entity.generic.explode",1,soundPitch,0);
    }

    private void launchSound(ServerPlayer owner) {
        for(int pitch=0;pitch<=2;pitch++)
            LbcOverload.sound(owner,owner.getEyePosition(),"minecraft:entity.generic.explode",1,pitch,0);
    }

    private void spawnCharge(ServerPlayer owner,int mode,int lifetime,double offset) {
        spawnChargeAt(owner,mode,lifetime,offset,owner.getEyePosition(),owner.getLookAngle(),owner.getYRot(),owner.getXRot());
    }
    private void spawnChargeAt(ServerPlayer owner,int mode,int lifetime,double offset,Vec3 origin,Vec3 direction,float yaw,float pitch) {
        Charge charge=TYPE.create(owner.level(),net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if(charge==null) return;
        charge.mode=mode; charge.lifetime=lifetime;
        Vec3 start=origin.add(direction.scale(offset));
        // Передние заряды не появляются за стеной: сначала ограничиваем сдвиг ближайшим препятствием.
        var wall=offset>0 ? LbcTerra.wallHit(owner.level(),origin,start) : null;
        if(wall!=null) start=wall.getLocation();
        var shield=offset>0 ? game.items().wands().shieldHit(owner,origin,start) : null;
        if(shield!=null) start=shield.getLocation();
        charge.setOwner(owner); charge.setPos(start); charge.entityTags().add("magic");
        charge.setNoGravity(true); charge.setDeltaMovement(direction.scale(mode==2 ? 4.5 : 1));
        charge.setYRot(yaw); charge.setXRot(pitch);
        game.items().markCurrentItemEpoch(charge);
        if(owner.level().addFreshEntity(charge)) {
            charges.add(charge);
            if(lifetime==0) game.items().lifetimes().track(charge,true);
        }
    }

    private void castNuke(ServerPlayer owner) {
        castNukeAt(owner,owner.getEyePosition(),owner.getLookAngle());
    }

    /** The Academy queue casts the nuclear spell; the falling-rocket tick is its consumer. */
    private void castNukeAt(ServerPlayer owner,Vec3 origin,Vec3 direction) {
        String epoch=game.items().epoch();
        var aim=direction.normalize();var reach=loadedRayExtent(owner,origin,aim,true,false);
        var terminal=new NukeAim(owner.getUUID(),epoch);var search=new Search(owner,epoch,origin,aim,reach,true,null,false);search.replay=terminal;
        if(reach>0)searches.addLast(search);else complete(search,null);
    }

    private final class NukeAim implements ReplayableRay {
        final java.util.UUID author;final String epoch;NukeAim(java.util.UUID author,String epoch){this.author=author;this.epoch=epoch;}
        public boolean block(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){throw new IllegalStateException("Nuclear aim uses native collision geometry");}
        public boolean target(Entity entity){throw new IllegalStateException("Nuclear aim uses the canonical target policy");}
        public void trail(Vec3 from,Vec3 to){}
        public void complete(net.minecraft.world.phys.HitResult hit){var owner=game.server().getPlayerList().getPlayer(author);if(hit!=null&&owner!=null)spawnNuke(owner,hit.getLocation(),epoch);}
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
        double reach=loadedRayExtent(owner,from,direction,walls,blocksOnly);
        var search=new Search(owner,game.items().epoch(),from,direction,reach,walls,trail,blocksOnly);
        if(reach>0) searches.addLast(search); else complete(search,null);
        return search.result;
    }
    private double loadedRayExtent(ServerPlayer owner,Vec3 from,Vec3 direction,boolean walls,boolean blocksOnly) {
        // Высота строительства ограничивает лишь наличие блоков. Мобы выше/ниже неё тоже остаются целями.
        double reach=blocksOnly?0:game.items().lifetimes().livingRayExtent(owner,from,direction);
        if(walls && Math.abs(direction.y)>1e-12) {
            double buildExit=Math.max((owner.level().getMinY()-from.y)/direction.y,
                    (owner.level().getMaxY()+1.0-from.y)/direction.y);
            reach=Math.max(reach,buildExit);
        } else if(walls && !owner.level().isOutsideBuildHeight(BlockPos.containing(from))) reach=Double.POSITIVE_INFINITY;
        return reach;
    }

    /** Physical targeting shares the queue/DDA and natural extent, with mechanism suppression only.
     * Infinite maxRange leaves the loaded terrain/target extent in control, including empty vertical misses. */
    java.util.concurrent.CompletableFuture<net.minecraft.world.phys.HitResult> useTechnologyRay(ServerPlayer owner,
            Vec3 from,Vec3 direction,double maxRange,java.util.function.Predicate<Entity> target,
            java.util.function.BiConsumer<Vec3,Vec3> trail) {
        direction=direction.normalize();
        double reach=Math.min(maxRange,loadedRayExtent(owner,from,direction,true,false));
        var search=new Search(owner,game.items().epoch(),from,direction,reach,true,trail,false,true,target);
        if(reach>0 && direction.lengthSqr()>1e-12) searches.addLast(search); else complete(search,null);
        return search.result;
    }

    private boolean searchOwnerSuppressed(Search search) {
        if(!search.technology) return game.items().magic().suppressed(search.owner);
        return game.items().magic().mechanismSuppressed(search.owner);
    }

    /** Instant source spells reuse this CPU-budgeted, epoch-owned resident DDA. */
    java.util.concurrent.CompletableFuture<net.minecraft.world.phys.HitResult> searchSpell(ServerPlayer owner,
            Vec3 from,Vec3 direction,boolean blocksOnly,
            java.util.function.BiPredicate<BlockPos,net.minecraft.world.level.block.state.BlockState> blocks,
            java.util.function.Predicate<Entity> targets,java.util.function.BiConsumer<Vec3,Vec3> trail){
        return searchSpell(owner,from,direction,Double.POSITIVE_INFINITY,blocksOnly,blocks,targets,trail);
    }
    /** Finite source beams also render through empty sky; unbounded spells keep their natural extent. */
    java.util.concurrent.CompletableFuture<net.minecraft.world.phys.HitResult> searchSpell(ServerPlayer owner,
            Vec3 from,Vec3 direction,double range,boolean blocksOnly,
            java.util.function.BiPredicate<BlockPos,net.minecraft.world.level.block.state.BlockState> blocks,
            java.util.function.Predicate<Entity> targets,java.util.function.BiConsumer<Vec3,Vec3> trail){
        return searchTool(owner,from,direction,range,blocksOnly,blocks,targets,trail,false);
    }
    /** Optional finite-range terminal; suppression/chunk/lifecycle cancellation remains a null result. */
    java.util.concurrent.CompletableFuture<net.minecraft.world.phys.HitResult> searchTool(ServerPlayer owner,
            Vec3 from,Vec3 direction,double range,boolean blocksOnly,
            java.util.function.BiPredicate<BlockPos,net.minecraft.world.level.block.state.BlockState> blocks,
            java.util.function.Predicate<Entity> targets,java.util.function.BiConsumer<Vec3,Vec3> trail,boolean finishAtRange){
        direction=direction.normalize();
        double reach=range;
        if(!Double.isFinite(reach)){
            reach=loadedRayExtent(owner,from,direction,true,blocksOnly);
            if(!blocksOnly&&targets!=null)reach=Math.max(reach,game.items().lifetimes().livingRayExtent(owner,from,direction,targets));
        }
        var search=new Search(owner,game.items().epoch(),from,direction,reach,true,trail,blocksOnly,false,targets);
        search.blockRule=blocks;search.shieldBlockable=false;search.finishAtRange=finishAtRange;
        if(reach>0&&direction.lengthSqr()>1e-12)searches.addLast(search);else complete(search,null);
        return search.result;
    }
    /** Dedicated Nexus tools share exact collision and CPU scheduling without ordinary-wand admission. */
    java.util.concurrent.CompletableFuture<net.minecraft.world.phys.HitResult> searchNexusTool(ServerPlayer owner,Vec3 from,Vec3 direction,double range,
            java.util.function.BiPredicate<BlockPos,net.minecraft.world.level.block.state.BlockState> blocks,java.util.function.Predicate<Entity> targets){
        return searchNexusTool(owner,from,direction,range,blocks,targets,null);
    }
    java.util.concurrent.CompletableFuture<net.minecraft.world.phys.HitResult> searchNexusTool(ServerPlayer owner,Vec3 from,Vec3 direction,double range,
            java.util.function.BiPredicate<BlockPos,net.minecraft.world.level.block.state.BlockState> blocks,java.util.function.Predicate<Entity> targets,
            java.util.function.BiConsumer<Vec3,Vec3> trail){
        var search=new Search(owner,game.items().epoch(),from,direction.normalize(),range,true,trail,false,false,targets);
        search.nexus=true;search.blockRule=blocks;search.shieldBlockable=false;
        if(range>0&&direction.lengthSqr()>1e-12)searches.addLast(search);else complete(search,null);return search.result;
    }
    private boolean searchPointSuppressed(Search search,Vec3 point) {
        if(search.nexus)return false;
        return search.technology ? game.items().magic().mechanismSuppressedAt(search.level,point)
                : game.items().magic().suppressedAt(search.level,point);
    }
    private double searchSuppressionFraction(Search search,Vec3 from,Vec3 to) {
        if(search.nexus)return Double.POSITIVE_INFINITY;
        if(!search.technology) return game.items().magic().suppressionFraction(search.level,from,to);
        return game.items().magic().mechanismSuppressionFraction(search.level,from,to);
    }

    /** Ограничиваем работу, а не дальность: каждый незавершённый луч продолжится в следующих тиках. */
    void tickSearchesWhileMapStopped(){searchTick();}
    private void searchTick() {
        if(searchRewind)return;if(searching)throw new IllegalStateException("Reentrant ray advance");searching=true;try {
        if(!game.server().tickRateManager().runsNormally()||game.items().wands().legacyTimeStopped()) return;
        long started=System.nanoTime(),headroom=EffectTerrain.deadlineNanos();
        boolean spare=headroom-started>=4_000_000;
        // Keep a small progress floor on a busy tick; otherwise share the terrain deadline.
        long deadline=Math.max(started+500_000,Math.min(headroom,started+(spare?4_000_000:3_500_000)));
        int budget=headroom<=started?8:spare?SEARCH_HEADROOM_BUDGET:SEARCH_BUDGET;
        for(int work=0;work<budget && !searches.isEmpty() && System.nanoTime()<deadline;work++) {
            var search=searches.removeFirst(); var owner=search.owner; var level=search.level;
            if(done(search)) continue;
            if(game.items().timeStop().stopped(level)){searches.addLast(search);continue;}
            boolean sampled=search.replay!=null&&search.replay.sampleStep()>0;
            boolean active=search.nexus?game.items().currentEpoch(search.epoch)&&game.playing()&&owner.isAlive()&&!owner.isRemoved()
                    &&game.canUseGameplayItems(owner)&&!level.dimension().equals(LbcHedgehogDimensions.IMPRINTED):sampled?game.items().currentEpoch(search.epoch)&&game.playing()&&owner.isAlive()&&!owner.isRemoved()
                    &&game.canUseGameplayItems(owner)&&LbcDimensionItems.ordinary(level):valid(owner,search.epoch);
            if(!active || owner.level()!=level || !sampled&&!search.nexus&&searchOwnerSuppressed(search)) { complete(search,null); continue; }
            if(owner.entityTags().contains("time_stopped")) { searches.addLast(search); continue; }
            if((sampled||search.nexus)&&game.items().timeStop().stopped(level)){searches.addLast(search);continue;}
            search.started=true;
            Vec3 from=search.position;
            if(sampled){
                if(!search.replay.readyAt(from)){searches.addLast(search);continue;}
                if(!level.hasChunkAt(BlockPos.containing(from))){complete(search,null);continue;}
                search.direction=search.replay.directionAt(from,search.direction).normalize();
                var hit=search.replay.sampleAt(from);
                if(hit!=null){complete(search,hit);continue;}
                if(search.remaining<=1e-7||!search.replay.sampleContinues(from)){complete(search,null);continue;}
                double step=search.replay.sampleStep();search.position=from.add(search.direction.scale(step));
                search.remaining-=step;searches.addLast(search);continue;
            }
            if(!level.hasChunkAt(BlockPos.containing(from))
                    || searchPointSuppressed(search,from)) { complete(search,null); continue; }
            if(search.remaining<=1e-7){
                complete(search,search.finishAtRange?net.minecraft.world.phys.BlockHitResult.miss(from,
                        net.minecraft.core.Direction.getApproximateNearest(search.direction),BlockPos.containing(from)):null);continue;
            }
            double length=Math.min(SEARCH_SEGMENT,search.remaining);
            Vec3 end=from.add(search.direction.scale(length));
            if(Math.abs(end.x)>30_000_000 || Math.abs(end.z)>30_000_000) { complete(search,null); continue; }
            var wall=search.blockRule!=null?LbcTerra.wallHit(level,from,end,search.blockRule)
                    :search.walls?LbcTerra.wallHit(level,from,end):LbcTerra.wallHit(level,from,end,state->false);
            if(wall!=null) end=wall.getLocation();
            var shield=search.blocksOnly || search.technology || !search.shieldBlockable?null:game.items().wands().shieldHit(owner,from,end);
            if(shield!=null) end=shield.getLocation();
            double suppression=searchSuppressionFraction(search,from,end);
            if(Double.isFinite(suppression)) end=from.lerp(end,suppression);
            var hit=search.blocksOnly?null:ProjectileUtil.getEntityHitResult(level,owner,from,end,new AABB(from,end).inflate(1),
                    entity->search.target==null?game.items().lifetimes().target(owner,entity):search.target.test(entity),0);
            if(search.trail!=null) search.trail.accept(from,hit==null?end:hit.getLocation());
            if(hit!=null) complete(search,hit);
            else if(shield!=null || Double.isFinite(suppression)) complete(search,null);
            else if(wall!=null) complete(search,level.hasChunkAt(wall.getBlockPos())?wall:null);
            else {
                search.position=end; search.remaining-=length; searches.addLast(search);
            }
        }
        }finally{searching=false;}
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
        } finally {
            batch.clear();
            // A freshly added actor in a hidden section may be discarded before an unload event.
            charges.removeIf(Entity::isRemoved);
        }
        searchTick();
        blastTick();
    }

    /** Один нативный взрыв неделим; бюджет проверяется между взрывами, до следующего. */
    void blastTick() {
        if(blastRewind)return;if(blastAdvancing)throw new IllegalStateException("Reentrant blast queue advance");blastAdvancing=true;try{
        if(blasts.isEmpty()) return;
        long started=System.nanoTime();
        for(int work=0;work<BLASTS_PER_TICK && !blasts.isEmpty();work++) {
            var burst=blasts.removeFirst();
            if(!game.items().currentEpoch(burst.epoch) || game.stage()==GameController.Stage.ENDING
                    || !burst.cosmos && burst.level!=game.maps().level()) continue;
            if(game.items().timeStop().stopped(burst.level)) { blasts.addLast(burst); continue; }
            if(burst.terrain!=null){
                if(!burst.level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES)){burst.terrain=null;blasts.addLast(burst);continue;}
                if(burst.terrain.step(game,burst.level,burst.terrainCause,()->nuclearTerrainApplication(burst),
                        writes->withNuclearTerrainSource(burst,writes)))burst.terrain=null;
                blasts.addLast(burst);
                if(System.nanoTime()-started>=BLAST_BUDGET_NANOS)break;
                continue;
            }
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
                        burst.protectedDamage,burst.sourceInit,false,burst.nuclear);
            if(burst.next<burst.offsets.length && game.items().currentEpoch(burst.epoch)
                    && game.stage()!=GameController.Stage.ENDING) blasts.addLast(burst);
            // Round-robin даёт прогресс каждой серии, даже при непрерывных новых попаданиях.
            if(System.nanoTime()-started>=BLAST_BUDGET_NANOS) break;
        }
        }finally{blastAdvancing=false;}
    }

    private net.minecraft.world.level.ServerExplosion nuclearTerrainApplication(BlastBatch batch){
        var owner=batch.owner;var at=batch.origin;var level=batch.level;
        PrimedTnt source=new PrimedTnt(level,at.x,at.y,at.z,owner){@Override public LivingEntity getOwner(){return owner;}};
        source.setDeltaMovement(Vec3.ZERO);source.setFuse(0);source.entityTags().add(EXPLOSIVE);
        source.entityTags().add(LbcDamage.UNPROTECTED_TNT);game.items().markCurrentItemEpoch(source);
        try{
            if(batch.sourceInit!=null)batch.sourceInit.accept(source);
            var interaction=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLOSION_DROP_DECAY)
                    ?net.minecraft.world.level.Explosion.BlockInteraction.DESTROY_WITH_DECAY:net.minecraft.world.level.Explosion.BlockInteraction.DESTROY;
            return new net.minecraft.world.level.ServerExplosion(level,source,level.damageSources().explosion(source,owner),null,at,20,false,interaction);
        }finally{game.items().academy().blastFinished(source);game.items().exoperso().blastFinished(source);}
    }
    private void withNuclearTerrainSource(BlastBatch batch,Runnable writes){
        var source=(PrimedTnt)batch.terrain.nativeApplication.getDirectSourceEntity();
        try{if(batch.sourceInit!=null)batch.sourceInit.accept(source);writes.run();}
        finally{game.items().academy().blastFinished(source);game.items().exoperso().blastFinished(source);}
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
    private void cosmosBlast(BlastBatch batch) {nativeBlasts++;try{
        var source=EntityTypes.CREEPER.create(batch.level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if(source==null) return;
        source.setPos(batch.origin);
        source.setCustomName(net.minecraft.network.chat.Component.translatable("cosmos"));
        source.entityTags().add(LbcSkyHazards.COSMOS); game.items().markCurrentItemEpoch(source);
        var damage=batch.level.damageSources().explosion(source,batch.owner==null?source:batch.owner);
        // The source is a mob explosion: retain mobGriefing and do not apply the TNT protection setting.
        batch.level.explode(source,damage,null,batch.origin.x,batch.origin.y,batch.origin.z,
                batch.radius,false,Level.ExplosionInteraction.MOB);
        }finally{nativeBlasts--;}
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
        game.items().nuclearVisuals().show(owner.level(),origin,12,exoperso,exoperso?200:400);
        String[] sounds={"minecraft:entity.dragon_fireball.explode","minecraft:entity.generic.explode",
                "minecraft:item.firecharge.use","minecraft:entity.firework_rocket.large_blast","minecraft:entity.firework_rocket.large_blast_far"};
        for(var sound:sounds) LbcOverload.sound(owner,origin,sound,100,0,0);
        var batch=new BlastBatch(owner,origin,epoch); batch.sourceInit=sourceInit;
        batch.nuclear=true;batch.terrain=NuclearTerrain.wand(origin);
        // Native TNT is neither a Projectile nor a WILD terrain writer.
        batch.terrainCause=game.items().indirectKills().coldCause(owner,false);batch.next=1;
        blasts.addLast(batch);
        // Native damage is synchronous; a first lethal wave cannot leave an unharmed
        // author producing new payloads while the common terrain queue catches up.
        nativeBlast(owner,owner.level(),origin,20,epoch,false,sourceInit,false,true);
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
        chaosAt(owner,owner.getEyePosition(),owner.getLookAngle());
    }
    private void chaosAt(ServerPlayer owner,Vec3 origin,Vec3 direction) {
        ServerLevel level=owner.level(); String epoch=game.items().epoch();
        Vec3 target=origin.add(direction.scale(60));
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

    /** Encounter fuse0 bursts are native and unprotected, in their actual encounter dimension. */
    void encounterBlast(LivingEntity owner,ServerLevel level,Vec3 at,float radius,
            java.util.function.Consumer<PrimedTnt> sourceInit) {
        nativeBlast(owner,level,at,radius,game.items().epoch(),false,sourceInit,true);
    }

    private void nativeBlast(LivingEntity owner,ServerLevel level,Vec3 at,float radius,String epoch,
            boolean protectedDamage,java.util.function.Consumer<PrimedTnt> sourceInit) {
        nativeBlast(owner,level,at,radius,epoch,protectedDamage,sourceInit,false);
    }
    private void nativeBlast(LivingEntity owner,ServerLevel level,Vec3 at,float radius,String epoch,
            boolean protectedDamage,java.util.function.Consumer<PrimedTnt> sourceInit,boolean encounterDimension) {
        nativeBlast(owner,level,at,radius,epoch,protectedDamage,sourceInit,encounterDimension,false);
    }
    private void nativeBlast(LivingEntity owner,ServerLevel level,Vec3 at,float radius,String epoch,
            boolean protectedDamage,java.util.function.Consumer<PrimedTnt> sourceInit,boolean encounterDimension,boolean nuclear) {nativeBlasts++;try{
        if(!encounterDimension && level!=game.maps().level() || !level.hasChunkAt(BlockPos.containing(at))
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
        try { if(sourceInit!=null) sourceInit.accept(source);
            if(nuclear)NuclearTerrain.damageWave(source,()->level.explode(source,at.x,at.y,at.z,radius,Level.ExplosionInteraction.TNT));
            else level.explode(source,at.x,at.y,at.z,radius,Level.ExplosionInteraction.TNT);
        }
        finally {
            game.items().academy().blastFinished(source);
            game.items().exoperso().blastFinished(source);
        }
        }finally{nativeBlasts--;}
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
            burst.sourceInit=MatchBlastCheckpoint.academyBinding(game,trial);
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
        if(LbcOreshek.managed(source))return game.items().oreshek().canAffect(source,target);
        if(!(source instanceof PrimedTnt tnt) || !source.entityTags().contains(EXPLOSIVE)) return true;
        return tnt.getOwner() instanceof ServerPlayer owner && game.items().currentEpoch(tnt)
                && game.items().wands().projectileTarget(owner,target);
    }
    void stop() {
        for(var search:searches) {if(search.replay==null)search.result.cancel(false);else complete(search,null);}
        searches.clear();searchRewind=false; game.items().nuclearVisuals().stop(); blasts.clear();blastRewind=false;
        var detached=List.copyOf(charges); charges.clear();
        for(var charge:detached) charge.discard();
    }
    void searchBoundary(){if(searching)throw new IllegalStateException("Finish ray callbacks before capture/replay");}
    void prepareSearchCheckpoint(){searchRewind=true;searches.clear();}
    void cancelSearchCheckpoint(){searchRewind=false;}
    SearchCheckpoint checkpointSearches(MatchSpellCheckpoint.Links links){return new SearchCheckpoint(links);}
    final class SearchCheckpoint {
        private record Image(Search original,MatchDamageSources.Ref owner,Vec3 position,Vec3 direction,double remaining,boolean finished,boolean started,MatchQueuedWorkCheckpoint.Image terminal) { }
        private final List<Image> images;
        private final java.util.Map<java.util.UUID,Integer> turns;
        SearchCheckpoint(MatchSpellCheckpoint.Links links){var list=new ArrayList<Image>();for(var s:searches){if(s.replay==null)throw new IllegalStateException("Missing typed continuation for opaque ray completion");list.add(new Image(s,links.key(s.owner),s.position,s.direction,s.remaining,s.finished,s.started,s.replay.checkpoint(links)));}images=List.copyOf(list);turns=searches.turns();}
        Runnable prepare(MatchSpellCheckpoint.Links links){var terminalPlans=images.stream().map(image->image.terminal.prepare(links)).toList();var owners=new java.util.IdentityHashMap<Search,ServerPlayer>();for(var image:images){MatchAuthorityCheckpoint.world(game,image.original.level);if(!(links.get(image.owner) instanceof ServerPlayer p))throw new IllegalStateException("Restore current ray author bodies first");owners.put(image.original,p);}
            return ()->{terminalPlans.forEach(Runnable::run);searches.clear();for(var image:images){var s=image.original;s.owner=owners.get(s);s.position=image.position;s.direction=image.direction;s.remaining=image.remaining;s.finished=image.finished;s.started=image.started;searches.addLast(s);}searches.restoreTurns(turns);searchRewind=false;};}
    }


    void blastBoundary(){if(nativeBlasts!=0||blastAdvancing||!batch.isEmpty())throw new IllegalStateException("Finish explosive callbacks before checkpointing");}
    void prepareBlastCheckpoint(){blastRewind=true;blasts.clear();}
    void cancelBlastCheckpoint(){blastRewind=false;}
    BlastCheckpoint checkpointBlasts(MatchBlastCheckpoint.Graph graph){return new BlastCheckpoint(graph);}
    final class BlastCheckpoint {
        private record Actor(MatchDamageSources.Ref key,int age,int mode,int life,boolean trial){}
        private record Burst(BlastBatch original,MatchDamageSources.Ref owner,Vec3[] offsets,float radius,boolean protectedDamage,boolean cosmos,int next,java.util.function.Consumer<PrimedTnt> init,boolean nuclear,NuclearTerrain.Shape terrain,LbcIndirectKills.Owner terrainCause){}
        private final List<Actor> actors;private final List<Burst> queue;
        BlastCheckpoint(MatchBlastCheckpoint.Graph graph){actors=charges.stream().map(c->new Actor(graph.links.key(c),c.age,c.mode,c.lifetime,c.academyTrial)).toList();queue=blasts.stream().map(b->{graph.binding(b.sourceInit);return new Burst(b,graph.links.key(b.owner),b.offsets.clone(),b.radius,b.protectedDamage,b.cosmos,b.next,b.sourceInit,b.nuclear,b.terrain==null?null:b.terrain.copy(),b.terrainCause);}).toList();}
        Runnable prepare(MatchSpellCheckpoint.Links links){var bodies=new LinkedHashSet<Charge>();for(var a:actors){if(!(links.get(a.key) instanceof Charge c))throw new IllegalStateException("Restore native Exasperato charges first");bodies.add(c);}var owners=new java.util.IdentityHashMap<BlastBatch,ServerPlayer>();for(var b:queue){MatchAuthorityCheckpoint.world(game,b.original.level);var owner=links.get(b.owner);if(owner!=null&&!(owner instanceof ServerPlayer))throw new IllegalStateException("Restore current burst authors first");owners.put(b.original,(ServerPlayer)owner);}
            return ()->{charges.clear();charges.addAll(bodies);for(var a:actors){var c=(Charge)links.get(a.key);c.age=a.age;c.mode=a.mode;c.lifetime=a.life;c.academyTrial=a.trial;}blasts.clear();for(var b:queue){var live=b.original;live.owner=owners.get(live);live.offsets=b.offsets.clone();live.radius=b.radius;live.protectedDamage=b.protectedDamage;live.cosmos=b.cosmos;live.next=b.next;live.sourceInit=b.init;live.nuclear=b.nuclear;live.terrain=b.terrain==null?null:b.terrain.copy();live.terrainCause=b.terrainCause;blasts.addLast(live);}blastRewind=false;};}
    }

    /** Charge state has no Search/RayTicket link; searches are separate accepted virtual casts. */
    LbcCloneSkills.ActorImage cloneImage(Entity source) {
        if(!(source instanceof Charge c)||c.academyTrial)return null;
        int age=c.age,mode=c.mode,life=c.lifetime;
        return CloneComplexProjectileStates.image(e->{var n=(Charge)e;n.age=age;n.mode=mode;n.lifetime=life;
            n.academyTrial=false;charges.add(n);},e->charges.remove(e));
    }

}
