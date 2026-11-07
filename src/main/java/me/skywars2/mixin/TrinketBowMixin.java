package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

/** Один proc на успешный выстрел лука/арбалета; Multishot сохраняет один общий бросок шанса. */
@Mixin(ProjectileWeaponItem.class)
public abstract class TrinketBowMixin {
    @Inject(method="shoot",at=@At("HEAD"))
    private void skywars2$captureWeapon(ServerLevel level,LivingEntity shooter,InteractionHand hand,ItemStack weapon,List<ItemStack> ammunition,
            float power,float uncertainty,boolean critical,LivingEntity target,CallbackInfo ci,@Share("firedWeapon") LocalRef<ItemStack> firedWeapon){
        firedWeapon.set(weapon.copy());
    }
    @WrapOperation(method = "shoot", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectile(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)Lnet/minecraft/world/entity/projectile/Projectile;"))
    private Projectile skywars2$spawned(Projectile projectile, ServerLevel level, ItemStack ammo,
                                       Consumer<Projectile> configure, Operation<Projectile> original,
                                       @Share("spawned") LocalRef<List<Projectile>> spawned) {
        Consumer<Projectile> modified=body->{
            configure.accept(body);
            var game=SkyWars2.game();
            if(game!=null&&body.getOwner() instanceof ServerPlayer owner){
                if((Object)this instanceof BowItem)game.items().projectileLevelups().beforePublish(owner,body,me.skywars2.game.LbcProjectileLevelups.Launch.BOW);
                else if((Object)this instanceof CrossbowItem)game.items().projectileLevelups().beforePublish(owner,body,me.skywars2.game.LbcProjectileLevelups.Launch.CROSSBOW);
                game.items().timeStop().prepareFrozen(body);
            }
        };
        var result = original.call(projectile, level, ammo, modified);
        if(result!=null&&level.getEntity(result.getUUID())==result){
            if(spawned.get()==null)spawned.set(new java.util.ArrayList<>());spawned.get().add(result);
        }
        return result;
    }

    @Inject(method = "shoot", at = @At("RETURN"))
    private void skywars2$shot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon,
                               List<ItemStack> ammunition, float power, float uncertainty, boolean critical,
                               LivingEntity target, CallbackInfo ci, @Share("spawned") LocalRef<List<Projectile>> spawned,@Share("firedWeapon") LocalRef<ItemStack> firedWeapon) {
        var game = SkyWars2.game();
        if (spawned.get()!=null&&!spawned.get().isEmpty() && game != null && ((Object) this instanceof BowItem || (Object) this instanceof CrossbowItem)
                && shooter instanceof ServerPlayer player){
            var fired=firedWeapon.get();boolean traitsRolled=false;
            if(me.skywars2.game.LbcTmtRanged.onShot(game,player,hand,weapon,fired,List.copyOf(spawned.get()))){
                return;
            }
            me.skywars2.game.LbcLegacyRanged.installRocketAmmo(game,player,fired,List.copyOf(spawned.get()));
            if(me.skywars2.game.ItemData.intValue(fired,"explosive",0)==1){
                var nativeFamily=List.copyOf(spawned.get());if(!traitsRolled)game.items().trinkets().shot(player,nativeFamily);traitsRolled=true;
                if(game.items().balance().explosiveBow(player,fired,nativeFamily))return;
            }
            if(me.skywars2.game.LbcChargedBows.matches(fired)){
                var nativeFamily=List.copyOf(spawned.get());if(!traitsRolled)game.items().trinkets().shot(player,nativeFamily);traitsRolled=true;
                game.items().chargedBows().release(player,fired,player.getTicksUsingItem(),nativeFamily);return;
            }
            var imbaFamily=me.skywars2.game.LbcImbaCrossbows.augment(game,player,fired,List.copyOf(spawned.get()));
            if(imbaFamily.isEmpty())return;
            if(me.skywars2.game.LbcImbaCrossbows.isBeam(fired)){
                game.items().trinkets().shot(player,imbaFamily);
                me.skywars2.game.LbcImbaCrossbows.release(game,player,fired,imbaFamily);return;
            }
            var oldFamily=game.items().balance().augment(player,fired,imbaFamily);
            var family=game.items().shulkerBow().augment(player,fired,oldFamily);
            game.items().madokaBow().augment(player,fired,family);
            if(!traitsRolled)game.items().trinkets().shot(player,family);
            me.skywars2.game.LbcLegacyRanged.convert(game,player,fired,family);
            me.skywars2.game.LbcMinigunCrossbow.afterShot(game,player,hand,weapon,family,fired);
            me.skywars2.game.LbcLegacyRanged.afterShot(game,player,hand,weapon,family,fired);
        }
    }
}
