package me.skywars2.game;

import me.skywars2.SkyWars2;
import me.skywars2.game.items.trinkets.LbcReactiveArtifacts;
import me.skywars2.game.presentation.effects.SmokeVisuals;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actual match finalization, including its player cleansing, must retain moving world smoke. */
public final class SmokeEndingChecks {
    private SmokeEndingChecks() { }
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Smoke ending: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final LbcReactiveArtifacts smoke;
        final boolean blocks,entities,mobs;
        ItemEntity carrier;Object cloud;Vec3 start;int began,expiry;boolean closed;
        Fixture(GameController game){
            this.game=game;f=new WoolBucketChecks.Fixture(game,false);smoke=game.items().reactiveArtifacts();
            var rules=f.level.getGameRules();blocks=rules.get(GameRules.BLOCK_DROPS);entities=rules.get(GameRules.ENTITY_DROPS);mobs=rules.get(GameRules.MOB_DROPS);
        }
        void begin(){
            f.prepareParticipants();game.config().endingSeconds=10;
            var p=BlockPos.containing(f.home.add(0,30,0));
            var at=new BlockPos((p.getX()>>4)*16+8,p.getY(),(p.getZ()>>4)*16+8);
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
                f.set(at.offset(x,0,z),Blocks.STONE.defaultBlockState());
                for(int y=1;y<=3;y++)f.set(at.offset(x,y,z),Blocks.AIR.defaultBlockState());
            }
            var position=Vec3.atBottomCenterOf(at.above());
            f.move(f.owner,position.add(-3,0,0));f.move(f.other,position.add(3,0,0));
            smoke.grenadeSmoke(f.owner,position,false);
            cloud=((List<?>)WoolBucketChecks.read(smoke,"clouds")).getFirst();
            carrier=(ItemEntity)WoolBucketChecks.read(cloud,"carrier");expiry=(Integer)WoolBucketChecks.read(cloud,"expires");
            try{var finish=GameController.class.getDeclaredMethod("finish");finish.setAccessible(true);finish.invoke(game);}
            catch(ReflectiveOperationException error){throw new IllegalStateException("real match finalization",error);}
            require(game.stage()==GameController.Stage.ENDING,"actual PLAYING -> ENDING transition");
            require(smoke.cloudCount()==1&&!carrier.isRemoved(),"real end cleansing keeps the independent physical grenade");
            smoke.clearEffects(f.owner);smoke.forget(f.owner);
            require(smoke.cloudCount()==1&&!carrier.isRemoved(),"purification and owner unload during ENDING also retain world smoke");
            // Keep the native test participants in this already resident scene during celebration.
            f.move(f.owner,position.add(-3,0,0));f.move(f.other,position.add(3,0,0));
            require(!SmokeVolumeChecks.shaderMarker(f.client.packets()),"no fullscreen smoke shader source");f.client.packets().clear();
            start=carrier.position();carrier.push(.35,.15,0);began=game.server().getTickCount();
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();int elapsed=game.server().getTickCount()-began;
            require(game.stage()==GameController.Stage.ENDING&&!carrier.isRemoved(),"world source retained throughout actual celebration");
            if(elapsed<12)return false;
            require(carrier.position().distanceToSqr(start)>.01,"physical body continues native movement during celebration");
            require(((Vec3)WoolBucketChecks.read(cloud,"center")).distanceToSqr(carrier.position())<1e-8,"actual ending item tick keeps particle source aligned");
            require(SmokeVolumeChecks.flatten(f.client.packets()).stream().filter(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class::isInstance)
                .map(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class::cast)
                .anyMatch(p->SmokeVisuals.particle(p.particle())),"observer receives public custom puffs during celebration");
            int held=(Integer)WoolBucketChecks.read(cloud,"expires")-expiry;
            require(held>=elapsed&&held<=elapsed+1,"accepted gameplay lifetime pauses during celebration while physical visuals continue");
            SkyWars2.LOGGER.info("[check] PASS smoke-ending (real finalization/cleansing/ENDING, {} native ticks, physical movement retained, public puff emission follows, graph lifetime held)",elapsed);
            return true;
        }
        public void close(){
            if(closed)return;closed=true;smoke.stop();var rules=f.level.getGameRules();
            rules.set(GameRules.BLOCK_DROPS,blocks,game.server());rules.set(GameRules.ENTITY_DROPS,entities,game.server());rules.set(GameRules.MOB_DROPS,mobs,game.server());f.close();
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.begin();}catch(Throwable error){running.close();running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL smoke-ending",error);}}
}
