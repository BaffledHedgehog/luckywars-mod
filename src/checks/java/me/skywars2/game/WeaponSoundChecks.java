package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Explicit live-match native item input / outbound packet regression, without a client audio claim. */
public final class WeaponSoundChecks {
    private static final String SLR_SOUND="minecraft:lbcsounds.karich";
    private WeaponSoundChecks() { }
    private static void require(boolean value,String message) {
        if(!value) throw new IllegalStateException("Weapon sounds: "+message);
    }
    private static final class Recording extends Connection {
        final ServerPlayer player;
        final List<Packet<?>> packets=new ArrayList<>();
        Recording(ServerPlayer player){super(PacketFlow.SERVERBOUND);this.player=player;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public PacketListener getPacketListener(){return player.connection;}
        @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);}
        @Override public void flushChannel(){ }
        List<ClientboundSoundPacket> gunSounds(){
            return sounds(SLR_SOUND);
        }
        List<ClientboundSoundPacket> sounds(String id){
            return packets.stream().filter(ClientboundSoundPacket.class::isInstance)
                    .map(ClientboundSoundPacket.class::cast)
                    .filter(p->p.getSound().value().location().toString().equals(id)).toList();
        }
    }
    private static final class Swap implements AutoCloseable {
        final ServerPlayer player;
        final ServerGamePacketListenerImpl previous;
        final Recording recording;
        Swap(GameController game,ServerPlayer player){
            this.player=player;previous=player.connection;recording=new Recording(player);
            player.connection=new ServerGamePacketListenerImpl(game.server(),recording,player,
                    new CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        }
        public void close(){player.connection=previous;previous.resetPosition();}
    }
    private static void sound(ClientboundSoundPacket packet,float volume,Vec3 at){
        sound(packet,volume,2,at);
    }
    private static void sound(ClientboundSoundPacket packet,float volume,float pitch,Vec3 at){
        require(packet.getSource()==SoundSource.MASTER && packet.getVolume()==volume && packet.getPitch()==pitch,
                "native MASTER volume "+volume+" / pitch"+pitch);
        require(Math.abs(packet.getX()-at.x)<=.125 && Math.abs(packet.getY()-at.y)<=.125
                && Math.abs(packet.getZ()-at.z)<=.125,"sound position retains native eighth-block quantization");
    }
    private static Vec3 source(ServerPlayer player){
        var forward=player.getLookAngle();
        var left=new Vec3(Math.cos(Math.toRadians(player.getYRot())),0,Math.sin(Math.toRadians(player.getYRot())));
        return player.getEyePosition().subtract(left.scale(.2)).subtract(forward.cross(left).scale(.2))
                .add(forward.scale(.5));
    }
    private static boolean use(ServerPlayer player,InteractionHand hand){
        return player.getItemInHand(hand).getItem().use(player.level(),player,hand).consumesAction();
    }
    private static void clear(Recording shooter,Recording viewer){shooter.packets.clear();viewer.packets.clear();}
    private static void silent(Recording shooter,Recording viewer,String reason){
        require(shooter.gunSounds().isEmpty() && viewer.gunSounds().isEmpty(),reason+" emits no gun sound");
    }
    private static void magnum(GameController game,ServerPlayer shooter,ServerPlayer viewer,Vec3 origin,
            Recording own,Recording other){
        String click="minecraft:ui.button.click";
        for(var hand:InteractionHand.values()) {
            shooter.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            shooter.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            var gun=game.lbc().rollTable(shooter.level(),origin,shooter,"lbc:ported/magnum").getFirst();
            shooter.setItemInHand(hand,gun);
            shooter.teleportTo(shooter.level(),origin.x,origin.y,origin.z,Set.of(),0,0,true);
            shooter.setShiftKeyDown(true);
            for(int distance:List.of(15,17,60)) {
                viewer.teleportTo(viewer.level(),origin.x+distance,origin.y,origin.z,Set.of(),0,0,true);
                clear(own,other);
                int mode=LbcMagnum.mode(gun);
                if(hand==InteractionHand.OFF_HAND) {
                    var before=gun.copy();int mana=game.items().mana().get(shooter);
                    require(!use(shooter,hand)&&ItemStack.matches(gun,before)&&game.items().mana().get(shooter)==mana,
                            "offhand Magnum mode input rejected without stack/mana mutation");
                    require(own.sounds(click).isEmpty()&&other.sounds(click).isEmpty(),"offhand Magnum emits no mode click at "+distance);
                    continue;
                }
                require(use(shooter,hand) && LbcMagnum.mode(gun)==(mode+1)%6,"native Magnum sneak mode input / "+hand);
                var clicks=own.sounds(click);var heard=other.sounds(click);
                require(clicks.size()==1 && heard.size()==(distance==15?1:0),"Magnum click uses native range16 without minimum volume at "+distance);
                for(var packet:clicks) require(packet.getSource()==SoundSource.MASTER && packet.getVolume()==.8f
                        && packet.getPitch()==1.5f && Math.abs(packet.getX()-origin.x)<=.125
                        && Math.abs(packet.getY()-origin.y)<=.125 && Math.abs(packet.getZ()-origin.z)<=.125,
                        "Magnum mode click MASTER/.8/1.5 at shooter position");
                if(!heard.isEmpty()) require(heard.getFirst().getVolume()==.8f && heard.getFirst().getPitch()==1.5f
                        && heard.getFirst().getSource()==SoundSource.MASTER
                        && heard.getFirst().getSeed()==clicks.getFirst().getSeed(),"nearby Magnum listener receives same source click");
            }
            shooter.setShiftKeyDown(false);
            game.items().shooters().resetCooldown(shooter);
            clear(own,other);
            require(!use(shooter,hand),"Magnum ordinary input without rounds rejects");
            require(own.sounds(click).isEmpty() && other.sounds(click).isEmpty(),"rejected non-sneak Magnum emits no mode click");
        }
        shooter.setShiftKeyDown(true);
        game.config().offhandWands=false;
        clear(own,other);
        require(!use(shooter,InteractionHand.OFF_HAND),"disabled offhand rejects Magnum mode input");
        require(own.sounds(click).isEmpty() && other.sounds(click).isEmpty(),"rejected offhand Magnum emits no mode click");
    }
    private static void helicopter(GameController game,ServerPlayer shooter,ServerPlayer viewer,Vec3 origin,
            Recording own,Recording other){
        var level=shooter.level();var service=game.items().helicopterEncounter();
        require(service.activeCount()==0,"exclusive helicopter sound fixture");
        require(level.hasChunkAt(BlockPos.containing(origin.add(0,20,50))),"encounter destination chunk already loaded");
        shooter.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);
        for(int distance:List.of(19,21)) {
            viewer.teleportTo(level,origin.x+distance,origin.y,origin.z,Set.of(),0,0,true);
            var token=new ItemEntity(level,origin.x,origin.y,origin.z,new ItemStack(LbcHelicopterTriggerItem.TOKEN,2));
            token.setNoGravity(true);token.setDeltaMovement(Vec3.ZERO);token.setPickUpDelay(1000);
            token.setYRot(0);token.setXRot(0);
            try {
                require(level.addFreshEntity(token),"actual native helicopter token admitted");
                clear(own,other);
                require(service.spawn(token) && token.getItem().getCount()==1 && service.activeCount()==1,
                        "actual encounter spawn debits one token and creates pair");
                for(String id:List.of("minecraft:lbcsounds.helicopter","minecraft:lbcsounds.kazahstan")) {
                    var heard=other.sounds(id);var local=own.sounds(id);
                    boolean rotor=id.endsWith(".helicopter");
                    require(local.size()==1 && heard.size()==(distance==19?1:0),"encounter spawn selector20 audience / "+id);
                    sound(local.getFirst(),rotor?.1f:1,1,origin);
                    if(distance==19) {
                        sound(heard.getFirst(),rotor?.1f:.4f,1,viewer.position().add(-2,0,0));
                        require(heard.getFirst().getSeed()==local.getFirst().getSeed(),"encounter spawn native seed shared across audience");
                    }
                }
            } finally {
                token.discard();service.entityUnloaded(token);
                // This command never yields a tick: every pair here belongs to this token.
                for(var pair:service.snapshot()) service.entityUnloaded(pair.display());
            }
            require(service.activeCount()==0,"owned encounter pair quietly removed before ticking");
        }
    }
    public static void run(GameController game){
        var shooter=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        var viewer=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(game.playing() && shooter!=null && viewer!=null && game.allowDamage(shooter)
                && game.allowDamage(viewer),"requires two actual match participants");
        var level=game.maps().level();
        require(shooter.level()==level && viewer.level()==level,"fixture players already in match dimension");
        require(!game.items().actionsBlocked(shooter) && !game.items().magic().suppressed(shooter),"unblocked native input");
        // Air-only ownership avoids erasing existing terrain attribution when restoring blocks.
        // The bedrock stop is twelve blocks ahead; mode3's radius4 blasts cannot reach either player.
        var center=new BlockPos(shooter.blockPosition().getX(),280,shooter.blockPosition().getZ());
        var origin=Vec3.atBottomCenterOf(center);
        var bounds=new AABB(origin.add(-10,-10,-3),origin.add(10,11,23));
        for(var pos:BlockPos.betweenClosed(center.offset(-10,-10,-3),center.offset(10,10,23))) {
            require(level.hasChunkAt(pos),"bounded fixture chunks already loaded");
            require(level.getBlockState(pos).isAir(),"requires empty high-altitude fixture volume");
        }
        require(level.getEntities((net.minecraft.world.entity.Entity)null,bounds,e->e!=shooter && e!=viewer).isEmpty(),"no foreign actors in ray/blast fixture");
        var shooterState=new GameChecks.WandThirdPlayerState(shooter,game.items().mana());
        var viewerState=new GameChecks.WandThirdPlayerState(viewer,game.items().mana());
        boolean offhand=game.config().offhandWands,shift=shooter.isShiftKeyDown();
        int cooldown=game.items().shooters().cooldownRemaining(shooter);
        Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        try(var shooterSwap=new Swap(game,shooter);var viewerSwap=new Swap(game,viewer)) {
            for(var pos:List.of(center.below(),center.offset(-1,1,12),center.offset(0,1,12),
                    center.offset(-1,2,12),center.offset(0,2,12))) {
                blocks.put(pos,game.maps().snapshot(pos));
                level.setBlock(pos,Blocks.BEDROCK.defaultBlockState(),3);
            }
            game.config().offhandWands=true;
            shooter.setNoGravity(true);viewer.setNoGravity(true);shooter.setShiftKeyDown(false);
            shooter.getInventory().clearContent();
            var craft=shooter.inventoryMenu.getCraftSlots();
            craft.clearContent();
            for(var hand:InteractionHand.values()) for(int mode=1;mode<=4;mode++) {
                shooter.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                shooter.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                var gun=game.lbc().rollTable(level,origin,shooter,"lbc:ported/slr").getFirst();
                var data=gun.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                data.putInt("mode",mode);gun.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
                shooter.setItemInHand(hand,gun);
                shooter.getInventory().setItem(9,new ItemStack(Items.ARROW,8));
                for(boolean distant:List.of(false,true)) {
                    shooter.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);
                    viewer.teleportTo(level,origin.x+(distant?121:60),origin.y,origin.z,Set.of(),0,0,true);
                    game.items().shooters().resetCooldown(shooter);
                    var at=source(shooter);
                    clear(shooterSwap.recording,viewerSwap.recording);
                    int ammo=shooter.getInventory().getItem(9).getCount();
                    if(hand==InteractionHand.OFF_HAND) {
                        var before=gun.copy();int mana=game.items().mana().get(shooter);
                        require(!use(shooter,hand)&&shooter.getInventory().getItem(9).getCount()==ammo
                                &&game.items().shooters().cooldownRemaining(shooter)==0
                                &&game.items().mana().get(shooter)==mana&&ItemStack.matches(gun,before),
                                "offhand SLR mode"+mode+" rejects without ammo/mana/stack/cooldown mutation");
                        silent(shooterSwap.recording,viewerSwap.recording,"offhand SLR rejection");
                        continue;
                    }
                    require(use(shooter,hand),"native item.use accepts mode"+mode+" / "+hand);
                    require(game.items().shooters().cooldownRemaining(shooter)==15
                            && shooter.getInventory().getItem(9).getCount()==ammo-1,"accepted native shot pays ammo and starts cooldown");
                    var own=shooterSwap.recording.gunSounds();var heard=viewerSwap.recording.gunSounds();
                    require(own.size()==1,"exactly one shooter gun packet for each mode/hand");
                    // No normalization or synthetic relocation: retain the native emitter position.
                    var quantized=new Vec3((int)(at.x*8)/8d,(int)(at.y*8)/8d,(int)(at.z*8)/8d);
                    sound(own.getFirst(),2,quantized);
                    if(distant) require(heard.isEmpty(),"listener beyond source selector120 receives no gun packet");
                    else {
                        require(heard.size()==1,"listener60 blocks away receives native minimum-volume packet");
                        var shifted=viewer.position().add(at.subtract(viewer.position()).normalize().scale(2));
                        sound(heard.getFirst(),.5f,shifted);
                        require(heard.getFirst().getSeed()==own.getFirst().getSeed(),"one native sound seed shared by both listeners");
                    }
                    clear(shooterSwap.recording,viewerSwap.recording);
                    require(!use(shooter,hand),"cooldown rejects immediate native second shot");
                    silent(shooterSwap.recording,viewerSwap.recording,"cooldown rejection");
                }
                game.items().shooters().resetCooldown(shooter);
                shooter.getInventory().setItem(9,ItemStack.EMPTY);
                clear(shooterSwap.recording,viewerSwap.recording);
                require(!use(shooter,hand),"empty ammo rejects native shot");
                silent(shooterSwap.recording,viewerSwap.recording,"no-ammo rejection");
                shooter.setShiftKeyDown(true);
                if(hand==InteractionHand.MAIN_HAND)
                    require(use(shooter,hand)&&LbcShooters.mode(gun)==mode%4+1,"native main-hand sneak input switches mode without ammo");
                else require(!use(shooter,hand)&&LbcShooters.mode(gun)==mode,"offhand sneak input preserves SLR mode");
                silent(shooterSwap.recording,viewerSwap.recording,"mode switch");
                shooter.setShiftKeyDown(false);
            }
            magnum(game,shooter,viewer,origin,shooterSwap.recording,viewerSwap.recording);
            helicopter(game,shooter,viewer,origin,shooterSwap.recording,viewerSwap.recording);
            require(shooter.getHealth()==shooterState.health && viewer.getHealth()==viewerState.health,"sound fixture deals no participant damage");
            SkyWars2.LOGGER.info("[check] PASS weapon-sounds (SLR native item.use, main-hand/all4modes and silent offhand rejection, MASTER2/pitch2, minVolume.5 at60, selector120, silent rejection/switch; Magnum native mode click MASTER.8/pitch1.5 range16; actual helicopter spawn selector20/minVolume.1/.4)");
        } finally {
            game.config().offhandWands=offhand;
            blocks.forEach((pos,saved)->game.maps().restoreSnapshot(pos,saved));
            shooterState.restore(game);viewerState.restore(game);
            shooter.setShiftKeyDown(shift);
            game.items().shooters().setCooldown(shooter,cooldown);
        }
    }
}
