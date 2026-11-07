package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.game.LbcTmtShotCompletion;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/** Shoot returns before these native callers finish their weapon criterion and use statistic. */
public final class TmtShotCompletionMixin {
    private TmtShotCompletionMixin(){}
    @Mixin(BowItem.class)
    public abstract static class Bow {
        @WrapMethod(method="releaseUsing")
        private boolean skywars2$completeBow(ItemStack weapon,Level level,LivingEntity owner,int remaining,Operation<Boolean> original){
            return LbcTmtShotCompletion.call(weapon,()->original.call(weapon,level,owner,remaining));
        }
    }
    @Mixin(CrossbowItem.class)
    public abstract static class Crossbow {
        @WrapMethod(method="performShooting")
        private void skywars2$completeCrossbow(Level level,LivingEntity owner,InteractionHand hand,ItemStack weapon,
                float power,float uncertainty,LivingEntity target,Operation<Void> original){
            LbcTmtShotCompletion.run(weapon,()->original.call(level,owner,hand,weapon,power,uncertainty,target));
        }
    }
}
