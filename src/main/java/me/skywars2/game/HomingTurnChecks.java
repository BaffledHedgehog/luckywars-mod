package me.skywars2.game;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.ActorCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Actual native flights near a player, a logical card and invariant checks of the shared angular step. */
final class HomingTurnChecks {
    private static Scene running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Homing turn: "+why);}
    private static double angle(Vec3 a,Vec3 b){return Math.toDegrees(Math.atan2(a.cross(b).length(),a.dot(b)));}
    private static Vec3 heading(double degrees){return new Vec3(Math.sin(Math.toRadians(degrees)),0,Math.cos(Math.toRadians(degrees)));}
    private static void geometry(){
        for(double degrees:new double[]{-180,-90,-1.1,-.5,0,.5,1.1,90,180}){
            var goal=heading(degrees);var motion=new Vec3(0,0,7.25);double remaining=angle(motion,goal);
            for(int i=0;i<181;i++){
                var next=ProjectileSteering.turn(motion,goal,Math.PI/180);double after=angle(next,goal);
                require(Math.abs(next.length()-7.25)<1e-10,"angular step preserves physical speed");
                require(angle(motion,next)<=1.00000001&&after<=remaining+1e-8,"bounded monotonic turn without overshoot");
                if(remaining<1)require(after<1e-8,"sub-degree remainder aligns exactly");
                motion=next;remaining=after;
            }
            require(remaining<1e-8,"even opposite direction reaches its target within180 angular steps");
        }
        require(ProjectileSteering.turn(Vec3.ZERO,new Vec3(0,1,0),1).equals(Vec3.ZERO),"stationary projectile never invents motion");
        require(ProjectileSteering.turn(new Vec3(1,2,3),Vec3.ZERO,1).equals(new Vec3(1,2,3)),"coincident target keeps finite original motion");
    }
    private static final class HomingQueries implements AutoCloseable {
        final Object manager;
        final Field field;
        final LevelEntityGetter<Entity> original;
        int broadQueries;

        HomingQueries(ServerLevel level) {
            var nativeManager = ((ActorCheckpointAccess.LevelEntities) level).skywars2$manager();
            manager = nativeManager;
            original = nativeManager.getEntityGetter();
            try {
                field = manager.getClass().getDeclaredField("entityGetter");
                field.setAccessible(true);
                var observer = Proxy.newProxyInstance(LevelEntityGetter.class.getClassLoader(),
                        new Class<?>[]{LevelEntityGetter.class}, (proxy, method, args) -> {
                    if (method.getName().equals("get") && args != null) {
                        for (Object arg : args) {
                            if (arg instanceof AABB box && Math.abs(box.maxX - box.minX - 200) < 1e-4
                                    && Math.abs(box.maxY - box.minY - 200) < 1e-4
                                    && Math.abs(box.maxZ - box.minZ - 200) < 1e-4) broadQueries++;
                        }
                    }
                    try {
                        return method.invoke(original, args);
                    } catch (InvocationTargetException error) {
                        throw error.getCause();
                    }
                });
                field.set(manager, observer);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Install scoped Homing query observer", error);
            }
        }

        @Override public void close() {
            try {
                field.set(manager, original);
            } catch (IllegalAccessException error) {
                throw new IllegalStateException("Restore native entity getter", error);
            }
        }
    }

    private static final class ProbeCow extends Cow {
        int roleChecks;

        ProbeCow(net.minecraft.server.level.ServerLevel level) {
            super(EntityTypes.COW, level);
        }

        @Override public boolean isAlive() {
            roleChecks++;
            return super.isAlive();
        }
    }

    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final List<Projectile> shots=new ArrayList<>();
        final List<Vec3> initial=new ArrayList<>();LivingEntity target;LbcDanmaku.Card card;Vec3 cardStart,cardGoal;
        int at,phase=-1,sparseQueries;boolean closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);}
        <T extends Projectile>T shot(T body,Vec3 point,Vec3 direction,net.minecraft.server.level.ServerPlayer author){
            body.setOwner(author);body.setPos(point);body.setDeltaMovement(direction);body.setNoGravity(true);
            require(f.level.addFreshEntity(body),"real native projectile publication");f.owned.add(body.getUUID());shots.add(body);initial.add(direction);return body;
        }
        void setup(){
            geometry();f.prepareParticipants();game.config().lbcSkills=true;game.config().ranked=true;
            game.players().get(f.owner.getUUID()).lbcSkill=30;game.players().get(f.other.getUUID()).lbcSkill=0;
            require(game.lbc().hasSkill(f.owner,30)&&!game.lbc().hasSkill(f.other,30),"actual personal skill admission");
            f.move(f.owner,f.home.add(0,60,0));f.move(f.other,f.owner.position().add(30,0,0));
            var cow=new Cow(EntityTypes.COW,f.level);cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(f.owner.position().add(0,4,15));
            require(f.level.addFreshEntity(cow),"native target publication");target=cow;f.owned.add(cow.getUUID());
            nearbySelectors();
            keeperVolley();
        }

        ProbeCow probe(Vec3 point) {
            var cow = new ProbeCow(f.level);
            cow.setPos(point);
            cow.setNoGravity(true);
            cow.setNoAi(true);
            require(f.level.addFreshEntity(cow), "published real selector probe");
            f.owned.add(cow.getUUID());
            require(game.items().lifetimes().loadedTargets().contains(cow), "probe exists in the shared target index");
            return cow;
        }

        void nearbySelectors() {
            var from = f.owner.position().add(0, 60, 0);
            // Both probes sit in loaded sections, with an exact tie at separate X coordinates.
            var first = probe(from.add(-4, 0, 0));
            var last = probe(from.add(4, 0, 0));
            var out = probe(from.add(0, 120, 0));
            var edge = probe(from.add(0, -100, 0));
            try {
                var lifetimes = game.items().lifetimes();
                require(lifetimes.loadedTargets().size() <= 64, "sparse scene stays within the fast-path limit");
                require(f.level.getEntity(edge.getUUID()) == edge, "radius witness is native-visible");
                try (var queries = new HomingQueries(f.level)) {
                    require(lifetimes.nearestTarget(f.level, from, 100, true, candidate -> candidate == edge) == edge,
                            "exactly 100 blocks includes the target's feet");
                    edge.setPos(from.add(0, -100.001, 0));
                    require(lifetimes.nearestTarget(f.level, from, 100, true, candidate -> candidate == edge) == null,
                            "live feet just outside the sphere are excluded even when the body crosses its edge");

                    Vec3 cold = new Vec3(2304.5, from.y, 2304.5);
                    var cell = BlockPos.containing(cold);
                    int chunkX = cell.getX() >> 4, chunkZ = cell.getZ() >> 4;
                    require(f.level.getChunkSource().getChunkNow(chunkX, chunkZ) == null
                            && !f.level.areEntitiesLoaded(ChunkPos.pack(chunkX, chunkZ)), "visibility witness uses an unloaded chunk");
                    edge.setPos(cold);
                    var manager = ((ActorCheckpointAccess.LevelEntities) f.level).skywars2$manager();
                    require(manager.isLoaded(edge.getUUID()) && f.level.getEntity(edge.getUUID()) == null
                                    && lifetimes.loadedTargets().contains(edge),
                            "hidden native body still appears in the index before deferred unload");
                    require(lifetimes.nearestTarget(f.level, cold, 100, true, candidate -> candidate == edge) == null,
                            "sparse selection excludes a registered but natively hidden body");
                    edge.setPos(from.add(0, -100, 0));
                    require(lifetimes.nearestTarget(f.level, from, 100, true, candidate -> candidate == edge) == edge,
                            "same-tick reappearance is eligible without a warmup or cache refresh");
                    sparseQueries = queries.broadQueries;
                }
                edge.discard();
                int[] remoteRoleChecks = {0};
                var beam = lifetimes.nearestBeamTarget(f.level, from, candidate -> {
                    if (candidate == out) remoteRoleChecks[0]++;
                    return candidate == out || candidate == first || candidate == last;
                });
                require(beam == first, "unlimited beam keeps its first published tie winner");

                out.roleChecks = 0;
                var heading = game.items().homing().skillDirection(f.owner, f.level, from, new Vec3(0, 0, 1));
                require(heading.x > 0, "skill 30 preserves last-published equal-distance tie selection");
                require(out.roleChecks == 0 && remoteRoleChecks[0] == 0,
                        "remote published probe requires no role check with a valid local target: homing=" + out.roleChecks +
                                " beam=" + remoteRoleChecks[0]);
                require(lifetimes.nearestBeamTarget(f.level, from, candidate -> candidate == out) == out,
                        "unlimited beam finds the same distant target when no local entity qualifies");
            } finally {
                first.discard();
                last.discard();
                out.discard();
                edge.discard();
            }
        }

        void keeperVolley() {
            var keeper = game.items().sauvojen();
            var wands = game.items().wands();
            require(keeper.count() == 0 && wands.electroSnapshot().isEmpty(), "exclusive real Keeper and electrical producers");
            var originalTarget = target.position();
            var previousItem = f.owner.getMainHandItem();
            var advancement = game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/staffs/sauvojen_tuntija_wand"));
            var earned = new HashSet<String>();
            if (advancement != null) f.owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(earned::add);
            try {
                var stack = f.leaf("sauvojen_wand");
                require(stack.is(LbcWands.SAUVOJEN), "uses actual ordinary Keeper loot");
                f.owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
                keeper.cast(f.owner, InteractionHand.MAIN_HAND, stack, LbcSauvojen.Kind.ELECTRO.ordinal());
                require(keeper.count() == 1, "one real electrical Keeper carrier");
                var drone = keeper.snapshot().getFirst();
                require(f.level.getBlockState(BlockPos.containing(drone.position)).isAir(), "Keeper flies from unobstructed air");
                drone.lifetime = 20;
                keeper.tick();
                var arcs = wands.electroSnapshot();
                require(arcs.size() == 4 && arcs.stream().allMatch(arc -> arc.owner == f.owner && arc.age == 0),
                        "normal Keeper cadence emits four live owned traces");
                var origin = arcs.getFirst().position;
                var initial = Vec3.directionFromRotation(arcs.getFirst().pitch, arcs.getFirst().yaw);
                require(angle(initial, target.getEyePosition().subtract(origin)) < .02, "Keeper originally aims at the living target");
                target.setPos(originalTarget.add(6, 0, 0));
                var goal = target.getEyePosition().subtract(origin);
                var expected = ProjectileSteering.turn(initial, goal, Math.PI / 180);
                try (var queries = new HomingQueries(f.level)) {
                    wands.tick();
                    for (var arc : arcs) {
                        require(wands.hasElectroTrace(arc) && arc.age == 1, "each of the four traces advances through native flight");
                        // Flight applies the post-step random sway to the next tick's heading.
                        var turned = arc.position.subtract(origin);
                        require(angle(turned, expected) < .02 && Math.abs(angle(initial, turned) - 1) < .02
                                        && angle(turned, goal) < angle(initial, goal),
                                "each Keeper trace turns one degree toward the target's current eye position");
                    }
                    require(queries.broadQueries == 0 && sparseQueries == 0,
                            "sparse selectors and real Keeper volley issue zero native 100-block AABB queries, selectors="
                                    + sparseQueries + " volley=" + queries.broadQueries);
                }
            } finally {
                target.setPos(originalTarget);
                wands.stop();
                keeper.stop();
                f.owner.setItemInHand(InteractionHand.MAIN_HAND, previousItem);
                if (advancement != null) {
                    var actual = new ArrayList<String>();
                    f.owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(actual::add);
                    for (var criterion : actual) if (!earned.contains(criterion)) f.owner.getAdvancements().revoke(advancement, criterion);
                }
            }
        }

        void denseSelectors() {
            var lifetimes = game.items().lifetimes();
            var from = f.owner.position().add(0, 60, 0);
            var cows = new ArrayList<ProbeCow>();
            try {
                var first = probe(from.add(-4, 0, 0));
                cows.add(first);
                var last = probe(from.add(4, 0, 0));
                cows.add(last);
                for (int i = 0; i < 63; i++) cows.add(probe(from.add(0, 8 + i, 0)));
                require(lifetimes.loadedTargets().size() > 64 && cows.stream().allMatch(cow -> f.level.getEntity(cow.getUUID()) == cow),
                        "dense scene contains 65 actual published and visible targets");
                try (var queries = new HomingQueries(f.level)) {
                    var winner = lifetimes.nearestTarget(f.level, from, 100, true, candidate -> cows.contains(candidate));
                    require(winner == last && queries.broadQueries == 1,
                            "dense fallback runs one native spatial query and keeps the later exact tie");
                }
            } finally {
                cows.forEach(Entity::discard);
            }
        }

        void launch(){
            var at=target.getEyePosition().add(0,0,-15);
            shot(new Snowball(EntityTypes.SNOWBALL,f.level),at,heading(.4),f.owner);
            shot(new Arrow(EntityTypes.ARROW,f.level),at,heading(15),f.owner);
            shot(new Snowball(EntityTypes.SNOWBALL,f.level),at,heading(15),f.other);
            shot(new Snowball(EntityTypes.SNOWBALL,f.level),at,heading(15),null);
            require(f.owner.position().distanceToSqr(at)<225,"native flights inside former blanket player exclusion");
            cardStart=at.add(0,2,0);cardGoal=target.getEyePosition().subtract(cardStart);card=game.items().danmaku().card(f.owner,cardStart,cardGoal.normalize().add(.001,0,0),false);
            require(card!=null,"actual shared logical card producer");f.owned.add(card.display.getUUID());this.at=game.server().getTickCount();
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();
            if(phase==-1){launch();phase=0;return false;}
            int elapsed=game.server().getTickCount()-at;if(elapsed==0)return false;require(elapsed==1,"one native physics frame observed");
            var fine=shots.get(0);var coarse=shots.get(1);require(!fine.isRemoved()&&!coarse.isRemoved(),"own native flights remain physical");
            require(angle(fine.getDeltaMovement(),target.getEyePosition().subtract(fine.position()))<1e-6,"native near-aligned throwable reaches exact eye direction in one frame");
            require(Math.abs(angle(initial.get(1),coarse.getDeltaMovement())-1)<1e-6,"native arrow turns exactly one degree, independently of speed");
            require(coarse.getDeltaMovement().length()>1.4,"original arrow acceleration remains active");
            for(int i=2;i<4;i++)require(!shots.get(i).isRemoved()&&angle(initial.get(i),shots.get(i).getDeltaMovement())<1e-7,"foreign and unowned projectiles keep original direction");
            require(!card.display.isRemoved()&&card.age==1&&angle(card.direction,cardGoal)<1e-6,"logical card shares exact sub-degree alignment and one real active step");
            require(fine.getOwner()==f.owner&&coarse.getOwner()==f.owner&&shots.get(2).getOwner()==f.other&&shots.get(3).getOwner()==null,"physical ownership remains independent of nearby skill user");
            denseSelectors();
            return true;
        }
        @Override public void close(){if(closed)return;closed=true;try{game.items().danmaku().stop();}finally{f.close();}}
    }
    static void run(GameController game){require(running==null,"exclusive native scene");running=new Scene(game);try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS homing-turn (native one-degree arrow turn with source acceleration, exact final throwable/card alignment near players, foreign/null ownership, bounded antipodal geometry, exact sparse and dense target selection and real Keeper volley query counts)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL homing-turn",error);}}
}
