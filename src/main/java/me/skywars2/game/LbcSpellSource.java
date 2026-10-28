package me.skywars2.game;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

/** Explicit emitter policy and causal author for shared asynchronous spell engines. */
public final class LbcSpellSource {
    public record Source(UUID emitter,UUID author,String epoch,LbcIndirectKills.Owner cause,Set<String> excluded){
        public Source{excluded=Set.copyOf(excluded);}
    }
    private final GameController game;
    private final Map<Entity,Source> sources=new IdentityHashMap<>();
    private final Map<UUID,Source> unloaded=new HashMap<>();
    private Source current;
    public LbcSpellSource(GameController game){this.game=game;}
    public Source hostile(LivingEntity emitter,ServerPlayer author,Set<String> excluded){
        return new Source(emitter.getUUID(),author.getUUID(),game.items().epoch(),game.items().indirectKills().wildCause(author),excluded);
    }
    /** Nesting is restored even when a native payload synchronously launches another spell. */
    public final class Scope implements AutoCloseable {
        private final Source previous;private boolean closed;
        private Scope(Source source){previous=current;current=source!=null&&game.items().currentEpoch(source.epoch())?source:null;}
        @Override public void close(){if(!closed){current=previous;closed=true;}}
    }
    public Scope scope(Source source){return new Scope(source);}
    public Scope scope(Entity entity){return scope(source(entity));}
    public Source current(){return current;}
    public void capture(Entity entity){
        // Reflection stamps the epoch inside the old flight scope; never rebind that old authority.
        if(current!=null&&!entity.entityTags().contains("skywars2.electro.reflected")&&!entity.entityTags().contains(LbcGreed.REFLECTED)
                &&game.items().currentEpoch(current.epoch()))sources.put(entity,current);
    }
    public void bind(Entity entity,Source source){if(source!=null)sources.put(entity,source);}
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
        if(current==null||author==null||!author.getUUID().equals(current.author()))return null;
        return target!=null&&!target.getUUID().equals(current.emitter())&&!LbcGreed.active(target)
                &&game.items().wands().validRoleTarget(target,false)
                &&current.excluded().stream().noneMatch(target.entityTags()::contains);
    }
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
        for(var entity:List.copyOf(sources.keySet()))if(sources.get(entity)!=null&&sources.get(entity).author().equals(author.getUUID())){
            sources.remove(entity);if(entity instanceof Projectile)entity.discard();
        }
        unloaded.values().removeIf(s->s.author().equals(author.getUUID()));
    }
    public void stop(){var actors=List.copyOf(sources.keySet());sources.clear();unloaded.clear();current=null;for(var actor:actors)if(actor instanceof Projectile)actor.discard();}
}
