package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.SkyWars2;
import me.skywars2.game.LbcSteampunkBreaker;
import me.skywars2.game.LbcImbaAxe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Action inputs do not depend on durability, Unbreaking, repair or unrelated equipment. */
@Mixin(Player.class)
public abstract class SteampunkActionMixin {
    @Inject(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;onAttack()V"))
    private void skywars2$breakerContact(Entity target,CallbackInfo ci){
        var game=SkyWars2.game();
        if(game!=null&&(Object)this instanceof ServerPlayer owner){
            game.items().steampunkBreaker().used(owner,owner.getMainHandItem());
            game.items().imbaAxe().activate(owner,owner.getMainHandItem(),LbcImbaAxe.Trigger.REAL_MELEE);
        }
    }
    @Mixin(ServerPlayerGameMode.class)
    public abstract static class Breaking {
        @Shadow @Final protected ServerPlayer player;
        @WrapMethod(method="destroyBlock")
        private boolean skywars2$breakerBlock(BlockPos pos,Operation<Boolean> original){
            var game=SkyWars2.game();var held=player.getMainHandItem();
            if(game==null||!LbcSteampunkBreaker.matches(held)&&!LbcImbaAxe.recognized(held)||player.level().getBlockState(pos).isAir())return original.call(pos);
            // The last legitimate use can consume the tool; retain the accepted source stack.
            var tool=held.copy();boolean broken=original.call(pos);
            if(broken){game.items().steampunkBreaker().used(player,tool);
                game.items().imbaAxe().activate(player,tool,LbcImbaAxe.Trigger.BLOCK_BREAK);}
            return broken;
        }
    }
}
