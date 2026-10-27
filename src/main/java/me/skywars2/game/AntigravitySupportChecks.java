package me.skywars2.game;

import io.netty.buffer.Unpooled;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.Set;
import java.util.ArrayList;

/** Bounded native support, movement-input and wire-contract checks; no client pixels are inferred. */
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
        void stage(double hover,boolean stacked) {
            service.stop();f.base.owner.removeAllEffects();
            for(var pos:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,4,1)))
                f.level.setBlock(pos,Blocks.AIR.defaultBlockState(),2|16|32);
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
                f.level.setBlock(center.offset(x,0,z),Blocks.STONE.defaultBlockState(),2|16|32);
            if(stacked)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
                f.level.setBlock(center.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2|16|32);
            var p=f.base.owner;
            p.setPos(center.getX()+.5,center.getY()+1+hover,center.getZ()+.5);
            p.setYRot(73);p.setXRot(-21);p.setNoGravity(true);p.setOnGround(hover==0);
            p.setDeltaMovement(new Vec3(.17,0,-.11));p.resetFallDistance();
            f.beta.setPos(center.getX()+12,center.getY()+1,center.getZ()+12);
            listener();
            var token=new ItemEntity(f.level,center.getX()+.5,center.getY()+.5,center.getZ()+.5,
                    new ItemStack(LbcAntigravityDropItem.Kind.NORMAL.item()));
            token.setThrower(p);token.setNoGravity(true);token.setPickUpDelay(1000);
            require(f.level.addFreshEntity(token),"native token admission");
            try {require(service.spawn(token),"resident service admission");}finally{token.discard();}
            require(service.snapshot().size()==(stacked?18:9),"one connected flat or stacked3x3 platform");
            f.base.recording.packets.clear();
        }
        void listener() {
            // Each staged fixture starts one loaded listener. The persistent transport loop below
            // retains it across100 real ticks and emits native client-tick boundaries.
            var p=f.base.owner;
            p.connection=new ServerGamePacketListenerImpl(f.base.game.server(),f.base.recording,p,
                    new CommonListenerCookie(p.getGameProfile(),0,p.clientInformation(),false));
            p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            p.connection.resetPosition();
        }
        double top() {return service.snapshot().stream().mapToDouble(d->d.getY()+1).max().orElseThrow();}
        Object listenerState(String name) {
            try {var field=ServerGamePacketListenerImpl.class.getDeclaredField(name);field.setAccessible(true);return field.get(f.base.owner.connection);}
            catch(ReflectiveOperationException error){throw new IllegalStateException("native listener state "+name,error);}
        }
        void noCorrection() {
            require(f.base.recording.packets.stream().filter(ClientboundPlayerPositionPacket.class::isInstance)
                            .map(ClientboundPlayerPositionPacket.class::cast).allMatch(packet->packet.id()==Integer.MIN_VALUE),
                    "carry/native movement never emits a genuine player teleport correction");
            require(f.base.recording.packets.stream().noneMatch(packet->packet instanceof ClientboundTeleportEntityPacket entity&&entity.id()==f.base.owner.getId()),
                    "carry never starts native self entity interpolation");
            require(listenerState("awaitingPositionFromClient")==null,"carry never opens player teleport acknowledgement lock");
        }
        void step() {
            var p=f.base.owner;var before=p.position();var motion=p.getDeltaMovement();
            float yaw=p.getYRot(),pitch=p.getXRot();p.fallDistance=19;
            f.base.recording.packets.clear();service.tick();
            near(p.getY(),top(),"player feet match rising display top");
            near(p.getX(),before.x,"carry preserves X");near(p.getZ(),before.z,"carry preserves Z");
            near(p.getDeltaMovement().x,motion.x,"carry preserves horizontal X momentum");
            near(p.getDeltaMovement().z,motion.z,"carry preserves horizontal Z momentum");
            near(p.getYRot(),yaw,"carry preserves yaw");near(p.getXRot(),pitch,"carry preserves pitch");
            require(p.onGround()&&p.fallDistance==0&&service.supported(p),"grounded supported player has no falling debt");
            noCorrection();wire();
        }
        void wire() {
            var p=f.base.owner;
            var sent=f.base.recording.packets.stream().filter(ClientboundPlayerPositionPacket.class::isInstance)
                    .map(ClientboundPlayerPositionPacket.class::cast).filter(packet->packet.id()==Integer.MIN_VALUE).toList();
            require(sent.size()==1,"exactly one vertical self transport per carry step");
            var bytes=new FriendlyByteBuf(Unpooled.buffer());
            try {
                ClientboundPlayerPositionPacket.STREAM_CODEC.encode(bytes,sent.getFirst());
                var packet=ClientboundPlayerPositionPacket.STREAM_CODEC.decode(bytes);
                require(packet.id()==Integer.MIN_VALUE&&packet.relatives().equals(Set.of(Relative.X,Relative.Z,Relative.Y_ROT,
                        Relative.X_ROT,Relative.DELTA_X,Relative.DELTA_Z)),"wire flags isolate absolute Y and zero vertical velocity");
                for(int n=0;n<3;n++) {
                    var client=new PositionMoveRotation(new Vec3(p.getX()+n*.37,p.getY()-4,p.getZ()-n*.29),
                            new Vec3(.1+n*.13,-.7+n*.2,-.15-n*.11),-121+n*77,35-n*17);
                    var actual=PositionMoveRotation.calculateAbsolute(client,packet.change(),packet.relatives());
                    near(actual.position().x,client.position().x,"wire retains newer client X");
                    near(actual.position().z,client.position().z,"wire retains newer client Z");
                    near(actual.position().y,p.getY(),"wire transports only vertical position");
                    near(actual.deltaMovement().x,client.deltaMovement().x,"wire retains client X momentum");
                    near(actual.deltaMovement().z,client.deltaMovement().z,"wire retains client Z momentum");
                    near(actual.deltaMovement().y,0,"wire clears vertical momentum");
                    near(actual.yRot(),client.yRot(),"wire retains newer client yaw");
                    near(actual.xRot(),client.xRot(),"wire retains newer client pitch");
                }
            }finally{bytes.release();}
        }
        void walk(double x) {
            var p=f.base.owner;double y=p.getY(),z=p.getZ()+.07;listener();
            f.base.recording.packets.clear();
            p.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(x,y-.08,z,112,-13,true,false));
            near(p.getX(),x,"actual native positional input retains requested horizontal movement");
            near(p.getZ(),z,"native positional input preserves requested strafe Z");
            near(p.getY(),y,"stale native input cannot fall through the rising floor");
            near(p.getYRot(),112,"native input retains requested yaw");near(p.getXRot(),-13,"native input retains requested pitch");
            noCorrection();
        }
        void walking() {
            stage(0);step();var p=f.base.owner;
            double y=p.getY();p.move(MoverType.SELF,new Vec3(0,-.5,0));
            near(p.getY(),y,"actual native downward collision clamps to display platform");
            for(double x:new double[]{1.05,1.55,2.15}) {walk(center.getX()+x);step();}
            require(p.getX()>center.getX()+2&&p.getBoundingBox().minX<center.getX()+2,
                    "center has crossed outer edge while feet still overlap");
            // Entire feet footprint leaves the union. Below-floor Y must now remain native input.
            listener();f.base.recording.packets.clear();
            double requestedY=p.getY()-.08;
            p.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(center.getX()+2.36,requestedY,p.getZ(),89,12,false,false));
            near(p.getX(),center.getX()+2.36,"native final step leaves whole platform");
            near(p.getY(),requestedY,"fully departed feet retain original downward input");
            double off=p.getY();service.tick();near(p.getY(),off,"fully departed feet stop vertical carry");
            require(!service.supported(p),"full departure releases support");noCorrection();
        }
        void persistentTransport() {
            stage(0,true);var p=f.base.owner;var connection=p.connection;
            var delayed=new ArrayList<ServerboundAcceptTeleportationPacket>();
            for(int tick=0;tick<100;tick++) {
                require(p.connection==connection,"one native listener persists across100 movement ticks");
                connection.tick();
                f.base.recording.packets.clear();service.tick();
                near(p.getBoundingBox().minY,top(),"persistent native tick feet match highest moving floor "+tick);
                noCorrection();wire();
                double floor=top(),x=center.getX()+.5+Math.sin(tick*.19)*.6,z=center.getZ()+.5+Math.cos(tick*.17)*.6;
                float yaw=(float)((tick*31)%360-180),pitch=(float)(Math.sin(tick*.23)*60);
                double staleY=floor-(tick%6)*.25;
                if(tick%2==0)connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(x,staleY,z,yaw,pitch,true,false));
                else {
                    connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(x,staleY,z,true,false));
                    connection.handleMovePlayer(new ServerboundMovePlayerPacket.Rot(yaw,pitch,true,false));
                }
                near(p.getX(),x,"persistent native movement accepts free X "+tick);
                near(p.getZ(),z,"persistent native movement accepts free Z "+tick);
                near(p.getBoundingBox().minY,floor,"stale clientY up to1.25 cannot select the lower stacked layer "+tick);
                near(p.getYRot(),yaw,"persistent native movement accepts free yaw "+tick);
                near(p.getXRot(),pitch,"persistent native movement accepts free pitch "+tick);
                delayed.add(new ServerboundAcceptTeleportationPacket(Integer.MIN_VALUE,x,floor,z,yaw,pitch));
                if(tick%7==6) {
                    var position=p.position();float oldYaw=p.getYRot(),oldPitch=p.getXRot();
                    for(int n=delayed.size()-1;n>=0;n--)connection.handleAcceptTeleportPacket(delayed.get(n));
                    delayed.clear();
                    near(p.position().distanceToSqr(position),0,"out-of-order delayed raw lift ACKs never move the player");
                    near(p.getYRot(),oldYaw,"raw ACKs preserve current yaw");near(p.getXRot(),oldPitch,"raw ACKs preserve current pitch");
                }
                noCorrection();connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
            }
            for(int n=delayed.size()-1;n>=0;n--)connection.handleAcceptTeleportPacket(delayed.get(n));
            require(p.connection==connection&&service.supported(p),"persistent native transport retains one active listener and floor");noCorrection();
            // Penetration is a server-state disturbance as well as stale packet input. The captured
            // top must survive a whole-block overlap with the lower cell in the same platform.
            double highest=top();p.setPos(p.getX(),highest-1,p.getZ());
            f.base.recording.packets.clear();service.tick();
            near(p.getBoundingBox().minY,top(),"anchored support recovers full1block penetration without choosing lower stacked layer");
            require(top()>highest,"stacked platform continues upward during penetration recovery");noCorrection();wire();
        }
        void pendingTeleport() {
            stage(0);step();var p=f.base.owner;var connection=p.connection;
            double x=p.getX()+.2,y=p.getY()+.3,z=p.getZ()+.1;
            connection.teleport(x,y,z,127,-19);
            var pending=listenerState("awaitingPositionFromClient");int genuine=(Integer)listenerState("awaitingTeleport");
            require(pending!=null&&genuine>=0,"ordinary native teleport installs genuine awaiting state");
            f.base.recording.packets.clear();service.tick();
            near(p.getX(),x,"lift cannot overwrite genuine teleport X");near(p.getY(),y,"lift cannot overwrite genuine teleport Y");near(p.getZ(),z,"lift cannot overwrite genuine teleport Z");
            require(f.base.recording.packets.stream().noneMatch(packet->packet instanceof ClientboundPlayerPositionPacket||packet instanceof ClientboundTeleportEntityPacket entity&&entity.id()==p.getId()),
                    "genuine pending teleport prevents all lift transport packets");
            require(listenerState("awaitingPositionFromClient")==pending,"lift retains exact native genuine awaiting state");
            connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(Integer.MIN_VALUE,x-4,y-8,z+5,-67,42));
            require(listenerState("awaitingPositionFromClient")==pending,"raw lift ACK cannot consume a genuine pending teleport");
            near(p.getY(),y,"raw lift ACK cannot overwrite genuine teleport destination");
            connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(genuine,x,y,z,127,-19));
            require(listenerState("awaitingPositionFromClient")==null,"ordinary genuine ACK still completes native teleport");
            near(p.getX(),x,"ordinary ACK accepts destination X");near(p.getY(),y,"ordinary ACK accepts destination Y");near(p.getZ(),z,"ordinary ACK accepts destination Z");
            near(p.getYRot(),127,"ordinary ACK preserves native yaw");near(p.getXRot(),-19,"ordinary ACK preserves native pitch");
            require(!service.supported(p),"genuine teleport releases moving floor support");
        }
        void guards() {
            var p=f.base.owner;
            stage(.2);double hover=p.getY();service.tick();
            near(p.getY(),hover,"hovering entity is never admitted as launch passenger");
            require(!service.supported(p),"initial contact is mandatory");
            stage(0);step();listener();f.base.recording.packets.clear();
            double jump=p.getY()+.42;
            p.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(p.getX()+.1,jump,p.getZ(),51,-8,false,false));
            near(p.getY(),jump,"actual native jump input escapes the floor");
            require(p.getDeltaMovement().y>0,"native jump applies ordinary upward momentum");
            service.tick();near(p.getY(),jump,"jumping player is no longer carried");
            require(!service.supported(p),"jump detaches support");noCorrection();
            stage(0);step();double stopped=p.getY(),display=top();
            f.base.game.items().timeStop().begin(f.level,40);
            try{service.tick();near(p.getY(),stopped,"dimension Time Stop pauses carrier");near(top(),display,"dimension Time Stop pauses platform");}
            finally{f.base.game.items().timeStop().end(f.level);}
            step();p.entityTags().add("sin_greed_active");double greed=p.getY();
            try{service.tick();near(p.getY(),greed,"Greed rejects further carry");require(!service.supported(p),"Greed releases support");}
            finally{p.entityTags().remove("sin_greed_active");}
            service.stop();require(service.displayCount()==0&&!service.supported(p),"stop clears displays and held support");
        }
    }
    public static void run(GameController game) {
        try(var f=new AllGrimoireChecks.Fixture(game)) {
            var probe=new Probe(f);
            try{probe.walking();probe.guards();probe.persistentTransport();probe.pendingTeleport();}finally{game.items().antigravity().stop();f.base.owner.entityTags().remove("sin_greed_active");}
        }
        SkyWars2.LOGGER.info("[check] PASS antigravity-support");
    }
}
