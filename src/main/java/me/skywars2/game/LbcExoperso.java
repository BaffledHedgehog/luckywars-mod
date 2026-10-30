package me.skywars2.game;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Подготовка Exoperso, нативный полёт и принадлежность смертей одной серии взрывов. */
public final class LbcExoperso {
    public static final String EXPLOSIVE = "skywars2.wand.exoperso";
    public static final EntityType<Charge> TYPE = registerType();

    private final GameController game;
    private final Set<Charge> charges = new LinkedHashSet<>();
    private final ArrayList<Charge> batch = new ArrayList<>();
    private IdentityHashMap<Entity, Series> blastSources = new IdentityHashMap<>();
    private boolean advancing,rewinding;

    private static EntityType<Charge> registerType() {
        var key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.parse("skywars2:exoperso_charge"));
        var type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
                EntityType.Builder.<Charge>of(Charge::new, MobCategory.MISC)
                        .sized(.1f, .1f).clientTrackingRange(8).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type);
        return type;
    }

    public static void register() { }

    public static final class Charge extends Projectile implements PolymerEntity {
        int age;
        int flightAge;
        boolean upgraded;
        Vec3 aim;
        Charge(EntityType<? extends Charge> type, Level level) { super(type, level); }
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
        @Override public EntityType<?> getPolymerEntityType(PacketContext context) { return EntityTypes.MARKER; }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean isPushedByFluid() { return false; }
    }

    private final class Series implements MatchBlastCheckpoint.Binding {
        final UUID owner;
        final String epoch;
        final Set<UUID> requiredPlayers = new HashSet<>();
        final Set<UUID> kills = new HashSet<>();
        Series(ServerPlayer owner, String epoch) {
            this.owner = owner.getUUID();
            this.epoch = epoch;
        }
        public void accept(PrimedTnt tnt){if(tnt==null)return;tnt.entityTags().add(EXPLOSIVE);game.items().markCurrentItemEpoch(tnt);blastSources.put(tnt,this);}
        public void checkpoint(MatchBlastCheckpoint.Graph graph){graph.remember(this,()->{var required=Set.copyOf(requiredPlayers);var saved=Set.copyOf(kills);return links->()->{requiredPlayers.clear();requiredPlayers.addAll(required);kills.clear();kills.addAll(saved);};});}
    }

    LbcExoperso(GameController game) { this.game = game; }
    public int count() { return charges.size(); }
    List<Charge> snapshot() { return List.copyOf(charges); }

    void loaded(Entity entity) {
        if (!(entity instanceof Charge charge)) return;
        if (!game.items().currentEpoch(charge) || charge.level() != game.maps().level()
                || !(charge.getOwner() instanceof ServerPlayer)) {
            cleanup(charge);
            return;
        }
        charges.add(charge);
        if (charge.age >= windup(charge) + 1) game.items().lifetimes().track(charge, true);
    }

    void unloaded(Entity entity) {
        if (entity instanceof Charge charge) {
            charges.remove(charge);
            cleanup(charge);
        }
    }

    /** Returns true only when the native charge was successfully created. */
    public boolean cast(ServerPlayer owner, boolean upgraded) {
        if (!validOwner(owner) || paused(owner)
                || game.items().magic().suppressedAt((net.minecraft.server.level.ServerLevel) owner.level(), owner.getEyePosition()))
            return false;
        Charge charge = new Charge(TYPE, owner.level());
        charge.setOwner(owner);
        charge.setPos(owner.getEyePosition());
        charge.setNoGravity(true);
        charge.upgraded = upgraded;
        charge.setYRot(owner.getYRot());
        charge.setXRot(owner.getXRot());
        charge.setDeltaMovement(Vec3.ZERO);
        game.items().markCurrentItemEpoch(charge);
        if (!owner.level().addFreshEntity(charge)) {
            charge.discard();
            return false;
        }
        charges.add(charge);
        LbcOverload.sound(owner, owner.position().add(0, 20, 0), "minecraft:lbcsounds.exoperso",
                15, upgraded ? 2 : 1, 0);
        return true;
    }

    private int windup(Charge charge) { return charge.upgraded ? 160 : 319; }

    void tick() {if(rewinding)return;if(advancing)throw new IllegalStateException("Reentrant Exoperso advance");advancing=true;try{
        // TNT удаляется перед native explode: освобождаем цепные источники только после его тика.
        blastSources.keySet().removeIf(Entity::isRemoved);
        if (charges.isEmpty()) return;
        batch.addAll(charges);
        try {
            for (Charge charge : batch) {
                if (!charges.contains(charge)) continue;
                if (charge.isRemoved() || !(charge.getOwner() instanceof ServerPlayer owner)
                        || !game.items().currentEpoch(charge) || charge.level() != game.maps().level()
                        || !validOwner(owner) || owner.level() != charge.level()) {
                    remove(charge);
                    continue;
                }
                if (paused(owner)) continue;
                Vec3 here = charge.position();
                if (!owner.level().hasChunkAt(net.minecraft.core.BlockPos.containing(here))
                        || game.items().magic().suppressedAt((net.minecraft.server.level.ServerLevel) charge.level(), here)) {
                    remove(charge);
                    continue;
                }

                int captureAge = windup(charge);
                int age = ++charge.age;
                if (age < captureAge) {
                    charge.setPos(owner.getEyePosition().add(owner.getLookAngle().scale(2)));
                    charge.setYRot(owner.getYRot());
                    charge.setXRot(owner.getXRot());
                    emitCharge(owner, charge.position());
                    continue;
                }
                if (age == captureAge) {
                    charge.aim = owner.getLookAngle().normalize();
                    continue;
                }
                if (age == captureAge + 1) {
                    charge.setPos(owner.getEyePosition());
                    charge.setDeltaMovement(charge.aim.scale(2));
                    game.items().lifetimes().track(charge, true);
                    continue;
                }

                Vec3 from = charge.position();
                Vec3 to = from.add(charge.getDeltaMovement());
                HitResult hit = game.items().wands().firstHit(owner, from, to,
                        entity -> !entity.entityTags().contains("strict_map_object"));
                if (hit instanceof BlockHitResult wall && !charge.level().hasChunkAt(wall.getBlockPos())) {
                    remove(charge);
                    continue;
                }
                Vec3 end = hit == null ? to : hit.getLocation();
                emitFlight(owner, from, end);
                charge.setPos(end);
                if (hit != null) {
                    remove(charge);
                    if (hit instanceof BlockHitResult || hit instanceof EntityHitResult entityHit
                            && entityHit.getEntity() instanceof LivingEntity target
                            && !target.entityTags().contains("strict_map_object")
                            && game.items().lifetimes().target(owner, target))
                        impact(owner, end);
                    continue;
                }
                charge.flightAge++;
                game.items().lifetimes().advance(charge, charge.flightAge);
            }
        } finally {
            batch.clear();
        }
        }finally{advancing=false;}
    }

    private void emitCharge(ServerPlayer owner, Vec3 at) {
        owner.level().sendParticles(ParticleTypes.FIREWORK, true, false, at.x, at.y, at.z, 1, .1, .1, .1, .3);
        owner.level().sendParticles(ParticleTypes.FLAME, true, false, at.x, at.y, at.z, 1, .1, .1, .1, .05);
        owner.level().sendParticles(ParticleTypes.FALLING_LAVA, true, false, at.x, at.y, at.z, 1, .1, .1, .1, .3);
    }

    private void emitFlight(ServerPlayer owner, Vec3 from, Vec3 to) {
        double distance = from.distanceTo(to);
        for (double d = 0; d <= distance + 1e-7; d += .5) {
            Vec3 p = distance < 1e-9 ? from : from.add(to.subtract(from).scale(Math.min(1, d / distance)));
            owner.level().sendParticles(ParticleTypes.FIREWORK, true, false, p.x, p.y, p.z,
                    1, .1, .1, .1, .3);
            owner.level().sendParticles(ParticleTypes.FLAME, true, false, p.x, p.y, p.z,
                    1, .1, .1, .1, .05);
            owner.level().sendParticles(ParticleTypes.FALLING_LAVA, true, false, p.x, p.y, p.z,
                    1, .1, .1, .1, .3);
        }
    }

    /** Edible Exoperso uses the same blast series and achievement/causal attribution as its wand. */
    void edibleBurst(ServerPlayer owner, Vec3 point) { impact(owner, point); }

    private void impact(ServerPlayer owner, Vec3 point) {
        if (!validOwner(owner)) return;
        Series series = new Series(owner, game.items().epoch());
        // Снимок в момент попадания: другие касты и отложенное выполнение очереди его не меняют.
        for (ServerPlayer player : game.alivePlayers())
            if (!player.getUUID().equals(series.owner)) series.requiredPlayers.add(player.getUUID());
        game.items().exasperato().exopersoBurst(owner, point, series);
    }

    /** Used by the queue's blast callback so source attribution survives the synchronous explosion. */
    public void blastFinished(Entity source) { blastSources.remove(source); }

    /** Attach a chain explosion to the same series as its parent TNT. */
    public void inheritBlast(Entity source, Entity spawned) {
        Series series = blastSources.get(source);
        if (series != null && spawned != null) blastSources.put(spawned, series);
    }

    /** Called before GameController records a player death. */
    public void died(LivingEntity target, DamageSource source) {
        if (!(target instanceof ServerPlayer victim) || source == null) return;
        Entity direct = source.getDirectEntity();
        Series series = blastSources.get(direct);
        if (series == null || !game.items().currentEpoch(series.epoch)
                || !(source.getEntity() instanceof ServerPlayer attacker) || !series.owner.equals(attacker.getUUID())
                || !series.requiredPlayers.contains(victim.getUUID())) return;
        series.kills.add(victim.getUUID());
        ServerPlayer owner = victim.level().getServer().getPlayerList().getPlayer(series.owner);
        if (owner == null || !owner.isAlive() || game.isObserver(owner) || !game.canUseGameplayItems(owner)) return;
        if (series.requiredPlayers.size() >= 2 && series.kills.containsAll(series.requiredPlayers))
            game.items().mana().award(owner, "lbc:true_advancements/staffs/exoperso");
    }

    private boolean validOwner(ServerPlayer owner) {
        return owner != null && !owner.isRemoved() && owner.isAlive() && owner.level() == game.maps().level()
                && game.canUseGameplayItems(owner) && !game.isObserver(owner)
                && game.stage() != GameController.Stage.ENDING;
    }

    private boolean paused(ServerPlayer owner) {
        return game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped");
    }

    private void remove(Charge charge) {
        charges.remove(charge);
        cleanup(charge);
    }

    private void cleanup(Charge charge) {
        game.items().lifetimes().unloaded(charge);
        if (!charge.isRemoved()) charge.discard();
    }

    public void stop() {
        for (Charge charge : List.copyOf(charges)) cleanup(charge);
        charges.clear();
        batch.clear();
        blastSources.clear();rewinding=false;
    }

    void checkpointBoundary(){if(advancing||!batch.isEmpty())throw new IllegalStateException("Finish Exoperso callbacks before checkpointing");}
    void prepareCheckpoint(){rewinding=true;}
    void cancelCheckpoint(){rewinding=false;}
    Checkpoint checkpoint(MatchBlastCheckpoint.Graph graph){return new Checkpoint(graph);}
    final class Checkpoint {
        private record Actor(MatchDamageSources.Ref key,int age,int flight,boolean upgraded,Vec3 aim){}
        private final List<Actor> actors;private final MatchSpellCheckpoint.IdentityImage<Entity,Series> series;
        Checkpoint(MatchBlastCheckpoint.Graph graph){actors=charges.stream().map(c->new Actor(graph.links.key(c),c.age,c.flightAge,c.upgraded,c.aim)).toList();blastSources.values().forEach(v->v.checkpoint(graph));series=new MatchSpellCheckpoint.IdentityImage<>(blastSources,graph.links);}
        Runnable prepare(MatchSpellCheckpoint.Links links){var bodies=new LinkedHashSet<Charge>();for(var a:actors){if(!(links.get(a.key) instanceof Charge c))throw new IllegalStateException("Restore native Exoperso charges first");bodies.add(c);}var ledger=series.restore(links);return ()->{charges.clear();charges.addAll(bodies);for(var a:actors){var c=(Charge)links.get(a.key);c.age=a.age;c.flightAge=a.flight;c.upgraded=a.upgraded;c.aim=a.aim;}blastSources=ledger;rewinding=false;};}
    }
}
