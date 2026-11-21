package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Native-entity tests for the absorption ledger, invoked by the existing isolated check runner. */
public final class ShadowAbsorptionChecks {
    private ShadowAbsorptionChecks() {}
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Shadow absorption: " + message);
    }
    private static void close(double actual, double expected, String message) {
        require(Math.abs(actual - expected) < .001, message + ": " + actual + " != " + expected);
    }
    private static Cow cow(ServerLevel level) {
        var cow = new Cow(EntityTypes.COW, level);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);
        cow.setHealth(20);
        cow.setNoAi(true);
        cow.setNoGravity(true);
        return cow;
    }
    private static void effect(Cow cow, int duration, int amplifier) {
        require(cow.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier)), "stack admitted " + amplifier);
    }
    private static void nativeFood(GameController game, ServerPlayer player) {
        require(!player.hasEffect(MobEffects.ABSORPTION) && ShadowAbsorption.state(player).entries.isEmpty(),
                "food fixture begins without absorption");
        require(!LbcGreed.active(player), "food fixture player is outside Greed");
        float oldHealth = player.getHealth(), oldAbsorption = player.getAbsorptionAmount();
        var level = player.level();
        var rules = level.getGameRules();
        boolean naturalRegen = rules.get(GameRules.NATURAL_HEALTH_REGENERATION);
        var savedFood = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        player.getFoodData().addAdditionalSaveData(savedFood);
        try {
            rules.set(GameRules.NATURAL_HEALTH_REGENERATION, true, game.server());
            float maximum = player.getMaxHealth();
            require(maximum > 4 && Float.isFinite(maximum), "food fixture has enough visible maximum HP");
            player.setHealth(maximum - 4);
            require(player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, -2), player), "native player negative stack");
            close(player.getHealth(), player.getMaxHealth(), "food fixture starts at full visible HP");
            close(ShadowAbsorption.state(player).stored, 0, "food fixture starts with empty shadow HP");
            require(player.isHurt(), "native Player.isHurt admits healing of empty shadow HP");
            var food = player.getFoodData();
            // Reset all four native food fields, including its elapsed regeneration timer.
            var fresh = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            fresh.putInt("foodLevel", 20); fresh.putInt("foodTickTimer", 0);
            fresh.putFloat("foodSaturationLevel", 6); fresh.putFloat("foodExhaustionLevel", 0);
            food.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), fresh.buildResult()));
            for (int tick = 0; tick < 10; tick++) food.tick(player);
            close(player.getHealth(), player.getMaxHealth(), "actual food regeneration keeps visible HP full");
            close(ShadowAbsorption.state(player).stored, 1, "actual saturated native food regeneration heals shadow HP");
            player.removeEffect(MobEffects.ABSORPTION);
            close(player.getHealth(), maximum - 3, "food-healed shadow HP returns on removal");

            player.setHealth(maximum - 4);
            require(player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, -2), player), "second native player negative stack");
            fresh.putInt("foodLevel", 18); fresh.putInt("foodTickTimer", 0);
            fresh.putFloat("foodSaturationLevel", 0); fresh.putFloat("foodExhaustionLevel", 0);
            food.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), fresh.buildResult()));
            for (int tick = 0; tick < 80; tick++) food.tick(player);
            close(ShadowAbsorption.state(player).stored, 1, "actual unsaturated native food regeneration heals shadow HP");
        } finally {
            player.removeEffect(MobEffects.ABSORPTION);
            player.setHealth(oldHealth);
            player.setAbsorptionAmount(oldAbsorption);
            player.getFoodData().readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING,
                    level.registryAccess(), savedFood.buildResult()));
            rules.set(GameRules.NATURAL_HEALTH_REGENERATION, naturalRegen, game.server());
        }
    }
    private static void nativeGuardsAndAttributes(GameController game, ServerPlayer source) {
        var level = source.level();
        var target = cow(level);
        var time = game.items().timeStop();
        require(!time.stopped(level), "guard fixture begins outside Time Stop");
        try {
            target.entityTags().add("sin_greed_active");
            require(!target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 10, -2), source),
                    "Greed rejects externally sourced negative absorption");
            require(!target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 10, 0), source),
                    "Greed preserves absolute external positive-effect rejection too");
            require(ShadowAbsorption.state(target).entries.isEmpty() && target.getHealth() == 20
                    && target.getMaxHealth() == 20, "Greed rejection leaves health and ledger unchanged");
            target.entityTags().remove("sin_greed_active");

            effect(target, 3, -2);
            time.begin(level, 20);
            try {
                require(time.frozen(target), "actual Time Stop recognizes ledger fixture");
                for (int tick = 0; tick < 5; tick++) target.tick();
                require(ShadowAbsorption.state(target).entries.get(-2).getDuration() == 3,
                        "native ticking during actual Time Stop does not consume ledger duration");
                close(target.getMaxHealth(), 16, "paused negative maximum preserved");
            } finally { time.end(level); }
            target.tick(); target.tick(); target.tick();
            require(!target.hasEffect(MobEffects.ABSORPTION), "resumed native clock expires at original remaining duration");
            close(target.getHealth(), 20, "Time Stop resume and expiration restore shadow HP");

            require(target.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 2, 1)), "native health boost admitted");
            close(target.getMaxHealth(), 28, "ordinary health boost native maximum");
            target.setHealth(28);
            effect(target, 100, -2);
            close(target.getMaxHealth(), 24, "negative absorption subtracts from boosted maximum");
            target.tick(); target.tick();
            close(target.getMaxHealth(), 16, "health boost expiration reconciles concealed maximum");
            target.removeAllEffects();
            close(target.getMaxHealth(), 20, "boost and absorption cleanup restores base maximum");

            var item = new ItemStack(Items.STICK);
            item.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                    .add(Attributes.MAX_HEALTH, new AttributeModifier(Identifier.fromNamespaceAndPath("skywars2", "shadow_check_add"),
                            10, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.MAX_HEALTH, new AttributeModifier(Identifier.fromNamespaceAndPath("skywars2", "shadow_check_total"),
                            .5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND).build());
            target.setItemSlot(EquipmentSlot.MAINHAND, item);
            target.tick();
            close(target.getMaxHealth(), 45, "actual native equipment applies additive and multiplicative maximum modifiers");
            target.setHealth(45);
            effect(target, 100, -2);
            close(target.getMaxHealth(), 41, "shadow reduction remains exactly four HP through equipment multipliers");
            target.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            target.tick();
            close(target.getMaxHealth(), 16, "actual unequip reconciles shadow against new underlying maximum");
            target.removeAllEffects();
            close(target.getMaxHealth(), 20, "unequipped cleanup restores base maximum");

            target.setHealth(20);
            target.setInvulnerableTime(0); target.damageCooldownTime = 0;
            effect(target, 100, 0);
            require(target.hurtServer(level, target.damageSources().generic(), 3), "ordinary positive absorption native damage");
            close(target.getHealth(), 20, "ordinary positive absorption protects visible HP");
            close(target.getAbsorptionAmount(), 1, "ordinary native damage spends yellow HP");
            target.setInvulnerableTime(0); target.damageCooldownTime = 0;
            require(target.hurtServer(level, target.damageSources().generic(), 2), "native positive pool exhaustion hit");
            close(target.getHealth(), 19, "only yellow overflow reaches native visible HP");
            target.tick();
            require(!target.hasEffect(MobEffects.ABSORPTION), "native next tick removes exhausted positive-only effect");

            var minimumItem = new ItemStack(Items.STICK);
            minimumItem.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                    .add(Attributes.MAX_HEALTH, new AttributeModifier(Identifier.fromNamespaceAndPath("skywars2", "shadow_check_minimum"),
                            -100, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build());
            target.setItemSlot(EquipmentSlot.MAINHAND, minimumItem);
            target.tick();
            close(target.getMaxHealth(), 1, "native equipment maximum-health lower bound remains one");
            effect(target, 100, -2);
            close(target.getMaxHealth(), 0, "concealment may exhaust native minimum available maximum");
            require(target.isDeadOrDying() && Float.isFinite(target.getHealth()), "native minimum concealment is finite and lethal");
        } finally {
            target.entityTags().remove("sin_greed_active");
            if (time.stopped(level)) time.end(level);
            target.discard();
        }
    }
    public static void run(GameController game) {
        var owner = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(owner != null && game.playing(), "native match participant");
        var level = owner.level();
        var cow = cow(level);
        var loaded = cow(level);
        try {
            nativeFood(game, owner);
            nativeGuardsAndAttributes(game, owner);
            cow.setHealth(12);
            effect(cow, 100, -2);
            close(cow.getMaxHealth(), 16, "negative level conceals four maximum HP");
            close(cow.getHealth(), 12, "concealing empty top hearts preserves visible health");
            close(ShadowAbsorption.state(cow).stored, 0, "empty shadow hearts stored");
            cow.heal(8);
            close(cow.getHealth(), 16, "native heal fills visible hearts");
            close(ShadowAbsorption.state(cow).stored, 4, "same native heal overflow fills shadow hearts");
            require(!ShadowAbsorption.needsHealing(cow), "both health pools full");
            require(cow.hurtServer(level, cow.damageSources().generic(), 4), "native damage admitted");
            close(cow.getHealth(), 12, "native damage only spends visible health");
            close(ShadowAbsorption.state(cow).stored, 4, "native damage cannot spend shadow health");
            cow.removeEffect(MobEffects.ABSORPTION);
            close(cow.getMaxHealth(), 20, "explicit effect removal restores maximum");
            close(cow.getHealth(), 16, "explicit removal returns current healed shadow amount");

            cow.setHealth(20);
            effect(cow, 5, -3); // -8 HP
            effect(cow, 2, 0);  // +4 HP: independent counterweight
            close(cow.getMaxHealth(), 16, "opposite levels add before concealment");
            close(cow.getAbsorptionAmount(), 0, "negative net has no yellow damage pool");
            cow.tick(); cow.tick();
            require(ShadowAbsorption.state(cow).entries.size() == 1, "positive expires independently");
            close(cow.getMaxHealth(), 12, "counterweight expiry exposes remaining negative level");
            cow.tick(); cow.tick(); cow.tick();
            require(!cow.hasEffect(MobEffects.ABSORPTION), "final stack expires in native entity ticks");
            close(cow.getHealth(), 20, "expiration restores healed shadow hearts");

            effect(cow, 100, 0);
            close(cow.getAbsorptionAmount(), 4, "native positive contribution");
            cow.setAbsorptionAmount(1);
            require(!cow.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1, 0)), "shorter same level keeps duration");
            close(cow.getAbsorptionAmount(), 4, "same positive reapplication refills natively");
            effect(cow, 50, 1);
            close(cow.getAbsorptionAmount(), 12, "distinct positive levels add");
            cow.removeAllEffects();
            close(cow.getAbsorptionAmount(), 0, "milk/all-effects clear removes yellow capacity");

            cow.setHealth(12);
            effect(cow, 100, -2);
            require(ShadowAbsorption.needsHealing(cow), "healing admission includes empty shadow hearts");
            cow.heal(6);
            close(ShadowAbsorption.state(cow).stored, 2, "partial shadow healing");
            effect(cow, 17, -1); // real level zero retains an independently expiring entry
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            cow.saveWithoutId(output);
            loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
            close(loaded.getHealth(), 16, "native entity load preserves visible HP");
            close(loaded.getMaxHealth(), 16, "native load rebuilds transient concealment");
            close(ShadowAbsorption.state(loaded).stored, 2, "native entity load preserves healed shadow HP");
            require(ShadowAbsorption.state(loaded).entries.size() == 2, "native entity load preserves independent levels");
            loaded.removeAllEffects();
            close(loaded.getHealth(), 18, "loaded clear returns only the healed shadow amount");

            cow.removeAllEffects(); cow.setHealth(20);
            effect(cow, 100, -2);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
            cow.tick();
            close(cow.getMaxHealth(), 36, "native maximum-health change reconciled");
            cow.removeAllEffects(); cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20); cow.setHealth(20);
            effect(cow, 100, Integer.MIN_VALUE);
            require(Float.isFinite(cow.getMaxHealth()) && cow.getHealth() == 0 && cow.isDeadOrDying(),
                    "full negative int can exhaust all visible life with bounded arithmetic");
            require(ShadowAbsorption.level(Integer.MAX_VALUE) == 2147483648L
                    && ShadowAbsorption.level(Integer.MIN_VALUE) == -2147483647L, "full signed-int level conversion");
            loaded.setHealth(20);
            effect(loaded, 100, Integer.MAX_VALUE);
            require(Float.isFinite(loaded.getAbsorptionAmount()), "full positive int yellow HP stays finite");
            close(loaded.getAbsorptionAmount(), 8589934592.0, "full positive int has no amplifier overflow");
            loaded.removeAllEffects();
            effect(loaded, 100, -2);
            loaded.hurtServer(level, loaded.damageSources().genericKill(), 100);
            require(loaded.isDeadOrDying() && ShadowAbsorption.state(loaded).stored > 0,
                    "native lethal visible damage kills even with healed shadow hearts remaining");
        } finally { cow.discard(); loaded.discard(); }
        SkyWars2.LOGGER.info("[check] PASS shadow-absorption (actual saturated/unsaturated Player food regeneration, Greed rejection, Time Stop pause/resume, native health boost/equipment changes, native heal/damage/death, signed additive levels, native tick expiration, reapply/clear, current healed shadow restoration, max-health changes, and entity persistence)");
    }
}
