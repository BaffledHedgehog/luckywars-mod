package me.skywars2.game;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;

/** One match save point. Capture runs between ticks; native bodies and private owners share one source graph. */
final class MatchRollbackSnapshot {
    final String epoch;
    final Set<UUID> omitted;
    final MatchTerrainCheckpoint terrain;
    final MatchCoreCheckpoint core;
    final MatchWorldCheckpoint world;
    final LbcBedwarsAuxiliaries.StorageImage bedwarsStorage;
    final MatchWorldRandomCheckpoint random;
    final MatchActorCheckpoint actors;
    final Map<UUID,MatchPlayerCheckpoint> players=new LinkedHashMap<>();
    final LbcTimeStop.Checkpoint timeStop;
    final LbcIndirectKills.Checkpoint attribution;
    final MatchSpellCheckpoint spells;
    final MatchAuthorityCheckpoint authorities;
    final MatchQueuedWorkCheckpoint queued;
    final MatchBlastCheckpoint blasts;
    final LbcFpvDrone.Checkpoint fpv;
    final LbcDuplicating.Checkpoint duplicating;
    final MatchSpellCheckpoint.Links duplicateLinks;
    final MatchBlackHoleCheckpoint holes;
    MatchAuraCheckpoint auras;
    MatchFieldCheckpoint fields;
    final MatchEventActorCheckpoint events;
    final MatchCombatStatusCheckpoint statuses;
    final MatchAcademyCheckpoint academy;
    final MatchChefCheckpoint chefs;
    final MatchHedgehogCheckpoint hedgehogs;
    final MatchEncounterCheckpoint encounters;
    MatchAimCheckpoint aim;
    final MatchResidentItemCheckpoint residents;
    MatchPolymorphCheckpoint polymorph;
    final long captureNanos;
    final VanityCheckpointSchedule.State vanitySchedule;

    MatchRollbackSnapshot(GameController game){
        long start=System.nanoTime();epoch=game.items().epoch();vanitySchedule=game.vanity().scheduleAfterCapture();
        // Check pending continuations BEFORE copying the whole terrain. Unsupported short work
        // finishes naturally and the coordinator retries; it is never cut in half at acquisition.
        var skipped=new java.util.LinkedHashSet<>(MatchTransientSpells.captureOmitted(game));
        skipped.addAll(game.items().polymorph().transientCheckpointActors());
        skipped.addAll(game.items().stand().transientCheckpointActors());
        skipped.retainAll(MatchActorCheckpoint.currentBodies(game).keySet());omitted=Set.copyOf(skipped);
        actors=MatchActorCheckpoint.captureRegistered(game,omitted);var sources=actors.damageSources();
        timeStop=game.items().timeStop().checkpoint(sources);
        attribution=game.items().indirectKills().checkpoint(sources);
        spells=new MatchSpellCheckpoint(game,sources);authorities=new MatchAuthorityCheckpoint(game,sources);
        queued=new MatchQueuedWorkCheckpoint(game,sources);blasts=new MatchBlastCheckpoint(game,sources);
        fpv=game.items().fpvDrone().checkpoint(sources);
        duplicateLinks=new MatchSpellCheckpoint.Links(sources);
        duplicating=game.items().duplicating().checkpoint(duplicateLinks);
        holes=new MatchBlackHoleCheckpoint(game,sources);polymorph=game.items().polymorph().checkpoint(sources);
        auras=new MatchAuraCheckpoint(game,sources);fields=new MatchFieldCheckpoint(game,sources);events=new MatchEventActorCheckpoint(game,sources);
        statuses=new MatchCombatStatusCheckpoint(game,sources);academy=new MatchAcademyCheckpoint(game,sources);
        chefs=new MatchChefCheckpoint(game,sources);hedgehogs=new MatchHedgehogCheckpoint(game,sources);
        encounters=new MatchEncounterCheckpoint(game,sources);aim=game.items().rifleZoom().checkpoint(sources);
        residents=new MatchResidentItemCheckpoint(game,sources);
        bedwarsStorage=game.bedwars().auxiliaries().storageImage();
        core=new MatchCoreCheckpoint(game);world=new MatchWorldCheckpoint(game);
        for(var player:game.server().getPlayerList().getPlayers())players.put(player.getUUID(),new MatchPlayerCheckpoint(game,player));
        terrain=MatchTerrainCheckpoint.captureLoaded(game);random=new MatchWorldRandomCheckpoint(game);
        captureNanos=System.nanoTime()-start;
        SkyWars2.LOGGER.info("[vanity] Captured {} chunks, {} actors and {} players in {} ms",
                terrain.chunks(),actors.size(),players.size(),captureNanos/1_000_000.0);
    }

    /** Pending offline rows belong to this reusable point, including rows inherited through earlier replacements. */
    MatchRollbackSnapshot(GameController game,MatchRollbackSnapshot previous){
        this(game);if(previous==null)return;
        polymorph=polymorph.withPending(previous.polymorph);fields=fields.withPending(previous.fields);
        auras=auras.withPending(previous.auras);aim=aim.withPending(previous.aim);
    }

    void prepareFrozen(GameController game){
        // Detach discarded future hits silently before any teardown can trigger an end-of-stop replay.
        timeStop.prepareFrozen();game.items().stonekivi().cancelQueuedForRollback();
        fpv.prepareFrozen();queued.prepareFrozen();blasts.prepareFrozen();
        holes.prepareFrozen();polymorph.prepareFrozen();auras.prepareFrozen();fields.prepareFrozen();
        statuses.prepareFrozen();academy.prepareFrozen();chefs.prepareFrozen();hedgehogs.prepareFrozen();encounters.prepareFrozen();aim.prepareFrozen();residents.prepareFrozen();
        events.cancelFutureTerrainForRollback();events.prepareFrozen();
        MatchTransientSpells.cancelFuture(game);game.items().sauvojen().meditation().stop();
        game.items().portalDrops().stop();SignedEnvironmentalEffects.discardQueuedForRollback();
        game.items().shooters().cancelQueuedTerrainForRollback();actors.prepareViewsFrozen();
    }

    Set<UUID> restoreDuplicatingFrozen(){
        var absent=duplicateLinks.resolve(actors::resolve);if(!absent.isEmpty())return absent;
        duplicating.prepare(duplicateLinks).run();return Set.of();
    }
    void restoreOnlinePlayers(GameController game,Map<UUID,MatchPlayerCheckpoint> pending){
        for(var image:players.values()){
            ServerPlayer player=game.server().getPlayerList().getPlayer(image.id());
            if(player==null)pending.put(image.id(),image);else image.restoreFrozen();
        }
    }
}
