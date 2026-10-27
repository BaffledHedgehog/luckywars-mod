package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Warden overrides Mob.canAttack; guard its brain, vibration anger and sonic-boom target at the real source. */
@Mixin(Warden.class)
public abstract class DeathCompassWardenMixin {
    @Inject(method="canTargetEntity",at=@At("HEAD"),cancellable=true)
    private void skywars2$compassTarget(Entity target,CallbackInfoReturnable<Boolean> ci){
        var game=SkyWars2.game();
        if(game!=null&&!game.items().creatureDrops().canTarget((Entity)(Object)this,
                target instanceof net.minecraft.world.entity.LivingEntity living?living:null))ci.setReturnValue(false);
    }
}
