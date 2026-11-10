package me.skywars2.mixin;

import me.skywars2.game.LbcTntTimer;
import me.skywars2.game.LbcSpellSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** One native name tag, no display entities, NBT scans or extra timer scheduler. */
@Mixin(PrimedTnt.class)
public abstract class TntTimerMixin implements LbcSpellSource.TntOwnerAccess {
    @Shadow private net.minecraft.world.entity.EntityReference<net.minecraft.world.entity.LivingEntity> owner;
    @Override public void skywars2$clearLootOwner(){owner=null;}
    @Override public net.minecraft.world.entity.LivingEntity skywars2$nativeLootOwner(){return net.minecraft.world.entity.EntityReference.getLivingEntity(owner,((PrimedTnt)(Object)this).level());}
    @Unique private int skywars2$timerBucket=Integer.MIN_VALUE+1;
    @Unique private Component skywars2$timerText;
    @Unique private boolean skywars2$countingFuse;
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="tick")
    private void skywars2$nativeCountdown(com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original){
        boolean previous=skywars2$countingFuse;skywars2$countingFuse=true;
        try{original.call();}finally{skywars2$countingFuse=previous;}
    }
    @Inject(method="<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V",at=@At("TAIL"))
    private void skywars2$initialTimer(CallbackInfo ci){skywars2$timer();}
    @Inject(method="setFuse",at=@At("TAIL"))
    private void skywars2$fuseTimer(int fuse,CallbackInfo ci){
        var self=(PrimedTnt)(Object)this;
        // An authored fuse-zero payload differs from an ordinary countdown reaching zero.
        if(!skywars2$countingFuse&&fuse<=0&&self.level() instanceof ServerLevel)self.entityTags().add(me.skywars2.game.LbcDamage.UNPROTECTED_TNT);
        skywars2$timer();
    }
    @Unique private void skywars2$timer(){
        var self=(PrimedTnt)(Object)this;if(!(self.level() instanceof ServerLevel))return;
        int bucket=LbcTntTimer.bucket(self.getFuse());
        if(bucket==skywars2$timerBucket&&self.getCustomName()==skywars2$timerText&&self.isCustomNameVisible())return;
        skywars2$timerBucket=bucket;skywars2$timerText=LbcTntTimer.label(self.getFuse());
        self.setCustomName(skywars2$timerText);self.setCustomNameVisible(true);
    }
}
