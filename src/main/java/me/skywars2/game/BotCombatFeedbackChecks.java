package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Actual scheduled Carpet combat, mining and expiring defense, with ordinary finite resources. */
final class BotCombatFeedbackChecks {
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Bot combat feedback: "+why);}
    private static final class Fixture extends CarpetBotChecks.Fixture {
        int phase,at,miningStart=-1,continuous,firstHit=-1;float health,lastYaw;BlockPos lucky;boolean supplied;
        Fixture(GameController game){super(game);}
        Object state(){return ((Map<?,?>)WoolBucketChecks.read(game.carpetBots(),"states")).get(bot.getUUID());}
        @Override void setup(){
            super.setup();if(base.level.getBlockEntity(firstChest) instanceof net.minecraft.world.Container chest)chest.clearContent();
            base.set(firstChest,Blocks.AIR.defaultBlockState());
            bot.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);bot.setHealth(20);bot.getFoodData().setFoodLevel(20);
            bot.getInventory().clearContent();bot.getInventory().setItem(0,new ItemStack(Items.BOW));
            bot.getInventory().setItem(1,new ItemStack(Items.ARROW,16));bot.getInventory().setItem(12,new ItemStack(Items.DIAMOND_AXE));
            move(bot,origin,-90);base.move(base.other,origin.add(4.5,0,0));
            base.other.setHealth(base.other.getMaxHealth());health=base.other.getHealth();lastYaw=bot.getYRot();at=game.server().getTickCount();
        }
        @Override boolean step(){
            base.client.acknowledge();base.audience.acknowledge();int now=game.server().getTickCount();
            require(now-at<200,"bounded phase "+phase+" "+game.carpetBots().diagnostics(bot));
            require(bot.isAlive()&&game.server().getPlayerList().getPlayer(bot.getUUID())==bot,"same ordinary live native player");
            require(Math.abs(net.minecraft.util.Mth.wrapDegrees(bot.getYRot()-lastYaw))<=12.01,"one bounded camera rotation per tick");lastYaw=bot.getYRot();
            if(phase==0){
                require(bot.getInventory().countItem(Items.ARROW)==16,"nearby target is not answered by drawing a bow instead of the owned axe");
                if(base.other.getHealth()>=health)return false;
                require(bot.getMainHandItem().is(Items.DIAMOND_AXE)&&base.other.getLastDamageSource().getEntity()==bot,"real native axe swing hits the nearby player");
                if(firstHit<0)firstHit=now;
                require(bot.getLookAngle().dot(base.other.getEyePosition().subtract(bot.getEyePosition()).normalize())>.9,"short native cooldown retreat keeps the camera on the opponent");
                if(now-firstHit<10)return false;
                game.carpetBots().forget(bot);bot.stopUsingItem();bot.getInventory().clearContent();
                bot.getInventory().setItem(0,new ItemStack(Items.SHIELD));move(bot,origin,-90);base.move(base.other,origin.add(15,0,0));
                phase=1;at=now;return false;
            }
            if(phase==1){
                var ai=state();if(ai==null)return false;
                var defense=(LbcBotDefense)WoolBucketChecks.read(ai,"defense");
                if(!supplied){defense.rangedDamage(base.other);supplied=true;at=now;}
                if(now-at<90)return false;
                require(WoolBucketChecks.read(defense,"rangedAttacker")==null&&!bot.isUsingItem(),"a ceased attack expires instead of leaving a perpetual shield/cover stalemate");
                game.carpetBots().forget(bot);bot.getInventory().clearContent();base.move(base.other,origin.add(84,8,0));move(bot,origin,-90);lastYaw=bot.getYRot();
                lucky=BlockPos.containing(origin).offset(2,0,0);base.set(lucky,LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NORMAL).defaultBlockState());
                bot.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE,400,2,false,false));phase=2;at=now;return false;
            }
            if(phase==3){
                require(bot.hasLineOfSight(base.other),"eye-to-eye visibility passes over the real lower slab");
                require(!LbcBotActions.clearArrowShot(bot,base.other,LbcBotActions.arrowAim(bot,base.other,3),3),"the lower curved arrow trajectory is blocked by that slab");
                return true;
            }
            var ai=state();if(ai==null||!lucky.equals(WoolBucketChecks.read(ai,"mining")))return false;
            int began=(Integer)WoolBucketChecks.read(ai,"miningSince");
            if(miningStart<0){miningStart=began;bot.getInventory().setItem(2,new ItemStack(Items.DIAMOND_CHESTPLATE));}
            require(began==miningStart&&bot.getItemBySlot(EquipmentSlot.CHEST).isEmpty(),"new armor waits while the native continuous break keeps the same start tick");
            if(++continuous<12)return false;
            require(!base.level.getBlockState(lucky).isAir(),"real fatigue leaves a genuinely unfinished native break");
            game.carpetBots().forget(bot);bot.removeAllEffects();base.set(lucky,Blocks.AIR.defaultBlockState());
            move(bot,origin,-90);lastYaw=bot.getYRot();base.move(base.other,origin.add(20,0,0));
            var obstruction=BlockPos.containing(origin.add(18,0,0)).above();
            require(LbcBotActions.clearArrowShot(bot,base.other,LbcBotActions.arrowAim(bot,base.other,3),3),"positive control: unobstructed native arrow arc is admitted");
            base.set(obstruction,Blocks.STONE_SLAB.defaultBlockState());phase=3;at=now;return false;
        }
    }
    static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    static void tick(GameController game){
        if(running==null)return;
        try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS bot-combat-feedback (native axe over close bow, expiring defense, uninterrupted real mining and bounded camera)");}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL bot-combat-feedback",error);}
    }
}
