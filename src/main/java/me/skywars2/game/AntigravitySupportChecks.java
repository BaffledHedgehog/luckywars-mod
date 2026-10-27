package me.skywars2.game;

import io.netty.buffer.Unpooled;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;

/** Native listener and real motion-packet checks. The bounded motion model is not a physical client. */
public final class AntigravitySupportChecks {
    private AntigravitySupportChecks() { }
    private static void require(boolean value,String why) {
        if(!value)throw new IllegalStateException("Antigravity support: "+why);
    }
    private static void near(double actual,double expected,String why) {
        require(Math.abs(actual-expected)<1e-7,why+": "+actual+" expected "+expected);
    }
    private static final class Probe {
        final AllGrimoireChecks.Fixture f;
        final LbcAntigravity service;
        final BlockPos center;
        Probe(AllGrimoireChecks.Fixture f) {
            this.f=f;service=f.base.game.items().antigravity();center=BlockPos.containing(f.at).below();
        }
        void stage(double hover) {stage(hover,false);}
        void stage(double hover,boolean stacked) {stage(hover,stacked,0);}
        void stage(double hover,boolean stacked,int latency) {
            service.stop();f.base.game.items().frost().clear(f.base.owner);f.base.owner.removeAllEffects();
            for(var pos:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,4,1)))
                f.level.setBlock(pos,Blocks.AIR.defaultBlockState(),2|16|32);
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
                f.level.setBlock(center.offset(x,0,z),Blocks.STONE.defaultBlockState(),2|16|32);
            if(stacked)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
                f.level.setBlock(center.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2|16|32);
            var p=f.base.owner;
            p.setPos(center.getX()+.5,center.getY()+1+hover,center.getZ()+.5);
            p.setYRot(73);p.setXRot(-21);p.setNoGravity(false);p.setOnGround(hover==0);
            p.setDeltaMovement(new Vec3(.17,0,-.11));p.setKnownMovement(Vec3.ZERO);p.resetFallDistance();
            f.beta.setPos(center.getX()+12,center.getY()+1,center.getZ()+12);
            p.connection=new ServerGamePacketListenerImpl(f.base.game.server(),f.base.recording,p,
                    new CommonListenerCookie(p.getGameProfile(),latency,p.clientInformation(),false));
            p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            p.connection.handlePlayerInput(new ServerboundPlayerInputPacket(Input.EMPTY));p.connection.resetPosition();
            var token=new ItemEntity(f.level,center.getX()+.5,center.getY()+.5,center.getZ()+.5,
                    new ItemStack(LbcAntigravityDropItem.Kind.NORMAL.item()));
            token.setThrower(p);token.setNoGravity(true);token.setPickUpDelay(1000);
            require(f.level.addFreshEntity(token),"native token admission");
            try {require(service.spawn(token),"resident service admission");}finally{token.discard();}
            require(service.snapshot().size()==(stacked?18:9),"one connected flat or stacked3x3 platform");
            f.base.recording.packets.clear();
        }
        double top() {return service.snapshot().stream().mapToDouble(d->d.getY()+1).max().orElseThrow();}
        Object listenerState(String name) {
            try {var field=ServerGamePacketListenerImpl.class.getDeclaredField(name);field.setAccessible(true);return field.get(f.base.owner.connection);}
            catch(ReflectiveOperationException error){throw new IllegalStateException("native listener state "+name,error);}
        }
        void noCorrection() {
            require(f.base.recording.packets.stream().noneMatch(ClientboundPlayerPositionPacket.class::isInstance),
                    "no player-position packet at all, including negative teleport IDs");
            require(f.base.recording.packets.stream().noneMatch(packet->packet instanceof ClientboundTeleportEntityPacket entity&&entity.id()==f.base.owner.getId()),
                    "no native self entity interpolation");
            require(listenerState("awaitingPositionFromClient")==null,"velocity carry never opens teleport acknowledgement lock");
        }
        Vec3 wire() {
            var p=f.base.owner;
            var sent=f.base.recording.packets.stream().filter(ClientboundSetEntityMotionPacket.class::isInstance)
                    .map(ClientboundSetEntityMotionPacket.class::cast).filter(packet->packet.id()==p.getId()).toList();
            require(!sent.isEmpty(),"carry sends an actual native self motion packet");
            var bytes=new FriendlyByteBuf(Unpooled.buffer());
            try {
                ClientboundSetEntityMotionPacket.STREAM_CODEC.encode(bytes,sent.getLast());
                var packet=ClientboundSetEntityMotionPacket.STREAM_CODEC.decode(bytes);var velocity=packet.movement();
                require(packet.id()==p.getId()&&Double.isFinite(velocity.x)&&Double.isFinite(velocity.y)
                        &&Double.isFinite(velocity.z)&&velocity.y>0,"wire contains finite positive vertical velocity for this player");
                require(velocity.distanceTo(p.getDeltaMovement())<.002,"wire carries applied server velocity within codec precision");
                return velocity;
            }finally{bytes.release();}
        }
        Vec3 step() {
            var p=f.base.owner;var before=p.position();
            float yaw=p.getYRot(),pitch=p.getXRot();p.fallDistance=19;
            f.base.recording.packets.clear();service.tick();
            near(p.position().distanceToSqr(before),0,"service tick changes no player server coordinates");
            near(p.getYRot(),yaw,"carry preserves yaw");near(p.getXRot(),pitch,"carry preserves pitch");
            require(p.fallDistance==0&&service.supported(p),"supported player has no falling debt");
            require(!p.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING),"normal rider acquires no hidden slowfall");
            require(!p.onGround(),"velocity ascent retains native airborne state");
            noCorrection();return wire();
        }
        void unchanged(ServerboundMovePlayerPacket packet) {
            require(service.supportInput(f.base.owner,packet)==packet,"support input returns exact original movement packet");
        }
        void inputIdentity() {
            stage(0,true);var velocity=step();var p=f.base.owner;
            require(velocity.x/.17>1&&velocity.x/.17<=1.5&&velocity.z/-.11>1&&velocity.z/-.11<=1.5,
                    "initial horizontal boost is modest and preserves both directions");
            for(double dy:new double[]{-.08,-1,0,.025}) {
                var input=new ServerboundMovePlayerPacket.PosRot(p.getX()+.04,p.getY()+dy,p.getZ()-.03,112,-13,true,false);
                unchanged(input);near(input.getY(0),p.getY()+dy,"stale/stacked floor Y is not rewritten");
                near(input.getX(0),p.getX()+.04,"packet X unchanged");near(input.getZ(0),p.getZ()-.03,"packet Z unchanged");
                near(input.getYRot(0),112,"packet yaw unchanged");near(input.getXRot(0),-13,"packet pitch unchanged");
            }
            unchanged(new ServerboundMovePlayerPacket.Rot(-83,37,false,false));
            double y=p.getY()-1;p.setPos(p.getX(),y,p.getZ());
            f.base.recording.packets.clear();service.tick();
            near(p.getY(),y,"stacked-floor penetration never snaps player up to captured top");noCorrection();
        }
        void nativeMove(double x,double y,double z,float yaw,float pitch,boolean ground) {
            var p=f.base.owner;var input=new ServerboundMovePlayerPacket.PosRot(x,y,z,yaw,pitch,ground,false);
            unchanged(input);p.connection.handleMovePlayer(input);
            near(p.getX(),x,"native listener accepts requested X");near(p.getY(),y,"native listener retains original Y");
            near(p.getZ(),z,"native listener accepts requested Z");
            near(p.getYRot(),yaw,"native listener accepts requested yaw");near(p.getXRot(),pitch,"native listener accepts requested pitch");
            noCorrection();p.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
        }
        void walking() {
            stage(0);var velocity=step();var p=f.base.owner;
            // Advance from the actual wire impulse. No fixture follows the display by teleporting the player.
            double y=p.getY()+velocity.y;
            nativeMove(center.getX()+1.05,y,p.getZ()+.07,112,-13,false);step();
            nativeMove(center.getX()+2.15,p.getY()+.02,p.getZ(),89,12,false);
            require(p.getX()>center.getX()+2&&p.getBoundingBox().minX<center.getX()+2,
                    "center crossed outer edge while feet still overlap");
            require(service.supported(p),"partial footprint keeps support");
            nativeMove(center.getX()+2.36,p.getY()+.01,p.getZ(),-31,23,false);
            var before=p.position();f.base.recording.packets.clear();service.tick();
            near(p.position().distanceToSqr(before),0,"whole-footprint departure changes no coordinates");
            require(!service.supported(p),"whole-footprint departure releases support");
            require(f.base.recording.packets.stream().noneMatch(packet->packet instanceof ClientboundSetEntityMotionPacket motion&&motion.id()==p.getId()),
                    "departed player receives no carry velocity");noCorrection();
        }
        void delayedLanding() {
            stage(2);var p=f.base.owner;p.setDeltaMovement(0,-.35,0);p.setOnGround(false);
            for(int tick=0;tick<3;tick++) {
                var before=p.position();f.base.recording.packets.clear();service.tick();
                near(p.position().distanceToSqr(before),0,"unadmitted falling player is never moved by service");
                require(!service.supported(p),"falling player above the platform is not admitted early");noCorrection();
            }
            require(top()>center.getY()+1,"landing target is already rising");
            double contact=top()+.01;
            nativeMove(p.getX()+.08,contact,p.getZ()-.04,-119,34,false);
            step();require(service.supported(p),"native descending contact catches an already rising piece");
        }
        private record MotionAt(int tick,Vec3 velocity) { }
        private record InputAt(int tick,Vec3 position,float yaw,float pitch) { }
        void persistentMotion(int delay) {
            stage(0,true,delay*100);var p=f.base.owner;var connection=p.connection;
            var pendingMotion=new ArrayDeque<MotionAt>();var pendingInput=new ArrayDeque<InputAt>();
            Vec3 client=p.position(),velocity=Vec3.ZERO;double startY=client.y;
            for(int tick=0;tick<100;tick++) {
                require(p.connection==connection,"one native listener persists through100 modeled ticks");
                connection.tick();noCorrection();var impulse=step();
                pendingMotion.addLast(new MotionAt(tick+delay,impulse));
                while(!pendingMotion.isEmpty()&&pendingMotion.getFirst().tick()<=tick)velocity=pendingMotion.removeFirst().velocity();
                // Native air travel moves with current motion, then applies gravity/drag.
                // X/Z steering and rotation are scripted inputs. This is a bounded open-air,
                // constant-delay motion model, not rendering, mining, or arbitrary-network proof.
                double x=center.getX()+.5+Math.sin(tick*.13)*.35,z=center.getZ()+.5+Math.cos(tick*.11)*.35;
                client=new Vec3(x,client.y+velocity.y,z);
                velocity=new Vec3(velocity.x*.91,(velocity.y-.08)*.98,velocity.z*.91);
                float yaw=(tick*31)%360-180,pitch=(float)(Math.sin(tick*.23)*60);
                pendingInput.addLast(new InputAt(tick+delay,client,yaw,pitch));
                while(!pendingInput.isEmpty()&&pendingInput.getFirst().tick()<=tick) {
                    var input=pendingInput.removeFirst();var at=input.position();
                    nativeMove(at.x,at.y,at.z,input.yaw(),input.pitch(),false);
                }
                if(tick>20)require(Math.abs(client.y-top())<=1.5+delay,
                        "bounded modeled client stays near its rising floor; delay="+delay+" tick="+tick+" gap="+(client.y-top()));
                noCorrection();
            }
            require(p.connection==connection&&service.supported(p),"native persistent listener retains support without position packets");
            require(p.getY()>startY+60&&client.y>startY+60,"repeated real wire impulses produce substantial modeled ascent");
        }
        void fastLanding() {
            stage(9);var p=f.base.owner;
            for(int tick=0;tick<5;tick++)service.tick();
            double floor=top(),reported=floor-3;
            f.base.recording.packets.clear();
            nativeMove(p.getX(),reported,p.getZ(),-87,43,false);
            near(p.getY(),reported,"fast crossing retains actual client Y below surface without snapping");
            var impulse=step();require(impulse.y>0&&service.supported(p),"fast falling report three blocks through a moving top is acquired by upward velocity");
            require(service.collisions(p,p.getBoundingBox().inflate(1),java.util.List.of()).isEmpty(),
                    "player carry adds no fake server-only walls that could reject free client input");
        }
        void platformTransfer() {
            stage(0);var p=f.base.owner;var first=step();
            nativeMove(p.getX(),p.getY()+first.y,p.getZ(),73,-21,false);
            var other=center.offset(4,0,0);
            for(var pos:BlockPos.betweenClosed(other.offset(-1,-1,-1),other.offset(1,4,1)))
                f.level.setBlock(pos,Blocks.AIR.defaultBlockState(),2|16|32);
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
                f.level.setBlock(other.offset(x,0,z),Blocks.STONE.defaultBlockState(),2|16|32);
            var token=new ItemEntity(f.level,other.getX()+.5,other.getY()+.5,other.getZ()+.5,
                    new ItemStack(LbcAntigravityDropItem.Kind.NORMAL.item()));
            token.setThrower(p);token.setNoGravity(true);token.setPickUpDelay(1000);
            require(f.level.addFreshEntity(token),"second native token admission");
            try{require(service.spawn(token),"second platform admission");}finally{token.discard();}
            require(service.snapshot().size()==18,"two independent nine-cell platforms");
            step();require(p.getDeltaMovement().y>.125,"old platform leaves positive carry velocity");
            double floor=service.snapshot().stream().filter(d->d.getX()>=center.getX()+3)
                    .mapToDouble(d->d.getY()+1).max().orElseThrow();
            nativeMove(other.getX()+.5,floor+.01,other.getZ()+.5,-97,31,false);
            require(service.supported(p),"same descending packet leaves old platform and acquires the new one");
            step();require(service.supported(p),"new landing survives retained upward velocity from previous platform");
        }
        void pendingTeleport() {
            stage(0);step();var p=f.base.owner;var connection=p.connection;
            double x=p.getX()+.2,y=p.getY()+.3,z=p.getZ()+.1;
            connection.teleport(x,y,z,127,-19);
            var pending=listenerState("awaitingPositionFromClient");int genuine=(Integer)listenerState("awaitingTeleport");
            require(pending!=null&&genuine>=0,"ordinary native teleport installs genuine awaiting state");
            var oldMotion=p.getDeltaMovement();f.base.recording.packets.clear();service.tick();
            near(p.getX(),x,"pending teleport retains X");near(p.getY(),y,"pending teleport retains Y");near(p.getZ(),z,"pending teleport retains Z");
            near(p.getDeltaMovement().distanceToSqr(oldMotion),0,"pending teleport receives no carry force");
            require(f.base.recording.packets.stream().noneMatch(packet->packet instanceof ClientboundPlayerPositionPacket
                    ||packet instanceof ClientboundTeleportEntityPacket entity&&entity.id()==p.getId()
                    ||packet instanceof ClientboundSetEntityMotionPacket motion&&motion.id()==p.getId()),"pending teleport prevents carry packets");
            require(listenerState("awaitingPositionFromClient")==pending,"carry retains exact genuine awaiting state");
            connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(genuine,x,y,z,127,-19));
            require(listenerState("awaitingPositionFromClient")==null,"ordinary genuine ACK completes native teleport");
            near(p.getX(),x,"ordinary ACK accepts X");near(p.getY(),y,"ordinary ACK accepts Y");near(p.getZ(),z,"ordinary ACK accepts Z");
            require(!service.supported(p),"genuine teleport releases support");
        }
        void guards() {
            var p=f.base.owner;
            stage(.2);double hover=p.getY();service.tick();
            near(p.getY(),hover,"hovering entity is not moved at launch");require(!service.supported(p),"hover alone is not landing contact");
            stage(0);step();
            p.connection.handlePlayerInput(new ServerboundPlayerInputPacket(new Input(false,false,false,false,true,false,false)));
            nativeMove(p.getX()+.1,p.getY()+.42,p.getZ(),51,-8,false);
            var jump=p.position();f.base.recording.packets.clear();service.tick();
            near(p.position().distanceToSqr(jump),0,"jumping player coordinates remain native");
            require(!service.supported(p),"jump detaches support");noCorrection();
            stage(0);step();
            var external=ItemEffectLevels.create(net.minecraft.world.effect.MobEffects.SLOW_FALLING,200,0);
            p.addEffect(external);service.tick();
            nativeMove(center.getX()+3,p.getY(),p.getZ(),73,-21,false);service.tick();
            var retained=p.getEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING);
            require(!service.supported(p)&&retained!=null&&retained.getDuration()==200,
                    "departure preserves genuine external slowfall rather than stripping shared effects");
            stage(0);step();double stopped=p.getY(),display=top();
            f.base.game.items().timeStop().begin(f.level,40);f.base.recording.packets.clear();
            try{service.tick();near(p.getY(),stopped,"Time Stop pauses carrier");near(top(),display,"Time Stop pauses platform");
                require(f.base.recording.packets.stream().noneMatch(packet->packet instanceof ClientboundSetEntityMotionPacket motion&&motion.id()==p.getId()),"stopped service sends no carry impulse");}
            finally{f.base.game.items().timeStop().end(f.level);}
            stage(0);step();p.entityTags().add("sin_greed_active");var greed=p.position();
            try{service.tick();near(p.position().distanceToSqr(greed),0,"Greed rejects carry");require(!service.supported(p),"Greed releases support");}
            finally{p.entityTags().remove("sin_greed_active");}
            stage(0);step();
            for(int n=0;n<10&&!f.base.game.items().frost().isFrozen(p);n++)f.base.game.items().frost().add(p,null);
            require(f.base.game.items().frost().isFrozen(p),"actual shared frost state installed");
            var frozen=p.position();var frozenMotion=p.getDeltaMovement();f.base.recording.packets.clear();
            try{service.tick();near(p.position().distanceToSqr(frozen),0,"frost holds player position");
                near(p.getDeltaMovement().distanceToSqr(frozenMotion),0,"carry cannot overwrite frost velocity");require(!service.supported(p),"frost releases support");}
            finally{f.base.game.items().frost().clear(p);}
            service.stop();require(service.displayCount()==0&&!service.supported(p),"stop clears displays and support");
        }
    }
    public static void run(GameController game) {
        try(var f=new AllGrimoireChecks.Fixture(game)) {
            var probe=new Probe(f);
            try{probe.inputIdentity();probe.walking();probe.delayedLanding();probe.fastLanding();probe.platformTransfer();probe.guards();probe.persistentMotion(0);probe.persistentMotion(2);probe.pendingTeleport();}
            finally{game.items().antigravity().stop();game.items().frost().clear(f.base.owner);f.base.owner.entityTags().remove("sin_greed_active");}
        }
        SkyWars2.LOGGER.info("[check] PASS antigravity-support");
    }
}
