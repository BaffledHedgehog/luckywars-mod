package me.skywars2.game;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Current and old mystical3 inventory passives, called after the native jump impulse. */
public final class LbcJumpArtifacts {
    private final GameController game;
    public LbcJumpArtifacts(GameController game){this.game=game;}
    private boolean admitted(ServerPlayer owner){
        return owner!=null&&owner.isAlive()&&!owner.isRemoved()&&owner.level()==game.maps().level()
                &&game.canUseGameplayItems(owner)&&!game.isObserver(owner)&&game.stage()!=GameController.Stage.ENDING
                &&game.items().wands().validRoleTarget(owner,false)&&!game.items().actionsBlocked(owner)
                &&!game.items().wands().inputTimeStopped(owner)&&!game.items().timeStop().stopped(owner.level());
    }
    private boolean magic(ServerPlayer owner){
        return !owner.entityTags().contains("nomagic_active")&&!game.items().magic().suppressed(owner)
                &&!game.items().magic().suppressedAt(owner.level(),owner.getEyePosition());
    }
    /** Also usable by source producers which explicitly invoke the armor jump helper. Never changes velocity. */
    public void jump(ServerPlayer owner){
        if(!admitted(owner))return;
        var dimension=owner.level().dimension().identifier();
        if(dimension.equals(Identifier.parse("minecraft:nexus"))||dimension.equals(Identifier.parse("minecraft:imprinted")))return;
        int current=0,old=0;
        // Native26 inventory also exposes body/saddle slots41/42; source inventory scope ends at offhand40.
        for(int slot=0;slot<=40;slot++){
            var stack=owner.getInventory().getItem(slot);if(stack.isEmpty())continue;
            var data=LbcFartus.data(stack);int type=data.getIntOr("type",0);if(type<1||type>3)continue;
            int bit=1<<type;
            if(data.getIntOr("mystical",0)==3)current|=bit;
            if(data.getIntOr("mystical_old",0)==3)old|=bit;
        }
        if(current!=0&&magic(owner)){
            if((current&2)!=0)highSpeed(owner);
            if((current&4)!=0)bigJump(owner);
            if((current&8)!=0)comboJump(owner);
        }
        // Old artifacts intentionally bypass the current family's magic gate, in their original order.
        if((old&2)!=0)owner.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,60,3,false,false,false),owner);
        if((old&4)!=0)owner.addEffect(ItemEffectLevels.create(MobEffects.LEVITATION,20,4,false,false,false),owner);
        if((old&8)!=0)owner.addEffect(ItemEffectLevels.create(MobEffects.SPEED,60,4,false,false,false),owner);
    }
    private void highSpeed(ServerPlayer owner){
        var level=owner.level();var at=owner.position();
        owner.addEffect(ItemEffectLevels.create(MobEffects.SPEED,60,5),owner);
        for(var target:level.getEntitiesOfClass(LivingEntity.class,owner.getBoundingBox().inflate(7),entity->{
            double distance=entity.position().distanceToSqr(at);
            return distance>=.0001&&distance<=49&&game.items().wands().target(owner,entity);
        }))target.addEffect(ItemEffectLevels.create(MobEffects.SLOWNESS,60,2),owner);
        level.sendParticles(ParticleTypes.END_ROD,at.x,at.y+1,at.z,30,.5,1,.5,.2);
        LbcOverload.sound(owner,"minecraft:entity.illusioner.cast_spell",1,1,0);
    }
    private void bigJump(ServerPlayer owner){
        var level=owner.level();var at=owner.position();
        for(var target:level.getEntitiesOfClass(LivingEntity.class,owner.getBoundingBox().inflate(7),entity->
                entity.position().distanceToSqr(at)<=49&&(entity==owner?game.items().wands().validRoleTarget(entity,false)
                        // Source has no team filter here: retain friendly mobs, with authoritative allied-player/Greed protection.
                        :game.items().wands().projectileTarget(owner,entity))))
            target.addEffect(ItemEffectLevels.create(MobEffects.LEVITATION,20,10,false,false,false),owner);
        owner.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,200,0,false,false,false),owner);
        level.sendParticles(ParticleTypes.CLOUD,at.x,at.y,at.z,50,.3,.1,.3,.1);
        LbcOverload.sound(owner,"minecraft:block.wool.step",1,.5f,0);
    }
    private static boolean jumpBoost(ServerPlayer owner,int amplifier){
        var effect=owner.getEffect(MobEffects.JUMP_BOOST);return effect!=null&&effect.getAmplifier()==amplifier;
    }
    private void comboJump(ServerPlayer owner){
        if(jumpBoost(owner,32))owner.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,600,0),owner);
        if(jumpBoost(owner,32))owner.addEffect(ItemEffectLevels.create(MobEffects.JUMP_BOOST,160,64),owner);
        if(jumpBoost(owner,16))owner.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,400,0),owner);
        if(jumpBoost(owner,16))owner.addEffect(ItemEffectLevels.create(MobEffects.JUMP_BOOST,200,32),owner);
        if(jumpBoost(owner,8))owner.addEffect(ItemEffectLevels.create(MobEffects.JUMP_BOOST,200,16),owner);
        if(jumpBoost(owner,4))owner.addEffect(ItemEffectLevels.create(MobEffects.JUMP_BOOST,200,8),owner);
        if(jumpBoost(owner,2))owner.addEffect(ItemEffectLevels.create(MobEffects.JUMP_BOOST,200,4),owner);
        if(jumpBoost(owner,1))owner.addEffect(ItemEffectLevels.create(MobEffects.JUMP_BOOST,200,2),owner);
        if(owner.hasEffect(MobEffects.JUMP_BOOST))owner.addEffect(ItemEffectLevels.create(MobEffects.JUMP_BOOST,200,1),owner);
        owner.addEffect(ItemEffectLevels.create(MobEffects.JUMP_BOOST,200,0),owner);
        var at=owner.position();
        owner.connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse("minecraft:entity.wither.shoot"))),
                SoundSource.MASTER,at.x,at.y,at.z,.7f,1,owner.getRandom().nextLong()));
        owner.level().sendParticles(ParticleTypes.POOF,at.x,at.y,at.z,6,.4,.4,.2,.1);
    }
}
