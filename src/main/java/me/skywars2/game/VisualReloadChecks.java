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

/** Genuine death/respawn and dimension packets with native virtual-entity replay capture. */
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
        final ItemDisplayElement hole,nuclear,border;
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
            game.items().nuclearVisuals().show(owner.level(),point,5,false);
            game.zoneBarrier().start(owner.getX(),owner.getZ(),500,500,4000);
            hole=game.items().blackHoleVisuals().displays(oldViewer).getFirst();
            nuclear=game.items().nuclearVisuals().displays(oldViewer).getFirst();border=game.zoneBarrier().display();
            var generic=new ItemDisplayElement(new ItemStack(Items.PAPER));extra.addElement(generic);
            game.visualTracking().attach(extra,owner.level(),()->point);extra.startWatching(oldViewer);extra.tick();
            for(var display:List.of(hole,nuclear,border,generic))ids.addAll(display.getEntityIds());
            require(beta.spawned().containsAll(ids),"all four owned manual carriers initially spawned");
        }
        void replay(String phase) {
            require(beta.spawned().containsAll(ids),phase+" restores every manual spawn");
            for(int id:ids)require(beta.packets.stream().anyMatch(p->p instanceof ClientboundSetEntityDataPacket data&&data.id()==id),phase+" includes full metadata "+id);
            require(alpha.spawned().isEmpty(),phase+" sends no duplicate spawn to unaffected observer");
            require(game.items().blackHoleVisuals().displays(beta.listener.player).equals(List.of(hole))
                    &&game.items().nuclearVisuals().displays(beta.listener.player).equals(List.of(nuclear)),phase+" keeps original slots/display identities");
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
            require(state.beta.packets.stream().anyMatch(ClientboundRespawnPacket.class::isInstance),"real client world-reset packet captured");
            require(!viewer.connection.hasClientLoaded(),"replay waits for genuine client load acknowledgement");
            require(java.util.Collections.disjoint(state.beta.spawned(),state.ids),"no premature manual spawn while client loading");
            state.alpha.packets.clear();state.beta.packets.clear();
            viewer.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());game.visualTracking().tick();
            state.replay("respawn");
            var scale=state.nuclear.getScale();
            state.alpha.packets.clear();state.beta.packets.clear();game.visualTracking().tick();
            require(state.beta.spawned().isEmpty(),"idle tick does not repeat the replay");
            var level=viewer.level();var elsewhere=game.server().overworld();var position=viewer.position();
            require(level!=elsewhere,"dimension roundtrip fixture uses independent worlds");
            viewer.teleportTo(elsewhere,position.x,position.y,position.z,Set.of(),0,0,true);
            viewer.hasChangedDimension();viewer.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            state.beta.packets.clear();game.visualTracking().tick();
            require(java.util.Collections.disjoint(state.beta.spawned(),state.ids),"no source-dimension carriers replay into another world");
            viewer.teleportTo(level,position.x,position.y,position.z,Set.of(),0,0,true);
            viewer.hasChangedDimension();viewer.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            state.beta.packets.clear();state.alpha.packets.clear();game.visualTracking().tick();state.replay("dimension return");
            require(state.nuclear.getScale().equals(scale),"replay preserves nuclear animation age/scale");
            SkyWars2.LOGGER.info("[check] PASS visual-reload (real death/respawn, stable network viewer, deferred full spawn+metadata, unchanged slots, nuclear/barrier/manual carrier, dimension return and no idle duplicates)");
        } finally {state.close();current=null;}
    }
}
