package me.skywars2.game;

import me.skywars2.mixin.NegativeMiningClientMixin;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import java.util.ArrayList;
import java.util.List;

/** Owner-only native prediction projection; signed authoritative attributes and game mode stay intact. */
public final class NegativeMiningClient {
    private NegativeMiningClient() { }
    public interface State {
        boolean skywars2$predictionBlocked();
        void skywars2$predictionBlocked(boolean blocked);
    }
    public static boolean blocked(ServerPlayer player) {
        if(!NegativeMining.mayHaveNegativeRate(player))return false;
        var hit=NegativeMining.aimedBlock(player);
        return hit!=null && NegativeMining.negativeRate(player,hit.getBlockPos());
    }
    public static void tick(ServerPlayer player) {
        var state=(State)player.gameMode;
        boolean blocked=blocked(player);
        if(blocked==state.skywars2$predictionBlocked())return;
        state.skywars2$predictionBlocked(blocked);
        // Bundle both switches: creative prediction also reads instabuild before consulting speed.
        player.connection.send(new ClientboundBundlePacket(List.of(
                new ClientboundUpdateAttributesPacket(player.getId(),List.of(player.getAttribute(Attributes.BLOCK_BREAK_SPEED))),
                new ClientboundPlayerAbilitiesPacket(player.getAbilities()))));
    }
    @SuppressWarnings("unchecked")
    public static Packet<?> project(ServerPlayer player,Packet<?> packet) {
        if(!(packet instanceof ClientboundBundlePacket) && !(packet instanceof ClientboundPlayerAbilitiesPacket)
                && !(packet instanceof ClientboundUpdateAttributesPacket))return packet;
        // Async sends use the last server-thread view, never read terrain from a network thread.
        boolean blocked=player.level().getServer().isSameThread()?blocked(player)
                :((State)player.gameMode).skywars2$predictionBlocked();
        return project(player,packet,blocked);
    }
    @SuppressWarnings("unchecked")
    private static Packet<?> project(ServerPlayer player,Packet<?> packet,boolean blocked) {
        if(packet instanceof ClientboundBundlePacket bundle) {
            var children=new ArrayList<Packet<? super ClientGamePacketListener>>();
            boolean changed=false;
            for(var child:bundle.subPackets()) {
                var projected=project(player,child,blocked);changed|=projected!=child;
                if(projected instanceof ClientboundBundlePacket nested)for(var entry:nested.subPackets())children.add(entry);
                else children.add((Packet<? super ClientGamePacketListener>)projected);
            }
            return changed?new ClientboundBundlePacket(children):packet;
        }
        if(!blocked)return packet;
        if(packet instanceof ClientboundUpdateAttributesPacket attributes && attributes.getEntityId()==player.getId()) {
            var values=new ArrayList<>(attributes.getValues());
            values.removeIf(value->value.attribute().equals(Attributes.BLOCK_BREAK_SPEED));
            // A dirty efficiency/water snapshot may omit speed. Always supply its complete zero view.
            values.add(new ClientboundUpdateAttributesPacket.AttributeSnapshot(Attributes.BLOCK_BREAK_SPEED,0,List.of()));
            var projected=NegativeMiningClientMixin.Attributes.skywars2$create(player.getId(),values);
            if(player.level().getServer().isSameThread() && player.getAbilities().instabuild)
                return new ClientboundBundlePacket(List.of(projected,
                        (Packet<? super ClientGamePacketListener>)project(player,new ClientboundPlayerAbilitiesPacket(player.getAbilities()),true)));
            return projected;
        }
        if(packet instanceof ClientboundPlayerAbilitiesPacket abilities && abilities.canInstabuild()) {
            var view=new Abilities();
            view.invulnerable=abilities.isInvulnerable();view.flying=abilities.isFlying();view.mayfly=abilities.canFly();
            view.instabuild=false;view.setFlyingSpeed(abilities.getFlyingSpeed());view.setWalkingSpeed(abilities.getWalkingSpeed());
            return new ClientboundPlayerAbilitiesPacket(view);
        }
        return packet;
    }
}
