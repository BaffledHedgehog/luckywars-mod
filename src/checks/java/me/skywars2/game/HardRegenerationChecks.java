package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import me.skywars2.validation.mixin.ItemFixtureAccess;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

/** Compare actual native food ticks across difficulties, without advancing the whole world. */
final class HardRegenerationChecks {
    private record Frame(float health,int food,float saturation,float exhaustion,int timer){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Hard regeneration: "+why);}
    static void run(GameController game) {
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(player!=null&&game.allowDamage(player),"active native participant");
        var saved=new GameChecks.WandThirdPlayerState(player,game.items().mana());
        var food=player.getFoodData();var access=(ItemFixtureAccess.Food)food;
        var absorption=player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION);
        double oldAbsorptionBase=absorption.getBaseValue();
        int oldFood=food.getFoodLevel(),oldTimer=access.validation$timer(),oldTick=player.tickCount,oldInvulnerability=player.getInvulnerableTime(),oldCooldown=player.damageCooldownTime;
        float oldSaturation=food.getSaturationLevel(),oldExhaustion=access.validation$exhaustion();
        var difficulty=game.server().getWorldData().getDifficulty();
        var rules=player.level().getGameRules();boolean regen=rules.get(GameRules.NATURAL_HEALTH_REGENERATION);
        try {
            player.removeAllEffects();player.getInventory().clearContent();player.getAbilities().invulnerable=false;
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            rules.set(GameRules.NATURAL_HEALTH_REGENERATION,true,game.server());
            var baselines=new ArrayList<List<Frame>>();
            for(var selected:List.of(Difficulty.HARD,Difficulty.PEACEFUL,Difficulty.EASY,Difficulty.NORMAL)) {
                game.server().setDifficulty(selected,true);
                require(player.level().getDifficulty()==selected,"real world difficulty remains "+selected);
                player.setHealth(player.getMaxHealth()-10);food.setFoodLevel(10);food.setSaturation(0);
                player.tickCount=20;((ItemFixtureAccess.PlayerSync)player).validation$regeneration();
                require(player.getHealth()==player.getMaxHealth()-10&&food.getFoodLevel()==10&&food.getSaturationLevel()==0,
                        selected+" has no Peaceful health/food/saturation bonus");
                for(int scenario=0;scenario<3;scenario++) {
                    player.setHealth(player.getMaxHealth()-10);player.setAbsorptionAmount(0);
                    food.setFoodLevel(scenario==0?20:18);food.setSaturation(scenario==0?5:0);
                    access.validation$exhaustion(scenario==2?4.01f:0);access.validation$timer(0);
                    var frames=new ArrayList<Frame>();
                    for(int tick=0;tick<180;tick++) {
                        food.tick(player);
                        frames.add(new Frame(player.getHealth(),food.getFoodLevel(),food.getSaturationLevel(),access.validation$exhaustion(),access.validation$timer()));
                    }
                    require(frames.getLast().health()>frames.getFirst().health()||scenario==2,"native healing actually ran");
                    if(selected==Difficulty.HARD)baselines.add(frames);
                    else require(frames.equals(baselines.get(scenario)),selected+" exact native food/regen timeline matches Hard, scenario "+scenario);
                }
                absorption.setBaseValue(20);player.setHealth(1);player.setAbsorptionAmount(5);player.setInvulnerableTime(0);player.damageCooldownTime=0;
                require(player.getAbsorptionAmount()==5,"native absorption capacity prepared before nonlethal starvation probe");
                food.setFoodLevel(0);food.setSaturation(0);access.validation$exhaustion(0);access.validation$timer(0);
                for(int tick=0;tick<80;tick++)food.tick(player);
                require(player.getHealth()==1&&player.getAbsorptionAmount()==4,
                        selected+" starvation still attempts damage at one HP, without Easy/Normal health floor: hp="+player.getHealth()+" absorption="+player.getAbsorptionAmount());
                rules.set(GameRules.NATURAL_HEALTH_REGENERATION,false,game.server());
                player.setHealth(player.getMaxHealth()-10);food.setFoodLevel(20);food.setSaturation(5);
                access.validation$exhaustion(0);access.validation$timer(0);
                for(int tick=0;tick<100;tick++)food.tick(player);
                ((ItemFixtureAccess.PlayerSync)player).validation$regeneration();
                require(player.getHealth()==player.getMaxHealth()-10,"natural regeneration gamerule remains authoritative");
                rules.set(GameRules.NATURAL_HEALTH_REGENERATION,true,game.server());
            }
            me.skywars2.SkyWars2.LOGGER.info("[check] PASS hard-regeneration (all four world difficulties, exact native food/saturation/exhaustion/heal timelines, starvation below Normal floor, no Peaceful bonus, gamerule preserved)");
        } finally {
            game.server().setDifficulty(difficulty,true);rules.set(GameRules.NATURAL_HEALTH_REGENERATION,regen,game.server());
            absorption.setBaseValue(oldAbsorptionBase);saved.restore(game);food.setFoodLevel(oldFood);food.setSaturation(oldSaturation);
            access.validation$exhaustion(oldExhaustion);access.validation$timer(oldTimer);
            player.tickCount=oldTick;player.setInvulnerableTime(oldInvulnerability);player.damageCooldownTime=oldCooldown;
        }
    }
}
