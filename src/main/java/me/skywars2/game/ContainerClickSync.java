package me.skywars2.game;

import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;

/** Publishes the server's view of the slots touched by one accepted client click. */
public interface ContainerClickSync {
    void skywars2$publishClick(ServerPlayer player, ServerboundContainerClickPacket packet, Runnable broadcast);
}
