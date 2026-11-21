package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.game.entities.bots.LbcBotCrafting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

/** Real Carpet input/physics regressions for full inventories and exposed melee retreats. */
public final class BotMovementFeedbackChecks {
    private BotMovementFeedbackChecks() {}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Bot movement feedback: "+why);}
    private static final class Fixture extends CarpetBotChecks.Fixture {
        final boolean edge;final String label;
        int at,phase,firstHit=-1;float health;Vec3 start;double lastYaw;
        PlayerTeam team;final Map<UUID,Vec3> starts=new LinkedHashMap<>();
        Fixture(GameController game,boolean edge){super(game,new Vec3(.5,120,.5));this.edge=edge;label=edge?"bot-edge-feedback":"bot-full-inventory";}
        Object state(){return ((Map<?,?>)WoolBucketChecks.read(game.carpetBots(),"states")).get(bot.getUUID());}
        @Override void setup(){
            super.setup();
            if(base.level.getBlockEntity(firstChest) instanceof net.minecraft.world.Container chest)chest.clearContent();
            var center=BlockPos.containing(origin);
            for(int x=(center.getX()-17)>>4;x<=(center.getX()+37)>>4;x++)for(int z=(center.getZ()-17)>>4;z<=(center.getZ()+17)>>4;z++){
                long key=ChunkPos.pack(x,z);if(!base.level.getForceLoadedChunks().contains(key)){forced.add(key);base.level.setChunkForced(x,z,true);}base.level.getChunk(x,z);
            }
            for(int x=-16;x<=35;x++)for(int z=-16;z<=16;z++)for(int y=-3;y<=3;y++)
                base.set(center.offset(x,y,z),x<=16&&y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
            if(!edge){
                for(int i=0;i<6;i++)create("botBench0"+i,origin);
                var board=game.server().getScoreboard();require(board.getPlayerTeam("botMovement")==null,"owned temporary friendly team");
                team=board.addPlayerTeam("botMovement");
            }
            for(int i=0;i<actors.size();i++){
                var actor=actors.get(i);game.carpetBots().forget(actor);actor.stopUsingItem();actor.getInventory().clearContent();
                for(int slot=0;slot<36;slot++)actor.getInventory().setItem(slot,new ItemStack(Items.POTATO,64));
                actor.getInventory().setItem(0,new ItemStack(Items.DIAMOND_SWORD));
                actor.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);actor.setHealth(20);
                actor.getFoodData().setFoodLevel(20);actor.getFoodData().setSaturation(20);
                move(actor,edge?origin.add(15.85,0,0):origin.add((i%3-1)*3,0,(i/3-1)*3),edge?90:0);
                starts.put(actor.getUUID(),actor.position());
                if(team!=null)game.server().getScoreboard().addPlayerToTeam(actor.getScoreboardName(),team);
            }
            if(edge)base.move(base.other,origin.add(13.35,0,0));
            start=bot.position();health=base.other.getHealth();at=game.server().getTickCount();lastYaw=bot.getYRot();
        }
        @Override boolean step(){
            base.client.acknowledge();base.audience.acknowledge();int now=game.server().getTickCount(),elapsed=now-at;
            require(elapsed<180,"bounded phase "+phase+" "+game.carpetBots().diagnostics(bot));
            require(bot.isAlive()&&bot.getMaxHealth()==20&&!bot.isNoGravity(),"ordinary live20HP native Carpet body and gravity");
            require(Math.abs(net.minecraft.util.Mth.wrapDegrees((float)(bot.getYRot()-lastYaw)))<=12.01,"one bounded camera rotation per native tick");lastYaw=bot.getYRot();
            if(!edge){
                if(phase==0){
                    if(elapsed<80)return false;
                    require(actors.size()==7,"seven actual native Carpet actors in the shared scene");
                    for(var actor:actors){
                        require(actor.isAlive()&&actor.getMaxHealth()==20&&!actor.isNoGravity(),"all seven keep ordinary health and gravity");
                        require(actor.position().subtract(starts.get(actor.getUUID())).horizontalDistanceSqr()>4,"a full ordinary inventory must not leave a roaming bot AFK: "+actor.getScoreboardName()+" "+game.carpetBots().diagnostics(actor));
                        require(actor.getInventory().getFreeSlot()<0,"real inventories remain full; no silent item deletion creates a fake recovery");
                        var ai=((Map<?,?>)WoolBucketChecks.read(game.carpetBots(),"states")).get(actor.getUUID());
                        require(((LbcBotCrafting)WoolBucketChecks.read(ai,"crafting")).prepared(),"unavailable crafting no longer blocks ordinary goals");
                    }
                    base.move(base.other,bot.position().add(2,0,0));health=base.other.getHealth();phase=1;at=now;return false;
                }
                if(base.other.getHealth()>=health||base.other.getLastDamageSource().getEntity()!=bot)return false;
                require(bot.getMainHandItem().is(Items.DIAMOND_SWORD),"the same full-inventory bot responds with a real native sword hit");
                SkyWars2.LOGGER.info("[check] PASS bot-full-inventory (seven full36slot inventories retained, all seven native roam, ordinary20HP and real nearby enemy hit)");return true;
            }
            require(bot.getY()>=origin.y-.1,"retreat and ordinary melee knockback must not send the bot off its supported platform: "+game.carpetBots().diagnostics(bot));
            if(phase==0){
                if(base.other.getHealth()>=health)return false;
                if(firstHit<0)firstHit=now;
                if(now-firstHit<12)return false;
                require(bot.getX()<start.x-.35,"cooldown retreat must gain inland footing rather than stay exposed along the void edge: start="+start+" now="+bot.position()+" "+game.carpetBots().diagnostics(bot));
                float before=bot.getHealth();
                require(base.other.distanceToSqr(bot)<16&&base.other.hasLineOfSight(bot),"real unobstructed nearby player can deliver the knockback control");
                base.other.connection.handleAttack(new ServerboundAttackPacket(bot.getId()));
                require(bot.getHealth()<before&&bot.getLastDamageSource().getEntity()==base.other,"actual player attack packet damages the bot, without invulnerability");
                require(bot.getDeltaMovement().horizontalDistanceSqr()>.001,"the native damage really applies physical knockback");
                phase=1;at=now;return false;
            }
            if(phase==1){
                if(elapsed<24)return false;
                require(bot.onGround(),"ordinary native landing after the real knockback");
                prepareThreatWhileLooting();
                phase=2;at=now;return false;
            }
            if(elapsed>=2)require(bot.containerMenu==bot.inventoryMenu,
                    "native chest closes before the ground-melee retreat can block block selection");
            if(game.carpetBots().observation(bot).placements()==0)return false;
            SkyWars2.LOGGER.info("[check] PASS bot-edge-feedback (bounded camera, real melee knockback, native survival, and actual defensive placement after closing chest)");return true;
        }
        void prepareThreatWhileLooting(){
            base.move(base.other,origin.add(-12,0,0));
            game.carpetBots().forget(bot);
            bot.setLastHurtByMob(null);
            bot.getInventory().setItem(1,new ItemStack(Items.STONE,8));
            bot.openMenu(new SimpleMenuProvider((sync,inventory,player)->ChestMenu.oneRow(sync,inventory),
                    Component.literal("Starter chest")));
            require(bot.containerMenu instanceof ChestMenu,"real vanilla chest screen is open before the threat");
            var zombie=EntityTypes.ZOMBIE.create(base.level,EntitySpawnReason.COMMAND);
            require(zombie!=null,"native ground threat factory");
            zombie.setPos(bot.position().add(-3,0,0));
            zombie.setNoAi(true);
            zombie.setTarget(bot);
            require(base.level.addFreshEntity(zombie),"native zombie targets the looting bot");
            base.own(zombie);
        }
        @Override public void close(){
            Throwable failure=null;try{super.close();}catch(Throwable error){failure=error;}
            var board=game.server().getScoreboard();if(team!=null&&board.getPlayerTeam(team.getName())==team)board.removePlayerTeam(team);
            if(failure!=null)throw new IllegalStateException("Movement feedback cleanup",failure);
        }
    }
    public static void run(GameController game,boolean edge){require(running==null,"exclusive fixture");running=new Fixture(game,edge);try{running.setup();}catch(Throwable error){running.close();running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;}catch(Throwable error){var label=running.label;try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL "+label,error);}}
}
