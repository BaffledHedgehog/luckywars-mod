package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import me.skywars2.game.LbcSpellSource;
import net.minecraft.advancements.triggers.PlayerHurtEntityTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The source advancement boundary includes lethal and resisted hits, once, with native blocking. */
@Mixin(PlayerHurtEntityTrigger.class)
public abstract class OutgoingSkillCriterionMixin {
    @Inject(method="trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;FFZ)V",at=@At("HEAD"))
    private void skywars2$skillHit(ServerPlayer attacker,Entity target,DamageSource source,
            float original,float actual,boolean blocked,CallbackInfo ci){
        var game=SkyWars2.game();
        if(game==null||blocked||!(target instanceof LivingEntity victim))return;
        try(var scope=game.items().spellSources().scope((LbcSpellSource.Source)null)){
            game.items().reactiveTrinkets().outgoing(attacker,victim,source,blocked);
            game.items().championSkill().damaged(victim,source);
            game.items().manaVampireSkill().damaged(victim,source);
            // Unlike Fabric AFTER_DAMAGE, the native criterion also observes lethal contacts.
            if(actual>0)game.items().terra().damaged(attacker,source);
            game.items().cinema().outgoing(attacker,victim,source,original,actual,blocked);
        }
    }
}
