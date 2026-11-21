package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Real world physics, legacy public particles and the full native smoke lifetime. */
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
        int began,phase,when,expiryBeforePause,oldParticleFrames,oldParticleTotal,suspendedExpiry,puffPayloadBytes;boolean closing;
        Fixture(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);smoke=game.items().reactiveArtifacts();}
        List<?> clouds(){return (List<?>)WoolBucketChecks.read(smoke,"clouds");}
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
            bombCleanup();
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
            smoke.grenadeSmoke(f.other,carrier.position(),true);
            require(smoke.cloudCount()==2&&!shaderMarker(f.client.packets()),"old smoke remains its separate particle implementation");
            next(0);
        }
        void bombCleanup(){
            var danmaku=game.items().danmaku();
            require(danmaku.bombs().isEmpty(),"exclusive native spheres for the smoke probe");
            var center=f.owner.position();var otherPosition=f.other.position();
            var board=game.server().getScoreboard();
            var charge=game.items().mana().score(f.owner,"danmaku_score");int previousCharge=charge.get();
            var volley=game.items().mana().score(f.owner,"danmaku");int previousVolley=volley.get();
            var failures=new ArrayList<String>();
            f.move(f.other,center.add(10,0,0));
            try{
                for(boolean fast:new boolean[]{false,true}){
                    String source=fast?"Hakurei bomb":"Gohai spellcard";
                    var nearby=center.add(.75,0,0);var outside=center.add(8,0,0);
                    smoke.grenadeSmoke(f.other,nearby,false);
                    var hostile=grenadeCarrier(nearby);
                    smoke.grenadeSmoke(f.other,center.add(-.75,0,0),true);
                    smoke.grenadeSmoke(f.other,outside,false);
                    var remote=grenadeCarrier(outside);
                    var ordinary=new AreaEffectCloud(EntityTypes.AREA_EFFECT_CLOUD,f.level);
                    ordinary.setPos(center.add(0,0,.5));ordinary.setRadius(1);
                    require(f.level.addFreshEntity(ordinary),"ordinary native effect cloud inside the sphere");
                    f.own(ordinary);
                    try{
                        var blindness=f.owner.getEffect(MobEffects.BLINDNESS);
                        require(smoke.cloudCount()==3&&blindness!=null&&blindness.getAmplifier()==1,
                                "actual hostile landed sources apply Blindness II before the sphere");
                        var item=f.loot(fast?"lbc:bomb_item":"lbc:ported/danmaku_staff");
                        f.owner.setItemInHand(InteractionHand.MAIN_HAND,item);
                        f.owner.setShiftKeyDown(!fast);charge.set(fast?0:100);
                        require(f.owner.gameMode.useItem(f.owner,f.level,item,InteractionHand.MAIN_HAND).consumesAction(),
                                "actual native "+source+" activates while blind");
                        var bomb=danmaku.bombs().getFirst();
                        require(bomb.fast==fast&&bomb.display.position().equals(center),"exact native "+source+" origin");
                        for(int tick=0;tick<10&&bomb.radius()<2;tick++)danmaku.tick();
                        require(bomb.radius()==2&&!remote.isRemoved()&&!ordinary.isRemoved(),
                                "only the radius-two sphere reaches both hostile near emitters");
                        smoke.tick(); // Production runs this later in the same game tick.
                        if(!hostile.isRemoved()||smoke.cloudCount()!=1||f.owner.hasEffect(MobEffects.BLINDNESS))
                            failures.add(source+" carrierRemoved="+hostile.isRemoved()+" clouds="+smoke.cloudCount()
                                    +" blindness="+f.owner.hasEffect(MobEffects.BLINDNESS));

                        int before=smoke.cloudCount();
                        smoke.grenadeSmoke(f.owner,center.add(0,0,-.75),false);
                        var own=grenadeCarrier(center.add(0,0,-.75));
                        var allies=board.addPlayerTeam("smoke_purge_fixture");
                        try{
                            board.addPlayerToTeam(f.owner.getScoreboardName(),allies);
                            board.addPlayerToTeam(f.other.getScoreboardName(),allies);
                            smoke.grenadeSmoke(f.other,center.add(0,0,.75),true);
                            danmaku.tick();smoke.tick();
                            require(!own.isRemoved()&&!remote.isRemoved()&&!ordinary.isRemoved()&&smoke.cloudCount()==before+2,
                                    "own and allied landed smoke survive the same active sphere");
                        }finally{board.removePlayerTeam(allies);}
                    }finally{
                        ordinary.discard();danmaku.forget(f.owner);smoke.stop();
                        f.owner.setShiftKeyDown(false);f.owner.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
                    }
                }
                require(failures.isEmpty(),"sphere cleanup of landed modern and old smoke: "+failures);
            }finally{
                charge.set(previousCharge);volley.set(previousVolley);f.move(f.other,otherPosition);
            }
        }
        ItemEntity grenadeCarrier(Vec3 at){
            var model=Identifier.parse("lbc:smokegrenade");
            var matches=f.level.getEntitiesOfClass(ItemEntity.class,new AABB(at,at).inflate(.4),
                    e->model.equals(e.getItem().get(DataComponents.ITEM_MODEL)));
            require(matches.size()==1,"one actual visual and physical grenade carrier at "+at);
            return matches.getFirst();
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
                require(graphics.stream().anyMatch(p->SmokeVisuals.particle(p.particle())&&p.count()==100),"legacy gray wool particle batch reaches owner");
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
                var markers=flatten(f.client.packets()).stream().filter(ClientboundLevelParticlesPacket.class::isInstance)
                    .map(ClientboundLevelParticlesPacket.class::cast).filter(p->SmokeVisuals.particle(p.particle())).toList();
                require(!markers.isEmpty()&&markers.stream().anyMatch(p->new Vec3(p.x(),p.y()-2.5,p.z()).distanceToSqr(carrier.position())>.1),
                    "independent marker batches were emitted at earlier positions during movement");
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
                require(!shaderMarker(f.client.packets()),"moving smoke uses particles without fullscreen shader bodies");
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
                next(9);return false;
            }
            if(phase==9){
                if(elapsed<20)return false;
                var puffs=flatten(f.client.packets()).stream().filter(ClientboundLevelParticlesPacket.class::isInstance)
                    .map(ClientboundLevelParticlesPacket.class::cast).filter(p->SmokeVisuals.particle(p.particle())).toList();
                require(puffs.size()==elapsed,"one mature smoke packet per actual tick, not individual puff packets: "+puffs.size()+"/"+elapsed);
                for(var packet:puffs){
                    var raw=io.netty.buffer.Unpooled.buffer();
                    try{
                        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(raw,f.level.registryAccess());
                        ClientboundLevelParticlesPacket.STREAM_CODEC.encode(buffer,packet);puffPayloadBytes+=buffer.readableBytes();
                        var decoded=ClientboundLevelParticlesPacket.STREAM_CODEC.decode(buffer);
                        require(!buffer.isReadable()&&SmokeVisuals.particle(decoded.particle())&&decoded.count()==100,
                            "real wire codec retains the legacy gray wool material and count");
                        require(Math.abs(decoded.xDist()-7)<.001&&Math.abs(decoded.yDist()-4)<.001&&Math.abs(decoded.zDist()-7)<.001
                            &&Math.abs(decoded.y()-carrier.getY()-2.5)<.001,"original7/4/7 Gaussian spread andY2.5: "+decoded.xDist()+"/"+decoded.yDist()+"/"+decoded.zDist()+" dy="+(decoded.y()-carrier.getY()));
                        require(decoded.xMaxSpeed()==0&&decoded.yMaxSpeed()==0&&decoded.zMaxSpeed()==0
                            &&decoded.randomizationType()==ClientboundLevelParticlesPacket.RandomizationType.DEFAULT
                            &&decoded.overrideLimiter()&&!decoded.alwaysShow(),"source speed0/Gaussian/public depth-tested delivery");
                    }finally{raw.release();}
                }
                require(puffPayloadBytes>0&&puffPayloadBytes<=1400,"bounded one-second encoded payload, excluding protocol framing");
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
            SkyWars2.LOGGER.info("[check] PASS smoke-volume ({} actual ticks; blast motion, support loss, Gravitas isolation, TimeStop, full1200tick lifetime; legacy gray wool100/tick, spread7/4/7 and Y2.5; {} encoded payload bytes/20ticks/viewer)",now-began,puffPayloadBytes);
            return true;
        }
        public void close(){if(closing)return;closing=true;smoke.stop();f.close();}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.begin();}catch(Throwable error){running.close();running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL smoke-volume",error);}}
}
