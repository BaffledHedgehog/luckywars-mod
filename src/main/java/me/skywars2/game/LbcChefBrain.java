package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
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
    private static final int SLICE=112,GLOBAL_NODES=896,TERRAIN_LIMIT=640,EDIT_LIMIT=1536,MAX_OPEN=8192;
    private static final int GLOBAL_PROBES=18000,SLICE_PROBES=2400;
    private static long budgetTick=Long.MIN_VALUE;
    private static int budget=GLOBAL_NODES,probes;
    static void beginTick(long tick){if(tick!=budgetTick){budgetTick=tick;budget=GLOBAL_NODES;probes=0;}}
    private final LbcWtf.Chef chef;
    private final ServerLevel level;
    private final List<ItemStack> tools=List.of(new ItemStack(Items.NETHERITE_PICKAXE),new ItemStack(Items.NETHERITE_AXE),
            new ItemStack(Items.NETHERITE_SHOVEL),new ItemStack(Items.NETHERITE_HOE),new ItemStack(Items.SHEARS),LbcWtf.sword());
    private final Set<BlockPos> placed=new HashSet<>();
    private final ArrayDeque<Step> path=new ArrayDeque<>();
    private Search search;
    private BlockPos goal,mining;
    private Vec3 lastGround;
    private BlockState miningState;
    private LbcIndirectKills.Owner miningCause;
    private float progress;
    private int replan,stuck,nextVoidAttempt=Integer.MIN_VALUE,nextBuild,nextJump;
    private Vec3 previous=Vec3.ZERO;
    private boolean airborneJump,pillarLaunched;
    private Step preparingJump,pillarStep;
    private Vec3 runway;
    private int runwayPhase,pillarSettle,resets;
    private String lastReset="none",lastPlacement="none";

    // Sixteenth-block floor precision preserves slabs, stairs, carpets and snow layers.
    private record Spot(int x,int z,int height){
        double y(){return height/16d;}
        Vec3 point(){return new Vec3(x+.5,y(),z+.5);}
        BlockPos cell(){return BlockPos.containing(x+.5,y()+.02,z+.5);}
    }
    private enum Action{WALK,DROP,JUMP,BRIDGE,MINE,PILLAR}
    private record Step(Spot from,Spot to,Action action){}
    private record Node(Spot spot,double cost,double estimate,Node parent,Action action){}
    private record FloorKey(int x,int z,int low,int high){}
    private static final class Search{
        final Spot start;
        final BlockPos destination;
        final boolean edits;
        final PriorityQueue<Node> open=new PriorityQueue<>(Comparator.comparingDouble(Node::estimate));
        final Map<Spot,Double> costs=new HashMap<>();
        final Map<FloorKey,List<Spot>> floors=new HashMap<>();
        final Node root;
        Node best;
        int expanded;
        Search(Spot start,BlockPos destination,boolean edits){
            this.start=start;this.destination=destination;this.edits=edits;
            root=new Node(start,0,heuristic(start,destination),null,Action.WALK);best=root;
            open.add(root);costs.put(start,0d);
        }
    }
    LbcChefBrain(LbcWtf.Chef chef){this.chef=chef;level=(ServerLevel)chef.level();}
    List<ItemStack> tools(){return tools.stream().map(ItemStack::copy).toList();}
    String diagnostics(){return "path="+path.size()+" next="+path.peek()+" search="+(search==null?"none":
            (search.edits?"edit":"terrain")+" expanded="+search.expanded+" open="+search.open.size()+" best="+search.best.spot)
            +" goal="+goal+" stuck="+stuck+" replan="+replan+" takeoff="+runwayPhase+" pillar="+pillarLaunched
            +" probes="+probes+" ground="+chef.onGround()+" velocity="+chef.getDeltaMovement()+" resets="+resets+" lastReset="+lastReset+" placement="+lastPlacement;}
    void reset(){reset("external");}
    private void reset(String why){lastReset=why+" step="+path.peek()+" at="+chef.position();resets++;
        path.clear();search=null;clearMining();replan=0;stuck=0;clearTakeoff();}
    private void clearTakeoff(){airborneJump=false;preparingJump=null;runway=null;runwayPhase=0;
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
        AABB feet=new AABB(p.x-.24,p.y-.075,p.z-.24,p.x+.24,p.y+.005,p.z+.24);
        if(!loaded(BlockPos.containing(p.x,p.y-.05,p.z)))return false;
        return !level.noCollision(chef,feet)&&!hazard(BlockPos.containing(p.x,p.y-.05,p.z));
    }
    private Spot actualStart(){
        List<Spot> candidates=floors(null,chef.blockPosition().getX(),chef.blockPosition().getZ(),chef.getY()-2.5,chef.getY()+.6);
        return candidates.stream().min(Comparator.comparingDouble(p->Math.abs(p.y()-chef.getY()))).orElse(null);
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
        for(int height:heights){Spot p=new Spot(x,z,height);if(clear(p.point()))result.add(p);}
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
    void tick(){
        beginTick(chef.owner.level().getServer().getTickCount());
        if(chef.getY()<level.getMinY()-8){returnFromVoid();return;}
        if(chef.onGround()&&floorSupported(chef.position())&&clear(chef.position()))lastGround=chef.position();
        if(replan>0)replan--;
        BlockPos target=chef.target.blockPosition();
        if(goal==null||(goal.distManhattan(target)>4&&(!airborneJump||chef.onGround()))){reset("target moved");goal=target;}
        if(!path.isEmpty()&&mining==null&&chef.onGround()&&chef.position().distanceToSqr(previous)<.001)stuck++;else stuck=0;
        previous=chef.position();
        if(stuck>30){reset("stuck");goal=target;replan=3;}
        retireReached();
        if(path.isEmpty()&&(search!=null||replan==0)){plan(target);if(search==null)replan=8;}
        if(path.isEmpty()){chef.halt();return;}
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
            if((s.action==Action.JUMP||s.action==Action.PILLAR)&&!chef.onGround())break;
            path.remove();clearTakeoff();stuck=0;
        }
    }
    private void execute(Step step){
        Spot to=step.to;Vec3 next=to.point();
        if(!loaded(to.cell())){reset("unloaded step");chef.halt();return;}
        if(step.action==Action.MINE){
            BlockPos obstacle=obstacle(next);
            if(obstacle!=null){mine(obstacle);return;}
        }
        if(step.action==Action.PILLAR){
            BlockPos block=BlockPos.containing(next.x,next.y-1,next.z);
            if(!support(block)){
                if(!clear(next)){reset("pillar headroom");chef.halt();return;}
                chef.setSprinting(false);chef.lookAt(Vec3.atCenterOf(block));
                if(pillarStep!=step){pillarStep=step;pillarLaunched=false;pillarSettle=0;}
                if(!pillarLaunched){
                    // Ground friction settles the real actor before its jump. A zero-input jump
                    // must not inherit the preceding walk's horizontal momentum off the column.
                    if(!chef.onGround()){chef.halt();return;}
                    double centering=chef.position().subtract(step.from.point()).horizontalDistanceSqr();
                    if(centering>.0064){pillarSettle=0;chef.steer(step.from.point(),false);return;}
                    if(chef.getDeltaMovement().horizontalDistanceSqr()>.000625){pillarSettle=0;chef.halt();return;}
                    if(++pillarSettle<2){chef.halt();return;}
                    pillarLaunched=true;
                }
                chef.halt();
                if(chef.onGround())chef.pillarJump();
                if(chef.getBoundingBox().minY>=block.getY()+1-.01&&chef.tickCount>=nextBuild){
                    if(!place(block)){reset("pillar place "+lastPlacement);return;}
                    nextBuild=chef.tickCount+2;
                }
                return;
            }
        }
        if(step.action==Action.BRIDGE&&!floorSupported(next)){
            // Commit support ahead while still on the preceding block. No fake player sneak is assumed.
            chef.setSprinting(false);
            BlockPos block=BlockPos.containing(next.x,next.y-1,next.z);
            if(chef.tickCount<nextBuild){chef.halt();return;}
            if(!floorSupported(chef.position())||!clear(next)){reset("bridge support/headroom");chef.halt();return;}
            chef.lookAt(Vec3.atCenterOf(block));
            if(!place(block)){reset("bridge place "+lastPlacement);chef.halt();return;}
            nextBuild=chef.tickCount+2;
        }
        if(!clear(next)||(!floorSupported(next)&&step.action!=Action.JUMP)){reset("invalid destination");chef.halt();return;}
        clearMining();chef.setItemSlot(EquipmentSlot.MAINHAND,chef.combatWeapon());
        boolean jump=step.action==Action.JUMP;
        if(jump){
            // Recheck a launch after terrain changes, but leave native flight in control after takeoff.
            if(!airborneJump&&chef.onGround()){
                if(!jumpClear(step.from,to)){reset("jump recheck");chef.halt();return;}
                double length=Math.sqrt(step.from.point().subtract(next).horizontalDistanceSqr());
                if(length>2.2&&!prepareRunup(step))return;
                airborneJump=true;preparingJump=null;runway=null;nextJump=chef.tickCount+12;
            }
            chef.setSprinting(true);chef.steer(next,chef.onGround()&&airborneJump);
            return;
        }
        boolean openRun=step.action==Action.WALK&&Math.abs(next.y-chef.getY())<=.6&&path.size()>=3;
        Vec3 aim=next;
        // Look ahead only across a collinear collision-tested corridor. Never cut a corner over a gap.
        if(openRun){
            Iterator<Step> it=path.iterator();it.next();
            for(int i=0;i<2&&it.hasNext();i++){
                Step further=it.next();
                if(further.action!=Action.WALK||Math.abs(further.to.y()-to.y())>.6)break;
                Vec3 a=next.subtract(chef.position()),b=further.to.point().subtract(chef.position());
                double den=Math.sqrt(a.horizontalDistanceSqr()*b.horizontalDistanceSqr());
                if(den<.01||(a.x*b.x+a.z*b.z)/den<.985||!walkClear(chef.position(),further.to.point(),true))break;
                aim=further.to.point();
            }
        }
        chef.setSprinting(openRun);
        boolean runningJump=false;
        if(openRun&&chef.onGround()&&chef.tickCount>=nextJump&&chef.position().distanceToSqr(chef.target.position())>25){
            Vec3 forward=aim.subtract(chef.position()).multiply(1,0,1);
            if(forward.horizontalDistanceSqr()>1){
                Vec3 landing=chef.position().add(forward.normalize().scale(3));
                List<Spot> ends=floors(null,(int)Math.floor(landing.x),(int)Math.floor(landing.z),chef.getY()-.4,chef.getY()+.2);
                Spot start=actualStart();
                for(Spot end:ends)if(start!=null&&walkClear(chef.position(),end.point(),true)&&jumpClear(start,end)){
                    runningJump=true;nextJump=chef.tickCount+14;break;
                }
            }
        }
        // Slabs/stairs within native step height stay walking; only explicit jump edges request a jump.
        chef.steer(aim,runningJump);
    }
    private Vec3 runway(Spot from,Spot to){
        if(!chef.survival.canSprint())return null;
        int dx=Integer.signum(to.x-from.x),dz=Integer.signum(to.z-from.z);
        Vec3 behind=new Vec3(from.x+.5-dx*2,from.y(),from.z+.5-dz*2);
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
        // getDeltaMovement is the previous tick's post-ground-friction velocity, not
        // the pre-drag travel distance. Native sprint at base .1 caches about .156;
        // requiring .245 would loop around the runway forever despite a full run-up.
        if(along>=-.13&&along<=.32&&lateral<.14&&speed>=.13)return true;
        if(preparingJump!=step){
            preparingJump=step;runway=runway(step.from,step.to);runwayPhase=0;
            if(runway==null||!walkClear(chef.position(),runway,true)){
                reset("runway changed");chef.halt();return false;
            }
        }
        if(runwayPhase==0){
            chef.setSprinting(false);chef.steer(runway,false);
            if(chef.position().subtract(runway).horizontalDistanceSqr()<.045)runwayPhase=1;
            return false;
        }
        // Aim through the takeoff point so native input does not throttle just before launch.
        // The accepted jump impulse is delivered only on the last supported part of the runway.
        if(along>.32||lateral>.25){runwayPhase=0;chef.halt();return false;}
        chef.setSprinting(true);chef.steer(landing,false);return false;
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
        double dx=to.x-from.x,dz=to.z-from.z,distance=Math.sqrt(dx*dx+dz*dz);
        double rise=to.y()-from.y();
        if(distance>4.05||rise>1.05||rise< -2.5||!safe(to))return false;
        if(distance>1.5&&!chef.survival.canSprint())return false;
        if(distance>2.2&&runway(from,to)==null)return false;
        // Conservative native jump envelope (.42 launch, .08 gravity, .98 drag). Delayed
        // horizontal progress on ascent models pressing into a one-block step until feet clear it.
        double y=from.y(),vy=.42;
        List<Double> arc=new ArrayList<>();
        for(int tick=0;tick<18;tick++){
            y+=vy;vy=(vy-.08)*.98;arc.add(y);
            if(tick>4&&y<=to.y()+.08)break;
        }
        int ticks=arc.size();
        if(ticks<6||distance/ticks>.34)return false;
        int wait=rise>.61?3:0;
        Vec3 prev=from.point();
        for(int i=0;i<ticks;i++){
            double t=Math.max(0,(i+1-wait)/(double)(ticks-wait));
            Vec3 p=new Vec3(from.x+.5+dx*t,Math.max(to.y(),arc.get(i)),from.z+.5+dz*t);
            if(!walkClear(prev,p,false))return false;
            prev=p;
        }
        return true;
    }
    private static double heuristic(Spot p,BlockPos target){
        double dx=p.x-target.getX(),dz=p.z-target.getZ();
        return Math.max(0,Math.sqrt(dx*dx+dz*dz)-1.5)+Math.abs(p.y()-target.getY())*.7;
    }
    private void plan(BlockPos destination){
        if(budget<=0||probes>=GLOBAL_PROBES)return;
        goal=destination;
        if(search==null){Spot start=actualStart();if(start==null)return;search=new Search(start,destination,false);}
        Search work=search;int count=0,probeStart=probes;
        Node completed=null;
        while(!work.open.isEmpty()&&count<SLICE&&budget>0&&probes<GLOBAL_PROBES&&probes-probeStart<SLICE_PROBES){
            Node n=work.open.poll();count++;budget--;
            if(n.cost>work.costs.getOrDefault(n.spot,Double.POSITIVE_INFINITY))continue;
            work.expanded++;
            if(heuristic(n.spot,work.destination)<heuristic(work.best.spot,work.destination))work.best=n;
            if(heuristic(n.spot,work.destination)<.75&&n!=work.root){completed=n;break;}
            expand(work,n);
            if(work.expanded>=(work.edits?EDIT_LIMIT:TERRAIN_LIMIT)||work.open.size()>=MAX_OPEN)break;
        }
        if(completed!=null){commit(completed);return;}
        boolean exhausted=work.open.isEmpty()||work.expanded>=(work.edits?EDIT_LIMIT:TERRAIN_LIMIT)||work.open.size()>=MAX_OPEN;
        if(!exhausted)return; // Retain the entire frontier through detours and CPU slices.
        if(!work.edits){
            // A bounded terrain-only search gets the first opportunity, including short parkour.
            // For distant victims retain a genuinely useful walk segment rather than build needlessly.
            if(work.best!=work.root&&heuristic(work.best.spot,destination)<heuristic(work.start,destination)-6){commit(work.best);return;}
            search=new Search(work.start,destination,true);return;
        }
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
            boolean plannedSupport=n.action==Action.BRIDGE||n.action==Action.PILLAR;
            for(Spot p:floors(work,x,z,s.y()-2.5,s.y()+1.05)){
                double rise=p.y()-s.y();Action action;
                if(rise>.61)action=Action.JUMP;else if(rise<-.65)action=Action.DROP;else action=Action.WALK;
                boolean possible=action==Action.JUMP?jumpClear(s,p):walkClear(s.point(),p.point(),action!=Action.DROP&&!plannedSupport);
                if(plannedSupport&&dx!=0&&dz!=0)possible=false;
                if(action==Action.DROP)possible=dropClear(s,p);
                if(possible){
                    if(action==Action.WALK)ordinaryWalk=true;
                    offer(work,p,n.cost+Math.hypot(dx,dz)+(action==Action.JUMP?.5:action==Action.DROP?.3:0),n,action);
                }
            }
            // Multi-cell parkour is considered before any construction edges. Diagonal corner
            // collisions are checked with the same complete mannequin box as cardinal movement.
            if(!work.edits&&!ordinaryWalk)for(int length=2;length<=4;length++){
                if(dx!=0&&dz!=0&&length>2)break;
                for(Spot p:floors(work,s.x+dx*length,s.z+dz*length,s.y()-1.2,s.y()+.65)){
                    if(jumpClear(s,p))offer(work,p,n.cost+Math.hypot(dx,dz)*length+.65,n,Action.JUMP);
                }
            }
            if(!work.edits||dx!=0&&dz!=0)continue;
            Spot flat=new Spot(x,z,s.height);
            BlockPos below=BlockPos.containing(x+.5,s.y()-1,z+.5);
            if(s.height%16==0&&!floorSupported(flat.point())&&clear(flat.point())&&replaceable(below))
                offer(work,flat,n.cost+8,n,Action.BRIDGE);
            if(s.height%16==0&&support(below)&&mineableBody(flat))offer(work,flat,n.cost+12+miningCost(flat),n,Action.MINE);
        }
        if(work.edits&&s.height%16==0&&work.destination.getY()>s.y()+.7){
            Spot up=new Spot(s.x,s.z,s.height+16);
            if(clear(up.point())&&replaceable(s.cell())&&Math.abs(up.y()-work.start.y())<14)
                offer(work,up,n.cost+10,n,Action.PILLAR);
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
    private double miningCost(Spot p){
        BlockPos obstacle=obstacle(p.point());
        return obstacle==null?0:Math.max(0,level.getBlockState(obstacle).getDestroySpeed(level,obstacle)*2);
    }
    private void offer(Search work,Spot p,double cost,Node parent,Action action){
        if(Math.abs(p.y()-work.start.y())>16||cost>=work.costs.getOrDefault(p,Double.POSITIVE_INFINITY)||work.costs.size()>=8192)return;
        work.costs.put(p,cost);work.open.add(new Node(p,cost,cost+heuristic(p,work.destination),parent,action));
    }
    private void clearMining(){
        if(mining!=null)level.destroyBlockProgress(chef.getId(),mining,-1);
        mining=null;miningState=null;miningCause=null;progress=0;
    }
    private void mine(BlockPos pos){
        chef.setSprinting(false);chef.halt();chef.lookAt(Vec3.atCenterOf(pos));
        if(Vec3.atCenterOf(pos).distanceToSqr(chef.getEyePosition())>20.25||!breakable(pos)){reset("mining reach/state");return;}
        BlockState state=level.getBlockState(pos);
        if(!pos.equals(mining)||!state.equals(miningState)){
            clearMining();mining=pos.immutable();miningState=state;
            miningCause=chef.service.gameItems().indirectKills().coldCause(chef.owner,false);
        }
        ItemStack tool=tools.stream().max(Comparator.comparingDouble(t->t.getDestroySpeed(state))).orElse(tools.getFirst());
        chef.setItemSlot(EquipmentSlot.MAINHAND,tool.copy());
        if(chef.tickCount%4==0)chef.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
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
    private boolean place(BlockPos pos){
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
                if(candidate.distanceToSqr(chef.getEyePosition())>20.25)continue;
                var ray=level.clip(new net.minecraft.world.level.ClipContext(chef.getEyePosition(),candidate.add(d.getStepX()*.01,d.getStepY()*.01,d.getStepZ()*.01),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,chef));
                if(ray.getType()==net.minecraft.world.phys.HitResult.Type.MISS||ray.getBlockPos().equals(neighbour)){hit=candidate;break;}
            }
            if(hit!=null)break;
        }
        if(hit==null){lastPlacement="no attached visible face "+pos;return false;}
        BlockState state=Blocks.COBBLESTONE.defaultBlockState();
        if(!level.isUnobstructed(state,pos,CollisionContext.empty())){lastPlacement="entity collision "+pos;return false;}
        chef.lookAt(hit);chef.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.COBBLESTONE,64));
        chef.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
        chef.service.gameItems().indirectKills().trapChanges(level,chef.service.gameItems().indirectKills().coldCause(chef.owner,false),()->level.setBlock(pos,state,3));
        if(!level.getBlockState(pos).is(Blocks.COBBLESTONE)){lastPlacement="world write rejected "+pos;return false;}
        lastPlacement="placed "+pos;placed.add(pos.immutable());return true;
    }
}
