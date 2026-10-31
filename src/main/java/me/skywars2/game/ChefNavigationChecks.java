package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Regression fixtures using explicit native physics steps, not elapsed server ticks. */
public final class ChefNavigationChecks {
    private ChefNavigationChecks(){}
    private static void require(boolean condition,String message){
        if(!condition)throw new IllegalStateException("Chef navigation: "+message);
    }
    private static String state(LbcWtf.Chef chef){return " pos="+chef.position()+" "+chef.brain.diagnostics();}
    private static void start(LbcWtf.Chef chef,Vec3 at){
        chef.setPos(at);chef.setDeltaMovement(Vec3.ZERO);chef.setOnGround(true);
        chef.setYRot(0);chef.setYHeadRot(0);chef.setYBodyRot(0);
    }
    private static void hit(ChefChecks.Fixture f,LbcWtf.Chef chef,net.minecraft.world.entity.animal.cow.Cow victim,String description){
        require(victim.getHealth()<20&&victim.getKillCredit()==f.owner&&chef.target==victim,
                description+" finishes with accepted fixed-victim damage credited to summoner"+state(chef));
    }
    private static void downward(ChefChecks.Fixture f){
        // Three real support layers separate the actor from a sealed chamber below.
        // Bedrock walls prevent walking around the shaft from passing as downward mining.
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<=7;y++)
            f.block(f.center.offset(x,y,z),x!=0||z!=0?Blocks.BEDROCK:y>=2&&y<=4?Blocks.STONE:Blocks.AIR);
        var victim=f.cow(f.at);var chef=f.spawn(f.owner,victim);start(chef,f.at.add(0,5,0));
        boolean fell=false;int steps;
        for(steps=0;steps<260&&!chef.isRemoved()&&(victim.getHealth()==20||chef.getY()>f.at.y+1);steps++){
            double before=chef.getY();f.advance(chef,1);fell|=chef.getY()<before-.03;
        }
        int mined=0;for(int y=2;y<=4;y++)if(f.owner.level().getBlockState(f.center.above(y)).isAir())mined++;
        require(mined>=2&&fell&&chef.getY()<=f.at.y+1,
                "downward mining removes actual support and descends into lower chamber; mined="+mined+" steps="+steps+state(chef));
        hit(f,chef,victim,"downward mining");
    }
    private static void stableRoute(ChefChecks.Fixture f,boolean jitter){
        for(int z=-13;z<=13;z++)for(int y=0;y<=3;y++)for(int x:List.of(-3,3))f.block(f.center.offset(x,y,z),Blocks.BEDROCK);
        for(int y=0;y<=3;y++)for(int x=-2;x<=0;x++)f.block(f.center.offset(x,y,-4),Blocks.BEDROCK);
        for(int y=0;y<=3;y++)for(int x=0;x<=2;x++)f.block(f.center.offset(x,y,4),Blocks.BEDROCK);
        var unchanged=f.terrain();var target=f.at.add(0,0,11);var victim=f.cow(target);
        var chef=f.spawn(f.owner,victim);start(chef,f.at.add(0,0,-11));
        int searches=chef.brain.searchCount(),resets=chef.brain.resetCount(),steps;
        Vec3 checkpoint=chef.position();
        for(steps=0;steps<260&&!chef.isRemoved()&&victim.getHealth()==20;steps++){
            if(jitter)victim.setPos(target.add((steps%4-1.5)*.04,0,((steps/4)%4-1.5)*.04));
            f.advance(chef,1);
            if(steps>31&&steps%32==31){
                require(chef.position().distanceToSqr(checkpoint)>.25,
                        "retained route continues progressing in each 32-step window; jitter="+jitter+state(chef));
                checkpoint=chef.position();
            }
        }
        require(chef.brain.searchCount()-searches<=3&&chef.brain.resetCount()-resets<=1,
                "stationary/small-jitter goal retains route and search frontier; jitter="+jitter
                        +" searches="+(chef.brain.searchCount()-searches)+" resets="+(chef.brain.resetCount()-resets)+state(chef));
        require(f.terrain().equals(unchanged),"stable route crosses existing passages without terrain edits"+state(chef));
        hit(f,chef,victim,"stable route jitter="+jitter+" steps="+steps);
    }
    private static void noisy(ChefChecks.Fixture f){
        for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){
            int noise=Math.floorMod(x*31+z*17+x*z*7,11);var p=f.center.offset(x,0,z);
            if(noise<=2)f.block(p,Blocks.STONE);
            else if(noise<=5)f.block(p,Blocks.STONE_SLAB);
            else if(noise<=8){
                f.block(p,Blocks.STONE_STAIRS);
                var direction=List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST).get(Math.floorMod(x*3+z,4));
                f.owner.level().setBlock(p,Blocks.STONE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,direction),2);
            }else{
                f.block(p,Blocks.SNOW);
                f.owner.level().setBlock(p,Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS,1+2*Math.floorMod(x+z,4)),2);
            }
        }
        f.block(f.center.offset(-9,0,-10),Blocks.STONE);f.block(f.center.offset(9,0,10),Blocks.STONE);
        var unchanged=f.terrain();var victim=f.cow(f.at.add(9,1,10));var chef=f.spawn(f.owner,victim);
        start(chef,f.at.add(-9,1,-10));int steps;
        for(steps=0;steps<360&&!chef.isRemoved()&&victim.getHealth()==20;steps++)f.advance(chef,1);
        require(f.terrain().equals(unchanged),"dense mixed snow/slab/stair/full-block field uses native surfaces without edits"+state(chef));
        hit(f,chef,victim,"dense noisy terrain steps="+steps);
    }
    private static void gap(ChefChecks.Fixture f,int width){
        // A straight lane rules out lateral walking. Its three-block-deep trench is
        // snapshotted, and the actor must stay at the upper crossing level.
        for(int z=-7;z<=12;z++)for(int y=-3;y<=4;y++)for(int x=-1;x<=1;x++)
            f.block(f.center.offset(x,y,z),x!=0?Blocks.BEDROCK:y==-1&&(z<0||z>=width)?Blocks.STONE:Blocks.AIR);
        for(int y=-3;y<=4;y++)f.block(f.center.offset(0,y,-7),Blocks.BEDROCK);
    }
    private static void stoppedLip(ChefChecks.Fixture f){
        gap(f,4);var unchanged=f.terrain();var victim=f.cow(f.at.add(0,0,9));var chef=f.spawn(f.owner,victim);
        start(chef,f.at.add(0,0,-1.1));double retreat=0;boolean airborne=false,crossed=false;int steps;
        for(steps=0;steps<160&&!chef.isRemoved();steps++){
            var previous=chef.position();float yaw=chef.getYRot(),head=chef.getYHeadRot(),body=chef.yBodyRot;
            f.advance(chef,1);double backward=previous.z-chef.getZ();retreat+=Math.max(0,backward);
            if(backward>.01)require(Math.abs(Mth.wrapDegrees(chef.getYRot()))<=40.01,
                    "run-up retreats while still facing landing, avoiding a 180-degree turn"+state(chef));
            require(Math.abs(Mth.wrapDegrees(chef.getYRot()-yaw))<=25.01
                    &&Math.abs(Mth.wrapDegrees(chef.getYHeadRot()-head))<=25.01
                    &&Math.abs(Mth.wrapDegrees(chef.yBodyRot-body))<=25.01,"stopped-lip turns stay bounded; step="+steps
                            +" yaw="+yaw+"->"+chef.getYRot()+" head="+head+"->"+chef.getYHeadRot()
                            +" body="+body+"->"+chef.yBodyRot+state(chef));
            require(retreat<=1.2,"stopped-lip run-up cannot recursively retreat; cumulative="+retreat+state(chef));
            if(chef.getZ()>f.at.z-.5&&chef.getZ()<f.at.z+3.6)airborne|=!chef.onGround();
            require(chef.getY()>=f.at.y-.15,"four-gap crossing stays above trench"+state(chef));
            if(chef.getZ()>=f.at.z+4.2&&chef.onGround()){crossed=true;break;}
        }
        require(crossed&&airborne&&f.terrain().equals(unchanged),
                "stopped near-lip actor completes real four-block jump without construction; steps="+steps+" retreat="+retreat+state(chef));
    }
    private static void flightPlacement(ChefChecks.Fixture f,int width){
        gap(f,width);var victim=f.cow(f.at.add(0,0,width+3));var chef=f.spawn(f.owner,victim);
        start(chef,f.at.add(0,0,-4));var initial=f.terrain();Set<BlockPos> placed=new HashSet<>();
        boolean airbornePlacement=false,rising=false,falling=false,crossed=false;int steps;
        for(steps=0;steps<260&&!chef.isRemoved()&&!(crossed&&victim.getHealth()<20);steps++){
            var previous=chef.position();boolean offGround=!chef.onGround();int actions=chef.actionSerial;
            f.advance(chef,1);Vec3 movement=chef.position().subtract(previous);
            require(!chef.isNoGravity()&&movement.horizontalDistance()<=.85&&Math.abs(movement.y)<=.65,
                    width+"-gap construction uses native movement/gravity without position jumps; delta="+movement+state(chef));
            if(!chef.onGround()){rising|=movement.y>.03;falling|=movement.y<-.03;}
            for(int z=0;z<width;z++)for(int y=-2;y<=0;y++){
                var p=f.center.offset(0,y,z);
                if(f.owner.level().getBlockState(p).is(Blocks.COBBLESTONE)&&placed.add(p)){
                    require(chef.actionSerial>actions&&chef.lastActionPoint!=null&&chef.lastActionError<=2.001,
                            "new support has a native accepted, precisely aimed placement"+state(chef));
                    airbornePlacement|=offGround;
                }
            }
            require(chef.getY()>=f.at.y-.2,"flight construction maintains crossing elevation, rather than walking trench floor"+state(chef));
            if(chef.getZ()>=f.at.z+width+.2&&chef.onGround())crossed=true;
        }
        require(crossed&&rising&&falling&&airbornePlacement&&!placed.isEmpty(),
                width+"-gap is crossed with real airborne support placement and native ballistic ascent/descent; steps="+steps
                        +" crossed="+crossed+" rise="+rising+" fall="+falling+" airPlace="+airbornePlacement+" placed="+placed+state(chef));
        for(var entry:initial.entrySet())if(!placed.contains(entry.getKey()))require(f.owner.level().getBlockState(entry.getKey()).equals(entry.getValue()),
                width+"-gap edits only its actual support cells: "+entry.getKey());
        hit(f,chef,victim,width+"-gap flight construction");
    }
    private static void lipStart(ChefChecks.Fixture f,boolean corner){
        for(int x=-4;x<=4;x++)for(int z=-7;z<=10;z++)f.block(f.center.offset(x,-1,z),Blocks.AIR);
        for(int z=-5;z<=8;z++)f.block(f.center.offset(0,-1,z),Blocks.STONE);
        // A visible lower floor must not displace the adjacent upper launch cell
        // which is actually supporting the corner of the mannequin.
        if(corner)for(int x=-4;x<=4;x++)for(int z=-7;z<=10;z++)f.block(f.center.offset(x,-3,z),Blocks.STONE);
        var unchanged=f.terrain();var victim=f.cow(f.at.add(0,0,8));var chef=f.spawn(f.owner,victim);
        // The centre column is air; the native .6-wide body still overlaps the
        // supporting lane by .02 blocks. A centre-only start probe cannot find it.
        start(chef,f.at.add(.78,0,corner?-5.78:-5));Vec3 beginning=chef.position();boolean progressed=false;int steps;
        require(f.owner.level().getBlockState(chef.blockPosition().below()).isAir(),"lip fixture centre is genuinely beyond support");
        for(steps=0;steps<180&&!chef.isRemoved()&&victim.getHealth()==20;steps++){
            var previous=chef.position();f.advance(chef,1);var delta=chef.position().subtract(previous);
            require(!chef.isNoGravity()&&delta.horizontalDistance()<=.85&&Math.abs(delta.y)<=.65,
                    "lip recovery uses native travel without a rescue teleport"+state(chef));
            if(steps<24)progressed|=chef.getZ()>beginning.z+.2||chef.getX()<beginning.x-.2;
            require(chef.getY()>=f.at.y-.2,"edge-contact start recovers onto narrow support instead of falling"+state(chef));
        }
        require(progressed&&f.terrain().equals(unchanged),"absolute lip contact promptly resumes existing supported route without edits"+state(chef));
        hit(f,chef,victim,"absolute lip/corner start corner="+corner+" steps="+steps);
    }
    private static void nativeStairs(ChefChecks.Fixture f){
        for(int z=0;z<6;z++)for(int x=-1;x<=1;x++){
            for(int y=0;y<z;y++)f.block(f.center.offset(x,y,z),Blocks.STONE);
            BlockPos at=f.center.offset(x,z,z);f.block(at,Blocks.STONE_STAIRS);
            f.owner.level().setBlock(at,Blocks.STONE_STAIRS.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH),2);
        }
        for(int z=6;z<=11;z++)for(int x=-2;x<=2;x++)f.block(f.center.offset(x,5,z),Blocks.STONE);
        var terrain=f.terrain();var victim=f.cow(f.at.add(0,6,10));var chef=f.spawn(f.owner,victim);start(chef,f.at.add(0,0,-2));
        int jumps=0,steps;
        for(steps=0;steps<180&&victim.getHealth()==20;steps++){
            f.advance(chef,1);
            // Once on the upper platform, pursuit may start its ordinary
            // critical attack jump. Only the actual stair ascent forbids jumps.
            if(chef.getY()<f.at.y+5.9&&chef.getZ()<f.at.z+5.5){
                if(chef.isJumping())jumps++;
                require(chef.getDeltaMovement().y<.2,"stair travel never receives native jump launch velocity"+state(chef));
            }
        }
        require(jumps==0,"native stair half-steps climb without requested jumps; jumps="+jumps+state(chef));
        require(f.terrain().equals(terrain),"native stairs need no construction/mining");
        hit(f,chef,victim,"native stepped staircase steps="+steps);
    }
    private static void movingGoal(ChefChecks.Fixture f){
        var victim=f.cow(f.at.add(0,0,12));var chef=f.spawn(f.owner,victim);start(chef,f.at.add(0,0,-12));
        f.advance(chef,8);int resets=chef.brain.resetCount();Vec3 before=chef.position();
        victim.setPos(f.at.add(6,0,12));
        for(int i=0;i<12;i++)f.advance(chef,1);
        require(chef.brain.resetCount()==resets&&chef.position().distanceToSqr(before)>1,
                "large target movement retains usable prefix and continues native progress during replanning"+state(chef));
        for(int i=0;i<180&&victim.getHealth()==20;i++)f.advance(chef,1);
        hit(f,chef,victim,"retained prefix joins newly computed target route");
    }
    private record Case(String name,java.util.function.Consumer<ChefChecks.Fixture> action){}
    public static void run(GameController game){
        var failures=new ArrayList<String>();
        for(var test:List.of(new Case("downward mining",ChefNavigationChecks::downward),
                new Case("native stair stepping",ChefNavigationChecks::nativeStairs),new Case("moving goal continuation",ChefNavigationChecks::movingGoal),
                new Case("stationary route",f->stableRoute(f,false)),new Case("jittered route",f->stableRoute(f,true)),
                new Case("extreme noisy terrain",ChefNavigationChecks::noisy),new Case("stopped lip",ChefNavigationChecks::stoppedLip),
                new Case("flight support gap5",f->flightPlacement(f,5)),new Case("flight support gap7",f->flightPlacement(f,7)),
                new Case("absolute lip start",f->lipStart(f,false)),new Case("absolute corner start",f->lipStart(f,true)))){
            try(var fixture=new ChefChecks.Fixture(game)){fixture.init();test.action.accept(fixture);}
            catch(RuntimeException failure){failures.add(test.name+": "+failure.getMessage());}
        }
        require(failures.isEmpty(),String.join("; ",failures));
        SkyWars2.LOGGER.info("[check] PASS chef-navigation (retained routes/frontiers, downward mining, dense shape noise, bounded stopped-lip run-up, native airborne support gap5/gap7; explicit physics steps)");
    }
}
