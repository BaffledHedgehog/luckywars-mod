package me.skywars2.mixin;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Native pathfinder and control state owned by one exact mob body. */
public final class NavigationCheckpointAccess {
    private NavigationCheckpointAccess() { }

    @Mixin(Mob.class)
    public interface MobAccess {
        @Accessor("navigation") PathNavigation skywars2$navigationNavigation();
        @Accessor("navigation") void skywars2$navigationNavigation(PathNavigation value);
        @Accessor("moveControl") MoveControl skywars2$navigationMoveControl();
        @Accessor("moveControl") void skywars2$navigationMoveControl(MoveControl value);
        @Accessor("lookControl") LookControl skywars2$navigationLookControl();
        @Accessor("lookControl") void skywars2$navigationLookControl(LookControl value);
        @Accessor("jumpControl") JumpControl skywars2$navigationJumpControl();
        @Accessor("jumpControl") void skywars2$navigationJumpControl(JumpControl value);
        @Accessor("bodyRotationControl") BodyRotationControl skywars2$navigationBodyRotationControl();
        @Accessor("pathfindingMalus") Map<PathType, Float> skywars2$navigationPathfindingMalus();
    }

    @Mixin(MoveControl.class)
    public interface MoveControlAccess {
        @Accessor("wantedX") double skywars2$navigationWantedX();
        @Accessor("wantedX") void skywars2$navigationWantedX(double value);
        @Accessor("wantedY") double skywars2$navigationWantedY();
        @Accessor("wantedY") void skywars2$navigationWantedY(double value);
        @Accessor("wantedZ") double skywars2$navigationWantedZ();
        @Accessor("wantedZ") void skywars2$navigationWantedZ(double value);
        @Accessor("speedModifier") double skywars2$navigationSpeedModifier();
        @Accessor("speedModifier") void skywars2$navigationSpeedModifier(double value);
        @Accessor("strafeForwards") float skywars2$navigationStrafeForwards();
        @Accessor("strafeForwards") void skywars2$navigationStrafeForwards(float value);
        @Accessor("strafeRight") float skywars2$navigationStrafeRight();
        @Accessor("strafeRight") void skywars2$navigationStrafeRight(float value);
        @Accessor("operation") MoveControl.Operation skywars2$navigationOperation();
        @Accessor("operation") void skywars2$navigationOperation(MoveControl.Operation value);
    }

    @Mixin(LookControl.class)
    public interface LookControlAccess {
        @Accessor("yMaxRotSpeed") float skywars2$navigationYMaxRotSpeed();
        @Accessor("yMaxRotSpeed") void skywars2$navigationYMaxRotSpeed(float value);
        @Accessor("xMaxRotAngle") float skywars2$navigationXMaxRotAngle();
        @Accessor("xMaxRotAngle") void skywars2$navigationXMaxRotAngle(float value);
        @Accessor("lookAtCooldown") int skywars2$navigationLookAtCooldown();
        @Accessor("lookAtCooldown") void skywars2$navigationLookAtCooldown(int value);
        @Accessor("wantedX") double skywars2$navigationWantedX();
        @Accessor("wantedX") void skywars2$navigationWantedX(double value);
        @Accessor("wantedY") double skywars2$navigationWantedY();
        @Accessor("wantedY") void skywars2$navigationWantedY(double value);
        @Accessor("wantedZ") double skywars2$navigationWantedZ();
        @Accessor("wantedZ") void skywars2$navigationWantedZ(double value);
    }

    @Mixin(JumpControl.class)
    public interface JumpControlAccess {
        @Accessor("jump") boolean skywars2$navigationJump();
        @Accessor("jump") void skywars2$navigationJump(boolean value);
    }

    @Mixin(BodyRotationControl.class)
    public interface BodyRotationControlAccess {
        @Accessor("headStableTime") int skywars2$navigationHeadStableTime();
        @Accessor("headStableTime") void skywars2$navigationHeadStableTime(int value);
        @Accessor("lastStableYHeadRot") float skywars2$navigationLastStableYHeadRot();
        @Accessor("lastStableYHeadRot") void skywars2$navigationLastStableYHeadRot(float value);
    }

    @Mixin(PathNavigation.class)
    public interface PathNavigationAccess {
        @Accessor("path") Path skywars2$navigationPath();
        @Accessor("path") void skywars2$navigationPath(Path value);
        @Accessor("speedModifier") double skywars2$navigationSpeedModifier();
        @Accessor("speedModifier") void skywars2$navigationSpeedModifier(double value);
        @Accessor("tick") int skywars2$navigationTick();
        @Accessor("tick") void skywars2$navigationTick(int value);
        @Accessor("lastStuckCheck") int skywars2$navigationLastStuckCheck();
        @Accessor("lastStuckCheck") void skywars2$navigationLastStuckCheck(int value);
        @Accessor("lastStuckCheckPos") Vec3 skywars2$navigationLastStuckCheckPos();
        @Accessor("lastStuckCheckPos") void skywars2$navigationLastStuckCheckPos(Vec3 value);
        @Accessor("timeoutCachedNode") Vec3i skywars2$navigationTimeoutCachedNode();
        @Accessor("timeoutCachedNode") void skywars2$navigationTimeoutCachedNode(Vec3i value);
        @Accessor("timeoutTimer") long skywars2$navigationTimeoutTimer();
        @Accessor("timeoutTimer") void skywars2$navigationTimeoutTimer(long value);
        @Accessor("timeoutLimit") double skywars2$navigationTimeoutLimit();
        @Accessor("timeoutLimit") void skywars2$navigationTimeoutLimit(double value);
        @Accessor("maxDistanceToWaypoint") float skywars2$navigationMaxDistanceToWaypoint();
        @Accessor("maxDistanceToWaypoint") void skywars2$navigationMaxDistanceToWaypoint(float value);
        @Accessor("hasDelayedRecomputation") boolean skywars2$navigationHasDelayedRecomputation();
        @Accessor("hasDelayedRecomputation") void skywars2$navigationHasDelayedRecomputation(boolean value);
        @Accessor("nodeEvaluator") NodeEvaluator skywars2$navigationNodeEvaluator();
        @Accessor("nodeEvaluator") void skywars2$navigationNodeEvaluator(NodeEvaluator value);
        @Accessor("targetPos") BlockPos skywars2$navigationTargetPos();
        @Accessor("targetPos") void skywars2$navigationTargetPos(BlockPos value);
        @Accessor("reachRange") int skywars2$navigationReachRange();
        @Accessor("reachRange") void skywars2$navigationReachRange(int value);
        @Accessor("maxVisitedNodesMultiplier") float skywars2$navigationMaxVisitedNodesMultiplier();
        @Accessor("maxVisitedNodesMultiplier") void skywars2$navigationMaxVisitedNodesMultiplier(float value);
        @Accessor("isStuck") boolean skywars2$navigationIsStuck();
        @Accessor("isStuck") void skywars2$navigationIsStuck(boolean value);
        @Accessor("requiredPathLength") float skywars2$navigationRequiredPathLength();
        @Accessor("requiredPathLength") void skywars2$navigationRequiredPathLength(float value);
        @Accessor("lastTimeoutCheck") long skywars2$navigationLastTimeoutCheck();
        @Accessor("lastTimeoutCheck") void skywars2$navigationLastTimeoutCheck(long value);
        @Accessor("timeLastRecompute") long skywars2$navigationTimeLastRecompute();
        @Accessor("timeLastRecompute") void skywars2$navigationTimeLastRecompute(long value);
        @Accessor("pathFinder") PathFinder skywars2$navigationPathFinder();
    }

    @Mixin(GroundPathNavigation.class)
    public interface GroundPathNavigationAccess {
        @Accessor("avoidSun") boolean skywars2$navigationAvoidSun();
        @Accessor("avoidSun") void skywars2$navigationAvoidSun(boolean value);
        @Accessor("canPathToTargetsBelowSurface") boolean skywars2$navigationCanPathToTargetsBelowSurface();
        @Accessor("canPathToTargetsBelowSurface") void skywars2$navigationCanPathToTargetsBelowSurface(boolean value);
    }

    @Mixin(WaterBoundPathNavigation.class)
    public interface WaterBoundPathNavigationAccess {
        @Accessor("allowBreaching") boolean skywars2$navigationAllowBreaching();
        @Accessor("allowBreaching") void skywars2$navigationAllowBreaching(boolean value);
    }

    @Mixin(WallClimberNavigation.class)
    public interface WallClimberNavigationAccess {
        @Accessor("pathToPosition") BlockPos skywars2$navigationPathToPosition();
        @Accessor("pathToPosition") void skywars2$navigationPathToPosition(BlockPos value);
    }

    @Mixin(NodeEvaluator.class)
    public interface NodeEvaluatorAccess {
        @Accessor("canPassDoors") boolean skywars2$navigationCanPassDoors();
        @Accessor("canPassDoors") void skywars2$navigationCanPassDoors(boolean value);
        @Accessor("canOpenDoors") boolean skywars2$navigationCanOpenDoors();
        @Accessor("canOpenDoors") void skywars2$navigationCanOpenDoors(boolean value);
        @Accessor("canFloat") boolean skywars2$navigationCanFloat();
        @Accessor("canFloat") void skywars2$navigationCanFloat(boolean value);
        @Accessor("canWalkOverFences") boolean skywars2$navigationCanWalkOverFences();
        @Accessor("canWalkOverFences") void skywars2$navigationCanWalkOverFences(boolean value);
    }

    @Mixin(PathFinder.class)
    public interface PathFinderAccess {
        @Accessor("maxVisitedNodes") int skywars2$navigationMaxVisitedNodes();
        @Accessor("maxVisitedNodes") void skywars2$navigationMaxVisitedNodes(int value);
    }

    @Mixin(Path.class)
    public interface PathAccess {
        @Accessor("debugData") Path.DebugData skywars2$navigationDebugData();
        @Accessor("debugData") void skywars2$navigationDebugData(Path.DebugData value);
        @Accessor("nodes") List<Node> skywars2$navigationNodes();
    }

    @Mixin(Target.class)
    public interface TargetAccess {
        @Accessor("bestHeuristic") float skywars2$navigationBestHeuristic();
        @Accessor("bestHeuristic") void skywars2$navigationBestHeuristic(float value);
        @Accessor("bestNode") Node skywars2$navigationBestNode();
        @Accessor("bestNode") void skywars2$navigationBestNode(Node value);
        @Accessor("reached") boolean skywars2$navigationReached();
        @Accessor("reached") void skywars2$navigationReached(boolean value);
    }
}
