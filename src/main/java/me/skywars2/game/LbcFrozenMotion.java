package me.skywars2.game;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Independent zero-gravity leases compose without overwriting the entity's original gravity. */
public final class LbcFrozenMotion {
    public static final String LOCK="skywars2.motion_locked";
    public static final Identifier STASIS=Identifier.parse("skywars2:personal_stasis_motion"),
            FROST=Identifier.parse("skywars2:frost_motion"),TIME=Identifier.parse("skywars2:time_stop_motion");
    private LbcFrozenMotion(){}
    public static void hold(LivingEntity entity,Identifier owner){
        var gravity=entity.getAttribute(Attributes.GRAVITY);
        if(gravity!=null&&!gravity.hasModifier(owner))gravity.addTransientModifier(new AttributeModifier(owner,-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        boolean moving=entity.getDeltaMovement().lengthSqr()!=0;
        entity.entityTags().add(LOCK);entity.setDeltaMovement(Vec3.ZERO);entity.fallDistance=0;
        if(moving&&entity.level() instanceof net.minecraft.server.level.ServerLevel level)
            level.getChunkSource().sendToTrackingPlayersAndSelf(entity,new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(entity));
    }
    public static void release(LivingEntity entity,Identifier owner){
        var gravity=entity.getAttribute(Attributes.GRAVITY);
        if(gravity!=null)gravity.removeModifier(owner);
        if(gravity==null||List.of(STASIS,FROST,TIME).stream().noneMatch(gravity::hasModifier))entity.entityTags().remove(LOCK);
    }
}
