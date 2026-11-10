package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shared native/raw-HP accepted damage boundary, before health is clamped or death is dispatched. */
@Mixin(CombatTracker.class)
public abstract class SkillDamageReceiptMixin {
    @Shadow @Final private LivingEntity mob;
    @Inject(method="recordDamage",at=@At("HEAD"))
    private void skywars2$vip(DamageSource source,float amount,CallbackInfo ci){
        var game=SkyWars2.game();
        if(game!=null){
            me.skywars2.game.LbcAcceptedHits.record(mob,source,amount);
            game.items().indirectKills().acceptedDamage(mob,source,amount);
            if(mob instanceof ServerPlayer player)game.items().containedSkills().recordDamage(player,source,amount);
        }
    }
}
