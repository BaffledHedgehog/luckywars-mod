package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player Overload armor movement passives; equipment identity is owned by LbcMana's dirty cache. */
public final class LbcOverloadArmor {
    private static final String JUMP_TIMER = "overload_jump";
    private static final String SLAM_TIMER = "overload_slam";
    private static final String SLAM_TAG = "overload_slam";
    private static final Identifier GRAVITY_ID = Identifier.parse("minecraft:slamslamslaaaaaaamslaaam");
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> SOURCE_MOBS =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:mobs"));
    private static final TagKey<net.minecraft.world.level.block.Block> SOURCE_AIRS =
            TagKey.create(Registries.BLOCK, Identifier.parse("minecraft:airs"));

    private final GameController game;
    private final LbcMana mana;
    private final Map<UUID, Integer> ownedLevitation = new HashMap<>();

    public LbcOverloadArmor(GameController game, LbcMana mana) {
        this.game = game;
        this.mana = mana;
    }

    /** Called by the native jump hook. The user can jump repeatedly; each activation restarts its timer. */
    public void jump(ServerPlayer player) {
        if (!active(player) || !player.isShiftKeyDown() || (mana.overloadArmor(player) & 4) == 0) return;

        int amplifier = mana.get(player) <= -20001 ? 127 : 76;
        clearOwnedLevitation(player);
        if (player.addEffect(ItemEffectLevels.create(MobEffects.LEVITATION, 20, amplifier, false, false, false))) {
            var effect = player.getEffect(MobEffects.LEVITATION);
            if (effect != null && effect.getAmplifier() == amplifier) ownedLevitation.put(player.getUUID(), amplifier);
        }
        player.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE, 40, 4, false, false, false));
        if (!game.items().magic().rejectCustomEffect(player)) mana.score(player, JUMP_TIMER).set(20);

        drainNearbyPlayers(player, 4, 2500, true, false);
        var level = (ServerLevel) player.level();
        level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH,1), player.getX(), player.getY(), player.getZ(),
                100, .5, 1, .5, .1);
        LbcOverload.sound(player, "minecraft:lbcsounds.overload_jump", 2, 1, 40);
    }

    /** Called once per server tick for a player; time stop pauses the source movement state. */
    public void tick(ServerPlayer player) {
        if (player.level() != game.maps().level() || player.isRemoved() || !player.isAlive()) {
            if (ownsTransientState(player)) clear(player);
            return;
        }
        if (player.entityTags().contains("time_stopped")) return;
        if (!mana.visible(player)) {
            if (ownsTransientState(player)) clear(player);
            return;
        }
        if (game.items().magic().rejectCustomEffect(player)) {
            if (ownsTransientState(player)) clear(player);
            return;
        }

        var jump = mana.score(player, JUMP_TIMER);
        int jumpTicks = jump.get();
        if (jumpTicks > 0) {
            if (jumpTicks == 18) clearOwnedLevitation(player);
            jump.set(jumpTicks - 1);
        }

        var slam = mana.score(player, SLAM_TIMER);
        int slamTicks = slam.get();
        if ((mana.overloadArmor(player) & 8) != 0 && player.isShiftKeyDown() && !player.onGround()
                && jump.get() <= 0 && slamTicks == 0 && hasFreeFallColumn(player)) {
            startSlam(player);
            slamTicks = slam.get();
        }
        if (!player.entityTags().contains(SLAM_TAG)) return;
        if (jump.get() == 1) clearOwnedLevitation(player);

        var localRight = net.minecraft.world.phys.Vec3.directionFromRotation(0, player.getYRot() - 90).scale(.2);
        player.level().sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH,1),
                player.getX() + localRight.x, player.getY(), player.getZ() + localRight.z, 1, 0, 0, 0, 1);
        player.level().sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH,1),
                player.getX() - localRight.x, player.getY(), player.getZ() - localRight.z, 1, 0, 0, 0, 1);
        player.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE, 20, 4, false, false, false));
        slamTicks += 2;
        slam.set(slamTicks);

        if (player.onGround() || !player.level().getBlockState(player.blockPosition().below()).is(SOURCE_AIRS))
            impact(player, slamTicks);
    }

    /** Remove transient timers, the owned jump effect, and the one gravity modifier owned by this ability. */
    public void clear(ServerPlayer player) {
        mana.score(player, JUMP_TIMER).set(0);
        mana.score(player, SLAM_TIMER).set(0);
        clearOwnedLevitation(player);
        player.entityTags().remove(SLAM_TAG);
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity != null) gravity.removeModifier(GRAVITY_ID);
    }

    private boolean active(ServerPlayer player) {
        return mana.visible(player) && !player.entityTags().contains("time_stopped");
    }

    private boolean ownsTransientState(ServerPlayer player) {
        UUID id = player.getUUID();
        if (ownedLevitation.containsKey(id) || player.entityTags().contains(SLAM_TAG)) return true;
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity != null && gravity.getModifier(GRAVITY_ID) != null) return true;
        var board = game.server().getScoreboard();
        for (String name : new String[]{JUMP_TIMER, SLAM_TIMER}) {
            var objective = board.getObjective(name);
            var value = objective == null ? null : board.getPlayerScoreInfo(player, objective);
            if (value != null && value.value() > 0) return true;
        }
        return false;
    }

    private void clearOwnedLevitation(ServerPlayer player) {
        Integer amplifier = ownedLevitation.remove(player.getUUID());
        if (amplifier == null) return;
        MobEffectInstance effect = player.getEffect(MobEffects.LEVITATION);
        // Do not erase a later, longer levitation applied by another mechanic.
        if (effect != null && effect.getAmplifier() == amplifier && effect.getDuration() <= 20)
            player.removeEffect(MobEffects.LEVITATION);
    }

    private void startSlam(ServerPlayer player) {
        if (game.items().magic().rejectCustomEffect(player)) return;
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity != null) {
            gravity.removeModifier(GRAVITY_ID);
            gravity.addTransientModifier(new AttributeModifier(GRAVITY_ID, .6, AttributeModifier.Operation.ADD_VALUE));
        }
        player.entityTags().add(SLAM_TAG);
        mana.score(player, JUMP_TIMER).set(2);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BREEZE_LAND, SoundSource.MASTER, 1, 1.5f);
    }

    private boolean hasFreeFallColumn(ServerPlayer player) {
        var level = player.level();
        BlockPos feet = player.blockPosition();
        for (int offset = 0; offset <= 8; offset++) {
            BlockPos actual = feet.below(offset);
            if (actual.getY() < level.getMinY() || actual.getY() >= level.getMaxY()
                    || !level.getBlockState(actual).is(SOURCE_AIRS)) return false;
        }
        return true;
    }

    private void impact(ServerPlayer player, int accumulatedTicks) {
        // Legacy computes damage from the original squared counter, then applies its unusual radius clamp.
        int squared = accumulatedTicks * accumulatedTicks;
        int damage = (int) Math.floor(squared * .04);
        int radiusScore = accumulatedTicks <= 60 ? 60 : accumulatedTicks >= 200 ? 140 : accumulatedTicks;
        int radius = (int) (radiusScore * .05);
        int particleCount = (int) (radiusScore * .1) * 100;
        ServerLevel level = (ServerLevel) player.level();
        double radiusSquared = radius * (double) radius;
        AABB area = new AABB(player.position(), player.position()).inflate(radius);
        var damageSource = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.OUTSIDE_BORDER), player);

        for (Entity entity : level.getEntities((Entity) null, area, target -> isDamageTarget(player, target))) {
            double distance = player.distanceToSqr(entity);
            if (distance < .000001 || distance > radiusSquared) continue;
            if (entity instanceof LivingEntity living)
                living.hurtServer(level, damageSource, damage);
        }

        level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH,1), player.getX(), player.getY(), player.getZ(),
                particleCount, radius, 0, radius, .05);
        LbcOverload.sound(player, "minecraft:lbcsounds.overload_impact", 1, 1, 0);
        mana.add(player, radius * 100);
        drainNearbyPlayers(player, radius, radius * 100, false, true);

        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity != null) gravity.removeModifier(GRAVITY_ID);
        mana.score(player, SLAM_TIMER).set(0);
        player.entityTags().remove(SLAM_TAG);
    }

    private boolean isDamageTarget(ServerPlayer owner, Entity target) {
        if (!(target instanceof LivingEntity living) || !living.isAlive() || living.isRemoved()
                || living.level() != owner.level() || living.entityTags().contains("spectator")
                || owner.isAlliedTo(living)) return false;
        if (living instanceof ServerPlayer player && (game.isObserver(player)
                || game.awaitingRespawn(player.getUUID())
                || !game.allowDamage(player) && !game.canUseGameplayItems(player))) return false;
        // The legacy #minecraft:mobs tag explicitly also included players.
        return living instanceof ServerPlayer || living.getType().builtInRegistryHolder().is(SOURCE_MOBS);
    }

    private void drainNearbyPlayers(ServerPlayer owner, int radius, int amount, boolean includeOwner, boolean enemiesOnly) {
        if (radius <= 0 || amount <= 0) return;
        double radiusSquared = radius * (double) radius;
        for (ServerPlayer target : game.server().getPlayerList().getPlayers()) {
            if (target.level() != owner.level() || !target.isAlive() || target.isRemoved()
                    || game.isObserver(target) || game.awaitingRespawn(target.getUUID()) || target.entityTags().contains("spectator")
                    || !includeOwner && target == owner || enemiesOnly && owner.isAlliedTo(target)) continue;
            double distance = owner.distanceToSqr(target);
            if ((includeOwner || distance >= .000001) && distance <= radiusSquared) mana.add(target, -amount);
        }
    }
}
