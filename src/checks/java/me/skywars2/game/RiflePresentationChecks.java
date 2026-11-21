package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Recipient-specific native model and tracer projection, including genuine inventory moves. */
public final class RiflePresentationChecks {
    private RiflePresentationChecks() { }
    private static void require(boolean value,String message) {
        if(!value)throw new IllegalStateException("Rifle presentation: "+message);
    }
    private static final class Recording extends Connection implements AutoCloseable {
        final ServerPlayer player;final ServerGamePacketListenerImpl previous;
        final List<Packet<?>> packets=new ArrayList<>();
        Recording(GameController game,ServerPlayer player) {
            super(PacketFlow.SERVERBOUND);this.player=player;previous=player.connection;
            player.connection=new ServerGamePacketListenerImpl(game.server(),this,player,
                    new CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        }
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public PacketListener getPacketListener(){return player.connection;}
        @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);}
        @Override public void flushChannel(){ }
        @Override public void close(){player.connection=previous;previous.resetPosition();}
        List<ClientboundLevelParticlesPacket> trail() {
            return packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance)
                    .map(ClientboundLevelParticlesPacket.class::cast).filter(p->p.particle().getType()==ParticleTypes.ASH).toList();
        }
        void model(ItemStack source,Identifier expected) {
            var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)this;
            var original=source.copy();
            net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->{
                var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(source,
                        net.minecraft.world.item.TooltipFlag.NORMAL,provider.getPacketContext(),player.registryAccess());
                require(expected.equals(proxy.get(DataComponents.ITEM_MODEL)),"recipient model "+expected+" got "+proxy.get(DataComponents.ITEM_MODEL));
                var restored=eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),player.registryAccess());
                require(ItemStack.matches(source,original)&&ItemStack.matches(source,restored),"projection retains authoritative source and full roundtrip");
            });
        }
    }
    private static ItemStack loot(GameController game,ServerPlayer owner,String table) {
        return game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/"+table).getFirst();
    }
    private static ItemStack round(GameController game,ServerPlayer owner) {
        for(int i=0;i<200;i++)for(var stack:game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/magnum_ammo")) {
            var data=stack.get(DataComponents.CUSTOM_DATA);
            if(data!=null&&data.copyTag().getIntOr("standard_round",0)==1)return stack.copyWithCount(1);
        }
        throw new IllegalStateException("Rifle presentation: missing standard ammo");
    }
    private static Vec3 point(ClientboundLevelParticlesPacket packet){return new Vec3(packet.x(),packet.y(),packet.z());}
    public static void run(GameController game) {
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        var other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(owner!=null&&other!=null&&game.playing()&&game.allowDamage(owner),"requires live native participants");
        var saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        var savedOther=new GameChecks.WandThirdPlayerState(other,game.items().mana());
        var known=owner.getKnownMovement();boolean ground=owner.onGround(),offhand=game.config().offhandWands;
        var effects=List.copyOf(owner.getPostEffects());var zoom=game.items().rifleZoom();
        try(var local=new Recording(game,owner);var observer=new Recording(game,other)) {
            owner.closeContainer();game.config().offhandWands=true;owner.removeAllEffects();owner.setNoGravity(true);
            owner.setOnGround(true);owner.setKnownMovement(Vec3.ZERO);owner.setDeltaMovement(Vec3.ZERO);owner.setShiftKeyDown(false);
            owner.teleportTo(owner.level(),owner.getX(),280,owner.getZ(),Set.of(),0,0,true);
            other.setNoGravity(true);other.teleportTo(owner.level(),owner.getX()+2,280,owner.getZ(),Set.of(),0,0,true);
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)owner.level().getChunk((owner.blockPosition().getX()>>4)+x,(owner.blockPosition().getZ()>>4)+z);
            for(String leaf:List.of("slr","magnum"))for(var hand:InteractionHand.values()) {
                zoom.forget(owner);game.items().magnum().forget(owner);game.items().shooters().resetCooldown(owner);
                owner.getInventory().clearContent();owner.getInventory().setSelectedSlot(0);owner.setYRot(0);owner.setXRot(0);
                var gun=loot(game,owner,leaf);owner.setItemInHand(hand,gun);
                owner.getInventory().setItem(9,leaf.equals("magnum")?round(game,owner):new ItemStack(Items.ARROW,8));
                var original=gun.copy();var model=gun.get(DataComponents.ITEM_MODEL);
                var name=leaf.equals("slr")?"slg":"magnum";
                var active=Identifier.parse("lbc:zoom_"+name);var out=Identifier.parse("lbc:zoom_"+name+"_out");
                if(hand==InteractionHand.OFF_HAND) {
                    var beforeEffects=List.copyOf(owner.getPostEffects());var ammo=owner.getInventory().getItem(9).copy();
                    int mana=game.items().mana().get(owner);
                    zoom.punch(owner,game.server().getTickCount()+100);
                    require(!zoom.scoped(owner)&&owner.getPostEffects().equals(beforeEffects),"offhand rifle leaves scope overlays unchanged");
                    local.model(gun,model);observer.model(gun,model);
                    local.packets.clear();observer.packets.clear();
                    require(!gun.getItem().use(owner.level(),owner,hand).consumesAction(),"native offhand rifle use rejected");
                    require(local.trail().isEmpty()&&observer.trail().isEmpty()
                            &&ItemStack.matches(owner.getInventory().getItem(9),ammo)&&ItemStack.matches(gun,original)
                            &&game.items().mana().get(owner)==mana&&game.items().magnum().queued(owner)==0
                            &&game.items().shooters().cooldownRemaining(owner)==0,"offhand rejection preserves native gun/ammo/mana and emits no tracer");
                    continue;
                }
                owner.addPostEffect(Identifier.parse("lbc:zoom"));owner.addPostEffect(Identifier.parse("lbc:zoom_magnum_out"));
                int now=game.server().getTickCount()+100;
                zoom.punch(owner,now);
                require(owner.getPostEffects().equals(List.of(active)),"exactly one new scope replaces all legacy/other scope overlays");
                local.model(gun,Identifier.parse(leaf.equals("slr")?"lbc:super_rifle_scoped":"lbc:magnum_scoped"));
                observer.model(gun,model);
                var eye=owner.getEyePosition();var down=owner.getUpVector(1).normalize().scale(-1);
                local.packets.clear();observer.packets.clear();
                require(gun.getItem().use(owner.level(),owner,hand).consumesAction(),"actual aimed native "+leaf+"/"+hand+" fires");
                var a=local.trail();var b=observer.trail();require(a.size()==b.size()&&a.size()>40,"both recipients retain the same trace sample count");
                boolean near=false,far=false;
                for(int i=0;i<a.size();i++) {
                    var actual=point(a.get(i));var world=point(b.get(i));double distance=eye.distanceTo(world);
                    double fade=Math.max(0,1-distance/8);var expected=world.add(down.scale(.22*fade*fade));
                    require(actual.distanceTo(expected)<1e-9,"only shooter view lowered with smooth convergence");
                    if(distance<1){near=true;require(actual.distanceTo(world)>.15,"near camera clears center");}
                    if(distance>=8){far=true;require(actual.equals(world),"distant samples are exactly the real ray");}
                    require(a.get(i).count()==b.get(i).count()&&a.get(i).xDist()==b.get(i).xDist(),"original particle payload retained");
                }
                require(near&&far&&ItemStack.matches(gun,original),"near/far trace and unchanged authoritative gun");
                game.items().magnum().forget(owner);
                zoom.punch(owner,now+3);require(owner.getPostEffects().equals(List.of(out)),"matching exit effect");
                local.model(gun,model);zoom.tick(now+8);require(owner.getPostEffects().contains(out),"exit remains five ticks");
                zoom.tick(now+9);require(owner.getPostEffects().isEmpty(),"matching exit expires at six ticks");
                zoom.punch(owner,now+12);
                GameChecks.inventoryClick(owner,hand==InteractionHand.MAIN_HAND?36:45,0,net.minecraft.world.inventory.ContainerInput.QUICK_MOVE);
                zoom.tick(now+13);
                int total=0;ItemStack moved=null;
                for(int slot=0;slot<owner.getInventory().getContainerSize();slot++) {
                    var stack=owner.getInventory().getItem(slot);
                    if(ItemStack.isSameItemSameComponents(stack,original)){total+=stack.getCount();moved=stack;}
                }
                require(total==1&&moved!=null&&!zoom.scoped(owner)&&owner.getPostEffects().isEmpty(),"native shift-click leaves one normal gun and clears scope");
                local.model(moved,model);
            }
            SkyWars2.LOGGER.info("[check] PASS rifle-presentation (main-hand rifles/offhand rejection, matching overlays, Polymer model roundtrip, native shift-click, shooter-only near tracers and exact far/world ray)");
        } finally {
            zoom.forget(owner);game.items().magnum().forget(owner);game.items().shooters().resetCooldown(owner);
            saved.restore(game);savedOther.restore(game);owner.setKnownMovement(known);owner.setOnGround(ground);game.config().offhandWands=offhand;
            owner.clearPostEffects();for(var effect:effects)owner.addPostEffect(effect);
        }
    }
}
