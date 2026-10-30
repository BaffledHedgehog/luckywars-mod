package me.skywars2.game;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Private authority state; shared policy, native bodies/effects/teams and terrain keep their own owners. */
final class MatchAuthorityCheckpoint {
    record Clock(int frame,int last,int logical) {
        Clock(GameController game,int last,int offset){this(game.server().getTickCount(),last,game.server().getTickCount()+offset);}
        int time(int value,GameController game){return value+game.server().getTickCount()-frame;}
        int last(GameController game){return last==Integer.MIN_VALUE?last:time(last,game);}
        int offset(GameController game){return logical-game.server().getTickCount();}
    }
    static void world(GameController game,ServerLevel level){if(game.server().getLevel(level.dimension())!=level)throw new IllegalStateException("Reconcile authority worlds before private replay");}
    private final GameController game;
    private final String epoch;
    private final MatchSpellCheckpoint.Links links;
    private final LbcWitchPassives.Checkpoint passives;
    private final LbcWitchWrath.Checkpoint wrath;
    private final LbcWitchIndolence.Checkpoint hands;
    private final LbcIndolenceFingers.Checkpoint fingers;
    private final LbcWitchLust.Checkpoint lust;
    MatchAuthorityCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;epoch=game.items().epoch();stable(false);links=new MatchSpellCheckpoint.Links(sources);
        passives=game.items().witchPassives().checkpoint(links);wrath=game.items().witchWrath().checkpoint();
        hands=game.items().witchIndolence().checkpoint(links);fingers=game.items().indolenceFingers().checkpoint(links);
        lust=game.items().witchLust().checkpoint(links);
    }
    private void stable(boolean frozen){
        if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)
                ||frozen&&!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Restore authorities on the same stable frozen match thread");
        game.items().spellSources().checkpointBoundary();game.items().witchPassives().checkpointBoundary();game.items().witchWrath().checkpointBoundary();
        game.items().witchIndolence().checkpointBoundary();game.items().indolenceFingers().checkpointBoundary();game.items().witchLust().checkpointBoundary();
    }
    Set<UUID> restoreFrozen(Function<UUID,Entity> lookup){
        stable(true);var pending=links.resolve(lookup);if(!pending.isEmpty())return pending;
        var p=passives.prepare(links);var w=wrath.prepare();var h=hands.prepare(links);var f=fingers.prepare(links);var l=lust.prepare(links);
        p.run();w.run();h.run();f.run();l.run();return Set.of();
    }
    /** After canonical rider/camera links. Refresh current clients without replaying grants or mounting. */
    void publishFrozen(){stable(true);for(var player:game.server().getPlayerList().getPlayers())FullInvisibility.effectsChanged(player);}
}
