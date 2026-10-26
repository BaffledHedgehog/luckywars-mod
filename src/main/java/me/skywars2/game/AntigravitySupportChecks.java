package me.skywars2.game;

import io.netty.buffer.Unpooled;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
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
        void stage(double hover) {
            service.stop();f.base.owner.removeAllEffects();
            for(var pos:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,4,1)))
                f.level.setBlock(pos,Blocks.AIR.defaultBlockState(),2|16|32);
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
                f.level.setBlock(center.offset(x,0,z),Blocks.STONE.defaultBlockState(),2|16|32);
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
            require(service.snapshot().size()==9,"one connected flat 3x3 platform");
            f.base.recording.packets.clear();
        }
        void listener() {
            // 26.3 admits one positional packet per native listener tick. An independent loaded
            // listener provides each input sample without ticking real players or the match.
            var p=f.base.owner;
            p.connection=new ServerGamePacketListenerImpl(f.base.game.server(),f.base.recording,p,
                    new CommonListenerCookie(p.getGameProfile(),0,p.clientInformation(),false));
            p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            p.connection.resetPosition();
        }
        double top() {return service.snapshot().stream().mapToDouble(d->d.getY()+1).max().orElseThrow();}
        void noCorrection() {
            require(f.base.recording.packets.stream().noneMatch(ClientboundPlayerPositionPacket.class::isInstance),
                    "carry/native movement never opens player teleport acknowledgement lock");
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
            var sent=f.base.recording.packets.stream().filter(ClientboundTeleportEntityPacket.class::isInstance)
                    .map(ClientboundTeleportEntityPacket.class::cast).filter(packet->packet.id()==p.getId()).toList();
            require(sent.size()==1,"exactly one vertical self transport per carry step");
            var bytes=new FriendlyByteBuf(Unpooled.buffer());
            try {
                ClientboundTeleportEntityPacket.STREAM_CODEC.encode(bytes,sent.getFirst());
                var packet=ClientboundTeleportEntityPacket.STREAM_CODEC.decode(bytes);
                require(packet.onGround()&&packet.relatives().equals(Set.of(Relative.X,Relative.Z,Relative.Y_ROT,
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
            try{probe.walking();probe.guards();}finally{game.items().antigravity().stop();f.base.owner.entityTags().remove("sin_greed_active");}
        }
        SkyWars2.LOGGER.info("[check] PASS antigravity-support");
    }
}
