package me.skywars2.game;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ManualAttachment;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/** Re-pairs owned manual visuals after the client clears its entity world. */
public final class ManualVisualTracking {
    private final GameController game;
    private final Map<ElementHolder,ServerLevel> holders=new WeakHashMap<>();
    private final Map<ServerGamePacketListenerImpl,Boolean> pending=new IdentityHashMap<>();
    ManualVisualTracking(GameController game){this.game=game;}

    public void attach(ElementHolder holder,ServerLevel level,Supplier<Vec3> position) {
        new ManualAttachment(holder,level,position);holders.put(holder,level);
    }
    public void clientWorldReset(ServerGamePacketListenerImpl connection) {
        // The native packet can be sent outside the simulation thread.
        game.server().execute(()->pending.put(connection,false));
    }
    public void clientLoaded(ServerGamePacketListenerImpl connection) {
        game.server().execute(()->{
            // Only a real acknowledgement completes a reset. Ignore duplicate/unsolicited ACKs.
            if(pending.containsKey(connection))pending.put(connection,true);
        });
    }
    void tick() {
        if(pending.isEmpty())return;
        for(var iterator=pending.entrySet().iterator();iterator.hasNext();) {
            var reset=iterator.next();var connection=reset.getKey();var player=connection.player;
            if(game.server().getPlayerList().getPlayer(player.getUUID())!=player||player.connection!=connection) {
                iterator.remove();continue;
            }
            // Native hasClientLoaded also becomes true after a timeout, and remains true on
            // dimension changes. It cannot prove that the client has finished replacing its level.
            if(!reset.getValue()||!connection.hasClientLoaded())continue;
            // Keep entity IDs, metadata, slot assignment and effect ages. Only resend pairing.
            int restored=0;
            for(var entry:List.copyOf(holders.entrySet())) {
                var holder=entry.getKey();if(holder==null)continue;
                var attachment=holder.getAttachment();
                if(!(attachment instanceof ManualAttachment)){holders.remove(holder);continue;}
                if(entry.getValue()!=connection.player.level()||!holder.getWatchingPlayers().contains(connection))continue;
                holder.stopWatching(connection);holder.startWatching(connection);holder.tick();
                restored++;
            }
            SkyWars2.LOGGER.info("Manual visual load acknowledgement for {}: restored {} holders",player.getScoreboardName(),restored);
            iterator.remove();
        }
    }
    void stop(){pending.clear();holders.clear();}
}
