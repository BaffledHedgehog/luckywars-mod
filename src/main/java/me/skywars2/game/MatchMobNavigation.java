package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.level.pathfinder.*;

/** Native navigation/control values for the original body; goals and Brain have separate lifecycles. */
final class MatchMobNavigation {
    private final Mob owner;
    private final long worldTime;
    private final List<Value> values=new ArrayList<>();
    private final Map<PathType,Float> costs;
    private final Map<PathType,Float> savedCosts=new EnumMap<>(PathType.class);
    private final Route route;

    @SuppressWarnings("unchecked")
    MatchMobNavigation(Mob mob) {
        owner=mob;worldTime=mob.level().getGameTime();
        // Get the body's controls directly: Mob.getMoveControl may return its vehicle's control.
        capture(mob,Mob.class,"navigation","moveControl","lookControl","jumpControl");
        Object move=read(mob,Mob.class,"moveControl"),look=read(mob,Mob.class,"lookControl");
        Object jump=read(mob,Mob.class,"jumpControl"),body=read(mob,Mob.class,"bodyRotationControl");
        capture(move,MoveControl.class,"wantedX","wantedY","wantedZ","speedModifier","strafeForwards","strafeRight","operation");
        capture(look,LookControl.class,"yMaxRotSpeed","xMaxRotAngle","lookAtCooldown","wantedX","wantedY","wantedZ");
        capture(jump,JumpControl.class,"jump");
        capture(body,BodyRotationControl.class,"headStableTime","lastStableYHeadRot");
        PathNavigation navigation=mob.getNavigation();
        capture(navigation,PathNavigation.class,"path","speedModifier","tick","lastStuckCheck","lastStuckCheckPos",
                "timeoutCachedNode","timeoutTimer","timeoutLimit","maxDistanceToWaypoint","hasDelayedRecomputation",
                "nodeEvaluator","targetPos","reachRange","maxVisitedNodesMultiplier","isStuck","requiredPathLength");
        captureStamp(navigation,PathNavigation.class,"lastTimeoutCheck","timeLastRecompute");
        if(navigation instanceof GroundPathNavigation)capture(navigation,GroundPathNavigation.class,"avoidSun","canPathToTargetsBelowSurface");
        if(navigation instanceof WaterBoundPathNavigation)capture(navigation,WaterBoundPathNavigation.class,"allowBreaching");
        if(navigation instanceof WallClimberNavigation)capture(navigation,WallClimberNavigation.class,"pathToPosition");
        capture(read(navigation,PathNavigation.class,"nodeEvaluator"),NodeEvaluator.class,
                "canPassDoors","canOpenDoors","canFloat","canWalkOverFences");
        capture(read(navigation,PathNavigation.class,"pathFinder"),PathFinder.class,"maxVisitedNodes");
        costs=(Map<PathType,Float>)read(mob,Mob.class,"pathfindingMalus");savedCosts.putAll(costs);
        route=navigation.getPath()==null?null:new Route(navigation.getPath());
        // PathFinder/NodeEvaluator search caches are reset for every synchronous search, not pending AI work.
    }
    void requireOwner(Entity target) {
        if(target!=owner)throw new IllegalArgumentException("Native navigation belongs to its original mob body");
    }
    void restore() {
        long delta=owner.level().getGameTime()-worldTime;
        if(route!=null)route.restore();
        for(var value:values)value.restore(delta);
        costs.clear();costs.putAll(savedCosts);
    }
    private void capture(Object source,Class<?> type,String... names) {
        for(String name:names){Field field=field(type,name);values.add(new Value(source,field,get(field,source),false));}
    }
    private void captureStamp(Object source,Class<?> type,String... names) {
        for(String name:names){Field field=field(type,name);values.add(new Value(source,field,get(field,source),true));}
    }
    private record Value(Object target,Field field,Object saved,boolean stamp) {
        void restore(long delta){set(field,target,stamp?Math.addExact((Long)saved,delta):saved);}
    }
    private static Field field(Class<?> type,String name) {
        try{var field=type.getDeclaredField(name);field.setAccessible(true);return field;}
        catch(ReflectiveOperationException error){throw new IllegalStateException("Review native navigation checkpoint "+type.getName()+"."+name,error);}
    }
    private static Object read(Object target,Class<?> type,String name){return get(field(type,name),target);}
    private static Object get(Field field,Object target) {
        try{return field.get(target);}catch(IllegalAccessException error){throw new IllegalStateException(error);}
    }
    private static void set(Field field,Object target,Object value) {
        try{field.set(target,value);}catch(IllegalAccessException error){throw new IllegalStateException(error);}
    }

    /** Path.copy shares its node list. Restore that list and the original nodes in place for goal aliases. */
    private static final class Route {
        final Path path;final int cursor;final NodeList nodes;
        final Path.DebugData debug;final NodeList open,closed;
        final Set<Target> debugTargets;
        final Map<Node,NodeImage> graph=new IdentityHashMap<>();
        final List<Value> targets=new ArrayList<>();
        @SuppressWarnings("unchecked") Route(Path path) {
            this.path=path;cursor=path.getNextNodeIndex();nodes=new NodeList((List<Node>)read(path,Path.class,"nodes"));
            debug=(Path.DebugData)read(path,Path.class,"debugData");
            open=debug==null?null:new NodeList(debug.openSet());closed=debug==null?null:new NodeList(debug.closedSet());
            debugTargets=debug==null?Set.of():new LinkedHashSet<>(debug.targetNodes());
            var pending=new ArrayDeque<Node>(nodes.saved);
            if(debug!=null){pending.addAll(open.saved);pending.addAll(closed.saved);pending.addAll(debugTargets);}
            while(!pending.isEmpty()) {
                Node node=pending.removeFirst();if(graph.containsKey(node))continue;
                graph.put(node,new NodeImage(node));if(node.cameFrom!=null)pending.addLast(node.cameFrom);
                if(node instanceof Target target)for(String name:List.of("bestHeuristic","bestNode","reached")) {
                    Field field=field(Target.class,name);Object saved=get(field,target);targets.add(new Value(target,field,saved,false));
                    if(saved instanceof Node best)pending.addLast(best);
                }
            }
        }
        void restore() {
            nodes.restore();graph.forEach((node,image)->image.restore(node));targets.forEach(value->value.restore(0));
            if(debug!=null) {
                open.restore();closed.restore();
                if(!debug.targetNodes().equals(debugTargets)){debug.targetNodes().clear();debug.targetNodes().addAll(debugTargets);}
            }
            set(field(Path.class,"debugData"),path,debug);path.setNextNodeIndex(cursor);
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
