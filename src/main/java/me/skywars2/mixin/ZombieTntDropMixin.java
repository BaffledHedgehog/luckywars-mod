package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native trap carriers keep ordinary AI, with the same role and allegiance policy as their TNT. */
@Mixin(Mob.class)
public abstract class ZombieTntDropMixin {
    @Inject(method="canAttack", at=@At("HEAD"), cancellable=true)
    private void skywars2$trapTarget(LivingEntity target, CallbackInfoReturnable<Boolean> ci) {
        var game=SkyWars2.game();
        if(me.skywars2.game.LbcSpellSource.playersOnlyTrap((Entity)(Object)this)
                &&!(target instanceof net.minecraft.server.level.ServerPlayer)){ci.setReturnValue(false);return;}
        if(game!=null && (!game.items().totems().canAffect((Entity)(Object)this,target)
                || !game.items().zombieTntDrops().canTarget((Entity)(Object)this,target)
                || !game.items().luckyPigs().canTarget((Entity)(Object)this,target)
                || !game.items().beeDrops().canTarget((Entity)(Object)this,target)
                || !game.items().slabDrops().canTarget((Entity)(Object)this,target)
                || !game.items().creatureDrops().canTarget((Entity)(Object)this,target)
                || !game.items().droneDrops().canTarget((Entity)(Object)this,target)
                || !game.items().antDrops().canTarget((Entity)(Object)this,target)
                || !game.items().chaosTrapActors().canTarget((Entity)(Object)this,target)
                || game.items().spellSources().wild((Entity)(Object)this)&&!game.items().sauvojenBoss().canAffect((Entity)(Object)this,target))) ci.setReturnValue(false);
    }

    @Inject(method="setTarget", at=@At("HEAD"), cancellable=true)
    private void skywars2$trapSetTarget(LivingEntity target, CallbackInfo ci) {
        var game=SkyWars2.game();
        if(target!=null&&me.skywars2.game.LbcSpellSource.playersOnlyTrap((Entity)(Object)this)
                &&!(target instanceof net.minecraft.server.level.ServerPlayer)){ci.cancel();return;}
        if(game!=null && (!game.items().totems().canAffect((Entity)(Object)this,target)
                || !game.items().zombieTntDrops().canTarget((Entity)(Object)this,target)
                || !game.items().luckyPigs().canTarget((Entity)(Object)this,target)
                || !game.items().beeDrops().canTarget((Entity)(Object)this,target)
                || !game.items().slabDrops().canTarget((Entity)(Object)this,target)
                || !game.items().creatureDrops().canTarget((Entity)(Object)this,target)
                || !game.items().droneDrops().canTarget((Entity)(Object)this,target)
                || !game.items().antDrops().canTarget((Entity)(Object)this,target)
                || !game.items().chaosTrapActors().canTarget((Entity)(Object)this,target)
                || target!=null&&game.items().spellSources().wild((Entity)(Object)this)&&!game.items().sauvojenBoss().canAffect((Entity)(Object)this,target))) ci.cancel();
    }

    @Inject(method="doHurtTarget", at=@At("HEAD"), cancellable=true)
    private void skywars2$trapMelee(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> ci) {
        var game=SkyWars2.game();
        if(game!=null && (!game.items().totems().canAffect((Entity)(Object)this,target)
                || !game.items().summons().canMelee((Entity)(Object)this,target)
                || !game.items().zombieTntDrops().canMelee((Entity)(Object)this,target)
                || !game.items().luckyPigs().canAffect((Entity)(Object)this,target)
                || !game.items().slabDrops().canAffect((Entity)(Object)this,target)
                || !game.items().creatureDrops().canAffect((Entity)(Object)this,target)
                || !game.items().droneDrops().canAffect((Entity)(Object)this,target)
                || !game.items().antDrops().canAffect((Entity)(Object)this,target)
                || !game.items().chaosTrapActors().canAffect((Entity)(Object)this,target)
                || game.items().spellSources().wild((Entity)(Object)this)&&!game.items().sauvojenBoss().canAffect((Entity)(Object)this,target))) ci.setReturnValue(false);
    }

    // Stamp native drowning/lightning conversions before ENTITY_LOAD without replacing conversion.
    @WrapOperation(method="convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;",
            at=@At(value="INVOKE", target="Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean skywars2$trapConversion(ServerLevel level, Entity converted, Operation<Boolean> original) {
        var game=SkyWars2.game();
        if(game!=null) {
            // Freezing must not turn a named reward mob into a vanilla-loot Stray.
            if(!game.balanceLegacy() && ((Entity)(Object)this).entityTags().contains("fat_skeleton")
                    && (Object)this instanceof net.minecraft.world.entity.Mob parent && converted instanceof net.minecraft.world.entity.Mob child)
                ((MobLootAccess)child).skywars2$lootTable(parent.getLootTable());
            game.items().summons().inheritConversion((Entity)(Object)this,converted);
            game.items().syringe().converted((Entity)(Object)this,converted);
            game.items().communism().converted((Entity)(Object)this,converted);
            game.items().zombieTntDrops().inheritConversion((Entity)(Object)this,converted);
            game.items().luckyPigs().inheritConversion((Entity)(Object)this,converted);
            game.items().slabDrops().inheritConversion((Entity)(Object)this,converted);
            game.items().droneDrops().inheritConversion((Entity)(Object)this,converted);
            game.items().antDrops().inheritConversion((Entity)(Object)this,converted);
            game.items().chaosTrapActors().inheritConversion((Entity)(Object)this,converted);
            game.items().spellSources().inherit((Entity)(Object)this,converted);
            game.items().nukeEvent().inherit((Entity)(Object)this,converted);
            game.items().oreshek().inherit((Entity)(Object)this,converted);
            game.items().foodLifecycle().inheritConversion((Entity)(Object)this,converted);
        }
        return original.call(level,converted);
    }

}
