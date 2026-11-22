package me.skywars2.game.items.weapons.ranged;

import me.skywars2.SkyWars2;
import me.skywars2.game.entities.LbcDimensionItems;
import me.skywars2.game.items.ItemData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Blocks;

/** The original ground minigun transformations run before typed items reject fire/explosion damage. */
public final class LbcGroundCrossbowCrafts {
    private LbcGroundCrossbowCrafts(){}
    private static boolean admitted(ItemEntity item){
        var game=SkyWars2.game();
        return game!=null&&!item.isRemoved()&&game.playing()
                &&(item.level()==game.maps().level()||item.level().dimension().equals(me.skywars2.game.entities.mobs.hedgehogs.LbcHedgehogDimensions.IMPRINTED))
                &&LbcDimensionItems.crafting(item.level())&&ItemData.intValue(item.getItem(),"fast",0)==1
                &&!game.items().timeStop().stopped((ServerLevel)item.level())&&!game.items().timeStop().frozen(item);
    }
    public static boolean tick(ItemEntity item){
        if(!admitted(item))return false;
        var level=(ServerLevel)item.level();
        if(level.getBlockState(item.blockPosition()).is(Blocks.FIRE)||level.getBlockState(item.blockPosition()).is(Blocks.SOUL_FIRE))return transform(item,false);
        for(var tnt:level.getEntitiesOfClass(PrimedTnt.class,item.getBoundingBox().inflate(7),t->t.getFuse()==2))
            if(tnt.distanceToSqr(item)<=49)return transform(item,true);
        return false;
    }
    public static boolean hurt(ItemEntity item,DamageSource damage){
        return admitted(item)&&(damage.is(DamageTypeTags.IS_EXPLOSION)?transform(item,true):damage.is(DamageTypeTags.IS_FIRE)&&transform(item,false));
    }
    private static boolean transform(ItemEntity item,boolean explosion){
        var level=(ServerLevel)item.level();
        var kind=explosion?LbcProjectileWeaponItems.Kind.GHAST_CROSSBOW:LbcProjectileWeaponItems.Kind.BLAZE_CROSSBOW;
        var reward=kind.item.getDefaultInstance().copyWithCount(item.getItem().getCount());
        item.setItem(reward);item.setDeltaMovement(0,.5,0);item.setRemainingFireTicks(0);item.setDefaultPickUpDelay();
        if(!explosion){
            for(var pos:net.minecraft.core.BlockPos.betweenClosed(item.blockPosition().offset(-1,-1,-1),item.blockPosition().offset(1,1,1)))
                if(level.getBlockState(pos).is(Blocks.FIRE))level.setBlock(pos,Blocks.AIR.defaultBlockState(),net.minecraft.world.level.block.Block.UPDATE_CLIENTS|net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
        }
        level.sendParticles(explosion?ParticleTypes.EXPLOSION_EMITTER:ParticleTypes.FLAME,item.getX(),item.getY()+1,item.getZ(),explosion?1:40,1,1,1,0);
        return true;
    }
}
