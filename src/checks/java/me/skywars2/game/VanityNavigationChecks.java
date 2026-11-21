package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.NavigationCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/** A real native path is resumed after discarding its future, including the shared node-list alias. */
public final class VanityNavigationChecks {
    private static Fixture running;
    private VanityNavigationChecks() {}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity navigation: "+why);}
    public static void run(GameController game){
        require(running==null,"one native path fixture");
        var f=new Fixture(game);running=f;
        try{f.begin();}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game){
        if(running==null)return;var f=running;
        try{
            require(f.game==game,"same match owner");if(!f.step())return;
            running=null;f.close();
            SkyWars2.LOGGER.info("[check] PASS vanity-navigation (actual native path/movement; retained path and node-list aliases; cursor/node/control restoration; repeatable image and resumed native ticks)");
        }catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL vanity-navigation",error);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;
        Cow cow;Path path,alias;MatchEntityState image,idleImage;List<Node> nodes;
        Vec3 start,saved,restored;int phase,when,index;float malus;double wantedX,lookZ;
        long worldTime,lastTimeout,recompute;boolean closed;
        NavigationCheckpointAccess.LookControlAccess look(){return (NavigationCheckpointAccess.LookControlAccess)cow.getLookControl();}
        NavigationCheckpointAccess.JumpControlAccess jump(){return (NavigationCheckpointAccess.JumpControlAccess)cow.getJumpControl();}
        NavigationCheckpointAccess.PathNavigationAccess navigation(){return (NavigationCheckpointAccess.PathNavigationAccess)cow.getNavigation();}
        Fixture(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);}
        void begin(){
            f.prepareParticipants();var floor=BlockPos.containing(f.home).above(3);
            for(int x=0;x<26;x++)for(int z=-2;z<=2;z++){
                var p=floor.offset(x,0,z);f.set(p,Blocks.STONE.defaultBlockState());
                for(int y=1;y<=3;y++)f.set(p.above(y),Blocks.AIR.defaultBlockState());
            }
            start=Vec3.atCenterOf(floor).add(0,.5,0);
            cow=new Cow(EntityTypes.COW,f.level);cow.setPos(start);cow.setOnGround(true);
            cow.removeAllGoals(goal->true);cow.setNoAi(false);cow.setNoGravity(false);
            require(f.level.addFreshEntity(cow),"ordinary native mob added");f.own(cow);
            cow.getNavigation().setRequiredPathLength(32);
            idleImage=new MatchEntityState(game,cow);
            path=cow.getNavigation().createPath(floor.offset(22,1,0),0);
            require(path!=null&&path.canReach()&&path.getNodeCount()>=18,"real pathfinder found the supported route: "+
                    (path==null?"null":path.getNodeCount()+" nodes, reached="+path.canReach()+", end="+path.getEndNode()));
            require(cow.getNavigation().moveTo(path,1),"native navigation accepted route");
            when=game.server().getTickCount();
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();int elapsed=game.server().getTickCount()-when;
            require(elapsed<120,"bounded path scene phase="+phase);
            if(phase==0){
                if(elapsed<8)return false;
                require(cow.position().distanceToSqr(start)>.01,"native AI actually walked before capture");
                path=cow.getNavigation().getPath();require(path!=null&&!path.isDone(),"capture a live path");
                cow.getLookControl().setLookAt(cow.getX()+7,cow.getEyeY(),cow.getZ()+4,17,11);
                cow.getMoveControl().setWantedPosition(cow.getX()+2,cow.getY(),cow.getZ(),.65);
                cow.getJumpControl().jump();cow.getNavigation().setCanFloat(true);
                cow.setPathfindingMalus(PathType.WATER,3.25F);
                lookZ=look().skywars2$navigationWantedZ();
                worldTime=f.level.getGameTime();
                lastTimeout=navigation().skywars2$navigationLastTimeoutCheck();
                recompute=navigation().skywars2$navigationTimeLastRecompute();
                wantedX=cow.getMoveControl().getWantedX();index=path.getNextNodeIndex();saved=cow.position();
                nodes=new ArrayList<>();for(int i=0;i<path.getNodeCount();i++)nodes.add(path.getNode(i));
                malus=nodes.getFirst().costMalus;alias=path.copy();image=new MatchEntityState(game,cow);
                // Native Path.copy shares the node list. A shallow saved Path is not a rewind.
                path.truncateNodes(path.getNodeCount()-2);nodes.getFirst().costMalus+=99;
                cow.getNavigation().stop();cow.getMoveControl().setWantedPosition(cow.getX()-6,cow.getY(),cow.getZ(),.1);
                cow.getLookControl().setLookAt(cow.getX()-8,cow.getEyeY()+2,cow.getZ()-7,3,4);
                cow.getNavigation().setCanFloat(false);cow.setPathfindingMalus(PathType.WATER,19);
                restore();verify();restored=cow.position();phase=1;when=game.server().getTickCount();return false;
            }
            if(phase==1){
                if(elapsed<20)return false;
                require(cow.position().distanceToSqr(restored)>.2,"restored native path continues moving through real world ticks");
                require(cow.getNavigation().getPath()==path&&path.getNextNodeIndex()>index,"native cursor continues from saved route");
                cow.getNavigation().stop();restore();verify();phase=2;when=game.server().getTickCount();return false;
            }
            if(phase==2){
                if(elapsed<10)return false;
                require(cow.position().distanceToSqr(saved)>.01,"same image can resume the native path a second time");
                idleImage.load(cow);require(cow.getNavigation().getPath()==null,"an idle snapshot removes a future path");
                require(!cow.getMoveControl().hasWanted(),"idle snapshot restores WAIT control");
                restored=cow.position();phase=3;when=game.server().getTickCount();return false;
            }
            if(elapsed<8)return false;
            require(cow.position().distanceToSqr(restored)<.01,"idle restored mob does not follow its future route");
            return true;
        }
        void restore(){
            boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
            try{image.load(cow);}finally{game.server().tickRateManager().setFrozen(frozen);}
        }
        void verify(){
            require(cow.getNavigation().getPath()==path,"restore the saved native Path identity");
            require(path.getNextNodeIndex()==index&&path.getNodeCount()==nodes.size(),"restore path cursor and truncated nodes");
            require(alias.getNodeCount()==nodes.size(),"native copied Path still shares the restored node list");
            for(int i=0;i<nodes.size();i++)require(path.getNode(i)==nodes.get(i),"retain original node identity "+i);
            require(nodes.getFirst().costMalus==malus,"future mutation cannot alter the saved node values");
            require(cow.getMoveControl().hasWanted()&&cow.getMoveControl().getWantedX()==wantedX,"restore pending movement control");
            require(look().skywars2$navigationWantedZ()==lookZ&&
                    look().skywars2$navigationYMaxRotSpeed()==17F,"restore pending look target and turn rate");
            require(jump().skywars2$navigationJump(),"restore queued native jump");
            require(cow.getNavigation().canFloat()&&cow.getPathfindingMalus(PathType.WATER)==3.25F,"restore evaluator flags and native costs");
            long delta=f.level.getGameTime()-worldTime;
            require(navigation().skywars2$navigationLastTimeoutCheck()==lastTimeout+delta&&
                    navigation().skywars2$navigationTimeLastRecompute()==recompute+delta,
                    "navigation timestamps keep their relative ages on the current world clock");
            require(cow.position().equals(saved),"native value image restores exact feet");
        }
        @Override public void close(){if(closed)return;closed=true;f.close();}
    }
}
