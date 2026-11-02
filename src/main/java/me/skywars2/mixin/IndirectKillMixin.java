package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Перед loot/XP устанавливает автора только подходящей отложенной причины смерти. */
@Mixin({LivingEntity.class, ServerPlayer.class})
public abstract class IndirectKillMixin {
    @Inject(method="die",at=@At("HEAD"))
    private void skywars2$indirectKill(DamageSource source, CallbackInfo ci) {
        var game=SkyWars2.game();
        if(game!=null){
            game.items().indirectKills().dying((LivingEntity)(Object)this,source);
            game.items().combatRewards().beforeDeath((LivingEntity)(Object)this,source);
        }
    }
}
