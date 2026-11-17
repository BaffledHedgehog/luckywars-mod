package me.skywars2.game;

import com.mojang.math.Transformation;
import io.netty.buffer.Unpooled;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Real charge producers and decoded display metadata, using the native client render transform. */
public final class StaticChargeGeometryChecks {
    private StaticChargeGeometryChecks() { }
    private static Scene running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Static charge geometry: "+why);}
    private static final class Scene implements AutoCloseable {
        final GameController game;
        final WoolBucketChecks.Fixture f;
        final LbcStaticCharge service;
        final Vec3[] offsets={new Vec3(4,0,0),new Vec3(-4,0,0),new Vec3(0,0,4),new Vec3(0,0,-4),
                new Vec3(0,4,0),new Vec3(0,-4,0),new Vec3(3,2,-3)};
        final Vec3 home;
        int test,started;
        boolean inspected;
        Entity distant;
        final List<LivingEntity> extra=new ArrayList<>();
        Vec3 from,targetEye,distantEye;
        AABB targetBox,distantBox;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);service=game.items().legacyEffects().staticCharge();home=f.home.add(0,40,0);}
        int charge(Entity e){return game.items().mana().hudValue(e,LbcStaticCharge.COUNTER);}
        List<Display.ItemDisplay> beams(){return f.level.getEntitiesOfClass(Display.ItemDisplay.class,
                new AABB(home,home).inflate(64),LbcStaticCharge::visual);}
        long sounds(RegenerationBeltChecks.ScheduledClient client){return client.packets().stream().filter(ClientboundSoundPacket.class::isInstance)
                .map(ClientboundSoundPacket.class::cast).filter(p->p.getSound().value().location().toString().equals("minecraft:lbcsounds.zap")).count();}
        void begin(){f.prepareParticipants();next();}
        void next(){
            service.stop();if(distant!=null){f.discard(distant);distant=null;}
            for(var target:extra)f.discard(target);extra.clear();
            f.move(f.owner,home);f.move(f.other,home.add(test<offsets.length?offsets[test]:new Vec3(4,0,0)));
            f.owner.setHealth(1000);f.other.setHealth(1000);f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;
            f.set(BlockPos.containing(f.owner.getEyePosition().add(2,0,0)),Blocks.AIR.defaultBlockState());
            if(test==7||test==12||test==13||test==14){
                var cow=EntityTypes.COW.create(f.level,EntitySpawnReason.COMMAND);require(cow!=null,"native distant target");
                cow.setPos(home.add(test==12||test==14?16.5:test==13?16.8:20,0,0));cow.setNoAi(true);cow.setNoGravity(true);f.own(cow);
                require(f.level.addFreshEntity(cow),"distant candidate publication");distant=cow;distantBox=cow.getBoundingBox();distantEye=cow.getEyePosition();
                if(test==14)for(double x:new double[]{28.5,40.5}){
                    var beyond=EntityTypes.COW.create(f.level,EntitySpawnReason.COMMAND);require(beyond!=null,"root-range target");
                    beyond.setPos(home.add(x,0,0));beyond.setNoAi(true);beyond.setNoGravity(true);f.own(beyond);
                    require(f.level.addFreshEntity(beyond),"native root-range candidate publication");extra.add(beyond);
                }
            }
            if(test==8)f.move(f.other,home.add(11,0,0));
            if(test==10)f.move(f.other,home.add(9.9,0,0));
            if(test==11)f.move(f.other,home.add(9.8,0,0));
            if(test==9)f.set(BlockPos.containing(f.owner.getEyePosition().add(2,0,0)),Blocks.STONE.defaultBlockState());
            if(test==15)f.set(BlockPos.containing(f.owner.getEyePosition().add(2,0,0)),Blocks.WATER.defaultBlockState());
            if(test==16)f.set(BlockPos.containing(f.owner.getEyePosition().add(2,0,0)),Blocks.COBWEB.defaultBlockState());
            f.client.packets().clear();f.audience.packets().clear();from=f.owner.getEyePosition();
            targetBox=f.other.getBoundingBox();targetEye=f.other.getEyePosition();
            f.owner.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN,200,126),f.other);
            require(charge(f.owner)==10&&!f.owner.hasEffect(MobEffects.RAID_OMEN),"actual marker126 grants ten custom charges");
            require(f.owner.hurtServer(f.level,f.owner.damageSources().playerAttack(f.other),1),"native accepted external hit");
            f.owner.setDeltaMovement(Vec3.ZERO);started=game.server().getTickCount();inspected=false;
        }
        Vec3[] decodedEndpoints(Display.ItemDisplay beam){
            var wire=new RegistryFriendlyByteBuf(Unpooled.buffer(),f.owner.registryAccess());
            var copy=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,f.level);
            try{
                var add=new ClientboundAddEntityPacket(beam.getId(),beam.getUUID(),beam.getX(),beam.getY(),beam.getZ(),
                        beam.getXRot(),beam.getYRot(),beam.getType(),0,beam.getDeltaMovement(),beam.getYHeadRot());
                ClientboundAddEntityPacket.STREAM_CODEC.encode(wire,add);
                copy.recreateFromPacket(ClientboundAddEntityPacket.STREAM_CODEC.decode(wire));wire.clear();
                var values=Objects.requireNonNull(beam.getEntityData().getNonDefaultValues());
                ClientboundSetEntityDataPacket.STREAM_CODEC.encode(wire,new ClientboundSetEntityDataPacket(beam.getId(),values));
                copy.getEntityData().assignValues(ClientboundSetEntityDataPacket.STREAM_CODEC.decode(wire).packedItems());
                var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,f.owner.registryAccess());copy.saveWithoutId(output);
                var pose=output.buildResult().read("transformation",Transformation.EXTENDED_CODEC).orElseThrow();
                // DisplayRenderer FIXED rotation, display transformation, ItemDisplayRenderer Y+PI,
                // ItemTransform.NONE centering, and the authored model's z endpoints (-8..8)/16.
                var matrix=new Matrix4f().rotate(new Quaternionf().rotationYXZ((float)Math.toRadians(-copy.getYRot()),
                        (float)Math.toRadians(copy.getXRot()),0)).mul(pose.getMatrix()).rotateY((float)Math.PI).translate(-.5f,-.5f,-.5f);
                var a=matrix.transformPosition(new Vector3f(.5f,.5f,.5f));
                var b=matrix.transformPosition(new Vector3f(.5f,.5f,-.5f));
                return new Vec3[]{copy.position().add(a.x,a.y,a.z),copy.position().add(b.x,b.y,b.z)};
            }finally{wire.release();}
        }
        boolean starts(Display.ItemDisplay beam,Vec3 origin){var e=decodedEndpoints(beam);return e[0].distanceTo(origin)<.0001||e[1].distanceTo(origin)<.0001;}
        Vec3 impact(Display.ItemDisplay beam,Vec3 origin){
            var ends=decodedEndpoints(beam);
            Vec3 impact;
            if(ends[0].distanceTo(origin)<.0001)impact=ends[1];
            else{require(ends[1].distanceTo(origin)<.0001,"decoded model reaches its true source: case="+test+" from="+origin+" ends="+Arrays.toString(ends));impact=ends[0];}
            require(impact.distanceTo(origin)<=12.2501,"rendered impact never exceeds the50step legacy ray reach");return impact;
        }
        void inspect(){
            var arcs=beams();
            if(test==8||test==10){require(arcs.isEmpty()&&f.other.getHealth()==1000&&charge(f.other)==0&&charge(f.owner)==0,
                    "legacy feet-relative ten-block selector never fires outside its sphere");return;}
            require(arcs.size()==(test==14?3:test==12?2:1),"one beam per reachable impact, no out-of-reach attempts: case="+test+" arcs="+arcs.size());
            var first=arcs.stream().filter(e->starts(e,from)).findFirst().orElseThrow();var impact=impact(first,from);
            if(test==9||test==15||test==16){
                var block=BlockPos.containing(from.add(2,0,0));
                require(impact.x>=block.getX()-.0001&&impact.x<=block.getX()+.0001&&Math.abs(impact.y-from.y)<.0001,
                        "wall clips the real rendered beam at its front face");
                require(f.other.getHealth()==1000&&charge(f.other)==0,"wall prevents damage and transfer");
            }else{
                require(targetBox.inflate(.0001).contains(impact),"decoded model ends on the hit body, not behind emitter: case="+test+" impact="+impact+" expected="+targetBox);
                require(f.other.getHealth()==994&&charge(f.other)==9,"exact six damage and nine retained recipient charge");
                if(test==12||test==14){
                    var second=arcs.stream().filter(e->e!=first&&starts(e,targetEye)).findFirst().orElseThrow();
                    require(distantBox.inflate(.0001).contains(impact(second,targetEye)),"near boundary ray hits the reachable face even when target center is farther away");
                    var living=(LivingEntity)distant;require(living.getHealth()==living.getMaxHealth()-6&&charge(living)==9,"second reachable recipient gets exact damage and charge");
                    if(test==14){
                        var third=arcs.stream().filter(e->e!=first&&e!=second).findFirst().orElseThrow();
                        require(extra.getFirst().getBoundingBox().inflate(.0001).contains(impact(third,distantEye)),"last reached recipient outside root range still gets its visual and charge");
                        require(extra.getFirst().getHealth()==extra.getFirst().getMaxHealth()&&charge(extra.getFirst())==9,
                                "root selector limits damage while preserving the already transferred charge");
                        require(extra.getLast().getHealth()==extra.getLast().getMaxHealth()&&charge(extra.getLast())==0,
                                "chain stops once the original carrier is outside its20block source selector");
                    }
                }else if(distant instanceof LivingEntity living)require(living.getHealth()==living.getMaxHealth()&&charge(living)==0,"out-of-reach continuation receives no damage or charge");
            }
            require(charge(f.owner)==0,"legacy consumes original charge once, not one charge per hit");
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();int elapsed=game.server().getTickCount()-started;
            require(elapsed<24,"bounded case "+test);
            if(!inspected&&elapsed>=2){inspect();inspected=true;
                // Remove the source water before its first five-tick flow update.
                if(test==15)f.set(BlockPos.containing(from.add(2,0,0)),Blocks.AIR.defaultBlockState());}
            if(elapsed<8)return false;
            require(beams().isEmpty(),"visual expires at its active deadline");
            require(sounds(f.client)==(test==8||test==10?0:4),"legacy bounded zap pulses: case="+test+" actual="+sounds(f.client));
            if(test==12||test==14)require(sounds(f.audience)==8,"only the closer receiver hears the second four-pulse impact");
            if(++test>=18)return true;next();return false;
        }
        @Override public void close(){try{service.stop();if(distant!=null)f.discard(distant);for(var target:extra)f.discard(target);}finally{f.close();}}
    }
    public static void run(GameController game){require(running==null,"exclusive scene");var s=new Scene(game);try{s.begin();running=s;}catch(Throwable error){s.close();throw error;}}
    public static void tick(GameController game){var s=running;if(s==null)return;try{if(!s.step())return;running=null;s.close();SkyWars2.LOGGER.info("[check] PASS static-charge-geometry (18native cases; decoded endpoints7directions; real potion/hit/charge; initial and hitbox reach limits/root20; solid/water/web occlusion; four spatial zap pulses)");}catch(Throwable error){running=null;try{s.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL static-charge-geometry",error);}}
}
