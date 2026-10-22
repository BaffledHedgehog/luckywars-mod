package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import me.skywars2.game.LbcCreatureDrops;
import me.skywars2.game.LbcAntDrops;
import me.skywars2.game.LbcIndirectKills;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.Ignite;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fire Aspect keeps the summoned attacker's author after vanilla's hurt cache expires. */
@Mixin(Ignite.class)
public abstract class CreatureDropIgniteMixin {
    @Inject(method="apply",at=@At("TAIL"))
    private void skywars2$creatureFire(ServerLevel level,int enchantmentLevel,EnchantedItemInUse item,
                                      Entity target,Vec3 position,CallbackInfo ci) {
        var game=SkyWars2.game();
        if(game!=null && (LbcCreatureDrops.managed(item.owner()) || LbcAntDrops.managed(item.owner()))
                && target instanceof LivingEntity living && game.items().creatureDrops().canAffect(item.owner(),target)
                && game.items().antDrops().canAffect(item.owner(),target)) {
            var author=LbcIndirectKills.playerSource(item.owner());
            if(author!=null)game.items().indirectKills().ignited(living,author,game.items().antDrops().wild(item.owner()));
        }
    }
}
