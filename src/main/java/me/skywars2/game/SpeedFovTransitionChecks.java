package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Native equipment transitions and ordered outbound snapshots; no physical client rendering claim. */
public final class SpeedFovTransitionChecks {
    private SpeedFovTransitionChecks() { }
    private static void require(boolean value,String message) {
        if(!value) throw new IllegalStateException("Speed FOV transitions: "+message);
    }
    private static void close(double actual,double expected,String context) {
        require(Double.isFinite(actual)&&Math.abs(actual-expected)<1e-5,context+": "+actual+" expected "+expected);
    }
    private record AttributeState(double base,List<AttributeModifier> modifiers,Set<Identifier> permanent) {
        AttributeState(AttributeInstance attribute) {
            this(attribute.getBaseValue(),List.copyOf(attribute.getModifiers()),attribute.getPermanentModifiers().stream()
                    .map(AttributeModifier::id).collect(java.util.stream.Collectors.toSet()));
        }
        void restore(AttributeInstance attribute) {
            attribute.removeModifiers();attribute.setBaseValue(base);
            for(var modifier:modifiers) if(permanent.contains(modifier.id())) attribute.addPermanentModifier(modifier);
            else attribute.addTransientModifier(modifier);
        }
        boolean matches(AttributeInstance attribute) {
            return attribute.getBaseValue()==base&&Set.copyOf(attribute.getModifiers()).equals(Set.copyOf(modifiers));
        }
    }
    private static final class Recording extends Connection {
        final ServerPlayer player;final List<Packet<?>> packets=new ArrayList<>();
        Recording(ServerPlayer player) {super(PacketFlow.SERVERBOUND);this.player=player;}
        @Override public boolean isMemoryConnection() {return true;}
        @Override public boolean isConnected() {return true;}
        @Override public PacketListener getPacketListener() {return player.connection;}
        @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush) {packets.add(packet);}
        @Override public void flushChannel() { }
    }
    private static void equipment(ServerPlayer player) {
        try {
            var method=LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");method.setAccessible(true);method.invoke(player);
        } catch(ReflectiveOperationException failure) {throw new IllegalStateException("Native equipment fixture changed",failure);}
        LbcMinigunMovement.refresh(player);
    }
    private static void swap(ServerPlayer player) {
        player.connection.handlePlayerAction(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,BlockPos.ZERO,Direction.DOWN));
        equipment(player);
    }
    private static ItemStack loot(GameController game,ServerPlayer player,String leaf) {
        var found=game.lbc().rollTable(player.level(),player.position(),player,"lbc:ported/"+leaf);
        require(found.size()==1,"source equipment exists: "+leaf);return found.getFirst();
    }
    private static ClientboundUpdateAttributesPacket movement(ServerPlayer player) {
        return new ClientboundUpdateAttributesPacket(player.getId(),List.of(player.getAttribute(Attributes.MOVEMENT_SPEED)));
    }
    private static double clientSpeed(ServerPlayer player,ClientboundUpdateAttributesPacket original) {
        var bytes=io.netty.buffer.Unpooled.buffer();
        try {
            var buffer=new RegistryFriendlyByteBuf(bytes,player.registryAccess());
            ClientboundUpdateAttributesPacket.STREAM_CODEC.encode(buffer,original);
            var decoded=ClientboundUpdateAttributesPacket.STREAM_CODEC.decode(buffer);
            require(bytes.readableBytes()==0&&decoded.getEntityId()==player.getId(),"native attribute codec roundtrip");
            var snapshot=decoded.getValues().stream().filter(s->s.attribute().equals(Attributes.MOVEMENT_SPEED)).findFirst().orElseThrow();
            // Native attribute application, then the unmodified client's movement bounds and float conversion.
            var received=new AttributeInstance(Attributes.MOVEMENT_SPEED,ignored->{});
            received.setBaseValue(snapshot.base());snapshot.modifiers().forEach(received::addTransientModifier);
            return (float)Math.clamp(received.getValue(),0,1024);
        } finally {bytes.release();}
    }
    private static void ratio(double speed,ClientboundPlayerAbilitiesPacket packet,ClientboundPlayerAbilitiesPacket source,boolean scoped,String context) {
        require(packet.isInvulnerable()==source.isInvulnerable()&&packet.isFlying()==source.isFlying()
                &&packet.canFly()==source.canFly()&&packet.canInstabuild()==source.canInstabuild()
                &&packet.getFlyingSpeed()==source.getFlyingSpeed(),context+" preserves every native ability flag and flight speed");
        double nativeRatio=source.getWalkingSpeed()==0?1:(speed/source.getWalkingSpeed()+1)/2;
        double multiplier=source.isFlying()?1.1:1;
        double expected=speed==0?1:scoped?.25:(1+(nativeRatio*multiplier-1)*.05)/multiplier;
        double actual=packet.getWalkingSpeed()==0?1:(speed/packet.getWalkingSpeed()+1)/2;
        close(actual,expected,context+" native client ratio uses five-percent gain/fifteen-percent cap or full scope");
    }
    private static ClientboundPlayerAbilitiesPacket onlyAbilities(Recording recording) {
        require(recording.packets.size()==1&&recording.packets.getFirst() instanceof ClientboundPlayerAbilitiesPacket,
                "pure native ability resend remains one abilities packet");
        return (ClientboundPlayerAbilitiesPacket)recording.packets.getFirst();
    }
    private static void resend(ServerPlayer player,Recording recording,double priorWireSpeed,boolean scoped,String context) {
        var source=new ClientboundPlayerAbilitiesPacket(player.getAbilities());recording.packets.clear();player.onUpdateAbilities();
        ratio(priorWireSpeed,onlyAbilities(recording),source,scoped,context);
    }
    private static void frame(ServerPlayer player,Recording recording,ClientboundUpdateAttributesPacket snapshot,
            ClientboundPlayerAbilitiesPacket source,boolean scoped,String context) {
        require(recording.packets.size()==1&&recording.packets.getFirst() instanceof ClientboundBundlePacket,context+" is one outer bundle");
        var children=new ArrayList<Packet<?>>();
        for(var child:((ClientboundBundlePacket)recording.packets.getFirst()).subPackets()) children.add(child);
        require(children.size()==2&&children.getFirst()==snapshot&&children.getLast() instanceof ClientboundPlayerAbilitiesPacket,
                context+" contains original movement snapshot then matching abilities without nested bundles");
        ratio(clientSpeed(player,snapshot),(ClientboundPlayerAbilitiesPacket)children.getLast(),source,scoped,context);
    }
    private static double send(ServerPlayer player,Recording recording,boolean scoped,String context) {
        return send(player,recording,movement(player),scoped,context);
    }
    private static double send(ServerPlayer player,Recording recording,ClientboundUpdateAttributesPacket snapshot,boolean scoped,String context) {
        var attr=player.getAttribute(Attributes.MOVEMENT_SPEED);var before=new AttributeState(attr);
        var abilities=player.getAbilities().pack();var source=new ClientboundPlayerAbilitiesPacket(player.getAbilities());
        var position=player.position();var velocity=player.getDeltaMovement();float speed=player.getSpeed();
        recording.packets.clear();player.connection.send(snapshot);frame(player,recording,snapshot,source,scoped,context);
        require(before.matches(attr)&&player.getAbilities().pack().equals(abilities)&&player.getSpeed()==speed
                &&player.position().equals(position)&&player.getDeltaMovement().equals(velocity),
                context+" projection does not mutate gameplay attributes, abilities, position or velocity");
        return clientSpeed(player,snapshot);
    }
    public static void run(GameController game) {
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(player!=null&&game.playing()&&game.allowDamage(player),"requires active TestAlpha");
        var saved=new GameChecks.WandThirdPlayerState(player,game.items().mana());
        var attributes=new java.util.LinkedHashMap<AttributeInstance,AttributeState>();
        for(var attribute:player.getAttributes().getSyncableAttributes()) attributes.put(attribute,new AttributeState(attribute));
        var packed=player.getAbilities().pack();var effects=List.copyOf(player.getPostEffects());boolean shift=player.isShiftKeyDown();
        var previous=player.connection;var recording=new Recording(player);var zoom=game.items().rifleZoom();
        player.connection=new ServerGamePacketListenerImpl(game.server(),recording,player,
                new CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        try {
            zoom.forget(player);player.closeContainer();player.setGameMode(GameType.SURVIVAL);player.removeAllEffects();
            player.entityTags().remove("time_stopped");player.setShiftKeyDown(false);
            player.getInventory().clearContent();equipment(player);
            var attr=player.getAttribute(Attributes.MOVEMENT_SPEED);attr.removeModifiers();attr.setBaseValue(.1);
            var abilities=player.getAbilities();abilities.setWalkingSpeed(.1f);abilities.setFlyingSpeed(.137f);
            abilities.invulnerable=true;abilities.mayfly=true;abilities.flying=false;abilities.instabuild=false;
            double wire=send(player,recording,false,"clean native baseline");close(wire,.1,"baseline speed");
            var gohei=loot(game,player,"danmaku_staff");var minigun=loot(game,player,"laser_minigun");
            player.setItemInHand(InteractionHand.MAIN_HAND,gohei);equipment(player);
            close(attr.getValue(),.1,"source gohei main hand has no movement penalty");wire=send(player,recording,false,"gohei main hand");
            swap(player);require(player.getOffhandItem()==gohei,"native F moves exact source gohei into offhand");
            close(attr.getValue(),.025,"source gohei offhand applies native minus75-percent modifier");
            resend(player,recording,wire,false,"ability resend before gohei attribute publication");wire=send(player,recording,false,"gohei offhand");
            var actual=attr.getModifiers().stream().filter(m->m.id().equals(Identifier.parse("minecraft:genej656j6j6tk87ukukyeed"))).findFirst().orElseThrow();
            attr.removeModifier(actual.id());close(attr.getValue(),.1,"remove actual source gohei modifier");
            resend(player,recording,wire,false,"removed modifier remains unpublished");wire=send(player,recording,false,"actual gohei modifier removal");
            attr.addTransientModifier(actual);close(attr.getValue(),.025,"readd actual source gohei modifier");
            resend(player,recording,wire,false,"readded modifier remains unpublished");wire=send(player,recording,false,"actual gohei modifier readdition");
            player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.MAIN_HAND,minigun);equipment(player);
            close(attr.getValue(),.05,"native single minigun movement penalty");resend(player,recording,wire,false,"minigun switch before new attribute");
            wire=send(player,recording,false,"minigun main hand");swap(player);close(attr.getValue(),.05,"native minigun offhand penalty");
            wire=send(player,recording,false,"minigun offhand");player.setItemInHand(InteractionHand.MAIN_HAND,minigun.copy());equipment(player);
            close(attr.getValue(),0,"real dual minigun lock");wire=send(player,recording,false,"dual minigun zero-speed fallback");
            player.getInventory().clearContent();equipment(player);close(attr.getValue(),.1,"native equipment removal restores baseline");
            wire=send(player,recording,false,"all equipment removed");
            recording.packets.clear();player.onUpdateAbilities();float walkingBeforeSprint=onlyAbilities(recording).getWalkingSpeed();
            double predictedSprint=wire*1.3;
            player.setSprinting(true);recording.packets.clear();var sprintPacket=movement(player);player.connection.send(sprintPacket);
            var sprintChildren=new ArrayList<Packet<?>>();for(var child:((ClientboundBundlePacket)recording.packets.getFirst()).subPackets())sprintChildren.add(child);
            var sprintAbilities=(ClientboundPlayerAbilitiesPacket)sprintChildren.getLast();
            require(Float.isFinite(sprintAbilities.getWalkingSpeed()),"sprint personal denominator remains finite");
            close((clientSpeed(player,sprintPacket)/sprintAbilities.getWalkingSpeed()+1)/2,
                    1+((predictedSprint/.1+1)/2-1)*.05,"acknowledged sprint obeys personal FOV strength");
            player.setSprinting(false);recording.packets.clear();player.connection.send(movement(player));
            var stopChildren=new ArrayList<Packet<?>>();for(var child:((ClientboundBundlePacket)recording.packets.getFirst()).subPackets())stopChildren.add(child);
            close(((ClientboundPlayerAbilitiesPacket)stopChildren.getLast()).getWalkingSpeed(),walkingBeforeSprint,"stop sprint acknowledgment leaves denominator stable");
            var boost=new AttributeModifier(Identifier.parse("skywars2:check_fov_transition"),.3,AttributeModifier.Operation.ADD_VALUE);
            attr.addTransientModifier(boost);wire=send(player,recording,false,"real moderate movement modifier");close(wire,.4,"moderate modifier is authoritative");
            attr.removeModifier(boost.id());resend(player,recording,wire,false,"ability resend ignores unsent modifier removal");
            attr.addTransientModifier(boost);wire=send(player,recording,false,"moderate modifier readded");attr.removeModifier(boost.id());
            var extreme=new AttributeModifier(boost.id(),2,AttributeModifier.Operation.ADD_VALUE);attr.addTransientModifier(extreme);
            var stale=movement(player);attr.removeModifier(extreme.id());close(attr.getValue(),.1,"live speed changed after packet construction");
            wire=send(player,recording,stale,false,"stale constructed cap-speed snapshot");close(wire,2.1,"snapshot speed survives later modifier removal");
            resend(player,recording,wire,false,"abilities match last stale wire snapshot instead of live baseline");
            wire=send(player,recording,false,"fresh snapshot after stale publication");
            // Native bundle input: movement gains one abilities sibling; an existing raw abilities child is projected once.
            // This recording connection is new to the individual-nameplate owner. Publish
            // its initial canonical team deltas before asserting a movement-only bundle.
            player.connection.send(new ClientboundBundlePacket(List.of()));recording.packets.clear();
            attr.addTransientModifier(boost);var movement=movement(player);var source=new ClientboundPlayerAbilitiesPacket(abilities);
            var other=new ClientboundUpdateAttributesPacket(player.getId()+100000,List.of(attr));
            var health=new ClientboundUpdateAttributesPacket(player.getId(),List.of(player.getAttribute(Attributes.MAX_HEALTH)));
            var nested=new ClientboundBundlePacket(List.<Packet<? super ClientGamePacketListener>>of(movement,source));
            var outer=new ClientboundBundlePacket(List.<Packet<? super ClientGamePacketListener>>of(other,nested,health));
            recording.packets.clear();player.connection.send(outer);
            require(recording.packets.size()==1&&recording.packets.getFirst() instanceof ClientboundBundlePacket,"native outer bundle retained");
            var children=new ArrayList<Packet<?>>();for(var child:((ClientboundBundlePacket)recording.packets.getFirst()).subPackets())children.add(child);
            require(children.size()==5&&children.get(0)==other&&children.get(1)==movement&&children.get(4)==health
                    &&children.stream().noneMatch(ClientboundBundlePacket.class::isInstance),"bundle children flattened with other-player/nonmovement packets untouched");
            require(children.get(2) instanceof ClientboundPlayerAbilitiesPacket&&children.get(3) instanceof ClientboundPlayerAbilitiesPacket,"both native abilities children remain");
            for(int index:new int[]{2,3})ratio(clientSpeed(player,movement),(ClientboundPlayerAbilitiesPacket)children.get(index),source,false,"nested child projects once");
            for(var passthrough:List.of(other,health)) {
                recording.packets.clear();player.connection.send(passthrough);
                require(recording.packets.size()==1&&recording.packets.getFirst()==passthrough,"standalone foreign/nonmovement snapshot unchanged");
            }
            // Native respawn copies the old client's attribute base; only KEEP flags retain its wire modifiers.
            player.setItemInHand(InteractionHand.MAIN_HAND,minigun);player.setItemInHand(InteractionHand.OFF_HAND,gohei);equipment(player);
            close(attr.getValue(),.05,"live boosted gohei/minigun differs from last published .4 movement");
            for(byte keep:new byte[]{0,ClientboundRespawnPacket.KEEP_ALL_DATA}) {
                double expected=keep==0?.1:.05;
                if(keep!=0) {
                    attr.removeModifier(boost.id());
                    close(attr.getValue(),.0125,"live modifier removal differs from retained .05 wire value");
                }
                var reset=new ClientboundRespawnPacket(player.createCommonSpawnInfo(player.level()),keep);
                var resetState=new AttributeState(attr);var resetAbilities=abilities.pack();
                var resetPosition=player.position();var resetVelocity=player.getDeltaMovement();
                recording.packets.clear();player.connection.send(reset);
                require(recording.packets.size()==1&&recording.packets.getFirst() instanceof ClientboundBundlePacket,
                        "native respawn atomically pairs reset with its client-copy denominator");
                var resetChildren=new ArrayList<Packet<?>>();
                for(var child:((ClientboundBundlePacket)recording.packets.getFirst()).subPackets())resetChildren.add(child);
                require(resetChildren.size()==2&&resetChildren.get(0)==reset
                        &&resetChildren.get(1) instanceof ClientboundPlayerAbilitiesPacket,
                        "respawn preserves native reset then abilities order without resending old server attributes");
                ratio(expected,(ClientboundPlayerAbilitiesPacket)resetChildren.get(1),new ClientboundPlayerAbilitiesPacket(abilities),false,
                        keep==0?"respawn byte0 retains wire base and drops wire modifiers":"respawn KEEP_ALL_DATA retains effective wire speed");
                require(resetState.matches(attr)&&abilities.pack().equals(resetAbilities)&&player.position().equals(resetPosition)
                        &&player.getDeltaMovement().equals(resetVelocity),"reset projection preserves server attributes, abilities and motion");
                resend(player,recording,expected,false,"first ability resend matches native reset copy semantics before any tick");
                player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
                if(keep==0) {
                    wire=send(player,recording,false,"fresh movement after reset publishes retained boosted equipment");
                    close(wire,.05,"fresh native update restores effective .05 client speed after byte0 reset");
                } else attr.addTransientModifier(boost);
            }
            for(var rifle:List.of(LbcShooters.SLR,LbcMagnum.ITEM)) {
                zoom.forget(player);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(rifle));
                player.setItemInHand(InteractionHand.OFF_HAND,gohei);equipment(player);wire=send(player,recording,false,"rifle with gohei before scope");
                recording.packets.clear();zoom.punch(player,game.server().getTickCount()+100);
                require(zoom.scoped(player),"main-hand rifle scope admitted");
                var scoped=recording.packets.stream().filter(ClientboundPlayerAbilitiesPacket.class::isInstance)
                        .map(ClientboundPlayerAbilitiesPacket.class::cast).reduce((a,b)->b).orElseThrow();
                ratio(wire,scoped,new ClientboundPlayerAbilitiesPacket(abilities),true,"scope entry uses last wire speed");
                wire=send(player,recording,true,"scoped native movement snapshot");
                swap(player);require(!zoom.scoped(player)&&player.getOffhandItem().is(rifle),"native F immediately invalidates main-hand rifle scope");
                wire=send(player,recording,false,"F-swap packet immediately uses ordinary baseline before zoom tick");
                zoom.tick();require(!zoom.scoped(player)&&player.getPostEffects().stream().noneMatch(id->id.getNamespace().equals("lbc")&&id.getPath().startsWith("zoom")),"next scope maintenance clears swapped rifle overlays");
            }
            zoom.forget(player);player.getInventory().clearContent();equipment(player);attr.removeModifier(boost.id());
            send(player,recording,false,"publish baseline before asynchronous send");
            attr.addTransientModifier(extreme);var offthread=movement(player);attr.removeModifier(extreme.id());
            var cached=new ClientboundPlayerAbilitiesPacket(abilities);var asyncState=new AttributeState(attr);
            var asyncAbilities=abilities.pack();var asyncPosition=player.position();var asyncVelocity=player.getDeltaMovement();
            recording.packets.clear();
            java.util.concurrent.CompletableFuture.runAsync(()->player.connection.send(offthread)).join();
            frame(player,recording,offthread,cached,false,"offthread native send uses immutable packet/ability snapshots");
            require(asyncState.matches(attr)&&abilities.pack().equals(asyncAbilities)&&player.position().equals(asyncPosition)
                    &&player.getDeltaMovement().equals(asyncVelocity),"offthread projection leaves server attributes, abilities and motion untouched");
            SkyWars2.LOGGER.info("[check] PASS speed-fov-transitions (native gohei/minigun equipment and modifier transitions, stale/pending wire snapshots, atomic flat/reset bundles, flags, F swaps, both scopes, offthread send)");
        } finally {
            zoom.forget(player);saved.restore(game);equipment(player);attributes.forEach((attribute,state)->state.restore(attribute));
            player.getAbilities().apply(packed);player.setShiftKeyDown(shift);
            player.clearPostEffects();effects.forEach(player::addPostEffect);
            player.connection=previous;previous.resetPosition();
            player.connection.send(movement(player));player.onUpdateAbilities();
        }
    }
}
