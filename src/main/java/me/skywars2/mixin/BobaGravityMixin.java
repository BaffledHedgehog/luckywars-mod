package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.game.BobaRegionPhysics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Redirect native gravity impulses and resolve grounding on the region's actual support axis. */
@Mixin(Entity.class)
public abstract class BobaGravityMixin {
    @Unique private Vec3 boba$moveStart=Vec3.ZERO;
    @Unique private Vec3 boba$requested=Vec3.ZERO;
    @Shadow private static Vec3 collideWithShapes(Vec3 movement,net.minecraft.world.phys.AABB box,java.util.List<net.minecraft.world.phys.shapes.VoxelShape> shapes){throw new AssertionError();}
    @Shadow private static float[] collectCandidateStepUpHeights(net.minecraft.world.phys.AABB box,java.util.List<net.minecraft.world.phys.shapes.VoxelShape> shapes,float step,float skip){throw new AssertionError();}
    @Unique private static net.minecraft.world.phys.AABB boba$box(net.minecraft.world.phys.AABB box,int turns){
        var a=BobaRegionPhysics.rotate(new Vec3(box.minX,box.minY,box.minZ),turns);
        var b=BobaRegionPhysics.rotate(new Vec3(box.maxX,box.maxY,box.maxZ),turns);
        return new net.minecraft.world.phys.AABB(Math.min(a.x,b.x),Math.min(a.y,b.y),Math.min(a.z,b.z),Math.max(a.x,b.x),Math.max(a.y,b.y),Math.max(a.z,b.z));
    }
    @Unique private static java.util.List<net.minecraft.world.phys.shapes.VoxelShape> boba$colliders(Entity self,net.minecraft.world.phys.AABB worldBox,int turns){
        var result=new java.util.ArrayList<net.minecraft.world.phys.shapes.VoxelShape>();
        for(var shape:Entity.collectAllColliders(self,self.level(),worldBox))for(var box:shape.toAabbs())
            result.add(net.minecraft.world.phys.shapes.Shapes.create(boba$box(box,-turns)));
        return result;
    }
    @WrapMethod(method="collide")
    private Vec3 boba$localCollision(Vec3 worldMovement,Operation<Vec3> original){
        var self=(Entity)(Object)this;int turns=BobaRegionPhysics.turns(self);
        if(turns==0||worldMovement.lengthSqr()==0)return original.call(worldMovement);
        var movement=BobaRegionPhysics.rotate(worldMovement,-turns);
        var box=boba$box(self.getBoundingBox(),-turns);
        var stepUp=BobaRegionPhysics.rotate(new Vec3(0,self.maxUpStep(),0),turns);
        var colliders=boba$colliders(self,self.getBoundingBox().expandTowards(worldMovement).expandTowards(stepUp),turns);
        var resolved=collideWithShapes(movement,box,colliders);
        boolean grounded=movement.y!=resolved.y&&movement.y<0;
        if(self.maxUpStep()>0&&(grounded||self.onGround())&&(movement.x!=resolved.x||movement.z!=resolved.z)){
            var groundBox=grounded?box.move(0,resolved.y,0):box;
            var stepBox=groundBox.expandTowards(movement.x,self.maxUpStep(),movement.z);
            if(!grounded)stepBox=stepBox.expandTowards(0,-1e-5,0);
            colliders=boba$colliders(self,boba$box(stepBox,turns),turns);
            for(float height:collectCandidateStepUpHeights(groundBox,colliders,self.maxUpStep(),(float)resolved.y)){
                var step=collideWithShapes(new Vec3(movement.x,height,movement.z),groundBox,colliders);
                if(step.horizontalDistanceSqr()>resolved.horizontalDistanceSqr())return BobaRegionPhysics.rotate(step.subtract(0,box.minY-groundBox.minY,0),turns);
            }
        }
        return BobaRegionPhysics.rotate(resolved,turns);
    }
    @com.llamalad7.mixinextras.injector.ModifyReturnValue(method="makeBoundingBox(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;",at=@At("RETURN"))
    private net.minecraft.world.phys.AABB boba$bodyBox(net.minecraft.world.phys.AABB box,Vec3 position){
        var self=(Entity)(Object)this;
        int turns=self instanceof net.minecraft.world.entity.player.Player?0:BobaRegionPhysics.turns(self.level(),net.minecraft.core.BlockPos.containing(position));
        return turns==0?box:boba$box(box.move(position.scale(-1)),turns).move(position);
    }
    @WrapMethod(method="applyGravity")
    private void boba$gravity(Operation<Void> original){
        var self=(Entity)(Object)this;int turns=BobaRegionPhysics.turns(self);
        if(turns==0){original.call();return;}
        self.setDeltaMovement(self.getDeltaMovement().add(BobaRegionPhysics.rotate(new Vec3(0,-self.getGravity(),0),turns)));
    }
    @WrapMethod(method="move")
    private void boba$ground(MoverType type,Vec3 requested,Operation<Void> original){
        var self=(Entity)(Object)this;int turns=BobaRegionPhysics.turns(self);
        if(turns==0||self.noPhysics){original.call(type,requested);return;}
        var before=self.position();boba$moveStart=before;boba$requested=requested;original.call(type,requested);
        var wanted=BobaRegionPhysics.rotate(requested,-turns);
        var actual=BobaRegionPhysics.rotate(self.position().subtract(before),-turns);
        boolean vertical=Math.abs(wanted.y-actual.y)>1.0e-7;
        boolean horizontal=Math.abs(wanted.x-actual.x)>1.0e-7||Math.abs(wanted.z-actual.z)>1.0e-7;
        self.verticalCollision=vertical;self.verticalCollisionBelow=vertical&&wanted.y<0;self.horizontalCollision=horizontal;
        self.setOnGroundWithMovement(self.verticalCollisionBelow,horizontal,self.position().subtract(before));
    }
    @WrapOperation(method="move",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;setOnGroundWithMovement(ZZLnet/minecraft/world/phys/Vec3;)V"))
    private void boba$groundBeforeEffects(Entity self,boolean grounded,boolean horizontal,Vec3 movement,Operation<Void> original){
        int turns=BobaRegionPhysics.turns(self);if(turns==0){original.call(self,grounded,horizontal,movement);return;}
        var actual=BobaRegionPhysics.rotate(movement,-turns);var wanted=BobaRegionPhysics.rotate(boba$requested,-turns);
        self.verticalCollision=Math.abs(actual.y-wanted.y)>1.0e-7;
        self.verticalCollisionBelow=self.verticalCollision&&wanted.y<0;
        boolean h=Math.abs(actual.x-wanted.x)>1.0e-7||Math.abs(actual.z-wanted.z)>1.0e-7;
        original.call(self,self.verticalCollisionBelow,h,movement);
    }
    @WrapOperation(method="move",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;checkFallDamage(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V"))
    private void boba$fall(Entity self,double dy,boolean ground,net.minecraft.world.level.block.state.BlockState state,net.minecraft.core.BlockPos pos,Operation<Void> original){
        int turns=BobaRegionPhysics.turns(self);
        original.call(self,turns==0?dy:BobaRegionPhysics.rotate(self.position().subtract(boba$moveStart),-turns).y,ground,state,pos);
    }
    @WrapMethod(method="restituteMovementAfterCollisions")
    private void boba$bounce(net.minecraft.world.level.block.state.BlockState state,boolean xHit,boolean zHit,Vec3 movement,Operation<Void> original){
        var self=(Entity)(Object)this;int turns=BobaRegionPhysics.turns(self);
        if(turns==0){original.call(state,xHit,zHit,movement);return;}
        var local=BobaRegionPhysics.rotate(movement,-turns);var wanted=BobaRegionPhysics.rotate(boba$requested,-turns);
        self.setDeltaMovement(BobaRegionPhysics.rotate(self.getDeltaMovement(),-turns));
        try{original.call(state,Math.abs(local.x-wanted.x)>1e-7,Math.abs(local.z-wanted.z)>1e-7,local);}
        finally{self.setDeltaMovement(BobaRegionPhysics.rotate(self.getDeltaMovement(),turns));}
    }
    @org.spongepowered.asm.mixin.injection.Inject(method="getOnPos(F)Lnet/minecraft/core/BlockPos;",at=@At("HEAD"),cancellable=true)
    private void boba$support(float offset,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.core.BlockPos> ci){
        var self=(Entity)(Object)this;int turns=BobaRegionPhysics.turns(self);if(turns==0)return;
        var box=self.getBoundingBox();
        var support=switch(turns){case 1->new Vec3(self.getX(),(box.minY+box.maxY)*.5,box.minZ-offset);
            case 2->new Vec3(self.getX(),box.maxY+offset,self.getZ());
            case 3->new Vec3(self.getX(),(box.minY+box.maxY)*.5,box.maxZ+offset);default->self.position();};
        ci.setReturnValue(net.minecraft.core.BlockPos.containing(support));
    }

    @Mixin(LivingEntity.class)
    public abstract static class Living {
        @Unique private double boba$airGravity;
        @Shadow protected abstract float getAirDrag();
        @Shadow protected abstract float getJumpPower();
        @WrapOperation(method="travelInAir",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;getEffectiveGravity()D"))
        private double boba$recordGravity(LivingEntity self,Operation<Double> original){double g=original.call(self);boba$airGravity=g;return g;}
        @WrapMethod(method="travelInAir")
        private void boba$air(Vec3 input,Operation<Void> original){
            boba$airGravity=0;original.call(input);
            var self=(LivingEntity)(Object)this;int turns=BobaRegionPhysics.turns(self);
            if(turns==0||boba$airGravity==0)return;
            double g=boba$airGravity*(self.shouldDiscardFriction()?1:getAirDrag());
            self.setDeltaMovement(self.getDeltaMovement().add(0,g,0).add(BobaRegionPhysics.rotate(new Vec3(0,-g,0),turns)));
        }
        @WrapMethod(method="getFluidFallingAdjustedMovement")
        private Vec3 boba$fluidGravity(double gravity,boolean falling,Vec3 movement,Operation<Vec3> original){
            int turns=BobaRegionPhysics.turns((Entity)(Object)this);
            if(turns==0)return original.call(gravity,falling,movement);
            var local=BobaRegionPhysics.rotate(movement,-turns);
            return BobaRegionPhysics.rotate(original.call(gravity,local.y<=0,local),turns);
        }
        @WrapOperation(method="jumpFromGround",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V"))
        private void boba$jump(LivingEntity self,double x,double y,double z,Operation<Void> original){
            int turns=BobaRegionPhysics.turns(self);if(turns==0){original.call(self,x,y,z);return;}
            var local=BobaRegionPhysics.rotate(self.getDeltaMovement(),-turns);
            double jump=getJumpPower();
            var result=BobaRegionPhysics.rotate(new Vec3(local.x,Math.max(jump,local.y),local.z),turns);
            original.call(self,result.x,result.y,result.z);
        }
        @WrapOperation(method={"travelInLava","goDownInWater","jumpInLiquid","floatInLiquidWhileRidden","updateFallFlyingMovement"},
                at=@At(value="INVOKE",target="Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"))
        private Vec3 boba$verticalImpulse(Vec3 current,double x,double y,double z,Operation<Vec3> original){
            int turns=BobaRegionPhysics.turns((Entity)(Object)this);
            if(turns==0||x!=0||z!=0)return original.call(current,x,y,z);
            return current.add(BobaRegionPhysics.rotate(new Vec3(x,y,z),turns));
        }
    }
    @Mixin(net.minecraft.world.entity.vehicle.boat.AbstractBoat.class)
    public abstract static class Boat {
        @WrapOperation(method="floatBoat",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;setDeltaMovement(DDD)V"))
        private void boba$boat(net.minecraft.world.entity.vehicle.boat.AbstractBoat self,double x,double y,double z,Operation<Void> original){
            int turns=BobaRegionPhysics.turns(self);if(turns==0){original.call(self,x,y,z);return;}
            var before=self.getDeltaMovement();var impulse=BobaRegionPhysics.rotate(new Vec3(0,y-before.y,0),turns);
            var result=new Vec3(x,before.y,z).add(impulse);original.call(self,result.x,result.y,result.z);
        }
    }
    @Mixin(net.minecraft.world.entity.animal.squid.Squid.class)
    public abstract static class Squid extends LivingEntity {
        protected Squid(net.minecraft.world.entity.EntityType<? extends LivingEntity> type,net.minecraft.world.level.Level level){super(type,level);}
        @WrapOperation(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/animal/squid/Squid;setDeltaMovement(DDD)V"))
        private void boba$squid(net.minecraft.world.entity.animal.squid.Squid self,double x,double y,double z,Operation<Void> original){
            int turns=BobaRegionPhysics.turns(self);
            if(turns==0||self.isInWater()||self.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)){original.call(self,x,y,z);return;}
            var local=BobaRegionPhysics.rotate(self.getDeltaMovement(),-turns);
            var result=BobaRegionPhysics.rotate(new Vec3(0,(local.y-self.getGravity())*getAirDrag(),0),turns);
            original.call(self,result.x,result.y,result.z);
        }
    }
}
