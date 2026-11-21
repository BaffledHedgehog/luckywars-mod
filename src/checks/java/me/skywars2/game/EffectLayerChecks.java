package me.skywars2.game;

import io.netty.buffer.Unpooled;
import me.skywars2.SkyWars2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Synchronous native checks; registration and execution belong to the isolated match runner. */
public final class EffectLayerChecks {
    private EffectLayerChecks() { }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Effect layers: " + message);
    }
    private static MobEffectInstance effect(int duration, int amplifier) {
        return new MobEffectInstance(MobEffects.GLOWING, duration, amplifier);
    }
    private static MobEffectInstance hidden(MobEffectInstance effect) {
        return ((EffectLayers.Access)effect).skywars2$layerHidden();
    }
    private static void layer(MobEffectInstance effect, int amplifier, int duration) {
        require(effect != null && effect.getAmplifier() == amplifier && effect.getDuration() == duration,
                "expected amplifier " + amplifier + " / duration " + duration);
    }
    private static void tick(MobEffectInstance effect, Cow cow, int ticks) {
        for (int i = 0; i < ticks; i++) require(effect.tickServer((ServerLevel)cow.level(), cow, () -> { }), "live tick " + i);
    }
    private static void command(GameController game, Cow cow, String arguments) {
        try {
            var source = game.server().createCommandSourceStack().withLevel((ServerLevel)cow.level()).withSuppressedOutput();
            require(game.server().getCommands().getDispatcher().execute(
                    "effect give " + cow.getUUID() + " minecraft:glowing " + arguments, source) == 1,
                    "native /effect succeeds: " + arguments);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) {
            throw new IllegalStateException("Native effect layer command", error);
        }
    }
    private static void clocks(Cow cow) {
        var lowFirst = effect(30, -100);
        require(lowFirst.update(effect(5, -2)), "higher signed layer activates");
        layer(hidden(lowFirst), -100, 30);
        tick(lowFirst, cow, 5);
        layer(lowFirst, -100, 25);

        var highFirst = effect(5, -2);
        require(!highFirst.update(effect(30, -100)), "lower grant changes no active details");
        tick(highFirst, cow, 5);
        layer(highFirst, -100, 25);

        var chain = effect(8, Integer.MAX_VALUE);
        require(!chain.update(effect(3, 0)), "masked short layer stored without active changes");
        require(!chain.update(effect(30, Integer.MIN_VALUE)), "deep signed layer admitted");
        layer(hidden(chain), 0, 3);
        layer(hidden(hidden(chain)), Integer.MIN_VALUE, 30);
        tick(chain, cow, 8);
        layer(chain, Integer.MIN_VALUE, 22);
        require(hidden(chain) == null, "expired intermediate skipped");

        var same = effect(10, 1);
        require(!same.update(effect(3, 1)), "same level keeps longer duration");
        layer(same, 1, 10);
        require(same.update(effect(20, 1)), "same level extends maximum");
        layer(same, 1, 20);
        require(hidden(same) == null, "same level does not create duplicate clocks");

        var infinite = effect(-1, 3);
        infinite.update(effect(2, 2));
        tick(infinite, cow, 3);
        layer(infinite, 3, -1);
        require(hidden(infinite) == null, "masked finite layer expires under infinite active");
        infinite.update(effect(-1, -5));
        layer(hidden(infinite), -5, -1);
        var finite = effect(2, 1);
        finite.update(effect(-1, -1));
        tick(finite, cow, 2);
        layer(finite, -1, -1);

        var incoming = effect(5, 8);
        incoming.update(effect(12, 4));
        var receiver = effect(20, 0);
        receiver.update(incoming);
        layer(receiver, 8, 5);
        layer(hidden(receiver), 4, 12);
        layer(hidden(hidden(receiver)), 0, 20);
        layer(incoming, 8, 5);
        layer(hidden(incoming), 4, 12);
    }
    private static void entityAndStorage(GameController game, Cow cow) {
        require(cow.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 2, false, true, true)), "active speed admitted");
        var speed = cow.getAttribute(Attributes.MOVEMENT_SPEED);
        var modifier = speed.getModifiers().iterator().next();
        double value = speed.getValue();
        require(cow.addEffect(new MobEffectInstance(MobEffects.SPEED, 10, 0, true, false, false)), "short hidden speed admitted");
        require(speed.getValue() == value && speed.getModifiers().iterator().next() == modifier,
                "hidden grant does not replace active attribute modifier");
        var active = cow.getEffect(MobEffects.SPEED);
        require(active.isVisible() && active.showIcon() && !active.isAmbient(), "hidden flags do not alter active flags");
        layer(hidden(active), 0, 10);
        require(cow.addEffect(new MobEffectInstance(MobEffects.SPEED, 1, 0, true, false, false)),
                "unchanged hidden same-amplifier grant still succeeds");
        require(speed.getModifiers().iterator().next() == modifier, "repeat hidden grant leaves modifiers untouched");
        cow.removeAllEffects();
        command(game, cow, "2 100 true");
        command(game, cow, "1 -100 false");
        command(game, cow, "10 -200 true");
        command(game, cow, "1 -200 true");
        active = cow.getEffect(MobEffects.GLOWING);
        layer(active, 100, 40);
        layer(hidden(active), -100, 20);
        layer(hidden(hidden(active)), -200, 200);
        var ops = cow.level().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var encoded = MobEffectInstance.CODEC.encodeStart(ops, active).getOrThrow();
        var restored = MobEffectInstance.CODEC.parse(ops, encoded).getOrThrow();
        require(MobEffectInstance.CODEC.encodeStart(ops, restored).getOrThrow().equals(encoded), "recursive persistent codec retains all layers");
        var raw = Unpooled.buffer();
        try {
            var buffer = new RegistryFriendlyByteBuf(raw, cow.level().registryAccess());
            MobEffectInstance.STREAM_CODEC.encode(buffer, active);
            var decoded = MobEffectInstance.STREAM_CODEC.decode(buffer);
            require(raw.readableBytes() == 0 && MobEffectInstance.CODEC.encodeStart(ops, decoded).getOrThrow().equals(encoded),
                    "recursive wire codec retains all layers");
        } finally { raw.release(); }
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, cow.level().registryAccess());
        cow.saveWithoutId(output);
        var loaded = new Cow(EntityTypes.COW, cow.level());
        try {
            loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, cow.level().registryAccess(), output.buildResult()));
            var saved = loaded.getEffect(MobEffects.GLOWING);
            require(saved != null && MobEffectInstance.CODEC.encodeStart(ops, saved).getOrThrow().equals(encoded), "entity save/load retains layers");
            tick(saved, loaded, 40);
            layer(saved, -200, 160);
        } finally { loaded.discard(); }
        cow.forceAddEffect(effect(12, -7), null);
        layer(cow.getEffect(MobEffects.GLOWING), -7, 12);
        require(hidden(cow.getEffect(MobEffects.GLOWING)) == null, "forceAddEffect remains exact replacement");
        command(game, cow, "infinite 2000000000 true");
        command(game, cow, "infinite -2000000000 true");
        layer(hidden(cow.getEffect(MobEffects.GLOWING)), -7, 12);
        layer(hidden(hidden(cow.getEffect(MobEffects.GLOWING))), -2000000000, -1);
        cow.removeAllEffects();
        require(cow.getActiveEffects().isEmpty(), "native clear removes all layers");
        require(cow.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0)), "positive absorption admitted");
        require(cow.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 50, -2)), "negative absorption admitted");
        require(ShadowAbsorption.state(cow).entries.size() == 2 && ShadowAbsorption.net(ShadowAbsorption.state(cow)) == 0,
                "shadow absorption retains separate additive ledger");
        cow.removeAllEffects();
    }
    public static void run(GameController game) {
        require(game.playing(), "actual gameplay match");
        var owner = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(owner != null, "native TestAlpha fixture");
        var cow = new Cow(EntityTypes.COW, owner.level());
        cow.setPos(owner.position().add(0, 8, 0));
        cow.setNoAi(true);
        cow.setNoGravity(true);
        require(owner.level().addFreshEntity(cow), "fixture admitted");
        try {
            clocks(cow);
            entityAndStorage(game, cow);
        } finally { cow.discard(); }
        SkyWars2.LOGGER.info("[check] PASS effect-layers (both grant orders, independent signed clocks, expired intermediates, same-level maximum, infinite layers, active flags/modifier stability, real finite/infinite commands, recursive codecs/entity save, exact force replacement, clear and separate shadow absorption)");
    }
}
