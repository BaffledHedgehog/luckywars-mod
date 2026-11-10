package me.skywars2.game;

import java.util.List;
import java.util.Objects;
import me.skywars2.SkyWars2;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.effect.MobEffects;

/** Real command dispatcher, registry-backed wire codec and independent recipient clocks. */
public final class AtlasGlitchChecks {
    private AtlasGlitchChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Atlas glitch: "+why);}
    private static List<?> packets(ShishEncounterChecks.Client client){return (List<?>)WoolBucketChecks.read(WoolBucketChecks.read(client,"wire"),"packets");}
    private static int command(GameController game,ServerPlayer player,String command,boolean op){
        try{return game.server().getCommands().getDispatcher().execute(command,player.createCommandSourceStack()
                .withPermission(op?PermissionSet.ALL_PERMISSIONS:PermissionSet.NO_PERMISSIONS).withSuppressedOutput());}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){if(!op)return -1;throw new IllegalStateException(command,e);}
    }
    public static void run(GameController game){
        for(int x=0;x<3600;x++){
            require(AtlasGlitchSignal.cameraSeed(x/10.0F,0)==3_240_000+x,"all yaw bins retain tenth-degree precision");
            require(AtlasGlitchSignal.cameraSeed(x/10.0F-360,0)==3_240_000+x,"negative yaw wraps to the same bin");
        }
        for(int y=0;y<=1800;y++)
            require(AtlasGlitchSignal.cameraSeed(0,y/10.0F-90)==3600*y,"all pitch bins retain tenth-degree precision");
        require(AtlasGlitchSignal.cameraSeed(359.96F,90)==6_480_000,"rounding across yaw seam wraps safely");
        require(AtlasGlitchSignal.cameraSeed(359.9F,91)==6_483_599,"maximum seed and out-of-range pitch clamp");
        require(AtlasGlitchSignal.cameraSeed(0,-91)==0,"negative pitch clamps safely");
        require(AtlasGlitchSignal.cameraSeed(Float.NaN,Float.POSITIVE_INFINITY)==3_240_000,"nonfinite camera components fall back to zero degrees");
        var owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
        var other=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
        var before=List.copyOf(owner.getPostEffects());var otherBefore=List.copyOf(other.getPostEffects());
        var saved=new ShishEncounterChecks.State(game,owner);
        var camera=owner.getCamera();float yaw=owner.getYRot(),pitch=owner.getXRot(),otherYaw=other.getYRot(),otherPitch=other.getXRot();
        boolean night=owner.hasEffect(MobEffects.NIGHT_VISION);long time=owner.level().getGameTime();
        try(var client=new ShishEncounterChecks.Client(game,owner)){
            AtlasGlitchSignal.manual(owner,false);AtlasGlitchSignal.manual(other,false);
            var unrelated=Identifier.parse("skywars2:atlas_glitch_neighbor");owner.addPostEffect(unrelated);
            require(command(game,owner,"skywars atlasglitch",false)==-1,"nonoperator cannot toggle shader");
            require(command(game,owner,"skywars atlasglitch",true)==1&&owner.getPostEffects().contains(AtlasGlitchSignal.EFFECT),"actual operator toggle enables preview outside Supernova");
            require(!other.getPostEffects().contains(AtlasGlitchSignal.EFFECT),"manual preview is recipient-only");
            require(packets(client).stream().anyMatch(p->p instanceof ClientboundSetTimePacket clock&&clock.clockUpdates().size()==1),"command sends independent visual clock immediately");
            var clockRegistry=owner.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK);
            var key=net.minecraft.resources.ResourceKey.create(Registries.WORLD_CLOCK,AtlasGlitchSignal.EFFECT);
            var clock=clockRegistry.getOrThrow(key);owner.setCamera(owner);
            for(int y=0;y<16;y++)for(int x=0;x<16;x++){
                owner.setYRot(x/10.0F);owner.setXRot(y/10.0F);
                var packet=AtlasGlitchSignal.packet(owner,true);
                var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),owner.registryAccess());
                try{
                    ClientboundSetTimePacket.STREAM_CODEC.encode(buffer,packet);
                    var decoded=ClientboundSetTimePacket.STREAM_CODEC.decode(buffer);
                    var signal=decoded.clockUpdates().get(clock);
                    require(decoded.gameTime()==time&&decoded.clockUpdates().size()==1&&signal!=null
                            &&signal.totalTicks()==1_000_000_000L+x+3600L*(900+y)&&signal.rate()==0&&signal.partialTick()==0,"native wire codec preserves all256 fine-angle payloads without ordinary clock updates");
                }finally{buffer.release();}
            }
            owner.setYRot(-720);owner.setXRot(90);require(AtlasGlitchSignal.code(owner)==1_006_480_000L,"negative yaw wraps and top pitch clamps to grid");
            other.setYRot(90);other.setXRot(0);owner.setCamera(other);
            require(AtlasGlitchSignal.code(owner)==1_003_240_900L,"signal samples the actual camera, not the hidden player body");owner.setCamera(owner);
            AtlasGlitchSignal.synchronize(owner,true);AtlasGlitchSignal.synchronize(other,true);
            require(owner.getPostEffects().contains(AtlasGlitchSignal.EFFECT)&&other.getPostEffects().contains(AtlasGlitchSignal.EFFECT),"automatic lease includes a player without manual preview");
            AtlasGlitchSignal.synchronize(owner,false);AtlasGlitchSignal.synchronize(other,false);
            require(owner.getPostEffects().contains(AtlasGlitchSignal.EFFECT)&&!other.getPostEffects().contains(AtlasGlitchSignal.EFFECT),"automatic completion preserves manual baseline only");
            // Native save/rewind carries these tags and the post-effect list; joining an idle event repairs its lease.
            AtlasGlitchSignal.synchronize(other,true);AtlasGlitchSignal.leaving(other);
            require(!other.getPostEffects().contains(AtlasGlitchSignal.EFFECT),"logout repairs automatic-only effect before save");
            require(command(game,owner,"skywars atlasglitch off",true)==1&&!owner.getPostEffects().contains(AtlasGlitchSignal.EFFECT),"explicit off disables preview");
            var packets=packets(client);require(packets.stream().filter(ClientboundSetTimePacket.class::isInstance).map(ClientboundSetTimePacket.class::cast)
                    .anyMatch(p->p.clockUpdates().containsKey(clock)&&p.clockUpdates().get(clock).totalTicks()==0),"off publishes zero, not merely a removed empty post chain");
            require(command(game,owner,"skywars atlasglitch on TestBeta",true)==1&&other.getPostEffects().contains(AtlasGlitchSignal.EFFECT),"native targeted command updates the selected player");
            require(command(game,owner,"skywars atlasglitch toggle TestBeta",true)==1&&!other.getPostEffects().contains(AtlasGlitchSignal.EFFECT),"targeted toggle disables the selected preview");
            require(owner.getPostEffects().contains(unrelated)&&owner.hasEffect(MobEffects.NIGHT_VISION)==night&&owner.level().getGameTime()==time,"unrelated post effects, real nightvision and authoritative gameTime preserved");
        }finally{
            AtlasGlitchSignal.leaving(owner);AtlasGlitchSignal.leaving(other);owner.setCamera(camera);
            owner.setYRot(yaw);owner.setXRot(pitch);other.setYRot(otherYaw);other.setXRot(otherPitch);
            saved.restore(game);
            owner.clearPostEffects();before.forEach(owner::addPostEffect);other.clearPostEffects();otherBefore.forEach(other::addPostEffect);
            AtlasGlitchSignal.tick(game.server());
        }
        SkyWars2.LOGGER.info("[check] PASS atlas-glitch (real operator and target commands, independent visual clock, 256 fine-angle registry-backed wire payloads, actual camera, automatic/manual leases, logout cleanup, unrelated effects/time/nightvision preserved)");
    }
}
