package me.skywars2.game;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;

/** Equipped armor passives, sharing the source tick/five-tick/one-second phases. */
public final class LbcArmorSets {
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
    // The asymmetrical sixteen-point source pattern is repeated at two heights.
    private static final int[][] ASSASSIN_OFFSETS={{0,0},{4,0},{-4,0},{0,4},{0,-4},{4,4},{-4,4},{-4,-4},{4,-4},
            {8,-4},{8,0},{8,4},{8,8},{4,8},{0,8},{-4,8}};
    private static final BlockParticleOption ASSASSIN_PARTICLE=new BlockParticleOption(ParticleTypes.BLOCK_MARKER,
            Blocks.CONCRETE_POWDER.pick(net.minecraft.world.item.DyeColor.BLACK).defaultBlockState());
    private final GameController game;
    private final LbcErosionSuppression erosionSuppression;
    private int phase;
    private String epoch;

    public LbcArmorSets(GameController game){this.game=game;this.erosionSuppression=new LbcErosionSuppression(game,this::eligible);}

    private boolean eligible(ServerPlayer player){
        return player.isAlive()&&!player.isRemoved()&&player.level()==game.maps().level()
                &&game.stage()!=GameController.Stage.ENDING&&game.canUseGameplayItems(player)
                &&!game.isObserver(player)&&game.items().wands().validRoleTarget(player,false)
                &&LbcDimensionItems.ordinary(player);
    }
    private boolean admitted(ServerPlayer player){return eligible(player)&&!game.items().wands().inputTimeStopped(player)
            &&!game.items().wands().timeStopped()&&!game.items().timeStop().stopped(player.level());}

    private static boolean tagged(ItemStack stack,String key){
        return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr(key,0)==1;
    }

    private static ItemStack[] worn(ServerPlayer player){
        var stacks=new ItemStack[4];
        for(int i=0;i<4;i++)stacks[i]=player.getItemBySlot(SLOTS[i]);
        return stacks;
    }

    private static int count(ItemStack[] stacks,String key){
        int count=0;for(var stack:stacks)if(tagged(stack,key))count++;
        return count;
    }

    public void tick(){
        if(!game.items().currentEpoch(epoch)){phase=0;epoch=game.items().epoch();}
        for(var player:game.server().getPlayerList().getPlayers())erosionSuppression.tick(player);
        if(game.maps().level()==null||game.stage()==GameController.Stage.ENDING||game.items().wands().timeStopped())return;
        phase=(phase+1)%20;
        for(var player:game.server().getPlayerList().getPlayers())if(admitted(player)){
            var stacks=worn(player);
            assassin(player,stacks);
            if(phase%5==0)erode(player,stacks);
            if(phase==0)pulse20(player,stacks);
        }
    }

    void assassin(ServerPlayer player){if(admitted(player))assassin(player,worn(player));}

    private void assassin(ServerPlayer player,ItemStack[] stacks){
        if(count(stacks,"assasin")!=4||game.items().magic().beaconSuppressed(player.position()))return;
        player.addEffect(ItemEffectLevels.create(MobEffects.INVISIBILITY,100,1,false,false),player);
        player.removeEffect(MobEffects.DARKNESS);
        player.removeEffect(MobEffects.BLINDNESS);
        // These are private decoys for opponents, not a global particle broadcast or damage targeting.
        // In particular, Greed does not suppress a viewer's ability to see someone else's visual.
        for(var viewer:game.server().getPlayerList().getPlayers()){
            if(viewer==player||viewer.level()!=player.level()||player.isAlliedTo(viewer)
                    ||viewer.position().distanceToSqr(player.position())<.0001
                    ||!game.canUseGameplayItems(viewer)||game.isObserver(viewer)
                    ||!game.items().wands().validRoleTarget(viewer,false))continue;
            for(double height:new double[]{1.6,.6})for(var offset:ASSASSIN_OFFSETS)
                player.level().sendParticles(viewer,ASSASSIN_PARTICLE,true,false,
                        player.getX()+offset[0],player.getY()+height,player.getZ()+offset[1],3,.2,.2,.2,0);
        }
    }

    void pulse20(ServerPlayer player){if(admitted(player))pulse20(player,worn(player));}

    private void pulse20(ServerPlayer player,ItemStack[] stacks){
        if(count(stacks,"paladin")==4&&!game.items().magic().beaconSuppressed(player.position()))
            player.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,60,3),player);
        // Author: the biological smell is never suppressed by either jammer.
        int amount=count(stacks,"gownoleggings");
        if(game.config().lbcSkills&&game.players().get(player.getUUID()).lbcSkill==9)amount*=2;
        if(amount==0)return;
        double radius=10*amount,witherRadius=amount>=4?4*(amount-3):0;
        for(var target:player.level().getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(radius),
                entity->game.items().wands().projectileTarget(player,entity))){
            double distance=target.position().distanceToSqr(player.position());
            if(distance<.0001||distance>radius*radius)continue;
            // Native sourced effects retain the wearer through poison, wither and hunger kill attribution.
            target.addEffect(ItemEffectLevels.create(MobEffects.POISON,60,0),player);
            target.addEffect(ItemEffectLevels.create(MobEffects.HUNGER,60,0),player);
            if(witherRadius>0&&distance<=witherRadius*witherRadius)
                target.addEffect(ItemEffectLevels.create(MobEffects.WITHER,60,3),player);
        }
        double spread=amount==1?16:8*amount;
        player.level().sendParticles(ParticleTypes.SNEEZE,player.getX(),player.getY()+1,player.getZ(),amount,spread,spread,spread,0);
    }

    void erode(ServerPlayer player){if(admitted(player))erode(player,worn(player));}

    private void erode(ServerPlayer player,ItemStack[] stacks){
        int amount=count(stacks,"erosion");
        if(amount==0)return;
        boolean changed=false;
        // A full set rolls every slot, even exhausted pieces: the original random choice can be wasted.
        if(amount==4)changed=decreaseProtection(player,stacks[player.getRandom().nextInt(4)]);
        else for(var stack:stacks)if(tagged(stack,"erosion"))changed|=decreaseProtection(player,stack);
        if(changed)player.getInventory().setChanged();
    }

    private static boolean decreaseProtection(ServerPlayer player,ItemStack stack){
        if(LbcErosionSuppression.parked(stack))return false;
        var protection=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
        int level=EnchantmentHelper.getItemEnchantmentLevel(protection,stack);
        if(level<=0)return false;
        EnchantmentHelper.updateEnchantments(stack,mutable->mutable.set(protection,level-1));
        return true;
    }

    public void inventoryChanged(ServerPlayer player){erosionSuppression.inventoryChanged(player);}
    public void loaded(Entity entity){erosionSuppression.loaded(entity);}
    public boolean recover(ItemStack stack,ServerLevel level){return erosionSuppression.recover(stack,level);}
    public void stop(){erosionSuppression.stop();phase=0;epoch=null;}
    public void forget(ServerPlayer player){erosionSuppression.forget(player);}
}
