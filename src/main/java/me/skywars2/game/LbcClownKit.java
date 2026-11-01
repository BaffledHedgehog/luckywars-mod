package me.skywars2.game;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Physical clown protection/gas and the immediate magical escape consumable. */
public final class LbcClownKit {
    private final GameController game;
    private boolean cleansing;
    public LbcClownKit(GameController game){this.game=game;}
    private static boolean flag(ItemStack stack,String key){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr(key,0)==1;}
    public static boolean mask(LivingEntity target){return flag(target.getItemBySlot(EquipmentSlot.HEAD),"clown_mask");}
    public static boolean smoke(ItemStack stack){return flag(stack,"smoke_bomb");}
    public static boolean grave(ItemStack stack){return flag(stack,"false_tumba");}
    public static boolean matches(ItemStack stack){return smoke(stack)||grave(stack);}
    private boolean paused(LivingEntity target){return game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()
            ||game.items().timeStop().frozen(target)||game.items().timeStop().stopped((ServerLevel)target.level())
            ||target.entityTags().contains("time_stopped");}
    private boolean eligible(ServerPlayer player){return game.canUseGameplayItems(player)&&!game.isObserver(player)&&player.isAlive()&&!player.isRemoved()
            &&game.stage()!=GameController.Stage.ENDING&&LbcDimensionItems.ordinary(player)&&game.items().wands().validRoleTarget(player,false);}
    public void effectsChanged(ServerPlayer player){
        if(cleansing||!mask(player)||!eligible(player)||paused(player)||game.items().wands().inputTimeStopped(player))return;
        cleansing=true;try{
            // Clear the entire native type, including hidden levels and owned projections.
            player.removeEffect(MobEffects.BLINDNESS);player.removeEffect(MobEffects.DARKNESS);
        }finally{cleansing=false;}
    }
    public boolean smokeAllowed(ServerLevel level,Vec3 at){return LbcDimensionItems.ordinary(level)
            &&!game.items().magic().mechanismSuppressedAt(level,at);}
    public boolean use(ServerPlayer player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!matches(stack)||!eligible(player)||paused(player)||game.items().actionsBlocked(player)
                ||game.items().wands().inputTimeStopped(player))return false;
        if(smoke(stack)){
            if(game.items().magic().mechanismSuppressed(player)||!smokeAllowed(player.level(),player.getEyePosition()))return false;
            return game.items().hedgehogSpikes().useLegacy(player,hand);
        }
        if(game.items().magic().suppressed(player)||player.entityTags().contains("nomagic_active")
                ||game.items().magic().suppressedAt(player.level(),player.getEyePosition()))return false;
        var level=player.level();var origin=player.position();var feet=BlockPos.containing(origin);
        int ceiling=level.dimension().equals(net.minecraft.world.level.Level.NETHER)?120:level.getMaxY()-1;
        var decoy=new AABB(feet).expandTowards(0,1,0);
        var destination=LbcSurfaceTeleport.destination(level,origin,15,ceiling,player.getRandom(),at->{
            var body=player.getBoundingBox().move(at.subtract(origin));
            return !body.intersects(decoy)&&level.noCollision(player,body);
        });
        // Failed safe-position searches retain the item and do not leave half a decoy.
        if(destination==null||!level.hasChunkAt(feet)||!level.isInsideBuildHeight(feet.getY())||!level.isInsideBuildHeight(feet.getY()+1))return false;
        var velocity=player.getDeltaMovement();var known=player.getKnownMovement();float yaw=player.getYRot(),pitch=player.getXRot();
        if(!player.teleportTo(level,destination.x,destination.y,destination.z,Set.of(),yaw,pitch,true))return false;
        player.setDeltaMovement(velocity);player.setKnownMovement(known);
        game.items().indirectKills().trapChanges(level,player,false,()->{
            LbcDropBlocks.setBlock(level,feet,Blocks.CHISELED_BOOKSHELF.defaultBlockState(),true);
            LbcDropBlocks.setBlock(level,feet.above(),Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING,Direction.UP),true);
        });
        LbcOverload.sound(player,origin,"minecraft:block.wood.place",1,1,0);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.OAK_PLANKS.defaultBlockState()),true,false,
                origin.x,origin.y+1,origin.z,100,.3,.6,.3,0);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));stack.shrink(1);
        player.getInventory().setChanged();game.items().magic().inventoryChanged(player);player.containerMenu.broadcastChanges();
        return true;
    }
    /** One physical WILD detonation; the mask excludes both poison and blindness. */
    public void gas(ServerPlayer owner,Vec3 at){
        var level=owner.level();if(!game.playing()||game.stage()==GameController.Stage.ENDING||!smokeAllowed(level,at)
                ||game.items().timeStop().stopped(level)||game.server().tickRateManager().isFrozen())return;
        var cause=game.items().indirectKills().wildCause(owner);
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(4),e->e.position().distanceToSqr(at)<=16
                &&game.items().wands().validRoleTarget(e,false)&&!mask(e)&&!paused(e))){
            game.items().indirectKills().wildEffect(target,ItemEffectLevels.create(MobEffects.BLINDNESS,100,0),cause);
            game.items().indirectKills().wildEffect(target,ItemEffectLevels.create(MobEffects.POISON,100,0),cause);
        }
        // Source emitted6000 forced particles separately for every victim. A single spatial batch
        // keeps the physical gas readable without multiplying the client load by nearby players.
        level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,true,false,at.x,at.y+.3,at.z,160,1.5,.6,1.5,.03);
    }
}
