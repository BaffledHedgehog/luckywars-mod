package me.skywars2.game;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Adds a server force to client-predicted motion without replacing a new jump or movement input. */
final class PlayerForces {
    private PlayerForces() {}

    static void add(ServerPlayer player,Vec3 force) {
        if(!force.isFinite()||force.lengthSqr()==0)return;
        player.setDeltaMovement(player.getDeltaMovement().add(force));
        player.needsSync=true;
        // This native packet's impulse is additive on the client. No explosion is run:
        // air has no terrain particle, empty blockParticles skips its effect tracker,
        // and sound is disabled. Do not set syncVelocity: it would overwrite the jump.
        player.connection.send(new ClientboundExplodePacket(player.position(),0,0,Optional.of(force),
                new BlockParticleOption(ParticleTypes.BLOCK,Blocks.AIR.defaultBlockState()),
                SoundEvents.GENERIC_EXPLODE,WeightedList.of(),false));
    }
}
