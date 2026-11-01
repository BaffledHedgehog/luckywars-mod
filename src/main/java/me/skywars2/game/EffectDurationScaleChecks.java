package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;

/** Synchronous actual-native checks, selected by the isolated match runner. */
public final class EffectDurationScaleChecks {
    private EffectDurationScaleChecks() { }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Effect duration scale: " + message);
    }
    private static MobEffectInstance hidden(MobEffectInstance effect) {
        return ((EffectLayers.Access)effect).skywars2$layerHidden();
    }
    private static void layer(MobEffectInstance effect, int duration, int amplifier,
                              boolean ambient, boolean particles, boolean icon) {
        require(effect != null && effect.getDuration() == duration && effect.getAmplifier() == amplifier
                && effect.isAmbient() == ambient && effect.isVisible() == particles && effect.showIcon() == icon,
                "clock/amplifier/flags " + duration + "/" + amplifier);
    }
    private static Cow fixture(ServerLevel level) {
        var cow = new Cow(EntityTypes.COW, level);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);
        cow.setHealth(20);
        cow.setNoAi(true);
        cow.setNoGravity(true);
        return cow;
    }
    private static void nativeLayers(GameController game, Cow cow) {
        require(!EffectLayers.multiplyDurations(game, cow, 5), "empty is unchanged");
        require(cow.addEffect(new MobEffectInstance(MobEffects.GLOWING, 3, Integer.MAX_VALUE, true, false, true)), "max signed admitted");
        require(cow.addEffect(new MobEffectInstance(MobEffects.GLOWING, -1, 0, false, true, false)), "hidden infinite admitted");
        require(cow.addEffect(new MobEffectInstance(MobEffects.GLOWING, Integer.MAX_VALUE - 1, Integer.MIN_VALUE, true, false, false)), "min signed admitted");
        require(cow.addEffect(new MobEffectInstance(MobEffects.SPEED, 7, 1)), "speed admitted");
        require(cow.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 9, 0)), "health boost admitted");
        // A retained instant-effect clock must not execute its effect during multiplication.
        cow.forceAddEffect(new MobEffectInstance(MobEffects.INSTANT_HEALTH, 4, 0), null);
        cow.setHealth(7);
        var active = cow.getEffect(MobEffects.GLOWING);
        var middle = hidden(active);
        var last = hidden(middle);
        var movement = cow.getAttribute(Attributes.MOVEMENT_SPEED);
        var modifier = movement.getModifiers().iterator().next();
        var maxHealth = cow.getAttribute(Attributes.MAX_HEALTH);
        var healthModifier = maxHealth.getModifiers().iterator().next();
        double speed = movement.getValue(), maximum = maxHealth.getValue();
        require(EffectLayers.multiplyDurations(game, cow, 5), "finite native clocks changed");
        require(cow.getEffect(MobEffects.GLOWING) == active && hidden(active) == middle && hidden(middle) == last,
                "native chain identities retained");
        layer(active, 15, Integer.MAX_VALUE, true, false, true);
        layer(middle, -1, 0, false, true, false);
        layer(last, Integer.MAX_VALUE, Integer.MIN_VALUE, true, false, false);
        require(cow.getEffect(MobEffects.SPEED).getDuration() == 35, "other effect scaled");
        require(cow.getEffect(MobEffects.HEALTH_BOOST).getDuration() == 45, "health boost clock scaled");
        require(cow.getEffect(MobEffects.INSTANT_HEALTH).getDuration() == 20 && cow.getHealth() == 7,
                "no instant healing or HP reset");
        require(movement.getValue() == speed && movement.getModifiers().iterator().next() == modifier
                && maxHealth.getValue() == maximum && maxHealth.getModifiers().iterator().next() == healthModifier,
                "attribute modifier identities and values retained");
        for (int i = 0; i < 15; i++) require(active.tickServer((ServerLevel)cow.level(), cow, () -> { }), "native live tick");
        layer(active, -1, 0, false, true, false);
        layer(hidden(active), Integer.MAX_VALUE - 15, Integer.MIN_VALUE, true, false, false);
        cow.removeAllEffects();
        cow.addEffect(new MobEffectInstance(MobEffects.GLOWING, -1, Integer.MIN_VALUE));
        require(!EffectLayers.multiplyDurations(game, cow, 5), "infinite-only unchanged");
        cow.removeAllEffects();
        cow.addEffect(new MobEffectInstance(MobEffects.GLOWING, Integer.MAX_VALUE, Integer.MAX_VALUE));
        require(!EffectLayers.multiplyDurations(game, cow, Integer.MAX_VALUE), "saturated clock unchanged without overflow");
        cow.removeAllEffects();
    }
    private static void owned(GameController game, Cow cow) {
        var leases = game.items().ownedEffects();
        var baseline = new MobEffectInstance(MobEffects.GLOWING, 7, -2, true, false, false);
        require(cow.addEffect(baseline), "baseline admitted");
        require(leases.grant(cow, "duration-check-a", new MobEffectInstance(MobEffects.GLOWING, 9, -2), null), "first lease");
        require(leases.grant(cow, "duration-check-b", new MobEffectInstance(MobEffects.GLOWING, 11, -2), null), "second lease");
        require(leases.grant(cow, "duration-check-high", new MobEffectInstance(MobEffects.GLOWING, 2, 3), null), "higher lease");
        require(EffectLayers.multiplyDurations(game, cow, 5), "owned projection and clocks scaled");
        leases.withdraw(cow, "duration-check-high");
        require(cow.getEffect(MobEffects.GLOWING).getDuration() == 55, "scaled lower owned layer survives withdrawal");
        leases.withdraw(cow, "duration-check-b");
        require(cow.getEffect(MobEffects.GLOWING).getDuration() == 45, "scaled independent second owner survives");
        var active = cow.getEffect(MobEffects.GLOWING);
        for (int i = 0; i < 3; i++) require(active.tickServer((ServerLevel)cow.level(), cow, () -> { }), "owned live tick");
        leases.withdraw(cow, "duration-check-a");
        layer(cow.getEffect(MobEffects.GLOWING), 32, -2, true, false, false);
        cow.removeAllEffects();
        require(leases.grant(cow, "duration-check-expiry", new MobEffectInstance(MobEffects.GLOWING, 1, 0), null), "expiry lease");
        require(EffectLayers.multiplyDurations(game, cow, 5), "expiry lease scaled");
        active = cow.getEffect(MobEffects.GLOWING);
        for (int i = 0; i < 4; i++) require(active.tickServer((ServerLevel)cow.level(), cow, () -> { }), "scaled lease remains live");
        require(!active.tickServer((ServerLevel)cow.level(), cow, () -> { }), "scaled lease expires on fifth tick");
        cow.removeAllEffects();
        leases.withdraw(cow, "duration-check-expiry");
        require(!EffectLayers.multiplyDurations(game, cow, 5), "expired ownership removed");
    }
    private static void absorption(GameController game, Cow cow) {
        cow.setHealth(20);
        require(cow.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 3, 0)), "positive absorption admitted");
        require(cow.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, -1, 1)), "infinite absorption admitted");
        cow.setAbsorptionAmount(5);
        var state = ShadowAbsorption.state(cow);
        var carrier = cow.getEffect(MobEffects.ABSORPTION);
        var positive = state.entries.get(0);
        var yellow = cow.getAttribute(Attributes.MAX_ABSORPTION);
        var yellowModifier = yellow.getModifiers().iterator().next();
        require(EffectLayers.multiplyDurations(game, cow, 5), "positive ledger scaled");
        require(state.entries.get(0) == positive && positive.getDuration() == 15
                && state.entries.get(1).isInfiniteDuration() && cow.getEffect(MobEffects.ABSORPTION) == carrier,
                "absorption ledger and native carrier identities preserved");
        require(cow.getAbsorptionAmount() == 5 && cow.getHealth() == 20
                && yellow.getModifiers().iterator().next() == yellowModifier, "no absorption refill or attribute reset");
        cow.removeAllEffects();
        cow.setHealth(20);
        require(cow.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 7, -3, true, false, false)), "shadow layer admitted");
        require(cow.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2, 0)), "shadow counterweight admitted");
        state = ShadowAbsorption.state(cow);
        double capacity = state.capacity, stored = state.stored, maximum = state.visibleMaximum;
        float health = cow.getHealth(), absorption = cow.getAbsorptionAmount();
        var shadow = state.entries.get(-3);
        var healthModifier = cow.getAttribute(Attributes.MAX_HEALTH).getModifiers().iterator().next();
        require(EffectLayers.multiplyDurations(game, cow, 5), "shadow clocks scaled");
        layer(shadow, 35, -3, true, false, false);
        require(state.entries.get(-3) == shadow && state.entries.get(0).getDuration() == 10
                && state.capacity == capacity && state.stored == stored && state.visibleMaximum == maximum
                && cow.getHealth() == health && cow.getAbsorptionAmount() == absorption
                && cow.getAttribute(Attributes.MAX_HEALTH).getModifiers().iterator().next() == healthModifier,
                "shadow HP, native HP, counters and attribute identity preserved");
        for (int i = 0; i < 9; i++) ShadowAbsorption.tick(cow);
        require(state.entries.get(0).getDuration() == 1 && state.entries.get(-3).getDuration() == 26,
                "independent scaled absorption clocks count down");
        ShadowAbsorption.tick(cow);
        require(!state.entries.containsKey(0) && state.entries.get(-3).getDuration() == 25 && state.capacity == 8,
                "counterweight expires at scaled boundary and reconciles shadow capacity");
        cow.removeAllEffects();
    }
    public static void run(GameController game) {
        require(game.playing(), "actual gameplay match");
        var owner = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(owner != null, "native TestAlpha fixture");
        var cow = fixture(owner.level());
        cow.setPos(owner.position().add(0, 8, 0));
        require(owner.level().addFreshEntity(cow), "fixture admitted");
        try {
            nativeLayers(game, cow);
            owned(game, cow);
            absorption(game, cow);
        } finally {
            game.items().ownedEffects().forget(cow);
            cow.discard();
        }
        SkyWars2.LOGGER.info("[check] PASS effect-duration-scale (native hidden clocks, signed-int amplifiers, flags, infinite/saturating ticks, no instant replay/HP or modifier reset, owned baseline/lease withdrawal and expiry, positive absorption and shadow HP preservation/expiry)");
    }
}
