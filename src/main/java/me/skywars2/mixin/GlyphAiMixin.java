package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;

@Mixin(Mob.class)
public abstract class GlyphAiMixin {
    @Inject(method="serverAiStep",at=@At("HEAD"),cancellable=true)
    private void skywars2$glyphGoal(CallbackInfo ci){
        var game=SkyWars2.game();
        if(game!=null&&game.items().corruptedGlyphs().aiStep((Mob)(Object)this))ci.cancel();
    }
    @Mixin(PathNavigation.class)
    public interface NavigationAccess {
        @Accessor("speedModifier") double skywars2$glyphSpeed();
    }
    @Mixin(AttributeMap.class)
    public interface AttributeAccess {
        @Accessor("attributes") Map<Holder<Attribute>,AttributeInstance> skywars2$glyphAttributes();
    }
}
