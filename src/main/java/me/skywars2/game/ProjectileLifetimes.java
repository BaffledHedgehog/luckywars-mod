package me.skywars2.game;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Общая очередь проверки дальних снарядов: ограничиваем CPU, а не дальность полёта. */
public final class ProjectileLifetimes {
    /** Spell trajectories ignore incidental native physics; deliberate owner/velocity controls stay available. */
    public static boolean protectedWandProjectile(Entity entity) {
        if (!(entity instanceof Projectile) || entity instanceof LbcClouds.AcidProjectile) return false;
        return entity instanceof LbcIceWand.IceShot || entity instanceof LbcPrism.Charge || LbcNeutral.managed(entity) || entity instanceof LbcAir.Charge || entity instanceof LbcBlind.Charge
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
    private int targetRevision;
    private int snapshotRevision = -1;
    private int serial;
    private boolean stopped;

    ProjectileLifetimes(GameController game) {
        this.game = game;
        var level = game.maps().level();
        if (level != null) for (var entity : level.getAllEntities())
            if (entity instanceof LivingEntity living) targets.add(living);
    }

    public void loaded(Entity entity) {
        if (stopped || entity.level() != game.maps().level()) return;
        if (entity instanceof LivingEntity living && targets.add(living)) targetRevision++;
    }

    public void unloaded(Entity entity) {
        if (entity instanceof LivingEntity living && targets.remove(living)) targetRevision++;
        if (entity instanceof Projectile projectile) forget(projectile);
    }

    public void track(Projectile projectile) {
        track(projectile,false);
    }

    /** Общий неизменяемый снимок загруженных целей: самонаведение не сканирует мир заново. */
    List<LivingEntity> loadedTargets() {
        if(snapshotRevision!=targetRevision) {
            targetSnapshot=List.copyOf(targets);
            snapshotRevision=targetRevision;
        }
        return targetSnapshot;
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
        if (snapshotRevision != targetRevision) {
            targetSnapshot = List.copyOf(targets);
            snapshotRevision = targetRevision;
        }

        int budget = REVIEW_BUDGET;
        int terrainBudget = TERRAIN_BUDGET;
        int finished = 0;
        int visits = queue.size();
        while (budget > 0 && finished < REVIEWS_PER_TICK && visits-- > 0 && !queue.isEmpty()) {
            var review = queue.removeFirst();
            var projectile = review.projectile;
            if (tracked.get(projectile) != review) continue;
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

    boolean target(ServerPlayer owner, Entity entity) {
        return entity instanceof LivingEntity && entity.level() == owner.level()
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
}
