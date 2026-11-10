package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native publication context; attribution/admission remains in the shared death entry. */
public interface CombatRewardMixin {
    @Mixin({LivingEntity.class,ServerPlayer.class})
    abstract class Death {
        @WrapMethod(method="die")
        private void skywars2$deathItems(DamageSource source,Operation<Void> original) {
            var victim=(LivingEntity)(Object)this;var game=SkyWars2.game();
            if(game==null||!(victim.level() instanceof ServerLevel level)){original.call(source);return;}
            game.items().combatRewards().withDeathLoot(victim,level,source,()->original.call(source));
        }
    }
    @Mixin(LivingEntity.class)
    abstract class DeathLoot {
        @Inject(method="getKillCredit",at=@At("RETURN"),cancellable=true)
        private void skywars2$retainedNpc(CallbackInfoReturnable<LivingEntity> result){
            if(result.getReturnValue()!=null)return;
            var attacker=me.skywars2.game.LbcIndirectKills.retainedNpcCredit((LivingEntity)(Object)this);
            if(attacker!=null)result.setReturnValue(attacker);
        }
        @WrapMethod(method="dropAllDeathLoot")
        private void skywars2$lootItems(ServerLevel level,DamageSource source,Operation<Void> original) {
            var game=SkyWars2.game();
            if(game==null){original.call(level,source);return;}
            game.items().combatRewards().withDeathLoot((LivingEntity)(Object)this,level,source,()->original.call(level,source));
        }
    }
    @Mixin(ServerLevel.class)
    abstract class Published {
        @Inject(method="addFreshEntity",at=@At("RETURN"))
        private void skywars2$actualDeathDrop(Entity child,CallbackInfoReturnable<Boolean> result) {
            var game=SkyWars2.game();
            if(game!=null&&result.getReturnValueZ())game.items().combatRewards().published((ServerLevel)(Object)this,child);
        }
    }
}
