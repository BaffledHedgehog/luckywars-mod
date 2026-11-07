package me.skywars2.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Weakness scales the complete attack attribute, including signed levels. */
@Mixin(AttributeInstance.class)
public abstract class WeaknessAttributeMixin {
    private static final Identifier WEAKNESS=Identifier.withDefaultNamespace("effect.weakness");
    @Inject(method="calculateValue",at=@At("HEAD"),cancellable=true)
    private void skywars2$weaknessPenalty(CallbackInfoReturnable<Double> cir){
        var attribute=(AttributeInstance)(Object)this;
        if(!attribute.getAttribute().equals(Attributes.ATTACK_DAMAGE))return;
        var weakness=attribute.getModifier(WEAKNESS);
        if(weakness==null||weakness.operation()!=AttributeModifier.Operation.ADD_VALUE)return;
        var modifiers=attribute.getModifiers();double base=attribute.getBaseValue();
        for(var modifier:modifiers)if(modifier.operation()==AttributeModifier.Operation.ADD_VALUE&&!modifier.id().equals(WEAKNESS))base+=modifier.amount();
        double result=base;
        for(var modifier:modifiers)if(modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_BASE)result+=base*modifier.amount();
        for(var modifier:modifiers)if(modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)result*=1+modifier.amount();
        // Native Weakness stores -4 * signed level. Multiply after the other modifiers,
        // rather than making a large weapon bonus immune to a small base penalty.
        double factor=Math.pow(.75,weakness.amount()/-4.0);
        if(result!=0)result*=factor; // 0 * infinity must remain zero at extreme negative levels.
        cir.setReturnValue(attribute.getAttribute().value().sanitizeValue(result));
    }
}
