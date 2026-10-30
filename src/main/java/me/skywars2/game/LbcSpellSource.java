package me.skywars2.game;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

/** Explicit emitter policy and causal author for shared asynchronous spell engines. */
public final class LbcSpellSource {
    public record Source(UUID emitter,UUID author,String epoch,LbcIndirectKills.Owner cause,Set<String> excluded,boolean persistent){
        public Source{excluded=Set.copyOf(excluded);}
        public Source(UUID emitter,UUID author,String epoch,LbcIndirectKills.Owner cause,Set<String> excluded){this(emitter,author,epoch,cause,excluded,false);}
    }
    private final GameController game;
    private IdentityHashMap<Entity,Source> sources=new IdentityHashMap<>();
    private final Map<UUID,Source> unloaded=new HashMap<>();
    private int scopes;
    private Source current;
    private ServerLevel currentLevel;
    public LbcSpellSource(GameController game){this.game=game;}
    public Source hostile(LivingEntity emitter,ServerPlayer author,Set<String> excluded){
        return new Source(emitter.getUUID(),author.getUUID(),game.items().epoch(),game.items().indirectKills().wildCause(author),excluded);
    }
    /** Nesting is restored even when a native payload synchronously launches another spell. */
    public final class Scope implements AutoCloseable {
        private final Source previous;private final ServerLevel previousLevel;private boolean closed;
        private Scope(Source source,ServerLevel level){previous=current;previousLevel=currentLevel;scopes++;
            current=source!=null&&game.items().currentEpoch(source.epoch())?source:null;currentLevel=current==null?null:level;}
        @Override public void close(){if(!closed){current=previous;currentLevel=previousLevel;closed=true;scopes--;}}
    }
    public Scope scope(Source source){return new Scope(source,currentLevel);}
    public Scope scope(Source source,ServerLevel level){return new Scope(source,level);}
    public Scope scope(Entity entity){return new Scope(source(entity),entity!=null&&entity.level() instanceof ServerLevel level?level:null);}
    public ServerLevel level(ServerPlayer fallback){return currentLevel!=null?currentLevel:fallback==null?null:fallback.level();}
    public ServerPlayer author(Entity entity){var policy=source(entity);return policy!=null?game.server().getPlayerList().getPlayer(policy.author())
            :entity instanceof Projectile shot&&shot.getOwner() instanceof ServerPlayer player?player:entity instanceof ServerPlayer player?player:null;}
    public boolean persistent(){return current!=null&&current.persistent()&&game.items().currentEpoch(current.epoch());}
    public Source current(){return current;}
    public void capture(Entity entity){
        // Reflection stamps the epoch inside the old flight scope; never rebind that old authority.
        if(current!=null&&!entity.entityTags().contains("skywars2.electro.reflected")&&!entity.entityTags().contains(LbcGreed.REFLECTED)
                &&game.items().currentEpoch(current.epoch())){sources.put(entity,current);entity.entityTags().add(current.epoch());}
    }
    public void bind(Entity entity,Source source){if(source!=null)sources.put(entity,source);}
    /** New attacks belong to the new controller; already inherited child policies remain immutable. */
    void controllerChanged(Entity emitter,ServerPlayer controller){
        var previous=source(emitter);if(previous==null||previous.author().equals(controller.getUUID()))return;
        var next=game.items().indirectKills().wildCause(controller);var old=previous.cause();
        var cause=old==null?next:new LbcIndirectKills.Owner(next.player(),next.order(),old.friendlyFire(),old.projectile());
        sources.put(emitter,new Source(emitter.getUUID(),controller.getUUID(),previous.epoch(),cause,previous.excluded(),previous.persistent()));
    }
    public Source source(Entity entity){
        var source=sources.get(entity);if(source==null||!game.items().currentEpoch(source.epoch()))return null;
        if(entity.entityTags().contains(LbcGreed.REFLECTED)||entity.entityTags().contains("skywars2.electro.reflected")){reflected(entity);return null;}
        // Native reflection transfers the projectile to its new author and restores ordinary targeting.
        if(entity instanceof Projectile projectile&&projectile.getOwner() instanceof ServerPlayer owner&&!owner.getUUID().equals(source.author()))return null;
        return source;
    }
    public void reflected(Entity entity){sources.remove(entity);unloaded.remove(entity.getUUID());}
    public boolean wild(Entity entity){return source(entity)!=null;}
    public ServerPlayer owner(Entity entity){var source=source(entity);return source==null?null:game.server().getPlayerList().getPlayer(source.author());}
    /** Null means the existing engine owns policy; false remains a protected role or source exclusion. */
    public Boolean target(ServerPlayer author,Entity target){
        if(current==null||author!=null&&!author.getUUID().equals(current.author()))return null;
        return target(current,target);
    }
    /** Accepted emitter policy does not depend on the causal author remaining online. */
    public boolean target(Source source,Entity target){
        return source!=null&&game.items().currentEpoch(source.epoch())&&target!=null
                &&!target.getUUID().equals(source.emitter())&&!LbcGreed.active(target)
                &&game.items().wands().validRoleTarget(target,false)
                &&source.excluded().stream().noneMatch(target.entityTags()::contains);
    }
    public boolean canAffect(Entity emitter,Entity target){
        var source=source(emitter);
        // Native arrows/bullets retain muzzle immunity in Projectile.canHitEntity, then may return.
        // Spell rays still exclude their emitter; captured kill credit must not change physics.
        if(source!=null&&LbcWands.physicalProjectile(emitter)&&!ProjectileLifetimes.protectedWandProjectile(emitter))return game.items().currentEpoch(source.epoch())
                &&target!=null&&!LbcGreed.active(target)&&game.items().wands().validRoleTarget(target,false)
                &&source.excluded().stream().noneMatch(target.entityTags()::contains);
        return target(source,target);
    }
    public boolean persistent(Entity emitter){var source=source(emitter);return source!=null&&source.persistent();}
    public DamageSource damage(DamageSource damage){
        var authority=source(damage.getDirectEntity());
        if(authority==null)authority=current;
        if(authority==null||damage.getEntity() instanceof ServerPlayer author&&!author.getUUID().equals(authority.author()))return damage;
        return game.items().indirectKills().captureDamage(damage,authority.cause());
    }
    public void inherit(Entity source,Entity child){
        var authority=source(source);if(authority==null)return;
        bind(child,authority);child.entityTags().add(authority.epoch());game.items().timeStop().prepareFrozen(child);
    }
    public void loaded(Entity entity){
        var source=unloaded.remove(entity.getUUID());
        if(source!=null&&game.items().currentEpoch(source.epoch()))sources.put(entity,source);
        if(entity instanceof Projectile projectile&&!sources.containsKey(entity))inherit(projectile.getOwner(),entity);
        if(sources.containsKey(entity))game.items().timeStop().prepareFrozen(entity);
    }
    public void unloaded(Entity entity){var source=sources.remove(entity);if(source!=null&&entity.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)unloaded.put(entity.getUUID(),source);}
    public void tick(){
        for(var entity:List.copyOf(sources.keySet())){
            var source=sources.get(entity);
            if(source==null)continue;
            if(entity.isRemoved())sources.remove(entity);
            else if(!game.items().currentEpoch(source.epoch())){sources.remove(entity);entity.discard();}
        }
        unloaded.values().removeIf(s->!game.items().currentEpoch(s.epoch()));
    }
    public void forget(ServerPlayer author){
        for(var entity:List.copyOf(sources.keySet()))if(sources.get(entity)!=null&&!sources.get(entity).persistent()&&sources.get(entity).author().equals(author.getUUID())){
            sources.remove(entity);if(entity instanceof Projectile)entity.discard();
        }
        unloaded.values().removeIf(s->!s.persistent()&&s.author().equals(author.getUUID()));
    }
    public void stop(){var actors=List.copyOf(sources.keySet());sources.clear();unloaded.clear();current=null;currentLevel=null;for(var actor:actors)if(actor instanceof Projectile)actor.discard();}
    void checkpointBoundary(){if(scopes!=0||current!=null||currentLevel!=null)throw new IllegalStateException("Finish spell scopes before checkpointing");}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){return new Checkpoint(links);}
    final class Checkpoint {
        private final MatchSpellCheckpoint.IdentityImage<Entity,Source> active;
        private final Map<UUID,Source> asleep=new HashMap<>(unloaded);
        Checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();active=new MatchSpellCheckpoint.IdentityImage<>(sources,links);}
        Runnable prepare(MatchSpellCheckpoint.Links links){var restored=active.restore(links);var dormant=new HashMap<>(asleep);
            return ()->{sources=restored;unloaded.clear();unloaded.putAll(dormant);};}
    }

}
