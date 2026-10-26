package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/** Actual item-use traces verify the accuracy policy reaches each native gun engine. */
public final class GunAccuracyChecks {
    private GunAccuracyChecks() { }
    private static void require(boolean condition,String message) {
        if(!condition)throw new IllegalStateException("Gun accuracy: "+message);
    }
    private static final class Recording extends Connection {
        final ServerPlayer owner;
        final List<Packet<?>> packets=new ArrayList<>();
        Recording(ServerPlayer owner){super(PacketFlow.SERVERBOUND);this.owner=owner;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public PacketListener getPacketListener(){return owner.connection;}
        @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);}
        @Override public void flushChannel(){ }
        Vec3 direction(boolean ak) {
            var trail=packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance)
                    .map(ClientboundLevelParticlesPacket.class::cast)
                    .filter(p->p.particle().getType()==(ak?ParticleTypes.WHITE_ASH:ParticleTypes.ASH))
                    .filter(p->owner.getEyePosition().distanceToSqr(new Vec3(p.x(),p.y(),p.z()))>=64)
                    .limit(2).toList();
            require(trail.size()==2,"actual native shot delivered two trace samples");
            var a=trail.getFirst();var b=trail.getLast();
            return new Vec3(b.x()-a.x(),b.y()-a.y(),b.z()-a.z()).normalize();
        }
    }
    private static ItemStack loot(GameController game,ServerPlayer owner,String leaf) {
        return game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/"+leaf).getFirst();
    }
    private static ItemStack round(GameController game,ServerPlayer owner) {
        for(int attempt=0;attempt<200;attempt++)for(var stack:game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/magnum_ammo")) {
            var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if(data!=null&&data.copyTag().getIntOr("standard_round",0)==1)return stack.copyWithCount(1);
        }
        throw new IllegalStateException("Gun accuracy: standard Magnum round unavailable");
    }
    private static void near(Vec3 actual,Vec3 expected,String message) {
        require(actual.distanceTo(expected)<1e-8,message+": "+actual+" expected "+expected);
    }
    private static void recoilAndTurning(GameController game,ServerPlayer owner,Recording recording) {
        // Complete the fixture's real teleport before sending native camera-only input.
        var setup=recording.packets.stream().filter(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::isInstance)
                .map(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::cast).reduce((a,b)->b).orElseThrow();
        owner.connection.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(
                setup.id(),owner.getX(),owner.getY(),owner.getZ(),owner.getYRot(),owner.getXRot()));
        // Reproduce the old failure: even a same-position teleport ACK clears onGround.
        require(!owner.onGround(),"native teleport acknowledgement reproduces the old false airborne state");
        owner.setDeltaMovement(new Vec3(0,-.0784,0));
        require(LbcGunAccuracy.movementDegrees(owner)>0,"false airborne contact gravity would add unwanted spread");
        for(String leaf:List.of("slr","magnum","ak47","ak47_finite"))for(var hand:InteractionHand.values()) {
            boolean ak=leaf.startsWith("ak47");
            var zoom=game.items().rifleZoom();zoom.forget(owner);game.items().magnum().forget(owner);game.items().shooters().resetCooldown(owner);
            owner.getInventory().clearContent();var gun=loot(game,owner,leaf);owner.setItemInHand(hand,gun);
            owner.getInventory().setItem(9,leaf.equals("magnum")?round(game,owner):new ItemStack(Items.ARROW,8));
            if(!ak && hand==InteractionHand.OFF_HAND) {
                var before=gun.copy();var ammo=owner.getInventory().getItem(9).copy();
                var position=owner.position();var motion=owner.getDeltaMovement();
                float yaw=owner.getYRot(),pitch=owner.getXRot();int mana=game.items().mana().get(owner);
                zoom.punch(owner);recording.packets.clear();
                require(!zoom.scoped(owner)&&!gun.getItem().use(owner.level(),owner,hand).consumesAction(),"offhand rifle rejects scope and native fire");
                require(recording.packets.stream().noneMatch(packet->packet instanceof ClientboundLevelParticlesPacket
                        ||packet instanceof net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket)
                        &&ItemStack.matches(gun,before)&&ItemStack.matches(owner.getInventory().getItem(9),ammo)
                        &&game.items().mana().get(owner)==mana&&game.items().magnum().queued(owner)==0
                        &&game.items().shooters().cooldownRemaining(owner)==0&&owner.position().equals(position)
                        &&owner.getDeltaMovement().equals(motion)&&owner.getYRot()==yaw&&owner.getXRot()==pitch,
                        "offhand rifle produces no trace/recoil or resource/position mutation");
                continue;
            }
            if(ak)game.items().ak47().setAmmo(owner,10);else zoom.punch(owner);
            owner.setOnGround(true);owner.setKnownMovement(Vec3.ZERO);owner.setDeltaMovement(new Vec3(0,-.0784,0));
            int count=leaf.equals("magnum")?5:1;
            for(int shot=0;shot<count;shot++) {
                // Rapid yaw/pitch changes are real input packets, with no coordinate change.
                owner.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(
                        37+shot*41,-15+shot*3,true,false));
                require(LbcGunAccuracy.movementDegrees(owner)==0,"camera rotation alone never adds spread: "+leaf+" shot="+shot);
                Vec3 look=owner.getLookAngle(),position=owner.position(),motion=owner.getDeltaMovement(),known=owner.getKnownMovement();
                recording.packets.clear();
                if(shot==0)require(gun.getItem().use(owner.level(),owner,hand).consumesAction(),"native recoil use "+leaf+"/"+hand);
                else game.items().magnum().tick();
                near(recording.direction(ak),look.normalize(),"stationary aimed/native AK shot follows current camera: "+leaf+" shot="+shot);
                require(owner.position().equals(position)&&owner.getDeltaMovement().equals(motion)&&owner.getKnownMovement().equals(known)
                        &&owner.onGround(),"recoil preserves native position, contact velocity and grounded state: "+leaf);
                require(recording.packets.stream().noneMatch(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::isInstance),
                        "recoil sends no teleport requiring a false-airborne ACK: "+leaf);
                require(recording.packets.stream().filter(net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket.class::isInstance).count()==1,
                        "one native camera-only rotation packet per shot: "+leaf);
                require(LbcGunAccuracy.movementDegrees(owner)==0,"later burst shots remain accurate after native recoil: "+leaf);
            }
        }
    }
    public static void run(GameController game) {
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(owner!=null&&game.playing()&&game.allowDamage(owner),"requires live participant");
        var saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        var previous=owner.connection;var recording=new Recording(owner);
        var known=owner.getKnownMovement();boolean grounded=owner.onGround(),offhand=game.config().offhandWands;
        int ammunition=game.items().ak47().ammo(owner);
        var cells=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
        owner.connection=new ServerGamePacketListenerImpl(game.server(),recording,owner,
                new CommonListenerCookie(owner.getGameProfile(),0,owner.clientInformation(),false));
        owner.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        var zoom=game.items().rifleZoom();
        try {
            game.config().offhandWands=true;owner.removeAllEffects();owner.setNoGravity(true);
            owner.setShiftKeyDown(false);owner.setOnGround(true);
            owner.teleportTo(owner.level(),owner.getX(),280,owner.getZ(),Set.of(),37,-15,true);
            for(var pos:BlockPos.betweenClosed(owner.blockPosition().offset(-2,-1,-2),owner.blockPosition().offset(2,4,2))) {
                var cell=pos.immutable();cells.put(cell,game.maps().snapshot(cell));owner.level().setBlock(cell,Blocks.AIR.defaultBlockState(),2);
            }
            recoilAndTurning(game,owner,recording);
            // Grounded downward contact motion must not count as running. Airborne ascent/descent does.
            owner.setKnownMovement(new Vec3(0,-.0784,0));owner.setDeltaMovement(Vec3.ZERO);
            require(LbcGunAccuracy.movementDegrees(owner)==0,"standing gravity jitter has no penalty");
            owner.setOnGround(false);require(LbcGunAccuracy.movementDegrees(owner)>0,"falling counts as movement");
            owner.setKnownMovement(new Vec3(0,.42,0));require(LbcGunAccuracy.movementDegrees(owner)==20,"fast jump ascent reaches cap");
            owner.setKnownMovement(Vec3.ZERO);owner.setDeltaMovement(new Vec3(.2,0,0));
            require(LbcGunAccuracy.movementDegrees(owner)>0,"external horizontal push counts as movement");
            owner.setOnGround(true);
            int shots=0;
            for(String leaf:List.of("slr","magnum","ak47","ak47_finite"))
                for(var hand:InteractionHand.values())for(boolean scoped:List.of(false,true))for(double speed:new double[]{0,.075,.15,.3,1.5}) {
                    boolean ak=leaf.startsWith("ak47");
                    if(ak&&scoped || !ak&&hand==InteractionHand.OFF_HAND)continue;
                    zoom.forget(owner);game.items().magnum().forget(owner);game.items().shooters().resetCooldown(owner);
                    owner.getInventory().clearContent();owner.setYRot(37);owner.setXRot(-15);
                    owner.setKnownMovement(new Vec3(speed,0,0));owner.setDeltaMovement(Vec3.ZERO);
                    var gun=loot(game,owner,leaf);owner.setItemInHand(hand,gun);
                    owner.getInventory().setItem(9,leaf.equals("magnum")?round(game,owner):new ItemStack(Items.ARROW,8));
                    if(ak)game.items().ak47().setAmmo(owner,10);
                    if(scoped)zoom.punch(owner);
                    require(zoom.scoped(owner)==scoped,"scope state matches actual held rifle");
                    double degrees=Math.min(20,(ak||scoped?0:5)+20*Math.min(1,speed/.3));
                    long seed=19001+shots++;
                    var look=owner.getLookAngle();var expected=LbcGunAccuracy.sample(look,degrees,RandomSource.create(seed));
                    recording.packets.clear();owner.getRandom().setSeed(seed);
                    require(gun.getItem().use(owner.level(),owner,hand).consumesAction(),"native use "+leaf+"/"+hand);
                    near(recording.direction(ak),expected.normalize(),leaf+"/"+hand+" scope="+scoped+" speed="+speed);
                    if(degrees==0)near(expected,look,"stationary aimed rifle/AK stays exact");
                    else require(expected.dot(look.normalize())>=Math.cos(Math.toRadians(degrees))-1e-10
                            &&expected.distanceTo(look)>.00001,"actual shot bounded by requested cone");
                    if(leaf.equals("magnum")) {
                        // A paid burst re-evaluates scope and motion for every scheduled shot.
                        owner.setYRot(37);owner.setXRot(-15);owner.setKnownMovement(speed>0?Vec3.ZERO:new Vec3(.15,0,0));
                        var next=LbcGunAccuracy.sample(owner.getLookAngle(),(scoped?0:5)+(speed>0?0:10),RandomSource.create(++seed));
                        recording.packets.clear();owner.getRandom().setSeed(seed);game.items().magnum().tick();
                        near(recording.direction(false),next.normalize(),"queued Magnum shot uses current motion");
                    }
                }
            // Vertical aim must keep finite unit directions and the same angular bound.
            for(var look:List.of(new Vec3(0,1,0),new Vec3(0,-1,0),new Vec3(0,0,1)))for(int i=0;i<128;i++) {
                var shot=LbcGunAccuracy.sample(look,20,RandomSource.create(i));
                require(Math.abs(shot.lengthSqr()-1)<1e-10&&shot.dot(look)>=Math.cos(Math.toRadians(20))-1e-10,"vertical and horizontal cone bounds");
            }
            SkyWars2.LOGGER.info("[check] PASS gun-accuracy (native teleport-ACK regression, camera-only recoil and full aimed five-shot Magnum bursts, rapid rotation, 40 speed/scope uses, rifle offhand rejection and both AK hands/variants and cone bounds)");
        } finally {
            zoom.forget(owner);game.items().magnum().forget(owner);game.items().shooters().resetCooldown(owner);
            game.items().ak47().setAmmo(owner,ammunition);
            cells.forEach(game.maps()::restoreSnapshot);saved.restore(game);owner.setKnownMovement(known);owner.setOnGround(grounded);
            game.config().offhandWands=offhand;owner.connection=previous;previous.resetPosition();
        }
    }
}
