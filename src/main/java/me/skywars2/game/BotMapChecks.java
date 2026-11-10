package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gamerules.GameRules;

/** Actual loaded island, original spawn/chest markers and the active mode's native loot. */
final class BotMapChecks {
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Bot map: "+why);}
    static class Fixture extends CarpetBotChecks.Fixture {
        final Set<BlockPos> starters=new HashSet<>(),opened=new HashSet<>();
        final Set<UUID> preexisting=new HashSet<>();int at;boolean landed,suppressLucky=true;
        Fixture(GameController game){super(game,game.maps().spawns().getFirst().position());}
        Object state(){return ((Map<?,?>)WoolBucketChecks.read(game.carpetBots(),"states")).get(bot.getUUID());}
        @Override void setup(){
            require(game.gameMode()==1||game.gameMode()==2,"real SkyWars match mode");
            for(var entity:base.level.getAllEntities()){
                preexisting.add(entity.getUUID());
                if(entity.entityTags().contains("swrg.default")&&entity.position().distanceToSqr(origin)<18*18){
                    var pos=entity.blockPosition();
                    if(base.level.getBlockEntity(pos) instanceof Container){starters.add(pos);base.remember(pos);}
                }
            }
            require(starters.size()==3,"map1 island has its three actual starter chest markers: "+starters);
            base.prepareParticipants();
            base.level.getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION,base.regen,game.server());
            base.move(base.owner,origin.add(120,40,120));base.move(base.other,origin.add(124,40,120));
            var center=BlockPos.containing(origin);
            for(int x=(center.getX()-20)>>4;x<=(center.getX()+20)>>4;x++)for(int z=(center.getZ()-20)>>4;z<=(center.getZ()+20)>>4;z++){
                long key=ChunkPos.pack(x,z);if(!base.level.getForceLoadedChunks().contains(key)){forced.add(key);base.level.setChunkForced(x,z,true);}
                base.level.getChunk(x,z);
            }
            bot=create("botNativeA",origin);bot.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);bot.setHealth(20);
            bot.getFoodData().setFoodLevel(20);bot.getFoodData().setSaturation(5);at=game.server().getTickCount();
            SkyWars2.LOGGER.info("[check] Actual bot island mode={} spawn={} chests={} table={}",game.gameMode(),origin,starters,MapManager.chestLootTable(game.gameMode(),"default",false));
        }
        @Override boolean step(){
            base.client.acknowledge();base.audience.acknowledge();int elapsed=game.server().getTickCount()-at;
            require(bot.isAlive()&&game.server().getPlayerList().getPlayer(bot.getUUID())==bot,"same native20HP body loots the island");
            require(elapsed<1800,"actual island loot deadline: "+game.carpetBots().diagnostics(bot));
            require(bot.getMaxHealth()==20&&!bot.isNoGravity(),"ordinary health and gravity");
            var state=state();if(state==null)return false;
            // This scene ends before opening random Lucky blocks; all movement/loot/tool recipes remain native.
            try{if(suppressLucky){var field=state.getClass().getDeclaredField("nextLuckyScan");field.setAccessible(true);field.setInt(state,Integer.MAX_VALUE);}}
            catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
            var survival=(LbcBotSurvival)WoolBucketChecks.read(state,"survival");
            if(!landed&&survival.hasSafe){
                require(survival.safe.y<origin.y-3&&survival.climbGoal==null,"first real island landing establishes support below the spawn cage");landed=true;
            }
            var chest=(BlockPos)WoolBucketChecks.read(state,"chest");
            if(bot.containerMenu!=bot.inventoryMenu&&starters.contains(chest))opened.add(chest);
            if(elapsed%100==0)SkyWars2.LOGGER.info("[check] Actual bot island ticks={} opened={} landed={} {}",elapsed,opened,landed,game.carpetBots().diagnostics(bot));
            if(opened.containsAll(starters)&&!Boolean.TRUE.equals(WoolBucketChecks.read(state,"startingLoot"))
                    &&((LbcBotCrafting)WoolBucketChecks.read(state,"crafting")).prepared()){
                require(landed&&game.carpetBots().observation(bot).looted()>0,"actual chest menu transfers precede completing starter preparation");
                SkyWars2.LOGGER.info("[check] Actual island complete mode={} ticks={} opened={} inventory={}",game.gameMode(),elapsed,opened,
                        java.util.stream.IntStream.range(0,36).mapToObj(slot->bot.getInventory().getItem(slot)).toList());return true;
            }
            return false;
        }
        @Override public void close(){
            for(var entity:base.level.getAllEntities())if(!(entity instanceof ServerPlayer)&&!preexisting.contains(entity.getUUID())
                    &&entity.position().distanceToSqr(origin)<120*120)base.owned.add(entity.getUUID());
            super.close();
        }
    }
    static void run(GameController game){
        require(running==null&&!game.maps().spawns().isEmpty(),"one fixture on an actual prepared map");running=new Fixture(game);
        try{running.setup();}catch(Throwable failure){try{running.close();}finally{running=null;}throw failure;}
    }
    static void tick(GameController game){
        if(running==null)return;
        try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS bot-map-start");}}
        catch(Throwable failure){try{running.close();}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL bot-map-start",failure);}
    }
}
