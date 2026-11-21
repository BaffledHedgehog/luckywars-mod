package me.skywars2.game;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import me.skywars2.mixin.combat.CombatCheckpointAccess;
import me.skywars2.mixin.entities.EntityRuntimeCheckpointAccess;
import me.skywars2.validation.mixin.ItemFixtureAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

/** Exact native state shared by the accessory and Quiver fixtures. */
final class ItemFixtureChecks {
    private ItemFixtureChecks() {}

    static Map<EquipmentSlot, ItemStack> equipment(ServerPlayer player) {
        return ((ItemFixtureAccess.Equipment) player).validation$equipment();
    }

    static Map<EquipmentSlot, ItemStack> captureEquipment(ServerPlayer player) {
        var saved = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);
        equipment(player).forEach((slot, item) -> saved.put(slot, item.copy()));
        return saved;
    }

    static void restoreEquipment(ServerPlayer player, Map<EquipmentSlot, ItemStack> saved) {
        var live = equipment(player);
        live.clear();
        saved.forEach((slot, item) -> live.put(slot, item.copy()));
    }

    static void clearMotionInput(ServerPlayer player) {
        player.xxa = 0;
        player.yya = 0;
        player.zza = 0;
        ((EntityRuntimeCheckpointAccess.LivingEntityAccess) player).skywars2$runtimeJumping(false);
    }

    record Combat(List<CombatEntry> entries, int last, int start, int end, boolean combat, boolean takingDamage) {
        static Combat capture(ServerPlayer player) {
            var live = (CombatCheckpointAccess.Tracker) player.getCombatTracker();
            return new Combat(List.copyOf(live.skywars2$entries()), live.skywars2$last(), live.skywars2$start(),
                    live.skywars2$end(), live.skywars2$combat(), live.skywars2$taking());
        }

        void restore(ServerPlayer player) {
            var live = (CombatCheckpointAccess.Tracker) player.getCombatTracker();
            live.skywars2$entries().clear();
            live.skywars2$entries().addAll(entries);
            live.skywars2$last(last);
            live.skywars2$start(start);
            live.skywars2$end(end);
            live.skywars2$combat(combat);
            live.skywars2$taking(takingDamage);
        }
    }

    record PlayerTicks(int attack, int swap, float inputX, float inputY, float inputZ, boolean jumping,
                       float head, float previousHead, int hurt, int hurtDuration, float lastHurt,
                       long damageStamp, DamageSource damage) {
        static PlayerTicks capture(ServerPlayer player) {
            var living = (EntityRuntimeCheckpointAccess.LivingEntityAccess) player;
            var combat = (CombatCheckpointAccess.Living) player;
            return new PlayerTicks(living.skywars2$runtimeAttackStrengthTicker(), living.skywars2$runtimeItemSwapTicker(),
                    player.xxa, player.yya, player.zza, living.skywars2$runtimeJumping(), player.yHeadRot,
                    player.yHeadRotO, player.hurtTime, player.hurtDuration, living.skywars2$runtimeLastHurt(),
                    combat.skywars2$damageTime(), combat.skywars2$damage());
        }

        void restore(ServerPlayer player, boolean restoreDamageSource) {
            var living = (EntityRuntimeCheckpointAccess.LivingEntityAccess) player;
            var combat = (CombatCheckpointAccess.Living) player;
            living.skywars2$runtimeAttackStrengthTicker(attack);
            living.skywars2$runtimeItemSwapTicker(swap);
            player.xxa = inputX;
            player.yya = inputY;
            player.zza = inputZ;
            living.skywars2$runtimeJumping(jumping);
            player.yHeadRot = head;
            player.yHeadRotO = previousHead;
            player.hurtTime = hurt;
            player.hurtDuration = hurtDuration;
            living.skywars2$runtimeLastHurt(lastHurt);
            combat.skywars2$damageTime(damageStamp);
            if (restoreDamageSource) combat.skywars2$damage(damage);
        }
    }

    record PlayerSync(int recordedFood, int sentFood, boolean zeroSaturation, float sentHealth, float recordedHealth) {
        static PlayerSync capture(ServerPlayer player) {
            var view = (ItemFixtureAccess.PlayerSync) player;
            return new PlayerSync(view.validation$recordedFood(), view.validation$sentFood(), view.validation$zeroSaturation(),
                    view.validation$sentHealth(), view.validation$recordedHealth());
        }

        void restore(ServerPlayer player) {
            var view = (ItemFixtureAccess.PlayerSync) player;
            view.validation$recordedFood(recordedFood);
            view.validation$sentFood(sentFood);
            view.validation$zeroSaturation(zeroSaturation);
            view.validation$sentHealth(sentHealth);
            view.validation$recordedHealth(recordedHealth);
        }
    }

    record Nutrition(int food, float saturation, float exhaustion, int timer) {
        static Nutrition capture(FoodData food) {
            var data = (ItemFixtureAccess.Food) food;
            return new Nutrition(food.getFoodLevel(), food.getSaturationLevel(), data.validation$exhaustion(),
                    data.validation$timer());
        }

        void restore(FoodData data) {
            var nativeData = (ItemFixtureAccess.Food) data;
            data.setFoodLevel(food);
            data.setSaturation(saturation);
            nativeData.validation$exhaustion(exhaustion);
            nativeData.validation$timer(timer);
        }
    }

    static void random(ServerLevel level, RandomSource random) {
        ((ItemFixtureAccess.LevelRandom) level).validation$random(random);
    }

    static class DelegatingRandom implements RandomSource {
        protected final RandomSource delegate;

        DelegatingRandom(RandomSource delegate) { this.delegate = delegate; }

        @Override public RandomSource fork() { return delegate.fork(); }
        @Override public PositionalRandomFactory forkPositional() { return delegate.forkPositional(); }
        @Override public void setSeed(long seed) { delegate.setSeed(seed); }
        @Override public int nextInt() { return delegate.nextInt(); }
        @Override public int nextInt(int bound) { return delegate.nextInt(bound); }
        @Override public int nextInt(int origin, int bound) { return delegate.nextInt(origin, bound); }
        @Override public int nextIntBetweenInclusive(int min, int max) { return delegate.nextIntBetweenInclusive(min, max); }
        @Override public long nextLong() { return delegate.nextLong(); }
        @Override public boolean nextBoolean() { return delegate.nextBoolean(); }
        @Override public float nextFloat() { return delegate.nextFloat(); }
        @Override public double nextDouble() { return delegate.nextDouble(); }
        @Override public double nextGaussian() { return delegate.nextGaussian(); }
        @Override public double triangle(double mean, double spread) { return delegate.triangle(mean, spread); }
        @Override public float triangle(float mean, float spread) { return delegate.triangle(mean, spread); }
        @Override public void consumeCount(int rounds) { delegate.consumeCount(rounds); }
    }
}
