package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import me.skywars2.game.LbcBedwarsArena;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native transaction preflights; every hook queries the same match original-type owner. */
public final class BedwarsProtectionMixin {
    private BedwarsProtectionMixin(){}
    @Mixin(Level.class)
    public abstract static class Destruction {
        @Inject(method="destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",at=@At("HEAD"),cancellable=true)
        private void skywars2$beforeLoot(BlockPos pos,boolean drops,Entity breaker,int limit,CallbackInfoReturnable<Boolean> cir){
            var arena=LbcBedwarsArena.at((Object)this);
            if(arena!=null&&!arena.allowsType((Object)this,pos,((Level)(Object)this).getFluidState(pos).createLegacyBlock()))cir.setReturnValue(false);
        }
    }
    @Mixin(LevelChunk.class)
    public abstract static class Chunk {
        @Inject(method="setBlockState",at=@At("HEAD"),cancellable=true)
        private void skywars2$directChunk(BlockPos pos,BlockState state,int flags,CallbackInfoReturnable<BlockState> cir){
            var level=((LevelChunk)(Object)this).getLevel();var arena=LbcBedwarsArena.at(level);
            if(arena!=null&&!arena.allowsType(level,pos,state))cir.setReturnValue(null);
        }
    }
    @Mixin(ServerPlayerGameMode.class)
    public abstract static class Breaking {
        @Shadow @Final protected ServerPlayer player;
        @Inject(method="destroyBlock",at=@At("HEAD"),cancellable=true)
        private void skywars2$beforePlayerCallbacks(BlockPos pos,CallbackInfoReturnable<Boolean> cir){
            var arena=LbcBedwarsArena.at(player.level());if(arena==null)return;
            var result=arena.playerBreak(player,pos);if(result==null)return;
            if(!result){var state=player.level().getBlockState(pos);player.level().sendBlockUpdated(pos,state,state,3);}
            cir.setReturnValue(result);
        }
    }
    @Mixin(BlockBehaviour.BlockStateBase.class)
    public abstract static class ExplosionBlocks {
        @Inject(method="onExplosionHit",at=@At("HEAD"),cancellable=true)
        private void skywars2$beforeExplosionLoot(ServerLevel level,BlockPos pos,net.minecraft.world.level.Explosion explosion,
                java.util.function.BiConsumer<ItemStack,BlockPos> output,CallbackInfo ci){
            var arena=LbcBedwarsArena.at(level);
            if(arena!=null&&explosion.getBlockInteraction()!=net.minecraft.world.level.Explosion.BlockInteraction.TRIGGER_BLOCK&&!arena.allowsRemoval(level,pos))ci.cancel();
        }
    }
    @Mixin(net.minecraft.world.entity.item.FallingBlockEntity.class)
    public abstract static class Falling {
        @WrapOperation(method="fall",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
        private static boolean skywars2$removeFallingSource(Level level,BlockPos pos,BlockState state,Operation<Boolean> original,
                @Share("bedwarsSourceRemoved") LocalBooleanRef removed){
            boolean changed=original.call(level,pos,state);removed.set(changed);return changed;
        }
        @WrapOperation(method="fall",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
        private static boolean skywars2$publishFalling(Level level,Entity entity,Operation<Boolean> original,
                @Share("bedwarsSourceRemoved") LocalBooleanRef removed){
            var arena=LbcBedwarsArena.at(level);
            if(arena!=null&&arena.active()&&!removed.get()){entity.discard();return false;}
            return original.call(level,entity);
        }
    }
    @Mixin(PistonBaseBlock.class)
    public abstract static class Piston {
        @Inject(method="moveBlocks",at=@At("HEAD"),cancellable=true)
        private void skywars2$beforePistonTransaction(Level level,BlockPos piston,Direction direction,boolean extending,CallbackInfoReturnable<Boolean> cir){
            var arena=LbcBedwarsArena.at(level);if(arena==null||!arena.active())return;
            var arm=piston.relative(direction);
            if(!extending&&level.getBlockState(arm).is(Blocks.PISTON_HEAD)&&!arena.allowsRemoval(level,arm)){cir.setReturnValue(false);return;}
            var resolver=new PistonStructureResolver(level,piston,direction,extending);if(!resolver.resolve())return;
            for(var pos:resolver.getToDestroy())if(!arena.allowsRemoval(level,pos)){cir.setReturnValue(false);return;}
            for(var pos:resolver.getToPush())if(!arena.allowsRemoval(level,pos)||!arena.allowsType(level,pos.relative(resolver.getPushDirection()),Blocks.MOVING_PISTON.defaultBlockState())){cir.setReturnValue(false);return;}
            if(extending&&!arena.allowsType(level,arm,Blocks.MOVING_PISTON.defaultBlockState()))cir.setReturnValue(false);
        }
    }
    @Mixin(FlowingFluid.class)
    public abstract static class Fluid {
        @Inject(method="spreadTo",at=@At("HEAD"),cancellable=true)
        private void skywars2$beforeFluidLoot(LevelAccessor level,BlockPos pos,BlockState state,Direction direction,FluidState target,CallbackInfo ci){
            var arena=LbcBedwarsArena.at(level);
            var wanted=state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlockContainer?state:target.createLegacyBlock();
            if(arena!=null&&!arena.allowsType(level,pos,wanted))ci.cancel();
        }
    }
    @Mixin(FireBlock.class)
    public abstract static class Fire {
        @Inject(method="checkBurnOut",at=@At("HEAD"),cancellable=true)
        private void skywars2$beforeBurn(Level level,BlockPos pos,int chance,net.minecraft.util.RandomSource random,int age,CallbackInfo ci){
            var arena=LbcBedwarsArena.at(level);if(arena!=null&&!arena.allowsRemoval(level,pos))ci.cancel();
        }
    }
    @Mixin(TntBlock.class)
    public abstract static class Tnt {
        @Inject(method="prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z",at=@At("HEAD"),cancellable=true)
        private static void skywars2$beforeFuse(Level level,BlockPos pos,LivingEntity source,ItemStack stack,CallbackInfoReturnable<Boolean> cir){
            var arena=LbcBedwarsArena.at(level);if(arena!=null&&!arena.allowsRemoval(level,pos))cir.setReturnValue(false);
        }
    }
    @Mixin(net.minecraft.world.item.BucketItem.class)
    public abstract static class Bucket {
        @Inject(method="emptyContents",at=@At("HEAD"),cancellable=true)
        private void skywars2$beforeBucketLoot(LivingEntity user,Level level,BlockPos pos,net.minecraft.world.phys.BlockHitResult hit,CallbackInfoReturnable<Boolean> cir){
            var arena=LbcBedwarsArena.at(level);if(arena==null||!arena.active())return;
            var fluid=((net.minecraft.world.item.BucketItem)(Object)this).getContent();var state=level.getBlockState(pos);
            boolean waterlogged=state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlockContainer container
                    &&fluid==net.minecraft.world.level.material.Fluids.WATER&&container.canPlaceLiquid(user,level,pos,state,fluid);
            if((state.isAir()||state.canBeReplaced(fluid)||waterlogged)&&!arena.allowsType(level,pos,waterlogged?state:fluid.defaultFluidState().createLegacyBlock()))cir.setReturnValue(false);
        }
    }
    @Mixin({net.minecraft.world.level.block.LiquidBlock.class,net.minecraft.world.level.block.PowderSnowBlock.class})
    public abstract static class BucketPickup {
        @Inject(method="pickupBlock",at=@At("HEAD"),cancellable=true)
        private void skywars2$beforeFilledBucket(LivingEntity user,LevelAccessor level,BlockPos pos,BlockState state,CallbackInfoReturnable<ItemStack> cir){
            var arena=LbcBedwarsArena.at(level);if(arena!=null&&!arena.allowsRemoval(level,pos))cir.setReturnValue(ItemStack.EMPTY);
        }
    }
    @Mixin(targets="net.minecraft.world.entity.monster.Enderman$EndermanTakeBlockGoal")
    public abstract static class EndermanPickup {
        @Shadow @Final private net.minecraft.world.entity.monster.Enderman enderman;
        @Inject(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"),cancellable=true)
        private void skywars2$beforeCarriedBlock(CallbackInfo ci,@Local(name="pos") BlockPos pos){
            var arena=LbcBedwarsArena.at(enderman.level());if(arena!=null&&!arena.allowsRemoval(enderman.level(),pos))ci.cancel();
        }
    }
    @Mixin(PistonBaseBlock.class)
    public interface PistonAccess {
        @org.spongepowered.asm.mixin.gen.Invoker("moveBlocks") boolean skywars2$move(Level level,BlockPos pos,Direction direction,boolean extending);
    }
    @Mixin(FlowingFluid.class)
    public interface FluidAccess {
        @org.spongepowered.asm.mixin.gen.Invoker("spreadTo") void skywars2$spread(LevelAccessor level,BlockPos pos,BlockState state,Direction direction,FluidState target);
    }
    @Mixin(FireBlock.class)
    public interface FireAccess {
        @org.spongepowered.asm.mixin.gen.Invoker("checkBurnOut") void skywars2$burn(Level level,BlockPos pos,int chance,net.minecraft.util.RandomSource random,int age);
    }
}
