package me.skywars2.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** The 26 equally likely outcomes of skills/parasitoit_stat_down.mcfunction. */
public final class LbcParasitePenalties {
    private LbcParasitePenalties() {}

    public static void roll(ServerPlayer player) {
        apply(player, player.level().getRandom().nextInt(26) + 1);
    }

    /** Source indices are one-based; the caller owns eligibility and the trigger chance. */
    public static void apply(ServerPlayer player, int index) {
        switch (index) {
            case 1 -> LbcAttributeChanges.change(player, Attributes.ARMOR, 1, -1, 1);
            case 2 -> LbcAttributeChanges.change(player, Attributes.ARMOR_TOUGHNESS, 1, -1, 1);
            case 3 -> LbcAttributeChanges.change(player, Attributes.ATTACK_DAMAGE, 1, -1, 1);
            case 4 -> LbcAttributeChanges.change(player, Attributes.ATTACK_KNOCKBACK, 10, -1, .1);
            case 5 -> LbcAttributeChanges.change(player, Attributes.ATTACK_SPEED, 5, -1, .2);
            case 6 -> LbcAttributeChanges.change(player, Attributes.BURNING_TIME, 10, 1, .1);
            case 7 -> LbcAttributeChanges.change(player, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 10, -1, .1);
            case 8 -> LbcAttributeChanges.change(player, Attributes.FALL_DAMAGE_MULTIPLIER, 10, 1, .1);
            case 9 -> LbcAttributeChanges.change(player, Attributes.JUMP_STRENGTH, 10, -1, .1);
            // The source removes ten scaled units here, unlike the other resistance penalty.
            case 10 -> LbcAttributeChanges.change(player, Attributes.KNOCKBACK_RESISTANCE, 10, -10, .1);
            case 11 -> LbcAttributeChanges.change(player, Attributes.LUCK, 1, -1, 1);
            case 12 -> LbcAttributeChanges.change(player, Attributes.MAX_HEALTH, 1, -1, 1);
            case 13 -> LbcAttributeChanges.change(player, Attributes.MOVEMENT_EFFICIENCY, 10, -1, .1);
            case 14 -> LbcAttributeChanges.change(player, Attributes.MOVEMENT_SPEED, 100000, -2000, .00001);
            case 15 -> LbcAttributeChanges.change(player, Attributes.OXYGEN_BONUS, 10, -1, .1);
            case 16 -> LbcAttributeChanges.change(player, Attributes.SAFE_FALL_DISTANCE, 5, -1, .2);
            case 17 -> LbcAttributeChanges.change(player, Attributes.SCALE, 10, -1, .1);
            case 18 -> LbcAttributeChanges.change(player, Attributes.STEP_HEIGHT, 10, -1, .1);
            case 19 -> LbcAttributeChanges.change(player, Attributes.WATER_MOVEMENT_EFFICIENCY, 10, -1, .1);
            case 20 -> LbcAttributeChanges.change(player, Attributes.BLOCK_BREAK_SPEED, 10, -1, .1);
            case 21 -> LbcAttributeChanges.change(player, Attributes.BLOCK_INTERACTION_RANGE, 5, -1, .2);
            case 22 -> LbcAttributeChanges.change(player, Attributes.ENTITY_INTERACTION_RANGE, 5, -1, .2);
            // Preserve the source decimal rather than replacing it with an exact third.
            case 23 -> LbcAttributeChanges.change(player, Attributes.MINING_EFFICIENCY, 3, -1, .333333333);
            case 24 -> LbcAttributeChanges.change(player, Attributes.SNEAKING_SPEED, 10, -1, .1);
            case 25 -> LbcAttributeChanges.change(player, Attributes.SUBMERGED_MINING_SPEED, 20, -1, .05);
            case 26 -> LbcAttributeChanges.change(player, Attributes.SWEEPING_DAMAGE_RATIO, 20, -1, .05);
            default -> throw new IllegalArgumentException("Parasite penalty index must be 1..26: " + index);
        }
    }
}
