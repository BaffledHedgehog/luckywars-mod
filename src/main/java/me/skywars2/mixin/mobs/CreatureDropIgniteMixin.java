package me.skywars2.mixin.mobs;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.skywars2.SkyWars2;
import me.skywars2.game.items.drops.LbcAntDrops;
import me.skywars2.game.items.drops.LbcChaosTrapActors;
import me.skywars2.game.items.drops.LbcCreatureDrops;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.Ignite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import me.skywars2.game.GameController;

/** Remember the attacker only when the native enchantment starts or extends the fire. */
@Mixin(Ignite.class)
public abstract class CreatureDropIgniteMixin {
    @WrapOperation(method="apply",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V"))
    private void skywars2$creatureFire(Entity target,float seconds,Operation<Void> original,
                                       @Local(argsOnly=true) EnchantedItemInUse item) {
        int previous=target.getRemainingFireTicks();
        original.call(target,seconds);
        var game=SkyWars2.game();
        if(game==null || target.getRemainingFireTicks()<=previous || !(target instanceof LivingEntity living)) return;
        var attacker=item.owner();
        if(attacker==null || !(LbcCreatureDrops.managed(attacker) || LbcAntDrops.managed(attacker)
                || LbcChaosTrapActors.managed(attacker))) return;
        if(game.items().creatureDrops().canAffect(attacker,target) && game.items().antDrops().canAffect(attacker,target)
                && game.items().chaosTrapActors().canAffect(attacker,target))
            game.items().indirectKills().creatureIgnited(living,attacker,game.items().antDrops().wild(attacker));
    }
}
