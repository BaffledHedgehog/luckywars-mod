package me.skywars2.game;

import java.util.*;
import me.skywars2.mixin.NavigationCheckpointAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.Vec3;

/** Native navigation/control values for the original body; goals and Brain have separate lifecycles. */
public final class MatchMobNavigation {
    private final Mob owner;
    private final long worldTime;
    private final MobValues controls;
    private final MoveControlValues move;
    private final LookControlValues look;
    private final JumpControlValues jump;
    private final BodyRotationControlValues body;
    private final PathNavigationValues navigation;
    private final GroundPathNavigationValues ground;
    private final WaterBoundPathNavigationValues water;
    private final WallClimberNavigationValues wallClimber;
    private final NodeEvaluatorValues evaluator;
    private final PathFinderValues finder;
    private final Map<PathType,Float> costs;
    private final Map<PathType,Float> savedCosts = new EnumMap<>(PathType.class);
    private final Route route;

    public MatchMobNavigation(Mob mob) {
        owner = mob;
        worldTime = mob.level().getGameTime();
        // A rider's public move-control getter may return its vehicle's control.
        var access = (NavigationCheckpointAccess.MobAccess)(Object) mob;
        controls = new MobValues(access);
        move = new MoveControlValues((NavigationCheckpointAccess.MoveControlAccess)(Object) controls.moveControl());
        look = new LookControlValues((NavigationCheckpointAccess.LookControlAccess)(Object) controls.lookControl());
        jump = new JumpControlValues((NavigationCheckpointAccess.JumpControlAccess)(Object) controls.jumpControl());
        body = new BodyRotationControlValues((NavigationCheckpointAccess.BodyRotationControlAccess)(Object) access.skywars2$navigationBodyRotationControl());
        PathNavigation nativeNavigation = mob.getNavigation();
        var nativeAccess = (NavigationCheckpointAccess.PathNavigationAccess)(Object) nativeNavigation;
        navigation = new PathNavigationValues(nativeAccess);
        ground = nativeNavigation instanceof GroundPathNavigation g
                ? new GroundPathNavigationValues((NavigationCheckpointAccess.GroundPathNavigationAccess)(Object) g) : null;
        water = nativeNavigation instanceof WaterBoundPathNavigation w
                ? new WaterBoundPathNavigationValues((NavigationCheckpointAccess.WaterBoundPathNavigationAccess)(Object) w) : null;
        wallClimber = nativeNavigation instanceof WallClimberNavigation w
                ? new WallClimberNavigationValues((NavigationCheckpointAccess.WallClimberNavigationAccess)(Object) w) : null;
        evaluator = new NodeEvaluatorValues((NavigationCheckpointAccess.NodeEvaluatorAccess)(Object) navigation.nodeEvaluator());
        finder = new PathFinderValues((NavigationCheckpointAccess.PathFinderAccess)(Object) nativeAccess.skywars2$navigationPathFinder());
        costs = access.skywars2$navigationPathfindingMalus();
        savedCosts.putAll(costs);
        route = nativeNavigation.getPath() == null ? null : new Route(nativeNavigation.getPath());
        // PathFinder/NodeEvaluator search caches are reset for every synchronous search.
    }

    public void requireOwner(Entity target) {
        if (target != owner) throw new IllegalArgumentException("Native navigation belongs to its original mob body");
    }

    public void restore() {
        long delta = owner.level().getGameTime() - worldTime;
        if (route != null) route.restore();
        controls.restore();
        move.restore();
        look.restore();
        jump.restore();
        body.restore();
        navigation.restore(delta);
        if (ground != null) ground.restore();
        if (water != null) water.restore();
        if (wallClimber != null) wallClimber.restore();
        evaluator.restore();
        finder.restore();
        costs.clear();
        costs.putAll(savedCosts);
    }

    private record MobValues(
            NavigationCheckpointAccess.MobAccess owner, PathNavigation navigation, MoveControl moveControl,
            LookControl lookControl, JumpControl jumpControl) {
        MobValues(NavigationCheckpointAccess.MobAccess owner) {
            this(owner, owner.skywars2$navigationNavigation(), owner.skywars2$navigationMoveControl(),
                owner.skywars2$navigationLookControl(), owner.skywars2$navigationJumpControl());
        }

        void restore() {
            owner.skywars2$navigationNavigation(navigation);
            owner.skywars2$navigationMoveControl(moveControl);
            owner.skywars2$navigationLookControl(lookControl);
            owner.skywars2$navigationJumpControl(jumpControl);
        }
    }

    private record MoveControlValues(
            NavigationCheckpointAccess.MoveControlAccess owner, double wantedX, double wantedY, double wantedZ,
            double speedModifier, float strafeForwards, float strafeRight, MoveControl.Operation operation) {
        MoveControlValues(NavigationCheckpointAccess.MoveControlAccess owner) {
            this(owner, owner.skywars2$navigationWantedX(), owner.skywars2$navigationWantedY(),
                owner.skywars2$navigationWantedZ(), owner.skywars2$navigationSpeedModifier(),
                owner.skywars2$navigationStrafeForwards(), owner.skywars2$navigationStrafeRight(),
                owner.skywars2$navigationOperation());
        }

        void restore() {
            owner.skywars2$navigationWantedX(wantedX);
            owner.skywars2$navigationWantedY(wantedY);
            owner.skywars2$navigationWantedZ(wantedZ);
            owner.skywars2$navigationSpeedModifier(speedModifier);
            owner.skywars2$navigationStrafeForwards(strafeForwards);
            owner.skywars2$navigationStrafeRight(strafeRight);
            owner.skywars2$navigationOperation(operation);
        }
    }

    private record LookControlValues(
            NavigationCheckpointAccess.LookControlAccess owner, float yMaxRotSpeed, float xMaxRotAngle,
            int lookAtCooldown, double wantedX, double wantedY, double wantedZ) {
        LookControlValues(NavigationCheckpointAccess.LookControlAccess owner) {
            this(owner, owner.skywars2$navigationYMaxRotSpeed(), owner.skywars2$navigationXMaxRotAngle(),
                owner.skywars2$navigationLookAtCooldown(), owner.skywars2$navigationWantedX(),
                owner.skywars2$navigationWantedY(), owner.skywars2$navigationWantedZ());
        }

        void restore() {
            owner.skywars2$navigationYMaxRotSpeed(yMaxRotSpeed);
            owner.skywars2$navigationXMaxRotAngle(xMaxRotAngle);
            owner.skywars2$navigationLookAtCooldown(lookAtCooldown);
            owner.skywars2$navigationWantedX(wantedX);
            owner.skywars2$navigationWantedY(wantedY);
            owner.skywars2$navigationWantedZ(wantedZ);
        }
    }

    private record JumpControlValues(
            NavigationCheckpointAccess.JumpControlAccess owner, boolean jump) {
        JumpControlValues(NavigationCheckpointAccess.JumpControlAccess owner) {
            this(owner, owner.skywars2$navigationJump());
        }

        void restore() {
            owner.skywars2$navigationJump(jump);
        }
    }

    private record BodyRotationControlValues(
            NavigationCheckpointAccess.BodyRotationControlAccess owner, int headStableTime,
            float lastStableYHeadRot) {
        BodyRotationControlValues(NavigationCheckpointAccess.BodyRotationControlAccess owner) {
            this(owner, owner.skywars2$navigationHeadStableTime(),
                owner.skywars2$navigationLastStableYHeadRot());
        }

        void restore() {
            owner.skywars2$navigationHeadStableTime(headStableTime);
            owner.skywars2$navigationLastStableYHeadRot(lastStableYHeadRot);
        }
    }

    private record PathNavigationValues(
            NavigationCheckpointAccess.PathNavigationAccess owner, Path path, double speedModifier, int tick,
            int lastStuckCheck, Vec3 lastStuckCheckPos, Vec3i timeoutCachedNode, long timeoutTimer,
            double timeoutLimit, float maxDistanceToWaypoint, boolean hasDelayedRecomputation,
            NodeEvaluator nodeEvaluator, BlockPos targetPos, int reachRange, float maxVisitedNodesMultiplier,
            boolean isStuck, float requiredPathLength, long lastTimeoutCheck, long timeLastRecompute) {
        PathNavigationValues(NavigationCheckpointAccess.PathNavigationAccess owner) {
            this(owner, owner.skywars2$navigationPath(), owner.skywars2$navigationSpeedModifier(),
                owner.skywars2$navigationTick(), owner.skywars2$navigationLastStuckCheck(),
                owner.skywars2$navigationLastStuckCheckPos(), owner.skywars2$navigationTimeoutCachedNode(),
                owner.skywars2$navigationTimeoutTimer(), owner.skywars2$navigationTimeoutLimit(),
                owner.skywars2$navigationMaxDistanceToWaypoint(),
                owner.skywars2$navigationHasDelayedRecomputation(), owner.skywars2$navigationNodeEvaluator(),
                owner.skywars2$navigationTargetPos(), owner.skywars2$navigationReachRange(),
                owner.skywars2$navigationMaxVisitedNodesMultiplier(), owner.skywars2$navigationIsStuck(),
                owner.skywars2$navigationRequiredPathLength(), owner.skywars2$navigationLastTimeoutCheck(),
                owner.skywars2$navigationTimeLastRecompute());
        }

        void restore(long delta) {
            owner.skywars2$navigationPath(path);
            owner.skywars2$navigationSpeedModifier(speedModifier);
            owner.skywars2$navigationTick(tick);
            owner.skywars2$navigationLastStuckCheck(lastStuckCheck);
            owner.skywars2$navigationLastStuckCheckPos(lastStuckCheckPos);
            owner.skywars2$navigationTimeoutCachedNode(timeoutCachedNode);
            owner.skywars2$navigationTimeoutTimer(timeoutTimer);
            owner.skywars2$navigationTimeoutLimit(timeoutLimit);
            owner.skywars2$navigationMaxDistanceToWaypoint(maxDistanceToWaypoint);
            owner.skywars2$navigationHasDelayedRecomputation(hasDelayedRecomputation);
            owner.skywars2$navigationNodeEvaluator(nodeEvaluator);
            owner.skywars2$navigationTargetPos(targetPos);
            owner.skywars2$navigationReachRange(reachRange);
            owner.skywars2$navigationMaxVisitedNodesMultiplier(maxVisitedNodesMultiplier);
            owner.skywars2$navigationIsStuck(isStuck);
            owner.skywars2$navigationRequiredPathLength(requiredPathLength);
            owner.skywars2$navigationLastTimeoutCheck(Math.addExact(lastTimeoutCheck, delta));
            owner.skywars2$navigationTimeLastRecompute(Math.addExact(timeLastRecompute, delta));
        }
    }

    private record GroundPathNavigationValues(
            NavigationCheckpointAccess.GroundPathNavigationAccess owner, boolean avoidSun,
            boolean canPathToTargetsBelowSurface) {
        GroundPathNavigationValues(NavigationCheckpointAccess.GroundPathNavigationAccess owner) {
            this(owner, owner.skywars2$navigationAvoidSun(),
                owner.skywars2$navigationCanPathToTargetsBelowSurface());
        }

        void restore() {
            owner.skywars2$navigationAvoidSun(avoidSun);
            owner.skywars2$navigationCanPathToTargetsBelowSurface(canPathToTargetsBelowSurface);
        }
    }

    private record WaterBoundPathNavigationValues(
            NavigationCheckpointAccess.WaterBoundPathNavigationAccess owner, boolean allowBreaching) {
        WaterBoundPathNavigationValues(NavigationCheckpointAccess.WaterBoundPathNavigationAccess owner) {
            this(owner, owner.skywars2$navigationAllowBreaching());
        }

        void restore() {
            owner.skywars2$navigationAllowBreaching(allowBreaching);
        }
    }

    private record WallClimberNavigationValues(
            NavigationCheckpointAccess.WallClimberNavigationAccess owner, BlockPos pathToPosition) {
        WallClimberNavigationValues(NavigationCheckpointAccess.WallClimberNavigationAccess owner) {
            this(owner, owner.skywars2$navigationPathToPosition());
        }

        void restore() {
            owner.skywars2$navigationPathToPosition(pathToPosition);
        }
    }

    private record NodeEvaluatorValues(
            NavigationCheckpointAccess.NodeEvaluatorAccess owner, boolean canPassDoors, boolean canOpenDoors,
            boolean canFloat, boolean canWalkOverFences) {
        NodeEvaluatorValues(NavigationCheckpointAccess.NodeEvaluatorAccess owner) {
            this(owner, owner.skywars2$navigationCanPassDoors(), owner.skywars2$navigationCanOpenDoors(),
                owner.skywars2$navigationCanFloat(), owner.skywars2$navigationCanWalkOverFences());
        }

        void restore() {
            owner.skywars2$navigationCanPassDoors(canPassDoors);
            owner.skywars2$navigationCanOpenDoors(canOpenDoors);
            owner.skywars2$navigationCanFloat(canFloat);
            owner.skywars2$navigationCanWalkOverFences(canWalkOverFences);
        }
    }

    private record PathFinderValues(
            NavigationCheckpointAccess.PathFinderAccess owner, int maxVisitedNodes) {
        PathFinderValues(NavigationCheckpointAccess.PathFinderAccess owner) {
            this(owner, owner.skywars2$navigationMaxVisitedNodes());
        }

        void restore() {
            owner.skywars2$navigationMaxVisitedNodes(maxVisitedNodes);
        }
    }

    private record TargetValues(
            NavigationCheckpointAccess.TargetAccess owner, float bestHeuristic, Node bestNode, boolean reached) {
        TargetValues(NavigationCheckpointAccess.TargetAccess owner) {
            this(owner, owner.skywars2$navigationBestHeuristic(), owner.skywars2$navigationBestNode(),
                owner.skywars2$navigationReached());
        }

        void restore() {
            owner.skywars2$navigationBestHeuristic(bestHeuristic);
            owner.skywars2$navigationBestNode(bestNode);
            owner.skywars2$navigationReached(reached);
        }
    }

    /** Path.copy shares the node list; retain it and the original nodes for goal aliases. */
    static final class Route {
        final Path path;
        final int cursor;
        final NodeList nodes;
        final Path.DebugData debug;
        final NodeList open, closed;
        final Set<Target> debugTargets;
        final Map<Node,NodeImage> graph = new IdentityHashMap<>();
        final List<TargetValues> targets = new ArrayList<>();

        Route(Path path) {
            this.path = path;
            cursor = path.getNextNodeIndex();
            var access = (NavigationCheckpointAccess.PathAccess)(Object) path;
            nodes = new NodeList(access.skywars2$navigationNodes());
            debug = access.skywars2$navigationDebugData();
            open = debug == null ? null : new NodeList(debug.openSet());
            closed = debug == null ? null : new NodeList(debug.closedSet());
            debugTargets = debug == null ? Set.of() : new LinkedHashSet<>(debug.targetNodes());
            var pending = new ArrayDeque<Node>(nodes.saved);
            if (debug != null) {
                pending.addAll(open.saved);
                pending.addAll(closed.saved);
                pending.addAll(debugTargets);
            }
            while (!pending.isEmpty()) {
                Node node = pending.removeFirst();
                if (graph.containsKey(node)) continue;
                graph.put(node, new NodeImage(node));
                if (node.cameFrom != null) pending.addLast(node.cameFrom);
                if (node instanceof Target target) {
                    var image = new TargetValues((NavigationCheckpointAccess.TargetAccess)(Object) target);
                    targets.add(image);
                    if (image.bestNode() != null) pending.addLast(image.bestNode());
                }
            }
        }

        void restore() {
            nodes.restore();
            graph.forEach((node,image) -> image.restore(node));
            targets.forEach(TargetValues::restore);
            if (debug != null) {
                open.restore();
                closed.restore();
                if (!debug.targetNodes().equals(debugTargets)) {
                    debug.targetNodes().clear();
                    debug.targetNodes().addAll(debugTargets);
                }
            }
            ((NavigationCheckpointAccess.PathAccess)(Object) path).skywars2$navigationDebugData(debug);
            path.setNextNodeIndex(cursor);
        }
    }

    private static final class NodeList {
        final List<Node> original,saved;
        NodeList(List<Node> original){this.original=original;saved=List.copyOf(original);}
        void restore() {
            if(original.size()!=saved.size()){original.clear();original.addAll(saved);return;}
            // A fixed-size list supports native replaceNode; don't clear it just to restore values.
            for(int i=0;i<saved.size();i++)if(original.get(i)!=saved.get(i))original.set(i,saved.get(i));
        }
    }
    private record NodeImage(int heap,float g,float h,float f,Node parent,boolean closed,float walked,float malus,PathType type) {
        NodeImage(Node node){this(node.heapIdx,node.g,node.h,node.f,node.cameFrom,node.closed,node.walkedDistance,node.costMalus,node.type);}
        void restore(Node node) {
            node.heapIdx=heap;node.g=g;node.h=h;node.f=f;node.cameFrom=parent;node.closed=closed;
            node.walkedDistance=walked;node.costMalus=malus;node.type=type;
        }
    }
}
