package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;

/** A real source-selected Lucky trap against the same ordinary native starter-looted body. */
final class BotTrapChecks {
    static final List<String> KINDS=List.of("explode","korrosion","creepers","yama","lookupanvil","lookuplava","bob","skeleton","vex","obsidiantrap",
            "badspawner","droppertnt","bullets","skrimmer","slab","clone","evilluckyblock","lookupfireball","lookuppotions","zombietnt","aimbob","sauvojen_tuntija","forest","ultraponos");
    private static Fixture running;
    private static boolean observingDamage;
    private static void observeDamage(){
        if(observingDamage)return;observingDamage=true;
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((body,source,before,amount,blocked)->{
            var test=running;
            if(test!=null&&test.started&&body==test.bot)SkyWars2.LOGGER.info("[check] Bot trap damage {} tick={} type={} amount={} health={} at={}",
                    test.kind,test.game.server().getTickCount()-test.at,source.typeHolder().unwrapKey().orElse(null),amount,body.getHealth(),body.position());
        });
    }
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Bot trap: "+why);}
    static ItemStack sourceToken(GameController game,net.minecraft.server.level.ServerPlayer bot,String kind){
        var params=new LootParams.Builder(bot.level()).withParameter(LootContextParams.ORIGIN,bot.position())
                .withParameter(LootContextParams.THIS_ENTITY,bot).create(LootContextParamSets.CHEST);
        var table=game.server().reloadableRegistries().getLootTable(LbcChaosTraps.TABLE);
        for(long seed=1;seed<=4096;seed++)for(var item:table.getRandomItems(params,seed))
            if(LbcFartus.data(item).getIntOr("drop",0)==1&&LbcFartus.data(item).getIntOr(kind,0)==1)return item.copy();
        throw new IllegalStateException("Actual lbc:traps never produces "+kind);
    }
    private static final class Fixture extends BotStarterChecks.Fixture {
        final String kind;final Set<UUID> beforeTrap=new HashSet<>();
        int at,armedAt=-1;boolean started;Vec3 center;float lowestHealth=20;
        Fixture(GameController game,String kind){super(game);this.kind=kind;}
        @Override void setup(){super.setup();at=game.server().getTickCount();}
        ItemStack sourceToken(){
            return BotTrapChecks.sourceToken(game,bot,kind);
        }
        void begin(){
            if(armedAt<0){center=origin.add(2,0,0);resetStarterBody(center);armedAt=game.server().getTickCount();return;}
            // The controller must observe the real supported ground before the trap removes it.
            if(game.server().getTickCount()-armedAt<2)return;
            require(state()!=null&&((LbcBotSurvival)WoolBucketChecks.read(state(),"survival")).hasSafe,"native pre-trap footing observed");
            for(var actor:base.level.getAllEntities())beforeTrap.add(actor.getUUID());
            var cell=BlockPos.containing(center);var floor=base.level.getBlockState(cell.below());var feet=base.level.getBlockState(cell);
            if(kind.equals("badspawner"))base.set(cell.above(10),Blocks.AIR.defaultBlockState());
            var item=sourceToken();var token=new ItemEntity(base.level,center.x,center.y,center.z,item);
            game.items().markCurrentItemEpoch(token);
            require(base.level.addFreshEntity(token),"native source token publication "+kind);base.owned.add(token.getUUID());
            require(game.items().specialDrops().spawn(token),"actual ordinary WILD dispatcher accepts "+kind);token.discard();
            int actors=0;
            for(var actor:base.level.getAllEntities())if(!beforeTrap.contains(actor.getUUID())&&actor!=token){
                actors++;base.owned.add(actor.getUUID());
                if(actor instanceof Projectile projectile)require(projectile.getOwner()!=bot&&game.items().spellSources().projectileController(actor)!=bot,
                        "a trap projectile is not its victim's physical projectile "+kind);
            }
            boolean geometry=!floor.equals(base.level.getBlockState(cell.below()))||!feet.equals(base.level.getBlockState(cell));
            if(kind.equals("explode")||kind.equals("creepers"))require(actors>0,"actual dangerous source bodies appear "+kind);
            if(kind.equals("yama"))require(actors>0||geometry,"actual trap geometry or source actor appears "+kind);
            if(kind.equals("badspawner"))require(hasSpawner(),"actual potion spawner appears ten blocks above the native token");
            started=true;at=game.server().getTickCount();SkyWars2.LOGGER.info("[check] Native bot trap started {} actors={} geometry={} token={}",kind,actors,geometry,item);
        }
        boolean defused(){
            if(bot.position().subtract(center).horizontalDistanceSqr()<16*16||!bot.onGround()||bot.isInWater()||bot.isOnFire())return false;
            for(var actor:base.level.getAllEntities())if(!beforeTrap.contains(actor.getUUID())&&actor.isAlive()){
                if(actor instanceof PrimedTnt||actor instanceof Creeper||actor instanceof Mob mob&&mob.getTarget()==bot)return false;
            }
            return switch(kind){
                case "explode","creepers"->true;
                case "yama"->bot.getY()>=center.y-1;
                // A standing spawner is still a hazard even if the bot temporarily walks away.
                case "badspawner"->!hasSpawner();
                default->false;
            };
        }
        boolean hasSpawner(){return base.level.getBlockState(BlockPos.containing(center).above(10)).is(Blocks.SPAWNER);}
        @Override boolean step(){
            base.client.acknowledge();base.audience.acknowledge();int elapsed=game.server().getTickCount()-at;
            require(bot.isAlive()&&game.server().getPlayerList().getPlayer(bot.getUUID())==bot,
                    "same native20HP body survives "+kind+" at "+elapsed+"ticks "+game.carpetBots().diagnostics(bot));
            if(!started){require(elapsed<1200,"bounded real starter-loot preparation "+kind);if(!prepareKit())return false;begin();return false;}
            lowestHealth=Math.min(lowestHealth,bot.getHealth());
            if(elapsed%100==0)SkyWars2.LOGGER.info("[check] Bot trap {} tick={} at={} health={} air={} {}",kind,elapsed,bot.position(),bot.getHealth(),bot.getAirSupply(),game.carpetBots().diagnostics(bot));
            if(elapsed>=200&&defused()||elapsed>=1200){
                if(kind.equals("yama"))require(bot.getY()>=center.y-2,"pit survival includes returning to the original map height");
                SkyWars2.LOGGER.info("[check] Bot trap survived {} ticks={} lowestHP={} finalHP={} defused={}",kind,elapsed,lowestHealth,bot.getHealth(),defused());return true;
            }
            return false;
        }
    }
    static void run(GameController game,String kind){
        require(running==null&&KINDS.contains(kind),"one known isolated trap");observeDamage();running=new Fixture(game,kind);
        try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}
    }
    static void tick(GameController game){
        if(running==null)return;String kind=running.kind;
        try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS bot-trap-{} (real source table/ordinary unowned trap, native starter loot and same body survival or complete escape)",kind);}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL bot-trap-"+kind,error);}
    }
}
