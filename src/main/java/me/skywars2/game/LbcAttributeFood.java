package me.skywars2.game;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.ScoreHolder;

/** Native consume_item rewards for the eight attribute foods and three random edible variants. */
public final class LbcAttributeFood {
    private record Food(String flag,Holder<Attribute> attribute,int inputScale,int increase,
                        double outputScale,ParticleOptions particle,int count,double speed) {}
    private static final ParticleOptions IRON=new BlockParticleOption(ParticleTypes.BLOCK,Blocks.IRON_BLOCK.defaultBlockState());
    private static final List<Food> FOODS=List.of(
            new Food("armorattribute",Attributes.ARMOR,1,2,1,IRON,100,0),
            new Food("armor_toughnessattribute",Attributes.ARMOR_TOUGHNESS,1,1,1,IRON,100,0),
            new Food("attack_damageattribute",Attributes.ATTACK_DAMAGE,1,1,1,ParticleTypes.SWEEP_ATTACK,100,0),
            new Food("attack_speedattribute",Attributes.ATTACK_SPEED,5,1,.2,ParticleTypes.CRIT,100,0),
            new Food("knockback_resistanceattribute",Attributes.KNOCKBACK_RESISTANCE,10,1,.1,ParticleTypes.SMOKE,100,0),
            new Food("luckattribute",Attributes.LUCK,1,1,1,ParticleTypes.HAPPY_VILLAGER,100,0),
            new Food("max_healthattribute",Attributes.MAX_HEALTH,1,2,1,ParticleTypes.HEART,10,0),
            new Food("movement_speedattribute",Attributes.MOVEMENT_SPEED,100000,2000,.00001,ParticleTypes.END_ROD,10,1));
    private final GameController game;

    public LbcAttributeFood(GameController game){this.game=game;}

    private boolean admitted(ServerPlayer p){
        return game.playing()&&game.stage()!=GameController.Stage.ENDING&&p.isAlive()&&!p.isRemoved()
                &&p.level()==game.maps().level()&&game.canUseGameplayItems(p)&&!game.isObserver(p)
                &&game.items().wands().validRoleTarget(p,false)&&LbcDimensionItems.ordinary(p)
                &&!game.items().timeStop().stopped(p.level())&&!game.items().wands().timeStopped()
                &&!game.items().wands().inputTimeStopped(p);
    }

    /** Native consumption owns the nutrition, item effects and sole item debit. */
    public void consumed(ServerPlayer p,ItemStack stack){
        if(stack.isEmpty()||!admitted(p))return;
        var data=LbcFartus.data(stack);
        // Independent advancement criteria: a payload bearing several flags can grant several rewards.
        for(var food:FOODS)if(data.getIntOr(food.flag(),0)==1)improve(p,food);
        if(data.getIntOr("random_damage",0)==1)randomize(p,Attributes.ATTACK_DAMAGE,3000);
        if(data.getIntOr("random_speed",0)==1)randomize(p,Attributes.MOVEMENT_SPEED,4000);
        if(data.getIntOr("random_health",0)==1)randomize(p,Attributes.MAX_HEALTH,2500);
    }

    private void improve(ServerPlayer p,Food food){
        var attribute=p.getAttribute(food.attribute());
        if(attribute!=null){
            // AttributeCommand returns a Java int, including truncation toward zero and saturation.
            // The scoreboard addition then uses ordinary signed int overflow before double storage.
            int result=(int)(attribute.getBaseValue()*food.inputScale());
            result+=food.increase();
            attribute.setBaseValue(result*food.outputScale());
        }
        LbcOverload.sound(p,"minecraft:entity.player.levelup",1,1,0);
        p.level().sendParticles(food.particle(),p.getX(),p.getY()+1,p.getZ(),food.count(),.5,.5,.5,food.speed());
    }

    private void randomize(ServerPlayer p,Holder<Attribute> key,int scale){
        var attribute=p.getAttribute(key);
        if(attribute!=null&&attribute.getBaseValue()>0){
            int maximum=Math.max(1,(int)(attribute.getBaseValue()*scale));
            // The singleton range is valid; invalid input never reuses another food's result.
            int value=maximum==1?1:p.level().getRandom().nextInt(maximum)+1;
            game.items().mana().score(ScoreHolder.forNameOnly("#random_out"),"lbc.math").set(value);
            attribute.setBaseValue(value*.001);
        }
        p.addEffect(ItemEffectLevels.create(MobEffects.NAUSEA,60,0,false,false),p);
        p.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,40,0,false,false),p);
        // Source advancement revocation is implicit in repeated native consume_item callbacks.
    }
}
