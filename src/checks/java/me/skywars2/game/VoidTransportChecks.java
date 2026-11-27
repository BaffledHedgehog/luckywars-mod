package me.skywars2.game;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import me.skywars2.game.combat.MatchDamageSources;
import me.skywars2.game.items.wands.LbcSauvojen;
import me.skywars2.game.items.wands.LbcVoidArcanum;
import me.skywars2.game.items.wands.LbcWands;
import me.skywars2.game.match.checkpoints.MatchActorCheckpoint;
import me.skywars2.game.match.checkpoints.MatchSpellCheckpoint;
import me.skywars2.game.world.maps.MapManager;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Authoritative stationary/virtual transport and canonical whole-match rollback of every test producer. */
public final class VoidTransportChecks {
    private static Scene running;
    private static void require(boolean ok, String why) { if (!ok) throw new IllegalStateException("Void transport: " + why); }
    private static void near(Vec3 actual, Vec3 wanted, String why) { require(actual.distanceToSqr(wanted) < 1e-9, why + ": " + actual + " vs " + wanted); }
    private static CompoundTag saved(Entity entity) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output); return output.buildResult();
    }
    private static void load(Entity entity, CompoundTag data) {
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), data.copy()));
    }
    @SuppressWarnings("unchecked") private static CompoundTag bossData(Warden boss) {
        try {
            var field = me.skywars2.game.entities.mobs.LbcGrimoireBoss.class.getDeclaredField("DATA"); field.setAccessible(true);
            return boss.getAttached((AttachmentType<CompoundTag>)field.get(null));
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    private static Vec3 anchor(Warden boss) {
        var data = bossData(boss); return new Vec3(data.getDoubleOr("anchorX", 0), data.getDoubleOr("anchorY", 0), data.getDoubleOr("anchorZ", 0));
    }
    private static final class Scene {
        final GameController game;
        final ServerPlayer owner, other;
        final ServerLevel level;
        final RegenerationBeltChecks.ScheduledClient wire, audience;
        final net.minecraft.server.network.ServerGamePacketListenerImpl previous, previousOther;
        final Vec3 base;
        final boolean frozen;
        final int start;
        int trialStart, restoreStart, baselineEoe, baselineNukes, baselineBosses, baselinePumpkins, baselineHoming, baselineDup, baselineDrones;
        final Map<BlockPos,MapManager.BlockSnapshot> baselineTerrain = new LinkedHashMap<>();
        CompletableFuture<?> ready;
        int phase, at;
        Marker beacon;
        Warden boss;
        LbcWands.ElectroTrace trace;
        Vec3 movedBeacon, movedBoss, oldTrace, movedTrace, movedDestiny, movedDup, movedDrone, movedPumpkin;
        LbcSauvojen.Drone drone;
        long pumpkinId;
        int traceAge, droneLifetime;
        Throwable failure;
        boolean captured, restoring, finished;
        Scene(GameController game) {
            this.game = game;
            owner = Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            other = Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            level = owner.level(); frozen = game.server().tickRateManager().isFrozen(); start = game.server().getTickCount();
            require(game.playing() && game.allowDamage(owner) && game.allowDamage(other) && !frozen
                    && !game.vanity().busy() && !game.vanity().hasSnapshot() && game.vanity().scheduleState() == null,
                    "exclusive native fixture with no borrowed save point");
            require(!owner.isUsingItem() && !other.isUsingItem() && game.items().wands().electroSnapshot().isEmpty()
                    && game.items().voidArcanum().count() == 0 && game.items().sauvojen().stars().count() == 0,
                    "no unrelated short effects that canonical rewind is allowed to cancel");
            base = new Vec3(Math.floor(owner.getX()) + .5, Math.min(242.5, level.getMaxY() - 20.5), Math.floor(owner.getZ()) + .5);
            previous = owner.connection; previousOther = other.connection;
            wire = new RegenerationBeltChecks.ScheduledClient(game, owner); audience = new RegenerationBeltChecks.ScheduledClient(game, other);
            var tasks = new ArrayList<CompletableFuture<?>>();
            for (int x = -1; x <= 7; x++) for (int z = -1; z <= 1; z++) tasks.add(game.maps().prepareChunk(level,
                    new ChunkPos((BlockPos.containing(base).getX() >> 4) + x, (BlockPos.containing(base).getZ() >> 4) + z)));
            ready = CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
        }
        void acknowledge() { wire.acknowledge(); audience.acknowledge(); }
        Vec3 center(int index) { return base.add(index * 16, 0, 0); }
        void moveOwner(Vec3 point) {
            owner.teleportTo(level, point.x, point.y, point.z, Set.of(), 0, 0, true); owner.setDeltaMovement(Vec3.ZERO); acknowledge();
        }
        void air(Vec3 center, int radius) {
            var origin = BlockPos.containing(center);
            for (var pos : BlockPos.betweenClosed(origin.offset(-radius, -5, -radius), origin.offset(radius, 10, radius)))
                if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16 | 32);
        }
        void captureBaseline() {
            // Freeze before acquisition so a borrowed producer cannot expire or lose one pendingEoe tick
            // between our oracle and the coordinator's actual end-frame capture. Native IO can still finish.
            require(game.items().nukeEvent().captureReady() && game.items().duplicating().pendingCheckpointWrites() == 0,
                    "borrowed nuclear/Duplicating work has naturally reached its capture boundary");
            game.server().tickRateManager().setFrozen(true);
            baselineEoe = game.items().nukeEvent().pointPendingTicks(); baselineNukes = game.items().nukeEvent().markerCount();
            baselineBosses = game.items().grimoireBoss().snapshot().size(); baselinePumpkins = game.items().pumpkinator().count();
            baselineHoming = game.items().homing().destinyCount(); baselineDup = game.items().duplicating().markerSnapshot().size();
            baselineDrones = game.items().sauvojen().count();
            for (int i = 0; i <= 6; i++) {
                var origin = BlockPos.containing(center(i));
                for (var pos : BlockPos.betweenClosed(origin.offset(-6, -5, -6), origin.offset(6, 10, 6)))
                    baselineTerrain.put(pos.immutable(), game.maps().snapshot(level, pos));
            }
            require(game.vanity().requestCapture(owner, false), "full native baseline save request");
        }
        LbcVoidArcanum.Hole hole(Vec3 point) {
            var hole = game.items().voidArcanum().spawn(owner, point); require(hole != null, "real native black-hole body"); return hole;
        }
        void pull(LbcVoidArcanum.Hole hole) { game.items().voidArcanum().physics(hole, owner, game.items().voidArcanum().source(hole).cause()); }
        void removeOwnHole() { owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); game.items().voidArcanum().forget(owner); }
        void initializeTrial() {
            game.config().lbcSkills = false; game.config().vanillaSkills = false; game.config().ranked = false;
            for (var player : List.of(owner, other)) {
                player.removeAllEffects(); player.setNoGravity(true); player.getInventory().clearContent();
                player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000); player.setHealth(1000);
                game.players().get(player.getUUID()).lbcSkill = 0; game.items().mana().set(player, 100000);
            }
            moveOwner(base.add(0, 0, -20)); other.teleportTo(level, base.x, base.y, base.z - 25, Set.of(), 0, 0, true); acknowledge();
            for (int i = 0; i <= 6; i++) air(center(i), 6);
            var previousMarkers = new HashSet<>(game.items().nukeEvent().markers());
            require(game.items().nukeEvent().startPlaced(owner, center(0).add(1.45, 0, 0)), "actual placed nuclear beacon producer");
            beacon = game.items().nukeEvent().markers().stream().filter(marker -> !previousMarkers.contains(marker)).findFirst().orElseThrow();
        }
        void beaconPull() {
            var service = game.items().nukeEvent(); var old = beacon.blockPosition();
            require(level.getBlockState(old.above()).is(Blocks.BEACON) && service.pointPendingTicks() > 0,
                    "native9-netherite/beacon/pane warning has really completed");
            var pane = level.getBlockState(old.above(2));
            for (var pos : BlockPos.betweenClosed(old.offset(-1, 0, -1), old.offset(1, 0, 1)))
                require(level.getBlockState(pos).is(Blocks.NETHERITE_BLOCK), "native authored beacon base");
            var h = hole(center(0)); for (int i = 0; i < 8; i++) pull(h);
            movedBeacon = beacon.position(); require(movedBeacon.x < old.getX(), "actual marker crossed a block boundary toward hole");
            var now = beacon.blockPosition();
            require(!old.equals(now) && level.getBlockState(now.above()).is(Blocks.BEACON)
                    && level.getBlockState(now.above(2)).equals(pane), "beacon and actual pane follow authoritative marker");
            for (var pos : BlockPos.betweenClosed(now.offset(-1, 0, -1), now.offset(1, 0, 1)))
                require(level.getBlockState(pos).is(Blocks.NETHERITE_BLOCK), "all9 native base blocks move to the new footprint");
            var nativeImage = saved(beacon);
            var terrain = new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
            for (var pos : BlockPos.betweenClosed(now.offset(-5, -1, -4), now.offset(6, 3, 4))) terrain.put(pos.immutable(), game.maps().snapshot(level, pos));
            var bodies = MatchActorCheckpoint.currentBodies(game); var links = new MatchSpellCheckpoint.Links(new MatchDamageSources(game, bodies.values()));
            var checkpoint = service.checkpoint(links); require(links.resolve(bodies::get).isEmpty(), "nuclear checkpoint source graph");
            int pending = service.pointPendingTicks(); h.point = center(0).add(2, 0, 0); for (int i = 0; i < 8; i++) pull(h);
            game.server().tickRateManager().setFrozen(true);
            try {
                load(beacon, nativeImage); terrain.forEach((pos,image) -> game.maps().restoreSnapshot(level, pos, image, 2 | 16 | 32));
                service.prepareFrozen(); checkpoint.prepare(links).run(); service.publishFrozen();
                near(beacon.position(), movedBeacon, "native marker position restores with nuclear checkpoint");
                require(service.pointPendingTicks() == pending, "placed global pendingEoe deadline is restored, not reset or leaked");
            } finally { game.server().tickRateManager().setFrozen(false); }
            removeOwnHole();
        }
        void bossPull() {
            boss = game.items().grimoireBoss().spawn(level, center(1).add(1.1, 0, 0), 0, owner);
            require(boss != null && game.items().grimoireBoss().stationaryRoot(boss), "native stationary Grimoire Warden");
            boss.setNoGravity(true); boss.noPhysics = true; var before = boss.position(); var h = hole(center(1)); pull(h);
            movedBoss = boss.position(); require(movedBoss.x < before.x && boss.getDeltaMovement().lengthSqr() == 0, "stationary root moves physically without unapplied velocity");
            near(anchor(boss), movedBoss, "authoritative native boss anchor follows root");
            var images = new IdentityHashMap<Entity,CompoundTag>();
            for (var entity : game.items().grimoireBoss().snapshot()) if (entity.position().distanceToSqr(movedBoss) < 100) images.put(entity, saved(entity));
            h.point = h.point.add(2, 0, 0); pull(h);
            game.server().tickRateManager().setFrozen(true);
            try { images.forEach(VoidTransportChecks::load); near(boss.position(), movedBoss, "native boss NBT restores moved pose"); near(anchor(boss), movedBoss, "native attachment checkpoint restores moved anchor"); }
            finally { game.server().tickRateManager().setFrozen(false); }
            removeOwnHole();
        }
        void virtualPulls() {
            var empty = ItemStack.EMPTY;
            var beforeTraces = new HashSet<>(game.items().wands().electroSnapshot());
            oldTrace = center(2).add(.1, 1.05, 0);
            game.items().wands().directedElectro(owner, oldTrace, new Vec3(0, 0, 1), 1, null);
            trace = game.items().wands().electroSnapshot().stream().filter(value -> !beforeTraces.contains(value)).findFirst().orElseThrow();
            trace.erecto=new me.skywars2.game.items.weapons.ranged.LbcErecto.Path(
                    me.skywars2.game.items.weapons.ranged.LbcErecto.SIDE,oldTrace,oldTrace.add(0,0,8),14,1,owner,InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            var curveOrigin=trace.erecto.origin;var curveFocus=trace.erecto.focus;int curveSide=trace.erecto.side;
            var h = hole(center(2)); pull(h); movedTrace = trace.position; traceAge = trace.age;
            require(movedTrace.y < oldTrace.y, "electrical trace's authoritative slow-ray position is pulled");
            var curveShift=movedTrace.subtract(oldTrace);near(trace.erecto.origin,curveOrigin.add(curveShift),"curved Erecto launch origin follows physical displacement");
            near(trace.erecto.focus,curveFocus.add(curveShift),"curved Erecto focus translates by the same delta rather than attracting independently");
            require(trace.age==0&&trace.erecto.side==curveSide&&trace.erecto.focusSteps==14,"physical displacement preserves curve phase and lifetime");removeOwnHole();
            var destiny = center(3).add(.1, 1.05, 0);
            game.items().homing().emitAt(owner, InteractionHand.MAIN_HAND, empty, 3, destiny, new Vec3(0, 0, 1));
            h = hole(center(3)); pull(h); movedDestiny = game.items().homing().destinySnapshot().getLast().position();
            require(movedDestiny.y < destiny.y, "Homing Destiny producer origin is pulled"); removeOwnHole();
            var duplicate = center(4).add(.1, 1.05, 0);
            // Ordinary mode exercises the shared marker origin without opening cross-dimension clone sources.
            game.items().duplicating().emitAt(owner, 1, duplicate.add(0, 0, -7), new Vec3(0, 0, 1));
            h = hole(center(4)); pull(h); movedDup = game.items().duplicating().markerSnapshot().getLast().point();
            require(movedDup.y < duplicate.y, "Duplicating marker origin is pulled"); removeOwnHole();
            var desired = center(5).add(.1, 1.05, 0);
            moveOwner(desired.add(0, -owner.getEyeHeight(), -2));
            game.items().sauvojen().cast(owner, InteractionHand.MAIN_HAND, new ItemStack(LbcWands.SAUVOJEN), 0);
            drone = game.items().sauvojen().snapshot().getLast(); var previousPoint = drone.position;
            h = hole(center(5)); pull(h); movedDrone = drone.position; droneLifetime = drone.lifetime;
            require(movedDrone.y < previousPoint.y, "Sauvojen drone logical origin is pulled"); near(drone.carrier.position(), movedDrone, "Sauvojen native carrier agrees with logical origin"); removeOwnHole();
            var pumpkin = center(6).add(.1, 1.05, 0);
            var egg = game.lbc().rollTable(level, pumpkin, owner, "lbc:ported/pumpkinator").getFirst(); owner.setItemInHand(InteractionHand.MAIN_HAND, egg);
            require(game.items().pumpkinator().use(owner, InteractionHand.MAIN_HAND, pumpkin), "actual Pumpkinator egg admission/debit");
            pumpkinId = game.items().pumpkinator().actorViews(Integer.MAX_VALUE).getLast().id();
            h = hole(center(6)); pull(h); movedPumpkin = pumpkin().position();
            require(movedPumpkin.y < pumpkin.y, "Pumpkinator authoritative marker height is pulled"); removeOwnHole();
            additionalLiveWriters();
            producerCheckpoint();
        }
        void additionalLiveWriters(){
            var blaze=game.items().blaze();var blind=game.items().blind();var danmaku=game.items().danmaku();var stand=game.items().stand();var erecto=game.items().erecto();
            require(blaze.turretCount()==0&&blind.cloudCount(owner)==0&&danmaku.count()==0&&stand.fistCount()==0&&erecto.sphereCount()==0,
                    "exclusive additional live native producer families");
            var point=center(6).add(0,50,0);var beforeOwner=owner.position();boolean hadStand=owner.entityTags().contains("have_stand");
            var block=BlockPos.containing(point.add(2,0,0));var cloudBlock=block.below(10);
            var original=game.maps().snapshot(level,block);var originalCloud=game.maps().snapshot(level,cloudBlock);
            try{
                level.setBlock(block,Blocks.STONE.defaultBlockState(),2|16|32);level.setBlock(cloudBlock,Blocks.STONE.defaultBlockState(),2|16|32);
                moveOwner(point.add(-5,0,0));
                blaze.chickenTurret(owner,point.add(1.2,.4,0));
                var cloudFrom=Vec3.atCenterOf(cloudBlock).add(-4,0,0);
                var cloudHit=me.skywars2.game.items.wands.LbcTerra.wallHit(level,cloudFrom,cloudFrom.add(8,0,0));
                require(cloudHit!=null&&cloudHit.getBlockPos().equals(cloudBlock),"real Blind ray starts inside the terminal block's Y/Z slab");
                blind.emitAt(owner,InteractionHand.MAIN_HAND,2,cloudFrom,new Vec3(1,0,0));
                require(blind.cloudCount(owner)==1,"real Blind rain terminal publishes one virtual cloud");
                require(danmaku.scriptBomb(owner,point.add(1.4,.5,0),300,true)&&!danmaku.bombs().isEmpty(),"real Danmaku native bomb display producer");
                var bomb=danmaku.bombs().getLast().display;
                owner.entityTags().add("have_stand");owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(me.skywars2.game.entities.mobs.summons.LbcStand.ORAORA));
                require(stand.use(owner,InteractionHand.MAIN_HAND),"real Stand burst input");
                for(int i=0;i<4&&stand.fistCount()==0;i++)stand.tick();
                require(stand.fistCount()>0,"bounded native Stand fist publication");var fist=stand.fistSnapshot().getLast();fist.setPos(point.add(1.3,.6,0));
                var sphereFrom=Vec3.atCenterOf(block).add(-4,0,0);
                var sphereHit=me.skywars2.game.items.wands.LbcTerra.wallHit(level,sphereFrom,sphereFrom.add(8,0,0));
                require(sphereHit!=null&&sphereHit.getBlockPos().equals(block),"real Erecto sphere ray intersects its authored terminal");
                erecto.emitAt(owner,InteractionHand.MAIN_HAND,ItemStack.EMPTY,3,sphereFrom,new Vec3(1,0,0));
                require(erecto.sphereCount()==1,"real Erecto sphere terminal");var sphere=erecto.sphereSnapshot().getLast();int sphereAge=sphere.age;
                var turretBefore=new ArrayList<Vec3>();blaze.transformOrigins(level,p->{turretBefore.add(p);return p;});
                var cloudBefore=new ArrayList<Vec3>();blind.transformOrigins(level,p->{cloudBefore.add(p);return p;});
                var bombBefore=bomb.position();var fistBefore=fist.position();var sphereBefore=sphere.position;
                var h=hole(point);pull(h);
                var turretAfter=new ArrayList<Vec3>();blaze.transformOrigins(level,p->{turretAfter.add(p);return p;});
                var cloudAfter=new ArrayList<Vec3>();blind.transformOrigins(level,p->{cloudAfter.add(p);return p;});
                require(turretAfter.size()==1&&turretAfter.getFirst().distanceToSqr(point)<turretBefore.getFirst().distanceToSqr(point),"actual shared black-hole callback pulls live Blaze turret origin");
                require(cloudAfter.size()==1&&cloudAfter.getFirst().distanceToSqr(point)<cloudBefore.getFirst().distanceToSqr(point),"actual shared black-hole callback pulls live Blind cloud origin");
                require(bomb.position().distanceToSqr(point)<bombBefore.distanceToSqr(point),"actual shared black-hole callback translates Danmaku native display");
                require(fist.position().distanceToSqr(point)<fistBefore.distanceToSqr(point),"actual shared black-hole callback translates Stand native fist display");
                require(sphere.position.distanceToSqr(point)<sphereBefore.distanceToSqr(point)&&sphere.age==sphereAge,"actual shared black-hole callback translates Erecto live sphere without advancing its clock");
            }finally{
                removeOwnHole();blaze.forget(owner);blind.forget(owner);danmaku.forget(owner);stand.forget(owner);erecto.forget(owner);
                if(!hadStand)owner.entityTags().remove("have_stand");
                for(var effect:List.of(net.minecraft.world.effect.MobEffects.SPEED,net.minecraft.world.effect.MobEffects.JUMP_BOOST,net.minecraft.world.effect.MobEffects.HEALTH_BOOST,net.minecraft.world.effect.MobEffects.RESISTANCE))owner.removeEffect(effect);
                game.maps().restoreSnapshot(level,block,original,2|16|32);game.maps().restoreSnapshot(level,cloudBlock,originalCloud,2|16|32);moveOwner(beforeOwner);
            }
        }
        me.skywars2.game.items.gadgets.LbcPumpkinator.ActorView pumpkin() {
            return game.items().pumpkinator().actorViews(Integer.MAX_VALUE).stream().filter(actor -> actor.id() == pumpkinId).findFirst().orElseThrow();
        }
        void producerCheckpoint() {
            var bodies = MatchActorCheckpoint.currentBodies(game); var sources = new MatchDamageSources(game, bodies.values());
            var spells = new MatchSpellCheckpoint(game, sources); var links = new MatchSpellCheckpoint.Links(sources);
            var homing = game.items().homing().checkpoint(links);
            game.items().duplicating().beginCheckpointDrain();
            me.skywars2.game.skills.clone.LbcDuplicating.Checkpoint duplicate;
            try { duplicate = game.items().duplicating().checkpoint(links); }
            finally { game.items().duplicating().endCheckpointDrain(); }
            var sauvojen = game.items().sauvojen().checkpointDrones(links); var pumpkin = game.items().pumpkinator().checkpoint(links);
            var carrier = drone.carrier; var carrierImage = saved(carrier);
            require(links.resolve(bodies::get).isEmpty(), "all transported private producer checkpoint references resolve");
            java.util.function.UnaryOperator<Vec3> shift = point -> point.add(0, 4, 0);
            game.items().homing().transformOrigins(level, shift); game.items().duplicating().transformOrigins(level, shift);
            game.items().sauvojen().transformOrigins(level, shift); game.items().pumpkinator().transformOrigins(level, shift);
            game.server().tickRateManager().setFrozen(true);
            try {
                load(carrier, carrierImage);
                spells.prepareFrozen(); homing.prepare(links).run(); duplicate.prepare(links).run(); sauvojen.prepare(links).run(); pumpkin.prepare(links).run();
                require(spells.restoreFrozen(bodies::get, false).isEmpty(), "complete MatchSpellCheckpoint restores canonical source policies");
                spells.publishFrozen(); game.items().duplicating().endCheckpointDrain(); game.items().sauvojen().publishCloneDrones();
                near(game.items().homing().destinySnapshot().getLast().position(), movedDestiny, "checkpoint keeps pulled Homing origin");
                near(game.items().duplicating().markerSnapshot().getLast().point(), movedDup, "checkpoint keeps pulled Duplicating origin");
                drone = game.items().sauvojen().snapshot().getLast(); near(drone.position, movedDrone, "checkpoint keeps pulled Sauvojen origin");
                near(drone.carrier.position(), movedDrone, "actual native NBT plus typed producer replay retain coherent carrier pose");
                near(pumpkin().position(), movedPumpkin, "checkpoint keeps pulled Pumpkinator origin");
            } finally { game.server().tickRateManager().setFrozen(false); }
        }
        boolean step() {
            acknowledge(); int now = game.server().getTickCount();
            require(now - (restoring ? restoreStart : start) < 2000, "bounded native preparation/rollback");
            if (phase >= 2 && phase <= 5) require(now - trialStart < 250, "placed400tick deadline cannot fire before rollback");
            if (phase == 0) { if (!ready.isDone()) return false; ready.join(); captureBaseline(); phase = 1; return false; }
            if (phase == 1) { if (!game.vanity().hasSnapshot() || game.vanity().busy()) return false; captured = true; trialStart = now; game.server().tickRateManager().setFrozen(false); initializeTrial(); phase = 2; return false; }
            if (phase == 2) { if (game.items().nukeEvent().jobCount() != 0) return false; beaconPull(); at = now; phase = 3; return false; }
            if (phase == 3) {
                if (now == at) return false; near(beacon.position(), movedBeacon, "next native tick retains translated nuclear marker");
                require(level.getBlockState(beacon.blockPosition().above()).is(Blocks.BEACON), "next native tick retains translated physical beacon");
                bossPull(); at = now; phase = 4; return false;
            }
            if (phase == 4) {
                if (now == at) return false; near(boss.position(), movedBoss, "next real boss tick does not snap back to old anchor"); near(anchor(boss), movedBoss, "next real boss tick uses transported canonical anchor");
                virtualPulls(); at = now; phase = 5; return false;
            }
            if (phase == 5) {
                if (now == at) return false;
                require(trace.age > traceAge && Math.abs(trace.position.y - movedTrace.y) < .0001, "next real electrical tick advances from pulled slow-ray origin");
                var duplicate = game.items().duplicating().markerSnapshot().getLast();
                near(duplicate.point(), movedDup.add(0, 0, 1), "next native Duplicating step begins at checkpointed pulled origin");
                require(drone.lifetime > droneLifetime && drone.position.distanceTo(movedDrone) > .09
                        && drone.position.distanceTo(movedDrone) < .11, "next real Sauvojen step advances from pulled logical point");
                near(drone.position, drone.carrier.position(), "next native Sauvojen carrier follows logical motion");
                require(pumpkin().pulses() > 0 && Math.abs(pumpkin().position().y - movedPumpkin.y) < 1e-9
                        && pumpkin().writes() > 0, "next real Pumpkinator terrain pulse retains transported Y and performs authoritative writes");
                var destiny = game.items().homing().destinySnapshot().getLast();
                require(destiny.remaining() == 599 && destiny.position().distanceTo(movedDestiny) <= .30001,
                        "next real Homing producer frame starts from pulled origin, with at most its native .3 step");
                requestRestore(); return false;
            }
            if (phase == 7) {
                if (!game.vanity().poiRetirement().isDone()) return false;
                game.vanity().poiRetirement().join(); finish(); return true;
            }
            if (game.vanity().busy()) { require(!game.vanity().status().startsWith("FAILED"), "canonical cleanup remains recoverable: " + game.vanity().status()); return false; }
            require(game.items().nukeEvent().pointPendingTicks() == baselineEoe && game.items().nukeEvent().markerCount() == baselineNukes,
                    "full native rollback restores original pendingEoe timer and nuclear producers");
            require(game.items().grimoireBoss().snapshot().size() == baselineBosses && game.items().pumpkinator().count() == baselinePumpkins
                    && game.items().homing().destinyCount() == baselineHoming && game.items().duplicating().markerSnapshot().size() == baselineDup
                    && game.items().sauvojen().count() == baselineDrones && game.items().voidArcanum().count() == 0
                    && game.items().wands().electroSnapshot().isEmpty(),
                    "original producers restored without global stop/forget cleanup");
            for (var entry : baselineTerrain.entrySet()) require(game.maps().snapshot(level, entry.getKey()).equals(entry.getValue()),
                    "canonical native terrain rollback at " + entry.getKey());
            game.vanity().stop(); phase = 7; return false;
        }
        void requestRestore() {
            // Keep the canonical result frozen until the oracle has checked its exact timer/producer state.
            game.server().tickRateManager().setFrozen(true);
            require(game.vanity().requestRestoreForCheck(), "full snapshot rollback accepted");
            restoring = true; restoreStart = game.server().getTickCount(); phase = 6;
        }
        void recover(Throwable error) {
            if (failure == null) failure = error; else failure.addSuppressed(error);
            if (captured && !restoring) requestRestore();
            else if (!captured) { game.vanity().stop(); finish(); }
        }
        void finish() {
            if (finished) return; finished = true;
            try { wire.close(); audience.close(); }
            finally {
                for (var pair : List.of(Map.entry(owner.getUUID(), previous), Map.entry(other.getUUID(), previousOther))) {
                    var current = game.server().getPlayerList().getPlayer(pair.getKey()); if (current == null) continue;
                    current.connection = pair.getValue(); pair.getValue().player = current; pair.getValue().resetPosition();
                }
                game.server().tickRateManager().setFrozen(frozen);
            }
        }
    }
    public static void run(GameController game) { require(running == null, "exclusive scene"); running = new Scene(game); SkyWars2.LOGGER.info("[check] START void-transport"); }
    public static void tick(GameController game) {
        if (running == null) return; var scene = running;
        try {
            if (!scene.step()) return; running = null;
            if (scene.failure != null) SkyWars2.LOGGER.error("[check] FAIL void-transport (canonical cleanup completed)", scene.failure);
            else SkyWars2.LOGGER.info("[check] PASS void-transport (native beacon/frame and Grimoire anchor,5 virtual origins, actual next ticks, typed producer/fullspell checkpoints, full terrain/producers/pendingEoe rollback)");
        } catch (Throwable error) {
            if (scene.restoring) { running = null; SkyWars2.LOGGER.error("[check] FAIL void-transport canonical cleanup (snapshot retained={})", game.vanity().hasSnapshot(), error); }
            else { try { scene.recover(error); if (scene.finished) { running = null; SkyWars2.LOGGER.error("[check] FAIL void-transport before mutation", scene.failure); } }
                catch (Throwable cleanup) { error.addSuppressed(cleanup); running = null; SkyWars2.LOGGER.error("[check] FAIL void-transport recovery; snapshot retained", error); } }
        }
    }
}
