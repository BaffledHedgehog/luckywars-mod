package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real world physics, growing public particles and the full native smoke lifetime. */
public final class SmokeVolumeChecks {
    private SmokeVolumeChecks() { }
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Smoke volume: "+why);}
    static List<Packet<?>> flatten(List<Packet<?>> packets){
        var out=new ArrayList<Packet<?>>();for(var p:packets){if(p instanceof ClientboundBundlePacket b)for(var q:b.subPackets())out.addAll(flatten(List.of(q)));else out.add(p);}return out;
    }
    static boolean shaderMarker(List<Packet<?>> packets){
        return flatten(packets).stream().filter(ClientboundSetEntityDataPacket.class::isInstance)
            .map(ClientboundSetEntityDataPacket.class::cast).flatMap(p->p.packedItems().stream())
            .anyMatch(v->v.value() instanceof Integer n&&n>=0xD57700&&n<0xD57710);
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final LbcReactiveArtifacts smoke;
        ItemEntity carrier;Object cloud;BlockPos origin;Vec3 blastStart,pausePosition;
        int began,phase,when,expiryBeforePause,oldParticleFrames,oldParticleTotal,suspendedExpiry;boolean closing;
        Fixture(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);smoke=game.items().reactiveArtifacts();}
        List<?> clouds(){return (List<?>)WoolBucketChecks.read(smoke,"clouds");}
        List<?> trails(){return (List<?>)WoolBucketChecks.read(smoke,"smokeTrails");}
        int expires(){return (Integer)WoolBucketChecks.read(cloud,"expires");}
        void next(int p){phase=p;when=game.server().getTickCount();f.client.packets().clear();f.audience.packets().clear();}
        void begin(){
            f.prepareParticipants();require(smoke.cloudCount()==0,"exclusive smoke scene");
            var at=BlockPos.containing(f.home.add(0,40,0));
            origin=new BlockPos((at.getX()>>4)*16+8,at.getY(),(at.getZ()>>4)*16+8);
            for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){
                f.set(origin.offset(x,0,z),Blocks.STONE.defaultBlockState());
                f.set(origin.offset(x,-8,z),Blocks.STONE.defaultBlockState());
            }
            f.move(f.owner,Vec3.atBottomCenterOf(origin.above()).add(-3,0,0));
            f.move(f.other,Vec3.atBottomCenterOf(origin.above()).add(3,0,0));
            game.config().lbcLevelups=true;game.config().ranked=false;
            var profile=game.players().get(f.owner.getUUID());Arrays.fill(profile.levelups,0);profile.levelups[0]=30;
            profile.lbcKit=profile.lbcSkill=profile.lbcChallengeKit=0;
            game.items().mana().award(f.owner,"lbc:true_advancements/bows/shulker");game.lbc().apply(f.owner,false);
            require(game.lbc().hasLevelup(f.owner,30),"resolved real Gravitas active");
            var shot=EntityTypes.SNOWBALL.create(f.level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            require(shot!=null,"positive control factory");shot.setOwner(f.owner);shot.setPos(f.owner.getEyePosition());
            require(f.level.addFreshEntity(shot)&&shot.isNoGravity(),"owned native projectile really receives Gravitas");shot.discard();
            f.client.packets().clear();f.audience.packets().clear();
            began=game.server().getTickCount();smoke.grenadeSmoke(f.owner,Vec3.atBottomCenterOf(origin.above()),false);
            require(smoke.cloudCount()==1,"one modern emitter");cloud=clouds().getFirst();carrier=(ItemEntity)WoolBucketChecks.read(cloud,"carrier");
            require(carrier!=null&&!carrier.isNoGravity()&&!carrier.shouldBeSaved()&&LbcIndirectKills.playerSource(carrier)==null,
                "unowned native physical carrier, gravity on, no saved/item-queue pollution");
            require(expires()-began==1200&&(Double)WoolBucketChecks.read(cloud,"radius")==5,"original five-block/sixty-second gameplay");
            require(!shaderMarker(f.client.packets())&&!shaderMarker(f.audience.packets()),"no fullscreen shader carriers remain");
            require((Double)WoolBucketChecks.read(cloud,"visualRadius")<.5&&(Double)WoolBucketChecks.read(cloud,"density")<.2,"smoke starts small and sparse");
            smoke.grenadeSmoke(f.other,carrier.position(),true);
            require(smoke.cloudCount()==2&&!shaderMarker(f.client.packets()),"old smoke remains its separate particle implementation");
            next(0);
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();int now=game.server().getTickCount(),elapsed=now-when;
            require(now-began<1300,"bounded full-lifetime native fixture");
            if(phase==0){
                var graphics=flatten(f.client.packets()).stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).toList();
                int poof=graphics.stream().filter(p->p.particle()==ParticleTypes.POOF).mapToInt(ClientboundLevelParticlesPacket::count).sum();
                if(elapsed==0&&poof==0)return false;
                require(poof-oldParticleTotal==70,"exact old POOF70 each observed native frame: before="+oldParticleTotal+" now="+poof+" tick="+elapsed);
                oldParticleTotal=poof;if(++oldParticleFrames<2||elapsed<4)return false;
                require(graphics.stream().noneMatch(p->p.particle().getType()==ParticleTypes.BLOCK_MARKER),"modern block-marker spam removed");
                require(graphics.stream().anyMatch(p->SmokeVisuals.particle(p.particle())&&p.count()>0&&p.count()<=12),"bounded custom smoke puffs actually reach owner");
                require(flatten(f.audience.packets()).stream().filter(ClientboundLevelParticlesPacket.class::isInstance)
                    .map(ClientboundLevelParticlesPacket.class::cast).anyMatch(p->SmokeVisuals.particle(p.particle())),"other players see public smoke too");
                require(f.owner.getEffect(MobEffects.BLINDNESS)!=null&&f.owner.getEffect(MobEffects.BLINDNESS).getAmplifier()==1,"source native BlindnessII preserved");
                smoke.clearEffects(f.other);blastStart=carrier.position();
                f.level.explode(null,blastStart.x-1,blastStart.y,blastStart.z,1f,Level.ExplosionInteraction.NONE);
                require(!carrier.isRemoved()&&carrier.getDeltaMovement().lengthSqr()>.01,"real native explosion pushes rather than destroys grenade");
                next(1);return false;
            }
            if(phase==1){
                if(elapsed<8)return false;
                require(carrier.position().distanceToSqr(blastStart)>.25&&!carrier.isNoGravity(),"actual blast displacement despite Gravitas");
                require(((Vec3)WoolBucketChecks.read(cloud,"center")).distanceToSqr(carrier.position())<1e-8,"gameplay center follows actual physical source");
                require(!trails().isEmpty()&&trails().stream().anyMatch(p->((Vec3)WoolBucketChecks.read(p,"center")).distanceToSqr(carrier.position())>.1),"smoke remains behind, not a rigidly carried sphere");
                require(flatten(f.client.packets()).stream().filter(ClientboundLevelParticlesPacket.class::isInstance)
                    .map(ClientboundLevelParticlesPacket.class::cast).anyMatch(p->SmokeVisuals.particle(p.particle())),"moving plume emits native custom particles");
                next(2);return false;
            }
            if(phase==2){
                if(elapsed<35)return false;
                require(carrier.onGround(),"native grenade settles on original support");
                var below=BlockPos.containing(carrier.position().add(0,-.1,0));
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)f.set(below.offset(x,0,z),Blocks.AIR.defaultBlockState());
                next(3);return false;
            }
            if(phase==3){
                if(elapsed<35)return false;
                require(carrier.getY()<origin.getY()-5&&carrier.onGround()&&!carrier.isNoGravity(),"support removal causes real native fall onto lower floor");
                require(((Vec3)WoolBucketChecks.read(cloud,"center")).distanceToSqr(carrier.position())<1e-8,"fog/gameplay follow falling carrier");
                next(4);return false;
            }
            if(phase==4){
                if(elapsed<70)return false;
                require(trails().isEmpty()&&!shaderMarker(f.client.packets()),"motion plume dissipates without fullscreen shader bodies");
                require(Math.abs((Double)WoolBucketChecks.read(cloud,"visualRadius")-7.5)<.05,"smoke grows to full7.5block horizontal radius");
                smoke.clearEffects(f.owner);smoke.forget(f.owner);
                require(!carrier.isRemoved()&&smoke.cloudCount()==1,"owner purification/logout cleanup does not erase independent physical smoke");
                require(!carrier.canUsePortal(false),"transient arena emitter cannot leave orphan unlimited items through a portal");
                suspendedExpiry=expires();carrier.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                next(8);return false;
            }
            if(phase==8){
                if(elapsed<8)return false;
                var prior=carrier;carrier=(ItemEntity)WoolBucketChecks.read(cloud,"carrier");
                require(prior!=carrier&&prior.isRemoved()&&!carrier.isRemoved()&&expires()==suspendedExpiry,
                    "native unload/resume recreates transient body exactly once without resetting lifetime");
                require(!shaderMarker(f.client.packets()),"unload/resume does not recreate shader carriers");
                require(f.level.getEntitiesOfClass(ItemEntity.class,carrier.getBoundingBox().inflate(1),e->e.getClass()==carrier.getClass()).size()==1,"no duplicated physical body after resume");
                next(5);return false;
            }
            if(phase==5){
                if(elapsed<20)return false;
                require(flatten(f.client.packets()).stream().filter(ClientboundLevelParticlesPacket.class::isInstance)
                    .map(ClientboundLevelParticlesPacket.class::cast).anyMatch(p->SmokeVisuals.particle(p.particle())&&p.count()<=12),"stationary cloud retains bounded puff emission");
                expiryBeforePause=expires();pausePosition=carrier.position();f.pause();next(6);return false;
            }
            if(phase==6){
                if(elapsed<20)return false;
                require(carrier.position().distanceToSqr(pausePosition)<1e-8,"TimeStop freezes actual physical grenade");
                require(expires()-expiryBeforePause>=19&&expires()-expiryBeforePause<=21,"native smoke lifetime pauses with time");
                f.thaw();next(7);return false;
            }
            if(now<expires()){
                require(smoke.cloudCount()==1&&!carrier.isRemoved(),"full original lifetime, not an early visual timeout");
                f.client.packets().clear();f.audience.packets().clear();return false;
            }
            require(smoke.cloudCount()==0&&carrier.isRemoved()&&!shaderMarker(f.client.packets()),"actual native deadline removes physical and visual source");
            require(flatten(f.client.packets()).stream().anyMatch(p->p instanceof ClientboundRemoveEntitiesPacket r&&r.entityIds().contains(carrier.getId())),"client receives actual physical source removal");
            SkyWars2.LOGGER.info("[check] PASS smoke-volume ({} actual ticks; real explosion, support-loss fall, resolved Gravitas isolation, dissipating trail, bounded stationary puff emission, TimeStop/full1200tick lifetime, bounded public custom particles and unchanged old particles)",now-began);
            return true;
        }
        public void close(){if(closing)return;closing=true;smoke.stop();f.close();}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.begin();}catch(Throwable error){running.close();running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL smoke-volume",error);}}
}
