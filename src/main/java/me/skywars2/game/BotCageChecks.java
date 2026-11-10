package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Five real cage producers against ordinary Carpet bodies using only the generated starter inventory. */
final class BotCageChecks {
    private static Fixture running;
    private static boolean observingBreaks;
    private static void observeBreaks(){
        if(observingBreaks)return;
        observingBreaks=true;
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((level,player,pos,state,entity)->{
            var test=running;
            if(test!=null&&player==test.bot&&test.trial>=0
                    &&state.is(test.kind()==LbcCageDropItem.Kind.OBSIDIAN?Blocks.GLASS:Blocks.IRON_BARS))test.openings++;
        });
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((body,source,before,amount,blocked)->{
            var test=running;if(test!=null&&body==test.bot&&test.trial>=0)
                SkyWars2.LOGGER.info("[check] Cage damage kind={} age={} type={} amount={} health={} direct={}",test.kind(),test.game.server().getTickCount()-test.at,source.typeHolder().unwrapKey().orElse(null),amount,body.getHealth(),source.getDirectEntity());
        });
    }
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Bot cages: "+why);}
    private static final class Fixture extends BotStarterChecks.Fixture {
        final List<String> results=new ArrayList<>();final int first,last;final String suite;int trial=-1,at,openings;BlockPos center;Vec3 previous;boolean roofSeen,directionSet;
        Fixture(GameController game,int first,int last,String suite){super(game);this.first=first;this.last=last;this.suite=suite;}
        @Override void setup(){
            super.setup();at=game.server().getTickCount();
        }
        LbcCageDropItem.Kind kind(){return LbcCageDropItem.Kind.values()[trial/4];}
        void begin(){
            game.items().cageDrops().stop();
            center=BlockPos.containing(origin.add(2,0,0));
            // Water from one independent case can flow seven blocks away. Clear its
            // whole possible source area before starting another selected trap.
            for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++)for(int y=-1;y<=5;y++)base.set(center.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
            resetStarterBody(Vec3.atBottomCenterOf(center));
            var token=new ItemEntity(base.level,bot.getX(),bot.getY(),bot.getZ(),new ItemStack(kind().item()));
            require(base.level.addFreshEntity(token),"real ordinary Lucky source token "+kind());base.owned.add(token.getUUID());
            require(game.items().specialDrops().spawn(token),"actual WILD loot dispatcher admits cage "+kind());
            for(var actor:base.level.getAllEntities())if(LbcCageDrops.managed(actor)&&actor.position().distanceToSqr(bot.position())<2100d*2100){
                require(game.items().spellSources().lootProjectile(actor)&&game.items().spellSources().projectileController(actor)==null,
                        "ordinary cage payload has no physical player controller "+kind());
            }
            token.discard();
            openings=0;roofSeen=false;directionSet=false;previous=bot.position();at=game.server().getTickCount();
        }
        @Override boolean step(){
            base.client.acknowledge();base.audience.acknowledge();int now=game.server().getTickCount();
            require(bot.isAlive()&&game.server().getPlayerList().getPlayer(bot.getUUID())==bot,"same ordinary20HP native body survives "+(trial<0?"starter loot":kind())+" "+game.carpetBots().diagnostics(bot));
            require(now-at<1200,"escape within one minute "+(trial<0?"starter loot":kind())+" "+game.carpetBots().diagnostics(bot));
            if(trial<0){
                if(!prepareKit())return false;trial=first;begin();return false;
            }
            require(bot.position().distanceToSqr(previous)<4,"escape uses real movement without teleporting "+kind());previous=bot.position();
            if(!directionSet&&state()!=null){
                var survival=(LbcBotSurvival)WoolBucketChecks.read(state(),"survival");
                if(survival.cage!=null){survival.exit=List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST).get(trial%4);directionSet=true;}
            }
            if(kind()==LbcCageDropItem.Kind.POTIONS&&now-at<=200&&(now-at)%10==0)
                SkyWars2.LOGGER.info("[check] Potion escape age={} at={} velocity={} health={} pose={} water={} {}",now-at,bot.position(),bot.getDeltaMovement(),bot.getHealth(),bot.getPose(),bot.isInWater(),game.carpetBots().diagnostics(bot));
            if(game.carpetBots().observation(bot).placements()>0)for(int height=2;height<=3;height++)
                if(!base.level.getBlockState(center.above(height)).getCollisionShape(base.level,center.above(height)).isEmpty())roofSeen=true;
            if(bot.position().subtract(Vec3.atBottomCenterOf(center)).horizontalDistanceSqr()<=16*16||!bot.onGround()||bot.isInWater()||bot.isOnFire())return false;
            // Vanilla does not award BLOCK_MINED for bars broken without a correct drop tool.
            // Observe the actual accepted break instead of mistaking a missing statistic for no mining.
            require(openings>0,"native survival mining opens the cage "+kind());
            if(kind()!=LbcCageDropItem.Kind.OBSIDIAN)require(game.carpetBots().observation(bot).placements()>0&&roofSeen,"actual protective roof before the payload, which may consume it "+kind());
            require(directionSet,"controlled real exit direction is exercised");
            results.add(kind()+"/"+List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST).get(trial%4)+":"+(now-at)+"ticks/"+bot.getHealth()+"HP");SkyWars2.LOGGER.info("[check] Cage escaped {}",results.getLast());
            if(++trial<=last){begin();return false;}return true;
        }
        @Override public void close(){
            game.items().cageDrops().stop();
            super.close();
        }
    }
    static void run(GameController game){start(game,0,LbcCageDropItem.Kind.values().length*4-1,"bot-cages");}
    static void fireball(GameController game){int index=LbcCageDropItem.Kind.FIREBALL.ordinal()*4;start(game,index,index+3,"bot-fireball-cage");}
    static void potions(GameController game){int index=LbcCageDropItem.Kind.POTIONS.ordinal()*4;start(game,index,index+3,"bot-potion-cage");}
    private static void start(GameController game,int first,int last,String suite){require(running==null,"exclusive fixture");observeBreaks();running=new Fixture(game,first,last,suite);try{running.setup();}catch(Throwable e){try{running.close();}catch(Throwable c){e.addSuppressed(c);}running=null;throw e;}}
    static void tick(GameController game){if(running==null)return;String suite=running.suite;try{if(!running.step())return;var results=List.copyOf(running.results);running.close();running=null;SkyWars2.LOGGER.info("[check] PASS {} (actual20HP Carpet/native starter tables/finite inventory/native roof and mining/water exit): {}",suite,results);}catch(Throwable e){try{running.close();}catch(Throwable c){e.addSuppressed(c);}running=null;SkyWars2.LOGGER.error("[check] FAIL "+suite,e);}}
}
