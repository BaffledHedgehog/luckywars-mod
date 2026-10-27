package me.skywars2.game;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** Equipped Paladin, Shitty and Erosion passives, sharing the source five/one-second phase. */
public final class LbcArmorSets {
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
    private final GameController game;
    private int phase;
    private String epoch;

    public LbcArmorSets(GameController game){this.game=game;}

    private boolean admitted(ServerPlayer player){
        return player.isAlive()&&!player.isRemoved()&&player.level()==game.maps().level()
                &&game.stage()!=GameController.Stage.ENDING&&game.canUseGameplayItems(player)
                &&!game.isObserver(player)&&game.items().wands().validRoleTarget(player,false)
                &&!game.items().wands().inputTimeStopped(player)&&!game.items().wands().timeStopped()
                &&!game.items().timeStop().stopped(player.level())
                &&LbcDimensionItems.ordinary(player);
    }

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
        if(game.maps().level()==null||game.stage()==GameController.Stage.ENDING||game.items().wands().timeStopped())return;
        phase=(phase+1)%20;
        if(phase%5!=0)return;
        for(var player:game.server().getPlayerList().getPlayers())if(admitted(player)){
            var stacks=worn(player);
            erode(player,stacks);
            if(phase==0)pulse20(player,stacks);
        }
    }

    void pulse20(ServerPlayer player){if(admitted(player))pulse20(player,worn(player));}

    private void pulse20(ServerPlayer player,ItemStack[] stacks){
        if(count(stacks,"paladin")==4)player.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,60,3),player);
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
        var protection=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
        int level=EnchantmentHelper.getItemEnchantmentLevel(protection,stack);
        if(level<=0)return false;
        EnchantmentHelper.updateEnchantments(stack,mutable->mutable.set(protection,level-1));
        return true;
    }

    public void stop(){phase=0;epoch=null;}
    public void forget(ServerPlayer player){ }
}
