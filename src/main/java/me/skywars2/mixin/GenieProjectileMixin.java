package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.skywars2.game.GenieNative;
import me.skywars2.game.LbcTmtShotCompletion;
import me.skywars2.game.genie.GenieRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Preserve the real shooter and native contact behavior; only tagged genie shots alter native power. */
@Mixin(Snowball.class)
public abstract class GenieProjectileMixin {
    private static final String THROW="Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectileFromRotation("
            +"Lnet/minecraft/world/entity/projectile/Projectile$ProjectileFactory;Lnet/minecraft/server/level/ServerLevel;"
            +"Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;FFF)Lnet/minecraft/world/entity/projectile/Projectile;";

    @Mixin({SnowballItem.class,EggItem.class,EnderpearlItem.class,WindChargeItem.class,
            ExperienceBottleItem.class,ThrowablePotionItem.class})
    public abstract static class ThrowingItems {
        @WrapMethod(method="use")
        private InteractionResult skywars2$completeThrow(Level level,Player player,InteractionHand hand,
                                                         Operation<InteractionResult> original){
            return LbcTmtShotCompletion.call(player.getItemInHand(hand),()->original.call(level,player,hand));
        }
        @WrapOperation(method="use",at=@At(value="INVOKE",target=THROW))
        private Projectile skywars2$acceptedThrow(Projectile.ProjectileFactory<Projectile> factory,ServerLevel level,
                ItemStack source,LivingEntity owner,float offset,float speed,float spread,Operation<Projectile> original,
                @Local(argsOnly=true) InteractionHand hand){
            var shot=original.call(factory,level,source,owner,offset,speed,spread);
            GenieNative.acceptedThrow(owner,hand,source,source,shot);
            return shot;
        }
    }

    @Mixin(TridentItem.class)
    public abstract static class Trident {
        @WrapMethod(method="releaseUsing")
        private boolean skywars2$completeTrident(ItemStack stack,Level level,LivingEntity owner,int remaining,
                                                Operation<Boolean> original){
            return LbcTmtShotCompletion.call(stack,()->original.call(stack,level,owner,remaining));
        }
        @WrapOperation(method="releaseUsing",at=@At(value="INVOKE",target=THROW))
        private Projectile skywars2$acceptedTrident(Projectile.ProjectileFactory<Projectile> factory,ServerLevel level,
                ItemStack thrownStack,LivingEntity owner,float offset,float speed,float spread,Operation<Projectile> original,
                @Local(argsOnly=true) ItemStack physical){
            var hand=owner.getUsedItemHand();
            var shot=original.call(factory,level,thrownStack,owner,offset,speed,spread);
            GenieNative.acceptedThrow(owner,hand,physical,thrownStack,shot);
            return shot;
        }
    }

    @ModifyArg(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"),index=1)
    private float skywars2$geniePower(float ordinary){return GenieRuntime.projectilePower((Projectile)(Object)this,ordinary);}
    @Mixin(SmallFireball.class)
    public abstract static class Fireball {
        @ModifyArg(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"),index=2)
        private float skywars2$genieFireballPower(float ordinary){return GenieRuntime.projectilePower((Projectile)(Object)this,ordinary);}
    }
}
