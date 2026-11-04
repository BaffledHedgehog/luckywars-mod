package me.skywars2.game;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.ScoreHolder;

/** The six finite source dice outcomes are immediate native operations, not delayed tick discovery. */
public final class LbcGodDice {
    private static final List<Holder<Attribute>> HALF=List.of(Attributes.KNOCKBACK_RESISTANCE,Attributes.ARMOR,
            Attributes.ARMOR_TOUGHNESS,Attributes.ATTACK_DAMAGE,Attributes.ATTACK_SPEED,Attributes.LUCK,Attributes.MAX_HEALTH,Attributes.MOVEMENT_SPEED);
    private static final int[] HALF_SCALES={100,1000,1000,10,100,10,1,10000};
    private LbcGodDice(){}
    public static boolean matches(ItemStack item){return !item.isEmpty()&&ItemData.intValue(item,"superdice",0)==1;}
    public static boolean use(GameController game,ServerPlayer player,InteractionHand hand){
        var item=player.getItemInHand(hand);
        if(!matches(item)||!game.playing()||game.stage()==GameController.Stage.ENDING||!player.isAlive()
                ||!game.canUseGameplayItems(player)||game.isObserver(player)||!LbcDimensionItems.ordinary(player)
                ||game.items().actionsBlocked(player)||!game.items().wands().validRoleTarget(player,false)
                ||game.items().wands().inputTimeStopped(player)||game.items().timeStop().stopped(player.level())
                ||!game.server().tickRateManager().runsNormally())return false;
        int outcome=player.getRandom().nextInt(6)+1;
        if(game.items().mana().score(ScoreHolder.forNameOnly("final_luck_modifier"),"lbc.math").get()==7)outcome=player.getRandom().nextInt(3)+4;
        // Consume the actual hand before a lethal outcome; no vanished snowball is used as an inventory proxy.
        player.awardStat(Stats.ITEM_USED.get(item.getItem()));item.consume(1,player);player.getInventory().setChanged();
        var announced=Component.empty().append(player.getDisplayName().copy().withStyle(ChatFormatting.WHITE))
                .append(Component.translatable("super_dice.event."+outcome).withStyle(ChatFormatting.YELLOW));
        game.server().getPlayerList().broadcastSystemMessage(announced,false);
        player.sendSystemMessage(Component.translatable("super_dice.event.grant").append(Component.literal(" "+outcome+"!")));
        switch(outcome){
            case 1->{if(!player.entityTags().contains("sin_greed_active"))player.kill(player.level());}
            case 2->{for(int i=0;i<HALF.size();i++){var attribute=player.getAttribute(HALF.get(i));if(attribute!=null)
                attribute.setBaseValue(Math.floorDiv(commandValue(attribute.getBaseValue()*HALF_SCALES[i]),2)/(double)HALF_SCALES[i]);}}
            case 3->{for(var effect:List.of(MobEffects.BLINDNESS,MobEffects.HUNGER,MobEffects.MINING_FATIGUE,MobEffects.NAUSEA,MobEffects.SLOWNESS,MobEffects.WEAKNESS))
                player.addEffect(ItemEffectLevels.create(effect,-1,0),player);}
            case 4->{for(var effect:List.of(MobEffects.FIRE_RESISTANCE,MobEffects.HASTE,MobEffects.JUMP_BOOST,MobEffects.INVISIBILITY,MobEffects.SPEED,
                    MobEffects.SLOW_FALLING,MobEffects.NIGHT_VISION,MobEffects.RESISTANCE,MobEffects.WATER_BREATHING))player.addEffect(ItemEffectLevels.create(effect,-1,0),player);}
            case 5->{gain(player,Attributes.ARMOR,1,2,1,3,0);gain(player,Attributes.ARMOR_TOUGHNESS,1,1,1,2,0);
                gain(player,Attributes.ATTACK_DAMAGE,1,1,1,3,1);gain(player,Attributes.ATTACK_SPEED,5,1,.2,5,2);
                gain(player,Attributes.MAX_HEALTH,1,2,1,20,3);gain(player,Attributes.MOVEMENT_SPEED,100000,2000,.00001,1,4);}
            case 6->{var reward=whiteArrow();if(!player.getInventory().add(reward))player.drop(reward,false,net.minecraft.util.Prediction.SERVER_ONLY);}
            default->throw new IllegalStateException("Invalid die face");
        }
        game.items().magic().inventoryChanged(player);player.containerMenu.broadcastChanges();return true;
    }
    /** Attribute command results saturate to int; scoreboard division floors negative operands. */
    private static int commandValue(double value){return (int)Math.floor(value);}
    private static void gain(ServerPlayer player,Holder<Attribute> type,int scale,int addition,double outputScale,int times,int visual){
        var attribute=player.getAttribute(type);if(attribute==null)return;
        for(int n=0;n<times;n++){
            int raw=commandValue(attribute.getBaseValue()*scale)+addition;attribute.setBaseValue(raw*outputScale);
            LbcOverload.sound(player,"minecraft:entity.player.levelup",1,1,0);
            ParticleOptions particle=switch(visual){case 0->new BlockParticleOption(ParticleTypes.BLOCK,Blocks.IRON_BLOCK.defaultBlockState());
                case 1->ParticleTypes.SWEEP_ATTACK;case 2->ParticleTypes.CRIT;case 3->ParticleTypes.HEART;default->ParticleTypes.END_ROD;};
            player.level().sendParticles(particle,player.getX(),player.getY()+1,player.getZ(),visual>=3?10:100,.5,.5,.5,visual==4?1:0);
        }
    }
    static ItemStack whiteArrow(){
        return new ItemStack(LbcWhiteArrowItem.TOKEN);
    }
}
