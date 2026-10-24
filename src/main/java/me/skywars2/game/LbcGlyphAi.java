package me.skywars2.game;

import me.skywars2.mixin.GlyphAiMixin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;
import java.util.Optional;

/** A reversible exclusive Goal lease. Ordinary running goals and Brain are paused, never removed. */
final class LbcGlyphAi extends Goal {
    final Mob mob;
    LbcCorruptedGlyphs.Glyph glyph;
    private final LivingEntity oldTarget;
    private final boolean oldAggressive;
    private final Path oldPath;
    private final double oldSpeed;
    private final Optional<LivingEntity> oldBrainTarget;
    private final long oldBrainExpiry;
    private AttributeInstance addedAttack;
    private LivingEntity opponent;
    private int replan,attackCooldown;
    LbcGlyphAi(Mob mob,LbcCorruptedGlyphs.Glyph glyph){
        this.mob=mob;this.glyph=glyph;oldTarget=mob.getTargetUnchecked();oldAggressive=mob.isAggressive();
        oldPath=mob.getNavigation().getPath();oldSpeed=((GlyphAiMixin.NavigationAccess)mob.getNavigation()).skywars2$glyphSpeed();
        oldBrainTarget=mob.getBrain().getMemoryInternal(MemoryModuleType.ATTACK_TARGET);
        oldBrainExpiry=oldBrainTarget==null?Long.MAX_VALUE:mob.getBrain().getTimeUntilExpiry(MemoryModuleType.ATTACK_TARGET);
        setFlags(EnumSet.allOf(Goal.Flag.class));mob.getNavigation().stop();
    }
    @Override public boolean canUse(){return opponent!=null&&opponent.isAlive();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    void target(LivingEntity target){
        opponent=target;mob.setTarget(target);mob.setAggressive(true);
        if(oldBrainTarget!=null)mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET,target);
    }
    void combat(LivingEntity target){
        target(target);
        if(!mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)){
            var map=mob.getAttributes();
            addedAttack=new AttributeInstance(Attributes.ATTACK_DAMAGE,attribute->{map.getAttributesToUpdate().add(attribute);if(attribute.getAttribute().value().isClientSyncable())map.getAttributesToSync().add(attribute);});
            ((GlyphAiMixin.AttributeAccess)map).skywars2$glyphAttributes().put(Attributes.ATTACK_DAMAGE,addedAttack);
        }
        tick();
    }
    @Override public void tick(){
        mob.getLookControl().setLookAt(opponent,30,30);
        if(--replan<=0){mob.getNavigation().moveTo(opponent,0,1.0);replan=10;}
        if(attackCooldown>0)attackCooldown--;
        if(attackCooldown==0&&mob.canAttack(opponent)&&mob.isWithinMeleeAttackRange(opponent)&&mob.getSensing().hasLineOfSight(opponent)){
            attackCooldown=20;mob.swingForAttack(InteractionHand.MAIN_HAND);mob.doHurtTarget((ServerLevel)mob.level(),opponent);
        }
    }
    void field(Vec3 center,boolean unholy){
        mob.setTarget(null);mob.setAggressive(false);
        if(oldBrainTarget!=null)mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        if(unholy&&mob.position().distanceToSqr(center)<2.25){mob.getNavigation().stop();return;}
        if(--replan>0)return;
        replan=10;
        if(unholy){mob.getNavigation().moveTo(center.x,center.y,center.z,0,1.0);return;}
        var away=mob.position().subtract(center).multiply(1,0,1).normalize();
        if(away.lengthSqr()<.001)away=new Vec3(1,0,0);
        Path best=null;double farthest=mob.position().distanceToSqr(center);
        // Native path search selects a reachable outward endpoint, including detours around obstacles.
        for(int i=0;i<7;i++){
            double angle=(i==0?0:(i%2==0?-1:1)*((i+1)/2)*Math.PI/6);
            var destination=mob.position().add(away.yRot((float)angle).scale(10));
            var path=mob.getNavigation().createPath(destination.x,destination.y,destination.z,0);
            if(path!=null&&path.getEndNode()!=null){var node=path.getEndNode();double distance=new Vec3(node.x+.5,node.y,node.z+.5).distanceToSqr(center);
                if(distance>farthest){farthest=distance;best=path;}}
        }
        if(best==null)mob.getNavigation().stop();else mob.getNavigation().moveTo(best,1.0);
    }
    void restore(){
        mob.getNavigation().stop();mob.setTarget(oldTarget!=null&&oldTarget.isAlive()?oldTarget:null);mob.setAggressive(oldAggressive);
        if(oldBrainTarget!=null){
            if(oldBrainTarget.isEmpty())mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            else if(oldBrainExpiry==Long.MAX_VALUE)mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET,oldBrainTarget.get());
            else mob.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET,oldBrainTarget.get(),oldBrainExpiry);
        }
        if(oldPath!=null&&!oldPath.isDone()&&mob.isAlive()&&!mob.isRemoved())mob.getNavigation().moveTo(oldPath,oldSpeed);
        else mob.getNavigation().setSpeedModifier(oldSpeed);
        if(addedAttack!=null){var map=mob.getAttributes();((GlyphAiMixin.AttributeAccess)map).skywars2$glyphAttributes().remove(Attributes.ATTACK_DAMAGE,addedAttack);map.getAttributesToSync().remove(addedAttack);map.getAttributesToUpdate().remove(addedAttack);}
    }
}
