package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.SkyWars2;
import me.skywars2.game.LbcPhantomArmor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe actual native actions after their outcome, including Fabric's early-return item callbacks. */
@Mixin(ServerPlayerGameMode.class)
public abstract class PhantomActivityMixin {
    @WrapMethod(method="useItem")
    private InteractionResult skywars2$used(ServerPlayer player,Level level,ItemStack stack,InteractionHand hand,
                                          Operation<InteractionResult> original){
        var result=original.call(player,level,stack,hand);LbcPhantomArmor.used(player,result);return result;
    }
    @WrapMethod(method="useItemOn")
    private InteractionResult skywars2$usedOn(ServerPlayer player,Level level,ItemStack stack,InteractionHand hand,
                                            BlockHitResult hit,Operation<InteractionResult> original){
        var result=original.call(player,level,stack,hand,hit);LbcPhantomArmor.used(player,result);return result;
    }
    @Mixin(Player.class)
    public abstract static class Combat {
        @Inject(method="attack",at=@At("RETURN"))
        private void skywars2$attacked(Entity target,CallbackInfo ci){
            // Clearing after the attack preserves Strength for the opening hit itself.
            if((Object)this instanceof ServerPlayer player){var game=SkyWars2.game();if(game!=null)game.items().armorSets().phantom().activity(player);}
        }
        @Inject(method="stabAttack",at=@At("RETURN"))
        private void skywars2$stabbed(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir){
            if(cir.getReturnValueZ()&&(Object)this instanceof ServerPlayer player){var game=SkyWars2.game();if(game!=null)game.items().armorSets().phantom().activity(player);}
        }
        @WrapMethod(method="interactOn")
        private InteractionResult skywars2$interacted(Entity target,InteractionHand hand,Vec3 point,
                                                     Operation<InteractionResult> original){
            var result=original.call(target,hand,point);if((Object)this instanceof ServerPlayer player)LbcPhantomArmor.used(player,result);return result;
        }
    }
    @Mixin(ServerGamePacketListenerImpl.class)
    public abstract static class Punch {
        @org.spongepowered.asm.mixin.Shadow public ServerPlayer player;
        @Inject(method="handlePunch",at=@At("RETURN"))
        private void skywars2$punched(net.minecraft.network.protocol.game.ServerboundPunchPacket packet,CallbackInfo ci){
            var game=SkyWars2.game();if(game!=null)game.items().armorSets().phantom().activity(player);
        }
    }
    @Mixin(Projectile.class)
    public abstract static class Shots implements LbcPhantomArmor.ShotState {
        @Override @Accessor("hasBeenShot") public abstract boolean skywars2$hasBeenShot();
        @Inject(method="tick",at=@At("HEAD"))
        private void skywars2$lateOwner(CallbackInfo ci){
            var game=SkyWars2.game();if(game!=null)game.items().armorSets().phantom().projectile((Projectile)(Object)this);
        }
    }
}
