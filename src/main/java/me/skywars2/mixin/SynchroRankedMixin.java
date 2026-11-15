package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.game.SynchroLootContext;
import me.skywars2.game.SynchroRanked;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.function.Consumer;

public final class SynchroRankedMixin {
    private SynchroRankedMixin(){}
    @Mixin(LootTable.class)
    public abstract static class Table {
        @ModifyExpressionValue(method={"getRandomItems","getRandomItemsRaw","fill"},at=@At(value="INVOKE",target="Lnet/minecraft/world/level/storage/loot/LootContext$Builder;create(Ljava/util/Optional;)Lnet/minecraft/world/level/storage/loot/LootContext;"))
        private LootContext skywars2$sequence(LootContext context){SynchroRanked.prepare((LootTable)(Object)this,context);return context;}
        @WrapMethod(method="getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V")
        private void skywars2$directContext(LootContext context,Consumer<ItemStack> output,Operation<Void> original){
            var state=(SynchroLootContext)context;state.skywars2$synchroEnter();
            try{SynchroRanked.prepare((LootTable)(Object)this,context);original.call(context,output);}finally{state.skywars2$synchroExit();}
        }
    }
    @Mixin(LootContext.class)
    public abstract static class Context implements SynchroLootContext {
        @Unique private boolean skywars2$synchroPrepared;
        @Unique private RandomSource skywars2$synchroRandom;
        @Unique private Integer skywars2$synchroSlab;
        @Unique private int skywars2$synchroDepth;
        @Unique private boolean skywars2$synchroConsumed;
        @Override public boolean skywars2$synchroPrepared(){return skywars2$synchroPrepared;}
        @Override public void skywars2$synchroPrepare(RandomSource random){skywars2$synchroPrepared=true;skywars2$synchroRandom=random;}
        @Override public boolean skywars2$synchroActive(){return skywars2$synchroRandom!=null;}
        @Override public Integer skywars2$synchroSlab(){return skywars2$synchroSlab;}
        @Override public void skywars2$synchroSlab(int value){skywars2$synchroSlab=value;}
        @Override public void skywars2$synchroEnter(){if(skywars2$synchroDepth++==0){if(skywars2$synchroConsumed){skywars2$synchroPrepared=false;skywars2$synchroRandom=null;skywars2$synchroSlab=null;}skywars2$synchroConsumed=true;}}
        @Override public void skywars2$synchroExit(){skywars2$synchroDepth--;}
        @Inject(method="getRandom",at=@At("HEAD"),cancellable=true)
        private void skywars2$random(CallbackInfoReturnable<RandomSource> ci){if(skywars2$synchroRandom!=null)ci.setReturnValue(skywars2$synchroRandom);}
    }
}
