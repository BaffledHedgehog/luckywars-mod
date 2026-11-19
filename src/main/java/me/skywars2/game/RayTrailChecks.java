package me.skywars2.game;

import java.util.List;
import java.util.function.Predicate;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
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
            movingTrails(game, fixture, from, aim);
            secondaryTrails(game, fixture, from.add(0, 6, 0));

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

    private static void movingTrails(GameController game, WoolBucketChecks.Fixture fixture, Vec3 from, Vec3 aim) {
        var owner = fixture.owner;
        var blind = game.items().blind();
        var teleport = game.items().teleport();
        var ink = ParticleTypes.SQUID_INK;
        require(blind.count() == 0 && blind.cloudCount(owner) == 0 && teleport.count() == 0,
                "exclusive Blind and Teleport trail fixture");
        try {
            fixture.client.packets().clear();
            blind.emitAt(owner, InteractionHand.MAIN_HAND, 2, from, aim);
            verifyMoving(fixture, "Blind Rain", p -> p.particle().getType() == ink, 29, 10, 29, true, 0);
            require(blind.cloudCount(owner) == 1, "Blind Rain still creates a cloud at the terminal terrain");
            blind.stop();

            blind.sourceEmitter(owner, from, aim);
            fixture.client.packets().clear();
            blind.tick();
            verifyMoving(fixture, "Blind emitter", p -> p.particle().getType() == ink, 4, 2, 4, true, 0);
            require(blind.snapshot().size() == 1
                            && blind.snapshot().getFirst().position().distanceTo(from.add(aim.scale(.99))) < 1e-6,
                    "Blind emitter retains its physical step");
            blind.stop();

            var cow = new net.minecraft.world.entity.animal.cow.Cow(
                    net.minecraft.world.entity.EntityTypes.COW, fixture.level);
            cow.setPos(from.add(.6, .1, 1.1));
            cow.setNoAi(true);
            cow.setNoGravity(true);
            require(fixture.level.addFreshEntity(cow), "loaded flying Teleport witness");
            fixture.own(cow);
            try {
                var charge = teleport.flying(owner, InteractionHand.MAIN_HAND, ItemStack.EMPTY, from, aim);
                fixture.client.packets().clear();
                teleport.tick();
                verifyMoving(fixture, "Flying Teleport", p -> p.particle().getType() == ParticleTypes.ANGRY_VILLAGER,
                        5, 2, 10, true, 0);
                var lifted = cow.getEffect(net.minecraft.world.effect.MobEffects.LEVITATION);
                require(lifted != null && lifted.getAmplifier() == 40 && lifted.getDuration() == 20
                                && charge.position().distanceTo(from.add(aim.scale(1.2))) < 1e-6,
                        "flying charge retains actual levitation and its physical step");
            } finally {
                fixture.discard(cow);
                teleport.stop();
            }

            teleport.castAt(owner, InteractionHand.MAIN_HAND, 2, ItemStack.EMPTY, from, aim);
            fixture.client.packets().clear();
            teleport.tick();
            verifyMoving(fixture, "Inverted Teleport clouds", p -> p.particle().getType() == ParticleTypes.CLOUD,
                    11, 4, 11, true, .05f);
            var sculk = fixture.client.packets().stream()
                    .filter(ClientboundLevelParticlesPacket.class::isInstance)
                    .map(ClientboundLevelParticlesPacket.class::cast)
                    .filter(p -> p.particle().getType() == ParticleTypes.SCULK_CHARGE_POP).toList();
            require(sculk.size() == 11, "inverted charge retains all directed sculk bursts");
            for (int i = 0; i < sculk.size(); i++) {
                var packet = sculk.get(i);
                var at = from.add(aim.scale(i * .3));
                require(packet.count() == 0 && packet.overrideLimiter() && !packet.alwaysShow()
                                && packet.yDist() * packet.yMaxSpeed() == -.5f
                                && at.distanceTo(new Vec3(packet.x(), packet.y(), packet.z())) < 1e-6,
                        "inverted sculk keeps each authored sample and its downward velocity");
            }
        } finally {
            blind.stop();
            teleport.stop();
        }
    }

    private static void secondaryTrails(GameController game, WoolBucketChecks.Fixture fixture, Vec3 origin) {
        var owner = fixture.owner;
        var other = fixture.other;
        var start = BlockPos.containing(origin);
        for (var cell : BlockPos.betweenClosed(start.offset(-1, -3, -1), start.offset(1, 1, 21))) {
            require(fixture.level.hasChunkAt(cell), "resident fixture corridor for Zeus and Strangeness");
            fixture.set(cell.immutable(), Blocks.AIR.defaultBlockState());
        }
        var oldOwner = owner.position();
        var oldOther = other.position();
        fixture.move(owner, origin.add(0, -1, 0));
        fixture.move(other, origin.add(0, -30, 0));
        var cow = new Cow(EntityTypes.COW, fixture.level);
        cow.setNoAi(true);
        cow.setNoGravity(true);
        cow.setPos(origin.add(0, -cow.getEyeHeight(), 5.5));
        require(fixture.level.addFreshEntity(cow), "resident Zeus and Strangeness target");
        fixture.own(cow);
        var summons = game.items().summons();
        boolean ungrouped = Boolean.getBoolean("skywars2.checkUnbatchedSecondaryTrails");
        try {
            var log = fixture.client.packets();
            float health = cow.getHealth();
            log.clear();
            game.items().zeusSkill().fire(owner);
            var sounds = log.stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast)
                    .filter(p -> p.getSound().value().location().getPath().equals("entity.firework_rocket.twinkle")).toList();
            require(sounds.size() == 22 && cow.getHealth() == health - 4 && cow.getLastHurtByMob() == owner,
                    "Zeus traverses 22 quarter-block samples, then does its original damage and attribution: sounds="
                            + sounds.size() + ", health=" + cow.getHealth());
            verify(fixture, "Zeus spark", p -> p.particle().getType() == ParticleTypes.ELECTRIC_SPARK,
                    ungrouped ? 22 : 8, 22, false, 1);

            require(summons.ready() && summons.count() == 0, "exclusive native Strangeness caster");
            var caster = summons.spawnPreset(owner, "strangeness", origin.add(0, -2.5, 0));
            require(caster != null, "actual Strangeness summon");
            fixture.own(caster);
            caster.setNoGravity(true);
            caster.setNoAi(true);
            cow.setPos(origin.add(0, -cow.getEyeHeight(), 8));
            cow.setHealth(health);
            cow.setInvulnerableTime(0);
            cow.damageCooldownTime = 0;
            var wall = start.south(10);
            fixture.set(wall, Blocks.BEDROCK.defaultBlockState());

            var coreColor = new DustParticleOptions(0x4c8596, 3).getColor();
            var hazeColor = new DustParticleOptions(0x1f1030, 3).getColor();
            int steps = 0, hazePackets = 0, hazeParticles = 0;
            for (int tick = 0; tick < 100 && steps < 21; tick++) {
                log.clear();
                summons.tick();
                int wave = 0;
                int haze = 0;
                for (var packet : log) {
                    if (!(packet instanceof ClientboundLevelParticlesPacket particle)
                            || !(particle.particle() instanceof DustParticleOptions dust)) continue;
                    if (dust.getColor().equals(coreColor)) {
                        require(particle.overrideLimiter() && particle.count() == 1
                                && particle.xDist() == 0 && particle.yDist() == 0 && particle.zDist() == 0
                                && new Vec3(particle.x(),particle.y(),particle.z()).distanceToSqr(origin.add(0,0,steps + wave)) < 1e-8,
                                "each Strangeness core stays at its original point");
                        wave++;
                    } else if (dust.getColor().equals(hazeColor)) {
                        require(!particle.overrideLimiter() && particle.count() % 3 == 0,
                                "Strangeness haze retains normal viewer range and three particles per sample");
                        haze++;
                        hazeParticles += particle.count();
                    }
                }
                require(haze == (ungrouped ? wave : (wave + 2) / 3),
                        "haze groups do not cross native tick boundaries: " + haze + " packets, " + wave + " core points");
                hazePackets += haze;
                steps += wave;
            }
            require(steps == 21 && hazeParticles == 63 && (ungrouped || hazePackets < 21)
                    && fixture.level.getBlockState(wall).isAir() && cow.getHealth() < health
                    && cow.getLastDamageSource().getEntity() == caster,
                    "Strangeness keeps its whole visible ray, terrain effect and physical attacker: steps="
                            + steps + ", haze=" + hazePackets + ", health=" + cow.getHealth());
            SkyWars2.LOGGER.info("[check] Strangeness trail: {} packets for 84 particles ({} haze and 21 core)", hazePackets + 21, hazePackets);
        } finally {
            summons.stop();
            fixture.discard(cow);
            fixture.move(owner, oldOwner);
            fixture.move(other, oldOther);
        }
    }

    private static Predicate<ClientboundLevelParticlesPacket> block(Block block) {
        return packet -> packet.particle() instanceof BlockParticleOption particle && particle.getState().is(block);
    }

    private static void drain(GameController game) {
        var rays = game.items().exasperato();
        for (int i = 0; i < 100 && rays.pendingSearches() > 0; i++) rays.tick();
        require(rays.pendingSearches() == 0, "bounded loaded ray completion");
    }

    private static void verifyMoving(WoolBucketChecks.Fixture fixture, String name,
                                     Predicate<ClientboundLevelParticlesPacket> type, int original, int grouped,
                                     int total, boolean forced, float speed) {
        verify(fixture, name, type, Boolean.getBoolean("skywars2.checkUnbatchedMovingTrails") ? original : grouped,
                total, forced, speed);
    }

    private static void verify(WoolBucketChecks.Fixture fixture, String name,
                               Predicate<ClientboundLevelParticlesPacket> type, int original, int grouped,
                               int total, boolean forced, float speed) {
        verify(fixture, name, type, Boolean.getBoolean("skywars2.checkUnbatchedRays") ? original : grouped,
                total, forced, speed);
    }

    private static void verify(WoolBucketChecks.Fixture fixture, String name,
                               Predicate<ClientboundLevelParticlesPacket> type, int expected,
                               int total, boolean forced, float speed) {
        List<ClientboundLevelParticlesPacket> packets = fixture.client.packets().stream()
                .filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast)
                .filter(type).toList();
        int particles = packets.stream().mapToInt(ClientboundLevelParticlesPacket::count).sum();
        SkyWars2.LOGGER.info("[check] {} trail: {} packets for {} particles", name, packets.size(), particles);
        require(packets.size() == expected && particles == total && packets.stream().allMatch(packet ->
                        packet.overrideLimiter() == forced && !packet.alwaysShow()
                                && packet.xMaxSpeed() == speed && packet.yMaxSpeed() == speed && packet.zMaxSpeed() == speed),
                name + " expected " + expected + " packets / " + total + " particles at original speed and visibility");
    }

    private static void require(boolean ok, String detail) {
        if (!ok) throw new IllegalStateException("Ray trails: " + detail);
    }
}
