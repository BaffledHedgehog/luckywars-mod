package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/** Finish all native accepted sweep hits before moving the attacker or any accepted target. */
@Mixin(Player.class)
public abstract class PendingSwordAttackMixin {
    @WrapMethod(method="attack")
    private void skywars2$pendingSwordAttack(Entity victim,Operation<Void> original){
        var game=SkyWars2.game();
        if(game==null||!((Object)this instanceof ServerPlayer owner)){original.call(victim);return;}
        var service=game.items().pendingSwords();var reactive=game.items().reactiveArtifacts();service.beginAttack(owner);reactive.beginAttack(owner);
        try{original.call(victim);}finally{try{service.endAttack(owner);}finally{reactive.endAttack(owner);}}
    }
}
