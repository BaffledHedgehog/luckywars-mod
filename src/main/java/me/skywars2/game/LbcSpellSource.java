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
    public static final String WILD_LOOT="skywars2.wild_loot";
    public static final String HELMET_CREDIT="skywars2.helmet_trap_credit";
    public interface TntOwnerAccess { void skywars2$clearLootOwner(); LivingEntity skywars2$nativeLootOwner(); }
    public record Source(UUID emitter,UUID author,String epoch,LbcIndirectKills.Owner cause,Set<String> excluded,boolean persistent,boolean unownedMobs,LivingEntity nativeEmitter,boolean helmetCredit){
        public Source{excluded=Set.copyOf(excluded);}
        public Source(UUID emitter,UUID author,String epoch,LbcIndirectKills.Owner cause,Set<String> excluded,boolean persistent,boolean unownedMobs,LivingEntity nativeEmitter){this(emitter,author,epoch,cause,excluded,persistent,unownedMobs,nativeEmitter,false);}
        public Source(UUID emitter,UUID author,String epoch,LbcIndirectKills.Owner cause,Set<String> excluded,boolean persistent,boolean unownedMobs){this(emitter,author,epoch,cause,excluded,persistent,unownedMobs,null);}
        public Source(UUID emitter,UUID author,String epoch,LbcIndirectKills.Owner cause,Set<String> excluded){this(emitter,author,epoch,cause,excluded,false,false);}
        public Source(UUID emitter,UUID author,String epoch,LbcIndirectKills.Owner cause,Set<String> excluded,boolean persistent){this(emitter,author,epoch,cause,excluded,persistent,false);}
        public Source hostileLoot(){return new Source(emitter,author,epoch,cause,excluded,persistent,true,nativeEmitter,helmetCredit);}
        public Source helmetLoot(){return new Source(emitter,author,epoch,cause,excluded,persistent,true,nativeEmitter,true);}
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
    public Scope scope(Entity entity){var policy=source(entity);if(policy!=null&&entity.entityTags().contains(WILD_LOOT))policy=policy.hostileLoot();return new Scope(policy,entity!=null&&entity.level() instanceof ServerLevel level?level:null);}
    /** Q drops use the same hostile-loot policy as Lucky rewards; creator is only causal credit. */
    public Scope lootScope(net.minecraft.world.entity.item.ItemEntity token){
        var policy=source(token);
        if(policy==null)policy=current;
        if(policy==null){
            var cause=game.items().specialDrops().cause(token);
            policy=new Source(token.getUUID(),cause==null?null:cause.player(),game.items().epoch(),cause,Set.of());
        }
        return new Scope(policy.hostileLoot(),(ServerLevel)token.level());
    }
    public boolean lootProjectile(Entity actor){
        if(actor==null)return false;
        var policy=source(actor);
        return actor.entityTags().contains(WILD_LOOT)||policy!=null&&policy.unownedMobs();
    }
    /** Physical controller is deliberately independent of retained player kill credit. */
    public Entity projectileController(Entity actor){
        if(actor==null)return null;
        var policy=source(actor);
        if(actor.entityTags().contains(WILD_LOOT)||policy!=null&&policy.unownedMobs()||policy==null&&current!=null&&current.unownedMobs())return null;
        var reflected=game.items().electro().reflectedOwner(actor);if(reflected!=null)return reflected;
        if(actor instanceof Projectile shot&&shot.getOwner()!=null)return shot.getOwner();
        if(actor instanceof net.minecraft.world.entity.item.PrimedTnt tnt&&tnt.getOwner()!=null)return tnt.getOwner();
        if(LbcClouds.sourceAcidManaged(actor))return game.items().clouds().sourceAcidOwner(actor);
        return policy!=null&&actor.level() instanceof ServerLevel level?level.getEntity(policy.emitter()):null;
    }
    public ServerLevel level(ServerPlayer fallback){return currentLevel!=null?currentLevel:fallback==null?null:fallback.level();}
    /** A source without a player author is a real NPC/world source, not a borrowed executor. */
    public ServerPlayer authorPlayer(Source source){return source==null||source.author()==null?null:game.server().getPlayerList().getPlayer(source.author());}
    public ServerPlayer author(Entity entity){var policy=source(entity);return policy!=null?policy.author()==null?null:game.server().getPlayerList().getPlayer(policy.author())
            :entity instanceof Projectile shot&&shot.getOwner() instanceof ServerPlayer player?player:entity instanceof ServerPlayer player?player:null;}
    public boolean persistent(){return current!=null&&current.persistent()&&game.items().currentEpoch(current.epoch());}
    public Source current(){return current;}
    public void capture(Entity entity){
        // Loading a teleported player inside a spell scope never transfers body ownership.
        if(entity instanceof ServerPlayer)return;
        // Native children keep their actual hostile parent, not the player's
        // outer loot-dispatch scope. This also covers TNT riding its carrier.
        Entity parent=entity instanceof Projectile shot?shot.getOwner():entity.getVehicle();
        if(parent!=null&&(unownedHazard(parent)||parent.entityTags().contains(WILD_LOOT))){inherit(parent,entity);return;}
        // A publication callback can run inside an unrelated loot scope. Accepted
        // provenance (including persistence) is immutable until explicit control transfer.
        var remembered=unloaded.remove(entity.getUUID());
        if(remembered!=null&&game.items().currentEpoch(remembered.epoch())){sources.put(entity,remembered);return;}
        var accepted=sources.get(entity);
        if(accepted!=null&&game.items().currentEpoch(accepted.epoch()))return;
        // Reflection stamps the epoch inside the old flight scope; never rebind that old authority.
        if(current!=null&&!entity.entityTags().contains("skywars2.electro.reflected")&&!entity.entityTags().contains(LbcGreed.REFLECTED)
                &&game.items().currentEpoch(current.epoch())){bind(entity,current);entity.entityTags().add(current.epoch());
            if(entity instanceof net.minecraft.world.entity.Mob&&(current.unownedMobs()||unownedHazard(entity)))unowned(entity);
            else if(current.unownedMobs()&&current.author()==null)entity.entityTags().add("skywars2.unowned_hazard");}
    }
    /** Loot mobs are hostile hazards; only helmet roots retain human statistical credit. */
    public void unowned(Entity entity){
        entity.entityTags().add(WILD_LOOT);
        var previous=sources.get(entity);if(previous==null)previous=current;
        boolean credit=previous!=null&&previous.helmetCredit();
        if(credit){entity.entityTags().remove("skywars2.unowned_hazard");entity.entityTags().add(HELMET_CREDIT);}
        else entity.entityTags().add("skywars2.unowned_hazard");
        var body=entity instanceof LivingEntity living&&!(living instanceof net.minecraft.world.entity.player.Player)?living:previous==null?null:previous.nativeEmitter();
        sources.put(entity,new Source(entity.getUUID(),credit?previous.author():null,game.items().epoch(),credit?previous.cause():null,previous==null?Set.of():previous.excluded(),previous!=null&&previous.persistent(),true,body,credit));
    }
    public static boolean playersOnlyTrap(Entity entity){return entity!=null&&(unownedHazard(entity)||entity.entityTags().contains(WILD_LOOT))&&entity.entityTags().contains("skywars2.players_only_trap");}
    public static boolean unownedHazard(Entity entity){
        // A native reflection/controller change may precede the next source lookup.
        if(entity instanceof Projectile shot&&shot.getOwner() instanceof ServerPlayer)return false;
        if(entity!=null&&entity.entityTags().contains("skywars2.unowned_hazard"))return true;
        if(entity instanceof Projectile shot)entity=shot.getOwner();
        return entity!=null&&entity.entityTags().contains("skywars2.unowned_hazard");
    }
    /** Resolve only native Mob authority; never turn retained creator metadata into a player attack. */
    public LivingEntity nativeMobSource(Entity actor){
        if(actor instanceof LivingEntity living&&!(living instanceof net.minecraft.world.entity.player.Player))return living;
        if(actor!=null&&actor.getVehicle() instanceof LivingEntity living&&!(living instanceof net.minecraft.world.entity.player.Player)&&lootProjectile(living))return living;
        var policy=source(actor);if(policy==null)policy=current;
        var level=actor!=null&&actor.level() instanceof ServerLevel actual?actual:currentLevel;
        if(policy!=null&&(policy.author()==null||policy.helmetCredit())&&level!=null){
            if(level.getEntity(policy.emitter()) instanceof LivingEntity living&&!(living instanceof net.minecraft.world.entity.player.Player))return living;
            if(policy.unownedMobs()&&policy.nativeEmitter()!=null)return policy.nativeEmitter();
        }
        return null;
    }
    public void bind(Entity entity,Source source){
        if(source==null)return;
        if(!source.helmetCredit()&&source.unownedMobs()&&source.author()==null&&current!=null&&current.helmetCredit()&&entity instanceof net.minecraft.world.entity.Mob)
            source=new Source(source.emitter(),current.author(),source.epoch(),current.cause(),source.excluded(),source.persistent(),true,source.nativeEmitter(),true);
        if(!source.helmetCredit()&&current!=null&&current.helmetCredit()&&Objects.equals(source.author(),current.author()))source=source.helmetLoot();
        if(entity.entityTags().contains(WILD_LOOT)||current!=null&&current.unownedMobs())source=source.hostileLoot();
        if(source.helmetCredit()){entity.entityTags().add(HELMET_CREDIT);entity.entityTags().remove("skywars2.unowned_hazard");}
        if(source.unownedMobs()){
            entity.entityTags().add(WILD_LOOT);
            // The receipt above retains the creator; native physics gets no creator ownership.
            if(entity instanceof Projectile shot&&shot.getOwner() instanceof ServerPlayer)shot.setOwner(null);
            if(entity instanceof net.minecraft.world.entity.item.PrimedTnt tnt&&((TntOwnerAccess)tnt).skywars2$nativeLootOwner() instanceof ServerPlayer)
                ((TntOwnerAccess)tnt).skywars2$clearLootOwner();
            if(entity instanceof net.minecraft.world.entity.AreaEffectCloud cloud&&cloud.getOwner() instanceof ServerPlayer)cloud.setOwner(null);
            if(entity instanceof net.minecraft.world.entity.projectile.EvokerFangs fangs&&fangs.getOwner() instanceof ServerPlayer)fangs.setOwner(null);
            if(entity instanceof net.minecraft.world.entity.LightningBolt bolt&&bolt.getCause()!=null)bolt.setCause(null);
        }
        // Retain a native body only for the lifetime of its accepted policy/carriers, never a UUID history.
        if((source.author()==null||source.helmetCredit())&&source.unownedMobs()&&source.nativeEmitter()==null
                &&entity.level() instanceof ServerLevel level&&level.getEntity(source.emitter()) instanceof LivingEntity living
                &&!(living instanceof net.minecraft.world.entity.player.Player))source=new Source(source.emitter(),source.author(),source.epoch(),source.cause(),source.excluded(),source.persistent(),true,living,source.helmetCredit());
        sources.put(entity,source);
    }
    /** New attacks belong to the new controller; already inherited child policies remain immutable. */
    void controllerChanged(Entity emitter,ServerPlayer controller){
        emitter.entityTags().remove("skywars2.unowned_hazard");
        emitter.entityTags().remove(HELMET_CREDIT);
        if(emitter instanceof net.minecraft.world.entity.Mob)emitter.entityTags().remove(WILD_LOOT);
        var previous=source(emitter);if(previous==null||controller.getUUID().equals(previous.author()))return;
        var next=game.items().indirectKills().wildCause(controller);var old=previous.cause();
        var cause=old==null?next:new LbcIndirectKills.Owner(next.player(),next.order(),old.friendlyFire(),old.projectile());
        sources.put(emitter,new Source(emitter.getUUID(),controller.getUUID(),previous.epoch(),cause,previous.excluded(),previous.persistent()));
    }
    public Source source(Entity entity){
        var source=sources.get(entity);if(source==null||!game.items().currentEpoch(source.epoch()))return null;
        if(entity.entityTags().contains(LbcGreed.REFLECTED)||entity.entityTags().contains("skywars2.electro.reflected")){reflected(entity);return null;}
        // Native reflection transfers the projectile to its new author and restores ordinary targeting.
        if(entity instanceof Projectile projectile&&projectile.getOwner() instanceof ServerPlayer owner&&!owner.getUUID().equals(source.author())){reflected(entity);return null;}
        return source;
    }
    public void reflected(Entity entity){entity.entityTags().removeAll(Set.of("skywars2.unowned_hazard",WILD_LOOT,HELMET_CREDIT,"skywars2.players_only_trap"));sources.remove(entity);unloaded.remove(entity.getUUID());}
    public boolean wild(Entity entity){return source(entity)!=null;}
    public ServerPlayer owner(Entity entity){var source=source(entity);return source==null||source.author()==null?null:game.server().getPlayerList().getPlayer(source.author());}
    /** Null means the existing engine owns policy; false remains a protected role or source exclusion. */
    public Boolean target(ServerPlayer author,Entity target){
        if(current==null||author!=null&&!author.getUUID().equals(current.author()))return null;
        return target(current,target);
    }
    /** Accepted emitter policy does not depend on the causal author remaining online. */
    public boolean target(Source source,Entity target){
        if(!LbcBedwarsHarm.sourceAllowed(source,target))return false;
        if(LbcBedwarsRoleGuards.shop(target)||!LbcBedwarsRoleGuards.canEmit(source,target!=null&&target.level() instanceof ServerLevel level?level:null))return false;
        return source!=null&&game.items().currentEpoch(source.epoch())&&target!=null
                &&(!target.getUUID().equals(source.emitter())||source.unownedMobs())&&!LbcGreed.active(target)
                &&game.items().wands().validRoleTarget(target,false)
                &&source.excluded().stream().noneMatch(target.entityTags()::contains);
    }
    public boolean canAffect(Entity emitter,Entity target){
        if(!LbcBedwarsRoleGuards.canEmitFrom(emitter)||LbcBedwarsRoleGuards.shop(target))return false;
        var source=source(emitter);
        if(!LbcBedwarsHarm.sourceAllowed(source,target))return false;
        // Native arrows/bullets retain muzzle immunity in Projectile.canHitEntity, then may return.
        // Spell rays still exclude their emitter; captured kill credit must not change physics.
        if(source!=null&&LbcWands.physicalProjectile(emitter)&&!ProjectileLifetimes.protectedWandProjectile(emitter))return game.items().currentEpoch(source.epoch())
                &&target!=null&&!LbcGreed.active(target)&&game.items().wands().validRoleTarget(target,false)
                &&source.excluded().stream().noneMatch(target.entityTags()::contains);
        return target(source,target);
    }
    public boolean persistent(Entity emitter){var source=source(emitter);return source!=null&&source.persistent();}
    public DamageSource damage(DamageSource damage){
        if(LbcIndirectKills.npcSource(damage))return damage;
        var authority=source(damage.getDirectEntity());
        if(authority==null)authority=current;
        if(authority!=null&&authority.unownedMobs()&&authority.author()==null
                &&!(damage.getEntity() instanceof ServerPlayer&&!unownedHazard(damage.getDirectEntity()))){
            // Native trap managers may retain a dead parent for its cloud/TNT.
            // Its accepted cause is stronger than a world lookup after removal.
            var attacker=damage.getEntity() instanceof LivingEntity living&&!(living instanceof net.minecraft.world.entity.player.Player)
                    ?living:nativeMobSource(damage.getDirectEntity()!=null?damage.getDirectEntity():damage.getEntity());
            return game.items().indirectKills().nativeMobDamage(damage,attacker);
        }
        if(authority!=null&&authority.unownedMobs()){
            var actual=damage.getEntity() instanceof LivingEntity living&&!(living instanceof net.minecraft.world.entity.player.Player)?living:nativeMobSource(damage.getDirectEntity());
            return game.items().indirectKills().captureDamage(damage,authority.cause(),actual);
        }
        if(authority==null||damage.getEntity() instanceof ServerPlayer author&&!author.getUUID().equals(authority.author()))return damage;
        return game.items().indirectKills().captureDamage(damage,authority.cause());
    }
    public void inherit(Entity source,Entity child){
        var authority=source(source);if(authority==null)return;
        if(source.entityTags().contains(WILD_LOOT))authority=authority.hostileLoot();
        if((authority.author()==null||authority.helmetCredit())&&authority.unownedMobs()&&source instanceof LivingEntity living
                &&!(living instanceof net.minecraft.world.entity.player.Player)&&authority.nativeEmitter()!=living)
            authority=new Source(authority.emitter(),authority.author(),authority.epoch(),authority.cause(),authority.excluded(),authority.persistent(),true,living,authority.helmetCredit());
        bind(child,authority);if(playersOnlyTrap(source)&&child instanceof net.minecraft.world.entity.Mob)child.entityTags().add("skywars2.players_only_trap");if(authority.unownedMobs()&&authority.author()==null)child.entityTags().add("skywars2.unowned_hazard");child.entityTags().add(authority.epoch());game.items().timeStop().prepareFrozen(child);
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
        for(var entity:List.copyOf(sources.keySet()))if(sources.get(entity)!=null&&!sources.get(entity).persistent()&&!sources.get(entity).unownedMobs()&&author.getUUID().equals(sources.get(entity).author())){
            sources.remove(entity);if(entity instanceof Projectile)entity.discard();
        }
        unloaded.values().removeIf(s->!s.persistent()&&!s.unownedMobs()&&author.getUUID().equals(s.author()));
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
