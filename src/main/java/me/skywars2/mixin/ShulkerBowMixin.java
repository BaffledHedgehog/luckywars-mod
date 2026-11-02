package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.SkyWars2;
import me.skywars2.game.LbcShulkerBow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Confined to typed bow carriers: unrelated shulker armor/drop projectiles retain vanilla behavior. */
@Mixin(ShulkerBullet.class)
public abstract class ShulkerBowMixin implements LbcShulkerBow.Carrier {
    @Unique private LbcShulkerBow.State skywars2$bowState;
    public LbcShulkerBow.State skywars2$shulkerState(){return skywars2$bowState;}
    public void skywars2$shulkerState(LbcShulkerBow.State state){skywars2$bowState=state;}
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void skywars2$bowTick(CallbackInfo ci){var game=SkyWars2.game();var self=(ShulkerBullet)(Object)this;
        if(game!=null&&self.level() instanceof ServerLevel&&(!game.items().trinketArrows().beforeProjectile(self)||!game.items().shulkerBow().beforeBullet(self)))ci.cancel();}
    @Inject(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/projectile/ProjectileUtil;getHitResultOnMoveVector(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;)Lnet/minecraft/world/phys/HitResult;"),cancellable=true)
    private void skywars2$residentAfterNativeSteering(CallbackInfo ci){var game=SkyWars2.game();var self=(ShulkerBullet)(Object)this;
        if(game!=null&&self.level() instanceof ServerLevel&&!game.items().shulkerBow().beforeNativeCollision(self))ci.cancel();}
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/projectile/ProjectileUtil;getHitResultOnMoveVector(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;)Lnet/minecraft/world/phys/HitResult;"))
    private HitResult skywars2$bedrockAfterNativeSteering(Entity self,java.util.function.Predicate<Entity> predicate,Operation<HitResult> original){
        var game=SkyWars2.game();if(game!=null&&self.level() instanceof ServerLevel)game.items().trinketArrows().bedrockBeforeNativeMove((ShulkerBullet)self);
        return original.call(self,predicate);
    }
    @Inject(method="canHitEntity",at=@At("RETURN"),cancellable=true)
    private void skywars2$bowTarget(Entity target,CallbackInfoReturnable<Boolean> ci){var game=SkyWars2.game();
        if(ci.getReturnValueZ()&&game!=null&&!game.items().shulkerBow().canHit((ShulkerBullet)(Object)this,target))ci.setReturnValue(false);}
    @Inject(method="onHitEntity",at=@At("HEAD"),cancellable=true)
    private void skywars2$bowDamage(EntityHitResult hit,CallbackInfo ci){var game=SkyWars2.game();var self=(ShulkerBullet)(Object)this;
        if(game!=null&&self.level() instanceof ServerLevel&&LbcShulkerBow.managed(self)){game.items().shulkerBow().entityHit(self,hit);ci.cancel();}}
    @Inject(method="onHitBlock",at=@At("TAIL"))
    private void skywars2$bowBlock(BlockHitResult hit,CallbackInfo ci){var game=SkyWars2.game();var self=(ShulkerBullet)(Object)this;
        if(game!=null&&self.level() instanceof ServerLevel)game.items().shulkerBow().blockHit(self,hit);}
    @WrapOperation(method="onHit",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/projectile/ShulkerBullet;destroy()V"))
    private void skywars2$bowPierce(ShulkerBullet self,Operation<Void> original,HitResult hit){var game=SkyWars2.game();
        if(game==null||!game.items().shulkerBow().keepAfterContact(self,hit))original.call(self);}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void skywars2$saveBow(ValueOutput output,CallbackInfo ci){LbcShulkerBow.save(skywars2$bowState,output);}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void skywars2$loadBow(ValueInput input,CallbackInfo ci){skywars2$bowState=LbcShulkerBow.load(input);}
    /** Exposes the accepted weapon to native enchantment and damage attribution consumers. */
    public ItemStack getWeaponItem(){return skywars2$bowState==null||skywars2$bowState.weapon.isEmpty()?null:skywars2$bowState.weapon;}
    @Mixin(ShulkerBullet.class)
    public interface BulletAccess {@Invoker("selectNextMoveDirection") void skywars2$selectDirection(net.minecraft.core.Direction.Axis axis,Entity target);}
    @Mixin(AbstractArrow.class)
    public interface ArrowAccess {@Accessor("baseDamage") double skywars2$baseDamage();}
    @Mixin(SpectralArrow.class)
    public interface SpectralAccess {@Accessor("duration") int skywars2$duration();}
}



