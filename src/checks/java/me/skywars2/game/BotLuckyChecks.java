package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.stats.Stats;
import net.minecraft.core.BlockPos;

/** Real chest preparation followed by an actual native Lucky-block break and ordinary reward. */
final class BotLuckyChecks {
    private static Fixture running;
    private static final class Fixture extends BotMapChecks.Fixture {
        int initialMined;boolean prepared;
        Fixture(GameController game){super(game);suppressLucky=false;}
        int mined(){int count=0;for(var kind:LbcLuckyBlocks.Kind.values())count+=bot.getStats().getValue(Stats.BLOCK_MINED.get(LbcLuckyBlocks.block(kind)));return count;}
        @Override void setup(){
            super.setup();initialMined=mined();
            int bottom=starters.stream().mapToInt(BlockPos::getY).min().orElseThrow();
            var lower=starters.stream().filter(pos->pos.getY()==bottom).toList();var anchor=lower.getFirst();
            BlockPos chosen=null;double best=-1;
            for(var pos:BlockPos.betweenClosed(anchor.offset(-6,0,-6),anchor.offset(6,0,6))){
                if(!base.level.hasChunkAt(pos)||!base.level.getBlockState(pos).isAir()||!base.level.getBlockState(pos.above()).isAir()
                        ||base.level.getBlockState(pos.below()).getCollisionShape(base.level,pos.below()).isEmpty())continue;
                double min=Double.POSITIVE_INFINITY,max=0;
                for(var chest:lower){double d=chest.distSqr(pos);min=Math.min(min,d);max=Math.max(max,d);}
                if(min>=4&&max<=36&&min>best){best=min;chosen=pos.immutable();}
            }
            if(chosen==null)throw new IllegalStateException("A real visible island-floor slot for the Lucky subject");
            base.set(chosen,LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NORMAL).defaultBlockState());
            SkyWars2.LOGGER.info("[check] Native Lucky preparation subject={} ordinary reward table, before chest collection",chosen);
        }
        @Override boolean step(){
            if(mined()>initialMined){
                if(!prepared)throw new IllegalStateException("Lucky block opened before all three real chest menus and native tool preparation");
                SkyWars2.LOGGER.info("[check] Bot native Lucky opening after3chests ticks={} mined={}",game.server().getTickCount()-at,mined()-initialMined);return true;
            }
            if(!prepared)prepared=super.step();
            else{
                base.client.acknowledge();base.audience.acknowledge();
                if(!bot.isAlive()||game.server().getTickCount()-at>=1800)
                    throw new IllegalStateException("Bounded native Lucky opening after preparation: "+game.carpetBots().diagnostics(bot));
            }
            return false;
        }
    }
    static void run(GameController game){if(running!=null)throw new IllegalStateException("One Lucky preparation fixture");var next=new Fixture(game);
        try{next.setup();running=next;}catch(Throwable error){next.close();throw error;}}
    static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS bot-lucky-preparation (real3chest loot and native tool preparation precede an actual ordinary Lucky break)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL bot-lucky-preparation",error);}}
}
