package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/** Recovery regressions with native collision/travel and accepted damage, using explicit physics steps. */
public final class ChefRecoveryChecks {
    private ChefRecoveryChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Chef recovery: "+why);}
    private static String state(LbcWtf.Chef chef){return " pos="+chef.position()+" "+chef.brain.diagnostics();}
    private static void start(LbcWtf.Chef chef,Vec3 at){
        chef.setPos(at);chef.setDeltaMovement(Vec3.ZERO);chef.setOnGround(true);
        chef.setYRot(0);chef.setYHeadRot(0);chef.setYBodyRot(0);
    }
    private static void advance(ChefChecks.Fixture f,LbcWtf.Chef chef){
        Vec3 before=chef.position();f.advance(chef,1);Vec3 delta=chef.position().subtract(before);
        require(!chef.isRemoved()&&!chef.isNoGravity()&&delta.horizontalDistance()<=.85&&Math.abs(delta.y)<=1.6,
                "recovery keeps the live native body under gravity without position jumps; delta="+delta+state(chef));
    }
    private static void hit(ChefChecks.Fixture f,LbcWtf.Chef chef,Cow victim,String why){
        require(victim.getHealth()<20&&victim.getKillCredit()==f.owner&&chef.target==victim,
                why+" ends in accepted fixed-victim damage credited to summoner"+state(chef));
    }
    private static void embedded(ChefChecks.Fixture f,boolean lowCell){
        // The only exit is forward. Both side/back walls and the roof are unmineable;
        // moving to a neighbouring clear start cannot satisfy the body-cell assertions.
        for(int z=-1;z<=3;z++)for(int y=0;y<=2;y++)for(int x:List.of(-1,1))
            f.block(f.center.offset(x,y,z),Blocks.BEDROCK);
        for(int y=0;y<=2;y++)f.block(f.center.offset(0,y,-1),Blocks.BEDROCK);
        f.block(f.center.above(lowCell?1:2),Blocks.BEDROCK);
        f.block(f.center,Blocks.STONE);
        if(!lowCell)f.block(f.center.above(),Blocks.STONE);
        var victim=f.cow(f.at.add(0,0,7));var chef=f.spawn(f.owner,victim);start(chef,f.at);
        if(lowCell)chef.setPose(Pose.SWIMMING);
        require(!f.owner.level().noCollision(chef,chef.getBoundingBox()),"fixture starts inside actual "+(lowCell?"prone":"standing")+" body block(s)");
        require(!lowCell||chef.getBoundingBox().getYsize()<.61,"one-block cell has actual prone native dimensions");
        int firstFree=-1,steps;
        for(steps=0;steps<160&&victim.getHealth()==20;steps++){
            advance(f,chef);
            boolean mined=f.owner.level().getBlockState(f.center).isAir()
                    &&(lowCell||f.owner.level().getBlockState(f.center.above()).isAir());
            if(mined&&firstFree<0)firstFree=steps;
            if(chef.getZ()>f.at.z+.8)require(mined,"leaves embedded start only after mining actual obstructing body cells"+state(chef));
        }
        require(firstFree>=0&&firstFree<64&&chef.getZ()>f.at.z+1,
                "body mining promptly frees "+(lowCell?"one-block cell":"both standing body cells")+"; firstFree="+firstFree+state(chef));
        require(f.owner.level().getBlockState(f.center.above(lowCell?1:2)).is(Blocks.BEDROCK),"body escape preserves unbreakable ceiling");
        hit(f,chef,victim,"embedded lowCell="+lowCell+" steps="+steps);
    }
    private static void uphill(ChefChecks.Fixture f){
        for(int z=-5;z<=8;z++)for(int y=0;y<=3;y++)for(int x:List.of(-1,1))
            f.block(f.center.offset(x,y,z),Blocks.BEDROCK);
        for(int z=0;z<=8;z++)f.block(f.center.offset(0,0,z),Blocks.STONE);
        var terrain=f.terrain();var victim=f.cow(f.at.add(0,1,5));var chef=f.spawn(f.owner,victim);
        start(chef,f.at.add(0,0,-.8));int rose=-1,steps;
        for(steps=0;steps<140&&victim.getHealth()==20;steps++){
            advance(f,chef);if(rose<0&&chef.getY()>f.at.y+.8&&chef.getZ()>f.at.z-.4)rose=steps;
        }
        require(rose>=0&&rose<48,"front collision on one-block uphill resumes promptly; rose="+rose+state(chef));
        require(f.terrain().equals(terrain),"one-block uphill uses existing native terrain without mining/building"+state(chef));
        hit(f,chef,victim,"uphill steps="+steps);
    }
    private static void retreat(ChefChecks.Fixture f){
        var terrain=f.terrain();var victim=f.cow(f.at.add(0,0,2.15));var chef=f.spawn(f.owner,victim);start(chef,f.at);
        int steps;
        for(steps=0;steps<120&&victim.getHealth()==20;steps++){
            // Slow retreat crosses the melee edge during native preparation/critical
            // ascent. The victim is never repositioned toward the attacker.
            victim.setPos(victim.position().add(0,0,.07));advance(f,chef);
        }
        require(chef.getZ()>f.at.z+.4,"pursuit keeps closing during a slowly retreating target's melee-edge crossing"+state(chef));
        require(f.terrain().equals(terrain),"flat retreat chase never edits terrain");
        hit(f,chef,victim,"retreat steps="+steps);
    }
    private static void lowerPlatform(ChefChecks.Fixture f){
        // A seven-block cliff gives enough actual falling time for a bucket, while
        // the existing lower floor keeps every native action in restored terrain.
        for(int x=-2;x<=2;x++)for(int z=-6;z<=-1;z++)f.block(f.center.offset(x,6,z),Blocks.BEDROCK);
        var victim=f.cow(f.at.add(0,0,7));var chef=f.spawn(f.owner,victim);start(chef,f.at.add(0,7,-2));
        int departed=-1,steps;BlockPos water=null;boolean contact=false,recovered=false;
        for(steps=0;steps<240&&(victim.getHealth()==20||!recovered);steps++){
            advance(f,chef);
            if(departed<0&&chef.getY()<f.at.y+6.5)departed=steps;
            if(chef.survival.ownedWater()!=null){
                water=chef.survival.ownedWater();
                require(f.blocks.containsKey(water)&&f.owner.level().getFluidState(water).isSource(),"descent creates a real owned source inside saved bounds");
            }
            contact|=chef.isInWater();
            if(water!=null&&contact&&chef.survival.ownedWater()==null)recovered=true;
        }
        require(departed>=0&&departed<48,"far-below victim causes prompt departure from existing ledge; departed="+departed+state(chef));
        require(water!=null&&contact&&recovered&&f.owner.level().getFluidState(water).isEmpty()
                        &&chef.survival.supplies().stream().anyMatch(s->s.is(Items.WATER_BUCKET)),
                "seven-block lower-platform pursuit places native bucket water, contacts it and retrieves its source; water="+water+" contact="+contact+" recovered="+recovered+state(chef));
        hit(f,chef,victim,"lower-platform descent steps="+steps);
    }
    private static void deepFloor(ChefChecks.Fixture f){
        // No edge is within the bucket shortcut's four-block supported approach.
        // A deep lower bank must remain reachable by native mining and falling.
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)f.block(f.center.offset(x,6,z),Blocks.STONE);
        var victim=f.cow(f.at.add(0,0,5));var chef=f.spawn(f.owner,victim);start(chef,f.at.add(0,7,0));
        int departed=-1;boolean source=false,contact=false;var trace=new StringBuilder();
        for(int steps=0;steps<240&&(victim.getHealth()==20||!contact);steps++){
            advance(f,chef);
            if(departed<0&&chef.getY()<f.at.y+6.5)departed=steps;
            source|=chef.survival.ownedWater()!=null;contact|=chef.isInWater();
            if(departed>=0&&steps<departed+22)trace.append(" t=").append(steps).append(" y=").append(chef.getY()).append(" vy=").append(chef.getDeltaMovement().y)
                    .append(" fall=").append(chef.fallDistance).append(" ground=").append(chef.onGround()).append(" water=").append(chef.survival.ownedWater())
                    .append(" hand=").append(chef.getMainHandItem()).append(" pitch=").append(chef.getXRot()).append(" body=").append(chef.getPose());
        }
        require(departed>=0&&departed<80&&source&&contact,"deep floor descent mines its own real support and performs a bucket clutch; departed="+departed+" source="+source+" contact="+contact+state(chef)+trace);
        hit(f,chef,victim,"deep floor descent");
    }
    private static int nativeTickets(ChefChecks.Fixture f,net.minecraft.world.level.ChunkPos pos){
        try{
            var source=f.owner.level().getChunkSource();
            var field=source.getClass().getDeclaredField("ticketStorage");field.setAccessible(true);
            var storage=(net.minecraft.world.level.TicketStorage)field.get(source);
            var own=storage.getTickets(pos.pack()).stream().filter(t->
                    net.minecraft.resources.Identifier.parse("skywars2:chef").equals(
                            net.minecraft.core.registries.BuiltInRegistries.TICKET_TYPE.getKey(t.getType()))).toList();
            for(var ticket:own)require(ticket.getTicketLevel()==30,"native Chef ticket keeps adjacent chunks entity-ticking");
            return own.size();
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static void lifecycle(ChefChecks.Fixture f){
        var victim=f.cow(f.at.add(0,0,7));var first=f.spawn(f.owner,victim);var second=f.spawn(f.owner,victim);
        start(first,f.at);start(second,f.at);
        var original=first.chunkPosition();
        require(f.service.count()==2&&f.service.pinnedChunks()==1&&nativeTickets(f,original)==1,
                "two pursuers share one actual native simulation ticket");
        first.setPos(f.at.add(16,0,0));var crossed=first.chunkPosition();
        require(!crossed.equals(original)&&f.service.pinnedChunks()==2&&nativeTickets(f,original)==1&&nativeTickets(f,crossed)==1,
                "crossing acquires the new native ticket without releasing another actor's old ticket");
        f.service.unloaded(first);
        require(!first.isRemoved()&&f.service.count()==2&&nativeTickets(f,crossed)==1,
                "loss of tracking/accessibility never retires a live Chef or its ticket");
        first.discard();f.service.unloaded(first);
        require(f.service.count()==1&&f.service.pinnedChunks()==1&&nativeTickets(f,crossed)==0&&nativeTickets(f,original)==1,
                "actual retirement releases only its own native ticket");
        f.service.stop();
        require(second.isRemoved()&&f.service.count()==0&&f.service.pinnedChunks()==0&&nativeTickets(f,original)==0,
                "actual stop releases every owned native simulation ticket");
    }
    private static void foreignWater(ChefChecks.Fixture f,boolean blocked){
        var victim=f.cow(f.at.add(0,0,8));var chef=f.spawn(f.owner,victim);start(chef,f.at);
        try{
            var supply=LbcChefSurvival.class.getDeclaredField("bucket");supply.setAccessible(true);
            supply.set(chef.survival,new net.minecraft.world.item.ItemStack(Items.BUCKET));
        }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        BlockPos source=f.center.offset(2,0,0);f.block(source,Blocks.WATER);
        if(blocked)for(int z=-1;z<=1;z++)for(int y=0;y<=2;y++)f.block(f.center.offset(1,y,z),Blocks.BEDROCK);
        // Keep the body still: only the native survival pickup may change this source.
        for(int i=0;i<45;i++){chef.tickCount++;chef.survival.tick();}
        boolean filled=chef.survival.supplies().stream().anyMatch(stack->stack.is(Items.WATER_BUCKET));
        require(filled!=blocked,"empty bucket collects visible foreign water and rejects occluded water; blocked="+blocked+state(chef));
        require(f.owner.level().getFluidState(source).isSource()==blocked,"only reachable source changes through native pickup");
    }
    private static void verticalAimReversal(ChefChecks.Fixture f){
        var victim=f.cow(f.at.add(0,0,8));var chef=f.spawn(f.owner,victim);start(chef,f.at);
        Vec3 overhead=chef.getEyePosition().add(0,2,0),floor=chef.getEyePosition().add(0,-2,0);
        for(int i=0;i<10;i++)chef.aimAction(overhead);
        require(chef.getXRot()<-88,"roof mining preparation looks straight overhead");
        boolean ready=false;
        for(int i=0;i<10&&!ready;i++)ready=chef.aimAction(floor);
        require(ready&&chef.getXRot()>88,"camera and head reverse from mined roof to pillar support within bounded turns"+state(chef));
    }
    private static void roofEscape(ChefChecks.Fixture f){
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<=4;y++)
            if(x!=0||z!=0)f.block(f.center.offset(x,y,z),Blocks.BEDROCK);
        f.block(f.center.above(2),Blocks.STONE);f.block(f.center.above(3),Blocks.STONE);
        // The required exit is four blocks above the start. Keep the victim
        // outside native melee reach until then, so successful combat cannot
        // legitimately preempt pillar placement during the proof of escape.
        var victim=f.cow(f.at.add(0,7,0));var chef=f.spawn(f.owner,victim);start(chef,f.at);
        int steps;
        for(steps=0;steps<220&&chef.getY()<f.at.y+4;steps++){
            require(victim.getHealth()==20,"shaft fixture stays outside combat before the required escape height"+state(chef));
            advance(f,chef);
        }
        require(f.owner.level().getBlockState(f.center.above(2)).is(Blocks.COBBLESTONE)
                &&chef.getY()>=f.at.y+4,"sealed chamber mines roof then towers through its own shaft; steps="+steps+state(chef));
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(x!=0||z!=0)
            require(f.owner.level().getBlockState(f.center.offset(x,2,z)).is(Blocks.BEDROCK),"escape preserves sealed walls");
    }
    private record Case(String name,java.util.function.Consumer<ChefChecks.Fixture> action){}
    public static void run(GameController game){
        var failures=new ArrayList<String>();
        for(var test:List.of(new Case("two body blocks",f->embedded(f,false)),new Case("one-block cell",f->embedded(f,true)),
                new Case("uphill collision",ChefRecoveryChecks::uphill),new Case("retreating melee edge",ChefRecoveryChecks::retreat),
                new Case("lower platform water descent",ChefRecoveryChecks::lowerPlatform),new Case("deep floor water descent",ChefRecoveryChecks::deepFloor),new Case("visible foreign water",f->foreignWater(f,false)),new Case("occluded foreign water",f->foreignWater(f,true)),
                new Case("vertical aim reversal",ChefRecoveryChecks::verticalAimReversal),new Case("sealed roof escape",ChefRecoveryChecks::roofEscape),new Case("live unload/shared tickets",ChefRecoveryChecks::lifecycle))){
            try(var fixture=new ChefChecks.Fixture(game)){fixture.init();test.action.accept(fixture);}
            catch(RuntimeException failure){failures.add(test.name+": "+failure.getMessage());}
        }
        require(failures.isEmpty(),String.join("; ",failures));
        SkyWars2.LOGGER.info("[check] PASS chef-recovery (actual embedded-body mining, prone one-block escape, prompt uphill collision, retreating-victim accepted hit, ledge descent/native bucket placement/contact/retrieval, live tracking loss and actual native shared/crossing/retired simulation tickets; explicit physics steps)");
    }
}
