package me.skywars2.game;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.skywars2.mixin.PlayerCheckpointAccess;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;

/** Native progress restoration without replaying awards, rewards, functions or stat hooks. */
final class MatchPlayerProgress {
    private final Object2IntOpenHashMap<Stat<?>> stats;
    private final Map<AdvancementHolder,Tag> advancements=new LinkedHashMap<>();
    private final Identifier selected;

    MatchPlayerProgress(ServerPlayer player) {
        stats=new Object2IntOpenHashMap<>(((PlayerCheckpointAccess.Stats)player.getStats()).skywars2$stats());
        var access=(PlayerCheckpointAccess.Advancements)player.getAdvancements();
        var ops=player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        access.skywars2$progress().forEach((holder,value)->advancements.put(holder,AdvancementProgress.CODEC.encodeStart(ops,value).getOrThrow().copy()));
        selected=access.skywars2$tab()==null?null:access.skywars2$tab().id();
    }

    void restore(ServerPlayer player) {
        var values=((PlayerCheckpointAccess.Stats)player.getStats()).skywars2$stats();
        var notify=new Object2IntOpenHashMap<Stat<?>>(stats);
        for(var stat:values.keySet())if(!stats.containsKey(stat))notify.put(stat,0);
        values.clear();values.putAll(stats);player.getStats().markAllDirty();
        player.connection.send(new ClientboundAwardStatsPacket(notify));
        var nativeProgress=player.getAdvancements();var access=(PlayerCheckpointAccess.Advancements)nativeProgress;
        nativeProgress.clearTriggers();access.skywars2$progress().clear();access.skywars2$visible().clear();
        access.skywars2$changed().clear();access.skywars2$roots().clear();
        var ops=player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        advancements.forEach((holder,tag)->access.skywars2$start(holder,AdvancementProgress.CODEC.parse(ops,tag.copy()).getOrThrow()));
        access.skywars2$changed().addAll(access.skywars2$progress().keySet());
        access.skywars2$tree().roots().forEach(access.skywars2$roots()::add);
        access.skywars2$listeners(player.level().getServer().getAdvancements());
        // Native flush may omit an empty reset; explicitly retire future client entries.
        player.connection.send(new ClientboundUpdateAdvancementsPacket(true,List.of(),Set.of(),Map.of(),false));
        access.skywars2$first(true);nativeProgress.flushDirty(player,false);
        nativeProgress.setSelectedTab(selected==null?null:player.level().getServer().getAdvancements().get(selected));
        // A reset also retires the client's selection when the server holder did not change.
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket(selected));
    }
}
