package me.skywars2.media.client.mixin;

import me.skywars2.media.client.ClientFovPreferences;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AbstractClientPlayer.class)
public abstract class PersonalFovMixin {
    @ModifyVariable(method="getFieldOfViewModifier",at=@At("HEAD"),argsOnly=true)
    private float skywars2$personalFov(float scale){return ClientFovPreferences.scale(scale);}
}
