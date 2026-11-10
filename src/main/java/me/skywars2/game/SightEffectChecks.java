package me.skywars2.game;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

/** Shared native potion admission and actual katana hit, with a non-item high-level control. */
public final class SightEffectChecks {
    private SightEffectChecks() {}
    private static void require(boolean value,String reason){if(!value)throw new IllegalStateException("Sight effects: "+reason);}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();f.move(f.other,f.home.add(0,0,2));
            f.owner.removeAllEffects();f.other.removeAllEffects();
            var potion=new ItemStack(Items.POTION);
            potion.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.of(0x505050),
                    List.of(new MobEffectInstance(MobEffects.BLINDNESS,100,127),new MobEffectInstance(MobEffects.DARKNESS,100,127)),Optional.empty()));
            potion.getItem().finishUsingItem(potion,f.level,f.owner);
            require(f.owner.getEffect(MobEffects.BLINDNESS)!=null&&f.owner.getEffect(MobEffects.BLINDNESS).getAmplifier()==1
                    &&f.owner.getEffect(MobEffects.DARKNESS)!=null&&f.owner.getEffect(MobEffects.DARKNESS).getAmplifier()==1,
                    "native consumption caps BOTH high generated potion effects at II");
            f.owner.removeAllEffects();
            var existing=new MobEffectInstance(MobEffects.BLINDNESS,100,7);
            f.owner.addEffect(existing);
            require(f.owner.getEffect(MobEffects.BLINDNESS).getAmplifier()==7,"explicit non-item status retains its true level");
            f.other.addEffect(new MobEffectInstance(existing),f.owner);
            require(f.other.getEffect(MobEffects.BLINDNESS).getAmplifier()==7,"native transfer of an existing status is not reclassified as an item grant");
            f.owner.removeAllEffects();f.other.removeAllEffects();
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcElectro.CYBERKATANA));
            f.other.setInvulnerableTime(0);f.other.damageCooldownTime=0;
            f.owner.attack(f.other);
            require(f.other.getEffect(MobEffects.BLINDNESS)!=null&&f.other.getEffect(MobEffects.BLINDNESS).getAmplifier()==1,
                    "actual native melee katana hit applies Blindness II");
            require(f.other.hasEffect(MobEffects.WITHER)&&f.other.hasEffect(MobEffects.SLOWNESS)&&f.other.hasEffect(MobEffects.GLOWING),
                    "other three katana debuffs survive the sight-only change");
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS sight-effects (native generated potion, existing high-level transfer/non-item control, real Electro melee and other debuffs)");
    }
}
