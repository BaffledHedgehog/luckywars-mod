package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import me.skywars2.mixin.AcademyCheckpointAccess;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

/** Completed Academy building ownership; templates, native blocks and blast cohorts keep their own owners. */
public final class MatchAcademyCheckpoint {
    private static final Map<LbcAcademy,Integer> ACTIVE=new IdentityHashMap<>();
    private static final Set<LbcAcademy> FROZEN=Collections.newSetFromMap(new IdentityHashMap<>());
    public static void enter(LbcAcademy academy){ACTIVE.merge(academy,1,Integer::sum);}
    public static void exit(LbcAcademy academy){int left=ACTIVE.getOrDefault(academy,0)-1;if(left==0)ACTIVE.remove(academy);else if(left>0)ACTIVE.put(academy,left);else throw new IllegalStateException("Unbalanced Academy source callback");}
    public static boolean frozen(LbcAcademy academy){return FROZEN.contains(academy);}
    static boolean captureReady(GameController game){
        var academy=game.items().academy();var access=(AcademyCheckpointAccess)(Object)academy;
        // Source placing is assigned only inside tick's try/finally. No active tick establishes placing == null.
        return !ACTIVE.containsKey(academy)&&!FROZEN.contains(academy)&&academy.pendingCount()==0&&access.skywars2$pendingWhite().isEmpty();
    }
    private record Part(Object original,boolean spent,int row,List<MatchDamageSources.Ref> spawned,Set<GlobalPos> panels){}
    private final GameController game;
    private final String epoch;
    private final MatchSpellCheckpoint.Links links;
    private final List<MatchDamageSources.Ref> drops,entities;
    private final Map<UUID,Long> interactions;
    private final Map<UUID,Object> singleUse;
    private final Map<GlobalPos,Object> panels;
    private final List<Part> parts=new ArrayList<>();
    private final boolean wasStopped;
    private boolean prepared,installed;

    MatchAcademyCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;epoch=game.items().epoch();stable(false);
        if(!captureReady(game))throw new IllegalStateException("Complete Academy placement and deferred panels before capture");
        links=new MatchSpellCheckpoint.Links(sources);var access=(AcademyCheckpointAccess)(Object)game.items().academy();
        drops=access.skywars2$drops().stream().map(links::key).toList();entities=access.skywars2$entities().stream().map(links::key).toList();
        interactions=new HashMap<>(access.skywars2$interactions());singleUse=new HashMap<>(access.skywars2$singleUse());panels=new HashMap<>(access.skywars2$panelOwners());wasStopped=access.skywars2$wasStopped();
        var unique=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());unique.addAll(singleUse.values());unique.addAll(panels.values());
        for(var original:unique){var part=(AcademyCheckpointAccess.Placement)original;MatchAuthorityCheckpoint.world(game,part.skywars2$level());
            parts.add(new Part(original,part.skywars2$spent(),part.skywars2$row(),part.skywars2$spawned().stream().map(links::key).toList(),Set.copyOf(part.skywars2$panels())));}
    }
    private void stable(boolean frozen){
        if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)
                ||frozen&&!game.server().tickRateManager().isFrozen()||ACTIVE.containsKey(game.items().academy()))
            throw new IllegalStateException("Restore Academy metadata on the stable native match thread outside source callbacks");
    }
    void prepareFrozen(){
        stable(true);var academy=game.items().academy();FROZEN.add(academy);academy.stop();prepared=true;installed=false;
    }
    Set<UUID> restoreFrozen(Function<UUID,Entity> lookup){
        stable(true);if(!prepared)prepareFrozen();var missing=links.resolve(lookup);if(!missing.isEmpty())return missing;
        var nextDrops=new ArrayList<ItemEntity>();for(var body:drops)nextDrops.add((ItemEntity)links.get(body));
        var nextEntities=new ArrayList<Entity>();for(var body:entities)nextEntities.add(links.get(body));
        var nextParts=new IdentityHashMap<Object,List<Entity>>();for(var saved:parts){var part=(AcademyCheckpointAccess.Placement)saved.original;MatchAuthorityCheckpoint.world(game,part.skywars2$level());
            var actors=new ArrayList<Entity>();for(var body:saved.spawned)actors.add(links.get(body));nextParts.put(saved.original,actors);}
        // All canonical links and source worlds validate before the first private write.
        var access=(AcademyCheckpointAccess)(Object)game.items().academy();
        for(var saved:parts){var part=(AcademyCheckpointAccess.Placement)saved.original;part.skywars2$spent(saved.spent);part.skywars2$row(saved.row);
            part.skywars2$spawned().clear();part.skywars2$spawned().addAll(nextParts.get(saved.original));part.skywars2$panels().clear();part.skywars2$panels().addAll(saved.panels);}
        access.skywars2$drops().clear();access.skywars2$drops().addAll(nextDrops);access.skywars2$entities().clear();access.skywars2$entities().addAll(nextEntities);
        access.skywars2$interactions().clear();access.skywars2$interactions().putAll(interactions);access.skywars2$singleUse().clear();access.skywars2$singleUse().putAll(singleUse);
        access.skywars2$panelOwners().clear();access.skywars2$panelOwners().putAll(panels);access.skywars2$pendingWhite().clear();access.skywars2$wasStopped(wasStopped);installed=true;return Set.of();
    }
    void publishFrozen(){stable(true);if(!installed)throw new IllegalStateException("Install Academy metadata before publication");FROZEN.remove(game.items().academy());prepared=false;}
    void cancelFrozen(){stable(true);FROZEN.remove(game.items().academy());prepared=false;installed=false;}
}
