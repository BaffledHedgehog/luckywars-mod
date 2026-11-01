package me.skywars2.game;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

/** Exact legacy scaled attribute-base arithmetic, with native attribute value sanitization. */
public final class LbcAttributeChanges {
    private LbcAttributeChanges() {}
    public static void change(LivingEntity entity,Holder<Attribute> key,int inputScale,int step,double outputScale){
        var attribute=entity.getAttribute(key);if(attribute==null)return;
        // Java conversion truncates/saturates; the scoreboard addition has signed int overflow.
        int scaled=(int)(attribute.getBaseValue()*inputScale);scaled+=step;
        attribute.setBaseValue(scaled*outputScale);
    }
}
