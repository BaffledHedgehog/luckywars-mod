package me.skywars2.mixin.tmt;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import me.skywars2.game.items.tmt.LbcTmtShotCompletion;
import me.skywars2.game.items.weapons.ranged.LbcImbaCrossbows;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shoot returns before these native callers finish their weapon criterion and use statistic. */
public final class TmtShotCompletionMixin {
    private TmtShotCompletionMixin(){}
    @Mixin(BowItem.class)
    public abstract static class Bow {
        @WrapMethod(method="releaseUsing")
        private boolean skywars2$completeBow(ItemStack weapon,Level level,LivingEntity owner,int remaining,Operation<Boolean> original){
            int reserved=LbcImbaCrossbows.reserveShot(owner,weapon);
            if(reserved<0)return false;
            boolean fired=LbcTmtShotCompletion.call(weapon,()->original.call(weapon,level,owner,remaining));
            if(!fired)LbcImbaCrossbows.releaseUnusedShot(owner,reserved);
            return fired;
        }
    }
    @Mixin(CrossbowItem.class)
    public abstract static class Crossbow {
        @Inject(method="use",at=@At("HEAD"),cancellable=true)
        private void skywars2$admitChargedCrossbow(Level level,Player owner,InteractionHand hand,
                CallbackInfoReturnable<InteractionResult> result){
            var weapon=owner.getItemInHand(hand);
            if(CrossbowItem.isCharged(weapon)&&!LbcImbaCrossbows.canShoot(owner,weapon))
                result.setReturnValue(InteractionResult.FAIL);
        }
        @WrapMethod(method="performShooting")
        private void skywars2$completeCrossbow(Level level,LivingEntity owner,InteractionHand hand,ItemStack weapon,
                float power,float uncertainty,LivingEntity target,Operation<Void> original){
            if(!CrossbowItem.isCharged(weapon)||LbcImbaCrossbows.reserveShot(owner,weapon)>=0)
                LbcTmtShotCompletion.run(weapon,()->original.call(level,owner,hand,weapon,power,uncertainty,target));
        }
    }
}
