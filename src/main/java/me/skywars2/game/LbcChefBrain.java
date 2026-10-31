package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import java.util.*;

/** Resident-chunk, shape-aware action graph. Only native travel moves the mannequin. */
final class LbcChefBrain {
    private static final int SLICE=192,GLOBAL_NODES=1536,EDIT_LIMIT=1536,MAX_OPEN=8192;
    private static final int GLOBAL_PROBES=36000,SLICE_PROBES=6000;
    private static long budgetTick=Long.MIN_VALUE;
    private static long deadline;
    private static int budget=GLOBAL_NODES,probes;
    static void beginTick(long tick){if(tick!=budgetTick){budgetTick=tick;budget=GLOBAL_NODES;probes=0;
        deadline=System.nanoTime()+12_000_000L;}}
    private final LbcWtf.Chef chef;
    private final ServerLevel level;
    private List<ItemStack> tools=List.of(new ItemStack(Items.NETHERITE_PICKAXE),new ItemStack(Items.NETHERITE_AXE),
            new ItemStack(Items.NETHERITE_SHOVEL),new ItemStack(Items.NETHERITE_HOE),new ItemStack(Items.SHEARS),LbcWtf.sword());
    private final Set<BlockPos> placed=new HashSet<>();
    private final ArrayDeque<Step> path=new ArrayDeque<>();
    private final Map<Step,Integer> failedSteps=new HashMap<>();
    private Search search;
    private Step progressStep;
    private double bestProgress;
    private int progressAt;
    private BlockPos goal,mining;
    private Vec3 lastGround;
    private BlockState miningState;
    private LbcIndirectKills.Owner miningCause;
    private float progress;
    private int replan,stuck,nextVoidAttempt=Integer.MIN_VALUE,nextBuild,nextJump;
    private Vec3 previous=Vec3.ZERO;
    private boolean airborneJump,pillarLaunched,escapingBody;
    private Step preparingJump,pillarStep;
    private Vec3 runway,flightAim;
    private boolean placementWaiting;
    private int runwayPhase,pillarSettle,resets,searches,runwayStarted,runwayRetreats;
    private String lastReset="none",lastPlacement="none";

    // Sixteenth-block floor precision preserves slabs, stairs, carpets and snow layers.
    private record Spot(int x,int z,int height){
        double y(){return height/16d;}
        Vec3 point(){return new Vec3(x+.5,y(),z+.5);}
        BlockPos cell(){return BlockPos.containing(x+.5,y()+.02,z+.5);}
    }
    private enum Action{WALK,DROP,JUMP,BRIDGE,MINE,MINE_DOWN,JUMP_BUILD,PILLAR,RAMP}
    private record Step(Spot from,Spot to,Action action){}
    private record Node(Spot spot,double cost,double estimate,Node parent,Action action){}
    private record FloorKey(int x,int z,int low,int high){}
    private static final class Search{
        final Spot start;
        final BlockPos destination;
        final PriorityQueue<Node> open=new PriorityQueue<>(Comparator.comparingDouble(Node::estimate));
        final Map<Spot,Double> costs=new HashMap<>();
        final Map<FloorKey,List<Spot>> floors=new HashMap<>();
        final Map<Spot,Boolean> clearance=new HashMap<>();
        final Map<Step,Boolean> links=new HashMap<>();
        final Node root;
        Node best;
        int expanded;
        Search(Spot start,BlockPos destination){
            this.start=start;this.destination=destination;
            root=new Node(start,0,heuristic(start,destination),null,Action.WALK);best=root;
            open.add(root);costs.put(start,0d);
        }
    }
    LbcChefBrain(LbcWtf.Chef chef){this.chef=chef;level=(ServerLevel)chef.level();}
    List<ItemStack> tools(){return tools.stream().map(ItemStack::copy).toList();}
    int searchCount(){return searches;}
    int resetCount(){return resets;}
    String diagnostics(){return "path="+path.size()+" next="+path.peek()+" search="+(search==null?"none":
            "actions expanded="+search.expanded+" open="+search.open.size()+" best="+search.best.spot)
            +" goal="+goal+" stuck="+stuck+" replan="+replan+" takeoff="+runwayPhase+" pillar="+pillarLaunched
            +" mining="+mining+" dig="+progress+" pose="+chef.getPose()+" eye="+chef.getEyePosition()+" yaw="+chef.getYRot()+" pitch="+chef.getXRot()+" head="+chef.getYHeadRot()+" action="+chef.actionSerial+" probes="+probes+" searches="+searches+" ground="+chef.onGround()+" velocity="+chef.getDeltaMovement()+" resets="+resets+" lastReset="+lastReset+" placement="+lastPlacement;}
    Set<BlockPos> checkpointPlaced(){return Set.copyOf(placed);}
    /** Search/mining/run-up caches are disposable; no block, packet, pose or equipment action runs here. */
    void resetForCheckpoint(){
        path.clear();search=null;progressStep=null;failedSteps.clear();goal=null;mining=null;miningState=null;miningCause=null;progress=0;
        bestProgress=0;progressAt=0;replan=0;stuck=0;nextVoidAttempt=Integer.MIN_VALUE;nextBuild=0;nextJump=0;
        previous=chef.position();lastGround=null;escapingBody=false;placementWaiting=false;clearTakeoff();
        resets=0;searches=0;lastReset="checkpoint";lastPlacement="none";
    }
    void restoreCheckpointResources(List<ItemStack> palette,Set<BlockPos> owned){
        tools=palette.stream().map(ItemStack::copy).toList();placed.clear();placed.addAll(owned);resetForCheckpoint();
    }
    void reset(){reset("external");}
    private void reset(String why){lastReset=why+" step="+path.peek()+" at="+chef.position();resets++;
        path.clear();search=null;progressStep=null;clearMining();replan=0;stuck=0;clearTakeoff();}
    private void clearTakeoff(){airborneJump=false;preparingJump=null;runway=null;flightAim=null;runwayPhase=0;runwayRetreats=0;
        pillarStep=null;pillarLaunched=false;pillarSettle=0;}
    private boolean loaded(BlockPos p){return level.isInWorldBounds(p)&&level.hasChunkAt(p)&&level.getWorldBorder().isWithinBounds(p);}
    private boolean hazard(BlockPos p){
        if(!loaded(p))return true;
        BlockState s=level.getBlockState(p);
        return level.getFluidState(p).is(FluidTags.LAVA)||s.is(Blocks.FIRE)||s.is(Blocks.SOUL_FIRE)
                ||s.is(Blocks.CACTUS)||s.is(Blocks.SWEET_BERRY_BUSH)||s.is(Blocks.WITHER_ROSE)
                ||s.is(Blocks.MAGMA_BLOCK)||s.is(Blocks.POWDER_SNOW)
                ||((s.is(Blocks.CAMPFIRE)||s.is(Blocks.SOUL_CAMPFIRE))
                &&s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT));
    }
    private boolean support(BlockPos p){return loaded(p)&&!hazard(p)&&!level.getBlockState(p).getCollisionShape(level,p,CollisionContext.of(chef)).isEmpty();}
    private boolean breakable(BlockPos p){return loaded(p)&&!hazard(p)&&level.getBlockState(p).getDestroySpeed(level,p)>=0
            &&!level.getBlockState(p).is(Blocks.BEDROCK);}
    private AABB body(Vec3 p){return new AABB(p.x-.299,p.y+.012,p.z-.299,p.x+.299,p.y+1.79,p.z+.299);}
    private boolean clear(Vec3 p){
        probes++;
        AABB b=body(p);
        for(BlockPos cell:BlockPos.betweenClosed(BlockPos.containing(b.minX,b.minY,b.minZ),BlockPos.containing(b.maxX,b.maxY,b.maxZ)))
            if(hazard(cell))return false;
        return level.noCollision(chef,b);
    }
    private boolean safe(Spot p){return clear(p.point())&&floorSupported(p.point());}
    private boolean floorSupported(Vec3 p){
        // A tiny downward native collision probe checks real shape, including partial blocks.
        probes++;
        AABB feet=new AABB(p.x-.299,p.y-.075,p.z-.299,p.x+.299,p.y+.005,p.z+.299);
        if(!loaded(BlockPos.containing(p.x,p.y-.05,p.z)))return false;
        return !level.noCollision(chef,feet)&&!hazard(BlockPos.containing(p.x,p.y-.05,p.z));
    }
    private Spot actualStart(){
        int x=chef.blockPosition().getX(),z=chef.blockPosition().getZ();
        List<Spot> candidates=new ArrayList<>(floors(null,x,z,chef.getY()-2.5,chef.getY()+.6));
        // The centre can already be over air while a corner of the actual hitbox is supported.
        // Recover a nearby launch cell rather than permanently halting an on-ground actor.
        if(candidates.isEmpty())for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            if(dx==0&&dz==0)continue;
            candidates.addAll(floors(null,x+dx,z+dz,chef.getY()-.65,chef.getY()+.6));
        }
        return candidates.stream().min(Comparator.comparingDouble(p->Math.abs(p.y()-chef.getY())*4
                +p.point().subtract(chef.position()).horizontalDistanceSqr())).orElse(null);
    }
    private List<Spot> floors(Search work,int x,int z,double low,double high){
        FloorKey key=new FloorKey(x,z,(int)Math.floor(low*16),(int)Math.ceil(high*16));
        if(work!=null&&work.floors.containsKey(key))return work.floors.get(key);
        TreeSet<Integer> heights=new TreeSet<>();
        for(int y=(int)Math.floor(low)-2;y<=(int)Math.floor(high);y++){
            BlockPos b=new BlockPos(x,y,z);
            if(!loaded(b)||hazard(b))continue;
            for(AABB shape:level.getBlockState(b).getCollisionShape(level,b,CollisionContext.of(chef)).toAabbs()){
                // Require support beneath the centre, rather than calling a fence in a corner a floor.
                if(shape.minX>.51||shape.maxX<.49||shape.minZ>.51||shape.maxZ<.49)continue;
                double top=y+shape.maxY;
                if(top>=low-.001&&top<=high+.001)heights.add((int)Math.round(top*16));
            }
        }
        List<Spot> result=new ArrayList<>(heights.size());
        for(int height:heights){Spot p=new Spot(x,z,height);
            if(work==null?clear(p.point()):work.clearance.computeIfAbsent(p,key2->clear(key2.point())))result.add(p);}
        if(work!=null)work.floors.put(key,result);
        return result;
    }
    void findSpawnSurface(){
        Vec3 found=nearestSurface(chef.position(),12);
        if(found!=null){chef.rescue(found);lastGround=found;}
    }
    void returnFromVoid(){
        if(chef.tickCount<nextVoidAttempt)return;
        nextVoidAttempt=chef.tickCount+20;
        Vec3 found=lastGround!=null&&clear(lastGround)&&floorSupported(lastGround)?lastGround:null;
        if(found==null)found=nearestSurface(chef.target.position(),24);
        if(found==null)found=nearestSurface(chef.position(),24);
        if(found!=null){chef.rescue(found);reset("void rescue");}
    }
    private Vec3 nearestSurface(Vec3 center,int radius){
        Vec3 best=null;double distance=Double.POSITIVE_INFINITY;
        // Prefer nearby resident columns. Scan below canopy/roof rather than only four top cells.
        for(int r=0;r<=radius;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
            int x=(int)Math.floor(center.x)+dx,z=(int)Math.floor(center.z)+dz;
            if(!level.hasChunkAt(new BlockPos(x,0,z)))continue;
            int top=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            double low=Math.max(level.getMinY(),Math.min(center.y-8,top-24));
            for(Spot s:floors(null,x,z,low,top+1)){
                double d=s.point().distanceToSqr(center);
                if(d<distance){distance=d;best=s.point();}
            }
            if(best!=null&&r>=3&&r*r>distance)return best;
        }
        return best;
    }
    /** Clearance of our own body precedes a graph whose nodes require free headroom. */
    boolean freeBody(){
        AABB standing=body(chef.position());
        BlockPos trapped=null;
        double nearest=Double.POSITIVE_INFINITY;
        for(BlockPos p:BlockPos.betweenClosed(BlockPos.containing(standing.minX,standing.minY,standing.minZ),
                BlockPos.containing(standing.maxX,standing.maxY,standing.maxZ))){
            if(!breakable(p))continue;
            boolean intersects=false;
            for(AABB shape:level.getBlockState(p).getCollisionShape(level,p,CollisionContext.of(chef)).toAabbs())
                if(shape.move(p).intersects(standing)){intersects=true;break;}
            if(!intersects)continue;
            double distance=Vec3.atCenterOf(p).distanceToSqr(chef.getEyePosition());
            if(distance<nearest){nearest=distance;trapped=p.immutable();}
        }
        if(trapped!=null){
            if(!escapingBody){reset("embedded body");escapingBody=true;}
            mine(trapped);return true;
        }
        if(escapingBody){reset("body freed");escapingBody=false;}
        // An unbreakable low ceiling is traversable in the actual prone pose.
        // Probe the full swept native body; never move through or erase bedrock.
        if(!clear(chef.position())&&chef.getBbHeight()<1.5&&floorSupported(chef.position())){
            Vec3 best=null;double score=Double.NEGATIVE_INFINITY;
            Vec3 toward=chef.target.position().subtract(chef.position()).multiply(1,0,1).normalize();
            for(int i=0;i<16;i++){
                double a=i*Math.PI/8;Vec3 direction=new Vec3(Math.cos(a),0,Math.sin(a));
                Vec3 end=chef.position().add(direction.scale(1.25));boolean pass=true;
                for(int n=1;n<=5;n++){
                    Vec3 at=chef.position().lerp(end,n/5d);
                    AABB box=chef.getBoundingBox().move(at.subtract(chef.position())).deflate(.005);
                    for(BlockPos cell:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),
                            BlockPos.containing(box.maxX,box.maxY,box.maxZ)))if(hazard(cell)){pass=false;break;}
                    if(!pass||!floorSupported(at)||!level.noCollision(chef,box)){pass=false;break;}
                }
                if(!pass)continue;
                double value=direction.dot(toward)+(clear(end)?2:0);
                if(value>score){score=value;best=end;}
            }
            if(best!=null){chef.setSprinting(false);chef.steer(best,false);return true;}
        }
        return false;
    }
    void tick(){
        beginTick(chef.owner.level().getServer().getTickCount());
        if(chef.getY()<level.getMinY()-8){returnFromVoid();return;}
        if(chef.onGround()&&floorSupported(chef.position())&&clear(chef.position()))lastGround=chef.position();
        if(flightAim!=null){
            if(!chef.onGround()){chef.setSprinting(true);chef.steer(flightAim,false);return;}
            flightAim=null;
        }
        if(replan>0)replan--;
        failedSteps.entrySet().removeIf(e->e.getValue()<=chef.tickCount);
        BlockPos target=chef.target.blockPosition();
        if(goal==null||(goal.distManhattan(target)>4&&(!airborneJump||chef.onGround()))){reset("target moved");goal=target;}
        if(!path.isEmpty()&&mining==null&&chef.onGround()&&chef.position().distanceToSqr(previous)<.001)stuck++;else stuck=0;
        previous=chef.position();
        if(stuck>(chef.horizontalCollision?6:24)){failedSteps.put(path.peek(),chef.tickCount+100);reset("stuck");goal=target;replan=3;}
        retireReached();
        // A short uphill leap can land against the near face before reaching its
        // waypoint. Re-arm its native jump promptly instead of waiting for the
        // two-second progress watchdog with an already-consumed launch latch.
        if(airborneJump&&chef.onGround()&&chef.tickCount>=nextJump-4)airborneJump=false;
        if(path.isEmpty()&&(search!=null||replan==0)){plan(target);if(search==null)replan=8;}
        if(path.isEmpty()){
            if(chef.onGround()){
                Spot footing=actualStart();
                if(footing!=null&&footing.point().subtract(chef.position()).horizontalDistanceSqr()>.04
                        &&walkClear(chef.position(),footing.point(),false))
                    chef.steer(footing.point(),false,chef.target.getEyePosition());
                else chef.halt();
            }
            return;
        }
        Step current=path.peek();
        if(current!=progressStep){progressStep=current;bestProgress=Double.NEGATIVE_INFINITY;progressAt=chef.tickCount;}
        Vec3 edge=current.to.point().subtract(current.from.point());
        double advance=chef.position().subtract(current.from.point()).dot(edge.normalize());
        if(advance>bestProgress+.15){bestProgress=advance;progressAt=chef.tickCount;}
        // A jump loop can repeatedly leave the ground without getting any closer.
        // Run-up has its own short deadline; it cannot suppress this watchdog forever.
        if(mining==null&&preparingJump==null&&chef.tickCount-progressAt>48){
            failedSteps.put(current,chef.tickCount+100);reset("no edge progress");chef.halt();return;
        }
        execute(path.peek());
    }
    private void retireReached(){
        while(!path.isEmpty()){
            Step s=path.peek();Vec3 to=s.to.point(),from=s.from.point(),now=chef.position();
            double dx=now.x-to.x,dz=now.z-to.z;
            Vec3 edge=to.subtract(from);double length=edge.horizontalDistanceSqr();
            double projection=length<.01?0:((now.x-from.x)*edge.x+(now.z-from.z)*edge.z)/length;
            double cross=length<.01?0:Math.abs((now.x-from.x)*edge.z-(now.z-from.z)*edge.x)/Math.sqrt(length);
            boolean reached=dx*dx+dz*dz<.13||(projection>=1&&cross<.45);
            if(s.action==Action.PILLAR)reached=dx*dx+dz*dz<.23;
            if(!reached||Math.abs(now.y-to.y)>.55||!floorSupported(to))break;
            if((s.action==Action.JUMP||s.action==Action.JUMP_BUILD||s.action==Action.PILLAR)&&!chef.onGround())break;
            path.remove();clearTakeoff();stuck=0;
        }
    }
    private void execute(Step step){
        Spot to=step.to;Vec3 next=to.point();
        if(!loaded(to.cell())){reset("unloaded step");chef.halt();return;}
        if(step.action==Action.JUMP_BUILD){jumpBuild(step);return;}
        if(step.action==Action.MINE||step.action==Action.MINE_DOWN){
            BlockPos obstacle=step.action==Action.MINE_DOWN?
                    obstacle(new Vec3(next.x,Math.max(next.y,chef.getY()-.9),next.z)):obstacle(next);
            if(obstacle!=null){mine(obstacle);return;}
            if(step.action==Action.MINE_DOWN&&!chef.onGround()){
                // A removed support may leave several air cells before the next floor.
                // Let gravity finish that fall instead of invalidating the still-solid lower landing.
                chef.halt();return;
            }
        }
        if(step.action==Action.RAMP){
            BlockPos block=BlockPos.containing(next.x,next.y-1,next.z);
            if(!support(block)){
                chef.setSprinting(false);
                if(chef.getBoundingBox().intersects(new AABB(block))){chef.steer(step.from.point(),false);return;}
                if(!place(block)){if(!placementWaiting){failedSteps.put(step,chef.tickCount+100);reset("ramp place "+lastPlacement);chef.halt();}return;}
                nextBuild=chef.tickCount+2;
            }
        }
        if(step.action==Action.PILLAR){
            BlockPos block=BlockPos.containing(next.x,next.y-1,next.z);
            if(!support(block)){
                if(!clear(next)){reset("pillar headroom");chef.halt();return;}
                chef.setSprinting(false);
                if(pillarStep!=step){pillarStep=step;pillarLaunched=false;pillarSettle=0;}
                if(!pillarLaunched){
                    // Ground friction settles the real actor before its jump. A zero-input jump
                    // must not inherit the preceding walk's horizontal momentum off the column.
                    if(!chef.onGround()){chef.halt();return;}
                    double centering=chef.position().subtract(step.from.point()).horizontalDistanceSqr();
                    if(centering>.0256){pillarSettle=0;chef.steer(step.from.point(),false);return;}
                    if(chef.getDeltaMovement().horizontalDistanceSqr()>.0025){pillarSettle=0;chef.halt();return;}
                    boolean equipped=chef.equip(new ItemStack(Items.COBBLESTONE,64));
                    boolean aimed=chef.aimAction(new Vec3(chef.getX(),block.getY(),chef.getZ()));
                    if(!equipped||!aimed){chef.halt();return;}
                    pillarLaunched=true;
                }
                chef.halt();
                if(chef.onGround())chef.pillarJump();
                if(chef.getBoundingBox().minY>=block.getY()+1-.01&&chef.tickCount>=nextBuild){
                    if(!place(block)){if(!placementWaiting)reset("pillar place "+lastPlacement);return;}
                    nextBuild=chef.tickCount+2;
                }
                return;
            }
            // Keep looking at the column between placements, including the landing.
            // Walking to its centre would turn the camera back toward the victim every floor.
            chef.halt();
            chef.aimAction(new Vec3(chef.getX(),block.getY()+1,chef.getZ()));
            return;
        }
        if(step.action==Action.BRIDGE&&!floorSupported(next)){
            // Commit support ahead while still on the preceding block. No fake player sneak is assumed.
            chef.setSprinting(false);
            BlockPos block=BlockPos.containing(next.x,next.y-1,next.z);
            if(chef.tickCount<nextBuild){chef.halt();return;}
            if(!floorSupported(chef.position())||!clear(next)){reset("bridge support/headroom");chef.halt();return;}
            if(!place(block)){if(!placementWaiting){reset("bridge place "+lastPlacement);chef.halt();}return;}
            nextBuild=chef.tickCount+2;
        }
        if(!clear(next)||(!floorSupported(next)&&step.action!=Action.JUMP)){reset("invalid destination");chef.halt();return;}
        clearMining();
        boolean jump=step.action==Action.JUMP||step.action==Action.RAMP;
        if(jump){
            boolean launch=false;
            // Recheck a launch after terrain changes, but leave native flight in control after takeoff.
            if(!airborneJump&&chef.onGround()){
                if(!jumpClear(step.from,to)){reset("jump recheck");chef.halt();return;}
                double length=Math.sqrt(step.from.point().subtract(next).horizontalDistanceSqr());
                if(length>3.2&&!prepareRunup(step))return;
                airborneJump=true;preparingJump=null;runway=null;nextJump=chef.tickCount+12;
                launch=true;
            }
            // A one-block stair rise needs an ordinary jump, not a sprint impulse
            // which throws the actor past its small diagonal landing.
            chef.setSprinting(to.y()-step.from.y()<=.61||next.subtract(step.from.point()).horizontalDistance()>2.2);
            // Landing on the near edge is already a successful leap. Walk the
            // remaining fraction instead of jumping again before retiring this step.
            chef.steer(next,launch);
            return;
        }
        boolean openRun=(step.action==Action.WALK||step.action==Action.DROP)
                &&chef.position().distanceToSqr(chef.target.position())>9;
        Vec3 aim=next;
        Step aimStep=step;
        // String-pull short turns only after checking the complete body and every support.
        // Uneven ground must not force braking at every grid-cell centre.
        if(openRun&&step.action==Action.WALK){
            Iterator<Step> it=path.iterator();it.next();
            for(int i=0;i<3&&it.hasNext();i++){
                Step further=it.next();
                // Aim through a takeoff instead of braking on its cell centre.
                if(further.action==Action.JUMP&&further.from.equals(to)){
                    Vec3 approach=next.subtract(chef.position()).multiply(1,0,1);
                    Vec3 flight=further.to.point().subtract(next).multiply(1,0,1);
                    if(approach.lengthSqr()>.001&&approach.normalize().dot(flight.normalize())>.985)
                        aim=further.to.point();
                    break;
                }
                if(further.action!=Action.WALK||Math.abs(further.to.y()-to.y())>.6)break;
                if(Math.abs(further.to.y()-chef.getY())>.61||!walkClear(chef.position(),further.to.point(),true))break;
                aim=further.to.point();
                aimStep=further;
            }
        }
        // Discard only the prefix whose entire shortcut was collision/support tested.
        // Keeping its old head made the actor chase a waypoint behind it until stuck.
        while(path.peek()!=aimStep&&!path.isEmpty())path.remove();
        chef.setSprinting(openRun);
        boolean runningJump=false;
        if(openRun&&chef.onGround()&&chef.tickCount>=nextJump
                &&chef.position().subtract(chef.target.position()).horizontalDistanceSqr()>25){
            Vec3 forward=aim.subtract(chef.position()).multiply(1,0,1);
            if(forward.horizontalDistanceSqr()>1){
                Vec3 landing=chef.position().add(forward.normalize().scale(4.25));
                List<Spot> ends=floors(null,(int)Math.floor(landing.x),(int)Math.floor(landing.z),chef.getY()-.4,chef.getY()+.2);
                Spot start=actualStart();
                for(Spot end:ends)if(start!=null&&walkClear(chef.position(),end.point(),true)&&jumpClear(start,end)){
                    runningJump=true;flightAim=end.point();nextJump=chef.tickCount+10;break;
                }
            }
        }
        // Slabs/stairs within native step height stay walking; only explicit jump edges request a jump.
        chef.steer(aim,runningJump);
    }
    private Vec3 runway(Spot from,Spot to){
        if(!chef.survival.canSprint())return null;
        Vec3 direction=to.point().subtract(from.point()).multiply(1,0,1).normalize();
        Vec3 behind=from.point().subtract(direction.scale(.4));
        return walkClear(behind,from.point(),true)?behind:null;
    }
    private boolean prepareRunup(Step step){
        Vec3 takeoff=step.from.point(),landing=step.to.point();
        Vec3 direction=landing.subtract(takeoff).multiply(1,0,1).normalize();
        Vec3 offset=chef.position().subtract(takeoff);
        double along=offset.x*direction.x+offset.z*direction.z;
        double lateral=Math.abs(offset.x*direction.z-offset.z*direction.x);
        Vec3 velocity=chef.getDeltaMovement();
        double speed=velocity.x*direction.x+velocity.z*direction.z;
        if(preparingJump!=step){
            preparingJump=step;runwayStarted=chef.tickCount;runwayPhase=1;runwayRetreats=0;
        }
        if(chef.tickCount-runwayStarted>24){
            failedSteps.put(step,chef.tickCount+100);reset("bounded takeoff deadline");chef.halt();return false;
        }
        // Finish a single short backstep before approaching again. The old forward
        // branch interrupted this retreat and repeated it forever near the lip.
        if(runwayPhase==0){
            chef.setSprinting(false);chef.steer(runway,false,landing);
            if(chef.position().subtract(runway).horizontalDistanceSqr()<.0256)runwayPhase=1;
            return false;
        }
        // Include the native +.2 sprint-jump impulse and subsequent air acceleration.
        // getDeltaMovement contains post-friction velocity; it is not ground travel speed.
        double required=landing.subtract(chef.position()).horizontalDistance()-.4;
        if(lateral<.38&&along<=.81&&nativeJumpReach(speed,landing.y-chef.getY())>=required)return true;
        double nextAdvance=Math.max(0,speed)+.1274;
        if(along+nextAdvance<=.78&&lateral<.38){
            chef.setSprinting(true);chef.steer(landing,false);return false;
        }
        if(runwayRetreats==0){
            runway=runway(step.from,step.to);
            if(runway!=null&&walkClear(chef.position(),runway,true)){
                runwayRetreats++;runwayPhase=0;
                chef.setSprinting(false);chef.steer(runway,false,landing);return false;
            }
        }
        failedSteps.put(step,chef.tickCount+100);reset("takeoff lacks supported approach");chef.halt();return false;
    }
    private double nativeJumpReach(double speed,double rise){
        double forward=speed+.2,travel=0,y=0,vy=.42;
        for(int tick=0;tick<22;tick++){
            y+=vy;vy=(vy-.08)*.98;
            forward+=.026*.98;travel+=forward;forward*=.91;
            if(tick>4&&y<=rise+.01)break;
        }
        return travel;
    }
    private void jumpBuild(Step step){
        Vec3 landing=step.to.point();
        int dx=Integer.signum(step.to.x-step.from.x),dz=Integer.signum(step.to.z-step.from.z);
        BlockPos end=step.to.cell().below(),first=end.offset(dx,0,dz);
        if(!airborneJump){
            if(!chef.onGround()){chef.steer(landing,false);return;}
            // Prepare the hand and camera before takeoff. The far bank supplies real
            // attachment faces; no floating blocks or position/velocity teleports are used.
            Vec3 face=Vec3.atCenterOf(first).add(dx*.5,0,dz*.5);
            boolean equipped=chef.equip(new ItemStack(Items.COBBLESTONE,64));
            boolean aimed=chef.aimAction(face);
            if(!equipped||!aimed){chef.halt();return;}
            if(!prepareRunup(step))return;
            airborneJump=true;preparingJump=null;runway=null;
            chef.setSprinting(true);chef.steer(landing,true);return;
        }
        if(!chef.onGround()){
            // Extend the far bank towards the flight, one legal face-connected block
            // at a time. Equipment/aim/cooldown waits must never erase airborne input.
            BlockPos pending=!support(first)?first:!support(end)?end:null;
            if(pending!=null&&chef.tickCount>=nextBuild
                    &&Vec3.atCenterOf(pending).distanceToSqr(chef.getEyePosition())<=20.25)
                place(pending,0,new HashSet<>());
            chef.setSprinting(true);chef.steer(landing,false);return;
        }
        if(floorSupported(landing)){chef.steer(landing,false);return;}
        failedSteps.put(step,chef.tickCount+100);reset("airborne support not completed");
    }
    private BlockPos obstacle(Vec3 feet){
        AABB box=body(feet);
        for(BlockPos p:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))){
            if(!loaded(p))return p.immutable();
            for(AABB b:level.getBlockState(p).getCollisionShape(level,p,CollisionContext.of(chef)).toAabbs())
                if(b.move(p.getX(),p.getY(),p.getZ()).intersects(box))return p.immutable();
        }
        return null;
    }
    private boolean walkClear(Vec3 from,Vec3 to,boolean supported){
        int samples=Math.max(2,(int)Math.ceil(from.distanceTo(to)*4));
        for(int i=0;i<=samples;i++){
            double t=i/(double)samples;
            Vec3 p=from.lerp(to,t);
            // Native step resolution raises the actor before crossing the step's horizontal face.
            if(to.y>from.y&&to.y-from.y<=.61)p=new Vec3(p.x,to.y,p.z);
            // Walk off a half-step at its current floor height. Interpolating downward
            // would falsely put the body inside the trailing slab before its edge is cleared.
            if(from.y>to.y&&from.y-to.y<=.65&&i<samples)p=new Vec3(p.x,from.y,p.z);
            if(!clear(p))return false;
            if(supported){
                boolean floor=false;
                for(double drop=0;drop<=.65;drop+=.125)if(floorSupported(p.add(0,-drop,0))){floor=true;break;}
                if(!floor)return false;
            }
        }
        return true;
    }
    private boolean jumpClear(Spot from,Spot to){
        return jumpClear(from,to,false);
    }
    private boolean jumpClear(Spot from,Spot to,boolean futureSupport){
        double dx=to.x-from.x,dz=to.z-from.z,distance=Math.sqrt(dx*dx+dz*dz);
        double rise=to.y()-from.y();
        if(distance>5.05||rise>1.05||rise< -2.5||!(futureSupport?clear(to.point()):safe(to)))return false;
        if(distance>1.5&&!chef.survival.canSprint())return false;
        // Conservative native jump envelope (.42 launch, .08 gravity, .98 drag). Delayed
        // horizontal progress on ascent models pressing into a one-block step until feet clear it.
        double y=from.y(),vy=.42;
        List<Double> arc=new ArrayList<>();
        for(int tick=0;tick<18;tick++){
            y+=vy;vy=(vy-.08)*.98;arc.add(y);
            if(tick>4&&y<=to.y()+.08)break;
        }
        int ticks=arc.size();
        double edge=distance>3?.30:0;
        if(ticks<6||(distance-edge*2)/ticks>.4)return false;
        int wait=rise>.61?3:0;
        Vec3 direction=to.point().subtract(from.point()).multiply(1,0,1).normalize();
        Vec3 start=from.point().add(direction.scale(edge)),finish=to.point().subtract(direction.scale(edge));
        Vec3 prev=start;
        for(int i=0;i<ticks;i++){
            double t=Math.max(0,(i+1-wait)/(double)(ticks-wait));
            Vec3 horizontal=start.lerp(finish,t);
            Vec3 p=new Vec3(horizontal.x,Math.max(to.y(),arc.get(i)),horizontal.z);
            if(!walkClear(prev,p,false))return false;
            prev=p;
        }
        return true;
    }
    private static double heuristic(Spot p,BlockPos target){
        double dx=p.x-target.getX(),dz=p.z-target.getZ();
        return Math.max(0,Math.sqrt(dx*dx+dz*dz)-1.5)+Math.abs(p.y()-target.getY())*.7;
    }
    private Node directColumn(Spot start,BlockPos destination){
        double rise=destination.getY()-start.y();
        if(start.height%16!=0||rise<=2||rise>14)return null;
        Spot base=new Spot(destination.getX(),destination.getZ(),start.height);
        if(base.point().distanceToSqr(start.point())>144||!walkClear(start.point(),base.point(),true))return null;
        Node column=new Node(start,0,0,null,Action.WALK);
        // The whole supported approach and column must be possible before taking
        // this shortcut. A wall, real staircase, roof or gap falls back to the graph.
        while(!column.spot.equals(base)){
            Spot previous=column.spot;
            Spot next=new Spot(previous.x+Integer.signum(base.x-previous.x),
                    previous.z+Integer.signum(base.z-previous.z),start.height);
            if(!walkClear(previous.point(),next.point(),true))return null;
            column=new Node(next,column.cost+previous.point().distanceTo(next.point()),0,column,Action.WALK);
        }
        for(int i=1;i<rise;i++){
            Spot up=new Spot(base.x,base.z,start.height+i*16);
            if(!clear(up.point())||!replaceable(column.spot.cell()))return null;
            column=new Node(up,column.cost+4.2,0,column,Action.PILLAR);
        }
        return column;
    }
    private Node directSurface(Spot start,BlockPos destination){
        Node current=new Node(start,0,0,null,Action.WALK);
        int started=probes;
        for(int i=0;i<24&&probes-started<720&&probes<GLOBAL_PROBES;i++){
            Spot from=current.spot;
            if(heuristic(from,destination)<.75)return current==null||current.parent==null?null:current;
            int dx=Integer.signum(destination.getX()-from.x),dz=Integer.signum(destination.getZ()-from.z);
            if(dx==0&&dz==0)return null;
            Node next=null;
            // This fast path never edits terrain. Obstructed/nonlinear routes retain the
            // general search frontier; straightforward noisy paths can start immediately.
            for(int length=1;length<=5&&next==null;length++){
                if(dx!=0&&dz!=0&&length>2)break;
                for(Spot to:floors(null,from.x+dx*length,from.z+dz*length,from.y()-2.5,from.y()+1.05)){
                    double rise=to.y()-from.y();
                    Action action=length>1||rise>.61?Action.JUMP:rise<-.65?Action.DROP:Action.WALK;
                    if(failedSteps.containsKey(new Step(from,to,action)))continue;
                    boolean possible=action==Action.JUMP?jumpClear(from,to):action==Action.DROP?
                            dropClear(from,to):walkClear(from.point(),to.point(),true);
                    if(!possible)continue;
                    double cost=current.cost+from.point().distanceTo(to.point())+(action==Action.JUMP?.65:0);
                    Node candidate=new Node(to,cost,cost+heuristic(to,destination),current,action);
                    if(next==null||candidate.estimate<next.estimate)next=candidate;
                }
            }
            if(next==null)return null;
            current=next;
        }
        return heuristic(start,destination)-heuristic(current.spot,destination)>12?current:null;
    }
    private void plan(BlockPos destination){
        if(budget<=0||probes>=GLOBAL_PROBES)return;
        goal=destination;
        if(search==null){
            Spot start=actualStart();if(start==null)return;
            searches++;
            Node surface=directSurface(start,destination);
            if(surface!=null){commit(surface);return;}
            Node column=directColumn(start,destination);
            if(column!=null){commit(column);return;}
            search=new Search(start,destination);
        }
        Search work=search;int count=0,probeStart=probes;
        long sliceDeadline=Math.min(deadline,System.nanoTime()+6_000_000L);
        Node completed=null;
        while(!work.open.isEmpty()&&count<SLICE&&budget>0&&probes<GLOBAL_PROBES&&probes-probeStart<SLICE_PROBES
                &&System.nanoTime()<sliceDeadline){
            Node n=work.open.poll();count++;budget--;
            if(n.cost>work.costs.getOrDefault(n.spot,Double.POSITIVE_INFINITY))continue;
            work.expanded++;
            if(heuristic(n.spot,work.destination)<heuristic(work.best.spot,work.destination))work.best=n;
            if(heuristic(n.spot,work.destination)<.75&&n!=work.root){completed=n;break;}
            expand(work,n);
            if(work.expanded>=EDIT_LIMIT||work.open.size()>=MAX_OPEN)break;
        }
        if(completed!=null){commit(completed);return;}
        boolean exhausted=work.open.isEmpty()||work.expanded>=EDIT_LIMIT||work.open.size()>=MAX_OPEN;
        if(!exhausted)return; // Retain the entire frontier through detours and CPU slices.
        if(work.best!=work.root)commit(work.best);else{search=null;replan=12;}
    }
    private void commit(Node end){
        for(Node n=end;n.parent!=null;n=n.parent)path.addFirst(new Step(n.parent.spot,n.spot,n.action));
        search=null;clearTakeoff();
    }
    private void expand(Search work,Node n){
        Spot s=n.spot;
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            if(dx==0&&dz==0)continue;
            int x=s.x+dx,z=s.z+dz;
            if(Math.abs(x-work.start.x)>32||Math.abs(z-work.start.z)>32)continue;
            boolean ordinaryWalk=false;
            boolean plannedSupport=n.action==Action.BRIDGE||n.action==Action.JUMP_BUILD||n.action==Action.PILLAR||n.action==Action.RAMP;
            for(Spot p:floors(work,x,z,s.y()-2.5,s.y()+1.05)){
                double rise=p.y()-s.y();Action action;
                if(rise>.61)action=Action.JUMP;else if(rise<-.65)action=Action.DROP;else action=Action.WALK;
                boolean possible=work.links.computeIfAbsent(new Step(s,p,action),key->action==Action.JUMP?
                        jumpClear(s,p):action==Action.DROP?dropClear(s,p):walkClear(s.point(),p.point(),!plannedSupport));
                if(possible){
                    if(action==Action.WALK)ordinaryWalk=true;
                    offer(work,p,n.cost+Math.hypot(dx,dz)+(action==Action.JUMP?1.4:action==Action.DROP?.3:0),n,action);
                }
            }
            // Multi-cell parkour is considered before any construction edges. Diagonal corner
            // collisions are checked with the same complete mannequin box as cardinal movement.
            boolean launchClear=!ordinaryWalk&&clear(s.point().add(dx*.75,1.1,dz*.75));
            if(launchClear)for(int length=2;length<=5;length++){
                if(dx!=0&&dz!=0&&length>2)break;
                for(Spot p:floors(work,s.x+dx*length,s.z+dz*length,s.y()-1.2,s.y()+.65)){
                    if(jumpClear(s,p))offer(work,p,n.cost+Math.hypot(dx,dz)*length+.65,n,Action.JUMP);
                }
            }
            if(launchClear&&s.height%16==0&&(dx==0||dz==0)&&chef.survival.canSprint()){
                for(int bankDistance=5;bankDistance<=6;bankDistance++){
                    BlockPos bank=new BlockPos(s.x+dx*bankDistance,(int)s.y()-1,s.z+dz*bankDistance);
                    Spot landing=new Spot(s.x+dx*(bankDistance-2),s.z+dz*(bankDistance-2),s.height);
                    BlockPos end=landing.cell().below(),first=end.offset(dx,0,dz);
                    Direction facing=Direction.getApproximateNearest(new Vec3(dx,0,dz)).getOpposite();
                    if(support(bank)&&level.getBlockState(bank).isFaceSturdy(level,bank,facing)
                            &&replaceable(first)&&replaceable(end)&&jumpClear(s,landing,true))
                        offer(work,landing,n.cost+bankDistance-2+2.4,n,Action.JUMP_BUILD);
                }
            }
            Spot flat=new Spot(x,z,s.height);
            BlockPos below=BlockPos.containing(x+.5,s.y()-1,z+.5);
            if(s.height%16==0&&!floorSupported(flat.point())&&clear(flat.point())&&replaceable(below))
                offer(work,flat,n.cost+Math.hypot(dx,dz)+1.8,n,Action.BRIDGE);
            if(s.height%16==0&&support(below)&&mineableBody(flat))offer(work,flat,n.cost+3+miningCost(flat),n,Action.MINE);
            if(s.height%16==0&&work.destination.getY()>s.y()+.7){
                Spot rising=new Spot(x,z,s.height+16);
                if(replaceable(flat.cell())&&clear(rising.point())&&Math.abs(rising.y()-work.start.y())<14)
                    offer(work,rising,n.cost+3.4+Math.hypot(dx,dz),n,Action.RAMP);
            }
        }
        if(work.destination.getY()<s.y()-.7&&s.height%16==0){
            for(int drop=1;drop<=16;drop++){
                Spot down=new Spot(s.x,s.z,s.height-drop*16);
                if(support(down.cell().below())&&mineableShaft(s,down))
                    offer(work,down,n.cost+2.2+drop*.3+miningCost(new Spot(s.x,s.z,s.height-16)),n,Action.MINE_DOWN);
            }
        }
        if(s.height%16==0&&work.destination.getY()>s.y()+.7){
            Spot up=new Spot(s.x,s.z,s.height+16);
            if(clear(up.point())&&replaceable(s.cell())&&Math.abs(up.y()-work.start.y())<14)
                offer(work,up,n.cost+4.2,n,Action.PILLAR);
        }
    }
    private boolean dropClear(Spot from,Spot to){
        // Walk off the edge before descending, rather than clipping diagonally through the ledge.
        Vec3 high=new Vec3(to.x+.5,from.y(),to.z+.5);
        if(!walkClear(from.point(),high,false))return false;
        return walkClear(high,to.point(),false)&&safe(to);
    }
    private boolean replaceable(BlockPos p){return loaded(p)&&!hazard(p)&&level.getBlockState(p).canBeReplaced()
            &&!level.getFluidState(p).is(FluidTags.WATER);}
    private boolean mineableBody(Spot p){
        BlockPos obstacle=obstacle(p.point());
        if(obstacle==null||obstacle.getY()<p.cell().getY())return false;
        for(BlockPos b:BlockPos.betweenClosed(p.cell(),p.cell().above()))
            if(!level.getBlockState(b).getCollisionShape(level,b,CollisionContext.of(chef)).isEmpty()&&!breakable(b))return false;
        return true;
    }
    private boolean mineableShaft(Spot from,Spot to){
        boolean obstruction=false;
        for(int y=to.cell().getY();y<from.cell().getY();y++){
            BlockPos p=new BlockPos(from.x,y,from.z);
            if(!loaded(p)||hazard(p))return false;
            if(!level.getBlockState(p).getCollisionShape(level,p,CollisionContext.of(chef)).isEmpty()){
                if(!breakable(p))return false;
                obstruction=true;
            }
        }
        return obstruction;
    }
    private double miningCost(Spot p){
        BlockPos obstacle=obstacle(p.point());
        return obstacle==null?0:Math.max(0,level.getBlockState(obstacle).getDestroySpeed(level,obstacle)*2);
    }
    private void offer(Search work,Spot p,double cost,Node parent,Action action){
        if(failedSteps.containsKey(new Step(parent.spot,p,action)))return;
        if(Math.abs(p.y()-work.start.y())>16||cost>=work.costs.getOrDefault(p,Double.POSITIVE_INFINITY)||work.costs.size()>=8192)return;
        double remaining=heuristic(p,work.destination);
        // Costs are sprint-distance equivalents, including time to turn/place/jump/mine.
        // Walking, parkour and edits compete from the first node; a long detour no longer
        // wins merely because the construction search was postponed until terrain exhausted.
        // Height can be gained along a staircase while covering horizontal ground.
        // Charge extra height only for the residual climb, rather than preferring an
        // early artificial tower beside a perfectly usable diagonal staircase.
        double horizontal=Math.hypot(work.destination.getX()-p.x,work.destination.getZ()-p.z);
        remaining+=Math.max(0,work.destination.getY()-p.y()-1-horizontal*.85)*3;
        work.costs.put(p,cost);work.open.add(new Node(p,cost,cost+remaining*1.25,parent,action));
    }
    private void clearMining(){
        if(mining!=null)level.destroyBlockProgress(chef.getId(),mining,-1);
        mining=null;miningState=null;miningCause=null;progress=0;
    }
    private void mine(BlockPos pos){
        chef.setSprinting(false);chef.halt();
        if(Vec3.atCenterOf(pos).distanceToSqr(chef.getEyePosition())>20.25||!breakable(pos)){reset("mining reach/state");return;}
        BlockState state=level.getBlockState(pos);
        if(!pos.equals(mining)||!state.equals(miningState)){
            clearMining();mining=pos.immutable();miningState=state;
            miningCause=chef.service.gameItems().indirectKills().coldCause(chef.owner,false);
        }
        ItemStack tool=tools.stream().max(Comparator.comparingDouble(t->t.getDestroySpeed(state))).orElse(tools.getFirst());
        if(!chef.equip(tool.copy())||!chef.aimAction(Vec3.atCenterOf(pos)))return;
        if(chef.tickCount%4==0)chef.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
        chef.acted(Vec3.atCenterOf(pos));
        if(chef.tickCount%4==0)level.playSound(null,pos,state.getSoundType().getHitSound(),net.minecraft.sounds.SoundSource.BLOCKS,.5f,1);
        float hardness=state.getDestroySpeed(level,pos);
        progress+=hardness==0?1:tool.getDestroySpeed(state)/hardness/(tool.isCorrectToolForDrops(state)||!state.requiresCorrectToolForDrops()?30:100);
        level.destroyBlockProgress(chef.getId(),pos,Math.min(9,(int)(progress*10)));
        if(progress<1)return;
        boolean own=placed.remove(pos);var blockEntity=level.getBlockEntity(pos);
        chef.service.gameItems().indirectKills().trapChanges(level,miningCause,()->{
            chef.service.gameItems().indirectKills().supportRemoved(level,miningCause,pos);
            if(!own&&(!state.requiresCorrectToolForDrops()||tool.isCorrectToolForDrops(state)))Block.dropResources(state,level,pos,blockEntity,chef,tool);
            level.levelEvent(2001,pos,Block.getId(state));level.removeBlock(pos,false);
        });
        clearMining();
    }
    private boolean place(BlockPos pos){return place(pos,2,new HashSet<>());}
    private boolean place(BlockPos pos,int connect,Set<BlockPos> visited){
        placementWaiting=false;
        if(!visited.add(pos))return false;
        if(chef.tickCount<nextBuild){placementWaiting=true;chef.halt();return false;}
        if(!replaceable(pos)||Vec3.atCenterOf(pos).distanceToSqr(chef.getEyePosition())>20.25){lastPlacement="reach/replaceable "+pos;return false;}
        // Find a real collision face on a neighbour and aim at its surface, as a player's
        // block interaction would. Fluids/noncolliding decorations do not attach a bridge.
        Vec3 hit=null;
        for(Direction d:Direction.values()){
            BlockPos neighbour=pos.relative(d);
            if(!support(neighbour))continue;
            for(AABB shape:level.getBlockState(neighbour).getCollisionShape(level,neighbour,CollisionContext.of(chef)).toAabbs()){
                double face=switch(d){case DOWN->shape.maxY;case UP->1-shape.minY;case NORTH->shape.maxZ;case SOUTH->1-shape.minZ;case WEST->shape.maxX;case EAST->1-shape.minX;};
                if(face<.999)continue;
                Vec3 candidate=Vec3.atCenterOf(pos).add(d.getStepX()*.5,d.getStepY()*.5,d.getStepZ()*.5);
                if(d==Direction.DOWN||d==Direction.UP)candidate=new Vec3(
                        net.minecraft.util.Mth.clamp(chef.getX(),pos.getX()+.1,pos.getX()+.9),candidate.y,
                        net.minecraft.util.Mth.clamp(chef.getZ(),pos.getZ()+.1,pos.getZ()+.9));
                if(candidate.distanceToSqr(chef.getEyePosition())>20.25)continue;
                var ray=level.clip(new net.minecraft.world.level.ClipContext(chef.getEyePosition(),candidate.add(d.getStepX()*.01,d.getStepY()*.01,d.getStepZ()*.01),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,chef));
                if(ray.getType()==net.minecraft.world.phys.HitResult.Type.MISS||ray.getBlockPos().equals(neighbour)){hit=candidate;break;}
            }
            if(hit!=null)break;
        }
        if(hit==null){
            // Diagonals still require a visible, face-connected real block.
            if(connect>0)for(Direction side:Direction.values()){
                BlockPos elbow=pos.relative(side);
                if(!replaceable(elbow))continue;
                boolean built=place(elbow,connect-1,visited);
                if(built||placementWaiting){placementWaiting=true;return false;}
            }
            lastPlacement="no attached visible face "+pos;return false;
        }
        BlockState state=Blocks.COBBLESTONE.defaultBlockState();
        if(!level.isUnobstructed(state,pos,CollisionContext.empty())){lastPlacement="entity collision "+pos;return false;}
        if(!chef.equip(new ItemStack(Items.COBBLESTONE,64))||!chef.aimAction(hit)){
            placementWaiting=true;chef.halt();return false;
        }
        chef.acted(hit);
        chef.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
        chef.service.gameItems().indirectKills().trapChanges(level,chef.service.gameItems().indirectKills().coldCause(chef.owner,false),()->level.setBlock(pos,state,3));
        if(!level.getBlockState(pos).is(Blocks.COBBLESTONE)){lastPlacement="world write rejected "+pos;return false;}
        level.playSound(null,pos,state.getSoundType().getPlaceSound(),net.minecraft.sounds.SoundSource.BLOCKS,1,.8f);
        level.gameEvent(chef,net.minecraft.world.level.gameevent.GameEvent.BLOCK_PLACE,pos);
        nextBuild=chef.tickCount+2;
        lastPlacement="placed "+pos;placed.add(pos.immutable());return true;
    }
}
