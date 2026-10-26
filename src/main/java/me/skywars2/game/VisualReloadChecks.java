package me.skywars2.game;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import me.skywars2.SkyWars2;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Native send-path capture; the memory connection does not observe rendered client pixels. */
public final class VisualReloadChecks {
    private static State current;
    private VisualReloadChecks() { }
    private static void require(boolean value,String message) {
        if(!value)throw new IllegalStateException("Visual reload: "+message);
    }
    private static final class Recording extends Connection {
        final ServerGamePacketListenerImpl listener,previous;
        final List<Packet<?>> packets=new ArrayList<>();
        Recording(GameController game,ServerPlayer player) {
            super(PacketFlow.SERVERBOUND);previous=player.connection;
            listener=new ServerGamePacketListenerImpl(game.server(),this,player,
                    new CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
            player.connection=listener;listener.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        }
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public PacketListener getPacketListener(){return listener;}
        @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener callback,boolean flush){append(packet);}
        @Override public void flushChannel(){ }
        void append(Packet<?> packet){if(packet instanceof ClientboundBundlePacket bundle)bundle.subPackets().forEach(this::append);else packets.add(packet);}
        Set<Integer> spawned() {
            var ids=new HashSet<Integer>();for(var packet:packets)if(packet instanceof ClientboundAddEntityPacket add)ids.add(add.getId());return ids;
        }
        void restore(){var player=listener.player;player.connection=previous;previous.player=player;previous.resetPosition();}
    }
    private static final class State {
        final GameController game;final ServerPlayer owner,oldViewer;
        final GameChecks.WandThirdPlayerState ownerSaved;
        final Recording alpha,beta;
        final Set<Integer> ids=new HashSet<>();
        final ElementHolder extra=new ElementHolder();
        final ItemDisplayElement hole,personalHole,nuclear,border,generic;
        final BlackHoleVisuals.Handle personalHandle;
        final Vec3 point;
        State(GameController game) {
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            oldViewer=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&oldViewer!=null&&game.playing()&&game.lives(oldViewer.getUUID())>=2,"two live bots and spare viewer life");
            ownerSaved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
            alpha=new Recording(game,owner);beta=new Recording(game,oldViewer);
            // Earlier headless dimension fixtures need the native teleport acknowledgement.
            owner.hasChangedDimension();oldViewer.hasChangedDimension();
            owner.setNoGravity(true);oldViewer.setNoGravity(true);oldViewer.removeAllEffects();
            point=owner.position().add(2,3,0);
            game.items().blackHoleVisuals().stop();game.items().nuclearVisuals().stop();
            for(int i=0;i<100;i++){game.items().blackHoleVisuals().tick();game.items().nuclearVisuals().tick();}
            game.items().blackHoleVisuals().create(owner,point,.5f,true);
            hole=game.items().blackHoleVisuals().displays(oldViewer).getFirst();
            // Keep a producer-owned handle whose owner will become the removed player object.
            personalHandle=game.items().blackHoleVisuals().create(oldViewer,point.add(1,0,0),.75f,false);
            personalHole=game.items().blackHoleVisuals().displays(oldViewer).getLast();
            game.items().nuclearVisuals().show(owner.level(),point,5,false);
            game.zoneBarrier().start(owner.getX(),owner.getZ(),500,500,4000);
            nuclear=game.items().nuclearVisuals().displays(oldViewer).getFirst();border=game.zoneBarrier().display();
            generic=new ItemDisplayElement(new ItemStack(Items.PAPER));extra.addElement(generic);
            game.visualTracking().attach(extra,owner.level(),()->point);extra.startWatching(oldViewer);extra.tick();
            for(var display:List.of(hole,personalHole,nuclear,border,generic))ids.addAll(display.getEntityIds());
            require(hole!=personalHole&&game.items().blackHoleVisuals().count()==2,"foreign and personal holes have independent original handles/carriers");
            require(beta.spawned().containsAll(ids),"all five owned manual carriers initially spawned");
        }
        void replay(String phase) {
            require(beta.spawned().containsAll(ids),phase+" restores every manual spawn");
            for(int id:ids) {
                require(beta.packets.stream().filter(p->p instanceof ClientboundAddEntityPacket add&&add.getId()==id).count()==1,phase+" spawns carrier exactly once "+id);
                require(beta.packets.stream().anyMatch(p->p instanceof ClientboundSetEntityDataPacket data&&data.id()==id),phase+" includes full metadata "+id);
            }
            require(alpha.spawned().isEmpty(),phase+" sends no duplicate spawn to unaffected observer");
            require(game.items().blackHoleVisuals().displays(beta.listener.player).equals(List.of(hole,personalHole))
                    &&game.items().nuclearVisuals().displays(beta.listener.player).equals(List.of(nuclear)),phase+" keeps original slots/display identities");
        }
        void clearPackets(){alpha.packets.clear();beta.packets.clear();}
        void noReplay(String phase) {
            require(beta.spawned().isEmpty(),phase+" sends no manual spawn to reset viewer");
            require(alpha.spawned().isEmpty(),phase+" sends no duplicate spawn to unaffected observer");
        }
        void bundledReset(ServerPlayer viewer,boolean nested) {
            var respawn=new ClientboundRespawnPacket(viewer.createCommonSpawnInfo(viewer.level()),(byte)0);
            var bundle=new ClientboundBundlePacket(List.of(respawn));
            viewer.connection.send(nested?new ClientboundBundlePacket(List.of(bundle)):bundle);
            require(beta.packets.stream().anyMatch(ClientboundRespawnPacket.class::isInstance),"actual outbound "+(nested?"nested ":"")+"bundle contains world reset");
        }
        void close() {
            extra.destroy();game.zoneBarrier().stop();game.items().blackHoleVisuals().stop();game.items().nuclearVisuals().stop();
            for(int i=0;i<100;i++){game.items().blackHoleVisuals().tick();game.items().nuclearVisuals().tick();}
            ownerSaved.restore(game);alpha.restore();beta.restore();
        }
    }
    public static void begin(GameController game) {
        require(current==null,"no earlier retained fixture");current=new State(game);
        try {
            var state=current;state.alpha.packets.clear();state.beta.packets.clear();
            state.oldViewer.setHealth(1);state.oldViewer.damageCooldownTime=0;state.oldViewer.setInvulnerableTime(0);
            boolean accepted=state.oldViewer.hurtServer(state.oldViewer.level(),state.oldViewer.damageSources().fellOutOfWorld(),10000);
            require(accepted&&state.oldViewer.isDeadOrDying()&&game.awaitingRespawn(state.oldViewer.getUUID()),"genuine lethal void damage enters normal respawn flow"
                    +" accepted="+accepted+" hp="+state.oldViewer.getHealth()+" changing="+state.oldViewer.isChangingDimension()+" allowed="+game.allowDamage(state.oldViewer));
            state.oldViewer.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        } catch(RuntimeException|Error error){current.close();current=null;throw error;}
    }
    public static void finish(GameController game) {
        var state=current;require(state!=null,"retained live fixture");
        try {
            var viewer=game.server().getPlayerList().getPlayer(state.oldViewer.getUUID());
            require(viewer!=null&&viewer!=state.oldViewer&&viewer.getId()==state.oldViewer.getId()
                    &&viewer.connection==state.beta.listener&&!viewer.isDeadOrDying(),"native respawn replaces player but keeps ID and listener");
            var holes=game.items().blackHoleVisuals();
            holes.tick();
            require(state.personalHandle.owner==state.oldViewer&&state.personalHandle.owner!=viewer,"personal hole deliberately retains pre-death owner object without rebind");
            require(holes.count()==2&&holes.displays(viewer).equals(List.of(state.hole,state.personalHole)),"renderer retains own producer handle and original slot across actual owner replacement");
            var personalPoint=state.point.add(3,1,-2);
            int geometryStart=state.beta.packets.size();
            holes.update(state.personalHandle,personalPoint,1.25f,true);holes.tick();
            require(state.personalHandle.owner==state.oldViewer&&holes.count()==2
                    &&holes.displays(viewer).equals(List.of(state.hole,state.personalHole)),"stale-owner handle accepts later geometry updates without recreation/rebind");
            require(holes.origins(viewer).equals(List.of(state.point,personalPoint))
                    &&state.personalHole.getScale().equals(new org.joml.Vector3f(1.25f)),"retained personal carrier follows current center/radius");
            require(state.personalHole.getItem().get(net.minecraft.core.component.DataComponents.ITEM_MODEL)
                    .equals(net.minecraft.resources.Identifier.parse("lbc:black_hole_large")),"retained personal carrier receives new disc model");
            require(state.beta.packets.stream().skip(geometryStart).anyMatch(p->p instanceof ClientboundSetEntityDataPacket data
                    &&state.personalHole.getEntityIds().contains(data.id())),"real send path receives personal carrier geometry metadata after owner death");
            require(state.beta.packets.stream().anyMatch(ClientboundRespawnPacket.class::isInstance),"real client world-reset packet captured");
            require(!viewer.connection.hasClientLoaded(),"replay waits for genuine client load acknowledgement");
            require(java.util.Collections.disjoint(state.beta.spawned(),state.ids),"no premature manual spawn while client loading");
            state.clearPackets();game.visualTracking().tick();state.noReplay("native respawn before ACK");
            // Vanilla's fallback loaded flag is not proof that the client accepted its new world.
            for(int i=0;i<61;i++)viewer.connection.tickClientLoadTimeout();
            require(viewer.connection.hasClientLoaded(),"native timeout sets loaded flag without a client ACK");
            game.visualTracking().tick();state.noReplay("native loaded timeout without ACK");
            viewer.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());game.visualTracking().tick();
            state.replay("respawn");
            var scale=state.nuclear.getScale();
            state.clearPackets();game.visualTracking().tick();state.noReplay("idle tick after respawn");
            viewer.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());game.visualTracking().tick();
            state.noReplay("duplicate/idle ACK");

            state.bundledReset(viewer,false);
            require(viewer.connection.hasClientLoaded(),"bundle fixture starts with already-loaded native listener");
            state.clearPackets();game.visualTracking().tick();state.noReplay("bundled reset without ACK");
            viewer.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            // A newer reset must revoke the earlier ACK, including a reset nested in a bundle.
            state.bundledReset(viewer,true);state.bundledReset(viewer,false);
            state.clearPackets();game.visualTracking().tick();state.noReplay("newest repeated reset supersedes earlier ACK");
            viewer.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());game.visualTracking().tick();
            state.replay("coalesced direct/nested bundle resets");
            state.clearPackets();game.visualTracking().tick();state.noReplay("coalesced reset consumed once");

            // Replay reads live holders and metadata rather than a snapshot made at reset time.
            var retired=new ElementHolder();var retiredDisplay=new ItemDisplayElement(new ItemStack(Items.PAPER));
            retired.addElement(retiredDisplay);game.visualTracking().attach(retired,viewer.level(),()->state.point);
            retired.startWatching(viewer);retired.tick();var retiredIds=Set.copyOf(retiredDisplay.getEntityIds());
            var late=new ElementHolder();
            try {
                state.bundledReset(viewer,false);retired.destroy();
                state.generic.setItem(new ItemStack(Items.DIAMOND,3));
                var latePosition=state.point.add(4,2,-3);
                var lateDisplay=new ItemDisplayElement(new ItemStack(Items.EMERALD,7));late.addElement(lateDisplay);
                game.visualTracking().attach(late,viewer.level(),()->latePosition);late.startWatching(viewer);late.tick();
                state.clearPackets();game.visualTracking().tick();state.noReplay("holder changes while awaiting ACK");
                viewer.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());game.visualTracking().tick();
                state.replay("live holder state");
                require(java.util.Collections.disjoint(state.beta.spawned(),retiredIds),"destroyed manual holder is not replayed");
                for(int id:lateDisplay.getEntityIds()) {
                    require(state.beta.packets.stream().filter(p->p instanceof ClientboundAddEntityPacket add&&add.getId()==id).count()==1,"holder created after reset is replayed exactly once");
                    require(state.beta.packets.stream().anyMatch(p->p instanceof ClientboundAddEntityPacket add&&add.getId()==id
                            &&Math.abs(add.getX()-latePosition.x)<1e-6&&Math.abs(add.getY()-latePosition.y)<1e-6&&Math.abs(add.getZ()-latePosition.z)<1e-6),"late holder replay uses current position");
                    require(state.beta.packets.stream().anyMatch(p->p instanceof ClientboundSetEntityDataPacket data&&data.id()==id
                            &&data.packedItems().stream().anyMatch(v->v.value() instanceof ItemStack stack&&stack.is(Items.EMERALD)&&stack.getCount()==7)),"late holder replay includes current item metadata");
                }
                for(int id:state.generic.getEntityIds())require(state.beta.packets.stream().anyMatch(p->p instanceof ClientboundSetEntityDataPacket data&&data.id()==id
                        &&data.packedItems().stream().anyMatch(v->v.value() instanceof ItemStack stack&&stack.is(Items.DIAMOND)&&stack.getCount()==3)),"existing holder replay uses metadata changed after reset");
            } finally {retired.destroy();late.destroy();}
            state.clearPackets();
            var level=viewer.level();var elsewhere=game.server().overworld();var position=viewer.position();
            require(level!=elsewhere,"dimension roundtrip fixture uses independent worlds");
            viewer.teleportTo(elsewhere,position.x,position.y,position.z,Set.of(),0,0,true);
            viewer.hasChangedDimension();
            require(state.beta.packets.stream().anyMatch(ClientboundRespawnPacket.class::isInstance),"outward teleport sends actual world reset");
            require(viewer.connection.hasClientLoaded(),"native dimension change retains loaded flag");
            state.clearPackets();game.visualTracking().tick();state.noReplay("dimension departure before ACK");
            viewer.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());game.visualTracking().tick();
            state.noReplay("source-dimension carriers stay absent after destination ACK");
            viewer.teleportTo(level,position.x,position.y,position.z,Set.of(),0,0,true);
            viewer.hasChangedDimension();
            require(state.beta.packets.stream().anyMatch(ClientboundRespawnPacket.class::isInstance),"return teleport sends actual world reset");
            require(viewer.connection.hasClientLoaded(),"native return dimension change retains loaded flag");
            state.clearPackets();game.visualTracking().tick();state.noReplay("dimension return before ACK");
            viewer.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());game.visualTracking().tick();state.replay("dimension return");
            require(state.nuclear.getScale().equals(scale),"replay preserves nuclear animation age/scale");
            state.clearPackets();viewer.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());game.visualTracking().tick();
            state.noReplay("duplicate dimension ACK");
            holes.remove(state.personalHandle);
            for(int i=0;i<10;i++)holes.tick();
            require(holes.count()==1&&holes.displays(viewer).equals(List.of(state.hole)),"producer explicitly removes only its own retained hole");
            state.clearPackets();
            holes.update(state.personalHandle,personalPoint.add(1,0,0),2,true);
            for(int i=0;i<10;i++)holes.tick();
            require(holes.count()==1&&holes.displays(viewer).equals(List.of(state.hole)),"late update cannot recreate explicitly removed handle");
            state.noReplay("explicit producer removal stays final");
            var staleOwned=holes.create(state.oldViewer,state.point.add(5,0,0),1,false);
            require(staleOwned.owner==state.oldViewer&&holes.count()==2,"UUID cleanup fixture uses original removed owner object");
            holes.forget(viewer);
            require(holes.count()==1&&holes.displays(viewer).equals(List.of(state.hole)),"forget replacement player removes same UUID stale-owner hole and retains foreign hole");
            state.clearPackets();for(int i=0;i<10;i++)holes.tick();state.noReplay("UUID cleanup does not recreate removed personal hole");
            SkyWars2.LOGGER.info("[check] PASS visual-reload (native own death retains stale-owner producer handle/slot and live geometry until explicit removal, UUID forget, native load-timeout requires real ACK, direct/nested bundle coalescing, live/destroyed/late holders, full spawn+metadata, unchanged five carrier slots/scale, dimension ACK gates and no duplicates; memory send-path capture, no rendered pixels)");
        } finally {state.close();current=null;}
    }
}
