package me.skywars2.game;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import io.netty.buffer.Unpooled;
import me.skywars2.SkyWars2;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Storage and real command coverage without assuming unapproved effect-specific formulas. */
public final class SignedEffectLevelChecks {
    private static final int[] AMPLIFIERS = {
            Integer.MIN_VALUE, -257, -2, -1, 0, 1, 254, 255, 256, Integer.MAX_VALUE
    };

    private SignedEffectLevelChecks() { }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Signed effect levels: " + message);
    }

    private static void codecs(ServerLevel level) {
        var ops = level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        int count = 0;
        for (var effect : BuiltInRegistries.MOB_EFFECT.listElements().toList()) {
            count++;
            for (int amplifier : AMPLIFIERS) {
                var hidden = new MobEffectInstance(effect, 1234, Integer.MIN_VALUE, true, false, true);
                var real = new MobEffectInstance(effect, 600, amplifier, false, true, false, hidden);
                require(real.getAmplifier() == amplifier, "native constructor preserves " + effect + " / " + amplifier);
                require(new MobEffectInstance(real).getAmplifier() == amplifier, "native copy preserves full amplifier");
                var encoded = MobEffectInstance.CODEC.encodeStart(ops, real).getOrThrow();
                var restored = MobEffectInstance.CODEC.parse(ops, encoded).getOrThrow();
                require(restored.getAmplifier() == amplifier && restored.getDuration() == 600
                        && !restored.showIcon() && restored.isVisible() && !restored.isAmbient(), "native save/load retains amplifier, duration and flags");
                require(MobEffectInstance.CODEC.encodeStart(ops, restored).getOrThrow().equals(encoded),
                        "recursive hidden signed effect also survives native persistent codec");
                var raw = Unpooled.buffer();
                try {
                    var buffer = new RegistryFriendlyByteBuf(raw, level.registryAccess());
                    MobEffectInstance.STREAM_CODEC.encode(buffer, real);
                    var decoded = MobEffectInstance.STREAM_CODEC.decode(buffer);
                    require(decoded.getAmplifier() == amplifier && raw.readableBytes() == 0,
                            "native effect-instance VarInt codec retains signed/full amplifier");
                    var packet = new ClientboundUpdateMobEffectPacket(123, real, true);
                    ClientboundUpdateMobEffectPacket.STREAM_CODEC.encode(buffer, packet);
                    var received = ClientboundUpdateMobEffectPacket.STREAM_CODEC.decode(buffer);
                    require(received.getEntityId() == 123 && received.getEffect().equals(effect)
                            && received.getEffectAmplifier() == amplifier && received.getEffectDurationTicks() == 600
                            && received.shouldBlend() && !received.effectShowsIcon() && raw.readableBytes() == 0,
                            "native effect-update packet carries signed/full VarInt without changing its format");
                } finally {
                    raw.release();
                }
            }
        }
        require(count == 40, "all forty native 26.3 effects covered: " + count);
    }

    private static void commandTree(GameController game) {
        var effect = game.server().getCommands().getDispatcher().getRoot().getChild("effect")
                .getChild("give").getChild("targets").getChild("effect");
        for (String duration : new String[]{"seconds", "infinite"}) {
            var node = (ArgumentCommandNode<?, ?>) effect.getChild(duration).getChild("amplifier");
            var argument = (IntegerArgumentType) node.getType();
            require(argument.getMinimum() == Integer.MIN_VALUE && argument.getMaximum() == Integer.MAX_VALUE,
                    "native " + duration + " branch permits full signed int");
        }
        var duration = (IntegerArgumentType) ((ArgumentCommandNode<?, ?>) effect.getChild("seconds")).getType();
        require(duration.getMinimum() == 1 && duration.getMaximum() == 1_000_000,
                "duration bounds and infinite syntax remain separate from amplifier policy");
    }

    private static void command(GameController game, net.minecraft.commands.CommandSourceStack source, String command) {
        try {
            require(game.server().getCommands().getDispatcher().execute(command, source) == 1,
                    "real native command reports one target: " + command);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) {
            throw new IllegalStateException("Native effect command: " + command, error);
        }
    }

    private static void commandsAndEntitySave(GameController game, ServerLevel level) {
        var owner = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(owner != null && owner.level() == level, "native fixture owner");
        var cow = new Cow(EntityTypes.COW, level);
        cow.setPos(owner.position().add(0, 8, 0));
        cow.setNoAi(true);
        cow.setNoGravity(true);
        require(level.addFreshEntity(cow), "native fixture actor admitted");
        var loaded = new Cow(EntityTypes.COW, level);
        try {
            var source = game.server().createCommandSourceStack().withLevel(level).withSuppressedOutput();
            for (int amplifier : AMPLIFIERS) {
                for (String duration : new String[]{"12", "infinite"}) {
                    cow.removeAllEffects();
                    command(game, source,
                            "effect give " + cow.getUUID() + " minecraft:glowing " + duration + " " + amplifier + " true");
                    var actual = cow.getEffect(MobEffects.GLOWING);
                    require(actual != null && actual.getAmplifier() == amplifier
                                    && actual.getDuration() == (duration.equals("infinite") ? -1 : 240) && !actual.isVisible(),
                            "real /effect native execution: " + duration + " / " + amplifier);
                    var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
                    cow.saveWithoutId(output);
                    loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
                    var restored = loaded.getEffect(MobEffects.GLOWING);
                    require(restored != null && restored.getAmplifier() == amplifier && restored.getDuration() == actual.getDuration(),
                            "real LivingEntity active_effects save/load retains signed/full amplifier");
                }
            }
            // Native precedence remains numerical, including negative hidden effects.
            var low = new MobEffectInstance(MobEffects.GLOWING, 100, -100);
            require(low.update(new MobEffectInstance(MobEffects.GLOWING, 1, -2)) && low.getAmplifier() == -2,
                    "native stronger signed amplifier takes over without rewriting stacking rules");
            require(low.tickServer(level, cow, () -> { }) && low.getAmplifier() == -100,
                    "native expiration restores the negative hidden effect");
            command(game, source, "effect clear " + cow.getUUID());
            require(cow.getActiveEffects().isEmpty(), "native /effect clear still works");
        } finally {
            cow.discard();
            loaded.discard();
        }
    }

    public static void run(GameController game) {
        require(game.playing(), "actual gameplay match");
        var owner = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(owner != null, "native TestAlpha participant");
        commandTree(game);
        codecs(owner.level());
        commandsAndEntitySave(game, owner.level());
        SkyWars2.LOGGER.info("[check] PASS signed-effect-levels (40 effects across signed/intmax constructor, copy, recursive persistent/stream codecs and native effect packets; both real /effect branches, actual entity save/load, clear and hidden negative precedence; formulas pending user choices)");
    }
}
