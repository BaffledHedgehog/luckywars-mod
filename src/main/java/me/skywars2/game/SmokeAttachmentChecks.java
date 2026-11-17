package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Native launched smoke must stick, then rejoin item physics without a new lifetime. */
public final class SmokeAttachmentChecks {
    private SmokeAttachmentChecks() { }
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Smoke attachment: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final LbcReactiveArtifacts smoke;
        BlockPos origin,support;Arrow arrow;ItemEntity carrier;Object cloud;Vec3 attached;int phase,began,when,expiry;
        boolean closed;
        Fixture(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);smoke=game.items().reactiveArtifacts();}
        void begin(){
            f.prepareParticipants();began=game.server().getTickCount();
            var at=BlockPos.containing(f.home.add(0,32,0));
            origin=new BlockPos((at.getX()>>4)*16+6,at.getY(),(at.getZ()>>4)*16+6);
            for(int x=-2;x<=6;x++)for(int z=-2;z<=3;z++)for(int y=0;y<=7;y++)
                f.set(origin.offset(x,y,z),(y==0?Blocks.STONE:Blocks.AIR).defaultBlockState());
            f.move(f.owner,Vec3.atBottomCenterOf(origin.above()).add(-1,0,-1));
            f.move(f.other,Vec3.atBottomCenterOf(origin.above()).add(-1,0,2));
            support=origin.offset(4,2,0);f.set(support,Blocks.STONE.defaultBlockState());
            launch(Vec3.atCenterOf(origin.offset(0,2,0)),new Vec3(2,0,0));next(0);
        }
        void launch(Vec3 at,Vec3 velocity){
            var ordinary=new Arrow(EntityTypes.ARROW,f.level);
            require(eu.pb4.polymer.core.api.entity.PolymerEntity.get(ordinary)==null,"ordinary vanilla arrows retain native rendering");
            arrow=game.items().hedgehogSpikes().emitLegacy(f.owner,7,at,velocity);
            require(arrow!=null,"actual native smoke arrow launched");
            var polymer=eu.pb4.polymer.core.api.entity.PolymerEntity.get(arrow);
            require(polymer!=null&&polymer.getPolymerEntityType(f.owner.connection.getPacketContext())==EntityTypes.MARKER,
                "flying smoke transport never asks the vanilla client to render an arrow");
            require(WoolBucketChecks.read(arrow,"display")!=null,"authored grenade item display is attached");
        }
        void next(int value){phase=value;when=game.server().getTickCount();}
        boolean acquired(){
            var clouds=(List<?>)WoolBucketChecks.read(smoke,"clouds");
            if(clouds.isEmpty())return false;
            require(clouds.size()==1&&arrow.isRemoved(),"one payload and retired native flight");
            cloud=clouds.getFirst();carrier=(ItemEntity)WoolBucketChecks.read(cloud,"carrier");
            require(carrier!=null&&!carrier.isRemoved()&&carrier.isNoGravity(),"attached item holds its hit face");
            require(support.equals(WoolBucketChecks.read(carrier,"support")),"attachment is the actual native collided block");
            require(LbcIndirectKills.playerSource(carrier)==null,"placed smoke is an unowned world source");
            attached=carrier.position();expiry=(Integer)WoolBucketChecks.read(cloud,"expires");return true;
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();int now=game.server().getTickCount(),elapsed=now-when;
            require(now-began<160,"bounded native attachment scene");
            if(phase==0||phase==3){
                if(!acquired()){require(elapsed<12,"native projectile reached the actual wall/ceiling");return false;}
                require(phase==0?attached.x<support.getX():attached.y+carrier.getBbHeight()<support.getY(),"body stays outside its hit face");
                next(phase+1);return false;
            }
            if(phase==1){
                require(carrier.isNoGravity()&&carrier.position().distanceToSqr(attached)<1e-8&&carrier.getDeltaMovement().lengthSqr()<1e-8,
                    "wall attachment stays fixed without accumulating downward speed");
                if(elapsed<8)return false;
                f.set(support,Blocks.AIR.defaultBlockState());next(2);return false;
            }
            if(phase==2){
                if(elapsed<4)return false;
                require(!carrier.isNoGravity()&&carrier.getY()<attached.y-.1,"breaking support releases native gravity");
                require((Integer)WoolBucketChecks.read(cloud,"expires")==expiry,"release does not renew the smoke lifetime");
                smoke.stop();support=origin.offset(0,6,0);f.set(support,Blocks.STONE.defaultBlockState());
                launch(Vec3.atCenterOf(origin.offset(0,2,0)),new Vec3(0,2.5,0));next(3);return false;
            }
            if(phase==4){
                require(carrier.isNoGravity()&&carrier.position().distanceToSqr(attached)<1e-8,"ceiling attachment is retained");
                if(elapsed<8)return false;
                carrier.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);next(5);return false;
            }
            if(phase==5){
                if(elapsed<4)return false;
                var previous=carrier;carrier=(ItemEntity)WoolBucketChecks.read(cloud,"carrier");
                require(carrier!=previous&&carrier.isNoGravity()&&carrier.position().distanceToSqr(attached)<1e-8,
                    "transient native unload/resume retains attachment");
                require((Integer)WoolBucketChecks.read(cloud,"expires")==expiry,"resume retains the original deadline");
                f.level.explode(null,attached.x-1,attached.y,attached.z,1f,Level.ExplosionInteraction.NONE);
                require(carrier.getDeltaMovement().lengthSqr()>.01,"real explosion supplies a native release impulse");next(6);return false;
            }
            if(elapsed<8)return false;
            require(!carrier.isNoGravity()&&carrier.position().distanceToSqr(attached)>.1,"blast releases and moves the actual grenade");
            require(((Vec3)WoolBucketChecks.read(cloud,"center")).distanceToSqr(carrier.position())<1e-8,"smoke follows the released physical source");
            require((Integer)WoolBucketChecks.read(cloud,"expires")==expiry,"blast does not restart the timer");
            SkyWars2.LOGGER.info("[check] PASS smoke-attachment (native wall/ceiling flights, hidden arrow and grenade display, support loss, unload/resume, real blast release and original lifetime)");return true;
        }
        public void close(){if(closed)return;closed=true;smoke.stop();if(arrow!=null&&!arrow.isRemoved())arrow.discard();f.close();}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.begin();}catch(Throwable error){running.close();running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL smoke-attachment",error);}}
}
