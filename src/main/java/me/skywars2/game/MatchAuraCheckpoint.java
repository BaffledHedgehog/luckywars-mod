package me.skywars2.game;

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

/** Essential sustained auras. All accepted terrain fills finish before capture; native values stay canonical. */
final class MatchAuraCheckpoint {
    private final GameController game;
    private final String epoch;
    private final MatchSpellCheckpoint.Links links;
    private final LbcBlaze.Checkpoint blaze;
    private final LbcKiuas.Checkpoint kiu;
    private final LbcTanner.Checkpoint tanner;
    private boolean prepared,installed,published;
    private Set<UUID> pendingOwners=Set.of();

    /** Real current bodies only. A player still in its login world waits for its canonical player image. */
    static ServerPlayer current(GameController game,UUID id,ServerLevel level){
        var player=game.server().getPlayerList().getPlayer(id);
        return player!=null&&!player.isRemoved()&&player.level()==level?player:null;
    }

    MatchAuraCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;epoch=game.items().epoch();stable(false);
        if(game.items().blaze().terrainCount()!=0||game.items().kiuas().queuedBlockCount()!=0||game.items().tanner().queuedBlockCount()!=0)
            throw new IllegalStateException("Finish original Blaze/Kiuas/Sun/Tanner fills before taking aura essentials");
        links=new MatchSpellCheckpoint.Links(sources);
        blaze=game.items().blaze().checkpoint(links);kiu=game.items().kiuas().checkpoint(links);tanner=game.items().tanner().checkpoint(links);prepared=true;installed=true;published=true;
    }
    private MatchAuraCheckpoint(MatchAuraCheckpoint current,MatchAuraCheckpoint previous){
        game=current.game;epoch=current.epoch;links=current.links;stable(false);
        blaze=current.blaze.withPending(previous.blaze);kiu=current.kiu.withPending(previous.kiu);tanner=current.tanner.withPending(previous.tanner);
        pendingOwners=previous.pendingOwners;prepared=true;installed=true;
    }
    MatchAuraCheckpoint withPending(MatchAuraCheckpoint previous){
        stable(false);return previous.hasPending()?new MatchAuraCheckpoint(this,previous):this;
    }
    private void stable(boolean frozen){
        if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)
                ||frozen&&!game.server().tickRateManager().isFrozen())
            throw new IllegalStateException("Restore auras on the same stable frozen match thread");
        game.items().blaze().checkpointBoundary();game.items().kiuas().checkpointBoundary();game.items().tanner().checkpointBoundary();
        game.items().spellSources().checkpointBoundary();
    }
    /** Rollback only. No source curse, native blaze, charge counter or held aura is stopped. */
    void prepareFrozen(){
        stable(true);blaze.resetReplay();kiu.resetReplay();tanner.resetReplay();
        game.items().blaze().prepareAuraFrozen();game.items().kiuas().prepareAuraFrozen();game.items().tanner().prepareAuraFrozen();
        prepared=true;installed=false;published=false;pendingOwners=Set.of();
    }
    Set<UUID> restoreFrozen(Function<UUID,Entity> lookup){
        stable(true);if(!prepared)prepareFrozen();if(!installed){var pending=links.resolve(lookup);if(!pending.isEmpty())return pending;}
        var fire=blaze.prepare(links);var sun=kiu.prepare(links);var earth=tanner.prepare(links);
        fire.run();sun.run();earth.run();installed=true;published=false;
        var owners=new LinkedHashSet<UUID>();owners.addAll(blaze.pendingPlayers());owners.addAll(kiu.pendingPlayers());owners.addAll(tanner.pendingPlayers());
        pendingOwners=Set.copyOf(owners);return pendingOwners;
    }
    /** Rebuild completed spatial coverage without enqueuing or executing any fill or activation. */
    Set<UUID> publishFrozen(){
        stable(true);if(published)return pendingOwners;if(!installed)throw new IllegalStateException("Install aura essentials before spatial publication");
        game.items().blaze().publishAuraFrozen();game.items().kiuas().publishAuraFrozen();game.items().tanner().publishAuraFrozen();
        prepared=!pendingOwners.isEmpty();published=true;return pendingOwners;
    }
    boolean hasPending(){return !pendingOwners.isEmpty();}
    void cancelFrozen(){
        if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())
            throw new IllegalStateException("Cancel aura replay on the frozen native thread");
        game.items().blaze().publishAuraFrozen();game.items().kiuas().publishAuraFrozen();game.items().tanner().publishAuraFrozen();
        prepared=false;installed=false;published=false;pendingOwners=Set.of();
    }
}
