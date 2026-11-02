package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import me.skywars2.game.LbcArmorReserveSkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Change only the mitigated HP debit: the enclosing native hit retains all physical reactions. */
public interface ArmorReserveMixin {
    @Mixin(LivingEntity.class)
    abstract class Damage {
        @Inject(method="getDamageAfterMagicAbsorb",at=@At("RETURN"),cancellable=true)
        private void skywars2$armorReserve(DamageSource source,float original,CallbackInfoReturnable<Float> result){
            var game=SkyWars2.game();
            if(game!=null&&(Object)this instanceof ServerPlayer player)
                result.setReturnValue(LbcArmorReserveSkill.apply(game,player,source,result.getReturnValueF()));
        }
    }
    @Mixin(LivingEntity.class)
    interface Equipment {
        @Invoker("detectEquipmentUpdates") void skywars2$refreshArmorReserveEquipment();
    }
}
