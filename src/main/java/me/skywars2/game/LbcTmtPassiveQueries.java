package me.skywars2.game;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Exact equipment queries and the shared source jump leaf; existing native boundaries own admission. */
final class LbcTmtPassiveQueries {
    private LbcTmtPassiveQueries(){}
    static boolean rocketFeet(ItemStack stack){
        for(var property:LbcTmtAbilities.read(stack))if(LbcTmtComponents.nativeItem(property.data())==LbcItems.ROCKET_BOOTS
                ||((ItemData.IntReader)(Object)property.data()).skywars2$readInt("explosive_feets",0)==1)return true;
        return false;
    }
    /** Caller supplies the actual FEET slot. Legacy Balance requires both iron boots and its own flag. */
    static boolean reactiveOldFeet(ItemStack feet){
        if(!LbcTmtItems.matches(feet))return feet.is(Items.IRON_BOOTS)&&ItemData.intValue(feet,"reactive_old",0)==1;
        for(var property:LbcTmtAbilities.read(feet))if(LbcTmtComponents.nativeItem(property.data())==Items.IRON_BOOTS
                &&((ItemData.IntReader)(Object)property.data()).skywars2$readInt("reactive_old",0)==1)return true;
        return false;
    }
    /** Called by the admitted native jump/armor boundary, before its separately gated artifact effects. */
    static void reactiveOldJump(ServerPlayer owner){
        var feet=owner.getItemBySlot(EquipmentSlot.FEET);boolean reactive=false;
        if(!LbcTmtItems.matches(feet))reactive=ItemData.intValue(feet,"reactive_old",0)==1;
        else for(var property:LbcTmtAbilities.read(feet))if(((ItemData.IntReader)(Object)property.data()).skywars2$readInt("reactive_old",0)==1){reactive=true;break;}
        if(!reactive)return;
        owner.addEffect(ItemEffectLevels.create(MobEffects.LEVITATION,20,0,false,false,false),owner);
        owner.level().sendParticles(ParticleTypes.FLAME,true,false,owner.getX(),owner.getY()-.2,owner.getZ(),100,.2,.1,.2,.1);
        LbcOverload.sound(owner,owner.position(),"minecraft:entity.firework_rocket.blast",10,0,20);
    }
}
