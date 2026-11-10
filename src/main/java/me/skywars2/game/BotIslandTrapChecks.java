package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/** Actual island/three starter chests/current-mode loot followed by an original Lucky trap. */
final class BotIslandTrapChecks {
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Actual island trap: "+why);}
    private static final class Fixture extends BotMapChecks.Fixture {
        final String kind;Vec3 center;boolean started;int trapAt;float lowestHealth=20;
        Fixture(GameController game,String kind){super(game);this.kind=kind;}
        @Override boolean step(){
            if(!started){
                if(!super.step()||!bot.onGround())return false;
                var survival=(LbcBotSurvival)WoolBucketChecks.read(state(),"survival");
                require(Math.abs(survival.safe.y-bot.getY())<1,"completed native lower-island route updates recovery footing before the trap: safe="+survival.safe+" body="+bot.position());
                center=bot.position();var before=new HashSet<UUID>();base.level.getAllEntities().forEach(entity->before.add(entity.getUUID()));
                // Reuse the ordinary block-break reward authority, retaining its acquisition
                // author for encounter admission while every actual trap remains WILD.
                try(var scope=LbcLuckyBlocks.rewardScope(LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NORMAL).defaultBlockState(),bot)){
                    var token=new ItemEntity(base.level,center.x,center.y,center.z,BotTrapChecks.sourceToken(game,bot,kind));token.setThrower(bot);
                    game.items().markCurrentItemEpoch(token);require(base.level.addFreshEntity(token),"publish actual source table token");base.owned.add(token.getUUID());
                    require(game.items().specialDrops().spawn(token),"ordinary Lucky dispatch accepts "+kind);token.discard();
                }
                for(var entity:base.level.getAllEntities())if(!before.contains(entity.getUUID())&&!(entity instanceof Player)){
                    base.owned.add(entity.getUUID());
                    if(entity instanceof Projectile shot)require(shot.getOwner()!=bot&&game.items().spellSources().projectileController(shot)!=bot,
                            "trap ammunition is not the block breaker's physical projectile");
                }
                started=true;trapAt=game.server().getTickCount();lowestHealth=bot.getHealth();
                SkyWars2.LOGGER.info("[check] Actual trap headroom {} {}",kind,java.util.stream.IntStream.rangeClosed(1,6)
                        .mapToObj(y->base.level.getBlockState(bot.blockPosition().above(y)).toString()).toList());
                SkyWars2.LOGGER.info("[check] Actual island trap {} mode={} at={} initialHP={} opened={} table={}",
                        kind,game.gameMode(),center,lowestHealth,opened,MapManager.chestLootTable(game.gameMode(),"default",false));return false;
            }
            base.client.acknowledge();base.audience.acknowledge();int elapsed=game.server().getTickCount()-trapAt;
            require(bot.isAlive()&&game.server().getPlayerList().getPlayer(bot.getUUID())==bot,
                    "same real20HP starter-looted body survives "+kind+" at "+elapsed+" "+game.carpetBots().diagnostics(bot));
            require(bot.getMaxHealth()==20&&!bot.isNoGravity(),"ordinary health and gravity throughout the encounter");
            lowestHealth=Math.min(lowestHealth,bot.getHealth());
            if(kind.equals("explode")&&elapsed<160&&elapsed%5==0){
                var survival=(LbcBotSurvival)WoolBucketChecks.read(state(),"survival");var damage=bot.getLastDamageSource();
                SkyWars2.LOGGER.info("[check] Native TNT flight tick={} at={} motion={} ground={} falling={} damage={} safe={} landing={} input={} {}",
                        elapsed,bot.position(),bot.getDeltaMovement(),bot.onGround(),bot.fallDistance,
                        damage==null?null:damage.typeHolder().unwrapKey().orElse(null),survival.safe,survival.landing,bot.getLastClientInput(),survival.diagnostics());
            }
            if(elapsed%100==0)SkyWars2.LOGGER.info("[check] Actual island trap {} ticks={} at={} health={} {}",kind,elapsed,bot.position(),bot.getHealth(),game.carpetBots().diagnostics(bot));
            if((kind.equals("ultraponos")&&elapsed>=280&&elapsed<=400
                    ||kind.equals("korrosion")&&elapsed>=180&&elapsed<=600
                    ||kind.equals("explode")&&elapsed>=300&&elapsed<=800)&&elapsed%2==0){
                var brain=(LbcChefBrain)WoolBucketChecks.read(state(),"brain");
                var step=((java.util.Deque<?>)WoolBucketChecks.read(brain,"path")).peek();net.minecraft.core.BlockPos bank=null,from=null;
                if(step!=null){var to=WoolBucketChecks.read(step,"to");var start=WoolBucketChecks.read(step,"from");bank=net.minecraft.core.BlockPos.containing(
                        (int)WoolBucketChecks.read(to,"x")+.5,(int)WoolBucketChecks.read(to,"height")/16d-.05,(int)WoolBucketChecks.read(to,"z")+.5);
                    from=net.minecraft.core.BlockPos.containing((int)WoolBucketChecks.read(start,"x")+.5,
                            (int)WoolBucketChecks.read(start,"height")/16d-.05,(int)WoolBucketChecks.read(start,"z")+.5);}
                SkyWars2.LOGGER.info("[check] Native ledge trace tick={} at={} motion={} ground={} sprint={} shift={} input={} feet={} bank={} bankState={} from={} fromState={} preparing={} airborne={} {}",
                        elapsed,bot.position(),bot.getDeltaMovement(),bot.onGround(),bot.isSprinting(),bot.isShiftKeyDown(),bot.getLastClientInput(),
                        base.level.getBlockState(bot.blockPosition().below()),bank,bank==null?null:base.level.getBlockState(bank),
                        from,from==null?null:base.level.getBlockState(from),
                        WoolBucketChecks.read(brain,"preparingJump"),WoolBucketChecks.read(brain,"airborneJump"),brain.diagnostics());
            }
            if(elapsed<1200)return false;
            if(kind.equals("yama"))require(bot.getY()>=center.y-2,"full pit escape returns to the actual island's original height");
            SkyWars2.LOGGER.info("[check] Actual island trap survived {} ticks={} lowestHP={} finalHP={}",kind,elapsed,lowestHealth,bot.getHealth());return true;
        }
        @Override public void close(){
            for(var entity:base.level.getAllEntities())if(!(entity instanceof Player)&&!preexisting.contains(entity.getUUID()))base.owned.add(entity.getUUID());
            super.close();
        }
    }
    static void run(GameController game,String kind){
        require(running==null&&BotTrapChecks.KINDS.contains(kind),"one known original trap");running=new Fixture(game,kind);
        try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}
    }
    static void tick(GameController game){
        if(running==null)return;var test=running;String suite="bot-island-trap-"+test.kind;
        try{if(!test.step())return;test.close();running=null;SkyWars2.LOGGER.info("[check] PASS {} (actual current-mode island, three original chests, native Lucky authority and full-minute same-body survival)",suite);}
        catch(Throwable error){try{test.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL "+suite,error);}
    }
}
