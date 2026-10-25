package me.skywars2.game;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ManualAttachment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/** Re-pairs owned manual visuals after the client clears its entity world. */
public final class ManualVisualTracking {
    private final GameController game;
    private final Map<ElementHolder,ServerLevel> holders=new WeakHashMap<>();
    private final Set<ServerGamePacketListenerImpl> pending=new HashSet<>();
    ManualVisualTracking(GameController game){this.game=game;}

    public void attach(ElementHolder holder,ServerLevel level,Supplier<Vec3> position) {
        new ManualAttachment(holder,level,position);holders.put(holder,level);
    }
    public void clientWorldReset(ServerGamePacketListenerImpl connection) {
        // The native packet can be sent outside the simulation thread.
        game.server().execute(()->pending.add(connection));
    }
    void tick() {
        if(pending.isEmpty())return;
        var online=new HashSet<ServerGamePacketListenerImpl>();
        for(var player:game.server().getPlayerList().getPlayers())online.add(player.connection);
        for(var iterator=pending.iterator();iterator.hasNext();) {
            var connection=iterator.next();
            if(!online.contains(connection)){iterator.remove();continue;}
            if(!connection.hasClientLoaded())continue;
            // Keep entity IDs, metadata, slot assignment and effect ages. Only resend pairing.
            for(var entry:List.copyOf(holders.entrySet())) {
                var holder=entry.getKey();if(holder==null)continue;
                var attachment=holder.getAttachment();
                if(!(attachment instanceof ManualAttachment)){holders.remove(holder);continue;}
                if(entry.getValue()!=connection.player.level()||!holder.getWatchingPlayers().contains(connection))continue;
                holder.stopWatching(connection);holder.startWatching(connection);holder.tick();
            }
            iterator.remove();
        }
    }
    void stop(){pending.clear();holders.clear();}
}
