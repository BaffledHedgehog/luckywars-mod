package me.skywars2.game;

import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import java.util.*;

/** Native signed-level presentation and packet checks; does not claim GPU-frame verification. */
public final class SignedEffectVisualChecks {
    private SignedEffectVisualChecks() { }
    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException("Signed effect visuals: " + message);
    }
    public static void verify(ServerPlayer viewer) {
        var pig = new Pig(EntityTypes.PIG, viewer.level());
        var effects = List.of(MobEffects.INVISIBILITY, MobEffects.BLINDNESS, MobEffects.DARKNESS,
                MobEffects.NIGHT_VISION, MobEffects.NAUSEA, MobEffects.GLOWING);
        int[] amplifiers = {Integer.MIN_VALUE, -3, -2, -1, 0, 1, 2, 255, 256, Integer.MAX_VALUE};
        try {
            for (var type : effects) for (int amplifier : amplifiers) {
                pig.removeAllEffects();
                pig.addEffect(new MobEffectInstance(type, 200, amplifier, false, true, true));
                require(SignedEffectVisuals.level(pig, type) == (long) amplifier + 1,
                        "native stored signed level " + type + "/" + amplifier);
                var raw = Unpooled.buffer();
                try {
                    var buffer = new RegistryFriendlyByteBuf(raw, viewer.registryAccess());
                    var packet = new ClientboundUpdateMobEffectPacket(pig.getId(), pig.getEffect(type), false);
                    ClientboundUpdateMobEffectPacket.STREAM_CODEC.encode(buffer, packet);
                    var decoded = ClientboundUpdateMobEffectPacket.STREAM_CODEC.decode(buffer);
                    require(decoded.getEffectAmplifier() == amplifier, "packet preserves true amplifier");
                    require(decoded.getEffectDurationTicks() == 200,
                            "raw codec keeps native duration without recipient context " + type + "/" + amplifier);
                    require(raw.readableBytes() == 0, "complete packet consumed");
                } finally { raw.release(); }
                var ids = SignedEffectVisuals.desired(pig,"minecraft:overworld");
                require(ids.size() <= 4, "bounded chain count");
                if (type.is(MobEffects.NAUSEA) && amplifier <= 0) require(ids.isEmpty(), "nausea through level I has no custom shader");
                if (type.is(MobEffects.NAUSEA) && amplifier > 0)
                    require(ids.equals(List.of(Identifier.parse("lbc:signed_effects/nausea_"+SignedEffectVisuals.visualTier((long)amplifier+1)))),"nausea II+ exact bounded shader tier");
                if (type.is(MobEffects.BLINDNESS) && amplifier < -1)
                    require(ids.equals(List.of(Identifier.parse("lbc:signed_effects/fried_"+SignedEffectVisuals.visualTier((long)amplifier+1)))), "negative blindness exact bounded fried tier");
                if (type.is(MobEffects.BLINDNESS) && amplifier == -1) require(ids.isEmpty(),"zero blindness has no custom shader");
                if (type.is(MobEffects.BLINDNESS) && amplifier >= 2)
                    require(ids.contains(Identifier.parse("lbc:signed_effects/black_3")), "positive blindness reaches black");
                if (type.is(MobEffects.DARKNESS) && amplifier < -1)
                    require(!ids.isEmpty(), "negative darkness has brightness shader");
            }
            for (int amplifier : amplifiers) {
                pig.removeAllEffects();
                pig.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, amplifier));
                pig.tick();pig.updateDataBeforeSync();
                require(pig.isInvisible() == (amplifier >= 0), "native invisible bit uses positive level only");
                require(pig.isCurrentlyGlowing() == (amplifier < -1), "negative invisibility becomes glowing");
                pig.removeAllEffects();
                pig.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, amplifier));
                pig.tick();pig.updateDataBeforeSync();
                require(pig.isCurrentlyGlowing() == (amplifier >= 0), "nonpositive glowing has no outline");
                require(pig.isInvisible() == (amplifier < -1), "negative glowing grants native invisibility");
                require(SignedEffectVisuals.invisibilityLevel(pig)==Math.max(0,-((long)amplifier+1)),
                        "negative glowing maps full signed magnitude to invisibility tier");
                pig.setGlowingTag(true);
                require(pig.isCurrentlyGlowing(), "command glowing tag preserved");
                pig.setGlowingTag(false);
            }
            pig.removeAllEffects();
            pig.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,300,4));
            pig.addEffect(new MobEffectInstance(MobEffects.GLOWING,200,-3));
            pig.tick();pig.updateDataBeforeSync();
            require(SignedEffectVisuals.invisibilityLevel(pig)==5,"stronger genuine invisibility survives negative glowing");
            pig.removeEffect(MobEffects.INVISIBILITY);pig.tick();pig.updateDataBeforeSync();
            require(pig.isInvisible()&&SignedEffectVisuals.invisibilityLevel(pig)==2,"removing genuine effect reveals independent negative-glow tier");
            pig.removeEffect(MobEffects.GLOWING);pig.tick();pig.updateDataBeforeSync();
            require(!pig.isInvisible(),"negative-glow removal restores native visibility without lingering synthetic effect");
            require(SignedEffectVisuals.visualTier(2147483648L) == 16
                    && SignedEffectVisuals.visualTier(-2147483647L) == 16, "extreme shader parameters finite");
            pig.removeAllEffects();
            var nexus=Identifier.parse("lbc:nexus");
            require(SignedEffectVisuals.desired(pig,"minecraft:nexus").equals(List.of(nexus)),"exact Nexus dimension owns original Nexus chain");
            var imprinted=Identifier.parse("lbc:imprinted");
            require(SignedEffectVisuals.desired(pig,"minecraft:imprinted").equals(List.of(imprinted)),"exact Imprinted dimension owns its chain");
            require(!SignedEffectVisuals.desired(pig,"minecraft:overworld").contains(imprinted)
                    &&!SignedEffectVisuals.desired(pig,"minecraft:nexus").contains(imprinted),"Imprinted chain absent in other worlds");
            for(var dimension:List.of("minecraft:overworld","minecraft:the_nether","minecraft:the_end","minecraft:imprinted","lbc:nexus"))
                require(!SignedEffectVisuals.desired(pig,dimension).contains(nexus),"dimension mismatch removes Nexus chain: "+dimension);
        } finally { pig.removeAllEffects(); SignedEffectVisuals.removed(pig); }
        lifecycle(viewer);
        var other=viewer.level().getServer().getPlayerList().getPlayers().stream()
                .filter(player -> player != viewer).findFirst().orElse(null);
        if(other!=null) perViewer(viewer,other);
        carrierLifecycle(viewer);
    }
    private static void carrierLifecycle(ServerPlayer viewer) {
        var saved=viewer.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        var post=List.copyOf(viewer.getPostEffects());
        try {
            viewer.removeAllEffects();
            viewer.addEffect(new MobEffectInstance(MobEffects.GLOWING,200,Integer.MAX_VALUE));
            SignedEffectVisuals.effectsChanged(viewer);
            SignedGlowVisuals.tick(viewer.level().getServer());
            require(!SignedGlowVisuals.displays(viewer).isEmpty(),"actual enhanced-glow carrier attaches");
            var display=SignedGlowVisuals.displays(viewer).getFirst();
            require(display.getScale().isFinite(),"IntMax carrier scale finite");
            SignedEffectVisuals.joined(viewer);
            require(SignedGlowVisuals.displays(viewer).isEmpty(),"respawn/relog destroys prior viewer carriers");
            SignedGlowVisuals.tick(viewer.level().getServer());
            require(!SignedGlowVisuals.displays(viewer).isEmpty(),"retained effect rebuilds carrier after world reset");
            var rebuilt=SignedGlowVisuals.displays(viewer).getFirst();
            viewer.removeEffect(MobEffects.GLOWING);
            SignedEffectVisuals.effectsChanged(viewer);
            SignedGlowVisuals.tick(viewer.level().getServer());
            require(SignedGlowVisuals.displays(viewer).stream().noneMatch(d -> d==rebuilt),"effect clear retires rebuilt carrier");
            SignedEffectVisuals.clear(viewer);
            require(SignedGlowVisuals.displays(viewer).isEmpty(),"disconnect destroys viewer carriers");
        } finally {
            viewer.removeAllEffects();for(var effect:saved)viewer.addEffect(effect);
            SignedEffectVisuals.effectsChanged(viewer);
            viewer.clearPostEffects();for(var id:post)viewer.addPostEffect(id);
            viewer.sendPostEffects();
        }
    }
    private static void perViewer(ServerPlayer viewer,ServerPlayer target) {
        var saved=viewer.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        var targetSaved=target.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        boolean invisible=target.isInvisible();
        // Carpet fixture connections deliberately omit their native listener binding.
        // Model the real outbound connection exactly, as the existing Polymer checks do.
        var recipientConnection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
            @Override public net.minecraft.network.PacketListener getPacketListener() { return viewer.connection; }
        };
        var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)recipientConnection;
        var context=provider.getPacketContext();
        require(eu.pb4.polymer.common.api.PolymerCommonUtils.getPlayer(context)==viewer,
                "outbound packet context resolves actual viewer rather than unbound Carpet connection");
        try {
            for(var source:List.of(MobEffects.INVISIBILITY,MobEffects.GLOWING)){
            viewer.removeAllEffects();target.removeAllEffects();
            target.addEffect(new MobEffectInstance(source,200,source.is(MobEffects.GLOWING)?-4:2,false,true,true));
            SignedEffectVisuals.effectsChanged(target);target.setInvisible(true);
            var overlay=eu.pb4.polymer.core.api.entity.PolymerEntity.get(target);
            require(overlay!=null,"player invisibility overlay installed");
            require(overlay.getPolymerEntityType(context)==EntityTypes.INTERACTION,"native invisibilityIII uses Interaction for ordinary viewer");
            var flag=me.skywars2.mixin.SignedEffectVisualMixin.Flags.flags();
            var particle=me.skywars2.mixin.SignedEffectVisualMixin.Particles.particles();
            var fields=List.<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>>of(
                    net.minecraft.network.syncher.SynchedEntityData.DataValue.create(flag,(byte)32),
                    net.minecraft.network.syncher.SynchedEntityData.DataValue.create(particle,List.of()));
            for(int magnitude:new int[]{1,2,3,5}) {
                viewer.removeEffect(MobEffects.BLINDNESS);
                viewer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,200,-magnitude-1));
                SignedEffectVisuals.effectsChanged(viewer);
                require(overlay.getPolymerEntityType(context)==EntityTypes.PLAYER,"negative blindness retracks native player type");
                var projected=SignedEffectVisuals.project(target.getId(),fields,context);
                byte flags=(Byte)projected.getFirst().value();
                var particles=(List<?>)projected.get(1).value();
                require((flags&32)!=0 == (magnitude<3),"viewer-specific invisible flag threshold");
                require(particles.isEmpty()==(magnitude==1),"invisibilityII hides particles; revealedI restores actual particles");
                // Exercise actual packet codec with the intended viewer, rather than only helper arithmetic.
                var raw=Unpooled.buffer();
                try {
                    net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->{
                        var buffer=new RegistryFriendlyByteBuf(raw,viewer.registryAccess());
                        var packet=new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(target.getId(),fields);
                        net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,packet);
                        var decoded=net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);
                        require(decoded.id()==target.getId(),"real metadata codec preserves target entity ID");
                        require(decoded.packedItems().size()==projected.size()
                                && decoded.packedItems().getFirst().value().equals(projected.getFirst().value()),
                                "real metadata codec preserves exact projected flags: expected="+projected+" decoded="+decoded);
                        // ColorParticleOption has identity equality; compare its native serialized
                        // payload, retaining exact ARGB/type/list order rather than object identity.
                        byte[] expectedBytes=metadataBytes(viewer,target.getId(),projected);
                        byte[] actualBytes=metadataBytes(viewer,decoded.id(),decoded.packedItems());
                        require(Arrays.equals(actualBytes,expectedBytes),
                                "real per-viewer metadata codec projection entity="+target.getId()
                                +" expected="+java.util.HexFormat.of().formatHex(expectedBytes)
                                +" decoded="+java.util.HexFormat.of().formatHex(actualBytes));
                        var nativeEffect=viewer.getEffect(MobEffects.BLINDNESS);
                        var effectPacket=new ClientboundUpdateMobEffectPacket(viewer.getId(),nativeEffect,false);
                        ClientboundUpdateMobEffectPacket.STREAM_CODEC.encode(buffer,effectPacket);
                        var clientEffect=ClientboundUpdateMobEffectPacket.STREAM_CODEC.decode(buffer);
                        require(clientEffect.getEffectDurationTicks()==0
                                && clientEffect.getEffectAmplifier()==nativeEffect.getAmplifier(),
                                "actual recipient sees inert negative blindness with true signed amplifier");
                        require(nativeEffect.getDuration()==200,"recipient projection preserves authoritative duration");
                    });
                }finally{raw.release();}
            }
            viewer.removeEffect(MobEffects.BLINDNESS);SignedEffectVisuals.effectsChanged(viewer);
            require(overlay.getPolymerEntityType(context)==EntityTypes.INTERACTION,"clear retracks full hidden form");
            target.removeEffect(source);SignedEffectVisuals.effectsChanged(target);
            require(overlay.getPolymerEntityType(context)==EntityTypes.PLAYER,"source removal restores real player representation");
            }
        } finally {
            viewer.removeAllEffects();for(var effect:saved)viewer.addEffect(effect);
            target.removeAllEffects();for(var effect:targetSaved)target.addEffect(effect);
            target.setInvisible(invisible);
            SignedEffectVisuals.effectsChanged(target);SignedEffectVisuals.effectsChanged(viewer);
        }
    }
    private static byte[] metadataBytes(ServerPlayer viewer,int id,
            List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> values) {
        return net.fabricmc.fabric.api.networking.v1.context.PacketContext.supplyWithoutContext(()->{
            var raw=Unpooled.buffer();
            try {
                var buffer=new RegistryFriendlyByteBuf(raw,viewer.registryAccess());
                var packet=new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(id,values);
                net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,packet);
                return io.netty.buffer.ByteBufUtil.getBytes(raw);
            }finally{raw.release();}
        });
    }
    private static void lifecycle(ServerPlayer viewer) {
        var saved = viewer.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        var post = List.copyOf(viewer.getPostEffects());
        var unrelated = Identifier.parse("minecraft:blur");
        try {
            viewer.removeAllEffects();
            viewer.addPostEffect(unrelated);
            viewer.addPostEffect(Identifier.parse("lbc:signed_effects/nausea_16"));
            viewer.addPostEffect(Identifier.parse("lbc:nexus"));
            SignedEffectVisuals.joined(viewer);
            require(viewer.getPostEffects().contains(unrelated)
                    && viewer.getPostEffects().stream().filter(SignedEffectVisuals::owned).toList().equals(SignedEffectVisuals.desired(viewer)),
                    "relog repairs stale saved chain and preserves unrelated chain");
            viewer.addEffect(new MobEffectInstance(MobEffects.NAUSEA,200,0));
            viewer.addPostEffect(Identifier.parse("lbc:signed_effects/nausea_1"));
            SignedEffectVisuals.synchronize(viewer);
            require(viewer.getPostEffects().stream().noneMatch(id->id.getPath().startsWith("signed_effects/nausea_")),"nausea I removes stale saved custom chain");
            viewer.removeEffect(MobEffects.NAUSEA);
            viewer.addPostEffect(Identifier.parse("lbc:nexus"));
            SignedEffectVisuals.synchronize(viewer);
            require(viewer.getPostEffects().contains(Identifier.parse("lbc:nexus"))==viewer.level().dimension().identifier().toString().equals("minecraft:nexus"),"same body reconciliation repairs stale dimension chain");
            for (int magnitude : new int[]{1,2,3,5}) {
                viewer.removeEffect(MobEffects.BLINDNESS);
                viewer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, -magnitude-1));
                SignedEffectVisuals.effectsChanged(viewer);
                require(viewer.getPostEffects().contains(Identifier.parse("lbc:signed_effects/fried_"+magnitude)),"negative blindness shader grows alongside reveal tier");
                require(!viewer.isMobilityRestricted(),"negative blindness preserves native critical/sprint admission");
                require(SignedEffectVisuals.effectiveInvisibility(magnitude, viewer) == 0,
                        "negative blindness reveals matching invisibility magnitude");
                require(SignedEffectVisuals.effectiveInvisibility(magnitude+2L, viewer) == 2,
                        "negative blindness subtracts tiers, restoring level-I particles");
            }
            viewer.removeEffect(MobEffects.BLINDNESS);
            viewer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,200,-1));
            require(!viewer.isMobilityRestricted(),"zero blindness has no native mobility restriction");
            viewer.removeEffect(MobEffects.BLINDNESS);
            viewer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,200,0));
            require(viewer.isMobilityRestricted(),"positive blindness keeps ordinary native mobility restriction");
            viewer.removeEffect(MobEffects.BLINDNESS);
            SignedEffectVisuals.effectsChanged(viewer);
            require(SignedEffectVisuals.effectiveInvisibility(3, viewer) == 3, "clear restores normal viewer tiers");
            SignedEffectVisuals.clear(viewer);
            require(viewer.getPostEffects().contains(unrelated), "disconnect clears only own chains");
        } finally {
            viewer.removeAllEffects();
            for (var effect : saved) viewer.addEffect(effect);
            SignedEffectVisuals.effectsChanged(viewer);
            viewer.clearPostEffects();
            for (var id : post) viewer.addPostEffect(id);
            viewer.sendPostEffects();
        }
    }
}
