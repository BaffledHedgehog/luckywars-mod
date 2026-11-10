package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real Carpet input: sustained native mace use must yield to pursuit/melee and retire with its target. */
final class BotUseChecks {
    private BotUseChecks(){}
    private static Fixture running;
    private static final class Fixture extends BotStarterChecks.Fixture {
        int started,trial,when;boolean sawChannel;long initialHits;Vec3 from;
        Fixture(GameController game){super(game);}
        @Override void setup(){super.setup();
            base.set(firstChest,Blocks.AIR.defaultBlockState());game.carpetBots().forget(bot);bot.getInventory().clearContent();
            bot.getInventory().setItem(0,new ItemStack(LbcSoulhunter.MACE));
            bot.getInventory().setItem(1,new ItemStack(LbcNexusToolItems.COMPASS));bot.getInventory().setItem(2,new ItemStack(LbcBalanceItems.BALANCE));
            require(!LbcTmtExtraUses.combatCandidate(bot.getInventory().getItem(1))&&!LbcTmtExtraUses.combatCandidate(bot.getInventory().getItem(2)),"native utility owner does not classify travel or Balance as aimed combat");
            base.other.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);base.other.setHealth(200);
            base.move(base.other,origin.add(15,0,0));from=bot.position();started=game.server().getTickCount();
            initialHits=game.carpetBots().observation(bot).hits();
        }
        @Override boolean step(){
            base.client.acknowledge();base.audience.acknowledge();int now=game.server().getTickCount();
            require(now-started<300,"bounded native mace pursuit and release: "+game.carpetBots().diagnostics(bot));
            require(bot.isAlive()&&bot.getHealth()==20,"ordinary20HP bot remains healthy");
            require(!game.balanceLegacy()&&bot.level()==base.level&&bot.getInventory().getItem(1).is(LbcNexusToolItems.COMPASS)
                    &&bot.getInventory().getItem(2).is(LbcBalanceItems.BALANCE),"combat does not consume Balance or teleport through Nexus compass");
            boolean held=bot.isUsingItem()&&LbcSoulhunter.isMace(bot.getUseItem());
            if(held){sawChannel=true;require(game.items().mana().hudValue(bot,"mace_2")>0,"channel began through the actual native mace owner and cooldown");}
            if(trial==0){
                if(game.carpetBots().observation(bot).hits()<=initialHits)return false;
                require(sawChannel&&bot.position().distanceToSqr(from)>4,"real channel, physical pursuit and native melee contact all occurred");
                require(base.other.getHealth()<200,"actual target damaged after channel yields to melee");
                base.move(base.other,bot.position().add(10,0,0));trial=1;return false;
            }
            if(trial==1){if(!held)return false;base.move(base.other,origin.add(84,8,0));trial=2;when=now;return false;}
            require(now-when<15,"lost target cannot leave an infinite channel");
            if(bot.isUsingItem())return false;
            SkyWars2.LOGGER.info("[check] bot-item-use real native mace chase/melee/lost-target release ticks={}",now-started);return true;
        }
        @Override public void close(){
            for(var actor:base.level.getAllEntities())if(!(actor instanceof net.minecraft.server.level.ServerPlayer)
                    &&!preexisting.contains(actor.getUUID())&&actor.position().distanceToSqr(origin)<120*120)base.owned.add(actor.getUUID());
            super.close();
        }
    }
    static void run(GameController game){if(running!=null)throw new IllegalStateException("Exclusive bot item fixture");var next=new Fixture(game);
        try{next.setup();running=next;}catch(Throwable failure){next.close();throw failure;}}
    static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS bot-item-use (native held mace, actual movement and melee, lost-target release, utilities retained)");
    }catch(Throwable failure){try{running.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL bot-item-use",failure);}}
}
