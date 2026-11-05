package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Shared spell policy/control/summon state. Native bodies, teams and effects have canonical owners. */
final class MatchSpellCheckpoint {
    private record Body(Class<?> kind,ServerLevel level) { }
    static final class Links {
        private final MatchDamageSources sources;
        private final Map<MatchDamageSources.Ref,Body> saved=new LinkedHashMap<>();
        private Map<MatchDamageSources.Ref,Entity> current;
        private MatchItemStackLinks stacks;
        private final Set<MatchDamageSources.Image> accepted=new LinkedHashSet<>();
        MatchDamageSources.Image damage(net.minecraft.world.damagesource.DamageSource value){var image=sources.capture(value);for(var next=image;next!=null;next=next.wrapped())accepted.add(next);return image;}
        MatchItemStackLinks.Ref stack(GameController game,net.minecraft.world.item.ItemStack stack){if(stacks==null)stacks=new MatchItemStackLinks(game,this);return stacks.capture(stack);}
        Links(MatchDamageSources sources){this.sources=sources;}
        boolean omitted(Entity entity){return sources.omitted(entity);}
        MatchDamageSources.Ref key(Entity entity){
            if(entity==null)return null;var ref=sources.bodyRef(entity);
            saved.putIfAbsent(ref,new Body(entity.getClass(),(ServerLevel)entity.level()));return ref;
        }
        Set<UUID> resolve(Function<UUID,Entity> lookup){
            var pending=new LinkedHashSet<UUID>();var next=new HashMap<MatchDamageSources.Ref,Entity>();
            saved.forEach((ref,body)->{var entity=MatchDamageSources.resolve(ref,lookup,pending);
                if(entity!=null){if(entity.getClass()!=body.kind||ref.historical()==null&&(!MatchOfflinePlayers.retained(entity)&&(entity.isRemoved()||entity.level()!=body.level)))
                    throw new IllegalStateException("Restore captured spell bodies/dimensions before private links");next.put(ref,entity);}});
            for(var image:accepted){MatchDamageSources.resolve(image.direct(),lookup,pending);MatchDamageSources.resolve(image.cause(),lookup,pending);}
            if(pending.isEmpty())for(var image:accepted){
                if(image.original().getDirectEntity()!=MatchDamageSources.resolve(image.direct(),lookup,pending)
                        ||image.original().getEntity()!=MatchDamageSources.resolve(image.cause(),lookup,pending))
                    throw new IllegalStateException("Restore the shared canonical native damage graph before private accepted sources");
            }
            current=pending.isEmpty()?next:null;if(current!=null&&stacks!=null)stacks.resolve();return Set.copyOf(pending);
        }
        Entity get(MatchDamageSources.Ref ref){return ref==null?null:Objects.requireNonNull(current.get(ref));}
    }
    /** Only immutable values; clone the reviewed identity table so saved traversal order survives growth. */
    static final class IdentityImage<K extends Entity,V> {
        private final IdentityHashMap<K,V> image;
        private final IdentityHashMap<K,MatchDamageSources.Ref> keys=new IdentityHashMap<>();
        @SuppressWarnings("unchecked") IdentityImage(IdentityHashMap<K,V> map,Links links){
            image=(IdentityHashMap<K,V>)map.clone();for(var key:map.keySet())keys.put(key,links.key(key));
        }
        @SuppressWarnings("unchecked") IdentityHashMap<K,V> restore(Links links){
            var result=(IdentityHashMap<K,V>)image.clone();keys.forEach((old,ref)->{var current=(K)links.get(ref);
                if(current!=old){var value=result.remove(old);result.put(current,value);}});return result;
        }
    }
    private final GameController game;
    private final String epoch;
    private final Links links;
    private final LbcSpellSource.Checkpoint policies;
    private final LbcCommunism.Checkpoint controls;
    private final LbcSummons.Checkpoint summons;
    private final LbcDanmaku.Checkpoint danmaku;
    private final LbcPrism.Checkpoint prism;
    private final LbcReactiveDefense.Checkpoint reactiveDefense;
    private final LbcBedwarsAuxiliaries.Checkpoint bedwars;
    private final CloneFinalAbilityStates.Checkpoint logicalActors;
    private boolean installed,published;
    MatchSpellCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;epoch=game.items().epoch();stable(false);links=new Links(sources);
        policies=game.items().spellSources().checkpoint(links);controls=game.items().communism().checkpoint(links);
        summons=game.items().summons().checkpoint(links);danmaku=game.items().danmaku().checkpoint(links);
        bedwars=game.bedwars().auxiliaries().checkpoint(links);
        prism=game.items().prism().checkpoint(links);reactiveDefense=game.items().reactiveTrinkets().defense().checkpoint(links);
        logicalActors=new CloneFinalAbilityStates.Checkpoint(game,links);
    }
    private void stable(boolean frozen){
        if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)
                ||frozen&&!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Restore spell state on the same stable frozen match thread");
        game.items().prism().checkpointBoundary();game.items().spellSources().checkpointBoundary();game.items().summons().checkpointBoundary();
        game.items().cloneSkills().checkpointBoundary();game.items().symbioteSkill().checkpointBoundary();
    }
    /** Resolve and prepare EVERY map/queue first. Missing bodies defer the entire private install. */
    Set<UUID> restoreFrozen(Function<UUID,Entity> lookup){return restoreFrozen(lookup,true);}
    Set<UUID> restoreFrozen(Function<UUID,Entity> lookup,boolean rewind){
        stable(true);var pending=links.resolve(lookup);if(!pending.isEmpty())return pending;
        var policy=policies.prepare(links);var control=controls.prepare(links);var summon=summons.prepare(links);
        var prismPlan=prism.prepare(links);var defensePlan=reactiveDefense.prepare(links);
        var actors=logicalActors.prepare(links);var danmakuState=danmaku.prepare(links);var bedwarsState=bedwars.prepare(links);
        prismPlan.run();defensePlan.run();policy.run();control.run();summon.run();actors.run();danmakuState.run();bedwarsState.run();
        installed=true;published=false;
        // User permits cancellation of short pending operations at rewind. No stale charge may commit.
        game.items().teleportGadgets().cancelChargesForRewind();
        // These stop methods touch only short moving electrical traces and detached star
        // actors (40/100/25 ticks), never the restored Sphere/Drone/Hammer producers.
        if(rewind){game.items().wands().stop();game.items().sauvojen().stars().stop();}
        game.items().cloneSkills().clearPending();
        game.items().psycho().silenceBeamAudio();
        // Frozen canonical body replay suppresses ENTITY_LOAD. Rebuild this derived
        // index only after every shared policy/body link has installed successfully.
        game.items().familyDrops().reindexAfterRestore();
        var nativeBodies=MatchActorCheckpoint.currentBodies(game).values();
        game.items().communism().reindexOwnership(nativeBodies);
        game.items().homingArrows().reindexRestored(nativeBodies);game.items().thrownUtilities().reindexRestored(nativeBodies);game.items().trinketArrows().reindexRestored(nativeBodies);game.items().madokaBow().reindexRestored(nativeBodies);game.items().shulkerBow().reindexRestored(nativeBodies);
        return Set.of();
    }
    /** Client presentation waits for the outer canonical body/authority/status publication phase. */
    void publishFrozen(){
        stable(true);if(published)return;
        if(!installed)throw new IllegalStateException("Install spell actors before presentation publication");
        CloneFinalAbilityStates.publishCheckpoint(game);published=true;
    }
}
