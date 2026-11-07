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
public abstract class BobaGravityMixin implements BobaRegionPhysics.ClientGravityData {
    @Shadow @org.spongepowered.asm.mixin.Final private static net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_NO_GRAVITY;
    @Override public net.minecraft.network.syncher.SynchedEntityData.DataValue<Boolean> boba$gravityData(boolean value){
        return net.minecraft.network.syncher.SynchedEntityData.DataValue.create(DATA_NO_GRAVITY,value);
    }
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
    @WrapOperation(method="isInWall",at=@At(value="INVOKE",target="Lnet/minecraft/world/phys/AABB;ofSize(Lnet/minecraft/world/phys/Vec3;DDD)Lnet/minecraft/world/phys/AABB;"))
    private net.minecraft.world.phys.AABB boba$eyeCollision(Vec3 eye,double x,double y,double z,Operation<net.minecraft.world.phys.AABB> original){
        var self=(Entity)(Object)this;int turns=BobaRegionPhysics.turns(self);
        if(turns==0)return original.call(eye,x,y,z);
        // Keep vanilla suffocation rules, but test the thin eye slice in the body's actual frame.
        return boba$box(original.call(new Vec3(0,self.getEyeHeight(),0),x,y,z),turns).move(self.position());
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

    /** Vanilla clients simulate these entities too: project gravity and correct their unrotated collisions. */
    @Mixin(net.minecraft.server.level.ServerEntity.class)
    public abstract static class Tracking {
        @Shadow @org.spongepowered.asm.mixin.Final private Entity entity;
        @Shadow @org.spongepowered.asm.mixin.Final private net.minecraft.server.level.ServerEntity.Synchronizer synchronizer;
        @Unique private boolean boba$projectedGravity;
        @Unique private boolean boba$pairedWithGravity;
        @org.spongepowered.asm.mixin.injection.ModifyArg(method={"sendPairingData","sendDirtyEntityData"},
                at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/game/ClientboundSetEntityDataPacket;<init>(ILjava/util/List;)V"),index=1)
        private java.util.List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> boba$metadata(
                java.util.List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> values){
            return BobaRegionPhysics.clientMetadata(entity,values);
        }
        @org.spongepowered.asm.mixin.injection.Inject(method="sendPairingData",at=@At("TAIL"))
        private void boba$pairGravity(net.minecraft.server.level.ServerPlayer player,
                java.util.function.Consumer<net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener>> broadcast,
                org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){
            // A default-data entity has no native metadata packet at all; late viewers still need the override.
            if(BobaRegionPhysics.clientNoGravity(entity)){
                broadcast.accept(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(
                        entity.getId(),java.util.List.of(((BobaRegionPhysics.ClientGravityData)entity).boba$gravityData(true))));
                boba$pairedWithGravity=true;
            }
        }
        @org.spongepowered.asm.mixin.injection.Inject(method="sendChanges",at=@At("HEAD"))
        private void boba$trackGravity(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){
            boolean projected=BobaRegionPhysics.clientNoGravity(entity);
            if(projected!=boba$projectedGravity||boba$pairedWithGravity){
                synchronizer.sendToTrackingPlayers(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(entity.getId(),
                        java.util.List.of(((BobaRegionPhysics.ClientGravityData)entity).boba$gravityData(projected||entity.isNoGravity()))));
                boba$projectedGravity=projected;
                boba$pairedWithGravity=false;
                entity.needsSync=true;entity.syncPosition=true;
            }
            if(projected){entity.needsSync=true;entity.syncPosition=true;}
        }
        @WrapOperation(method="createMovePacket",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getRequiresPrecisePosition()Z"))
        private boolean boba$precisePosition(Entity self,Operation<Boolean> original){
            return original.call(self)||BobaRegionPhysics.clientNoGravity(self);
        }
        @org.spongepowered.asm.mixin.injection.Inject(method="sendChanges",at=@At("TAIL"))
        private void boba$trackMovement(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){
            // NoGravity suppresses acceleration, not client drag or world-axis collision prediction.
            if(BobaRegionPhysics.clientNoGravity(entity))synchronizer.sendToTrackingPlayers(
                    new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(entity));
        }
    }
    @Mixin(net.minecraft.world.level.block.FallingBlock.class)
    public abstract static class FallingBlock {
        @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/core/BlockPos;below()Lnet/minecraft/core/BlockPos;"))
        private net.minecraft.core.BlockPos boba$fallSupport(net.minecraft.core.BlockPos pos,Operation<net.minecraft.core.BlockPos> original,
                @com.llamalad7.mixinextras.sugar.Local(argsOnly=true) net.minecraft.server.level.ServerLevel level){
            int turns=BobaRegionPhysics.turns(level,pos);
            return turns==0?original.call(pos):pos.relative(BobaRegionPhysics.rotate(net.minecraft.core.Direction.DOWN,turns));
        }
    }
    @Mixin(net.minecraft.world.entity.item.FallingBlockEntity.class)
    public abstract static class FallingEntity {
        @WrapOperation(method="fall",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
        private static boolean boba$fallOrigin(net.minecraft.world.level.Level level,Entity entity,Operation<Boolean> original,
                @com.llamalad7.mixinextras.sugar.Local(argsOnly=true) net.minecraft.core.BlockPos pos){
            int turns=BobaRegionPhysics.turns(level,pos);
            if(turns!=0){
                // Rotate local feet around the source cell center before the native spawn packet is built.
                entity.setPos(Vec3.atCenterOf(pos).subtract(BobaRegionPhysics.rotate(new Vec3(0,.5,0),turns)));
                entity.xo=entity.getX();entity.yo=entity.getY();entity.zo=entity.getZ();
            }
            return original.call(level,entity);
        }
        @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/item/FallingBlockEntity;blockPosition()Lnet/minecraft/core/BlockPos;"))
        private net.minecraft.core.BlockPos boba$landingCell(net.minecraft.world.entity.item.FallingBlockEntity self,Operation<net.minecraft.core.BlockPos> original){
            return BobaRegionPhysics.turns(self)==0?original.call(self):net.minecraft.core.BlockPos.containing(self.getBoundingBox().getCenter());
        }
        @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/core/BlockPos;below()Lnet/minecraft/core/BlockPos;"))
        private net.minecraft.core.BlockPos boba$landingSupport(net.minecraft.core.BlockPos pos,Operation<net.minecraft.core.BlockPos> original){
            int turns=BobaRegionPhysics.turns((Entity)(Object)this);
            return turns==0?original.call(pos):pos.relative(BobaRegionPhysics.rotate(net.minecraft.core.Direction.DOWN,turns));
        }
        @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="tick",at=@At(value="FIELD",target="Lnet/minecraft/core/Direction;DOWN:Lnet/minecraft/core/Direction;"))
        private net.minecraft.core.Direction boba$placeDown(net.minecraft.core.Direction down){return BobaRegionPhysics.rotate(down,BobaRegionPhysics.turns((Entity)(Object)this));}
        @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="tick",at=@At(value="FIELD",target="Lnet/minecraft/core/Direction;UP:Lnet/minecraft/core/Direction;"))
        private net.minecraft.core.Direction boba$placeUp(net.minecraft.core.Direction up){return BobaRegionPhysics.rotate(up,BobaRegionPhysics.turns((Entity)(Object)this));}
        @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"))
        private Vec3 boba$landingBounce(Vec3 movement,double x,double y,double z,Operation<Vec3> original){
            int turns=BobaRegionPhysics.turns((Entity)(Object)this);
            return turns==0?original.call(movement,x,y,z):BobaRegionPhysics.rotate(original.call(BobaRegionPhysics.rotate(movement,-turns),x,y,z),turns);
        }
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
