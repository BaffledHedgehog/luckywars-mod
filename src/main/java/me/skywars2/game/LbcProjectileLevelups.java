package me.skywars2.game;

import java.util.*;
import me.skywars2.mixin.ShulkerBowMixin;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.phys.AABB;

/** Native launch scalars and neutral Gravitas proximity work; no damage/flight engine. */
public final class LbcProjectileLevelups {
    public static final String FINALIZED="skywars2.projectile_levelups.finalized";
    public static final String SNOWBALL_PENDING="skywars2.projectile_levelups.snowball_pending";
    public enum Launch { BOW, CROSSBOW, SNOWBALL, MACHINEGUN }
    public interface Policy {
        /** Supplied by the canonical match-resolved loadout owner, including Random14. */
        boolean hasLevelup(ServerPlayer owner,int id);
        /** Role/helper protection only; neutral physical aura includes hostile/ally ammo. */
        boolean auraTarget(ServerPlayer owner,Entity body);
    }
    private final GameController game;
    private final Policy policy;
    private final LinkedHashSet<UUID> owners=new LinkedHashSet<>();
    private Dispatcher queued;
    private boolean restoring;
    private int lastTick=Integer.MIN_VALUE;
    public LbcProjectileLevelups(GameController game,Policy policy){this.game=game;this.policy=Objects.requireNonNull(policy);}
    private boolean admitted(ServerPlayer p){
        return p!=null&&p.isAlive()&&game.playing()&&!game.maps().busy()
                &&game.canUseGameplayItems(p)&&!game.isObserver(p)&&game.config().lbcLevelups;
    }
    private boolean held(ServerPlayer p){return game.items().timeStop().stopped(p.level())
            ||game.items().timeStop().frozen(p)||game.items().wands().inputTimeStopped(p);}
    /** Called only after the native shot configuration, before actual publication or conversion.
     * No RNG, item consumption, target selection or publication is performed here.
     * Frozen-launch callers must prepareFrozen AFTER these scalar writes. */
    public void beforePublish(ServerPlayer p,Projectile body,Launch launch){
        if(!admitted(p)||body.level()!=p.level()||body.getOwner()!=p||body.isRemoved()
                ||body.entityTags().contains(FINALIZED))return;
        if(launch==Launch.SNOWBALL&&!(body instanceof Snowball))return;
        body.entityTags().add(FINALIZED);
        if(policy.hasLevelup(p,25))body.setDeltaMovement(body.getDeltaMovement().scale(1.3));
        if(body instanceof AbstractArrow arrow&&launch!=Launch.SNOWBALL
                &&(policy.hasLevelup(p,28)||policy.hasLevelup(p,29))){
            double value=((ShulkerBowMixin.ArrowAccess)arrow).skywars2$baseDamage();
            if(policy.hasLevelup(p,28))value=Math.floor(value*125.0)/100.0;
            if(policy.hasLevelup(p,29))value=Math.floor(value*100.0)/100.0+0.5;
            arrow.setBaseDamage(value);
        }
        if(policy.hasLevelup(p,30))body.setNoGravity(true);
    }
    /** Native SnowballItem factory marks only that real use; global spawn hook consumes this
     * after shootFunction has established motion and immediately before addFreshEntity. */
    public static <T extends Projectile> T pendingSnowball(T body){
        body.entityTags().add(SNOWBALL_PENDING);return body;
    }
    public void beforeNativeSpawn(Projectile body){
        if(body.entityTags().remove(SNOWBALL_PENDING)&&body.getOwner() instanceof ServerPlayer p){
            beforePublish(p,body,Launch.SNOWBALL);
            game.items().timeStop().prepareFrozen(body);
        }
    }
    /** Exact finite source #minecraft:projectiles body types. Native special bows/fireballs,
     * cloned ammo and OwnedDirt use these same actual types. Bare custom Projectile helper
     * classes (soul gatherers, wand markers, virtual charges) are not source projectile types. */
    private static final Set<EntityType<?>> GRAVITY_TYPES=Set.of(
            EntityTypes.ARROW,EntityTypes.SPECTRAL_ARROW,EntityTypes.DRAGON_FIREBALL,
            EntityTypes.EGG,EntityTypes.WITHER_SKULL,EntityTypes.ENDER_PEARL,
            EntityTypes.FALLING_BLOCK,EntityTypes.FIREBALL,EntityTypes.FIREWORK_ROCKET,
            EntityTypes.FISHING_BOBBER,EntityTypes.LLAMA_SPIT,EntityTypes.SPLASH_POTION,
            EntityTypes.LINGERING_POTION,EntityTypes.SHULKER_BULLET,EntityTypes.SMALL_FIREBALL,
            EntityTypes.SNOWBALL,EntityTypes.TRIDENT,EntityTypes.TNT,
            EntityTypes.WIND_CHARGE,EntityTypes.BREEZE_WIND_CHARGE);
    public static boolean gravityBody(Entity e){return GRAVITY_TYPES.contains(e.getType());}
    public void reconcile(ServerPlayer p){
        if(admitted(p)&&policy.hasLevelup(p,30))owners.add(p.getUUID());else clear(p.getUUID());
    }
    public void clear(UUID id){owners.remove(id);}
    public void stop(){if(queued!=null)queued.cancel();owners.clear();lastTick=Integer.MIN_VALUE;}
    public void tick(){
        int now=game.server().getTickCount();
        if(restoring||lastTick==now||!game.playing()||game.maps().busy()||!game.server().tickRateManager().runsNormally())return;
        lastTick=now;
        if(queued==null&&!owners.isEmpty()){queued=new Dispatcher();game.items().stonekivi().queuedWork(queued);}
    }
    private boolean auraOwner(ServerPlayer p){return admitted(p)&&policy.hasLevelup(p,30)&&owners.contains(p.getUUID());}
    private boolean target(ServerPlayer p,Entity e){return gravityBody(e)&&!e.isRemoved()&&!e.isNoGravity()
            &&e.level()==p.level()&&!game.items().timeStop().frozen(e)&&!e.entityTags().contains("time_stopped")
            &&e.position().distanceToSqr(p.getEyePosition())<=9&&policy.auraTarget(p,e);}
    private void aura(ServerPlayer p){
        var eye=p.getEyePosition();
        // One radius-3 native spatial query per shared-work owner quantum. No world roster/NBT scan.
        // Source limit1 without sort has no stable identity: choose the first eligible mutable body.
        for(var e:p.level().getEntities((Entity)null,new AABB(eye,eye).inflate(3),e->target(p,e))){
            e.setNoGravity(true);break;
        }
    }
    private final class Dispatcher implements LbcStonekivi.Work {
        final ArrayDeque<UUID> jobs=new ArrayDeque<>(owners);
        boolean cancelled,stepping;
        private ServerPlayer player(UUID id){return game.server().getPlayerList().getPlayer(id);}
        public boolean ready(){
            if(restoring)return false;
            if(cancelled||jobs.isEmpty())return true;
            for(var id:jobs){var p=player(id);if(!auraOwner(p)||!held(p))return true;}
            return false;
        }
        public boolean step(){
            if(stepping)throw new IllegalStateException("Reentrant Gravitas quantum");
            stepping=true;
            try{
                if(cancelled)return true;
                int attempts=jobs.size();
                while(attempts-->0){
                    var id=jobs.removeFirst();var p=player(id);
                    if(auraOwner(p)&&held(p)){jobs.addLast(id);continue;}
                    if(auraOwner(p))aura(p);else owners.remove(id);
                    break;
                }
                if(jobs.isEmpty()){if(queued==this)queued=null;return true;}
                return false;
            }finally{stepping=false;}
        }
        public boolean snapshotIdle(){return !stepping;}
        public void cancel(){cancelled=true;if(queued==this)queued=null;}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            checkpointBoundary();var saved=List.copyOf(jobs);boolean stopped=cancelled;
            return links->()->{jobs.clear();jobs.addAll(saved);cancelled=stopped;if(!cancelled)queued=this;};
        }
    }
    public void checkpointBoundary(){if(!game.server().isSameThread()||queued!=null&&queued.stepping)
        throw new IllegalStateException("Capture projectile perk ownership between native callbacks");}
    public record Checkpoint(List<UUID> owners,int lastTick){
        public Checkpoint {owners=List.copyOf(owners);}
    }
    public Checkpoint checkpoint(){checkpointBoundary();return new Checkpoint(List.copyOf(owners),lastTick);}
    public void prepareFrozen(){checkpointBoundary();restoring=true;if(queued!=null)queued.cancel();}
    /** Shared job graph installs Dispatcher separately; call this after graph.prepare().run(). */
    public void restoreFrozen(Checkpoint image){checkpointBoundary();owners.clear();owners.addAll(image.owners);lastTick=image.lastTick;}
    public void publishFrozen(){restoring=false;}
    /** Ordinary loadout clone adapter supplies the already resolved levelups and enrolls the clone.
     * It does not touch native projectile state, which the native actor adapter copies. */
    public void cloneOwner(ServerPlayer clone){reconcile(clone);}
}
