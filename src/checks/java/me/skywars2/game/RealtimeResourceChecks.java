package me.skywars2.game;

import me.skywars2.SkyWars2;
import me.skywars2.game.effects.magic.LbcMana;
import me.skywars2.game.time.RealtimeTickClock;

/** Native resource pipeline with an injected monotonic clock, without sleeping the server thread. */
public final class RealtimeResourceChecks {
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Realtime resources: "+why);}
    public static void run(GameController game){
        long[] now={1_000_000_000L};var clock=new RealtimeTickClock(()->now[0]);
        now[0]+=200_000_000L;require(clock.advance(false)==4&&clock.ticks()==4,"one5TPS frame accounts four real twentieths");
        now[0]+=2_000_000_000L;require(clock.advance(true)==0&&clock.ticks()==4,"intentional pause never accrues elapsed work");
        now[0]+=50_000_000L;require(clock.advance(false)==1,"resume has no paused catchup");
        clock.restore(123);now[0]+=25_000_000L;require(clock.advance(false)==0,"fractional frame retained");
        now[0]+=25_000_000L;require(clock.advance(false)==1&&clock.ticks()==124,"fractions accumulate without drift");
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(player!=null,"native active participant");var saved=new RegenerationBeltChecks.Snapshot(game,player);
        try{
            player.getInventory().clearContent();player.removeAllEffects();
            var mana=new LbcMana(game,()->now[0]);mana.inventoryChanged(player);
            mana.maximum(player,5000);mana.set(player,0);mana.baseRegen(player,10);
            mana.noMagic(player,0);mana.reverse(player,0);mana.boost(player,0);
            mana.tickPlayer(player);require(mana.get(player)==10,"first active regeneration quantum");
            mana.tickPlayer(player);require(mana.get(player)==10,"reentrant calls do not create free time");
            now[0]+=200_000_000L;mana.tickPlayer(player);require(mana.get(player)==50,"actual native mana5TPS frame restores four quanta");
            now[0]+=800_000_000L;mana.tickPlayer(player);require(mana.get(player)==210,"one wall second restores20quanta independent of sample count");
            mana.noMagic(player,10);now[0]+=1_000_000_000L;mana.tickPlayer(player);require(mana.get(player)==210,"suppression does not bank regeneration");
            mana.noMagic(player,0);now[0]+=50_000_000L;mana.tickPlayer(player);require(mana.get(player)==220,"suppression resume only restores current quantum");
            mana.set(player,4995);now[0]+=200_000_000L;mana.tickPlayer(player);require(mana.get(player)==5000,"catchup preserves maximum cap");
        }finally{saved.restore(game);saved.restoreAttributes();saved.restoreUse();game.items().mana().inventoryChanged(player);}
        SkyWars2.LOGGER.info("[check] PASS realtime-resources (native mana with monotonic5TPS catchup, no double sampling, suppression, cap and checkpoint clock)");
    }
}
