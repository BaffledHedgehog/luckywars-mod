package me.skywars2.game.combat;

import me.skywars2.game.combat.LbcIndirectKills;

/** Typed server-only provenance for one native effect instance. */
public interface WandPowerEffectAccess {
    net.minecraft.world.effect.MobEffectInstance skywars2$hiddenEffect();
    LbcIndirectKills.Owner skywars2$powerCause();
    void skywars2$powerCause(LbcIndirectKills.Owner cause);
}
