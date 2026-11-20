package me.skywars2.game;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Общая очередь проверки дальних снарядов: ограничиваем CPU, а не дальность полёта. */
public final class ProjectileLifetimes {
    /** Spell trajectories ignore incidental native physics; deliberate owner/velocity controls stay available. */
    public static boolean protectedWandProjectile(Entity entity) {
        if (!(entity instanceof Projectile) || entity instanceof LbcClouds.AcidProjectile) return false;
        return LbcHalloweenWind.managed(entity) || LbcFartus.magic(entity) || entity instanceof LbcIceWand.IceShot || entity instanceof LbcPrism.Charge || LbcNeutral.managed(entity) || entity instanceof LbcAir.Charge || entity instanceof LbcBlind.Charge
                || entity instanceof LbcExasperato.Charge || entity instanceof LbcExoperso.Charge
                || entity instanceof LbcPsycho.Charge || entity instanceof LbcStone.Charge
                || entity instanceof LbcLifestealer.Charge || entity instanceof LbcGovnomet.Charge
                || entity instanceof LbcRuby.Charge || entity instanceof LbcTeleport.Charge
                || entity instanceof LbcTerra.TerraProjectile
                || entity.entityTags().contains("skywars2.wand.fire")
                || entity.entityTags().contains("skywars2.wand.air")
                || entity.entityTags().contains(LbcTeleport.CLOUD);
    }

    private static final int REVIEW_PERIOD = 100;
    private static final int REVIEW_BUDGET = 2048;
    private static final int REVIEWS_PER_TICK = 8;
    private static final double TARGET_BUFFER = 48;
    private static final double LOCAL_NEAREST_RANGE = 32;
    // Длина отрезка ограничивает native DDA: даже диагональ посещает не более 32 клеток за порцию.
    private static final int TERRAIN_SEGMENT = 16, TERRAIN_WORK = 32, TERRAIN_BUDGET = 2048;
    private static final TagKey<EntityType<?>> MOBS = TagKey.create(Registries.ENTITY_TYPE,
            Identifier.parse("minecraft:mobs"));

    private static final class Review {
        final Projectile projectile;
        final boolean terrain;
        Entity owner;
        Vec3 scanPosition, scanDirection;
        int nextReview;
        int targetIndex;
        int targetRevision = -1;
        int age;
        boolean queued;

        Review(Projectile projectile, int nextReview,boolean terrain) {
            this.projectile = projectile;
            this.nextReview = nextReview;
            this.terrain=terrain;
        }
    }

    private final GameController game;
    private final Set<LivingEntity> targets = new LinkedHashSet<>();
    private final ArrayDeque<Review> queue = new ArrayDeque<>();
    private final IdentityHashMap<Projectile, Review> tracked = new IdentityHashMap<>();
    private List<LivingEntity> targetSnapshot = List.of();
    private Map<LivingEntity, Integer> targetRanks;
    private int targetRevision;
    private int snapshotRevision = -1;
    private int serial;
    private boolean stopped;
    private boolean reviewing;

    ProjectileLifetimes(GameController game) {
        this.game = game;
        var level = game.maps().level();
        if (level != null) for (var entity : level.getAllEntities())
            if (entity instanceof LivingEntity living) targets.add(living);
    }

    public void loaded(Entity entity) {
        if (stopped || entity.level() != game.maps().level() || game.items().polymorph().detached(entity)) return;
        if (entity instanceof LivingEntity living && targets.add(living)) targetRevision++;
    }

    public void unloaded(Entity entity) {
        if (entity instanceof LivingEntity living && targets.remove(living)) targetRevision++;
        if (entity instanceof Projectile projectile) forget(projectile);
    }
    /** Visibility changes do not cancel accepted projectile reviews or their private clocks. */
    void polymorphVisibility(Entity entity,boolean visible){
        if(visible){loaded(entity);return;}
        if(entity instanceof LivingEntity living&&targets.remove(living))targetRevision++;
    }

    public void track(Projectile projectile) {
        track(projectile,false);
    }

    /** Общий неизменяемый снимок загруженных целей: самонаведение не сканирует мир заново. */
    List<LivingEntity> loadedTargets() {
        if(snapshotRevision!=targetRevision) {
            targetSnapshot=List.copyOf(targets);
            targetRanks=null;
            snapshotRevision=targetRevision;
        }
        return targetSnapshot;
    }

    /** Native section iteration has its own order. Preserve publication order for equal-distance targets. */
    Map<LivingEntity, Integer> loadedTargetRanks() {
        var snapshot = loadedTargets();
        if (targetRanks == null) {
            var ranks = new IdentityHashMap<LivingEntity, Integer>(snapshot.size());
            for (int i = 0; i < snapshot.size(); i++) ranks.put(snapshot.get(i), i);
            targetRanks = Collections.unmodifiableMap(ranks);
        }
        return targetRanks;
    }

    /** Разрушительные заряды сохраняются также ради блоков; обычные Terra ищут только живые цели. */
    public void track(Projectile projectile,boolean terrain) {
        if (stopped || projectile.isRemoved()) return;
        tracked.computeIfAbsent(projectile, value -> new Review(value,
                REVIEW_PERIOD + Math.floorMod(serial++, REVIEW_PERIOD),terrain));
    }

    public void advance(Projectile projectile, int age) {
        if (stopped) return;
        var review = tracked.get(projectile);
        if (review != null) review.age = age;
        if (review == null || review.queued || age < review.nextReview) return;
        review.queued = true;
        queue.addLast(review);
    }

    /** Один бюджет проверок целей для всех зарегистрированных семейств снарядов. */
    public void tick() {
        if (stopped || queue.isEmpty() || game.items().wands().timeStopped()) return;
        loadedTargets();

        reviewing=true;
        try {
        int budget = REVIEW_BUDGET;
        int terrainBudget = TERRAIN_BUDGET;
        int finished = 0;
        int visits = queue.size();
        while (budget > 0 && finished < REVIEWS_PER_TICK && visits-- > 0 && !queue.isEmpty()) {
            var review = queue.removeFirst();
            var projectile = review.projectile;
            if (tracked.get(projectile) != review) continue;
            if(game.items().polymorph().suspended(projectile)){queue.addLast(review);continue;}
            if (projectile.isRemoved() || projectile.level() != game.maps().level()
                    || !(projectile.getOwner() instanceof ServerPlayer owner)
                    || owner.isRemoved() || !owner.isAlive() || owner.level() != projectile.level()) {
                forget(projectile);
                continue;
            }
            if (owner.entityTags().contains("time_stopped")) {
                queue.addLast(review);
                continue;
            }
            if(review.owner!=owner) {
                review.owner=owner; review.targetIndex=0;
                review.scanPosition=null;
            }
            if (review.targetRevision != snapshotRevision) {
                review.targetIndex = 0;
                review.targetRevision = snapshotRevision;
            }

            boolean possible = false;
            while (budget > 0 && review.targetIndex < targetSnapshot.size()) {
                var candidate = targetSnapshot.get(review.targetIndex++);
                budget--;
                if (target(owner, candidate) && mayReach(projectile, candidate.getBoundingBox())) {
                    possible = true;
                    break;
                }
            }
            if(!possible && review.targetIndex==targetSnapshot.size() && review.terrain) {
                if(terrainBudget<TERRAIN_WORK) { queue.addLast(review); continue; }
                terrainBudget-=TERRAIN_WORK;
                int terrain=terrain(review);
                if(terrain==0) { queue.addLast(review); continue; }
                possible=terrain>0;
            }
            if (possible) {
                review.targetIndex = 0;
                review.queued = false;
                review.scanPosition=null;
                review.nextReview = review.age + REVIEW_PERIOD;
                finished++;
            } else if (review.targetIndex == targetSnapshot.size()) {
                forget(projectile);
                projectile.discard();
                finished++;
            } else {
                queue.addLast(review); // Исчерпание бюджета не означает отсутствие цели.
            }
        }
        } finally { reviewing=false; }
    }

    /** 1: впереди столкновение, -1: доступная траектория закончилась, 0: продолжить в следующем тике. */
    private int terrain(Review review) {
        var projectile=review.projectile;
        var level=(net.minecraft.server.level.ServerLevel)projectile.level();
        Vec3 direction=projectile.getDeltaMovement().normalize(),current=projectile.position();
        if(direction.lengthSqr()<1e-12) return -1;
        if(review.scanPosition==null || review.scanDirection.distanceToSqr(direction)>1e-10
                || current.subtract(review.scanPosition).dot(direction)>0
                || current.subtract(review.scanPosition).cross(direction).lengthSqr()>1e-8) {
            review.scanPosition=current; review.scanDirection=direction;
        }
        Vec3 from=review.scanPosition;
        double min=level.getMinY(),max=level.getMaxY()+1.0;
        // Вне высоты строительства блоков нет; нисходящий выстрел сверху может вернуться в мир.
        if(from.y>=max) {
            if(direction.y>=0) return -1;
            from=from.add(direction.scale((max-1e-6-from.y)/direction.y));
        } else if(from.y<min) {
            if(direction.y<=0) return -1;
            from=from.add(direction.scale((min+1e-6-from.y)/direction.y));
        }
        double length=TERRAIN_SEGMENT;
        if(direction.y>0) length=Math.min(length,(max-from.y)/direction.y);
        else if(direction.y<0) length=Math.min(length,(min-from.y)/direction.y);
        Vec3 end=from.add(direction.scale(Math.max(0,length)));
        var wall=LbcTerra.wallHit(level,from,end);
        if(wall!=null) return level.hasChunkAt(wall.getBlockPos())
                && !level.isOutsideBuildHeight(wall.getBlockPos()) ? 1 : -1;
        if(length<TERRAIN_SEGMENT) return -1;
        review.scanPosition=end;
        return 0;
    }

    public void stop() {
        stopped = true;
        try { queue.clear(); tracked.clear(); }
        finally { stopped=false; }
        // Индекс живых целей принадлежит миру: сохранившиеся игроки/мобы не получают новый ENTITY_LOAD в следующем матче.
    }

    void forget(Projectile projectile) {
        var review = tracked.remove(projectile);
        if (review != null) {
            review.queued = false;
            queue.remove(review);
        }
    }

    private List<LivingEntity> targetSphere(ServerLevel level, Vec3 at, double radius,
                                            Map<LivingEntity, Integer> ranks) {
        // Native intersection excludes the boundary. The final feet-distance check includes it.
        var box = new AABB(at, at).inflate(radius + 1e-6);
        double squaredRadius = radius * radius;
        return level.getEntitiesOfClass(LivingEntity.class, box,
                target -> ranks.containsKey(target) && target.position().distanceToSqr(at) <= squaredRadius);
    }

    List<LivingEntity> nearbyTargets(ServerLevel level, Vec3 at, double radius, Predicate<LivingEntity> selection) {
        var ranks = loadedTargetRanks();
        var nearby = targetSphere(level, at, radius, ranks);
        nearby.removeIf(selection.negate());
        nearby.sort(Comparator.comparingInt(ranks::get));
        return nearby;
    }

    LivingEntity nearestTarget(ServerLevel level, Vec3 at, double radius, boolean preferLastOnTie,
                               Predicate<LivingEntity> selection) {
        var ranks = loadedTargetRanks();
        boolean bounded = Double.isFinite(radius);
        // A target inside the sphere is nearer than every target outside it.
        var local = targetSphere(level, at, bounded ? radius : LOCAL_NEAREST_RANGE, ranks);
        var nearest = nearestEligible(level, at, local, ranks, preferLastOnTie, selection);
        if (nearest != null || bounded) return nearest;
        return nearestEligible(level, at, loadedTargets(), ranks, preferLastOnTie, selection);
    }

    private static LivingEntity nearestEligible(ServerLevel level, Vec3 at, List<LivingEntity> candidates,
                                                Map<LivingEntity, Integer> ranks, boolean preferLastOnTie,
                                                Predicate<LivingEntity> selection) {
        LivingEntity nearest = null;
        double best = Double.POSITIVE_INFINITY;
        int bestRank = 0;
        for (var candidate : candidates) {
            if (candidate.level() != level) continue;
            double squared = candidate.position().distanceToSqr(at);
            if (!(squared <= best)) continue;
            int rank = ranks.get(candidate);
            if (nearest != null && squared == best && (preferLastOnTie ? rank <= bestRank : rank >= bestRank)) continue;
            if (!selection.test(candidate)) continue;
            nearest = candidate;
            best = squared;
            bestRank = rank;
        }
        return nearest;
    }

    LivingEntity nearestBeamTarget(ServerLevel level, Vec3 at, Predicate<Entity> selection) {
        return nearestTarget(level, at, Double.POSITIVE_INFINITY, false,
                target -> !target.isRemoved() && target.isAlive() && selection.test(target));
    }
    boolean target(ServerPlayer owner, Entity entity) {
        return entity instanceof LivingEntity && entity.level() == game.items().spellSources().level(owner)
                && entity.getType().builtInRegistryHolder().is(MOBS)
                && game.items().wands().projectileTarget(owner, entity);
    }

    /** Конечная высота поиска Nuke: учитываем и мобов за высотой строительства, без обхода всего мира. */
    double livingRayExtent(ServerPlayer owner,Vec3 from,Vec3 direction) {
        return livingRayExtent(owner,from,direction,e->target(owner,e));
    }
    double livingRayExtent(ServerPlayer owner,Vec3 from,Vec3 direction,java.util.function.Predicate<Entity> selection){
        double extent=0;
        for(var candidate:targets) if(candidate.level()==owner.level()&&selection.test(candidate)) {
            var box=candidate.getBoundingBox();
            double length=box.getCenter().subtract(from).dot(direction)+box.getSize()*2+1;
            if(length>extent && (box.contains(from) || box.clip(from,from.add(direction.scale(length))).isPresent()))
                extent=length;
        }
        return extent;
    }

    private static boolean mayReach(Projectile projectile, AABB target) {
        Vec3 from = projectile.position(), direction = projectile.getDeltaMovement().normalize();
        AABB expanded = target.inflate(TARGET_BUFFER);
        if (expanded.contains(from)) return true;
        double forward = target.getCenter().subtract(from).dot(direction);
        if (forward < 0) return false;
        double reach = forward + TARGET_BUFFER + target.getSize() * 2;
        return expanded.clip(from, from.add(direction.scale(reach))).isPresent();
    }
    public boolean retireEscapedFireball(AbstractHurtingProjectile projectile) {
        // Spread checks over time even when a single throw published several fireballs.
        if (((projectile.tickCount + projectile.getId()) & 15) != 0 || !game.playing()
                || projectile.level() != game.maps().level() || !game.items().currentEpoch(projectile)) return false;
        var level = (ServerLevel) projectile.level();
        var bounds = projectile.getBoundingBox();
        double verticalSpeed = projectile.getDeltaMovement().y;
        boolean above = bounds.minY >= level.getMaxY() + 1.0 && verticalSpeed >= 0;
        boolean below = bounds.maxY <= level.getMinY() && verticalSpeed <= 0;
        if (!above && !below) return false;
        // Guided, falling-star and trial actors have their own native flight decisions. Delta payloads
        // can be stationary attackable traps. Only review the ordinary straight spell and throw families.
        var tags = projectile.entityTags();
        if (tags.contains(LbcHoming.TAG) || tags.contains(LbcHoming.DONOR_TAG) || tags.contains(LbcBlaze.TRIAL_PROJECTILE)
                || (!LbcSpecialFireballs.escapingFlightCanExpire(projectile) && !game.items().wands().isFireProjectile(projectile))
                || game.items().timeStop().frozen(projectile) || tags.contains("time_stopped")) return false;
        // Native fireballs accelerate along their velocity. With no terrain ahead, retain one
        // if the existing live target index still has a plausible collision along that half-line.
        for (var candidate : loadedTargets()) {
            if (candidate.level() != level || !candidate.isAlive() || candidate.isRemoved() || candidate.isSpectator()) continue;
            var target = candidate.getBoundingBox();
            if (above && target.maxY < bounds.minY - 1 || below && target.minY > bounds.maxY + 1) continue;
            if (mayReach(projectile, target)) return false;
        }
        projectile.discard();
        return true;
    }

    public boolean retireEscapedGravitasArrow(AbstractArrow arrow) {
        if (((arrow.tickCount + arrow.getId()) & 15) != 0 || !arrow.isNoGravity()
                || !arrow.entityTags().contains(LbcProjectileLevelups.GRAVITAS_ARROW)
                || !game.playing() || !(arrow.level() instanceof ServerLevel level)
                || !LbcDimensionItems.ordinary(level)) return false;
        var bounds = arrow.getBoundingBox();
        var motion = arrow.getDeltaMovement();
        boolean above = bounds.minY >= level.getMaxY() + 1.0 && motion.y >= 0;
        boolean below = bounds.maxY <= level.getMinY() && motion.y <= 0;
        if (!above && !below) return false;
        if (arrow.isVehicle() || arrow.isPassenger() || protectedWandProjectile(arrow)
                || LbcHomingArrows.isManaged(arrow) || LbcMadokaBow.recognizes(arrow)
                || LbcSteampunkRocket.managed(arrow) || LbcMachinegunSkill.managed(arrow)
                || game.items().wands().legacyTimeStopped() || game.items().timeStop().stopped(level)
                || game.items().timeStop().frozen(arrow)
                || arrow.entityTags().contains("time_stopped") || game.items().polymorph().suspended(arrow)) return false;
        if (game.items().homing().canSteerArrow(arrow, level)) return false;

        // Native arrows lose 1% of their remaining speed per tick in air. This bounds the
        // straight path, so a distant target cannot retain a stalled arrow forever.
        Vec3 from = arrow.position();
        Vec3 end = from.add(motion.scale(101));
        for (var candidate : loadedTargets()) {
            if (candidate.level() != level || !candidate.isAlive() || candidate.isRemoved() || candidate.isSpectator()) continue;
            var target = candidate.getBoundingBox();
            if (above && target.maxY < bounds.minY - 1 || below && target.minY > bounds.maxY + 1) continue;
            var collision = target.inflate(.5);
            if ((collision.contains(from) || collision.clip(from, end).isPresent())
                    && game.items().wands().canHit(arrow, candidate)) return false;
        }
        arrow.discard();
        return true;
    }

    void checkpointBoundary(){if(reviewing)throw new IllegalStateException("Finish shared lifetime review before checkpointing");}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();return new Checkpoint(links);}
    private record ReviewImage(MatchDamageSources.Ref body,MatchDamageSources.Ref owner,boolean terrain,
                               Vec3 position,Vec3 direction,int next,int targetIndex,int revision,int age,boolean queued){}
    /** One owner for every family's bounded search queue; no re-registration or fresh review budget on replay. */
    final class Checkpoint {
        private final List<ReviewImage> reviews;
        private final List<MatchDamageSources.Ref> ordered,liveTargets,snapshot;
        private final int revision=targetRevision,snapshotVersion=snapshotRevision,sequence=serial;
        Checkpoint(MatchSpellCheckpoint.Links links){
            reviews=tracked.values().stream().filter(r->!links.omitted(r.projectile)).map(r->new ReviewImage(
                    links.key(r.projectile),links.key(r.owner),r.terrain,r.scanPosition,r.scanDirection,r.nextReview,
                    r.targetIndex,r.targetRevision,r.age,r.queued)).toList();
            ordered=queue.stream().filter(r->tracked.get(r.projectile)==r&&!links.omitted(r.projectile)).map(r->links.key(r.projectile)).toList();
            liveTargets=targets.stream().map(links::key).toList();snapshot=targetSnapshot.stream().map(links::key).toList();
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            var restored=new IdentityHashMap<Projectile,Review>();
            for(var image:reviews){var body=(Projectile)links.get(image.body);var r=new Review(body,image.next,image.terrain);
                r.owner=links.get(image.owner);r.scanPosition=image.position;r.scanDirection=image.direction;r.targetIndex=image.targetIndex;
                r.targetRevision=image.revision;r.age=image.age;r.queued=image.queued;restored.put(body,r);}
            var restoredQueue=ordered.stream().map(ref->java.util.Objects.requireNonNull(restored.get((Projectile)links.get(ref)))).toList();
            var restoredTargets=liveTargets.stream().map(ref->(LivingEntity)links.get(ref)).toList();
            var restoredSnapshot=snapshot.stream().map(ref->(LivingEntity)links.get(ref)).toList();
            return ()->{tracked.clear();tracked.putAll(restored);queue.clear();queue.addAll(restoredQueue);
                targets.clear();targets.addAll(restoredTargets);targetSnapshot=restoredSnapshot;targetRanks=null;
                targetRevision=revision;snapshotRevision=snapshotVersion;serial=sequence;stopped=false;};
        }
    }

    LbcCloneSkills.ActorImage cloneImage(Entity source){
        if(!(source instanceof Projectile p))return null;var r=tracked.get(p);if(r==null)return null;
        boolean terrain=r.terrain,queued=r.queued;int next=r.nextReview,index=r.targetIndex,revision=r.targetRevision,age=r.age;
        var owner=r.owner;var position=r.scanPosition;var direction=r.scanDirection;
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity original,Entity clone,java.util.Map<Entity,Entity> family){
                var current=new Review((Projectile)clone,next,terrain);current.owner=owner==null?null:family.getOrDefault(owner,owner);
                current.scanPosition=position;current.scanDirection=direction;current.targetIndex=index;current.targetRevision=revision;current.age=age;current.queued=queued;
                tracked.put((Projectile)clone,current);if(queued)queue.addLast(current);
            }
            public void rollback(Entity clone){if(clone instanceof Projectile shot)forget(shot);}
        };
    }

}
