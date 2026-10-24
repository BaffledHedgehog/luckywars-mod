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
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** A deliberately bounded server planner, inspired by terrain-action pathfinding, with no client dependency. */
final class LbcChefBrain {
    private static final int NODES_PER_PLAN=256,GLOBAL_NODES=1536,MAX_SEARCH_POSITIONS=16384,MAX_OPEN=32768;
    private static long budgetTick=Long.MIN_VALUE;
    private static int budget=GLOBAL_NODES;
    static void beginTick(long tick){if(tick!=budgetTick){budgetTick=tick;budget=GLOBAL_NODES;}}
    private final LbcWtf.Chef chef;
    private final ServerLevel level;
    private final List<ItemStack> tools=List.of(new ItemStack(Items.NETHERITE_PICKAXE),new ItemStack(Items.NETHERITE_AXE),
            new ItemStack(Items.NETHERITE_SHOVEL),new ItemStack(Items.NETHERITE_HOE),new ItemStack(Items.SHEARS),new ItemStack(Items.NETHERITE_SWORD));
    private final Set<BlockPos> placed=new HashSet<>();
    private final ArrayDeque<BlockPos> path=new ArrayDeque<>();
    private BlockPos goal,mining,lastGround;
    private BlockState miningState;
    private Search search;
    private static final class Search {
        final BlockPos start,destination;
        final PriorityQueue<Node> open=new PriorityQueue<>(Comparator.comparingDouble(Node::estimate));
        final Map<BlockPos,Double> costs=new HashMap<>();
        final Node root;
        Node best;
        Search(BlockPos start,BlockPos destination,double estimate){
            this.start=start;this.destination=destination;
            root=new Node(start,0,estimate,null);best=root;open.add(root);costs.put(start,0d);
        }
    }
    private float progress;
    private LbcIndirectKills.Owner miningCause;
    private int replan,stuck;
    private int nextVoidAttempt=Integer.MIN_VALUE;
    private Vec3 previous=Vec3.ZERO;
    LbcChefBrain(LbcWtf.Chef chef){this.chef=chef;this.level=(ServerLevel)chef.level();}
    List<ItemStack> tools(){return tools.stream().map(ItemStack::copy).toList();}
    void reset(){path.clear();search=null;clearMining();replan=0;stuck=0;}
    private boolean loaded(BlockPos pos){return level.isInWorldBounds(pos)&&level.hasChunkAt(pos);}
    private boolean empty(BlockPos pos){return loaded(pos)&&level.getBlockState(pos).getCollisionShape(level,pos).isEmpty()
            &&!level.getFluidState(pos).is(FluidTags.LAVA)&&!level.getBlockState(pos).is(Blocks.FIRE);}
    private boolean support(BlockPos pos){return loaded(pos)&&!level.getBlockState(pos).getCollisionShape(level,pos).isEmpty();}
    private boolean breakable(BlockPos pos){return loaded(pos)&&level.getBlockState(pos).getDestroySpeed(level,pos)>=0
            &&!level.getBlockState(pos).is(Blocks.BEDROCK);}
    private boolean canOccupy(BlockPos pos){return loaded(pos)&&loaded(pos.above())
            &&(empty(pos)||breakable(pos))&&(empty(pos.above())||breakable(pos.above()));}
    private boolean standing(BlockPos pos){return empty(pos)&&empty(pos.above())&&support(pos.below());}
    void findSpawnSurface(){
        BlockPos found=nearestSurface(chef.blockPosition(),12);
        if(found!=null){chef.setPos(found.getX()+.5,found.getY(),found.getZ()+.5);lastGround=found;}
    }
    void returnFromVoid(){
        if(chef.tickCount<nextVoidAttempt)return;
        nextVoidAttempt=chef.tickCount+20;
        // Search the nearest resident surface above/around the fall, then the last known safe surface.
        BlockPos found=nearestSurface(chef.blockPosition(),24);
        if(found==null&&lastGround!=null&&standing(lastGround))found=lastGround;
        if(found==null)found=nearestSurface(chef.target.blockPosition(),24);
        if(found!=null){chef.setPos(found.getX()+.5,found.getY(),found.getZ()+.5);chef.setDeltaMovement(Vec3.ZERO);chef.fallDistance=0;reset();}
    }
    private BlockPos nearestSurface(BlockPos center,int radius){
        BlockPos best=null;double distance=Double.POSITIVE_INFINITY;
        // Heightmap gives one bounded column probe; descend only near its resident top.
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            BlockPos column=new BlockPos(center.getX()+dx,0,center.getZ()+dz);
            if(!level.hasChunkAt(column))continue;
            int top=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column.getX(),column.getZ());
            for(int dy=0;dy<4;dy++){
                BlockPos p=new BlockPos(column.getX(),top-dy,column.getZ());
                if(!standing(p))continue;
                double d=p.distSqr(center);if(d<distance){distance=d;best=p;}break;
            }
        }
        return best;
    }
    void tick(){
        beginTick(chef.owner.level().getServer().getTickCount());
        if(chef.getY()<level.getMinY()-8){returnFromVoid();return;}
        if(chef.onGround()&&standing(chef.blockPosition()))lastGround=chef.blockPosition();
        if(!path.isEmpty()&&mining==null&&chef.position().distanceToSqr(previous)<.0004)stuck++;else stuck=0;
        previous=chef.position();
        if(replan>0)replan--;
        BlockPos target=chef.target.blockPosition();
        if(goal==null||goal.distManhattan(target)>3||stuck>35){reset();goal=target;}
        // A jump can pass near a pillar node before its supporting block is placed.
        // Reaching the air coordinate alone must not skip that construction action.
        while(!path.isEmpty()&&support(path.peek().below())
                &&Vec3.atBottomCenterOf(path.peek()).distanceToSqr(chef.position())<.18)path.remove();
        if(path.isEmpty()&&(search!=null||replan==0)){plan(target);if(search==null)replan=10;}
        if(path.isEmpty()){chef.halt();return;}
        BlockPos next=path.peek();
        if(!loaded(next)||!canOccupy(next)){reset();chef.halt();return;}
        // Clear headroom first, using the fastest real tool and native hardness/drop rules.
        if(!empty(next.above())){mine(next.above());return;}
        if(!empty(next)){mine(next);return;}
        if(!support(next.below())){
            if(next.getY()>chef.blockPosition().getY()){
                if(next.getX()!=chef.blockPosition().getX()||next.getZ()!=chef.blockPosition().getZ()){
                    // A previously supported diagonal step lost its support after planning.
                    reset();chef.halt();return;
                }
                // Jump-pillar: place only once the body is above the replacement cell.
                chef.pillarJump();
                if(chef.getY()>=next.getY()-.05)place(next.below());
                return;
            }
            if(!place(next.below())){reset();chef.halt();return;}
        }
        clearMining();
        chef.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.NETHERITE_SWORD));
        chef.steer(Vec3.atBottomCenterOf(next),next.getY()>chef.getY()+.35||chef.horizontalCollision);
    }
    private record Node(BlockPos pos,double cost,double estimate,Node parent){}
    private void plan(BlockPos destination){
        if(budget<=0)return;
        BlockPos start=chef.blockPosition();goal=destination;
        if(search==null||!search.start.equals(start)||search.destination.distManhattan(destination)>3)
            search=new Search(start,destination,heuristic(start,destination));
        Search work=search;
        int count=0;
        boolean finished=false,capped=false;
        while(!work.open.isEmpty()&&count<NODES_PER_PLAN&&budget>0){
            // Charge stale queue entries too, so replacement nodes cannot exceed the CPU slice.
            Node n=work.open.poll();count++;budget--;
            if(n.cost()>work.costs.getOrDefault(n.pos(),Double.POSITIVE_INFINITY))continue;
            if(heuristic(n.pos(),work.destination)<heuristic(work.best.pos(),work.destination))work.best=n;
            if(n.pos().distManhattan(work.destination)<=1){work.best=n;finished=true;break;}
            for(Direction dir:Direction.Plane.HORIZONTAL){
                BlockPos flat=n.pos().relative(dir);
                for(int dy=-1;dy<=1;dy++){
                    BlockPos p=flat.offset(0,dy,0);
                    if(!canOccupy(p))continue;
                    // A diagonal ascent needs an existing step. Unsupported ascent must first pillar
                    // in the current column, then bridge horizontally from that created support.
                    if(dy!=0&&!support(p.below()))continue;
                    if(dy>0&&!empty(n.pos().above(2)))continue;
                    double c=n.cost()+1+(dy!=0?.5:0)+actionCost(p)+actionCost(p.above())+(!support(p.below())?4:0);
                    offer(work,p,c,n);
                }
            }
            BlockPos up=n.pos().above();
            if(work.destination.getY()>n.pos().getY()&&canOccupy(up)&&empty(up.above()))offer(work,up,n.cost()+6,n);
            if(work.costs.size()>=MAX_SEARCH_POSITIONS||work.open.size()>=MAX_OPEN){capped=true;break;}
        }
        boolean useful=work.best!=work.root;
        if(finished||useful||work.open.isEmpty()||capped){
            // Keep the frontier when a complete budget slice has found only a necessary detour.
            // Once a closer segment is found, execute it and replan from the actual new position.
            if(useful)for(Node n=work.best;n.parent()!=null;n=n.parent())path.addFirst(n.pos());
            search=null;
        }
    }
    private void offer(Search work,BlockPos pos,double cost,Node parent){
        if(cost>=work.costs.getOrDefault(pos,Double.POSITIVE_INFINITY))return;
        if(!work.costs.containsKey(pos)&&work.costs.size()>=MAX_SEARCH_POSITIONS)return;
        if(work.open.size()>=MAX_OPEN)return;
        work.costs.put(pos,cost);work.open.add(new Node(pos,cost,cost+heuristic(pos,work.destination),parent));
    }
    private double heuristic(BlockPos p,BlockPos target){return Math.abs(p.getX()-target.getX())+Math.abs(p.getZ()-target.getZ())+Math.abs(p.getY()-target.getY())*1.25;}
    private double actionCost(BlockPos p){return empty(p)?0:Math.max(2,level.getBlockState(p).getDestroySpeed(level,p)*2);}
    private void clearMining(){if(mining!=null)level.destroyBlockProgress(chef.getId(),mining,-1);mining=null;miningState=null;miningCause=null;progress=0;}
    private void mine(BlockPos pos){
        chef.halt();
        if(Vec3.atCenterOf(pos).distanceToSqr(chef.getEyePosition())>16||!breakable(pos)){reset();return;}
        BlockState state=level.getBlockState(pos);
        if(!pos.equals(mining)||!state.equals(miningState)){clearMining();mining=pos.immutable();miningState=state;miningCause=chef.service.gameItems().indirectKills().coldCause(chef.owner,false);}
        ItemStack tool=tools.stream().max(Comparator.comparingDouble(t->t.getDestroySpeed(state))).orElse(tools.getFirst());
        chef.setItemSlot(EquipmentSlot.MAINHAND,tool.copy());chef.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
        float hardness=state.getDestroySpeed(level,pos);
        progress+=hardness==0?1:tool.getDestroySpeed(state)/hardness/(tool.isCorrectToolForDrops(state)||!state.requiresCorrectToolForDrops()?30:100);
        level.destroyBlockProgress(chef.getId(),pos,Math.min(9,(int)(progress*10)));
        if(progress<1)return;
        boolean own=placed.remove(pos);
        var blockEntity=level.getBlockEntity(pos);
        chef.service.gameItems().indirectKills().trapChanges(level,miningCause,()->{
            chef.service.gameItems().indirectKills().supportRemoved(level,miningCause,pos);
            if(!own&&(!state.requiresCorrectToolForDrops()||tool.isCorrectToolForDrops(state)))Block.dropResources(state,level,pos,blockEntity,chef,tool);
            level.levelEvent(2001,pos,Block.getId(state));level.removeBlock(pos,false);
        });
        clearMining();
    }
    private boolean place(BlockPos pos){
        if(!loaded(pos)||Vec3.atCenterOf(pos).distanceToSqr(chef.position())>9
                ||!level.getBlockState(pos).canBeReplaced()||!level.getWorldBorder().isWithinBounds(pos))return false;
        // A bridge must attach to an existing face; a pillar may attach to its supporting block.
        boolean attached=false;for(Direction d:Direction.values())if(support(pos.relative(d))){attached=true;break;}
        if(!attached)return false;
        var state=Blocks.COBBLESTONE.defaultBlockState();
        if(!level.isUnobstructed(state,pos,net.minecraft.world.phys.shapes.CollisionContext.empty()))return false;
        chef.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.COBBLESTONE,64));chef.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
        chef.service.gameItems().indirectKills().trapChanges(level,chef.service.gameItems().indirectKills().coldCause(chef.owner,false),()->level.setBlock(pos,state,3));
        placed.add(pos.immutable());return true;
    }
}
