package me.skywars2.game;

import java.util.List;
import java.util.function.Predicate;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

final class RayTrailChecks {
    private RayTrailChecks() {}

    static void run(GameController game) {
        try (var fixture = new WoolBucketChecks.Fixture(game, false)) {
            fixture.prepareParticipants();
            var owner = fixture.owner;
            var chunk = owner.chunkPosition();
            int y = Math.min(owner.getBlockY() + 24, fixture.level.getMaxY() - 10);
            Vec3 from = new Vec3(chunk.getMinBlockX() + 8.5, y + .5, chunk.getMinBlockZ() + 2.5);
            Vec3 aim = new Vec3(0, 0, 1);
            var hit = BlockPos.containing(from.add(aim.scale(12)));
            for (var pos : BlockPos.betweenClosed(hit.offset(-4, -3, -12), hit.offset(4, 4, 0)))
                fixture.set(pos.immutable(), Blocks.AIR.defaultBlockState());
            fixture.set(hit, Blocks.OBSIDIAN.defaultBlockState());
            double eyes = owner.getEyeY() - owner.getY();
            fixture.move(owner, from.add(0, -eyes, 0));
            require(owner.getEyePosition().distanceToSqr(from) < 1e-10, "exact resident ray origin");

            fixture.client.packets().clear();
            LbcWall.scriptCast(game, owner, from, aim, true, 1);
            verify(fixture, "Wall", block(Blocks.BRICKS), 20, 7, 40, false, .1f);
            require(fixture.level.getBlockState(hit.east(3)).is(Blocks.BRICKS), "Wall terminal still builds");

            fixture.client.packets().clear();
            require(game.items().sheep().scriptRay(owner, InteractionHand.MAIN_HAND, false, from, aim),
                    "native Polymorph ray");
            verify(fixture, "Polymorph", p -> p.particle().getType() == ParticleTypes.DUST, 16, 8, 32, false, .1f);

            fixture.client.packets().clear();
            try {
                game.items().stonekivi().emitAt(owner, InteractionHand.MAIN_HAND, ItemStack.EMPTY, 1, from, aim);
                verify(fixture, "Stonekivi stone", block(Blocks.STONE), 24, 8, 48, true, 0);
                verify(fixture, "Stonekivi bedrock", block(Blocks.BEDROCK), 24, 8, 24, true, 0);
            } finally {
                game.items().stonekivi().stop();
            }

            fixture.client.packets().clear();
            game.items().advancedSnow().grimoire(owner, InteractionHand.MAIN_HAND, false, false, 1);
            drain(game);
            verify(fixture, "Advanced Ender", p -> p.particle().getType() == ParticleTypes.PORTAL,
                    23, 8, 115, false, 1);
            require(owner.getZ() > from.z + 10, "normal teleport still reaches the ray terminal");
            fixture.move(owner, from.add(0, -eyes, 0));

            fixture.client.packets().clear();
            try {
                game.items().advancedSnow().grimoire(owner, InteractionHand.MAIN_HAND, true, true, 1);
                drain(game);
                verify(fixture, "Snow Wave", p -> p.particle().getType() == ParticleTypes.SNOWFLAKE && p.count() < 300,
                        23, 8, 23, false, .1f);
                require(game.items().advancedSnow().snapshot().size() == 1 && fixture.client.packets().stream()
                        .anyMatch(packet -> packet instanceof ClientboundLevelParticlesPacket burst
                                && burst.particle().getType() == ParticleTypes.SNOWFLAKE && burst.count() == 300
                                && burst.xMaxSpeed() == .2f), "Snow terminal keeps the native cloud and its separate burst");
            } finally {
                game.items().advancedSnow().forget(owner);
            }
        }
        SkyWars2.LOGGER.info("[check] PASS ray-trails");
    }

    private static Predicate<ClientboundLevelParticlesPacket> block(Block block) {
        return packet -> packet.particle() instanceof BlockParticleOption particle && particle.getState().is(block);
    }

    private static void drain(GameController game) {
        var rays = game.items().exasperato();
        for (int i = 0; i < 100 && rays.pendingSearches() > 0; i++) rays.tick();
        require(rays.pendingSearches() == 0, "bounded loaded ray completion");
    }

    private static void verify(WoolBucketChecks.Fixture fixture, String name,
                               Predicate<ClientboundLevelParticlesPacket> type, int original, int grouped,
                               int total, boolean forced, float speed) {
        List<ClientboundLevelParticlesPacket> packets = fixture.client.packets().stream()
                .filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast)
                .filter(type).toList();
        int particles = packets.stream().mapToInt(ClientboundLevelParticlesPacket::count).sum();
        SkyWars2.LOGGER.info("[check] {} trail: {} packets for {} particles", name, packets.size(), particles);
        int expected = Boolean.getBoolean("skywars2.checkUnbatchedRays") ? original : grouped;
        require(packets.size() == expected && particles == total && packets.stream().allMatch(packet ->
                        packet.overrideLimiter() == forced && !packet.alwaysShow()
                                && packet.xMaxSpeed() == speed && packet.yMaxSpeed() == speed && packet.zMaxSpeed() == speed),
                name + " expected " + expected + " packets / " + total + " particles at original speed and visibility");
    }

    private static void require(boolean ok, String detail) {
        if (!ok) throw new IllegalStateException("Ray trails: " + detail);
    }
}
